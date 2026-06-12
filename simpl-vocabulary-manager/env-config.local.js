// Local-stack runtime config for simpl-vocabulary-manager-ui.
// Served as /env-config.js (loaded by index.html before the app bundle).
//
// Empty Keycloak values flip the app's built-in isAuthenticationEnabled()
// switch (src/services/keycloak.ts) — the router guard then passes without
// any login flow. Same pattern as the schema-manager stack.
globalThis.window._env_ = {
  PUBLIC_AUTH_KEYCLOAK_SERVER_URL: '',
  PUBLIC_AUTH_KEYCLOAK_REALM: '',
  PUBLIC_AUTH_KEYCLOAK_CLIENT_ID: '',
  PUBLIC_AUTH_DEV_MOCK_TOKEN: '',
  // Relative URL — nginx proxies /api/ to the backend container, so the
  // browser never needs CORS (the backend has no CORS config).
  PUBLIC_VOCABULARY_MANAGER_API_URL: '/api/',
};

// Plant a long-lived token cookie (exp 2100-01-01). The UI's role gates
// (useVocabularyAccess: GA_VOCABULARY_ADMIN/GA_VOCABULARY_VIEWER) read this
// cookie, and useVocabularyApi forwards it as the Bearer token — which the
// backend only JWT.decode()s for its 'email' claim, never verifies.
// PUBLIC_AUTH_DEV_MOCK_TOKEN can't do this job: it is compiled out of
// production builds (import.meta.env.DEV check in src/util/authentication.ts).
document.cookie =
  'token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImxvY2FsQHNpbXBsLmxvY2FsIiwicm9sZXMiOlsiR0FfVk9DQUJVTEFSWV9BRE1JTiJdLCJleHAiOjQxMDI0NDQ4MDB9.devsignature; path=/; SameSite=Strict';
