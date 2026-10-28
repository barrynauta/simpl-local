"""Swap experiment 1: the same API walk against upstream and the factory build.

Runs one fixed sequence of calls (every in-scope operation of the canonical
OpenAPI, plus GET /schemas/{name}, which the OpenAPI lacks) and records, per
step, the status code, the content type and the field names of the answer.
Schema files are the bridge files of make_bridge.sh, so both backends accept them.

    python api_walk.py upstream    # http://localhost:8085, self-made token
    python api_walk.py build       # http://localhost:8098/v1, Keycloak token
    python api_walk.py compare     # results/api-compare.txt
    python api_walk.py free-slots build   # before a UI walk: revoke the published Data and Application schemas

Needs the tests venv (requests). No source of either backend is read.
"""
import json
import os
import sys
import time
from pathlib import Path

import requests

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent.parent / "tests" / "features"))
import smlib  # noqa: E402  (self_made_token only)

FIX = HERE / "fixtures"
RES = HERE / "results"
TARGETS = {
    "upstream": {"base": os.environ.get("UP_URL", "http://localhost:8085"), "auth": "none"},
    "build": {"base": os.environ.get("BUILD_URL", "http://localhost:8098/v1"), "auth": "oidc"},
}
KEYCLOAK = os.environ.get("KEYCLOAK_URL", "http://localhost:8185")
RECEIVER = "https://catalogue.example.com/internal/events/swap"


def token(target, user):
    if TARGETS[target]["auth"] == "none":
        return smlib.self_made_token(user)
    r = requests.post(
        f"{KEYCLOAK}/realms/simpl/protocol/openid-connect/token",
        data={"grant_type": "password", "client_id": "schema-tests", "username": user, "password": "password"},
        timeout=10,
    )
    r.raise_for_status()
    return r.json()["access_token"]


def shape(body):
    """Field names, one level into lists and objects, never values."""
    if isinstance(body, dict):
        out = {}
        for k, v in sorted(body.items()):
            if isinstance(v, list) and v and isinstance(v[0], dict):
                out[k] = ["[]"] + sorted(v[0].keys())
            elif isinstance(v, dict):
                out[k] = sorted(v.keys())
            else:
                out[k] = type(v).__name__
        return out
    if isinstance(body, list):
        return ["[]"] + (sorted(body[0].keys()) if body and isinstance(body[0], dict) else [])
    return None


def free_slots(target, admin=None, rtypes=("Data", "Application")):
    """Revoke every PUBLISHED schema of these resource types (D16: one PUBLISHED schema per
    resourceType, the first one wins), so the next create is published."""
    base = TARGETS[target]["base"]
    admin = admin or token(target, "ga-admin")
    hdr = {"Authorization": f"Bearer {admin}"}
    for rtype in rtypes:
        r = requests.get(f"{base}/schemas?resourceType={rtype}&status=PUBLISHED", headers=hdr, timeout=30)
        for m in r.json().get("metadata", []):
            if m.get("resourceType") == rtype and m.get("status") == "PUBLISHED":
                requests.patch(f"{base}/schemas/{m['name']}", json={"status": "REVOKED"}, headers=hdr, timeout=30)


