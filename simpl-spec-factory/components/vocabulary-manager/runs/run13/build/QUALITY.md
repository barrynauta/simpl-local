# Quality targets

How the build meets the Q decisions and NFR-04 to NFR-08. Measured on this machine (Apple silicon, Docker Desktop, 64 GB) on 21 Nov 2026.

## Q1 Response time and throughput (D99, D136)

Seed (D99): 1000 vocabularies with 20 versions each (files of about 10 KB), loaded with `scripts/seed-store.sh LoadSeed 1000 20` through the restore path (new dataset, count check, swap; 357 991 quads), on top of earlier test data (about 1 300 vocabularies in the list). Offered load: 20 reads per second (mix of list, filtered list, vocabulary, version list, search, download) and 1 new version per second (about 10 KB) for 300 s, after warm-up. Two backend replicas. Script: `scripts/loadtest.py --skip-seed --prefix LoadSeed`. Output: `reports/loadtest.txt`.

| Operation | Count | Errors | p50 ms | p95 ms | Target p95 |
|---|---|---|---|---|---|
| list (`GET /vocabularies`, default sort, 1 300 vocabularies) | 991 | 0 | 113 | 168 | 300 ms |
| list filtered (name contains, sort -lastUpdated) | 1021 | 0 | 66 | 96 | 300 ms |
| vocabulary (`GET /vocabularies/{name}`) | 993 | 0 | 30 | 39 | 300 ms |
| version list | 976 | 0 | 37 | 49 | 300 ms |
| search | 1039 | 0 | 31 | 41 | 300 ms |
| download (10 KB) | 980 | 0 | 34 | 43 | 300 ms |
| new version (10 KB) | 300 | 0 | 99 | 123 | 3 s |

Achieved rate: 6 300 of 6 300 offered requests (100 %), no errors. Q1 met per operation.

The first run missed the list target (p95 329 ms): the query joined every vocabulary's latest description before sorting. The list query now sorts and pages in a sub-query and joins the description for the page only (`VocabularyRepository#list`).

5 MB writes (D71, measured apart): create 3.6 s, new version 3.3 s, download 0.13 s (target 30 s).

## Q2 Limits (NFR-04)

| Limit | Value | Answer above it | Own tests (at and above) |
|---|---|---|---|
| vocabularyFile part | 5 242 880 bytes | 413 payloadTooLarge, issue vocabularyFile | `ApiEdgeCasesIntegrationTest#sizeLimits` (5 MB at limit gives content validation, 5 MB + 1 gives 413) |
| multipart request | 6 MB | 413, same body | container limit; live probe with 6 MB file |
| proxy body | 7 MB | 413 problem+json from nginx | live probe with 8 MB |
| JSON body | 1 MB | 413 'The request body is larger than 1 MB.' | `#sizeLimits` |
| multipart parts | 64 | 400 | `#sizeLimits` (70 parts) |
| pageSize | 1 to 100 | 400 | `#queryParameterRules`, `RequestRulesTest` (100 and 101) |
| page, version, integers | 1 to 2 147 483 647 | 400 | `RequestRulesTest#validIntegersAndUpperBounds`, `#invalidIntegers` |
| name | 3 to 64, pattern | 400 | `RequestRulesTest#namesFollowTheDecidedPattern` (64 and 65) |
| description, changelog | 1 to 1000 after trimming | 400 | `RequestRulesTest#textIsTrimmedAndLimited` (1000 and 1001) |
| internal search items | 1 to 100 | 400 name items | `#searchValidation` (101) |
| external search URIs | at most 100 in total | 400 name aliasUris or the array sent | `#searchValidation` (101) |
| aliasUris | at most 20, unique, not the namespace | 400 name aliasUris | `#externalRegistrationRules` |
| validation report | 1000 issues | first 1000, `"truncated": true` | `ContentValidatorTest#reportIsCappedAt1000IssuesInFileOrder` |
| parallel uploads per replica | 4 | 503 'Too many uploads in progress. Retry later.' | Resilience4j bulkhead; seen live when 8 seed clients hit 2 replicas |
| write budget | 30 s, 6 attempts | 503 | `WriteRetryTest` |
| store timeouts | connect 2 s, read 10 s, update 45 s | 503 | `StoreDownIntegrationTest` |

## Q3 Availability and recovery (NFR-06, D89, D156)

