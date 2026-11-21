"""Playwright steps for the @ui scenarios (step 60).

Rules (D75): elements are found by roles, decided labels and decided texts only. No test ids,
CSS classes or routes taken from the build. The only routes used are the ones the decisions fix
(D31, D74, D163) and the API base path /v1 (E2), which the route stubs match on.

Assumptions (also in COVERAGE.md):
- Sign-in uses the stock Keycloak login form of the decided identity provider (E1): the fields
  'Username or email' and 'Password' and the button 'Sign In'.
- An action named a "button" in a scenario may be a button or a link: the decisions name the
  action labels (D18, D60, D115) but not their roles.
- A menu item (D72) is found by role menuitem, or else by its exact visible text.
- A field error is shown with aria-invalid="true" on the field (WCAG technique ARIA21); the error
  text is checked through aria-describedby or aria-errormessage, or else as visible text.
- "no request was sent to the API" means no POST, PUT, PATCH or DELETE request to /v1.
- Every scenario gets a new browser context: no stored session, cookies or storage (brief).
- axe runs with the browser's CSP bypass (D123) and the WCAG 2.0 and 2.1 A and AA rule tags (Q6).
"""
import json
import re
import time
import urllib.parse

import parse
from behave import given, register_type, then, when

from vmtests import api, config
from vmtests.data import fixture_text, sized_turtle, substitute
from vmtests.http import send

try:  # The API suite runs without Playwright; only ./run.sh ui needs it.
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    sync_playwright = None


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(U=_quoted)

