#!/usr/bin/env bash
# Restore of the store (Q3, NFR-06). Never leaves the store emptier than before:
#  1. checks the SHA-256 of the backup;
#  2. loads it into a NEW, temporary dataset (a throw-away Fuseki container on the stack network) and
#     checks that the quad count there equals the count recorded at backup time;
#  3. refuses a backup with fewer quads than the live store unless --force is given;
#  4. only then replaces the live dataset in one transaction (Graph Store Protocol PUT) and checks the count.
#   ops/restore.sh <backup.nq> [--force]
set -euo pipefail
FILE=${1:?usage: restore.sh <backup.nq> [--force]}
FORCE=${2:-}
FUSEKI=${FUSEKI_URL:-http://127.0.0.1:3141}
NETWORK=${STACK_NETWORK:-vm-factory-run7_default}
IMAGE=${FUSEKI_IMAGE:-vm-factory-run7/fuseki:6.2.0}
CHECK=vm-factory-run7-restore-check
COUNT_QUERY='SELECT (COUNT(*) AS ?c) WHERE { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } } }'

(cd "$(dirname "$FILE")" && shasum -a 256 -c "$(basename "$FILE").sha256" >/dev/null) || { echo "checksum mismatch" >&2; exit 1; }
EXPECTED=$(cat "$FILE.count")
[ "$EXPECTED" -gt 0 ] || { echo "backup holds no quads, refusing" >&2; exit 1; }

cleanup() { docker rm -f "$CHECK" >/dev/null 2>&1 || true; }
trap cleanup EXIT
docker run -d --name "$CHECK" --network "$NETWORK" --entrypoint java "$IMAGE" \
  -jar /opt/fuseki/fuseki-server.jar --mem --update --ping /check >/dev/null
for _ in $(seq 1 60); do
  docker exec "$CHECK" wget -q -O /dev/null 'http://127.0.0.1:3030/$/ping' 2>/dev/null && break
  sleep 1
done
docker cp "$FILE" "$CHECK:/tmp/backup.nq"
docker exec "$CHECK" wget -q -O /dev/null --header 'Content-Type: application/n-quads' \
  --post-file /tmp/backup.nq http://127.0.0.1:3030/check
LOADED=$(docker exec "$CHECK" wget -q -O - --header 'Accept: text/csv' \
  --post-data "query=$(python3 -c 'import sys,urllib.parse;print(urllib.parse.quote(sys.argv[1]))' "$COUNT_QUERY")" \
  http://127.0.0.1:3030/check/query | tail -1 | tr -d '\r')
[ "$LOADED" = "$EXPECTED" ] || { echo "temporary dataset holds $LOADED quads, expected $EXPECTED; live store untouched" >&2; exit 1; }

CURRENT=$(curl -sf -H 'Accept: text/csv' --data-urlencode "query=$COUNT_QUERY" "$FUSEKI/vocabulary/query" | tail -1 | tr -d '\r')
if [ "$EXPECTED" -lt "$CURRENT" ] && [ "$FORCE" != "--force" ]; then
  echo "backup has $EXPECTED quads, live store $CURRENT; restore would make the store emptier (use --force)" >&2
  exit 1
fi
curl -sf -X PUT -H 'Content-Type: application/n-quads' --data-binary "@$FILE" "$FUSEKI/vocabulary" >/dev/null
AFTER=$(curl -sf -H 'Accept: text/csv' --data-urlencode "query=$COUNT_QUERY" "$FUSEKI/vocabulary/query" | tail -1 | tr -d '\r')
[ "$AFTER" = "$EXPECTED" ] || { echo "live store holds $AFTER quads after restore, expected $EXPECTED" >&2; exit 1; }
echo "restored $EXPECTED quads (store held $CURRENT before)"
