#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Removes ONLY what this stack installed. Never touches other namespaces,
# pre-existing cluster workloads, or the OrbStack cluster itself.

PURGE=false
for arg in "$@"; do
  case "$arg" in
    --purge) PURGE=true ;;
    *) echo "unknown flag: $arg (known: --purge)"; exit 1 ;;
  esac
done

# shellcheck disable=SC1091
[ -f .env ] && source .env
NS="${NS:-provisioner}"

for rel in provisioner-resources tf-controller kafka provisioner-dependencies; do
  helm -n "$NS" status "$rel" >/dev/null 2>&1 && helm -n "$NS" uninstall "$rel"
done

if $PURGE; then
  kubectl delete ns "$NS" --ignore-not-found
  # namespace hardcoded in the dependencies chart's namespace.yaml template
  kubectl delete ns crossplane-resource-secrets --ignore-not-found
  echo "Purged namespaces. Crossplane/Argo CRDs installed by the charts remain;"
  echo "list with: kubectl get crd | grep -E 'crossplane|argoproj|toolkit.fluxcd'"
else
  echo "Releases uninstalled. Namespace $NS kept (PVCs incl. gitea data). Use --purge to remove."
fi
