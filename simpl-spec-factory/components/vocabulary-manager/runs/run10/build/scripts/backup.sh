#!/usr/bin/env bash
# Daily backup of the whole store (Q3, D89): all quads of the dataset 'vocabulary' as N-Quads, read by the store
# admin user over the internal store network, gzipped with a SHA-256 and a quad count, written to BACKUP_TARGET.
# BACKUP_TARGET must be a location outside the host in production (mounted NFS/SMB share, or a directory that a
# following rsync/rclone step copies off the host); default ./backups for the local stack.
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
TARGET="${BACKUP_TARGET:-./backups}"
mkdir -p "${TARGET}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="${TARGET}/vocabulary-${STAMP}.nq"

printf 'user = "admin:%s"\n' "${STORE_ADMIN_PASSWORD}" \
  | docker compose -p vm-factory-run10 --profile tools run --rm -T store-tools \
      -sS --fail -K - -H 'Accept: application/n-quads' http://fuseki:3030/vocabulary/data > "${FILE}.part"
mv "${FILE}.part" "${FILE}"
COUNT="$(grep -c . "${FILE}" || true)"
gzip -9 "${FILE}"
( cd "${TARGET}" && shasum -a 256 "$(basename "${FILE}.gz")" > "$(basename "${FILE}.gz").sha256" )
echo "${COUNT}" > "${FILE}.gz.count"
echo "backup ${FILE}.gz quads=${COUNT}"
