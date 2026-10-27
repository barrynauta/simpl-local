"""Black-box API steps. They know only the OpenAPI contract and spec-digest.md."""
import json
import os
import re

import requests
import parse
from behave import given, register_type, then, when


@parse.with_pattern(r'[^"]*')
def _quoted(text):
    return text


@parse.with_pattern(r"[A-Z]+")
def _word(text):
    return text


register_type(Q=_quoted, W=_word)


# ---------- helpers ----------

def name(context, alias):
    """Map an alias like {Alpha} or Alpha to this scenario's unique vocabulary name."""
    alias = alias.strip("{}")
    if alias not in context.names:
        context.names[alias] = alias + context.run_id
    return context.names[alias]


def sub(context, text):
    """Replace {Alias} placeholders, {RUN} and {BASE} in paths, bodies and expectations."""
    def repl(m):
        key = m.group(1)
        if key == "RUN":
            return context.run_id
        if key == "BASE":
            return context.cfg["base_iri"]
        return name(context, key)
    return re.sub(r"\{([A-Za-z][A-Za-z0-9]*)\}", repl, text)


def headers(context, persona=None, accept="application/json"):
    h = {"Accept": accept}
    persona = context.persona if persona is None else persona
    if persona:
        h["Authorization"] = "Bearer " + context.token(persona)
    return h


def fixture(context, filename, vocab_name, **extra):
    with open(os.path.join(context.fixtures, filename), encoding="utf-8") as f:
        text = f.read()
    text = text.replace("{BASE}", context.cfg["base_iri"]).replace("{NAME}", vocab_name)
    for k, v in extra.items():
        text = text.replace("{" + k + "}", v)
    return text


def url(context, path):
    return context.cfg["api"] + sub(context, path)


def upload(context, path, fields, file_text, filename="vocabulary.ttl", content_type="text/turtle", persona=None):
    # Always multipart/form-data, also when the file is deliberately left out.
    files = [(k, (None, v if isinstance(v, str) else json.dumps(v))) for k, v in fields.items()]
    if file_text is not None:
        files.append(("vocabularyFile", (filename, file_text.encode("utf-8"), content_type)))
    context.resp = requests.post(url(context, path), files=files,
                                 headers=headers(context, persona), timeout=60)
    return context.resp


def create(context, alias, filename="valid_v1.ttl", persona=None, **fields):
    n = name(context, alias)
    data = {"name": n, "description": f"Test vocabulary {n}", "changelog": "Initial version"}
    data.update(fields)
    return upload(context, "/vocabularies", data, fixture(context, filename, n), persona=persona)


def new_version(context, alias, filename="valid_v2.ttl", persona=None, **fields):
    n = name(context, alias)
    data = {"description": f"Test vocabulary {n}", "changelog": "Next version"}
    data.update(fields)
    return upload(context, f"/vocabularies/{n}/versions", data, fixture(context, filename, n), persona=persona)


def json_at(body, path):
    cur = body
    for part in path.split("."):
        if isinstance(cur, list):
            cur = cur[int(part)]
        else:
            cur = cur[part]
    return cur


def body(context):
    try:
        return context.resp.json()
    except ValueError:
        raise AssertionError(f"Response is not JSON ({context.resp.status_code}): {context.resp.text[:300]}")


def show(context):
    return f"HTTP {context.resp.status_code} {context.resp.headers.get('Content-Type')}: {context.resp.text[:400]}"


def admin_get(context, path):
    return requests.get(url(context, path), headers=headers(context, "admin"), timeout=30)


# ---------- given ----------

@given('I am authenticated as "{persona:Q}"')
def step_persona(context, persona):
    context.persona = persona


@given("I am not authenticated")
def step_anonymous(context):
    context.persona = None


@given('a vocabulary "{alias:Q}" exists')
def step_vocab_exists(context, alias):
    r = create(context, alias, persona="admin")
    assert r.status_code == 201, "setup failed: " + show(context)


