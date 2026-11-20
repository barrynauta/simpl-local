"""Shared step library of the Vocabulary Manager black-box suite.

API steps talk HTTP only. UI steps (third person, 'the user ...' and 'the UI ...')
are in ui_steps.py (step 60, Playwright).
"""
import json
import math
import re
import time
from concurrent.futures import ThreadPoolExecutor

import parse
from behave import given, when, then, step, use_step_matcher, register_type

import vmtest as vm


@parse.with_pattern(r'[^"]*')
def _parse_quoted(text):
    return text


register_type(Q=_parse_quoted)

PROBLEM_JSON = "application/problem+json"
TIME_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")  # D58
WARNING_CODES = {"externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"}  # D10


def X(context, text):
    return vm.expand(context, text)


def body(context):
    return vm.json_body(context.response)


def media_type(resp):
    return (resp.headers.get("Content-Type") or "").split(";")[0].strip().lower()


def fail(context, msg):
    r = context.response
    extra = f"\nLast response: {r.status_code} {r.headers.get('Content-Type')} {r.text[:800]}" if r is not None else ""
    raise AssertionError(msg + extra)


def table_value(context, raw):
    """Values in multipart tables: '@valid' generated file, '@empty' 0 bytes,
    '@file:<fixture>' a fixture, '@bytes:<n>' valid Turtle of n bytes, else text."""
    if raw.startswith("@valid"):
        return vm.valid_turtle(context), True
    if raw == "@empty":
        return b"", True
    if raw.startswith("@file:"):
        return vm.fixture(context, raw[6:]), True
    if raw.startswith("@bytes:"):
        return vm.turtle_of_size(context, int(raw[7:])), True
    if raw.startswith("@text-file:"):
        return X(context, raw[11:]).encode(), True
    if raw.startswith("@content-of:"):  # @content-of:<alias>:<version>, the uploaded bytes
        alias, n = raw[12:].split(":")
        return context.uploads[(vm.vocab_name(context, alias), int(n))], True
    if raw.startswith("@description-of:"):  # @description-of:<alias>:<version>
        alias, n = raw[16:].split(":")
        return context.descriptions[(vm.vocab_name(context, alias), int(n))], False
    return X(context, raw), False


# ================================================================ identity

@given('I am "{user:Q}"')
def step_i_am(context, user):
    context.user = user
    context.token_override = None


@given('I use a token of kind "{kind:Q}"')
def step_token_kind(context, kind):
    context.token_override = vm.special_token(kind)


@given('the request header "{header:Q}" is "{value:Q}"')
def step_request_header(context, header, value):
    context.extra_headers = dict(getattr(context, "extra_headers", {}) or {})
    context.extra_headers[header] = X(context, value)


@given('the request has no Accept header')
def step_no_accept(context):
    context.extra_headers = dict(getattr(context, "extra_headers", {}) or {})
    context.extra_headers["Accept"] = None


# ================================================================ data set-up (always as ga-admin)

def _must(resp, code, what):
    if resp.status_code != code:
        raise AssertionError(f"Set-up failed ({what}): {resp.status_code} {resp.text[:500]}")
    return resp


@given('a vocabulary "{alias:Q}" exists')
def step_vocab_exists(context, alias):
    _must(vm.create_vocabulary(context, alias), 201, f"create {alias}")


@given('a vocabulary "{alias:Q}" exists with the description "{description:Q}"')
def step_vocab_exists_desc(context, alias, description):
    _must(vm.create_vocabulary(context, alias, description=X(context, description)), 201, f"create {alias}")


@given('a vocabulary "{alias:Q}" exists with {n:d} versions')
def step_vocab_exists_versions(context, alias, n):
    _must(vm.create_vocabulary(context, alias), 201, f"create {alias}")
    for _ in range(n - 1):
        _must(vm.create_version(context, alias), 201, f"new version of {alias}")


@given('a vocabulary "{alias:Q}" exists with the file "{fixture:Q}"')
def step_vocab_exists_fixture(context, alias, fixture):
    _must(vm.create_vocabulary(context, alias, content=vm.fixture(context, fixture)), 201, f"create {alias}")


@given('a new version of "{alias:Q}" was created')
def step_given_new_version(context, alias):
    _must(vm.create_version(context, alias), 201, f"new version of {alias}")


@given('the vocabulary "{alias:Q}" was set to "{status:Q}"')
def step_vocab_set(context, alias, status):
    name = vm.vocab_name(context, alias)
    _must(vm.request(context, "PATCH", f"/vocabularies/{name}", token=vm.token_for("ga-admin"),
                     json={"status": status}), 200, f"set {alias} to {status}")


@given('version {n:d} of "{alias:Q}" was set to "{status:Q}"')
def step_version_set(context, n, alias, status):
    name = vm.vocab_name(context, alias)
    _must(vm.request(context, "PATCH", f"/vocabularies/{name}/versions/{n}", token=vm.token_for("ga-admin"),
                     json={"status": status}), 200, f"set version {n} of {alias} to {status}")


@given('an external vocabulary "{alias:Q}" is registered')
def step_ext_registered(context, alias):
    _must(vm.register_external(context, alias), 201, f"register {alias}")


@given('an external vocabulary "{alias:Q}" is registered with namespace "{ns:Q}"')
def step_ext_registered_ns(context, alias, ns):
    _must(vm.register_external(context, alias, namespace=X(context, ns)), 201, f"register {alias}")


@given('an external vocabulary "{alias:Q}" is registered with namespace "{ns:Q}" and alias "{alias_uri:Q}"')
def step_ext_registered_ns_alias(context, alias, ns, alias_uri):
    _must(vm.register_external(context, alias, namespace=X(context, ns), aliases=[X(context, alias_uri)]),
          201, f"register {alias}")


