# QUALITY.md

How the build meets the quality targets Q1 to Q6 and NFR-04 to NFR-08. Measured on 19 Nov 2026 on a MacBook
(Apple silicon, OrbStack), stack of this folder, after warm-up.

## Q1 Response time and throughput

| Measure | Target | Measured (`perf/measure.py 60`) |
|---|---|---|
| Create with a 5 MB file (D71) | at most 30 s | 1.17 s, 201 |
| Create with a 1 MB file | p95 at most 3 s | 0.24 s (single), write p95 in the mix 245 ms |
| Read p95 (list, details, search, download under 1 MB) | at most 300 ms | 131 ms |
| Offered 20 reads/s and 1 write/s | at least 95 % achieved, no errors | 60 s: 1200 reads (20.0/s), 60 writes (1.0/s), 0 errors |

Limit of this measurement: 60 s instead of the 5 minutes of D99, and the store held a few hundred versions, not the
20 000 of the D99 seed. Q1 is therefore met on this stack at a smaller store size; it is not proven at design load.
Design choices that keep it at design load: every list query runs ORDER BY, LIMIT and OFFSET in the store with a
COUNT for the total (D85); content is stored in chunks in its own named graph and read only on download; the
reference check is one store query per upload (Q5).

## Q2 Limits (NFR-04)

| Limit | Value | Enforced in | Error |
|---|---|---|---|
| vocabularyFile part | 5 242 880 bytes | `MultipartForm` | 413 `simpl:payloadTooLarge` |
| Whole multipart request | 6 291 456 bytes | `MultipartForm` | 413 |
| JSON body | 1 MB (own choice) | `JsonBody` | 413 |
| Multipart parts | 64 (own choice) | `MultipartForm` | 400 |
| page, pageSize | 1 to 2147483647, pageSize at most 100 | `QueryParsers` | 400 |
| Name | 3 to 64, `^[A-Z][a-zA-Z0-9]*$` | `FieldRules` | 400 |
| Description, changelog | 1 to 1000 after trimming | `FieldRules` | 400 |
| Internal search items | 1 to 100 | `SearchController` | 400 |
| External search URIs | at most 100 in total | `SearchController` | 400 |
| aliasUris | at most 20, no duplicates | `ExternalVocabularyController` | 400 |
| Concurrent uploads per instance (D92) | 4 | `UploadSlots` | 503 with Retry-After 5 |
| Write lock wait / retries (D71, D82) | 5 retries within 30 s | `VocabularyService` | 503 |
| Store call timeouts (D44, D71) | connect 2 s, read 10 s, update 45 s | `FusekiSparqlStore` | 503 |

The limits are configuration (`vocabulary-manager.limits` in `application.yaml`). Own tests cover the 413, page,
pageSize, field length and search size limits at and above the limit (`VocabularyApiTest`, `RequestParsingTest`)
and the fifth upload (`VocabularyServiceTest.fifthUploadIsRefused`). The heap is 75 % of 1 GB, sized for 4 uploads at
6 MB with their parsed models; a test with 4 parallel 6 MB uploads was not run.

## Q3 Availability and recovery (NFR-06)

- Health and restart: compose health checks on every service, `restart: unless-stopped`; liveness and readiness
  on the management port (MACH-08).
- Graceful shutdown: `server.shutdown=graceful`, 30 s per phase, `stop_grace_period: 35s`.
- Backup: `ops/backup.sh` dumps the whole dataset (N-Quads, gzip) and copies it to `BACKUP_COPY_TARGET` outside
  the host. Daily schedule examples: `ops/backup.cron.example`, `ops/backup-cronjob.example.yaml` (D89).
- Restore: `ops/restore.sh` stops the backend, loads into a staging dataset, checks the quad count, only then
  replaces the live data, and starts the backend (never emptier than before).
- Round trip tested this run: `ops/backup-restore-test.sh` passed, quads before 38, after a marker write 61, after
  restore 38, marker gone, corrupt backup left the store untouched.
- Recovery time: not measured on its own. The whole round trip (backup, write, restore with a backend stop and
  start, corrupt-backup check) finished in about 2 minutes on this small data size; a restore at design load is
  not measured.

## Q4 Design load and scaling

The service holds no lock or session state (D82); several instances may run on one store, proven by
`StoreModelTest.twoInstancesOnOneStoreKeepVersionNumbersUnique`. The stack runs one instance.

## Q5 Maintainability

| Measure | Target | Result |
|---|---|---|
| Backend line coverage (JaCoCo, `backend/target/site/jacoco/`) | at least 80 % | 92.0 % |
| Backend branch coverage | at least 60 % | 77.9 % |
| Backend tests (`mvn verify`) | green | 59 tests, 0 failures, 0 errors |
| UI tests (Vitest, in the UI image build) | green | 14 tests in 4 files, all pass |
| Formatter and lint (D95) | fail on errors | Spotless (Palantir) check in `mvn verify`; ESLint, Prettier check and vue-tsc in `npm run build` |
| HIGH and CRITICAL in own images and dependencies | none | none (SECURITY-SCAN.md) |
| Store queries for the reference check | one per upload | one (`ReferenceAnalyzer`) |

## Q6 Accessibility

`ui/tests/a11y.spec.ts` runs axe-core on the list, details and both form pages: no violations. It runs in jsdom,
so colour contrast is not checked there. All actions are buttons, links or menu items reachable by keyboard; the
actions menu opens with Enter or Space and closes with Escape.

## Container limits (NFR-05)

| Service | Memory | CPU | Seen at rest |
|---|---|---|---|
| backend | 1 GB (`BACKEND_MEM_LIMIT`) | 2 (`BACKEND_CPUS`) | 277 MB |
| fuseki | 1 GB (`STORE_MEM_LIMIT`) | 2 | 678 MB |
| keycloak | 1 GB (`KEYCLOAK_MEM_LIMIT`) | 2 | 534 MB |
| ui | 128 MB (`UI_MEM_LIMIT`) | 1 | 14 MB |

Containers run read-only with tmpfs, all capabilities dropped and no-new-privileges, as non-root users.

## Observability (NFR-07, NFR-08)

- JSON log lines with timestamp, level, `traceId`, `spanId`, `correlationId`, `operationId`, and `vocabularyName`
  and `version` when known (D96). An access log line per request with method, path, status and duration. No email,
  name or token (D80).
- `X-Correlation-Id` returned on every response and forwarded with `traceparent` to the store and Keycloak (D86).
- Prometheus metrics on the internal management port 8081 (`/actuator/prometheus`): request count, errors by
  status and duration per operation.
