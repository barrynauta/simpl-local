#!/usr/bin/env bash
# Smoke check for a running stack (docker compose up -d --wait). Exits non-zero on the first failure.
set -euo pipefail

API=${API:-http://localhost:8096/v1}
UI=${UI:-http://localhost:4333}
TOKEN_URL=${TOKEN_URL:-http://localhost:8183/realms/simpl/protocol/openid-connect/token}
NAME="Smoke$(date +%s)"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; exit 1; }
json() { python3 -c "import json,sys; d=json.load(sys.stdin); print(eval(sys.argv[1], {'d': d}))" "$1"; }

token() {
  curl -fsS -X POST "$TOKEN_URL" -d grant_type=password -d client_id=vocabulary-tests \
    -d username="$1" -d password=password | json "d['access_token']"
}

expect() { # expect <expected-status> <actual-status> <label>
  [ "$1" = "$2" ] && pass "$3 ($2)" || fail "$3: expected $1, got $2 (body: $(cat "$TMP/body" 2>/dev/null | head -c 400))"
}

ADMIN=$(token ga-admin);   pass "token for ga-admin"
VIEWER=$(token ga-viewer); pass "token for ga-viewer"
NOROLE=$(token ga-norole); pass "token for ga-norole"

cat > "$TMP/v1.ttl" <<TTL
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix ex: <https://example.org/smoke#> .
ex:Ontology a owl:Ontology ; rdfs:label "Smoke test vocabulary" .
ex:Person a owl:Class .
TTL
cat > "$TMP/v2.ttl" <<TTL
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix ex: <https://example.org/smoke#> .
ex:Ontology a owl:Ontology ; rdfs:label "Smoke test vocabulary, version 2" .
ex:Person a owl:Class .
ex:Address a owl:Class .
TTL

code=$(curl -s -o "$TMP/body" -w '%{http_code}' "$API/health"); expect 200 "$code" "health is public"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" -X POST "$API/vocabularies" \
  -F name="$NAME" -F description="Smoke test" -F changelog="Initial version" \
  -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
expect 201 "$code" "ga-admin creates $NAME"
[ "$(json "d['version']" < "$TMP/body")" = 1 ] && pass "created version is 1" || fail "version is not 1"
[ "$(json "d['status']" < "$TMP/body")" = ACTIVE ] && pass "created version is ACTIVE" || fail "status is not ACTIVE"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" -X POST \
  "$API/vocabularies/$NAME/versions" -F description="Smoke test v2" -F changelog="Added Address" \
  -F "vocabularyFile=@$TMP/v2.ttl;type=text/turtle")
expect 201 "$code" "ga-admin creates a second version"
[ "$(json "d['version']" < "$TMP/body")" = 2 ] && pass "new version is 2" || fail "new version is not 2"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API/vocabularies/$NAME/versions/1")
expect 200 "$code" "ga-viewer reads version 1"
[ "$(json "d['status']" < "$TMP/body")" = DEPRECATED ] && pass "version 1 is DEPRECATED" || fail "version 1 not DEPRECATED"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API/vocabularies?name=$NAME")
expect 200 "$code" "ga-viewer lists vocabularies"
[ "$(json "d['items'][0]['version']" < "$TMP/body")" = 2 ] && pass "list shows version 2" || fail "list does not show v2"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API/content/$NAME")
expect 200 "$code" "download latest content"
grep -q "ex:Address" "$TMP/body" && pass "downloaded content is version 2" || fail "downloaded content is not v2"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API/content/$NAME/versions/1")
expect 200 "$code" "download version 1"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" -X POST "$API/vocabularies" \
  -F name="${NAME}Viewer" -F description="x" -F changelog="x" -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
expect 403 "$code" "ga-viewer cannot create"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $NOROLE" "$API/vocabularies")
expect 403 "$code" "ga-norole cannot read"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' "$API/vocabularies")
expect 401 "$code" "no token gets 401"
grep -q '"status":401' "$TMP/body" && pass "401 body is Problem Details" || fail "401 body is not Problem Details"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" "$UI/v1/vocabularies/$NAME")
expect 200 "$code" "UI proxy forwards the API"
code=$(curl -s -o "$TMP/body" -w '%{http_code}' "$UI/")
expect 200 "$code" "UI serves the application"

echo "Smoke check passed."
