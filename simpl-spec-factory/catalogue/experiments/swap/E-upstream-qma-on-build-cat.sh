#!/bin/bash
# Swap E: a second container of the local upstream adapter image on port 8087, pointed at the
# build's catalogue of data space A (18101). Needs the upstream stack running (for the image and
# its settings). Remove: docker rm -f swap-upstream-qma-on-build
set -euo pipefail
docker rm -f swap-upstream-qma-on-build >/dev/null 2>&1 || true
docker run -d --name swap-upstream-qma-on-build -p 8087:8084 --add-host host.docker.internal:host-gateway \
  -e FEDERATED_CATALOOGUE_CLIENT_URL=http://host.docker.internal:18101 \
  -e SERVER_SERVLET_CONTEXT_PATH=/v1 -e OTEL_SDK_DISABLED=true \
  query-mapper-adapter:local >/dev/null
echo "upstream adapter on the build catalogue: http://localhost:8087/v1"
