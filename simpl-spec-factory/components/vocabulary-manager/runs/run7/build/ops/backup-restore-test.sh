#!/usr/bin/env bash
# Round trip of Q3, run on every build: create data, back up, change the store, restore, compare.
set -euo pipefail
cd "$(dirname "$0")"
API_URL=${API_URL:-http://localhost:8196/v1}
TOKEN_URL=${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}
export BACKUP_DIR=$(mktemp -d)
trap 'rm -rf "$BACKUP_DIR"' EXIT
TOKEN=$(curl -s -X POST "$TOKEN_URL" -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin \
  --data-urlencode 'password=Password1!' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
NAME="Backup$(date +%s)"
printf '<http://example.org/backup> <http://example.org/p> "v" .\n' > "$BACKUP_DIR/v.ttl"
curl -sf -o /dev/null -X POST "$API_URL/vocabularies" -H "Authorization: Bearer $TOKEN" --form-string "name=$NAME" \
  --form-string 'description=Backup round trip' --form-string 'changelog=Initial version.' -F "vocabularyFile=@$BACKUP_DIR/v.ttl"
FILE=$(./backup.sh | awk '{print $1}')
curl -sf -o /dev/null -X PATCH "$API_URL/vocabularies/$NAME" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"DEPRECATED"}'
./restore.sh "$FILE"
STATUS=$(curl -sf "$API_URL/vocabularies/$NAME" -H "Authorization: Bearer $TOKEN" | python3 -c 'import sys,json;print(json.load(sys.stdin)["status"])')
[ "$STATUS" = "ACTIVE" ] && echo "PASS backup and restore round trip ($NAME back to ACTIVE)" || { echo "FAIL status $STATUS"; exit 1; }
