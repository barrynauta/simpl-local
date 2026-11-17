#!/usr/bin/env python3
"""Map acceptance-criterion ids from an old inventory to a regenerated one, and rewrite them in
the review files (links.json, ac-findings.json, domain-findings.json).

Needed when the step 12a parser changes and a story's criteria get new ids (split, merged or
removed). Each old AC is matched to the new AC of the same story whose clause text is most alike;
an old AC with no good match (below 0.6) is reported as removed and left as it is in the files.

Run: python3 pipeline/remap_ids.py <old inventory.json> <new inventory.json> <file.json> [...]
Prints the mapping of every id that changed.
"""
import difflib
import json
import sys


def acs(inv):
    out = {}
    for s in inv["stories"]:
        for a in s["acs"]:
            out[a["id"]] = (s["key"], " ".join(c["text"] for c in a["clauses"]).lower())
    return out


def main():
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    old, new = (json.load(open(p, encoding="utf-8")) for p in sys.argv[1:3])
    o, n = acs(old), acs(new)
    mapping, removed = {}, []
    for oid, (key, text) in o.items():
        if oid in n and difflib.SequenceMatcher(None, text, n[oid][1]).ratio() > 0.9:
            continue
        cands = [(difflib.SequenceMatcher(None, text, nt).ratio(), nid) for nid, (k, nt) in n.items() if k == key]
        best = max(cands, default=(0, None))
        if best[0] >= 0.6:
            mapping[oid] = best[1]
        else:
            removed.append(oid)
    for oid, nid in sorted(mapping.items()):
        print(f"{oid} -> {nid}")
    for oid in removed:
        print(f"{oid} -> (removed: no matching criterion in the new inventory)")

    def fix(x):
        if isinstance(x, str):
            return mapping.get(x, x)
        if isinstance(x, list):
            return [fix(v) for v in x]
        if isinstance(x, dict):
            return {k: (fix(v) if k in ("from", "to", "target", "ids", "id_list") else v) for k, v in x.items()}
        return x

    for path in sys.argv[3:]:
        data = json.load(open(path, encoding="utf-8"))
        data = fix(data)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=1, ensure_ascii=False)
            f.write("\n")
        print(f"rewrote {path}")


if __name__ == "__main__":
    main()
