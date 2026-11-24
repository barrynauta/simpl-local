"""Shared step library of the Vocabulary Manager API suite (black box, HTTP only).

Placeholders such as $Alpha, $RUN, $CB and ${key} are described in vmtest/state.py.
"""
import json
import re
import time
import urllib.parse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timedelta, timezone

import parse
from behave import given, then, when, step, register_type

@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(Q=_quoted)

from vmtest import client, config, turtle

TIMESTAMP = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")


# =========================================================================== helpers
def vm(context):
    return context.vm


def x(context, text):
    return context.vm.expand(text)


def body(context):
    response = context.vm.response
    assert response is not None, "no response yet"
    try:
        return response.json()
    except ValueError as exc:
        raise AssertionError(f"response is not JSON: {response.status_code} {response.text[:500]}") from exc


def show(response):
    return f"{response.status_code} {response.headers.get('Content-Type')} {response.text[:800]}"


def lookup(data, path):
    """Resolve a dotted path with [i] indexes: items[0].name, issues[1].in."""
    current = data
    for part in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if part.startswith("["):
            index = int(part[1:-1])
            if not isinstance(current, list) or index >= len(current):
                raise KeyError(path)
            current = current[index]
        else:
            if not isinstance(current, dict) or part not in current:
                raise KeyError(path)
            current = current[part]
    return current


def media_type(response):
    return (response.headers.get("Content-Type") or "").split(";")[0].strip().lower()


def issues(context):
    data = body(context)
    return data.get("issues") or []


def table_value(context, raw):
    return x(context, raw)


def part_value(context, raw, name):
    """Values of multipart tables. Specials:
    @file            the file set by 'the vocabulary file is'
    @valid           a fresh valid Turtle file
    @size:<n>        a valid Turtle file of exactly n bytes
    @repeat:<c>:<n>  the character c repeated n times
    @padded:<c>:<n>  three spaces, c repeated n times, three spaces
    @empty           an empty value
    """
    if raw == "@file":
        return ("vocabulary.ttl", context.vm.file, "text/turtle")
    if raw == "@valid":
        return ("vocabulary.ttl", turtle.default_file(context.vm.run, "Part"), "text/turtle")
    if raw.startswith("@size:"):
        return ("vocabulary.ttl", turtle.sized_file(context.vm.run, "Big", int(raw[6:])), "text/turtle")
    if raw.startswith("@repeat:"):
        _, char, count = raw.split(":")
        value = char * int(count)
        return ("vocabulary.ttl", value.encode(), "text/turtle") if name == "vocabularyFile" else value
    if raw.startswith("@padded:"):
        _, char, count = raw.split(":")
        return "   " + char * int(count) + "   "
    if raw == "@empty":
        return ("vocabulary.ttl", b"", "text/turtle") if name == "vocabularyFile" else ""
    value = x(context, raw)
    if name == "vocabularyFile":
        return ("vocabulary.ttl", value.encode(), "text/turtle")
    return value


def save_response(context, response):
    context.vm.response = response
    return response


def parse_time(value):
    return datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)


# =========================================================================== authentication
@given('I am authenticated as "{user:Q}"')
def step_auth(context, user):
    context.vm.token = client.token_for(user)


@given("I am not authenticated")
def step_no_auth(context):
    context.vm.token = None


@given('I send the authorization header "{value:Q}"')
def step_raw_auth(context, value):
    context.vm.token = None
    context.vm.headers["Authorization"] = x(context, value)


@given('I use a "{user:Q}" token with a broken signature')
def step_broken_sig(context, user):
    context.vm.token = client.break_signature(client.token_for(user))


@given('I use a "{user:Q}" token without the audience of the service')
def step_noaud(context, user):
    context.vm.token = client.fetch_token(user, client=config.TOKEN_CLIENT_NOAUD, use_cache=False)[0]


@given('I use a "{user:Q}" token from the other realm')
def step_other_realm(context, user):
    context.vm.token = client.fetch_token(user, token_url=config.TOKEN_URL_OTHER, use_cache=False)[0]


@given('I use a "{user:Q}" token that has expired')
def step_expired(context, user):
    token, expires_in = client.fetch_token(user, client=config.TOKEN_CLIENT_SHORT, use_cache=False)
    assert expires_in <= 60, f"short-lived client gives {expires_in} s tokens"
    time.sleep(expires_in + 70)  # beyond the 60 s default clock skew of the libraries D132 names (SR-01)
    context.vm.token = token


# =========================================================================== request options
@given('the request header "{header:Q}" is "{value:Q}"')
def step_header(context, header, value):
    context.vm.headers[header] = x(context, value)


@given("the request has no Accept header")
def step_no_accept(context):
    context.vm.headers["Accept"] = None


@given("the vocabulary file is")
@given("the vocabulary file is:")
def step_file_doc(context):
    context.vm.file = x(context, context.text).encode("utf-8") + b"\n"


@given("the vocabulary file has these exact bytes")
@given("the vocabulary file has these exact bytes:")
def step_file_exact(context):
    context.vm.file = x(context, context.text).encode("utf-8")


