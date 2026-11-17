# API review (step 21b): contract-management

Input: the three OpenAPI files under `specs/api/`, `api-checks.json` (21a), `ac-findings.json` (20b), `domain-findings.json` (23b), `decisions/decisions.json` (step 30), `inventory/` and the `API-`, `SEC-` and `OWASP-` review rules for `api-spec` in `guidelines/rules.json`. Findings are in [`api-findings.json`](api-findings.json), ids P-001 to P-013. contract-consumption-adapter-be is context only (D2): reviewed lightly, no coverage findings.

## Summary

13 findings: **1 blocker, 6 major, 6 minor.**

| Rule | Blocker | Major | Minor | Findings |
|---|---|---|---|---|
| API-12 (status codes) | 1 | 2 | | P-001, P-003, P-007 |
| COV-01 (uncovered operation) | | 1 | | P-002 |
| API-60 (required flags) | | 1 | | P-004 |
| API-21 (examples) | | 1 | | P-005 |
| API-46 (problem type) | | 1 | | P-006 |
| SEC-07 (user authentication) | | | 1 | P-008 |
| API-49 (info.description) | | | 1 | P-009 |
| API-22 (realistic examples) | | | 1 | P-010 |
| API-57 (parameter description) | | | 1 | P-011 |
| API-11 (method fits action) | | | 1 | P-012 |
| API-03 (hand-written spec) | | | 1 | P-013 |

The five findings with most impact:

1. **P-001 (blocker).** POST `.../issue` and PATCH `/agreements/{contractAgreementId}/definitions/{contractDefinitionId}/status` are still in the OpenAPI but no decision says whether they are built. PATCH `.../status` would let any API-key caller set FINALIZED or TERMINATED and skip the D12 state machine.
2. **P-003 (major).** The OpenAPI does not list the 404, 406, 409, 415 and 502 answers that D3, D4, D5, D8 and D14 define. It also lists a 404 for GET `/contractOffers`, which D14 forbids.
3. **P-004 (major).** Request schemas do not match the decisions. The search body and ContractAgreementCreate have no required fields, the VC body of PUT `/verifications` is required although D8 says it is ignored, and the contract-ui-be ContractOffer lacks the ids that D4 and D14 add.
4. **P-005 (major).** Every success example in contract-ui-be contradicts its schema (`humanReadableData`, `createdDate`, `offerStatus`). `lastUpdatedDate` and the target of `offerUrl` are decided nowhere.
5. **P-002 (major).** Four Contract Manager operations have no story and no decision. Three are leftovers of the orchestrator design and can be retired. POST `/agreements/search` needs a story.

The API is in good shape on many points. Both main APIs use RFC 9457 problem details everywhere, declare license, contact and servers, have examples for almost every body, and keep plural collection names. The 21a script found nothing on 31 of its 41 rules.

## Blockers and their questions

| Id | Question | Options |
|---|---|---|
| P-001 | Are POST `/credentials/agreements/{contractAgreementId}/definitions/{contractDefinitionId}/issue` and PATCH `/agreements/{contractAgreementId}/definitions/{contractDefinitionId}/status` retired in the target design, or kept? If kept, which D12 transitions may they cause and on which side? | (1) Retire both (simplest, matches D5 and D7). (2) Keep `/issue` as an internal alias of the D5 or D7 signing step, retire PATCH `.../status`. (3) Keep both, guarded by D12 with 409 otherwise. |

Context: D1 says SIMPL-19934 is superseded and that SIMPL-21928 and 21963 are made concrete by D5, D6, D7 and D10. links.json still links those stories to the two operations. If the step 30 owners choose option 1, this closes as a short addition to D1.

## Rule-by-rule table per API

Review rules (`check: review`) that apply to `api-spec`: 20 rules (18 API, 1 SEC, 0 OWASP). CM = contract-manager-be, UI = contract-ui-be, AD = contract-consumption-adapter-be (light).

