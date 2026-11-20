"""Shared step library for the Vocabulary Manager API suite (black box, HTTP only)."""

import json
import re
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from urllib.parse import parse_qsl, urlsplit

import parse
from behave import given, register_type, then, use_step_matcher, when

from vmclient import (CONFIG, encode_multipart, fetch_token, http, invalid_token,
                      load_fixture, problem_body, turtle_of_size)

use_step_matcher("parse")


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


register_type(Q=_quoted)
ADMIN = object()  # sentinel: act as ga-admin (test data set-up)

TOKEN_RE = re.compile(r"\$\{([A-Za-z][A-Za-z0-9]*)(?::([^}]*))?\}")
UTC_TS = re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$")
_content_base = {}


# --------------------------------------------------------------------------- helpers

def vname(context, alias):
    return context.prefix + alias


def own_ns(context, alias):
    return f"https://example.org/vt/{vname(context, alias)}#"


def admin():
    return fetch_token("ga-admin")


def current_token(context):
    return context.token


def content_base(context):
    if CONFIG.content_base:
        return CONFIG.content_base.rstrip("/")
    if "value" not in _content_base:
        # Run-level probe name, outside every scenario prefix, so no name-filtered count sees it.
        probe = f"Vt{CONFIG.run_id}Probe"
        resp = _create(context, "BaseProbe", render(context, "valid", "BaseProbe"), name=probe,
                       api_url=CONFIG.api_url)
        if resp.status_code == 409:
            resp = http("GET", f"{CONFIG.api_url}/vocabularies/{probe}", token=admin())
            assert resp.status_code == 200, f"probe vocabulary not readable: {resp.status_code} {resp.text[:300]}"
            suffix = f"/content/{probe}"
        else:
            assert resp.status_code == 201, f"probe vocabulary not created: {resp.status_code} {resp.text[:300]}"
            suffix = f"/content/{probe}/versions/1"
        href = resp.json()["contentHref"]
        assert href.endswith(suffix), f"unexpected contentHref {href}"
        _content_base["value"] = href[: -len(suffix)]
    return _content_base["value"]


def sub(context, text):
    if text is None:
        return None

    def repl(m):
        key, arg = m.group(1), m.group(2)
        if key == "base":
            return content_base(context)
        if key == "run":
            return CONFIG.run_id
        if key == "prefix":
            return context.prefix
        if key == "email":
            return CONFIG.emails[arg]
        if key == "ns":
            return own_ns(context, arg)
        if key == "extns":
            return f"https://ext.example.org/vt/{context.prefix}/{arg}"
        if key == "lower":
            return vname(context, arg).lower()
        if key == "upper":
            return vname(context, arg).upper()
        if key == "long":
            n = int(arg)
            return context.prefix + "L" * (n - len(context.prefix))
        if key == "text":
            return "d" * int(arg)
        if key == "var":
            return sub(context, context.vars[arg])
        if key == "saved":
            return str(context.saved[arg])
        if key[0].isupper() and arg is None:
            return vname(context, key)
        raise AssertionError(f"unknown placeholder {m.group(0)}")

    return TOKEN_RE.sub(repl, text)


def render(context, fixture, alias, extra=None):
    raw = load_fixture(fixture)
    if not raw:
        return b""
    text = raw.decode("utf-8")
    for key, value in (extra or {}).items():
        text = text.replace(key, value)
    text = text.replace("${own}", own_ns(context, alias))
    text = text.replace("${self}", vname(context, alias))
    text = text.replace("${marker}", f"marker {context.prefix} {next(context.marker)}")
    return sub(context, text).encode("utf-8")


def url(context, path):
    return context.api_url + sub(context, path)


def send(context, method, path, body=None, content_type=None, headers=None, token="current"):
    tok = current_token(context) if token == "current" else token
    # Content operations answer text/turtle; every other operation answers JSON (D63).
    hdrs = {"Accept": "text/turtle" if "/content" in path.split("?")[0] else "application/json"}
    hdrs.update(context.extra_headers)
    if headers:
        hdrs.update(headers)
    resp = http(method, url(context, path), token=tok, headers=hdrs, body=body, content_type=content_type)
    context.resp = resp
    return resp


def file_part(data, name="vocabularyFile"):
    return {"name": name, "data": data, "filename": "vocabulary.ttl", "content_type": "text/turtle"}


def text_part(name, value):
    return {"name": name, "data": value}


def _tok(token):
    return admin() if token is ADMIN else token


def _create(context, alias, data, description=None, changelog="Initial version.", token=ADMIN, name=None,
            api_url=None):
    name = name or vname(context, alias)
    description = description if description is not None else f"Description of {alias}"
    parts = [file_part(data), text_part("name", name), text_part("description", description),
             text_part("changelog", changelog)]
    body, ctype = encode_multipart(parts)
    resp = http("POST", (api_url or context.api_url) + "/vocabularies", token=_tok(token), body=body,
                content_type=ctype)
    if resp.status_code == 201:
        context.files.setdefault(alias, {})[1] = data
        context.descriptions.setdefault(alias, {})[1] = description
        context.created.setdefault(alias, {})[1] = resp.json()
    return resp


def _new_version(context, alias, data, description=None, changelog=None, token=ADMIN, extra_parts=None):
    versions = context.files.get(alias, {})
    n = (max(versions) + 1) if versions else 2
    description = description if description is not None else f"Description of {alias} version {n}"
    changelog = changelog if changelog is not None else f"Change {n} of {alias}"
    parts = [file_part(data), text_part("description", description), text_part("changelog", changelog)]
    parts += extra_parts or []
    body, ctype = encode_multipart(parts)
    resp = http("POST", f"{context.api_url}/vocabularies/{vname(context, alias)}/versions",
                token=_tok(token), body=body, content_type=ctype)
    if resp.status_code == 201:
        v = resp.json()["version"]
        context.files.setdefault(alias, {})[v] = data
        context.descriptions.setdefault(alias, {})[v] = description
        context.created.setdefault(alias, {})[v] = resp.json()
    return resp


