#!/usr/bin/env bash
# Restore of the vocabulary store from a backup (Q3, D89). The backend is stopped first so no write lands during
# the restore. The backup is loaded into the staging dataset and counted; only when the count matches is the live
# dataset replaced, in one transaction. A failed restore leaves the live dataset as it was.
# Usage: ops/restore.sh backups/vocabulary-<stamp>.nq.gz
set -euo pipefail
cd "$(dirname "$0")/.."
PROJECT=vm-factory-run9
SOURCE="$1"
FILE="restore-$(date -u +%Y%m%dT%H%M%SZ).nq"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
case "$SOURCE" in
  *.gz) gunzip -c "$SOURCE" > "$TMP/$FILE" ;;
  *) cp "$SOURCE" "$TMP/$FILE" ;;
esac
docker compose -p "$PROJECT" stop backend
restart_backend() { docker compose -p "$PROJECT" up -d --wait backend; }
trap 'restart_backend; rm -rf "$TMP"' EXIT
docker compose -p "$PROJECT" cp "$TMP/$FILE" "fuseki:/fuseki/backups/$FILE"
docker compose -p "$PROJECT" exec -T fuseki sh -c "java -cp /opt/fuseki/ops StoreOps restore /fuseki/backups/$FILE"
docker compose -p "$PROJECT" exec -T fuseki rm -f "/fuseki/backups/$FILE"
echo "restore done from $SOURCE"
