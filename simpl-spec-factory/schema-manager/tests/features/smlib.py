"""Support library for the Schema Manager black-box suite.

Holds the configuration, the token provider, the HTTP client helpers, the
webhook receiver, the Mailpit client and the Vocabulary Manager stub client.
Everything here talks HTTP only, so the suite runs against any implementation.
"""

import base64
import hashlib
import hmac
import json
import os
import random
import re
import string
import threading
import time
import uuid
from collections import defaultdict
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import quote, urlsplit

import requests

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures"

ROLES = {
    "ga-admin": ["GA_SCHEMA_ADMIN"],
    "ga-viewer": ["GA_SCHEMA_VIEWER"],
    "ga-norole": [],
}


def _env(name, default):
    value = os.environ.get(name)
    return default if value is None else value


class Config:
    """All settings, read from environment variables (see environment.py)."""

    def __init__(self):
        self.sm_base_url = _env("SM_BASE_URL", "http://localhost:8098/v1").rstrip("/")
        self.keycloak_url = _env("KEYCLOAK_URL", "http://localhost:8185").rstrip("/")
        self.realm = _env("KEYCLOAK_REALM", "simpl")
        self.client_id = _env("CLIENT_ID", "schema-tests")
        self.password = _env("TEST_USER_PASSWORD", "password")
        self.auth_mode = _env("AUTH_MODE", "oidc").lower()
        self.vocabulary_manager_url = _env("VOCABULARY_MANAGER_URL", "http://localhost:8099").rstrip("/")
        self.vocabulary_namespace_base = _env(
            "VOCABULARY_NAMESPACE_BASE", "https://vocabulary.simpl.test/content/")
        self.mail_api_url = _env("MAIL_API_URL", "http://localhost:8027").rstrip("/")
        self.provider_emails = [e.strip() for e in _env("PROVIDER_NOTIFICATION_EMAILS", "").split(",") if e.strip()]
        self.webhook_receiver_url = _env("WEBHOOK_RECEIVER_URL", "http://host.docker.internal:8299").rstrip("/")
        self.webhook_listen_port = int(_env("WEBHOOK_LISTEN_PORT", "8299"))
        self.vm_down_run = _env("SM_VM_DOWN_RUN", "false").lower() == "true"
        self.http_timeout = float(_env("HTTP_TIMEOUT", "30"))
        self.event_wait = float(_env("EVENT_WAIT_SECONDS", "10"))
        self.absence_wait = float(_env("ABSENCE_WAIT_SECONDS", "3"))
        self.default_property_ns = "https://simpl-programme.ec.europa.eu/ns/default#"
        # UI (step 4): see environment.py
        self.sm_ui_url = _env("SM_UI_URL", "http://localhost:4334").rstrip("/")
        self.ui_auth = _env("UI_AUTH", "oidc").lower()
        # API base URLs as the browser calls them: SM_UI_API_URL (comma-separated) if set, else
        # SM_BASE_URL and the same path on the UI origin (a UI may proxy the API on its own origin).
        explicit = [u.strip().rstrip("/") for u in _env("SM_UI_API_URL", "").split(",") if u.strip()]
        api_path = urlsplit(self.sm_base_url).path.rstrip("/")
        self.sm_ui_api_urls = explicit or ([self.sm_base_url] + ([self.sm_ui_url + api_path] if api_path else []))
        self.ui_headless = _env("UI_HEADLESS", "true").lower() != "false"
        self.ui_screenshot_dir = _env("UI_SCREENSHOT_DIR", "")

    def summary(self):
        return {k: v for k, v in vars(self).items() if k != "password"}


def _b64(data):
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()


def self_made_token(user, secret=b"not-validated", alg="HS256", expired=False):
    """A token the suite builds itself. Used when AUTH_MODE=none, and to
    produce tokens a validating service must reject (unsigned, wrong key)."""
    now = int(time.time())
    claims = {
        "iss": "http://localhost/realms/simpl",
        "sub": str(uuid.uuid5(uuid.NAMESPACE_DNS, user)),
        "preferred_username": user,
        "iat": now - 600 if expired else now,
        "exp": now - 300 if expired else now + 3600,
        "roles": ROLES.get(user, []),
        "realm_access": {"roles": ROLES.get(user, [])},
    }
    header = {"alg": alg, "typ": "JWT"}
    signing_input = _b64(json.dumps(header).encode()) + "." + _b64(json.dumps(claims).encode())
    if alg == "none":
        return signing_input + "."
    sig = hmac.new(secret, signing_input.encode(), hashlib.sha256).digest()
    return signing_input + "." + _b64(sig)


