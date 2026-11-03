#!/usr/bin/env bash
# Smoke test of the running cm-factory stack, with curl only (python3 reads JSON).
# Usage: ./smoke.sh   (the stack must be up: docker compose -p cm-factory up -d --wait)
set -u
STUB=http://localhost:19090
CMP=http://localhost:19101/contract/v1
CMC=http://localhost:19201/contract/v1
UIP=http://localhost:19102/contract-ui/v1
UIC=http://localhost:19202/contract-ui/v1
KC=http://localhost:19080
PKEY=test-cm-provider-key
CKEY=test-cm-consumer-key
FAILS=0

pass() { echo "PASS  $*"; }
fail() { echo "FAIL  $*"; FAILS=$((FAILS + 1)); }
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }
uuid() { python3 -c 'import uuid; print(uuid.uuid4())'; }

token() { # realm user password
  curl -s -d grant_type=password -d client_id=contract-ui -d "username=$2" -d "password=$3" \
    "$KC/realms/$1/protocol/openid-connect/token" | json 'd["access_token"]'
}

cm_status() { # base key nid
  curl -s -H "X-Api-Key: $2" "$1/contracts/$3" | json 'd.get("status")'
}

wait_status() { # base key nid expected
  for _ in $(seq 1 80); do
    [ "$(cm_status "$1" "$2" "$3")" = "$4" ] && return 0
    sleep 0.25
  done
  return 1
}

wait_stub_state() { # nid expected
  for _ in $(seq 1 80); do
    [ "$(curl -s "$STUB/_control/negotiations/$1" | json 'd["state"]')" = "$2" ] && return 0
    sleep 0.25
  done
  return 1
}

new_asset() {
  local a="asset-$(uuid)"
  curl -s -o /dev/null -XPOST "$STUB/_control/catalogue/assets" -H 'Content-Type: application/json' \
    -d "{\"assetId\":\"$a\",\"resourceName\":\"Smoke $a\",\"providerId\":\"provider-01\",\"description\":\"Smoke test asset\",\"policies\":[{\"type\":\"USAGE\",\"description\":\"No resale\"}]}"
  echo "$a"
}

start_negotiation() { # asset [extra json fields]
  curl -s -XPOST "$STUB/_control/negotiations" -H 'Content-Type: application/json' \
    -d "{\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\",\"assetId\":\"$1\"${2:-}}" | json 'd["contractNegotiationId"]'
}

echo "== Tokens"
CTOK=$(token consumer consumer-admin 'ConsumerAdmin1!')
PTOK=$(token provider provider-admin 'ProviderAdmin1!')
NOROLE=$(token consumer consumer-norole 'ConsumerNoRole1!')
[ -n "$CTOK" ] && [ -n "$PTOK" ] && [ -n "$NOROLE" ] && pass "tokens from Keycloak" || fail "tokens from Keycloak"

echo "== 1. Whole negotiation to ACTIVE at both parties"
ASSET=$(new_asset)
NID=$(start_negotiation "$ASSET")
wait_status "$CMC" "$CKEY" "$NID" REQUESTED && pass "consumer record REQUESTED ($NID)" || fail "consumer record REQUESTED"
OFFER=$(curl -s -H "Authorization: Bearer $CTOK" "$UIC/contractOffers/$NID" | json 'd["status"]')
[ "$OFFER" = CREATED ] && pass "UIBE offer CREATED" || fail "UIBE offer CREATED (got $OFFER)"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -XPATCH -H "Authorization: Bearer $CTOK" "$UIC/contractOffers/$NID/accept")
[ "$CODE" = 200 ] && pass "accept through Consumer UIBE: 200" || fail "accept through Consumer UIBE: $CODE"
wait_status "$CMC" "$CKEY" "$NID" ACTIVE && pass "consumer ACTIVE" || fail "consumer ACTIVE (got $(cm_status "$CMC" "$CKEY" "$NID"))"
wait_status "$CMP" "$PKEY" "$NID" ACTIVE && pass "provider ACTIVE" || fail "provider ACTIVE (got $(cm_status "$CMP" "$PKEY" "$NID"))"
wait_stub_state "$NID" FINALIZED && pass "stub negotiation FINALIZED" || fail "stub negotiation FINALIZED"
for P in provider-01 consumer-01; do
  N=$(curl -s "$STUB/wallet/v1/$P/credentials?contractNegotiationId=$NID" | json 'len(d)')
  ROLES=$(curl -s "$STUB/wallet/v1/$P/credentials?contractNegotiationId=$NID" | json '",".join(sorted(v["credentialSubject"]["signerRole"] for v in d))')
  [ "$N" = 2 ] && [ "$ROLES" = "CONSUMER,PROVIDER" ] && pass "wallet $P holds both VCs" || fail "wallet $P holds $N VCs ($ROLES)"
