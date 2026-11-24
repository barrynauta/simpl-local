#!/usr/bin/env bash
# Daily backup of the whole store (Q3, D89): dumps every named graph as N-Quads through the admin user, from
# inside the store network, writes <file>.nq.gz with its quad count and SHA-256, and copies it outside the host.
#   BACKUP_DIR          local directory (default: ../backups next to this script)
#   BACKUP_COPY_TARGET  copy target outside the host: a mounted path or an scp target user@host:/path (optional)
set -euo pipefail
cd "$(dirname "$0")/.."
export COMPOSE_PROJECT_NAME=vm-factory-run14
BACKUP_DIR="${BACKUP_DIR:-$PWD/backups}"
mkdir -p "$BACKUP_DIR"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="$BACKUP_DIR/vocabulary-$stamp.nq.gz"
docker compose exec -T fuseki sh -c \
  'wget -q -O - --header "Accept: application/n-quads" --header "Authorization: Basic $(printf "%s:%s" "$FUSEKI_ADMIN_USER" "$FUSEKI_ADMIN_PASSWORD" | base64)" http://localhost:3030/vocabulary/dump' \
  | gzip -9 > "$file.part"
count="$(gzip -dc "$file.part" | grep -c . || true)"
if [ "$count" -lt 1 ]; then
  rm -f "$file.part"
  echo "backup refused: the dump holds no quads" >&2
  exit 1
fi
mv "$file.part" "$file"
echo "$count" > "$file.count"
( cd "$BACKUP_DIR" && shasum -a 256 "$(basename "$file")" > "$(basename "$file").sha256" )
echo "backup written: $file ($count quads)"
if [ -n "${BACKUP_COPY_TARGET:-}" ]; then
  case "$BACKUP_COPY_TARGET" in
    /*) mkdir -p "$BACKUP_COPY_TARGET" && cp -p "$file" "$file.count" "$file.sha256" "$BACKUP_COPY_TARGET/" ;;
    *)  scp -q "$file" "$file.count" "$file.sha256" "$BACKUP_COPY_TARGET/" ;;
  esac
  echo "copied to $BACKUP_COPY_TARGET"
fi
