"""Bearer tokens from Keycloak with the password grant (decisions D40, E1, E7)."""
import base64
import json
import threading
import time

import requests

from . import config

_cache = {}
_lock = threading.Lock()


def _exp(token):
    try:
        payload = token.split(".")[1]
        payload += "=" * (-len(payload) % 4)
        return json.loads(base64.urlsafe_b64decode(payload)).get("exp", 0)
    except Exception:  # noqa: BLE001
        return 0


def fetch_token(user, token_url=None, client_id=None):
    """Return an access token for one of the E1 users. Cached until shortly before expiry."""
    token_url = token_url or config.TOKEN_URL
    client_id = client_id or config.TOKEN_CLIENT
    username = config.USERS[user]["username"] if user in config.USERS else user
    key = (token_url, client_id, username)
    with _lock:
        cached = _cache.get(key)
        if cached and cached[1] - time.time() > 30:
            return cached[0]
    resp = requests.post(
        token_url,
        data={
            "grant_type": "password",
            "client_id": client_id,
            "username": username,
            "password": config.USER_PASSWORD,
            "scope": "openid",
        },
        timeout=30,
    )
    if resp.status_code != 200:
        raise AssertionError(
            f"Could not get a token for {username} from {token_url} (client {client_id}): "
            f"{resp.status_code} {resp.text[:300]}"
        )
    token = resp.json()["access_token"]
    with _lock:
        _cache[key] = (token, _exp(token) or time.time() + 60)
    return token


def tampered(token):
    """The same token with a changed signature."""
    head, payload, sig = token.split(".")
    first = "A" if sig[0] != "A" else "B"
    return ".".join([head, payload, first + sig[1:]])


def unsigned(token):
    """The same claims with alg none and no signature."""
    _, payload, _ = token.split(".")
    head = base64.urlsafe_b64encode(json.dumps({"alg": "none", "typ": "JWT"}).encode()).decode().rstrip("=")
    return f"{head}.{payload}."
