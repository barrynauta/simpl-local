#!/bin/bash
# Builds the bridge files from the upstream sample schemas: each sample plus the
# two default properties of D31 under a separate prefix (upstream reserves "simpl:").
# The output is gitignored, like the upstream samples it is made from.
set -euo pipefail
SAMPLES=${1:-$HOME/src/simpl-local/simpl-schema-manager/samples}
OUT=$(cd "$(dirname "$0")" && pwd)/fixtures
mkdir -p "$OUT"
for k in data application infrastructure; do
  f=$SAMPLES/sample-$k-offering.ttl
  shape=$(grep -oE 'gax-validation:[A-Za-z]+OfferingShape a sh:NodeShape' "$f" | head -1 | cut -d' ' -f1)
  {
    sed '8a\
@prefix dp: <https://simpl-programme.ec.europa.eu/ns/default#> .
' "$f"
    printf '\n%s sh:property [ sh:path dp:uuid ; sh:datatype xsd:string ] , [ sh:path dp:version ; sh:datatype xsd:string ] .\n' "$shape"
  } > "$OUT/bridge-$k-offering.ttl"
done
ls "$OUT"
