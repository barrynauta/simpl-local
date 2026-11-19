#!/usr/bin/env bash
# Backup of the whole store (Q3): dumps dataset 'vocabulary' as N-Quads with the backup user (D83).
#   ops/backup.sh [backup-dir]      default: work/backups/
# Writes vocabulary-<UTC time>.nq, a .count file with the quad count and a .sha256 file. Copy the directory to a
# place outside the host (Q3: kept outside the host); a daily scheduler entry runs this script in production.
source "$(dirname "$0")/lib.sh"
DEST="${1:-$WORK_DIR/backups}"
mkdir -p "$DEST"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
FILE="$DEST/vocabulary-$STAMP.nq"
umask 077
dataset_dump vocabulary "$FILE.partial"
mv "$FILE.partial" "$FILE"
COUNT="$(quads_in "$FILE")"
echo "$COUNT" > "$FILE.count"
(cd "$DEST" && shasum -a 256 "$(basename "$FILE")" > "$(basename "$FILE").sha256")
echo "backup written: $FILE ($COUNT quads)"
