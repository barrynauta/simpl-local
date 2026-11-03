"""Generic HTTP request and response steps."""
import json
import re

from behave import step

import param_types  # noqa: F401  (registers the custom parse types)
from cmtest.checks import _store, check_problem, compare
from cmtest.core import (body_json, get_path, has_path, parse_datetime,
                         render, render_json, sha256_hex)

def _body_for(method, text, context):
    if text is not None:
        return render_json(text, context.vars)
    return {} if method in ("POST", "PUT", "PATCH") else None


# ------------------------------------------------------------------ CM

@step('I send {method:Method} to the {party:Party} CM at {path:Q}')
def send_cm(context, method, party, path):
    _store(context, context.world.cm(party, method, render(path, context.vars)))


@step('I send {method:Method} to the {party:Party} CM at {path:Q} with JSON')
def send_cm_json(context, method, party, path):
    _store(context, context.world.cm(party, method, render(path, context.vars),
                                     json_body=render_json(context.text, context.vars)))


@step('I send {method:Method} to the {party:Party} CM at {path:Q} without an API key')
def send_cm_nokey(context, method, party, path):
    _store(context, context.world.cm(party, method, render(path, context.vars),
                                     json_body=_body_for(method, None, context),
                                     api_key=None))


@step('I send {method:Method} to the {party:Party} CM at {path:Q} with the API key {key:Q}')
def send_cm_key(context, method, party, path, key):
    _store(context, context.world.cm(party, method, render(path, context.vars),
                                     json_body=_body_for(method, None, context),
                                     api_key=key))


@step('I send {method:Method} to the {party:Party} CM at {path:Q} with content type {ct:Q} and body {body:Q}')
def send_cm_ct(context, method, party, path, ct, body):
    _store(context, context.world.cm(party, method, render(path, context.vars),
                                     headers={"Content-Type": ct},
                                     data=render(body, context.vars).encode("utf-8")))


@step('I send {method:Method} to the {party:Party} CM at {path:Q} with header {name:Q} set to {value:Q}')
def send_cm_header(context, method, party, path, name, value):
    _store(context, context.world.cm(party, method, render(path, context.vars),
                                     json_body=_body_for(method, None, context),
                                     headers={name: render(value, context.vars)}))


@step('I send {method:Method} to the {party:Party} CM at {path:Q} with header {name:Q} set to {value:Q} and JSON')
def send_cm_header_json(context, method, party, path, name, value):
    _store(context, context.world.cm(party, method, render(path, context.vars),
                                     json_body=render_json(context.text, context.vars),
                                     headers={name: render(value, context.vars)}))


# ------------------------------------------------------------------ UIBE

@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} as {user:Q}')
def send_uibe_user(context, method, party, path, user):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=_body_for(method, None, context),
                                       user=user))


@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} as {user:Q} with JSON')
def send_uibe_user_json(context, method, party, path, user):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=render_json(context.text, context.vars),
                                       user=user))


@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} without credentials')
def send_uibe_none(context, method, party, path):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=_body_for(method, None, context),
                                       user=None))


@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} with the UIBE API key')
def send_uibe_key(context, method, party, path):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=_body_for(method, None, context),
                                       user=None, api_key="default"))


@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} with the UIBE API key and JSON')
def send_uibe_key_json(context, method, party, path):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=render_json(context.text, context.vars),
                                       user=None, api_key="default"))


@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} with the API key {key:Q} and JSON')
def send_uibe_wrongkey_json(context, method, party, path, key):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=render_json(context.text, context.vars),
                                       user=None, api_key=key))


@step('I send {method:Method} to the {party:Party} UIBE at {path:Q} with a malformed token')
def send_uibe_badtoken(context, method, party, path):
    _store(context, context.world.uibe(party, method, render(path, context.vars),
                                       json_body=_body_for(method, None, context),
                                       user=None,
                                       headers={"Authorization": "Bearer not-a-jwt"}))


# ------------------------------------------------------------------ stub

@step('I send {method:Method} to the stub at {path:Q}')
def send_stub(context, method, path):
    _store(context, context.world.stub(method, render(path, context.vars)))


# ------------------------------------------------------------------ checks

def _json(context):
    return body_json(context.response)


@step('the response status is {code:d}')
def status_is(context, code):
    resp = context.response
    assert resp.status_code == code, "expected %d, got %d: %s" % (
        code, resp.status_code, resp.text[:800])


