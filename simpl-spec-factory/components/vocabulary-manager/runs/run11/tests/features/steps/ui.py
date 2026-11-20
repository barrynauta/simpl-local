"""Browser steps of the @ui scenarios (step 60), Playwright for Python.

Elements are found only by what the decisions fix: visible texts, labels, column headers,
the routes of D31 and D74, the menu button names of D75, roles status and alert (D75, D90,
D114, D115) and regions named as in D75 and D90. Nothing is taken from a particular build.
Playwright is imported lazily, so the dry run works without it.
"""
import datetime as dt
import json
import os
import re
import time
import uuid
from urllib.parse import urlsplit

import requests
from behave import given, when, then

import vm

# small copies of the API helpers of library.py (behave loads step files as scripts, not as importable modules)


def X(context, text):
    return vm.expand(text, context)


def name_of(context, alias):
    return vm.vocab_name(context.sid, alias)


def next_turtle(context, alias):
    context.revision += 1
    return vm.default_turtle(name_of(context, alias), vm.namespace(context.sid, alias), context.revision)


def admin_create(context, alias):
    name = name_of(context, alias)
    content = next_turtle(context, alias)
    r = vm.create_vocabulary(name, content)
    assert r.status_code == 201, "setup: " + vm.describe(r)
    context.contents[(name, 1)] = content
    return r


def admin_get(context, path):
    r = vm.send("GET", path, "admin")
    assert r.status_code == 200, "check: " + vm.describe(r)
    return r.json()

UI_URL = os.environ.get("UI_URL", "http://localhost:4433").rstrip("/")
KEYCLOAK_ORIGIN = vm.origin(vm.TOKEN_URL)
WAIT = float(os.environ.get("UI_WAIT", "15"))  # seconds for eventual UI state

STATUS_LABEL = {"ACTIVE": "Active", "DEPRECATED": "Deprecated"}  # D6

# ======================================================================= helpers


_live = {"page": None}  # the scenario's page; waiting on it lets Playwright deliver events (downloads)


def pause(seconds):
    pg = _live["page"]
    if pg is not None:
        try:
            pg.wait_for_timeout(seconds * 1000)
            return
        except Exception:  # noqa: BLE001 page closed
            pass
    time.sleep(seconds)


def eventually(fn, timeout=WAIT, interval=0.25):
    """Retry fn until it stops raising AssertionError (the UI renders asynchronously)."""
    end = time.time() + timeout
    while True:
        try:
            return fn()
        except AssertionError:
            if time.time() > end:
                raise
            pause(interval)


def norm(text):
    return re.sub(r"\s+", " ", text or "").strip()


def csv(text):
    return [norm(p) for p in text.split(",") if norm(p)]


def quoted_list(text):
    """'"A", "B" and "C"' -> [A, B, C]"""
    return re.findall(r'"((?:[^"\\]|\\.)*)"', text)


def fmt_utc(iso):
    """D60: dates shown in UTC as 'YYYY-MM-DD HH:mm UTC'."""
    return vm.parse_ts(iso).strftime("%Y-%m-%d %H:%M UTC")


def page(context):
    assert getattr(context, "page", None) is not None, "no browser page (is the scenario tagged @ui?)"
    _live["page"] = context.page
    return context.page


def path_of(url):
    return urlsplit(url).path.rstrip("/") or "/"


def settle(pg):
    try:
        pg.wait_for_load_state("networkidle", timeout=WAIT * 1000)
    except Exception:  # noqa: BLE001 long polling or a slow page; assertions poll anyway
        pass


def on_ui(pg):
    """Wait until an OIDC round trip through Keycloak has come back to the UI."""
    end = time.time() + WAIT
    while not pg.url.startswith(UI_URL) and time.time() < end:
        pg.wait_for_timeout(200)
    settle(pg)


def goto(context, path):
    pg = page(context)
    pg.goto(UI_URL + X(context, path))
    on_ui(pg)


def visible(locator):
    return [locator.nth(i) for i in range(locator.count()) if locator.nth(i).is_visible()]


def clickable(pg, name):
    """A button or link with exactly this name (decisions name actions, not their element type)."""
    for role in ("button", "link"):
        loc = visible(pg.get_by_role(role, name=name, exact=True))
        if loc:
            return loc[0]
    return None


def must_click(pg, name):
    el = eventually(lambda: clickable(pg, name) or (_ for _ in ()).throw(
        AssertionError(f"no button or link named '{name}'")))
    el.click()


def text_of(locator):
    return norm(locator.inner_text())


# ----------------------------------------------------------------------- tables (D23, D35, D100)


def find_table(pg, first_header):
    for t in visible(pg.get_by_role("table")):
        if t.get_by_role("columnheader", name=first_header, exact=True).count():
            return t
    raise AssertionError(f"no table with the column header '{first_header}'")


def headers(table):
    # sort indicators (arrows) are decoration, not part of the decided header text
    return [norm(re.sub(r"[\u2190-\u21ff\u25a0-\u25ff]", " ", h)) for h in table.get_by_role("columnheader").all_inner_texts()]


def data_rows(table):
    rows = table.get_by_role("row")
    out = []
    for i in range(rows.count()):
        r = rows.nth(i)
        if r.get_by_role("columnheader").count():
            continue
        out.append(r)
    return out


def cells(row):
    return [norm(t) for t in row.locator(":scope > *").all_inner_texts()]


def column(table, header):
    hs = headers(table)
    assert header in hs, f"no column '{header}' in {hs}"
    return hs.index(header)


def list_table(context):
    return find_table(page(context), "Name")


def list_names(context):
    t = list_table(context)
    idx = column(t, "Name")
    return [c[idx] for c in (cells(r) for r in data_rows(t)) if len(c) > idx]


def list_row(context, name):
    t = list_table(context)
    idx = column(t, "Name")
    for r in data_rows(t):
        c = cells(r)
        if len(c) > idx and c[idx] == name:
            return t, r, c
    raise AssertionError(f"no list row '{name}'; rows: {list_names(context)}")


def version_table(context):
    return find_table(page(context), "Version")


def version_numbers(context):
    t = version_table(context)
    idx = column(t, "Version")
    out = []
    for r in data_rows(t):
        m = re.match(r"\d+", cells(r)[idx])
        if m:
            out.append(int(m.group()))
    return out


def version_row(context, v):
    t = version_table(context)
    idx = column(t, "Version")
    for r in data_rows(t):
        c = cells(r)
        if re.match(rf"{v}\b", c[idx]):
            return t, r, c
    raise AssertionError(f"no version row {v}; rows: {version_numbers(context)}")


