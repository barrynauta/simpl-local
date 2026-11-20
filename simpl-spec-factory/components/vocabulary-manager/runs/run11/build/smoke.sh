#!/usr/bin/env bash
# Smoke test of the running stack (finish line): token for ga-admin, create a vocabulary, create a
# new version, list, download, search; ga-viewer gets 403 on create; no token gets 401.
# Environment (defaults from the build profile): API_URL, TOKEN_URL, TOKEN_CLIENT.
set -euo pipefail
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
PASS=0
FAIL=0

check() { # name expected actual
  if [[ "$2" == "$3" ]]; then
    echo "PASS $1 ($3)"
    PASS=$((PASS + 1))
  else
    echo "FAIL $1: expected $2, got $3"
    FAIL=$((FAIL + 1))
  fi
}

token() {
  curl -sf -d grant_type=password -d "client_id=$TOKEN_CLIENT" -d "username=$1" --data-urlencode 'password=Password1!' \
    "$TOKEN_URL" | python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])'
}

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"
NAME="Smoke$(date +%s)$RANDOM"

cat > "$WORK/v1.ttl" <<EOF
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.org/$NAME#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .
EOF
cat "$WORK/v1.ttl" > "$WORK/v2.ttl"
echo 'ex:Extra a skos:Concept ; skos:prefLabel "Extra"@en .' >> "$WORK/v2.ttl"

code=$(curl -s -o "$WORK/create.json" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
  -F "name=$NAME" -F "description=Smoke test vocabulary" -F "changelog=Initial version." \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle" "$API_URL/vocabularies")
check "create vocabulary" 201 "$code"

code=$(curl -s -o "$WORK/version.json" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
  -F "description=Smoke test vocabulary, version 2" -F "changelog=Added a concept." \
  -F "vocabularyFile=@$WORK/v2.ttl;type=text/turtle" "$API_URL/vocabularies/$NAME/versions")
check "create new version" 201 "$code"
check "new version number" 2 "$(python3 -c 'import sys, json; print(json.load(open(sys.argv[1]))["version"])' "$WORK/version.json")"

code=$(curl -s -o "$WORK/v1meta.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies/$NAME/versions/1")
check "version 1 readable" 200 "$code"
check "version 1 deprecated" DEPRECATED "$(python3 -c 'import sys, json; print(json.load(open(sys.argv[1]))["status"])' "$WORK/v1meta.json")"

code=$(curl -s -o "$WORK/list.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies?name=$NAME")
check "list" 200 "$code"
check "list finds it" 1 "$(python3 -c 'import sys, json; print(json.load(open(sys.argv[1]))["total"])' "$WORK/list.json")"

code=$(curl -s -o "$WORK/download.ttl" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/content/$NAME")
check "download latest" 200 "$code"
check "download bytes as uploaded" same "$(cmp -s "$WORK/download.ttl" "$WORK/v2.ttl" && echo same || echo different)"

code=$(curl -s -o "$WORK/search.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}" "$API_URL/vocabularies/searches")
check "search" 200 "$code"
check "search results" 2 "$(python3 -c 'import sys, json; print(json.load(open(sys.argv[1]))["total"])' "$WORK/search.json")"

code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  -F "name=${NAME}X" -F "description=d" -F "changelog=c" -F "vocabularyFile=@$WORK/v1.ttl" "$API_URL/vocabularies")
check "viewer create forbidden" 403 "$code"

code=$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/vocabularies")
check "no token" 401 "$code"

echo "smoke: $PASS passed, $FAIL failed"
[[ $FAIL -eq 0 ]]
