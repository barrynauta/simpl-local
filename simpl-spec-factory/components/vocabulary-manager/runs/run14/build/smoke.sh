#!/usr/bin/env bash
# Smoke test of the finish line: main flow of the decisions end to end with curl.
# token (ga-admin) -> create -> new version -> list -> download -> search; viewer 403 on create; no token 401.
set -euo pipefail
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
CLIENT="${TOKEN_CLIENT:-vocabulary-tests}"
RUN="Smoke$(date +%s)$RANDOM"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
fail() { echo "FAIL: $*" >&2; exit 1; }
pass() { echo "ok   $*"; }

token() {
  curl -sS -X POST "$TOKEN_URL" --data-urlencode grant_type=password --data-urlencode "client_id=$CLIENT" \
    --data-urlencode "username=$1" --data-urlencode 'password=Password1!' \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["access_token"])'
}
status_of() { curl -s -o "$WORK/body" -w '%{http_code}' "$@"; }
expect() { [ "$1" = "$2" ] || { cat "$WORK/body" >&2; fail "$3: expected $2, got $1"; }; pass "$3 ($1)"; }

ADMIN="$(token ga-admin)"; VIEWER="$(token ga-viewer)"
printf '@prefix ex: <https://example.org/%s#> .\n@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\nex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme"@en .\n' "$RUN" > "$WORK/v1.ttl"
printf '@prefix ex: <https://example.org/%s#> .\n@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\nex:Scheme a skos:ConceptScheme ; skos:prefLabel "Smoke scheme v2"@en .\n' "$RUN" > "$WORK/v2.ttl"

code=$(status_of "$API_URL/health"); expect "$code" 200 "GET /health"
code=$(status_of "$API_URL/vocabularies"); expect "$code" 401 "no token gets 401"
code=$(status_of -H "Authorization: Bearer $VIEWER" -F "name=$RUN" -F description=d -F changelog=c -F "vocabularyFile=@$WORK/v1.ttl" "$API_URL/vocabularies")
expect "$code" 403 "viewer gets 403 on create"
code=$(status_of -H "Authorization: Bearer $ADMIN" -F "name=$RUN" -F 'description=Smoke vocabulary' -F 'changelog=Initial version.' -F "vocabularyFile=@$WORK/v1.ttl" "$API_URL/vocabularies")
expect "$code" 201 "create vocabulary"
grep -q '"version":1' "$WORK/body" || fail "version 1 expected"
code=$(status_of -H "Authorization: Bearer $ADMIN" -F 'description=Smoke vocabulary v2' -F 'changelog=Second.' -F "vocabularyFile=@$WORK/v2.ttl" "$API_URL/vocabularies/$RUN/versions")
expect "$code" 201 "create new version"
grep -q '"version":2' "$WORK/body" || fail "version 2 expected"
code=$(status_of -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies?name=$RUN")
expect "$code" 200 "list with name filter"
grep -q "\"name\":\"$RUN\"" "$WORK/body" || fail "created vocabulary not listed"
code=$(status_of -H "Authorization: Bearer $VIEWER" "$API_URL/vocabularies/$RUN/versions")
expect "$code" 200 "version history"
grep -q '"status":"DEPRECATED"' "$WORK/body" || fail "version 1 should be DEPRECATED"
code=$(status_of -H "Authorization: Bearer $VIEWER" "$API_URL/content/$RUN")
expect "$code" 200 "download latest"
cmp -s "$WORK/body" "$WORK/v2.ttl" || fail "downloaded bytes differ from upload"
code=$(status_of -H "Authorization: Bearer $VIEWER" -H 'Content-Type: application/json' -d "{\"items\":[{\"name\":\"$RUN\"},{\"name\":\"$RUN\",\"version\":1}]}" "$API_URL/vocabularies/searches")
expect "$code" 200 "search"
grep -q '"latest":true' "$WORK/body" && grep -q '"latest":false' "$WORK/body" || fail "search results incomplete"
code=$(status_of -H "Authorization: Bearer $ADMIN" "$API_URL/actuator/health")
expect "$code" 404 "/actuator is not on the API port"
code=$(status_of -H "Authorization: Bearer $ADMIN" "http://localhost:${UI_PORT:-4433}/v1/actuator/prometheus")
expect "$code" 404 "/actuator not reachable through the UI proxy"
echo "SMOKE PASSED ($RUN)"