@given('a vocabulary "{alias:Q}" exists from "{filename:Q}"')
def step_vocab_exists_from(context, alias, filename):
    r = create(context, alias, filename, persona="admin")
    assert r.status_code == 201, "setup failed: " + show(context)


@given('vocabulary "{alias:Q}" has {count:d} versions')
def step_vocab_versions(context, alias, count):
    r = create(context, alias, persona="admin")
    assert r.status_code == 201, "setup failed: " + show(context)
    for _ in range(count - 1):
        r = new_version(context, alias, persona="admin")
        assert r.status_code == 201, "setup failed: " + show(context)


@given('vocabulary "{alias:Q}" is deprecated')
def step_vocab_deprecated(context, alias):
    r = requests.patch(url(context, "/vocabularies/{" + alias.strip("{}") + "}"), json={"status": "DEPRECATED"},
                       headers=headers(context, "admin"), timeout=30)
    assert r.status_code == 200, f"setup failed: HTTP {r.status_code} {r.text[:300]}"


# ---------- when ----------

@when('I create vocabulary "{alias:Q}" from "{filename:Q}"')
def step_create(context, alias, filename):
    create(context, alias, filename)


@when('I create vocabulary "{alias:Q}" from "{filename:Q}" without the "{field:Q}" field')
def step_create_without(context, alias, filename, field):
    n = name(context, alias)
    data = {"name": n, "description": "Test", "changelog": "Initial version"}
    data.pop(field, None)
    upload(context, "/vocabularies", data, None if field == "vocabularyFile" else fixture(context, filename, n))


@when('I create a vocabulary named "{raw_name:Q}" from "{filename:Q}"')
def step_create_raw_name(context, raw_name, filename):
    data = {"name": raw_name, "description": "Test", "changelog": "Initial version"}
    upload(context, "/vocabularies", data, fixture(context, filename, raw_name))


@when('I upload "{filename:Q}" as vocabulary "{alias:Q}" with content type "{ctype:Q}"')
def step_create_ctype(context, filename, alias, ctype):
    n = name(context, alias)
    data = {"name": n, "description": "Test", "changelog": "Initial version"}
    upload(context, "/vocabularies", data, fixture(context, filename, n), filename=filename, content_type=ctype)


@when('I create vocabulary "{alias:Q}" referencing term "{term:Q}" of "{ref_alias:Q}" version {version:d}')
def step_create_ref_version(context, alias, term, ref_alias, version):
    n = name(context, alias)
    ns = f"{context.cfg['base_iri']}/{name(context, ref_alias)}/{version}#"
    data = {"name": n, "description": "Test", "changelog": "Initial version"}
    upload(context, "/vocabularies", data, fixture(context, "references.ttl", n, REF_NS=ns, REF_TERM=term))


@when('I create vocabulary "{alias:Q}" referencing term "{term:Q}" of the latest "{ref_alias:Q}"')
def step_create_ref_latest(context, alias, term, ref_alias):
    n = name(context, alias)
    ns = f"{context.cfg['base_iri']}/{name(context, ref_alias)}#"
    data = {"name": n, "description": "Test", "changelog": "Initial version"}
    upload(context, "/vocabularies", data, fixture(context, "references.ttl", n, REF_NS=ns, REF_TERM=term))


@when('I create a new version of "{alias:Q}" from "{filename:Q}"')
def step_new_version(context, alias, filename):
    new_version(context, alias, filename)


@when('I create a new version of "{alias:Q}" from "{filename:Q}" with form field "{field:Q}" set to "{value:Q}"')
def step_new_version_extra(context, alias, filename, field, value):
    new_version(context, alias, filename, **{field: value})


@when('I send GET "{path:Q}"')
def step_get(context, path):
    context.resp = requests.get(url(context, path), headers=headers(context), timeout=30)


@when('I send GET "{path:Q}" accepting "{accept:Q}"')
def step_get_accept(context, path, accept):
    context.resp = requests.get(url(context, path), headers=headers(context, accept=accept), timeout=30)


