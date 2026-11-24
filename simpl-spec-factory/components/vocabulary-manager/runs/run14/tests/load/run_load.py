"""Run the Q1 load test and write reports/load.json.

Phases:
1. seed (D99): top up LOAD_VOCABULARIES vocabularies named <LOAD_PREFIX>00000.. with LOAD_VERSIONS
   versions of about 10 KB each; idempotent; skipped with LOAD_SEED=0.
2. throughput mix (D99, D136, D183): Locust, 20 reads/s and 1 write/s offered, LOAD_WARMUP seconds of
   warm-up, then LOAD_DURATION seconds measured.
3. 5 MB writes apart (D71, D99): LOAD_BIG_WRITES new versions with a file at the 5 MB limit, one after
   the other; target 30 s each.
Per operation it reports p50, p95, p99 (ms), the error rate and the achieved rate.
"""
import csv
import json
import os
import subprocess
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, ROOT)
from vmtest import client, config, turtle  # noqa: E402

PREFIX = os.environ.get("LOAD_PREFIX", "Load")
VOCABULARIES = int(os.environ.get("LOAD_VOCABULARIES", "1000"))
VERSIONS = int(os.environ.get("LOAD_VERSIONS", "20"))
SEED = os.environ.get("LOAD_SEED", "1") != "0"
SEED_THREADS = int(os.environ.get("LOAD_SEED_THREADS", "4"))
WARMUP = int(os.environ.get("LOAD_WARMUP", "30"))
DURATION = int(os.environ.get("LOAD_DURATION", "300"))
READS = float(os.environ.get("LOAD_READS_PER_SECOND", "20"))
WRITES = float(os.environ.get("LOAD_WRITES_PER_SECOND", "1"))
READ_USERS = int(os.environ.get("LOAD_READ_USERS", "20"))
WRITE_USERS = int(os.environ.get("LOAD_WRITE_USERS", "3"))
BIG_WRITES = int(os.environ.get("LOAD_BIG_WRITES", "3"))
REPORT = os.path.join(ROOT, "reports", "load.json")
CSV_PREFIX = os.path.join(ROOT, "reports", "load")

READ_OPS = ["getVocabularies", "getVocabulary", "getVocabularyVersions", "getVocabularyVersion",
            "searchVocabularies", "downloadVocabulary", "downloadVocabularyVersion", "getExternalVocabularies"]
WRITE_OPS = ["createVocabularyVersion"]
READ_P95_MS = 300        # Q1
WRITE_P95_MS = 3000      # Q1, files up to 1 MB
BIG_WRITE_MS = 30000     # Q1 and D71, file at the 5 MB limit


def seed_one(i):
    name = f"{PREFIX}{i:05d}"
    token = client.token_for("ga-admin")
    response = client.send("GET", f"{config.API_URL}/vocabularies/{name}", token=token)
    have = response.json().get("version", 0) if response.status_code == 200 else 0
    while have < VERSIONS:
        content = turtle.sized_file(f"seed{i}v{have + 1}", name, 10_000)
        parts = [("description", f"Seed version {have + 1} of {name}"), ("changelog", f"Seed {have + 1}"),
                 ("vocabularyFile", ("v.ttl", content, "text/turtle"))]
        if have == 0:
            parts.insert(0, ("name", name))
            url = f"{config.API_URL}/vocabularies"
        else:
            url = f"{config.API_URL}/vocabularies/{name}/versions"
        for attempt in range(5):
            result = client.send("POST", url, token=client.token_for("ga-admin"), files=client.multipart(parts))
            if result.status_code == 201:
                break
            if result.status_code == 409 and have == 0:
                break
            time.sleep(1 + attempt)
        else:
            raise RuntimeError(f"seed of {name} failed: {result.status_code} {result.text[:200]}")
        have += 1
    return name


def seed():
    started = time.time()
    with ThreadPoolExecutor(max_workers=SEED_THREADS) as pool:
        list(pool.map(seed_one, range(VOCABULARIES)))
    return round(time.time() - started, 1)


def run_locust():
    users = READ_USERS + WRITE_USERS
    env = dict(os.environ, LOAD_WARMUP=str(WARMUP), PYTHONPATH=ROOT)
    cmd = [sys.executable, "-m", "locust", "-f", os.path.join(HERE, "locustfile.py"), "--headless",
           "--host", config.API_URL, "-u", str(users), "-r", str(users), "-t", f"{WARMUP + DURATION}s",
           "--csv", CSV_PREFIX, "--only-summary", "--stop-timeout", "30"]
    subprocess.run(cmd, env=env, check=False)
    stats = {}
    path = CSV_PREFIX + "_stats.csv"
    if not os.path.exists(path):
        return stats
    with open(path, newline="") as handle:
        for row in csv.DictReader(handle):
            if row["Name"] == "Aggregated":
                continue
            requests_ = int(row["Request Count"])
            failures = int(row["Failure Count"])
            stats[row["Name"]] = {
                "requests": requests_,
                "failures": failures,
                "error_rate": round(failures / requests_, 4) if requests_ else None,
                "p50": _num(row.get("50%")),
                "p95": _num(row.get("95%")),
                "p99": _num(row.get("99%")),
                "rps": round(requests_ / DURATION, 3) if DURATION else None,
            }
    return stats


