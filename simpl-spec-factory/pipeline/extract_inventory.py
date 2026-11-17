#!/usr/bin/env python3
"""Step 12a: extract the spec inventory of one component, mechanically.

Reads <component>/specs/ (stories, OpenAPI files, business processes) and writes
<component>/inventory/inventory.json and inventory.md. Every story, acceptance
criterion, API operation and business process requirement gets a stable id, with
its text word for word. Nothing is judged here: anything the parser cannot place
goes to the extraction issues, for step 12b (an agent) to review.

Run: python3 pipeline/extract_inventory.py contract-management
"""
import json
import re
import sys
from pathlib import Path

import yaml

FACTORY = Path(__file__).resolve().parent.parent
METHODS = ("get", "put", "post", "patch", "delete", "head", "options")
CLAUSE = re.compile(r"^\*{0,2}(Given|When|Then|And|But)\*{0,2}\s+(.*)$", re.I)
# An AC or scenario heading: "AC1 - ...", "### AC2", "**AC3: ...**", "Scenario 2 - ...",
# "**Positive Scenario 1:**", "Negative Scenarios:".
AC_HEAD = re.compile(
    r"^(?:#+\s*)?(?:[-*]\s+)?\*{0,2}\s*(?P<strike>-)?\*{0,2}\s*"
    r"(?P<label>(?:AC|Acceptance Criterion)\s*(?P<num>\d+)|(?:(?:Positive|Negative)\s+)?Scenarios?(?:\s*(?P<snum>\d+))?)"
    r"\s*(?P<rest>.*)$",
    re.I,
)
STORY_KEY = re.compile(r"\bSIMPL-\d+\b")
PATH_REF = re.compile(r"(?<![\w/])/[a-zA-Z][\w{}\-]*(?:/[\w{}\-]+)*")


def clean(line):
    """Strip the markdown hard break and surrounding blanks."""
    return line.rstrip().rstrip("\\").strip()


def unlist(line):
    """Drop a leading list marker ("- " or "* ")."""
    return re.sub(r"^[-*]\s+", "", clean(line))


def struck(line):
    """Jira strikethrough comes through as -text-, also inside a list item."""
    s = unlist(line).strip("*").strip()
    return len(s) > 2 and s.startswith("-") and s.endswith("-")


def unstrike(line):
    """The clause text without list marker, strike dashes and emphasis around the keyword."""
    s = unlist(line).strip()
    if struck(line):
        s = s.strip("*").strip()[1:-1].strip()
    s = re.sub(r"^\*?-?\*?(Given|When|Then|And|But)-?\*?\*?", lambda m: "**" + m.group(1) + "**", s, flags=re.I)
    return s


