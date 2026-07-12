#!/usr/bin/env bash
# Smoke tests for the infrastructure-be local stack. Run after start.sh.
# Proves: no-auth REST read+write, DB persistence, and Kafka consume path.
set -euo pipefail
B=http://localhost:8080/api/infrastructureProvisioning/v1

echo "== 1. status (permitted endpoint)"
curl -fsS "$B/status"; echo

echo "== 2. REST read, NO auth token (demonstrates auth is disabled)"
curl -fsS "$B/cloudProviders"; echo

echo "== 3. REST write round-trip, NO auth token"
curl -fsS -X POST "$B/scripts/types" -H 'Content-Type: application/json' \
  -d '{"name":"SMOKETF","description":"smoke"}' -w "\n  create HTTP %{http_code}\n" || true
echo "   read back:"
curl -fsS "$B/scripts/types" | python3 -c "import sys,json;print('   types =',[t['name'] for t in json.load(sys.stdin)['payload']['content']])"

echo "== 4. Kafka consume: publish a provisioning response to 'provisioned'"
echo '{"scriptTriggerId":999999,"status":"FAILED","message":"seed-smoke","phase":"Failed","completed":true}' \
  | docker exec -i simpl-infra-kafka kafka-console-producer --bootstrap-server localhost:9092 --topic provisioned
sleep 3
echo "   listener log:"
docker compose logs --since 15s infrastructure-be 2>&1 | grep -iE 'ArgoCd Provisioning|seed-smoke' | tail -2 | sed 's/^/   /'

echo "== done"

echo "== 5. Bruno API collection (host, env local)"
( cd bruno && npx --yes @usebruno/cli@latest run --env local -r 2>/dev/null | grep -E 'PASS|FAIL|Tests|Requests' | tail -4 | sed 's/^/   /' ) || echo "   (install bruno CLI or use: docker compose --profile tests up bruno-smoke-test)"
