#!/usr/bin/env bash
# Bring the SD Tooling stack up with a single command, then print the links.
#
# Everything (clone, build, schema seed, auth keypair stub) now happens inside
# `docker compose` via git build contexts and a one-shot init service, so this
# is just a thin wrapper: it runs compose detached, waits for health, and then
# prints a smoke test and the service URLs. You can equally run
#   docker compose up -d --build --wait
# yourself; this only adds the banner. Live logs: docker compose logs -f
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

[ -f .env ] || { cp .env.example .env; echo ".env created from .env.example"; }
# shellcheck disable=SC1091
source .env 2>/dev/null || true

echo "Starting stack (build + up + wait for healthy)..."
docker compose up -d --build --wait

echo ""
echo "Smoke test:"
api=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${SDTOOLING_API_PORT:-8087}/v2/schemas" 2>/dev/null || echo 000)
echo "  GET api /v2/schemas      HTTP $api  (expect 200, 3 schemas)"
val=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${VALIDATION_API_PORT:-8088}/status" 2>/dev/null || echo 000)
echo "  GET validation /status   HTTP $val  (expect 200)"
ui=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${UI_PORT:-4324}/status" 2>/dev/null || echo 000)
echo "  GET ui /status           HTTP $ui  (expect 200)"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service                 URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  SD UI (wizard)          http://localhost:${UI_PORT:-4324}"
echo "  SD Tooling API (Swagger) http://localhost:${SDTOOLING_API_PORT:-8087}/swagger-ui/index.html"
echo "  Schemas endpoint        http://localhost:${SDTOOLING_API_PORT:-8087}/v2/schemas"
echo "  Validation API /status  http://localhost:${VALIDATION_API_PORT:-8088}/status"
echo "  Peer stubs (WireMock)   http://localhost:${STUBS_PORT:-8089}/__admin/mappings"
echo ""
echo "  (The API bare root / returns a 404 by design - it is a REST backend,"
echo "   not a web page. Browse the UI or the Swagger link above.)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  Live logs:  docker compose logs -f        Stop:  docker compose down"
echo ""