- Targets as decided: 99.5 % per month in office hours, RTO 30 minutes, RPO 24 hours.
- Backup: `scripts/backup.sh` reads the whole dataset as N-Quads through the admin user's read-only endpoint inside the stack network, gzips it, writes the quad count and copies both to `BACKUP_TARGET` (a directory outside the host in production). Example schedule: `0 2 * * *` (in the script header).
- Restore: `scripts/restore.sh <file>` stops the backends and the store, loads the backup into a new TDB2 directory with `tdb2.tdbloader`, checks the quad count, and only then swaps it in (old directory kept as `vocabulary.before-<stamp>`); on any failure the old store is started again unchanged. Writes after the backup are lost within the RPO (D89).
- Round trip tested: `scripts/backup-restore-test.sh`: 112 quads, count matched, vocabulary list total equal before and after, 16 s end to end. The same procedure loaded the 357 991-quad load seed in 22 s.
- Compaction: `scripts/compact.sh` (Fuseki online `/$/compact`, delete old), example weekly schedule `0 3 * * 0`, after the backup; tested once, no downtime.

## Q4 Design load and scaling (D82, D102)

- Two backend replicas behind the proxy (`replicas: 2`, no container name or host port per backend); smoke and the load test ran on two replicas.
- Stateless backend; overlapping writes are serialised by conditional store updates: `ConcurrencyIntegrationTest` (4 parallel new versions through the API give versions 2 to 5 once each; 6 parallel new versions through two service instances on one store give 2 to 7 once each).
- Scale up: CPU and memory limits are variables. Scale out of the backend works; the store is one instance.

## Q5 Maintainability

- Backend coverage (JaCoCo, `reports/jacoco/index.html`): 94.3 % lines, 83.4 % branches (targets 80 % and 60 %, enforced by `jacoco:check` in `mvn verify`). 80 tests.
- UI coverage (Vitest V8, `ui/coverage/`): 83 % lines, 88 % branches, 45 tests.
- Trivy: 0 HIGH or CRITICAL in own images and dependencies (`SECURITY-SCAN.md`).
- Reference check: one store query per upload (`VocabularyRepository#referenceState`); every store query has a LIMIT (largest 100 000 rows for the reference and search lookups; list pages 101 at most).
- Formatting: Spotless (Palantir Java Format) check in `mvn verify`; ESLint and Prettier in the UI build.

## Q6 Accessibility

Built for WCAG 2.1 AA (labels, required markers, roles and regions of D75, D90, D109, D115, D163, keyboard-reachable action menus). Not measured with axe in this step (no browser); step 60 runs axe.

## Container limits, health and shutdown (NFR-05, NFR-07)

| Service | CPU | Memory | Health check |
|---|---|---|---|
| backend (each of 2) | 1.5 | 1 GB (JVM at most 70 %) | `/actuator/health/liveness` on management port 8081 (D152) |
| fuseki | 2.0 | 2 GB (heap 1 GB) | authenticated `ASK {}` query |
| keycloak | 2.0 | 1 GB | `/health/ready` on management port 9000 |
| proxy | 0.5 | 128 MB | `/healthz` on 127.0.0.1:8082 only |

Variables: `BACKEND_CPUS`, `BACKEND_MEMORY`, `FUSEKI_CPUS`, `FUSEKI_MEMORY`, `FUSEKI_HEAP`, `KEYCLOAK_CPUS`, `KEYCLOAK_MEMORY`, `PROXY_CPUS`, `PROXY_MEMORY`, `BACKEND_REPLICAS`. Observed under the load test: backend about 250 MB each, Fuseki about 560 MB.

Readiness view: `GET /v1/health` (UP, DEGRADED, DOWN; 503 DOWN when the store does not answer in 2 s). Graceful shutdown: Spring `server.shutdown=graceful` with 30 s per phase and a 40 s stop grace period in compose; in-flight requests finish, new ones are refused; each write is one store transaction, so nothing is left half written.

## Observability (NFR-08, D86, D96)

- Logs: JSON lines (Logstash format) with `traceId`, `spanId`, `correlationId`, `operationId`, `vocabularyName`, `version` and `userId` (token subject only, D80). Checked live on the running stack.
- Metrics: `/actuator/prometheus` on the management port: `http_server_requests_seconds` per `operationId`, status and outcome (count, errors by status, duration histogram), Resilience4j circuit breaker state and calls, JVM metrics. Not published on a host port and never proxied (D98, D120).
- Trace context: W3C `traceparent` read or created by Micrometer Tracing (Brave); `X-Correlation-Id` read or created and returned; both forwarded on calls to the store and to Keycloak.