@given('the external vocabulary "{alias:Q}" was removed')
def step_ext_removed(context, alias):
    name = vm.ext_name(context, alias)
    _must(vm.request(context, "DELETE", f"/external-vocabularies/{name}", token=vm.token_for("ga-admin")),
          204, f"remove {alias}")


@given('the vocabularies "{a:Q}" and "{b:Q}" were created on the same UTC day')
def step_same_day(context, a, b):
    days = set()
    for alias in (a, b):
        r = vm.admin_get(context, f"/vocabularies/{vm.vocab_name(context, alias)}")
        assert r.status_code == 200, r.text
        days.add(r.json()["creationDate"][:10])
    if len(days) != 1:
        context.scenario.skip("created across UTC midnight; the day filter check does not apply")


@given('a vocabulary "{alias:Q}" exists with {n:d} versions created one second apart')
def step_versions_apart(context, alias, n):
    _must(vm.create_vocabulary(context, alias), 201, f"create {alias}")
    for _ in range(n - 1):
        time.sleep(1.2)
        _must(vm.create_version(context, alias), 201, f"new version of {alias}")


@given('{n:d} vocabularies of this scenario exist')
def step_n_vocabularies(context, n):
    for i in range(n):
        _must(vm.create_vocabulary(context, f"Page{i:02d}"), 201, f"create Page{i:02d}")


@given('the clock moves to the next second')
def step_next_second(context):
    time.sleep(1.2)


@given('I remember the response as "{key:Q}"')
@then('I remember the response as "{key:Q}"')
def step_remember(context, key):
    context.remembered[key] = body(context)


@given('I remember version {n:d} of "{alias:Q}" as "{key:Q}"')
@then('I remember version {n:d} of "{alias:Q}" as "{key:Q}"')
def step_remember_version(context, n, alias, key):
    name = vm.vocab_name(context, alias)
    resp = vm.admin_get(context, f"/vocabularies/{name}/versions/{n}")
    assert resp.status_code == 200, resp.text
    context.remembered[key] = resp.json()


@given('I remember the vocabulary "{alias:Q}" as "{key:Q}"')
@then('I remember the vocabulary "{alias:Q}" as "{key:Q}"')
def step_remember_vocab(context, alias, key):
    name = vm.vocab_name(context, alias)
    resp = vm.admin_get(context, f"/vocabularies/{name}")
    assert resp.status_code == 200, resp.text
    context.remembered[key] = resp.json()


# ================================================================ actions (as the current user)

@when('I create the vocabulary "{alias:Q}"')
def step_create(context, alias):
    vm.create_vocabulary(context, alias, user=None)


@when('I create the vocabulary "{alias:Q}" with the description "{description:Q}"')
def step_create_desc(context, alias, description):
    vm.create_vocabulary(context, alias, user=None, description=X(context, description))


@when('I create the vocabulary "{alias:Q}" with the file "{fixture:Q}"')
def step_create_fixture(context, alias, fixture):
    vm.create_vocabulary(context, alias, user=None, content=vm.fixture(context, fixture))


@when('I create the vocabulary "{alias:Q}" with an empty file')
def step_create_empty(context, alias):
    vm.create_vocabulary(context, alias, user=None, content=b"")


@when('I create the vocabulary "{alias:Q}" with a file of {size:d} bytes')
def step_create_size(context, alias, size):
    vm.create_vocabulary(context, alias, user=None, content=vm.turtle_of_size(context, size))


@when('I create the vocabulary "{alias:Q}" with a file that declares the prefix "{prefix:Q}" for "{iri:Q}"')
def step_create_prefix(context, alias, prefix, iri):
    content = vm.valid_turtle(context, prefixes=[(prefix, X(context, iri))])
    vm.create_vocabulary(context, alias, user=None, content=content)


@when('I create the vocabulary "{alias:Q}" with a file that declares these prefixes')
def step_create_prefixes(context, alias):
    prefixes = [(row["prefix"], X(context, row["namespace"])) for row in context.table]
    vm.create_vocabulary(context, alias, user=None, content=vm.valid_turtle(context, prefixes=prefixes))


@when('I create a vocabulary with a free name of {n:d} characters')
def step_create_free_name(context, n):
    import random
    import string
    for _ in range(40):
        name = random.choice(string.ascii_uppercase) + "".join(
            random.choice(string.ascii_letters + string.digits) for _ in range(n - 1))
        resp = vm.create_vocabulary(context, "Free", user=None, name=name)
        if resp.status_code != 409:
            context.names["Free"] = name
            return
    fail(context, f"no free name of {n} characters found after 40 tries")


@when('I create a new version of "{alias:Q}"')
def step_new_version(context, alias):
    vm.create_version(context, alias, user=None)


@when('I create a new version of "{alias:Q}" with the description "{description:Q}"')
def step_new_version_desc(context, alias, description):
    vm.create_version(context, alias, user=None, description=X(context, description))


@when('I create a new version of "{alias:Q}" with the file "{fixture:Q}"')
def step_new_version_fixture(context, alias, fixture):
    vm.create_version(context, alias, user=None, content=vm.fixture(context, fixture))


@when('I create a new version of "{alias:Q}" with an empty file')
def step_new_version_empty(context, alias):
    vm.create_version(context, alias, user=None, content=b"")


@when('I create a new version of "{alias:Q}" with a file of {size:d} bytes')
def step_new_version_size(context, alias, size):
    vm.create_version(context, alias, user=None, content=vm.turtle_of_size(context, size))


@when('I create a new version of "{alias:Q}" with a file that declares the prefix "{prefix:Q}" for "{iri:Q}"')
def step_new_version_prefix(context, alias, prefix, iri):
    content = vm.valid_turtle(context, prefixes=[(prefix, X(context, iri))])
    vm.create_version(context, alias, user=None, content=content)


@when('I create a new version of "{alias:Q}" with the content of version {n:d} and the description "{description:Q}"')
def step_new_version_same_content(context, alias, n, description):
    name = vm.vocab_name(context, alias)
    content = context.uploads[(name, n)]
    vm.create_version(context, alias, user=None, content=content, description=X(context, description))


