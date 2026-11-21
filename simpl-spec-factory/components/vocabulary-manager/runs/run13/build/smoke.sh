#!/usr/bin/env bash
# Smoke test of the finish line: main flow of the decisions with curl, plus 401 and 403.
# Environment: API_URL (default http://localhost:8196/v1), TOKEN_URL, TOKEN_CLIENT.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
RUN_ID="$(date +%s)$RANDOM"
NAME="Smoke${RUN_ID}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAILURES=0

token() {
  curl -sf -d grant_type=password -d "client_id=${TOKEN_CLIENT}" -d "username=$1" --data-urlencode 'password=Password1!' \
    "$TOKEN_URL" | sed -E 's/.*"access_token":"([^"]+)".*/\1/'
}

check() {
  local label="$1" expected="$2" actual="$3"
  if [[ "$expected" == "$actual" ]]; then
    echo "PASS  $label ($actual)"
  else
    echo "FAIL  $label: expected $expected, got $actual"
    FAILURES=$((FAILURES + 1))
  fi
}

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"

cat > "$WORK/v1.ttl" <<EOF
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/${NAME}#> .
ex:Scheme a skos:ConceptScheme ;
  skos:prefLabel "Smoke scheme"@en .
EOF
cp "$WORK/v1.ttl" "$WORK/v2.ttl"
echo 'ex:Concept a skos:Concept ; skos:inScheme ex:Scheme ; skos:prefLabel "Concept"@en .' >> "$WORK/v2.ttl"

code=$(curl -s -o "$WORK/create.json" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle" --form-string "name=$NAME" \
  --form-string "description=Smoke vocabulary." --form-string "changelog=Initial version." "$API_URL/vocabularies")
check "admin creates a vocabulary" 201 "$code"
grep -q '"version":1' "$WORK/create.json" && check "version 1 assigned" yes yes || check "version 1 assigned" yes no

code=$(curl -s -o "$WORK/version.json" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
  -F "vocabularyFile=@$WORK/v2.ttl;type=text/turtle" --form-string "description=Smoke vocabulary, version 2." \
  --form-string "changelog=Added a concept." "$API_URL/vocabularies/$NAME/versions")
check "admin creates a new version" 201 "$code"
grep -q '"version":2' "$WORK/version.json" && check "version 2 assigned" yes yes || check "version 2 assigned" yes no

code=$(curl -s -o "$WORK/list.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies?name=$NAME")
check "viewer lists vocabularies" 200 "$code"
grep -q "\"name\":\"$NAME\"" "$WORK/list.json" && check "list holds the vocabulary" yes yes || check "list holds the vocabulary" yes no

code=$(curl -s -o "$WORK/versions.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies/$NAME/versions")
check "viewer reads the version history" 200 "$code"
grep -q '"DEPRECATED"' "$WORK/versions.json" && check "version 1 deprecated" yes yes || check "version 1 deprecated" yes no

code=$(curl -s -o "$WORK/download.ttl" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/content/$NAME")
check "viewer downloads the latest content" 200 "$code"
cmp -s "$WORK/download.ttl" "$WORK/v2.ttl" && check "download equals upload" yes yes || check "download equals upload" yes no

code=$(curl -s -o "$WORK/search.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}" "$API_URL/vocabularies/searches")
check "viewer searches vocabularies" 200 "$code"
grep -q '"total":2' "$WORK/search.json" && check "search finds both references" yes yes || check "search finds both references" yes no

code=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle" --form-string "name=${NAME}B" \
  --form-string "description=x" --form-string "changelog=x" "$API_URL/vocabularies")
check "viewer gets 403 on create" 403 "$code"

code=$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/vocabularies")
check "no token gets 401" 401 "$code"

if [[ $FAILURES -gt 0 ]]; then
  echo "smoke: $FAILURES check(s) failed"
  exit 1
fi
echo "smoke: all checks passed"
