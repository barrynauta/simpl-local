#!/usr/bin/env bash
# simpl-vocabulary-manager — bring the stack up with one command.
#
# Cloning both source repos (backend main, UI v1.1.0), building, and the
# Fuseki readiness wait now all happen inside docker compose (git build
# contexts + a one-shot fuseki-wait service). This is a thin wrapper: it runs
# compose, waits for health, then prints a smoke test and the URLs. Raw form:
#   docker compose up -d --build --wait
#
# Usage:
#   ./start.sh            Build + up + wait, then print the links.
#   ./start.sh --seed     Also load the upstream demo vocabularies into Fuseki.
#   ./start.sh --help     Show this help.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"; cd "$SCRIPT_DIR"

SEED=false
for arg in "$@"; do
  case "$arg" in
    --seed) SEED=true ;;
    --help|-h) grep '^#' "$0" | grep -v '#!/' | sed 's/^# \{0,1\}//'; exit 0 ;;
  esac
done

[ -f .env ] || { cp .env.example .env; echo ".env created from .env.example"; }
# shellcheck disable=SC1091
source .env 2>/dev/null || true
VOCABULARY_MANAGER_PORT="${VOCABULARY_MANAGER_PORT:-8086}"; FUSEKI_PORT="${FUSEKI_PORT:-3031}"; UI_PORT="${UI_PORT:-4323}"
FUSEKI_ADMIN_USER="${FUSEKI_ADMIN_USER:-admin}"; FUSEKI_ADMIN_PASSWORD="${FUSEKI_ADMIN_PASSWORD:-admin1234}"

echo "Starting vocabulary-manager stack (build + up + wait for healthy)..."
echo "First run builds two source repos with Maven/npm and can take ~10 min."
docker compose up -d --build --wait

if [ "$SEED" = true ]; then
  echo ""
  echo "Loading upstream demo vocabularies into Fuseki (seed profile)..."
  docker compose --profile seed run --rm seed || echo "  (seed reported a problem; inspect output above)"
fi

echo ""
echo "Smoke test:"
h=$(curl -s "http://localhost:${VOCABULARY_MANAGER_PORT}/v1/health" 2>/dev/null || echo unreachable)
echo "  GET /health        $h   (expect {\"status\":\"UP\"})"
v=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${VOCABULARY_MANAGER_PORT}/v1/vocabularies" 2>/dev/null || echo 000)
echo "  GET /vocabularies  HTTP $v  (expect 200)"
u=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${UI_PORT}/" 2>/dev/null || echo 000)
echo "  GET UI /           HTTP $u  (expect 200)"
up=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${UI_PORT}/api/vocabularies" 2>/dev/null || echo 000)
echo "  GET UI /api proxy  HTTP $up  (expect 200, nginx -> backend)"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service                 URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Vocabulary Manager UI   http://localhost:${UI_PORT}   (Keycloak bypassed)"
echo "  Vocabulary Manager API  http://localhost:${VOCABULARY_MANAGER_PORT}"
echo "  Fuseki triplestore      http://localhost:${FUSEKI_PORT}  (${FUSEKI_ADMIN_USER} / ${FUSEKI_ADMIN_PASSWORD})"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Live logs:  docker compose logs -f        Stop:  docker compose down"
echo ""
echo "  Uploads need a Bearer token with an 'email' claim (decoded, never"
echo "  signature-verified). Ready-to-use local token:"
echo "    export VOCAB_TOKEN='eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImxvY2FsQHNpbXBsLmxvY2FsIn0.devsignature'"
echo ""
