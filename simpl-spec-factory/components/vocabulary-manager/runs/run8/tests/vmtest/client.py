"""HTTP client, tokens and helpers shared by the step library and the load test."""
import base64
import json
import random
import string
import threading
import time

import requests

from . import config

_token_cache = {}
_token_lock = threading.Lock()
_content_base = None
_content_base_lock = threading.Lock()


class TokenError(RuntimeError):
    pass


def _fetch_token(token_url, client_id, username):
    resp = requests.post(
        token_url,
        data={
            "grant_type": "password",
            "client_id": client_id,
            "username": username,
            "password": config.USER_PASSWORD,
            "scope": "openid",
        },
        timeout=config.HTTP_TIMEOUT,
    )
    if resp.status_code != 200:
        raise TokenError(
            "token request for %s at %s (client %s) failed: %s %s"
            % (username, token_url, client_id, resp.status_code, resp.text[:300])
        )
    body = resp.json()
    return body["access_token"], time.time() + int(body.get("expires_in", 60))


def token(user, kind="valid"):
    """Return a bearer token for a test user.

    kind: valid (client TOKEN_CLIENT), noaud (E7, no audience mapper),
    otherissuer (E7, realm simpl-other).
    """
    username = config.USERS.get(user, user)
    if kind == "valid":
        key = (config.TOKEN_URL, config.TOKEN_CLIENT, username)
    elif kind == "noaud":
        key = (config.TOKEN_URL, config.TOKEN_CLIENT_NOAUD, username)
    elif kind == "otherissuer":
        key = (config.TOKEN_URL_OTHER, config.TOKEN_CLIENT_OTHER, username)
    else:
        raise ValueError("unknown token kind %s" % kind)
    with _token_lock:
        cached = _token_cache.get(key)
        if cached and cached[1] - 20 > time.time():
            return cached[0]
        tok, exp = _fetch_token(*key)
        _token_cache[key] = (tok, exp)
        return tok


def tampered(tok):
    """Same header and claims, broken signature."""
    head, payload, sig = tok.split(".")
    raw = bytearray(base64.urlsafe_b64decode(sig + "=" * (-len(sig) % 4)))
    raw[0] ^= 0xFF
    raw[-1] ^= 0xFF
    new_sig = base64.urlsafe_b64encode(bytes(raw)).decode().rstrip("=")
    return "%s.%s.%s" % (head, payload, new_sig)


def unsigned(tok):
    """alg none token with the same claims."""
    _, payload, _ = tok.split(".")
    head = base64.urlsafe_b64encode(json.dumps({"alg": "none", "typ": "JWT"}).encode()).decode().rstrip("=")
    return "%s.%s." % (head, payload)


def auth_header(principal):
    """principal: None (no token), a user key, or a (user, kind) tuple, or ('raw', token)."""
    if principal is None:
        return {}
    if isinstance(principal, tuple):
        if principal[0] == "raw":
            return {"Authorization": "Bearer %s" % principal[1]}
        user, kind = principal
        if kind == "tampered":
            return {"Authorization": "Bearer %s" % tampered(token(user))}
        if kind == "unsigned":
            return {"Authorization": "Bearer %s" % unsigned(token(user))}
        return {"Authorization": "Bearer %s" % token(user, kind)}
    return {"Authorization": "Bearer %s" % token(principal)}


def request(method, path, principal="ga-admin", base=None, headers=None, **kwargs):
    url = path if path.startswith("http") else (base or config.API_URL) + path
    h = dict(auth_header(principal))
    if headers:
        h.update(headers)
    kwargs.setdefault("timeout", config.HTTP_TIMEOUT)
    kwargs.setdefault("allow_redirects", False)
    return requests.request(method, url, headers=h, **kwargs)


def random_token(n=8):
    alphabet = string.ascii_lowercase + string.digits
    return "".join(random.choice(alphabet) for _ in range(n))


def default_turtle(name, marker=None):
    """Valid content with no errors and no warnings: own namespace plus W3C namespaces."""
    marker = marker or random_token(6)
    return (
        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
        "@prefix ex: <https://example.org/vocab/%s#> .\n"
        "\n"
        "ex:Scheme a skos:ConceptScheme ;\n"
        "  skos:prefLabel \"%s scheme\"@en .\n"
        "ex:Concept%s a skos:Concept ;\n"
        "  skos:inScheme ex:Scheme ;\n"
        "  rdfs:comment \"Marker %s\" ;\n"
        "  skos:prefLabel \"Concept %s\"@en .\n" % (name, name, marker, marker, marker)
    ).encode("utf-8")


def turtle_of_size(name, size):
    """Valid Turtle of exactly `size` bytes (padding in a comment line)."""
    head = default_turtle(name, "big")
    if size < len(head) + 4:
        raise ValueError("size too small")
    pad = size - len(head) - 3  # "# " + pad + "\n"
    return head + b"# " + (b"x" * pad) + b"\n"


def create_vocabulary(name, content=None, description=None, changelog="Initial version.", principal="ga-admin", base=None):
    content = content if content is not None else default_turtle(name)
    description = description if description is not None else "Description of %s" % name
    return request(
        "POST", "/vocabularies", principal, base=base,
        data={"name": name, "description": description, "changelog": changelog},
        files={"vocabularyFile": ("%s.ttl" % name, content, "text/turtle")},
    )


def create_version(name, content=None, description=None, changelog=None, principal="ga-admin", base=None):
    marker = random_token(6)
    content = content if content is not None else default_turtle(name, marker)
    description = description if description is not None else "Description of %s %s" % (name, marker)
    changelog = changelog if changelog is not None else "Change %s" % marker
    return request(
        "POST", "/vocabularies/%s/versions" % name, principal, base=base,
        data={"description": description, "changelog": changelog},
        files={"vocabularyFile": ("%s.ttl" % name, content, "text/turtle")},
    )


def content_base():
    """contentBase of E2: from CONTENT_BASE_URL, else learnt once from a probe vocabulary."""
    global _content_base
    if config.CONTENT_BASE_URL:
        return config.CONTENT_BASE_URL.rstrip("/")
    with _content_base_lock:
        if _content_base:
            return _content_base
        name = "Probe" + random_token(10)
        resp = create_vocabulary(name)
        if resp.status_code != 201:
            raise RuntimeError("cannot learn contentBase: probe create gave %s %s" % (resp.status_code, resp.text[:300]))
        href = resp.json()["contentHref"]
        suffix = "/content/%s/versions/1" % name
        if not href.endswith(suffix):
            raise RuntimeError("cannot learn contentBase from contentHref %s" % href)
        _content_base = href[: -len(suffix)]
        return _content_base
