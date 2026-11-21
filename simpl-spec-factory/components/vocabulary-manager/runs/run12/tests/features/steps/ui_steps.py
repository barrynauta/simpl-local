"""Playwright steps for the @ui scenarios (step 60).

Elements are found only by what the decisions give: visible texts and labels
(D60, D72, D115), column headers (D23, D35, D100, D159), the accessible names
of D75 ('Actions for <Name>', 'Actions for version <n>', regions 'Version <n>',
'Validation report', 'Validation warnings'), roles status and alert (D75, D90,
D114), aria-current and aria-sort (D163), aria-required (D141), the routes of
D31 and D74 and the API path of E2 (/v1). Nothing is taken from a build.

The sign-in uses the Keycloak login form (E1); its labels belong to Keycloak.
Every scenario gets a fresh browser context, so no stored session is reused.
"""
import json
import os
import re
import time
from urllib.parse import urljoin, urlparse

from behave import given, then, when

try:  # the API virtualenv has no Playwright; @ui scenarios run in .venv-ui
    from playwright.sync_api import expect
    HAVE_PLAYWRIGHT = True
except ImportError:  # pragma: no cover
    expect = None
    HAVE_PLAYWRIGHT = False

import vmclient
from api_steps import fixture_bytes, split_list, sub

UI_URL = os.environ.get("UI_URL", "http://localhost:4433").rstrip("/")
WAIT_MS = int(os.environ.get("UI_TIMEOUT_MS", "15000"))
UPLOAD_WAIT_MS = int(vmclient.UPLOAD_TIMEOUT * 1000)
REPORT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.abspath(__file__)))), "reports", "axe")
LOGIN_URL = vmclient.TOKEN_URL.rsplit("/", 1)[0] + "/auth"

if HAVE_PLAYWRIGHT:
    expect.set_options(timeout=WAIT_MS)


# ---------------------------------------------------------------- helpers

def page(context):
    _ACTIVE["page"] = context.ui_page
    return context.ui_page


def settle(context):
    pg = page(context)
    try:
        pg.wait_for_load_state("networkidle", timeout=WAIT_MS)
    except Exception:
        pass


def label_re(label):
    """A form label as D60, optionally followed by the required marker of D141."""
    return re.compile(r"^\s*" + re.escape(label) + r"\s*\*?\s*$")


def field(context, label):
    return page(context).get_by_label(label_re(label))


_ACTIVE = {}


def pause(seconds):
    """Wait while letting Playwright deliver events (time.sleep would block them)."""
    pg = _ACTIVE.get("page")
    if pg is not None:
        pg.wait_for_timeout(seconds * 1000)
    else:
        time.sleep(seconds)


def poll(check, timeout_ms=WAIT_MS, interval=0.25):
    """Run check() until it returns without AssertionError or the time is up."""
    end = time.time() + timeout_ms / 1000.0
    while True:
        try:
            return check()
        except AssertionError:
            if time.time() >= end:
                raise
            pause(interval)


def vocab_row(context, name):
    pg = page(context)
    return pg.get_by_role("row").filter(
        has=pg.get_by_role("button", name=f"Actions for {name}", exact=True))


def version_row(context, version):
    pg = page(context)
    return pg.get_by_role("row").filter(
        has=pg.get_by_role("button", name=f"Actions for version {version}", exact=True))


def table_with_header(context, header):
    pg = page(context)
    return pg.get_by_role("table").filter(
        has=pg.get_by_role("columnheader", name=header, exact=True)).first


def body_rows(table):
    return table.get_by_role("row").filter(has=table.page.get_by_role("cell"))


def first_cells(table):
    texts = []
    for row in body_rows(table).all():
        cell = row.get_by_role("cell").first
        texts.append(cell.inner_text().strip())
    return texts


def clean_header(text):
    return re.sub(r"[^\w ]+", "", text).strip()


