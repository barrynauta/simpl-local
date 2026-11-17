# Input specs: Contract Management

Frozen input set for the fourth factory candidate, collected **2026-10-29**. As for the earlier candidates, it holds only material that is published or will be published: no review comments, no Confluence pages. The factory works from these files only. Do not refresh them in place: a new copy is a new, dated input set.

Scope of the input: the Jira component "Contract Management", the most business logic of the non-common components. Several parties negotiate, sign, store, verify, suspend, revoke and terminate usage contracts, with a signer, a wallet, Kafka and the EDC connector around it.

## What is here

| Folder | Content | Source |
|---|---|---|
| `stories/` | 36 user stories: every live story (not cancelled) of the Jira component "Contract Management" (10 cancelled stories left out; the component "Contract" adds no live story). Key, summary, status, dates and the description with the acceptance criteria; same layout as the earlier candidates. Review comments, links and authors are left out. | Jira SIMPL, collected with [`pipeline/collect_stories.py`](../../pipeline/collect_stories.py): a personal access token and a local model (Mistral Small 3.2 through Ollama). 35 converted by the model, 1 by the plain fallback (SIMPL-35889); all with a word match of 1.000 against the source |
| `api/contract-manager-be/` | OpenAPI of the contract manager backend, version 1.1, 12 operations | code.europa.eu `simpl/simpl-open/governance/contract-management/contract-lifecycle-management/contract-manager/contract-manager-be`, commit `b6c32c0` (main, 2026-10-20), folder `openapi/` |
| `api/contract-ui-be/` | OpenAPI of the contract UI backend, version 1.0.0, 5 operations, plus two PlantUML files from the same folder (`cdm.puml` data model, `flow.puml` flow) | `.../contract-manager/contract-ui-be`, commit `b639f7d` (main, 2026-10-20) |
| `api/contract-consumption-adapter-be/` | OpenAPI of the contract consumption adapter (Tier 1), 9 operations. Context only: no story of this component describes it | `simpl/simpl-open/integration/resource-sharing/resource-sharing-runtime/resource-consumption/contract-consumption-adapter-be`, commit `6845f63` (main, 2026-10-19) |
| `functional/bp07-*/` | Business process BP07 "Establish usage contract" (8 sub-processes, BP71 to BP78, and the dynamic view) | code.europa.eu `simpl/simpl-open/foundations/business-processes`, commit `8090f32` (main, 2026-10-23), without the `media/` diagrams |
| `functional/bp10-*/` | Business process BP10 "Provider bills consumer based on resource consumption" (README only) | same commit |

## Not included, on purpose

- **Upstream source code and tests**, also the repository READMEs. Spec-only generation.
- **The 10 cancelled stories** and the Helm route values next to the OpenAPI.
- **Review comments** on the stories, roadmap item descriptions in Confluence (not published).
- **Diagrams** of the business processes (`media/`), and any Figma design linked from the stories.

## First observations (input to [`findings.md`](../findings.md))

- **No published event contract.** The parties exchange contract offers and signed contracts over Kafka (with the EDC connector), but there is no AsyncAPI or topic description in the published material. The topic names exist only in code.
- **Three replaced stories are still live** next to their replacements: SIMPL-19824 and SIMPL-32509 (verify an existing usage contract), SIMPL-19829 and SIMPL-32511 (create a usage contract offer), SIMPL-22690 and SIMPL-32505 (consumer accepts an offer via UI). SIMPL-35888 and SIMPL-35889 are both "API for VC exchange"; the second has no acceptance criteria and a body about getting a user by id.
- **Few stories are settled:** 11 are new backlog items, 7 "FOR SC TO RESPOND", 4 blocked, 4 for DG review; 5 are "ready for implementation check".
- **The UI backend and the contract manager overlap:** both have `POST` and `PATCH` operations on `/contractOffers/{contractNegotiationId}`.
- **Signing and wallets are external:** signing or the digital wallet is in the title of 11 stories and in the text of 19, but the signer, wallet and VC issuer contracts are not in this input set.
- The local upstream stack (`simpl-contract`) builds from the earlier `development/contract-billing` repositories, a June 2026 version, with signer, wallet, EDC and Keycloak stripped.

## Story index

