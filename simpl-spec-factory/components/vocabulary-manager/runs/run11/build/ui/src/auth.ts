import Keycloak from "keycloak-js";

/** Keycloak login (D18, D40): authorization code flow with PKCE; no scopes beyond openid. */
export interface Session {
  token(): Promise<string>;
  roles(): string[];
  username(): string;
  logout(): void;
}

const ADMIN = "GA_VOCABULARY_ADMIN";

let current: Session | null = null;

export function session(): Session | null {
  return current;
}

export function setSession(value: Session | null): void {
  current = value;
}

export function isAdmin(): boolean {
  return current?.roles().includes(ADMIN) ?? false;
}

export async function login(): Promise<Session> {
  const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8193",
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? "simpl",
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT ?? "vocabulary-manager-ui",
  });
  await keycloak.init({ onLoad: "login-required", pkceMethod: "S256", checkLoginIframe: false });
  const created: Session = {
    async token() {
      await keycloak.updateToken(30);
      return keycloak.token ?? "";
    },
    roles() {
      const access = keycloak.tokenParsed?.realm_access;
      return access?.roles ?? [];
    },
    username() {
      return (keycloak.tokenParsed?.preferred_username as string | undefined) ?? "";
    },
    logout() {
      void keycloak.logout({ redirectUri: window.location.origin + "/vocabularies" });
    },
  };
  setSession(created);
  return created;
}
