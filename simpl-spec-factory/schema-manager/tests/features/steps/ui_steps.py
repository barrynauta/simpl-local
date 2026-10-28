"""Browser steps for the @ui scenarios (behave + Playwright sync API, headless Chromium).

Black-box rules for locators, in this order:
1. Accessible roles, labels and visible texts taken from the story wording
   (labels quoted by a story match case-insensitive and whitespace tolerant).
2. Where no story fixes it, the `data-testid` values and the texts that the
   digest defines (D45 to D57). The scenarios carry the decision tags, so a
   failure on such a locator reads as "different decision".
3. Where neither fixes it, a tolerant pattern with a comment naming the story.
No CSS classes or ids are used.

The browser, the context and the page are created per @ui scenario in
environment.py (`ctx.ui`). API setup steps (`Given the RESOURCE schema ...
exists`) come from api_steps.py and share the alias mechanism.
"""

import re
import time
from datetime import datetime
from urllib.parse import parse_qsl, unquote, urlsplit

import requests
from behave import given, then, when

import smlib  # features/ is on sys.path (environment.py)


# Step modules are executed by behave, not imported, so the three small helpers
# of api_steps.py are repeated here instead of imported (an import would
# register every API step a second time).
def S(ctx, text):
    return ctx.names.sub(text)


def concept(ctx, alias):
    return ctx.concepts.get(alias, {"type": "RESOURCE", "rt": None})


def admin_headers(ctx):
    return {"Authorization": f"Bearer {ctx.tokens.token('ga-admin')}"}

WAIT = 10.0          # seconds to wait for something to appear or change
SETTLE = 1.0         # seconds to let the page settle before checking an absence
ROW_TESTID = re.compile(r"^(schema-row|action-download|action-revoke|action-publish|action-new-version)-(.+)$")


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def ui(ctx):
    assert getattr(ctx, "ui", None) is not None, "no browser page: is the scenario tagged @ui?"
    return ctx.ui


def page(ctx):
    return ui(ctx).page


def norm(text):
    return re.sub(r"\s+", " ", text or "").strip()


def header_key(text):
    """Header text without sort arrows or other decoration, lower case (story labels)."""
    return re.sub(r"[^a-z0-9 ]", "", norm(text).lower()).strip()


def eventually(check, timeout=WAIT, interval=0.25):
    """Run check() until it returns without AssertionError or the timeout passes."""
    end = time.time() + timeout
    while True:
        try:
            return check()
        except AssertionError:
            if time.time() >= end:
                raise
        except Exception as exc:  # Playwright errors while the DOM changes
            if time.time() >= end:
                raise AssertionError(str(exc).splitlines()[0])
        time.sleep(interval)


def settle(ctx, extra=0.3):
    pg = page(ctx)
    try:
        pg.wait_for_load_state("networkidle", timeout=WAIT * 1000)
    except Exception:
        pass
    pg.wait_for_timeout(int(extra * 1000))


def by_testid(ctx, testid):
    return page(ctx).get_by_test_id(S(ctx, testid))


def need(ctx, testid, timeout=WAIT):
    """The first visible element with this data-testid (D46), or a clear failure."""
    loc = by_testid(ctx, testid).first
    try:
        loc.wait_for(state="visible", timeout=timeout * 1000)
    except Exception:
        raise AssertionError(f"no visible element with data-testid '{S(ctx, testid)}' (D46) on {page(ctx).url}")
    return loc


def visible_count(loc):
    n = 0
    for i in range(loc.count()):
        try:
            if loc.nth(i).is_visible():
                n += 1
        except Exception:
            pass
    return n


def tag_of(loc):
    return loc.evaluate("e => e.tagName.toLowerCase()")


def current_path(ctx):
    return urlsplit(page(ctx).url).path.rstrip("/") or "/"


def on_list_page(ctx):
    return current_path(ctx) == "/schemas"


