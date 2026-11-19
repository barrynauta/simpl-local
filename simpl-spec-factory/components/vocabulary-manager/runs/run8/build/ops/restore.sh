#!/usr/bin/env bash
# Restore of the store from a backup of ops/backup.sh (Q3). The restore never leaves the store emptier than before:
#   1. check the backup file against its checksum and its recorded quad count;
#   2. refuse a backup with fewer quads than the live store holds, unless --allow-smaller is given;
#   3. load the backup into the staging dataset 'vocabulary-restore' (GSP PUT, replaces its content) and check that
#      its quad count equals the backup count; on a mismatch stop, 'vocabulary' is not touched;
#   4. save the live dataset as a safety copy, then replace 'vocabulary' (GSP PUT: clear and load in one transaction)
#      and check its count again; on a mismatch put the safety copy back and fail.
#   ops/restore.sh <backup.nq> [--allow-smaller]
source "$(dirname "$0")/lib.sh"
FILE="${1:?usage: ops/restore.sh <backup.nq> [--allow-smaller]}"
ALLOW_SMALLER="${2:-}"
[[ -f "$FILE" ]] || { echo "error: $FILE not found" >&2; exit 1; }

if [[ -f "$FILE.sha256" ]]; then
  (cd "$(dirname "$FILE")" && shasum -a 256 -c "$(basename "$FILE").sha256" > /dev/null) \
    || { echo "error: checksum of $FILE does not match" >&2; exit 1; }
fi
EXPECTED="$(quads_in "$FILE")"
if [[ -f "$FILE.count" && "$(cat "$FILE.count")" != "$EXPECTED" ]]; then
  echo "error: $FILE holds $EXPECTED quads, its .count file says $(cat "$FILE.count")" >&2
  exit 1
fi

LIVE_BEFORE="$(dataset_count vocabulary)"
if (( EXPECTED < LIVE_BEFORE )) && [[ "$ALLOW_SMALLER" != "--allow-smaller" ]]; then
  echo "error: backup has $EXPECTED quads, the live store $LIVE_BEFORE; refusing to make the store emptier" >&2
  echo "       (pass --allow-smaller to restore an older, smaller state on purpose)" >&2
  exit 1
fi

dataset_replace vocabulary-restore "$FILE"
STAGED="$(dataset_count vocabulary-restore)"
if [[ "$STAGED" != "$EXPECTED" ]]; then
  echo "error: staging dataset holds $STAGED quads, expected $EXPECTED; 'vocabulary' not touched" >&2
  exit 1
fi
echo "staging check passed: $STAGED quads"

SAFETY="$(dirname "$FILE")/pre-restore-$(date -u +%Y%m%dT%H%M%SZ).nq"
umask 077
dataset_dump vocabulary "$SAFETY"
dataset_replace vocabulary "$FILE"
AFTER="$(dataset_count vocabulary)"
if [[ "$AFTER" != "$EXPECTED" ]]; then
  echo "error: store holds $AFTER quads after the restore, expected $EXPECTED; putting the previous state back" >&2
  dataset_replace vocabulary "$SAFETY"
  exit 1
fi
# Empty the staging dataset again so it holds no second copy of the data.
store_curl -X PUT -H 'Content-Type: application/n-quads' --data-binary '' "$STORE_BASE/vocabulary-restore/data" > /dev/null
echo "restore done: $AFTER quads (before: $LIVE_BEFORE); safety copy of the previous state: $SAFETY"
