"""Browser steps for features/ui (tagged @ui): Playwright sync API, headless Chromium.

Black box: the steps see only what a user sees (DOM, accessibility tree, URL) and
the browser's own traffic. Setup (negotiations driven through the stub, lifecycle
actions, consent changes) reuses the API and stub Given steps.

Locators, in this order:
1. Story wording, matched case-insensitive and whitespace tolerant. The UI stories
   quote only the overview column names (SIMPL-30959 AC1 and AC2, used by the
   table header step) and the status labels (30959, 32505 "Accepted"), which are
   also the D37 labels. No story quotes a button or field label.
2. Everything else: the D72 data-testid values (contracts-table,
   contract-row-<nid>, cell-*, filter-*, detail-*, action-*, reason-*, ...) and
   the D71 routes (/ overview, /contracts/<nid> details, /contractOffers/<nid>
   offer URL). They are shared decisions, so the scenarios carry @D71 or @D72; a
   failure there reads as "different decision".
3. Where neither fixes a detail, a tolerant pattern with a comment naming the
   story (reason dialog refusal, consent redirect, sanitising).

Absence (D73) is checked only after the screen has loaded (details or overview
rendered and the network idle), so a missing button is never read too early.
"""
import re
import time
from urllib.parse import parse_qs, urlparse

from behave import given, then, when
from playwright.sync_api import expect

import param_types  # noqa: F401
from cmtest import browser
from cmtest.core import body_json, render

# All D72 hooks, for "the only visible element is ...".
D72_TESTIDS = [
    "contracts-table", "cell-negotiation-id", "cell-resource-description-id",
    "cell-resource-name", "cell-counterparty-name", "cell-status", "contracts-empty",
    "filter-negotiation-id", "filter-resource-name", "filter-counterparty-name",
    "filter-status", "filter-apply", "contract-details", "detail-negotiation-id",
    "detail-resource-description-id", "detail-resource-name", "detail-provider-name",
    "detail-consumer-name", "detail-status", "detail-consent-status", "human-readable",
    "back-to-overview", "action-accept", "action-reject", "action-suspend",
    "action-request-reactivation", "action-reactivate", "action-request-revocation",
    "action-accept-revocation", "action-deny-revocation", "action-terminate",
    "action-update-consent", "reason-input", "reason-submit", "reason-cancel",
    "error-message", "access-denied"]
D72_PREFIXES = ["contract-row-", "open-details-"]

# Elements a screen shows once it has loaded (D71, D72, D74).
READY = ["contract-details", "contracts-table", "contracts-empty", "access-denied",
         "error-message"]


# ---------------------------------------------------------------- helpers

def _page(context):
    return browser.page(context)


def _timeout(context):
    return context.config_cm.ui_timeout_ms


def _r(context, text):
    return render(text, context.vars)


def _loc(context, testid):
    return _page(context).get_by_test_id(_r(context, testid))


def _norm(text):
    return re.sub(r"\s+", " ", text or "").strip()


def _exact(text):
    """Case-insensitive, whitespace tolerant, whole text."""
    words = [re.escape(w) for w in _norm(text).split(" ") if w]
    return re.compile(r"^\s*" + r"\s+".join(words) + r"\s*$", re.I)


def _settle(context):
    browser.settle(_page(context))


def _wait_ready(context):
    """Wait until the screen shows one of its D72 anchors and the network is idle."""
    pg = _page(context)
    sel = ", ".join('[data-testid="%s"]' % t for t in READY)
    try:
        pg.locator(sel).first.wait_for(state="visible", timeout=_timeout(context))
    except Exception as exc:
        raise AssertionError("the screen did not load (none of %s visible); page is %s, "
                             "text: %r" % (READY, pg.url, _norm(pg.inner_text("body"))[:300])
                             ) from exc
    if pg.get_by_test_id("contract-details").count():
        # Details: also wait for the status, so that allowedActions (D73) are in.
        status = pg.get_by_test_id("detail-status")
        try:
            expect(status).not_to_have_text(re.compile(r"^\s*$"), timeout=_timeout(context))
        except AssertionError:
            pass
    _settle(context)


def _nid(context):
    return context.vars["nid"]


def _row(context):
    return _page(context).get_by_test_id("contract-row-" + _nid(context))


def _fill(loc, text):
    loc.fill(text)


