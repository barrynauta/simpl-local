#!/usr/bin/env bash
# Restore of the store from a backup (Q3, D89). Never leaves the store emptier than the backup:
#   1. checks the backup file against its SHA-256;
#   2. stops the backends, so no write lands during the restore;
#   3. loads the backup into a NEW dataset directory and counts its quads;
#   4. only when the count equals the backup count, swaps the new dataset in (the old one is kept aside);
#   5. starts the store and the backends again.
# Usage: scripts/restore.sh backups/vocabulary-<stamp>.nq.gz
set -euo pipefail
cd "$(dirname "$0")/.."
export COMPOSE_PROJECT_NAME=vm-factory-run14
backup="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
[ -f "$backup" ] && [ -f "$backup.count" ] && [ -f "$backup.sha256" ] || { echo "backup, .count or .sha256 missing" >&2; exit 1; }
( cd "$(dirname "$backup")" && shasum -a 256 -c "$(basename "$backup").sha256" >/dev/null ) || { echo "checksum mismatch" >&2; exit 1; }
expected="$(cat "$backup.count")"
replicas="$(docker compose ps -q backend | wc -l | tr -d ' ')"
[ "$replicas" -gt 0 ] || replicas=2
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
run() { docker compose run --rm --no-deps -T -v "$(dirname "$backup")":/restore:ro --entrypoint sh fuseki -c "$1"; }

echo "stopping backends"
docker compose stop backend >/dev/null
echo "loading into a new dataset"
run "rm -rf /fuseki/databases/vocabulary-restore && java -Xmx1g -cp /opt/fuseki/fuseki-server.jar tdb2.tdbloader --loc /fuseki/databases/vocabulary-restore /restore/$(basename "$backup") >/dev/null"
loaded="$(run "java -cp /opt/fuseki/fuseki-server.jar tdb2.tdbquery --loc /fuseki/databases/vocabulary-restore --results=csv 'SELECT (COUNT(*) AS ?n) { GRAPH ?g { ?s ?p ?o } }'" | tail -1 | tr -d '\r')"
if [ "$loaded" != "$expected" ]; then
  echo "count check failed: loaded $loaded, backup $expected; the old dataset stays in place" >&2
  docker compose up -d --wait backend >/dev/null
  exit 1
fi
echo "count check passed ($loaded quads); swapping datasets"
docker compose stop fuseki >/dev/null
run "mv /fuseki/databases/vocabulary /fuseki/databases/vocabulary-before-$stamp && mv /fuseki/databases/vocabulary-restore /fuseki/databases/vocabulary"
docker compose up -d --wait fuseki >/dev/null
docker compose up -d --wait --scale backend="$replicas" backend >/dev/null
for _ in $(seq 1 30); do
  [ "$(curl -s -o /dev/null -w '%{http_code}' "${API_URL:-http://localhost:8196/v1}/health")" = "200" ] && break
  sleep 1
done
echo "restore done; previous dataset kept as vocabulary-before-$stamp"
