#!/usr/bin/env python3
"""Load test of Q1 with the shape of D99 (standard library only).

Phase 1 seeds VOCABULARIES vocabularies with VERSIONS versions each (files of about 10 KB).
Phase 2 offers 20 reads per second and 1 write per second for DURATION seconds and reports, per
operation, the count, errors and p95, and the achieved rate against the offered rate.

  python3 scripts/loadtest.py --vocabularies 1000 --versions 20 --duration 300
  ./scripts/seed-store.sh LoadSeed 1000 20 && python3 scripts/loadtest.py --skip-seed --prefix LoadSeed
"""
import argparse
import concurrent.futures as cf
import json
import random
import statistics
import threading
import time
import urllib.parse
import urllib.request
import uuid

API = "http://localhost:8196/v1"
TOKEN_URL = "http://localhost:8193/realms/simpl/protocol/openid-connect/token"


class Token:
    def __init__(self, user):
        self.user, self.value, self.expires, self.lock = user, None, 0, threading.Lock()

    def get(self):
        with self.lock:
            if time.time() > self.expires - 60:
                data = urllib.parse.urlencode({"grant_type": "password", "client_id": "vocabulary-tests",
                                               "username": self.user, "password": "Password1!"}).encode()
                body = json.load(urllib.request.urlopen(TOKEN_URL, data, timeout=10))
                self.value, self.expires = body["access_token"], time.time() + body["expires_in"]
            return self.value


def request(method, path, token, body=None, content_type=None):
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header("Authorization", "Bearer " + token)
    if content_type:
        req.add_header("Content-Type", content_type)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            resp.read()
            status = resp.status
    except urllib.error.HTTPError as error:
        error.read()
        status = error.code
    except Exception:  # noqa: BLE001 - a transport failure counts as an error
        status = 0
    return status, (time.perf_counter() - start) * 1000


def turtle(name, version, size=10_000):
    lines = ["@prefix skos: <http://www.w3.org/2004/02/skos/core#> .",
             f"@prefix ex: <https://example.eu/{name}#> ."]
    i = 0
    while sum(len(line) + 1 for line in lines) < size:
        lines.append(f'ex:c{i} a skos:Concept ; skos:prefLabel "Concept {i} of {name} v{version}"@en .')
        i += 1
    return ("\n".join(lines) + "\n").encode()


def multipart(fields, file_bytes):
    boundary = "b" + uuid.uuid4().hex
    parts = []
    for key, value in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode())
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="vocabularyFile"; filename="v.ttl"\r\n'
                 f'Content-Type: text/turtle\r\n\r\n'.encode() + file_bytes + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), "multipart/form-data; boundary=" + boundary


def seed(prefix, count, versions, admin, workers):
    def one(index):
        name = f"{prefix}{index:05d}"
        body, ctype = multipart({"name": name, "description": f"Load {index}", "changelog": "Initial version."},
                                turtle(name, 1))
        status, _ = request("POST", "/vocabularies", admin.get(), body, ctype)
        for version in range(2, versions + 1):
            body, ctype = multipart({"description": f"Load {index} v{version}", "changelog": f"v{version}"},
                                    turtle(name, version))
            request("POST", f"/vocabularies/{name}/versions", admin.get(), body, ctype)
        return status
    start = time.time()
    with cf.ThreadPoolExecutor(workers) as pool:
        statuses = list(pool.map(one, range(count)))
    print(f"seeded {count} x {versions} in {time.time() - start:.0f} s, create statuses {set(statuses)}")
    return [f"{prefix}{i:05d}" for i in range(count)]


def run(names, admin, viewer, duration, read_rate, write_rate):
    results, lock = {}, threading.Lock()

    def record(op, status, ms):
        with lock:
            results.setdefault(op, []).append((status, ms))

    def read():
        name = random.choice(names)
        kind = random.choice(["list", "listFiltered", "vocabulary", "versions", "search", "download"])
        if kind == "list":
            status, ms = request("GET", "/vocabularies", viewer.get())
        elif kind == "listFiltered":
            status, ms = request("GET", f"/vocabularies?name={name[-4:]}&sort=-lastUpdated", viewer.get())
        elif kind == "vocabulary":
            status, ms = request("GET", f"/vocabularies/{name}", viewer.get())
        elif kind == "versions":
            status, ms = request("GET", f"/vocabularies/{name}/versions", viewer.get())
        elif kind == "search":
            body = json.dumps({"items": [{"name": name}, {"name": name, "version": 1}]}).encode()
            status, ms = request("POST", "/vocabularies/searches", viewer.get(), body, "application/json")
        else:
            status, ms = request("GET", f"/content/{name}", viewer.get())
        record(kind, status, ms)

    def write():
        name = random.choice(names)
        body, ctype = multipart({"description": "Load write " + uuid.uuid4().hex, "changelog": "Load test."},
                                turtle(name, random.randint(100, 10_000_000)))
        status, ms = request("POST", f"/vocabularies/{name}/versions", admin.get(), body, ctype)
        record("newVersion", status, ms)

    start = time.time()
    offered = 0
    with cf.ThreadPoolExecutor(64) as pool:
        futures = []
        tick = 0
        while time.time() - start < duration:
            for _ in range(read_rate):
                futures.append(pool.submit(read))
                offered += 1
            for _ in range(write_rate):
                futures.append(pool.submit(write))
                offered += 1
            tick += 1
            time.sleep(max(0, start + tick - time.time()))
        cf.wait(futures)
    elapsed = time.time() - start
    done = sum(len(v) for v in results.values())
    print(f"offered {offered} requests in {duration} s, completed {done} in {elapsed:.0f} s "
          f"({100 * done / offered:.1f} % of offered)")
    print(f"{'operation':14} {'count':>6} {'errors':>6} {'p50 ms':>8} {'p95 ms':>8} {'max ms':>8}")
    for op, values in sorted(results.items()):
        times = sorted(ms for _, ms in values)
        errors = sum(1 for status, _ in values if status >= 400 or status == 0)
        p95 = times[max(0, int(len(times) * 0.95) - 1)]
        print(f"{op:14} {len(values):6d} {errors:6d} {statistics.median(times):8.0f} {p95:8.0f} {times[-1]:8.0f}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--vocabularies", type=int, default=1000)
    parser.add_argument("--versions", type=int, default=20)
    parser.add_argument("--duration", type=int, default=300)
    parser.add_argument("--prefix", default="Load" + uuid.uuid4().hex[:6].upper())
    parser.add_argument("--workers", type=int, default=4, help="at most 4: D92 allows 4 uploads per replica")
    parser.add_argument("--skip-seed", action="store_true", help="use names <prefix>00000.. already in the store")
    args = parser.parse_args()
    admin, viewer = Token("ga-admin"), Token("ga-viewer")
    if args.skip_seed:
        names = [f"{args.prefix}{i:05d}" for i in range(args.vocabularies)]
    else:
        names = seed(args.prefix, args.vocabularies, args.versions, admin, args.workers)
    request("GET", "/vocabularies", viewer.get())  # warm-up
    for _ in range(50):
        request("GET", f"/vocabularies/{random.choice(names)}/versions", viewer.get())
    run(names, admin, viewer, args.duration, 20, 1)


if __name__ == "__main__":
    main()
