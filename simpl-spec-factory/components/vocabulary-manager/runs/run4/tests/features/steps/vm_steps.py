"""Shared step library for the Vocabulary Manager API suite (black box, HTTP only)."""
import json
import re
from collections import Counter

from behave import given, then, when

from vmclient import json_path, parse_dt, to_second, utc_today


def W(context):
    return context.w


def table_fields(context):
    return {row["field"]: row["value"] for row in context.table} if context.table else {}


def expect(cond, context, msg):
    if not cond:
        raise AssertionError("%s\n%s" % (msg, W(context).describe()))


def ok(context, r, codes=(200, 201, 204)):
    if r.status_code not in codes:
        raise AssertionError("setup call failed: %s" % W(context).describe(r))
    return r


# ====================================================================
# Who calls
# ====================================================================
@given('I am authenticated as "{user}"')
def step_auth(context, user):
    W(context).set_user(user)


@given("I am not authenticated")
def step_anon(context):
    W(context).set_anonymous()


@given('I use a token that is "{kind}"')
def step_bad_token(context, kind):
    W(context).set_invalid_token(kind)


# ====================================================================
# Data set-up (always as ga-admin, must succeed)
# ====================================================================
@given('a vocabulary "{alias}" exists')
def step_vocab_exists(context, alias):
    ok(context, W(context).create_vocabulary(alias, auth=("user", "ga-admin"), record=False))


@given('a vocabulary "{alias}" exists with:')
def step_vocab_exists_with(context, alias):
    ok(context, W(context).create_vocabulary(alias, table_fields(context), auth=("user", "ga-admin"), record=False))


@given('a vocabulary "{alias}" exists with the content:')
def step_vocab_exists_content(context, alias):
    w = W(context)
    ok(context, w.create_vocabulary(alias, content=w.expand(context.text).encode("utf-8"),
                                     auth=("user", "ga-admin"), record=False))


@given('a vocabulary "{alias}" exists with {n:d} versions')
def step_vocab_exists_n(context, alias, n):
    w = W(context)
    ok(context, w.create_vocabulary(alias, auth=("user", "ga-admin"), record=False))
    for _ in range(n - 1):
        ok(context, w.create_version(alias, auth=("user", "ga-admin"), record=False))


@given('a new version of "{alias}" exists')
def step_version_exists(context, alias):
    ok(context, W(context).create_version(alias, auth=("user", "ga-admin"), record=False))


@given('a new version of "{alias}" exists with:')
def step_version_exists_with(context, alias):
    ok(context, W(context).create_version(alias, table_fields(context), auth=("user", "ga-admin"), record=False))


@given('the vocabulary "{alias}" has status "{status}"')
def step_vocab_status_given(context, alias, status):
    ok(context, W(context).patch_status(alias, status, auth=("user", "ga-admin"), record=False))


@given('version {v:d} of vocabulary "{alias}" has status "{status}"')
def step_version_status_given(context, v, alias, status):
    ok(context, W(context).patch_status(alias, status, version=v, auth=("user", "ga-admin"), record=False))


@given('an external vocabulary "{alias}" is registered')
def step_ext_exists(context, alias):
    ok(context, W(context).register_external(alias, auth=("user", "ga-admin"), record=False))


@given('an external vocabulary "{alias}" is registered with:')
def step_ext_exists_with(context, alias):
    ok(context, W(context).register_external(alias, table_fields(context), auth=("user", "ga-admin"), record=False))


@given('the external vocabulary "{alias}" is removed')
def step_ext_removed(context, alias):
    w = W(context)
    ok(context, w.as_admin("DELETE", "/external-vocabularies/%s" % w.ext_name(alias)))


@given('I remember the field "{path}" as "{key}"')
@when('I remember the field "{path}" as "{key}"')
def step_remember(context, path, key):
    W(context).saved[key] = json_path(W(context).body(), path)


# ====================================================================
# Requests as the current user
# ====================================================================
@when('I create the vocabulary "{alias}" with:')
def step_create_with(context, alias):
    W(context).create_vocabulary(alias, table_fields(context))


@when('I create the vocabulary "{alias}" with the content:')
def step_create_content(context, alias):
    w = W(context)
    w.create_vocabulary(alias, content=w.expand(context.text).encode("utf-8"))


@when('I create the vocabulary "{alias}" with the file "{fixture}"')
def step_create_file(context, alias, fixture):
    W(context).create_vocabulary(alias, {"file": fixture})


@when('I create a new version of "{alias}" with:')
def step_new_version_with(context, alias):
    W(context).create_version(alias, table_fields(context))


@when('I create a new version of "{alias}" with the content:')
def step_new_version_content(context, alias):
    w = W(context)
    w.create_version(alias, content=w.expand(context.text).encode("utf-8"))


@when('I create a new version of "{alias}" with the file "{fixture}"')
def step_new_version_file(context, alias, fixture):
    W(context).create_version(alias, {"file": fixture})


@when('I change the status of vocabulary "{alias}" to "{status}"')
def step_patch_vocab(context, alias, status):
    W(context).patch_status(alias, status)


@when('I change the status of version {v:d} of vocabulary "{alias}" to "{status}"')
def step_patch_version(context, v, alias, status):
    W(context).patch_status(alias, status, version=v)


