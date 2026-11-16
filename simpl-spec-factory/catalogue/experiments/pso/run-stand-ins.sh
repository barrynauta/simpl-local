#!/bin/bash
# The PSO gateway stand-in on port 18500 (as run-gateway.sh) plus the environment stand-ins of
# 4 Nov 2026: SD Tooling and signer (stand-ins/sd-tooling.mjs, signing with the build's test key tool).
# Remove: docker rm -f pso-gateway-cat pso-sd-tooling-cat; docker network rm pso-cat
HERE=$(cd "$(dirname "$0")" && pwd)
TOOLS=$HERE/../../generated/run1/tools
docker rm -f pso-gateway-cat pso-sd-tooling-cat >/dev/null 2>&1 || true
docker network create pso-cat >/dev/null 2>&1 || true
docker run -d --name pso-sd-tooling-cat --network pso-cat \
  -v "$HERE/stand-ins/sd-tooling.mjs:/app/sd-tooling.mjs:ro" -v "$TOOLS:/factory-tools:ro" \
  node:22-alpine node /app/sd-tooling.mjs >/dev/null
docker run -d --name pso-gateway-cat --network pso-cat -p 18500:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/stand-ins/gateway.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine >/dev/null
echo "PSO gateway stand-in with SD Tooling stand-in: http://localhost:18500"
