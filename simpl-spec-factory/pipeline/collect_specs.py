#!/usr/bin/env python3
"""Step 10: build the input set of one component from its manifest.

Reads <component>/sources.yaml and writes a new, dated input set: the OpenAPI files and
business processes from code.europa.eu at the pinned commits, and the stories from Jira
(through collect_stories.py, with the local model). Also writes SOURCES.json: the full
commit ids, the story keys and a sha256 per file, so the set can be checked later.

code.europa.eu is read without a token (the projects are public). Jira needs JIRA_PAT.

Usage:
  collect_specs.py <component>                    # new set in <component>/specs-YYYYMMDD/
  collect_specs.py <component> --out <dir>
  collect_specs.py <component> --no-stories       # API and business processes only
  collect_specs.py <component> --verify           # compare the manifest with <component>/specs/,
                                                  # write nothing; story keys only if JIRA_PAT is set
Prints counts and differences only, never story text.
"""
import argparse
import datetime
import fnmatch
import hashlib
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

import yaml

FACTORY = Path(__file__).resolve().parent.parent
GITLAB = os.environ.get("GITLAB_URL", "https://code.europa.eu").rstrip("/")
JIRA_URL = os.environ.get("JIRA_URL", "https://jira.simplprogramme.eu").rstrip("/")


def get(url, headers=None, raw=False, missing_ok=False):
    """GET with a wait and retry when code.europa.eu rate-limits (429)."""
    for attempt in range(6):
        req = urllib.request.Request(url, headers=headers or {})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
            return data if raw else json.loads(data)
        except urllib.error.HTTPError as e:
            if e.code == 429 and attempt < 5:
                time.sleep(int(e.headers.get("Retry-After") or 10 * (attempt + 1)))
                continue
            if e.code == 404 and missing_ok:
                return None
            if e.code == 404:
                sys.exit(f"not found (check project, ref and path in sources.yaml): {url.split('?')[0]}")
            raise


def proj(path):
    return f"{GITLAB}/api/v4/projects/{urllib.parse.quote(path, safe='')}"


def commit(project, ref):
    c = get(f"{proj(project)}/repository/commits/{urllib.parse.quote(ref, safe='')}")
    return c["id"], c["committed_date"][:10]


def tree(project, sha, path):
    """All blobs under path (a folder), or [path] if it is a file."""
    out, page = [], 1
    while True:
        q = urllib.parse.urlencode({"ref": sha, "path": path, "recursive": "true", "per_page": 100, "page": page})
        items = get(f"{proj(project)}/repository/tree?{q}", missing_ok=True) or []
        out += [i["path"] for i in items if i["type"] == "blob"]
        if len(items) < 100:
            break
        page += 1
    return out or [path]


def raw(project, sha, path):
    return get(f"{proj(project)}/repository/files/{urllib.parse.quote(path, safe='')}/raw?ref={sha}", raw=True)


ALWAYS_EXCLUDE = [".gitkeep"]


def excluded(rel, patterns):
    return any(fnmatch.fnmatch(rel, p) or fnmatch.fnmatch(rel.split("/")[-1], p) for p in (patterns or []) + ALWAYS_EXCLUDE)


def fetch_repo_files(src, kind):
    """Yield (path inside the input set, bytes) for one api or functional source."""
    sha, date = commit(src["project"], src["ref"])
    src["_commit"], src["_date"] = sha, date
    paths = src.get("paths") or [src["path"]]
    for base in paths:
        for f in tree(src["project"], sha, base):
            rel = f[len(base):].lstrip("/") if f != base else Path(f).name
            if excluded(rel, src.get("exclude")):
                continue
            if src.get("dest"):
                target = src["dest"]  # older input sets put a single file straight in api/ or functional/
            elif kind == "api":
                target = f"api/{src['name']}/{rel}"
            else:
                target = f"functional/{Path(base).name}/{rel}" if f != base else f"functional/{rel}"
            yield target, raw(src["project"], sha, f)