@when('I register the external vocabulary "{alias}"')
def step_register(context, alias):
    W(context).register_external(alias)


@when('I register the external vocabulary "{alias}" with:')
def step_register_with(context, alias):
    W(context).register_external(alias, table_fields(context))


@when('I send {method} "{path}" with the header "{header}" set to "{value}"')
def step_send_header(context, method, path, header, value):
    w = W(context)
    w.call(method, w.expand(path), headers={header: w.expand(value)})


@when('I send {method} "{path}" with the JSON body:')
def step_send_json(context, method, path):
    w = W(context)
    w.call(method, w.expand(path), data=w.expand(context.text).encode("utf-8"),
           headers={"Content-Type": "application/json"})


@when('I send {method} "{path}" with the content type "{ctype}" and the body:')
def step_send_ctype(context, method, path, ctype):
    w = W(context)
    w.call(method, w.expand(path), data=w.expand(context.text).encode("utf-8"),
           headers={"Content-Type": ctype})


@when('I send {method} "{path}" with a valid multipart vocabulary form')
def step_send_multipart(context, method, path):
    """Generic multipart create body, used by the access-control matrix."""
    w = W(context)
    from world import multipart
    fields = [("name", w.prefix + "Matrix"), ("description", "Matrix."), ("changelog", "Matrix.")]
    data = w.default_content("Matrix", 1)
    if "external" in path:
        fields = [("name", "E" + w.runid + "Matrix"), ("namespaceUri", "https://ext.example/%s/matrix#" % w.runid)]
    w.call(method, w.expand(path), files=multipart(fields, "Matrix", data))


@when('I search vocabularies for:')
def step_search(context):
    w = W(context)
    items = []
    for row in context.table:
        item = {"name": w.expand(row["name"])}
        if row.get("version", "").strip():
            item["version"] = int(row["version"])
        items.append(item)
    w.call("POST", "/vocabularies/searches", json={"items": items})


@when('I search vocabularies with {n:d} references')
def step_search_n(context, n):
    w = W(context)
    items = [{"name": "%sRef%d" % (w.prefix, i)} for i in range(n)]
    w.call("POST", "/vocabularies/searches", json={"items": items})


@when('I search external vocabularies with {n:d} namespace URIs and {m:d} alias URIs')
def step_ext_search_n(context, n, m):
    w = W(context)
    body = {}
    if n:
        body["namespaceUris"] = ["https://none.example/%s/n%d" % (w.runid, i) for i in range(n)]
    if m:
        body["aliasUris"] = ["https://none.example/%s/a%d" % (w.runid, i) for i in range(m)]
    w.call("POST", "/external-vocabulary/searches", json=body)


# ---------- parallel ----------
@when('I create {n:d} new versions of "{alias}" in parallel')
def step_parallel_versions(context, n, alias):
    w = W(context)
    contents = [w.default_content(alias, "parallel-%d" % i) for i in range(n)]
    nxt = w.latest.get(alias, 0)

    def mk(i):
        return lambda: w.create_version(
            alias, {"description": "Parallel version %d." % i, "changelog": "Parallel %d." % i},
            content=contents[i], record=False)

    w.parallel([mk(i) for i in range(n)])
    w.parallel_base = nxt


@when('I create the vocabulary "{alias}" {n:d} times in parallel')
def step_parallel_create(context, alias, n):
    w = W(context)
    w.name(alias)
    w.parallel([lambda: w.create_vocabulary(alias, record=False) for _ in range(n)])


@when('I create the vocabularies "{a}" and "{b}" in parallel')
def step_parallel_create_two(context, a, b):
    w = W(context)
    w.name(a)
    w.parallel([lambda: w.create_vocabulary(a, record=False), lambda: w.create_vocabulary(b, record=False)])


@when('I change the status of vocabulary "{alias}" to "{status}" {n:d} times in parallel')
def step_parallel_patch(context, alias, status, n):
    w = W(context)
    w.parallel([lambda: w.patch_status(alias, status, record=False) for _ in range(n)])


# ====================================================================
# Response checks
# ====================================================================
@then("the response status is {code:d}")
def step_status(context, code):
    expect(W(context).response.status_code == code, context, "expected status %d" % code)


@then("the response status is not {code:d}")
def step_status_not(context, code):
    expect(W(context).response.status_code != code, context, "unexpected status %d" % code)


@then("the response status is one of {codes}")
def step_status_one_of(context, codes):
    allowed = [int(c) for c in re.findall(r"\d+", codes)]
    expect(W(context).response.status_code in allowed, context, "expected one of %s" % allowed)


@then("the request is authorised and processed")
def step_authorised(context):
    r = W(context).response
    expect(r.status_code not in (401, 403) and r.status_code < 500, context,
           "expected the request to pass access control and be processed")


@then('the response content type is "{ctype}"')
def step_ctype(context, ctype):
    got = W(context).response.headers.get("Content-Type", "")
    expect(got.split(";")[0].strip().lower() == ctype.lower(), context,
           "expected Content-Type %s, got %r" % (ctype, got))


@then("the response has a Content-Type header")
def step_has_ctype(context):
    expect(bool(W(context).response.headers.get("Content-Type")), context, "no Content-Type header")


