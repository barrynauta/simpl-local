"""Shared step library of the Vocabulary Manager API suite (black box, HTTP only).

Names in quotes are logical names: the suite adds the run id, so 'Health' is stored as
'Health<run id>'. Strings may hold template tokens: {{run}}, {{name:Health}}, {{base}}
(contentBase), {{ext:Label}} (an external namespace of this run), {{var:x}} (a remembered
value), {{datepart:x}}, {{email:ga-admin}}.
"""
import re
import threading

import parse
from behave import given, register_type, step, then, when

from vmtest import auth, operations, turtle
from vmtest.world import field, json_body, parse_value, wait_a_second


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(Q=_quoted)

DATE_TIME_RE = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


def W(context):
    return context.world


def body(context):
    return json_body(W(context).resp)


def describe(resp):
    return f"{resp.request.method} {resp.request.url} -> {resp.status_code} {resp.headers.get('Content-Type')} {resp.text[:800]}"


# ---------------------------------------------------------------------------
# Who calls
# ---------------------------------------------------------------------------
@given('I am authenticated as "{user:Q}"')
def step_auth(context, user):
    W(context).user = user
    W(context).raw_token = None


@given("I am not authenticated")
def step_no_auth(context):
    W(context).user = None
    W(context).raw_token = None


@given('I use the invalid token "{token:Q}"')
def step_invalid_token(context, token):
    W(context).raw_token = token


@given("I use a token with a tampered signature")
def step_tampered(context):
    W(context).raw_token = auth.tampered_token()


@given("I use an unsigned token")
def step_unsigned(context):
    W(context).raw_token = auth.unsigned_token()


# ---------------------------------------------------------------------------
# Setup, always as ga-admin
# ---------------------------------------------------------------------------
@given('the vocabulary "{name:Q}" exists')
def step_vocab_exists(context, name):
    W(context).setup_vocabulary(name)


@given('the vocabularies "{names:Q}" exist')
def step_vocabs_exist(context, names):
    for name in [n.strip() for n in names.split(",")]:
        W(context).setup_vocabulary(name)


@given('the vocabularies "{names:Q}" exist, created one second apart')
def step_vocabs_exist_apart(context, names):
    for i, name in enumerate([n.strip() for n in names.split(",")]):
        if i:
            wait_a_second()
        W(context).setup_vocabulary(name)


@given('the vocabulary "{name:Q}" exists with {count:d} versions')
def step_vocab_versions(context, name, count):
    W(context).setup_vocabulary(name)
    for _ in range(count - 1):
        W(context).setup_version(name)


@given('the vocabulary "{name:Q}" exists with the content of the file "{fixture:Q}"')
def step_vocab_fixture(context, name, fixture):
    W(context).setup_vocabulary(name, content=W(context).fixture(fixture))


@given('the vocabulary "{name:Q}" has a new version')
def step_vocab_new_version(context, name):
    W(context).setup_version(name)


@given('the vocabulary "{name:Q}" is deprecated')
def step_vocab_deprecated(context, name):
    W(context).setup_patch(f"/vocabularies/{W(context).real(name)}", "DEPRECATED")


@given('version {version:d} of "{name:Q}" is set to "{status:Q}"')
def step_version_status(context, version, name, status):
    W(context).setup_patch(f"/vocabularies/{W(context).real(name)}/versions/{version}", status)


@given('the external vocabulary "{name:Q}" is registered')
def step_ext_registered(context, name):
    W(context).setup_external(name)


@given('the external vocabulary "{name:Q}" is registered with the namespace "{ns:Q}"')
def step_ext_registered_ns(context, name, ns):
    W(context).setup_external(name, namespace=W(context).expand(ns))


@given('the external vocabulary "{name:Q}" is registered with the namespace "{ns:Q}" and the alias "{alias:Q}"')
def step_ext_registered_alias(context, name, ns, alias):
    W(context).setup_external(name, namespace=W(context).expand(ns), aliases=[W(context).expand(alias)])


@step("a second has passed")
def step_second(context):
    wait_a_second()


@step('I remember the response field "{path:Q}" as "{var:Q}"')
def step_remember(context, path, var):
    W(context).vars[var] = field(body(context), path)


@step('I remember the field "{path:Q}" of "{rpath:Q}" as "{var:Q}"')
def step_remember_of(context, path, rpath, var):
    resp = W(context).admin_get(rpath)
    assert resp.status_code == 200, describe(resp)
    W(context).vars[var] = field(resp.json(), path)


# ---------------------------------------------------------------------------
# Requests: vocabularies and versions
# ---------------------------------------------------------------------------
def _create(context, name, parts, path="/vocabularies"):
    world = W(context)
    resp = world.call("POST", path, files=world.multipart(parts))
    world.remember_created(name, parts, resp)
    return resp


@when('I create the vocabulary "{name:Q}"')
def step_create(context, name):
    _create(context, name, W(context).create_parts(name))


