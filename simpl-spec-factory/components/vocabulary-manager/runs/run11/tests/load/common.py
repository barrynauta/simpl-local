"""Shared settings of the load test (Q1, Q4, D99). Configuration only through environment variables."""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, os.path.join(ROOT, "support"))

import vm  # noqa: E402,F401  (re-exported to the load scripts)

REPORTS = os.path.join(ROOT, "reports")
SEED_FILE = os.path.join(REPORTS, "load-seed.json")
LOCUST_FILE = os.path.join(REPORTS, "load-locust.json")
BIG_FILE = os.path.join(REPORTS, "load-bigwrites.json")
LOAD_FILE = os.path.join(REPORTS, "load.json")

SEED_VOCABULARIES = int(os.environ.get("SEED_VOCABULARIES", "1000"))
SEED_VERSIONS = int(os.environ.get("SEED_VERSIONS", "20"))
SEED_FILE_BYTES = int(os.environ.get("SEED_FILE_BYTES", "10240"))
SEED_WORKERS = int(os.environ.get("SEED_WORKERS", "4"))          # D92: at most 4 uploads at once
SEED_EXTERNALS = int(os.environ.get("SEED_EXTERNALS", "10"))     # Q4: 10 consuming services search
READ_USERS = int(os.environ.get("LOAD_READ_USERS", "20"))        # Q4: 20 concurrent users
READS_PER_SECOND = float(os.environ.get("LOAD_READS_PER_SECOND", "20"))   # Q1 / D99
WRITES_PER_SECOND = float(os.environ.get("LOAD_WRITES_PER_SECOND", "1"))  # Q1 / D99
BIG_WRITES = int(os.environ.get("LOAD_BIG_WRITES", "3"))         # D71 / D99: 5 MB writes measured apart

READ_P95_MS = 300
WRITE_P95_MS = 3000
BIG_WRITE_MAX_MS = 30000

READ_OPERATIONS = [
    "GET /vocabularies",
    "GET /vocabularies/{vocabularyName}",
    "GET /vocabularies/{vocabularyName}/versions",
    "GET /vocabularies/{vocabularyName}/versions/{version}",
    "POST /vocabularies/searches",
    "GET /content/{vocabularyName}",
    "GET /content/{vocabularyName}/versions/{version}",
    "GET /external-vocabularies",
    "GET /external-vocabularies/{name}",
    "GET /external-vocabularies/{name}/content",
    "POST /external-vocabulary/searches",
    "POST /external-vocabularies/content",
]
WRITE_OPERATIONS = [
    "POST /vocabularies",
    "POST /vocabularies/{vocabularyName}/versions",
]


def load_seed():
    with open(SEED_FILE) as fh:
        return json.load(fh)


def write_json(path, data):
    os.makedirs(REPORTS, exist_ok=True)
    with open(path, "w") as fh:
        json.dump(data, fh, indent=2)
