#!/bin/sh
# Renders the nginx configuration and the UI runtime configuration from the environment, then runs nginx.
set -eu
: "${KEYCLOAK_PUBLIC_URL:=http://localhost:8193}"
export KEYCLOAK_PUBLIC_URL
envsubst '${KEYCLOAK_PUBLIC_URL}' < /etc/nginx/templates-src/nginx.conf.template > /tmp/nginx.conf
printf '{ "keycloakUrl": "%s", "realm": "%s", "clientId": "%s", "apiBase": "/v1" }\n' \
  "$KEYCLOAK_PUBLIC_URL" "${KEYCLOAK_REALM:-simpl}" "${KEYCLOAK_CLIENT_ID:-vocabulary-manager-ui}" > /tmp/config.json
exec nginx -c /tmp/nginx.conf -g 'daemon off;'
