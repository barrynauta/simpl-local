"""Browser steps for the @ui scenarios (step 60), Playwright for Python.

Elements are found by the texts, labels and column names that the decisions and criteria give,
never by CSS classes, test ids or routes taken from the build. Two kinds of matching:

- A text that a decision fixes (D34, D35, D36, D46, D60) matches exactly, case-sensitive.
- A label, column, filter or action name that no decision fixes (for example the filter "Name",
  the filter "Status", the row action "Download") matches case-insensitively as a whole word,
  so "Download vocabulary" (SIMPL-18161) matches "Download" and "Last updated" does not match
  "Update".

Only the routes of D31 (/vocabularies and /vocabularies/<Name>) are written in the scenarios.
The URL of the create and new version forms is not decided; the step that opens such a form
directly learns it at run time by opening the form as ga-admin through its decided control
('New Vocabulary +', 'Update').

Every scenario starts a new browser context: no stored cookies, storage or sessions.
"""
import json
import re
import time
from pathlib import Path
from urllib.parse import unquote, urlparse

from behave import given, then, when

from vmtest import config
from vmtest import turtle

TIMEOUT = 15.0          # seconds to wait for a condition on the page
SHORT = 2.5             # seconds of quiet before a negative check
CONTROL_ROLES = ("button", "link", "menuitem", "tab", "option")


# ---------------------------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------------------------
def ui(context):
    return context.ui


def page(context):
    return context.ui["page"]


def word_re(text):
    return re.compile(r"(?<![A-Za-z0-9])" + re.escape(text.strip()) + r"(?![A-Za-z0-9])", re.I)


def eventually(check, timeout=TIMEOUT, interval=0.25, message="condition"):
    """Run check() until it returns a value other than False, None or an exception object.

    A check may return an AssertionError (instead of raising it) to give the failure text.
    """
    deadline = time.time() + timeout
    last = None
    while True:
        try:
            result = check()
            if isinstance(result, BaseException):
                last = result if isinstance(result, AssertionError) else AssertionError(str(result))
            elif result is not False and result is not None:
                return result
            else:
                last = AssertionError(message)
        except AssertionError as exc:
            last = exc
        except Exception as exc:  # Playwright errors while the page re-renders
            last = AssertionError(f"{message}: {exc}")
        if time.time() > deadline:
            raise last
        pump(interval)


_PAGES = []   # pages whose events must be processed while a check waits


def pump(seconds):
    """Wait while letting Playwright process events (time.sleep would freeze page.url)."""
    for pg in reversed(_PAGES):
        try:
            if not pg.is_closed():
                pg.wait_for_timeout(int(seconds * 1000))
                return
        except Exception:
            continue
    time.sleep(seconds)


def settle(context, extra=0.3):
    pg = page(context)
    try:
        pg.wait_for_load_state("networkidle", timeout=8000)
    except Exception:
        pass
    if extra:
        pg.wait_for_timeout(int(extra * 1000))


def body_text(context):
    return page(context).locator("body").inner_text()


def visible(locator):
    try:
        return locator.is_visible()
    except Exception:
        return False


def first_visible(locators):
    for loc in locators:
        try:
            count = loc.count()
        except Exception:
            continue
        for i in range(count):
            item = loc.nth(i)
            if visible(item):
                return item
    return None


def ui_base():
    return config.UI_URL


def idp_base():
    """Realm URL of the identity provider, from TOKEN_URL (decision E1, build profile)."""
    return config.TOKEN_URL.split("/protocol/openid-connect")[0]


def api_path_prefix():
    return urlparse(config.API_URL).path.rstrip("/")  # '/v1' (decision E2)


def control(scope, label, roles=CONTROL_ROLES):
    """A visible control by its name: exact first, then case-insensitive whole word."""
    exact = [scope.get_by_role(r, name=label, exact=True) for r in roles]
    found = first_visible(exact)
    if found:
        return found
    loose = [scope.get_by_role(r, name=word_re(label)) for r in roles]
    return first_visible(loose)


