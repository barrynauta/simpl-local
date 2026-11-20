#!/usr/bin/env bash
# The one entry point for the stack (build profile run 11, D131).
#   ./stack.sh up              start (creates local secrets in .env on first use), returns when usable
#   ./stack.sh down            stop (data volume kept)
#   ./stack.sh images          every image of the stack, one per line
#   ./stack.sh store-down-up   second backend with an unreachable store on port 8199 (D44)
#   ./stack.sh store-down-down remove it
set -euo pipefail
cd "$(dirname "$0")"
export COMPOSE_PROJECT_NAME=vm-factory-run11
COMPOSE=(docker compose -p vm-factory-run11 -f docker-compose.yml)

secret() { LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32; }

ensure_env() {
  if [[ ! -f .env ]]; then
    umask 077
    {
      echo "FUSEKI_READ_PASSWORD=$(secret)"
      echo "FUSEKI_UPDATE_PASSWORD=$(secret)"
      echo "FUSEKI_BACKUP_PASSWORD=$(secret)"
      echo "KEYCLOAK_ADMIN_PASSWORD=$(secret)"
    } > .env
    echo "Created local secrets in .env (never commit this file)."
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
    ensure_env >/dev/null
    "${COMPOSE[@]}" --profile storedown config --images | sort -u
    ;;
  store-down-up)
    ensure_env
    "${COMPOSE[@]}" --profile storedown up -d --wait --wait-timeout 300 --no-deps backend-storedown
    ;;
  store-down-down)
    "${COMPOSE[@]}" --profile storedown rm -s -f backend-storedown
    ;;
  *)
    echo "usage: $0 up|down|images|store-down-up|store-down-down" >&2
    exit 2
    ;;
esac
