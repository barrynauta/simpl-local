"""HTTP client used by all steps. HTTP only, so the suite runs against any implementation."""
import json
import time

import requests

from .config import USERS


class Response:
    def __init__(self, raw, elapsed):
        self.raw = raw
        self.status = raw.status_code
        self.headers = raw.headers
        self.text = raw.text
        self.elapsed = elapsed
        self._json = None
        self._parsed = False

    @property
    def content_type(self):
        return (self.headers.get("Content-Type") or "").split(";")[0].strip().lower()

    def json(self):
        if not self._parsed:
            self._parsed = True
            try:
                self._json = json.loads(self.text) if self.text else None
            except ValueError:
                self._json = None
        return self._json

    def summary(self, limit=600):
        return f"HTTP {self.status} {self.content_type} {self.text[:limit]}"


def authorization_for(context, who):
    """Return (Authorization header value or None, identity attributes used in none mode)."""
    cfg = context.cfg
    if who in (None, "", "anonymous"):
        return None, None
    if who == "an invalid token":
        return "Bearer not-a-valid-token", None
    if who.startswith("client "):
        client = who[len("client "):]
        token = context.tokens.client_token(client)
        return (f"Bearer {token}" if token else None), None
    if who not in USERS:
        raise KeyError(f"unknown actor {who!r}")
    token = context.tokens.user_token(who)
    return (f"Bearer {token}" if token else None), USERS[who]["attributes"]


def send(context, who, method, service, path, json_body=None, raw_body=None,
         content_type=None, headers=None, consume_pending=True):
    cfg = context.cfg
    if service not in cfg.services:
        raise KeyError(f"unknown service {service!r}; known: {sorted(cfg.services)}")
    url = cfg.services[service] + path
    hdrs = {}
    auth, attributes = authorization_for(context, who)
    if auth:
        hdrs["Authorization"] = auth
    if json_body is not None:
        raw_body = json.dumps(json_body)
        hdrs["Content-Type"] = content_type or "application/json"
    elif raw_body is not None:
        hdrs["Content-Type"] = content_type or "application/json"
    if headers:
        hdrs.update(headers)
    removed = set()
    if consume_pending:
        for name, value in context.pending_headers.items():
            if value is None:
                removed.add(name.lower())
            else:
                hdrs[name] = value
        context.pending_headers = {}
    # Without authentication the token attributes cannot reach the backend. In none mode
    # the suite sends them as User-Attributes instead (documented in environment.py).
    if (cfg.auth_mode == "none" and cfg.none_mode_attribute_header and attributes is not None
            and "user-attributes" not in {k.lower() for k in hdrs} and "user-attributes" not in removed):
        hdrs["User-Attributes"] = ",".join(attributes)
    hdrs = {k: v for k, v in hdrs.items() if k.lower() not in removed}
    data = raw_body.encode("utf-8") if isinstance(raw_body, str) else raw_body
    start = time.monotonic()
    try:
        raw = requests.request(method, url, data=data, headers=hdrs, timeout=cfg.http_timeout,
                               allow_redirects=False)
    except requests.RequestException as exc:
        raise AssertionError(f"{method} {url} failed: {exc}") from exc
    resp = Response(raw, time.monotonic() - start)
    if cfg.verbose:
        print(f"{method} {url} -> {resp.summary(300)}")
    context.response = resp
    return resp
