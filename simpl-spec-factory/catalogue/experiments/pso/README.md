# PSO tests on the Catalogue

Run on 29 Oct 2026. An independent check: the API tests of the PSO test team (code.europa.eu `simpl/pso/pso-test/pso-test-automation-unified`, commit `ec7a546`, 27 Oct 2026), written from the same stories by people who never saw the factory. No PSO test file was changed. A profile in `profiles/` becomes the suite's `application.properties`; [`../../../pipeline/run_pso.sh`](../../../pipeline/run_pso.sh) runs it.

Features: the regression and schema-validation features of `fc-catalogue`, `xfsc-catalogue`, `xfsc-advsearch`, `catalogue-search` and `fc-proxy`, 52 scenarios without `@skip`. The suite's environment health check is skipped with its own switch `-Dhealthcheck.skip=true`.

The suite expects one participant gateway with fixed mount paths (`/fc/`, `/xfsc-advsearch-be/`, `/sdtooling-api/`, `/realms/`). A small nginx container stands in for it and maps the paths onto the stack under test (`gateway.conf` for the build, `gateway-upstream.conf` for upstream). PSO roles are mapped to the build's test users in the profile; the local upstream stack has no Keycloak and checks no tokens, so its run takes tokens from the build Keycloak.

```
PSO=<checkout of pso-test-automation-unified>
F=~/src/simpl-local/simpl-spec-factory; X=$F/catalogue/experiments/pso
$X/run-gateway.sh                     # 18500, build of data space A
$F/pipeline/run_pso.sh $PSO $X/profiles/factorycat.properties out.json \
  regression/xfsc-catalogue regression/fc-catalogue regression/xfsc-advsearch regression/fc-proxy \
  schema-validation/xfsc-catalogue schema-validation/fc-catalogue schema-validation/xfsc-advsearch \
  schema-validation/catalogue-search schema-validation/fc-proxy
$X/run-gateway-upstream.sh            # 18501, local upstream stack; same command with upstreamcat.properties
docker rm -f pso-gateway-cat pso-gateway-cat-up
```

## Results

| Target | Passed |
|---|---|
| Factory build, data space A | **32 of 52** |
| Local upstream stack (May 2026) | 17 of 52 |

## The 20 failures on the build

| Scenarios | What PSO expects | Build | Reading |
|---|---|---|---|
| 10 | A self-description signed live, through SD Tooling and the signer | the suite cannot prepare it | **Environment.** The same 10 fail on upstream. Nine stop on the live signing step, one on a constants class of the suite that does not initialise outside a full environment |
| 4 | `GET /schemas/latest`, `GET /verification`, `POST /query` (twice: malformed query, no token) | 501 | Out of scope by decision (D10): the build answers 501 before it checks the token. Upstream implements them |
| 1 | `POST /query/search` with an empty body: 400 | 404 | The operation is in no spec the factory got. Upstream has it |
| 2 | Error body with a `message` field | Problem JSON without `message` | D2 chose RFC 9457 Problem for every error, as the newer parts of the OpenAPI do. PSO checks the older `Error {code, message}` shape |
| 2 | A user with the IAA role `T2IAA_M` posts a resource and reads the schemas | 403 | The OpenAPI lists publisher roles and `GA_SCHEMA_*` for these operations (D66); PSO uses a role the catalogue OpenAPI does not name. Upstream checks no roles |
| 1 | The "Gaia-X Core" ontology in the schema list | not there | Seed data: neither the build nor upstream has it loaded. Same failure on upstream |

## The 35 failures on upstream

| Scenarios | Reading |
|---|---|
| 12 | No token: PSO expects 401, the local stack answers 200, 400, 404, 405, 422 or 501. It runs without Keycloak, so this says nothing about upstream in a full environment |
| 11 | The May stack has no client backend (`/xfsc-advsearch-be/`) |
| 10 | Live signing, as on the build |
| 1 | Gaia-X Core ontology, as on the build |
| 1 | `GET /schemas`: the response carries `vocabularies: null`, which PSO's own response schema does not allow. PSO marks the scenario `@bug` |

## What this adds

- **Independent agreement:** of the 42 scenarios that can run outside a full environment (52 minus the 10 signing scenarios), 32 pass on the build. None of the 10 others is a build defect: 5 hit operations the factory left out or never saw (D10, `/query/search`), 4 decisions the digest took differently (D2 twice, D66 twice), 1 seed data.
- **PSO tests partly against an older contract.** The `message` field is the older error shape; the role `T2IAA_M` is not in the catalogue OpenAPI. Where PSO and the factory differ, PSO follows the running system, as for the Schema Manager.
- **Upstream on a local stack is a weak baseline here.** 23 of its 35 failures come from what the May stack lacks (Keycloak, client backend), not from what it does.
