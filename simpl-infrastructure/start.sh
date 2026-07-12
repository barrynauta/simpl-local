#!/usr/bin/env bash
# Build (if needed) and run the two-component infrastructure local stack:
# infrastructure-be + infrastructure-fe + Postgres + Kafka.
# Idempotent: skips the backend jar build when target/infrastructure-be.jar exists
# (pass --rebuild to force). See README.md and docs/ for what is omitted and why.
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || cp .env.example .env
# shellcheck disable=SC1091
set -a; . ./.env; set +a

REPO="${INFRA_BE_REPO:?set INFRA_BE_REPO in .env}"
JAR="$REPO/target/infrastructure-be.jar"

if [ "${1:-}" = "--rebuild" ] || [ ! -f "$JAR" ]; then
  echo "==> building infrastructure-be jar (PROJECT_RELEASE_VERSION required by pom)"
  ( cd "$REPO" && PROJECT_RELEASE_VERSION=2.2.0-local mvn -q -DskipTests -Dmaven.test.skip=true clean package )
else
  echo "==> reusing existing backend jar: $JAR"
fi

echo "==> starting stack (this builds the fe nginx image on first run)"
docker compose up -d --build

echo "==> waiting for infrastructure-be health"
for i in $(seq 1 60); do
  if curl -fsS http://localhost:8080/api/infrastructureProvisioning/v1/status >/dev/null 2>&1; then
    echo "==> backend UP"; break
  fi
  sleep 3
done

echo
echo "  Backend status : http://localhost:8080/api/infrastructureProvisioning/v1/status"
echo "  Swagger UI      : http://localhost:8080/swagger-ui.html"
echo "  Frontend (SPA)  : http://localhost:3001   (needs a Keycloak for interactive login, see README)"
echo
echo "  Smoke tests: ./seed.sh   or   docker compose --profile tests up bruno-smoke-test"
