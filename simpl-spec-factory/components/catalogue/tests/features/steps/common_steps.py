"""Shared HTTP and assertion steps. Quoted values use the custom parse types Q, QList and Num
registered in environment.py."""
import json
import os
import re

from behave import given, then, when

from support import body as B
from support import client

FIXTURES = os.path.join(os.path.dirname(__file__), "..", "..", "fixtures")
PROBLEM_PREFIX = "urn:problem-type:simpl:"


def _json(context):
    resp = context.response
    assert resp is not None, "no request was sent"
    doc = resp.json()
    assert doc is not None, f"response is not JSON: {resp.summary()}"
    return doc


def _field(context, path):
    doc = _json(context)
    value = B.lookup(doc, B.render(path, context))
    assert not B.missing(value), f"field {path!r} not found in {context.response.summary()}"
    return value


def _value_text(context, text):
    return B.render(text, context)


def _parse_value(text):
    try:
        return json.loads(text)
    except ValueError:
        return text


# ---------------------------------------------------------------- requests

@when('{who:Q} sends {method:w} {path:Q} to {service:Q}')
def step_send(context, who, method, path, service):
    body = B.render(context.text, context) if context.text else None
    client.send(context, who, method.upper(), service, B.render(path, context), raw_body=body)


@when('{who:Q} sends {method:w} {path:Q} to {service:Q} with the content type {ctype:Q}')
def step_send_ct(context, who, method, path, service, ctype):
    body = B.render(context.text, context) if context.text else ""
    client.send(context, who, method.upper(), service, B.render(path, context),
                raw_body=body, content_type=ctype)


@when('{who:Q} sends {method:w} {path:Q} to {service:Q} with the fixture {name:Q}')
def step_send_fixture(context, who, method, path, service, name):
    with open(os.path.join(FIXTURES, "queries", f"{name}.json"), encoding="utf-8") as fh:
        body = B.render(fh.read(), context)
    client.send(context, who, method.upper(), service, B.render(path, context), raw_body=body)


@given('the next request has the header {name:Q} set to {value:Q}')
def step_header(context, name, value):
    context.pending_headers[name] = B.render(value, context)


@given('the next request has no header {name:Q}')
def step_no_header(context, name):
    context.pending_headers[name] = None


@given('the federated request headers for {attrs:Q}')
def step_federated_headers(context, attrs):
    """User-Attributes (comma-separated, may be empty) and a fresh Correlation-Id (D48, D69)."""
    import uuid
    context.vars["correlation"] = str(uuid.uuid4())
    context.pending_headers["User-Attributes"] = attrs
    context.pending_headers["Correlation-Id"] = context.vars["correlation"]


@given('I remember the field {path:Q} as {name:Q}')
@then('I remember the field {path:Q} as {name:Q}')
def step_remember(context, path, name):
    context.vars[name] = _field(context, path)


# ---------------------------------------------------------------- status and problems

@then('the status is {code:d}')
def step_status(context, code):
    resp = context.response
    assert resp is not None, "no request was sent"
    assert resp.status == code, f"expected {code}, got {resp.summary()}"


@then('the status is one of {codes:QList}')
def step_status_one_of(context, codes):
    resp = context.response
    assert str(resp.status) in codes, f"expected one of {codes}, got {resp.summary()}"


@then('the status is not a success')
def step_not_success(context):
    resp = context.response
    assert not (200 <= resp.status < 300), f"expected a non-2xx status, got {resp.summary()}"


def _check_problem(context, code):
    resp = context.response
    assert resp.status == code, f"expected {code}, got {resp.summary()}"
    assert resp.content_type == "application/problem+json", \
        f"expected application/problem+json (D2), got {resp.content_type!r}: {resp.summary()}"
    doc = resp.json()
    assert isinstance(doc, dict), f"problem body is not an object: {resp.summary()}"
    for member in ("type", "title", "status", "detail"):
        assert member in doc, f"problem without {member!r} (D2): {resp.summary()}"
    assert doc["status"] == code, f"problem status {doc['status']} differs from HTTP {code}"
    return doc


