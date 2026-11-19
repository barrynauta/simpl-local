"""Scenario state and the HTTP calls the steps share.

One World per scenario. It holds the run id, the vocabularies the scenario created, the
current user and the last response. Setup calls always run as ga-admin, whatever the current
user is, so a scenario can prepare data and then act as another user.
"""
import datetime as dt
import json
import re
import secrets
import time
from pathlib import Path
from urllib.parse import parse_qsl

import requests

from . import auth, config, turtle

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures"
TOKEN_RE = re.compile(r"\{\{(\w+)(?::([^}]*))?\}\}")


class World:
    def __init__(self, api_url=None):
        self.api_url = (api_url or config.API_URL).rstrip("/")
        self.run = "R" + secrets.token_hex(5)
        self.user = None          # E1 user name, or None for no token
        self.raw_token = None     # a forged token, overrides user
        self.resp = None
        self.responses = []
        self.vars = {}
        self.vocabs = {}          # logical name -> {"versions": {n: {"content": bytes, "description": str}}}
        self.externals = {}       # logical name -> {"namespace": str, "content": bytes}
        self.content_base = None
        self.variant = 0
        self.operation = None

    # ---- names and templates -------------------------------------------------
    def real(self, logical):
        """Stored name of a logical vocabulary name: the logical name plus the run id."""
        logical = logical.strip()
        if logical.startswith("raw:"):
            return self.expand(logical[4:])
        return logical + self.run

    def base(self):
        return self.content_base or config.CONTENT_BASE

    def ext_namespace(self, logical):
        return f"https://ext.example.org/{self.run}/{logical.lower()}"

    def expand(self, text):
        if text is None:
            return None

        def sub(match):
            key, arg = match.group(1), match.group(2)
            if key == "run":
                return self.run
            if key == "runlower":
                return self.run.lower()
            if key == "runupper":
                return self.run.upper()
            if key == "name":
                return self.real(arg)
            if key == "base":
                return self.base()
            if key == "ext":
                return self.ext_namespace(arg)
            if key == "var":
                return str(self.vars[arg])
            if key == "plus1s":
                value = dt.datetime.strptime(str(self.vars[arg]), "%Y-%m-%dT%H:%M:%SZ")
                return (value + dt.timedelta(seconds=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
            if key == "datepart":
                return str(self.vars[arg])[:10]
            if key == "today":
                return dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d")
            if key == "email":
                return config.USERS[arg]["email"]
            if key == "chars":
                return "d" * int(arg)
            raise AssertionError(f"unknown template token {match.group(0)}")

        return TOKEN_RE.sub(sub, text)

    def fixture(self, name):
        raw = (FIXTURES / "turtle" / name).read_text(encoding="utf-8")
        return self.expand(raw).encode("utf-8")

    def next_variant(self):
        self.variant += 1
        return self.variant

    # ---- HTTP -----------------------------------------------------------------
    def headers_for(self, user=None, use_current=True):
        headers = {}
        token = None
        if use_current:
            if self.raw_token is not None:
                token = self.raw_token
            elif self.user:
                token = auth.token_for(self.user)
        elif user:
            token = auth.token_for(user)
        if token:
            headers["Authorization"] = f"Bearer {token}"
        return headers

    def url(self, path):
        if path.startswith("http://") or path.startswith("https://"):
            return path
        return self.api_url + path

    def call(self, method, path, as_admin=False, record=True, params=None, headers=None, **kwargs):
        path = self.expand(path)
        if params is None and "?" in path:
            path, query = path.split("?", 1)
            params = parse_qsl(query, keep_blank_values=True)
        hdrs = self.headers_for("ga-admin", use_current=False) if as_admin else self.headers_for()
        hdrs.setdefault("Accept", "application/json, application/problem+json, text/turtle, */*")
        if headers:
            hdrs.update(headers)
        resp = requests.request(method, self.url(path), params=params, headers=hdrs,
                                timeout=config.HTTP_TIMEOUT, **kwargs)
        self._learn(resp)
        if record:
            self.resp = resp
            self.responses.append(resp)
        return resp

    def _learn(self, resp):
        """Learn contentBase from the first contentHref (decisions D25, D26, E2)."""
        if self.content_base:
            return
        try:
            body = resp.json()
        except ValueError:
            return
        if isinstance(body, dict):
            href = body.get("contentHref")
            if isinstance(href, str) and "/content/" in href:
                self.content_base = href.split("/content/")[0]

    # ---- multipart --------------------------------------------------------------
    @staticmethod
    def multipart(parts):
        """parts: list of (field, value); a bytes value is sent as the Turtle file part."""
        files = []
        for field, value in parts:
            if isinstance(value, bytes):
                files.append((field, ("vocabulary.ttl", value, "text/turtle")))
            else:
                files.append((field, (None, value)))
        return files

    def create_parts(self, logical, content=None, description=None, changelog="Initial version."):
        content = content if content is not None else turtle.default_content(self.run, logical, self.next_variant())
        return [
            ("name", self.real(logical)),
            ("description", description if description is not None else f"Description of {logical} {self.run}"),
            ("changelog", changelog),
            ("vocabularyFile", content),
        ]

    def version_parts(self, logical, content=None, description=None, changelog=None):
        k = self.next_variant()
        content = content if content is not None else turtle.default_content(self.run, logical, k)
        return [
            ("description", description if description is not None else f"Description of {logical} variant {k}"),
            ("changelog", changelog if changelog is not None else f"Change {k} of {logical}"),
            ("vocabularyFile", content),
        ]

    def external_parts(self, logical, namespace=None, content=None):
        namespace = namespace or self.ext_namespace(logical)
        content = content if content is not None else turtle.external_content(self.run, logical, namespace)
        return [
            ("name", self.real(logical)),
            ("namespaceUri", namespace),
            ("description", f"Cached copy of {logical} {self.run}"),
            ("sourceUri", f"https://source.example.org/{self.run}/{logical.lower()}.ttl"),
            ("vocabularyFile", content),
        ]

    @staticmethod
    def apply_table(parts, table, world):
        """Change default parts with a table of | field | value |.

        The first row for a field replaces its default, later rows for the same field add parts.
        Values: (omitted), (empty), (spaces), file:<fixture>, turtle:<size>, chars:<n> or text.
        """
        result = list(parts)
        touched = set()
        for row in table:
            field = row["field"].strip()
            value = row["value"]
            if field not in touched:
                result = [p for p in result if p[0] != field]
                touched.add(field)
            if value == "(omitted)":
                continue
            if value == "(empty)":
                result.append((field, ""))
            elif value == "(spaces)":
                result.append((field, "   "))
            elif value.startswith("file:"):
                result.append((field, world.fixture(value[5:])))
            elif value.startswith("turtle:"):
                result.append((field, turtle.sized_content(world.run, "Big", int(value[7:]))))
            elif value.startswith("chars:"):
                result.append((field, "d" * int(value[6:])))
            else:
                result.append((field, world.expand(value)))
        return result

    def remember_created(self, logical, parts, resp):
        if resp.status_code != 201:
            return
        body = resp.json()
        content = next((v for f, v in parts if f == "vocabularyFile" and isinstance(v, bytes)), None)
        description = next((v for f, v in parts if f == "description"), None)
        entry = self.vocabs.setdefault(logical, {"versions": {}})
        entry["versions"][body.get("version")] = {"content": content, "description": description}

    # ---- setup (always as ga-admin) --------------------------------------------
    def setup_vocabulary(self, logical, content=None):
        parts = self.create_parts(logical, content=content)
        resp = self.call("POST", "/vocabularies", as_admin=True, record=False, files=self.multipart(parts))
        if resp.status_code != 201:
            raise AssertionError(f"setup: creating {logical} gave {resp.status_code}: {resp.text[:500]}")
        self.remember_created(logical, parts, resp)
        return resp

    def setup_version(self, logical, content=None):
        parts = self.version_parts(logical, content=content)
        resp = self.call("POST", f"/vocabularies/{self.real(logical)}/versions", as_admin=True,
                         record=False, files=self.multipart(parts))
        if resp.status_code != 201:
            raise AssertionError(f"setup: new version of {logical} gave {resp.status_code}: {resp.text[:500]}")
        self.remember_created(logical, parts, resp)
        return resp

    def setup_patch(self, path, status):
        resp = self.call("PATCH", path, as_admin=True, record=False, json={"status": status})
        if resp.status_code != 200:
            raise AssertionError(f"setup: PATCH {path} to {status} gave {resp.status_code}: {resp.text[:500]}")
        return resp

    def setup_external(self, logical, namespace=None, aliases=()):
        parts = self.external_parts(logical, namespace=namespace)
        for alias in aliases:
            parts.append(("aliasUris", alias))
        resp = self.call("POST", "/external-vocabularies", as_admin=True, record=False,
                         files=self.multipart(parts))
        if resp.status_code != 201:
            raise AssertionError(f"setup: registering {logical} gave {resp.status_code}: {resp.text[:500]}")
        self.externals[logical] = {
            "namespace": dict(parts)["namespaceUri"],
            "content": next(v for f, v in parts if f == "vocabularyFile"),
        }
        return resp

    # ---- reads used as proof (always as ga-admin) -------------------------------
    def admin_get(self, path):
        return self.call("GET", path, as_admin=True, record=False)

    def list_all(self, params):
        """All items of GET /vocabularies for the given filter, following pages of 100."""
        items, page = [], 1
        while True:
            q = list(params) + [("page", str(page)), ("pageSize", "100")]
            resp = self.call("GET", "/vocabularies", as_admin=True, record=False, params=q)
            if resp.status_code != 200:
                raise AssertionError(f"list for proof gave {resp.status_code}: {resp.text[:300]}")
            body = resp.json()
            items.extend(body.get("items", []))
            if page >= int(body.get("totalPages") or 0) or not body.get("items"):
                return items
            page += 1

    def all_externals(self):
        items, page = [], 1
        while True:
            resp = self.call("GET", "/external-vocabularies", as_admin=True, record=False,
                             params=[("page", str(page)), ("pageSize", "100")])
            if resp.status_code != 200:
                raise AssertionError(f"external list gave {resp.status_code}: {resp.text[:300]}")
            body = resp.json()
            items.extend(body.get("items", []))
            if page >= int(body.get("totalPages") or 0) or not body.get("items"):
                return items, body
            page += 1


def json_body(resp):
    try:
        return resp.json()
    except ValueError:
        raise AssertionError(f"response is not JSON: {resp.status_code} {resp.text[:300]}")


def field(body, path):
    """Read a dotted path such as 'items.0.name' or 'issues.1.in'. Raises KeyError when absent."""
    cur = body
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


def parse_value(text):
    try:
        return json.loads(text)
    except ValueError:
        return text


def wait_a_second():
    time.sleep(1.2)
