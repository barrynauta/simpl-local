#!/usr/bin/env python3
"""Step 21a: check the OpenAPI files of one component against the API guideline rules.

Every rule of guidelines/rules.json with check "tool" that applies to an API specification
is implemented here, one function per rule id, so a finding always names its rule. Two
coverage checks are added: operations no criterion covers (COV-01, from the step 12b links)
and operations the stories name that no OpenAPI has (COV-02, from the step 12a issues).

Writes <component>/validation/api-checks.json and api-checks.md: one finding per rule and
API, with the number of places and the first examples.

Run: python3 pipeline/check_api.py contract-management
"""
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

import yaml

FACTORY = Path(__file__).resolve().parent.parent
METHODS = ("get", "put", "post", "patch", "delete", "head", "options")
LOWER_CAMEL = re.compile(r"^[a-z][a-zA-Z0-9]*$")
EUPL = "European Union Public License"


def api_files(comp, manifest):
    out = {}
    for a in manifest.get("api", []):
        if a.get("dest"):
            files = [comp / "specs" / a["dest"]]
            entry = files
        else:
            base = comp / "specs/api" / a["name"]
            files = sorted(base.rglob("*.y*ml"))
            entry = [base / e for e in a["entry"]] if a.get("entry") else files
        out[a["name"]] = (files, entry)
    return out


def load(f):
    try:
        return yaml.safe_load(f.read_text(encoding="utf-8")) or {}
    except yaml.YAMLError:
        return {}


_DOCS = {}


def _doc(f):
    f = Path(f).resolve()
    if f not in _DOCS:
        _DOCS[f] = load(f) if f.exists() else {}
    return _DOCS[f]


def deref(f, obj):
    """Follow '$ref' chains, local ('#/...') and to other files ('../common/x.yaml#/...'), up to 6 hops."""
    cur = Path(f).resolve()
    for _ in range(6):
        if not (isinstance(obj, dict) and isinstance(obj.get("$ref"), str)):
            return obj
        ref = obj["$ref"]
        target, _, pointer = ref.partition("#")
        if target:
            cur = (cur.parent / target).resolve()
        node = _doc(cur)
        for part in [x for x in pointer.split("/") if x]:
            node = node.get(part, {}) if isinstance(node, dict) else {}
        obj = node
    return obj


