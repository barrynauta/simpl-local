#!/usr/bin/env bash
# Smoke check against a running stack (docker compose up -d --wait).
# Needs: bash, curl, python3 (only to read JSON).
set -euo pipefail

API=${API:-http://localhost:8097/v1}
TOKEN_URL=${TOKEN_URL:-http://localhost:8184/realms/simpl/protocol/openid-connect/token}
NAME=${NAME:-Smoke$(date +%s)}
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
FAIL=0

json() { python3 -c "import sys,json; d=json.load(sys.stdin); print(eval('d'+sys.argv[1]))" "$1"; }
token() {
  curl -sf -X POST "$TOKEN_URL" -d grant_type=password -d client_id=vocabulary-tests \
    -d username="$1" -d password=password | json "['access_token']"
}
check() { # check <label> <expected> <actual>
  if [ "$2" = "$3" ]; then echo "PASS  $1 ($3)"; else echo "FAIL  $1: expected $2, got $3"; FAIL=1; fi
}

echo "API $API, vocabulary name $NAME"
check "health is public" 200 "$(curl -s -o /dev/null -w '%{http_code}' "$API/health")"

ADMIN=$(token ga-admin)
VIEWER=$(token ga-viewer)
NOROLE=$(token ga-norole)

cat > "$TMP/v1.ttl" <<TTL
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix ex: <https://example.eu/smoke#> .
ex:SmokeOntology a owl:Ontology ; rdfs:label "Smoke test ontology" .
ex:Patient a owl:Class ; rdfs:label "Patient" .
TTL
cat > "$TMP/v2.ttl" <<TTL
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix ex: <https://example.eu/smoke#> .
ex:SmokeOntology a owl:Ontology ; rdfs:label "Smoke test ontology" .
ex:Patient a owl:Class ; rdfs:label "Patient" .
ex:Doctor a owl:Class ; rdfs:label "Doctor" .
TTL

code=$(curl -s -o "$TMP/create.json" -w '%{http_code}' -X POST "$API/vocabularies" \
  -H "Authorization: Bearer $ADMIN" \
  -F name="$NAME" -F description="Smoke test vocabulary" -F changelog="Initial version" \
  -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
check "admin creates vocabulary" 201 "$code"
check "created version" 1 "$(json "['version']" < "$TMP/create.json")"

code=$(curl -s -o "$TMP/v2.json" -w '%{http_code}' -X POST "$API/vocabularies/$NAME/versions" \
  -H "Authorization: Bearer $ADMIN" \
  -F description="Smoke test vocabulary, with doctors" -F changelog="Added Doctor" \
  -F "vocabularyFile=@$TMP/v2.ttl;type=text/turtle")
check "admin creates version 2" 201 "$code"
check "new version number" 2 "$(json "['version']" < "$TMP/v2.json")"

curl -s "$API/vocabularies/$NAME/versions" -H "Authorization: Bearer $VIEWER" > "$TMP/versions.json"
check "version 2 status" ACTIVE "$(json "['items'][0]['status']" < "$TMP/versions.json")"
check "version 1 status" DEPRECATED "$(json "['items'][1]['status']" < "$TMP/versions.json")"

curl -s "$API/vocabularies?name=$NAME" -H "Authorization: Bearer $VIEWER" > "$TMP/list.json"
check "list finds the vocabulary" 1 "$(json "['total']" < "$TMP/list.json")"

code=$(curl -s -o "$TMP/dl.ttl" -w '%{http_code}' "$API/content/$NAME" -H "Authorization: Bearer $VIEWER")
check "download latest" 200 "$code"
check "downloaded content is version 2" 1 "$(grep -c 'ex:Doctor' "$TMP/dl.ttl")"
code=$(curl -s -o "$TMP/dl1.ttl" -w '%{http_code}' "$API/content/$NAME/versions/1" -H "Authorization: Bearer $VIEWER")
check "download version 1" 200 "$code"
check "version 1 content unchanged" "" "$(diff "$TMP/v1.ttl" "$TMP/dl1.ttl")"

code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/vocabularies" \
  -H "Authorization: Bearer $VIEWER" \
  -F name="${NAME}Viewer" -F description="x" -F changelog="x" \
  -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
check "viewer cannot create (403)" 403 "$code"
check "user without role cannot list (403)" 403 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$API/vocabularies" -H "Authorization: Bearer $NOROLE")"
check "no token (401)" 401 "$(curl -s -o /dev/null -w '%{http_code}' "$API/vocabularies")"
check "401 is Problem Details" "application/problem+json" \
  "$(curl -s -o /dev/null -w '%{content_type}' "$API/vocabularies")"

if [ "$FAIL" = 0 ]; then echo "SMOKE OK"; else echo "SMOKE FAILED"; exit 1; fi
