# infrastructure-fe: component architecture

React 18 + Vite single-page app (artifact `ionos-fe`), built to static assets and
served by nginx. In this stack it is published on the host at `:3001`.

## Structure and runtime configuration

```mermaid
flowchart TB
    subgraph img["nginx image (multi-stage build)"]
        direction TB
        ENTRY["99-custom-env.sh (entrypoint)\nsed-replaces placeholders in the\nminified bundle at container start"]
        NGINX["nginx :80\ntry_files -> index.html (SPA)"]
        DIST["/usr/share/nginx/html\n(built React bundle)"]
    end
    ENTRY --> DIST
    NGINX --> DIST
    Browser["Browser"] -->|HTTP :3001| NGINX
    Browser -->|REST| BE["infrastructure-be :8080"]
    Browser -.->|OIDC login| KC["Keycloak (not in stack)"]
```

Config is a mix: the **API base URL** and **Keycloak** settings are placeholder
tokens (`##VITEAPIBASEURL##`, etc.) baked into the bundle and rewritten by the
container entrypoint via `sed` at start. This is a runtime-config pattern (build
once, configure at deploy), but string-replacing minified JS is brittle
(see fe-findings F-FE-3).

## Authentication flow

The SPA implements the OAuth2 Authorization Code + PKCE flow by hand (it does not
use `keycloak-js`):

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant FE as SPA
    participant KC as Keycloak
    participant BE as infrastructure-be

    U->>FE: open app
    FE->>FE: create PKCE verifier -> localStorage
    FE->>KC: redirect to authorize (code + PKCE)
    KC-->>FE: redirect back with code
    FE->>KC: exchange code for tokens
    KC-->>FE: access + refresh + id token
    FE->>FE: store tokens in sessionStorage (see fe-findings F-FE-1)
    FE->>BE: REST with Authorization: Bearer <access_token>
```

Because this stack has no Keycloak, the app renders and initiates the auth redirect
but cannot reach the authenticated UI. The backend accepts requests regardless
(auth disabled), so the API is reachable directly for testing.

## Security-relevant facts

- Access and refresh tokens live in `sessionStorage`; the PKCE verifier in
  `localStorage` (fe-findings F-FE-1).
- nginx serves **no** security headers and there is no CSP (fe-findings F-FE-2).
- Route authorisation reads roles from `sessionStorage` (client-side only;
  fe-findings F-FE-3).
- No secrets are committed: `.env` holds only URLs and the public Keycloak client id
  `frontend-cli` (public client + PKCE).

## Build and test

- Build: `npm ci` then `vite build` (Node 18 in the Dockerfile; builds under newer
  Node too). Output is static assets served by nginx.
- Tests: Cypress **component** tests (`npx cypress run --component`), 26 specs. Run
  headless with the bundled Electron. See fe-findings F-FE-4 for the current pass/fail
  and the Node-version caveat.
