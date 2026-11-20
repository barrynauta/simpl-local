#!/usr/bin/env bash
# Daily backup of the whole vocabulary store (Q3, D89). Writes N-Quads inside the store container (admin user,
# internal network only, D83), copies the file to ./backups on the host and then to BACKUP_COPY_TARGET, a place
# outside the host (an rsync or scp destination such as backup@vault.example:/srv/vm-backups/, or a mounted path).
# Example schedule: see docs/operations.md (cron line and Kubernetes CronJob).
set -euo pipefail
cd "$(dirname "$0")/.."
PROJECT=vm-factory-run9
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="vocabulary-${STAMP}.nq"
mkdir -p backups
docker compose -p "$PROJECT" exec -T fuseki sh -c \
  "java -cp /opt/fuseki/ops StoreOps backup /fuseki/backups/${FILE}"
docker compose -p "$PROJECT" cp "fuseki:/fuseki/backups/${FILE}" "backups/${FILE}"
docker compose -p "$PROJECT" exec -T fuseki rm -f "/fuseki/backups/${FILE}"
gzip -f "backups/${FILE}"
echo "backup written: backups/${FILE}.gz"
if [ -n "${BACKUP_COPY_TARGET:-}" ]; then
  if [[ "${BACKUP_COPY_TARGET}" == *:* ]]; then
    rsync -a "backups/${FILE}.gz" "${BACKUP_COPY_TARGET}"
  else
    cp "backups/${FILE}.gz" "${BACKUP_COPY_TARGET}/"
  fi
  echo "copied to ${BACKUP_COPY_TARGET}"
else
  echo "WARNING: BACKUP_COPY_TARGET is not set; the backup stays on this host only." >&2
fi
