# Draft glossary: contract-management

Step 23b. A draft, not a decision. One row per concept. The proposed term follows the business process first, then the stories, then the CDM, then the API. Findings `D-` are in [`domain-findings.json`](./domain-findings.json), findings `F-` in [`ac-findings.json`](./ac-findings.json).

Status: `agreed` when all sources already use one term; `proposed` when this draft chose among synonyms; `open` when the sources disagree on the meaning.

CDM ids use the form `CDM:<file>:<Class>`. "none" means no class or schema exists.

## Business objects and processes

| Term (proposed) | Definition (from the specs) | CDM class | API schema(s) and field(s) | Stories and ACs | Synonyms found (where) | Status |
|---|---|---|---|---|---|---|
| usage contract | Legally binding contract between a Consumer and a Provider that defines the rights, limits and obligations for using a resource (BP07 overview). | `CDM:contract-manager-be:ContractAgreement` | contract-manager-be `ContractAgreementData`, `ContractAgreementCreate`, `ContractSearchItem`, `ContractSearchConfirmation`; paths `/agreements/...`, `/credentials/agreements/...` | most stories, for example SIMPL-31053, 31115, 31118, 30959, 19927; BP7.2.2, 7.7.2, 7.8.1 | contract agreement (CDM, API, SIMPL-35584 title, SIMPL-21928 Why); contract (SIMPL-35660, 21948, 35584); Usage Contract Agreement (BP07 dynamic view); contract data (SIMPL-35526, 35584, 35677) | proposed (D-001, D-002) |
| usage contract offer | The offer a Provider creates for a Consumer's request, which the Consumer reviews, accepts or rejects, and signs (BP 7.3 to 7.5). | none (the external `CDM:contract-manager-be:ContractOffer` is the EDC offer, see below) | path `/contractOffers/{contractNegotiationId}` in both APIs; status on `ContractOffer.status` (both APIs) | SIMPL-19829, 21924, 21928, 21963, 22690, 30959, 30960, 32505, 32511, 35526; BP7.1.1 to 7.5.2 | contract offer (SIMPL-35526, 35660, 35692, 19933, 21966, API paths); Contract Offer (dynamic view); usage contract offer overview (SIMPL-32511/AC1); machine-readable usage contract offer (SIMPL-19829/AC1) | open (D-003) |
| human-readable usage contract offer | The HTML rendering of the usage contract offer, with a hash of it, shown to the Consumer for review (contract-ui-be `ContractOffer.humanReadable`, SIMPL-30960). | `CDM:contract-ui-be:HumanReadableContractData` | contract-ui-be `ContractOffer` {humanReadable, hash, status}, `ContractOfferResponse.offerUrl`; contract-manager-be `ContractOffer` {humanReadable, hash, status} | SIMPL-22690/AC1, 30960/AC1, 32511/AC1, 35526/AC1 | contract offer presentation (operation summaries in both APIs); contract offer data (SIMPL-35526, 19933); HumanReadableContractData (UI CDM); usageContract (html) and usageContractHash (SIMPL-32511/AC1); Human readable version of the usage contract (offer) (SIMPL-30960/AC1) | proposed (D-003, D-017) |
| EDC contract offer | The connector's offer for a dataset, referenced by its id only (consumption CDM `Dataset.offerId`). | `CDM:contract-manager-be:ContractOffer` (external); `CDM:contract-consumption-adapter-be:Dataset.offerId` | contract-manager-be `ContractAgreementCreate.contractOfferId`, `ContractAgreementData.contractOfferId`; adapter `Dataset.offerId` | SIMPL-32511/AC1 (contractOfferId) | ContractOffer (CM CDM, same name as the presentation) | open (D-003) |
| usage contract offer request | The Consumer's request for a usage contract offer for a selected resource (BP 7.1.1). | none | adapter `ContractNegotiationRequest` (POST /contracts, context) | SIMPL-32509/AC1, 32511/AC1, 21924; BP7.1.1, 7.2 | usage contract request (SIMPL-19824, 19829); Contract Offer Request (dynamic view) | proposed (D-020) |
| usage contract establishment | The business process in which Consumer and Provider request, review, sign, validate and store a usage contract (BP07). | none | none | SIMPL-19824/AC1, 19829/AC2, 21963, 21966 | contract negotiation process or flow (SIMPL-19824/AC6, 19829 user story) | proposed (D-021) |
| contract negotiation (EDC) | The EDC protocol exchange whose state is driven by the connectors; its id is used as the key of offers and verifications (consumption CDM section 8). | `CDM:contract-manager-be:ContractNegotiation` (external); `CDM:contract-consumption-adapter-be:EDCContractNegotiation` | path parameter `contractNegotiationId` (both APIs); `ContractAgreementData.contractNegotiationId`; adapter `ContractNegotiation`, `ContractNegotiationId` | SIMPL-21928, 22690, 30959, 35440, 35526, 35660, 35677, 35692, 35858, 35863, 35888 | negotiation (SIMPL-35660/AC3); EDCContractNegotiation (consumption CDM) | proposed (D-004, D-014) |
| contract definition (EDC) | The provider-side EDC binding of an asset to an access and a contract policy (consumption CDM `EDCContractDefinition`). | `CDM:contract-manager-be:ContractDefinition` (external); `CDM:contract-consumption-adapter-be:EDCContractDefinition` | path parameter `contractDefinitionId`; `ContractAgreementData.contractDefinitionId`; adapter `ContractNegotiationRequest.contractDefinitionId` | SIMPL-32511/AC1 (path only) | EDCContractDefinition | proposed (D-013) |
| resource description | The catalogue description of a data, application or infrastructure resource that the usage contract covers (BP07 prerequisites, BP 7.2). | `CDM:contract-manager-be:Asset` (external); owner's class `ResourceDescription` (consent CDM reference); `CDM:contract-consumption-adapter-be:Dataset` | contract-manager-be `assetId`; adapter `Dataset.assetId`; SIMPL-32509 `resourceDescriptionId` (endpoint not in OpenAPI) | SIMPL-19824, 19829, 30959/AC1, 30960/AC1, 32509; BP7.2 | asset, assetId (CM API, SIMPL-32511); self-description, selfDescriptionId (SIMPL-35526); resource (SIMPL-21924, 31115); Dataset (consumption CDM) | open (D-011) |
| Participant | An organisation onboarded into the data space (consent CDM). | owner Identity Provider; referenced as `Participant` in the consent and consumption CDMs | none directly; see Consumer and Provider | SIMPL-34633, 35440, 35888, 31053, 31076 | participant (lower case, SIMPL-35440) | agreed |
| Consumer | The Participant that requests and uses a resource (BP07 actors). | `CDM:contract-manager-be:Consumer` (external, no owner); owners model it as a role (PartyRole, PartyType CONSUMER) | `consumerId` (`ContractAgreementData`, `ContractAgreementCreate`, `ContractSearchItem`, search bodies) | most stories; BP07 | Consumer user (SIMPL-19824, 21928); Consumer end user (SIMPL-21924, 32505); ConsumerId (SIMPL-32509/AC1) | proposed (D-013) |
| Provider | The Participant that offers a resource: Infrastructure, Application or Data Provider (BP07 actors). | `CDM:contract-manager-be:Provider` (external, no owner); role PROVIDER in the owners' CDMs | `providerId`; adapter `providerParticipantId`, `counterPartyId` | most stories; BP07 | Provider user (SIMPL-28846); proverId (typo, SIMPL-32511/AC1, see F-032) | proposed (D-013) |
| requesting Participant, responding Participant | The Provider or Consumer that requests a revocation, and the one that responds to it (SIMPL-31053). | none | none | SIMPL-31053, 31076 | none | agreed |
| access policy | The policy in the resource description that decides whether a Consumer may request a usage contract (BP 7.2.1). | owner outside; `CDM:contract-consumption-adapter-be:EDCPolicy` type ACCESS (context) | adapter `Policy.type` ACCESS (context) | SIMPL-19824, 21924/AC3, 32511/AC1; BP7.2.1 | contract access policies (SIMPL-21924) | proposed |
| existence check | The Provider's check that the Consumer has no active usage contract for the same resource (BP 7.2.2). | none | contract-manager-be POST `/agreements/{contractNegotiationId}/search` (`ContractSearchConfirmation`), POST `/agreements/search` (`ContractSearchResponse`) | SIMPL-19824, 32509, 35858; BP7.2.2 | verify existence of a usage contract (BP); validateExistenceContract (SIMPL-32509); isPresent (SIMPL-35858) | open (F-001) |
| active usage contract | A usage contract that blocks a new offer for the same Consumer and resource. | none | none | SIMPL-19824/AC2, 21924/AC4, 32509 | active contract (SIMPL-19824) | open (F-018) |
| signed usage contract offer | The usage contract offer after the Consumer signed it, before the Provider signs (BP 7.5.2, 7.6). | none | none | SIMPL-19933, 21963, 21966; BP7.5.2, 7.6.1 | signed usage contract (SIMPL-19938/AC1.1); Consumer-signed usage contract offer or signing evidence (SIMPL-21963/AC1) | proposed (D-009) |
| signed usage contract | The usage contract signed by both Consumer and Provider (BP 7.7.2, 7.8). | `CDM:contract-manager-be:ContractAgreement` with relation "is signed by" VerifiableCredential | `ContractAgreementData.consumerSignatureDate`, `providerSignatureDate`; GET `/agreements/{contractAgreementId}/file` | SIMPL-21948, 19927, 19938; BP7.7.2, 7.8.1 | fully-signed usage contract (SIMPL-19927, 19938); final contract signed by both parties (SIMPL-19934/AC1) | proposed (D-009) |
| Verifiable Credential (VC) | The credential the VC Issuer issues for the usage contract, signed by the Signer Service and stored in the Wallet (BP07 dynamic view, step 4). | `CDM:contract-manager-be:VerifiableCredential` (own, should be external); owner `CDM:issuer-service-be:VerifiableCredential`; `CDM:wallet-service-be:Credential` | contract-manager-be `VerifiableCredential` (W3C fields), `VerifyCredentialRequest`; POST `.../issue` | SIMPL-19934, 35440, 35584, 35660, 35677, 35863, 35888, 19927 | Signature VC (SIMPL-19934); unsigned Signature VC (SIMPL-19934) vs "must not assume an unsigned VC" (SIMPL-21928, see F-025); Credential (wallet CDM); signing evidence (SIMPL-21963); signing result (SIMPL-21928) | proposed (D-012) |
| signature | A party's digital signature on the usage contract offer or the usage contract. | owner `CDM:signer-service-be:Signature` | `consumerSignatureDate`, `providerSignatureDate` | SIMPL-19933/AC2, 21948/AC2, 21928, 19934 | digital signature (SIMPL-19938); Consumer signature, Provider signature | open: stories disagree whether the Signer signs the contract data (SIMPL-35660) or the VC (SIMPL-19934) |
| validation | The check, by one party, of the other party's signature and of the signed content against the expected offer or contract (BP 7.6, 7.8). | none | contract-manager-be PUT `/verifications/{contractNegotiationId}`, `VerificationConfirmationResponse` | SIMPL-19933, 21948, 35584, 35677; BP7.6.1, 7.8.1 | verify, verification (API, SIMPL-35677); isValid (SIMPL-35863) | proposed (D-010) |
| validation result | The outcome of a validation, with a rejection reason when it fails (SIMPL-21948 table). | none | none (SIMPL-35863 `isValid` true or false, EDC side) | SIMPL-19933/AC1 to AC10, 21948/AC1 to AC10 | validation outcome (SIMPL-21966) | open (D-016, F-014, F-015) |
| rejection reason | The code that says why a validation failed (SIMPL-21948: INVALID_PROVIDER_SIGNATURE and others). | none | none | SIMPL-19933/AC2 to AC9 (text only), 21948 (codes) | none | proposed (D-016, F-015) |
| revocation request | A request by one party to end a usage contract, with a reason, that the other party accepts or denies (SIMPL-31053, 31076). | none | none | SIMPL-31053, 31076 | none (the response is 'deny' in AC3 and 'Reject' in AC4.2, see F-054) | agreed |
| suspension | The Provider's temporary removal of access under a usage contract, with a reason (SIMPL-31115). | none | none | SIMPL-31115 | suspend | agreed |
| reactivation request | The Consumer's request, with a justification, to lift a suspension (SIMPL-31116). | none | none | SIMPL-31116, 31117 | none | agreed |
| termination | The Provider ending a usage contract when its termination conditions are met (SIMPL-31118). | none | contract-manager-be `StatusUpdate.status` TERMINATED (different meaning, see D-008) | SIMPL-31118; SIMPL-35692 (offer timeout) | terminate unsigned offers (SIMPL-35692); EDC terminates the negotiation (SIMPL-35660/AC3) | open (D-008) |
| contract consent assertion | The consent assertion of type CONTRACT linked to one usage contract, with status ACTIVE or WITHDRAWN (consent CDM). | owner `CDM:consent-management-be:ConsentAssertion` (Type CONTRACT) | none in this component | SIMPL-31047 | contract consent status (SIMPL-31047/AC1); resource consent status is the RESOURCE type (SIMPL-32511/AC1) | proposed (D-025) |
| Contract Clause document | A document the Provider uploads, stored on the Provider side, with metadata documentId, documentName, mediaType, hash and hashAlgorithm (SIMPL-28846). | none | none (upload API not in the input) | SIMPL-28846; BP10 ('contract clause holding the billing schema') | uploaded file (SIMPL-28846, same artefact) | agreed (no CDM, D-016) |
| usage contract audit data | The number of usage contracts per status at a given moment, produced for the logging and monitoring component (SIMPL-34633). | none | none | SIMPL-34633/AC1 | aggregations, usage contract audit (SIMPL-34633) | proposed (D-016, F-009) |
| notification request | The message Contract Management hands to the Notification Service to email the Consumer (SIMPL-21966). | owner Notification Service (no CDM in the input) | `EmailNotification` (AsyncAPI, not in the input) | SIMPL-21966 | EmailNotification | proposed (D-016) |
| billing data | Data prepared from the billing schema, the parties and the reference to the usage contract, stored by both parties (BP10). | none | none | BP10 only, no story | none | agreed (no story) |

