"""Black-box browser steps. They know only the user stories, spec-digest.md and the OpenAPI.

Locators use what a user sees: accessible roles, labels and visible text. No CSS classes, ids or
test ids of one build. Two kinds of wording:
  - a label the story quotes ("New Vocabulary +", "View vocabulary details", "Update") must be
    contained in the accessible name, case-insensitive and whitespace tolerant (see quoted());
  - where the story does not fix the wording, a tolerant regex, with a comment naming the story.
Setup goes through the API steps (api_steps.py), the browser only does what the scenario is about.
"""
import json
import os
import re
import tempfile
from urllib.parse import urlparse

import requests
from behave import given, then, when

import api_steps as api  # shared helpers: name(), sub(), fixture(), headers(), url(); behave skips re-registration

PROBLEM = {"type": "about:blank", "title": "Internal Server Error", "status": 500,
           "detail": "Injected failure from the UI test suite (fault injection)"}
SHORT_MS = 5000
USERS = {"admin": "ga-admin", "viewer": "ga-viewer", "norole": "ga-norole"}  # spec-digest D6


# ---------- helpers: matching and waiting ----------

def quoted(label):
    """A label quoted by a story: contained in the accessible name, any case, any whitespace.

    A trailing "+" is the usual "add" convention and may be an icon, so it is not required (Barry, 27 Oct)."""
    label = re.sub(r"\s*\+\s*$", "", label)
    return re.compile(r"\s*".join(re.escape(w) for w in label.split()), re.I)


def matches(pattern, value):
    """Search with a story label (compiled by quoted()) or a plain regex string, ignoring case."""
    return (pattern.search(value) if hasattr(pattern, "search") else re.search(pattern, value, re.I)) is not None


def page(context):
    return context.page


def settle(context, extra_ms=400):
    try:
        page(context).wait_for_load_state("networkidle", timeout=SHORT_MS)
    except Exception:
        pass
    page(context).wait_for_timeout(extra_ms)


def text(context):
    """Visible text of the page, without the contents of select boxes and text areas
    (an option list or a pre-filled Turtle text must not satisfy a message check)."""
    t = page(context).evaluate("""() => {
        const s = document.createElement('style');
        s.textContent = 'select, textarea { display: none !important; }';
        document.head.appendChild(s);
        const t = document.body.innerText;
        s.remove();
        return t;
    }""")
    context.seen.append(t)
    return t


def watch(context, seconds=3.0):
    """Record the page text for a while: confirmations and toasts may disappear again."""
    for _ in range(int(seconds / 0.3)):
        text(context)
        page(context).wait_for_timeout(300)
    text(context)


def mark(context):
    """Remember what is on screen before a user action: a message must be new, not a static hint
    such as "Must be unique." or a column header such as "Last updated"."""
    context.baseline = {l.strip() for l in text(context).splitlines() if l.strip()}


def windows(snapshot, size=3, skip=frozenset()):
    lines = [l.strip() for l in snapshot.splitlines() if l.strip() and l.strip() not in skip]
    for i in range(len(lines)):
        yield " ".join(lines[i:i + size])


def seen_window(context, *patterns, size=3, seconds=4.0):
    """A passage of a few lines, seen at any time in this scenario and new since the last mark(),
    that matches all patterns; None when there is none."""
    deadline = int(seconds / 0.3)
    for i in range(deadline + 1):
        snapshots = context.seen + [text(context)]
        for snap in snapshots:
            for ignored in context.ignore_text:  # e.g. "syntax_errors.ttl" is not an error message
                snap = snap.replace(ignored, "")
            for w in windows(snap, size, context.baseline):
                if all(re.search(p, w, re.I) for p in patterns):
                    if os.environ.get("UI_DEBUG"):
                        print(f"    matched: {w[:200]}")
                    return w
        page(context).wait_for_timeout(300)
    return None


def visible(locator):
    return [l for l in locator.all() if l.is_visible()]


def scope(context):
    """Where a form lives: the last open dialog that holds input fields, else the main region."""
    p = page(context)
    for d in reversed(visible(p.get_by_role("dialog"))):
        if d.get_by_role("textbox").count() or d.locator("input[type=file]").count():
            return d
    main = p.get_by_role("main")
    return main.first if main.count() else p.locator("body")


def tmp_fixture(context, filename, vocab_name, **extra):
    d = tempfile.mkdtemp(prefix="ui-upload-")
    path = os.path.join(d, filename)
    with open(path, "w", encoding="utf-8") as f:
        f.write(api.fixture(context, filename, vocab_name, **extra))
    return path


def api_get(context, path, accept="application/json"):
    return requests.get(api.url(context, path), headers=api.headers(context, "admin", accept=accept), timeout=30)


# ---------- helpers: sign-in and navigation ----------

def password_field(context):
    return page(context).locator("input[type=password]")


def sign_in(context, persona):
    user = USERS[persona]
    p = page(context)
    if context.cfg["ui_auth"] == "none":
        # Environment, not behaviour: a UI without sign-in always acts as one fixed user.
        assert persona == "admin", (f"UI_AUTH=none: this UI has no sign-in, so the tests cannot act as "
                                    f"'{persona}' ({user}). Environment limitation, not a UI verdict.")
        return
    p.goto(context.cfg["ui"])
    settle(context)
    if not password_field(context).first.is_visible():
        # Some UIs show their own landing page with a sign-in button before redirecting.
        btn = visible(p.get_by_role("button", name=re.compile(r"sign\s*in|log\s*in|login", re.I))
                      ) + visible(p.get_by_role("link", name=re.compile(r"sign\s*in|log\s*in|login", re.I)))
        if btn:
            btn[0].click()
            settle(context)
    pw = password_field(context).first
    pw.wait_for(state="visible")
    # Generic login page: the first text field is the user name.
    p.get_by_role("textbox", name=re.compile(r"user|e-?mail|login", re.I)).first.fill(user)
    pw.fill("password")
    p.get_by_role("button", name=re.compile(r"sign\s*in|log\s*in|login|submit", re.I)).first.click()
    p.wait_for_url(re.compile("^" + re.escape(context.cfg["ui"])), timeout=15000)
    settle(context)
    assert not password_field(context).first.is_visible(), f"sign-in as {user} failed: still on the login page"


