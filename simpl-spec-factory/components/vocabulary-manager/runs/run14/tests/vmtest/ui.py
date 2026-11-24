"""Browser session for the @ui scenarios (step 60): Playwright for Python.

Every scenario gets a new browser context: no stored cookies, storage or login (no stored sessions).
The browser process is shared by the run; it starts on the first @ui scenario.
"""
import os

from . import config

UI_URL = config.env("UI_URL", "http://localhost:4433").rstrip("/")
HEADLESS = config.env("UI_HEADLESS", "1") != "0"
TIMEOUT_MS = int(config.env("UI_TIMEOUT_MS", "15000"))
AXE_REPORT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "reports", "axe")

_runtime = {"pw": None, "browser": None}


def browser():
    if _runtime["browser"] is None:
        from playwright.sync_api import sync_playwright
        _runtime["pw"] = sync_playwright().start()
        _runtime["browser"] = _runtime["pw"].chromium.launch(headless=HEADLESS)
    return _runtime["browser"]


def stop():
    if _runtime["browser"] is not None:
        _runtime["browser"].close()
        _runtime["pw"].stop()
        _runtime["browser"] = None
        _runtime["pw"] = None


class UiSession:
    """One fresh context and page per scenario, with a log of API requests and downloads."""

    def __init__(self, bypass_csp=False):
        # D123: axe runs with the browser's CSP bypass; other scenarios see the real CSP.
        self.context = browser().new_context(accept_downloads=True, bypass_csp=bypass_csp,
                                             storage_state=None, viewport={"width": 1400, "height": 1000})
        self.context.set_default_timeout(TIMEOUT_MS)
        self.page = self.context.new_page()
        self.requests = []
        self.downloads = []
        self.form_name = None
        self.page.on("request", lambda req: self.requests.append((req.method, req.url)))
        self.page.on("download", lambda d: self.downloads.append(d))

    def close(self):
        try:
            self.context.close()
        except Exception:  # noqa: BLE001
            pass
