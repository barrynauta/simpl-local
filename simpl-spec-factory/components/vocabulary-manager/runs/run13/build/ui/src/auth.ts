import Keycloak from 'keycloak-js';

export interface RuntimeConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

export interface AuthSession {
  token(): Promise<string | undefined>;
  roles(): string[];
  login(): void;
  logout(): void;
  username(): string;
}

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';
export const VIEWER_ROLE = 'GA_VOCABULARY_VIEWER';

let session: AuthSession | undefined;

export function setSession(s: AuthSession): void {
  session = s;
}

export function getSession(): AuthSession | undefined {
  return session;
}

export function isAdmin(): boolean {
  return session?.roles().includes(ADMIN_ROLE) ?? false;
}

export async function loadConfig(fetchFn: typeof fetch = fetch): Promise<RuntimeConfig> {
  const response = await fetchFn('/config.json', { headers: { Accept: 'application/json' } });
  return (await response.json()) as RuntimeConfig;
}

/** Starts the OpenID Connect login (authorization code flow with PKCE) through keycloak-js. */
export async function initKeycloak(config: RuntimeConfig): Promise<AuthSession> {
  const keycloak = new Keycloak({
    url: config.keycloakUrl,
    realm: config.realm,
    clientId: config.clientId,
  });
  await keycloak.init({ onLoad: 'login-required', checkLoginIframe: false, pkceMethod: 'S256' });
  return {
    async token() {
      try {
        await keycloak.updateToken(30);
      } catch {
        await keycloak.login();
        return undefined;
      }
      return keycloak.token;
    },
    roles() {
      return keycloak.tokenParsed?.realm_access?.roles ?? [];
    },
    login() {
      void keycloak.login();
    },
    logout() {
      void keycloak.logout({ redirectUri: window.location.origin + '/' });
    },
    username() {
      const parsed = keycloak.tokenParsed as { preferred_username?: string } | undefined;
      return parsed?.preferred_username ?? '';
    },
  };
}
