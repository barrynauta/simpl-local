"""UI steps for the @ui scenarios (step 60): Playwright for Python against UI_URL.

Elements are found by role, label and text (the decisions name no test ids).
Data set-up and checks of what is stored use the API steps of vocabulary_steps.py.
The browser itself is started in environment.py (before_all) when UI_STEPS is set;
each @ui scenario gets a fresh browser context, so no state leaks between scenarios.
"""
import json
import os
import re
import sys
import time
from urllib.parse import urlparse, parse_qs

from behave import given, when, then

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from vmtest import client as c  # noqa: E402

UI_URL = os.environ.get("UI_URL", "http://localhost:4433").rstrip("/")
# Path prefix under which the UI calls the API (the UI's nginx proxies it; seen in the rendered app).
UI_API_PREFIX = os.environ.get("UI_API_PREFIX", "/v1")
TIMEOUT_MS = int(float(os.environ.get("UI_TIMEOUT", "10")) * 1000)
WORK = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
STATE_DIR = os.path.join(WORK, ".ui-state")  # stored Keycloak sessions per role (not a report)
AXE_DIR = os.path.join(WORK, "reports", "axe")
WRITE_ACTIONS = ("Update", "Deprecate", "Activate", "New Vocabulary +")
REASONS = {400: "Bad Request", 403: "Forbidden", 404: "Not Found", 409: "Conflict",
           500: "Internal Server Error", 503: "Service Unavailable"}

_CURRENT = {"page": None}

try:
    from playwright.sync_api import expect
    expect.set_options(timeout=TIMEOUT_MS)
except ImportError:  # the API suite runs without Playwright installed
    expect = None


# =========================================================================== browser helpers

def _labels(text):
    """Parse a JSON list of strings from the feature text, placeholders expanded."""
    return json.loads(text)


def _page(context):
    assert getattr(context, "ui_page", None) is not None, "no browser page: log in first"
    return context.ui_page


def _new_browser_context(context, role):
    """Fresh browser context; reuses the stored Keycloak session of the role when there is one."""
    if getattr(context, "ui_context", None) is not None:
        context.ui_context.close()
    state = os.path.join(STATE_DIR, f"{role}.json") if role else None
    kwargs = {"accept_downloads": True, "base_url": UI_URL}
    if state and os.path.exists(state):
        kwargs["storage_state"] = state
    bctx = context.ui_holder["browser"].new_context(**kwargs)
    bctx.set_default_timeout(TIMEOUT_MS)
    page = bctx.new_page()
    _CURRENT["page"] = page
    context.ui_context, context.ui_page, context.ui_role = bctx, page, role
    context.ui_downloads, context.ui_api_calls = [], []
    page.on("download", lambda d: context.ui_downloads.append(d))
    page.on("request", lambda r: context.ui_api_calls.append(r.url)
            if urlparse(r.url).path.startswith(UI_API_PREFIX + "/") else None)
    return page


def _login_if_asked(context):
    """When the app has sent the browser to Keycloak, sign in as the scenario's user."""
    page = _page(context)
    page.locator("#username, header, [role=banner]").first.wait_for(timeout=TIMEOUT_MS * 2)
    if not page.locator("#username").count():
        return
    role = context.ui_role
    assert role, "the UI asked for a login, but the scenario has no user"
    user, password = c.USERS[role]
    page.locator("#username").fill(user)
    page.locator("#password").fill(password)
    page.locator("#kc-login, [type=submit]").first.click()
    page.wait_for_url(UI_URL + "/**", timeout=TIMEOUT_MS * 2)
    page.get_by_role("banner").wait_for()
    os.makedirs(STATE_DIR, exist_ok=True)
    context.ui_context.storage_state(path=os.path.join(STATE_DIR, f"{role}.json"))


def _settle(page):
    try:
        page.wait_for_load_state("networkidle", timeout=TIMEOUT_MS)
    except Exception:  # noqa: BLE001 - a long poll must not fail the step
        pass


