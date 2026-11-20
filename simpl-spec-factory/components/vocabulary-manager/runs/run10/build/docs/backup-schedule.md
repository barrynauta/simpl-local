# Backup, restore and schedule (Q3, D89, NFR-06)

## Targets

Availability 99.5 % per month in office hours; recovery time 30 minutes; recovery point 24 hours (Q3).

## Backup

`scripts/backup.sh` exports every quad of the dataset `vocabulary` as N-Quads through the Graph Store Protocol,
with the store admin user, from a short-lived `store-tools` container on the internal store network. The file is
gzipped and gets a `.sha256` and a `.count` (number of quads). `BACKUP_TARGET` sets where it is written; in
production it must be outside the host (a mounted network share, or a directory copied off the host by the
operations job right after the backup).

Example schedule (cron on the deployment host, daily at 02:30 UTC):

```cron
30 2 * * * cd /opt/vocabulary-manager && BACKUP_TARGET=/mnt/backup-share/vocabulary-manager scripts/backup.sh >> /var/log/vm-backup.log 2>&1
```

Example for Kubernetes (the store service `fuseki`, the admin password from a secret filled by OpenBao):

```yaml
apiVersion: batch/v1
kind: CronJob
metadata: { name: vocabulary-store-backup }
spec:
  schedule: "30 2 * * *"
  concurrencyPolicy: Forbid
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: backup
              image: vm-factory-run10/store-tools:1.0.0
              command: ["/bin/sh", "-c"]
              args:
                - >-
                  curl -sS --fail -u "admin:${STORE_ADMIN_PASSWORD}" -H 'Accept: application/n-quads'
                  http://fuseki:3030/vocabulary/data | gzip -9 > /backup/vocabulary-$(date -u +%Y%m%dT%H%M%SZ).nq.gz
              env:
                - name: STORE_ADMIN_PASSWORD
                  valueFrom: { secretKeyRef: { name: vocabulary-store, key: admin-password } }
              volumeMounts: [{ name: backup, mountPath: /backup }]
          volumes:
            - name: backup
              persistentVolumeClaim: { claimName: vocabulary-backup-offsite }
```

Installing the schedule is an operations task (D89); the build delivers the scripts and these examples.

## Restore

`scripts/restore.sh <archive.nq.gz>`:

1. checks the archive against its `.sha256`;
2. stops the backends, so no write lands during the restore;
3. loads the backup into the staging dataset `vocabulary-restore` and compares its quad count with the backup;
4. only when equal, replaces the live dataset with one Graph Store Protocol PUT (one store transaction: all or
   nothing), and checks the count again;
5. clears the staging dataset and starts the backends (also when a step fails: the live store is then untouched).

Writes made after the backup are lost, which is within the recovery point (D89).

## Round trip test

`scripts/backup-restore-test.sh` creates a vocabulary, backs up, creates a second vocabulary, restores, and checks
that the first exists and the second does not. Result of this build: see QUALITY.md.
