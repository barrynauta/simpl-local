# Operations

## Configuration (environment, MACH-07)

| Variable | Default | Meaning |
|---|---|---|
| VOCABULARY_CONTENT_BASE_URL | https://authority.example/vocabulary-manager | contentBase of E2 |
| ISSUER_URI | http://localhost:8193/realms/simpl | the one accepted issuer (E5) |
| JWKS_URI | realm certs endpoint | realm keys |
| TOKEN_AUDIENCE | vocabulary-manager-be | required audience |
| FUSEKI_URL, FUSEKI_READ_USER/PASSWORD, FUSEKI_UPDATE_USER/PASSWORD | | store access (D83) |
| VM_MAX_FILE_BYTES, VM_MAX_MULTIPART_BYTES, VM_MAX_PARALLEL_UPLOADS | 5242880, 6291456, 4 | limits (D15, D76, D92) |
| BACKEND_REPLICAS | 2 | backend instances (D102) |
| BACKEND_CPUS/MEMORY, FUSEKI_CPUS/MEMORY/HEAP, KEYCLOAK_CPUS/MEMORY, UI_CPUS/MEMORY | 2/1g, 2/2g/1g, 2/1536m, 1/128m | container limits (D98) |

Secrets live in `.env`, created by `./stack.sh up` with random values; the file is never committed
(D131). In production they come from OpenBao (E6).

## Backup and restore (Q3, D89)

- `ops/backup.sh`: reads the whole dataset as N-Quads with the backup user from inside the stack,
  writes `backups/vocabulary-<UTC time>.nq.gz` and a `.count` file. With `BACKUP_COPY_TARGET`
  set (a mounted path or an rsync target) it copies the backup outside the host.
- `ops/backup.cron`: example daily schedule (02:30 UTC); installing it is an operations task. In
  Kubernetes the same command runs in a CronJob.
- `ops/restore.sh <file>`: stops the backends and the store, loads the backup into a new TDB2
  dataset, checks the quad count against the backup, and only then swaps it in; the old dataset is
  kept as `vocabulary-old-<time>`. Writes after the backup are lost (within the 24 h recovery point).
- `ops/backup-restore-test.sh`: the round trip (backup, restore, backup, compare). Run on this build:
  PASS (68 quads before and after, no differing line).

TDB2 grows with every write (copy-on-write). Compaction (`tdb2.tdbcompact`) is an operations task
with the store stopped, like the restore.

## Observability (D86, D96)

- Logs: JSON lines (Logstash layout) with `traceId`, `spanId`, `correlationId`, `operationId`,
  `userId` (token subject), `vocabularyName` and `version` from the MDC. No email or token is logged
  (D80).
- Metrics: `/actuator/prometheus` on the management port 8081 (inside the stack only): request
  count, duration and status per operation (`http_server_requests_seconds` with the `uri` and
  `status` tags), circuit breaker states (`resilience4j_circuitbreaker_state`), JVM.
- Trace context: W3C `traceparent` is read and created by Micrometer Tracing (Brave bridge);
  `traceparent` and `X-Correlation-Id` are forwarded to the store and to the identity provider.

## Graceful shutdown (NFR-07)

`server.shutdown=graceful` with a 30 s phase timeout; compose gives 35 s before it kills a backend.
