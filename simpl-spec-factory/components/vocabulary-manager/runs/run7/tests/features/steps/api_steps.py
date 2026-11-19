"""Shared step library of the Vocabulary Manager API suite.

Black box, HTTP only. Placeholders in feature text are expanded by `expand`:

  ${rid}           run id of the scenario (lower-case hex)
  ${RID}           run id in upper case
  ${V:Alpha}       vocabulary name of alias Alpha in this scenario
  ${VU:Alpha}      the same name in upper case
  ${E:Dc}          external vocabulary name of alias Dc
  ${NS:Dc}         namespace URI used for external alias Dc (no trailing '#' or '/')
  ${OWN:Alpha}     the own namespace of vocabulary Alpha in the generated content
  ${CB}            the content base URL (VOCABULARY_CONTENT_BASE_URL, decision E2)
  ${R:x:1000}      the character x repeated 1000 times
  ${NAME:64}       a valid vocabulary name of exactly 64 characters holding the run id
  ${ENAME:64}      the same for an external vocabulary name
  ${S:key}         a saved value; ${SD:key} its first 10 characters (the date)
  ${EMAIL:ga-admin} the email of a test user
"""
import json
import os
import re
import sys
import threading
import time
from urllib.parse import parse_qs, urlsplit

import parse
from behave import given, register_type, step, then, when

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from vmlib import client  # noqa: E402


@parse.with_pattern(r'[^"]*')
def _parse_q(text):
    return text


register_type(Q=_parse_q)

FIXTURES = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "fixtures"))

MAX_EXT_PAGES = int(os.environ.get("MAX_EXT_PAGES", "1000"))

TS_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


# ------------------------------------------------------------------ helpers


def vname(ctx, alias):
    return f"V{ctx.rid}{alias}"


def ename(ctx, alias):
    return f"E{ctx.rid}{alias}"


def ext_ns(ctx, alias):
    return f"http://example.org/{ctx.rid}/ext/{alias}"


def sized_name(prefix, rid, n):
    base = prefix + rid
    if n <= len(base):
        return (prefix + "x" * (n - 1))[:n]
    return base + "x" * (n - len(base))


def expand(ctx, text):
    if text is None:
        return None

    def rep(m):
        kind, _, arg = m.group(1).partition(":")
        if kind == "rid":
            return ctx.rid
        if kind == "RID":
            return ctx.rid.upper()
        if kind == "V":
            return vname(ctx, arg)
        if kind == "VU":
            return vname(ctx, arg).upper()
        if kind == "E":
            return ename(ctx, arg)
        if kind == "EU":
            return ename(ctx, arg).upper()
        if kind == "NS":
            return ext_ns(ctx, arg)
        if kind == "OWN":
            return client.own_namespace(ctx.rid, arg)
        if kind == "CB":
            return client.CONTENT_BASE
        if kind == "R":
            ch, _, n = arg.rpartition(":")
            return ch * int(n)
        if kind == "NAME":
            return sized_name("V", ctx.rid, int(arg))
        if kind == "ENAME":
            return sized_name("E", ctx.rid, int(arg))
        if kind == "S":
            return str(ctx.saved[arg])
        if kind == "SD":
            return str(ctx.saved[arg])[:10]
        if kind == "EMAIL":
            return client.USERS[arg]["email"]
        raise ValueError("unknown placeholder " + m.group(0))

    return re.sub(r"\$\{([^}]*)\}", rep, text)


def vn(ctx, alias_or_expr):
    """A vocabulary name from an alias, or from an expression holding placeholders."""
    if "${" in alias_or_expr:
        return expand(ctx, alias_or_expr)
    return vname(ctx, alias_or_expr)


def en(ctx, alias_or_expr):
    if "${" in alias_or_expr:
        return expand(ctx, alias_or_expr)
    return ename(ctx, alias_or_expr)


def split_list(text):
    return [x.strip() for x in text.split(",") if x.strip()]


def do(ctx, method, path, caller, **kw):
    path = expand(ctx, path)
    t0 = time.monotonic()
    resp = client.call(method, path, caller, base_url=ctx.base_url, **kw)
    ctx.elapsed = time.monotonic() - t0
    ctx.resp = resp
    q = urlsplit(path).query
    ctx.request_query = parse_qs(q, keep_blank_values=True) if q else {}
    return resp


def check(ctx, method, path, caller="ga-admin", **kw):
    """A read used for an assertion; it does not replace the response under test."""
    return client.call(method, expand(ctx, path), caller, base_url=ctx.base_url, **kw)


def body(ctx, resp=None):
    resp = resp if resp is not None else ctx.resp
    try:
        return resp.json()
    except ValueError:
        raise AssertionError(
            f"response is not JSON: {resp.status_code} {resp.headers.get('Content-Type')} {resp.text[:300]}"
        )


def describe(resp):
    return f"{resp.request.method} {resp.request.url} -> {resp.status_code} {resp.text[:600]}"


def media_type(resp):
    return (resp.headers.get("Content-Type") or "").split(";")[0].strip().lower()


def fixture_bytes(ctx, name):
    with open(os.path.join(FIXTURES, name), "rb") as f:
        raw = f.read()
    return expand(ctx, raw.decode("utf-8")).encode("utf-8")


def file_value(ctx, value, alias, marker):
    """Turn a table value for vocabularyFile into bytes. None means omit."""
    if value == "<omit>":
        return None
    if value == "valid":
        return client.valid_ttl(ctx.rid, alias, marker)
    if value == "<empty>":
        return b""
    if value.startswith("fixture:"):
        return fixture_bytes(ctx, value[len("fixture:"):])
    m = re.match(r"^valid of (\d+) bytes$", value)
    if m:
        return client.sized_ttl(ctx.rid, alias, int(m.group(1)), marker)
    if value == "docstring":
        return expand(ctx, ctx.text).encode("utf-8")
    return expand(ctx, value).encode("utf-8")


def next_marker(ctx):
    ctx.counter += 1
    return str(ctx.counter)


def build_parts(ctx, defaults, table_rows, alias, marker):
    """Multipart parts: defaults overridden by table rows. Several rows with the same
    field add several parts (aliasUris, decision D56)."""
    fields = dict(defaults)
    extra = []
    overridden = set()
    for row in table_rows:
        field, value = row["field"], row["value"]
        if field in overridden or field not in fields:
            extra.append((field, value))
            continue
        fields[field] = value
        overridden.add(field)
    parts = []
    file_bytes = None
    for field, value in list(fields.items()) + extra:
        if field == "vocabularyFile":
            data = file_value(ctx, value, alias, marker)
            if data is None:
                continue
            file_bytes = data
            parts.append(("vocabularyFile", ("vocabulary.ttl", data, "text/turtle")))
        else:
            if value == "<omit>":
                continue
            if value == "<empty>":
                value = ""
            parts.append((field, (None, expand(ctx, value).encode("utf-8"))))
    return parts, file_bytes


