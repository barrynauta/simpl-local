#!/usr/bin/env bash
# Bulk seed for the load test (D99): current store + generated seed, loaded with the restore procedure
# (new dataset, count check, swap). Usage: ./scripts/seed-store.sh <prefix> [vocabularies] [versions]
set -euo pipefail
cd "$(dirname "$0")/.."
PREFIX="${1:?prefix}"; COUNT="${2:-1000}"; VERSIONS="${3:-20}"
OUT=$(./scripts/backup.sh)
FILE=$(echo "$OUT" | sed -nE 's/^backup ([^ ]+) with.*/\1/p')
SEEDED="backups/seeded-$PREFIX.nq.gz"
{ gzip -dc "$FILE"; python3 scripts/seed_store.py --prefix "$PREFIX" --vocabularies "$COUNT" --versions "$VERSIONS"; } | gzip -1 -c > "$SEEDED"
gzip -dc "$SEEDED" | grep -c . > "$SEEDED.count"
./scripts/restore.sh "$SEEDED"
