# Inventory review: contract-management

Step 12b. A fresh reviewer checked [`inventory.json`](./inventory.json) against `specs/` and added the links in [`links.json`](./links.json). Only `specs/` and `inventory/` were read. This step extracts and links; it does not judge the specs.

## Extraction issues

The three issues in `inventory.json`:

| Issue | Parser error or real | What to change |
|---|---|---|
| SIMPL-31076: label AC4 used 2 times; ids suffixed | Real. The story has two headings "AC4", one for the Accept case and one for the Reject case. | Nothing. `AC4.1` and `AC4.2` match the source. |
| SIMPL-31116/AC2: no Then clause | Real. After the When, the last clause is written as "And I can observe that the status of the usage contract has changed to 'Reactivation requested'". There is no Then in the source. | Nothing. |
| SIMPL-35889: no acceptance criteria found | Real. The story is an API definition task. Its "Acceptance Criteria" is a checklist (spec available, ITB compliance, mocks, YAML, versioning) without Given, When or Then. | Nothing for the criteria. See row c below for the user story of this story. |

None of the three is a parser error.

### Further parser findings (not flagged by the script)

Found while reading the sources for the spot check and the links.

| # | Where | What the script did | What it should do |
|---|---|---|---|
| a | SIMPL-19938 | The story holds two versions of AC1 to AC4: first as list items (`- **AC1 - ...:**`), then as plain bold headings. It also has `- **AC5 - Contract validation failure: (ADDED)**`. The script did not see headings behind a list marker. The list-item versions became derived `AC5` to `AC8`, and the explicit AC5 became derived `AC9`. All 5 "derived" ids in the totals come from this. | Accept an AC heading behind a list marker. Then flag AC1 to AC4 as used twice (suffix them) and keep AC5 as an explicit label. |
| b | SIMPL-22690 | The first AC1 is struck in Jira (`-*AC1 - Successful usage contract offer review*-`, clauses as list items `* -*Given* ...-`). It is dropped completely: not in the ACs, not counted in `acs_struck` (0). | Recognise a struck heading that starts with `-*` and struck list-item clauses. Keep the criterion with `struck: true`. |
| c | SIMPL-35889 | `user_story` has only `as a`, taken from rule 23 of the review checklist ("As an exception, use the standardized name for the business identifier ..."). | Look for As a / I want / So that only before the acceptance criteria, or only accept a user story with all three parts. |
| d | SIMPL-19824 | The Technical Info names `POST /agreements/\{contractNegotiationId}/search`. The escaped brace split it into the paths `/agreements` and `/search`, so no operation was named. | Unescape `\{` before matching paths. The story then names `contract-manager-be:POST /agreements/{contractNegotiationId}/search`. |
| e | SIMPL-35526, SIMPL-35660, SIMPL-35677 | Operations are matched on the path only, across all APIs and methods. SIMPL-35526 gets 4 operations, but the text names the Contract Manager `.../create` and "the Contract UI backend ... (POST /contractOffers/{contractNegotiationId})", not the two GET or Contract Manager POST variants. SIMPL-35660 also gets `contract-ui-be:PATCH ...`, while the story is about the Contract Manager endpoint. SIMPL-35660 says POST and SIMPL-35677 says POST, while the APIs have PATCH and PUT. | Use the method when the text gives one. Report a method mismatch as an extraction issue. Prefer the API that the sentence names ("Contract UI backend", "Contract Manager"). |
| f | SIMPL-35692 | Names `/contractOffers/{contractNegotiationId}/reject`, a concrete value of `{operation}`. Not matched. | Optional: match a concrete segment against a path parameter. |
| g | BP07 README, BP07 dynamic view, BP10 | No detailed requirements, but no issue is raised, although the script says it flags "a process without detailed requirements". BP10 has only high-level requirements 10.1 and 10.2, so no BP10 id exists in the inventory. | Either flag them, or mark overview files as such so the flag does not apply. |
| h | SIMPL-35440, 35526, 35584, 35660, 35677, 35692 | These name BP07 steps (for example "steps BP07.09 and BP07.10"). The script does not capture them. These step numbers (BP07.01 to BP07.12) are not the requirement ids (7.1.1 to 7.8.1). | Capture BP step references in `refs`, as a separate kind. |
| i | SIMPL-31115, SIMPL-31116 | Headings keep emphasis markers, for example `AC2 -** *A Consumer requests ...` and `AC3 -** **A Provider observes ...`. Clauses are fine. | Strip `*` from headings. |
| j | SIMPL-32509 | The labels are AC1, AC2, AC4, AC5, AC6. There is no AC3 in the source. This is real, but not flagged. | Optional: flag gaps in AC numbers, as for requirement numbers. |

