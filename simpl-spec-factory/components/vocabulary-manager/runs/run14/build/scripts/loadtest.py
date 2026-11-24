#!/usr/bin/env python3
"""Q1 load test (D99, D136, D183): offered rate of 20 reads per second and 1 write per second against the stack,
p95 per operation. Seeds VOCABS vocabularies with VERSIONS versions of about 10 KB first (smaller than the D99
seed unless set). Usage: python3 scripts/loadtest.py [seconds] [vocabs] [versions]"""
import concurrent.futures, random, statistics, sys, threading, time, uuid
import requests

API = "http://localhost:8196/v1"
TOKEN_URL = "http://localhost:8193/realms/simpl/protocol/openid-connect/token"
DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 60
VOCABS = int(sys.argv[2]) if len(sys.argv) > 2 else 200
VERSIONS = int(sys.argv[3]) if len(sys.argv) > 3 else 2
RUN = "Load" + uuid.uuid4().hex[:6]

def token():
    return requests.post(TOKEN_URL, data={"grant_type": "password", "client_id": "vocabulary-tests",
        "username": "ga-admin", "password": "Password1!"}).json()["access_token"]

def content(name, n, size=10_000):
    head = f"@prefix ex: <https://example.org/{name}#> .\n@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
    body, i = [], 0
    while sum(map(len, body)) < size:
        body.append(f'ex:c{i} a skos:Concept ; skos:prefLabel "Concept {i} of version {n}"@en .\n'); i += 1
    return (head + "".join(body)).encode()

session = requests.Session()
auth = {"Authorization": "Bearer " + token()}
def write(name, n):
    files = {"vocabularyFile": ("v.ttl", content(name, n))}
    if n == 1:
        return session.post(f"{API}/vocabularies", headers=auth, files=files, data={"name": name, "description": "d", "changelog": "c"})
    return session.post(f"{API}/vocabularies/{name}/versions", headers=auth, files=files, data={"description": f"d{n}", "changelog": "c"})

names = [f"{RUN}N{i:04d}" for i in range(VOCABS)]
t = time.time()
with concurrent.futures.ThreadPoolExecutor(4) as pool:
    list(pool.map(lambda nm: [write(nm, 1)], names))
    for v in range(2, VERSIONS + 1):
        list(pool.map(lambda nm: write(nm, v), names))
print(f"seeded {VOCABS} x {VERSIONS} in {time.time()-t:.0f} s")

results, lock = {}, threading.Lock()
def record(op, start, status):
    with lock:
        results.setdefault(op, []).append(((time.time() - start) * 1000, status))
def read_op():
    name = random.choice(names); h = {"Authorization": auth["Authorization"]}
    op = random.choice(["list", "listFiltered", "details", "versions", "download", "search"])
    s = time.time()
    if op == "list": r = requests.get(f"{API}/vocabularies?page={random.randint(1, 5)}", headers=h)
    elif op == "listFiltered": r = requests.get(f"{API}/vocabularies?name={name[-6:]}&sort=-lastUpdated", headers=h)
    elif op == "details": r = requests.get(f"{API}/vocabularies/{name}", headers=h)
    elif op == "versions": r = requests.get(f"{API}/vocabularies/{name}/versions", headers=h)
    elif op == "download": r = requests.get(f"{API}/content/{name}", headers=h)
    else: r = requests.post(f"{API}/vocabularies/searches", headers=h, json={"items": [{"name": n} for n in random.sample(names, 10)]})
    record(op, s, r.status_code)
counter = [VERSIONS + 1000]
def write_op():
    with lock:
        counter[0] += 1
        n = counter[0]
    name = random.choice(names); s = time.time(); r = write(name, n); record("newVersion", s, r.status_code)

pool = concurrent.futures.ThreadPoolExecutor(64)
start = time.time(); sent = 0
while time.time() - start < DURATION:
    tick = time.time()
    for _ in range(20): pool.submit(read_op); sent += 1
    pool.submit(write_op); sent += 1
    time.sleep(max(0, 1 - (time.time() - tick)))
pool.shutdown(wait=True)
elapsed = time.time() - start
done = sum(len(v) for v in results.values())
errors = sum(1 for v in results.values() for _, st in v if st >= 400)
print(f"offered {sent} requests in {DURATION} s, completed {done} in {elapsed:.0f} s ({done/elapsed:.1f}/s), errors {errors}")
for op, values in sorted(results.items()):
    times = sorted(t for t, _ in values)
    p95 = times[int(len(times) * 0.95) - 1] if len(times) > 1 else times[0]
    print(f"  {op:14s} n={len(times):5d} p50={statistics.median(times):7.1f} ms p95={p95:7.1f} ms max={times[-1]:7.1f} ms")
