"""Load test for Q1 (D99): an offered rate of 20 reads per second and 1 write per second.

Reads: lists, details, version list, version, searches, downloads (Q1: p95 <= 300 ms).
Writes: create and new version with files up to 1 MB (Q1: p95 <= 3 s).
Each request is named by its operationId, so the report has one row per operation.
Started by run_load.py; per-operation results go to reports/load-locust.json.
"""
import json
import os
import random
import sys
import time
import uuid

from locust import HttpUser, task, constant_throughput, events

sys.path.insert(0, os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "lib")))
import vmtest as vm  # noqa: E402

SEED_PREFIX = os.environ.get("LOAD_SEED_PREFIX", "Loadseed")
SEED_VOCABULARIES = int(os.environ.get("SEED_VOCABULARIES", "1000"))
READ_USERS = int(os.environ.get("LOAD_READ_USERS", "20"))
WRITE_USERS = int(os.environ.get("LOAD_WRITE_USERS", "4"))
READS_PER_SECOND = float(os.environ.get("LOAD_READS_PER_SECOND", "20"))
WRITES_PER_SECOND = float(os.environ.get("LOAD_WRITES_PER_SECOND", "1"))
WRITE_FILE_BYTES = int(os.environ.get("LOAD_WRITE_FILE_BYTES", "10240"))
LARGE_WRITE_BYTES = int(os.environ.get("LOAD_LARGE_WRITE_BYTES", str(1024 * 1024)))  # Q1: writes up to 1 MB
WARMUP_S = float(os.environ.get("LOAD_WARMUP_S", "30"))
OUT = os.environ.get("LOAD_LOCUST_OUT", os.path.join(os.path.dirname(__file__), "..", "reports", "load-locust.json"))
API_PATH = "/" + vm.API_URL.split("/", 3)[3] if vm.API_URL.count("/") >= 3 else ""


def seed_name(i):
    return f"{SEED_PREFIX}{i:05d}"


class _Ctx:
    """Minimal context for the vmtest Turtle generators."""
    def __init__(self):
        self.run = "load" + uuid.uuid4().hex[:6]


class Base(HttpUser):
    abstract = True
    host = vm.API_URL.rsplit(API_PATH, 1)[0] if API_PATH else vm.API_URL

    def on_start(self):
        self.ctx = _Ctx()
        self.token = vm.fetch_token(vm.USERS["ga-admin"])
        self.token_time = time.time()

    def headers(self):
        if time.time() - self.token_time > 120:
            self.token = vm.fetch_token(vm.USERS["ga-admin"])
            self.token_time = time.time()
        return {"Authorization": f"Bearer {self.token}"}

    def p(self, path):
        return API_PATH + path


