#!/usr/bin/env bash
# Single entry point for the orchestrator. Usage: ./stack.sh up|down|images|store-down-up|store-down-down
set -euo pipefail
cd "$(dirname "$0")"
PROJECT=vm-factory-run6
COMPOSE=(docker compose -p "$PROJECT" -f compose.yaml)

case "${1:-}" in
  up)
    # Idempotent: compose leaves running, unchanged services alone and waits for health.
    "${COMPOSE[@]}" up -d --build --wait
    ;;
  down)
    "${COMPOSE[@]}" --profile store-down down --remove-orphans
    ;;
  images)
    "${COMPOSE[@]}" --profile store-down config --images | sort -u
    ;;
  store-down-up)
    "${COMPOSE[@]}" --profile store-down up -d --wait backend-store-down
    ;;
  store-down-down)
    "${COMPOSE[@]}" --profile store-down rm -s -f backend-store-down
    ;;
  *)
    echo "usage: $0 up|down|images|store-down-up|store-down-down" >&2
    exit 2
    ;;
esac
