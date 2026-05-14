#!/usr/bin/env bash
# simpl-catalogue-local — stop the local catalogue stack.
#
#   ./stop.sh           Stop containers, preserve volumes (n10s init survives).
#   ./stop.sh --full    Also remove volumes (n10s init will re-run on next ./start.sh).

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$REPO_DIR"

if [ "${1:-}" = "--full" ]; then
  echo "==> Full cleanup (containers + volumes)"
  docker compose down -v
  echo "    Volumes removed: postgres-data, neo4j-data, neo4j-logs, neo4j-plugins."
  echo "    Cloned upstream code under repos/ is preserved (delete manually if needed)."
  echo "    Built JAR (repos/simpl-fc-service/fc-service-server/target/) is preserved."
  echo "    Docker image simpl-fc-service:local is preserved (docker rmi to remove)."
else
  echo "==> Stopping containers (data preserved in volumes)"
  docker compose down
fi

echo ""
echo "✓ Stopped"
