"""HTTP client, token handling, multipart encoding and test data helpers.

Black box only: everything is configured through environment variables, so the
suite runs unchanged against any implementation of the Vocabulary Manager API.
"""

import base64
import json
import os
import random
import re
import string
import threading
import time
import uuid
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "fixtures"


def env(name, default=None):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


class Config:
    def __init__(self):
        self.api_url = env("API_URL", "http://localhost:8196/v1").rstrip("/")
        self.store_down_api_url = env("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")
        self.token_url = env("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
        self.token_client = env("TOKEN_CLIENT", "vocabulary-tests")
        self.token_client_noaud = env("TOKEN_CLIENT_NOAUD", "vocabulary-tests-noaud")
        self.token_url_other = env(
            "TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token")
        self.password = env("USER_PASSWORD", "Password1!")
        self.users = {
            "ga-admin": env("ADMIN_USER", "ga-admin"),
            "ga-viewer": env("VIEWER_USER", "ga-viewer"),
            "ga-norole": env("NOROLE_USER", "ga-norole"),
        }
        self.emails = {
            "ga-admin": env("ADMIN_EMAIL", "ga-admin@example.com"),
            "ga-viewer": env("VIEWER_EMAIL", "ga-viewer@example.com"),
            "ga-norole": env("NOROLE_EMAIL", "ga-norole@example.com"),
        }
        self.content_base = env("CONTENT_BASE_URL")  # discovered from contentHref when unset
        self.timeout = float(env("HTTP_TIMEOUT", "90"))
        self.run_id = env("RUN_ID") or "".join(
            random.choice(string.ascii_lowercase + string.digits) for _ in range(6))


CONFIG = Config()

_token_cache = {}
_token_lock = threading.Lock()


def fetch_token(user, token_url=None, client=None):
    token_url = token_url or CONFIG.token_url
    client = client or CONFIG.token_client
    key = (token_url, client, user)
    with _token_lock:
        cached = _token_cache.get(key)
        if cached and cached[1] > time.time() + 20:
            return cached[0]
    resp = requests.post(token_url, data={
        "grant_type": "password",
        "client_id": client,
        "username": CONFIG.users.get(user, user),
        "password": CONFIG.password,
        "scope": "openid",
    }, timeout=CONFIG.timeout)
    if resp.status_code != 200:
        raise AssertionError(f"token request for {user} at {token_url} ({client}) failed: "
                             f"{resp.status_code} {resp.text[:300]}")
    body = resp.json()
    token = body["access_token"]
    with _token_lock:
        _token_cache[key] = (token, time.time() + int(body.get("expires_in", 60)))
    return token


def _b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _b64url_decode(text):
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def invalid_token(kind):
    """Tokens that the service must refuse with 401 (D17, E5, E7, D88)."""
    if kind == "garbage":
        return "not-a-jwt"
    if kind == "noaud":
        return fetch_token("ga-admin", client=CONFIG.token_client_noaud)
    if kind == "otherissuer":
        return fetch_token("ga-admin", token_url=CONFIG.token_url_other, client=CONFIG.token_client)
    good = fetch_token("ga-admin")
    header, payload, signature = good.split(".")
    if kind == "tampered":
        claims = json.loads(_b64url_decode(payload))
        claims["realm_access"] = {"roles": ["GA_VOCABULARY_ADMIN", "GA_VOCABULARY_VIEWER", "extra"]}
        claims["preferred_username"] = "intruder"
        return f"{header}.{_b64url(json.dumps(claims).encode())}.{signature}"
    if kind == "unsigned":
        hdr = {"alg": "none", "typ": "JWT"}
        return f"{_b64url(json.dumps(hdr).encode())}.{payload}."
    if kind == "unknownkid":
        hdr = json.loads(_b64url_decode(header))
        hdr["kid"] = "unknown-" + uuid.uuid4().hex
        return f"{_b64url(json.dumps(hdr).encode())}.{payload}.{signature}"
    raise AssertionError(f"unknown invalid token kind {kind}")


def encode_multipart(parts):
    """parts: list of dicts with name, data (bytes), optional filename and content_type.

    Hand-made so that tests can send repeated parts, empty part names and
    arbitrary sizes exactly as written.
    """
    boundary = "vmtest" + uuid.uuid4().hex
    chunks = []
    for part in parts:
        disp = f'form-data; name="{part["name"]}"'
        if part.get("filename") is not None:
            disp += f'; filename="{part["filename"]}"'
        head = f"--{boundary}\r\nContent-Disposition: {disp}\r\n"
        if part.get("content_type"):
            head += f"Content-Type: {part['content_type']}\r\n"
        head += "\r\n"
        data = part["data"]
        if isinstance(data, str):
            data = data.encode("utf-8")
        chunks.append(head.encode("utf-8"))
        chunks.append(data)
        chunks.append(b"\r\n")
    chunks.append(f"--{boundary}--\r\n".encode())
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def http(method, url, token=None, headers=None, body=None, content_type=None, timeout=None):
    hdrs = {"Accept": "application/json"}
    if headers:
        hdrs.update(headers)
    if token:
        hdrs["Authorization"] = f"Bearer {token}"
    if content_type:
        hdrs["Content-Type"] = content_type
    # An empty value means "do not send this header"; requests drops headers set to None,
    # including its own default Accept.
    hdrs = {k: (None if v == "" else v) for k, v in hdrs.items()}
    return requests.request(method, url, headers=hdrs, data=body,
                            timeout=timeout or CONFIG.timeout, allow_redirects=False)


def load_fixture(name):
    path = FIXTURES / "ttl" / f"{name}.ttl"
    return path.read_bytes()


TOKEN_RE = re.compile(r"\$\{([A-Za-z]+)(?::([^}]*))?\}")


def turtle_of_size(size, namespace):
    """A valid Turtle document of exactly `size` bytes: one triple plus comment padding."""
    head = (f"@prefix ex: <{namespace}> .\n"
            f"@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
            f"ex:Scheme a skos:ConceptScheme .\n").encode()
    if size < len(head):
        raise AssertionError("size too small for a valid Turtle document")
    remaining = size - len(head)
    lines = []
    line = b"# " + b"p" * 1021 + b"\n"  # 1024 bytes per padding line
    while remaining >= len(line):
        lines.append(line)
        remaining -= len(line)
    if remaining > 0:
        if remaining == 1:
            lines.append(b"\n")
        else:
            lines.append(b"#" + b"p" * (remaining - 2) + b"\n")
    data = head + b"".join(lines)
    assert len(data) == size, (len(data), size)
    return data


def problem_body(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError(f"response body is not JSON: {resp.status_code} {resp.text[:300]}")
