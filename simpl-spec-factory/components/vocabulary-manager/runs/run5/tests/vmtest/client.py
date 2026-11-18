"""Shared HTTP client, token handling, data builders and text substitution.

Configuration comes only from environment variables (see README.md), so the
suite runs unchanged against any implementation of the Vocabulary Manager.
"""
import base64
import json
import os
import random
import re
import string
import threading
import time
from datetime import datetime, timezone

import requests

API_URL = os.environ.get("API_URL", "http://localhost:8196/v1").rstrip("/")
TOKEN_URL = os.environ.get(
    "TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_CLIENT = os.environ.get("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_SECRET = os.environ.get("TOKEN_CLIENT_SECRET")
TOKEN_SCOPE = os.environ.get("TOKEN_SCOPE", "openid")
HTTP_TIMEOUT = float(os.environ.get("HTTP_TIMEOUT", "30"))
STORE_DOWN_API_URL = os.environ.get("STORE_DOWN_API_URL", "").rstrip("/")
DEFAULT_PASSWORD = os.environ.get("USER_PASSWORD", "Password1!")

# Role key -> (user name, password). Decision E1 users are the defaults.
USERS = {
    "GA_VOCABULARY_ADMIN": (os.environ.get("ADMIN_USER", "ga-admin"),
                            os.environ.get("ADMIN_PASSWORD", DEFAULT_PASSWORD)),
    "GA_VOCABULARY_VIEWER": (os.environ.get("VIEWER_USER", "ga-viewer"),
                             os.environ.get("VIEWER_PASSWORD", DEFAULT_PASSWORD)),
    "NONE": (os.environ.get("NOROLE_USER", "ga-norole"),
             os.environ.get("NOROLE_PASSWORD", DEFAULT_PASSWORD)),
}

ADMIN = "GA_VOCABULARY_ADMIN"
FIXTURES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "fixtures")
DEFAULT_CONTENT_BASE = "https://authority.example/vocabulary-manager"
UPLOAD_LIMIT = 5 * 1024 * 1024  # 5 242 880 bytes, decision D15

_token_lock = threading.Lock()
_tokens = {}
_derived_content_base = {"value": os.environ.get("VOCABULARY_CONTENT_BASE_URL")}


# --------------------------------------------------------------------------- tokens

def token_for(role):
    """Password-grant token for the user that holds `role` (cached until near expiry)."""
    with _token_lock:
        cached = _tokens.get(role)
        if cached and cached[1] - time.time() > 30:
            return cached[0]
        user, password = USERS[role]
        data = {"grant_type": "password", "client_id": TOKEN_CLIENT,
                "username": user, "password": password, "scope": TOKEN_SCOPE}
        if TOKEN_CLIENT_SECRET:
            data["client_secret"] = TOKEN_CLIENT_SECRET
        r = requests.post(TOKEN_URL, data=data, timeout=HTTP_TIMEOUT)
        assert r.status_code == 200, f"token request for {user} failed: {r.status_code} {r.text[:300]}"
        body = r.json()
        token = body["access_token"]
        _tokens[role] = (token, time.time() + int(body.get("expires_in", 60)))
        return token


def jwt_claims(token):
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload))


def creator_of(role):
    """Expected createdBy for a user (decision D27: email, else preferred_username)."""
    claims = jwt_claims(token_for(role))
    return claims.get("email") or claims.get("preferred_username")


def tampered(token):
    head, payload, sig = token.split(".")
    sig = ("A" if sig[0] != "A" else "B") + sig[1:]
    claims = jwt_claims(token)
    claims["realm_access"] = {"roles": ["GA_VOCABULARY_ADMIN"]}
    new_payload = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
    return f"{head}.{new_payload}.{sig}"


def unsigned(token):
    claims = jwt_claims(token)
    head = base64.urlsafe_b64encode(b'{"alg":"none","typ":"JWT"}').decode().rstrip("=")
    body = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
    return f"{head}.{body}."


