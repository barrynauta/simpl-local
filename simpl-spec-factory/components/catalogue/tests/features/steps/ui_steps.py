"""Browser steps for features/ui (tagged @ui): Playwright sync API, headless Chromium.

Black box: the steps see only what a user sees (DOM, accessibility tree) and the browser's
own HTTP traffic. Setup (publishing RDs, quality stub scores) reuses the API Given steps.

Locators, in this order:
1. Story wording, through accessible roles and labels, case-insensitive and whitespace tolerant:
   - "quick-search-tab"    a tab, button or link named "Quick search"   (SIMPL-29292 AC1 "Quick
                           Search Tab", SIMPL-31102 "Quick Search")
   - "advanced-search-tab" a tab, button or link named "Advanced search" (SIMPL-29293 AC1,
                           SIMPL-31103, SIMPL-35874)
   - "search-button"       a button named exactly "Search" (SIMPL-31142 "the Search button")
   - "min-score-input"     the control labelled "minimum score" (SIMPL-29292, SIMPL-29293 AC1
                           "filter fields for minimum score and minimum classification")
   - "min-classification-select" the control labelled "minimum classification" (same ACs)
2. Everything else: the data-testid values of digest section 7 (decision D70), with the
   attributes data-sd-id (result-row) and data-path (detail-property) of the same section. A
   failure there reads as "different decision", which is why the UI features carry @D70.
3. Where neither fixes the detail (property paths of D75 for "every property"), a tolerant
   pattern, commented at the step.
"""
import json
import re
import time
import uuid
from urllib.parse import parse_qs, quote, urlparse

from behave import given, then, when
from playwright.sync_api import expect

from support import body as B
from support import browser

# ---------------------------------------------------------------- locators

_STORY_NAMES = {
    "quick-search-tab": re.compile(r"^\s*quick\s*search\s*$", re.I),
    "advanced-search-tab": re.compile(r"^\s*advanced\s*search\s*$", re.I),
}
_SEARCH_BUTTON = re.compile(r"^\s*search\s*$", re.I)
_STORY_LABELS = {
    "min-score-input": re.compile(r"minimum\s*score", re.I),
    "min-classification-select": re.compile(r"minimum\s*classification", re.I),
}


def _page(context):
    return browser.page(context)


def _loc(context, testid):
    """Locator for a step name: story wording first (level 1), digest testid (level 2)."""
    pg = _page(context)
    if testid in _STORY_NAMES:
        name = _STORY_NAMES[testid]
        return (pg.get_by_role("tab", name=name)
                .or_(pg.get_by_role("button", name=name))
                .or_(pg.get_by_role("link", name=name))).first
    if testid == "search-button":
        return pg.get_by_role("button", name=_SEARCH_BUTTON).first
    if testid in _STORY_LABELS:
        return pg.get_by_label(_STORY_LABELS[testid]).first
    return pg.get_by_test_id(testid)


def _norm(text):
    return re.sub(r"\s+", " ", text or "").strip()


def _render(context, text):
    return B.render(text, context)


def _rd_id(context, alias):
    if alias in context.rds:
        return context.rds[alias].id
    # An alias without an RD ("missing"): a well-formed id that nobody published.
    key = f"_ui_unknown_{alias}"
    if key not in context.vars:
        context.vars[key] = f"did:web:test.simpl.local:DataOffering:{uuid.uuid4()}"
    return context.vars[key]


def _row_selector(rd_id):
    return f'[data-testid="result-row"][data-sd-id="{rd_id}"]'


def _settle(context):
    browser.settle(_page(context))


def _timeout(context):
    return context.cfg.ui_timeout_ms


# ---------------------------------------------------------------- Given

@given('I am logged in to the UI as {user:Q}')
def step_logged_in(context, user):
    assert getattr(context, "ui", None), "test bug: browser not started (scenario not tagged @ui?)"
    context.ui["user"] = user
    # The page opens on first use, so that fault injection of later Given steps is in place.