def status_of(cell_text):
    return cell_text.split(" ")[0] if cell_text else ""


# ----------------------------------------------------------------------- menus (D72, D75)


def open_menu(context, menu_name):
    pg = page(context)
    btn = eventually(lambda: (visible(pg.get_by_role("button", name=menu_name, exact=True)) or (_ for _ in ()).throw(
        AssertionError(f"no button '{menu_name}'")))[0])
    before = set(norm(t) for t in pg.get_by_role("button").all_inner_texts() + pg.get_by_role("link").all_inner_texts())
    btn.click()

    def items():
        mi = visible(pg.get_by_role("menuitem"))
        if mi:
            return [(norm(m.inner_text()), m) for m in mi]
        # no menu roles: the controls that appeared when the button was pressed
        found = []
        for role in ("button", "link"):
            for el in visible(pg.get_by_role(role)):
                t = norm(el.inner_text())
                if t and t not in before:
                    found.append((t, el))
        assert found, f"menu '{menu_name}' shows no items"
        return found

    return eventually(items)


def close_menu(context):
    page(context).keyboard.press("Escape")
    page(context).wait_for_timeout(150)


def choose_in_menu(context, menu_name, item):
    items = open_menu(context, menu_name)
    for text, el in items:
        if text == item:
            el.click()
            settle(page(context))
            return
    raise AssertionError(f"menu '{menu_name}' has no item '{item}'; items: {[t for t, _ in items]}")


def menu_texts(context, menu_name):
    texts = [t for t, _ in open_menu(context, menu_name)]
    close_menu(context)
    return texts


# ----------------------------------------------------------------------- forms (D60, D141)


def label_re(label):
    return re.compile(rf"^\s*{re.escape(label)}\s*\*?\s*$")


def field(context, label):
    pg = page(context)
    loc = pg.get_by_label(label_re(label))
    eventually(lambda: loc.count() > 0 or (_ for _ in ()).throw(AssertionError(f"no field '{label}'")))
    assert loc.count() == 1, f"{loc.count()} fields labelled '{label}'"
    return loc.first


def has_field(context, label):
    return page(context).get_by_label(label_re(label)).count() > 0


_FIELD_TEXT_JS = """
(el) => {
  let c = el;
  while (c.parentElement) {
    const p = c.parentElement;
    if (p.querySelectorAll('input,textarea,select').length > 1 || p.tagName === 'FORM') break;
    c = p;
  }
  const labels = Array.from(el.labels || []).map(l => l.innerText);
  return {text: c.innerText, labels: labels, invalid: el.getAttribute('aria-invalid'),
          required: el.getAttribute('aria-required'),
          described: (el.getAttribute('aria-describedby') || '').split(/\\s+/).concat(
                     (el.getAttribute('aria-errormessage') || '').split(/\\s+/))
                     .filter(Boolean).map(id => document.getElementById(id))
                     .filter(Boolean).map(n => n.innerText).join(' ')};
}
"""


def field_info(context, label):
    return field(context, label).evaluate(_FIELD_TEXT_JS)


def error_under(context, label):
    """Text shown under the field besides its label (D37: field errors under the field)."""
    info = field_info(context, label)
    rest = norm(info["text"])
    for lab in info["labels"] or [label]:
        rest = rest.replace(norm(lab), " ", 1)
    rest = norm(rest.replace(label, " ", 1).replace("*", " "))
    rest = norm(re.sub(r"\S+\.ttl \(current content\)", " ", rest))
    rest = norm(rest.replace("Syntax check passed.", " "))
    return norm(rest + " " + (info["described"] or "")) if not rest else rest


def fill(context, label, value):
    f = field(context, label)
    f.fill(X(context, value))


def choose_file(context, label, content, filename="vocabulary.ttl"):
    f = field(context, label)
    f.set_input_files({"name": filename, "mimeType": "text/turtle", "buffer": content})
    context.ui_upload = content
    settle(page(context))


def submit_button(context):
    pg = page(context)
    return eventually(lambda: (visible(pg.get_by_role("button", name="Submit", exact=True)) or (_ for _ in ()).throw(
        AssertionError("no button 'Submit'")))[0])


def first_field_top(context):
    pg = page(context)
    tops = []
    for label in ("Name", "Description", "Change log", "Vocabulary file"):
        loc = pg.get_by_label(label_re(label))
        if loc.count():
            box = loc.first.bounding_box()
            if box:
                tops.append(box["y"])
    assert tops, "no form field found"
    return min(tops)


def alerts(context):
    """Visible elements with role alert that hold text (empty live regions do not count)."""
    return [a for a in visible(page(context).get_by_role("alert")) if norm(a.inner_text())]


def statuses(context):
    return [s for s in visible(page(context).get_by_role("status")) if norm(s.inner_text())]


def region(context, name):
    pg = page(context)
    loc = pg.get_by_role("region", name=X(context, name), exact=True)
    eventually(lambda: loc.first.is_visible() or (_ for _ in ()).throw(AssertionError(f"no region '{X(context, name)}'")))
    return loc.first


# ----------------------------------------------------------------------- Turtle for the forms


def ns(context, alias):
    return vm.namespace(context.sid, alias)


def turtle_with_errors(context, alias, codes):
    n = ns(context, alias)
    lines = ["@prefix owl: <http://www.w3.org/2002/07/owl#> .", "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .",
             f"@prefix ex: <{n}> .", "ex:Concept0 a skos:Concept ."]
    if "S1" in codes:
        lines += ["ex:hasPart a owl:ObjectProperty .", 'ex:Thing1 ex:hasPart "literal value" .']
    if "S3" in codes:
        lines += ["ex:mixed a owl:ObjectProperty , owl:DatatypeProperty ."]
    if "S5" in codes:
        lines += ["ex:Cat a owl:Class ; owl:disjointWith ex:Dog .", "ex:Felix a ex:Cat , ex:Dog ."]
    return ("\n".join(lines) + "\n").encode()


SEMANTIC_LOCATIONS = {  # the subject each error is reported on (D9, OpenAPI example)
    "Literal value for object property": "Thing1",
    "Property declared as both object and datatype property": "mixed",
    "Resource is an instance of disjoint classes": "Felix",
}


# ======================================================================= session and navigation


