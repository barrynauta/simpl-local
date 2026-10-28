"""Swap experiment 3 helper: drive the BUILD UI through list, create, details, new version,
download and revoke, and log every API call with its status.

    walk_build_ui.py <ui_url> <ttl> [user]

Signs in on the build's Keycloak (the UI's own flow). Locators are the data-testid values
of D46 and the routes of D45.
"""
import re
import sys
import time

from playwright.sync_api import sync_playwright

ui, ttl = sys.argv[1].rstrip("/"), sys.argv[2]
user = sys.argv[3] if len(sys.argv) > 3 else "ga-admin"
name = "AaSwapBui" + time.strftime("%H%M%S")  # sorts first in a long list
calls = []


def step(msg):
    print("STEP", msg)


def tid(pg, t):
    return pg.locator(f"[data-testid='{t}']")


def notes(pg):
    out = []
    for t in ("notification-success", "notification-error"):
        loc = tid(pg, t)
        if loc.count() and loc.first.is_visible():
            out.append(f"{t}: " + loc.first.inner_text().replace("\n", " ")[:200])
    return "; ".join(out) or "no notification"


def submit(pg):
    pg.get_by_role("button", name=re.compile(r"^(create|submit|save|upload)", re.I)).last.click()
    pg.wait_for_timeout(2500)


with sync_playwright() as p:
    b = p.chromium.launch()
    c = b.new_context(accept_downloads=True)
    pg = c.new_page()
    pg.set_default_timeout(8000)
    pg.on("response", lambda r: calls.append((r.request.method, r.status, r.url.replace(ui, "")))
          if "/v1/" in r.url else None)

    pg.goto(ui + "/schemas")
    pg.wait_for_load_state("networkidle")
    if pg.locator("#username").count():
        pg.fill("#username", user)
        pg.fill("#password", "password")
        pg.click("#kc-login")
        pg.wait_for_load_state("networkidle")
    pg.wait_for_timeout(1000)
    step("list: " + pg.inner_text("body")[:300].replace("\n", " | "))

    pg.goto(ui + "/schemas/new")
    pg.wait_for_load_state("networkidle")
    for t, v in (("field-name", name), ("field-title", f"Title {name}"), ("field-description", "Swap UI walk")):
        tid(pg, t).first.fill(v)
    rt = tid(pg, "field-resource-type").first
    if rt.evaluate("e => e.tagName") == "SELECT":
        rt.select_option("Data")
    else:
        rt.fill("Data")
    pg.locator("input[type=file]").first.set_input_files(ttl)
    submit(pg)
    step("after create at " + pg.url.replace(ui, "") + ": " + notes(pg))

    pg.goto(f"{ui}/schemas/{name}?schemaType=RESOURCE")
    pg.wait_for_load_state("networkidle")
    pg.wait_for_timeout(800)
    step("details: " + pg.inner_text("main")[:400].replace("\n", " | ") if pg.locator("main").count()
         else "details: " + pg.inner_text("body")[:400].replace("\n", " | "))

    pg.goto(f"{ui}/schemas/{name}/versions/new?schemaType=RESOURCE")
    pg.wait_for_load_state("networkidle")
    tid(pg, "field-changelog").first.fill("Swap UI walk, second version")
    pg.locator("input[type=file]").first.set_input_files(ttl)
    submit(pg)
    step("after new version at " + pg.url.replace(ui, "") + ": " + notes(pg))

    pg.goto(ui + "/schemas")
    pg.wait_for_load_state("networkidle")
    pg.wait_for_timeout(800)
    row = tid(pg, f"schema-row-{name}")
    step(f"row on page 1: {row.count()}" + (" | " + row.first.inner_text().replace("\n", " ")[:160] if row.count() else ""))
    if row.count():
        dl = tid(pg, f"action-download-{name}")
        if dl.count():
            try:
                with pg.expect_download(timeout=5000) as d:
                    dl.first.click()
                step("download file " + d.value.suggested_filename)
            except Exception as e:
                step("no download: " + str(e).splitlines()[0][:120] + " | " + notes(pg))
        for action in ("publish", "revoke"):
            btn = tid(pg, f"action-{action}-{name}")
            if not btn.count():
                step(f"no {action} action")
                continue
            btn.first.click()
            pg.wait_for_timeout(600)
            tid(pg, "confirm-button").first.click()
            pg.wait_for_timeout(1500)
            row = tid(pg, f"schema-row-{name}")
            step(f"after {action}: " + (row.first.inner_text().replace("\n", " ")[:160] if row.count() else "row gone")
                 + " | " + notes(pg))
    b.close()

for x in calls:
    print("CALL", *x)
