#!/usr/bin/env bash
# Weekly online compaction of the TDB2 store (D156), run after the daily backup. No downtime.
# Example schedule (cron, Sunday 03:00): 0 3 * * 0 cd /opt/vocabulary-manager && ./scripts/compact.sh
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose -p vm-factory-run13 -f compose.yaml exec -T fuseki sh -c \
  'wget -q -O - --post-data "" "http://admin:${FUSEKI_ADMIN_PASSWORD}@127.0.0.1:3030/\$/compact/vocabulary?deleteOld=true"'
echo