def walk(target):
    base = TARGETS[target]["base"]
    admin, viewer = token(target, "ga-admin"), token(target, "ga-viewer")
    run = time.strftime("%H%M%S")
    a, b, c = f"SwapA{run}", f"SwapB{run}", f"SwapC{run}"
    steps = []

    def call(label, method, path, user=admin, expect_note="", **kw):
        headers = kw.pop("headers", {})
        if user:
            headers["Authorization"] = f"Bearer {user}"
        r = requests.request(method, base + path, headers=headers, timeout=30, **kw)
        ctype = r.headers.get("Content-Type", "").split(";")[0]
        try:
            body = r.json()
        except ValueError:
            body = None
        step = {
            "label": label, "call": f"{method} {path.replace(run, '<run>')}", "status": r.status_code,
            "type": ctype, "shape": shape(body), "location": bool(r.headers.get("Location")),
        }
        if isinstance(body, dict):
            for k in ("status", "schemaVersion", "version", "latestVersion", "total"):
                if k in body and not isinstance(body[k], (dict, list)):
                    step.setdefault("values", {})[k] = body[k]
            if "type" in body and isinstance(body["type"], str) and body["type"].startswith("urn:"):
                step.setdefault("values", {})["problemType"] = body["type"]
            if isinstance(body.get("metadata"), list):
                step.setdefault("values", {})["items"] = [
                    {k: m.get(k) for k in ("name", "status", "version", "latestVersion") if k in m}
                    for m in body["metadata"] if str(m.get("name", "")).endswith(run)
                ]
        if isinstance(body, list):
            step.setdefault("values", {})["items"] = [
                {k: m.get(k) for k in ("version", "status", "webhookId") if k in m} for m in body
                if not isinstance(m, dict) or "targetUrl" not in m or m["targetUrl"] == RECEIVER
            ]
        if ctype.startswith("text/"):
            step["bytes"] = len(r.content)
        steps.append(step)
        return r, body

    def upload(name, kind="data", rtype="Data", user=admin, **extra):
        f = FIX / f"bridge-{kind}-offering.ttl"
        data = {"name": name, "title": f"Title {name}", "description": f"Swap walk {name}"}
        if rtype:
            data["resourceType"] = rtype
        data.update(extra)
        return call(f"create {name.replace(run, '')} ({kind}, {rtype})", "POST", "/schemas", user=user,
                    files={"schemaFile": (f.name, f.read_bytes(), "text/turtle")}, data=data)

    free_slots(target, admin)

    call("list, no filter", "GET", "/schemas")
    upload(a)
    upload(a)  # duplicate name
    upload("bad name")
    upload(f"SwapNoAuth{run}", user=None)
    upload(f"SwapViewer{run}", user=viewer)
    upload(f"SwapMismatch{run}", kind="data", rtype="Application")
    upload(f"SwapNoType{run}", rtype=None)
    upload(c, kind="application", rtype="Application")
    call("list as viewer", "GET", "/schemas", user=viewer)
    call("list, filter Data + PUBLISHED", "GET", "/schemas?resourceType=Data&status=PUBLISHED")
    call("list, schemaType SEARCH", "GET", "/schemas?schemaType=SEARCH")
    call("list, pageSize 1000", "GET", "/schemas?page=1&pageSize=1000")
    call("list, Accept xml", "GET", "/schemas", headers={"Accept": "application/xml"})
    call("get one schema (not in OpenAPI)", "GET", f"/schemas/{a}")
    call("versions of A", "GET", f"/schemas/{a}/versions")
    call("versions of unknown", "GET", f"/schemas/Unknown{run}/versions")
    call("content A v1, Accept turtle", "GET", f"/schemas/{a}/1", headers={"Accept": "text/turtle"})
    call("content A v1, Accept json", "GET", f"/schemas/{a}/1", headers={"Accept": "application/json"})
    call("content A v99", "GET", f"/schemas/{a}/99", headers={"Accept": "text/turtle"})
    f = FIX / "bridge-data-offering.ttl"
    call("new version of A", "POST", f"/schemas/{a}/versions",
         files={"schemaFile": (f.name, f.read_bytes(), "text/turtle")},
         data={"changelog": "Swap walk second version", "resourceType": "Data"})
    call("new version, no changelog", "POST", f"/schemas/{a}/versions",
         files={"schemaFile": (f.name, f.read_bytes(), "text/turtle")}, data={"resourceType": "Data"})
    call("versions of A after v2", "GET", f"/schemas/{a}/versions")
    upload(b)  # second Data schema while A holds the slot
    call("list, filter Data, after B", "GET", "/schemas?resourceType=Data")
    call("revoke A", "PATCH", f"/schemas/{a}", json={"status": "REVOKED"})
    call("revoke A again", "PATCH", f"/schemas/{a}", json={"status": "REVOKED"})
    call("PATCH A to PUBLISHED", "PATCH", f"/schemas/{a}", json={"status": "PUBLISHED"})
    call("new version of B, slot held by A", "POST", f"/schemas/{b}/versions",
         files={"schemaFile": (f.name, f.read_bytes(), "text/turtle")},
         data={"changelog": "Swap walk, slot held", "resourceType": "Data"})
    call("list, filter Data, after B v2", "GET", "/schemas?resourceType=Data")
    call("revoke A to free the slot", "PATCH", f"/schemas/{a}", json={"status": "REVOKED"})
    call("new version of B, slot free", "POST", f"/schemas/{b}/versions",
         files={"schemaFile": (f.name, f.read_bytes(), "text/turtle")},
         data={"changelog": "Swap walk, B takes the free slot", "resourceType": "Data"})
    call("list, filter Data, after B v3", "GET", "/schemas?resourceType=Data")
    call("revoke as viewer", "PATCH", f"/schemas/{c}", user=viewer, json={"status": "REVOKED"})
    r, hook = call("webhook create", "POST", "/webhooks",
                   json={"targetUrl": RECEIVER, "events": ["SchemaPublished", "SchemaRevoked"]})
    call("webhook duplicate", "POST", "/webhooks",
         json={"targetUrl": RECEIVER, "events": ["SchemaPublished", "SchemaRevoked"]})
    call("webhook unknown event", "POST", "/webhooks", json={"targetUrl": RECEIVER, "events": ["Nope"]})
    call("webhook list", "GET", "/webhooks")
    hid = (hook or {}).get("webhookId") or (hook or {}).get("id") or "00000000-0000-0000-0000-000000000000"
    call("webhook delete", "DELETE", f"/webhooks/{hid}")
    call("webhook delete again", "DELETE", f"/webhooks/{hid}")
    call("webhook delete malformed id", "DELETE", "/webhooks/not-a-uuid")

    RES.mkdir(exist_ok=True)
    out = RES / f"api-{target}.json"
    out.write_text(json.dumps(steps, indent=1))
    print(f"{target}: {len(steps)} calls -> {out.relative_to(HERE)}")


