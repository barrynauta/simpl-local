#!/usr/bin/env python3
"""Step 23a: mechanical checks of the domain language: API against CDM against the ACs.

For each API of a component (from its sources.yaml), takes the CDM that the manifest maps
to it (from the snapshot in cdm/) and compares:

  - is there a CDM, is it approved, does its API link point to the API we have;
  - API schemas against CDM classes (DDD-06, DDD-07), properties against attributes (DDD-08);
  - the names under which each entity's identity appears (DDD-09);
  - enum values in the API against CDM value sets and quoted statuses in the ACs (DDD-04);
  - CDM class names used in the stories and the business process, and the reverse (DDD-01);
  - naming of CDM classes and attributes (DATA-09 and the CDM naming rules).

Writes <component>/validation/domain-checks.json and domain-checks.md. Pointers, not
verdicts: step 23b (an agent) judges them and drafts the glossary.

Run: python3 pipeline/check_domain.py contract-management
"""
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

import yaml

FACTORY = Path(__file__).resolve().parent.parent
TECHNICAL_SCHEMA = re.compile(r"^(Health\w*|Problem\w*|Error\w*|Link\w*|Pageable|Sort)$")
WRAPPER = re.compile(r"(Request|Response|Dto|DTO|Page|List|Result|Results|Error|Problem|Wrapper|Payload|Body|Input|Output|Params|Filter|Criteria|Summary|Details|Data|Info|Create|Update|Patch)$")
TECH = re.compile(r"(Request|Response|Dto|DTO|Entity|Table|Record|Bean|Impl|Json|Payload)$")


def norm(name):
    return re.sub(r"[^a-z0-9]", "", name.lower())


def stem(name):
    """Strip wrapper suffixes repeatedly: ContractOfferStatusUpdateRequest -> ContractOfferStatus."""
    prev = None
    while prev != name:
        prev = name
        name = WRAPPER.sub("", name) or prev
    return name


def api_files(comp, manifest):
    out = {}
    for a in manifest.get("api", []):
        if a.get("dest"):
            out[a["name"]] = [comp / "specs" / a["dest"]]
        else:
            out[a["name"]] = sorted((comp / "specs/api" / a["name"]).rglob("*.y*ml"))
    return out