@then('the response is a problem with status {code:d}')
def step_problem(context, code):
    _check_problem(context, code)


@then('the response is a problem {ptype:Q} with status {code:d}')
def step_problem_type(context, ptype, code):
    doc = _check_problem(context, code)
    expected = ptype if ptype.startswith("urn:") else PROBLEM_PREFIX + ptype
    assert doc["type"] == expected, f"expected problem type {expected}, got {doc['type']}: {context.response.summary()}"


@then('the problem detail is {text:Q}')
def step_problem_detail(context, text):
    doc = _json(context)
    expected = _value_text(context, text)
    assert doc.get("detail") == expected, f"expected detail {expected!r}, got {doc.get('detail')!r}"


@then('the problem detail contains {text:Q}')
def step_problem_detail_contains(context, text):
    doc = _json(context)
    expected = _value_text(context, text)
    assert expected.lower() in str(doc.get("detail", "")).lower(), \
        f"detail {doc.get('detail')!r} does not mention {expected!r}"


# ---------------------------------------------------------------- body and headers

@then('the response content type is {ctype:Q}')
def step_content_type(context, ctype):
    assert context.response.content_type == ctype, \
        f"expected content type {ctype}, got {context.response.content_type!r}"


@then('the response body contains {text:Q}')
def step_body_contains(context, text):
    expected = _value_text(context, text)
    assert expected in context.response.text, f"body does not contain {expected!r}: {context.response.summary()}"


@then('the response body does not contain {text:Q}')
def step_body_not_contains(context, text):
    expected = _value_text(context, text)
    assert expected not in context.response.text, f"body contains {expected!r}"


@then('the response body is empty')
def step_body_empty(context):
    assert context.response.text.strip() == "", f"expected an empty body: {context.response.summary()}"


@then('the response header {name:Q} is {value:Q}')
def step_header_is(context, name, value):
    expected = _value_text(context, value)
    actual = context.response.headers.get(name)
    assert actual == expected, f"header {name}: expected {expected!r}, got {actual!r}"


@then('the response header {name:Q} contains {value:Q}')
def step_header_contains(context, name, value):
    expected = _value_text(context, value)
    actual = context.response.headers.get(name) or ""
    # A list header (Vary) is split on commas, a URI header (Location) on path segments.
    parts = [p.strip() for p in re.split(r"[,/]", actual)]
    assert expected in parts, f"header {name}: {actual!r} does not list {expected!r}"


@then('the response header {name:Q} does not contain {value:Q}')
def step_header_not_contains(context, name, value):
    expected = _value_text(context, value)
    actual = context.response.headers.get(name) or ""
    assert expected not in [p.strip() for p in actual.split(",")], f"header {name}: {actual!r} lists {expected!r}"


@then('the response header {name:Q} is absent')
def step_header_absent(context, name):
    assert context.response.headers.get(name) is None, \
        f"header {name} present: {context.response.headers.get(name)!r}"


@then('the field {path:Q} is {value:Q}')
def step_field_is(context, path, value):
    actual = _field(context, path)
    expected = _value_text(context, value)
    assert B.same_value(actual, expected), f"field {path}: expected {expected!r}, got {actual!r}"


@then('the field {path:Q} is not {value:Q}')
def step_field_is_not(context, path, value):
    actual = _field(context, path)
    expected = _value_text(context, value)
    assert not B.same_value(actual, expected), f"field {path} is {actual!r}"


@then('the field {path:Q} is null')
def step_field_null(context, path):
    actual = _field(context, path)
    assert actual is None, f"field {path}: expected null, got {actual!r}"


@then('the field {path:Q} is null or absent')
def step_field_null_or_absent(context, path):
    value = B.lookup(_json(context), B.render(path, context))
    assert B.missing(value) or value is None, f"field {path}: expected null or absent, got {value!r}"


@then('the field {path:Q} exists')
def step_field_exists(context, path):
    _field(context, path)


@then('the field {path:Q} does not exist')
def step_field_absent(context, path):
    value = B.lookup(_json(context), B.render(path, context))
    assert B.missing(value), f"field {path} exists: {value!r}"


