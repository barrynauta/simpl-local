# PSO tests as the highest specification (run 3, 16 Nov 2026)

Step 3 of [`../../../tests-as-spec.md`](../../../tests-as-spec.md), with one change asked by Barry: the PSO scenarios are taken as **bullet-proof**. They outrank the OpenAPI, the stories and the digest for what they check. In the 4 Nov plan the specifications won.

## Set-up

| Part | What |
|---|---|
| PSO suite | `pso-test-automation-unified` commit `ec7a546` (the contract-billing features are unchanged up to `57ce7d2d`, 13 Nov), features `regression/contract-billing` and `schema-validation/contract-billing`, 14 scenarios |
| Hold-out split | [`split.py`](./split.py), seed 20261116, half of each feature file. **Visible (8):** TC_CB_02, 04, 06, 07, TCCB_SV03, 04, 05, 07. **Hidden (6):** TC_CB_03, 05, 08, TCCB_SV02, 06, 08 ([`split.json`](./split.json)) |
| Input to the writers | [`input-pso/`](./input-pso/): the visible scenarios only, the request each step sends, the PSO error schema, the gateway rules |
| Base digest | the run 1.1 digest ([`spec-digest-run1.1.md`](./spec-digest-run1.1.md), commit 497fdab). The current digest could not be used: its section 1.1 and questions 18 and 19 (run 1.2) name hidden scenarios |
| Step 1, digest | a fresh agent in its own workspace, visible half only: [`spec-digest.md`](./spec-digest.md), [`DIGEST-NOTES.md`](./DIGEST-NOTES.md) |
| Step 2, test writer | a fresh agent, the run 1.1 suite plus the new digest, no build: [`tests-delta/`](./tests-delta/) ([`PSO-RUN.md`](./tests-delta/PSO-RUN.md), patch against 497fdab) |
| Step 3, code writer | a fresh agent, a build from scratch: [`../../generated/run3-pso-spec/`](../../generated/run3-pso-spec/), brief [`brief-code-writer.md`](./brief-code-writer.md), compose project `cm-factory-pso` |
| Isolation | the tool calls of the three agents were checked: no file read outside their workspaces, no web access. The code writer used the browser on its own UI at localhost only |

## What the digest step made of 8 scenarios

12 rows changed or added and 3 open questions, from 8 scenarios:

- **D12 revised:** an unknown path id gives 404, not 400, on the six CM operations that work on an existing record. The OpenAPI lists no 404 there, so this overrides the published contract (question 18).
- **D84 new:** a fixed order of checks: API key (401), path format, body syntax, whether the record exists (404), body fields, party, state.
- **D13, D65 revised:** CM health needs the key (UIBE stays open; question 19).
- **D11, D25 revised, D85 new:** the 401 problem body, `{}` counts as no body, the gateway stand-in as a decision.
- **Confirmations only:** D19, D20, D34.
- **Judgement call:** the 404 rule was **not** extended to `/search` and `/create`, because they do not work on an existing record.

## Results

| Suite | run 1.1 + stand-ins (4 Nov) | run 1.2 (PSO as a lower source) | **run 3 (PSO on top)** |
|---|---|---|---|
| Own API scenarios | 345 of 345 | 345 of 345 | **361 of 361** (16 new) |
| Own UI scenarios | 38 of 38 | 38 of 38 | **38 of 38** |
| PSO visible half | 6 of 8 | 7 of 8 | **8 of 8** |
| PSO hidden half | 2 of 6 | 2 of 6 | **2 of 6** |
| PSO total | 8 of 14 | 9 of 14 | 10 of 14 |

Run 3 is a new build from scratch, not a patch on run 1. It met its finish line on the first start with 4 fix iterations (smoke 33 of 33, `pso-check.sh` 8 of 8, `mvn verify` 63 tests) and passed the updated own suite with no clarification round.

The 4 hidden failures, the same as on every earlier build and on upstream:

| Scenario | PSO expects | Run 3 | Why the visible half did not help |
|---|---|---|---|
| TC_CB_03, `POST /agreements/<uuid>/search`, body `{}` | 404 | 400 | The digest kept 404 to operations on an existing record. A search with a random negotiation id is not one. A reasonable reading, and the wrong guess for PSO |
| TC_CB_05, `POST .../create`, body `{}` | 404 | 400 | The same judgement call |
| TC_CB_08, TCCB_SV08, `GET /agreements/{aid}/definitions/{did}/status` | 404 | 405 | The OpenAPI has only `PATCH` on this path, and no visible scenario uses the `GET`. Nothing in the input could lead a builder to it |

Result files: [`results/`](./results/) (PSO cucumber JSON, own suite JSON and progress).

## What this shows

1. **The visible tests are met, and the hidden ones are not.** All 8 visible scenarios pass, and 2 of 6 hidden ones pass, as before. Using the tests as a specification raised the PSO score from 9 to 10 of 14, and the whole gain is in scenarios the builder saw.
2. **Tests generalise only as far as the person reading them.** Two hidden failures depend on one judgement: does "404 for an unknown id" reach operations that do not look a record up? The tests show the rule on one operation, so the digest writer had to guess how far it reaches. Two others test an operation that is in no specification at all. No reading of the visible half finds it.
3. **The tests are cheap to satisfy and add few decisions.** 8 scenarios gave 2 new rows and changed the meaning of 10. Every one is about error codes, the order of checks and authentication. None says anything about the negotiation, the lifecycle, the events between the parties or the UI, which is where the 83 decisions of run 1 sit.
4. **Treating tests as bullet-proof overrides the published contract.** The 404 rule goes against the OpenAPI, and health behind the key goes against what liveness probes need. Both are recorded as questions for the spec owners (18 to 20), because the official tests and the official API contract disagree.
5. **Nothing broke.** The own suite went up from 383 to 399 scenarios and all pass. The change was safe because it touched only the edges.