def body_json(context):
    try:
        return context.resp.json()
    except ValueError:
        raise AssertionError(f"body is not JSON: {context.resp.status_code} {context.resp.text[:300]}")


def field(obj, path):
    cur = obj
    for token in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if token.startswith("["):
            idx = int(token[1:-1])
            if not isinstance(cur, list) or idx >= len(cur):
                raise KeyError(path)
            cur = cur[idx]
        else:
            if not isinstance(cur, dict) or token not in cur:
                raise KeyError(path)
            cur = cur[token]
    return cur


def expect_status(context, code):
    r = context.resp
    assert r.status_code == code, f"expected {code}, got {r.status_code}: {r.text[:500]}"


def admin_get(context, path):
    return http("GET", CONFIG.api_url + sub(context, path), token=admin())


def media_type(resp):
    return (resp.headers.get("Content-Type") or "").split(";")[0].strip().lower()


# --------------------------------------------------------------------------- who calls

@given('I use the token of "{user:Q}"')
def step_token(context, user):
    context.token = fetch_token(user)


@given("I use no token")
def step_no_token(context):
    context.token = None


@given('I use an invalid token of kind "{kind:Q}"')
def step_invalid_token(context, kind):
    context.token = invalid_token(kind)


@given("I use the store-down backend")
def step_store_down(context):
    context.api_url = CONFIG.store_down_api_url


@given('I send the header "{header:Q}" with value "{value:Q}"')
def step_extra_header(context, header, value):
    context.extra_headers[header] = sub(context, value)


@given('the file variable "{var:Q}" is "{value:Q}"')
def step_file_var(context, var, value):
    context.vars[var] = value


@given("I wait {seconds:f} seconds")
@when("I wait {seconds:f} seconds")
def step_wait(context, seconds):
    time.sleep(seconds)


# --------------------------------------------------------------------------- test data (always as ga-admin)

@given('a vocabulary "{alias:Q}" exists')
def step_vocab_exists(context, alias):
    resp = _create(context, alias, render(context, "valid", alias))
    assert resp.status_code == 201, f"setup create failed: {resp.status_code} {resp.text[:500]}"


@given('a vocabulary "{alias:Q}" exists with the file "{fixture:Q}"')
def step_vocab_exists_file(context, alias, fixture):
    resp = _create(context, alias, render(context, fixture, alias))
    assert resp.status_code == 201, f"setup create failed: {resp.status_code} {resp.text[:500]}"


@given('a vocabulary "{alias:Q}" exists with {count:d} versions')
def step_vocab_exists_versions(context, alias, count):
    step_vocab_exists(context, alias)
    for _ in range(count - 1):
        resp = _new_version(context, alias, render(context, "valid", alias))
        assert resp.status_code == 201, f"setup version failed: {resp.status_code} {resp.text[:500]}"


@given('vocabulary "{alias:Q}" has a new version with the file "{fixture:Q}"')
def step_vocab_new_version_file(context, alias, fixture):
    resp = _new_version(context, alias, render(context, fixture, alias))
    assert resp.status_code == 201, f"setup version failed: {resp.status_code} {resp.text[:500]}"


@given('vocabulary "{alias:Q}" has the status "{status:Q}"')
def step_vocab_status(context, alias, status):
    resp = http("PATCH", f"{CONFIG.api_url}/vocabularies/{vname(context, alias)}", token=admin(),
                body=json.dumps({"status": status}), content_type="application/json")
    assert resp.status_code in (200, 409), f"setup status failed: {resp.status_code} {resp.text[:300]}"


@given('version {version:d} of vocabulary "{alias:Q}" has the status "{status:Q}"')
def step_version_status(context, version, alias, status):
    resp = http("PATCH", f"{CONFIG.api_url}/vocabularies/{vname(context, alias)}/versions/{version}",
                token=admin(), body=json.dumps({"status": status}), content_type="application/json")
    assert resp.status_code in (200, 409), f"setup version status failed: {resp.status_code} {resp.text[:300]}"


def _register_external(context, alias, namespace, aliases=(), fixture="external", token=ADMIN,
                       description=None, source=None):
    data = render(context, fixture, alias, {"${extns}": namespace})
    parts = [file_part(data), text_part("name", vname(context, alias)), text_part("namespaceUri", namespace)]
    for a in aliases:
        parts.append(text_part("aliasUris", a))
    if description is not None:
        parts.append(text_part("description", description))
    if source is not None:
        parts.append(text_part("sourceUri", source))
    body, ctype = encode_multipart(parts)
    resp = http("POST", context.api_url + "/external-vocabularies", token=_tok(token), body=body,
                content_type=ctype)
    if resp.status_code == 201:
        context.files.setdefault("ext:" + alias, {})[0] = data
    return resp


@given('an external vocabulary "{alias:Q}" is registered with namespace "{namespace:Q}"')
def step_ext_registered(context, alias, namespace):
    resp = _register_external(context, alias, sub(context, namespace))
    assert resp.status_code == 201, f"setup register failed: {resp.status_code} {resp.text[:500]}"


@given('an external vocabulary "{alias:Q}" is registered with namespace "{namespace:Q}" and aliases "{aliases:Q}"')
def step_ext_registered_aliases(context, alias, namespace, aliases):
    resp = _register_external(context, alias, sub(context, namespace), sub(context, aliases).split())
    assert resp.status_code == 201, f"setup register failed: {resp.status_code} {resp.text[:500]}"


# --------------------------------------------------------------------------- requests

@when('I create the vocabulary "{alias:Q}" with the file "{fixture:Q}"')
def step_create_file(context, alias, fixture):
    context.resp = _create(context, alias, render(context, fixture, alias), token=current_token(context))


