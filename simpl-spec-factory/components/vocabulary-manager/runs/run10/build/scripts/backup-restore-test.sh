#!/usr/bin/env bash
# Backup and restore round trip (Q3): a vocabulary created before the backup survives the restore, one created after
# it is gone (within the RPO), and the API works again after the restore.
set -euo pipefail
cd "$(dirname "$0")/.."
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN=$(curl -sf -X POST "${TOKEN_URL}" -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin \
  --data-urlencode 'password=Password1!' | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')
create() {
  printf '@prefix ex: <https://example.org/%s#> .\nex:a ex:b ex:c .\n' "$1" > /tmp/rt.ttl
  curl -sf -o /dev/null -H "Authorization: Bearer ${TOKEN}" --form-string "name=$1" --form-string description=d \
    --form-string changelog=c -F vocabularyFile=@/tmp/rt.ttl "${API_URL}/vocabularies"
}
status_of() { curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${TOKEN}" "${API_URL}/vocabularies/$1"; }
BEFORE="Backup$(date +%s)A"; AFTER="Backup$(date +%s)B"
create "${BEFORE}"
export BACKUP_TARGET="${BACKUP_TARGET:-./backups}"
OUT="$(scripts/backup.sh)"; echo "${OUT}"
ARCHIVE="$(echo "${OUT}" | awk '{print $2}')"
create "${AFTER}"
scripts/restore.sh "${ARCHIVE}"
TOKEN=$(curl -sf -X POST "${TOKEN_URL}" -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin \
  --data-urlencode 'password=Password1!' | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])')
[[ "$(status_of "${BEFORE}")" == 200 ]] || { echo "FAIL: ${BEFORE} missing after restore"; exit 1; }
[[ "$(status_of "${AFTER}")" == 404 ]] || { echo "FAIL: ${AFTER} still present after restore"; exit 1; }
echo "PASS backup and restore round trip"
