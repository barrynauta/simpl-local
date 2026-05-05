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
  echo ".env created from .env.example — edit DEFAULT_EMAIL_RECEIVER if needed"
fi
# shellcheck disable=SC1091
source .env 2>/dev/null || true

# ── 2. Prerequisites ─────────────────────────────────────────────────────────
for cmd in docker git; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' not found. See README prerequisites." >&2
    exit 1
  fi
done

# ── 3. Clone / pull upstream repos ──────────────────────────────────────────
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
  "https://code.europa.eu/simpl/simpl-open/development/contract-billing/notification-service.git" \
  "repos/notification-service" \
  "notification-service"

clone_or_pull \
  "https://code.europa.eu/simpl/simpl-open/development/contract-billing/common_logging.git" \
  "repos/common_logging" \
  "common_logging"

clone_or_pull \
  "https://code.europa.eu/simpl/simpl-open/development/contract-billing/common.git" \
  "repos/common" \
  "common"

# ── 5. Build Docker image ────────────────────────────────────────────────────
# Dockerfile.local is a multi-stage build:
#   Stage 1 — Maven: COMMON_LOGGING:2.4.0 → COMMON_LOGGING:2.0.1 → COMMON:2.5.1 → notification-service
#   Stage 2 — Runtime: mirrors upstream Dockerfile
# Build context is $SCRIPT_DIR (local stack root) so all repos/ are accessible.
IMAGE_EXISTS=$(docker images -q simpl-notification-service:local 2>/dev/null || true)
if [ -z "$IMAGE_EXISTS" ] || [ "$REBUILD" = true ]; then
  echo "Building Docker image (Stage 1: Maven ~10 min first run, Stage 2: runtime)..."
  docker build \
    -f "$SCRIPT_DIR/Dockerfile.local" \
    -t simpl-notification-service:local \
    "$SCRIPT_DIR/"
  echo "Docker image built."
else
  echo "Docker image already exists, skipping build (use --rebuild to force)."
fi

# ── 6. Start services ────────────────────────────────────────────────────────
echo "Starting services (with test overlay for Mailpit email capture)..."
docker compose -f docker-compose.yml -f docker-compose.test.yml up -d

# ── 7. Health checks ────────────────────────────────────────────────────────
echo "Waiting for Kafka..."
TIMEOUT=90
ELAPSED=0
until docker exec simpl-kafka kafka-broker-api-versions --bootstrap-server kafka:9093 &>/dev/null; do
  sleep 3
  ELAPSED=$((ELAPSED + 3))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "ERROR: Kafka did not become ready in ${TIMEOUT}s" >&2
    exit 1
  fi
done
echo "Kafka ready."

echo "Waiting for notification-service to start..."
ELAPSED=0
TIMEOUT=60
until docker logs simpl-notification-service 2>&1 | grep -q "Started\|started\|Listening\|listening"; do
  sleep 3
  ELAPSED=$((ELAPSED + 3))
  if [ $ELAPSED -ge $TIMEOUT ]; then
    echo "  (startup log not detected after ${TIMEOUT}s — check: docker logs simpl-notification-service)"
    break
  fi
done

if curl -sf http://localhost:"${NOTIFICATION_SERVICE_PORT:-8081}"/health >/dev/null 2>&1; then
  echo "Notification-service health endpoint OK."
else
  echo "  (/health not responding — service may still be starting or actuator is not on classpath)"
fi

# ── 8. Seed: send a test notification ────────────────────────────────────────
echo ""
echo "Publishing a test notification to Kafka topic 'notifications'..."
docker exec -i simpl-kafka kafka-console-producer \
  --broker-list kafka:9093 \
  --topic notifications <<< \
  '{"channel":"email","to":"test@example.com","subject":"Test from simpl-notification-service-local","message":"Hello from start.sh — if you see this in Mailpit, the stack is working."}'
echo "Test message sent. Check Mailpit at http://localhost:${MAILPIT_UI_PORT:-8025}"

# ── 9. Service URLs ──────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service                 URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Mailpit Web UI          http://localhost:${MAILPIT_UI_PORT:-8025}"
echo "  Kafka UI                http://localhost:${KAFKA_UI_PORT:-9081}"
echo "  Notification Service    http://localhost:${NOTIFICATION_SERVICE_PORT:-8081}/health"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
