"""Browser steps of the Vocabulary Manager suite (step 60), Playwright for Python.

Elements are found only by decided routes (D31, D74), decided labels and texts (D60, D72,
D34, D35, D36, D18), the decided semantics of D75 and D90 (tables with column headers,
'Actions for <Name>' menu buttons, regions, role status and role alert), and the standard
Keycloak login page of E1. Nothing is taken from the build: no test ids, no CSS classes.

Every scenario gets a new browser context: no stored sessions, cookies or local storage.
API stubs (D24) intercept the browser's fetch and XHR calls to the API path of E2 (/v1/...).
"""
import json
import os
import re
import time
from urllib.parse import urlparse

import parse
from behave import given, when, then, register_type

import vmtest as vm


@parse.with_pattern(r'[^"]*')
def _parse_quoted(text):
    return text


register_type(Q=_parse_quoted)

UI_URL = vm.env("UI_URL", "http://localhost:4433").rstrip("/")
UI_TIMEOUT_MS = int(float(vm.env("UI_TIMEOUT_S", "15")) * 1000)
HEADLESS = vm.env("UI_HEADED", "") == ""
KEYCLOAK_ORIGIN = "{0.scheme}://{0.netloc}".format(urlparse(vm.TOKEN_URL))
API_PREFIX = "/v1"  # E2: the API lives under /v1; the UI proxies it (build profile)
REPORTS = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "..", "reports"))

_pw = {"playwright": None, "browser": None}


def X(context, text):
    return vm.expand(context, text)


def split_list(text):
    return [p.strip() for p in text.split(", ") if p.strip()]


def norm(text):
    return re.sub(r"\s+", " ", text or "").strip()


def retry(fn, what, timeout_ms=UI_TIMEOUT_MS):
    """Call fn until it returns without AssertionError or the time is up."""
    deadline = time.time() + timeout_ms / 1000
    last = None
    while True:
        try:
            return fn()
        except AssertionError as exc:
            last = exc
        if time.time() > deadline:
            raise AssertionError(f"{what}: {last}")
        time.sleep(0.25)


# ================================================================ browser lifecycle

def browser():
    if _pw["browser"] is None:
        from playwright.sync_api import sync_playwright
        _pw["playwright"] = sync_playwright().start()
        _pw["browser"] = _pw["playwright"].chromium.launch(headless=HEADLESS)
    return _pw["browser"]


def stop_browser():
    if _pw["browser"] is not None:
        _pw["browser"].close()
        _pw["playwright"].stop()
        _pw["browser"] = _pw["playwright"] = None


def close_page(context):
    ctx = getattr(context, "ui_context", None)
    if ctx is not None:
        try:
            ctx.close()
        except Exception:  # noqa: BLE001
            pass
    context.ui_context = None
    context.page = None


vm.ui_hooks = {"close": lambda context: close_page(context), "stop": lambda: stop_browser()}


def page(context):
    if getattr(context, "page", None) is None:
        new_page(context)
    return context.page


def new_page(context):
    close_page(context)
    # a fresh context: no storage_state, so no stored browser session (step 60 rule)
    ctx = browser().new_context(accept_downloads=True, viewport={"width": 1400, "height": 1000})
    ctx.set_default_timeout(UI_TIMEOUT_MS)
    pg = ctx.new_page()
    context.ui_context = ctx
    context.page = pg
    context.ui_requests = []
    context.ui_downloads = []
    context.ui_stubs = []
    pg.on("request", lambda rq: context.ui_requests.append((rq.method, rq.url, rq.resource_type)))
    pg.on("download", lambda d: context.ui_downloads.append(d))
    ctx.route("**/*", lambda route, request: _route(context, route, request))
    return pg


def _api_path(url):
    path = urlparse(url).path
    i = path.find(API_PREFIX + "/")
    return path[i + len(API_PREFIX):] if i >= 0 else None


def _route(context, route, request):
    if request.resource_type in ("fetch", "xhr"):
        path = _api_path(request.url)
        for method, stub_path, status, detail in context.ui_stubs:
            if path is not None and request.method == method and path == stub_path:
                body = {"type": "about:blank", "title": "Stubbed", "status": status, "detail": detail}
                route.fulfill(status=status, content_type="application/problem+json", body=json.dumps(body))
                return
    route.continue_()