@given("the vocabulary file is empty")
def step_file_empty(context):
    context.vm.file = b""


@given("the vocabulary file is a valid Turtle file of {size:d} bytes")
def step_file_size(context, size):
    context.vm.file = turtle.sized_file(context.vm.run, "Big", size)


@given('the vocabulary file is the fixture "{fname:Q}"')
def step_file_fixture(context, fname):
    context.vm.file = turtle.fixture(fname)


@given('the vocabulary file is a valid Turtle file')
def step_file_valid(context):
    context.vm.file = turtle.default_file(context.vm.run, "File")


@given('the vocabulary file is the current content of "{name:Q}"')
def step_file_current(context, name):
    name = x(context, name)
    response = context.vm.admin("GET", f"/content/{name}")
    assert response.status_code == 200, show(response)
    context.vm.file = response.content


@given("the vocabulary file has {count:d} subjects with a duplicate English label")
def step_file_many_errors(context, count):
    lines = ["@prefix skos: <http://www.w3.org/2004/02/skos/core#> .",
             f"@prefix ex: <https://example.org/{context.vm.run}/many#> ."]
    for i in range(count):
        lines.append(f'ex:c{i} skos:prefLabel "A {i}"@en , "B {i}"@en .')
    context.vm.file = ("\n".join(lines) + "\n").encode()


# =========================================================================== setup (always as ga-admin)
@given('a vocabulary "{name:Q}" exists')
def step_vocab_exists(context, name):
    response = context.vm.create_vocabulary(x(context, name))
    assert response.status_code == 201, show(response)


@given('a vocabulary "{name:Q}" exists with description "{description:Q}"')
def step_vocab_exists_desc(context, name, description):
    response = context.vm.create_vocabulary(x(context, name), description=x(context, description))
    assert response.status_code == 201, show(response)


@given('a vocabulary "{name:Q}" exists with the file')
@given('a vocabulary "{name:Q}" exists with the file:')
def step_vocab_exists_file(context, name):
    response = context.vm.create_vocabulary(x(context, name), file=x(context, context.text).encode() + b"\n")
    assert response.status_code == 201, show(response)


@given('a vocabulary "{name:Q}" exists with that file')
def step_vocab_exists_that_file(context, name):
    response = context.vm.create_vocabulary(x(context, name), file=context.vm.file)
    assert response.status_code == 201, show(response)


@given('vocabularies "{names:Q}" exist, created one second apart')
def step_vocabs_apart(context, names):
    for i, name in enumerate(n.strip() for n in names.split(",")):
        if i:
            time.sleep(1.1)
        response = context.vm.create_vocabulary(x(context, name))
        assert response.status_code == 201, show(response)


@given('vocabulary "{name:Q}" has {count:d} versions')
def step_vocab_versions(context, name, count):
    name = x(context, name)
    if name not in context.vm.uploads:
        response = context.vm.create_vocabulary(name)
        assert response.status_code == 201, show(response)
    while len(context.vm.uploads[name]) < count:
        response = context.vm.create_version(name)
        assert response.status_code == 201, show(response)


@given('a new version of "{name:Q}" is created')
def step_new_version_setup(context, name):
    response = context.vm.create_version(x(context, name))
    assert response.status_code == 201, show(response)


@given('a new version of "{name:Q}" is created with description "{description:Q}"')
def step_new_version_setup_desc(context, name, description):
    response = context.vm.create_version(x(context, name), description=x(context, description))
    assert response.status_code == 201, show(response)


@given('vocabulary "{name:Q}" has vocabulary status "{status:Q}"')
def step_set_vocab_status(context, name, status):
    response = context.vm.admin("PATCH", f"/vocabularies/{x(context, name)}", json_body={"status": status})
    assert response.status_code == 200, show(response)


@given('version {version:d} of vocabulary "{name:Q}" has version status "{status:Q}"')
def step_set_version_status(context, version, name, status):
    response = context.vm.admin("PATCH", f"/vocabularies/{x(context, name)}/versions/{version}",
                                json_body={"status": status})
    assert response.status_code == 200, show(response)


@given('an external vocabulary "{name:Q}" is registered with namespace "{namespace:Q}"')
def step_ext_registered(context, name, namespace):
    response = context.vm.register_external(x(context, name), x(context, namespace))
    assert response.status_code == 201, show(response)


@given('an external vocabulary "{name:Q}" is registered with namespace "{namespace:Q}" and aliases "{aliases:Q}"')
def step_ext_registered_alias(context, name, namespace, aliases):
    alias_list = [x(context, a.strip()) for a in aliases.split(",") if a.strip()]
    response = context.vm.register_external(x(context, name), x(context, namespace), alias_list)
    assert response.status_code == 201, show(response)


@given('the external vocabulary "{name:Q}" is removed')
def step_ext_removed(context, name):
    response = context.vm.admin("DELETE", f"/external-vocabularies/{x(context, name)}")
    assert response.status_code == 204, show(response)


