#!/bin/bash
# Swap D: a second container of the local upstream UI image on port 4326, with its API URLs
# pointed at the build of data space A (adapter on 18102, catalogue on 18101). The upstream UI
# calls the APIs from its own server routes with the token from its cookie "token"; the walk
# sets that cookie to a real token of the build's Keycloak. Remove: docker rm -f swap-upstream-ui-on-build
set -euo pipefail
docker rm -f swap-upstream-ui-on-build >/dev/null 2>&1 || true
docker run -d --name swap-upstream-ui-on-build -p 4326:4321 --add-host host.docker.internal:host-gateway \
  -e PORT=4321 -e PUBLIC_AUTH_KEYCLOAK_SERVER_URL= \
  -e PUBLIC_QUERY_MAPPER_ADAPTER_API_URL=http://host.docker.internal:18102 \
  -e PUBLIC_QUERY_MAPPER_ADAPTER_API_VERSION=v1 \
  -e PUBLIC_FEDERATED_CATALOGUE_API_URL=http://host.docker.internal:18101 \
  -e PUBLIC_SEARCH_API_URL=http://host.docker.internal:18101 \
  -e PUBLIC_CONTRACT_CONSUMPTION_API_URL= \
  simpl-catalogue-client:local >/dev/null
echo "upstream UI on the build: http://localhost:4326"