def field(context, label):
    """A form field by its label (D60 labels; others case-insensitive whole word)."""
    pg = page(context)
    found = first_visible([pg.get_by_label(label, exact=True)])
    if found:
        return found
    found = first_visible([pg.get_by_label(word_re(label))])
    if found:
        return found
    for role in ("textbox", "combobox", "spinbutton", "searchbox"):
        found = first_visible([pg.get_by_role(role, name=word_re(label))])
        if found:
            return found
    return None


def tables(context):
    pg = page(context)
    return [pg.get_by_role("table").nth(i) for i in range(pg.get_by_role("table").count())]


def headers_of(table):
    names = []
    hdrs = table.get_by_role("columnheader")
    for i in range(hdrs.count()):
        names.append(" ".join(hdrs.nth(i).inner_text().split()))
    return names


def column_index(headers, column):
    for i, h in enumerate(headers):
        if h == column:
            return i
    rx = word_re(column)
    for i, h in enumerate(headers):
        if rx.search(h):
            return i
    return None


def data_rows(table):
    rows = table.get_by_role("row")
    out = []
    for i in range(rows.count()):
        row = rows.nth(i)
        if row.get_by_role("columnheader").count():
            continue
        out.append(row)
    return out


def cell_texts(row):
    cells = row.get_by_role("cell")
    return [" ".join(cells.nth(i).inner_text().split()) for i in range(cells.count())]


def table_with_column(context, column):
    for t in tables(context):
        if visible(t) and column_index(headers_of(t), column) is not None:
            return t
    return None


def vocabulary_row(context, logical):
    """The list row whose Name cell is the stored name of a logical vocabulary."""
    real = context.world.real(logical)

    def find():
        t = table_with_column(context, "Name")
        assert t is not None, "no table with a 'Name' column"
        idx = column_index(headers_of(t), "Name")
        for row in data_rows(t):
            texts = cell_texts(row)
            if idx is not None and idx < len(texts) and texts[idx] == real:
                return row
            if real in texts:
                return row
        raise AssertionError(f"no row for {real}; rows: {[cell_texts(r) for r in data_rows(t)][:10]}")

    return eventually(find, message=f"row of {real}")


def version_row(context, version):
    def find():
        t = table_with_column(context, "Version")
        assert t is not None, "no table with a 'Version' column"
        idx = column_index(headers_of(t), "Version")
        for row in data_rows(t):
            texts = cell_texts(row)
            if idx < len(texts) and texts[idx] == str(version):
                return row
        raise AssertionError(f"no row for version {version}")

    return eventually(find, message=f"row of version {version}")


def open_row_menu(context, row):
    """Open a menu or disclosure inside the row, when it has one (ARIA aria-expanded/aria-haspopup)."""
    toggles = row.locator("[aria-haspopup]:not([aria-haspopup='false']), button[aria-expanded='false']")
    for i in range(toggles.count()):
        t = toggles.nth(i)
        if visible(t) and t.get_attribute("aria-expanded") != "true":
            t.click()
            page(context).wait_for_timeout(250)
            return True
    return False


def row_control(context, row, label):
    found = control(row, label)
    if found:
        return found
    open_row_menu(context, row)
    found = control(row, label)
    if found:
        return found
    # A menu rendered outside the row (portal): look for a menu item on the page.
    return control(page(context), label, roles=("menuitem",))


def write_upload(context, fixture):
    """Path of a file to upload for a fixture name ('x.ttl' or 'turtle:<bytes>')."""
    tmp = Path(context.ui["tmp"].name)
    if fixture.startswith("turtle:"):
        data = turtle.sized_content(context.world.run, "Big", int(fixture.split(":", 1)[1]))
        path = tmp / f"big{context.world.run}.ttl"
    else:
        data = context.world.fixture(fixture)
        path = tmp / fixture
    path.write_bytes(data)
    return path


# ---------------------------------------------------------------------------------------------
# API stubs (decision E3: UI tests may stub the API for error cases; D24)
# ---------------------------------------------------------------------------------------------
PROBLEMS = {
    500: {"type": "urn:problem-type:belgif:internalServerError", "title": "Internal Server Error",
          "status": 500, "detail": "Internal Server Error"},
    503: {"type": "urn:problem-type:belgif:serviceUnavailable", "title": "Service Unavailable",
          "status": 503, "detail": "The vocabulary store is not available. Retry later."},
}


