# Domain review (step 23b): contract-management

A fresh reviewer judged the domain language of Contract Management: stories, BP07 and BP10, the three OpenAPI files, their CDMs and the context CDMs (signer, wallet, issuer, consent). Inputs as listed in the step 23b brief; nothing else was read. Findings are in [`domain-findings.json`](./domain-findings.json) (`D-001` to `D-028`), the draft glossary in [`glossary.md`](./glossary.md). Findings of step 20b are referred to by id (`F-`), not repeated.

## Summary

28 findings: 6 blockers, 13 major, 9 minor.

| Rule | Blocker | Major | Minor | Total |
|---|---|---|---|---|
| DDD-01 one term per concept | 1 | 1 | 5 | 7 |
| DDD-02 one concept per term | 2 | 2 | 0 | 4 |
| DDD-03 glossary | 0 | 0 | 1 | 1 |
| DDD-04 value sets | 1 | 0 | 0 | 1 |
| DDD-05 UI label mapping | 0 | 1 | 0 | 1 |
| DDD-06 API objects map to CDM | 0 | 1 | 0 | 1 |
| DDD-07 wrappers | 0 | 0 | 1 | 1 |
| DDD-08 attributes | 0 | 1 | 0 | 1 |
| DDD-09 one identity name | 1 | 1 | 1 | 3 |
| DDD-11 aggregate root | 0 | 1 | 0 | 1 |
| DDD-12 lifecycle defined once | 1 | 0 | 0 | 1 |
| DDD-13 externals marked | 0 | 2 | 0 | 2 |
| DDD-14 own language, one translation | 0 | 1 | 0 | 1 |
| DDD-15 context map | 0 | 1 | 0 | 1 |
| DATA-01 CDM exists | 0 | 1 | 0 | 1 |
| DATA-09 business nouns | 0 | 0 | 1 | 1 |
| **Total** | **6** | **13** | **9** | **28** |

The five findings with most impact:

1. **D-001** The main object has four names: 'usage contract' (process, stories), 'contract agreement' (CDM, API), 'contract' and 'Usage Contract Agreement'.
2. **D-004** The usage contract has no single identity: some endpoints use contractNegotiationId, others contractAgreementId, and `/agreements` is keyed by both.
3. **D-005** No lifecycle is defined once. The CDM types the status but lists no values, and the stories add six states no API carries. All 18 value-set hits from 23a trace back to this.
4. **D-002** 'Contract agreement' names both the EDC operational agreement and the business usage contract. The consumption CDM says their ids are not equal, but the EDC calls the Contract Manager with a contractAgreementId.
5. **D-003** 'ContractOffer' names the EDC offer in the CDM and the human-readable presentation in both APIs. The UI CDM calls the presentation HumanReadableContractData.

The language is in better shape than the size of this list suggests. The business process is consistent with itself and gives a good base: 'usage contract', 'usage contract offer', 'signed usage contract offer', 'signed usage contract', 'validate'. Most findings come from technical stories and APIs that took over the EDC connector's words. The consumption adapter's CDM is a good model for the rest: it marks every external concept, names its owner and documents the wire names.

## Blockers and their questions

| Id | Rule | Question | See |
|---|---|---|---|
| D-001 | DDD-01 | Which single term names the legally binding contract between Consumer and Provider, in the stories, the CDM class and the API schemas? Proposed: 'usage contract' (business process and 30+ stories). | F-004 |
| D-002 | DDD-02 | Is the Contract Manager's contractAgreementId the EDC agreement id, or its own key with a separate reference to the EDC agreement? | F-002 |
| D-003 | DDD-02 | Are the EDC contract offer, the machine-readable usage contract offer and the human-readable offer presentation one concept or three, and which one owns the offer status? Proposed: three terms, status on the usage contract offer. | F-002, F-044 |
| D-004 | DDD-09 | Which identifier identifies a usage contract offer and the resulting usage contract over the whole lifecycle, and under which name do the APIs expose it? | F-001, F-002 |
| D-005 | DDD-12 | Where is the one state machine for the usage contract offer and the usage contract written down, and which operation performs each transition? | F-004, F-005, F-018, F-037, F-048, F-049 |
| D-006 | DDD-04 | Is the accepted state ACCEPTED (UI API, stories, cdm.puml) or APPROVED (Contract Manager API) in both APIs? Proposed: ACCEPTED. | F-004 |