@when('I create the vocabulary "{alias:Q}"')
def step_create_default(context, alias):
    context.resp = _create(context, alias, render(context, "valid", alias), token=current_token(context))


@when('I create the vocabulary "{alias:Q}" with a file of {size:d} bytes')
def step_create_size(context, alias, size):
    context.resp = _create(context, alias, turtle_of_size(size, own_ns(context, alias)),
                           token=current_token(context))


@when('I create the vocabulary "{alias:Q}" with the description "{description:Q}"')
def step_create_desc(context, alias, description):
    context.resp = _create(context, alias, render(context, "valid", alias), description=sub(context, description),
                           token=current_token(context))


@when('I create the vocabulary named "{name:Q}"')
def step_create_named(context, name):
    real = sub(context, name)
    context.resp = _create(context, "Named", render(context, "valid", "Named"), name=real,
                           token=current_token(context))


@when('I send a multipart "{method:Q}" to "{path:Q}" with these parts')
def step_multipart(context, method, path):
    parts = []
    for row in context.table:
        name = sub(context, row["name"])
        value = row["value"]
        if value.startswith("@file:"):
            spec = value[len("@file:"):].split(":")
            fixture, alias = spec[0], (spec[1] if len(spec) > 1 else "A")
            parts.append(file_part(render(context, fixture, alias), name))
        elif value.startswith("@size:"):
            spec = value[len("@size:"):].split(":")
            size, alias = int(spec[0]), (spec[1] if len(spec) > 1 else "A")
            parts.append(file_part(turtle_of_size(size, own_ns(context, alias)), name))
        elif value.startswith("@extfile:"):
            ns = sub(context, value[len("@extfile:"):])
            parts.append(file_part(render(context, "external", "A", {"${extns}": ns}), name))
        elif value == "@empty":
            parts.append(text_part(name, ""))
        elif value.startswith("@spaces:"):
            parts.append(text_part(name, " " * int(value[len("@spaces:"):])))
        elif value.startswith("@text:"):
            parts.append(text_part(name, "d" * int(value[len("@text:"):])))
        else:
            parts.append(text_part(name, sub(context, value)))
    body, ctype = encode_multipart(parts)
    send(context, method, path, body=body, content_type=ctype)


@when('I send a multipart "{method:Q}" to "{path:Q}" of exactly {size:d} bytes with a padded description')
def step_multipart_exact(context, method, path, size):
    def build(pad):
        return encode_multipart([file_part(render(context, "valid", "A")), text_part("name", vname(context, "A")),
                                 text_part("description", "d" * pad), text_part("changelog", "Initial version.")])
    body, _ = build(0)
    pad = size - len(body)
    assert pad > 0
    body, ctype = build(pad)
    # The boundary has a fixed length, so the size does not change between the two builds.
    assert len(body) == size, (len(body), size)
    send(context, method, path, body=body, content_type=ctype)


@when('I send a multipart "{method:Q}" to "{path:Q}" with {count:d} extra parts')
def step_multipart_many(context, method, path, count):
    parts = [file_part(render(context, "valid", "A")), text_part("name", vname(context, "A")),
             text_part("description", "d"), text_part("changelog", "c")]
    parts += [text_part("description", "d") for _ in range(count)]
    body, ctype = encode_multipart(parts)
    send(context, method, path, body=body, content_type=ctype)


@when('I create a new version of "{alias:Q}" with the file "{fixture:Q}"')
def step_version_file(context, alias, fixture):
    context.resp = _new_version(context, alias, render(context, fixture, alias), token=current_token(context))


@when('I create a new version of "{alias:Q}"')
def step_version_default(context, alias):
    context.resp = _new_version(context, alias, render(context, "valid", alias), token=current_token(context))


@when('I create a new version of "{alias:Q}" with the description "{description:Q}"')
def step_version_desc(context, alias, description):
    context.resp = _new_version(context, alias, render(context, "valid", alias),
                                description=sub(context, description), token=current_token(context))


@when('I create a new version of "{alias:Q}" with the changelog "{changelog:Q}"')
def step_version_changelog(context, alias, changelog):
    context.resp = _new_version(context, alias, render(context, "valid", alias),
                                changelog=sub(context, changelog), token=current_token(context))


@when('I create a new version of "{alias:Q}" with a file of {size:d} bytes')
def step_version_size(context, alias, size):
    context.resp = _new_version(context, alias, turtle_of_size(size, own_ns(context, alias)),
                                token=current_token(context))


@when('I create a new version of "{alias:Q}" with the file of version {version:d} and the description "{description:Q}"')
def step_version_same_file(context, alias, version, description):
    context.resp = _new_version(context, alias, context.files[alias][version],
                                description=sub(context, description), token=current_token(context))


@when('I create a new version of "{alias:Q}" with the file and description of version {version:d}')
def step_version_identical(context, alias, version):
    context.resp = _new_version(context, alias, context.files[alias][version],
                                description=context.descriptions[alias][version], token=current_token(context))


@when('I create a new version of "{alias:Q}" with the file of version {version:d}, its description and the changelog "{changelog:Q}"')
def step_version_identical_changelog(context, alias, version, changelog):
    context.resp = _new_version(context, alias, context.files[alias][version],
                                description=context.descriptions[alias][version], changelog=changelog,
                                token=current_token(context))


@when('I send "{method:Q}" to "{path:Q}"')
def step_send(context, method, path):
    send(context, method, path)


@when('I send "{method:Q}" to "{path:Q}" with the JSON body')
def step_send_json(context, method, path):
    send(context, method, path, body=sub(context, context.text).encode("utf-8"),
         content_type="application/json")


@when('I send "{method:Q}" to "{path:Q}" with the JSON body padded with spaces to {size:d} bytes')
def step_send_json_padded(context, method, path, size):
    body = sub(context, context.text).encode("utf-8")
    assert len(body) <= size
    body += b" " * (size - len(body))
    send(context, method, path, body=body, content_type="application/json")


