# contract-ui overlay

Files here are copied **over** the cloned `repos/contract-ui` source during the
UI image build (`Dockerfile.local-ui`, after the repo copy, before `npm run
build`). The clone stays pristine and pull-safe; the patch lives in the stack.

## What it adds: a configurable auth switch

contract-ui hard-wires Keycloak — every protected route redirects to the IdP and
there is no way to run it without one (unlike the sibling Simpl UIs). This
overlay adds the missing **`VITE_PUBLIC_AUTH_MODE`** configuration:

| Value | Behaviour |
|---|---|
| `keycloak` (default) | unchanged upstream Keycloak OIDC/PKCE login |
| `disabled` (`none` / `off`) | no external IdP; app runs with a static local identity, no login screen, no redirect |

Default is `keycloak`, so the change is **additive and backwards-compatible** —
upstream behaviour only changes when you explicitly opt out.

Files:
- `src/auth/authConfig.ts` — **new.** Reads the mode, exposes
  `isAuthenticationEnabled()`.
- `src/auth/hooks/useAuth.ts` — **patched.** Short-circuits `processAuthFlow`
  (mark authenticated, skip Keycloak) and makes `logout` a no-op when disabled.
  The returned `{ isAuthenticated, loading, logout }` interface is unchanged.

## What it adds: a real read-path page

- `src/pages/ContractViewPage/ContractViewPage.tsx` — **replaced.** The upstream
  page is a static placeholder ("Here contract informations"); this version
  calls `GET /contract/v1/agreements/{id}` (relative URL → nginx proxy, which
  injects the API key) and renders the agreement, with an id box to load others.
  Pairs with the stack's `samples/seed.sql` / `./seed.sh`.

## Upstream-drift caveat

`useAuth.ts` is a **full-file** overlay. If SC-1 changes `useAuth.ts` upstream,
this copy will mask those changes — re-sync it after a `contract-ui` update.
`authConfig.ts` is a new file and carries no drift risk.

This is a clean, configuration-driven change that could be lifted upstream as a
contribution (it closes a real gap: contract-ui cannot currently run without a
reachable Keycloak).
