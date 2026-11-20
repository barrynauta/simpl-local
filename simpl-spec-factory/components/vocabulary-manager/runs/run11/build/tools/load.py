"""Own load measurement for Q1 and D99 (reduced seed, see QUALITY.md).

Usage: python3 tools/load.py [--vocabularies 200] [--versions 3] [--seconds 120]
Seeds vocabularies with files of about 10 KB, then offers 20 reads per second and 1 write per
second (files up to 1 MB) for the given time and reports p95 per operation; then times one write of
a file at the 5 MB limit (D71). Standard library only.
"""
import argparse
import concurrent.futures
import json
import os
import random
import statistics
import threading
import time
import urllib.parse
import urllib.request
import uuid

API = os.environ.get("API_URL", "http://localhost:8196/v1")
TOKEN_URL = os.environ.get("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")


def token():
    data = urllib.parse.urlencode({"grant_type": "password", "client_id": "vocabulary-tests",
                                   "username": "ga-admin", "password": "Password1!"}).encode()
    return json.load(urllib.request.urlopen(TOKEN_URL, data))["access_token"]


TOKEN = {"value": token(), "at": time.time()}
LOCK = threading.Lock()


def auth():
    with LOCK:
        if time.time() - TOKEN["at"] > 600:
            TOKEN["value"], TOKEN["at"] = token(), time.time()
        return "Bearer " + TOKEN["value"]


def turtle(name, size, salt):
    lines = [f"@prefix ex: <https://example.org/{name}#> .",
             "@prefix skos: <http://www.w3.org/2004/02/skos/core#> ."]
    i = 0
    while sum(len(x) + 1 for x in lines) < size:
        lines.append(f'ex:C{i} a skos:Concept ; skos:prefLabel "Concept {i} {salt}"@en .')
        i += 1
    return ("\n".join(lines) + "\n").encode()


def multipart(fields, content):
    boundary = uuid.uuid4().hex
    body = b""
    for k, v in fields.items():
        body += f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
    body += (f'--{boundary}\r\nContent-Disposition: form-data; name="vocabularyFile"; filename="v.ttl"\r\n'
             "Content-Type: text/turtle\r\n\r\n").encode() + content + b"\r\n" + f"--{boundary}--\r\n".encode()
    return body, "multipart/form-data; boundary=" + boundary


def call(method, path, body=None, ctype=None):
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header("Authorization", auth())
    if ctype:
        req.add_header("Content-Type", ctype)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            r.read()
            status = r.status
    except urllib.error.HTTPError as e:
        status = e.code
    return status, (time.perf_counter() - start) * 1000


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--vocabularies", type=int, default=200)
    ap.add_argument("--versions", type=int, default=3)
    ap.add_argument("--seconds", type=int, default=120)
    a = ap.parse_args()
    run = uuid.uuid4().hex[:8]
    names = [f"Load{run}N{i}" for i in range(a.vocabularies)]

    t0 = time.time()
    def seed(name):
        b, c = multipart({"name": name, "description": "Load", "changelog": "Initial version."}, turtle(name, 10_000, 0))
        call("POST", "/vocabularies", b, c)
        for v in range(2, a.versions + 1):
            b, c = multipart({"description": f"Load {v}", "changelog": "c"}, turtle(name, 10_000, v))
            call("POST", f"/vocabularies/{name}/versions", b, c)
    with concurrent.futures.ThreadPoolExecutor(4) as pool:
        list(pool.map(seed, names))
    print(f"seeded {a.vocabularies} x {a.versions} in {time.time() - t0:.0f} s")

    results = {}
    errors = []
    rlock = threading.Lock()

    def record(op, status, ms):
        with rlock:
            results.setdefault(op, []).append(ms)
            if status >= 400:
                errors.append((op, status))

    reads = [
        ("list", lambda: call("GET", f"/vocabularies?page={random.randint(1, max(1, a.vocabularies // 20))}&pageSize=20")),
        ("list-filter", lambda: call("GET", f"/vocabularies?name=Load{run}&sort=-lastUpdated")),
        ("details", lambda: call("GET", f"/vocabularies/{random.choice(names)}")),
        ("versions", lambda: call("GET", f"/vocabularies/{random.choice(names)}/versions")),
        ("download", lambda: call("GET", f"/content/{random.choice(names)}")),
        ("search", lambda: call("POST", "/vocabularies/searches", json.dumps(
            {"items": [{"name": n} for n in random.sample(names, min(10, len(names)))]}).encode(), "application/json")),
    ]
    pool = concurrent.futures.ThreadPoolExecutor(32)
    end = time.time() + a.seconds
    offered = {"reads": 0, "writes": 0}
    tick = time.time()
    while time.time() < end:
        for _ in range(20):
            op, fn = random.choice(reads)
            offered["reads"] += 1
            pool.submit(lambda op=op, fn=fn: record(op, *fn()))
        name = random.choice(names)
        size = random.choice([10_000, 100_000, 1_000_000])
        b, c = multipart({"description": "Load write " + uuid.uuid4().hex, "changelog": "c"}, turtle(name, size, uuid.uuid4().hex))
        offered["writes"] += 1
        pool.submit(lambda b=b, c=c, name=name: record("new-version", *call("POST", f"/vocabularies/{name}/versions", b, c)))
        tick += 1
        time.sleep(max(0, tick - time.time()))
    pool.shutdown(wait=True)

    def p95(xs):
        return statistics.quantiles(xs, n=100)[94] if len(xs) >= 20 else max(xs)
    print(f"offered: {offered['reads']} reads, {offered['writes']} writes in {a.seconds} s")
    for op, xs in sorted(results.items()):
        print(f"{op:12s} n={len(xs):5d} p50={statistics.median(xs):7.1f} ms p95={p95(xs):7.1f} ms max={max(xs):7.1f} ms")
    print(f"errors: {len(errors)} {errors[:5]}")

    big_name = names[0]
    b, c = multipart({"description": "Five MB", "changelog": "c"}, turtle(big_name, 5_200_000, "big"))
    status, ms = call("POST", f"/vocabularies/{big_name}/versions", b, c)
    print(f"5 MB new version: status {status}, {ms / 1000:.1f} s, file {len(b)} bytes of request")
    status, ms = call("GET", f"/content/{big_name}")
    print(f"5 MB download: status {status}, {ms:.0f} ms")


if __name__ == "__main__":
    main()
