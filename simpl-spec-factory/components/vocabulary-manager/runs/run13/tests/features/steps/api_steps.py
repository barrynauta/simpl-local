"""Step definitions for the API scenarios. One shared library: vmtests/."""
import functools
import json
import random
import re
import string
import threading
import time
import urllib.parse

import parse
from behave import given, register_type, then, when


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(Q=_quoted)

from vmtests import api, auth, config
from vmtests.data import default_turtle, fixture_text, many_s4_errors, sized_turtle, substitute
from vmtests.http import Part, multipart_body, multipart_overhead, send

DATE_TIME_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")
LOWER_CAMEL_RE = re.compile(r"^[a-z][a-zA-Z0-9]*$")
INVALID_INPUT = "urn:problem-type:belgif:input-validation:invalidInput"


# ---------------------------------------------------------------- helpers

def S(context, text):
    return substitute(text, context)


def _send(context, method, path, **kwargs):
    started = time.monotonic()
    context.resp = send(method, S(context, path), base=context.base, token=context.token, **kwargs)
    context.elapsed = time.monotonic() - started
    _record_upload(context)
    return context.resp


def _record_upload(context):
    """Remember the bytes of a created version so downloads can be compared (D29)."""
    resp = context.resp
    pending = getattr(context, "pending_upload", None)
    context.pending_upload = None
    if pending is None or resp is None or resp.status_code != 201:
        return
    try:
        body = resp.json()
        context.uploaded[(body["name"], int(body["version"]))] = pending
    except Exception:  # noqa: BLE001
        pass


def _json(context):
    try:
        return context.resp.json()
    except Exception as exc:  # noqa: BLE001
        raise AssertionError(f"response is not JSON ({context.resp.status_code}): {context.resp.text[:500]}") from exc


def _get_path(obj, path):
    cur = obj
    for part in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if part.startswith("["):
            idx = int(part[1:-1])
            if not isinstance(cur, list) or idx >= len(cur):
                return KeyError
            cur = cur[idx]
        else:
            if not isinstance(cur, dict) or part not in cur:
                return KeyError
            cur = cur[part]
    return cur


