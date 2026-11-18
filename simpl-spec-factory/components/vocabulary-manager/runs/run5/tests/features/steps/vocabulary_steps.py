"""Step definitions for the Vocabulary Manager API suite (one shared library).

Every name in feature text is expanded with vmtest.client.sub(): "{Alpha}" becomes
"Alpha" plus the scenario run id, so every scenario owns its data.
"""
import json
import os
import re
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime

from behave import given, when, then

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")))
from vmtest import client as c  # noqa: E402

ADMIN = ("role", c.ADMIN)

# Problem types per status (decisions D17, D44, D48 and the OpenAPI responses).
PROBLEM_TYPES = {
    400: "urn:problem-type:belgif:badRequest",
    401: "urn:problem-type:simpl:unauthorized",
    403: "urn:problem-type:simpl:forbidden",
    404: "urn:problem-type:belgif:resourceNotFound",
    409: "urn:problem-type:simpl:conflict",
    413: "urn:problem-type:simpl:payloadTooLarge",
    415: "urn:problem-type:simpl:unsupportedMediaType",
    503: "urn:problem-type:belgif:serviceUnavailable",
}
PROBLEM_TITLES = {401: "Unauthorized", 403: "Forbidden", 503: "Service Unavailable"}
LEAK_PATTERNS = [r"Exception", r"\bat [a-z]+\.[a-z]+\.", r"java\.", r"org\.springframework",
                 r"org\.apache", r"stack ?trace", r"NullPointer", r"SQL", r"SPARQL", r"fuseki"]


# =========================================================================== helpers

def _json(response):
    try:
        return response.json()
    except ValueError:
        raise AssertionError(f"response is not JSON: {c.describe(response)}")


def get_path(obj, path):
    """Return (found, value) for a dotted path with [index] parts, e.g. items[0].name."""
    cur = obj
    for part in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if part.startswith("["):
            idx = int(part[1:-1])
            if not isinstance(cur, list) or idx >= len(cur):
                return False, None
            cur = cur[idx]
        else:
            if not isinstance(cur, dict) or part not in cur:
                return False, None
            cur = cur[part]
    return True, cur


def _field(context, path, obj=None):
    body = _json(context.response) if obj is None else obj
    found, value = get_path(body, path)
    assert found, f"field {path} missing: {c.describe(context.response)}"
    return value


def _literal(context, text):
    text = c.sub(context, text)
    try:
        return json.loads(text)
    except ValueError:
        raise AssertionError(f"not a JSON literal: {text}")


def _set_response(context, response):
    context.response = response
    return response


def _send(context, method, path, **kwargs):
    return _set_response(context, c.request(context, method, c.sub(context, path), **kwargs))


def _admin_get(context, path):
    return c.request(context, "GET", c.sub(context, path), auth=ADMIN)


def _create(context, name, content, description=None, changelog="Initial version.", auth=None):
    r = c.request(context, "POST", "/vocabularies", auth=auth,
                  files=c.multipart(c.create_fields(name, content, description, changelog)))
    c.track_created(context, r, content)
    return r


def _new_version(context, name, content, description, changelog, auth=None):
    r = c.request(context, "POST", f"/vocabularies/{name}/versions", auth=auth,
                  files=c.multipart(c.version_fields(content, description, changelog)))
    c.track_created(context, r, content)
    return r


def _next_description(context):
    context.counter += 1
    return f"Description {context.counter} of run {context.run}."


def _register(context, name, ns, aliases=(), auth=None, content=None, description=None):
    content = content if content is not None else (
        f"@prefix ext: <{ns}> .\n@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
        f"ext:Term a rdfs:Class ;\n  rdfs:label \"Term of {name}\"@en .\n").encode("utf-8")
    fields = [("name", name), ("namespaceUri", ns)]
    fields += [("aliasUris", a) for a in aliases]
    fields += [("sourceUri", "https://source.example.org/" + name + ".ttl"),
               ("description", description or f"Cached copy of {name}."),
               ("vocabularyFile", ("file", content))]
    r = c.request(context, "POST", "/external-vocabularies", auth=auth, files=c.multipart(fields))
    if r.status_code == 201:
        context.ext_uploaded[name] = content
        context.ext_ns[name] = ns
    return r


def _form_fields(context, table, path_name=None):
    fields, name, file_spec = [], path_name, None
    for row in table:
        key, value = row["field"], row["value"]
        if key == "vocabularyFile":
            file_spec = value
            fields.append((key, None))
        else:
            value = c.sub(context, value)
            if key == "name":
                name = value
            fields.append((key, value))
    content = None
    if file_spec is not None:
        if file_spec.startswith("version:"):
            content = context.uploaded[(path_name, int(file_spec.split(":")[1]))]
        else:
            content = c.file_value(context, file_spec, name or "Unnamed")
        fields = [(k, ("file", content)) if k == "vocabularyFile" else (k, v) for k, v in fields]
    return fields, name, content


