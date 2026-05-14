// Runtime config injected into window._env_ by the UI's index.html.
//
// Empty Keycloak fields disable the navigation guard via the UI's built-in
// isAuthenticationEnabled() switch (src/services/keycloak.ts):
//   isAuthenticationEnabled = () => server.length>0 && clientId.length>0 && realm.length>0
// → routes skip the Keycloak redirect.
//
// PUBLIC_SCHEMA_MANAGER_API_URL is a relative path. The UI is served by nginx
// (this stack's UI container); requests to /v1/* are reverse-proxied to the
// schema-manager backend with an injected Authorization header — see nginx.conf.
window._env_ = {
  PUBLIC_AUTH_KEYCLOAK_SERVER_URL: "",
  PUBLIC_AUTH_KEYCLOAK_REALM: "",
  PUBLIC_AUTH_KEYCLOAK_CLIENT_ID: "",
  PUBLIC_SCHEMA_MANAGER_API_URL: "/v1",
};
