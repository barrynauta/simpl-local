# Quality targets: how the build meets them

Measured on 19 Nov 2026 on the build's own stack (Docker on a MacBook, OrbStack/Docker Desktop, all services local).

## Q1 Response time and throughput

| Target | Measured | How |
|---|---|---|
| Reads p95 at most 300 ms at 20 reads per second | p95 207 ms, 1200 reads, 0 errors | `ops/load-check.py 60`: 60 s after a 5 s warm-up, 20 reads per second (list with filter, details, versions, 1 MB content download, search) and 1 write per second |
| Create or new version up to 1 MB: p95 at most 3 s, 1 write per second | p95 439 ms, 60 writes, 0 errors | same run, 1 MB Turtle per new version |
| A file at the 5 MB limit within 30 s (D71) | 1.5 s for a 5 242 860 byte file; download 0.26 s, bytes equal | manual run with curl (BUILD-LOG.md) |

The first storage design took 15 s for one 5 MB write inside Fuseki (the SPARQL parser is slow on a single multi-megabyte literal). Content is now stored as base64 in 64 KB chunks, which brought the write down to 0.6 s in Fuseki.

## Q2 Limits (NFR-04)

| Limit | Enforced in | Error | Test at and above |
|---|---|---|---|
| vocabularyFile 5 MB (D15, D76) | `MultipartForm` | 413 payloadTooLarge | VIT.uploadLimits (5 MB + 1 byte); 5 MB exactly measured above |
| Multipart request 6 MB (D76) | `RequestGateFilter` (Content-Length), `MultipartForm` (stream) | 413 | VIT.uploadLimits |
| JSON body 1 MB (own limit) | `JsonBody` | 413 | code review |
| pageSize 1 to 100, page from 1 (D22) | `QueryRules` | 400 | RRT.pagingRules, VIT |
| Name 3 to 64, pattern (D37) | `FieldRules` | 400 | RRT.fieldRules |
| Description, changelog 1 to 1000 after trimming (D37, D56) | `FieldRules` | 400 | RRT.fieldRules (1000 and 1001) |
| Search: 1 to 100 items, at most 100 URIs (D38) | `SearchController` | 400 | VIT, XIT (101) |
| aliasUris at most 20 (D56) | `ExternalVocabularyController` | 400 | XIT (21) |
| Lock wait 30 s (D71) | `NameLocks` | 503 | SRT.locksSerialiseAndTimeOut (with a short timeout) |
| Multipart parts at most 200 | `MultipartForm` | 400 | code review |
| Call rate | Not in the service (D46): Tier 1 gateway in production; Keycloak brute-force protection for logins | n/a | n/a |

## Q3 Availability and recovery (NFR-06)

- Target: 99.5 % per month in office hours, RTO 30 min, RPO 24 h. Not measurable inside a build; the design supports it as follows.
- Backup: `ops/backup.sh` dumps the whole dataset as N-Quads with a quad count and SHA-256. It must run daily (cron or a Kubernetes CronJob) and the files must be copied off the host (`BACKUP_DIR` on mounted remote storage). Scheduling and the off-host copy are operations tasks, not automated in this stack.
- Restore: `ops/restore.sh <file>` checks the checksum, loads the backup into a new in-memory dataset in a throw-away Fuseki container, checks the quad count, refuses a backup with fewer quads than the live store unless `--force`, then replaces the live dataset in one transaction (Graph Store Protocol PUT) and checks the count again.
- Round trip tested on this build: `ops/backup-restore-test.sh` passed ("restored 350 quads", the changed status returned to ACTIVE).
- Recovery: `./stack.sh up` restarts everything; data lives in the named volume `fuseki-data`; containers use `restart: unless-stopped`.

## Q4 Design load and scaling

One backend instance (D81). Scale up: backend 2 CPU and 1.5 GB, Fuseki 1 CPU and 1 GB. The list operation reads all vocabulary rows in one query and filters in memory; at 1000 vocabularies that is one small query. Not load-tested at 1000 vocabularies with 20 versions each.

## Q5 Maintainability

- Backend coverage from my own tests (JaCoCo, `docs/coverage/jacoco/index.html`): line 93.0 % (2008 of 2158), branch 78.9 % (696 of 882). The JaCoCo check in `mvn verify` enforces 80 % and 60 %.
- Tests: 59 backend tests (unit and integration against an embedded Fuseki with signed test tokens), 6 Vitest UI tests, the Playwright UI check, `smoke.sh`.
- Trivy: 0 HIGH and 0 CRITICAL in own images and dependencies; Keycloak upstream image findings are in SECURITY-SCAN.md.
- Reference check: one store query per upload (`VocabularyStore.referenceFacts`, a UNION of internal and external lookups), none when the file declares no reference that needs the store.
- Formatting checked in `mvn verify` (Spotless, Palantir) and `npm run format:check` (Prettier).

## Q6 Accessibility

axe-core 4.14.1 with the WCAG 2.0 and 2.1 A and AA rule sets: no violations on the list, details, create and new version pages (UIC). Menus open with Enter or Space, move with the arrow keys and close with Escape; every action is a button or link. Keyboard-only walkthrough was not automated beyond the menu and focus checks.

## NFR-05 Bounded resources, health and shutdown (NFR-07, NFR-08)

| Item | Setting |
|---|---|
| Container memory and CPU | backend 1.5 GB / 2 CPU (JVM `MaxRAMPercentage=70`), store-down backend 1 GB / 1 CPU, Fuseki 1 GB / 1 CPU, Keycloak 1 GB / 1.5 CPU, UI 128 MB / 0.5 CPU |
| Measured idle use after the load run | backend 266 MB, Fuseki 384 MB, Keycloak 552 MB, UI 14 MB |
| Health | Docker health checks on every service; backend readiness on the management port 8081 (not published); `GET /v1/health` per D44 |
| Graceful shutdown | `server.shutdown=graceful`, 30 s per phase, `stop_grace_period` 35 s |
| Timeouts | store connect 2 s, reads 10 s, writes 45 s, key fetch 2 s, lock wait 30 s; Tomcat connection timeout 20 s; nginx proxy 90 s |
| Hardening | non-root users, `cap_drop: ALL`, `no-new-privileges`, read-only root file system for backend and UI |

## Observability

Structured JSON logs (ECS) with a correlation id per request (`X-Correlation-ID`, echoed in the response). Authentication failures and access denials are logged at WARN with the token subject only (D80). No metrics endpoint is published; the management port exposes health only.