def settle(context, ms=400):
    pg = page(context)
    try:
        pg.wait_for_load_state("networkidle", timeout=UI_TIMEOUT_MS)
    except Exception:  # noqa: BLE001
        pass
    pg.wait_for_timeout(ms)


def on_keycloak(pg):
    return pg.url.startswith(KEYCLOAK_ORIGIN)


def goto(context, route):
    pg = page(context)
    pg.goto(UI_URL + route)
    settle(context, 200)  # the app may redirect to Keycloak from script (D18)
    if getattr(context, "ui_user", None) and on_keycloak(pg):
        _login(pg, context.ui_user)
        settle(context)


def _login(pg, user):
    # the standard Keycloak login page of realm 'simpl' (E1)
    pg.get_by_label("Username or email").fill(user)
    pg.get_by_label("Password", exact=True).fill(vm.USER_PASSWORD)
    pg.get_by_role("button", name="Sign In").click()
    pg.wait_for_url(lambda u: not u.startswith(KEYCLOAK_ORIGIN), timeout=UI_TIMEOUT_MS)


# ================================================================ element helpers

def field(context, label):
    return page(context).get_by_label(label, exact=True)


def button(context, name, scope=None):
    return (scope or page(context)).get_by_role("button", name=name, exact=True)


def table_data(context, header):
    """The table that has the column header `header`: header names and rows as cell texts."""
    pg = page(context)
    tables = pg.get_by_role("table").filter(has=pg.get_by_role("columnheader", name=header, exact=True))
    if tables.count() == 0:
        raise AssertionError(f"no table with the column header '{header}'")
    return tables.first.evaluate(
        """t => {
            const txt = e => (e.innerText || e.textContent || '').replace(/\\s+/g, ' ').trim();
            const heads = [...t.querySelectorAll('th, [role=columnheader]')].map(txt);
            const rows = [...t.querySelectorAll('tr, [role=row]')]
              .map(r => [...r.querySelectorAll(':scope > td, :scope > [role=cell], :scope > [role=gridcell]')].map(txt))
              .filter(c => c.length > 0);
            return {heads, rows};
        }""")


def column(data, header):
    try:
        return data["heads"].index(header)
    except ValueError:
        raise AssertionError(f"no column '{header}' in {data['heads']}")


def list_names(context):
    data = table_data(context, "Name")
    i = column(data, "Name")
    return [r[i] for r in data["rows"] if len(r) > i]


def list_row(context, name):
    pg = page(context)
    table = pg.get_by_role("table").filter(has=pg.get_by_role("columnheader", name="Name", exact=True)).first
    rows = table.get_by_role("row").filter(has=pg.get_by_role("cell", name=name, exact=True))
    if rows.count() == 0:
        raise AssertionError(f"no row for '{name}' in the list; rows: {list_names(context)}")
    return rows.first


def leading_int(text):
    m = re.match(r"\s*(\d+)", text)
    return m.group(1) if m else None


def version_numbers(context):
    data = table_data(context, "Version")
    i = column(data, "Version")
    return [leading_int(r[i]) for r in data["rows"] if len(r) > i]


def version_row(context, n):
    pg = page(context)
    table = pg.get_by_role("table").filter(has=pg.get_by_role("columnheader", name="Version", exact=True)).first
    rows = table.get_by_role("row")
    for k in range(rows.count()):
        row = rows.nth(k)
        cells = row.get_by_role("cell")
        if cells.count() and leading_int(cells.first.inner_text()) == str(n):
            return row
    raise AssertionError(f"no row for version {n}; versions: {version_numbers(context)}")


def choose_menu_item(context, menu, item):
    pg = page(context)
    button(context, menu).click()
    for role in ("menuitem", "button", "link"):
        loc = pg.get_by_role(role, name=item, exact=True)
        if loc.count() and loc.first.is_visible():
            loc.first.click()
            return
    raise AssertionError(f"the menu '{menu}' has no item '{item}'")


