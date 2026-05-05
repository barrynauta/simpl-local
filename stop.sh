#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

FULL=false
for arg in "$@"; do
  case "$arg" in
    --full) FULL=true ;;
  esac
done

if [ "$FULL" = true ]; then
  echo "Stopping containers and removing volumes..."
  docker compose down -v
  echo "Done. Re-run ./start.sh to rebuild from scratch."
else
  echo "Stopping containers (volumes preserved)..."
  docker compose down
  echo "Done. Re-run ./start.sh to restart."
fi
