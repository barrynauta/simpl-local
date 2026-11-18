"""Browser helpers for the @ui scenarios (Playwright for Python, sync API).

Only the rendered pages are used: elements are found by role, label and text.
One browser per behave run; one browser context (fresh cookies, so a fresh sign-in)
per scenario.
"""
import json
import os
import re
import time

from playwright.sync_api import sync_playwright

UI_URL = os.environ.get("UI_URL", "http://localhost:4433").rstrip("/")
HEADLESS = os.environ.get("UI_HEADLESS", "1") != "0"
UI_TIMEOUT_MS = int(os.environ.get("UI_TIMEOUT_MS", "10000"))

# Operation ids of the OpenAPI mapped to the request the UI sends for them.
# Each entry: (HTTP method, regex on the URL path after /v1).
OPERATIONS = {
    "getVocabularies": ("GET", r"^/vocabularies$"),
    "updateVocabularyStatus": ("PATCH", r"^/vocabularies/[^/]+$"),
    "createVocabularyVersion": ("POST", r"^/vocabularies/[^/]+/versions$"),
    "createVocabulary": ("POST", r"^/vocabularies$"),
}

# Problem bodies for stubbed answers: the OpenAPI examples (404, 500), D44 (503) and
# the body the running API returns for 403.
PROBLEMS = {
    403: {"type": "urn:problem-type:simpl:forbidden", "title": "Forbidden", "status": 403,
          "detail": "The caller does not have the role required for this operation."},
    404: {"type": "urn:problem-type:belgif:resourceNotFound", "title": "Resource Not Found",
          "status": 404, "detail": "Vocabulary with name '{name}' was not found."},
    500: {"type": "urn:problem-type:belgif:internalServerError", "title": "Internal Server Error",
          "status": 500, "detail": "Internal Server Error"},
    503: {"type": "urn:problem-type:belgif:serviceUnavailable", "title": "Service Unavailable",
          "status": 503, "detail": "The vocabulary store is not available. Retry later."},
}

EMPTY_PAGE = {"page": 1, "totalPages": 0, "total": 0, "pageSize": 20,
              "self": "/vocabularies?page=1&pageSize=20",
              "first": "/vocabularies?page=1&pageSize=20", "items": []}


class Browser:
    """Process-wide browser, started on first use."""
    _pw = None
    _browser = None

    @classmethod
    def get(cls):
        if cls._browser is None:
            cls._pw = sync_playwright().start()
            cls._browser = cls._pw.chromium.launch(headless=HEADLESS)
        return cls._browser

    @classmethod
    def stop(cls):
        if cls._browser is not None:
            cls._browser.close()
            cls._pw.stop()
            cls._browser = None
            cls._pw = None


