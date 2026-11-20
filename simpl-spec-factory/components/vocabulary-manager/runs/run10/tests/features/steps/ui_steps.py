"""Browser steps for the @ui scenarios (step 60), Playwright for Python.

Black box. Elements are found only by what the decisions give: visible texts, form labels (D60),
column headers (D23, D35, D100), filter labels and row actions (D72), the ARIA semantics of D75,
D90, D109, D114 and D115, and the routes of D31 and D74. Nothing is taken from the build's code,
test ids or generated names. Every scenario starts in a fresh browser context without stored
sessions; sign-in goes through the Keycloak login form of the realm (E1, D18).

Generic, undecided means used here, all plain user or ARIA behaviour:
  - a filter is applied by pressing Enter in a filter field;
  - a field error is the accessible description of a field (aria-describedby) or, failing that,
    the extra text in the field's own container;
  - a sortable column header holds a button or carries aria-sort;
  - a version row is selected by a click on its first cell.
"""

import json
import os
import re
import time
from pathlib import Path
from urllib.parse import urlparse

from behave import given, then, when

import vmclient
from vmclient import CONFIG

# Helpers of api_steps.py (loaded first, behave sorts the step modules by name).
render = vmclient.API_HELPERS["render"]
sub = vmclient.API_HELPERS["sub"]

