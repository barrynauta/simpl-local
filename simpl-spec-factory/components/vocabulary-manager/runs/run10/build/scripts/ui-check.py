#!/usr/bin/env python3
"""Own UI check against the running stack (not part of the test suite): logs in through Keycloak, walks the main
UI flow (list, create, details, new version, status change) and runs axe-core (WCAG 2.1 AA) on each page.
Usage: python3 scripts/ui-check.py <path to axe.min.js>   (needs the Playwright Python package and Chromium)."""
import sys
import time

from playwright.sync_api import expect, sync_playwright

UI = "http://localhost:4433"
AXE = open(sys.argv[1], encoding="utf-8").read()
RUN = f"Ui{int(time.time())}"
TURTLE = f"@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n@prefix ex: <https://example.org/{RUN}#> .\nex:S a skos:ConceptScheme .\n"
failures = []


def axe(page, label):
    page.evaluate(AXE)
    result = page.evaluate("async () => (await axe.run(document, {runOnly: ['wcag2a','wcag2aa','wcag21a','wcag21aa']})).violations.map(v => v.id + ': ' + v.nodes.length)")
    print(f"axe {label}: {result or 'no violations'}")
    if result:
        failures.append(f"axe {label}: {result}")


def login(page, user):
    page.goto(UI + "/vocabularies")
    page.fill("#username", user)
    page.fill("#password", "Password1!")
    page.click("#kc-login")
    page.wait_for_url(UI + "/vocabularies*")


with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    errors = []
    page.on("console", lambda msg: errors.append(msg.text) if msg.type == "error" else None)
    login(page, "ga-admin")
    expect(page.get_by_role("heading", name="Vocabularies")).to_be_visible()
    axe(page, "list")

    page.get_by_role("link", name="New Vocabulary +").click()
    page.wait_for_url(UI + "/vocabularies/new")
    page.get_by_role("button", name="Submit").click()
    expect(page.get_by_text("Name is required.")).to_be_visible()
    axe(page, "create form")
    page.get_by_label("Name").fill(RUN)
    page.get_by_label("Description").fill("Created by the UI check")
    page.get_by_label("Vocabulary file").set_input_files(files=[{"name": "v.ttl", "mimeType": "text/turtle", "buffer": TURTLE.encode()}])
    expect(page.get_by_text("Syntax check passed.")).to_be_visible()
    page.get_by_role("button", name="Submit").click()
    page.wait_for_url(UI + f"/vocabularies/{RUN}")
    expect(page.get_by_role("status").filter(has_text=f"Version 1 of {RUN} was created.")).to_be_visible()
    axe(page, "details")

    page.get_by_role("link", name="Update").click()
    page.wait_for_url(UI + f"/vocabularies/{RUN}/versions/new")
    expect(page.get_by_text(f"{RUN}.ttl (current content)")).to_be_visible()
    axe(page, "new version form")
    page.get_by_label("Description").fill("Second description")
    page.get_by_label("Change log").fill("Changed the description.")
    page.get_by_role("button", name="Submit").click()
    page.wait_for_url(UI + f"/vocabularies/{RUN}")
    expect(page.get_by_text(f"Version 2 of {RUN} was created.")).to_be_visible()
    expect(page.get_by_role("region", name="Version 2")).to_be_visible()
    expect(page.get_by_role("table").get_by_text("This version is deprecated.")).to_be_visible()

    page.get_by_role("button", name="Deprecate").click()
    expect(page.get_by_text(f"Status of {RUN} changed to Deprecated.")).to_be_visible()
    expect(page.locator(".banner")).to_have_text("This vocabulary is deprecated. Its use is discouraged.")

    page.get_by_role("link", name="Back to vocabulary list").first.click()
    page.get_by_label("Name contains").fill(RUN)
    page.get_by_role("button", name="Apply filters").click()
    expect(page.get_by_role("img", name="This vocabulary is deprecated. Its use is discouraged.")).to_be_visible()
    page.get_by_role("button", name=f"Actions for {RUN}").click()
    expect(page.get_by_role("menuitem", name="Activate")).to_be_visible()
    page.keyboard.press("Escape")

    viewer = browser.new_page()
    login(viewer, "ga-viewer")
    expect(viewer.get_by_role("link", name="New Vocabulary +")).to_have_count(0)
    viewer.goto(UI + "/vocabularies/new")
    expect(viewer.get_by_role("alert")).to_have_text("You are not authorized to perform this action.")
    print("console errors:", errors or "none")
    browser.close()

if failures:
    print("FAILURES:", failures)
    sys.exit(1)
print("UI check passed")