## Spot check

Ten stories picked with a seeded random sample (`random.seed(12)`). Every Given, When, Then and And clause in the source was compared, in order, with the clauses in `inventory.json`, after removing markdown. The number of criteria was also checked against the headings.

| Story | Result |
|---|---|
| SIMPL-19824 | OK. 6 ACs, 55 clauses, same words. |
| SIMPL-21948 | OK. 10 ACs, 60 clauses. |
| SIMPL-21963 | OK. 4 ACs, 12 clauses. |
| SIMPL-22690 | Difference. The struck AC1 ("Successful usage contract offer review", 5 clauses) is missing (finding b). The live AC1 and AC2 are OK, 14 clauses. |
| SIMPL-28846 | OK. 6 ACs, 32 clauses. The doubled "Acceptance Criteria" heading did no harm. |
| SIMPL-31047 | OK. 4 ACs, 21 clauses. |
| SIMPL-31076 | OK. 5 ACs (AC4.1, AC4.2), 29 clauses. |
| SIMPL-32505 | OK. 3 ACs, 16 clauses. |
| SIMPL-35677 | OK. 4 ACs, 14 clauses. |
| SIMPL-35863 | OK. 2 ACs, 8 clauses. Blank lines between clauses were handled. |

9 of 10 are OK. The same comparison over all 36 stories gives one more difference: the extra "And I can decide ..." line in SIMPL-30960, which the script rightly put in the user story (`extra`), not in a criterion. SIMPL-19938 passes the clause comparison but has wrong labels (finding a).

## Links

Rules used:

- `named`: the story text names the path or the endpoint. Where the method in the text differs from the API, the reason says so.
- `inferred`: matched by meaning; the reason quotes the words.
- A criterion is linked to an operation when it describes a call to that operation, including error cases (401, 405 and so on).
- A criterion is linked to a business process requirement when its Then describes the business step (success or a business failure). Pure technical cases (unauthenticated, wrong method, system error, "called on the wrong side") get operation links only.
- Each link is stored once, from the criterion to the operation or requirement. The reverse view is the table below.

### By operation and requirement

`*` marks a named link.

