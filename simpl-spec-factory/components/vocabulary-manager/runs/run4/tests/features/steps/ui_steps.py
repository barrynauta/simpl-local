"""Browser steps for the @ui scenarios (step 60).

Written against the rendered pages only (roles, labels, texts); never against the code.
Data set-up still goes through the API steps of vm_steps.py.
"""
import json
import os
import re

from behave import given, then, when

from uidriver import UI, UI_URL, action_name

REPORTS = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "reports")


def U(context):
    if getattr(context, "ui", None) is None:
        context.ui = UI(context.w)
    return context.ui


def X(context, text):
    return context.w.expand(text)


def split(text):
    return [p.strip() for p in text.split(",") if p.strip()]


def visible_text(page, text):
    """Visible elements whose text contains text (case-sensitive)."""
    return page.get_by_text(text).filter(visible=True)


def body_text(page):
    return page.locator("body").inner_text()


def poll(fn, msg):
    return UI.poll(fn, msg)


# ====================================================================
# Who and where
# ====================================================================
@given('I sign in to the UI as "{user}"')
def ui_sign_in(context, user):
    U(context).sign_in(user)


@given("I open the UI without signing in")
@when("I open the UI without signing in")
def ui_anon(context):
    ui = U(context)
    ui.close()
    ui.ensure()


@given("I open the vocabulary list")
@when("I open the vocabulary list")
def ui_open_list(context):
    ui = U(context)
    ui.goto("/vocabularies")
    page = ui.page
    # The table, or one of the list messages, is on screen.
    poll(lambda: (page.get_by_role("table").count() > 0 or "vocabular" in body_text(page).lower(), page.url),
         "the vocabulary list did not open")


@given('I open the details of vocabulary "{alias}"')
@when('I open the details of vocabulary "{alias}"')
def ui_open_details(context, alias):
    ui = U(context)
    name = context.w.name(alias)
    ui.goto("/vocabularies")
    row = ui.row(alias)
    row.get_by_role("link", name=name, exact=True).click()
    ui.page.get_by_role("heading", level=1, name=name).wait_for()
    ui.settle()


@given('I open the UI address "{path}"')
@when('I open the UI address "{path}"')
def ui_open_path(context, path):
    U(context).goto(X(context, path))


@given('the API is stubbed to answer {code:d} to "{operation}"')
def ui_stub(context, code, operation):
    U(context).stub(operation, code)


@when('I open the address of the {form} form directly')
def ui_open_form_direct(context, form):
    ui = U(context)
    if form == "create":
        ui.goto("/vocabularies/new")
        return
    m = re.match(r'^new version of "(.+)"$', form)
    if not m:
        raise AssertionError("unknown form %r" % form)
    ui.goto("/vocabularies/%s/update" % context.w.name(m.group(1)))


# ====================================================================
# Actions
# ====================================================================
@given('I click "{label}" for vocabulary "{alias}"')
@when('I click "{label}" for vocabulary "{alias}"')
def ui_click_row(context, label, alias):
    ui = U(context)
    row = ui.open_row_menu(alias)
    row.get_by_role("menuitem", name=action_name(X(context, label))).click()
    ui.settle()


@given('I click "{label}" for version {v:d}')
@when('I click "{label}" for version {v:d}')
def ui_click_version(context, label, v):
    ui = U(context)
    ui.version_row(v).get_by_role("button", name=action_name(label)).click()
    ui.settle()


@given('I click "{label}"')
@when('I click "{label}"')
def ui_click(context, label):
    ui = U(context)
    page = ui.page
    name = action_name(X(context, label))
    scopes = []
    panel = page.get_by_role("complementary")
    if panel.count() and panel.first.is_visible():
        scopes.append(panel.first)       # the selected version's panel first
    scopes.append(page.locator("main"))
    for scope in scopes:
        for role in ("button", "link", "menuitem"):
            loc = scope.get_by_role(role, name=name).filter(visible=True)
            if loc.count():
                loc.first.click()
                ui.settle()
                return
    raise AssertionError("no visible button or link %r" % label)


