# Catalogue black-box test suite

Black-box tests for the Simpl-Open Catalogue (catalogue-be, query mapper adapter, catalogue
client backend, UI), written from `../specs/` and `../spec-digest.md` only. HTTP only, plus the
stub control endpoints of digest 6.4 and 6.5, so the suite runs unchanged against any
implementation.

## Contents

| Path | What |
|---|---|
| `features/api/health.feature` | `GET /health` on every service (D3) |
| `features/api/cat_publication.feature` | Publication pipeline of D14: syntax, signature, ownership, schema, SHACL, duplicates, roles |
| `features/api/cat_lifecycle.feature` | Versions (D13, D19), revoke (D16), delete (D17), list and filters (D18) |
| `features/api/cat_verification.feature` | `POST /verification` (D25) |
| `features/api/cat_schemas.feature` | Shape store (D58), schema webhook (D60), recalculation (D56), out-of-scope operations (D10) |
| `features/api/cat_quality.feature` | Quality gate through the quality stub (SIMPL-27998, D49 to D53), QAR store (D55) |
| `features/api/cat_search.feature` | Quick and advanced search on catalogue-be, discovery policy, paging, quality filters |
| `features/api/qma_local.feature` | Local search through qma-a, content negotiation, participant operations |
| `features/api/federation_search.feature` | Trustor side (qma-b inbound) and Trusted side (qma-a forwarding) of federated search |
| `features/api/federation_detail.feature` | Detail through the GA, own and federated, and the Trustor detail operation (D43) |
| `features/api/cbe_search.feature` | Grouped cbe search, partial failure, per-group paging, quality filters (D38 to D41) |
| `features/api/cbe_catalogues_detail_schemas.feature` | Catalogue list, QAR, v1 and v2 detail, schemas and quality profile, roles |
| `features/ui/*.feature` | UI stories and the UI parts of BP06, tagged `@ui @wip`, selectors of digest section 7 |
| `features/steps/common_steps.py` | HTTP, status, problem, field, result and group steps |
| `features/steps/rd_steps.py` | Build, sign, publish, version, revoke, delete resource descriptions |
| `features/steps/quality_schema_steps.py` | Quality stub control, QAR upsert, test shapes, schema events |
| `features/steps/ui_steps.py` | Browser step definitions, pending until the UI test-writer step |
| `features/support/` | Config, Keycloak tokens, HTTP client, JCS (RFC 8785), D22 signing, RD builder, QAR builder |
| `fixtures/rd/<Type>.json` | Valid RD templates per resource type (digest 6.7) |
| `fixtures/rd/broken/*.json` | Changes that break an RD in one way each, with the pipeline step they target |
| `fixtures/keys/keys.json` | The four test keys of D23 (seed and public `x`) |
| `fixtures/shapes/*.ttl.tmpl` | Test shapes with a unique name per scenario |
| `fixtures/qar/qar-template.json` | QAR in the shape of the quality stub and the contract example |
| `fixtures/queries/*.json` | Advanced search bodies, `{token}` is the run token |
| `reports/` | Progress and JSON reports per target |

Parse types for quoted values: `{x:Q}` a quoted string, `{x:QList}` a quoted comma-separated
list, `{x:Num}` a number. Inside quoted values, `{token}` is the run token of the scenario,
`{TOKEN}` the same in upper case, `{alias.id}`, `{alias.enc}` (URL-encoded) and `{alias.hash}`
refer to a resource description of the scenario, `{shape.name}` to a test shape.

## Test data (D80)

Every scenario gets a fresh run token (`t` + 10 hex characters). Every RD has the id
`did:web:test.simpl.local:<Type>:<uuid4>` and the token in `simpl:name` and
`simpl:description`. Searches use the token, so scenarios do not see each other's data and do
not depend on an empty catalogue. After each scenario the suite deletes the RDs it published
(by `sdHash`, as the owner, also revoked ones), the shapes it uploaded and the quality stub
entries it set. Not cleaned up: schema statuses remembered by the webhook (unique names are used),
log lines, Keycloak users and clients, seed shapes and seed schemas.