| Target | ACs | Criteria |
|---|---|---|
| `BP7.1.1` | 4 | 21924/AC1, 21924/AC2, 21924/AC3, 21924/AC4 |
| `BP7.2.1` | 2 | 21924/AC2, 21924/AC3 |
| `BP7.2.2` | 7 | 21924/AC2, 21924/AC4, 19824/AC1, 19824/AC2, 32509/AC1, 32509/AC2, 35858/AC1 |
| `BP7.3.1` | 4 | 19829/AC1, 32511/AC1, 32511/AC2, 32511/AC3 |
| `BP7.4.1` | 8 | 30959/AC1, 30960/AC1, 35526/AC1, 22690/AC1, 22690/AC2, 32505/AC1, 32505/AC2, 32505/AC3 |
| `BP7.4.2` | 1 | 35660/AC3 |
| `BP7.5.1` | 5 | 21928/AC1, 21928/AC2, 21928/AC3, 21928/AC4, 35660/AC1 |
| `BP7.5.2` | 4 | 35660/AC1, 21963/AC1, 21963/AC4, 35440/AC1 |
| `BP7.6.1` | 15 | 19933/AC1 to AC10, 21963/AC2, 21963/AC3, 35584/AC1, 35888/AC1, 35863/AC1 |
| `BP7.7.1` | 2 | 19934/AC1, 35584/AC1 |
| `BP7.7.2` | 9 | 19938/AC1 to AC9 |
| `BP7.7.3` | 1 | 35440/AC1 |
| `BP7.8.1` | 14 | 21948/AC1 to AC10, 35677/AC1, 35677/AC3, 35888/AC1, 35863/AC1 |
| `contract-consumption-adapter-be:POST /contracts` | 1 | 21924/AC2 |
| `contract-manager-be:GET /agreements/{contractAgreementId}` | 1 | 19938/AC1 |
| `contract-manager-be:PATCH /agreements/{contractAgreementId}/definitions/{contractDefinitionId}/status` | 1 | 21963/AC1 |
| `contract-manager-be:PATCH /contractOffers/{contractNegotiationId}/{operation}` | 5 | 35660/AC1\*, 35660/AC2\*, 35660/AC3\*, 35692/AC1\*, 22690/AC1 |
| `contract-manager-be:POST /agreements/{contractNegotiationId}/search` | 6 | 19824/AC1\* to AC6\* |
| `contract-manager-be:POST /contractOffers/{contractNegotiationId}` | 1 | 35526/AC1 |
| `contract-manager-be:POST /credentials/agreements/{contractAgreementId}/definitions/{contractDefinitionId}/create` | 11 | 19829/AC1\* to AC6\*, 32511/AC1\* to AC3\*, 35526/AC1\*, 35526/AC2\* |
| `contract-manager-be:POST /credentials/agreements/{contractAgreementId}/definitions/{contractDefinitionId}/issue` | 5 | 21963/AC1\*, 21928/AC1, 21928/AC2, 19934/AC1, 19934/AC2 |
| `contract-manager-be:PUT /verifications/{contractNegotiationId}` | 4 | 35677/AC1\* to AC4\* |
| `contract-ui-be:GET /contractOffers` | 3 | 30959/AC1, 30959/AC2, 35692/AC4 |
| `contract-ui-be:GET /contractOffers/{contractNegotiationId}` | 2 | 32505/AC1, 30960/AC1 |
| `contract-ui-be:PATCH /contractOffers/{contractNegotiationId}/{operation}` | 3 | 22690/AC1, 22690/AC2, 32505/AC2 |
| `contract-ui-be:POST /contractOffers/{contractNegotiationId}` | 1 | 35526/AC1\* |

Endpoints named in stories that are not in the three OpenAPI files, so they have no link: `POST /resourceDescriptions/{resourceDescriptionId}/validateExistenceContract` (SIMPL-32509), `GET /verifications/{contractNegotiationId}/vc` (SIMPL-35440), `POST .../validateAndIssue` (SIMPL-35584), and the EDC endpoints `/isPresent/...` (SIMPL-35858), `/isValid/...` (SIMPL-35863, 35677), `/vcExchange/{contractNegotiationId}/request` (SIMPL-35888, 35584), `/signed/...` (SIMPL-35660, 35692).

## Coverage from the links

### Criteria with no operation and no requirement (54)

