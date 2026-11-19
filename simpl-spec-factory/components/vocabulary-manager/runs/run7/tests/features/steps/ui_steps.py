"""Browser steps for the @ui scenarios (step 60), Playwright for Python.

Rules (D75): elements are found only by roles, the decided labels and the decided texts
(D18, D31, D35, D36, D60, D72, D74, D75). No test ids, CSS classes or accessible names
taken from a build. Routes come from D31 and D74; the API path from E2.

Each scenario gets a new browser context: no stored cookies, storage or sessions.
The sign-in goes through the Keycloak login page of E1 (realm 'simpl').

Assumptions, listed in COVERAGE.md:
  - 'the button X' accepts a button or a link: D18 and D60 call 'New Vocabulary +' and
    'Update' actions without fixing the role. 'Submit' and 'Cancel' are buttons (D60).
  - 'I open the version n' clicks the control in the 'Version' cell of that row, or the
    cell itself (D35 says selecting a version opens the panel, not how).
  - 'points to version n' means: following the link shows the region 'Version n' (D36, D75).
  - A field error is the field's accessible description or error message, or
    aria-invalid=true (D37 'under the field' gives no role).
"""
import json
import os
import re
import time
from urllib.parse import urlsplit

import parse
from behave import given, register_type, then, when

import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from vmlib import client  # noqa: E402


@parse.with_pattern(r'[^"]*')
def _parse_q(text):
    return text


register_type(Q=_parse_q)

UI_URL = os.environ.get("UI_URL", "http://localhost:4433").rstrip("/")
AUTH_URL = client.TOKEN_URL.rsplit("/", 1)[0] + "/auth"
KC_ORIGIN = "{0.scheme}://{0.netloc}".format(urlsplit(client.TOKEN_URL))
UI_TIMEOUT = float(os.environ.get("UI_TIMEOUT", "15"))  # seconds
FIXTURES = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "fixtures"))
REPORTS = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "reports"))
WCAG_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]


# ------------------------------------------------------------------ placeholders
# Same rules as api_steps.expand (the step modules cannot import each other).


def vname(ctx, alias):
    return f"V{ctx.rid}{alias}"


def expand(ctx, text):
    def rep(m):
        kind, _, arg = m.group(1).partition(":")
        if kind == "rid":
            return ctx.rid
        if kind == "RID":
            return ctx.rid.upper()
        if kind == "V":
            return vname(ctx, arg)
        if kind == "S":
            return str(ctx.saved[arg])
        if kind == "SD":
            return str(ctx.saved[arg])[:10]
        if kind == "EMAIL":
            return client.USERS[arg]["email"]
        if kind == "OWN":
            return client.own_namespace(ctx.rid, arg)
        if kind == "CB":
            return client.CONTENT_BASE
        raise ValueError("unknown placeholder " + m.group(0))

    return re.sub(r"\$\{([^}]*)\}", rep, text)


def split_list(text):
    return [x.strip() for x in text.split(",") if x.strip()]


_CURRENT = {}


def poll(check, what, timeout=UI_TIMEOUT):
    """Retry `check` until it returns a truthy value; fail with the last observation."""
    deadline = time.monotonic() + timeout
    last = None
    while True:
        try:
            ok, last = check()
            if ok:
                return last
        except Exception as e:  # element detached while the page re-renders
            last = repr(e)
        if time.monotonic() > deadline:
            raise AssertionError(f"{what}; last seen: {last}")
        # Wait through Playwright, so its event loop keeps serving routes and events.
        if _CURRENT.get("page") is not None:
            _CURRENT["page"].wait_for_timeout(250)
        else:
            time.sleep(0.25)


# ------------------------------------------------------------------ browser