def control_disabled(loc):
    if loc.is_disabled():
        return True
    if (loc.get_attribute("aria-disabled") or "").lower() == "true":
        return True
    return False


def ensure_row(ctx, name):
    """Bring the row of schema `name` on screen in the schemas list.

    The list is paged by 20 and ordered by name (D26, D53); the shared store
    holds the schemas of earlier runs, so a new schema is not always on page 1.
    A user would page with the pagination controls; so does the test (forward,
    then back). Requests sent while paging are not counted by "sent no request".
    """
    if not on_list_page(ctx):
        return
    pg = page(ctx)
    row = pg.get_by_test_id(f"schema-row-{name}")
    settle(ctx, 0.2)
    if visible_count(row):
        return
    for control in ("pagination-next", "pagination-prev"):
        for _ in range(200):
            if visible_count(row):
                break
            button = pg.get_by_test_id(control)
            if not visible_count(button) or control_disabled(button.first):
                break
            button.first.click()
            settle(ctx, 0.2)
        if visible_count(row):
            break
    ui(ctx).checkpoint = len(ui(ctx).requests)
    assert visible_count(row), (f"no row 'schema-row-{name}' on any page of the schemas list "
                                f"(paged with pagination-next and pagination-prev)")


def ensure_row_for(ctx, testid):
    m = ROW_TESTID.match(S(ctx, testid))
    if m and on_list_page(ctx):
        ensure_row(ctx, m.group(2))


def select_value(loc):
    """The shown value of a form control: the selected option label of a select,
    the value of an input, else the visible text."""
    tag = tag_of(loc)
    if tag == "select":
        return loc.evaluate("e => e.selectedIndex < 0 ? '' : e.options[e.selectedIndex].text")
    if tag in ("input", "textarea"):
        return loc.input_value()
    return loc.inner_text()


def raw_value(loc):
    tag = tag_of(loc)
    if tag in ("select", "input", "textarea"):
        return loc.input_value()
    return loc.inner_text()


def options_of(loc):
    """(value, label) of the real options of a select, without placeholders (empty value)."""
    if tag_of(loc) == "select":
        opts = loc.evaluate("e => Array.from(e.options).map(o => [o.value, o.text])")
        return [(v, norm(t)) for v, t in opts if v != ""]
    # Not a native select: open it and read the ARIA options (tolerant pattern).
    loc.click()
    labels = [norm(o.inner_text()) for o in page_of(loc).get_by_role("option").all() if o.is_visible()]
    page_of(loc).keyboard.press("Escape")
    return [(lbl, lbl) for lbl in labels if lbl]


def page_of(loc):
    return loc.page


def table_headers(table):
    heads = table.get_by_role("columnheader")
    if not heads.count():
        heads = table.locator("thead th")
    return [norm(h.inner_text()) for h in heads.all()]


def column_index(table, column):
    headers = table_headers(table)
    keys = [header_key(h) for h in headers]
    want = header_key(column)
    assert want in keys, f"no column '{column}' in the headers {headers}"
    return keys.index(want)


def row_cells(row):
    cells = row.get_by_role("cell")
    if not cells.count():
        cells = row.locator("td, th")
    return [norm(c.inner_text()) for c in cells.all()]


def data_rows(table):
    # D46 fixes the row test ids; empty-state rows have none.
    return table.locator("[data-testid^='schema-row-'], [data-testid^='version-row-']")


DATE_FORMATS = ("%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M", "%Y-%m-%d", "%d/%m/%Y %H:%M:%S", "%d/%m/%Y %H:%M",
                "%d/%m/%Y", "%d-%m-%Y %H:%M:%S", "%d-%m-%Y", "%m/%d/%Y, %I:%M:%S %p", "%d.%m.%Y %H:%M:%S",
                "%d.%m.%Y", "%b %d, %Y, %I:%M:%S %p", "%d %b %Y %H:%M:%S", "%d %b %Y")