## Tags

| Tag | Meaning |
|---|---|
| `@api`, `@ui` | API scenarios, UI scenarios |
| `@cat`, `@qma`, `@cbe` | Backend under test |
| `@federation` | Needs data spaces B and C (cat-b, qma-b, qma-c) and the slow and inactive catalogues |
| `@quality` | Drives the quality stub or needs a quality profile from it |
| `@stub` | Calls a stub directly |
| `@rbac` | Needs real tokens: roles, participants, realms, federation tokens. Skipped with `AUTH_MODE=none` |
| `@slow` | Waits on a timeout (3 s federation, 10 s quality scoring) |
| `@wip` | Not run unless `RUN_WIP=true` (all UI scenarios) |
| `@SIMPL-nnnnn` | Story key |
| `@bp05b`, `@bp06` | Business process |
| `@openapi-only` | Operation without a story |
| `@Dnn` | Digest decision the scenario depends on |
| `@health`, `@publication`, `@lifecycle`, `@verification`, `@schemas`, `@search`, `@detail` | Area |

No scenario needs to run serially: all data is scoped by the run token and by unique ids.

## Environment variables

All are documented in the docstring of `features/environment.py`. The main ones:

| Variable | Default |
|---|---|
| `AUTH_MODE` | `oidc` (`none` skips `@rbac` and sends the user's attributes as `User-Attributes`) |
| `CAT_A_URL`, `CAT_B_URL` | `http://localhost:18101`, `http://localhost:18201` |
| `QMA_A_URL`, `QMA_B_URL`, `QMA_C_URL` | `http://localhost:18102/v1`, `http://localhost:18202/v1`, `http://localhost:18302/v1` |
| `CBE_A_URL` | `http://localhost:18103` (`/v1` and `/v2` are appended) |
| `UI_A_URL` | `http://localhost:18104` |
| `SCHEMA_STUB_URL`, `QUALITY_STUB_URL` | `http://localhost:18401`, `http://localhost:18402` |
| `KEYCLOAK_URL` | `http://localhost:18080` |
| `TEST_CLIENT_ID`, `USER_PASSWORD` | `test-client`, `Passw0rd!` |
| `QMA_A_FEDERATION_SECRET`, `QMA_C_FEDERATION_SECRET` | the secrets of digest 5.2 |
| `SKIP_TAGS` | empty; comma-separated tags to skip as "missing in this environment" |
| `HTTP_TIMEOUT` | `30` seconds |
| `KEEP_TEST_DATA`, `TEST_VERBOSE`, `RUN_WIP` | `false` |

## Setup

```bash
cd catalogue/tests
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/behave --dry-run -f null          # every step defined, no errors
```

## Run against the factory build

Start the reference environment (`docker compose -p simpl-catalogue-factory up -d --wait`),
then:

```bash
.venv/bin/behave -f progress -o reports/factory-progress.txt -f json.pretty -o reports/factory.json -f progress
```

Leave out the slow scenarios with `-t ~@slow`. The UI scenarios stay skipped until the browser
steps exist.

## Run against the local upstream stack

The upstream stack (`~/src/simpl-local/simpl-catalogue`, May 2026) has one data space, no
Keycloak, no stubs and no client backend. Start it with `./start.sh` there (set
`NEO4J_HTTP_PORT` and `NEO4J_BOLT_PORT` if 7474 and 7687 are taken), then:

```bash
AUTH_MODE=none CAT_A_URL=http://localhost:8081 QMA_A_URL=http://localhost:8084/v1 \
SKIP_TAGS=federation,cbe,quality,stub,ui \
.venv/bin/behave -f progress -o reports/upstream-progress.txt -f json.pretty -o reports/upstream.json -f progress
```

Stop it with `./stop.sh`. Failures there are expected; the baseline in `reports/` classifies
them (environment difference, decision difference, newer story).
