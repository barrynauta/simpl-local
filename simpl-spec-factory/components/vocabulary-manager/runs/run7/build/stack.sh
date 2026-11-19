#!/usr/bin/env bash
# Single entry point of the Vocabulary Manager stack for the orchestrator.
#   ./stack.sh up              start the stack (returns when usable; no-op when it already runs)
#   ./stack.sh down            stop the stack (data volume kept)
#   ./stack.sh images          print every image of the stack, one per line
#   ./stack.sh store-down-up   start a second backend whose store is unreachable, API on port 8199 (D44)
#   ./stack.sh store-down-down remove that backend
set -euo pipefail
cd "$(dirname "$0")"
PROJECT=vm-factory-run7
COMPOSE=(docker compose -p "$PROJECT" -f compose.yaml)

case "${1:-}" in
  up)
    "${COMPOSE[@]}" up -d --build --wait
    ;;
  down)
    "${COMPOSE[@]}" --profile storedown down --remove-orphans
    ;;
  images)
    "${COMPOSE[@]}" --profile storedown config --images | sort -u
    ;;
  store-down-up)
    "${COMPOSE[@]}" up -d --build --wait backend
    "${COMPOSE[@]}" --profile storedown up -d --no-deps backend-store-down
    for _ in $(seq 1 60); do
      code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8199/v1/health || true)
      if [ "$code" = "503" ]; then
        echo "store-down backend ready on http://localhost:8199/v1"
        exit 0
      fi
      sleep 2
    done
    echo "store-down backend did not become ready" >&2
    exit 1
    ;;
  store-down-down)
    "${COMPOSE[@]}" --profile storedown rm -s -f backend-store-down
    ;;
  *)
    echo "usage: $0 up|down|images|store-down-up|store-down-down" >&2
    exit 2
    ;;
esac
