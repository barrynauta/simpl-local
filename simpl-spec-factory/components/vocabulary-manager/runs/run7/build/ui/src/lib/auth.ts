import Keycloak from 'keycloak-js'

/** Runtime configuration served by nginx from environment variables (E4). */
export interface UiConfig {
  keycloakUrl: string
  realm: string
  clientId: string
}

export const ADMIN_ROLE = 'GA_VOCABULARY_ADMIN'
export const VIEWER_ROLE = 'GA_VOCABULARY_VIEWER'

let keycloak: Keycloak | null = null

/** Logs the user in through Keycloak (OIDC code flow with PKCE, D18: no token means the login page). */
export async function initAuth(config: UiConfig): Promise<void> {
  keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId })
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
}

/** A fresh access token for API calls. */
export async function token(): Promise<string> {
  if (!keycloak) {
    throw new Error('Not authenticated')
  }
  try {
    await keycloak.updateToken(30)
  } catch {
    await keycloak.login()
  }
  return keycloak.token ?? ''
}

export function roles(): string[] {
  return keycloak?.realmAccess?.roles ?? []
}

/** Write actions are shown only to GA_VOCABULARY_ADMIN (D16, D18). */
export function isAdmin(): boolean {
  return roles().includes(ADMIN_ROLE)
}

export function userName(): string {
  const parsed = keycloak?.tokenParsed as { preferred_username?: string } | undefined
  return parsed?.preferred_username ?? ''
}

export function logout(): void {
  void keycloak?.logout({ redirectUri: window.location.origin + '/vocabularies' })
}