- SIMPL-19927/AC1, AC2, AC3, AC4
- SIMPL-21966/AC1, AC2, AC3, AC4, AC5, AC6
- SIMPL-28846/AC1, AC2, AC3, AC4, AC5, AC6
- SIMPL-30959/AC3
- SIMPL-31047/AC1, AC2, AC3, AC4
- SIMPL-31053/AC1, AC2, AC3
- SIMPL-31076/AC1, AC2, AC3, AC4.1, AC4.2
- SIMPL-31115/AC1, AC2, AC3
- SIMPL-31116/AC1, AC2, AC3
- SIMPL-31117/AC1, AC2, AC3
- SIMPL-31118/AC1, AC2, AC3
- SIMPL-32509/AC4, AC5, AC6
- SIMPL-34633/AC1
- SIMPL-35440/AC2, AC3
- SIMPL-35584/AC2, AC3
- SIMPL-35692/AC2, AC3
- SIMPL-35858/AC2
- SIMPL-35863/AC2
- SIMPL-35888/AC2

In numbers: 84 of 138 live criteria have at least one link. 98 have no operation, 71 have no requirement.

### Operations with no criterion (13 of 26)

- `contract-consumption-adapter-be:GET /contracts/{contractNegotiationId}`
- `contract-consumption-adapter-be:GET /resourceAddresses/sharingMethods/{sharingMethodId}/templates`
- `contract-consumption-adapter-be:GET /resourceAddresses/templates/{templateId}/schema`
- `contract-consumption-adapter-be:GET /resourceAddresses/templates/{templateId}/uiSchema`
- `contract-consumption-adapter-be:GET /transfers/{transferProcessId}`
- `contract-consumption-adapter-be:GET /transfers/{transferProcessId}/dataAddress`
- `contract-consumption-adapter-be:POST /connectorCatalog/assets`
- `contract-consumption-adapter-be:POST /transfers`
- `contract-manager-be:GET /agreements/{contractAgreementId}/file`
- `contract-manager-be:GET /health`
- `contract-manager-be:POST /agreements/search`
- `contract-manager-be:PUT /contractOffers/{contractNegotiationId}/update`
- `contract-ui-be:GET /health`

### Requirements with no criterion (1 of 14)

- `BP7.3.2` A Provider provides the usage contract offer to a Consumer

## Vocabulary

### Actors and roles

| Term | Where |
|---|---|
| Consumer | most stories; BP07; BP7.1.1, BP7.4.1, BP7.4.2, BP7.5.1, BP7.5.2, BP7.8.1 |
| Consumer end user, Consumer user | SIMPL-21924, SIMPL-32505 (end user); SIMPL-19824, SIMPL-21928 (user) |
| Provider | most stories; BP07; BP7.2.1 to BP7.3.2, BP7.6.1 to BP7.7.3 |
| Provider user | SIMPL-28846 |
| Participant | SIMPL-34633, SIMPL-35440, SIMPL-35888 |
| requesting Participant, responding Participant | SIMPL-31053, SIMPL-31076 |
| Governance Authority | SIMPL-34633; BP07 prerequisites and dynamic view |
| EDC, EDC connector, Connector | SIMPL-19824, 19829, 35440, 35526, 35584, 35677, 35858, 35863, 35888; BP07 dynamic view; `contract-consumption-adapter-be` |
| Contract Manager (Provider and Consumer side) | SIMPL-19933, 19934, 21928, 21948, 35440, 35526, 35584, 35660, 35677, 35858, 35863, 35888; `contract-manager-be` |
| Contract Backend | SIMPL-22690; `flow.puml` |
| Contract UI backend, Contract UI frontend | SIMPL-22690, 35526, 35660, 35692; `contract-ui-be`; `flow.puml` |
| Contract Orchestrator | SIMPL-22690; `flow.puml` |
| Catalogue UI | SIMPL-21924 |
| consent manager | SIMPL-31047 |
| VC Issuer | SIMPL-19934, 35584, 35660, 35677; BP07 dynamic view |
| Digital Signature Service | SIMPL-19934 |
| Signer Service | SIMPL-35584, 35660; BP07 dynamic view |
| digital wallet, wallet service, Wallet | SIMPL-19927, 19938 (digital wallet); SIMPL-35440, 35584, 35660 (wallet service); BP07 dynamic view (Wallet) |
| Notification Service | SIMPL-21966 |
| Scheduler | SIMPL-35692 |
| logging and monitoring component | SIMPL-34633 |

