#!/usr/bin/env bash
# Smoke test of the finish line (build profile): token for ga-admin, create a vocabulary, create a new
# version, list, download, search; ga-viewer gets 403 on create; no token gets 401.
set -euo pipefail
API_URL=${API_URL:-http://localhost:8196/v1}
TOKEN_URL=${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}
TOKEN_CLIENT=${TOKEN_CLIENT:-vocabulary-tests}
RUN=$(date +%s)
NAME="Smoke${RUN}"
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
fail=0

check() { # name expected actual
  if [ "$2" = "$3" ]; then echo "PASS $1 ($3)"; else echo "FAIL $1: expected $2, got $3"; fail=1; fi
}

token() {
  curl -s -X POST "$TOKEN_URL" -d grant_type=password -d client_id="$TOKEN_CLIENT" \
    --data-urlencode "username=$1" --data-urlencode 'password=Password1!' |
    python3 -c 'import sys, json; print(json.load(sys.stdin)["access_token"])'
}

ADMIN=$(token ga-admin)
VIEWER=$(token ga-viewer)

cat > "$WORK/v1.ttl" <<'TTL'
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/smoke#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .
TTL
cat > "$WORK/v2.ttl" <<'TTL'
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/smoke#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .
ex:Concept a skos:Concept ; skos:inScheme ex:Scheme ; skos:prefLabel "Concept"@en .
TTL

code=$(curl -s -o "$WORK/create.json" -w '%{http_code}' -X POST "$API_URL/vocabularies" -H "Authorization: Bearer $ADMIN" \
  --form-string "name=$NAME" --form-string 'description=Smoke test vocabulary' --form-string 'changelog=Initial version.' \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle")
check "create vocabulary" 201 "$code"

code=$(curl -s -o "$WORK/version.json" -w '%{http_code}' -X POST "$API_URL/vocabularies/$NAME/versions" -H "Authorization: Bearer $ADMIN" \
  --form-string 'description=Smoke test vocabulary with a concept' --form-string 'changelog=Added a concept.' \
  -F "vocabularyFile=@$WORK/v2.ttl;type=text/turtle")
check "create new version" 201 "$code"
check "new version number" 2 "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["version"])' "$WORK/version.json")"

code=$(curl -s -o "$WORK/list.json" -w '%{http_code}' "$API_URL/vocabularies?name=$NAME" -H "Authorization: Bearer $VIEWER")
check "list vocabularies" 200 "$code"
check "list total" 1 "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["total"])' "$WORK/list.json")"

code=$(curl -s -o "$WORK/versions.json" -w '%{http_code}' "$API_URL/vocabularies/$NAME/versions" -H "Authorization: Bearer $VIEWER")
check "version history" 200 "$code"
check "version 1 deprecated" DEPRECATED "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["items"][1]["status"])' "$WORK/versions.json")"

code=$(curl -s -o "$WORK/download.ttl" -w '%{http_code}' "$API_URL/content/$NAME" -H "Authorization: Bearer $VIEWER")
check "download latest" 200 "$code"
if cmp -s "$WORK/download.ttl" "$WORK/v2.ttl"; then echo "PASS download bytes equal upload"; else echo "FAIL download bytes differ"; fail=1; fi

code=$(curl -s -o "$WORK/search.json" -w '%{http_code}' -X POST "$API_URL/vocabularies/searches" -H "Authorization: Bearer $VIEWER" \
  -H 'Content-Type: application/json' -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}")
check "search" 200 "$code"
check "search total" 2 "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["total"])' "$WORK/search.json")"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API_URL/vocabularies" -H "Authorization: Bearer $VIEWER" \
  --form-string "name=${NAME}Viewer" --form-string 'description=d' --form-string 'changelog=c' -F "vocabularyFile=@$WORK/v1.ttl")
check "viewer create forbidden" 403 "$code"

code=$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/vocabularies")
check "no token" 401 "$code"

exit $fail
