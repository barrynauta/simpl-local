#!/usr/bin/env bash
# Smoke test of the running stack (docker compose -p cm-factory-pso up -d --wait).
# Needs curl and jq. Exits non-zero on the first failure.
set -u

STUB=http://localhost:19090
CMP=http://localhost:19101/contract/v1
CMC=http://localhost:19201/contract/v1
UIBEP=http://localhost:19102/contract-ui/v1
UIBEC=http://localhost:19202/contract-ui/v1
KC=http://localhost:19080
KP='X-Api-Key: test-cm-provider-key'
KCON='X-Api-Key: test-cm-consumer-key'
JSON='Content-Type: application/json'

PASS=0
FAIL=0
ok() { echo "PASS  $1"; PASS=$((PASS + 1)); }
ko() { echo "FAIL  $1"; FAIL=$((FAIL + 1)); }
check() { if [ "$2" = "$3" ]; then ok "$1"; else ko "$1 (expected '$3', got '$2')"; fi; }
uuid() { uuidgen | tr 'A-Z' 'a-z'; }

token() { # realm user password
  curl -s -d grant_type=password -d client_id=contract-ui -d "username=$2" --data-urlencode "password=$3" \
    "$KC/realms/$1/protocol/openid-connect/token" | jq -r .access_token
}

# wait_for <description> <command that prints a value> <expected value> [timeout seconds]
wait_for() {
  local desc=$1 cmd=$2 want=$3 limit=${4:-20} got="" i=0
  while [ $i -lt $((limit * 4)) ]; do
    got=$(eval "$cmd" 2>/dev/null)
    if [ "$got" = "$want" ]; then ok "$desc"; return 0; fi
    sleep 0.25; i=$((i + 1))
  done
  ko "$desc (expected '$want', last value '$got')"
  return 1
}

cm_status() { curl -s -H "$2" "$1/contracts/$3" | jq -r .status; }
stub_state() { curl -s "$STUB/_control/negotiations/$1" | jq -r .state; }

new_asset() {
  local a="asset-$(uuid)"
  curl -s -X POST "$STUB/_control/catalogue/assets" -H "$JSON" -d "{\"assetId\":\"$a\",\"resourceName\":\"Smoke $a\",
    \"description\":\"Smoke test asset\",\"providerId\":\"provider-01\",
    \"policies\":[{\"type\":\"USAGE\",\"description\":\"Research only\"}],
    \"contractClauses\":[{\"documentName\":\"terms.pdf\",\"hash\":\"abc123\"}]}" > /dev/null
  echo "$a"
}

echo "== tokens"
TC=$(token consumer consumer-admin 'ConsumerAdmin1!')
TP=$(token provider provider-admin 'ProviderAdmin1!')
TNR=$(token provider provider-norole 'ProviderNoRole1!')
[ -n "$TC" ] && [ "$TC" != null ] && ok "consumer token" || ko "consumer token"
[ -n "$TP" ] && [ "$TP" != null ] && ok "provider token" || ko "provider token"

echo "== 1. whole negotiation to ACTIVE"
ASSET=$(new_asset)
NEG=$(curl -s -X POST "$STUB/_control/negotiations" -H "$JSON" \
  -d "{\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\",\"assetId\":\"$ASSET\"}")
NID=$(echo "$NEG" | jq -r .contractNegotiationId)
AID=$(echo "$NEG" | jq -r .contractAgreementId)
wait_for "stub reached OFFERED" "stub_state $NID" OFFERED
wait_for "consumer record REQUESTED" "cm_status $CMC '$KCON' $NID" REQUESTED
check "consumer UIBE offer CREATED" "$(curl -s -H "Authorization: Bearer $TC" "$UIBEC/contractOffers/$NID" | jq -r .status)" CREATED
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X PATCH -H "Authorization: Bearer $TC" "$UIBEC/contractOffers/$NID/accept")
check "accept through consumer UIBE" "$CODE" 200
wait_for "stub reached FINALIZED" "stub_state $NID" FINALIZED
wait_for "consumer ACTIVE" "cm_status $CMC '$KCON' $NID" ACTIVE
wait_for "provider ACTIVE" "cm_status $CMP '$KP' $NID" ACTIVE
check "provider wallet holds 2 VCs" "$(curl -s "$STUB/wallet/v1/provider-01/credentials?contractNegotiationId=$NID" | jq 'map(.credentialSubject.signerRole) | sort | join(",")' -r)" "CONSUMER,PROVIDER"
check "consumer wallet holds 2 VCs" "$(curl -s "$STUB/wallet/v1/consumer-01/credentials?contractNegotiationId=$NID" | jq 'map(.credentialSubject.signerRole) | sort | join(",")' -r)" "CONSUMER,PROVIDER"
check "GET /agreements/{aid} technical status" "$(curl -s -H "$KP" "$CMP/agreements/$AID" | jq -r .status)" FINALIZED
check "agreement file has seven lines" "$(curl -s -H "$KP" "$CMP/agreements/$AID/file" | awk 'END{print NR}')" 7

