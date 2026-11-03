"""Browser lifecycle for the @ui scenarios: Playwright sync API, headless Chromium.

One Chromium per run, one fresh browser context (cookies, storage) per login, so a
scenario can sign in at one party and then at the other. Every request of the
page is recorded, and every dialog (alert, confirm) is recorded and accepted.
Configuration: see features/environment.py.

Black box: only what a user sees (DOM, accessibility tree, URL) and the browser's
own traffic. The sign-in uses generic login-page locators, not Keycloak markup.
"""
import json
import os
import re
from urllib.parse import urlparse

_PW = {"playwright": None, "browser": None}


def _browser(cfg):
    if _PW["browser"] is None:
        from playwright.sync_api import sync_playwright
        _PW["playwright"] = sync_playwright().start()
        _PW["browser"] = _PW["playwright"].chromium.launch(headless=cfg.ui_headless)
    return _PW["browser"]


def start(context):
    """State of a @ui scenario. Browser contexts are opened by the login steps."""
    context.ui = {"sessions": [], "current": None, "failing": []}


def _new_session(context, party, user):
    cfg = context.config_cm
    bctx = _browser(cfg).new_context(viewport={"width": 1400, "height": 1000})
    bctx.set_default_timeout(cfg.ui_timeout_ms)
    session = {"party": party, "user": user, "context": bctx, "page": None,
               "requests": [], "dialogs": []}

    def record(request):
        try:
            body = request.post_data
        except Exception:  # noqa: BLE001  (binary bodies)
            body = None
        session["requests"].append({"method": request.method, "url": request.url,
                                    "body": body})
    bctx.on("request", record)
    for path in context.ui["failing"]:
        _route_failure(bctx, path)
    context.ui["sessions"].append(session)
    context.ui["current"] = session
    return session


def stop(context, scenario=None):
    ui = getattr(context, "ui", None)
    if not ui:
        return
    shot_dir = context.config_cm.ui_screenshot_dir
    current = ui.get("current")
    if shot_dir and scenario is not None and scenario.status == "failed" \
            and current is not None and current.get("page") is not None:
        try:
            os.makedirs(shot_dir, exist_ok=True)
            name = re.sub(r"[^A-Za-z0-9]+", "_", scenario.name)[:90]
            current["page"].screenshot(path=os.path.join(shot_dir, "%s.png" % name),
                                       full_page=True)
        except Exception:  # noqa: BLE001
            pass
    for session in ui["sessions"]:
        try:
            session["context"].close()
        except Exception:  # noqa: BLE001
            pass
    context.ui = None


def shutdown():
    if _PW["browser"] is not None:
        try:
            _PW["browser"].close()
        finally:
            _PW["playwright"].stop()
            _PW["browser"] = None
            _PW["playwright"] = None


# ---------------------------------------------------------------- fault injection

def problem_body(status=500):
    return json.dumps({
        "type": "urn:problem-type:simpl:internalServerError",
        "title": "Internal Server Error",
        "status": status,
        "detail": "Failure injected by the UI test (page.route).",
    })


def _route_failure(bctx, path):
    def matches(url):
        return urlparse(url).path.rstrip("/").endswith(path.rstrip("/"))

    def handler(route):
        route.fulfill(status=500, content_type="application/problem+json",
                      body=problem_body(500))
    bctx.route(matches, handler)


def fail_requests(context, path):
    """Every browser request whose URL path ends with `path` gets a 500 Problem (D11)."""
    context.ui["failing"].append(path)
    for session in context.ui["sessions"]:
        _route_failure(session["context"], path)


# ---------------------------------------------------------------- navigation

def settle(page, timeout_ms=10000):
    """Wait until the page has no network traffic for a moment (SPA fetches done)."""
    try:
        page.wait_for_load_state("networkidle", timeout=timeout_ms)
    except Exception:  # noqa: BLE001
        pass


def _same_origin(url, base):
    return urlparse(url).netloc == urlparse(base).netloc


def _password(context, user):
    users = context.config_cm.users
    assert user in users, "test bug: unknown test user %s" % user
    return users[user][1]


def _sign_in(context, page, base):
    """UI_AUTH=oidc: sign in on the identity provider's login page, generic locators."""
    cfg = context.config_cm
    session = context.ui["current"]
    password_box = page.get_by_label(re.compile(r"^\s*password\s*$", re.I))
    try:
        password_box.first.wait_for(state="visible", timeout=cfg.ui_timeout_ms)
    except Exception as exc:
        raise AssertionError("UI_AUTH=oidc but no login page with a password field "
                             "appeared; page is %s" % page.url) from exc
    page.get_by_label(re.compile(r"user\s*name|e-?mail|^\s*user\s*$", re.I)).first.fill(
        session["user"])
    password_box.first.fill(_password(context, session["user"]))
    page.get_by_role("button", name=re.compile(r"sign\s*in|log\s*in|login", re.I)).first.click()
    try:
        page.wait_for_url(lambda u: _same_origin(u, base), timeout=cfg.ui_timeout_ms)
    except Exception as exc:
        raise AssertionError("sign-in as %s did not return to the UI; page is %s" % (
            session["user"], page.url)) from exc
    settle(page)


def login(context, party, user):
    """Open the UI of a party in a fresh browser context and sign in as user."""
    session = _new_session(context, party, user)
    page = session["context"].new_page()
    session["page"] = page
    page.on("dialog", lambda d: (session["dialogs"].append(d.message), d.accept()))
    base = base_url(context)
    page.goto(base + "/")
    settle(page)
    if context.config_cm.ui_auth == "oidc" and not _same_origin(page.url, base):
        _sign_in(context, page, base)
    return page


def current(context):
    ui = getattr(context, "ui", None)
    assert ui is not None, "test bug: browser not started (scenario not tagged @ui?)"
    assert ui["current"] is not None, "test bug: no 'I am logged in to the ... UI' step"
    return ui["current"]


def page(context):
    return current(context)["page"]


def base_url(context):
    return context.config_cm.party(current(context)["party"]).ui_url


def goto(context, url):
    """Navigate within the UI, signing in again if the UI sends us to the login page."""
    pg = page(context)
    base = base_url(context)
    pg.goto(url)
    settle(pg)
    if context.config_cm.ui_auth == "oidc" and not _same_origin(pg.url, base):
        _sign_in(context, pg, base)
        if pg.url.rstrip("/") != url.rstrip("/"):
            pg.goto(url)
            settle(pg)
    return pg
