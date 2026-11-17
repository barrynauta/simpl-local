"""Shared API step library for the Schema Manager suite.

Aliases: `$A`, `$B`, `$S` ... are schema names, `$RT...` resource types,
`$VOC...` vocabulary names, `$NS` the local vocabulary namespace base and `$DP`
the default property namespace. Every alias becomes a unique value per run and
per scenario (D61). Quoted values use the custom parse type `Str`.
"""

import concurrent.futures
import json
import os
import re
import sys
import threading
import time
import uuid
from datetime import datetime

import parse
import requests
from behave import given, register_type, then, when

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import smlib  # noqa: E402


@parse.with_pattern(r'[^"]*')
def _str(text):
    return text


@parse.with_pattern(r"\d+")
def _int(text):
    return int(text)


@parse.with_pattern(r"RESOURCE|SEARCH")
def _stype(text):
    return text


@parse.with_pattern(r"ga-admin|ga-viewer|ga-norole")
def _user(text):
    return text


@parse.with_pattern(r"[^']*")
def _sq(text):
    return text


register_type(Str=_str, SQ=_sq, Int=_int, SchemaType=_stype, User=_user)

PROBLEM = "application/problem+json"
UUID_RE = re.compile(r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
OMIT = "<omit>"


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def S(ctx, text):
    return ctx.names.sub(text)


def auth_headers(ctx, user=None):
    if user is not None:
        return {"Authorization": f"Bearer {ctx.tokens.token(user)}"}
    mode, user = ctx.auth, ctx.user
    if mode == "none":
        return {}
    if mode == "garbage":
        return {"Authorization": "Bearer not-a-jwt"}
    if mode == "tampered":
        return {"Authorization": f"Bearer {ctx.tokens.tampered(user)}"}
    if mode == "unsigned":
        return {"Authorization": f"Bearer {smlib.self_made_token(user, alg='none')}"}
    if mode == "foreign":
        return {"Authorization": f"Bearer {smlib.self_made_token(user, secret=b'another-realm-key')}"}
    if mode == "expired":
        return {"Authorization": f"Bearer {smlib.self_made_token(user, expired=True)}"}
    return {"Authorization": f"Bearer {ctx.tokens.token(user)}"}


def call(ctx, method, path, user=None, headers=None, store=True, absolute=False, **kw):
    url = S(ctx, path) if absolute else ctx.cfg.sm_base_url + S(ctx, path)
    h = auth_headers(ctx, user)
    h.update(headers or {})
    resp = requests.request(method, url, headers=h, timeout=ctx.cfg.http_timeout, **kw)
    if store:
        ctx.response = resp
    return resp


def concept(ctx, alias):
    return ctx.concepts.get(alias, {"type": "RESOURCE", "rt": None})


def type_query(ctx, alias, first=True):
    if concept(ctx, alias)["type"] == "SEARCH":
        return ("?" if first else "&") + "schemaType=SEARCH"
    return ""


def multipart(fields, file_name, file_bytes):
    parts = [(k, (None, v)) for k, v in fields.items()]
    if file_bytes is not None:
        parts.append(("schemaFile", (file_name, file_bytes, "text/turtle")))
    return parts


def apply_overrides(ctx, fields, table):
    file_name = None
    if table is None:
        return fields, file_name
    for row in table:
        key, value = row["field"], row["value"]
        if key == "schemaFile":
            file_name = value
            continue
        if value == OMIT:
            fields.pop(key, None)
        else:
            fields[key] = S(ctx, value)
    return fields, file_name


def do_create(ctx, alias, stype, rt=None, fixture=None, table=None, user=None, store=True):
    name = S(ctx, alias)
    fields = {"name": name, "title": f"Title of {name}", "description": f"Description of {name}"}
    if stype == "SEARCH":
        fields["schemaType"] = "SEARCH"
    if rt is not None:
        fields["resourceType"] = S(ctx, rt)
    fields, override_file = apply_overrides(ctx, fields, table)
    fixture = override_file or fixture or ("valid-search.ttl" if stype == "SEARCH" else "valid-resource.ttl")
    data = None if fixture == OMIT else smlib.fixture_bytes(fixture, ctx.names)
    resp = call(ctx, "POST", "/schemas", user=user, store=store, files=multipart(fields, fixture, data))
    if resp.status_code == 201:
        real_type = fields.get("schemaType", "RESOURCE")
        ctx.concepts[alias] = {"type": real_type, "rt": fields.get("resourceType")}
        ctx.uploads[(alias, real_type, "1")] = data
    return resp


def do_new_version(ctx, alias, fixture=None, table=None, user=None, store=True):
    c = concept(ctx, alias)
    count = len([k for k in ctx.uploads if k[0] == alias and k[1] == c["type"]]) + 1
    fields = {"changelog": f"Change number {count} of {S(ctx, alias)}"}
    if c["type"] == "SEARCH":
        fields["schemaType"] = "SEARCH"
    fields, override_file = apply_overrides(ctx, fields, table)
    fixture = override_file or fixture or ("valid-search-v2.ttl" if c["type"] == "SEARCH" else "valid-resource-v2.ttl")
    data = None if fixture == OMIT else smlib.fixture_bytes(fixture, ctx.names)
    resp = call(ctx, "POST", f"/schemas/{S(ctx, alias)}/versions", user=user, store=store,
                files=multipart(fields, fixture, data))
    if resp.status_code == 201:
        try:
            version = str(resp.json().get("version"))
        except ValueError:
            version = str(count)
        ctx.uploads[(alias, c["type"], version)] = data
        ctx.last_changelog = fields.get("changelog")
    return resp


def do_set_status(ctx, alias, status, user=None, store=True):
    return call(ctx, "PATCH", f"/schemas/{S(ctx, alias)}{type_query(ctx, alias)}", user=user, store=store,
                json={"status": status})


def must(resp, code, what):
    assert resp.status_code == code, f"{what}: expected {code}, got {resp.status_code}: {resp.text[:500]}"


def list_all(ctx, query):
    items, page = [], 1
    while page <= 500:
        sep = "&" if query else ""
        resp = call(ctx, "GET", f"/schemas?{S(ctx, query)}{sep}page={page}&pageSize=100", user="ga-admin",
                    store=False)
        must(resp, 200, f"GET /schemas?{query}")
        body = resp.json()
        items.extend(body.get("metadata") or [])
        if not body.get("next") or page >= int(body.get("totalPages") or 0):
            break
        page += 1
    return items


def versions_all(ctx, alias, stype=None):
    stype = stype or concept(ctx, alias)["type"]
    items, page = [], 1
    while page <= 100:
        resp = call(ctx, "GET", f"/schemas/{S(ctx, alias)}/versions?schemaType={stype}&page={page}&pageSize=100",
                    user="ga-admin", store=False)
        must(resp, 200, "GET versions")
        body = resp.json()
        items.extend(body.get("metadata") or [])
        if not body.get("next") or page >= int(body.get("totalPages") or 0):
            break
        page += 1
    return items


def find_schema(ctx, alias, stype=None):
    c = concept(ctx, alias)
    stype = stype or c["type"]
    query = f"schemaType={stype}"
    if stype == "RESOURCE" and c.get("rt"):
        query += f"&resourceType={c['rt']}"
    name = S(ctx, alias)
    matches = [i for i in list_all(ctx, query) if i.get("name") == name]
    return matches[0] if matches else None


def json_body(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError(f"Response is not JSON: {resp.status_code} {resp.text[:300]}")


def get_path(data, path):
    cur = data
    for part in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if part.startswith("["):
            idx = int(part[1:-1])
            assert isinstance(cur, list) and len(cur) > idx, f"{path}: no item {idx} in {cur!r}"
            cur = cur[idx]
        else:
            assert isinstance(cur, dict) and part in cur, f"{path}: field '{part}' missing in {str(cur)[:300]}"
            cur = cur[part]
    return cur


def is_iso_timestamp(value):
    if not isinstance(value, str):
        return False
    try:
        datetime.fromisoformat(value.replace("Z", "+00:00"))
        return True
    except ValueError:
        return False


# ---------------------------------------------------------------------------
# identity
# ---------------------------------------------------------------------------

@given('I am "{user:User}"')
def i_am(ctx, user):
    ctx.user, ctx.auth = user, "user"


@given("I am not authenticated")
def not_authenticated(ctx):
    ctx.auth = "none"


@given('I send the malformed bearer token "not-a-jwt"')
def garbage_token(ctx):
    ctx.auth = "garbage"


@given('I am "{user:User}" with a token whose signature was altered')
def tampered_token(ctx, user):
    ctx.user, ctx.auth = user, "tampered"


@given('I am "{user:User}" with an unsigned token')
def unsigned_token(ctx, user):
    ctx.user, ctx.auth = user, "unsigned"


@given('I am "{user:User}" with a token signed by another issuer')
def foreign_token(ctx, user):
    ctx.user, ctx.auth = user, "foreign"


@given('I am "{user:User}" with an expired token')
def expired_token(ctx, user):
    ctx.user, ctx.auth = user, "expired"


# ---------------------------------------------------------------------------
# setup (always as ga-admin, success asserted)
# ---------------------------------------------------------------------------

@given('the RESOURCE schema "{alias:Str}" exists for resource type "{rt:Str}"')
def given_resource(ctx, alias, rt):
    must(do_create(ctx, alias, "RESOURCE", rt, user="ga-admin", store=False), 201, f"create {alias}")


@given('the RESOURCE schema "{alias:Str}" exists for resource type "{rt:Str}" from "{fixture:Str}"')
def given_resource_from(ctx, alias, rt, fixture):
    must(do_create(ctx, alias, "RESOURCE", rt, fixture=fixture, user="ga-admin", store=False), 201,
         f"create {alias}")


@given('the SEARCH schema "{alias:Str}" exists')
def given_search(ctx, alias):
    must(do_create(ctx, alias, "SEARCH", user="ga-admin", store=False), 201, f"create SEARCH {alias}")


@given('schema "{alias:Str}" has a new version')
def given_new_version(ctx, alias):
    must(do_new_version(ctx, alias, user="ga-admin", store=False), 201, f"new version of {alias}")


@given('schema "{alias:Str}" has {n:Int} new versions')
def given_n_versions(ctx, alias, n):
    for _ in range(n):
        must(do_new_version(ctx, alias, user="ga-admin", store=False), 201, f"new version of {alias}")


@given('schema "{alias:Str}" has been revoked')
def given_revoked(ctx, alias):
    must(do_set_status(ctx, alias, "REVOKED", user="ga-admin", store=False), 200, f"revoke {alias}")


@given('schema "{alias:Str}" has been published')
def given_published(ctx, alias):
    must(do_set_status(ctx, alias, "PUBLISHED", user="ga-admin", store=False), 200, f"publish {alias}")


@given("no SEARCH schema is published")
def no_search_published(ctx):
    for item in list_all(ctx, "schemaType=SEARCH&status=PUBLISHED"):
        resp = call(ctx, "PATCH", f"/schemas/{item['name']}?schemaType=SEARCH", user="ga-admin", store=False,
                    json={"status": "REVOKED"})
        must(resp, 200, f"revoke leftover SEARCH schema {item['name']}")
    assert not list_all(ctx, "schemaType=SEARCH&status=PUBLISHED"), "a SEARCH schema is still PUBLISHED"


@given('the vocabulary "{voc:Str}" defines the terms "{terms:Str}"')
def seed_vocab(ctx, voc, terms):
    name = S(ctx, voc)
    lines = ["@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .", ""]
    for term in [t.strip() for t in terms.split(",") if t.strip()]:
        lines.append(f"<{ctx.cfg.vocabulary_namespace_base}{name}#{term}> a rdfs:Class ; "
                     f'rdfs:label "{term}" .')
    smlib.seed_vocabulary(ctx.cfg, name, "\n".join(lines) + "\n")


# ---------------------------------------------------------------------------
# schema actions (current identity, response stored)
# ---------------------------------------------------------------------------

@when('I create the RESOURCE schema "{alias:Str}" for resource type "{rt:Str}"')
def create_resource(ctx, alias, rt):
    do_create(ctx, alias, "RESOURCE", rt)


@when('I create the RESOURCE schema "{alias:Str}" for resource type "{rt:Str}" from "{fixture:Str}"')
def create_resource_from(ctx, alias, rt, fixture):
    do_create(ctx, alias, "RESOURCE", rt, fixture=fixture)


@when('I create the RESOURCE schema "{alias:Str}" for resource type "{rt:Str}" with')
def create_resource_with(ctx, alias, rt):
    do_create(ctx, alias, "RESOURCE", rt, table=ctx.table)


@when('I create the RESOURCE schema "{alias:Str}" without a resource type')
def create_resource_no_rt(ctx, alias):
    do_create(ctx, alias, "RESOURCE", None)


@when('I create the SEARCH schema "{alias:Str}"')
def create_search(ctx, alias):
    do_create(ctx, alias, "SEARCH")


@when('I create the SEARCH schema "{alias:Str}" from "{fixture:Str}"')
def create_search_from(ctx, alias, fixture):
    do_create(ctx, alias, "SEARCH", fixture=fixture)


@when('I create the SEARCH schema "{alias:Str}" with')
def create_search_with(ctx, alias):
    do_create(ctx, alias, "SEARCH", table=ctx.table)


@when('I add a new version to schema "{alias:Str}"')
def add_version(ctx, alias):
    do_new_version(ctx, alias)


@when('I add a new version to schema "{alias:Str}" from "{fixture:Str}"')
def add_version_from(ctx, alias, fixture):
    do_new_version(ctx, alias, fixture=fixture)


@when('I add a new version to schema "{alias:Str}" with')
def add_version_with(ctx, alias):
    do_new_version(ctx, alias, table=ctx.table)


@when('I add a new version to the unknown schema "{name:Str}" with schema type "{stype:SchemaType}"')
def add_version_unknown(ctx, name, stype):
    fields = {"changelog": "A change."}
    if stype == "SEARCH":
        fields["schemaType"] = "SEARCH"
    fixture = "valid-search.ttl" if stype == "SEARCH" else "valid-resource.ttl"
    call(ctx, "POST", f"/schemas/{S(ctx, name)}/versions",
         files=multipart(fields, fixture, smlib.fixture_bytes(fixture, ctx.names)))


@when('I revoke schema "{alias:Str}"')
def revoke(ctx, alias):
    do_set_status(ctx, alias, "REVOKED")


@when('I publish schema "{alias:Str}"')
def publish(ctx, alias):
    do_set_status(ctx, alias, "PUBLISHED")


@when('I set the status of schema "{alias:Str}" to "{status:Str}"')
def set_status(ctx, alias, status):
    do_set_status(ctx, alias, status)


@when('I send a PATCH request to "{path:Str}" with JSON body')
def patch_json(ctx, path):
    call(ctx, "PATCH", path, data=S(ctx, ctx.text).encode(), headers={"Content-Type": "application/json"})


@when('I send a PATCH request to "{path:Str}" with content type "{ctype:Str}" and body')
def patch_ctype(ctx, path, ctype):
    call(ctx, "PATCH", path, data=S(ctx, ctx.text).encode(), headers={"Content-Type": ctype})


@when('I send a PATCH request to "{path:Str}" with JSON body and header "{h:Str}" set to "{v:Str}"')
def patch_json_header(ctx, path, h, v):
    call(ctx, "PATCH", path, data=S(ctx, ctx.text).encode(), headers={"Content-Type": "application/json", h: v})


@when('I send a POST request to "{path:Str}" with content type "{ctype:Str}" and body')
def post_ctype(ctx, path, ctype):
    call(ctx, "POST", path, data=S(ctx, ctx.text).encode(), headers={"Content-Type": ctype})


@when('I create the RESOURCE schema "{alias:Str}" for resource type "{rt:Str}" with header "{h:Str}" set to "{v:Str}"')
def create_with_header(ctx, alias, rt, h, v):
    name = S(ctx, alias)
    fields = {"name": name, "title": "T", "description": "D", "resourceType": S(ctx, rt)}
    data = smlib.fixture_bytes("valid-resource.ttl", ctx.names)
    call(ctx, "POST", "/schemas", headers={h: v}, files=multipart(fields, "valid-resource.ttl", data))


@when('I send a GET request to "{path:Str}"')
def get_path_step(ctx, path):
    call(ctx, "GET", path)


@when('I send a GET request to "{path:Str}" with header "{h:Str}" set to "{v:Str}"')
def get_with_header(ctx, path, h, v):
    call(ctx, "GET", path, headers={h: v})


@when('I send a DELETE request to "{path:Str}"')
def delete_path(ctx, path):
    call(ctx, "DELETE", path)


@when('I request the latest content of schema "{alias:Str}"')
def latest(ctx, alias):
    call(ctx, "GET", f"/schemas/{S(ctx, alias)}{type_query(ctx, alias)}")


@when('I request version "{version:Str}" of schema "{alias:Str}"')
def version_content(ctx, version, alias):
    call(ctx, "GET", f"/schemas/{S(ctx, alias)}/{version}{type_query(ctx, alias)}")


@when('I request version "{version:Str}" of schema "{alias:Str}" as JSON')
def version_json(ctx, version, alias):
    call(ctx, "GET", f"/schemas/{S(ctx, alias)}/{version}{type_query(ctx, alias)}",
         headers={"Accept": "application/json"})


@when('I request the versions of schema "{alias:Str}"')
def versions(ctx, alias):
    call(ctx, "GET", f"/schemas/{S(ctx, alias)}/versions{type_query(ctx, alias)}")


@when("I list schemas")
def list_schemas(ctx):
    call(ctx, "GET", "/schemas")


@when('I list schemas with "{query:Str}"')
def list_schemas_q(ctx, query):
    call(ctx, "GET", f"/schemas?{query}")


@when('I follow the "next" link')
def follow_next(ctx):
    nxt = json_body(ctx.response).get("next")
    assert nxt, "no next link"
    assert nxt.startswith("/schemas"), f"next link is not relative to /schemas: {nxt}"
    call(ctx, "GET", nxt)


OPERATIONS = {
    "list schemas": lambda ctx, a: call(ctx, "GET", "/schemas"),
    "get latest content": lambda ctx, a: call(ctx, "GET", f"/schemas/{S(ctx, a)}"),
    "list versions": lambda ctx, a: call(ctx, "GET", f"/schemas/{S(ctx, a)}/versions"),
    "get version": lambda ctx, a: call(ctx, "GET", f"/schemas/{S(ctx, a)}/1"),
    "create schema": lambda ctx, a: do_create(ctx, a + "NEW", "RESOURCE", "$RTNEW"),
    "create version": lambda ctx, a: do_new_version(ctx, a),
    "revoke schema": lambda ctx, a: do_set_status(ctx, a, "REVOKED"),
    "publish schema": lambda ctx, a: do_set_status(ctx, a, "PUBLISHED"),
    "list webhooks": lambda ctx, a: call(ctx, "GET", "/webhooks"),
    "create webhook": lambda ctx, a: subscribe(ctx, "hook", "SchemaPublished"),
    "delete webhook": lambda ctx, a: call(ctx, "DELETE", f"/webhooks/{uuid.uuid4()}"),
}


@when('I call the operation "{op:Str}" on schema "{alias:Str}"')
def call_operation(ctx, op, alias):
    assert op in OPERATIONS, f"unknown operation {op}"
    OPERATIONS[op](ctx, alias)


@when('two admins create the RESOURCE schemas "{a:Str}" and "{b:Str}" for resource type "{rt:Str}" at the same time')
def parallel_resource(ctx, a, b, rt):
    run_parallel(ctx, [(a, "RESOURCE", rt), (b, "RESOURCE", rt)])


@when('two admins create the SEARCH schemas "{a:Str}" and "{b:Str}" at the same time')
def parallel_search(ctx, a, b):
    run_parallel(ctx, [(a, "SEARCH", None), (b, "SEARCH", None)])


def run_parallel(ctx, jobs):
    ctx.tokens.token("ga-admin")  # warm the cache outside the race
    barrier = threading.Barrier(len(jobs))

    def job(args):
        alias, stype, rt = args
        barrier.wait()
        return do_create(ctx, alias, stype, rt, user="ga-admin", store=False)

    with concurrent.futures.ThreadPoolExecutor(len(jobs)) as pool:
        ctx.parallel = list(pool.map(job, jobs))


# ---------------------------------------------------------------------------
# response assertions
# ---------------------------------------------------------------------------

@then("the response status is {code:Int}")
def status_is(ctx, code):
    must(ctx.response, code, "response status")


@then('the response is a "{ptype:Str}" problem with status {code:Int}')
def problem(ctx, ptype, code):
    resp = ctx.response
    must(resp, code, "problem status")
    ctype = resp.headers.get("Content-Type", "")
    assert ctype.startswith(PROBLEM), f"Content-Type is {ctype!r}, expected {PROBLEM}"
    body = json_body(resp)
    assert body.get("status") == code, f"problem status field is {body.get('status')!r}"
    expected = f"urn:problem-type:simpl:{ptype}"
    assert body.get("type") == expected, f"problem type is {body.get('type')!r}, expected {expected}"


@then('the problem detail is "{text:Str}"')
def detail_is(ctx, text):
    actual = json_body(ctx.response).get("detail")
    assert actual == S(ctx, text), f"detail is {actual!r}, expected {S(ctx, text)!r}"


@then('the problem detail starts with "{text:Str}"')
def detail_starts(ctx, text):
    actual = json_body(ctx.response).get("detail") or ""
    assert actual.startswith(S(ctx, text)), f"detail {actual!r} does not start with {S(ctx, text)!r}"


@then('the problem detail does not start with "{text:Str}"')
def detail_not_starts(ctx, text):
    actual = json_body(ctx.response).get("detail") or ""
    assert not actual.startswith(S(ctx, text)), f"detail {actual!r} starts with {S(ctx, text)!r}"


@then('the problem lists the issue "{code:Str}" at "{location:Str}"')
def issue_exact(ctx, code, location):
    issues = json_body(ctx.response).get("issues") or []
    loc = S(ctx, location)
    assert any(i.get("code") == code and i.get("location") == loc for i in issues), \
        f"no issue {code} at {loc} in {issues}"


@then('the problem lists the issue "{code:Str}" mentioning "{text:Str}"')
def issue_mentions(ctx, code, text):
    issues = json_body(ctx.response).get("issues") or []
    t = S(ctx, text)
    assert any(i.get("code") == code and t in (i.get("location") or "") for i in issues), \
        f"no issue {code} with location mentioning {t} in {issues}"


@then('the problem lists {n:Int} issues with code "{code:Str}"')
def issue_count(ctx, n, code):
    issues = json_body(ctx.response).get("issues") or []
    found = [i for i in issues if i.get("code") == code]
    assert len(found) == n, f"expected {n} issues {code}, got {len(found)}: {issues}"


@then("every problem issue has a code, a description and a location")
def issues_complete(ctx):
    issues = json_body(ctx.response).get("issues") or []
    assert issues, "no issues"
    for i in issues:
        for key in ("code", "description", "location"):
            assert i.get(key), f"issue without {key}: {i}"


@then('the problem has no issue with code "{code:Str}"')
def no_issue(ctx, code):
    issues = json_body(ctx.response).get("issues") or []
    assert not any(i.get("code") == code for i in issues), f"unexpected issue {code}: {issues}"


@then('the JSON field "{path:Str}" is "{value:Str}"')
def field_is(ctx, path, value):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, str) and actual == S(ctx, value), f"{path} is {actual!r}, expected {S(ctx, value)!r}"


@then('the JSON field "{path:Str}" is null')
def field_null(ctx, path):
    actual = get_path(json_body(ctx.response), path)
    assert actual is None, f"{path} is {actual!r}, expected null"


@then('the JSON field "{path:Str}" is the number {n:Int}')
def field_number(ctx, path, n):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, int) and not isinstance(actual, bool) and actual == n, f"{path} is {actual!r}"


