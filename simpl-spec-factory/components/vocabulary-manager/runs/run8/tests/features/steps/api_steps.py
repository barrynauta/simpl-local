"""Shared step library of the Vocabulary Manager API suite (black box, HTTP only).

Every scenario makes its own data with names built from a unique scenario uid
(see vmtest.resolve). Setup steps ("Given ... exists") always run as ga-admin;
the steps under test run as the principal the scenario chose.
"""
import json
import re
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from behave import given, register_type, step, then, when


def _quoted(text):
    return text


_quoted.pattern = r'[^"]*'


def _word(text):
    return text


_word.pattern = r"[A-Za-z]+"
register_type(Q=_quoted, W=_word)

from vmtest import client, config
from vmtest.resolve import expand

FIXTURES = Path(__file__).resolve().parents[2] / "fixtures"

# --------------------------------------------------------------------------
# helpers
# --------------------------------------------------------------------------


def _x(context, text):
    return expand(context, text)


def _send(context, method, path, record=True, principal="__current__", **kwargs):
    principal = context.principal if principal == "__current__" else principal
    headers = dict(context.extra_headers)
    headers.update(kwargs.pop("headers", {}) or {})
    start = time.monotonic()
    resp = client.request(method, path, principal, base=context.base, headers=headers, **kwargs)
    context.elapsed = time.monotonic() - start
    if record:
        context.resp = resp
    _track_upload(context, method, path, resp, kwargs)
    return resp


def _track_upload(context, method, path, resp, kwargs):
    if method != "POST" or resp.status_code != 201:
        return
    files = kwargs.get("files")
    content = None
    if isinstance(files, dict) and "vocabularyFile" in files:
        content = files["vocabularyFile"][1]
    elif isinstance(files, list):
        for name, value in files:
            if name == "vocabularyFile" and isinstance(value, tuple):
                content = value[1]
    if content is None:
        return
    try:
        body = resp.json()
    except ValueError:
        return
    if path.startswith("/external-vocabularies"):
        context.uploaded[("ext", body.get("name"))] = content
    elif "version" in body:
        context.uploaded[(body.get("name"), int(body["version"]))] = content