@when('I create a new version of "{alias:Q}" with the content and description of version {n:d} and the changelog "{changelog:Q}"')
def step_new_version_identical(context, alias, n, changelog):
    name = vm.vocab_name(context, alias)
    vm.create_version(context, alias, user=None, content=context.uploads[(name, n)],
                      description=context.descriptions[(name, n)], changelog=X(context, changelog))


@when('I create a new version of "{alias:Q}" with the content of version {n:d} and its description padded with spaces')
def step_new_version_identical_padded(context, alias, n):
    name = vm.vocab_name(context, alias)
    vm.create_version(context, alias, user=None, content=context.uploads[(name, n)],
                      description="  " + context.descriptions[(name, n)] + "  ")


@when('I send a multipart POST to "{path:Q}" with')
def step_multipart(context, path):
    parts = []
    for row in context.table:
        if row["value"] == "@omit":
            continue
        value, is_file = table_value(context, row["value"])
        name = X(context, row["part"])
        parts.append((name, value, is_file))
    vm.request(context, "POST", X(context, path), files=vm.multipart(parts))


@when('I send a "{method:Q}" request to "{path:Q}"')
def step_send(context, method, path):
    vm.request(context, method, X(context, path))


@when('I send a "{method:Q}" request to "{path:Q}" with JSON')
def step_send_json(context, method, path):
    data = X(context, context.text)
    vm.request(context, method, X(context, path), data=data.encode(), headers={"Content-Type": "application/json"})


@when('I send a "{method:Q}" request to "{path:Q}" with content type "{ctype:Q}" and body')
def step_send_ctype(context, method, path, ctype):
    data = X(context, context.text or "")
    vm.request(context, method, X(context, path), data=data.encode(), headers={"Content-Type": ctype})


@when('I send a "{method:Q}" request to "{path:Q}" with content type "{ctype:Q}" and {size:d} bytes')
def step_send_ctype_size(context, method, path, ctype, size):
    vm.request(context, method, X(context, path), data=b"x" * size, headers={"Content-Type": ctype})


@when('I send a GET request for the raw path "{raw:Q}"')
def step_raw_path(context, raw):
    # requests keeps percent-encoded characters as given
    vm.request(context, "GET", X(context, raw))


@when('I search vocabularies with {n:d} references')
def step_search_n(context, n):
    items = [{"name": f"Missing{context.run}{i}"} for i in range(n)]
    vm.request(context, "POST", "/vocabularies/searches", json={"items": items})


@when('I search external vocabularies with {a:d} namespace URIs and {b:d} alias URIs')
def step_ext_search_n(context, a, b):
    payload = {}
    if a:
        payload["namespaceUris"] = [f"https://none.example.org/{context.run}/ns{i}" for i in range(a)]
    if b:
        payload["aliasUris"] = [f"https://none.example.org/{context.run}/alias{i}" for i in range(b)]
    vm.request(context, "POST", "/external-vocabulary/searches", json=payload)


@when('I register the external vocabulary "{alias:Q}" with {n:d} alias URIs')
def step_register_n_aliases(context, alias, n):
    aliases = [f"https://ext.example.org/{context.run}/{alias}/alias{i}#" for i in range(n)]
    vm.register_external(context, alias, aliases=aliases, user=None)


@when('I register the external vocabulary "{alias:Q}" with namespace "{ns:Q}"')
def step_register_ns(context, alias, ns):
    vm.register_external(context, alias, namespace=X(context, ns), user=None)


@when('I register the external vocabulary "{alias:Q}" with namespace "{ns:Q}" and alias "{alias_uri:Q}"')
def step_register_ns_alias(context, alias, ns, alias_uri):
    vm.register_external(context, alias, namespace=X(context, ns), aliases=[X(context, alias_uri)], user=None)


@when('I register the external vocabulary "{alias:Q}" with the file "{fixture:Q}"')
def step_register_fixture(context, alias, fixture):
    vm.register_external(context, alias, content=vm.fixture(context, fixture), user=None)


# ---------------------------------------------------------------- concurrency (D43, D82, D92)

@when('I send {n:d} new versions of "{alias:Q}" in parallel')
def step_parallel_versions(context, n, alias):
    """D43: all succeed. A 503 'Too many uploads in progress' (D92) is retried by the
    client after Retry-After, as D44 tells clients to do."""
    name = vm.vocab_name(context, alias)
    token = vm.token_for("ga-admin")

    def one(i):
        content = vm.valid_turtle(context)
        parts = [("description", f"Parallel {i}", False), ("changelog", f"Parallel change {i}", False),
                 ("vocabularyFile", content, True)]
        for _ in range(10):
            import requests
            r = requests.post(f"{context.api_base}/vocabularies/{name}/versions",
                              headers=vm.auth_headers(token), files=vm.multipart(parts), timeout=vm.REQUEST_TIMEOUT)
            if r.status_code == 503 and "Too many uploads" in r.text:
                time.sleep(float(r.headers.get("Retry-After", "5")))
                continue
            return r
        return r

    with ThreadPoolExecutor(max_workers=n) as pool:
        context.parallel = list(pool.map(one, range(n)))


@when('I create the vocabulary "{alias:Q}" twice in parallel')
def step_parallel_create(context, alias):
    import requests
    name = vm.vocab_name(context, alias)
    token = vm.token_for("ga-admin")

    def one(i):
        parts = [("name", name, False), ("description", f"Parallel {i}", False),
                 ("changelog", "Initial version.", False), ("vocabularyFile", vm.valid_turtle(context), True)]
        return requests.post(f"{context.api_base}/vocabularies", headers=vm.auth_headers(token),
                             files=vm.multipart(parts), timeout=vm.REQUEST_TIMEOUT)

    with ThreadPoolExecutor(max_workers=2) as pool:
        context.parallel = list(pool.map(one, range(2)))


