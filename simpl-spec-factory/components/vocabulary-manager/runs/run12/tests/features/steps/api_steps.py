"""Shared step library for the Vocabulary Manager API suite.

Placeholders in step text, tables, doc strings and fixtures:
  {{Alias}}         unique vocabulary name of this scenario: Alias + run id
  {{upper:Alias}}   the same name in upper case (case tests, D37, D54)
  {{run}}           run id of the scenario (letters and digits)
  {{contentBase}}   the content base URL, from CONTENT_BASE_URL or a contentHref
  {{adminEmail}}    creator value of ga-admin (D27)
  {{today}}, {{tomorrow}}  UTC dates
  {{Long64}}, {{Long65}}   names of 64 and 65 characters that contain the run id
  {{key}}           any value remembered with "I remember ..."
"""
import concurrent.futures
import datetime
import json
import re
import time

from behave import given, register_type, then, when
import parse


@parse.with_pattern(r'[^"]*')
def _parse_quoted(text):
    return text


register_type(Q=_parse_quoted)

import vmclient

PLACEHOLDER = re.compile(r"\{\{([A-Za-z][A-Za-z0-9_]*(?::[A-Za-z][A-Za-z0-9_]*)?)\}\}")
_content_base_cache = {}


# ---------------------------------------------------------------- helpers

def name_of(context, alias):
    if alias not in context.names:
        context.names[alias] = alias + context.run
    return context.names[alias]


def content_base(context):
    import os
    if os.environ.get("CONTENT_BASE_URL"):
        return os.environ["CONTENT_BASE_URL"].rstrip("/")
    if "value" in _content_base_cache:
        return _content_base_cache["value"]
    probe = "Probe" + vmclient.run_id(12)  # never matches a scenario run-id filter
    resp = create_vocabulary(context, probe, record=False)
    learn_content_base(resp)
    if "value" not in _content_base_cache:
        raise AssertionError("Could not learn contentBase from POST /vocabularies: "
                             f"{resp.status_code} {resp.text[:300]}")
    return _content_base_cache["value"]


def learn_content_base(resp):
    try:
        body = resp.json()
        href, name = body["contentHref"], body["name"]
    except Exception:
        return
    marker = "/content/" + name
    if marker in href:
        _content_base_cache.setdefault("value", href[: href.index(marker)])


def sub(context, text):
    if text is None:
        return None

    def repl(match):
        key = match.group(1)
        if ":" in key:
            mod, alias = key.split(":", 1)
            if mod in ("date", "nextdate"):
                day = datetime.date.fromisoformat(str(context.vars[alias])[:10])
                if mod == "nextdate":
                    day += datetime.timedelta(days=1)
                return day.isoformat()
            value = name_of(context, alias)
            return value.upper() if mod == "upper" else value
        if key in context.vars:
            value = context.vars[key]
            return value if isinstance(value, str) else json.dumps(value)
        if key == "run":
            return context.run
        if key == "contentBase":
            return content_base(context)
        if key == "adminEmail":
            return vmclient.EMAILS["ga-admin"]
        if key == "today":
            return datetime.datetime.now(datetime.timezone.utc).date().isoformat()
        if key == "tomorrow":
            return (datetime.datetime.now(datetime.timezone.utc).date()
                    + datetime.timedelta(days=1)).isoformat()
        if key == "Long64":
            return ("L" + context.run + "x" * 64)[:64]
        if key == "Long65":
            return ("L" + context.run + "x" * 65)[:65]
        if key[0].isupper():
            return name_of(context, key)
        raise AssertionError(f"Unknown placeholder {{{{{key}}}}}")

    return PLACEHOLDER.sub(repl, text)


def fixture_bytes(context, name):
    return sub(context, vmclient.read_fixture(name).decode("utf-8")).encode("utf-8")


def admin_token():
    return vmclient.fetch_token("ga-admin")


def current_headers(context, extra=None):
    headers = dict(context.extra_headers)
    headers.update(extra or {})
    context.extra_headers = {}
    return headers


def do_request(context, method, path, **kwargs):
    headers = current_headers(context, kwargs.pop("headers", None))
    timeout = kwargs.pop("timeout", None)
    context.response = vmclient.send(method, sub(context, path), token=context.token,
                                     headers=headers, base=context.base_url,
                                     timeout=timeout, **kwargs)
    context.last_body = None
    return context.response


def body(context):
    if getattr(context, "last_body", None) is None:
        try:
            context.last_body = context.response.json()
        except ValueError:
            raise AssertionError(
                f"Response is not JSON: {context.response.status_code} "
                f"{context.response.headers.get('Content-Type')} {context.response.text[:300]}")
    return context.last_body


def get_path(obj, path):
    cur = obj
    if path in ("", "$"):
        return cur
    for part in path.split("."):
        if isinstance(cur, list):
            cur = cur[int(part)]
        elif isinstance(cur, dict):
            if part not in cur:
                raise KeyError(path)
            cur = cur[part]
        else:
            raise KeyError(path)
    return cur


def has_path(obj, path):
    try:
        get_path(obj, path)
        return True
    except (KeyError, IndexError, ValueError):
        return False


