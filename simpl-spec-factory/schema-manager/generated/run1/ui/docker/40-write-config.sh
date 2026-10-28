#!/bin/sh
# Writes the runtime configuration of the single page application from environment variables.
set -e
cat > /usr/share/nginx/html/config.js <<CFG
window.__SM_CONFIG__ = {
  keycloakUrl: '${KEYCLOAK_URL:-http://localhost:8185}',
  realm: '${KEYCLOAK_REALM:-simpl}',
  clientId: '${KEYCLOAK_UI_CLIENT_ID:-schema-ui}',
  apiBase: '${UI_API_BASE:-/v1}',
};
CFG