def open_list(context):
    p = page(context)
    mark(context)
    p.goto(context.cfg["ui"])
    # Wait for the list, an error, or a login page; the Then steps judge which one it is.
    try:
        p.get_by_role("table").or_(p.get_by_role("alert")).or_(password_field(context)).first.wait_for(
            state="visible")
    except Exception:
        pass
    settle(context)


def list_table(context):
    # SIMPL-18161: the list has a Creator column; the version history (SIMPL-18167) has a Change log column.
    p = page(context)
    return p.get_by_role("table").filter(has=p.get_by_role("columnheader", name=re.compile(r"creat", re.I))).filter(
        has_not=p.get_by_role("columnheader", name=re.compile(r"change\s*-?\s*log", re.I))).first


def history_table(context):
    p = page(context)
    return p.get_by_role("table").filter(
        has=p.get_by_role("columnheader", name=re.compile(r"change\s*-?\s*log", re.I))).first


def headers_of(table):
    return [h.inner_text().strip() for h in table.get_by_role("columnheader").all()]


def col(headers, pattern, what):
    for i, h in enumerate(headers):
        if re.search(pattern, h, re.I):
            return i
    raise AssertionError(f"no column for {what} (/{pattern}/) in headers {headers}")


def data_rows(table):
    # Rows with one cell per column; a full-width row (a note or warning under a row) is not a data row.
    width = table.get_by_role("columnheader").count()
    return [r for r in table.get_by_role("row").all() if r.get_by_role("cell").count() >= max(width, 1)]


def cell(row, i):
    return row.get_by_role("cell").nth(i)


def cell_text(row, i):
    return " ".join(cell(row, i).inner_text().split())


def row_for(context, n):
    t = list_table(context)
    return t.get_by_role("row").filter(has=page(context).get_by_role("cell", name=n, exact=True))


def name_filter(context):
    # SIMPL-18163: "Name contains ..."; the wording of the field is not fixed.
    return page(context).get_by_role("textbox", name=re.compile(r"name", re.I)).first


def apply_filters(context):
    # Some UIs filter while typing, others have an apply button; the story does not say which.
    btn = visible(page(context).get_by_role("button", name=re.compile(r"^\s*(apply|search|filter)", re.I)))
    if btn and btn[0].is_enabled():
        btn[0].click()
    settle(context, 800)


def filter_name(context, value):
    name_filter(context).fill(value)
    apply_filters(context)


def find_row(context, alias, narrow=True):
    """The list row of a vocabulary. The shared list grows with every run, so when the row is not on
    the first page the step narrows the list by name, as a user would."""
    n = api.name(context, alias)
    row = row_for(context, n)
    if not row.count() and narrow:
        filter_name(context, n)
        row = row_for(context, n)
    try:
        row.first.wait_for(state="visible", timeout=SHORT_MS)
    except Exception:
        shown = [cell_text(r, 1) if r.get_by_role("cell").count() > 1 else r.inner_text()
                 for r in data_rows(list_table(context))[:10]] if list_table(context).count() else []
        raise AssertionError(f"no row for {n} in the vocabulary list; rows shown: {shown}")
    return row.first


def menu_scope(context):
    p = page(context)
    menus = visible(p.get_by_role("menu"))
    if menus:
        return menus[-1]
    dialogs = visible(p.get_by_role("dialog"))
    return dialogs[-1] if dialogs else p


def open_row_menu(context, row):
    # SIMPL-18161: "an expandable three-dot menu named actions": the control in the Actions column.
    table = row.locator("xpath=ancestor::table[1]")
    i = col(headers_of(table), r"action", "actions")
    btn = cell(row, i).get_by_role("button")
    assert btn.count(), f"no button in the Actions cell of row: {row.inner_text()[:100]}"
    btn.first.click()
    context.menu_toggle = btn.first
    page(context).wait_for_timeout(400)


def menu_items(context):
    s = menu_scope(context)
    items = visible(s.get_by_role("menuitem")) + visible(s.get_by_role("button")) + visible(s.get_by_role("link"))
    return items


def menu_item(context, pattern):
    for it in menu_items(context):
        if matches(pattern, it.get_attribute("aria-label") or it.inner_text() or ""):
            return it
    return None


def menu_names(context):
    return [" ".join((it.get_attribute("aria-label") or it.inner_text() or "").split()) for it in menu_items(context)]


def close_menu(context):
    # Click the toggle again when it says it is expanded; Escape could also close an enclosing dialog.
    t = getattr(context, "menu_toggle", None)
    if t is not None and t.is_visible() and t.get_attribute("aria-expanded") == "true":
        t.click()
    elif menu_items(context) and visible(page(context).get_by_role("menu")):
        page(context).keyboard.press("Escape")
    context.menu_toggle = None
    page(context).wait_for_timeout(200)


def row_action(context, row, pattern):
    """An action of a row: a visible control in the row, else an entry of the row's actions menu."""
    for loc in visible(row.get_by_role("button")) + visible(row.get_by_role("link")):
        if matches(pattern, loc.get_attribute("aria-label") or loc.inner_text() or ""):
            return loc
    open_row_menu(context, row)
    item = menu_item(context, pattern)
    assert item is not None, f"no action /{pattern.pattern if hasattr(pattern, 'pattern') else pattern}/ " \
                             f"for the row; the menu offers {menu_names(context)}"
    return item