def show(context):
    r = context.response
    return f"{r.request.method} {r.url} -> {r.status_code} {r.headers.get('Content-Type')} {r.text[:800]}"


def multipart_value(context, field, value):
    """Turn a table cell into a multipart part. Returns (is_file, payload)."""
    value = value if value is not None else ""
    if value.startswith("file:"):
        return True, fixture_bytes(context, value[5:])
    if value.startswith("generated:"):
        return True, vmclient.turtle_of_size(int(value[10:]), context.run)
    if value == "empty-file":
        return True, b""
    if value.startswith("turtle:"):
        return True, sub(context, value[7:]).encode()
    if value.startswith("repeat:"):
        _, count, char = value.split(":", 2)
        return False, char * int(count)
    if value.startswith("padded-repeat:"):
        _, count, char = value.split(":", 2)
        return False, "  " + char * int(count) + "  "
    if value == "blank":
        return False, "   "
    return False, sub(context, value)


def build_multipart(context, rows):
    parts = []
    files = {}
    for field, value in rows:
        is_file, payload = multipart_value(context, field, value)
        if is_file:
            files[field] = payload
            parts.append((field, (field + ".ttl", payload, "text/turtle")))
        else:
            parts.append((field, (None, payload)))
    return parts, files


def remember_upload(context, resp, payload):
    if resp is not None and resp.status_code == 201:
        try:
            b = resp.json()
            context.uploaded[(b["name"], int(b["version"]))] = payload
        except Exception:
            pass


def create_vocabulary(context, name, content=None, description=None,
                      changelog="Initial version.", token=None, record=True):
    if content is None:
        content = fixture_bytes(context, "valid.ttl")
    description = description if description is not None else f"Description of {name}"
    parts = [
        ("name", (None, name)),
        ("description", (None, description)),
        ("changelog", (None, changelog)),
        ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle")),
    ]
    resp = vmclient.send("POST", "/vocabularies", token=token or admin_token(),
                         files=parts, base=vmclient.API_URL, timeout=vmclient.UPLOAD_TIMEOUT)
    learn_content_base(resp)
    if record:
        remember_upload(context, resp, content)
    return resp


def create_version(context, name, content=None, description=None, changelog=None,
                   token=None, headers=None):
    if content is None:
        content = fixture_bytes(context, "valid.ttl") + (
            f"\n# variant {vmclient.run_id(8)}\n").encode()
    if description is None:
        description = f"New version {vmclient.run_id(6)} of {name}"
    if changelog is None:
        changelog = "Changed for a test."
    parts = [
        ("description", (None, description)),
        ("changelog", (None, changelog)),
        ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle")),
    ]
    resp = vmclient.send("POST", f"/vocabularies/{name}/versions", token=token or admin_token(),
                         files=parts, headers=headers, base=vmclient.API_URL,
                         timeout=vmclient.UPLOAD_TIMEOUT)
    remember_upload(context, resp, content)
    return resp


def expect_created(resp, what):
    if resp.status_code != 201:
        raise AssertionError(f"Setup failed ({what}): {resp.status_code} {resp.text[:500]}")
    return resp


def admin_get(path):
    resp = vmclient.send("GET", path, token=admin_token(), base=vmclient.API_URL)
    return resp


def set_status(path, status):
    resp = vmclient.send("PATCH", path, token=admin_token(), json={"status": status},
                         base=vmclient.API_URL)
    if resp.status_code != 200:
        raise AssertionError(f"Setup PATCH {path} {status} failed: {resp.status_code} {resp.text[:300]}")


def split_list(context, text):
    text = sub(context, text).strip()
    if not text:
        return []
    return [part.strip() for part in text.split(",")]


# ---------------------------------------------------------------- identity

@given('I am authenticated as "{user:Q}"')
def step_auth(context, user):
    context.user = user
    context.token = vmclient.fetch_token(user)


@given("I am not authenticated")
def step_no_auth(context):
    context.user = None
    context.token = None


@given('I use a bearer token that is "{kind:Q}"')
def step_bad_token(context, kind):
    context.user = None
    context.token = vmclient.bad_token(kind)


@given('the request header "{header:Q}" is "{value:Q}"')
def step_header(context, header, value):
    context.extra_headers[header] = sub(context, value)


# ---------------------------------------------------------------- setup data

@given('a vocabulary "{name:Q}" exists')
def step_vocab_exists(context, name):
    expect_created(create_vocabulary(context, sub(context, name)), "create " + name)


@given('a vocabulary "{name:Q}" exists with file "{fixture:Q}"')
def step_vocab_exists_file(context, name, fixture):
    expect_created(create_vocabulary(context, sub(context, name),
                                     content=fixture_bytes(context, fixture)), "create " + name)


@given('a vocabulary "{name:Q}" exists with description "{description:Q}"')
def step_vocab_exists_desc(context, name, description):
    expect_created(create_vocabulary(context, sub(context, name),
                                     description=sub(context, description)), "create " + name)


@given('a vocabulary "{name:Q}" exists with content')
def step_vocab_exists_content(context, name):
    expect_created(create_vocabulary(context, sub(context, name),
                                     content=sub(context, context.text).encode()), "create " + name)