D-005 and D-006 sharpen F-004 from the domain side: F-004 asks for one status set, D-005 asks where its state machine lives, and D-006 names the one pair that already breaks the accept path between the two backends.

## Rule by rule

| Rule | Verdict | Evidence |
|---|---|---|
| DDD-01 | not met | Seven synonym groups, the main object included: usage contract and contract agreement (D-001); offer request (D-020); establishment and negotiation (D-021); validate and verify (D-010); collaborator names (D-022); consent (D-025); adapter template (D-027). |
| DDD-02 | not met | 'contract agreement' (D-002), 'ContractOffer' (D-003), 'Terminated' (D-008) and 'signed usage contract' (D-009) each name two or three concepts. |
| DDD-03 | not met | No glossary existed; drafted in this step (D-026). |
| DDD-04 | not met | Offer status CREATED/APPROVED/REJECTED (contract-manager-be) against CREATED/ACCEPTED/REJECTED (contract-ui-be) (D-006); no CDM value sets (D-005). |
| DDD-05 | not met | UI labels Requested, Signed, Active have no stored value and no mapping (SIMPL-30959/AC1) (D-007). |
| DDD-06 | partly met | ContractAgreementData and ContractAgreementCreate match ContractAgreement exactly. The offer presentation does not match by name (D-003). Validation result, revocation request, Contract Clause document and audit data have neither CDM class nor schema (D-016). |
| DDD-07 | partly met | 16 of 23a's 18 wrapper hits are clean wrappers (Problems, acknowledgements, requests, lists). Two carry state the CDM lacks: ConfirmationResponse, ContractOfferStatusUpdate (D-023). |
| DDD-08 | partly met | ContractAgreementData has all ten attributes of the CDM class under the same names. The offer attributes have three naming sets (D-017). VerifiableCredential attributes differ, but the fix is to mark the class external (D-012). |
| DDD-09 | not met | contractNegotiationId and contractAgreementId for one object (D-004); resourceDescriptionId, assetId, selfDescriptionId for the resource (D-011); contractAgreementId and contractId in the adapter (D-028). |
| DDD-10 | met, with a caveat | No API schema gives a value-like object its own id. No CDM marks value objects, so the rule cannot be fully checked. The human-readable offer and DataDestination are value objects and carry no id of their own. |
| DDD-11 | partly met | The usage contract is changed through the Contract Manager. The offer is changed in two components with no named root (D-015). |
| DDD-12 | not met | No state machine for offer or contract anywhere; ContractAgreementStatus has no values (D-005). |
| DDD-13 | partly met | The consumption and consent CDMs mark externals with owners. The Contract Manager CDM marks six externals with no owner and with names that differ from the owners' (D-013), and redefines VerifiableCredential (D-012). |
| DDD-14 | not met | The Contract Manager API is keyed on EDC ids and describes itself as 'for interaction with the Dataspace connector'; translation to EDC is in the Contract Manager (stories) and in a Contract Orchestrator (SIMPL-22690, flow.puml) (D-014). |
| DDD-15 | not met | No context map. The register's concept reference index is not in the snapshot (D-019). |
| DATA-01 | partly met | A CDM exists for each API, but all three are 'In review', not approved; the two Contract Management CDMs are thin and point to old repositories (D-018). |
| DATA-04 | partly met | The Contract Manager CDM describes business data. The UI CDM uses storage words ('humanReadableData', 'contractData'), covered under D-024. The consumption CDM keeps transfer details out on purpose (section 3). |
| DATA-09 | partly met | ContractAgreement and VerifiableCredential are business nouns; HumanReadableContractData is not (D-024). |
| API-01 | partly met | Each API has a CDM in the register, but none is delivered next to its API definition. contract-ui-be has a `cdm.puml` in its OpenAPI folder that adds status values the register CDM lacks, so there are two versions (D-018, D-005). |