@given("I am logged in to the UI as {who}")
def step_ui_login(context, who):
    identities = {"a vocabulary viewer": "viewer", "a vocabulary administrator": "admin",
                  "a user without a vocabulary role": "norole"}
    assert who in identities, f"unknown identity '{who}'"
    key = identities[who]
    pg = page(context)
    pg.goto(UI_URL + "/")
    end = time.time() + WAIT
    while not pg.url.startswith(KEYCLOAK_ORIGIN) and time.time() < end:
        pg.wait_for_timeout(200)
    assert pg.url.startswith(KEYCLOAK_ORIGIN), f"the UI did not send the browser to Keycloak: {pg.url}"
    # Keycloak's own login form (E1), not part of the build
    pg.get_by_label("Username").first.fill(vm.USERS[key])
    pg.get_by_label("Password", exact=True).fill(vm.USER_PASSWORD)
    pg.get_by_role("button", name=re.compile(r"^(Sign In|Log In)$", re.I)).click()
    on_ui(pg)
    assert pg.url.startswith(UI_URL), f"login for {vm.USERS[key]} did not return to the UI: {pg.url}"


@given("I am not logged in to the UI")
def step_not_logged_in(context):
    pass  # every scenario starts with a fresh browser context


@when("I open the vocabulary list")
@given("I am on the vocabulary list")
def step_open_list(context):
    goto(context, "/vocabularies")


@given("I am on the vocabulary list filtered on the scenario id")
def step_list_filtered(context):
    goto(context, "/vocabularies")
    field(context, "Name contains").fill(context.sid)
    settle(page(context))
    eventually(lambda: list_names(context) and all(context.sid in n for n in list_names(context)) or (_ for _ in ()).throw(
        AssertionError(f"list not filtered on {context.sid}")))


@when('I open the menu item "{item:Q}"')
def step_menu_item(context, item):
    pg = page(context)
    eventually(lambda: pg.get_by_role("link", name=item, exact=True).first.is_visible() or (_ for _ in ()).throw(
        AssertionError(f"no menu item '{item}'")))
    pg.get_by_role("link", name=item, exact=True).first.click()
    settle(pg)


@when('I open the page "{path:Q}"')
@given('I am on the page "{path:Q}"')
def step_open_page(context, path):
    goto(context, path)


@when('I open the details page of "{name:Q}"')
def step_open_details(context, name):
    goto(context, f"/vocabularies/{X(context, name)}")


@given('I am on the details page of "{name:Q}"')
def step_on_details(context, name):
    goto(context, f"/vocabularies/{X(context, name)}")
    eventually(lambda: version_numbers(context) or (_ for _ in ()).throw(AssertionError("no version rows")))


@then('I am on the page "{path:Q}"')
def step_on_page(context, path):
    want = X(context, path).rstrip("/")
    eventually(lambda: path_of(page(context).url) == want or (_ for _ in ()).throw(
        AssertionError(f"on {page(context).url}, expected the path {want}")))
    settle(page(context))


@then('I am on the Keycloak login page of the realm "{realm:Q}"')
def step_on_keycloak(context, realm):
    pg = page(context)
    prefix = f"{KEYCLOAK_ORIGIN}/realms/{realm}/"
    eventually(lambda: pg.url.startswith(prefix) or (_ for _ in ()).throw(AssertionError(f"on {pg.url}, not {prefix}")))
    eventually(lambda: pg.get_by_label("Password", exact=True).is_visible() or (_ for _ in ()).throw(
        AssertionError("no password field on the Keycloak page")))


# ======================================================================= API stubs (D24, E3)


@given('the API answers {method:w} {path} with {status:d} and the problem detail "{detail:Q}"')
def step_stub_detail(context, method, path, status, detail):
    context.stubs.append((method, X(context, path), status, X(context, detail)))


@given('the API answers {method:w} {path} with {status:d}')
def step_stub(context, method, path, status):
    context.stubs.append((method, X(context, path), status, None))


@then("no request was sent to POST /vocabularies")
def step_no_post(context):
    page(context).wait_for_timeout(500)
    sent = [r for r in context.ui_requests if r[0] == "POST" and r[1].endswith("/vocabularies")
            and r[2] in ("fetch", "xhr")]
    assert not sent, f"requests sent: {sent}"


# ======================================================================= generic texts and controls


@then('I see the text "{text:Q}"')
def step_see_text(context, text):
    t = X(context, text)
    pg = page(context)
    eventually(lambda: visible(pg.get_by_text(t, exact=False)) or (_ for _ in ()).throw(
        AssertionError(f"text '{t}' not visible; page: {norm(pg.locator('body').inner_text())[:600]}")))


@then('I do not see the text "{text:Q}"')
def step_not_see_text(context, text):
    page(context).wait_for_timeout(500)
    assert not visible(page(context).get_by_text(X(context, text))), f"text '{text}' is visible"


@then('I see a link "{text:Q}" that opens "{path:Q}"')
def step_link_opens(context, text, path):
    pg = page(context)
    eventually(lambda: visible(pg.get_by_role("link", name=text, exact=True)) or (_ for _ in ()).throw(
        AssertionError(f"no link '{text}'")))
    visible(pg.get_by_role("link", name=text, exact=True))[0].click()
    step_on_page(context, path)


@when('I click the button "{name:Q}"')
def step_click_button(context, name):
    n = X(context, name)
    pg = page(context)
    if n.startswith("Actions for"):
        context.open_menu = open_menu(context, n)
        return
    must_click(pg, n)
    settle(pg)


@then('I see the button "{name:Q}"')
def step_see_button(context, name):
    pg = page(context)
    eventually(lambda: clickable(pg, X(context, name)) or (_ for _ in ()).throw(AssertionError(f"no '{name}'")))


@then('I do not see the button "{name:Q}"')
def step_not_see_button(context, name):
    pg = page(context)
    eventually(lambda: list_names(context) or (_ for _ in ()).throw(AssertionError("list not loaded")))
    assert clickable(pg, X(context, name)) is None, f"'{name}' is shown"


@then('I see the buttons "{a:Q}" and "{b:Q}"')
def step_see_buttons(context, a, b):
    step_see_button(context, a)
    step_see_button(context, b)


@then('the button "{name:Q}" is enabled')
def step_enabled(context, name):
    eventually(lambda: submit_button(context).is_enabled() or (_ for _ in ()).throw(AssertionError(f"'{name}' is disabled")))


@then('the button "{name:Q}" is disabled')
def step_disabled(context, name):
    eventually(lambda: (not submit_button(context).is_enabled()) or (_ for _ in ()).throw(
        AssertionError(f"'{name}' is enabled")))


@then('I see one alert with the text "{a:Q}" followed by "{b:Q}" and "{c:Q}"')
def step_one_alert3(context, a, b, c):
    _one_alert(context, [a, b, c])


