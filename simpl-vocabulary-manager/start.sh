#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD=false
SEED=false
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=true ;;
    --seed) SEED=true ;;
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

# ── 3. Clone / pull upstream repos ──────────────────────────────────────────
clone_or_pull() {
  local url="$1" dir="$2" label="$3" branch="${4:-}"
  if [ ! -d "$dir" ]; then
    echo "Cloning $label..."
    if [ -n "$branch" ]; then
      git clone -b "$branch" "$url" "$dir"
    else
      git clone "$url" "$dir"
    fi
  else
    echo "Pulling latest $label..."
    git -C "$dir" pull --ff-only || echo "  (pull skipped — local changes or detached HEAD)"
  fi
}

clone_or_pull \
  "https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-vocabulary-manager.git" \
  "repos/simpl-vocabulary-manager" \
  "simpl-vocabulary-manager"

# UI: main is an empty stub upstream — the actual app lives on release-1.0.0
# (= develop + security dependency bumps, 2026-06-12). Pin that branch.
clone_or_pull \
  "https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-vocabulary-manager-ui.git" \
  "repos/simpl-vocabulary-manager-ui" \
  "simpl-vocabulary-manager-ui" \
  "release-1.0.0"

# ── 4. Build Docker image ───────────────────────────────────────────────────
IMAGE_EXISTS=$(docker images -q simpl-vocabulary-manager:local 2>/dev/null || true)
if [ -z "$IMAGE_EXISTS" ] || [ "$REBUILD" = true ]; then
  echo "Building Docker image (Stage 1: Maven — first run ~5 min for dep download, Stage 2: runtime)..."
  echo "(simpl-semantic-validation-sdk is pulled anonymously from the public code.europa.eu Maven registry)"
  docker build \
    -f "$SCRIPT_DIR/Dockerfile.local" \
    -t simpl-vocabulary-manager:local \
    "$SCRIPT_DIR/"
  echo "Docker image built."
else
  echo "Docker image already exists, skipping build (use --rebuild to force)."
fi

UI_IMAGE_EXISTS=$(docker images -q simpl-vocabulary-manager-ui:local 2>/dev/null || true)
if [ -z "$UI_IMAGE_EXISTS" ] || [ "$REBUILD" = true ]; then
  echo "Building UI Docker image (Stage 1: Vite — first run ~3 min for npm install, Stage 2: nginx)..."
  docker build \
    -f "$SCRIPT_DIR/Dockerfile.local-ui" \
    -t simpl-vocabulary-manager-ui:local \
    "$SCRIPT_DIR/"
  echo "UI Docker image built."
else
  echo "UI Docker image already exists, skipping build (use --rebuild to force)."
fi

# ── 5. Start Fuseki first, wait, then the service ───────────────────────────
# Two-phase start: the app contacts Fuseki at boot to ensure its datasets, so
# we gate on Fuseki readiness from the host instead of relying on in-container
# healthcheck tooling.
echo "Starting Fuseki..."
docker compose up -d fuseki

echo "Waiting for Fuseki..."
ELAPSED=0; TIMEOUT=60
until curl -sf "http://localhost:${FUSEKI_PORT:-3031}/" >/dev/null 2>&1; do
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "ERROR: Fuseki did not become ready in ${TIMEOUT}s" >&2
    exit 1
  fi
done
echo "Fuseki ready."

echo "Starting vocabulary-manager + UI..."
docker compose up -d vocabulary-manager vocabulary-manager-ui

echo "Waiting for vocabulary-manager to start..."
ELAPSED=0; TIMEOUT=90
until docker logs simpl-vocabulary-manager 2>&1 | grep -q "Started SimplVocabularyManagerApplication"; do
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "  (startup log not detected after ${TIMEOUT}s — check: docker logs simpl-vocabulary-manager)"
    break
  fi
done

# ── 6. Optional demo data ────────────────────────────────────────────────────
if [ "$SEED" = true ]; then
  echo ""
  echo "Loading upstream demo vocabularies into Fuseki..."
  FUSEKI_BASE_URL="http://localhost:${FUSEKI_PORT:-3031}" \
  ADMIN_USERNAME="${FUSEKI_ADMIN_USER:-admin}" \
  ADMIN_PASSWORD="${FUSEKI_ADMIN_PASSWORD:-admin1234}" \
    bash repos/simpl-vocabulary-manager/scripts/load-fuseki-seed-data.sh \
    || echo "  (seed script reported a problem — inspect output above)"
fi

# ── 7. Smoke test ────────────────────────────────────────────────────────────
echo ""
echo "Smoke test:"
HEALTH=$(curl -s "http://localhost:${VOCABULARY_MANAGER_PORT:-8086}/health" || echo "unreachable")
echo "  GET /health        → $HEALTH   (expected {\"status\":\"UP\"})"

VOCAB_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${VOCABULARY_MANAGER_PORT:-8086}/vocabularies")
echo "  GET /vocabularies  → HTTP $VOCAB_HTTP  (expected 200)"

DATASETS_JSON=$(curl -sf -u "${FUSEKI_ADMIN_USER:-admin}:${FUSEKI_ADMIN_PASSWORD:-admin1234}" \
  "http://localhost:${FUSEKI_PORT:-3031}/\$/datasets" 2>/dev/null || echo '{}')
FOUND=0
for name in ds_vocabularies ds_external_vocabularies; do
  grep -q "\"/$name\"" <<<"$DATASETS_JSON" && FOUND=$((FOUND + 1))
done
echo "  Fuseki datasets    → $FOUND / 2 expected (ds_vocabularies, ds_external_vocabularies)"

UI_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${UI_PORT:-4323}/")
echo "  GET UI /           → HTTP $UI_HTTP  (expected 200)"
UI_PROXY_HTTP=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${UI_PORT:-4323}/api/vocabularies")
echo "  GET UI /api proxy  → HTTP $UI_PROXY_HTTP  (expected 200 — nginx → backend)"

# ── 8. Service URLs ──────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service                 URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Vocabulary Manager UI   http://localhost:${UI_PORT:-4323}   (Keycloak bypassed — see README)"
echo "  Vocabulary Manager API  http://localhost:${VOCABULARY_MANAGER_PORT:-8086}"
echo "  Fuseki triplestore      http://localhost:${FUSEKI_PORT:-3031}  (${FUSEKI_ADMIN_USER:-admin} / ${FUSEKI_ADMIN_PASSWORD:-admin1234})"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Uploads need a Bearer token with an 'email' claim (decoded, never"
echo "signature-verified — see README). Ready-to-use local token:"
echo "  export VOCAB_TOKEN='eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImxvY2FsQHNpbXBsLmxvY2FsIn0.devsignature'"
