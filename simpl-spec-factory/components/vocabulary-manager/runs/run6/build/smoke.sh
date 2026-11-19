#!/usr/bin/env bash
# Smoke test of the Vocabulary Manager stack (finish line of the build profile).
# Main flow: token for ga-admin, create a vocabulary, create a new version, list, download, search.
# Access: ga-viewer gets 403 on create, no token gets 401.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
PASSWORD="${TEST_PASSWORD:-Password1!}"
RUN_ID="$(date +%s)$RANDOM"
NAME="Smoke${RUN_ID}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
FAILURES=0

token() {
  curl -sS -X POST "$TOKEN_URL" \
    --data-urlencode "grant_type=password" --data-urlencode "client_id=$TOKEN_CLIENT" \
    --data-urlencode "username=$1" --data-urlencode "password=$PASSWORD" --data-urlencode "scope=openid" \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}

check() { # check <label> <expected> <actual>
  if [ "$2" = "$3" ]; then echo "PASS $1 ($3)"; else echo "FAIL $1: expected $2, got $3"; FAILURES=$((FAILURES + 1)); fi
}

cat > "$TMP/v1.ttl" <<EOF
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.org/${NAME}#> .
ex:Thing a owl:Class ; rdfs:label "Thing"@en ; skos:prefLabel "Thing"@en .
EOF
cat > "$TMP/v2.ttl" <<EOF
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix ex: <https://example.org/${NAME}#> .
ex:Thing a owl:Class ; rdfs:label "Thing"@en .
ex:Other a owl:Class ; rdfs:label "Other"@en .
EOF

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"
echo "Tokens obtained for ga-admin and ga-viewer"

code=$(curl -sS -o "$TMP/create.json" -w '%{http_code}' -X POST "$API_URL/vocabularies" -H "Authorization: Bearer $ADMIN" \
  --form-string "name=$NAME" --form-string "description=Smoke vocabulary" --form-string "changelog=Initial version." \
  -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
check "create vocabulary" 201 "$code"

code=$(curl -sS -o "$TMP/version.json" -w '%{http_code}' -X POST "$API_URL/vocabularies/$NAME/versions" -H "Authorization: Bearer $ADMIN" \
  --form-string "description=Smoke vocabulary, second version" --form-string "changelog=Added Other." \
  -F "vocabularyFile=@$TMP/v2.ttl;type=text/turtle")
check "create version" 201 "$code"
check "new version number" 2 "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["version"])' "$TMP/version.json")"

code=$(curl -sS -o "$TMP/list.json" -w '%{http_code}' "$API_URL/vocabularies?name=$NAME" -H "Authorization: Bearer $VIEWER")
check "list vocabularies" 200 "$code"
check "list finds the vocabulary" "1 2" "$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(d["total"], d["items"][0]["version"])' "$TMP/list.json")"

code=$(curl -sS -o "$TMP/download.ttl" -w '%{http_code}' "$API_URL/content/$NAME/versions/1" -H "Authorization: Bearer $VIEWER")
check "download version 1" 200 "$code"
if cmp -s "$TMP/v1.ttl" "$TMP/download.ttl"; then echo "PASS download returns the uploaded bytes"; else echo "FAIL download bytes differ"; FAILURES=$((FAILURES + 1)); fi

code=$(curl -sS -o "$TMP/search.json" -w '%{http_code}' -X POST "$API_URL/vocabularies/searches" -H "Authorization: Bearer $VIEWER" \
  -H 'Content-Type: application/json' -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}")
check "search vocabularies" 200 "$code"
check "search results (version, status)" "2:ACTIVE 1:DEPRECATED" "$(python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); print(" ".join(str(i["version"]) + ":" + i["status"] for i in d["items"]))' "$TMP/search.json")"

code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$API_URL/vocabularies" -H "Authorization: Bearer $VIEWER" \
  --form-string "name=${NAME}Viewer" --form-string "description=x" --form-string "changelog=x" -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
check "ga-viewer create is forbidden" 403 "$code"

code=$(curl -sS -o /dev/null -w '%{http_code}' "$API_URL/vocabularies")
check "no token is unauthorized" 401 "$code"

if [ "$FAILURES" -eq 0 ]; then echo "SMOKE OK"; else echo "SMOKE FAILED: $FAILURES check(s)"; exit 1; fi