@step('the response status is one of {codes:Q}')
def status_one_of(context, codes):
    allowed = [int(c) for c in codes.split(",")]
    assert context.response.status_code in allowed, "expected one of %s, got %d: %s" % (
        allowed, context.response.status_code, context.response.text[:500])


@step('the response is a problem with status {code:d} and type {short:Q}')
def problem(context, code, short):
    check_problem(context.response, code, short)


@step('the response is a problem with status {code:d}')
def problem_any(context, code):
    check_problem(context.response, code, None)


@step('the problem detail contains {text:Q}')
def problem_detail(context, text):
    detail = _json(context).get("detail") or ""
    assert render(text, context.vars).lower() in detail.lower(), \
        "detail %r does not contain %r" % (detail, text)


@step('the response field {path:Q} is {value:Q}')
def field_is(context, path, value):
    data = _json(context)
    assert has_path(data, path), "field %s missing in %s" % (path, json.dumps(data)[:800])
    actual = get_path(data, path)
    expected = render(value, context.vars)
    assert compare(actual, expected), "field %s is %r, expected %r" % (path, actual, expected)


@step('the response fields are')
def fields_are(context):
    for row in context.table:
        field_is(context, row["field"], row["value"])


@step('the response field {path:Q} is present')
def field_present(context, path):
    data = _json(context)
    assert has_path(data, path), "field %s missing in %s" % (path, json.dumps(data)[:800])


@step('the response field {path:Q} is absent')
def field_absent(context, path):
    data = _json(context)
    assert not has_path(data, path), "field %s should be absent: %s" % (
        path, json.dumps(data)[:800])


@step('the response field {path:Q} is not null')
def field_not_null(context, path):
    data = _json(context)
    assert has_path(data, path) and get_path(data, path) is not None, \
        "field %s is missing or null in %s" % (path, json.dumps(data)[:800])


@step('the response field {path:Q} matches {regex:Q}')
def field_matches(context, path, regex):
    actual = get_path(_json(context), path)
    assert isinstance(actual, str) and re.fullmatch(render(regex, context.vars), actual), \
        "field %s = %r does not match %r" % (path, actual, regex)


@step('the response field {path:Q} contains {text:Q}')
def field_contains(context, path, text):
    actual = get_path(_json(context), path)
    expected = render(text, context.vars)
    if isinstance(actual, list):
        assert any(compare(a, expected) for a in actual), "%s %r lacks %r" % (path, actual, expected)
    else:
        assert expected in str(actual), "%s %r does not contain %r" % (path, actual, expected)


@step('the response field {path:Q} does not contain {text:Q}')
def field_not_contains(context, path, text):
    actual = get_path(_json(context), path)
    expected = render(text, context.vars)
    if isinstance(actual, list):
        assert not any(compare(a, expected) for a in actual), "%s %r has %r" % (path, actual, expected)
    else:
        assert expected not in str(actual), "%s %r contains %r" % (path, actual, expected)


@step('the response field {path:Q} is an empty list')
def field_empty_list(context, path):
    actual = get_path(_json(context), path)
    assert actual == [], "%s is %r, expected []" % (path, actual)


@step('the response field {path:Q} is a date-time')
def field_datetime(context, path):
    actual = get_path(_json(context), path)
    assert isinstance(actual, str), "%s is %r" % (path, actual)
    parse_datetime(actual)


