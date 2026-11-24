#!/usr/bin/env bash
# Backup and restore round trip (Q3), run on every build: backup, restore it, compare the quad counts and check
# the API answers with the same data. Prints the measured recovery time.
set -euo pipefail
cd "$(dirname "$0")/.."
export BACKUP_DIR="${BACKUP_DIR:-$PWD/backups}"
before="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8196/v1/health)"
out="$(scripts/backup.sh)"; echo "$out"
file="$(echo "$out" | sed -n 's/^backup written: \(.*\) (.*$/\1/p')"
start=$(date +%s)
scripts/restore.sh "$file"
end=$(date +%s)
scripts/backup.sh > /tmp/vm-after-backup.txt
after_count="$(sed -n 's/^backup written: .* (\([0-9]*\) quads)$/\1/p' /tmp/vm-after-backup.txt)"
expected="$(cat "$file.count")"
[ "$after_count" = "$expected" ] || { echo "ROUND TRIP FAILED: $after_count != $expected" >&2; exit 1; }
health="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8196/v1/health)"
echo "ROUND TRIP PASSED: $expected quads before and after, restore took $((end - start)) s, health $before -> $health"
