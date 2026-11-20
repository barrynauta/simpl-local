"""D71 / D99: creates with a file at the 5 MB limit, measured apart from the throughput mix."""
import time
import uuid

from common import BIG_FILE, BIG_WRITES, load_seed, write_json, vm


def main():
    prefix = load_seed()["prefix"]
    times, failures = [], 0
    for _ in range(BIG_WRITES):
        name = f"{prefix}Big{uuid.uuid4().hex[:8]}"
        content = vm.turtle_of_size(f"https://load-big.example.org/{name.lower()}#", 5 * 1024 * 1024)
        start = time.time()
        r = vm.create_vocabulary(name, content)
        ms = (time.time() - start) * 1000
        if r.status_code == 201:
            times.append(ms)
        else:
            failures += 1
            print("big write failed:", vm.describe(r))
    write_json(BIG_FILE, {"requests": BIG_WRITES, "failures": failures, "millis": [round(t) for t in times]})


if __name__ == "__main__":
    main()
