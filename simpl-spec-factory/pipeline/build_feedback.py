#!/usr/bin/env python3
"""Step 29: assemble the spec feedback report of one component for the spec owners.

Collects the findings of steps 12b, 20 and 23 into one report:
<component>/validation/spec-feedback.md (and spec-feedback.json with every finding).
Nothing new is judged here: the report orders, groups and counts what the earlier steps
found, so a spec owner can act on it without reading the factory.

Run: python3 pipeline/build_feedback.py contract-management
"""
import datetime
import json
import re
import sys
from collections import Counter
from pathlib import Path

import yaml

FACTORY = Path(__file__).resolve().parent.parent
SEV = ["blocker", "major", "minor"]
THEMES = [  # (theme, codes) in the order the report shows them
    ("Behaviour and interactions", {"AC-C03", "AC-C04", "AC-C05"}),
    ("One language for the domain", {"DDD-01", "DDD-02", "DDD-03", "DDD-04", "DDD-05"}),
    ("Entities, identities and states", {"DDD-06", "DDD-07", "DDD-08", "DDD-09", "DDD-10", "DDD-11", "DDD-12"}),
    ("Boundaries with other components", {"DDD-13", "DDD-14", "DDD-15"}),
    ("Error cases and testability", {"AC-C01", "AC-C02"}),
    ("Data model governance", {"DATA-01", "DATA-04", "DATA-09", "API-01"}),
    ("Scope", {"AC-C06"}),
]


def theme(code):
    return next((t for t, codes in THEMES if code in codes), "Other")


def load(p, default):
    return json.loads(p.read_text(encoding="utf-8")) if p.exists() else default


def where(f):
    ids = f.get("ids") or []
    if f["target"] == "cross" and ids:
        return ", ".join(ids[:6]) + (f" and {len(ids) - 6} more" if len(ids) > 6 else "")
    return f["target"]