| Rule | Level | CM | UI | AD (light) | Evidence, finding |
|---|---|---|---|---|---|
| API-01 CDM delivered | MUST | partly | partly | partly | A CDM exists in the register for each API (sources.yaml `cdm:`), but all are in review and point to old repositories. UI also has `cdm.puml` with other names. Covered by D-018, D-017, D24. |
| API-02 schemas reference the CDM | SHOULD | not met | not met | not met | All schemas are local, and the CM VerifiableCredential redefines an external class. Covered by D-012, D-013, D24. |
| API-03 hand-written | SHOULD | met | met | not met | CM and UI have hand-written examples and comments. AD looks generated: a property named `additionalProperties` in four problem schemas, and `JsonNode`. P-013. |
| API-10 plural collections | SHOULD | met | met | met | `/agreements`, `/credentials`, `/verifications`, `/contractOffers`, `/transfers`, `/contracts`. |
| API-11 method fits action | SHOULD | partly | met | partly | PUT `/verifications` starts an async check, POST `/agreements/search` reads, PUT `.../update` pushes a status. P-012, P-002. AD: POST create answers 200. |
| API-15 American spelling | SHOULD | met | met | met | No British spellings in property names. |
| API-19 business meaning | SHOULD | partly | partly | met | `contractSearchTO` (example only), `humanReadable`, `hash`, `ContractAgreementData`. P-012, D-024. |
| API-22 realistic examples | SHOULD | partly | partly | met | VC example `credentialSubject` {userId, collectionId}, `consumerId: 123456`, `did:jwk:...`, Windows paths. P-010. |
| API-28 document, collection, controller | SHOULD | partly | met | met | `/agreements/{contractNegotiationId}/search` is a controller under a document addressed by another id (D-004, D20). `.../update` is a verb segment (P-002). |
| API-30 detail path form | SHOULD | met | met | met | GET `/agreements/{contractAgreementId}`, GET `/contractOffers/{contractNegotiationId}`. |
| API-49 business info.description | SHOULD | not met | not met | met | CM "for interaction with the Dataspace connector". UI "UI for contract and billing module". P-009. |
| API-50 own id named id or code | SHOULD | not met | not met | partly | `contractAgreementId` in ContractAgreementData. The UI ContractOffer has no id (D14 adds `contractNegotiationId`). D19 and D20 keep wire names. P-012. |
| API-51 reference named after relation | SHOULD | not met | n/a | not met | `providerId`, `consumerId`, `assetId`, `contractDefinitionId`. Kept by D19 and D23. P-012. |
| API-54 identifier schema types end in Id | SHOULD | n/a | n/a | met | CM and UI have no identifier schemas (inline strings). AD has `ContractNegotiationId` and `TransferProcessId`. |
| API-55 standard identifier names | SHOULD | n/a | n/a | n/a | No standardised business identifier applies. Participant ids are opaque strings. |
| API-58 enum for closed lists | SHOULD | met | met | met | `operation` [accept, reject], status enums, Policy.type. |
| API-60 required flags | SHOULD | partly | met | met | All path parameters are `required: true`. Bodies: search and ContractAgreementCreate have no `required`, and `/issue` has `required: false`. P-004. |
| API-62 AsyncAPI for Kafka | MUST | n/a | n/a | n/a | D2: no Kafka flow and no orchestrator in the target. The Kafka events in `flow.puml` belong to the replaced design. |
| API-79 topic owner prefix | SHOULD | n/a | n/a | n/a | No topics (D2). |
| SEC-07 Tier 1 OIDC | SHOULD | n/a | partly | met | CM is called by the Connector and contract-ui-be (machine callers). UI declares only an API key, and D2 and E2 put user login at the gateway, which the spec does not say. 403 responses cannot occur. P-008. AD declares bearer JWT from the gateway. |

Rules not met (any API): API-02, API-03 (AD), API-49, API-50, API-51. Partly met: API-01, API-11, API-19, API-22, API-28, API-60, SEC-07.

## Coverage