@then('the field {path:Q} has {n:d} items')
def step_field_len(context, path, n):
    value = _field(context, path)
    assert isinstance(value, list), f"field {path} is not a list: {value!r}"
    assert len(value) == n, f"field {path}: expected {n} items, got {len(value)}"


@then('the field {path:Q} is an empty list')
def step_field_empty_list(context, path):
    value = _field(context, path)
    assert value == [], f"field {path}: expected [], got {value!r}"


@then('the field {path:Q} is a list containing {value:Q}')
def step_field_list_contains(context, path, value):
    actual = _field(context, path)
    expected = _value_text(context, value)
    assert isinstance(actual, list), f"field {path} is not a list: {actual!r}"
    assert any(B.same_value(a, expected) for a in actual), f"field {path}: {actual!r} lacks {expected!r}"


@then('the field {path:Q} is a list not containing {value:Q}')
def step_field_list_not_contains(context, path, value):
    actual = _field(context, path)
    expected = _value_text(context, value)
    assert isinstance(actual, list), f"field {path} is not a list: {actual!r}"
    assert not any(B.same_value(a, expected) for a in actual), f"field {path} contains {expected!r}"


@then('the field {path:Q} is the list {values:QList}')
def step_field_list_equals(context, path, values):
    actual = _field(context, path)
    expected = [_value_text(context, v) for v in values]
    assert isinstance(actual, list) and len(actual) == len(expected) and all(
        B.same_value(a, e) for a, e in zip(actual, expected)), f"field {path}: expected {expected!r}, got {actual!r}"


@then('the field {path:Q} contains the text {text:Q}')
def step_field_contains_text(context, path, text):
    actual = _field(context, path)
    expected = _value_text(context, text)
    assert expected in str(actual), f"field {path}: {actual!r} does not contain {expected!r}"


@then('the field {path:Q} is an integer')
def step_field_int(context, path):
    actual = _field(context, path)
    assert isinstance(actual, int) and not isinstance(actual, bool), f"field {path} is not an integer: {actual!r}"


@then('the field {path:Q} is a number')
def step_field_number(context, path):
    actual = _field(context, path)
    assert isinstance(actual, (int, float)) and not isinstance(actual, bool), f"field {path} is not a number: {actual!r}"


@then('the field {path:Q} is a non-empty string')
def step_field_string(context, path):
    actual = _field(context, path)
    assert isinstance(actual, str) and actual != "", f"field {path} is not a non-empty string: {actual!r}"


@then('the field {path:Q} is a timestamp')
def step_field_timestamp(context, path):
    from datetime import datetime
    actual = _field(context, path)
    assert isinstance(actual, str), f"field {path} is not a string: {actual!r}"
    datetime.fromisoformat(actual.replace("Z", "+00:00"))


@then('the field {path:Q} matches {pattern:Q}')
def step_field_matches(context, path, pattern):
    actual = _field(context, path)
    assert re.fullmatch(pattern, str(actual)), f"field {path}: {actual!r} does not match {pattern}"


@then('the field {path:Q} equals the field {other:Q}')
def step_field_equals_field(context, path, other):
    a, b = _field(context, path), _field(context, other)
    assert a == b, f"field {path} = {a!r} differs from {other} = {b!r}"


@then('the field {path:Q} is the JSON')
def step_field_json(context, path):
    actual = _field(context, path)
    expected = json.loads(B.render(context.text, context))
    assert actual == expected, f"field {path}: expected {expected!r}, got {actual!r}"


@then('the response is a JSON object without the member {member:Q}')
def step_no_member(context, member):
    doc = _json(context)
    assert isinstance(doc, dict) and member not in doc, f"member {member!r} present"


@then('the response members are exactly {members:QList}')
def step_members_exactly(context, members):
    doc = _json(context)
    assert isinstance(doc, dict), f"not an object: {context.response.summary()}"
    assert sorted(doc.keys()) == sorted(members), f"members {sorted(doc.keys())} differ from {sorted(members)}"


# ---------------------------------------------------------------- search results

def _items(context):
    return B.result_items(_json(context))


def _rd(context, alias):
    assert alias in context.rds, f"unknown RD alias {alias!r}"
    return context.rds[alias]


