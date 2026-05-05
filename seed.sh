#!/usr/bin/env bash
# simpl-catalogue-local — seed the catalogue with example self-descriptions.
#
# Two-stage seeding:
#   1. Upload the Simpl-specific ontology + SHACL shapes (from upstream
#      sdtooling-sd-schemas) — required because fc-service is Simpl-aware:
#      it expects a schema with id "http://w3id.org/gaia-x/simpl#" before
#      it will accept any SD, but the simpl ontology is NOT bundled in the
#      4 default schemas fc-service auto-loads.
#   2. Upload upstream-shipped Gaia-X example SDs (from simpl-fc-service
#      examples/) — Verifiable Presentations wrapping legal-person and
#      service-offering credentials.
#
# Idempotent: skips schema upload if the Simpl ontology is already loaded;
# skips SD upload if the catalogue already has SDs (unless --force).
#
# Prerequisites:
#   - ./start.sh has been run (fc-service is up + responsive)
#   - repos/simpl-fc-service/examples/ exists (cloned by start.sh)
#   - sdtooling-sd-schemas will be cloned into repos/ on first run.
#
# Usage:
#   ./seed.sh           Seed only if catalogue is empty.
#   ./seed.sh --force   Re-upload SDs even if catalogue already has data.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# fc-service's own mock-data fixtures use Simpl types (simpl:DataOffering,
# simpl:ServiceOffering, etc.) — required because fc-service rejects gax-only
# SDs (the upstream examples/ folder) once the Simpl ontology is loaded.
SD_FIXTURES="$REPO_DIR/repos/simpl-fc-service/fc-service-server/src/test/resources/mock-data"
SCHEMAS_REPO="$REPO_DIR/repos/sdtooling-sd-schemas"
SCHEMAS_GIT="https://code.europa.eu/simpl/simpl-open/development/data1/sdtooling-sd-schemas.git"
SIMPL_ONTOLOGY_URI="http://w3id.org/gaia-x/simpl#"

# Honour .env overrides if present.
if [ -f "$REPO_DIR/.env" ]; then
  # shellcheck disable=SC1091
  set -a; . "$REPO_DIR/.env"; set +a
fi

FC_SERVICE_PORT="${FC_SERVICE_PORT:-8081}"
FC_URL="http://localhost:${FC_SERVICE_PORT}"

# --- Pre-flight ---

if ! curl -fsS "${FC_URL}/self-descriptions" > /dev/null 2>&1; then
  echo "ERROR: fc-service not reachable at ${FC_URL}/self-descriptions." >&2
  echo "       Run ./start.sh first, then retry." >&2
  exit 1
fi

if [ ! -d "$SD_FIXTURES" ]; then
  echo "ERROR: fc-service mock-data fixtures missing at:" >&2
  echo "       $SD_FIXTURES" >&2
  echo "       Run ./start.sh first (it clones the upstream repo)." >&2
  exit 1
fi

# --- Stage 1: upload Simpl ontology + shapes ---

echo "==> Stage 1/2: ensure Simpl ontology + shapes are loaded"

# Clone sdtooling-sd-schemas if missing.
if [ ! -d "$SCHEMAS_REPO/.git" ]; then
  echo "    Cloning $SCHEMAS_GIT ..."
  git clone "$SCHEMAS_GIT" "$SCHEMAS_REPO"
fi

ONTOLOGY_FILE="$SCHEMAS_REPO/ontology/simpl_ontology_generated.ttl"
SHAPES_FILE="$SCHEMAS_REPO/shape/merged-shapes.ttl"

for f in "$ONTOLOGY_FILE" "$SHAPES_FILE"; do
  if [ ! -f "$f" ]; then
    echo "ERROR: expected schema file missing: $f" >&2
    echo "       The sdtooling-sd-schemas repo layout may have changed upstream." >&2
    exit 1
  fi
done

# Skip upload if Simpl ontology is already loaded.
ALREADY_LOADED=0
if curl -fsS "${FC_URL}/schemas" \
   | python3 -c "import sys, json; sys.exit(0 if '${SIMPL_ONTOLOGY_URI}' in json.load(sys.stdin).get('ontologies', []) else 1)" 2>/dev/null; then
  echo "    Simpl ontology already loaded — skipping schema upload"
  ALREADY_LOADED=1
fi

if [ "$ALREADY_LOADED" -eq 0 ]; then
  printf "    → POST %-32s " "simpl_ontology_generated.ttl"
  HTTP_STATUS=$(curl -s -o /tmp/seed-response.json -w "%{http_code}" \
    -X POST -H "Content-Type: text/turtle" \
    --data-binary "@$ONTOLOGY_FILE" \
    "${FC_URL}/schemas")
  if [ "$HTTP_STATUS" = "201" ] || [ "$HTTP_STATUS" = "200" ]; then
    echo "✓ HTTP ${HTTP_STATUS}"
  else
    echo "✗ HTTP ${HTTP_STATUS}"
    echo "      Response body (first 10 lines):"
    head -10 /tmp/seed-response.json | sed 's/^/        /'
    exit 1
  fi

  printf "    → POST %-32s " "merged-shapes.ttl"
  HTTP_STATUS=$(curl -s -o /tmp/seed-response.json -w "%{http_code}" \
    -X POST -H "Content-Type: text/turtle" \
    --data-binary "@$SHAPES_FILE" \
    "${FC_URL}/schemas")
  if [ "$HTTP_STATUS" = "201" ] || [ "$HTTP_STATUS" = "200" ]; then
    echo "✓ HTTP ${HTTP_STATUS}"
  else
    echo "✗ HTTP ${HTTP_STATUS}"
    echo "      Response body (first 10 lines):"
    head -10 /tmp/seed-response.json | sed 's/^/        /'
    exit 1
  fi
