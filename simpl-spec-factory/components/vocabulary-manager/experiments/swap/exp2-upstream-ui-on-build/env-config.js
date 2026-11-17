// Upstream UI runtime config for experiment 2: Keycloak ON (build's Keycloak), API = build backend.
// The upstream UI builds its Keycloak URLs as <server>/auth/realms/<realm>/... (legacy /auth prefix).
// Keycloak 26 has no /auth prefix, so the server URL is the UI origin itself and nginx
// (nginx.conf) maps /auth/ to the build's Keycloak. No fake token cookie is planted.
globalThis.window._env_ = {
  PUBLIC_AUTH_KEYCLOAK_SERVER_URL: 'http://localhost:4323',
  PUBLIC_AUTH_KEYCLOAK_REALM: 'simpl',
  PUBLIC_AUTH_KEYCLOAK_CLIENT_ID: 'frontend-cli',
  PUBLIC_AUTH_DEV_MOCK_TOKEN: '',
  PUBLIC_VOCABULARY_MANAGER_API_URL: '/api/',
};
