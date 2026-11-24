import Keycloak from 'keycloak-js';
import { reactive } from 'vue';
import { config } from './config';

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';
export const VIEWER_ROLE = 'GA_VOCABULARY_VIEWER';

export interface Session {
  authenticated: boolean;
  username: string;
  roles: string[];
}

/** The signed-in user (D16, D18). Tests set it directly. */
export const session = reactive<Session>({ authenticated: false, username: '', roles: [] });

let keycloak: Keycloak | null = null;

/** Sends a user without a session to the Keycloak login page (D18). */
export async function initAuth(): Promise<void> {
  const settings = config();
  keycloak = new Keycloak({ url: settings.keycloakUrl, realm: settings.realm, clientId: settings.clientId });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  refreshSession();
}

function refreshSession(): void {
  const parsed = keycloak?.tokenParsed as
    { preferred_username?: string; realm_access?: { roles?: string[] } } | undefined;
  session.authenticated = !!keycloak?.authenticated;
  session.username = parsed?.preferred_username ?? '';
  session.roles = parsed?.realm_access?.roles ?? [];
}

/** A valid access token, refreshed when it is about to expire. */
export async function accessToken(): Promise<string | undefined> {
  if (!keycloak) {
    return undefined;
  }
  try {
    await keycloak.updateToken(30);
    refreshSession();
  } catch {
    await keycloak.login();
  }
  return keycloak.token;
}

export function logout(): void {
  void keycloak?.logout({ redirectUri: window.location.origin + '/' });
}

export function isAdmin(): boolean {
  return session.roles.includes(ADMIN_ROLE);
}

export function canRead(): boolean {
  return session.roles.includes(ADMIN_ROLE) || session.roles.includes(VIEWER_ROLE);
}