def browser(ctx):
    """One browser per run, kept in a holder made in before_all (behave drops
    attributes set inside a scenario when it ends)."""
    holder = ctx.ui_holder
    if holder.get("browser") is None:
        from playwright.sync_api import sync_playwright

        pw = sync_playwright().start()
        headless = os.environ.get("HEADED", "") == ""
        holder["pw"] = pw
        holder["browser"] = pw.chromium.launch(headless=headless)
    return holder["browser"]


def page(ctx):
    """The page of this scenario, in a fresh context without stored sessions."""
    if getattr(ctx, "ui_page", None) is None:
        bctx = browser(ctx).new_context(accept_downloads=True, storage_state=None)
        bctx.set_default_timeout(UI_TIMEOUT * 1000)
        pg = bctx.new_page()
        ctx.ui_context = bctx
        ctx.ui_page = pg
        _CURRENT["page"] = pg
        ctx.ui_writes = []
        ctx.ui_downloads = []
        ctx.ui_stubs = []

        def on_request(req):
            if req.resource_type in ("fetch", "xhr") and req.method in ("POST", "PUT", "PATCH", "DELETE"):
                if not req.url.startswith(KC_ORIGIN):
                    ctx.ui_writes.append(f"{req.method} {req.url}")

        pg.on("request", on_request)
        pg.on("download", lambda d: ctx.ui_downloads.append(d))
        bctx.route("**/*", lambda route, req: _route(ctx, route, req))
    return ctx.ui_page


PROBLEM = {
    403: ("urn:problem-type:simpl:forbidden", "Forbidden"),
    404: ("urn:problem-type:belgif:resourceNotFound", "Resource Not Found"),
    500: ("urn:problem-type:belgif:internalServerError", "Internal Server Error"),
    503: ("urn:problem-type:belgif:serviceUnavailable", "Service Unavailable"),
}


def _route(ctx, route, req):
    """API stubs of the 'the API answers' steps (D24, E3). Only fetch/xhr calls are
    matched, so page navigations to the same route are never stubbed."""
    if req.resource_type in ("fetch", "xhr") and not req.url.startswith(KC_ORIGIN):
        path = urlsplit(req.url).path
        for method, api_path, code, detail in ctx.ui_stubs:
            if req.method == method and path.endswith(api_path):
                ptype, title = PROBLEM.get(code, ("about:blank", "Error"))
                body = {"type": ptype, "title": title, "status": code,
                        "detail": detail or f"Stubbed {code} for {method} {api_path}"}
                headers = {"Retry-After": "5"} if code == 503 else {}
                return route.fulfill(status=code, content_type="application/problem+json",
                                     body=json.dumps(body), headers=headers)
    return route.continue_()


def settle(pg):
    try:
        pg.wait_for_load_state("networkidle", timeout=UI_TIMEOUT * 1000)
    except Exception:
        pass
    pg.wait_for_timeout(300)


def on_ui(pg):
    return pg.url.startswith(UI_URL)


# ------------------------------------------------------------------ locators


def clickable(pg, label):
    """A button or a link with this exact name ('the button X')."""
    return pg.get_by_role("button", name=label, exact=True).or_(
        pg.get_by_role("link", name=label, exact=True)
    )


def field(pg, label):
    return pg.get_by_label(label, exact=True)


TABLE_JS = """
(table) => {
  const rowSel = 'tr, [role=row]';
  const cellSel = 'th, td, [role=cell], [role=gridcell], [role=columnheader], [role=rowheader]';
  const rows = [];
  for (const r of table.querySelectorAll(rowSel)) {
    if (r.closest('table, [role=table], [role=grid]') !== table) continue;
    const cells = [...r.querySelectorAll(cellSel)].filter(c => c.closest(rowSel) === r);
    const header = cells.length > 0 && cells.every(c => c.tagName === 'TH' && c.closest('thead')
                    || c.getAttribute('role') === 'columnheader');
    // Visible text without parts hidden from assistive technology (sort arrows, icons).
    const text = (c) => {
      const k = c.cloneNode(true);
      k.querySelectorAll('[aria-hidden=true]').forEach(n => n.remove());
      document.body.appendChild(k); k.style.position = 'absolute'; k.style.left = '-99999px';
      const t = (k.innerText || '').replace(/\\s+/g, ' ').trim();
      k.remove();
      return t;
    };
    rows.push({header, cells: cells.map(text)});
  }
  return rows;
}
"""