@given("these vocabularies exist")
def step_vocabs_exist(context):
    for row in context.table:
        name = sub(context, row["name"])
        expect_created(create_vocabulary(context, name), "create " + name)
        if "status" in row.headings and row["status"] and row["status"] != "ACTIVE":
            set_status(f"/vocabularies/{name}", row["status"])


@given("these vocabularies are created one second apart")
def step_vocabs_apart(context):
    for index, row in enumerate(context.table):
        if index:
            time.sleep(1.2)
        name = sub(context, row["name"])
        expect_created(create_vocabulary(context, name), "create " + name)


@given('vocabulary "{name:Q}" has {count:d} more versions')
def step_more_versions(context, name, count):
    for _ in range(count):
        expect_created(create_version(context, sub(context, name)), "new version of " + name)


@given('vocabulary "{name:Q}" has {count:d} more versions created one second apart')
def step_more_versions_apart(context, name, count):
    for _ in range(count):
        time.sleep(1.2)
        expect_created(create_version(context, sub(context, name)), "new version of " + name)


@given('vocabulary "{name:Q}" has a new version with description "{description:Q}"')
def step_version_desc(context, name, description):
    expect_created(create_version(context, sub(context, name),
                                  description=sub(context, description)), "new version")


@given('vocabulary "{name:Q}" has a new version with content')
def step_version_content(context, name):
    expect_created(create_version(context, sub(context, name),
                                  content=sub(context, context.text).encode()), "new version")


@given('vocabulary "{name:Q}" has status "{status:Q}"')
def step_vocab_status(context, name, status):
    set_status(f"/vocabularies/{sub(context, name)}", status)


@given('version {version:d} of vocabulary "{name:Q}" has status "{status:Q}"')
def step_version_status(context, version, name, status):
    set_status(f"/vocabularies/{sub(context, name)}/versions/{version}", status)


@given("one second passes")
def step_sleep(context):
    time.sleep(1.2)


@given('an external vocabulary "{name:Q}" is registered with namespace "{namespace:Q}"')
def step_external(context, name, namespace):
    register_external(context, name, namespace, [])


@given('an external vocabulary "{name:Q}" is registered with namespace "{namespace:Q}" and aliases "{aliases:Q}"')
def step_external_alias(context, name, namespace, aliases):
    register_external(context, name, namespace, split_list(context, aliases))


def register_external(context, name, namespace, aliases):
    parts = [
        ("name", (None, sub(context, name))),
        ("namespaceUri", (None, sub(context, namespace))),
        ("vocabularyFile", ("external.ttl", fixture_bytes(context, "external-dcterms.ttl"),
                            "text/turtle")),
    ]
    for alias in aliases:
        parts.append(("aliasUris", (None, alias)))
    resp = vmclient.send("POST", "/external-vocabularies", token=admin_token(), files=parts,
                         base=vmclient.API_URL)
    expect_created(resp, "register external " + name)


@given('external vocabulary "{name:Q}" is deleted')
def step_external_deleted(context, name):
    resp = vmclient.send("DELETE", f"/external-vocabularies/{sub(context, name)}",
                         token=admin_token(), base=vmclient.API_URL)
    if resp.status_code != 204:
        raise AssertionError(f"Setup delete failed: {resp.status_code} {resp.text[:300]}")


# ---------------------------------------------------------------- requests

@when('I send "{method:Q}" to "{path:Q}"')
def step_send(context, method, path):
    do_request(context, method, path)


@when('I send "{method:Q}" to "{path:Q}" with JSON body')
def step_send_json(context, method, path):
    do_request(context, method, path, data=sub(context, context.text).encode("utf-8"),
               headers={"Content-Type": "application/json"})


@when('I send "{method:Q}" to "{path:Q}" with a JSON body of {size:d} bytes')
def step_send_json_size(context, method, path, size):
    prefix = b'{"status":"DEPRECATED","pad":"'
    suffix = b'"}'
    payload = prefix + b"x" * (size - len(prefix) - len(suffix)) + suffix
    do_request(context, method, path, data=payload, headers={"Content-Type": "application/json"})


@when('I send "{method:Q}" to "{path:Q}" with body "{text:Q}" and content type "{ctype:Q}"')
def step_send_raw(context, method, path, text, ctype):
    do_request(context, method, path, data=sub(context, text).encode("utf-8"),
               headers={"Content-Type": ctype})


@when('I send "{method:Q}" to "{path:Q}" with a body of {size:d} bytes and content type "{ctype:Q}"')
def step_send_raw_size(context, method, path, size, ctype):
    do_request(context, method, path, data=b"x" * size, headers={"Content-Type": ctype},
               timeout=vmclient.UPLOAD_TIMEOUT)


@when('I send "{method:Q}" to "{path:Q}" with a multipart body')
def step_send_multipart(context, method, path):
    rows = [(sub(context, r["field"]), r["value"]) for r in context.table]
    parts, files = build_multipart(context, rows)
    do_request(context, method, path, files=parts, timeout=vmclient.UPLOAD_TIMEOUT)
    if "vocabularyFile" in files:
        remember_upload(context, context.response, files["vocabularyFile"])
        learn_content_base(context.response)


