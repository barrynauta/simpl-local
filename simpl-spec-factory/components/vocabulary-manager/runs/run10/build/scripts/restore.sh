#!/usr/bin/env bash
# Restore of a backup (Q3, D89). Never leaves the store emptier than before: the backup is first loaded into the
# staging dataset 'vocabulary-restore' and its quad count checked; only then the live dataset is replaced in one
# store transaction (Graph Store Protocol PUT on the dataset). The backends are stopped during the restore so no
# write lands in it, and started again after the final count check. Writes made after the backup are lost (RPO).
# Usage: scripts/restore.sh <backups/vocabulary-...nq.gz>
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
ARCHIVE="$1"
( cd "$(dirname "${ARCHIVE}")" && shasum -a 256 -c "$(basename "${ARCHIVE}").sha256" >/dev/null )
WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT
gunzip -c "${ARCHIVE}" > "${WORK}/backup.nq"
chmod 0644 "${WORK}/backup.nq"
chmod 0755 "${WORK}"
EXPECTED="$(grep -c . "${WORK}/backup.nq" || true)"
COMPOSE=(docker compose -p vm-factory-run10)
COUNT_QUERY='SELECT (COUNT(*) AS ?c) WHERE { { ?s ?p ?o } UNION { GRAPH ?g { ?s ?p ?o } } }'

tools() {
  printf 'user = "admin:%s"\n' "${STORE_ADMIN_PASSWORD}" \
    | "${COMPOSE[@]}" --profile tools run --rm -T -v "${WORK}/backup.nq:/backup/backup.nq:ro" store-tools -sS --fail -K - "$@"
}

count() {
  tools -H 'Accept: text/csv' --data-urlencode "query=${COUNT_QUERY}" "http://fuseki:3030/$1/query" | tail -1 | tr -d '\r'
}

echo "stopping backends"
"${COMPOSE[@]}" stop backend
restart() { "${COMPOSE[@]}" up -d --wait backend proxy >/dev/null; }
trap 'restart; rm -rf "${WORK}"' EXIT

tools -X PUT -H 'Content-Type: application/n-quads' --data-binary @/backup/backup.nq \
  http://fuseki:3030/vocabulary-restore/data
STAGED="$(count vocabulary-restore)"
if [[ "${STAGED}" != "${EXPECTED}" ]]; then
  echo "restore aborted: staging holds ${STAGED} quads, backup has ${EXPECTED}; live store untouched" >&2
  exit 1
fi
tools -X PUT -H 'Content-Type: application/n-quads' --data-binary @/backup/backup.nq \
  http://fuseki:3030/vocabulary/data
LIVE="$(count vocabulary)"
tools -X PUT -H 'Content-Type: application/n-quads' --data-binary '' http://fuseki:3030/vocabulary-restore/data
if [[ "${LIVE}" != "${EXPECTED}" ]]; then
  echo "restore check failed: live store holds ${LIVE} quads, backup has ${EXPECTED}" >&2
  exit 1
fi
echo "restored ${EXPECTED} quads; starting backends"
