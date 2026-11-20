#!/usr/bin/env bash
# Round trip of backup and restore (Q3): back up, restore that backup, back up again, compare.
set -euo pipefail
cd "$(dirname "$0")/.."
FIRST="$(ops/backup.sh)"
ops/restore.sh "$FIRST"
SECOND="$(ops/backup.sh)"
A="$(cat "$FIRST.count")"; B="$(cat "$SECOND.count")"
DIFF="$(diff <(gzip -dc "$FIRST" | sort) <(gzip -dc "$SECOND" | sort) | wc -l | tr -d ' ')"
echo "quads before: $A, after: $B, differing lines: $DIFF"
[[ "$A" == "$B" && "$DIFF" == "0" ]] && echo "backup-restore round trip: PASS" || { echo "FAIL"; exit 1; }