def menu_items(context, menu):
    pg = page(context)
    button(context, menu).click()
    pg.wait_for_timeout(300)
    menus = pg.get_by_role("menu")
    names = []
    for k in range(menus.count()):
        if menus.nth(k).is_visible():
            items = menus.nth(k).get_by_role("menuitem")
            names += [norm(items.nth(j).inner_text()) for j in range(items.count())]
    pg.keyboard.press("Escape")
    return names


def submit_filters(context, loc):
    # submit the filter form the way a user does (Enter); also covers filters applied on change
    loc.evaluate("e => { if (e.form) { e.form.requestSubmit ? e.form.requestSubmit() : e.form.submit(); } }")
    settle(context)


def field_error_text(context, label):
    """Text shown under a field (D37): texts after the field inside its own wrapper (the largest
    ancestor that holds no other form control), plus aria-errormessage and aria-describedby
    targets. The name of a chosen file is not an error and is left out."""
    loc = field(context, label)
    if loc.count() == 0:
        raise AssertionError(f"no field '{label}'")
    return loc.first.evaluate(
        """e => {
            const txt = n => (n.innerText || n.textContent || '').replace(/\\s+/g, ' ').trim();
            const files = e.files ? [...e.files].map(f => f.name) : [];
            const keep = t => t && !files.includes(t);
            const ctl = 'input,textarea,select';
            let wrap = e;
            while (wrap.parentElement && wrap.parentElement.tagName !== 'FORM'
                   && wrap.parentElement.querySelectorAll(ctl).length === 1) wrap = wrap.parentElement;
            const out = [];
            for (const n of wrap.querySelectorAll('*')) {
              if (n === e || n.tagName === 'LABEL' || n.children.length) continue;
              if (!(e.compareDocumentPosition(n) & Node.DOCUMENT_POSITION_FOLLOWING)) continue;
              const t = txt(n); if (keep(t)) out.push(t);
            }
            const ids = ((e.getAttribute('aria-errormessage') || '') + ' ' + (e.getAttribute('aria-describedby') || ''))
              .split(/\\s+/).filter(Boolean);
            for (const i of ids) { const n = document.getElementById(i); if (n && keep(txt(n)) && !out.includes(txt(n))) out.push(txt(n)); }
            return out.join(' | ');
        }""")


def file_payload(context, fixture, filename=None):
    if fixture == "valid":
        data = vm.valid_turtle(context)
        name = filename or "valid.ttl"
    else:
        data = vm.fixture(context, fixture)
        name = filename or fixture
    return {"name": name, "mimeType": "text/turtle", "buffer": data}


def attach(context, label, payload):
    field(context, label).set_input_files(payload)
    settle(context)


# ================================================================ sign-in and stubs

@given('the user is signed in as "{user:Q}"')
def step_signed_in(context, user):
    context.ui_user = user
    context.user = user
    new_page(context)
    goto(context, "/")
    if on_keycloak(page(context)):
        raise AssertionError(f"sign-in of {user} did not leave the login page")


@given('the user is not signed in')
def step_not_signed_in(context):
    context.ui_user = None
    new_page(context)


@given('the UI API stub answers {status:d} on "{call:Q}" with the detail "{detail:Q}"')
def step_stub(context, status, call, detail):
    method, path = X(context, call).split(" ", 1)
    page(context)
    context.ui_stubs.append((method, path, status, X(context, detail)))


# ================================================================ navigation

@when('the user opens the route "{route:Q}"')
def step_open_route(context, route):
    goto(context, X(context, route))


@when('the user opens the vocabulary list')
def step_open_list(context):
    goto(context, "/vocabularies")


@when('the user opens the vocabulary list filtered on "{text:Q}"')
@when('the user filters the vocabulary list on the name "{text:Q}"')
def step_filter_name(context, text):
    goto(context, "/vocabularies")
    loc = field(context, "Name contains")
    loc.fill(X(context, text))
    loc.press("Enter")
    settle(context)


@when('the user chooses "{option:Q}" in the filter "{label:Q}"')
def step_choose_filter(context, option, label):
    loc = field(context, label)
    loc.select_option(label=option)
    submit_filters(context, loc)


@when('the user clicks the column header "{header:Q}"')
def step_click_header(context, header):
    pg = page(context)
    head = pg.get_by_role("columnheader", name=header, exact=True).first
    inner = head.get_by_role("button")
    (inner.first if inner.count() else head).click()
    settle(context)