def menu_item(context, action):
    pg = page(context)
    pattern = re.compile(r"^\s*" + re.escape(action) + r"\s*$")
    return pg.get_by_role("menuitem", name=pattern).or_(
        pg.get_by_role("menu").get_by_text(pattern))


def open_menu(context, button_name):
    pg = page(context)
    button = pg.get_by_role("button", name=button_name, exact=True)
    expect(button).to_be_visible()
    button.click()
    return button


def close_menu(context):
    page(context).keyboard.press("Escape")


def stub_body(code, detail):
    titles = {400: "Bad Request", 403: "Forbidden", 404: "Not Found", 409: "Conflict",
              500: "Internal Server Error", 503: "Service Unavailable"}
    return {
        "type": "about:blank" if code != 503 else "urn:problem-type:belgif:serviceUnavailable",
        "title": titles.get(code, "Error"),
        "status": code,
        "detail": detail,
    }


def add_stub(context, code, method, path, detail=None):
    path = sub(context, path)
    method = method.upper()
    pattern = re.compile(r"/v1" + re.escape(path) + r"(\?.*)?$")

    def handler(route):
        req = route.request
        if req.method.upper() != method or not pattern.search(req.url.split("#")[0]):
            route.fallback()
            return
        if code == 200 and method == "GET" and path == "/vocabularies":
            payload = {"page": 1, "totalPages": 0, "total": 0, "pageSize": 20,
                       "self": "/vocabularies?page=1&pageSize=20",
                       "first": "/vocabularies?page=1&pageSize=20", "items": []}
            route.fulfill(status=200, content_type="application/json", body=json.dumps(payload))
            return
        text = sub(context, detail) if detail else f"Stubbed {code} for a UI test."
        route.fulfill(status=code, content_type="application/problem+json",
                      body=json.dumps(stub_body(code, text)))

    context.ui_context.route(re.compile(r".*/v1/.*"), handler)


def api_requests_since_mark(context):
    return [r for r in context.ui_requests[context.ui_mark:]
            if "/v1/" in r[1] and r[0] != "GET"]


# ---------------------------------------------------------------- sign-in

@given('I am signed in to the UI as "{user}"')
def ui_signed_in(context, user):
    pg = page(context)
    pg.goto(UI_URL + "/vocabularies")
    pg.wait_for_url(re.compile("^" + re.escape(LOGIN_URL)), timeout=WAIT_MS)
    pg.get_by_label("Username or email").fill(user)
    pg.get_by_label("Password", exact=True).fill(vmclient.PASSWORD)
    pg.get_by_role("button", name="Sign In").click()
    pg.wait_for_url(re.compile("^" + re.escape(UI_URL)), timeout=WAIT_MS)
    settle(context)
    context.ui_user = user


@given("I am not signed in to the UI")
def ui_not_signed_in(context):
    context.ui_user = None


@then("I am sent to the Keycloak login page")
def ui_login_page(context):
    pg = page(context)
    pg.wait_for_url(re.compile("^" + re.escape(LOGIN_URL)), timeout=WAIT_MS)
    expect(pg.get_by_label("Password", exact=True)).to_be_visible()


# ---------------------------------------------------------------- stubs

@given('the API answers {code:d} to "{method}" "{path}"')
def ui_stub(context, code, method, path):
    add_stub(context, code, method, path)


@given('the API answers {code:d} with detail "{detail}" to "{method}" "{path}"')
def ui_stub_detail(context, code, detail, method, path):
    add_stub(context, code, method, path, detail)


# ---------------------------------------------------------------- navigation and input

@when('I open the page "{route}"')
def ui_open(context, route):
    pg = page(context)
    pg.goto(UI_URL + sub(context, route))
    settle(context)


@when('I fill in "{label}" with "{value}"')
def ui_fill(context, label, value):
    target = field(context, label)
    expect(target).to_be_visible()
    target.fill(sub(context, value))
    if label == "Name contains":
        # the list reloads from the server (D19); wait for the request to finish
        pause(0.6)
        settle(context)


