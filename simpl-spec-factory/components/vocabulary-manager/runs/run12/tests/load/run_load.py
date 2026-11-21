"""Q1 load test of the Vocabulary Manager (Q1, Q4, D71, D99, D136, NFR-03).

Phases:
  1. Seed (D99): LOAD_SEED_VOCABULARIES vocabularies (default 1000) with
     LOAD_SEED_VERSIONS versions each (default 20), files of about 10 KB.
     Idempotent: an existing seed with the same LOAD_SEED_ID is topped up, not repeated.
  2. Throughput mix (Q1, D99): offered rate 20 reads/s and 1 write/s for LOAD_DURATION
     seconds (default 300), files up to 1 MB on writes.
  3. Big writes (D71): LOAD_BIG_WRITES (default 3) creates with a 5 MB file, one at a time.

Writes reports/load.json with p50, p95, p99, error rate and achieved rate per operation,
the Q1 targets and whether each one is met.
"""
import gevent.monkey  # noqa: E402  (locust needs this before other imports)

gevent.monkey.patch_all()

import argparse  # noqa: E402
import json  # noqa: E402
import os  # noqa: E402
import random  # noqa: E402
import sys  # noqa: E402
import time  # noqa: E402
from concurrent.futures import ThreadPoolExecutor  # noqa: E402

import gevent  # noqa: E402
from locust import HttpUser, constant_throughput, events, task  # noqa: E402
from locust.env import Environment  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(HERE), "lib"))
import vmclient  # noqa: E402

