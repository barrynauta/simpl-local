#!/usr/bin/env bash
# Restore of the vocabulary store from an N-Quads backup made by ops/backup.sh.
# Stops the backend first (no writes during the restore), replaces the whole dataset, starts the backend again.
# Usage: ops/restore.sh <backup-file>
set -euo pipefail
cd "$(dirname "$0")/.."
IN="${1:?usage: ops/restore.sh <backup-file>}"
[ -s "$IN" ] || { echo "Backup file is empty or missing: $IN" >&2; exit 1; }
C=(docker compose -p vm-factory-run6)
"${C[@]}" stop backend
"${C[@]}" exec -T fuseki wget -q -O /dev/null --post-data "update=DROP ALL" http://127.0.0.1:3030/vocabulary/update
"${C[@]}" exec -T fuseki sh -c 'cat > /tmp/restore.nq && wget -q -O /dev/null --header "Content-Type: application/n-quads" --post-file=/tmp/restore.nq http://127.0.0.1:3030/vocabulary; rc=$?; rm -f /tmp/restore.nq; exit $rc' < "$IN"
"${C[@]}" up -d --wait backend
COUNT=$("${C[@]}" exec -T fuseki wget -q -O - --header "Accept: application/n-quads" http://127.0.0.1:3030/vocabulary | wc -l | tr -d ' ')
echo "Restored $COUNT quads from $IN (backup held $(wc -l < "$IN" | tr -d ' '))"
