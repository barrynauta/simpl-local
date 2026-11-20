# Quality targets

Measured on this build's own stack (MacBook, 18 cores, 64 GB; Docker with the default container
limits of the compose file; two backend replicas behind nginx). Raw output: `docs/load-run.txt`.

## Q1 Response time and throughput

Method: `tools/load.py`. Seed: **200 vocabularies with 3 versions each (600 versions), files of about
10 KB**, not the D99 seed of 1000 x 20; seeding the full D99 seed (20 000 uploads) did not fit the time
of this run. Then 120 s of offered load: 20 reads per second (mix of list, filtered list, details,
versions, download, search of 10 references) and 1 write per second (new versions with files of 10 KB,
100 KB or 1 MB).

| Operation | n | p50 | p95 | Target | Met |
|---|---|---|---|---|---|
| list (paged) | 406 | 58 ms | 117 ms | 300 ms | yes |
| list with name filter and sort | 376 | 58 ms | 145 ms | 300 ms | yes |
| vocabulary details | 412 | 41 ms | 95 ms | 300 ms | yes |
| version list | 427 | 45 ms | 105 ms | 300 ms | yes |
| download (about 10 KB) | 383 | 49 ms | 116 ms | 300 ms | yes |
| search (10 references) | 396 | 43 ms | 112 ms | 300 ms | yes |
| new version (10 KB to 1 MB) | 120 | 127 ms | 626 ms | 3 s | yes |
| new version at the 5 MB limit (5 200 000 bytes) | 1 | 6.3 s | | 30 s (D71) | yes |
| download of the 5 MB version | 1 | 133 ms | | | |

Throughput: 2400 reads and 120 writes offered in 120 s, all completed, 0 errors: the offered rate
was achieved. Not shown: the p95 at the full D99 seed (owner of step 70 measures it).

## Q2 Limits (NFR-04)

| Limit | Enforced by | Error | Tested at and above |
|---|---|---|---|
| file part 5 MB (5 242 880 bytes) | Tomcat multipart `max-file-size` | 413 payloadTooLarge | 5 200 000 bytes accepted (load run); 5 242 881 refused (`RequestRulesIT.formFieldRules`) |
| multipart request 6 MB | `NegotiationFilter` (Content-Length), Tomcat `max-request-size` | 413 | 6.2 MB refused (probe) |
| proxy body 7 MB | nginx `client_max_body_size` | 413 problem+json | 8 MB refused (probe) |
| JSON body 1 MB | `NegotiationFilter`, `JsonBody` | 413 | `RequestRulesIT.negotiationOrder` |
| multipart parts 64 | Tomcat `max-part-count` | 400 | unit test of the mapping only |
| pageSize 1 to 100, page and versions 1 to 2147483647 | `Names.positiveInt` | 400 | `HelpersTest.integers`, `RequestRulesIT.queryParameterRules` |
| name 3 to 64, pattern; description and changelog 1 to 1000 after trim | `RequestChecks` | 400 | `RequestRulesIT.formFieldRules`, `HelpersTest.names` |
| search: 1 to 100 items; external search at most 100 URIs | `SearchRequests` | 400 | `SearchAndExternalIT` |
| aliasUris at most 20, unique | `ExternalVocabularyController.aliases` | 400 | `SearchAndExternalIT.externalRegistrationRules` (unique); 20 limit by code review |
| validation report at most 1000 issues | `ContentValidator` | `truncated: true` | `ContentRulesTest.reportIsTruncatedAtTheLimit` (limit set to 3) |
| 4 parallel uploads per instance | `UploadLimiter` | 503 | `UploadLimiterTest` |
| write budget 30 s, at most 5 conditional attempts, store update 45 s, store read 10 s, connect 2 s | `VocabularyService`, `SparqlClient` | 503 | `StoreDownIT` (connect failure); timeouts by configuration |

## Q3 Availability and recovery (NFR-06)

- Target 99.5 % per month in office hours; RTO 30 minutes; RPO 24 hours. Not measurable in this
  build (no production run); the design supports it: two backend replicas, health checks with
  restart (`restart: unless-stopped`), store on a named volume.
- Daily backup: `ops/backup.sh` with the example schedule `ops/backup.cron` and a copy target
  outside the host (`BACKUP_COPY_TARGET`). Restore: `ops/restore.sh` loads into a new dataset, checks
  the count, then swaps (never leaves the store emptier). Round trip run on this build with
  `ops/backup-restore-test.sh`: **PASS** (68 quads before and after, no differing line). The restore
  time was not timed separately; it stops and starts the whole stack once.

## Q4 Design load and scaling

- Two backend replicas on one store (D102); scale with `BACKEND_REPLICAS`. Concurrency rules hold
  across instances through conditional store updates (D82): `tools/parallel-check.sh` sent 4 parallel
  new versions through the proxy; both replicas wrote (3 and 1), four 201, versions 2 to 5 once each,
  only version 5 ACTIVE. The own suite repeats this on one instance
  (`VocabularyLifecycleIT.parallelNewVersions...`).
- Container limits (variables in `docker-compose.yml`, D98): backend 2 CPU / 1 GB each (JVM uses
  70 % of it), Fuseki 2 CPU / 2 GB (heap 1 GB), Keycloak 2 CPU / 1.5 GB, UI 1 CPU / 128 MB.
- The heap is sized for 4 uploads at 6 MB with their parsed form (D92); measured only with one 5 MB
  write after the load run (no restart, no error). Four parallel 5 MB uploads were not measured.

## Q5 Maintainability

- Own tests: 49 backend tests (unit and integration), **line coverage 93.3 %, branch coverage
  82.3 %** (JaCoCo, `docs/coverage-report/`); `mvn verify` fails below 80 % / 60 %. UI: 18 Vitest
  component and unit tests, one per view at least.
- Formatter and linters on the finish line: Spotless (Palantir) in `mvn verify`; ESLint, Prettier
  check and vue-tsc in `npm run build` (D95).
- Trivy: 0 HIGH or CRITICAL in own images and dependencies; Keycloak image findings listed in
  SECURITY-SCAN.md.
- Reference check: one store query per upload (`VocabularyRepository.references`); every list query
  has a LIMIT (D85).

## Q6 Accessibility

Built for WCAG 2.1 AA: labelled form fields with `aria-required` and visible `*`, error texts tied
with `aria-describedby`, `aria-invalid`, regions and roles of D75 and D90, sortable headers as buttons
with `aria-sort`, menus reachable by keyboard (Escape closes), visible focus outline, colour contrast
of at least 4.5:1 for text. **Not measured with axe in this build** (step 60 runs axe).

## Health, graceful shutdown, observability (NFR-07, NFR-08)

- GET /v1/health (UP, DEGRADED, DOWN) for the stack's health checks; liveness and readiness on the
  management port.
- Graceful shutdown: in-flight requests finish within 30 s (`server.shutdown=graceful`), compose
  stop grace 35 s.
- JSON logs with trace id, correlation id, operation id, user id (token subject); Prometheus metrics
  per operation and circuit breaker state (see docs/operations.md).
