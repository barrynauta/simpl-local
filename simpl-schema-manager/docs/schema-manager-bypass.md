# Schema Manager — Keycloak bypass mechanics

This stack runs the upstream `simpl-schema-manager-ui` against the upstream `simpl-schema-manager`
backend with **no Keycloak instance** and **no source patching**. Both the UI and backend ship with
auth checks that, looked at closely, are loose enough to short-circuit cleanly.

This document explains how, so you can reason about what's safe to do with this stack and what isn't.

## TL;DR

```
┌──────────────┐    UI's built-in auth-disable switch    ┌──────────────┐
│   Vue UI     │ ─── Keycloak env vars set to empty ───▶ │ no redirect  │
│              │    isAuthenticationEnabled() == false   │ to Keycloak  │
└──────────────┘                                         └──────────────┘
       │
       │ GET /v1/schemas
       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  nginx (same container as UI)                                        │
│                                                                      │
│   location /v1/ {                                                    │
│     proxy_set_header Authorization "Bearer <fake-jwt>";              │
│     rewrite ^/v1/(.*)$ /$1 break;                                    │
│     proxy_pass http://schema-manager:8085;                           │
│   }                                                                  │
└──────────────────────────────────────────────────────────────────────┘
       │
       │ GET /schemas    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
       ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Spring Boot schema-manager                                          │
│                                                                      │
│   @RequestHeader("Authorization") String authToken                   │
│     ↓                                                                │
│   RoleUtil.validateRoles(authToken, [GA_SCHEMA_ADMIN])               │
│     ↓                                                                │
│   JWT.decode(token)  ◀── auth0/java-jwt; payload-only, no            │
│     .getClaim("realm_access")    signature/exp/issuer check          │
│     .roles  ⊇  ["GA_SCHEMA_ADMIN"]   →  pass                         │
└──────────────────────────────────────────────────────────────────────┘
```

## Three load-bearing facts

### 1. The UI is opt-out by design

[`src/services/keycloak.ts`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager-ui/-/blob/main/src/services/keycloak.ts):

```ts
export const isAuthenticationEnabled = () =>
  keycloakServer?.length > 0 && clientId?.length > 0 && realm?.length > 0;
```

[`src/router/authentication.ts`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager-ui/-/blob/main/src/router/authentication.ts):

```ts
if (to.meta?.noAuthentication || !isAuthenticationEnabled()) {
  return true;   // ← guard bypasses entirely
}
```

`env-config.local.js` sets `PUBLIC_AUTH_KEYCLOAK_SERVER_URL`, `_REALM`, and `_CLIENT_ID` to empty
strings, so `isAuthenticationEnabled()` returns false and every route loads without a redirect.
No UI source patching. No `git apply`. No fork.

### 2. The backend never reaches for JWT verification

[`src/main/java/eu/europa/ec/simpl/util/role/RoleUtil.java`](https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager/-/blob/main/src/main/java/eu/europa/ec/simpl/util/role/RoleUtil.java):

```java
public static void validateRoles(final String token, final List<String> expectedRoles) {
    final var tokenWithoutBearerPrefix = token.replace("Bearer ", "");
    final var hasProperRole = extractRolesFromToken(tokenWithoutBearerPrefix).stream()
            .anyMatch(expectedRoles::contains);
    ...
}

private static List<String> extractRolesFromToken(final String token) {
    return JWT.decode(token)
            .getClaim("realm_access")
            .as(RealmAccessRoles.class)
            .getRoles();
}
```

`com.auth0.jwt.JWT.decode()` (per the auth0 library docs) is documented as **"this method
doesn't verify the JWT's signature."** No expiry check. No issuer check. No signing-key
configuration anywhere in the codebase (no `SecurityConfig`, no `SecurityFilter`, no
`JwtDecoder` bean — verified by grepping `src/main/java`).

The backend is designed to run **behind** Tier-1, which validates and forwards the token. Once the
token reaches the schema-manager, it is treated as a trusted source of claims.

### 3. nginx can inject any header it wants

Because the UI is served by nginx anyway (the upstream Dockerfile builds on
`nginxinc/nginx-unprivileged:1`), we replace its config in `Dockerfile.local-ui` with one that:

- Serves the SPA at `/` with the usual `try_files $uri $uri/ /index.html` fallback.
- Proxies `/v1/*` to `http://schema-manager:8085/*`, rewriting the path to strip `/v1` and
  setting `Authorization` to the fake JWT.

The UI calls relative `/v1/...` URLs (configured via `env-config.local.js`), so the browser
never sees the injected header — it's added at the proxy.

## The fake JWT

```
header   {"alg":"HS256","typ":"JWT"}
payload  {
           "sub": "local-dev",
           "preferred_username": "local-dev",
           "given_name": "Local",
           "family_name": "Dev",
           "name": "Local Dev",
           "email": "local-dev@example.com",
           "realm_access": { "roles": ["GA_SCHEMA_ADMIN", "GA_SCHEMA_VIEWER"] }
         }
signature  arbitrary base64url bytes (never verified)
```

`GA_SCHEMA_ADMIN` and `GA_SCHEMA_VIEWER` are the two roles the controllers check for; including
both lets the same token satisfy both `validateRoles` call sites. The display claims
(`preferred_username`, `given_name`, `family_name`, `name`, `email`) keep the UI's toolbar from
showing `undefined`.

## What this stack does NOT prove

- **Real auth integration.** Substituting the JWT does not validate the Keycloak ↔ Tier-1 ↔
  schema-manager handshake. If upstream tightens `RoleUtil` to call `JWT.require(...).verify()`,
  this bypass breaks immediately — and that is the correct outcome, because production has been
  protecting itself against a different attack model than the local stack does.
- **Signature/issuer trust.** A real deployment would reject this token. Treat the bypass as
  "I trust whatever client is sending me requests because there is no client other than
  localhost." Never reuse this config anywhere networked.

## How to verify the bypass works

```bash
# Direct backend — gated, returns 400
curl -s -o /dev/null -w "direct  HTTP %{http_code}\n" http://localhost:8085/schemas

# Through the UI proxy — auth header injected, /v1 stripped, returns 200
curl -s -o /dev/null -w "proxied HTTP %{http_code}\n" http://localhost:4322/v1/schemas
```

Expected:

```
direct  HTTP 400
proxied HTTP 200
```

Bruno test `06-ui-proxy-injects-auth.bru` pins this contrast.
