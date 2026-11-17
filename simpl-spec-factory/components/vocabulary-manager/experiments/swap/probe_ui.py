"""Open a UI, sign in if a Keycloak login page appears, dump visible buttons/links and API calls.
Usage: probe_ui.py <ui_url> [path]"""
import sys, re
from playwright.sync_api import sync_playwright
ui=sys.argv[1].rstrip('/'); path=sys.argv[2] if len(sys.argv)>2 else '/'
calls=[]
with sync_playwright() as p:
    b=p.chromium.launch(); c=b.new_context(); pg=c.new_page()
    pg.on("response", lambda r: calls.append((r.request.method, r.status, r.url)) if re.search(r'/(api|v1)/', r.url) else None)
    pg.goto(ui+path)
    pg.wait_for_load_state("networkidle")
    if pg.locator("#username").count():
        pg.fill("#username","ga-admin"); pg.fill("#password","password"); pg.click("#kc-login")
        pg.wait_for_load_state("networkidle")
    pg.wait_for_timeout(1500)
    print("URL", pg.url)
    print("BUTTONS", [t.strip() for t in pg.locator("button, a").all_inner_texts() if t.strip()][:60])
    print("TEXT", pg.inner_text("body")[:800].replace("\n"," | "))
    for x in calls: print("CALL", *x)
    b.close()