def stub_body(context, method, path, code):
    if code == 404:
        name = path.rstrip("/").split("/")[-1]
        return {"type": "urn:problem-type:belgif:resourceNotFound", "title": "Resource Not Found",
                "status": 404, "detail": f"Vocabulary with name '{name}' was not found."}
    if code in PROBLEMS:
        return PROBLEMS[code]
    if code == 200 and method == "GET":
        return {"items": [], "page": 1, "pageSize": 20, "total": 0, "totalPages": 0,
                "self": api_path_prefix() + path, "first": api_path_prefix() + path}
    return {"status": code, "title": "Stub", "detail": f"Stubbed {code}"}


def install_stubs(context, pg):
    prefix = api_path_prefix()
    for stub in context.ui["stubs"]:
        target = prefix + stub["path"]
        method = stub["method"]

        def matcher(url, target=target):
            return unquote(urlparse(url).path).rstrip("/").endswith(target)

        def handler(route, request, method=method, stub=stub):
            if request.method.upper() != method or request.resource_type not in ("fetch", "xhr"):
                return route.fallback()
            ctype = "application/json" if stub["code"] < 400 else "application/problem+json"
            return route.fulfill(status=stub["code"], content_type=ctype, body=json.dumps(stub["body"]))

        pg.route(matcher, handler)


# ---------------------------------------------------------------------------------------------
# browser lifecycle (called from environment.py)
# ---------------------------------------------------------------------------------------------
from vmtest.browser import close_ui, open_ui, start_browser, stop_browser  # noqa: E402,F401


def new_page(context):
    pg = context.ui["context"].new_page()
    _PAGES[:] = [pg]
    install_stubs(context, pg)
    context.ui["page"] = pg
    return pg


def on_idp(pg):
    return pg.url.startswith(idp_base())


def sign_in_on_idp(pg, user):
    """Fill the Keycloak login form (decision E1); the labels are Keycloak's own."""
    creds = config.USERS[user]
    username = first_visible([pg.get_by_label(re.compile(r"user ?name", re.I)),
                              pg.get_by_role("textbox", name=re.compile(r"user ?name", re.I))])
    assert username, "no user name field on the login page"
    username.fill(creds["username"])
    password = first_visible([pg.get_by_label("Password", exact=True), pg.locator("input[type=password]")])
    password.fill(config.PASSWORD)
    button = control(pg, "Sign In") or control(pg, "Log In")
    assert button, "no sign-in button on the login page"
    button.click()


def ensure_page(context):
    if context.ui["page"] is None:
        new_page(context)
    return context.ui["page"]


# ---------------------------------------------------------------------------------------------
# steps: session and navigation
# ---------------------------------------------------------------------------------------------
@given('I am signed in to the UI as "{user:Q}"')
def ui_signed_in(context, user):
    pg = ensure_page(context)
    pg.goto(ui_base() + "/vocabularies")
    eventually(lambda: on_idp(pg) or control(pg, "Log in") or control(pg, "Sign in"),
               message="the UI sends to the login page or offers a sign-in control")
    if not on_idp(pg):
        (control(pg, "Log in") or control(pg, "Sign in")).click()
        eventually(lambda: on_idp(pg), message="login page of the identity provider")
    sign_in_on_idp(pg, user)
    eventually(lambda: pg.url.startswith(ui_base()), message="back on the UI after sign-in")
    settle(context)
    context.ui["user"] = user


@given("I am not signed in to the UI")
def ui_not_signed_in(context):
    ensure_page(context)
    context.ui["user"] = None


@given('the API is stubbed to answer "{method:Q}" "{path:Q}" with status {code:d}')
def ui_stub(context, method, path, code):
    path = context.world.expand(path)
    context.ui["stubs"].append({"method": method.upper(), "path": path, "code": code,
                                "body": stub_body(context, method.upper(), path, code)})
    if context.ui["page"] is not None:
        install_stubs(context, context.ui["page"])


@when('I open the page "{route:Q}"')
def ui_open(context, route):
    pg = ensure_page(context)
    pg.goto(ui_base() + context.world.expand(route))
    settle(context)