def open_history(context, alias):
    # SIMPL-18167: "open the history of a selected vocabulary": the story names no control, so take the
    # row action for history or details (SIMPL-18161 "View vocabulary details"), else the name link.
    open_list(context)
    row = find_row(context, alias)
    open_row_menu(context, row)
    item = menu_item(context, r"history") or menu_item(context, r"details|view")
    if item is None:
        close_menu(context)
        row.get_by_role("link", name=api.name(context, alias), exact=True).click()
    else:
        item.click()
    settle(context)
    history_table(context).wait_for(state="visible")


def version_row(context, version):
    t = history_table(context)
    i = col(headers_of(t), r"^\s*version|version\s*(number|no)", "version")
    for r in data_rows(t):
        if cell_text(r, i) == str(version):
            return r
    raise AssertionError(f"no row for version {version}; versions shown: {[cell_text(r, i) for r in data_rows(t)]}")


def status_ok(cell_txt, status):
    t = cell_txt.lower()
    if status.lower() == "active":
        return re.search(r"\bactive\b", t) is not None and "deprecat" not in t
    return status.lower() in t


WARNING = r"warning|do not use|don't use|should not be used|not recommended|discouraged|avoid|caution|⚠"


def deprecation_warning(context, row, version=None):
    """A clear deprecation warning (SIMPL-18178, SIMPL-18559): warning wording in the row itself, or an
    alert on the page that names this version. A plain status value "Deprecated" is not a warning."""
    if re.search(WARNING, row.inner_text(), re.I) and re.search(r"deprecat", row.inner_text(), re.I):
        return row
    for img in visible(row.get_by_role("img")):
        if re.search(r"warn", img.get_attribute("aria-label") or img.get_attribute("alt") or "", re.I):
            return row
    if version is not None:
        p = page(context)
        for a in visible(p.get_by_role("alert")) + visible(p.get_by_role("note")) + visible(p.get_by_role("status")):
            t = a.inner_text()
            if re.search(r"deprecat", t, re.I) and re.search(rf"version\s*{version}\b|\bv{version}\b", t, re.I):
                return a
    return None


def set_file(context, path):
    # SIMPL-18169 "File upload": the browser's file input, whatever it looks like.
    f = scope(context).locator("input[type=file]")
    assert f.count(), "no file upload field in the form"
    context.ignore_text.append(os.path.basename(path))
    mark(context)
    f.first.set_input_files(path)


def field(context, pattern):
    return scope(context).get_by_role("textbox", name=re.compile(pattern, re.I))


def submit_button(context):
    # The stories do not name the submit button: create, submit or save.
    btns = visible(scope(context).get_by_role("button", name=re.compile(r"^\s*(create|submit|save)", re.I)))
    assert btns, "no submit button in the form"
    return btns[-1]


def submit(context):
    mark(context)
    b = submit_button(context)
    context.submit_blocked = not b.is_enabled()
    if not context.submit_blocked:
        b.click()
    watch(context, 3)


def open_create_form(context):
    open_list(context)
    # The quoted label is checked in its own scenario; here any "New vocabulary" button will do.
    page(context).get_by_role("button", name=re.compile(r"new\s*vocabulary", re.I)).first.click()
    settle(context)


def fill_create_form(context, alias, filename, description=True):
    n = api.name(context, alias)
    open_create_form(context)
    field(context, r"^\s*name").first.fill(n)
    if description:
        field(context, r"^\s*description").first.fill(f"Test vocabulary {n}")
    set_file(context, tmp_fixture(context, filename, n))
    watch(context, 2)


def open_update_form(context, alias):
    open_list(context)
    row = find_row(context, alias)
    open_row_menu(context, row)
    item = menu_item(context, quoted("Update"))
    assert item is not None, f'no "Update" action; the menu offers {menu_names(context)}'
    item.click()
    settle(context)
    context.ui_alias = alias


def fill_update_form(context, alias, filename, **extra):
    n = api.name(context, alias)
    desc = field(context, r"^\s*description")
    if desc.count() and not desc.first.input_value().strip():
        desc.first.fill(f"Test vocabulary {n}")  # pre-fill is checked in its own step
    # SIMPL-18172 "change summary"; the OpenAPI calls it changelog.
    field(context, r"change|changelog").first.fill("Changed in the UI test")
    set_file(context, tmp_fixture(context, filename, n, **extra))
    watch(context, 1)


def fail_api(context, methods, path_pattern):
    """Fault injection: answer matching API calls with a 500 Problem Details response."""
    def handler(route):
        r = route.request
        if (r.method in methods and r.resource_type in ("fetch", "xhr")
                and re.search(path_pattern, urlparse(r.url).path)):
            route.fulfill(status=500, content_type="application/problem+json", body=json.dumps(PROBLEM))
        else:
            route.fallback()
    page(context).route("**/*", handler)


# ---------- given ----------

@given('I am signed in to the UI as "{persona:Q}"')
def step_ui_sign_in(context, persona):
    sign_in(context, persona)


@given("I am not signed in to the UI")
def step_ui_anonymous(context):
    pass  # every UI scenario starts with a fresh browser context


@given("the API fails when the vocabulary list is requested")
def step_fail_list(context):
    fail_api(context, ("GET", "POST"), r"/vocabularies(/searches)?/?$")


@given("the API fails when a new version is created")
def step_fail_new_version(context):
    fail_api(context, ("POST",), r"/vocabularies/[^/]+/versions/?$")


@given("the API fails when a status is changed")
def step_fail_status(context):
    fail_api(context, ("PATCH", "PUT", "POST"), r"/vocabularies/[^/]+(/versions/\d+)?(/status)?/?$")


# ---------- when ----------

@when("I open the Vocabulary Management page")
def step_open_list(context):
    open_list(context)


@when('I open the Vocabulary Management page filtered on name "{value:Q}"')
def step_open_list_filtered(context, value):
    open_list(context)
    filter_name(context, api.sub(context, value))


