"""Load test of Q1 at the design load of Q4, shaped as D99.

Offered rate: 20 reads per second (spread over the read operations Q1 names: lists, details,
searches, downloads under 1 MB) and 1 write per second (create or new version, files up to 1 MB),
for LOAD_RUN_TIME (default 5 minutes, set in run.sh). Every request is named after its operation,
so the report gives p50, p95, p99 and the error rate per operation.
"""
import random
import time
import uuid

from locust import HttpUser, constant_throughput, events, task

from common import (LOCUST_FILE, READ_USERS, READS_PER_SECOND, WRITES_PER_SECOND, load_seed,
                    write_json, vm)

SEED = load_seed()
PREFIX = SEED["prefix"]
N = SEED["vocabularies"]
V = SEED["versions"]
EXTERNALS = SEED["externals"]
WRITE_SIZES = [10 * 1024] * 7 + [100 * 1024] * 2 + [1024 * 1024]   # files up to 1 MB
_created = []
_started = {"t": None}


def headers():
    return {"Authorization": "Bearer " + vm.get_token("admin")}


def random_name():
    return f"{PREFIX}{random.randrange(N):05d}"


class ReadUser(HttpUser):
    fixed_count = READ_USERS
    wait_time = constant_throughput(READS_PER_SECOND / READ_USERS)

    def get(self, path, name):
        with self.client.get(path, headers=headers(), name=name, catch_response=True) as r:
            if r.status_code != 200:
                r.failure(f"{r.status_code}")

    def post(self, path, body, name):
        with self.client.post(path, json=body, headers=headers(), name=name, catch_response=True) as r:
            if r.status_code != 200:
                r.failure(f"{r.status_code}")

    @task
    def list_vocabularies(self):
        page = random.randint(1, max(1, N // 20))
        sort = random.choice(["name", "-name", "status", "creationDate", "-lastUpdated"])
        self.get(f"/vocabularies?page={page}&pageSize=20&sort={sort}", "GET /vocabularies")

    @task
    def get_vocabulary(self):
        self.get(f"/vocabularies/{random_name()}", "GET /vocabularies/{vocabularyName}")

    @task
    def get_versions(self):
        self.get(f"/vocabularies/{random_name()}/versions", "GET /vocabularies/{vocabularyName}/versions")

    @task
    def get_version(self):
        self.get(f"/vocabularies/{random_name()}/versions/{random.randint(1, V)}",
                 "GET /vocabularies/{vocabularyName}/versions/{version}")

    @task
    def search(self):
        items = [{"name": random_name()} for _ in range(5)] + [{"name": random_name(), "version": random.randint(1, V)}]
        self.post("/vocabularies/searches", {"items": items}, "POST /vocabularies/searches")

    @task
    def download_latest(self):
        self.get(f"/content/{random_name()}", "GET /content/{vocabularyName}")

    @task
    def download_version(self):
        self.get(f"/content/{random_name()}/versions/{random.randint(1, V)}",
                 "GET /content/{vocabularyName}/versions/{version}")

    @task
    def list_externals(self):
        self.get("/external-vocabularies?page=1&pageSize=20", "GET /external-vocabularies")

    @task
    def get_external(self):
        if EXTERNALS:
            self.get(f"/external-vocabularies/{random.choice(EXTERNALS)['name']}", "GET /external-vocabularies/{name}")

    @task
    def get_external_content(self):
        if EXTERNALS:
            self.get(f"/external-vocabularies/{random.choice(EXTERNALS)['name']}/content",
                     "GET /external-vocabularies/{name}/content")

    @task
    def search_externals(self):
        if EXTERNALS:
            uris = [e["namespaceUri"] for e in random.sample(EXTERNALS, min(3, len(EXTERNALS)))]
            self.post("/external-vocabulary/searches", {"namespaceUris": uris}, "POST /external-vocabulary/searches")

    @task
    def external_content(self):
        if EXTERNALS:
            self.post("/external-vocabularies/content", {"namespaceUri": random.choice(EXTERNALS)["alias"]},
                      "POST /external-vocabularies/content")


class WriteUser(HttpUser):
    fixed_count = 1
    wait_time = constant_throughput(WRITES_PER_SECOND)

    def multipart(self, name, fields, size):
        ns = f"https://load-write.example.org/{name.lower()}#"
        content = vm.turtle_of_size(ns, size).replace(b"#x", ("#" + uuid.uuid4().hex).encode(), 1)
        return vm.multipart(fields, content)

    @task
    def write(self):
        size = random.choice(WRITE_SIZES)
        if not _created or random.random() < 0.5:
            name = f"{PREFIX}W{uuid.uuid4().hex[:10]}"
            parts = self.multipart(name, [("name", name), ("description", "Load write"),
                                          ("changelog", "Initial version.")], size)
            with self.client.post("/vocabularies", files=parts, headers=headers(),
                                  name="POST /vocabularies", catch_response=True) as r:
                if r.status_code == 201:
                    _created.append(name)
                else:
                    r.failure(f"{r.status_code}")
        else:
            name = random.choice(_created)
            tag = uuid.uuid4().hex[:8]
            parts = self.multipart(name, [("description", f"Load version {tag}"), ("changelog", tag)], size)
            with self.client.post(f"/vocabularies/{name}/versions", files=parts, headers=headers(),
                                  name="POST /vocabularies/{vocabularyName}/versions", catch_response=True) as r:
                if r.status_code != 201:
                    r.failure(f"{r.status_code}")


@events.test_start.add_listener
def on_start(environment, **kw):
    _started["t"] = time.time()


@events.quitting.add_listener
def on_quit(environment, **kw):
    seconds = max(1.0, time.time() - (_started["t"] or time.time()))
    ops = {}
    for (name, method), e in environment.stats.entries.items():
        ops[name] = {
            "requests": e.num_requests,
            "failures": e.num_failures,
            "errorRate": (e.num_failures / e.num_requests) if e.num_requests else 0.0,
            "p50": e.get_response_time_percentile(0.50),
            "p95": e.get_response_time_percentile(0.95),
            "p99": e.get_response_time_percentile(0.99),
            "ratePerSecond": e.num_requests / seconds,
        }
    write_json(LOCUST_FILE, {"seconds": round(seconds, 1), "operations": ops})
