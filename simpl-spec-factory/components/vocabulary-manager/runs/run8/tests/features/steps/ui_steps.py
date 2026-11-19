"""Browser steps of the @ui scenarios (step 60, Playwright for Python).

Elements are found only by roles, decided labels, decided texts and decided
column names (D18, D23, D35, D36, D60, D72, D74, D75). Routes come from D31
and D74, the API path prefix /v1 from E2. Nothing is taken from the build:
no test ids, no CSS classes, no accessible names that no decision gives.

Setup data goes through the API steps (api_steps.py) as ga-admin. Every
scenario opens a fresh browser context, so no session is reused.
Run with ./run.sh ui (behave JSON in reports/ui.json).
"""
import json
import re
import time
from pathlib import Path

from behave import given, then, when

from vmtest import client, config
from vmtest import ui
from vmtest.resolve import expand

FIXTURES = Path(__file__).resolve().parents[2] / "fixtures"
REPORTS = Path(__file__).resolve().parents[2] / "reports"
T = config.UI_TIMEOUT_MS

# D9, D11, D12, D47: detail texts of validation issues the UI may list.
ISSUE_DETAILS = (
    "Encountered invalid Turtle syntax",
    "Literal value for object property",
    "Resource value for datatype property",
    "Property declared as both object and datatype property",
    "Duplicate preferred label for the same language",
    "Resource is an instance of disjoint classes",
    "Vocabulary contains no statements",
    "Referenced internal vocabulary",
)


def _expect():
    from playwright.sync_api import expect

    expect.set_options(timeout=T)
    return expect


def _x(context, text):
    return expand(context, text)


def _page(context):
    page = getattr(context, "ui_page", None)
    if page is None:
        raise AssertionError("no browser page: open the UI first")
    return page


def _settle(page, ms=8000):
    try:
        page.wait_for_load_state("networkidle", timeout=ms)
    except Exception:  # noqa: BLE001 (polling apps never go idle; the assertions wait)
        pass


# --------------------------------------------------------------------------
# browser context, stubs and request log
# --------------------------------------------------------------------------


def _ensure_state(context):
    if not hasattr(context, "ui_stubs") or context.ui_stubs is None:
        context.ui_stubs = []
        context.ui_requests = []
        context.ui_stub_detail = None
        context.ui_last_menu = None
        context.ui_download = None
        context.ui_user = None
        context.ui_fixture = None


def _router(context):
    def handle(route, request):
        for stub in context.ui_stubs:
            if request.resource_type in ("fetch", "xhr") and ui.api_call_matches(request.url, request.method, stub["method"], stub["path"]):
                headers = {"Content-Type": stub["ctype"]}
                if stub["status"] == 503:
                    headers["Retry-After"] = "5"
                route.fulfill(status=stub["status"], headers=headers, body=stub["body"])
                return
        route.continue_()

    return handle


def _new_browser(context):
    _ensure_state(context)
    ui.close_scenario(context)
    ctx = ui.new_context()
    ctx.set_default_timeout(T)
    ctx.route("**/*", _router(context))
    page = ctx.new_page()
    page.on("request", lambda r: context.ui_requests.append((r.method, r.url, r.resource_type)))
    context.ui_ctx = ctx
    context.ui_page = page
    return page


def _is_keycloak_url(url):
    return url.startswith(ui.keycloak_origin()) and "/realms/" in url


def _on_keycloak(page):
    return _is_keycloak_url(page.url)


def _login(page, user):
    expect = _expect()
    username = config.USERS.get(user, user)
    # Keycloak's own login form (E1), not the build's UI.
    user_field = page.get_by_label(re.compile(r"user ?name", re.I)).first
    expect(user_field).to_be_visible()
    user_field.fill(username)
    page.get_by_label("Password", exact=True).fill(config.USER_PASSWORD)
    page.get_by_role("button", name=re.compile(r"^\s*(sign in|log in)\s*$", re.I)).click()
    page.wait_for_url(config.UI_URL + "/**", timeout=T * 2)


@given('the API is stubbed to answer {code:d} to {method:W} "{path:Q}"')
def step_stub_code(context, code, method, path):
    _ensure_state(context)
    path = _x(context, path)
    detail = "Stubbed %d problem detail %s" % (code, context.names.uid)
    context.ui_stub_detail = detail
    context.ui_stubs.append({"method": method, "path": path, "status": code,
                             "ctype": "application/problem+json", "body": ui.problem_body(code, detail)})