def tables(pg):
    return pg.get_by_role("table").or_(pg.get_by_role("grid"))


def read_table(pg, column):
    """(headers, data rows) of the first visible table that has the column header."""
    tl = tables(pg)
    for i in range(tl.count()):
        t = tl.nth(i)
        if not t.is_visible():
            continue
        rows = t.evaluate(TABLE_JS)
        heads = [r["cells"] for r in rows if r["header"]]
        if heads and column in heads[0]:
            return heads[0], [r["cells"] for r in rows if not r["header"]], t
    return None, [], None


def column_values(pg, column):
    heads, data, _ = read_table(pg, column)
    if heads is None:
        return None
    i = heads.index(column)
    return [r[i] if i < len(r) else "" for r in data]


def vocab_row(ctx, alias):
    name = expand(ctx, alias) if "${" in alias else vname(ctx, alias)
    pg = page(ctx)
    return pg.get_by_role("row").filter(
        has=pg.get_by_role("cell", name=name, exact=True)
    ).or_(pg.get_by_role("row").filter(has=pg.get_by_role("link", name=name, exact=True))).first


def version_row(ctx, v):
    """The row whose 'Version' cell is the number v."""
    pg = page(ctx)

    def find():
        heads, data, t = read_table(pg, "Version")
        if t is None:
            return False, "no table with a 'Version' column"
        i = heads.index("Version")
        for n, r in enumerate(data):
            if i < len(r) and re.match(rf"^{v}\b", r[i]):
                return True, (t, n, r)
        return False, [r[i] if i < len(r) else "" for r in data]

    t, n, _ = poll(find, f"no version row {v}")
    rows = t.get_by_role("row")
    # data rows follow the header row(s); count the header rows
    k = 0
    for j in range(rows.count()):
        if rows.nth(j).get_by_role("columnheader").count() == 0:
            if k == n:
                return rows.nth(j)
            k += 1
    raise AssertionError(f"version row {v} not found")


# ------------------------------------------------------------------ Given


@given('I am signed in to the UI as "{user:Q}"')
def g_signed_in(ctx, user):
    pg = page(ctx)
    pg.goto(UI_URL + "/vocabularies")
    poll(lambda: (pg.url.startswith(AUTH_URL), pg.url), "the UI did not send the browser to the login page")
    u = client.USERS[user]
    pg.get_by_label("Username or email").fill(u["username"])
    pg.get_by_label("Password", exact=True).fill(client.TEST_PASSWORD)
    pg.get_by_role("button", name="Sign In").click()
    poll(lambda: (on_ui(pg), pg.url), "sign-in did not return to the UI")
    settle(pg)


@given("I am not signed in to the UI")
def g_not_signed_in(ctx):
    page(ctx)  # a fresh context holds no session


@given('the API answers {method:w} "{path:Q}" with status {code:d}')
def g_stub(ctx, method, path, code):
    page(ctx)
    ctx.ui_stubs.append((method.upper(), expand(ctx, path), code, None))


@given('the API answers {method:w} "{path:Q}" with status {code:d} and detail "{detail:Q}"')
def g_stub_detail(ctx, method, path, code, detail):
    page(ctx)
    ctx.ui_stubs.append((method.upper(), expand(ctx, path), code, expand(ctx, detail)))


# ------------------------------------------------------------------ When


@when('I open the page "{route:Q}"')
def w_open(ctx, route):
    pg = page(ctx)
    pg.goto(UI_URL + expand(ctx, route))
    settle(pg)


@when('I click the button "{label:Q}"')
def w_click_button(ctx, label):
    pg = page(ctx)
    clickable(pg, expand(ctx, label)).first.click()
    settle(pg)