def _all_external(context):
    names, page = [], 1
    while True:
        r = c.request(context, "GET", f"/external-vocabularies?page={page}&pageSize=100", auth=ADMIN)
        assert r.status_code == 200, c.describe(r)
        body = r.json()
        names += [i.get("name") for i in body.get("items", [])]
        if page >= body.get("totalPages", 0):
            return names
        page += 1


def _run_names(context):
    r = c.request(context, "GET", f"/vocabularies?name={context.run}&pageSize=100", auth=ADMIN)
    assert r.status_code == 200, c.describe(r)
    return [i.get("name") for i in r.json().get("items", [])]


def _parse_time(value):
    return datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ")


# =========================================================================== who am I

@given('I am a Governance Authority user with role "{role:Q}"')
def step_role(context, role):
    context.auth = ("role", role)


@given('I am an authenticated user with no vocabulary management role')
def step_norole(context):
    context.auth = ("role", "NONE")


@given('I am not authenticated')
def step_no_token(context):
    context.auth = ("none", None)


@given('I send a token with a tampered signature')
def step_tampered(context):
    context.auth = ("raw", "Bearer " + c.tampered(c.token_for(c.ADMIN)))


@given('I send an unsigned token')
def step_unsigned(context):
    context.auth = ("raw", "Bearer " + c.unsigned(c.token_for(c.ADMIN)))


@given('I send an expired token')
def step_expired(context):
    context.auth = ("raw", "Bearer " + c.expired_looking(c.token_for(c.ADMIN)))


@given('I send the authorization header "{value:Q}"')
def step_raw_auth(context, value):
    context.auth = ("raw", value)


@given('the API runs with an unreachable store')
def step_store_down(context):
    assert c.STORE_DOWN_API_URL, "STORE_DOWN_API_URL is not set"
    context.api_url = c.STORE_DOWN_API_URL


# =========================================================================== setup data (as admin)