def expired_looking(token):
    """A token whose exp lies in the past; the signature no longer matches either."""
    head, _, sig = token.split(".")
    claims = jwt_claims(token)
    claims["exp"] = int(time.time()) - 3600
    claims["iat"] = int(time.time()) - 7200
    body = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
    return f"{head}.{body}.{sig}"


# --------------------------------------------------------------------------- state

def new_run_id():
    first = random.choice(string.ascii_lowercase)
    return first + "".join(random.choices(string.ascii_lowercase + string.digits, k=9))


def reset_state(context):
    context.run = new_run_id()
    context.api_url = API_URL
    context.auth = ("role", ADMIN)
    context.response = None
    context.responses = []
    context.uploaded = {}      # (name, version) -> bytes
    context.ext_uploaded = {}  # name -> bytes
    context.ext_ns = {}        # name -> namespace URI as registered
    context.saved = {}
    context.counter = 0


def remember_content_base(content_href, name, version=None):
    if _derived_content_base["value"] or not content_href:
        return
    suffix = f"/content/{name}" + (f"/versions/{version}" if version else "")
    if content_href.endswith(suffix):
        _derived_content_base["value"] = content_href[: -len(suffix)]


def content_base():
    return _derived_content_base["value"] or DEFAULT_CONTENT_BASE


# --------------------------------------------------------------------------- substitution

_PLACEHOLDER = re.compile(r"\{([A-Za-z][A-Za-z0-9_]*)(?::([^{}\"\s]*))?\}")


def alias_name(context, alias):
    return alias + context.run


def sub(context, text):
    """Expand placeholders in feature text.

    {Alpha}            -> 'Alpha' + run id (a vocabulary or external vocabulary name)
    {run}              -> the scenario run id
    {upper:Alpha}      -> the name of Alpha in upper case
    {content:Alpha}    -> <contentBase>/content/<name of Alpha>
    {content:Alpha:2}  -> <contentBase>/content/<name of Alpha>/versions/2
    {contentBase}      -> the content base URL
    {ns:Ext}           -> namespace URI the external vocabulary Ext was registered with
    {chars:N}          -> N times the letter 'a'
    {blank}            -> three spaces
    {today}            -> today's UTC date (YYYY-MM-DD)
    {saved:key}        -> a value remembered earlier in the scenario
    {creator:ROLE}     -> createdBy expected for the user with that role (D27)
    """
    if text is None:
        return None

    def repl(m):
        key, arg = m.group(1), m.group(2)
        if key == "run" and arg is None:
            return context.run
        if key == "chars":
            return "a" * int(arg)
        if key == "blank" and arg is None:
            return "   "
        if key == "today" and arg is None:
            return datetime.now(timezone.utc).strftime("%Y-%m-%d")
        if key == "contentBase" and arg is None:
            return content_base()
        if key == "upper":
            return alias_name(context, arg).upper()
        if key == "lower":
            return alias_name(context, arg).lower()
        if key == "content":
            parts = arg.split(":")
            url = f"{content_base()}/content/{alias_name(context, parts[0])}"
            if len(parts) > 1:
                url += f"/versions/{parts[1]}"
            return url
        if key == "ns":
            return context.ext_ns[alias_name(context, arg)]
        if key == "creator":
            return creator_of(arg)
        if key == "saved":
            return str(context.saved[arg])
        if arg is None and key[0].isupper():
            return alias_name(context, key)
        return m.group(0)

    return _PLACEHOLDER.sub(repl, text)


# --------------------------------------------------------------------------- content builders