@when('I select "{option}" in "{label}"')
def ui_select(context, option, label):
    target = field(context, label)
    expect(target).to_be_visible()
    tag = target.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        target.select_option(label=option)
    else:
        target.click()
        page(context).get_by_role("option", name=option, exact=True).click()
    pause(0.6)
    settle(context)


@when('I choose the file "{fixture}" for "{label}"')
def ui_choose_file(context, fixture, label):
    data = fixture_bytes(context, fixture)
    field(context, label).set_input_files(
        {"name": fixture, "mimeType": "text/turtle", "buffer": data})
    settle(context)


@when('I choose a file of {size:d} bytes for "{label}"')
def ui_choose_big_file(context, size, label):
    data = vmclient.turtle_of_size(size, context.run)
    assert len(data) == size, f"generated {len(data)} bytes, wanted {size}"
    field(context, label).set_input_files(
        {"name": "big.ttl", "mimeType": "text/turtle", "buffer": data})
    settle(context)


@when('I click the button "{label}"')
def ui_click_button(context, label):
    context.ui_mark = len(context.ui_requests)
    button = page(context).get_by_role("button", name=sub(context, label), exact=True)
    expect(button).to_be_visible()
    button.click()
    pause(0.3)
    settle(context)


@when('I click the link "{label}"')
def ui_click_link(context, label):
    link = page(context).get_by_role("link", name=sub(context, label), exact=True).first
    expect(link).to_be_visible()
    link.click()
    settle(context)


@when('I click the column header "{label}"')
def ui_click_header(context, label):
    header = page(context).get_by_role("columnheader", name=label, exact=True)
    header.get_by_role("button").click()
    pause(0.3)
    settle(context)


@when('I choose "{action}" in the actions menu of "{name}"')
def ui_row_action(context, action, name):
    open_menu(context, f"Actions for {sub(context, name)}")
    item = menu_item(context, action)
    expect(item.first).to_be_visible()
    item.first.click()
    pause(0.3)
    settle(context)


@when('I choose "{action}" in the actions menu of version {version:d}')
def ui_version_action(context, action, version):
    open_menu(context, f"Actions for version {version}")
    item = menu_item(context, action)
    expect(item.first).to_be_visible()
    item.first.click()
    pause(0.3)
    settle(context)


# ---------------------------------------------------------------- texts, messages, buttons

def visible_text(context):
    return page(context).locator("body").inner_text()


@then('I see the text "{text}"')
def ui_see_text(context, text):
    text = sub(context, text)
    expect(page(context).locator("body")).to_contain_text(text, use_inner_text=True)


@then('I do not see the text "{text}"')
def ui_no_text(context, text):
    text = sub(context, text)
    settle(context)
    assert text not in visible_text(context), f"the page shows {text!r}"


@then('I see a status message "{text}"')
def ui_status_message(context, text):
    text = sub(context, text)
    msg = page(context).get_by_role("status").filter(has_text=text)
    expect(msg.first).to_be_visible()


@then('I see an alert containing "{text}"')
def ui_alert(context, text):
    text = sub(context, text)
    alert = page(context).get_by_role("alert").filter(has_text=text)
    expect(alert.first).to_be_visible()


@then("there is exactly one alert")
def ui_one_alert(context):
    alerts = page(context).get_by_role("alert")
    expect(alerts.first).to_be_visible()
    settle(context)

    def check():
        visible = [a for a in alerts.all() if a.is_visible()]
        assert len(visible) == 1, f"{len(visible)} visible alerts: " + " | ".join(
            a.inner_text() for a in visible)
    poll(check, 3000)


@then('I see the button "{label}"')
def ui_see_button(context, label):
    expect(page(context).get_by_role("button", name=label, exact=True)).to_be_visible()


@then('I do not see the button "{label}"')
def ui_no_button(context, label):
    settle(context)
    expect(page(context).get_by_role("button", name=label, exact=True)).to_have_count(0)


