"""The one shared step library of the Vocabulary Manager API suite."""
import concurrent.futures
import json
import re
import time
import uuid

import parse
import requests
from behave import given, when, then, register_type

import vm


@parse.with_pattern(r'(?:[^"\\]|\\.)*')
def _quoted(text):
    return text.replace('\\"', '"')


register_type(Q=_quoted)


@parse.with_pattern(r"a vocabulary administrator|a vocabulary viewer|a user without a vocabulary role|not authenticated")
def _who(text):
    return text


register_type(Who=_who)  # keeps "I am ..." from matching the UI steps

IDENTITIES = {
    "a vocabulary administrator": "admin",
    "a vocabulary viewer": "viewer",
    "a user without a vocabulary role": "norole",
}

# ======================================================================= helpers


def X(context, text):
    return vm.expand(text, context)


def name_of(context, alias):
    return vm.vocab_name(context.sid, alias)


def resp(context):
    assert context.resp is not None, "no response yet"
    return context.resp


def body(context):
    return vm.body_json(resp(context))


def remember_upload(context, name, version, content):
    context.contents[(name, int(version))] = content


def next_turtle(context, alias):
    context.revision += 1
    return vm.default_turtle(name_of(context, alias), vm.namespace(context.sid, alias), context.revision)


def admin_create(context, alias, content=None, description=None):
    name = name_of(context, alias)
    content = content if content is not None else next_turtle(context, alias)
    r = vm.create_vocabulary(name, content, description=description)
    assert r.status_code == 201, "setup: " + vm.describe(r)
    remember_upload(context, name, 1, content)
    return r


def admin_new_version(context, alias, content=None, description=None):
    name = name_of(context, alias)
    content = content if content is not None else next_turtle(context, alias)
    tag = uuid.uuid4().hex[:8]
    r = vm.create_version(name, content, description or f"Version description {tag}", f"Changelog {tag}")
    assert r.status_code == 201, "setup: " + vm.describe(r)
    remember_upload(context, name, r.json()["version"], content)
    return r


def admin_get(context, path):
    r = vm.send("GET", path, "admin")
    assert r.status_code == 200, "check: " + vm.describe(r)
    return r.json()


def problem(context):
    r = resp(context)
    ct = r.headers.get("Content-Type", "")
    assert ct.startswith("application/problem+json"), f"not a problem response: {vm.describe(r)}"
    return r.json()


def issues(context):
    return problem(context).get("issues") or []


def set_field(fields, key, value):
    for f in fields:
        if f[0] == key:
            f[1] = value
            return
    fields.append([key, value])


def request_parts(req):
    parts = [(k, (None, v)) for k, v in req["fields"]]
    if req.get("file") is not None:
        parts.append(("vocabularyFile", (req.get("filename", "vocabulary.ttl"), req["file"], "text/turtle")))
    return parts


def store_success(context, req, r):
    if r.status_code != 201 or req["kind"] not in ("create", "version"):
        return
    b = r.json()
    if "name" in b and "version" in b:
        remember_upload(context, b["name"], b["version"], req.get("file"))
        vm.learn_content_base(b["name"], (b.get("contentHref") or "").rsplit("/versions/", 1)[0])


def submit(context, headers=None, query="", content_type=None):
    req = context.request
    path = req["path"] + query
    if content_type is None:
        try:
            r = vm.send("POST", path, context.identity, base=context.base, headers=headers,
                        files=request_parts(req))
        except requests.exceptions.ConnectionError as exc:
            size = len(req.get("file") or b"") + sum(len(str(v)) for _, v in req["fields"])
            if size > 5 * 1024 * 1024:
                # SR-08: the server may answer 413 and close before the body is sent; inconclusive
                context.scenario.skip(f"inconclusive: connection closed while sending {size} bytes ({exc})")
                return None
            raise
    else:
        h = dict(headers or {})
        h["Content-Type"] = content_type
        if "json" in content_type:
            data = json.dumps({k: v for k, v in req["fields"]}).encode()
        elif content_type.startswith("application/x-www-form-urlencoded"):
            from urllib.parse import urlencode
            data = urlencode(req["fields"]).encode()
        else:
            data = req.get("file") or b""
        r = vm.send("POST", path, context.identity, base=context.base, headers=h, data=data)
    context.resp = r
    store_success(context, req, r)
    return r


# ======================================================================= identity


@given("I am {who:Who}")
def step_identity(context, who):
    if who in IDENTITIES:
        context.identity = IDENTITIES[who]
    elif who == "not authenticated":
        context.identity = "none"
    else:
        raise AssertionError(f"unknown identity '{who}'")


@given("I use a malformed bearer token")
def step_garbage_token(context):
    context.identity = "garbage"


@given("I use an administrator token with a broken signature")
def step_tampered_token(context):
    context.identity = "tampered"


@given("I use an administrator token without the vocabulary-manager-be audience")
def step_noaud_token(context):
    context.identity = "noaud"


