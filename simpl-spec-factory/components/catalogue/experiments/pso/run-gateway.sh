#!/bin/bash
# Starts the PSO gateway stand-in on port 18500. Remove: docker rm -f pso-gateway-cat
HERE=$(cd "$(dirname "$0")" && pwd)
docker rm -f pso-gateway-cat >/dev/null 2>&1 || true
docker run -d --name pso-gateway-cat -p 18500:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/gateway.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine >/dev/null
echo "PSO gateway stand-in: http://localhost:18500"
