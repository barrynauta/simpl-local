# UI notes: Vocabulary Manager UI and proxy

## Build

- Stack: Vue 3.5.46, Vue Router 4.6.5, Vite 8.3.8, TypeScript 6.0.3, PrimeVue 4.5.5 in unstyled mode (Menu for the row action menus) with Tailwind 4.3.5 classes, keycloak-js 26.2.5 (authorization code flow with PKCE S256, `login-required`, `checkLoginIframe: false`), n3 2.13.12 for the browser Turtle check. All versions pinned exactly in `package.json`; `package-lock.json` is committed.
- PrimeVue 4.5.5 is used, not 5.x: PrimeVue 5 adds a dependency on `@primeui/license-manager`, which is not needed and whose licence terms are unclear.
- Scripts: `npm run lint` (ESLint flat config: js, typescript-eslint, eslint-plugin-vue, prettier), `npm run format:check` (Prettier), `npm test` (Vitest with V8 coverage), `npm run build` (vue-tsc then vite build).
- `Dockerfile`: stage 1 `node:24-alpine@sha256:116b800c3754b440ee0aa3ed2f20f5bf3d99a554bfd9adeea09ea8017f6e85f5` runs `npm ci`, lint, format check, tests and build. Stage 2 `nginxinc/nginx-unprivileged:1.29-alpine@sha256:0c79d56aee561a1d81c63f00eee5fb5fe29279560cdc55e91425133104c7fbe6` plus `apk upgrade --no-cache`, runs as uid 101.
- The Vite build emits no inline scripts or styles (`assetsInlineLimit: 0`, no module preload polyfill); `dist/index.html` has only a module script and a stylesheet link.

## Runtime

| Variable             | Default                 | Use                                                |
| -------------------- | ----------------------- | -------------------------------------------------- |
| `KEYCLOAK_URL`       | `http://localhost:8193` | Keycloak base URL for the browser; also in the CSP |
| `KEYCLOAK_REALM`     | `simpl`                 | realm                                              |
| `KEYCLOAK_CLIENT_ID` | `vocabulary-manager-ui` | public client                                      |

- `/config.json` is answered by nginx from these variables (template `nginx/vocabulary-manager.conf.template`, envsubst at container start).
- Ports inside the container: `8080` UI plus same-origin `/v1/` proxy (host 4433), `8081` API proxy (host 8196), `127.0.0.1:8082` health only.
- Health check for compose: `wget -q -O /dev/null http://127.0.0.1:8082/healthz || exit 1` (also set as `HEALTHCHECK` in the image).
- Backend upstream: `backend:8080`, resolved per request through `resolver 127.0.0.11 valid=10s ipv6=off`, so scaled replicas share the load. The container must be on the same compose network as `backend`.

## Proxy behaviour (nginx/)

