#!/bin/bash
# Starts the PSO gateway stand-in on port 19500. Remove: docker rm -f pso-gateway-cm
HERE=$(cd "$(dirname "$0")" && pwd)
docker rm -f pso-gateway-cm >/dev/null 2>&1 || true
docker run -d --name pso-gateway-cm -p 19500:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/gateway.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine >/dev/null
echo "PSO gateway stand-in: http://localhost:19500"
