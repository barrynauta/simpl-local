# Glossary: contract-management (final, step 30a)

The vocabulary for the build and the tests. It settles the `proposed` and `open` terms of the draft glossary in [`../validation/glossary.md`](../validation/glossary.md). Decisions `D` and `E` are in [`decisions.json`](./decisions.json). Business terms come from the business process first; published API names stay as they are and are listed as wire names (D19, D22). New names in code, tests and logs use the term in the first column.

## Business objects and processes

| Term | Definition | CDM class | API names (wire) | Retired synonyms | Decision |
|---|---|---|---|---|---|
| usage contract | Legally binding contract between a Consumer and a Provider for the use of one resource. One record per contractNegotiationId and side in the Contract Manager, which owns it; its first phase is the usage contract offer. | `ContractAgreement` (rename to UsageContract proposed) | `ContractAgreementData`, `ContractAgreementCreate`, paths `/agreements/...`, `/credentials/agreements/...` | contract agreement (except as wire name), contract, Usage Contract Agreement, contract data | D19, D20 |
| usage contract offer | The offer phase of a usage contract: created on the Consumer side when the Connector asks for it (D4), reviewed and accepted or rejected by the Consumer user. | phase of `ContractAgreement` | `ContractOffer.status` in contract-ui-be; path `/contractOffers/{contractNegotiationId}` | contract offer, Contract Offer, usage contract offer overview, machine-readable usage contract offer | D21 |
| human-readable usage contract offer | Value object: the HTML rendering of the usage contract offer (`humanReadable`) and its SHA-256 hash (`hash`), stored by contract-ui-be for the user. | `HumanReadableContractData` (align names) | `ContractOffer` {humanReadable, hash, status} in both APIs | contract offer presentation, contract offer data, usageContract (html), usageContractHash, humanReadableData, contractData | D21, E8 |
| EDC contract offer | The Connector's offer for a dataset. External; referenced by `contractOfferId` only. | `ContractOffer` (external, owner Connector) | `contractOfferId` | ContractOffer (for this meaning) | D21, D24 |
| usage contract offer request | The Consumer's request for a usage contract offer for a resource (BP 7.1.1), carried by the Connector. | none | none in this component | usage contract request, Contract Offer Request | D25 |
| usage contract establishment | The whole process BP07 from the request to the signed usage contract. | none | none | contract establishment process, contract negotiation process, contract negotiation flow | D25 |
| contract negotiation (EDC) | The Connector protocol exchange; its id `contractNegotiationId` is the identity of the usage contract (D20). | `ContractNegotiation` (external, owner Connector) | `contractNegotiationId` | negotiation (alone) | D20, D24 |
| contract definition (EDC) | The Provider's Connector binding of an asset to policies. External; referenced by `contractDefinitionId`. | `ContractDefinition` (external, owner Connector) | `contractDefinitionId` | none | D24 |
| resource description | The catalogue description of the resource a usage contract covers. In this build referenced through the EDC `assetId` (D23). | `Asset` (external, owner Federated Catalogue as ResourceDescription) | `assetId` | asset (as business term), self-description, selfDescriptionId, resource (alone) | D23 |
| Participant | An organisation onboarded into the data space. | external, owner Identity Provider | `PARTICIPANT_ID` (E3) | participant (lower case) | D24 |
| Consumer | The Participant that requests and uses a resource (a role of Participant). | `Consumer` (external, role of Participant) | `consumerId` | Consumer user, Consumer end user (except for the human user), ConsumerId | D24 |
| Provider | The Participant that offers a resource (a role of Participant). | `Provider` (external, role of Participant) | `providerId` | proverId, Provider user (except for the human user) | D24 |
| access policy | The policy of the resource description that decides whether a Consumer may request a usage contract (BP 7.2.1). Checked by the Connector, not built here. | external | none | contract access policies | D2 |
| existence check | The check that the Consumer has no active usage contract for the same resource (BP 7.2.2): POST search plus the /isPresent callback. | none | `POST /agreements/{contractNegotiationId}/search`, `ContractSearchConfirmation`; Connector `/isPresent/...` | validateExistenceContract, isPresent (as business term), verify existence | D3 |
| active usage contract | A usage contract in state Active, or in a reserved state Suspended, Revocation requested or Reactivation requested. Blocks a new offer for the same consumerId and assetId. | none | none | active contract | D3 |
| signed usage contract offer | The usage contract after one party signed and before both signatures are validated (state Signed, on both sides since D31). | phase of `ContractAgreement` | status `FINALIZING` | signed usage contract (for this meaning), Consumer-signed usage contract offer, signing evidence | D12 |
| signed usage contract | The usage contract signed by both parties and validated (state Active). | `ContractAgreement` | status `FINALIZED` | fully-signed usage contract, final contract signed by both parties | D12 |
| Verifiable Credential (VC) | The credential each party's VC Issuer issues for the usage contract, holding the contract data and the party's signature value (E8), stored in that party's Wallet. External. | `VerifiableCredential` (external, owner VC Issuer) | `VerifiableCredential`, `GET /verifications/{contractNegotiationId}/vc` | Signature VC, unsigned Signature VC, Credential (wallet), signing result, signing evidence | D5, D24 |
| signature | A party's digital signature on the canonical contract data, made by the Signer Service and carried in its VC as `signatureValue`. | `Signature` (external, owner Signer Service) | `consumerSignatureDate`, `providerSignatureDate` | digital signature | D5, E8 |
| validation | One party's check of the other party's VC and signed content (BP 7.6 and 7.8). | none | `PUT /verifications/{contractNegotiationId}`, `validateAndIssue`, Connector `/isValid/...` | verify, verification (for this step) | D22, D9 |
| validation result | The outcome of a validation: SUCCESS, REJECTED, FAILED or IGNORED, with a rejection reason. Stored with the usage contract. | `ValidationResult` (new, internal) | `rejectionReason` in the 422 and 409 problem details | validation outcome | D9, D24 |
| rejection reason | Code that says why a validation did not succeed (list below). | attribute of `ValidationResult` | `rejectionReason` | none | D9 |
| revocation request | A request by one party to end a usage contract, with a reason. Not built. | none | none | none | D15 |
| suspension | The Provider's temporary removal of access under a usage contract. Not built. | none | none | none | D15 |
| reactivation request | The Consumer's request to lift a suspension. Not built. | none | none | none | D15 |
| termination | The Provider ending a usage contract whose termination conditions are met (reserved state Terminated). Only this meaning. Not built. | none | none (the wire value TERMINATED means Closed, see states) | terminate unsigned offers (now: expiry), EDC terminates the negotiation (now: Closed) | D13, D15 |
| expiry | The closing of an unsigned usage contract offer by the timeout scheduler (state Expired). | none | contract-ui-be status `EXPIRED` (new) | Terminated (for offers) | D13 |
| contract consent assertion | The consent assertion of type CONTRACT linked to one usage contract (ACTIVE or WITHDRAWN). External, not built. | `ConsentAssertion` (external, owner Consent Management) | none | contract consent status, contract consent assertion status | D16 |
| usage contract audit data | The number of usage contracts per state at the time of the request. | `UsageContractStatusCount` (new, internal) | `GET /audits/usageContractStatusCounts` | aggregations, usage contract audit | D17 |
| notification request | The message the Contract Manager hands to the Notification Service; the Notification Service resolves the address and sends the email. | `NotificationRequest` (new, internal; external owner Notification Service) | stub `POST /notifications` (E4) | EmailNotification | D11 |
| Contract Clause document | A document the Provider uploads with hash metadata. Not built. | none | none | uploaded file | D18 |
| billing data | Data prepared for billing from the usage contract (BP10). No story; not built. | none | none | none | D2 |