@then('the JSON field "{path:Str}" is an empty list')
def field_empty(ctx, path):
    actual = get_path(json_body(ctx.response), path)
    assert actual == [], f"{path} is {actual!r}"


@then('the JSON field "{path:Str}" is a list of {n:Int} items')
def field_len(ctx, path, n):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, list) and len(actual) == n, f"{path} has {len(actual) if isinstance(actual, list) else actual!r}"


@then('the JSON field "{path:Str}" is a list')
def field_list(ctx, path):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, list), f"{path} is {actual!r}"


@then('the JSON field "{path:Str}" is a UUID')
def field_uuid(ctx, path):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, str) and UUID_RE.match(actual), f"{path} is {actual!r}"


@then('the JSON field "{path:Str}" is an ISO timestamp')
def field_ts(ctx, path):
    actual = get_path(json_body(ctx.response), path)
    assert is_iso_timestamp(actual), f"{path} is {actual!r}"


@then('the JSON field "{path:Str}" contains "{text:Str}"')
def field_contains(ctx, path, text):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, str) and S(ctx, text) in actual, f"{path} is {actual!r}"


@then('the JSON field "{path:Str}" is an absolute URI ending with "{text:Str}"')
def field_uri(ctx, path, text):
    actual = get_path(json_body(ctx.response), path)
    assert isinstance(actual, str) and re.match(r"^https?://", actual) and actual.endswith(S(ctx, text)), \
        f"{path} is {actual!r}, expected an absolute URI ending with {S(ctx, text)!r}"


