"""Q1 load test (D99, D136, D183): an offered rate of 20 reads per second and 1 write per second.

Reads (each one task, named after its operationId so p50/p95/p99 are per operation):
getVocabularies, getVocabulary, getVocabularyVersions, getVocabularyVersion, searchVocabularies,
downloadVocabulary, downloadVocabularyVersion, getExternalVocabularies.
Write: createVocabularyVersion with files of 10 KB, 100 KB and 1 MB in turn (files up to 1 MB).
The writes are not retried and a 503 counts as an error (D183).
"""
import itertools
import os
import random
import sys

from locust import HttpUser, constant_throughput, events, task

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from vmtest import client, turtle  # noqa: E402

PREFIX = os.environ.get("LOAD_PREFIX", "Load")
VOCABULARIES = int(os.environ.get("LOAD_VOCABULARIES", "1000"))
VERSIONS = int(os.environ.get("LOAD_VERSIONS", "20"))
READS_PER_SECOND = float(os.environ.get("LOAD_READS_PER_SECOND", "20"))
WRITES_PER_SECOND = float(os.environ.get("LOAD_WRITES_PER_SECOND", "1"))
READ_USERS = int(os.environ.get("LOAD_READ_USERS", "20"))
WRITE_USERS = int(os.environ.get("LOAD_WRITE_USERS", "3"))
WRITE_SIZES = [10_000, 100_000, 1_000_000]
_write_counter = itertools.count()


def name(i):
    return f"{PREFIX}{i:05d}"


def auth():
    return {"Authorization": "Bearer " + client.token_for("ga-admin")}


class Reader(HttpUser):
    """20 readers at 1 request per second each = 20 reads per second offered."""
    fixed_count = READ_USERS
    wait_time = constant_throughput(READS_PER_SECOND / max(READ_USERS, 1))

    def _get(self, path, op, accept="application/json"):
        headers = auth()
        headers["Accept"] = accept
        with self.client.get(path, headers=headers, name=op, catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"{response.status_code}")

    @task
    def get_vocabularies(self):
        page = random.randint(1, max(VOCABULARIES // 20, 1))
        sort = random.choice(["name", "-name", "status", "creationDate", "-lastUpdated"])
        self._get(f"/vocabularies?name={PREFIX}&page={page}&pageSize=20&sort={sort}", "getVocabularies")

    @task
    def get_vocabulary(self):
        self._get(f"/vocabularies/{name(random.randrange(VOCABULARIES))}", "getVocabulary")

    @task
    def get_versions(self):
        self._get(f"/vocabularies/{name(random.randrange(VOCABULARIES))}/versions?pageSize=20",
                  "getVocabularyVersions")

    @task
    def get_version(self):
        self._get(f"/vocabularies/{name(random.randrange(VOCABULARIES))}/versions/{random.randint(1, VERSIONS)}",
                  "getVocabularyVersion")

    @task
    def search(self):
        items = [{"name": name(random.randrange(VOCABULARIES))} for _ in range(5)]
        items += [{"name": name(random.randrange(VOCABULARIES)), "version": random.randint(1, VERSIONS)}
                  for _ in range(5)]
        with self.client.post("/vocabularies/searches", json={"items": items}, headers=auth(),
                              name="searchVocabularies", catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"{response.status_code}")

    @task
    def download_latest(self):
        self._get(f"/content/{name(random.randrange(VOCABULARIES))}", "downloadVocabulary", "text/turtle")

    @task
    def download_version(self):
        self._get(f"/content/{name(random.randrange(VOCABULARIES))}/versions/{random.randint(1, VERSIONS)}",
                  "downloadVocabularyVersion", "text/turtle")

    @task
    def external_list(self):
        self._get("/external-vocabularies?page=1&pageSize=20", "getExternalVocabularies")


class Writer(HttpUser):
    """WRITE_USERS writers at WRITES_PER_SECOND / WRITE_USERS each = 1 write per second offered.
    Several writers keep the offered rate when a write takes longer than 1 s (Q1 allows 3 s at p95);
    3 writers stay below the 4 upload slots of D92."""
    fixed_count = WRITE_USERS
    wait_time = constant_throughput(WRITES_PER_SECOND / max(WRITE_USERS, 1))

    @task
    def create_version(self):
        n = next(_write_counter)
        target = name(random.randrange(VOCABULARIES))
        size = WRITE_SIZES[n % len(WRITE_SIZES)]
        content = turtle.sized_file(f"load{n}", target, size)
        files = [("description", (None, f"Load version {n} {random.random()}")),
                 ("changelog", (None, f"Load change {n}")),
                 ("vocabularyFile", ("v.ttl", content, "text/turtle"))]
        with self.client.post(f"/vocabularies/{target}/versions", files=files, headers=auth(),
                              name="createVocabularyVersion", catch_response=True) as response:
            if response.status_code != 201:
                response.failure(f"{response.status_code}")


@events.test_start.add_listener
def _warm_up(environment, **_kwargs):
    """Q1 is measured after warm-up: statistics are reset after LOAD_WARMUP seconds."""
    import gevent

    warmup = float(os.environ.get("LOAD_WARMUP", "30"))

    def reset():
        gevent.sleep(warmup)
        environment.stats.reset_all()
        environment.measure_start = __import__("time").time()

    gevent.spawn(reset)
