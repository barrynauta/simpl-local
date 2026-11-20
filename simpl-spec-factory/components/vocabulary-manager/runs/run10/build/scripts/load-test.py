#!/usr/bin/env python3
"""Own load measurement for Q1 (shape of D99, scaled by arguments). Seeds vocabularies with versions of about 10 KB,
then offers 20 reads per second and 1 write per second (files of 100 KB to 1 MB) for a duration, and finally times
single writes at 1 MB and at the 5 MB limit. Prints achieved rates and p95 per kind.
Usage: python3 scripts/load-test.py [vocabularies] [versions] [seconds]"""
import json
import os
import random
import statistics
import sys
import threading
import time
import urllib.parse
import urllib.request
import uuid

API = os.environ.get("API_URL", "http://localhost:8196/v1")
TOKEN_URL = os.environ.get("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
VOCABULARIES = int(sys.argv[1]) if len(sys.argv) > 1 else 100
VERSIONS = int(sys.argv[2]) if len(sys.argv) > 2 else 3
SECONDS = int(sys.argv[3]) if len(sys.argv) > 3 else 60
RUN = "Load" + uuid.uuid4().hex[:8]


def token():
    data = urllib.parse.urlencode({"grant_type": "password", "client_id": "vocabulary-tests",
                                   "username": "ga-admin", "password": "Password1!"}).encode()
    return json.load(urllib.request.urlopen(TOKEN_URL, data))["access_token"]


TOKEN = token()


def turtle(name, size, salt):
    lines = [f"@prefix ex: <https://example.org/{name}#> .", f"# {salt}"]
    i = 0
    while sum(len(line) + 1 for line in lines) < size:
        lines.append(f'ex:term{i} ex:label "Term number {i} of {name}" .')
        i += 1
    return ("\n".join(lines) + "\n").encode()


def multipart(fields, file_bytes):
    boundary = uuid.uuid4().hex
    body = b""
    for key, value in fields.items():
        body += f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode()
    body += (f'--{boundary}\r\nContent-Disposition: form-data; name="vocabularyFile"; filename="v.ttl"\r\n'
             f"Content-Type: text/turtle\r\n\r\n").encode() + file_bytes + f"\r\n--{boundary}--\r\n".encode()
    return body, f"multipart/form-data; boundary={boundary}"


def call(method, path, body=None, content_type=None):
    request = urllib.request.Request(API + path, data=body, method=method)
    request.add_header("Authorization", "Bearer " + TOKEN)
    if content_type:
        request.add_header("Content-Type", content_type)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            response.read()
            status = response.status
    except urllib.error.HTTPError as error:
        status = error.code
    return status, time.perf_counter() - start


def create(name, size):
    body, ctype = multipart({"name": name, "description": "Load", "changelog": "Initial version."},
                            turtle(name, size, uuid.uuid4()))
    return call("POST", "/vocabularies", body, ctype)


def version(name, size):
    body, ctype = multipart({"description": "Load " + uuid.uuid4().hex, "changelog": "Change."},
                            turtle(name, size, uuid.uuid4()))
    return call("POST", f"/vocabularies/{name}/versions", body, ctype)


def p95(values):
    return statistics.quantiles(values, n=20)[18] if len(values) >= 20 else max(values)


names = [f"{RUN}N{i}" for i in range(VOCABULARIES)]
seed_start = time.perf_counter()
for name in names:
    assert create(name, 10_000)[0] == 201
    for _ in range(VERSIONS - 1):
        assert version(name, 10_000)[0] == 201
print(f"seeded {VOCABULARIES} vocabularies x {VERSIONS} versions in {time.perf_counter() - seed_start:.0f} s")

results = {"read": [], "write": []}
errors = []
lock = threading.Lock()


def read_once():
    name = random.choice(names)
    kind = random.choice(["list", "details", "versions", "search", "download"])
    if kind == "list":
        status, elapsed = call("GET", f"/vocabularies?name={RUN}&page={random.randint(1, 5)}")
    elif kind == "details":
        status, elapsed = call("GET", f"/vocabularies/{name}")
    elif kind == "versions":
        status, elapsed = call("GET", f"/vocabularies/{name}/versions")
    elif kind == "search":
        status, elapsed = call("POST", "/vocabularies/searches", json.dumps({"items": [{"name": name}]}).encode(),
                               "application/json")
    else:
        status, elapsed = call("GET", f"/content/{name}")
    with lock:
        results["read"].append(elapsed)
        if status != 200:
            errors.append((kind, status))


def write_once():
    status, elapsed = version(random.choice(names), random.choice([100_000, 500_000, 1_000_000]))
    with lock:
        results["write"].append(elapsed)
        if status != 201:
            errors.append(("write", status))


threads = []
start = time.perf_counter()
offered = {"read": 0, "write": 0}
while time.perf_counter() - start < SECONDS:
    second = time.perf_counter()
    for i in range(20):
        thread = threading.Thread(target=read_once)
        thread.start()
        threads.append(thread)
        offered["read"] += 1
        time.sleep(0.04)
    thread = threading.Thread(target=write_once)
    thread.start()
    threads.append(thread)
    offered["write"] += 1
    time.sleep(max(0.0, 1.0 - (time.perf_counter() - second)))
for thread in threads:
    thread.join()
elapsed = time.perf_counter() - start
print(f"mix {SECONDS} s: offered {offered['read'] / SECONDS:.1f} reads/s and {offered['write'] / SECONDS:.2f} writes/s;"
      f" achieved {len(results['read']) / elapsed:.1f} reads/s, {len(results['write']) / elapsed:.2f} writes/s;"
      f" errors {len(errors)} {errors[:5]}")
print(f"read p95 {p95(results['read']) * 1000:.0f} ms, write (100 KB to 1 MB) p95 {p95(results['write']) * 1000:.0f} ms")
one_mb = [version(names[0], 1_000_000)[1] for _ in range(5)]
print(f"1 MB new version: max {max(one_mb) * 1000:.0f} ms")
status, five = create(RUN + "Five", 5 * 1024 * 1024 - 200)
print(f"5 MB create: status {status}, {five:.1f} s")
status, five_version = version(RUN + "Five", 5 * 1024 * 1024 - 200)
print(f"5 MB new version: status {status}, {five_version:.1f} s")
