"""Browser lifecycle and API stubs for the @ui scenarios (used by features/environment.py).

Every scenario gets a new browser context, so no cookies, storage or Keycloak session is reused.
"""
import json
import os
import re
from urllib.parse import urlsplit

WAIT = float(os.environ.get("UI_WAIT", "15"))


def path_of(url):
    return urlsplit(url).path.rstrip("/") or "/"


_pw = {"pw": None, "browser": None}


def start_scenario(context):
    from playwright.sync_api import sync_playwright
    if _pw["browser"] is None:
        _pw["pw"] = sync_playwright().start()
        _pw["browser"] = _pw["pw"].chromium.launch(headless=os.environ.get("UI_HEADED") != "1")
    # a fresh context per scenario: no stored cookies, storage or sessions
    ctx = _pw["browser"].new_context(bypass_csp=True, accept_downloads=True, locale="en-GB",
                                     viewport={"width": 1400, "height": 1000})
    ctx.set_default_timeout(WAIT * 1000)
    context.bctx = ctx
    context.page = ctx.new_page()
    context.stubs = []
    context.ui_requests = []
    context.downloads = []
    context.ui_upload = None
    ctx.on("request", lambda r: context.ui_requests.append((r.method, path_of(r.url), r.resource_type)))
    context.page.on("download", lambda d: context.downloads.append(d))
    ctx.route(re.compile(".*"), lambda route, request: _route(context, route, request))


def end_scenario(context, scenario):
    ctx = getattr(context, "bctx", None)
    if ctx is None:
        return
    if scenario.status == "failed" and getattr(context, "page", None) is not None:
        try:
            os.makedirs("reports/ui-screens", exist_ok=True)
            slug = re.sub(r"[^A-Za-z0-9]+", "-", scenario.name)[:80]
            context.page.screenshot(path=f"reports/ui-screens/{slug}-{context.sid}.png", full_page=True)
        except Exception:  # noqa: BLE001
            pass
    try:
        ctx.close()
    except Exception:  # noqa: BLE001
        pass
    context.bctx = None
    context.page = None


def stop_all():
    if _pw["browser"] is not None:
        _pw["browser"].close()
        _pw["pw"].stop()
        _pw["browser"] = None


def _route(context, route, request):
    if request.resource_type in ("fetch", "xhr"):
        p = path_of(request.url)
        for method, path, status, detail in context.stubs:
            if request.method == method and (p == path or p.endswith(path)):
                body = {"type": "about:blank", "status": status, "title": {
                    403: "Forbidden", 404: "Not Found", 500: "Internal Server Error", 503: "Service Unavailable"}.get(status, "Error")}
                if detail is not None:
                    body["detail"] = detail
                if status == 404 and detail is None:
                    body["detail"] = "Vocabulary not found"
                return route.fulfill(status=status, content_type="application/problem+json", body=json.dumps(body))
    return route.fallback()
