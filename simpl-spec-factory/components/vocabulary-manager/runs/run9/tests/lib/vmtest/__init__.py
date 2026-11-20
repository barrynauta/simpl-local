"""Support library for the Vocabulary Manager black-box API suite.

Everything is configured through environment variables, so the suite runs
unchanged against any implementation of the component. See README.md.
"""
import json
import os
import random
import re
import string
import threading
import time
import uuid
from urllib.parse import urlsplit, parse_qs

import requests

# ---------------------------------------------------------------- configuration


def env(name, default=None):
    value = os.environ.get(name)
    return value if value not in (None, "") else default


API_URL = env("API_URL", "http://localhost:8196/v1").rstrip("/")
STORE_DOWN_API_URL = env("STORE_DOWN_API_URL", "http://localhost:8199/v1").rstrip("/")
TOKEN_URL = env("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
TOKEN_URL_OTHER = env("TOKEN_URL_OTHER", "http://localhost:8193/realms/simpl-other/protocol/openid-connect/token")
TOKEN_CLIENT = env("TOKEN_CLIENT", "vocabulary-tests")
TOKEN_CLIENT_NOAUD = env("TOKEN_CLIENT_NOAUD", "vocabulary-tests-noaud")
USERS = {
    "ga-admin": env("ADMIN_USER", "ga-admin"),
    "ga-viewer": env("VIEWER_USER", "ga-viewer"),
    "ga-norole": env("NOROLE_USER", "ga-norole"),
}
USER_PASSWORD = env("USER_PASSWORD", "Password1!")
ADMIN_EMAIL = env("ADMIN_EMAIL", "ga-admin@example.com")
CONTENT_BASE_URL = env("CONTENT_BASE_URL")  # optional; derived from contentHref otherwise
REQUEST_TIMEOUT = float(env("REQUEST_TIMEOUT", "90"))

FILE_LIMIT = 5 * 1024 * 1024        # D15, D76: 5 242 880 bytes on the vocabularyFile part
REQUEST_LIMIT = 6 * 1024 * 1024     # D76: whole multipart request

W3C_PREFIXES = (
    "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
    "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
    "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
    "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
    "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
)

FIXTURES_DIR = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "..", "fixtures"))

_shared = {"content_base": CONTENT_BASE_URL.rstrip("/") if CONTENT_BASE_URL else None}

# ---------------------------------------------------------------- tokens

_token_cache = {}
_token_lock = threading.Lock()


def fetch_token(username, client=None, token_url=None):
    client = client or TOKEN_CLIENT
    token_url = token_url or TOKEN_URL
    key = (token_url, client, username)
    with _token_lock:
        cached = _token_cache.get(key)
        if cached and cached[1] > time.time() + 30:
            return cached[0]
    resp = requests.post(
        token_url,
        data={
            "grant_type": "password",
            "client_id": client,
            "username": username,
            "password": USER_PASSWORD,
            "scope": "openid",
        },
        timeout=REQUEST_TIMEOUT,
    )
    if resp.status_code != 200:
        raise AssertionError(
            f"Could not get a token for {username} from {token_url} (client {client}): "
            f"{resp.status_code} {resp.text[:300]}"
        )
    body = resp.json()
    token = body["access_token"]
    expires = time.time() + int(body.get("expires_in", 60))
    with _token_lock:
        _token_cache[key] = (token, expires)
    return token


def token_for(user):
    """user: ga-admin, ga-viewer, ga-norole or anonymous. Returns None for anonymous."""
    if user == "anonymous":
        return None
    if user not in USERS:
        raise AssertionError(f"Unknown test user '{user}'")
    return fetch_token(USERS[user])


def special_token(kind):
    """Tokens that must be refused with 401 (D17, E5, E7, OWASP-08)."""
    if kind == "no-audience":
        return fetch_token(USERS["ga-admin"], client=TOKEN_CLIENT_NOAUD)
    if kind == "other-issuer":
        return fetch_token(USERS["ga-admin"], token_url=TOKEN_URL_OTHER)
    if kind == "tampered-signature":
        token = fetch_token(USERS["ga-admin"])
        head, payload, sig = token.split(".")
        i = len(sig) // 2
        sig = sig[:i] + ("A" if sig[i] != "A" else "B") + sig[i + 1:]
        return ".".join([head, payload, sig])
    if kind == "tampered-payload":
        token = fetch_token(USERS["ga-admin"])
        head, payload, sig = token.split(".")
        import base64
        pad = "=" * (-len(payload) % 4)
        claims = json.loads(base64.urlsafe_b64decode(payload + pad))
        claims["preferred_username"] = "someone-else"
        new_payload = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip("=")
        return ".".join([head, new_payload, sig])
    if kind == "malformed":
        return "not-a-jwt"
    raise AssertionError(f"Unknown token kind '{kind}'")


