import Keycloak from 'keycloak-js';
import type { AppConfig } from './config';

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';
export const VIEWER_ROLE = 'GA_VOCABULARY_VIEWER';

let keycloak: Keycloak | undefined;

/** Signs the user in through Keycloak; a user without a session is sent to the login page (decision D18). */
export async function initAuth(config: AppConfig): Promise<void> {
  keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
}

/** A fresh access token for the API. */
export async function accessToken(): Promise<string> {
  if (!keycloak) throw new Error('Not signed in.');
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }
  return keycloak.token ?? '';
}

export function hasRole(role: string): boolean {
  const roles = (keycloak?.tokenParsed as { realm_access?: { roles?: string[] } } | undefined)?.realm_access
    ?.roles;
  return roles?.includes(role) ?? false;
}

export function isAdmin(): boolean {
  return hasRole(ADMIN_ROLE);
}

export function userName(): string {
  const parsed = keycloak?.tokenParsed as { email?: string; preferred_username?: string } | undefined;
  return parsed?.email ?? parsed?.preferred_username ?? '';
}

export function logout(): void {
  void keycloak?.logout({ redirectUri: window.location.origin + '/' });
}
