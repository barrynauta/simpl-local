"""Own measurements for QUALITY.md (Q1, D71): a 5 MB create, a 1 MB create, and a read and write mix.

Usage: python3 perf/measure.py [seconds]   (stack running; ga-admin token from the realm)
Not the full Q4/D99 load test: no 20 000-version seed, short duration. Results are printed as JSON.
"""
import concurrent.futures
import json
import statistics
import sys
import time
import urllib.parse
import urllib.request
import uuid

API = "http://localhost:8196/v1"
TOKEN_URL = "http://localhost:8193/realms/simpl/protocol/openid-connect/token"


def token():
    data = urllib.parse.urlencode({"grant_type": "password", "client_id": "vocabulary-tests", "username": "ga-admin",
                                   "password": "Password1!"}).encode()
    return json.load(urllib.request.urlopen(TOKEN_URL, data))["access_token"]


def turtle(name, size):
    lines = [f"@prefix ex: <https://example.eu/{name}#> .", "@prefix skos: <http://www.w3.org/2004/02/skos/core#> ."]
    i = 0
    total = 0
    while total < size:
        line = f'ex:c{i} a skos:Concept ; skos:prefLabel "Concept number {i} of the measurement vocabulary"@en .'
        lines.append(line)
        total += len(line) + 1
        i += 1
    return "\n".join(lines).encode()[:size].rsplit(b"\n", 1)[0] + b"\n"


def multipart(fields, content):
    boundary = uuid.uuid4().hex
    body = b""
    for k, v in fields.items():
        body += f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
    body += (f'--{boundary}\r\nContent-Disposition: form-data; name="vocabularyFile"; filename="v.ttl"\r\n'
             f"Content-Type: text/turtle\r\n\r\n").encode() + content + b"\r\n" + f"--{boundary}--\r\n".encode()
    return body, f"multipart/form-data; boundary={boundary}"


def call(method, path, tok, body=None, ctype=None):
    req = urllib.request.Request(API + path, data=body, method=method)
    req.add_header("Authorization", "Bearer " + tok)
    if ctype:
        req.add_header("Content-Type", ctype)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            r.read()
            status = r.status
    except urllib.error.HTTPError as e:
        status = e.code
    return status, time.perf_counter() - start


def create(tok, size):
    name = "Perf" + uuid.uuid4().hex[:10]
    body, ctype = multipart({"name": name, "description": "perf", "changelog": "Initial version."}, turtle(name, size))
    status, took = call("POST", "/vocabularies", tok, body, ctype)
    return name, status, took


def p95(values):
    return statistics.quantiles(values, n=20)[18] if len(values) >= 20 else max(values)


def main():
    seconds = int(sys.argv[1]) if len(sys.argv) > 1 else 60
    tok = token()
    result = {}
    _, status, took = create(tok, 5 * 1024 * 1024 - 1024)
    result["create_5mb"] = {"status": status, "seconds": round(took, 2)}
    name, status, took = create(tok, 1024 * 1024)
    result["create_1mb"] = {"status": status, "seconds": round(took, 2)}
    for _ in range(20):  # warm-up
        call("GET", "/vocabularies?pageSize=20", tok)
    reads, writes, errors = [], [], 0
    pool = concurrent.futures.ThreadPoolExecutor(32)
    futures = []
    start = time.time()
    tick = 0
    while time.time() - start < seconds:
        for i in range(20):
            path = ["/vocabularies?pageSize=20", f"/vocabularies/{name}", f"/vocabularies/{name}/versions",
                    f"/content/{name}"][i % 4]
            futures.append(("r", pool.submit(call, "GET", path, tok)))
        futures.append(("w", pool.submit(create, tok, 10 * 1024)))
        tick += 1
        time.sleep(max(0.0, start + tick - time.time()))
        if tick % 240 == 0:
            tok = token()
    for kind, f in futures:
        r = f.result()
        status, took = (r[1], r[2]) if kind == "w" else r
        if status >= 300:
            errors += 1
        (writes if kind == "w" else reads).append(took)
    elapsed = time.time() - start
    result["mix"] = {"seconds": round(elapsed, 1), "reads": len(reads), "writes": len(writes), "errors": errors,
                     "read_rate": round(len(reads) / elapsed, 1), "write_rate": round(len(writes) / elapsed, 2),
                     "read_p95_ms": round(p95(reads) * 1000), "write_p95_ms": round(p95(writes) * 1000)}
    print(json.dumps(result, indent=2))


main()