# ---------------------------------------------------------------- run ids and names


def new_run_id():
    first = random.choice(string.ascii_lowercase)
    rest = "".join(random.choice(string.ascii_lowercase + string.digits) for _ in range(9))
    return first + rest


def ensure_state(context):
    if not hasattr(context, "run"):
        context.run = new_run_id()
    for attr, default in (("names", {}), ("uploads", {}), ("descriptions", {}),
                          ("ext_uploads", {}), ("remembered", {}), ("ext_names", {})):
        if not hasattr(context, attr) or getattr(context, attr) is None:
            setattr(context, attr, default)


def vocab_name(context, alias):
    """Real vocabulary name for an alias: alias plus the scenario run id."""
    ensure_state(context)
    if alias not in context.names:
        name = f"{alias}{context.run}"
        if not re.match(r"^[A-Z][a-zA-Z0-9]*$", name):
            raise AssertionError(f"Alias '{alias}' does not give a valid vocabulary name")
        context.names[alias] = name
    return context.names[alias]


def padded_name(context, length, prefix="P"):
    base = prefix + context.run
    if length < len(base):
        raise AssertionError("length too small for a run-unique name")
    return base + "x" * (length - len(base))


def content_base(context):
    if _shared["content_base"]:
        return _shared["content_base"]
    # create a probe vocabulary and derive contentBase from its contentHref (E2, D25)
    alias = "Probe"
    resp = create_vocabulary(context, alias, user="ga-admin")
    if resp.status_code != 201:
        raise AssertionError(f"Could not create a probe vocabulary to learn contentBase: {resp.status_code} {resp.text[:300]}")
    return _shared["content_base"]


def learn_content_base(body, name, version=None):
    if _shared["content_base"] or not isinstance(body, dict):
        return
    href = body.get("contentHref")
    if not isinstance(href, str):
        return
    suffix = f"/content/{name}" + (f"/versions/{version}" if version else "")
    if href.endswith(suffix):
        _shared["content_base"] = href[: -len(suffix)]


# ---------------------------------------------------------------- templates

_PLACEHOLDER = re.compile(r"\$\{([^}]+)\}")


def expand(context, text):
    """Replace ${...} placeholders.

    ${run}             scenario run id
    ${ns}              own namespace of the run, https://example.org/vm/<run>/
    ${extns:X}         external namespace base https://ext.example.org/<run>/X
    ${contentBase}     contentBase of the build (E2)
    ${adminEmail}      creator of the admin user (D27, E1)
    ${repeat:c:N}      the character c repeated N times
    ${pad:N}           a run-unique name of N characters
    ${upper:X} ${lower:X}  the vocabulary name of alias X in upper or lower case
    ${ext:X}           the name of external vocabulary alias X
    ${X}               the vocabulary name of alias X
    """
    if text is None:
        return None
    ensure_state(context)

    def repl(m):
        key = m.group(1)
        if key == "run":
            return context.run
        if key == "ns":
            return f"https://example.org/vm/{context.run}/"
        if key.startswith("extns:"):
            return f"https://ext.example.org/{context.run}/{key[6:]}"
        if key == "contentBase":
            return content_base(context)
        if key == "adminEmail":
            return ADMIN_EMAIL
        if key.startswith("repeat:"):
            _, ch, n = key.split(":")
            return ch * int(n)
        if key.startswith("pad:"):
            return padded_name(context, int(key[4:]))
        if key.startswith("upper:"):
            return vocab_name(context, key[6:]).upper()
        if key.startswith("lower:"):
            return vocab_name(context, key[6:]).lower()
        if key.startswith("ext:"):
            return ext_name(context, key[4:])
        if key.startswith("day:"):
            _, rkey, path = key.split(":", 2)
            return str(get_path(context.remembered[rkey], path))[:10]
        if key.startswith("remembered:"):
            _, rkey, path = key.split(":", 2)
            return str(get_path(context.remembered[rkey], path))
        return vocab_name(context, key)

    return _PLACEHOLDER.sub(repl, text)