@when('I click the link "{text:Q}"')
def w_click_link(ctx, text):
    pg = page(ctx)
    pg.get_by_role("link", name=expand(ctx, text), exact=True).first.click()
    settle(pg)


@when('I click the column header "{column:Q}"')
def w_click_header(ctx, column):
    pg = page(ctx)
    head = pg.get_by_role("columnheader", name=column, exact=True).first
    inner = head.get_by_role("button")
    (inner.first if inner.count() else head).click()
    settle(pg)


@when('I fill in "{label:Q}" with "{value:Q}"')
def w_fill(ctx, label, value):
    pg = page(ctx)
    field(pg, label).fill(expand(ctx, value))
    settle(pg)


@when('I clear the field "{label:Q}"')
def w_clear(ctx, label):
    pg = page(ctx)
    field(pg, label).fill("")
    settle(pg)


@when('I select "{option:Q}" in "{label:Q}"')
def w_select(ctx, option, label):
    pg = page(ctx)
    field(pg, label).select_option(label=option)
    settle(pg)


def choose_file(ctx, label, name, data, mime="text/turtle"):
    pg = page(ctx)
    payload = {"name": name, "mimeType": mime, "buffer": data}
    loc = field(pg, label)
    try:
        loc.set_input_files(payload, timeout=3000)
    except Exception:
        with pg.expect_file_chooser() as fc:
            loc.click()
        fc.value.set_files(payload)
    settle(pg)


def fixture_bytes(ctx, name):
    with open(os.path.join(FIXTURES, name), "rb") as f:
        return expand(ctx, f.read().decode("utf-8")).encode("utf-8")


@when('I choose the file "{fixture:Q}" for "{label:Q}"')
def w_choose_fixture(ctx, fixture, label):
    choose_file(ctx, label, fixture, fixture_bytes(ctx, fixture))


@when('I choose a valid file of {n:d} bytes for "{label:Q}"')
def w_choose_sized(ctx, n, label):
    choose_file(ctx, label, f"big{n}.ttl", client.sized_ttl(ctx.rid, "Generated", n))


@when('I choose the current content of version {v:d} of "{alias:Q}" for "{label:Q}"')
def w_choose_version_content(ctx, v, alias, label):
    name = vname(ctx, alias)
    data = ctx.contents[(name, v)]
    choose_file(ctx, label, f"{name}.ttl", data)


def open_menu(ctx, menu):
    pg = page(ctx)
    name = expand(ctx, menu)
    btn = pg.get_by_role("button", name=name, exact=True).first
    btn.click()
    items = pg.get_by_role("menu", name=name).get_by_role("menuitem").or_(
        pg.get_by_role("menu").get_by_role("menuitem")
    )
    items.first.wait_for(state="visible")
    return items


@when('I choose "{item:Q}" in the actions menu "{menu:Q}"')
def w_choose_menu(ctx, item, menu):
    pg = page(ctx)
    open_menu(ctx, menu)
    pg.get_by_role("menuitem", name=expand(ctx, item), exact=True).first.click()
    settle(pg)


@when("I open the version {v:d}")
def w_open_version(ctx, v):
    pg = page(ctx)
    row = version_row(ctx, v)
    heads, _, _ = read_table(pg, "Version")
    cell = row.get_by_role("cell").nth(heads.index("Version"))
    ctl = cell.get_by_role("button").or_(cell.get_by_role("link"))
    (ctl.first if ctl.count() else cell).click()
    settle(pg)


MARK_JS = """
() => {
  window.__kbSeen = window.__kbSeen || new Set();
  if (document.activeElement) window.__kbSeen.add(document.activeElement);
}
"""