def learn_form_url(context, form, logical):
    """Open the create or new version form as ga-admin through its decided control; return its URL."""
    bctx = context._browser.new_context(storage_state=None)
    try:
        pg = bctx.new_page()
        _PAGES.append(pg)
        pg.set_default_timeout(TIMEOUT * 1000)
        pg.goto(ui_base() + "/vocabularies")
        eventually(lambda: on_idp(pg) or AssertionError(f"learning the form URL: on {pg.url}"), message="login page")

        sign_in_on_idp(pg, "ga-admin")
        eventually(lambda: pg.url.startswith(ui_base()), message="back on the UI")
        pg.wait_for_load_state("networkidle")
        before = pg.url.split("#")[0]
        if form == "create":
            eventually(lambda: control(pg, "New Vocabulary +"), message="'New Vocabulary +'").click()
        else:
            real = context.world.real(logical)
            flt = first_visible([pg.get_by_label(word_re("Name"))])
            flt.fill(real)
            flt.press("Enter")
            pg.wait_for_load_state("networkidle")
            row = eventually(lambda: first_visible([pg.get_by_role("row").filter(has_text=real)]),
                             message=f"row of {real}")
            btn = control(row, "Update")
            if not btn:
                toggles = row.locator("[aria-haspopup]:not([aria-haspopup='false']), button[aria-expanded='false']")
                if toggles.count():
                    toggles.first.click()
                btn = control(row, "Update") or control(pg, "Update", roles=("menuitem",))
            btn.click()
        eventually(lambda: pg.url.split("#")[0] != before, timeout=5,
                   message=f"the {form} form has a URL of its own (D18)")
        pg.wait_for_load_state("networkidle")
        return pg.url.split("#")[0]
    finally:
        if _PAGES and _PAGES[-1] is not page(context):
            _PAGES.pop()
        bctx.close()


@when('I open the URL of the "{form:Q}" form of "{name:Q}" directly')
def ui_open_form_url(context, form, name):
    url = learn_form_url(context, form, name)
    pg = ensure_page(context)
    pg.goto(url)
    settle(context)


# ---------------------------------------------------------------------------------------------
# steps: actions
# ---------------------------------------------------------------------------------------------
@when('I click "{label:Q}"')
def ui_click(context, label):
    label = context.world.expand(label)
    pg = page(context)
    eventually(lambda: control(pg, label), message=f"control '{label}'").click()
    settle(context)


@when('I click "{label:Q}" in the row of "{name:Q}"')
def ui_click_row(context, label, name):
    row = vocabulary_row(context, name)
    btn = eventually(lambda: row_control(context, row, label), message=f"'{label}' in the row of {name}")
    if word_re("Download").search(label):
        with page(context).expect_download() as info:
            btn.click()
        context.ui["download"] = info.value
    else:
        btn.click()
    settle(context)


@when('I click "{label:Q}" in the row of version {version:d}')
def ui_click_version_row(context, label, version):
    row = version_row(context, version)
    btn = eventually(lambda: row_control(context, row, label) or first_visible([row.get_by_text(label, exact=True)]),
                     message=f"'{label}' in the row of version {version}")
    if word_re("Download").search(label):
        with page(context).expect_download() as info:
            btn.click()
        context.ui["download"] = info.value
    else:
        btn.click()
    settle(context)


@when('I open the actions menu in the row of "{name:Q}"')
def ui_open_menu(context, name):
    row = vocabulary_row(context, name)
    open_row_menu(context, row)
    context.ui["menu_row"] = row


@when('I fill in "{field_name:Q}" with "{value:Q}"')
def ui_fill(context, field_name, value):
    value = context.world.expand(value)
    f = eventually(lambda: field(context, field_name), message=f"field '{field_name}'")
    f.fill(value)
    f.blur()
    page(context).wait_for_timeout(200)


def file_target(context, label):
    pg = page(context)
    return first_visible([pg.get_by_label(label, exact=True), pg.get_by_label(word_re(label)),
                          pg.get_by_role("button", name=word_re(label))])