class TokenProvider:
    def __init__(self, cfg):
        self.cfg = cfg
        self.cache = {}

    def token(self, user):
        if self.cfg.auth_mode == "none":
            return self_made_token(user)
        cached = self.cache.get(user)
        if cached and cached[1] > time.time() + 30:
            return cached[0]
        url = f"{self.cfg.keycloak_url}/realms/{self.cfg.realm}/protocol/openid-connect/token"
        resp = requests.post(url, data={
            "grant_type": "password",
            "client_id": self.cfg.client_id,
            "username": user,
            "password": self.cfg.password,
        }, timeout=self.cfg.http_timeout)
        if resp.status_code != 200:
            raise AssertionError(f"Token request for {user} failed: {resp.status_code} {resp.text[:300]}")
        body = resp.json()
        self.cache[user] = (body["access_token"], time.time() + int(body.get("expires_in", 60)))
        return body["access_token"]

    def tampered(self, user):
        tok = self.token(user)
        head, payload, sig = tok.split(".")
        sig = (sig[:-4] + ("AAAA" if not sig.endswith("AAAA") else "BBBB")) if len(sig) > 4 else "AAAA"
        return f"{head}.{payload}.{sig}"


def run_id():
    if os.environ.get("SM_RUN_ID"):
        return os.environ["SM_RUN_ID"]
    return "".join(random.choice(string.ascii_uppercase + string.digits) for _ in range(6))


TOKEN_RE = re.compile(r"\$([A-Z][A-Z0-9_]*)")


class Names:
    """Maps aliases used in the feature files to unique values (D61)."""

    def __init__(self, run, seq, cfg):
        self.prefix = f"{run}{seq:04d}"
        self.cfg = cfg
        self.values = {}

    def value(self, token):
        if token == "NS":
            return self.cfg.vocabulary_namespace_base
        if token == "DP":
            return self.cfg.default_property_ns
        if token not in self.values:
            if token.startswith("RT"):
                self.values[token] = f"Rt{self.prefix}{token[2:]}"
            elif token.startswith("VOC"):
                self.values[token] = f"V{self.prefix}{token[3:]}"
            else:
                self.values[token] = f"T{self.prefix}{token}"
        return self.values[token]

    def sub(self, text):
        if text is None:
            return None
        return TOKEN_RE.sub(lambda m: self.value(m.group(1)), text)


def fixture_bytes(name, names):
    raw = (FIXTURES / name).read_bytes()
    if name.endswith(".ttl") or name.endswith(".txt") or name.endswith(".json"):
        return names.sub(raw.decode("utf-8")).encode("utf-8")
    return raw


# ---------------------------------------------------------------------------
# Webhook receiver
# ---------------------------------------------------------------------------

class WebhookReceiver:
    """Records every POST it receives, per path. Can be told to fail the
    first N deliveries on a path (to observe retries, D44)."""

    def __init__(self, port):
        self.port = port
        self.lock = threading.Lock()
        self.deliveries = defaultdict(list)
        self.failures = {}
        receiver = self

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                length = int(self.headers.get("Content-Length") or 0)
                body = self.rfile.read(length) if length else b""
                with receiver.lock:
                    left = receiver.failures.get(self.path, 0)
                    status = 500 if left > 0 else 200
                    if left > 0:
                        receiver.failures[self.path] = left - 1
                    receiver.deliveries[self.path].append({
                        "headers": {k.lower(): v for k, v in self.headers.items()},
                        "body": body,
                        "answered": status,
                        "at": time.time(),
                    })
                self.send_response(status)
                self.send_header("Content-Length", "0")
                self.end_headers()

            def do_GET(self):
                self.send_response(200)
                self.send_header("Content-Length", "2")
                self.end_headers()
                self.wfile.write(b"ok")

            def log_message(self, *args):
                pass

        self.server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    def start(self):
        self.thread.start()

    def stop(self):
        self.server.shutdown()
        self.server.server_close()

    def fail_first(self, path, n):
        with self.lock:
            self.failures[path] = n

    def received(self, path):
        with self.lock:
            return list(self.deliveries.get(path, []))


def parse_event(delivery):
    try:
        return json.loads(delivery["body"].decode("utf-8"))
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Mailpit
# ---------------------------------------------------------------------------

class Mailpit:
    def __init__(self, base):
        self.base = base

    def messages_mentioning(self, text):
        resp = requests.get(f"{self.base}/api/v1/search", params={"query": text, "limit": 200}, timeout=15)
        resp.raise_for_status()
        msgs = resp.json().get("messages") or []
        return [m for m in msgs if text in (m.get("Subject") or "")]

    def text(self, msg_id):
        resp = requests.get(f"{self.base}/api/v1/message/{quote(msg_id)}", timeout=15)
        resp.raise_for_status()
        return resp.json()


# ---------------------------------------------------------------------------
# Vocabulary Manager stub (D60)
# ---------------------------------------------------------------------------

def seed_vocabulary(cfg, name, turtle):
    resp = requests.put(f"{cfg.vocabulary_manager_url}/vocabularies/{quote(name)}/content",
                        data=turtle.encode("utf-8"), headers={"Content-Type": "text/turtle"},
                        timeout=cfg.http_timeout)
    if resp.status_code not in (200, 201, 204):
        raise AssertionError(f"Seeding vocabulary {name} failed: {resp.status_code} {resp.text[:200]}")


def wait_until(predicate, timeout, interval=0.3):
    end = time.time() + timeout
    while True:
        result = predicate()
        if result:
            return result
        if time.time() >= end:
            return result
        time.sleep(interval)