def _open(context, path):
    page = _page(context)
    page.goto(UI_URL + c.sub(context, path))
    if context.ui_role:
        _login_if_asked(context)
    _settle(page)
    return page


def _field(page, label):
    """A form control whose label starts with the given text (field errors sit inside the label)."""
    return page.get_by_label(re.compile(r"^\s*" + re.escape(label) + r"(\s|$)"))


def _action(page, label):
    """A visible button, link or menu item with exactly this name."""
    loc = (page.get_by_role("button", name=label, exact=True)
           .or_(page.get_by_role("link", name=label, exact=True))
           .or_(page.get_by_role("menuitem", name=label, exact=True)))
    return loc.filter(visible=True)


def _list_table(page):
    return page.get_by_role("table", name="Vocabulary list")


def _list_names(page):
    table = _list_table(page)
    table.wait_for()
    rows = table.get_by_role("rowgroup").nth(1).get_by_role("row")
    return [rows.nth(i).get_by_role("link").first.inner_text().strip() for i in range(rows.count())]


def _wait_list_call(context, action):
    """Run an action that reloads the vocabulary list and wait for the list call to answer."""
    page = _page(context)

    def is_list(r):
        return urlparse(r.url).path == UI_API_PREFIX + "/vocabularies" and r.request.method == "GET"

    with page.expect_response(is_list, timeout=TIMEOUT_MS):
        action()
    page.wait_for_timeout(200)


def _row(context, name):
    """The list row of a vocabulary. When it is not on the shown page, the name filter finds it
    (the list pages at 20 rows and the shared store holds many vocabularies)."""
    page = _page(context)
    name = c.sub(context, name)
    table = _list_table(page)
    table.wait_for()
    row = table.get_by_role("row").filter(has=page.get_by_role("link", name=name, exact=True))
    try:
        row.first.wait_for(timeout=3000)
    except Exception:  # noqa: BLE001
        box = page.get_by_label("Name contains")
        if box.count() and box.input_value() != name:
            _wait_list_call(context, lambda: box.fill(name))
        row.first.wait_for()
    return row.first


def _column_index(table, header):
    headers = [h.strip() for h in table.get_by_role("columnheader").all_inner_texts()]
    assert header in headers, f"no column {header}: {headers}"
    return headers.index(header)


def _open_menu(context, name):
    row = _row(context, name)
    button = row.get_by_role("button", name="Actions")
    if button.get_attribute("aria-expanded") != "true":
        button.click()
    menu = row.get_by_role("menu")
    menu.wait_for()
    return row, menu


def _version_table(page):
    return page.get_by_role("table", name="Version history")


def _version_row(context, v):
    page = _page(context)
    table = _version_table(page)
    table.wait_for()
    row = table.get_by_role("row").filter(has=page.get_by_role("button", name=str(v), exact=True))
    row.first.wait_for()
    return row.first


def _row_shows(row, text):
    """True when a cell equals the text, or (for longer texts) the row contains it."""
    cells = [t.strip() for t in row.get_by_role("cell").all_inner_texts()]
    if text in cells:
        return True
    return len(text) > 3 and text in row.inner_text()


def _wait_true(fn, message):
    deadline = time.time() + TIMEOUT_MS / 1000
    while True:
        last = fn()
        if last is True:
            return
        if time.time() > deadline:
            raise AssertionError(f"{message} (last seen: {last})")
        # wait through Playwright, so its events (downloads, responses) are processed meanwhile
        if _CURRENT["page"] is not None:
            _CURRENT["page"].wait_for_timeout(250)
        else:
            time.sleep(0.25)


def _choose_file(context, filename, data):
    page = _page(context)
    _field(page, "Vocabulary file").set_input_files(
        files=[{"name": filename, "mimeType": "text/turtle", "buffer": data}])
    page.wait_for_timeout(300)


# =========================================================================== who, where

@given('I am logged in to the UI as a Governance Authority user with role "{role:Q}"')
def step_ui_login_role(context, role):
    _new_browser_context(context, role)
    _open(context, "/")