@when('I send "{method:Q}" to "{path:Q}" with content type "{ctype:Q}" and the body')
def step_send_ctype(context, method, path, ctype):
    send(context, method, path, body=sub(context, context.text or "").encode("utf-8"), content_type=ctype)


@when('I send "{method:Q}" to "{path:Q}" with the header "{header:Q}" set to "{value:Q}"')
def step_send_header(context, method, path, header, value):
    send(context, method, path, headers={header: sub(context, value)})


@when('I send "{method:Q}" to "{path:Q}" with the header "{header:Q}" set to "{value:Q}" and the JSON body')
def step_send_header_json(context, method, path, header, value):
    send(context, method, path, headers={header: sub(context, value)},
         body=sub(context, context.text).encode("utf-8"), content_type="application/json")


@when('I send "{method:Q}" to "{path:Q}" with a JSON body of {size:d} bytes')
def step_send_big_json(context, method, path, size):
    head = '{"status": "DEPRECATED", "padding": "'
    tail = '"}'
    body = head + "p" * (size - len(head) - len(tail)) + tail
    send(context, method, path, body=body.encode(), content_type="application/json")


@when('I send "{method:Q}" to "{path:Q}" with the "Authorization" header written as "{header_name:Q}"')
def step_send_header_case(context, method, path, header_name):
    hdrs = {header_name: f"Bearer {current_token(context)}"}
    resp = http(method, url(context, path), token=None, headers=hdrs)
    context.resp = resp


@when('I search vocabularies with {count:d} references to "{alias:Q}"')
def step_search_many(context, count, alias):
    items = [{"name": vname(context, alias), "version": i} for i in range(1, count + 1)]
    send(context, "POST", "/vocabularies/searches", body=json.dumps({"items": items}).encode(),
         content_type="application/json")


@when('I search external vocabularies with {ns_count:d} namespace URIs and {alias_count:d} alias URIs')
def step_search_ext_many(context, ns_count, alias_count):
    body = {}
    base = f"https://ext.example.org/vt/{context.prefix}/none"
    if ns_count:
        body["namespaceUris"] = [f"{base}/n{i}" for i in range(ns_count)]
    if alias_count:
        body["aliasUris"] = [f"{base}/a{i}" for i in range(alias_count)]
    send(context, "POST", "/external-vocabulary/searches", body=json.dumps(body).encode(),
         content_type="application/json")


@when('I register the external vocabulary "{alias:Q}" with namespace "{namespace:Q}"')
def step_register(context, alias, namespace):
    context.resp = _register_external(context, alias, sub(context, namespace), token=current_token(context))


@when('I register the external vocabulary "{alias:Q}" with namespace "{namespace:Q}" and aliases "{aliases:Q}"')
def step_register_aliases(context, alias, namespace, aliases):
    context.resp = _register_external(context, alias, sub(context, namespace), sub(context, aliases).split(),
                                      token=current_token(context))


@when('I register the external vocabulary "{alias:Q}" with namespace "{namespace:Q}" and the file "{fixture:Q}"')
def step_register_fixture(context, alias, namespace, fixture):
    context.resp = _register_external(context, alias, sub(context, namespace), fixture=fixture,
                                      token=current_token(context))


@when('I register the external vocabulary "{alias:Q}" with namespace "{namespace:Q}", description "{description:Q}" and source "{source:Q}"')
def step_register_full(context, alias, namespace, description, source):
    context.resp = _register_external(context, alias, sub(context, namespace), description=sub(context, description),
                                      source=sub(context, source), token=current_token(context))


def _parallel(context, fns):
    with ThreadPoolExecutor(max_workers=len(fns)) as pool:
        context.responses = list(pool.map(lambda f: f(), fns))


@when('I send {count:d} new versions of "{alias:Q}" in parallel')
def step_parallel_versions(context, count, alias):
    token = current_token(context)
    datas = [render(context, "valid", alias) for _ in range(count)]

    def make(data, i):
        def run():
            parts = [file_part(data), text_part("description", f"Parallel {i}"),
                     text_part("changelog", f"Parallel change {i}")]
            body, ctype = encode_multipart(parts)
            return http("POST", f"{context.api_url}/vocabularies/{vname(context, alias)}/versions",
                        token=token, body=body, content_type=ctype)
        return run

    _parallel(context, [make(d, i) for i, d in enumerate(datas)])


@when('I create the vocabulary "{alias:Q}" twice in parallel')
def step_parallel_create(context, alias):
    token = current_token(context)
    datas = [render(context, "valid", alias) for _ in range(2)]

    def make(data):
        def run():
            parts = [file_part(data), text_part("name", vname(context, alias)), text_part("description", "Parallel"),
                     text_part("changelog", "Initial version.")]
            body, ctype = encode_multipart(parts)
            return http("POST", f"{context.api_url}/vocabularies", token=token, body=body, content_type=ctype)
        return run

    _parallel(context, [make(d) for d in datas])


@when('I send "{method:Q}" to "{path:Q}" twice in parallel with the JSON body')
def step_parallel_json(context, method, path):
    token = current_token(context)
    body = sub(context, context.text).encode()
    target = url(context, path)

    def run():
        return http(method, target, token=token, body=body, content_type="application/json")

    _parallel(context, [run, run])


@given('I remember the response field "{path:Q}" as "{key:Q}"')
@when('I remember the response field "{path:Q}" as "{key:Q}"')
@then('I remember the response field "{path:Q}" as "{key:Q}"')
def step_remember(context, path, key):
    context.saved[key] = field(body_json(context), path)


@given('I remember the field "{path:Q}" of "{target:Q}" as "{key:Q}"')
@when('I remember the field "{path:Q}" of "{target:Q}" as "{key:Q}"')
def step_remember_of(context, path, target, key):
    resp = admin_get(context, target)
    assert resp.status_code == 200, f"GET {target}: {resp.status_code} {resp.text[:300]}"
    context.saved[key] = field(resp.json(), path)


