# Quality targets

The decisions contain no separate Q decisions. The quality targets of this build are the limits and failure rules in D15, D22, D37, D38, D43, D44 and D46, and the rules NFR-04 to NFR-08. Measurements below were taken on 19 Nov 2026 on a 16 GB Mac (Docker Desktop), with the stack of this folder.

## Limits (NFR-04)

| Limit | Value | Enforced by | Beyond the limit |
|---|---|---|---|
| Vocabulary file | 5 242 880 bytes (D15) | `vm.limits.max-upload-bytes`, controller check | 413 UploadTooLarge (checked live with a 6 MB file) |
| Multipart request | 12 MB container limit for the request and for its text parts; 10 MB per file part | Tomcat multipart | 413 problem+json; an over-long text field below 12 MB gets the D37 400 (test `TomcatLimitsIntegrationTest`) |
| JSON body (PATCH, searches) | 256 KB | `vm.limits.max-json-body-bytes` | 413 problem+json |
| Page size | 1 to 100, default 20 (D22) | `api/Paging` | 400 |
| Search items | 1 to 100; external at most 100 URIs (D38) | `api/VocabularyController.search`, `api/ExternalSearchBody` | 400 |
| Alias URIs | at most 20 (D56) | `api/ExternalVocabularyController` | 400 |
| Description, changelog | 1 to 1000 characters after trimming (D37) | `api/RequestChecks` | 400 |
| Validation issues per report | 1000 | `vm.limits.max-validation-issues` | report truncated at 1000 |
| Turtle nesting depth | bounded by the parser; deep nesting is a syntax error, not a crash | `validation/TurtleParser` | 400 (test `deeplyNestedContentIsASyntaxErrorNotACrash`) |
| Call rate | none in the service (D46); the Tier 1 gateway applies it in production | n/a | n/a |

## Bounded resources (NFR-05)

| Container | Memory limit | CPU limit | Measured at rest |
|---|---|---|---|
| backend | 768 MB (heap 70 %) | 1.5 | 183 MB |
| fuseki | 768 MB (heap 70 %) | 1.0 | 209 MB |
| keycloak | 1 GB | 1.5 | 487 MB |
| ui | 128 MB | 0.5 | 14 MB |
| backend-store-down (profile) | 768 MB | 1.0 | n/a |

Tomcat: 100 worker threads, accept queue 100, 2000 connections. Locks: 256 fixed stripes. List queries are always paged; version lists are paged; searches are capped at 100 references. Root filesystems are read-only, with small tmpfs mounts; all capabilities dropped; `no-new-privileges`.

## Health and graceful shutdown (NFR-07, MACH-08)

- Compose health checks on every service; `docker compose up --wait` returns only when all are healthy.
- `GET /v1/health` gives UP, DEGRADED or DOWN as D44. Liveness and readiness probes on the management port 8081.
- `server.shutdown: graceful` with 20 s per phase and `stop_grace_period: 30s`: in-flight requests finish; each write is one store transaction, so a stop never leaves half a change.

## Store and identity provider down (D44, MSV-06)

Checked live on the store-down backend (port 8199): `GET /v1/vocabularies` and `POST /v1/vocabularies` give 503 `urn:problem-type:belgif:serviceUnavailable` with `Retry-After: 5`; `GET /v1/health` gives 503 DOWN. Unit tests cover retries (`SparqlClientTest`) and the identity provider cases (`RealmKeySourceTest`).

## Concurrency (D43)

`VocabularyServiceTest.parallelVersionCreationsAreSerialised`: five parallel new versions give versions 2 to 6 once each, version 6 ACTIVE and 1 to 5 DEPRECATED.

## Observability (NFR-08, MSV-09, CODE-26 to CODE-29)

- Structured JSON logs in ECS format with timestamp, level, logger and the correlation id (`X-Correlation-Id`, taken from the request or created, returned in the response).
- Security events (401, 403, firewall rejections, token failures) on the `security` logger without token contents.
- Prometheus metrics at `:8081/actuator/prometheus` (`http_server_requests_seconds` per URI, method and status gives rate, errors and duration per operation).
- Trace context: not propagated to Fuseki (no tracing backend in the stack); see DEVIATIONS.md.

## Availability and backup (NFR-06)

- Availability target: 99.5 % in office hours for this single-instance build; RTO 30 minutes (restore plus restart); RPO 24 hours with a daily backup.
- Backup: `ops/backup.sh` dumps the whole Fuseki dataset as N-Quads while the stack runs. Run it daily (cron or the platform scheduler) and keep the files off the host.
- Restore: `ops/restore.sh <file>` stops the backend, drops and reloads the dataset, starts the backend and prints the quad count.
- Restore tested on 19 Nov 2026: backup of 169 quads, restore, second backup; the two sorted dumps were identical, and `smoke.sh` passed afterwards.

## Measured

| What | Result |
|---|---|
| Start from a clean state (volumes and own images removed, Docker layer cache kept) | 34 s until all four services healthy |
| `GET /v1/vocabularies?pageSize=100`, 100 calls | p50 11 ms, p95 15 ms, max 23 ms |
| `GET /v1/content/<Name>`, 30 calls | p50 9 ms, max 15 ms |
| Fuzzing (Schemathesis, about 46 000 requests) | no 5xx |
| Backend line coverage (JaCoCo) | 87 % lines, 67 % branches |
| UI helper coverage (Vitest) | 96 % lines |