@given('a vocabulary "{name:Q}" exists')
def step_vocab_exists(context, name):
    name = c.sub(context, name)
    r = _create(context, name, c.default_ttl(name), auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('a vocabulary "{name:Q}" exists with content')
def step_vocab_exists_content(context, name):
    name = c.sub(context, name)
    r = _create(context, name, c.sub(context, context.text).encode("utf-8"), auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('a vocabulary "{name:Q}" exists with the description "{description:Q}"')
def step_vocab_exists_descr(context, name, description):
    name = c.sub(context, name)
    r = _create(context, name, c.default_ttl(name), description=c.sub(context, description), auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('a vocabulary "{name:Q}" exists with {n:d} versions')
def step_vocab_versions(context, name, n):
    step_vocab_exists(context, name)
    name = c.sub(context, name)
    for v in range(2, n + 1):
        r = _new_version(context, name, c.default_ttl(name, extra_concept=v),
                         _next_description(context), f"Change {v}.", auth=ADMIN)
        assert r.status_code == 201, c.describe(r)


@given('a new version of "{name:Q}" exists')
def step_given_new_version(context, name):
    name = c.sub(context, name)
    context.counter += 1
    r = _new_version(context, name, c.default_ttl(name, extra_concept=100 + context.counter),
                     _next_description(context), "Another change.", auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('a new version of "{name:Q}" exists with content')
def step_given_new_version_content(context, name):
    name = c.sub(context, name)
    r = _new_version(context, name, c.sub(context, context.text).encode("utf-8"),
                     _next_description(context), "Another change.", auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('the vocabulary "{name:Q}" has status "{status:Q}"')
def step_given_status(context, name, status):
    r = c.request(context, "PATCH", "/vocabularies/" + c.sub(context, name), auth=ADMIN,
                  json={"status": status})
    assert r.status_code == 200, c.describe(r)


@given('version {v:d} of "{name:Q}" has status "{status:Q}"')
def step_given_version_status(context, v, name, status):
    r = c.request(context, "PATCH", f"/vocabularies/{c.sub(context, name)}/versions/{v}", auth=ADMIN,
                  json={"status": status})
    assert r.status_code == 200, c.describe(r)


@given('an external vocabulary "{name:Q}" is registered with namespace "{ns:Q}"')
def step_ext_registered(context, name, ns):
    r = _register(context, c.sub(context, name), c.sub(context, ns), auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('an external vocabulary "{name:Q}" is registered with namespace "{ns:Q}" and alias "{alias:Q}"')
def step_ext_registered_alias(context, name, ns, alias):
    r = _register(context, c.sub(context, name), c.sub(context, ns), [c.sub(context, alias)], auth=ADMIN)
    assert r.status_code == 201, c.describe(r)


@given('I wait until the next second')
@when('I wait until the next second')
def step_wait_second(context):
    time.sleep(1.1)


# =========================================================================== actions

@when('I create the vocabulary "{name:Q}"')
def step_create(context, name):
    name = c.sub(context, name)
    _set_response(context, _create(context, name, c.default_ttl(name)))


@when('I create the vocabulary "{name:Q}" with the fixture "{fixture:Q}"')
def step_create_fixture(context, name, fixture):
    name = c.sub(context, name)
    _set_response(context, _create(context, name, c.fixture_bytes(context, fixture)))


@when('I create the vocabulary "{name:Q}" with content')
def step_create_content(context, name):
    name = c.sub(context, name)
    _set_response(context, _create(context, name, c.sub(context, context.text).encode("utf-8")))


@when('I create a vocabulary with the form')
def step_create_form(context):
    fields, _, content = _form_fields(context, context.table)
    r = c.request(context, "POST", "/vocabularies", files=c.multipart(fields))
    c.track_created(context, r, content)
    _set_response(context, r)


@when('I create a new version of "{name:Q}"')
def step_new_version(context, name):
    name = c.sub(context, name)
    context.counter += 1
    _set_response(context, _new_version(context, name, c.default_ttl(name, extra_concept=200 + context.counter),
                                        _next_description(context), "A change."))


@when('I create a new version of "{name:Q}" with the fixture "{fixture:Q}"')
def step_new_version_fixture(context, name, fixture):
    name = c.sub(context, name)
    _set_response(context, _new_version(context, name, c.fixture_bytes(context, fixture),
                                        _next_description(context), "A change."))


@when('I create a new version of "{name:Q}" with content')
def step_new_version_content(context, name):
    name = c.sub(context, name)
    _set_response(context, _new_version(context, name, c.sub(context, context.text).encode("utf-8"),
                                        _next_description(context), "A change."))


@when('I create a new version of "{name:Q}" with the form')
def step_new_version_form(context, name):
    name = c.sub(context, name)
    fields, _, content = _form_fields(context, context.table, path_name=name)
    r = c.request(context, "POST", f"/vocabularies/{name}/versions", files=c.multipart(fields))
    c.track_created(context, r, content)
    _set_response(context, r)


@when('I change the status of "{name:Q}" to "{status:Q}"')
def step_patch_status(context, name, status):
    _send(context, "PATCH", "/vocabularies/" + name, json={"status": status})


@when('I change the status of version {v:d} of "{name:Q}" to "{status:Q}"')
def step_patch_version_status(context, v, name, status):
    _send(context, "PATCH", f"/vocabularies/{name}/versions/{v}", json={"status": status})


@when('I register the external vocabulary "{name:Q}" with namespace "{ns:Q}"')
def step_register(context, name, ns):
    _set_response(context, _register(context, c.sub(context, name), c.sub(context, ns)))


@when('I register the external vocabulary "{name:Q}" with namespace "{ns:Q}" and alias "{alias:Q}"')
def step_register_alias(context, name, ns, alias):
    _set_response(context, _register(context, c.sub(context, name), c.sub(context, ns), [c.sub(context, alias)]))


@when('I register the external vocabulary "{name:Q}" with namespace "{ns:Q}" and {n:d} aliases')
def step_register_many_aliases(context, name, ns, n):
    aliases = [f"https://alias{i}-{context.run}.example.org/ns" for i in range(n)]
    _set_response(context, _register(context, c.sub(context, name), c.sub(context, ns), aliases))


@when('I register an external vocabulary with the form')
def step_register_form(context):
    fields, name, content = _form_fields(context, context.table)
    r = c.request(context, "POST", "/external-vocabularies", files=c.multipart(fields))
    if r.status_code == 201 and name:
        context.ext_uploaded[name] = content
        for k, v in fields:
            if k == "namespaceUri":
                context.ext_ns[name] = v
    _set_response(context, r)


@when('I send "{method:Q}" to "{path:Q}"')
def step_send(context, method, path):
    _send(context, method, path)


@when('I send "{method:Q}" to "{path:Q}" with JSON')
def step_send_json(context, method, path):
    body = c.sub(context, context.text)
    _send(context, method, path, data=body.encode("utf-8"), headers={"Content-Type": "application/json"})


@when('I send "{method:Q}" to "{path:Q}" with the body "{body:Q}" as "{ctype:Q}"')
def step_send_body(context, method, path, body, ctype):
    _send(context, method, path, data=c.sub(context, body).encode("utf-8"), headers={"Content-Type": ctype})


@when('I send "{method:Q}" to "{path:Q}" accepting "{accept:Q}"')
def step_send_accept(context, method, path, accept):
    _send(context, method, path, headers={"Accept": accept})


@when('I send "{method:Q}" to "{path:Q}" with the form')
def step_send_form(context, method, path):
    path = c.sub(context, path)
    m = re.match(r"/vocabularies/([^/?]+)/versions", path)
    fields, _, content = _form_fields(context, context.table, path_name=m.group(1) if m else None)
    r = c.request(context, method, path, files=c.multipart(fields))
    c.track_created(context, r, content)
    _set_response(context, r)


@when('I search vocabularies with {n:d} distinct names')
def step_search_many(context, n):
    items = [{"name": f"Missing{i}{context.run}"} for i in range(n)]
    _send(context, "POST", "/vocabularies/searches", json={"items": items})


@when('I search external vocabularies with {n:d} namespace URIs and {m:d} alias URIs')
def step_ext_search_many(context, n, m):
    body = {}
    if n:
        body["namespaceUris"] = [f"https://ns{i}-{context.run}.example.org/x" for i in range(n)]
    if m:
        body["aliasUris"] = [f"https://al{i}-{context.run}.example.org/x" for i in range(m)]
    _send(context, "POST", "/external-vocabulary/searches", json=body)


# --------------------------------------------------------------------------- operations by id

def _operation(context, op):
    """Request of one OpenAPI operation against the scenario's background data:
    vocabulary {Subject} (version 1) and external vocabulary {ExtSubject}."""
    s = c.alias_name(context, "Subject")
    ext = c.alias_name(context, "ExtSubject")
    new = c.alias_name(context, "New")
    ext_new = c.alias_name(context, "ExtNew")
    ext_ns = context.ext_ns.get(ext, f"https://ext-{context.run}.example.org/subject#")
    if op == "getVocabularies":
        return "GET", f"/vocabularies?name={context.run}", {}
    if op == "createVocabulary":
        return "POST", "/vocabularies", {"files": c.multipart(c.create_fields(new, c.default_ttl(new)))}
    if op == "searchVocabularies":
        return "POST", "/vocabularies/searches", {"json": {"items": [{"name": s}]}}
    if op == "getVocabulary":
        return "GET", f"/vocabularies/{s}", {}
    if op == "updateVocabularyStatus":
        return "PATCH", f"/vocabularies/{s}", {"json": {"status": "DEPRECATED"}}
    if op == "getVocabularyVersions":
        return "GET", f"/vocabularies/{s}/versions", {}
    if op == "createVocabularyVersion":
        return "POST", f"/vocabularies/{s}/versions", {"files": c.multipart(c.version_fields(
            c.default_ttl(s, extra_concept=2), "Second version.", "Second change."))}
    if op == "getVocabularyVersion":
        return "GET", f"/vocabularies/{s}/versions/1", {}
    if op == "updateVocabularyVersionStatus":
        return "PATCH", f"/vocabularies/{s}/versions/1", {"json": {"status": "DEPRECATED"}}
    if op == "downloadVocabulary":
        return "GET", f"/content/{s}", {}
    if op == "downloadVocabularyVersion":
        return "GET", f"/content/{s}/versions/1", {}
    if op == "getExternalVocabularies":
        return "GET", "/external-vocabularies", {}
    if op == "createExternalVocabulary":
        ns = f"https://ext-{context.run}.example.org/new#"
        return "POST", "/external-vocabularies", {"files": c.multipart([
            ("name", ext_new), ("namespaceUri", ns),
            ("vocabularyFile", ("file", f"@prefix n: <{ns}> .\nn:T a n:C .\n".encode()))])}
    if op == "searchExternalVocabularies":
        return "POST", "/external-vocabulary/searches", {"json": {"namespaceUri": ext_ns}}
    if op == "downloadExternalVocabularyContent":
        return "POST", "/external-vocabularies/content", {"json": {"namespaceUri": ext_ns}}
    if op == "getExternalVocabularyContentByName":
        return "GET", f"/external-vocabularies/{ext}/content", {}
    if op == "getExternalVocabulary":
        return "GET", f"/external-vocabularies/{ext}", {}
    if op == "deleteExternalVocabulary":
        return "DELETE", f"/external-vocabularies/{ext}", {}
    raise AssertionError(f"unknown operation {op}")


@when('I call the operation "{op:Q}"')
def step_call_operation(context, op):
    method, path, kwargs = _operation(context, op)
    _set_response(context, c.request(context, method, path, **kwargs))


@when('I call the operation "{op:Q}" with the query parameter "{param:Q}"')
def step_call_operation_param(context, op, param):
    method, path, kwargs = _operation(context, op)
    path += ("&" if "?" in path else "?") + c.sub(context, param)
    _set_response(context, c.request(context, method, path, **kwargs))


# --------------------------------------------------------------------------- parallel

def _parallel(context, n, fn):
    with ThreadPoolExecutor(max_workers=n) as pool:
        context.responses = list(pool.map(fn, range(n)))
    context.response = context.responses[-1]


@when('I create {n:d} new versions of "{name:Q}" in parallel')
def step_parallel_versions(context, n, name):
    name = c.sub(context, name)
    token = c.token_for(context.auth[1]) if context.auth[0] == "role" else None

    def one(i):
        content = c.default_ttl(name, extra_concept=1000 + i)
        r = c.request(context, "POST", f"/vocabularies/{name}/versions",
                      auth=("raw", f"Bearer {token}") if token else None,
                      files=c.multipart(c.version_fields(content, f"Parallel description {i}.", f"Parallel {i}.")))
        return r
    _parallel(context, n, one)
    for r in context.responses:
        if r.status_code == 201:
            c.remember_content_base(r.json().get("contentHref"), name, r.json().get("version"))


@when('I create the vocabulary "{name:Q}" {n:d} times in parallel')
def step_parallel_create(context, name, n):
    name = c.sub(context, name)
    token = c.token_for(context.auth[1])

    def one(i):
        return c.request(context, "POST", "/vocabularies", auth=("raw", f"Bearer {token}"),
                         files=c.multipart(c.create_fields(name, c.default_ttl(name))))
    _parallel(context, n, one)


@when('I change the status of "{name:Q}" to "{status:Q}" {n:d} times in parallel')
def step_parallel_patch(context, name, status, n):
    name = c.sub(context, name)
    token = c.token_for(context.auth[1])

    def one(i):
        return c.request(context, "PATCH", f"/vocabularies/{name}", auth=("raw", f"Bearer {token}"),
                         json={"status": status})
    _parallel(context, n, one)


@then('the parallel responses have the statuses {statuses}')
def step_parallel_statuses(context, statuses):
    got = sorted(r.status_code for r in context.responses)
    want = sorted(json.loads(statuses))
    assert got == want, f"statuses {got}, expected {want}: " + " | ".join(c.describe(r) for r in context.responses)


@then('the parallel responses created the versions {versions}')
def step_parallel_versions_created(context, versions):
    got = sorted(r.json()["version"] for r in context.responses if r.status_code == 201)
    assert got == sorted(json.loads(versions)), f"versions {got}"


@then('the parallel responses have distinct Location headers')
def step_parallel_locations(context):
    locs = [r.headers.get("Location") for r in context.responses]
    assert None not in locs and len(set(locs)) == len(locs), f"Location headers {locs}"


# =========================================================================== status and problems

@then('the response status is {code:d}')
def step_status(context, code):
    r = context.response
    assert r.status_code == code, f"expected {code}: {c.describe(r)}"
    if r.content:
        assert r.headers.get("Content-Type"), "response with a body has no Content-Type (API-36)"


@then('the response is a problem with status {code:d}')
def step_problem(context, code):
    r = context.response
    assert r.status_code == code, f"expected {code}: {c.describe(r)}"
    ctype = r.headers.get("Content-Type", "")
    assert ctype.startswith("application/problem+json"), f"Content-Type {ctype}: {c.describe(r)}"
    body = _json(r)
    assert body.get("status") == code, f"problem status {body.get('status')}"
    if code in PROBLEM_TYPES:
        assert body.get("type") == PROBLEM_TYPES[code], f"problem type {body.get('type')}, expected {PROBLEM_TYPES[code]}"
    if code in PROBLEM_TITLES:
        assert body.get("title") == PROBLEM_TITLES[code], f"problem title {body.get('title')}"


@then('the response status is {code:d} with a problem body')
def step_problem_free_type(context, code):
    r = context.response
    assert r.status_code == code, f"expected {code}: {c.describe(r)}"
    ctype = r.headers.get("Content-Type", "")
    assert ctype.startswith("application/problem+json"), f"Content-Type {ctype}"
    assert _json(r).get("status") == code


@then('the problem type follows the simpl URN form')
def step_problem_simpl_urn(context):
    t = _json(context.response).get("type", "")
    assert re.match(r"^urn:problem-type:simpl:([a-z][a-zA-Z0-9]*)(:[a-z][a-zA-Z0-9]*){0,2}$", t), \
        f"type {t} is not a simpl URN with lowerCamelCase parts (API-46, API-47)"


@then('the problem detail is "{text:Q}"')
def step_problem_detail(context, text):
    got = _json(context.response).get("detail")
    want = c.sub(context, text)
    assert got == want, f"detail {got!r}, expected {want!r}"


@then('the problem detail is present')
def step_problem_detail_present(context):
    d = _json(context.response).get("detail")
    assert isinstance(d, str) and d.strip(), f"no detail: {c.describe(context.response)}"


@then('the problem reveals no implementation details')
def step_no_leak(context):
    text = context.response.text
    for p in LEAK_PATTERNS:
        assert not re.search(p, text), f"problem body matches {p}: {text[:500]}"


def _issues(context):
    issues = _json(context.response).get("issues")
    assert isinstance(issues, list), f"no issues array: {c.describe(context.response)}"
    return issues


@then('the problem has exactly {n:d} issue')
@then('the problem has exactly {n:d} issues')
def step_issue_count(context, n):
    issues = _issues(context)
    assert len(issues) == n, f"{len(issues)} issues, expected {n}: {issues}"


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_in_named(context, where, name):
    issues = _issues(context)
    assert any(i.get("in") == where and i.get("name") == c.sub(context, name) for i in issues), \
        f"no issue in {where} named {name}: {issues}"


@then('every issue is in "{where:Q}" named "{name:Q}"')
def step_every_issue(context, where, name):
    issues = _issues(context)
    assert issues and all(i.get("in") == where and i.get("name") == name for i in issues), issues


@then('every issue has the type "{t:Q}"')
def step_every_issue_type(context, t):
    issues = _issues(context)
    assert issues and all(i.get("type") == t for i in issues), issues


@then('the problem has an issue with detail "{d:Q}"')
def step_issue_detail(context, d):
    want = c.sub(context, d)
    issues = _issues(context)
    assert any(i.get("detail") == want for i in issues), f"no issue with detail {want!r}: {issues}"


@then('the problem has an issue with detail starting "{d:Q}"')
def step_issue_detail_start(context, d):
    want = c.sub(context, d)
    issues = _issues(context)
    assert any(str(i.get("detail", "")).startswith(want) for i in issues), f"no issue starting {want!r}: {issues}"


@then('the issue with detail "{d:Q}" has location "{loc:Q}"')
def step_issue_location(context, d, loc):
    want_d, want_l = c.sub(context, d), c.sub(context, loc)
    issues = [i for i in _issues(context) if i.get("detail") == want_d]
    assert issues and any(i.get("location") == want_l for i in issues), f"locations {issues}"


@then('the issue with detail "{d:Q}" has a location containing "{s:Q}"')
def step_issue_location_contains(context, d, s):
    want_d, want_s = c.sub(context, d), c.sub(context, s)
    issues = [i for i in _issues(context) if i.get("detail") == want_d]
    assert issues and any(want_s in str(i.get("location", "")) for i in issues), f"locations {issues}"


@then('the first issue has a location matching "{regex:Q}"')
def step_issue_location_regex(context, regex):
    issues = _issues(context)
    loc = str(issues[0].get("location", ""))
    assert re.match(regex, loc), f"location {loc!r} does not match {regex}"


@then('the response has no "{field:Q}" field')
def step_no_field(context, field):
    if context.response.content:
        try:
            body = context.response.json()
        except ValueError:
            return
        assert not (isinstance(body, dict) and field in body), f"unexpected field {field}: {body}"


# =========================================================================== headers and bodies

@then('the response content type is "{ctype:Q}"')
def step_content_type(context, ctype):
    got = context.response.headers.get("Content-Type", "")
    assert got.split(";")[0].strip() == ctype, f"Content-Type {got}"


@then('the response header "{h:Q}" is "{v:Q}"')
def step_header_is(context, h, v):
    got = context.response.headers.get(h)
    assert got == c.sub(context, v), f"header {h}: {got!r}, expected {c.sub(context, v)!r}"


@then('the response header "{h:Q}" contains "{v:Q}"')
def step_header_contains(context, h, v):
    got = context.response.headers.get(h) or ""
    assert c.sub(context, v) in got, f"header {h}: {got!r}"


@then('the response body is the content of version {v:d} of "{name:Q}"')
def step_body_version(context, v, name):
    want = context.uploaded[(c.sub(context, name), v)]
    assert context.response.content == want, "downloaded bytes differ from the uploaded bytes"


@then('the response body is the content of the external vocabulary "{name:Q}"')
def step_body_ext(context, name):
    want = context.ext_uploaded[c.sub(context, name)]
    assert context.response.content == want, "downloaded bytes differ from the uploaded bytes"


@then('the response body is empty')
def step_body_empty(context):
    assert not context.response.content, f"body: {context.response.text[:300]}"


# =========================================================================== JSON fields

@then('the response field "{path:Q}" equals {value}')
def step_field_equals(context, path, value):
    got, want = _field(context, c.sub(context, path)), _literal(context, value)
    assert got == want, f"{path}: {got!r}, expected {want!r}"


@then('the response field "{path:Q}" is "{value:Q}"')
def step_field_is(context, path, value):
    got, want = _field(context, c.sub(context, path)), c.sub(context, value)
    assert got == want, f"{path}: {got!r}, expected {want!r}"


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    found, value = get_path(_json(context.response), path)
    assert not found, f"{path} present: {value!r}"


@then('the response field "{path:Q}" is present')
def step_field_present(context, path):
    _field(context, path)


@then('the response field "{path:Q}" is absent or empty')
def step_field_absent_or_empty(context, path):
    found, value = get_path(_json(context.response), path)
    assert not found or value in ([], None), f"{path}: {value!r}"


@then('the response field "{path:Q}" ends with "{s:Q}"')
def step_field_ends(context, path, s):
    got = _field(context, path)
    assert str(got).endswith(c.sub(context, s)), f"{path}: {got!r}"


@then('the response field "{path:Q}" contains "{s:Q}"')
def step_field_contains(context, path, s):
    got = _field(context, path)
    assert c.sub(context, s) in str(got), f"{path}: {got!r}"


@then('the response field "{path:Q}" matches "{regex:Q}"')
def step_field_matches(context, path, regex):
    got = _field(context, path)
    assert re.match(regex, str(got)), f"{path}: {got!r} !~ {regex}"


@then('the response field "{path:Q}" is a UTC time to the second')
def step_field_utc(context, path):
    got = _field(context, path)
    assert re.match(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$", str(got)), f"{path}: {got!r} (D58)"


@then('the response field "{path:Q}" identifies the user with role "{role:Q}"')
def step_field_creator(context, path, role):
    got = _field(context, path)
    assert got == c.creator_of(role), f"{path}: {got!r}, expected {c.creator_of(role)!r} (D27)"


@then('the response field "{path:Q}" is not earlier than the remembered "{key:Q}"')
def step_field_not_earlier(context, path, key):
    got = _parse_time(_field(context, path))
    assert got >= _parse_time(context.saved[key]), f"{path} {got} earlier than {context.saved[key]}"


@given('I remember the response field "{path:Q}" as "{key:Q}"')
@when('I remember the response field "{path:Q}" as "{key:Q}"')
@then('I remember the response field "{path:Q}" as "{key:Q}"')
def step_remember(context, path, key):
    context.saved[key] = _field(context, path)


@then('the response has {n:d} items')
@then('the response has {n:d} item')
def step_item_count(context, n):
    items = _field(context, "items")
    assert isinstance(items, list) and len(items) == n, f"{len(items)} items, expected {n}: {items}"


@then('the response items have the "{field:Q}" values {values}')
def step_item_values(context, field, values):
    items = _field(context, "items")
    got = [i.get(field) for i in items]
    want = _literal(context, values)
    assert got == want, f"{field} values {got}, expected {want}"


@then('every item has the fields "{fields:Q}"')
def step_item_fields(context, fields):
    items = _field(context, "items")
    assert items, "no items"
    for item in items:
        for f in [x.strip() for x in fields.split(",")]:
            assert f in item, f"item without {f}: {item}"


@then('the response has the paging fields')
def step_paging(context):
    body = _json(context.response)
    for f in ("page", "pageSize", "total", "totalPages", "self", "first", "items"):
        assert f in body, f"paging field {f} missing: {body}"


@then('the response has no paging links')
def step_no_links(context):
    body = _json(context.response)
    for f in ("self", "first", "prev", "next", "last"):
        assert body.get(f) is None, f"unexpected link {f}: {body.get(f)}"


@then('the response has the links {links}')
def step_links_present(context, links):
    body = _json(context.response)
    for f in json.loads(links):
        assert body.get(f), f"link {f} missing: {body}"


@then('the response has none of the links {links}')
def step_links_absent(context, links):
    body = _json(context.response)
    for f in json.loads(links):
        assert body.get(f) is None, f"link {f} present: {body.get(f)}"


@then('the "{link:Q}" link points to page {n:d}')
def step_link_page(context, link, n):
    url = _field(context, link)
    assert re.search(rf"[?&]page={n}(&|$)", url), f"{link} = {url}"


# =========================================================================== verification reads (admin)

@then('the vocabulary "{name:Q}" has status "{status:Q}"')
def step_check_status(context, name, status):
    r = _admin_get(context, "/vocabularies/" + name)
    assert r.status_code == 200 and r.json().get("status") == status, c.describe(r)


@then('version {v:d} of "{name:Q}" has status "{status:Q}"')
def step_check_version_status(context, v, name, status):
    r = _admin_get(context, f"/vocabularies/{name}/versions/{v}")
    assert r.status_code == 200 and r.json().get("status") == status, c.describe(r)


@then('the vocabulary "{name:Q}" has {n:d} versions')
@then('the vocabulary "{name:Q}" has {n:d} version')
def step_check_version_count(context, name, n):
    r = _admin_get(context, f"/vocabularies/{name}/versions?pageSize=100")
    assert r.status_code == 200 and r.json().get("total") == n, c.describe(r)


@then('the vocabulary "{name:Q}" has the field "{path:Q}" equal to {value}')
def step_check_vocab_field(context, name, path, value):
    r = _admin_get(context, "/vocabularies/" + name)
    assert r.status_code == 200, c.describe(r)
    found, got = get_path(r.json(), path)
    want = _literal(context, value)
    assert found and got == want, f"{path}: {got!r}, expected {want!r}"


@then('version {v:d} of "{name:Q}" has the field "{path:Q}" equal to {value}')
def step_check_version_field(context, v, name, path, value):
    r = _admin_get(context, f"/vocabularies/{name}/versions/{v}")
    assert r.status_code == 200, c.describe(r)
    found, got = get_path(r.json(), path)
    want = _literal(context, value)
    assert found and got == want, f"{path}: {got!r}, expected {want!r}"


@then('the "{p1:Q}" of version {a:d} of "{name:Q}" equals the "{p2:Q}" of version {b:d}')
def step_compare_versions(context, p1, a, name, p2, b):
    ra = _admin_get(context, f"/vocabularies/{name}/versions/{a}")
    rb = _admin_get(context, f"/vocabularies/{name}/versions/{b}")
    assert ra.status_code == 200 and rb.status_code == 200
    assert ra.json().get(p1) == rb.json().get(p2), f"{p1}={ra.json().get(p1)} vs {p2}={rb.json().get(p2)}"


@then('the "{p1:Q}" of the vocabulary "{name:Q}" equals the "{p2:Q}" of version {b:d}')
def step_compare_vocab_version(context, p1, name, p2, b):
    ra = _admin_get(context, f"/vocabularies/{name}")
    rb = _admin_get(context, f"/vocabularies/{name}/versions/{b}")
    assert ra.status_code == 200 and rb.status_code == 200
    assert ra.json().get(p1) == rb.json().get(p2), f"{p1}={ra.json().get(p1)} vs {p2}={rb.json().get(p2)}"


@then('no vocabulary named "{name:Q}" is stored')
def step_not_stored(context, name):
    name = c.sub(context, name)
    names = _run_names(context)
    assert not any(n.lower() == name.lower() for n in names), f"{name} is stored: {names}"


@then('exactly one vocabulary named "{name:Q}" is stored')
def step_stored_once(context, name):
    name = c.sub(context, name)
    names = _run_names(context)
    assert [n.lower() for n in names].count(name.lower()) == 1, f"names {names}"


@then('no vocabulary of this scenario is stored')
def step_none_stored(context):
    names = _run_names(context)
    assert names == [], f"stored: {names}"


@then('no external vocabulary named "{name:Q}" is registered')
def step_ext_not_registered(context, name):
    name = c.sub(context, name)
    assert name not in _all_external(context)


@then('the external vocabulary "{name:Q}" is registered')
def step_ext_registered_check(context, name):
    r = _admin_get(context, "/external-vocabularies/" + name)
    assert r.status_code == 200, c.describe(r)


@then('the content of version {v:d} of "{name:Q}" is unchanged')
def step_content_unchanged(context, v, name):
    r = _admin_get(context, f"/content/{name}/versions/{v}")
    assert r.status_code == 200, c.describe(r)
    assert r.content == context.uploaded[(c.sub(context, name), v)]


@then('the vocabulary list filtered by name "{name:Q}" has the names {names}')
def step_list_names(context, name, names):
    r = _admin_get(context, f"/vocabularies?name={name}&pageSize=100")
    assert r.status_code == 200, c.describe(r)
    got = [i.get("name") for i in r.json().get("items", [])]
    assert got == _literal(context, names), f"names {got}"


@then('the external vocabulary list shows "{first:Q}" before "{second:Q}"')
def step_ext_order(context, first, second):
    names = _all_external(context)
    a, b = c.sub(context, first), c.sub(context, second)
    assert a in names and b in names, f"{a} or {b} missing"
    assert names.index(a) < names.index(b), f"order: {names}"
    assert names == sorted(names), "the external vocabulary list is not in name order"
