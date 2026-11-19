# Quality targets (Q1 to Q6) and operations (NFR-04 to NFR-08)

Measured on 19 Nov 2026 on the build's own stack (OrbStack on a 16 GB Mac, compose project `vm-factory-run8`).

## Q1 Response time and throughput

Measured with a Python load script, twice (20 reader threads at one request per second each, spread over list, details, version list and download; one writer creating a new version per second with a 41 KB file; 60 s after 30 warm-up reads; about a dozen vocabularies already in the store):

| Measure | Target | Measured |
|---|---|---|
| Reads p95 | at most 300 ms | 30 ms with a dozen vocabularies stored; 152 ms with 1000 more vocabularies of 2 versions each (1200 reads, 20.0 per second) |
| Writes up to 1 MB p95 | at most 3 s | 83 ms, and 172 ms with the 1000 vocabularies stored (60 new versions, 1.0 per second) |
| Errors under that load | none | 0 in both runs |
| Create with a file at the 5 MB limit (5 242 660 bytes, 200 000 triples) | at most 30 s (D71) | 21.9 s |
| New version with a 5 MB file | at most 30 s | 16.5 s |

The 5 MB write leaves only 8 s of margin. I did not profile where the time goes (parsing, validation or the store update of the base64 content). The second run used Q4's 1000 vocabularies (seeded with `Seed5515N0000` to `Seed5515N0999`, 2 versions each, 200 triples per file); the full Q4 volume of 20 versions per vocabulary (20 000 versions) was not loaded.

## Q2 Limits (NFR-04)

| Limit | Where enforced | Error | Own test at and above the limit |
|---|---|---|---|
| vocabularyFile at most 5 MB (D15) | `web/body/MultipartForm` | 413 UploadTooLarge | `SecurityAndShapeApiTest.uploadLimits` (5 MB gives 201, 5 MB + 1 gives 413) |
| Multipart request at most 6 MB (D76) | `RequestShapeInterceptor` (Content-Length) and `MultipartForm.LimitedInputStream` (streamed) | 413 | `uploadLimits` |
| JSON body at most 1 MB (own limit, no decision) | `RequestShapeInterceptor` (Content-Length) | 413 | `uploadLimits` (2 MB search body) |
| page at least 1, pageSize 1 to 100 (D22) | `web/api/Paging` | 400 | `VocabularyApiTest.listFiltersSortsAndPages` |
| Name 3 to 64, pattern (D37) | `domain/FieldRules` | 400 | `VocabularyApiTest.fieldRulesOnCreateAndNewVersion`, `DomainAndValidationTest.nameRules` |
| Description and changelog 1 to 1000 after trimming (D37, D56) | `FieldRules` | 400 | same tests (1000 accepted, 1001 refused) |
| Search: 1 to 100 items, at most 100 URIs (D38) | `VocabularyController`, `ExternalVocabularyController` | 400 | `searchReturnsResultsInRequestOrder`, `ExternalVocabularyApiTest.searchByUris` |
| At most 20 aliasUris (D56) | `ExternalVocabularyController` | 400 | `registrationConflictsAndRules` (21 refused) |
| Store write budget 30 s, update timeout 45 s, read timeout 10 s (D44, D71) | `application.yml`, `HttpSparqlStore`, `ConditionalWrite` | 503 | `ConcurrencyAndFailureTest.storeErrorsAndTimeouts` (with shortened timeouts) |
| Parts per request 50, part header 2 KB | `MultipartForm` | 400 | covered by the multipart tests |

## Q3 Availability and recovery (NFR-06, NFR-07)

- Backup: `ops/backup.sh [dir]` dumps the whole dataset `vocabulary` as N-Quads through the Graph Store Protocol with the backup user, from a short-lived container on the internal network `store-net` (the store publishes no port, D83). It writes the quad count next to the file. Daily scheduling and copying the file off the host (Q3) belong to the deployment (cron or a Kubernetes CronJob); the script is the unit it runs.
- Restore: `ops/restore.sh <file>` loads the backup into the staging dataset `vocabulary-restore`, checks that the count equals the backup's count, takes a safety copy of the live data, and only then replaces `vocabulary` and checks the count again. On a mismatch it stops, or puts the safety copy back. It refuses a backup with fewer quads than the live store unless `--allow-smaller` is given, so a restore never leaves the store emptier by accident.
- Round trip test, run on this build: `ops/backup-restore-test.sh` gave 532 quads live, 532 in the backup, 532 staged and 532 after the restore; the API still serves the test vocabulary with identical bytes.
- Caveat: writes made between the backup and a restore are lost. Run a restore in a quiet window, after stopping writes.
- RTO 30 min: a restore of the current store took under 5 s. RPO 24 h follows from a daily backup.