@when('I send "{method:Q}" to "{path:Q}" with a multipart body of {count:d} parts')
def step_send_many_parts(context, method, path, count):
    parts = [("name", (None, name_of(context, "Parts"))),
             ("description", (None, "d")), ("changelog", (None, "c")),
             ("vocabularyFile", ("v.ttl", fixture_bytes(context, "valid.ttl"), "text/turtle"))]
    index = 0
    while len(parts) < count:
        parts.append((f"extra{index}", (None, "x")))
        index += 1
    do_request(context, method, path, files=parts)


@when('I create vocabulary "{name:Q}"')
def step_create(context, name):
    context.token = context.token
    rows = default_create_rows(name, "file:valid.ttl")
    parts, files = build_multipart(context, rows)
    do_request(context, "POST", "/vocabularies", files=parts, timeout=vmclient.UPLOAD_TIMEOUT)
    remember_upload(context, context.response, files["vocabularyFile"])
    learn_content_base(context.response)


@when('I create vocabulary "{name:Q}" with file "{fixture:Q}"')
def step_create_file(context, name, fixture):
    rows = default_create_rows(name, "file:" + fixture)
    parts, files = build_multipart(context, rows)
    do_request(context, "POST", "/vocabularies", files=parts, timeout=vmclient.UPLOAD_TIMEOUT)
    remember_upload(context, context.response, files["vocabularyFile"])


@when('I create vocabulary "{name:Q}" with content')
def step_create_content(context, name):
    rows = default_create_rows(name, "turtle:" + context.text)
    parts, files = build_multipart(context, rows)
    do_request(context, "POST", "/vocabularies", files=parts, timeout=vmclient.UPLOAD_TIMEOUT)
    remember_upload(context, context.response, files["vocabularyFile"])


def default_create_rows(name, file_value):
    return [("name", name), ("description", "Description of " + name),
            ("changelog", "Initial version."), ("vocabularyFile", file_value)]


@when('I create a new version of "{name:Q}"')
def step_new_version(context, name):
    send_version(context, name, None, None)


@when('I create a new version of "{name:Q}" with content')
def step_new_version_content(context, name):
    send_version(context, name, sub(context, context.text).encode(), None)


@when('I create a new version of "{name:Q}" with file "{fixture:Q}"')
def step_new_version_file(context, name, fixture):
    send_version(context, name, fixture_bytes(context, fixture), None)


@when('I create a new version of "{name:Q}" with description "{description:Q}"')
def step_new_version_desc(context, name, description):
    send_version(context, name, None, sub(context, description))


@when('I create a new version of "{name:Q}" with the content of version {version:d} and description "{description:Q}"')
def step_new_version_same(context, name, version, description):
    real = sub(context, name)
    content = context.uploaded[(real, version)]
    send_version(context, name, content, sub(context, description))


def send_version(context, name, content, description):
    real = sub(context, name)
    if content is None:
        content = fixture_bytes(context, "valid.ttl") + (
            f"\n# variant {vmclient.run_id(8)}\n").encode()
    if description is None:
        description = f"New version {vmclient.run_id(6)} of {real}"
    parts = [("description", (None, description)),
             ("changelog", (None, "Changed for a test.")),
             ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))]
    do_request(context, "POST", f"/vocabularies/{real}/versions", files=parts,
               timeout=vmclient.UPLOAD_TIMEOUT)
    remember_upload(context, context.response, content)


@when('I send {count:d} new versions of "{name:Q}" in parallel')
def step_parallel_versions(context, count, name):
    real = sub(context, name)
    token = context.token

    def one(index):
        content = fixture_bytes(context, "valid.ttl") + f"\n# parallel {index} {vmclient.run_id(6)}\n".encode()
        parts = [("description", (None, f"Parallel version {index}")),
                 ("changelog", (None, f"Parallel change {index}")),
                 ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))]
        return vmclient.send("POST", f"/vocabularies/{real}/versions", token=token, files=parts,
                             base=context.base_url, timeout=vmclient.UPLOAD_TIMEOUT)

    with concurrent.futures.ThreadPoolExecutor(max_workers=count) as pool:
        context.responses = list(pool.map(one, range(count)))


@when('I send {count:d} identical "{method:Q}" requests to "{path:Q}" in parallel with JSON body')
def step_parallel_json(context, count, method, path):
    payload = sub(context, context.text).encode()
    token = context.token
    real = sub(context, path)

    def one(_):
        return vmclient.send(method, real, token=token, data=payload,
                             headers={"Content-Type": "application/json"}, base=context.base_url)

    with concurrent.futures.ThreadPoolExecutor(max_workers=count) as pool:
        context.responses = list(pool.map(one, range(count)))


@when('I send {count:d} requests to create vocabulary "{name:Q}" in parallel')
def step_parallel_create(context, count, name):
    real = sub(context, name)
    token = context.token

    def one(index):
        variant = real if index % 2 == 0 else real.upper()
        return create_vocabulary(context, variant, token=token, record=False)

    with concurrent.futures.ThreadPoolExecutor(max_workers=count) as pool:
        context.responses = list(pool.map(one, range(count)))