@then('I see one alert with the text "{a:Q}" followed by "{b:Q}"')
def step_one_alert2(context, a, b):
    _one_alert(context, [a, b])


@then('I see one alert with the text "{a:Q}"')
def step_one_alert1(context, a):
    _one_alert(context, [a])


def _one_alert(context, parts):
    parts = [X(context, p) for p in parts]

    def check():
        al = alerts(context)
        assert len(al) == 1, f"{len(al)} alerts: {[norm(a.inner_text()) for a in al]}"
        text = norm(al[0].inner_text())
        pos = 0
        for p in parts:
            i = text.find(p, pos)
            assert i >= 0, f"alert '{text}' lacks '{p}' (in order {parts})"
            pos = i + len(p)
    eventually(check)


@then('I see an alert with the text "{a:Q}" and "{b:Q}"')
def step_alert_and(context, a, b):
    a, b = X(context, a), X(context, b)

    def check():
        texts = [norm(x.inner_text()) for x in alerts(context)]
        assert any(a in t and b in t for t in texts), f"no alert with '{a}' and '{b}': {texts}"
    eventually(check)


@then('I see a message with role status "{text:Q}"')
@then('I see an element with role status and the text "{text:Q}"')
def step_status_message(context, text):
    t = X(context, text)
    eventually(lambda: any(t in norm(s.inner_text()) for s in statuses(context)) or (_ for _ in ()).throw(
        AssertionError(f"no status '{t}': {[norm(s.inner_text()) for s in statuses(context)]}")))


@then("I see one element with role alert above the form")
def step_alert_above_form(context):
    def check():
        al = alerts(context)
        assert len(al) == 1, f"{len(al)} alerts: {[norm(a.inner_text()) for a in al]}"
        assert al[0].bounding_box()["y"] < first_field_top(context), "the alert is not above the form"
    eventually(check)


@then("the form stays open")
def step_form_open(context):
    page(context).wait_for_timeout(500)
    assert re.search(r"/vocabularies/(new|[^/]+/versions/new)$", path_of(page(context).url)), page(context).url
    assert submit_button(context).is_visible()


# ======================================================================= list (D34, D72, D100, D115)


@then('I see a table with the column headers "{text:Q}"')
def step_table_headers(context, text):
    want = csv(text)
    eventually(lambda: headers(find_table(page(context), want[0])) == want or (_ for _ in ()).throw(
        AssertionError(f"headers {headers(find_table(page(context), want[0]))}, expected {want}")))


@when('I type "{text:Q}" into the filter "{label:Q}"')
def step_type_filter(context, text, label):
    field(context, label).fill(X(context, text))
    settle(page(context))


@when('I type the scenario id into the filter "{label:Q}"')
def step_type_sid(context, label):
    step_type_filter(context, context.sid, label)


@when('I choose "{option:Q}" in the filter "{label:Q}"')
def step_choose_filter(context, option, label):
    f = field(context, label)
    if f.evaluate("e => e.tagName") == "SELECT":
        f.select_option(label=option)
    else:
        f.click()
        page(context).get_by_role("option", name=option, exact=True).click()
    settle(page(context))


@when('I choose the UTC date of the creation date of "{name:Q}" in the filter "{label:Q}"')
def step_choose_creation_date(context, name, label):
    v = admin_get(context, f"/vocabularies/{X(context, name)}/versions/1")
    step_choose_date(context, v["creationDate"][:10], label)


@when('I choose the date "{date:Q}" in the filter "{label:Q}"')
def step_choose_date(context, date, label):
    field(context, label).fill(date)
    settle(page(context))


@then('the list shows only "{name:Q}"')
def step_list_only(context, name):
    n = X(context, name)
    eventually(lambda: list_names(context) == [n] or (_ for _ in ()).throw(
        AssertionError(f"list shows {list_names(context)}, expected only {n}")))


@then('the list shows "{text:Q}" in this order')
def step_list_order(context, text):
    want = csv(X(context, text))
    eventually(lambda: list_names(context) == want or (_ for _ in ()).throw(
        AssertionError(f"list shows {list_names(context)}, expected {want}")))


@when('I click the column header "{header:Q}"')
def step_click_header(context, header):
    t = list_table(context)
    h = t.get_by_role("columnheader", name=header, exact=True).first
    inner = h.get_by_role("button")
    (inner.first if inner.count() else h).click()
    settle(page(context))


def _sortable(table, header):
    h = table.get_by_role("columnheader", name=header, exact=True).first
    return h.get_attribute("aria-sort") is not None or h.get_by_role("button").count() > 0


@then('the column headers "{text:Q}" are sortable')
def step_sortable(context, text):
    t = eventually(lambda: list_table(context))
    bad = [h for h in csv(text) if not _sortable(t, h)]
    assert not bad, f"not sortable: {bad}"


@then('the column headers "{text:Q}" are not sortable')
def step_not_sortable(context, text):
    t = eventually(lambda: list_table(context))
    bad = [h for h in csv(text) if _sortable(t, h)]
    assert not bad, f"sortable: {bad}"


@then('the row of "{name:Q}" shows the status "{status:Q}", the description "{desc:Q}" and the creator "{creator:Q}"')
def step_row_status_desc_creator(context, name, status, desc, creator):
    def check():
        t, r, c = list_row(context, X(context, name))
        assert status_of(c[column(t, "Status")]) == status, c
        assert c[column(t, "Description")] == X(context, desc), c
        assert c[column(t, "Creator")] == X(context, creator), c
    eventually(check)


@then('the row of "{name:Q}" shows the date created and last updated as "YYYY-MM-DD HH:mm UTC"')
def step_row_dates(context, name):
    n = X(context, name)
    v1 = admin_get(context, f"/vocabularies/{n}/versions/1")
    voc = admin_get(context, f"/vocabularies/{n}")

    def check():
        t, r, c = list_row(context, n)
        assert c[column(t, "Date created")] == fmt_utc(v1["creationDate"]), (c, v1["creationDate"])
        assert c[column(t, "Last updated")] == fmt_utc(voc["lastUpdateDate"]), (c, voc["lastUpdateDate"])
    eventually(check)


@then('the row of "{name:Q}" has a button "{button:Q}"')
def step_row_button(context, name, button):
    def check():
        t, r, c = list_row(context, X(context, name))
        assert r.get_by_role("button", name=X(context, button), exact=True).count() == 1, c
    eventually(check)


@then('the row of "{name:Q}" shows the status "{status:Q}"')
def step_row_status(context, name, status):
    def check():
        t, r, c = list_row(context, X(context, name))
        assert status_of(c[column(t, "Status")]) == status, c
    eventually(check)