def parse_date(text):
    t = norm(text)
    try:
        return datetime.fromisoformat(t.replace("Z", "+00:00")).replace(tzinfo=None)
    except ValueError:
        pass
    for fmt in DATE_FORMATS:
        try:
            return datetime.strptime(t, fmt)
        except ValueError:
            continue
    raise AssertionError(f"cannot read '{text}' as a date")


def sort_keys(column, values):
    key = header_key(column)
    if key == "version":
        return [[int(re.match(r"\d+", v).group(0)) for v in values]]
    if "date" in key:
        return [[parse_date(v) for v in values]]
    # Text columns: accept code-point order and case-insensitive (locale-like) order.
    return [list(values), [v.casefold() for v in values]]


def api_base_of(ctx, url):
    """The API base URL that `url` falls under, or None (not an API request)."""
    for base in ctx.cfg.sm_ui_api_urls:
        if url == base or url.startswith(base + "/") or url.startswith(base + "?"):
            return base
    return None


def api_requests(ctx, since=0):
    return [(m, u) for m, u in ui(ctx).requests[since:] if api_base_of(ctx, u) and m != "OPTIONS"]


def relative(ctx, url):
    return url[len(api_base_of(ctx, url)):]


def request_matches(expected_method, expected_path, method, rel):
    if method != expected_method:
        return False
    exp, act = urlsplit(expected_path), urlsplit(rel)
    if unquote(exp.path).rstrip("/") != unquote(act.path).rstrip("/"):
        return False
    exp_q = sorted(parse_qsl(exp.query, keep_blank_values=True))
    act_q = parse_qsl(act.query, keep_blank_values=True)
    names = {k for k, _ in exp_q}
    # Paging parameters are only compared when the scenario names them (the
    # versions endpoint is paged too, D25, but D54 does not fix its paging).
    if "page" not in names and "pageSize" not in names:
        act_q = [(k, v) for k, v in act_q if k not in ("page", "pageSize")]
    # schemaType=RESOURCE is the default selector of every by-name operation and
    # of the list (D11, D27, SIMPL-33537): sending it equals leaving it out.
    if "schemaType" not in names:
        act_q = [(k, v) for k, v in act_q if (k, v) != ("schemaType", "RESOURCE")]
    return exp_q == sorted(act_q)


def fixture_payload(ctx, fixture):
    return {"name": fixture, "mimeType": "text/turtle", "buffer": smlib.fixture_bytes(fixture, ctx.names)}


def file_input(loc):
    if tag_of(loc) == "input" and (loc.get_attribute("type") or "").lower() == "file":
        return loc
    return loc.locator("input[type=file]").first


def do_select(ctx, testid, value):
    loc = need(ctx, testid)
    value = S(ctx, value)
    if tag_of(loc) == "select":
        opts = loc.evaluate("e => Array.from(e.options).map(o => [o.value, o.text])")
        match = [v for v, t in opts if v == value] or [v for v, t in opts if norm(t).lower() == value.lower()]
        assert match, f"'{testid}' has no option '{value}': {opts}"
        loc.select_option(value=match[0])
    else:
        loc.click()
        page(ctx).get_by_role("option", name=re.compile(rf"^\s*{re.escape(value)}\s*$", re.I)).first.click()
    settle(ctx)


# ---------------------------------------------------------------------------
# sign in and navigation
# ---------------------------------------------------------------------------

def login_on_idp(ctx, user):
    """Generic login page: a user name field, a password field and a submit button."""
    pg = page(ctx)
    user_field = pg.get_by_label(re.compile(r"user ?name|e-?mail|login", re.I))
    if not visible_count(user_field):
        user_field = pg.locator("input[type=text], input[type=email], input:not([type])")
    user_field.first.fill(user)
    pg.locator("input[type=password]").first.fill(ctx.cfg.password)
    submit = pg.get_by_role("button", name=re.compile(r"sign ?in|log ?in", re.I))
    if not visible_count(submit):
        submit = pg.locator("input[type=submit], button[type=submit]")
    submit.first.click()