@when('the user chooses "{item:Q}" in the menu "{menu:Q}"')
def step_choose_menu(context, item, menu):
    choose_menu_item(context, X(context, menu), X(context, item))
    settle(context)


@when('the user opens the details page of "{name:Q}"')
def step_open_details(context, name):
    goto(context, f"/vocabularies/{X(context, name)}")


@when('the user selects the version "{n:Q}"')
def step_select_version(context, n):
    row = retry(lambda: version_row(context, n), f"version {n} row")
    cell = row.get_by_role("cell").first
    target = cell.get_by_role("button")
    if not target.count():
        target = cell.get_by_role("link")
    (target.first if target.count() else cell).click()
    settle(context)


@when('the user clicks the button "{name:Q}"')
def step_click_button(context, name):
    if "Submit" == name:
        context.ui_requests_before_submit = len(context.ui_requests)
    button(context, X(context, name)).click()
    settle(context)


@when('the user opens the creation form')
def step_open_create(context):
    goto(context, "/vocabularies/new")


@when('the user fills "{label:Q}" with "{value:Q}"')
def step_fill(context, label, value):
    field(context, label).fill(X(context, value))


@when('the user attaches a file of {size:d} bytes to "{label:Q}"')
def step_attach_size(context, size, label):
    attach(context, label, {"name": "big.ttl", "mimeType": "text/turtle", "buffer": vm.turtle_of_size(context, size)})


@when('the user attaches the file "{fixture:Q}" to "{label:Q}"')
def step_attach_file(context, fixture, label):
    attach(context, label, file_payload(context, fixture))


def _submit(context):
    btn = button(context, "Submit")
    retry(lambda: _assert(btn.is_enabled(), "Submit is disabled"), "Submit enabled")
    context.ui_requests_before_submit = len(context.ui_requests)
    btn.click()
    settle(context, 800)


def _assert(cond, msg):
    if not cond:
        raise AssertionError(msg)


@when('the user creates a vocabulary named "{name:Q}" with the description "{description:Q}" and the file "{fixture:Q}"')
def step_ui_create(context, name, description, fixture):
    goto(context, "/vocabularies/new")
    field(context, "Name").fill(X(context, name))
    field(context, "Description").fill(X(context, description))
    attach(context, "Vocabulary file", file_payload(context, fixture))
    _submit(context)


def _new_version(context, name, changelog, payload):
    goto(context, f"/vocabularies/{X(context, name)}/versions/new")
    retry(lambda: _assert(field(context, "Change log").count() > 0, "no field 'Change log'"), "new version form")
    field(context, "Change log").fill(X(context, changelog))
    attach(context, "Vocabulary file", payload)
    _submit(context)


@when('the user creates a new version of "{name:Q}" with the change log "{changelog:Q}" and the file "{fixture:Q}"')
def step_ui_new_version(context, name, changelog, fixture):
    _new_version(context, name, changelog, file_payload(context, fixture))


@when('the user creates a new version of "{name:Q}" referencing version {n:d} of "{ref:Q}"')
def step_ui_new_version_ref(context, name, n, ref):
    ns = f"{vm.content_base(context)}/content/{X(context, ref)}/versions/{n}#"
    data = vm.valid_turtle(context, prefixes=[("legacy", ns)])
    _new_version(context, name, "Reference.", {"name": "ref.ttl", "mimeType": "text/turtle", "buffer": data})


@when('the user goes to the next page')
def step_next_page(context):
    next_control(context).click()
    settle(context)


def next_control(context):
    # no decision names the page controls (D34); a control whose name says 'next' is assumed
    pg = page(context)
    for role in ("button", "link"):
        loc = pg.get_by_role(role, name=re.compile(r"next", re.I))
        if loc.count():
            return loc.first
    raise AssertionError("no page control named 'next'")


# ================================================================ checks: pages and texts

@then('the UI shows the route "{route:Q}"')
def step_route(context, route):
    want = X(context, route)
    retry(lambda: _assert(urlparse(page(context).url).path.rstrip("/") == want.rstrip("/"),
                          f"route is {urlparse(page(context).url).path}, expected {want}"), "route")


