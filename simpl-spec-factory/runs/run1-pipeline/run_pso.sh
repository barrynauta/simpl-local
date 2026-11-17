#!/bin/bash
# Runs features of the PSO test suite (code.europa.eu simpl/pso/pso-test/pso-test-automation-unified)
# against a factory build or a local upstream stack, with a profile from <candidate>/experiments/pso/profiles.
# No PSO test file is changed: the profile only becomes the suite's application.properties.
#   run_pso.sh <pso checkout> <profile file> <out.json> <feature dir> [<feature dir> ...]
set -euo pipefail
PSO=$1; PROFILE=$2; OUT=$3; shift 3
FEATURES=$(printf 'classpath:features/%s,' "$@"); FEATURES=${FEATURES%,}
ENV=$(grep -E '^env=' "$PROFILE" | cut -d= -f2)
cp "$PROFILE" "$PSO/backend-tests/src/test/resources/application.properties"
(cd "$PSO" && mvn -q -B test -pl backend-tests -Denv="$ENV" -Dhealthcheck.skip=true \
   -Dcucumber.features="$FEATURES" -Dcucumber.filter.tags="not @skip" >/dev/null 2>&1) || true
cp "$PSO/backend-tests/target/cucumber.json" "$OUT"
# access tokens of the test Keycloak can end up in step output: replace them
sed -i.bak -E 's/eyJ[A-Za-z0-9_-]{20,}(\.[A-Za-z0-9_-]+){0,2}/<token>/g' "$OUT" && rm -f "$OUT.bak"
python3 - "$OUT" <<'PY'
import json,sys,collections
c=collections.Counter()
for f in json.load(open(sys.argv[1])):
    for s in f.get('elements',[]):
        if s.get('type')!='scenario': continue
        st=[x.get('result',{}).get('status') for x in s.get('steps',[])]
        c['passed' if all(v=='passed' for v in st) else ('failed' if 'failed' in st or 'undefined' in st else 'skipped')]+=1
print(dict(c))
PY