@step('the response field {path:Q} is an OpenAPI local date-time')
def field_local_datetime(context, path):
    actual = get_path(_json(context), path)
    assert isinstance(actual, str) and re.fullmatch(
        r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?", actual), \
        "%s = %r is not an ISO-8601 local date-time without offset (D15)" % (path, actual)


@step('the response field {path:Q} is the SHA-256 of the response field {other:Q}')
def field_is_sha(context, path, other):
    data = _json(context)
    expected = sha256_hex(get_path(data, other))
    assert get_path(data, path) == expected, "%s = %r, expected sha256 %r" % (
        path, get_path(data, path), expected)


@step('the response is a JSON list')
def is_list(context):
    assert isinstance(_json(context), list), "expected a JSON array: %s" % context.response.text[:300]


def _list_at(context, path):
    data = _json(context)
    items = data if path in ("", ".") else get_path(data, path)
    assert isinstance(items, list), "%s is not a list: %r" % (path, items)
    return items


@step('the response list {path:Q} contains an item with {field:Q} {value:Q}')
def list_contains(context, path, field, value):
    items = _list_at(context, path)
    expected = render(value, context.vars)
    assert any(isinstance(i, dict) and field in i and compare(i[field], expected)
               for i in items), "no item with %s=%r in %s" % (field, expected,
                                                               json.dumps(items)[:800])


@step('the response list {path:Q} does not contain an item with {field:Q} {value:Q}')
def list_not_contains(context, path, field, value):
    items = _list_at(context, path)
    expected = render(value, context.vars)
    assert not any(isinstance(i, dict) and field in i and compare(i[field], expected)
                   for i in items), "unexpected item with %s=%r" % (field, expected)


@step('the response list {path:Q} has {n:d} items')
def list_len(context, path, n):
    items = _list_at(context, path)
    assert len(items) == n, "%s has %d items, expected %d" % (path, len(items), n)


@step('the item of the response list {path:Q} with {field:Q} {value:Q} has')
def list_item_has(context, path, field, value):
    items = _list_at(context, path)
    expected = render(value, context.vars)
    match = [i for i in items if isinstance(i, dict) and compare(i.get(field), expected)]
    assert match, "no item with %s=%r" % (field, expected)
    for row in context.table:
        assert has_path(match[0], row["field"]), "item lacks %s: %s" % (row["field"], match[0])
        actual = get_path(match[0], row["field"])
        exp = render(row["value"], context.vars)
        assert compare(actual, exp), "item %s is %r, expected %r" % (row["field"], actual, exp)


@step('the response list {path:Q} is sorted by {field:Q} descending')
def list_sorted_desc(context, path, field):
    items = _list_at(context, path)
    values = [parse_datetime(i[field]) for i in items]
    assert values == sorted(values, reverse=True), "not sorted by %s descending" % field


@step('every item of the response list {path:Q} has {field:Q} {value:Q}')
def list_every(context, path, field, value):
    items = _list_at(context, path)
    expected = render(value, context.vars)
    bad = [i for i in items if not compare(i.get(field), expected)]
    assert not bad, "items with %s != %r: %s" % (field, expected, json.dumps(bad)[:500])


@step('every item of the response list {path:Q} has {field:Q} containing {value:Q} ignoring case')
def list_every_contains(context, path, field, value):
    items = _list_at(context, path)
    expected = render(value, context.vars).lower()
    bad = [i for i in items if expected not in str(i.get(field, "")).lower()]
    assert not bad, "items whose %s lacks %r: %s" % (field, expected, json.dumps(bad)[:500])


@step('the response content type is {ct:Q}')
def content_type(context, ct):
    actual = context.response.headers.get("Content-Type", "")
    assert actual.startswith(ct), "content type %r, expected %r" % (actual, ct)


@step('the response header {name:Q} is {value:Q}')
def header_is(context, name, value):
    actual = context.response.headers.get(name)
    expected = render(value, context.vars)
    assert actual == expected, "header %s = %r, expected %r" % (name, actual, expected)


@step('the response header {name:Q} is a UUID')
def header_uuid(context, name):
    actual = context.response.headers.get(name) or ""
    assert re.fullmatch(r"[0-9a-fA-F-]{36}", actual), "header %s = %r is not a UUID" % (name, actual)


@step('the response body contains {text:Q}')
def body_contains(context, text):
    expected = render(text, context.vars)
    assert expected in context.response.text, "body lacks %r: %s" % (expected, context.response.text[:500])


@step('the response body does not contain {text:Q}')
def body_not_contains(context, text):
    expected = render(text, context.vars)
    assert expected not in context.response.text, "body contains %r" % expected


@step('I remember the response field {path:Q} as {name:Q}')
def remember(context, path, name):
    context.vars[name] = get_path(_json(context), path)


@step('the response body is the text')
def body_is_text(context):
    expected = render(context.text, context.vars)
    actual = context.response.text
    assert actual == expected, "body differs:\n--- actual\n%r\n--- expected\n%r" % (actual, expected)


@step('the response body lines match')
def body_lines_match(context):
    lines = context.response.text.split("\n")
    rows = list(context.table)
    assert len(lines) == len(rows), "expected %d lines, got %d: %r" % (
        len(rows), len(lines), context.response.text)
    for line, row in zip(lines, rows):
        pattern = render(row["pattern"], context.vars)
        assert re.fullmatch(pattern, line), "line %r does not match %r" % (line, pattern)


@step('the response field {path:Q} is absent or null')
def field_absent_or_null(context, path):
    data = _json(context)
    assert not has_path(data, path) or get_path(data, path) is None, \
        "field %s should be absent or null: %r" % (path, get_path(data, path))