def wait_for_ui(ctx):
    pg = page(ctx)
    base = ctx.cfg.sm_ui_url
    try:
        pg.wait_for_url(lambda u: u.startswith(base), timeout=WAIT * 2000)
    except Exception:
        raise AssertionError(f"the browser did not come back to the UI, it shows {pg.url}")
    settle(ctx, 0.5)


@given('I am signed in to the UI as "{user:User}"')
def ui_signed_in(ctx, user):
    ctx.ui_user = user
    pg = page(ctx)
    if ctx.cfg.ui_auth == "none":
        # The UI has no sign-in; the user's roles cannot be chosen (environment).
        pg.goto(ctx.cfg.sm_ui_url + "/")
        settle(ctx)
        return
    pg.goto(ctx.cfg.sm_ui_url + "/")
    settle(ctx)
    if not pg.url.startswith(ctx.cfg.sm_ui_url):
        login_on_idp(ctx, user)
    wait_for_ui(ctx)
    if pg.locator("input[type=password]").count() and visible_count(pg.locator("input[type=password]")):
        raise AssertionError(f"sign-in as {user} failed, still on a login page: {pg.url}")


@when('I open the page "{path:Str}"')
def ui_open(ctx, path):
    pg = page(ctx)
    pg.goto(ctx.cfg.sm_ui_url + S(ctx, path))
    if not pg.url.startswith(ctx.cfg.sm_ui_url) and ctx.cfg.ui_auth != "none":
        login_on_idp(ctx, getattr(ctx, "ui_user", "ga-admin"))
    wait_for_ui(ctx)
    ui(ctx).checkpoint = len(ui(ctx).requests)


# ---------------------------------------------------------------------------
# actions
# ---------------------------------------------------------------------------

@when('I click "{testid:Str}"')
def ui_click(ctx, testid):
    tid = S(ctx, testid)
    ensure_row_for(ctx, tid)
    loc = need(ctx, tid)
    m = ROW_TESTID.match(tid)
    if m and m.group(1) == "schema-row":
        # SIMPL-15837/SIMPL-32790: "I click on the name of a schema in the list".
        name_el = loc.get_by_text(m.group(2), exact=True)
        target = name_el.first if name_el.count() else loc
        target.click()
    elif tid.startswith("action-download"):
        try:
            with page(ctx).expect_download(timeout=WAIT * 1000) as info:
                loc.click()
            ui(ctx).downloads.append(info.value)
        except Exception as exc:
            ui(ctx).download_error = str(exc).splitlines()[0]
    else:
        loc.click()
    settle(ctx, 0.5)


@when('I click the column header "{text:Str}"')
def ui_click_header(ctx, text):
    # D53/D54 fix the header texts; the header is a column header (role) of the visible table.
    pattern = re.compile(rf"^\s*{re.escape(text)}\b", re.I)
    header = page(ctx).get_by_role("columnheader", name=pattern)
    assert visible_count(header), f"no column header '{text}'"
    header.first.click()
    page(ctx).wait_for_timeout(500)


@when('I fill "{testid:Str}" with "{value:Str}"')
def ui_fill(ctx, testid, value):
    need(ctx, testid).fill(S(ctx, value))
    page(ctx).wait_for_timeout(200)


@when('I clear "{testid:Str}"')
def ui_clear(ctx, testid):
    need(ctx, testid).fill("")
    page(ctx).wait_for_timeout(200)


@when('I select "{value:Str}" in "{testid:Str}"')
def ui_select(ctx, value, testid):
    do_select(ctx, testid, value)


@when('I choose the file "{fixture:Str}" in "{testid:Str}"')
def ui_file(ctx, fixture, testid):
    file_input(need(ctx, testid)).set_input_files(fixture_payload(ctx, fixture))
    page(ctx).wait_for_timeout(200)


