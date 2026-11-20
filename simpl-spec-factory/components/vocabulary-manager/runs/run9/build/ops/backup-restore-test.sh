#!/usr/bin/env bash
# Backup and restore round trip, run on every build (Q3): count, back up, write a marker vocabulary, restore,
# check that the count is back to the backup and the marker is gone, and that a corrupt backup leaves the store
# untouched.
set -euo pipefail
cd "$(dirname "$0")/.."
PROJECT=vm-factory-run9
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
count() { docker compose -p "$PROJECT" exec -T fuseki java -cp /opt/fuseki/ops StoreOps count; }
token() {
  curl -sf -X POST "$TOKEN_URL" -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin \
    --data-urlencode 'password=Password1!' | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}
BEFORE="$(count)"
OUT="$(ops/backup.sh)"
echo "$OUT"
BACKUP="$(echo "$OUT" | sed -n 's/^backup written: //p')"
MARKER="Restoremarker$(date +%s)"
printf '@prefix ex: <https://example.eu/%s#> .\nex:a ex:b ex:c .\n' "$MARKER" > /tmp/marker.ttl
code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $(token)" -X POST "$API_URL/vocabularies" \
  -F "vocabularyFile=@/tmp/marker.ttl" --form-string "name=$MARKER" --form-string description=marker \
  --form-string changelog=marker)
[ "$code" = 201 ] || { echo "marker create failed: $code"; exit 1; }
AFTER_WRITE="$(count)"
[ "$AFTER_WRITE" -gt "$BEFORE" ] || { echo "marker did not change the count"; exit 1; }
ops/restore.sh "$BACKUP"
RESTORED="$(count)"
[ "$RESTORED" = "$BEFORE" ] || { echo "count after restore $RESTORED, expected $BEFORE"; exit 1; }
code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $(token)" "$API_URL/vocabularies/$MARKER")
[ "$code" = 404 ] || { echo "marker still present after restore: $code"; exit 1; }
printf '<urn:a> <urn:b> "broken .\n' | gzip > /tmp/corrupt.nq.gz
if ops/restore.sh /tmp/corrupt.nq.gz > /tmp/corrupt.log 2>&1; then echo "corrupt restore did not fail"; exit 1; fi
[ "$(count)" = "$BEFORE" ] || { echo "corrupt restore changed the live store"; exit 1; }
echo "backup/restore round trip passed: quads before=$BEFORE after write=$AFTER_WRITE after restore=$RESTORED"
