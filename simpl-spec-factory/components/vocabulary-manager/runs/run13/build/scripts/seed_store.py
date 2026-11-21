#!/usr/bin/env python3
"""Writes the D99 seed (vocabularies x versions, files of about 10 KB) as N-Quads in the physical model of
docs/physical-model.md, for a fast bulk load through scripts/seed-store.sh. The data conforms to
docs/physical-model-shapes.ttl. Used only for load tests; the API path is tested by smoke.sh and the suites."""
import argparse
import hashlib
import sys
from datetime import datetime, timedelta, timezone

VM = "https://w3id.org/simpl/vocabulary-manager#"
XSD = "http://www.w3.org/2001/XMLSchema#"
G_INT = "<urn:simpl:vocabulary-manager:graph:internal>"
G_CONTENT = "<urn:simpl:vocabulary-manager:graph:content>"
TYPE = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>"


def lit(value):
    text = value.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")
    return f'"{text}"'


def typed(value, datatype):
    return f'"{value}"^^<{XSD}{datatype}>'


def turtle(name, version, size=10_000):
    lines = ["@prefix skos: <http://www.w3.org/2004/02/skos/core#> .", f"@prefix ex: <https://example.eu/{name}#> ."]
    i = 0
    while sum(len(line) + 1 for line in lines) < size:
        lines.append(f'ex:c{i} a skos:Concept ; skos:prefLabel "Concept {i} of {name} v{version}"@en .')
        i += 1
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--vocabularies", type=int, default=1000)
    parser.add_argument("--versions", type=int, default=20)
    parser.add_argument("--prefix", required=True)
    args = parser.parse_args()
    out = sys.stdout
    start = datetime(2026, 1, 1, tzinfo=timezone.utc)
    for index in range(args.vocabularies):
        name = f"{args.prefix}{index:05d}"
        voc = f"<urn:simpl:vocabulary-manager:vocabulary:{name}>"
        created = start + timedelta(minutes=index)
        updated = created + timedelta(minutes=args.versions)
        quads = [(voc, TYPE, f"<{VM}InternalVocabulary>"), (voc, f"<{VM}name>", lit(name)),
                 (voc, f"<{VM}nameKey>", lit(name.lower())), (voc, f"<{VM}status>", lit("ACTIVE")),
                 (voc, f"<{VM}latestVersion>", typed(args.versions, "integer")),
                 (voc, f"<{VM}createdBy>", lit("ga-admin@example.com")),
                 (voc, f"<{VM}creationDate>", typed(created.strftime("%Y-%m-%dT%H:%M:%SZ"), "dateTime")),
                 (voc, f"<{VM}lastUpdateDate>", typed(updated.strftime("%Y-%m-%dT%H:%M:%SZ"), "dateTime"))]
        for q in quads:
            out.write(f"{q[0]} {q[1]} {q[2]} {G_INT} .\n")
        for version in range(1, args.versions + 1):
            ver = f"<urn:simpl:vocabulary-manager:vocabulary:{name}:version:{version}>"
            content = turtle(name, version)
            body = content.encode()
            at = (created + timedelta(minutes=version - 1)).strftime("%Y-%m-%dT%H:%M:%SZ")
            status = "ACTIVE" if version == args.versions else "DEPRECATED"
            for p, o in ((TYPE, f"<{VM}InternalVocabularyVersion>"), (f"<{VM}vocabulary>", voc),
                         (f"<{VM}name>", lit(name)), (f"<{VM}version>", typed(version, "integer")),
                         (f"<{VM}description>", lit(f"Load {index} v{version}")),
                         (f"<{VM}changelog>", lit("Initial version." if version == 1 else f"v{version}")),
                         (f"<{VM}status>", lit(status)), (f"<{VM}createdBy>", lit("ga-admin@example.com")),
                         (f"<{VM}creationDate>", typed(at, "dateTime")),
                         (f"<{VM}lastUpdateDate>", typed(updated.strftime("%Y-%m-%dT%H:%M:%SZ"), "dateTime")),
                         (f"<{VM}contentSha256>", lit(hashlib.sha256(body).hexdigest())),
                         (f"<{VM}contentLength>", typed(len(body), "integer"))):
                out.write(f"{ver} {p} {o} {G_INT} .\n")
            out.write(f"{ver} <{VM}content> {lit(content)} {G_CONTENT} .\n")


if __name__ == "__main__":
    main()