@given('the API is stubbed to answer an empty list to {method:W} "{path:Q}"')
def step_stub_empty(context, method, path):
    _ensure_state(context)
    context.ui_stubs.append({"method": method, "path": _x(context, path), "status": 200,
                             "ctype": "application/json", "body": ui.empty_list_body()})


@when('I open the UI as "{user:Q}"')
def step_open_as(context, user):
    page = _new_browser(context)
    context.ui_user = user
    page.goto(config.UI_URL + "/vocabularies")
    # The app sends an anonymous visitor to Keycloak (D18) from script, after load.
    try:
        page.wait_for_url(lambda u: _is_keycloak_url(u), timeout=T)
    except Exception:  # noqa: BLE001 (no redirect: the browser already holds a session)
        pass
    if _on_keycloak(page):
        _login(page, user)
    _settle(page)


@when("I open the UI without being logged in")
def step_open_anon(context):
    _new_browser(context)
    context.ui_user = None


@when('I go to "{route:Q}"')
def step_go(context, route):
    page = _page(context)
    page.goto(config.UI_URL + _x(context, route))
    _settle(page)
    if context.ui_user and _on_keycloak(page):
        # Single sign-on should bring the user straight back; log in again if not.
        try:
            page.wait_for_url(config.UI_URL + "/**", timeout=5000)
        except Exception:  # noqa: BLE001
            _login(page, context.ui_user)
    _settle(page)


# --------------------------------------------------------------------------
# generic finders (roles plus decided names only)
# --------------------------------------------------------------------------


def _action(page, label, scope=None):
    """A visible control with this exact name: menu item, button or link."""
    s = scope or page
    loc = (s.get_by_role("menuitem", name=label, exact=True)
           .or_(s.get_by_role("button", name=label, exact=True))
           .or_(s.get_by_role("link", name=label, exact=True)))
    return loc.filter(visible=True)


def _button(page, label):
    # 'New Vocabulary +' and 'Submit' are actions; a link styled as a button counts too.
    return (page.get_by_role("button", name=label, exact=True)
            .or_(page.get_by_role("link", name=label, exact=True))).filter(visible=True)


def _field(page, label):
    return page.get_by_label(label, exact=True)


def _clean(text):
    return re.sub(r"[\s\u2191\u2193\u25b2\u25bc\u25b4\u25be\u21c5\u2195]+", " ", text or "").strip()


def _table(page, marker_column):
    return page.get_by_role("table").filter(
        has=page.get_by_role("columnheader", name=re.compile(r"^\W*%s\W*$" % re.escape(marker_column)))
    ).first


def _vocab_table(page):
    return _table(page, "Last updated")


def _version_table(page):
    return _table(page, "Change log")


def _headers(table):
    return [_clean(t) for t in table.get_by_role("columnheader").all_inner_texts()]


def _col(table, name):
    heads = _headers(table)
    if name not in heads:
        raise AssertionError("column %r not in %r" % (name, heads))
    return heads.index(name)


def _data_rows(table):
    rows = table.get_by_role("row")
    out = []
    for i in range(rows.count()):
        r = rows.nth(i)
        if r.get_by_role("columnheader").count() == 0:
            out.append(r)
    return out


def _cells(row):
    return row.locator(":scope > *")


def _cell_text(row, idx):
    return _clean(_cells(row).nth(idx).inner_text())


def _vocab_rows(page):
    """(name, row) for every data row of the vocabulary table."""
    table = _vocab_table(page)
    idx = _col(table, "Name")
    return [(_cell_text(r, idx), r) for r in _data_rows(table)]


def _find_vocab_row(page, name):
    for n, r in _vocab_rows(page):
        if n == name:
            return r
    return None


def _set_filter(context, label, value):
    page = _page(context)
    field = _field(page, label)
    _expect()(field).to_be_visible()
    tag = field.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        field.select_option(label=value)
    elif field.get_attribute("role") == "combobox" and tag != "input":
        field.click()
        page.get_by_role("option", name=value, exact=True).click()
    else:
        field.fill(value)
        field.press("Enter")
    time.sleep(0.4)
    _settle(page)


