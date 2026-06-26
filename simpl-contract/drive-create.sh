#!/usr/bin/env bash
# Step 2 — drive the create-contract-data Kafka round-trip.
#
# Produces a `create-contract-request` message (as the EDC connector would).
# The contract backend consumes it, calls the catalogue stub (DID lookup +
# contract data), renders the human-readable HTML + hash, and produces a
# `create-contract-response`. This script then reads that response topic and
# prints what came back — proving the backend processed a real message end to
# end (no persistence / no UI; that needs the full signer+vc+wallet+EDC mesh).
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

KAFKA=simpl-contract-kafka
BIN=/opt/bitnami/kafka/bin
NEG_ID="negotiation-$(date +%s 2>/dev/null || echo 2002)"

# Kafka client config for the SASL_PLAINTEXT/PLAIN broker.
docker exec "$KAFKA" bash -lc 'cat > /tmp/client.properties <<EOF
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="simpl" password="simpl-local";
EOF'

# Single-line request message (raw event JSON; depth <= 5, <= 10KB).
REQ=$(cat <<EOF
{"contractAgreementId":"22222222-2222-2222-2222-222222222222","mode":"PROVIDER","contractDefinitionId":"contract-definition-001","contractAgreementCreateTO":{"contractNegotiationId":"${NEG_ID}","assetId":"asset-dataset-42","providerId":"did:web:provider01","consumerId":"did:web:consumer01","contractOfferId":"offer-7002"}}
EOF
)

echo "==> Producing create-contract-request (negotiationId=${NEG_ID})"
echo "$REQ" | docker exec -i "$KAFKA" bash -lc \
  "$BIN/kafka-console-producer.sh --bootstrap-server localhost:9092 --producer.config /tmp/client.properties --topic create-contract-request"

echo "==> Reading create-contract-response (up to 20s)..."
docker exec "$KAFKA" bash -lc \
  "$BIN/kafka-console-consumer.sh --bootstrap-server localhost:9092 --consumer.config /tmp/client.properties --topic create-contract-response --from-beginning --timeout-ms 20000" \
  2>/dev/null || true

echo ""
echo "(If nothing printed: check 'docker logs simpl-contract' for the consumer + catalogue-stub calls.)"
