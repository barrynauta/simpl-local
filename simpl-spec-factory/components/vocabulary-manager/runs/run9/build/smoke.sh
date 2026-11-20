#!/usr/bin/env bash
# Smoke test of the finish line: token for ga-admin, create a vocabulary, create a new version, list, download,
# search; ga-viewer gets 403 on create; no token gets 401. Exits non-zero on the first failure.
set -euo pipefail
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
RUN="$(date +%s)$RANDOM"
NAME="Smoke${RUN}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
fail() { echo "FAIL: $*" >&2; exit 1; }
pass() { echo "ok   $*"; }

token() {
  curl -sf -X POST "$TOKEN_URL" --data-urlencode grant_type=password --data-urlencode "client_id=$CLIENT" \
    --data-urlencode "username=$1" --data-urlencode 'password=Password1!' --data-urlencode scope=openid \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}
expect() { [ "$1" = "$2" ] || fail "$3: expected $2, got $1 ($(cat "$TMP/body" 2>/dev/null | head -c 400))"; pass "$3 -> $1"; }

ADMIN="$(token ga-admin)"; VIEWER="$(token ga-viewer)"
pass "tokens for ga-admin and ga-viewer"

cat > "$TMP/v1.ttl" <<TTL
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/${NAME}#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .
TTL
cat > "$TMP/v2.ttl" <<TTL
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix ex: <https://example.eu/${NAME}#> .
ex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .
ex:Concept a skos:Concept ; skos:inScheme ex:Scheme ; skos:prefLabel "Concept"@en .
TTL

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -X POST "$API_URL/vocabularies" -F "vocabularyFile=@$TMP/v1.ttl" \
  --form-string "name=$NAME" --form-string 'description=Smoke vocabulary' --form-string 'changelog=Initial version.')
expect "$code" 401 "create without token"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" -X POST "$API_URL/vocabularies" \
  -F "vocabularyFile=@$TMP/v1.ttl" --form-string "name=$NAME" --form-string 'description=Smoke vocabulary' \
  --form-string 'changelog=Initial version.')
expect "$code" 403 "create as ga-viewer"

code=$(curl -s -o "$TMP/body" -D "$TMP/headers" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" -X POST "$API_URL/vocabularies" \
  -F "vocabularyFile=@$TMP/v1.ttl" --form-string "name=$NAME" --form-string 'description=Smoke vocabulary' \
  --form-string 'changelog=Initial version.')
expect "$code" 201 "create vocabulary $NAME"
grep -qi "^location: /vocabularies/$NAME/versions/1" "$TMP/headers" || fail "Location header of version 1"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" -X POST "$API_URL/vocabularies/$NAME/versions" \
  -F "vocabularyFile=@$TMP/v2.ttl" --form-string 'description=Smoke vocabulary with a concept' --form-string 'changelog=Added a concept.')
expect "$code" 201 "create version 2"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["version"]==2 and d["status"]=="ACTIVE", d' "$TMP/body"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies?name=$NAME")
expect "$code" 200 "list with name filter (viewer)"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["total"]==1 and d["items"][0]["version"]==2, d' "$TMP/body"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies/$NAME/versions")
expect "$code" 200 "version history"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); s=[(i["version"],i["status"]) for i in d["items"]]; assert s==[(2,"ACTIVE"),(1,"DEPRECATED")], s' "$TMP/body"

code=$(curl -s -o "$TMP/download.ttl" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/content/$NAME")
cp "$TMP/download.ttl" "$TMP/body"; expect "$code" 200 "download latest"
cmp -s "$TMP/download.ttl" "$TMP/v2.ttl" || fail "downloaded content differs from the upload"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" -H 'Content-Type: application/json' \
  -X POST "$API_URL/vocabularies/searches" -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}")
expect "$code" 200 "search"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); s=[(i["version"],i["status"],i["latest"]) for i in d["items"]]; assert s==[(2,"ACTIVE",True),(1,"DEPRECATED",False)], s' "$TMP/body"

code=$(curl -s -o "$TMP/body" -w '%{http_code}' "$API_URL/vocabularies")
expect "$code" 401 "list without token"
echo "smoke passed ($NAME)"