_ICON_JS = """
(row, text) => Array.from(row.querySelectorAll('*')).filter(e =>
   e.getAttribute('aria-label') === text || e.getAttribute('title') === text ||
   (e.tagName.toLowerCase() === 'title' && e.textContent.trim() === text)).map(e => ({
     tag: e.tagName, label: e.getAttribute('aria-label'), title: e.getAttribute('title') ||
       (e.tagName.toLowerCase() === 'title' ? e.textContent.trim() : null)}))
"""


@then('the row of "{name:Q}" has a warning icon with the accessible name and tooltip "{text:Q}"')
def step_row_icon(context, name, text):
    def check():
        t, r, c = list_row(context, X(context, name))
        named = r.get_by_label(text, exact=True)
        assert named.count() >= 1, f"no element named '{text}' in the row"
        found = r.evaluate(_ICON_JS, text)
        has_tooltip = any(f["title"] == text for f in found)
        if not has_tooltip:
            named.first.hover()
            page(context).wait_for_timeout(400)
            has_tooltip = any(text in norm(x.inner_text()) for x in visible(page(context).get_by_role("tooltip")))
        assert has_tooltip, f"no tooltip '{text}' ({found})"
    eventually(check)


@then('I see {n:d} rows and the text "{text:Q}"')
@then('I see {n:d} row and the text "{text:Q}"')
def step_rows_and_text(context, n, text):
    eventually(lambda: len(list_names(context)) == n or (_ for _ in ()).throw(
        AssertionError(f"{len(list_names(context))} rows, expected {n}")))
    step_see_text(context, text)


@given("{n:d} vocabularies exist with the scenario id in their name")
def step_n_vocabs(context, n):
    for i in range(1, n + 1):
        admin_create(context, f"N{i:02d}")


# ----------------------------------------------------------------------- menus


@when('I choose "{item:Q}" in the menu "{menu:Q}"')
def step_choose_menu(context, item, menu):
    choose_in_menu(context, X(context, menu), item)


@then('I see the menu items "{text:Q}"')
def step_menu_items(context, text):
    got = [t for t, _ in context.open_menu]
    assert got == csv(text), f"menu items {got}, expected {csv(text)}"


@then('the menu "{menu:Q}" offers "{a:Q}" and not "{b:Q}"')
def step_menu_offers_not(context, menu, a, b):
    def check():
        got = menu_texts(context, X(context, menu))
        assert a in got and b not in got, f"menu items {got}"
    eventually(check)


@then('the menu "{menu:Q}" offers "{a:Q}" and "{b:Q}"')
def step_menu_offers(context, menu, a, b):
    got = menu_texts(context, X(context, menu))
    assert a in got and b in got, f"menu items {got}"


@then('the menu "{menu:Q}" offers {items} only')
def step_menu_only(context, menu, items):
    got = menu_texts(context, X(context, menu))
    assert got == quoted_list(items), f"menu items {got}, expected {quoted_list(items)}"


# ----------------------------------------------------------------------- downloads (D29)


@then('a file "{filename:Q}" is downloaded with the content of version {v:d} of "{alias:Q}"')
def step_downloaded(context, filename, v, alias):
    eventually(lambda: context.downloads or (_ for _ in ()).throw(AssertionError("no download started")))
    d = context.downloads[-1]
    assert d.suggested_filename == X(context, filename), f"file '{d.suggested_filename}'"
    with open(d.path(), "rb") as fh:
        got = fh.read()
    want = context.contents[(name_of(context, alias), v)]
    assert got == want, f"downloaded {len(got)} bytes differ from version {v} ({len(want)} bytes)"


# ======================================================================= details page (D35, D36, D64, D118)


_HEADER_JS = """
(table) => {
  const root = table.closest('main') || document.body;
  const r = document.createRange();
  r.setStart(root, 0);
  r.setEndBefore(table);
  return r.toString();
}
"""


def header_text(context):
    t = version_table(context)
    return norm(t.evaluate(_HEADER_JS))


@then('I see "{name:Q}" in the header')
def step_name_in_header(context, name):
    eventually(lambda: X(context, name) in header_text(context) or (_ for _ in ()).throw(
        AssertionError(f"header: {header_text(context)}")))


@then('I see the vocabulary name, its status "{status:Q}" and the description of version {v:d} in the header')
def step_header_full(context, status, v):
    name = name_of(context, "Alpha")
    desc = admin_get(context, f"/vocabularies/{name}/versions/{v}")["description"]

    def check():
        h = header_text(context)
        for part in (name, status, desc):
            assert part in h, f"'{part}' not in the header: {h}"
    eventually(check)


@then('the version rows are "{text:Q}" in this order')
@then('I see the version rows "{text:Q}"')
def step_version_rows(context, text):
    want = [int(x) for x in csv(text)]
    eventually(lambda: version_numbers(context) == want or (_ for _ in ()).throw(
        AssertionError(f"version rows {version_numbers(context)}, expected {want}")))


@then("I see {n:d} version rows")
def step_n_version_rows(context, n):
    eventually(lambda: len(version_numbers(context)) == n or (_ for _ in ()).throw(
        AssertionError(f"{len(version_numbers(context))} version rows, expected {n}")), timeout=WAIT * 2)


def _version_status(context, v, status):
    def check():
        t, r, c = version_row(context, v)
        assert status_of(c[column(t, "Status")]) == status, c
    eventually(check)


@then('the row of version {a:d} shows the status "{sa:Q}" and the row of version {b:d} the status "{sb:Q}"')
def step_two_version_statuses(context, a, sa, b, sb):
    _version_status(context, a, sa)
    _version_status(context, b, sb)


@then('the rows of versions {a:d} and {b:d} show the status "{status:Q}"')
def step_versions_status(context, a, b, status):
    _version_status(context, a, status)
    _version_status(context, b, status)


@then('the row of version {v:d} shows the status "{status:Q}"')
def step_version_status(context, v, status):
    _version_status(context, v, status)


@then('the row of version {v:d} shows the change log "{text:Q}"')
def step_version_changelog(context, v, text):
    def check():
        t, r, c = version_row(context, v)
        assert c[column(t, "Change log")] == text, c
    eventually(check)


@then('the row of version {v:d} shows the status "{status:Q}" and the change log "{text:Q}"')
def step_version_status_changelog(context, v, status, text):
    _version_status(context, v, status)
    step_version_changelog(context, v, text)


@then('each version row has a button "Actions for version <n>" with the action "{action:Q}"')
def step_each_version_action(context, action):
    for v in version_numbers(context):
        items = [t for t, _ in open_menu(context, f"Actions for version {v}")]
        close_menu(context)
        assert action in items, f"version {v} menu: {items}"