## Q4 Design load and scaling

- D82: no lock state in the service; every write is a conditional SPARQL update confirmed by its change record, retried at most 5 times within 30 s. Two service instances on one store are tested in `ConcurrencyAndFailureTest.twoInstancesOnOneStoreSerialiseThroughTheStore` (6 parallel versions across both instances give versions 2 to 7 exactly once; 4 parallel creations of one name give one 201 and three 409; two identical PATCH give 200 and 409).
- The compose file runs one backend replica; scale out is allowed by D82.

## Q5 Maintainability

- Coverage from the own tests (JaCoCo, `reports/jacoco/index.html`): line 94.5 % (1991 of 2106), branch 83.7 % (709 of 847). `mvn verify` fails below 80 % line or 60 % branch.
- Trivy: 0 HIGH and 0 CRITICAL in own images and dependencies (SECURITY-SCAN.md).
- Reference check: one store query per upload (`ReferenceLookup`), tested in `DomainAndValidationTest.referenceRulesWarnAndErrorWithOneLookup` (call count 1) and `noLookupWithoutReferences` (call count 0).
- D85: every list query runs ORDER BY, LIMIT and OFFSET in the store, the total from a COUNT query.

## Q6 Accessibility

The UI uses semantic tables with column headers, labelled form fields, `role="status"` for success and `role="alert"` for errors, named action menu buttons and regions (D75). I did not run axe; WCAG 2.1 AA is not measured by this build (see DEVIATIONS.md).

## Containers: limits, health, shutdown (NFR-05, NFR-08)

| Service | CPU / memory limit | Health check | Shutdown |
|---|---|---|---|
| backend | 2 CPU / 1 GB (JVM at 70 % of it) | readiness on the management port 8081 (not published) | `server.shutdown: graceful`, 25 s per phase, `stop_grace_period: 30s` |
| backend-store-down | 1 CPU / 768 MB | same | same |
| fuseki | 1.5 CPU / 1 GB (`-Xmx768m`) | `/$/ping` | TDB2 is transactional; SIGTERM stops the JVM |
| keycloak | 1.5 CPU / 1 GB | `/health/ready` on 9000 | default |
| ui (nginx) | 0.5 CPU / 128 MB | `/healthz` | nginx graceful quit |

Every container: non-root user, `cap_drop: ALL`, `no-new-privileges`, read-only root file system where the product allows it (backend, fuseki, ui), tmpfs for scratch. Measured memory at the end of the load test: backend 331 MB, fuseki 465 MB, keycloak 599 MB, ui 14 MB.

## Observability (D86)

- Logs: ECS JSON on stdout, with `traceId`, `spanId`, `correlationId` and the token subject as user id; never email or name (D80).
- Trace context: W3C `traceparent` and `X-Correlation-Id` are read or created, returned, and forwarded to the store and the identity provider.
- Metrics: Prometheus at `/actuator/prometheus` on the management port: `http_server_requests` count, error status and duration per operation (`SecurityAndShapeApiTest.managementPortServesHealthAndMetricsWithoutToken`).
- `GET /v1/health`: UP, DEGRADED (store fine, no realm keys and Keycloak down) or DOWN with 503 (store not answering in 2 s), tested in `ConcurrencyAndFailureTest`.

## API robustness (finish line 5)

Schemathesis (seed 1, 30 examples per operation, all checks) against the published OpenAPI: 1073 generated requests, no 5xx, no response that breaks the response schemas. The findings left are decided contract changes (D79): 13 "schema-valid request rejected" (stricter field, URI and file rules) and 1 "schema-violating request accepted" (`name=000`, the relaxed name filter of D19). Output: `reports/fuzz-schemathesis.txt`.
