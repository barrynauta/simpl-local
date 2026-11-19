"""Browser handling for the @ui scenarios (Playwright for Python).

One browser per run, one fresh browser context per scenario: no stored
sessions, no storage state, no user data directory. Every scenario logs in
through the Keycloak login page of decision E1.
"""
import json
import time
from urllib.parse import urlsplit

from . import config

_pw = None
_browser = None


def browser():
    global _pw, _browser
    if _browser is None:
        from playwright.sync_api import sync_playwright

        _pw = sync_playwright().start()
        _browser = _pw.chromium.launch(headless=config.UI_HEADLESS)
    return _browser


def new_context():
    # A new context holds no cookies and no local storage: a clean session.
    return browser().new_context(accept_downloads=True, locale="en-GB", timezone_id="UTC")


def close_scenario(context):
    ctx = getattr(context, "ui_ctx", None)
    if ctx is not None:
        try:
            ctx.close()
        except Exception:  # noqa: BLE001
            pass
        context.ui_ctx = None


def close_all():
    global _pw, _browser
    if _browser is not None:
        try:
            _browser.close()
        except Exception:  # noqa: BLE001
            pass
    if _pw is not None:
        try:
            _pw.stop()
        except Exception:  # noqa: BLE001
            pass
    _pw = _browser = None


def keycloak_origin():
    parts = urlsplit(config.TOKEN_URL)
    return "%s://%s" % (parts.scheme, parts.netloc)


def api_call_matches(request_url, request_method, method, path):
    """True when a browser request is the API call METHOD /v1<path> (E2 gives /v1)."""
    if request_method.upper() != method.upper():
        return False
    url_path = urlsplit(request_url).path.rstrip("/")
    return url_path.endswith("/v1" + path.rstrip("/"))


def problem_body(code, detail):
    types = {
        400: ("urn:problem-type:belgif:badRequest", "Bad Request"),
        403: ("urn:problem-type:simpl:forbidden", "Forbidden"),
        404: ("urn:problem-type:belgif:resourceNotFound", "Resource Not Found"),
        413: ("urn:problem-type:simpl:uploadTooLarge", "Payload Too Large"),
        500: ("urn:problem-type:belgif:internalServerError", "Internal Server Error"),
        503: ("urn:problem-type:belgif:serviceUnavailable", "Service Unavailable"),
    }
    ptype, title = types.get(code, ("about:blank", "Error"))
    return json.dumps({"type": ptype, "title": title, "status": code, "detail": detail, "instance": "/stub"})


def empty_list_body():
    return json.dumps({
        "page": 1, "pageSize": 20, "total": 0, "totalPages": 0, "items": [],
        "links": {"self": {"href": "/vocabularies?page=1&pageSize=20"}, "first": {"href": "/vocabularies?page=1&pageSize=20"}},
    })


def eventually(fn, timeout=10.0, interval=0.25):
    """Retry an assertion until it passes or the time runs out."""
    deadline = time.monotonic() + timeout
    while True:
        try:
            return fn()
        except AssertionError:
            if time.monotonic() >= deadline:
                raise
            time.sleep(interval)