@when("I select version {v:d}")
def step_select_version(context, v):
    t, r, c = eventually(lambda: version_row(context, v))
    for role in ("link", "button"):
        loc = r.get_by_role(role, name=str(v), exact=True)
        if loc.count():
            loc.first.click()
            break
    else:
        r.locator(":scope > *").first.click()
    settle(page(context))


@then('I see a region named "{name:Q}"')
def step_see_region(context, name):
    region(context, name)


@then('the region shows the version "{v:Q}", the status "{status:Q}", the date created as "YYYY-MM-DD HH:mm UTC", '
      'the creator "{creator:Q}", the description and the change log "{changelog:Q}"')
def step_region_metadata(context, v, status, creator, changelog):
    name = name_of(context, "Alpha")
    meta = admin_get(context, f"/vocabularies/{name}/versions/{v}")

    def check():
        text = norm(region(context, f"Version {v}").inner_text())
        for part in (v, status, fmt_utc(meta["creationDate"]), X(context, creator), meta["description"], changelog):
            assert part in text, f"'{part}' not in the panel: {text}"
    eventually(check)


@then('the region has a button "{name:Q}"')
def step_region_has_button(context, name):
    reg = region(context, "Version 1")
    assert reg.get_by_role("button", name=name, exact=True).count() + reg.get_by_role(
        "link", name=name, exact=True).count() >= 1, f"no '{name}' in the panel"


@when('I click the button "{button:Q}" in the region "{name:Q}"')
def step_click_in_region(context, button, name):
    reg = region(context, name)
    for role in ("button", "link"):
        loc = reg.get_by_role(role, name=button, exact=True)
        if loc.count():
            loc.first.click()
            return
    raise AssertionError(f"no '{button}' in region '{name}'")


@then('the region "{name:Q}" shows the warning "{text:Q}"')
def step_region_warning(context, name, text):
    eventually(lambda: text in norm(region(context, name).inner_text()) or (_ for _ in ()).throw(
        AssertionError(f"no '{text}' in region '{name}'")))


@then('the region "{name:Q}" has one link with the text "{text:Q}"')
def step_region_one_link(context, name, text):
    def check():
        n = region(context, name).get_by_role("link", name=text, exact=True).count()
        assert n == 1, f"{n} links '{text}' in region '{name}'"
    eventually(check)


@then('the region "{name:Q}" has no link with a text starting "{text:Q}"')
def step_region_no_link(context, name, text):
    reg = region(context, name)
    page(context).wait_for_timeout(300)
    links = [norm(x) for x in reg.get_by_role("link").all_inner_texts()]
    assert not [x for x in links if x.startswith(text)], f"links: {links}"
    assert text not in norm(reg.inner_text()), f"text '{text}' shown in region '{name}'"


@when('I click the link "{text:Q}"')
def step_click_link(context, text):
    pg = page(context)
    regs = [r for r in visible(pg.get_by_role("region")) if r.get_by_role("link", name=text, exact=True).count()]
    loc = regs[0].get_by_role("link", name=text, exact=True) if regs else pg.get_by_role("link", name=text, exact=True)
    loc.first.click()
    settle(pg)


@then('the region "{name:Q}" shows no deprecation warning')
def step_region_no_warning(context, name):
    reg = region(context, name)
    page(context).wait_for_timeout(300)
    assert "This version is deprecated." not in norm(reg.inner_text())


@then('the row of version {v:d} shows "{text:Q}" and the link "{link:Q}"')
def step_version_row_warning(context, v, text, link):
    def check():
        t, r, c = version_row(context, v)
        assert text in " ".join(c), c
        assert r.get_by_role("link", name=link, exact=True).count() == 1, f"no link '{link}' in row {v}"
    eventually(check)


@then("the row of version {v:d} shows no deprecation warning")
def step_version_row_no_warning(context, v):
    t, r, c = eventually(lambda: version_row(context, v))
    assert "This version is deprecated." not in " ".join(c), c


# ======================================================================= forms (D13, D14, D33, D37, D46, D62, D76, D101, D141)


@then('I see the fields "{a:Q}", "{b:Q}" and "{c:Q}", each marked required with a visible "*" and aria-required="true"')
def step_fields_required(context, a, b, c):
    for label in (a, b, c):
        info = field_info(context, label)
        assert info["required"] == "true", f"'{label}' aria-required={info['required']}"
        labels = " ".join(info["labels"])
        assert re.search(rf"{re.escape(label)}\s*\*", labels), f"no visible '*' after '{label}': {labels!r}"


def _field_labels(context):
    return page(context).evaluate("""() => Array.from(document.querySelectorAll('input,textarea,select'))
        .filter(e => e.type !== 'hidden')
        .map(e => [Array.from(e.labels || []).map(l => l.innerText).join(' '), e.getAttribute('aria-label') || ''].join(' '))""")


@then('I see no field "{label:Q}" and no field for a version')
def step_no_field_and_version(context, label):
    submit_button(context)
    assert not has_field(context, label), f"field '{label}' present"
    step_no_version_field(context)


@then("I see no field for a version")
def step_no_version_field(context):
    labels = [norm(x) for x in _field_labels(context)]
    assert not [x for x in labels if re.search(r"\bversion\b", x, re.I)], f"version field among {labels}"


@then('I see an error under "{a:Q}", under "{b:Q}" and under "{c:Q}"')
def step_errors_under3(context, a, b, c):
    for label in (a, b, c):
        step_error_under(context, label)


@then('I see an error under "{label:Q}"')
def step_error_under(context, label):
    eventually(lambda: error_under(context, label) or (_ for _ in ()).throw(AssertionError(f"no error under '{label}'")))


@then('I see the error "{text:Q}" under "{label:Q}"')
def step_error_text_under(context, text, label):
    eventually(lambda: text in norm(field_info(context, label)["text"] + " " + field_info(context, label)["described"])
               or (_ for _ in ()).throw(AssertionError(f"under '{label}': {field_info(context, label)}")))


@then('the focus is on the field "{label:Q}"')
def step_focus_on(context, label):
    eventually(lambda: field(context, label).evaluate("e => e === document.activeElement") or (_ for _ in ()).throw(
        AssertionError(f"focus is on {page(context).evaluate('() => document.activeElement.outerHTML.slice(0, 200)')}")))


@when('I type "{text:Q}" into the field "{label:Q}" and leave the field')
def step_type_leave(context, text, label):
    f = field(context, label)
    f.fill(X(context, text))
    f.press("Tab")