## Components and collaborators

| Term (proposed) | Definition (from the specs) | CDM | Where | Synonyms found (where) | Status |
|---|---|---|---|---|---|
| Contract Manager | Manages the contract lifecycle, offer templates and agreement persistence (BP07 dynamic view). | contract-manager-be.md | contract-manager-be; most stories | Contract Backend (SIMPL-22690, flow.puml); CM (SIMPL-35584 and others); Provider Contract Manager, Consumer Contract Manager (side qualifiers, fine) | proposed (D-022) |
| Contract UI backend | Backend of the contract manager UI; stores and serves the human-readable offer. | contract-ui-be.md | contract-ui-be; SIMPL-22690, 35526, 35660, 35692 | Contract-ui backend (flow.puml) | agreed |
| Contract Orchestrator | Sits between Contract Backend and EDC over Kafka (SIMPL-22690, flow.puml). | none | SIMPL-22690; flow.puml | none | open (D-014) |
| Connector | Contract negotiation control plane on both sides (BP07 dynamic view). | none (EDC Connector has no CDM, consumption CDM section 1) | all EDC callbacks | EDC, EDC connector (stories); Dataspace connector (contract-manager-be info) | proposed (D-022) |
| VC Issuer | Issues Verifiable Credentials for usage contracts (BP07 dynamic view). | issuer-service-be.md | SIMPL-19934, 35584, 35660, 35677 | VC Issuer Service (SIMPL-19934), VC issuer | proposed (D-022) |
| Signer Service | Signs the usage contract VC (BP07 dynamic view). | signer-service-be.md | SIMPL-35584, 35660 | Digital Signature Service (SIMPL-19934) | proposed (D-022) |
| Wallet | Stores the signed usage contract VCs on both sides (BP07 dynamic view). | wallet-service-be.md | SIMPL-19927, 19938, 35440, 35584, 35660 | digital wallet (SIMPL-19927, 19938); wallet service (SIMPL-35440, 35584, 35660) | proposed (D-022) |
| Notification Service | Existing SIMPL service that sends the email (SIMPL-21966). | none in the input | SIMPL-21966 | none | agreed |
| consent manager | Component where the Provider changes the contract consent assertion (SIMPL-31047). | consent-management-be.md | SIMPL-31047 | Consent Management (CDM) | agreed |
| Governance Authority | Configures the data space and receives audit data (BP07 prerequisites, SIMPL-34633). | none | BP07; SIMPL-34633 | none | agreed |