@then('the JSON field "{path:Str}" lists "{values:Str}"')
def field_lists(ctx, path, values):
    actual = get_path(json_body(ctx.response), path)
    expected = [S(ctx, v.strip()) for v in values.split(",")]
    assert isinstance(actual, list) and sorted(actual) == sorted(expected), f"{path} is {actual!r}"


@then('the JSON object has the fields "{fields:Str}"')
def has_fields(ctx, fields):
    body = json_body(ctx.response)
    missing = [f.strip() for f in fields.split(",") if f.strip() not in body]
    assert not missing, f"missing fields {missing} in {body}"


@then('every item of "{path:Str}" has the fields "{fields:Str}"')
def items_have_fields(ctx, path, fields):
    items = get_path(json_body(ctx.response), path)
    assert items, f"{path} is empty"
    for item in items:
        missing = [f.strip() for f in fields.split(",") if f.strip() not in item]
        assert not missing, f"missing fields {missing} in {item}"


@then('every item of "{path:Str}" has "{field:Str}" equal to "{value:Str}"')
def items_field(ctx, path, field, value):
    items = get_path(json_body(ctx.response), path)
    assert items, f"{path} is empty"
    bad = [i for i in items if i.get(field) != S(ctx, value)]
    assert not bad, f"items with {field} != {value}: {bad[:3]}"