## Components and collaborators

| Term | Definition | Where | Retired synonyms | Decision |
|---|---|---|---|---|
| Contract Manager | contract-manager-be: owns the usage contract, its state and the translation to the Connector. | contract-manager-be | Contract Backend, Contract backend; CM only as an abbreviation | D2, D25 |
| Contract UI backend | contract-ui-be: stores the human-readable usage contract offer and the user decision, runs the timeout scheduler. | contract-ui-be | Contract-ui backend | D14 |
| Contract Orchestrator | Former intermediary between the Contract Manager and the Connector over Kafka. Not part of the target design; not built. | none | none | D2 |
| Connector | The EDC connector of each party (contract negotiation control plane). A stub in tests. | stub (E4) | EDC (alone), EDC connector, Dataspace connector | D2, E4 |
| VC Issuer | Issues and validates the VCs of usage contracts. A stub in tests. | stub (E4) | VC Issuer Service, VC issuer | E4 |
| Signer Service | Signs the canonical contract data. A stub in tests. | stub (E4) | Digital Signature Service | E4 |
| Wallet | Stores each party's VCs. A stub in tests. | stub (E4) | digital wallet, wallet service | D6, E4 |
| Notification Service | Sends the emails for notification requests. A stub in tests. | stub (E4) | none | D11 |
| consent manager | Consent Management component. Not called in this build. | none | none | D16 |
| Governance Authority | Configures the data space and receives audit data. | none | none | D17 |

## Context only (contract-consumption-adapter-be, not built)

