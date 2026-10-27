"""Experiment 4 migration: run migrate.sparql, then fill the raw-text graphs the build needs for downloads.
Upstream keeps only parsed triples, so the raw text is the content graph serialised as Turtle by Fuseki
(the original file, its comments and prefix layout are not recoverable).
Usage: python3 migrate.py http://localhost:3032/ds_vocabularies"""
import sys, os, re, urllib.request, urllib.parse
ds = sys.argv[1].rstrip("/")
def post(path, field, text, accept=None):
    req = urllib.request.Request(ds + path, data=urllib.parse.urlencode({field: text}).encode(),
                                 headers={"Accept": accept} if accept else {})
    return urllib.request.urlopen(req).read().decode()
here = os.path.dirname(os.path.abspath(__file__))
post("/update", "update", open(os.path.join(here, "migrate.sparql")).read())
rows = post("/query", "query", 'SELECT DISTINCT ?g WHERE { GRAPH ?g {} FILTER(STRSTARTS(STR(?g),"urn:simpl:vm:content:")) }',
            "text/csv").splitlines()[1:]
def esc(s): return s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n").replace("\r", "\\r")
n = 0
for g in rows:
    g = g.strip()
    ttl = urllib.request.urlopen(urllib.request.Request(ds + "/data?graph=" + urllib.parse.quote(g, safe=""),
                                 headers={"Accept": "text/turtle"})).read().decode()
    raw = g.replace("urn:simpl:vm:content:", "urn:simpl:vm:raw:")
    post("/update", "update", f'DROP SILENT GRAPH <{raw}> ; INSERT DATA {{ GRAPH <{raw}> {{ <{raw}> <https://vocabulary.simpl.local/ns#ttl> "{esc(ttl)}" }} }}')
    n += 1
print(f"content graphs: {len(rows)}, raw graphs written: {n}")
