import Keycloak from 'keycloak-js';

// Keycloak is reached by the browser on port 18080 of the same host (digest D68).
export const keycloak = new Keycloak({
  url: `${window.location.protocol}//${window.location.hostname}:18080`,
  realm: 'dataspace-a',
  clientId: 'catalogue-ui',
});

export async function initAuth(): Promise<void> {
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
}

export async function bearer(): Promise<string> {
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }
  return `Bearer ${keycloak.token}`;
}