@when('I filter on name "{value:Q}"')
def step_filter_name(context, value):
    filter_name(context, api.sub(context, value))


@when('I also filter on status "{status:Q}"')
def step_filter_status(context, status):
    # SIMPL-18163: "Vocabulary Status = Deprecated"; the list's own status filter, not a form field.
    box = page(context).get_by_role("combobox", name=re.compile(r"status", re.I)).first
    select_matching(box, quoted(status))
    apply_filters(context)


@when("I clear the name filter")
def step_clear_name(context):
    filter_name(context, "")


@when('I click the "{label:Q}" column header')
@when('I click the "{label:Q}" column header again')
def step_click_header(context, label):
    t = list_table(context)
    t.get_by_role("columnheader", name=re.compile(r"^\s*" + re.escape(label) + r"\b", re.I)).first.click()
    settle(context, 800)


@when('I open the history of "{alias:Q}"')
def step_open_history(context, alias):
    open_history(context, alias)


@when("I open version {version:d}")
def step_open_version(context, version):
    # SIMPL-18176 AC3 "select or open that version": a view or open action on the version row.
    row = version_row(context, version)
    before = (page(context).url, len(visible(page(context).get_by_role("dialog"))))
    action = row_action(context, row, r"^\s*(view|open|details|show)")
    action.click()
    settle(context)
    after = (page(context).url, len(visible(page(context).get_by_role("dialog"))))
    assert after != before, "opening the version changed nothing on screen"


@when("I download version {version:d}")
def step_download_version(context, version):
    row = version_row(context, version)
    action = row_action(context, row, r"download")
    with page(context).expect_download() as dl:
        action.click()
    context.download = dl.value


@when('I choose the download action for "{alias:Q}"')
def step_download_latest(context, alias):
    row = find_row(context, alias)
    action = row_action(context, row, r"download")
    with page(context).expect_download() as dl:
        action.click()
    context.download = dl.value


@when('I click "{label:Q}"')
def step_click_label(context, label):
    p = page(context)
    btn = p.get_by_role("button", name=quoted(label)).or_(p.get_by_role("link", name=quoted(label)))
    assert btn.count(), f'no button or link "{label}"; buttons: ' \
                        f'{[b.inner_text().strip() for b in visible(p.get_by_role("button"))][:15]}'
    btn.first.click()
    settle(context)


@when('I create vocabulary "{alias:Q}" in the UI from "{filename:Q}"')
def step_ui_create(context, alias, filename):
    fill_create_form(context, alias, filename)
    submit(context)


@when('I submit the creation form with name "{alias:Q}" and no description')
def step_ui_create_no_desc(context, alias):
    fill_create_form(context, alias, "valid_v1.ttl", description=False)
    field(context, r"^\s*description").first.focus()
    page(context).keyboard.press("Tab")  # leave the empty field, for UIs that validate on blur
    submit(context)


@when("I add a description and submit again")
def step_ui_add_desc(context):
    field(context, r"^\s*description").first.fill("A description")
    submit(context)


@when('I upload "{filename:Q}" in the creation form for "{alias:Q}"')
def step_ui_upload_only(context, filename, alias):
    fill_create_form(context, alias, filename)
    watch(context, 2)


@when('I open the actions menu of "{alias:Q}" and click "{label:Q}"')
def step_menu_click(context, alias, label):
    open_list(context)
    row = find_row(context, alias)
    open_row_menu(context, row)
    item = menu_item(context, quoted(label))
    assert item is not None, f'no "{label}" in the actions menu; it offers {menu_names(context)}'
    item.click()
    settle(context)
    context.ui_alias = alias


@when('I enter a change summary, upload "{filename:Q}" and submit')
def step_update_submit(context, filename):
    fill_update_form(context, context.ui_alias, filename)
    submit(context)


@when('I submit a new version of "{alias:Q}" from "{filename:Q}"')
def step_new_version_ui(context, alias, filename):
    open_update_form(context, alias)
    fill_update_form(context, alias, filename)
    submit(context)


@when('I start a new version of "{alias:Q}" that references version {version:d} of "{ref:Q}"')
def step_reference(context, alias, version, ref):
    # SIMPL-18184 AC4 "select or reference a vocabulary version": a picker if the form has one,
    # else the D18 mechanism (namespace IRI in the uploaded Turtle) and a submit.
    open_update_form(context, alias)
    ref_name = api.name(context, ref)
    picker = [c for c in visible(scope(context).get_by_role("combobox"))
              if ref_name in " ".join(option_texts(c))]
    if picker:
        mark(context)
        select_matching(picker[0], re.compile(r"^\s*" + re.escape(ref_name) + r"\b"))
        settle(context)
        versions = [c for c in visible(scope(context).get_by_role("combobox", name=re.compile(r"version", re.I)))]
        assert versions, "the reference picker has no version selection"
        select_matching(versions[-1], re.compile(rf"^\s*(v|version\s*)?{version}\b", re.I))
        watch(context, 2)
    else:
        ns = f"{context.cfg['base_iri']}/{ref_name}/{version}#"
        fill_update_form(context, alias, "references.ttl", REF_NS=ns, REF_TERM="Dataset")
        submit(context)


def option_texts(box):
    return box.evaluate("e => e.options ? Array.from(e.options).map(o => o.text) : []") or []


def select_matching(box, pattern):
    """Choose the option whose text matches, in a native select or an ARIA combobox."""
    opts = option_texts(box)
    if opts:
        match = [o for o in opts if pattern.search(o)]
        assert match, f"no option /{pattern.pattern}/ in {opts}"
        box.select_option(label=match[0])
        return
    box.click()
    box.page.get_by_role("option", name=pattern).first.click()