def fill_create_form(ctx, alias, stype, rt, fixture):
    name = S(ctx, alias)
    need(ctx, "field-name").fill(name)
    need(ctx, "field-title").fill(f"Title of {name}")
    need(ctx, "field-description").fill(f"Description of {name}")
    do_select(ctx, "field-schema-type", stype)
    if rt is not None:
        do_select(ctx, "field-resource-type", rt)
    file_input(need(ctx, "field-file")).set_input_files(fixture_payload(ctx, fixture))
    page(ctx).wait_for_timeout(200)


@when('I fill the create form for the RESOURCE schema "{alias:Str}" with resource type "{rt:Str}" and file "{fixture:Str}"')
def ui_fill_create(ctx, alias, rt, fixture):
    fill_create_form(ctx, alias, "RESOURCE", rt, fixture)


@when('I fill the create form for the SEARCH schema "{alias:Str}" with file "{fixture:Str}"')
def ui_fill_create_search(ctx, alias, fixture):
    fill_create_form(ctx, alias, "SEARCH", None, fixture)


@given('the API answers "{method:Str} {path:Str}" with status {code:Int}')
def ui_fault(ctx, method, path, code):
    """Fault injection for any implementation: the browser gets a Problem Details
    answer for the matching API request (method and path, any query)."""
    want_path = S(ctx, path)

    def handler(route):
        req = route.request
        rel = relative(ctx, req.url) if api_base_of(ctx, req.url) else None
        if rel is not None and req.method == method and urlsplit(rel).path.rstrip("/") == want_path.rstrip("/"):
            route.fulfill(status=code, content_type="application/problem+json",
                          body=('{"type":"about:blank","title":"Injected failure","status":%d,'
                                '"detail":"Injected failure for the test."}' % code))
        else:
            route.continue_()

    ui(ctx).context.route(lambda u: api_base_of(ctx, u) is not None, handler)


# ---------------------------------------------------------------------------
# checks on elements
# ---------------------------------------------------------------------------

@then('"{testid:Str}" is visible')
def ui_visible(ctx, testid):
    ensure_row_for(ctx, testid)

    def check():
        assert visible_count(by_testid(ctx, testid)), f"'{S(ctx, testid)}' is not visible"
    eventually(check)


@then('"{testid:Str}" is not rendered')
def ui_not_rendered(ctx, testid):
    ensure_row_for(ctx, testid)
    settle(ctx, SETTLE)

    def check():
        loc = by_testid(ctx, testid)
        n = visible_count(loc)
        assert n == 0, f"'{S(ctx, testid)}' is rendered ({n} visible): {norm(loc.first.inner_text())[:200]!r}"
    eventually(check, timeout=5)


@then('"{testid:Str}" is disabled')
def ui_disabled(ctx, testid):
    def check():
        assert control_disabled(need(ctx, testid, 1)), f"'{S(ctx, testid)}' is enabled"
    eventually(check, timeout=5)


@then('"{testid:Str}" is enabled')
def ui_enabled(ctx, testid):
    def check():
        assert not control_disabled(need(ctx, testid, 1)), f"'{S(ctx, testid)}' is disabled"
    eventually(check, timeout=5)


@then('"{testid:Str}" is read-only and shows "{value:Str}"')
def ui_readonly(ctx, testid, value):
    expected = S(ctx, value)

    def check():
        loc = need(ctx, testid, 1)
        tag = tag_of(loc)
        if tag in ("input", "textarea", "select"):
            readonly = (not loc.is_editable() or control_disabled(loc)
                        or (loc.get_attribute("aria-readonly") or "").lower() == "true")
            assert readonly, f"'{testid}' can be changed by the user"
        shown = norm(select_value(loc))
        assert expected.lower() in shown.lower(), f"'{testid}' shows {shown!r}, expected {expected!r}"
    eventually(check, timeout=5)


@then('"{testid:Str}" is empty')
def ui_empty(ctx, testid):
    def check():
        value = raw_value(need(ctx, testid, 1))
        assert norm(value) == "", f"'{testid}' holds {value!r}"
    eventually(check, timeout=5)