done
AID=$(curl -s -H "X-Api-Key: $PKEY" "$CMP/contracts/$NID" | json 'd["contractAgreementId"]')
TECH=$(curl -s -H "X-Api-Key: $PKEY" "$CMP/agreements/$AID" | json 'd["status"]')
[ "$TECH" = FINALIZED ] && pass "GET /agreements/{aid} technical status FINALIZED" || fail "agreement status $TECH"
LINES=$(curl -s -H "X-Api-Key: $PKEY" "$CMP/agreements/$AID/file" | grep -c ':')
[ "$LINES" = 7 ] && pass "agreement file has seven lines" || fail "agreement file lines: $LINES"

echo "== 2. Incorrectly signed offer is rejected"
ASSET2=$(new_asset)
NID2=$(uuid); AID2=$(uuid); DID2="def-smoke"
VC=$(curl -s -XPOST "$STUB/vc-issuer/v1/credentials/issue" -H 'Content-Type: application/json' -d "{\"participantId\":\"consumer-01\",\"credentialSubject\":{
  \"id\":\"urn:simpl:contract:$NID2\",\"contractNegotiationId\":\"$NID2\",\"contractAgreementId\":\"$AID2\",\"contractDefinitionId\":\"$DID2\",
  \"assetId\":\"$ASSET2\",\"resourceDescriptionId\":\"rd-$ASSET2\",\"providerId\":\"provider-01\",\"consumerId\":\"consumer-01\",
  \"usageContractHash\":\"00\",\"signerRole\":\"CONSUMER\",\"signedAt\":\"2026-10-29T10:00:00Z\"}}")
BAD=$(echo "$VC" | python3 -c 'import sys,json; v=json.load(sys.stdin); p=v["proof"]["proofValue"]; v["proof"]["proofValue"]=p[:-1]+("A" if p[-1]!="A" else "B"); print(json.dumps(v))')
start_negotiation "$ASSET2" ",\"contractNegotiationId\":\"$NID2\",\"contractAgreementId\":\"$AID2\",\"contractDefinitionId\":\"$DID2\",\"consumerVcOverride\":$BAD" > /dev/null
wait_status "$CMC" "$CKEY" "$NID2" REQUESTED || true
curl -s -o /dev/null -XPATCH -H "Authorization: Bearer $CTOK" "$UIC/contractOffers/$NID2/accept"
wait_status "$CMP" "$PKEY" "$NID2" FAILED && pass "provider FAILED" || fail "provider FAILED (got $(cm_status "$CMP" "$PKEY" "$NID2"))"
REASON=$(curl -s -H "X-Api-Key: $PKEY" "$CMP/contracts/$NID2" | json 'd["validationReasonCode"]')
[ "$REASON" = INVALID_CONSUMER_SIGNATURE ] && pass "reason INVALID_CONSUMER_SIGNATURE" || fail "reason $REASON"
wait_status "$CMC" "$CKEY" "$NID2" FAILED && pass "consumer FAILED (PROVIDER_REJECTED)" || fail "consumer FAILED"
MAIL=$(curl -s -H "X-Api-Key: $PKEY" "$CMP/contracts/$NID2" | json '[h["entryType"] for h in d["history"] if h["entryType"].startswith("NOTIFICATION")]')
echo "$MAIL" | grep -q NOTIFICATION_SENT && pass "notification published" || fail "notification: $MAIL"

echo "== 3. Suspend and reactivate"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -XPOST -H "Authorization: Bearer $PTOK" -H 'Content-Type: application/json' \
  -d '{"action":"SUSPEND","reason":"Breach of usage policy"}' "$UIP/contracts/$NID/lifecycleActions")