@when('I set the status of "{alias:Q}" to "{status:Q}" twice in parallel')
def step_parallel_patch(context, alias, status):
    import requests
    name = vm.vocab_name(context, alias)
    token = vm.token_for("ga-admin")

    def one(_):
        return requests.patch(f"{context.api_base}/vocabularies/{name}", headers=vm.auth_headers(token),
                              json={"status": status}, timeout=vm.REQUEST_TIMEOUT)

    with ThreadPoolExecutor(max_workers=2) as pool:
        context.parallel = list(pool.map(one, range(2)))


@when('I send {n:d} uploads of {size:d} bytes in parallel')
def step_parallel_uploads(context, n, size):
    import requests
    token = vm.token_for("ga-admin")
    content = vm.turtle_of_size(context, size)

    def one(i):
        name = f"Upload{context.run}{i}"
        parts = [("name", name, False), ("description", "Parallel upload", False),
                 ("changelog", "Initial version.", False), ("vocabularyFile", content, True)]
        return requests.post(f"{context.api_base}/vocabularies", headers=vm.auth_headers(token),
                             files=vm.multipart(parts), timeout=vm.REQUEST_TIMEOUT)

    with ThreadPoolExecutor(max_workers=n) as pool:
        context.parallel = list(pool.map(one, range(n)))


@then('the parallel responses have the statuses "{statuses:Q}"')
def step_parallel_statuses(context, statuses):
    expected = sorted(int(s) for s in statuses.split(","))
    got = sorted(r.status_code for r in context.parallel)
    assert got == expected, f"expected statuses {expected}, got {got}: " + " | ".join(r.text[:200] for r in context.parallel)


@then('every parallel response is 201 or a 503 with the detail "{detail:Q}" and Retry-After "{retry:Q}"')
def step_parallel_201_or_503(context, detail, retry):
    for r in context.parallel:
        if r.status_code == 201:
            continue
        assert r.status_code == 503, f"unexpected status {r.status_code}: {r.text[:300]}"
        b = r.json()
        assert b.get("type") == "urn:problem-type:belgif:serviceUnavailable", b
        assert b.get("detail") == detail, b
        assert r.headers.get("Retry-After") == retry, r.headers


@then('the parallel responses have distinct Location headers for versions {first:d} to {last:d}')
def step_parallel_locations(context, first, last):
    locations = sorted(r.headers.get("Location", "") for r in context.parallel)
    versions = sorted(r.json()["version"] for r in context.parallel)
    assert versions == list(range(first, last + 1)), f"versions {versions}"
    assert len(set(locations)) == len(locations), f"locations not distinct: {locations}"


# ---------------------------------------------------------------- the operation matrix (D16, D17)

def _op_request(context, op):
    v = vm.vocab_name(context, "Subject")
    e = vm.ext_name(context, "ExtSubject")
    new_name = vm.vocab_name(context, "Fresh")
    ext_ns = f"https://ext.example.org/{context.run}/ExtSubject#"
    file_part = ("vocabularyFile", vm.valid_turtle(context), True)
    ops = {
        "getVocabularies": ("GET", "/vocabularies", {}),
        "createVocabulary": ("POST", "/vocabularies", {"files": vm.multipart(
            [("name", new_name, False), ("description", "Fresh", False), ("changelog", "Initial version.", False),
             file_part])}),
        "searchVocabularies": ("POST", "/vocabularies/searches", {"json": {"items": [{"name": v}]}}),
        "getVocabulary": ("GET", f"/vocabularies/{v}", {}),
        "updateVocabularyStatus": ("PATCH", f"/vocabularies/{v}", {"json": {"status": "DEPRECATED"}}),
        "getVocabularyVersions": ("GET", f"/vocabularies/{v}/versions", {}),
        "createVocabularyVersion": ("POST", f"/vocabularies/{v}/versions", {"files": vm.multipart(
            [("description", "Next", False), ("changelog", "Next.", False), file_part])}),
        "getVocabularyVersion": ("GET", f"/vocabularies/{v}/versions/1", {}),
        "updateVocabularyVersionStatus": ("PATCH", f"/vocabularies/{v}/versions/1", {"json": {"status": "DEPRECATED"}}),
        "downloadVocabulary": ("GET", f"/content/{v}", {}),
        "downloadVocabularyVersion": ("GET", f"/content/{v}/versions/1", {}),
        "getExternalVocabularies": ("GET", "/external-vocabularies", {}),
        "createExternalVocabulary": ("POST", "/external-vocabularies", {"files": vm.multipart(
            [("name", vm.ext_name(context, "ExtFresh"), False),
             ("namespaceUri", f"https://ext.example.org/{context.run}/ExtFresh#", False),
             ("vocabularyFile", vm.W3C_PREFIXES.encode() + b"<urn:x:a> rdfs:label \"a\" .\n", True)])}),
        "searchExternalVocabularies": ("POST", "/external-vocabulary/searches", {"json": {"namespaceUri": ext_ns}}),
        "downloadExternalVocabularyContent": ("POST", "/external-vocabularies/content", {"json": {"namespaceUri": ext_ns}}),
        "getExternalVocabularyContentByName": ("GET", f"/external-vocabularies/{e}/content", {}),
        "getExternalVocabulary": ("GET", f"/external-vocabularies/{e}", {}),
        "deleteExternalVocabulary": ("DELETE", f"/external-vocabularies/{e}", {}),
        "getHealth": ("GET", "/health", {}),
    }
    if op not in ops:
        raise AssertionError(f"Unknown operation {op}")
    return ops[op]


@given('the data for the operation matrix exists')
def step_matrix_data(context):
    _must(vm.create_vocabulary(context, "Subject"), 201, "create Subject")
    _must(vm.register_external(context, "ExtSubject"), 201, "register ExtSubject")


