// ─────────────────────────────────────────────────────────────────────────────
// Auth mode switch  (local-stack overlay — simpl-local/simpl-contract)
//
// Additive and backwards-compatible: VITE_PUBLIC_AUTH_MODE defaults to
// "keycloak", so the upstream Keycloak/OIDC behaviour is unchanged unless you
// explicitly opt out via configuration.
//
//   VITE_PUBLIC_AUTH_MODE=keycloak   (default) → Keycloak OIDC/PKCE login
//   VITE_PUBLIC_AUTH_MODE=disabled            → no external IdP; the app runs
//                                               with a static local identity
//                                               (no login screen, no redirect)
//
// This is the feature the sibling Simpl UIs already have (auth toggled off by
// configuration for local evaluation) and that contract-ui was missing.
// ─────────────────────────────────────────────────────────────────────────────

const raw = (import.meta.env.VITE_PUBLIC_AUTH_MODE ?? 'keycloak')
  .toString()
  .trim()
  .toLowerCase();

export type AuthMode = 'keycloak' | 'disabled';

export const AUTH_MODE: AuthMode =
  raw === 'disabled' || raw === 'none' || raw === 'off' ? 'disabled' : 'keycloak';

/** True when Keycloak/OIDC should be used; false when auth is bypassed. */
export const isAuthenticationEnabled = (): boolean => AUTH_MODE === 'keycloak';
