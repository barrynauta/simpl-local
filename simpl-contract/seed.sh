#!/usr/bin/env bash
# Seed a sample contract agreement into Postgres so the UI's read path has data.
# Idempotent (ON CONFLICT DO NOTHING). Run after ./start.sh.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

docker exec -i simpl-contract-postgres psql -U contract -d contract < samples/seed.sql
echo "Seeded. UI: http://localhost:${UI_PORT:-4323}  (agreement 11111111-1111-1111-1111-111111111111)"
