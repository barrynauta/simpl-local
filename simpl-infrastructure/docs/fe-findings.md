# infrastructure-fe: findings from local build/run (2026-07-12)

Component: `development/infrastructure/infrastructure-fe` (React 18 + Vite SPA, artifact `ionos-fe`, served by nginx), `main` branch. Assessed by installing deps, building the production bundle, serving it, running the Cypress component suite, and reading the auth/token/nginx surface.

Verified: `npm ci` (983 pkgs) and `npm run build-vite` green (built in ~2s); the bundle serves (HTTP 200, SPA fallback works). Cypress component suite: **80 tests, 71 passing, 6 failing, 3 pending across 26 specs** (see F-FE-4). This is a browser SPA whose auth is an interactive Keycloak login, so a fully interactive UI needs Keycloak; that dependency is why this is not a lightweight standalone stack (build+serve+tests are the useful signal here).

Severity: **critical** = exploitable as-is; **high** = serious, real impact; **medium** = should fix.

---

## F-FE-1: MEDIUM-HIGH (security): OAuth access and refresh tokens stored in Web Storage

`src/keycloack/authentication.ts`:
```js
sessionStorage.setItem('access_token', accessToken);
sessionStorage.setItem('refresh_token', refreshToken);
sessionStorage.setItem('expires_in', expiresIn);
localStorage.setItem("pkceCodeVerifier", pkceCodeVerifier);
```
Every API call reads the bearer from `sessionStorage` (`src/services/generics/Header.ts`, `baseUrl.ts`, and per-service modules) and sets `Authorization: Bearer <access_token>`.

Storing the **access and refresh tokens in `sessionStorage`** exposes them to theft by any XSS in the app: JavaScript can read Web Storage directly. The refresh token is the more serious item, since it lets an attacker mint new access tokens. The `pkceCodeVerifier` is put in `localStorage` (persists beyond the tab). The app also hand-rolls the OAuth2/PKCE flow rather than delegating to `keycloak-js`, which enlarges the surface for subtle mistakes.

**Fix:** prefer the BFF pattern (tokens in httpOnly, SameSite cookies handled server-side) or, if that is out of scope, keep the access token in memory only and never persist the refresh token in Web Storage; if `keycloak-js` is the intended library, use it rather than a bespoke flow. At minimum move `pkceCodeVerifier` to `sessionStorage`.

## F-FE-2: MEDIUM (security): no Content-Security-Policy and no security headers

Neither `nginx/default.conf` nor `nginx/nginx.conf` sets any security header, and there is no CSP anywhere in the app (no meta CSP in `index.html`, no helmet). `default.conf` is just `root` + `try_files`.

CSP is the primary mitigation against exactly the XSS that would steal the Web-Storage tokens in F-FE-1, so the two compound. Also missing: `X-Content-Type-Options: nosniff`, `X-Frame-Options`/`frame-ancestors` (clickjacking), `Referrer-Policy`, and `server_tokens off` (nginx version is advertised).

**Fix:** add a restrictive `Content-Security-Policy` (script-src 'self', connect-src limited to the API and Keycloak origins, frame-ancestors 'none'), plus the standard headers, in the nginx config. Turn off `server_tokens`.

## F-FE-3: LOW-MEDIUM: client-side role gating and runtime config via `sed` on the minified bundle

- Route authorization reads roles from Web Storage: `AuthRoutes.tsx` does `JSON.parse(sessionStorage.getItem("roles") || "[]")`. This is a client-side gate only; real enforcement must be server-side. It matters here because the paired backend has authentication fully disabled (see the infrastructure-be findings), so nothing enforces these roles on the API. Not a defect in the FE per se, but the pairing means the role model is currently decorative end to end.
- The container entrypoint `99-custom-env.sh` injects runtime config by `sed`-replacing placeholder tokens (`##VITEAPIBASEURL##`, Keycloak values) inside the built `assets/index-*.js`. It works, but string-replacing minified JS is brittle (a bundler change to how the literal is emitted silently breaks injection). A generated `config.json` read at runtime (one already exists in `dist/`, currently unused for this purpose) would be sturdier.

## F-FE-4: test suite: 6 failures, attribution uncertain (likely toolchain skew)

`npx cypress run --component` (Cypress 13.15, bundled Electron, headless) reports 71/80 passing; 6 failures in `input/SIMPLCheckBox.cy.tsx` (4) and `input/SIMPLSelectDropdown.cy.tsx` (1) plus one more, with errors including `TypeError: Cannot read properties of undefined (reading 'get')` (from application code), chai-jQuery assertion-on-non-DOM errors, and a Cypress `queue.insert ... index out of bounds`.

These were run on **Node 25**, while the project's Dockerfile pins **Node 18.20.4**; the mixed errors (some Cypress-internal, some app TypeErrors) are consistent with dependency/runtime version skew rather than a confirmed product defect. Flagged for the team to reproduce on the pinned Node 18 before treating any as a real component bug; the `Cannot read properties of undefined (reading 'get')` one is the only candidate worth a closer look if it reproduces on Node 18.

## F-FE-5: MEDIUM (build): the Dockerfile pins Alpine package versions that no longer resolve

The nginx stage pins exact Alpine package versions:
```dockerfile
RUN apk add --no-cache libcap=2.70-r0 libcap-utils=2.70-r0 ...
```
The base image `nginx:1.27.2-alpine` has since been rebuilt on a newer Alpine patch that ships `libcap 2.78-r0`, so `apk` cannot satisfy `=2.70-r0` and the image build fails:
```
ERROR: unable to select packages:
  libcap-2.78-r0: breaks: world[libcap=2.70-r0]
```
This means `docker build` of the frontend is broken today against the tag the Dockerfile itself references. Pinning a distro package to an exact patch under a floating base-image tag is inherently fragile: the base moves, the pin does not.

**Reproduced:** the FE image fails to build until the pin is relaxed. This local stack works around it with an inline Dockerfile that uses `apk add libcap libcap-utils` (no exact pin).

**Fix:** drop the exact version pins (the packages are only needed for `setcap`), or pin the base image to a digest so the Alpine package set is frozen alongside it. Prefer `npm ci` over `npm install --ignore-scripts` in the same file for lockfile-reproducible builds.

---

## Not findings (checked, clear)
- **No secrets committed:** `.env` holds only URLs and the **public** Keycloak client id `frontend-cli` (public client + PKCE), no credentials.
- Build is reproducible-ish but the Dockerfile uses `npm install --ignore-scripts` rather than `npm ci`; pinning to the lockfile would be marginally better.

## Filing recommendation
F-FE-1 (token storage) and F-FE-2 (missing CSP/headers) are the two worth filing, and they compound, so file them together with the note that CSP is the mitigation for the token-theft exposure. F-FE-5 (broken Dockerfile pin) is a clear, reproducible build bug worth filing on its own. F-FE-3 is a note. F-FE-4 should be reproduced on Node 18 before filing.
