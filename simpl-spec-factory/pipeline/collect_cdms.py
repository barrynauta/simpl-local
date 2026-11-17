#!/usr/bin/env python3
"""Step 10 (CDMs): freeze the CDM register and every CDM it links to.

Reads the CDM register (cdm-register/cdm-index.md in the architecture repository, at the
commit in cdm/register.yaml), follows each link to its repository and branch, pins the
branch to its current commit and writes:

  cdm/cdm-index.md            the register itself
  cdm/<service-repo>.md       one file per CDM that could be fetched
  cdm/SOURCES.json            per CDM: link, project, branch, commit, date, sha256, or the error

Links that no longer resolve are kept in SOURCES.json with the error; cdm/register.yaml can
give a corrected location for them (`overrides`), which is then used and recorded as such.

One register snapshot serves all components: each component's sources.yaml says which CDM
belongs to which of its APIs (`cdm:`). A new snapshot goes to a new folder (--out).

Usage:
  collect_cdms.py                  # writes cdm/ (must be empty or absent)
  collect_cdms.py --out cdm-YYYYMMDD
"""
import argparse
import datetime
import hashlib
import json
import re
import sys
import urllib.parse
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).resolve().parent))
from collect_specs import commit, get, proj  # noqa: E402

FACTORY = Path(__file__).resolve().parent.parent
LINK = re.compile(r"https://code\.europa\.eu/(?P<project>.+?)/-/blob/(?P<rest>[^)\s?]+)")


def split_ref(project, rest):
    """'feature/add-cdm/documents/x.md' -> ('feature/add-cdm', 'documents/x.md'), trying branch names."""
    parts = rest.split("/")
    for i in range(1, len(parts)):
        ref, path = "/".join(parts[:i]), "/".join(parts[i:])
        try:
            sha, date = commit(project, ref)
            return ref, path, sha, date
        except SystemExit:
            continue
    raise SystemExit(f"no branch found in {rest}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--out", default="cdm")
    a = ap.parse_args()
    out = FACTORY / a.out
    cfg = yaml.safe_load((FACTORY / "cdm/register.yaml").read_text(encoding="utf-8"))
    keep = ("register.yaml", "README.md", "scan.json")
    if out.exists() and any(p.name not in keep for p in out.iterdir()):
        sys.exit(f"{out} already holds a snapshot: write a new one with --out")
    out.mkdir(exist_ok=True)

    reg = cfg["register"]
    rsha, rdate = commit(reg["project"], reg["ref"])
    index = get(f"{proj(reg['project'])}/repository/files/{urllib.parse.quote(reg['path'], safe='')}/raw?ref={rsha}", raw=True)
    (out / "cdm-index.md").write_bytes(index)

    records = []
    for row in index.decode("utf-8").splitlines():
        if not row.startswith("|") or "code.europa.eu" not in row:
            continue
        cells = [c.strip() for c in row.strip("|").split("|")]
        service, owner, status, approved = cells[0], cells[1], cells[2], cells[3]
        m = LINK.search(row)
        link = m.group(0)
        rec = {"service": service, "owner": owner, "status": status, "approved": approved, "link": link}
        loc = cfg.get("overrides", {}).get(service)
        try:
            if loc:
                project, ref, path = loc["project"], loc["ref"], loc["path"]
                sha, date = commit(project, ref)
                rec["override"] = "the register link does not resolve; location from register.yaml"
            else:
                project = m.group("project")
                ref, path, sha, date = split_ref(project, m.group("rest").strip())
            data = get(f"{proj(project)}/repository/files/{urllib.parse.quote(path, safe='')}/raw?ref={sha}",
                       raw=True, missing_ok=True)
            if data is None:
                raise SystemExit("file not found at that branch")
            name = project.split("/")[-1]
            (out / f"{name}.md").write_bytes(data)
            rec |= {"file": f"{name}.md", "project": project, "branch": ref, "path": path, "commit": sha,
                    "commit_date": date, "sha256": hashlib.sha256(data).hexdigest()}
        except SystemExit as e:
            rec["error"] = str(e)
        records.append(rec)

    for service, loc in (cfg.get("extra") or {}).items():
        rec = {"service": service, "owner": "", "status": "not in register", "approved": "No", "link": "",
               "extra": "found in the repository by scan_cdms.py, not listed in the register"}
        try:
            sha, date = commit(loc["project"], loc["ref"])
            data = get(f"{proj(loc['project'])}/repository/files/{urllib.parse.quote(loc['path'], safe='')}/raw?ref={sha}",
                       raw=True, missing_ok=True)
            if data is None:
                raise SystemExit("file not found at that branch")
            name = loc.get("file") or loc["project"].split("/")[-1] + Path(loc["path"]).suffix
            (out / name).write_bytes(data)
            rec |= {"file": name, "project": loc["project"], "branch": loc["ref"], "path": loc["path"], "commit": sha,
                    "commit_date": date, "sha256": hashlib.sha256(data).hexdigest()}
        except SystemExit as e:
            rec["error"] = str(e)
        records.append(rec)

    src = {"collected": f"{datetime.date.today():%Y-%m-%d}",
           "register": {"project": reg["project"], "path": reg["path"], "commit": rsha, "commit_date": rdate},
           "cdms": records}
    (out / "SOURCES.json").write_text(json.dumps(src, indent=1, ensure_ascii=False) + "\n", encoding="utf-8")
    ok = [r for r in records if "file" in r]
    print(f"{out.name}: register at {rsha[:8]} ({rdate}), {len(ok)} of {len(records)} CDMs fetched")
    for r in records:
        if "error" in r:
            print(f"  not fetched: {r['service']}: {r['error']}")


if __name__ == "__main__":
    main()
