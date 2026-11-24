# Build log (step 50, run 14, 23 Nov 2026)

## Result

* Stack: proxy (nginx with UI), 2 backends (Spring Boot 4.1.3, Java 21), Fuseki 6.3.0, Keycloak 26.8.2.
  `bash stack.sh up` from a clean state (no volume, no `.env`) took 53 s and returned with all containers healthy.
* smoke.sh passes (also through two backend replicas). Backend `mvn verify`: 39 tests green, line coverage 94.5 %,
  branch 86.3 %, Spotless check passed. UI: ESLint and Prettier clean, 14 Vitest component tests green.
* Trivy: own images and dependency files 0 HIGH, 0 CRITICAL after one fix; Keycloak 6 HIGH listed with reasons.
* Own fuzz run with the step 70 settings (Schemathesis 4.34, published OpenAPI, 30 examples, seed 1): 1 438 of
  1 438 cases passed, no 5xx (an earlier run: 1 241 of 1 241).
* Browser check (Playwright, Chromium): login, list, create with browser syntax check, details with warnings, new
  version, deprecated-version link, status change with confirmation, external screens all work; axe 0 violations.
* Store-down backend (`stack.sh store-down-up`): GET and POST /vocabularies give 503 with Retry-After 5 and the D44
  body; GET /health gives 503 DOWN.
* Backup and restore round trip passed twice. Load test: Q1 met on a store of about 1 250 vocabularies.

## Where the specs and decisions left me guessing (new gaps)

1. D94 names Jena `ParameterizedSparqlString`, but it throws an exception for a literal value that contains a word
   equal to a variable name (a description "see ?now" would give 500). I built a one-pass binder on Jena's
   `NodeFmtLib` instead (REUSE.md, DEVIATIONS.md). Owner question: accept "values escaped by Jena's term formatter"
   as meeting D94.
2. D133 wants query parameters bound by the framework, D168 wants `+` to be a plus sign. Tomcat decodes `+` in
   query strings as a space, so the two cannot both hold; I parse the raw query string myself.
3. D65 and D181: S1 and S2 locations use N-Triples with angle brackets; for S3, S4 and S5 the text
   `resource '<property IRI>'` could mean literal angle brackets or a placeholder. I chose no brackets (as for the
   prefix locations), e.g. `resource 'https://example.org/sem#both'`.
4. D178 wants a TRACE answered by the proxy to carry an Allow header with the path's methods. nginx refuses TRACE
   itself, so the proxy needs its own copy of the path table (a `map` in nginx.conf); ContractTest keeps it equal
   to the operation table (D206).
5. D122/D178 and D155 together: nginx cannot send a 414 through a named location (the URI is still empty), and a
   normal internal location would answer a direct request itself. The 414 location therefore passes direct
   requests to the backend (401/404 like any unknown path) and only answers 414 for the error redirect.
6. D67 "404 with a token" for unknown paths: I return 404 for any valid token, also without a role.
7. D63 lists Accept rules for JSON operations and downloads; DELETE answers 204 without body. I apply the JSON rules
   to DELETE (so `Accept: text/html` gives 406).
8. D92 says "create or new-version requests"; external registration (also a file upload) does not take an upload
   slot in this build.
9. D51 and D169: for POST .../versions the file-part 413 comes before the 400 for an invalid name in the path,
   because the parts must be read to know the file size ("size comes before shape").
10. When a request has several invalid query parameters, only the first failing one is reported (one issue), while
    several unknown parameters give one issue each (D21). Not decided either way.
11. The Fuseki server jar's default command enables the UI and admin modules, which answer `/$/datasets` and
    `/$/backup` without any login even when the datasets have users. I run the "Fuseki main" command
    (`FusekiMainCmd`) and set `fuseki:allowedUsers` on the server, so `/$/ping` and `/$/compact` also need a user.
    Without the server-level setting, `--compact` makes compaction anonymous. Worth an owner note for D83 and D193.
12. PrimeVue unstyled still injects a base `<style>` element; the strict CSP of D123 blocks it (DEVIATIONS.md).
13. After the backends restart (restore), nginx needs up to 10 s to re-resolve them; the API answers 503 for a few
    seconds. restore.sh waits for the API before it returns.

## Problems and how they were solved

| Problem | Fix |
|---|---|
| Fuseki container could not write its password file (tmpfs owned by root) | tmpfs with uid/gid 10001 |
| Backend did not start: `application.yml` needs SnakeYAML at runtime | dependency moved from test to runtime scope |
| No `Propagator` bean: Boot 4's tracing auto-configuration module does not bring the Brave bridge | added `micrometer-tracing-bridge-brave` |
| Proxy answered 500 HTML for a too long URL | dedicated 414 location (gap 5) |
| Proxy image HIGH CVE-2026-86144 (libxml2) | removed unused nginx modules and libxml2 |
| Load test showed 19 errors | the script resent identical content and description; the API correctly answered 409 (D46). Script fixed, 0 errors |
| CycloneDX execution in the pom did not write `target/bom.json` during `mvn verify` (the Spring Boot parent configures its own SBOM execution) | ran the plugin goal explicitly; SBOM copied to `sbom-backend.cdx.json` |

Fix iterations: about 10 (2 compile, 3 stack start, 2 proxy, 1 test code, 1 image scan, 1 load script).

## What does not work or was not done

* Q1 was measured with 300 x 2 seeded vocabularies (store about 1 250), not with the full D99 seed of 1 000 x 20,
  and for 2 minutes instead of 5.
* Availability (99.5 %) is not measured; it needs production monitoring (D191).
* CSP console errors from PrimeVue's base style (gap 12).
* The trace context forwarding to the store and Keycloak (D86) is implemented through Micrometer's propagator but
  not checked by an own test.
* No CI pipeline (D95); no SAST tool run.
