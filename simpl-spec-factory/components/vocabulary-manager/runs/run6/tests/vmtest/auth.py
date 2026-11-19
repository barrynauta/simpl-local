"""Bearer tokens from Keycloak with the password grant (build profile, decisions E1 and D40)."""
import base64
import threading
import time

import requests

from . import config

_cache = {}
_lock = threading.Lock()


def token_for(user):
    """Return an access token for a user of decision E1 ('ga-admin', 'ga-viewer', 'ga-norole')."""
    if user not in config.USERS:
        raise AssertionError(f"unknown test user '{user}'")
    with _lock:
        cached = _cache.get(user)
        if cached and cached[1] > time.time() + 20:
            return cached[0]
        data = {
            "grant_type": "password",
            "client_id": config.TOKEN_CLIENT,
            "username": config.USERS[user]["username"],
            "password": config.PASSWORD,
            "scope": "openid",
        }
        if config.TOKEN_CLIENT_SECRET:
            data["client_secret"] = config.TOKEN_CLIENT_SECRET
        resp = requests.post(config.TOKEN_URL, data=data, timeout=config.HTTP_TIMEOUT)
        if resp.status_code != 200:
            raise AssertionError(
                f"could not get a token for '{user}' from {config.TOKEN_URL}: {resp.status_code} {resp.text[:300]}"
            )
        body = resp.json()
        token = body["access_token"]
        _cache[user] = (token, time.time() + float(body.get("expires_in", 60)))
        return token


def tampered_token(user="ga-admin"):
    """A real token whose signature no longer matches its content (OWASP-08)."""
    token = token_for(user)
    header, payload, signature = token.split(".")
    raw = bytearray(base64.urlsafe_b64decode(signature + "=" * (-len(signature) % 4)))
    raw[len(raw) // 2] ^= 0xFF
    new_sig = base64.urlsafe_b64encode(bytes(raw)).decode().rstrip("=")
    return f"{header}.{payload}.{new_sig}"


def unsigned_token(user="ga-admin"):
    """The payload of a real token with alg 'none' and no signature (OWASP-08)."""
    token = token_for(user)
    _, payload, _ = token.split(".")
    header = base64.urlsafe_b64encode(b'{"alg":"none","typ":"JWT"}').decode().rstrip("=")
    return f"{header}.{payload}."
