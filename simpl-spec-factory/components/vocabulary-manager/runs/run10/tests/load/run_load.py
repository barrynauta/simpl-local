"""Seed the store (Q4, D99), run the Locust throughput mix (Q1, D99) and the separate 5 MB write
test (D71), then write reports/load.json with p50, p95, p99 and the error rate per operation.

Environment (defaults follow the decisions):
  API_URL, TOKEN_URL, TOKEN_CLIENT, USER_PASSWORD   as for the API suite
  LOAD_DURATION=5m                 throughput run length (D99)
  LOAD_SEED_VOCABULARIES=1000      seed size (Q4, D99)
  LOAD_SEED_VERSIONS=20            versions per seeded vocabulary
  LOAD_SEED_FILE_BYTES=10240       seeded file size, about 10 KB (D99)
  LOAD_SEED_TAG=Seed1              seed names Ld<tag>N0001...; a rerun tops up the same seed
  LOAD_SEED_WORKERS=4              parallel seed uploads (D92 allows 4 per instance)
  LOAD_SKIP_SEED=0                 1 to skip seeding
  LOAD_BIG_WRITES=5                number of 5 MB creations in the separate test (0 to skip)
"""

import json
import math
import os
import subprocess
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
sys.path.insert(0, str(ROOT / "features" / "support"))

from vmclient import CONFIG, encode_multipart, fetch_token, http, turtle_of_size  # noqa: E402