def _num(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def big_writes():
    if BIG_WRITES <= 0:
        return None
    run = str(int(time.time()))
    name = f"LoadBig{run}"
    token = client.token_for("ga-admin")
    parts = [("name", name), ("description", "Big write target"), ("changelog", "Initial version."),
             ("vocabularyFile", ("v.ttl", turtle.default_file(run, name), "text/turtle"))]
    client.send("POST", f"{config.API_URL}/vocabularies", token=token, files=client.multipart(parts))
    durations, failures = [], 0
    for i in range(BIG_WRITES):
        content = turtle.sized_file(f"big{run}{i}", name, 5 * 1024 * 1024)
        parts = [("description", f"Big {i}"), ("changelog", f"Big change {i}"),
                 ("vocabularyFile", ("v.ttl", content, "text/turtle"))]
        started = time.time()
        response = client.send("POST", f"{config.API_URL}/vocabularies/{name}/versions",
                               token=client.token_for("ga-admin"), files=client.multipart(parts), timeout=120)
        durations.append((time.time() - started) * 1000)
        if response.status_code != 201:
            failures += 1
    durations.sort()

    def pct(p):
        index = min(len(durations) - 1, max(0, int(round(p * len(durations) + 0.5)) - 1))
        return round(durations[index], 1)

    return {"requests": len(durations), "failures": failures,
            "error_rate": round(failures / len(durations), 4),
            "p50": pct(0.50), "p95": pct(0.95), "p99": pct(0.99), "rps": None}


def main():
    report = {
        "generated": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "api_url": config.API_URL,
        "offered": {"reads_per_second": READS, "writes_per_second": WRITES,
                    "warmup_s": WARMUP, "duration_s": DURATION},
        "seed": {"vocabularies": VOCABULARIES, "versions_each": VERSIONS, "file_bytes": 10_000,
                 "seconds": None},
        "unit": "ms",
        "operations": {},
        "targets": [],
    }
    if SEED:
        report["seed"]["seconds"] = seed()
    ops = run_locust()
    report["operations"].update(ops)
    big = big_writes()
    if big:
        report["operations"]["createVocabularyVersion (5 MB)"] = big

    achieved_reads = sum(ops.get(op, {}).get("requests", 0) for op in READ_OPS) / DURATION if DURATION else 0
    achieved_writes = sum(ops.get(op, {}).get("requests", 0) for op in WRITE_OPS) / DURATION if DURATION else 0
    report["achieved"] = {"reads_per_second": round(achieved_reads, 3),
                          "writes_per_second": round(achieved_writes, 3)}
    targets = report["targets"]
    for op in READ_OPS:
        stat = ops.get(op)
        targets.append({"id": "Q1", "operation": op, "check": f"p95 <= {READ_P95_MS} ms, no errors",
                        "met": bool(stat and stat["p95"] is not None and stat["p95"] <= READ_P95_MS
                                    and stat["failures"] == 0)})
    for op in WRITE_OPS:
        stat = ops.get(op)
        targets.append({"id": "Q1", "operation": op, "check": f"p95 <= {WRITE_P95_MS} ms, no errors",
                        "met": bool(stat and stat["p95"] is not None and stat["p95"] <= WRITE_P95_MS
                                    and stat["failures"] == 0)})
    if big:
        targets.append({"id": "Q1", "operation": "createVocabularyVersion (5 MB)",
                        "check": f"each <= {BIG_WRITE_MS} ms (D71), no errors",
                        "met": big["p99"] <= BIG_WRITE_MS and big["failures"] == 0})
    targets.append({"id": "Q1", "operation": "throughput",
                    "check": "achieved >= 95 % of 20 reads/s and 1 write/s, no errors (D99)",
                    "met": achieved_reads >= 0.95 * READS and achieved_writes >= 0.95 * WRITES
                    and all(s["failures"] == 0 for s in ops.values())})
    report["met"] = all(t["met"] for t in targets)
    os.makedirs(os.path.dirname(REPORT), exist_ok=True)
    with open(REPORT, "w") as handle:
        json.dump(report, handle, indent=2)
    print(json.dumps({"met": report["met"], "achieved": report["achieved"]}))


if __name__ == "__main__":
    main()