def check_shows(ctx, testid, text):
    expected = norm(S(ctx, text))
    ensure_row_for(ctx, testid)

    def check():
        loc = by_testid(ctx, testid)
        shown = [norm(loc.nth(i).inner_text()) for i in range(loc.count()) if loc.nth(i).is_visible()]
        assert shown, f"'{S(ctx, testid)}' is not visible"
        assert any(expected in s for s in shown), f"'{S(ctx, testid)}' shows {shown}, expected {expected!r}"
    eventually(check, timeout=WAIT * 2)


@then('"{testid:Str}" shows "{text:Str}"')
def ui_shows(ctx, testid, text):
    check_shows(ctx, testid, text)


@then('"{testid:Str}" shows \'{text:SQ}\'')
def ui_shows_sq(ctx, testid, text):
    check_shows(ctx, testid, text)


@then('"{testid:Str}" has the value "{value:Str}"')
def ui_value(ctx, testid, value):
    expected = S(ctx, value)

    def check():
        loc = need(ctx, testid, 1)
        got = raw_value(loc)
        assert norm(got) == expected or norm(select_value(loc)) == expected, \
            f"'{testid}' has the value {got!r}, expected {expected!r}"
    eventually(check, timeout=5)


@then('"{testid:Str}" offers the options "{options:Str}"')
def ui_options(ctx, testid, options):
    expected = sorted(o.strip().lower() for o in options.split(","))
    loc = need(ctx, testid)
    opts = options_of(loc)
    labels = sorted(lbl.lower() for _, lbl in opts)
    values = sorted(v.lower() for v, _ in opts)
    assert expected in (labels, values), f"'{testid}' offers {opts}, expected {options}"


@then('"{testid:Str}" has the column headers "{headers:Str}"')
def ui_headers(ctx, testid, headers):
    expected = [header_key(h) for h in headers.split(",")]

    def check():
        table = need(ctx, testid, 1)
        got = table_headers(table)
        keys = [header_key(h) for h in got]
        # D57 gives every version row a download action; one trailing action
        # column ("Actions" or no text) is accepted next to the D54 columns.
        if len(keys) == len(expected) + 1 and keys[-1] in ("", "actions") and "actions" not in expected:
            keys = keys[:-1]
        assert keys == expected, f"'{testid}' has the headers {got}, expected {headers}"
    eventually(check, timeout=5)


@then('the row "{testid:Str}" shows "{text:Str}" in the column "{column:Str}"')
def ui_row_cell(ctx, testid, text, column):
    expected = norm(S(ctx, text))
    tid = S(ctx, testid)

    def check():
        row = page(ctx).get_by_test_id(tid)
        if not visible_count(row):
            ensure_row_for(ctx, tid)
        row = row.first
        assert row.is_visible(), f"row '{tid}' is not visible"
        table = row.locator("xpath=ancestor::table[1]")
        idx = column_index(table, column)
        cells = row_cells(row)
        assert idx < len(cells), f"row '{tid}' has only {len(cells)} cells: {cells}"
        assert expected.lower() in cells[idx].lower(), \
            f"row '{tid}' shows {cells[idx]!r} in '{column}', expected {expected!r}"
    eventually(check, timeout=WAIT * 2)


@then('the rows of "{testid:Str}" are ordered by "{column:Str}" {direction:Str}')
def ui_order(ctx, testid, column, direction):
    descending = direction.strip().lower().startswith("desc")

    def check():
        table = need(ctx, testid, 1)
        idx = column_index(table, column)
        rows = data_rows(table)
        values = [row_cells(r)[idx] for r in rows.all()]
        assert len(values) >= 2, f"'{testid}' has {len(values)} rows, too few to check an order"
        candidates = sort_keys(column, values)
        ok = any(keys == sorted(keys, reverse=descending) for keys in candidates)
        assert ok, f"'{testid}' is not ordered by {column} {direction}: {values}"
    eventually(check, timeout=5)


