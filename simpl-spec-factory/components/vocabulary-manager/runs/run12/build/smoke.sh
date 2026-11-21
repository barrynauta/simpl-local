#!/usr/bin/env bash
# Smoke test of the main flow (build profile finish line): token for ga-admin, create a vocabulary,
# create a new version, list, download, search; ga-viewer gets 403 on create; no token gets 401.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
RUN="Smoke$(date +%s)$RANDOM"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FAIL=0

token() {
  curl -sf -X POST "$TOKEN_URL" -d grant_type=password -d "client_id=$CLIENT" \
    --data-urlencode "username=$1" --data-urlencode 'password=Password1!' \
    | sed -E 's/.*"access_token":"([^"]+)".*/\1/'
}

check() { # name expected actual
  if [[ "$2" == "$3" ]]; then echo "ok   $1 ($3)"; else echo "FAIL $1: expected $2, got $3"; FAIL=1; fi
}

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"

cat > "$TMP/v1.ttl" <<EOF
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.org/${RUN}#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .
EOF
cat > "$TMP/v2.ttl" <<EOF
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.org/${RUN}#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme, version 2"@en .
EOF

code=$(curl -s -o "$TMP/create.json" -w '%{http_code}' -X POST "$API_URL/vocabularies" \
  -H "Authorization: Bearer $ADMIN" -F "name=$RUN" -F "description=Smoke vocabulary" \
  -F "changelog=Initial version." -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
check "create vocabulary" 201 "$code"
grep -q '"version":1' "$TMP/create.json" && grep -q '"validationWarnings":\[\]' "$TMP/create.json" \
  || { echo "FAIL create body: $(cat "$TMP/create.json")"; FAIL=1; }

code=$(curl -s -o "$TMP/version.json" -w '%{http_code}' -X POST "$API_URL/vocabularies/$RUN/versions" \
  -H "Authorization: Bearer $ADMIN" -F "description=Smoke vocabulary v2" \
  -F "changelog=Second label." -F "vocabularyFile=@$TMP/v2.ttl;type=text/turtle")
check "create new version" 201 "$code"
grep -q '"version":2' "$TMP/version.json" || { echo "FAIL version body: $(cat "$TMP/version.json")"; FAIL=1; }

code=$(curl -s -o "$TMP/list.json" -w '%{http_code}' "$API_URL/vocabularies?name=$RUN" \
  -H "Authorization: Bearer $VIEWER")
check "list (viewer)" 200 "$code"
grep -q "\"name\":\"$RUN\",\"version\":2" "$TMP/list.json" || { echo "FAIL list body: $(cat "$TMP/list.json")"; FAIL=1; }

code=$(curl -s -o "$TMP/versions.json" -w '%{http_code}' "$API_URL/vocabularies/$RUN/versions" \
  -H "Authorization: Bearer $ADMIN")
check "version history" 200 "$code"
grep -q '"version":1,[^}]*"status":"DEPRECATED"' "$TMP/versions.json" || { echo "FAIL v1 not deprecated: $(cat "$TMP/versions.json")"; FAIL=1; }

code=$(curl -s -o "$TMP/dl.ttl" -D "$TMP/dl.h" -w '%{http_code}' "$API_URL/content/$RUN" \
  -H "Authorization: Bearer $VIEWER")
check "download latest" 200 "$code"
cmp -s "$TMP/dl.ttl" "$TMP/v2.ttl" && echo "ok   download bytes equal" || { echo "FAIL download bytes differ"; FAIL=1; }

code=$(curl -s -o "$TMP/search.json" -w '%{http_code}' -X POST "$API_URL/vocabularies/searches" \
  -H "Authorization: Bearer $VIEWER" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"name\":\"$RUN\"},{\"name\":\"$RUN\",\"version\":1}]}")
check "search" 200 "$code"
grep -q '"total":2' "$TMP/search.json" || { echo "FAIL search body: $(cat "$TMP/search.json")"; FAIL=1; }

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/vocabularies" \
  -H "Authorization: Bearer $VIEWER" -F "name=${RUN}X" -F "description=x" -F "changelog=x" \
  -F "vocabularyFile=@$TMP/v1.ttl")
check "viewer create is 403" 403 "$code"

code=$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/vocabularies")
check "no token is 401" 401 "$code"

if [[ $FAIL -ne 0 ]]; then echo "SMOKE FAILED"; exit 1; fi
echo "SMOKE PASSED"
