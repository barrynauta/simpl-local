#!/usr/bin/env bash
# Daily backup of the whole store (Q3, D89): reads every quad through the admin user's read-only
# dataset endpoint inside the stack network, writes a gzipped N-Quads file with its quad count, and
# copies it to BACKUP_TARGET (a directory outside the host in production, for example a mounted share).
# Example schedule (cron, 02:00 every day):
#   0 2 * * * cd /opt/vocabulary-manager && ./scripts/backup.sh >> backups/backup.log 2>&1
set -euo pipefail
cd "$(dirname "$0")/.."
COMPOSE=(docker compose -p vm-factory-run13 -f compose.yaml)
BACKUP_DIR="${BACKUP_DIR:-backups}"
BACKUP_TARGET="${BACKUP_TARGET:-}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$BACKUP_DIR/vocabulary-$STAMP.nq.gz"
mkdir -p "$BACKUP_DIR"
"${COMPOSE[@]}" exec -T fuseki sh -c \
  'wget -q -O - --header "Accept: application/n-quads" "http://admin:${FUSEKI_ADMIN_PASSWORD}@127.0.0.1:3030/vocabulary/data"' \
  | gzip -c > "$FILE.part"
COUNT=$(gzip -dc "$FILE.part" | grep -c . || true)
mv "$FILE.part" "$FILE"
echo "$COUNT" > "$FILE.count"
echo "backup $FILE with $COUNT quads"
if [[ -n "$BACKUP_TARGET" ]]; then
  mkdir -p "$BACKUP_TARGET"
  cp "$FILE" "$FILE.count" "$BACKUP_TARGET/"
  echo "copied to $BACKUP_TARGET"
fi
