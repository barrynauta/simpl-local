#!/bin/bash
# Starts the PSO gateway stand-in on port 19501. Remove: docker rm -f pso-gateway-cm-up
HERE=$(cd "$(dirname "$0")" && pwd)
docker rm -f pso-gateway-cm-up >/dev/null 2>&1 || true
docker run -d --name pso-gateway-cm-up -p 19501:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/gateway-upstream.conf:/etc/nginx/conf.d/default.conf:ro" nginx:1.27-alpine >/dev/null
echo "PSO gateway stand-in: http://localhost:19501"
