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
KW = r"(?:[Gg]iven|[Ww]hen|[Tt]hen|[Aa]nd|[Bb]ut|[Oo]r|GIVEN|WHEN|THEN|AND|BUT|OR)"
# A clause starts with a keyword in bold (any case), or plain with a capital letter. A lowercase
# plain "when" or "then" is prose (a note), not a clause.
CLAUSE = re.compile(r"^(?:\*{1,2}`?(" + KW + r")`?\*{1,2}|`(" + KW + r")`|((?:Given|When|Then|And|But|Or|GIVEN|WHEN|THEN|AND|BUT|OR)))[:,]?\s+(.*)$")
GROUP = re.compile(r"^(?:#+\s*)?\**\s*((?:(?:positive|negative|happy|unhappy|error|edge|alternative|alternate)\s+)?"
                   r"(?:scenarios|cases|paths|flows)|(?:positive|negative|happy|unhappy|error|edge|alternative)\s+(?:case|path|flow))"
                   r"\s*:?\s*\**\s*:?$", re.I)
NUM_HEAD = re.compile(r"^(?:#{2,5}\s*|\*\*)\s*(\d+)[.)]\s+(.+?)\**:?$")
MD_HEAD = re.compile(r"^#{2,5}\s+(.+)$")
STOP = re.compile(r"(?i)^(implementation|technical|tech\b|scope|out of scope|in scope|definition of done|dependencies|"
                  r"assumptions|design|api details|endpoint|non-functional|nfr|test data|open questions|references)")
URL = re.compile(r"https?://\S+")
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
    """Drop a leading list marker ("- ", "* " or "1. ")."""
    return re.sub(r"^(?:[-*]|\d+\.)\s+", "", clean(line))


def struck(line):
    """Jira strikethrough comes through as -text-, also inside a list item."""
    s = unlist(line).strip("*").strip()
    return len(s) > 2 and s.startswith("-") and s.endswith("-")


def unstrike(line):
    """The clause text without list marker, strike dashes and emphasis around the keyword."""
    s = unlist(line).strip()
    if struck(line):
        s = s.strip("*").strip()[1:-1].strip()
    if struck(line):  # keyword emphasis inside a strike: *-*Given*-* and the like
        s = re.sub(r"^\*?-?\*?(Given|When|Then|And|But|Or)-?\*?\*?", lambda m: "**" + m.group(1) + "**", s, flags=re.I)
    return s


