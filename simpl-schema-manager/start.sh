#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD=false
RUN_TESTS=false
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=true ;;
    --run-tests) RUN_TESTS=true ;;
  esac
done

# ── 1. Environment ──────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  cp .env.example .env
  echo ".env created from .env.example — edit ports/credentials if needed"
fi
# shellcheck disable=SC1091
source .env 2>/dev/null || true

# ── 2. Prerequisites ─────────────────────────────────────────────────────────
for cmd in docker git curl; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' not found. See README prerequisites." >&2
    exit 1
  fi
done

# ── 3. Clone / pull upstream repo ───────────────────────────────────────────
clone_or_pull() {
  local url="$1" dir="$2" label="$3"
  if [ ! -d "$dir" ]; then
    echo "Cloning $label..."
    git clone "$url" "$dir"
  else
    echo "Pulling latest $label..."
    git -C "$dir" pull --ff-only || echo "  (pull skipped — local changes or detached HEAD)"
  fi
}

clone_or_pull \
  "https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager.git" \
  "repos/simpl-schema-manager" \
  "simpl-schema-manager"

clone_or_pull \
  "https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-schema-manager-ui.git" \
  "repos/simpl-schema-manager-ui" \
  "simpl-schema-manager-ui"

# ── 4. Build Docker image ───────────────────────────────────────────────────
IMAGE_EXISTS=$(docker images -q simpl-schema-manager:local 2>/dev/null || true)
if [ -z "$IMAGE_EXISTS" ] || [ "$REBUILD" = true ]; then
  echo "Building backend Docker image (Stage 1: Maven — first run ~5 min for dep download, Stage 2: runtime)..."
  docker build \
    -f "$SCRIPT_DIR/Dockerfile.local" \
    -t simpl-schema-manager:local \
    "$SCRIPT_DIR/"
  echo "Backend Docker image built."
else
  echo "Backend Docker image already exists, skipping build (use --rebuild to force)."
fi

UI_IMAGE_EXISTS=$(docker images -q simpl-schema-manager-ui:local 2>/dev/null || true)
if [ -z "$UI_IMAGE_EXISTS" ] || [ "$REBUILD" = true ]; then
  echo "Building UI Docker image (Stage 1: Vite — first run ~3 min for npm install, Stage 2: nginx)..."
  docker build \
    -f "$SCRIPT_DIR/Dockerfile.local-ui" \
    -t simpl-schema-manager-ui:local \
    "$SCRIPT_DIR/"
  echo "UI Docker image built."
else
  echo "UI Docker image already exists, skipping build (use --rebuild to force)."
fi

# ── 5. Start services ────────────────────────────────────────────────────────
echo "Starting services..."
docker compose up -d

# ── 6. Health checks ────────────────────────────────────────────────────────
echo "Waiting for Fuseki..."
ELAPSED=0; TIMEOUT=60
until curl -sf "http://localhost:${FUSEKI_PORT:-3030}/" >/dev/null 2>&1; do
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "ERROR: Fuseki did not become ready in ${TIMEOUT}s" >&2
    exit 1
  fi
done
echo "Fuseki ready."

echo "Waiting for schema-manager to start..."
ELAPSED=0; TIMEOUT=90
until docker logs simpl-schema-manager 2>&1 | grep -q "Started SimplSchemaManagerApplication"; do
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "  (startup log not detected after ${TIMEOUT}s — check: docker logs simpl-schema-manager)"
    break
  fi
done

# ── 7. Smoke test ────────────────────────────────────────────────────────────
echo ""
echo "Smoke test:"
DATASETS_JSON=$(curl -sf -u "${FUSEKI_ADMIN_USER:-admin}:${FUSEKI_ADMIN_PASSWORD:-admin1234}" \
  "http://localhost:${FUSEKI_PORT:-3030}/\$/datasets" 2>/dev/null || echo '{}')
FOUND=0
for name in ds_schemas ds_schema_metadata ds_schema_categories ds_webhooks; do
  grep -q "\"/$name\"" <<<"$DATASETS_JSON" && FOUND=$((FOUND + 1))
done
echo "  Fuseki datasets present: $FOUND / 4 expected (ds_schemas, ds_schema_metadata, ds_schema_categories, ds_webhooks)"

WEBHOOKS_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${SCHEMA_MANAGER_PORT:-8085}/webhooks")
echo "  GET /webhooks → HTTP $WEBHOOKS_HTTP  (expected 200, body []  — unauthenticated liveness probe)"

# ── 8. Service URLs ──────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service                URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Schema Manager UI      http://localhost:${UI_PORT:-4322}   (Keycloak bypassed — see README)"
echo "  Schema Manager API     http://localhost:${SCHEMA_MANAGER_PORT:-8085}"
echo "  Fuseki triplestore     http://localhost:${FUSEKI_PORT:-3030}  (${FUSEKI_ADMIN_USER:-admin} / ${FUSEKI_ADMIN_PASSWORD:-admin1234})"
echo "  Kafka broker           localhost:${KAFKA_HOST_PORT:-9094}"
echo "  Kafka UI               http://localhost:${KAFKA_UI_PORT:-9001}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ── 9. Bruno smoke tests (opt-in) ───────────────────────────────────────────
if [ "$RUN_TESTS" = true ]; then
  echo ""
  echo "==> Running Bruno smoke tests (inside docker network)"
  echo ""
  # `run --rm` creates a fresh, ephemeral container against the running stack
  # network and cleans it up on exit. Same pattern as simpl-catalogue.
  set +e
  docker compose --profile tests run --rm bruno-smoke-test
  BRUNO_EXIT=$?
  set -e
  exit $BRUNO_EXIT
fi
