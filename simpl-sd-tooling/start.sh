#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD=false
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=true ;;
  esac
done

# ── 1. Environment ──────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  cp .env.example .env
  echo ".env created from .env.example — edit ports/token if needed"
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

# ── 3. Clone upstream repos ─────────────────────────────────────────────────
clone_branch() {
  local url="$1" dir="$2" branch="$3" label="$4"
  if [ ! -d "$dir" ]; then
    echo "Cloning $label ($branch)..."
    git clone --branch "$branch" "$url" "$dir"
  else
    echo "Pulling latest $label..."
    git -C "$dir" pull --ff-only || echo "  (pull skipped — local changes or detached HEAD)"
  fi
}

clone_branch \
  "https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-api-be.git" \
  "repos/sdtooling-api-be" "main" "sdtooling-api-be"

clone_branch \
  "https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-validation-api-be.git" \
  "repos/sdtooling-validation-api-be" "main" "sdtooling-validation-api-be"

clone_branch \
  "https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-sd-ui.git" \
  "repos/simpl-sd-ui" "main" "simpl-sd-ui"

# ── 4. Seed the schema repository ────────────────────────────────────────────
# The api-be reads schemas as flat <Name>.ttl + <Name>.json pairs from
# SCHEMA_SYNC_SERVICE_REPOSITORY_PATH. Production fills that dir via the
# schema-sync-adapter (Postgres + Kafka + IAA); locally we copy the sample
# schemas shipped in the api-be repo itself. Re-copied on every start so
# upstream sample updates propagate; drop extra .ttl/.json pairs into
# ./schemas/ to add your own (they survive, the copy does not delete).
echo "Seeding schemas from repos/sdtooling-api-be/data/schemas..."
mkdir -p schemas
cp repos/sdtooling-api-be/data/schemas/*.ttl repos/sdtooling-api-be/data/schemas/*.json schemas/
echo "  $(ls schemas/*.json | wc -l | tr -d ' ') schema(s) in ./schemas"

# ── 4a. Generate the auth-provider keypair stub (throwaway self-signed) ─────
./gen-auth-stubs.sh

# ── 5. Build images ──────────────────────────────────────────────────────────
build_image() {
  local image="$1" dockerfile="$2" label="$3"; shift 3
  if [ -z "$(docker images -q "$image" 2>/dev/null)" ] || [ "$REBUILD" = true ]; then
    echo "Building $label..."
    if ! docker build \
        --build-arg GITLAB_TOKEN="${GITLAB_TOKEN:-}" \
        "$@" \
        -f "$SCRIPT_DIR/$dockerfile" \
        -t "$image" \
        "$SCRIPT_DIR/"; then
      echo "" >&2
      echo "$label build failed. Most common cause: an eu.europa.ec.simpl artifact" >&2
      echo "(or @simpl npm package) could not be resolved anonymously from the EU" >&2
      echo "GitLab registry. Fix: put a code.europa.eu PAT (read_api) in .env as" >&2
      echo "GITLAB_TOKEN= and re-run: ./start.sh --rebuild" >&2
      exit 1
    fi
    echo "$label built."
  else
    echo "$label image already exists, skipping (use --rebuild to force)."
  fi
}

build_image simpl-sdtooling-validation:local Dockerfile.local-validation "validation API (Maven stage on first run)"
build_image simpl-sdtooling-api:local        Dockerfile.local-api        "SD Tooling API (Maven stage on first run)"
# MOCK_APIS defaults to false; upstream treats ANY non-empty USE_MOCK_APIS as
# true, so the Dockerfile only exports it when the arg is exactly "true".
build_image simpl-sd-ui:local                Dockerfile.local-ui         "SD UI (npm install + astro build on first run)" \
  --build-arg MOCK_APIS=false \
  --build-arg MOCK_IDENTITY_ATTRIBUTES=false

# ── 6. Start services ────────────────────────────────────────────────────────
echo "Starting services..."
docker compose up -d

# ── 7. Health checks ─────────────────────────────────────────────────────────
wait_healthy() {
  local container="$1" timeout="${2:-120}" elapsed=0
  echo "Waiting for $container to report healthy..."
  until [ "$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || echo starting)" = "healthy" ]; do
    sleep 3
    elapsed=$((elapsed + 3))
    if [ "$elapsed" -ge "$timeout" ]; then
      echo "  (not healthy after ${timeout}s — check: docker logs $container)"
      return 1
    fi
  done
}
wait_healthy simpl-sd-tooling-validation 90 || true
wait_healthy simpl-sd-tooling-api 120 || true
wait_healthy simpl-sd-tooling-ui 90 || true

# ── 8. Smoke test ────────────────────────────────────────────────────────────
echo ""
echo "Smoke test:"
SCHEMAS_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
  "http://localhost:${SDTOOLING_API_PORT:-8087}/v2/schemas" 2>/dev/null || echo 000)
echo "  GET api /v2/schemas        → HTTP $SCHEMAS_HTTP  (expected 200 with 3 seeded schemas)"
VALIDATION_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
  "http://localhost:${VALIDATION_API_PORT:-8088}/status" 2>/dev/null || echo 000)
echo "  GET validation /status     → HTTP $VALIDATION_HTTP  (expected 200)"
UI_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
  "http://localhost:${UI_PORT:-4324}/status" 2>/dev/null || echo 000)
echo "  GET ui /status             → HTTP $UI_HTTP  (expected 200)"
STUBS_COUNT=$(curl -s "http://localhost:${STUBS_PORT:-8089}/__admin/mappings" 2>/dev/null \
  | grep -o '"total"[ ]*:[ ]*[0-9]*' | grep -o '[0-9]*' || echo "?")
echo "  WireMock mappings loaded   → $STUBS_COUNT"

# ── 9. Service URLs ──────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service               URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  SD UI (wizard)        http://localhost:${UI_PORT:-4324}"
echo "  SD Tooling API        http://localhost:${SDTOOLING_API_PORT:-8087}"
echo "  Schemas endpoint      http://localhost:${SDTOOLING_API_PORT:-8087}/v2/schemas"
echo "  Validation API        http://localhost:${VALIDATION_API_PORT:-8088}"
echo "  Peer stubs (WireMock) http://localhost:${STUBS_PORT:-8089}/__admin/mappings"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
