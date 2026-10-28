#!/usr/bin/env bash
# Smoke test of the running stack (brief, finish line 2). Needs curl, jq and node (or docker for node).
set -euo pipefail
cd "$(dirname "$0")"

KC=${KC:-http://localhost:18080}
CAT_A=${CAT_A:-http://localhost:18101}
CAT_B=${CAT_B:-http://localhost:18201}
CBE=${CBE:-http://localhost:18103}

if command -v node >/dev/null 2>&1; then
  NODE=(node)
else
  NODE=(docker run --rm -i -v "$PWD/tools:/tools:ro" node:22-alpine node)
fi
SIGN() { "${NODE[@]}" "$( [ "${NODE[0]}" = node ] && echo tools || echo /tools )/sign-rd.mjs" "$@"; }

fail() { echo "FAIL: $*" >&2; exit 1; }
pass() { echo "ok   $*"; }

token() { # realm user
  curl -sf -d grant_type=password -d client_id=test-client -d "username=$2" -d 'password=Passw0rd!' \
    "$KC/realms/$1/protocol/openid-connect/token" | jq -r .access_token
}

TA_PROVIDER=$(token dataspace-a provider-a)
TA_CONSUMER=$(token dataspace-a consumer-a)
TA_NOBODY=$(token dataspace-a nobody-a)
TB_PROVIDER=$(token dataspace-b provider-b)
[ -n "$TA_PROVIDER" ] && [ "$TA_PROVIDER" != null ] || fail "no token for provider-a"
pass "tokens by password grant"

RUN="t$(openssl rand -hex 5)"

rd() { # key-name provider assignee
  local did; did=$(SIGN --did "$1")
  jq -n --arg id "did:web:test.simpl.local:DataOffering:$(uuidgen | tr 'A-Z' 'a-z')" --arg did "$did" \
        --arg run "$RUN" --arg provider "$2" --arg assignee "$3" '{
    "@context": ["https://www.w3.org/2018/credentials/v1", "https://w3id.org/security/suites/jws-2020/v1"],
    "type": "VerifiableCredential", "issuer": $did, "issuanceDate": "2026-10-28T10:00:00Z",
    "credentialSubject": {
      "@context": {"simpl": "http://w3id.org/gaia-x/simpl#", "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                   "xsd": "http://www.w3.org/2001/XMLSchema#", "dct": "http://purl.org/dc/terms/"},
      "@id": $id, "rdf:type": {"@id": "simpl:DataOffering"},
      "dct:conformsTo": {"@id": "urn:simpl:schema:DataOfferingShape:1", "dct:schemaName": "DataOfferingShape", "dct:hasVersion": "1"},
      "simpl:version": 1,
      "simpl:generalServiceProperties": {"rdf:type": {"@id": "simpl:GeneralServiceProperties"},
        "simpl:name": ($run + " dataset"), "simpl:description": ($run + " description"), "simpl:inLanguage": "en",
        "simpl:offeringType": "data",
        "simpl:serviceAccessPoint": {"@type": "xsd:anyURI", "@value": "https://provider.example.org/data"}},
      "simpl:providerInformation": {"rdf:type": {"@id": "simpl:ProviderInformation"},
        "simpl:providedBy": $provider, "simpl:contact": ($provider + "@example.org")},
      "simpl:offeringPrice": {"rdf:type": {"@id": "simpl:OfferingPrice"},
        "simpl:price": {"@type": "xsd:decimal", "@value": 100}, "simpl:currency": "EUR"},
      "simpl:servicePolicy": {"rdf:type": {"@id": "simpl:ServicePolicy"},
        "simpl:access-policy": ({"@context": "http://www.w3.org/ns/odrl.jsonld", "@type": "Set", "uid": "p1",
          "permission": [{"action": ["http://simpl.eu/odrl/actions/search"], "assignee": {"uid": $assignee}, "constraint": []}]} | tojson)}
    }}' | SIGN "$1"
}

publish() { # base token json
  curl -s -o /tmp/smoke-pub.json -w '%{http_code}' -X POST "$1/self-descriptions" \
    -H "Authorization: Bearer $2" -H 'Content-Type: application/json' --data-binary "$3"
}

RD_A=$(rd provider-a provider-a CONSUMER)
ID_A=$(echo "$RD_A" | jq -r '.credentialSubject["@id"]')
code=$(publish "$CAT_A" "$TA_PROVIDER" "$RD_A"); [ "$code" = 201 ] || { cat /tmp/smoke-pub.json; fail "publish in A: $code"; }
HASH_A=$(jq -r .sdHash /tmp/smoke-pub.json)
pass "provider-a publishes a signed RD in data space A ($ID_A)"

RD_B=$(rd provider-b provider-b PARTNER_CONSUMER)
ID_B=$(echo "$RD_B" | jq -r '.credentialSubject["@id"]')
code=$(publish "$CAT_B" "$TB_PROVIDER" "$RD_B"); [ "$code" = 201 ] || { cat /tmp/smoke-pub.json; fail "publish in B: $code"; }
HASH_B=$(jq -r .sdHash /tmp/smoke-pub.json)
pass "provider-b publishes a signed RD in data space B ($ID_B)"

cleanup() {
  curl -s -o /dev/null -X DELETE "$CAT_A/self-descriptions/$HASH_A" -H "Authorization: Bearer $TA_PROVIDER" || true
  curl -s -o /dev/null -X DELETE "$CAT_B/self-descriptions/$HASH_B" -H "Authorization: Bearer $TB_PROVIDER" || true
}
trap cleanup EXIT

res=$(curl -sf "$CBE/v1/selfDescriptions?q=$RUN" -H "Authorization: Bearer $TA_CONSUMER")
echo "$res" | jq -e --arg id "$ID_A" '.groups[0].catalogueId == "catalogue-a" and any(.groups[0].items[]; .i.claimsGraphUri[0] == $id)' >/dev/null \
  || fail "quick search did not find the RD: $res"
pass "consumer-a finds it with quick search through cbe"

res=$(curl -sf -X POST "$CBE/v1/selfDescriptions/advanced" -H "Authorization: Bearer $TA_CONSUMER" -H 'Content-Type: application/json' \
  -d "{\"simpl:GeneralServiceProperties\": {\"name\": \"$RUN\"}, \"simpl:OfferingPrice\": {\"price\": {\"op1\": \">=\", \"value1\": 50}}}")
echo "$res" | jq -e --arg id "$ID_A" 'any(.items[]; .i.claimsGraphUri[0] == $id)' >/dev/null || fail "advanced search did not find the RD: $res"
pass "consumer-a finds it with advanced search through cbe"

res=$(curl -sf "$CBE/v1/selfDescriptions?q=$RUN&catalogueIds=local&catalogueIds=catalogue-b" -H "Authorization: Bearer $TA_CONSUMER")
echo "$res" | jq -e --arg a "$ID_A" --arg b "$ID_B" '
  (.groups | map(.catalogueId)) == ["catalogue-a", "catalogue-b"]
  and any(.groups[0].items[]; .i.claimsGraphUri[0] == $a)
  and any(.groups[1].items[]; .i.claimsGraphUri[0] == $b)
  and (.groups[1].items | all(.i.qualityScore == null))' >/dev/null || fail "federated search: $res"
pass "federated search from A returns the B result in its own group (no quality data)"

code=$(curl -s -o /dev/null -w '%{http_code}' "$CBE/v1/selfDescriptions?q=$RUN" -H "Authorization: Bearer $TA_NOBODY")
[ "$code" = 403 ] || fail "nobody-a got $code"
pass "a consumer without the role gets 403"

code=$(curl -s -o /dev/null -w '%{http_code}' "$CBE/v1/selfDescriptions?q=$RUN")
[ "$code" = 401 ] || fail "no token got $code"
pass "no token gets 401"

echo "SMOKE PASSED"