### Statuses

| Status set | Where |
|---|---|
| Requested, Rejected, Accepted, Signed, Active | SIMPL-30959/AC1, AC2; SIMPL-30960/AC1 |
| "In progress" | SIMPL-21924/AC2 |
| 'Requested' (response field `status`) | SIMPL-32511/AC1 |
| "Accepted"; accepted, active, rejected, suspended, terminated | SIMPL-32505/AC2, AC3 |
| 'Revocation requested', 'Revoked', 'Active' | SIMPL-31053/AC2, AC3; SIMPL-31076/AC1 to AC4.2 |
| 'Suspended' | SIMPL-31115/AC2, AC3; SIMPL-31116; SIMPL-31117 |
| 'Reactivation requested' | SIMPL-31116/AC2, AC3; SIMPL-31117/AC1, AC2 |
| 'Terminated' | SIMPL-31118/AC2, AC3; SIMPL-35692/AC4 |
| pending, finalized (offer) | SIMPL-35692/AC1, AC2, AC3 |
| "Finalized" (contract negotiation state) | SIMPL-19934/AC1 |
| validation result: successful, rejected or failed, pending | SIMPL-19933/AC1 to AC9; SIMPL-21948/AC1 to AC9 |
| SUCCESS, REJECTED / FAILED, REJECTED / IGNORED, FAILED / PENDING_RETRY | SIMPL-21948 technical info table |
| contract consent (assertion) status | SIMPL-31047/AC1 to AC4 |
| INITIATED, CREATED, FINALIZING, FINALIZED, TERMINATED | `contract-manager-be` schemas `ConfirmationResponse`, `ContractAgreementData` (for example the `.../create`, `.../issue` and `PATCH .../status` operations) |
| CREATED, APPROVED, REJECTED | `contract-manager-be` schemas `ContractOffer`, `ContractOfferStatusUpdate` |
| CREATED, ACCEPTED, REJECTED | `contract-ui-be` offer status; `cdm.puml` `HumanReadableContractData` |
| operation: accept, reject | `contract-ui-be:PATCH /contractOffers/{contractNegotiationId}/{operation}`; SIMPL-35660 |

### Main data objects

| Object | Where |
|---|---|
| usage contract | most stories; BP07 |
| usage contract offer | SIMPL-19829, 21924, 21928, 21963, 22690, 30959, 30960, 32505, 32511, 35526; BP7.1.1 to BP7.5.2 |
| usage contract request, usage contract offer request | SIMPL-19824, 19829 (request); SIMPL-32509, 32511 (offer request) |
| contract offer, contract offer data, contract offer presentation | SIMPL-19933, 35526, 35660, 35692; `contract-manager-be:POST /contractOffers/{contractNegotiationId}`, `contract-ui-be:POST /contractOffers/{contractNegotiationId}` |
| human-readable usage contract offer, `humanReadable`, `humanReadableData`, `usageContract (html)` | SIMPL-22690, 30960, 32511/AC1; `ContractOffer`; `cdm.puml` |
| contract agreement (`contractAgreementId`), contract definition (`contractDefinitionId`) | SIMPL-32511, 35584; `contract-manager-be` paths |
| contract negotiation (`contractNegotiationId`) | SIMPL-21928, 22690, 30959, 35440, 35526, 35660, 35677, 35692, 35858, 35863, 35888; all `/contractOffers` and `/verifications` paths |
| `contractOfferId`, `assetId`, `providerId`, `consumerId` | SIMPL-32511; `ContractAgreementCreate`, `ContractAgreementData` |
| signed usage contract offer, signed usage contract, fully-signed usage contract | SIMPL-21963, 21966, 19933 (offer); SIMPL-19927, 19938, 21948 (contract); BP7.5.2, BP7.7.2 |
| Verifiable Credential (VC), Signature VC | SIMPL-19934, 35440, 35584, 35660, 35677, 35888, 35863; `.../issue`, `PUT /verifications/{contractNegotiationId}` |
| resource description (`resourceDescriptionId`), resource, asset (`assetId`), self-description (`selfDescriptionId`) | SIMPL-19824, 32509 (resource description); SIMPL-32511 (asset); SIMPL-35526 (selfDescriptionId) |
| validation result, rejection reason | SIMPL-19933, SIMPL-21948 |
| Contract Clause document, document metadata (`documentId`, `documentName`, `mediaType`, `hash`, `hashAlgorithm`) | SIMPL-28846 |
| `usageContractHash`, `hash` | SIMPL-32511/AC1; `ContractOffer` |
| notification request, EmailNotification | SIMPL-21966 |
| audit data, aggregations | SIMPL-34633 |
| `correlationId` | SIMPL-21963, 21966, 22690 |