@when('I choose the file "{fixture:Q}" for "{field_name:Q}"')
def ui_choose_file(context, fixture, field_name):
    path = write_upload(context, fixture)
    pg = page(context)
    target = eventually(lambda: file_target(context, field_name), message=f"file control '{field_name}'")
    input_type = (target.get_attribute("type") or "").lower()
    if input_type == "file":
        target.set_input_files(str(path))
    else:
        with pg.expect_file_chooser() as chooser:
            target.click()
        chooser.value.set_files(str(path))
    settle(context, extra=0.8)


@when('I click the column header "{column:Q}"')
def ui_sort(context, column):
    t = eventually(lambda: table_with_column(context, column), message=f"column '{column}'")
    idx = column_index(headers_of(t), column)
    header = t.get_by_role("columnheader").nth(idx)
    btn = first_visible([header.get_by_role("button"), header.get_by_role("link")]) or header
    btn.click()
    settle(context)


@when('I set the filter "{name:Q}" to "{value:Q}"')
def ui_filter(context, name, value):
    value = context.world.expand(value)
    pg = page(context)

    def find():
        for loc in (pg.get_by_label(name, exact=True), pg.get_by_label(word_re(name))):
            for i in range(loc.count()):
                item = loc.nth(i)
                tag = item.evaluate("e => e.tagName.toLowerCase()")
                if visible(item) and tag in ("input", "select", "textarea"):
                    return item
        return None

    f = eventually(find, message=f"filter '{name}'")
    if f.evaluate("e => e.tagName.toLowerCase()") == "select":
        f.select_option(label=value)
    else:
        f.fill(value)
        f.press("Enter")
    settle(context, extra=0.6)


# ---------------------------------------------------------------------------------------------
# steps: checks
# ---------------------------------------------------------------------------------------------
@then('I see the text "{text:Q}"')
def ui_see_text(context, text):
    text = context.world.expand(text)
    eventually(lambda: text in body_text(context) or AssertionError(
        f"text '{text}' not on the page; page text: {body_text(context)[:800]!r}"),
        message=f"text '{text}'")


@then('I do not see the text "{text:Q}"')
def ui_not_see_text(context, text):
    text = context.world.expand(text)
    settle(context, extra=SHORT)
    assert text not in body_text(context), f"text '{text}' is on the page"


@then('I see the control "{label:Q}"')
def ui_see_control(context, label):
    pg = page(context)
    eventually(lambda: control(pg, label), message=f"control '{label}' visible")


@then('I do not see the control "{label:Q}"')
def ui_not_see_control(context, label):
    settle(context, extra=SHORT)
    found = control(page(context), label)
    assert not found, f"control '{label}' is visible: {found.inner_text() if found else ''}"


@then('the control "{label:Q}" is disabled')
def ui_disabled(context, label):
    pg = page(context)

    def check():
        c = control(pg, label)
        assert c, f"control '{label}' not found"
        assert c.is_disabled() or c.get_attribute("aria-disabled") == "true", f"control '{label}' is enabled"
        return True

    eventually(check, message=f"'{label}' disabled")


@then('I see the columns "{columns:Q}"')
def ui_columns(context, columns):
    wanted = [c.strip() for c in columns.split(",")]

    def check():
        for t in tables(context):
            hdrs = headers_of(t)
            if all(column_index(hdrs, c) is not None for c in wanted):
                return True
        raise AssertionError(f"no table with columns {wanted}; tables: {[headers_of(t) for t in tables(context)]}")

    eventually(check, message="columns")


def row_shows(row, text):
    texts = cell_texts(row)
    if text in texts:
        return True
    if len(text) > 2 and any(text in t for t in texts):
        return True
    return text in " ".join(row.inner_text().split()) if len(text) > 2 else False


@then('the row of "{name:Q}" shows "{text:Q}"')
def ui_row_shows(context, name, text):
    text = context.world.expand(text)

    def check():
        row = vocabulary_row(context, name)
        if row_shows(row, text):
            return True
        # A warning icon may carry the text as its accessible name or title only.
        icons = row.locator(f"[aria-label*={json.dumps(text)}], [title*={json.dumps(text)}]")
        if icons.count():
            return True
        raise AssertionError(f"row of {name} does not show '{text}': {cell_texts(row)}")

    eventually(check, message=f"row of {name} shows {text}")