def _choose(loc, value, name):
    """Select an option by its label (case-insensitive) or value; fill a text field."""
    tag = loc.evaluate("e => e.tagName.toLowerCase()")
    if tag == "select":
        labels = [_norm(t) for t in loc.locator("option").all_inner_texts()]
        match = next((t for t in labels if t.lower() == _norm(value).lower()), None)
        if match is not None:
            loc.select_option(label=match)
            return
        try:
            loc.select_option(value=value)
        except Exception as exc:
            raise AssertionError("%s: no option %r; options are %s" % (name, value, labels)) \
                from exc
    else:
        _fill(loc, value)


# ---------------------------------------------------------------- Given / When: sign-in

@given('I am logged in to the {party:Party} UI as {user:Q}')
def ui_login(context, party, user):
    browser.login(context, party, user)


@when('I log in to the {party:Party} UI as {user:Q}')
def ui_login_when(context, party, user):
    browser.login(context, party, user)


@given('the browser request to {path:Q} fails')
def ui_request_fails(context, path):
    """Fault injection with page.route: a Problem Details 500 for the matching request."""
    browser.fail_requests(context, _r(context, path))


# ---------------------------------------------------------------- When: navigation

@when('I open the overview')
def ui_overview(context):
    # D71: route "/" is the overview.
    browser.goto(context, browser.base_url(context) + "/")
    _wait_ready(context)


@when('I open the details of the negotiation')
def ui_details(context):
    # D71: route "/contracts/<nid>" is the details screen.
    browser.goto(context, browser.base_url(context) + "/contracts/" + _nid(context))
    _wait_ready(context)


@when('I open the offer URL of the negotiation')
def ui_offer_url(context):
    # D20, D77: the offerUrl returned by /create (the URL of the notification, D22).
    # Without a /create response in this scenario, the D71 route /contractOffers/<nid>.
    url = None
    resp = getattr(context, "setup_response", None)
    if resp is not None:
        try:
            url = body_json(resp).get("offerUrl")
        except AssertionError:
            url = None
    if not url:
        url = browser.base_url(context) + "/contractOffers/" + _nid(context)
    browser.goto(context, url)
    _wait_ready(context)


# ---------------------------------------------------------------- When: interaction

@when('I click {testid:Q}')
def ui_click(context, testid):
    loc = _loc(context, testid).first
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    expect(loc, "%s: not enabled" % testid).to_be_enabled(timeout=_timeout(context))
    loc.click()
    _settle(context)


@when('I click the button {testid:Q} of the negotiation')
def ui_click_nid(context, testid):
    loc = _row(context).get_by_test_id(_r(context, testid))
    if not loc.count():
        loc = _loc(context, testid)  # the D72 button may sit outside the row element
    loc = loc.first
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    loc.click()
    _settle(context)
    _wait_ready(context)


@when('I type {text:Q} into {testid:Q}')
def ui_type(context, text, testid):
    loc = _loc(context, testid).first
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    _fill(loc, _r(context, text))
    _settle(context)


@when('I select {text:Q} in {testid:Q}')
def ui_select(context, text, testid):
    loc = _loc(context, testid).first
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    _choose(loc, _r(context, text), testid)
    _settle(context)


@when('I filter the overview by {testid:Q} with {text:Q}')
def ui_filter(context, testid, text):
    # D71 filters: negotiation id, resource name, counterparty name (text), status (one value).
    loc = _loc(context, testid).first
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    _choose(loc, _r(context, text), testid)
    _settle(context)


# ---------------------------------------------------------------- Then: elements

@then('the element {testid:Q} is visible')
def ui_visible(context, testid):
    loc = _loc(context, testid).first
    expect(loc, "%s: not visible" % _r(context, testid)).to_be_visible(
        timeout=_timeout(context))


@then('the element {testid:Q} is absent')
def ui_absent(context, testid):
    # D73: actions that are not allowed are not rendered at all.
    _wait_ready(context)
    loc = _loc(context, testid)
    n = loc.count()
    assert n == 0, "%s: expected absent from the DOM (D73), found %d (visible: %s); page %s" % (
        _r(context, testid), n, loc.first.is_visible(), _page(context).url)


@then('the element {testid:Q} shows {text:Q}')
def ui_shows(context, testid, text):
    loc = _loc(context, testid).first
    expected = _r(context, text)
    try:
        expect(loc).to_have_text(_exact(expected), timeout=_timeout(context))
    except AssertionError as exc:
        actual = _norm(loc.inner_text()) if loc.count() else "(element missing)"
        raise AssertionError("%s: expected %r, the browser shows %r" % (
            _r(context, testid), expected, actual)) from exc


