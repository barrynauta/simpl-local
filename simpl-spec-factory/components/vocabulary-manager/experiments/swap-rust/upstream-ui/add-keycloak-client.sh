#!/usr/bin/env bash
# Adds a public client for the UPSTREAM UI to the Rust build's Keycloak (realm simpl), at runtime via the admin API.
# Client id frontend-cli is the one the upstream env-config.example.js uses.
# The 'roles' realm-role mapper puts the roles in a top-level 'roles' claim, which both the Rust
# backend and the upstream UI (getTokenRoles) read.
set -euo pipefail
KC=${KC:-http://localhost:8184}
adm=$(curl -s -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  $KC/realms/master/protocol/openid-connect/token | python3 -c 'import json,sys;print(json.load(sys.stdin)["access_token"])')
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST $KC/admin/realms/simpl/clients \
  -H "Authorization: Bearer $adm" -H 'Content-Type: application/json' -d '{
  "clientId": "frontend-cli", "enabled": true, "publicClient": true, "protocol": "openid-connect",
  "standardFlowEnabled": true, "directAccessGrantsEnabled": false, "fullScopeAllowed": true,
  "redirectUris": ["http://localhost:4323/*"], "webOrigins": ["http://localhost:4323"],
  "attributes": {"pkce.code.challenge.method": "S256", "post.logout.redirect.uris": "http://localhost:4323/*"},
  "protocolMappers": [{"name": "roles", "protocol": "openid-connect", "protocolMapper": "oidc-usermodel-realm-role-mapper",
    "config": {"multivalued": "true", "claim.name": "roles", "jsonType.label": "String",
      "access.token.claim": "true", "id.token.claim": "true", "userinfo.token.claim": "true"}}]}')
echo "create client frontend-cli: HTTP $code (201 created, 409 already there)"