@when('I change the status of "{alias:Q}" to "{status:Q}"')
def step_change_status(context, alias, status):
    # SIMPL-18174: "select the status-change action ... choose ACTIVE or DEPRECATED". The story names
    # neither the action nor the dialog, so: a "status" action with a choice, or a direct
    # deprecate / (re)activate action, followed by a confirmation if one is asked.
    open_list(context)
    row = find_row(context, alias)
    mark(context)
    open_row_menu(context, row)
    item = menu_item(context, r"status")
    if item is not None:
        item.click()
        settle(context)
        boxes = visible(page(context).get_by_role("combobox", name=re.compile(r"status", re.I)))
        radios = visible(page(context).get_by_role("radio", name=quoted(status)))
        if radios:
            radios[0].check()
        else:
            assert boxes, "the status-change action shows no choice of status"
            select_matching(boxes[-1], quoted(status))
        confirm = visible(page(context).get_by_role(
            "button", name=re.compile(r"^\s*(apply|save|confirm|update|ok|change)", re.I)))
        assert confirm, "no button to apply the status change"
        confirm[-1].click()
    else:
        verb = r"deprecat" if status.lower().startswith("deprecat") else r"activat|restor|reinstat"
        item = menu_item(context, verb)
        assert item is not None, f"no status-change action for {status}; the menu offers {menu_names(context)}"
        item.click()
        settle(context)
        confirm = visible(page(context).get_by_role(
            "button", name=re.compile(r"^\s*(confirm|yes|ok|apply|deprecate|activate|restore)", re.I)))
        if confirm:
            confirm[-1].click()
    watch(context, 3)


# ---------- then: list ----------

@then("I see a row for \"{alias:Q}\" with: actions menu, name, description, current version, status, "
      "date created, last updated, creator")
def step_row_columns(context, alias):
    n = api.name(context, alias)
    row = find_row(context, alias)
    h = headers_of(list_table(context))
    # SIMPL-18161 lists the columns; the header wording is ours to match loosely.
    c = {k: col(h, p, k) for k, p in [
        ("actions", r"action"), ("name", r"^\s*name"), ("description", r"description"),
        ("current version", r"version"), ("status", r"status"), ("date created", r"created"),
        ("last updated", r"updated|modified"), ("creator", r"creator|created\s*by|author|owner")]}
    assert cell(row, c["actions"]).get_by_role("button").count(), "no actions control in the row"
    assert cell_text(row, c["name"]) == n, f"name cell: {cell_text(row, c['name'])!r}"
    assert f"Test vocabulary {n}" in cell_text(row, c["description"]), "description cell: " + cell_text(row, c["description"])
    assert cell_text(row, c["current version"]) == "1", "version cell: " + cell_text(row, c["current version"])
    assert status_ok(cell_text(row, c["status"]), "Active"), "status cell: " + cell_text(row, c["status"])
    for k in ("date created", "last updated"):
        assert re.search(r"\d", cell_text(row, c[k])), f"{k} cell: {cell_text(row, c[k])!r}"
    assert "ga-admin" in cell_text(row, c["creator"]), "creator cell: " + cell_text(row, c["creator"])


@then('the actions menu of "{alias:Q}" offers "{first:Q}" and "{second:Q}"')
def step_menu_offers(context, alias, first, second):
    row = find_row(context, alias)
    open_row_menu(context, row)
    names = menu_names(context)
    missing = [l for l in (first, second) if not any(quoted(l).search(nm) for nm in names)]
    close_menu(context)
    assert not missing, f"actions menu lacks {missing}; it offers {names}"


@then("I am told that the vocabulary list could not be loaded")
def step_list_error(context):
    # SIMPL-18161 negative: wording not fixed by the story.
    w = seen_window(context, r"(could\s*n[o']?t|cannot|can't|unable to|failed to|error)[^.]{0,60}"
                             r"(load|retriev|fetch|get)|(load|retriev|fetch)\w*[^.]{0,40}(fail|error)")
    assert w, "no message that the list could not be loaded; page shows: " + text(context)[:400]


@then("I see the details of the error")
def step_error_details(context):
    w = seen_window(context, r"Injected failure|Internal Server Error|\b500\b")
    assert w, "the error details (status, title or detail of the problem) are not shown; page: " + text(context)[:400]


@then('I see "{first:Q}" and "{second:Q}"')
def step_see_both(context, first, second):
    for a in (first, second):
        find_row(context, a, narrow=False)


@then('I see "{first:Q}" and not "{second:Q}"')
def step_see_not(context, first, second):
    find_row(context, first, narrow=False)
    n = api.name(context, second)
    assert not row_for(context, n).count(), f"{n} is still listed"


@then('every row shows status "{status:Q}"')
def step_every_status(context, status):
    t = list_table(context)
    i = col(headers_of(t), r"status", "status")
    rows = data_rows(t)
    assert rows, "the list is empty"
    bad = [cell_text(r, i) for r in rows if not status_ok(cell_text(r, i), status)]
    assert not bad, f"rows with another status: {bad[:5]}"


def names_in_order(context):
    t = list_table(context)
    i = col(headers_of(t), r"^\s*name", "name")
    return [cell_text(r, i) for r in data_rows(t)]


@then("the rows are ordered by name")
def step_ordered(context):
    names = names_in_order(context)
    assert len(names) >= 2, f"need two rows to judge the order, got {names}"
    low = [x.lower() for x in names]
    if low == sorted(low):
        context.sort_dir = "ascending"
    elif low == sorted(low, reverse=True):
        context.sort_dir = "descending"
    else:
        raise AssertionError(f"rows not ordered by name: {names}")


@then("the rows are ordered by name in the opposite direction")
def step_ordered_opposite(context):
    names = names_in_order(context)
    low = [x.lower() for x in names]
    want = sorted(low, reverse=(context.sort_dir == "ascending"))
    assert low == want, f"second click did not reverse the order (was {context.sort_dir}): {names}"


