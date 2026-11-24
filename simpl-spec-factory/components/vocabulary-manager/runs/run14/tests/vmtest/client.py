"""HTTP helpers: tokens from Keycloak (password grant, E1 and D40) and requests
to the API. Black box: only HTTP."""
import base64
import json
import threading
import time

import requests

from . import config

_token_cache = {}
_lock = threading.Lock()


class TokenError(RuntimeError):
    pass


def fetch_token(user="ga-admin", client=None, token_url=None, use_cache=True):
    """Return (access_token, expires_in) for a test user of E1."""
    client = client or config.TOKEN_CLIENT
    token_url = token_url or config.TOKEN_URL
    username = config.USERS.get(user, user)
    key = (token_url, client, username)
    now = time.time()
    with _lock:
        cached = _token_cache.get(key)
        if use_cache and cached and cached[1] - now > 20:
            return cached[0], cached[1] - now
    response = requests.post(
        token_url,
        data={
            "grant_type": "password",
            "client_id": client,
            "username": username,
            "password": config.PASSWORD,
            "scope": "openid",
        },
        timeout=config.HTTP_TIMEOUT,
    )
    if response.status_code != 200:
        raise TokenError(
            f"token request for {username} at {token_url} (client {client}) "
            f"failed: {response.status_code} {response.text[:300]}"
        )
    body = response.json()
    expires_in = float(body.get("expires_in", 60))
    with _lock:
        _token_cache[key] = (body["access_token"], now + expires_in)
    return body["access_token"], expires_in


def token_for(user):
    return fetch_token(user)[0]


def break_signature(token):
    """Change the signature of a JWT so that it no longer verifies."""
    header, payload, signature = token.split(".")
    raw = bytearray(base64.urlsafe_b64decode(signature + "=" * (-len(signature) % 4)))
    raw[0] ^= 0xFF
    raw[-1] ^= 0xFF
    new_sig = base64.urlsafe_b64encode(bytes(raw)).decode().rstrip("=")
    return f"{header}.{payload}.{new_sig}"


def send(method, url, token=None, headers=None, json_body=None, data=None,
         files=None, content_type=None, timeout=None):
    """Send one request. headers may hold None values to drop a default header."""
    hdrs = {}
    if token:
        hdrs["Authorization"] = token if token.startswith(("Bearer ", "Basic ")) else f"Bearer {token}"
    if headers:
        hdrs.update(headers)
    body = data
    if json_body is not None:
        body = json.dumps(json_body).encode("utf-8")
        hdrs.setdefault("Content-Type", "application/json")
    if content_type is not None:
        hdrs["Content-Type"] = content_type
    return requests.request(
        method, url, headers=hdrs, data=body, files=files,
        timeout=timeout or config.HTTP_TIMEOUT, allow_redirects=False,
    )


def multipart(parts):
    """Build the files= list for requests from (name, value) pairs.
    A bytes value or a (filename, bytes, ctype) tuple is sent as a file part;
    a str value as a plain form field. Order and repetition are kept."""
    files = []
    for name, value in parts:
        if isinstance(value, tuple):
            files.append((name, value))
        elif isinstance(value, (bytes, bytearray)):
            files.append((name, ("vocabulary.ttl", bytes(value), "text/turtle")))
        else:
            files.append((name, (None, str(value))))
    return files
