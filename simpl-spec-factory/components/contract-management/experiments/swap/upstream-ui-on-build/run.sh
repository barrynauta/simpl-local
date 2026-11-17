#!/bin/bash
# Second container of the local upstream UI image (shell plus the local read-path overlay), with its
# nginx pointed at the build's Provider contract manager. Port 19601. Remove: docker rm -f swap-upstream-ui-on-build-cm
HERE=$(cd "$(dirname "$0")" && pwd)
docker rm -f swap-upstream-ui-on-build-cm >/dev/null 2>&1 || true
docker run -d --name swap-upstream-ui-on-build-cm -p 19601:8080 --add-host host.docker.internal:host-gateway \
  -v "$HERE/nginx.conf:/etc/nginx/nginx.conf:ro" simpl-contract-ui:local >/dev/null
echo "upstream UI on the build: http://localhost:19601"