def default_ttl(name, extra_concept=None):
    lines = [
        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .",
        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .",
        f"@prefix ex: <https://vocab.example.org/{name}#> .",
        "",
        "ex:Scheme a skos:ConceptScheme ;",
        f"  skos:prefLabel \"{name} scheme\"@en .",
        "ex:ConceptA a skos:Concept ;",
        "  skos:inScheme ex:Scheme ;",
        "  skos:prefLabel \"Concept A\"@en .",
    ]
    if extra_concept is not None:
        lines += [f"ex:Concept{extra_concept} a skos:Concept ;",
                  "  skos:inScheme ex:Scheme ;",
                  f"  skos:prefLabel \"Concept {extra_concept}\"@en ."]
    return ("\n".join(lines) + "\n").encode("utf-8")


def padded_ttl(name, size):
    """Valid Turtle of exactly `size` bytes (comment lines pad the default content)."""
    base = default_ttl(name)
    pad = size - len(base)
    assert pad >= 2, "size too small"
    chunks = []
    while pad > 0:
        n = min(pad, 1000)
        if n == 1:
            chunks.append(b"\n")
        else:
            chunks.append(b"#" + b"x" * (n - 2) + b"\n")
        pad -= n
    data = base + b"".join(chunks)
    assert len(data) == size
    return data


def fixture_bytes(context, fixture):
    path = os.path.join(FIXTURES, "turtle", fixture)
    with open(path, "r", encoding="utf-8") as fh:
        return sub(context, fh.read()).encode("utf-8")


def file_value(context, spec, name):
    """Turn a vocabularyFile spec from a feature into bytes.

    default | fixture:<file> | size:<bytes> | empty | text:<raw turtle>
    """
    spec = spec.strip()
    if spec == "default":
        return default_ttl(name)
    if spec.startswith("fixture:"):
        return fixture_bytes(context, spec[len("fixture:"):])
    if spec.startswith("size:"):
        return padded_ttl(name or "Padding", int(spec[len("size:"):]))
    if spec == "empty":
        return b""
    if spec.startswith("text:"):
        return sub(context, spec[len("text:"):]).encode("utf-8")
    raise AssertionError(f"unknown file spec {spec}")


# --------------------------------------------------------------------------- HTTP

def auth_headers(context, auth=None):
    auth = auth or context.auth
    kind, value = auth
    if kind == "none":
        return {}
    if kind == "role":
        return {"Authorization": f"Bearer {token_for(value)}"}
    if kind == "raw":
        return {"Authorization": value}
    raise AssertionError(auth)


def request(context, method, path, auth=None, base=None, **kwargs):
    url = path if path.startswith("http") else (base or context.api_url) + path
    headers = dict(auth_headers(context, auth))
    headers.update(kwargs.pop("headers", {}) or {})
    return requests.request(method, url, headers=headers, timeout=HTTP_TIMEOUT, **kwargs)


def multipart(fields):
    """fields: list of (name, value) where value is str or ('file', bytes)."""
    parts = []
    for key, value in fields:
        if isinstance(value, tuple) and value[0] == "file":
            parts.append((key, ("vocabulary.ttl", value[1], "text/turtle")))
        else:
            parts.append((key, (None, value)))
    return parts


def create_fields(name, content, description=None, changelog="Initial version."):
    return [("name", name),
            ("description", description if description is not None else f"Description of {name}."),
            ("changelog", changelog),
            ("vocabularyFile", ("file", content))]


def version_fields(content, description, changelog):
    return [("description", description), ("changelog", changelog),
            ("vocabularyFile", ("file", content))]


def track_created(context, response, content):
    """Remember uploaded bytes of a created vocabulary version."""
    if response.status_code != 201:
        return
    try:
        body = response.json()
    except ValueError:
        return
    name, version = body.get("name"), body.get("version")
    if name and version:
        context.uploaded[(name, int(version))] = content
        remember_content_base(body.get("contentHref"), name, version)


def problem(response):
    try:
        return response.json()
    except ValueError:
        return {}


def describe(response):
    if response is None:
        return "<no response>"
    return f"{response.request.method} {response.request.url} -> {response.status_code} {response.text[:800]}"
