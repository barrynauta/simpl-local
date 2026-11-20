import Keycloak from 'keycloak-js';

/** Runtime configuration served by nginx from the environment (MACH-07). */
export interface UiConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

/** The session seen by the views. */
export interface Session {
  token(): Promise<string>;
  isAdmin(): boolean;
  username(): string;
  logout(): void;
}

const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';
let current: Session | null = null;

/** Sets the session (tests use a stub). */
export function setSession(session: Session): void {
  current = session;
}

/** The current session. */
export function session(): Session {
  if (!current) {
    throw new Error('No session');
  }
  return current;
}

/** Logs in through Keycloak with PKCE (D18: a user without a token goes to the login page). */
export async function login(config: UiConfig): Promise<Session> {
  const keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  const result: Session = {
    async token() {
      await keycloak.updateToken(30);
      return keycloak.token ?? '';
    },
    isAdmin() {
      return keycloak.hasRealmRole(ADMIN_ROLE);
    },
    username() {
      const parsed = keycloak.tokenParsed as { preferred_username?: string } | undefined;
      return parsed?.preferred_username ?? '';
    },
    logout() {
      void keycloak.logout({ redirectUri: window.location.origin });
    },
  };
  setSession(result);
  return result;
}
