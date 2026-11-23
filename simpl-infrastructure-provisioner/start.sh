#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Local-evaluation stack for the Crossplane Infrastructure Provisioner
# (development/infrastructure/infrastructure-provisioner) on a local
# Kubernetes cluster (OrbStack). See README.md for scope and findings.

REFRESH_SRC=false
WITH_KAFKA=false
SKIP_RESOURCES=false
for arg in "$@"; do
  case "$arg" in
    --refresh-src) REFRESH_SRC=true ;;
    --with-kafka) WITH_KAFKA=true ;;
    --skip-resources) SKIP_RESOURCES=true ;;
    *) echo "unknown flag: $arg (known: --refresh-src --with-kafka --skip-resources)"; exit 1 ;;
  esac
done

# ── 1. Environment ──────────────────────────────────────────────────────────
if [ ! -f .env ]; then
  cp .env.example .env
  echo ".env created from .env.example, edit if needed"
fi
# shellcheck disable=SC1091
source .env

NS="${NS:-provisioner}"
SRC_DIR="${SRC_DIR:-$SCRIPT_DIR/../../gitlab/development/infrastructure/infrastructure-provisioner}"
CHART_VERSION="${CHART_VERSION:-1.2.1-local}"

# ── 2. Prerequisites ────────────────────────────────────────────────────────
for cmd in kubectl helm rsync; do
  command -v "$cmd" >/dev/null || { echo "ERROR: '$cmd' not found"; exit 1; }
done
kubectl get nodes >/dev/null || { echo "ERROR: no reachable Kubernetes cluster (enable OrbStack k8s)"; exit 1; }

STORAGE_CLASS="${STORAGE_CLASS:-$(kubectl get sc -o jsonpath='{.items[?(@.metadata.annotations.storageclass\.kubernetes\.io/is-default-class=="true")].metadata.name}')}"
[ -n "$STORAGE_CLASS" ] || { echo "ERROR: no default StorageClass found; set STORAGE_CLASS in .env"; exit 1; }
echo "cluster OK, storage class: $STORAGE_CLASS, namespace: $NS"

# Upstream warns against installing Crossplane twice in one cluster.
if [ -z "${ALLOW_EXISTING_CROSSPLANE:-}" ] && kubectl get crd providers.pkg.crossplane.io >/dev/null 2>&1 \
   && ! kubectl -n "$NS" get deploy crossplane >/dev/null 2>&1; then
  echo "ERROR: Crossplane CRDs already exist in this cluster but not from this stack."
  echo "Upstream InstallGuide forbids double installation. Set ALLOW_EXISTING_CROSSPLANE=1 to override."
  exit 1
fi

# ── 3. Source: copy from local checkout (no git needed) ─────────────────────
# Convention deviation from other simpl-local stacks (which git-clone at build
# time): this stack copies from the existing ~/src/gitlab checkout so it can
# run without network or git. To use a fresh clone instead, run:
#   git clone https://code.europa.eu/simpl/simpl-open/development/infrastructure/infrastructure-provisioner.git repos/infrastructure-provisioner
REPO="repos/infrastructure-provisioner"
if $REFRESH_SRC || [ ! -d "$REPO" ]; then
  [ -d "$SRC_DIR" ] || { echo "ERROR: SRC_DIR not found: $SRC_DIR (clone it or fix SRC_DIR in .env)"; exit 1; }
  mkdir -p repos
  rsync -a --delete --exclude '.git' "$SRC_DIR"/ "$REPO"/
  echo "source copied from $SRC_DIR"
fi

# ── 4. Patch upstream gotchas ───────────────────────────────────────────────
# Root chart ships version: ${PROJECT_RELEASE_VERSION} (CI substitution),
# which is not valid semver and breaks helm. Same pattern as SD-tooling poms.
sed -i '' "s/\${PROJECT_RELEASE_VERSION}/$CHART_VERSION/g" "$REPO/charts/Chart.yaml"

# ── 5. Local values overlays ────────────────────────────────────────────────
# Upstream values.env.yaml files are broken templates ('key: #PLACEHOLDER'
# comment placeholders for CI sed); we supply proper overlays instead.
cat > values/dependencies-local.yaml <<EOF
gitea:
  storageClassName: $STORAGE_CLASS
  volumeSize: ${GITEA_VOLUME_SIZE:-2Gi}
argo-cd:
  crds:
    install: ${ARGO_CD_CRDS_INSTALL:-false}
secrets:
  gitea:
    username: ${GITEA_USERNAME:-gitea_admin}
    password: ${GITEA_PASSWORD:-local-gitea-pass}
EOF

