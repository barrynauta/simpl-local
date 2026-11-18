"""Scenario state and the API actions the steps use."""
import json
import re
import uuid
from concurrent.futures import ThreadPoolExecutor

import requests

from vmclient import get_token, invalid_token, new_run_id, read_fixture, to_second, utc_today

DEFAULT_FIXTURE = "valid-skos.ttl"


def multipart(fields, alias, data):
    """multipart/form-data parts: plain fields plus the vocabularyFile part."""
    parts = [(k, (None, v)) for k, v in fields]
    if data is not None:
        parts.append(("vocabularyFile", ("%s.ttl" % alias, data, "text/turtle")))
    return parts


class World:
    def __init__(self, cfg):
        self.cfg = cfg
        self.runid = new_run_id()
        self.prefix = "V" + self.runid
        self.names = {}          # alias -> vocabulary name
        self.ext_names = {}      # alias -> external vocabulary name
        self.ext_ns = {}         # alias -> namespace URI
        self.contents = {}       # (alias, version) -> bytes
        self.descriptions = {}   # (alias, version) -> str
        self.created = {}        # (alias, version) -> body of the 201 response
        self.ext_contents = {}   # alias -> bytes
        self.latest = {}         # alias -> highest version number known
        self.auth = ("user", "ga-admin")
        self.response = None
        self.responses = []
        self.saved = {}

    # ---------- names and text expansion ----------
    def name(self, alias):
        if alias not in self.names:
            self.names[alias] = self.prefix + alias
        return self.names[alias]

    def ext_name(self, alias):
        if alias not in self.ext_names:
            self.ext_names[alias] = "E" + self.runid + alias
        return self.ext_names[alias]

    def ext_namespace(self, alias):
        if alias not in self.ext_ns:
            self.ext_ns[alias] = "https://ext.example/%s/%s#" % (self.runid, alias.lower())
        return self.ext_ns[alias]

    def own_ns(self):
        return "https://example.org/%s/own#" % self.runid

    def content_url(self, alias, version=None):
        url = "%s/content/%s" % (self.cfg.content_base, self.name(alias))
        if version is not None:
            url += "/versions/%s" % version
        return url

    def expand(self, text):
        if text is None:
            return None

        def rep(m):
            key = m.group(1)
            if key == "run":
                return self.prefix
            if key == "runlower":
                return self.prefix.lower()
            if key == "runupper":
                return self.prefix.upper()
            if key == "runid":
                return self.runid
            if key == "base":
                return self.cfg.content_base
            if key == "own":
                return self.own_ns()
            if key == "today":
                return utc_today()
            if ":" in key:
                kind, arg = key.split(":", 1)
                if kind == "name":
                    return self.name(arg)
                if kind == "lname":
                    return self.name(arg).lower()
                if kind == "uname":
                    return self.name(arg).upper()
                if kind == "ext":
                    return self.ext_name(arg)
                if kind == "extns":
                    return self.ext_namespace(arg)
                if kind == "content":
                    if "@" in arg:
                        a, v = arg.split("@", 1)
                        return self.content_url(a, int(v))
                    return self.content_url(arg)
                if kind == "email":
                    return self.cfg.emails[arg]
                if kind == "saved":
                    return str(self.saved[arg])
                if kind == "second":
                    return to_second(self.saved[arg])
                if kind == "x":
                    return "x" * int(arg)
            raise KeyError("unknown placeholder ${%s}" % key)

        return re.sub(r"\$\{([^}]+)\}", rep, text)

    def special(self, value):
        """Special table values. Returns (present, value)."""
        if value == "<absent>":
            return False, None
        if value == "<empty>":
            return True, ""
        if value == "<spaces>":
            return True, "   "
        m = re.match(r"^<text:(\d+)>$", value)
        if m:
            return True, "d" * int(m.group(1))
        m = re.match(r"^<padded:(.*)>$", value)
        if m:
            return True, "  " + self.expand(m.group(1)) + "  "
        m = re.match(r"^<name:(\d+)>$", value)
        if m:
            n = int(m.group(1))
            if n > len(self.prefix):
                return True, self.prefix + "N" + "a" * (n - len(self.prefix) - 1)
            return True, ("Ab" + "c" * n)[:n]
        return True, self.expand(value)

    # ---------- content ----------
    def default_content(self, alias, version):
        body = self.expand(read_fixture(DEFAULT_FIXTURE))
        return (body + "\n# %s version %s marker %s\n" % (alias, version, uuid.uuid4().hex)).encode("utf-8")

    def padded_content(self, size):
        base = self.expand(read_fixture(DEFAULT_FIXTURE)).encode("utf-8") + b"\n# "
        if size < len(base) + 1:
            raise ValueError("size too small")
        return base + b"p" * (size - len(base) - 1) + b"\n"

    def file_value(self, value, alias):
        """Resolve a file description to bytes."""
        if value == "<default>":
            return self.default_content(alias, self.latest.get(alias, 0) + 1)
        m = re.match(r"^<same as version (\d+)>$", value)
        if m:
            return self.contents[(alias, int(m.group(1)))]
        m = re.match(r"^<bytes:(\d+)>$", value)
        if m:
            return self.padded_content(int(m.group(1)))
        return self.expand(read_fixture(value)).encode("utf-8")

    # ---------- HTTP ----------
    def headers_for(self, auth=None):
        auth = auth or self.auth
        h = {}
        if auth[0] == "user":
            h["Authorization"] = "Bearer " + get_token(self.cfg, auth[1])
        elif auth[0] == "token":
            h["Authorization"] = "Bearer " + auth[1]
        return h

    def set_user(self, user):
        self.auth = ("user", user)

    def set_anonymous(self):
        self.auth = ("none",)

    def set_invalid_token(self, kind):
        self.auth = ("token", invalid_token(self.cfg, kind))

    def call(self, method, path, auth=None, headers=None, record=True, **kwargs):
        h = self.headers_for(auth)
        if headers:
            h.update(headers)
        url = self.cfg.api_url + path
        r = requests.request(method, url, headers=h, timeout=self.cfg.timeout, **kwargs)
        if record:
            self.response = r
        return r

    def as_admin(self, method, path, **kwargs):
        return self.call(method, path, auth=("user", "ga-admin"), record=False, **kwargs)

    # ---------- vocabulary actions ----------
    def create_vocabulary(self, alias, fields=None, content=None, auth=None, record=True):
        fields = dict(fields or {})
        name_present, name = True, self.name(alias)
        if "name" in fields:
            name_present, name = self.special(fields.pop("name"))
            if name_present and name is not None:
                self.names[alias] = name
        desc_present, desc = True, "Description of %s." % alias
        if "description" in fields:
            desc_present, desc = self.special(fields.pop("description"))
        cl_present, cl = True, "Initial version."
        if "changelog" in fields:
            cl_present, cl = self.special(fields.pop("changelog"))
        file_present, data = True, content
        if "file" in fields:
            fv = fields.pop("file")
            if fv == "<absent>":
                file_present = False
            else:
                data = self.file_value(fv, alias)
        if data is None and file_present:
            data = self.default_content(alias, 1)
        form = {}
        if name_present:
            form["name"] = name
        if desc_present:
            form["description"] = desc
        if cl_present:
            form["changelog"] = cl
        for k, v in fields.items():
            p, val = self.special(v)
            if p:
                form[k] = val
        r = self.call("POST", "/vocabularies", auth=auth, record=record,
                      files=multipart(form.items(), alias, data if file_present else None))
        if r.status_code == 201:
            self.contents[(alias, 1)] = data
            self.descriptions[(alias, 1)] = desc
            self.created[(alias, 1)] = r.json()
            self.latest[alias] = 1
        return r

    def create_version(self, alias, fields=None, content=None, auth=None, record=True):
        fields = dict(fields or {})
        nxt = self.latest.get(alias, 0) + 1
        desc_present, desc = True, "Description of %s version %d." % (alias, nxt)
        if "description" in fields:
            desc_present, desc = self.special(fields.pop("description"))
        cl_present, cl = True, "Change number %d." % nxt
        if "changelog" in fields:
            cl_present, cl = self.special(fields.pop("changelog"))
        file_present, data = True, content
        if "file" in fields:
            fv = fields.pop("file")
            if fv == "<absent>":
                file_present = False
            else:
                data = self.file_value(fv, alias)
        if data is None and file_present:
            data = self.default_content(alias, "%d-%s" % (nxt, uuid.uuid4().hex[:6]))
        form = {}
        if desc_present:
            form["description"] = desc
        if cl_present:
            form["changelog"] = cl
        for k, v in fields.items():
            p, val = self.special(v)
            if p:
                form[k] = val
        r = self.call(
            "POST", "/vocabularies/%s/versions" % self.name(alias), auth=auth, record=record,
            files=multipart(form.items(), alias, data if file_present else None),
        )
        if r.status_code == 201:
            v = r.json().get("version")
            self.contents[(alias, v)] = data
            self.descriptions[(alias, v)] = desc
            self.created[(alias, v)] = r.json()
            self.latest[alias] = max(self.latest.get(alias, 0), v)
        return r

    def patch_status(self, alias, status, version=None, auth=None, record=True):
        path = "/vocabularies/%s" % self.name(alias)
        if version is not None:
            path += "/versions/%s" % version
        return self.call("PATCH", path, auth=auth, record=record, json={"status": status})

    def register_external(self, alias, fields=None, auth=None, record=True):
        fields = dict(fields or {})
        form = {"name": self.ext_name(alias), "namespaceUri": self.ext_namespace(alias),
                "description": "External vocabulary %s." % alias,
                "sourceUri": "https://source.example/%s/%s.ttl" % (self.runid, alias)}
        aliases = []
        data = None
        file_present = True
        for k, v in fields.items():
            if k == "file":
                if v == "<absent>":
                    file_present = False
                else:
                    data = self.file_value(v, alias)
                continue
            if k == "aliasUris":
                aliases = [self.expand(x.strip()) for x in v.split(",") if x.strip()]
                continue
            p, val = self.special(v)
            if not p:
                form.pop(k, None)
            else:
                form[k] = val
                if k == "name":
                    self.ext_names[alias] = val
                if k == "namespaceUri":
                    self.ext_ns[alias] = val
        if data is None and file_present:
            data = self.expand(read_fixture("external-terms.ttl").replace(
                "${extns}", form.get("namespaceUri", self.ext_namespace(alias)))).encode("utf-8")
        payload = list(form.items()) + [("aliasUris", a) for a in aliases]
        r = self.call("POST", "/external-vocabularies", auth=auth, record=record,
                      files=multipart(payload, alias, data if file_present else None))
        if r.status_code == 201:
            self.ext_contents[alias] = data
        return r

    def parallel(self, fns):
        with ThreadPoolExecutor(max_workers=len(fns)) as ex:
            futures = [ex.submit(f) for f in fns]
            self.responses = [f.result() for f in futures]
        return self.responses

    def body(self, r=None):
        r = r if r is not None else self.response
        try:
            return r.json()
        except ValueError:
            raise AssertionError("response is not JSON: %s %s" % (r.status_code, r.text[:300]))

    def describe(self, r=None):
        r = r if r is not None else self.response
        return "%s %s -> %s %s" % (r.request.method, r.request.url, r.status_code, r.text[:600])