@then('no item of "{path:Str}" has "{field:Str}" other than "{value:Str}"')
def items_only(ctx, path, field, value):
    items = get_path(json_body(ctx.response), path)
    assert isinstance(items, list), f"{path} is not a list"
    bad = [i for i in items if i.get(field) != S(ctx, value)]
    assert not bad, f"items with {field} != {value}: {bad[:3]}"


@then('the names in "{path:Str}" are "{names:Str}"')
def names_are(ctx, path, names):
    items = get_path(json_body(ctx.response), path)
    actual = [i.get("name") for i in items]
    expected = [S(ctx, n.strip()) for n in names.split(",") if n.strip()]
    assert actual == expected, f"names are {actual}, expected {expected}"


@then('the versions in "{path:Str}" are "{versions:Str}"')
def versions_in(ctx, path, versions):
    items = get_path(json_body(ctx.response), path)
    actual = [i.get("version") for i in items]
    expected = [v.strip() for v in versions.split(",")]
    assert actual == expected, f"versions are {actual}, expected {expected}"


@then('the response header "{h:Str}" is an absolute URI ending with "{text:Str}"')
def header_uri(ctx, h, text):
    actual = ctx.response.headers.get(h)
    assert actual and re.match(r"^https?://", actual) and actual.endswith(S(ctx, text)), \
        f"{h} is {actual!r}, expected an absolute URI ending with {S(ctx, text)!r}"


