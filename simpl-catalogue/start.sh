#!/usr/bin/env bash
# simpl-catalogue-local — bring the catalogue stack up with one command.
#
# Everything (clone all three source repos, build, Neo4j n10s init) now happens
# inside `docker compose` via git build contexts and a one-shot init service,
# so this is a thin wrapper: it runs compose, waits for health, then prints a
# smoke test and the service URLs. Equivalent raw command:
#   docker compose up -d --build --wait
#
# Usage:
#   ./start.sh                Build + up + wait, then print the links.
#   ./start.sh --run-tests    Also run the Bruno smoke tests (stack stays up).
#   ./start.sh --help         Show this help.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RUN_TESTS=false
for arg in "$@"; do
  case "$arg" in
    --run-tests) RUN_TESTS=true ;;
    --help|-h) grep '^#' "$0" | grep -v '#!/' | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown argument: $arg (use --help)" >&2; exit 2 ;;
  esac
done

[ -f .env ] && { set -a; . ./.env; set +a; }
FC_SERVICE_PORT="${FC_SERVICE_PORT:-8081}"; QMA_PORT="${QMA_PORT:-8084}"; UI_PORT="${UI_PORT:-4321}"
NEO4J_HTTP_PORT="${NEO4J_HTTP_PORT:-7474}"; POSTGRES_PORT="${POSTGRES_PORT:-5432}"
NEO4J_USER="${NEO4J_USER:-neo4j}"; NEO4J_PASSWORD="${NEO4J_PASSWORD:-neo12345}"

echo "Starting catalogue stack (build + up + wait for healthy)..."
echo "First run builds three source repos with Maven/npm and can take 10-20 min."
docker compose up -d --build --wait

echo ""
echo "Smoke test:"
fc=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${FC_SERVICE_PORT}/self-descriptions" 2>/dev/null || echo 000)
echo "  GET fc-service /self-descriptions   HTTP $fc  (expect 200)"
sch=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${FC_SERVICE_PORT}/schemas" 2>/dev/null || echo 000)
echo "  GET fc-service /schemas             HTTP $sch  (expect 200)"
ui=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${UI_PORT}/" 2>/dev/null || echo 000)
echo "  GET ui /                            HTTP $ui  (expect 200)"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service          URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Catalogue UI     http://localhost:${UI_PORT}"
echo "  fc-service       http://localhost:${FC_SERVICE_PORT}/self-descriptions"
echo "  query-mapper     http://localhost:${QMA_PORT}/v1"
echo "  Neo4j Browser    http://localhost:${NEO4J_HTTP_PORT}  (login: ${NEO4J_USER} / ${NEO4J_PASSWORD})"
echo "  Postgres         localhost:${POSTGRES_PORT}  (db: fed_cat)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Live logs:  docker compose logs -f        Stop:  docker compose down"
echo ""

if [ "$RUN_TESTS" = true ]; then
  echo "==> Seeding catalogue (idempotent) then running Bruno smoke tests"
  [ -x ./seed.sh ] && { ./seed.sh || echo "  (seed reported a problem; tests may fail)"; }
  set +e
  docker compose --profile tests run --rm bruno-smoke-test
  BRUNO_EXIT=$?
  set -e
  echo ""
  [ "$BRUNO_EXIT" = "0" ] && echo "  All smoke tests passed." || echo "  One or more smoke tests failed (exit $BRUNO_EXIT)."
  echo "  Stack remains running. Stop: docker compose down"
  exit "$BRUNO_EXIT"
fi