def _row_for(context, name):
    """The list row of a vocabulary. The store is shared (E4), so when the row is
    not on the first page the decided 'Name contains' filter (D72) narrows the list."""
    page = _page(context)
    _expect()(_vocab_table(page)).to_be_visible()
    try:
        return ui.eventually(lambda: _must(_find_vocab_row(page, name), name), timeout=3)
    except AssertionError:
        _set_filter(context, "Name contains", context.names.uid)
        return ui.eventually(lambda: _must(_find_vocab_row(page, name), name), timeout=T / 1000)


def _must(value, what):
    if value is None:
        raise AssertionError("not found: %s" % what)
    return value


def _version_row(page, n):
    table = _version_table(page)
    _expect()(table).to_be_visible()

    def find():
        idx = _col(table, "Version")
        for r in _data_rows(table):
            if _cell_text(r, idx) == str(n):
                return r
        raise AssertionError("no version row %d" % n)

    return ui.eventually(find)


def _aliases(context, csv):
    return [context.names.real(a.strip()) for a in csv.split(",") if a.strip()]


# --------------------------------------------------------------------------
# list
# --------------------------------------------------------------------------


@then('the menu item "{label:Q}" is shown')
def step_menu_item(context, label):
    page = _page(context)
    loc = (page.get_by_role("menuitem", name=label, exact=True)
           .or_(page.get_by_role("navigation").get_by_role("link", name=label, exact=True)))
    _expect()(loc.first).to_be_visible()


@then('the vocabulary table includes the columns "{csv:Q}"')
def step_vocab_columns(context, csv):
    table = _vocab_table(_page(context))
    _expect()(table).to_be_visible()
    heads = _headers(table)
    missing = [c.strip() for c in csv.split(",") if c.strip() not in heads]
    assert not missing, "columns %r missing, headers are %r" % (missing, heads)


@then('the vocabulary table shows a row for "{alias:Q}" with status "{label:Q}"')
def step_row_status(context, alias, label):
    page = _page(context)
    name = context.names.real(alias)
    _row_for(context, name)

    def check():
        row = _must(_find_vocab_row(page, name), name)
        text = _cell_text(row, _col(_vocab_table(page), "Status"))
        assert text.startswith(label), "status cell of %s is %r, expected %r" % (name, text, label)

    ui.eventually(check)


@then('the row of "{alias:Q}" shows its dates as "{fmt:Q}"')
def step_row_dates(context, alias, fmt):
    page = _page(context)
    row = _row_for(context, context.names.real(alias))
    pattern = re.escape(fmt).replace("YYYY", r"\d{4}").replace("MM", r"\d{2}").replace("DD", r"\d{2}").replace("HH", r"\d{2}").replace("mm", r"\d{2}")
    table = _vocab_table(page)
    for col in ("Date created", "Last updated"):
        text = _cell_text(row, _col(table, col))
        assert re.fullmatch(pattern, text), "%s shows %r, expected format %s" % (col, text, fmt)


@when('I open the actions menu "{name:Q}"')
def step_open_menu(context, name):
    page = _page(context)
    name = _x(context, name)
    m = re.match(r"^Actions for (.+)$", name)
    if m and not m.group(1).startswith("version "):
        _row_for(context, m.group(1))
    button = page.get_by_role("button", name=name, exact=True)
    _expect()(button).to_be_visible()
    button.click()
    context.ui_last_menu = name


@then('the menu offers "{csv:Q}"')
def step_menu_offers(context, csv):
    page = _page(context)
    for label in [c.strip() for c in csv.split(",")]:
        _expect()(_action(page, label).first).to_be_visible()


@when('I choose "{label:Q}"')
def step_choose(context, label):
    page = _page(context)
    if _action(page, label).count() == 0 and context.ui_last_menu:
        btn = page.get_by_role("button", name=context.ui_last_menu, exact=True)
        if btn.count():
            btn.click()
    item = _action(page, label).first
    _expect()(item).to_be_visible()
    if label.startswith("Download"):
        with page.expect_download(timeout=T) as info:
            item.click()
        context.ui_download = info.value
    else:
        item.click()
    _settle(page, 4000)


