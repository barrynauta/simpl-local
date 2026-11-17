#!/bin/bash
# Swap experiment 3: the build UI on the upstream backend. Replaces the build's ui container
# on port 4334 (the only redirect URI of the build's Keycloak client); the UI keeps signing in
# on the build's Keycloak, upstream only decodes the token. Restore: ./run.sh restore
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
B=${BUILD_DIR:-$HOME/src/spec-factory-build-sm/build}
if [ "${1:-}" = restore ]; then
  docker rm -f swap-build-ui-on-upstream >/dev/null 2>&1 || true
  (cd "$B" && docker compose start ui)
  exit 0
fi
(cd "$B" && docker compose stop ui)
IMAGE=$(cd "$B" && docker compose images ui --format json | python3 -c 'import sys,json;d=json.load(sys.stdin);d=d[0] if isinstance(d,list) else d;print(d["Repository"]+":"+d["Tag"])')
docker rm -f swap-build-ui-on-upstream >/dev/null 2>&1 || true
docker run -d --name swap-build-ui-on-upstream -p 4334:8080 --add-host host.docker.internal:host-gateway \
  -e KEYCLOAK_URL=http://localhost:8185 -e KEYCLOAK_REALM=simpl \
  -v "$HERE/default.conf:/etc/nginx/conf.d/default.conf:ro" "$IMAGE" >/dev/null
echo "build UI on the upstream backend: http://localhost:4334"
