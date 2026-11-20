"""Merges the Locust statistics and the 5 MB writes into reports/load.json with the Q1 verdicts."""
import json
import os
import sys

from common import (BIG_FILE, BIG_WRITE_MAX_MS, LOAD_FILE, LOCUST_FILE, READ_OPERATIONS, READ_P95_MS,
                    READS_PER_SECOND, WRITE_OPERATIONS, WRITE_P95_MS, WRITES_PER_SECOND, write_json)


def pct(values, q):
    if not values:
        return None
    s = sorted(values)
    return s[min(len(s) - 1, int(round(q * (len(s) - 1))))]


def main():
    if not os.path.exists(LOCUST_FILE):
        print("no Locust statistics", file=sys.stderr)
        return 1
    locust = json.load(open(LOCUST_FILE))
    ops = locust["operations"]
    result = {"targets": {"Q1": {
        "readP95Ms": READ_P95_MS, "writeP95Ms": WRITE_P95_MS, "bigWriteMaxMs": BIG_WRITE_MAX_MS,
        "offeredReadsPerSecond": READS_PER_SECOND, "offeredWritesPerSecond": WRITES_PER_SECOND,
        "achievedRateShare": 0.95, "errorRate": 0.0}},
        "seconds": locust["seconds"], "operations": {}, "verdicts": {}}
    reads = writes = 0.0
    for name, s in ops.items():
        if name == "Aggregated":
            continue
        entry = dict(s)
        if name in READ_OPERATIONS:
            entry["kind"] = "read"
            entry["met"] = s["p95"] <= READ_P95_MS and s["failures"] == 0
            reads += s["ratePerSecond"]
        elif name in WRITE_OPERATIONS:
            entry["kind"] = "write"
            entry["met"] = s["p95"] <= WRITE_P95_MS and s["failures"] == 0
            writes += s["ratePerSecond"]
        result["operations"][name] = entry
    if os.path.exists(BIG_FILE):
        big = json.load(open(BIG_FILE))
        ms = big["millis"]
        result["operations"]["POST /vocabularies (5 MB file)"] = {
            "kind": "write-5MB", "requests": big["requests"], "failures": big["failures"],
            "errorRate": big["failures"] / big["requests"] if big["requests"] else 0.0,
            "p50": pct(ms, 0.5), "p95": pct(ms, 0.95), "p99": pct(ms, 0.99),
            "met": big["failures"] == 0 and bool(ms) and max(ms) <= BIG_WRITE_MAX_MS}
    result["achieved"] = {"readsPerSecond": round(reads, 2), "writesPerSecond": round(writes, 2)}
    result["verdicts"] = {
        "readP95": all(o["met"] for o in result["operations"].values() if o.get("kind") == "read"),
        "writeP95": all(o["met"] for o in result["operations"].values() if o.get("kind") == "write"),
        "bigWrites": all(o["met"] for o in result["operations"].values() if o.get("kind") == "write-5MB"),
        "throughput": reads >= 0.95 * READS_PER_SECOND and writes >= 0.95 * WRITES_PER_SECOND,
        "noErrors": all(o["failures"] == 0 for o in result["operations"].values()),
    }
    result["verdicts"]["Q1"] = all(result["verdicts"].values())
    write_json(LOAD_FILE, result)
    print(json.dumps(result["verdicts"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
