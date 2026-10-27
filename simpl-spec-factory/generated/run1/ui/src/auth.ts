import { User, UserManager, WebStorageStateStore } from 'oidc-client-ts'
import { reactive } from 'vue'
import { getConfig } from './config'

export const ADMIN = 'GA_VOCABULARY_ADMIN'
export const VIEWER = 'GA_VOCABULARY_VIEWER'

/** Reactive session state used by every view. */
export const session = reactive({
  ready: false,
  user: null as User | null,
  username: '',
  roles: [] as string[],
  get isAdmin(): boolean {
    return this.roles.includes(ADMIN)
  },
  get canRead(): boolean {
    return this.roles.includes(ADMIN) || this.roles.includes(VIEWER)
  },
})

let manager: UserManager | null = null

function userManager(): UserManager {
  if (!manager) {
    const cfg = getConfig()
    manager = new UserManager({
      authority: cfg.authority,
      client_id: cfg.clientId,
      redirect_uri: `${window.location.origin}/callback`,
      post_logout_redirect_uri: `${window.location.origin}/`,
      response_type: 'code', // authorization code flow; oidc-client-ts always adds PKCE (S256)
      scope: 'openid profile email',
      userStore: new WebStorageStateStore({ store: window.sessionStorage }),
      automaticSilentRenew: false,
    })
  }
  return manager
}

/** Roles come from the top-level 'roles' claim of the access token (D6). */
function rolesFromToken(token: string): string[] {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const json = JSON.parse(decodeURIComponent(escape(atob(payload))))
    return Array.isArray(json.roles) ? json.roles.map(String) : []
  } catch {
    return []
  }
}

function setUser(user: User | null) {
  session.user = user
  session.username = (user?.profile?.preferred_username as string) ?? ''
  session.roles = user ? rolesFromToken(user.access_token) : []
}

export async function initAuth(): Promise<void> {
  const user = await userManager().getUser()
  setUser(user && !user.expired ? user : null)
  session.ready = true
}

export async function login(returnTo: string = window.location.pathname + window.location.search) {
  await userManager().signinRedirect({ state: returnTo })
}

export async function completeLogin(): Promise<string> {
  const user = await userManager().signinRedirectCallback()
  setUser(user)
  return typeof user.state === 'string' ? user.state : '/'
}

export async function logout() {
  const mgr = userManager()
  const user = await mgr.getUser()
  await mgr.removeUser()
  setUser(null)
  await mgr.signoutRedirect({ id_token_hint: user?.id_token })
}

export function accessToken(): string | null {
  const u = session.user
  if (!u || u.expired) return null
  return u.access_token
}
