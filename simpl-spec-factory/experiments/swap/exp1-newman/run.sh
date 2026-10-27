#!/usr/bin/env bash
# Experiment 1: run the upstream Postman collection (unchanged requests) with newman.
# Usage: ./run.sh upstream|build
# Only collection variables are set on the command line (--env-var overrides).
set -euo pipefail
cd "$(dirname "$0")"
target="${1:?upstream|build}"
name="Swap$(date +%H%M%S)$RANDOM"   # unique vocabulary name per run (PascalCase)
case "$target" in
  upstream)
    base="http://host.docker.internal:8086/v1"
    # upstream only decodes the JWT for an email claim; this is the local stack's fake token
    token='eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJlbWFpbCI6ImxvY2FsQHNpbXBsLmxvY2FsIn0.devsignature' ;;
  build)
    base="http://host.docker.internal:8096/v1"
    token=$(curl -s -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin -d password=password \
      http://localhost:8183/realms/simpl/protocol/openid-connect/token | python3 -c 'import json,sys;print(json.load(sys.stdin)["access_token"])') ;;
esac
docker run --rm -v "$PWD:/etc/newman" -w /etc/newman postman/newman:6-alpine \
  run collection.json \
  --env-var "baseUrl=$base" --env-var "bearerToken=$token" \
  --env-var "vocabularyName=$name" --env-var "vocabularyFilePath=data/vocabulary.ttl" \
  --env-var "externalVocabularyName=DublinCoreTerms$RANDOM" \
  --reporters cli,json --reporter-json-export "results/$target.json" | tee "results/$target.txt"
