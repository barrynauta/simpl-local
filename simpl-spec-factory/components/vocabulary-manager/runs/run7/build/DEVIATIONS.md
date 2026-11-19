# Deviations

Rules of `input/rules.json` that this build does not follow, and decisions not implemented exactly as written. Rules that do not apply to this component (Angular rules CODE-03 and CODE-04, Kafka and CloudEvents rules API-62 to API-74, MSV-04, BLG-async-*, long-running tasks BLG-lng-task, embedding BLG-prf-embed) are not listed.

## Rules

| Rule | What is not followed | Reason |
|---|---|---|
| API-46 (via D48) | 400, 404, 500 and 503 use `urn:problem-type:belgif:...` types, not the project namespace | Decided in D48; the contract and D44 fix these types. |
| D79 contract changes (API contract rules, BLG-oas-*) | The service is stricter or wider than `vocabulary_openapi-tier1.yaml` on the points listed in D79 (401 and 403 everywhere, 405, 406, pageSize 100, field limits, unknown fields, createdBy, validationWarnings, latestVersionHref, relaxed name filter, 503 with Retry-After) | Decided (D39, D79). The OpenAPI file in `input/` was not edited. The creation response also carries `latestVersionHref` (equal to `href`), an extra property the schema allows. |
| OWT-A04, OWASP-03, ARCH-17 | No TLS inside the local stack (Keycloak, API and UI on plain HTTP on localhost) and the Fuseki volume is not encrypted at rest | Local evaluation stack of the build profile; in production TLS ends at the Tier 1 gateway and the platform encrypts volumes. |
| ARCH-07 | The backend runs as one replica | Accepted risk D81: the per-vocabulary lock of D43 is in process. |
| OPS-06, OPS-33, OPS-35, OPS-37 | No CI pipeline and no SAST quality gate | The factory has no pipeline for this component. Formatting is checked in `mvn verify` (Spotless) and by `npm run format:check`; Trivy was run by hand (SECURITY-SCAN.md); no SAST tool was run. |
| OWT-A09, OWASP-10 | Security events are logged (401, 403 at WARN with the token subject) but nothing raises an alert | No alerting stack in the build; the logs are structured (ECS JSON) for a SIEM to consume. |
| OWT-A07 (repeated failures limited) | The API has no call-rate limit | D46: the Tier 1 gateway applies it in production. Keycloak brute-force protection limits failed logins. |
| OWASP-07, OWT-A03 (scan blocking on high findings) | The Keycloak image has 1 CRITICAL and 6 HIGH findings without a fixed version in the newest Keycloak build | Third-party product of the reuse catalogue; see SECURITY-SCAN.md. |
| E6 (UI stack) and UX-01 | The UI is plain Vue 3 with semantic HTML and one CSS file, not participant-fe-components (PrimeVue unstyled, TailwindCSS) and not the Figma design | The library and the Figma file are not available to the factory (E6, specs README). |
| CODE-39 | Not every public method documents its runtime exceptions with `@throws` | Exceptions are unchecked `ApiException` problems; the main entry points document them, small helpers do not. |
| BLG-int-conlang | No `Content-Language` header | The API returns English only and no internationalised content. |
| API-01, DATA-26 to DATA-29 | No CDM is delivered with the build | The CDM is an input in review (D2), not produced by the code writer; owner questions are in the decisions. |
| NFR-06 (backup kept outside the host, daily) | The backup and restore scripts and the round-trip test exist; scheduling and the off-host copy are not automated | Operations task outside the build stack. |
| REUSE-05 (Simpl-Open authenticator plugin for Keycloak) | Plain Keycloak 26.8.2 | The plugin is not available to the factory and not needed for the flows used. |

## Decisions implemented with a difference

| Decision | Difference | Reason |
|---|---|---|
| D46 | Identical version is detected by equal SHA-256 hashes of the bytes (stored with each version), not by comparing the stored bytes | Avoids reading up to 5 MB from the store on every new version. A SHA-256 collision is not a practical risk. |
| D67 | A known path with a method it does not declare (for example `PUT /vocabularies`) also gives 405 before authentication, not only an unknown method | D67 and D77 do not say where an undeclared method on a known path sits relative to 401; answering 405 first keeps "undeclared method gets 405" true for every client, as the finish line asks. |
| D38, D73 | An invalid URI inside `namespaceUris` or `aliasUris` of an external search gives an issue named after the array (`namespaceUris`), without an index | D73 says "name: the field"; D68 gives indexes only for the internal search. |
| D13 | The browser syntax check resolves relative IRIs against a fixed content base, not the configured one | Only syntax is checked in the browser; the base does not change the outcome. |
