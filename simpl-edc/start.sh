#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD=false
RUN_TRANSFER=true
for arg in "$@"; do
  case "$arg" in
    --rebuild) REBUILD=true ;;
    --no-transfer) RUN_TRANSFER=false ;;
  esac
done

# ── 1. Environment ──────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  cp .env.example .env
  echo ".env created from .env.example — edit ports/credentials if needed"
fi
# shellcheck disable=SC1091
source .env 2>/dev/null || true

# ── 2. Prerequisites ─────────────────────────────────────────────────────────
for cmd in docker git curl jq; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' not found. See README prerequisites." >&2
    exit 1
  fi
done

# ── 3. Clone upstream connector ─────────────────────────────────────────────
if [ ! -d repos/simpl-edc ]; then
  echo "Cloning simpl-edc..."
  git clone "https://code.europa.eu/simpl/simpl-open/development/gaia-x-edc/simpl-edc.git" repos/simpl-edc
else
  echo "Pulling latest simpl-edc..."
  git -C repos/simpl-edc pull --ff-only || echo "  (pull skipped — local changes or detached HEAD)"
fi

# ── 4. Generate containerized config from upstream ──────────────────────────
# Inherit the upstream local/*-config.properties (incl. the mocked IAM identity
# blob and contractmanager.extension.enabled=false) and only rewrite the
# localhost references to compose service names + disable the Dagster callback.
echo "Generating localized connector configs..."
mkdir -p config
sed -e 's|jdbc:postgresql://localhost:5432/|jdbc:postgresql://provider-db:5432/|g' \
    -e 's|http://localhost:19194/protocol|http://provider:19194/protocol|g' \
    -e 's|http://localhost:19291/public|http://provider:19291/public|g' \
    -e 's|http://localhost:19191/api|http://provider:19191/api|g' \
    -e 's|fr.gxfs.s3.endpoint=http://localhost:9000|fr.gxfs.s3.endpoint=http://minio:9000|' \
    -e 's|transfer.extension.enabled=true|transfer.extension.enabled=false|' \
    repos/simpl-edc/local/provider-config.properties > config/provider.properties

sed -e 's|jdbc:postgresql://localhost:5433/|jdbc:postgresql://consumer-db:5432/|g' \
    -e 's|http://localhost:29194/protocol|http://consumer:29194/protocol|g' \
    -e 's|http://localhost:29291/public|http://consumer:29291/public|g' \
    -e 's|fr.gxfs.s3.endpoint=http://localhost:9000|fr.gxfs.s3.endpoint=http://minio:9000|' \
    -e 's|transfer.extension.enabled=true|transfer.extension.enabled=false|' \
    repos/simpl-edc/local/consumer-config.properties > config/consumer.properties

# ── 5. Build connector image ────────────────────────────────────────────────
IMAGE_EXISTS=$(docker images -q simpl-edc-connector:local 2>/dev/null || true)
if [ -z "$IMAGE_EXISTS" ] || [ "$REBUILD" = true ]; then
  echo "Building connector image (Maven — EDC dep tree, first run ~10 min)..."
  docker build -f "$SCRIPT_DIR/Dockerfile.local" -t simpl-edc-connector:local "$SCRIPT_DIR/"
  echo "Connector image built."
else
  echo "Connector image already exists, skipping build (use --rebuild to force)."
fi

# ── 6. Start the stack ───────────────────────────────────────────────────────
echo "Starting stack (2 Postgres, MinIO + init, provider + consumer)..."
docker compose up -d