@when('I create the vocabulary "{name:Q}" with:')
def step_create_with(context, name):
    world = W(context)
    parts = world.apply_table(world.create_parts(name), context.table, world)
    _create(context, name, parts)


@when('I create the vocabulary "{name:Q}" from the file "{fixture:Q}"')
def step_create_file(context, name, fixture):
    world = W(context)
    _create(context, name, world.create_parts(name, content=world.fixture(fixture)))


@when('I create the vocabulary "{name:Q}" with the prefix "{prefix:Q}" for "{uri:Q}"')
def step_create_prefix(context, name, prefix, uri):
    world = W(context)
    content = turtle.default_content(world.run, name, world.next_variant(), [(prefix, world.expand(uri))])
    _create(context, name, world.create_parts(name, content=content))


@when('I create the vocabulary "{name:Q}" with the prefixes:')
def step_create_prefixes(context, name):
    world = W(context)
    prefixes = [(r["prefix"], world.expand(r["namespace"])) for r in context.table]
    content = turtle.default_content(world.run, name, world.next_variant(), prefixes)
    _create(context, name, world.create_parts(name, content=content))


@when('I create the vocabulary "{name:Q}" with a query parameter "{query:Q}"')
def step_create_query(context, name, query):
    world = W(context)
    parts = world.create_parts(name)
    world.call("POST", "/vocabularies?" + query, files=world.multipart(parts))


@when('I create the vocabulary "{name:Q}" as JSON')
def step_create_json(context, name):
    world = W(context)
    world.call("POST", "/vocabularies", json={
        "name": world.real(name), "description": "JSON body", "changelog": "Initial version.",
        "vocabularyFile": turtle.default_content(world.run, name, 1).decode(),
    })


def _version_path(world, name):
    return f"/vocabularies/{world.real(name)}/versions"


@when('I create a new version of "{name:Q}"')
def step_new_version(context, name):
    world = W(context)
    _create(context, name, world.version_parts(name), _version_path(world, name))


@when('I create a new version of "{name:Q}" with:')
def step_new_version_with(context, name):
    world = W(context)
    parts = world.apply_table(world.version_parts(name), context.table, world)
    _create(context, name, parts, _version_path(world, name))


@when('I create a new version of "{name:Q}" from the file "{fixture:Q}"')
def step_new_version_file(context, name, fixture):
    world = W(context)
    _create(context, name, world.version_parts(name, content=world.fixture(fixture)), _version_path(world, name))


@when('I create a new version of "{name:Q}" with the prefix "{prefix:Q}" for "{uri:Q}"')
def step_new_version_prefix(context, name, prefix, uri):
    world = W(context)
    content = turtle.default_content(world.run, name, world.next_variant(), [(prefix, world.expand(uri))])
    _create(context, name, world.version_parts(name, content=content), _version_path(world, name))


@when('I create a new version of "{name:Q}" as JSON')
def step_new_version_json(context, name):
    world = W(context)
    world.call("POST", _version_path(world, name), json={
        "description": "JSON body", "changelog": "Change",
        "vocabularyFile": turtle.default_content(world.run, name, 99).decode(),
    })


def _latest(world, name):
    versions = world.vocabs[name]["versions"]
    n = max(versions)
    return n, versions[n]


@when('I resubmit the latest version of "{name:Q}" unchanged with a new change log')
def step_resubmit(context, name):
    world = W(context)
    _, latest = _latest(world, name)
    parts = [("description", latest["description"]), ("changelog", "Resubmitted " + world.run),
             ("vocabularyFile", latest["content"])]
    _create(context, name, parts, _version_path(world, name))


@when('I resubmit the latest version of "{name:Q}" with the description padded with spaces')
def step_resubmit_padded(context, name):
    world = W(context)
    _, latest = _latest(world, name)
    parts = [("description", "  " + latest["description"] + "  "), ("changelog", "Padded"),
             ("vocabularyFile", latest["content"])]
    _create(context, name, parts, _version_path(world, name))


@when('I resubmit the content of the latest version of "{name:Q}" with a new description')
def step_resubmit_new_description(context, name):
    world = W(context)
    _, latest = _latest(world, name)
    parts = [("description", "A new description " + world.run), ("changelog", "Description only"),
             ("vocabularyFile", latest["content"])]
    _create(context, name, parts, _version_path(world, name))


@when('I submit new content for "{name:Q}" with the description of the latest version')
def step_new_content_same_description(context, name):
    world = W(context)
    _, latest = _latest(world, name)
    content = turtle.default_content(world.run, name, world.next_variant())
    parts = [("description", latest["description"]), ("changelog", "Content only"),
             ("vocabularyFile", content)]
    _create(context, name, parts, _version_path(world, name))


