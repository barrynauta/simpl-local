#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD=false
SEED=false
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=true ;;
    --seed)    SEED=true ;;
  esac
done

# ── 1. Environment ──────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  cp .env.example .env
  echo ".env created from .env.example — edit ports/token/keycloak if needed"
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
# contract        → default branch (main); substantive backend code.
# contract-ui     → develop branch; the SPA shell lives there, not on main.
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
  "https://code.europa.eu/simpl/simpl-open/development/contract-billing/contract.git" \
  "repos/contract" "main" "contract (backend)"

clone_branch \
  "https://code.europa.eu/simpl/simpl-open/development/contract-billing/contract-ui.git" \
  "repos/contract-ui" "develop" "contract-ui (frontend shell)"

# ── 4. Build backend image ──────────────────────────────────────────────────
if [ -z "$(docker images -q simpl-contract:local 2>/dev/null)" ] || [ "$REBUILD" = true ]; then
  echo "Building backend image (Stage 1: Maven — first run downloads deps incl. simpl-common:3.3.0)..."
  if ! docker build \
      --build-arg GITLAB_TOKEN="${GITLAB_TOKEN:-}" \
      -f "$SCRIPT_DIR/Dockerfile.local" \
      -t simpl-contract:local \
      "$SCRIPT_DIR/"; then
    echo "" >&2
    echo "Backend build failed. The most common cause is that simpl-common:3.3.0" >&2
    echo "could not be resolved anonymously from the EU GitLab registry." >&2
    echo "Fix: put a code.europa.eu PAT (read_api) in .env as GITLAB_TOKEN= and re-run:" >&2
    echo "  ./start.sh --rebuild" >&2
    exit 1
  fi
  echo "Backend image built."
else
  echo "Backend image already exists, skipping (use --rebuild to force)."
fi

# ── 5. Build UI image ───────────────────────────────────────────────────────
if [ -z "$(docker images -q simpl-contract-ui:local 2>/dev/null)" ] || [ "$REBUILD" = true ]; then
  echo "Building UI image (Stage 1: Vite — first run installs npm deps)..."
  docker build \
    --build-arg VITE_PUBLIC_AUTH_MODE="${VITE_PUBLIC_AUTH_MODE:-keycloak}" \
    --build-arg VITE_PUBLIC_AUTH_KEYCLOAK_SERVER_URL="${VITE_PUBLIC_AUTH_KEYCLOAK_SERVER_URL:-}" \
    --build-arg VITE_PUBLIC_AUTH_KEYCLOAK_REALM="${VITE_PUBLIC_AUTH_KEYCLOAK_REALM:-}" \
    --build-arg VITE_PUBLIC_AUTH_KEYCLOAK_CLIENT_ID="${VITE_PUBLIC_AUTH_KEYCLOAK_CLIENT_ID:-}" \
    -f "$SCRIPT_DIR/Dockerfile.local-ui" \
    -t simpl-contract-ui:local \
    "$SCRIPT_DIR/"
  echo "UI image built."
else
  echo "UI image already exists, skipping (use --rebuild to force)."
fi

# ── 6. Start services ────────────────────────────────────────────────────────
echo "Starting services..."
docker compose up -d

# ── 7. Health check (backend) ───────────────────────────────────────────────
echo "Waiting for the contract service to report healthy..."
ELAPSED=0; TIMEOUT=120
until [ "$(docker inspect -f '{{.State.Health.Status}}' simpl-contract 2>/dev/null || echo starting)" = "healthy" ]; do
  sleep 3
  ELAPSED=$((ELAPSED + 3))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "  (not healthy after ${TIMEOUT}s — check: docker logs simpl-contract)"
    break
  fi
done

# ── 8. Smoke test ────────────────────────────────────────────────────────────
echo ""
echo "Smoke test:"
HEALTH_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
  "http://localhost:${CONTRACT_API_PORT:-8086}/contract/v1/health" 2>/dev/null || echo 000)
echo "  GET /contract/v1/health → HTTP $HEALTH_HTTP  (expected 200, status UP)"

# ── 8a. Seed sample data (opt-in: --seed) ───────────────────────────────────
if [ "$SEED" = true ]; then
  echo "  Seeding sample contract agreement..."
  docker exec -i simpl-contract-postgres psql -U contract -d contract < samples/seed.sql \
    && echo "  Seeded agreement 11111111-1111-1111-1111-111111111111" \
    || echo "  (seed failed — is the stack healthy?)"
fi

# ── 9. Service URLs ──────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service              URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Contract UI (shell)  http://localhost:${UI_PORT:-4323}   (non-integrated — see README)"
echo "  Contract API         http://localhost:${CONTRACT_API_PORT:-8086}"
echo "  Health               http://localhost:${CONTRACT_API_PORT:-8086}/contract/v1/health"
echo "  Postgres             localhost:${POSTGRES_PORT:-5433}  (contract / contract)"
echo "  Kafka broker         localhost:${KAFKA_HOST_PORT:-9095}"
echo "  Kafka UI             http://localhost:${KAFKA_UI_PORT:-9002}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  OpenAPI spec: repos/contract/openapi/openapi3-v1.yaml"
