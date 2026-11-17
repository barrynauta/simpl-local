import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts'

export interface UiConfig {
  keycloakUrl: string
  realm: string
  party: string
}

let manager: UserManager | null = null
let config: UiConfig | null = null

export async function loadConfig(): Promise<UiConfig> {
  if (config) return config
  const res = await fetch('/config.json', { cache: 'no-store' })
  config = (await res.json()) as UiConfig
  return config
}

export async function userManager(): Promise<UserManager> {
  if (manager) return manager
  const c = await loadConfig()
  manager = new UserManager({
    authority: `${c.keycloakUrl}/realms/${c.realm}`,
    client_id: 'contract-ui',
    redirect_uri: `${window.location.origin}/callback`,
    post_logout_redirect_uri: `${window.location.origin}/`,
    response_type: 'code',
    scope: 'openid profile email',
    userStore: new WebStorageStateStore({ store: window.sessionStorage }),
    automaticSilentRenew: false,
  })
  return manager
}

export async function currentUser(): Promise<User | null> {
  const m = await userManager()
  const u = await m.getUser()
  return u && !u.expired ? u : null
}

export async function login(target: string): Promise<void> {
  const m = await userManager()
  await m.signinRedirect({ state: { target } })
}

export async function completeLogin(): Promise<string> {
  const m = await userManager()
  const u = await m.signinRedirectCallback()
  const state = u.state as { target?: string } | undefined
  return state?.target ?? '/'
}

export async function logout(): Promise<void> {
  const m = await userManager()
  await m.signoutRedirect()
}

export function rolesOf(accessToken: string): string[] {
  try {
    const payload = JSON.parse(atob(accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload?.realm_access?.roles ?? []
  } catch {
    return []
  }
}