def rows_of(ctx):
    if ctx.table is None:
        return []
    return [{"field": r["field"], "value": r["value"]} for r in ctx.table]


def record_created(ctx, resp, name, data):
    if resp.status_code == 201 and data is not None:
        try:
            version = resp.json().get("version")
        except ValueError:
            version = None
        if version is not None:
            ctx.contents[(name, int(version))] = data


# ------------------------------------------------------------- vocabulary writes


def create_vocabulary(ctx, caller, alias, rows=(), content=None):
    name = vn(ctx, alias)
    label = alias if "${" not in alias else "Generated"
    defaults = {
        "name": name,
        "description": f"Description of {label}",
        "changelog": "Initial version.",
        "vocabularyFile": "valid",
    }
    rows = list(rows)
    if content is not None:
        rows = [r for r in rows if r["field"] != "vocabularyFile"]
        rows.append({"field": "vocabularyFile", "value": "docstring"})
    marker = "1"
    parts, data = build_parts(ctx, defaults, rows, label, marker)
    # The name actually sent may come from the table.
    sent_name = name
    for f, v in parts:
        if f == "name":
            sent_name = v[1].decode("utf-8")
    resp = do(ctx, "POST", "/vocabularies", caller, files=parts)
    record_created(ctx, resp, sent_name, data)
    return resp


def create_version(ctx, caller, alias, rows=(), content=None, path_name=None):
    name = path_name or vn(ctx, alias)
    label = alias if "${" not in alias else "Generated"
    marker = next_marker(ctx)
    defaults = {
        "description": f"Description of {label} revision {marker}",
        "changelog": f"Changes in revision {marker}.",
        "vocabularyFile": "valid",
    }
    rows = list(rows)
    if content is not None:
        rows = [r for r in rows if r["field"] != "vocabularyFile"]
        rows.append({"field": "vocabularyFile", "value": "docstring"})
    parts, data = build_parts(ctx, defaults, rows, label, marker)
    resp = do(ctx, "POST", f"/vocabularies/{name}/versions", caller, files=parts)
    record_created(ctx, resp, name, data)
    return resp


def must(resp, code=None):
    ok = resp.status_code == code if code else 200 <= resp.status_code < 300
    assert ok, "setup request failed: " + describe(resp)
    return resp


def patch_status(ctx, caller, path, status):
    return do(ctx, "PATCH", path, caller, json={"status": status})


# ------------------------------------------------------------------- Given steps


@given('vocabulary "{alias:Q}" exists')
def g_vocab_exists(ctx, alias):
    must(create_vocabulary(ctx, "ga-admin", alias), 201)


@given('vocabularies "{aliases:Q}" exist')
def g_vocabs_exist(ctx, aliases):
    for a in split_list(aliases):
        must(create_vocabulary(ctx, "ga-admin", a), 201)


@given('vocabularies "{aliases:Q}" exist, created one second apart')
def g_vocabs_exist_apart(ctx, aliases):
    for i, a in enumerate(split_list(aliases)):
        if i:
            time.sleep(1.2)
        must(create_vocabulary(ctx, "ga-admin", a), 201)


@given('vocabulary "{alias:Q}" exists with this content')
def g_vocab_exists_content(ctx, alias):
    must(create_vocabulary(ctx, "ga-admin", alias, content=ctx.text), 201)


@given('vocabulary "{alias:Q}" exists with the fields')
def g_vocab_exists_fields(ctx, alias):
    must(create_vocabulary(ctx, "ga-admin", alias, rows_of(ctx)), 201)


@given('vocabulary "{alias:Q}" has {n:d} versions')
def g_vocab_versions(ctx, alias, n):
    must(create_vocabulary(ctx, "ga-admin", alias), 201)
    for _ in range(n - 1):
        must(create_version(ctx, "ga-admin", alias), 201)


@given('vocabulary "{alias:Q}" has {n:d} versions, created one second apart')
def g_vocab_versions_apart(ctx, alias, n):
    must(create_vocabulary(ctx, "ga-admin", alias), 201)
    for _ in range(n - 1):
        time.sleep(1.2)
        must(create_version(ctx, "ga-admin", alias), 201)


@given('a new version of "{alias:Q}" was created')
def g_new_version(ctx, alias):
    must(create_version(ctx, "ga-admin", alias), 201)


@given('a new version of "{alias:Q}" was created with this content')
def g_new_version_content(ctx, alias):
    must(create_version(ctx, "ga-admin", alias, content=ctx.text), 201)


@given('vocabulary "{alias:Q}" has status "{status:Q}"')
def g_vocab_status(ctx, alias, status):
    name = vn(ctx, alias)
    cur = must(check(ctx, "GET", f"/vocabularies/{name}"), 200).json()["status"]
    if cur != status:
        must(patch_status(ctx, "ga-admin", f"/vocabularies/{name}", status), 200)


@given('version {v:d} of "{alias:Q}" has status "{status:Q}"')
def g_version_status(ctx, v, alias, status):
    name = vn(ctx, alias)
    cur = must(check(ctx, "GET", f"/vocabularies/{name}/versions/{v}"), 200).json()["status"]
    if cur != status:
        must(patch_status(ctx, "ga-admin", f"/vocabularies/{name}/versions/{v}", status), 200)


@given('I save the field "{field:Q}" of vocabulary "{alias:Q}" as "{key:Q}"')
def g_save_vocab_field(ctx, field, alias, key):
    b = must(check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}"), 200).json()
    ctx.saved[key] = client.json_get(b, field)