@then('the UI shows the heading "{text:Q}"')
def step_heading(context, text):
    t = X(context, text)
    loc = page(context).get_by_role("heading", name=t, exact=True)
    retry(lambda: _assert(loc.count() > 0 and loc.first.is_visible(), f"no heading '{t}'"), "heading")


@then('the UI shows the text "{text:Q}"')
def step_text(context, text):
    t = X(context, text)
    loc = page(context).get_by_text(t, exact=False)
    retry(lambda: _assert(any(loc.nth(i).is_visible() for i in range(loc.count())),
                          f"text '{t}' not visible"), "text")


@then('the UI shows the link "{text:Q}"')
def step_link(context, text):
    t = X(context, text)
    loc = page(context).get_by_role("link", name=t, exact=True)
    retry(lambda: _assert(loc.count() > 0, f"no link '{t}'"), "link")


@then('the UI shows the menu item "{text:Q}"')
def step_menu_item(context, text):
    pg = page(context)
    nav = pg.get_by_role("navigation")
    loc = nav.get_by_role("link", name=text, exact=True)
    if loc.count() == 0:
        loc = nav.get_by_text(text, exact=True)
    retry(lambda: _assert(loc.count() > 0, f"no menu item '{text}' in a navigation landmark"), "menu item")


def _alerts(context):
    loc = page(context).get_by_role("alert")
    return [norm(loc.nth(i).inner_text()) for i in range(loc.count()) if loc.nth(i).is_visible()]


@then('the UI shows an alert containing "{text:Q}"')
def step_alert_contains(context, text):
    t = X(context, text)
    retry(lambda: _assert(any(t in a for a in _alerts(context)), f"no alert containing '{t}'; alerts: {_alerts(context)}"),
          "alert")


@then('the UI shows an alert "{text:Q}"')
def step_alert(context, text):
    t = X(context, text)
    retry(lambda: _assert(any(a == t for a in _alerts(context)), f"no alert '{t}'; alerts: {_alerts(context)}"), "alert")


def _statuses(context):
    loc = page(context).get_by_role("status")
    return [norm(loc.nth(i).inner_text()) for i in range(loc.count())]


@then('the UI shows a status message "{text:Q}"')
def step_status(context, text):
    t = X(context, text)
    retry(lambda: _assert(any(t in s for s in _statuses(context)),
                          f"no status message '{t}'; status messages: {_statuses(context)}; alerts: {_alerts(context)}"),
          "status message")


@then('the UI shows the banner "{text:Q}"')
def step_banner(context, text):
    step_text(context, text)


@then('the UI shows the Keycloak login page')
def step_keycloak(context):
    pg = page(context)
    retry(lambda: _assert(on_keycloak(pg), f"not on Keycloak: {pg.url}"), "Keycloak login page")
    if pg.get_by_label("Password", exact=True).count() == 0:
        raise AssertionError("the Keycloak page has no password field")


# ================================================================ checks: tables

@then('the UI shows a table with the column headers "{headers:Q}"')
def step_table_headers(context, headers):
    want = split_list(headers)

    def check():
        data = table_data(context, want[0])
        missing = [h for h in want if h not in data["heads"]]
        _assert(not missing, f"missing headers {missing}; headers are {data['heads']}")
    retry(check, "table headers")


@then('the UI shows a row for "{name:Q}" with the version "{version:Q}" and the status "{status:Q}"')
def step_row(context, name, version, status):
    n = X(context, name)

    def check():
        data = table_data(context, "Name")
        iname, iver, ist = column(data, "Name"), column(data, "Latest version"), column(data, "Status")
        rows = [r for r in data["rows"] if len(r) > max(iname, iver, ist) and r[iname] == n]
        _assert(rows, f"no row for {n}")
        r = rows[0]
        _assert(r[iver] == version and r[ist].startswith(status),
                f"row {n}: version '{r[iver]}', status '{r[ist]}'")
    retry(check, "list row")


@then('the UI shows only the rows "{names:Q}"')
def step_only_rows(context, names):
    want = [X(context, n) for n in split_list(names)]
    retry(lambda: _assert(sorted(list_names(context)) == sorted(want), f"rows are {list_names(context)}"), "rows")


