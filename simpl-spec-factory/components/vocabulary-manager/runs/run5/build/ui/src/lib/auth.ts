import Keycloak from 'keycloak-js';
import type { UiConfig } from './config';

/** Keycloak login (D18: a user without a token is sent to the Keycloak login page). */
let keycloak: Keycloak | null = null;

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';
export const VIEWER_ROLE = 'GA_VOCABULARY_VIEWER';

export async function initAuth(config: UiConfig): Promise<void> {
  keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
}

export async function token(): Promise<string | undefined> {
  if (!keycloak) {
    return undefined;
  }
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }
  return keycloak.token;
}

export function hasRole(role: string): boolean {
  return keycloak?.realmAccess?.roles?.includes(role) ?? false;
}

/** True for a Governance Authority user with GA_VOCABULARY_ADMIN (D16, D18). */
export function isAdmin(): boolean {
  return hasRole(ADMIN_ROLE);
}

export function userName(): string {
  const parsed = keycloak?.tokenParsed as { preferred_username?: string } | undefined;
  return parsed?.preferred_username ?? '';
}

export function logout(): void {
  void keycloak?.logout({ redirectUri: window.location.origin });
}