@when('I send {method:W} "{path:Q}" with JSON')
def step_send_json(context, method, path):
    payload = json.loads(sub(context, context.text))
    context.resp = requests.request(method, url(context, path), json=payload, headers=headers(context), timeout=30)


@when('I send {method:W} "{path:Q}" with JSON accepting "{accept:Q}"')
def step_send_json_accept(context, method, path, accept):
    payload = json.loads(sub(context, context.text))
    context.resp = requests.request(method, url(context, path), json=payload,
                                    headers=headers(context, accept=accept), timeout=30)


# ---------- then ----------

@then("the response status is {code:d}")
def step_status(context, code):
    assert context.resp.status_code == code, f"expected {code}, got " + show(context)


@then("the response status is one of {codes}")
def step_status_one_of(context, codes):
    allowed = [int(c) for c in re.findall(r"\d+", codes)]
    assert context.resp.status_code in allowed, f"expected one of {allowed}, got " + show(context)


@then("the response is a problem with status {code:d}")
def step_problem(context, code):
    assert context.resp.status_code == code, f"expected {code}, got " + show(context)
    ctype = context.resp.headers.get("Content-Type", "")
    assert ctype.startswith("application/problem+json"), "expected application/problem+json, got " + show(context)
    b = body(context)
    assert b.get("status") in (None, code), f"problem status field {b.get('status')} != {code}"


@then('the response content type is "{ctype:Q}"')
def step_ctype(context, ctype):
    got = context.resp.headers.get("Content-Type", "")
    assert got.startswith(ctype), f"expected {ctype}, got " + show(context)


@then('the response JSON at "{path:Q}" is "{value:Q}"')
def step_json_str(context, path, value):
    got = json_at(body(context), path)
    assert str(got) == sub(context, value), f'{path}: expected "{sub(context, value)}", got "{got}"'


@then('the response JSON at "{path:Q}" is {number:d}')
def step_json_int(context, path, number):
    got = json_at(body(context), path)
    assert got == number, f"{path}: expected {number}, got {got!r}"


@then('the response JSON has a non-empty "{path:Q}"')
def step_json_nonempty(context, path):
    got = json_at(body(context), path)
    assert got not in (None, "", [], {}), f"{path} is empty: " + show(context)


@then('the response JSON at "{path:Q}" is an empty list')
def step_json_empty_list(context, path):
    got = json_at(body(context), path)
    assert got == [], f"{path}: expected [], got {got!r}"


@then("the response JSON has the fields")
def step_json_fields(context):
    b = body(context)
    missing = [row["field"] for row in context.table if row["field"] not in b]
    assert not missing, f"missing fields {missing}: " + show(context)


@then('the response JSON at "{path:Q}" contains "{text:Q}"')
def step_json_contains(context, path, text):
    got = str(json_at(body(context), path))
    assert sub(context, text) in got, f'{path}: "{sub(context, text)}" not in "{got}"'


@then('the response body contains "{text:Q}"')
def step_body_contains(context, text):
    assert sub(context, text) in context.resp.text, "not found: " + show(context)


@then("the problem lists at least {count:d} issues")
def step_issues(context, count):
    issues = body(context).get("issues") or []
    assert len(issues) >= count, f"expected >= {count} issues, got {len(issues)}: " + show(context)


@then("every issue has a location")
def step_issue_location(context):
    issues = body(context).get("issues") or []
    assert issues, "no issues: " + show(context)
    bad = [i for i in issues if not i.get("location")]
    assert not bad, f"issues without location: {bad}"


@then('an issue mentions "{text:Q}"')
def step_issue_mentions(context, text):
    issues = body(context).get("issues") or []
    needle = sub(context, text).lower()
    assert any(needle in json.dumps(i).lower() for i in issues), f'no issue mentions "{needle}": ' + show(context)


@then("the response has validation warnings")
def step_warnings(context):
    w = body(context).get("validationWarnings") or []
    assert w, "expected validationWarnings: " + show(context)