## Context only (contract-consumption-adapter-be, no story describes it)

| Term (proposed) | Definition | CDM class | API schema | Synonyms | Status |
|---|---|---|---|---|---|
| resource address template | A labelled template that defines the destination parameters a consumer supplies for a transfer (consumption CDM). | `CDM:contract-consumption-adapter-be:ResourceAddressTemplate` (owned) | `Template` | DestinationAddressTemplate (operation ids) | proposed (D-027) |
| transfer process (EDC) | Runtime transfer executed by the EDC connector (consumption CDM). | `CDM:contract-consumption-adapter-be:EDCTransferProcess` | `TransferProcess`, `TransferProcessId`, `TransferRequest` | transfer (contract-manager-be uses 'transfer' for the offer hand-over, D-023) | proposed |
| EDC contract agreement | The operational agreement produced by EDC negotiation finalisation (consumption CDM). | `CDM:contract-consumption-adapter-be:EDCContractAgreement` | `ContractNegotiation.contractAgreementId`, `TransferRequest.contractId` | contractId (D-028) | open (D-002) |

## Value sets

Per entity, each state with its name in the CDM, each API enum, the ACs and the UI. "none" means the source has no value for it.

### Usage contract offer

| State (proposed) | CDM | contract-manager-be `ContractOffer.status` / `ContractOfferStatusUpdate.status` | contract-ui-be `ContractOffer.status` | ACs | UI label |
|---|---|---|---|---|---|
| Requested | none (UI CDM status has no values; cdm.puml: CREATED default) | CREATED | CREATED | 'Requested' (SIMPL-30959/AC1, 32511/AC1 response); "In progress" (SIMPL-21924/AC2); 'pending' (SIMPL-35692/AC1) | Requested (SIMPL-30959) |
| Accepted | none (cdm.puml: ACCEPTED) | APPROVED | ACCEPTED | "Accepted" (SIMPL-32505/AC2); 'accept' operation (SIMPL-35660/AC1) | Accepted |
| Rejected | none (cdm.puml: REJECTED) | REJECTED | REJECTED | 'reject' operation (SIMPL-35660/AC3); rejected (SIMPL-32505/AC3) | Rejected |
| Signed (by Consumer) | none | none | none | 'marked as signed' (SIMPL-21928/AC2) | Signed (SIMPL-30959) |
| Expired (proposed for timeout) | none | none | none | 'terminated' and 'Terminated' (SIMPL-35692/AC1, AC4) | Terminated |
| finalized (meaning open) | none | none | none | 'already finalized' (SIMPL-35692/AC3) | none |