UI_URL = os.environ.get("UI_URL", "http://localhost:4433").rstrip("/")
TIMEOUT_MS = int(float(os.environ.get("UI_TIMEOUT", "15")) * 1000)
HEADLESS = os.environ.get("UI_HEADED", "") in ("", "0", "false")
REPORTS = Path(__file__).resolve().parents[2] / "reports"
DATE_SHOWN = re.compile(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$")
DATE_ANY = re.compile(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC")
WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]
SORT_GLYPHS = re.compile(r"[\s\u25b2\u25bc\u25b4\u25be\u2191\u2193\u21c5\u2195]+$")
_CURRENT = {"page": None}  # page of the running scenario, so waits keep Playwright events flowing


# --------------------------------------------------------------------------- browser life cycle

def _browser(context):
    holder = context.pw_holder  # set in before_all, so it outlives every scenario
    if not holder:
        from playwright.sync_api import sync_playwright
        pw = sync_playwright().start()
        holder["pw"] = pw
        holder["browser"] = pw.chromium.launch(headless=HEADLESS)
    return holder["browser"]


def stop_browser(context):
    holder = getattr(context, "pw_holder", None)
    if holder:
        try:
            holder["browser"].close()
        finally:
            holder["pw"].stop()
        holder.clear()


def new_page(context):
    """A fresh context per scenario: no cookies, no local storage, no stored sign-in."""
    if getattr(context, "page", None) is not None:
        return context.page
    browser = _browser(context)
    bctx = browser.new_context(accept_downloads=True, storage_state=None,
                               viewport={"width": 1400, "height": 1000})
    bctx.set_default_timeout(TIMEOUT_MS)
    page = bctx.new_page()
    context.bctx = bctx
    context.page = page
    _CURRENT["page"] = page
    context.downloads = []
    context.api_requests = []    # (time, method, path)
    context.api_responses = []   # (method, path, status, body text)
    context.request_mark = None
    page.on("download", lambda d: context.downloads.append(d))

    def on_request(req):
        path = urlparse(req.url).path
        if "/v1/" in path:
            context.api_requests.append((time.time(), req.method, path))

    def on_response(resp):
        path = urlparse(resp.url).path
        if "/v1/" in path:
            try:
                text = resp.text()
            except Exception:  # noqa: BLE001 - body of a download or redirect
                text = ""
            context.api_responses.append((resp.request.method, path, resp.status, text))

    page.on("request", on_request)
    page.on("response", on_response)
    return page


def close_page(context, scenario):
    page = getattr(context, "page", None)
    if page is None:
        return
    try:
        if scenario.status == "failed":
            out = REPORTS / "ui-artifacts"
            out.mkdir(parents=True, exist_ok=True)
            safe = re.sub(r"[^A-Za-z0-9]+", "_", scenario.name)[:80]
            page.screenshot(path=str(out / f"{safe}.png"), full_page=True)
    except Exception:  # noqa: BLE001
        pass
    try:
        context.bctx.close()
    except Exception:  # noqa: BLE001
        pass
    context.page = None
    _CURRENT["page"] = None


def wait_until(fn, message, timeout=None):
    """Retry a check until it holds; the UI loads data asynchronously."""
    deadline = time.time() + (timeout or TIMEOUT_MS / 1000)
    last = None
    while True:
        try:
            result = fn()
            if result is None or result is True:
                return
            last = result
        except AssertionError as exc:
            last = str(exc)
        except Exception as exc:  # noqa: BLE001 - detached elements while the page re-renders
            last = repr(exc)
        if time.time() > deadline:
            raise AssertionError(f"{message}: {last}")
        page = _CURRENT["page"]
        if page is not None:
            page.wait_for_timeout(200)  # unlike time.sleep, lets Playwright deliver events
        else:
            time.sleep(0.2)


def settle(page):
    try:
        page.wait_for_load_state("networkidle", timeout=TIMEOUT_MS)
    except Exception:  # noqa: BLE001
        pass


def norm(text):
    return re.sub(r"\s+", " ", (text or "")).strip()


def split_list(text):
    return [t.strip() for t in text.split(",") if t.strip()]


# --------------------------------------------------------------------------- locators

def action(page, name):
    """A user action by its visible name: a button or a link (D18 calls both 'actions')."""
    return page.get_by_role("button", name=name, exact=True).or_(
        page.get_by_role("link", name=name, exact=True))


def field(page, label):
    return page.get_by_label(label, exact=True)


def tables_with(page, header):
    return page.get_by_role("table").filter(
        has=page.get_by_role("columnheader", name=header, exact=True))


def vocab_table(page):
    return tables_with(page, "Last updated").first


def version_table(page):
    return tables_with(page, "Change log").first


def headers(table):
    """Header texts without a sort direction glyph (an arrow is a usual indicator, not a name)."""
    return [SORT_GLYPHS.sub("", norm(h)) for h in table.get_by_role("columnheader").all_inner_texts()]


def body_rows(table):
    return table.get_by_role("row").filter(has=table.page.get_by_role("cell"))


def first_cells(table):
    texts = []
    for row in body_rows(table).all():
        cell = row.get_by_role("cell").first
        texts.append(norm(cell.inner_text()).split(" ")[0])
    return texts


def row_for(table, key):
    """The body row whose first cell starts with key (vocabulary name or version number)."""
    for row in body_rows(table).all():
        first = norm(row.get_by_role("cell").first.inner_text())
        if first == key or first.split(" ")[0] == key:
            return row
    return None


def cell(table, row, column):
    idx = headers(table).index(column)
    return norm(row.get_by_role("cell").nth(idx).inner_text())


def field_error(page, label):
    """Text that describes the field: aria-describedby targets, else its container's extra text."""
    loc = field(page, label).first
    return loc.evaluate(
        """(el) => {
            const ids = (el.getAttribute('aria-describedby') || '') + ' ' +
                        (el.getAttribute('aria-errormessage') || '');
            const parts = ids.split(/\\s+/).filter(Boolean)
                .map(id => document.getElementById(id))
                .filter(n => n && n.offsetParent !== null)
                .map(n => n.innerText.trim()).filter(Boolean);
            if (parts.length) return parts.join(' | ');
            const box = el.closest('div, fieldset, p, li') || el.parentElement;
            let text = box ? box.innerText : '';
            for (const l of (el.labels || [])) text = text.replace(l.innerText, '');
            return text.trim();
        }""")


def field_invalid(page, label):
    return field(page, label).first.get_attribute("aria-invalid") == "true"


def mark_requests(context):
    context.request_mark = time.time()


def requests_since_mark(context):
    mark = context.request_mark or 0
    return [(m, p) for (t, m, p) in context.api_requests if t >= mark]


def open_route(context, route):
    page = new_page(context)
    page.goto(UI_URL + sub(context, route))
    settle(page)


def apply_filter(page, label, value):
    target = field(page, label).first
    tag = target.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        target.select_option(label=value)
        # Apply with Enter in the name filter, as a user would after choosing.
        field(page, "Name contains").first.press("Enter")
    else:
        target.fill(value)
        target.press("Enter")
    settle(page)
    page.wait_for_timeout(300)
    settle(page)


def fixture_bytes(context, fixture):
    alias = getattr(context, "ui_alias", "A")
    return render(context, fixture, alias)


def choose_file(context, label, name, data):
    page = context.page
    target = field(page, label).first
    payload = {"name": name, "mimeType": "text/turtle", "buffer": data}
    if target.evaluate("e => e.tagName.toLowerCase() === 'input' && e.type === 'file'"):
        target.set_input_files(payload)
    else:
        with page.expect_file_chooser() as fc:
            target.click()
        fc.value.set_files(payload)
    settle(page)


def open_menu(page, menu):
    button = page.get_by_role("button", name=menu, exact=True)
    button.click()
    m = page.get_by_role("menu").filter(visible=True).first
    m.wait_for(state="visible")
    return m


def menu_items(page, menu):
    m = open_menu(page, menu)
    items = [norm(t) for t in m.get_by_role("menuitem").all_inner_texts()]
    page.keyboard.press("Escape")
    page.wait_for_timeout(150)
    return items


# --------------------------------------------------------------------------- sign-in and routes

@given('the user "{user:Q}" is signed in to the UI')
def step_signed_in(context, user):
    page = new_page(context)
    page.goto(UI_URL + "/")
    page.get_by_label("Username or email").fill(CONFIG.users.get(user, user))
    page.get_by_label("Password", exact=True).fill(CONFIG.password)
    page.get_by_role("button", name="Sign In").click()
    page.wait_for_url(lambda u: u.startswith(UI_URL), timeout=TIMEOUT_MS)
    settle(page)
    context.ui_user = user


@given("the user is not signed in to the UI")
def step_not_signed_in(context):
    new_page(context)


@given('the user\'s API answers "{method} {path:Q}" with {status:d} and detail "{detail:Q}"')
def step_stub(context, method, path, status, detail):
    page = new_page(context)
    target = "/v1" + sub(context, path)
    body = json.dumps({"type": "about:blank", "title": "Stubbed", "status": status,
                       "detail": sub(context, detail)})

    def handler(route):
        req = route.request
        if req.method == method and urlparse(req.url).path.endswith(target):
            route.fulfill(status=status, body=body, headers={"Content-Type": "application/problem+json"})
        else:
            route.fallback()

    page.route(re.compile(r".*/v1/.*"), handler)


@when('the user opens the route "{route:Q}"')
def step_open_route(context, route):
    m = re.search(r"/vocabularies/\$\{([A-Za-z]+)\}", route)
    if m:
        context.ui_alias = m.group(1)
    open_route(context, route)


@when('the user opens the menu item "{item:Q}"')
def step_open_menu_item(context, item):
    page = context.page
    page.get_by_role("link", name=item, exact=True).or_(
        page.get_by_role("menuitem", name=item, exact=True)).first.click()
    settle(page)


@then('the user is on the route "{route:Q}"')
def step_on_route(context, route):
    want = sub(context, route)
    wait_until(lambda: urlparse(context.page.url).path.rstrip("/") == want.rstrip("/") or
               f"at {context.page.url}", f"not on route {want}")


@then("the user is on the Keycloak login page")
def step_on_keycloak(context):
    realm = CONFIG.token_url.split("/protocol/")[0]
    page = context.page
    wait_until(lambda: page.url.startswith(realm) or f"at {page.url}", "not on the Keycloak page")
    page.get_by_label("Password", exact=True).wait_for(state="visible")


# --------------------------------------------------------------------------- generic visibility

@then('the user sees the text "{text:Q}"')
def step_sees_text(context, text):
    want = sub(context, text)
    context.page.get_by_text(want).first.wait_for(state="visible")


@then('the user sees the link "{name:Q}"')
def step_sees_link(context, name):
    context.page.get_by_role("link", name=sub(context, name), exact=True).first.wait_for(state="visible")


@then('the user sees the heading "{name:Q}"')
def step_sees_heading(context, name):
    context.page.get_by_role("heading", name=sub(context, name), exact=True).first.wait_for(state="visible")


@then('the user sees the button "{name:Q}"')
def step_sees_button(context, name):
    action(context.page, sub(context, name)).first.wait_for(state="visible")


@then('the user does not see the button "{name:Q}"')
def step_not_sees_button(context, name):
    page = context.page
    vocab_table(page).wait_for(state="visible")
    count = action(page, sub(context, name)).filter(visible=True).count()
    assert count == 0, f"'{name}' is visible"


@then('the user sees the buttons "{names:Q}"')
def step_sees_buttons(context, names):
    for name in split_list(names):
        context.page.get_by_role("button", name=name, exact=True).first.wait_for(state="visible")


@then('the user sees a region "{name:Q}"')
def step_sees_region(context, name):
    context.page.get_by_role("region", name=sub(context, name), exact=True).wait_for(state="visible")


@then('the user sees a status message "{text:Q}"')
def step_status_message(context, text):
    want = sub(context, text)
    context.page.get_by_role("status").filter(has_text=want).first.wait_for(state="visible")


@then('the user sees a status banner with the text "{text:Q}"')
def step_status_banner(context, text):
    step_status_message(context, text)


def _alerts(page):
    return page.get_by_role("alert").filter(visible=True)


@then('the user sees one alert with the text "{first:Q}" followed by "{second:Q}"')
def step_one_alert_followed(context, first, second):
    page = context.page
    a, b = sub(context, first), sub(context, second)

    def check():
        alerts = [norm(t) for t in _alerts(page).all_inner_texts()]
        alerts = [t for t in alerts if t]
        assert len(alerts) == 1, f"alerts: {alerts}"
        t = alerts[0]
        assert a in t and b in t[t.index(a) + len(a):], f"alert text: {t!r}"
    wait_until(check, "one alert with the decided text and the detail")


@then('the user sees one alert with the text "{text:Q}"')
def step_one_alert(context, text):
    page = context.page
    want = sub(context, text)

    def check():
        alerts = [t for t in (norm(x) for x in _alerts(page).all_inner_texts()) if t]
        assert len(alerts) == 1, f"alerts: {alerts}"
        assert want in alerts[0], f"alert text: {alerts[0]!r}"
    wait_until(check, f"one alert '{want}'")


@then('the user sees an alert with the text "{first:Q}" and "{second:Q}"')
def step_alert_and(context, first, second):
    page = context.page
    a, b = sub(context, first), sub(context, second)
    wait_until(lambda: any(a in t and b in t for t in (norm(x) for x in _alerts(page).all_inner_texts()))
               or f"alerts: {_alerts(page).all_inner_texts()}", "alert with both texts")


@then('the user sees an alert above the form with the text "{text:Q}"')
def step_alert_above(context, text):
    page = context.page
    want = sub(context, text)
    alert = _alerts(page).filter(has_text=want).first
    alert.wait_for(state="visible")
    top_alert = alert.bounding_box()["y"]
    top_field = field(page, "Name").first.bounding_box()["y"]
    assert top_alert < top_field, f"alert at y={top_alert} is not above the form (Name at y={top_field})"


# --------------------------------------------------------------------------- list

@then('the user sees a table with the column headers "{names:Q}"')
def step_table_headers(context, names):
    want = split_list(names)
    page = context.page
    table = tables_with(page, want[0]).filter(
        has=page.get_by_role("columnheader", name=want[-1], exact=True)).first
    wait_until(lambda: headers(table) == want or f"headers {headers(table)}", "column headers differ")


@when('the user filters the list with "{label:Q}" set to the date of the remembered "{key:Q}"')
@then('the user filters the list with "{label:Q}" set to the date of the remembered "{key:Q}"')
def step_filter_date(context, label, key):
    apply_filter(context.page, label, str(context.saved[key])[:10])


@when('the user filters the list with "{label:Q}" set to "{value:Q}"')
@then('the user filters the list with "{label:Q}" set to "{value:Q}"')
def step_filter(context, label, value):
    apply_filter(context.page, label, sub(context, value))


@then('the user sees a row for "{name:Q}" with status "{status:Q}", description "{desc:Q}", '
      'creator "{creator:Q}" and dates as "{fmt:Q}"')
def step_row_values(context, name, status, desc, creator, fmt):
    page = context.page
    table = vocab_table(page)
    key = sub(context, name)
    wait_until(lambda: row_for(table, key) is not None or "no row", f"no row for {key}")
    row = row_for(table, key)
    assert cell(table, row, "Status").startswith(status), cell(table, row, "Status")
    assert cell(table, row, "Description") == sub(context, desc), cell(table, row, "Description")
    assert cell(table, row, "Creator") == sub(context, creator), cell(table, row, "Creator")
    for col in ("Date created", "Last updated"):
        assert DATE_SHOWN.match(cell(table, row, col)), f"{col}: {cell(table, row, col)!r} is not {fmt}"


@then('the user sees only the rows "{names:Q}"')
def step_only_rows(context, names):
    want = [sub(context, n) for n in split_list(names)]
    table = vocab_table(context.page)
    wait_until(lambda: sorted(first_cells(table)) == sorted(want) or f"rows {first_cells(table)}",
               f"rows are not {want}")


@then('the rows are in the order "{names:Q}"')
def step_rows_order(context, names):
    want = [sub(context, n) for n in split_list(names)]
    table = vocab_table(context.page)
    wait_until(lambda: first_cells(table) == want or f"rows {first_cells(table)}", f"order is not {want}")


@then('the rows are sorted by "{column:Q}" {direction}')
def step_rows_sorted(context, column, direction):
    table = vocab_table(context.page)

    def check():
        idx = headers(table).index(column)
        values = [norm(r.get_by_role("cell").nth(idx).inner_text()) for r in body_rows(table).all()]
        assert len(values) >= 2, f"too few rows: {values}"
        want = sorted(values, reverse=(direction == "descending"))  # code point order (D58)
        assert values == want, f"values {values}"
    wait_until(check, f"rows not sorted by {column} {direction}")


@when('the user clicks the column header "{column:Q}"')
def step_click_header(context, column):
    page = context.page
    header = vocab_table(page).get_by_role("columnheader", name=column, exact=True)
    inner = header.get_by_role("button")
    (inner.first if inner.count() else header).click()
    settle(page)
    page.wait_for_timeout(300)


@then('the column headers "{names:Q}" are not sortable')
def step_not_sortable(context, names):
    table = vocab_table(context.page)
    table.wait_for(state="visible")
    for name in split_list(names):
        header = table.get_by_role("columnheader", name=name, exact=True)
        assert header.get_by_role("button").count() == 0, f"'{name}' holds a sort button"
        assert header.get_attribute("aria-sort") is None, f"'{name}' has aria-sort"


@then('the user sees the buttons "{names:Q}" and a text "{pattern:Q}"')
def step_paging(context, names, pattern):
    step_sees_buttons(context, names)
    rx = re.escape(pattern).replace(re.escape("<n>"), r"\d+").replace(re.escape("<m>"), r"\d+")
    context.page.get_by_text(re.compile(rx)).first.wait_for(state="visible")


@then("the user sees at most {count:d} rows")
def step_at_most(context, count):
    table = vocab_table(context.page)
    table.wait_for(state="visible")
    settle(context.page)
    n = body_rows(table).count()
    assert n <= count, f"{n} rows"


@then('the row "{name:Q}" has a warning icon named "{text:Q}"')
def step_warning_icon(context, name, text):
    table = vocab_table(context.page)
    key, want = sub(context, name), sub(context, text)
    wait_until(lambda: row_for(table, key) is not None or "no row", f"no row for {key}")
    row = row_for(table, key)
    icon = row.get_by_role("img", name=want, exact=True).or_(row.get_by_label(want, exact=True)).or_(
        row.get_by_title(want, exact=True))
    assert icon.count() >= 1, f"no element named '{want}' in the row"


@then('the row "{name:Q}" shows the status "{status:Q}"')
def step_row_status(context, name, status):
    table = vocab_table(context.page)
    key = sub(context, name)

    def check():
        row = row_for(table, key)
        assert row is not None, "no row"
        shown = cell(table, row, "Status")
        assert shown.startswith(status), f"status {shown!r}"
    wait_until(check, f"row {key} does not show {status}")


@when('the user opens the menu "{menu:Q}"')
def step_open_menu(context, menu):
    open_menu(context.page, sub(context, menu))


@then('the user sees the menu items "{names:Q}"')
def step_menu_items(context, names):
    m = context.page.get_by_role("menu").filter(visible=True).first
    items = [norm(t) for t in m.get_by_role("menuitem").all_inner_texts()]
    assert items == split_list(names), f"menu items {items}"


@when('the user chooses "{item:Q}" in the menu "{menu:Q}"')
@then('the user chooses "{item:Q}" in the menu "{menu:Q}"')
def step_choose(context, item, menu):
    page = context.page
    m = open_menu(page, sub(context, menu))
    mark_requests(context)
    m.get_by_role("menuitem", name=item, exact=True).click()
    settle(page)


@then('the menu "{menu:Q}" holds "{a:Q}" and "{b:Q}"')
def step_menu_holds(context, menu, a, b):
    items = menu_items(context.page, sub(context, menu))
    assert a in items and b in items, f"menu items {items}"


@then('the menu "{menu:Q}" holds neither "{a:Q}" nor "{b:Q}"')
def step_menu_not_holds(context, menu, a, b):
    items = menu_items(context.page, sub(context, menu))
    assert a not in items and b not in items, f"menu items {items}"


@then('the user receives the file "{name:Q}" with the content of version {version:d} of "{alias:Q}"')
def step_download(context, name, version, alias):
    wait_until(lambda: bool(context.downloads) or "no download", "no file received")
    d = context.downloads[-1]
    assert d.suggested_filename == sub(context, name), f"file name {d.suggested_filename!r}"
    data = Path(d.path()).read_bytes()
    want = context.files[alias][version]
    assert data == want, f"content differs from version {version} ({len(data)} vs {len(want)} bytes)"
    context.downloads.clear()


# --------------------------------------------------------------------------- details page

@then('the version rows are "{numbers:Q}"')
def step_version_rows(context, numbers):
    want = split_list(numbers)
    table = version_table(context.page)
    wait_until(lambda: first_cells(table) == want or f"rows {first_cells(table)}", f"version rows not {want}")


@then('the version rows are {count:d} rows from "{top:Q}" down to "{bottom:Q}"')
def step_version_rows_range(context, count, top, bottom):
    want = [str(n) for n in range(int(top), int(bottom) - 1, -1)]
    assert len(want) == count
    table = version_table(context.page)
    wait_until(lambda: first_cells(table) == want or f"{len(first_cells(table))} rows",
               f"version rows not {top}..{bottom}", timeout=max(TIMEOUT_MS / 1000, 30))


def _version_row(context, number):
    table = version_table(context.page)
    wait_until(lambda: row_for(table, number) is not None or "no row", f"no version row {number}")
    return table, row_for(table, number)


@then('the version row "{number:Q}" shows the status "{status:Q}" and the change log "{log:Q}"')
def step_version_status_log(context, number, status, log):
    table, row = _version_row(context, number)
    assert cell(table, row, "Status").startswith(status), cell(table, row, "Status")
    assert cell(table, row, "Change log") == log, cell(table, row, "Change log")


@then('the version row "{number:Q}" shows the status "{status:Q}"')
def step_version_status(context, number, status):
    def check():
        table, row = _version_row(context, number)
        shown = cell(table, row, "Status")
        assert shown.startswith(status), f"status {shown!r}"
    wait_until(check, f"version {number} does not show {status}")


@then('the version row "{number:Q}" shows the warning "{warning:Q}" and a link "{link:Q}"')
def step_version_warning_link(context, number, warning, link):
    table, row = _version_row(context, number)
    text = norm(row.inner_text())
    assert warning in text, f"row text {text!r}"
    links = row.get_by_role("link", name=link, exact=True)
    assert links.count() == 1, (f"no link named '{link}' in the row; links: "
                                f"{[norm(x) for x in row.get_by_role('link').all_inner_texts()]}")


@then('the version row "{number:Q}" shows the warning "{warning:Q}" and no "{line:Q}" line')
def step_version_warning_noline(context, number, warning, line):
    table, row = _version_row(context, number)
    text = norm(row.inner_text())
    assert warning in text, f"row text {text!r}"
    assert line not in text, f"row text {text!r}"


@then('the version row "{number:Q}" shows no deprecation warning')
def step_version_no_warning(context, number):
    table, row = _version_row(context, number)
    text = norm(row.inner_text())
    assert "This version is deprecated." not in text, f"row text {text!r}"


@when('the user selects the version row "{number:Q}"')
def step_select_version(context, number):
    table, row = _version_row(context, number)
    first = row.get_by_role("cell").first
    inner = first.get_by_role("button").or_(first.get_by_role("link"))
    (inner.first if inner.count() else first).click()
    settle(context.page)


@when('the user follows the link "{name:Q}"')
def step_follow_link(context, name):
    context.page.get_by_role("link", name=sub(context, name), exact=True).first.click()
    settle(context.page)


@then('the user sees a region "{region:Q}" with the version, status "{status:Q}", date created, '
      'creator "{creator:Q}", description "{desc:Q}", change log "{log:Q}" and a "{button:Q}" button')
def step_version_panel(context, region, status, creator, desc, log, button):
    page = context.page
    reg = page.get_by_role("region", name=region, exact=True)
    reg.wait_for(state="visible")
    text = norm(reg.inner_text())
    number = region.split(" ")[-1]
    for part in (number, status, sub(context, creator), sub(context, desc), log):
        assert part in text, f"'{part}' not in the panel: {text!r}"
    assert DATE_ANY.search(text), f"no date created in the panel: {text!r}"
    reg.get_by_role("button", name=button, exact=True).or_(
        reg.get_by_role("link", name=button, exact=True)).first.wait_for(state="visible")


# --------------------------------------------------------------------------- forms

@when('the user clicks the button "{name:Q}"')
def step_click(context, name):
    page = context.page
    mark_requests(context)
    action(page, sub(context, name)).first.click()
    settle(page)
    page.wait_for_timeout(300)


@then('the user sees the fields "{names:Q}"')
def step_fields(context, names):
    for name in split_list(names):
        field(context.page, name).first.wait_for(state="visible")


@then('the user sees no field "{a:Q}" and no field "{b:Q}"')
def step_no_fields(context, a, b):
    field(context.page, "Name").first.wait_for(state="visible")
    for name in (a, b):
        assert field(context.page, name).count() == 0, f"field '{name}' present"


@then('the user sees no field "{name:Q}"')
def step_no_field(context, name):
    field(context.page, "Change log").first.wait_for(state="visible")
    assert field(context.page, name).count() == 0, f"field '{name}' present"


@then('the user sees an error under "{a:Q}", "{b:Q}" and "{c:Q}"')
def step_errors_three(context, a, b, c):
    for name in (a, b, c):
        step_error_under(context, name)


@then('the user sees an error under "{name:Q}"')
def step_error_under(context, name):
    page = context.page
    wait_until(lambda: (field_error(page, name) != "" and field_invalid(page, name))
               or f"description {field_error(page, name)!r}, aria-invalid "
                  f"{field(page, name).first.get_attribute('aria-invalid')}",
               f"no error under '{name}'")


@then('the user sees the error "{text:Q}" under "{name:Q}"')
def step_error_text_under(context, text, name):
    page = context.page
    wait_until(lambda: text in field_error(page, name) or f"text under the field {field_error(page, name)!r}",
               f"'{text}' not under '{name}'")


@then("no request was sent to the API")
def step_no_request(context):
    context.page.wait_for_timeout(500)
    sent = requests_since_mark(context)
    assert not sent, f"requests sent: {sent}"


@then('the focus is on the field "{name:Q}"')
def step_focus(context, name):
    focused = field(context.page, name).first.evaluate("e => e === document.activeElement")
    assert focused, "focus is elsewhere: " + context.page.evaluate("document.activeElement.outerHTML")[:200]


@when('the user types "{text:Q}" at the end of the field "{name:Q}"')
def step_type_end(context, text, name):
    target = field(context.page, name).first
    target.click()
    context.page.keyboard.press("End" if target.evaluate("e => e.tagName") == "INPUT" else "Control+End")
    target.press_sequentially(sub(context, text))


@when('the user types "{text:Q}" in the field "{name:Q}"')
def step_type(context, text, name):
    field(context.page, name).first.fill(sub(context, text))


@when('the user leaves the field "{name:Q}"')
def step_leave(context, name):
    field(context.page, name).first.press("Tab")


@when('the user fills the creation form with name "{name:Q}", description "{desc:Q}" and the file "{fixture:Q}"')
def step_fill_create(context, name, desc, fixture):
    m = re.fullmatch(r"\$\{([A-Za-z]+)\}", name)
    context.ui_alias = m.group(1) if m else "A"
    page = context.page
    field(page, "Name").first.fill(sub(context, name))
    field(page, "Description").first.fill(sub(context, desc))
    choose_file(context, "Vocabulary file", "vocabulary.ttl", fixture_bytes(context, fixture))


@when('the user chooses the file "{fixture:Q}" for "{label:Q}"')
def step_choose_file(context, fixture, label):
    choose_file(context, label, "vocabulary.ttl", fixture_bytes(context, fixture))


@when('the user chooses a file of {size:d} bytes for "{label:Q}"')
def step_choose_size(context, size, label):
    line = b"# padding line for the size limit test\n"
    data = (line * (size // len(line) + 1))[:size]
    choose_file(context, label, "large.ttl", data)


@then('the button "{name:Q}" is disabled')
def step_disabled(context, name):
    button = context.page.get_by_role("button", name=name, exact=True).first
    wait_until(lambda: button.is_disabled() or "enabled", f"'{name}' is not disabled")


@then('the button "{name:Q}" is enabled')
def step_enabled(context, name):
    button = context.page.get_by_role("button", name=name, exact=True).first
    wait_until(lambda: button.is_enabled() or "disabled", f"'{name}' is not enabled")


@then("the user sees a syntax error that names line {line:d}")
def step_syntax_line(context, line):
    page = context.page
    rx = re.compile(rf"\bline {line}\b", re.IGNORECASE)
    wait_until(lambda: bool(rx.search(field_error(page, "Vocabulary file"))) or
               f"text under the field {field_error(page, 'Vocabulary file')!r}",
               f"no syntax error naming line {line}")


def _region(page, name):
    reg = page.get_by_role("region", name=name, exact=True)
    reg.wait_for(state="visible")
    return reg


@then('the user sees a region "{name:Q}" whose first line is an alert "{text:Q}"')
def step_region_first_alert(context, name, text):
    _region_first(context, name, "alert", text)


@then('the user sees a region "{name:Q}" whose first line is a status "{text:Q}"')
def step_region_first_status(context, name, text):
    _region_first(context, name, "status", text)


def _region_first(context, name, role, text):
    reg = _region(context.page, name)
    first = reg.get_by_role(role).first
    first.wait_for(state="visible")
    assert norm(first.inner_text()) == text, f"first {role}: {norm(first.inner_text())!r}"
    lines = [ln.strip() for ln in reg.inner_text().splitlines() if ln.strip()]
    assert text in lines[:2], f"'{text}' is not the first line of the region: {lines[:3]}"


@then('the region "{name:Q}" lists each issue with its detail and location')
def step_region_issues(context, name):
    reg = _region(context.page, name)
    text = norm(reg.inner_text())
    bodies = [b for (m, p, s, b) in context.api_responses if m == "POST" and s == 400]
    assert bodies, "the UI received no 400 report"
    issues = json.loads(bodies[-1]).get("issues", [])
    assert issues, "the 400 answer holds no issues"
    for issue in issues:
        for key in ("detail", "location"):
            value = norm(issue.get(key))
            if value:
                assert value in text, f"{key} {value!r} not in the report: {text!r}"


@then('the region "{name:Q}" lists the location "{text:Q}"')
@then('the user sees a region "{name:Q}" listing "{text:Q}"')
def step_region_lists(context, name, text):
    page = context.page
    want = sub(context, text)
    reg = _region(page, name)
    wait_until(lambda: want in norm(reg.inner_text()) or norm(reg.inner_text()), f"'{want}' not in {name}")


@then('the field "{name:Q}" holds "{value:Q}"')
def step_field_holds(context, name, value):
    target = field(context.page, name).first
    want = sub(context, value)
    wait_until(lambda: target.input_value() == want or f"value {target.input_value()!r}", f"'{name}' differs")


@then('the field "{name:Q}" shows "{text:Q}"')
def step_field_shows(context, name, text):
    page = context.page
    want = sub(context, text)
    wait_until(lambda: want in field_error(page, name) or f"text at the field {field_error(page, name)!r}",
               f"'{want}' not shown at '{name}'")


@then('the field "{name:Q}" is empty and required')
def step_field_empty_required(context, name):
    target = field(context.page, name).first
    target.wait_for(state="visible")
    assert target.input_value() == "", f"value {target.input_value()!r}"
    marked = target.evaluate("e => e.required || e.getAttribute('aria-required') === 'true'")
    if marked:
        return
    # D33 and D37 decide the rule, not its markup: an empty field must stop Submit with an error
    # under it, and nothing is sent (D62).
    context.request_mark = time.time()
    context.page.get_by_role("button", name="Submit", exact=True).click()
    context.page.wait_for_timeout(500)
    assert field_error(context.page, name) and field_invalid(context.page, name), \
        f"'{name}' is neither marked required nor refused empty on Submit"
    sent = [(m, p) for (m, p) in requests_since_mark(context) if m != "GET"]
    assert not sent, f"Submit with an empty '{name}' sent {sent}"


# --------------------------------------------------------------------------- accessibility

@then("the page has no WCAG 2.1 AA axe violations")
def step_axe(context):
    from axe_playwright_python.sync_playwright import Axe
    page = context.page
    settle(page)
    page.wait_for_timeout(500)
    results = Axe().run(page, options={"runOnly": {"type": "tag", "values": WCAG_TAGS}})
    violations = results.response.get("violations", [])
    REPORTS.mkdir(exist_ok=True)
    out = REPORTS / "a11y.json"
    data = json.loads(out.read_text()) if out.exists() else {}
    data[urlparse(page.url).path] = {
        "rules_passed": len(results.response.get("passes", [])),
        "violations": [{"id": v["id"], "impact": v.get("impact"), "help": v.get("help"),
                        "nodes": [n.get("target") for n in v.get("nodes", [])]} for v in violations]}
    out.write_text(json.dumps(data, indent=2))
    summary = "; ".join(f"{v['id']} ({v.get('impact')}, {len(v.get('nodes', []))} nodes): {v.get('help')}"
                        for v in violations)
    assert not violations, f"{len(violations)} axe violations: {summary}"


@then("every action on the page is reachable by keyboard")
def step_keyboard(context):
    page = context.page
    total = page.evaluate(
        """() => {
            const sel = 'a[href], button, input, select, textarea, [role=button], [role=link], [tabindex]';
            let n = 0;
            for (const el of document.querySelectorAll(sel)) {
                const r = el.getBoundingClientRect();
                const hidden = r.width === 0 || r.height === 0 || getComputedStyle(el).visibility === 'hidden';
                if (hidden || el.disabled || el.getAttribute('tabindex') === '-1') continue;
                el.setAttribute('data-kbcheck', String(n++));
            }
            return n;
        }""")
    reached = set()
    page.evaluate("document.activeElement && document.activeElement.blur()")
    for _ in range(total * 2 + 10):
        page.keyboard.press("Tab")
        mark = page.evaluate("document.activeElement && document.activeElement.getAttribute('data-kbcheck')")
        if mark is not None:
            reached.add(mark)
        if len(reached) == total:
            break
    missing = page.evaluate(
        """(reached) => [...document.querySelectorAll('[data-kbcheck]')]
            .filter(e => !reached.includes(e.getAttribute('data-kbcheck')))
            .map(e => (e.getAttribute('aria-label') || e.innerText || e.outerHTML).trim().slice(0, 60))""",
        sorted(reached))
    assert not missing, f"not reachable by Tab: {missing}"


# environment.py closes the browser context after each scenario and the browser at the end.
vmclient.UI_HOOKS = {"close_page": close_page, "stop_browser": stop_browser}
