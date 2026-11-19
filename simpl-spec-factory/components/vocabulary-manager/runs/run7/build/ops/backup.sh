#!/usr/bin/env bash
# Backup of the whole store (Q3, NFR-06): every quad of dataset 'vocabulary' as N-Quads, with a quad
# count and a SHA-256. Run daily (cron or a Kubernetes CronJob) and copy BACKUP_DIR off the host.
#   ops/backup.sh                -> writes $BACKUP_DIR/vocabulary-<UTC time>.nq(.sha256,.count)
set -euo pipefail
FUSEKI=${FUSEKI_URL:-http://127.0.0.1:3141}
BACKUP_DIR=${BACKUP_DIR:-$(cd "$(dirname "$0")" && pwd)/../backups}
mkdir -p "$BACKUP_DIR"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
FILE="$BACKUP_DIR/vocabulary-$STAMP.nq"
curl -sf -H 'Accept: application/n-quads' "$FUSEKI/vocabulary" -o "$FILE.part"
COUNT=$(grep -c -E '^(<|_:)' "$FILE.part" || true)
mv "$FILE.part" "$FILE"
echo "$COUNT" > "$FILE.count"
(cd "$BACKUP_DIR" && shasum -a 256 "$(basename "$FILE")" > "$(basename "$FILE").sha256")
echo "$FILE ($COUNT quads)"