# --------------------------------------------------------------------------- status and problem checks

@then("the status is {code:d}")
def step_status(context, code):
    expect_status(context, code)


@then('the status is one of "{codes:Q}"')
def step_status_one_of(context, codes):
    allowed = [int(c) for c in codes.replace(" ", "").split(",")]
    assert context.resp.status_code in allowed, \
        f"expected one of {allowed}, got {context.resp.status_code}: {context.resp.text[:300]}"


@then('the status is not one of "{codes:Q}"')
def step_status_not_one_of(context, codes):
    banned = [int(c) for c in codes.replace(" ", "").split(",")]
    assert context.resp.status_code not in banned, \
        f"did not expect {context.resp.status_code}: {context.resp.text[:300]}"


@then('the response is a problem with status {code:d} and type "{ptype:Q}"')
def step_problem(context, code, ptype):
    expect_status(context, code)
    assert media_type(context.resp) == "application/problem+json", \
        f"Content-Type is {context.resp.headers.get('Content-Type')}"
    body = problem_body(context.resp)
    assert body.get("status") == code, f"problem status {body.get('status')}"
    assert body.get("type") == ptype, f"problem type {body.get('type')!r}, expected {ptype!r}"


@then('the response is a problem with status {code:d}')
def step_problem_any_type(context, code):
    expect_status(context, code)
    assert media_type(context.resp) == "application/problem+json", \
        f"Content-Type is {context.resp.headers.get('Content-Type')}"
    body = problem_body(context.resp)
    assert body.get("status") == code, f"problem status {body.get('status')}"


@then('the problem title is "{title:Q}"')
def step_problem_title(context, title):
    assert body_json(context).get("title") == title, body_json(context).get("title")


@then('the problem detail is "{detail:Q}"')
def step_problem_detail(context, detail):
    exp = sub(context, detail)
    got = body_json(context).get("detail")
    assert got == exp, f"detail {got!r}, expected {exp!r}"


@then('the problem detail starts with "{prefix:Q}"')
def step_problem_detail_starts(context, prefix):
    got = body_json(context).get("detail") or ""
    assert got.startswith(sub(context, prefix)), f"detail {got!r}"


def issues(context):
    return body_json(context).get("issues") or []


@then("the problem has exactly {count:d} issues")
@then("the problem has exactly {count:d} issue")
def step_issue_count(context, count):
    got = issues(context)
    assert len(got) == count, f"expected {count} issues, got {len(got)}: {json.dumps(got)[:800]}"


@then("the problem has no issues")
def step_no_issues(context):
    got = body_json(context).get("issues")
    assert not got, f"expected no issues, got {got}"


@then('the problem has an issue in "{where:Q}"')
def step_issue_in(context, where):
    assert [i for i in issues(context) if i.get("in") == where], \
        f"no issue in {where}: {json.dumps(issues(context))[:800]}"


@then('the problem has an issue in "{where:Q}" named "{name:Q}"')
def step_issue_named(context, where, name):
    name = sub(context, name)
    found = [i for i in issues(context) if i.get("in") == where and i.get("name") == name]
    assert found, f"no issue in {where} named {name!r}: {json.dumps(issues(context))[:800]}"


@then('every issue is in "{where:Q}" named "{name:Q}"')
def step_every_issue_named(context, where, name):
    name = sub(context, name)
    got = issues(context)
    assert got, "no issues"
    for i in got:
        assert i.get("in") == where and i.get("name") == name, f"issue {i}"


@then('every issue has the type "{itype:Q}"')
def step_every_issue_type(context, itype):
    got = issues(context)
    assert got, "no issues"
    for i in got:
        assert i.get("type") == itype, f"issue type {i.get('type')!r}"


@then('the problem has an issue with the detail "{detail:Q}" and the location "{location:Q}"')
def step_issue_detail_location(context, detail, location):
    d, loc = sub(context, detail), sub(context, location)
    found = [i for i in issues(context) if i.get("detail") == d and i.get("location") == loc]
    assert found, f"no issue with detail {d!r} and location {loc!r}: {json.dumps(issues(context))[:1200]}"


@then('the problem has an issue with the detail "{detail:Q}" and a location containing "{parts:Q}"')
def step_issue_detail_location_contains(context, detail, parts):
    d = sub(context, detail)
    needles = [sub(context, p.strip()) for p in parts.split("|")]
    found = [i for i in issues(context)
             if i.get("detail") == d and all(n in (i.get("location") or "") for n in needles)]
    assert found, f"no issue with detail {d!r} and location containing {needles}: {json.dumps(issues(context))[:1200]}"


@then('the problem has an issue with a detail starting with "{prefix:Q}" and the location matching "{pattern:Q}"')
def step_issue_prefix_location_rx(context, prefix, pattern):
    rx = re.compile(pattern)
    found = [i for i in issues(context)
             if (i.get("detail") or "").startswith(prefix) and rx.search(i.get("location") or "")]
    assert found, f"no issue starting {prefix!r} with location /{pattern}/: {json.dumps(issues(context))[:800]}"


@then('the problem has an issue with the detail "{detail:Q}"')
def step_issue_detail(context, detail):
    d = sub(context, detail)
    assert [i for i in issues(context) if i.get("detail") == d], \
        f"no issue with detail {d!r}: {json.dumps(issues(context))[:800]}"


@then('no issue has a detail starting with "{prefix:Q}"')
def step_no_issue_prefix(context, prefix):
    for i in issues(context):
        assert not (i.get("detail") or "").startswith(prefix), f"unexpected issue {i}"


# --------------------------------------------------------------------------- headers and media types

@then('the header "{header:Q}" is "{value:Q}"')
def step_header_is(context, header, value):
    got = context.resp.headers.get(header)
    assert got == sub(context, value), f"{header}: {got!r}, expected {sub(context, value)!r}"