@then('the response header "{header}" is "{value}"')
def step_header_is(context, header, value):
    w = W(context)
    got = w.response.headers.get(header)
    expect(got == w.expand(value), context, "header %s: expected %r, got %r" % (header, w.expand(value), got))


@then('the response header "{header}" contains "{value}"')
def step_header_contains(context, header, value):
    w = W(context)
    got = w.response.headers.get(header) or ""
    expect(w.expand(value) in got, context, "header %s %r does not contain %r" % (header, got, w.expand(value)))


@then('the response is a download named "{filename}"')
def step_download(context, filename):
    w = W(context)
    cd = w.response.headers.get("Content-Disposition", "")
    fn = w.expand(filename)
    expect(cd.lower().startswith("attachment"), context, "Content-Disposition is not attachment: %r" % cd)
    m = re.search(r'filename\*?=(?:UTF-8\'\')?"?([^";]+)"?', cd)
    expect(m is not None and m.group(1) == fn, context, "expected filename %r in %r" % (fn, cd))


@then('the response body is the content of version {v:d} of "{alias}"')
def step_body_content(context, v, alias):
    w = W(context)
    expect(w.response.content == w.contents[(alias, v)], context,
           "downloaded bytes differ from the uploaded content of version %d" % v)


@then('the response body is the content of the external vocabulary "{alias}"')
def step_body_ext_content(context, alias):
    w = W(context)
    expect(w.response.content == w.ext_contents[alias], context, "downloaded bytes differ from the registered content")


# ---------- problems ----------
@then('the response is a problem with status {code:d} and type "{ptype}"')
def step_problem(context, code, ptype):
    w = W(context)
    r = w.response
    expect(r.status_code == code, context, "expected status %d" % code)
    ct = r.headers.get("Content-Type", "").split(";")[0].strip().lower()
    expect(ct == "application/problem+json", context, "expected application/problem+json, got %r" % ct)
    body = w.body()
    expect(body.get("type") == ptype, context, "expected problem type %s" % ptype)
    expect(body.get("status") == code, context, "problem status field should be %d" % code)


@then('the response is a problem with status {code:d}')
def step_problem_any(context, code):
    w = W(context)
    r = w.response
    expect(r.status_code == code, context, "expected status %d" % code)
    ct = r.headers.get("Content-Type", "").split(";")[0].strip().lower()
    expect(ct == "application/problem+json", context, "expected application/problem+json, got %r" % ct)
    body = w.body()
    expect(body.get("status") == code, context, "problem status field should be %d" % code)
    expect(bool(body.get("detail")) or bool(body.get("issues")), context, "problem has no detail of the error")


@then('the problem title is "{title}"')
def step_problem_title(context, title):
    expect(W(context).body().get("title") == title, context, "expected title %r" % title)


@then('the problem detail is "{detail}"')
def step_problem_detail(context, detail):
    w = W(context)
    expect(w.body().get("detail") == w.expand(detail), context, "expected detail %r" % w.expand(detail))


@then('the problem detail contains "{text}"')
def step_problem_detail_contains(context, text):
    w = W(context)
    expect(w.expand(text) in (w.body().get("detail") or ""), context, "detail does not contain %r" % w.expand(text))


@then("the problem has {n:d} issue")
@then("the problem has {n:d} issues")
def step_issue_count(context, n):
    issues = W(context).body().get("issues") or []
    expect(len(issues) == n, context, "expected %d issues, got %d" % (n, len(issues)))


@then('the problem has an issue in "{where}" named "{name}"')
def step_issue_named(context, where, name):
    w = W(context)
    issues = w.body().get("issues") or []
    expect(any(i.get("in") == where and i.get("name") == w.expand(name) for i in issues), context,
           "no issue in %s named %s" % (where, name))


@then('every issue is a vocabulary file issue')
def step_issue_vocabfile(context):
    issues = W(context).body().get("issues") or []
    expect(issues, context, "no issues")
    for i in issues:
        expect(i.get("type") == "urn:problem-type:belgif:input-validation:invalidInput"
               and i.get("in") == "body" and i.get("name") == "vocabularyFile"
               and bool(i.get("detail")) and bool(i.get("location")), context,
               "issue is not a vocabulary file issue with detail and location: %r" % i)


@then('the problem has an issue with detail "{detail}" and location "{loc}"')
def step_issue_detail_loc(context, detail, loc):
    w = W(context)
    d, l = w.expand(detail), w.expand(loc)
    issues = w.body().get("issues") or []
    expect(any(i.get("detail") == d and i.get("location") == l for i in issues), context,
           "no issue with detail %r and location %r" % (d, l))


@then('the problem has an issue whose detail starts with "{prefix}" and contains "{text}"')
def step_issue_detail_prefix_contains(context, prefix, text):
    w = W(context)
    p, t = w.expand(prefix), w.expand(text)
    issues = w.body().get("issues") or []
    expect(any((i.get("detail") or "").startswith(p) and t in i.get("detail") for i in issues), context,
           "no issue detail starting %r containing %r" % (p, t))