| Key | Kind | Given/When/Then | Status | Summary |
|---|---|---|---|---|
| [SIMPL-19824](stories/SIMPL-19824.md) | API | yes | DevTeam PBI In Progress | [Replaced][API] - API to verify existing usage contract |
| [SIMPL-19829](stories/SIMPL-19829.md) | API | yes | DevTeam PBI In Progress | [Replaced] [API] - API to create usage contract offer |
| [SIMPL-19927](stories/SIMPL-19927.md) |  | yes | DevTeam PBI is ready for Implementation check | Consumer stores signed usage contract securely in digital wallet |
| [SIMPL-19933](stories/SIMPL-19933.md) |  | yes | DevTeam PBI blocked | Provider Contract Manager to validate the usage contract offer signed by the Consumer |
| [SIMPL-19934](stories/SIMPL-19934.md) |  | yes | DevTeam PBI is ready for Implementation check | Provider signs the received contract previously signed by the Consumer |
| [SIMPL-19938](stories/SIMPL-19938.md) |  | yes | DevTeam PBI is ready for Implementation check | Provider stores signed usage contract securely in digital wallet |
| [SIMPL-21924](stories/SIMPL-21924.md) | UI | yes | PBI Defined | [UI] Consumer requests a contract offer for using a resource via UI |
| [SIMPL-21928](stories/SIMPL-21928.md) |  | yes | DevTeam PBI is ready for Implementation check | Consumer signs the usage contract offer received from a Provider |
| [SIMPL-21948](stories/SIMPL-21948.md) |  | yes | DevTeam PBI blocked | Consumer validates the usage contract signed by the Provider |
| [SIMPL-21963](stories/SIMPL-21963.md) |  | yes | PBI Defined | Consumer delivers the signed usage contract offer to the Provider |
| [SIMPL-21966](stories/SIMPL-21966.md) |  | yes | DevTeam PBI blocked | Provider notifies Consumer about incorrectly signed usage contract offer |
| [SIMPL-22690](stories/SIMPL-22690.md) | UI | yes | DevTeam PBI blocked | [Replaced] A Consumer accepts a usage contract offer via UI |
| [SIMPL-28846](stories/SIMPL-28846.md) |  | yes | DevTeam PBI is ready for Implementation check | Store uploaded Contract Clauses documents and generate hash metadata |
| [SIMPL-30959](stories/SIMPL-30959.md) | UI | yes | FOR SC TO RESPOND | A Consumer consults an overview of their usage contracts via UI |
| [SIMPL-30960](stories/SIMPL-30960.md) | UI | yes | PBI For DG Review | A Consumer consults the details of a usage contract via UI |
| [SIMPL-31047](stories/SIMPL-31047.md) | UI | yes | PBI For DG Review | A Provider chooses the action to update the contract consent assertion status of a selected usage contract in their contract manager UI |
| [SIMPL-31053](stories/SIMPL-31053.md) | UI | yes | FOR SC TO RESPOND | A Provider or Consumer requests the revocation of a usage contract in their contract manager UI |
| [SIMPL-31076](stories/SIMPL-31076.md) | UI | yes | FOR SC TO RESPOND | The responding Participant responds to a revocation request of a usage contract in their contract manager UI |
| [SIMPL-31115](stories/SIMPL-31115.md) | UI | yes | New Product Backlog Item | A Provider suspends a usage contract by providing a reason in their contract manager UI |
| [SIMPL-31116](stories/SIMPL-31116.md) | UI | yes | New Product Backlog Item | A Consumer requests the reactivation of a suspended usage contract in their contract manager UI |
| [SIMPL-31117](stories/SIMPL-31117.md) | UI | yes | New Product Backlog Item | A Provider reactivates a suspended usage contract in their contract manager UI |
| [SIMPL-31118](stories/SIMPL-31118.md) | UI | yes | New Product Backlog Item | A Provider terminates a usage contract by providing a justification related to the termination conditions in their contract manager UI |
| [SIMPL-32505](stories/SIMPL-32505.md) | UI | yes | New Product Backlog Item | A Consumer accepts a usage contract offer via UI |
| [SIMPL-32509](stories/SIMPL-32509.md) | API | yes | New Product Backlog Item | [API] - API to verify existing usage contract |
| [SIMPL-32511](stories/SIMPL-32511.md) | API | yes | New Product Backlog Item | [API] - API to create usage contract offer |
| [SIMPL-34633](stories/SIMPL-34633.md) | API | yes | FOR SC TO RESPOND | [API] A Participant generates aggregated usage contract data for auditing purposes |
| [SIMPL-35440](stories/SIMPL-35440.md) | API | yes | PBI For DG Review | [API] Contract Manager returns VC for contract for given negotiation ID |
| [SIMPL-35526](stories/SIMPL-35526.md) | UI | yes | FOR SC TO RESPOND | Contract Manager prepares contract data for contract UI |
| [SIMPL-35584](stories/SIMPL-35584.md) | API | yes | PBI For DG Review | [API] Contract Manager validates and signs contract agreement |
| [SIMPL-35660](stories/SIMPL-35660.md) |  | yes | For PSO Review | Contract Manager signs contract on consumer side |
| [SIMPL-35677](stories/SIMPL-35677.md) | API | yes | FOR SC TO RESPOND | [API] Contract Manager verify contract on consumer side |
| [SIMPL-35692](stories/SIMPL-35692.md) | UI | yes | FOR SC TO RESPOND | Contract UI backend will terminate unsigned contract offers after X days |
| [SIMPL-35858](stories/SIMPL-35858.md) | API | yes | New Product Backlog Item | [API] Create API for contract existence verification |
| [SIMPL-35863](stories/SIMPL-35863.md) | API | yes | New Product Backlog Item | [API] Create API for contract validation result |
| [SIMPL-35888](stories/SIMPL-35888.md) | API | yes | New Product Backlog Item | [API] Create API for VC exchange |
| [SIMPL-35889](stories/SIMPL-35889.md) | API | no | New Product Backlog Item | [API Definition] Create API for VC exchange |