@given("I use an administrator token from another issuer")
def step_other_issuer(context):
    context.identity = "otherissuer"


# ======================================================================= setup (always as administrator)


@given('a vocabulary "{alias:Q}" exists')
def step_vocab_exists(context, alias):
    admin_create(context, alias)


@given('a vocabulary "{alias:Q}" exists with description "{desc:Q}"')
def step_vocab_exists_desc(context, alias, desc):
    admin_create(context, alias, description=X(context, desc))


@given('a vocabulary "{alias:Q}" exists with the Turtle')
def step_vocab_exists_turtle(context, alias):
    admin_create(context, alias, content=X(context, context.text).encode())


@given('vocabulary "{alias:Q}" has a new version')
def step_new_version(context, alias):
    admin_new_version(context, alias)


@given('vocabulary "{alias:Q}" has a new version with description "{desc:Q}"')
def step_new_version_desc(context, alias, desc):
    admin_new_version(context, alias, description=X(context, desc))


@given('vocabulary "{alias:Q}" has a new version with the Turtle')
def step_new_version_turtle(context, alias):
    admin_new_version(context, alias, content=X(context, context.text).encode())


@given('vocabulary "{alias:Q}" has {n:d} versions')
def step_n_versions(context, alias, n):
    name = name_of(context, alias)
    current = admin_get(context, f"/vocabularies/{name}")["version"]
    for _ in range(n - current):
        admin_new_version(context, alias)


@given('the status of vocabulary "{alias:Q}" is changed to "{status:Q}"')
def step_set_vocab_status(context, alias, status):
    r = vm.patch_status(f"/vocabularies/{name_of(context, alias)}", status)
    assert r.status_code == 200, "setup: " + vm.describe(r)


@given('the status of version {v:d} of vocabulary "{alias:Q}" is changed to "{status:Q}"')
def step_set_version_status(context, v, alias, status):
    r = vm.patch_status(f"/vocabularies/{name_of(context, alias)}/versions/{v}", status)
    assert r.status_code == 200, "setup: " + vm.describe(r)


@given('an external vocabulary "{alias:Q}" is registered with namespace "{ns:Q}"')
def step_ext_registered(context, alias, ns):
    step_ext_registered_aliases(context, alias, ns, "")


@given('an external vocabulary "{alias:Q}" is registered with namespace "{ns:Q}" and aliases "{aliases:Q}"')
def step_ext_registered_aliases(context, alias, ns, aliases):
    name = name_of(context, alias)
    ns = X(context, ns)
    content = (vm.PREFIXES + f"@prefix e: <{ns.rstrip('#/')}#> .\ne:Term a rdfs:Class ;\n  rdfs:label \"Term of {name}\"@en .\n").encode()
    al = [X(context, a.strip()) for a in aliases.split(",") if a.strip()]
    r = vm.register_external(name, ns, content, aliases=al, description=f"External {name}")
    assert r.status_code == 201, "setup: " + vm.describe(r)
    remember_upload(context, "ext:" + name, 0, content)


@given("the clock has moved on by more than one second")
def step_wait_second(context):
    time.sleep(1.2)


@given('I remember the field "{path:Q}" of "{url:Q}" as "{key:Q}"')
def step_remember_field_of(context, path, url, key):
    context.remembered[key] = vm.json_path(admin_get(context, X(context, url)), path)


# ======================================================================= request builder


@given('a create vocabulary request for "{alias:Q}"')
def step_create_request(context, alias):
    name = name_of(context, alias)
    context.request = {
        "kind": "create", "path": "/vocabularies", "alias": alias,
        "fields": [["name", name], ["description", f"Description of {name}"], ["changelog", "Initial version."]],
        "file": next_turtle(context, alias),
    }


@given('a new version request for vocabulary "{alias:Q}"')
def step_version_request(context, alias):
    name = name_of(context, alias)
    tag = uuid.uuid4().hex[:8]
    context.request = {
        "kind": "version", "path": f"/vocabularies/{name}/versions", "alias": alias,
        "fields": [["description", f"Version description {tag}"], ["changelog", f"Changelog {tag}"]],
        "file": next_turtle(context, alias),
    }


@given('an external vocabulary registration request for "{alias:Q}" with namespace "{ns:Q}"')
def step_ext_request(context, alias, ns):
    name = name_of(context, alias)
    ns = X(context, ns)
    content = (vm.PREFIXES + f"@prefix e: <{ns.rstrip('#/')}#> .\ne:Term a rdfs:Class ;\n  rdfs:label \"Term\"@en .\n").encode()
    context.request = {
        "kind": "external", "path": "/external-vocabularies", "alias": alias,
        "fields": [["name", name], ["namespaceUri", ns]],
        "file": content,
    }


@given('the request field "{field:Q}" is "{value:Q}"')
def step_field_value(context, field, value):
    set_field(context.request["fields"], field, X(context, value))