@then('the header "{header:Q}" is present')
def step_header_present(context, header):
    assert context.resp.headers.get(header), f"{header} missing; headers {dict(context.resp.headers)}"


@then('the header "{header:Q}" names the file "{filename:Q}"')
def step_header_filename(context, header, filename):
    got = context.resp.headers.get(header) or ""
    exp = sub(context, filename)
    assert got.lower().startswith("attachment"), f"{header}: {got!r}"
    m = re.search(r'filename="?([^";]+)"?', got)
    assert m and m.group(1) == exp, f"{header}: {got!r}, expected filename {exp!r}"


@then('the content type is exactly "{ctype:Q}"')
def step_ctype_exact(context, ctype):
    got = context.resp.headers.get("Content-Type")
    assert got == ctype, f"Content-Type {got!r}, expected exactly {ctype!r}"


@then('the media type is "{mtype:Q}"')
def step_media_type(context, mtype):
    assert media_type(context.resp) == mtype, f"Content-Type {context.resp.headers.get('Content-Type')!r}"


# --------------------------------------------------------------------------- JSON fields

@then('the response field "{path:Q}" equals {value}')
def step_field_equals(context, path, value):
    exp = json.loads(sub(context, value))
    got = field(body_json(context), sub(context, path))
    assert got == exp, f"{path}: {got!r}, expected {exp!r}"


@then('the response field "{path:Q}" is "{value:Q}"')
def step_field_is(context, path, value):
    exp = sub(context, value)
    got = field(body_json(context), sub(context, path))
    assert got == exp, f"{path}: {got!r}, expected {exp!r}"


@then('the response field "{path:Q}" matches "{pattern:Q}"')
def step_field_matches(context, path, pattern):
    got = field(body_json(context), path)
    assert isinstance(got, str) and re.search(pattern, got), f"{path}: {got!r} does not match /{pattern}/"


@then('the response field "{path:Q}" is a UTC timestamp')
def step_field_ts(context, path):
    got = field(body_json(context), path)
    assert isinstance(got, str) and UTC_TS.match(got), f"{path}: {got!r} is not YYYY-MM-DDTHH:MM:SSZ"


@then('the response field "{path:Q}" is absent')
def step_field_absent(context, path):
    try:
        got = field(body_json(context), path)
    except KeyError:
        return
    raise AssertionError(f"{path} present: {got!r}")


@then('the response field "{path:Q}" is absent or empty')
def step_field_absent_or_empty(context, path):
    try:
        got = field(body_json(context), path)
    except KeyError:
        return
    assert got in (None, [], ""), f"{path} not empty: {got!r}"


@then('the response field "{path:Q}" is the remembered "{key:Q}"')
def step_field_equals_saved(context, path, key):
    got = field(body_json(context), path)
    assert got == context.saved[key], f"{path}: {got!r}, remembered {context.saved[key]!r}"


@then('the response field "{path:Q}" is later than the remembered "{key:Q}"')
def step_field_later(context, path, key):
    got = field(body_json(context), path)
    assert got > context.saved[key], f"{path}: {got!r} not later than {context.saved[key]!r}"


@then('the response field "{path:Q}" is not earlier than the remembered "{key:Q}"')
def step_field_not_earlier(context, path, key):
    got = field(body_json(context), path)
    assert got >= context.saved[key], f"{path}: {got!r} earlier than {context.saved[key]!r}"


@then('the response has no field "{name:Q}"')
def step_no_field(context, name):
    assert name not in body_json(context), f"field {name} present"


# --------------------------------------------------------------------------- validation warnings

def warnings(context):
    body = body_json(context)
    assert "validationWarnings" in body, "validationWarnings missing from the 201 body (D10)"
    assert isinstance(body["validationWarnings"], list)
    return body["validationWarnings"]


@then("the response has no validation warnings")
def step_no_warnings(context):
    got = warnings(context)
    assert got == [], f"expected an empty validationWarnings array, got {got}"


@then("the response has exactly {count:d} validation warnings")
@then("the response has exactly {count:d} validation warning")
def step_warning_count(context, count):
    got = warnings(context)
    assert len(got) == count, f"expected {count} warnings, got {got}"


@then('there is a validation warning with the code "{code:Q}" and the location "{location:Q}"')
def step_warning(context, code, location):
    loc = sub(context, location)
    got = warnings(context)
    assert [w for w in got if w.get("code") == code and w.get("location") == loc], \
        f"no warning {code} at {loc!r}: {got}"


@then('there is a validation warning with the code "{code:Q}", the description "{description:Q}" and the location "{location:Q}"')
def step_warning_full(context, code, description, location):
    loc, desc = sub(context, location), sub(context, description)
    got = warnings(context)
    assert [w for w in got if w.get("code") == code and w.get("location") == loc
            and w.get("description") == desc], f"no warning {code} {desc!r} at {loc!r}: {got}"


@then('there is no validation warning with the code "{code:Q}"')
def step_no_warning_code(context, code):
    got = warnings(context)
    assert not [w for w in got if w.get("code") == code], f"unexpected {code}: {got}"


@then('every validation warning code is one of "{codes:Q}"')
def step_warning_codes(context, codes):
    allowed = [c.strip() for c in codes.split(",")]
    for w in warnings(context):
        assert w.get("code") in allowed, f"warning code {w.get('code')!r} not in {allowed}"


# --------------------------------------------------------------------------- store state, read through safe reads

@then('the vocabulary list filtered by name "{text:Q}" has {count:d} items')
@then('the vocabulary list filtered by name "{text:Q}" has {count:d} item')
def step_list_filter_count(context, text, count):
    resp = admin_get(context, f"/vocabularies?name={sub(context, text)}&pageSize=100")
    assert resp.status_code == 200, f"list: {resp.status_code} {resp.text[:300]}"
    body = resp.json()
    assert body["total"] == count and len(body["items"]) == count, \
        f"expected {count}, got total {body['total']}: {[i['name'] for i in body['items']]}"


