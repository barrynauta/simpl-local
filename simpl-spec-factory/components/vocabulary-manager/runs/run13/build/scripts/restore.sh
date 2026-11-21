#!/usr/bin/env bash
# Restore of a backup (Q3, D89). The backends are stopped first so no write lands during the restore.
# The backup is loaded into a NEW TDB2 directory, its quad count is checked against the backup, and only
# then the new directory replaces the old one (kept as vocabulary.before-<stamp>). On any failure the old
# store stays as it was, so a restore never leaves the store emptier than before.
#   ./scripts/restore.sh backups/vocabulary-<stamp>.nq.gz
set -euo pipefail
cd "$(dirname "$0")/.."
BACKUP="${1:?usage: restore.sh <backup.nq.gz>}"
EXPECTED=$(cat "$BACKUP.count" 2>/dev/null || gzip -dc "$BACKUP" | grep -c .)
COMPOSE=(docker compose -p vm-factory-run13 -f compose.yaml)
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
REPLICAS=$("${COMPOSE[@]}" ps -q backend | wc -l | tr -d ' ')
[[ "$REPLICAS" -ge 1 ]] || REPLICAS=2

echo "stopping backends ($REPLICAS) and the store"
"${COMPOSE[@]}" stop backend fuseki
VOLUME="vm-factory-run13_fuseki-data"
LOADER=(docker run --rm -i -u fuseki -v "$VOLUME:/fuseki/databases" --entrypoint sh vm-factory-run13-fuseki:6.2.0)

restart() {
  "${COMPOSE[@]}" up -d --wait fuseki
  "${COMPOSE[@]}" up -d --wait --scale backend="$REPLICAS" backend
}
trap 'echo "restore failed, old store kept"; restart' ERR

gzip -dc "$BACKUP" | "${LOADER[@]}" -c '
  set -e
  rm -rf /fuseki/databases/vocabulary.new
  cat > /tmp/restore.nq
  java -cp /fuseki/fuseki-server.jar tdb2.tdbloader --loc /fuseki/databases/vocabulary.new /tmp/restore.nq >/dev/null'
LOADED=$("${LOADER[@]}" -c 'java -cp /fuseki/fuseki-server.jar tdb2.tdbquery --loc /fuseki/databases/vocabulary.new --results=TSV "SELECT (COUNT(*) AS ?n) WHERE { GRAPH ?g { ?s ?p ?o } }"' | tail -1 | sed -E 's/"?([0-9]+).*/\1/')
echo "backup quads: $EXPECTED, loaded quads: $LOADED"
if [[ "$LOADED" != "$EXPECTED" ]]; then
  echo "count check failed; the old store is kept"
  "${LOADER[@]}" -c 'rm -rf /fuseki/databases/vocabulary.new'
  trap - ERR
  restart
  exit 1
fi
"${LOADER[@]}" -c "mv /fuseki/databases/vocabulary /fuseki/databases/vocabulary.before-$STAMP && mv /fuseki/databases/vocabulary.new /fuseki/databases/vocabulary"
trap - ERR
restart
echo "restore done; previous store kept as vocabulary.before-$STAMP"
