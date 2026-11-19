import Keycloak from "keycloak-js";

/** Runtime configuration served by nginx at /config.json (no URLs baked into the image). */
export interface UiConfig {
  keycloakUrl: string;
  realm: string;
  clientId: string;
}

let keycloak: Keycloak | null = null;

/** Logs the user in through Keycloak (OpenID Connect with PKCE); a user without a session is sent to login. */
export async function initAuth(config: UiConfig): Promise<void> {
  keycloak = new Keycloak({ url: config.keycloakUrl, realm: config.realm, clientId: config.clientId });
  await keycloak.init({ onLoad: "login-required", pkceMethod: "S256", checkLoginIframe: false });
}

/** Gives a fresh access token. */
export async function accessToken(): Promise<string> {
  if (!keycloak) {
    throw new Error("Not signed in");
  }
  try {
    await keycloak.updateToken(30);
  } catch {
    await keycloak.login();
  }
  return keycloak.token ?? "";
}

/** Tells whether the user holds GA_VOCABULARY_ADMIN (decision D18). */
export function isAdmin(): boolean {
  return keycloak?.hasRealmRole("GA_VOCABULARY_ADMIN") ?? false;
}

export function userName(): string {
  const parsed = keycloak?.tokenParsed as { preferred_username?: string } | undefined;
  return parsed?.preferred_username ?? "";
}

export function logout(): void {
  void keycloak?.logout({ redirectUri: window.location.origin });
}