@then('the row of "{alias:Q}" shows a clear deprecation warning')
def step_row_warning(context, alias):
    row = find_row(context, alias)
    assert deprecation_warning(context, row), "no deprecation warning in the row: " + " ".join(row.inner_text().split())


# ---------- then: details, history, download ----------

@then('I am on the details page of "{alias:Q}" with its name in the header')
def step_details_header(context, alias):
    n = api.name(context, alias)
    p = page(context)
    heads = visible(p.get_by_role("heading", name=n, exact=False)) + visible(p.get_by_role("dialog", name=n))
    assert heads, f"no heading with {n}; headings: {[h.inner_text() for h in visible(p.get_by_role('heading'))]}"


@then("I see {count:d} versions in chronological order, each with: download action, version number, status, "
      "date created, description, change log")
def step_history_rows(context, count):
    t = history_table(context)
    h = headers_of(t)
    c = {k: col(h, p, k) for k, p in [
        ("version", r"^\s*version|version\s*(number|no)"), ("status", r"status"), ("date created", r"created|date"),
        ("description", r"description"), ("change log", r"change\s*-?\s*log")]}
    rows = data_rows(t)
    versions = [cell_text(r, c["version"]) for r in rows]
    assert len(rows) == count, f"expected {count} versions, got {versions}"
    # "Chronological" (SIMPL-18167) does not fix the direction: oldest or newest first (Barry, 27 Oct).
    asc = [str(v) for v in range(1, count + 1)]
    assert versions in (asc, asc[::-1]), f"not in chronological order: {versions}"
    for r in rows:
        v = cell_text(r, c["version"])
        assert cell_text(r, c["status"]), f"v{v}: empty status"
        assert re.search(r"\d", cell_text(r, c["date created"])), f"v{v}: no date"
        assert cell_text(r, c["description"]), f"v{v}: empty description"
        assert cell_text(r, c["change log"]), f"v{v}: empty change log"
        action = row_action(context, r, r"download")
        assert action.is_visible(), f"v{v}: no download action"
        close_menu(context)


@then('version {version:d} is labelled "{status:Q}" with a clear warning')
def step_version_label(context, version, status):
    row = version_row(context, version)
    i = col(headers_of(history_table(context)), r"status", "status")
    assert status_ok(cell_text(row, i), status), f"v{version} status: {cell_text(row, i)!r}"
    w = deprecation_warning(context, row, version)
    assert w, f"no deprecation warning for version {version}: " + " ".join(row.inner_text().split())
    context.warning = w
    context.warning_version = version


@then("the warning links to version {version:d}, the latest active version")
def step_warning_link(context, version):
    # SIMPL-18559 AC1: "a visible reference to the latest ACTIVE version", in the warning about this version:
    # the row's own warning, or an alert on the page about the same version.
    places = [context.warning] + [a for a in visible(page(context).get_by_role("alert"))
                                  if re.search(r"deprecat", a.inner_text(), re.I)
                                  and re.search(rf"version\s*{context.warning_version}\b", a.inner_text(), re.I)]
    links = [l for pl in places for l in visible(pl.get_by_role("link")) + visible(pl.get_by_role("button"))]
    hits = [l for l in links if re.search(rf"version\s*{version}\b|\bv{version}\b|latest|active",
                                          l.inner_text() or l.get_attribute("aria-label") or "", re.I)
            or re.search(rf"/versions?/{version}\b", l.get_attribute("href") or "")]
    assert hits, f"the warning has no link to version {version}; it says: " + \
                 " | ".join(" ".join(pl.inner_text().split()) for pl in places)


@then("I see its metadata: version, date created, creator, change log")
def step_version_metadata(context):
    t = text(context)
    missing = [k for k, p in [("version", r"version"), ("date created", r"\d{1,4}[-/. ]\w{1,3}[-/. ]\d{2,4}"),
                              ("creator", r"ga-admin"), ("change log", r"Initial version")]
               if not re.search(p, t, re.I)]
    assert not missing, f"version metadata lacks {missing}"


@then("every version shows a clear deprecation warning")
def step_every_version_warning(context):
    t = history_table(context)
    i = col(headers_of(t), r"^\s*version", "version")
    bad = [cell_text(r, i) for r in data_rows(t) if not deprecation_warning(context, r, cell_text(r, i))]
    assert not bad, f"versions without a deprecation warning: {bad}"


@then("no link to an active version is shown")
def step_no_active_link(context):
    p = page(context)
    places = [history_table(context)] + visible(p.get_by_role("alert"))
    hits = []
    for place in places:
        for l in visible(place.get_by_role("link")) + visible(place.get_by_role("button")):
            label = " ".join((l.inner_text() or l.get_attribute("aria-label") or "").split())
            if re.search(r"latest|active", label, re.I) and not re.search(r"download", label, re.I):
                hits.append(label)
    assert not hits, f"links to an active version shown although none exists: {hits}"


@then("a Turtle file of version {version:d} is downloaded")
def step_downloaded(context, version):
    assert context.download is not None, "nothing was downloaded"
    with open(context.download.path(), encoding="utf-8") as f:
        got = f.read()
    n = next(iter(context.names.values()))
    assert re.search(r"@prefix|PREFIX", got) and "owl:Ontology" in got, "not Turtle: " + got[:200]
    # D24: the file is the Turtle content of that version, as the API serves it.
    r = api_get(context, f"/content/{n}/versions/{version}", accept="text/turtle")
    assert r.status_code == 200, f"API content of v{version}: HTTP {r.status_code}"
    norm = lambda s: "\n".join(l.rstrip() for l in s.replace("\r\n", "\n").strip().splitlines())
    assert norm(got) == norm(r.text), f"downloaded file is not version {version}: starts {got[:160]!r}"


# ---------- then: create and new version ----------