fi

# --- Stage 2: upload self-descriptions ---

echo ""
echo "==> Stage 2/2: upload example self-descriptions"

CURRENT_COUNT=$(curl -fsS "${FC_URL}/self-descriptions" \
  | python3 -c "import sys, json; print(json.load(sys.stdin).get('totalCount', 0))")

if [ "$CURRENT_COUNT" -gt 0 ] && [ "${1:-}" != "--force" ]; then
  echo "    Catalogue already has ${CURRENT_COUNT} self-description(s). Nothing to seed."
  echo "    Pass --force to seed anyway (will not de-duplicate)."
  exit 0
fi

# fc-service's own test fixtures with Simpl-shape credentialSubjects.
#
# Currently only one is included:
#   - default-sd.json: simpl:DataOffering, single VC. Lands cleanly.
#
# default-sd-service-offering.json (a VP-wrapped service-offering) is NOT
# included because it triggers an upstream ClassCastException in
# VerificationServiceImpl.verifyOfferingSelfDescription — the type-inference
# path returns a base VerificationResult that is then unchecked-cast to
# VerificationResultOffering. Re-add it once the upstream bug is fixed.
SDS=(
  "default-sd.json"
)

ADDED=0
FAILED=0

for sd in "${SDS[@]}"; do
  SD_FILE="$SD_FIXTURES/$sd"
  if [ ! -f "$SD_FILE" ]; then
    echo "    ⚠️  $sd not found in mock-data/ — skipping"
    continue
  fi
  printf "    → POST %-32s " "$sd"
  # Patch the SD before posting:
  #   - simpl:access-policy: replace upstream's placeholder string "swvwe" with
  #     a valid stringified ODRL policy that grants access to CONSUMER role.
  #     Without this, fc-service's QuickSearchService crashes with a 500
  #     because its isAccessGranted() unsafely JSON-parses the field.
  #   - simpl:usage-policy: same root cause; "{}" is the minimum valid JSON.
  #   Both fields live at credentialSubject.simpl:servicePolicy.simpl:*
  FIXED_SD=$(python3 -c '
import json, sys
sd = json.load(open(sys.argv[1]))
cs = sd.get("credentialSubject", {})
# Fix access/usage-policy (nested under simpl:servicePolicy)
sp = cs.get("simpl:servicePolicy")
if isinstance(sp, dict):
    sp["simpl:access-policy"] = "{\"permission\":[{\"assignee\":{\"uid\":\"CONSUMER\"}}]}"
    sp["simpl:usage-policy"] = "{}"
# Add simpl:offeringType to generalServiceProperties — upstream code reads this
# field directly but the example SD only encodes the type as rdf:type on the subject.
gsp = cs.get("simpl:generalServiceProperties")
if isinstance(gsp, dict) and "simpl:offeringType" not in gsp:
    rdf_type = cs.get("rdf:type", {})
    type_id = rdf_type.get("@id", "") if isinstance(rdf_type, dict) else str(rdf_type)
    gsp["simpl:offeringType"] = type_id.split(":")[-1] if type_id else "DataOffering"
print(json.dumps(sd))
' "$SD_FILE")
  HTTP_STATUS=$(echo "$FIXED_SD" | curl -s -o /tmp/seed-response.json -w "%{http_code}" \
    -X POST -H "Content-Type: application/json" \
    --data-binary @- \
    "${FC_URL}/self-descriptions")

  if [ "$HTTP_STATUS" = "201" ] || [ "$HTTP_STATUS" = "200" ]; then
    echo "✓ HTTP ${HTTP_STATUS}"
    ADDED=$((ADDED + 1))
  else
    echo "✗ HTTP ${HTTP_STATUS}"
    echo "      Response body (first 10 lines):"
    head -10 /tmp/seed-response.json | sed 's/^/        /'
    echo ""
    FAILED=$((FAILED + 1))
  fi
done

# --- Summary ---

echo ""
echo "==> Seed complete: ${ADDED} self-description(s) added, ${FAILED} failed"

if [ "$ADDED" -gt 0 ]; then
  NEW_COUNT=$(curl -fsS "${FC_URL}/self-descriptions" \
    | python3 -c "import sys, json; print(json.load(sys.stdin).get('totalCount', 0))")
  echo "    Catalogue now has ${NEW_COUNT} self-description(s)."
  echo ""
  echo "    Try browsing them:"
  echo "      curl -s ${FC_URL}/self-descriptions | python3 -m json.tool"
fi

if [ "$FAILED" -gt 0 ]; then
  exit 1
fi
