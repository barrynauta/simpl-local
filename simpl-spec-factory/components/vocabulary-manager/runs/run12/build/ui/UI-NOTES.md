# UI notes (Vocabulary Manager, run 12)

## Build

- `npm ci && npm run build` runs, in order: ESLint (flat config, `--max-warnings=0`), Prettier check, `vue-tsc` type check, Vitest (jsdom) and the Vite build. Any failure stops the build (D95).
- `docker build -t vm-factory-run12/ui:local work/ui` runs the same `npm run build` in the build stage (node:24-alpine, pinned by digest), then copies `dist/` into `nginxinc/nginx-unprivileged:alpine-slim` (pinned by digest, user 101, port 8080). The runtime stage runs `apk upgrade` (musl and zlib fixes) and `nginx -t` on the rendered config at build time.
- Health: `GET /healthz` returns 200 `ok` (Dockerfile HEALTHCHECK uses busybox wget).
- Runtime config: `GET /config.json` is rendered by the nginx template from `KEYCLOAK_PUBLIC_URL` (default `http://localhost:8193`), `KEYCLOAK_REALM` (`simpl`), `KEYCLOAK_CLIENT_ID` (`vocabulary-manager-ui`). `NGINX_ENVSUBST_FILTER=^KEYCLOAK_` keeps nginx variables untouched.
- Trivy image scan of `vm-factory-run12/ui:local`: 3 HIGH before (musl CVE-2026-107891, musl-utils, zlib CVE-2026-85091), 0 HIGH/CRITICAL after `apk upgrade`. `npm audit --omit=dev`: 0 vulnerabilities. Dev dependencies have 5 moderate findings (not shipped in the image).

## Library versions (exact pins, package-lock.json committed)

vue 3.5.46, vue-router 5.5.1, primevue 4.5.5 and @primevue/core 4.5.5 (unstyled, theme none), keycloak-js 26.2.5, n3 2.13.12, tailwindcss and @tailwindcss/vite 4.3.5, vite 8.3.8, @vitejs/plugin-vue 6.0.11, typescript 6.0.3, vue-tsc 3.3.14, vitest 5.1.1, @vue/test-utils 2.6.0, jsdom 31.0.1, eslint 10.15.0, typescript-eslint 8.73.0, eslint-plugin-vue 10.12.1, prettier 3.10.1.

PrimeVue 5 was not used: it adds a licence manager dependency. TypeScript 7 was not used: typescript-eslint supports TypeScript below 6.1.

## nginx (nginx/default.conf.template)

- SPA history fallback to index.html; `/assets/` cached for a year; index and config not cached.
- `/v1/` proxied to `http://backend:8080` through the Docker resolver (127.0.0.11, valid 10 s) with a variable, so both replicas and restarted backends are found. Authorization, X-Correlation-Id and traceparent pass through unchanged. `client_max_body_size 7m` (D122), read and send timeout 60 s.
- `/actuator...` answers 404 and is never proxied (D67, D120).
- Answers nginx gives itself are application/problem+json (D122, D158): 400 belgif badRequest, 404 belgif resourceNotFound, 405 simpl methodNotAllowed (TRACE), 413 simpl payloadTooLarge, 414, 431/494, 500, and 502/503/504 as 503 belgif serviceUnavailable. Upstream answers are passed through as is.
- Headers on every response: CSP of D123 (`connect-src`, `frame-src`, `form-action` include the Keycloak URL), X-Content-Type-Options nosniff, Referrer-Policy no-referrer, X-Frame-Options DENY; `server_tokens off`.
- No inline scripts or styles: the Vite build emits one module script and one stylesheet (no modulepreload polyfill, no inlined assets). PrimeVue still injects its 'base' `<style>` element even in unstyled mode; `src/primevue.ts` turns that loader off and `style.css` carries its single rule (`.p-hidden-accessible`). A test asserts that no `<style>` element exists after opening a menu.

## Decisions to files

