#!/usr/bin/env bash
# Restore the store from a backup (Q3, D89). Never leaves the store emptier than before:
#  1. stop the backends (no write lands during the restore), then the store;
#  2. load the backup into a NEW TDB2 dataset directory with the offline loader;
#  3. count the quads of the new dataset and compare with the backup file; stop on a mismatch
#     or an empty result, keeping the old dataset untouched;
#  4. swap the directories (the old one is kept as vocabulary-old-<time>), start store and backends.
# Writes made after the backup are lost; that is within the recovery point of 24 h (D89).
#   ops/restore.sh backups/vocabulary_2026-11-20_19-00-00.nq.gz
set -euo pipefail
cd "$(dirname "$0")/.."
BACKUP="${1:?usage: ops/restore.sh <backup.nq.gz>}"
[[ -f "$BACKUP" ]] || { echo "no such file: $BACKUP" >&2; exit 1; }
PROJECT=vm-factory-run12
DC=(docker compose -p "$PROJECT" -f docker-compose.yml)
expected=$(gunzip -c "$BACKUP" | grep -c . || true)
[[ "$expected" -gt 0 ]] || { echo "backup is empty; refusing to restore" >&2; exit 1; }
abs="$(cd "$(dirname "$BACKUP")" && pwd)/$(basename "$BACKUP")"
ts=$(date -u +%Y%m%dT%H%M%SZ)

"${DC[@]}" stop backend gateway ui
"${DC[@]}" stop fuseki
"${DC[@]}" run --rm --no-deps -T -v "$abs:/restore/backup.nq.gz:ro" --entrypoint sh fuseki -c "
  set -e
  rm -rf /fuseki/databases/vocabulary-restore
  java -cp /opt/fuseki/fuseki-server.jar tdb2.tdbloader --loc /fuseki/databases/vocabulary-restore /restore/backup.nq.gz
  n=\$(java -cp /opt/fuseki/fuseki-server.jar tdb2.tdbquery --loc /fuseki/databases/vocabulary-restore --results=csv 'SELECT (COUNT(*) AS ?n) { { GRAPH ?g { ?s ?p ?o } } UNION { ?s ?p ?o } }' | tail -1 | tr -d '\r')
  echo \"restored dataset holds \$n quads, backup holds $expected\"
  if [ \"\$n\" != \"$expected\" ] || [ \"\$n\" = 0 ]; then echo 'count check failed; old dataset kept' >&2; rm -rf /fuseki/databases/vocabulary-restore; exit 1; fi
  if [ -d /fuseki/databases/vocabulary ]; then mv /fuseki/databases/vocabulary /fuseki/databases/vocabulary-old-$ts; fi
  mv /fuseki/databases/vocabulary-restore /fuseki/databases/vocabulary
"
"${DC[@]}" up -d --wait fuseki
"${DC[@]}" up -d --wait backend gateway ui
echo "restore done; previous dataset kept as vocabulary-old-$ts"