@then('the issue location is a parser position on line {line:d}')
def step_issue_line(context, line):
    issues = W(context).body().get("issues") or []
    loc = issues[0].get("location", "") if issues else ""
    m = re.match(r"^line (\d+), column (\d+)$", loc)
    expect(m is not None, context, "location %r is not 'line N, column M'" % loc)
    expect(int(m.group(1)) == line, context, "expected line %d in %r" % (line, loc))


@then("the response body has no field \"{field}\"")
def step_no_field(context, field):
    r = W(context).response
    try:
        body = r.json()
    except ValueError:
        return
    expect(not (isinstance(body, dict) and field in body), context, "body should not contain %r" % field)


# ---------- generic JSON fields ----------
@then('the field "{path}" is "{value}"')
def step_field_str(context, path, value):
    w = W(context)
    got = json_path(w.body(), path)
    expect(got == w.expand(value), context, "field %s: expected %r, got %r" % (path, w.expand(value), got))


@then('the field "{path}" is the number {n:d}')
def step_field_num(context, path, n):
    got = json_path(W(context).body(), path)
    expect(got == n and not isinstance(got, bool), context, "field %s: expected %d, got %r" % (path, n, got))


@then('the field "{path}" is the boolean {b}')
def step_field_bool(context, path, b):
    if b not in ("true", "false"):
        raise ValueError("use true or false")
    got = json_path(W(context).body(), path)
    expect(got is (b == "true"), context, "field %s: expected %s, got %r" % (path, b, got))


@then('the field "{path}" is an empty list')
def step_field_empty(context, path):
    got = json_path(W(context).body(), path)
    expect(got == [], context, "field %s should be []" % path)


@then('the field "{path}" is absent')
def step_field_absent(context, path):
    try:
        json_path(W(context).body(), path)
    except KeyError:
        return
    raise AssertionError("field %s should be absent\n%s" % (path, W(context).describe()))


@then('the fields "{paths}" are absent')
def step_fields_absent(context, paths):
    for p in [x.strip() for x in paths.split(",")]:
        step_field_absent(context, p)


@then('the field "{path}" is a UTC date-time')
def step_field_dt(context, path):
    got = json_path(W(context).body(), path)
    parse_dt(got)


@then('the response has the fields "{fields}"')
def step_fields_present(context, fields):
    body = W(context).body()
    for f in [x.strip() for x in fields.split(",")]:
        expect(f in body, context, "field %s missing" % f)


# ---------- vocabulary and version objects ----------
def check_version_object(context, body, alias, v, status=None):
    w = W(context)
    name = w.name(alias)
    href = "/vocabularies/%s/versions/%d" % (name, v)
    errs = []
    if body.get("href") != href:
        errs.append("href %r != %r" % (body.get("href"), href))
    if body.get("contentHref") != w.content_url(alias, v):
        errs.append("contentHref %r != %r" % (body.get("contentHref"), w.content_url(alias, v)))
    if body.get("name") != name:
        errs.append("name %r" % body.get("name"))
    if body.get("version") != v:
        errs.append("version %r" % body.get("version"))
    if status and body.get("status") != status:
        errs.append("status %r != %s" % (body.get("status"), status))
    if (alias, v) in w.descriptions and body.get("description") != w.descriptions[(alias, v)]:
        errs.append("description %r != %r" % (body.get("description"), w.descriptions[(alias, v)]))
    for f in ("creationDate", "lastUpdateDate"):
        try:
            parse_dt(body.get(f))
        except Exception:
            errs.append("%s not a date-time: %r" % (f, body.get(f)))
    if not isinstance(body.get("changelog"), str):
        errs.append("changelog missing")
    if errs:
        raise AssertionError("version object of %s v%d: %s\n%r" % (alias, v, "; ".join(errs), body))


@then('the response is version {v:d} of "{alias}" with status "{status}"')
def step_is_version(context, v, alias, status):
    check_version_object(context, W(context).body(), alias, v, status)


@then('the response is vocabulary "{alias}" with latest version {v:d} and status "{status}"')
def step_is_vocab(context, alias, v, status):
    w = W(context)
    body = w.body()
    name = w.name(alias)
    errs = []
    if body.get("href") != "/vocabularies/%s" % name:
        errs.append("href %r" % body.get("href"))
    if body.get("contentHref") != w.content_url(alias):
        errs.append("contentHref %r != %r" % (body.get("contentHref"), w.content_url(alias)))
    if body.get("name") != name:
        errs.append("name %r" % body.get("name"))
    if body.get("version") != v:
        errs.append("version %r != %d" % (body.get("version"), v))
    if body.get("status") != status:
        errs.append("status %r != %s" % (body.get("status"), status))
    if (alias, v) in w.descriptions and body.get("description") != w.descriptions[(alias, v)]:
        errs.append("description %r is not the latest version's %r" % (body.get("description"), w.descriptions[(alias, v)]))
    for f in ("creationDate", "lastUpdateDate"):
        try:
            parse_dt(body.get(f))
        except Exception:
            errs.append("%s not a date-time" % f)
    if errs:
        raise AssertionError("vocabulary %s: %s\n%r" % (alias, "; ".join(errs), body))


@then('the Location header is the href of version {v:d} of "{alias}"')
def step_location(context, v, alias):
    w = W(context)
    href = "/vocabularies/%s/versions/%d" % (w.name(alias), v)
    loc = w.response.headers.get("Location", "")
    expect(loc == href or loc.endswith(href) and loc[: -len(href)].rstrip("/").endswith("/v1"), context,
           "Location %r is not %r" % (loc, href))
    expect(w.body().get("href") == href, context, "href is not %r" % href)