@when('I remember the response field "{path:Q}" as "{key:Q}"')
@then('I remember the response field "{path:Q}" as "{key:Q}"')
def step_remember(context, path, key):
    context.vars[key] = get_path(body(context), path)


@when('I remember the response header "{header:Q}" as "{key:Q}"')
def step_remember_header(context, header, key):
    context.vars[key] = context.response.headers.get(header)


# ---------------------------------------------------------------- status and problems

@then("the status is {code:d}")
def step_status(context, code):
    assert context.response.status_code == code, f"expected {code}: {show(context)}"


@then("the status is {code1:d} or {code2:d}")
def step_status_either(context, code1, code2):
    assert context.response.status_code in (code1, code2), f"expected {code1}/{code2}: {show(context)}"


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def step_problem(context, code, ptype):
    r = context.response
    assert r.status_code == code, f"expected {code}: {show(context)}"
    ctype = r.headers.get("Content-Type", "")
    assert ctype.split(";")[0].strip() == "application/problem+json", \
        f"expected application/problem+json: {show(context)}"
    b = body(context)
    assert b.get("type") == ptype, f"expected type {ptype}: {show(context)}"
    assert b.get("status") == code, f"problem status field must be {code}: {show(context)}"


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    assert body(context).get("title") == title, show(context)


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    expected = sub(context, detail)
    assert body(context).get("detail") == expected, f"expected detail {expected!r}: {show(context)}"


@then('the problem detail starts with "{detail:Q}"')
def step_problem_detail_start(context, detail):
    assert str(body(context).get("detail", "")).startswith(sub(context, detail)), show(context)


@then("the problem has no issues")
def step_no_issues(context):
    assert not body(context).get("issues"), show(context)


@then("the problem has {count:d} issues")
@then("the problem has {count:d} issue")
def step_issue_count(context, count):
    issues = body(context).get("issues") or []
    assert len(issues) == count, f"expected {count} issues, got {len(issues)}: {show(context)}"


def issues_matching(context, where, name):
    issues = body(context).get("issues") or []
    return [i for i in issues if i.get("in") == where and i.get("name") == sub(context, name)]


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_named(context, where, name):
    assert issues_matching(context, where, name), f"no issue {where}/{name}: {show(context)}"


@then('the problem has exactly one issue, in "{where:Q}" named "{name:Q}"')
def step_one_issue(context, where, name):
    issues = body(context).get("issues") or []
    assert len(issues) == 1, f"expected one issue: {show(context)}"
    assert issues[0].get("in") == where and issues[0].get("name") == sub(context, name), show(context)


@then('every issue has type "{itype:Q}", in "{where:Q}" and name "{name:Q}"')
def step_all_issues(context, itype, where, name):
    issues = body(context).get("issues") or []
    assert issues, show(context)
    for issue in issues:
        assert issue.get("type") == itype and issue.get("in") == where \
            and issue.get("name") == name, f"bad issue {issue}: {show(context)}"


@then('the problem has an issue with detail "{detail:Q}" and location "{location:Q}"')
def step_issue_detail_loc(context, detail, location):
    d, loc = sub(context, detail), sub(context, location)
    issues = body(context).get("issues") or []
    assert any(i.get("detail") == d and i.get("location") == loc for i in issues), \
        f"no issue with detail {d!r} and location {loc!r}: {show(context)}"


@then('the problem has {count:d} issues with detail "{detail:Q}"')
def step_issue_detail_count(context, count, detail):
    d = sub(context, detail)
    issues = body(context).get("issues") or []
    found = [i for i in issues if i.get("detail") == d]
    assert len(found) == count, f"expected {count} issues {d!r}: {show(context)}"


@then('issue {index:d} has detail starting with "{prefix:Q}"')
def step_issue_detail_prefix(context, index, prefix):
    issue = (body(context).get("issues") or [])[index]
    assert str(issue.get("detail", "")).startswith(sub(context, prefix)), show(context)


@then('issue {index:d} has location matching "{pattern:Q}"')
def step_issue_loc_re(context, index, pattern):
    issue = (body(context).get("issues") or [])[index]
    assert re.fullmatch(pattern, str(issue.get("location", ""))), show(context)


@then('issue {index:d} has type "{itype:Q}"')
def step_issue_type(context, index, itype):
    issue = (body(context).get("issues") or [])[index]
    assert issue.get("type") == itype, show(context)


# ---------------------------------------------------------------- headers and bodies

@then('the response header "{header:Q}" equals "{value:Q}"')
def step_header_eq(context, header, value):
    actual = context.response.headers.get(header)
    assert actual == sub(context, value), f"{header}={actual!r}: {show(context)}"


@then('the response header "{header:Q}" is present')
def step_header_present(context, header):
    assert context.response.headers.get(header), f"no {header}: {show(context)}"


@then('the response header "{header:Q}" contains "{value:Q}"')
def step_header_contains(context, header, value):
    actual = context.response.headers.get(header) or ""
    assert sub(context, value) in actual, f"{header}={actual!r}: {show(context)}"


