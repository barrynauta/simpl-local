#!/bin/sh
# Runs in a minio/mc container on the compose network. Creates the two buckets
# and seeds the source object. Idempotent (mb -p ignores existing).
set -e
echo "Waiting for MinIO..."
until mc alias set local http://minio:9000 minioadmin minioadmin >/dev/null 2>&1; do
  sleep 2
done
mc mb -p local/provider-bucket
mc mb -p local/consumer-bucket
mc cp /seed/example-s3.txt local/provider-bucket/example-s3.txt
echo "MinIO seeded: provider-bucket/example-s3.txt, consumer-bucket created."