def ext_name(context, alias):
    ensure_state(context)
    if alias not in context.ext_names:
        context.ext_names[alias] = f"{alias}{context.run}"
    return context.ext_names[alias]


def fixture(context, filename):
    path = os.path.join(FIXTURES_DIR, filename)
    with open(path, "r", encoding="utf-8") as fh:
        return expand(context, fh.read()).encode("utf-8")


# ---------------------------------------------------------------- Turtle generators


def valid_turtle(context, prefixes=(), nonce=None):
    """A semantically valid vocabulary that uses only W3C namespaces and its own
    namespace, so it gives no validation warnings (D10, D12)."""
    ensure_state(context)
    n = nonce or uuid.uuid4().hex[:12]
    own = f"https://example.org/vm/{context.run}/{n}#"
    lines = [W3C_PREFIXES, f"@prefix ex: <{own}> .\n"]
    for p, iri in prefixes:
        lines.append(f"@prefix {p}: <{iri}> .\n")
    lines.append("\n")
    lines.append(f'ex:Scheme a skos:ConceptScheme ;\n  skos:prefLabel "Scheme {n}"@en ;\n'
                 f'  rdfs:comment "Generated by the black-box suite."^^xsd:string .\n')
    lines.append("ex:code a owl:DatatypeProperty .\n")
    lines.append("ex:broaderScheme a owl:ObjectProperty .\n")
    lines.append('ex:Concept1 a skos:Concept ;\n  skos:inScheme ex:Scheme ;\n'
                 '  skos:prefLabel "Concept one"@en , "Begriff eins"@de ;\n'
                 '  ex:code "C1" ;\n  ex:broaderScheme ex:Scheme .\n')
    for p, _ in prefixes:
        lines.append(f"ex:Concept1 rdfs:seeAlso {p}:Thing .\n")
    return "".join(lines).encode("utf-8")


def turtle_of_size(context, size):
    """Valid Turtle of exactly `size` bytes: generated concepts, then a padding comment."""
    head = valid_turtle(context)
    own = f"https://example.org/vm/{context.run}/big{uuid.uuid4().hex[:6]}#"
    parts = [head, f"@prefix big: <{own}> .\n".encode()]
    total = sum(len(p) for p in parts)
    i = 0
    chunk = []
    while total < size - 400:
        line = f'big:c{i} a skos:Concept ; skos:prefLabel "Generated concept {i}"@en .\n'.encode()
        if total + len(line) > size - 400:
            break
        chunk.append(line)
        total += len(line)
        i += 1
    parts.append(b"".join(chunk))
    data = b"".join(parts)
    remaining = size - len(data)
    if remaining < 0:
        raise AssertionError("cannot build a Turtle file that small")
    if remaining == 0:
        return data
    if remaining == 1:
        return data + b"\n"
    return data + b"#" + b"x" * (remaining - 2) + b"\n"


# ---------------------------------------------------------------- HTTP


def auth_headers(token):
    return {"Authorization": f"Bearer {token}"} if token else {}


def request(context, method, path, base=None, token="__current__", headers=None, **kwargs):
    base = base or getattr(context, "api_base", None) or API_URL
    if token == "__current__":
        token = current_token(context)
    h = auth_headers(token)
    extra = getattr(context, "extra_headers", None) or {}
    h.update(extra)
    if headers:
        h.update(headers)
    url = path if path.startswith("http") else base + path
    start = time.time()
    resp = requests.request(method, url, headers=h, timeout=REQUEST_TIMEOUT, allow_redirects=False, **kwargs)
    elapsed = time.time() - start
    context.response = resp
    context.response_time = elapsed
    context.extra_headers = {}
    return resp


def current_token(context):
    if getattr(context, "token_override", None) is not None:
        return context.token_override
    return token_for(getattr(context, "user", "ga-admin"))


