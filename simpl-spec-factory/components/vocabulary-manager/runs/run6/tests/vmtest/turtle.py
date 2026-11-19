"""Turtle content built for a scenario. Every file carries the run id, so contents never collide."""

SKOS = "http://www.w3.org/2004/02/skos/core#"


def own_namespace(run, logical, variant):
    return f"https://example.org/{run}/{logical.lower()}/v{variant}#"


def default_content(run, logical, variant, extra_prefixes=None):
    """A valid vocabulary without validation errors or warnings (decisions D9, D12).

    The only namespaces are skos (W3C) and the vocabulary's own namespace, which is the prefix
    of a subject IRI in the file.
    """
    lines = [f"@prefix skos: <{SKOS}> ."]
    lines.append(f"@prefix own: <{own_namespace(run, logical, variant)}> .")
    related = []
    for prefix, uri in extra_prefixes or []:
        lines.append(f"@prefix {prefix}: <{uri}> .")
        related.append(f"{prefix}:Thing")
    lines.append("")
    lines.append("own:Scheme a skos:ConceptScheme ;")
    lines.append(f'    skos:prefLabel "{logical} scheme {run}"@en ;')
    if related:
        lines.append("    skos:related " + " , ".join(related) + " ;")
    lines.append(f'    skos:definition "Variant {variant} of {logical}"@en .')
    lines.append("")
    return "\n".join(lines).encode("utf-8")


def external_content(run, logical, namespace):
    lines = [
        "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .",
        f"@prefix ext: <{namespace.rstrip('#/')}/> .",
        "",
        f'ext:title rdfs:label "Title of {logical} {run}"@en .',
        "",
    ]
    return "\n".join(lines).encode("utf-8")


def sized_content(run, logical, size):
    """Valid Turtle of exactly `size` bytes: one triple and comment lines (decision D15 limit)."""
    head = default_content(run, logical, "big")
    if size < len(head) + 2:
        raise ValueError("size too small")
    remaining = size - len(head)
    chunks = []
    while remaining > 0:
        line_len = min(100, remaining)
        if line_len == 1:
            chunks.append(b"\n")
        else:
            chunks.append(b"#" + b"x" * (line_len - 2) + b"\n")
        remaining -= line_len
    body = head + b"".join(chunks)
    assert len(body) == size
    return body
