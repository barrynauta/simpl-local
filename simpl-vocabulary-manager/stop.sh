#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Pass -v to also remove the Fuseki volumes (wipes all stored vocabularies):
#   ./stop.sh -v
docker compose down "$@"
