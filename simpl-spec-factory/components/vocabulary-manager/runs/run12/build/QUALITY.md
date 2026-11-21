# Quality targets

## Q1 Response time and throughput

Measured on the build's own stack (two backends, one Fuseki, laptop with OrbStack/Docker) with a
reduced seed: about 250 vocabularies in the store, of which 50 with 3 versions of about 10 KB.
Closed loop, 20 concurrent clients, 400 requests per operation (this is far above the offered 20
reads per second of D99).

| Operation | p50 | p95 | Errors |
|---|---|---|---|
| GET /vocabularies (pageSize 20, sort -lastUpdated) | 98 ms | 305 ms | 0 |
| GET /vocabularies/{name} | 18 ms | 52 ms | 0 |
| GET /vocabularies/{name}/versions | 71 ms | 93 ms | 0 |
| POST /vocabularies/searches (10 items) | 22 ms | 75 ms | 0 |
| GET /content/{name} (10 KB) | 27 ms | 77 ms | 0 |
| POST .../versions with a 1 MB file (10 sequential) | 904 ms | max 1052 ms | 0 |
| POST .../versions with a 5 MB file | 2265 ms | n/a | 0 |
| GET /content/{name} with 5 MB | 124 ms | n/a | 0 |

Write targets (3 s for 1 MB, 30 s for 5 MB) are met. Read p95 is within 300 ms for every
operation except the list, which is at 305 ms under 20 concurrent closed-loop clients. Not
measured: the full D99 seed (1000 vocabularies by 20 versions) and the 5-minute offered-rate run;
step 70 runs them. The list sorts in the store with ORDER BY and LIMIT, so its cost grows with the
number of vocabularies.

## Q2 Limits (NFR-04)

| Limit | Enforced by | At the limit | Above | Test |
|---|---|---|---|---|
| vocabularyFile 5 MB (D15, D76) | `MultipartForm` | 201 (5 MB write measured) | 413 | `VocabularyApiTest.requestShapeErrors` |
| multipart request 6 MB (D76) | `NegotiationFilter` (Content-Length), Tomcat max-request-size | accepted | 413 | `requestShapeErrors` |
| proxy body 7 MB (D122) | nginx `client_max_body_size` | passed to the service | 413 problem+json | config |
| JSON body 1 MB (D110, D150) | `NegotiationFilter`, `JsonBody` | accepted | 413 | `requestShapeErrors`, `negotiationOrder` |
| pageSize 1 to 100 (D22) | `QueryParams.positiveInt` | 200 | 400 | `requestShapeErrors`, `RequestRulesTest` |
| integers 1 to 2147483647 (D91) | `Validation.positiveInt` | 200 | 400 | `RequestRulesTest.integers` |
| name 3 to 64 (D37) | `Validation.validName` | 201 | 400 | `RequestRulesTest.names`, `requestShapeErrors` |
| description, changelog 1 to 1000 after trim (D37) | `Validation.validText` | 201 | 400 | `RequestRulesTest.texts`, `ExternalApiTest.registrationValidation` |
| search items 1 to 100, URIs at most 100 (D38) | `SearchController`, `ExternalController` | 200 | 400 | both `searchValidation` tests |
| aliasUris at most 20 (D56) | `ExternalController.aliasIssues` | 201 | 400 | `registrationValidation` |
| multipart parts 64 (D110) | Tomcat `max-part-count`, `MultipartForm` | accepted | 400 | `requestShapeErrors` |
| 4 parallel uploads per instance (D92) | `UploadLimiter` | 4 accepted | 503 | `DomainUnitTest.uploadLimiter` |
| validation report 1000 issues (D125) | `ContentValidator` | 1000 | truncated | `ReferencesTest.reportIsTruncatedAt1000` |
| write budget 30 s, lock or retry (D71) | `WriteRetry` | | 503 | `DomainUnitTest.retriesEndIn503` |
| store timeouts 2/10/45 s (D44, D71) | `StoreClient` | | 503 | `StoreDownTest` |

## Q3 Availability and recovery

Backup, restore and compaction scripts in `ops/`, documented in `docs/operations.md`. The round
trip was run on this build (backup 565 quads, restore count 565 = 565, data back to the backup
state, smoke test green after restore). Restore loads into a new dataset, checks the count, and
only then swaps (the old dataset is kept). The backends are stopped during the restore (D89).
Health: liveness on the management port, readiness GET /v1/health. Graceful shutdown:
`server.shutdown=graceful`, 30 s per phase, compose `stop_grace_period: 35s`. Availability in
office hours is not measured (no long run).

## Q4 Design load and scaling

20 users, 1000 vocabularies, 10 consuming services. Two backend replicas behind the gateway on one
store (D102); writes are conditional updates, so more replicas need no change (D82). Scale-up
through the container limits: backend 2 CPU and 1024 MB each, Fuseki 2 CPU and 1536 MB, Keycloak 2
CPU and 1024 MB, gateway and UI 1 CPU and 128 MB (variables in `docker-compose.yml`, D98). The
backend heap (70 % of 1024 MB) holds 4 uploads of 6 MB with their parsed graphs.

## Q5 Maintainability

`mvn verify`: Spotless check (Palantir format), 59 tests green, JaCoCo line coverage 92.8 %
(2117 of 2282) and branch coverage 82.4 % (916 of 1112); the build fails below 80 % or 60 %
(`pom.xml`). Report: `docs/jacoco-report/index.html`. UI: ESLint, Prettier, vue-tsc and 32 Vitest
tests in `npm run build`. Trivy: 0 HIGH or CRITICAL in own images and dependencies (SECURITY-SCAN.md).
The reference check uses one store query per upload (`ReferenceStore.lookup`); every query has a
LIMIT (D85). Not done: mutation score and Sonar analysis (DEVIATIONS.md).

## Q6 Accessibility

axe-core 4 (wcag2a, wcag2aa, wcag21a, wcag21aa) on the list, details, create form and new version
form of the running stack: 0 violations on each page (run with Playwright, CSP bypassed as D123
allows). Keyboard: actions are buttons and links; sort buttons in column headers; menus are
PrimeVue menus with keyboard support (not separately tested).

## Observability (NFR-08)

JSON log lines with traceId, spanId, correlationId, operationId, vocabularyName, version, userId
(token subject); X-Correlation-Id returned; traceparent and X-Correlation-Id forwarded to Fuseki
and Keycloak; Prometheus metrics per operation (count, status, duration) and circuit breaker state
on the management port.