@given('I am logged in to the UI as a user with no vocabulary management role')
def step_ui_login_norole(context):
    _new_browser_context(context, "NONE")
    _open(context, "/")


@given('I am not logged in to the UI')
def step_ui_logged_out(context):
    _new_browser_context(context, None)


@given('the API answers "{method:Q}" "{path:Q}" with status {code:d} for the UI')
def step_ui_stub(context, method, path, code):
    """Stub one API operation in the browser (decision D24: UI error tests stub the API)."""
    target = UI_API_PREFIX + c.sub(context, path)
    detail = REASONS.get(code, "Error")
    if code == 404:
        detail = f"Vocabulary with name '{c.sub(context, path).split('/')[2]}' was not found."
    body = json.dumps({"type": "about:blank", "title": REASONS.get(code, "Error"),
                       "status": code, "detail": detail})

    def handler(route):
        if route.request.method == method and urlparse(route.request.url).path == target:
            route.fulfill(status=code, content_type="application/problem+json", body=body)
        else:
            route.fallback()

    context.ui_context.route(lambda url: urlparse(url).path == target, handler)


@given('I am on the UI page "{path:Q}"')
@when('I open the UI page "{path:Q}"')
def step_ui_open(context, path):
    _open(context, path)


@when('I open the create form by its URL')
def step_ui_open_create_url(context):
    _open(context, "/vocabularies/new")


@when('I open the new version form of "{name:Q}" by its URL')
def step_ui_open_update_url(context, name):
    _open(context, f"/vocabularies/{c.sub(context, name)}/update")


# =========================================================================== actions

@when('I click "{label:Q}"')
def step_ui_click(context, label):
    page = _page(context)
    _action(page, c.sub(context, label)).first.click()
    _settle(page)


@when('I open the actions menu of "{name:Q}"')
def step_ui_open_menu(context, name):
    _open_menu(context, name)


@when('I click "{label:Q}" in the actions menu of "{name:Q}"')
def step_ui_click_menu(context, label, name):
    _, menu = _open_menu(context, name)
    menu.get_by_role("menuitem", name=c.sub(context, label), exact=True).click()
    page = _page(context)
    _settle(page)
    if c.sub(context, label) == "Update":
        # The update form is pre-filled from the latest version (D33); wait until it is,
        # so a later "fill in" is not overwritten by the late pre-fill.
        expect(page.get_by_text("(current content)").first).to_be_visible()
        _wait_true(lambda: _field(page, "Description").first.input_value() != "" or "empty",
                   "the update form was not pre-filled")


@when('I fill in "{label:Q}" with "{value:Q}"')
def step_ui_fill(context, label, value):
    page = _page(context)
    box = _field(page, label)
    box.fill(c.sub(context, value))
    box.blur()


@when('I choose the fixture "{fixture:Q}" as the vocabulary file')
def step_ui_choose_fixture(context, fixture):
    _choose_file(context, fixture, c.fixture_bytes(context, fixture))


@when('I choose a file of {n:d} bytes as the vocabulary file')
def step_ui_choose_size(context, n):
    _choose_file(context, f"large-{n}.ttl", c.padded_ttl("Padding", n))


@when('I click the column header "{label:Q}"')
def step_ui_sort(context, label):
    page = _page(context)
    header = _list_table(page).get_by_role("columnheader", name=label, exact=True)
    _wait_list_call(context, lambda: header.get_by_role("button").click())


@when('I filter the list by "{field:Q}" with "{value:Q}"')
def step_ui_filter(context, field, value):
    page = _page(context)
    value = c.sub(context, value)
    control = page.get_by_label(field, exact=True)
    if not control.count():
        control = page.get_by_label(field)
    control = control.first
    tag = control.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        _wait_list_call(context, lambda: control.select_option(label=value))
    else:
        _wait_list_call(context, lambda: control.fill(value))


@when('I select version {v:d}')
def step_ui_select_version(context, v):
    _version_row(context, v).get_by_role("button", name=str(v), exact=True).click()
    _page(context).get_by_role("region", name="Version details").wait_for()


