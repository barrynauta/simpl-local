#!/usr/bin/env bash
# Online backup of the vocabulary store: dumps the whole Fuseki dataset (all named graphs) as N-Quads.
# Usage: ops/backup.sh [output-file]   (default: backups/vocabulary-<UTC timestamp>.nq)
# Writes are serialised per request and each write is one store transaction (D43), so the dump is a consistent
# snapshot of committed transactions.
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-backups/vocabulary-$(date -u +%Y%m%dT%H%M%SZ).nq}"
mkdir -p "$(dirname "$OUT")"
docker compose -p vm-factory-run6 exec -T fuseki \
  wget -q -O - --header "Accept: application/n-quads" http://127.0.0.1:3030/vocabulary > "$OUT"
echo "Backup written: $OUT ($(wc -l < "$OUT" | tr -d ' ') quads)"