@then('the vocabulary list rows are "{names:Q}"')
def ui_rows(context, names):
    wanted = [context.world.real(n) for n in names.split(",")]

    def check():
        t = table_with_column(context, "Name")
        assert t is not None, "no list table"
        idx = column_index(headers_of(t), "Name")
        got = []
        for row in data_rows(t):
            texts = cell_texts(row)
            if idx < len(texts) and texts[idx]:
                got.append(texts[idx])
        assert got == wanted, f"rows are {got}, expected {wanted}"
        return True

    eventually(check, message="list rows")


@then('the version rows are "{numbers:Q}"')
def ui_version_rows(context, numbers):
    wanted = [n.strip() for n in numbers.split(",")]

    def check():
        t = table_with_column(context, "Version")
        assert t is not None, "no version table"
        idx = column_index(headers_of(t), "Version")
        got = [cell_texts(r)[idx] for r in data_rows(t) if idx < len(cell_texts(r))]
        assert got == wanted, f"version rows are {got}, expected {wanted}"
        return True

    eventually(check, message="version rows")


@then('the field "{field_name:Q}" holds "{value:Q}"')
def ui_field_holds(context, field_name, value):
    value = context.world.expand(value)

    def check():
        f = field(context, field_name)
        assert f, f"field '{field_name}' not found"
        got = f.input_value()
        assert got == value, f"field '{field_name}' holds {got!r}, expected {value!r}"
        return True

    eventually(check, message=f"field {field_name}")


@then('a file "{filename:Q}" is downloaded with the content of version {version:d} of "{name:Q}"')
def ui_download(context, filename, version, name):
    filename = context.world.expand(filename)
    dl = context.ui.get("download")
    assert dl is not None, "no download happened"
    assert dl.suggested_filename == filename, f"file name is {dl.suggested_filename!r}, expected {filename!r}"
    data = Path(dl.path()).read_bytes()
    entry = context.world.vocabs.get(name, {}).get("versions", {}).get(version)
    if entry and entry.get("content") is not None:
        expected = entry["content"]
    else:
        resp = context.world.admin_get(f"/content/{context.world.real(name)}/versions/{version}")
        assert resp.status_code == 200, f"reading the content gave {resp.status_code}"
        expected = resp.content
    assert data == expected, f"downloaded content differs from version {version} ({len(data)} vs {len(expected)} bytes)"


@then('I am on the page "{route:Q}"')
def ui_on_page(context, route):
    route = context.world.expand(route)
    pg = page(context)
    eventually(lambda: unquote(urlparse(pg.url).path).rstrip("/") == route.rstrip("/")
               or AssertionError(f"on {pg.url}, expected {route}"), message=f"page {route}")


@then("I am sent to the login page of the identity provider")
def ui_login_page(context):
    pg = page(context)
    eventually(lambda: on_idp(pg) or AssertionError(f"on {pg.url}"), message="login page")
    eventually(lambda: first_visible([pg.locator("input[type=password]")]), message="password field")


@then('I see a message that the vocabulary could not be found')
def ui_not_found_message(context):
    """SIMPL-18174/AC2: any not-found message, for example the problem detail of the 404 stub."""
    rx = re.compile(r"not (been )?found|no longer (exists|available)|could not be found", re.I)
    eventually(lambda: rx.search(body_text(context)) or AssertionError(
        f"no not-found message; page text: {body_text(context)[:600]!r}"), message="not-found message")


