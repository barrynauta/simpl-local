# Quality targets

Measured on 23 Nov 2026 on the build's own stack (Docker Desktop on an Apple silicon Mac, 18 CPUs, 39 GB for
Docker), two backend replicas, the limits of D202.

| Target | How the build meets it | Measured |
|---|---|---|
| Q1 response time and throughput (D99, D136, D183, D188) | Paging, filter and sort in the store with LIMIT/OFFSET and a COUNT; the vocabulary node carries all listed fields (no join per version); the reference check is one query; content in one literal per version | `scripts/loadtest.py 120 300 2`: store with about 1 250 vocabularies (300 x 2 seeded plus earlier runs and tests). Offered 20 reads/s and 1 write/s for 120 s: 2 520 of 2 520 completed (20.9/s), 0 errors. p95 per operation: details 42.6 ms, download 44.8 ms, list 89.7 ms, filtered list 56.9 ms, search (10 names) 43.2 ms, version list 46.1 ms, new version (10 KB) 108.4 ms. Writes at the limit: a 5 242 834-byte file, create 4.91 s, new version 4.64 s (target 30 s, D71). Not measured with the full D99 seed of 1 000 x 20 (DEVIATIONS.md). |
| Q2 limits (NFR-04) | Each limit has a defined error: file 5 MB (413 with issue), request 6 MB (413), proxy 7 MB (413), JSON 1 MB (413), 64 parts (400 body), pageSize 100, page and version int32, names, texts 1 to 1000, search 100 items or URIs, 20 aliases, report 1000 issues with `truncated`, 4 uploads per instance (503), lock-free write budget 30 s (503) | Own tests at and above the limits: VocabularyApiTest.createFieldRules, listParameterErrors, searchFollowsD38; ExternalApiTest; AccessApiTest.negotiationAndOrder, fifthUploadGets503BeforeTheBodyIsRead; proxy limits checked by hand (BUILD-LOG) |
| Q3 availability and recovery (D89, D156, D191) | Daily backup script with copy target outside the host; restore loads into a new dataset, checks the count, then swaps; backends stopped during restore; weekly online compaction | Availability: not measured; needs production monitoring. Backup and restore round trip (`scripts/backup-restore-test.sh`): passed, 316 quads before and after, restore 16 s (RTO target 30 min); the API answered again about 3 s later. RPO: daily backup, 24 h. |
| Q4 design load and scaling (D82, D102, D189) | Stateless backends, conditional store updates; the stack runs two backend replicas behind the proxy | smoke.sh passes on the two-replica stack; StoreBehaviourTest.twoInstancesOnOneStoreNeverReuseANumber (8 parallel versions through two service instances, numbers 2 to 9 once each); 4 parallel new versions through the API give 201 each with distinct numbers |
| Q5 maintainability (D95, D175) | JaCoCo gate in `mvn verify` (line 80 %, branch 60 %); Spotless check; ESLint, Prettier and Vitest in the UI build; Trivy | Backend line coverage 94.5 %, branch 86.2 % (report `backend/target/site/jacoco/index.html`, kept in `reports/jacoco/`). 0 HIGH and 0 CRITICAL in own images and dependencies (SECURITY-SCAN.md). Reference check: one store query per upload. |
| Q6 accessibility | Labels on every field, `aria-required` with a visible `*`, table headers, `aria-sort`, region and role names of D75 and D90, focus-managed modal dialog with focus on Cancel, keyboard menu (arrow keys, Escape) | `scripts/ui-check.py bypass` (Playwright, Chromium, axe-core 4, WCAG 2.0/2.1 A and AA rules, page CSP bypassed as D123 says): 0 violations on the list, create form, details page, new version form, status dialog, external list and external form. The dialog opens with focus on Cancel. |

## Container limits (D98, D202)

| Container | CPU | Memory | Variables |
|---|---|---|---|
| backend (each of 2) | 2 | 1.5 GB (heap 70 %) | BACKEND_CPUS, BACKEND_MEMORY, BACKEND_REPLICAS |
| fuseki | 2 | 2 GB (heap 1.2 GB) | FUSEKI_CPUS, FUSEKI_MEMORY, FUSEKI_HEAP |
| keycloak | 1 | 1 GB | KEYCLOAK_CPUS, KEYCLOAK_MEMORY |
| proxy (nginx with UI) | 0.5 | 256 MB | PROXY_CPUS, PROXY_MEMORY |

## Health and shutdown (MACH-08, D44, D152)

* Container health: backend `/actuator/health/liveness` on the management port (not the store, so a backend stays
  healthy while the store is down); Fuseki `/$/ping` with the read user; Keycloak `/health/ready`; proxy serves
  `index.html`. `docker compose up --wait` returns only when all are healthy.
* Readiness view `GET /v1/health`: UP, DEGRADED, or DOWN (503) when the store does not answer within 2 s.
* Graceful shutdown: `server.shutdown=graceful`, 30 s per phase, compose `stop_grace_period` 40 s.

## Observability (D86, D96)

JSON log lines with traceId, spanId, correlationId, operationId, vocabularyName, version, userId (token subject).
Prometheus metrics on the management port: per operation request count and duration by status
(`vocabulary_api_requests_seconds`), HTTP server metrics, circuit breaker state and calls. The proxy never forwards
`/actuator` (smoke.sh checks 404 through both proxy ports).

## Backup and restore procedure (NFR-06)

See docs/operations.md: `scripts/backup.sh` (daily, `BACKUP_COPY_TARGET` outside the host),
`scripts/restore.sh <file>`, `scripts/backup-restore-test.sh`, `scripts/compact.sh`, `scripts/cronjobs.example`.