SEED_ID = vmclient.env("LOAD_SEED_ID", "Q4")
SEED_VOCABS = int(vmclient.env("LOAD_SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(vmclient.env("LOAD_SEED_VERSIONS", "20"))
SEED_THREADS = min(4, int(vmclient.env("LOAD_SEED_THREADS", "4")))  # D92: at most 4 uploads per instance
WRITERS = 4  # open model for writes (D99): up to 4 uploads in flight, within D92
SKIP_SEED = vmclient.env("LOAD_SKIP_SEED", "0") == "1"
DURATION = int(vmclient.env("LOAD_DURATION", "300"))
READ_RATE = int(vmclient.env("LOAD_READ_RATE", "20"))
WRITE_RATE = int(vmclient.env("LOAD_WRITE_RATE", "1"))
BIG_WRITES = int(vmclient.env("LOAD_BIG_WRITES", "3"))
WARMUP = int(vmclient.env("LOAD_WARMUP", "30"))

READ_OPS = ["getVocabularies", "getVocabulary", "getVocabularyVersions", "getVocabularyVersion",
            "downloadVocabulary", "downloadVocabularyVersion", "searchVocabularies",
            "getExternalVocabularies", "searchExternalVocabularies"]
WRITE_OPS = ["createVocabulary", "createVocabularyVersion"]
TARGET_MS = {op: 300 for op in READ_OPS}
TARGET_MS.update({op: 3000 for op in WRITE_OPS})
BIG_TARGET_MS = 30000


def seed_name(index):
    return f"Seed{SEED_ID}V{index:05d}"


def ten_kb(tag):
    return vmclient.turtle_of_size(10 * 1024, f"seed-{tag}")


def upload(path, parts, attempts=8):
    """POST an upload; retry a D92/D44 503 after its Retry-After. Seed only."""
    for _ in range(attempts):
        r = vmclient.send("POST", path, token=vmclient.fetch_token("ga-admin"), files=parts,
                          timeout=vmclient.UPLOAD_TIMEOUT)
        if r.status_code != 503:
            return r
        time.sleep(float(r.headers.get("Retry-After") or 5))
    return r


def seed_one(index):
    token = vmclient.fetch_token("ga-admin")
    name = seed_name(index)
    resp = vmclient.send("GET", f"/vocabularies/{name}", token=token)
    have = resp.json()["version"] if resp.status_code == 200 else 0
    if have == 0:
        parts = [("name", (None, name)), ("description", (None, f"Seed {name}")),
                 ("changelog", (None, "Initial version.")),
                 ("vocabularyFile", ("v.ttl", ten_kb(f"{name}-1"), "text/turtle"))]
        r = upload("/vocabularies", parts)
        if r.status_code == 409:  # created by an earlier, interrupted attempt
            r = vmclient.send("GET", f"/vocabularies/{name}", token=token)
            have = r.json()["version"] if r.status_code == 200 else 0
        elif r.status_code != 201:
            return f"{name}: create {r.status_code}"
        else:
            have = 1
    for version in range(have + 1, SEED_VERSIONS + 1):
        parts = [("description", (None, f"Seed {name} v{version}")),
                 ("changelog", (None, f"Seed change {version}")),
                 ("vocabularyFile", ("v.ttl", ten_kb(f"{name}-{version}"), "text/turtle"))]
        r = upload(f"/vocabularies/{name}/versions", parts)
        if r.status_code != 201:
            return f"{name}: version {version} {r.status_code}"
    return None


def seed():
    started = time.time()
    with ThreadPoolExecutor(max_workers=SEED_THREADS) as pool:
        errors = [e for e in pool.map(seed_one, range(SEED_VOCABS)) if e]
    incomplete = check_seed()
    if errors or incomplete:
        raise RuntimeError(f"seed incomplete: {len(errors)} upload errors, {len(incomplete)} vocabularies "
                           f"without {SEED_VERSIONS} versions, for example {(errors + incomplete)[:5]}")
    return {"vocabularies": SEED_VOCABS, "versionsEach": SEED_VERSIONS,
            "seconds": round(time.time() - started, 1), "errors": errors[:20],
            "errorCount": len(errors)}


def check_seed():
    """Every seed vocabulary must have at least SEED_VERSIONS versions before the mix starts."""
    token = vmclient.fetch_token("ga-admin")
    missing = []
    for index in range(SEED_VOCABS):
        r = vmclient.send("GET", f"/vocabularies/{seed_name(index)}", token=token)
        if r.status_code != 200 or r.json().get("version", 0) < SEED_VERSIONS:
            missing.append(seed_name(index))
    return missing


def auth():
    return {"Authorization": "Bearer " + vmclient.fetch_token("ga-admin")}


class Reader(HttpUser):
    wait_time = constant_throughput(1)
    host = vmclient.API_URL

    def path(self, p):
        return vmclient.API_URL + p

    @task
    def read(self):
        op = random.choice(READ_OPS)
        name = seed_name(random.randrange(SEED_VOCABS))
        version = random.randint(1, SEED_VERSIONS)
        h = auth()
        if op == "getVocabularies":
            page = random.randint(1, max(1, SEED_VOCABS // 20))
            self.client.get(self.path(f"/vocabularies?page={page}&pageSize=20&sort=-lastUpdated"),
                            headers=h, name=op)
        elif op == "getVocabulary":
            self.client.get(self.path(f"/vocabularies/{name}"), headers=h, name=op)
        elif op == "getVocabularyVersions":
            self.client.get(self.path(f"/vocabularies/{name}/versions?pageSize=100"), headers=h, name=op)
        elif op == "getVocabularyVersion":
            self.client.get(self.path(f"/vocabularies/{name}/versions/{version}"), headers=h, name=op)
        elif op == "downloadVocabulary":
            self.client.get(self.path(f"/content/{name}"), headers=h, name=op)
        elif op == "downloadVocabularyVersion":
            self.client.get(self.path(f"/content/{name}/versions/{version}"), headers=h, name=op)
        elif op == "searchVocabularies":
            items = [{"name": seed_name(random.randrange(SEED_VOCABS))} for _ in range(10)]
            items.append({"name": name, "version": version})
            self.client.post(self.path("/vocabularies/searches"), json={"items": items}, headers=h, name=op)
        elif op == "getExternalVocabularies":
            self.client.get(self.path("/external-vocabularies?pageSize=20"), headers=h, name=op)
        else:
            self.client.post(self.path("/external-vocabulary/searches"),
                             json={"namespaceUris": ["http://purl.org/dc/terms"]}, headers=h, name=op)


RUN = vmclient.run_id(6)


class Writer(HttpUser):
    # Open model (D99): WRITERS users each start one write every WRITERS/WRITE_RATE seconds,
    # so 1 write/s is offered even when a write takes up to WRITERS seconds (Q1 p95 3 s).
    wait_time = constant_throughput(WRITE_RATE / WRITERS)
    host = vmclient.API_URL
    counter = 0

    @task
    def write(self):
        Writer.counter += 1
        size = 1024 * 1024 if Writer.counter % 10 == 0 else 10 * 1024
        content = vmclient.turtle_of_size(size, f"load-{RUN}-{Writer.counter}")
        h = auth()
        if Writer.counter % 2:
            name = f"Load{RUN}N{Writer.counter:06d}"
            parts = [("name", (None, name)), ("description", (None, "Load test")),
                     ("changelog", (None, "Initial version.")),
                     ("vocabularyFile", ("v.ttl", content, "text/turtle"))]
            self.client.post(vmclient.API_URL + "/vocabularies", files=parts, headers=h,
                             name="createVocabulary")
        else:
            name = seed_name(random.randrange(SEED_VOCABS))
            parts = [("description", (None, f"Load {RUN} {Writer.counter}")),
                     ("changelog", (None, "Load test change")),
                     ("vocabularyFile", ("v.ttl", content, "text/turtle"))]
            self.client.post(vmclient.API_URL + f"/vocabularies/{name}/versions", files=parts,
                             headers=h, name="createVocabularyVersion")


def run_mix():
    Reader.fixed_count = READ_RATE
    Writer.fixed_count = WRITERS
    env = Environment(user_classes=[Reader, Writer], events=events)
    runner = env.create_local_runner()
    runner.start(READ_RATE + WRITERS, spawn_rate=READ_RATE + WRITERS)
    gevent.sleep(WARMUP)
    env.stats.reset_all()
    started = time.time()
    gevent.sleep(DURATION)
    elapsed = time.time() - started
    runner.quit()
    ops = {}
    for (name, _method), entry in env.stats.entries.items():
        n = entry.num_requests
        ops[name] = {
            "requests": n,
            "failures": entry.num_failures,
            "errorRate": round(entry.num_failures / n, 4) if n else None,
            "p50": entry.get_response_time_percentile(0.5) if n else None,
            "p95": entry.get_response_time_percentile(0.95) if n else None,
            "p99": entry.get_response_time_percentile(0.99) if n else None,
            "achievedPerSecond": round(n / elapsed, 3),
            "targetP95Ms": TARGET_MS.get(name),
        }
        ops[name]["p95Met"] = (ops[name]["p95"] is not None and TARGET_MS.get(name) is not None
                               and ops[name]["p95"] <= TARGET_MS[name])
    reads = sum(o["requests"] for k, o in ops.items() if k in READ_OPS) / elapsed
    writes = sum(o["requests"] for k, o in ops.items() if k in WRITE_OPS) / elapsed
    failures = sum(o["failures"] for o in ops.values())
    throughput = {
        "offeredReadsPerSecond": READ_RATE, "achievedReadsPerSecond": round(reads, 3),
        "offeredWritesPerSecond": WRITE_RATE, "achievedWritesPerSecond": round(writes, 3),
        "durationSeconds": round(elapsed, 1), "failures": failures,
        "met": reads >= 0.95 * READ_RATE and writes >= 0.95 * WRITE_RATE and failures == 0,
    }
    return ops, throughput


def run_big_writes():
    times, errors = [], 0
    for index in range(BIG_WRITES):
        content = vmclient.turtle_of_size(5 * 1024 * 1024, f"big-{RUN}-{index}")
        parts = [("name", (None, f"Big{RUN}N{index:03d}")), ("description", (None, "5 MB load")),
                 ("changelog", (None, "Initial version.")),
                 ("vocabularyFile", ("v.ttl", content, "text/turtle"))]
        started = time.time()
        r = vmclient.send("POST", "/vocabularies", token=vmclient.fetch_token("ga-admin"),
                          files=parts, timeout=120)
        times.append(round((time.time() - started) * 1000))
        errors += r.status_code != 201
    times_sorted = sorted(times)

    def pct(p):
        return times_sorted[min(len(times_sorted) - 1, int(p * len(times_sorted)))] if times else None

    return {"createVocabulary5MB": {
        "requests": len(times), "failures": errors,
        "errorRate": round(errors / len(times), 4) if times else None,
        "p50": pct(0.5), "p95": pct(0.95), "p99": pct(0.99), "maxMs": max(times) if times else None,
        "targetMaxMs": BIG_TARGET_MS,
        "met": bool(times) and errors == 0 and max(times) <= BIG_TARGET_MS}}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--report", default="reports/load.json")
    args = parser.parse_args()
    report = {"apiUrl": vmclient.API_URL, "startedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "targets": "Q1 (D99, D136): reads p95 <= 300 ms per operation; writes up to 1 MB p95 <= 3000 ms; "
                         "5 MB write <= 30 s (D71); 20 reads/s and 1 write/s achieved at >= 95 % with no errors"}
    try:
        if SKIP_SEED:
            incomplete = check_seed()
            if incomplete:
                raise RuntimeError(f"LOAD_SKIP_SEED=1 but {len(incomplete)} seed vocabularies are incomplete")
            report["seed"] = {"skipped": True, "checked": True}
        else:
            report["seed"] = seed()
        ops, throughput = run_mix()
        ops.update(run_big_writes())
        report["operations"] = ops
        report["throughput"] = throughput
        report["met"] = throughput["met"] and all(
            o.get("p95Met", o.get("met", True)) for o in ops.values())
    except Exception as exc:  # report what happened instead of crashing without a report
        report["error"] = repr(exc)
    with open(args.report, "w") as fh:
        json.dump(report, fh, indent=2)
    print(json.dumps({k: report.get(k) for k in ("met", "throughput", "error")}, indent=2))
    return 0 if "error" not in report else 1


if __name__ == "__main__":
    sys.exit(main())
