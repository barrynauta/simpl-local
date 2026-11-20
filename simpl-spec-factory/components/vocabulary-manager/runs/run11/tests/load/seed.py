"""Seed of Q4 / D99: 1000 vocabularies with 20 versions each (files of about 10 KB) and a few
external vocabularies. Never resets the store (E4); reuses an earlier seed when it is still there."""
import concurrent.futures
import os
import random
import string
import sys
import time

from common import (SEED_FILE, SEED_VOCABULARIES, SEED_VERSIONS, SEED_FILE_BYTES, SEED_WORKERS,
                    SEED_EXTERNALS, load_seed, write_json, vm)


def existing_seed():
    if not os.path.exists(SEED_FILE):
        return None
    seed = load_seed()
    if (seed.get("vocabularies") != SEED_VOCABULARIES or seed.get("versions") != SEED_VERSIONS
            or seed.get("api") != vm.API_URL):
        return None
    last = f"{seed['prefix']}{SEED_VOCABULARIES - 1:05d}"
    r = vm.send("GET", f"/vocabularies/{last}", "admin")
    if r.status_code == 200 and r.json().get("version") == SEED_VERSIONS:
        return seed
    return None


def seed_one(prefix, i):
    name = f"{prefix}{i:05d}"
    ns = f"https://load.example.org/{prefix.lower()}/{i}#"
    r = vm.send("GET", f"/vocabularies/{name}", "admin")
    have = r.json()["version"] if r.status_code == 200 else 0
    for v in range(have + 1, SEED_VERSIONS + 1):
        content = vm.turtle_of_size(ns, SEED_FILE_BYTES).replace(b"#x", f"#v{v}".encode(), 1)
        for attempt in range(5):
            if v == 1:
                resp = vm.create_vocabulary(name, content, description=f"Load vocabulary {i}")
            else:
                resp = vm.create_version(name, content, f"Load vocabulary {i} version {v}", f"Version {v}")
            if resp.status_code == 201:
                break
            if resp.status_code == 503:
                time.sleep(1 + attempt)
                continue
            raise RuntimeError(vm.describe(resp))
        else:
            raise RuntimeError(f"seeding {name} v{v} kept failing")
    return name


def main():
    seed = existing_seed()
    if seed:
        print(f"reusing seed {seed['prefix']} ({SEED_VOCABULARIES} x {SEED_VERSIONS})")
        return 0
    prefix = "Load" + "".join(random.choice(string.ascii_lowercase) for _ in range(6))
    print(f"seeding {SEED_VOCABULARIES} vocabularies x {SEED_VERSIONS} versions as {prefix}...")
    start = time.time()
    with concurrent.futures.ThreadPoolExecutor(max_workers=SEED_WORKERS) as ex:
        futures = [ex.submit(seed_one, prefix, i) for i in range(SEED_VOCABULARIES)]
        for n, f in enumerate(concurrent.futures.as_completed(futures), 1):
            f.result()
            if n % 50 == 0:
                print(f"  {n} vocabularies in {time.time() - start:.0f} s")
    externals = []
    for i in range(SEED_EXTERNALS):
        name = f"{prefix}Ext{i:02d}"
        ns = f"https://load-ext.example.org/{prefix.lower()}/{i}"
        content = vm.turtle_of_size(ns + "#", SEED_FILE_BYTES)
        r = vm.register_external(name, ns, content, aliases=[ns + "/alias"])
        if r.status_code not in (201, 409):
            raise RuntimeError(vm.describe(r))
        externals.append({"name": name, "namespaceUri": ns, "alias": ns + "/alias"})
    write_json(SEED_FILE, {"prefix": prefix, "vocabularies": SEED_VOCABULARIES, "versions": SEED_VERSIONS,
                           "fileBytes": SEED_FILE_BYTES, "externals": externals, "api": vm.API_URL,
                           "seconds": round(time.time() - start)})
    print(f"seed done in {time.time() - start:.0f} s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
