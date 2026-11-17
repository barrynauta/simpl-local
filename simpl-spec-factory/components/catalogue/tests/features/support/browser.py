"""Browser lifecycle for the @ui scenarios: Playwright sync API, headless Chromium.

One Chromium per run, one fresh browser context (cookies, storage) per scenario. The page is
opened lazily by the first step that needs it, so that "the browser request to ... fails"
(route interception) is in place before the UI loads. Every request of the page is recorded
for the "the browser requested ..." steps. Configuration: see features/environment.py.
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
    """Create the browser context of a @ui scenario (no page yet)."""
    cfg = context.cfg
    bctx = _browser(cfg).new_context(accept_downloads=True, viewport={"width": 1400, "height": 1000})
    bctx.set_default_timeout(cfg.ui_timeout_ms)
    context.ui = {"context": bctx, "page": None, "user": None, "requests": [], "failing": []}

    def record(request):
        try:
            body = request.post_data
        except Exception:  # binary bodies
            body = None
        context.ui["requests"].append({"method": request.method, "url": request.url, "body": body})
    bctx.on("request", record)


def stop(context, scenario=None):
    ui = getattr(context, "ui", None)
    if not ui:
        return
    shot_dir = context.cfg.ui_screenshot_dir
    if shot_dir and scenario is not None and scenario.status == "failed" and ui.get("page") is not None:
        try:
            os.makedirs(shot_dir, exist_ok=True)
            name = re.sub(r"[^A-Za-z0-9]+", "_", scenario.name)[:80]
            ui["page"].screenshot(path=os.path.join(shot_dir, f"{name}.png"), full_page=True)
        except Exception:
            pass
    try:
        ui["context"].close()
    except Exception:
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


def problem_body(status=500):
    return json.dumps({
        "type": "urn:problem-type:simpl:internalServerError",
        "title": "Internal Server Error",
        "status": status,
        "detail": "Failure injected by the UI test (page.route).",
    })


def fail_requests(context, path):
    """Fault injection: every browser request whose URL path ends with `path` gets a 500 Problem."""
    def matches(url):
        return urlparse(url).path.rstrip("/").endswith(path.rstrip("/"))

    def handler(route):
        route.fulfill(status=500, content_type="application/problem+json", body=problem_body(500))
    context.ui["context"].route(matches, handler)
    context.ui["failing"].append(path)


def settle(page, timeout_ms=None):
    """Wait until the page has no network traffic for a moment (SPA fetches done)."""
    try:
        page.wait_for_load_state("networkidle", timeout=timeout_ms or 10000)
    except Exception:
        pass


def login_if_needed(context, page):
    """UI_AUTH=oidc: sign in on the identity provider's login page with generic locators."""
    cfg = context.cfg
    if cfg.ui_auth != "oidc":
        return
    user = context.ui["user"]
    password_box = page.get_by_label(re.compile(r"^\s*password\s*$", re.I))
    try:
        password_box.first.wait_for(state="visible", timeout=cfg.ui_timeout_ms)
    except Exception as exc:
        raise AssertionError(f"UI_AUTH=oidc but no login page with a password field appeared; "
                             f"page is {page.url}") from exc
    page.get_by_label(re.compile(r"user\s*name|e-?mail|^\s*user\s*$", re.I)).first.fill(user)
    password_box.first.fill(cfg.user_password)
    page.get_by_role("button", name=re.compile(r"sign\s*in|log\s*in|login", re.I)).first.click()
    ui_origin = urlparse(cfg.services["ui-a"]).netloc
    page.wait_for_url(lambda u: urlparse(u).netloc == ui_origin, timeout=cfg.ui_timeout_ms)


def page(context):
    """The page of the scenario: opened, signed in and settled on first use."""
    ui = context.ui
    if ui["page"] is None:
        assert ui["user"] is not None, "test bug: no 'I am logged in to the UI as ...' step"
        pg = ui["context"].new_page()
        ui["page"] = pg
        pg.goto(context.cfg.services["ui-a"] + "/")
        settle(pg)
        login_if_needed(context, pg)
        settle(pg)
    return ui["page"]


def goto(context, url):
    """Navigate to a URL of the UI (detail page), signing in first if needed."""
    pg = page(context)
    pg.goto(url)
    settle(pg)
    if context.cfg.ui_auth == "oidc" and urlparse(pg.url).netloc != urlparse(context.cfg.services["ui-a"]).netloc:
        login_if_needed(context, pg)
        settle(pg)
    return pg