# ── 6. Install: dependencies chart ──────────────────────────────────────────
echo "── helm dep up (dependencies): downloads crossplane, argo-cd, argo-events, argo-workflows, gitea, flux2 charts"
helm dependency update "$REPO/charts/dependencies"
echo "── installing provisioner-dependencies into ns/$NS"
helm upgrade --install provisioner-dependencies --create-namespace -n "$NS" \
  "$REPO/charts/dependencies" \
  -f "$REPO/charts/dependencies/values.yaml" \
  -f values/dependencies-local.yaml

# ── 7. Install: tofu-controller (separate per upstream note on Flux CRDs) ───
echo "── helm dep up (tofu-controller)"
helm dependency update "$REPO/charts/tofu-controller"
echo "── installing tf-controller into ns/$NS"
helm upgrade --install tf-controller -n "$NS" "$REPO/charts/tofu-controller"

# ── 8. Optional local Kafka (SASL PLAIN) for the event flows ────────────────
KAFKA_ENDPOINT="${KAFKA_ENDPOINT:-kafka.local.invalid:9092}"
if $WITH_KAFKA; then
  echo "── installing local Kafka (bitnami, SASL PLAIN)"
  helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null 2>&1 || true
  helm upgrade --install kafka bitnami/kafka -n "$NS" \
    --set image.repository=bitnamilegacy/kafka \
    --set listeners.client.protocol=SASL_PLAINTEXT \
    --set sasl.client.users[0]="${KAFKA_USERNAME:-local-user}" \
    --set sasl.client.passwords="${KAFKA_PASSWORD:-local-pass}" \
    --set controller.replicaCount=1 --set broker.replicaCount=0 \
    --set controller.persistence.size=1Gi
  KAFKA_ENDPOINT="kafka.$NS.svc.cluster.local:9092"
fi

# ── 9. Install: resources chart (the provisioner logic itself) ──────────────
# NOTE upstream InstallGuide points at charts/resources, which does not exist;
# the resources chart is the ROOT charts/ directory. Documented in README.
# NOTE (F6): the resources chart contains ProviderConfig CRs whose CRDs are
# installed ASYNCHRONOUSLY by crossplane from the configuration package. The
# upstream guide has no wait step, so a first run fails with "ensure CRDs are
# installed first". We wait for the provider CRDs before applying.
if ! $SKIP_RESOURCES; then
  echo "── waiting up to 10 min for crossplane provider CRDs (async from configuration package)"
  for crd in providerconfigs.ionoscloud.crossplane.io providerconfigs.ovh.edixos.io; do
    n=0
    until kubectl get crd "$crd" >/dev/null 2>&1; do
      n=$((n+1)); [ $n -gt 120 ] && { echo "ERROR: timed out waiting for CRD $crd"; kubectl get providers 2>/dev/null; exit 1; }
      sleep 5
    done
    echo "   CRD ready: $crd"
  done
  GITEA_SVC="$(kubectl -n "$NS" get svc -o name | grep -i 'gitea.*http' | head -1 | sed 's|service/||')"
  # NOTE (F7): giteaUrl must be host:port WITHOUT scheme; the chart's configmap
  # prepends http:// itself. With a scheme the setup job curls host "http".
  GITEA_URL="${GITEA_SVC:-provisioner-dependencies-gitea-http}.$NS.svc.cluster.local:3000"
  echo "── gitea url: $GITEA_URL"
  cat > values/resources-local.yaml <<EOF
kafkaEndpoint: $KAFKA_ENDPOINT
giteaUrl: $GITEA_URL
secrets:
  ionos:
    token: ${IONOS_TOKEN:-dummy-local-ionos-token}
  ovh:
    application_key: ${OVH_APPLICATION_KEY:-dummy}
    application_secret: ${OVH_APPLICATION_SECRET:-dummy}
    consumer_key: ${OVH_CONSUMER_KEY:-dummy}
    endpoint: ${OVH_ENDPOINT:-ovh-eu}
  kafka:
    username: ${KAFKA_USERNAME:-local-user}
    password: ${KAFKA_PASSWORD:-local-pass}
  gitea:
    username: ${GITEA_USERNAME:-gitea_admin}
    password: ${GITEA_PASSWORD:-local-gitea-pass}
EOF
  echo "── installing provisioner-resources into ns/$NS"
  helm upgrade --install provisioner-resources -n "$NS" \
    "$REPO/charts" \
    -f "$REPO/charts/values.yaml" \
    -f values/resources-local.yaml
fi

# ── 10. Status ──────────────────────────────────────────────────────────────
echo
echo "── waiting up to 5 min for pods in ns/$NS to settle"
kubectl -n "$NS" wait --for=condition=Ready pods --all --timeout=300s || true
echo
kubectl -n "$NS" get pods
echo
echo "Done. Inspect with: kubectl -n $NS get pods,applications,eventsources,sensors"
echo "ArgoCD UI: kubectl -n $NS port-forward svc/provisioner-dependencies-argocd-server 8090:80"
echo "Gitea UI:  kubectl -n $NS port-forward svc/\${GITEA_SVC} 3010:3000"
