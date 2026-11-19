import Keycloak from 'keycloak-js';
import type { UiConfig } from './config';

/** Login through the platform identity provider (Keycloak, OIDC code flow with PKCE): no own password handling. */
export interface Session {
  token(): Promise<string>;
  roles(): string[];
  username(): string;
  logout(): void;
}

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN';

export async function startSession(config: UiConfig): Promise<Session> {
  const keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId });
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  return {
    async token() {
      await keycloak.updateToken(30);
      return keycloak.token ?? '';
    },
    roles() {
      const parsed = keycloak.tokenParsed as { realm_access?: { roles?: string[] } } | undefined;
      return parsed?.realm_access?.roles ?? [];
    },
    username() {
      const parsed = keycloak.tokenParsed as { preferred_username?: string } | undefined;
      return parsed?.preferred_username ?? '';
    },
    logout() {
      void keycloak.logout({ redirectUri: window.location.origin });
    },
  };
}
