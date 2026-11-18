"""Lists scenarios per tag from the feature files (no behave run needed).

Usage: python tools/tag_report.py [--json]
"""
import glob
import json
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def scan():
    rows = []
    for path in sorted(glob.glob(os.path.join(ROOT, "features", "*.feature"))):
        tags = []
        for line in open(path, encoding="utf-8"):
            s = line.strip()
            if s.startswith("@"):
                tags += s.split()
            elif re.match(r"^(Scenario|Scenario Outline|Scenario Template):", s):
                rows.append({"feature": os.path.basename(path), "name": s.split(":", 1)[1].strip(),
                             "line": 0, "tags": tags})
                tags = []
            elif s and not s.startswith("#"):
                tags = [] if not s.startswith("Feature") else tags
    return rows


if __name__ == "__main__":
    rows = scan()
    by_tag = defaultdict(list)
    for r in rows:
        for t in r["tags"]:
            by_tag[t].append("%s: %s" % (r["feature"], r["name"]))
    if "--json" in sys.argv:
        print(json.dumps({"scenarios": rows, "by_tag": by_tag}, indent=1))
    else:
        for t in sorted(by_tag):
            print(t, len(by_tag[t]))