@when('I fill in "{n:Q}" with "{nv:Q}", "{d:Q}" with "{dv:Q}" and choose a valid Turtle file for "{f:Q}"')
def step_fill_create_valid(context, n, nv, d, dv, f):
    fill(context, n, nv)
    fill(context, d, dv)
    choose_file(context, f, next_turtle(context, "Alpha"), "alpha.ttl")


@when('I fill in "{n:Q}" with "{nv:Q}", "{d:Q}" with "{dv:Q}" and choose a Turtle file with the errors {codes} for "{f:Q}"')
@when('I fill in "{n:Q}" with "{nv:Q}", "{d:Q}" with "{dv:Q}" and choose a Turtle file with the error {codes} for "{f:Q}"')
def step_fill_create_errors(context, n, nv, d, dv, codes, f):
    fill(context, n, nv)
    fill(context, d, dv)
    choose_file(context, f, turtle_with_errors(context, "Alpha", re.findall(r"S\d", codes)), "alpha.ttl")


@when('I choose a valid Turtle file for "{f:Q}"')
def step_choose_valid(context, f):
    choose_file(context, f, next_turtle(context, "Alpha"), "alpha.ttl")


@when('I choose a Turtle file of {n:d} bytes for "{f:Q}"')
def step_choose_size(context, n, f):
    choose_file(context, f, vm.turtle_of_size(ns(context, "Alpha"), n), "big.ttl")


@when('I choose the fixture "{fixture:Q}" for "{f:Q}"')
def step_choose_fixture(context, fixture, f):
    here = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    with open(os.path.join(here, "fixtures", fixture), encoding="utf-8") as fh:
        content = X(context, fh.read()).encode()
    choose_file(context, f, content, fixture)


@then("I see a syntax error that names line {n:d}")
def step_syntax_line(context, n):
    def check():
        text = norm(field_info(context, "Vocabulary file")["text"] + " " + field_info(context, "Vocabulary file")["described"])
        assert re.search(rf"\bline\s*:?\s*{n}\b", text, re.I), f"under 'Vocabulary file': {text}"
    eventually(check)


@then('I see a region named "{name:Q}" above the form')
def step_region_above_form(context, name):
    reg = region(context, name)
    assert reg.bounding_box()["y"] < first_field_top(context), f"region '{name}' is not above the form"
    context.last_region = name


def _first_line(reg):
    lines = [norm(x) for x in reg.inner_text().splitlines() if norm(x)]
    return lines[0] if lines else ""


@then('its first line has role alert and the text "{text:Q}"')
def step_first_line_alert(context, text):
    reg = region(context, context.last_region)
    al = [a for a in visible(reg.get_by_role("alert")) if norm(a.inner_text())]
    assert al and norm(al[0].inner_text()) == text, f"alerts in the region: {[norm(a.inner_text()) for a in al]}"
    assert _first_line(reg) == text, f"first line '{_first_line(reg)}'"


@then('the report lists the details {details}, each with its location')
def step_report_details(context, details):
    reg = region(context, context.last_region)
    items = [norm(x) for x in reg.get_by_role("listitem").all_inner_texts()] or [norm(reg.inner_text())]
    n = ns(context, "Alpha")
    for d in quoted_list(details):
        hits = [i for i in items if d in i]
        assert hits, f"'{d}' not in the report: {items}"
        loc = n + SEMANTIC_LOCATIONS[d]
        assert any(loc in h for h in hits), f"location '{loc}' not shown with '{d}': {hits}"


@then('the first line of the region "{name:Q}" is "{text:Q}"')
def step_region_first_line(context, name, text):
    eventually(lambda: _first_line(region(context, name)) == text or (_ for _ in ()).throw(
        AssertionError(f"first line '{_first_line(region(context, name))}'")))


@given('I submitted the form with a Turtle file with the error S3 and see the region "{name:Q}"')
def step_submitted_s3(context, name):
    fill(context, "Name", "${Alpha}")
    fill(context, "Description", "Fix and resubmit")
    choose_file(context, "Vocabulary file", turtle_with_errors(context, "Alpha", ["S3"]), "alpha.ttl")
    submit_button(context).click()
    region(context, name)


@then('the field "{label:Q}" holds "{value:Q}"')
def step_field_holds(context, label, value):
    eventually(lambda: field(context, label).input_value() == X(context, value) or (_ for _ in ()).throw(
        AssertionError(f"'{label}' holds '{field(context, label).input_value()}'")))


@then('the field "{label:Q}" shows "{text:Q}"')
def step_field_shows(context, label, text):
    eventually(lambda: X(context, text) in norm(field_info(context, label)["text"]) or (_ for _ in ()).throw(
        AssertionError(f"'{label}' shows: {norm(field_info(context, label)['text'])}")))


@then('the field "{label:Q}" is empty and required')
def step_field_empty_required(context, label):
    f = field(context, label)
    assert f.input_value() == "", f"'{label}' holds '{f.input_value()}'"
    info = field_info(context, label)
    assert info["required"] == "true", f"'{label}' aria-required={info['required']}"


@when('I fill in "{label:Q}" with "{value:Q}" and choose a valid changed Turtle file for "{f:Q}"')
def step_fill_changed(context, label, value, f):
    fill(context, label, value)
    choose_file(context, f, next_turtle(context, "Alpha"), "alpha.ttl")


@when('I replace the field "{label:Q}" with "{value:Q}" and fill in "{label2:Q}" with "{value2:Q}"')
def step_replace_and_fill(context, label, value, label2, value2):
    fill(context, label, value)
    fill(context, label2, value2)


@when('I fill in "{label:Q}" with "{value:Q}"')
def step_fill_one(context, label, value):
    fill(context, label, value)


@when('I fill in "{label:Q}" with "{value:Q}" and choose a Turtle file that references version {v:d} of "{ref:Q}" for "{f:Q}"')
def step_fill_reference(context, label, value, v, ref, f):
    fill(context, label, value)
    content = X(context, (
        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
        "@prefix ex: <${NS:Alpha}> .\n"
        f"@prefix ref: <${{CB}}/content/${{{ref}}}/versions/{v}#> .\n"
        "ex:Concept1 a skos:Concept ; skos:related ref:Concept1 .\n")).encode()
    choose_file(context, f, content, "alpha.ttl")


