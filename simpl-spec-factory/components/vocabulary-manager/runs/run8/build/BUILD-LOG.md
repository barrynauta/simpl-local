# Build log (step 50, run 8, 19 Nov 2026)

## Starting point

An earlier attempt of this step was interrupted. `work/` held the backend source (about 6400 lines, Spring Boot 4.1.2, compiled), the UI (Vue 3, built), the Fuseki image, the Keycloak realms, `compose.yaml`, `stack.sh` and `.env`. There were no tests, no docs, no ops scripts and none of the reports. The stack was running, but the backend was `unhealthy`.

I kept the code. A probe of about 80 requests against the running stack showed it already followed the decisions on nearly every point I checked: the order of checks, problem types, 405/406/415/413, paging links, search, warnings, D43 and D44. So I fixed what was broken and wrote everything that was missing.

## Fix iterations (6)

1. **Backend unhealthy.** The container health check calls `/actuator/health/readiness` on the management port 8081. That request went through the API's security chain and got 401 ("Access without a token refused" every 5 s in the log). Fix: a separate `SecurityFilterChain` (`@Order(1)`) that permits requests arriving on the management port. That port is never published (D67).
2. **Encoded slash gave HTML.** `GET /v1/vocabularies/a%2Fb` was refused by Tomcat with its HTML error page, not the Belgif 400 of D67. Fix (`config/TomcatConfig`): the connector passes encoded slashes through to the Spring Security firewall, which refuses them with the decided body. An own `ErrorReportValve` gives problem+json for errors Tomcat answers itself, for example a control character in the request line.
3. **Part with an empty name was dropped (D70).** Tomcat's `request.getParts()` silently skips a part with `name=""`. So the decided 400 (`in: body, name: ''`) never appeared, and with valid fields the request even gave 201. Fix: `MultipartForm` now reads the body with Tomcat's bundled Commons FileUpload `MultipartStream` and `ParameterParser`, and Spring's multipart support is off. It enforces 5 MB per file part, 6 MB per request, 50 parts and 2 KB of part headers, the 413 before 400 order (D51) included. Recorded under REUSE-09 in DEVIATIONS.md.
4. **`FieldRules.isValidOptionalText(null)` threw an NPE.** My unit test found it; it is now null-safe. No API path reached it with null, so it was not a 500 in practice.
5. **405 without Allow.** Schemathesis found that a method the server does not know (for example `QUERY`) on a known path gave 405 without an `Allow` header (RFC 9110). Fix in `RoutingGuardFilter`: `Allow` is sent whenever the path is known.
6. **Image findings in the backup helper.** `curlimages/curl:8.23.0`, the newest tag, has 32 HIGH findings in krb5, and Alpine has fixed them. Fix: an own helper image `ops/Dockerfile` (Alpine 3.24.4 pinned by digest, `apk upgrade`, curl), built on demand by `ops/lib.sh` and listed by `stack.sh images`. The backup and restore round trip passes with it.

Test mistakes that were mine, not the code's: a sort test hit same-second ties (times are kept to the second, D58), and one assertion expected the literal of an S1 location in quotes.

## Gaps where the specs and decisions left me guessing

- **D65 "the literal as written".** The OpenAPI example writes `statement '... invalid-literal'`, with no quotes. The build follows the example and writes the lexical form without quotes or datatype. If "as written" means the Turtle source text (`"literal"@en`), this needs a decision.
- **D44 key refresh.** "Fetches the keys again when a token names an unknown key id" sets no rate. The build waits 10 s after a successful fetch before fetching again, so a token with an unknown key id inside that window gets 401 at once. This stops a flood of bad tokens from hammering Keycloak. Outside that window it fetches on every such token, and gives 503 when the fetch fails (D44).
- **JSON body size.** No decision. The build refuses a JSON body over 1 MB with 413 (UploadTooLarge body type, name `body`). The largest valid search body is far smaller.
- **405 on an unknown path with an unknown method.** D67 orders 405 before 401 for an unknown method. On a path that is not in the contract it gives 405 without `Allow`, because no methods are declared there.
- **Restore and concurrent writes (Q3).** Q3 says a restore never leaves the store emptier, but not what happens to writes made between the backup and the restore. The restore replaces the store, so those writes are lost. The script refuses a smaller backup unless `--allow-smaller` is given, and QUALITY.md says to restore in a quiet window.
- **Q3 schedule and off-host copy.** The build delivers the backup and restore scripts and a tested round trip. A daily schedule and storage outside the host belong to the deployment, which this build does not have.
- **Multipart limits beyond D15 and D76.** 50 parts and 2 KB of headers per part are my own limits; above them the API gives 400.
- **External aliases** are stored as one space-separated literal. This is safe only because D73 requires absolute URIs, which cannot contain spaces.

## What I verified

- `./stack.sh up` from the existing state; `smoke.sh` passes (401 without a token, 403 for the viewer on create, then create, new version, list, download and search).
- `mvn verify`: 46 tests green, JaCoCo line 94.5 % and branch 83.7 %; the check fails the build below 80/60.
- D43 and D82: 5 parallel versions on one instance, and 6 across two instances on one store, each give consecutive numbers exactly once; a parallel create race gives one 201.
- D44: `./stack.sh store-down-up` gives 503 with Retry-After 5 on reads and writes, and `GET /health` gives 503 DOWN.
- Backup and restore round trip on the live stack: 532 quads before, in the backup and after.
- SHACL: `PhysicalModelShaclTest` validates every named graph the service wrote with Jena SHACL, and the docs fork validated a dump of the live store with pyshacl. Both conform.
- Schemathesis against the published OpenAPI: 1073 requests, no 5xx, no response schema mismatch; only decided contract changes remain.
- UI with Playwright: Keycloak login, list table headers, 'New Vocabulary +', create form labels (no Change log, D32), the browser syntax check disabling Submit, create redirecting to the details page with 'Version 1 of <Name> was created.', and the version table headers.
- Q1 load: reads p95 30 ms, and 152 ms with 1000 more vocabularies stored; writes p95 83 to 172 ms; a 5 MB create takes 21.9 s.

## What does not work or is not done

- No automated UI tests in the repo: Vitest is configured, but `ui/tests/` is empty. The UI checks above were manual Playwright runs and are not committed as tests.
- No axe run (Q6 not measured).
- No TLS in the local stack; no CI pipeline; Spotless is declared in the pom properties but not configured (see DEVIATIONS.md).
- The Keycloak image keeps 1 CRITICAL and 6 HIGH findings that no newer image fixes (SECURITY-SCAN.md).
- Test data from my probes, the seed of 1000 vocabularies (`Seed5515N...`) and the load runs stay in the store. The tests never depend on an empty store (E4).
