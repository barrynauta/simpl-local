"""Unique names, placeholder substitution and Turtle content for the scenarios.

Every scenario gets its own token (tok). Every vocabulary name a scenario uses is
<alias><tok>, so a list filtered on the token shows only that scenario's data (E4).
"""
import datetime as _dt
import os
import random
import re
import string

from . import config

FIXTURES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "fixtures")

_ALNUM_UP = string.ascii_uppercase + string.digits


def new_token():
    """8 characters, upper case letters and digits, starts with a letter. Unique per scenario."""
    return "Z" + random.choice(string.ascii_uppercase) + "".join(random.choice(_ALNUM_UP) for _ in range(6))


class Names:
    """Maps scenario aliases (Alpha, Ref, ...) to real, unique vocabulary names."""

    def __init__(self, tok):
        self.tok = tok
        self.map = {}

    def get(self, alias):
        if alias not in self.map:
            self.map[alias] = f"{alias}{self.tok}"
        return self.map[alias]

    def set(self, alias, name):
        self.map[alias] = name


_PLACEHOLDER = re.compile(r"\{(\$?)([A-Za-z][A-Za-z0-9_]*)(?::([A-Za-z0-9_+-]+))?\}")


def _iso(ts):
    return ts.strftime("%Y-%m-%dT%H:%M:%SZ")


def _parse(ts):
    return _dt.datetime.strptime(ts, "%Y-%m-%dT%H:%M:%SZ")


def apply_filter(value, flt):
    if not flt:
        return value
    if flt == "date":
        return value[:10]
    if flt == "nooffset":
        return value[:-1] if value.endswith("Z") else value
    if flt == "plus1s":
        return _iso(_parse(value) + _dt.timedelta(seconds=1))
    if flt == "plus1d":
        return (_parse(value) + _dt.timedelta(days=1)).strftime("%Y-%m-%d")
    if flt == "offset1h":
        # The same instant written with +01:00.
        local = _parse(value) + _dt.timedelta(hours=1)
        return local.strftime("%Y-%m-%dT%H:%M:%S") + "%2B01:00"
    if flt == "lower":
        return value.lower()
    if flt == "upper":
        return value.upper()
    raise ValueError(f"unknown placeholder filter {flt}")


def substitute(text, ctx):
    """Replace {Alias}, {tok}, {base}, {admin_email}, {$memory:filter} in a step text."""
    if text is None:
        return None

    def repl(m):
        is_mem, key, flt = m.group(1), m.group(2), m.group(3)
        if is_mem:
            if key not in ctx.memory:
                raise AssertionError(f"nothing remembered as {key}")
            return apply_filter(str(ctx.memory[key]), flt)
        if key == "tok":
            val = ctx.names.tok
        elif key == "base":
            val = config.CONTENT_BASE
        elif key == "admin_email":
            val = config.USERS["ga-admin"]["email"]
        elif key == "viewer_email":
            val = config.USERS["ga-viewer"]["email"]
        elif key == "name":
            val = ctx.current_name
        else:
            val = ctx.names.get(key)
        return apply_filter(val, flt)

    return _PLACEHOLDER.sub(repl, text)


def default_turtle(name, version=1, extra=""):
    """A small valid vocabulary: own namespace, W3C prefixes only, no warnings expected."""
    return (
        f"@prefix ex: <https://example.org/{name}/ns#> .\n"
        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
        "\n"
        "ex:Scheme a skos:ConceptScheme ;\n"
        f"  skos:prefLabel \"Scheme of {name}\"@en .\n"
        "ex:Concept1 a skos:Concept ;\n"
        "  skos:inScheme ex:Scheme ;\n"
        f"  skos:prefLabel \"Concept of version {version}\"@en ;\n"
        "  rdfs:comment \"Created by the Vocabulary Manager test suite.\"@en .\n"
        f"{extra}"
    ).encode("utf-8")


def sized_turtle(name, size):
    """Valid Turtle of exactly size bytes: a small vocabulary padded with comment lines."""
    head = default_turtle(name)
    if size < len(head) + 2:
        raise ValueError("size too small")
    remaining = size - len(head)
    lines = []
    line = "# " + "p" * 1021 + "\n"  # 1024 bytes
    while remaining >= len(line) + 3:
        lines.append(line)
        remaining -= len(line)
    lines.append("#" + "q" * (remaining - 2) + "\n")
    body = head + "".join(lines).encode("ascii")
    assert len(body) == size, (len(body), size)
    return body


def many_s4_errors(name, count):
    """count subjects, each with two English preferred labels (one S4 error each)."""
    out = [
        f"@prefix ex: <https://example.org/{name}/ns#> .",
        "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .",
    ]
    for i in range(count):
        out.append(f'ex:c{i} skos:prefLabel "a{i}"@en , "b{i}"@en .')
    return ("\n".join(out) + "\n").encode("utf-8")


def fixture_bytes(filename):
    with open(os.path.join(FIXTURES, filename), "rb") as fh:
        return fh.read()


def fixture_text(filename, ctx):
    """Fixture file with placeholders replaced; line endings are kept as they are in the file."""
    raw = fixture_bytes(filename).decode("utf-8")
    return substitute(raw, ctx).encode("utf-8")
