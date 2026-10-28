#!/usr/bin/env bash
# Smoke test of the running stack (docker compose up -d --build --wait first).
# Needs curl and jq.
set -euo pipefail

SM_BASE_URL="${SM_BASE_URL:-http://localhost:8098/v1}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8185}"
MAIL_API_URL="${MAIL_API_URL:-http://localhost:8027}"

pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; exit 1; }

token() {
  curl -fsS -X POST "$KEYCLOAK_URL/realms/simpl/protocol/openid-connect/token" \
    -d grant_type=password -d client_id=schema-tests -d "username=$1" -d password=password \
    | jq -r .access_token
}

SUFFIX="$(date +%s)$RANDOM"
NAME="TSmoke${SUFFIX}"
RT="RtSmoke${SUFFIX}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

cat > "$TMP/schema.ttl" <<'TTL'
@prefix sh: <http://www.w3.org/ns/shacl#> .
@prefix simpl: <https://simpl-programme.ec.europa.eu/ns/default#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

<urn:shape:Smoke> a sh:NodeShape ;
  sh:property [ sh:path simpl:uuid ; sh:datatype xsd:string ] ;
  sh:property [ sh:path simpl:version ; sh:datatype xsd:string ] .
TTL
cp "$TMP/schema.ttl" "$TMP/schema-v2.ttl"
echo "# second version" >> "$TMP/schema-v2.ttl"

ADMIN="$(token ga-admin)"
VIEWER="$(token ga-viewer)"
[ -n "$ADMIN" ] && [ "$ADMIN" != null ] || fail "no token for ga-admin"
pass "token for ga-admin (password grant, client schema-tests)"

# 1. Create a RESOURCE schema.
code=$(curl -sS -o "$TMP/create.json" -D "$TMP/create.h" -w '%{http_code}' -X POST "$SM_BASE_URL/schemas" \
  -H "Authorization: Bearer $ADMIN" \
  -F "name=$NAME" -F "title=Smoke schema" -F "description=Created by smoke.sh" \
  -F "resourceType=$RT" -F "schemaFile=@$TMP/schema.ttl;type=text/turtle")
[ "$code" = 201 ] || { cat "$TMP/create.json"; fail "create schema: HTTP $code"; }
grep -qi "^location: .*/schemas/$NAME" "$TMP/create.h" || fail "create schema: Location header"
[ "$(jq -r .schemaVersion "$TMP/create.json")" = 1 ] || fail "create schema: version 1"
pass "create $NAME -> 201, version 1, status $(jq -r .status "$TMP/create.json")"

# 2. Create a second version.
code=$(curl -sS -o "$TMP/v2.json" -D "$TMP/v2.h" -w '%{http_code}' -X POST "$SM_BASE_URL/schemas/$NAME/versions" \
  -H "Authorization: Bearer $ADMIN" \
  -F "changelog=Second version" -F "schemaFile=@$TMP/schema-v2.ttl;type=text/turtle")
[ "$code" = 201 ] || { cat "$TMP/v2.json"; fail "new version: HTTP $code"; }
[ "$(jq -r .version "$TMP/v2.json")" = 2 ] || fail "new version: version 2"
grep -qi "^location: .*/schemas/$NAME/2" "$TMP/v2.h" || fail "new version: Location header"
pass "new version -> 201, version 2"

# 3. List.
code=$(curl -sS -o "$TMP/list.json" -w '%{http_code}' "$SM_BASE_URL/schemas?resourceType=$RT" -H "Authorization: Bearer $VIEWER")
[ "$code" = 200 ] || fail "list: HTTP $code"
[ "$(jq -r '.metadata[0].name' "$TMP/list.json")" = "$NAME" ] || fail "list: schema missing"
[ "$(jq -r '.metadata[0].version' "$TMP/list.json")" = 2 ] || fail "list: latest version is not 2"
pass "list as ga-viewer -> 200, latest version 2"

# 4. Retrieve content (latest and version 1).
code=$(curl -sS -o "$TMP/latest.ttl" -D "$TMP/latest.h" -w '%{http_code}' "$SM_BASE_URL/schemas/$NAME" -H "Authorization: Bearer $VIEWER")
[ "$code" = 200 ] || fail "latest content: HTTP $code"
cmp -s "$TMP/latest.ttl" "$TMP/schema-v2.ttl" || fail "latest content differs from version 2"
grep -qi "^content-location: .*/schemas/$NAME/2" "$TMP/latest.h" || fail "latest content: Content-Location"
grep -qi "^content-type: text/turtle" "$TMP/latest.h" || fail "latest content: Content-Type"
code=$(curl -sS -o "$TMP/v1.ttl" -w '%{http_code}' "$SM_BASE_URL/schemas/$NAME/1" -H "Authorization: Bearer $VIEWER")
[ "$code" = 200 ] && cmp -s "$TMP/v1.ttl" "$TMP/schema.ttl" || fail "version 1 content"
pass "content: latest = version 2 byte for byte, version 1 byte for byte"

# 5. Access control.
code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$SM_BASE_URL/schemas" -H "Authorization: Bearer $VIEWER" \
  -F "name=TViewer${SUFFIX}" -F "title=x" -F "description=y" -F "resourceType=$RT" -F "schemaFile=@$TMP/schema.ttl")
[ "$code" = 403 ] || fail "ga-viewer create: expected 403, got $code"
pass "ga-viewer create -> 403"
code=$(curl -sS -o "$TMP/401.json" -w '%{http_code}' "$SM_BASE_URL/schemas")
[ "$code" = 401 ] || fail "no token: expected 401, got $code"
[ "$(jq -r .type "$TMP/401.json")" = "urn:problem-type:simpl:unauthorized" ] || fail "401 problem type"
pass "no token -> 401 problem+json"

# 6. Provider e-mail (D40) in Mailpit.
SUBJECT="Schema published: $NAME (version 1)"
for i in $(seq 1 20); do
  found=$(curl -fsS "$MAIL_API_URL/api/v1/search?query=$(jq -rn --arg q "subject:\"$NAME\"" '$q|@uri')" \
    | jq -r --arg s "$SUBJECT" '[.messages[] | select(.Subject == $s)] | length')
  [ "$found" -ge 1 ] && break
  sleep 0.5
done
[ "${found:-0}" -ge 1 ] || fail "provider e-mail '$SUBJECT' not found in Mailpit"
pass "Mailpit has '$SUBJECT'"
NEWV=$(curl -fsS "$MAIL_API_URL/api/v1/search?query=$(jq -rn --arg q "subject:\"$NAME\"" '$q|@uri')" \
  | jq -r '[.messages[].Subject] | join(" | ")')
echo "      all messages for $NAME: $NEWV"

echo "SMOKE OK"