@given('the request field "{field:Q}" has {n:d} characters')
def step_field_len(context, field, n):
    set_field(context.request["fields"], field, "D" * n)


@given('the request field "{field:Q}" has {n:d} characters surrounded by spaces')
def step_field_len_spaces(context, field, n):
    set_field(context.request["fields"], field, "  " + "D" * n + "  ")


@given('the request field "name" is a unique name of {n:d} characters')
def step_name_len(context, n):
    base = name_of(context, "N")
    if n >= len(base):
        value = base + "x" * (n - len(base))
    else:
        value = base[:n]
    set_field(context.request["fields"], "name", value)
    context.request["alias_name"] = value


@given('the request field "{field:Q}" is removed')
def step_field_removed(context, field):
    context.request["fields"] = [f for f in context.request["fields"] if f[0] != field]
    if field == "vocabularyFile":
        context.request["file"] = None


@given('the request has an extra part "{field:Q}" with value "{value:Q}"')
def step_extra_part(context, field, value):
    context.request["fields"].append([field, X(context, value)])


@given('the request has {n:d} alias URIs')
def step_n_aliases(context, n):
    context.request["fields"] = [f for f in context.request["fields"] if f[0] != "aliasUris"]
    for i in range(n):
        context.request["fields"].append(["aliasUris", vm.ext_uri(context.sid, f"alias{i}")])


@given("the request file is")
def step_file_text(context):
    context.request["file"] = X(context, context.text).encode("utf-8")


@given('the request file is the fixture "{fixture:Q}"')
def step_file_fixture(context, fixture):
    import os
    path = os.path.join(os.path.dirname(__file__), "..", "..", "fixtures", fixture)
    with open(path, encoding="utf-8") as fh:
        context.request["file"] = X(context, fh.read()).encode("utf-8")


@given("the request file is empty")
def step_file_empty(context):
    context.request["file"] = b""


@given("the request file is not Turtle")
def step_file_binary(context):
    context.request["file"] = bytes(range(256)) * 4


@given('the request file is valid Turtle of exactly {n:d} bytes')
def step_file_size(context, n):
    alias = context.request.get("alias", "A")
    context.request["file"] = vm.turtle_of_size(vm.namespace(context.sid, alias), n)


@given('the request file is the generated Turtle with {n:d} duplicate label errors')
def step_file_s4_errors(context, n):
    ns = vm.namespace(context.sid, context.request.get("alias", "A"))
    lines = ["@prefix skos: <http://www.w3.org/2004/02/skos/core#> .", f"@prefix ex: <{ns}> ."]
    for i in range(n):
        lines.append(f'ex:c{i:05d} skos:prefLabel "One {i}"@en , "Two {i}"@en .')
    context.request["file"] = ("\n".join(lines) + "\n").encode()


@given('the request field "{field:Q}" has {n:d} bytes')
def step_field_bytes(context, field, n):
    set_field(context.request["fields"], field, "x" * n)


@given('the request file is the content of version {v:d} of vocabulary "{alias:Q}"')
def step_file_same(context, v, alias):
    context.request["file"] = context.contents[(name_of(context, alias), v)]


@given('the request description is the description of version {v:d} of vocabulary "{alias:Q}"')
def step_desc_same(context, v, alias):
    d = admin_get(context, f"/vocabularies/{name_of(context, alias)}/versions/{v}")["description"]
    set_field(context.request["fields"], "description", d)


@given('the request description is the description of version {v:d} of vocabulary "{alias:Q}" surrounded by spaces')
def step_desc_same_spaces(context, v, alias):
    d = admin_get(context, f"/vocabularies/{name_of(context, alias)}/versions/{v}")["description"]
    set_field(context.request["fields"], "description", "  " + d + "  ")


@when("I submit the request")
def step_submit(context):
    submit(context)


@when('I submit the request as "{content_type:Q}"')
def step_submit_ct(context, content_type):
    submit(context, content_type=content_type)


@when('I submit the request with the query "{query:Q}"')
def step_submit_query(context, query):
    submit(context, query=X(context, query))


@when('I submit the request with the header "{header:Q}" set to "{value:Q}"')
def step_submit_header(context, header, value):
    submit(context, headers={header: value})


@when('I submit {n:d} new version requests for vocabulary "{alias:Q}" in parallel')
def step_parallel_versions(context, n, alias):
    name = name_of(context, alias)
    payloads = []
    for _ in range(n):
        tag = uuid.uuid4().hex[:8]
        payloads.append((next_turtle(context, alias), f"Parallel {tag}", f"Changelog {tag}"))

    def run(p):
        return vm.create_version(name, p[0], p[1], p[2], identity=context.identity)

    with concurrent.futures.ThreadPoolExecutor(max_workers=n) as ex:
        context.parallel = list(ex.map(run, payloads))


