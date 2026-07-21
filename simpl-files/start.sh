#!/usr/bin/env bash
# simpl-files local stack: build and run the Simpl-Files nginx document server.
# No dependencies, no Helm, no cluster. Just nginx serving baked-in static files.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$HERE/repos/simpl-files"
IMAGE="simpl-files:local"
NAME="simpl-files"
PORT="${PORT:-8085}"

# 1. Clone upstream (main) if absent.
if [ ! -d "$REPO/.git" ]; then
  echo "==> cloning simpl-files (main)"
  git clone --depth 1 \
    https://code.europa.eu/simpl/simpl-open/development/data1/simpl-files "$REPO"
fi

# 2. Build the image. The contract/SLA templates in the repo's files/ folder are
#    copied into the image (COPY ./files/ /home/nginx/files/), so no volume mount.
echo "==> docker build $IMAGE"
docker build -t "$IMAGE" "$REPO"

# 3. Run it. nginx listens on NGINX_PORT (8080) and serves /home/nginx/files.
echo "==> docker run $NAME on host port $PORT"
docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" -e NGINX_PORT=8080 -p "$PORT:8080" "$IMAGE" >/dev/null

# 4. Smoke test.
sleep 2
echo "==> smoke test"
echo "    status:   $(curl -s "http://localhost:$PORT/status")"
echo "    template: $(curl -s "http://localhost:$PORT/static/contract/ContractTemplate1.json" | head -c 50)..."
echo
echo "Simpl-Files up on http://localhost:$PORT/"
echo "  /static/contract/ContractTemplate{1,2,3}.json   /static/pdf/*.pdf   /status"
