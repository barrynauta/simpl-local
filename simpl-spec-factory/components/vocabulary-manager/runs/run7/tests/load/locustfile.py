"""Load test for the quality targets Q1 (response time and throughput) at the design load of Q4.

Q1: reads (lists, details, searches, downloads under 1 MB) p95 <= 300 ms; create or new version
p95 <= 3 s for files up to 1 MB and <= 30 s at the 5 MB limit (D71); 20 reads/s and 1 write/s
sustained with no errors. Q4: 20 concurrent Governance Authority users, one backend instance.

Run with ./run.sh load (20 users, 5 minutes by default). Writes reports/load.json with
p50, p95, p99 and the error rate per operation, and whether each target was met.
Data is created with a unique run id; the store is never reset (E4).
"""
import json
import os
import random
import sys
import time

import gevent
from locust import HttpUser, constant, constant_throughput, events, task

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from vmlib import client  # noqa: E402

RID = client.new_run_id()
# Q4 design volume: 1000 vocabularies with 20 versions each. The seed is setup, not measured load:
# it runs in parallel and is reused between runs (same LOAD_SEED_ID), so only missing data is added.
SEED_ID = os.environ.get("LOAD_SEED_ID", "q4seed")
SEED_VOCABULARIES = int(os.environ.get("LOAD_SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(os.environ.get("LOAD_SEED_VERSIONS", "20"))
SEED_PARALLEL = int(os.environ.get("LOAD_SEED_PARALLEL", "16"))
Q4_VOCABULARIES, Q4_VERSIONS = 1000, 20
BIG_WRITES = int(os.environ.get("LOAD_5MB_WRITES", "3"))
BIG_INTERVAL = float(os.environ.get("LOAD_5MB_INTERVAL", "60"))
WRITE_TARGETS = 10  # per-run vocabularies that take the 1 MB versions; readers never read them
REPORT = os.path.join(os.path.dirname(__file__), "..", "reports", "load.json")

READ_P95_MS = 300
WRITE_1MB_P95_MS = 3000
WRITE_5MB_P95_MS = 30000

# operation name -> (kind, p95 target in ms)
TARGETS = {
    "GET /vocabularies": ("read", READ_P95_MS),
    "GET /vocabularies/{vocabularyName}": ("read", READ_P95_MS),
    "GET /vocabularies/{vocabularyName}/versions": ("read", READ_P95_MS),
    "GET /vocabularies/{vocabularyName}/versions/{version}": ("read", READ_P95_MS),
    "GET /content/{vocabularyName}": ("read", READ_P95_MS),
    "GET /content/{vocabularyName}/versions/{version}": ("read", READ_P95_MS),
    "POST /vocabularies/searches": ("read", READ_P95_MS),
    "GET /external-vocabularies": ("read", READ_P95_MS),
    "POST /external-vocabulary/searches": ("read", READ_P95_MS),
    "POST /external-vocabularies/content": ("read", READ_P95_MS),
    "POST /vocabularies [<=1 MB]": ("write", WRITE_1MB_P95_MS),
    "POST /vocabularies/{vocabularyName}/versions [<=1 MB]": ("write", WRITE_1MB_P95_MS),
    "POST /vocabularies/{vocabularyName}/versions [5 MB]": ("write5mb", WRITE_5MB_P95_MS),
}

SEEDED = []  # vocabulary names with SEED_VERSIONS versions
EXT_NS = f"http://example.org/{RID}/load/ext"


def name_of(i):
    return f"V{SEED_ID}L{i}"


def target_of(k):
    return f"V{RID}T{k}"


BIG_TARGET = f"V{RID}Big"


def _multipart(name, desc, data, changelog="Initial version.", with_name=True):
    parts = []
    if with_name:
        parts.append(("name", (None, name)))
    parts += [
        ("description", (None, desc)),
        ("changelog", (None, changelog)),
        ("vocabularyFile", ("vocabulary.ttl", data, "text/turtle")),
    ]
    return parts


def _duration_s():
    text = os.environ.get("LOAD_DURATION", "5m").strip()
    units = {"s": 1, "m": 60, "h": 3600}
    if text[-1] in units:
        return float(text[:-1]) * units[text[-1]]
    return float(text)


@events.test_start.add_listener
def seed(environment, **_kw):
    """Seed data and warm up before the measured run."""
    host = environment.host.rstrip("/")

    def seed_one(i):
        name = name_of(i)
        r = client.call("GET", f"/vocabularies/{name}", "ga-admin", base_url=host)
        have = r.json().get("version", 0) if r.status_code == 200 else 0
        if have == 0:
            r = client.call("POST", "/vocabularies", "ga-admin", base_url=host,
                            files=_multipart(name, f"Seed {i}", client.valid_ttl(SEED_ID, f"L{i}", "1")))
            if r.status_code != 201:
                print(f"seed create failed {r.status_code} {r.text[:200]}")
                return None
            have = 1
        for v in range(have + 1, SEED_VERSIONS + 1):
            client.call("POST", f"/vocabularies/{name}/versions", "ga-admin", base_url=host,
                        files=_multipart(name, f"Seed {i} v{v}", client.valid_ttl(SEED_ID, f"L{i}", str(v)),
                                         changelog=f"Seed v{v}.", with_name=False))
        return name

    from concurrent.futures import ThreadPoolExecutor
    with ThreadPoolExecutor(max_workers=SEED_PARALLEL) as pool:
        SEEDED.extend(n for n in pool.map(seed_one, range(SEED_VOCABULARIES)) if n)
    for name in [target_of(k) for k in range(WRITE_TARGETS)] + [BIG_TARGET]:
        client.call("POST", "/vocabularies", "ga-admin", base_url=host,
                    files=_multipart(name, "Write target", client.valid_ttl(RID, name, "1")))
    client.call("POST", "/external-vocabularies", "ga-admin", base_url=host, files=[
        ("name", (None, f"E{RID}Load")),
        ("namespaceUri", (None, EXT_NS)),
        ("vocabularyFile", ("external.ttl", client.valid_ttl(RID, "Ext", "1"), "text/turtle")),
    ])
    for _ in range(30):  # warm-up, not measured
        client.call("GET", f"/vocabularies?name={SEED_ID}", "ga-viewer", base_url=host)
    environment.stats.reset_all()
    environment.runner.started_at = time.time()
    # The measured run lasts LOAD_DURATION after the seed, however long the seed took.
    gevent.spawn_later(_duration_s(), environment.runner.quit)


class Base(HttpUser):
    abstract = True

    def auth(self, user):
        return {"Authorization": "Bearer " + client.get_token(user)}

    def pick(self):
        return random.choice(SEEDED) if SEEDED else name_of(0)


class Reader(Base):
    """Read traffic: 18 users at about 20 requests per second together."""
    weight = 18
    wait_time = constant_throughput(20 / 18)

    @task(4)
    def list_vocabularies(self):
        self.client.get(f"/vocabularies?name={SEED_ID}&pageSize=20", headers=self.auth("ga-viewer"),
                        name="GET /vocabularies")

    @task(3)
    def get_vocabulary(self):
        self.client.get(f"/vocabularies/{self.pick()}", headers=self.auth("ga-viewer"),
                        name="GET /vocabularies/{vocabularyName}")

    @task(2)
    def get_versions(self):
        self.client.get(f"/vocabularies/{self.pick()}/versions", headers=self.auth("ga-viewer"),
                        name="GET /vocabularies/{vocabularyName}/versions")

    @task(2)
    def get_version(self):
        self.client.get(f"/vocabularies/{self.pick()}/versions/1", headers=self.auth("ga-viewer"),
                        name="GET /vocabularies/{vocabularyName}/versions/{version}")

    @task(2)
    def download_latest(self):
        self.client.get(f"/content/{self.pick()}", headers=self.auth("ga-viewer"),
                        name="GET /content/{vocabularyName}")

    @task(1)
    def download_version(self):
        self.client.get(f"/content/{self.pick()}/versions/1", headers=self.auth("ga-viewer"),
                        name="GET /content/{vocabularyName}/versions/{version}")

    @task(3)
    def search(self):
        items = [{"name": self.pick()} for _ in range(5)] + [{"name": self.pick(), "version": 1} for _ in range(5)]
        self.client.post("/vocabularies/searches", json={"items": items}, headers=self.auth("ga-viewer"),
                         name="POST /vocabularies/searches")

    @task(1)
    def external_list(self):
        self.client.get("/external-vocabularies?pageSize=20", headers=self.auth("ga-viewer"),
                        name="GET /external-vocabularies")

    @task(2)
    def external_search(self):
        self.client.post("/external-vocabulary/searches", json={"namespaceUris": [EXT_NS, EXT_NS + "/x"]},
                         headers=self.auth("ga-viewer"), name="POST /external-vocabulary/searches")

    @task(1)
    def external_content(self):
        self.client.post("/external-vocabularies/content", json={"namespaceUri": EXT_NS + "#"},
                         headers=self.auth("ga-viewer"), name="POST /external-vocabularies/content")


class Writer(Base):
    """Write traffic with an open arrival rate: one write starts every second, whatever the
    latency of earlier writes (Q1 '1 write per second sustained'). Writes go to per-run
    vocabularies that the readers never read."""
    fixed_count = 1
    wait_time = constant(1)
    counter = 0

    def _post(self, path, parts, name):
        self.client.post(path, headers=self.auth("ga-admin"), files=parts, name=name, timeout=60)

    @task
    def tick(self):
        Writer.counter += 1
        n = Writer.counter
        if n % 2:
            data = client.valid_ttl(RID, f"N{n}", "1")
            gevent.spawn(self._post, "/vocabularies", _multipart(f"V{RID}N{n}", f"Load {n}", data),
                         "POST /vocabularies [<=1 MB]")
        else:
            target = target_of(n % WRITE_TARGETS)
            data = client.sized_ttl(RID, target, 1024 * 1024 - 64, f"w{n}")
            gevent.spawn(self._post, f"/vocabularies/{target}/versions",
                         _multipart(None, f"Load {n}", data, changelog=f"Load {n}.", with_name=False),
                         "POST /vocabularies/{vocabularyName}/versions [<=1 MB]")


class BigWriter(Base):
    """The 5 MB writes (D71) on their own user and their own vocabulary, outside the throughput count."""
    fixed_count = 1
    wait_time = constant(BIG_INTERVAL)
    done = 0

    @task
    def big(self):
        if BigWriter.done >= BIG_WRITES:
            return
        BigWriter.done += 1
        n = BigWriter.done
        data = client.sized_ttl(RID, BIG_TARGET, client.FIVE_MB, f"big{n}")
        self.client.post(f"/vocabularies/{BIG_TARGET}/versions", headers=self.auth("ga-admin"),
                         files=_multipart(None, f"Big {n}", data, changelog=f"Big {n}.", with_name=False),
                         name="POST /vocabularies/{vocabularyName}/versions [5 MB]", timeout=120)


@events.quitting.add_listener
def write_report(environment, **_kw):
    stats = environment.stats
    duration = max(time.time() - (getattr(environment.runner, "started_at", None) or time.time()), 1)
    ops = {}
    reads = writes = 0
    for (name, method), entry in stats.entries.items():
        if entry.num_requests == 0:
            continue
        kind, target = TARGETS.get(name, ("other", None))
        p95 = entry.get_response_time_percentile(0.95)
        error_rate = entry.num_failures / entry.num_requests
        ops[name] = {
            "method": method,
            "kind": kind,
            "requests": entry.num_requests,
            "failures": entry.num_failures,
            "error_rate": round(error_rate, 4),
            "p50_ms": entry.get_response_time_percentile(0.50),
            "p95_ms": p95,
            "p99_ms": entry.get_response_time_percentile(0.99),
            "rps": round(entry.num_requests / duration, 2),
            "target_p95_ms": target,
            "meets_target": (target is None or p95 <= target) and entry.num_failures == 0,
        }
        if kind == "read":
            reads += entry.num_requests
        elif kind == "write":
            writes += entry.num_requests
    report = {
        "run_id": RID,
        "host": environment.host,
        "users": environment.runner.user_count if environment.runner else None,
        "duration_s": round(duration, 1),
        "seed": {"seed_id": SEED_ID, "vocabularies": len(SEEDED), "versions_each": SEED_VERSIONS},
        "measured_at_q4_volume": len(SEEDED) >= Q4_VOCABULARIES and SEED_VERSIONS >= Q4_VERSIONS,
        "targets": {
            "Q1_read_p95_ms": READ_P95_MS,
            "Q1_write_1mb_p95_ms": WRITE_1MB_P95_MS,
            "Q1_write_5mb_p95_ms": WRITE_5MB_P95_MS,
            "Q1_reads_per_s": 20,
            "Q1_writes_per_s": 1,
        },
        "throughput": {
            "reads_per_s": round(reads / duration, 2),
            "writes_per_s": round(writes / duration, 2),
            "meets_read_throughput": reads / duration >= 19.0,
            "meets_write_throughput": writes / duration >= 0.95,
        },
        "operations": ops,
    }
    report["meets_all_targets"] = (
        report["measured_at_q4_volume"]
        and all(o["meets_target"] for o in ops.values())
        and report["throughput"]["meets_read_throughput"]
        and report["throughput"]["meets_write_throughput"]
    )
    os.makedirs(os.path.dirname(REPORT), exist_ok=True)
    with open(REPORT, "w") as f:
        json.dump(report, f, indent=2)