@step("a second has passed")
def step_second(context):
    time.sleep(1.2)


@given('the content base is known')
def step_cb_known(context):
    x(context, "$CB")


# =========================================================================== actions
@when('I send a {method:w} request to "{path:Q}"')
def step_send(context, method, path):
    save_response(context, context.vm.request(method, x(context, path)))


@when('I send a {method:w} request to "{path:Q}" with JSON')
@when('I send a {method:w} request to "{path:Q}" with JSON:')
def step_send_json(context, method, path):
    data = x(context, context.text).encode("utf-8")
    save_response(context, context.vm.request(method, x(context, path), data=data,
                                              headers={"Content-Type": "application/json"}))


@when('I send a {method:w} request to "{path:Q}" with the body "{text:Q}" as "{ctype:Q}"')
def step_send_raw(context, method, path, text, ctype):
    headers = {"Content-Type": ctype} if ctype else {}
    save_response(context, context.vm.request(method, x(context, path), data=x(context, text).encode(),
                                              headers=headers))


@when('I send a {method:w} request to "{path:Q}" with the body')
@when('I send a {method:w} request to "{path:Q}" with the body:')
def step_send_doc(context, method, path):
    save_response(context, context.vm.request(method, x(context, path), data=x(context, context.text).encode()))


@when('I send a {method:w} request to "{path:Q}" with a JSON body of {size:d} bytes')
def step_send_big_json(context, method, path, size):
    prefix = b'{"status": "ACTIVE", "padding": "'
    data = prefix + b"x" * (size - len(prefix) - 2) + b'"}'
    save_response(context, context.vm.request(method, x(context, path), data=data,
                                              headers={"Content-Type": "application/json"}))


@when('I send a multipart {method:w} request to "{path:Q}" with the parts')
@when('I send a multipart {method:w} request to "{path:Q}" with the parts:')
def step_send_multipart(context, method, path):
    parts = []
    for row in context.table:
        name = "" if row["part"] == "@noname" else x(context, row["part"])
        parts.append((name, part_value(context, row["value"], name)))
    save_response(context, context.vm.request(method, x(context, path), files=client.multipart(parts)))


@when('I send a multipart POST request to "{path:Q}" with {count:d} parts')
def step_send_many_parts(context, path, count):
    parts = [("name", x(context, "$Many")), ("description", "d"), ("changelog", "c"),
             ("vocabularyFile", ("v.ttl", turtle.default_file(context.vm.run, "Many"), "text/turtle"))]
    i = 0
    while len(parts) < count:
        parts.append((f"extra{i}", "v"))
        i += 1
    save_response(context, context.vm.request("POST", x(context, path), files=client.multipart(parts)))


@when('I create vocabulary "{name:Q}"')
def step_create(context, name):
    name = x(context, name)
    content = context.vm.file if context.vm.file is not None else turtle.default_file(context.vm.run, name)
    parts = [("name", name), ("description", f"Description of {name}"), ("changelog", "Initial version."),
             ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))]
    response = save_response(context, context.vm.request("POST", "/vocabularies", files=client.multipart(parts)))
    if response.status_code == 201:
        context.vm.uploads.setdefault(name, {})[1] = content


@when('I create vocabulary "{name:Q}" with description "{description:Q}"')
def step_create_desc(context, name, description):
    name = x(context, name)
    content = context.vm.file if context.vm.file is not None else turtle.default_file(context.vm.run, name)
    parts = [("name", name), ("description", x(context, description)), ("changelog", "Initial version."),
             ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))]
    response = save_response(context, context.vm.request("POST", "/vocabularies", files=client.multipart(parts)))
    if response.status_code == 201:
        context.vm.uploads.setdefault(name, {})[1] = content


@when('I create a new version of "{name:Q}"')
def step_new_version(context, name):
    step_new_version_full(context, name, None, None)


@when('I create a new version of "{name:Q}" with description "{description:Q}"')
def step_new_version_desc(context, name, description):
    step_new_version_full(context, name, description, None)


@when('I create a new version of "{name:Q}" with description "{description:Q}" and changelog "{changelog:Q}"')
def step_new_version_full(context, name, description, changelog):
    name = x(context, name)
    content = context.vm.file if context.vm.file is not None else turtle.default_file(context.vm.run, name)
    count = len(context.vm.uploads.get(name, {})) + 1
    parts = [("description", x(context, description) if description else f"Description {count} {time.time()}"),
             ("changelog", x(context, changelog) if changelog else f"Change {count}"),
             ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))]
    response = save_response(context, context.vm.request("POST", f"/vocabularies/{name}/versions",
                                                         files=client.multipart(parts)))
    if response.status_code == 201:
        context.vm.uploads.setdefault(name, {})[response.json()["version"]] = content


