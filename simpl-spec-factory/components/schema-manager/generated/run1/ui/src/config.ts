export const config = {
  keycloakUrl: window.__SM_CONFIG__?.keycloakUrl ?? 'http://localhost:8185',
  realm: window.__SM_CONFIG__?.realm ?? 'simpl',
  clientId: window.__SM_CONFIG__?.clientId ?? 'schema-ui',
  apiBase: window.__SM_CONFIG__?.apiBase ?? '/v1',
};