def cell(t):
    return str(t).replace("|", "/").replace("\n", " ")


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    name = sys.argv[1]
    comp = FACTORY / name
    v = comp / "validation"
    inv = load(comp / "inventory/inventory.json", {})
    links = load(comp / "inventory/links.json", [])
    ac = load(v / "ac-findings.json", [])
    dom = load(v / "domain-findings.json", [])
    acc = load(v / "ac-checks.json", {"summary": {}, "findings": []})
    domc = load(v / "domain-checks.json", {"summary": {}, "findings": []})
    manifest = yaml.safe_load((comp / "sources.yaml").read_text(encoding="utf-8"))
    cdm_src = load(FACTORY / "cdm/SOURCES.json", {"cdms": []})

    findings = []
    for f in ac:
        findings.append(f | {"code": f["check"], "step": "20b"})
    for f in dom:
        findings.append(f | {"code": f["rule"], "step": "23b"})
    for f in findings:
        f["theme"] = theme(f["code"])
    by_sev = Counter(f["severity"] for f in findings)

    title = manifest.get("component", name)
    live = [a for s in inv.get("stories", []) for a in s["acs"] if not a["struck"]]
    ops = [o for a in inv.get("apis", []) for o in a["operations"]]
    reqs = inv.get("bp_requirements", [])
    linked_from = {l["from"] for l in links}
    linked_to = {l["to"] for l in links}
    ac_unlinked = [a["id"] for a in live if a["id"] not in linked_from and a["id"] not in linked_to]
    op_unlinked = [o["id"] for o in ops if o["id"] not in linked_to and o["id"] not in linked_from]
    req_unlinked = [r["id"] for r in reqs if r["id"] not in linked_to and r["id"] not in linked_from]

    md = [f"# Spec feedback: {title}", "",
          f"Step 29 of the spec factory, assembled on {datetime.date.today():%d %b %Y} by "
          "[`pipeline/build_feedback.py`](../../pipeline/build_feedback.py). For the owners of the stories, the API "
          "definitions, the CDMs and the business process. Every point cites the ids it is about and the question that "
          "needs an answer; the full evidence is in the linked files.", "",
          "**What this is.** An automated reading of the published specifications, as a builder would read them, "
          "before anything is built. A finding says where two careful readers would build different things, or where "
          "a builder would have to guess. It is not a judgement on the people who wrote the specs: most points are "
          "normal for specifications written by several teams over time.", "",
          "## In short", "",
          f"- **Input:** {len(inv.get('stories', []))} stories with {len(live)} live acceptance criteria, "
          f"{len(ops)} API operations, {len(reqs)} business process requirements, and the CDMs of each API.",
          f"- **Findings:** {len(findings)}: **{by_sev['blocker']} to decide before building**, "
          f"{by_sev['major']} to confirm, {by_sev['minor']} small.",
          f"- **Mechanical checks:** {acc['summary'].get('findings', 0)} hits on the acceptance criteria and "
          f"{domc['summary'].get('findings', 0)} on the API and CDM names; the agents triaged them, and the real ones "
          "are part of the findings above.",
          f"- **Coverage:** {len(ac_unlinked)} of {len(live)} criteria link to no operation and no process requirement; "
          f"{len(op_unlinked)} of {len(ops)} operations and {len(req_unlinked)} of {len(reqs)} process requirements "
          "have no criterion.", ""]

    md += ["## Decide first", "",
           "The blockers. A builder or tester cannot proceed on these without a decision, and two builders would decide "
           "differently.", ""]
    for t, _ in THEMES + [("Other", set())]:
        items = [f for f in findings if f["severity"] == "blocker" and f["theme"] == t]
        if not items:
            continue
        md += [f"### {t}", ""]
        for f in items:
            md.append(f"**{f['id']}** ({f['code']}). {f['finding']}")
            md.append("")
            md.append(f"- Where: {where(f)}")
            md.append(f"- Question: {f.get('question', '')}")
            for o in f.get("options") or []:
                md.append(f"  - Option: {o}")
            md.append("")

    gl = v / "glossary.md"
    if gl.exists():
        rows = [r for r in gl.read_text(encoding="utf-8").splitlines() if r.startswith("| ") and not r.startswith("| Term")]
        def st(r):
            last = r.rstrip(" |").rsplit("|", 1)[-1].strip().lower()
            return next((k for k in ("agreed", "proposed", "open") if last.startswith(k)), None)
        status = Counter(st(r) for r in rows if r.count("|") > 6 and st(r))
        open_terms = [r.split("|")[1].strip() for r in rows if r.count("|") > 6 and st(r) == "open"]
        md += ["## Terms to agree", "",
               f"The draft glossary ([`glossary.md`](./glossary.md)) has {sum(status.values())} terms: "
               f"{status.get('agreed', 0)} already used the same way everywhere, {status.get('proposed', 0)} where the "
               f"draft proposes one term among synonyms, and {status.get('open', 0)} where the sources disagree on the "
               "meaning. One agreed term per concept, used the same way in the stories, the API and the CDM, removes "
               "a large part of the findings above.", ""]
        if open_terms:
            md += ["Open: " + ", ".join(open_terms) + ".", ""]

    md += ["## To confirm", "",
           "The majors: the build can proceed on a sensible default, but the default should be confirmed.", "",
           "| Id | Theme | Where | Finding | Question |", "|---|---|---|---|---|"]
    for f in sorted((f for f in findings if f["severity"] == "major"), key=lambda f: ([t for t, _ in THEMES] + ["Other"]).index(f["theme"])):
        md.append(f"| {f['id']} | {f['theme']} | {cell(where(f))} | {cell(f['finding'])} | {cell(f.get('question', ''))} |")
    md += ["", f"The {by_sev['minor']} minor findings (wording, numbering, duplicates) are in "
           "[`ac-findings.json`](./ac-findings.json) and [`domain-findings.json`](./domain-findings.json).", ""]

    md += ["## Coverage", "",
           "From the links between criteria, operations and process requirements (step 12b, "
           "[`../inventory/inventory-review.md`](../inventory/inventory-review.md)). A gap is not always wrong, "
           "but each one is a place where the specs do not say how the parts connect.", "",
           f"- Operations with no criterion ({len(op_unlinked)}): " + (", ".join(f"`{x}`" for x in op_unlinked) or "none"),
           f"- Process requirements with no criterion ({len(req_unlinked)}): " + (", ".join(f"`{x}`" for x in req_unlinked) or "none"),
           f"- Criteria with no operation and no process requirement: {len(ac_unlinked)}, listed in the review.", ""]

    cmap = {k: val for k, val in (manifest.get("cdm") or {}).items() if k != "context"}
    by_file = {r.get("file"): r for r in cdm_src["cdms"]}
    md += ["## The data models (CDMs)", "", "| API | CDM | Register status | Approved | Branch |", "|---|---|---|---|---|"]
    for api_name, fn in cmap.items():
        r = by_file.get(fn, {})
        md.append(f"| {api_name} | {fn or 'none'} | {r.get('status', 'not found')} | {r.get('approved', '')} | {r.get('branch', '')} |")
    md += ["", "See [`../../cdm/README.md`](../../cdm/README.md) for the state of the CDM register as a whole.", ""]

    md += ["## How this was made", "",
           "| Step | What | File |", "|---|---|---|",
           "| 12 | every story, criterion, operation and process requirement with a stable id, and the links between them | [`../inventory/`](../inventory/inventory.md) |",
           "| 20a, 20b | the acceptance criteria: structure and wording (script), then completeness, testability, ambiguity, consistency and correctness (agent) | [`ac-checks.md`](./ac-checks.md), [`ac-review.md`](./ac-review.md) |",
           "| 23a, 23b | the domain language: API against CDM against the stories, by the `DDD-` and CDM rules; the draft glossary | [`domain-checks.md`](./domain-checks.md), [`domain-review.md`](./domain-review.md) |",
           "", "The agents worked only from the published specs and each other's output, not from code or earlier "
           "builds. Their findings were not checked one by one by a person; treat each as a well-argued question, "
           "not a verdict.", ""]
    (v / "spec-feedback.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    (v / "spec-feedback.json").write_text(json.dumps({"component": name, "counts": dict(by_sev), "findings": findings},
                                                     indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"{name}: {len(findings)} findings ({dict(by_sev)}), report in {v / 'spec-feedback.md'}")


if __name__ == "__main__":
    main()