def parse_story(path):
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    head = re.match(r"#\s*(SIMPL-\d+):\s*(.*)", lines[0])
    key, title = head.group(1), head.group(2).strip()
    meta = {}
    for ln in lines[1:8]:
        m = re.match(r"-\s*(Status|Type|Created|Source):\s*(.*)", ln)
        if m:
            meta[m.group(1).lower()] = m.group(2).strip()
    kind = "API" if re.search(r"\[API\]|via API|\bAPI\b", title) else "UI" if re.search(r"\[UI\]|\bUI\b", title) else ""
    replaced = bool(re.search(r"\[Replaced\]", title, re.I))

    body_start = next((i for i, ln in enumerate(lines) if ln.startswith("## ")), len(lines))
    body = lines[body_start:]

    user_story = {}
    for ln in body:
        m = re.match(r"^\*\*(As an?|I want|So that)\*\*\s*(.*)", clean(ln), re.I)
        if m:
            user_story[m.group(1).lower().replace("as an", "as a")] = m.group(2).strip()

    acs, issues = [], []
    cur = None
    seq = 0
    group = ""

    def close():
        nonlocal cur
        if cur is not None:
            acs.append(cur)
        cur = None

    def new_ac(label, explicit, strike=False, heading=""):
        nonlocal cur, seq, group
        close()
        seq += 1
        cur = {"label": label, "explicit": explicit, "struck": strike, "heading": heading,
               "group": group, "clauses": [], "other_text": []}

    last_kw = None
    for raw in body:
        ln = clean(raw)
        if not ln or ln.startswith("## Description") or re.match(r"^#*\s*\**Acceptance Criteria\**:?$", ln, re.I):
            continue
        h = AC_HEAD.match(ln)
        if h and not CLAUSE.match(unstrike(ln)) and re.search(r"scenarios\b", h.group("label"), re.I):
            # "Negative Scenarios:" groups the scenarios that follow; it is not one itself.
            close()
            group = ln.strip("*#:- ").strip()
            last_kw = None
            continue
        if h and not CLAUSE.match(unstrike(ln)):
            num = h.group("num") or h.group("snum")
            label = f"AC{num}" if num else None
            new_ac(label, bool(num), strike=bool(h.group("strike")) or struck(ln),
                   heading=ln.strip("*#- ").strip())
            last_kw = None
            continue
        is_struck = struck(ln)
        c = CLAUSE.match(unstrike(ln))
        if c:
            kw = c.group(1).capitalize()
            # An "And" or "Then" before any scenario continues the user story ("So that ... And ..."):
            # not an acceptance criterion.
            if cur is None and kw not in ("Given", "When"):
                user_story.setdefault("extra", []).append(c.group(2).strip())
                continue
            # A Given after a Then (or the first clause) starts a new, unlabelled scenario.
            if cur is None or (kw == "Given" and last_kw in ("Then", "And", "But") and
                               any(x["kw"] == "Then" for x in cur["clauses"])):
                new_ac(None, False, strike=is_struck)
            cur["clauses"].append({"kw": kw, "text": c.group(2).strip().rstrip("-").strip(), "struck": is_struck})
            last_kw = kw
            continue
        if cur is not None and cur["clauses"]:
            cur["other_text"].append(ln)
    close()

    # A heading with no clauses, followed by another heading, labels a group ("Positive Scenarios").
    kept = []
    for i, a in enumerate(acs):
        if not a["clauses"] and not a["label"] and i + 1 < len(acs):
            for b in acs[i + 1:]:
                if b["group"] == a["group"]:
                    b["group"] = a["heading"]
            continue
        kept.append(a)
    acs = kept

    # Ids: explicit numbers win; unlabelled scenarios get the next free number, marked derived.
    used = [int(a["label"][2:]) for a in acs if a["label"]]
    nxt = 1
    for a in acs:
        if not a["label"]:
            while nxt in used:
                nxt += 1
            a["label"] = f"AC{nxt}"
            used.append(nxt)
        a["id"] = f"{key}/{a['label']}"
        if all(c["struck"] for c in a["clauses"]) and a["clauses"]:
            a["struck"] = True
    seen = {}
    for a in acs:
        seen.setdefault(a["label"], 0)
        seen[a["label"]] += 1
    for lab, n in seen.items():
        if n > 1:
            issues.append(f"{key}: label {lab} used {n} times; ids suffixed")
    counts = {}
    for a in acs:
        if seen[a["label"]] > 1:
            counts[a["label"]] = counts.get(a["label"], 0) + 1
            a["id"] = f"{key}/{a['label']}.{counts[a['label']]}"

    live = [a for a in acs if not a["struck"]]
    if not acs:
        issues.append(f"{key}: no acceptance criteria found")
    for a in live:
        kws = {c["kw"] for c in a["clauses"]}
        if not a["clauses"]:
            issues.append(f"{a['id']}: heading without Given/When/Then clauses")
        else:
            for need in ("Given", "When", "Then"):
                if need not in kws:
                    issues.append(f"{a['id']}: no {need} clause")
        if any(c["struck"] for c in a["clauses"]):
            issues.append(f"{a['id']}: some clauses are struck through")
    for a in acs:
        if a["struck"]:
            issues.append(f"{a['id']}: struck through in Jira, left out of the live count")

    plain_text = text.replace("\\{", "{").replace("\\}", "}")
    refs = {
        "stories": sorted(set(STORY_KEY.findall(text)) - {key}),
        "paths": sorted(set(p for p in PATH_REF.findall(plain_text) if len(p) > 3 and not p.startswith("//"))),
        "method_paths": sorted(set(f"{m.upper()} {p}" for m, p in re.findall(
            r"\b(GET|POST|PUT|PATCH|DELETE)\s+(/[\w{}\-/]+)", plain_text, re.I))),
        "bp_steps": sorted(set(re.findall(r"\bBP\s?\d+[A-Z]?(?:\.\d+)*\b", text))),
    }
    return {
        "key": key, "title": title, "kind": kind, "replaced": replaced,
        "status": meta.get("status", ""), "type": meta.get("type", ""),
        "source": meta.get("source", ""), "file": str(path.relative_to(FACTORY)),
        "user_story": user_story, "acs": acs, "refs": refs,
    }, issues


