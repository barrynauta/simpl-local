# PSO tests on Contract Management

Run on 29 Oct 2026. An independent check: the API tests of the PSO test team (code.europa.eu `simpl/pso/pso-test/pso-test-automation-unified`, commit `ec7a546`, 27 Oct 2026), written from the same stories by people who never saw the factory. No PSO test file was changed. A profile in `profiles/` becomes the suite's `application.properties`; [`../../../pipeline/run_pso.sh`](../../../pipeline/run_pso.sh) runs it.

Features: `regression/contract-billing` (7 scenarios) and `schema-validation/contract-billing` (7), 14 scenarios on the contract manager backend. The suite also has `contract-consumption` (20 scenarios, the consumption adapter, out of scope here, see the digest) and `signer` (5, a stub here); they are not run.

The suite reaches the contract manager at `/contract/v1` through the Provider's participant gateway, with a user token. The contract manager itself takes an API key (OpenAPI `ApiKeyAuth`, D77). A small nginx container stands in for the gateway (`gateway.conf`): it forwards token requests to the build's Keycloak (realm `provider`), and adds the API key when a request carries a token. It does not check the token. That is an assumption about the real gateway, written down here because it decides the 401 scenarios.

```
PSO=<checkout of pso-test-automation-unified>
F=~/src/simpl-local/simpl-spec-factory; X=$F/contract-management/experiments/pso
$X/run-gateway.sh                     # 19500, Provider of the build
$F/pipeline/run_pso.sh $PSO $X/profiles/factorycm.properties out.json \
  regression/contract-billing schema-validation/contract-billing
$X/run-gateway-upstream.sh            # 19501, local upstream stack; same command with upstreamcm.properties
docker rm -f pso-gateway-cm pso-gateway-cm-up
```

The local upstream stack runs the backend at commit `63c6749` (7 Aug 2026; `start.sh` had fast-forwarded the June clone). That version needs six catalogue settings the stack does not set; `upstream-inert-catalog.override.yml` gives them inert values. Start it with `docker compose -f docker-compose.yml -f <that file> up -d` in the stack folder.

## Results

| Target | Passed |
|---|---|
| Factory build, Provider | **7 of 14** |
| Local upstream stack (`63c6749`) | 7 of 14, **the same 7** |

## The 7 failures, on the build and on upstream

Upstream fails the same 7 scenarios with the same status codes: 400 on the three random-id bodies, 200 on health without a token, 405 on `GET .../status`, and the same 2 environment failures.

| Scenarios | What PSO expects | Build | Reading |
|---|---|---|---|
| 3 | Random ids on search, issue and create: 404 | 400, a required body field is missing | PSO sends bodies without `assetId` or `contractNegotiationId`; the build validates the body first. The OpenAPI lists no 404 for search and issue (D12); the 404 on create comes from a story (32511). The build and upstream both answer 400 |
| 1 | `GET /health` without a token: 401 | 200 | The OpenAPI puts the whole API, `/health` included, under the API key; the digest decided health is open (D13). PSO follows the letter of the OpenAPI |
| 1 | `GET /agreements/{aid}/definitions/{did}/status`: 404 | 405 | The OpenAPI has only `PATCH` on this path. PSO tests an operation the contract does not have |
| 2 | A participant onboarded through IAA first | the suite cannot get the onboarding token | **Environment** |

## What this adds

- **Build and upstream agree on all 14 scenarios.** Where PSO fails, it fails on both, so the gap is between PSO's expectations and the running system, not between the build and upstream.
- **None of the 7 failures is a build defect.** 3 come from the order of checks on bodies the OpenAPI calls invalid, 2 from places where PSO and the OpenAPI disagree (an extra `GET`, the health check), 2 from the environment.
- **The same pattern as for the Schema Manager and the Catalogue:** where PSO and the factory differ, PSO follows the letter of the OpenAPI or what it expects of the running system. Here it differs from the running system too. Both are signals for the spec owners: the OpenAPI should list the 404s that PSO and the stories expect, and say whether health is behind the key.
