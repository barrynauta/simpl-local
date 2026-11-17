"""Profile of the PSO API suite: scenarios per area, what they assert, how many touch a flow.
Usage: python3 pso_suite_profile.py <features dir> > pso-suite-profile.json"""
import re, sys, json, pathlib, collections
root = pathlib.Path(sys.argv[1])
NEG = re.compile(r"random|without authori|unauthori|invalid|non-?existent|missing|wrong|empty|malformed|not found|forbidden|expired|incorrect|bad ", re.I)
rows = []
for f in sorted(root.rglob("*.feature")):
    kind, area = f.relative_to(root).parts[0], f.relative_to(root).parts[1] if len(f.relative_to(root).parts) > 2 else "-"
    text = f.read_text()
    ftags = re.findall(r"@[\w-]+", text.split("Feature:")[0])
    for m in re.finditer(r"((?:^\s*@.*\n)*)^\s*Scenario(?: Outline)?:\s*(.*)\n((?:(?!^\s*(?:@|Scenario)).*\n?)*)", text, re.M):
        tags, name, body = m.group(1), m.group(2), m.group(3)
        tags = re.findall(r"@[\w-]+", tags)
        steps = [l.strip() for l in body.splitlines() if re.match(r"\s*(Given|When|Then|And|But)\b", l)]
        ex = len(re.findall(r"^\s*\|", body.split("Examples:")[1], re.M)) - 1 if "Examples:" in body else 1
        thens, cur = [], None
        for s in steps:
            w = s.split()[0]
            if w in ("Given", "When", "Then"): cur = w
            if cur == "Then": thens.append(s)
        whens = [s for s in steps if s.startswith("When")]
        status_only = all(re.search(r"status code", t) for t in thens) if thens else False
        schema = any("schema" in t for t in thens)
        rows.append(dict(kind=kind, area=area, file=f.name, name=name.strip(), tags=tags + ftags,
                         examples=max(ex, 1), steps=len(steps), whens=len(whens), thens=len(thens),
                         status_only=status_only, schema=schema,
                         negative=bool(NEG.search(name + " " + " ".join(whens))),
                         story=[t for t in tags if t.startswith("@SIMPL-")],
                         bug="@bug" in tags, live_only="@live-only" in tags, skip="@skip" in tags))
json.dump(rows, sys.stdout, indent=1)