def _as_text(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if value is None:
        return "null"
    if isinstance(value, (dict, list)):
        return json.dumps(value)
    return str(value)


def _check_value(field, actual, expected):
    if expected == "<absent>":
        assert actual is KeyError, f"{field} should be absent, is {actual!r}"
        return
    if expected == "<not true>":
        assert actual is not True, f"{field} should not be true"
        return
    assert actual is not KeyError, f"{field} is missing"
    if expected == "<present>":
        assert actual is not None, f"{field} is null"
    elif expected == "<date-time>":
        assert isinstance(actual, str) and DATE_TIME_RE.match(actual), f"{field} is not a UTC date-time to the second: {actual!r}"
    elif expected == "<empty list>":
        assert actual == [], f"{field} should be an empty list, is {actual!r}"
    elif expected == "<not true>":
        assert actual is not True, f"{field} should not be true"
    elif expected == "<list>":
        assert isinstance(actual, list), f"{field} should be a list, is {actual!r}"
    else:
        assert _as_text(actual) == expected, f"{field}: expected {expected!r}, got {_as_text(actual)!r}"


def _admin_get(context, path):
    return send("GET", S(context, path), token=api.admin_token())


def _problem(context):
    resp = context.resp
    ctype = resp.headers.get("Content-Type", "")
    assert ctype.split(";")[0].strip().lower() == "application/problem+json", (
        f"expected application/problem+json, got {ctype!r} ({resp.status_code}): {resp.text[:300]}"
    )
    return _json(context)


def _issues(context):
    body = _problem(context)
    issues = body.get("issues") or []
    assert isinstance(issues, list), f"issues is not a list: {issues!r}"
    return issues


def _csv(text):
    return [x.strip() for x in text.split(",") if x.strip()]


def _list_all(context, query):
    """All items of GET /vocabularies?<query>, following the pages (pageSize 100)."""
    items, page = [], 1
    while True:
        resp = _admin_get(context, f"/vocabularies?{query}&pageSize=100&page={page}")
        api.expect(resp, 200, "list vocabularies")
        body = resp.json()
        items += body.get("items", [])
        if page >= (body.get("totalPages") or 0):
            return items
        page += 1


# ---------------------------------------------------------------- who calls

@given('I am authenticated as "{user:Q}"')
def step_auth(context, user):
    context.user = user
    context.token = auth.fetch_token(user)


@given("I am not authenticated")
def step_no_auth(context):
    context.user = None
    context.token = None


@given("I send a bearer token that is not a JWT")
def step_bad_token(context):
    context.token = "not-a-jwt-token"


@given('I use a token of "{user:Q}" with a changed signature')
def step_tampered(context, user):
    context.token = auth.tampered(auth.fetch_token(user))


@given('I use a token of "{user:Q}" without a signature')
def step_unsigned(context, user):
    context.token = auth.unsigned(auth.fetch_token(user))


@given('I use a token of "{user:Q}" from the client without the audience mapper')
def step_noaud(context, user):
    context.token = auth.fetch_token(user, client_id=config.TOKEN_CLIENT_NOAUD)


@given('I use a token of "{user:Q}" from the realm "simpl-other"')
def step_other_realm(context, user):
    context.token = auth.fetch_token(user, token_url=config.TOKEN_URL_OTHER, client_id=config.TOKEN_CLIENT_OTHER)


# ---------------------------------------------------------------- set-up (always as ga-admin)

def _setup_create(context, alias, content=None, description=None):
    name = context.names.get(alias)
    context.current_name = name
    content = content if content is not None else default_turtle(name)
    resp = api.create_vocabulary(name, description=description, content=content)
    api.expect(resp, 201, f"create vocabulary {name}")
    context.uploaded[(name, 1)] = content
    return resp


def _setup_version(context, alias, content=None, description=None):
    name = context.names.get(alias)
    context.current_name = name
    latest = api.expect(_admin_get(context, f"/vocabularies/{name}"), 200, "get vocabulary").json()["version"]
    content = content if content is not None else default_turtle(name, latest + 1)
    resp = api.create_version(name, latest + 1, description=description, content=content)
    api.expect(resp, 201, f"create version of {name}")
    context.uploaded[(name, resp.json()["version"])] = content
    return resp


@given('a vocabulary "{alias:Q}" exists')
def step_vocab_exists(context, alias):
    _setup_create(context, alias)


@given('a vocabulary "{alias:Q}" exists with {count:d} versions')
def step_vocab_versions(context, alias, count):
    _setup_create(context, alias)
    for _ in range(count - 1):
        _setup_version(context, alias)


@given('a vocabulary "{alias:Q}" exists with the content')
def step_vocab_content(context, alias):
    context.current_name = context.names.get(alias)
    _setup_create(context, alias, content=S(context, context.text).encode("utf-8"))


@given('a vocabulary "{alias:Q}" exists with the description "{description:Q}"')
def step_vocab_desc(context, alias, description):
    _setup_create(context, alias, description=S(context, description))


@given('a new version of "{alias:Q}" exists')
def step_version_exists(context, alias):
    _setup_version(context, alias)


@given('a new version of "{alias:Q}" exists with the content')
def step_version_exists_content(context, alias):
    context.current_name = context.names.get(alias)
    _setup_version(context, alias, content=S(context, context.text).encode("utf-8"))


@given('the vocabulary status of "{alias:Q}" is "{status:Q}"')
def step_given_vocab_status(context, alias, status):
    name = context.names.get(alias)
    current = api.expect(_admin_get(context, f"/vocabularies/{name}"), 200, "get vocabulary").json()["status"]
    if current != status:
        api.expect(api.patch_status(f"/vocabularies/{name}", status), 200, "patch vocabulary status")


@given('version {version:d} of "{alias:Q}" has the version status "{status:Q}"')
def step_given_version_status(context, version, alias, status):
    name = context.names.get(alias)
    current = api.expect(_admin_get(context, f"/vocabularies/{name}/versions/{version}"), 200, "get version").json()["status"]
    if current != status:
        api.expect(api.patch_status(f"/vocabularies/{name}/versions/{version}", status), 200, "patch version status")


@given('an external vocabulary "{alias:Q}" is registered with the namespace "{ns:Q}"')
def step_external(context, alias, ns):
    name = context.names.get(alias)
    api.expect(api.register_external(name, S(context, ns)), 201, f"register external {name}")


@given('an external vocabulary "{alias:Q}" is registered with the namespace "{ns:Q}" and the aliases "{aliases:Q}"')
def step_external_aliases(context, alias, ns, aliases):
    name = context.names.get(alias)
    api.expect(api.register_external(name, S(context, ns), aliases=_csv(S(context, aliases))), 201,
               f"register external {name}")


@given('the external vocabulary "{alias:Q}" is removed')
def step_external_removed(context, alias):
    name = context.names.get(alias)
    api.expect(send("DELETE", f"/external-vocabularies/{name}", token=api.admin_token()), 204, "delete external")


@given("I wait one second")
@when("I wait one second")
def step_wait(context):
    time.sleep(1.2)


@given('I remember the field "{field:Q}" of the vocabulary "{alias:Q}" as "{key:Q}"')
@when('I remember the field "{field:Q}" of the vocabulary "{alias:Q}" as "{key:Q}"')
def step_remember_vocab_field(context, field, alias, key):
    body = api.expect(_admin_get(context, "/vocabularies/{%s}" % alias), 200, "get vocabulary").json()
    context.memory[key] = body[field]


@given('I remember the field "{field:Q}" of version {version:d} of "{alias:Q}" as "{key:Q}"')
@when('I remember the field "{field:Q}" of version {version:d} of "{alias:Q}" as "{key:Q}"')
def step_remember_version_field(context, field, version, alias, key):
    body = api.expect(_admin_get(context, "/vocabularies/{%s}/versions/%d" % (alias, version)), 200,
                      "get version").json()
    context.memory[key] = body[field]


@when('I remember the response field "{field:Q}" as "{key:Q}"')
def step_remember_resp(context, field, key):
    value = _get_path(_json(context), field)
    assert value is not KeyError, f"{field} missing"
    context.memory[key] = value


@given('I remember the state of the vocabulary "{alias:Q}"')
def step_snapshot(context, alias):
    vocab = api.expect(_admin_get(context, "/vocabularies/{%s}" % alias), 200, "get vocabulary").json()
    versions = api.expect(_admin_get(context, "/vocabularies/{%s}/versions?pageSize=100" % alias), 200,
                          "get versions").json()
    context.snapshots[alias] = (vocab, versions["items"])


# ---------------------------------------------------------------- multipart part tables

def _part_value(context, raw):
    """Value forms of a part table: see README (Part tables)."""
    value = S(context, raw)
    name = context.current_name or context.names.tok
    if value.startswith("file:"):
        return value[5:], fixture_text(value[5:], context), True
    if value == "default-turtle":
        return f"{name}.ttl", default_turtle(name), True
    if value.startswith("turtle-of-size:"):
        return f"{name}.ttl", sized_turtle(name, int(value.split(":")[1])), True
    if value.startswith("s4-errors:"):
        return f"{name}.ttl", many_s4_errors(name, int(value.split(":")[1])), True
    if value == "empty-file":
        return "empty.ttl", b"", True
    if value == "binary-file":
        return "binary.ttl", bytes(range(256)) * 8, True
    if value.startswith("text-of-length:"):
        n = int(value.split(":")[1])
        return None, ("D" + "x" * (n - 1)).encode() if n else b"", False
    if value.startswith("padded-text:"):
        # padded-text:<n> is n characters that are blank on both sides: "  T…  "
        n = int(value.split(":")[1])
        return None, ("  " + "T" * (n - 4) + "  ").encode(), False
    if value == "empty-text":
        return None, b"", False
    if value.startswith("blank:"):
        return None, b" " * int(value.split(":")[1]), False
    return None, value.encode("utf-8"), False


def _build_parts(context, table):
    rows = [(row["part"], row["value"]) for row in table]
    for part, value in rows:
        if part == "name":
            context.current_name = S(context, value)
    parts, pad_to = [], None
    for part, value in rows:
        if value.startswith("pad-request-to:"):
            pad_to = (part, int(value.split(":")[1]))
            continue
        filename, data, is_file = _part_value(context, value)
        if is_file:
            parts.append(Part(part, data, filename=filename, content_type="text/turtle"))
            if part == "vocabularyFile":
                context.pending_upload = data
        else:
            parts.append(Part(part, data))
    boundary = "vmtestsboundary" + context.names.tok
    if pad_to:
        parts.append(Part(pad_to[0], b""))
        overhead = multipart_overhead(parts, boundary)
        parts[-1] = Part(pad_to[0], b"a" * (pad_to[1] - overhead))
    body, ctype = multipart_body(parts, boundary)
    if pad_to:
        assert len(body) == pad_to[1], (len(body), pad_to[1])
    return body, ctype


# ---------------------------------------------------------------- actions

@when('I create the vocabulary "{alias:Q}"')
def step_create(context, alias):
    name = context.names.get(alias)
    context.current_name = name
    content = default_turtle(name)
    body, ctype = multipart_body(api.create_parts(name, f"Description of {name}.", "Initial version.", content))
    context.pending_upload = content
    _send(context, "POST", "/vocabularies", body=body, content_type=ctype)


@when('I create the vocabulary "{alias:Q}" with the content')
def step_create_content(context, alias):
    name = context.names.get(alias)
    context.current_name = name
    content = S(context, context.text).encode("utf-8")
    body, ctype = multipart_body(api.create_parts(name, f"Description of {name}.", "Initial version.", content))
    context.pending_upload = content
    _send(context, "POST", "/vocabularies", body=body, content_type=ctype)


@when('I create the vocabulary "{alias:Q}" with the fixture "{fixture:Q}"')
def step_create_fixture(context, alias, fixture):
    name = context.names.get(alias)
    context.current_name = name
    content = fixture_text(fixture, context)
    body, ctype = multipart_body(api.create_parts(name, f"Description of {name}.", "Initial version.", content))
    context.pending_upload = content
    _send(context, "POST", "/vocabularies", body=body, content_type=ctype)


@when("I create a vocabulary with the parts")
def step_create_parts(context):
    body, ctype = _build_parts(context, context.table)
    _send(context, "POST", "/vocabularies", body=body, content_type=ctype)


@when('I create a vocabulary with a name of {length:d} characters')
def step_create_name_length(context, length):
    tok = context.names.tok
    if length > len(tok):
        name = "N" + tok + "x" * (length - 1 - len(tok))
    else:
        # Too short to hold the token: pick a random name that is not stored yet.
        for _ in range(50):
            name = random.choice(string.ascii_uppercase) + "".join(
                random.choice(string.ascii_letters + string.digits) for _ in range(length - 1))
            if length < 3:
                break
            # Uniqueness is case-insensitive (D37) and the store is never reset (E4).
            taken = _list_all(context, f"name={name}")
            if all(i["name"].lower() != name.lower() for i in taken):
                break
    context.last_name = name
    context.current_name = name
    context.names.set("LastName", name)
    content = default_turtle(name)
    body, ctype = multipart_body(api.create_parts(name, f"Description of {name}.", "Initial version.", content))
    context.pending_upload = content
    _send(context, "POST", "/vocabularies", body=body, content_type=ctype)


def _new_version(context, alias, description=None, changelog=None, content=None):
    name = context.names.get(alias)
    context.current_name = name
    description = description if description is not None else f"New description of {name} {time.time_ns()}."
    changelog = changelog if changelog is not None else "Changes for the new version."
    content = content if content is not None else default_turtle(name, time.time_ns())
    body, ctype = multipart_body(api.create_parts(None, description, changelog, content))
    context.pending_upload = content
    _send(context, "POST", f"/vocabularies/{name}/versions", body=body, content_type=ctype)


@when('I create a new version of "{alias:Q}"')
def step_new_version(context, alias):
    _new_version(context, alias)


@when('I create a new version of "{alias:Q}" with the content')
def step_new_version_content(context, alias):
    context.current_name = context.names.get(alias)
    _new_version(context, alias, content=S(context, context.text).encode("utf-8"))


@when('I create a new version of "{alias:Q}" with the parts')
def step_new_version_parts(context, alias):
    context.current_name = context.names.get(alias)
    body, ctype = _build_parts(context, context.table)
    _send(context, "POST", "/vocabularies/{%s}/versions" % alias, body=body, content_type=ctype)


@when('I create a new version of "{alias:Q}" with the same content and description as version {version:d}')
def step_new_version_identical(context, alias, version):
    name = context.names.get(alias)
    v = api.expect(_admin_get(context, f"/vocabularies/{name}/versions/{version}"), 200, "get version").json()
    content = context.uploaded[(name, version)]
    _new_version(context, alias, description=v["description"], changelog="Another changelog.", content=content)


@when('I create a new version of "{alias:Q}" with the content of version {version:d} and the description "{description:Q}"')
def step_new_version_same_content(context, alias, version, description):
    name = context.names.get(alias)
    _new_version(context, alias, description=S(context, description), content=context.uploaded[(name, version)])


@when('I send a multipart POST to "{path:Q}" with the parts')
def step_multipart_post(context, path):
    body, ctype = _build_parts(context, context.table)
    _send(context, "POST", path, body=body, content_type=ctype)


@when('I send {method:w} "{path:Q}"')
def step_send(context, method, path):
    _send(context, method, path)


@when('I send {method:w} "{path:Q}" with the JSON body')
def step_send_json(context, method, path):
    _send(context, method, path, body=S(context, context.text).encode("utf-8"), content_type="application/json")


@when('I send {method:w} "{path:Q}" with the headers')
def step_send_headers(context, method, path):
    headers = {row["header"]: S(context, row["value"]) for row in context.table}
    _send(context, method, path, headers=headers)


@when('I send {method:w} "{path:Q}" with a JSON body and the Accept header "{accept:Q}"')
def step_send_json_accept(context, method, path, accept):
    _send(context, method, path, body=b'{"name": "Abc"}', content_type="application/json",
          headers={"Accept": accept})


@when('I send GET "{path:Q}" outside the API base path')
def step_send_origin(context, path):
    parts = urllib.parse.urlsplit(context.base)
    origin = f"{parts.scheme}://{parts.netloc}"
    started = time.monotonic()
    context.resp = send("GET", origin + S(context, path), token=context.token, absolute=True)
    context.elapsed = time.monotonic() - started


@when('I send {method:w} "{path:Q}" with the body "{body:Q}" of type "{ctype:Q}"')
def step_send_body(context, method, path, body, ctype):
    _send(context, method, path, body=S(context, body).encode("utf-8"), content_type=ctype)


@when('I send {method:w} "{path:Q}" with a JSON body of exactly {size:d} bytes')
def step_send_sized_json(context, method, path, size):
    name = context.names.get("Alpha")
    base = json.dumps({"items": [{"name": name}]})
    pad = size - len(base)
    assert pad >= 0
    body = (base[:-1] + " " * pad + "}").encode()
    assert len(body) == size
    _send(context, method, path, body=body, content_type="application/json")


@when('I send {method:w} "{path:Q}" with a multipart body of type "{ctype:Q}"')
def step_send_multipart_wrong_type(context, method, path, ctype):
    name = context.names.get("Alpha")
    body, _ = multipart_body(api.create_parts(name, "d", "c", default_turtle(name)))
    _send(context, method, path, body=body, content_type=ctype)


@when('I send {method:w} "{path:Q}" with a body of {size:d} bytes of type "{ctype:Q}"')
def step_send_sized(context, method, path, size, ctype):
    _send(context, method, path, body=b"a" * size, content_type=ctype)


@then('the response field "{field:Q}" is later than "{earlier:Q}"')
def step_later(context, field, earlier):
    got = _get_path(_json(context), field)
    want = S(context, earlier)
    assert got is not KeyError and DATE_TIME_RE.match(str(got)), f"{field}: {got!r}"
    assert got > want, f"{field} {got} is not later than {want}"


@when('I send GET "{path:Q}" with a query string of {size:d} characters')
def step_long_url(context, path, size):
    _send(context, "GET", S(context, path) + "?name=" + "a" * size)


@when('I send GET "{path:Q}" with a header "{header:Q}" of {size:d} characters')
def step_big_header(context, path, header, size):
    _send(context, "GET", path, headers={header: "a" * size})


@when('I set the vocabulary status of "{alias:Q}" to "{status:Q}"')
def step_patch_vocab(context, alias, status):
    _send(context, "PATCH", "/vocabularies/{%s}" % alias, json={"status": status})


@when('I set the version status of version {version:d} of "{alias:Q}" to "{status:Q}"')
def step_patch_version(context, version, alias, status):
    _send(context, "PATCH", "/vocabularies/{%s}/versions/%d" % (alias, version), json={"status": status})


@when('I register the external vocabulary "{alias:Q}" with the namespace "{ns:Q}"')
def step_register(context, alias, ns):
    name = context.names.get(alias)
    namespace = S(context, ns)
    content = (f"@prefix t: <{namespace.rstrip('#/')}#> .\n"
               "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
               "t:term a rdfs:Class .\n").encode()
    parts = [Part("name", name), Part("namespaceUri", namespace), api.turtle_part(content, f"{name}.ttl")]
    body, ctype = multipart_body(parts)
    _send(context, "POST", "/external-vocabularies", body=body, content_type=ctype)


@when("I register an external vocabulary with the parts")
def step_register_parts(context):
    body, ctype = _build_parts(context, context.table)
    _send(context, "POST", "/external-vocabularies", body=body, content_type=ctype)


@when('I call the operation "{op:Q}"')
def step_call_op(context, op):
    method, path, kwargs = api.build_operation(
        op,
        vocab=context.names.get("Voc"),
        external=context.names.get("Ext"),
        external_ns=f"https://ext-{context.names.tok.lower()}.example.org/ns",
        new_name=context.names.get("Fresh"),
        new_external=context.names.get("FreshExt"),
        tok=context.names.tok,
    )
    _send(context, method, path, **kwargs)


@when("I search for the vocabularies")
def step_search(context):
    items = []
    for row in context.table:
        item = {"name": S(context, row["name"])}
        if row["version"].strip():
            item["version"] = int(row["version"])
        items.append(item)
    _send(context, "POST", "/vocabularies/searches", json={"items": items})


@when("I search for {count:d} references to the vocabulary \"{alias}\"")
def step_search_many(context, count, alias):
    name = context.names.get(alias)
    items = [{"name": name}] + [{"name": name, "version": 1}] * (count - 1)
    _send(context, "POST", "/vocabularies/searches", json={"items": items})


@when("I search the external vocabularies with {count:d} namespace URIs and {alias_count:d} alias URIs")
def step_ext_search_many(context, count, alias_count):
    body = {}
    if count:
        body["namespaceUris"] = [f"https://n{i}-{context.names.tok.lower()}.example.org/ns" for i in range(count)]
    if alias_count:
        body["aliasUris"] = [f"https://a{i}-{context.names.tok.lower()}.example.org/ns" for i in range(alias_count)]
    _send(context, "POST", "/external-vocabulary/searches", json=body)


# ---------------------------------------------------------------- concurrency (D43, D119)

def _parallel(context, calls):
    results = [None] * len(calls)
    barrier = threading.Barrier(len(calls))

    def run(i, fn):
        barrier.wait()
        results[i] = fn()

    threads = [threading.Thread(target=run, args=(i, fn)) for i, fn in enumerate(calls)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    context.responses = results


@when('{count:d} new versions of "{alias:Q}" are created in parallel')
def step_parallel_versions(context, count, alias):
    name = context.names.get(alias)
    token = context.token

    def make(i):
        return lambda: api.create_version(name, 100 + i, description=f"Parallel version {i} of {name}.",
                                          content=default_turtle(name, 100 + i), token=token)

    _parallel(context, [make(i) for i in range(count)])


@when('the vocabulary "{alias:Q}" is created twice in parallel')
def step_parallel_create(context, alias):
    name = context.names.get(alias)
    token = context.token
    _parallel(context, [lambda: api.create_vocabulary(name, token=token),
                        lambda: api.create_vocabulary(name, token=token)])


@when('the vocabulary status of "{alias:Q}" is set to "{status:Q}" twice in parallel')
def step_parallel_patch(context, alias, status):
    name = context.names.get(alias)
    token = context.token
    _parallel(context, [lambda: api.patch_status(f"/vocabularies/{name}", status, token),
                        lambda: api.patch_status(f"/vocabularies/{name}", status, token)])


@then("all {count:d} responses have the status {status:d}")
def step_all_status(context, count, status):
    got = [r.status_code for r in context.responses]
    assert len(got) == count and all(s == status for s in got), f"statuses: {got}; bodies: {[r.text[:200] for r in context.responses]}"


@then("the responses have the statuses {statuses}")
def step_resp_statuses(context, statuses):
    want = sorted(int(s) for s in _csv(statuses.replace(" and ", ",")))
    got = sorted(r.status_code for r in context.responses)
    assert got == want, f"expected {want}, got {got}"


@then("the created version numbers are {low:d} to {high:d}, each exactly once")
def step_version_numbers(context, low, high):
    got = sorted(r.json()["version"] for r in context.responses)
    assert got == list(range(low, high + 1)), f"versions: {got}"


@then("the Location headers of the responses are all different")
def step_locations_distinct(context):
    locs = [r.headers.get("Location") for r in context.responses]
    assert all(locs) and len(set(locs)) == len(locs), f"Location headers: {locs}"


# ---------------------------------------------------------------- status and problem checks

@then("the response status is {status:d}")
def step_status(context, status):
    resp = context.resp
    assert resp.status_code == status, f"expected {status}, got {resp.status_code}: {resp.text[:800]}"


@then("the response status is not {status:d}")
def step_status_not(context, status):
    assert context.resp.status_code != status, f"got {status}: {context.resp.text[:500]}"


@then("the response is a problem with status {status:d}")
def step_problem(context, status):
    step_status(context, status)
    body = _problem(context)
    assert body.get("status") == status, f"problem status field is {body.get('status')!r}"


@then('the response is a problem with status {status:d} and type "{ptype:Q}"')
def step_problem_type(context, status, ptype):
    step_problem(context, status)
    body = _json(context)
    assert body.get("type") == ptype, f"problem type is {body.get('type')!r}, expected {ptype!r}"


@then("the problem type follows the Simpl URN form")
def step_problem_simpl(context):
    ptype = _json(context).get("type", "")
    assert ptype.startswith("urn:problem-type:simpl:"), f"type {ptype!r} is not a simpl URN (API-46)"
    parts = ptype[len("urn:problem-type:simpl:"):].split(":")
    assert 1 <= len(parts) <= 2 and all(LOWER_CAMEL_RE.match(p) for p in parts), (
        f"type part of {ptype!r} is not lowerCamelCase (API-47)")


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    assert _problem(context).get("title") == title, f"title is {_json(context).get('title')!r}"


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    want = S(context, detail)
    got = _problem(context).get("detail")
    assert got == want, f"detail is {got!r}, expected {want!r}"


@then('the problem detail starts with "{prefix:Q}"')
def step_problem_detail_prefix(context, prefix):
    got = _problem(context).get("detail") or ""
    assert got.startswith(S(context, prefix)), f"detail is {got!r}"


@then("the problem has no issues")
def step_no_issues(context):
    assert _issues(context) == [], f"issues: {_issues(context)}"


@then("the problem has exactly one issue")
def step_one_issue(context):
    issues = _issues(context)
    assert len(issues) == 1, f"expected one issue, got {len(issues)}: {issues}"


@then("the problem has exactly {count:d} issues")
def step_n_issues(context, count):
    issues = _issues(context)
    assert len(issues) == count, f"expected {count} issues, got {len(issues)}: {issues[:5]}"


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_named(context, where, name):
    want = S(context, name)
    issues = _issues(context)
    assert any(i.get("in") == where and i.get("name") == want for i in issues), (
        f"no issue in {where} named {want!r}: {issues}")


@then('the problem has some issue in "{where:Q}"')
def step_issue_in(context, where):
    issues = _issues(context)
    assert any(i.get("in") == where for i in issues), f"no issue in {where}: {issues}"


@then('the problem has an issue in "{where:Q}" with a name starting with "{prefix:Q}"')
def step_issue_prefix(context, where, prefix):
    issues = _issues(context)
    assert any(i.get("in") == where and str(i.get("name", "")).startswith(prefix) for i in issues), (
        f"no issue in {where} named {prefix}...: {issues}")


@then('every issue is in "{where:Q}" named "{name:Q}"')
def step_every_issue(context, where, name):
    issues = _issues(context)
    assert issues, "no issues"
    bad = [i for i in issues if i.get("in") != where or i.get("name") != name]
    assert not bad, f"issues not in {where} named {name}: {bad[:5]}"


@then('the issue in "{where:Q}" named "{name:Q}" has the type "{itype:Q}"')
def step_issue_type(context, where, name, itype):
    issues = [i for i in _issues(context) if i.get("in") == where and i.get("name") == S(context, name)]
    assert issues and all(i.get("type") == itype for i in issues), f"issues: {issues}"


@then('the issue in "{where:Q}" named "{name:Q}" has the detail "{detail:Q}"')
def step_issue_detail(context, where, name, detail):
    want = S(context, detail)
    issues = [i for i in _issues(context) if i.get("in") == where and i.get("name") == S(context, name)]
    assert any(i.get("detail") == want for i in issues), f"no such detail {want!r}: {issues}"


# ---------------------------------------------------------------- validation report (D9, D10, D65)

def _report(context):
    step_problem_type(context, 400, "urn:problem-type:belgif:badRequest")
    issues = _issues(context)
    for i in issues:
        assert i.get("type") == INVALID_INPUT, f"issue type {i.get('type')!r}"
        assert i.get("in") == "body" and i.get("name") == "vocabularyFile", f"issue {i}"
    return issues


@then("the response is a validation report")
def step_report(context):
    assert _report(context), "validation report without issues"


@then("the validation report has exactly the issues")
def step_report_exact(context):
    issues = _report(context)
    want = sorted((S(context, r["detail"]), S(context, r["location"])) for r in context.table)
    got = sorted((i.get("detail"), i.get("location")) for i in issues)
    assert got == want, f"expected {want}\n got {got}"


@then('the validation report has an issue with the detail "{detail:Q}" and the location "{location:Q}"')
def step_report_has(context, detail, location):
    issues = _report(context)
    pair = (S(context, detail), S(context, location))
    assert pair in [(i.get("detail"), i.get("location")) for i in issues], f"{pair} not in {issues}"


@then("the validation report has one syntax error on line {line:d}")
def step_report_syntax(context, line):
    issues = _report(context)
    assert len(issues) == 1, f"expected one syntax issue, got {issues}"
    issue = issues[0]
    assert str(issue.get("detail", "")).startswith("Encountered invalid Turtle syntax"), issue
    loc = str(issue.get("location", ""))
    assert re.match(rf"^line {line}(, column \d+)?$", loc), f"location {loc!r}, expected line {line}"


@then("the validation report has {count:d} issues")
def step_report_count(context, count):
    issues = _report(context)
    assert len(issues) == count, f"expected {count}, got {len(issues)}"


@then("the response has no validation warnings")
def step_no_warnings(context):
    body = _json(context)
    assert "validationWarnings" in body, "validationWarnings is missing (D10: always present)"
    assert body["validationWarnings"] == [], f"warnings: {body['validationWarnings']}"


@then("the response has exactly the validation warnings")
def step_warnings_exact(context):
    body = _json(context)
    got = sorted((w.get("code"), w.get("description"), w.get("location")) for w in body.get("validationWarnings", []))
    want = []
    for r in context.table:
        want.append((S(context, r["code"]), S(context, r["description"]) if "description" in r.headings else None,
                     S(context, r["location"])))
    if "description" not in context.table.headings:
        got = sorted((c, None, l) for c, _, l in got)
    assert got == sorted(want), f"expected {sorted(want)}\n got {got}"


@then("every validation warning has a code from the closed set")
def step_warning_codes(context):
    allowed = {"externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"}
    for w in _json(context).get("validationWarnings", []):
        assert w.get("code") in allowed, f"code {w.get('code')!r}"
        assert w.get("code") != "semanticReasoningSkipped", "semanticReasoningSkipped is not emitted (D10)"
        assert w.get("description") and w.get("location"), f"warning without description or location: {w}"


# ---------------------------------------------------------------- JSON and headers

@then("the response JSON has")
def step_json_has(context):
    body = _json(context)
    for row in context.table:
        field = row["field"]
        _check_value(field, _get_path(body, field), S(context, row["value"]))


@then('the response header "{header:Q}" is "{value:Q}"')
def step_header_is(context, header, value):
    got = context.resp.headers.get(header)
    assert got == S(context, value), f"{header}: {got!r}"


@then('the response header "{header:Q}" is present')
def step_header_present(context, header):
    assert context.resp.headers.get(header), f"{header} missing: {dict(context.resp.headers)}"


@then('the response header "{header:Q}" contains "{value:Q}"')
def step_header_contains(context, header, value):
    got = context.resp.headers.get(header) or ""
    assert S(context, value) in got, f"{header}: {got!r}"


@then('the response header "Allow" lists "{methods:Q}"')
def step_allow(context, methods):
    got = {m.strip().upper() for m in (context.resp.headers.get("Allow") or "").split(",") if m.strip()}
    want = set(_csv(methods))
    assert want <= got, f"Allow is {got}, expected at least {want}"


@then('the response media type is "{media:Q}"')
def step_media(context, media):
    got = (context.resp.headers.get("Content-Type") or "").split(";")[0].strip().lower()
    assert got == media, f"media type {got!r}"


@then('the response Content-Type is exactly "{value:Q}"')
def step_ctype_exact(context, value):
    got = context.resp.headers.get("Content-Type")
    assert got is not None and got.strip().lower() == value, f"Content-Type {got!r}"


@then("every response with a body has a Content-Type header")
def step_every_ctype(context):
    resp = context.resp
    if resp.content:
        assert resp.headers.get("Content-Type"), "body without Content-Type (API-36)"


@then('the download file name is "{filename:Q}"')
def step_filename(context, filename):
    disp = context.resp.headers.get("Content-Disposition") or ""
    want = S(context, filename)
    assert disp.lower().startswith("attachment"), f"Content-Disposition {disp!r}"
    m = re.search(r'filename\s*=\s*"?([^";]+)"?', disp)
    assert m and m.group(1) == want, f"Content-Disposition {disp!r}, expected filename {want!r}"


@then('the response body is the uploaded content of version {version:d} of "{alias:Q}"')
def step_body_is_upload(context, version, alias):
    name = context.names.get(alias)
    want = context.uploaded[(name, version)]
    assert context.resp.content == want, (
        f"downloaded {len(context.resp.content)} bytes differ from the {len(want)} uploaded bytes")


@then("the response body is empty")
def step_body_empty(context):
    assert context.resp.content in (b"",), f"body: {context.resp.text[:200]}"


@then("the response arrived within {seconds:d} seconds")
def step_within(context, seconds):
    assert context.elapsed is not None and context.elapsed <= seconds, f"took {context.elapsed:.1f} s"


# ---------------------------------------------------------------- created version (D25, D26, D27, D10, D5)

@then('the response is the created version {version:d} of "{alias:Q}"')
def step_created(context, version, alias):
    step_status(context, 201)
    name = context.names.get(alias)
    body = _json(context)
    href = f"/vocabularies/{name}/versions/{version}"
    assert context.resp.headers.get("Location") == href, f"Location {context.resp.headers.get('Location')!r}"
    expected = {
        "href": href,
        "contentHref": f"{config.CONTENT_BASE}/content/{name}/versions/{version}",
        "name": name,
        "version": str(version),
        "status": "ACTIVE",
        "createdBy": config.USERS["ga-admin"]["email"],
        "creationDate": "<date-time>",
        "lastUpdateDate": "<date-time>",
        "validationWarnings": "<list>",
        "latestVersionHref": href,
        "description": "<present>",
        "changelog": "<present>",
    }
    for field, value in expected.items():
        _check_value(field, _get_path(body, field), value)


# ---------------------------------------------------------------- stored state, read as ga-admin

@then('the vocabulary "{alias:Q}" has the vocabulary status "{status:Q}"')
def step_vocab_status(context, alias, status):
    body = api.expect(_admin_get(context, "/vocabularies/{%s}" % alias), 200, "get vocabulary").json()
    assert body["status"] == status, f"vocabulary status {body['status']}"


@then('version {version:d} of "{alias:Q}" has the version status "{status:Q}"')
def step_version_status(context, version, alias, status):
    body = api.expect(_admin_get(context, "/vocabularies/{%s}/versions/%d" % (alias, version)), 200,
                      "get version").json()
    assert body["status"] == status, f"version {version} status {body['status']}"


@then('the vocabulary "{alias:Q}" has {count:d} versions')
def step_version_count(context, alias, count):
    body = api.expect(_admin_get(context, "/vocabularies/{%s}/versions" % alias), 200, "get versions").json()
    assert body["total"] == count, f"total {body['total']}"


@then('the vocabulary "{alias:Q}" has {count:d} version')
def step_version_count_one(context, alias, count):
    step_version_count(context, alias, count)


@then('the vocabulary "{alias:Q}" has the latest version {version:d}')
def step_latest(context, alias, version):
    body = api.expect(_admin_get(context, "/vocabularies/{%s}" % alias), 200, "get vocabulary").json()
    assert body["version"] == version, f"latest version {body['version']}"


@then('the vocabulary "{alias:Q}" is unchanged')
def step_unchanged(context, alias):
    vocab = api.expect(_admin_get(context, "/vocabularies/{%s}" % alias), 200, "get vocabulary").json()
    versions = api.expect(_admin_get(context, "/vocabularies/{%s}/versions?pageSize=100" % alias), 200,
                          "get versions").json()["items"]
    before_vocab, before_versions = context.snapshots[alias]
    assert vocab == before_vocab, f"vocabulary changed:\n{before_vocab}\n{vocab}"
    assert versions == before_versions, "versions changed"


@then('no vocabulary named "{name:Q}" is stored')
def step_not_stored(context, name):
    want = S(context, name)
    items = _list_all(context, f"name={context.names.tok}")
    assert all(i["name"] != want for i in items), f"{want} was stored"


@then('no vocabulary with that name is stored')
def step_last_not_stored(context):
    name = context.last_name
    flt = name if re.match(r"^[A-Za-z0-9]{1,64}$", name) and context.names.tok not in name else context.names.tok
    items = _list_all(context, f"name={flt}")
    assert all(i["name"] != context.last_name for i in items), f"{context.last_name} was stored"


@then('the vocabulary with that name is stored')
def step_last_stored(context):
    resp = _admin_get(context, f"/vocabularies/{context.last_name}")
    assert resp.status_code == 200, f"{context.last_name}: {resp.status_code}"


@then("no vocabulary of this scenario is stored")
def step_none_stored(context):
    items = _list_all(context, f"name={context.names.tok}")
    assert items == [], f"stored: {[i['name'] for i in items]}"


@then("exactly {count:d} vocabularies of this scenario are stored")
def step_n_stored(context, count):
    items = _list_all(context, f"name={context.names.tok}")
    assert len(items) == count, f"stored: {[i['name'] for i in items]}"


@then("exactly {count:d} vocabulary of this scenario is stored")
def step_one_stored(context, count):
    step_n_stored(context, count)


@then('the external vocabulary "{alias:Q}" exists')
def step_ext_exists(context, alias):
    resp = _admin_get(context, "/external-vocabularies/{%s}" % alias)
    assert resp.status_code == 200, f"{resp.status_code}"


@then('the external vocabulary "{alias:Q}" does not exist')
def step_ext_missing(context, alias):
    resp = _admin_get(context, "/external-vocabularies/{%s}" % alias)
    assert resp.status_code == 404, f"{resp.status_code}"


# ---------------------------------------------------------------- lists, order and paging

def _names(context):
    return [i.get("name") for i in _json(context).get("items", [])]


@then('the response lists the vocabularies "{names:Q}" in this order')
def step_list_order(context, names):
    want = [S(context, n) for n in _csv(names)]
    assert _names(context) == want, f"expected {want}, got {_names(context)}"


@then('the response lists the vocabularies "{names:Q}"')
def step_list_set(context, names):
    want = sorted(S(context, n) for n in _csv(names))
    assert sorted(_names(context)) == want, f"expected {want}, got {_names(context)}"


@then("the response lists no items")
def step_list_empty(context):
    assert _json(context).get("items") == [], f"items: {_json(context).get('items')}"


@then('the response lists the versions "{versions:Q}" in this order')
def step_versions_order(context, versions):
    want = [int(v) for v in _csv(versions)]
    got = [i.get("version") for i in _json(context).get("items", [])]
    assert got == want, f"expected {want}, got {got}"


_VOCAB_SORT = {"name": "name", "status": "status", "creationDate": "creationDate", "lastUpdated": "lastUpdateDate"}


def _cmp(a, b):
    return (a > b) - (a < b)


@then('the vocabulary items are sorted by "{sort:Q}"')
def step_vocab_sorted(context, sort):
    desc = sort.startswith("-")
    field = _VOCAB_SORT[sort.lstrip("-")]
    items = _json(context)["items"]

    def cmp(x, y):
        c = _cmp(x[field], y[field])
        if desc:
            c = -c
        return c or _cmp(x["name"], y["name"])  # ties: name ascending (D23)

    want = sorted(items, key=functools.cmp_to_key(cmp))
    assert [i["name"] for i in items] == [i["name"] for i in want], (
        f"order {[ (i['name'], i[field]) for i in items]}")


@then('the version items are sorted by "{sort:Q}"')
def step_versions_sorted(context, sort):
    desc = sort.startswith("-")
    field = sort.lstrip("-")
    items = _json(context)["items"]

    def cmp(x, y):
        c = _cmp(x[field], y[field])
        if desc:
            c = -c
        return c or -_cmp(x["version"], y["version"])  # ties: version descending (D23)

    want = sorted(items, key=functools.cmp_to_key(cmp))
    assert [i["version"] for i in items] == [i["version"] for i in want], (
        f"order {[(i['version'], i[field]) for i in items]}")


@then("the page fields are page {page:d}, pageSize {size:d}, total {total:d}, totalPages {pages:d}")
def step_page_fields(context, page, size, total, pages):
    body = _json(context)
    got = (body.get("page"), body.get("pageSize"), body.get("total"), body.get("totalPages"))
    assert got == (page, size, total, pages), f"page fields {got}"


@then("the response has {count:d} items")
def step_item_count(context, count):
    assert len(_json(context).get("items", [])) == count, f"items: {len(_json(context).get('items', []))}"


@then('the response has the links "{links:Q}" and no others')
def step_links(context, links):
    want = set() if links == "none" else set(_csv(links))
    body = _json(context)
    got = {k for k in ("self", "first", "prev", "next", "last") if body.get(k)}
    assert got == want, f"links present: {got}, expected {want}"


@then('the link "{link:Q}" is "{value:Q}"')
def step_link(context, link, value):
    got = _json(context).get(link)
    assert got == S(context, value), f"{link} is {got!r}, expected {S(context, value)!r}"


@then("the external vocabulary items are sorted by name")
def step_ext_sorted(context):
    names = _names(context)
    assert names == sorted(names), f"not sorted by name: {names}"


@then('the external vocabulary list contains "{alias:Q}" on some page')
def step_ext_in_list(context, alias):
    name = context.names.get(alias)
    page = 1
    while True:
        resp = _admin_get(context, f"/external-vocabularies?pageSize=100&page={page}")
        api.expect(resp, 200, "list external vocabularies")
        body = resp.json()
        names = [i["name"] for i in body["items"]]
        assert names == sorted(names), f"page {page} not sorted by name"
        if name in names:
            return
        if page >= body.get("totalPages", 0):
            raise AssertionError(f"{name} not found in the external vocabulary list")
        page += 1


# ---------------------------------------------------------------- search results (D38)

@then("the search result has the items")
def step_search_items(context):
    items = _json(context).get("items", [])
    rows = list(context.table)
    assert len(items) == len(rows), f"expected {len(rows)} items, got {len(items)}: {items}"
    for item, row in zip(items, rows):
        for field in context.table.headings:
            _check_value(field, _get_path(item, field), S(context, row[field]))


@then('the external search result lists "{aliases:Q}" in this order')
def step_ext_search_order(context, aliases):
    want = [context.names.get(a) for a in _csv(aliases)]
    assert _names(context) == want, f"expected {want}, got {_names(context)}"


# ---------------------------------------------------------------- UI over HTTP (D123)

@when("I request the UI start page")
def step_ui_start(context):
    context.resp = send("GET", config.UI_URL + "/", absolute=True)


@then('the response body is the fixture "{fixture:Q}"')
def step_body_fixture(context, fixture):
    from vmtests.data import fixture_text
    want = fixture_text(fixture, context)
    assert context.resp.content == want, f"body differs from fixture {fixture}: {context.resp.content[:200]!r}"


@when("I register an external vocabulary with {count:d} aliases")
def step_register_aliases(context, count):
    name = context.names.get("Ext")
    tok = context.names.tok.lower()
    parts = [Part("name", name), Part("namespaceUri", f"https://ext-{tok}.example.org/ns")]
    for i in range(count):
        parts.append(Part("aliasUris", f"https://alias{i}-{tok}.example.org/ns"))
    parts.append(api.turtle_part(b"@prefix t: <https://x.example.org/ns#> .\nt:a t:b t:c .\n", f"{name}.ttl"))
    body, ctype = multipart_body(parts)
    _send(context, "POST", "/external-vocabularies", body=body, content_type=ctype)


@then("the response is a problem with one of the statuses {statuses}")
def step_problem_one_of(context, statuses):
    want = [int(x) for x in _csv(statuses)]
    assert context.resp.status_code in want, f"status {context.resp.status_code}, expected one of {want}"
    step_problem(context, context.resp.status_code)


@then('the response includes the vocabulary "{name:Q}"')
def step_includes(context, name):
    assert S(context, name) in _names(context), f"{S(context, name)} not in {_names(context)}"


@then('the response header "{header:Q}" does not contain "{value:Q}"')
def step_header_not_contains(context, header, value):
    got = context.resp.headers.get(header) or ""
    assert S(context, value) not in got, f"{header}: {got!r}"


@then("the Content-Security-Policy allows connections to 'self' and the identity provider")
def step_csp_connect(context):
    csp = context.resp.headers.get("Content-Security-Policy") or ""
    directives = {d.strip().split(" ", 1)[0]: d.strip() for d in csp.split(";") if d.strip()}
    connect = directives.get("connect-src", "")
    parts = urllib.parse.urlsplit(config.TOKEN_URL)
    idp = f"{parts.scheme}://{parts.netloc}"
    assert "'self'" in connect.split(), f"connect-src: {connect!r}"
    assert any(src.rstrip("/").startswith(idp) for src in connect.split()[1:]), (
        f"connect-src {connect!r} does not name the identity provider {idp}")


@given("{count:d} vocabularies of this scenario exist")
def step_n_vocabs(context, count):
    for i in range(count):
        _setup_create(context, f"V{i:02d}")