@then('the button "{label}" is disabled')
def ui_disabled(context, label):
    expect(page(context).get_by_role("button", name=label, exact=True)).to_be_disabled()


@then('the button "{label}" is enabled')
def ui_enabled(context, label):
    expect(page(context).get_by_role("button", name=label, exact=True)).to_be_enabled()


# ---------------------------------------------------------------- tables and menus

@then('I see a table with the column headers "{headers}"')
def ui_table_headers(context, headers):
    wanted = split_list(context, headers)
    pg = page(context)

    def check():
        found = []
        for table in pg.get_by_role("table").all():
            names = [clean_header(h.inner_text())
                     for h in table.get_by_role("columnheader").all()]
            found.append(names)
            if names == wanted:
                return
        raise AssertionError(f"no table with headers {wanted}; tables: {found}")
    poll(check)


@then('the row of "{name}" shows "{text}"')
def ui_row_shows(context, name, text):
    row = vocab_row(context, sub(context, name))
    expect(row).to_have_count(1)
    expect(row).to_contain_text(sub(context, text), use_inner_text=True)


@then('the row of "{name}" shows text matching "{pattern}"')
def ui_row_matches(context, name, pattern):
    row = vocab_row(context, sub(context, name))
    expect(row).to_have_count(1)
    expect(row).to_contain_text(re.compile(pattern), use_inner_text=True)


@then('the row of version {version:d} shows "{text}"')
def ui_version_row_shows(context, version, text):
    row = version_row(context, version)
    expect(row).to_have_count(1)
    expect(row).to_contain_text(sub(context, text), use_inner_text=True)


@then("the row of version {version:d} is selected")
def ui_version_selected(context, version):
    expect(version_row(context, version)).to_have_attribute("aria-current", "true")


@then('the status icon of "{name}" is named "{text}"')
def ui_icon_name(context, name, text):
    row = vocab_row(context, sub(context, name))
    icon = row.get_by_role("img", name=text, exact=True).or_(row.get_by_label(text, exact=True))
    expect(icon.first).to_be_attached()
    assert text not in row.inner_text(), "D72: the warning text must not be visible row text"


@then('the actions menu of "{name}" offers "{actions}"')
def ui_menu_offers(context, name, actions):
    open_menu(context, f"Actions for {sub(context, name)}")
    for action in split_list(context, actions):
        expect(menu_item(context, action).first).to_be_visible()
    close_menu(context)


@then('the actions menu of "{name}" does not offer "{action}"')
def ui_menu_lacks(context, name, action):
    open_menu(context, f"Actions for {sub(context, name)}")
    pg = page(context)
    # wait until the menu shows something, then check the item is absent
    expect(pg.get_by_role("menuitem").or_(pg.get_by_role("menu")).first).to_be_visible()
    expect(menu_item(context, action)).to_have_count(0)
    close_menu(context)


@then('the vocabulary list shows the names in the order "{names}"')
def ui_list_order(context, names):
    wanted = split_list(context, names)
    table = table_with_header(context, "Latest version")

    def check():
        got = first_cells(table)
        assert got == wanted, f"list shows {got}, expected {wanted}"
    poll(check)


@then("the vocabulary list shows {count:d} rows")
def ui_row_count(context, count):
    table = table_with_header(context, "Latest version")

    def check():
        got = body_rows(table).count()
        assert got == count, f"list shows {got} rows, expected {count}"
    poll(check)


@then('the version list shows the versions in the order "{versions}"')
def ui_version_order(context, versions):
    wanted = split_list(context, versions)
    table = table_with_header(context, "Change log")

    def check():
        got = [re.match(r"\s*(\d+)", t).group(1) if re.match(r"\s*(\d+)", t) else t
               for t in first_cells(table)]
        assert got == wanted, f"versions shown {got}, expected {wanted}"
    poll(check)


# ---------------------------------------------------------------- regions

def region(context, name):
    return page(context).get_by_role("region", name=sub(context, name), exact=True)


