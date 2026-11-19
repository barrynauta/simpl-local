"""HTTP client, tokens, configuration and test data for the Vocabulary Manager suite.

Configuration comes only from environment variables, so the suite runs unchanged
against any implementation of the component (decision E4).
"""
import base64
import json
import os
import re
import threading
import time
import uuid

import requests

API_URL = os.environ.get("API_URL", "http://localhost:8196/v1").rstrip("/")
TOKEN_URL = os.environ.get(
    "TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token"
)
TOKEN_CLIENT = os.environ.get("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_SECRET = os.environ.get("TOKEN_CLIENT_SECRET", "")
TEST_PASSWORD = os.environ.get("TEST_PASSWORD", "Password1!")
CONTENT_BASE = os.environ.get(
    "VOCABULARY_CONTENT_BASE_URL", "https://authority.example/vocabulary-manager"
).rstrip("/")
REQUEST_TIMEOUT = float(os.environ.get("REQUEST_TIMEOUT", "90"))

# Test users of decision E1. The label is what the feature files use.
USERS = {
    "ga-admin": {
        "username": os.environ.get("ADMIN_USER", "ga-admin"),
        "email": os.environ.get("ADMIN_EMAIL", "ga-admin@example.com"),
    },
    "ga-viewer": {
        "username": os.environ.get("VIEWER_USER", "ga-viewer"),
        "email": os.environ.get("VIEWER_EMAIL", "ga-viewer@example.com"),
    },
    "ga-norole": {
        "username": os.environ.get("NOROLE_USER", "ga-norole"),
        "email": os.environ.get("NOROLE_EMAIL", "ga-norole@example.com"),
    },
}

# Labels for callers that do not hold a valid token.
NO_TOKEN = "anonymous"
SPECIAL_CALLERS = {"anonymous", "invalid-token", "tampered-token", "unsigned-token"}

FIVE_MB = 5 * 1024 * 1024  # 5 242 880 bytes, decision D15
SIX_MB = 6 * 1024 * 1024  # whole multipart request cap, decision D76

_token_lock = threading.Lock()
_token_cache = {}


def get_token(label):
    """Password grant at the Keycloak token endpoint (build profile, decision D40)."""
    user = USERS[label]
    with _token_lock:
        cached = _token_cache.get(label)
        if cached and cached[1] > time.time() + 20:
            return cached[0]
        data = {
            "grant_type": "password",
            "client_id": TOKEN_CLIENT,
            "username": user["username"],
            "password": TEST_PASSWORD,
            "scope": "openid",
        }
        if TOKEN_CLIENT_SECRET:
            data["client_secret"] = TOKEN_CLIENT_SECRET
        resp = requests.post(TOKEN_URL, data=data, timeout=30)
        if resp.status_code != 200:
            raise AssertionError(
                f"token request for {label} failed: {resp.status_code} {resp.text[:300]}"
            )
        body = resp.json()
        token = body["access_token"]
        _token_cache[label] = (token, time.time() + float(body.get("expires_in", 60)))
        return token


def _b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _b64url_decode(text):
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def special_token(label):
    if label == "invalid-token":
        return "not-a-jwt"
    admin = get_token("ga-admin")
    header, payload, signature = admin.split(".")
    claims = json.loads(_b64url_decode(payload))
    if label == "tampered-token":
        # Same signature, changed claims: the signature check must fail.
        claims["preferred_username"] = "intruder"
        claims["exp"] = int(time.time()) + 3600
        return ".".join([header, _b64url(json.dumps(claims).encode()), signature])
    if label == "unsigned-token":
        none_header = _b64url(json.dumps({"alg": "none", "typ": "JWT"}).encode())
        return ".".join([none_header, payload, ""])
    raise ValueError(label)


def auth_headers(caller):
    if caller in (None, NO_TOKEN):
        return {}
    if caller in SPECIAL_CALLERS:
        return {"Authorization": "Bearer " + special_token(caller)}
    return {"Authorization": "Bearer " + get_token(caller)}


def url_for(path):
    if path.startswith("http://") or path.startswith("https://"):
        return path
    if not path.startswith("/"):
        path = "/" + path
    return API_URL + path


def call(method, path, caller="ga-admin", headers=None, base_url=None, **kwargs):
    hdrs = {}
    hdrs.update(auth_headers(caller))
    if headers:
        hdrs.update(headers)
    kwargs.setdefault("timeout", REQUEST_TIMEOUT)
    url = (base_url.rstrip("/") + path) if base_url else url_for(path)
    return requests.request(method, url, headers=hdrs, **kwargs)


def new_run_id():
    # Lower-case hex, so it fits the name pattern after a capital letter
    # and the relaxed name filter of D19.
    return uuid.uuid4().hex[:10]


# ---------------------------------------------------------------- Turtle content

PREFIXES = (
    "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
    "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
    "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
    "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
)


def own_namespace(rid, alias):
    return f"http://example.org/{rid}/{alias}#"


def valid_ttl(rid, alias, marker="1"):
    """Valid Turtle with only W3C namespaces and the vocabulary's own namespace:
    no validation errors and no validation warnings (D9, D12)."""
    ns = own_namespace(rid, alias)
    return (
        f"# {alias} revision {marker}\n"
        + PREFIXES
        + f"@prefix ex: <{ns}> .\n"
        + f"ex:Scheme a skos:ConceptScheme ;\n  skos:prefLabel \"{alias} scheme\"@en .\n"
        + f"ex:Concept{marker} a skos:Concept ;\n  skos:inScheme ex:Scheme ;\n"
        + f"  skos:prefLabel \"Concept {marker}\"@en .\n"
    ).encode("utf-8")


def sized_ttl(rid, alias, size, marker="big"):
    """Valid Turtle of exactly `size` bytes, padded with comment lines."""
    base = valid_ttl(rid, alias, marker)
    if len(base) > size:
        raise ValueError("size too small")
    remaining = size - len(base)
    lines = []
    line = b"# " + b"p" * 1021 + b"\n"  # 1024 bytes per line
    while remaining >= len(line):
        lines.append(line)
        remaining -= len(line)
    if remaining > 0:
        if remaining == 1:
            lines.append(b"\n")
        else:
            lines.append(b"#" + b"p" * (remaining - 2) + b"\n")
    data = base + b"".join(lines)
    assert len(data) == size, (len(data), size)
    return data


# ---------------------------------------------------------------- JSON helpers

class MissingField(AssertionError, KeyError):
    """A field the test expects is absent: an assertion failure, not an error."""


_TOKEN_RE = re.compile(r"([^.\[\]]+)|\[(\d+)\]")


def json_get(body, path):
    """Read a dotted path such as items[0].name. Raises KeyError when absent."""
    cur = body
    if path in ("", "."):
        return cur
    for name, index in _TOKEN_RE.findall(path):
        if name:
            if not isinstance(cur, dict) or name not in cur:
                raise MissingField(f"field {path!r} is absent")
            cur = cur[name]
        else:
            i = int(index)
            if not isinstance(cur, list) or i >= len(cur):
                raise MissingField(f"field {path!r} is absent")
            cur = cur[i]
    return cur


def json_has(body, path):
    try:
        json_get(body, path)
        return True
    except KeyError:
        return False