@when('I submit {n:d} create requests for vocabulary "{alias:Q}" in parallel')
def step_parallel_creates(context, n, alias):
    name = name_of(context, alias)
    payloads = [next_turtle(context, alias) for _ in range(n)]

    def run(c):
        return vm.create_vocabulary(name, c, identity=context.identity)

    with concurrent.futures.ThreadPoolExecutor(max_workers=n) as ex:
        context.parallel = list(ex.map(run, payloads))


@when('I send {n:d} PATCH requests to "{path:Q}" in parallel with JSON')
def step_parallel_patch(context, n, path):
    path = X(context, path)
    payload = json.loads(X(context, context.text))

    def run(_):
        return vm.send("PATCH", path, context.identity, json=payload)

    with concurrent.futures.ThreadPoolExecutor(max_workers=n) as ex:
        context.parallel = list(ex.map(run, range(n)))


# ======================================================================= generic HTTP


@when('I send a {method:w} request to "{path:Q}"')
def step_send(context, method, path):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base)


@when('I send a {method:w} request to "{path:Q}" with the header "{header:Q}" set to "{value:Q}"')
def step_send_header(context, method, path, header, value):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base,
                           headers={header: X(context, value)})


@when('I send a {method:w} request to "{path:Q}" without an Accept header')
def step_send_no_accept(context, method, path):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base,
                           headers={"Accept": None})


@when('I send a {method:w} request to "{path:Q}" with JSON')
def step_send_json(context, method, path):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base,
                           headers={"Content-Type": "application/json"},
                           data=X(context, context.text).encode())


@when('I send a {method:w} request to "{path:Q}" with JSON and the header "{header:Q}" set to "{value:Q}"')
def step_send_json_header(context, method, path, header, value):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base,
                           headers={"Content-Type": "application/json", header: value},
                           data=X(context, context.text).encode())


@when('I send a {method:w} request to "{path:Q}" with content type "{ct:Q}", the header "{header:Q}" set to "{value:Q}" and body')
def step_send_ct_header_body(context, method, path, ct, header, value):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base,
                           headers={"Content-Type": ct, header: value}, data=X(context, context.text or "").encode())


@when('I send a {method:w} request to "{path:Q}" with content type "{ct:Q}" and body')
def step_send_ct_body(context, method, path, ct):
    context.resp = vm.send(method, X(context, path), context.identity, base=context.base,
                           headers={"Content-Type": ct}, data=X(context, context.text or "").encode())


@when('I send a POST request to "{path:Q}" with a JSON body of {n:d} bytes')
def step_send_big_json(context, path, n):
    prefix = '{"items":[{"name":"'
    suffix = '"}]}'
    filler = "A" * (n - len(prefix) - len(suffix))
    context.resp = vm.send("POST", X(context, path), context.identity, base=context.base,
                           headers={"Content-Type": "application/json"},
                           data=(prefix + filler + suffix).encode())


@when('I search vocabularies with {n:d} references to unknown names')
def step_search_n(context, n):
    items = [{"name": vm.vocab_name(context.sid, f"Unknown{i}")} for i in range(n)]
    context.resp = vm.send("POST", "/vocabularies/searches", context.identity, base=context.base,
                           json={"items": items})


@when('I search external vocabularies with {n:d} namespace URIs and {m:d} alias URIs')
def step_ext_search_n(context, n, m):
    payload = {}
    if n:
        payload["namespaceUris"] = [vm.ext_uri(context.sid, f"n{i}") for i in range(n)]
    if m:
        payload["aliasUris"] = [vm.ext_uri(context.sid, f"a{i}") for i in range(m)]
    context.resp = vm.send("POST", "/external-vocabulary/searches", context.identity, base=context.base,
                           json=payload)


OPERATION_BODIES = {
    "POST /vocabularies": "create",
    "POST /vocabularies/${A}/versions": "version",
    "POST /external-vocabularies": "external",
    "POST /vocabularies/searches": {"items": [{"name": "${A}"}]},
    "POST /external-vocabulary/searches": {"namespaceUri": "${URI:X}"},
    "POST /external-vocabularies/content": {"namespaceUri": "${URI:X}"},
    "PATCH /vocabularies/${A}": {"status": "DEPRECATED"},
    "PATCH /vocabularies/${A}/versions/1": {"status": "DEPRECATED"},
}


@when('I call the operation "{operation:Q}"')
def step_call_operation(context, operation):
    method, path = operation.split(" ", 1)
    spec = OPERATION_BODIES.get(operation)
    url = X(context, path)
    if spec == "create":
        name = name_of(context, "Op")
        parts = vm.multipart([("name", name), ("description", "d"), ("changelog", "c")],
                             next_turtle(context, "Op"))
        context.resp = vm.send(method, url, context.identity, base=context.base, files=parts)
    elif spec == "version":
        parts = vm.multipart([("description", "d" + uuid.uuid4().hex), ("changelog", "c")],
                             next_turtle(context, "A"))
        context.resp = vm.send(method, url, context.identity, base=context.base, files=parts)
    elif spec == "external":
        name = name_of(context, "Opx")
        parts = vm.multipart([("name", name), ("namespaceUri", vm.ext_uri(context.sid, "opx"))],
                             (vm.PREFIXES + "<urn:x:a> a rdfs:Class .\n").encode())
        context.resp = vm.send(method, url, context.identity, base=context.base, files=parts)
    elif isinstance(spec, dict):
        context.resp = vm.send(method, url, context.identity, base=context.base,
                               headers={"Content-Type": "application/json"},
                               data=X(context, json.dumps(spec)).encode())
    else:
        context.resp = vm.send(method, url, context.identity, base=context.base)


