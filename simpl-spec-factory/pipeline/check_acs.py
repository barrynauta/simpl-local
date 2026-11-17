#!/usr/bin/env python3
"""Step 20a: mechanical checks on the acceptance criteria of one component.

Reads <component>/inventory/inventory.json (step 12) and writes
<component>/validation/ac-checks.json and ac-checks.md: one finding per check that
fails, per AC id. Only what a script can decide reliably: structure and wording.
Completeness, testability, consistency and correctness are for step 20b (an agent).

The checks and their codes are listed in pipeline/20-validate-acs.md.

Run: python3 pipeline/check_acs.py contract-management
"""
import difflib
import json
import re
import sys
from collections import Counter
from pathlib import Path

FACTORY = Path(__file__).resolve().parent.parent

VAGUE = [
    "appropriate", "appropriately", "properly", "correctly", "correct", "relevant", "suitable",
    "user-friendly", "user friendly", "quickly", "fast", "efficient", "efficiently", "securely",
    "robust", "adequate", "adequately", "as needed", "if necessary", "if needed", "etc", "and so on",
    "some", "several", "various", "sufficient", "reasonable", "intuitive", "seamless", "seamlessly",
    "easily", "timely", "normal", "normally", "gracefully", "clear", "clearly",
    "meaningful", "proper", "accordingly", "acceptable", "minimal", "optimal",
]
PLACEHOLDER = [r"\bTBD\b", r"\bTBC\b", r"\bTODO\b", r"\bXXX\b", r"to be (?:defined|confirmed|decided|clarified)",
               r"\?\s*$", r"\bto check\b"]
OPTIONAL = [r"\band/or\b", r"\bmay\b", r"\bmight\b", r"\bcould\b(?! not)", r"\bpossibly\b", r"\boptionally\b",
            r"\beither\b", r"\bshould\b"]
ACTION = re.compile(r"(?i)\b(click|clicks|select|selects|submit|submits|send|sends|call|calls|upload|uploads|enter|enters|"
                    r"navigate|navigates|open|opens|press|presses|choose|chooses|request|requests|create|creates|"
                    r"delete|deletes|update|updates|invoke|invokes|fill|fills|confirm|confirms|provide|provides|"
                    r"type|types|drag|search|searches|filter|filters|download|downloads|sign|signs|accept|accepts|reject|rejects)\b")
INFO = {"AC-S09", "AC-S10"}  # reported for completeness, not a pointer for 20b
LIMIT_WORDS = r"\b(limit|maximum|minimum|max|min|timeout|time-out|within|retention|retry|retries|threshold|size)\b"


def words_hit(text, patterns, whole=True):
    hits = []
    for p in patterns:
        rx = p if p.startswith(("\\b", "(", "\\")) or "\\" in p else (r"\b" + re.escape(p) + r"\b" if whole else re.escape(p))
        if re.search(rx, text, re.I):
            hits.append(re.sub(r"\\b|\\s\*\$|\\", "", p).replace("(?:", "(").strip())
    return hits


def check_story(st):
    out = []
    live = [a for a in st["acs"] if not a["struck"]]

    def f(target, code, detail, evidence=""):
        out.append({"id": target, "check": code, "detail": detail, "evidence": evidence})

    if not live:
        f(st["key"], "AC-S08", "the story has no acceptance criteria")
    if st.get("replaced"):
        f(st["key"], "AC-S11", "the story is marked [Replaced] but is still live")
    labels = Counter(a["label"] for a in live)
    for lab, n in labels.items():
        if n > 1:
            f(st["key"], "AC-S07", f"label {lab} is used {n} times")
    for a in live:
        kws = [c["kw"] for c in a["clauses"]]
        for need, code in (("Given", "AC-S01"), ("When", "AC-S02"), ("Then", "AC-S03")):
            if need not in kws:
                f(a["id"], code, f"no {need} clause")
        # Several actions: an And between When and Then.
        if "When" in kws and "Then" in kws:
            wi, ti = kws.index("When"), kws.index("Then")
            # Only a second action counts; "And the file is valid" is a condition, not an action.
            extra = [c["text"] for c in a["clauses"][wi + 1:ti] if c["kw"] in ("And", "But") and ACTION.search(c["text"])]
            if extra:
                f(a["id"], "AC-S04", f"{1 + len(extra)} actions in the When part", extra[0])
            if kws.count("When") > 1:
                f(a["id"], "AC-S04", f"{kws.count('When')} When clauses")
            outcomes = len([k for k in kws[ti:] if k in ("Then", "And", "But")])
            if outcomes > 5:
                f(a["id"], "AC-S05", f"{outcomes} outcomes in the Then part")
        if any(c["struck"] for c in a["clauses"]):
            f(a["id"], "AC-S06", "some clauses are struck through")
        if not a["explicit"]:
            f(a["id"], "AC-S09", "no number in the story; the id is derived from the order")
        if a.get("other_text"):
            f(a["id"], "AC-S10", f"{len(a['other_text'])} lines of text outside the clauses (notes, tables, lists)",
              a["other_text"][0][:120])
        for c in a["clauses"]:
            t = c["text"]
            v = words_hit(t, VAGUE)
            if v:
                f(a["id"], "AC-W01", "vague word: " + ", ".join(v), f"{c['kw']} {t}")
            p = words_hit(t, PLACEHOLDER)
            if p:
                f(a["id"], "AC-W02", "open placeholder", f"{c['kw']} {t}")
            o = words_hit(t, OPTIONAL)
            if o and c["kw"] != "Given":
                f(a["id"], "AC-W03", "optional or open wording: " + ", ".join(o), f"{c['kw']} {t}")
            if re.search(LIMIT_WORDS, t, re.I) and not re.search(r"\d", t):
                f(a["id"], "AC-W05", "a limit or time without a value", f"{c['kw']} {t}")
    # Near-duplicate criteria within the story.
    texts = [(a["id"], " ".join(c["text"] for c in a["clauses"]).lower()) for a in live]
    for i in range(len(texts)):
        for j in range(i + 1, len(texts)):
            if texts[i][1] and difflib.SequenceMatcher(None, texts[i][1], texts[j][1]).ratio() > 0.97:
                f(texts[j][0], "AC-S12", f"nearly the same text as {texts[i][0]}")
    return out


