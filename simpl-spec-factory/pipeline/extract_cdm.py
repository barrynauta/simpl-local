#!/usr/bin/env python3
"""Step 12 (CDMs): extract the classes, attributes, relations and value sets of every CDM.

Reads the frozen CDM snapshot (cdm/*.md, from collect_cdms.py) and writes
cdm/inventory.json and cdm/inventory.md. Ids: CDM:<file>:<Class> and
CDM:<file>:<Class>.<attribute>. Mechanical only; the CDMs are Mermaid class diagrams
with a few variants (attributes with or without a type, stereotypes inside the class or
on their own line, value sets as a list under "State/value sets").

Run: python3 pipeline/extract_cdm.py [--dir cdm]
"""
import argparse
import json
import re
from pathlib import Path

FACTORY = Path(__file__).resolve().parent.parent
CLASS_OPEN = re.compile(r"^\s*class\s+([A-Za-z_][\w]*)\s*(?:\[\"[^\"]*\"\])?\s*(?:~[^~]*~)?\s*(\{)?\s*$")
STEREO_LINE = re.compile(r"^\s*<<\s*([^>]+?)\s*>>\s*([A-Za-z_]\w*)?\s*$")
REL = re.compile(r'^\s*([A-Za-z_]\w*)\s*(?:"([^"]*)")?\s*(<\|--|--\|>|\*--|--\*|o--|--o|-->|<--|\.\.>|<\.\.|\.\.\|>|<\|\.\.|--|\.\.)\s*(?:"([^"]*)")?\s*([A-Za-z_]\w*)\s*(?::\s*(.*))?$')
ATTR = re.compile(r"^\s*[+\-#~]?\s*(?:(?P<t1>[\w<>\[\],. ]+?)\s+(?P<n1>[A-Za-z_]\w*)|(?P<n2>[A-Za-z_]\w*)(?:\[\])?\s*(?::\s*(?P<t2>.+?))?)\s*$")
VALUESET = re.compile(r"^\s*[-*]\s*`?([A-Za-z_]\w*)\.([A-Za-z_]\w*)`?\s*:\s*(.*)$")


