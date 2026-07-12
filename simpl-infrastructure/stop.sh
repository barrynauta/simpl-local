#!/usr/bin/env bash
# Stop the stack. Pass --clean to also drop the postgres volume.
set -euo pipefail
cd "$(dirname "$0")"
if [ "${1:-}" = "--clean" ]; then
  docker compose down -v
else
  docker compose down
fi