@then('the response has no validation warnings')
def step_no_warnings(context):
    body = W(context).body()
    expect("validationWarnings" in body and body["validationWarnings"] == [], context,
           "validationWarnings should be present and empty")


@then('the response has a validation warning "{code}" with description "{desc}" and location "{loc}"')
def step_warning_full(context, code, desc, loc):
    w = W(context)
    d, l = w.expand(desc), w.expand(loc)
    ws = w.body().get("validationWarnings") or []
    expect(any(x.get("code") == code and x.get("description") == d and x.get("location") == l for x in ws), context,
           "no warning %s / %r / %r" % (code, d, l))


@then('the response has a validation warning "{code}" with location "{loc}"')
def step_warning_loc(context, code, loc):
    w = W(context)
    l = w.expand(loc)
    ws = w.body().get("validationWarnings") or []
    expect(any(x.get("code") == code and x.get("location") == l and x.get("description") for x in ws), context,
           "no warning %s at %r" % (code, l))


@then('the response has a validation warning "{code}" whose description starts with "{desc}"')
def step_warning_prefix(context, code, desc):
    w = W(context)
    d = w.expand(desc)
    ws = w.body().get("validationWarnings") or []
    expect(any(x.get("code") == code and (x.get("description") or "").startswith(d) for x in ws), context,
           "no warning %s starting %r" % (code, d))


@then('the response has {n:d} validation warnings with code "{code}"')
@then('the response has {n:d} validation warning with code "{code}"')
def step_warning_count(context, n, code):
    ws = W(context).body().get("validationWarnings") or []
    got = len([x for x in ws if x.get("code") == code])
    expect(got == n, context, "expected %d warnings %s, got %d" % (n, code, got))


@then('the response has no validation warning with code "{code}"')
def step_warning_none(context, code):
    step_warning_count(context, 0, code)


@then('every validation warning code is one of "{codes}"')
def step_warning_codes(context, codes):
    allowed = [c.strip() for c in codes.split(",")]
    ws = W(context).body().get("validationWarnings")
    expect(isinstance(ws, list), context, "validationWarnings must be a list")
    for x in ws:
        expect(x.get("code") in allowed, context, "warning code %r not in %s" % (x.get("code"), allowed))


# ---------- state checks (read back as ga-admin) ----------
@then('the vocabulary "{alias}" does not exist')
def step_vocab_absent(context, alias):
    w = W(context)
    r = w.as_admin("GET", "/vocabularies/%s" % w.name(alias))
    if r.status_code != 404:
        raise AssertionError("vocabulary %s should not exist: %s" % (alias, w.describe(r)))


@then('the vocabulary "{alias}" has status "{status}"')
def step_vocab_has_status(context, alias, status):
    w = W(context)
    r = ok(context, w.as_admin("GET", "/vocabularies/%s" % w.name(alias)), (200,))
    if r.json().get("status") != status:
        raise AssertionError("vocabulary %s status %r != %s" % (alias, r.json().get("status"), status))


@then('the vocabulary "{alias}" has latest version {v:d}')
def step_vocab_latest(context, alias, v):
    w = W(context)
    r = ok(context, w.as_admin("GET", "/vocabularies/%s" % w.name(alias)), (200,))
    if r.json().get("version") != v:
        raise AssertionError("vocabulary %s latest version %r != %d" % (alias, r.json().get("version"), v))


def all_versions(context, alias):
    w = W(context)
    r = ok(context, w.as_admin("GET", "/vocabularies/%s/versions?pageSize=100" % w.name(alias)), (200,))
    return r.json().get("items", [])


@then('the vocabulary "{alias}" has {n:d} versions')
@then('the vocabulary "{alias}" has {n:d} version')
def step_vocab_count(context, alias, n):
    items = all_versions(context, alias)
    if len(items) != n:
        raise AssertionError("vocabulary %s has %d versions, expected %d" % (alias, len(items), n))


@then('the versions of "{alias}" have the statuses "{statuses}"')
def step_versions_statuses(context, alias, statuses):
    """statuses like '1=DEPRECATED, 2=ACTIVE'."""
    items = {i["version"]: i["status"] for i in all_versions(context, alias)}
    want = {int(k): v for k, v in (p.strip().split("=") for p in statuses.split(","))}
    if items != want:
        raise AssertionError("version statuses of %s: got %r, expected %r" % (alias, items, want))


@then('version {v:d} of "{alias}" has a lastUpdateDate equal to the creationDate of version {w2:d}')
def step_lud_equals(context, v, alias, w2):
    items = {i["version"]: i for i in all_versions(context, alias)}
    a, b = parse_dt(items[v]["lastUpdateDate"]), parse_dt(items[w2]["creationDate"])
    if a != b:
        raise AssertionError("lastUpdateDate of v%d %s != creationDate of v%d %s" % (v, a, w2, b))