TIMEOUT = 10.0          # seconds a check waits for the UI to settle
SETTLE = 1.5            # seconds a negative check waits before it decides
UTC_DATE_RE = re.compile(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC")
UTC_DATE_FULL_RE = re.compile(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$")
WRITE_METHODS = {"POST", "PUT", "PATCH", "DELETE"}
SORT_MARKS = "▲▼↑↓⇅↕▴▾⌃⌄"
IDP_BASE = config.TOKEN_URL.split("/protocol/")[0]  # .../realms/simpl


# ---------------------------------------------------------------- browser life cycle

def _browser(context):
    store = context.ui_store  # set in environment.before_all
    if store.get("_ui_browser") is None:
        if sync_playwright is None:
            raise AssertionError("Playwright is not installed; run the UI scenarios with ./run.sh ui")
        pw = sync_playwright().start()
        store["_ui_pw"] = pw
        store["_ui_browser"] = pw.chromium.launch(headless=True)
    return store["_ui_browser"]


def _ui(context):
    """The scenario's browser context and page. Created on first use, always fresh."""
    if getattr(context, "ui_page", None) is None:
        browser = _browser(context)
        ctx = browser.new_context(accept_downloads=True, bypass_csp=True, storage_state=None)
        ctx.set_default_timeout(TIMEOUT * 1000)
        context.ui_context = ctx
        context.ui_requests = []
        context.ui_downloads = []
        context.ui_stub_requests = []
        page = ctx.new_page()
        page.on("request", lambda req: _record_request(context, req))
        page.on("download", lambda d: context.ui_downloads.append(d))
        context.ui_page = page
    _CURRENT["page"] = context.ui_page
    return context.ui_page


def _record_request(context, req):
    path = urllib.parse.urlparse(req.url).path
    if "/v1/" in path or path.endswith("/v1"):
        context.ui_requests.append((req.method, req.url))


# ---------------------------------------------------------------- helpers

_CURRENT = {"page": None}


def _sleep(seconds):
    """Wait while letting Playwright process its events (time.sleep would freeze page.url)."""
    page = _CURRENT["page"]
    if page is not None and not page.is_closed():
        page.wait_for_timeout(seconds * 1000)
    else:
        time.sleep(seconds)


def S(context, text):
    return substitute(text, context)


def _poll(check, timeout=TIMEOUT, interval=0.2):
    """Run check() until it returns a truthy value or raises no AssertionError. Return its value."""
    deadline = time.monotonic() + timeout
    last = None
    while True:
        try:
            value = check()
            if value or value is None:
                return value
            last = AssertionError("condition not met")
        except AssertionError as exc:
            last = exc
        except Exception as exc:  # noqa: BLE001  e.g. an element replaced while the UI re-renders
            last = AssertionError(f"{type(exc).__name__}: {str(exc)[:300]}")
        if time.monotonic() > deadline:
            raise last
        _sleep(interval)


def _hold(check, duration=SETTLE, interval=0.25):
    """check() must hold for the whole duration (negative checks)."""
    deadline = time.monotonic() + duration
    while True:
        check()
        if time.monotonic() > deadline:
            return
        _sleep(interval)


def _words(text):
    """Case-sensitive match of text as a whole phrase (not inside a longer word)."""
    return re.compile(r"(?<![\w])" + re.escape(text) + r"(?![\w])")


def _norm(text):
    text = (text or "").replace(" ", " ")
    text = text.translate({ord(c): None for c in SORT_MARKS})
    return re.sub(r"\s+", " ", text).strip()


def _visible(locator):
    return [locator.nth(i) for i in range(locator.count()) if locator.nth(i).is_visible()]


def _label_re(label):
    """A decided label, optionally followed by the required marker '*' (D141)."""
    return re.compile(r"^\s*" + re.escape(label) + r"\s*\*?\s*$")


def _field(page, label):
    return page.get_by_label(_label_re(label))


def _action(page, label):
    """A button or a link with that exact accessible name."""
    return page.get_by_role("button", name=label, exact=True).or_(
        page.get_by_role("link", name=label, exact=True))


def _wait_ui_loaded(page):
    try:
        page.wait_for_load_state("networkidle", timeout=TIMEOUT * 1000)
    except Exception:  # noqa: BLE001
        pass


def _path(page):
    return urllib.parse.urlparse(page.url).path.rstrip("/") or "/"


def _sign_in(context, user):
    page = _ui(context)
    page.goto(config.UI_URL + "/vocabularies")
    # A new browser context never has a session, so the UI always sends us to the login (D18).
    _poll(lambda: page.url.startswith(IDP_BASE) or (_ for _ in ()).throw(
        AssertionError(f"the UI did not send the browser to the identity provider: {page.url}")), timeout=20)
    page.get_by_label("Username or email").fill(config.USERS[user]["username"])
    page.get_by_label("Password", exact=True).fill(config.USER_PASSWORD)
    page.get_by_role("button", name="Sign In").click()
    _poll(lambda: page.url.startswith(config.UI_URL) or (_ for _ in ()).throw(
        AssertionError(f"sign-in did not return to the UI: {page.url}")), timeout=20)
    _wait_ui_loaded(page)
    context.ui_user = user


def _tables(page):
    return _visible(page.get_by_role("table"))


def _headers(table):
    return [_norm(h.inner_text(timeout=2000)) for h in _visible(table.get_by_role("columnheader"))]


def _body_rows(table):
    rows = []
    for row in _visible(table.get_by_role("row")):
        if row.get_by_role("cell").count() > 0:
            rows.append(row)
    return rows


def _first_cell(row):
    return _norm(row.get_by_role("cell").first.inner_text(timeout=2000))


def _table_with(page, header):
    for table in _tables(page):
        if header in _headers(table):
            return table
    raise AssertionError(f"no table with the column header {header!r}; tables: "
                         f"{[_headers(t) for t in _tables(page)]}")


def _main_table(page):
    tables = _tables(page)
    if not tables:
        raise AssertionError("no table on the page")
    return tables[0]


def _version_table(page):
    return _table_with(page, "Version")


def _row_of(table, key):
    for row in _body_rows(table):
        if _first_cell(row) == key:
            return row
    raise AssertionError(f"no row {key!r}; rows: {[_first_cell(r) for r in _body_rows(table)]}")


def _cell(table, key, column):
    headers = _headers(table)
    assert column in headers, f"no column {column!r} in {headers}"
    row = _row_of(table, key)
    cells = row.get_by_role("cell").or_(row.get_by_role("rowheader"))
    return _norm(cells.nth(headers.index(column)).inner_text())


def _table_for_row(page, key):
    for table in _tables(page):
        if any(_first_cell(r) == key for r in _body_rows(table)):
            return table
    raise AssertionError(f"no table has a row {key!r}")


def _bring_row_into_view(page, key, max_pages=60):
    """Find the row on the list. The store is never reset (E4), so a scenario that does not
    filter may have its row on a later page: follow 'Next page' (D115) until the row shows."""
    def has_row():
        return any(_first_cell(r) == key for t in _tables(page) for r in _body_rows(t))
    try:
        _poll(lambda: has_row() or (_ for _ in ()).throw(AssertionError("row not on this page")), timeout=3)
        return
    except AssertionError:
        pass
    for _ in range(max_pages):
        nxt = page.get_by_role("button", name="Next page", exact=True)
        if not _visible(nxt) or not nxt.first.is_enabled():
            return
        before = [_first_cell(r) for r in _body_rows(_main_table(page))]
        nxt.first.click()
        _poll(lambda: [_first_cell(r) for r in _body_rows(_main_table(page))] != before
              or (_ for _ in ()).throw(AssertionError("next page did not load")))
        if has_row():
            return


def _text_visible(page, text):
    return len(_visible(page.get_by_text(_words(text)))) > 0


def _name(context, alias):
    return context.names.get(alias)


def _api_get(context, path):
    return send("GET", S(context, path), token=api.admin_token())


def _form_name(context):
    """Name for the {name} placeholder of a fixture: the name typed into the form when it is a
    valid name (D37), else the scenario's Alpha name, so the file itself stays valid Turtle."""
    name = getattr(context, "form_name", None)
    if name and re.match(r"^[A-Z][A-Za-z0-9]{2,63}$", name):
        return name
    return context.names.get("Alpha")


# ---------------------------------------------------------------- given

@given('I am signed in to the UI as "{user:U}"')
def step_signed_in(context, user):
    _sign_in(context, user)


@given("I am not signed in to the UI")
def step_not_signed_in(context):
    _ui(context)  # a fresh context has no session


def _problem_body(status, detail):
    titles = {403: "Forbidden", 404: "Not Found", 409: "Conflict", 413: "Payload Too Large",
              500: "Internal Server Error", 503: "Service Unavailable"}
    return json.dumps({
        "type": "about:blank",
        "title": titles.get(status, "Error"),
        "status": status,
        "detail": detail,
    })


def _api_route_regex(context, path):
    return re.compile(r"^[^?#]*/v1" + re.escape(S(context, path)) + r"(\?.*)?$")


def _stub(context, method, path, status, detail=None):
    _ui(context)
    rx = _api_route_regex(context, path)
    detail = S(context, detail) if detail is not None else f"Stubbed status {status}."

    def handler(route, request):
        if request.method.upper() != method.upper():
            route.fallback()
            return
        headers = {"Content-Type": "application/problem+json"}
        if status == 503:
            headers["Retry-After"] = "5"
        route.fulfill(status=status, headers=headers, body=_problem_body(status, detail))

    context.ui_context.route(rx, handler)


@given('the API answers {method:w} "{path:U}" with the status {status:d}')
def step_stub(context, method, path, status):
    _stub(context, method, path, status)


@given('the API answers {method:w} "{path:U}" with the status {status:d} and the detail "{detail:U}"')
def step_stub_detail(context, method, path, status, detail):
    _stub(context, method, path, status, detail)


@given('the API answers GET "{path:U}" with a version list of total {total:d}')
def step_stub_version_list(context, path, total):
    """Answer the version list with `total` versions, built from the real first item (D35, D85)."""
    _ui(context)
    rx = _api_route_regex(context, path)

    def handler(route, request):
        if request.method.upper() != "GET":
            route.fallback()
            return
        query = urllib.parse.parse_qs(urllib.parse.urlparse(request.url).query)
        page_no = int((query.get("page") or ["1"])[0])
        size = int((query.get("pageSize") or ["20"])[0])
        context.ui_stub_requests.append({"page": page_no, "pageSize": size})
        # The real vocabulary has one version: take the wire format from its first page.
        parts = urllib.parse.urlparse(request.url)
        q = urllib.parse.parse_qs(parts.query)
        q["page"] = ["1"]
        first_url = urllib.parse.urlunparse(parts._replace(query=urllib.parse.urlencode(q, doseq=True)))
        real = route.fetch(url=first_url)
        body = real.json()
        template = body["items"][0]
        start = (page_no - 1) * size
        numbers = list(range(total - start, max(total - start - size, 0), -1))
        items = []
        for n in numbers:
            item = json.loads(json.dumps(template))
            item["version"] = n
            item["status"] = "ACTIVE" if n == total else "DEPRECATED"
            for key, value in list(item.items()):
                if isinstance(value, str) and re.search(r"/versions/\d+", value):
                    item[key] = re.sub(r"/versions/\d+", f"/versions/{n}", value)
            items.append(item)
        body["items"] = items
        body["page"] = page_no
        body["pageSize"] = size
        body["total"] = total
        body["totalPages"] = -(-total // size)
        route.fulfill(response=real, body=json.dumps(body),
                      headers={**real.headers, "content-type": "application/json"})

    context.ui_context.route(rx, handler)


# ---------------------------------------------------------------- when

@when('I open the UI page "{route:U}"')
def step_open(context, route):
    page = _ui(context)
    target = S(context, route)
    m = re.match(r"^/vocabularies/([^/]+)/versions/new$", target)
    if m:
        context.form_name = m.group(1)
    page.goto(config.UI_URL + target)
    _poll(lambda: page.url.startswith(config.UI_URL) or page.url.startswith(IDP_BASE), timeout=20)
    _wait_ui_loaded(page)


@when('I click the button "{label:U}"')
def step_click_button(context, label):
    page = _ui(context)
    _action(page, S(context, label)).first.click()
    _wait_ui_loaded(page)


@when('I click the link "{label:U}"')
def step_click_link(context, label):
    page = _ui(context)
    page.get_by_role("link", name=S(context, label), exact=True).first.click()
    _wait_ui_loaded(page)


@when('I open the actions menu "{menu:U}"')
def step_open_menu(context, menu):
    page = _ui(context)
    name = S(context, menu)
    if name.startswith("Actions for ") and not name.startswith("Actions for version "):
        _bring_row_into_view(page, name[len("Actions for "):])
    button = page.get_by_role("button", name=name, exact=True)
    _poll(lambda: button.count() == 1 or (_ for _ in ()).throw(
        AssertionError(f"expected one button {name!r}, found {button.count()}")))
    button.click()


@when('I choose the menu item "{item:U}"')
def step_menu_item(context, item):
    page = _ui(context)
    text = S(context, item)
    loc = page.get_by_role("menuitem", name=text, exact=True)
    if not _visible(loc):
        loc = page.get_by_text(text, exact=True)
    _poll(lambda: len(_visible(loc)) > 0 or (_ for _ in ()).throw(
        AssertionError(f"no visible menu item {text!r}")))
    _visible(loc)[0].click()
    _wait_ui_loaded(page)


@when('I fill in "{label:U}" with "{value:U}"')
def step_fill(context, label, value):
    page = _ui(context)
    text = S(context, value)
    if label == "Name":
        context.form_name = text
    _field(page, label).fill(text)
    _wait_ui_loaded(page)


@when('I clear the field "{label:U}"')
def step_clear(context, label):
    _field(_ui(context), label).fill("")


def _set_file(context, label, filename, content):
    page = _ui(context)
    _field(page, label).set_input_files(
        {"name": filename, "mimeType": "text/turtle", "buffer": content})
    _wait_ui_loaded(page)


@when('I choose the fixture file "{fixture:U}" for "{label:U}"')
def step_choose_fixture(context, fixture, label):
    context.current_name = _form_name(context)
    _set_file(context, label, fixture, fixture_text(fixture, context))


@when('I choose a valid Turtle file of {size:d} bytes for "{label:U}"')
def step_choose_sized(context, size, label):
    _set_file(context, label, "large.ttl", sized_turtle(_form_name(context), size))


@when('I select "{option:U}" in "{label:U}"')
def step_select(context, option, label):
    page = _ui(context)
    field = _field(page, label)
    tag = field.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        field.select_option(label=option)
    else:
        field.click()
        page.get_by_role("option", name=option, exact=True).click()
    _wait_ui_loaded(page)


def _column_header(page, header):
    for table in _tables(page):
        for h in _visible(table.get_by_role("columnheader")):
            if _norm(h.inner_text(timeout=2000)) == header:
                return h
    raise AssertionError(f"no column header {header!r}")


@when('I click the column header "{header:U}"')
def step_click_header(context, header):
    page = _ui(context)
    h = _column_header(page, header)
    button = h.get_by_role("button")
    (button.first if button.count() else h).click()
    _wait_ui_loaded(page)


@when("I open version {version:d} in the version list")
def step_open_version(context, version):
    page = _ui(context)

    def find():
        row = _row_of(_version_table(page), str(version))
        link = row.get_by_role("link", name=str(version), exact=True)
        assert link.count() > 0, f"row of version {version} has no link named {version}"
        return link.first

    _poll(find).click()
    _wait_ui_loaded(page)


_TAG_ACTIONS = """
() => {
  const sel = 'a[href], button, [role=button], [role=link]';
  const els = Array.from(document.querySelectorAll(sel)).filter(e => {
    if (e.disabled || e.getAttribute('aria-disabled') === 'true') return false;
    const r = e.getBoundingClientRect(); const s = getComputedStyle(e);
    return r.width > 0 && r.height > 0 && s.visibility !== 'hidden' && s.display !== 'none';
  });
  window.__uiActions = els; window.__uiReached = new Set();
  return els.map(e => (e.getAttribute('aria-label') || e.textContent || '').trim().slice(0, 60));
}
"""
_RECORD_FOCUS = """
() => { const i = window.__uiActions.indexOf(document.activeElement);
        if (i >= 0) window.__uiReached.add(i); return window.__uiReached.size; }
"""


@when("I press Tab until every action has had the focus")
def step_tab(context):
    page = _ui(context)
    _poll(lambda: len(_body_rows(_main_table(page))) > 0)
    names = page.evaluate(_TAG_ACTIONS)
    context.ui_action_names = names
    page.locator("body").focus()
    page.evaluate("() => document.activeElement && document.activeElement.blur()")
    reached = 0
    for _ in range(len(names) * 2 + 30):
        page.keyboard.press("Tab")
        reached = page.evaluate(_RECORD_FOCUS)
        if reached >= len(names):
            break
    context.ui_reached = page.evaluate("() => Array.from(window.__uiReached)")


# ---------------------------------------------------------------- then: texts and elements

@then('I see the heading "{text:U}"')
def step_see_heading(context, text):
    page = _ui(context)
    name = S(context, text)
    _poll(lambda: len(_visible(page.get_by_role("heading", name=name, exact=True))) > 0
          or (_ for _ in ()).throw(AssertionError(f"no heading {name!r}")))


@then('I see the text "{text:U}"')
def step_see_text(context, text):
    page = _ui(context)
    t = S(context, text)
    _poll(lambda: _text_visible(page, t) or (_ for _ in ()).throw(AssertionError(f"text {t!r} not visible")))


@then('I do not see the text "{text:U}"')
def step_not_see_text(context, text):
    page = _ui(context)
    t = S(context, text)

    def check():
        assert not _text_visible(page, t), f"text {t!r} is visible"
    _hold(check)


@then('I see the button "{label:U}"')
def step_see_button(context, label):
    page = _ui(context)
    name = S(context, label)
    _poll(lambda: len(_visible(_action(page, name))) > 0
          or (_ for _ in ()).throw(AssertionError(f"no button {name!r}")))


@then('I do not see the button "{label:U}"')
def step_not_see_button(context, label):
    page = _ui(context)
    _wait_ui_loaded(page)
    name = S(context, label)

    def check():
        assert not _visible(_action(page, name)), f"button {name!r} is visible"
    _hold(check)


@then('I see the field "{label:U}"')
def step_see_field(context, label):
    page = _ui(context)
    _poll(lambda: _field(page, label).count() > 0
          or (_ for _ in ()).throw(AssertionError(f"no field labelled {label!r}")))


@then('I do not see the field "{label:U}"')
def step_not_see_field(context, label):
    page = _ui(context)

    def check():
        assert not _visible(_field(page, label)), f"field {label!r} is visible"
    _hold(check)


_LABEL_TEXT = """
e => {
  const parts = [];
  if (e.labels) for (const l of e.labels) parts.push(l.textContent);
  const ids = (e.getAttribute('aria-labelledby') || '').split(/\\s+/).filter(Boolean);
  for (const id of ids) { const n = document.getElementById(id); if (n) parts.push(n.textContent); }
  return parts.join(' ');
}
"""


@then('the field "{label:U}" is required')
def step_required(context, label):
    field = _field(_ui(context), label).first
    _poll(lambda: field.count() > 0)
    aria = field.get_attribute("aria-required")
    assert aria == "true", f"field {label!r} has aria-required={aria!r} (D141 wants \"true\")"
    label_text = field.evaluate(_LABEL_TEXT)
    assert "*" in label_text, f"label of {label!r} has no '*' marker: {label_text!r} (D141)"


@then('the field "{label:U}" contains "{value:U}"')
def step_field_contains(context, label, value):
    field = _field(_ui(context), label).first
    want = S(context, value)
    _poll(lambda: field.input_value() == want
          or (_ for _ in ()).throw(AssertionError(f"field {label!r} holds {field.input_value()!r}, not {want!r}")))


@then('the field "{label:U}" is empty')
def step_field_empty(context, label):
    field = _field(_ui(context), label).first
    assert field.input_value() == "", f"field {label!r} holds {field.input_value()!r}"


@then('the field "{label:U}" shows the file "{text:U}"')
def step_field_file(context, label, text):
    page = _ui(context)
    _field(page, label).first.wait_for(state="attached")
    t = S(context, text)
    _poll(lambda: _text_visible(page, t) or (_ for _ in ()).throw(AssertionError(f"file text {t!r} not visible")))


def _button(page, label):
    loc = page.get_by_role("button", name=label, exact=True)
    _poll(lambda: loc.count() > 0 or (_ for _ in ()).throw(AssertionError(f"no button {label!r}")))
    return loc.first


@then('the button "{label:U}" is disabled')
def step_disabled(context, label):
    b = _button(_ui(context), S(context, label))
    _poll(lambda: (not b.is_enabled()) or b.get_attribute("aria-disabled") == "true"
          or (_ for _ in ()).throw(AssertionError(f"button {label!r} is enabled")))


@then('the button "{label:U}" is enabled')
def step_enabled(context, label):
    b = _button(_ui(context), S(context, label))
    _poll(lambda: (b.is_enabled() and b.get_attribute("aria-disabled") != "true")
          or (_ for _ in ()).throw(AssertionError(f"button {label!r} is disabled")))


def _role_texts(page, role):
    return [_norm(x.inner_text()) for x in _visible(page.get_by_role(role))]


@then('I see an alert containing "{text:U}"')
def step_alert(context, text):
    page = _ui(context)
    t = S(context, text)
    _poll(lambda: any(t in a for a in _role_texts(page, "alert"))
          or (_ for _ in ()).throw(AssertionError(f"no alert with {t!r}; alerts: {_role_texts(page, 'alert')}")))


@then("I see exactly one alert")
def step_one_alert(context):
    page = _ui(context)
    _poll(lambda: len(_role_texts(page, "alert")) > 0 or (_ for _ in ()).throw(AssertionError("no alert")))
    _sleep(SETTLE)
    alerts = [a for a in _role_texts(page, "alert") if a]
    assert len(alerts) == 1, f"expected one alert, found {len(alerts)}: {alerts}"


@then('I see a status message containing "{text:U}"')
def step_status(context, text):
    page = _ui(context)
    t = S(context, text)
    _poll(lambda: any(t in a for a in _role_texts(page, "status"))
          or (_ for _ in ()).throw(AssertionError(f"no status with {t!r}; status: {_role_texts(page, 'status')}")))


_ERROR_TEXT = """
e => {
  const ids = ((e.getAttribute('aria-describedby') || '') + ' ' + (e.getAttribute('aria-errormessage') || ''))
    .split(/\\s+/).filter(Boolean);
  return ids.map(id => { const n = document.getElementById(id); return n ? n.textContent : ''; }).join(' ');
}
"""


def _field_error(page, label):
    field = _field(page, label).first
    invalid = field.get_attribute("aria-invalid") == "true"
    return invalid, _norm(field.evaluate(_ERROR_TEXT))


@then('the field "{label:U}" shows an error')
def step_field_error(context, label):
    page = _ui(context)

    def check():
        invalid, text = _field_error(page, label)
        assert invalid, f"field {label!r} is not marked aria-invalid; described text {text!r}"
    _poll(check)


@then('the field "{label:U}" shows the error "{text:U}"')
def step_field_error_text(context, label, text):
    page = _ui(context)
    t = S(context, text)

    def check():
        invalid, described = _field_error(page, label)
        assert invalid, f"field {label!r} is not marked aria-invalid"
        assert t in described or _text_visible(page, t), f"error {t!r} not shown; described text {described!r}"
    _poll(check)


@then('the focus is on the field "{label:U}"')
def step_focus(context, label):
    page = _ui(context)
    field = _field(page, label).first
    _poll(lambda: field.evaluate("e => e === document.activeElement")
          or (_ for _ in ()).throw(AssertionError(f"focus is not on {label!r}")))


def _region(page, name):
    return page.get_by_role("region", name=name, exact=True)


@then('the region "{region:U}" contains "{text:U}"')
def step_region_contains(context, region, text):
    page = _ui(context)
    name, t = S(context, region), S(context, text)

    def check():
        regs = _visible(_region(page, name))
        assert regs, f"no region {name!r}"
        content = _norm(regs[0].inner_text())
        assert t in content, f"region {name!r} does not contain {t!r}: {content[:400]!r}"
    _poll(check)


@then('the region "{region:U}" does not contain "{text:U}"')
def step_region_not_contains(context, region, text):
    page = _ui(context)
    name, t = S(context, region), S(context, text)
    _poll(lambda: len(_visible(_region(page, name))) > 0
          or (_ for _ in ()).throw(AssertionError(f"no region {name!r}")))

    def check():
        content = _norm(_visible(_region(page, name))[0].inner_text())
        assert t not in content, f"region {name!r} contains {t!r}"
    _hold(check)


@then('the region "{region:U}" lists {count:d} issues')
def step_region_issues(context, region, count):
    page = _ui(context)

    def check():
        regs = _visible(_region(page, region))
        assert regs, f"no region {region!r}"
        n = len(_visible(regs[0].get_by_role("listitem")))
        assert n == count, f"region {region!r} lists {n} issues, not {count}"
    _poll(check)


@then('I do not see the region "{region:U}"')
def step_no_region(context, region):
    page = _ui(context)

    def check():
        assert not _visible(_region(page, region)), f"region {region!r} is visible"
    _hold(check)


@then('the region "{region:U}" contains a UTC date')
def step_region_date(context, region):
    page = _ui(context)
    name = S(context, region)

    def check():
        regs = _visible(_region(page, name))
        assert regs, f"no region {name!r}"
        assert UTC_DATE_RE.search(regs[0].inner_text()), f"region {name!r} has no 'YYYY-MM-DD HH:mm UTC' date"
    _poll(check)


# ---------------------------------------------------------------- then: tables

@then('the table has the column headers "{headers:U}"')
def step_headers(context, headers):
    page = _ui(context)
    want = [h.strip() for h in headers.split(",")]

    def check():
        found = [_headers(t) for t in _tables(page)]
        assert want in found, f"no table with headers {want}; found {found}"
    _poll(check)


@then('the row of "{row:U}" shows "{text:U}" in the column "{column:U}"')
def step_row_cell(context, row, text, column):
    page = _ui(context)
    key, t = S(context, row), S(context, text)
    _bring_row_into_view(page, key)

    def check():
        value = _cell(_table_for_row(page, key), key, column)
        assert t in value, f"row {key!r} column {column!r} shows {value!r}, not {t!r}"
    _poll(check)


@then('the row of "{row:U}" shows a UTC date in the column "{column:U}"')
def step_row_date(context, row, column):
    page = _ui(context)
    key = S(context, row)
    _bring_row_into_view(page, key)

    def check():
        value = _cell(_table_for_row(page, key), key, column)
        assert UTC_DATE_FULL_RE.match(value), f"row {key!r} column {column!r} shows {value!r}"
    _poll(check)


@then('the rows are ordered "{names:U}"')
def step_rows_ordered(context, names):
    page = _ui(context)
    want = [S(context, n.strip()) for n in names.split(",")]

    def check():
        got = [_first_cell(r) for r in _body_rows(_main_table(page))]
        assert got == want, f"rows are {got}, expected {want}"
    _poll(check)


@then('the column header "{header:U}" has aria-sort "{value:U}"')
def step_aria_sort(context, header, value):
    page = _ui(context)

    def check():
        got = _column_header(page, header).get_attribute("aria-sort")
        assert got == value, f"column {header!r} has aria-sort {got!r}, not {value!r}"
    _poll(check)


@then("the list shows {count:d} rows")
def step_list_rows(context, count):
    page = _ui(context)

    def check():
        n = len(_body_rows(_main_table(page)))
        assert n == count, f"the list shows {n} rows, not {count}"
    _poll(check)


@then("the version list shows {count:d} rows")
def step_version_rows(context, count):
    page = _ui(context)

    def check():
        n = len(_body_rows(_version_table(page)))
        assert n == count, f"the version list shows {n} rows, not {count}"
    _poll(check, timeout=20)


@then("the version list requested page {page:d} with pageSize 100")
def step_version_paged(context, page):
    _poll(lambda: {"page": page, "pageSize": 100} in context.ui_stub_requests
          or (_ for _ in ()).throw(AssertionError(f"version list requests: {context.ui_stub_requests}")))


@then('the status icon of "{row:U}" has the accessible name "{text:U}"')
def step_icon(context, row, text):
    page = _ui(context)
    key, t = S(context, row), S(context, text)
    _bring_row_into_view(page, key)

    def check():
        r = _row_of(_table_for_row(page, key), key)
        loc = r.get_by_role("img", name=t, exact=True).or_(r.get_by_label(t, exact=True)).or_(
            r.get_by_title(t, exact=True))
        assert loc.count() > 0, f"row {key!r} has no element named {t!r}"
    _poll(check)


@then("the row of version {version:d} has aria-current \"true\"")
def step_aria_current(context, version):
    page = _ui(context)

    def check():
        r = _row_of(_version_table(page), str(version))
        got = r.get_attribute("aria-current")
        assert got == "true", f"row of version {version} has aria-current {got!r}"
    _poll(check)


# ---------------------------------------------------------------- then: navigation

@then('I am on the UI page "{route:U}"')
def step_on_page(context, route):
    page = _ui(context)
    want = S(context, route)
    _poll(lambda: _path(page) == want or (_ for _ in ()).throw(AssertionError(f"on {_path(page)}, not {want}")))


@then('I am on the UI page "{route:U}" or "{other:U}"')
def step_on_page_or(context, route, other):
    page = _ui(context)
    want = {S(context, route), S(context, other)}
    _poll(lambda: _path(page) in want or (_ for _ in ()).throw(AssertionError(f"on {_path(page)}, not {want}")))


@then("I am on the identity provider login page")
def step_on_login(context):
    page = _ui(context)
    _poll(lambda: page.url.startswith(IDP_BASE) or (_ for _ in ()).throw(
        AssertionError(f"on {page.url}, not the identity provider {IDP_BASE}")), timeout=20)
    _poll(lambda: page.get_by_label("Password", exact=True).count() > 0)


@then('the link "{text:U}" points to "{route:U}"')
def step_link(context, text, route):
    page = _ui(context)
    name, want = S(context, text), S(context, route)

    def check():
        links = _visible(page.get_by_role("link", name=name, exact=True))
        assert links, f"no link {name!r}"
        href = urllib.parse.urlparse(urllib.parse.urljoin(page.url, links[0].get_attribute("href"))).path
        assert href.rstrip("/") == want, f"link {name!r} points to {href!r}, not {want!r}"
    _poll(check)


@then('I do not see the link "{text:U}"')
def step_no_link(context, text):
    page = _ui(context)
    name = S(context, text)

    def check():
        assert not _visible(page.get_by_role("link", name=name, exact=True)), f"link {name!r} is visible"
    _hold(check)


@then("no request was sent to the API")
def step_no_request(context):
    _sleep(SETTLE)
    writes = [r for r in context.ui_requests if r[0] in WRITE_METHODS]
    assert not writes, f"requests sent to the API: {writes}"


# ---------------------------------------------------------------- then: accessibility

@then("there are no axe violations on the page")
def step_axe(context):
    from axe_playwright_python.sync_playwright import Axe

    page = _ui(context)
    _wait_ui_loaded(page)
    _poll(lambda: len(_visible(page.get_by_role("heading"))) > 0)
    _sleep(0.5)
    results = Axe().run(page, options={"runOnly": {"type": "tag", "values": [
        "wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]}})
    violations = results.response.get("violations", [])
    context.axe_violations = violations
    if violations:
        lines = []
        for v in violations:
            targets = [n.get("target") for n in v.get("nodes", [])][:5]
            lines.append(f"{v['id']} ({v.get('impact')}): {v.get('help')} - {len(v.get('nodes', []))} nodes {targets}")
        raise AssertionError("axe violations (WCAG 2.1 AA):\n" + "\n".join(lines))


@then("every action had the focus")
def step_every_action(context):
    names = context.ui_action_names
    reached = set(context.ui_reached)
    missed = [names[i] for i in range(len(names)) if i not in reached]
    assert names, "no actions found on the page"
    assert not missed, f"{len(missed)} of {len(names)} actions never had the focus: {missed[:15]}"


# ---------------------------------------------------------------- then: downloads and API checks

@then('a file named "{filename:U}" is downloaded with the content of version {version:d} of "{alias:U}"')
def step_download(context, filename, version, alias):
    _ui(context)
    _poll(lambda: len(context.ui_downloads) > 0 or (_ for _ in ()).throw(AssertionError("no download started")))
    d = context.ui_downloads[-1]
    want_name = S(context, filename)
    assert d.suggested_filename == want_name, f"downloaded {d.suggested_filename!r}, not {want_name!r}"
    with open(d.path(), "rb") as fh:
        got = fh.read()
    name = _name(context, alias)
    want = context.uploaded.get((name, version))
    if want is None:
        resp = _api_get(context, f"/content/{name}/versions/{version}")
        api.expect(resp, 200, "download content")
        want = resp.content
    assert got == want, f"downloaded {len(got)} bytes differ from version {version} ({len(want)} bytes)"


@then('the vocabulary "{alias:U}" has the vocabulary status "{status:U}" in the API')
def step_api_status(context, alias, status):
    name = _name(context, alias)

    def check():
        got = api.expect(_api_get(context, f"/vocabularies/{name}"), 200, "get vocabulary").json()["status"]
        assert got == status, f"{name} has status {got}, not {status}"
    _poll(check)


@then('version {version:d} of "{alias:U}" exists in the API')
def step_api_version_exists(context, version, alias):
    name = _name(context, alias)
    _poll(lambda: api.expect(_api_get(context, f"/vocabularies/{name}/versions/{version}"), 200, "get version"))


@then('version {version:d} of "{alias:U}" does not exist in the API')
def step_api_version_missing(context, version, alias):
    name = _name(context, alias)
    _sleep(SETTLE)
    api.expect(_api_get(context, f"/vocabularies/{name}/versions/{version}"), 404, "get version")


@then('version {version:d} of "{alias:U}" has the same content as version {other:d} in the API')
def step_api_same_content(context, version, alias, other):
    name = _name(context, alias)
    a = api.expect(_api_get(context, f"/content/{name}/versions/{version}"), 200, "content").content
    b = api.expect(_api_get(context, f"/content/{name}/versions/{other}"), 200, "content").content
    assert a == b, f"version {version} content ({len(a)} bytes) differs from version {other} ({len(b)} bytes)"
