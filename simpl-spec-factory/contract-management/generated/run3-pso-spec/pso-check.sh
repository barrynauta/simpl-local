#!/usr/bin/env bash
# The 8 PSO scenarios of input/pso, sent straight to the Provider's contract manager,
# following the gateway rules of input/pso/README.md (D85):
#   "a user with a role"          -> X-Api-Key: test-cm-provider-key
#   "a user without authorization" -> no API key
# Needs curl and jq.
set -u

BASE=${CM_PROVIDER_URL:-http://localhost:19101/contract/v1}
KEY='X-Api-Key: test-cm-provider-key'
JSON='Content-Type: application/json'
PASS=0
FAIL=0
uuid() { uuidgen | tr 'A-Z' 'a-z'; }

# Checks a body against pso/schemas/error-response.json (draft-07): a JSON object; type a URI string;
# title and detail strings; status an integer 400..599; instance a string or null; extra fields allowed.
# Absent fields are allowed, as the schema requires none.
schema_ok() {
  jq -e '
    type == "object"
    and ((has("type") | not) or ((.type | type) == "string" and (.type | test("^[A-Za-z][A-Za-z0-9+.-]*:"))))
    and ((has("title") | not) or ((.title | type) == "string"))
    and ((has("detail") | not) or ((.detail | type) == "string"))
    and ((has("status") | not) or ((.status | type) == "number" and (.status | floor) == .status
         and .status >= 400 and .status < 600))
    and ((has("instance") | not) or .instance == null or ((.instance | type) == "string"))
  ' > /dev/null 2>&1
}

# scenario <id> <expected status> <check schema: yes|no> <curl args...>
scenario() {
  local id=$1 want=$2 schema=$3
  shift 3
  local out code body
  out=$(curl -s -w '\n%{http_code}' "$@")
  code=$(echo "$out" | tail -n 1)
  body=$(echo "$out" | sed '$d')
  local result="PASS" note="status $code"
  if [ "$code" != "$want" ]; then
    result="FAIL"; note="expected $want, got $code"
  elif [ "$schema" = yes ]; then
    if echo "$body" | schema_ok; then
      note="$note, body conforms to error-response.json"
    else
      result="FAIL"; note="body does not conform: $body"
    fi
  fi
  echo "$result  $id: $note"
  if [ "$result" = PASS ]; then PASS=$((PASS + 1)); else FAIL=$((FAIL + 1)); fi
}

echo "PSO scenarios against $BASE"
# contract-billing.feature
scenario TC_CB_02 404 no  -H "$KEY" "$BASE/agreements/$(uuid)"
scenario TC_CB_04 404 no  -H "$KEY" -H "$JSON" -X POST -d '{}' \
  "$BASE/credentials/agreements/$(uuid)/definitions/$(uuid)/issue"
scenario TC_CB_06 404 no  -H "$KEY" "$BASE/agreements/$(uuid)/file"
scenario TC_CB_07 401 no  "$BASE/health"
# contract-billing-schema-validation.feature
scenario TCCB_SV03 401 yes -H "$JSON" -X POST -d '{}' "$BASE/credentials/agreements/$(uuid)/definitions/test-def/issue"
scenario TCCB_SV04 401 yes -H "$JSON" -X POST -d '{}' "$BASE/credentials/agreements/$(uuid)/definitions/test-def/create"
scenario TCCB_SV05 401 yes -H "$JSON" -X POST -d '{}' "$BASE/agreements/test-negotiation/search"
scenario TCCB_SV07 404 yes -H "$KEY" "$BASE/agreements/$(uuid)"

echo
echo "pso-check: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ]