@when('I set the filter "{label:Q}" to "{value:Q}"')
def step_set_filter(context, label, value):
    _set_filter(context, label, _x(context, value))


def _own_order(context):
    """Names of the scenario's own rows (uid in the name), in table order."""
    uid = context.names.uid
    return [n for n, _ in _vocab_rows(_page(context)) if uid in n]


@then('the vocabulary table shows only "{csv:Q}"')
def step_table_only(context, csv):
    want = _aliases(context, csv)

    def check():
        names = [n for n, _ in _vocab_rows(_page(context))]
        assert names == want, "table shows %r, expected only %r" % (names, want)

    ui.eventually(check)


@then('the vocabulary table is ordered "{csv:Q}"')
def step_table_order(context, csv):
    want = _aliases(context, csv)

    def check():
        got = _own_order(context)
        assert got == want, "order %r, expected %r" % (got, want)

    ui.eventually(check)


@when('I click the column header "{column:Q}"')
def step_click_header(context, column):
    page = _page(context)
    header = _vocab_table(page).get_by_role("columnheader", name=re.compile(r"^\W*%s\W*$" % re.escape(column)))
    inner = header.get_by_role("button")
    (inner.first if inner.count() else header).click()
    time.sleep(0.3)
    _settle(page)


@then('the vocabulary table is sorted by "{column:Q}" {direction:W}')
def step_sorted_by(context, column, direction):
    page = _page(context)

    def check():
        table = _vocab_table(page)
        idx = _col(table, column)
        uid = context.names.uid
        name_idx = _col(table, "Name")
        values = [_cell_text(r, idx) for r in _data_rows(table) if uid in _cell_text(r, name_idx)]
        assert len(values) >= 2, "too few own rows to judge the order: %r" % values
        want = sorted(values, reverse=(direction == "descending"))
        assert values == want, "%s values %r are not %s" % (column, values, direction)
        header = table.get_by_role("columnheader", name=re.compile(r"^\W*%s\W*$" % re.escape(column)))
        sort = header.get_attribute("aria-sort")
        assert sort in (None, direction), "aria-sort of %s is %r, expected %s" % (column, sort, direction)

    ui.eventually(check)


@then('the column headers "{csv:Q}" are not sortable')
def step_not_sortable(context, csv):
    page = _page(context)
    table = _vocab_table(page)
    for column in [c.strip() for c in csv.split(",")]:
        before = _own_order(context)
        header = table.get_by_role("columnheader", name=re.compile(r"^\W*%s\W*$" % re.escape(column)))
        _expect()(header).to_be_visible()
        assert header.get_by_role("button").count() == 0, "column %s has a sort control" % column
        assert header.get_attribute("aria-sort") in (None, "none"), "column %s has aria-sort" % column
        header.click()
        time.sleep(0.3)
        _settle(page, 3000)
        assert _own_order(context) == before, "a click on %s changed the order" % column


@then('the row of "{alias:Q}" has a warning icon named "{text:Q}"')
def step_row_icon(context, alias, text):
    page = _page(context)
    row = _row_for(context, context.names.real(alias))
    icon = (row.get_by_role("img", name=text, exact=True)
            .or_(row.get_by_label(text, exact=True))
            .or_(row.get_by_title(text, exact=True)))
    _expect()(icon.first).to_be_attached()
    # D72: the name is not visible row text.
    assert text not in row.inner_text(), "the warning is visible row text, D72 wants it as the icon name"
    del page


# --------------------------------------------------------------------------
# details, history, download
# --------------------------------------------------------------------------


@then('I am on the route "{route:Q}"')
def step_route(context, route):
    page = _page(context)
    want = config.UI_URL + _x(context, route)
    _expect()(page).to_have_url(re.compile("^" + re.escape(want) + r"/?([?#].*)?$"))


@then('the header shows the name "{name:Q}" and the status "{label:Q}"')
def step_header(context, name, label):
    page = _page(context)
    name = _x(context, name)
    _expect()(page.get_by_role("heading").filter(has_text=name).first).to_be_visible()
    status = page.locator("xpath=//*[not(ancestor-or-self::table)][not(ancestor-or-self::*[@role='table'])][normalize-space(.)='%s']" % label)
    _expect()(status.filter(visible=True).first).to_be_visible()