Decision operation (both APIs, path enum): `accept`, `reject`.

### Usage contract

| State (proposed) | CDM `ContractAgreement.status` | contract-manager-be `ConfirmationResponse.status`, `ContractAgreementData.status` | `StatusUpdate.status` | ACs | UI label |
|---|---|---|---|---|---|
| (process) initiated | ContractAgreementStatus, no values | INITIATED | none | none | none |
| (process) created | no values | CREATED | none | none | none |
| (process) finalizing | no values | FINALIZING | none | none | none |
| Finalized | no values | FINALIZED | FINALIZED | "Finalized" (SIMPL-19934/AC1, as negotiation state); 'validated / finalized' (SIMPL-21948 table) | Signed? (SIMPL-30959, mapping open) |
| Active | no values | none | none | 'Active' (SIMPL-31076/AC3, 31117/AC2, 30959/AC1) | Active |
| Suspended | no values | none | none | 'Suspended' (SIMPL-31115/AC2, 31116, 31117) | Suspended |
| Reactivation requested | no values | none | none | 'Reactivation requested' (SIMPL-31116/AC2, 31117/AC1) | Reactivation requested |
| Revocation requested | no values | none | none | 'Revocation requested' (SIMPL-31053/AC2, 31076/AC1) | Revocation requested |
| Revoked | no values | none | none | 'Revoked' (SIMPL-31076/AC2, AC4.1) | Revoked |
| Terminated | no values | TERMINATED (meaning: process ended, see D-008) | TERMINATED | 'Terminated' (SIMPL-31118/AC2) | Terminated |