class UI:
    """Per-scenario browser state."""

    current_page = None   # used by poll(): sync Playwright only delivers events while it is called

    def __init__(self, world):
        self.w = world
        self.ctx = None
        self.page = None
        self.downloads = []
        self.stubs = []          # (method, path regex, status, body)
        self.stub_hits = []
        self.user = None

    # ---------- lifecycle ----------
    def ensure(self):
        if self.page is None:
            self.ctx = Browser.get().new_context(accept_downloads=True)
            self.ctx.set_default_timeout(UI_TIMEOUT_MS)
            self.ctx.route(re.compile(r".*/v1/.*"), self._route)
            self.page = self.ctx.new_page()
            self.page.on("download", lambda d: self.downloads.append(d))
            UI.current_page = self.page
        return self.page

    def close(self):
        if self.ctx is not None:
            try:
                self.ctx.close()
            except Exception:
                pass
        self.ctx = self.page = None
        UI.current_page = None

    # ---------- stubs ----------
    def stub(self, operation, status):
        variant = None
        if " with " in operation:
            operation, variant = operation.split(" with ", 1)
        if operation not in OPERATIONS:
            raise AssertionError("no stub mapping for operation %r" % operation)
        method, rx = OPERATIONS[operation]
        if status == 200 and variant == "no items":
            body, ctype = EMPTY_PAGE, "application/json"
        elif status in PROBLEMS:
            body, ctype = dict(PROBLEMS[status]), "application/problem+json"
        else:
            raise AssertionError("no stub body for %s %s" % (status, variant))
        self.stubs.append((method, re.compile(rx), status, body, ctype))
        self.ensure()

    def _route(self, route):
        req = route.request
        m = re.match(r"^[a-z]+://[^/]+/v1(/[^?#]*)", req.url)
        path = m.group(1) if m else ""
        for method, rx, status, body, ctype in self.stubs:
            if req.method == method and rx.match(path):
                b = dict(body)
                if "detail" in b and "{name}" in b["detail"]:
                    seg = path.split("/")
                    b["detail"] = b["detail"].replace("{name}", seg[2] if len(seg) > 2 else "")
                if "status" in b and isinstance(b.get("status"), int) and ctype.startswith("application/problem"):
                    b["instance"] = "urn:uuid:00000000-0000-0000-0000-000000000000"
                self.stub_hits.append((req.method, path, status))
                route.fulfill(status=status, content_type=ctype, body=json.dumps(b),
                              headers={"Retry-After": "5"} if status == 503 else {})
                return
        route.continue_()

    # ---------- navigation ----------
    def goto(self, path):
        page = self.ensure()
        page.goto(UI_URL + path)
        self.settle()

    def settle(self):
        try:
            self.page.wait_for_load_state("networkidle", timeout=UI_TIMEOUT_MS)
        except Exception:
            pass

    def sign_in(self, user):
        page = self.ensure()
        page.goto(UI_URL + "/vocabularies")
        page.locator("#username").wait_for()
        page.locator("#username").fill(self.w.cfg.users.get(user, user))
        page.locator("#password").fill(self.w.cfg.password)
        page.locator("#kc-login").click()
        page.wait_for_url(re.compile("^" + re.escape(UI_URL)))
        self.settle()
        self.user = user

    def path(self):
        m = re.match(r"^[a-z]+://[^/]+(/[^?#]*)", self.page.url)
        return m.group(1) if m else ""

    def on_list(self):
        return self.path() in ("/", "/vocabularies") and \
            self.page.get_by_role("searchbox", name="Name").count() > 0

    # ---------- the vocabulary list ----------
    def list_rows(self):
        return self.page.locator("table").first.locator("tbody tr")

    def row(self, alias):
        """The list row of a vocabulary. Opens the list and filters on the name if needed."""
        page = self.page
        name = self.w.name(alias)
        if not self.on_list():
            self.goto("/vocabularies")
        link = page.get_by_role("link", name=name, exact=True)
        row = page.get_by_role("row").filter(has=link)
        try:
            row.first.wait_for(timeout=2500)
        except Exception:
            box = page.get_by_role("searchbox", name="Name")
            box.fill(name)
            row.first.wait_for()
        return row.first

    def open_row_menu(self, alias):
        row = self.row(alias)
        menu = row.get_by_role("menu")
        if menu.count() == 0 or not menu.first.is_visible():
            row.get_by_role("button", name="Actions").click()
            menu.first.wait_for()
        return row

    def close_menus(self):
        try:
            self.page.keyboard.press("Escape")
        except Exception:
            pass

    def list_names(self):
        cells = self.list_rows()
        names = []
        for i in range(cells.count()):
            link = cells.nth(i).get_by_role("link")
            if link.count():
                names.append(link.first.inner_text().strip())
        return names

    # ---------- details page ----------
    def version_table(self):
        return self.page.get_by_role("table").filter(
            has=self.page.get_by_role("columnheader", name="Change log"))

    def version_row(self, v):
        return self.version_table().get_by_role("row").filter(
            has=self.page.get_by_role("button", name="View version %d" % v, exact=True)).first

    # ---------- waiting ----------
    @staticmethod
    def poll(fn, msg="", timeout=UI_TIMEOUT_MS / 1000.0, interval=0.25):
        """Repeat fn() -> (ok, observed) until ok; raise AssertionError with msg otherwise."""
        end = time.time() + timeout
        last = None
        while True:
            try:
                ok, last = fn()
                if ok:
                    return last
            except Exception as e:  # element detached while the page re-renders
                last = repr(e)
            if time.time() > end:
                raise AssertionError("%s\nobserved: %s" % (msg, last))
            page = UI.current_page
            if page is not None and not page.is_closed():
                page.wait_for_timeout(interval * 1000)
            else:
                time.sleep(interval)


# ---------- action labels ----------
# The specs name the vocabulary status action only as "the status-change action" that
# "offers only the other status" (SIMPL-18174, D4, D18). The scenarios call it "Deprecate"
# and "Activate"; these patterns also accept the label the build renders for the same action.
ACTION_ALIASES = {
    "Deprecate": r"^(Deprecate|Change status to Deprecated)$",
    "Activate": r"^(Activate|Change status to Active)$",
    # D35 asks for "a link back to the list" without fixing its label.
    "Back to the list": r"^Back to (the )?(vocabulary )?list$",
}


def action_name(label):
    if label in ACTION_ALIASES:
        return re.compile(ACTION_ALIASES[label])
    return re.compile("^" + re.escape(label) + "$")
