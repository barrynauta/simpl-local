"""Own UI check of the Vocabulary Manager (Playwright for Python, headless Chromium).

Runs the main UI flow of the decisions against the running stack and checks WCAG 2.1 AA with axe-core
on the list, details and both form pages (Q6). Uses only roles, decided labels and decided texts (D75).

Usage: python ui_check.py  (env UI_URL, default http://localhost:4433)
"""

import os
import pathlib
import sys
import tempfile
import time

from playwright.sync_api import expect, sync_playwright

UI = os.environ.get("UI_URL", "http://localhost:4433")
AXE = (pathlib.Path(__file__).parent.parent / "node_modules" / "axe-core" / "axe.min.js").read_text()
RUN = str(int(time.time()))[-6:]
NAME = f"Ui{RUN}Vocab"
failures = []


def check(condition, message):
    print(("PASS " if condition else "FAIL ") + message)
    if not condition:
        failures.append(message)


def axe(page, label):
    page.add_script_tag(content=AXE)
    result = page.evaluate(
        "async () => (await axe.run(document, {runOnly: {type: 'tag', values: ['wcag2a','wcag2aa','wcag21a','wcag21aa']}})).violations"
    )
    details = [f"{v['id']} ({len(v['nodes'])})" for v in result]
    check(not result, f"axe {label}: {details or 'no violations'}")


def login(page, user):
    page.goto(UI + "/vocabularies")
    page.fill("#username", user)
    page.fill("#password", "Password1!")
    page.click("#kc-login")
    page.wait_for_url(UI + "/vocabularies*")


def turtle(text):
    path = pathlib.Path(tempfile.mkdtemp()) / "vocabulary.ttl"
    path.write_text(text)
    return str(path)


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        context = browser.new_context(bypass_csp=True)  # only to inject axe-core
        page = context.new_page()
        page.on("pageerror", lambda e: failures.append(f"page error: {e}"))
        login(page, "ga-admin")
        expect(page.get_by_role("heading", name="Vocabularies")).to_be_visible()
        axe(page, "list")

        page.get_by_role("link", name="New Vocabulary +").click()
        page.wait_for_url(UI + "/vocabularies/new")
        check(page.get_by_label("Change log").count() == 0, "create form has no change log field (D32)")
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_text("Name is required.")).to_be_visible()
        check(page.evaluate("document.activeElement.id") == "field-name", "focus moves to first field error (D62)")
        page.get_by_label("Vocabulary file").set_input_files(turtle("@prefix ex: <http://e.org/> .\nex:a ex:b .\n"))
        expect(page.get_by_text("line 2")).to_be_visible()
        check(page.get_by_role("button", name="Submit").is_disabled(), "submit disabled on syntax error (D13)")
        axe(page, "create form")
        good = (
            "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
            "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
            "<http://example.org/ui#a> a skos:Concept ; skos:prefLabel \"A\"@en ; dcterms:title \"t\" .\n"
        )
        page.get_by_label("Vocabulary file").set_input_files(turtle(good))
        expect(page.get_by_text("Syntax check passed.")).to_be_visible()
        page.get_by_label("Name").fill(NAME)
        page.get_by_label("Description").fill("UI check vocabulary")
        page.get_by_role("button", name="Submit").click()
        page.wait_for_url(UI + f"/vocabularies/{NAME}")
        expect(page.get_by_role("status").filter(has_text=f"Version 1 of {NAME} was created.")).to_be_visible()
        expect(page.get_by_role("region", name="Validation warnings")).to_be_visible()
        axe(page, "details")

        page.get_by_role("link", name="Update").click()
        page.wait_for_url(UI + f"/vocabularies/{NAME}/versions/new")
        expect(page.get_by_text(f"{NAME}.ttl (current content)")).to_be_visible()
        expect(page.get_by_text("Submitting creates version 2 and deprecates every older ACTIVE version.")).to_be_visible()
        check(page.get_by_label("Description").input_value() == "UI check vocabulary", "description pre-filled (D33)")
        axe(page, "new version form")
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_text("Change log is required.")).to_be_visible()
        page.get_by_label("Change log").fill("Unchanged content")
        page.get_by_role("button", name="Submit").click()
        expect(page.get_by_role("alert")).to_contain_text("already has this content and description")
        page.get_by_label("Description").fill("UI check vocabulary, second version")
        page.get_by_role("button", name="Submit").click()
        page.wait_for_url(UI + f"/vocabularies/{NAME}")
        expect(page.get_by_role("status").filter(has_text=f"Version 2 of {NAME} was created.")).to_be_visible()
        rows = page.get_by_role("table").get_by_role("row")
        expect(rows).to_have_count(3)
        expect(page.get_by_text("This version is deprecated.")).to_be_visible()
        page.get_by_role("link", name="Latest ACTIVE version: 2").click()
        expect(page.get_by_role("region", name="Version 2")).to_be_visible()
        page.get_by_role("button", name="Actions for version 1").click()
        page.get_by_role("menuitem", name="View version details").click()
        region = page.get_by_role("region", name="Version 1")
        expect(region).to_contain_text("ga-admin@example.com")
        with page.expect_download() as download:
            region.get_by_role("button", name="Download").click()
        check(download.value.suggested_filename == f"{NAME}-v1.ttl", "version download filename (D29)")

        page.get_by_role("link", name="Back to vocabulary list").click()
        page.get_by_label("Name contains").fill(NAME.lower())
        expect(page.get_by_role("row")).to_have_count(2)
        page.get_by_role("button", name=f"Actions for {NAME}").click()
        page.get_by_role("menuitem", name="Deprecate").click()
        expect(page.get_by_role("status")).to_have_text(f"Status of {NAME} changed to Deprecated.")
        expect(page.get_by_role("img", name="This vocabulary is deprecated. Its use is discouraged.")).to_be_visible()
        page.get_by_role("columnheader", name="Name").get_by_role("button").click()
        check(
            page.get_by_role("columnheader", name="Name").get_attribute("aria-sort") == "descending",
            "click on active sort column reverses it (D50)",
        )
        page.get_by_role("button", name=f"Actions for {NAME}").click()
        page.get_by_role("menuitem", name="Activate").click()
        expect(page.get_by_role("status")).to_have_text(f"Status of {NAME} changed to Active.")

        viewer = browser.new_context().new_page()
        login(viewer, "ga-viewer")
        check(viewer.get_by_role("link", name="New Vocabulary +").count() == 0, "viewer sees no create action (D18)")
        viewer.goto(UI + "/vocabularies/new")
        expect(viewer.get_by_role("alert")).to_have_text("You are not authorized to perform this action.")
        browser.close()

    print(f"\n{len(failures)} failure(s)")
    for failure in failures:
        print(" -", failure)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