@when('I register the external vocabulary "{name:Q}" with namespace "{namespace:Q}"')
def step_register(context, name, namespace):
    name = x(context, name)
    content = context.vm.file if context.vm.file is not None else None
    parts = [("name", name), ("namespaceUri", x(context, namespace))]
    file = content if content is not None else (
        f"@prefix skos: <{turtle.SKOS}> .\n<{x(context, namespace).rstrip('#/')}#T> a skos:Concept .\n").encode()
    parts.append(("vocabularyFile", ("external.ttl", file, "text/turtle")))
    response = save_response(context, context.vm.request("POST", "/external-vocabularies",
                                                         files=client.multipart(parts)))
    if response.status_code == 201:
        context.vm.ext_uploads[name] = file


@when('I send {count:d} new versions of "{name:Q}" in parallel')
def step_parallel(context, count, name):
    name = x(context, name)
    token = context.vm.token

    def one(i):
        content = turtle.default_file(context.vm.run, name, extra=f"# parallel {i}\n")
        parts = [("description", f"Parallel {i}"), ("changelog", f"Parallel change {i}"),
                 ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle"))]
        return client.send("POST", context.vm.url(f"/vocabularies/{name}/versions"), token=token,
                           files=client.multipart(parts))

    with ThreadPoolExecutor(max_workers=count) as pool:
        context.vm.parallel = list(pool.map(one, range(count)))


@when('I send {count:d} identical requests to "{path:Q}" in parallel with JSON')
@when('I send {count:d} identical requests to "{path:Q}" in parallel with JSON:')
def step_parallel_patch(context, count, path):
    data = x(context, context.text).encode()
    url = context.vm.url(x(context, path))
    token = context.vm.token
    with ThreadPoolExecutor(max_workers=count) as pool:
        context.vm.parallel = list(pool.map(
            lambda _: client.send("PATCH", url, token=token, data=data,
                                  headers={"Content-Type": "application/json"}), range(count)))


@when('I create vocabulary "{name:Q}" twice in parallel')
def step_parallel_create(context, name):
    name = x(context, name)
    token = context.vm.token

    def one(i):
        parts = [("name", name if i == 0 else name.upper()), ("description", f"Parallel {i}"),
                 ("changelog", "Initial version."),
                 ("vocabularyFile", ("v.ttl", turtle.default_file(context.vm.run, name), "text/turtle"))]
        return client.send("POST", context.vm.url("/vocabularies"), token=token, files=client.multipart(parts))

    with ThreadPoolExecutor(max_workers=2) as pool:
        context.vm.parallel = list(pool.map(one, range(2)))


@when('I send a GET request to "{path:Q}" with a query value of {size:d} characters')
def step_long_url(context, path, size):
    save_response(context, context.vm.request("GET", x(context, path) + "?name=" + "A" * size))


@when('I send a GET request to "{path:Q}" with a header of {size:d} characters')
def step_long_header(context, path, size):
    save_response(context, context.vm.request("GET", x(context, path), headers={"X-Padding": "a" * size}))


@when('I read "{path:Q}" as "{user:Q}"')
def step_read_as(context, path, user):
    save_response(context, client.send("GET", context.vm.url(x(context, path)), token=client.token_for(user)))


@when('I follow the "{rel:Q}" link')
def step_follow(context, rel):
    link = body(context)[rel]
    save_response(context, context.vm.request("GET", link))


@step('I save the response field "{path:Q}" as "{key:Q}"')
def step_save(context, path, key):
    context.vm.saved[key] = lookup(body(context), path)


@given('I save field "{field:Q}" of "{path:Q}" as "{key:Q}"')
def step_save_from(context, field, path, key):
    response = context.vm.admin("GET", x(context, path))
    assert response.status_code == 200, show(response)
    context.vm.saved[key] = lookup(response.json(), field)


@given('I save the time "{key:Q}" as "{key2:Q}" shifted to "{offset:Q}"')
def step_save_shifted(context, key, key2, offset):
    """Write a saved UTC timestamp with another offset, for example +01:00."""
    moment = parse_time(context.vm.saved[key])
    sign = 1 if offset[0] == "+" else -1
    hours, minutes = (int(v) for v in offset[1:].split(":"))
    delta = timedelta(hours=hours, minutes=minutes) * sign
    local = moment + delta
    context.vm.saved[key2] = local.strftime("%Y-%m-%dT%H:%M:%S") + offset


@given('I save the date of "{key:Q}" as "{key2:Q}"')
def step_save_date(context, key, key2):
    context.vm.saved[key2] = context.vm.saved[key][:10]


@given('I save the time "{key:Q}" without its offset as "{key2:Q}"')
def step_save_no_offset(context, key, key2):
    context.vm.saved[key2] = context.vm.saved[key].rstrip("Z")


# =========================================================================== status and problem assertions
@then("the response status is {code:d}")
def step_status(context, code):
    response = context.vm.response
    assert response.status_code == code, f"expected {code}, got {show(response)}"


@then("the response is a problem with status {code:d}")
def step_problem(context, code):
    response = context.vm.response
    assert response.status_code == code, f"expected {code}, got {show(response)}"
    assert media_type(response) == "application/problem+json", f"media type: {show(response)}"
    data = body(context)
    assert data.get("status") == code, f"problem status field: {data}"


@then('the problem type is "{ptype:Q}"')
def step_problem_type(context, ptype):
    assert body(context).get("type") == ptype, body(context)


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    assert body(context).get("title") == title, body(context)


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    assert body(context).get("detail") == x(context, detail), body(context)


@then('the problem detail starts with "{detail:Q}"')
def step_problem_detail_start(context, detail):
    assert str(body(context).get("detail", "")).startswith(x(context, detail)), body(context)


@then("the problem has {count:d} issue")
@then("the problem has {count:d} issues")
def step_issue_count(context, count):
    found = issues(context)
    assert len(found) == count, f"expected {count} issues, got {len(found)}: {json.dumps(found)[:1500]}"


@then("the problem has no issues")
def step_no_issues(context):
    assert not issues(context), body(context)


@then('issue {index:d} is in "{where:Q}" named "{name:Q}"')
def step_issue_in(context, index, where, name):
    issue = issues(context)[index - 1]
    assert issue.get("in") == where and issue.get("name") == x(context, name), issue


@then('issue {index:d} has type "{itype:Q}"')
def step_issue_type(context, index, itype):
    issue = issues(context)[index - 1]
    assert issue.get("type") == itype, issue


@then('issue {index:d} has detail "{detail:Q}"')
def step_issue_detail(context, index, detail):
    issue = issues(context)[index - 1]
    assert issue.get("detail") == x(context, detail), issue


@then('issue {index:d} has a detail starting with "{detail:Q}"')
def step_issue_detail_start(context, index, detail):
    issue = issues(context)[index - 1]
    assert str(issue.get("detail", "")).startswith(x(context, detail)), issue


@then('issue {index:d} has location "{location:Q}"')
def step_issue_location(context, index, location):
    issue = issues(context)[index - 1]
    assert issue.get("location") == x(context, location), issue


@then('issue {index:d} has a location matching "{pattern:Q}"')
def step_issue_location_re(context, index, pattern):
    issue = issues(context)[index - 1]
    assert re.fullmatch(x(context, pattern), str(issue.get("location", ""))), issue


@then('every issue is in "{where:Q}" named "{name:Q}" with type "{itype:Q}"')
def step_every_issue(context, where, name, itype):
    for issue in issues(context):
        assert issue.get("in") == where and issue.get("name") == name and issue.get("type") == itype, issue


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_any_issue(context, where, name):
    name = x(context, name)
    assert any(i.get("in") == where and i.get("name") == name for i in issues(context)), issues(context)


@then('the problem has an issue with detail "{detail:Q}" and location "{location:Q}"')
def step_issue_detail_location(context, detail, location):
    detail, location = x(context, detail), x(context, location)
    assert any(i.get("detail") == detail and i.get("location") == location for i in issues(context)), \
        json.dumps(issues(context), indent=1)[:3000]


@then('the problem has an issue with detail "{detail:Q}" and a location containing "{part:Q}"')
def step_issue_detail_location_part(context, detail, part):
    detail, part = x(context, detail), x(context, part)
    assert any(i.get("detail") == detail and part in str(i.get("location", "")) for i in issues(context)), \
        json.dumps(issues(context), indent=1)[:3000]


@then('the problem has {count:d} issues with detail "{detail:Q}"')
@then('the problem has {count:d} issue with detail "{detail:Q}"')
def step_issue_detail_count(context, count, detail):
    found = [i for i in issues(context) if i.get("detail") == x(context, detail)]
    assert len(found) == count, json.dumps(issues(context), indent=1)[:3000]


@then('the problem has an issue with detail "{detail:Q}"')
def step_issue_detail_any(context, detail):
    detail = x(context, detail)
    assert any(i.get("detail") == detail for i in issues(context)), json.dumps(issues(context), indent=1)[:3000]


# =========================================================================== headers and body assertions
@then('the response header "{header:Q}" is "{value:Q}"')
def step_header_is(context, header, value):
    got = context.vm.response.headers.get(header)
    assert got == x(context, value), f"{header}: {got!r}"


@then('the response header "{header:Q}" is present')
def step_header_present(context, header):
    assert context.vm.response.headers.get(header), dict(context.vm.response.headers)


@then('the response header "{header:Q}" is absent')
def step_header_absent(context, header):
    assert context.vm.response.headers.get(header) is None, dict(context.vm.response.headers)


@then('the response header "{header:Q}" lists "{values:Q}"')
def step_header_lists(context, header, values):
    got = {v.strip().upper() for v in (context.vm.response.headers.get(header) or "").split(",") if v.strip()}
    want = {v.strip().upper() for v in values.split(",")}
    assert got == want, f"{header}: {got}"


@then('the response media type is "{mtype:Q}"')
def step_media_type(context, mtype):
    assert media_type(context.vm.response) == mtype, show(context.vm.response)


@then('the response Content-Type is exactly "{value:Q}"')
def step_ctype_exact(context, value):
    assert context.vm.response.headers.get("Content-Type") == value, context.vm.response.headers.get("Content-Type")


@then("the response has a Content-Type header")
def step_has_ctype(context):
    response = context.vm.response
    if response.content:
        assert response.headers.get("Content-Type"), dict(response.headers)


@then('the response field "{path:Q}" equals {value}')
def step_field_equals(context, path, value):
    expected = json.loads(x(context, value))
    try:
        got = lookup(body(context), x(context, path))
    except KeyError as exc:
        raise AssertionError(f"field {path} missing in {json.dumps(body(context))[:1500]}") from exc
    assert got == expected, f"{path}: expected {expected!r}, got {got!r}"


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    try:
        got = lookup(body(context), path)
    except KeyError:
        return
    raise AssertionError(f"{path} present: {got!r}")


@then('the response field "{path:Q}" is present')
def step_field_present(context, path):
    try:
        lookup(body(context), path)
    except KeyError as exc:
        raise AssertionError(f"{path} missing in {json.dumps(body(context))[:1500]}") from exc


@then('the response field "{path:Q}" is absent or null')
def step_field_absent_null(context, path):
    try:
        got = lookup(body(context), path)
    except KeyError:
        return
    assert got is None, f"{path}: {got!r}"


@then('the response field "{path:Q}" is a UTC timestamp to the second')
def step_field_timestamp(context, path):
    got = lookup(body(context), path)
    assert isinstance(got, str) and TIMESTAMP.match(got), f"{path}: {got!r}"


@then('the response field "{path:Q}" is the saved "{key:Q}"')
def step_field_saved(context, path, key):
    assert lookup(body(context), path) == context.vm.saved[key], (lookup(body(context), path), context.vm.saved[key])


@then('the response field "{path:Q}" is later than the saved "{key:Q}"')
def step_field_later(context, path, key):
    assert parse_time(lookup(body(context), path)) > parse_time(context.vm.saved[key])


@then('the response field "{path:Q}" is not later than the saved "{key:Q}"')
def step_field_not_later(context, path, key):
    assert parse_time(lookup(body(context), path)) <= parse_time(context.vm.saved[key])


@then('the response field "{path:Q}" is the same as the response field "{other:Q}"')
def step_field_equals_field(context, path, other):
    assert lookup(body(context), path) == lookup(body(context), other)


@then('the response field "{path:Q}" has {count:d} entries')
def step_field_len(context, path, count):
    got = lookup(body(context), path)
    assert isinstance(got, list) and len(got) == count, f"{path}: {len(got) if isinstance(got, list) else got}"


@then('the response has the fields "{names:Q}"')
def step_has_fields(context, names):
    data = body(context)
    for name in (n.strip() for n in names.split(",")):
        try:
            lookup(data, name)
        except KeyError as exc:
            raise AssertionError(f"{name} missing in {json.dumps(data)[:1500]}") from exc


@then('every item has the fields "{names:Q}"')
def step_items_fields(context, names):
    items = body(context)["items"]
    assert items, "no items"
    for item in items:
        for name in (n.strip() for n in names.split(",")):
            assert name in item, f"{name} missing in {item}"


@then('the response items are named "{names:Q}"')
def step_items_named(context, names):
    expected = [x(context, n.strip()) for n in names.split(",") if n.strip()]
    got = [item.get("name") for item in body(context)["items"]]
    assert got == expected, f"expected {expected}, got {got}"


@then('the response item versions are "{versions:Q}"')
def step_items_versions(context, versions):
    expected = [int(v) for v in versions.split(",") if v.strip()]
    got = [item.get("version") for item in body(context)["items"]]
    assert got == expected, f"expected {expected}, got {got}"


@then("the response has {count:d} items")
@then("the response has {count:d} item")
def step_item_count(context, count):
    items = body(context).get("items")
    assert isinstance(items, list) and len(items) == count, f"items: {json.dumps(items)[:1500]}"


@then("the response holds no items")
def step_no_items_field(context):
    response = context.vm.response
    try:
        data = response.json()
    except ValueError:
        return
    assert not (isinstance(data, dict) and data.get("items")), data


@then('the link "{rel:Q}" is "{expected:Q}"')
def step_link(context, rel, expected):
    got = body(context).get(rel)
    assert got is not None, f"link {rel} missing: {body(context)}"
    assert urllib.parse.unquote(got) == urllib.parse.unquote(x(context, expected)), f"{rel}: {got}"


@then('the link "{rel:Q}" is absent')
def step_link_absent(context, rel):
    assert body(context).get(rel) is None, f"{rel}: {body(context).get(rel)}"


@then('the response body is the uploaded file of "{name:Q}" version {version:d}')
def step_body_upload(context, name, version):
    expected = context.vm.uploads[x(context, name)][version]
    assert context.vm.response.content == expected, "downloaded bytes differ from the uploaded file"


@then('the response body is the uploaded file of the external vocabulary "{name:Q}"')
def step_body_ext_upload(context, name):
    assert context.vm.response.content == context.vm.ext_uploads[x(context, name)]


# =========================================================================== validation warnings
@then("the response has no validation warnings")
def step_no_warnings(context):
    data = body(context)
    assert data.get("validationWarnings") == [], f"validationWarnings: {data.get('validationWarnings')!r}"


@then("the response has {count:d} validation warning")
@then("the response has {count:d} validation warnings")
def step_warning_count(context, count):
    warnings = body(context).get("validationWarnings")
    assert isinstance(warnings, list) and len(warnings) == count, warnings


@then('the response has a validation warning "{code:Q}" with location "{location:Q}"')
def step_warning_loc(context, code, location):
    location = x(context, location)
    warnings = body(context).get("validationWarnings") or []
    assert any(w.get("code") == code and w.get("location") == location for w in warnings), warnings


@then('the response has a validation warning "{code:Q}" with description "{description:Q}"')
def step_warning_desc(context, code, description):
    description = x(context, description)
    warnings = body(context).get("validationWarnings") or []
    assert any(w.get("code") == code and w.get("description") == description for w in warnings), warnings


@then('the response has no validation warning "{code:Q}"')
def step_no_warning_code(context, code):
    warnings = body(context).get("validationWarnings")
    assert isinstance(warnings, list), body(context)
    assert not [w for w in warnings if w.get("code") == code], warnings


@then('every validation warning code is one of "{codes:Q}"')
def step_warning_codes(context, codes):
    allowed = {c.strip() for c in codes.split(",")}
    for warning in body(context).get("validationWarnings") or []:
        assert warning.get("code") in allowed, warning


# =========================================================================== state read back (as ga-admin)
@then('{count:d} vocabularies have a name containing "{text:Q}"')
@then('{count:d} vocabulary has a name containing "{text:Q}"')
def step_count_names(context, count, text):
    response = context.vm.admin("GET", "/vocabularies?name=" + x(context, text))
    assert response.status_code == 200, show(response)
    assert response.json()["total"] == count, response.json()


@then('vocabulary "{name:Q}" has {count:d} versions')
@then('vocabulary "{name:Q}" has {count:d} version')
def step_count_versions(context, name, count):
    response = context.vm.admin("GET", f"/vocabularies/{x(context, name)}/versions?pageSize=100")
    assert response.status_code == 200, show(response)
    assert response.json()["total"] == count, response.json()


@then('version {version:d} of vocabulary "{name:Q}" has version status "{status:Q}"')
def step_check_version_status(context, version, name, status):
    response = context.vm.admin("GET", f"/vocabularies/{x(context, name)}/versions/{version}")
    assert response.status_code == 200, show(response)
    assert response.json()["status"] == status, response.json()


@then('vocabulary "{name:Q}" has vocabulary status "{status:Q}"')
def step_check_vocab_status(context, name, status):
    response = context.vm.admin("GET", f"/vocabularies/{x(context, name)}")
    assert response.status_code == 200, show(response)
    assert response.json()["status"] == status, response.json()


@then('vocabulary "{name:Q}" has latest version {version:d}')
def step_check_latest(context, name, version):
    response = context.vm.admin("GET", f"/vocabularies/{x(context, name)}")
    assert response.status_code == 200, show(response)
    assert response.json()["version"] == version, response.json()


@then('version {older:d} of "{name:Q}" was last updated when version {newer:d} was created')
def step_deprecated_at(context, older, name, newer):
    name = x(context, name)
    old = context.vm.admin("GET", f"/vocabularies/{name}/versions/{older}").json()
    new = context.vm.admin("GET", f"/vocabularies/{name}/versions/{newer}").json()
    assert old["lastUpdateDate"] == new["creationDate"], (old, new)


@then('the external vocabulary "{name:Q}" is listed')
def step_ext_listed(context, name):
    assert _ext_position(context, x(context, name)) is not None


@then('the external vocabulary "{name:Q}" is not registered')
def step_ext_not_registered(context, name):
    response = context.vm.admin("GET", f"/external-vocabularies/{x(context, name)}")
    assert response.status_code == 404, show(response)


@then('the external vocabularies "{first:Q}" and "{second:Q}" are listed in this order')
def step_ext_order(context, first, second):
    a = _ext_position(context, x(context, first))
    b = _ext_position(context, x(context, second))
    assert a is not None and b is not None and a < b, (a, b)


def _ext_position(context, name):
    page, position = 1, 0
    while True:
        response = context.vm.admin("GET", f"/external-vocabularies?page={page}&pageSize=100")
        assert response.status_code == 200, show(response)
        data = response.json()
        for item in data["items"]:
            if item.get("name") == name:
                return position
            position += 1
        if page >= data.get("totalPages", 0) or page > 200:
            return None
        page += 1


# =========================================================================== parallel results
@then('all parallel responses have status {code:d}')
def step_parallel_all(context, code):
    codes = [r.status_code for r in context.vm.parallel]
    assert all(c == code for c in codes), [show(r) for r in context.vm.parallel if r.status_code != code]


@then('the parallel responses created versions {first:d} to {last:d} once each')
def step_parallel_versions(context, first, last):
    versions = sorted(r.json()["version"] for r in context.vm.parallel)
    assert versions == list(range(first, last + 1)), versions


@then("the parallel responses have distinct Location headers")
def step_parallel_locations(context):
    locations = [r.headers.get("Location") for r in context.vm.parallel]
    assert None not in locations and len(set(locations)) == len(locations), locations


@then('the parallel responses have the status codes "{codes:Q}"')
def step_parallel_codes(context, codes):
    got = sorted(r.status_code for r in context.vm.parallel)
    want = sorted(int(c) for c in codes.split(","))
    assert got == want, [show(r) for r in context.vm.parallel]


@then('the problem has an issue with detail "{detail:Q}" and the location')
@then('the problem has an issue with detail "{detail:Q}" and the location:')
def step_issue_detail_location_doc(context, detail):
    detail, location = x(context, detail), x(context, context.text.strip())
    assert any(i.get("detail") == detail and i.get("location") == location for i in issues(context)), \
        f"wanted location {location!r} in " + json.dumps(issues(context), indent=1)[:3000]


@then('the response is an attachment named "{filename:Q}"')
def step_attachment(context, filename):
    """D29: Content-Disposition attachment with the decided file name (quoting form is free)."""
    header = context.vm.response.headers.get("Content-Disposition") or ""
    kind, _, params = header.partition(";")
    assert kind.strip().lower() == "attachment", header
    match = re.search(r'filename\s*=\s*"?([^";]+)"?', params)
    assert match and match.group(1) == x(context, filename), header


@then('the link "{rel:Q}" is present')
def step_link_present(context, rel):
    assert body(context).get(rel), f"link {rel} missing: {body(context)}"


@then('the response field "{path:Q}" is absent or false')
def step_field_absent_false(context, path):
    try:
        got = lookup(body(context), path)
    except KeyError:
        return
    assert got is False, f"{path}: {got!r}"


@when('I search external vocabularies with {ns:d} namespace URIs and {al:d} alias URIs')
def step_ext_search_many(context, ns, al):
    base = x(context, "$EXT")
    payload = {}
    if ns:
        payload["namespaceUris"] = [f"{base}/n{i}" for i in range(ns)]
    if al:
        payload["aliasUris"] = [f"{base}/a{i}" for i in range(al)]
    save_response(context, context.vm.request("POST", "/external-vocabulary/searches", json_body=payload))


@when('I register an external vocabulary "{name:Q}" with {count:d} alias URIs')
def step_register_aliases(context, name, count):
    base = x(context, "$EXT")
    parts = [("name", x(context, name)), ("namespaceUri", f"{base}/main")]
    parts += [("aliasUris", f"{base}/alias/{i}") for i in range(count)]
    parts.append(("vocabularyFile", ("e.ttl", turtle.default_file(context.vm.run, "Ext"), "text/turtle")))
    save_response(context, context.vm.request("POST", "/external-vocabularies", files=client.multipart(parts)))


@then('no external vocabulary has a name containing "{text:Q}"')
def step_no_ext_with(context, text):
    """Scans the external list (it has no filter, D41) for a name containing the text, ignoring case."""
    needle = x(context, text).lower()
    page = 1
    while True:
        response = context.vm.admin("GET", f"/external-vocabularies?page={page}&pageSize=100")
        assert response.status_code == 200, show(response)
        data = response.json()
        found = [i["name"] for i in data["items"] if needle in i.get("name", "").lower()]
        assert not found, found
        if page >= data.get("totalPages", 0) or page > 200:
            return
        page += 1


@then('no vocabulary is named exactly "{name:Q}"')
def step_no_exact_name(context, name):
    """Nothing stored under a rejected name that the run-id filter cannot see (SR-09).
    Only names the name filter accepts (D19: ^[A-Za-z0-9]+$, 1 to 64) can be checked this way."""
    value = x(context, name)
    if not re.fullmatch(r"[A-Za-z0-9]{1,64}", value):
        return
    response = context.vm.admin("GET", "/vocabularies?pageSize=100&name=" + value)
    assert response.status_code == 200, show(response)
    page, data = 1, response.json()
    while True:
        assert not [i for i in data["items"] if i.get("name") == value], f"{value} was stored"
        if page >= data.get("totalPages", 0) or page > 50:
            return
        page += 1
        data = context.vm.admin("GET", f"/vocabularies?pageSize=100&page={page}&name={value}").json()


@given('the scenario is skipped unless "{name:Q}" was created on the UTC day "{day:Q}"')
def step_skip_day(context, name, day):
    """Guards against a run that crosses 00:00 UTC between two creations (SR-11)."""
    response = context.vm.admin("GET", f"/vocabularies/{x(context, name)}")
    assert response.status_code == 200, show(response)
    if response.json()["creationDate"][:10] != x(context, day):
        context.scenario.skip("the creations crossed 00:00 UTC; D20 day match cannot be shown")


@then('the external vocabulary "{name:Q}" is registered')
def step_ext_registered_check(context, name):
    response = context.vm.admin("GET", f"/external-vocabularies/{x(context, name)}")
    assert response.status_code == 200, show(response)
