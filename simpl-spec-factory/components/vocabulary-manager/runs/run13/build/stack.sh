#!/usr/bin/env bash
# Entry point of the Vocabulary Manager stack (build profile run 13).
#   ./stack.sh up               start the stack; returns when it is usable; does nothing if it already runs
#   ./stack.sh down             stop the stack (data volume kept)
#   ./stack.sh images           print every image of the stack, one per line
#   ./stack.sh store-down-up    start a second backend with an unreachable store on port 8199 (D44)
#   ./stack.sh store-down-down  remove that backend
set -euo pipefail
cd "$(dirname "$0")"

COMPOSE=(docker compose -p vm-factory-run13 -f compose.yaml)

ensure_env() {
  # Local secrets are generated once and never committed (D131).
  if [[ ! -f .env ]]; then
    umask 077
    {
      echo "FUSEKI_READ_PASSWORD=$(openssl rand -hex 16)"
      echo "FUSEKI_UPDATE_PASSWORD=$(openssl rand -hex 16)"
      echo "FUSEKI_ADMIN_PASSWORD=$(openssl rand -hex 16)"
      echo "KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -hex 16)"
    } > .env
  fi
}

all_healthy() {
  local ids status
  ids=$("${COMPOSE[@]}" ps -q fuseki keycloak backend proxy 2>/dev/null || true)
  [[ -n "$ids" ]] || return 1
  [[ $(echo "$ids" | wc -l) -ge 5 ]] || return 1
  for id in $ids; do
    status=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$id")
    [[ "$status" == "healthy" ]] || return 1
  done
}

case "${1:-}" in
  up)
    ensure_env
    if all_healthy; then
      echo "Stack already running."
      exit 0
    fi
    "${COMPOSE[@]}" up -d --build --wait --wait-timeout 600
    ;;
  down)
    "${COMPOSE[@]}" --profile store-down down --remove-orphans
    ;;
  images)
    ensure_env
    "${COMPOSE[@]}" --profile store-down config --images | sort -u
    ;;
  store-down-up)
    ensure_env
    "${COMPOSE[@]}" --profile store-down up -d --wait --wait-timeout 300 --no-deps backend-store-down
    ;;
  store-down-down)
    "${COMPOSE[@]}" --profile store-down rm -s -f backend-store-down
    ;;
  *)
    echo "usage: $0 {up|down|images|store-down-up|store-down-down}" >&2
    exit 2
    ;;
esac