@given('I fill in "{field}" with "{text}"')
@when('I fill in "{field}" with "{text}"')
def ui_fill(context, field, text):
    page = U(context).page
    page.get_by_label(field, exact=True).fill(X(context, text))


@given('I choose the file "{fixture}"')
@when('I choose the file "{fixture}"')
def ui_file(context, fixture):
    ui = U(context)
    data = context.w.file_value(fixture, "Upload")
    fname = fixture if not fixture.startswith("<") else "large-upload.ttl"
    ui.page.locator("input[type=file]").set_input_files(
        {"name": fname, "mimeType": "text/turtle", "buffer": data})
    # the browser-side syntax check (D13) runs on choice
    poll(lambda: (fname in body_text(ui.page), None), "the chosen file name is not shown")
    ui.page.wait_for_timeout(300)


@given("I submit the form")
@when("I submit the form")
def ui_submit(context):
    ui = U(context)
    btn = ui.page.get_by_role("button", name="Submit", exact=True)
    btn.wait_for()
    ui.page.wait_for_timeout(300)
    if btn.is_disabled():
        # D13: the browser check keeps Submit disabled while it shows errors. Pressing a
        # disabled button does nothing, so the step does nothing; the next steps check the page.
        return
    btn.click()
    ui.settle()
    ui.page.wait_for_timeout(300)


@when('I select version {v:d}')
def ui_select_version(context, v):
    ui = U(context)
    ui.page.get_by_role("button", name="View version %d" % v, exact=True).click()
    ui.page.get_by_role("complementary").first.wait_for()


@when('I set the filter "{name}" to "{value}"')
def ui_filter(context, name, value):
    page = U(context).page
    value = X(context, value)
    box = page.get_by_role("searchbox", name=name)
    if box.count():
        box.fill(value)
    else:
        combo = page.get_by_role("combobox", name=name)
        if combo.count():
            combo.select_option(label=value)
        else:
            page.get_by_label(name, exact=True).fill(value)
    U(context).settle()


@when('I click the column header "{column}" {n:d} times')
def ui_sort(context, column, n):
    ui = U(context)
    page = ui.page
    btn = page.get_by_role("columnheader").get_by_role(
        "button", name=re.compile("^" + re.escape(column) + r"\s*[▲▼↑↓]?$"))
    for _ in range(n):
        btn.first.click()
        ui.settle()
        page.wait_for_timeout(300)


# ====================================================================
# Checks
# ====================================================================
@then('I see "{text}"')
def ui_see(context, text):
    page = U(context).page
    text = X(context, text)
    poll(lambda: (visible_text(page, text).count() > 0 or text in body_text(page), body_text(page)[:1500]),
         "text not on the page: %r" % text)


@then('I do not see "{text}"')
def ui_not_see(context, text):
    page = U(context).page
    text = X(context, text)
    page.wait_for_timeout(500)
    assert text not in body_text(page), "text is on the page: %r" % text


def actions_on_page(context):
    """Visible action names: buttons, links and the items of one open row menu."""
    ui = U(context)
    page = ui.page
    names = set()
    for role in ("button", "link", "menuitem"):
        loc = page.get_by_role(role).filter(visible=True)
        for i in range(loc.count()):
            try:
                names.add((loc.nth(i).get_attribute("aria-label") or loc.nth(i).inner_text()).strip())
            except Exception:
                pass
    return names


def matches(names, label):
    rx = action_name(label)
    return [n for n in names if rx.match(n)]


@then('I see the action "{label}"')
def ui_see_action(context, label):
    page = U(context).page
    label = X(context, label)
    poll(lambda: (bool(matches(actions_on_page(context), label)), sorted(actions_on_page(context))),
         "action not shown: %r" % label)