@then('the response header "{h:Str}" contains "{text:Str}"')
def header_contains(ctx, h, text):
    actual = ctx.response.headers.get(h) or ""
    assert S(ctx, text) in actual, f"{h} is {actual!r}"


@then('the response content type is "{ctype:Str}"')
def content_type(ctx, ctype):
    actual = ctx.response.headers.get("Content-Type", "")
    assert actual.split(";")[0].strip() == ctype, f"Content-Type is {actual!r}, expected {ctype}"


@then('the response body is the content uploaded for version "{version:Str}" of schema "{alias:Str}"')
def body_is_upload(ctx, version, alias):
    key = (alias, concept(ctx, alias)["type"], version)
    assert key in ctx.uploads, f"no upload recorded for {key}"
    assert ctx.response.content == ctx.uploads[key], \
        f"body differs from the uploaded bytes of {key}: {ctx.response.text[:300]!r}"


@then('the response body is the content uploaded for version "{version:Str}" of the {stype:SchemaType} schema "{alias:Str}"')
def body_is_upload_typed(ctx, version, stype, alias):
    key = (alias, stype, version)
    assert key in ctx.uploads, f"no upload recorded for {key}"
    assert ctx.response.content == ctx.uploads[key], f"body differs from upload {key}"


@then("both requests succeeded with status 201")
def both_201(ctx):
    codes = [r.status_code for r in ctx.parallel]
    assert codes == [201, 201], f"status codes {codes}: {[r.text[:200] for r in ctx.parallel]}"


