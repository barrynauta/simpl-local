"""Load test for the quality targets Q1 (response time, throughput) at the design load of Q4.

Run with ./run.sh load (step 70). Not part of the default run.

Load (Q1, Q4): 20 concurrent Governance Authority users; 20 reads per second
and 1 write per second sustained. 19 reader users and 1 writer user. Reads
are spread over the read operations; writes create vocabularies and new
versions with files up to 1 MB, plus an occasional create with a file at the
5 MB limit (D71).

Targets (Q1): reads p95 <= 300 ms; create or new version p95 <= 3 s for files
up to 1 MB; create at 5 MB <= 30 s; no errors.

Output: reports/load.json with p50, p95, p99 (ms), request count, error
rate and throughput per operation, the target per operation and whether it
was met.

Environment (besides the suite's, see README):
  LOAD_SEED_VOCABULARIES  vocabularies that must exist before the run (default 1000, Q4)
  LOAD_SEED_VERSIONS      versions of the one deep vocabulary (default 20, Q4)
  LOAD_READS_PER_SECOND   default 20
  LOAD_WRITES_PER_SECOND  default 1
  LOAD_BIG_EVERY          one 5 MB create every N writes (default 30)
"""
import json
import os
import random
import sys
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from locust import HttpUser, constant_throughput, events, task

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from vmtest import client, config  # noqa: E402