@then('the response media type is "{mtype:Q}"')
def step_media_type(context, mtype):
    actual = (context.response.headers.get("Content-Type") or "").split(";")[0].strip()
    assert actual == mtype, f"Content-Type {actual!r}: {show(context)}"


@then('the response field "{path:Q}" equals {value}')
def step_field_eq(context, path, value):
    expected = json.loads(sub(context, value))
    actual = get_path(body(context), sub(context, path))
    assert actual == expected, f"{path}: expected {expected!r}, got {actual!r}: {show(context)}"


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    assert not has_path(body(context), sub(context, path)), f"{path} present: {show(context)}"


@then('the response field "{path:Q}" is present')
def step_field_present(context, path):
    assert has_path(body(context), sub(context, path)), f"{path} absent: {show(context)}"


@then('the response field "{path:Q}" matches "{pattern:Q}"')
def step_field_re(context, path, pattern):
    actual = get_path(body(context), sub(context, path))
    assert re.fullmatch(sub(context, pattern), str(actual)), f"{path}={actual!r}: {show(context)}"


@then('the response field "{path:Q}" is a UTC timestamp')
def step_field_ts(context, path):
    actual = get_path(body(context), path)
    assert re.fullmatch(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", str(actual)), \
        f"{path}={actual!r}: {show(context)}"


@then('the response field "{path:Q}" is not earlier than "{value:Q}"')
def step_field_not_earlier(context, path, value):
    actual = str(get_path(body(context), path))
    assert actual >= sub(context, value), f"{path}={actual!r}: {show(context)}"


@then('the response field "{path:Q}" has {count:d} items')
def step_field_count(context, path, count):
    actual = get_path(body(context), path)
    assert isinstance(actual, list) and len(actual) == count, \
        f"{path} has {len(actual) if isinstance(actual, list) else actual!r}: {show(context)}"


@then('the response has the fields "{fields:Q}"')
def step_fields(context, fields):
    b = body(context)
    for field in split_list(context, fields):
        assert field in b, f"missing {field}: {show(context)}"


@then('every item has the fields "{fields:Q}"')
def step_item_fields(context, fields):
    items = body(context)["items"]
    assert items, show(context)
    for item in items:
        for field in split_list(context, fields):
            assert field in item, f"item misses {field}: {item}"


@then('the item names are "{names:Q}"')
def step_item_names(context, names):
    actual = [i.get("name") for i in body(context)["items"]]
    expected = split_list(context, names)
    assert actual == expected, f"expected {expected}, got {actual}"


@then('the item versions are "{versions:Q}"')
def step_item_versions(context, versions):
    actual = [i.get("version") for i in body(context)["items"]]
    expected = [int(v) for v in split_list(context, versions)]
    assert actual == expected, f"expected {expected}, got {actual}"


@then('every item has "{field:Q}" equal to {value}')
def step_every_item(context, field, value):
    expected = json.loads(sub(context, value))
    items = body(context)["items"]
    assert items, show(context)
    for item in items:
        assert item.get(field) == expected, f"item {item}"


@then('the response body is the content uploaded for version {version:d} of "{name:Q}"')
def step_body_bytes(context, version, name):
    expected = context.uploaded[(sub(context, name), version)]
    assert context.response.content == expected, \
        f"bytes differ ({len(context.response.content)} vs {len(expected)})"


@then('the response field "{path:Q}" is the contentBase followed by "{suffix:Q}"')
def step_content_href(context, path, suffix):
    expected = content_base(context) + sub(context, suffix)
    actual = get_path(body(context), path)
    assert actual == expected, f"{path}: expected {expected!r}, got {actual!r}"


@then('the warning codes are "{codes:Q}"')
def step_warning_codes(context, codes):
    actual = sorted(w.get("code") for w in body(context)["validationWarnings"])
    expected = sorted(split_list(context, codes))
    assert actual == expected, f"expected {expected}, got {actual}: {show(context)}"


@then("there are no validation warnings")
def step_no_warnings(context):
    assert body(context).get("validationWarnings") == [], show(context)


@then('there is a warning "{code:Q}" with description "{description:Q}" and location "{location:Q}"')
def step_warning(context, code, description, location):
    d, loc = sub(context, description), sub(context, location)
    warnings = body(context).get("validationWarnings") or []
    assert any(w.get("code") == code and w.get("description") == d and w.get("location") == loc
               for w in warnings), f"no warning {code} {d!r} {loc!r}: {show(context)}"


@then('there is a warning "{code:Q}" with location "{location:Q}"')
def step_warning_loc(context, code, location):
    loc = sub(context, location)
    warnings = body(context).get("validationWarnings") or []
    assert any(w.get("code") == code and w.get("location") == loc for w in warnings), \
        f"no warning {code} at {loc!r}: {show(context)}"


# ---------------------------------------------------------------- parallel results

@then('all {count:d} responses have status {code:d}')
def step_all_status(context, count, code):
    codes = [r.status_code for r in context.responses]
    assert len(codes) == count and all(c == code for c in codes), f"statuses {codes}"


@then('the responses have the statuses "{codes:Q}"')
def step_statuses(context, codes):
    actual = sorted(r.status_code for r in context.responses)
    expected = sorted(int(c) for c in split_list(context, codes))
    assert actual == expected, f"expected {expected}, got {actual}"


@then('the created versions are {first:d} to {last:d}, each exactly once')
def step_created_versions(context, first, last):
    versions = sorted(r.json()["version"] for r in context.responses)
    assert versions == list(range(first, last + 1)), f"versions {versions}"


@then("the Location headers are all different")
def step_locations_distinct(context):
    locs = [r.headers.get("Location") for r in context.responses]
    assert None not in locs and len(set(locs)) == len(locs), f"locations {locs}"


# ---------------------------------------------------------------- stored state (read as admin)

@then('vocabulary "{name:Q}" has status "{status:Q}"')
def step_check_vocab_status(context, name, status):
    resp = admin_get(f"/vocabularies/{sub(context, name)}")
    assert resp.status_code == 200 and resp.json()["status"] == status, resp.text[:300]


@then('version {version:d} of vocabulary "{name:Q}" has status "{status:Q}"')
def step_check_version_status(context, version, name, status):
    resp = admin_get(f"/vocabularies/{sub(context, name)}/versions/{version}")
    assert resp.status_code == 200 and resp.json()["status"] == status, resp.text[:300]


@then('the versions of vocabulary "{name:Q}" have the statuses "{statuses:Q}"')
def step_check_statuses(context, name, statuses):
    resp = admin_get(f"/vocabularies/{sub(context, name)}/versions?pageSize=100")
    assert resp.status_code == 200, resp.text[:300]
    actual = {i["version"]: i["status"] for i in resp.json()["items"]}
    expected = {}
    for part in split_list(context, statuses):
        version, status = part.split("=")
        expected[int(version)] = status
    assert actual == expected, f"expected {expected}, got {actual}"


@then('vocabulary "{name:Q}" has {count:d} versions')
@then('vocabulary "{name:Q}" has {count:d} version')
def step_check_version_count(context, name, count):
    resp = admin_get(f"/vocabularies/{sub(context, name)}/versions?pageSize=100")
    assert resp.status_code == 200 and resp.json()["total"] == count, resp.text[:300]


@then('vocabulary "{name:Q}" has latest version {version:d}')
def step_check_latest(context, name, version):
    resp = admin_get(f"/vocabularies/{sub(context, name)}")
    assert resp.status_code == 200 and resp.json()["version"] == version, resp.text[:300]


@then('no vocabulary named "{name:Q}" is stored')
def step_not_stored(context, name):
    """Nothing stored: list filtered on the scenario run id, no item with that name."""
    real = sub(context, name).lower()
    resp = admin_get(f"/vocabularies?name={context.run}&pageSize=100")
    assert resp.status_code == 200, resp.text[:300]
    names = [i["name"].lower() for i in resp.json()["items"]]
    assert real not in names, f"{name} was stored: {names}"


@then("no vocabulary of this scenario is stored")
def step_none_stored(context):
    resp = admin_get(f"/vocabularies?name={context.run}&pageSize=100")
    assert resp.status_code == 200 and resp.json()["total"] == 0, resp.text[:300]


@then('{count:d} vocabularies of this scenario are stored')
@then('{count:d} vocabulary of this scenario is stored')
def step_count_stored(context, count):
    resp = admin_get(f"/vocabularies?name={context.run}&pageSize=100")
    assert resp.status_code == 200 and resp.json()["total"] == count, resp.text[:300]


@then('no external vocabulary is registered for namespace "{namespace:Q}"')
def step_no_external_ns(context, namespace):
    resp = vmclient.send("POST", "/external-vocabulary/searches", token=admin_token(),
                         json={"namespaceUri": sub(context, namespace)}, base=vmclient.API_URL)
    assert resp.status_code == 200 and resp.json()["items"] == [], resp.text[:300]


@then('the external vocabulary "{name:Q}" is registered')
def step_external_registered(context, name):
    resp = admin_get(f"/external-vocabularies/{sub(context, name)}")
    assert resp.status_code == 200, resp.text[:300]


@then('no external vocabulary named "{name:Q}" is registered')
def step_external_not_registered(context, name):
    resp = admin_get(f"/external-vocabularies/{sub(context, name)}")
    assert resp.status_code == 404, resp.text[:300]


@when('I create vocabulary "{name:Q}" with {count:d} properties declared both object and datatype property')
def step_create_many_s3(context, name, count):
    lines = ["@prefix owl: <http://www.w3.org/2002/07/owl#> .",
             f"@prefix ex: <https://example.org/vm/{context.run}#> ."]
    for index in range(count):
        lines.append(f"ex:p{index:05d} a owl:ObjectProperty , owl:DatatypeProperty .")
    content = ("\n".join(lines) + "\n").encode()
    rows = default_create_rows(name, "file:valid.ttl")
    parts, _ = build_multipart(context, rows)
    parts[-1] = ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))
    do_request(context, "POST", "/vocabularies", files=parts, timeout=vmclient.UPLOAD_TIMEOUT)