def jira_keys(stories):
    keys = set(stories.get("keys") or [])
    if stories.get("jql"):
        token = os.environ.get("JIRA_PAT")
        if not token:
            return None
        start = 0
        while True:
            q = urllib.parse.urlencode({"jql": stories["jql"], "fields": "key", "maxResults": 100, "startAt": start})
            r = get(f"{JIRA_URL}/rest/api/2/search?{q}", {"Authorization": "Bearer " + token, "Accept": "application/json"})
            keys |= {i["key"] for i in r["issues"]}
            start += len(r["issues"])
            if start >= r["total"] or not r["issues"]:
                break
    keys -= set(stories.get("exclude_keys") or [])
    return sorted(keys, key=lambda k: int(k.split("-")[1]))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("component")
    ap.add_argument("--out")
    ap.add_argument("--no-stories", action="store_true")
    ap.add_argument("--verify", action="store_true")
    a = ap.parse_args()

    comp = FACTORY / a.component
    manifest = yaml.safe_load((comp / "sources.yaml").read_text(encoding="utf-8"))

    files = {}
    for src in manifest.get("api", []):
        files.update(fetch_repo_files(src, "api"))
    for src in manifest.get("functional", []):
        files.update(fetch_repo_files(src, "functional"))
    keys = jira_keys(manifest.get("stories", {})) if not a.no_stories else None

    if a.verify:
        current = comp / "specs"
        have = {str(p.relative_to(current)) for p in current.rglob("*")
                if p.is_file() and p.parts[len(current.parts)] in ("api", "functional") and p.name not in ALWAYS_EXCLUDE}
        same = [f for f in files if f in have and (current / f).read_bytes() == files[f]]
        diff = [f for f in files if f in have and f not in same]
        missing = sorted(set(files) - have)
        extra = sorted(have - set(files))
        print(f"{a.component}: {len(same)} files identical, {len(diff)} different, "
              f"{len(missing)} in the manifest but not in specs/, {len(extra)} in specs/ but not in the manifest")
        for label, lst in (("different", diff), ("not in specs/", missing), ("not in the manifest", extra)):
            for f in lst:
                print(f"  {label}: {f}")
        if keys is not None:
            local = {p.stem for p in (current / "stories").glob("SIMPL-*.md")}
            print(f"  stories: {len(keys)} from Jira now, {len(local)} in specs/; "
                  f"new since collection: {sorted(set(keys) - local)}; gone: {sorted(local - set(keys))}")
        else:
            print("  stories: not checked (set JIRA_PAT, or the manifest lists no query)")
        return

    out = Path(a.out) if a.out else comp / f"specs-{datetime.date.today():%Y%m%d}"
    if out.exists() and any(out.iterdir()):
        sys.exit(f"{out} exists and is not empty: an input set is never refreshed in place")
    for f, data in files.items():
        p = out / f
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_bytes(data)

    if keys:
        subprocess.run([sys.executable, str(FACTORY / "pipeline/collect_stories.py"), "--out", str(out / "stories"), *keys],
                       check=True)

    record = {
        "component": a.component,
        "collected": f"{datetime.date.today():%Y-%m-%d}",
        "api": [{k: s[k] for k in ("name", "project", "ref") if k in s} | {"commit": s["_commit"], "commit_date": s["_date"]}
                for s in manifest.get("api", [])],
        "functional": [{"project": s["project"], "ref": s["ref"], "commit": s["_commit"], "commit_date": s["_date"],
                        "paths": s.get("paths") or [s["path"]]} for s in manifest.get("functional", [])],
        "stories": {"query": manifest.get("stories", {}).get("jql"), "keys": keys or []},
        "files": {str(p.relative_to(out)): hashlib.sha256(p.read_bytes()).hexdigest()
                  for p in sorted(out.rglob("*")) if p.is_file()},
    }
    (out / "SOURCES.json").write_text(json.dumps(record, indent=1) + "\n", encoding="utf-8")
    print(f"{out}: {len(files)} API and process files, {len(keys or [])} stories. "
          "Write the README of the set by hand: what is in, what is left out and why.")


if __name__ == "__main__":
    main()
