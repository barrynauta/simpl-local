#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

FULL=false
for arg in "$@"; do
  case "$arg" in
    --full) FULL=true ;;
  esac
done

if [ "$FULL" = true ]; then
  echo "Stopping and removing containers, network, volumes, and local images..."
  docker compose down --volumes --rmi local
  echo "Done. Cloned sources in repos/ and seeded schemas/ are kept;"
  echo "delete them manually if you want a truly clean slate."
else
  echo "Stopping containers (volumes preserved)..."
  docker compose down
  echo "Done. Use './stop.sh --full' to also remove volumes and images."
fi