@when('I click "{label:Q}" for version {v:d}')
def step_ui_click_version(context, label, v):
    _version_row(context, v).get_by_role("button", name=label, exact=True).click()
    _settle(_page(context))


@when('I submit')
def step_ui_submit(context):
    page = _page(context)
    page.get_by_role("button", name="Submit", exact=True).click()
    _settle(page)


# =========================================================================== what is shown

@then('I see the text "{text:Q}"')
def step_ui_see_text(context, text):
    expect(_page(context).locator("body")).to_contain_text(c.sub(context, text))


@then('I do not see the text "{text:Q}"')
def step_ui_not_see_text(context, text):
    page = _page(context)
    _settle(page)
    expect(page.locator("body")).not_to_contain_text(c.sub(context, text))


@then('I see the action "{label:Q}"')
def step_ui_see_action(context, label):
    expect(_action(_page(context), c.sub(context, label)).first).to_be_visible()


@then('I do not see the action "{label:Q}"')
def step_ui_not_see_action(context, label):
    page = _page(context)
    _settle(page)
    expect(_action(page, c.sub(context, label))).to_have_count(0)


@then('I see the columns {labels}')
def step_ui_columns(context, labels):
    page = _page(context)
    wanted = _labels(labels)
    table = page.get_by_role("table").first
    table.wait_for()
    _wait_true(lambda: [h.strip() for h in table.get_by_role("columnheader").all_inner_texts()] == wanted
               or [h.strip() for h in table.get_by_role("columnheader").all_inner_texts()],
               f"columns are not {wanted}")


@then('I see the form fields {labels}')
def step_ui_form_fields(context, labels):
    page = _page(context)
    for label in _labels(labels):
        expect(_field(page, label).first).to_be_visible()


@then('I do not see the form fields {labels}')
def step_ui_no_form_fields(context, labels):
    page = _page(context)
    page.locator("form").first.wait_for()
    for label in _labels(labels):
        expect(_field(page, label)).to_have_count(0)


@then('the field "{label:Q}" contains "{value:Q}"')
def step_ui_field_contains(context, label, value):
    expect(_field(_page(context), label).first).to_have_value(c.sub(context, value))


@then('the field "{label:Q}" is empty')
def step_ui_field_empty(context, label):
    expect(_field(_page(context), label).first).to_have_value("")


@then('the "{label:Q}" button is disabled')
def step_ui_button_disabled(context, label):
    expect(_page(context).get_by_role("button", name=label, exact=True)).to_be_disabled()


@then('the "{label:Q}" button is enabled')
def step_ui_button_enabled(context, label):
    expect(_page(context).get_by_role("button", name=label, exact=True)).to_be_enabled()


@then('the row of "{name:Q}" shows "{text:Q}"')
def step_ui_row_shows(context, name, text):
    row, text = _row(context, name), c.sub(context, text)
    _wait_true(lambda: _row_shows(row, text) or row.inner_text(), f"row of {name} does not show {text!r}")


@then('the row of "{name:Q}" shows the status "{label:Q}"')
def step_ui_row_status(context, name, label):
    page = _page(context)
    row = _row(context, name)
    idx = _column_index(_list_table(page), "Status")
    cell = row.get_by_role("cell").nth(idx)

    def check():
        text = cell.inner_text().strip()
        return text.startswith(label) or text
    _wait_true(check, f"status of {name} is not {label}")


@then('the row of "{name:Q}" shows a warning icon with the text "{text:Q}"')
def step_ui_row_warning(context, name, text):
    row = _row(context, name)
    expect(row.get_by_role("img", name=c.sub(context, text), exact=True)).to_be_visible()


@then('the row of "{name:Q}" shows no warning')
def step_ui_row_no_warning(context, name):
    row = _row(context, name)
    expect(row.get_by_role("img")).to_have_count(0)
    expect(row).not_to_contain_text("deprecated.")


