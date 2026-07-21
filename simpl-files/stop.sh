#!/usr/bin/env bash
# Stop the simpl-files local stack. Use --full to also remove the image + clone.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"

docker rm -f simpl-files >/dev/null 2>&1 || true
echo "simpl-files container removed."

if [ "${1:-}" = "--full" ]; then
  docker rmi simpl-files:local >/dev/null 2>&1 || true
  rm -rf "$HERE/repos"
  echo "removed local image + repos/ clone."
fi
