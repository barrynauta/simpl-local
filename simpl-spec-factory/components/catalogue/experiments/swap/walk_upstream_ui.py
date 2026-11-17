"""Swap D helper: publish one resource description, then drive the UPSTREAM UI through a quick
search for it and its detail page, logging every API call the UI's server makes on its behalf
(seen as the browser's calls to the UI's /api routes).

    walk_upstream_ui.py <ui_url> <catalogue_url> [oidc|none]

oidc: publish with a token of provider-a from the build's Keycloak and put a consumer-a token in
the cookie "token" that the upstream UI forwards. none: the upstream stack, no tokens checked.
The description is the suite's DataOffering template, signed as the digest says (D22, D23).
"""
import re
import sys
import uuid
from pathlib import Path

import requests
from playwright.sync_api import sync_playwright

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tests" / "features"))
from support import rd as RD  # noqa: E402

ui, cat = sys.argv[1].rstrip("/"), sys.argv[2].rstrip("/")
auth = sys.argv[3] if len(sys.argv) > 3 else "oidc"
KC = "http://localhost:18080/realms/dataspace-a/protocol/openid-connect/token"


def token(user):
    r = requests.post(KC, data={"grant_type": "password", "client_id": "test-client",
                                "username": user, "password": "Passw0rd!"}, timeout=10)
    r.raise_for_status()
    return r.json()["access_token"]


def step(msg):
    print("STEP", msg)


run = "t" + uuid.uuid4().hex[:10]
desc = RD.ResourceDescription("walk", "DataOffering", "provider-a", run)
headers = {"Content-Type": "application/json"}
if auth == "oidc":
    headers["Authorization"] = "Bearer " + token("provider-a")
r = requests.post(f"{cat}/self-descriptions", json=desc.body(), headers=headers, timeout=30)
step(f"publish to {cat}: {r.status_code}")

calls = []
with sync_playwright() as p:
    b = p.chromium.launch()
    c = b.new_context()
    if auth == "oidc":
        c.add_cookies([{"name": "token", "value": token("consumer-a"), "url": ui}])
    pg = c.new_page()
    pg.set_default_timeout(10000)
    pg.on("response", lambda resp: calls.append((resp.request.method, resp.status, resp.url.replace(ui, "")))
          if "/api/" in resp.url else None)
    pg.goto(ui + "/")
    pg.wait_for_load_state("networkidle")
    box = pg.get_by_role("textbox").first
    box.fill(run)
    box.press("Enter")
    pg.wait_for_timeout(3000)
    text = pg.inner_text("body")
    step("results contain the description: " + str(run in text or "walk" in text.lower()))
    step("results page: " + re.sub(r"\s+", " ", text)[:400])
    link = pg.get_by_role("link").filter(has_text=re.compile(r"walk|" + run, re.I))
    if not link.count():
        link = pg.locator("a[href*='resourceDescriptions'], a[href*='details']")
    if link.count():
        link.first.click()
        pg.wait_for_load_state("networkidle")
        pg.wait_for_timeout(1500)
        step("detail at " + pg.url.replace(ui, "") + ": " + re.sub(r"\s+", " ", pg.inner_text("body"))[:400])
    else:
        step("no link to a detail page")
    b.close()

for x in calls:
    print("CALL", *x)
