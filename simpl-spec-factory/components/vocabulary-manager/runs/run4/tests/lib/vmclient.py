"""Shared helpers for the black-box suite of the Vocabulary Manager.

Configuration comes only from environment variables, so the suite runs
unchanged against any implementation of the component.
"""
import base64
import json
import os
import random
import re
import string
import time
import uuid
from datetime import datetime, timezone

import requests

HERE = os.path.dirname(os.path.abspath(__file__))
FIXTURES = os.path.join(os.path.dirname(HERE), "fixtures")


class Config:
    def __init__(self):
        env = os.environ.get
        self.api_url = env("API_URL", "http://localhost:8196/v1").rstrip("/")
        self.token_url = env(
            "TOKEN_URL",
            "http://localhost:8193/realms/simpl/protocol/openid-connect/token",
        )
        self.token_client = env("TOKEN_CLIENT", "vocabulary-tests")
        self.token_client_secret = env("TOKEN_CLIENT_SECRET", "")
        self.password = env("TEST_PASSWORD", "Password1!")
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
        self.content_base = env(
            "VOCABULARY_CONTENT_BASE_URL", "https://authority.example/vocabulary-manager"
        ).rstrip("/")
        self.timeout = float(env("HTTP_TIMEOUT", "30"))
        self.max_upload = int(env("MAX_UPLOAD_BYTES", str(5 * 1024 * 1024)))


_TOKENS = {}


def get_token(cfg, user):
    """Password grant against the identity provider; cached until near expiry."""
    cached = _TOKENS.get(user)
    if cached and cached[1] > time.time() + 30:
        return cached[0]
    data = {
        "grant_type": "password",
        "client_id": cfg.token_client,
        "username": cfg.users.get(user, user),
        "password": cfg.password,
        "scope": "openid",
    }
    if cfg.token_client_secret:
        data["client_secret"] = cfg.token_client_secret
    r = requests.post(cfg.token_url, data=data, timeout=cfg.timeout)
    if r.status_code != 200:
        raise AssertionError(
            "Could not get a token for %s: %s %s" % (user, r.status_code, r.text[:300])
        )
    body = r.json()
    token = body["access_token"]
    _TOKENS[user] = (token, time.time() + int(body.get("expires_in", 60)))
    return token


def _b64url(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64url_decode(text):
    pad = "=" * (-len(text) % 4)
    return base64.urlsafe_b64decode(text + pad)


def invalid_token(cfg, kind):
    """Tokens that must be refused with 401 (D17, OWASP-08)."""
    if kind == "garbage":
        return "not-a-jwt"
    valid = get_token(cfg, "ga-admin")
    header, payload, signature = valid.split(".")
    if kind == "tampered":
        # Change the payload (role claim area) and keep the old signature.
        claims = json.loads(_b64url_decode(payload))
        claims["preferred_username"] = "intruder"
        return ".".join([header, _b64url(json.dumps(claims).encode()), signature])
    if kind == "unsigned":
        hdr = _b64url(json.dumps({"alg": "none", "typ": "JWT"}).encode())
        return hdr + "." + payload + "."
    if kind == "unknown key":
        hdr = _b64url(
            json.dumps({"alg": "RS256", "typ": "JWT", "kid": "unknown-" + uuid.uuid4().hex}).encode()
        )
        return hdr + "." + payload + "." + _b64url(os.urandom(256))
    if kind == "expired":
        claims = json.loads(_b64url_decode(payload))
        claims["exp"] = int(time.time()) - 3600
        claims["iat"] = int(time.time()) - 7200
        return ".".join([header, _b64url(json.dumps(claims).encode()), signature])
    raise ValueError("unknown token kind %s" % kind)


def new_run_id():
    return "".join(random.choice(string.ascii_lowercase + string.digits) for _ in range(10))


def utc_today():
    return datetime.now(timezone.utc).strftime("%Y-%m-%d")


def parse_dt(value):
    if not isinstance(value, str):
        raise AssertionError("not a date-time string: %r" % (value,))
    v = value.strip()
    if v.endswith("Z"):
        v = v[:-1] + "+00:00"
    # Python < 3.11 does not accept more than 6 fraction digits.
    m = re.match(r"^(.*T\d{2}:\d{2}:\d{2})(\.\d+)?(.*)$", v)
    if m and m.group(2) and len(m.group(2)) > 7:
        v = m.group(1) + m.group(2)[:7] + m.group(3)
    dt = datetime.fromisoformat(v)
    if dt.tzinfo is None:
        raise AssertionError("date-time without offset: %r" % value)
    return dt.astimezone(timezone.utc)


def to_second(value):
    return parse_dt(value).strftime("%Y-%m-%dT%H:%M:%SZ")


def read_fixture(name):
    with open(os.path.join(FIXTURES, name), "r", encoding="utf-8") as fh:
        return fh.read()


def json_path(body, path):
    """Very small path reader: a.b[0].c"""
    cur = body
    if path in ("", "$"):
        return cur
    for part in re.findall(r"[^.\[\]]+|\[\d+\]", path):
        if part.startswith("["):
            idx = int(part[1:-1])
            if not isinstance(cur, list) or idx >= len(cur):
                raise KeyError(path)
            cur = cur[idx]
        else:
            if not isinstance(cur, dict) or part not in cur:
                raise KeyError(path)
            cur = cur[part]
    return cur