@then('the result contains {alias:Q}')
def step_result_contains(context, alias):
    rd = _rd(context, alias)
    assert B.find_item(_items(context), rd.id), f"{alias} ({rd.id}) not in result: {context.response.summary()}"


@then('the result does not contain {alias:Q}')
def step_result_not_contains(context, alias):
    rd = _rd(context, alias)
    assert not B.find_item(_items(context), rd.id), f"{alias} ({rd.id}) unexpectedly in result"


@then('the result contains exactly {aliases:QList} of this scenario')
def step_result_exactly(context, aliases):
    items = _items(context)
    present = sorted(a for a, rd in context.rds.items() if B.find_item(items, rd.id))
    assert present == sorted(aliases), f"expected {sorted(aliases)}, found {present}"


@then('the result lists {aliases:QList} in this order')
def step_result_order(context, aliases):
    items = _items(context)
    positions = []
    for alias in aliases:
        rd = _rd(context, alias)
        idx = next((i for i, item in enumerate(items) if rd.id in B.item_ids(item)), None)
        assert idx is not None, f"{alias} not in result"
        positions.append(idx)
    assert positions == sorted(positions), f"order of {aliases} is {positions}"


@then('the result has {n:d} items')
def step_result_count(context, n):
    items = _items(context)
    assert len(items) == n, f"expected {n} items, got {len(items)}"


def _result_field(context, alias, path):
    rd = _rd(context, alias)
    item = B.find_item(_items(context), rd.id)
    assert item is not None, f"{alias} not in result: {context.response.summary()}"
    path = B.render(path, context)
    value = B.lookup(B.item_payload(item), path)
    if B.missing(value) and isinstance(item.get("meta"), dict):
        # list items (GET /self-descriptions) carry the fields inside "meta"
        value = B.lookup(item["meta"], path)
    return value


@then('the result {alias:Q} has the field {path:Q} set to {value:Q}')
def step_result_field(context, alias, path, value):
    actual = _result_field(context, alias, path)
    expected = _value_text(context, value)
    assert not B.missing(actual), f"{alias}: field {path} missing"
    assert B.same_value(actual, expected), f"{alias}: field {path} expected {expected!r}, got {actual!r}"


@then('the result {alias:Q} has the field {path:Q} null')
def step_result_field_null(context, alias, path):
    actual = _result_field(context, alias, path)
    assert not B.missing(actual) and actual is None, f"{alias}: field {path} expected null, got {actual!r}"


@then('the result {alias:Q} has the field {path:Q} as the list {values:QList}')
def step_result_field_list(context, alias, path, values):
    actual = _result_field(context, alias, path)
    expected = [_value_text(context, v) for v in values]
    assert actual == expected, f"{alias}: field {path} expected {expected!r}, got {actual!r}"


# ---------------------------------------------------------------- grouped results (cbe, 6.2)

def _group(context, catalogue_id):
    doc = _json(context)
    groups = doc.get("groups") if isinstance(doc, dict) else None
    assert isinstance(groups, list), f"no groups in {context.response.summary()}"
    for g in groups:
        if g.get("catalogueId") == catalogue_id:
            return g
    raise AssertionError(f"no group {catalogue_id}; groups: {[g.get('catalogueId') for g in groups]}")


@then('the groups are {ids:QList} in this order')
def step_groups_order(context, ids):
    doc = _json(context)
    actual = [g.get("catalogueId") for g in doc.get("groups", [])]
    assert actual == ids, f"expected groups {ids}, got {actual}"


@then('the group {cid:Q} contains {alias:Q}')
def step_group_contains(context, cid, alias):
    rd = _rd(context, alias)
    assert B.find_item(_group(context, cid).get("items", []), rd.id), f"{alias} not in group {cid}"


@then('the group {cid:Q} does not contain {alias:Q}')
def step_group_not_contains(context, cid, alias):
    rd = _rd(context, alias)
    assert not B.find_item(_group(context, cid).get("items", []), rd.id), f"{alias} in group {cid}"


