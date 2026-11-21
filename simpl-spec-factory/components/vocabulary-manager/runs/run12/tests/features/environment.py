import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "lib"))
sys.path.insert(0, os.path.join(HERE, "steps"))

import vmclient  # noqa: E402


def before_all(context):
    context.config.setup_logging()
    context.base_url = vmclient.API_URL


# one browser for the whole run; behave pops scenario-level context attributes,
# so the handles live at module level
_BROWSER = {}


def _start_browser(context):
    if "browser" not in _BROWSER:
        from playwright.sync_api import sync_playwright
        _BROWSER["pw"] = sync_playwright().start()
        headless = os.environ.get("UI_HEADED", "0") != "1"
        _BROWSER["browser"] = _BROWSER["pw"].chromium.launch(headless=headless)
    return _BROWSER["browser"]


def before_scenario(context, scenario):
    context.run = vmclient.run_id(10)
    context.vars = {}
    context.names = {}
    context.uploaded = {}
    context.token = None
    context.user = None
    context.extra_headers = {}
    context.response = None
    context.responses = []
    context.base_url = vmclient.API_URL
    if "store_down" in scenario.effective_tags:
        context.base_url = vmclient.STORE_DOWN_API_URL
    if "ui" in scenario.effective_tags and "manual" not in scenario.effective_tags:
        try:
            browser = _start_browser(context)
        except ImportError:
            scenario.skip("Playwright is not installed; run ./run.sh ui")
            return
        # A fresh context per scenario: no stored cookies, storage or sessions.
        # bypass_csp lets axe run under the CSP of D123.
        bctx = browser.new_context(accept_downloads=True, bypass_csp=True,
                                   storage_state=None, locale="en-GB",
                                   viewport={"width": 1366, "height": 900})
        context.ui_context = bctx
        context.ui_page = bctx.new_page()
        context.ui_requests = []
        context.ui_mark = 0
        context.ui_downloads = []
        context.ui_page.on("request", lambda r: context.ui_requests.append((r.method, r.url)))
        context.ui_page.on("download", lambda d: context.ui_downloads.append(d))


def after_scenario(context, scenario):
    bctx = getattr(context, "ui_context", None)
    if bctx is not None:
        if scenario.status == "failed":
            shots = os.path.join(os.path.dirname(HERE), "reports", "ui-screens")
            os.makedirs(shots, exist_ok=True)
            safe = "".join(c if c.isalnum() else "-" for c in scenario.name)[:80]
            try:
                context.ui_page.screenshot(path=os.path.join(shots, safe + ".png"), full_page=True)
            except Exception:
                pass
        try:
            bctx.close()
        except Exception:
            pass
        context.ui_context = None


def after_all(context):
    if "browser" in _BROWSER:
        _BROWSER["browser"].close()
        _BROWSER["pw"].stop()