@then('the region "{name}" contains "{text}"')
def ui_region_contains(context, name, text):
    expect(region(context, name)).to_contain_text(sub(context, text), use_inner_text=True)


@then('the region "{name}" does not contain "{text}"')
def ui_region_lacks(context, name, text):
    reg = region(context, name)
    expect(reg).to_be_visible()
    settle(context)
    expect(reg).not_to_contain_text(sub(context, text), use_inner_text=True)


@then('the region "{name}" contains text matching "{pattern}"')
def ui_region_matches(context, name, pattern):
    expect(region(context, name)).to_contain_text(re.compile(pattern), use_inner_text=True)


@then('the region "{name}" is not shown')
def ui_region_absent(context, name):
    expect(region(context, name)).to_have_count(0)


# ---------------------------------------------------------------- form fields

def field_error_text(context, target):
    """Text of the error tied to a field: aria-errormessage or aria-describedby."""
    return target.evaluate("""e => {
        const ids = ((e.getAttribute('aria-errormessage') || '') + ' ' +
                     (e.getAttribute('aria-describedby') || '')).split(/\\s+/).filter(Boolean);
        return ids.map(id => { const n = document.getElementById(id); return n ? n.innerText : ''; })
                  .join(' ').trim();
    }""")


@then('the field "{label}" shows an error')
def ui_field_any_error(context, label):
    target = field(context, label)

    def check():
        invalid = target.get_attribute("aria-invalid") == "true"
        text = field_error_text(context, target)
        assert invalid and text, (f"field {label!r}: aria-invalid={target.get_attribute('aria-invalid')}, "
                                  f"error text {text!r}")
    poll(check)


@then('the field "{label}" shows the error "{text}"')
def ui_field_error(context, label, text):
    target = field(context, label)
    text = sub(context, text)

    def check():
        got = field_error_text(context, target)
        assert target.get_attribute("aria-invalid") == "true", f"field {label!r} is not aria-invalid"
        assert text in got, f"field {label!r} error is {got!r}, expected {text!r}"
    poll(check)


@then('the field "{label}" contains "{value}"')
def ui_field_value(context, label, value):
    expect(field(context, label)).to_have_value(sub(context, value))


@then('the field "{label}" is empty')
def ui_field_empty(context, label):
    expect(field(context, label)).to_have_value("")


@then('the form has no field "{label}"')
def ui_no_field(context, label):
    settle(context)
    expect(field(context, label)).to_have_count(0)


@then('the focus is on the field "{label}"')
def ui_focus(context, label):
    expect(field(context, label)).to_be_focused()


@then("no request was sent to the API")
def ui_no_request(context):
    pause(1)
    sent = api_requests_since_mark(context)
    assert not sent, f"requests sent to the API: {sent}"


# ---------------------------------------------------------------- pages and links

def current_path(context):
    return urlparse(page(context).url).path


@then('I am on the page "{route}"')
def ui_on_page(context, route):
    route = sub(context, route)
    poll(lambda: _assert_eq(current_path(context), route), UPLOAD_WAIT_MS)


@then('I am on one of the pages "{route1}" and "{route2}"')
def ui_on_either(context, route1, route2):
    routes = {sub(context, route1), sub(context, route2)}

    def check():
        assert current_path(context) in routes, f"on {current_path(context)}, expected one of {routes}"
    poll(check, UPLOAD_WAIT_MS)


def _assert_eq(got, wanted):
    assert got == wanted, f"got {got!r}, expected {wanted!r}"


@then('the link "{text}" points to "{route}"')
def ui_link_target(context, text, route):
    link = page(context).get_by_role("link", name=sub(context, text), exact=True)
    expect(link.first).to_be_visible()
    href = link.first.get_attribute("href") or ""
    path = urlparse(urljoin(page(context).url, href)).path
    _assert_eq(path, sub(context, route))


# ---------------------------------------------------------------- downloads