| Decision | Where |
|---|---|
| D18 hidden admin actions, not-authorized message, login redirect | `src/auth.ts`, `src/main.ts` (login-required, PKCE S256, checkLoginIframe false), all views |
| D23, D50, D163 sort (Name, Status, Date created, Last updated; first click ascending, active column reverses; th aria-sort; button in th; arrow aria-hidden) | `src/views/VocabularyList.vue` |
| D34, D115 server paging 20 rows, 'Previous page', 'Next page', 'Page n of m', 'No vocabularies found.', load error | `VocabularyList.vue` |
| D36, D72 deprecated icon (role img, accessible name and title = D36 text), row actions menu | `VocabularyList.vue`, `src/components/ActionsMenu.vue` |
| D60, D72, D114 status change texts, one alert per event | `src/format.ts` (statusChangeError), list and details |
| D100, D159 list columns | `VocabularyList.vue` |
| D35, D163 details page, version table, version panel region 'Version n', aria-current row, pageSize 100 loop | `src/views/VocabularyDetails.vue`, `src/api.ts` (listAllVersions) |
| D36, D47, D64, D118 'This version is deprecated.' and one link 'Latest ACTIVE version: n' | `VocabularyDetails.vue`, `src/format.ts` (recommendedVersion) |
| D115 deprecation banner role status | `VocabularyDetails.vue` |
| D14, D72, D113 success notification and warnings on the details page | `src/flash.ts`, `VocabularyDetails.vue`, `src/components/ValidationWarnings.vue` |
| D90, D109, D125 validation report region, alert line, counts, truncated note | `src/components/ValidationReport.vue` |
| D13, D60, D62, D76 browser Turtle check, 'Syntax check passed.', 5 MB refusal, Submit disabled only on upload errors | `src/components/VocabularyFileField.vue`, `src/turtle.ts` |
| D32, D37, D101, D141 create form, field rules, all fields checked on Submit, focus first error, reserved error space, aria-required and '*' outside the label | `src/views/CreateVocabulary.vue` |
| D33, D46, D161 new version form, pre-filled description, current content attached and sent, fixed line, 409 detail | `src/views/NewVersion.vue`, `src/submitError.ts` |
| D74 routes | `src/router.ts` |
| D123, D167 CSP, PrimeVue unstyled + Tailwind | `nginx/default.conf.template`, `src/primevue.ts`, `src/style.css` |

## Tests (32, all green)

- `tests/format.test.ts` (9): date format, error and warning counts, 5 MB limit, status labels, name and text rules, latest ACTIVE recommendation, status change failure texts, submit failure mapping, Content-Disposition filename.
- `tests/turtle.test.ts` (2): valid Turtle, error line.
- `tests/list.test.ts` (5): columns, deprecation icon, admin button, paging text; sort toggling; viewer and empty state; one load-error alert; status filter.
- `tests/details.test.ts` (4): version table, deprecation warnings and links; version panel and aria-current, banner, no admin actions for a viewer; creation notification and warnings; 404.
- `tests/create.test.ts` (7): viewer message; fields; all-field check on Submit with focus; syntax error disables Submit then passes; 5 MB refusal; 400 report; 409 detail then 201 navigation.
- `tests/newversion.test.ts` (4): pre-fill and fixed line; change log required and current content sent, navigation to the new version; 500 text; viewer message.
- `tests/primevue.test.ts` (1): no `<style>` element injected; menu item labels.

## Gaps and guesses (findings)

1. Field error texts are free (D37 gives rules, not texts). Chosen: 'Name is required.', 'Name must be 3 to 64 characters.', 'Name must start with an upper-case letter and contain only letters and digits.', '<Field> is required.', '<Field> must be at most 1000 characters.', 'Vocabulary file is required.'.
2. Text of the browser syntax error is not decided beyond 'line and column'. Chosen: 'Syntax error at line N[, column M]: <parser message>'. The n3 parser gives no column, so the column never shows (D60 allows this). It is shown under the file field with role alert.
3. Headings are not decided: 'Vocabularies' (list), '<Name>' (details), 'New vocabulary', 'New version of <Name>', 'Version history', 'Version <n>' (panel), 'Validation report'.
4. The version row actions menu holds one item, 'Download'; the panel has a 'Download' button. The list status filter offers 'All', 'Active', 'Deprecated' as a native select.
5. The details page also offers 'Update' and 'Deprecate'/'Activate' buttons to admins (same texts and messages as the list). D18 hides them for others.
6. The vocabulary status in the details header is shown as 'Status: Active' or 'Status: Deprecated'.
7. A version route for a version that is not in the list shows the alert "Version <n> of vocabulary '<Name>' was not found." (no decision).
8. A download failure shows 'The vocabulary content could not be downloaded.' plus the problem detail (no decision).
9. Load errors of the new version form: 404 'Vocabulary not found.', others the D60 text plus detail (D72 covers list and history only).
10. With 0 vocabularies the page text reads 'Page 1 of 1' (D115 does not say what m is when totalPages is 0).
11. The 'Name contains' filter is sent 300 ms after typing stops; other filters at once. A name filter with characters outside D19's pattern gets the API's 400 and shows the list load error.
12. Keycloak logout button 'Log out' and the user name are in the header (no decision).

## Not done

- No axe run in this step (no browser here); markup was written for WCAG 2.1 AA (labelled landmarks, table captions, th scope, labelled regions, no aria-controls on closed menus). Step 60 runs axe.
- The image was built but not started; nginx config was only checked with `nginx -t` at build time. The error_page problem+json answers are untested against a live request.