- `client_max_body_size 7m` (D122), `large_client_header_buffers 4 32k`, `proxy_read_timeout 120s`, request buffering on, `server_tokens off`, X-Forwarded-For/Proto/Host set, `traceparent` and `X-Correlation-Id` pass through unchanged.
- Backend errors pass through (`proxy_intercept_errors off`). Answers nginx gives itself are problem+json (`nginx/problem-pages.inc`): 400 belgif badRequest, 405 simpl methodNotAllowed (TRACE), 408 simpl requestTimeout, 413 simpl payloadTooLarge, 414 simpl uriTooLong, 494 returned as 431 simpl requestHeaderFieldsTooLarge, 502/503/504 returned as 503 belgif serviceUnavailable with `Retry-After: 5` and detail 'The vocabulary service is not available. Retry later.'.
- 414 uses an internal location `/__vm_problem/414` (a named location gave 500 for a too-long request line). An outside request to that path is passed to the backend, which answers it like any unknown path (D155).
- UI server headers (D123): `Content-Security-Policy: default-src 'self'; connect-src 'self' <KEYCLOAK_URL>; img-src 'self' data:; style-src 'self'; script-src 'self'; frame-src <KEYCLOAK_URL>; frame-ancestors 'none'; base-uri 'self'; form-action 'self' <KEYCLOAK_URL>`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`.
- Verified on a standalone container: CSP present, `/config.json` filled, TRACE 405 problem+json, 8 MB body 413 problem+json, 40 000 character URI 414 problem+json, 40 KB header 431 problem+json, backend unreachable 503 problem+json with Retry-After, runs as uid 101, health check healthy.

## Decisions per view

| View / file                                                                    | Decisions                                                                                                                                                                                                                                                                                                                                                                                                                            |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| App shell `src/App.vue`, `src/main.ts`, `src/auth.ts`                          | D18 (login redirect for users without token, re-login on 401 in `src/api.ts`), D60 menu item 'Vocabularies', D123 (no inline code), E1 roles from `realm_access.roles`                                                                                                                                                                                                                                                               |
| Routes `src/router.ts`                                                         | D31, D74 (/vocabularies, /vocabularies/new, /vocabularies/:name, /vocabularies/:name/versions/:version, /vocabularies/:name/versions/new), D163 version selection by link                                                                                                                                                                                                                                                            |
| Vocabulary list `src/views/VocabularyList.vue`                                 | D5, D23, D34 (20 rows, server paging, empty text, load error), D36/D72 (deprecation icon with accessible name and tooltip), D50 (sort toggle), D60 (status change labels and success text), D72 (filter labels, row actions, failed status change texts), D75 (table, 'Actions for <Name>'), D100/D159 (columns), D114 (one alert), D115 (page controls), D128, D163 (aria-sort and arrow), D18 (admin actions hidden)               |
| Vocabulary details `src/views/VocabularyDetails.vue`                           | D35 (header, versions newest first, pageSize 100 with further pages, panel, 404 and load error texts), D36/D47/D64/D118 (version warning, whole 'Latest ACTIVE version: <n>' link, none for a DEPRECATED vocabulary, never to itself), D60 ('Back to vocabulary list'), D72/D113 (notification and warnings after a creation), D75 (region 'Version <n>', 'Actions for version <n>'), D115 (banner role status), D163 (aria-current) |
| Create form `src/views/CreateVocabulary.vue`                                   | D13, D14, D18 (direct URL shows 'You are not authorized to perform this action.'), D32 (sends 'Initial version.'), D37, D60 labels and buttons, D62, D76, D90/D109 (report region), D101 (all fields checked on Submit, focus first error, reserved space), D141 (aria-required and '*'), D161 (409 and 413 detail in an alert, values kept)                                                                                         |
| New version form `src/views/NewVersion.vue`                                    | D33 (prefilled description, '<Name>.ttl (current content)', current content fetched when no file chosen), D46 fixed line, D7 (no version field), D113 (opens the new version panel), plus the create form rules                                                                                                                                                                                                                      |
| File field `src/components/FileField.vue`, `src/turtleCheck.ts`                | D13, D60 (line, column when known), D62, D76 ('The file is larger than 5 MB.', Submit disabled)                                                                                                                                                                                                                                                                                                                                      |
| Report and warnings `src/components/ValidationReport.vue`, `WarningsPanel.vue` | D14, D90, D109                                                                                                                                                                                                                                                                                                                                                                                                                       |
| Texts `src/messages.ts`, `src/errors.ts`                                       | D24, D34, D35, D36, D44 (503 shown like 500), D60, D72                                                                                                                                                                                                                                                                                                                                                                               |

## Gaps I had to guess

- Field error texts (D55 leaves them free): 'Name is required.', 'Name must be 3 to 64 characters long.', 'Name must start with an upper-case letter and contain only letters and digits.', '<Label> is required.', '<Label> must be at most 1000 characters long.', 'Vocabulary file is required.'.
- Page headings: 'Vocabularies', 'New vocabulary', 'New version of <Name>', '<Name>' on the details page, 'Version history' as h2.
- The status filter has an 'All' option; the name filter reloads 300 ms after typing; filters reload at once on change.
- Version row menu: 'View version details' and 'Download'. The details page also has a header menu 'Actions for <Name>' with 'Download vocabulary' and, for admins, 'Update' and 'Deprecate'/'Activate'.
- 'Page <n> of <m>' shows 'Page 1 of 1' for an empty list.
- Browser syntax error text: 'Line <n>[, column <m>]: Turtle syntax error: <n3 message>'. n3 gives the line, not the column.
- A network failure in the browser is shown as a 503 ('Something went wrong. Retry later or contact support.').

## Tests

- 7 files, 45 tests in `tests/` (list, details, create form, new version form, components and auth, API client with format/errors/validation, Turtle check). Coverage (V8): lines 83.15 %, statements 83.63 %, branches 87.92 %, functions 67.47 %. Report in `coverage/` (html and `coverage-summary.json`).
- Not covered in unit tests: real Keycloak login (`initKeycloak`), real browser download. axe was not run here (no browser in this step).

## Trivy (0.76.2)

| Target                                 | HIGH+CRITICAL before                                                                    | after                            |
| -------------------------------------- | --------------------------------------------------------------------------------------- | -------------------------------- |
| UI image (alpine 3.23.4)               | 57 (1 CRITICAL), all in base image Alpine packages (pcre2, musl, zlib, tiff and others) | 0 after `apk upgrade --no-cache` |
| `trivy fs work/ui` (package-lock.json) | 0                                                                                       | 0                                |