@then('the version table has the columns "{csv:Q}"')
def step_version_columns(context, csv):
    table = _version_table(_page(context))
    _expect()(table).to_be_visible()
    heads = _headers(table)
    want = [c.strip() for c in csv.split(",")]
    assert heads == want or all(c in heads for c in want), "headers %r, expected %r" % (heads, want)


@then('the version table lists the versions "{csv:Q}"')
def step_version_list(context, csv):
    page = _page(context)
    want = [c.strip() for c in csv.split(",")]

    def check():
        table = _version_table(page)
        idx = _col(table, "Version")
        got = [_cell_text(r, idx) for r in _data_rows(table)]
        assert got == want, "versions %r, expected %r" % (got, want)

    ui.eventually(check)


@then('the version rows "{csv:Q}" show the status "{label:Q}"')
def step_version_rows_status(context, csv, label):
    page = _page(context)
    for n in [int(c) for c in csv.split(",")]:
        row = _version_row(page, n)
        text = _cell_text(row, _col(_version_table(page), "Status"))
        assert text.startswith(label), "version %d status %r, expected %r" % (n, text, label)


def _region(page, name):
    return page.get_by_role("region", name=name, exact=True)


@when("I select version {n:d}")
def step_select_version(context, n):
    page = _page(context)
    row = _version_row(page, n)
    cell = _cells(row).nth(_col(_version_table(page), "Version"))
    control = cell.get_by_role("button").or_(cell.get_by_role("link"))
    (control.first if control.count() else cell).click()
    _expect()(_region(page, "Version %d" % n)).to_be_visible()


@then('the region "{name:Q}" shows "{csv:Q}"')
def step_region_shows(context, name, csv):
    page = _page(context)
    region = _region(page, _x(context, name))
    _expect()(region).to_be_visible()
    want = _x(context, csv)

    def check():
        text = _clean(region.inner_text())
        if want in text:
            return
        # A list of labels (for example "Version,Status,Date created").
        missing = [c.strip() for c in want.split(",") if c.strip() not in text]
        assert "," in want and not missing, "region %s lacks %r: %r" % (name, missing or [want], text)

    ui.eventually(check)


@then('the region "{name:Q}" shows the creator "{value:Q}"')
def step_region_creator(context, name, value):
    region = _region(_page(context), _x(context, name))
    _expect()(region).to_contain_text(_x(context, value))


@then('the region "{name:Q}" offers "{label:Q}"')
def step_region_offers(context, name, label):
    page = _page(context)
    region = _region(page, _x(context, name))
    _expect()(_action(page, label, scope=region).first).to_be_visible()


def _items(region):
    items = region.get_by_role("listitem")
    if items.count():
        return [_clean(items.nth(i).inner_text()) for i in range(items.count())]
    tables = region.get_by_role("table")
    if tables.count():
        return [_clean(r.inner_text()) for r in _data_rows(tables.first)]
    return []


@then('the region "{name:Q}" lists {n:d} issues with detail and location')
def step_region_issues(context, name, n):
    page = _page(context)
    region = _region(page, name)
    _expect()(region).to_be_visible()

    def check():
        items = _items(region)
        assert len(items) == n, "%s lists %d items, expected %d: %r" % (name, len(items), n, items)
        for item in items:
            assert any(d in item for d in ISSUE_DETAILS), "item without a decided detail: %r" % item
            assert re.search(r"https?://|line \d+|document|prefix '", item), "item without a location: %r" % item

    ui.eventually(check)


@then('the region "{name:Q}" lists {n:d} warning with description and location')
def step_region_warnings(context, name, n):
    page = _page(context)
    region = _region(page, name)
    _expect()(region).to_be_visible()

    def check():
        items = _items(region)
        assert len(items) == n, "%s lists %d items, expected %d: %r" % (name, len(items), n, items)
        for item in items:
            assert re.search(r"prefix '[^']*' -> '", item), "warning without the D65 location: %r" % item
            assert len(re.sub(r"prefix '[^']*' -> '[^']*'", "", item).strip()) > 3, "warning without a description: %r" % item

    ui.eventually(check)