@then('the group {cid:Q} has the field {path:Q} set to {value:Q}')
def step_group_field(context, cid, path, value):
    actual = B.lookup(_group(context, cid), B.render(path, context))
    expected = _value_text(context, value)
    assert not B.missing(actual), f"group {cid}: field {path} missing"
    assert B.same_value(actual, expected), f"group {cid}: field {path} expected {expected!r}, got {actual!r}"


@then('the group {cid:Q} has the field {path:Q} null')
def step_group_field_null(context, cid, path):
    actual = B.lookup(_group(context, cid), B.render(path, context))
    assert not B.missing(actual) and actual is None, f"group {cid}: field {path} expected null, got {actual!r}"


@then('the group {cid:Q} has {n:d} items')
def step_group_count(context, cid, n):
    items = _group(context, cid).get("items")
    assert isinstance(items, list) and len(items) == n, f"group {cid}: expected {n} items, got {items!r}"


@then('the group {cid:Q} result {alias:Q} has the field {path:Q} set to {value:Q}')
def step_group_result_field(context, cid, alias, path, value):
    rd = _rd(context, alias)
    item = B.find_item(_group(context, cid).get("items", []), rd.id)
    assert item is not None, f"{alias} not in group {cid}"
    actual = B.lookup(B.item_payload(item), B.render(path, context))
    expected = _value_text(context, value)
    assert not B.missing(actual) and B.same_value(actual, expected), \
        f"group {cid} {alias}: field {path} expected {expected!r}, got {actual!r}"


@then('the group {cid:Q} result {alias:Q} has the field {path:Q} null')
def step_group_result_field_null(context, cid, alias, path):
    rd = _rd(context, alias)
    item = B.find_item(_group(context, cid).get("items", []), rd.id)
    assert item is not None, f"{alias} not in group {cid}"
    actual = B.lookup(B.item_payload(item), B.render(path, context))
    assert not B.missing(actual) and actual is None, f"group {cid} {alias}: field {path} expected null, got {actual!r}"


@then('the top-level totals and items add up over the groups')
def step_groups_add_up(context):
    doc = _json(context)
    groups = doc.get("groups", [])
    assert doc.get("totalCount") == sum(g.get("total", 0) for g in groups), \
        f"totalCount {doc.get('totalCount')} is not the sum of group totals"
    flat = [item for g in groups for item in g.get("items", [])]
    assert doc.get("items") == flat, "top-level items are not the concatenation of the group pages"


@when('the health of {service:Q} is requested without a token')
def step_health(context, service):
    """D3 and 5.1 say GET /health on every service. For the /v1 based services the digest does
    not say whether that is the port root or under /v1, so the root is tried first, then /v1."""
    import time
    import requests as _rq
    root = context.cfg.health_roots[service]
    candidates = [root + "/health"]
    if context.cfg.services.get(service, "").endswith("/v1") or service in ("qma-a", "qma-b", "qma-c", "cbe-a"):
        candidates.append(root + "/v1/health")
    for url in candidates:
        start = time.monotonic()
        try:
            raw = _rq.get(url, timeout=context.cfg.http_timeout)
        except _rq.RequestException as exc:
            raise AssertionError(f"GET {url} failed: {exc}") from exc
        context.response = client.Response(raw, time.monotonic() - start)
        if raw.status_code == 200:
            return


@then('the result {alias:Q} has the field {path:Q} different from {value:Q}')
def step_result_field_differs(context, alias, path, value):
    actual = _result_field(context, alias, path)
    expected = _value_text(context, value)
    assert not B.missing(actual), f"{alias}: field {path} missing"
    assert not B.same_value(actual, expected), f"{alias}: field {path} is still {actual!r}"


@given('more than one second has passed')
def step_wait_second(context):
    import time
    time.sleep(1.1)


@then('the request took less than {seconds:d} seconds')
def step_took_less(context, seconds):
    assert context.response.elapsed < seconds, f"request took {context.response.elapsed:.1f} s"


@then('the request took at least {seconds:d} seconds')
def step_took_at_least(context, seconds):
    assert context.response.elapsed >= seconds, f"request took only {context.response.elapsed:.1f} s"