def parse(path):
    text = path.read_text(encoding="utf-8")
    api_links = list(dict.fromkeys(re.findall(r"(https?://[^\s)\]]+?\.ya?ml|(?<![\w/])(?:\.\./)*openapi/[^\s)\]]+?\.ya?ml)", text)))
    classes, rels, issues = {}, [], []
    blocks = re.findall(r"```mermaid\s*\n(.*?)```", text, re.S)
    if not blocks:
        issues.append("no Mermaid diagram")
    for block in blocks:
        cur = None
        block = re.sub(r"^\s*---\n.*?\n---\s*\n", "", block, flags=re.S)  # Mermaid front matter (layout config)
        if not block.lstrip().startswith("classDiagram"):
            kind = block.strip().split()[0] if block.strip() else "empty"
            issues.append(f"a Mermaid {kind} diagram, not a class diagram: not read")
            continue
        for ln in block.splitlines():
            s = ln.strip()
            if not s or s.startswith("%%") or s == "classDiagram":
                continue
            if cur is not None:
                if s == "}":
                    cur = None
                    continue
                st = STEREO_LINE.match(s)
                if st:
                    classes[cur]["stereotypes"].append(st.group(1).lower())
                    continue
                if "(" in s:  # a method: not part of a conceptual model
                    classes[cur]["methods"].append(s)
                    continue
                s = s.rstrip(",;").strip()
                if "enumeration" in classes[cur]["stereotypes"]:
                    classes[cur]["attributes"].append({"name": s, "type": "", "pk": False})  # a value, as written
                    continue
                m = ATTR.match(s)
                if m:
                    name = m.group("n1") or m.group("n2")
                    typ = (m.group("t1") or m.group("t2") or "").strip()
                    pk = bool(re.search(r"\bPK\b", typ))
                    typ = re.sub(r"\bPK\b|\bFK\b", "", typ).strip()
                    classes[cur]["attributes"].append({"name": name, "type": typ, "pk": pk})
                else:
                    issues.append(f"{cur}: unread line {s!r}")
                continue
            m = CLASS_OPEN.match(s)
            if m:
                name = m.group(1)
                classes.setdefault(name, {"name": name, "stereotypes": [], "attributes": [], "methods": []})
                if m.group(2):
                    cur = name
                continue
            st = STEREO_LINE.match(s)
            if st and st.group(2):
                classes.setdefault(st.group(2), {"name": st.group(2), "stereotypes": [], "attributes": [], "methods": []})
                classes[st.group(2)]["stereotypes"].append(st.group(1).lower())
                continue
            m = REL.match(s)
            if m:
                a, ma, arrow, mb, b, label = m.groups()
                for n in (a, b):
                    classes.setdefault(n, {"name": n, "stereotypes": [], "attributes": [], "methods": []})
                rels.append({"from": a, "to": b, "arrow": arrow, "from_mult": ma or "", "to_mult": mb or "",
                             "label": (label or "").strip()})
                continue
            if s.startswith(("note", "direction", "namespace", "style", "classDef", "cssClass", "click", "link")):
                continue
            issues.append(f"unread line {s!r}")
    valuesets = []
    for ln in text.splitlines():
        m = VALUESET.match(ln)
        if m and m.group(1) in classes:
            vals = re.findall(r"\*([^*]+)\*|`([^`]+)`", m.group(3))
            vals = [x or y for x, y in vals]
            valuesets.append({"class": m.group(1), "attribute": m.group(2), "values": vals, "text": m.group(3).strip()})
    for c in classes.values():
        if "enumeration" in c["stereotypes"]:
            valuesets.append({"class": c["name"], "attribute": "", "values": [a["name"] for a in c["attributes"]],
                              "text": "enumeration"})
        c["external"] = any("extern" in s or s in ("actor",) for s in c["stereotypes"])
    return {"file": path.name, "api_links": api_links, "classes": list(classes.values()), "relations": rels,
            "valuesets": valuesets, "issues": issues}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default="cdm")
    a = ap.parse_args()
    d = FACTORY / a.dir
    src = json.loads((d / "SOURCES.json").read_text(encoding="utf-8"))
    by_file = {r["file"]: r for r in src["cdms"] if "file" in r}
    cdms = []
    for f in sorted(d.glob("*.md")):
        if f.name in ("cdm-index.md", "README.md", "inventory.md") or f.name not in by_file:
            continue
        c = parse(f)
        r = by_file[f.name]
        c |= {"service": r["service"], "owner": r["owner"], "status": r["status"], "approved": r["approved"],
              "project": r["project"]}
        for cl in c["classes"]:
            cl["id"] = f"CDM:{f.stem}:{cl['name']}"
            for at in cl["attributes"]:
                at["id"] = f"{cl['id']}.{at['name']}"
        cdms.append(c)
    summary = {"cdms": len(cdms), "not_fetched": [r["service"] for r in src["cdms"] if "file" not in r],
               "classes": sum(len(c["classes"]) for c in cdms),
               "external_classes": sum(1 for c in cdms for cl in c["classes"] if cl["external"]),
               "attributes": sum(len(cl["attributes"]) for c in cdms for cl in c["classes"]),
               "relations": sum(len(c["relations"]) for c in cdms),
               "valuesets": sum(len(c["valuesets"]) for c in cdms),
               "issues": sum(len(c["issues"]) for c in cdms)}
    (d / "inventory.json").write_text(json.dumps({"summary": summary, "cdms": cdms}, indent=1, ensure_ascii=False) + "\n",
                                      encoding="utf-8")
    md = ["# CDM inventory", "", "Generated by [`pipeline/extract_cdm.py`](../pipeline/extract_cdm.py) from the frozen snapshot in this folder. Do not edit by hand.", "",
          f"{summary['cdms']} CDMs, {summary['classes']} classes ({summary['external_classes']} external), "
          f"{summary['attributes']} attributes, {summary['relations']} relations, {summary['valuesets']} value sets. "
          f"Not fetched: {', '.join(summary['not_fetched']) or 'none'}.", "",
          "| CDM | Service | Owner | Status | Approved | Classes | External | Attributes | Value sets | API link in the CDM | Parse issues |",
          "|---|---|---|---|---|---|---|---|---|---|---|"]
    for c in cdms:
        md.append(f"| [{c['file']}](./{c['file']}) | {c['service']} | {c['owner']} | {c['status']} | {c['approved']} | "
                  f"{len(c['classes'])} | {sum(cl['external'] for cl in c['classes'])} | "
                  f"{sum(len(cl['attributes']) for cl in c['classes'])} | {len(c['valuesets'])} | "
                  f"{'<br>'.join(c['api_links']) or 'none'} | {len(c['issues'])} |")
    (d / "inventory.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=1))
    for c in cdms:
        for i in c["issues"][:3]:
            print(f"  {c['file']}: {i}")


if __name__ == "__main__":
    main()
