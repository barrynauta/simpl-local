import Keycloak from 'keycloak-js';
import { config } from './config';

// Sign-in through the realm with the public client schema-ui, authorization code + PKCE (D45).
export const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.realm,
  clientId: config.clientId,
});

export async function initAuth(): Promise<void> {
  await keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false,
  });
}

export async function token(): Promise<string> {
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }
  return keycloak.token ?? '';
}

function roles(): string[] {
  const parsed = keycloak.tokenParsed as { roles?: unknown } | undefined;
  return Array.isArray(parsed?.roles) ? (parsed!.roles as string[]) : [];
}

export const isAdmin = (): boolean => roles().includes('GA_SCHEMA_ADMIN');
export const isViewer = (): boolean => isAdmin() || roles().includes('GA_SCHEMA_VIEWER');
export const username = (): string =>
  ((keycloak.tokenParsed as { preferred_username?: string } | undefined)?.preferred_username) ?? '';

export function logout(): void {
  keycloak.logout({ redirectUri: window.location.origin + '/' });
}