def _json(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError("response is not JSON: %s %s" % (resp.status_code, resp.text[:500]))


def _get(obj, path):
    cur = obj
    for part in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if part.startswith("["):
            idx = int(part[1:-1])
            if not isinstance(cur, list) or idx >= len(cur):
                raise KeyError(path)
            cur = cur[idx]
        else:
            if not isinstance(cur, dict) or part not in cur:
                raise KeyError(path)
            cur = cur[part]
    return cur


def _scalar(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if value is None:
        return "null"
    if isinstance(value, (dict, list)):
        return json.dumps(value, sort_keys=True)
    return str(value)


def _fail(context, message):
    resp = context.resp
    extra = ""
    if resp is not None:
        extra = "\n  last response: %s %s\n  %s" % (resp.status_code, resp.headers.get("Content-Type"), resp.text[:1500])
    raise AssertionError(message + extra)


def _media_type(resp):
    return (resp.headers.get("Content-Type") or "").split(";")[0].strip().lower()


def _issues(context):
    body = _json(context.resp)
    issues = body.get("issues") or []
    if not isinstance(issues, list):
        _fail(context, "issues is not a list")
    return issues


def _file_value(context, spec, docstring=None):
    """Turn a file spec of a form table into bytes."""
    if spec == "default":
        return client.default_turtle(context.names.uid + client.random_token(4))
    if spec == "empty":
        return b""
    if spec == "docstring":
        return _x(context, docstring).encode("utf-8")
    if spec.startswith("fixture:"):
        return _x(context, (FIXTURES / spec.split(":", 1)[1]).read_text(encoding="utf-8")).encode("utf-8")
    if spec.startswith("size:"):
        return client.turtle_of_size(context.names.uid, int(spec.split(":", 1)[1]))
    if spec.startswith("text:"):
        return _x(context, spec.split(":", 1)[1]).encode("utf-8")
    if spec.startswith("uploaded:"):
        _, alias, n = spec.split(":")
        return context.uploaded[(context.names.real(alias), int(n))]
    raise ValueError("unknown file spec %s" % spec)


def _form(context, table, docstring=None):
    """Build a multipart body from a table with columns part and value."""
    parts = []
    for row in table:
        part, value = row["part"], row["value"]
        if part == "vocabularyFile":
            parts.append(("vocabularyFile", ("upload.ttl", _file_value(context, value, docstring), "text/turtle")))
        elif part == "<empty name>":
            parts.append(("", (None, _x(context, value))))
        else:
            parts.append((part, (None, _x(context, value))))
    return parts


def _admin(context, method, path, **kwargs):
    resp = _send(context, method, path, record=False, principal="ga-admin", **kwargs)
    return resp


def _setup_ok(resp, what):
    if resp.status_code not in (200, 201, 204):
        raise AssertionError("setup failed (%s): %s %s" % (what, resp.status_code, resp.text[:800]))
    return resp


def _create_vocab_admin(context, alias, content=None, description=None):
    name = context.names.real(alias)
    content = content if content is not None else client.default_turtle(name)
    resp = client.create_vocabulary(name, content=content, description=description, base=context.base)
    _setup_ok(resp, "create vocabulary %s" % name)
    context.uploaded[(name, 1)] = content
    return resp


def _create_version_admin(context, alias, content=None, description=None, changelog=None):
    name = context.names.real(alias)
    marker = client.random_token(6)
    content = content if content is not None else client.default_turtle(name, marker)
    resp = client.create_version(name, content=content, description=description, changelog=changelog, base=context.base)
    _setup_ok(resp, "create version of %s" % name)
    context.uploaded[(name, int(resp.json()["version"]))] = content
    return resp


# --------------------------------------------------------------------------
# who is calling
# --------------------------------------------------------------------------


@given('I am authenticated as "{user:Q}"')
def step_auth(context, user):
    context.principal = user


@given("I am not authenticated")
def step_no_auth(context):
    context.principal = None


@given('I use a "{kind:Q}" token of "{user:Q}"')
def step_bad_token(context, kind, user):
    assert kind in ("tampered", "unsigned", "noaud", "otherissuer"), kind
    context.principal = (user, kind)


@given('I use the bearer token "{raw:Q}"')
def step_raw_token(context, raw):
    context.principal = ("raw", raw)


@given("I use the backend whose store is down")
def step_store_down(context):
    context.base = config.STORE_DOWN_API_URL


@given('every request carries the header "{name:Q}" with value "{value:Q}"')
def step_extra_header(context, name, value):
    context.extra_headers[name] = _x(context, value)


# --------------------------------------------------------------------------
# setup (always as ga-admin)
# --------------------------------------------------------------------------


@given('a vocabulary "{alias:Q}" exists')
def step_vocab_exists(context, alias):
    _create_vocab_admin(context, alias)


@given('a vocabulary "{alias:Q}" exists with description "{description:Q}"')
def step_vocab_exists_desc(context, alias, description):
    _create_vocab_admin(context, alias, description=_x(context, description))


@given('a vocabulary "{alias:Q}" exists with content')
def step_vocab_exists_content(context, alias):
    _create_vocab_admin(context, alias, content=_x(context, context.text).encode("utf-8"))


@given('a vocabulary "{alias:Q}" exists with {n:d} versions')
def step_vocab_exists_versions(context, alias, n):
    _create_vocab_admin(context, alias)
    for _ in range(n - 1):
        _create_version_admin(context, alias)


@step("I wait for the next second")
def step_wait_second(context):
    # Times are to the second (D58): make the next change fall in a later second.
    now = time.time()
    time.sleep(1.05 - (now % 1.0) + 0.05)


@given('a vocabulary "{alias:Q}" exists with {n:d} versions a second apart')
def step_vocab_versions_apart(context, alias, n):
    _create_vocab_admin(context, alias)
    for _ in range(n - 1):
        step_wait_second(context)
        _create_version_admin(context, alias)


@given('vocabulary "{alias:Q}" has a new version')
def step_vocab_new_version(context, alias):
    _create_version_admin(context, alias)


@given('vocabulary "{alias:Q}" has a new version with content')
def step_vocab_new_version_content(context, alias):
    _create_version_admin(context, alias, content=_x(context, context.text).encode("utf-8"))


@given('the vocabulary status of "{alias:Q}" is set to "{status:Q}"')
def step_set_vocab_status(context, alias, status):
    name = context.names.real(alias)
    _setup_ok(_admin(context, "PATCH", "/vocabularies/%s" % name, json={"status": status}), "patch %s" % name)


@given('the version status of version {n:d} of "{alias:Q}" is set to "{status:Q}"')
def step_set_version_status(context, n, alias, status):
    name = context.names.real(alias)
    _setup_ok(_admin(context, "PATCH", "/vocabularies/%s/versions/%d" % (name, n), json={"status": status}), "patch %s v%d" % (name, n))


def _register_external(context, alias, namespace, aliases=None, content=None):
    name = context.names.real(alias)
    namespace = _x(context, namespace)
    if content is None:
        content = (
            "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
            "@prefix t: <%s/> .\n"
            "t:title a rdf:Property .\n" % namespace.rstrip("/#")
        ).encode("utf-8")
    parts = [("name", (None, name)), ("namespaceUri", (None, namespace)), ("description", (None, "External %s" % name))]
    for a in aliases or []:
        parts.append(("aliasUris", (None, _x(context, a))))
    parts.append(("vocabularyFile", ("%s.ttl" % name, content, "text/turtle")))
    resp = _admin(context, "POST", "/external-vocabularies", files=parts)
    _setup_ok(resp, "register external %s" % name)
    context.uploaded[("ext", name)] = content
    return resp


@given('an external vocabulary "{alias:Q}" is registered with namespace "{namespace:Q}"')
def step_ext_registered(context, alias, namespace):
    _register_external(context, alias, namespace)


@given('an external vocabulary "{alias:Q}" is registered with namespace "{namespace:Q}" and alias "{alias_uri:Q}"')
def step_ext_registered_alias(context, alias, namespace, alias_uri):
    _register_external(context, alias, namespace, [alias_uri])


@given('the external vocabulary "{alias:Q}" is deleted')
def step_ext_deleted(context, alias):
    _setup_ok(_admin(context, "DELETE", "/external-vocabularies/%s" % context.names.real(alias)), "delete external")


# --------------------------------------------------------------------------
# requests under test
# --------------------------------------------------------------------------


@when('I send a {method:W} request to "{path:Q}"')
def step_send(context, method, path):
    _send(context, method, _x(context, path))


@when('I send a {method:W} request to "{path:Q}" with header "{name:Q}" set to "{value:Q}"')
def step_send_header(context, method, path, name, value):
    # "<none>" removes the header (requests sends Accept: */* by default).
    _send(context, method, _x(context, path), headers={name: None if value == "<none>" else _x(context, value)})


@when('I send a {method:W} request to "{path:Q}" with JSON body')
def step_send_json(context, method, path):
    _send(context, method, _x(context, path), data=_x(context, context.text).encode("utf-8"),
          headers={"Content-Type": "application/json"})


@when('I send a {method:W} request to "{path:Q}" with header "{name:Q}" set to "{value:Q}" and JSON body')
def step_send_json_header(context, method, path, name, value):
    _send(context, method, _x(context, path), data=_x(context, context.text).encode("utf-8"),
          headers={"Content-Type": "application/json", name: _x(context, value)})


@when('I send a {method:W} request to "{path:Q}" with body "{body:Q}" and content type "{ctype:Q}"')
def step_send_raw(context, method, path, body, ctype):
    _send(context, method, _x(context, path), data=_x(context, body).encode("utf-8"), headers={"Content-Type": ctype})


@when('I send a {method:W} request to "{path:Q}" with these form parts')
def step_send_form(context, method, path):
    _send(context, method, _x(context, path), files=_form(context, context.table))


@when('I send a {method:W} request to "{path:Q}" with header "{name:Q}" set to "{value:Q}" and these form parts')
def step_send_form_header(context, method, path, name, value):
    _send(context, method, _x(context, path), files=_form(context, context.table),
          headers={name: None if value == "<none>" else _x(context, value)})


@when('I send a {method:W} request to "{path:Q}" with these form parts and the file')
def step_send_form_doc(context, method, path):
    _send(context, method, _x(context, path), files=_form(context, context.table, context.text))


@when('I send a {method:W} request to "{path:Q}" with a search for {n:d} distinct names')
def step_search_n(context, method, path, n):
    items = [{"name": "N%s%04d" % (context.names.uid, i)} for i in range(n)]
    _send(context, method, _x(context, path), json={"items": items})


@when('I send a {method:W} request to "{path:Q}" with {a:d} namespace URIs and {b:d} alias URIs')
def step_ext_search_n(context, method, path, a, b):
    body = {}
    if a:
        body["namespaceUris"] = ["http://ns.example.org/%s/%d" % (context.names.uid, i) for i in range(a)]
    if b:
        body["aliasUris"] = ["http://alias.example.org/%s/%d" % (context.names.uid, i) for i in range(b)]
    _send(context, method, _x(context, path), json=body)


@when('I create the vocabulary "{alias:Q}"')
def step_create(context, alias):
    name = context.names.real(alias)
    _send(context, "POST", "/vocabularies", files=[
        ("name", (None, name)), ("description", (None, "Description of %s" % name)),
        ("changelog", (None, "Initial version.")),
        ("vocabularyFile", ("%s.ttl" % name, client.default_turtle(name), "text/turtle"))])


@when('I create the vocabulary "{alias:Q}" with content')
def step_create_content(context, alias):
    name = context.names.real(alias)
    _send(context, "POST", "/vocabularies", files=[
        ("name", (None, name)), ("description", (None, "Description of %s" % name)),
        ("changelog", (None, "Initial version.")),
        ("vocabularyFile", ("%s.ttl" % name, _x(context, context.text).encode("utf-8"), "text/turtle"))])


@when('I create the vocabulary "{alias:Q}" with the fixture "{fixture:Q}"')
def step_create_fixture(context, alias, fixture):
    name = context.names.real(alias)
    content = _x(context, (FIXTURES / fixture).read_text(encoding="utf-8")).encode("utf-8")
    _send(context, "POST", "/vocabularies", files=[
        ("name", (None, name)), ("description", (None, "Description of %s" % name)),
        ("changelog", (None, "Initial version.")),
        ("vocabularyFile", ("%s.ttl" % name, content, "text/turtle"))])


@when('I create a new version of "{alias:Q}"')
def step_new_version(context, alias):
    name = context.names.real(alias)
    marker = client.random_token(6)
    _send(context, "POST", "/vocabularies/%s/versions" % name, files=[
        ("description", (None, "Description %s" % marker)), ("changelog", (None, "Change %s" % marker)),
        ("vocabularyFile", ("%s.ttl" % name, client.default_turtle(name, marker), "text/turtle"))])


@when('I create a new version of "{alias:Q}" with content')
def step_new_version_content(context, alias):
    name = context.names.real(alias)
    marker = client.random_token(6)
    _send(context, "POST", "/vocabularies/%s/versions" % name, files=[
        ("description", (None, "Description %s" % marker)), ("changelog", (None, "Change %s" % marker)),
        ("vocabularyFile", ("%s.ttl" % name, _x(context, context.text).encode("utf-8"), "text/turtle"))])


@when('I create a vocabulary with a fresh {n:d}-character name')
def step_create_short(context, n):
    # Short names cannot carry the scenario uid; retry on a name clash with
    # an earlier run (the store is never reset, E4).
    for _ in range(8):
        name = ("Q" + client.random_token(n))[:n]
        name = name[0] + name[1:]
        _send(context, "POST", "/vocabularies", files=[
            ("name", (None, name)), ("description", (None, "Short name")), ("changelog", (None, "Initial version.")),
            ("vocabularyFile", ("x.ttl", client.default_turtle(name), "text/turtle"))])
        if context.resp.status_code != 409:
            context.saved["shortName"] = name
            return


# ---- parallel requests (D43, D82) -------------------------------------------


@when('I create {n:d} new versions of "{alias:Q}" in parallel')
def step_parallel_versions(context, n, alias):
    name = context.names.real(alias)

    def one(i):
        marker = client.random_token(6) + str(i)
        return client.create_version(name, content=client.default_turtle(name, marker), base=context.base)

    with ThreadPoolExecutor(max_workers=n) as pool:
        context.parallel = list(pool.map(one, range(n)))


@when('I send {n:d} PATCH requests to "{path:Q}" with status "{status:Q}" in parallel')
def step_parallel_patch(context, n, path, status):
    path = _x(context, path)
    with ThreadPoolExecutor(max_workers=n) as pool:
        context.parallel = list(pool.map(lambda _: client.request("PATCH", path, "ga-admin", base=context.base, json={"status": status}), range(n)))


@when('I create the vocabulary "{alias:Q}" {n:d} times in parallel')
def step_parallel_create(context, alias, n):
    name = context.names.real(alias)
    with ThreadPoolExecutor(max_workers=n) as pool:
        context.parallel = list(pool.map(lambda _: client.create_vocabulary(name, base=context.base), range(n)))


# ---- one call per operation (access control outlines) ----------------------


def _operations(context):
    alpha = context.names.real("Alpha")
    ext = context.names.real("Ext")
    fresh = context.names.real("Fresh")
    uid = context.names.uid
    ttl = client.default_turtle(fresh)
    return {
        "getVocabularies": ("GET", "/vocabularies", {}),
        "createVocabulary": ("POST", "/vocabularies", {"files": [
            ("name", (None, fresh)), ("description", (None, "d")), ("changelog", (None, "Initial version.")),
            ("vocabularyFile", ("f.ttl", ttl, "text/turtle"))]}),
        "searchVocabularies": ("POST", "/vocabularies/searches", {"json": {"items": [{"name": alpha}]}}),
        "getVocabulary": ("GET", "/vocabularies/%s" % alpha, {}),
        "updateVocabularyStatus": ("PATCH", "/vocabularies/%s" % alpha, {"json": {"status": "DEPRECATED"}}),
        "getVocabularyVersions": ("GET", "/vocabularies/%s/versions" % alpha, {}),
        "createVocabularyVersion": ("POST", "/vocabularies/%s/versions" % alpha, {"files": [
            ("description", (None, "d2")), ("changelog", (None, "c2")),
            ("vocabularyFile", ("f.ttl", client.default_turtle(alpha, "v2"), "text/turtle"))]}),
        "getVocabularyVersion": ("GET", "/vocabularies/%s/versions/1" % alpha, {}),
        "updateVocabularyVersionStatus": ("PATCH", "/vocabularies/%s/versions/1" % alpha, {"json": {"status": "DEPRECATED"}}),
        "downloadVocabulary": ("GET", "/content/%s" % alpha, {}),
        "downloadVocabularyVersion": ("GET", "/content/%s/versions/1" % alpha, {}),
        "getExternalVocabularies": ("GET", "/external-vocabularies", {}),
        "createExternalVocabulary": ("POST", "/external-vocabularies", {"files": [
            ("name", (None, fresh)), ("namespaceUri", (None, "http://fresh.example.org/%s/terms" % uid)),
            ("vocabularyFile", ("f.ttl", b"@prefix t: <http://fresh.example.org/x/> .\nt:a t:b t:c .\n", "text/turtle"))]}),
        "searchExternalVocabularies": ("POST", "/external-vocabulary/searches", {"json": {"namespaceUri": "http://ext.example.org/%s/terms" % uid}}),
        "downloadExternalVocabularyContent": ("POST", "/external-vocabularies/content", {"json": {"namespaceUri": "http://ext.example.org/%s/terms" % uid}}),
        "getExternalVocabularyContentByName": ("GET", "/external-vocabularies/%s/content" % ext, {}),
        "getExternalVocabulary": ("GET", "/external-vocabularies/%s" % ext, {}),
        "deleteExternalVocabulary": ("DELETE", "/external-vocabularies/%s" % ext, {}),
    }


@when('I call the operation "{operation:Q}"')
def step_call_operation(context, operation):
    method, path, kwargs = _operations(context)[operation]
    _send(context, method, path, **kwargs)


@when('I call the operation "{operation:Q}" with the query parameter "{param:Q}"')
def step_call_operation_param(context, operation, param):
    method, path, kwargs = _operations(context)[operation]
    _send(context, method, path + "?" + _x(context, param), **kwargs)


@when('I call the operation "{operation:Q}" with the header "{name:Q}" set to "{value:Q}"')
def step_call_operation_header(context, operation, name, value):
    method, path, kwargs = _operations(context)[operation]
    _send(context, method, path, headers={name: value}, **kwargs)


# --------------------------------------------------------------------------
# status, problem bodies
# --------------------------------------------------------------------------


@then("the response status is {code:d}")
def step_status(context, code):
    if context.resp.status_code != code:
        _fail(context, "expected status %d, got %d" % (code, context.resp.status_code))


@then('the response status is one of "{codes:Q}"')
def step_status_one_of(context, codes):
    allowed = [int(c) for c in codes.split(",")]
    if context.resp.status_code not in allowed:
        _fail(context, "expected one of %s, got %d" % (allowed, context.resp.status_code))


@then("the request is authorised and processed")
def step_authorised(context):
    code = context.resp.status_code
    if code not in (200, 201, 204):
        _fail(context, "expected a 2xx outcome, got %d" % code)


@then("the response is a problem with status {code:d}")
def step_problem(context, code):
    step_status(context, code)
    if _media_type(context.resp) != "application/problem+json":
        _fail(context, "expected application/problem+json, got %s" % context.resp.headers.get("Content-Type"))
    body = _json(context.resp)
    if "status" in body and body.get("status") != code:
        _fail(context, "problem status field is %r, expected %d" % (body.get("status"), code))


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def step_problem_type(context, code, ptype):
    step_problem(context, code)
    step_problem_field(context, "type", ptype)


@then('the problem {field:W} is "{value:Q}"')
def step_problem_field(context, field, value):
    body = _json(context.resp)
    value = _x(context, value)
    if body.get(field) != value:
        _fail(context, "problem %s is %r, expected %r" % (field, body.get(field), value))


@then('the problem detail starts with "{prefix:Q}"')
def step_problem_detail_prefix(context, prefix):
    detail = _json(context.resp).get("detail") or ""
    if not detail.startswith(_x(context, prefix)):
        _fail(context, "problem detail %r does not start with %r" % (detail, prefix))


@then("the problem has a detail")
def step_problem_has_detail(context):
    if not (_json(context.resp).get("detail") or "").strip():
        _fail(context, "problem has no detail")


@then("the problem has {n:d} issues")
@then("the problem has {n:d} issue")
def step_issue_count(context, n):
    issues = _issues(context)
    if len(issues) != n:
        _fail(context, "expected %d issues, got %d" % (n, len(issues)))


@then("the problem has no issues")
def step_no_issues(context):
    if _issues(context):
        _fail(context, "expected no issues")


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_in_named(context, where, name):
    name = _x(context, name)
    if not any(i.get("in") == where and i.get("name") == name for i in _issues(context)):
        _fail(context, "no issue in %s named %r" % (where, name))


@then('the problem has an issue in "{where:Q}" named one of "{names:Q}"')
def step_issue_in_named_one_of(context, where, names):
    allowed = [_x(context, n) for n in names.split("|")]
    if not any(i.get("in") == where and i.get("name") in allowed for i in _issues(context)):
        _fail(context, "no issue in %s named one of %r" % (where, allowed))


@then('every issue is in "{where:Q}" named "{name:Q}"')
def step_every_issue(context, where, name):
    issues = _issues(context)
    if not issues:
        _fail(context, "no issues")
    for i in issues:
        if i.get("in") != where or i.get("name") != _x(context, name):
            _fail(context, "issue %r is not in %s named %s" % (i, where, name))


@then('every issue has type "{itype:Q}"')
def step_every_issue_type(context, itype):
    issues = _issues(context)
    if not issues:
        _fail(context, "no issues")
    for i in issues:
        if i.get("type") != itype:
            _fail(context, "issue %r does not have type %s" % (i, itype))


def _find_issue(context, detail=None, detail_prefix=None, loc=None, loc_prefix=None, loc_contains=None, loc_regex=None):
    for i in _issues(context):
        d, l = i.get("detail") or "", i.get("location") or ""
        if detail is not None and d != detail:
            continue
        if detail_prefix is not None and not d.startswith(detail_prefix):
            continue
        if loc is not None and l != loc:
            continue
        if loc_prefix is not None and not l.startswith(loc_prefix):
            continue
        if loc_contains is not None and loc_contains not in l:
            continue
        if loc_regex is not None and not re.fullmatch(loc_regex, l):
            continue
        return i
    return None


@then('the problem has an issue with detail "{detail:Q}" and location "{loc:Q}"')
def step_issue_detail_loc(context, detail, loc):
    if not _find_issue(context, detail=_x(context, detail), loc=_x(context, loc)):
        _fail(context, "no issue with detail %r and location %r" % (_x(context, detail), _x(context, loc)))


@then('the problem has an issue with detail "{detail:Q}" and location starting with "{loc:Q}"')
def step_issue_detail_locprefix(context, detail, loc):
    if not _find_issue(context, detail=_x(context, detail), loc_prefix=_x(context, loc)):
        _fail(context, "no issue with detail %r and location starting %r" % (_x(context, detail), _x(context, loc)))


@then('the problem has an issue with detail "{detail:Q}", location starting with "{loc:Q}" and containing "{part:Q}"')
def step_issue_detail_locprefix_contains(context, detail, loc, part):
    if not _find_issue(context, detail=_x(context, detail), loc_prefix=_x(context, loc), loc_contains=_x(context, part)):
        _fail(context, "no issue with detail %r and location starting %r containing %r" % (detail, loc, part))


@then('the problem has an issue with detail starting with "{detail:Q}" and location matching "{regex:Q}"')
def step_issue_detailprefix_locregex(context, detail, regex):
    if not _find_issue(context, detail_prefix=_x(context, detail), loc_regex=_x(context, regex)):
        _fail(context, "no issue with detail starting %r and location matching %r" % (detail, regex))


@then('the problem has an issue with detail starting with "{detail:Q}" and location starting with "{loc:Q}"')
def step_issue_detailprefix_locprefix(context, detail, loc):
    if not _find_issue(context, detail_prefix=_x(context, detail), loc_prefix=_x(context, loc)):
        _fail(context, "no issue with detail starting %r and location starting %r" % (detail, loc))


@then('the problem has an issue with detail "{detail:Q}"')
def step_issue_detail(context, detail):
    if not _find_issue(context, detail=_x(context, detail)):
        _fail(context, "no issue with detail %r" % _x(context, detail))


@then('the problem has an issue with detail starting with "{detail:Q}"')
def step_issue_detail_prefix(context, detail):
    if not _find_issue(context, detail_prefix=_x(context, detail)):
        _fail(context, "no issue with detail starting %r" % _x(context, detail))


# --------------------------------------------------------------------------
# JSON fields
# --------------------------------------------------------------------------


@then('the response field "{path:Q}" is "{value:Q}"')
def step_field_is(context, path, value):
    body = _json(context.resp)
    try:
        actual = _get(body, path)
    except KeyError:
        _fail(context, "field %s is absent" % path)
    if _scalar(actual) != _x(context, value):
        _fail(context, "field %s is %r, expected %r" % (path, actual, _x(context, value)))


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    try:
        value = _get(_json(context.resp), path)
    except KeyError:
        return
    if value is not None:
        _fail(context, "field %s should be absent but is %r" % (path, value))


@then('the response field "{path:Q}" is present')
def step_field_present(context, path):
    try:
        value = _get(_json(context.resp), path)
    except KeyError:
        _fail(context, "field %s is absent" % path)
    if value is None or value == "":
        _fail(context, "field %s is empty" % path)


@then('the response field "{path:Q}" is an empty list')
def step_field_empty_list(context, path):
    try:
        value = _get(_json(context.resp), path)
    except KeyError:
        _fail(context, "field %s is absent" % path)
    if value != []:
        _fail(context, "field %s is %r, expected []" % (path, value))


@then('the response field "{path:Q}" is empty or absent')
def step_field_empty_or_absent(context, path):
    try:
        value = _get(_json(context.resp), path)
    except KeyError:
        return
    if value not in (None, [], ""):
        _fail(context, "field %s is %r, expected empty or absent" % (path, value))


@then('the response field "{path:Q}" has {n:d} items')
def step_field_count(context, path, n):
    value = _get(_json(context.resp), path)
    if not isinstance(value, list) or len(value) != n:
        _fail(context, "field %s has %s items, expected %d" % (path, len(value) if isinstance(value, list) else "no", n))


@then('the response field "{path:Q}" matches "{regex:Q}"')
def step_field_matches(context, path, regex):
    try:
        value = _scalar(_get(_json(context.resp), path))
    except KeyError:
        _fail(context, "field %s is absent" % path)
    if not re.fullmatch(_x(context, regex), value):
        _fail(context, "field %s = %r does not match %r" % (path, value, regex))


@then('the response field "{path:Q}" is a UTC time to the second')
def step_field_utc(context, path):
    step_field_matches(context, path, r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z")


@then('the response field "{path:Q}" is the list "{csv:Q}"')
def step_field_list(context, path, csv):
    expected = [v for v in _x(context, csv).split("|")] if csv else []
    actual = _get(_json(context.resp), path)
    if actual != expected:
        _fail(context, "field %s is %r, expected %r" % (path, actual, expected))


@step('I remember the response field "{path:Q}" as "{key:Q}"')
def step_remember(context, path, key):
    context.saved[key] = _get(_json(context.resp), path)


@step('I remember the date of the response field "{path:Q}" as "{key:Q}"')
def step_remember_date(context, path, key):
    context.saved[key] = str(_get(_json(context.resp), path))[:10]


@step('I remember field "{path:Q}" of {method:W} "{url:Q}" as "{key:Q}"')
def step_remember_from(context, path, method, url, key):
    resp = _admin(context, method, _x(context, url))
    context.saved[key] = _get(_json(resp), path)


@then('the response field "{path:Q}" equals the remembered "{key:Q}"')
def step_field_eq_saved(context, path, key):
    actual = _get(_json(context.resp), path)
    if actual != context.saved[key]:
        _fail(context, "field %s is %r, remembered %s is %r" % (path, actual, key, context.saved[key]))


@then('the response field "{path:Q}" is later than the remembered "{key:Q}"')
def step_field_later(context, path, key):
    actual = _get(_json(context.resp), path)
    if not actual > context.saved[key]:
        _fail(context, "field %s is %r, not later than %r" % (path, actual, context.saved[key]))


@then('the response field "{path:Q}" is not before the remembered "{key:Q}"')
def step_field_not_before(context, path, key):
    actual = _get(_json(context.resp), path)
    if actual < context.saved[key]:
        _fail(context, "field %s is %r, before %r" % (path, actual, context.saved[key]))


@then('the response items have the names "{csv:Q}"')
def step_items_names(context, csv):
    expected = _x(context, csv).split(",") if csv else []
    actual = [i.get("name") for i in _json(context.resp).get("items", [])]
    if actual != expected:
        _fail(context, "item names %r, expected %r" % (actual, expected))


@then('the response items are those of "{csv:Q}" whose "{field:Q}" falls on the day "{day:Q}"')
def step_items_on_day(context, csv, field, day):
    names = _x(context, csv).split(",")
    day = _x(context, day)
    expected = []
    for name in names:
        resp = _admin(context, "GET", "/vocabularies/%s" % name)
        if str(resp.json().get(field, ""))[:10] == day:
            expected.append(name)
    actual = [i.get("name") for i in _json(context.resp).get("items", [])]
    if sorted(actual) != sorted(expected) or not expected:
        _fail(context, "items %r, expected %r" % (actual, expected))


@then('the response items have the versions "{csv:Q}"')
def step_items_versions(context, csv):
    expected = [int(v) for v in csv.split(",")] if csv else []
    actual = [i.get("version") for i in _json(context.resp).get("items", [])]
    if actual != expected:
        _fail(context, "item versions %r, expected %r" % (actual, expected))


@then("the response has no items")
def step_no_items(context):
    if _json(context.resp).get("items") != []:
        _fail(context, "expected an empty items array")


@then('every item has the fields "{csv:Q}"')
def step_every_item_fields(context, csv):
    items = _json(context.resp).get("items", [])
    if not items:
        _fail(context, "no items")
    for item in items:
        for f in csv.split(","):
            if f not in item or item[f] in (None, ""):
                _fail(context, "item %r lacks field %s" % (item.get("name"), f))


_VOCAB_SORT_FIELD = {"name": "name", "status": "status", "creationDate": "creationDate", "lastUpdated": "lastUpdateDate"}


@then('the vocabulary items are sorted by "{field:Q}" "{direction:Q}"')
def step_vocab_sorted(context, field, direction):
    items = _json(context.resp).get("items", [])
    key = _VOCAB_SORT_FIELD[field]
    for a, b in zip(items, items[1:]):
        ka, kb = a[key], b[key]
        if ka == kb:
            if a["name"] > b["name"]:  # ties: name ascending, code point order (D23, D58)
                _fail(context, "tie on %s not broken by name ascending: %s, %s" % (key, a["name"], b["name"]))
        elif (direction == "ascending") != (ka < kb):
            _fail(context, "items not sorted by %s %s: %r then %r" % (key, direction, ka, kb))


@then('the version items are sorted by "{field:Q}" "{direction:Q}"')
def step_version_sorted(context, field, direction):
    items = _json(context.resp).get("items", [])
    for a, b in zip(items, items[1:]):
        ka, kb = a[field], b[field]
        if ka == kb:
            if a["version"] < b["version"]:  # ties: version descending (D23)
                _fail(context, "tie on %s not broken by version descending" % field)
        elif (direction == "ascending") != (ka < kb):
            _fail(context, "items not sorted by %s %s: %r then %r" % (field, direction, ka, kb))


# ---- paging links ----------------------------------------------------------


def _link(context, rel):
    value = _json(context.resp).get(rel)
    return value


@then('the link "{rel:Q}" is absent')
def step_link_absent(context, rel):
    if _link(context, rel) not in (None, ""):
        _fail(context, "link %s should be absent, is %r" % (rel, _link(context, rel)))


@then('the link "{rel:Q}" has path "{path:Q}" and query "{query:Q}"')
def step_link_query(context, rel, path, query):
    """The link keeps every request parameter (D78); page and pageSize may be
    left out when they have their default value (1 and 20, D22)."""
    link = _link(context, rel)
    if not link:
        _fail(context, "link %s is absent" % rel)
    parts = urlsplit(link)
    expected_path = _x(context, path)
    if not (parts.path == expected_path or parts.path.endswith("/v1" + expected_path)):
        _fail(context, "link %s path %r, expected %r" % (rel, parts.path, expected_path))
    actual = {k: v for k, v in parse_qs(parts.query, keep_blank_values=True).items()}
    expected = {k: v for k, v in parse_qs(_x(context, query), keep_blank_values=True).items()}
    for key, default in (("page", ["1"]), ("pageSize", ["20"])):
        actual.setdefault(key, default)
        expected.setdefault(key, default)
    if actual != expected:
        _fail(context, "link %s query %r, expected %r" % (rel, actual, expected))


# --------------------------------------------------------------------------
# headers and raw bodies
# --------------------------------------------------------------------------


@then('the response header "{name:Q}" is "{value:Q}"')
def step_header_is(context, name, value):
    actual = context.resp.headers.get(name)
    if actual != _x(context, value):
        _fail(context, "header %s is %r, expected %r" % (name, actual, _x(context, value)))


@then('the response header "{name:Q}" is present')
def step_header_present(context, name):
    if not context.resp.headers.get(name):
        _fail(context, "header %s is absent" % name)


@then('the response header "{name:Q}" matches "{regex:Q}"')
def step_header_matches(context, name, regex):
    actual = context.resp.headers.get(name) or ""
    if not re.fullmatch(_x(context, regex), actual):
        _fail(context, "header %s = %r does not match %r" % (name, actual, regex))


@then('the response media type is "{media:Q}"')
def step_media(context, media):
    if _media_type(context.resp) != media:
        _fail(context, "media type %r, expected %r" % (context.resp.headers.get("Content-Type"), media))


@then('the response Content-Type is exactly "{value:Q}"')
def step_ct_exact(context, value):
    if context.resp.headers.get("Content-Type") != value:
        _fail(context, "Content-Type %r, expected exactly %r" % (context.resp.headers.get("Content-Type"), value))


@then('the response has an attachment named "{filename:Q}"')
def step_attachment(context, filename):
    cd = context.resp.headers.get("Content-Disposition") or ""
    filename = _x(context, filename)
    if not cd.lower().startswith("attachment") or not re.search(r'filename="?%s"?(;|$)' % re.escape(filename), cd):
        _fail(context, "Content-Disposition %r does not name attachment %r" % (cd, filename))


@then('the response body is the content uploaded for version {n:d} of "{alias:Q}"')
def step_body_uploaded(context, n, alias):
    expected = context.uploaded[(context.names.real(alias), n)]
    if context.resp.content != expected:
        _fail(context, "body differs from the uploaded bytes (%d vs %d bytes)" % (len(context.resp.content), len(expected)))


@then('the response body is the content uploaded for the external vocabulary "{alias:Q}"')
def step_body_uploaded_ext(context, alias):
    expected = context.uploaded[("ext", context.names.real(alias))]
    if context.resp.content != expected:
        _fail(context, "body differs from the uploaded bytes")


@then('the Location header is the response field "href"')
def step_location_href(context):
    loc = context.resp.headers.get("Location")
    href = _json(context.resp).get("href")
    if not loc or not (loc == href or loc.endswith("/v1" + href)):
        _fail(context, "Location %r does not match href %r" % (loc, href))


@then("the request took at most {seconds:d} seconds")
def step_elapsed(context, seconds):
    if context.elapsed is None or context.elapsed > seconds:
        _fail(context, "request took %.1f s, limit %d s" % (context.elapsed or -1, seconds))


# ---- validation warnings (D10, D11, D12, D47) --------------------------------


def _warnings(context):
    body = _json(context.resp)
    if "validationWarnings" not in body or not isinstance(body["validationWarnings"], list):
        _fail(context, "validationWarnings is not present as an array")
    return body["validationWarnings"]


@then("the response has no validation warnings")
def step_no_warnings(context):
    if _warnings(context) != []:
        _fail(context, "expected validationWarnings []")


@then("the response has {n:d} validation warnings")
@then("the response has {n:d} validation warning")
def step_n_warnings(context, n):
    if len(_warnings(context)) != n:
        _fail(context, "expected %d warnings" % n)


@then('the response has a validation warning "{code:Q}" with location "{loc:Q}"')
def step_warning_loc(context, code, loc):
    loc = _x(context, loc)
    if not any(w.get("code") == code and w.get("location") == loc and w.get("description") for w in _warnings(context)):
        _fail(context, "no warning %s at %r" % (code, loc))


@then('the response has a validation warning "{code:Q}" with description "{desc:Q}" and location "{loc:Q}"')
def step_warning_full(context, code, desc, loc):
    loc, desc = _x(context, loc), _x(context, desc)
    if not any(w.get("code") == code and w.get("location") == loc and w.get("description") == desc for w in _warnings(context)):
        _fail(context, "no warning %s with description %r at %r" % (code, desc, loc))


@then('every validation warning code is one of "{csv:Q}"')
def step_warning_codes(context, csv):
    allowed = csv.split(",")
    for w in _warnings(context):
        if w.get("code") not in allowed:
            _fail(context, "warning code %r not in %r" % (w.get("code"), allowed))


# --------------------------------------------------------------------------
# stored state, read back as ga-admin through reads that cannot fail on input
# --------------------------------------------------------------------------


@step('vocabulary "{alias:Q}" has vocabulary status "{status:Q}"')
def step_vocab_status(context, alias, status):
    resp = _admin(context, "GET", "/vocabularies/%s" % context.names.real(alias))
    if resp.status_code != 200 or resp.json().get("status") != status:
        raise AssertionError("vocabulary %s: %s %s, expected status %s" % (alias, resp.status_code, resp.text[:300], status))


@step('version {n:d} of "{alias:Q}" has version status "{status:Q}"')
def step_version_status(context, n, alias, status):
    resp = _admin(context, "GET", "/vocabularies/%s/versions/%d" % (context.names.real(alias), n))
    if resp.status_code != 200 or resp.json().get("status") != status:
        raise AssertionError("version %d of %s: %s %s, expected %s" % (n, alias, resp.status_code, resp.text[:300], status))


@step('versions {a:d} to {b:d} of "{alias:Q}" have version status "{status:Q}"')
def step_versions_status(context, a, b, alias, status):
    for n in range(a, b + 1):
        step_version_status(context, n, alias, status)


@step('vocabulary "{alias:Q}" has {n:d} versions')
@step('vocabulary "{alias:Q}" has {n:d} version')
def step_vocab_version_count(context, alias, n):
    resp = _admin(context, "GET", "/vocabularies/%s/versions?pageSize=100" % context.names.real(alias))
    if resp.status_code != 200 or resp.json().get("total") != n:
        raise AssertionError("vocabulary %s: %s %s, expected total %d" % (alias, resp.status_code, resp.text[:300], n))


def _count_by_name(context, text):
    resp = _admin(context, "GET", "/vocabularies?name=%s&pageSize=100" % _x(context, text))
    if resp.status_code != 200:
        raise AssertionError("list with name filter failed: %s %s" % (resp.status_code, resp.text[:300]))
    return resp.json()["total"]


@step('no vocabulary whose name contains "{text:Q}" is stored')
def step_none_stored(context, text):
    total = _count_by_name(context, text)
    if total != 0:
        raise AssertionError("expected no vocabulary containing %s, found %d" % (_x(context, text), total))


@step('{n:d} vocabularies whose name contains "{text:Q}" are stored')
@step('{n:d} vocabulary whose name contains "{text:Q}" is stored')
def step_n_stored(context, n, text):
    total = _count_by_name(context, text)
    if total != n:
        raise AssertionError("expected %d vocabularies containing %s, found %d" % (n, _x(context, text), total))


@step('no external vocabulary is registered for namespace "{ns:Q}"')
def step_no_ext(context, ns):
    resp = _admin(context, "POST", "/external-vocabulary/searches", json={"namespaceUri": _x(context, ns)})
    if resp.status_code != 200 or resp.json().get("items") != []:
        raise AssertionError("expected no external vocabulary for %s: %s %s" % (ns, resp.status_code, resp.text[:300]))


@step('an external vocabulary is registered for namespace "{ns:Q}"')
def step_ext_found(context, ns):
    resp = _admin(context, "POST", "/external-vocabulary/searches", json={"namespaceUri": _x(context, ns)})
    if resp.status_code != 200 or len(resp.json().get("items", [])) != 1:
        raise AssertionError("expected one external vocabulary for %s: %s %s" % (ns, resp.status_code, resp.text[:300]))


@step('the external vocabulary list contains "{alias:Q}" in name order')
def step_ext_list_contains(context, alias):
    name = context.names.real(alias)
    page = 1
    previous_last = None
    while True:
        resp = _admin(context, "GET", "/external-vocabularies?page=%d&pageSize=100" % page)
        if resp.status_code != 200:
            raise AssertionError("external list failed: %s" % resp.status_code)
        body = resp.json()
        names = [i["name"] for i in body["items"]]
        if names != sorted(names):
            raise AssertionError("external list page %d is not in name order" % page)
        if names and previous_last is not None and previous_last > names[0]:
            raise AssertionError("page %d starts with %s, before the last name %s of page %d" % (page, names[0], previous_last, page - 1))
        previous_last = names[-1] if names else previous_last
        if name in names:
            return
        if page >= body.get("totalPages", 0):
            raise AssertionError("external vocabulary %s not in the list" % name)
        page += 1


# ---- parallel outcomes -----------------------------------------------------


@then("all parallel responses have status {code:d}")
def step_parallel_all(context, code):
    codes = [r.status_code for r in context.parallel]
    if any(c != code for c in codes):
        raise AssertionError("parallel statuses %r, expected all %d: %s" % (codes, code, [r.text[:200] for r in context.parallel]))


@then('the parallel responses have the statuses "{csv:Q}"')
def step_parallel_statuses(context, csv):
    codes = sorted(r.status_code for r in context.parallel)
    if codes != sorted(int(c) for c in csv.split(",")):
        raise AssertionError("parallel statuses %r, expected %s" % (codes, csv))


@then("the parallel responses created versions {a:d} to {b:d} once each with distinct Location headers")
def step_parallel_versions_once(context, a, b):
    versions = sorted(r.json()["version"] for r in context.parallel)
    if versions != list(range(a, b + 1)):
        raise AssertionError("versions %r, expected %d..%d" % (versions, a, b))
    locations = [r.headers.get("Location") for r in context.parallel]
    if None in locations or len(set(locations)) != len(locations):
        raise AssertionError("Location headers not distinct: %r" % locations)