@then("one request succeeded with status 201 and the other got 409")
def one_201_one_409(ctx):
    codes = sorted(r.status_code for r in ctx.parallel)
    assert codes == [201, 409], f"status codes {codes}"


@then('exactly one of the schemas "{aliases:Str}" has status "PUBLISHED"')
def exactly_one_published(ctx, aliases):
    statuses = []
    for alias in [a.strip() for a in aliases.split(",")]:
        item = find_schema(ctx, alias)
        statuses.append(item and item.get("status"))
    assert statuses.count("PUBLISHED") == 1, f"statuses {statuses}"


# ---------------------------------------------------------------------------
# state assertions (read as ga-admin)
# ---------------------------------------------------------------------------

@then('schema "{alias:Str}" has status "{status:Str}"')
def schema_status(ctx, alias, status):
    item = find_schema(ctx, alias)
    assert item, f"schema {S(ctx, alias)} not listed"
    assert item.get("status") == status, f"{S(ctx, alias)} has status {item.get('status')}, expected {status}"


@then('schema "{alias:Str}" is listed with')
def schema_listed_with(ctx, alias):
    item = find_schema(ctx, alias)
    assert item, f"schema {S(ctx, alias)} not listed"
    for row in ctx.table:
        field, value = row["field"], row["value"]
        actual = item.get(field)
        if value == "<null>":
            assert actual is None, f"{field} is {actual!r}, expected null"
        elif value == "<timestamp>":
            assert is_iso_timestamp(actual), f"{field} is {actual!r}"
        else:
            assert actual == S(ctx, value), f"{field} is {actual!r}, expected {S(ctx, value)!r}"


@then('schema "{alias:Str}" is not listed as a {stype:SchemaType} schema')
def not_listed(ctx, alias, stype):
    assert find_schema(ctx, alias, stype) is None, f"{S(ctx, alias)} is listed as {stype}"


@then('listing "{query:Str}" returns exactly "{aliases:Str}"')
def listing_exactly(ctx, query, aliases):
    names = [i.get("name") for i in list_all(ctx, query)]
    expected = [S(ctx, a.strip()) for a in aliases.split(",") if a.strip()]
    assert names == expected, f"listed {names}, expected {expected}"


@then('listing "{query:Str}" includes "{alias:Str}"')
def listing_includes(ctx, query, alias):
    names = [i.get("name") for i in list_all(ctx, query)]
    assert S(ctx, alias) in names, f"{S(ctx, alias)} not in listing {query}"


@then('listing "{query:Str}" does not include "{alias:Str}"')
def listing_excludes(ctx, query, alias):
    names = [i.get("name") for i in list_all(ctx, query)]
    assert S(ctx, alias) not in names, f"{S(ctx, alias)} is in listing {query}"


@then('listing "{query:Str}" is empty')
def listing_empty(ctx, query):
    items = list_all(ctx, query)
    assert not items, f"listing {query} is not empty: {[i.get('name') for i in items]}"


@then('version "{version:Str}" of schema "{alias:Str}" has status "{status:Str}"')
def version_status(ctx, version, alias, status):
    items = [i for i in versions_all(ctx, alias) if i.get("version") == version]
    assert items, f"version {version} of {S(ctx, alias)} not listed"
    assert items[0].get("status") == status, f"version {version} has status {items[0].get('status')}"


@then('version "{version:Str}" of schema "{alias:Str}" has the changelog "{text:Str}"')
def version_changelog(ctx, version, alias, text):
    items = [i for i in versions_all(ctx, alias) if i.get("version") == version]
    assert items, f"version {version} of {S(ctx, alias)} not listed"
    assert items[0].get("changelog") == S(ctx, text), f"changelog is {items[0].get('changelog')!r}"


@then('schema "{alias:Str}" has the versions "{versions:Str}"')
def schema_versions(ctx, alias, versions):
    actual = [i.get("version") for i in versions_all(ctx, alias)]
    expected = [v.strip() for v in versions.split(",")]
    assert actual == expected, f"versions {actual}, expected {expected}"


# ---------------------------------------------------------------------------
# webhooks (D41 to D44)
# ---------------------------------------------------------------------------

def hook_path(ctx, alias):
    if alias not in ctx.hooks:
        ctx.hooks[alias] = {"path": f"/hooks/{uuid.uuid4()}", "id": None}
    return ctx.hooks[alias]["path"]


def receiver_base(ctx):
    # Without a receiver the subscription CRUD scenarios still need a valid URL.
    return ctx.cfg.webhook_receiver_url or "http://webhook-receiver.example.org"


