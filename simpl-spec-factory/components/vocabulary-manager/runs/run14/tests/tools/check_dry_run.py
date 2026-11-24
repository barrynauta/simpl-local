"""Dry-run check: every feature parses and no step is undefined or ambiguous.

behave 1.3 reports a feature as 'failed' in --dry-run when it mixes Scenario Outlines (shown as
passed) and Scenarios (shown as untested), although no step failed. So this check reads the step
results instead of the feature status. Usage: check_dry_run.py <tag expression>
"""
import json
import subprocess
import sys

tags = sys.argv[1] if len(sys.argv) > 1 else "not @ui"
out = "reports/dry-run.json"
import os
if os.path.exists(out):
    os.remove(out)
proc = subprocess.run([sys.executable, "-m", "behave", "--dry-run", f"--tags={tags}", "-f", "json", "-o", out, "-f", "null"],
                      capture_output=True, text=True)
if proc.returncode not in (0, 1):
    print(proc.stdout[-3000:], proc.stderr[-3000:])
    sys.exit(2)
try:
    data = json.load(open(out))
except Exception as exc:  # noqa: BLE001
    print("dry-run produced no valid report:", exc, proc.stderr[-3000:])
    sys.exit(2)
counts, undefined = {}, []
scenarios = 0
for feature in data:
    for element in feature.get("elements", []):
        if element.get("type") == "scenario":
            scenarios += 1
        for step in element.get("steps", []):
            status = step.get("result", {}).get("status", "untested")
            counts[status] = counts.get(status, 0) + 1
            if status in ("undefined", "failed", "error"):
                undefined.append(f"{feature['location']}: {step['keyword']} {step['name']} ({status})")
print(f"tags: {tags}; features {len(data)}; scenarios {scenarios}; step results {counts}")
for line in undefined[:50]:
    print("  ", line)
if "ParserError" in proc.stdout + proc.stderr:
    print((proc.stdout + proc.stderr)[-2000:])
    sys.exit(1)
summary = [l for l in (proc.stdout + proc.stderr).splitlines() if " steps passed" in l]
print(summary[-1] if summary else "no summary line")
import re
m = re.search(r"(\d+) undefined", summary[-1]) if summary else None
if m and int(m.group(1)):
    undefined.append(f"{m.group(1)} undefined steps")
    print("   ", undefined[-1])
if "AmbiguousStep" in proc.stderr or "ParserError" in proc.stderr:
    print(proc.stderr[-3000:])
    sys.exit(1)
sys.exit(1 if undefined else 0)