SEED_TAG = os.environ.get("LOAD_SEED_TAG", "Seed1")
SEED_COUNT = int(os.environ.get("LOAD_SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(os.environ.get("LOAD_SEED_VERSIONS", "20"))
SEED_BYTES = int(os.environ.get("LOAD_SEED_FILE_BYTES", "10240"))
SEED_WORKERS = int(os.environ.get("LOAD_SEED_WORKERS", "4"))
EXT_NAMESPACE = os.environ.get("LOAD_EXT_NAMESPACE", f"https://ext.example.org/load/{SEED_TAG}")
DURATION = os.environ.get("LOAD_DURATION", "5m")
BIG_WRITES = int(os.environ.get("LOAD_BIG_WRITES", "5"))
READ_RPS, WRITE_RPS = 20.0, 1.0
READ_USERS = int(os.environ.get("LOAD_READ_USERS", "20"))
WRITE_USERS = int(os.environ.get("LOAD_WRITE_USERS", "3"))
REPORT = ROOT / "reports" / "load.json"

READ_OPS = ["getVocabularies", "getVocabulary", "getVocabularyVersions", "getVocabularyVersion",
            "searchVocabularies", "searchExternalVocabularies", "downloadVocabulary",
            "downloadVocabularyVersion", "getExternalVocabularies"]
WRITE_OPS = ["createVocabulary", "createVocabularyVersion"]


def seed_name(i):
    return f"Ld{SEED_TAG}N{i:04d}"


def upload(path, parts):
    body, ctype = encode_multipart(parts)
    return http("POST", CONFIG.api_url + path, token=fetch_token("ga-admin"), body=body, content_type=ctype,
                timeout=120)


def seed_one(i):
    name = seed_name(i)
    resp = http("GET", f"{CONFIG.api_url}/vocabularies/{name}", token=fetch_token("ga-admin"))
    have = resp.json()["version"] if resp.status_code == 200 else 0
    while have < SEED_VERSIONS:
        data = turtle_of_size(SEED_BYTES, f"https://example.org/load/{name}/v{have + 1}#")
        if have == 0:
            r = upload("/vocabularies", [
                {"name": "vocabularyFile", "data": data, "filename": "v.ttl", "content_type": "text/turtle"},
                {"name": "name", "data": name}, {"name": "description", "data": f"Seed {name}"},
                {"name": "changelog", "data": "Initial version."}])
        else:
            r = upload(f"/vocabularies/{name}/versions", [
                {"name": "vocabularyFile", "data": data, "filename": "v.ttl", "content_type": "text/turtle"},
                {"name": "description", "data": f"Seed {name} v{have + 1}"},
                {"name": "changelog", "data": f"Seed change {have + 1}"}])
        if r.status_code == 503:
            time.sleep(1)
            continue
        if r.status_code != 201:
            raise RuntimeError(f"seed {name}: {r.status_code} {r.text[:200]}")
        have += 1


def seed():
    started = time.time()
    with ThreadPoolExecutor(max_workers=SEED_WORKERS) as pool:
        list(pool.map(seed_one, range(1, SEED_COUNT + 1)))
    ext = http("POST", CONFIG.api_url + "/external-vocabulary/searches", token=fetch_token("ga-admin"),
               body=json.dumps({"namespaceUri": EXT_NAMESPACE}).encode(), content_type="application/json")
    if ext.status_code == 200 and not ext.json().get("items"):
        data = f"@prefix e: <{EXT_NAMESPACE}#> .\ne:term a <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .\n"
        upload("/external-vocabularies", [
            {"name": "vocabularyFile", "data": data, "filename": "e.ttl", "content_type": "text/turtle"},
            {"name": "name", "data": f"Ld{SEED_TAG}Ext"}, {"name": "namespaceUri", "data": EXT_NAMESPACE}])
    return time.time() - started


def percentile(values, p):
    if not values:
        return None
    values = sorted(values)
    k = max(0, math.ceil(p * len(values)) - 1)
    return values[k]


def big_writes():
    durations, failures = [], 0
    for i in range(BIG_WRITES):
        name = f"Ld{uuid.uuid4().hex[:8]}Big{i}"
        data = turtle_of_size(5 * 1024 * 1024, f"https://example.org/load/{name}#")
        t0 = time.time()
        r = upload("/vocabularies", [
            {"name": "vocabularyFile", "data": data, "filename": "v.ttl", "content_type": "text/turtle"},
            {"name": "name", "data": name}, {"name": "description", "data": "5 MB write"},
            {"name": "changelog", "data": "Initial version."}])
        durations.append((time.time() - t0) * 1000)
        if r.status_code != 201:
            failures += 1
    n = len(durations)
    return {"method": "POST", "requests": n, "failures": failures,
            "error_rate": failures / n if n else 0.0,
            "p50_ms": percentile(durations, 0.50), "p95_ms": percentile(durations, 0.95),
            "p99_ms": percentile(durations, 0.99)}


def main():
    report = {"targets": {
        "Q1_read_p95_ms": 300, "Q1_write_p95_ms_up_to_1MB": 3000, "Q1_write_p95_ms_5MB": 30000,
        "Q1_offered_reads_per_s": READ_RPS, "Q1_offered_writes_per_s": WRITE_RPS,
        "D99_min_achieved_share": 0.95, "D99_duration": DURATION},
        "api_url": CONFIG.api_url}
    if os.environ.get("LOAD_SKIP_SEED", "0") != "1":
        report["seed_seconds"] = seed()
    locust_out = ROOT / "reports" / "load-locust.json"
    if locust_out.exists():
        locust_out.unlink()
    cmd = [str(Path(sys.executable).parent / "locust"), "-f", str(HERE / "locustfile.py"), "--headless",
           "-u", str(READ_USERS + WRITE_USERS), "-r", str(READ_USERS + WRITE_USERS), "-t", DURATION, "--host", CONFIG.api_url, "--only-summary",
           "--stop-timeout", "30"]
    proc = subprocess.run(cmd, cwd=str(ROOT))
    report["locust_exit_code"] = proc.returncode
    locust = json.loads(locust_out.read_text()) if locust_out.exists() else {"operations": {}, "duration_s": 0}
    ops = locust["operations"]
    report["duration_s"] = locust["duration_s"]
    if BIG_WRITES:
        ops["createVocabulary_5MB"] = big_writes()
    report["operations"] = ops

    reads = [ops[o] for o in READ_OPS if o in ops]
    writes = [ops[o] for o in WRITE_OPS if o in ops]
    read_rate = sum(o["achieved_rps"] for o in reads)
    write_rate = sum(o["achieved_rps"] for o in writes)
    errors = sum(o["failures"] for o in reads + writes)
    report["summary"] = {
        "achieved_reads_per_s": read_rate,
        "achieved_writes_per_s": write_rate,
        "errors_in_mix": errors,
        "reads_p95_met": all(o["p95_ms"] is not None and o["p95_ms"] <= 300 for o in reads) and bool(reads),
        "writes_p95_met": all(o["p95_ms"] is not None and o["p95_ms"] <= 3000 for o in writes) and bool(writes),
        "throughput_met": read_rate >= 0.95 * READ_RPS and write_rate >= 0.95 * WRITE_RPS and errors == 0,
        "write_5MB_p95_met": (ops["createVocabulary_5MB"]["p95_ms"] or 1e12) <= 30000
        if "createVocabulary_5MB" in ops else None,
    }
    REPORT.write_text(json.dumps(report, indent=2))
    print(json.dumps(report["summary"], indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
