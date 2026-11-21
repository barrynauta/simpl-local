import { reactive } from 'vue';

export const ROLE_ADMIN = 'GA_VOCABULARY_ADMIN';
export const ROLE_VIEWER = 'GA_VOCABULARY_VIEWER';

/** The signed-in Governance Authority user, as read from the Keycloak token (E1, D16). */
export const session = reactive({
  username: '',
  roles: [] as string[],
});

let tokenProvider: () => Promise<string | undefined> = async () => undefined;
let logoutHandler: () => void = () => undefined;

export function configureSession(options: {
  username: string;
  roles: string[];
  tokenProvider: () => Promise<string | undefined>;
  logout: () => void;
}): void {
  session.username = options.username;
  session.roles = [...options.roles];
  tokenProvider = options.tokenProvider;
  logoutHandler = options.logout;
}

/** D16, D18: only GA_VOCABULARY_ADMIN may create, update and change status. */
export function isAdmin(): boolean {
  return session.roles.includes(ROLE_ADMIN);
}

export function getToken(): Promise<string | undefined> {
  return tokenProvider();
}

export function logout(): void {
  logoutHandler();
}
