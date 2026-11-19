"""Regenerate the tables of COVERAGE.md from the feature tags.

Usage: python3 tools/coverage.py ../input > /tmp/tables.md
Prints the criterion, decision and rule tables; the reasons for gaps are kept by hand in COVERAGE.md.
"""
import collections
import glob
import json
import os
import re
import sys

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
inp = sys.argv[1] if len(sys.argv) > 1 else os.path.join(root, "..", "input")
ids = json.load(open(os.path.join(inp, "ids.json")))
decisions = json.load(open(os.path.join(inp, "decisions", "decisions.json")))

ac_map = collections.defaultdict(list)
dec_map = collections.defaultdict(list)
rule_map = collections.defaultdict(list)
for fn in sorted(glob.glob(os.path.join(root, "features", "*.feature"))):
    lines = open(fn).read().splitlines()
    ftags, pend = [], []
    for line in lines:
        s = line.strip()
        if s.startswith("@"):
            pend += s.split()
        elif s.startswith("Feature:"):
            ftags, pend = pend, []
        elif re.match(r"(Scenario|Scenario Outline):", s):
            tags = [t[1:] for t in ftags + pend]
            pend = []
            name = s.split(":", 1)[1].strip()
            kind = "UI" if "ui" in tags else ("wip" if "wip" in tags else ("store-down" if "store_down" in tags else "API"))
            ref = f"{os.path.basename(fn)}: {name} ({kind})"
            for t in tags:
                if t.startswith("AC_"):
                    ac_map[t[3:].replace("_", "/", 1)].append(ref)
                elif re.match(r"^(D|E|Q)\d+$", t):
                    dec_map[t].append(ref)
                elif t not in ("negative", "ui", "wip", "store_down", "slow"):
                    rule_map[t].append(ref)

print("| Criterion | Scenarios |")
print("|---|---|")
for story in ids["stories"]:
    for ac in story["acs"]:
        if ac["struck"]:
            continue
        refs = ac_map.get(ac["id"], [])
        print(f"| {ac['id']} | " + ("<br>".join(refs) if refs else "none") + " |")
print()
print("| Decision | Scenarios |")
print("|---|---|")
for d in decisions:
    refs = dec_map.get(d["id"], [])
    shown = refs[:6]
    more = f"<br>and {len(refs) - 6} more" if len(refs) > 6 else ""
    print(f"| {d['id']} | " + ("<br>".join(shown) + more if refs else "none") + " |")
print()
print("| Rule | Scenarios |")
print("|---|---|")
for r in sorted(rule_map):
    print(f"| {r} | {len(rule_map[r])} scenarios, for example {rule_map[r][0]} |")
