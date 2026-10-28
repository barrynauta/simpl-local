"""Swap experiments 2 and 3 helper: drive the UPSTREAM UI through list, create, details,
new version, download and revoke, and log every API call with its status.

    walk_upstream_ui.py <ui_url> <ttl>

The upstream UI reads the roles from the access token in the cookie "token" (set by its
Keycloak flow, which is off in the local image). The walk sets that cookie to a self-made
token of ga-admin; the backend behind the UI's nginx gets the token nginx injects.
"""
import re
import sys
import time
from pathlib import Path

from playwright.sync_api import sync_playwright

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tests" / "features"))
import smlib  # noqa: E402

ui, ttl = sys.argv[1].rstrip("/"), sys.argv[2]
name = "AaSwapUi" + time.strftime("%H%M%S")  # sorts first in a long list
calls = []


def step(msg):
    print("STEP", msg)


def body(pg, n=400):
    return pg.inner_text("body")[:n].replace("\n", " | ")


with sync_playwright() as p:
    b = p.chromium.launch()
    c = b.new_context(accept_downloads=True)
    c.add_cookies([{"name": "token", "value": smlib.self_made_token("ga-admin"), "url": ui}])
    pg = c.new_page()
    pg.set_default_timeout(8000)
    pg.on("response", lambda r: calls.append((r.request.method, r.status, r.url.replace(ui, "")))
          if "/v1/" in r.url else None)

    pg.goto(ui + "/")
    pg.wait_for_load_state("networkidle")
    step("list: " + body(pg, 300))

    pg.get_by_role("button", name="New schema").click()
    pg.wait_for_timeout(1000)
    step("create form at " + pg.url.replace(ui, ""))
    pg.locator("input[type=file]").first.set_input_files(ttl)
    # Upstream form: ids schemaName, title, description; resource type is a select of
    # Data, Infrastructure and Application, preset to Data.
    for fid, value in (("schemaName", name), ("title", f"Title {name}"), ("description", "Swap UI walk")):
        loc = pg.locator(f"#{fid}")
        if loc.count():
            loc.first.fill(value)
            loc.first.blur()
        else:
            step(f"no field {fid}")
    step("buttons " + str([t for t in pg.get_by_role("button").all_inner_texts() if t.strip()]))
    pg.get_by_role("button", name=re.compile("^(upload|save|submit|create)", re.I)).last.click()
    pg.wait_for_timeout(2500)
    step("after create at " + pg.url.replace(ui, "") + ": " + body(pg, 500))

    pg.goto(f"{ui}/schemas/{name}")
    pg.wait_for_load_state("networkidle")
    pg.wait_for_timeout(800)
    step("details: " + body(pg, 600))

    nv = pg.get_by_role("button", name=re.compile("new version", re.I))
    if nv.count():
        nv.first.click()
        pg.wait_for_timeout(1000)
        step("new version form at " + pg.url.replace(ui, ""))
        pg.locator("input[type=file]").first.set_input_files(ttl)
        cl = pg.get_by_label(re.compile("change ?log", re.I))
        if cl.count():
            cl.first.fill("Swap UI walk, second version")
        pg.get_by_role("button", name=re.compile("^(upload|save|submit|create)", re.I)).last.click()
        pg.wait_for_timeout(2500)
        step("after new version at " + pg.url.replace(ui, "") + ": " + body(pg, 600))
    else:
        step("no New version button")

    pg.goto(ui + "/")
    pg.wait_for_load_state("networkidle")
    # The list is long; narrow it with the UI's own resource type filter.
    flt = pg.get_by_label(re.compile("^Resource type", re.I))
    if flt.count():
        flt.first.fill("Data")
        pg.get_by_role("button", name="Apply filters").click()
        pg.wait_for_load_state("networkidle")
        pg.wait_for_timeout(800)
    row = pg.locator("tr", has_text=name)
    step(f"row in the filtered list: {row.count()}")
    if row.count():
        # Row actions: a dropdown with Download, Revoke and Publish (plain texts, no menu roles).
        def menu():
            row.first.locator("button[id^=schema-actions-]").click()
            pg.wait_for_timeout(500)
        def item(label):
            return pg.get_by_text(label, exact=True).locator("visible=true")
        menu()
        step("row menu " + str([t for t in ("Download", "Revoke", "Publish") if item(t).count()]))
        if item("Download").count():
            try:
                with pg.expect_download(timeout=5000) as d:
                    item("Download").first.click()
                step("download file " + d.value.suggested_filename)
            except Exception as e:
                step("no download: " + str(e).splitlines()[0][:120])
            pg.keyboard.press("Escape")
        # Publish (if the slot was taken on create) and revoke, each through its confirm dialog.
        for action in ("Publish", "Revoke"):
            menu()
            if not item(action).count():
                step(f"no {action} in the menu")
                pg.keyboard.press("Escape")
                continue
            item(action).first.click()
            pg.wait_for_timeout(800)
            pg.locator(f"#{action.lower()}-modal-confirm").click()
            pg.wait_for_timeout(1500)
            row = pg.locator("tr", has_text=name)
            step(f"after {action.lower()}: " + row.first.inner_text().replace("\n", " ")[:160]
                 + " || " + body(pg, 2000).split("Schema management")[0][-160:])
    b.close()

for x in calls:
    print("CALL", *x)
