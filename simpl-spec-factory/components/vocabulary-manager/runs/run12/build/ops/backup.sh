#!/usr/bin/env bash
# Daily backup of the whole store (Q3, D89): online Fuseki backup (N-Quads, gzip) through the admin
# endpoint inside the store network, copied to BACKUP_DIR and then to BACKUP_COPY_TARGET, which must
# be outside the host in production (for example an rsync or S3 target).
#   BACKUP_DIR=./backups BACKUP_COPY_CMD='rclone copy {file} offsite:vm-backups' ops/backup.sh
set -euo pipefail
cd "$(dirname "$0")/.."
source .env
PROJECT=vm-factory-run12
BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "$BACKUP_DIR"
DC=(docker compose -p "$PROJECT" -f docker-compose.yml)
ADMIN="http://vmadmin:${FUSEKI_ADMIN_PASSWORD}@127.0.0.1:3030"

task=$("${DC[@]}" exec -T fuseki wget -q -O - --post-data='' "$ADMIN/\$/backup/vocabulary" | sed -nE 's/.*"taskId" *: *"([0-9]+)".*/\1/p')
[[ -n "$task" ]] || { echo "backup: no task id" >&2; exit 1; }
for _ in $(seq 1 300); do
  state=$("${DC[@]}" exec -T fuseki wget -q -O - "$ADMIN/\$/tasks/$task" || true)
  if grep -q '"finished"' <<<"$state"; then
    grep -q '"success" *: *true' <<<"$state" || { echo "backup task failed: $state" >&2; exit 1; }
    break
  fi
  sleep 1
done
file=$("${DC[@]}" exec -T fuseki sh -c 'ls -1t /fuseki/backups/vocabulary_*.nq.gz | head -1')
[[ -n "$file" ]] || { echo "backup: no file" >&2; exit 1; }
cid=$("${DC[@]}" ps -q fuseki)
docker cp "$cid:$file" "$BACKUP_DIR/"
local_file="$BACKUP_DIR/$(basename "$file")"
quads=$(gunzip -c "$local_file" | grep -c . || true)
echo "backup written: $local_file ($quads quads)"
if [[ -n "${BACKUP_COPY_CMD:-}" ]]; then
  eval "${BACKUP_COPY_CMD//\{file\}/$local_file}"
  echo "backup copied off the host"
fi
