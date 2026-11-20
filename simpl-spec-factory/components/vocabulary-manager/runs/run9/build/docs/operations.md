# Operations

## Configuration (environment, MACH-07, E4)

| Variable | Default | Meaning |
|---|---|---|
| `VOCABULARY_CONTENT_BASE_URL` | `https://authority.example/vocabulary-manager` | contentHref and internal references (E2, D12) |
| `STORE_QUERY_URL`, `STORE_UPDATE_URL` | Fuseki in the stack | SPARQL endpoints |
| `STORE_READ_USER/PASSWORD`, `STORE_UPDATE_USER/PASSWORD` | from `.env` | store users (D83); OpenBao in production |
| `STORE_ADMIN_PASSWORD` | from `.env` | used only by the backup and restore tools |
| `ISSUER_URI` | `http://localhost:8193/realms/simpl` | accepted issuer (E5) |
| `JWK_SET_URI` | Keycloak certs endpoint | realm keys (D44) |
| `TOKEN_AUDIENCE` | `vocabulary-manager-be` | required audience (E5) |
| `MANAGEMENT_PORT`, `MANAGEMENT_ADDRESS` | 8081, internal network address | health and metrics (D67, D98) |
| `*_MEM_LIMIT`, `*_CPUS` | see compose.yaml | container limits (D98) |

## Health, shutdown, logs, metrics

- `GET /v1/health`: UP, DEGRADED (store fine, no realm keys and Keycloak down) or 503 DOWN (store not answering
  in 2 s). Liveness and readiness: `/actuator/health/liveness|readiness` on the management port.
- Graceful shutdown: Spring `server.shutdown=graceful`, 30 s per phase, compose `stop_grace_period: 35s`.
- Logs: JSON lines (logstash layout) with `traceId`, `spanId`, `correlationId`, `operationId`, and when known
  `vocabularyName` and `version` (D96). No email, name or token in logs; the user id is the token subject (D80).
- Metrics: `http://10.209.9.10:8081/actuator/prometheus` on the internal network (`http_server_requests_seconds`
  per operation URI and status). From the host: `docker compose -p vm-factory-run9 exec -T fuseki wget -qO-
  http://10.209.9.10:8081/actuator/prometheus` (the store container is on the internal network). The front network
  (UI, Keycloak) cannot reach the management port (D98).

## Backup and restore (Q3, D89)

- `ops/backup.sh`: whole dataset as N-Quads (gzip) in `backups/`, then copied to `BACKUP_COPY_TARGET` (rsync or
  a mounted path outside the host). Example schedules: `ops/backup.cron.example` (cron, daily 02:30 UTC) and
  `ops/backup-cronjob.example.yaml` (Kubernetes CronJob). Installing the schedule is an operations task.
- `ops/restore.sh <file>`: stops the backend, loads the file into the staging dataset, compares the quad count with
  the file, and only then replaces the live dataset in one transaction (graph store PUT); restarts the backend.
  Writes after the backup are lost, within the 24 h recovery point.
- `ops/backup-restore-test.sh`: the round trip of every build (backup, write a marker, restore, check the count
  and that the marker is gone, then check that a corrupt backup leaves the store untouched).

## Scaling

One instance is the default. Several instances on one store are safe for writes (D82, tested in
`StoreModelTest`); the upload limit of D92 is per instance.