@then('the actions menu of "{name:Q}" offers {labels}')
def step_ui_menu_offers(context, name, labels):
    """Every listed action is offered and no write action that is not listed.
    Extra read actions (D35 names 'history' as a way to open the details) are allowed."""
    page = _page(context)
    _, menu = _open_menu(context, name)
    offered = [t.strip() for t in menu.get_by_role("menuitem").all_inner_texts()]
    page.keyboard.press("Escape")
    wanted = _labels(labels)
    missing = [w for w in wanted if w not in offered]
    extra_writes = [o for o in offered if o in WRITE_ACTIONS and o not in wanted]
    assert not missing and not extra_writes, \
        f"menu of {name} offers {offered}; missing {missing}; write actions not expected {extra_writes}"


@then('the list shows the vocabularies {names} in this order')
def step_ui_list_order(context, names):
    page = _page(context)
    wanted = _labels(c.sub(context, names))
    _wait_true(lambda: _list_names(page) == wanted or _list_names(page), f"list is not {wanted}")


@then('the list shows only the vocabularies {names}')
def step_ui_list_only(context, names):
    page = _page(context)
    wanted = sorted(_labels(c.sub(context, names)))
    _wait_true(lambda: sorted(_list_names(page)) == wanted or _list_names(page), f"list is not only {wanted}")


@then('I am on the UI page "{path:Q}"')
def step_ui_on_page(context, path):
    target = UI_URL + c.sub(context, path)
    expect(_page(context)).to_have_url(re.compile("^" + re.escape(target) + r"/?([?#].*)?$"))


@then('I am sent to the login page')
def step_ui_login_page(context):
    page = _page(context)
    issuer = urlparse(c.TOKEN_URL)
    expect(page).to_have_url(re.compile("^" + re.escape(f"{issuer.scheme}://{issuer.netloc}") +
                                        r"/realms/[^/]+/(protocol/openid-connect/auth|login-actions/)"))
    expect(page.locator("#username")).to_be_visible()
    expect(page.locator("#password")).to_be_visible()


@then('a file named "{filename:Q}" is downloaded with the content of version {v:d} of "{name:Q}"')
def step_ui_download(context, filename, v, name):
    filename, name = c.sub(context, filename), c.sub(context, name)
    _wait_true(lambda: bool(context.ui_downloads) or "no download", "no file was downloaded")
    download = context.ui_downloads[-1]
    assert download.suggested_filename == filename, \
        f"downloaded {download.suggested_filename}, expected {filename}"
    with open(download.path(), "rb") as fh:
        data = fh.read()
    expected = context.uploaded[(name, v)]
    assert data == expected, f"downloaded bytes ({len(data)}) differ from version {v} ({len(expected)})"


@then('the version list shows the versions {versions} in this order')
def step_ui_versions_order(context, versions):
    page = _page(context)
    wanted = [str(v) for v in json.loads(versions)]
    table = _version_table(page)
    table.wait_for()

    def seen():
        rows = table.get_by_role("rowgroup").nth(1).get_by_role("row")
        return [rows.nth(i).get_by_role("cell").first.inner_text().strip() for i in range(rows.count())]
    _wait_true(lambda: seen() == wanted or seen(), f"versions are not {wanted}")


@then('the row of version {v:d} shows "{text:Q}"')
def step_ui_version_row_shows(context, v, text):
    row, text = _version_row(context, v), c.sub(context, text)
    _wait_true(lambda: _row_shows(row, text) or row.inner_text(), f"row of version {v} does not show {text!r}")


@then('the row of version {v:d} shows no warning')
def step_ui_version_row_no_warning(context, v):
    row = _version_row(context, v)
    expect(row).not_to_contain_text("deprecated.")
    expect(row.get_by_role("link", name=re.compile("Latest ACTIVE version"))).to_have_count(0)


@then('the version panel shows "{text:Q}"')
def step_ui_panel_shows(context, text):
    panel = _page(context).get_by_role("region", name="Version details")
    expect(panel).to_contain_text(c.sub(context, text))


