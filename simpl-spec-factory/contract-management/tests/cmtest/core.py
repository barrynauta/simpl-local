"""HTTP access, templating, polling and token handling for the suite."""
import hashlib
import json
import re
import time
import uuid
from datetime import datetime, timedelta, timezone

import requests

PROBLEM_PREFIX = "urn:problem-type:simpl:"
CM_PROBLEM_PREFIX = "urn:problem-type:simpl:contract-management:"
CM_SPECIFIC_TYPES = {"wrongParty", "invalidTransition", "alreadyExists",
                     "offerNotPending", "invalidDocument"}


def problem_type(short):
    """Expand a short problem type name to the full URN of D11."""
    if short.startswith("urn:"):
        return short
    if short in CM_SPECIFIC_TYPES:
        return CM_PROBLEM_PREFIX + short
    return PROBLEM_PREFIX + short


def sha256_hex(data):
    if isinstance(data, str):
        data = data.encode("utf-8")
    return hashlib.sha256(data).hexdigest()


def now_utc():
    return datetime.now(timezone.utc)


def rfc3339(dt):
    return dt.astimezone(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def parse_datetime(text):
    """Lenient parser for both date-time styles of D15."""
    if text is None:
        return None
    value = text.strip().replace(" ", "T")
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    m = re.match(r"^(.*T\d{2}:\d{2}:\d{2})(\.\d+)?(.*)$", value)
    if m:
        frac = (m.group(2) or "")[:7]
        value = m.group(1) + frac + m.group(3)
    dt = datetime.fromisoformat(value)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt


def new_uuid():
    return str(uuid.uuid4())


# ---------------------------------------------------------------- templating

_PLACEHOLDER = re.compile(r"\$\{([A-Za-z0-9_.:+-]+)\}")


def render(text, variables):
    """Replace ${name} placeholders with values from the scenario variables.

    ${uuid} gives a fresh UUID each time. Unknown names raise KeyError so a
    test-writer typo shows up as an error, not as a silent wrong value.
    """
    if text is None:
        return None

    def repl(match):
        name = match.group(1)
        if name == "uuid":
            return new_uuid()
        if name not in variables:
            raise KeyError("unknown placeholder ${%s}" % name)
        value = variables[name]
        return "" if value is None else str(value)

    return _PLACEHOLDER.sub(repl, text)


def render_json(text, variables):
    return json.loads(render(text, variables))


# ---------------------------------------------------------------- polling

class PollTimeout(AssertionError):
    pass


def poll(fetch, check, config, description, timeout=None):
    """Poll fetch() every POLL_INTERVAL until check(value) is true (D79).

    Returns the last value. Raises PollTimeout with the last value otherwise.
    """
    deadline = time.monotonic() + (timeout or config.poll_timeout)
    last = None
    last_error = None
    while True:
        try:
            last = fetch()
            if check(last):
                return last
            last_error = None
        except AssertionError as exc:
            last_error = exc
        if time.monotonic() >= deadline:
            raise PollTimeout("timed out waiting for %s; last value: %s%s" % (
                description, _short(last),
                (" (last error: %s)" % last_error) if last_error else ""))
        time.sleep(config.poll_interval)


def _short(value, limit=1500):
    try:
        if isinstance(value, requests.Response):
            text = "%s %s" % (value.status_code, value.text)
        else:
            text = json.dumps(value, default=str)
    except Exception:  # noqa: BLE001
        text = repr(value)
    return text if len(text) <= limit else text[:limit] + "..."


# ---------------------------------------------------------------- HTTP

class Http:
    def __init__(self, config):
        self.config = config
        self.session = requests.Session()
        self.log = []

    def request(self, method, url, headers=None, json_body=None, data=None,
                files=None, params=None):
        hdrs = dict(headers or {})
        kwargs = {"headers": hdrs, "timeout": self.config.http_timeout,
                  "params": params}
        if files is not None:
            kwargs["files"] = files
        elif json_body is not None:
            kwargs["data"] = json.dumps(json_body)
            hdrs.setdefault("Content-Type", "application/json")
        elif data is not None:
            kwargs["data"] = data
        resp = self.session.request(method, url, **kwargs)
        self.log.append((method, url, resp.status_code))
        return resp


def body_json(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError("response is not JSON: %s %r" % (
            resp.status_code, resp.text[:500]))


def get_path(obj, path):
    """Read a dotted path (a.b.0.c) from parsed JSON. Raises KeyError."""
    cur = obj
    if path in ("", "."):
        return cur
    for part in path.split("."):
        if isinstance(cur, list):
            cur = cur[int(part)]
        elif isinstance(cur, dict):
            if part not in cur:
                raise KeyError(path)
            cur = cur[part]
        else:
            raise KeyError(path)
    return cur


def has_path(obj, path):
    try:
        get_path(obj, path)
        return True
    except (KeyError, IndexError, ValueError):
        return False


def coerce(text):
    """Interpret an expected value written in a feature file."""
    if text == "null":
        return None
    if text == "true":
        return True
    if text == "false":
        return False
    if re.fullmatch(r"-?\d+", text):
        return int(text)
    return text


# ---------------------------------------------------------------- tokens

class Tokens:
    def __init__(self, config, http):
        self.config = config
        self.http = http
        self.cache = {}

    def token_for(self, username):
        if username in self.cache and self.cache[username][1] > time.time() + 10:
            return self.cache[username][0]
        if username not in self.config.users:
            raise AssertionError("unknown test user %s" % username)
        role, password = self.config.users[username]
        realm = self.config.party(role).realm
        url = "%s/realms/%s/protocol/openid-connect/token" % (
            self.config.keycloak_url, realm)
        resp = self.http.request("POST", url, data={
            "grant_type": "password", "client_id": self.config.keycloak_client_id,
            "username": username, "password": password, "scope": "openid"})
        assert resp.status_code == 200, "token request for %s failed: %s %s" % (
            username, resp.status_code, resp.text[:300])
        data = resp.json()
        self.cache[username] = (data["access_token"],
                                time.time() + int(data.get("expires_in", 60)))
        return data["access_token"]

    def auth_header(self, username):
        if self.config.auth_mode == "none" or username is None:
            return {}
        return {"Authorization": "Bearer " + self.token_for(username)}


def days_from_now(days, minutes=0):
    return rfc3339(now_utc() + timedelta(days=days, minutes=minutes))