def walk_props(schema, path, out, seen):
    """Collect (location, property name, property schema) through nested objects and arrays."""
    if not isinstance(schema, dict) or id(schema) in seen:
        return
    seen.add(id(schema))
    for k in ("allOf", "oneOf", "anyOf"):
        for sub in schema.get(k, []) or []:
            walk_props(sub, path, out, seen)
    for name, ps in (schema.get("properties") or {}).items():
        out.append((f"{path}.{name}", name, ps))
        walk_props(ps, f"{path}.{name}", out, seen)
    if isinstance(schema.get("items"), dict):
        walk_props(schema["items"], path + "[]", out, seen)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    name = sys.argv[1]
    comp = FACTORY / name
    manifest = yaml.safe_load((comp / "sources.yaml").read_text(encoding="utf-8"))
    rules = {r["id"]: r for r in json.loads((FACTORY / "guidelines/rules.json").read_text(encoding="utf-8"))}
    inv = json.loads((comp / "inventory/inventory.json").read_text(encoding="utf-8"))
    links = json.loads((comp / "inventory/links.json").read_text(encoding="utf-8")) if (comp / "inventory/links.json").exists() else []
    context_only = {a["name"] for a in manifest.get("api", []) if a.get("context_only")}

    hits = defaultdict(list)  # (api, rule) -> locations

    def hit(api, rule, where):
        assert rule in rules or rule.startswith("COV-"), rule
        hits[(api, rule)].append(where)

    for api, (files, entry) in api_files(comp, manifest).items():
        docs = {f: load(f) for f in files}
        entry_docs = {f: docs[f] for f in entry if f in docs}
        # Document level: only the entry files describe the published API.
        for f, doc in entry_docs.items():
            fn = f.name
            ver = str(doc.get("openapi", ""))
            if not ver.startswith("3.0"):
                hit(api, "API-04", f"{fn}: openapi {ver or 'missing'}")
            if f.suffix not in (".yaml", ".yml"):
                hit(api, "API-05", fn)
            info = doc.get("info") or {}
            if not info.get("version"):
                hit(api, "API-34", fn)
            elif not re.match(r"^\d+\.\d+(\.\d+)?$", str(info["version"])):
                hit(api, "API-31", f"{fn}: version {info['version']}")
            if not str(info.get("description", "")).strip():
                hit(api, "API-48", fn)
            lic = info.get("license") or {}
            if EUPL not in str(lic.get("name", "")) or "1.2" not in str(lic.get("name", "")) + str(lic.get("url", "")):
                hit(api, "API-42", f"{fn}: license {lic.get('name', 'missing')}")
            contact = info.get("contact") or {}
            if contact.get("name") != "Simpl Programme" or contact.get("email") != "cnect-simpl@ec.europa.eu":
                hit(api, "API-43", f"{fn}: contact {contact.get('name', 'missing')}")
            servers = doc.get("servers") or []
            if not servers:
                hit(api, "API-24", f"{fn}: no servers")
            for s in servers:
                url = str(s.get("url", ""))
                if not re.search(r"/v\d+/?$", url):
                    hit(api, "API-24", f"{fn}: server {url}")
                if re.search(r"/v\d+\.\d+", url):
                    hit(api, "API-32", f"{fn}: server {url}")
                if re.search(r"/v0\b", url):
                    hit(api, "API-27", f"{fn}: server {url}")
                for seg in [x for x in re.sub(r"^https?://[^/]+", "", url).split("/") if x and not re.match(r"^v\d+$", x) and "{" not in x]:
                    if not LOWER_CAMEL.match(seg):
                        hit(api, "API-26", f"{fn}: server segment {seg}")
            schemes = set(((doc.get("components") or {}).get("securitySchemes") or {}).keys())
            root_sec = doc.get("security")
            for p, item in (doc.get("paths") or {}).items():
                item = item or {}
                if p.endswith("/") and p != "/":
                    hit(api, "API-08", p)
                for seg in [s for s in p.split("/") if s and not s.startswith("{")]:
                    if re.search(r"\.(json|xml|yaml|yml|csv|txt|html)$", seg, re.I):
                        hit(api, "API-09", p)
                    elif not LOWER_CAMEL.match(seg):
                        hit(api, "API-06", f"{p} ({seg})")
                    if re.search(r"(?i)^(own|my)[A-Z]|^(own|my)$", seg):
                        hit(api, "API-29", p)
                pparams = re.findall(r"\{([^}]+)\}", p)
                if len(pparams) == 1 and pparams[0] not in ("id", "code"):
                    hit(api, "API-52", f"{p} ({pparams[0]})")
                for m in METHODS:
                    op = item.get(m)
                    if not isinstance(op, dict):
                        continue
                    where = f"{m.upper()} {p}"
                    sec = op.get("security", root_sec)
                    if not sec and not re.search(r"(?i)health|status$|ready|live", p):
                        hit(api, "API-41", where)
                    for req in sec or []:
                        for sname in (req or {}):
                            if sname not in schemes:
                                hit(api, "API-40", f"{where}: {sname}")
                    for prm in (op.get("parameters") or []) + (item.get("parameters") or []):
                        prm = deref(f, prm)
                        if not isinstance(prm, dict) or "$ref" in prm:
                            continue
                        pn, pin = prm.get("name", ""), prm.get("in", "")
                        if not pn or not pin or not (prm.get("schema") or prm.get("content")):
                            hit(api, "API-56", f"{where}: {pn or '?'}")
                        if not str(prm.get("description", "")).strip():
                            hit(api, "API-57", f"{where}: {pn}")
                        if pin == "path" and prm.get("required") is not True:
                            hit(api, "API-61", f"{where}: {pn}")
                        if pin == "query" and not LOWER_CAMEL.match(pn):
                            hit(api, "API-07", f"{where}: {pn}")
                        if pin == "header" and not re.match(r"^[A-Z][a-z0-9]*(-[A-Z][a-z0-9]*)*$", pn):
                            hit(api, "API-23", f"{where}: {pn}")
                        sch = prm.get("schema") or {}
                        if isinstance(sch, dict) and sch.get("enum") and sch.get("type"):
                            pytype = {"string": str, "integer": int, "number": (int, float), "boolean": bool}.get(sch["type"])
                            if pytype and not all(isinstance(v, pytype) for v in sch["enum"]):
                                hit(api, "API-59", f"{where}: {pn}")
                    bodies = []
                    rb = deref(f, op.get("requestBody") or {})
                    if isinstance(rb, dict):
                        bodies += [("request", ct, mt) for ct, mt in (rb.get("content") or {}).items()]
                    for code, resp in (op.get("responses") or {}).items():
                        resp = deref(f, resp)
                        if not isinstance(resp, dict):
                            continue
                        for ct, mt in (resp.get("content") or {}).items():
                            bodies.append((str(code), ct, mt))
                            if str(code)[:1] in ("4", "5") and ct != "application/problem+json":
                                hit(api, "API-45", f"{where} {code}: {ct}")
                        for hn in (resp.get("headers") or {}):
                            if not re.match(r"^[A-Z][a-z0-9]*(-[A-Z][a-z0-9]*)*$", hn):
                                hit(api, "API-23", f"{where} {code}: header {hn}")
                    for kind, ct, mt in bodies:
                        mt = mt or {}
                        sch = deref(f, mt.get("schema") or {})
                        if not (mt.get("example") is not None or mt.get("examples") or (isinstance(sch, dict) and sch.get("example") is not None)):
                            hit(api, "API-21", f"{where} {kind} {ct}")
        # Property names: every schema in every file of the API, once per schema name.
        seen_schema = set()
        for f, doc in docs.items():
            for sname, sch in ((doc.get("components") or {}).get("schemas") or {}).items():
                if sname in seen_schema or not isinstance(sch, dict):
                    continue
                seen_schema.add(sname)
                props = []
                walk_props(sch, sname, props, set())
                for where, pn, ps in props:
                    if not LOWER_CAMEL.match(pn):
                        if re.search(r"[_\-.]", pn):
                            hit(api, "API-16", where)
                        elif re.match(r"^\d", pn):
                            hit(api, "API-17", where)
                        else:
                            hit(api, "API-13", where)
                    elif re.search(r"[A-Z]{3,}", pn):
                        hit(api, "API-14", where)
                    if re.search(r"(?i)^(info|information|data)$|[a-z](Info|Information|Data)$", pn):
                        hit(api, "API-18", where)
                    if pn.lower().startswith(sname.lower()) and pn.lower() != sname.lower():
                        hit(api, "API-20", where)
                    if isinstance(ps, dict) and ps.get("enum") and ps.get("type"):
                        pytype = {"string": str, "integer": int, "number": (int, float), "boolean": bool}.get(ps["type"])
                        if pytype and not all(isinstance(v, pytype) for v in ps["enum"]):
                            hit(api, "API-59", where)

    # Coverage: operations no criterion covers, and operations the stories name that do not exist.
    linked = {l["from"] for l in links} | {l["to"] for l in links}
    for a in inv["apis"]:
        if a["name"] in context_only:
            continue
        for o in a["operations"]:
            if o["id"] not in linked and not re.search(r"(?i)/health|/status$|/ready|/live", o["path"]):
                hit(a["name"], "COV-01", f"{o['method']} {o['path']}")
    for iss in inv.get("extraction_issues", []):
        m = re.match(r"(SIMPL-\d+): names (.+?), (?:which is in no API entry file|which is not in the (.+))$", iss)
        if m:
            hit("stories", "COV-02", f"{m.group(1)}: {m.group(2)}")
        m2 = re.match(r"(SIMPL-\d+): names (.+?), but the OpenAPI has that path with another method", iss)
        if m2:
            hit("stories", "COV-02", f"{m2.group(1)}: {m2.group(2)} (path exists with another method)")
    asyncapi = list((comp / "specs").rglob("*asyncapi*"))
    if not asyncapi:
        hit("component", "API-63", "no AsyncAPI file in the input set")

    cov_text = {"COV-01": "Operation that no acceptance criterion covers (step 12b links)",
                "COV-02": "Operation a story names that no OpenAPI entry file has"}
    findings = []
    for (api, rule), where in sorted(hits.items()):
        r = rules.get(rule, {})
        findings.append({"id": f"A-{len(findings) + 1:03d}", "api": api, "rule": rule,
                         "level": r.get("level", "SHOULD" if rule == "COV-01" else "MUST"),
                         "rule_text": r.get("rule", cov_text.get(rule, "")), "count": len(where), "examples": where[:8]})
    summary = {"component": name, "findings": len(findings), "places": sum(f["count"] for f in findings),
               "rules_hit": sorted({f["rule"] for f in findings}),
               "rules_checked": sorted(k for k, r in rules.items() if r["check"] == "tool" and "api-spec" in r["applies_to"])}
    out = comp / "validation"
    out.mkdir(exist_ok=True)
    (out / "api-checks.json").write_text(json.dumps({"summary": summary, "findings": findings}, indent=1,
                                                    ensure_ascii=False) + "\n", encoding="utf-8")
    md = [f"# API checks (step 21a): {name}", "",
          "Generated by [`pipeline/check_api.py`](../../pipeline/check_api.py): the OpenAPI entry files against the "
          "tool-checkable `API-` rules of [`guidelines/rules.md`](../../guidelines/rules.md), plus two coverage checks. "
          "One row per rule and API; the count is the number of places. Pointers, not verdicts: step 21b judges them.", "",
          f"{len(findings)} findings in {summary['places']} places; {len(summary['rules_hit'])} rules hit.", "",
          "| Id | API | Rule | Level | Places | Rule text | Examples |", "|---|---|---|---|---|---|---|"]
    for f in findings:
        ex = "; ".join(f["examples"][:3]).replace("|", "/")
        md.append(f"| {f['id']} | {f['api']} | {f['rule']} | {f['level']} | {f['count']} | {f['rule_text'][:90]} | {ex} |")
    (out / "api-checks.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if k != "rules_checked"}, indent=1))


if __name__ == "__main__":
    main()
