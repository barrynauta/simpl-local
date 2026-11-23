#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Produces a crafted provisioning request onto the local Kafka (to-provision
# topic) to drive the full loop: event source -> sensor -> Argo Workflow
# (gitops image) -> push to management/data repos -> ArgoCD sync -> Crossplane
# claim -> provider auth (fails at the dummy-credential boundary, by design).
#
# Body must satisfy workflows/gitops/parse/crossplane.py validate_structure:
# exactly {apiVersion, kind, spec.parameters, metadata.{name, labels.provider}}.
# Single-line JSON (valid YAML) so the console producer can carry it.
# NOTE: the shipped example claim (package/examples/compute/ionos.yaml) does
# NOT pass this validator (metadata has namespace, lacks labels.provider).

# shellcheck disable=SC1091
[ -f .env ] && source .env
NS="${NS:-provisioner}"
KAFKA_POD="${KAFKA_POD:-kafka-controller-0}"
TOPIC="${TOPIC:-to-provision}"
UUID="${1:-$(uuidgen | tr '[:upper:]' '[:lower:]')}"

BODY='{"apiVersion":"compute.infrastructure.simpl-europe.eu/v1alpha1","kind":"ServerInstance","metadata":{"name":"local-test-server","labels":{"provider":"ionos"}},"spec":{"parameters":{"datacenterName":"local-test-dc","datacenterDescription":"local test","datacenterLocation":"es/vit","serverName":"local-test-server","cores":1,"ram":1024,"cpuFamily":"INTEL_ICELAKE"}}}'

CLIENT_PROPS="security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${KAFKA_USERNAME:-local-user}\" password=\"${KAFKA_PASSWORD:-local-pass}\";"

echo "── ensuring topic $TOPIC exists"
kubectl -n "$NS" exec -i "$KAFKA_POD" -- bash -c \
  "printf '%s\n' '$CLIENT_PROPS' > /tmp/client.properties
   kafka-topics.sh --bootstrap-server localhost:9092 --command-config /tmp/client.properties \
     --create --if-not-exists --topic $TOPIC --partitions 1 --replication-factor 1"

echo "── producing provisioning request, scriptTriggerId=$UUID"
printf 'scriptTriggerId:%s\t%s\n' "$UUID" "$BODY" | kubectl -n "$NS" exec -i "$KAFKA_POD" -- bash -c \
  "kafka-console-producer.sh --bootstrap-server localhost:9092 --topic $TOPIC \
     --producer.config /tmp/client.properties \
     --property parse.headers=true"

echo "── sent. Watch with:"
echo "   kubectl -n $NS get workflows -w"
echo "   kubectl -n $NS get applications.argoproj.io"
echo "   kubectl -n $NS get serverinstances.compute.infrastructure.simpl-europe.eu"