@given('the browser request to {path:Q} fails')
def step_request_fails(context, path):
    browser.fail_requests(context, _render(context, path))


# ---------------------------------------------------------------- When

@when('I open the {tab:Q} tab')
def step_open_tab(context, tab):
    loc = _loc(context, tab)
    expect(loc, f"{tab}: no element for it").to_be_visible(timeout=_timeout(context))
    loc.click()
    _settle(context)


def _fill(loc, text):
    try:
        loc.fill(text)
    except Exception:
        # For example letters into input[type=number]: type them as a user would.
        loc.click()
        loc.press("Control+a")
        loc.press("Backspace")
        loc.press_sequentially(text)


@when('I type {text:Q} into {testid:Q}')
def step_type(context, text, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    _fill(loc, _render(context, text))
    loc.blur()
    _settle(context)


@when('I clear {testid:Q}')
def step_clear(context, testid):
    _loc(context, testid).fill("")
    _settle(context)


@when('I click {testid:Q}')
def step_click(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    expect(loc, f"{testid}: not enabled").to_be_enabled(timeout=_timeout(context))
    loc.click()
    _settle(context)


@when('I check {testid:Q}')
def step_check(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    loc.check()
    _settle(context)


@when('I uncheck {testid:Q}')
def step_uncheck(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    loc.uncheck()
    _settle(context)


@when('I select {value:Q} in {testid:Q}')
def step_select(context, value, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    value = _render(context, value)
    tag = loc.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        labels = [_norm(t) for t in loc.locator("option").all_inner_texts()]
        match = next((t for t in labels if t.lower() == value.lower()), None)
        if match is not None:
            loc.select_option(label=match)
        else:
            try:
                loc.select_option(value=value)
            except Exception as exc:
                raise AssertionError(f"{testid}: no option {value!r}; options are {labels}") from exc
    else:
        _fill(loc, value)
    _settle(context)


@when('I open the result row of {alias:Q} in {testid:Q}')
def step_open_row(context, alias, testid):
    panel = _loc(context, testid)
    row = panel.locator(_row_selector(_rd_id(context, alias)))
    expect(row, f"{testid}: no result row of {alias}").to_be_visible(timeout=_timeout(context))
    row.first.click()
    _settle(context)
    if context.cfg.verbose:
        print(f"detail page of {alias}: {_page(context).url}")


@when('I open the detail page of {alias:Q} in the catalogue {cid:Q}')
def step_open_detail(context, alias, cid):
    rd_id = _rd_id(context, alias)
    url = context.cfg.ui_detail_url.format(
        ui=context.cfg.services["ui-a"], catalogueId=quote(cid, safe=""), id=rd_id,
        id_enc=quote(rd_id, safe=""))
    browser.goto(context, url)


# ---------------------------------------------------------------- Then: elements

@then('{testid:Q} is visible')
def step_visible(context, testid):
    expect(_loc(context, testid), f"{testid}: expected visible").to_be_visible(timeout=_timeout(context))


@then('{testid:Q} is not visible')
def step_not_visible(context, testid):
    _settle(context)
    expect(_loc(context, testid), f"{testid}: expected not visible").to_be_hidden(timeout=_timeout(context))


@then('{testid:Q} is enabled')
def step_enabled(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: no such element").to_be_attached(timeout=_timeout(context))
    expect(loc, f"{testid}: expected enabled").to_be_enabled(timeout=_timeout(context))


@then('{testid:Q} is disabled')
def step_disabled(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: no such element").to_be_attached(timeout=_timeout(context))
    expect(loc, f"{testid}: expected disabled").to_be_disabled(timeout=_timeout(context))


@then('{testid:Q} is checked')
def step_checked(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: no such element").to_be_attached(timeout=_timeout(context))
    expect(loc, f"{testid}: expected checked").to_be_checked(timeout=_timeout(context))


@then('{testid:Q} is not checked')
def step_not_checked(context, testid):
    loc = _loc(context, testid)
    expect(loc, f"{testid}: no such element").to_be_attached(timeout=_timeout(context))
    expect(loc, f"{testid}: expected not checked").not_to_be_checked(timeout=_timeout(context))


def _text_pattern(text, whole):
    words = [re.escape(w) for w in _norm(text).split(" ") if w]
    core = r"\s*".join(words) if len(words) <= 1 else r"\s+".join(words)
    return re.compile((r"^\s*" + core + r"\s*$") if whole else core, re.I)


@then('{testid:Q} shows the text {text:Q}')
def step_shows_text(context, testid, text):
    text = _render(context, text)
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    expect(loc, f"{testid}: expected the text {text!r}").to_have_text(
        _text_pattern(text, whole=True), timeout=_timeout(context))


@then('{testid:Q} contains the text {text:Q}')
def step_contains_text(context, testid, text):
    text = _render(context, text)
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    expect(loc, f"{testid}: expected to contain {text!r}").to_contain_text(
        _text_pattern(text, whole=False), timeout=_timeout(context))


@then('{testid:Q} has the options {options:Q} in this order')
def step_options(context, testid, options):
    # Kept with empty parts: ", Red, Yellow, Green" means an empty option first (D72).
    expected = [p.strip() for p in options.split(",")]
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    _settle(context)
    deadline = time.monotonic() + _timeout(context) / 1000
    actual = [_norm(t) for t in loc.locator("option").all_inner_texts()]
    while len(actual) < len(expected) and time.monotonic() < deadline:
        time.sleep(0.2)
        actual = [_norm(t) for t in loc.locator("option").all_inner_texts()]
    assert [a.lower() for a in actual] == [e.lower() for e in expected], \
        f"{testid}: options {actual}, expected {expected}"


@then('{testid:Q} offers the choices {options:QList}')
def step_choices(context, testid, options):
    """SIMPL-35874 AC1 "the list of schemas available": the set of real choices. Neither the story
    nor D74 fixes order or label form, so the order is free, a placeholder option (empty value or
    disabled) is ignored, and labels compare case-insensitive with all whitespace removed
    ("Data Offering Shape" is the stub title of DataOfferingShape)."""
    loc = _loc(context, testid)
    expect(loc, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    _settle(context)
    script = """e => Array.from(e.options || []).filter(o => o.value !== '' && !o.disabled)
                                 .map(o => o.textContent)"""
    deadline = time.monotonic() + _timeout(context) / 1000
    actual = loc.evaluate(script)
    while len(actual) < len(options) and time.monotonic() < deadline:
        time.sleep(0.2)
        actual = loc.evaluate(script)
    squash = lambda t: re.sub(r"\s+", "", t or "").lower()
    assert sorted(squash(a) for a in actual) == sorted(squash(o) for o in options), \
        f"{testid}: choices {[_norm(a) for a in actual]}, expected {options} (any order)"


# ---------------------------------------------------------------- Then: results (D73)

@then('the result tabs are {ids:QList} in this order')
def step_tabs(context, ids):
    pg = _page(context)
    tabs = pg.locator('[data-testid^="results-tab-"]')
    expect(tabs.first, "no results-tab-* element").to_be_visible(timeout=_timeout(context))
    _settle(context)
    actual = [t.get_attribute("data-testid")[len("results-tab-"):] for t in tabs.all() if t.is_visible()]
    assert actual == ids, f"result tabs {actual}, expected {ids}"


def _panel(context, testid):
    panel = _loc(context, testid)
    expect(panel, f"{testid}: not visible").to_be_visible(timeout=_timeout(context))
    _settle(context)
    return panel


@then('{testid:Q} shows the result row of {alias:Q}')
def step_shows_row(context, testid, alias):
    row = _panel(context, testid).locator(_row_selector(_rd_id(context, alias)))
    expect(row.first, f"{testid}: no visible result row of {alias}").to_be_visible(timeout=_timeout(context))


@then('{testid:Q} does not show the result row of {alias:Q}')
def step_no_row(context, testid, alias):
    row = _panel(context, testid).locator(_row_selector(_rd_id(context, alias)))
    expect(row, f"{testid}: result row of {alias} is shown").to_be_hidden(timeout=_timeout(context))


@then('the result row of {alias:Q} in {testid:Q} shows {child:Q}')
def step_row_shows(context, alias, testid, child):
    row = _panel(context, testid).locator(_row_selector(_rd_id(context, alias))).first
    expect(row, f"{testid}: no result row of {alias}").to_be_visible(timeout=_timeout(context))
    cell = row.get_by_test_id(child).first
    expect(cell, f"row of {alias}: {child} not visible").to_be_visible(timeout=_timeout(context))
    assert _norm(cell.inner_text()), f"row of {alias}: {child} is empty"


@then('the result row of {alias:Q} in {testid:Q} does not show {child:Q}')
def step_row_not_shows(context, alias, testid, child):
    row = _panel(context, testid).locator(_row_selector(_rd_id(context, alias))).first
    expect(row, f"{testid}: no result row of {alias}").to_be_visible(timeout=_timeout(context))
    cells = row.get_by_test_id(child)
    shown = [_norm(c.inner_text()) for c in cells.all() if c.is_visible()]
    assert not [s for s in shown if s], f"row of {alias}: {child} is shown with {shown}"


@then('{testid:Q} shows {n:d} result rows')
def step_row_count(context, testid, n):
    rows = _panel(context, testid).locator('[data-testid="result-row"]')
    visible = [r for r in rows.all() if r.is_visible()]
    assert len(visible) == n, f"{testid}: {len(visible)} visible result rows, expected {n}"


# ---------------------------------------------------------------- Then: detail (D75)

def _property_rows(context):
    _settle(context)
    return _page(context).locator('[data-testid="detail-property"]')


def _detail_paths(node, prefix=""):
    """Leaf paths of credentialSubject per D75: nested objects flattened with '/', @context out."""
    out = []
    for key, value in node.items():
        if key == "@context":
            continue
        path = f"{prefix}/{key}" if prefix else key
        if isinstance(value, dict):
            out.extend(_detail_paths(value, path))
        elif isinstance(value, list):
            for i, item in enumerate(value):
                if isinstance(item, dict):
                    out.extend(_detail_paths(item, f"{path}/{i}"))
                else:
                    out.append(path)
        else:
            out.append(path)
    return sorted(set(out))


def _covered(leaf, shown):
    """Tolerant (SIMPL-31143 "all published properties", D75): a leaf counts as shown if its path
    is shown, or the path without trailing JSON-LD keyword segments (for example
    simpl:price/@value shown as simpl:price) or without list indexes. A top-level JSON-LD
    keyword (@id) is the identity, not a property, and is not required."""
    if "/" not in leaf and leaf.startswith("@"):
        return True
    candidates = {leaf, re.sub(r"/\d+(?=/|$)", "", leaf)}
    parts = leaf.split("/")
    while parts and parts[-1].startswith("@"):
        parts = parts[:-1]
        if parts:
            candidates.add("/".join(parts))
    return bool(candidates & shown)


@then('the detail page shows the property {path:Q} with the value {value:Q}')
def step_detail_property(context, path, value):
    value = _render(context, value)
    row = _page(context).locator(f'[data-testid="detail-property"][data-path="{path}"]')
    expect(row.first, f"no detail-property row with data-path {path!r}").to_be_visible(timeout=_timeout(context))
    expect(row.first, f"detail-property {path}: expected the value {value!r}").to_have_text(
        _text_pattern(value, whole=True), timeout=_timeout(context))


@then('the detail page shows no property rows')
def step_detail_no_rows(context):
    rows = _property_rows(context)
    visible = [r.get_attribute("data-path") for r in rows.all() if r.is_visible()]
    assert not visible, f"detail-property rows shown: {visible[:10]}"


@then('the detail page shows a property row for every property of {alias:Q}')
def step_detail_all(context, alias):
    rows = _page(context).locator('[data-testid="detail-property"]')
    expect(rows.first, "no detail-property rows").to_be_visible(timeout=_timeout(context))
    _settle(context)
    shown = {r.get_attribute("data-path") for r in rows.all()}
    rd = context.rds[alias]
    subject = (rd.last_body or rd.vc)["credentialSubject"]
    missing = [leaf for leaf in _detail_paths(subject) if not _covered(leaf, shown)]
    assert not missing, f"no detail-property row for {missing}; shown paths: {sorted(p for p in shown if p)}"


# ---------------------------------------------------------------- Then: browser traffic

def _requests_to(context, path):
    target = urlparse(path)
    out = []
    for req in context.ui["requests"]:
        u = urlparse(req["url"])
        if u.path.rstrip("/").endswith(target.path.rstrip("/")):
            out.append((req, u))
    return target, out


@then('the browser requested {path:Q}')
def step_requested(context, path):
    """Path match plus: every query parameter given in the step has that value in the request.
    Further parameters (paging, catalogueIds) are allowed."""
    _settle(context)
    path = _render(context, path)
    target, hits = _requests_to(context, path)
    want = parse_qs(target.query, keep_blank_values=True)
    for req, u in hits:
        got = parse_qs(u.query, keep_blank_values=True)
        if all(sorted(got.get(k, [])) == sorted(v) for k, v in want.items()):
            return
    seen = sorted({r["method"] + " " + urlparse(r["url"]).path + ("?" + urlparse(r["url"]).query if urlparse(r["url"]).query else "")
                   for r in context.ui["requests"] if "/api/" in r["url"]})
    raise AssertionError(f"no browser request to {path}; API requests seen: {seen}")


@then('no browser request to {path:Q} had the parameter {name:Q}')
def step_no_param(context, path, name):
    _settle(context)
    _, hits = _requests_to(context, _render(context, path))
    assert hits, f"no browser request to {path} at all"
    bad = [r["url"] for r, u in hits if name in parse_qs(u.query, keep_blank_values=True)]
    assert not bad, f"request(s) sent the parameter {name}: {bad}"


@then('the browser sent no request to {path:Q}')
def step_no_request(context, path):
    _settle(context)
    _, hits = _requests_to(context, _render(context, path))
    assert not hits, f"unexpected browser request(s): {[r['url'] for r, _ in hits]}"


@then('the last advanced search request body is the JSON')
def step_adv_body(context):
    _settle(context)
    expected = json.loads(_render(context, context.text))
    posts = [r for r in context.ui["requests"]
             if r["method"] == "POST" and "/selfdescriptions/advanced" in urlparse(r["url"]).path.lower()]
    assert posts, ("no POST to .../selfDescriptions/advanced; POSTs seen: "
                   f"{[r['url'] for r in context.ui['requests'] if r['method'] == 'POST']}")
    raw = posts[-1]["body"]
    try:
        actual = json.loads(raw) if raw else None
    except ValueError:
        actual = raw
    assert actual == expected, f"advanced search body {json.dumps(actual)}\nexpected {json.dumps(expected)}\nURL {posts[-1]['url']}"


# ---------------------------------------------------------------- downloads

def download_bytes(context, trigger):
    """Run `trigger` (a callable that clicks something) and return the downloaded bytes.
    For download scenarios: compare with the API's content of that version."""
    pg = _page(context)
    with pg.expect_download(timeout=_timeout(context)) as info:
        trigger()
    path = info.value.path()
    with open(path, "rb") as fh:
        return fh.read()