def parse_openapi(path, api_name):
    doc = yaml.safe_load(path.read_text(encoding="utf-8"))
    ops, issues = [], []
    info = doc.get("info", {}) if isinstance(doc, dict) else {}
    global_sec = doc.get("security") if isinstance(doc, dict) else None
    for p, item in (doc.get("paths") or {}).items():
        for m in METHODS:
            op = (item or {}).get(m)
            if not op:
                continue
            oid = op.get("operationId")
            body = op.get("requestBody") or {}
            content = list((body.get("content") or {}).keys())
            ops.append({
                "id": f"{api_name}:{m.upper()} {p}",
                "api": api_name, "method": m.upper(), "path": p,
                "operationId": oid or "", "summary": (op.get("summary") or "").strip(),
                "description": (op.get("description") or "").strip(),
                "tags": op.get("tags", []),
                "parameters": [
                    {"name": x.get("name"), "in": x.get("in"), "required": x.get("required", False)}
                    for x in (op.get("parameters", []) + (item.get("parameters") or [])) if isinstance(x, dict) and "name" in x
                ],
                "request_content": content,
                "responses": sorted(str(k) for k in (op.get("responses") or {}).keys()),
                "security": op.get("security", global_sec),
                "file": str(path.relative_to(FACTORY)),
            })
            if not oid:
                issues.append(f"{api_name}:{m.upper()} {p}: no operationId")
            if not op.get("responses"):
                issues.append(f"{api_name}:{m.upper()} {p}: no responses")
    return {"name": api_name, "title": info.get("title", ""), "version": info.get("version", ""),
            "file": str(path.relative_to(FACTORY)), "operations": ops}, issues