@then('I see a field error under "{field_name:Q}"')
def ui_field_error(context, field_name):
    """D37: a field error under the field, before submit. Proof, any of: the field is marked
    invalid (aria-invalid), a visible text is tied to it by aria-describedby or
    aria-errormessage, or its label or the element right after it shows text beyond the label
    (the message placed under the field)."""
    pg = page(context)
    js = """([el, label]) => {
        const norm = t => (t || '').replace(/\\s+/g, ' ').trim();
        const vis = e => e && e.offsetParent !== null && norm(e.innerText) !== '';
        const fields = [el];
        if (el.tagName.toLowerCase() !== 'input' && el.tagName.toLowerCase() !== 'textarea') {
            const lab = el.closest('label');
            if (lab) lab.querySelectorAll('input,textarea,select').forEach(f => fields.push(f));
        }
        for (const f of fields) {
            if (f.getAttribute('aria-invalid') === 'true') return 'aria-invalid';
            for (const a of ['aria-errormessage', 'aria-describedby']) {
                for (const id of (f.getAttribute(a) || '').split(/\\s+/).filter(Boolean)) {
                    if (vis(document.getElementById(id))) return a;
                }
            }
            for (const lab of Array.from(f.labels || [])) {
                const extra = norm(lab.innerText).replace(label, '').trim();
                if (extra.length > 0) return 'label text: ' + extra;
            }
            const next = f.nextElementSibling;
            if (vis(next) && !['INPUT','TEXTAREA','SELECT','BUTTON','LABEL'].includes(next.tagName)) return 'next: ' + norm(next.innerText);
        }
        return null;
    }"""

    def check():
        f = field(context, field_name) or file_target(context, field_name)
        assert f, f"field '{field_name}' not found"
        handle = f.element_handle()
        how = pg.evaluate(js, [handle, field_name])
        assert how, f"no field error under '{field_name}'"
        return how

    eventually(check, message=f"field error under {field_name}")


@then('the form has no field labelled "{label:Q}"')
def ui_no_field(context, label):
    settle(context, extra=0.5)
    pg = page(context)
    found = first_visible([pg.get_by_label(word_re(label))])
    assert not found, f"the form has a field labelled '{label}'"


@then('the row of version {version:d} shows "{text:Q}"')
def ui_version_row_shows(context, version, text):
    def check():
        row = version_row(context, version)
        assert row_shows(row, text), f"row of version {version} does not show '{text}': {cell_texts(row)}"
        return True

    eventually(check, message=f"row of version {version}")


@then('the version panel shows "{text:Q}"')
def ui_panel_shows(context, text):
    """D35: the panel of the selected version. Proof: the text is visible outside the version list."""
    pg = page(context)
    js = """(text) => {
        const all = Array.from(document.querySelectorAll('body *'));
        return all.some(e => !e.closest('table') && e.offsetParent !== null &&
            Array.from(e.childNodes).some(n => n.nodeType === 3 && n.textContent.includes(text)));
    }"""
    eventually(lambda: pg.evaluate(js, text) or AssertionError(f"'{text}' not shown outside the version list"),
               message=f"version panel shows {text}")


# ---------------------------------------------------------------------------------------------
# accessibility (NFR-10: WCAG 2.1 AA, axe per main page)
# ---------------------------------------------------------------------------------------------
@then("the page has no WCAG 2.1 AA violations")
def ui_axe(context):
    from axe_playwright_python.sync_playwright import Axe
    settle(context, extra=0.8)
    pg = page(context)
    results = Axe().run(pg, options={"runOnly": {"type": "tag",
                                                 "values": ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]}})
    violations = results.response.get("violations", [])
    incomplete = results.response.get("incomplete", [])
    report = {
        "url": pg.url,
        "violations": [{"id": v["id"], "impact": v.get("impact"), "help": v.get("help"),
                        "nodes": [n.get("target") for n in v.get("nodes", [])][:5]} for v in violations],
        "incomplete": [{"id": v["id"], "help": v.get("help"), "nodes": len(v.get("nodes", []))} for v in incomplete],
    }
    out = Path(__file__).resolve().parents[2] / "reports" / "a11y"
    out.mkdir(parents=True, exist_ok=True)
    slug = re.sub(r"[^a-z0-9]+", "-", context.scenario.name.lower()).strip("-")
    (out / f"{slug}.json").write_text(json.dumps(report, indent=2))
    print(f"axe {pg.url}: {len(violations)} violations, {len(incomplete)} incomplete")
    for v in report["violations"]:
        print(f"  - {v['id']} ({v['impact']}): {v['help']} {v['nodes']}")
    assert not violations, "WCAG 2.1 AA violations: " + ", ".join(
        f"{v['id']} ({v['impact']}, {len(v['nodes'])} nodes)" for v in report["violations"])
