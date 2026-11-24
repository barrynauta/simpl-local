#!/usr/bin/env bash
# Weekly TDB2 compaction (D156), run after the daily backup, online with delete of the old generation.
# Also removes receipts of conditional writes older than one hour (D176).
set -euo pipefail
cd "$(dirname "$0")/.."
export COMPOSE_PROJECT_NAME=vm-factory-run14
docker compose exec -T fuseki sh -c '
  auth="Authorization: Basic $(printf "%s:%s" "$FUSEKI_UPDATE_USER" "$FUSEKI_UPDATE_PASSWORD" | base64)"
  cutoff="$(date -u -d @$(( $(date +%s) - 3600 )) +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u +%Y-%m-%dT%H:%M:%SZ)"
  wget -q -O /dev/null --header "$auth" --header "Content-Type: application/sparql-update" \
    --post-data "PREFIX vm: <https://w3id.org/simpl/vocabulary-manager#> DELETE { GRAPH <urn:simpl:vm:graph:receipts> { ?r vm:at ?t } } WHERE { GRAPH <urn:simpl:vm:graph:receipts> { ?r vm:at ?t } FILTER(?t < \"$cutoff\"^^<http://www.w3.org/2001/XMLSchema#dateTime>) }" \
    http://localhost:3030/vocabulary/update
  wget -q -O - --header "$auth" --post-data "" "http://localhost:3030/\$/compact/vocabulary?deleteOld=true"'
echo
echo "compaction started"