@then('I do not see the action "{label}"')
def ui_not_see_action(context, label):
    ui = U(context)
    page = ui.page
    ui.settle()
    page.wait_for_timeout(500)
    seen = actions_on_page(context)
    # Row actions sit in the three-dot menu: open the menu of every own vocabulary row.
    if ui.on_list():
        for alias in list(context.w.names):
            try:
                ui.open_row_menu(alias)
                seen |= actions_on_page(context)
                ui.close_menus()
            except Exception:
                pass
    hits = matches(seen, label)
    assert not hits, "action shown: %r (as %s)" % (label, hits)


@then('the button "{label}" is disabled')
def ui_disabled(context, label):
    btn = U(context).page.get_by_role("button", name=label, exact=True)
    poll(lambda: (btn.is_disabled(), "enabled"), "button %r is not disabled" % label)


@then('the button "{label}" is enabled')
def ui_enabled(context, label):
    btn = U(context).page.get_by_role("button", name=label, exact=True)
    poll(lambda: (btn.is_enabled(), "disabled"), "button %r is not enabled" % label)


def header_names(table):
    hs = table.get_by_role("columnheader")
    return [re.sub(r"\s*[▲▼↑↓]\s*$", "", hs.nth(i).inner_text().strip()) for i in range(hs.count())]


@then('the vocabulary list shows the columns "{columns}"')
def ui_list_columns(context, columns):
    page = U(context).page
    want = split(columns)
    table = page.get_by_role("table").first
    table.wait_for()
    got = header_names(table)
    assert got == want, "columns %s, expected %s" % (got, want)


@then('the version list shows the columns "{columns}"')
def ui_version_columns(context, columns):
    ui = U(context)
    want = split(columns)
    t = ui.page.get_by_role("table").filter(has=ui.page.get_by_role("columnheader", name="Version", exact=True))
    t.first.wait_for()
    got = header_names(t.first)
    assert got == want, "columns %s, expected %s" % (got, want)


@then('the row of vocabulary "{alias}" shows "{text}"')
def ui_row_shows(context, alias, text):
    ui = U(context)
    text = X(context, text)
    poll(lambda: (text in ui.row(alias).inner_text(), ui.row(alias).inner_text()),
         "row of %s does not show %r" % (alias, text))


@then('the row of version {v:d} shows "{text}"')
def ui_version_row_shows(context, v, text):
    ui = U(context)
    text = X(context, text)
    poll(lambda: (text in ui.version_row(v).inner_text(), ui.version_row(v).inner_text()),
         "row of version %d does not show %r" % (v, text))


@then('the row of version {v:d} does not show "{text}"')
def ui_version_row_not(context, v, text):
    ui = U(context)
    row = ui.version_row(v)
    row.wait_for()
    got = row.inner_text()
    assert X(context, text) not in got, "row of version %d shows %r: %s" % (v, text, got)


@then('the row of vocabulary "{alias}" has the actions "{actions}"')
def ui_row_actions(context, alias, actions):
    ui = U(context)
    row = ui.open_row_menu(alias)
    items = row.get_by_role("menuitem")
    names = {items.nth(i).inner_text().strip() for i in range(items.count())}
    missing = [a for a in split(actions) if not matches(names, a)]
    ui.close_menus()
    assert not missing, "row menu of %s lacks %s; it has %s" % (alias, missing, sorted(names))


@then('the vocabulary list shows "{aliases}" in this order')
def ui_list_order(context, aliases):
    ui = U(context)
    want = [context.w.name(a) for a in split(aliases)]
    poll(lambda: (ui.list_names() == want, ui.list_names()),
         "list order is not %s" % want)


@then('the version list shows the versions "{versions}" in this order')
def ui_versions_order(context, versions):
    ui = U(context)
    want = split(versions)

    def got():
        btns = ui.version_table().get_by_role("button", name=re.compile(r"^View version \d+$"))
        return [btns.nth(i).inner_text().strip() for i in range(btns.count())]
    poll(lambda: (got() == want, got()), "versions are not in the order %s" % want)


