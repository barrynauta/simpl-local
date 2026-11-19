#!/usr/bin/env bash
# Backup and restore round trip on the running stack (Q3: tested on every build).
#   1. create a vocabulary through the API (ga-admin), so the store holds known data;
#   2. back up the store; 3. restore that backup (staging dataset, count check, replace);
#   4. check the quad count is unchanged and the vocabulary and its content are still served by the API.
set -euo pipefail
OPS="$(cd "$(dirname "$0")" && pwd)"
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
DIR="$(mktemp -d)"
trap 'rm -rf "$DIR"' EXIT
source "$OPS/lib.sh"

TOKEN="$(curl -sf "$TOKEN_URL" --data-urlencode grant_type=password --data-urlencode client_id=vocabulary-tests \
  --data-urlencode username=ga-admin --data-urlencode 'password=Password1!' \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')"
NAME="Backup$(python3 -c 'import secrets; print(secrets.token_hex(4))')"
printf '@prefix ex: <https://example.org/%s#> .\nex:a ex:b "backup round trip" .\n' "$NAME" > "$DIR/v1.ttl"
code="$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TOKEN" "$API_URL/vocabularies" \
  -F "name=$NAME" -F 'description=Backup test' -F 'changelog=Initial version.' -F "vocabularyFile=@$DIR/v1.ttl")"
[[ "$code" == 201 ]] || { echo "FAIL: create returned $code" >&2; exit 1; }

BEFORE="$(dataset_count vocabulary)"
"$OPS/backup.sh" "$DIR/backups"
FILE="$(ls "$DIR"/backups/vocabulary-*.nq | head -1)"
BACKUP_COUNT="$(cat "$FILE.count")"
"$OPS/restore.sh" "$FILE"
AFTER="$(dataset_count vocabulary)"
echo "quads: live before $BEFORE, backup $BACKUP_COUNT, live after restore $AFTER"
[[ "$BACKUP_COUNT" == "$BEFORE" && "$AFTER" == "$BEFORE" ]] || { echo "FAIL: counts differ" >&2; exit 1; }

code="$(curl -s -o "$DIR/voc.json" -w '%{http_code}' -H "Authorization: Bearer $TOKEN" "$API_URL/vocabularies/$NAME")"
[[ "$code" == 200 ]] || { echo "FAIL: GET /vocabularies/$NAME returned $code after restore" >&2; exit 1; }
curl -sf -H "Authorization: Bearer $TOKEN" "$API_URL/content/$NAME" -o "$DIR/content.ttl"
cmp -s "$DIR/content.ttl" "$DIR/v1.ttl" || { echo "FAIL: content differs after restore" >&2; exit 1; }

# A smaller backup must be refused without --allow-smaller (never leave the store emptier).
head -n 1 "$FILE" > "$DIR/small.nq"
if "$OPS/restore.sh" "$DIR/small.nq" 2> "$DIR/err.txt"; then
  echo "FAIL: a smaller backup was restored without --allow-smaller" >&2; exit 1
fi
[[ "$(dataset_count vocabulary)" == "$BEFORE" ]] || { echo "FAIL: refused restore changed the store" >&2; exit 1; }
echo "BACKUP-RESTORE TEST PASSED ($NAME, $AFTER quads)"