def compare():
    up = json.loads((RES / "api-upstream.json").read_text())
    bu = json.loads((RES / "api-build.json").read_text())
    lines, same_status, same_shape = [], 0, 0
    for u, b in zip(up, bu):
        s = u["status"] == b["status"]
        sh = u["shape"] == b["shape"]
        same_status += s
        same_shape += s and sh
        mark = "=" if s and sh else ("~" if s else "x")
        lines.append(f"{mark} {u['label']:<40} upstream {u['status']} {u['type'] or '-':<20} build {b['status']} {b['type'] or '-'}")
        if not sh and (u["shape"] or b["shape"]):
            uk, bk = u["shape"] or {}, b["shape"] or {}
            if isinstance(uk, dict) and isinstance(bk, dict):
                only_u = sorted(set(uk) - set(bk))
                only_b = sorted(set(bk) - set(uk))
                diff_in = [k for k in set(uk) & set(bk) if uk[k] != bk[k]]
                if only_u:
                    lines.append(f"    fields only upstream: {', '.join(only_u)}")
                if only_b:
                    lines.append(f"    fields only build:    {', '.join(only_b)}")
                for k in sorted(diff_in):
                    lines.append(f"    {k}: upstream {uk[k]} / build {bk[k]}")
            else:
                lines.append(f"    upstream {uk} / build {bk}")
        if u.get("values") != b.get("values"):
            lines.append(f"    values: upstream {json.dumps(u.get('values'))}")
            lines.append(f"            build    {json.dumps(b.get('values'))}")
    head = (f"{len(up)} calls: same status {same_status}, same status and field names {same_shape}\n"
            "= same status and fields, ~ same status other fields, x other status\n")
    (RES / "api-compare.txt").write_text(head + "\n".join(lines) + "\n")
    print(head + "\n".join(lines))


if __name__ == "__main__":
    arg = sys.argv[1] if len(sys.argv) > 1 else ""
    if arg in TARGETS:
        walk(arg)
    elif arg == "free-slots" and len(sys.argv) > 2 and sys.argv[2] in TARGETS:
        free_slots(sys.argv[2])
    elif arg == "compare":
        compare()
    else:
        sys.exit(__doc__)
