#!/usr/bin/env bash
# Smoke test of the finish line: token for ga-admin, create a vocabulary, create a new version, list, download,
# search; ga-viewer gets 403 on create; no token gets 401; a user without a role gets 403.
# Environment (defaults from the build profile): API_URL, TOKEN_URL, TOKEN_CLIENT.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
RUN_ID="Smoke$(date +%s)$RANDOM"
WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT
FAILED=0

token() {
  curl -sf -X POST "${TOKEN_URL}" \
    --data-urlencode "grant_type=password" --data-urlencode "client_id=${TOKEN_CLIENT}" \
    --data-urlencode "username=$1" --data-urlencode 'password=Password1!' \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}

check() {
  local label="$1" expected="$2" actual="$3"
  if [[ "${expected}" == "${actual}" ]]; then
    echo "PASS ${label} (${actual})"
  else
    echo "FAIL ${label}: expected ${expected}, got ${actual}"
    FAILED=1
  fi
}

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"
NOROLE="$(token ga-norole)"

cat > "${WORK}/v1.ttl" <<EOF
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.org/${RUN_ID}#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "${RUN_ID}"@en .
EOF
cp "${WORK}/v1.ttl" "${WORK}/v2.ttl"
echo 'ex:Concept a skos:Concept ; skos:inScheme ex:Scheme .' >> "${WORK}/v2.ttl"

status=$(curl -s -o "${WORK}/create.json" -w '%{http_code}' -H "Authorization: Bearer ${ADMIN}" \
  --form-string "name=${RUN_ID}" --form-string "description=Smoke vocabulary" \
  --form-string "changelog=Initial version." -F "vocabularyFile=@${WORK}/v1.ttl;type=text/turtle" \
  "${API_URL}/vocabularies")
check "create vocabulary" 201 "${status}"
check "created version 1 ACTIVE" "1 ACTIVE" "$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(d["version"], d["status"])' "${WORK}/create.json")"

status=$(curl -s -o "${WORK}/version.json" -w '%{http_code}' -H "Authorization: Bearer ${ADMIN}" \
  --form-string "description=Smoke vocabulary v2" --form-string "changelog=Added a concept." \
  -F "vocabularyFile=@${WORK}/v2.ttl;type=text/turtle" "${API_URL}/vocabularies/${RUN_ID}/versions")
check "create new version" 201 "${status}"
check "version 1 deprecated" DEPRECATED "$(curl -s -H "Authorization: Bearer ${VIEWER}" \
  "${API_URL}/vocabularies/${RUN_ID}/versions/1" | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])')"

check "list with name filter" "1 2" "$(curl -s -H "Authorization: Bearer ${VIEWER}" \
  "${API_URL}/vocabularies?name=${RUN_ID}" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d["total"], d["items"][0]["version"])')"

curl -s -o "${WORK}/download.ttl" -H "Authorization: Bearer ${VIEWER}" "${API_URL}/content/${RUN_ID}"
if cmp -s "${WORK}/download.ttl" "${WORK}/v2.ttl"; then check "download latest" same same; else check "download latest" same different; fi

check "search" "2 True" "$(curl -s -H "Authorization: Bearer ${VIEWER}" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"name\":\"${RUN_ID}\"}]}" "${API_URL}/vocabularies/searches" \
  | python3 -c 'import json,sys; d=json.load(sys.stdin)["items"][0]; print(d["version"], d["latest"])')"

check "viewer create gives 403" 403 "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${VIEWER}" \
  --form-string "name=${RUN_ID}X" --form-string "description=d" --form-string "changelog=c" \
  -F "vocabularyFile=@${WORK}/v1.ttl" "${API_URL}/vocabularies")"
check "no token gives 401" 401 "$(curl -s -o /dev/null -w '%{http_code}' "${API_URL}/vocabularies")"
check "no role gives 403" 403 "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer ${NOROLE}" \
  "${API_URL}/vocabularies")"

exit "${FAILED}"