@then('the UI shows the rows in the order "{names:Q}"')
def step_rows_order(context, names):
    want = [X(context, n) for n in split_list(names)]
    retry(lambda: _assert(list_names(context) == want, f"rows are {list_names(context)}"), "row order")


@then('the UI shows {n:d} rows')
def step_n_rows(context, n):
    retry(lambda: _assert(len(list_names(context)) == n, f"{len(list_names(context))} rows"), "row count")


@then('the UI shows the page controls')
def step_page_controls(context):
    retry(lambda: next_control(context), "page controls")


@then('the UI shows in the row "{name:Q}" a date matching "{pattern:Q}" under "{header:Q}"')
def step_row_date(context, name, pattern, header):
    n = X(context, name)

    def check():
        data = table_data(context, "Name")
        iname, icol = column(data, "Name"), column(data, header)
        rows = [r for r in data["rows"] if len(r) > icol and r[iname] == n]
        _assert(rows, f"no row for {n}")
        _assert(re.match(pattern, rows[0][icol]), f"'{rows[0][icol]}' does not match {pattern}")
    retry(check, "date")


@then('the UI shows a warning icon named "{text:Q}" in the row "{name:Q}"')
def step_warning_icon(context, text, name):
    def check():
        row = list_row(context, X(context, name))
        found = row.get_by_role("img", name=text, exact=True).count() + row.get_by_label(text, exact=True).count() \
            + row.get_by_title(text, exact=True).count()
        _assert(found > 0, f"no element named '{text}' in the row")
    retry(check, "warning icon")


@then('the UI shows the versions in the order "{versions:Q}"')
def step_versions_order(context, versions):
    want = split_list(versions)
    retry(lambda: _assert(version_numbers(context) == want, f"versions are {version_numbers(context)}"), "versions")


@then('the UI shows the status "{status:Q}" for the versions "{versions:Q}"')
def step_version_status(context, status, versions):
    def check():
        data = table_data(context, "Version")
        iv, ist = column(data, "Version"), column(data, "Status")
        got = {leading_int(r[iv]): r[ist] for r in data["rows"] if len(r) > ist}
        for v in split_list(versions):
            _assert(v in got, f"no version {v}; versions {list(got)}")
            _assert(got[v].startswith(status), f"version {v} has status '{got[v]}'")
    retry(check, "version status")


@then('the UI shows the details page of "{name:Q}" with the latest version "{n:Q}"')
def step_details_latest(context, name, n):
    nm = X(context, name)
    step_route(context, f"/vocabularies/{nm}")
    step_heading(context, nm)
    retry(lambda: _assert(version_numbers(context)[:1] == [n], f"versions {version_numbers(context)}"), "latest version")


@then('the UI shows for the version "{n:Q}" the warning "{text:Q}"')
def step_version_warning(context, n, text):
    retry(lambda: _assert(text in norm(version_row(context, n).inner_text()), f"no '{text}' for version {n}"), "warning")


@then('the UI shows for the version "{n:Q}" the link "{text:Q}"')
def step_version_link(context, n, text):
    retry(lambda: _assert(version_row(context, n).get_by_role("link", name=text, exact=True).count() > 0,
                          f"no link '{text}' for version {n}"), "link")


@then('the UI shows for the version "{n:Q}" no warning')
def step_version_no_warning(context, n):
    retry(lambda: version_row(context, n), f"version {n}")
    txt = norm(version_row(context, n).inner_text())
    if "This version is deprecated." in txt:
        raise AssertionError(f"version {n} shows a warning: {txt}")


@then('the UI shows for the version "{n:Q}" no link "{text:Q}"')
def step_version_no_link(context, n, text):
    retry(lambda: version_row(context, n), f"version {n}")
    row = version_row(context, n)
    if row.get_by_role("link", name=re.compile("^" + re.escape(text))).count() or text in norm(row.inner_text()):
        raise AssertionError(f"version {n} shows '{text}'")


# ================================================================ checks: menus and buttons

@then('the UI shows the button "{name:Q}" with the items "{items:Q}"')
def step_menu_with(context, name, items):
    got = retry(lambda: menu_items(context, X(context, name)), "menu")
    missing = [i for i in split_list(items) if i not in got]
    if missing:
        raise AssertionError(f"menu items {got}; missing {missing}")


