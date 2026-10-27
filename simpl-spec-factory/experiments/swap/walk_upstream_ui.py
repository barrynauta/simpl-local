"""Experiment 2/3 helper: drive the UPSTREAM UI through new version, version status change and
external vocabulary creation, logging every API call. Usage: walk_upstream_ui.py <ui_url> <vocabularyName> <ttl>"""
import sys, re
from playwright.sync_api import sync_playwright
ui, name, ttl = sys.argv[1].rstrip('/'), sys.argv[2], sys.argv[3]
calls=[]
def step(msg): print("STEP", msg)
with sync_playwright() as p:
    b=p.chromium.launch(); c=b.new_context(accept_downloads=True); pg=c.new_page(); pg.set_default_timeout(8000)
    pg.on("response", lambda r: calls.append((r.request.method, r.status, r.url.split('?')[0])) if re.search(r'/(api|v1)/', r.url) else None)
    pg.goto(f"{ui}/vocabularies/{name}"); pg.wait_for_load_state("networkidle")
    if pg.locator("#username").count():
        pg.fill("#username","ga-admin"); pg.fill("#password","password"); pg.click("#kc-login"); pg.wait_for_load_state("networkidle")
    pg.get_by_role("button", name=re.compile("New version", re.I)).click(); pg.wait_for_timeout(800)
    step("dialog text: "+pg.locator("[role=dialog], form").first.inner_text()[:300].replace("\n"," | "))
    pg.locator("input[type=file]").first.set_input_files(ttl)
    for lab in ["Description","Change log","Changelog"]:
        loc=pg.get_by_label(re.compile(lab, re.I))
        if loc.count(): loc.first.fill(f"walk {lab}")
    btns=[t for t in pg.locator("[role=dialog] button, form button").all_inner_texts()]; step("dialog buttons "+str(btns))
    pg.get_by_role("button", name=re.compile("^(upload|save|submit|create)", re.I)).last.click(); pg.wait_for_timeout(2500)
    step("after new version: "+pg.inner_text("body")[:500].replace("\n"," | "))
    # actions menu of the newest version row
    rows=pg.locator("tbody tr"); step(f"rows {rows.count()}")
    rows.first.locator("button").first.click(); pg.wait_for_timeout(500)
    items=[t.strip() for t in pg.get_by_role("menuitem").all_inner_texts()]; step("row menu "+str(items))
    dl=[i for i in items if re.search("download", i, re.I)]
    if dl:
        with pg.expect_download() as d: pg.get_by_role("menuitem", name=dl[0]).click()
        step("download file "+d.value.suggested_filename)
        rows.first.locator("button").first.click(); pg.wait_for_timeout(500)
    st=[i for i in items if re.search("deprecat|status|activ", i, re.I)]
    if st:
        pg.get_by_role("menuitem", name=st[0]).click(); pg.wait_for_timeout(800)
        btn=pg.get_by_role("button", name=re.compile("confirm|yes|deprecate|update|save", re.I))
        if btn.count(): btn.last.click()
        pg.wait_for_timeout(1500); step("after status: "+pg.inner_text("body")[:400].replace("\n"," | "))
    b.close()
for x in calls: print("CALL", *x)