class Reader(HttpUser):
    """20 reads per second in total: READ_USERS users, each at READS_PER_SECOND / READ_USERS."""
    abstract = False
    fixed_count = READ_USERS
    wait_time = constant_throughput(READS_PER_SECOND / max(READ_USERS, 1))
    host = Base.host
    on_start = Base.on_start
    headers = Base.headers
    p = Base.p

    def name(self):
        return seed_name(random.randrange(SEED_VOCABULARIES))

    @task(3)
    def getVocabularies(self):
        page = random.randint(1, max(1, SEED_VOCABULARIES // 20))
        self.client.get(self.p(f"/vocabularies?page={page}&pageSize=20&sort=name"), headers=self.headers(),
                        name="getVocabularies")

    @task(2)
    def getVocabulary(self):
        self.client.get(self.p(f"/vocabularies/{self.name()}"), headers=self.headers(), name="getVocabulary")

    @task(2)
    def getVocabularyVersions(self):
        self.client.get(self.p(f"/vocabularies/{self.name()}/versions"), headers=self.headers(),
                        name="getVocabularyVersions")

    @task(2)
    def getVocabularyVersion(self):
        self.client.get(self.p(f"/vocabularies/{self.name()}/versions/1"), headers=self.headers(),
                        name="getVocabularyVersion")

    @task(3)
    def searchVocabularies(self):
        items = [{"name": self.name()} for _ in range(5)] + [{"name": self.name(), "version": 1}]
        self.client.post(self.p("/vocabularies/searches"), json={"items": items}, headers=self.headers(),
                         name="searchVocabularies")

    @task(1)
    def searchExternalVocabularies(self):
        self.client.post(self.p("/external-vocabulary/searches"),
                         json={"namespaceUris": ["http://purl.org/dc/terms", "https://schema.org"]},
                         headers=self.headers(), name="searchExternalVocabularies")

    @task(1)
    def getExternalVocabularies(self):
        self.client.get(self.p("/external-vocabularies"), headers=self.headers(), name="getExternalVocabularies")

    @task(2)
    def downloadVocabulary(self):
        self.client.get(self.p(f"/content/{self.name()}"), headers=self.headers(), name="downloadVocabulary")

    @task(2)
    def downloadVocabularyVersion(self):
        self.client.get(self.p(f"/content/{self.name()}/versions/1"), headers=self.headers(),
                        name="downloadVocabularyVersion")


class Writer(HttpUser):
    """1 write per second in total: WRITE_USERS users (writes may take up to 3 s each)."""
    abstract = False
    fixed_count = WRITE_USERS
    wait_time = constant_throughput(WRITES_PER_SECOND / max(WRITE_USERS, 1))
    host = Base.host
    on_start = Base.on_start
    headers = Base.headers
    p = Base.p

    def content(self):
        if WRITE_FILE_BYTES <= 2048:
            return vm.valid_turtle(self.ctx)
        return vm.turtle_of_size(self.ctx, WRITE_FILE_BYTES)

    @task(1)
    def createVocabulary(self):
        name = f"Loadw{uuid.uuid4().hex[:16]}"
        parts = [("name", name, False), ("description", "Load test", False), ("changelog", "Initial version.", False),
                 ("vocabularyFile", self.content(), True)]
        self.client.post(self.p("/vocabularies"), files=vm.multipart(parts), headers=self.headers(),
                         name="createVocabulary")

    @task(1)
    def createVocabularyVersion1MB(self):
        """A share of the writes at 1 MB, reported on its own (Q1: p95 <= 3 s up to 1 MB)."""
        name = seed_name(random.randrange(SEED_VOCABULARIES))
        parts = [("description", f"Load 1MB {uuid.uuid4().hex[:8]}", False), ("changelog", "Load change", False),
                 ("vocabularyFile", vm.turtle_of_size(self.ctx, LARGE_WRITE_BYTES), True)]
        self.client.post(self.p(f"/vocabularies/{name}/versions"), files=vm.multipart(parts), headers=self.headers(),
                         name="createVocabularyVersion1MB")

    @task(2)
    def createVocabularyVersion(self):
        name = seed_name(random.randrange(SEED_VOCABULARIES))
        parts = [("description", f"Load {uuid.uuid4().hex[:8]}", False), ("changelog", "Load change", False),
                 ("vocabularyFile", self.content(), True)]
        self.client.post(self.p(f"/vocabularies/{name}/versions"), files=vm.multipart(parts), headers=self.headers(),
                         name="createVocabularyVersion")


@events.test_start.add_listener
def schedule_warmup_reset(environment, **kwargs):
    """Q1 is measured after warm-up: statistics of the first LOAD_WARMUP_S seconds are dropped."""
    import gevent

    def reset():
        gevent.sleep(WARMUP_S)
        environment.stats.reset_all()

    if WARMUP_S > 0:
        gevent.spawn(reset)


@events.quitting.add_listener
def write_results(environment, **kwargs):
    stats = environment.stats
    duration = max(1e-6, (stats.total.last_request_timestamp or time.time()) - (stats.total.start_time or time.time()))
    ops = {}
    for (name, method), entry in stats.entries.items():
        if entry.num_requests == 0:
            continue
        ops[name] = {
            "method": method,
            "requests": entry.num_requests,
            "failures": entry.num_failures,
            "error_rate": entry.num_failures / entry.num_requests,
            "p50_ms": entry.get_response_time_percentile(0.50),
            "p95_ms": entry.get_response_time_percentile(0.95),
            "p99_ms": entry.get_response_time_percentile(0.99),
            "rate_per_s": entry.num_requests / duration,
        }
    with open(OUT, "w") as fh:
        json.dump({"duration_s": duration, "operations": ops}, fh, indent=2)
