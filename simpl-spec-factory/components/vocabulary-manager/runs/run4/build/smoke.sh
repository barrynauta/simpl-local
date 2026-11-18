#!/usr/bin/env bash
# Smoke test of the Vocabulary Manager (finish line of the build profile).
# Main flow: token for ga-admin, create a vocabulary, create a new version, list, download, search.
# Access: ga-viewer gets 403 on create, no token gets 401.
set -euo pipefail

API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
PASSWORD="${PASSWORD:-Password1!}"
RUN_ID="$(date +%s)$RANDOM"
NAME="Smoke${RUN_ID}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAILURES=0

pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; FAILURES=$((FAILURES + 1)); }
expect() { # expect <label> <expected> <actual>
  if [ "$2" = "$3" ]; then pass "$1 ($3)"; else fail "$1: expected $2, got $3"; fi
}
json() { python3 -c 'import sys, json; d = json.load(open(sys.argv[1])); print(eval("d" + sys.argv[2]))' "$1" "$2"; }

token() {
  curl -fsS -X POST "$TOKEN_URL" \
    --data-urlencode grant_type=password \
    --data-urlencode "client_id=$TOKEN_CLIENT" \
    --data-urlencode "username=$1" \
    --data-urlencode "password=$PASSWORD" \
    -o "$WORK/token.json"
  json "$WORK/token.json" "['access_token']"
}

cat > "$WORK/v1.ttl" <<TTL
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix smoke: <https://example.eu/smoke/${RUN_ID}#> .
smoke:Scheme a skos:ConceptScheme ;
  skos:prefLabel "Smoke scheme"@en .
TTL
cat > "$WORK/v2.ttl" <<TTL
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix smoke: <https://example.eu/smoke/${RUN_ID}#> .
smoke:Scheme a skos:ConceptScheme ;
  skos:prefLabel "Smoke scheme, version 2"@en .
smoke:Concept a skos:Concept ; skos:inScheme smoke:Scheme .
TTL

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"
pass "tokens for ga-admin and ga-viewer"

code=$(curl -s -o "$WORK/health.json" -w '%{http_code}' "$API_URL/health")
expect "GET /health" 200 "$code"

code=$(curl -s -o "$WORK/create.json" -D "$WORK/create.headers" -w '%{http_code}' \
  -H "Authorization: Bearer $ADMIN" \
  -F "name=$NAME" -F "description=Smoke test vocabulary" -F "changelog=Initial version." \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle" "$API_URL/vocabularies")
expect "POST /vocabularies creates the vocabulary" 201 "$code"
expect "version 1 is ACTIVE" "1 ACTIVE" "$(json "$WORK/create.json" "['version']") $(json "$WORK/create.json" "['status']")"
grep -qi "^location: /vocabularies/$NAME/versions/1" "$WORK/create.headers" \
  && pass "Location header of version 1" || fail "Location header of version 1"

code=$(curl -s -o "$WORK/version.json" -w '%{http_code}' \
  -H "Authorization: Bearer $ADMIN" \
  -F "description=Smoke test vocabulary, version 2" -F "changelog=Added a concept." \
  -F "vocabularyFile=@$WORK/v2.ttl;type=text/turtle" "$API_URL/vocabularies/$NAME/versions")
expect "POST .../versions creates version 2" 201 "$code"
expect "new version number" 2 "$(json "$WORK/version.json" "['version']")"

code=$(curl -s -o "$WORK/versions.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  "$API_URL/vocabularies/$NAME/versions")
expect "GET .../versions" 200 "$code"
expect "version 1 deprecated, version 2 active" "2:ACTIVE 1:DEPRECATED" \
  "$(python3 -c "import json; d=json.load(open('$WORK/versions.json')); print(' '.join(f\"{i['version']}:{i['status']}\" for i in d['items']))")"

code=$(curl -s -o "$WORK/list.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  "$API_URL/vocabularies?name=$NAME")
expect "GET /vocabularies?name=..." 200 "$code"
expect "list shows the latest version" "$NAME 2" \
  "$(json "$WORK/list.json" "['items'][0]['name']") $(json "$WORK/list.json" "['items'][0]['version']")"

code=$(curl -s -o "$WORK/download.ttl" -D "$WORK/download.headers" -w '%{http_code}' \
  -H "Authorization: Bearer $VIEWER" "$API_URL/content/$NAME")
expect "GET /content/{name}" 200 "$code"
cmp -s "$WORK/download.ttl" "$WORK/v2.ttl" && pass "download returns the uploaded bytes of version 2" \
  || fail "download returns the uploaded bytes of version 2"
grep -qi "filename=\"$NAME.ttl\"" "$WORK/download.headers" && pass "download filename" || fail "download filename"

code=$(curl -s -o "$WORK/search.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}" "$API_URL/vocabularies/searches")
expect "POST /vocabularies/searches" 200 "$code"
expect "search results" "2 True DEPRECATED" \
  "$(json "$WORK/search.json" "['total']") $(json "$WORK/search.json" "['items'][0]['latest']") $(json "$WORK/search.json" "['items'][1]['status']")"

code=$(curl -s -o "$WORK/forbidden.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  -F "name=${NAME}Viewer" -F "description=d" -F "changelog=c" \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle" "$API_URL/vocabularies")
expect "ga-viewer gets 403 on create" 403 "$code"

code=$(curl -s -o "$WORK/unauthorized.json" -w '%{http_code}' "$API_URL/vocabularies")
expect "no token gets 401" 401 "$code"

echo
if [ "$FAILURES" -eq 0 ]; then
  echo "Smoke test passed."
else
  echo "Smoke test failed: $FAILURES check(s)."
  exit 1
fi