| Check | Operation | Class | Basis |
|---|---|---|---|
| COV-01 | CM GET `/agreements/{contractAgreementId}/file` | retire (should not be there) | Reads a text file from the filesystem. The D12 record and the E8 rendering do not have it, and no story uses it. P-002. |
| COV-01 | CM POST `/agreements/search` | story missing | It lists "active" usage contracts. This could be the overview after acceptance that D14 leaves open. P-002. |
| COV-01 | CM PUT `/contractOffers/{contractNegotiationId}/update` | retire (should not be there) | An orchestrator push of a status to the UI side. It conflicts with D21 (the Contract Manager is the root) and D2. Its example status SIGNED is not in its own enum. P-002. |
| COV-01 (extra, inferred link only) | CM POST `/contractOffers/{contractNegotiationId}` | retire (should not be there) | Relays the offer to contract-ui-be for the orchestrator (`flow.puml`). D4 does this inside `/create`. P-002. |
| COV-01 (extra, superseded links only) | CM POST `.../issue`, PATCH `.../status` | undecided, blocker | Linked only from SIMPL-19934 (superseded) and 21928, 21963 (made concrete by D5, D7). P-001. |
| COV-02 | SIMPL-32509: POST `/resourceDescriptions/{resourceDescriptionId}/validateExistenceContract` | already decided | D3 (not built). F-001, F-020. |
| COV-02 | SIMPL-35440: GET `/verifications/{contractNegotiationId}/vc` | already decided | D10 (new operation, owner asked to add it). F-020, F-036. |
| COV-02 | SIMPL-35584: POST `.../validateAndIssue` | already decided | D7. F-012, F-020. |
| COV-02 | SIMPL-35584: GET `/vcExchange/{contractNegotiationId}/request` | already decided | A Connector endpoint, used as a stub (E4). F-021. |
| COV-02 | SIMPL-35660: POST `/contractOffers/{contractNegotiationId}/{operation}` | already decided | D5 (PATCH, 200). F-010. |

Coverage classes in numbers: COV-01 (3 from 21a): 1 story missing, 2 retire, 0 internal plumbing. Three more operations with only inferred or superseded links: 1 retire, 2 undecided (P-001). COV-02 (5): all 5 already covered by 20b or step 30, 0 new. The adapter is context only: no coverage findings.

## 21a triage

14 findings: **6 harmless, 8 real.** Of the 8 real ones, 7 became part of P-findings and 1 (A-014) was already covered.

| A-id | Rule | Verdict | Reason or finding |
|---|---|---|---|
| A-001 | API-63 | harmless | No Kafka in the target design (D2), so there is no channel to describe. |
| A-002 | API-13 | harmless | `@id` is a JSON-LD keyword that the EDC payload requires. |
| A-003 | API-20 | real | `Policy.policyConstraints`. Context only, P-013. |
| A-004 | API-52 | harmless | The rule's own example (`/users/{userId}`) uses a prefix. In these paths the prefix says which of two id kinds is meant (D20). The adapter is context only. |
| A-005 | API-13 | harmless | `@context` is required by the W3C VC data model. |
| A-006 | API-18 | real | `ContractOfferStatusUpdate.additionalInfo`, on an operation to retire. P-002. |
| A-007 | API-21 | real | The search request bodies have no body-level example. P-010. |
| A-008 | API-52 | harmless | Same as A-004. Under `/agreements` a bare `id` would hide whether it is a negotiation id or an agreement id (D-004, D20). Wire names are kept (D19). |
| A-009 | API-57 | real | The `operation` parameter has no description. P-011. |
| A-010 | COV-01 | real | Three uncovered operations, classified above. P-002. |
| A-011 | API-26 | real | The base path segment `contract-ui`, kept for this build by E1. P-011. |
| A-012 | API-52 | harmless | Same as A-004. |
| A-013 | API-57 | real | Same parameter in contract-ui-be. P-011. |
| A-014 | COV-02 | real, already covered | All five are decided or raised (D3, D5, D7, D10, E4; F-010, F-020, F-021). No new finding. |