@then('vocabulary "{alias:Q}" has {count:d} versions')
@then('vocabulary "{alias:Q}" has {count:d} version')
def step_version_count(context, alias, count):
    resp = admin_get(context, f"/vocabularies/{vname(context, alias)}/versions?pageSize=100")
    assert resp.status_code == 200, f"versions: {resp.status_code} {resp.text[:300]}"
    body = resp.json()
    assert body["total"] == count, f"expected {count} versions, got {body['total']}"
    assert sorted(i["version"] for i in body["items"]) == list(range(1, count + 1)), body["items"]


@then('version {version:d} of vocabulary "{alias:Q}" has the status "{status:Q}"')
def step_check_version_status(context, version, alias, status):
    resp = admin_get(context, f"/vocabularies/{vname(context, alias)}/versions/{version}")
    assert resp.status_code == 200, f"version: {resp.status_code} {resp.text[:300]}"
    assert resp.json()["status"] == status, f"version {version} status {resp.json()['status']}"


@then('vocabulary "{alias:Q}" has the status "{status:Q}"')
def step_check_vocab_status(context, alias, status):
    resp = admin_get(context, f"/vocabularies/{vname(context, alias)}")
    assert resp.status_code == 200, f"vocabulary: {resp.status_code} {resp.text[:300]}"
    assert resp.json()["status"] == status, f"vocabulary status {resp.json()['status']}"


@then('vocabulary "{alias:Q}" has the latest version {version:d}')
def step_check_latest(context, alias, version):
    resp = admin_get(context, f"/vocabularies/{vname(context, alias)}")
    assert resp.status_code == 200, f"vocabulary: {resp.status_code} {resp.text[:300]}"
    assert resp.json()["version"] == version, f"latest version {resp.json()['version']}"


@then('the field "{path:Q}" of "{target:Q}" equals the field "{path2:Q}" of "{target2:Q}"')
def step_cross_field(context, path, target, path2, target2):
    a = admin_get(context, target)
    b = admin_get(context, target2)
    assert a.status_code == 200 and b.status_code == 200, (a.status_code, b.status_code)
    assert field(a.json(), path) == field(b.json(), path2), \
        f"{target} {path}={field(a.json(), path)!r} vs {target2} {path2}={field(b.json(), path2)!r}"


@then('the field "{path:Q}" of "{target:Q}" is "{value:Q}"')
def step_target_field(context, path, target, value):
    resp = admin_get(context, target)
    assert resp.status_code == 200, f"GET {target}: {resp.status_code} {resp.text[:300]}"
    got = field(resp.json(), path)
    assert got == sub(context, value), f"{target} {path}: {got!r}"


@then('"{target:Q}" answers {code:d}')
def step_target_answers(context, target, code):
    resp = admin_get(context, target)
    assert resp.status_code == code, f"GET {target}: {resp.status_code} {resp.text[:300]}"


# --------------------------------------------------------------------------- content

@then('the body is the file of version {version:d} of "{alias:Q}"')
def step_body_is_file(context, version, alias):
    exp = context.files[alias][version]
    assert context.resp.content == exp, \
        f"body differs from the uploaded bytes ({len(context.resp.content)} vs {len(exp)} bytes)"


@then('the body is the file of the external vocabulary "{alias:Q}"')
def step_body_is_ext_file(context, alias):
    exp = context.files["ext:" + alias][0]
    assert context.resp.content == exp, "body differs from the registered bytes"


# --------------------------------------------------------------------------- collections

def items(context):
    return body_json(context)["items"]


@then("the response has {count:d} items")
@then("the response has {count:d} item")
def step_items_count(context, count):
    got = items(context)
    assert len(got) == count, f"expected {count} items, got {len(got)}: {[i.get('name') for i in got]}"


@then('the item names are "{aliases:Q}"')
def step_item_names(context, aliases):
    exp = [vname(context, a.strip()) for a in aliases.split(",") if a.strip()]
    got = [i["name"] for i in items(context)]
    assert got == exp, f"names {got}, expected {exp}"


@then('the item versions are "{versions:Q}"')
def step_item_versions(context, versions):
    exp = [int(v) for v in versions.split(",")]
    got = [i["version"] for i in items(context)]
    assert got == exp, f"versions {got}, expected {exp}"


@then('the search results are "{spec:Q}"')
def step_search_results(context, spec):
    exp = []
    for s in [x.strip() for x in spec.split(",") if x.strip()]:
        if "@" in s:
            a, v = s.split("@")
            exp.append((vname(context, a), False, int(v)))
        else:
            exp.append((vname(context, s), True, None))
    got = items(context)
    assert len(got) == len(exp), f"expected {len(exp)} results, got {got}"
    for g, (name, latest, version) in zip(got, exp):
        assert g["name"] == name and g["latest"] is latest, f"result {g}, expected {name} latest={latest}"
        if version is not None:
            assert g["version"] == version, f"result {g}, expected version {version}"


LIST_KEYS = {"name": "name", "status": "status", "creationDate": "creationDate", "lastUpdated": "lastUpdateDate"}


def _check_order(got, key, desc, tie_key, tie_desc):
    exp = sorted(got, key=lambda i: i[tie_key], reverse=tie_desc)
    exp = sorted(exp, key=lambda i: i[key], reverse=desc)
    assert [i[tie_key] for i in got] == [i[tie_key] for i in exp], \
        f"order {[(i[key], i[tie_key]) for i in got]}, expected {[(i[key], i[tie_key]) for i in exp]}"


@then('the vocabulary items are sorted by "{sort:Q}"')
def step_vocab_sorted(context, sort):
    desc = sort.startswith("-")
    key = LIST_KEYS[sort.lstrip("-")]
    _check_order(items(context), key, desc, "name", False)


