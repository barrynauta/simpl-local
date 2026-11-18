#!/usr/bin/env bash
# Smoke test of the finish line: main flow of the decisions plus 401 and 403.
# Needs: curl, python3. Uses API_URL, TOKEN_URL and TOKEN_CLIENT from the environment (build profile defaults).
set -euo pipefail

API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
TOKEN_CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
PASSWORD="${TEST_PASSWORD:-Password1!}"
RUN_ID="$(date +%s)$RANDOM"
NAME="Smoke${RUN_ID}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAILURES=0

pass() { printf 'PASS %s\n' "$1"; }
fail() { printf 'FAIL %s\n' "$1"; FAILURES=$((FAILURES + 1)); }
expect() { # expect <description> <expected status> <actual status>
  if [ "$2" = "$3" ]; then pass "$1 ($3)"; else fail "$1: expected $2, got $3"; fi
}
token() {
  curl -sS -X POST "$TOKEN_URL" -d grant_type=password -d client_id="$TOKEN_CLIENT" \
    --data-urlencode username="$1" --data-urlencode password="$PASSWORD" -d scope=openid |
    python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}
json_field() { python3 -c "import json,sys; d=json.load(open('$1')); print($2)"; }

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"
[ -n "$ADMIN" ] && pass "token for ga-admin" || fail "token for ga-admin"

cat > "$WORK/v1.ttl" <<TTL
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/${NAME}#> .
ex:Scheme a skos:ConceptScheme ;
  skos:prefLabel "Smoke scheme"@en .
TTL
cat > "$WORK/v2.ttl" <<TTL
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/${NAME}#> .
ex:Scheme a skos:ConceptScheme ;
  skos:prefLabel "Smoke scheme, second version"@en .
ex:Concept a skos:Concept ; skos:inScheme ex:Scheme ; skos:prefLabel "Concept"@en .
TTL

# 1. Create a vocabulary.
status=$(curl -sS -o "$WORK/create.json" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
  -F "name=$NAME" -F "description=Smoke test vocabulary" -F "changelog=Initial version." \
  -F "vocabularyFile=@$WORK/v1.ttl;type=text/turtle" "$API_URL/vocabularies")
expect "create vocabulary" 201 "$status"
[ "$(json_field "$WORK/create.json" '(d["version"], d["status"])')" = "(1, 'ACTIVE')" ] \
  && pass "version 1 is ACTIVE" || fail "version 1 is ACTIVE: $(cat "$WORK/create.json")"

# 2. Create a new version.
status=$(curl -sS -o "$WORK/version.json" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" \
  -F "description=Smoke test vocabulary, second version" -F "changelog=Added a concept." \
  -F "vocabularyFile=@$WORK/v2.ttl;type=text/turtle" "$API_URL/vocabularies/$NAME/versions")
expect "create new version" 201 "$status"
[ "$(json_field "$WORK/version.json" 'd["version"]')" = "2" ] && pass "version 2 assigned" || fail "version 2 assigned"

status=$(curl -sS -o "$WORK/versions.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  "$API_URL/vocabularies/$NAME/versions")
expect "version history" 200 "$status"
[ "$(json_field "$WORK/versions.json" '[(i["version"], i["status"]) for i in d["items"]]')" = "[(2, 'ACTIVE'), (1, 'DEPRECATED')]" ] \
  && pass "older version deprecated" || fail "older version deprecated: $(cat "$WORK/versions.json")"

# 3. List with a name filter.
status=$(curl -sS -o "$WORK/list.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  "$API_URL/vocabularies?name=$NAME")
expect "list vocabularies" 200 "$status"
[ "$(json_field "$WORK/list.json" '(d["total"], d["items"][0]["version"])')" = "(1, 2)" ] \
  && pass "list shows latest version 2" || fail "list shows latest version: $(cat "$WORK/list.json")"

# 4. Download the latest and a specific version.
status=$(curl -sS -o "$WORK/latest.ttl" -D "$WORK/headers.txt" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  "$API_URL/content/$NAME")
expect "download latest" 200 "$status"
cmp -s "$WORK/latest.ttl" "$WORK/v2.ttl" && pass "downloaded bytes equal upload" || fail "downloaded bytes equal upload"
grep -qi "filename=\"$NAME.ttl\"" "$WORK/headers.txt" && pass "attachment file name" || fail "attachment file name"
status=$(curl -sS -o "$WORK/v1-down.ttl" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  "$API_URL/content/$NAME/versions/1")
expect "download version 1" 200 "$status"
cmp -s "$WORK/v1-down.ttl" "$WORK/v1.ttl" && pass "version 1 bytes" || fail "version 1 bytes"

# 5. Search.
status=$(curl -sS -o "$WORK/search.json" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  -H 'Content-Type: application/json' -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}" \
  "$API_URL/vocabularies/searches")
expect "search vocabularies" 200 "$status"
[ "$(json_field "$WORK/search.json" '[(i["version"], i["status"], i["latest"]) for i in d["items"]]')" = "[(2, 'ACTIVE', True), (1, 'DEPRECATED', False)]" ] \
  && pass "search results" || fail "search results: $(cat "$WORK/search.json")"

# 6. Access rules.
status=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $VIEWER" \
  -F "name=${NAME}Viewer" -F "description=Should fail" -F "changelog=Initial version." \
  -F "vocabularyFile=@$WORK/v1.ttl" "$API_URL/vocabularies")
expect "viewer cannot create (403)" 403 "$status"
status=$(curl -sS -o /dev/null -w '%{http_code}' "$API_URL/vocabularies")
expect "no token (401)" 401 "$status"
status=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $(token ga-norole)" "$API_URL/vocabularies")
expect "user without role (403)" 403 "$status"

if [ "$FAILURES" -gt 0 ]; then
  echo "smoke: $FAILURES failure(s)"
  exit 1
fi
echo "smoke: all checks passed"