@then('the text "{text:Q}" links to version {v:d} of "{name:Q}"')
def step_ui_links_to_version(context, text, v, name):
    page, name = _page(context), c.sub(context, name)
    link = page.get_by_role("link", name=c.sub(context, text), exact=True).first
    expect(link).to_be_visible()
    link.click()
    expect(page).to_have_url(re.compile("^" + re.escape(f"{UI_URL}/vocabularies/{name}")))
    panel = page.get_by_role("region", name="Version details")
    expect(panel.get_by_role("heading")).to_contain_text(f"Version {v}")


@then('the validation report lists an issue with detail "{detail:Q}"')
def step_ui_report_issue(context, detail):
    report = _page(context).get_by_role("alert", name="Validation report")
    expect(report.get_by_role("listitem").filter(has_text=c.sub(context, detail))).to_have_count(1)


@then('the validation report lists an issue with detail "{detail:Q}" and location "{location:Q}"')
def step_ui_report_issue_location(context, detail, location):
    report = _page(context).get_by_role("alert", name="Validation report")
    item = report.get_by_role("listitem").filter(has_text=c.sub(context, detail))
    expect(item).to_have_count(1)
    expect(item).to_contain_text(c.sub(context, location))


@then('the form is still open')
def step_ui_form_open(context):
    page = _page(context)
    expect(page.locator("form").first).to_be_visible()
    expect(page.get_by_role("button", name="Submit", exact=True)).to_be_visible()


@then('the warnings panel lists "{text:Q}"')
def step_ui_warnings(context, text):
    panel = _page(context).get_by_role("region", name="Validation warnings")
    expect(panel.get_by_role("listitem").filter(has_text=c.sub(context, text))).to_have_count(1)


@then('a success notification says "{text:Q}"')
def step_ui_success(context, text):
    status = _page(context).get_by_role("status").filter(has_text=c.sub(context, text))
    expect(status.first).to_be_visible()


@then('an error message says "{text:Q}"')
def step_ui_error(context, text):
    """An alert or status message with the text (the scenario does not fix the ARIA role)."""
    page = _page(context)
    message = page.get_by_role("alert").or_(page.get_by_role("status")).filter(has_text=c.sub(context, text))
    expect(message.first).to_be_visible()


@then('the vocabulary list loads with page size {n:d}')
def step_ui_page_size(context, n):
    page = _page(context)
    _list_table(page).wait_for()
    calls = [u for u in context.ui_api_calls if urlparse(u).path == UI_API_PREFIX + "/vocabularies"]
    assert calls, "the UI made no list call"
    sizes = {parse_qs(urlparse(u).query).get("pageSize", [None])[0] for u in calls}
    assert sizes == {str(n)}, f"list calls used page sizes {sizes}: {calls}"
    rows = _list_table(page).get_by_role("rowgroup").nth(1).get_by_role("row").count()
    assert rows <= n, f"{rows} rows shown"


# =========================================================================== accessibility (@a11y)

A11Y_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]


@then('the page has no WCAG 2.1 AA violations, reported as "{report:Q}"')
def step_ui_axe(context, report):
    from axe_playwright_python.sync_playwright import Axe
    page = _page(context)
    _settle(page)
    page.wait_for_timeout(300)
    result = Axe().run(page, options={"runOnly": {"type": "tag", "values": A11Y_TAGS}})
    out_dir = AXE_DIR
    os.makedirs(out_dir, exist_ok=True)
    violations = result.response.get("violations", [])
    summary = [{"id": v["id"], "impact": v.get("impact"), "help": v.get("help"),
                "nodes": [n.get("target") for n in v.get("nodes", [])]} for v in violations]
    with open(os.path.join(out_dir, f"{report}.json"), "w", encoding="utf-8") as fh:
        json.dump({"url": page.url.split("#")[0], "tags": A11Y_TAGS, "violations": summary,
                   "passes": len(result.response.get("passes", [])),
                   "incomplete": [v["id"] for v in result.response.get("incomplete", [])]}, fh, indent=2)
    assert not violations, "axe WCAG 2.1 AA violations: " + "; ".join(
        f"{v['id']} ({v.get('impact')}, {len(v.get('nodes', []))} nodes): {v.get('help')}" for v in violations)