@then('the version panel shows "{fields}"')
def ui_panel(context, fields):
    page = U(context).page
    panel = page.get_by_role("complementary").first
    panel.wait_for()
    missing = []
    for f in split(fields):
        has_term = panel.get_by_role("term").filter(has_text=re.compile("^" + re.escape(f) + "$")).count()
        has_button = panel.get_by_role("button", name=f, exact=True).count()
        if not (has_term or has_button):
            missing.append(f)
    assert not missing, "version panel lacks %s:\n%s" % (missing, panel.inner_text())


def form_field(page, field):
    loc = page.locator("main").get_by_label(field, exact=True).filter(visible=True)
    if loc.count():
        return loc
    return page.locator("main").get_by_role("button", name=field, exact=True).filter(visible=True)


@then('the form shows the fields "{fields}"')
def ui_form_fields(context, fields):
    page = U(context).page
    page.get_by_role("button", name="Submit", exact=True).wait_for()
    missing = [f for f in split(fields) if form_field(page, f).count() == 0]
    assert not missing, "form lacks the fields %s" % missing


@then('the form does not show the fields "{fields}"')
def ui_form_no_fields(context, fields):
    page = U(context).page
    page.get_by_role("button", name="Submit", exact=True).wait_for()
    shown = [f for f in split(fields) if form_field(page, f).count() > 0]
    assert not shown, "form shows the fields %s" % shown


@then('the field "{field}" holds "{text}" in the form')
def ui_field_holds(context, field, text):
    page = U(context).page
    loc = page.get_by_label(field, exact=True)
    want = X(context, text)
    poll(lambda: (loc.input_value().strip() == want.strip(), loc.input_value()),
         "field %s does not hold %r" % (field, want))


@then('the field "{field}" is empty in the form')
def ui_field_empty(context, field):
    page = U(context).page
    loc = page.get_by_label(field, exact=True)
    loc.wait_for()
    assert loc.input_value() == "", "field %s holds %r" % (field, loc.input_value())


@then('the file "{name}" is attached in the form')
def ui_file_attached(context, name):
    page = U(context).page
    want = X(context, name)
    poll(lambda: (want in page.locator("main").inner_text(), page.locator("main").inner_text()[:800]),
         "form does not show the attached file %r" % want)


@then('a file "{filename}" is downloaded with the content of version {v:d} of "{alias}"')
def ui_downloaded(context, filename, v, alias):
    ui = U(context)
    poll(lambda: (len(ui.downloads) > 0, "no download"), "no file was downloaded")
    d = ui.downloads.pop(0)
    want = X(context, filename)
    assert d.suggested_filename == want, "downloaded %r, expected %r" % (d.suggested_filename, want)
    with open(d.path(), "rb") as fh:
        data = fh.read()
    expected = context.w.contents[(alias, v)]
    assert data == expected, "downloaded content differs from version %d of %s (%d bytes vs %d)" % (
        v, alias, len(data), len(expected))


@then('I am on the UI address "{path}"')
def ui_on_path(context, path):
    ui = U(context)
    want = X(context, path)
    poll(lambda: (ui.path() == want, ui.page.url), "not on %s" % want)


@then("I am sent to the sign-in page")
def ui_signin_page(context):
    page = U(context).page
    token_host = re.match(r"^([a-z]+://[^/]+)", context.w.cfg.token_url).group(1)
    poll(lambda: (page.url.startswith(token_host) and page.locator("#username, input[name=username]").count() > 0,
                  page.url), "not on the sign-in page of the identity provider")


def report_panel(page):
    return page.get_by_role("alert").filter(has_text="Validation report")


@then('the report panel lists an issue "{detail}" at "{location}"')
def ui_report_issue(context, detail, location):
    page = U(context).page
    panel = report_panel(page)
    detail, location = X(context, detail), X(context, location)

    def check():
        items = panel.get_by_role("listitem")
        texts = [items.nth(i).inner_text() for i in range(items.count())]
        return any(detail in t and location in t for t in texts), texts
    poll(check, "report panel has no issue %r at %r" % (detail, location))