UNREACHED_JS = """
() => {
  const sel = 'a[href], button, input, select, textarea, summary, [tabindex], [role=button], [role=link], [role=menuitem]';
  const seen = window.__kbSeen || new Set();
  const out = [];
  for (const el of document.querySelectorAll(sel)) {
    if (el.disabled || el.getAttribute('tabindex') === '-1' && !el.matches('a[href],button,input,select,textarea')) continue;
    const r = el.getBoundingClientRect();
    const st = getComputedStyle(el);
    if (r.width === 0 || r.height === 0 || st.visibility === 'hidden' || st.display === 'none') continue;
    if (el.closest('[aria-hidden=true], [inert]')) continue;
    if (el.matches('input[type=radio]') && [...seen].some(s => s.name && s.name === el.name)) continue;
    if (![...seen].some(s => s === el || s.contains(el) || el.contains(s))) {
      out.push((el.tagName + ' ' + (el.getAttribute('aria-label') || el.innerText || el.getAttribute('name') || '')).trim().slice(0, 80));
    }
  }
  return out;
}
"""


@when("I move through the page with the keyboard only")
def w_keyboard(ctx):
    pg = page(ctx)
    pg.evaluate("() => { window.__kbSeen = new Set(); document.activeElement && document.activeElement.blur(); }")
    n = pg.evaluate("() => document.querySelectorAll('a[href],button,input,select,textarea,[tabindex]').length")
    for _ in range(n * 2 + 10):
        pg.keyboard.press("Tab")
        pg.evaluate(MARK_JS)
    ctx.ui_unreached = pg.evaluate(UNREACHED_JS)
    ctx.ui_reached = pg.evaluate("() => (window.__kbSeen || new Set()).size")


# ------------------------------------------------------------------ Then: texts


def body_text(pg):
    return pg.locator("body").inner_text()


@then('I see the text "{text:Q}"')
def t_see_text(ctx, text):
    pg = page(ctx)
    want = expand(ctx, text)
    poll(lambda: (want in body_text(pg), body_text(pg)[:800]), f"text {want!r} not shown")


@then('I do not see the text "{text:Q}"')
def t_not_see_text(ctx, text):
    pg = page(ctx)
    settle(pg)
    want = expand(ctx, text)
    assert want not in body_text(pg), f"text {want!r} is shown"


def role_with_text(ctx, role, text):
    pg = page(ctx)
    want = expand(ctx, text)

    def check():
        loc = pg.get_by_role(role)
        texts = [loc.nth(i).inner_text() for i in range(loc.count()) if loc.nth(i).is_visible()]
        return any(want in t for t in texts), texts

    poll(check, f"no visible {role} with text {want!r}")


@then('I see a status message "{text:Q}"')
def t_status_message(ctx, text):
    role_with_text(ctx, "status", text)


@then('I see an alert "{text:Q}"')
def t_alert(ctx, text):
    role_with_text(ctx, "alert", text)


@then('I see the action "{label:Q}"')
@then('I see the button "{label:Q}"')
def t_see_button(ctx, label):
    pg = page(ctx)
    clickable(pg, expand(ctx, label)).first.wait_for(state="visible")


@then('I do not see the action "{label:Q}"')
@then('I do not see the button "{label:Q}"')
def t_not_see_button(ctx, label):
    pg = page(ctx)
    settle(pg)
    loc = clickable(pg, expand(ctx, label))
    visible = [i for i in range(loc.count()) if loc.nth(i).is_visible()]
    assert not visible, f"{label!r} is shown"


@then('I see the menu item "{label:Q}"')
def t_menu_item(ctx, label):
    pg = page(ctx)
    nav = pg.get_by_role("navigation")
    loc = nav.get_by_role("link", name=label, exact=True).or_(
        nav.get_by_role("menuitem", name=label, exact=True)).or_(
        nav.get_by_role("button", name=label, exact=True))
    loc.first.wait_for(state="visible")


@then('I see the region "{name:Q}"')
def t_region(ctx, name):
    page(ctx).get_by_role("region", name=expand(ctx, name), exact=True).first.wait_for(state="visible")


