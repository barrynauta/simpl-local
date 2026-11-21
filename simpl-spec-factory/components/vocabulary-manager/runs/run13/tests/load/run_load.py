"""Load test for the quality targets Q1 and Q4 in the shape of D99 and D136.

Steps:
  1. Seed (idempotent): LOAD_SEED_VOCABS vocabularies (default 1000) named LoadSeed0000...,
     each with LOAD_SEED_VERSIONS versions (default 20), files of about 10 KB (D99, Q4).
     At most 4 uploads at a time (D92). Existing seed data is reused.
  2. Throughput mix: 20 reads per second and 1 write per second offered, for LOAD_DURATION
     seconds (default 300) after LOAD_WARMUP seconds (default 30) of warm-up (Q1, D99).
     20 read users (Q4: 20 concurrent users) and 1 write user. Writes use files up to 1 MB.
  3. Large writes: LOAD_BIG_WRITES (default 5) creations with a file at the 5 MB limit,
     measured apart from the mix (D71, D99).
  4. reports/load.json: p50, p95, p99, error rate and achieved rate per operation, and the
     verdict per target.

Run with ./run.sh load. Environment: API_URL, TOKEN_URL, TOKEN_CLIENT, USER_PASSWORD and the
LOAD_* variables above. LOAD_SKIP_SEED=1 skips step 1.
"""
from gevent import monkey  # noqa: I001  (must patch before anything else imports ssl/socket)

monkey.patch_all()

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
ROOT = os.path.dirname(HERE)
sys.path.insert(0, ROOT)

from vmtests import auth, config  # noqa: E402
from vmtests.api import create_parts, register_external  # noqa: E402
from vmtests.data import default_turtle, sized_turtle  # noqa: E402
from vmtests.http import Part, multipart_body, send  # noqa: E402

SEED_VOCABS = int(os.environ.get("LOAD_SEED_VOCABS", "1000"))
SEED_VERSIONS = int(os.environ.get("LOAD_SEED_VERSIONS", "20"))
DURATION = int(os.environ.get("LOAD_DURATION", "300"))
WARMUP = int(os.environ.get("LOAD_WARMUP", "30"))
BIG_WRITES = int(os.environ.get("LOAD_BIG_WRITES", "5"))
READ_USERS = int(os.environ.get("LOAD_READ_USERS", "20"))
READ_RATE = float(os.environ.get("LOAD_READ_RATE", "20"))   # reads per second, offered
WRITE_RATE = float(os.environ.get("LOAD_WRITE_RATE", "1"))  # writes per second, offered
SEED_PREFIX = "LoadSeed"
EXT_NAME = "LoadExternal"
EXT_NS = "https://load-external.example.org/ns"
RUN = f"R{int(time.time())}"

READ_P95_MS = 300
WRITE_P95_MS = 3000
BIG_WRITE_P95_MS = 30000

READ_OPS = [
    "getVocabularies", "getVocabulary", "getVocabularyVersions", "getVocabularyVersion",
    "downloadVocabulary", "downloadVocabularyVersion", "searchVocabularies",
    "getExternalVocabularies", "getExternalVocabulary", "searchExternalVocabularies",
    "downloadExternalVocabularyContent", "getExternalVocabularyContentByName",
]
WRITE_OPS = ["createVocabulary", "createVocabularyVersion"]


def token():
    return auth.fetch_token("ga-admin")


def seed_content(name, version):
    """About 10 KB of valid Turtle (D99)."""
    base = default_turtle(name, version)
    return sized_turtle(name, max(len(base) + 64, 10 * 1024))


# ---------------------------------------------------------------- 1. seed

def _seed_one(i):
    name = f"{SEED_PREFIX}{i:04d}"
    resp = send("GET", f"/vocabularies/{name}", token=token())
    if resp.status_code == 404:
        body, ctype = multipart_body(create_parts(name, f"Seed vocabulary {i}.", "Initial version.",
                                                  seed_content(name, 1)))
        r = send("POST", "/vocabularies", token=token(), body=body, content_type=ctype)
        if r.status_code not in (201, 409):
            return f"{name}: create {r.status_code}"
        latest = 1
    elif resp.status_code == 200:
        latest = resp.json()["version"]
    else:
        return f"{name}: get {resp.status_code}"
    for v in range(latest + 1, SEED_VERSIONS + 1):
        body, ctype = multipart_body(create_parts(None, f"Seed vocabulary {i} version {v}.",
                                                  f"Seed version {v}.", seed_content(name, v)))
        r = send("POST", f"/vocabularies/{name}/versions", token=token(), body=body, content_type=ctype)
        if r.status_code == 503:
            time.sleep(1)
            r = send("POST", f"/vocabularies/{name}/versions", token=token(), body=body, content_type=ctype)
        if r.status_code != 201:
            return f"{name}: version {v} {r.status_code}"
    return None