@then('the report panel lists {n:d} issues')
def ui_report_count(context, n):
    page = U(context).page
    panel = report_panel(page)

    def check():
        items = panel.get_by_role("listitem")
        return items.count() == n, [items.nth(i).inner_text() for i in range(items.count())]
    poll(check, "report panel does not list %d issues" % n)


@then('the warnings panel lists "{text}"')
def ui_warning_panel(context, text):
    page = U(context).page
    want = X(context, text)

    def check():
        panels = page.locator("[role=alert], [role=status], [role=region], [role=note], aside, section").filter(
            has_text=re.compile("[Ww]arning"))
        texts = [panels.nth(i).inner_text() for i in range(panels.count())]
        return any(want in t for t in texts), texts or body_text(page)[:1500]
    poll(check, "no warnings panel lists %r" % want)


@then("the form stays open")
def ui_form_open(context):
    ui = U(context)
    page = ui.page
    page.wait_for_timeout(500)
    assert page.get_by_role("button", name="Submit", exact=True).is_visible(), \
        "the form is not open; page is %s" % page.url
    assert re.search(r"/(new|update)$", ui.path()), "not on a form address: %s" % ui.path()


@then('the link "{label}" leads to version {v:d}')
def ui_link_version(context, label, v):
    page = U(context).page
    page.get_by_role("link", name=X(context, label), exact=True).first.click()
    panel = page.get_by_role("complementary")
    poll(lambda: (panel.count() > 0 and panel.first.get_by_role("heading", name="Version %d" % v, exact=True).count() > 0,
                  panel.first.inner_text() if panel.count() else page.url),
         "the link did not open version %d" % v)


@then("I see a success notification")
def ui_success(context):
    page = U(context).page
    region = page.get_by_role("region", name="Notifications")
    poll(lambda: (region.get_by_role("status").count() > 0, body_text(page)[:600]),
         "no success notification")


# ====================================================================
# Accessibility (WCAG 2.1 AA, axe-core)
# ====================================================================
@when('I open the {which} page for the accessibility check')
def ui_open_a11y_page(context, which):
    ui = U(context)
    w = context.w
    if which == "vocabulary list":
        ui.goto("/vocabularies")
        ui.page.get_by_role("table").first.wait_for()
    elif which == "vocabulary details":
        ui.goto("/vocabularies/%s" % w.name("Health"))
        ui.page.get_by_role("heading", level=2, name="Version history").wait_for()
        ui.page.get_by_role("button", name="View version 1", exact=True).click()
        ui.page.get_by_role("complementary").first.wait_for()
    elif which == "creation form":
        ui.goto("/vocabularies/new")
        ui.page.get_by_role("button", name="Submit", exact=True).wait_for()
    elif which == "new version form":
        ui.goto("/vocabularies/%s/update" % w.name("Health"))
        ui.page.get_by_role("button", name="Submit", exact=True).wait_for()
    else:
        raise AssertionError("unknown page %r" % which)
    ui.page.wait_for_timeout(500)
    context.a11y_page = which


@then("the page has no WCAG 2.1 AA violations")
def ui_axe(context):
    from axe_playwright_python.sync_playwright import Axe
    page = U(context).page
    result = Axe().run(page, options={"runOnly": {"type": "tag",
                                                  "values": ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]}})
    violations = result.response.get("violations", [])
    os.makedirs(REPORTS, exist_ok=True)
    slug = re.sub(r"[^a-z]+", "-", context.a11y_page)
    with open(os.path.join(REPORTS, "a11y-%s.json" % slug), "w") as fh:
        json.dump({"page": context.a11y_page, "url": page.url, "violations": violations}, fh, indent=2)
    lines = ["%s (%s): %s [%d nodes]" % (v["id"], v.get("impact"), v["help"], len(v.get("nodes", [])))
             for v in violations]
    assert not violations, "%d axe violations on the %s page:\n%s" % (len(violations), context.a11y_page,
                                                                       "\n".join(lines))
