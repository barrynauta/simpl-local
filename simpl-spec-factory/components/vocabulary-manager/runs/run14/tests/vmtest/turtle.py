"""Turtle content for the scenarios. Every file uses its own namespace
(https://example.org/<run>/<Name>#), which D12 treats as the vocabulary's own
namespace, plus SKOS (a W3C namespace), so a default file gives no warning."""
import itertools
import os

_counter = itertools.count(1)
FIXTURES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "fixtures")

SKOS = "http://www.w3.org/2004/02/skos/core#"


def own_namespace(run, name):
    return f"https://example.org/{run}/{name}#"


def default_file(run, name, extra=""):
    """A small valid file; each call gives different bytes (unique revision)."""
    n = next(_counter)
    text = (
        f"@prefix skos: <{SKOS}> .\n"
        f"@prefix ex: <{own_namespace(run, name)}> .\n"
        f"\n"
        f"ex:Scheme a skos:ConceptScheme ;\n"
        f"    skos:prefLabel \"{name}\"@en ;\n"
        f"    skos:definition \"Revision {n} of {name}\" .\n"
        f"{extra}"
    )
    return text.encode("utf-8")


def sized_file(run, name, size):
    """A valid Turtle file of exactly `size` bytes (padded with a comment)."""
    head = (
        f"@prefix skos: <{SKOS}> .\n"
        f"@prefix ex: <{own_namespace(run, name)}> .\n"
        f"ex:Scheme a skos:ConceptScheme ; skos:prefLabel \"{name}\"@en .\n"
    ).encode("utf-8")
    lines = [head]
    total = len(head)
    i = 0
    while True:
        line = f"ex:c{i} a skos:Concept ; skos:inScheme ex:Scheme ; skos:prefLabel \"Concept {i}\"@en .\n".encode()
        if total + len(line) + 64 > size:
            break
        lines.append(line)
        total += len(line)
        i += 1
    remaining = size - total
    if remaining < 3:
        raise ValueError("size too small")
    lines.append(b"#" + b"x" * (remaining - 2) + b"\n")
    data = b"".join(lines)
    assert len(data) == size, (len(data), size)
    return data


def fixture(fname):
    with open(os.path.join(FIXTURES, fname), "rb") as handle:
        return handle.read()