@then('the vocabulary "{alias}" has a lastUpdateDate not before the creationDate of version {v:d}')
def step_vocab_lud(context, alias, v):
    w = W(context)
    body = ok(context, w.as_admin("GET", "/vocabularies/%s" % w.name(alias)), (200,)).json()
    items = {i["version"]: i for i in all_versions(context, alias)}
    if parse_dt(body["lastUpdateDate"]) < parse_dt(items[v]["creationDate"]):
        raise AssertionError("vocabulary lastUpdateDate %s is before v%d creation" % (body["lastUpdateDate"], v))


@then('the vocabulary "{alias}" has a lastUpdateDate not before the saved "{key}"')
def step_vocab_lud_saved(context, alias, key):
    w = W(context)
    body = ok(context, w.as_admin("GET", "/vocabularies/%s" % w.name(alias)), (200,)).json()
    if parse_dt(body["lastUpdateDate"]) < parse_dt(w.saved[key]):
        raise AssertionError("lastUpdateDate went backwards")


@then('the vocabulary "{alias}" was created by "{user}"')
def step_vocab_creator(context, alias, user):
    w = W(context)
    body = ok(context, w.as_admin("GET", "/vocabularies/%s" % w.name(alias)), (200,)).json()
    if body.get("createdBy") != w.cfg.emails[user]:
        raise AssertionError("createdBy %r != %r" % (body.get("createdBy"), w.cfg.emails[user]))


@then('the field "createdBy" is the email of "{user}"')
def step_created_by(context, user):
    w = W(context)
    expect(w.body().get("createdBy") == w.cfg.emails[user], context, "createdBy is not %s" % w.cfg.emails[user])


@then('the field "{path}" is the href of version {v:d} of "{alias}"')
def step_latest_href(context, path, v, alias):
    w = W(context)
    href = "/vocabularies/%s/versions/%d" % (w.name(alias), v)
    expect(json_path(w.body(), path) == href, context, "%s is not %r" % (path, href))


@then('the external vocabulary "{alias}" does not exist')
def step_ext_absent(context, alias):
    w = W(context)
    r = w.as_admin("GET", "/external-vocabularies/%s" % w.ext_name(alias))
    if r.status_code != 404:
        raise AssertionError("external vocabulary should not exist: %s" % w.describe(r))


# ---------- parallel results ----------
@then("the responses have the statuses {codes}")
def step_parallel_statuses(context, codes):
    w = W(context)
    want = Counter(int(c) for c in re.findall(r"\d+", codes))
    got = Counter(r.status_code for r in w.responses)
    if want != got:
        raise AssertionError("statuses %r, expected %r: %s" % (
            dict(got), dict(want), [w.describe(r) for r in w.responses if r.status_code not in want]))


@then('the parallel versions are {first:d} to {last:d}, each once, with distinct Location headers')
def step_parallel_versions_check(context, first, last):
    w = W(context)
    versions = sorted(r.json()["version"] for r in w.responses)
    if versions != list(range(first, last + 1)):
        raise AssertionError("assigned versions %r" % versions)
    locs = [r.headers.get("Location") for r in w.responses]
    if len(set(locs)) != len(locs) or None in locs:
        raise AssertionError("Location headers not distinct: %r" % locs)


# ====================================================================
# Lists
# ====================================================================
def items(context):
    body = W(context).body()
    expect(isinstance(body.get("items"), list), context, "items is not a list")
    return body["items"]


@then('the list holds the vocabularies "{aliases}" in this order')
def step_list_order(context, aliases):
    w = W(context)
    want = [w.name(a.strip()) for a in aliases.split(",") if a.strip()]
    got = [i.get("name") for i in items(context)]
    expect(got == want, context, "list order %r, expected %r" % (got, want))


@then('the list holds the vocabularies "{aliases}" in any order')
def step_list_any(context, aliases):
    w = W(context)
    want = sorted(w.name(a.strip()) for a in aliases.split(",") if a.strip())
    got = sorted(i.get("name") for i in items(context))
    expect(got == want, context, "list holds %r, expected %r" % (got, want))


@then('the list contains the vocabulary "{alias}"')
def step_list_contains(context, alias):
    w = W(context)
    expect(any(i.get("name") == w.name(alias) for i in items(context)), context, "%s not in list" % alias)


@then('the list contains the external vocabulary "{alias}"')
def step_list_contains_ext(context, alias):
    w = W(context)
    expect(any(i.get("name") == w.ext_name(alias) for i in items(context)), context, "%s not in list" % alias)


@then("the list holds no items")
def step_list_empty(context):
    expect(items(context) == [], context, "items should be empty")


@then('the response has no "items" field')
def step_no_items(context):
    step_no_field(context, "items")


@then("the page fields are page {p:d}, pageSize {ps:d}, total {t:d} and totalPages {tp:d}")
def step_page_fields(context, p, ps, t, tp):
    b = W(context).body()
    got = (b.get("page"), b.get("pageSize"), b.get("total"), b.get("totalPages"))
    expect(got == (p, ps, t, tp), context, "page fields %r, expected %r" % (got, (p, ps, t, tp)))


@then('the page links "{links}" are present')
def step_links_present(context, links):
    b = W(context).body()
    for l in [x.strip() for x in links.split(",")]:
        expect(isinstance(b.get(l), str) and b.get(l), context, "link %s missing" % l)


