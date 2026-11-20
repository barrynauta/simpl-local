#!/usr/bin/env bash
# The one entry point of the orchestrator for the Vocabulary Manager stack.
#   ./stack.sh up              start the stack (returns when usable; no-op when it already runs)
#   ./stack.sh down            stop and remove the stack (data volume kept)
#   ./stack.sh images          print every image of the stack, one per line
#   ./stack.sh store-down-up   start a second backend whose store is unreachable, API on port 8199 (D44)
#   ./stack.sh store-down-down remove that backend
set -euo pipefail
cd "$(dirname "$0")"
PROJECT=vm-factory-run9
COMPOSE=(docker compose -p "$PROJECT" --profile store-down)

ensure_env() {
  if [ ! -f .env ]; then
    umask 077
    {
      echo "STORE_READ_PASSWORD=$(openssl rand -hex 16)"
      echo "STORE_UPDATE_PASSWORD=$(openssl rand -hex 16)"
      echo "STORE_ADMIN_PASSWORD=$(openssl rand -hex 16)"
      echo "KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -hex 16)"
    } > .env
  fi
}

case "${1:-}" in
  up)
    ensure_env
    docker compose -p "$PROJECT" up -d --build --wait
    ;;
  down)
    "${COMPOSE[@]}" down --remove-orphans
    ;;
  images)
    ensure_env
    "${COMPOSE[@]}" config --images | sort -u
    ;;
  store-down-up)
    ensure_env
    "${COMPOSE[@]}" up -d --wait --no-deps backend-store-down
    ;;
  store-down-down)
    "${COMPOSE[@]}" rm -sf backend-store-down
    ;;
  *)
    echo "usage: $0 up|down|images|store-down-up|store-down-down" >&2
    exit 2
    ;;
esac