def seed():
    started = time.time()
    with ThreadPoolExecutor(max_workers=4) as pool:  # D92: at most 4 uploads at a time
        errors = [e for e in pool.map(_seed_one, range(SEED_VOCABS)) if e]
    r = send("GET", f"/external-vocabularies/{EXT_NAME}", token=token())
    if r.status_code == 404:
        register_external(EXT_NAME, EXT_NS, aliases=[EXT_NS + "-alias"], token=token())
    return {"vocabularies": SEED_VOCABS, "versions_each": SEED_VERSIONS, "seconds": round(time.time() - started),
            "errors": errors[:20], "error_count": len(errors)}


# ---------------------------------------------------------------- 2. throughput mix

samples = {}  # name -> list of (start time, ms, ok)
measuring = {"on": False}


@events.request.add_listener
def _on_request(request_type, name, response_time, response_length, response=None, exception=None, **_kw):
    if not measuring["on"]:
        return
    ok = exception is None and response is not None and response.status_code < 400
    samples.setdefault(name, []).append((time.time(), response_time, ok))


def _auth():
    return {"Authorization": f"Bearer {token()}"}


def _seed_name():
    return f"{SEED_PREFIX}{random.randrange(SEED_VOCABS):04d}"


class ReadUser(HttpUser):
    fixed_count = READ_USERS
    wait_time = constant_throughput(READ_RATE / max(READ_USERS, 1))

    def _check(self, resp, ok=(200,)):
        if resp.status_code not in ok:
            resp.failure(f"status {resp.status_code}")

    @task
    def read(self):
        op = random.choice(READ_OPS)
        n = _seed_name()
        v = random.randint(1, SEED_VERSIONS)
        h = _auth()
        if op == "getVocabularies":
            sort = random.choice(["name", "-name", "status", "creationDate", "-lastUpdated"])
            url = f"/vocabularies?page={random.randint(1, max(1, SEED_VOCABS // 20))}&pageSize=20&sort={sort}"
            kw = {}
            method = "GET"
        elif op == "getVocabulary":
            method, url, kw = "GET", f"/vocabularies/{n}", {}
        elif op == "getVocabularyVersions":
            method, url, kw = "GET", f"/vocabularies/{n}/versions?pageSize=20", {}
        elif op == "getVocabularyVersion":
            method, url, kw = "GET", f"/vocabularies/{n}/versions/{v}", {}
        elif op == "downloadVocabulary":
            method, url, kw = "GET", f"/content/{n}", {}
        elif op == "downloadVocabularyVersion":
            method, url, kw = "GET", f"/content/{n}/versions/{v}", {}
        elif op == "searchVocabularies":
            items = [{"name": _seed_name()} for _ in range(5)] + [{"name": _seed_name(), "version": v}]
            method, url, kw = "POST", "/vocabularies/searches", {"json": {"items": items}}
        elif op == "getExternalVocabularies":
            method, url, kw = "GET", "/external-vocabularies?pageSize=20", {}
        elif op == "getExternalVocabulary":
            method, url, kw = "GET", f"/external-vocabularies/{EXT_NAME}", {}
        elif op == "searchExternalVocabularies":
            method, url, kw = "POST", "/external-vocabulary/searches", {"json": {"namespaceUris": [EXT_NS + "#"]}}
        elif op == "downloadExternalVocabularyContent":
            method, url, kw = "POST", "/external-vocabularies/content", {"json": {"namespaceUri": EXT_NS}}
        else:
            method, url, kw = "GET", f"/external-vocabularies/{EXT_NAME}/content", {}
        with self.client.request(method, url, headers=h, name=op, catch_response=True, **kw) as resp:
            self._check(resp)


class WriteUser(HttpUser):
    fixed_count = 1
    wait_time = constant_throughput(WRITE_RATE)
    counter = 0

    @task
    def write(self):
        WriteUser.counter += 1
        size = random.randint(10 * 1024, 1024 * 1024)  # files up to 1 MB (Q1, D99)
        h = _auth()
        if WriteUser.counter % 2:
            name = f"LoadW{RUN}N{WriteUser.counter:06d}"
            parts = create_parts(name, f"Load write {WriteUser.counter}.", "Initial version.", sized_turtle(name, size))
            body, ctype = multipart_body(parts)
            url, op = "/vocabularies", "createVocabulary"
        else:
            name = _seed_name()
            parts = create_parts(None, f"Load version {RUN} {WriteUser.counter}.", "Load test version.",
                                 sized_turtle(name, size))
            body, ctype = multipart_body(parts)
            url, op = f"/vocabularies/{name}/versions", "createVocabularyVersion"
        h["Content-Type"] = ctype
        with self.client.post(url, data=body, headers=h, name=op, catch_response=True) as resp:
            if resp.status_code != 201:
                resp.failure(f"status {resp.status_code}")