@then('the element {testid:Q} shows one of {texts:Q}')
def ui_shows_one_of(context, testid, texts):
    # Digest C5: after an action that starts further work at once, the page shows the
    # current status, which may already be a later one than the action's own status.
    loc = _loc(context, testid).first
    options = [t.strip() for t in _r(context, texts).split(",") if t.strip()]
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    deadline = time.monotonic() + _timeout(context) / 1000.0
    while True:
        actual = _norm(loc.inner_text())
        if any(actual.lower() == _norm(o).lower() for o in options):
            return
        if time.monotonic() > deadline:
            raise AssertionError("%s: expected one of %r, the browser shows %r" % (
                _r(context, testid), options, actual))
        time.sleep(0.25)


@then('the element {testid:Q} contains {text:Q}')
def ui_contains(context, testid, text):
    loc = _loc(context, testid).first
    expected = _r(context, text)
    expect(loc, "%s: not visible" % testid).to_be_visible(timeout=_timeout(context))
    deadline = time.monotonic() + _timeout(context) / 1000.0
    while True:
        actual = _norm(loc.inner_text())
        if _norm(expected).lower() in actual.lower():
            return
        if time.monotonic() >= deadline:
            raise AssertionError("%s: expected to contain %r, the browser shows %r" % (
                _r(context, testid), expected, actual[:600]))
        time.sleep(0.25)


# ---------------------------------------------------------------- Then: overview rows

@then('the row of the negotiation is visible')
def ui_row(context):
    row = _row(context)
    try:
        expect(row.first).to_be_visible(timeout=_timeout(context))
    except AssertionError as exc:
        rows = _page(context).locator('[data-testid^="contract-row-"]').count()
        raise AssertionError("no row contract-row-%s (D72); the overview shows %d rows" % (
            _nid(context), rows)) from exc


@then('the row of the negotiation shows {testid:Q} as {text:Q}')
def ui_row_cell(context, testid, text):
    ui_row(context)
    cell = _row(context).first.get_by_test_id(_r(context, testid)).first
    expected = _r(context, text)
    try:
        expect(cell).to_have_text(_exact(expected), timeout=_timeout(context))
    except AssertionError as exc:
        actual = _norm(cell.inner_text()) if cell.count() else "(cell missing)"
        raise AssertionError("row %s, %s: expected %r, the browser shows %r" % (
            _nid(context), testid, expected, actual)) from exc


@then('the row of the negotiation is absent')
def ui_row_absent(context):
    _wait_ready(context)
    n = _row(context).count()
    assert n == 0, "row contract-row-%s is shown, expected absent" % _nid(context)


@then('the table header shows the columns {columns:Q}')
def ui_header(context, columns):
    # SIMPL-30959 AC1 and AC2 name the columns; matched case-insensitive and
    # whitespace tolerant, in this order. Other columns (an action column) may exist.
    expected = [_norm(c) for c in columns.split(",") if c.strip()]
    pg = _page(context)
    headers = pg.get_by_role("columnheader")
    try:
        expect(headers.first).to_be_visible(timeout=_timeout(context))
    except AssertionError as exc:
        raise AssertionError("no table header row is shown (30959 AC2)") from exc
    actual = [_norm(t) for t in headers.all_inner_texts()]
    actual = [a for a in actual if a]
    pos = 0
    missing = []
    for col in expected:
        pattern = _exact(col)
        idx = next((i for i in range(pos, len(actual)) if pattern.match(actual[i])), None)
        if idx is None:
            missing.append(col)
        else:
            pos = idx + 1
    assert not missing, "header columns %s not found in order; the browser shows %s" % (
        missing, actual)


# ---------------------------------------------------------------- Then: special checks

def _lifecycle_posts(context):
    return [r for r in browser.current(context)["requests"]
            if r["method"] == "POST" and "/lifecycleActions" in r["url"]]


