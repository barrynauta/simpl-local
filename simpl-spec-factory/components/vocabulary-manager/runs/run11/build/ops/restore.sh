#!/usr/bin/env bash
# Restore of the store from a backup (Q3, D89). Writes made after the backup are lost (within the
# recovery point of 24 h). The backends are stopped first so no write lands during the restore.
# The backup is loaded into a NEW TDB2 dataset; only when its quad count equals the backup's count
# does it replace the old dataset, which is kept as vocabulary-old-<time>. Usage: ops/restore.sh FILE.nq.gz
set -euo pipefail
cd "$(dirname "$0")/.."
FILE="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
[[ -f "$FILE" ]] || { echo "no such backup: $1" >&2; exit 2; }
EXPECTED="$(gzip -dc "$FILE" | grep -c . || true)"
COMPOSE=(docker compose -p vm-factory-run11 -f docker-compose.yml)
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

echo "stopping backends and store"
"${COMPOSE[@]}" --profile storedown stop backend backend-storedown ui >/dev/null
"${COMPOSE[@]}" stop fuseki >/dev/null

echo "loading $EXPECTED quads into a new dataset"
"${COMPOSE[@]}" run --rm --no-deps -T -v "$FILE:/restore/backup.nq.gz:ro" --entrypoint sh fuseki -c "
  set -eu
  rm -rf /fuseki/databases/vocabulary-new
  gzip -dc /restore/backup.nq.gz > /tmp/backup.nq
  java -cp /fuseki/fuseki-server.jar tdb2.tdbloader --loc /fuseki/databases/vocabulary-new /tmp/backup.nq
  COUNT=\$(java -cp /fuseki/fuseki-server.jar tdb2.tdbquery --loc /fuseki/databases/vocabulary-new --results=csv \
    'SELECT (COUNT(*) AS ?n) { GRAPH ?g { ?s ?p ?o } }' | tail -1 | tr -d '\r')
  echo \"loaded \$COUNT quads\"
  if [ \"\$COUNT\" != \"$EXPECTED\" ]; then echo 'count differs, old dataset kept'; rm -rf /fuseki/databases/vocabulary-new; exit 3; fi
  if [ -d /fuseki/databases/vocabulary ]; then mv /fuseki/databases/vocabulary /fuseki/databases/vocabulary-old-$STAMP; fi
  mv /fuseki/databases/vocabulary-new /fuseki/databases/vocabulary
"
echo "starting store and backends"
"${COMPOSE[@]}" up -d --wait --wait-timeout 300 >/dev/null
echo "restore done"