@given('I save the field "{field:Q}" of version {v:d} of "{alias:Q}" as "{key:Q}"')
def g_save_version_field(ctx, field, v, alias, key):
    b = must(check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions/{v}"), 200).json()
    ctx.saved[key] = client.json_get(b, field)


@step("one second passes")
def s_wait(ctx):
    time.sleep(1.2)


# --------------------------------------------------------- create and version


@when('"{caller:Q}" creates vocabulary "{alias:Q}"')
def w_create(ctx, caller, alias):
    create_vocabulary(ctx, caller, alias)


@when('"{caller:Q}" creates vocabulary "{alias:Q}" with the fields')
def w_create_fields(ctx, caller, alias):
    create_vocabulary(ctx, caller, alias, rows_of(ctx))


@when('"{caller:Q}" creates vocabulary "{alias:Q}" with this content')
def w_create_content(ctx, caller, alias):
    create_vocabulary(ctx, caller, alias, content=ctx.text)


@when('"{caller:Q}" creates vocabulary "{alias:Q}" with the file "{fixture:Q}"')
def w_create_fixture(ctx, caller, alias, fixture):
    create_vocabulary(ctx, caller, alias, [{"field": "vocabularyFile", "value": "fixture:" + fixture}])


@when('"{caller:Q}" creates vocabulary "{alias:Q}" with a valid file of {n:d} bytes')
def w_create_sized(ctx, caller, alias, n):
    create_vocabulary(ctx, caller, alias, [{"field": "vocabularyFile", "value": f"valid of {n} bytes"}])


@when('"{caller:Q}" creates a new version of "{alias:Q}"')
def w_version(ctx, caller, alias):
    create_version(ctx, caller, alias)


@when('"{caller:Q}" creates a new version of "{alias:Q}" with the fields')
def w_version_fields(ctx, caller, alias):
    create_version(ctx, caller, alias, rows_of(ctx))


@when('"{caller:Q}" creates a new version of "{alias:Q}" with this content')
def w_version_content(ctx, caller, alias):
    create_version(ctx, caller, alias, content=ctx.text)


@when('"{caller:Q}" creates a new version of "{alias:Q}" with the file "{fixture:Q}"')
def w_version_fixture(ctx, caller, alias, fixture):
    create_version(ctx, caller, alias, [{"field": "vocabularyFile", "value": "fixture:" + fixture}])


@when('"{caller:Q}" creates a new version of "{alias:Q}" with a valid file of {n:d} bytes')
def w_version_sized(ctx, caller, alias, n):
    create_version(ctx, caller, alias, [{"field": "vocabularyFile", "value": f"valid of {n} bytes"}])


@when('"{caller:Q}" creates a new version of "{alias:Q}" with the same content and description as version {v:d}')
def w_version_identical(ctx, caller, alias, v):
    name = vn(ctx, alias)
    meta = must(check(ctx, "GET", f"/vocabularies/{name}/versions/{v}"), 200).json()
    assert (name, v) in ctx.contents, f"no content recorded for version {v} of {name}"
    data = ctx.contents[(name, v)]
    rows = ctx.table and rows_of(ctx) or []
    desc = meta["description"]
    for r in rows:
        if r["field"] == "description":
            raw = r["value"]
            if len(raw) >= 2 and raw[0] == raw[-1] == '"':
                raw = raw[1:-1]  # quotes keep the blanks around the text
            desc = expand(ctx, raw).replace("<description>", meta["description"])
    changelog = "Resubmitted without changes."
    for r in rows:
        if r["field"] == "changelog":
            changelog = "" if r["value"] == "<empty>" else expand(ctx, r["value"])
    parts = [
        ("description", (None, desc.encode("utf-8"))),
        ("changelog", (None, changelog.encode("utf-8"))),
        ("vocabularyFile", ("vocabulary.ttl", data, "text/turtle")),
    ]
    resp = do(ctx, "POST", f"/vocabularies/{name}/versions", caller, files=parts)
    record_created(ctx, resp, name, data)


@when('"{caller:Q}" creates a new version of "{alias:Q}" with the same content as version {v:d} and a new description')
def w_version_same_content_new_desc(ctx, caller, alias, v):
    name = vn(ctx, alias)
    data = ctx.contents[(name, v)]
    parts = [
        ("description", (None, f"A changed description {ctx.rid}".encode())),
        ("changelog", (None, b"Description changed.")),
        ("vocabularyFile", ("vocabulary.ttl", data, "text/turtle")),
    ]
    resp = do(ctx, "POST", f"/vocabularies/{name}/versions", caller, files=parts)
    record_created(ctx, resp, name, data)


@when('"{caller:Q}" sends the content of version {v:d} of "{alias:Q}" as a new version of "{target:Q}"')
def w_version_other_name(ctx, caller, v, alias, target):
    data = ctx.contents[(vn(ctx, alias), v)]
    meta = must(check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions/{v}"), 200).json()
    parts = [
        ("description", (None, meta["description"].encode("utf-8"))),
        ("changelog", (None, b"Resubmitted to another name.")),
        ("vocabularyFile", ("vocabulary.ttl", data, "text/turtle")),
    ]
    do(ctx, "POST", f"/vocabularies/{vn(ctx, target)}/versions", caller, files=parts)


@when('"{caller:Q}" sends a multipart {method:w} to "{path:Q}" with the fields')
def w_multipart(ctx, caller, method, path):
    parts, data = build_parts(ctx, {}, rows_of(ctx), "Generic", next_marker(ctx))
    do(ctx, method, path, caller, files=parts)


# ----------------------------------------------------------------- status PATCH


@when('"{caller:Q}" sets the status of vocabulary "{alias:Q}" to "{status:Q}"')
def w_patch_vocab(ctx, caller, alias, status):
    patch_status(ctx, caller, f"/vocabularies/{vn(ctx, alias)}", status)


@when('"{caller:Q}" sets the status of version {v:d} of "{alias:Q}" to "{status:Q}"')
def w_patch_version(ctx, caller, v, alias, status):
    patch_status(ctx, caller, f"/vocabularies/{vn(ctx, alias)}/versions/{v}", status)


# ------------------------------------------------------------------ reads


@when('"{caller:Q}" gets vocabulary "{alias:Q}"')
def w_get_vocab(ctx, caller, alias):
    do(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}", caller)


@when('"{caller:Q}" gets version {v:d} of "{alias:Q}"')
def w_get_version(ctx, caller, v, alias):
    do(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions/{v}", caller)


@when('"{caller:Q}" lists the versions of "{alias:Q}"')
def w_list_versions(ctx, caller, alias):
    do(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions", caller)


@when('"{caller:Q}" lists the versions of "{alias:Q}" with the query "{query:Q}"')
def w_list_versions_q(ctx, caller, alias, query):
    do(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions?{query}", caller)


@when('"{caller:Q}" lists vocabularies with the query "{query:Q}"')
def w_list_q(ctx, caller, query):
    do(ctx, "GET", f"/vocabularies?{query}", caller)


@when('"{caller:Q}" downloads the latest content of "{alias:Q}"')
def w_download_latest(ctx, caller, alias):
    do(ctx, "GET", f"/content/{vn(ctx, alias)}", caller)


@when('"{caller:Q}" downloads version {v:d} of "{alias:Q}"')
def w_download_version(ctx, caller, v, alias):
    do(ctx, "GET", f"/content/{vn(ctx, alias)}/versions/{v}", caller)


# ------------------------------------------------------------- generic requests


@when('"{caller:Q}" sends {method:w} "{path:Q}"')
def w_send(ctx, caller, method, path):
    do(ctx, method, path, caller)


@when('"{caller:Q}" sends {method:w} "{path:Q}" with JSON')
def w_send_json(ctx, caller, method, path):
    do(ctx, method, path, caller, data=expand(ctx, ctx.text).encode("utf-8"),
       headers={"Content-Type": "application/json"})


@when('"{caller:Q}" sends {method:w} "{path:Q}" with the header "{header:Q}" set to "{value:Q}"')
def w_send_header(ctx, caller, method, path, header, value):
    do(ctx, method, path, caller, headers={header: expand(ctx, value)})


@when('"{caller:Q}" sends {method:w} "{path:Q}" with JSON and the header "{header:Q}" set to "{value:Q}"')
def w_send_json_header(ctx, caller, method, path, header, value):
    do(ctx, method, path, caller, data=expand(ctx, ctx.text).encode("utf-8"),
       headers={"Content-Type": "application/json", header: expand(ctx, value)})


@when('"{caller:Q}" sends {method:w} "{path:Q}" with the body "{text:Q}" of type "{ctype:Q}"')
def w_send_body(ctx, caller, method, path, text, ctype):
    do(ctx, method, path, caller, data=expand(ctx, text).encode("utf-8"),
       headers={"Content-Type": ctype})


@when('"{caller:Q}" sends {method:w} "{path:Q}" with a body of {n:d} bytes of type "{ctype:Q}"')
def w_send_big_body(ctx, caller, method, path, n, ctype):
    do(ctx, method, path, caller, data=b"a" * n, headers={"Content-Type": ctype})


@when('"{caller:Q}" sends {method:w} "{path:Q}" with a valid vocabulary as JSON')
def w_send_vocab_as_json(ctx, caller, method, path):
    payload = {
        "name": vname(ctx, "Json"),
        "description": "Sent as JSON",
        "changelog": "Initial version.",
        "vocabularyFile": client.valid_ttl(ctx.rid, "Json").decode(),
    }
    do(ctx, method, path, caller, data=json.dumps(payload).encode(),
       headers={"Content-Type": "application/json"})


# ------------------------------------------------------------------- searches


@when('"{caller:Q}" searches vocabularies with the items')
def w_search_items(ctx, caller):
    items = []
    for row in ctx.table:
        item = {"name": vn(ctx, row["name"])}
        if row["version"].strip():
            item["version"] = int(row["version"])
        items.append(item)
    do(ctx, "POST", "/vocabularies/searches", caller, json={"items": items})


@when('"{caller:Q}" searches vocabularies with {n:d} items for "{alias:Q}"')
def w_search_n(ctx, caller, n, alias):
    items = [{"name": vn(ctx, alias), "version": i + 1} for i in range(n)]
    do(ctx, "POST", "/vocabularies/searches", caller, json={"items": items})


@when('"{caller:Q}" searches external vocabularies with {a:d} namespace URIs and {b:d} alias URIs')
def w_ext_search_counts(ctx, caller, a, b):
    payload = {}
    if a:
        payload["namespaceUris"] = [f"http://example.org/{ctx.rid}/n/{i}" for i in range(a)]
    if b:
        payload["aliasUris"] = [f"http://example.org/{ctx.rid}/a/{i}" for i in range(b)]
    do(ctx, "POST", "/external-vocabulary/searches", caller, json=payload)


# ------------------------------------------------------------ external vocabularies


def register_external(ctx, caller, alias, rows=(), content=None):
    name = en(ctx, alias)
    label = alias if "${" not in alias else "Generated"
    defaults = {
        "name": name,
        "namespaceUri": ext_ns(ctx, label),
        "description": f"Cached copy of {label} for validation.",
        "sourceUri": f"https://source.example.org/{ctx.rid}/{label}.ttl",
        "vocabularyFile": f"fixture:external.ttl",
    }
    rows = list(rows)
    if content is not None:
        rows = [r for r in rows if r["field"] != "vocabularyFile"]
        rows.append({"field": "vocabularyFile", "value": "docstring"})
    ctx.saved["_ext_label"] = label
    parts, data = build_parts(ctx, defaults, rows, "ext" + label, next_marker(ctx))
    resp = do(ctx, "POST", "/external-vocabularies", caller, files=parts)
    if resp.status_code == 201:
        ctx.ext_contents[name] = data
    return resp


@given('external vocabulary "{alias:Q}" is registered')
def g_ext(ctx, alias):
    must(register_external(ctx, "ga-admin", alias), 201)


@given('external vocabulary "{alias:Q}" is registered with the fields')
def g_ext_fields(ctx, alias):
    must(register_external(ctx, "ga-admin", alias, rows_of(ctx)), 201)


@when('"{caller:Q}" registers external vocabulary "{alias:Q}"')
def w_ext(ctx, caller, alias):
    register_external(ctx, caller, alias)


@when('"{caller:Q}" registers external vocabulary "{alias:Q}" with the fields')
def w_ext_fields(ctx, caller, alias):
    register_external(ctx, caller, alias, rows_of(ctx))


@when('"{caller:Q}" registers external vocabulary "{alias:Q}" with this content')
def w_ext_content(ctx, caller, alias):
    register_external(ctx, caller, alias, content=ctx.text)


@when('"{caller:Q}" registers external vocabulary "{alias:Q}" with {n:d} alias URIs')
def w_ext_aliases(ctx, caller, alias, n):
    rows = [{"field": "aliasUris", "value": f"http://alias.example.org/{ctx.rid}/{alias}/{i}"} for i in range(n)]
    register_external(ctx, caller, alias, rows)


@when('"{caller:Q}" registers external vocabulary "{alias:Q}" with a valid file of {n:d} bytes')
def w_ext_sized(ctx, caller, alias, n):
    register_external(ctx, caller, alias, [{"field": "vocabularyFile", "value": f"valid of {n} bytes"}])


@when('"{caller:Q}" deletes external vocabulary "{alias:Q}"')
def w_ext_delete(ctx, caller, alias):
    do(ctx, "DELETE", f"/external-vocabularies/{en(ctx, alias)}", caller)


@given('external vocabulary "{alias:Q}" was deleted')
def g_ext_deleted(ctx, alias):
    must(check(ctx, "DELETE", f"/external-vocabularies/{en(ctx, alias)}"), 204)


@when('"{caller:Q}" gets external vocabulary "{alias:Q}"')
def w_ext_get(ctx, caller, alias):
    do(ctx, "GET", f"/external-vocabularies/{en(ctx, alias)}", caller)


@when('"{caller:Q}" downloads external vocabulary "{alias:Q}"')
def w_ext_download(ctx, caller, alias):
    do(ctx, "GET", f"/external-vocabularies/{en(ctx, alias)}/content", caller)


@when('"{caller:Q}" downloads the external content for namespace "{uri:Q}"')
def w_ext_content_by_ns(ctx, caller, uri):
    do(ctx, "POST", "/external-vocabularies/content", caller, json={"namespaceUri": expand(ctx, uri)})


@then('the external vocabulary list contains "{alias:Q}"')
def t_ext_list_contains(ctx, alias):
    name = en(ctx, alias)
    for page in range(1, MAX_EXT_PAGES + 1):
        r = check(ctx, "GET", f"/external-vocabularies?page={page}&pageSize=100")
        assert r.status_code == 200, describe(r)
        b = r.json()
        if any(i.get("name") == name for i in b.get("items", [])):
            return
        if not b.get("items") or page >= b.get("totalPages", 0):
            raise AssertionError(f"{name} not found in the external vocabulary list")
    raise AssertionError(f"external list longer than {MAX_EXT_PAGES} pages; {name} not checked to the end")


@then('external vocabulary "{alias:Q}" is not stored')
def t_ext_not_stored(ctx, alias):
    name = en(ctx, alias)
    for page in range(1, MAX_EXT_PAGES + 1):
        r = check(ctx, "GET", f"/external-vocabularies?page={page}&pageSize=100")
        assert r.status_code == 200, describe(r)
        b = r.json()
        assert not any(i.get("name") == name for i in b.get("items", [])), f"{name} was stored"
        if not b.get("items") or page >= b.get("totalPages", 0):
            return
    raise AssertionError(f"external list longer than {MAX_EXT_PAGES} pages; {name} not checked to the end")


@then('the response body is the content sent for external vocabulary "{alias:Q}"')
def t_ext_body(ctx, alias):
    assert ctx.resp.content == ctx.ext_contents[en(ctx, alias)], "downloaded bytes differ from the uploaded bytes"


# ------------------------------------------------------------- access check data

OPERATIONS = {
    # operation: (method, path, body kind)
    "GET /vocabularies": ("GET", "/vocabularies?name=${rid}", None),
    "POST /vocabularies": ("POST", "/vocabularies", "create"),
    "POST /vocabularies/searches": ("POST", "/vocabularies/searches", "search"),
    "GET /vocabularies/{vocabularyName}": ("GET", "/vocabularies/${V:Acc}", None),
    "PATCH /vocabularies/{vocabularyName}": ("PATCH", "/vocabularies/${V:Acc}", "patch"),
    "GET /vocabularies/{vocabularyName}/versions": ("GET", "/vocabularies/${V:Acc}/versions", None),
    "POST /vocabularies/{vocabularyName}/versions": ("POST", "/vocabularies/${V:Acc}/versions", "version"),
    "GET /vocabularies/{vocabularyName}/versions/{version}": ("GET", "/vocabularies/${V:Acc}/versions/1", None),
    "PATCH /vocabularies/{vocabularyName}/versions/{version}": ("PATCH", "/vocabularies/${V:Acc}/versions/1", "patch"),
    "GET /content/{vocabularyName}": ("GET", "/content/${V:Acc}", None),
    "GET /content/{vocabularyName}/versions/{version}": ("GET", "/content/${V:Acc}/versions/1", None),
    "GET /external-vocabularies": ("GET", "/external-vocabularies", None),
    "POST /external-vocabularies": ("POST", "/external-vocabularies", "register"),
    "POST /external-vocabulary/searches": ("POST", "/external-vocabulary/searches", "extsearch"),
    "POST /external-vocabularies/content": ("POST", "/external-vocabularies/content", "extcontent"),
    "GET /external-vocabularies/{name}/content": ("GET", "/external-vocabularies/${E:Acc}/content", None),
    "GET /external-vocabularies/{name}": ("GET", "/external-vocabularies/${E:Acc}", None),
    "DELETE /external-vocabularies/{name}": ("DELETE", "/external-vocabularies/${E:Acc}", None),
}


@given("the access check data exists")
def g_access_data(ctx):
    must(create_vocabulary(ctx, "ga-admin", "Acc"), 201)
    must(register_external(ctx, "ga-admin", "Acc"), 201)


@when('"{caller:Q}" calls the operation "{op:Q}"')
def w_call_operation(ctx, caller, op):
    method, path, kind = OPERATIONS[op]
    kw = {}
    if kind == "create":
        parts, _ = build_parts(ctx, {
            "name": vname(ctx, "AccNew"), "description": "Access check",
            "changelog": "Initial version.", "vocabularyFile": "valid"}, [], "AccNew", "1")
        kw["files"] = parts
    elif kind == "version":
        parts, _ = build_parts(ctx, {
            "description": "Access check version", "changelog": "Access check.",
            "vocabularyFile": "valid"}, [], "Acc", next_marker(ctx))
        kw["files"] = parts
    elif kind == "register":
        parts, _ = build_parts(ctx, {
            "name": ename(ctx, "AccNew"), "namespaceUri": ext_ns(ctx, "AccNew"),
            "vocabularyFile": "fixture:external.ttl"}, [], "extAccNew", "1")
        kw["files"] = parts
    elif kind == "patch":
        kw["json"] = {"status": "DEPRECATED"}
    elif kind == "search":
        kw["json"] = {"items": [{"name": vname(ctx, "Acc")}]}
    elif kind == "extsearch":
        kw["json"] = {"namespaceUri": ext_ns(ctx, "Acc")}
    elif kind == "extcontent":
        kw["json"] = {"namespaceUri": ext_ns(ctx, "Acc")}
    do(ctx, method, path, caller, **kw)


# ------------------------------------------------------------------ parallel


def run_parallel(funcs):
    results = [None] * len(funcs)
    barrier = threading.Barrier(len(funcs))

    def runner(i, f):
        barrier.wait()
        results[i] = f()

    threads = [threading.Thread(target=runner, args=(i, f)) for i, f in enumerate(funcs)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    return results


@when('"{caller:Q}" creates {n:d} new versions of "{alias:Q}" in parallel')
def w_parallel_versions(ctx, caller, n, alias):
    name = vn(ctx, alias)
    funcs = []
    for i in range(n):
        marker = next_marker(ctx)
        parts, data = build_parts(ctx, {
            "description": f"Parallel revision {marker}",
            "changelog": f"Parallel change {marker}.",
            "vocabularyFile": "valid"}, [], alias, marker)

        def f(parts=parts):
            return client.call("POST", f"/vocabularies/{name}/versions", caller,
                               base_url=ctx.base_url, files=parts)
        funcs.append(f)
    ctx.parallel = run_parallel(funcs)


@when('"{caller:Q}" creates vocabulary "{alias:Q}" twice in parallel')
def w_parallel_create(ctx, caller, alias):
    name = vn(ctx, alias)
    funcs = []
    for i in range(2):
        parts, _ = build_parts(ctx, {
            "name": name, "description": f"Parallel create {i}",
            "changelog": "Initial version.", "vocabularyFile": "valid"}, [], alias, str(i + 1))

        def f(parts=parts):
            return client.call("POST", "/vocabularies", caller, base_url=ctx.base_url, files=parts)
        funcs.append(f)
    ctx.parallel = run_parallel(funcs)


@when('"{caller:Q}" sets the status of vocabulary "{alias:Q}" to "{status:Q}" twice in parallel')
def w_parallel_patch(ctx, caller, alias, status):
    name = vn(ctx, alias)

    def f():
        return client.call("PATCH", f"/vocabularies/{name}", caller, base_url=ctx.base_url,
                           json={"status": status})
    ctx.parallel = run_parallel([f, f])


@then('the parallel response statuses are "{codes:Q}"')
def t_parallel_statuses(ctx, codes):
    got = sorted(r.status_code for r in ctx.parallel)
    want = sorted(int(c) for c in split_list(codes))
    assert got == want, f"statuses {got}, expected {want}: " + "; ".join(describe(r) for r in ctx.parallel)


@then("the parallel responses carry the version numbers {a:d} to {b:d}, each once")
def t_parallel_versions(ctx, a, b):
    got = sorted(r.json()["version"] for r in ctx.parallel)
    assert got == list(range(a, b + 1)), got


@then("the parallel responses carry distinct Location headers")
def t_parallel_locations(ctx):
    locs = [r.headers.get("Location") for r in ctx.parallel]
    assert all(locs) and len(set(locs)) == len(locs), locs


# ------------------------------------------------------------------ assertions


@then("the response status is {code:d}")
def t_status(ctx, code):
    assert ctx.resp.status_code == code, f"expected {code}: " + describe(ctx.resp)


@then('the response status is one of "{codes:Q}"')
def t_status_in(ctx, codes):
    ok = [int(c) for c in split_list(codes)]
    assert ctx.resp.status_code in ok, describe(ctx.resp)


@then('the response status is not "{codes:Q}"')
def t_status_not(ctx, codes):
    bad = [int(c) for c in split_list(codes)]
    assert ctx.resp.status_code not in bad, describe(ctx.resp)


@then("the request took at most {n:d} seconds")
def t_elapsed(ctx, n):
    assert ctx.elapsed is not None and ctx.elapsed <= n, f"took {ctx.elapsed:.1f} s"


@then("the response is a problem with status {code:d}")
def t_problem(ctx, code):
    r = ctx.resp
    assert r.status_code == code, f"expected {code}: " + describe(r)
    assert media_type(r) == "application/problem+json", "Content-Type: " + str(r.headers.get("Content-Type"))
    b = body(ctx)
    assert b.get("status") == code, f"problem status {b.get('status')}"


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def t_problem_type(ctx, code, ptype):
    t_problem(ctx, code)
    assert body(ctx).get("type") == ptype, f"type {body(ctx).get('type')!r}, expected {ptype!r}"


@then('the problem type is "{ptype:Q}"')
def t_ptype(ctx, ptype):
    assert body(ctx).get("type") == ptype, body(ctx).get("type")


@then('the problem title is "{title:Q}"')
def t_ptitle(ctx, title):
    assert body(ctx).get("title") == title, body(ctx).get("title")


@then('the problem detail is "{detail:Q}"')
def t_pdetail(ctx, detail):
    want = expand(ctx, detail)
    assert body(ctx).get("detail") == want, f"detail {body(ctx).get('detail')!r}, expected {want!r}"


@then('the problem type is a simpl problem type in lowerCamelCase')
def t_simpl_type(ctx):
    t = body(ctx).get("type", "")
    m = re.match(r"^urn:problem-type:simpl:([a-z][a-zA-Z0-9]*)$", t)
    assert m, f"type {t!r} is not urn:problem-type:simpl:<lowerCamelCase>"


def issues(ctx):
    return body(ctx).get("issues") or []


@then("the problem has {n:d} issue")
@then("the problem has {n:d} issues")
def t_issue_count(ctx, n):
    assert len(issues(ctx)) == n, f"{len(issues(ctx))} issues: {issues(ctx)}"


@then("the problem has no issues")
def t_no_issues(ctx):
    assert not issues(ctx), issues(ctx)


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def t_issue_named(ctx, where, name):
    name = expand(ctx, name)
    assert any(i.get("in") == where and i.get("name") == name for i in issues(ctx)), issues(ctx)


@then('the problem has an issue in "{where:Q}" named one of "{names:Q}"')
def t_issue_named_one_of(ctx, where, names):
    allowed = set(split_list(expand(ctx, names)))
    assert any(i.get("in") == where and i.get("name") in allowed for i in issues(ctx)), issues(ctx)


@then('the problem has an issue in "{where:Q}"')
def t_issue_in(ctx, where):
    assert any(i.get("in") == where for i in issues(ctx)), issues(ctx)


@then('the problem has exactly one issue, in "{where:Q}" named "{name:Q}"')
def t_one_issue(ctx, where, name):
    iss = issues(ctx)
    assert len(iss) == 1, iss
    assert iss[0].get("in") == where and iss[0].get("name") == expand(ctx, name), iss


@then('the problem has no issue named "{name:Q}"')
def t_no_issue_named(ctx, name):
    assert not any(i.get("name") == name for i in issues(ctx)), issues(ctx)


@then('every issue is in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def t_every_issue(ctx, where, name, itype):
    iss = issues(ctx)
    assert iss, "no issues"
    for i in iss:
        assert i.get("in") == where and i.get("name") == name and i.get("type") == itype, i


def _match_value(want, got):
    if want == "":
        return True
    if want.startswith("^"):
        return isinstance(got, str) and got.startswith(want[1:])
    if want.startswith("~"):
        return isinstance(got, str) and re.search(want[1:], got) is not None
    return got == want


def _row_matches(ctx, row, item):
    for key in row.headings:
        if not _match_value(expand(ctx, row[key]), item.get(key)):
            return False
    return True


def _match_rows(ctx, items, exact):
    remaining = list(items)
    for row in ctx.table:
        found = next((i for i in remaining if _row_matches(ctx, row, i)), None)
        assert found is not None, f"no entry matches {dict(zip(row.headings, row.cells))} in {items}"
        remaining.remove(found)
    if exact:
        assert not remaining, f"unexpected entries {remaining}"


@then("the problem issues include")
def t_issues_include(ctx):
    _match_rows(ctx, issues(ctx), exact=False)


@then("the problem issues are exactly")
def t_issues_exact(ctx):
    _match_rows(ctx, issues(ctx), exact=True)


@then("the response has no validation warnings")
def t_no_warnings(ctx):
    b = body(ctx)
    assert "validationWarnings" in b, "validationWarnings is missing"
    assert b["validationWarnings"] == [], b["validationWarnings"]


@then("the validation warnings are exactly")
def t_warnings_exact(ctx):
    _match_rows(ctx, body(ctx).get("validationWarnings") or [], exact=True)


@then("the validation warnings include")
def t_warnings_include(ctx):
    _match_rows(ctx, body(ctx).get("validationWarnings") or [], exact=False)


@then('the validation warnings have no code "{code:Q}"')
def t_warnings_no_code(ctx, code):
    ws = body(ctx).get("validationWarnings")
    assert isinstance(ws, list), ws
    assert not any(w.get("code") == code for w in ws), ws


@then('every validation warning code is one of "{codes:Q}"')
def t_warning_codes(ctx, codes):
    allowed = set(split_list(codes))
    ws = body(ctx).get("validationWarnings")
    assert isinstance(ws, list)
    for w in ws:
        assert w.get("code") in allowed, w


@then('the response header "{header:Q}" is "{value:Q}"')
def t_header(ctx, header, value):
    assert ctx.resp.headers.get(header) == expand(ctx, value), f"{header}: {ctx.resp.headers.get(header)!r}"


@then('the response header "{header:Q}" is present')
def t_header_present(ctx, header):
    assert ctx.resp.headers.get(header), f"{header} missing"


@then('the response header "{header:Q}" equals the response field "{field:Q}"')
def t_header_eq_field(ctx, header, field):
    assert ctx.resp.headers.get(header) == client.json_get(body(ctx), field), (
        ctx.resp.headers.get(header), client.json_get(body(ctx), field))


@then('the download file name is "{filename:Q}"')
def t_download_name(ctx, filename):
    cd = ctx.resp.headers.get("Content-Disposition") or ""
    m = re.match(r'^attachment;\s*filename=("?)([^";]+)\1\s*$', cd)
    assert m and m.group(2) == expand(ctx, filename), f"Content-Disposition: {cd!r}"


@then('the response content type is exactly "{ctype:Q}"')
def t_ctype_exact(ctx, ctype):
    assert ctx.resp.headers.get("Content-Type") == ctype, ctx.resp.headers.get("Content-Type")


@then('the response media type is "{ctype:Q}"')
def t_media_type(ctx, ctype):
    assert media_type(ctx.resp) == ctype, ctx.resp.headers.get("Content-Type")


@then('the response field "{field:Q}" equals {value}')
def t_field_eq(ctx, field, value):
    want = json.loads(expand(ctx, value))
    got = client.json_get(body(ctx), field)
    assert got == want, f"{field} = {got!r}, expected {want!r}"


@then('the response field "{field:Q}" is absent')
def t_field_absent(ctx, field):
    assert not client.json_has(body(ctx), field), f"{field} present: {client.json_get(body(ctx), field)!r}"


@then('the response field "{field:Q}" is absent or null')
def t_field_absent_or_null(ctx, field):
    b = body(ctx)
    assert not client.json_has(b, field) or client.json_get(b, field) is None, client.json_get(b, field)


@then('the response fields "{fields:Q}" are absent or null')
def t_fields_absent_or_null(ctx, fields):
    for f in split_list(fields):
        t_field_absent_or_null(ctx, f)


@then('the response field "{field:Q}" is present')
def t_field_present(ctx, field):
    assert client.json_has(body(ctx), field), f"{field} missing in {body(ctx)}"


@then('the response fields "{fields:Q}" are present')
def t_fields_present(ctx, fields):
    for f in split_list(fields):
        t_field_present(ctx, f)


@then('the response field "{field:Q}" is a UTC timestamp to the second')
def t_field_ts(ctx, field):
    v = client.json_get(body(ctx), field)
    assert isinstance(v, str) and TS_RE.match(v), f"{field} = {v!r}"


@then('the response field "{field:Q}" matches the saved "{key:Q}"')
def t_field_saved(ctx, field, key):
    assert client.json_get(body(ctx), field) == ctx.saved[key], (client.json_get(body(ctx), field), ctx.saved[key])


@then('the response field "{field:Q}" is later than the saved "{key:Q}"')
def t_field_later(ctx, field, key):
    got = client.json_get(body(ctx), field)
    assert got > ctx.saved[key], (got, ctx.saved[key])


@then('the response field "{field:Q}" differs from the response field "{other:Q}"')
def t_fields_differ(ctx, field, other):
    assert client.json_get(body(ctx), field) != client.json_get(body(ctx), other)


@then('the response field "{field:Q}" is the same as the response field "{other:Q}"')
def t_fields_equal(ctx, field, other):
    assert client.json_get(body(ctx), field) == client.json_get(body(ctx), other), (
        client.json_get(body(ctx), field), client.json_get(body(ctx), other))


@then('I save the response field "{field:Q}" as "{key:Q}"')
@when('I save the response field "{field:Q}" as "{key:Q}"')
def t_save(ctx, field, key):
    ctx.saved[key] = client.json_get(body(ctx), field)


@then("the response has {n:d} item")
@then("the response has {n:d} items")
def t_items(ctx, n):
    items = body(ctx).get("items")
    assert isinstance(items, list) and len(items) == n, f"{len(items or [])} items: {items}"


@then('the item names are "{names:Q}"')
def t_item_names(ctx, names):
    want = [vn(ctx, a) for a in split_list(names)]
    got = [i.get("name") for i in body(ctx).get("items", [])]
    assert got == want, f"{got}, expected {want}"


@then('the external item names are "{names:Q}"')
def t_ext_item_names(ctx, names):
    want = [en(ctx, a) for a in split_list(names)]
    got = [i.get("name") for i in body(ctx).get("items", [])]
    assert got == want, f"{got}, expected {want}"


@then('the item versions are "{versions:Q}"')
def t_item_versions(ctx, versions):
    want = [int(v) for v in split_list(versions)]
    got = [i.get("version") for i in body(ctx).get("items", [])]
    assert got == want, f"{got}, expected {want}"


@then('every item has the fields "{fields:Q}"')
def t_every_item_fields(ctx, fields):
    items = body(ctx).get("items")
    assert items, "no items"
    for i in items:
        for f in split_list(fields):
            assert f in i, f"{f} missing in {i}"


@then('every item has the field "{field:Q}" equal to {value}')
def t_every_item_eq(ctx, field, value):
    want = json.loads(expand(ctx, value))
    for i in body(ctx).get("items", []):
        assert client.json_get(i, field) == want, i


@then("the items are in name order by code point")
def t_items_name_order(ctx):
    names = [i["name"] for i in body(ctx).get("items", [])]
    assert names == sorted(names), names


@then("the items")
def t_items_table(ctx):
    items = body(ctx).get("items", [])
    assert len(items) == len(ctx.table.rows), f"{len(items)} items: {items}"
    for row, item in zip(ctx.table, items):
        for key in row.headings:
            want = expand(ctx, row[key])
            got = item.get(key)
            if key == "name":
                want = vn(ctx, row[key]) if "${" not in row[key] else want
            if isinstance(got, bool):
                want = want == "true"
            elif isinstance(got, int):
                want = int(want)
            assert got == want, f"{key}: {got!r}, expected {want!r} in {item}"


@then('the response body does not reveal implementation details')
def t_sanitised(ctx):
    text = ctx.resp.text
    for marker in ("Exception", "java.", "org.springframework", "org.apache", "\tat ", "Traceback", "SQL"):
        assert marker not in text, f"{marker!r} in {text[:500]}"


# ------------------------------------------------------- paging links (D22, D59, D78)


@then('the link "{rel:Q}" is absent')
def t_link_absent(ctx, rel):
    t_field_absent_or_null(ctx, rel)


@then('the links "{rels:Q}" are absent')
def t_links_absent(ctx, rels):
    for rel in split_list(rels):
        t_field_absent_or_null(ctx, rel)


@then('the link "{rel:Q}" points to page {page:d} with the request query')
def t_link_page(ctx, rel, page):
    link = client.json_get(body(ctx), rel)
    assert isinstance(link, str) and link, f"{rel} missing"
    parts = urlsplit(link)
    req_path = urlsplit(ctx.resp.request.url).path
    assert req_path.endswith(parts.path), f"{rel} path {parts.path!r} does not match {req_path!r}"
    q = parse_qs(parts.query, keep_blank_values=True)
    assert q.get("page") == [str(page)], f"{rel} = {link}"
    for key, values in (ctx.request_query or {}).items():
        if key == "page":
            continue
        assert q.get(key) == values, f"{rel} = {link} lost {key}={values}"


# ------------------------------------------------------------ stored state checks


@then('vocabulary "{alias:Q}" has status "{status:Q}"')
def t_vocab_status(ctx, alias, status):
    r = check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}")
    assert r.status_code == 200, describe(r)
    assert r.json()["status"] == status, r.json()


@then('version {v:d} of "{alias:Q}" has status "{status:Q}"')
def t_version_status(ctx, v, alias, status):
    r = check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions/{v}")
    assert r.status_code == 200, describe(r)
    assert r.json()["status"] == status, r.json()


@then('versions "{versions:Q}" of "{alias:Q}" have status "{status:Q}"')
def t_versions_status(ctx, versions, alias, status):
    for v in split_list(versions):
        t_version_status(ctx, int(v), alias, status)


@then('the field "{field:Q}" of version {v:d} of "{alias:Q}" matches the saved "{key:Q}"')
def t_version_field_saved(ctx, field, v, alias, key):
    r = check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions/{v}")
    assert r.status_code == 200, describe(r)
    assert client.json_get(r.json(), field) == ctx.saved[key], (client.json_get(r.json(), field), ctx.saved[key])


@then('the field "{field:Q}" of vocabulary "{alias:Q}" matches the saved "{key:Q}"')
def t_vocab_field_saved(ctx, field, alias, key):
    r = check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}")
    assert r.status_code == 200, describe(r)
    assert client.json_get(r.json(), field) == ctx.saved[key], (client.json_get(r.json(), field), ctx.saved[key])


@then('the field "{field:Q}" of vocabulary "{alias:Q}" equals {value}')
def t_vocab_field_eq(ctx, field, alias, value):
    r = check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}")
    assert r.status_code == 200, describe(r)
    want = json.loads(expand(ctx, value))
    assert client.json_get(r.json(), field) == want, r.json()


@then('vocabulary "{alias:Q}" has {n:d} version')
@then('vocabulary "{alias:Q}" has {n:d} versions')
def t_vocab_versions(ctx, alias, n):
    r = check(ctx, "GET", f"/vocabularies/{vn(ctx, alias)}/versions?pageSize=100")
    assert r.status_code == 200, describe(r)
    assert r.json()["total"] == n, r.json()


def _find_by_filter(ctx, name):
    if ctx.rid in name.lower():
        flt = ctx.rid
    elif re.match(r"^[A-Za-z0-9]{1,64}$", name):
        flt = name
    else:
        raise AssertionError(f"cannot filter on {name!r}")
    found = []
    for page in range(1, 51):
        r = check(ctx, "GET", f"/vocabularies?name={flt}&page={page}&pageSize=100")
        assert r.status_code == 200, describe(r)
        b = r.json()
        found += [i["name"] for i in b.get("items", [])]
        if page >= b.get("totalPages", 0):
            return found
    raise AssertionError(f"more than 50 pages match {flt!r}; not checked to the end")


@then("no vocabulary of this scenario is stored")
def t_none_stored(ctx):
    r = check(ctx, "GET", f"/vocabularies?name={ctx.rid}")
    assert r.status_code == 200, describe(r)
    assert r.json()["total"] == 0 and r.json()["items"] == [], r.json()


@then('no vocabulary named "{expr:Q}" is stored')
def t_name_not_stored(ctx, expr):
    name = vn(ctx, expr)
    assert name not in _find_by_filter(ctx, name), f"{name} was stored"


@then('vocabulary "{expr:Q}" is stored')
def t_name_stored(ctx, expr):
    name = vn(ctx, expr)
    assert name in _find_by_filter(ctx, name), f"{name} not found"


@then('the response body is the content sent for version {v:d} of "{alias:Q}"')
def t_body_content(ctx, v, alias):
    want = ctx.contents[(vn(ctx, alias), v)]
    assert ctx.resp.content == want, "downloaded bytes differ from the uploaded bytes"


@then('the content of version {v:d} of "{alias:Q}" is unchanged')
def t_content_unchanged(ctx, v, alias):
    name = vn(ctx, alias)
    r = check(ctx, "GET", f"/content/{name}/versions/{v}")
    assert r.status_code == 200, describe(r)
    assert r.content == ctx.contents[(name, v)]


# ------------------------------------------------------------------- health


@when("I request the health status without a token")
def w_health(ctx):
    do(ctx, "GET", "/health", "anonymous")


# -------------------------------------------------------- not runnable (@wip)


@given("the store answers a query with an unexpected error")
def g_store_error(ctx):
    ctx.scenario.skip("A store error that is not a timeout cannot be provoked from outside.")


@given("the identity provider is down and the token names an unknown key id")
def g_idp_down(ctx):
    ctx.scenario.skip("D44: covered by unit tests in the code, not by the API tests.")