@then('I do not see the region "{name:Q}"')
def t_no_region(ctx, name):
    pg = page(ctx)
    settle(pg)
    loc = pg.get_by_role("region", name=expand(ctx, name), exact=True)
    assert loc.count() == 0 or not loc.first.is_visible(), f"region {name!r} is shown"


@then('the region "{name:Q}" shows "{text:Q}"')
def t_region_shows(ctx, name, text):
    pg = page(ctx)
    reg = pg.get_by_role("region", name=expand(ctx, name), exact=True).first
    want = expand(ctx, text)
    poll(lambda: (want in reg.inner_text(), reg.inner_text()[:800]), f"region {name!r} does not show {want!r}")


@then('the button "{label:Q}" is disabled')
def t_disabled(ctx, label):
    btn = page(ctx).get_by_role("button", name=label, exact=True).first
    poll(lambda: (btn.is_disabled(), "enabled"), f"button {label!r} is not disabled")


@then('the button "{label:Q}" is enabled')
def t_enabled(ctx, label):
    btn = page(ctx).get_by_role("button", name=label, exact=True).first
    poll(lambda: (btn.is_enabled(), "disabled"), f"button {label!r} is not enabled")


# ------------------------------------------------------------------ Then: tables


def _headers_check(ctx, columns, exact):
    pg = page(ctx)
    want = split_list(columns)

    def check():
        tl = tables(pg)
        seen = []
        for i in range(tl.count()):
            heads = [r["cells"] for r in tl.nth(i).evaluate(TABLE_JS) if r["header"]]
            if not heads:
                continue
            seen.append(heads[0])
            got = [h for h in heads[0] if h]
            if (got == want) if exact else all(w in got for w in want):
                return True, seen
        return False, seen

    poll(check, f"no table with the columns {want}")


@then('the table has the columns "{columns:Q}"')
def t_columns_exact(ctx, columns):
    _headers_check(ctx, columns, True)


@then('the table includes the columns "{columns:Q}"')
def t_columns_include(ctx, columns):
    _headers_check(ctx, columns, False)


@then('the vocabulary rows are "{aliases:Q}"')
def t_vocab_rows(ctx, aliases):
    pg = page(ctx)
    want = [vname(ctx, a) for a in split_list(aliases)]
    poll(lambda: ((column_values(pg, "Name") or []) == want, column_values(pg, "Name")),
         f"vocabulary rows are not {want}")


@then('the row of "{alias:Q}" shows "{text:Q}"')
def t_row_shows(ctx, alias, text):
    row = vocab_row(ctx, alias)
    want = expand(ctx, text)
    poll(lambda: (want in row.inner_text(), row.inner_text()), f"row of {alias} does not show {want!r}")


@then('the row of "{alias:Q}" shows a date matching "{pattern:Q}"')
def t_row_date(ctx, alias, pattern):
    row = vocab_row(ctx, alias)
    poll(lambda: (re.search(pattern, row.inner_text()) is not None, row.inner_text()),
         f"row of {alias} has no date matching {pattern}")


@then('the warning icon of "{alias:Q}" is named "{text:Q}"')
def t_warning_icon(ctx, alias, text):
    row = vocab_row(ctx, alias)
    want = expand(ctx, text)
    loc = row.get_by_role("img", name=want, exact=True).or_(row.get_by_title(want, exact=True)).or_(
        row.get_by_label(want, exact=True))
    loc.first.wait_for(state="attached")


@then('the version rows are "{versions:Q}"')
def t_version_rows(ctx, versions):
    pg = page(ctx)
    want = split_list(versions)

    def check():
        vals = column_values(pg, "Version") or []
        got = [re.match(r"^\s*(\d+)", x).group(1) if re.match(r"^\s*(\d+)", x) else x for x in vals]
        return got == want, got

    poll(check, f"version rows are not {want}")