@then('the first action of "{testid:Str}" is "{action:Str}"')
def ui_first_action(ctx, testid, action):
    ensure_row_for(ctx, testid)
    row = need(ctx, testid)
    actions = row.locator("[data-testid^='action-']")
    assert actions.count(), f"row '{S(ctx, testid)}' has no actions"
    first = actions.first.get_attribute("data-testid")
    assert first == S(ctx, action), f"the first action of '{S(ctx, testid)}' is '{first}', expected '{S(ctx, action)}'"


# ---------------------------------------------------------------------------
# checks on navigation, requests and downloads
# ---------------------------------------------------------------------------

@then('the browser shows the page "{path:Str}"')
def ui_page(ctx, path):
    exp = urlsplit(S(ctx, path))

    def check():
        act = urlsplit(page(ctx).url)
        same = (act.path.rstrip("/") == exp.path.rstrip("/")
                and sorted(parse_qsl(act.query)) == sorted(parse_qsl(exp.query)))
        assert same, f"the browser shows {page(ctx).url}, expected {ctx.cfg.sm_ui_url}{S(ctx, path)}"
    eventually(check)


@then('the UI sent the request "{method:Str} {path:Str}"')
def ui_sent(ctx, method, path):
    expected = S(ctx, path)

    def check():
        seen = api_requests(ctx)
        hits = [u for m, u in seen if request_matches(method, expected, m, relative(ctx, u))]
        assert hits, (f"no request {method} {expected} to {ctx.cfg.sm_ui_api_urls}; API requests seen: "
                      + "; ".join(f"{m} {relative(ctx, u)}" for m, u in seen))
    eventually(check)


@then("the UI sent no request to the API")
def ui_sent_none(ctx):
    settle(ctx, SETTLE)
    sent = api_requests(ctx, ui(ctx).checkpoint)
    assert not sent, "the UI sent: " + "; ".join(f"{m} {relative(ctx, u)}" for m, u in sent)


def wait_download(ctx):
    def check():
        assert ui(ctx).downloads, ("no file was downloaded"
                                   + (f" ({ui(ctx).download_error})" if ui(ctx).download_error else ""))
        return ui(ctx).downloads[-1]
    return eventually(check, timeout=5)


@then('a file named "{name:Str}" is downloaded')
def ui_download(ctx, name):
    dl = wait_download(ctx)
    assert dl.suggested_filename == S(ctx, name), \
        f"the file is named '{dl.suggested_filename}', expected '{S(ctx, name)}'"


@then('a file named "{name:Str}" is downloaded with the content uploaded for version "{version:Str}" of schema "{alias:Str}"')
def ui_download_content(ctx, name, version, alias):
    dl = wait_download(ctx)
    with open(dl.path(), "rb") as fh:
        got = fh.read()
    stype = concept(ctx, alias)["type"]
    resp = requests.get(f"{ctx.cfg.sm_base_url}/schemas/{S(ctx, alias)}/{version}?schemaType={stype}",
                        headers={**admin_headers(ctx), "Accept": "text/turtle"},
                        timeout=ctx.cfg.http_timeout)
    assert resp.status_code == 200, f"the API did not return version {version}: {resp.status_code}"
    uploaded = ctx.uploads.get((alias, stype, version))
    problems = []
    if dl.suggested_filename != S(ctx, name):
        problems.append(f"the file is named '{dl.suggested_filename}', expected '{S(ctx, name)}'")
    if got != resp.content:
        hint = " (it is the content of another upload)" if got in ctx.uploads.values() else ""
        problems.append(f"the file content ({len(got)} bytes) differs from version {version} in the API"
                        f" ({len(resp.content)} bytes){hint}")
    if uploaded is not None and resp.content != uploaded:
        problems.append(f"the API returns other bytes than were uploaded for version {version}")
    assert not problems, "; ".join(problems)
