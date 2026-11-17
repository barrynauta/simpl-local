import Keycloak from 'keycloak-js'
import { config } from './config'

export const keycloak = new Keycloak({
  url: config.keycloakUrl,
  realm: config.realm,
  clientId: config.clientId
})

export async function initAuth(): Promise<void> {
  await keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
}

export function roles(): string[] {
  const parsed = keycloak.tokenParsed as { realm_access?: { roles?: string[] } } | undefined
  return parsed?.realm_access?.roles ?? []
}

export function hasRole(role: string): boolean {
  return roles().includes(role)
}

export function username(): string {
  const parsed = keycloak.tokenParsed as { preferred_username?: string } | undefined
  return parsed?.preferred_username ?? ''
}

export async function token(): Promise<string> {
  try {
    await keycloak.updateToken(30)
  } catch {
    keycloak.login()
  }
  return keycloak.token ?? ''
}
