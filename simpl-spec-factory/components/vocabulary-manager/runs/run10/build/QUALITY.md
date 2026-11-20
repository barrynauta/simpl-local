# Quality targets and how the build meets them

## Q1 Response time and throughput

Measured with `scripts/load-test.py` on this machine (Apple silicon, Docker Desktop, the full stack with 2 backend
replicas and the default container limits). Result file: `security/load-test-result.txt`.

| Measure | Target | Measured | Met |
|---|---|---|---|
| Read p95 (list, details, versions, search, download about 10 KB), mixed with writes | at most 300 ms | 158 ms | yes, at the measured rate |
| Write p95 (new version, files 100 KB to 1 MB) | at most 3 s | 526 ms | yes |
| New version of 1 MB, single | at most 3 s | 398 ms (max of 5) | yes |
| Create and new version at the 5 MB limit | at most 30 s (D71) | 1.0 s and 0.9 s | yes |
| Throughput: 20 reads/s and 1 write/s offered for 5 minutes, at least 95 % achieved, no errors (D99) | | offered 16.0 reads/s and 0.80 writes/s for 90 s; achieved 15.6 and 0.78; 0 errors | not fully shown |

Not measured as decided: the seed was 300 vocabularies with 3 versions (900 versions), not 1000 x 20 (D99); the run
lasted 90 s, not 5 minutes; and the thread-per-request driver reached 16 offered reads per second, not 20. The
results at that load are well inside the targets, but Q1 at the full design load is not proven by this build.

## Q2 Limits (NFR-04)