def load_api(files):
    schemas, params, ops = {}, Counter(), 0
    for f in files:
        try:
            doc = yaml.safe_load(f.read_text(encoding="utf-8")) or {}
        except yaml.YAMLError:
            continue
        for name, sch in ((doc.get("components") or {}).get("schemas") or {}).items():
            if not isinstance(sch, dict):
                continue
            props = dict(sch.get("properties") or {})
            for part in sch.get("allOf", []) or []:
                if isinstance(part, dict):
                    props.update(part.get("properties") or {})
            enums = {}
            if sch.get("enum"):
                enums[""] = [str(v) for v in sch["enum"]]
            for pn, pv in props.items():
                if isinstance(pv, dict):
                    e = pv.get("enum") or (pv.get("items") or {}).get("enum") if isinstance(pv.get("items"), dict) else pv.get("enum")
                    if e:
                        enums[pn] = [str(v) for v in e]
            schemas.setdefault(name, {"properties": list(props), "enums": enums, "file": f.name})
        for p, item in (doc.get("paths") or {}).items():
            for m in re.findall(r"\{([^}]+)\}", p):
                params[m] += 1
            ops += sum(1 for k in (item or {}) if k in ("get", "post", "put", "patch", "delete"))
    return schemas, params, ops


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    comp = FACTORY / sys.argv[1]
    manifest = yaml.safe_load((comp / "sources.yaml").read_text(encoding="utf-8"))
    cdm_map = dict(manifest.get("cdm") or {})
    context = cdm_map.pop("context", []) or []
    cdm_inv = json.loads((FACTORY / "cdm/inventory.json").read_text(encoding="utf-8"))
    cdms = {c["file"]: c for c in cdm_inv["cdms"]}
    inv = json.loads((comp / "inventory/inventory.json").read_text(encoding="utf-8"))
    rules = {r["id"]: r for r in json.loads((FACTORY / "guidelines/rules.json").read_text(encoding="utf-8"))}

    findings = []

    def f(target, rule, detail, evidence=""):
        assert rule in rules, rule
        findings.append({"id": target, "rule": rule, "detail": detail, "evidence": evidence})

    # Text of the stories and the business process, for the language checks.
    story_text = {s["key"]: " ".join([s["title"]] + [c["text"] for a in s["acs"] if not a["struck"] for c in a["clauses"]])
                  for s in inv["stories"]}
    bp_text = " ".join(r["title"] + " " + r["text"] for r in inv["bp_requirements"])
    all_text = (" ".join(story_text.values()) + " " + bp_text).lower()
    quoted_status = Counter(m.strip() for t in story_text.values() for m in re.findall(r"['‘’\"]([A-Z][A-Za-z ]{2,30})['‘’\"]", t))

    files = api_files(comp, manifest)
    per_api = []
    for api, flist in files.items():
        schemas, params, nops = load_api(flist)
        schemas = {k: v for k, v in schemas.items() if not TECHNICAL_SCHEMA.match(k)}  # health, errors: not domain
        cfile = cdm_map.get(api, "missing")
        row = {"api": api, "schemas": len(schemas), "operations": nops, "cdm": cfile}
        if not cfile or cfile == "missing" or cfile not in cdms:
            f(api, "DDD-06", "no CDM for this API in the snapshot" + ("" if api in cdm_map else " (not mapped in sources.yaml)"))
            per_api.append(row)
            continue
        c = cdms[cfile]
        if c["approved"].lower() != "yes":
            f(api, "DATA-01", f"the CDM is not approved for delivery (register: {c['status']}, approved {c['approved']})", cfile)
        api_project = next((a["project"] for a in manifest["api"] if a["name"] == api), "")
        links = c["api_links"]
        foreign = [l for l in links if l.startswith("http") and api_project not in l]
        if foreign:
            f(api, "DDD-06", "the CDM points to an API definition in another repository", foreign[0])
        if not links:
            f(api, "DDD-06", "the CDM names no API definition", cfile)
        classes = {cl["name"]: cl for cl in c["classes"]}
        own = {n: cl for n, cl in classes.items() if not cl["external"]}
        by_norm = {norm(n): n for n in classes}
        matched, wrappers, unmatched = {}, [], []
        for s in sorted(schemas):
            hit = by_norm.get(norm(s)) or by_norm.get(norm(stem(s)))
            if hit:
                matched[s] = hit
            elif WRAPPER.search(s):
                wrappers.append(s)
            else:
                unmatched.append(s)
        for s in unmatched:
            f(f"{api}#{s}", "DDD-06", "API schema with no CDM class of the same name", schemas[s]["file"])
        for s in wrappers:
            f(f"{api}#{s}", "DDD-07", f"schema looks like a wrapper and has no CDM class; check that it adds no business concept", schemas[s]["file"])
        unused = [n for n in own if n not in matched.values() and norm(n) not in {norm(stem(s)) for s in schemas}]
        for n in unused:
            f(f"{cfile}#{n}", "DDD-06", "CDM class (not external) that no API schema exposes", "")
        # Attributes of matched pairs.
        for s, cn in matched.items():
            attrs = {norm(a["name"]): a["name"] for a in classes[cn]["attributes"]}
            for p in schemas[s]["properties"]:
                if norm(p) not in attrs:
                    f(f"{api}#{s}.{p}", "DDD-08", f"property has no attribute in CDM class {cn}", "")
            for na, a in attrs.items():
                if na not in {norm(p) for p in schemas[s]["properties"]} and classes[cn]["attributes"]:
                    pass  # an attribute not exposed is fine; the agent looks at it if it matters
        # Identity names per entity.
        idnames = defaultdict(set)
        for p in list(params):
            idnames[norm(re.sub(r"(?i)_?id$", "", p))].add(p)
        for s in schemas:
            for p in schemas[s]["properties"]:
                if re.search(r"(?i)(^id$|Id$|_id$)", p):
                    key = norm(re.sub(r"(?i)_?id$", "", p)) or norm(stem(s))
                    idnames[key].add(p)
        for key, names in idnames.items():
            forms = {n for n in names}
            if len({norm(n) for n in forms}) == 1 and len(forms) > 1:
                f(api, "DDD-09", f"one identity written in {len(forms)} ways", ", ".join(sorted(forms)))
        # Enums against CDM value sets and AC statuses.
        cdm_values = {norm(v) for vs in c["valuesets"] for v in vs["values"]}
        ac_values = {norm(v) for v in quoted_status}
        for s, sch in schemas.items():
            for pn, vals in sch["enums"].items():
                miss_cdm = [v for v in vals if norm(v) not in cdm_values]
                if miss_cdm and (c["valuesets"] or "status" in pn.lower()):
                    f(f"{api}#{s}.{pn or '(enum)'}", "DDD-04", "enum values not in a CDM value set" +
                      ("" if c["valuesets"] else " (the CDM has no value sets)"), ", ".join(miss_cdm[:8]))
                if "status" in (pn or s).lower() and ac_values:
                    miss_ac = [v for v in vals if norm(v) not in ac_values]
                    if miss_ac:
                        f(f"{api}#{s}.{pn or '(enum)'}", "DDD-04", "status values the ACs never use", ", ".join(miss_ac[:8]))
        # Naming of the CDM itself.
        for n, cl in own.items():
            if TECH.search(n):
                f(f"{cfile}#{n}", "DATA-09", "class name ends in a technical word, not a business noun", n)
            for a in cl["attributes"]:
                if "_" in a["name"]:
                    f(f"{cfile}#{n}.{a['name']}", "DATA-09", "attribute in snake_case in a conceptual model", a["name"])
        # Language: CDM class names in the stories and the process.
        for n in own:
            words = re.sub(r"(?<!^)(?=[A-Z])", " ", n).lower()
            if words not in all_text:
                f(f"{cfile}#{n}", "DDD-01", "CDM class whose name never appears in the stories or the business process",
                  words)
        row |= {"cdm_classes_own": len(own), "matched": len(matched), "wrappers": len(wrappers),
                "unmatched": len(unmatched), "cdm_classes_not_exposed": len(unused)}
        per_api.append(row)

    # Statuses the ACs quote that no API enum and no CDM value set has.
    all_enum = {norm(v) for api, fl in files.items() for sch in load_api(fl)[0].values() for vals in sch["enums"].values() for v in vals}
    all_cdm_vals = {norm(v) for fn in list(cdm_map.values()) + context if fn in cdms for vs in cdms[fn]["valuesets"] for v in vs["values"]}
    for v, n in quoted_status.most_common():
        if norm(v) not in all_enum and norm(v) not in all_cdm_vals and re.search(r"(?i)request|ed$|active|pending|draft", v):
            f("stories", "DDD-04", f"status quoted in {n} AC clause(s) with no API enum value or CDM value", v)
    for fn in context:
        if fn not in cdms:
            f("context", "DDD-13", "a context CDM in sources.yaml is not in the snapshot", fn)

    by_rule = Counter(x["rule"] for x in findings)
    summary = {"component": sys.argv[1], "apis": per_api, "findings": len(findings), "by_rule": dict(sorted(by_rule.items()))}
    out = comp / "validation"
    out.mkdir(exist_ok=True)
    (out / "domain-checks.json").write_text(json.dumps({"summary": summary, "findings": findings}, indent=1,
                                                       ensure_ascii=False) + "\n", encoding="utf-8")
    md = [f"# Domain checks (step 23a): {sys.argv[1]}", "",
          "Generated by [`pipeline/check_domain.py`](../../pipeline/check_domain.py): each API against the CDM mapped to it in "
          "`sources.yaml` (snapshot in [`../../cdm/`](../../cdm/inventory.md)), and both against the language of the stories. "
          "Rule ids from [`guidelines/rules.md`](../../guidelines/rules.md). Pointers, not verdicts; step 23b judges them.", "",
          "| API | Operations | Schemas | CDM | Own CDM classes | Schemas matched | Wrappers | Unmatched | CDM classes not exposed |",
          "|---|---|---|---|---|---|---|---|---|"]
    for r in per_api:
        md.append(f"| {r['api']} | {r['operations']} | {r['schemas']} | {r['cdm']} | {r.get('cdm_classes_own', '')} | "
                  f"{r.get('matched', '')} | {r.get('wrappers', '')} | {r.get('unmatched', '')} | {r.get('cdm_classes_not_exposed', '')} |")
    md += ["", "| Rule | Findings | Rule text |", "|---|---|---|"]
    md += [f"| {k} | {v} | {rules[k]['rule']} |" for k, v in sorted(by_rule.items())]
    md += ["", "## Findings", "", "| Where | Rule | Detail | Evidence |", "|---|---|---|---|"]
    md += [f"| `{x['id']}` | {x['rule']} | {x['detail']} | {x['evidence'].replace('|', '/')} |" for x in findings]
    (out / "domain-checks.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=1))


if __name__ == "__main__":
    main()