[ "$CODE" = 200 ] && pass "suspend at Provider UIBE: 200" || fail "suspend: $CODE"
wait_status "$CMC" "$CKEY" "$NID" SUSPENDED && pass "consumer follows: SUSPENDED" || fail "consumer SUSPENDED"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -XPOST -H "Authorization: Bearer $CTOK" -H 'Content-Type: application/json' \
  -d '{"action":"SUSPEND","reason":"x"}' "$UIC/contracts/$NID/lifecycleActions")
[ "$CODE" = 409 ] && pass "consumer may not suspend: 409" || fail "consumer suspend: $CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -XPOST -H "Authorization: Bearer $PTOK" -H 'Content-Type: application/json' \
  -d '{"action":"REACTIVATE"}' "$UIP/contracts/$NID/lifecycleActions")
[ "$CODE" = 200 ] && pass "reactivate at Provider UIBE: 200" || fail "reactivate: $CODE"
wait_status "$CMC" "$CKEY" "$NID" ACTIVE && pass "consumer follows: ACTIVE" || fail "consumer ACTIVE"
[ "$(cm_status "$CMP" "$PKEY" "$NID")" = ACTIVE ] && pass "provider ACTIVE" || fail "provider ACTIVE"

echo "== 4. Offer expiry run (D82)"
ASSET3=$(new_asset)
NID3=$(start_negotiation "$ASSET3")
wait_status "$CMC" "$CKEY" "$NID3" REQUESTED || true
for _ in $(seq 1 40); do
  [ "$(curl -s -H "Authorization: Bearer $CTOK" "$UIC/contractOffers/$NID3" | json 'd.get("status")')" = CREATED ] && break; sleep 0.25
done
ASOF=$(python3 -c 'import datetime as d; print((d.datetime.now(d.timezone.utc)+d.timedelta(days=7,minutes=1)).strftime("%Y-%m-%dT%H:%M:%SZ"))')
RUN=$(curl -s -XPOST -H 'x-api-key: test-uibe-consumer-key' -H 'Content-Type: application/json' \
  -d "{\"asOf\":\"$ASOF\",\"contractNegotiationIds\":[\"$NID3\"]}" "$UIC/admin/offerExpiry/run")
echo "$RUN" | grep -q "\"expired\":\[\"$NID3\"\]" && pass "expiry run expired the offer" || fail "expiry run: $RUN"
wait_status "$CMC" "$CKEY" "$NID3" TERMINATED && pass "consumer TERMINATED" || fail "consumer TERMINATED"
RC=$(curl -s -H "X-Api-Key: $CKEY" "$CMC/contracts/$NID3" | json 'd["reasonCode"]')
[ "$RC" = OFFER_EXPIRED ] && pass "reason OFFER_EXPIRED" || fail "reason $RC"
[ "$(curl -s -H "Authorization: Bearer $CTOK" "$UIC/contractOffers/$NID3" | json 'd["status"]')" = TERMINATED ] && pass "UIBE offer TERMINATED" || fail "UIBE offer status"
wait_status "$CMP" "$PKEY" "$NID3" TERMINATED && pass "provider TERMINATED" || fail "provider TERMINATED"

echo "== 5. 401 and 403"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$UIC/contracts")
[ "$CODE" = 401 ] && pass "no token: 401" || fail "no token: $CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -H 'x-api-key: test-uibe-consumer-key' "$UIC/contracts")
[ "$CODE" = 401 ] && pass "API key alone on a user operation: 401" || fail "API key alone: $CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $NOROLE" "$UIC/contracts")
[ "$CODE" = 403 ] && pass "user without role: 403" || fail "user without role: $CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $PTOK" "$UIC/contracts")
[ "$CODE" = 401 ] && pass "token of the other realm: 401" || fail "token of the other realm: $CODE"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$CMP/contracts")
[ "$CODE" = 401 ] && pass "CM without API key: 401" || fail "CM without API key: $CODE"

echo
if [ "$FAILS" = 0 ]; then echo "SMOKE OK"; exit 0; else echo "SMOKE FAILED: $FAILS check(s)"; exit 1; fi