REPORT = Path(__file__).resolve().parents[1] / "reports" / "load.json"
SEED_COUNT = int(os.environ.get("LOAD_SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(os.environ.get("LOAD_SEED_VERSIONS", "20"))
READS_PER_SECOND = float(os.environ.get("LOAD_READS_PER_SECOND", "20"))
WRITES_PER_SECOND = float(os.environ.get("LOAD_WRITES_PER_SECOND", "1"))
BIG_EVERY = int(os.environ.get("LOAD_BIG_EVERY", "30"))
READERS = 19
RUN = "Ld" + client.random_token(6)
SEED_PREFIX = "LoadSeed"
DEEP = "LoadDeepVocabulary"
EXT_NAME = "LoadExternalTerms"
EXT_NS = "http://load.example.org/terms"

READ_TARGET_MS = 300
WRITE_TARGET_MS = 3000
BIG_WRITE_TARGET_MS = 30000

TARGETS = {
    "getVocabularies": READ_TARGET_MS,
    "getVocabulary": READ_TARGET_MS,
    "getVocabularyVersions": READ_TARGET_MS,
    "getVocabularyVersion": READ_TARGET_MS,
    "downloadVocabulary": READ_TARGET_MS,
    "downloadVocabularyVersion": READ_TARGET_MS,
    "searchVocabularies": READ_TARGET_MS,
    "getExternalVocabularies": READ_TARGET_MS,
    "getExternalVocabulary": READ_TARGET_MS,
    "searchExternalVocabularies": READ_TARGET_MS,
    "downloadExternalVocabularyContent": READ_TARGET_MS,
    "getExternalVocabularyContentByName": READ_TARGET_MS,
    "createVocabulary": WRITE_TARGET_MS,
    "createVocabularyVersion": WRITE_TARGET_MS,
    "createVocabulary5MB": BIG_WRITE_TARGET_MS,
}


def _seed_name(i):
    return "%s%04d" % (SEED_PREFIX, i)


def _seed():
    """Top up the design-load data set (Q4). Existing data is reused (409 = present)."""
    total = client.request("GET", "/vocabularies?name=%s&pageSize=1" % SEED_PREFIX).json().get("total", 0)
    if total < SEED_COUNT:
        def one(i):
            client.create_vocabulary(_seed_name(i))
        with ThreadPoolExecutor(max_workers=8) as pool:
            list(pool.map(one, range(SEED_COUNT)))
    resp = client.request("GET", "/vocabularies/%s/versions?pageSize=1" % DEEP)
    have = resp.json().get("total", 0) if resp.status_code == 200 else 0
    if have == 0:
        client.create_vocabulary(DEEP)
        have = 1
    for _ in range(SEED_VERSIONS - have):
        client.create_version(DEEP)
    client.request(
        "POST", "/external-vocabularies",
        files=[("name", (None, EXT_NAME)), ("namespaceUri", (None, EXT_NS)),
               ("vocabularyFile", ("t.ttl", b"@prefix t: <http://load.example.org/terms/> .\nt:a t:b t:c .\n", "text/turtle"))],
    )


@events.test_start.add_listener
def on_test_start(environment, **_kwargs):
    started = time.time()
    _seed()
    print("load: seed ready in %.0f s" % (time.time() - started))


def _auth():
    return {"Authorization": "Bearer %s" % client.token("ga-admin")}


class Reader(HttpUser):
    weight = READERS
    wait_time = constant_throughput(READS_PER_SECOND / READERS)

    def _get(self, path, name, **kw):
        self.client.get(path, headers=_auth(), name=name, **kw)

    @task(4)
    def get_vocabularies(self):
        self._get("/vocabularies?name=%s&page=%d" % (SEED_PREFIX, random.randint(1, max(1, SEED_COUNT // 20))), "getVocabularies")

    @task(3)
    def get_vocabulary(self):
        self._get("/vocabularies/%s" % _seed_name(random.randrange(SEED_COUNT)), "getVocabulary")

    @task(2)
    def get_versions(self):
        self._get("/vocabularies/%s/versions" % DEEP, "getVocabularyVersions")

    @task(2)
    def get_version(self):
        self._get("/vocabularies/%s/versions/%d" % (DEEP, random.randint(1, SEED_VERSIONS)), "getVocabularyVersion")

    @task(2)
    def download_latest(self):
        self._get("/content/%s" % _seed_name(random.randrange(SEED_COUNT)), "downloadVocabulary")

    @task(1)
    def download_version(self):
        self._get("/content/%s/versions/%d" % (DEEP, random.randint(1, SEED_VERSIONS)), "downloadVocabularyVersion")

    @task(3)
    def search(self):
        items = [{"name": _seed_name(random.randrange(SEED_COUNT))} for _ in range(10)]
        items.append({"name": DEEP, "version": 1})
        self.client.post("/vocabularies/searches", json={"items": items}, headers=_auth(), name="searchVocabularies")

    @task(1)
    def external_list(self):
        self._get("/external-vocabularies", "getExternalVocabularies")

    @task(1)
    def external_get(self):
        self._get("/external-vocabularies/%s" % EXT_NAME, "getExternalVocabulary")

    @task(2)
    def external_search(self):
        self.client.post("/external-vocabulary/searches", json={"namespaceUris": [EXT_NS, "http://none.example.org/x"]},
                         headers=_auth(), name="searchExternalVocabularies")

    @task(1)
    def external_content(self):
        self.client.post("/external-vocabularies/content", json={"namespaceUri": EXT_NS + "/"},
                         headers=_auth(), name="downloadExternalVocabularyContent")

    @task(1)
    def external_content_by_name(self):
        self._get("/external-vocabularies/%s/content" % EXT_NAME, "getExternalVocabularyContentByName")


class Writer(HttpUser):
    weight = 1
    fixed_count = 1
    wait_time = constant_throughput(WRITES_PER_SECOND)

    def on_start(self):
        self.n = 0
        self.own = []

    def _create(self, size, label):
        name = "%s%s%05d" % (RUN, client.random_token(3), self.n)
        content = client.turtle_of_size(name, size) if size else client.default_turtle(name)
        with self.client.post(
            "/vocabularies", headers=_auth(), name=label, catch_response=True,
            data={"name": name, "description": "Load %s" % name, "changelog": "Initial version."},
            files={"vocabularyFile": ("v.ttl", content, "text/turtle")},
        ) as resp:
            if resp.status_code != 201:
                resp.failure("status %s" % resp.status_code)
            else:
                self.own.append(name)

    @task
    def write(self):
        self.n += 1
        if BIG_EVERY and self.n % BIG_EVERY == 0:
            self._create(5242880, "createVocabulary5MB")
        elif not self.own or self.n % 2:
            self._create(1048576 if self.n % 10 == 1 else 0, "createVocabulary")
        else:
            name = random.choice(self.own)
            marker = client.random_token(6)
            size = 1048576 if self.n % 10 == 0 else 0
            content = client.turtle_of_size(name + marker, size) if size else client.default_turtle(name, marker)
            with self.client.post(
                "/vocabularies/%s/versions" % name, headers=_auth(), name="createVocabularyVersion", catch_response=True,
                data={"description": "Load %s" % marker, "changelog": "Change %s" % marker},
                files={"vocabularyFile": ("v.ttl", content, "text/turtle")},
            ) as resp:
                if resp.status_code != 201:
                    resp.failure("status %s" % resp.status_code)


@events.quitting.add_listener
def write_report(environment, **_kwargs):
    stats = environment.stats
    duration = max(1.0, (stats.last_request_timestamp or time.time()) - (stats.start_time or time.time()))
    operations = {}
    for (name, method), entry in stats.entries.items():
        n = entry.num_requests
        target = TARGETS.get(name)
        p95 = entry.get_response_time_percentile(0.95) if n else None
        error_rate = (entry.num_failures / n) if n else None
        operations[name] = {
            "method": method,
            "requests": n,
            "failures": entry.num_failures,
            "p50_ms": entry.get_response_time_percentile(0.50) if n else None,
            "p95_ms": p95,
            "p99_ms": entry.get_response_time_percentile(0.99) if n else None,
            "max_ms": entry.max_response_time if n else None,
            "error_rate": error_rate,
            "rps": n / duration,
            "target_p95_ms": target,
            "target_met": (n > 0 and p95 is not None and target is not None and p95 <= target and entry.num_failures == 0),
        }
    reads = sum(v["requests"] for k, v in operations.items() if TARGETS.get(k) == READ_TARGET_MS)
    writes = sum(v["requests"] for k, v in operations.items() if TARGETS.get(k, 0) >= WRITE_TARGET_MS)
    report = {
        "targets": {"Q1": {"read_p95_ms": READ_TARGET_MS, "write_p95_ms_up_to_1MB": WRITE_TARGET_MS,
                           "write_ms_at_5MB": BIG_WRITE_TARGET_MS, "reads_per_second": READS_PER_SECOND,
                           "writes_per_second": WRITES_PER_SECOND, "errors": 0},
                    "Q4": {"users": environment.runner.user_count if environment.runner else None,
                           "seed_vocabularies": SEED_COUNT, "seed_versions_of_deep_vocabulary": SEED_VERSIONS}},
        "duration_s": duration,
        "throughput": {"reads_per_second": reads / duration, "writes_per_second": writes / duration},
        "operations": operations,
        "total": {
            "requests": stats.total.num_requests,
            "failures": stats.total.num_failures,
            "error_rate": (stats.total.num_failures / stats.total.num_requests) if stats.total.num_requests else None,
        },
        "api_url": config.API_URL,
    }
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(report, indent=2))
    print("load: report written to %s" % REPORT)
