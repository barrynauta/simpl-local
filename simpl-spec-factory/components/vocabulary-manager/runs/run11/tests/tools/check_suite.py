"""Static check of the suite, no running system needed.

- every API step matches exactly one step definition (no undefined, no ambiguous step);
- every scenario has a criterion tag or a decision tag;
- every criterion, decision and rule tag exists (tools/known_ids.json);
- prints scenario counts per feature and per tag kind.
Exit code 1 on a problem.
"""
import collections
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(ROOT, "support"))

from behave.parser import parse_file  # noqa: E402
from behave import step_registry  # noqa: E402
import importlib.util  # noqa: E402

spec = importlib.util.spec_from_file_location("library", os.path.join(ROOT, "features", "steps", "library.py"))
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
spec_ui = importlib.util.spec_from_file_location("ui", os.path.join(ROOT, "features", "steps", "ui.py"))
mod_ui = importlib.util.module_from_spec(spec_ui)
spec_ui.loader.exec_module(mod_ui)

known = json.load(open(os.path.join(HERE, "known_ids.json")))
criteria = set(known["criteria"])
decisions = set(known["decisions"])
rules = set(known["rules"])
FREE_TAGS = {"negative", "ui", "wip", "store_down", "a11y", "manual"}

problems = []
per_feature = collections.Counter()
per_kind = collections.Counter()
scenario_count = 0


def matches(step_type, text):
    found = []
    for kind in (step_type,):
        for sd in step_registry.registry.steps[kind]:
            if sd.match(text):
                found.append(sd)
    return found


for path in sorted(glob.glob(os.path.join(ROOT, "features", "*.feature"))):
    feature = parse_file(path)
    for sc in feature.walk_scenarios():
        scenario_count += 1
        tags = set(sc.effective_tags)
        per_feature[os.path.basename(path)] += 1
        kinds = set()
        for t in tags:
            if t.startswith("AC_"):
                kinds.add("criterion")
                cid = t[3:].replace("_AC", "/AC")
                if cid not in criteria:
                    problems.append(f"{path}: unknown criterion tag @{t}")
            elif re.fullmatch(r"[DEQ]\d+", t):
                kinds.add("decision" if t[0] in "DE" else "quality")
                if t not in decisions:
                    problems.append(f"{path}: unknown decision tag @{t}")
            elif t in FREE_TAGS:
                kinds.add(t)
            else:
                kinds.add("rule")
                if t not in rules:
                    problems.append(f"{path}: unknown rule tag @{t} in '{sc.name}'")
        for k in kinds:
            per_kind[k] += 1
        if not any(t.startswith("AC_") for t in tags) and not any(re.fullmatch(r"[DE]\d+", t) for t in tags):
            problems.append(f"{path}: '{sc.name}' has neither a criterion nor a decision tag")
        if "manual" in tags:
            continue
        for step in sc.steps:
            found = matches(step.step_type, step.name)
            if len(found) != 1:
                problems.append(f"{path}:{step.line}: step '{step.keyword} {step.name}' matches {len(found)} definitions")

print(f"{scenario_count} scenarios")
for f, n in sorted(per_feature.items()):
    print(f"  {f}: {n}")
for k, n in sorted(per_kind.items()):
    print(f"  tagged {k}: {n}")
if problems:
    print("\n".join(problems))
    sys.exit(1)
print("suite check passed: every step matches one definition, every tag is known")