Open: whether the process states INITIATED, CREATED, FINALIZING belong to the usage contract or to the EDC negotiation (D-005).

### Contract negotiation (EDC, external)

| State | Consumption CDM `NegotiationState` | adapter `ContractNegotiation.state` | ACs |
|---|---|---|---|
| initial to verified | INITIAL, REQUESTED, OFFERED, ACCEPTED, AGREED, VERIFIED | free string (example CONFIRMED) | none |
| finalized | FINALIZED | free string | "contract negotiation state as Finalized" (SIMPL-19934/AC1) |
| terminated | TERMINATED | free string | 'EDC terminates the negotiation' (SIMPL-35660/AC3) |

### Validation result

| Value (proposed) | CDM | API | ACs |
|---|---|---|---|
| successful | none | `isValid` true (EDC, SIMPL-35863); `VerificationConfirmationResponse.status` INITIATED is only the acknowledgement | 'marked as successful' (SIMPL-19933/AC1, 21948/AC1); SUCCESS (SIMPL-21948 table) |
| rejected | none | `isValid` false | 'rejected or failed' (SIMPL-19933/AC2 to AC9, 21948/AC2 to AC9); REJECTED / FAILED, REJECTED / IGNORED (table) |
| failed | none | `isValid` false | FAILED / PENDING_RETRY (SIMPL-21948 table) |
| pending | none | none | 'remains pending' (SIMPL-19933/AC8, 21948/AC8) |

