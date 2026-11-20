"""Runs the Q1 load test (D99) and writes reports/load.json.

1. Seed (Q4, D99): SEED_VOCABULARIES vocabularies with SEED_VERSIONS versions each, files of about
   10 KB, names <LOAD_SEED_PREFIX><nnnnn>. Already seeded vocabularies are kept (the store is not reset).
   LOAD_SEED=0 skips seeding.
2. Throughput mix: Locust, offered 20 reads/s and 1 write/s for LOAD_DURATION (default 5m).
3. 5 MB writes (D71), apart from the mix: LOAD_BIG_WRITES creates with a file at the 5 MB limit.
The report holds p50, p95, p99 and the error rate per operation, and the Q1 verdicts.
"""
import json
import os
import subprocess
import sys
import time
import uuid
from concurrent.futures import ThreadPoolExecutor

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
sys.path.insert(0, os.path.join(ROOT, "lib"))
import requests  # noqa: E402
import vmtest as vm  # noqa: E402

REPORTS = os.path.join(ROOT, "reports")
SEED = os.environ.get("LOAD_SEED", "1") != "0"
SEED_PREFIX = os.environ.get("LOAD_SEED_PREFIX", "Loadseed")
SEED_VOCABULARIES = int(os.environ.get("SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(os.environ.get("SEED_VERSIONS", "20"))
SEED_FILE_BYTES = int(os.environ.get("SEED_FILE_BYTES", "10240"))
SEED_WORKERS = int(os.environ.get("SEED_WORKERS", "4"))  # D92: at most 4 uploads at a time
DURATION = os.environ.get("LOAD_DURATION", "5m")
WARMUP_S = int(os.environ.get("LOAD_WARMUP_S", "30"))


def total_duration():
    """Measured duration plus the warm-up whose statistics are dropped (SR-16)."""
    d = DURATION.strip()
    secs = int(d[:-1]) * 60 if d.endswith("m") else int(d[:-1]) if d.endswith("s") else int(d)
    return f"{secs + WARMUP_S}s"
BIG_WRITES = int(os.environ.get("LOAD_BIG_WRITES", "3"))
READ_P95_MS = 300        # Q1
WRITE_P95_MS = 3000      # Q1, files up to 1 MB
BIG_WRITE_S = 30         # Q1, D71
OFFERED = {"reads": float(os.environ.get("LOAD_READS_PER_SECOND", "20")),
           "writes": float(os.environ.get("LOAD_WRITES_PER_SECOND", "1"))}
WRITE_OPS = {"createVocabulary", "createVocabularyVersion", "createVocabularyVersion1MB"}


class Ctx:
    def __init__(self):
        self.run = "seed" + uuid.uuid4().hex[:6]


def headers():
    return {"Authorization": f"Bearer {vm.fetch_token(vm.USERS['ga-admin'])}"}


def post_with_retry(url, parts):
    for _ in range(20):
        r = requests.post(url, headers=headers(), files=vm.multipart(parts), timeout=120)
        if r.status_code == 503:
            time.sleep(float(r.headers.get("Retry-After", "5")))
            continue
        return r
    return r


def seed_one(i):
    name = f"{SEED_PREFIX}{i:05d}"
    ctx = Ctx()
    r = requests.get(f"{vm.API_URL}/vocabularies/{name}", headers=headers(), timeout=60)
    have = r.json().get("version", 0) if r.status_code == 200 else 0
    if have == 0:
        parts = [("name", name, False), ("description", "Seed", False), ("changelog", "Initial version.", False),
                 ("vocabularyFile", vm.turtle_of_size(ctx, SEED_FILE_BYTES), True)]
        r = post_with_retry(f"{vm.API_URL}/vocabularies", parts)
        if r.status_code not in (201, 409):
            return f"{name}: create {r.status_code}"
        have = 1
    for v in range(have + 1, SEED_VERSIONS + 1):
        parts = [("description", f"Seed version {v}", False), ("changelog", f"Seed change {v}", False),
                 ("vocabularyFile", vm.turtle_of_size(ctx, SEED_FILE_BYTES), True)]
        r = post_with_retry(f"{vm.API_URL}/vocabularies/{name}/versions", parts)
        if r.status_code != 201:
            return f"{name}: version {v} {r.status_code}"
    return None


def seed():
    started = time.time()
    with ThreadPoolExecutor(max_workers=SEED_WORKERS) as pool:
        errors = [e for e in pool.map(seed_one, range(SEED_VOCABULARIES)) if e]
    return {"vocabularies": SEED_VOCABULARIES, "versions_each": SEED_VERSIONS, "file_bytes": SEED_FILE_BYTES,
            "seconds": round(time.time() - started, 1), "errors": errors[:20], "error_count": len(errors)}


def run_locust():
    out = os.path.join(REPORTS, "load-locust.json")
    if os.path.exists(out):
        os.remove(out)
    env = dict(os.environ, LOAD_LOCUST_OUT=out, SEED_VOCABULARIES=str(SEED_VOCABULARIES),
               LOAD_SEED_PREFIX=SEED_PREFIX)
    users = int(os.environ.get("LOAD_READ_USERS", "20")) + int(os.environ.get("LOAD_WRITE_USERS", "4"))
    cmd = [os.path.join(os.path.dirname(sys.executable), "locust"), "-f", os.path.join(HERE, "locustfile.py"),
           "--headless", "-u", str(users), "-r", str(users), "-t", total_duration(), "--only-summary",
           "--stop-timeout", "30"]
    subprocess.run(cmd, env=env, cwd=ROOT)
    with open(out) as fh:
        return json.load(fh)


def big_writes():
    ctx = Ctx()
    content = vm.turtle_of_size(ctx, vm.FILE_LIMIT)
    times, errors = [], 0
    for i in range(BIG_WRITES):
        name = f"Loadbig{uuid.uuid4().hex[:12]}"
        parts = [("name", name, False), ("description", "5 MB write", False), ("changelog", "Initial version.", False),
                 ("vocabularyFile", content, True)]
        start = time.time()
        r = requests.post(f"{vm.API_URL}/vocabularies", headers=headers(), files=vm.multipart(parts), timeout=120)
        times.append(time.time() - start)
        if r.status_code != 201:
            errors += 1
    times.sort()
    pct = lambda q: times[min(len(times) - 1, int(round(q * (len(times) - 1))))] * 1000 if times else None  # noqa: E731
    return {"createVocabulary5MB": {"requests": len(times), "failures": errors,
                                    "error_rate": errors / len(times) if times else None,
                                    "p50_ms": pct(0.5), "p95_ms": pct(0.95), "p99_ms": pct(0.99),
                                    "target_ms": BIG_WRITE_S * 1000,
                                    "met": bool(times) and errors == 0 and max(times) < BIG_WRITE_S}}


def main():
    os.makedirs(REPORTS, exist_ok=True)
    report = {"started": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "api": vm.API_URL,
              "targets": {"read_p95_ms": READ_P95_MS, "write_p95_ms": WRITE_P95_MS, "big_write_s": BIG_WRITE_S,
                          "offered_reads_per_s": OFFERED["reads"], "offered_writes_per_s": OFFERED["writes"],
                          "achieved_min_ratio": 0.95, "duration": DURATION, "warmup_s": WARMUP_S,
                          "write_sizes": "10 KB and 1 MB (createVocabularyVersion1MB reported apart)"}}
    try:
        report["seed"] = seed() if SEED else {"skipped": True}
        mix = run_locust()
        ops = mix["operations"]
        for name, o in ops.items():
            target = WRITE_P95_MS if name in WRITE_OPS else READ_P95_MS
            o["target_p95_ms"] = target
            o["met"] = o["p95_ms"] is not None and o["p95_ms"] <= target and o["failures"] == 0
        reads = sum(o["rate_per_s"] for n, o in ops.items() if n not in WRITE_OPS)
        writes = sum(o["rate_per_s"] for n, o in ops.items() if n in WRITE_OPS)
        report["operations"] = ops
        report["throughput"] = {
            "achieved_reads_per_s": reads, "achieved_writes_per_s": writes,
            "met": reads >= 0.95 * OFFERED["reads"] and writes >= 0.95 * OFFERED["writes"]
            and all(o["failures"] == 0 for o in ops.values())}
        report["operations"].update(big_writes())
        report["q1_met"] = report["throughput"]["met"] and all(o.get("met") for o in report["operations"].values())
    except Exception as exc:  # the report still says what happened
        report["error"] = repr(exc)
    with open(os.path.join(REPORTS, "load.json"), "w") as fh:
        json.dump(report, fh, indent=2)
    print(json.dumps({k: report.get(k) for k in ("q1_met", "throughput", "error")}, indent=2))


if __name__ == "__main__":
    main()