@then("I see a creation form with the required fields Name, Description and File upload")
def step_create_form(context):
    s = scope(context)
    for label, loc in [("Name", field(context, r"^\s*name")), ("Description", field(context, r"^\s*description")),
                       ("File upload", s.locator("input[type=file]"))]:
        assert loc.count(), f"no {label} field in the creation form"
        el = loc.first
        required = el.evaluate("e => e.required || e.getAttribute('aria-required') === 'true'")
        label_txt = el.evaluate("e => (e.labels && e.labels[0] ? e.labels[0].innerText : '')") or ""
        assert required or "*" in label_txt or re.search(r"required", label_txt, re.I), \
            f"{label} is not marked as required"


@then("there is no field for version or change log")
def step_no_version_field(context):
    s = scope(context)
    bad = [x.get_attribute("aria-label") or x.evaluate("e => e.labels && e.labels[0] ? e.labels[0].innerText : ''")
           for x in visible(s.get_by_role("textbox")) + visible(s.get_by_role("spinbutton"))
           + visible(s.get_by_role("combobox"))
           if re.search(r"version|change\s*-?\s*log|change\s*summary",
                        x.evaluate("e => (e.labels && e.labels[0] ? e.labels[0].innerText : '') + ' ' + "
                                   "(e.getAttribute('aria-label') || '')"), re.I)]
    assert not bad, f"the creation form has fields {bad}"


@then("I see a confirmation that syntax and structure validation passed")
def step_validation_passed(context):
    # SIMPL-18180 AC4, SIMPL-18193: "a clear confirmation message ... validation passed".
    w = seen_window(context, r"(valid|syntax|structure)\w*[^.]{0,60}(pass|succe|ok\b)|"
                             r"(pass|succe)\w*[^.]{0,40}validat|\bis valid\b|no (validation )?errors")
    assert w, "no confirmation that validation passed was seen"


@then('"{alias:Q}" appears in the list with version {version:d} and status "{status:Q}"')
def step_in_list(context, alias, version, status):
    open_list(context)
    row = find_row(context, alias)
    h = headers_of(list_table(context))
    v = cell_text(row, col(h, r"version", "version"))
    st = cell_text(row, col(h, r"status", "status"))
    assert v == str(version), f"version cell {v!r}"
    assert status_ok(st, status), f"status cell {st!r}"


@then("I see an error that the description is required")
def step_desc_required(context):
    # SIMPL-18169 AC2: "informed with appropriate error messages". A disabled button alone informs nobody.
    # Either a message naming the description, or a "required" message attached to the Description field
    # (its own wrapper or aria-describedby), shown on blur or on submit.
    w = seen_window(context, r"description.{0,40}(required|mandatory|must|empty|missing)|"
                             r"(required|mandatory|missing|provide|enter).{0,40}description", size=1)
    attached = field(context, r"^\s*description").first.evaluate("""e => {
        const ids = (e.getAttribute('aria-describedby') || '').split(/\\s+/).filter(Boolean);
        let t = ids.map(i => (document.getElementById(i) || {}).innerText || '').join(' ');
        let n = e;
        for (let i = 0; i < 4 && n.parentElement; i++) {
            n = n.parentElement;
            if (n.querySelectorAll('input, textarea, select').length > 1) break;
            t += ' ' + n.innerText;
        }
        return t;
    }""")
    near = re.search(r"required|mandatory|must not be empty|cannot be empty|is empty|missing", attached, re.I)
    assert w or near, "no message that the description is required" + \
                      (" (the submit button is disabled without saying why)" if context.submit_blocked else "")


@then('I see an error that the name "{alias:Q}" is already used')
def step_name_used(context, alias):
    assert not context.submit_blocked, "the form cannot be submitted, so no duplicate-name check ran"
    w = seen_window(context, r"already|exists|unique|in use|taken|duplicate")
    assert w, "no message that the name is already used; page: " + text(context)[:300]


@then("no vocabulary is created")
def step_none_created(context):
    n = next(iter(context.names.values()))
    r = api_get(context, f"/vocabularies/{n}/versions")
    assert r.status_code == 200 and len(r.json().get("items", [])) == 1, f"{n}: {r.status_code} {r.text[:200]}"


@then("validation runs automatically on upload")
def step_validated_on_upload(context):
    # SIMPL-18180 AC1: on upload, without submitting.
    w = seen_window(context, r"error|invalid|syntax|unexpected|expected")
    assert w, "no validation result after the upload (nothing was submitted)"


LOCATION = r"line\s*:?\s*\d+|\[\s*\d+\s*[:,]\s*\d+\s*\]|\b\d+:\d+\b|column\s*\d+|row\s*\d+"


@then("I see a list of the syntax errors, each with its location")
def step_syntax_list(context):
    s = scope(context)
    items = [i.inner_text() for i in visible(s.get_by_role("listitem"))
             if re.search(r"error|unexpected|expected|invalid|syntax|line", i.inner_text(), re.I)]
    if items:
        bad = [i for i in items if not re.search(LOCATION, i, re.I)]
        assert not bad, f"errors without a location: {bad}"
    else:
        w = seen_window(context, r"error|unexpected|expected|invalid|syntax", LOCATION)
        assert w, "no syntax error with a location is shown; page: " + text(context)[:400]


@then("I cannot submit the form until the errors are fixed")
def step_cannot_submit(context):
    b = submit_button(context)
    if b.is_enabled():
        b.click()
        watch(context, 3)
    n = next(iter(context.names.values()))
    r = api_get(context, f"/vocabularies/{n}")
    assert r.status_code == 404, f"{n} was created although the file has syntax errors (HTTP {r.status_code})"


@then("I see a structured report of the semantic errors")
def step_semantic_report(context):
    # SIMPL-18182 AC3 and D17 (d): the term declared both as a class and a property is named.
    w = seen_window(context, r"Thing", r"class|property", size=4)
    assert w, "no report naming the offending term; page: " + text(context)[:400]