def multipart(parts):
    """parts: list of (name, value, is_file). Values are bytes or str."""
    files = []
    for name, value, is_file in parts:
        if is_file:
            data = value if isinstance(value, bytes) else value.encode("utf-8")
            files.append((name, ("vocabulary.ttl", data, "text/turtle")))
        else:
            files.append((name, (None, value)))
    return files


def json_body(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError(f"Response is not JSON: {resp.status_code} {resp.headers.get('Content-Type')} {resp.text[:300]}")


_PATH_TOKEN = re.compile(r"([^.\[\]]+)|\[(\d+)\]")


def get_path(obj, path, default=KeyError):
    cur = obj
    for name, index in _PATH_TOKEN.findall(path):
        try:
            if name:
                cur = cur[name]
            else:
                cur = cur[int(index)]
        except (KeyError, IndexError, TypeError):
            if default is KeyError:
                raise AssertionError(f"Field '{path}' not found in {json.dumps(obj)[:500]}")
            return default
    return cur


# ---------------------------------------------------------------- domain helpers (run as a given user)


def create_vocabulary(context, alias, user="ga-admin", content=None, description=None, changelog="Initial version.",
                      name=None):
    ensure_state(context)
    name = name or vocab_name(context, alias)
    content = content if content is not None else valid_turtle(context)
    description = description if description is not None else f"Description of {name}"
    parts = [("name", name, False), ("description", description, False),
             ("changelog", changelog, False), ("vocabularyFile", content, True)]
    resp = request(context, "POST", "/vocabularies", token=token_for(user) if user else "__current__",
                   files=multipart(parts))
    if resp.status_code == 201:
        context.uploads[(name, 1)] = content
        context.descriptions[(name, 1)] = description
        try:
            learn_content_base(resp.json(), name, 1)
        except ValueError:
            pass
    return resp


def create_version(context, alias, user="ga-admin", content=None, description=None, changelog="Changed content.",
                   extra_parts=()):
    ensure_state(context)
    name = vocab_name(context, alias)
    content = content if content is not None else valid_turtle(context)
    description = description if description is not None else f"Version description {uuid.uuid4().hex[:8]}"
    parts = [("description", description, False), ("changelog", changelog, False),
             ("vocabularyFile", content, True)] + list(extra_parts)
    resp = request(context, "POST", f"/vocabularies/{name}/versions",
                   token=token_for(user) if user else "__current__", files=multipart(parts))
    if resp.status_code == 201:
        try:
            v = resp.json().get("version")
            context.uploads[(name, v)] = content
            context.descriptions[(name, v)] = description
        except ValueError:
            pass
    return resp


def register_external(context, alias, namespace=None, aliases=(), user="ga-admin", content=None,
                      description=None, source=None):
    ensure_state(context)
    name = ext_name(context, alias)
    namespace = namespace or f"https://ext.example.org/{context.run}/{alias}#"
    if content is None:
        content = (W3C_PREFIXES + f"@prefix e: <{namespace}> .\n"
                   f'e:Term a rdf:Property ;\n  rdfs:label "External term of {name}"@en .\n').encode()
    parts = [("name", name, False), ("namespaceUri", namespace, False), ("vocabularyFile", content, True)]
    for a in aliases:
        parts.append(("aliasUris", a, False))
    if description is not None:
        parts.append(("description", description, False))
    if source is not None:
        parts.append(("sourceUri", source, False))
    resp = request(context, "POST", "/external-vocabularies", token=token_for(user) if user else "__current__",
                   files=multipart(parts))
    if resp.status_code == 201:
        context.ext_uploads[name] = content
    return resp


def admin_get(context, path, **kwargs):
    """A read as ga-admin that does not replace context.response."""
    saved = getattr(context, "response", None)
    saved_extra = getattr(context, "extra_headers", None)
    context.extra_headers = {}
    resp = request(context, "GET", path, token=token_for("ga-admin"), **kwargs)
    context.response = saved
    context.extra_headers = saved_extra
    return resp


def list_by_name(context, name_filter):
    resp = admin_get(context, "/vocabularies", params={"name": name_filter, "pageSize": 100})
    assert resp.status_code == 200, f"list with name filter failed: {resp.status_code} {resp.text[:300]}"
    return resp.json()


def link_query(link):
    parts = urlsplit(link)
    return parts.path, {k: v[0] if len(v) == 1 else v for k, v in parse_qs(parts.query, keep_blank_values=True).items()}
