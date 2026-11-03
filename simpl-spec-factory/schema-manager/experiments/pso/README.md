# PSO tests on the Schema Manager

Run on 29 Oct 2026. An independent check: the API tests of the PSO test team (code.europa.eu `simpl/pso/pso-test/pso-test-automation-unified`, commit `ec7a546`, 27 Oct 2026), written from the same stories by people who never saw the factory. No PSO test file was changed. A profile in `profiles/` becomes the suite's `application.properties`; [`../../../pipeline/run_pso.sh`](../../../pipeline/run_pso.sh) runs it.

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
