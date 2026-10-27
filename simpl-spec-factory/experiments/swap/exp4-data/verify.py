"""Experiment 4 check: compare every vocabulary and version as served by upstream (on the original data)
with what the build backend serves from the migrated copy. Usage: verify.py <upstream /v1> <build /v1> <build token>"""
import sys, json, urllib.request
up, bd, tok = sys.argv[1], sys.argv[2], sys.argv[3]
def get(base, path, auth):
    h = {"Accept": "application/json"}
    if auth: h["Authorization"] = "Bearer " + auth
    try: return json.load(urllib.request.urlopen(urllib.request.Request(base + path, headers=h)))
    except urllib.error.HTTPError as e: return {"_status": e.code}
def items(base, path, auth):
    out, page = [], 1
    while True:
        r = get(base, f"{path}?page={page}&pageSize=100", auth); out += r.get("items", [])
        if page >= r.get("totalPages", 0): return out
        page += 1
fake = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImxvY2FsQHNpbXBsLmxvY2FsIn0.devsignature"
U = {v["name"]: v for v in items(up, "/vocabularies", fake)}
Bv = {v["name"]: v for v in items(bd, "/vocabularies", tok)}
keys = ["status", "version", "description", "createdBy", "creationDate"]
print(f"vocabularies: upstream {len(U)}, build {len(Bv)}, missing in build {len(set(U)-set(Bv))}")
diff = {k: 0 for k in keys}; vdiff = {}; nver = 0; content_ok = 0
for n, u in U.items():
    b = Bv.get(n)
    if not b: continue
    for k in keys:
        if str(u.get(k)) != str(b.get(k)): diff[k] += 1
    uv = {x["version"]: x for x in items(up, f"/vocabularies/{n}/versions", fake)}
    bv = {x["version"]: x for x in items(bd, f"/vocabularies/{n}/versions", tok)}
    for ver, x in uv.items():
        nver += 1; y = bv.get(ver, {})
        for k in ["status", "description", "changelog", "creationDate"]:
            if str(x.get(k)) != str(y.get(k)): vdiff[k] = vdiff.get(k, 0) + 1
print("vocabulary field differences:", diff)
print(f"versions compared: {nver}, field differences:", vdiff or "none")
