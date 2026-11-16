#!/bin/bash
# Environment stand-ins for the PSO run (4 Nov 2026): a gateway on port 8186 in front of the build's
# Keycloak, and the IAA users stand-in behind it. Needs the build running (Keycloak on 8185).
# Remove: docker rm -f pso-gateway-sm pso-iaa-users-sm; docker network rm pso-sm
HERE=$(cd "$(dirname "$0")" && pwd)
COMPOSE=$HERE/../../generated/run1/docker-compose.yml
# the build's throwaway Keycloak admin, read from its compose file (not copied here)
KC_USER=$(sed -nE 's/^ *KC_BOOTSTRAP_ADMIN_USERNAME: *//p' "$COMPOSE")
KC_PW=$(sed -nE 's/^ *KC_BOOTSTRAP_ADMIN_PASSWORD: *//p' "$COMPOSE")
docker rm -f pso-gateway-sm pso-iaa-users-sm >/dev/null 2>&1 || true
docker network create pso-sm >/dev/null 2>&1 || true
docker run -d --name pso-iaa-users-sm --network pso-sm --add-host host.docker.internal:host-gateway \
  -e KC_URL=http://host.docker.internal:8185 -e KC_REALM=simpl -e KC_ADMIN_USER="$KC_USER" -e KC_ADMIN_PASSWORD="$KC_PW" \
  -v "$HERE/stand-ins/iaa-users.mjs:/app/iaa-users.mjs:ro" node:22-alpine node /app/iaa-users.mjs >/dev/null
docker run -d --name pso-gateway-sm --network pso-sm -p 8186:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/stand-ins/gateway.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine >/dev/null
echo "PSO stand-ins: gateway http://localhost:8186 (Keycloak + IAA users)"