Rejection reasons (SIMPL-21948 only): INVALID_PROVIDER_SIGNATURE, PAYLOAD_INTEGRITY_MISMATCH, CONTRACT_VALIDITY_ISSUE, PROVIDER_CREDENTIAL_TRUST_ISSUE, SCHEMA_OR_CONTENT_VALIDATION_FAILURE, DUPLICATE_OR_REPLAYED_SUBMISSION, TECHNICAL_VALIDATION_ERROR, MISSING_CONTRACT_DATA. The Provider side (SIMPL-19933) has the same cases in text, with no codes (F-015).

### Revocation response

| Value | ACs |
|---|---|
| accept | 'respond with accept' (SIMPL-31076/AC2); 'Accept decision' (AC4.1) |
| deny | 'respond with deny' (SIMPL-31076/AC3); 'Reject decision' (AC4.2), see F-054 |

### Contract consent assertion (external, Consent Management)

| Value | Consent CDM `ConsentAssertion.Status` | ACs |
|---|---|---|
| active | ACTIVE | none named (SIMPL-31047 says only "contract consent status") |
| withdrawn | WITHDRAWN | none named |

## Counts

45 rows in all.

| Table | Rows | agreed | proposed | open |
|---|---|---|---|---|
| Business objects and processes | 32 | 7 | 17 | 8 |
| Components and collaborators | 10 | 4 | 5 | 1 |
| Context only | 3 | 0 | 2 | 1 |
| Total | 45 | 11 | 24 | 10 |
