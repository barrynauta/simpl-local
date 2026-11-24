"""Browser steps for the @ui scenarios (step 60), Playwright for Python.

Elements are found only by what the decisions fix: routes (D31, D74, D163, D180, D210), visible texts and
labels (D18, D34 to D36, D60, D72, D109, D115, D209, D210), roles and accessible names (D75, D90, D163),
and the API base path /v1 (E2) for the stubbed answers of D24. Nothing is taken from the build.
"""
import json
import os
import re
import time
import urllib.parse
from datetime import datetime, timezone

import parse
from behave import given, then, when, step, register_type
try:
    from playwright.sync_api import expect
except ImportError:  # the API and load modes do not install Playwright
    expect = None

from vmtest import client, config, turtle
from vmtest.ui import AXE_REPORT_DIR, UI_URL


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(Q=_quoted)

DATE_FORM = re.compile(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC$")
SORT_MARKS = re.compile(r"[←-⇿■-◿⬀-⯿]")


# =========================================================================== helpers
def x(context, text):
    return context.vm.expand(text)


def pg(context):
    assert context.ui is not None, "no browser session (is the scenario tagged @ui?)"
    return context.ui.page


def quoted_list(text):
    """'"a", "b" and "c"' -> [a, b, c]."""
    return re.findall(r'"([^"]*)"', text)


def comma_list(text):
    return [part.strip() for part in text.split(",") if part.strip()]


def settle(page, pause=400):
    """Let a debounced action start, then wait until the network is quiet."""
    page.wait_for_timeout(pause)
    try:
        page.wait_for_load_state("networkidle", timeout=10000)
    except Exception:  # noqa: BLE001
        pass


def exact_name(text):
    return re.compile(r"^\s*" + re.escape(text) + r"\s*$")


def label_name(text):
    """A field label as D60 gives it, allowing the '*' of D141 after it."""
    return re.compile(r"^\s*" + re.escape(text) + r"\s*\*?\s*$")


def field(page, label):
    return page.get_by_label(label_name(label)).first


def action(scope, name):
    """A button or a link with this exact name: the decisions name actions, not their element."""
    return scope.get_by_role("button", name=exact_name(name)).or_(
        scope.get_by_role("link", name=exact_name(name))).first


def norm(text):
    return re.sub(r"\s+", " ", SORT_MARKS.sub("", text or "")).strip()


def api_json(context, path):
    response = context.vm.admin("GET", path)
    assert response.status_code == 200, f"{path}: {response.status_code} {response.text[:300]}"
    return response.json()


def ui_date(iso):
    stamp = datetime.fromisoformat(iso.replace("Z", "+00:00")).astimezone(timezone.utc)
    return stamp.strftime("%Y-%m-%d %H:%M UTC")


def version_content(context, name, version):
    # GET /content/<Name>/versions/<n> of the API (D29); contentHref holds the public contentBase (E2).
    response = context.vm.admin("GET", f"/content/{name}/versions/{version}")
    assert response.status_code == 200, f"content of {name} v{version}: {response.status_code}"
    return response.content


# --------------------------------------------------------------------------- tables
def table(page, index=0):
    return page.get_by_role("table").nth(index)


def headers(tbl):
    return [norm(t) for t in tbl.get_by_role("columnheader").all_inner_texts()]


def column_index(tbl, column):
    names = headers(tbl)
    assert column in names, f"no column '{column}' in {names}"
    return names.index(column)


def data_rows(page, tbl=None):
    tbl = tbl or table(page)
    return tbl.get_by_role("row").filter(has=page.get_by_role("cell"))


def row_of(page, name, tbl=None):
    """The data row whose first cell starts with the name (a row's text has no separators between cells)."""
    first_cell = page.locator(":scope > td:first-child, :scope > th:first-child").filter(
        has_text=re.compile(r"^\s*" + re.escape(name) + r"(?![A-Za-z0-9])"))
    return data_rows(page, tbl).filter(has=first_cell)


def cells(row):
    return row.locator(":scope > td, :scope > th")


def cell_text(tbl, row, column):
    return norm(cells(row).nth(column_index(tbl, column)).inner_text())


def column_texts(page, column, tbl=None):
    tbl = tbl or table(page)
    idx = column_index(tbl, column)
    rows = data_rows(page, tbl)
    return [norm(cells(rows.nth(i)).nth(idx).inner_text()) for i in range(rows.count())]


def version_row(page, version):
    rows = data_rows(page)
    for i in range(rows.count()):
        row = rows.nth(i)
        if norm(cells(row).nth(0).inner_text()) == str(version):
            return row
    raise AssertionError(f"no row for version {version}: {column_texts(page, 'Version')}")


def wait_rows(page):
    """Wait until the table has data rows or an empty state."""
    deadline = time.time() + 10
    while time.time() < deadline:
        if data_rows(page).count() > 0:
            return
        page.wait_for_timeout(200)


def header_cell(tbl, column):
    return tbl.get_by_role("columnheader").nth(column_index(tbl, column))


# --------------------------------------------------------------------------- paging (D115, D210)
def next_button(page):
    return page.get_by_role("button", name=exact_name("Next page"))


def find_by_paging(context, name, route=None, max_pages=500):
    """Press 'Next page' until the row of `name` shows; returns the row locator."""
    page = pg(context)
    if route:
        page.goto(UI_URL + route)
        settle(page)
    for _ in range(max_pages):
        wait_rows(page)
        row = row_of(page, name)
        if row.count() > 0:
            return row.first
        button = next_button(page)
        if button.count() == 0 or not button.is_enabled():
            break
        button.click()
        settle(page, 200)
    raise AssertionError(f"row of {name} not found on any page")


# --------------------------------------------------------------------------- Turtle for the forms
def form_vocabulary_name(context):
    if context.ui.form_name:
        return context.ui.form_name
    match = re.search(r"/vocabularies/([A-Za-z0-9]+)/versions/new", pg(context).url)
    return match.group(1) if match else "Form" + context.vm.run


def ttl(context, kind, arg=None):
    run = context.vm.run
    name = form_vocabulary_name(context)
    ns = turtle.own_namespace(run, name)
    head = f"@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix ex: <{ns}> .\n"
    if kind == "valid":
        return turtle.default_file(run, name)
    if kind == "size":
        return turtle.sized_file(run, name, arg)
    if kind == "syntax3":
        return (f"@prefix skos: <{turtle.SKOS}> .\n@prefix ex: <{ns}> .\n"
                f"ex:Scheme a skos:ConceptScheme ) .\n"
                f"ex:Other a skos:Concept .\n").encode()
    if kind == "s1":
        return (head + "ex:hasPart a owl:ObjectProperty .\nex:car ex:hasPart \"wheel\" .\n").encode()
    if kind == "s3errors":
        return (head + f"@prefix skos: <{turtle.SKOS}> .\n"
                "ex:hasPart a owl:ObjectProperty .\nex:car ex:hasPart \"wheel\" .\n"
                "ex:size a owl:DatatypeProperty .\nex:car ex:size ex:big .\n"
                "ex:car skos:prefLabel \"Car\"@en , \"Auto\"@en .\n").encode()
    if kind == "reference":
        ref_name, ref_version = arg
        base = context.vm.special("CB")
        return (f"@prefix skos: <{turtle.SKOS}> .\n@prefix b: <{base}/content/{ref_name}/versions/{ref_version}#> .\n"
                f"@prefix ex: <{ns}> .\nex:Scheme a skos:ConceptScheme ; skos:related b:Scheme .\n").encode()
    if kind == "external":
        ext = context.vm.special("EXT")
        return (f"@prefix skos: <{turtle.SKOS}> .\n@prefix dct: <{ext}/terms/> .\n@prefix ex: <{ns}> .\n"
                "ex:Scheme a skos:ConceptScheme ; dct:title \"Alpha\" .\n").encode()
    if kind == "external_registration":
        namespace = arg
        return (f"@prefix skos: <{turtle.SKOS}> .\n"
                f"<{namespace.rstrip('#/')}#Term> a skos:Concept ; skos:prefLabel \"Term\"@en .\n").encode()
    raise KeyError(kind)


def choose_file(context, label, data, filename=None):
    page = pg(context)
    filename = filename or f"{form_vocabulary_name(context)}.ttl"
    payload = {"name": filename, "mimeType": "text/turtle", "buffer": data}
    control = field(page, label)
    is_input = control.evaluate("e => e.tagName === 'INPUT' && e.type === 'file'")
    if is_input:
        control.set_input_files(payload)
    else:
        with page.expect_file_chooser() as chooser:
            control.click()
        chooser.value.set_files(payload)
    settle(page, 600)


# =========================================================================== session and navigation
@given('I am logged in to the UI as "{user:Q}"')
def step_login(context, user):
    page = pg(context)
    page.goto(UI_URL + "/")
    username = page.get_by_label(re.compile(r"user", re.I)).first
    username.wait_for(state="visible")
    username.fill(config.USERS.get(user, user))
    password = page.get_by_label(re.compile(r"^\s*password\s*$", re.I)).first
    password.fill(config.PASSWORD)
    password.press("Enter")
    page.wait_for_url(lambda url: url.startswith(UI_URL))
    settle(page)


@given("I am not logged in to the UI")
def step_not_logged_in(context):
    pg(context)  # a fresh context without stored sessions


@when('I open the route "{route:Q}"')
def step_open_route(context, route):
    page = pg(context)
    page.goto(UI_URL + x(context, route))
    settle(page, 600)


@when('I open the menu item "{label:Q}"')
def step_menu_item(context, label):
    page = pg(context)
    nav = page.get_by_role("navigation")
    scope = nav if nav.count() > 0 else page
    scope.get_by_role("link", name=exact_name(label)).first.click()
    settle(page)


@then('I am on the route "{route:Q}"')
def step_on_route(context, route):
    expected = UI_URL + x(context, route)
    expect(pg(context)).to_have_url(re.compile("^" + re.escape(expected) + r"/?([?#].*)?$"))


@then('I am no longer on the route "{route:Q}"')
def step_not_on_route(context, route):
    expected = UI_URL + x(context, route)
    expect(pg(context)).not_to_have_url(re.compile("^" + re.escape(expected) + r"/?([?#].*)?$"))


@then("I am on the Keycloak login page")
def step_keycloak(context):
    auth = config.TOKEN_URL.rsplit("/token", 1)[0] + "/auth"
    page = pg(context)
    expect(page).to_have_url(re.compile("^" + re.escape(auth)))
    expect(page.get_by_label(re.compile(r"^\s*password\s*$", re.I)).first).to_be_visible()


# =========================================================================== generic look-ups
@step('I press the button "{name:Q}"')
def step_press(context, name):
    page = pg(context)
    dialog = page.get_by_role("dialog").or_(page.get_by_role("alertdialog"))
    scope = dialog.first if name in ("Confirm",) and dialog.count() > 0 else page
    action(scope, x(context, name)).click()
    settle(page, 600)


@step('I press the button "{name:Q}" in the region "{region:Q}"')
def step_press_in_region(context, name, region):
    page = pg(context)
    page.get_by_role("region", name=exact_name(x(context, region))).get_by_role(
        "button", name=exact_name(x(context, name))).click()
    settle(page, 600)


@then('I see the button "{name:Q}"')
def step_see_button(context, name):
    expect(action(pg(context), x(context, name))).to_be_visible()


@then('I do not see the button "{name:Q}"')
def step_no_button(context, name):
    page = pg(context)
    settle(page)
    expect(page.get_by_role("button", name=exact_name(x(context, name)))).to_have_count(0)
    expect(page.get_by_role("link", name=exact_name(x(context, name)))).to_have_count(0)


@then('I see the text "{text:Q}"')
def step_see_text(context, text):
    expect(pg(context).get_by_text(x(context, text)).first).to_be_visible()


@then('I see the link "{name:Q}"')
def step_see_link(context, name):
    expect(pg(context).get_by_role("link", name=exact_name(x(context, name))).first).to_be_visible()


@then("I do not see a page counter")
def step_no_counter(context):
    expect(pg(context).get_by_text(re.compile(r"Page \d+ of \d+"))).to_have_count(0)


@then('a status message shows "{text:Q}"')
@then('a status banner shows "{text:Q}"')
def step_status(context, text):
    expect(pg(context).get_by_role("status").filter(has_text=x(context, text)).first).to_be_visible()


def _one_alert(context, parts):
    page = pg(context)
    first = parts[0]
    alerts = page.get_by_role("alert").filter(has_text=first)
    expect(alerts.first).to_be_visible()
    expect(alerts).to_have_count(1)
    text = norm(alerts.first.inner_text())
    position = 0
    for part in parts:
        found = text.find(part, position)
        assert found >= 0, f"alert text {text!r} lacks {part!r} after position {position}"
        position = found + len(part)


@then('one alert shows "{text:Q}"')
def step_one_alert(context, text):
    _one_alert(context, [x(context, text)])


@then('one alert shows "{text:Q}" followed by "{detail:Q}"')
def step_one_alert_detail(context, text, detail):
    _one_alert(context, [x(context, text), x(context, detail)])


@then('one alert shows "{text:Q}" followed by "{second:Q}" and "{detail:Q}"')
def step_one_alert_two(context, text, second, detail):
    _one_alert(context, [x(context, text), x(context, second), x(context, detail)])


# =========================================================================== stubbed API answers (D24)
def _stub(context, status, method, path, body):
    target = "/v1" + x(context, path).strip()

    def matches(url):
        return urllib.parse.urlparse(url).path.rstrip("/") == target or \
            urllib.parse.urlparse(url).path.rstrip("/").endswith(target)

    def handler(route, request):
        if request.method.upper() != method.upper() or \
                not urllib.parse.urlparse(request.url).path.rstrip("/").endswith(target):
            route.fallback()
            return
        route.fulfill(status=status, content_type="application/problem+json" if status >= 400
                      else "application/json", body=json.dumps(body))

    context.ui.context.route(matches, handler)


def _problem(status, detail=None):
    titles = {403: "Forbidden", 404: "Not Found", 500: "Internal Server Error", 503: "Service Unavailable"}
    body = {"type": "about:blank", "title": titles.get(status, "Error"), "status": status}
    if detail is not None:
        body["detail"] = detail
    return body


@given('the API answers {status:d} with detail "{detail:Q}" to {method:w} {path}')
def step_stub_detail(context, status, detail, method, path):
    _stub(context, status, method, path, _problem(status, x(context, detail)))


@given('the API answers {status:d} to {method:w} {path}')
def step_stub(context, status, method, path):
    _stub(context, status, method, path, _problem(status))


@given("the API answers GET /vocabularies with an empty page")
def step_stub_empty(context):
    # The 'empty' example of the OpenAPI (GET /vocabularies).
    _stub(context, 200, "GET", "/vocabularies", {
        "page": 1, "totalPages": 0, "total": 0, "pageSize": 20,
        "self": "/vocabularies?page=1&pageSize=20", "first": "/vocabularies?page=1&pageSize=20", "items": []})


def _api_requests(context, method):
    page = pg(context)
    page.wait_for_timeout(800)
    return [url for m, url in context.ui.requests
            if m.upper() == method and "/v1/" in urllib.parse.urlparse(url).path + "/"]


@then("no POST request was sent")
def step_no_post(context):
    sent = _api_requests(context, "POST")
    assert not sent, f"POST requests were sent: {sent}"


@then("no PATCH request was sent")
def step_no_patch(context):
    sent = _api_requests(context, "PATCH")
    assert not sent, f"PATCH requests were sent: {sent}"


# =========================================================================== setup through the API
@given('vocabularies "{names:Q}" exist')
def step_vocabs_exist(context, names):
    for name in comma_list(names):
        response = context.vm.create_vocabulary(x(context, name))
        assert response.status_code == 201, f"{response.status_code} {response.text[:300]}"


@given("{count:d} vocabularies of this scenario exist")
def step_n_vocabs(context, count):
    for i in range(count):
        response = context.vm.create_vocabulary(f"V{i:02d}{context.vm.run}")
        assert response.status_code == 201, f"{response.status_code} {response.text[:300]}"


@then('the stored changelog of version {version:d} of "{name:Q}" is "{text:Q}"')
def step_stored_changelog(context, version, name, text):
    meta = api_json(context, f"/vocabularies/{x(context, name)}/versions/{version}")
    assert meta.get("changelog") == x(context, text), meta


@then('the content of version {new:d} of "{name:Q}" equals the content of version {old:d}')
def step_same_content(context, new, name, old):
    name = x(context, name)
    assert version_content(context, name, new) == version_content(context, name, old)


# =========================================================================== list: filters
@step('I filter "{label:Q}" with "{text:Q}"')
def step_filter(context, label, text):
    page = pg(context)
    box = field(page, label)
    box.fill(x(context, text))
    box.press("Enter")
    settle(page, 800)
    wait_rows(page)


@step('I choose "{value:Q}" in the filter "{label:Q}"')
def step_filter_choose(context, value, label):
    page = pg(context)
    control = field(page, label)
    if control.evaluate("e => e.tagName === 'SELECT'"):
        control.select_option(label=value)
    else:
        control.click()
        page.get_by_role("option", name=exact_name(value)).click()
    settle(page, 800)


@step('I set the filter "{label:Q}" to the UTC day of the stored creationDate of "{name:Q}"')
def step_filter_day(context, label, name):
    page = pg(context)
    day = api_json(context, f"/vocabularies/{x(context, name)}")["creationDate"][:10]
    context.vm.saved["filter_day"] = day
    control = field(page, label)
    control.fill(day)
    control.press("Enter")
    settle(page, 800)


# =========================================================================== list: table
@then('the table has the column headers "{names:Q}"')
@then('the version table has the column headers "{names:Q}"')
def step_headers(context, names):
    """The named columns come first, in this order. A further column is accepted only when it holds
    nothing but the row actions (buttons or links), as D210 asks for 'Download' and 'Remove' per row."""
    page = pg(context)
    tbl = table(page)
    expect(tbl).to_be_visible()
    wait_rows(page)
    expected = comma_list(names)
    found = headers(tbl)
    assert found[:len(expected)] == expected, found
    rows = data_rows(page, tbl)
    for extra in range(len(expected), len(found)):
        for i in range(min(rows.count(), 5)):
            cell = cells(rows.nth(i)).nth(extra)
            controls = " ".join(cell.get_by_role("button").all_inner_texts() + cell.get_by_role("link").all_inner_texts())
            assert norm(cell.inner_text()).replace(" ", "") == norm(controls).replace(" ", ""), \
                f"extra column '{found[extra]}' holds data: {cell.inner_text()!r}"


@then('the row of "{name:Q}" shows the status "{status:Q}", the latest version "{version:Q}" and the creator "{creator:Q}"')
def step_row_values(context, name, status, version, creator):
    page = pg(context)
    row = row_of(page, x(context, name)).first
    expect(row).to_be_visible()
    tbl = table(page)
    assert cell_text(tbl, row, "Status").startswith(status), cell_text(tbl, row, "Status")
    assert cell_text(tbl, row, "Latest version") == version, cell_text(tbl, row, "Latest version")
    assert cell_text(tbl, row, "Creator") == creator, cell_text(tbl, row, "Creator")


@then('the row of "{name:Q}" shows the status "{status:Q}"')
def step_row_status(context, name, status):
    page = pg(context)
    row = row_of(page, x(context, name)).first
    expect(row).to_be_visible()
    idx = column_index(table(page), "Status")
    expect(cells(row).nth(idx)).to_contain_text(status)


@then('the dates in the row of "{name:Q}" have the form "YYYY-MM-DD HH:mm UTC"')
def step_row_dates(context, name):
    page = pg(context)
    row = row_of(page, x(context, name)).first
    for column in ("Date created", "Last updated"):
        value = cell_text(table(page), row, column)
        assert DATE_FORM.match(value), f"{column}: {value!r}"


@then('the row of "{name:Q}" has a warning icon named "{label:Q}"')
def step_row_icon(context, name, label):
    page = pg(context)
    row = row_of(page, x(context, name)).first
    icon = row.get_by_role("img", name=exact_name(label)).or_(row.get_by_label(exact_name(label)))
    expect(icon.first).to_be_attached()


@then("the table shows {count:d} rows")
@then("the table shows {count:d} row")
def step_row_count(context, count):
    page = pg(context)
    wait_rows(page)
    expect(data_rows(page)).to_have_count(count)


@then('the version table shows {count:d} rows')
def step_version_row_count(context, count):
    page = pg(context)
    wait_rows(page)
    expect(data_rows(page)).to_have_count(count, timeout=30000)


@then('the table shows only "{name:Q}"')
def step_only(context, name):
    page = pg(context)
    expect(data_rows(page)).to_have_count(1)
    expect(row_of(page, x(context, name))).to_have_count(1)


@then('the table shows "{name:Q}"')
def step_shows(context, name):
    expect(row_of(pg(context), x(context, name)).first).to_be_visible()


@then('the table shows "{name:Q}" when its stored creationDate has the same UTC day')
def step_shows_same_day(context, name):
    name = x(context, name)
    day = api_json(context, f"/vocabularies/{name}")["creationDate"][:10]
    rows = row_of(pg(context), name)
    if day == context.vm.saved.get("filter_day"):
        expect(rows.first).to_be_visible()
    else:
        expect(rows).to_have_count(0)


def _names_in_order(context, expected):
    names = column_texts(pg(context), "Name")
    found = []
    for text in names:
        for name in expected:
            if re.search(r"(?<![A-Za-z0-9])" + re.escape(name) + r"(?![A-Za-z0-9])", text):
                found.append(name)
    return found


@then('the rows are in the order "{names:Q}"')
def step_order(context, names):
    expected = [x(context, n) for n in comma_list(names)]
    deadline = time.time() + 8
    found = []
    while time.time() < deadline:
        found = _names_in_order(context, expected)
        if found == expected:
            return
        pg(context).wait_for_timeout(300)
    raise AssertionError(f"order {found}, expected {expected}")


@then('the column header "{column:Q}" has aria-sort "{value:Q}"')
def step_aria_sort(context, column, value):
    expect(header_cell(table(pg(context)), column)).to_have_attribute("aria-sort", value)


@when('I press the sort button of the column "{column:Q}"')
def step_sort(context, column):
    page = pg(context)
    header_cell(table(page), column).get_by_role("button").click()
    settle(page, 600)


@then('the columns "{columns:Q}" have a sort button')
def step_sortable(context, columns):
    tbl = table(pg(context))
    for column in comma_list(columns):
        expect(header_cell(tbl, column).get_by_role("button")).to_have_count(1)


@then('the columns "{columns:Q}" have no sort button')
def step_not_sortable(context, columns):
    tbl = table(pg(context))
    for column in comma_list(columns):
        expect(header_cell(tbl, column).get_by_role("button")).to_have_count(0)


# =========================================================================== row menus (D72, D75)
def open_menu(context, name):
    page = pg(context)
    page.get_by_role("button", name=exact_name(name)).first.click()
    expect(page.get_by_role("menu").first).to_be_visible()


@when('I open the menu "{name:Q}"')
def step_open_menu(context, name):
    open_menu(context, x(context, name))


@step('I choose "{item:Q}" in the menu "{name:Q}"')
def step_menu_choose(context, item, name):
    open_menu(context, x(context, name))
    page = pg(context)
    page.get_by_role("menuitem", name=exact_name(item)).click()
    settle(page, 600)


@then("the menu offers {items}")
def step_menu_offers(context, items):
    page = pg(context)
    for item in quoted_list(items):
        expect(page.get_by_role("menuitem", name=exact_name(item))).to_have_count(1)


@then("the menu does not offer {items}")
def step_menu_not_offers(context, items):
    page = pg(context)
    expect(page.get_by_role("menu").first).to_be_visible()
    for item in quoted_list(items):
        expect(page.get_by_role("menuitem", name=exact_name(item))).to_have_count(0)


# =========================================================================== status dialog (D209)
def dialog(page, title=None):
    if title:
        return page.get_by_role("dialog", name=exact_name(title)).or_(
            page.get_by_role("alertdialog", name=exact_name(title))).first
    return page.get_by_role("dialog").or_(page.get_by_role("alertdialog")).first


@then('a dialog "{title:Q}" asks "{question:Q}" with focus on "{button:Q}"')
def step_dialog_asks(context, title, question, button):
    dlg = dialog(pg(context), title)
    expect(dlg).to_be_visible()
    expect(dlg).to_contain_text(x(context, question))
    expect(dlg.get_by_role("button", name=exact_name(button))).to_be_focused()


@then('a dialog asks "{question:Q}" with focus on "{button:Q}"')
def step_dialog_asks_untitled(context, question, button):
    dlg = dialog(pg(context))
    expect(dlg).to_be_visible()
    expect(dlg).to_contain_text(x(context, question))
    expect(dlg.get_by_role("button", name=exact_name(button))).to_be_focused()


@when("I close the dialog with the Cancel button")
def step_close_cancel(context):
    page = pg(context)
    dlg = dialog(page)
    expect(dlg).to_be_visible()
    dlg.get_by_role("button", name=exact_name("Cancel")).click()
    expect(dialog(page)).to_be_hidden()


@when("I close the dialog with the Escape key")
def step_close_escape(context):
    page = pg(context)
    expect(dialog(page)).to_be_visible()
    page.keyboard.press("Escape")
    expect(dialog(page)).to_be_hidden()


@when('I deprecate "{name:Q}" from the list and confirm')
def step_deprecate_confirm(context, name):
    page = pg(context)
    name = x(context, name)
    page.goto(UI_URL + "/vocabularies")
    settle(page)
    step_filter(context, "Name contains", name)
    open_menu(context, f"Actions for {name}")
    page.get_by_role("menuitem", name=exact_name("Deprecate")).click()
    dlg = dialog(page, "Update vocabulary status")
    expect(dlg).to_be_visible()
    dlg.get_by_role("button", name=exact_name("Confirm")).click()
    settle(page, 600)


# =========================================================================== keyboard (D209, Q6)
def _focused_name(page):
    return norm(page.evaluate(
        "() => { const e = document.activeElement; if (!e) return '';"
        " return e.getAttribute('aria-label') || e.innerText || e.value || ''; }"))


@then('I can reach "{first:Q}", the sort buttons and "{last:Q}" with the Tab key')
def step_tab_reach(context, first, last):
    page = pg(context)
    wanted = {x(context, first), "Name", "Status", "Date created", "Last updated", x(context, last)}
    page.evaluate("() => document.activeElement && document.activeElement.blur()")
    page.locator("body").focus()
    reached = []
    for _ in range(150):
        page.keyboard.press("Tab")
        name = _focused_name(page)
        reached.append(name)
        if wanted.issubset(set(reached)):
            return
    raise AssertionError(f"not reached by Tab: {sorted(wanted - set(reached))}; reached {reached[:60]}")


@then('I can open "{menu:Q}" and choose "{item:Q}" with the keyboard')
def step_keyboard_menu(context, menu, item):
    page = pg(context)
    menu = x(context, menu)
    page.get_by_role("button", name=exact_name(menu)).first.focus()
    page.keyboard.press("Enter")
    expect(page.get_by_role("menu").first).to_be_visible()
    for _ in range(12):
        if _focused_name(page) == item:
            break
        page.keyboard.press("ArrowDown")
    assert _focused_name(page) == item, f"focus is on {_focused_name(page)!r}"
    page.keyboard.press("Enter")
    expect(dialog(page)).to_be_visible()


@then('the dialog "{title:Q}" can be cancelled with the Escape key')
def step_dialog_escape(context, title):
    page = pg(context)
    expect(dialog(page, title)).to_be_visible()
    page.keyboard.press("Escape")
    expect(dialog(page, title)).to_be_hidden()


# =========================================================================== forms (D32, D33, D37, D60 ...)
def _fill(context, label, value):
    page = pg(context)
    value = x(context, value)
    if label == "Name":
        context.ui.form_name = value
    box = field(page, label)
    box.fill(value)


@step('I fill "{label:Q}" with "{value:Q}"')
def step_fill_one(context, label, value):
    _fill(context, label, value)


@step('I fill "{label:Q}" with "{value:Q}" and "{label2:Q}" with "{value2:Q}"')
def step_fill_two(context, label, value, label2, value2):
    _fill(context, label, value)
    _fill(context, label2, value2)


@step('I fill "{label:Q}" with "{value:Q}" and leave the field')
def step_fill_blur(context, label, value):
    _fill(context, label, value)
    field(pg(context), label).press("Tab")
    pg(context).wait_for_timeout(300)


@step('I fill "{label:Q}" with two lines "{first:Q}" and "{second:Q}"')
def step_fill_lines(context, label, first, second):
    field(pg(context), label).fill(x(context, first) + "\n" + x(context, second))


@step('I choose a valid Turtle file for "{label:Q}"')
def step_choose_valid(context, label):
    page = pg(context)
    if "external-vocabularies" in page.url:
        namespace = field(page, "Namespace URI").input_value() or context.vm.special("EXT")
        choose_file(context, label, ttl(context, "external_registration", namespace), "external.ttl")
    else:
        choose_file(context, label, ttl(context, "valid"))


@step('I choose a valid Turtle file of {size:d} bytes for "{label:Q}"')
def step_choose_size(context, size, label):
    choose_file(context, label, ttl(context, "size", size))


@step('I choose for "{label:Q}" a Turtle file with a syntax error on line 3')
def step_choose_syntax(context, label):
    choose_file(context, label, ttl(context, "syntax3"))


@step('I choose for "{label:Q}" a Turtle file with a literal value for an object property')
def step_choose_s1(context, label):
    choose_file(context, label, ttl(context, "s1"))


@step('I choose for "{label:Q}" a Turtle file with 3 semantic errors')
def step_choose_s3(context, label):
    choose_file(context, label, ttl(context, "s3errors"))


@step('I choose for "{label:Q}" a Turtle file that references version {version:d} of "{name:Q}"')
def step_choose_ref(context, label, version, name):
    choose_file(context, label, ttl(context, "reference", (x(context, name), version)))


@step('I choose for "{label:Q}" a Turtle file with an unregistered external namespace')
def step_choose_ext(context, label):
    choose_file(context, label, ttl(context, "external"))


def _label_text(page, label):
    """Visible text of the label element(s) whose text is the D60 label, with or without '*'."""
    return [norm(t) for t in page.locator("label").filter(has_text=label_name(label)).all_inner_texts()]


def _is_required(control):
    return control.evaluate(
        "e => e.getAttribute('aria-required') === 'true' || e.required === true")


@then("the form has the required fields {labels} marked with '*'")
def step_required_fields(context, labels):
    page = pg(context)
    for label in quoted_list(labels):
        control = field(page, label)
        expect(control).to_be_visible()
        texts = _label_text(page, label)
        assert any(re.fullmatch(re.escape(label) + r"\s*\*", t) for t in texts), \
            f"label of {label}: {texts}"
        assert _is_required(control), f"{label} has no aria-required='true' (D141)"


@then('the form has no field "{label:Q}" and no field for a version')
def step_no_field_no_version(context, label):
    page = pg(context)
    expect(page.get_by_label(label_name(label))).to_have_count(0)
    step_no_version_field(context)


@then("the form has no field for a version")
def step_no_version_field(context):
    page = pg(context)
    for role in ("textbox", "spinbutton", "combobox"):
        expect(page.get_by_role(role, name=re.compile(r"\bversion\b", re.I))).to_have_count(0)


@then('the form has the buttons "{first:Q}" and "{second:Q}"')
def step_form_buttons(context, first, second):
    page = pg(context)
    for name in (first, second):
        expect(action(page, name)).to_be_visible()


def _has_field_error(control):
    return control.evaluate(
        """e => {
             if (e.getAttribute('aria-invalid') === 'true') return true;
             const ids = ((e.getAttribute('aria-errormessage') || '') + ' ' +
                          (e.getAttribute('aria-describedby') || '')).split(/\\s+/).filter(Boolean);
             return ids.some(id => { const d = document.getElementById(id);
                                     return d && d.innerText.trim().length > 0 && d.offsetParent !== null; });
           }""")


@then("each of {labels} shows a field error")
def step_each_error(context, labels):
    page = pg(context)
    page.wait_for_timeout(300)
    missing = [label for label in quoted_list(labels) if not _has_field_error(field(page, label))]
    assert not missing, f"no field error on {missing}"


@then('the field "{label:Q}" shows a field error')
def step_field_error(context, label):
    page = pg(context)
    page.wait_for_timeout(300)
    assert _has_field_error(field(page, label)), f"no field error on {label}"


@then('the field "{label:Q}" shows the error "{text:Q}"')
def step_field_error_text(context, label, text):
    page = pg(context)
    expect(page.get_by_text(text).first).to_be_visible()
    assert _has_field_error(field(page, label)), f"the error is not tied to {label}"


@then('the focus is on the field "{label:Q}"')
def step_focus(context, label):
    expect(field(pg(context), label)).to_be_focused()


@then('the field "{label:Q}" still holds "{value:Q}"')
@then('the field "{label:Q}" holds "{value:Q}"')
def step_field_holds(context, label, value):
    expect(field(pg(context), label)).to_have_value(x(context, value))


@then('the field "{label:Q}" shows "{text:Q}"')
def step_field_shows(context, label, text):
    expect(pg(context).get_by_text(x(context, text)).first).to_be_visible()


@then('the field "{label:Q}" is empty and required')
def step_field_empty_required(context, label):
    control = field(pg(context), label)
    expect(control).to_have_value("")
    assert _is_required(control), f"{label} is not required"


@then('the button "{name:Q}" is disabled')
def step_disabled(context, name):
    expect(pg(context).get_by_role("button", name=exact_name(name))).to_be_disabled()


@then('the button "{name:Q}" is enabled')
def step_enabled(context, name):
    expect(pg(context).get_by_role("button", name=exact_name(name))).to_be_enabled()


@then("the upload check shows an error on line {line:d}")
def step_upload_line(context, line):
    expect(pg(context).get_by_text(re.compile(r"\bline " + str(line) + r"\b", re.I)).first).to_be_visible()


# --------------------------------------------------------------------------- report regions (D90, D109)
def region(page, name):
    return page.get_by_role("region", name=exact_name(name))


@then('the region "{name:Q}" starts with an alert "{text:Q}"')
def step_region_alert(context, name, text):
    reg = region(pg(context), name)
    expect(reg).to_be_visible()
    expect(reg.get_by_role("alert").first).to_have_text(exact_name(text))


@then('the region "{name:Q}" starts with a status line "{text:Q}"')
def step_region_status(context, name, text):
    reg = region(pg(context), name)
    expect(reg).to_be_visible()
    expect(reg.get_by_role("status").first).to_have_text(exact_name(text))


@then('the region "{name:Q}" lists "{detail:Q}" with its location')
def step_region_lists_location(context, name, detail):
    reg = region(pg(context), x(context, name))
    item = reg.get_by_role("listitem").filter(has_text=x(context, detail)).first
    expect(item).to_be_visible()
    expect(item).to_contain_text("statement")


@then('the region "{name:Q}" lists {count:d} issues with detail and location')
def step_region_lists_n(context, name, count):
    reg = region(pg(context), name)
    items = reg.get_by_role("listitem")
    expect(items).to_have_count(count)
    details = ("Literal value for object property", "Resource value for datatype property",
               "Duplicate preferred label for the same language")
    for i in range(count):
        text = norm(items.nth(i).inner_text())
        hit = [d for d in details if d in text]
        assert hit and len(text) > len(hit[0]) + 5, f"issue {i + 1} has no detail and location: {text!r}"


@then('the region "{name:Q}" lists "{text:Q}"')
def step_region_lists(context, name, text):
    expect(region(pg(context), name)).to_contain_text(x(context, text))


@then('the region "{name:Q}" is shown')
def step_region_shown(context, name):
    expect(region(pg(context), name)).to_be_visible()


@then('the region "{name:Q}" is not shown')
def step_region_hidden(context, name):
    pg(context).wait_for_timeout(300)
    expect(region(pg(context), name)).to_have_count(0)


# =========================================================================== details page (D35, D36, D163)
@then('the header shows the name "{name:Q}", the status "{status:Q}" and the latest description')
def step_header(context, name, status):
    page = pg(context)
    name = x(context, name)
    latest = api_json(context, f"/vocabularies/{name}")
    expect(page.get_by_role("heading", level=1, name=exact_name(name))).to_be_visible()
    outside = norm(page.evaluate(
        "() => { const m = (document.querySelector('main') || document.body).cloneNode(true);"
        " m.querySelectorAll('table, [role=region], section[aria-label]').forEach(n => n.remove());"
        " document.body.appendChild(m); const t = m.innerText; m.remove(); return t; }"))
    assert status in outside, f"status {status} not in the header: {outside[:400]!r}"
    assert norm(latest["description"]) in outside, f"description not in the header: {outside[:400]!r}"


@then('the version rows are in the order "{versions:Q}"')
def step_version_order(context, versions):
    page = pg(context)
    wait_rows(page)
    assert column_texts(page, "Version") == comma_list(versions), column_texts(page, "Version")


@then('the version statuses are "{statuses:Q}"')
def step_version_statuses(context, statuses):
    page = pg(context)
    found = column_texts(page, "Status")
    expected = comma_list(statuses)
    assert len(found) == len(expected) and all(f.startswith(e) for f, e in zip(found, expected)), found


@then('the row of version {version:d} shows the status "{status:Q}"')
def step_version_row_status(context, version, status):
    page = pg(context)
    wait_rows(page)
    row = version_row(page, version)
    assert cell_text(table(page), row, "Status").startswith(status), cell_text(table(page), row, "Status")


@then('the version list shows version {new:d} as "{s1:Q}" and version {old:d} as "{s2:Q}"')
def step_version_list_two(context, new, s1, old, s2):
    step_version_row_status(context, new, s1)
    step_version_row_status(context, old, s2)


@when("I select the version {version:d}")
def step_select_version(context, version):
    page = pg(context)
    wait_rows(page)
    version_row(page, version).get_by_role("link", name=exact_name(str(version))).click()
    settle(page)


@then('the row of version {version:d} has aria-current "{value:Q}"')
def step_aria_current(context, version, value):
    expect(version_row(pg(context), version)).to_have_attribute("aria-current", value)


@then('the region "{name:Q}" shows the version, status "{status:Q}", date created, creator "{creator:Q}", description and change log')
def step_panel(context, name, status, creator):
    page = pg(context)
    version = int(re.search(r"\d+", name).group())
    vocabulary = re.search(r"/vocabularies/([A-Za-z0-9]+)/versions/", page.url).group(1)
    meta = api_json(context, f"/vocabularies/{vocabulary}/versions/{version}")
    reg = region(page, name)
    expect(reg).to_be_visible()
    text = norm(reg.inner_text())
    for value in (str(version), status, ui_date(meta["creationDate"]), creator,
                  norm(meta["description"]), norm(meta["changelog"])):
        assert value in text, f"{value!r} not in region {name}: {text[:500]!r}"


@then('the region "{name:Q}" has the button "{button:Q}"')
def step_region_button(context, name, button):
    expect(region(pg(context), name).get_by_role("button", name=exact_name(button))).to_be_visible()


@then('the region "{name:Q}" shows "{text:Q}"')
def step_region_shows(context, name, text):
    expect(region(pg(context), name)).to_contain_text(x(context, text))


@then('the region "{name:Q}" has the link "{link:Q}" to "{route:Q}"')
def step_region_link(context, name, link, route):
    anchor = region(pg(context), name).get_by_role("link", name=exact_name(link))
    expect(anchor).to_be_visible()
    href = anchor.get_attribute("href") or ""
    assert urllib.parse.urlparse(urllib.parse.urljoin(UI_URL + "/", href)).path == x(context, route), href


@then('the region "{name:Q}" has no "{link:Q}" link')
def step_region_no_link(context, name, link):
    reg = region(pg(context), name)
    expect(reg).to_be_visible()
    expect(reg.get_by_role("link", name=re.compile(re.escape(link)))).to_have_count(0)


@then('the row of version {version:d} shows "{text:Q}" and the link "{link:Q}"')
def step_version_row_warning(context, version, text, link):
    row = version_row(pg(context), version)
    expect(row).to_contain_text(text)
    expect(row.get_by_role("link", name=exact_name(link))).to_be_visible()


# =========================================================================== downloads (D29)
@then('a file "{filename:Q}" is downloaded with the content of version {version:d} of "{name:Q}"')
def step_download(context, filename, version, name):
    page = pg(context)
    deadline = time.time() + 20
    while not context.ui.downloads and time.time() < deadline:
        page.wait_for_timeout(200)
    assert context.ui.downloads, "no download started"
    download = context.ui.downloads[-1]
    assert download.suggested_filename == x(context, filename), download.suggested_filename
    with open(download.path(), "rb") as handle:
        data = handle.read()
    assert data == context.vm.uploads[x(context, name)][version], "downloaded bytes differ from the upload"


# =========================================================================== external vocabularies (D210)
@then('"{first:Q}" is listed before "{second:Q}", paging with "Next page" where needed')
def step_ext_before(context, first, second):
    page = pg(context)
    first, second = x(context, first), x(context, second)
    positions = {}
    offset = 0
    for _ in range(500):
        wait_rows(page)
        names = column_texts(page, "Name")
        for i, text in enumerate(names):
            for name in (first, second):
                if text == name and name not in positions:
                    positions[name] = offset + i
        if len(positions) == 2:
            break
        offset += len(names)
        button = next_button(page)
        if button.count() == 0 or not button.is_enabled():
            break
        button.click()
        settle(page, 200)
    assert len(positions) == 2, f"not both found: {positions}"
    assert positions[first] < positions[second], positions


@then('the row of "{name:Q}", found by paging, has the action "{name2:Q}"')
def step_ext_row_action(context, name, name2):
    row = find_by_paging(context, x(context, name), route="/external-vocabularies")
    expect(action(row, name2)).to_be_visible()


@then('the row of "{name:Q}", found by paging, has no action "{name2:Q}"')
def step_ext_row_no_action(context, name, name2):
    row = find_by_paging(context, x(context, name), route="/external-vocabularies")
    expect(row.get_by_role("button", name=exact_name(name2))).to_have_count(0)
    expect(row.get_by_role("link", name=exact_name(name2))).to_have_count(0)


@when('I page with "Next page" until the row of "{name:Q}" appears')
def step_page_until(context, name):
    find_by_paging(context, x(context, name))


@when('I choose "{name2:Q}" for "{name:Q}"')
def step_row_action(context, name2, name):
    page = pg(context)
    action(row_of(page, x(context, name)).first, name2).click()
    page.wait_for_timeout(300)


# =========================================================================== accessibility (Q6, NFR-10, D123)
@then("axe reports no WCAG 2.1 AA violations")
def step_axe(context):
    from axe_playwright_python.sync_playwright import Axe

    page = pg(context)
    settle(page)
    results = Axe().run(page, options={"runOnly": {"type": "tag",
                                                   "values": ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]}})
    violations = results.response.get("violations", [])
    os.makedirs(AXE_REPORT_DIR, exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9]+", "-", urllib.parse.urlparse(page.url).path.replace(context.vm.run, "")).strip("-")
    with open(os.path.join(AXE_REPORT_DIR, f"{slug or 'root'}.json"), "w") as handle:
        json.dump({"url": page.url, "violations": violations,
                   "passes": len(results.response.get("passes", []))}, handle, indent=2)
    summary = [f"{v['id']} ({v.get('impact')}): {len(v.get('nodes', []))} nodes" for v in violations]
    assert not violations, "axe violations: " + "; ".join(summary)