def subscribe(ctx, alias, events, user=None, store=True):
    url = receiver_base(ctx) + hook_path(ctx, alias)
    body = {"targetUrl": url, "events": [e.strip() for e in events.split(",") if e.strip()]}
    resp = call(ctx, "POST", "/webhooks", user=user, store=store, json=body)
    if resp.status_code == 201:
        try:
            ctx.hooks[alias]["id"] = resp.json().get("webhookId")
            ctx.created_hook_ids.append(ctx.hooks[alias]["id"])
        except ValueError:
            pass
    return resp


@given('the webhook "{alias:Str}" is subscribed to "{events:Str}"')
def given_subscribed(ctx, alias, events):
    must(subscribe(ctx, alias, events, user="ga-admin", store=False), 201, f"subscribe {alias}")


@given('the webhook "{alias:Str}" uses the same target URL as "{other:Str}"')
def same_target(ctx, alias, other):
    ctx.hooks[alias] = {"path": hook_path(ctx, other), "id": None}


@when('I subscribe the webhook "{alias:Str}" to "{events:Str}"')
def when_subscribe(ctx, alias, events):
    subscribe(ctx, alias, events)


@when("I create a webhook subscription with JSON body")
def create_hook_body(ctx):
    url = receiver_base(ctx) + hook_path(ctx, "body")
    text = S(ctx, ctx.text.replace("$RECEIVER", url))
    resp = call(ctx, "POST", "/webhooks", data=text.encode(), headers={"Content-Type": "application/json"})
    if resp.status_code == 201:
        try:
            ctx.hooks["body"]["id"] = resp.json().get("webhookId")
            ctx.created_hook_ids.append(ctx.hooks["body"]["id"])
        except ValueError:
            pass


@when('I delete the webhook "{alias:Str}"')
def delete_hook(ctx, alias):
    call(ctx, "DELETE", f"/webhooks/{ctx.hooks[alias]['id']}")


@when("I list the webhooks")
def list_hooks(ctx):
    call(ctx, "GET", "/webhooks")


@then('the webhook list contains "{alias:Str}" with the events "{events:Str}"')
def hook_listed(ctx, alias, events):
    body = json_body(ctx.response)
    assert isinstance(body, list), f"GET /webhooks is not a plain array: {str(body)[:200]}"
    hook = ctx.hooks[alias]
    match = [w for w in body if w.get("webhookId") == hook["id"]]
    assert match, f"webhook {hook['id']} not listed"
    assert match[0].get("targetUrl") == receiver_base(ctx) + hook["path"], match[0]
    assert sorted(match[0].get("events") or []) == sorted(e.strip() for e in events.split(",")), match[0]


@then('the webhook list does not contain "{alias:Str}"')
def hook_not_listed(ctx, alias):
    body = json_body(ctx.response)
    assert isinstance(body, list), f"GET /webhooks is not a plain array: {str(body)[:200]}"
    assert not [w for w in body if w.get("webhookId") == ctx.hooks[alias]["id"]], "webhook still listed"


@then('the response describes the webhook "{alias:Str}" with the events "{events:Str}"')
def hook_body(ctx, alias, events):
    body = json_body(ctx.response)
    assert UUID_RE.match(str(body.get("webhookId"))), f"webhookId is {body.get('webhookId')!r}"
    assert body.get("targetUrl") == receiver_base(ctx) + ctx.hooks[alias]["path"], body
    assert sorted(body.get("events") or []) == sorted(e.strip() for e in events.split(",")), body


@given('the webhook "{alias:Str}" fails the first {n:Int} deliveries')
def hook_fails(ctx, alias, n):
    ctx.receiver.fail_first(hook_path(ctx, alias), n)


def events_for(ctx, alias, event, schema_alias):
    name = S(ctx, schema_alias)
    out = []
    for d in ctx.receiver.received(hook_path(ctx, alias)):
        body = smlib.parse_event(d)
        if body and body.get("schemaName") == name and (event is None or body.get("eventType") == event):
            out.append((d, body))
    return out


@then('the webhook "{alias:Str}" receives a "{event:Str}" event for schema "{schema:Str}"')
def receives_event(ctx, alias, event, schema):
    found = smlib.wait_until(lambda: events_for(ctx, alias, event, schema), ctx.cfg.event_wait)
    raw = ctx.receiver.received(hook_path(ctx, alias))
    assert found, f"no {event} event for {S(ctx, schema)} within {ctx.cfg.event_wait}s; received {len(raw)}: " \
                  f"{[d['body'][:200] for d in raw]}"
    ctx.delivery, ctx.event = found[-1]


@then('the webhook "{alias:Str}" receives no "{event:Str}" event for schema "{schema:Str}"')
def receives_no_event(ctx, alias, event, schema):
    time.sleep(ctx.cfg.absence_wait)
    found = events_for(ctx, alias, event, schema)
    assert not found, f"unexpected {event} events: {[b for _, b in found]}"


@then('the webhook "{alias:Str}" receives no event for schema "{schema:Str}"')
def receives_nothing(ctx, alias, schema):
    time.sleep(ctx.cfg.absence_wait)
    found = events_for(ctx, alias, None, schema)
    assert not found, f"unexpected events: {[b for _, b in found]}"


@then('the webhook "{alias:Str}" receives exactly {n:Int} "{event:Str}" event for schema "{schema:Str}"')
def receives_exactly(ctx, alias, n, event, schema):
    smlib.wait_until(lambda: len(events_for(ctx, alias, event, schema)) >= n, ctx.cfg.event_wait)
    time.sleep(ctx.cfg.absence_wait)
    found = events_for(ctx, alias, event, schema)
    ids = {b.get("eventId") for d, b in found if 200 <= d["answered"] < 300}
    assert len(ids) == n, f"expected {n} distinct delivered {event} events, got {len(ids)}: {[b for _, b in found]}"