# ======================================================================= response checks


@then("the response status is {code:d}")
def step_status(context, code):
    r = resp(context)
    assert r.status_code == code, f"expected {code}: {vm.describe(r)}"


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def step_problem(context, code, ptype):
    step_status(context, code)
    p = problem(context)
    assert p.get("type") == ptype, f"type {p.get('type')!r} != {ptype!r}: {vm.describe(resp(context))}"
    if "status" in p:
        assert p["status"] == code, f"problem status {p['status']} != {code}"


@then("the response is a problem with status {code:d}")
def step_problem_any(context, code):
    step_status(context, code)
    p = problem(context)
    assert p.get("status", code) == code, f"problem status {p.get('status')} != {code}"


@then('the response is a problem with status {code:d} and a type starting "{prefix:Q}"')
def step_problem_prefix(context, code, prefix):
    step_status(context, code)
    p = problem(context)
    assert str(p.get("type", "")).startswith(prefix), f"type {p.get('type')!r}"


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    assert problem(context).get("title") == title, vm.describe(resp(context))


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    want = X(context, detail)
    got = problem(context).get("detail")
    assert got == want, f"detail {got!r} != {want!r}"


@then('the problem detail starts with "{detail:Q}"')
def step_problem_detail_starts(context, detail):
    got = problem(context).get("detail") or ""
    assert got.startswith(X(context, detail)), f"detail {got!r}"


@then("the problem has exactly one issue")
def step_one_issue(context):
    assert len(issues(context)) == 1, f"issues: {issues(context)}"


@then("the problem has exactly {n:d} issues")
def step_n_issues(context, n):
    assert len(issues(context)) == n, f"{len(issues(context))} issues: {issues(context)[:5]}"


@then("the problem has no issues")
def step_no_issues(context):
    assert not issues(context), f"issues: {issues(context)}"


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_in_named(context, where, name):
    want = X(context, name)
    found = [i for i in issues(context) if i.get("in") == where and i.get("name") == want]
    assert found, f"no issue in {where} named {want!r}: {issues(context)}"


