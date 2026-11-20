import Keycloak from 'keycloak-js';

/** Runtime configuration served by the UI proxy from its environment (E4). */
export interface UiConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';
export const VIEWER_ROLE = 'GA_VOCABULARY_VIEWER';

/** The signed-in Governance Authority user, as seen by the UI. */
export interface Session {
  token(): Promise<string>;
  roles(): string[];
  userName(): string;
  logout(): void;
}

let current: Session | null = null;

export function session(): Session {
  if (!current) {
    throw new Error('No session');
  }
  return current;
}

export function setSession(value: Session | null): void {
  current = value;
}

export function isAdmin(): boolean {
  return current !== null && current.roles().includes(ADMIN_ROLE);
}

/** Signs in through Keycloak (OpenID Connect, authorization code with PKCE, D18, D40). */
export async function signIn(config: UiConfig): Promise<Session> {
  const keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  const value: Session = {
    async token() {
      await keycloak.updateToken(30);
      return keycloak.token ?? '';
    },
    roles() {
      return keycloak.realmAccess?.roles ?? [];
    },
    userName() {
      const parsed = keycloak.tokenParsed as { preferred_username?: string } | undefined;
      return parsed?.preferred_username ?? '';
    },
    logout() {
      void keycloak.logout({ redirectUri: window.location.origin });
    },
  };
  setSession(value);
  return value;
}
