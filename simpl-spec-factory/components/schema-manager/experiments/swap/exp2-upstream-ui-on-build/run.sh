#!/bin/bash
# Swap experiment 2: the upstream UI (local image, Keycloak guard off) on the build backend.
# The upstream local stack injects a fixed Authorization header in nginx; here it injects a
# real Keycloak token of ga-admin, since the build validates tokens. To keep that token valid
# for a test run, the access token lifespan of the build realm is raised to 4 hours through
# the admin API (lost when the Keycloak container is recreated).
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
KC=${KEYCLOAK_URL:-http://localhost:8185}
ADMIN=$(curl -s -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  "$KC/realms/master/protocol/openid-connect/token" | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
curl -s -X PUT -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"accessTokenLifespan": 14400}' "$KC/admin/realms/simpl" -o /dev/null -w 'realm lifespan: %{http_code}\n'
TOKEN=$(curl -s -d grant_type=password -d client_id=schema-tests -d username=ga-admin -d password=password \
  "$KC/realms/simpl/protocol/openid-connect/token" | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
sed "s#__TOKEN__#$TOKEN#" "$HERE/nginx.conf.template" > "$HERE/nginx.conf"
docker rm -f swap-upstream-ui-on-build >/dev/null 2>&1 || true
docker run -d --name swap-upstream-ui-on-build -p 4325:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/nginx.conf:/etc/nginx/nginx.conf:ro" simpl-schema-manager-ui:local >/dev/null
echo "upstream UI on the build backend: http://localhost:4325"