@then('I see a region named "{name:Q}" whose first line has role status and the text "{text:Q}"')
def step_warnings_region(context, name, text):
    def check():
        reg = region(context, name)
        st = [s for s in visible(reg.get_by_role("status")) if norm(s.inner_text())]
        assert st and norm(st[0].inner_text()) == text, f"status lines: {[norm(s.inner_text()) for s in st]}"
        assert _first_line(reg) == text, f"first line '{_first_line(reg)}'"
    eventually(check)
    context.last_region = name


@then('the region lists "{text:Q}" with its location')
def step_region_lists(context, text):
    reg = region(context, context.last_region)
    t = X(context, text)
    loc = X(context, "${CB}/content/${Ref}/versions/1#")
    items = [norm(x) for x in reg.get_by_role("listitem").all_inner_texts()] or [norm(reg.inner_text())]
    hits = [i for i in items if t in i]
    assert hits, f"'{t}' not listed: {items}"
    assert any(loc in h for h in hits), f"location '{loc}' not shown: {hits}"


# ======================================================================= accessibility (Q6, NFR-10) and keyboard

A11Y_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]


@then("axe reports no WCAG 2.1 AA violations")
def step_axe(context):
    from axe_playwright_python.sync_playwright import Axe
    pg = page(context)
    settle(pg)
    pg.wait_for_timeout(500)
    result = Axe().run(pg, options={"runOnly": {"type": "tag", "values": A11Y_TAGS}})
    violations = result.response.get("violations", [])
    os.makedirs("reports/a11y", exist_ok=True)
    slug = re.sub(r"[^A-Za-z0-9]+", "-", path_of(pg.url).replace(context.sid, "SID")).strip("-") or "root"
    with open(f"reports/a11y/{slug}.json", "w") as fh:
        json.dump({"url": pg.url, "scenario": context.scenario.name, "violations": [
            {"id": v["id"], "impact": v.get("impact"), "help": v.get("help"),
             "nodes": [n.get("target") for n in v.get("nodes", [])]} for v in violations]}, fh, indent=1)
    assert not violations, "axe violations: " + "; ".join(
        f"{v['id']} ({v.get('impact')}, {len(v.get('nodes', []))} nodes): {v.get('help')}" for v in violations)


_FOCUS_TARGETS_JS = """
() => {
  const root = document.querySelector('main') || document.body;
  const els = Array.from(root.querySelectorAll('input,textarea,select,button,a[href]'))
    .filter(e => !e.disabled && e.type !== 'hidden' && e.getClientRects().length > 0
                 && getComputedStyle(e).visibility !== 'hidden');
  els.forEach((e, i) => e.setAttribute('data-kb-probe', String(i)));
  return els.map(e => (e.labels && e.labels[0] ? e.labels[0].innerText : '') || e.getAttribute('aria-label') || e.innerText || e.tagName);
}
"""


@then("every field and button can be reached with the keyboard")
def step_keyboard_all(context):
    pg = page(context)
    names = pg.evaluate(_FOCUS_TARGETS_JS)
    seen = set()
    pg.evaluate("() => document.activeElement && document.activeElement.blur()")
    for _ in range(len(names) * 2 + 30):
        pg.keyboard.press("Tab")
        idx = pg.evaluate("() => document.activeElement && document.activeElement.getAttribute('data-kb-probe')")
        if idx is not None:
            seen.add(int(idx))
        if len(seen) == len(names):
            break
    missing = [norm(names[i]) for i in range(len(names)) if i not in seen]
    assert not missing, f"not reachable with Tab: {missing}"


def _focused_name(pg):
    return norm(pg.evaluate(
        "() => { const e = document.activeElement; return e ? (e.getAttribute('aria-label') || e.innerText || '') : ''; }"))


def _tab_to(pg, predicate, limit=120, key="Tab"):
    for _ in range(limit):
        pg.keyboard.press(key)
        if predicate():
            return True
    return False


@then('I can open "{menu:Q}" and choose "{item:Q}" with the keyboard only')
def step_keyboard_menu(context, menu, item):
    pg = page(context)
    m = X(context, menu)
    pg.evaluate("() => document.activeElement && document.activeElement.blur()")
    assert _tab_to(pg, lambda: _focused_name(pg) == m), f"'{m}' not reachable with Tab"
    pg.keyboard.press("Enter")
    pg.wait_for_timeout(300)
    if _focused_name(pg) != item:
        if not _tab_to(pg, lambda: _focused_name(pg) == item, limit=10, key="ArrowDown"):
            assert _tab_to(pg, lambda: _focused_name(pg) == item, limit=10), f"'{item}' not reachable by keyboard"
    pg.keyboard.press("Enter")
    name = m.replace("Actions for ", "")
    step_on_page(context, f"/vocabularies/{name}")


@then('I can select version {v:d} and reach "{button:Q}" with the keyboard only')
def step_keyboard_version(context, v, button):
    pg = page(context)
    t, r, c = eventually(lambda: version_row(context, v))
    r.evaluate("row => row.setAttribute('data-kb-row', '1')")
    in_row = ("() => { const e = document.activeElement; const row = document.querySelector('[data-kb-row]');"
              " return !!(e && row && (row === e || row.contains(e)) && !(e.getAttribute('aria-label') || '').startsWith('Actions for')); }")
    pg.evaluate("() => document.activeElement && document.activeElement.blur()")
    assert _tab_to(pg, lambda: pg.evaluate(in_row)), f"no control of version row {v} reachable with Tab"
    pg.keyboard.press("Enter")
    reg = region(context, f"Version {v}")
    reg.evaluate("r => r.setAttribute('data-kb-region', '1')")
    in_reg = ("(b) => { const e = document.activeElement; const r = document.querySelector('[data-kb-region]');"
              " return !!(e && r && r.contains(e) && ((e.getAttribute('aria-label') || e.innerText || '').trim() === b)); }")
    if not pg.evaluate(in_reg, button):
        assert _tab_to(pg, lambda: pg.evaluate(in_reg, button)), f"'{button}' in the panel not reachable with Tab"


# ======================================================================= headers (D123)


@when("I load the UI start page")
def step_load_start(context):
    context.ui_resp = requests.get(UI_URL + "/", timeout=30)


@then("the response header \"Content-Security-Policy\" has default-src 'self', frame-ancestors 'none' and no 'unsafe-inline'")
def step_csp(context):
    csp = context.ui_resp.headers.get("Content-Security-Policy")
    assert csp, "no Content-Security-Policy header"
    directives = {}
    for part in csp.split(";"):
        bits = part.strip().split()
        if bits:
            directives[bits[0].lower()] = bits[1:]
    assert "'self'" in directives.get("default-src", []), csp
    assert directives.get("frame-ancestors") == ["'none'"], csp
    assert "'unsafe-inline'" not in csp, csp