@then('the webhook "{alias:Str}" receives the same "{event:Str}" event for schema "{schema:Str}" {n:Int} times')
def receives_retries(ctx, alias, event, schema, n):
    smlib.wait_until(lambda: len(events_for(ctx, alias, event, schema)) >= n, ctx.cfg.event_wait)
    found = events_for(ctx, alias, event, schema)
    assert len(found) >= n, f"expected {n} attempts, got {len(found)}"
    ids = {b.get("eventId") for _, b in found}
    assert len(ids) == 1, f"retries carry different event ids: {ids}"
    assert found[-1][0]["answered"] == 200, "the last attempt was not answered with 200"


@then('the webhook "{alias:Str}" receives exactly {n:Int} delivery attempts for schema "{schema:Str}"')
def receives_attempts(ctx, alias, n, schema):
    smlib.wait_until(lambda: len(events_for(ctx, alias, None, schema)) >= n, ctx.cfg.event_wait)
    time.sleep(ctx.cfg.absence_wait)
    found = events_for(ctx, alias, None, schema)
    assert len(found) == n, f"expected {n} delivery attempts, got {len(found)}"
    assert len({b.get("eventId") for _, b in found}) == 1, "attempts carry different event ids"


@then('the event field "{field:Str}" is "{value:Str}"')
def event_field(ctx, field, value):
    actual = ctx.event.get(field, "<missing>")
    assert actual == S(ctx, value), f"event {field} is {actual!r}, expected {S(ctx, value)!r}"


@then('the event field "{field:Str}" is null')
def event_field_null(ctx, field):
    assert field in ctx.event and ctx.event[field] is None, f"event {field} is {ctx.event.get(field, '<missing>')!r}"


@then('the event field "{field:Str}" is a UUID')
def event_uuid(ctx, field):
    assert UUID_RE.match(str(ctx.event.get(field))), f"event {field} is {ctx.event.get(field)!r}"


@then('the event field "{field:Str}" is an ISO timestamp')
def event_ts(ctx, field):
    assert is_iso_timestamp(ctx.event.get(field)), f"event {field} is {ctx.event.get(field)!r}"


@then('the event field "{field:Str}" is an absolute URI containing "{text:Str}"')
def event_uri(ctx, field, text):
    actual = ctx.event.get(field) or ""
    assert re.match(r"^https?://", actual) and S(ctx, text) in actual, f"event {field} is {actual!r}"


@then('the event was posted as JSON with the header "X-Simpl-Event" set to "{event:Str}"')
def event_headers(ctx, event):
    headers = ctx.delivery["headers"]
    assert headers.get("content-type", "").startswith("application/json"), headers
    assert headers.get("x-simpl-event") == event, f"X-Simpl-Event is {headers.get('x-simpl-event')!r}"


# ---------------------------------------------------------------------------
# provider e-mail (D39, D40)
# ---------------------------------------------------------------------------

def mails_with_subject(ctx, subject):
    subject = S(ctx, subject)
    return [m for m in ctx.mailpit.messages_mentioning(subject) if m.get("Subject") == subject]


@then('a provider e-mail with the subject "{subject:Str}" arrives')
def mail_arrives(ctx, subject):
    found = smlib.wait_until(lambda: mails_with_subject(ctx, subject), ctx.cfg.event_wait, 0.5)
    assert found, f"no e-mail with subject {S(ctx, subject)!r} within {ctx.cfg.event_wait}s"
    ctx.mail = ctx.mailpit.text(found[0]["ID"])


@then('the e-mail body has the line "{line:Str}"')
def mail_line(ctx, line):
    lines = [l.strip() for l in (ctx.mail.get("Text") or "").splitlines()]
    assert S(ctx, line) in lines, f"line {S(ctx, line)!r} not in body: {lines}"


@then('the e-mail body has a line starting with "{prefix:Str}"')
def mail_line_prefix(ctx, prefix):
    lines = [l.strip() for l in (ctx.mail.get("Text") or "").splitlines()]
    assert any(l.startswith(S(ctx, prefix)) for l in lines), f"no line starting with {prefix!r}: {lines}"


@then('the e-mail body has a timestamp line')
def mail_ts(ctx):
    lines = [l.strip() for l in (ctx.mail.get("Text") or "").splitlines()]
    ts = [l[len("Timestamp: "):] for l in lines if l.startswith("Timestamp: ")]
    assert ts and is_iso_timestamp(ts[0]), f"no ISO timestamp line: {lines}"


@then("the e-mail went to every configured provider address")
def mail_recipients(ctx):
    if not ctx.cfg.provider_emails:
        return  # recipients unknown to the suite; nothing more to check
    to = {a.get("Address") for a in ctx.mail.get("To") or []}
    missing = [a for a in ctx.cfg.provider_emails if a not in to]
    assert not missing, f"provider addresses not addressed: {missing}"


@then('no provider e-mail with the subject "{subject:Str}" arrives')
def no_mail_subject(ctx, subject):
    time.sleep(ctx.cfg.absence_wait)
    found = mails_with_subject(ctx, subject)
    assert not found, f"unexpected e-mail {S(ctx, subject)!r}"


@then('no provider e-mail mentions schema "{alias:Str}"')
def no_mail_mentions(ctx, alias):
    time.sleep(ctx.cfg.absence_wait)
    found = ctx.mailpit.messages_mentioning(S(ctx, alias))
    assert not found, f"unexpected e-mails: {[m.get('Subject') for m in found]}"


@then('exactly {n:Int} provider e-mail mentions schema "{alias:Str}"')
def n_mails(ctx, n, alias):
    smlib.wait_until(lambda: len(ctx.mailpit.messages_mentioning(S(ctx, alias))) >= n, ctx.cfg.event_wait, 0.5)
    time.sleep(ctx.cfg.absence_wait)
    found = ctx.mailpit.messages_mentioning(S(ctx, alias))
    assert len(found) == n, f"expected {n} e-mails, got {[m.get('Subject') for m in found]}"