echo "== 2. incorrectly signed offer is rejected"
ASSET2=$(new_asset)
NID2=$(uuid); AID2=$(uuid); DID2="def-$(uuid | cut -c1-8)"
VC=$(curl -s -X POST "$STUB/vc-issuer/v1/credentials/issue" -H "$JSON" -d "{\"participantId\":\"consumer-01\",
  \"credentialSubject\":{\"id\":\"urn:simpl:contract:$NID2\",\"contractNegotiationId\":\"$NID2\",
  \"contractAgreementId\":\"$AID2\",\"contractDefinitionId\":\"$DID2\",\"assetId\":\"$ASSET2\",
  \"resourceDescriptionId\":\"rd-$ASSET2\",\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\",
  \"usageContractHash\":\"0000\",\"signerRole\":\"CONSUMER\",\"signedAt\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}}")
BAD=$(echo "$VC" | jq '.credentialSubject.assetId = "tampered"')
curl -s -X POST "$STUB/_control/negotiations" -H "$JSON" -d "{\"contractNegotiationId\":\"$NID2\",
  \"contractAgreementId\":\"$AID2\",\"contractDefinitionId\":\"$DID2\",\"providerId\":\"provider-01\",
  \"consumerId\":\"consumer-01\",\"assetId\":\"$ASSET2\",\"consumerVcOverride\":$BAD}" > /dev/null
wait_for "stub reached OFFERED (bad VC)" "stub_state $NID2" OFFERED
curl -s -o /dev/null -X PATCH -H "Authorization: Bearer $TC" "$UIBEC/contractOffers/$NID2/accept"
wait_for "provider FAILED" "cm_status $CMP '$KP' $NID2" FAILED
check "provider reason INVALID_CONSUMER_SIGNATURE" "$(curl -s -H "$KP" "$CMP/contracts/$NID2" | jq -r .reasonCode)" INVALID_CONSUMER_SIGNATURE
check "provider validationResult REJECTED" "$(curl -s -H "$KP" "$CMP/contracts/$NID2" | jq -r .validationResult)" REJECTED
wait_for "consumer FAILED (PROVIDER_REJECTED)" "curl -s -H '$KCON' $CMC/contracts/$NID2 | jq -r '.status + \":\" + .reasonCode'" "FAILED:PROVIDER_REJECTED"
wait_for "stub TERMINATED" "stub_state $NID2" TERMINATED

echo "== 3. suspend and reactivate"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $TP" -H "$JSON" \
  -d '{"action":"SUSPEND","reason":"Breach of usage policy"}' "$UIBEP/contracts/$NID/lifecycleActions")
check "provider suspends" "$CODE" 200
wait_for "consumer sees SUSPENDED" "cm_status $CMC '$KCON' $NID" SUSPENDED 10
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $TP" -H "$JSON" \
  -d '{"action":"REACTIVATE"}' "$UIBEP/contracts/$NID/lifecycleActions")
check "provider reactivates" "$CODE" 200
wait_for "consumer sees ACTIVE again" "cm_status $CMC '$KCON' $NID" ACTIVE 10
CODE=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $TC" -H "$JSON" \
  -d '{"action":"SUSPEND","reason":"not mine"}' "$UIBEC/contracts/$NID/lifecycleActions")
check "consumer may not suspend (409)" "$CODE" 409

echo "== 4. offer expiry run (D82)"
ASSET3=$(new_asset)
NID3=$(curl -s -X POST "$STUB/_control/negotiations" -H "$JSON" \
  -d "{\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\",\"assetId\":\"$ASSET3\"}" | jq -r .contractNegotiationId)
wait_for "stub reached OFFERED (expiry)" "stub_state $NID3" OFFERED
ASOF=$(python3 -c 'import datetime as d;print((d.datetime.now(d.timezone.utc)+d.timedelta(days=7,minutes=1)).strftime("%Y-%m-%dT%H:%M:%SZ"))')
RUN=$(curl -s -X POST -H 'x-api-key: test-uibe-consumer-key' -H "$JSON" -d "{\"asOf\":\"$ASOF\",\"contractNegotiationIds\":[\"$NID3\"]}" "$UIBEC/admin/offerExpiry/run")
check "expiry run expired the offer" "$(echo "$RUN" | jq -r '.expired | join(",")')" "$NID3"
check "consumer TERMINATED with OFFER_EXPIRED" "$(curl -s -H "$KCON" "$CMC/contracts/$NID3" | jq -r '.status + ":" + .reasonCode')" "TERMINATED:OFFER_EXPIRED"
check "UIBE offer TERMINATED" "$(curl -s -H "Authorization: Bearer $TC" "$UIBEC/contractOffers/$NID3" | jq -r .status)" TERMINATED
wait_for "provider TERMINATED" "cm_status $CMP '$KP' $NID3" TERMINATED

echo "== 5. access control"
check "no token on UIBE: 401" "$(curl -s -o /dev/null -w '%{http_code}' "$UIBEP/contracts")" 401
check "user without role on UIBE: 403" "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TNR" "$UIBEP/contracts")" 403
check "consumer token at provider UIBE: 401" "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TC" "$UIBEP/contracts")" 401
check "no key on CM: 401" "$(curl -s -o /dev/null -w '%{http_code}' "$CMP/contracts")" 401

echo
echo "smoke: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