@then('the problem has an issue in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def step_issue_in_named_type(context, where, name, itype):
    want = X(context, name)
    found = [i for i in issues(context)
             if i.get("in") == where and i.get("name") == want and i.get("type") == itype]
    assert found, f"no issue in {where} named {want!r} of type {itype}: {issues(context)}"


@then('every issue is in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def step_every_issue(context, where, name, itype):
    iss = issues(context)
    assert iss, "no issues"
    for i in iss:
        assert (i.get("in"), i.get("name"), i.get("type")) == (where, name, itype), f"issue {i}"


@then('the problem has an issue with detail "{detail:Q}" and location "{location:Q}"')
def step_issue_detail_loc(context, detail, location):
    d, l = X(context, detail), X(context, location)
    found = [i for i in issues(context) if i.get("detail") == d and i.get("location") == l]
    assert found, f"no issue ({d!r}, {l!r}) in {issues(context)}"


@then('the problem has an issue with detail "{detail:Q}"')
def step_issue_detail(context, detail):
    d = X(context, detail)
    found = [i for i in issues(context) if i.get("detail") == d]
    assert found, f"no issue with detail {d!r} in {issues(context)}"


@then('the problem has an issue with detail starting "{detail:Q}" and location matching "{regex:Q}"')
def step_issue_detail_start_loc(context, detail, regex):
    d = X(context, detail)
    found = [i for i in issues(context)
             if str(i.get("detail", "")).startswith(d) and re.fullmatch(regex, str(i.get("location", "")))]
    assert found, f"no issue starting {d!r} at /{regex}/ in {issues(context)}"


@then("the problem reveals no implementation details")
def step_no_internals(context):
    text = resp(context).text
    for marker in ("Exception", "\tat ", "at org.", "at java.", "springframework", "jakarta.",
                   "com.fasterxml", "org.apache", "NullPointer", "Caused by", "SQL", "SPARQL"):
        assert marker not in text, f"problem leaks {marker!r}: {text[:500]}"


@then('the response header "{header:Q}" is "{value:Q}"')
def step_header_is(context, header, value):
    got = resp(context).headers.get(header)
    assert got == X(context, value), f"{header}: {got!r} != {X(context, value)!r}"


@then('the response header "{header:Q}" is present')
def step_header_present(context, header):
    assert resp(context).headers.get(header), f"no {header}: {dict(resp(context).headers)}"


@then('the response header "Allow" lists "{methods:Q}"')
def step_allow(context, methods):
    allow = resp(context).headers.get("Allow", "")
    got = {m.strip().upper() for m in allow.split(",") if m.strip()}
    for m in methods.split(","):
        assert m.strip().upper() in got, f"Allow {allow!r} misses {m}"


@then('the response header "Content-Disposition" names the attachment "{filename:Q}"')
def step_content_disposition(context, filename):
    cd = resp(context).headers.get("Content-Disposition", "")
    want = X(context, filename)
    assert cd.lower().startswith("attachment"), f"Content-Disposition {cd!r}"
    m = re.search(r'filename="?([^";]+)"?', cd)
    assert m and m.group(1) == want, f"Content-Disposition {cd!r}, want filename {want!r}"


@then('the response content type is exactly "{ct:Q}"')
def step_ct_exact(context, ct):
    got = resp(context).headers.get("Content-Type")
    assert got == ct, f"Content-Type {got!r} != {ct!r}"


@then("the response content type is JSON")
def step_ct_json(context):
    got = resp(context).headers.get("Content-Type", "")
    assert got.split(";")[0].strip() == "application/json", f"Content-Type {got!r}"


@then('the response field "{path:Q}" is "{value:Q}"')
def step_field_is(context, path, value):
    got = vm.json_path(body(context), X(context, path))
    want = X(context, value)
    assert got == want, f"{path}: {got!r} != {want!r}"


@then('the response field "{path:Q}" is {n:d}')
def step_field_int(context, path, n):
    got = vm.json_path(body(context), path)
    assert got == n and not isinstance(got, bool), f"{path}: {got!r} != {n}"


@then('the response field "{path:Q}" is true')
def step_field_true(context, path):
    assert vm.json_path(body(context), path) is True, f"{path}: {vm.json_path(body(context), path)!r}"


@then('the response field "{path:Q}" is false')
def step_field_false(context, path):
    assert vm.json_path(body(context), path) is False, f"{path}: {vm.json_path(body(context), path)!r}"


@then('the response field "{path:Q}" is an empty list')
def step_field_empty(context, path):
    got = vm.json_path(body(context), path)
    assert got == [], f"{path}: {got!r}"


@then('the response field "{path:Q}" is absent or false')
def step_field_absent_false(context, path):
    b = body(context)
    assert not vm.has_path(b, path) or vm.json_path(b, path) in (None, False), f"{path}: {b.get(path)!r}"


@then('the response field "{path:Q}" is present')
def step_field_present(context, path):
    b = body(context)
    assert vm.has_path(b, path) and vm.json_path(b, path) not in (None, ""), f"{path} missing in {b}"


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    b = body(context)
    assert not vm.has_path(b, path) or vm.json_path(b, path) is None, f"{path} present in {b}"


@then('the response field "{path:Q}" is a UTC timestamp to the second')
def step_field_ts(context, path):
    got = vm.json_path(body(context), path)
    assert isinstance(got, str) and re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", got), f"{path}: {got!r}"


@then('the response field "{path:Q}" equals the response header "{header:Q}"')
def step_field_eq_header(context, path, header):
    got = vm.json_path(body(context), path)
    h = resp(context).headers.get(header)
    assert got == h, f"{path} {got!r} != header {header} {h!r}"


@then('the response field "{path:Q}" has {n:d} entries')
def step_field_count(context, path, n):
    got = vm.json_path(body(context), path)
    assert isinstance(got, list) and len(got) == n, f"{path}: {len(got) if isinstance(got, list) else got!r} != {n}"


@then('the response field "{path:Q}" lists "{csv:Q}"')
def step_field_lists(context, path, csv):
    got = vm.json_path(body(context), path)
    want = [X(context, v.strip()) for v in csv.split(",")] if csv.strip() else []
    assert got == want, f"{path}: {got!r} != {want!r}"


@then('the response field "{path:Q}" contains "{text:Q}"')
def step_field_contains(context, path, text):
    got = str(vm.json_path(body(context), path))
    assert X(context, text) in got, f"{path}: {got!r} lacks {X(context, text)!r}"


@then('I remember the response field "{path:Q}" as "{key:Q}"')
def step_remember(context, path, key):
    context.remembered[key] = vm.json_path(body(context), path)


@then('the response field "{path:Q}" equals the remembered "{key:Q}"')
def step_field_eq_remembered(context, path, key):
    got = vm.json_path(body(context), path)
    assert got == context.remembered[key], f"{path}: {got!r} != {context.remembered[key]!r}"


@then('the response field "{path:Q}" is later than the remembered "{key:Q}"')
def step_field_later(context, path, key):
    got = vm.parse_ts(vm.json_path(body(context), path))
    assert got > vm.parse_ts(context.remembered[key]), f"{path}: {got} not later than {context.remembered[key]}"


@then('the item names are "{csv:Q}"')
def step_item_names(context, csv):
    got = [i.get("name") for i in body(context).get("items", [])]
    want = [X(context, v.strip()) for v in csv.split(",")] if csv.strip() else []
    assert got == want, f"names {got} != {want}"


@then('the item versions are "{csv:Q}"')
def step_item_versions(context, csv):
    got = [i.get("version") for i in body(context).get("items", [])]
    want = [int(v) for v in csv.split(",")] if csv.strip() else []
    assert got == want, f"versions {got} != {want}"


@then('the item statuses are "{csv:Q}"')
def step_item_statuses(context, csv):
    got = [i.get("status") for i in body(context).get("items", [])]
    want = [v.strip() for v in csv.split(",")] if csv.strip() else []
    assert got == want, f"statuses {got} != {want}"


@then("the response has no items")
def step_no_items(context):
    assert body(context).get("items") == [], f"items: {body(context).get('items')}"


@then('the response body equals the content of version {v:d} of vocabulary "{alias:Q}"')
def step_body_eq_content(context, v, alias):
    want = context.contents[(name_of(context, alias), v)]
    assert resp(context).content == want, "downloaded bytes differ from the uploaded bytes"


@then('the response body equals the content of external vocabulary "{alias:Q}"')
def step_body_eq_ext(context, alias):
    want = context.contents[("ext:" + name_of(context, alias), 0)]
    assert resp(context).content == want, "downloaded bytes differ from the registered bytes"


@then("the response body equals the uploaded file")
def step_body_eq_upload(context):
    assert resp(context).content == context.request["file"], "downloaded bytes differ from the uploaded bytes"


# ----------------------------------------------------------------------- validation warnings


def warnings(context):
    b = body(context)
    assert "validationWarnings" in b, f"validationWarnings missing: {b}"
    return b["validationWarnings"]


@then("the response has no validation warnings")
def step_no_warnings(context):
    assert warnings(context) == [], f"warnings: {warnings(context)}"


@then("the response has exactly {n:d} validation warnings")
def step_n_warnings(context, n):
    assert len(warnings(context)) == n, f"warnings: {warnings(context)}"


@then('the response has a validation warning "{code:Q}" with location "{location:Q}"')
def step_warning_loc(context, code, location):
    loc = X(context, location)
    found = [w for w in warnings(context) if w.get("code") == code and w.get("location") == loc]
    assert found, f"no warning {code} at {loc!r}: {warnings(context)}"


@then('the response has a validation warning "{code:Q}" with description "{desc:Q}" and location "{location:Q}"')
def step_warning_full(context, code, desc, location):
    d, loc = X(context, desc), X(context, location)
    found = [w for w in warnings(context)
             if w.get("code") == code and w.get("description") == d and w.get("location") == loc]
    assert found, f"no warning {code} {d!r} at {loc!r}: {warnings(context)}"


@then('the response has no validation warning "{code:Q}"')
def step_no_warning_code(context, code):
    found = [w for w in warnings(context) if w.get("code") == code]
    assert not found, f"unexpected warnings: {found}"


@then("every validation warning code is in the closed set")
def step_warning_codes_closed(context):
    allowed = {"externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"}
    for w in warnings(context):
        assert w.get("code") in allowed, f"warning code {w.get('code')!r}"
        assert set(w) >= {"code", "description", "location"}, f"warning fields {w}"


@then("the response has no validation warnings field or an empty one")
def step_no_warnings_field(context):
    b = body(context)
    assert not b.get("validationWarnings"), f"warnings: {b.get('validationWarnings')}"


# ----------------------------------------------------------------------- stored state (read as administrator)


def list_by_name(context, name):
    data = admin_get(context, f"/vocabularies?name={name}&pageSize=100")
    return [i for i in data["items"] if i["name"].lower() == name.lower()]


@then('no vocabulary "{alias:Q}" is stored')
def step_not_stored(context, alias):
    name = name_of(context, alias)
    assert list_by_name(context, name) == [], f"{name} is stored"


@then("no vocabulary with the requested name is stored")
def step_not_stored_requested(context):
    name = dict((k, v) for k, v in context.request["fields"]).get("name")
    assert name, "request has no name"
    if not re.fullmatch(r"[A-Za-z0-9]{1,64}", name):
        # the name breaks the filter pattern; check through the scenario id instead
        data = admin_get(context, f"/vocabularies?name={context.sid}&pageSize=100")
        assert all(i["name"] != name for i in data["items"]), f"{name} is stored"
        return
    assert list_by_name(context, name) == [], f"{name} is stored"


@then("no vocabulary of this scenario is stored")
def step_none_of_scenario(context):
    data = admin_get(context, f"/vocabularies?name={context.sid}&pageSize=100")
    assert data["total"] == 0 and data["items"] == [], f"stored: {[i['name'] for i in data['items']]}"


@then('exactly one vocabulary "{alias:Q}" is stored')
def step_one_stored(context, alias):
    name = name_of(context, alias)
    assert len(list_by_name(context, name)) == 1, f"{name} not stored exactly once"


@then('vocabulary "{alias:Q}" has {n:d} versions')
def step_has_versions(context, alias, n):
    data = admin_get(context, f"/vocabularies/{name_of(context, alias)}/versions?pageSize=100")
    assert data["total"] == n, f"{data['total']} versions, expected {n}"


@then('vocabulary "{alias:Q}" has 1 version')
def step_has_one_version(context, alias):
    step_has_versions(context, alias, 1)


@then('version {v:d} of vocabulary "{alias:Q}" has status "{status:Q}"')
def step_version_status(context, v, alias, status):
    data = admin_get(context, f"/vocabularies/{name_of(context, alias)}/versions/{v}")
    assert data["status"] == status, f"version {v} is {data['status']}"


@then('versions {csv} of vocabulary "{alias:Q}" have status "{status:Q}"')
def step_versions_status(context, csv, alias, status):
    for v in csv.replace(" and ", ",").split(","):
        step_version_status(context, int(v.strip()), alias, status)


@then('vocabulary "{alias:Q}" has status "{status:Q}"')
def step_vocab_status(context, alias, status):
    data = admin_get(context, f"/vocabularies/{name_of(context, alias)}")
    assert data["status"] == status, f"vocabulary is {data['status']}"


@then('vocabulary "{alias:Q}" has latest version {v:d}')
def step_vocab_latest(context, alias, v):
    data = admin_get(context, f"/vocabularies/{name_of(context, alias)}")
    assert data["version"] == v, f"latest version {data['version']}"


@then('no external vocabulary "{alias:Q}" is stored')
def step_ext_not_stored(context, alias):
    r = vm.send("GET", f"/external-vocabularies/{name_of(context, alias)}", "admin")
    assert r.status_code == 404, f"expected 404: {vm.describe(r)}"


@then('the stored external vocabulary "{alias:Q}" has namespace "{ns:Q}"')
def step_ext_ns(context, alias, ns):
    data = admin_get(context, f"/external-vocabularies/{name_of(context, alias)}")
    assert data["namespaceUri"] == X(context, ns), f"namespace {data['namespaceUri']!r}"


# ----------------------------------------------------------------------- parallel results


@then("every parallel response has status {code:d}")
def step_parallel_all(context, code):
    for r in context.parallel:
        assert r.status_code == code, vm.describe(r)


@then("the parallel responses have the statuses {csv}")
def step_parallel_statuses(context, csv):
    got = sorted(r.status_code for r in context.parallel)
    want = sorted(int(c) for c in csv.replace(" and ", ",").split(","))
    assert got == want, f"statuses {got} != {want}: {[vm.describe(r) for r in context.parallel]}"


@then("the parallel responses carry the versions {csv} each exactly once")
def step_parallel_versions_check(context, csv):
    got = sorted(r.json()["version"] for r in context.parallel)
    want = sorted(int(c) for c in csv.replace(" and ", ",").split(","))
    assert got == want, f"versions {got} != {want}"


@then("the parallel responses carry distinct Location headers")
def step_parallel_locations(context):
    locs = [r.headers.get("Location") for r in context.parallel]
    assert all(locs) and len(set(locs)) == len(locs), f"locations {locs}"


@then("the response took less than {seconds:d} seconds")
def step_elapsed(context, seconds):
    assert resp(context).elapsed.total_seconds() < seconds, f"took {resp(context).elapsed}"


@then('the external vocabulary list holds "{first:Q}" before "{second:Q}"')
def step_ext_list_order(context, first, second):
    # SR-07: read pages of 100 only until both names are found; check their order there
    a, b = X(context, first), X(context, second)
    names = []
    page = 1
    while not (a in names and b in names):
        data = admin_get(context, f"/external-vocabularies?page={page}&pageSize=100")
        if not data["items"]:
            break
        names.extend(i["name"] for i in data["items"])
        page += 1
    assert a in names and b in names, f"{a} or {b} missing from the external list"
    assert names.index(a) < names.index(b), f"{a} is not before {b}"


@given('the remembered "{a:Q}" and "{b:Q}" fall on the same UTC day')
def step_same_day(context, a, b):
    if str(context.remembered[a])[:10] != str(context.remembered[b])[:10]:
        context.scenario.skip("the two writes fell on different UTC days (run crossed midnight)")


@given('the request has {n:d} extra parts')
def step_n_extra_parts(context, n):
    for i in range(n):
        context.request["fields"].append([f"extra{i}", "x"])


@then('the problem has an issue with location "{location:Q}"')
def step_issue_location(context, location):
    loc = X(context, location)
    assert [i for i in issues(context) if i.get("location") == loc], f"no issue at {loc!r}"


@then('the problem has no issue with location "{location:Q}"')
def step_no_issue_location(context, location):
    loc = X(context, location)
    assert not [i for i in issues(context) if i.get("location") == loc], f"unexpected issue at {loc!r}"
