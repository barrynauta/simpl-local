#!/usr/bin/env bash
# Backup of the whole store (Q3, D89): all named graphs as gzipped N-Quads, read by the backup user
# from inside the deployment network (the store publishes no port, D83).
#   BACKUP_DIR          local directory for backups (default ./backups)
#   BACKUP_COPY_TARGET  optional copy target outside the host: a mounted path, or an rsync/scp target
# Prints the path of the backup file.
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; source .env; set +a
BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "$BACKUP_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$BACKUP_DIR/vocabulary-$STAMP.nq.gz"
AUTH="$(printf 'backup:%s' "$FUSEKI_BACKUP_PASSWORD" | base64 | tr -d '\n')"
docker compose -p vm-factory-run11 exec -T fuseki \
  wget -q -O - --header "Authorization: Basic $AUTH" --header "Accept: application/n-quads" \
  http://localhost:3030/vocabulary/data | gzip -9 > "$FILE.part"
mv "$FILE.part" "$FILE"
QUADS="$(gzip -dc "$FILE" | grep -c . || true)"
echo "$QUADS" > "$FILE.count"
if [[ -n "${BACKUP_COPY_TARGET:-}" ]]; then
  if [[ "$BACKUP_COPY_TARGET" == *:* ]]; then
    rsync -a "$FILE" "$FILE.count" "$BACKUP_COPY_TARGET/"
  else
    mkdir -p "$BACKUP_COPY_TARGET" && cp "$FILE" "$FILE.count" "$BACKUP_COPY_TARGET/"
  fi
fi
echo "$FILE"