@when('I create {count:d} new versions of "{name:Q}" in parallel')
def step_parallel_versions(context, count, name):
    world = W(context)
    path = world.expand(_version_path(world, name))
    jobs = [world.multipart(world.version_parts(name)) for _ in range(count)]
    results = [None] * count
    barrier = threading.Barrier(count)

    def run(i):
        barrier.wait()
        results[i] = world.call("POST", path, record=False, files=jobs[i])

    threads = [threading.Thread(target=run, args=(i,)) for i in range(count)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    world.parallel = results


@when('two requests create the vocabulary "{name:Q}" at the same time')
def step_parallel_create(context, name):
    world = W(context)
    parts_a = world.create_parts(name)
    parts_b = world.create_parts(name)
    results = [None, None]
    barrier = threading.Barrier(2)

    def run(i, parts):
        barrier.wait()
        results[i] = world.call("POST", "/vocabularies", record=False, files=world.multipart(parts))

    threads = [threading.Thread(target=run, args=(0, parts_a)),
               threading.Thread(target=run, args=(1, parts_b.copy()))]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    world.parallel = results


@when('two requests set the status of "{name:Q}" to "{status:Q}" at the same time')
def step_parallel_patch(context, name, status):
    world = W(context)
    path = world.expand(f"/vocabularies/{world.real(name)}")
    results = [None, None]
    barrier = threading.Barrier(2)

    def run(i):
        barrier.wait()
        results[i] = world.call("PATCH", path, record=False, json={"status": status})

    threads = [threading.Thread(target=run, args=(i,)) for i in range(2)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    world.parallel = results


# ---------------------------------------------------------------------------
# Requests: generic and status changes
# ---------------------------------------------------------------------------
@when('I set the status of "{name:Q}" to "{status:Q}"')
def step_patch_vocab(context, name, status):
    world = W(context)
    world.call("PATCH", f"/vocabularies/{world.real(name)}", json={"status": status})


@when('I set the status of version {version:d} of "{name:Q}" to "{status:Q}"')
def step_patch_version(context, version, name, status):
    world = W(context)
    world.call("PATCH", f"/vocabularies/{world.real(name)}/versions/{version}", json={"status": status})


@when('I send "{method:Q}" to "{path:Q}"')
def step_send(context, method, path):
    W(context).call(method, path)


@when('I send "{method:Q}" to "{path:Q}" with the JSON body:')
def step_send_json(context, method, path):
    world = W(context)
    world.call(method, path, data=world.expand(context.text).encode("utf-8"),
               headers={"Content-Type": "application/json"})


@when('I send "{method:Q}" to "{path:Q}" with the body "{payload:Q}" as "{ctype:Q}"')
def step_send_raw(context, method, path, payload, ctype):
    world = W(context)
    world.call(method, path, data=world.expand(payload).encode("utf-8"), headers={"Content-Type": ctype})


@when('I send "{method:Q}" to "{path:Q}" with the header "{name:Q}" set to "{value:Q}"')
def step_send_header(context, method, path, name, value):
    W(context).call(method, path, headers={name: value})


@when('I list the vocabularies with "{query:Q}"')
def step_list(context, query):
    W(context).call("GET", "/vocabularies?" + query if query else "/vocabularies")


@when('I list the versions of "{name:Q}" with "{query:Q}"')
def step_list_versions(context, name, query):
    world = W(context)
    path = f"/vocabularies/{world.real(name)}/versions"
    world.call("GET", path + ("?" + query if query else ""))


@when('I follow the "{link:Q}" link')
def step_follow(context, link):
    href = body(context)[link]
    W(context).call("GET", href)


@when("I search for vocabularies with:")
def step_search(context):
    world = W(context)
    world.call("POST", "/vocabularies/searches", data=world.expand(context.text).encode("utf-8"),
               headers={"Content-Type": "application/json"})


@when("I search for vocabularies with {count:d} different names")
def step_search_many(context, count):
    world = W(context)
    items = [{"name": world.real(f"Missing{i}")} for i in range(count)]
    world.call("POST", "/vocabularies/searches", json={"items": items})


@when("I search for external vocabularies with:")
def step_search_ext(context):
    world = W(context)
    world.call("POST", "/external-vocabulary/searches", data=world.expand(context.text).encode("utf-8"),
               headers={"Content-Type": "application/json"})


@when("I search for external vocabularies with {ns:d} namespace URIs and {aliases:d} alias URIs")
def step_search_ext_many(context, ns, aliases):
    world = W(context)
    req = {}
    if ns:
        req["namespaceUris"] = [f"https://none.example.org/{world.run}/n{i}" for i in range(ns)]
    if aliases:
        req["aliasUris"] = [f"https://none.example.org/{world.run}/a{i}" for i in range(aliases)]
    world.call("POST", "/external-vocabulary/searches", json=req)


@when('I request the external content for the namespace "{ns:Q}"')
def step_ext_content(context, ns):
    world = W(context)
    world.call("POST", "/external-vocabularies/content", json={"namespaceUri": world.expand(ns)})


@when('I register the external vocabulary "{name:Q}"')
def step_register(context, name):
    world = W(context)
    parts = world.external_parts(name)
    resp = world.call("POST", "/external-vocabularies", files=world.multipart(parts))
    if resp.status_code == 201:
        world.externals[name] = {"namespace": dict(parts)["namespaceUri"],
                                 "content": next(v for f, v in parts if f == "vocabularyFile")}


@when('I register the external vocabulary "{name:Q}" with:')
def step_register_with(context, name):
    world = W(context)
    parts = world.apply_table(world.external_parts(name), context.table, world)
    resp = world.call("POST", "/external-vocabularies", files=world.multipart(parts))
    if resp.status_code == 201:
        world.externals[name] = {"namespace": dict(parts).get("namespaceUri"),
                                 "content": next((v for f, v in parts if f == "vocabularyFile"), None)}


@when('I register the external vocabulary "{name:Q}" with {count:d} alias URIs')
def step_register_aliases(context, name, count):
    world = W(context)
    parts = world.external_parts(name)
    for i in range(count):
        parts.append(("aliasUris", f"https://alias.example.org/{world.run}/{name.lower()}/{i}"))
    world.call("POST", "/external-vocabularies", files=world.multipart(parts))


@when('I register the external vocabulary "{name:Q}" as JSON')
def step_register_json(context, name):
    world = W(context)
    world.call("POST", "/external-vocabularies", json={
        "name": world.real(name), "namespaceUri": world.ext_namespace(name),
        "vocabularyFile": "@prefix x: <https://x.example.org/> .",
    })


@when('I call the operation "{op:Q}"')
def step_call_op(context, op):
    world = W(context)
    world.operation = op
    method, path, kwargs = operations.build(world, op)
    world.call(method, path, **kwargs)


# ---------------------------------------------------------------------------
# Checks: status, headers, problems
# ---------------------------------------------------------------------------
@then("the response status is {code:d}")
def step_status(context, code):
    resp = W(context).resp
    assert resp.status_code == code, f"expected {code}, got {describe(resp)}"


@then("the response status is {code1:d} or {code2:d}")
def step_status_either(context, code1, code2):
    resp = W(context).resp
    assert resp.status_code in (code1, code2), f"expected {code1} or {code2}, got {describe(resp)}"


@then('the operation "{op:Q}" answers {code:d}')
def step_op_answers(context, op, code):
    step_status(context, code)


@then("the refused request changed nothing")
def step_nothing_changed(context):
    operations.check_nothing_changed(W(context), W(context).operation)


@then('the response media type is "{media:Q}"')
def step_media(context, media):
    resp = W(context).resp
    ctype = resp.headers.get("Content-Type", "")
    assert ctype.split(";")[0].strip().lower() == media.lower(), f"media type {ctype!r}: {describe(resp)}"


@then("the response has a Content-Type header")
def step_has_ctype(context):
    resp = W(context).resp
    if resp.content:
        assert resp.headers.get("Content-Type"), f"no Content-Type on a response with a body: {describe(resp)}"


@then('the response header "{name:Q}" is "{value:Q}"')
def step_header_is(context, name, value):
    resp = W(context).resp
    actual = resp.headers.get(name)
    expected = W(context).expand(value)
    assert actual == expected, f"header {name}: {actual!r} != {expected!r}; {describe(resp)}"


@then('the response header "{name:Q}" is absent')
def step_header_absent(context, name):
    assert name not in W(context).resp.headers, W(context).resp.headers


@then('the download file name is "{filename:Q}"')
def step_filename(context, filename):
    resp = W(context).resp
    disp = resp.headers.get("Content-Disposition", "")
    assert disp.lower().startswith("attachment"), f"Content-Disposition {disp!r}"
    match = re.search(r'filename\*?=(?:UTF-8\'\')?"?([^";]+)"?', disp)
    assert match, f"no filename in {disp!r}"
    expected = W(context).expand(filename)
    assert match.group(1) == expected, f"filename {match.group(1)!r} != {expected!r}"


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def step_problem(context, code, ptype):
    resp = W(context).resp
    assert resp.status_code == code, f"expected {code}, got {describe(resp)}"
    ctype = resp.headers.get("Content-Type", "").split(";")[0].strip().lower()
    assert ctype == "application/problem+json", f"media type {ctype!r}: {describe(resp)}"
    data = resp.json()
    assert data.get("status") == code, f"problem status {data.get('status')!r}: {resp.text[:400]}"
    assert data.get("type") == ptype, f"problem type {data.get('type')!r} != {ptype!r}: {resp.text[:400]}"


@then("the response is a problem with status {code:d}")
def step_problem_any(context, code):
    resp = W(context).resp
    assert resp.status_code == code, f"expected {code}, got {describe(resp)}"
    ctype = resp.headers.get("Content-Type", "").split(";")[0].strip().lower()
    assert ctype == "application/problem+json", f"media type {ctype!r}: {describe(resp)}"
    data = resp.json()
    assert data.get("status") == code, resp.text[:400]


@then('the problem type follows the simpl URN form')
def step_problem_simpl(context):
    ptype = body(context).get("type", "")
    assert re.fullmatch(r"urn:problem-type:simpl:[a-z][a-zA-Z0-9]*(:[a-z][a-zA-Z0-9]*)?", ptype), ptype


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    assert body(context).get("title") == title, body(context)


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    expected = W(context).expand(detail)
    assert body(context).get("detail") == expected, f"detail {body(context).get('detail')!r} != {expected!r}"


@then('the problem detail starts with "{detail:Q}"')
def step_problem_detail_starts(context, detail):
    actual = body(context).get("detail") or ""
    assert actual.startswith(W(context).expand(detail)), f"detail {actual!r}"


@then('the problem detail ends with "{detail:Q}"')
def step_problem_detail_ends(context, detail):
    actual = body(context).get("detail") or ""
    assert actual.endswith(W(context).expand(detail)), f"detail {actual!r}"


@then("the problem has a detail")
def step_problem_has_detail(context):
    detail = body(context).get("detail")
    assert isinstance(detail, str) and detail.strip(), body(context)


@then("the problem detail does not leak implementation details")
def step_no_leak(context):
    text = W(context).resp.text
    for marker in ("Exception", "\tat ", "java.", "org.springframework", "com.fasterxml", "Traceback", "SQL"):
        assert marker not in text, f"response leaks '{marker}': {text[:500]}"


@then("the problem type segments are lowerCamelCase")
def step_type_camel(context):
    ptype = body(context).get("type", "")
    assert ptype.startswith("urn:problem-type:"), ptype
    for segment in ptype[len("urn:problem-type:"):].split(":"):
        assert re.fullmatch(r"[a-z][a-zA-Z0-9]*", segment), f"segment {segment!r} of {ptype!r}"


def _issues(context):
    issues = body(context).get("issues")
    assert isinstance(issues, list), f"no issues list: {body(context)}"
    return issues


@then("the problem has {count:d} issue")
@then("the problem has {count:d} issues")
def step_issue_count(context, count):
    issues = _issues(context)
    assert len(issues) == count, f"{len(issues)} issues: {issues}"


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_in(context, where, name):
    issues = _issues(context)
    assert any(i.get("in") == where and i.get("name") == name for i in issues), issues


@then('the problem has an issue in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def step_issue_in_type(context, where, name, itype):
    issues = _issues(context)
    assert any(i.get("in") == where and i.get("name") == name and i.get("type") == itype for i in issues), issues


@then('every issue is in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def step_every_issue(context, where, name, itype):
    issues = _issues(context)
    assert issues, "no issues"
    for i in issues:
        assert i.get("in") == where and i.get("name") == name and i.get("type") == itype, i


@then('the problem has an issue with detail "{detail:Q}"')
def step_issue_detail(context, detail):
    expected = W(context).expand(detail)
    issues = _issues(context)
    assert any(i.get("detail") == expected for i in issues), f"no issue with detail {expected!r}: {issues}"


@then('the problem has an issue with detail "{detail:Q}" and location "{location:Q}"')
def step_issue_detail_loc(context, detail, location):
    expected, loc = W(context).expand(detail), W(context).expand(location)
    issues = _issues(context)
    assert any(i.get("detail") == expected and i.get("location") == loc for i in issues), issues


@then('the first issue has a detail starting with "{detail:Q}"')
def step_first_issue_detail(context, detail):
    issue = _issues(context)[0]
    assert (issue.get("detail") or "").startswith(detail), issue


@then('the first issue has a location matching "{regex:Q}"')
def step_first_issue_loc(context, regex):
    issue = _issues(context)[0]
    assert re.fullmatch(regex, issue.get("location") or ""), issue


@then("every issue has a non-empty location")
def step_issue_locations(context):
    for issue in _issues(context):
        loc = issue.get("location")
        assert isinstance(loc, str) and loc.strip(), f"issue without location: {issue}"


@then("the response holds no metadata")
def step_no_metadata(context):
    data = body(context)
    assert "items" not in data and "name" not in data, data


# ---------------------------------------------------------------------------
# Checks: JSON fields
# ---------------------------------------------------------------------------
@then('the response field "{path:Q}" matches "{regex:Q}"')
def step_field_matches(context, path, regex):
    actual = field(body(context), path)
    assert isinstance(actual, str) and re.fullmatch(W(context).expand(regex), actual), f"{path}: {actual!r}"


@then('the response field "{path:Q}" is a UTC time to the second')
def step_field_time(context, path):
    actual = field(body(context), path)
    assert isinstance(actual, str) and DATE_TIME_RE.match(actual), f"{path}: {actual!r}"


@then('the response field "{path:Q}" is later than "{var:Q}"')
def step_field_later(context, path, var):
    actual = field(body(context), path)
    before = W(context).vars[var]
    assert actual > before, f"{path}: {actual!r} is not later than {before!r}"


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    try:
        value = field(body(context), path)
    except (KeyError, IndexError):
        return
    assert value is None, f"{path} is present: {value!r}"


@then('the response field "{path:Q}" is present')
def step_field_present(context, path):
    field(body(context), path)


@then('the response has the fields "{names:Q}"')
def step_fields(context, names):
    data = body(context)
    missing = [n.strip() for n in names.split(",") if n.strip() not in data]
    assert not missing, f"missing fields {missing}: {data}"


@then('every item has the fields "{names:Q}"')
def step_item_fields(context, names):
    items = body(context).get("items")
    assert items, body(context)
    for item in items:
        missing = [n.strip() for n in names.split(",") if n.strip() not in item]
        assert not missing, f"missing {missing} in {item}"


@then("the response has {count:d} items")
@then("the response has {count:d} item")
def step_item_count(context, count):
    items = body(context).get("items")
    assert isinstance(items, list) and len(items) == count, f"{len(items or [])} items: {body(context)}"


@then('the item names are "{names:Q}"')
def step_item_names(context, names):
    world = W(context)
    expected = [world.real(n) for n in names.split(",") if n.strip()]
    actual = [i.get("name") for i in body(context).get("items", [])]
    assert actual == expected, f"{actual} != {expected}"


@then('the item names in this run are "{names:Q}"')
def step_item_names_run(context, names):
    world = W(context)
    expected = [world.real(n) for n in names.split(",") if n.strip()]
    actual = [i.get("name") for i in body(context).get("items", []) if world.run in i.get("name", "")]
    assert actual == expected, f"{actual} != {expected}"


@then('every item has the status "{status:Q}"')
def step_items_status(context, status):
    items = body(context).get("items")
    assert items and all(i.get("status") == status for i in items), items


@then("the items with the same status are grouped and sorted by name ascending")
def step_status_grouped(context):
    items = body(context).get("items", [])
    seen, last = [], None
    for item in items:
        if item["status"] != last:
            assert item["status"] not in seen, f"status groups are not contiguous: {items}"
            seen.append(item["status"])
            last = item["status"]
    for status in seen:
        names = [i["name"] for i in items if i["status"] == status]
        assert names == sorted(names), f"ties not sorted by name: {names}"
    context.world.vars["_status_groups"] = seen


@then("the items with the same status are grouped and sorted by version descending")
def step_status_grouped_versions(context):
    items = body(context).get("items", [])
    seen, last = [], None
    for item in items:
        if item["status"] != last:
            assert item["status"] not in seen, f"status groups are not contiguous: {items}"
            seen.append(item["status"])
            last = item["status"]
    for status in seen:
        numbers = [i["version"] for i in items if i["status"] == status]
        assert numbers == sorted(numbers, reverse=True), f"ties not sorted by version descending: {numbers}"
    context.world.vars["_status_groups"] = seen


@then("the status groups come in the reverse order of the ascending sort")
def step_status_reverse(context):
    world = W(context)
    items = body(context).get("items", [])
    order = []
    for item in items:
        if not order or order[-1] != item["status"]:
            order.append(item["status"])
    asc = world.vars.get("_status_asc")
    assert asc and order == list(reversed(asc)), f"{order} vs ascending {asc}"


@then("I keep the status order as the ascending order")
def step_keep_status(context):
    W(context).vars["_status_asc"] = W(context).vars.get("_status_groups")


@then('the version numbers are "{numbers:Q}"')
def step_version_numbers(context, numbers):
    expected = [int(n) for n in numbers.split(",")]
    actual = [i.get("version") for i in body(context).get("items", [])]
    assert actual == expected, f"{actual} != {expected}"


@then('the item names on this page are sorted by code point')
def step_sorted_code_point(context):
    names = [i.get("name") for i in body(context).get("items", [])]
    assert names == sorted(names), names


@then('the response has the links "{links:Q}" and not "{absent:Q}"')
def step_links(context, links, absent):
    data = body(context)
    for link in [x.strip() for x in links.split(",") if x.strip()]:
        assert isinstance(data.get(link), str) and data[link], f"link {link} missing: {data}"
    for link in [x.strip() for x in absent.split(",") if x.strip()]:
        assert data.get(link) in (None, ""), f"link {link} should be absent: {data}"


@then('the response has none of the links "{links:Q}"')
def step_no_links(context, links):
    data = body(context)
    for link in [x.strip() for x in links.split(",")]:
        assert data.get(link) in (None, ""), f"link {link} should be absent: {data}"


@then('the "{link:Q}" link points to page {page:d}')
def step_link_page(context, link, page):
    href = body(context).get(link) or ""
    assert re.search(rf"[?&]page={page}(&|$)", href), f"{link} = {href!r}"


# ---------------------------------------------------------------------------
# Checks: validation warnings
# ---------------------------------------------------------------------------
def _warnings(context):
    warnings = body(context).get("validationWarnings")
    assert isinstance(warnings, list), f"validationWarnings missing or not a list: {body(context)}"
    return warnings


@then("the response has no validation warnings")
def step_no_warnings(context):
    warnings = _warnings(context)
    assert warnings == [], warnings


@then("the response has {count:d} validation warning")
@then("the response has {count:d} validation warnings")
def step_warning_count(context, count):
    warnings = _warnings(context)
    assert len(warnings) == count, warnings


@then('the response has a validation warning "{code:Q}" with location "{location:Q}"')
def step_warning_loc(context, code, location):
    loc = W(context).expand(location)
    warnings = _warnings(context)
    assert any(w.get("code") == code and w.get("location") == loc for w in warnings), f"{code} at {loc!r}: {warnings}"


@then('the response has a validation warning "{code:Q}" with description "{description:Q}"')
def step_warning_desc(context, code, description):
    desc = W(context).expand(description)
    warnings = _warnings(context)
    assert any(w.get("code") == code and w.get("description") == desc for w in warnings), f"{desc!r}: {warnings}"


@then('the response has no validation warning "{code:Q}"')
def step_no_warning_code(context, code):
    warnings = _warnings(context)
    assert not any(w.get("code") == code for w in warnings), warnings


@then("the response carries no validation warnings")
def step_no_warnings_any(context):
    warnings = body(context).get("validationWarnings")
    assert warnings in (None, []), f"warnings returned: {warnings}"


@then("every validation warning code is in the closed set")
def step_warning_codes(context):
    allowed = {"externalReferenceSkipped", "deprecatedVocabularyReference", "semanticReasoningSkipped"}
    for w in _warnings(context):
        assert w.get("code") in allowed, w
        assert w.get("description") and w.get("location"), w


# ---------------------------------------------------------------------------
# Checks: stored state, read as ga-admin
# ---------------------------------------------------------------------------
@then('the vocabulary "{name:Q}" has the status "{status:Q}"')
def step_vocab_status(context, name, status):
    world = W(context)
    resp = world.admin_get(f"/vocabularies/{world.real(name)}")
    assert resp.status_code == 200, describe(resp)
    assert resp.json().get("status") == status, resp.text


@then('version {version:d} of "{name:Q}" has the status "{status:Q}"')
def step_version_has_status(context, version, name, status):
    world = W(context)
    resp = world.admin_get(f"/vocabularies/{world.real(name)}/versions/{version}")
    assert resp.status_code == 200, describe(resp)
    assert resp.json().get("status") == status, resp.text


@then('the version statuses of "{name:Q}" are "{statuses:Q}"')
def step_version_statuses(context, name, statuses):
    world = W(context)
    resp = world.admin_get(f"/vocabularies/{world.real(name)}/versions?pageSize=100")
    assert resp.status_code == 200, describe(resp)
    actual = {i["version"]: i["status"] for i in resp.json()["items"]}
    expected = {}
    for pair in statuses.split(","):
        number, status = pair.strip().split(":")
        expected[int(number)] = status.strip()
    assert actual == expected, f"{actual} != {expected}"


@then('the vocabulary "{name:Q}" has {count:d} versions')
@then('the vocabulary "{name:Q}" has {count:d} version')
def step_version_count(context, name, count):
    world = W(context)
    resp = world.admin_get(f"/vocabularies/{world.real(name)}/versions")
    assert resp.status_code == 200, describe(resp)
    assert resp.json().get("total") == count, resp.text[:500]


@then('the field "{path:Q}" of "{rpath:Q}" is {value}')
def step_field_of(context, path, rpath, value):
    world = W(context)
    resp = world.admin_get(rpath)
    assert resp.status_code == 200, describe(resp)
    expected = parse_value(world.expand(value))
    actual = field(resp.json(), path)
    assert actual == expected, f"{path} of {rpath}: {actual!r} != {expected!r}"


@then('no vocabulary named "{name:Q}" is stored')
def step_not_stored(context, name):
    world = W(context)
    real = world.real(name)
    items = world.list_all([("name", real)])
    found = [i for i in items if i.get("name", "").lower() == real.lower()]
    assert not found, f"vocabulary {real} is stored: {found}"


@then('exactly one vocabulary named "{name:Q}" in any case is stored')
def step_one_stored(context, name):
    world = W(context)
    real = world.real(name)
    items = world.list_all([("name", real)])
    found = [i for i in items if i.get("name", "").lower() == real.lower()]
    assert len(found) == 1, f"{len(found)} vocabularies named {real}: {found}"


@then("no vocabulary of this run is stored")
def step_none_of_run(context):
    world = W(context)
    items = world.list_all([("name", world.run)])
    assert not items, f"vocabularies of this run are stored: {[i.get('name') for i in items]}"


@then('the external vocabulary "{name:Q}" is not registered')
def step_ext_not_registered(context, name):
    world = W(context)
    real = world.real(name)
    items, _ = world.all_externals()
    found = [i for i in items if i.get("name", "").lower() == real.lower()]
    assert not found, f"external vocabulary {real} is registered: {found}"


@then('exactly one external vocabulary named "{name:Q}" in any case is registered, with its original name')
def step_ext_one(context, name):
    world = W(context)
    real = world.real(name)
    items, _ = world.all_externals()
    found = [i.get("name") for i in items if i.get("name", "").lower() == real.lower()]
    assert found == [real], f"expected only {real}, found {found}"


@then('the response aliasUris are "{uris:Q}" in any order')
def step_alias_set(context, uris):
    def norm(u):
        return u.rstrip("#/")
    expected = {norm(W(context).expand(u.strip())) for u in uris.split(",") if u.strip()}
    actual = body(context).get("aliasUris")
    assert isinstance(actual, list), body(context)
    assert {norm(a) for a in actual} == expected and len(actual) == len(expected), f"{actual} != {expected}"


@then('the external vocabulary list holds "{name:Q}"')
def step_ext_listed(context, name):
    world = W(context)
    items, _ = world.all_externals()
    assert any(i.get("name") == world.real(name) for i in items), "not in the external list"


@then('the external vocabulary "{name:Q}" is still registered')
def step_ext_still(context, name):
    world = W(context)
    resp = world.admin_get(f"/external-vocabularies/{world.real(name)}")
    assert resp.status_code == 200, describe(resp)


@then('the response body equals the content of version {version:d} of "{name:Q}"')
def step_body_equals_version(context, version, name):
    expected = W(context).vocabs[name]["versions"][version]["content"]
    actual = W(context).resp.content
    assert actual == expected, f"content differs: {actual[:200]!r} vs {expected[:200]!r}"


@then('the response body equals the content registered for "{name:Q}"')
def step_body_equals_ext(context, name):
    expected = W(context).externals[name]["content"]
    assert W(context).resp.content == expected, W(context).resp.content[:200]


@then('the response body equals the file "{fixture:Q}"')
def step_body_equals_fixture(context, fixture):
    assert W(context).resp.content == W(context).fixture(fixture), W(context).resp.content[:200]


# ---------------------------------------------------------------------------
# Checks: search results and parallel requests
# ---------------------------------------------------------------------------
@then("the search results are:")
def step_search_results(context):
    world = W(context)
    items = body(context).get("items", [])
    expected = list(context.table)
    assert len(items) == len(expected), f"{len(items)} results: {items}"
    for item, row in zip(items, expected):
        assert item.get("name") == world.real(row["name"]), (item, row)
        assert item.get("version") == int(row["version"]), (item, row)
        assert item.get("status") == row["status"], (item, row)
        assert item.get("latest") == (row["latest"] == "true"), (item, row)
        if "href" in row.headings:
            assert item.get("href") == world.expand(row["href"]), (item, row)
        if "contentHref" in row.headings:
            assert item.get("contentHref") == world.expand(row["contentHref"]), (item, row)


@then('the external search results are "{names:Q}"')
def step_ext_results(context, names):
    world = W(context)
    expected = [world.real(n) for n in names.split(",") if n.strip()]
    actual = [i.get("name") for i in body(context).get("items", [])]
    assert actual == expected, f"{actual} != {expected}"


@then("every parallel response has the status {code:d}")
def step_parallel_all(context, code):
    results = W(context).parallel
    for r in results:
        assert r.status_code == code, describe(r)


@then("the parallel responses are one {code1:d} and one {code2:d}")
def step_parallel_mix(context, code1, code2):
    codes = sorted(r.status_code for r in W(context).parallel)
    assert codes == sorted([code1, code2]), [describe(r) for r in W(context).parallel]


@then("the parallel responses got the version numbers {low:d} to {high:d} each once")
def step_parallel_numbers(context, low, high):
    numbers = sorted(r.json().get("version") for r in W(context).parallel)
    assert numbers == list(range(low, high + 1)), numbers


@then("the parallel responses have distinct Location headers")
def step_parallel_locations(context):
    locations = [r.headers.get("Location") for r in W(context).parallel]
    assert all(locations) and len(set(locations)) == len(locations), locations


@then('the stored statuses of "{name:Q}" after the parallel requests are one of "{a:Q}" or "{b:Q}"')
def step_parallel_states(context, name, a, b):
    world = W(context)
    resp = world.admin_get(f"/vocabularies/{world.real(name)}")
    assert resp.json().get("status") in (a, b), resp.text


@then("the health status is \"{status:Q}\"")
def step_health(context, status):
    data = body(context)
    assert data.get("status") == status, data


@then('the response took less than {seconds:d} seconds')
def step_fast(context, seconds):
    elapsed = W(context).resp.elapsed.total_seconds()
    assert elapsed < seconds, f"took {elapsed} s"




# Registered last: behave takes the first matching step, and this pattern would also match
# 'is absent', 'is present' and the other specific field checks above.
@then('the response field "{path:Q}" is {value}')
def step_field_is(context, path, value):
    expected = parse_value(W(context).expand(value))
    actual = field(body(context), path)
    assert actual == expected, f"{path}: {actual!r} != {expected!r}"