@then('the version row {v:d} shows "{text:Q}"')
def t_version_row_shows(ctx, v, text):
    want = expand(ctx, text)

    def check():
        t = version_row(ctx, v).inner_text()
        return want in t, t

    poll(check, f"version row {v} does not show {want!r}")


@then('the version row {v:d} does not show "{text:Q}"')
def t_version_row_not_shows(ctx, v, text):
    settle(page(ctx))
    t = version_row(ctx, v).inner_text()
    want = expand(ctx, text)
    assert want not in t, f"version row {v} shows {want!r}: {t}"


def menu_items(ctx, menu):
    pg = page(ctx)
    items = open_menu(ctx, menu)
    names = [items.nth(i).inner_text().strip() for i in range(items.count()) if items.nth(i).is_visible()]
    pg.keyboard.press("Escape")
    pg.wait_for_timeout(200)
    return names


@then('the actions menu "{menu:Q}" offers "{items:Q}"')
def t_menu_offers(ctx, menu, items):
    got = menu_items(ctx, menu)
    missing = [i for i in split_list(items) if i not in got]
    assert not missing, f"menu {expand(ctx, menu)!r} lacks {missing}; offers {got}"


@then('the actions menu "{menu:Q}" does not offer "{items:Q}"')
def t_menu_not_offers(ctx, menu, items):
    got = menu_items(ctx, menu)
    present = [i for i in split_list(items) if i in got]
    assert not present, f"menu {expand(ctx, menu)!r} offers {present}"


# ------------------------------------------------------------------ Then: links, routes


@then('the link "{text:Q}" points to version {v:d} of "{alias:Q}"')
def t_link_version(ctx, text, v, alias):
    pg = page(ctx)
    pg.get_by_role("link", name=expand(ctx, text), exact=True).first.click()
    settle(pg)
    pg.get_by_role("region", name=f"Version {v}", exact=True).first.wait_for(state="visible")
    path = urlsplit(pg.url).path
    assert path == f"/vocabularies/{vname(ctx, alias)}", f"link led to {pg.url}"


@then('the link "{text:Q}" leads to "{route:Q}"')
def t_link_leads(ctx, text, route):
    pg = page(ctx)
    pg.get_by_role("link", name=expand(ctx, text), exact=True).first.click()
    want = expand(ctx, route)
    poll(lambda: (urlsplit(pg.url).path == want, pg.url), f"link did not lead to {want}")


@then('I am on the page "{route:Q}"')
def t_on_page(ctx, route):
    pg = page(ctx)
    want = expand(ctx, route)
    poll(lambda: (urlsplit(pg.url).path == want, pg.url), f"not on the page {want}")
    settle(pg)


@then("I am sent to the login page")
def t_login_page(ctx):
    pg = page(ctx)
    poll(lambda: (pg.url.startswith(AUTH_URL), pg.url), "not sent to the Keycloak login page")
    pg.get_by_label("Password", exact=True).wait_for(state="visible")


# ------------------------------------------------------------------ Then: downloads


@then('a file named "{filename:Q}" is downloaded with the content of version {v:d} of "{alias:Q}"')
def t_download(ctx, filename, v, alias):
    poll(lambda: (len(ctx.ui_downloads) > 0, "no download"), "no file was downloaded")
    d = ctx.ui_downloads[-1]
    want_name = expand(ctx, filename)
    assert d.suggested_filename == want_name, f"file name {d.suggested_filename!r}, want {want_name!r}"
    with open(d.path(), "rb") as f:
        got = f.read()
    want = ctx.contents[(vname(ctx, alias), v)]
    assert got == want, f"downloaded {len(got)} bytes differ from version {v} ({len(want)} bytes)"


# ------------------------------------------------------------------ Then: forms

