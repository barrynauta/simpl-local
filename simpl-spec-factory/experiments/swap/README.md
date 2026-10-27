# Swap experiments: can the generated build replace upstream?

Run on 27 Oct 2026. Configuration only: no source file of either implementation was changed. Upstream ran on copies of its Fuseki volumes; the original volumes were only ever mounted read-only.

Paths used below:

```
UP=~/src/simpl-local/simpl-vocabulary-manager      # upstream local stack
B=~/src/spec-factory-build/build                    # build working copy (images built)
S=~/src/simpl-local/simpl-spec-factory/experiments/swap
```

## Setup

```
cd $UP && docker compose down                          # upstream must be stopped for the copy
$S/setup-volumes.sh                                    # copies -> swap-up-fuseki-data, swap-up-fuseki-configuration
docker compose -p simpl-vocabulary-manager -f $UP/docker-compose.yml -f $S/upstream-on-copy.override.yml up -d --wait
cd $B && docker compose up -d --wait
```

## 1. Upstream Postman collection (`exp1-newman/`)

`collection.json` is an unchanged copy of `repos/simpl-vocabulary-manager/postman/...postman_collection.json`. `run.sh` only sets collection variables (`baseUrl`, `bearerToken`, a unique `vocabularyName`, `vocabularyFilePath`, `externalVocabularyName`).

```
$S/exp1-newman/run.sh upstream     # fake JWT, http://host.docker.internal:8086/v1
$S/exp1-newman/run.sh build        # real token: password grant, client vocabulary-tests, ga-admin
python3 $S/exp1-newman/compare.py  # status codes, assertions and response field sets side by side
```

Results: `exp1-newman/results/{upstream,build}.{txt,json}`, `results/compare.txt`.

## 2. Upstream UI on the build backend (`exp2-upstream-ui-on-build/`)

- `add-keycloak-client.sh`: adds public client `frontend-cli` (PKCE, redirect `http://localhost:4323/*`, realm-role mapper to a top-level `roles` claim) to the build's Keycloak through the admin API. Lost when the Keycloak container is removed.
- `env-config.js`: Keycloak on (server `http://localhost:4323`, realm `simpl`, client `frontend-cli`), API `/api/`, no fake cookie.
- `nginx.conf`: `/api/` to the build backend (`host.docker.internal:8096/v1/`), `/auth/` to the build's Keycloak (the upstream UI adds a legacy `/auth` prefix to Keycloak URLs).

```
$S/exp2-upstream-ui-on-build/add-keycloak-client.sh
cd $S/exp2-upstream-ui-on-build && docker compose -p simpl-vocabulary-manager --project-directory $PWD \
  -f $UP/docker-compose.yml -f ../upstream-on-copy.override.yml -f compose.override.yml up -d --no-deps vocabulary-manager-ui
cd ../../../tests && UI_URL=http://localhost:4323 UI_AUTH=oidc API_URL=http://localhost:8096/v1 \
  TOKEN_URL=http://localhost:8183/realms/simpl/protocol/openid-connect/token \
  .venv/bin/behave features/ui -f json -o ../experiments/swap/results/exp2-upstream-ui-on-build.json
../tests/.venv/bin/python $S/walk_upstream_ui.py http://localhost:4323 <ExistingName> $S/exp1-newman/data/vocabulary.ttl
```

The API calls of the run can be tallied from `docker logs simpl-vocabulary-manager-ui`. `probe_ui.py` signs in and dumps a page with its API calls.

## 3. Build UI on the upstream backend (`exp3-build-ui-on-upstream/`)

`nginx.conf` is the build's `ui/nginx.conf` with `/v1/` sent to `host.docker.internal:8086/v1/`. The build UI keeps signing in on the build's Keycloak; upstream only decodes the token.

```
cd $S/exp3-build-ui-on-upstream && docker compose -p vocabulary-manager --project-directory $PWD \
  -f $B/docker-compose.yml -f compose.override.yml up -d --no-deps ui
cd ../../../tests && UI_URL=http://localhost:4333 UI_AUTH=oidc AUTH_MODE=unsigned API_URL=http://localhost:8086/v1 \
  .venv/bin/behave features/ui -f json -o ../experiments/swap/results/exp3-build-ui-on-upstream.json
cd $B && docker compose up -d --no-deps ui        # restore
```

Compare UI reports: `python3 $S/compare_ui.py base=tests/reports/upstream-ui.json exp2=$S/results/exp2-upstream-ui-on-build.json`.

## 4. Existing data (`exp4-data/`)

- `copy-volumes.sh`: copies the ORIGINAL upstream volumes (read-only mount) to `swap-exp4-*`. Stop `fuseki-upcopy` first if it runs.
- `compose.override.yml`: adds `fuseki-upcopy` (upstream's Fuseki image and config on the copy, host port 3032) and points the build backend at `http://fuseki-upcopy:3030/ds_vocabularies`.
- `migrate.sparql`: one SPARQL Update, upstream layout to build layout, upstream graphs left in place.
- `migrate.py`: runs `migrate.sparql`, then writes the raw-text graphs the build needs for downloads (Fuseki serialises each content graph as Turtle).
- `verify.py`: compares every vocabulary and version served by upstream and by the build.

```
cd $S/exp4-data && ./copy-volumes.sh
docker compose -p vocabulary-manager --project-directory $B -f $B/docker-compose.yml -f compose.override.yml up -d --wait fuseki-upcopy backend
python3 migrate.py http://localhost:3032/ds_vocabularies
python3 verify.py http://localhost:8086/v1 http://localhost:8096/v1 "$TOKEN"
```

## Cleanup

```
docker compose -p vocabulary-manager --project-directory $B -f $B/docker-compose.yml -f $S/exp4-data/compose.override.yml down
docker compose -p simpl-vocabulary-manager -f $UP/docker-compose.yml -f $S/upstream-on-copy.override.yml down
docker volume rm swap-up-fuseki-data swap-up-fuseki-configuration swap-exp4-fuseki-data swap-exp4-fuseki-configuration
```

## Results (27 Oct 2026)

| Experiment | Result |
|---|---|
| 1. Postman collection | Same on both: 11 of 11 requests give the same status code, 6 of 7 assertions pass. The one failure (409 on the version PATCH) is the collection's own order: it deprecates the vocabulary first, which already deprecates every version. Field differences: the build adds `createdBy` and `latestVersionHref` on versions (both in the OpenAPI); `createdBy` holds `preferred_username` on the build and the `email` claim on upstream; external `namespaceUri` keeps the trailing `/` on the build, upstream strips it. |
| 2. Upstream UI on build | Sign-in by configuration works. UI suite 13 of 22 (upstream UI on upstream: 9). The same 9 scenarios fail as on upstream, all upstream UI wording or features. Gains: sign-in and viewer (environment), semantic errors and deprecated reference (build backend rules D17, D18). Every proxied API call gave the expected status. New version, download, status change work. |
| 3. Build UI on upstream | UI suite 21 of 22 (build on build: 22). The one failure: upstream accepts a class-and-property IRI (D17). Every API call gave the expected status. The viewer scenario passes because the build UI hides actions; upstream itself would accept a viewer's write. |
| 4. Existing data | Before migration the build sees 0 of 306 vocabularies (different graph names, predicates, one dataset vs two, no raw text). After `migrate.py`: 306 of 306 vocabularies and 412 of 412 versions match field for field; downloads and a new version on a migrated vocabulary work. 4 vocabularies have two ACTIVE versions in upstream data (D10), which the build's rules do not allow. |
