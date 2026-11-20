"""Shared helpers for the Vocabulary Manager black-box suite.

Configuration comes only from environment variables, so the suite runs
unchanged against any implementation of the component.
"""
import base64
import datetime as dt
import json
import os
import random
import re
import string
import threading
import time
from urllib.parse import urlsplit

import requests

# ---------------------------------------------------------------- configuration

API_URL = os.environ.get("API_URL", "http://localhost:8196/v1").rstrip("/")
STORE_DOWN_API_URL = os.environ.get("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")
TOKEN_URL = os.environ.get(
    "TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_CLIENT = os.environ.get("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_NOAUD = os.environ.get("TOKEN_CLIENT_NOAUD", "vocabulary-tests-noaud")
TOKEN_URL_OTHER = os.environ.get(
    "TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token")
USER_PASSWORD = os.environ.get("USER_PASSWORD", "Password1!")
USERS = {
    "admin": os.environ.get("USER_ADMIN", "ga-admin"),
    "viewer": os.environ.get("USER_VIEWER", "ga-viewer"),
    "norole": os.environ.get("USER_NOROLE", "ga-norole"),
}
USER_EMAILS = {
    "admin": os.environ.get("USER_ADMIN_EMAIL", "ga-admin@example.com"),
    "viewer": os.environ.get("USER_VIEWER_EMAIL", "ga-viewer@example.com"),
    "norole": os.environ.get("USER_NOROLE_EMAIL", "ga-norole@example.com"),
}
HTTP_TIMEOUT = float(os.environ.get("HTTP_TIMEOUT", "120"))

RUN_ID = os.environ.get("RUN_ID") or (random.choice(string.ascii_lowercase) + "".join(
    random.choice(string.ascii_lowercase + string.digits) for _ in range(5)))
_counter = 0
_counter_lock = threading.Lock()


def new_sid():
    """Unique, lower-case alphanumeric id for one scenario."""
    global _counter
    with _counter_lock:
        _counter += 1
        return f"{RUN_ID}{_counter:04d}"


def api_base():
    return API_URL


def origin(base=None):
    parts = urlsplit(base or API_URL)
    return f"{parts.scheme}://{parts.netloc}"


# ---------------------------------------------------------------- tokens

_token_cache = {}
_token_lock = threading.Lock()


def get_token(user_key, token_url=None, client_id=None):
    token_url = token_url or TOKEN_URL
    client_id = client_id or TOKEN_CLIENT
    username = USERS[user_key]
    key = (token_url, client_id, username)
    with _token_lock:
        cached = _token_cache.get(key)
        if cached and cached[1] > time.time() + 20:
            return cached[0]
    resp = requests.post(token_url, data={
        "grant_type": "password",
        "client_id": client_id,
        "username": username,
        "password": USER_PASSWORD,
        "scope": "openid",
    }, timeout=30)
    if resp.status_code != 200:
        raise AssertionError(
            f"token request for {username} at {token_url} ({client_id}) failed: "
            f"{resp.status_code} {resp.text[:300]}")
    body = resp.json()
    token = body["access_token"]
    with _token_lock:
        _token_cache[key] = (token, time.time() + int(body.get("expires_in", 60)))
    return token


def tampered(token):
    """Same header and claims, broken signature."""
    head, payload, sig = token.split(".")
    raw = bytearray(base64.urlsafe_b64decode(sig + "=" * (-len(sig) % 4)))
    raw[0] ^= 0xFF
    raw[-1] ^= 0xFF
    new_sig = base64.urlsafe_b64encode(bytes(raw)).decode().rstrip("=")
    return f"{head}.{payload}.{new_sig}"


def auth_header(identity):
    """identity: admin, viewer, norole, none, garbage, tampered, noaud, otherissuer."""
    if identity in (None, "none"):
        return {}
    if identity == "garbage":
        return {"Authorization": "Bearer not-a-valid-token"}
    if identity == "tampered":
        return {"Authorization": "Bearer " + tampered(get_token("admin"))}
    if identity == "noaud":
        return {"Authorization": "Bearer " + get_token("admin", client_id=TOKEN_CLIENT_NOAUD)}
    if identity == "otherissuer":
        return {"Authorization": "Bearer " + get_token("admin", token_url=TOKEN_URL_OTHER)}
    return {"Authorization": "Bearer " + get_token(identity)}


# ---------------------------------------------------------------- HTTP

def url_for(path, base=None):
    if path.startswith("http://") or path.startswith("https://"):
        return path
    if path.startswith("//"):  # origin-relative path, outside the API base
        return origin(base) + path[1:]
    return (base or API_URL) + path


def send(method, path, identity="admin", base=None, headers=None, **kw):
    h = dict(auth_header(identity))
    if headers:
        for k, v in headers.items():
            if v is None:
                h[k] = None
            else:
                h[k] = v
    timeout = kw.pop("timeout", HTTP_TIMEOUT)
    kw.pop("allow_redirects", None)  # never follow redirects; not a Request argument
    session = requests.Session()
    req = requests.Request(method, url_for(path, base), headers={k: v for k, v in h.items() if v is not None}, **kw)
    prepared = session.prepare_request(req)
    for k, v in h.items():
        if v is None and k in prepared.headers:
            del prepared.headers[k]
    # keep the path exactly as written (encoded slashes and such)
    prepared.url = url_for(path, base) if "params" not in kw else prepared.url
    return session.send(prepared, timeout=timeout, allow_redirects=False)


# ---------------------------------------------------------------- names and placeholders

def vocab_name(sid, alias):
    return f"V{sid}{alias}"


def namespace(sid, alias):
    return f"https://data.example.org/{sid}/{alias.lower()}#"


def ext_uri(sid, alias):
    return f"https://ext.example.org/{sid}/{alias.lower()}"


_PH = re.compile(r"\$\{([^}]+)\}")


def parse_ts(value):
    return dt.datetime.strptime(value, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=dt.timezone.utc)


def _ts_transform(value, op):
    t = parse_ts(value)
    if op == "+1s":
        return (t + dt.timedelta(seconds=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
    if op == "-1s":
        return (t - dt.timedelta(seconds=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
    if op == "noz":
        return t.strftime("%Y-%m-%dT%H:%M:%S")
    if op == "date":
        return t.strftime("%Y-%m-%d")
    if op.startswith("offset"):
        sign = op[6]
        hh, mm = op[7:].split(":")
        delta = dt.timedelta(hours=int(hh), minutes=int(mm))
        local = t + delta if sign == "+" else t - delta
        return local.strftime("%Y-%m-%dT%H:%M:%S") + ("%2B" if sign == "+" else "-") + f"{hh}:{mm}"
    raise ValueError(op)


def expand(text, ctx):
    """Replace ${...} placeholders.

    ${A}           name of vocabulary alias A in this scenario
    ${lower:A}     the name in lower case; ${upper:A} in upper case
    ${NS:A}        the own namespace used in the Turtle of alias A
    ${URI:X}       a unique external namespace URI for alias X
    ${CB}          the content base, learnt from contentHref
    ${SID}         the scenario id (lower-case alphanumeric)
    ${TODAY}       today's UTC date
    ${R:key}       a remembered value; ${R:key:+1s}, ${R:key:noz}, ${R:key:date},
                   ${R:key:offset+01:00} transform a remembered timestamp
    ${EMAIL:admin} the e-mail of a test user
    """
    if text is None:
        return None

    def repl(m):
        expr = m.group(1)
        parts = expr.split(":", 1)
        head = parts[0]
        if head == "CB":
            return content_base(ctx)
        if head == "SID":
            return ctx.sid
        if head == "TODAY":
            return dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d")
        if head == "NS":
            return namespace(ctx.sid, parts[1])
        if head == "URI":
            return ext_uri(ctx.sid, parts[1])
        if head == "lower":
            return vocab_name(ctx.sid, parts[1]).lower()
        if head == "upper":
            return vocab_name(ctx.sid, parts[1]).upper()
        if head == "EMAIL":
            return USER_EMAILS[parts[1]]
        if head == "R":
            rest = parts[1]
            if ":" in rest:
                key, op = rest.split(":", 1)
                return _ts_transform(str(ctx.remembered[key]), op)
            return str(ctx.remembered[rest])
        return vocab_name(ctx.sid, expr)

    return _PH.sub(repl, text)


# ---------------------------------------------------------------- content base

_content_base = {"value": os.environ.get("CONTENT_BASE") or None}


def learn_content_base(name, content_href):
    if _content_base["value"] or not content_href:
        return
    suffix_latest = f"/content/{name}"
    idx = content_href.find(suffix_latest)
    if idx > 0:
        _content_base["value"] = content_href[:idx]


def content_base(ctx):
    if _content_base["value"]:
        return _content_base["value"]
    # create a probe vocabulary to learn it from contentHref
    name = vocab_name(new_sid(), "Probe")
    resp = create_vocabulary(name, default_turtle(name, f"https://data.example.org/probe/{name.lower()}#", 0))
    assert resp.status_code == 201, f"probe creation failed: {resp.status_code} {resp.text[:300]}"
    learn_content_base(name, resp.json().get("contentHref"))
    assert _content_base["value"], "could not learn the content base from contentHref"
    return _content_base["value"]


# ---------------------------------------------------------------- Turtle

PREFIXES = (
    "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
    "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
)


def default_turtle(name, ns, revision):
    return (
        PREFIXES
        + f"@prefix ex: <{ns}> .\n\n"
        + "ex:Scheme a skos:ConceptScheme ;\n"
        + f"    skos:prefLabel \"Scheme {name}\"@en .\n"
        + "ex:Concept1 a skos:Concept ;\n"
        + "    skos:inScheme ex:Scheme ;\n"
        + "    skos:prefLabel \"Concept one\"@en ;\n"
        + f"    rdfs:comment \"revision {revision}\" .\n"
    ).encode("utf-8")


def turtle_of_size(ns, size):
    """Valid Turtle (no validation errors or warnings) of exactly `size` bytes."""
    head = (PREFIXES + f"@prefix ex: <{ns}> .\n").encode()
    lines = [head]
    total = len(head)
    i = 0
    while True:
        line = f"ex:c{i:07d} skos:prefLabel \"Label {i:07d}\"@en .\n".encode()
        if total + len(line) > size - 64:
            break
        lines.append(line)
        total += len(line)
        i += 1
    rest = size - total
    assert rest >= 2
    lines.append(b"#" + b"x" * (rest - 2) + b"\n")
    data = b"".join(lines)
    assert len(data) == size, (len(data), size)
    return data


# ---------------------------------------------------------------- admin setup calls

def multipart(fields, file_bytes=None, file_field="vocabularyFile", filename="vocabulary.ttl"):
    parts = []
    for k, v in fields:
        parts.append((k, (None, v)))
    if file_bytes is not None:
        parts.append((file_field, (filename, file_bytes, "text/turtle")))
    return parts


def create_vocabulary(name, content, description=None, changelog="Initial version.", identity="admin"):
    fields = [("name", name), ("description", description or f"Description of {name}"),
              ("changelog", changelog)]
    resp = send("POST", "/vocabularies", identity, files=multipart(fields, content))
    if resp.status_code == 201:
        learn_content_base(name, resp.json().get("contentHref"))
    return resp


def create_version(name, content, description, changelog, identity="admin"):
    fields = [("description", description), ("changelog", changelog)]
    return send("POST", f"/vocabularies/{name}/versions", identity, files=multipart(fields, content))


def register_external(name, ns, content, aliases=(), description=None, source=None, identity="admin"):
    fields = [("name", name), ("namespaceUri", ns)]
    for a in aliases:
        fields.append(("aliasUris", a))
    if description is not None:
        fields.append(("description", description))
    if source is not None:
        fields.append(("sourceUri", source))
    return send("POST", "/external-vocabularies", identity, files=multipart(fields, content))


def patch_status(path, status, identity="admin"):
    return send("PATCH", path, identity, json={"status": status})


def json_path(body, path):
    """Resolve a dotted path with [index] parts: items[0].name."""
    cur = body
    if path in ("", "$"):
        return cur
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


def has_path(body, path):
    try:
        json_path(body, path)
        return True
    except KeyError:
        return False


def body_json(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError(f"response is not JSON: {resp.status_code} {resp.headers.get('Content-Type')} {resp.text[:300]}")


def describe(resp):
    return f"{resp.request.method} {resp.request.url} -> {resp.status_code} {resp.headers.get('Content-Type')} {resp.text[:600]}"


def dumps(o):
    return json.dumps(o, ensure_ascii=False)
