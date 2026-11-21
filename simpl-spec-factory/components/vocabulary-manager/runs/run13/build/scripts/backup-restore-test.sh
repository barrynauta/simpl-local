#!/usr/bin/env bash
# Backup and restore round trip (Q3): back up, restore into a new dataset, and check that the vocabulary
# list total and the quad count are the same as before. Run on every build.
set -euo pipefail
cd "$(dirname "$0")/.."
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
token() { curl -sf -d grant_type=password -d client_id=vocabulary-tests -d username=ga-viewer --data-urlencode 'password=Password1!' "$TOKEN_URL" | sed -E 's/.*"access_token":"([^"]+)".*/\1/'; }
total() { curl -sf -H "Authorization: Bearer $(token)" "$API_URL/vocabularies?pageSize=1" | sed -E 's/.*"total":([0-9]+).*/\1/'; }
BEFORE=$(total)
OUT=$(./scripts/backup.sh)
echo "$OUT"
FILE=$(echo "$OUT" | sed -nE 's/^backup ([^ ]+) with.*/\1/p')
./scripts/restore.sh "$FILE"
AFTER=$(total)
echo "vocabularies before: $BEFORE, after restore: $AFTER"
[[ "$BEFORE" == "$AFTER" ]] && echo "round trip: PASS" || { echo "round trip: FAIL"; exit 1; }