DESCR = {
    "AC-S01": "no Given", "AC-S02": "no When", "AC-S03": "no Then",
    "AC-S04": "more than one action in When", "AC-S05": "more than 5 outcomes in Then",
    "AC-S06": "struck clauses in a live AC", "AC-S07": "AC number used twice", "AC-S08": "story without ACs",
    "AC-S09": "AC without a number", "AC-S10": "text outside the clauses", "AC-S11": "replaced story still live",
    "AC-S12": "near-duplicate AC", "AC-W01": "vague word", "AC-W02": "open placeholder",
    "AC-W03": "optional or open wording", "AC-W05": "limit or time without a value",
}


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    comp = FACTORY / sys.argv[1]
    inv = json.loads((comp / "inventory/inventory.json").read_text(encoding="utf-8"))
    findings = [x for st in inv["stories"] for x in check_story(st)]
    for x in findings:
        x["level"] = "info" if x["check"] in INFO else "pointer"
    live = [a for st in inv["stories"] for a in st["acs"] if not a["struck"]]
    by_code = Counter(x["check"] for x in findings)
    acs_hit = {x["id"] for x in findings if "/" in x["id"] and x["level"] == "pointer"}
    summary = {"component": sys.argv[1], "stories": len(inv["stories"]), "acs_live": len(live),
               "acs_with_a_finding": len(acs_hit), "findings": sum(x["level"] == "pointer" for x in findings),
               "info": sum(x["level"] == "info" for x in findings),
               "by_check": dict(sorted(by_code.items()))}
    out = comp / "validation"
    out.mkdir(exist_ok=True)
    (out / "ac-checks.json").write_text(json.dumps({"summary": summary, "findings": findings}, indent=1,
                                                   ensure_ascii=False) + "\n", encoding="utf-8")
    md = [f"# AC checks (step 20a): {sys.argv[1]}", "",
          "Generated by [`pipeline/check_acs.py`](../../pipeline/check_acs.py) from the step 12 inventory. "
          "Mechanical checks only: structure and wording. A hit is a pointer for step 20b, not a verdict: "
          "a vague word can be harmless in context.", "",
          f"{len(live)} live ACs in {len(inv['stories'])} stories; {len(acs_hit)} ACs with at least one finding; "
          f"{summary['findings']} pointers and {summary['info']} info lines (AC-S09, AC-S10: reported, not pointers).", "",
          "| Check | What | Level | Hits |", "|---|---|---|---|"]
    md += [f"| {k} | {DESCR[k]} | {'info' if k in INFO else 'pointer'} | {v} |" for k, v in sorted(by_code.items())]
    md += ["", "## Findings", "", "| Id | Check | Detail | Evidence |", "|---|---|---|---|"]
    for x in sorted(findings, key=lambda x: x["level"] == "info"):
        ev = x["evidence"].replace("|", "/")
        md.append(f"| `{x['id']}` | {x['check']} | {x['detail']} | {ev} |")
    (out / "ac-checks.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=1))


if __name__ == "__main__":
    main()