@then('a file "{filename:Q}" is downloaded with the content of version {n:d} of "{alias:Q}"')
def step_downloaded(context, filename, n, alias):
    dl = context.ui_download
    assert dl is not None, "no download happened"
    want_name = _x(context, filename)
    assert dl.suggested_filename == want_name, "file name %r, expected %r" % (dl.suggested_filename, want_name)
    got = Path(dl.path()).read_bytes()
    name = context.names.real(alias)
    resp = client.request("GET", "/content/%s/versions/%d" % (name, n), "ga-admin")
    assert resp.status_code == 200, "content read failed: %s" % resp.status_code
    assert got == resp.content, "downloaded %d bytes differ from version %d (%d bytes)" % (len(got), n, len(resp.content))


@then('the version row {n:d} shows "{text:Q}"')
def step_version_row_text(context, n, text):
    page = _page(context)
    ui.eventually(lambda: _assert_in(_x(context, text), _version_row(page, n).inner_text(), "version row %d" % n))


def _assert_in(needle, hay, what):
    assert needle in hay, "%s does not show %r: %r" % (what, needle, hay)


@then('the version row {n:d} links "{text:Q}" to version {m:d}')
def step_version_row_link(context, n, text, m):
    page = _page(context)
    row = _version_row(page, n)
    _expect()(row).to_contain_text(text)
    link = row.get_by_role("link", name=text, exact=True).or_(row.get_by_role("link", name=str(m), exact=True))
    _expect()(link.first).to_be_visible()
    link.first.click()
    _expect()(_region(page, "Version %d" % m)).to_be_visible()


@then("the version row {n:d} shows no deprecation warning")
def step_version_row_no_warning(context, n):
    text = _version_row(_page(context), n).inner_text()
    assert "This version is deprecated." not in text, "version row %d shows a deprecation warning" % n


@then('the version row {n:d} shows no "{text:Q}" line')
def step_version_row_no_line(context, n, text):
    row_text = _version_row(_page(context), n).inner_text()
    assert text not in row_text, "version row %d shows %r" % (n, text)


@then('no version row shows a "{text:Q}" line')
def step_no_version_row_line(context, text):
    page = _page(context)
    table = _version_table(page)
    _expect()(table).to_be_visible()
    assert text not in table.inner_text(), "a version row shows %r" % text


# --------------------------------------------------------------------------
# messages
# --------------------------------------------------------------------------


@then('I see the alert "{text:Q}"')
def step_alert(context, text):
    page = _page(context)
    _expect()(page.get_by_role("alert").filter(has_text=_x(context, text)).first).to_be_visible()


@then("the alert shows the problem detail")
def step_alert_detail(context):
    page = _page(context)
    detail = context.ui_stub_detail
    assert detail, "no stubbed problem detail in this scenario"
    _expect()(page.get_by_role("alert").filter(has_text=detail).first).to_be_visible()


@then('I see the status message "{text:Q}"')
def step_status(context, text):
    page = _page(context)
    _expect()(page.get_by_role("status").filter(has_text=_x(context, text)).first).to_be_visible()


@then('I see the text "{text:Q}"')
def step_text(context, text):
    page = _page(context)
    _expect()(page.get_by_text(_x(context, text)).filter(visible=True).first).to_be_visible()


@then('I see the link "{text:Q}"')
def step_link(context, text):
    _expect()(_page(context).get_by_role("link", name=text, exact=True)).to_be_visible()


@then('I see the button "{label:Q}"')
def step_see_button(context, label):
    _expect()(_button(_page(context), label).first).to_be_visible()


@then('I do not see the button "{label:Q}"')
def step_no_button(context, label):
    page = _page(context)
    # The page must have rendered before the absence means anything. A user
    # without a role may see no table at all (D16), so wait for the heading.
    _expect()(page.get_by_role("heading").first).to_be_visible()
    _settle(page, 3000)
    time.sleep(0.5)
    assert _button(page, label).count() == 0, "the action %r is shown" % label


@then("I see the Keycloak login page")
def step_keycloak(context):
    page = _page(context)
    _expect()(page).to_have_url(re.compile("^" + re.escape(ui.keycloak_origin()) + "/realms/"))
    _expect()(page.get_by_label("Password", exact=True)).to_be_visible()


# --------------------------------------------------------------------------
# forms
# --------------------------------------------------------------------------