FIELD_ERROR_JS = """
(el) => {
  const ids = ((el.getAttribute('aria-describedby') || '') + ' ' + (el.getAttribute('aria-errormessage') || '')).split(/\\s+/).filter(Boolean);
  const texts = ids.map(id => document.getElementById(id)).filter(Boolean).map(n => n.innerText.trim()).filter(Boolean);
  return {invalid: el.getAttribute('aria-invalid') === 'true', texts};
}
"""


def field_error(ctx, label):
    return field(page(ctx), label).first.evaluate(FIELD_ERROR_JS)


@then('the field "{label:Q}" shows an error')
def t_field_error(ctx, label):
    def check():
        e = field_error(ctx, label)
        return e["invalid"] or bool(e["texts"]), e

    poll(check, f"field {label!r} shows no error")


@then('the field "{label:Q}" shows the error "{text:Q}"')
def t_field_error_text(ctx, label, text):
    want = expand(ctx, text)

    def check():
        e = field_error(ctx, label)
        return any(want in t for t in e["texts"]), e

    poll(check, f"field {label!r} does not show the error {want!r}")


@then('the focus is on the field "{label:Q}"')
def t_focus(ctx, label):
    loc = field(page(ctx), label).first
    poll(lambda: (loc.evaluate("el => el === document.activeElement"),
                  page(ctx).evaluate("() => document.activeElement && document.activeElement.outerHTML.slice(0, 200)")),
         f"focus is not on {label!r}")


@then('the field "{label:Q}" has the value "{value:Q}"')
def t_field_value(ctx, label, value):
    loc = field(page(ctx), label).first
    want = expand(ctx, value)
    poll(lambda: (loc.input_value() == want, loc.input_value()), f"field {label!r} is not {want!r}")


@then('the field "{label:Q}" is empty')
def t_field_empty(ctx, label):
    loc = field(page(ctx), label).first
    settle(page(ctx))
    assert loc.input_value() == "", f"field {label!r} holds {loc.input_value()!r}"


@then('the form has the fields "{labels:Q}"')
def t_form_fields(ctx, labels):
    pg = page(ctx)
    for label in split_list(labels):
        field(pg, label).first.wait_for(state="attached")


@then('the form has no field "{label:Q}"')
def t_form_no_field(ctx, label):
    pg = page(ctx)
    settle(pg)
    assert field(pg, label).count() == 0, f"the form has a field {label!r}"


@then("no write request was sent to the API")
def t_no_write(ctx):
    pg = page(ctx)
    pg.wait_for_timeout(1000)
    assert not ctx.ui_writes, f"write requests sent: {ctx.ui_writes}"


# ------------------------------------------------------------------ Then: accessibility


@then("the page has no accessibility violations")
def t_axe(ctx):
    from axe_playwright_python.sync_playwright import Axe

    pg = page(ctx)
    settle(pg)
    res = Axe().run(pg, options={"runOnly": {"type": "tag", "values": WCAG_TAGS}})
    violations = res.response.get("violations", [])
    os.makedirs(os.path.join(REPORTS, "a11y"), exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9]+", "-", urlsplit(pg.url).path.replace(ctx.rid, "")).strip("-") or "root"
    with open(os.path.join(REPORTS, "a11y", f"{slug}.json"), "w") as f:
        json.dump({"url": pg.url, "rules_passed": len(res.response.get("passes", [])),
                   "violations": violations}, f, indent=2)
    assert res.response.get("passes"), "axe ran no rules on the page"
    summary = [
        f"{v['id']} ({v['impact']}): {v['help']} [{len(v['nodes'])} nodes] "
        + "; ".join(n["target"][0] if n.get("target") else "" for n in v["nodes"][:3])
        for v in violations
    ]
    assert not violations, "axe WCAG 2.1 AA violations:\n" + "\n".join(summary)


@then("every action on the page was reachable by keyboard")
def t_keyboard(ctx):
    assert ctx.ui_reached > 3, f"Tab reached only {ctx.ui_reached} elements"
    assert not ctx.ui_unreached, f"not reachable with Tab: {ctx.ui_unreached}"