@when('I call the operation "{op:Q}"')
def step_call_op(context, op):
    method, path, kwargs = _op_request(context, op)
    vm.request(context, method, path, **kwargs)


@then('the operation "{op:Q}" changed nothing')
def step_op_changed_nothing(context, op):
    v = vm.vocab_name(context, "Subject")
    if op == "createVocabulary":
        assert vm.list_by_name(context, vm.vocab_name(context, "Fresh"))["total"] == 0
    elif op == "updateVocabularyStatus":
        assert vm.admin_get(context, f"/vocabularies/{v}").json()["status"] == "ACTIVE"
    elif op == "createVocabularyVersion":
        assert vm.admin_get(context, f"/vocabularies/{v}/versions").json()["total"] == 1
    elif op == "updateVocabularyVersionStatus":
        assert vm.admin_get(context, f"/vocabularies/{v}/versions/1").json()["status"] == "ACTIVE"
    elif op == "createExternalVocabulary":
        r = vm.request(context, "POST", "/external-vocabulary/searches", token=vm.token_for("ga-admin"),
                       json={"namespaceUri": f"https://ext.example.org/{context.run}/ExtFresh#"})
        assert r.status_code == 200 and r.json()["total"] == 0, r.text
    elif op == "deleteExternalVocabulary":
        e = vm.ext_name(context, "ExtSubject")
        assert vm.admin_get(context, f"/external-vocabularies/{e}").status_code == 200
    else:
        raise AssertionError(f"no write check for {op}")


# ================================================================ response checks

@then('the response status is {code:d}')
def step_status(context, code):
    if context.response.status_code != code:
        fail(context, f"Expected status {code}, got {context.response.status_code}")


@then('the response status is not {code:d}')
def step_status_not(context, code):
    if context.response.status_code == code:
        fail(context, f"Did not expect status {code}")


@then('the response status is one of "{codes:Q}"')
def step_status_one_of(context, codes):
    allowed = [int(c) for c in codes.split(",")]
    if context.response.status_code not in allowed:
        fail(context, f"Expected one of {allowed}, got {context.response.status_code}")


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def step_problem(context, code, ptype):
    r = context.response
    if r.status_code != code:
        fail(context, f"Expected status {code}, got {r.status_code}")
    if media_type(r) != PROBLEM_JSON:
        fail(context, f"Expected Content-Type {PROBLEM_JSON}, got {r.headers.get('Content-Type')}")
    b = body(context)
    if b.get("status") != code:
        fail(context, f"Problem status field is {b.get('status')}, expected {code}")
    if b.get("type") != ptype:
        fail(context, f"Problem type is {b.get('type')}, expected {ptype}")


@then('the response is a problem with status {code:d}')
def step_problem_any_type(context, code):
    r = context.response
    if r.status_code != code:
        fail(context, f"Expected status {code}, got {r.status_code}")
    if media_type(r) != PROBLEM_JSON:
        fail(context, f"Expected Content-Type {PROBLEM_JSON}, got {r.headers.get('Content-Type')}")
    if body(context).get("status") != code:
        fail(context, "problem status field differs")


@then('the problem type follows API-46 and API-47')
def step_problem_api46(context):
    t = body(context).get("type", "")
    # D48: API-46 applies to the simpl codes only; API-47 (lowerCamelCase type part) to every problem
    if context.response.status_code in (401, 403, 406, 409, 413, 415):
        if not re.match(r"^urn:problem-type:simpl:[a-zA-Z0-9]+(:[a-zA-Z0-9]+){0,2}$", t):
            fail(context, f"type {t} does not follow API-46")
    last = t.split(":")[-1]
    if not re.match(r"^[a-z][a-zA-Z0-9]*$", last):
        fail(context, f"type part '{last}' of {t} is not lowerCamelCase (API-47)")


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    if body(context).get("title") != title:
        fail(context, f"title is {body(context).get('title')!r}, expected {title!r}")


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    expected = X(context, detail)
    if body(context).get("detail") != expected:
        fail(context, f"detail is {body(context).get('detail')!r}, expected {expected!r}")


@then('the problem has a detail')
def step_problem_has_detail(context):
    d = body(context).get("detail")
    if not isinstance(d, str) or not d.strip():
        fail(context, "problem has no detail")


def _issues(context):
    issues = body(context).get("issues") or []
    if not isinstance(issues, list):
        fail(context, "issues is not a list")
    return issues


@then('the problem has no issues')
def step_no_issues(context):
    if _issues(context):
        fail(context, "expected no issues")


@then('the problem has exactly {n:d} issues')
def step_n_issues(context, n):
    if len(_issues(context)) != n:
        fail(context, f"expected {n} issues, got {len(_issues(context))}")


@then('the problem has exactly one issue')
def step_one_issue(context):
    step_n_issues(context, 1)


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_named(context, where, name):
    name = X(context, name)
    if not any(i.get("in") == where and i.get("name") == name for i in _issues(context)):
        fail(context, f"no issue in {where} named {name!r}")


@then('the problem has exactly one issue, in "{where:Q}" named "{name:Q}"')
def step_exactly_one_issue_named(context, where, name):
    issues = _issues(context)
    name = X(context, name)
    if len(issues) != 1 or issues[0].get("in") != where or issues[0].get("name") != name:
        fail(context, f"expected exactly one issue in {where} named {name!r}")


