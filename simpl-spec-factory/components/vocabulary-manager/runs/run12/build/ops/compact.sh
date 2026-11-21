#!/usr/bin/env bash
# Online TDB2 compaction (D156): Fuseki /$/compact with deleteOld, run weekly after the daily backup.
set -euo pipefail
cd "$(dirname "$0")/.."
source .env
DC=(docker compose -p vm-factory-run12 -f docker-compose.yml)
"${DC[@]}" exec -T fuseki wget -q -O - --post-data='' \
  "http://vmadmin:${FUSEKI_ADMIN_PASSWORD}@127.0.0.1:3030/\$/compact/vocabulary?deleteOld=true"
echo