@then('vocabulary "{alias:Q}" has status "{status:Q}"')
def step_vocab_status(context, alias, status):
    r = admin_get(context, "/vocabularies/{" + alias.strip("{}") + "}")
    assert r.status_code == 200, f"HTTP {r.status_code} {r.text[:300]}"
    assert r.json().get("status") == status, f"expected {status}, got {r.json().get('status')}"


@then('version {version:d} of "{alias:Q}" has status "{status:Q}"')
def step_version_status(context, version, alias, status):
    r = admin_get(context, "/vocabularies/{" + alias.strip("{}") + "}/versions/" + str(version))
    assert r.status_code == 200, f"HTTP {r.status_code} {r.text[:300]}"
    assert r.json().get("status") == status, f"v{version}: expected {status}, got {r.json().get('status')}"


@then('vocabulary "{alias:Q}" does not exist')
def step_vocab_absent(context, alias):
    r = admin_get(context, "/vocabularies/{" + alias.strip("{}") + "}")
    assert r.status_code == 404, f"expected 404, got HTTP {r.status_code} {r.text[:200]}"


@then('vocabulary "{alias:Q}" has {count:d} versions')
def step_vocab_count(context, alias, count):
    r = admin_get(context, "/vocabularies/{" + alias.strip("{}") + "}/versions?pageSize=100")
    assert r.status_code == 200, f"HTTP {r.status_code} {r.text[:300]}"
    items = r.json().get("items", [])
    assert len(items) == count, f"expected {count} versions, got {len(items)}"


@then('the list contains "{alias:Q}"')
def step_list_contains(context, alias):
    names = [i.get("name") for i in body(context).get("items", [])]
    assert name(context, alias) in names, f"{name(context, alias)} not in {names}"


@then('the list does not contain "{alias:Q}"')
def step_list_not_contains(context, alias):
    names = [i.get("name") for i in body(context).get("items", [])]
    assert name(context, alias) not in names, f"{name(context, alias)} unexpectedly in {names}"


@then('the list is ordered by "{field:Q}" {direction}')
def step_list_order(context, field, direction):
    values = [i.get(field) for i in body(context).get("items", [])]
    expected = sorted(values, key=lambda v: (v is None, str(v).lower()), reverse=(direction == "descending"))
    assert [str(v).lower() for v in values] == [str(v).lower() for v in expected], f"not {direction} by {field}: {values}"


@then('every item in the list has "{field:Q}" "{value:Q}"')
def step_list_all(context, field, value):
    items = body(context).get("items", [])
    assert items, "empty list"
    bad = [i.get("name") for i in items if str(i.get(field)) != value]
    assert not bad, f"items without {field}={value}: {bad}"


@then("the list has {count:d} items")
def step_list_count(context, count):
    items = body(context).get("items", [])
    assert len(items) == count, f"expected {count} items, got {len(items)}"


# ---------- external vocabularies (OpenAPI only) ----------

@when('I register external vocabulary "{alias:Q}" with namespace "{ns:Q}" and alias "{alias_uri:Q}"')
def step_register_external(context, alias, ns, alias_uri):
    n = name(context, alias)
    ns, alias_uri = sub(context, ns), sub(context, alias_uri)
    data = {"name": n, "namespaceUri": ns, "aliasUris": alias_uri, "description": "Test external vocabulary",
            "sourceUri": "https://ext.example.org/source.ttl"}
    upload(context, "/external-vocabularies", data, fixture(context, "external_dcat.ttl", n, EXT_NS=ns))
    if context.resp.status_code == 201:
        context.externals.add(n)


@when('I send DELETE "{path:Q}"')
def step_delete(context, path):
    context.resp = requests.delete(url(context, path), headers=headers(context), timeout=30)
    m = re.match(r"^/external-vocabularies/([A-Za-z0-9]+)$", sub(context, path))
    if m and context.resp.status_code == 204:
        context.externals.discard(m.group(1))
