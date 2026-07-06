#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Pass -v to also drop the MinIO volume (wipes transferred objects + DBs):
#   ./stop.sh -v
docker compose down "$@"