# ── 7. Wait for both connectors ─────────────────────────────────────────────
wait_for_connector() {
  local name="$1" port="$2" elapsed=0 timeout=240
  echo "Waiting for $name management API on :$port ..."
  until [ "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'X-Api-Key: password' \
            -H 'Content-Type: application/json' --data '{"@type":"QuerySpec"}' \
            "http://localhost:$port/management/v3/assets/request" 2>/dev/null)" = "200" ]; do
    sleep 4; elapsed=$((elapsed + 4))
    if [ $elapsed -ge $timeout ]; then
      echo "ERROR: $name not ready after ${timeout}s — check: docker logs simpl-edc-$name" >&2
      exit 1
    fi
  done
  echo "$name ready."
}
wait_for_connector provider 19193
wait_for_connector consumer 29193

# ── 8. Drive the MinioS3-PUSH transfer (mirrors upstream complete-minio-transfer.sh) ─
if [ "$RUN_TRANSFER" = true ]; then
  echo ""
  echo "━━━ Running provider→consumer MinioS3-PUSH transfer ━━━"
  PMGMT="localhost:19193"; CMGMT="localhost:29193"
  PROTO="provider:19194"          # container hostname — used inside consumer
  MINIO_EP="http://minio:9000"    # container hostname — used inside connectors
  KEY='X-Api-Key: password'

  echo "1. Provider: create MinioS3 asset"
  curl -s -o /dev/null -X POST "http://$PMGMT/management/v3/assets" -H "$KEY" -H 'Content-Type: application/json' \
    --data-raw "{\"@context\":{\"@vocab\":\"https://w3id.org/edc/v0.0.1/ns/\"},\"@id\":\"example-s3-asset\",\"properties\":{\"name\":\"Example S3 File\",\"contenttype\":\"text/plain\"},\"dataAddress\":{\"type\":\"MinioS3\",\"bucketName\":\"provider-bucket\",\"objectName\":\"example-s3.txt\",\"endpoint\":\"$MINIO_EP\"}}"

  echo "2. Provider: create policy"
  curl -s -o /dev/null -X POST "http://$PMGMT/management/v3/policydefinitions" -H "$KEY" -H 'Content-Type: application/json' \
    --data-raw '{"@context":{"@vocab":"https://w3id.org/edc/v0.0.1/ns/"},"@id":"minio-s3-policy","policy":{"@context":"http://www.w3.org/ns/odrl.jsonld","@type":"Set","permission":[{"action":"use"}],"prohibition":[],"obligation":[]}}'

  echo "3. Provider: create contract definition"
  curl -s -o /dev/null -X POST "http://$PMGMT/management/v3/contractdefinitions" -H "$KEY" -H 'Content-Type: application/json' \
    --data-raw '{"@context":{"@vocab":"https://w3id.org/edc/v0.0.1/ns/"},"@id":"minio-s3-contract-def","accessPolicyId":"minio-s3-policy","contractPolicyId":"minio-s3-policy","assetsSelector":[{"@type":"CriterionDto","operandLeft":"https://w3id.org/edc/v0.0.1/ns/id","operator":"=","operandRight":"example-s3-asset"}]}'

  echo "4. Consumer: query catalogue"
  CATALOG=$(curl -s -X POST "http://$CMGMT/management/v3/catalog/request" -H "$KEY" -H 'Content-Type: application/json' \
    --data-raw "{\"@context\":{\"@vocab\":\"https://w3id.org/edc/v0.0.1/ns/\"},\"@type\":\"CatalogRequestDto\",\"counterPartyAddress\":\"http://$PROTO/protocol\",\"protocol\":\"dataspace-protocol-http\"}")
  POLICY_ID=$(echo "$CATALOG" | jq -r '."dcat:dataset" | if type=="array" then .[] else . end | select(."@id"=="example-s3-asset") | ."odrl:hasPolicy" | if type=="array" then .[0] else . end | ."@id"' 2>/dev/null | head -1)
  if [ -z "$POLICY_ID" ] || [ "$POLICY_ID" = "null" ]; then
    echo "ERROR: no policy id in catalogue. Response:"; echo "$CATALOG" | jq . 2>/dev/null || echo "$CATALOG"; exit 1
  fi
  echo "   policy id: $POLICY_ID"

  echo "5. Consumer: start contract negotiation"
  NEG=$(curl -s -X POST "http://$CMGMT/management/v3/contractnegotiations" -H "$KEY" -H 'Content-Type: application/json' \
    --data-raw "{\"@context\":{\"@vocab\":\"https://w3id.org/edc/v0.0.1/ns/\"},\"@type\":\"ContractRequest\",\"counterPartyAddress\":\"http://$PROTO/protocol\",\"protocol\":\"dataspace-protocol-http\",\"policy\":{\"@context\":\"http://www.w3.org/ns/odrl.jsonld\",\"@id\":\"$POLICY_ID\",\"@type\":\"Offer\",\"permission\":[{\"action\":\"use\",\"target\":\"example-s3-asset\"}],\"assigner\":\"provider\",\"target\":\"example-s3-asset\"}}")
  NEG_ID=$(echo "$NEG" | jq -r '."@id"')
  [ -n "$NEG_ID" ] && [ "$NEG_ID" != "null" ] || { echo "ERROR: negotiation failed: $NEG"; exit 1; }

  echo "6. Poll negotiation until FINALIZED (auto-finalizes; contractmanager disabled)"
  AGREEMENT=""
  for _ in $(seq 1 15); do
    sleep 2
    STATUS=$(curl -s -H "$KEY" "http://$CMGMT/management/v3/contractnegotiations/$NEG_ID")
    STATE=$(echo "$STATUS" | jq -r '.state')
    AGREEMENT=$(echo "$STATUS" | jq -r '.contractAgreementId')
    echo "   negotiation state: $STATE"
    # Agreement id appears at AGREED, but the transfer needs FINALIZED.
    [ "$STATE" = "FINALIZED" ] && [ "$AGREEMENT" != "null" ] && [ -n "$AGREEMENT" ] && break
  done
  [ "$STATE" = "FINALIZED" ] || { echo "ERROR: negotiation never FINALIZED (last: $STATE)"; exit 1; }
  echo "   agreement: $AGREEMENT"

  echo "7. Consumer: start MinioS3-PUSH transfer"
  TRANSFER=$(curl -s -X POST "http://$CMGMT/management/v3/transferprocesses" -H "$KEY" -H 'Content-Type: application/json' \
    --data-raw "{\"@context\":{\"@vocab\":\"https://w3id.org/edc/v0.0.1/ns/\"},\"@type\":\"TransferRequestDto\",\"connectorId\":\"provider\",\"counterPartyAddress\":\"http://$PROTO/protocol\",\"contractId\":\"$AGREEMENT\",\"assetId\":\"example-s3-asset\",\"protocol\":\"dataspace-protocol-http\",\"transferType\":\"MinioS3-PUSH\",\"dataDestination\":{\"type\":\"MinioS3\",\"bucketName\":\"consumer-bucket\",\"objectName\":\"example-s3.txt\",\"endpoint\":\"$MINIO_EP\"}}")
  TRANSFER_ID=$(echo "$TRANSFER" | jq -r '."@id"')
  [ -n "$TRANSFER_ID" ] && [ "$TRANSFER_ID" != "null" ] || { echo "ERROR: transfer init failed: $TRANSFER"; exit 1; }

  echo "8. Poll transfer until COMPLETED"
  TSTATE=""
  for _ in $(seq 1 20); do
    sleep 3
    TSTATE=$(curl -s -H "$KEY" "http://$CMGMT/management/v3/transferprocesses/$TRANSFER_ID" | jq -r '.state')
    echo "   transfer state: $TSTATE"
    [ "$TSTATE" = "COMPLETED" ] && break
    case "$TSTATE" in TERMINATED|FAILED|ERROR) echo "ERROR: transfer ended $TSTATE"; exit 1 ;; esac
  done

  echo "9. Verify example-s3.txt in consumer-bucket (containerized mc)"
  LS=$(docker compose run --rm --no-deps --entrypoint /bin/sh minio-init -c \
    "mc alias set local http://minio:9000 minioadmin minioadmin >/dev/null 2>&1; mc ls local/consumer-bucket/" 2>/dev/null || true)
  if echo "$LS" | grep -q "example-s3.txt"; then
    echo "   ✅ Transfer verified: example-s3.txt present in consumer-bucket"
  else
    echo "   ❌ Not found in consumer-bucket (transfer state was: $TSTATE)"; echo "$LS"; exit 1
  fi
fi

# ── 9. Service URLs ──────────────────────────────────────────────────────────
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Service                 URL"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Provider management API http://localhost:19193/management   (X-Api-Key: password)"
echo "  Consumer management API http://localhost:29193/management   (X-Api-Key: password)"
echo "  Provider DSP protocol   http://localhost:19194/protocol"
echo "  MinIO S3 API            http://localhost:${MINIO_API_PORT:-9000}"
echo "  MinIO console           http://localhost:${MINIO_CONSOLE_PORT:-9090}   (minioadmin / minioadmin)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
