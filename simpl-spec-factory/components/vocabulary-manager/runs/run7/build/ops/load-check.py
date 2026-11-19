"""Q1 check on the build's own stack: 20 reads per second and 1 write per second for DURATION seconds,
after a warm-up, then p95 per kind. Standard library only.

Usage: python3 ops/load-check.py [duration_seconds]
"""

import concurrent.futures
import json
import os
import statistics
import sys
import threading
import time
import urllib.parse
import urllib.request
import uuid

API = os.environ.get("API_URL", "http://localhost:8196/v1")
TOKEN_URL = os.environ.get("TOKEN_URL", "http://localhost:8193/realms/simpl/protocol/openid-connect/token")
DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 60


def token():
    data = urllib.parse.urlencode({"grant_type": "password", "client_id": "vocabulary-tests",
                                   "username": "ga-admin", "password": "Password1!"}).encode()
    return json.load(urllib.request.urlopen(TOKEN_URL, data))["access_token"]


TOKEN = token()


def call(method, path, body=None, content_type=None):
    request = urllib.request.Request(API + path, data=body, method=method)
    request.add_header("Authorization", "Bearer " + TOKEN)
    if content_type:
        request.add_header("Content-Type", content_type)
    started = time.perf_counter()
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            response.read()
            status = response.status
    except urllib.error.HTTPError as error:
        status = error.code
    return status, time.perf_counter() - started


def multipart(fields, content):
    boundary = uuid.uuid4().hex
    parts = []
    for name, value in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode())
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="vocabularyFile"; filename="v.ttl"\r\n'
                 f'Content-Type: text/turtle\r\n\r\n'.encode() + content + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def turtle(size, seed):
    lines = ["@prefix ex: <https://example.eu/load#> .", "@prefix skos: <http://www.w3.org/2004/02/skos/core#> ."]
    i = 0
    while sum(len(line) + 1 for line in lines) < size:
        lines.append(f'ex:c{i} a skos:Concept ; skos:prefLabel "Concept {i} {seed}"@en .')
        i += 1
    return "\n".join(lines).encode()


NAME = "Load" + uuid.uuid4().hex[:8].upper()
body, ctype = multipart({"name": NAME, "description": "Load check", "changelog": "Initial version."}, turtle(200_000, 0))
assert call("POST", "/vocabularies", body, ctype)[0] == 201
CONTENT_1MB = turtle(1_000_000, 1)

results = {"read": [], "write": []}
errors = []
lock = threading.Lock()
counter = [0]


def read(i):
    path = [f"/vocabularies?name={NAME[:6]}", f"/vocabularies/{NAME}", f"/vocabularies/{NAME}/versions",
            f"/content/{NAME}"][i % 4]
    if i % 5 == 4:
        status, elapsed = call("POST", "/vocabularies/searches",
                               json.dumps({"items": [{"name": NAME}]}).encode(), "application/json")
    else:
        status, elapsed = call("GET", path)
    with lock:
        (results["read"] if status == 200 else errors).append(elapsed if status == 200 else ("read", path, status))


def write(i):
    with lock:
        counter[0] += 1
        n = counter[0]
    data, content_type = multipart({"description": f"Load version {n}", "changelog": "Load"}, CONTENT_1MB)
    status, elapsed = call("POST", f"/vocabularies/{NAME}/versions", data, content_type)
    with lock:
        (results["write"] if status == 201 else errors).append(elapsed if status == 201 else ("write", status))


def run(seconds, record=True):
    pool = concurrent.futures.ThreadPoolExecutor(max_workers=40)
    start = time.time()
    tick = 0
    while time.time() - start < seconds:
        for j in range(20):
            pool.submit(read, tick * 20 + j)
        pool.submit(write, tick)
        tick += 1
        time.sleep(max(0.0, start + tick - time.time()))
    pool.shutdown(wait=True)


run(5)
results = {"read": [], "write": []}
errors.clear()
run(DURATION)


def p95(values):
    return statistics.quantiles(values, n=20)[18] if len(values) >= 20 else max(values)


print(f"duration {DURATION}s, reads {len(results['read'])}, writes {len(results['write'])}, errors {len(errors)}")
print(f"read p95 {p95(results['read']) * 1000:.0f} ms (target 300 ms)")
print(f"write (1 MB) p95 {p95(results['write']) * 1000:.0f} ms (target 3000 ms)")
if errors:
    print("errors:", errors[:10])