def clause(line):
    """(keyword, text) if the line is a Given/When/Then clause, else None."""
    m = CLAUSE.match(unstrike(line))
    if not m:
        return None
    kw = (m.group(1) or m.group(2) or m.group(3)).capitalize()
    return kw, m.group(4)


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
        m = re.match(r"^(?:\*\*)?(As an?|I want|So that)(?:\*\*)?[,:]?\s+(.*)", clean(ln), re.I) or \
            re.match(r"^\|+\s*(As an?|I want|So that)\s*\|\s*([^|]+?)\s*\|", clean(ln), re.I)
        if m:
            user_story.setdefault(m.group(1).lower().replace("as an", "as a"), m.group(2).strip())

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
    in_list = False   # the last clause ends with ":" and a list follows
    pending = ""      # a plain heading or table title waiting for the scenario under it
    stopped = False   # inside a non-criteria section (implementation scope, technical notes)
    # Clauses before the "Acceptance Criteria" marker belong to the description, if the marker exists.
    marker = next((i for i, ln in enumerate(body)
                   if re.match(r"^#*\s*\**Acceptance Criteria\**:?\\?$", clean(ln), re.I)), None)
    for idx, raw in enumerate(body):
        ln = clean(raw)
        if not ln or ln.startswith("## Description"):
            continue
        if re.match(r"^#*\s*\**Acceptance Criteria\**:?$", ln, re.I):
            stopped = False
            continue
        if marker is not None and idx < marker:
            continue
        if GROUP.match(ln) and not clause(ln):
            close()
            group, last_kw, stopped = ln.strip("*#:- ").strip(), None, False
            continue
        nh = NUM_HEAD.match(ln)
        if nh and not clause(nh.group(2)):
            new_ac(f"AC{nh.group(1)}", True, heading=ln.strip("*#- ").strip())
            last_kw, stopped = None, False
            continue
        h = AC_HEAD.match(ln)
        if h and not clause(ln) and re.search(r"scenarios\b", h.group("label"), re.I):
            close()
            group, last_kw, stopped = ln.strip("*#:- ").strip(), None, False
            continue
        if h and not clause(ln):
            num = h.group("num") or h.group("snum")
            label = f"AC{num}" if num else None
            new_ac(label, bool(num), strike=bool(h.group("strike")) or struck(ln),
                   heading=ln.strip("*#- ").strip())
            last_kw, stopped = None, False
            continue
        mh = MD_HEAD.match(ln) or re.match(r"^\*\*([^*]{3,80})\*\*:?$", ln)
        if mh and not clause(ln):
            title = mh.group(1).strip("*: ")
            if STOP.match(title):
                close()
                stopped, pending = True, ""
            else:
                pending = title
            continue
        if ln.startswith("|") and re.search(r"(?i)\b(scenario|AC\s*\d)", ln):
            pending = ln.strip("| ").split("|")[0].strip()
            continue
        if stopped:
            continue
        is_struck = struck(ln)
        c = clause(ln)
        if c:
            kw, ctext = c
            # An "And" or "Then" before any scenario continues the user story ("So that ... And ..."):
            # not an acceptance criterion.
            if cur is None and kw not in ("Given", "When"):
                user_story.setdefault("extra", []).append(ctext.strip())
                continue
            # A Given after a Then (or the first clause) starts a new, unlabelled scenario.
            if cur is None or (kw == "Given" and last_kw in ("Then", "And", "But", "Or") and
                               any(x["kw"] == "Then" for x in cur["clauses"])) or (
                    cur["clauses"] and pending and kw == "Given"):
                new_ac(None, False, strike=is_struck, heading=pending)
            elif not cur["heading"] and pending:
                cur["heading"] = pending
            pending = ""
            cur["clauses"].append({"kw": kw, "text": ctext.strip().rstrip("-").strip(), "struck": is_struck})
            last_kw = kw
            in_list = ctext.rstrip().endswith(":")
            continue
        if cur is not None and cur["clauses"]:
            if re.match(r"^(?:[-*]|\d+\.)\s+", ln) and in_list:
                # A list that finishes a clause ("the following fields are available:") belongs to it.
                cur["clauses"][-1].setdefault("items", []).append(unlist(ln))
            else:
                cur["other_text"].append(ln)
                in_list = False
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

    plain_text = URL.sub(" ", text.replace("\\{", "{").replace("\\}", "}"))
    plain_text = re.sub(r"<([A-Za-z_]\w*)>", r"{\1}", plain_text)
    refs = {
        "stories": sorted(set(STORY_KEY.findall(text)) - {key}),
        "paths": sorted(set(p for p in PATH_REF.findall(plain_text) if len(p) > 3 and not p.startswith("//"))),
        "method_paths": sorted(set(f"{m.upper()} {p}" for m, p in re.findall(
            r"\b(GET|POST|PUT|PATCH|DELETE)\s+(/[\w{}\-/]+)", plain_text, re.I))),
        "bp_steps": sorted(set(re.findall(r"\bBP\s?\d+[A-Z]?(?:\.\d+)*\b", text))),
        "api_method_paths": sorted(set(f"{a} {m.upper()} {p}" for a, m, p in re.findall(
            r"\b([a-z][a-z0-9-]+-be)\b\s*:?\s*\b(GET|POST|PUT|PATCH|DELETE)\s+(/[\w{}\-/]+)", plain_text))),
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
    """A business process: a folder of sub-process files, or one file (folder may be a file)."""
    reqs, procs, issues = [], [], []
    files = [folder] if folder.is_file() else sorted(folder.glob("*.md"))
    for f in files:
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
        # Single-file processes: steps "### BP02B.01 - Create new vocabulary", requirements in the
        # "High-level requirements" table "| 2B.1 | title | SIMPL-15931 | link |" and their descriptions.
        for ln in text.splitlines():
            st = re.match(r"^#{2,4}\s*(BP\d+[A-Z]?\.\d+)\s*[-–—:]?\s*(.+)$", ln)
            if st:
                reqs.append({"id": st.group(1), "process": pid, "kind": "step", "title": st.group(2).strip(),
                             "text": "", "details_url": "", "file": str(f.relative_to(FACTORY))})
            tr = re.match(r"^\|\s*(\d+[A-Z]?\.\d+(?:\.\d+)*)\s*\|\s*([^|]+?)\s*\|\s*([^|]*?)\s*\|\s*([^|]*)\|", ln)
            if tr and not any(q["id"] == f"BP{tr.group(1)}" for q in reqs):
                link = re.search(r"\]\(([^)]*)\)", tr.group(4))
                reqs.append({"id": f"BP{tr.group(1)}", "process": pid, "kind": "requirement",
                             "title": tr.group(2).strip().rstrip("."), "text": "",
                             "issue": tr.group(3).strip(), "details_url": link.group(1) if link else "",
                             "file": str(f.relative_to(FACTORY))})
            dl = re.match(r"^\s*[-*]\s+(\d+[A-Z]?\.\d+\.\d+)\s*[-–—]\s*(.+)$", ln)
            if dl and not any(q["id"] == f"BP{dl.group(1)}" for q in reqs):
                reqs.append({"id": f"BP{dl.group(1)}", "process": pid, "kind": "detailed requirement",
                             "title": dl.group(2).strip(), "text": "", "details_url": "",
                             "file": str(f.relative_to(FACTORY))})
        # Requirement descriptions: "**2B.1 - title** (SIMPL-n)" followed by the paragraph.
        lines = text.splitlines()
        for i, ln in enumerate(lines):
            d = re.match(r"^\*\*(\d+[A-Z]?\.\d+(?:\.\d+)*)\s*[-–—]\s*(.+?)\*\*", ln)
            if d:
                q = next((q for q in reqs if q["id"] == f"BP{d.group(1)}"), None)
                para = next((x.strip() for x in lines[i + 1:i + 4] if x.strip()), "")
                if q is not None and not q["text"]:
                    q["text"] = para
        # Text of each step, from its heading to the next heading.
        for q in reqs:
            if q.get("kind") == "step" and q["file"] == str(f.relative_to(FACTORY)):
                i = next(i for i, ln in enumerate(lines) if re.match(r"^#{2,4}\s*" + re.escape(q["id"]), ln))
                body = []
                for ln in lines[i + 1:]:
                    if ln.startswith("#"):
                        break
                    body.append(ln.strip())
                q["text"] = " ".join(x for x in body if x)[:600]
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
    args = [a for a in sys.argv[1:] if not a.startswith("--out=")]
    outs = [a[6:] for a in sys.argv[1:] if a.startswith("--out=")]
    if len(args) != 1:
        sys.exit(__doc__)
    comp = FACTORY / args[0]
    specs = comp / "specs"
    if not specs.is_dir():
        sys.exit(f"no specs folder: {specs}")
    out = Path(outs[0]) if outs else comp / "inventory"
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
        p, r, iss = parse_bp(f)
        procs += p
        reqs += r
        issues += iss

    # Mechanical cross-references only: what the texts literally name.
    op_by_path = {}
    for a in apis:
        for o in a["operations"]:
            norm = re.sub(r"\{[^}]+\}", "{}", o["path"])
            op_by_path.setdefault(norm, []).append(o["id"])
    api_names = {a["name"] for a in apis}
    for s in stories:
        hits, notes = set(), []
        # "catalogue-client-be GET /x": that API only.
        scoped = {}
        for amp in s["refs"].get("api_method_paths", []):
            api, m, p = amp.split(" ", 2)
            scoped[re.sub(r"\{[^}]+\}", "{}", p.rstrip("/"))] = (api, m)
        named = {}
        for mp in s["refs"]["method_paths"]:
            m, p = mp.split(" ", 1)
            named.setdefault(re.sub(r"\{[^}]+\}", "{}", p.rstrip("/")), set()).add(m)
        for p in s["refs"]["paths"]:
            norm = re.sub(r"\{[^}]+\}", "{}", p.rstrip("/"))
            ops = op_by_path.get(norm, [])
            if norm in scoped:
                api, m = scoped[norm]
                same = [o for o in ops if o == f"{api}:{m} {o.split(' ', 1)[1]}" and o.startswith(api + ":")]
                if not same:
                    where = f"{api} entry files" if api in api_names else f"this input set ({api} is not an API here)"
                    notes.append(f"names {api} {m} {p}, which is not in the {where}")
                hits.update(same)
                continue
            if norm in named:  # the story names the method too: only those count
                same = [o for o in ops if o.split(":", 1)[1].split(" ", 1)[0] in named[norm]]
                for m in sorted(named[norm]):
                    if not [o for o in same if o.split(":", 1)[1].startswith(m + " ")]:
                        notes.append(f"names {m} {p}, " + ("but the OpenAPI has that path with another method"
                                                          if ops else "which is in no API entry file"))
                ops = same
            elif len(ops) > 1:
                notes.append(f"names {p} without a method; {len(ops)} operations have that path, all linked")
            hits.update(ops)
        s["refs"]["operations"] = sorted(hits)
        for n in notes:
            issues.append(f"{s['key']}: {n}")

    live_acs = [a for s in stories for a in s["acs"] if not a["struck"]]
    summary = {
        "component": args[0],
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
