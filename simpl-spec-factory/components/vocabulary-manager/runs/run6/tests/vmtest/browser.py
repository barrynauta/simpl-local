"""Browser lifecycle for the @ui scenarios: one Chromium per run, one new context per scenario.

A new context has no cookies, no local storage and no stored session, so every scenario starts
signed out (step 60 rule: no stored browser sessions).
"""
import tempfile

TIMEOUT = 15.0


def start_browser(context):
    if getattr(context, "_pw", None):
        return
    from playwright.sync_api import sync_playwright
    context._pw = sync_playwright().start()
    context._browser = context._pw.chromium.launch(headless=True)


def stop_browser(context):
    if getattr(context, "_browser", None):
        context._browser.close()
    if getattr(context, "_pw", None):
        context._pw.stop()


def open_ui(context):
    """A new browser context per scenario, with no stored session (fresh cookies and storage)."""
    start_browser(context)
    tmp = tempfile.TemporaryDirectory(prefix="vm-ui-")
    bctx = context._browser.new_context(accept_downloads=True, storage_state=None,
                                        viewport={"width": 1400, "height": 900})
    bctx.set_default_timeout(TIMEOUT * 1000)
    context.ui = {"context": bctx, "page": None, "stubs": [], "tmp": tmp, "user": None}


def close_ui(context):
    state = getattr(context, "ui", None)
    if not state:
        return
    try:
        state["context"].close()
    finally:
        state["tmp"].cleanup()
        context.ui = None


