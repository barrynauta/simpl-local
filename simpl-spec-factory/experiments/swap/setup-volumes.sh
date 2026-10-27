#!/usr/bin/env bash
# Copy the upstream Fuseki volumes into experiment volumes (read-only on the source).
# Run with the upstream stack stopped.
set -euo pipefail
for v in fuseki-data fuseki-configuration; do
  docker volume create "swap-up-$v" >/dev/null
  docker run --rm -v "simpl-vocabulary-manager_$v:/from:ro" -v "swap-up-$v:/to" alpine sh -c 'cp -a /from/. /to/'
done
echo "copies ready: swap-up-fuseki-data swap-up-fuseki-configuration"
