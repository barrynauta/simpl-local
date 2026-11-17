"""behave hooks: target configuration, tokens and per-scenario state.

Configuration (environment variables):
  API_URL         base URL of the API, including /v1      (default http://localhost:8086/v1)
  AUTH_MODE       "oidc" or "unsigned"                    (default oidc)
                  oidc:     password grant against TOKEN_URL (spec-digest D6)
                  unsigned: hand-made unsigned JWTs, for stacks that only decode the token
  TOKEN_URL       OIDC token endpoint                      (oidc mode)
  CLIENT_ID       OIDC client                              (default vocabulary-tests)
  VOCAB_BASE_IRI  namespace base for references, D18       (default https://vocabulary.simpl.local/vocabularies)

UI scenarios (tag @ui) also use:
  UI_URL          base URL of the Vocabulary Management UI (default http://localhost:4333)
  UI_AUTH         "oidc" or "none"                         (default oidc)
                  oidc: the UI redirects to the identity provider's login page; the steps sign in
                        there with the D6 users ga-admin / ga-viewer / ga-norole, password "password"
                  none: the UI has no sign-in (for example a stack without gateway); only "admin" works
  UI_HEADED       set to 1 to watch the browser                (default headless)
  UI_SCREENSHOTS  directory for a screenshot of every failed UI scenario (default: none taken)

The browser (Playwright, Chromium) starts at the first @ui scenario only, so API runs do not need it.
Every @ui scenario gets a fresh browser context: no cookies, no session, no downloads carried over.
"""
import base64
import json
import os
import random
import string

import requests

UI_TIMEOUT_MS = 10000

PERSONAS = {
    "admin": ("ga-admin", ["GA_VOCABULARY_ADMIN"]),
    "viewer": ("ga-viewer", ["GA_VOCABULARY_VIEWER"]),
    "norole": ("ga-norole", []),
}


def _b64(obj):
    return base64.urlsafe_b64encode(json.dumps(obj).encode()).rstrip(b"=").decode()


def _unsigned_token(user, roles):
    header = {"alg": "none", "typ": "JWT"}
    payload = {"sub": user, "email": f"{user}@simpl.local", "preferred_username": user, "roles": roles}
    return f"{_b64(header)}.{_b64(payload)}.unsigned"


def _oidc_token(cfg, user):
    r = requests.post(cfg["token_url"], data={
        "grant_type": "password", "client_id": cfg["client_id"],
        "username": user, "password": "password"}, timeout=10)
    r.raise_for_status()
    return r.json()["access_token"]


def before_all(context):
    context.cfg = {
        "api": os.environ.get("API_URL", "http://localhost:8086/v1").rstrip("/"),
        "auth_mode": os.environ.get("AUTH_MODE", "oidc"),
        "token_url": os.environ.get("TOKEN_URL", "http://localhost:8180/realms/simpl/protocol/openid-connect/token"),
        "client_id": os.environ.get("CLIENT_ID", "vocabulary-tests"),
        "base_iri": os.environ.get("VOCAB_BASE_IRI", "https://vocabulary.simpl.local/vocabularies").rstrip("/"),
    }
    context.fixtures = os.path.join(os.path.dirname(__file__), "..", "fixtures")
    context.tokens = {}

    def token(persona):
        if persona not in context.tokens:
            user, roles = PERSONAS[persona]
            if context.cfg["auth_mode"] == "unsigned":
                context.tokens[persona] = _unsigned_token(user, roles)
            else:
                context.tokens[persona] = _oidc_token(context.cfg, user)
        return context.tokens[persona]

    context.token = token

    context.cfg["ui"] = os.environ.get("UI_URL", "http://localhost:4333").rstrip("/")
    context.cfg["ui_auth"] = os.environ.get("UI_AUTH", "oidc")
    # A dict, not attributes: behave drops attributes set during a scenario when it ends.
    context.ui_runtime = {"playwright": None, "browser": None}


def _browser(context):
    # Started lazily: only UI scenarios pay for Chromium.
    rt = context.ui_runtime
    if rt["browser"] is None:
        from playwright.sync_api import sync_playwright
        rt["playwright"] = sync_playwright().start()
        rt["browser"] = rt["playwright"].chromium.launch(headless=os.environ.get("UI_HEADED") != "1")
    return rt["browser"]


def before_scenario(context, scenario):
    # Internal vocabularies cannot be deleted, so every scenario gets its own names.
    context.run_id = "Zr" + "".join(random.choices(string.ascii_lowercase + string.digits, k=6))
    context.names = {}
    context.persona = "admin"
    context.resp = None
    context.externals = set()
    context.page = None
    if "ui" in scenario.effective_tags:
        context.browser_context = _browser(context).new_context(accept_downloads=True, locale="en-GB")
        context.browser_context.set_default_timeout(UI_TIMEOUT_MS)
        context.page = context.browser_context.new_page()
        context.seen = []          # page texts seen during the scenario (toasts disappear)
        context.download = None
        context.ignore_text = []   # uploaded file names: not messages
        context.menu_toggle = None
        context.baseline = set()   # lines on screen before the last user action


def after_scenario(context, scenario):
    # External vocabularies can be deleted: remove what a failed scenario left behind.
    for n in list(context.externals):
        requests.delete(f"{context.cfg['api']}/external-vocabularies/{n}",
                        headers={"Authorization": "Bearer " + context.token("admin")}, timeout=30)
    if context.page is not None:
        shots = os.environ.get("UI_SCREENSHOTS")
        if shots and scenario.status == "failed":
            os.makedirs(shots, exist_ok=True)
            slug = "".join(c if c.isalnum() else "_" for c in scenario.name)[:80]
            try:
                context.page.screenshot(path=os.path.join(shots, slug + ".png"), full_page=True)
            except Exception:
                pass
        context.browser_context.close()
        context.page = None


def after_all(context):
    rt = context.ui_runtime
    if rt["browser"] is not None:
        rt["browser"].close()
        rt["playwright"].stop()