## Entities

| Business object (proposed term) | CDM class | Owned or external in CDM, and is it right | Identity and its names | States | Operations and ACs that change state |
|---|---|---|---|---|---|
| usage contract | `CDM:contract-manager-be:ContractAgreement` | owned; right | `contractAgreementId` (UUID, PK), also reached by `contractNegotiationId` (D-004) | API: INITIATED, CREATED, FINALIZING, FINALIZED, TERMINATED; stories: Active, Suspended, Reactivation requested, Revocation requested, Revoked, Terminated (D-005) | POST `.../create` (SIMPL-19829, 32511, 35526), POST `.../issue` (SIMPL-19934/AC1, 21963), PATCH `/agreements/{id}/definitions/{id}/status` FINALIZED or TERMINATED; lifecycle ACs SIMPL-31053, 31076, 31115 to 31118 have no operation (F-005) |
| usage contract offer | none | not modelled; the CM CDM's external ContractOffer is a different concept (D-003) | `contractNegotiationId` | CREATED, ACCEPTED or APPROVED, REJECTED; stories add Signed, Terminated (timeout) | contract-ui-be PATCH `/{operation}` accept or reject (SIMPL-22690, 32505); contract-manager-be PATCH `/{operation}` (SIMPL-35660), PUT `/update`; scheduler (SIMPL-35692) |
| human-readable usage contract offer | `CDM:contract-ui-be:HumanReadableContractData` | owned by the UI backend; right if it stays a rendering, but it also holds the offer status (D-015) | `contractNegotiationId` | as the offer | POST `/contractOffers/{contractNegotiationId}` in both APIs (SIMPL-35526) |
| Verifiable Credential | `CDM:contract-manager-be:VerifiableCredential` | owned; wrong, the VC Issuer owns it (D-012) | `id` (W3C) | issuer CDM: ACTIVE, REVOKED, SUSPENDED; none in this component | POST `.../issue` (SIMPL-19934), PUT `/verifications/{contractNegotiationId}` (SIMPL-35677), GET `/verifications/{id}/vc` (SIMPL-35440, not in OpenAPI) |
| existence check result | none | not modelled | by `contractNegotiationId` | found or not found | POST `/agreements/{contractNegotiationId}/search`, `ContractSearchConfirmation` (SIMPL-19824); SIMPL-32509 endpoint not in OpenAPI (F-001) |
| validation result | none | not modelled (D-016) | none | successful, rejected, failed, pending | asynchronous; SIMPL-19933, 21948; `isValid` callback (SIMPL-35863) |
| contract negotiation | `CDM:contract-manager-be:ContractNegotiation` | external; right, but no owner named (consumption CDM: EDC Connector) | `contractNegotiationId` | EDC NegotiationState; stories set it to "Finalized" (SIMPL-19934/AC1) | Contract Manager changes it only through EDC callbacks (`/signed`, `/isValid`, `/isPresent`) |
| resource description | `CDM:contract-manager-be:Asset` | external; name differs from owner's ResourceDescription (D-013) | `assetId`, `resourceDescriptionId`, `selfDescriptionId` (D-011) | none here | none |
| Consumer, Provider | `Consumer`, `Provider` in CM CDM | external; owners model Participant with a role (D-013) | `consumerId`, `providerId` | none | none |
| Contract Clause document | none | not modelled; owner not decided (D-016) | `documentId` | none | upload API not in the input (SIMPL-28846, F-013) |

## Boundaries

