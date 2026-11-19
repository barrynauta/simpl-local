#!/usr/bin/env bash
# Shared helpers of the backup and restore scripts (Q3, D83). The store publishes no port to the host, so every
# call runs in a short-lived curl container on the internal network store-net, with the backup user of .env.
# Secrets go into the container as an environment variable and are never printed.
set -euo pipefail
OPS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK_DIR="$(cd "$OPS_DIR/.." && pwd)"
PROJECT="${PROJECT:-vm-factory-run8}"
STORE_NETWORK="${STORE_NETWORK:-${PROJECT}_store-net}"
STORE_BASE="${STORE_BASE:-http://fuseki:3030}"
CURL_IMAGE="vm-factory-run8/ops-curl:1.0.0"
if ! docker image inspect "$CURL_IMAGE" >/dev/null 2>&1; then
  docker compose -p "$PROJECT" -f "$WORK_DIR/compose.yaml" --profile tools build -q ops-tools >&2
fi

if [[ ! -f "$WORK_DIR/.env" ]]; then
  echo "error: $WORK_DIR/.env not found; start the stack with ./stack.sh up first" >&2
  exit 1
fi
STORE_BACKUP_USER="$(grep -E '^STORE_BACKUP_USER=' "$WORK_DIR/.env" | cut -d= -f2-)"
STORE_BACKUP_PASSWORD="$(grep -E '^STORE_BACKUP_PASSWORD=' "$WORK_DIR/.env" | cut -d= -f2-)"
export STORE_CRED="${STORE_BACKUP_USER}:${STORE_BACKUP_PASSWORD}"

COUNT_QUERY='SELECT (COUNT(*) AS ?n) WHERE { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } } }'

# store_curl <curl args...>: runs curl inside store-net; stdin is passed through.
store_curl() {
  docker run --rm -i --network "$STORE_NETWORK" -e STORE_CRED --entrypoint sh "$CURL_IMAGE" \
    -c 'exec curl -sS --fail-with-body --max-time 600 -u "$STORE_CRED" "$@"' curl "$@"
}

# dataset_count <dataset>: number of quads (default graph plus named graphs).
dataset_count() {
  store_curl -H 'Accept: text/csv' --data-urlencode "query=$COUNT_QUERY" "$STORE_BASE/$1/query" \
    | tr -d '\r' | sed -n 2p
}

# dataset_dump <dataset> <file>: the whole dataset as N-Quads.
dataset_dump() {
  store_curl -H 'Accept: application/n-quads' "$STORE_BASE/$1/data" > "$2"
}

# dataset_replace <dataset> <file>: replaces the whole dataset with the N-Quads file in one transaction (GSP PUT).
dataset_replace() {
  store_curl -X PUT -H 'Content-Type: application/n-quads' --data-binary @- "$STORE_BASE/$1/data" < "$2" > /dev/null
}

# quads_in <file>: N-Quads has one quad per non-empty line (line breaks in literals are escaped).
quads_in() {
  grep -c -v -e '^[[:space:]]*$' -e '^#' "$1" || true
}