def _percentile(values, p):
    if not values:
        return None
    s = sorted(values)
    k = max(0, min(len(s) - 1, int(round(p / 100.0 * len(s) + 0.5)) - 1))
    return round(s[k], 1)


def throughput_mix():
    env = Environment(user_classes=[ReadUser, WriteUser], host=config.API_URL, events=events)
    runner = env.create_local_runner()
    runner.start(READ_USERS + 1, spawn_rate=READ_USERS + 1)
    gevent.sleep(WARMUP)
    measuring["on"] = True
    started = time.time()
    gevent.sleep(DURATION)
    measuring["on"] = False
    elapsed = time.time() - started
    runner.quit()
    return elapsed


def summarise(elapsed):
    ops = {}
    for name, rows in samples.items():
        times = [ms for _, ms, _ in rows]
        failures = sum(1 for _, _, ok in rows if not ok)
        ops[name] = {
            "kind": "write" if name in WRITE_OPS else "read",
            "count": len(rows),
            "failures": failures,
            "error_rate": round(failures / len(rows), 4) if rows else None,
            "p50_ms": _percentile(times, 50),
            "p95_ms": _percentile(times, 95),
            "p99_ms": _percentile(times, 99),
            "rate_per_s": round(len(rows) / elapsed, 3),
        }
    reads = sum(o["count"] for n, o in ops.items() if o["kind"] == "read")
    writes = sum(o["count"] for n, o in ops.items() if o["kind"] == "write")
    errors = sum(o["failures"] for o in ops.values())
    return ops, reads / elapsed, writes / elapsed, errors


# ---------------------------------------------------------------- 3. large writes

def big_writes():
    rows = []
    for i in range(BIG_WRITES):
        name = f"LoadBig{RUN}N{i:03d}"
        content = sized_turtle(name, 5 * 1024 * 1024)
        body, ctype = multipart_body([Part("name", name), Part("description", "Write at the 5 MB limit."),
                                      Part("changelog", "Initial version."),
                                      Part("vocabularyFile", content, filename=f"{name}.ttl",
                                           content_type="text/turtle")])
        t0 = time.time()
        r = send("POST", "/vocabularies", token=token(), body=body, content_type=ctype, timeout=120)
        rows.append(((time.time() - t0) * 1000, r.status_code == 201))
    times = [ms for ms, _ in rows]
    failures = sum(1 for _, ok in rows if not ok)
    return {
        "count": len(rows), "failures": failures,
        "error_rate": round(failures / len(rows), 4) if rows else None,
        "p50_ms": _percentile(times, 50), "p95_ms": _percentile(times, 95), "p99_ms": _percentile(times, 99),
    }


def main():
    report = {"api_url": config.API_URL, "started": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "offered": {"reads_per_s": READ_RATE, "writes_per_s": WRITE_RATE, "duration_s": DURATION,
                          "warmup_s": WARMUP, "read_users": READ_USERS, "write_users": 1}}
    if os.environ.get("LOAD_SKIP_SEED") != "1":
        report["seed"] = seed()
    elapsed = throughput_mix()
    ops, read_rate, write_rate, errors = summarise(elapsed)
    report["operations"] = ops
    report["achieved"] = {"reads_per_s": round(read_rate, 3), "writes_per_s": round(write_rate, 3),
                          "errors": errors}
    big = big_writes() if BIG_WRITES > 0 else None
    report["big_writes_5mb"] = big
    targets = {
        "Q1 read p95 <= 300 ms per operation (D136)": all(
            o["p95_ms"] is not None and o["p95_ms"] <= READ_P95_MS for o in ops.values() if o["kind"] == "read"),
        "Q1 write p95 <= 3 s for files up to 1 MB": all(
            o["p95_ms"] is not None and o["p95_ms"] <= WRITE_P95_MS for o in ops.values() if o["kind"] == "write"),
        "Q1 throughput: achieved >= 95 % of offered, no errors (D99)": (
            read_rate >= 0.95 * READ_RATE and write_rate >= 0.95 * WRITE_RATE and errors == 0),
        "Q1/D71 5 MB write p95 <= 30 s": (big is not None and big["failures"] == 0
                                          and big["p95_ms"] is not None and big["p95_ms"] <= BIG_WRITE_P95_MS),
        "every operation of Q1 measured": all(op in ops for op in READ_OPS + WRITE_OPS),
    }
    report["targets"] = targets
    report["met"] = all(targets.values())
    os.makedirs(os.path.join(ROOT, "reports"), exist_ok=True)
    with open(os.path.join(ROOT, "reports", "load.json"), "w") as fh:
        json.dump(report, fh, indent=2)
    print(json.dumps({"achieved": report["achieved"], "targets": targets}, indent=2))


if __name__ == "__main__":
    main()