def parse_bp(folder):
    reqs, procs, issues = [], [], []
    for f in sorted(folder.glob("*.md")):
        text = f.read_text(encoding="utf-8")
        title = next((ln[2:].strip() for ln in text.splitlines() if ln.startswith("# ")), f.stem)
        m = re.match(r"(BP\d+\w*|\d+[A-Z]?(?:\.\d+)*)\s*[-–—:]\s*(.*)", title)
        pid = m.group(1) if m else f.stem
        procs.append({"id": pid, "title": title, "file": str(f.relative_to(FACTORY)),
                      "story_refs": sorted(set(STORY_KEY.findall(text)))})
        for ln in text.splitlines():
            r = re.match(r"^\s*[-*]\s*\*\*(\d+[A-Z]?(?:\.\d+)+)\s*[-–—]\s*(.*?)\*\*\s*[-–—]?\s*(.*)$", ln)
            if r:
                desc = re.sub(r"\s*\[Details\]\([^)]*\)", "", r.group(3)).strip()
                link = re.search(r"\[Details\]\(([^)]*)\)", r.group(3))
                reqs.append({"id": f"BP{r.group(1)}", "process": pid, "title": r.group(2).strip(),
                             "text": desc, "details_url": link.group(1) if link else "",
                             "file": str(f.relative_to(FACTORY))})
        mine = [q for q in reqs if q["file"] == str(f.relative_to(FACTORY))]
        if f.name != "README.md" and "dynamic-view" not in f.name and not mine:
            issues.append(f"{f.relative_to(FACTORY)}: no detailed requirements found")
        # Numbering gaps (5B.1.1, 5B.1.3: where is 5B.1.2?).
        last = {}
        for q in mine:
            base, _, n = q["id"][2:].rpartition(".")
            if n.isdigit():
                if base in last and int(n) != last[base] + 1:
                    issues.append(f"{q['id']}: numbering gap after {base}.{last[base]}")
                last[base] = int(n)
    return procs, reqs, issues


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    comp = FACTORY / sys.argv[1]
    specs = comp / "specs"
    if not specs.is_dir():
        sys.exit(f"no specs folder: {specs}")
    out = comp / "inventory"
    out.mkdir(exist_ok=True)

    issues = []
    stories = []
    for f in sorted((specs / "stories").glob("SIMPL-*.md"), key=lambda p: int(p.stem.split("-")[1])):
        s, iss = parse_story(f)
        stories.append(s)
        issues += iss

    entries = None
    mf = comp / "sources.yaml"
    if mf.exists():
        man = yaml.safe_load(mf.read_text(encoding="utf-8"))
        entries = {}
        for a in man.get("api", []):
            if a.get("entry"):
                entries[a["name"]] = set(a["entry"])
    apis = []
    for f in sorted((specs / "api").rglob("*.y*ml")):
        rel_api = f.relative_to(specs / "api")
        if entries and rel_api.parts[0] in entries and "/".join(rel_api.parts[1:]) not in entries[rel_api.parts[0]]:
            continue  # references, -itb copies or older versions: not where the operations are counted
        name = rel_api.parts[0] if len(rel_api.parts) > 1 else f.stem
        a, iss = parse_openapi(f, name)
        apis.append(a)
        issues += iss

    procs, reqs = [], []
    func = specs / "functional"
    for d in sorted(p for p in func.iterdir() if p.is_dir()) if func.is_dir() else []:
        p, r, iss = parse_bp(d)
        procs += p
        reqs += r
        issues += iss
    for f in sorted(func.glob("*.md")) if func.is_dir() else []:
        issues.append(f"{f.relative_to(FACTORY)}: single-file process, not parsed into requirements")

    # Mechanical cross-references only: what the texts literally name.
    op_by_path = {}
    for a in apis:
        for o in a["operations"]:
            norm = re.sub(r"\{[^}]+\}", "{}", o["path"])
            op_by_path.setdefault(norm, []).append(o["id"])
    for s in stories:
        hits, mismatch = set(), []
        named = {re.sub(r"\{[^}]+\}", "{}", mp.split(" ", 1)[1]): mp.split(" ", 1)[0] for mp in s["refs"]["method_paths"]}
        for p in s["refs"]["paths"]:
            norm = re.sub(r"\{[^}]+\}", "{}", p.rstrip("/"))
            ops = op_by_path.get(norm, [])
            if norm in named:  # the story names the method too: only that one counts
                same = [o for o in ops if o.split(":", 1)[1].startswith(named[norm] + " ")]
                if ops and not same:
                    mismatch.append(f"{named[norm]} {p}")
                ops = same
            hits.update(ops)
        s["refs"]["operations"] = sorted(hits)
        for m in mismatch:
            issues.append(f"{s['key']}: names {m}, but the OpenAPI has that path with another method")

    live_acs = [a for s in stories for a in s["acs"] if not a["struck"]]
    summary = {
        "component": sys.argv[1],
        "stories": len(stories),
        "stories_replaced": sum(s["replaced"] for s in stories),
        "stories_without_ac": sum(1 for s in stories if not [a for a in s["acs"] if not a["struck"]]),
        "acs_live": len(live_acs),
        "acs_struck": sum(a["struck"] for s in stories for a in s["acs"]),
        "acs_label_derived": sum(1 for a in live_acs if not a["explicit"]),
        "acs_incomplete": sum(1 for a in live_acs if not {"Given", "When", "Then"} <= {c["kw"] for c in a["clauses"]}),
        "apis": len(apis),
        "operations": sum(len(a["operations"]) for a in apis),
        "processes": len(procs),
        "bp_requirements": len(reqs),
        "stories_naming_an_operation": sum(1 for s in stories if s["refs"]["operations"]),
        "processes_naming_a_story": sum(1 for p in procs if p["story_refs"]),
        "extraction_issues": len(issues),
    }
    inv = {"summary": summary, "stories": stories, "apis": apis, "processes": procs,
           "bp_requirements": reqs, "extraction_issues": issues}
    (out / "inventory.json").write_text(json.dumps(inv, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    (out / "inventory.md").write_text(render(inv), encoding="utf-8")
    print(json.dumps(summary, indent=1))


def render(inv):
    s = inv["summary"]
    o = [f"# Spec inventory: {s['component']}", "",
         "Generated by [`pipeline/extract_inventory.py`](../../pipeline/extract_inventory.py) (step 12a) from `specs/`. "
         "Do not edit by hand: rerun the script. Step 12b reviews it in [`inventory-review.md`](./inventory-review.md).", "",
         "## Totals", "", "| Item | Count |", "|---|---|"]
    for k, v in s.items():
        if k != "component":
            o.append(f"| {k.replace('_', ' ')} | {v} |")
    o += ["", "## Stories and acceptance criteria", "",
          "Ids: `<story>/AC<n>`. *derived* means the story gives no number and the parser counted the scenarios in order.", "",
          "| Story | Kind | Status | ACs (live) | Derived ids | Operations named |", "|---|---|---|---|---|---|"]
    for st in inv["stories"]:
        live = [a for a in st["acs"] if not a["struck"]]
        der = sum(1 for a in live if not a["explicit"])
        flag = " (replaced)" if st["replaced"] else ""
        o.append(f"| [{st['key']}](../specs/stories/{st['key']}.md){flag} | {st['kind']} | {st['status']} | {len(live)} | {der or ''} | "
                 f"{', '.join('`'+x+'`' for x in st['refs']['operations'])} |")
    o += ["", "### Acceptance criteria", ""]
    for st in inv["stories"]:
        for a in st["acs"]:
            if a["struck"]:
                continue
            first = {c["kw"]: c["text"] for c in reversed(a["clauses"])}
            o.append(f"- `{a['id']}`{'' if a['explicit'] else ' (derived)'}: "
                     f"{a['heading'] or ('When ' + first.get('When', '?') + ', then ' + first.get('Then', '?'))}")
    o += ["", "## API operations", "", "| Id | operationId | Responses |", "|---|---|---|"]
    for a in inv["apis"]:
        for op in a["operations"]:
            o.append(f"| `{op['id']}` | {op['operationId']} | {', '.join(op['responses'])} |")
    o += ["", "## Business process requirements", "", "| Id | Process | Title |", "|---|---|---|"]
    for r in inv["bp_requirements"]:
        o.append(f"| `{r['id']}` | {r['process']} | {r['title']} |")
    o += ["", "## Extraction issues", "", "What the parser could not place or found incomplete. Step 12b checks each one.", ""]
    o += [f"- {i}" for i in inv["extraction_issues"]] or ["None."]
    return "\n".join(o) + "\n"


if __name__ == "__main__":
    main()
