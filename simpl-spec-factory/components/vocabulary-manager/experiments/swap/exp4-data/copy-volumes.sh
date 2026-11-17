#!/usr/bin/env bash
# Experiment 4: copy the ORIGINAL upstream Fuseki volumes (mounted read-only) to swap-exp4-* volumes.
set -euo pipefail
for v in fuseki-data fuseki-configuration; do
  docker volume rm -f "swap-exp4-$v" >/dev/null 2>&1 || true
  docker volume create "swap-exp4-$v" >/dev/null
  docker run --rm -v "simpl-vocabulary-manager_$v:/from:ro" -v "swap-exp4-$v:/to" alpine sh -c 'cp -a /from/. /to/'
done
echo "copies ready: swap-exp4-fuseki-data swap-exp4-fuseki-configuration"