@then('every issue is in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def step_every_issue(context, where, name, itype):
    issues = _issues(context)
    if not issues:
        fail(context, "no issues")
    for i in issues:
        if i.get("in") != where or i.get("name") != name or i.get("type") != itype:
            fail(context, f"issue {i} is not in {where} named {name} with type {itype}")


@then('the issue in "{where:Q}" named "{name:Q}" has the type "{itype:Q}"')
def step_issue_type(context, where, name, itype):
    name = X(context, name)
    matching = [i for i in _issues(context) if i.get("in") == where and i.get("name") == name]
    if not matching or any(i.get("type") != itype for i in matching):
        fail(context, f"issue in {where} named {name} does not have type {itype}")


@then('the problem has an issue with the detail "{detail:Q}" and the location "{location:Q}"')
def step_issue_detail_location(context, detail, location):
    detail, location = X(context, detail), X(context, location)
    if not any(i.get("detail") == detail and i.get("location") == location for i in _issues(context)):
        fail(context, f"no issue with detail {detail!r} and location {location!r}")


@then('the problem has an issue with the detail "{detail:Q}" and a location matching "{pattern:Q}"')
def step_issue_detail_location_re(context, detail, pattern):
    detail = X(context, detail)
    pattern = X(context, pattern)
    if not any(i.get("detail") == detail and re.match(pattern, i.get("location") or "") for i in _issues(context)):
        fail(context, f"no issue with detail {detail!r} and location matching {pattern!r}")


@then('the problem has an issue with the detail "{detail:Q}"')
def step_issue_detail(context, detail):
    detail = X(context, detail)
    if not any(i.get("detail") == detail for i in _issues(context)):
        fail(context, f"no issue with detail {detail!r}")


@then('the problem has an issue whose detail starts with "{prefix:Q}" and whose location matches "{pattern:Q}"')
def step_issue_detail_prefix_location(context, prefix, pattern):
    if not any((i.get("detail") or "").startswith(prefix) and re.match(pattern, i.get("location") or "")
               for i in _issues(context)):
        fail(context, f"no issue with detail starting {prefix!r} and location matching {pattern!r}")


@then('every issue has a detail and a location')
def step_issue_detail_and_location(context):
    for i in _issues(context):
        if not i.get("detail") or not i.get("location"):
            fail(context, f"issue without detail or location: {i}")


# ---------------------------------------------------------------- headers and body

@then('the response header "{header:Q}" is "{value:Q}"')
def step_header_is(context, header, value):
    got = context.response.headers.get(header)
    if got != X(context, value):
        fail(context, f"header {header} is {got!r}, expected {X(context, value)!r}")


@then('the response header "{header:Q}" matches "{pattern:Q}"')
def step_header_matches(context, header, pattern):
    got = context.response.headers.get(header) or ""
    if not re.match(X(context, pattern), got):
        fail(context, f"header {header} {got!r} does not match {pattern!r}")


@then('the response header "{header:Q}" is present')
def step_header_present(context, header):
    if not context.response.headers.get(header):
        fail(context, f"header {header} missing")


@then('the response header "{header:Q}" equals the response field "{path:Q}"')
def step_header_eq_field(context, header, path):
    got = context.response.headers.get(header)
    if got != vm.get_path(body(context), path):
        fail(context, f"header {header}={got!r} differs from field {path}")


@then('the response media type is "{mtype:Q}"')
def step_media_type(context, mtype):
    if media_type(context.response) != mtype:
        fail(context, f"media type is {context.response.headers.get('Content-Type')!r}, expected {mtype}")


@then('the response content type is exactly "{ctype:Q}"')
def step_ctype_exact(context, ctype):
    if context.response.headers.get("Content-Type") != ctype:
        fail(context, f"Content-Type is {context.response.headers.get('Content-Type')!r}, expected exactly {ctype!r}")


@then('the response has a Content-Type header')
def step_has_ctype(context):
    if context.response.content and not context.response.headers.get("Content-Type"):
        fail(context, "response with a body has no Content-Type (API-36)")


@then('the response is a download named "{filename:Q}"')
def step_download_name(context, filename):
    # D29: type attachment and filename as decided; other parameters (filename*) are allowed
    cd = context.response.headers.get("Content-Disposition") or ""
    parts = [p.strip() for p in cd.split(";")]
    params = {}
    for p in parts[1:]:
        if "=" in p:
            k, v = p.split("=", 1)
            params[k.strip().lower()] = v.strip().strip('"')
    if not parts or parts[0].lower() != "attachment" or params.get("filename") != X(context, filename):
        fail(context, f"Content-Disposition {cd!r} is not an attachment named {X(context, filename)}")


@then('the response body is the uploaded content of version {n:d} of "{alias:Q}"')
def step_body_is_upload(context, n, alias):
    name = vm.vocab_name(context, alias)
    if context.response.content != context.uploads[(name, n)]:
        fail(context, "downloaded bytes differ from the uploaded bytes")


@then('the response body is the uploaded content of the external vocabulary "{alias:Q}"')
def step_body_is_ext_upload(context, alias):
    if context.response.content != context.ext_uploads[vm.ext_name(context, alias)]:
        fail(context, "downloaded bytes differ from the registered bytes")


@then('the response time is below {seconds:d} seconds')
def step_response_time(context, seconds):
    if context.response_time >= seconds:
        fail(context, f"response took {context.response_time:.1f} s")


def _field(context, path):
    return vm.get_path(body(context), X(context, path))


@then('the response field "{path:Q}" is "{value:Q}"')
def step_field_str(context, path, value):
    got = _field(context, path)
    if got != X(context, value):
        fail(context, f"{path} is {got!r}, expected {X(context, value)!r}")


@then('the response field "{path:Q}" is {n:d}')
def step_field_int(context, path, n):
    got = _field(context, path)
    if got != n or isinstance(got, bool):
        fail(context, f"{path} is {got!r}, expected {n}")


@then('the response field "{path:Q}" is true')
def step_field_true(context, path):
    if _field(context, path) is not True:
        fail(context, f"{path} is not true")


@then('the response field "{path:Q}" is false')
def step_field_false(context, path):
    if _field(context, path) is not False:
        fail(context, f"{path} is not false")


@then('the response field "{path:Q}" is absent or null')
def step_field_absent(context, path):
    if vm.get_path(body(context), X(context, path), default=None) is not None:
        fail(context, f"{path} is present")


@then('the response field "{path:Q}" is an empty list')
def step_field_empty_list(context, path):
    if _field(context, path) != []:
        fail(context, f"{path} is not an empty list")


@then('the response field "{path:Q}" is a time in UTC to the second')
def step_field_time(context, path):
    got = _field(context, path)
    if not isinstance(got, str) or not TIME_RE.match(got):
        fail(context, f"{path} = {got!r} is not ISO 8601 UTC to the second with Z (D58)")


@then('the response field "{path:Q}" matches "{pattern:Q}"')
def step_field_matches(context, path, pattern):
    got = _field(context, path)
    if not isinstance(got, str) or not re.match(X(context, pattern), got):
        fail(context, f"{path} = {got!r} does not match {pattern!r}")


@then('the response field "{path:Q}" equals "{other:Q}" of the remembered "{key:Q}"')
def step_field_eq_remembered(context, path, other, key):
    got = _field(context, path)
    exp = vm.get_path(context.remembered[key], other)
    if got != exp:
        fail(context, f"{path} = {got!r}, expected {other} of {key} = {exp!r}")


@then('the response field "{path:Q}" differs from "{other:Q}" of the remembered "{key:Q}"')
def step_field_ne_remembered(context, path, other, key):
    if _field(context, path) == vm.get_path(context.remembered[key], other):
        fail(context, f"{path} did not change")


@then('the response field "{path:Q}" is the list "{values:Q}"')
def step_field_list(context, path, values):
    exp = [X(context, v.strip()) for v in values.split(",")] if values else []
    if _field(context, path) != exp:
        fail(context, f"{path} = {_field(context, path)!r}, expected {exp!r}")


@then('the response has the fields "{fields:Q}"')
def step_has_fields(context, fields):
    b = body(context)
    missing = [f.strip() for f in fields.split(",") if vm.get_path(b, f.strip(), default=None) is None]
    if missing:
        fail(context, f"missing fields {missing}")


@then('every item has the fields "{fields:Q}"')
def step_items_have_fields(context, fields):
    items = body(context).get("items") or []
    if not items:
        fail(context, "no items")
    for it in items:
        missing = [f.strip() for f in fields.split(",") if it.get(f.strip()) is None]
        if missing:
            fail(context, f"item {it.get('name')} misses {missing}")


@then('the response has {n:d} items')
def step_n_items(context, n):
    items = body(context).get("items")
    if not isinstance(items, list) or len(items) != n:
        fail(context, f"expected {n} items, got {len(items) if isinstance(items, list) else items}")


@then('the items are named, in order')
def step_items_order(context):
    exp = [X(context, row["name"]) for row in context.table]
    got = [it.get("name") for it in body(context).get("items", [])]
    if got != exp:
        fail(context, f"items {got}, expected {exp}")


@then('the items are, in order')
def step_items_table(context):
    items = body(context).get("items", [])
    rows = list(context.table)
    if len(items) != len(rows):
        fail(context, f"expected {len(rows)} items, got {len(items)}")
    for it, row in zip(items, rows):
        for col in context.table.headings:
            exp = X(context, row[col])
            got = it.get(col)
            if isinstance(got, bool):
                got = "true" if got else "false"
            elif got is not None and not isinstance(got, str):
                got = str(got)
            if got != exp:
                fail(context, f"item field {col} is {got!r}, expected {exp!r}")


@then('the items are sorted by "{field:Q}" by code point')
def step_items_sorted(context, field):
    vals = [it.get(field) for it in body(context).get("items", [])]
    if vals != sorted(vals):
        fail(context, f"items not sorted by {field}: {vals}")


@then('the version numbers of the items are "{versions:Q}"')
def step_item_versions(context, versions):
    exp = [int(v) for v in versions.split(",")]
    got = [it.get("version") for it in body(context).get("items", [])]
    if got != exp:
        fail(context, f"versions {got}, expected {exp}")


@then('the page fields are page {page:d}, pageSize {size:d}, total {total:d} and totalPages {pages:d}')
def step_page_fields(context, page, size, total, pages):
    b = body(context)
    got = (b.get("page"), b.get("pageSize"), b.get("total"), b.get("totalPages"))
    if got != (page, size, total, pages):
        fail(context, f"page fields {got}, expected {(page, size, total, pages)}")


@then('totalPages is total divided by pageSize, rounded up')
def step_total_pages(context):
    b = body(context)
    if b["totalPages"] != math.ceil(b["total"] / b["pageSize"]):
        fail(context, "totalPages inconsistent")


@then('the paging link "{link:Q}" is absent')
def step_link_absent(context, link):
    if body(context).get(link) is not None:
        fail(context, f"link {link} present: {body(context).get(link)}")


@then('the response has no paging links')
def step_no_links(context):
    for link in ("self", "first", "prev", "next", "last"):
        if body(context).get(link) is not None:
            fail(context, f"link {link} present")


@then('the paging link "{link:Q}" is "{path:Q}" with the parameters "{params:Q}"')
def step_link(context, link, path, params):
    value = body(context).get(link)
    if not isinstance(value, str):
        fail(context, f"link {link} missing")
    got_path, got_params = vm.link_query(value)
    exp = {}
    for pair in X(context, params).split("&"):
        k, v = pair.split("=", 1)
        exp[k] = v
    # D78: every request parameter is kept with its value; only page changes. A link may in
    # addition carry page, pageSize or sort with their defaults (as the OpenAPI examples do).
    # Defaults the request did not send (page=1, pageSize=20) may be left out of a link (D22, D78).
    defaults = {"page": "1", "pageSize": "20"}
    missing = {k: v for k, v in exp.items()
               if got_params.get(k) != v and not (k not in got_params and defaults.get(k) == v)}
    extra = set(got_params) - set(exp) - {"page", "pageSize", "sort"}
    if not got_path.endswith(X(context, path)) or missing or extra:
        fail(context, f"link {link} is {value!r}, expected {path} with {exp}")


# ---------------------------------------------------------------- validation warnings (D10, D11, D12, D47)

@then('there are no validation warnings')
def step_no_warnings(context):
    b = body(context)
    if "validationWarnings" not in b or b["validationWarnings"] != []:
        fail(context, "validationWarnings must be present and empty")


@then('the validation warnings are')
def step_warnings_are(context):
    warnings = body(context).get("validationWarnings")
    if not isinstance(warnings, list):
        fail(context, "validationWarnings missing")
    rows = list(context.table)
    if len(warnings) != len(rows):
        fail(context, f"expected {len(rows)} warnings, got {warnings}")
    for row in rows:
        ok = False
        for w in warnings:
            if w.get("code") != row["code"]:
                continue
            if "location" in context.table.headings and w.get("location") != X(context, row["location"]):
                continue
            if "description" in context.table.headings and row["description"] != "*" \
                    and w.get("description") != X(context, row["description"]):
                continue
            if not w.get("description"):
                continue
            ok = True
        if not ok:
            fail(context, f"no warning like {dict(row.items())} in {warnings}")


@then('every validation warning code is in the closed set')
def step_warning_codes(context):
    for w in body(context).get("validationWarnings", []):
        if w.get("code") not in WARNING_CODES:
            fail(context, f"warning code {w.get('code')} not in the closed set")


@then('the response has no field "{field:Q}" or it is an empty list')
def step_no_field_or_empty(context, field):
    v = body(context).get(field)
    if v not in (None, []):
        fail(context, f"{field} = {v!r}")


# ---------------------------------------------------------------- stored state (read as ga-admin)

@then('no vocabulary named "{alias:Q}" is stored')
def step_not_stored(context, alias):
    name = vm.vocab_name(context, alias)
    items = vm.list_by_name(context, name)["items"]
    if any(it["name"].lower() == name.lower() for it in items):
        raise AssertionError(f"vocabulary {name} was stored")


@then('no vocabulary of this scenario is stored')
def step_none_stored(context):
    data = vm.list_by_name(context, context.run)
    if data["total"] != 0:
        raise AssertionError(f"{data['total']} vocabularies of run {context.run} stored")


@then('exactly {n:d} vocabularies of this scenario are stored')
def step_n_stored(context, n):
    data = vm.list_by_name(context, context.run)
    if data["total"] != n:
        raise AssertionError(f"{data['total']} vocabularies of run {context.run} stored, expected {n}")


@then('the vocabulary "{alias:Q}" has {n:d} versions')
def step_has_versions(context, alias, n):
    name = vm.vocab_name(context, alias)
    r = vm.admin_get(context, f"/vocabularies/{name}/versions", params={"pageSize": 100})
    assert r.status_code == 200, r.text
    if r.json()["total"] != n:
        raise AssertionError(f"{name} has {r.json()['total']} versions, expected {n}")


@then('version {n:d} of "{alias:Q}" has the status "{status:Q}"')
def step_version_status(context, n, alias, status):
    name = vm.vocab_name(context, alias)
    r = vm.admin_get(context, f"/vocabularies/{name}/versions/{n}")
    assert r.status_code == 200, r.text
    if r.json()["status"] != status:
        raise AssertionError(f"version {n} of {name} is {r.json()['status']}, expected {status}")


@then('versions {first:d} to {last:d} of "{alias:Q}" have the status "{status:Q}"')
def step_versions_status(context, first, last, alias, status):
    for n in range(first, last + 1):
        step_version_status(context, n, alias, status)


@then('the vocabulary "{alias:Q}" has the status "{status:Q}"')
def step_vocab_status(context, alias, status):
    name = vm.vocab_name(context, alias)
    r = vm.admin_get(context, f"/vocabularies/{name}")
    assert r.status_code == 200, r.text
    if r.json()["status"] != status:
        raise AssertionError(f"{name} is {r.json()['status']}, expected {status}")


@then('version {n:d} of "{alias:Q}" has a last update date equal to the creation date of version {m:d}')
def step_lastupdate_eq(context, n, alias, m):
    name = vm.vocab_name(context, alias)
    a = vm.admin_get(context, f"/vocabularies/{name}/versions/{n}").json()
    b = vm.admin_get(context, f"/vocabularies/{name}/versions/{m}").json()
    if a["lastUpdateDate"] != b["creationDate"]:
        raise AssertionError(f"lastUpdateDate {a['lastUpdateDate']} != creationDate {b['creationDate']}")


@then('no external vocabulary with namespace "{ns:Q}" is stored')
def step_ext_not_stored(context, ns):
    r = vm.request(context, "POST", "/external-vocabulary/searches", token=vm.token_for("ga-admin"),
                   json={"namespaceUri": X(context, ns)})
    assert r.status_code == 200, r.text
    if r.json()["total"] != 0:
        raise AssertionError(f"an external vocabulary with namespace {X(context, ns)} is stored")


@then('no external vocabulary named "{alias:Q}" is stored')
def step_ext_name_not_stored(context, alias):
    r = vm.admin_get(context, f"/external-vocabularies/{vm.ext_name(context, alias)}")
    if r.status_code != 404:
        raise AssertionError(f"external vocabulary {alias} is stored: {r.status_code}")


@then('the external vocabulary "{alias:Q}" is stored')
def step_ext_stored(context, alias):
    r = vm.admin_get(context, f"/external-vocabularies/{vm.ext_name(context, alias)}")
    if r.status_code != 200:
        raise AssertionError(f"external vocabulary {alias} not stored: {r.status_code}")


# UI steps (third person, 'the user ...' and 'the UI ...') are in ui_steps.py (step 60).


# ================================================================ @wip placeholders

@given('the store answers the list query with an error that is not a timeout or a connection failure')
def step_wip_store_error(context):
    raise NotImplementedError("needs fault injection inside the component; not possible black box")
