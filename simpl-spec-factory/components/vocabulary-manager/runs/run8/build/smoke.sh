#!/usr/bin/env bash
# Smoke test of the main flow (finish line of the build profile): token for ga-admin, create a vocabulary,
# create a new version, list, download, search; ga-viewer gets 403 on create; no token gets 401.
set -euo pipefail
API_URL=${API_URL:-http://localhost:8196/v1}
TOKEN_URL=${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}
TOKEN_CLIENT=${TOKEN_CLIENT:-vocabulary-tests}
RUN=$(python3 -c 'import secrets; print(secrets.token_hex(4))')
NAME="Smoke${RUN}"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
fail() { echo "FAIL: $*" >&2; exit 1; }
ok() { echo "ok   $*"; }

token() {
  curl -sf "$TOKEN_URL" --data-urlencode grant_type=password --data-urlencode "client_id=$TOKEN_CLIENT" \
    --data-urlencode "username=$1" --data-urlencode 'password=Password1!' --data-urlencode scope=openid \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}
expect() { # expect <status> <actual> <what>
  [[ "$2" == "$1" ]] || { cat "$TMP/body" >&2 2>/dev/null || true; fail "$3: expected $1, got $2"; }
  ok "$3 -> $2"
}

ADMIN=$(token ga-admin)
VIEWER=$(token ga-viewer)

cat > "$TMP/v1.ttl" <<TTL
@prefix ex: <https://example.org/${RUN}#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
ex:Concept a skos:Concept ; skos:prefLabel "Concept"@en .
TTL
cat > "$TMP/v2.ttl" <<TTL
@prefix ex: <https://example.org/${RUN}#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
ex:Concept a skos:Concept ; skos:prefLabel "Concept"@en ; skos:prefLabel "Begrip"@nl .
TTL

s=$(curl -s -o "$TMP/body" -w '%{http_code}' "$API_URL/vocabularies")
expect 401 "$s" "GET /vocabularies without a token"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies" \
  -F "name=$NAME" -F 'description=Smoke test' -F 'changelog=Initial version.' -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
expect 403 "$s" "POST /vocabularies as ga-viewer"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" "$API_URL/vocabularies" \
  -F "name=$NAME" -F 'description=Smoke test' -F 'changelog=Initial version.' -F "vocabularyFile=@$TMP/v1.ttl;type=text/turtle")
expect 201 "$s" "POST /vocabularies as ga-admin"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $ADMIN" "$API_URL/vocabularies/$NAME/versions" \
  -F 'description=Smoke test, Dutch label' -F 'changelog=Dutch label added.' -F "vocabularyFile=@$TMP/v2.ttl;type=text/turtle")
expect 201 "$s" "POST /vocabularies/$NAME/versions"
grep -q '"version":2' "$TMP/body" || fail "new version is not version 2"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies?name=$NAME")
expect 200 "$s" "GET /vocabularies?name=$NAME as ga-viewer"
grep -q "\"name\":\"$NAME\"" "$TMP/body" || fail "list does not hold $NAME"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/content/$NAME")
expect 200 "$s" "GET /content/$NAME (download latest)"
cmp -s "$TMP/body" "$TMP/v2.ttl" || fail "downloaded content differs from version 2"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies/$NAME/versions")
expect 200 "$s" "GET /vocabularies/$NAME/versions"

s=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Authorization: Bearer $VIEWER" -H 'Content-Type: application/json' \
  "$API_URL/vocabularies/searches" -d "{\"items\":[{\"name\":\"$NAME\"},{\"name\":\"$NAME\",\"version\":1}]}")
expect 200 "$s" "POST /vocabularies/searches"
python3 - "$TMP/body" <<'PY' || fail "search result is not as expected"
import json, sys
items = json.load(open(sys.argv[1]))["items"]
assert len(items) == 2, items
assert items[0]["version"] == 2 and items[0]["status"] == "ACTIVE", items[0]
assert items[1]["version"] == 1 and items[1]["status"] == "DEPRECATED", items[1]
PY
ok "search returns latest ACTIVE v2 and deprecated v1"

echo "SMOKE PASSED ($NAME)"