| Concept | Owner | How Contract Management uses it | Same class in the context CDM? |
|---|---|---|---|
| Verifiable Credential | VC Issuer (`issuer-service-be.md`) | issues through it, verifies it, exposes it in its API | No. CM CDM redefines it with other attributes; the wallet CDM calls it Credential (D-012) |
| Signature | Signer Service (`signer-service-be.md`) | requests signing | Not referenced in the CM CDM. Stories call the service 'Digital Signature Service' too (D-022) |
| Credential storage | Wallet (`wallet-service-be.md`) | stores the VC | Not referenced in the CM CDM |
| Contract consent assertion | Consent Management (`consent-management-be.md`) | shows and links to it (SIMPL-31047) | The consent CDM references ContractAgreement with the same name; the CM CDM does not reference ConsentAssertion back (D-019) |
| Contract negotiation, definition, agreement, offer, transfer | EDC Connector (no CDM; consumption CDM section 1) | API paths and payloads keyed on their ids | Names differ: CM CDM uses ContractNegotiation, ContractDefinition; consumption CDM uses EDC prefixed names (D-013, D-014) |
| Resource description | Federated Catalogue | covered by the usage contract | CM CDM says Asset; consent CDM says ResourceDescription; consumption CDM says Dataset (D-011, D-013) |
| Participant | Identity Provider | Consumer and Provider ids | CM CDM has Provider and Consumer classes; owners model Participant with a role (D-013) |
| Notification | Notification Service | publishes EmailNotification (SIMPL-21966) | no CDM in the input |

The Contract Manager API uses EDC terms instead of its own (DDD-14). Four stories also specify EDC endpoints (F-021). The translation to EDC is described in two places (D-014).

## 23a triage in numbers

78 hits: 29 real, 49 harmless.

| API or source | Rule | Hits | Real | Harmless | Reason for harmless ones |
|---|---|---|---|---|---|
| contract-manager-be | DATA-01 | 1 | 1 | 0 | |
| contract-manager-be | DDD-06 | 3 | 1 | 2 | ContractSearchConfirmation and ContractSearchItem are projections of ContractAgreement (ids and consumerId only) |
| contract-manager-be | DDD-07 | 7 | 2 | 5 | ContractSearchResponse (list), TransferConfirmationResponse and VerificationConfirmationResponse (acknowledgements), StatusUpdate (command on ContractAgreement.status, value issue counted under DDD-04), VerifyCredentialRequest (carries a VC) |
| contract-manager-be | DDD-08 | 13 | 3 | 10 | The 10 VerifiableCredential fields are the W3C data model of a concept owned by the VC Issuer; the fix is to mark the class external (D-012), not to add attributes |
| contract-manager-be | DDD-04 | 13 | 9 | 4 | INITIATED on VerificationConfirmationResponse and TransferConfirmationResponse is the acknowledgement of an asynchronous call, not a business state |
| contract-manager-be | DDD-01 | 1 | 0 | 1 | False positive: 'Verifiable Credential (VC)' appears in the BP07 dynamic view and SIMPL-19927, 35888 |
| contract-ui-be | DATA-01, DDD-06, DDD-07, DDD-04, DDD-01 | 8 | 7 | 1 | ContractOfferResponse returns only offerUrl |
| contract-consumption-adapter-be | DATA-01 | 1 | 1 | 0 | |
| contract-consumption-adapter-be | DDD-06 | 12 | 2 | 10 | ContractNegotiation, TransferProcess, Policy, PolicyConstraint: the CDM maps these wire names to EDC-prefixed external classes. ContractNegotiationId, TransferProcessId: id wrappers. DataAddressProperties, JsonNode, InputObject, InputValidationIssue: parts of other payloads or error details. Real: Template and ResourceAddressTemplate (D-027) |
| contract-consumption-adapter-be | DDD-07 | 10 | 0 | 10 | Six Problem schemas, CatalogSearchResult (list), ContractNegotiationRequest and TransferRequest (requests), DataAddressInfo (wire name of EDCDataAddressInfo) |
| contract-consumption-adapter-be | DDD-08 | 5 | 0 | 5 | Dataset.policy is modelled as the relation hasPolicy; the four Kafka security fields are left out on purpose (CDM section 3) |
| contract-consumption-adapter-be | DDD-01 | 1 | 0 | 1 | Context-only API; no Contract Management story describes it |
| stories | DDD-04 | 3 | 3 | 0 | |
| **Total** | | **78** | **29** | **49** | |

The real hits sit in D-003, D-005, D-006, D-017, D-018, D-023, D-024 and D-027. The 23a script found no DDD-09, DDD-02 or DDD-14 hits. Those rules hold half of this review's blockers, so they need review by a person even after the script improves.