@then('"{alias:Q}" is not created')
def step_not_created(context, alias):
    r = api_get(context, "/vocabularies/{" + alias.strip("{}") + "}")
    assert r.status_code == 404, f"{api.name(context, alias)} was created (HTTP {r.status_code})"


@then("the form is pre-filled with the current metadata")
def step_prefilled(context):
    n = next(iter(context.names.values()))
    d = field(context, r"^\s*description")
    assert d.count(), "no description field in the update form"
    assert d.first.input_value().strip() == f"Test vocabulary {n}", \
        f"description not pre-filled: {d.first.input_value()!r}"


@then("there is no field for the version number")
def step_no_version_number(context):
    s = scope(context)
    bad = []
    for x in visible(s.get_by_role("textbox")) + visible(s.get_by_role("spinbutton")):
        label = x.evaluate("e => (e.labels && e.labels[0] ? e.labels[0].innerText : '') + ' ' + "
                           "(e.getAttribute('aria-label') || '')")
        if re.search(r"version", label, re.I) and not re.search(r"description|change|content", label, re.I):
            bad.append(label.strip())
    assert not bad, f"the form has a version field: {bad}"


@then('I see a confirmation that version {version:d} of "{alias:Q}" was created')
def step_version_confirmed(context, version, alias):
    # SIMPL-18172 AC2 notification; SIMPL-18189 AC3 with the new version identifier.
    w = seen_window(context, r"creat|success|saved|published", rf"(version|\bv)\s*:?\s*{version}\b", size=2)
    assert w, f"no confirmation naming version {version}; page: " + text(context)[:300]


@then('the history of "{alias:Q}" shows version {v1:d} "{s1:Q}" and version {v2:d} "{s2:Q}"')
def step_history_statuses(context, alias, v1, s1, v2, s2):
    open_history(context, alias)
    i = col(headers_of(history_table(context)), r"status", "status")
    for v, s in ((v1, s1), (v2, s2)):
        got = cell_text(version_row(context, v), i)
        assert status_ok(got, s), f"version {v}: expected {s}, got {got!r}"


@then("I see an error that the operation failed, with advice to retry or contact support")
def step_failed_with_advice(context):
    w = seen_window(context, r"fail|error|could\s*n[o']?t|unable", r"try again|retry|contact|support", size=4)
    assert w, "no failure notice with advice to retry or contact support; page: " + text(context)[:300]


@then('the history of "{alias:Q}" shows only version {version:d}')
def step_history_only(context, alias, version):
    open_history(context, alias)
    t = history_table(context)
    i = col(headers_of(t), r"^\s*version", "version")
    got = [cell_text(r, i) for r in data_rows(t)]
    assert got == [str(version)], f"versions shown: {got}"


@then('I see a warning that version {version:d} of "{alias:Q}" is deprecated')
def step_ref_warning(context, version, alias):
    n = api.name(context, alias)
    w = seen_window(context, re.escape(n), r"deprecat",
                    rf"version\W{{0,3}}{version}\b|/{version}#|\bv{version}\b|\b{version}\b", size=3)
    assert w, f"no warning that version {version} of {n} is deprecated; page: " + text(context)[:400]
    context.warning_text = w


@then("the warning recommends version {version:d}, the latest active version")
def step_ref_recommend(context, version):
    w = seen_window(context, r"deprecat|latest|active|recommend|instead",
                    rf"version\W{{0,3}}{version}\b|/{version}#|\bv{version}\b", size=3)
    assert w, f"the warning does not recommend version {version}"


# ---------- then: status and access ----------

@then("I see a confirmation that the status was updated")
def step_status_confirmed(context):
    w = seen_window(context, r"status.{0,60}(updated|changed|saved|success)|"
                             r"(updated|changed|success).{0,40}status", size=1)
    assert w, "no confirmation that the status was updated"


def row_status(context, alias):
    if not list_table(context).count() or not row_for(context, api.name(context, alias)).count():
        open_list(context)
    row = find_row(context, alias)
    return cell_text(row, col(headers_of(list_table(context)), r"status", "status"))


@then('the row of "{alias:Q}" shows status "{status:Q}"')
@then('the row of "{alias:Q}" still shows status "{status:Q}"')
def step_row_status(context, alias, status):
    got = row_status(context, alias)
    assert status_ok(got, status), f"status of {api.name(context, alias)}: {got!r}, expected {status}"


@then("I am told that the status could not be updated")
def step_status_failed(context):
    w = seen_window(context, r"could\s*n[o']?t|cannot|unable|fail|error", size=2)
    assert w, "no message that the status could not be updated; page: " + text(context)[:300]


@then('I see "{alias:Q}" in the list')
def step_in_list_plain(context, alias):
    find_row(context, alias)


@then('"New Vocabulary +", "Update" and the status change action are not shown')
def step_viewer_hidden(context):
    # D26: hidden, not just disabled. Checked on the list page and in the row's actions menu.
    p = page(context)
    shown = [b.inner_text().strip() for b in visible(p.get_by_role("button", name=re.compile(r"new\s*vocabulary", re.I)))]
    row = data_rows(list_table(context))[0]
    open_row_menu(context, row)
    shown += [nm for nm in menu_names(context) if re.search(r"update|status|deprecat|activat|restor", nm, re.I)]
    close_menu(context)
    assert not shown, f"management actions shown to a viewer: {shown}"


@then("I am asked to sign in")
def step_asked_to_sign_in(context):
    p = page(context)
    asked = password_field(context).first.is_visible() or bool(
        visible(p.get_by_role("button", name=re.compile(r"sign\s*in|log\s*in|login", re.I)))
        + visible(p.get_by_role("link", name=re.compile(r"sign\s*in|log\s*in|login", re.I))))
    listed = list_table(context).count() and len(data_rows(list_table(context)))
    assert asked and not listed, f"not asked to sign in (sign-in prompt: {asked}, vocabularies listed: {listed})"
