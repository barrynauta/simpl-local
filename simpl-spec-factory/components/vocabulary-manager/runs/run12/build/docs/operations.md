# Operations

## Targets (Q3)

Availability 99.5 % per month in office hours; recovery time 30 minutes; recovery point 24 hours.

## Backup (daily)

`ops/backup.sh` calls the Fuseki admin backup (online, consistent, gzip N-Quads) inside the store
network with the admin user, waits for the task, copies the file to `BACKUP_DIR` (default
`./backups`) and, when `BACKUP_COPY_CMD` is set, copies it off the host (for example
`BACKUP_COPY_CMD='rclone copy {file} offsite:vm-backups'`). Schedules: `ops/schedule.cron.example`
(cron) and `ops/backup-cronjob.k8s.example.yaml` (Kubernetes CronJob). Installing a schedule is an
operations task (D89).

## Restore

`ops/restore.sh <backup.nq.gz>`:

1. stops the backends, gateway and UI (no write lands during the restore), then the store;
2. loads the backup into a new TDB2 directory with the offline loader (`tdb2.tdbloader`);
3. counts the quads of the new dataset and compares with the backup file; on a mismatch or an
   empty result it stops and leaves the old dataset untouched;
4. moves the old dataset to `vocabulary-old-<time>`, puts the new one in place, starts the store
   and then the services.

Writes made after the backup are lost (within the recovery point, D89). The round trip was run
on this build: backup of 565 quads, one more vocabulary created, restore, count 565 = 565, the
list shows the state of the backup, smoke test passes after it (BUILD-LOG.md).

## Compaction (weekly)

`ops/compact.sh` calls Fuseki `/$/compact/vocabulary?deleteOld=true` (online, D156). Run it after
the daily backup, for example on Sunday (see the cron example).

## Health, metrics, logs

- Container liveness: `GET :9090/actuator/health/liveness` (not the store, D152).
- Readiness for clients: `GET /v1/health` (UP, DEGRADED, DOWN; D44).
- Metrics: `docker exec <backend> wget -q -O - http://127.0.0.1:9090/actuator/prometheus`
  (management port is not published, D98, D120).
- Logs: JSON lines on stdout (`docker logs`), with traceId, correlationId, operationId,
  vocabularyName, version and userId (token subject). No e-mail, no token.

## Secrets

`./stack.sh up` writes `.env` (mode 600) with random passwords for the three store users and the
Keycloak admin on first start. `.env` is in `.gitignore`. In production the values come from
OpenBao (E6, D83).

## Limits per container (D98)

Defaults in `docker-compose.yml`, overridable through environment variables: backend 2 CPU and
1024 MB each (`BACKEND_CPUS`, `BACKEND_MEMORY`), Fuseki 2 CPU and 1536 MB, Keycloak 2 CPU and
1024 MB, gateway and UI 1 CPU and 128 MB. Replicas: `BACKEND_REPLICAS` (default 2).