@then('the response field "{path:Q}" is not true')
def step_field_not_true(context, path):
    b = body(context)
    assert not (has_path(b, path) and get_path(b, path) is True), f"{path} is true: {show(context)}"


@then('the problem has an issue with detail "{detail:Q}" and the location')
def step_issue_detail_doc_loc(context, detail):
    d, loc = sub(context, detail), sub(context, context.text.strip())
    issues = body(context).get("issues") or []
    assert any(i.get("detail") == d and i.get("location") == loc for i in issues), \
        f"no issue with detail {d!r} and location {loc!r}: {show(context)}"


@when('I send "POST" to "/vocabularies/searches" with {count:d} references')
def step_search_many(context, count):
    items = [{"name": f"Ref{index:04d}{context.run}"} for index in range(count)]
    do_request(context, "POST", "/vocabularies/searches", data=json.dumps({"items": items}).encode(),
               headers={"Content-Type": "application/json"})


@when('I send an external search with {namespaces:d} namespace URIs and {aliases:d} alias URIs')
def step_external_search_many(context, namespaces, aliases):
    payload = {}
    if namespaces:
        payload["namespaceUris"] = [f"https://example.org/ns/{context.run}/{i}/" for i in range(namespaces)]
    if aliases:
        payload["aliasUris"] = [f"https://example.org/alias/{context.run}/{i}/" for i in range(aliases)]
    do_request(context, "POST", "/external-vocabulary/searches", data=json.dumps(payload).encode(),
               headers={"Content-Type": "application/json"})