@then('a file named "{filename}" is downloaded with the content of version {version:d} of "{name}"')
def ui_download(context, filename, version, name):
    filename, name = sub(context, filename), sub(context, name)

    def check():
        assert context.ui_downloads, "no download started"
    poll(check, UPLOAD_WAIT_MS)
    download = context.ui_downloads[-1]
    _assert_eq(download.suggested_filename, filename)
    with open(download.path(), "rb") as fh:
        got = fh.read()
    expected = context.uploaded.get((name, version))
    if expected is None:
        resp = vmclient.send("GET", f"/content/{name}/versions/{version}",
                             token=vmclient.fetch_token("ga-admin"), base=vmclient.API_URL)
        assert resp.status_code == 200, f"GET content failed: {resp.status_code}"
        expected = resp.content
    assert got == expected, (f"downloaded {len(got)} bytes differ from version {version} "
                             f"({len(expected)} bytes)")


# ---------------------------------------------------------------- accessibility

@then("the page has no axe violations")
def ui_axe(context):
    from axe_playwright_python.sync_playwright import Axe
    settle(context)
    pause(0.5)
    result = Axe().run(page(context), options={
        "runOnly": {"type": "tag", "values": ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]}})
    violations = result.response.get("violations", [])
    os.makedirs(REPORT_DIR, exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9]+", "-", current_path(context)).strip("-") or "root"
    with open(os.path.join(REPORT_DIR, f"{slug.replace(context.run, '')}.json"), "w") as fh:
        json.dump({"url": page(context).url, "standard": "WCAG 2.1 AA",
                   "rules_passed": len(result.response.get("passes", [])),
                   "violations": violations}, fh, indent=2)
    assert result.response.get("passes"), "axe ran no rule; the check did not work"
    summary = [f"{v['id']} ({v['impact']}, {len(v['nodes'])} nodes): {v['help']}"
               for v in violations]
    assert not violations, "axe WCAG 2.1 AA violations:\n" + "\n".join(summary)


FOCUSABLE_JS = """() => {
  const sel = 'a[href],button,input,select,textarea,[tabindex],[role=button],[role=link],' +
              '[role=menuitem],[role=checkbox],[role=combobox],[role=tab]';
  const els = Array.from(document.querySelectorAll(sel)).filter(e => {
    if (e.closest('[inert]') || e.closest('[aria-hidden=true]')) return false;
    if (e.disabled) return false;
    if (e.getAttribute('tabindex') === '-1') return false;
    if (e.type === 'hidden') return false;
    const r = e.getClientRects();
    if (!r.length) return false;
    const s = getComputedStyle(e);
    return s.visibility !== 'hidden' && s.display !== 'none';
  });
  window.__kbd = els;
  return els.map(e => (e.getAttribute('aria-label') || e.innerText || e.value || e.name ||
                       e.id || e.tagName).trim().slice(0, 60));
}"""


@then("every action on the page can be reached and used with the keyboard only")
def ui_keyboard(context):
    pg = page(context)
    settle(context)
    names = pg.evaluate(FOCUSABLE_JS)
    assert names, "no interactive elements on the page"
    pg.evaluate("() => { document.activeElement && document.activeElement.blur(); }")
    pg.locator("body").focus()
    reached = set()
    for _ in range(len(names) * 2 + 20):
        pg.keyboard.press("Tab")
        idx = pg.evaluate("() => window.__kbd.indexOf(document.activeElement)")
        if idx >= 0:
            if idx in reached and len(reached) == len(names):
                break
            reached.add(idx)
        if len(reached) == len(names):
            break
    missing = [names[i] for i in range(len(names)) if i not in reached]
    assert not missing, f"not reachable with Tab: {missing}"
    # 'used': an actions menu opens with Enter and closes with Escape (D75 menu buttons)
    menus = pg.get_by_role("button", name=re.compile(r"^Actions for "))
    if menus.count():
        menus.first.focus()
        pg.keyboard.press("Enter")
        expect(pg.get_by_role("menuitem").or_(pg.get_by_role("menu")).first).to_be_visible()
        pg.keyboard.press("Escape")
