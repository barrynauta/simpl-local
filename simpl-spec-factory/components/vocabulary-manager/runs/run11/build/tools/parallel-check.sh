#!/usr/bin/env bash
# D82, D119: 4 parallel new versions through the proxy, which spreads them over both backend
# replicas; expect four 201, versions 2 to 5 once each, only version 5 ACTIVE.
set -euo pipefail
API_URL="${API_URL:-http://localhost:8196/v1}"
TOKEN_URL="${TOKEN_URL:-http://localhost:8193/realms/simpl/protocol/openid-connect/token}"
T=$(curl -sf -d grant_type=password -d client_id=vocabulary-tests -d username=ga-admin --data-urlencode 'password=Password1!' "$TOKEN_URL" | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
N="Par$(date +%s)$RANDOM"; W="$(mktemp -d)"
printf '@prefix ex: <https://example.org/%s#> .\nex:a ex:b ex:c .\n' "$N" > "$W/v.ttl"
curl -sf -o /dev/null -H "Authorization: Bearer $T" -F name=$N -F description=d -F changelog=c -F "vocabularyFile=@$W/v.ttl" "$API_URL/vocabularies"
for i in 1 2 3 4; do
  printf '# %s\n' "$i" >> "$W/v.ttl"; cp "$W/v.ttl" "$W/v$i.ttl"
  curl -s -o /dev/null -D "$W/h$i" -H "Authorization: Bearer $T" -F "description=p$i" -F changelog=c -F "vocabularyFile=@$W/v$i.ttl" "$API_URL/vocabularies/$N/versions" &
done
wait
grep -h '^HTTP' "$W"/h* | sort | uniq -c
grep -hi '^location' "$W"/h* | sort
curl -s -H "Authorization: Bearer $T" "$API_URL/vocabularies/$N/versions?pageSize=100" | python3 -c '
import sys, json
d = json.load(sys.stdin)
print("versions:", sorted(i["version"] for i in d["items"]), "ACTIVE:", [i["version"] for i in d["items"] if i["status"] == "ACTIVE"])'
docker compose -p vm-factory-run11 logs backend 2>/dev/null | grep -c "\"vocabularyName\":\"$N\"" | xargs echo "log lines for it:"
for c in $(docker ps --filter name=vm-factory-run11-backend- --format '{{.Names}}' | grep -v storedown); do
  echo "$c: $(docker logs "$c" 2>&1 | grep "\"vocabularyName\":\"$N\"" | grep -c 'Vocabulary version created') version(s) created"
done
rm -rf "$W"