@then('the version items are sorted by "{sort:Q}"')
def step_version_sorted(context, sort):
    desc = sort.startswith("-")
    key = sort.lstrip("-")
    key = {"version": "version"}.get(key, key)
    if key == "version":
        _check_order(items(context), "version", desc, "version", desc)
    else:
        _check_order(items(context), key, desc, "version", True)


@then('the pages of "{path:Q}" with page size {size:d} together give the full sorted list')
def step_pages_concat(context, path, size):
    full = admin_get(context, f"{path}&pageSize=100").json()["items"]
    collected = []
    page = 1
    while True:
        body = admin_get(context, f"{path}&pageSize={size}&page={page}").json()
        assert len(body["items"]) <= size
        collected += body["items"]
        if not body["items"] or page >= body["totalPages"]:
            break
        page += 1
    assert [i["name"] for i in collected] == [i["name"] for i in full], \
        f"pages {[i['name'] for i in collected]} vs full {[i['name'] for i in full]}"


def link(context, rel):
    body = body_json(context)
    value = body.get(rel)
    assert value, f"link {rel} missing: {json.dumps(body)[:400]}"
    return value


@then('the link "{rel:Q}" is absent')
def step_link_absent(context, rel):
    assert not body_json(context).get(rel), f"link {rel} present: {body_json(context).get(rel)}"


@then('the link "{rel:Q}" is present')
def step_link_present(context, rel):
    link(context, rel)


@then('the link "{rel:Q}" has the path "{path:Q}"')
def step_link_path(context, rel, path):
    got = urlsplit(link(context, rel)).path
    assert got.endswith(sub(context, path)), f"link {rel} path {got!r}"


@then('the link "{rel:Q}" has the query parameter "{param:Q}" with value "{value:Q}"')
def step_link_param(context, rel, param, value):
    q = parse_qsl(urlsplit(link(context, rel)).query, keep_blank_values=True)
    vals = [v for k, v in q if k == param]
    assert vals == [sub(context, value)], f"link {rel}: {param}={vals}, expected {sub(context, value)!r}"


@then('the link "{rel:Q}" starts with the parameters page and pageSize')
def step_link_param_order(context, rel):
    q = parse_qsl(urlsplit(link(context, rel)).query, keep_blank_values=True)
    assert [k for k, _ in q[:2]] == ["page", "pageSize"], f"link {rel} parameters {q}"


@then('the response has no paging links')
def step_no_links(context):
    body = body_json(context)
    for rel in ("self", "first", "prev", "next", "last"):
        assert not body.get(rel), f"unexpected link {rel}: {body.get(rel)}"


@then('the external vocabulary list contains "{alias:Q}"')
def step_ext_list_contains(context, alias):
    name = vname(context, alias)
    page = 1
    while True:
        resp = admin_get(context, f"/external-vocabularies?page={page}&pageSize=100")
        assert resp.status_code == 200, resp.text[:300]
        body = resp.json()
        if any(i["name"] == name for i in body["items"]):
            return
        if page >= body["totalPages"]:
            raise AssertionError(f"{name} not in the external vocabulary list")
        page += 1


@then('the external vocabulary list does not contain "{name:Q}"')
def step_ext_list_not_contains(context, name):
    name = sub(context, name).lower()
    page = 1
    while True:
        resp = admin_get(context, f"/external-vocabularies?page={page}&pageSize=100")
        assert resp.status_code == 200, resp.text[:300]
        body = resp.json()
        found = [i["name"] for i in body["items"] if i["name"].lower() == name]
        assert not found, f"external vocabulary {found} is stored"
        if page >= body["totalPages"]:
            return
        page += 1


@then("the items are sorted by name")
def step_items_by_name(context):
    names = [i["name"] for i in items(context)]
    assert names == sorted(names), f"not in name order: {names}"


# --------------------------------------------------------------------------- parallel results

@then('the parallel responses have the statuses "{codes:Q}"')
def step_parallel_statuses(context, codes):
    exp = sorted(int(c) for c in codes.replace(" ", "").split(","))
    got = sorted(r.status_code for r in context.responses)
    assert got == exp, f"statuses {got}, expected {exp}: {[r.text[:200] for r in context.responses]}"


@then('the parallel responses got the versions {first:d} to {last:d} once each')
def step_parallel_versions_once(context, first, last):
    got = sorted(r.json()["version"] for r in context.responses if r.status_code == 201)
    assert got == list(range(first, last + 1)), f"versions {got}"
    locs = [r.headers.get("Location") for r in context.responses]
    assert len(set(locs)) == len(locs) and all(locs), f"Location headers {locs}"


@then('the 409 response has the type "{ptype:Q}"')
def step_parallel_409(context, ptype):
    conflict = [r for r in context.responses if r.status_code == 409]
    assert conflict and conflict[0].json().get("type") == ptype, [r.text[:200] for r in context.responses]


# --------------------------------------------------------------------------- dates

@then('the vocabulary list filtered by "{param:Q}" with the {part} of the remembered "{key:Q}" contains "{alias:Q}"')
def step_date_filter_contains(context, param, part, key, alias):
    value = context.saved[key]
    if part == "date":
        value = value[:10]
    elif part == "date-time without offset":
        value = value.rstrip("Z")
    resp = admin_get(context, f"/vocabularies?name={vname(context, alias)}&{param}={value}")
    assert resp.status_code == 200, f"{resp.status_code} {resp.text[:300]}"
    assert [i["name"] for i in resp.json()["items"]] == [vname(context, alias)], resp.json()


@then('today in UTC is remembered as "{key:Q}"')
def step_today(context, key):
    context.saved[key] = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


# Behave runs every step module in its own namespace; publish the helpers for ui_steps.py.
import vmclient as _vmclient  # noqa: E402

_vmclient.API_HELPERS = globals()