@when('I register external vocabulary "{name:Q}" with {count:d} aliases')
def step_register_aliases(context, name, count):
    real = sub(context, name)
    parts = [("name", (None, real)),
             ("namespaceUri", (None, f"https://example.org/ext/{context.run}/{real}/")),
             ("vocabularyFile", ("external.ttl", fixture_bytes(context, "external-dcterms.ttl"),
                                 "text/turtle"))]
    for index in range(count):
        parts.append(("aliasUris", (None, f"https://alias.example.org/{context.run}/{real}/{index}/")))
    do_request(context, "POST", "/external-vocabularies", files=parts)


@then('the external items of this scenario are in the order "{names:Q}"')
def step_external_order(context, names):
    expected = split_list(context, names)
    found = []
    page = 1
    while True:
        resp = admin_get(f"/external-vocabularies?page={page}&pageSize=100")
        assert resp.status_code == 200, resp.text[:300]
        data = resp.json()
        found += [i["name"] for i in data["items"] if i["name"] in expected]
        if page >= data.get("totalPages", 1) or not data["items"]:
            break
        page += 1
    assert found == expected, f"expected {expected}, got {found}"


@then('the response body is the external fixture "{fixture:Q}"')
def step_external_bytes(context, fixture):
    expected = fixture_bytes(context, fixture)
    assert context.response.content == expected, "external content differs"


@when('I send "{method:Q}" to "{path:Q}" followed by a query of {size:d} characters')
def step_send_long_url(context, method, path, size):
    do_request(context, method, path + "&pad=" + "a" * size)


@given('the request header "{header:Q}" is a value of {size:d} characters')
def step_big_header(context, header, size):
    context.extra_headers[header] = "a" * size


# ---------------------------------------------------------------- additions after the suite review

@then('the response field "{path:Q}" is later than "{value:Q}"')
def step_field_later(context, path, value):
    actual = str(get_path(body(context), path))
    assert actual > sub(context, value), f"{path}={actual!r} not later than {sub(context, value)!r}"


@then('the response field "{path:Q}" is absent or an empty list')
def step_field_absent_or_empty(context, path):
    b = body(context)
    assert not has_path(b, path) or get_path(b, path) == [], f"{path}: {show(context)}"


@given("the request has no Accept header")
def step_no_accept(context):
    context.extra_headers["Accept"] = None  # requests drops headers set to None


@when('I send a create request of exactly {size:d} bytes padded with the unknown field "{field:Q}"')
def step_exact_multipart(context, size, field):
    """Multipart body of an exact total size: valid fields plus one padding part."""
    boundary = "vmtestboundary" + context.run
    name = name_of(context, "Health")

    def part(pname, value, filename=None):
        head = f'--{boundary}\r\nContent-Disposition: form-data; name="{pname}"'
        if filename:
            head += f'; filename="{filename}"\r\nContent-Type: text/turtle'
        return head.encode() + b"\r\n\r\n" + value + b"\r\n"

    fixed = (part("name", name.encode()) + part("description", b"Exact size")
             + part("changelog", b"First.")
             + part("vocabularyFile", fixture_bytes(context, "valid.ttl"), "v.ttl"))
    tail = f"--{boundary}--\r\n".encode()
    empty_pad = part(field, b"")
    pad = size - len(fixed) - len(tail) - len(empty_pad)
    assert pad >= 0, "size too small"
    data = fixed + part(field, b"x" * pad) + tail
    assert len(data) == size
    do_request(context, "POST", "/vocabularies", data=data,
               headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
               timeout=vmclient.UPLOAD_TIMEOUT)


@given('{count:d} numbered vocabularies "{alias:Q}" exist')
def step_numbered_vocabs(context, count, alias):
    base = sub(context, alias)
    for index in range(1, count + 1):
        name = f"{base}N{index:02d}"
        expect_created(create_vocabulary(context, name), "create " + name)