| Limit | Value | Enforced in | Error | Own test at and above the limit |
|---|---|---|---|---|
| vocabularyFile part | 5 242 880 bytes (D15, D76) | `MultipartForm` | 413 `payloadTooLarge`, issue `vocabularyFile` | `AccessAndOrderApiTest.fileAbove5MbGives413...` (exactly 5 MB accepted for validation, 5 MB + 1 refused) |
| Whole multipart request | 6 MB (D76) | Tomcat `max-request-size`, nginx `client_max_body_size 8m` | 413 | same test (7 MB request) |
| JSON body | 1 MB (D110) | `JsonBody` | 413 | `RequestRulesTest` (parser), code |
| Multipart parts | 64 (D110) | `MultipartForm` (Tomcat cap 200) | 400 | code |
| pageSize | 1 to 100 (D22) | `Paging` | 400 | `ListApiTest` (100 ok, 101 refused) |
| page, versions, search versions | 1 to 2147483647 (D91, D112) | `FieldRules.positiveInt`, `VocabularyController` | 400 | `AccessAndOrderApiTest.invalidPathValuesGive400`, `RequestRulesTest.integers` |
| Name | 3 to 64, pattern (D37) | `FieldRules` | 400 | `RequestRulesTest.names` |
| Description, changelog | 1 to 1000 after trimming (D37, D56) | `FieldRules.text` | 400 | `RequestRulesTest.textFields...` |
| Internal search items | 1 to 100 (D38) | `VocabularyController.searchReferences` | 400 `items` | `SearchAndExternalApiTest.internalSearchValidation` |
| External search URIs | 100 in total, arrays at least 1 (D38) | `ExternalVocabularyController.searchUris` | 400 | `SearchAndExternalApiTest.externalSearchValidation` |
| aliasUris | at most 20, absolute, no duplicates (D56, D73) | `ExternalVocabularyController.aliases` | 400 `aliasUris` | `SearchAndExternalApiTest.externalRegistrationValidation` |
| Parallel uploads | 4 per instance (D92) | `UploadLimiter` | 503, Retry-After 5 | `VocabularyServiceTest.uploadLimiterRefuses...` |
| Write retries | 5 (D82) | `VocabularyService` | 503 | `VocabularyServiceTest` |
| Lock wait | not applicable: no lock (D82 replaces D43's lock) | | | |
| Store timeouts | connect 2 s, read 10 s, update 45 s (D44, D71) | `SparqlStore` | 503 | `SparqlStoreTest` |
| Validation issues per response | 1000 (build choice) | `SemanticValidator`, `ContentValidation` | rest cut | code |
| Store query rows | LIMIT on every query, at most 101 rows except search (100 references) and the reference check (one query, D85, Q5) | repositories | | code review |

## Q3 Availability and recovery (NFR-06)

- Targets: availability 99.5 % per month in office hours, RTO 30 minutes, RPO 24 hours.
- Backup: `scripts/backup.sh` (daily, example cron line and Kubernetes CronJob in `docs/backup-schedule.md`), whole
  dataset as N-Quads with SHA-256 and count, configurable target `BACKUP_TARGET` outside the host.
- Restore: `scripts/restore.sh` loads into the staging dataset, checks the count, then replaces the live dataset in
  one transaction; the backends are stopped meanwhile (D89).
- Round trip tested on this build: `scripts/backup-restore-test.sh` passed (141 quads restored; the vocabulary made
  before the backup came back, the one made after it was gone).
- Availability: two backend replicas behind the proxy; health checks restart failed containers
  (`restart: unless-stopped`). The store is one Fuseki instance (single point of failure accepted for this build).

## Q4 Design load and scaling

Two backend instances run on one store (D102), proven by `ConcurrencyApiTest` (two instances, parallel versions
numbered without gaps or duplicates) and by `smoke.sh` on the 2-replica stack. The design-load seed of D99 was not
run (see Q1).

## Q5 Maintainability

| Target | Result |
|---|---|
| Line coverage at least 80 % | 96.1 % (2093 of 2177 lines), JaCoCo, `reports/jacoco/index.html` |
| Branch coverage at least 60 % | 86.9 % (692 of 796 branches); `mvn verify` fails below 80/60 |
| No HIGH or CRITICAL in own images and dependencies | 0 in the backend, Fuseki, proxy and tools images and in `pom.xml` and `package-lock.json` (SECURITY-SCAN.md) |
| Reference check: at most one store query per upload | `VocabularyRepository#referenceData` (one UNION query) |
| Formatter and lint (D95) | Spotless (Palantir) check in `mvn verify`; ESLint, Prettier check, vue-tsc and Vitest in `npm run build` (run in the UI image build) |

## Q6 Accessibility

`scripts/ui-check.py` runs axe-core 4 (WCAG 2.0 and 2.1, A and AA) on the list, the details page and both forms on
the running stack: no violations. All actions are buttons, links or menu items reachable by keyboard; the actions
menu supports Escape and arrow keys; focus is visible.

## Health, shutdown, resources (NFR-05, NFR-07)

- `GET /v1/health` (UP, DEGRADED, DOWN) and the actuator liveness and readiness probes on the management port.
  Compose health checks: backend `/v1/health`, Fuseki `/$/ping`, Keycloak `/health/ready`, proxy `/config.json`.
- Graceful shutdown: Spring Boot `server.shutdown=graceful`, 30 s per phase, compose `stop_grace_period: 35s`.
  A write is one store transaction, so an interrupted request leaves nothing half written.
- Container limits (variables with defaults, D98): backend 1.5 CPU and 1 GiB (heap 70 %), Fuseki 2 CPU and
  1.5 GiB (heap 768 MiB), Keycloak 2 CPU and 1 GiB, proxy 0.5 CPU and 128 MiB.
- Bounded resources: Tomcat 100 threads, 4 uploads, bounded queries, key cache of one realm key set.

## Observability (NFR-08, D86, D96)

JSON log lines with `traceId`, `spanId`, `correlationId`, `operationId`, `vocabularyName` and `version`; the token
subject, never email or token (D80). Prometheus metrics per operation (`http_server_requests_seconds` with `uri`,
`method`, `status`, `outcome`), circuit breaker state and calls, JVM. `./stack.sh metrics` reads them. Trace
context and `X-Correlation-Id` are forwarded to the store and to Keycloak.