### Words that may mean the same

Listed as pairs; not decided.

- "usage contract offer" and "contract offer"
- "contract offer" and "contract offer presentation" (`contract-manager-be:POST /contractOffers/{contractNegotiationId}`, `contract-ui-be:POST /contractOffers/{contractNegotiationId}`)
- "human-readable usage contract offer" and "humanReadableData" / "usageContract (html)"
- "usage contract" and "contract agreement"
- "usage contract request" (SIMPL-19829) and "usage contract offer request" (SIMPL-32509, 32511)
- "signed usage contract offer" (BP7.5.2, SIMPL-21963) and "signed usage contract" (SIMPL-19927)
- "fully-signed usage contract" (SIMPL-19927, 19938) and "final contract signed by both parties" (SIMPL-19934)
- "Digital Signature Service" (SIMPL-19934) and "Signer Service" (SIMPL-35584, 35660)
- "digital wallet" and "wallet service"
- "Contract Manager" and "Contract Backend" (SIMPL-22690)
- "EDC", "EDC connector" and "Connector"
- "resource description", "asset" and "self-description"
- "Consumer end user", "Consumer user" and "Consumer"
- "Accepted" (SIMPL-32505, UI status ACCEPTED) and APPROVED (`ContractOfferStatusUpdate`)
- "Requested" (SIMPL-30959, 32511), "In progress" (SIMPL-21924), "pending" (SIMPL-35692) and CREATED / INITIATED (APIs)
- "Finalized" (SIMPL-19934) and "Signed" / "Active" (SIMPL-30959)
- "Terminated" (SIMPL-35692/AC4, offer timed out) and REJECTED (the reject call it makes)
- "contract negotiation state" (SIMPL-19933, 19934) and "contract status" (SIMPL-21948)
- "deny" (SIMPL-31076/AC3) and "Reject" decision (SIMPL-31076/AC4.2)
- "contract consent assertion status" and "contract consent status" (SIMPL-31047)
- "verify" the VC (SIMPL-35677) and "validate" the signed contract (SIMPL-21948)

## Totals

| Item | Count |
|---|---|
| links | 120 |
| named | 27 |
| inferred | 93 |
| to operations | 44 |
| to requirements | 76 |
| live criteria with at least one link | 84 of 138 |
| operations with at least one criterion | 13 of 26 |
| requirements with at least one criterion | 13 of 14 |

## After the review (17 Nov 2026)

The parser errors above were fixed in `extract_inventory.py` and the inventory was regenerated: AC headings after a list marker, struck criteria inside list items, escaped braces in paths, and method plus path matching (SIMPL-35660 now flagged). SIMPL-19938 turns out to hold each of AC1 to AC4 twice (a short and a longer version), now `AC1.1`, `AC1.2` and so on, flagged as duplicate labels. The ids in `links.json` for SIMPL-19938 and SIMPL-22690 were mapped to the new ids; the links themselves are unchanged.