@then('the reason submit is refused while the reason is blank')
def ui_reason_blank(context):
    # D71: "submit is refused in the UI while the reason is blank". Refused means:
    # the submit button is disabled, or pressing it sends no lifecycle action and
    # leaves the dialog open. Checked for an empty and a whitespace-only reason (D39).
    pg = _page(context)
    reason = _loc(context, "reason-input").first
    submit = _loc(context, "reason-submit").first
    expect(reason, "reason-input: not visible").to_be_visible(timeout=_timeout(context))
    expect(submit, "reason-submit: not visible").to_be_visible(timeout=_timeout(context))
    for blank in ("", "   "):
        before = len(_lifecycle_posts(context))
        reason.fill(blank)
        _settle(context)
        if submit.is_disabled():
            continue
        try:
            submit.click(timeout=2000)
        except Exception:  # noqa: BLE001  (not clickable counts as refused)
            continue
        pg.wait_for_timeout(int(context.config_cm.quiet_period * 1000))
        sent = _lifecycle_posts(context)[before:]
        assert not sent, "a blank reason %r was submitted: %s" % (blank, sent)
        assert reason.is_visible(), \
            "the reason dialog closed after submitting a blank reason %r" % blank


@then('the browser is redirected to the consent manager for the negotiation')
def ui_consent_redirect(context):
    # SIMPL-31047 AC2 "redirected to the consent manager"; D43: consentManagerUrl =
    # CONSENT_MANAGER_URL + "?contractNegotiationId=<nid>". The link may open in the
    # same tab or a new one; every page of the browser context is checked.
    target = urlparse(context.config_cm.consent_manager_url)
    session = browser.current(context)
    deadline = time.monotonic() + _timeout(context) / 1000.0
    seen = []
    while True:
        seen = []
        for pg in session["context"].pages:
            u = urlparse(pg.url)
            seen.append(pg.url)
            if (u.scheme, u.netloc, u.path.rstrip("/")) == (
                    target.scheme, target.netloc, target.path.rstrip("/")):
                nids = parse_qs(u.query).get("contractNegotiationId", [])
                if _nid(context) in nids:
                    return
        if time.monotonic() >= deadline:
            raise AssertionError("no page at %s?contractNegotiationId=%s; pages: %s" % (
                context.config_cm.consent_manager_url, _nid(context), seen))
        time.sleep(0.25)


_SANITY_JS = """
el => {
  const bad = [];
  const forbiddenTags = ['SCRIPT','IFRAME','OBJECT','EMBED','LINK','STYLE','BASE','META','FRAME'];
  const all = [el, ...el.querySelectorAll('*')];
  for (const e of all) {
    if (forbiddenTags.includes(e.tagName.toUpperCase())) bad.push('element ' + e.tagName);
    for (const a of Array.from(e.attributes)) {
      const n = a.name.toLowerCase();
      if (n.startsWith('on')) bad.push(e.tagName + ' handler ' + n);
      if (['src','srcset','data','poster','background','formaction','xlink:href'].includes(n)
          && !a.value.trim().toLowerCase().startsWith('data:'))
        bad.push(e.tagName + ' resource ' + n + '=' + a.value);
      if (n === 'href' && a.value.trim().toLowerCase().startsWith('javascript:'))
        bad.push(e.tagName + ' javascript link');
      if (n === 'style' && /url\\s*\\(/i.test(a.value)) bad.push(e.tagName + ' style url()');
    }
  }
  return bad;
}
"""


@then('the human-readable offer shows no script and no external resource')
def ui_sanitised(context):
    # D75: rendered after sanitising (no scripts, no event handlers, no external
    # resources). No dialog may have fired either (the payload calls alert()).
    loc = _loc(context, "human-readable").first
    expect(loc, "human-readable: not visible").to_be_visible(timeout=_timeout(context))
    _settle(context)
    bad = loc.evaluate(_SANITY_JS)
    dialogs = browser.current(context)["dialogs"]
    assert not bad, "the human-readable offer is not sanitised: %s" % bad
    assert not dialogs, "a script ran on the details screen (dialogs: %s)" % dialogs


@then('the only visible element is {testid:Q}')
def ui_only(context, testid):
    # D74: after login, a user without contract_manager sees only access-denied.
    pg = _page(context)
    target = _r(context, testid)
    expect(pg.get_by_test_id(target).first, "%s: not visible" % target).to_be_visible(
        timeout=_timeout(context))
    _settle(context)
    others = []
    for t in D72_TESTIDS:
        if t == target:
            continue
        loc = pg.get_by_test_id(t)
        if loc.count() and loc.first.is_visible():
            others.append(t)
    for prefix in D72_PREFIXES:
        loc = pg.locator('[data-testid^="%s"]' % prefix)
        if loc.count() and loc.first.is_visible():
            others.append(prefix + "*")
    assert not others, "besides %s the browser shows %s" % (target, others)