@when('I click "{label:Q}"')
def step_click(context, label):
    page = _page(context)
    target = _button(page, label).first
    _expect()(target).to_be_visible()
    target.click()
    _settle(page, 5000)


@when('I fill in "{label:Q}" with "{value:Q}"')
def step_fill(context, label, value):
    field = _field(_page(context), label)
    _expect()(field).to_be_visible()
    field.fill(_x(context, value))


def _attach(context, label, filename, content):
    page = _page(context)
    field = _field(page, label)
    _expect()(field).to_be_attached()
    payload = {"name": filename, "mimeType": "text/turtle", "buffer": content}
    try:
        field.set_input_files(payload, timeout=3000)
    except Exception:  # noqa: BLE001 (label on a custom control: go through the file chooser)
        with page.expect_file_chooser() as info:
            field.click()
        info.value.set_files(payload)
    time.sleep(0.3)
    _settle(page, 4000)


@when('I attach the fixture "{fixture:Q}" as "{label:Q}"')
def step_attach_fixture(context, fixture, label):
    content = _x(context, (FIXTURES / fixture).read_text(encoding="utf-8")).encode("utf-8")
    context.ui_fixture = fixture
    _attach(context, label, fixture, content)


@when('I attach a file of {n:d} bytes as "{label:Q}"')
def step_attach_size(context, n, label):
    name = context.names.real("Big")
    _attach(context, label, "%s.ttl" % name, client.turtle_of_size(name, n))


@when('I attach a file that references version {n:d} of "{alias:Q}" as "{label:Q}"')
def step_attach_reference(context, n, alias, label):
    ref = context.names.real(alias)
    content = (
        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
        "@prefix ex: <https://example.org/vocab/%s/main#> .\n"
        "@prefix ref: <%s/content/%s/versions/%d#> .\n"
        "ex:Thing a skos:Concept ;\n"
        "  skos:related ref:Something .\n" % (context.names.uid, client.content_base(), ref, n)
    ).encode("utf-8")
    _attach(context, label, "references.ttl", content)


@then('the form has the fields "{csv:Q}"')
def step_form_fields(context, csv):
    page = _page(context)
    for label in [c.strip() for c in csv.split(",")]:
        _expect()(_field(page, label)).to_be_attached()


@then('the form has no field "{label:Q}"')
def step_form_no_field(context, label):
    page = _page(context)
    _expect()(_field(page, "Description")).to_be_attached()
    assert _field(page, label).count() == 0, "the form has a field %r" % label


def _error_of(field):
    invalid = field.get_attribute("aria-invalid") == "true"
    desc = field.evaluate(
        """e => {
            const ids = ((e.getAttribute('aria-describedby') || '') + ' ' + (e.getAttribute('aria-errormessage') || '')).trim().split(/\\s+/);
            return ids.filter(Boolean).map(i => { const n = document.getElementById(i); return n ? n.innerText : ''; }).join(' ').trim();
        }"""
    )
    return invalid, desc


@then('the field "{label:Q}" shows an error')
def step_field_error(context, label):
    field = _field(_page(context), label)

    def check():
        invalid, desc = _error_of(field)
        assert invalid or desc, "field %s shows no error (no aria-invalid, no described error text)" % label

    ui.eventually(check)


@then('the field "{label:Q}" shows the error "{text:Q}"')
def step_field_error_text(context, label, text):
    page = _page(context)
    field = _field(page, label)
    _expect()(page.get_by_text(text).filter(visible=True).first).to_be_visible()

    def check():
        invalid, desc = _error_of(field)
        assert invalid or text in desc, "the error %r is not tied to the field %s" % (text, label)

    ui.eventually(check)


@then('the field "{label:Q}" shows "{text:Q}"')
def step_field_shows(context, label, text):
    page = _page(context)
    _expect()(_field(page, label)).to_be_attached()
    _expect()(page.get_by_text(_x(context, text)).filter(visible=True).first).to_be_visible()


@then('the field "{label:Q}" is empty')
def step_field_empty(context, label):
    _expect()(_field(_page(context), label)).to_have_value("")


