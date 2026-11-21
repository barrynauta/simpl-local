"""Black-box HTTP client helpers for the Vocabulary Manager suite.

Configuration comes only from environment variables (see README.md), so the
suite runs unchanged against any implementation of the component.
"""
import base64
import json
import os
import random
import string
import threading
import time

import requests

HERE = os.path.dirname(os.path.abspath(__file__))
FIXTURES = os.path.join(os.path.dirname(HERE), "fixtures")


def env(name, default):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


API_URL = env("API_URL", "http://localhost:8196/v1").rstrip("/")
STORE_DOWN_API_URL = env("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")
TOKEN_URL = env("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_CLIENT = env("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_NOAUD = env("TOKEN_CLIENT_NOAUD", "vocabulary-tests-noaud")
TOKEN_URL_OTHER = env(
    "TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token")
TOKEN_CLIENT_OTHER = env("TOKEN_CLIENT_OTHER", "vocabulary-tests")
PASSWORD = env("USER_PASSWORD", "Password1!")
USERS = {
    "ga-admin": env("USER_ADMIN", "ga-admin"),
    "ga-viewer": env("USER_VIEWER", "ga-viewer"),
    "ga-norole": env("USER_NOROLE", "ga-norole"),
}
EMAILS = {
    "ga-admin": env("USER_ADMIN_EMAIL", "ga-admin@example.com"),
    "ga-viewer": env("USER_VIEWER_EMAIL", "ga-viewer@example.com"),
    "ga-norole": env("USER_NOROLE_EMAIL", "ga-norole@example.com"),
}
TIMEOUT = float(env("HTTP_TIMEOUT", "60"))
UPLOAD_TIMEOUT = float(env("UPLOAD_TIMEOUT", "90"))

_token_cache = {}
_token_lock = threading.Lock()


def run_id(length=10):
    alphabet = string.ascii_letters + string.digits
    return "".join(random.choice(alphabet) for _ in range(length))


def fetch_token(user, token_url=None, client_id=None):
    """Password grant against Keycloak. Cached for 60 s per (user, url, client)."""
    token_url = token_url or TOKEN_URL
    client_id = client_id or TOKEN_CLIENT
    key = (user, token_url, client_id)
    with _token_lock:
        hit = _token_cache.get(key)
        if hit and hit[1] > time.time():
            return hit[0]
    resp = requests.post(
        token_url,
        data={
            "grant_type": "password",
            "client_id": client_id,
            "username": USERS.get(user, user),
            "password": PASSWORD,
            "scope": "openid",
        },
        timeout=TIMEOUT,
    )
    if resp.status_code != 200:
        raise AssertionError(
            f"Token request for {user} at {token_url} ({client_id}) failed: "
            f"{resp.status_code} {resp.text[:300]}")
    token = resp.json()["access_token"]
    with _token_lock:
        _token_cache[key] = (token, time.time() + 60)
    return token


def _b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def _b64url_decode(text):
    pad = "=" * (-len(text) % 4)
    return base64.urlsafe_b64decode(text + pad)


def bad_token(kind):
    """Tokens that must be refused with 401 (D17, E5, E7)."""
    if kind == "garbage":
        return "not-a-jwt"
    if kind == "edited-claims":
        token = fetch_token("ga-admin")
        head, payload, sig = token.split(".")
        claims = json.loads(_b64url_decode(payload))
        claims["preferred_username"] = "intruder"
        return ".".join([head, _b64url(json.dumps(claims).encode()), sig])
    if kind == "bad-signature":
        token = fetch_token("ga-admin")
        head, payload, sig = token.split(".")
        raw = bytearray(_b64url_decode(sig))
        raw[0] ^= 0xFF
        return ".".join([head, payload, _b64url(bytes(raw))])
    if kind == "unsigned":
        token = fetch_token("ga-admin")
        _head, payload, _sig = token.split(".")
        head = _b64url(json.dumps({"alg": "none", "typ": "JWT"}).encode())
        return head + "." + payload + "."
    if kind == "edited-expiry":
        token = fetch_token("ga-admin")
        head, payload, sig = token.split(".")
        claims = json.loads(_b64url_decode(payload))
        claims["exp"] = int(time.time()) - 3600
        return ".".join([head, _b64url(json.dumps(claims).encode()), sig])
    if kind == "no-audience":
        return fetch_token("ga-admin", client_id=TOKEN_CLIENT_NOAUD)
    if kind == "other-issuer":
        return fetch_token("ga-admin", token_url=TOKEN_URL_OTHER, client_id=TOKEN_CLIENT_OTHER)
    raise ValueError(f"unknown bad token kind {kind}")


def url_for(path, base=None):
    if path.startswith("http://") or path.startswith("https://"):
        return path
    return (base or API_URL) + path


def send(method, path, token=None, headers=None, base=None, timeout=None, **kwargs):
    """Send one request without any client-side normalisation of the URL."""
    hdrs = dict(headers or {})
    if token:
        hdrs["Authorization"] = "Bearer " + token
    session = requests.Session()
    req = requests.Request(method, url_for(path, base), headers=hdrs, **kwargs)
    prepared = session.prepare_request(req)
    # Keep the path exactly as written in the scenario (for example %2F).
    prepared.url = url_for(path, base)
    if "params" in kwargs and kwargs["params"]:
        prepared.prepare_url(prepared.url, kwargs["params"])
    return session.send(prepared, timeout=timeout or TIMEOUT, allow_redirects=False)


def turtle_of_size(size, run="x"):
    """Valid Turtle (one triple plus comment padding) of exactly `size` bytes."""
    head = (
        f"@prefix ex: <https://example.org/vm/{run}/big#> .\n"
        f"ex:Big ex:label \"big file\" .\n"
    ).encode()
    if size < len(head) + 2:
        raise ValueError("size too small")
    line = b"#" + b"x" * 98 + b"\n"  # 100 bytes per comment line
    data = head + line * ((size - len(head)) // 100)
    remainder = size - len(data)
    if remainder >= 2:
        data += b"#" + b"x" * (remainder - 2) + b"\n"
    elif remainder == 1:
        data += b"\n"
    assert len(data) == size
    return data


def read_fixture(name):
    with open(os.path.join(FIXTURES, name), "rb") as fh:
        return fh.read()