@then('the UI shows the button "{name:Q}" without the items "{items:Q}"')
def step_menu_without(context, name, items):
    got = retry(lambda: menu_items(context, X(context, name)), "menu")
    present = [i for i in split_list(items) if i in got]
    if not got:
        raise AssertionError("the menu has no items")
    if present:
        raise AssertionError(f"menu items {got}; must not have {present}")


@then('the UI shows the button "{name:Q}"')
def step_button_shown(context, name):
    loc = button(context, X(context, name))
    retry(lambda: _assert(loc.count() > 0 and loc.first.is_visible(), f"no button '{name}'"), "button")


@then('the UI shows no button "{name:Q}"')
def step_no_button(context, name):
    settle(context)
    list_names(context)  # the page has loaded
    if button(context, X(context, name)).count():
        raise AssertionError(f"button '{name}' is shown")


@then('the UI has the button "{name:Q}" disabled')
def step_disabled(context, name):
    retry(lambda: _assert(button(context, name).is_disabled(), f"'{name}' is enabled"), "disabled")


@then('the UI has the button "{name:Q}" enabled')
def step_enabled(context, name):
    retry(lambda: _assert(button(context, name).is_enabled(), f"'{name}' is disabled"), "enabled")


# ================================================================ checks: forms

@then('the UI shows the fields "{labels:Q}"')
def step_fields(context, labels):
    for label in split_list(labels):
        retry(lambda: _assert(field(context, label).count() > 0, f"no field '{label}'"), "field")


@then('the UI shows no field "{label:Q}"')
def step_no_field(context, label):
    if field(context, label).count():
        raise AssertionError(f"field '{label}' is shown")


@then('the UI shows the field "{label:Q}" with "{value:Q}"')
def step_field_value(context, label, value):
    retry(lambda: _assert(field(context, label).input_value() == X(context, value),
                          f"'{label}' is '{field(context, label).input_value()}'"), "field value")


@then('the UI shows the field "{label:Q}" empty')
def step_field_empty(context, label):
    retry(lambda: _assert(field(context, label).input_value() == "", f"'{label}' is not empty"), "empty field")


@then('the UI sent no request "{call:Q}"')
def step_no_request(context, call):
    method, path = X(context, call).split(" ", 1)
    sent = [(m, u) for m, u, t in context.ui_requests if m == method and _api_path(u) == path]
    if sent:
        raise AssertionError(f"the UI sent {sent}")


@then('the UI shows a field error under "{label:Q}"')
def step_field_error(context, label):
    retry(lambda: _assert(field_error_text(context, label), f"no error under '{label}'"), "field error")


@then('the UI shows the field error "{text:Q}" under "{label:Q}"')
@then('the UI shows a field error containing "{text:Q}" under "{label:Q}"')
def step_field_error_text(context, text, label):
    retry(lambda: _assert(text in field_error_text(context, label),
                          f"error under '{label}' is '{field_error_text(context, label)}'"), "field error")


@then('the UI has the focus on the field "{label:Q}"')
def step_focus(context, label):
    retry(lambda: _assert(field(context, label).first.evaluate("e => e === document.activeElement"),
                          f"focus is not on '{label}'"), "focus")


# ================================================================ checks: regions

def region(context, name):
    return page(context).get_by_role("region", name=name, exact=True)


@then('the UI shows the region "{name:Q}" with "{texts:Q}"')
def step_region_with(context, name, texts):
    def check():
        reg = region(context, name)
        _assert(reg.count() > 0, f"no region '{name}'")
        txt = norm(reg.first.inner_text())
        missing = [t for t in split_list(texts) if t not in txt]
        _assert(not missing, f"region '{name}' lacks {missing}: {txt}")
    retry(check, "region")


@then('the UI shows the button "{bname:Q}" in the region "{name:Q}"')
def step_region_button(context, bname, name):
    retry(lambda: _assert(button(context, bname, region(context, name).first).count() > 0,
                          f"no button '{bname}' in region '{name}'"), "region button")