@then('the field "{label:Q}" contains the description of version {n:d} of "{alias:Q}"')
def step_field_description(context, label, n, alias):
    name = context.names.real(alias)
    resp = client.request("GET", "/vocabularies/%s/versions/%d" % (name, n), "ga-admin")
    assert resp.status_code == 200, "read of version %d failed: %s" % (n, resp.status_code)
    _expect()(_field(_page(context), label)).to_have_value(resp.json()["description"])


@then('the focus is on the field "{label:Q}"')
def step_focus(context, label):
    _expect()(_field(_page(context), label)).to_be_focused()


@then('no request was sent to {method:W} "{path:Q}"')
def step_no_request(context, method, path):
    time.sleep(0.5)
    path = _x(context, path)
    sent = [u for (m, u, kind) in context.ui_requests
            if kind in ("fetch", "xhr") and ui.api_call_matches(u, m, method, path)]
    assert not sent, "requests were sent: %r" % sent


@then("I see a syntax error at line {n:d}")
def step_syntax_error(context, n):
    page = _page(context)
    loc = page.get_by_text(re.compile(r"\bline %d\b" % n, re.I)).filter(visible=True)
    _expect()(loc.first).to_be_visible()


@then('the button "{label:Q}" is {state:W}')
def step_button_state(context, label, state):
    button = _page(context).get_by_role("button", name=label, exact=True)
    if state == "disabled":
        _expect()(button).to_be_disabled()
    elif state == "enabled":
        _expect()(button).to_be_enabled()
    else:
        raise AssertionError("unknown state %s" % state)


# --------------------------------------------------------------------------
# accessibility (Q6, NFR-10)
# --------------------------------------------------------------------------

AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]


def _page_ready(page):
    _settle(page)
    # Wait for the main content of the page (a heading), so axe sees the rendered page.
    _expect()(page.get_by_role("heading").first).to_be_visible()
    time.sleep(0.5)


@then("the page has no axe violations")
def step_axe(context):
    from axe_playwright_python.sync_playwright import Axe

    page = _page(context)
    _page_ready(page)
    results = Axe().run(page, options={"runOnly": {"type": "tag", "values": AXE_TAGS}})
    violations = results.response.get("violations", [])
    out = REPORTS / "a11y"
    out.mkdir(parents=True, exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9]+", "_", page.url.split("://", 1)[-1].split("/", 1)[-1].replace(context.names.uid, "")).strip("_") or "root"
    (out / ("%s.json" % slug)).write_text(json.dumps({"url": page.url, "tags": AXE_TAGS, "violations": violations}, indent=2))
    summary = ["%s (%s, %d nodes): %s" % (v["id"], v.get("impact"), len(v.get("nodes", [])), v.get("help")) for v in violations]
    assert not violations, "axe WCAG 2.1 AA violations on %s:\n  %s" % (page.url, "\n  ".join(summary))


@then("every action on the page is reachable by keyboard")
def step_keyboard(context):
    page = _page(context)
    _page_ready(page)
    total = page.evaluate(
        """() => {
            const sel = 'a[href], button, input, select, textarea, [role=button], [role=link], [role=menuitem], [tabindex]';
            let i = 0;
            for (const e of document.querySelectorAll(sel)) {
                const r = e.getBoundingClientRect();
                const visible = r.width > 0 && r.height > 0 && getComputedStyle(e).visibility !== 'hidden';
                if (!visible || e.disabled || e.getAttribute('tabindex') === '-1' || e.type === 'hidden') continue;
                e.setAttribute('data-kbd-probe', String(i++));
            }
            document.activeElement && document.activeElement.blur && document.activeElement.blur();
            return i;
        }"""
    )
    assert total > 0, "no actions found on the page"
    reached = set()
    for _ in range(total * 2 + 10):
        page.keyboard.press("Tab")
        probe = page.evaluate("() => document.activeElement && document.activeElement.getAttribute('data-kbd-probe')")
        if probe is not None:
            reached.add(probe)
        if len(reached) >= total:
            break
    missing = page.evaluate(
        """(reached) => [...document.querySelectorAll('[data-kbd-probe]')]
            .filter(e => !reached.includes(e.getAttribute('data-kbd-probe')))
            .map(e => (e.getAttribute('aria-label') || e.innerText || e.name || e.tagName).trim().slice(0, 60))""",
        sorted(reached),
    )
    assert not missing, "not reachable by Tab: %r" % missing