| Term | Definition | Wire names | Retired synonyms | Decision |
|---|---|---|---|---|
| resource address template | A labelled template of destination parameters for a transfer. | `Template` | DestinationAddressTemplate | D2, D25 |
| transfer process (EDC) | A runtime data transfer executed by the Connector. Never used for the offer hand-over. | `TransferProcess`, `TransferRequest` | transfer (for the offer hand-over; TransferConfirmationResponse stays only as a wire name) | D2, D25 |
| EDC contract agreement | The Connector's agreement from a finalised negotiation. Its id is the `contractAgreementId` of the usage contract (D19). | `contractAgreementId`, `contractId` (adapter) | contractId (as business term) | D19 |

## State tables

### Usage contract (Contract Manager, one state machine, D12)

| State | Meaning | Wire value `ContractAgreementData.status` | UI label | Reached by |
|---|---|---|---|---|
| Requested | Offer prepared (Consumer) or Provider record created, not yet signed | CREATED | Requested | D4 (Consumer), D7 start (Provider) |
| Signed | Signed by one side, waiting for the other side's result: the Consumer after accepting (D5), the Provider after validateAndIssue (D31) | FINALIZING | Signed | D5 accept, D31 |
| Active | Signed usage contract: both signed, validated | FINALIZED | Active | Provider: the Consumer's confirmation passed on (D31); Consumer: D8 success |
| Closed | Ended without a usage contract: rejected, expired or failed validation | TERMINATED | Closed | D5 reject, D13 timeout, D7 rejection, D8 failure |
| Suspended | reserved, not built | none yet | Suspended | D15 |
| Revocation requested | reserved, not built | none yet | Revocation requested | D15 |
| Revoked | reserved, not built | none yet | Revoked | D15 |
| Reactivation requested | reserved, not built | none yet | Reactivation requested | D15 |
| Terminated | reserved, not built; Provider ended an active usage contract | none yet (TERMINATED is taken by Closed) | Terminated | D15 |

INITIATED appears only in acknowledgements (`ConfirmationResponse`, `VerificationConfirmationResponse`, `TransferConfirmationResponse`), never as a stored state.

Transitions built (D12, refined by D31 and D32): Requested to Signed (both sides); Requested to Closed; Signed to Active (the other side confirmed); Signed to Closed (passed-on rejection or Signed timeout, default one day); Active to Closed on the Consumer side only as the compensating step of D32. Requested to Active no longer exists. Reserved transitions (proposal, D15): Active to Revocation requested; Revocation requested to Revoked or to Active; Active to Suspended; Suspended to Reactivation requested; Suspended or Reactivation requested to Active; Reactivation requested to Suspended; Active, Suspended, Revocation requested or Reactivation requested to Terminated.

### Usage contract offer view (contract-ui-be, D12, D13, D14)

| State | Wire value `ContractOffer.status` (contract-ui-be) | contract-manager-be `ContractOffer.status` | UI label | Meaning |
|---|---|---|---|---|
| Requested | CREATED | CREATED | Requested | Waiting for the user decision (the only pending state) |
| Accepted | ACCEPTED | APPROVED (not used in this build; rename to ACCEPTED proposed) | Accepted | User accepted and the Consumer signing succeeded |
| Rejected | REJECTED | REJECTED | Rejected | User rejected |
| Expired | EXPIRED (new value) | none | Expired | Closed by the timeout scheduler |

### Validation result (D9)

| Value | Meaning | Usage contract state after | Connector callback |
|---|---|---|---|
| SUCCESS | All checks passed | Signed on the Provider side until the Consumer confirms (D31), then Active | Provider: /signed true; Consumer: /isValid true |
| REJECTED | A business check failed | Closed | /isValid false (both sides) |
| FAILED | Technical error | Provider: stays Requested (caller may retry); Consumer: Closed | Provider: none (500 to the caller); Consumer: /isValid false |
| IGNORED | Duplicate or replayed submission on an Active or Closed record | unchanged | none (409 to the caller) |

Rejection reasons: MISSING_CONTRACT_DATA, INVALID_PROVIDER_SIGNATURE, INVALID_CONSUMER_SIGNATURE, PROVIDER_CREDENTIAL_TRUST_ISSUE, CONSUMER_CREDENTIAL_TRUST_ISSUE, SCHEMA_OR_CONTENT_VALIDATION_FAILURE, PAYLOAD_INTEGRITY_MISMATCH, CONTRACT_VALIDITY_ISSUE, TECHNICAL_VALIDATION_ERROR, DUPLICATE_OR_REPLAYED_SUBMISSION. The Consumer side uses the PROVIDER variants, the Provider side the CONSUMER variants.

### Contract negotiation (EDC, external)

The Connector's states (INITIAL to FINALIZED, TERMINATED) are not stored by this component. The Contract Manager only receives and sends contractNegotiationId and the callbacks.

### Revocation response (reserved, D15)

| Value | Retired synonym |
|---|---|
| accept | Accept decision |
| deny | Reject decision |

### Contract consent assertion (external, not built)

ACTIVE, WITHDRAWN (Consent Management CDM).
