# PSO tests on the Schema Manager

Run on 29 Oct 2026. An independent check: the API tests of the PSO test team (code.europa.eu `simpl/pso/pso-test/pso-test-automation-unified`, commit `ec7a546`, 27 Oct 2026), written from the same stories by people who never saw the factory. No PSO test file was changed. A profile in `profiles/` becomes the suite's `application.properties`; [`../../../pipeline/run_pso.sh`](../../../../runs/run1-pipeline/run_pso.sh) runs it.

Features: `regression/schema-manager` (16 scenarios without `@skip`) and `schema-validation/schema-manager` (11), so 27 scenarios. The suite's environment health check expects a whole Simpl environment (onboarding, IAA roles) and is skipped with its own switch `-Dhealthcheck.skip=true`.

```
PSO=<checkout of pso-test-automation-unified>
F=~/src/simpl-local/simpl-spec-factory
$F/pipeline/run_pso.sh $PSO $F/schema-manager/experiments/pso/profiles/factory.properties out.json \
  regression/schema-manager schema-validation/schema-manager
```

## Results

| Target | Passed |
|---|---|
| Factory build, as built (default properties of D31 required) | 15 of 27 |
| Factory build, empty default property list (`no-default-properties.override.yml`) | **20 of 27** |
| Local upstream stack (Aug 2026) behind its UI proxy | 14 of 27 |

The default property list is configuration in the build (D31), because the real list is not published. PSO's schema file carries no default properties, so with the list in place every create fails; with an empty list the create scenarios run.

## The 7 remaining failures on the build

| Scenarios | What PSO expects | Build | Reading |
|---|---|---|---|
| 2 | A schema file without a declared resource type is rejected (PSO test case SIMPL-18293) | accepted | **The unstated rule again** (findings, step 2). It exists in a PSO test case and in upstream code, in no story |
| 2 | No token on `GET /schemas`: 400 | 401 | The OpenAPI lists no 401 for these operations; the digest chose 401 (D5). PSO follows the contract literally |
| 1 | A new version is published | stays REVOKED | Slot rule D16: another Data schema holds the slot. Upstream answers PUBLISHED (see the swap tests). PSO marks the scenario `@bug` |
| 1 | `GET /vocabularies` on the Schema Manager | 404 | The stale vocabulary operations of the OpenAPI (findings, step 0), out of scope by decision. PSO marks it `@bug` |
| 1 | Webhook subscription by a newly created Tier 1 user | 404 | Needs IAA to create the user (`@live-only`): environment |

## What this adds

- **Independent agreement:** 20 of 27 scenarios written by another team pass on a build made from the specifications plus the decision list. None of the 7 failures is a build defect: 4 scenarios hit decisions the digest took differently (D5 twice, D16, the out-of-scope vocabulary operations), 2 the rule that lives only in code and in PSO's own test, 1 the environment.
- **Where PSO and the factory read the specifications differently, PSO follows either the running system or the letter of the OpenAPI.** Both are signals for the spec owners: the resource type rule should be written down, and the OpenAPI should list the 401.

## Environment stand-ins (4 Nov 2026)

A second run, for the one environment failure: the webhook subscription by a newly created Tier 1 user (TC_SM_15, `@live-only`). No PSO test file and no build code was changed.

`stand-ins/iaa-users.mjs` fakes the IAA Users and Roles API of the governance authority, for one operation: `POST /userApi/tier1/v2/users`. It creates the user in the build's Keycloak (realm `simpl`) with a password and the realm roles of the request that the realm has, here `GA_SCHEMA_ADMIN`. `SIMPL_USER`, which the suite always adds, is not a role of the realm and is left out. The assumption: IAA creates Tier 1 users in the identity provider the Schema Manager trusts, with the roles as asked. The stand-in needs a token but does not check it or its role. It uses the throwaway Keycloak admin of the build's compose file, read when it starts, not copied. `stand-ins/gateway.conf` puts it and the Keycloak token endpoint behind one gateway on port 8186, the participant URL of the new profile `profiles/factory-stand-ins.properties`.

```
$F/schema-manager/experiments/pso/run-stand-ins.sh     # 8186, needs the build running
$F/pipeline/run_pso.sh $PSO $F/schema-manager/experiments/pso/profiles/factory-stand-ins.properties out.json \
  regression/schema-manager schema-validation/schema-manager
docker rm -f pso-gateway-sm pso-iaa-users-sm; docker network rm pso-sm
```

The build ran with `no-default-properties.override.yml`, as for the 20 of 27.

| Target | Passed |
|---|---|
| Factory build, empty default property list (29 Oct) | 20 of 27 |
| The same, with the stand-ins | **21 of 27** |

Result file: `results/pso-on-build-stand-ins.json`. TC_SM_15 passes: the new user gets a token and the build creates the webhook subscription (201). No scenario that passed before fails now. The 6 that remain are the decisions and the unstated rule in the table above.

## Run 1.2: the PSO tests as a source (4 Nov 2026)

The build after decision D64 (a RESOURCE schema declares its resource type, from TC_SM_22 and PSO test case SIMPL-18293; see the digest and `findings.md`). Same commands as for the stand-ins above, same override. No PSO test file changed.

| Target | Passed |
|---|---|
| Factory build with the stand-ins, run 1.1 state (4 Nov) | 21 of 27 |
| The same after run 1.2 | **23 of 27** |

Result file: `results/pso-on-build-run1.2.json`. TC_SM_22 passes; TC_SM_19 passes as a side effect (marked `@bug` by PSO, not a source). The 4 that remain: 2 times 400 against 401 without a token (a conflict with the OpenAPI, kept as a question), and TC_SM_08 and TC_SM_23, both marked `@bug`.