@then('the UI shows the region "{name:Q}" with an alert "{text:Q}"')
def step_region_alert(context, name, text):
    def check():
        reg = region(context, name)
        _assert(reg.count() > 0, f"no region '{name}'; alerts: {_alerts(context)}")
        al = reg.first.get_by_role("alert")
        got = [norm(al.nth(i).inner_text()) for i in range(al.count())]
        _assert(text in got, f"alerts in region '{name}': {got}")
    retry(check, "region alert")


@then('the UI shows in the region "{name:Q}" the texts "{texts:Q}"')
def step_region_texts(context, name, texts):
    want = X(context, texts)

    def check():
        reg = region(context, name)
        _assert(reg.count() > 0, f"no region '{name}'; alerts: {_alerts(context)}")
        txt = norm(reg.first.inner_text())
        if want in txt:
            return
        missing = [t for t in split_list(want) if t not in txt]
        _assert(not missing, f"region '{name}' lacks {missing}: {txt}")
    retry(check, "region texts")


@then('the UI shows no region "{name:Q}"')
def step_no_region(context, name):
    settle(context)
    if region(context, name).count():
        raise AssertionError(f"region '{name}' is shown")


# ================================================================ checks: downloads

@then('the UI downloads the file "{filename:Q}"')
def step_download(context, filename):
    want = X(context, filename)

    def check():
        names = [d.suggested_filename for d in context.ui_downloads]
        _assert(want in names, f"downloads: {names}")
    retry(check, "download")
    d = [d for d in context.ui_downloads if d.suggested_filename == want][-1]
    path = d.path()
    if not path or os.path.getsize(path) == 0:
        raise AssertionError("the downloaded file is empty")


# ================================================================ accessibility (Q6, NFR-10)

@then('the UI has no axe violations at level WCAG 2.1 AA')
def step_axe(context):
    from axe_playwright_python.sync_playwright import Axe
    pg = page(context)
    settle(context, 800)
    result = Axe().run(pg, options={"runOnly": {"type": "tag",
                                                "values": ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]}})
    violations = result.response.get("violations", [])
    os.makedirs(os.path.join(REPORTS, "a11y"), exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9]+", "_", urlparse(pg.url).path).strip("_") or "root"
    slug = slug.replace(context.run, "RUN")
    with open(os.path.join(REPORTS, "a11y", f"{slug}.json"), "w", encoding="utf-8") as fh:
        json.dump({"url": pg.url, "violations": violations}, fh, indent=2)
    if violations:
        lines = [f"{v['id']} ({v.get('impact')}): {v['help']} [{len(v.get('nodes', []))} nodes]" for v in violations]
        raise AssertionError("axe violations (WCAG 2.1 AA):\n" + "\n".join(lines))


@then('the UI lets the user reach with the keyboard only "{names:Q}"')
def step_keyboard(context, names):
    pg = page(context)
    want = [X(context, n) for n in split_list(names)]
    reached = set()
    focus_name = """() => { const e = document.activeElement; if (!e || e === document.body) return '';
        const l = e.getAttribute('aria-label'); if (l) return l.trim();
        const lb = e.getAttribute('aria-labelledby');
        if (lb) return lb.split(/\\s+/).map(i => (document.getElementById(i) || {}).textContent || '').join(' ').trim();
        return (e.innerText || e.value || '').replace(/\\s+/g, ' ').trim(); }"""
    list_names(context)
    pg.locator("body").click(position={"x": 1, "y": 1})
    menus = [n for n in want if n.startswith("Actions for ")]
    for _ in range(150):
        pg.keyboard.press("Tab")
        name = pg.evaluate(focus_name)
        reached.add(name)
        if name in menus:
            # open the menu with the keyboard and walk its items
            pg.keyboard.press("Enter")
            pg.wait_for_timeout(300)
            for _ in range(12):
                n2 = pg.evaluate(focus_name)
                reached.add(n2)
                pg.keyboard.press("ArrowDown")
                pg.wait_for_timeout(50)
            for _ in range(12):
                pg.keyboard.press("Tab")
                reached.add(pg.evaluate(focus_name))
            pg.keyboard.press("Escape")
            menus.remove(name)
        if all(w in reached for w in want):
            return
    missing = [w for w in want if w not in reached]
    raise AssertionError(f"not reachable by keyboard: {missing}")
