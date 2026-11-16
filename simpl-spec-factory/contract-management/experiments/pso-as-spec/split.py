"""Hold-out split of the PSO contract-billing scenarios, stratified by feature file, fixed seed.
Usage: python3 split.py <pso checkout> > split.json"""
import json, random, re, sys, pathlib
FEATURES = ["regression/contract-billing/contract-billing.feature",
            "schema-validation/contract-billing/contract-billing-schema-validation.feature"]
SEED = 20261116
root = pathlib.Path(sys.argv[1]) / "backend-tests/src/test/resources/features"
rng = random.Random(SEED)
out = {"seed": SEED, "pso_commit": "ec7a546", "visible": [], "hidden": []}
for f in FEATURES:
    ids = re.findall(r"@(TC_?CB_\w+)", (root / f).read_text())
    ids = sorted(set(ids))
    rng.shuffle(ids)
    half = (len(ids) + 1) // 2
    out["visible"] += sorted(ids[:half]); out["hidden"] += sorted(ids[half:])
json.dump(out, sys.stdout, indent=2); print()
