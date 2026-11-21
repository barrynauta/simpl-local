#!/usr/bin/env bash
# One entry point for the stack (build profile, decision D131).
#   ./stack.sh up              start (builds; returns when healthy; no-op when already running)
#   ./stack.sh down            stop and remove the containers (volumes are kept)
#   ./stack.sh images          print every image of the stack, one per line
#   ./stack.sh store-down-up   start a backend with an unreachable store on port 8199 (D44)
#   ./stack.sh store-down-down remove that backend
set -euo pipefail
cd "$(dirname "$0")"

PROJECT=vm-factory-run12
COMPOSE=(docker compose -p "$PROJECT" -f docker-compose.yml)

secret() { LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32; }

ensure_env() {
  if [[ ! -f .env ]]; then
    umask 077
    {
      echo "# Local secrets of the stack, created by stack.sh. Never commit this file."
      echo "FUSEKI_READ_PASSWORD=$(secret)"
      echo "FUSEKI_UPDATE_PASSWORD=$(secret)"
      echo "FUSEKI_ADMIN_PASSWORD=$(secret)"
      echo "KEYCLOAK_ADMIN_PASSWORD=$(secret)"
    } > .env
  fi
}

case "${1:-}" in
  up)
    ensure_env
    "${COMPOSE[@]}" up -d --build --wait --wait-timeout 600
    ;;
  down)
    "${COMPOSE[@]}" --profile storedown down --remove-orphans
    ;;
  images)
    ensure_env
    "${COMPOSE[@]}" --profile storedown config --images | sort -u
    ;;
  store-down-up)
    ensure_env
    "${COMPOSE[@]}" --profile storedown up -d --no-deps --wait --wait-timeout 300 backend-store-down
    ;;
  store-down-down)
    "${COMPOSE[@]}" --profile storedown rm -s -f backend-store-down
    ;;
  *)
    echo "usage: $0 {up|down|images|store-down-up|store-down-down}" >&2
    exit 2
    ;;
esac