@then('the page links "{links}" are absent')
def step_links_absent(context, links):
    b = W(context).body()
    for l in [x.strip() for x in links.split(",")]:
        expect(b.get(l) in (None,), context, "link %s should be absent" % l)


@then('every list item has the fields "{fields}"')
def step_items_fields(context, fields):
    its = items(context)
    expect(its, context, "list is empty")
    for i in its:
        for f in [x.strip() for x in fields.split(",")]:
            expect(f in i and i[f] is not None, context, "item %s lacks %s" % (i.get("name"), f))


@then('every list item has status "{status}"')
def step_items_status(context, status):
    for i in items(context):
        expect(i.get("status") == status, context, "item %s has status %s" % (i.get("name"), i.get("status")))


@then('every list item has a {field} on the current UTC day')
def step_items_today(context, field):
    today = utc_today()
    for i in items(context):
        expect(parse_dt(i[field]).strftime("%Y-%m-%d") == today, context, "%s not today" % field)


@then('every list item has a {field} equal to the saved "{key}" to the second')
def step_items_eq_second(context, field, key):
    w = W(context)
    for i in items(context):
        expect(to_second(i[field]) == to_second(w.saved[key]), context, "%s differs" % field)


def _key(item, field):
    if field == "name":
        return item["name"]
    if field == "status":
        return item["status"]
    if field == "creationDate":
        return parse_dt(item["creationDate"])
    if field == "lastUpdated":
        return parse_dt(item["lastUpdateDate"])
    raise ValueError(field)


@then('the list is sorted by "{sort}" with ties broken by name ascending')
def step_list_sorted(context, sort):
    desc = sort.startswith("-")
    field = sort.lstrip("-")
    its = items(context)
    for a, b in zip(its, its[1:]):
        ka, kb = _key(a, field), _key(b, field)
        if ka == kb:
            ok_pair = a["name"] <= b["name"]
        else:
            ok_pair = ka > kb if desc else ka < kb
        expect(ok_pair, context, "items %s and %s are not in %s order" % (a["name"], b["name"], sort))


@then('the version list holds the versions "{versions}" in this order')
def step_version_order(context, versions):
    want = [int(v) for v in versions.split(",")]
    got = [i.get("version") for i in items(context)]
    expect(got == want, context, "version order %r, expected %r" % (got, want))


@then('every version in the list belongs to "{alias}" and has the fields "{fields}"')
def step_version_items(context, alias, fields):
    w = W(context)
    for i in items(context):
        check_version_object(context, i, alias, i["version"])
        for f in [x.strip() for x in fields.split(",")]:
            expect(f in i and i[f] is not None, context, "version %s lacks %s" % (i.get("version"), f))


@then('the external list is sorted by name ascending')
def step_ext_sorted(context):
    names = [i["name"] for i in items(context)]
    expect(names == sorted(names), context, "external list not sorted by name: %r" % names)


# ---------- search results ----------
@then("the search results are:")
def step_search_results(context):
    w = W(context)
    its = items(context)
    want = []
    for row in context.table:
        want.append((w.expand(row["name"]), int(row["version"]), row["status"], row["latest"] == "true"))
    got = [(i.get("name"), i.get("version"), i.get("status"), i.get("latest")) for i in its]
    expect(got == want, context, "search results %r, expected %r" % (got, want))
    for i in its:
        href = "/vocabularies/%s" % i["name"] if i["latest"] else "/vocabularies/%s/versions/%d" % (i["name"], i["version"])
        content = "%s/content/%s" % (w.cfg.content_base, i["name"]) + ("" if i["latest"] else "/versions/%d" % i["version"])
        expect(i.get("href") == href, context, "search href %r != %r" % (i.get("href"), href))
        expect(i.get("contentHref") == content, context, "search contentHref %r != %r" % (i.get("contentHref"), content))


@then('the external search results are "{aliases}" in this order')
def step_ext_results(context, aliases):
    w = W(context)
    want = [w.ext_name(a.strip()) for a in aliases.split(",") if a.strip()]
    got = [i.get("name") for i in items(context)]
    expect(got == want, context, "external results %r, expected %r" % (got, want))


@then('the response is the external vocabulary "{alias}"')
def step_is_ext(context, alias):
    w = W(context)
    b = w.body()
    name = w.ext_name(alias)
    expect(b.get("href") == "/external-vocabularies/%s" % name, context, "href wrong")
    expect(b.get("name") == name, context, "name wrong")
    expect(b.get("namespaceUri") == w.ext_namespace(alias), context, "namespaceUri wrong")
    expect(b.get("createdBy") == w.cfg.emails["ga-admin"], context, "createdBy wrong")
    parse_dt(b.get("creationDate"))


@then('the field "aliasUris" holds "{uris}"')
def step_alias_uris(context, uris):
    w = W(context)
    want = sorted(w.expand(u.strip()) for u in uris.split(","))
    expect(sorted(w.body().get("aliasUris") or []) == want, context, "aliasUris differ from %r" % want)


# ---- generic patterns last, so the longer patterns above match first ----
@when('I create the vocabulary "{alias}"')
def step_create(context, alias):
    W(context).create_vocabulary(alias)


@when('I create a new version of "{alias}"')
def step_new_version(context, alias):
    W(context).create_version(alias)


