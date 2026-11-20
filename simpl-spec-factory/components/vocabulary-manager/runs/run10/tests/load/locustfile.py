"""Locust load test for Q1 at the design load of Q4, shaped as D99.

Offered rate: 20 reads per second (20 users at 1 request per second each) and 1 write per second
(1 user), files up to 1 MB. One task per operation that Q1 names. Started by load/run_load.py,
which seeds the store first and writes reports/load.json.
"""

import json
import os
import random
import sys
import uuid
from pathlib import Path

from locust import HttpUser, constant_throughput, events, task

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "features" / "support"))

from vmclient import encode_multipart, fetch_token, turtle_of_size  # noqa: E402

SEED_TAG = os.environ.get("LOAD_SEED_TAG", "Seed1")
SEED_COUNT = int(os.environ.get("LOAD_SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(os.environ.get("LOAD_SEED_VERSIONS", "20"))
EXT_NAMESPACE = os.environ.get("LOAD_EXT_NAMESPACE", f"https://ext.example.org/load/{SEED_TAG}")
WRITE_SIZES = [int(x) for x in os.environ.get("LOAD_WRITE_SIZES", "10240,102400,524288,1048576").split(",")]
RUN = uuid.uuid4().hex[:6]


def seed_name(i):
    return f"Ld{SEED_TAG}N{i:04d}"


def auth():
    return {"Authorization": f"Bearer {fetch_token('ga-admin')}"}


class Reader(HttpUser):
    fixed_count = int(os.environ.get("LOAD_READ_USERS", "20"))
    wait_time = constant_throughput(float(os.environ.get("LOAD_READ_RPS_PER_USER", "1")))

    def pick(self):
        return seed_name(random.randint(1, SEED_COUNT))

    def json_get(self, path, name):
        self.client.get(path, name=name, headers={**auth(), "Accept": "application/json"})

    @task(3)
    def getVocabularies(self):
        self.json_get(f"/vocabularies?name=Ld{SEED_TAG}&sort=-lastUpdated&page={random.randint(1, 50)}",
                      "getVocabularies")

    @task(2)
    def getVocabulary(self):
        self.json_get(f"/vocabularies/{self.pick()}", "getVocabulary")

    @task(2)
    def getVocabularyVersions(self):
        self.json_get(f"/vocabularies/{self.pick()}/versions", "getVocabularyVersions")

    @task(2)
    def getVocabularyVersion(self):
        self.json_get(f"/vocabularies/{self.pick()}/versions/{random.randint(1, SEED_VERSIONS)}",
                      "getVocabularyVersion")

    @task(3)
    def searchVocabularies(self):
        items = [{"name": self.pick()} for _ in range(5)] + [
            {"name": self.pick(), "version": random.randint(1, SEED_VERSIONS)} for _ in range(5)]
        self.client.post("/vocabularies/searches", name="searchVocabularies", data=json.dumps({"items": items}),
                         headers={**auth(), "Content-Type": "application/json", "Accept": "application/json"})

    @task(2)
    def searchExternalVocabularies(self):
        self.client.post("/external-vocabulary/searches", name="searchExternalVocabularies",
                         data=json.dumps({"namespaceUris": [EXT_NAMESPACE, "https://none.example.org/x"]}),
                         headers={**auth(), "Content-Type": "application/json", "Accept": "application/json"})

    @task(2)
    def downloadVocabulary(self):
        self.client.get(f"/content/{self.pick()}", name="downloadVocabulary",
                        headers={**auth(), "Accept": "text/turtle"})

    @task(2)
    def downloadVocabularyVersion(self):
        self.client.get(f"/content/{self.pick()}/versions/{random.randint(1, SEED_VERSIONS)}",
                        name="downloadVocabularyVersion", headers={**auth(), "Accept": "text/turtle"})

    @task(1)
    def getExternalVocabularies(self):
        self.json_get("/external-vocabularies", "getExternalVocabularies")


class Writer(HttpUser):
    # D99 asks for an offered rate of 1 write per second. One closed-loop user cannot offer that when a write
    # takes longer than 1 s (Q1 allows 3 s p95), so 3 users each offer 1/3 per second (below the D92 cap of 4).
    fixed_count = int(os.environ.get("LOAD_WRITE_USERS", "3"))
    wait_time = constant_throughput(float(os.environ.get("LOAD_WRITE_RPS_PER_USER", str(1 / 3))))

    def on_start(self):
        self.count = 0
        self.mine = []

    def file(self, name):
        size = random.choice(WRITE_SIZES)
        return turtle_of_size(size, f"https://example.org/load/{name}/{uuid.uuid4().hex}#")

    def post(self, path, op, parts):
        body, ctype = encode_multipart(parts)
        with self.client.post(path, name=op, data=body, catch_response=True,
                              headers={**auth(), "Content-Type": ctype, "Accept": "application/json"}) as r:
            if r.status_code != 201:
                r.failure(f"{r.status_code} {r.text[:200]}")
            return r

    @task
    def write(self):
        self.count += 1
        if not self.mine or self.count % 2 == 1:
            name = f"Ld{RUN}U{id(self) % 100000:05d}W{self.count:05d}"
            r = self.post("/vocabularies", "createVocabulary", [
                {"name": "vocabularyFile", "data": self.file(name), "filename": "v.ttl", "content_type": "text/turtle"},
                {"name": "name", "data": name}, {"name": "description", "data": "Load test"},
                {"name": "changelog", "data": "Initial version."}])
            if r.status_code == 201:
                self.mine.append(name)
        else:
            name = random.choice(self.mine)
            self.post(f"/vocabularies/{name}/versions", "createVocabularyVersion", [
                {"name": "vocabularyFile", "data": self.file(name), "filename": "v.ttl", "content_type": "text/turtle"},
                {"name": "description", "data": f"Load test {self.count}"},
                {"name": "changelog", "data": f"Load change {self.count}"}])


@events.quitting.add_listener
def write_stats(environment, **_kwargs):
    out = {}
    stats = environment.stats
    duration = max(stats.total.last_request_timestamp - stats.total.start_time, 1) \
        if stats.total.last_request_timestamp else 1
    for entry in stats.entries.values():
        out[entry.name] = {
            "method": entry.method,
            "requests": entry.num_requests,
            "failures": entry.num_failures,
            "error_rate": (entry.num_failures / entry.num_requests) if entry.num_requests else 0.0,
            "p50_ms": entry.get_response_time_percentile(0.50),
            "p95_ms": entry.get_response_time_percentile(0.95),
            "p99_ms": entry.get_response_time_percentile(0.99),
            "achieved_rps": entry.num_requests / duration,
        }
    path = os.environ.get("LOAD_LOCUST_OUT", str(HERE.parent / "reports" / "load-locust.json"))
    Path(path).write_text(json.dumps({"duration_s": duration, "operations": out}, indent=2))
