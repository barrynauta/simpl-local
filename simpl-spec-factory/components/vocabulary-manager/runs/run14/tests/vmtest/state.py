"""Per-scenario state and the placeholder language of the feature files.

Placeholders (expanded in every step argument, doc string and table cell):
  $Alpha          a vocabulary name unique to the scenario: 'Alpha' + run id
  $Alpha!upper    the same name in upper case (also !lower)
  $RUN            the run id of the scenario (lower case letters and digits)
  $CB             contentBase (E2), from CONTENT_BASE or read from a contentHref
  $EXT            https://ext.example.org/<run>, a namespace base only this scenario uses
  $N64 / $N65     valid-pattern names of exactly 64 / 65 characters, unique
  $N3             an unused name of exactly 3 characters
  ${key}          a value saved earlier in the scenario
"""
import random
import re
import string
import threading

from . import client, config, turtle

_PLACEHOLDER = re.compile(r"\$\{(\w+)\}|\$([A-Za-z][A-Za-z0-9]*)(!upper|!lower)?")
_SPECIAL = {"RUN", "CB", "EXT", "N64", "N65", "N3"}
_cb_lock = threading.Lock()
_content_base = {"value": config.CONTENT_BASE or None}


class ScenarioState:
    def __init__(self, run, base_url):
        self.run = run
        self.base_url = base_url
        self.token = None
        self.response = None
        self.saved = {}
        self.file = None
        self.headers = {}
        self.uploads = {}       # name -> {version: bytes}
        self.ext_uploads = {}   # external name -> bytes
        self.parallel = []
        self.cache = {}

    # ---------------------------------------------------------------- names
    def expand(self, text):
        if text is None:
            return None

        def repl(match):
            if match.group(1):
                return str(self.saved[match.group(1)])
            token, modifier = match.group(2), match.group(3)
            if token in _SPECIAL:
                value = self.special(token)
            else:
                value = token + self.run
            if modifier == "!upper":
                value = value.upper()
            elif modifier == "!lower":
                value = value.lower()
            return value

        return _PLACEHOLDER.sub(repl, text)

    def special(self, token):
        if token == "RUN":
            return self.run
        if token == "EXT":
            return f"https://ext.example.org/{self.run}"
        if token == "CB":
            return content_base(self)
        if token in ("N64", "N65"):
            length = int(token[1:])
            if token not in self.cache:
                self.cache[token] = ("L" + self.run + "x" * length)[:length]
            return self.cache[token]
        if token == "N3":
            if "N3" not in self.cache:
                self.cache["N3"] = fresh_three_letter_name()
            return self.cache["N3"]
        raise KeyError(token)

    # ---------------------------------------------------------------- HTTP
    def url(self, path):
        return self.base_url + path

    def admin_token(self):
        return client.token_for("ga-admin")

    def request(self, method, path, token="current", **kwargs):
        if token == "current":
            token = self.token
        headers = dict(self.headers)
        headers.update(kwargs.pop("headers", {}) or {})
        response = client.send(method, self.url(path), token=token, headers=headers, **kwargs)
        learn_content_base(response)
        return response

    def admin(self, method, path, **kwargs):
        response = client.send(method, self.url(path), token=self.admin_token(), **kwargs)
        learn_content_base(response)
        return response

    # ---------------------------------------------------------------- setup
    def create_vocabulary(self, name, file=None, description=None, changelog="Initial version."):
        content = file if file is not None else turtle.default_file(self.run, name)
        parts = [
            ("name", name),
            ("description", description or f"Description of {name}"),
            ("changelog", changelog),
            ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle")),
        ]
        response = self.admin("POST", "/vocabularies", files=client.multipart(parts))
        if response.status_code == 201:
            self.uploads.setdefault(name, {})[1] = content
        return response

    def create_version(self, name, file=None, description=None, changelog=None):
        content = file if file is not None else turtle.default_file(self.run, name)
        n = len(self.uploads.get(name, {})) + 1
        parts = [
            ("description", description or f"Description {n} of {name} {random.random()}"),
            ("changelog", changelog or f"Change {n}"),
            ("vocabularyFile", ("vocabulary.ttl", content, "text/turtle")),
        ]
        response = self.admin("POST", f"/vocabularies/{name}/versions", files=client.multipart(parts))
        if response.status_code == 201:
            self.uploads.setdefault(name, {})[response.json()["version"]] = content
        return response

    def register_external(self, name, namespace, aliases=(), file=None, description=None, source=None):
        content = file if file is not None else (
            f"@prefix skos: <{turtle.SKOS}> .\n"
            f"<{namespace.rstrip('#/')}#Term> a skos:Concept ; skos:prefLabel \"Term\"@en .\n"
        ).encode()
        parts = [("name", name), ("namespaceUri", namespace)]
        parts += [("aliasUris", alias) for alias in aliases]
        if description is not None:
            parts.append(("description", description))
        if source is not None:
            parts.append(("sourceUri", source))
        parts.append(("vocabularyFile", ("external.ttl", content, "text/turtle")))
        response = self.admin("POST", "/external-vocabularies", files=client.multipart(parts))
        if response.status_code == 201:
            self.ext_uploads[name] = content
        return response


def learn_content_base(response):
    """contentHref = <contentBase>/content/<Name>[/versions/<n>] (D25, D26)."""
    if _content_base["value"]:
        return
    try:
        if "json" not in response.headers.get("Content-Type", ""):
            return
        body = response.json()
    except Exception:  # noqa: BLE001
        return
    candidates = [body] if isinstance(body, dict) else []
    if isinstance(body, dict) and isinstance(body.get("items"), list):
        candidates += body["items"]
    for item in candidates:
        href = item.get("contentHref") if isinstance(item, dict) else None
        if href and "/content/" in href:
            with _cb_lock:
                _content_base["value"] = href[: href.rindex("/content/" + item.get("name", ""))] \
                    if item.get("name") and ("/content/" + item["name"]) in href else href.split("/content/")[0]
            return


def content_base(state):
    if _content_base["value"]:
        return _content_base["value"]
    # The probe name must not contain the scenario's run id: scenarios count their own names.
    probe = "Probe" + "".join(random.choice(string.ascii_lowercase + string.digits) for _ in range(12))
    response = state.create_vocabulary(probe)
    if response.status_code != 201:
        raise AssertionError(f"cannot learn contentBase: create gave {response.status_code} {response.text[:300]}")
    learn_content_base(response)
    return _content_base["value"]


def fresh_three_letter_name():
    """A 3-character name no stored vocabulary uses (checked case-insensitively)."""
    token = client.token_for("ga-admin")
    for _ in range(50):
        name = random.choice(string.ascii_uppercase) + "".join(
            random.choice(string.ascii_letters + string.digits) for _ in range(2))
        response = client.send("GET", config.API_URL + "/vocabularies?name=" + name, token=token)
        if response.status_code == 200 and response.json().get("total") == 0:
            return name
    raise AssertionError("no free 3-character name found")