@when('I send {method} "{path}"')
def step_send(context, method, path):
    w = W(context)
    w.call(method, w.expand(path))


@then('the problem has an issue with detail "{detail}"')
def step_issue_detail(context, detail):
    w = W(context)
    d = w.expand(detail)
    issues = w.body().get("issues") or []
    expect(any(i.get("detail") == d for i in issues), context, "no issue with detail %r" % d)


@then('the problem has an issue whose detail starts with "{prefix}"')
def step_issue_detail_prefix(context, prefix):
    w = W(context)
    p = w.expand(prefix)
    issues = w.body().get("issues") or []
    expect(any((i.get("detail") or "").startswith(p) for i in issues), context, "no issue detail starting %r" % p)



# ====================================================================
# Operation matrix (access control): one call per OpenAPI operation
# ====================================================================
def _operation(w, op):
    from world import multipart
    a = w.name("A")
    x = w.ext_name("X")
    ns = w.ext_namespace("X")
    if op == "getVocabularies":
        return w.call("GET", "/vocabularies")
    if op == "createVocabulary":
        return w.call("POST", "/vocabularies", files=multipart(
            [("name", w.prefix + "Created"), ("description", "Created."), ("changelog", "Initial version.")],
            "Created", w.default_content("Created", 1)))
    if op == "searchVocabularies":
        return w.call("POST", "/vocabularies/searches", json={"items": [{"name": a}]})
    if op == "getVocabulary":
        return w.call("GET", "/vocabularies/%s" % a)
    if op == "updateVocabularyStatus":
        return w.call("PATCH", "/vocabularies/%s" % a, json={"status": "DEPRECATED"})
    if op == "getVocabularyVersions":
        return w.call("GET", "/vocabularies/%s/versions" % a)
    if op == "createVocabularyVersion":
        return w.call("POST", "/vocabularies/%s/versions" % a, files=multipart(
            [("description", "Matrix version."), ("changelog", "Matrix change.")],
            "A", w.default_content("A", "matrix")))
    if op == "getVocabularyVersion":
        return w.call("GET", "/vocabularies/%s/versions/1" % a)
    if op == "updateVocabularyVersionStatus":
        return w.call("PATCH", "/vocabularies/%s/versions/1" % a, json={"status": "DEPRECATED"})
    if op == "downloadVocabulary":
        return w.call("GET", "/content/%s" % a)
    if op == "downloadVocabularyVersion":
        return w.call("GET", "/content/%s/versions/1" % a)
    if op == "getExternalVocabularies":
        return w.call("GET", "/external-vocabularies")
    if op == "createExternalVocabulary":
        ns2 = "https://ext.example/%s/created#" % w.runid
        from vmclient import read_fixture
        data = w.expand(read_fixture("external-terms.ttl").replace("${extns}", ns2)).encode("utf-8")
        return w.call("POST", "/external-vocabularies", files=multipart(
            [("name", "E" + w.runid + "Created"), ("namespaceUri", ns2)], "Created", data))
    if op == "searchExternalVocabularies":
        return w.call("POST", "/external-vocabulary/searches", json={"namespaceUri": ns})
    if op == "downloadExternalVocabularyContent":
        return w.call("POST", "/external-vocabularies/content", json={"namespaceUri": ns})
    if op == "getExternalVocabularyContentByName":
        return w.call("GET", "/external-vocabularies/%s/content" % x)
    if op == "getExternalVocabulary":
        return w.call("GET", "/external-vocabularies/%s" % x)
    if op == "deleteExternalVocabulary":
        return w.call("DELETE", "/external-vocabularies/%s" % x)
    if op == "getHealth":
        return w.call("GET", "/health")
    raise ValueError("unknown operation %s" % op)


@when('I call the operation "{op}"')
def step_call_op(context, op):
    _operation(W(context), op)


@then('nothing was changed by the refused call')
def step_nothing_changed(context):
    """After a refused write: A still has 1 version, ACTIVE, vocabulary ACTIVE, X still registered."""
    w = W(context)
    step_vocab_count(context, "A", 1)
    step_versions_statuses(context, "A", "1=ACTIVE")
    step_vocab_has_status(context, "A", "ACTIVE")
    r = w.as_admin("GET", "/external-vocabularies/%s" % w.ext_name("X"))
    if r.status_code != 200:
        raise AssertionError("external vocabulary X was removed")
    for path in ("/vocabularies/%sCreated" % w.prefix, "/external-vocabularies/E%sCreated" % w.runid):
        r = w.as_admin("GET", path)
        if r.status_code != 404:
            raise AssertionError("%s was created by a refused call" % path)


@then('the version list is sorted by "{sort}" with ties broken by version descending')
def step_version_sorted(context, sort):
    desc = sort.startswith("-")
    field = sort.lstrip("-")
    its = items(context)
    expect(len(its) > 1, context, "too few versions to check the order")
    for a, b in zip(its, its[1:]):
        ka = parse_dt(a[field]) if field == "creationDate" else a[field]
        kb = parse_dt(b[field]) if field == "creationDate" else b[field]
        if ka == kb:
            good = a["version"] > b["version"]
        else:
            good = ka > kb if desc else ka < kb
        expect(good, context, "versions %s and %s are not in %s order" % (a["version"], b["version"], sort))
