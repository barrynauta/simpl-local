# Spec digest: Contract Management

Input set: `specs/` as frozen on 2026-10-29 (36 stories, three OpenAPI folders, BP07 and BP10). Nothing else was read, apart from public standards: W3C Verifiable Credentials Data Model 2.0, W3C Data Integrity EdDSA Cryptosuites (`eddsa-jcs-2022`), RFC 8785 (JCS), RFC 9457 (problem details), the W3C `did:key` method and the Eclipse Dataspace Protocol (for the negotiation vocabulary).

This document is the shared contract between the test writer and the code writer. Where it says "decision", both follow it, even when a spec says something else. Every decision is also a finding for the spec owners.

Words used throughout:

- **CM**: the contract manager backend (OpenAPI `api/contract-manager-be`, base path `/contract/v1`).
- **UIBE**: the contract UI backend (OpenAPI `api/contract-ui-be`, base path `/contract-ui/v1`).
- **UI**: the contract manager UI (frontend).
- **Party**: one participant's installation of CM + UIBE + UI. The run has two: the Provider party (`provider-01`) and the Consumer party (`consumer-01`).
- **Stub**: the single stand-in service for everything outside the component (connector, signer, VC issuer, wallet, catalogue, participant directory, consent manager).
- **nid**: `contractNegotiationId`. **aid**: `contractAgreementId`. **did** in paths: `contractDefinitionId`.

---

## 1. Source hierarchy

The earlier rule (OpenAPI > user stories > business process) holds, with three refinements for this input.

| Rank | Source | Wins on |
|---|---|---|
| 1 | OpenAPI of CM and UIBE (`openapi3-v1.yaml` in each folder) | Paths, methods, path parameters, field names, documented status codes, security scheme. Inside one file, the schema wins over the example, except where a decision row says otherwise (D9). |
| 2 | The newest stories, created in October 2026: SIMPL-35440, 35526, 35584, 35660, 35677, 35692, 35858, 35863, 35888 | Behaviour of the establishment flow and which party runs an operation. They describe the current target flow (connector calls CM directly over HTTP, CM calls connector callbacks). |
| 3 | Replacement stories ("New proposed: Replaces ..."): SIMPL-32505, 32509, 32511 | Over the story they replace. |
| 4 | Other live stories | Behaviour where ranks 1 to 3 are silent. |
| 5 | Replaced stories still live: SIMPL-19824, 19829, 22690 | Only where their replacement is silent (for example the "ACK now, result later" model of SIMPL-19824). |
| 6 | PlantUML files next to the UIBE OpenAPI (`cdm.puml`, `flow.puml`) | Data names only. The flow they draw (an orchestrator between connector and CM over Kafka) is older than rank 2 and is superseded. |
| 7 | BP07 (process steps, outcomes), BP10 | Intent and order of steps. BP10 is context only. |

Refinements:

1. **A story path or method that differs from the OpenAPI for the same operation loses to the OpenAPI.** The story's behaviour still applies to the OpenAPI operation (D4).
2. **A story endpoint with no OpenAPI counterpart** is mapped to the OpenAPI operation with the same intent if one exists (D4). If none exists, it is added with the shape the story gives (D5).
3. **Two backends exposing the same paths is not a conflict.** `/contractOffers/{nid}` exists on both, but under different base paths and for different callers: the UI calls UIBE, UIBE calls CM, CM calls UIBE. They form a chain (D1).

---

## 2. Component map

### 2.1 Who owns what

| Concern | Owner in this run | Notes |
|---|---|---|
| Contract record, state machine, lifecycle, validation, signing orchestration, audit counts, contract clause upload | CM (one per party) | Single source of truth for contract status at a party. |
| Offer presentation (human-readable HTML, hash, offer decision status), offer expiry scheduler, user-facing API with token checks | UIBE (one per party) | UIBE is a thin backend for the UI. It keeps its own offer table and reads contract data from its own CM. |
| Screens: overview, details, actions | UI (one per party) | Calls only its own UIBE. Never calls CM. |
| Negotiation transport (DSP), access policy check, VC exchange between parties, callbacks `/isPresent`, `/isValid`, `/signed`, `/vcExchange` | Stub (connector part) | Stands in for both connectors. Also drives a whole negotiation on request. |
| Signer, VC issuer, VC verification | Stub (trust part) | One "issue" call returns a signed VC. |
| Wallet | Stub (wallet part) | One logical wallet per participant. |
| Catalogue (resource descriptions), participant directory (names, DIDs, contact emails), consent manager page | Stub | Read-only for CM, seeded through stub control endpoints. |
| Notification Service | Out. CM only publishes to the Kafka topic `notifications`. | Tests read the topic. |
| Identity provider | Keycloak, one realm per party | |

### 2.2 How the parties talk

```
                 Provider party                                   Consumer party
  UI-P --> UIBE-P --HTTP--> CM-P                    CM-C <--HTTP-- UIBE-C <-- UI-C
                     ^        |  ^                    ^  |        ^
                     |   HTTP |  | HTTP          HTTP |  | HTTP   | HTTP (POST /contractOffers)
                     |        v  |                    |  v        |
                     |      +-----------------------------------+ |
                     |      | Stub: connector (both sides),     | |
                     |      | signer + VC issuer, wallet,       | |
                     |      | catalogue, directory              | |
                     |      +-----------------------------------+ |
                     |                                            |
           CM-P <----+--------- Kafka (shared broker) -------------+--> CM-C
                     lifecycle messages, status events, notifications
```

- **Establishment (BP07) is HTTP only**, CM to connector stub and back. The two CMs never call each other directly. This follows the rank 2 stories and the CM OpenAPI title ("HTTP APIs for interaction with the Dataspace connector").
- **Lifecycle after establishment (suspend, reactivate, revoke, terminate) crosses parties over Kafka**, on a shared broker that stands in for the connector-mediated channel (D46). No spec defines this channel.
- **Inside a party**, UI to UIBE to CM is HTTP. CM to UIBE is HTTP (`POST /contractOffers/{nid}` on UIBE). UIBE does not use Kafka.

### 2.3 What the consumption adapter OpenAPI tells us (context, not scope)

`api/contract-consumption-adapter-be` is the Consumer-side entry point in front of the connector. It shows:

- The negotiation really starts with `POST /contracts` (body `providerEndpoint`, `contractDefinitionId`, `assetId`), returning a `contractNegotiationId`. In this run the stub control endpoint `POST /_control/negotiations` stands in for it (D17).
- `GET /contracts/{nid}` exposes the connector's own negotiation state (example `CONFIRMED`). That state machine belongs to the connector. CM does not mirror it.
- `contractId` used for transfers equals the agreement id. CM uses `contractAgreementId` for the same thing.
- It uses bearer JWT from the IAA gateway, while CM and UIBE use API keys. That difference is a boundary fact, not a defect of this component.

Nothing in the adapter is built or tested.

---

## 3. Scope

### 3.1 Stories (36)

| Verdict | Count | Stories |
|---|---|---|
| In | 28 | SIMPL-32509, 32511, 32505, 19927, 19933, 19934, 19938, 21928, 21948, 21963, 21966, 28846, 30959, 30960, 31047, 31053, 31076, 31115, 31116, 31117, 31118, 34633, 35440, 35526, 35584, 35660, 35677, 35692 |
| Stub contract (the story describes a connector endpoint that CM calls; the stub implements it) | 3 | SIMPL-35858 (`/isPresent`), 35863 (`/isValid`), 35888 (`/vcExchange`) |
| Out | 5 | SIMPL-19824, 19829, 22690 (replaced, see below); 21924 (catalogue UI, other component); 35889 (API definition task for the connector API, no acceptance criteria, body is a guideline checklist) |

Partial scope inside "In" stories:

- SIMPL-19927 and 19938: "requested to specify another secure location" is out. The error is reported, the negotiation fails (D57).
- SIMPL-34633: the "supplied audit rule package" is out. Only counts per status are produced (D62).
- SIMPL-31047: the consent manager itself is out. CM keeps a consent status field, the UI shows a link (D43).
- SIMPL-28846: the external retrieval mechanism and Resource Description update are out, as the story says.
- SIMPL-21966: delivery of the email is out (Notification Service). Only the published message is in.

### 3.2 Replaced story pairs

| Pair | Which counts | Why |
|---|---|---|
| 19824 (replaced) / 32509 | 32509 for intent and auth codes (401, 403). The OpenAPI operation `POST /agreements/{nid}/search` (which 19824 names) for shape. 19824 for the "ACK now, result later through the connector" model. | 32509's path `POST /resourceDescriptions/{id}/validateExistenceContract` is not in the OpenAPI (D4, D19). |
| 19829 (replaced) / 32511 | 32511 for the response fields and the 403 and 404 codes. 35526 (rank 2) for which party runs `/create`: the Consumer. | 32511 says the Provider creates the offer, 35526 moves the same operation to the Consumer side (D20). |
| 22690 (replaced) / 32505 | 32505. 22690 only for "UI never calls CM directly" and "acceptance is recorded in the audit trail". The orchestrator and Kafka hop it draws are out. | |
| 35888 / 35889 (both "API for VC exchange") | 35888 as the stub contract. 35889 is out. | 35889 has no acceptance criteria. |

### 3.3 Operations

| Group | In | Out |
|---|---|---|
| CM OpenAPI (12) | All 12: `GET /health`, `POST .../issue`, `POST .../create`, `PATCH /agreements/{aid}/definitions/{did}/status`, `GET /agreements/{aid}`, `GET /agreements/{aid}/file`, `POST /agreements/{nid}/search`, `POST /agreements/search`, `PUT /verifications/{nid}`, `POST /contractOffers/{nid}`, `PATCH /contractOffers/{nid}/{operation}`, `PUT /contractOffers/{nid}/update` | none |
| UIBE OpenAPI (5) | All 5: `GET /health`, `POST /contractOffers/{nid}`, `GET /contractOffers/{nid}`, `GET /contractOffers`, `PATCH /contractOffers/{nid}/{operation}` | none |
| Consumption adapter OpenAPI (9) | none | All 9 (context) |
| In scope without OpenAPI, CM (7) | `GET /verifications/{nid}/vc` (35440), `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions`, `PUT /contracts/{nid}/consentStatus`, `GET /audit/contractStatusCounts`, `POST /contractClauseDocuments` | |
| In scope without OpenAPI, UIBE (4) | `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions`, `POST /admin/offerExpiry/run` | |
| Story endpoints mapped, not built | | `POST /resourceDescriptions/{id}/validateExistenceContract` (32509), `POST .../validateAndIssue` (35584), `POST /verifications/{nid}` (35677), `POST /contractOffers/{nid}/{operation}` (35660) |

Shapes of all added operations are in Appendix A and B.

### 3.4 Business process steps

| Step | Verdict |
|---|---|
| BP07.01 Request usage contract offer | Out (catalogue UI, consumption adapter, connector). The stub starts the negotiation. |
| BP07.02 Verify access policy | Out (connector policy engine). The stub assumes "allowed". |
| BP07.03 Verify existence usage contract | In (`POST /agreements/{nid}/search`). |
| BP07.04 Provide usage contract | Partly: offer transport is the stub's; offer data preparation is CM at the Consumer (35526). |
| BP07.05 Review terms | In (UI details, UIBE, CM). |
| BP07.06 Sign (Consumer) | In. |
| BP07.07 Store (Consumer) | In (wallet stub). |
| BP07.08 Validate (Provider) | In. |
| BP07.09 Sign (Provider) | In. |
| BP07.10 Store (Provider) | In. |
| BP07.11 Notify Consumer | In through the connector stub (provider VC relayed, status update relayed). |
| BP07.12 Confirm usage contract (Consumer) | In (`PUT /verifications/{nid}`). |
| BP10 all steps | Out. Billing is another component. |

### 3.5 Other components

| Component | Verdict | Contract |
|---|---|---|
| Connector (EDC), both sides | Stub | Appendix C.1 and C.2 |
| Signer Service + VC Issuer | Stub (one call) | Appendix C.3 |
| Wallet | Stub | Appendix C.4 |
| Catalogue | Stub (read by asset id) | Appendix C.5 |
| Participant directory (names, DIDs, contact emails) | Stub | Appendix C.5 |
| Consent manager | Stub (a URL to redirect to, plus the CM endpoint it would call) | D43 |
| Notification Service | Out. Kafka topic only. | Appendix D.3 |
| Identity provider | Keycloak | D67 |
| Billing, catalogue UI, consumption adapter, connector policy engine | Out | |

---

## 4. Decisions

Each row: topic, what the sources say, decision for this run.

### 4.1 Component map and API

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D1 | Same paths on two backends | CM and UIBE both have `POST /contractOffers/{nid}` and `PATCH /contractOffers/{nid}/{operation}`. `flow.puml` shows the chain. | Different services, one chain. CM's `PATCH /contractOffers/{nid}/{op}` is called only by UIBE (API key). UIBE's `PATCH /contractOffers/{nid}/{op}` is called only by the UI (user token). UIBE's `POST /contractOffers/{nid}` is called only by CM (API key). CM's `POST /contractOffers/{nid}` is called by the connector stub (D33). |
| D2 | Which party a call runs at | 35526, 35584, 35660, 35677 say "only when called on the provider (consumer) side" but no spec says how CM knows its side. | Each CM is configured with its own participant id (`CM_PARTICIPANT_ID`). The side is decided per negotiation: Consumer side if the body's `consumerId` equals the own id, Provider side if `providerId` equals it. Without a body, the stored record's side is used. A call that cannot run at this side returns 400 (problem type `urn:problem-type:simpl:contract-management:wrongParty`). The same image runs at both parties. |
| D3 | Orchestrator and Kafka between connector and CM | 22690 (replaced) and `flow.puml` route connector -> orchestrator -> Kafka -> CM. Rank 2 stories have the connector call CM over HTTP and CM call connector callbacks. | No orchestrator. The connector stub calls CM over HTTP. CM calls the stub's connector callbacks over HTTP. |
| D4 | Story method or path differs from the OpenAPI | 35660: `POST /contractOffers/{nid}/{operation}`, 201. OpenAPI: `PATCH`, 200. 35677: `POST /verifications/{nid}`, "202 Created". OpenAPI: `PUT`, 200. 35584: `POST .../validateAndIssue`. OpenAPI has `POST .../issue` ("Start the process of signing contract"). 32509: `POST /resourceDescriptions/{id}/validateExistenceContract`. OpenAPI has `POST /agreements/{nid}/search`. | OpenAPI wins on method, path and code. `PATCH /contractOffers/{nid}/{op}` returns 200. `PUT /verifications/{nid}` returns 200. 35584's behaviour is built on `POST .../issue`. 32509's behaviour is built on `POST /agreements/{nid}/search`. The story paths are not built. |
| D5 | Endpoint with no OpenAPI counterpart | 35440: `GET /verifications/{nid}/vc`, both sides, 200 with VC, 404 if not signed, 401. | Added to CM as written (Appendix A.1). It returns this party's own signed VC for the nid. |
| D6 | Read model and lifecycle API for the UI | UI stories need overview, details and seven lifecycle actions. No OpenAPI covers them. CM's `POST /agreements/search` returns only ids. | Added to CM: `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions` (Appendix A.2 to A.4). CM owns all state rules. |
| D7 | UIBE operations for the UI | Same gap. 22690: "the frontend must not call Contract Backend directly". | Added to UIBE: `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions` (user token), `POST /admin/offerExpiry/run` (API key). UIBE checks the token and role, then calls CM. UIBE adds `humanReadableData`, `hashData` and `offerStatus` from its own table to the details (Appendix B). |
| D8 | UIBE `GET /contractOffers` returns one object or a list | Schema: `$ref ContractOffer` (one object). Example: an array. | A JSON array of offer objects (D9 field names). Empty array when there are none. No 404 for an empty list. |
| D9 | Offer field names in UIBE responses | Schema `ContractOffer`: `humanReadable`, `hash`, `status`. GET examples and `cdm.puml`: `contractNegotiationId`, `humanReadableData`, `contractData`, `hashData`, `status`, `createdDate`, `lastUpdatedDate`. | Requests (`POST /contractOffers/{nid}` on UIBE and CM) use the schema names `humanReadable`, `hash`. Responses of UIBE `GET /contractOffers`, `GET /contractOffers/{nid}` and `PATCH /contractOffers/{nid}/{op}` use the example names: `contractNegotiationId`, `humanReadableData`, `contractData` (always `null` in this run), `hashData`, `status`, `createdDate`, `lastUpdatedDate`. The PATCH 200 body is that JSON object, not a string as the schema says. |
| D10 | Offer status vocabularies | UIBE schema and `cdm.puml`: `CREATED`, `ACCEPTED`, `REJECTED`. CM `ContractOffer` and `ContractOfferStatusUpdate`: `CREATED`, `APPROVED`, `REJECTED`, with example `SIGNED`. 35692 needs "Terminated". | UIBE offer status: `CREATED`, `ACCEPTED`, `REJECTED`, `TERMINATED` (added for expiry). CM `PUT /contractOffers/{nid}/update` accepts only `CREATED`, `APPROVED`, `REJECTED`; anything else (including `SIGNED`) is 400. Offer status is separate from the contract status of D37. |
| D11 | Errors | OpenAPI: `application/problem+json`, RFC 9457, `type` as `urn:problem-type:simpl:<type>`. | Every 4xx and 5xx from CM and UIBE is `application/problem+json` with `type`, `title`, `status`, `detail`. Types used: `unauthorized`, `forbidden`, `notFound`, `methodNotSupported`, `notAcceptable`, `unsupportedMediaType`, `argumentTypeMismatch` (bad path or query value), `validationFailure` (bad body), `internalServerError`, and for this component `urn:problem-type:simpl:contract-management:<type>` with `wrongParty`, `invalidTransition`, `alreadyExists`, `offerNotPending`, `invalidDocument`. Wrong method: 405. `Accept` that excludes JSON: 406. Body with a content type other than `application/json` where a JSON body is expected: 415. No error leaks stack traces or internal hostnames (19824 security note). |
| D12 | Unknown id where the OpenAPI documents no 404 | CM `PATCH /contractOffers/{nid}/{op}`, `PUT /contractOffers/{nid}/update`, `PUT /verifications/{nid}`, `POST /contractOffers/{nid}`, `PATCH /agreements/.../status`, `POST .../issue` have no 404. | Return 400 `validationFailure` with detail "Unknown contractNegotiationId" (or contractAgreementId). Where 404 is documented (`GET /agreements/{aid}`, `GET /agreements/{aid}/file`, UIBE `GET` and `PATCH /contractOffers/{nid}`, `GET /verifications/{nid}/vc`) and on all added operations, return 404. On `POST .../create`, 32511 adds 403 and 404 (D20). |
| D13 | Health | Both OpenAPI files: `GET /health`, `application/vnd.spring-boot.actuator.v3+json`, `status: UP`. | Both expose `GET {base}/health` without auth, content type as documented, body with at least `"status": "UP"`. 503 with problem body when the database is down. No technology is mandated. |
| D14 | Correlation id | 22690, 21966, 21963: "correlationId where available". No header is named. | Header `X-Correlation-Id`. CM and UIBE accept it, generate a UUID if absent, echo it on every response, pass it on every outbound HTTP call, and put it in Kafka message headers (`correlationId`). |
| D15 | Date-time format | OpenAPI examples: `2024-05-05T10:20:30` (CM) and `2026-06-03T13:38:15.561754` (UIBE), no offset. | Fields of OpenAPI-documented responses: UTC, ISO-8601 local date-time without offset, as in the examples (any fraction length allowed). Added operations, events and VCs: RFC 3339 UTC with `Z`. Tests parse both leniently. |
| D16 | Id formats | `contractAgreementId` is `format: uuid` in paths. `contractNegotiationId` pattern `^[A-Za-z0-9:-]+$` only on `/agreements/{nid}/search`. 32509 says `ConsumerId (UUID)`; examples use `Cons2000`. | `contractAgreementId`: UUID, else 400 `argumentTypeMismatch`. `contractNegotiationId`, `contractDefinitionId`, `assetId`, `providerId`, `consumerId`: pattern `^[A-Za-z0-9:-]+$`, 1 to 100 characters, everywhere. Tests use UUIDs for nid and aid. |

### 4.2 Contract establishment

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D17 | Who starts the negotiation and how a test drives it | BP07.01, 21924 (catalogue UI, `startNegotiation`), adapter `POST /contracts`. | Out of scope for the build. The stub control endpoint `POST /_control/negotiations` starts a negotiation and runs the connector choreography of Appendix C.2 automatically. The only human step is the Consumer's accept or reject, which the test makes through Consumer UIBE (or the UI). A test drives a full negotiation from the Consumer side: start it at the stub, accept at Consumer UIBE, poll both CMs. |
| D18 | One negotiation id for both parties | All stories use one `contractNegotiationId`. The Dataspace Protocol gives each side its own process id. | One shared `contractNegotiationId` and one shared `contractAgreementId` at both parties for this run. Finding: the mapping between DSP process ids and the CM id is not described. |
| D19 | Existence check: which party, which contracts count, how the result travels | BP07.03 and 32509: the Provider checks. 35858: "Contract Manager on consumer side" informs the connector through `/isPresent/{nid}/{isPresent}`. 19824: 200 ACK now, result later through the connector. OpenAPI: `POST /agreements/{nid}/search`, 200 `ContractSearchConfirmation` (required `contractAgreementId`, `contractDefinitionId`; example also has `contractSearchTO`). 32509: response `resourceDescriptionId`, `ConsumerId`, `Exists`. 21924 AC4 blocks a second request while one is active. | The operation runs at whichever party is called; the stub calls it at the Provider (BP07.03). It checks this party's own records for a contract with the same `assetId`, `providerId` and `consumerId`, another nid, and an **open** status (REQUESTED, ACCEPTED, SIGNED, ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED). Body: `assetId` and `consumerId` required (400 if missing), `providerId` defaults to the own id. Response 200: `{contractAgreementId, contractDefinitionId, exists, contractSearchTO}`. If one exists, the two ids are those of the existing contract; else they echo the request (may be `null`). `contractSearchTO` echoes the request body. After responding, CM calls the stub `POST /edc/{ownId}/isPresent/{nid}/{exists}`. If `exists` is false and this is the Provider side, CM creates the Provider record in REQUESTED (enriched from catalogue and directory; enrichment failure does not fail the call). A repeated search for the same nid does not create a second record. |
| D20 | `/create`: which party, sync or async, response | 19829 and 32511: the Provider creates the offer and returns it to its connector. 35526 (rank 2): the same endpoint runs at the Consumer, does not call the connector back, gets resource data from the catalogue, stores contract data, calls UIBE `POST /contractOffers/{nid}`, notifies the user. OpenAPI: 201 `ConfirmationResponse` (`contractAgreementId`, `contractDefinitionId`, `status`), example `INITIATED`, codes 400, 401, 405, 415, 500. 32511 adds 403 (asset not owned by provider), 404 (asset not found) and response fields. | Runs at the Consumer only; at the Provider, 400 `wrongParty`. Synchronous: validate body (all five fields required, D16 pattern), 400 `alreadyExists` if the nid is known, read the asset from the catalogue stub (404 if absent, 403 if its `providerId` differs from the body's), read provider and consumer names from the directory stub, build the human-readable HTML and hash (D21), store the record in REQUESTED, call UIBE `POST /contractOffers/{nid}` (if UIBE fails: 500, no record kept), publish the "offer available" email (D22). Then 201 with `contractAgreementId`, `contractDefinitionId`, `status: "CREATED"` plus the 32511 fields: `contractNegotiationId`, `assetId`, `resourceDescriptionId`, `resourceDescriptionName`, `providerId` (the story's `proverId` is read as a typo), `providerName`, `consumerId`, `consumerName`, `contractOfferId`, `usageContract` (the HTML), `usageContractHash`, `contractStatus: "REQUESTED"`, `offerUrl`. No connector callback. |
| D21 | Human-readable offer and its hash | 30960: resource description properties, policies, contract clauses. UIBE example hash has 32 hex characters (MD5 length). 28846: SHA-256 for clause documents. | HTML fragment (no `<html>` wrapper) with, in this order: resource name, `resourceDescriptionId`, `assetId`, provider name, consumer name, description, a list of policies (type and description), a list of contract clauses (document name and hash). Exact markup is free. Every value is HTML-escaped. `hash` = lowercase hex SHA-256 of the UTF-8 bytes of the HTML. Tests check the hash by recomputing it and check that the listed values appear in the text. |
| D22 | Consumer notified that an offer is ready | 35526: "collect the user email and notify the user". No source of the email. 21966: recipient must not be hardcoded. | CM publishes an `EmailNotification` (Appendix D.3) to the Consumer participant's `contactEmail` from the directory stub. Subject: `Usage contract offer available for review: <nid>`. Message contains the `offerUrl`. If the directory has no contact email: no message, history entry `NOTIFICATION_SKIPPED`, `/create` still succeeds. |
| D23 | Accept or reject by the Consumer | 32505: accept button, status "Accepted", no accept when accepted, active, rejected, suspended or terminated. 35660: accept signs, reject calls `/signed/{nid}/false`. 30959: actions only on status "Requested". UIBE `PATCH /contractOffers/{nid}/{op}`. 22690: idempotent per nid. | UI -> UIBE `PATCH /contractOffers/{nid}/{accept|reject}` (user token, `contract_manager`). UIBE requires its offer status CREATED, else 400 `offerNotPending`. UIBE calls CM `PATCH /contractOffers/{nid}/{op}`; CM requires contract status REQUESTED at the Consumer side, else 400 `offerNotPending`. On CM success UIBE sets its offer status (ACCEPTED or REJECTED) and returns 200 with the offer object. On CM failure UIBE returns the same status code, keeps CREATED (22690 AC2). A repeated accept is 400, not a silent 200: 32505 counts over the replaced 22690. CM: accept -> ACCEPTED, then asynchronous signing (D24); reject -> REJECTED and CM calls stub `POST /edc/{ownId}/signed/{nid}/false`. CM response 200 `{contractNegotiationId, status: "INITIATED"}`. |
| D24 | Consumer signing | 21928: signing result registered only if the backend flow completes; not signed on failure; no "unsigned VC" as an artefact. 35660: sign with Signer, create VC with VC Issuer, store in wallet, call `/signed/{nid}/true`. 19927: wallet errors. | Asynchronous after the accept. CM calls stub `POST /vc-issuer/v1/credentials/issue` with its own participant id and the credential subject of D55 (`signerRole: CONSUMER`), then stores the VC in its wallet (`PUT /wallet/v1/{ownId}/credentials/{id}`), then sets SIGNED with `consumerSignatureDate`, then calls `POST /edc/{ownId}/signed/{nid}/true`. If issuing fails: FAILED, reason `SIGNING_FAILED`. If the wallet fails: FAILED, reason `WALLET_UNAVAILABLE`, `WALLET_FULL` or `WALLET_CORRUPTED` (D57). On any failure CM calls `/signed/{nid}/false`. |
| D25 | Provider validate-and-sign trigger | 35584: `validateAndIssue`, 201, 400 on the Consumer side. OpenAPI `/issue`: body `ContractAgreementCreate` with `required: false`, 201 `ConfirmationResponse` `INITIATED`. 19934: "returns HTTP 200 with status Finalized". 19933 AC10: result is never synchronous. | `POST /credentials/agreements/{aid}/definitions/{did}/issue` runs at the Provider only. With a body, the body's `providerId` must be the own id (else 400 `wrongParty`) and the record is created if absent. Without a body, a Provider record for the nid must exist from the existence check (D19), else 400 `validationFailure`. Path ids are stored on the record. Response 201 `{contractAgreementId, contractDefinitionId, status: "INITIATED"}`. Validation (D27) and signing (D29) happen after the response. The "200 Finalized" of 19934 does not apply (async model of 19933 wins). |
| D26 | How the Provider gets the Consumer's VC | 35584: CM provider -> connector provider -> connector consumer -> CM consumer, through `GET /vcExchange/{nid}/request` (35888) and `GET /verifications/{nid}/vc` (35440). 21963: delivery follows "the agreed integration flow". | Provider CM calls stub `GET /edc/{ownId}/vcExchange/{nid}/request`. The stub fetches the VC from Consumer CM `GET /verifications/{nid}/vc` (or returns a test override). 200 -> validate. 404 -> REJECTED with `MISSING_CONTRACT_DATA`. 5xx or unreachable -> technical error (D28). |
| D27 | Validation checks, their order, reason codes | 19933 (Provider) and 21948 (Consumer): signature, payload integrity, validity period, credential trust, schema, duplicate, technical error, missing data; result "rejected or failed"; mapping table in 21948 with reason codes. 35584 and 35677: technical validation through the VC Issuer validation endpoint, business validation against stored contract data. | One algorithm at both sides. The first failing check decides the reason. Codes are those of 21948, with the party in the name: (1) duplicate (D28); (2) no VC or no stored contract data: `MISSING_CONTRACT_DATA`; (3) VC lacks a required field of D53 or D55: `SCHEMA_OR_CONTENT_VALIDATION_FAILURE`; (4) stub verify returns `MALFORMED`: `SCHEMA_OR_CONTENT_VALIDATION_FAILURE`; `INVALID_SIGNATURE`: `INVALID_CONSUMER_SIGNATURE` or `INVALID_PROVIDER_SIGNATURE`; `UNKNOWN_ISSUER` or `REVOKED_ISSUER`: `CONSUMER_CREDENTIAL_TRUST_ISSUE` or `PROVIDER_CREDENTIAL_TRUST_ISSUE`; verify call fails: `TECHNICAL_VALIDATION_ERROR`; (5) VC `issuer` differs from the counterparty's DID in the directory: the `..._CREDENTIAL_TRUST_ISSUE` code; (6) now outside `validFrom`..`validUntil`: `CONTRACT_VALIDITY_ISSUE`; (7) any credential subject field of D55 differs from the stored record (`contractNegotiationId`, `contractAgreementId`, `contractDefinitionId`, `assetId`, `providerId`, `consumerId`, and at the Consumer also `usageContractHash`), or `signerRole` is not the counterparty's role: `PAYLOAD_INTEGRITY_MISMATCH`. `validationResult` values: `SUCCESS`, `REJECTED` (checks 2 to 7), `IGNORED` (duplicate), `FAILED` (technical). Stored on the record with `validationReasonCode` and `validatedAt`, shown in `GET /contracts/{nid}`. |
| D28 | Duplicate submission and technical errors | 19933 and 21948 AC7: a replay is detected, not processed as new. AC8: technical error is "failed or remains pending". | A validation trigger (`/issue` at the Provider, `PUT /verifications` at the Consumer) for a nid whose last `validationResult` is `SUCCESS` or `REJECTED` is a duplicate: result `IGNORED`, reason `DUPLICATE_OR_REPLAYED_SUBMISSION` written to history, status unchanged, no connector callback; the HTTP response is the normal one (201 or 200). After a `FAILED` (technical) result the status stays unchanged, no callback is made, and a new trigger runs a fresh validation (retry). |
| D29 | Provider outcome after validation | 19934: VC issuer + signer produce the final contract. 19938: store in wallet, retrievable through CM API. 35584: store VC in wallet, store contract data, "inform EDC by calling the sign endpoint". 35863: CM informs the connector of the VC validation result. 21966: notify the Consumer on failure. | Success: CM calls `POST /edc/{ownId}/isValid/{nid}/true`, issues its own VC (subject copied from the Consumer VC, `signerRole: PROVIDER`, new `signedAt`), stores the Consumer VC and its own VC in its wallet, sets SIGNED with both signature dates, calls `POST /edc/{ownId}/signed/{nid}/true`. Rejected: FAILED with the reason, call `/isValid/{nid}/false`, publish the "could not be validated" email to the Consumer (D30). Issue or wallet failure after a successful validation: FAILED with `SIGNING_FAILED` or `WALLET_*`, call `/signed/{nid}/false`. |
| D30 | Notification of an incorrectly signed offer | 21966: a message for the Notification Service, recipient from the participant context, no hardcoded contacts, failure logged, no invalid message when the recipient is unknown; Kafka topic "notifications", `EmailNotification` with `channel`, `to`, `cc`, `subject`, `message`. The AsyncAPI is not in the input. | On Provider validation result `REJECTED`, CM publishes one `EmailNotification` (Appendix D.3) to the Consumer's directory `contactEmail`. Subject: `Usage contract offer could not be validated: <nid>`. Message contains the nid and the reason code. No contact email: nothing published, history entry `NOTIFICATION_SKIPPED`. Publish error: history entry `NOTIFICATION_FAILED`, no retry. Not sent for `IGNORED` or `FAILED` (technical). |
| D31 | Consumer verification of the Provider VC | 35677: `POST /verifications/{nid}`, CM fetches the Provider VC through `/vcExchange`, then calls `/isValid/{nid}/{bool}`, 400 at the Provider side. OpenAPI: `PUT /verifications/{nid}` with the VC as a required body, 200 `{contractNegotiationId, status: INITIATED}`. 19927: store the fully signed contract in the wallet. | OpenAPI wins: the Provider VC comes in the body; CM does not call `/vcExchange` here. Runs at the Consumer only (record side CONSUMER, else 400 `wrongParty`); record must be SIGNED (else 400 `validationFailure`, except duplicates per D28). Response 200 `INITIATED`, then D27. Success: store the Provider VC in the wallet, set ACTIVE with `providerSignatureDate` (from the VC subject `signedAt`), call `/isValid/{nid}/true`. Rejected: FAILED with reason, `/isValid/{nid}/false`. Wallet failure: FAILED with `WALLET_*`, `/isValid/{nid}/false`. |
| D32 | Finalisation at the Provider | OpenAPI `PATCH /agreements/{aid}/definitions/{did}/status`, body `{status: FINALIZED or TERMINATED}`, "final step", example response `FINALIZING`. 19934: state "Finalized". | Runs at the Provider. `FINALIZED` is accepted only from SIGNED and sets ACTIVE. `TERMINATED` is accepted from REQUESTED or SIGNED and sets TERMINATED with reason `COUNTERPARTY_TERMINATED`. Anything else: 400 `invalidTransition`. Response 200 `{contractAgreementId, contractDefinitionId, status}` with the technical status of D38 after the change (`FINALIZED` or `TERMINATED`). |
| D33 | CM `POST /contractOffers/{nid}` and `PUT /contractOffers/{nid}/update` | OpenAPI: "Transfers a contract offer presentation to contract-ui-backend" (200 `INITIATED`); "Get status update from provider side and transfer it to UI side". No story uses them in the R17 flow. | Both run at the Consumer and need an existing Consumer record (else 400). `POST`: replaces the stored `humanReadable` and `hash` while the record is REQUESTED and forwards them to UIBE `POST /contractOffers/{nid}` (UIBE keeps the offer status); 200 `INITIATED`; not REQUESTED: 400 `offerNotPending`. `PUT .../update`: `REJECTED` moves REQUESTED, ACCEPTED or SIGNED to FAILED with reason `PROVIDER_REJECTED` and `additionalInfo` as the reason text; `APPROVED` and `CREATED` only add a history entry. 200 `INITIATED`. The connector stub uses `PUT .../update` to tell the Consumer that the Provider rejected or failed (Appendix C.2). |
| D34 | Agreement read operations | OpenAPI `GET /agreements/{aid}` (`ContractAgreementData`), `GET /agreements/{aid}/file` (text/plain example), `POST /agreements/search` ("existing active" agreements). | `GET /agreements/{aid}`: any record at this party with that aid; `status` is the technical status of D38; signature dates in D15 OpenAPI format or absent. `GET .../file`: 404 until the record has both signature dates; then `text/plain`, seven lines exactly as the example labels (`CONTRACT AGREEMENT:`, `CONTRACT DEFINITION:`, `ASSET:`, `PROVIDER ID:`, `PROVIDER SIGNATURE DATE:`, `CONSUMER:`, `CONSUMER SIGNATURE DATE:`), one space after the colon, dates `yyyy-MM-dd HH:mm:ss` UTC, `\n` line ends. `POST /agreements/search`: records with an **established** status (ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED) matching every given field; `{items: [{contractAgreementId, consumerId}]}`; empty `items` when none. |
| D35 | Contract validity and contract expiry | BP07: contracts are immutable and a new one is needed on expiry. 19933 and 21948 check a validity period. No story sets one. | VC `validUntil` = signing time + `CONTRACT_VC_VALIDITY` (ISO-8601 duration, default `P365D`). It is checked during validation only. No automatic status change when it passes. Finding: the contract term, its source (offer, policy or clause) and the expiry behaviour are not specified. |
| D36 | Notifying both parties of the outcome | BP07 outcomes: "both parties are notified" of success and of termination. | Only the two emails of D22 and D30 are sent. Every outcome is visible as a status event (Appendix D.2) and in the UI. Finding. |

### 4.3 Contract lifecycle

#### D37: the state machine

One contract record per party per nid. `status` uses this enum:

`REQUESTED`, `ACCEPTED`, `REJECTED`, `SIGNED`, `ACTIVE`, `FAILED`, `SUSPENDED`, `REACTIVATION_REQUESTED`, `REVOCATION_REQUESTED`, `REVOKED`, `TERMINATED`.

Sets used elsewhere: **open** = REQUESTED, ACCEPTED, SIGNED + established. **Established** = ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED. **Terminal** = REJECTED, FAILED, REVOKED, TERMINATED (no transition leaves them).

UI labels: Requested, Accepted, Rejected, Signed, Active, Failed, Suspended, Reactivation requested, Revocation requested, Revoked, Terminated.

Sources: 21924 ("In progress"), 30959 and 30960 (Requested, Rejected, Accepted, Signed, Active), 31053 and 31076 (Revocation requested, Revoked, Active), 31115 to 31118 (Suspended, Reactivation requested, Active, Terminated), 35692 (Terminated), 19934 ("Finalized"), OpenAPI technical statuses (D38). None of them gives the whole machine.

Establishment transitions:

| T | Party | From | To | Trigger | Actor | Side effects | Counterparty |
|---|---|---|---|---|---|---|---|
| E1 | Provider | (none) | REQUESTED | `POST /agreements/{nid}/search`, no open contract found | connector | `/isPresent/{nid}/false` | none |
| E2 | Provider | (none) | (none) | same, open contract found | connector | `/isPresent/{nid}/true`; no record | stub ends negotiation |
| E3 | Consumer | (none) | REQUESTED | `POST .../create` | connector | UIBE offer CREATED; email D22 | none |
| E4 | Consumer | REQUESTED | ACCEPTED | UIBE `PATCH .../accept` -> CM `PATCH .../accept` | Consumer user | UIBE offer ACCEPTED | none |
| E5 | Consumer | ACCEPTED | SIGNED | signing succeeded (D24) | system | own VC in wallet; `/signed/{nid}/true` | stub calls Provider `/issue` |
| E6 | Consumer | ACCEPTED | FAILED | signing or wallet failed | system | `/signed/{nid}/false`; reason `SIGNING_FAILED` or `WALLET_*` | Provider REQUESTED -> TERMINATED (P4) |
| E7 | Consumer | REQUESTED | REJECTED | UIBE `PATCH .../reject` -> CM | Consumer user | UIBE offer REJECTED; `/signed/{nid}/false` | Provider -> TERMINATED (P4) |
| E8 | Consumer | REQUESTED | TERMINATED | offer expiry (D44) | scheduler | UIBE offer TERMINATED; reason `OFFER_EXPIRED`; `/signed/{nid}/false` | Provider -> TERMINATED (P4) |
| E9 | Consumer | SIGNED | ACTIVE | `PUT /verifications/{nid}`, result SUCCESS, wallet ok | connector | Provider VC in wallet; `/isValid/{nid}/true` | Provider SIGNED -> ACTIVE (P3) |
| E10 | Consumer | SIGNED | FAILED | same, result REJECTED or wallet failed | connector | `/isValid/{nid}/false`; reason per D27 or `WALLET_*` | Provider SIGNED -> TERMINATED (P4) |
| E11 | Consumer | REQUESTED, ACCEPTED, SIGNED | FAILED | `PUT /contractOffers/{nid}/update` with `REJECTED` | connector | reason `PROVIDER_REJECTED` | (Provider already FAILED) |
| P1 | Provider | REQUESTED | SIGNED | `/issue`, result SUCCESS, issue and wallet ok | connector | `/isValid/{nid}/true`; both VCs in wallet; `/signed/{nid}/true` | stub calls Consumer `PUT /verifications` |
| P2 | Provider | REQUESTED | FAILED | `/issue`, result REJECTED; or issue or wallet failed after SUCCESS | connector | REJECTED: `/isValid/{nid}/false` and email D30. Issue or wallet: `/signed/{nid}/false` | Consumer SIGNED -> FAILED (E11) |
| P3 | Provider | SIGNED | ACTIVE | `PATCH .../status` `FINALIZED` | connector | none | (Consumer already ACTIVE) |
| P4 | Provider | REQUESTED, SIGNED | TERMINATED | `PATCH .../status` `TERMINATED` | connector | reason `COUNTERPARTY_TERMINATED` | none |

Technical validation errors and duplicates cause no transition (D28).

Lifecycle transitions. These are the same at both parties: the acting party changes its record, publishes a lifecycle message (Appendix D.1), and the other party applies the same transition on receipt.

| T | From | To | Action (`POST /contracts/{nid}/lifecycleActions`) | Who may act | Reason | Message type |
|---|---|---|---|---|---|---|
| L1 | ACTIVE | SUSPENDED | `SUSPEND` | Provider user | required | `SUSPENDED` |
| L2 | SUSPENDED | REACTIVATION_REQUESTED | `REQUEST_REACTIVATION` | Consumer user | required (justification) | `REACTIVATION_REQUESTED` |
| L3 | SUSPENDED, REACTIVATION_REQUESTED | ACTIVE | `REACTIVATE` | Provider user | optional | `REACTIVATED` |
| L4 | ACTIVE | REVOCATION_REQUESTED | `REQUEST_REVOCATION` | Provider or Consumer user; that party becomes `revocationRequestedBy` | required | `REVOCATION_REQUESTED` |
| L5 | REVOCATION_REQUESTED | REVOKED | `ACCEPT_REVOCATION` | user of the party that did not request | optional | `REVOCATION_ACCEPTED` |
| L6 | REVOCATION_REQUESTED | ACTIVE | `DENY_REVOCATION` | user of the party that did not request | required (justification) | `REVOCATION_DENIED` |
| L7 | ACTIVE, SUSPENDED, REACTIVATION_REQUESTED, REVOCATION_REQUESTED | TERMINATED | `TERMINATE` | Provider user | required (justification) | `TERMINATED` |

Any other combination of state, party and action is refused with 409 `invalidTransition` and changes nothing. Every transition (E, P, L, and the receiving side of L) writes one history entry and publishes one status event (Appendix D.2). Both parties show the same status within 10 seconds of a lifecycle action.

`allowedActions` in `GET /contracts/{nid}` lists the actions the table allows for this party in the current state: `ACCEPT` and `REJECT` (Consumer, REQUESTED), the L actions, and `UPDATE_CONSENT` (Provider, consent status not `NOT_DEFINED`, any open status). UIBE removes `UPDATE_CONSENT` for users without `consent_manager`.

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D38 | Business status versus OpenAPI technical status | `ConfirmationResponse` and `ContractAgreementData` `status`: `INITIATED`, `CREATED`, `FINALIZING`, `FINALIZED`, `TERMINATED`. | Records carry the D37 status. OpenAPI fields named `status` on CM's documented operations carry the technical status by this mapping: REQUESTED -> `CREATED`; ACCEPTED -> `INITIATED`; SIGNED -> `FINALIZING`; established -> `FINALIZED`; terminal -> `TERMINATED`. Trigger responses keep their documented fixed values (`INITIATED` for `/issue`, `PUT /verifications`, `POST` and `PATCH /contractOffers`, `PUT /contractOffers/.../update`; `CREATED` for `/create`). Added operations expose the D37 value as `status`. |
| D39 | Reasons and justifications | 31115 (reason), 31116 (justification), 31053 (reason), 31076 (justification for deny), 31118 (justification "related to the termination conditions"). | One field `reason` in the request. Required where D37 says so: non-blank after trimming, at most 1000 characters, else 400 `validationFailure`. Stored on the history entry and carried in the lifecycle message. No check against termination conditions (they are not machine readable in the input). |
| D40 | Wrong party, wrong state, missing role | Stories only show the positive path ("I can choose an action"). | Missing or invalid token: 401. Token without `contract_manager`: 403. Valid user, but the action is not allowed for this party or state: 409 `invalidTransition`. Two concurrent actions on one contract: one wins, the other gets 409 (optimistic locking on the record). |
| D41 | Who may answer a revocation request | 31053 and 31076 define requesting and responding Participant. | `revocationRequestedBy` (`PROVIDER` or `CONSUMER`) is stored on L4 at both parties and shown in details. Only the other party may use `ACCEPT_REVOCATION` or `DENY_REVOCATION`. It is cleared on L5 and L6. |
| D42 | From which states the Provider may terminate | 31118: "not revoked or terminated"; title says "active". | Any established status (L7). Not from establishment states, not from terminal states. |
| D43 | Contract consent status | 31047: Provider sees consent status, is redirected to the consent manager, sees the change afterwards; no action when consent is not defined. 32511: offer validation includes "resource consent status". No values, no source, no API. | Field `consentStatus` with values `NOT_DEFINED`, `PENDING`, `GRANTED`, `WITHDRAWN`. Initial value at record creation: `PENDING` if the catalogue asset has `consentRequired: true`, else `NOT_DEFINED`. The consent manager (here: the test, standing in for it) changes it through CM `PUT /contracts/{nid}/consentStatus` (Appendix A.5), Provider records only. UIBE details include `consentManagerUrl` = `CONSENT_MANAGER_URL` + `?contractNegotiationId=<nid>`; the UI shows it as the `UPDATE_CONSENT` link. Consent status does not block any transition in this run. |
| D44 | Expiry of unsigned offers | 35692: UIBE scheduler, daily at 01:00, timeout default 7 days from the UI database creation date, both configurable; marks the offer terminated in the UI database; sends a rejection to CM `/contractOffers/{nid}/reject`, which calls `/signed/{nid}/false`; terminated offer shown as Terminated; recent and finalised offers untouched. | UIBE settings `OFFER_EXPIRY_CRON` (default `0 0 1 * * *`, UTC) and `OFFER_EXPIRY_TIMEOUT` (ISO-8601 duration, default `P7D`). A run selects offers with status CREATED and `createdDate` < `asOf` - timeout. For each it calls CM `PATCH /contractOffers/{nid}/reject?reason=EXPIRED` (the optional query parameter is added to the OpenAPI operation); on 200 it sets the offer to TERMINATED; on any error it leaves the offer unchanged and retries on the next run. CM with `reason=EXPIRED` sets TERMINATED with reason `OFFER_EXPIRED` (not REJECTED) and calls `/signed/{nid}/false`. Trigger without waiting: `POST /admin/offerExpiry/run` (Appendix B.4). The order differs from the story (CM first, then the UI database) so that UI and CM never disagree. |
| D45 | Effect of a lifecycle status on resource access | 31115 "temporarily remove access", 31118 "remove access". Enforcement belongs to the connector. | Status change and events only. No call to the connector. Finding: the enforcement contract is not specified. |

### 4.4 Events between the parties

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D46 | Transport for lifecycle between parties | No published event contract. 21966 names a Kafka topic `notifications`. `flow.puml` and 22690 use Kafka between orchestrator and CM. No source says how a suspension reaches the other party. | Kafka (single broker, KRaft). Three topics, created at startup by CM if absent, 1 partition each in the reference environment: `simpl.contract.lifecycle.v1` (between parties), `simpl.contract.status.v1` (observability), `notifications` (to the Notification Service). JSON, UTF-8, no schema registry. The shared broker is a stand-in for the connector-mediated channel between two organisations. Finding: the real channel for lifecycle messages is not specified. |
| D47 | Lifecycle message | none | Appendix D.1. Key: nid. A receiving CM handles only messages whose `recipientParticipantId` equals its own id, ignores its own, is idempotent on `messageId`, and checks that `senderRole` may perform that transition (D37). It applies the transition with trigger `COUNTERPARTY`. It does not publish a lifecycle message in return. |
| D48 | Counterparty message that does not fit | Race conditions are not covered by any source. | If the transition is not allowed from the receiver's current state, or the nid is unknown, the receiver changes nothing and writes history entry `COUNTERPARTY_MESSAGE_REJECTED` (when the nid is known). |
| D49 | Status event | none | Appendix D.2. Published by CM for every transition, also on creation (`previousStatus: null`). Key: nid. |
| D50 | Ordering and delivery | none | Ordering is guaranteed per key (one partition). At-least-once delivery; consumers deduplicate on `messageId` or `eventId`. Consumer group per CM: `cm-<participantId>`, starting at the latest committed offset (`earliest` on first start). |
| D51 | How tests observe events | none | Tests connect to `localhost:19092`, subscribe to `simpl.contract.status.v1` or `notifications` before acting, and filter by key or `contractNegotiationId`. The same facts are also visible through `GET /contracts/{nid}` (history) for tests that do not use Kafka. |

### 4.5 Signer, wallet and credentials

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D52 | How the external trust services are stubbed | Signer, VC issuer, wallet appear in 11 story titles and BP07 dynamic view; no contract is in the input. 21928: no "unsigned VC" artefact. 19934 lists four internal steps (unsigned VC, sign, return, produce). | One stub service (Appendix C) holds connector, signer + VC issuer, verifier, wallet, catalogue and directory. One `issue` call returns a signed VC: the "unsigned VC" step is internal to the stub. CM never handles private keys. |
| D53 | VC format and proof | OpenAPI `VerifiableCredential`: required `@context`, `type`, `issuer`, `credentialSubject`; optional `validFrom`, `validUntil`, `proof`; example uses VC 1.1 context and `Ed25519Signature2020`. 19927: JSON-LD, W3C VC. | W3C VC Data Model 2.0 (`"@context": ["https://www.w3.org/ns/credentials/v2"]`, `validFrom`, `validUntil`), type `["VerifiableCredential", "UsageContractCredential"]`, `id` `urn:uuid:<uuid>`, embedded proof `DataIntegrityProof` with cryptosuite `eddsa-jcs-2022` (Ed25519 over JCS, no RDF canonicalisation). Algorithm and a worked vector in Appendix E. The OpenAPI schema accepts this shape. `Ed25519Signature2020` is not used because it needs RDF canonicalisation, which a test suite cannot produce simply. |
| D54 | Test keys, DIDs, trust | none | Three fixed Ed25519 keys, `did:key` identifiers (Appendix E.1). `provider-01` and `consumer-01` are trusted. `rogue-01` is a known DID that is not on the trust list. Keys live only in the stub. |
| D55 | Credential subject | 32511 response fields, `ContractAgreementData` fields. | `credentialSubject`: `id` (`urn:simpl:contract:<nid>`), `contractNegotiationId`, `contractAgreementId`, `contractDefinitionId`, `assetId`, `resourceDescriptionId`, `providerId`, `consumerId`, `usageContractHash`, `signerRole` (`CONSUMER` or `PROVIDER`), `signedAt`. All required. The Provider copies all fields except `signerRole` and `signedAt` from the Consumer VC. |
| D56 | What "incorrectly signed" means and how a test produces it | 19933 and 21948 list the failure kinds. | A test needs no crypto code. Valid VC for any participant: stub `POST /vc-issuer/v1/credentials/issue` (also for `rogue-01`, and with any `validFrom`/`validUntil`). Then: **invalid signature** = change any character in `credentialSubject` or `proofValue` after issuing; **untrusted** = issue as `rogue-01`; **revoked** = stub fault `VERIFIER:REVOKED` for the nid; **wrong issuer** = issue as the wrong trusted party (for example a "Provider" VC issued as `consumer-01`); **expired** = issue with `validUntil` in the past; **schema** = remove `credentialSubject.assetId`; **payload mismatch** = issue a correctly signed VC with another `assetId`; **missing** = stub override `notFound` on vcExchange; **technical** = stub fault `VERIFIER:FAIL`. Tests inject these at the Provider through `consumerVcOverride` and at the Consumer by calling `PUT /verifications/{nid}` directly or through `providerVcOverride`. |
| D57 | Wallet errors | 19927 and 19938: messages "Secure storage location unavailable", "Secure storage location is full", "Secure storage location is corrupted"; asked for another location. | Stub wallet answers 503 (unavailable), 507 (full), 500 (corrupted). CM maps any other error or timeout to unavailable. Reason codes `WALLET_UNAVAILABLE`, `WALLET_FULL`, `WALLET_CORRUPTED`; the history entry `reason` text is exactly the story message. The contract goes to FAILED (D24, D29, D31). Choosing another storage location is out. Finding. |
| D58 | What each wallet holds | 19927 and 19938: "fully-signed usage contract". BP07 dynamic view: one VC per party. | Each party's wallet ends with two VCs for an ACTIVE contract: its own and the counterparty's. `GET /verifications/{nid}/vc` returns only the own VC. |
| D59 | Signer and issuer failures | 19934 AC2: VC issuer or signature service error -> logged, 500. | The stub `issue` call fails with 500 under fault `SIGNER:FAIL`. Because signing runs after the HTTP response, CM reports it as FAILED with `SIGNING_FAILED`, not as an HTTP 500. |

### 4.6 Audit and data model

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D60 | Contract record at CM | `ContractAgreementData`, `ContractAgreementCreate`, 32511, 30959, 30960. | Fields: `contractNegotiationId` (unique per party), `contractAgreementId`, `contractDefinitionId`, `contractOfferId`, `assetId`, `resourceDescriptionId`, `resourceName`, `providerId`, `providerName`, `consumerId`, `consumerName`, `partyRole` (`PROVIDER` or `CONSUMER`, this party's role), `status`, `reasonCode`, `consentStatus`, `revocationRequestedBy`, `validationResult`, `validationReasonCode`, `validatedAt`, `humanReadable` and `usageContractHash` (Consumer; Provider gets the hash from the Consumer VC), `ownCredentialId`, `counterpartyCredentialId`, `consumerSignatureDate`, `providerSignatureDate`, `createdDate`, `lastUpdatedDate`, `version`. Schema and storage technology are free. Each CM has its own database. |
| D61 | Audit trail | 22690: decisions recorded immutably. 19933, 21948, 21966, 21963: traceable. | Append-only history per record: `at`, `fromStatus`, `toStatus` (equal for non-transition entries), `entryType` (`TRANSITION`, `VALIDATION`, `NOTIFICATION_SENT`, `NOTIFICATION_SKIPPED`, `NOTIFICATION_FAILED`, `COUNTERPARTY_MESSAGE_REJECTED`, `CONNECTOR_CALLBACK`), `trigger` (`USER`, `CONNECTOR`, `COUNTERPARTY`, `SCHEDULER`, `SYSTEM`), `actor` (username or `null`), `reasonCode`, `reason`, `correlationId`. No API changes or deletes entries. Shown oldest first in `GET /contracts/{nid}`. |
| D62 | Aggregated audit data | 34633: counts of usage contracts per status at a given moment, through an endpoint for logging and monitoring, according to an audit rule package. | `GET /audit/contractStatusCounts` on CM (Appendix A.6): every D37 status with its count (zeros included), `total`, `generatedAt`, `participantId`. API key. The audit rule package is out. Finding: package format and delivery are not specified. |
| D63 | Contract clause document upload | 28846: Provider side upload, SHA-256 of exact bytes, metadata returned, no content returned, re-upload never overwrites, formats and limits "defined by the upload API". No API in the input. | CM `POST /contractClauseDocuments`, `multipart/form-data` field `file` (Appendix A.7). Allowed media types: `application/pdf`, `text/plain`, `text/markdown`; others 415 `invalidDocument`. Max size `CLAUSE_MAX_BYTES` (default 5 MiB), larger 413. Empty or missing file 400. Response 201 metadata only. Every upload gets a new `documentId`, even for identical content (the allowed reuse is not used). No download endpoint. Runs at any party. |
| D64 | "Traceable in backend logs" | Many stories. | Review only. Tests use the history (D61) and events instead of logs. CM logs one line per transition with nid and correlation id. |

### 4.7 Access and identity

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D65 | CM authentication | CM OpenAPI: global `ApiKeyAuth`, header `X-Api-Key`; 403 documented only on the `contract-ui` tagged operations. Stories: 401 for an unauthenticated connector. | Every CM operation except `/health` needs `X-Api-Key` equal to the CM's configured key; missing or wrong: 401. One key per CM, shared by its callers (connector stub, UIBE, tests). CM never returns 403. |
| D66 | UIBE authentication | UIBE OpenAPI: only `ApiKeyAuth`, header `x-api-key`. UI stories need an authenticated user with a role. | Machine operations (`POST /contractOffers/{nid}`, `POST /admin/offerExpiry/run`) need `x-api-key` equal to the UIBE key; missing or wrong: 401. User operations (`GET /contractOffers`, `GET /contractOffers/{nid}`, `PATCH /contractOffers/{nid}/{op}`, `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions`) need `Authorization: Bearer <JWT>` from this party's realm; the API key alone gives 401 there. Finding: the OpenAPI does not declare the user scheme. |
| D67 | Identity provider contract | none | Keycloak, realms `provider` and `consumer` (one per party). In each realm: public client `contract-ui`, standard flow with PKCE and direct access grants enabled, redirect URIs `http://localhost:19103/*` (provider) or `http://localhost:19203/*` (consumer), web origins `+`. Realm roles `contract_manager`, `consent_manager`, in the access token at `realm_access.roles`. Claim `participant_id` (hardcoded claim mapper on the client, value `provider-01` or `consumer-01`). `preferred_username` is the actor in history. Issuer `http://localhost:19080/realms/<realm>` for tokens fetched from the host and from containers (Keycloak `KC_HOSTNAME=http://localhost:19080`, `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`). UIBE fetches keys from `http://keycloak:8080/realms/<realm>/protocol/openid-connect/certs`. Users, all enabled, email verified, no required actions: realm `provider`: `provider-admin` / `ProviderAdmin1!` (`contract_manager`, `consent_manager`), `provider-operator` / `ProviderOperator1!` (`contract_manager`), `provider-norole` / `ProviderNoRole1!` (none). Realm `consumer`: `consumer-admin` / `ConsumerAdmin1!` (`contract_manager`), `consumer-norole` / `ConsumerNoRole1!` (none). Emails `<username>@example.test`. Tests get tokens with the password grant at `http://localhost:19080/realms/<realm>/protocol/openid-connect/token`. Keycloak admin: `admin` / `admin`. |
| D68 | Roles per operation | Stories: "a role authorising the management of usage contracts" (30959 and others), "... and consent" (31047), "contract management" (32505). | UIBE user operations: `contract_manager`. `UPDATE_CONSENT` link: `contract_manager` and `consent_manager`. Missing role: 403. CM and UIBE machine operations: API key only. |
| D69 | Token from the other party | none | UIBE accepts only tokens with its own realm issuer (else 401) and with `participant_id` equal to its own participant id (else 403). |
| D70 | Connector callback authentication | 35858, 35863, 35888: 401 when the caller is not authenticated. These are connector endpoints. | The stub requires `X-Api-Key: test-edc-key` on `/edc/...`, else 401. CM sends it on every callback. The 401 behaviour itself is the connector's, not tested against CM. |

### 4.8 UI

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D71 | Screens | 30959 (overview, filters, empty table), 30960 (details, back), 32505 (accept), 31047, 31053, 31076, 31115 to 31118 (actions with reason), 35692 AC4 (Terminated). | One UI image, run once per party, configured with its UIBE URL and realm. OIDC login with Keycloak (code flow, PKCE). Routes: `/` overview, `/contracts/<nid>` details, `/contractOffers/<nid>` (the `offerUrl`) shows the same details. Overview columns: negotiation id, resource description id, resource name, counterparty name (Provider name at the Consumer, Consumer name at the Provider), status label. Filters: negotiation id (exact), resource name (contains, case-insensitive), counterparty name (contains, case-insensitive), status (one value). Details: the overview fields plus both party names, consent status (Provider), human-readable offer (Consumer), history, and one button per allowed action. An action that needs a reason opens a dialog; submit is refused in the UI while the reason is blank. Technology is free. |
| D72 | Test hooks | none | `data-testid` values: `contracts-table`, `contract-row-<nid>`, `cell-negotiation-id`, `cell-resource-description-id`, `cell-resource-name`, `cell-counterparty-name`, `cell-status`, `contracts-empty`, `filter-negotiation-id`, `filter-resource-name`, `filter-counterparty-name`, `filter-status`, `filter-apply`, `open-details-<nid>`, `contract-details`, `detail-negotiation-id`, `detail-resource-description-id`, `detail-resource-name`, `detail-provider-name`, `detail-consumer-name`, `detail-status`, `detail-consent-status`, `human-readable`, `back-to-overview`, `action-accept`, `action-reject`, `action-suspend`, `action-request-reactivation`, `action-reactivate`, `action-request-revocation`, `action-accept-revocation`, `action-deny-revocation`, `action-terminate`, `action-update-consent`, `reason-input`, `reason-submit`, `reason-cancel`, `error-message`, `access-denied`. Status cells show the D37 label text. The empty overview still renders the table header row (30959 AC2). |
| D73 | Buttons for actions that are not allowed | 32505 AC3, 31118 AC1, 31047 AC4: "cannot click", "cannot choose". | Not rendered (absent from the DOM), driven by `allowedActions`. |
| D74 | User without the role | 30959 AC3: "cannot open the contract manager UI". | After login, a user without `contract_manager` sees only `access-denied`. |
| D75 | Rendering the human-readable HTML | 30960 shows it. It comes from another party's resource description. | Rendered after sanitising (no scripts, no event handlers, no external resources). Finding: the format of the human-readable contract is not specified. |

### 4.9 Environment and testability

| ID | Topic | Sources | Decision |
|---|---|---|---|
| D76 | Topology, names, ports | OpenAPI servers use 8085 and 8086 (8086 is reserved by the local upstream stack on the host). | Compose project `cm-factory`. Services, all apps listening on 8080 inside the network, plain HTTP: `postgres` (host 19432), `kafka` (host 19092, internal `kafka:9092`, advertised `EXTERNAL://localhost:19092`), `keycloak` (host 19080), `stubs` (host 19090), `cm-provider` (19101), `uibe-provider` (19102), `ui-provider` (19103), `cm-consumer` (19201), `uibe-consumer` (19202), `ui-consumer` (19203). Base URLs from the host: `http://localhost:19101/contract/v1`, `http://localhost:19102/contract-ui/v1`, and the same pattern for the Consumer. Postgres 16, user `simpl`, password `simpl`, databases `cm_provider`, `cm_consumer`, `uibe_provider`, `uibe_consumer`. |
| D77 | Keys and configuration | none | API keys: `cm-provider` `test-cm-provider-key`, `cm-consumer` `test-cm-consumer-key`, `uibe-provider` `test-uibe-provider-key`, `uibe-consumer` `test-uibe-consumer-key`, stub connector `test-edc-key`. CM settings: `CM_PARTICIPANT_ID`, `CM_API_KEY`, `STUB_BASE_URL` (`http://stubs:8080`), `STUB_EDC_API_KEY`, `UIBE_BASE_URL`, `UIBE_API_KEY`, `KAFKA_BOOTSTRAP` (`kafka:9092`), `UI_PUBLIC_URL` (`http://localhost:19103` or `19203`, used for `offerUrl` = `<UI_PUBLIC_URL>/contractOffers/<nid>`), `CONTRACT_VC_VALIDITY`, `CLAUSE_MAX_BYTES`. UIBE settings: `UIBE_PARTICIPANT_ID`, `UIBE_API_KEY`, `CM_BASE_URL`, `CM_API_KEY`, `OIDC_ISSUER`, `OIDC_JWKS_URI`, `OFFER_EXPIRY_CRON`, `OFFER_EXPIRY_TIMEOUT`, `CONSENT_MANAGER_URL` (`http://localhost:19090/consent`). |
| D78 | Who builds stubs and realm files | none | The code writer builds the stub service, the Keycloak realm imports and the compose file exactly to this digest, in the build's environment folder. The test writer uses only the documented stub API, never stub internals. |
| D79 | Asynchronous timing | 19933 AC10: verify through state. | Every asynchronous step completes within 10 seconds in the reference environment. Tests poll every 250 ms with a 20 second limit. |
| D80 | Fault scoping | none | Every CM call to the stub carries header `X-Contract-Negotiation-Id: <nid>`. Stub faults and overrides are registered per nid, so parallel tests do not affect each other. |
| D81 | Test data | none | Each test registers its own asset (`asset-<uuid>`) through `POST /_control/catalogue/assets` and lets the stub generate nid and aid (or passes its own UUIDs). Nothing can be deleted: records, history, wallet content, Kafka messages and Keycloak sessions stay until `docker compose -p cm-factory down -v`. Tests must not assume an empty database: overview tests filter by their own nid or resource name; audit count tests compare before and after. |
| D82 | Time-based rules without waiting | 35692 (7 days). | `POST /admin/offerExpiry/run` with `asOf` and `contractNegotiationIds` (Appendix B.4). Tests pass `asOf` = now + 7 days + 1 minute and their own nid. No clock manipulation of containers. |
| D83 | Seed data | none | The stub starts with the directory of Appendix C.5 and one asset `asset-seed-01` (provider `provider-01`, `consentRequired: true`) for UI smoke tests. No contracts are seeded. |

---

## 5. Open questions for the spec owners

Ordered by impact on buildability.

1. **Event and callback contract.** Which messages pass between CM and the connector (names, payloads, methods of `/isPresent`, `/isValid`, `/signed`, `/vcExchange`), and how a lifecycle change (suspend, reactivate, revoke, terminate) reaches the other party. No AsyncAPI or topic list is published (D3, D46, D47).
2. **Signer, VC issuer and wallet interfaces and the contract VC format.** Proof type, context version (the OpenAPI example uses VC 1.1 with `Ed25519Signature2020`), credential subject fields, wallet error model (D52 to D58).
3. **The usage contract state machine.** One status vocabulary for CM, UIBE and UI, with the allowed transitions and who may trigger them. Today there are three partial enums (D10, D37, D38).
4. **Lifecycle API.** Suspend, reactivate, revoke, terminate and consent have UI stories but no API in either OpenAPI (D6, D7, D43).
5. **Which party runs `/create`, `/issue`, `/verifications`.** The R17 stories move `/create` to the Consumer, rename `/issue` to `validateAndIssue`, and change methods and codes. Please align the OpenAPI with the target flow, or the stories with the OpenAPI (D4, D20, D25, D31).
6. **User authentication on UIBE.** The OpenAPI declares only an API key; the stories need users and roles. Please name the roles and the token claims (D66 to D68).
7. **Existence check.** Which party checks (BP07.03 and 32509 say Provider, 35858 says Consumer), which statuses count as "existing", and what the 200 body contains when nothing is found (D19).
8. **Consumer verification input.** Does the Provider VC arrive in the `PUT /verifications` body (OpenAPI) or does CM fetch it through `/vcExchange` (35677) (D31)?
9. **Offer field names in UIBE.** Schema (`humanReadable`, `hash`) and examples (`humanReadableData`, `hashData`) differ; `GET /contractOffers` is a single object in the schema and a list in the example (D8, D9).
10. **Contract term and expiry**, and whether "usage contract expiration" (BP07) changes the status automatically (D35).
11. **Notification Service AsyncAPI** for `EmailNotification` (field types of `to` and `cc`), and the source of the Consumer user's email (D22, D30).
12. **Consent status** values and the consent manager integration (D43).
13. **Human-readable contract format** and hash algorithm (the example looks like MD5; 28846 uses SHA-256) (D21).
14. **Upload API for contract clause documents**: path, formats, limits, document reference format (D63).
15. **Audit rule package** for 34633 (D62).
16. **Wallet failure recovery**: what "specify another secure location" means for an automatic backend flow (D57).
17. **Mapping between DSP process ids and `contractNegotiationId`** (D18).

---

## 6. Next steps

### Test writer

Read: this digest in full, then `specs/api/contract-manager-be/openapi3-v1.yaml`, `specs/api/contract-ui-be/openapi3-v1.yaml`, and the 28 in-scope stories. Write API tests against CM and UIBE first (one per acceptance criterion, plus one per row of the D37 tables), then event tests on Kafka, then a small UI suite with the D72 hooks.

### Code writer

Read: the same files. Build CM, UIBE, UI, the stub service, the realm imports and the compose file of D76 to D78. Keep the stub minimal and exactly to Appendix C.

### Where the two are most likely to diverge

1. **Status vocabulary**: D37 status on added operations versus the technical status (D38) on OpenAPI operations. Offer status (D10) is a third, separate field.
2. **UIBE field names** (D9): request uses `humanReadable`/`hash`, responses use `humanReadableData`/`hashData`.
3. **Status codes taken from the OpenAPI, not the stories**: 200 (not 201) on `PATCH /contractOffers`, 200 (not 202) on `PUT /verifications`, 400 (not 404) for unknown ids on operations without a documented 404 (D12), 409 only on added operations.
4. **Asynchrony**: `/issue`, `PATCH /contractOffers/{nid}/accept` and `PUT /verifications` return before validation or signing ends. Tests must poll; code must not block.
5. **Duplicate versus retry** (D28): duplicates only after `SUCCESS` or `REJECTED`.
6. **Validation check order** (D27): the first failing check decides the single reason code.
7. **Where lifecycle actions are allowed** (D37 L table, D40): 409 versus 403 versus 401.
8. **Expiry trigger** (D44, B.4): CM first, UIBE status second; `reason=EXPIRED` gives TERMINATED, not REJECTED.
9. **Stub control API** (Appendix C.6): exact field names, the per-nid fault scoping, and `stopAfter`.
10. **Keycloak issuer** (D67): the issuer must be the `localhost:19080` URL inside containers too.

---

## 7. Clarifications after run 1

Added on 29 Oct 2026, after the suite ran on build run 1. Each row closes a point where the test writer and the code writer read the digest differently, or where the build deviated. They take precedence over the rows they name.

| ID | Rows | Clarification |
|---|---|---|
| C1 | D27, D28 | A duplicate trigger does not change the stored `validationResult`, `validationReasonCode` or `validatedAt`: they keep the last real result (`SUCCESS`, `REJECTED` or `FAILED`). `IGNORED` and `DUPLICATE_OR_REPLAYED_SUBMISSION` appear only in the `VALIDATION` history entry of the duplicate. |
| C2 | D60, A.3 | `GET /contracts/{nid}` also returns `ownCredentialId` and `counterpartyCredentialId` (`null` until set). |
| C3 | D61 | A `CONNECTOR_CALLBACK` entry is written for every callback to the connector, after the callback has been made, so it may appear a moment after the HTTP response that caused it. Tests check the entries they name and do not count all entries. |
| C4 | D37 P1 | The Provider move REQUESTED to SIGNED has `trigger` `CONNECTOR`, as the P1 row says (it is caused by the connector's `/issue` call), also though the work runs asynchronously. The same holds for every transition the table attributes to the connector. |
| C5 | D24, D71, 32505 | After the accept, the page shows the current status of the contract. Signing starts at once (D24), so "Accepted" or a later establishment status ("Signed", "Active") all satisfy 32505 AC2. |
| C6 | D71, 30959 | Overview column labels use the story wording: "Contract negotiation ID", "Resource description ID", "Resource name", the counterparty column as "Provider name" at the Consumer and "Consumer name" at the Provider, and "Status". |

## Appendix A. CM operations added in this run

All under `/contract/v1`, `X-Api-Key` required, JSON, errors per D11.

### A.1 `GET /verifications/{nid}/vc`

200: this party's own signed VC for the nid (D53), `application/json`. 404 if the record does not exist or the own VC is not yet issued. 401.

### A.2 `GET /contracts`

Query (all optional): `contractNegotiationId` (exact), `resourceName` (contains, case-insensitive), `providerName` (contains, case-insensitive), `consumerName` (contains, case-insensitive), `status` (one or more D37 values, comma separated; unknown value: 400). Sorted by `createdDate` descending. No paging.

200:

```json
{
  "items": [
    {
      "contractNegotiationId": "9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
      "contractAgreementId": "677186f6-1179-4cf1-b845-68d1c00c185f",
      "contractDefinitionId": "def-1a2b3c4d",
      "assetId": "asset-1a2b3c4d",
      "resourceDescriptionId": "rd-1a2b3c4d",
      "resourceName": "Air quality 2026",
      "providerId": "provider-01",
      "providerName": "Provider One",
      "consumerId": "consumer-01",
      "consumerName": "Consumer One",
      "partyRole": "CONSUMER",
      "status": "REQUESTED",
      "reasonCode": null,
      "consentStatus": "NOT_DEFINED",
      "createdDate": "2026-10-29T10:00:00Z",
      "lastUpdatedDate": "2026-10-29T10:00:00Z"
    }
  ]
}
```

### A.3 `GET /contracts/{nid}`

200: the A.2 item plus `contractOfferId`, `revocationRequestedBy`, `validationResult`, `validationReasonCode`, `validatedAt`, `consumerSignatureDate`, `providerSignatureDate`, `humanReadable` (Consumer, else `null`), `usageContractHash`, `allowedActions` (array of D37 action names), `history` (array of D61 entries, oldest first). 404.

### A.4 `POST /contracts/{nid}/lifecycleActions`

Body: `{"action": "SUSPEND", "reason": "Breach of usage policy", "actor": "provider-admin"}`. `action` one of `SUSPEND`, `REQUEST_REACTIVATION`, `REACTIVATE`, `REQUEST_REVOCATION`, `ACCEPT_REVOCATION`, `DENY_REVOCATION`, `TERMINATE`. `actor` is set by UIBE from the token (optional when tests call CM directly). 200: the A.3 body after the change. 400 (bad action, missing required reason), 404, 409 `invalidTransition`.

### A.5 `PUT /contracts/{nid}/consentStatus`

Body: `{"consentStatus": "GRANTED"}` (`PENDING`, `GRANTED`, `WITHDRAWN`). 200: the A.3 body. 404. 409 if the record is not a Provider record, is terminal, or has `NOT_DEFINED`. Writes a history entry and a status event (with unchanged status). No lifecycle message.

### A.6 `GET /audit/contractStatusCounts`

200:

```json
{
  "participantId": "provider-01",
  "generatedAt": "2026-10-29T10:00:00Z",
  "total": 3,
  "counts": {
    "REQUESTED": 1, "ACCEPTED": 0, "REJECTED": 0, "SIGNED": 0, "ACTIVE": 2, "FAILED": 0,
    "SUSPENDED": 0, "REACTIVATION_REQUESTED": 0, "REVOCATION_REQUESTED": 0, "REVOKED": 0, "TERMINATED": 0
  }
}
```

### A.7 `POST /contractClauseDocuments`

`multipart/form-data`, part `file` with filename and content type. 201:

```json
{
  "documentId": "3f2a6c1e-8d7b-4a55-9b0e-2c4f1a7d9e10",
  "documentName": "clauses.pdf",
  "mediaType": "application/pdf",
  "size": 18432,
  "hash": "<lowercase hex sha-256 of the exact bytes>",
  "hashAlgorithm": "SHA-256",
  "sourceReference": "urn:simpl:contract-clause:3f2a6c1e-8d7b-4a55-9b0e-2c4f1a7d9e10"
}
```

400 (no file or empty), 413 (too large), 415 (media type not allowed). The response never contains the document content.

## Appendix B. UIBE operations added or refined in this run

All under `/contract-ui/v1`.

### B.1 `GET /contracts`

Bearer token, `contract_manager`. Same query and body as A.2 (UIBE passes it to CM), plus a query `counterpartyName` that UIBE maps to `providerName` (Consumer party) or `consumerName` (Provider party).

### B.2 `GET /contracts/{nid}`

Bearer token, `contract_manager`. The A.3 body, plus `offerStatus`, `humanReadableData`, `hashData` from the UIBE offer table (all `null` if there is no offer row), plus `consentManagerUrl` when `UPDATE_CONSENT` is in `allowedActions` after the role filter (D43).

### B.3 `POST /contracts/{nid}/lifecycleActions`

Bearer token, `contract_manager`. Body `{action, reason}`. UIBE sets `actor` from `preferred_username` and calls A.4. Codes as A.4, plus 401 and 403.

### B.4 `POST /admin/offerExpiry/run`

`x-api-key`. Body (all optional): `{"asOf": "2026-11-05T10:01:00Z", "contractNegotiationIds": ["9e82cdd6-..."]}`. `asOf` defaults to now. Without `contractNegotiationIds` all offers are considered. Runs D44 once, synchronously. 200:

```json
{ "expired": ["9e82cdd6-..."], "skipped": [], "failed": [] }
```

`skipped`: selected ids that were not CREATED or not old enough. `failed`: ids where the CM call failed.

### B.5 Refinements of OpenAPI operations

- `POST /contractOffers/{nid}` (API key): creates the offer row with status CREATED, or replaces `humanReadable` and `hash` of an existing row without changing its status. 201 `{offerStatus, offerUrl}`.
- `GET /contractOffers`, `GET /contractOffers/{nid}`, `PATCH /contractOffers/{nid}/{op}`: bearer token, `contract_manager`; field names per D9; `PATCH` behaviour per D23.

## Appendix C. Stub contract

One service `stubs`, `http://stubs:8080` inside the network, `http://localhost:19090` from the host. It keeps state in memory. It reads the header `X-Contract-Negotiation-Id` to apply per-nid faults and overrides. It records every inbound call.

### C.1 Connector endpoints (called by CM)

Header `X-Api-Key: test-edc-key` required, else 401. `{p}` is the calling CM's participant id.

| Method and path | Response | Meaning |
|---|---|---|
| `POST /edc/{p}/isPresent/{nid}/{true or false}` | 204 | existence result (35858) |
| `POST /edc/{p}/isValid/{nid}/{true or false}` | 204 | validation result (35863) |
| `POST /edc/{p}/signed/{nid}/{true or false}` | 204 | signing result or Consumer decline (35660) |
| `GET /edc/{p}/vcExchange/{nid}/request` | 200 VC, or 404 | counterparty VC (35888): the stub calls the counterparty CM `GET /verifications/{nid}/vc`, unless an override is set |

### C.2 Negotiation choreography (stub acting as both connectors)

State values: `STARTED`, `EXISTENCE_CHECKED`, `OFFERED`, `CONSUMER_SIGNED`, `PROVIDER_SIGNED`, `CONSUMER_VERIFIED`, `FINALIZED`, `TERMINATED`.

1. `STARTED`: call Provider CM `POST /agreements/{nid}/search` with `{contractAgreementId, contractDefinitionId, assetId, providerId, consumerId}`. Wait for `/isPresent`. `true`: `TERMINATED` (reason `EXISTING_CONTRACT`). `false`: `EXISTENCE_CHECKED`.
2. Call Consumer CM `POST /credentials/agreements/{aid}/definitions/{did}/create` with `{contractNegotiationId, assetId, providerId, consumerId, contractOfferId}`. 201: `OFFERED`. Other: `TERMINATED` (`OFFER_FAILED`), and Provider CM `PATCH .../status` `TERMINATED`.
3. Wait (no time limit, a user decides) for Consumer `/signed`. `false`: Provider CM `PATCH /agreements/{aid}/definitions/{did}/status` `{"status":"TERMINATED"}`, then `TERMINATED` (`CONSUMER_DECLINED`). `true`: `CONSUMER_SIGNED`.
4. Call Provider CM `POST .../issue` with the step 2 body. Answer the Provider's `/vcExchange` call. Wait for the Provider's outcome: `/isValid false` or `/signed false`: Consumer CM `PUT /contractOffers/{nid}/update` `{"status":"REJECTED","additionalInfo":"Provider validation or signing failed"}`, then `TERMINATED` (`PROVIDER_REJECTED`). `/signed true`: `PROVIDER_SIGNED`.
5. Get the Provider VC from Provider CM `GET /verifications/{nid}/vc` (or `providerVcOverride`), send it to Consumer CM `PUT /verifications/{nid}`. Wait for Consumer `/isValid`. `false`: Provider CM `PATCH .../status` `TERMINATED`, then `TERMINATED` (`CONSUMER_REJECTED`). `true`: `CONSUMER_VERIFIED`.
6. Provider CM `PATCH .../status` `{"status":"FINALIZED"}`. Then `FINALIZED`.

Every wait except step 3 ends after 30 seconds with `TERMINATED` (`TIMEOUT`) and no further calls. Stub calls to a CM use that CM's API key (D77) and `X-Correlation-Id` = nid.

### C.3 Signer and VC issuer, verifier

- `POST /vc-issuer/v1/credentials/issue`, body `{"participantId": "consumer-01", "credentialSubject": {...}, "validFrom": "...", "validUntil": "..."}` (dates optional: now and now + 365 days). 201: VC per D53, `issuer` = the participant's DID, signed with its key (Appendix E). Fault `SIGNER:FAIL`: 500. Unknown participant: 400.
- `POST /vc-issuer/v1/credentials/verify`, body `{"credential": {...}}`. 200 `{"verified": true, "issuerParticipantId": "consumer-01", "problems": []}`. Problems, in this order of checking, first one only: `MALFORMED` (missing `@context`, `type`, `issuer`, `credentialSubject`, `proof`, or proof `type`/`cryptosuite` not as D53, or `verificationMethod` DID differs from `issuer`), `INVALID_SIGNATURE`, `UNKNOWN_ISSUER` (not `provider-01` or `consumer-01`), `REVOKED_ISSUER` (fault `VERIFIER:REVOKED`). `verified` is true only with no problems. Fault `VERIFIER:FAIL`: 500. The verifier does not check dates or subject content; CM does.

### C.4 Wallet

- `PUT /wallet/v1/{p}/credentials/{credentialId}` (URL-encoded VC `id`), body the VC. 201. Faults `WALLET:UNAVAILABLE` 503, `WALLET:FULL` 507, `WALLET:CORRUPTED` 500.
- `GET /wallet/v1/{p}/credentials?contractNegotiationId=<nid>`: 200 array of stored VCs whose subject has that nid. No auth (tests read it too).

### C.5 Catalogue and directory

- `GET /catalogue/v1/assets/{assetId}`: 200 asset or 404. Asset: `{"assetId", "resourceDescriptionId", "resourceName", "description", "providerId", "providerName", "consentRequired", "policies": [{"type": "ACCESS|USAGE", "description"}], "contractClauses": [{"documentName", "hash"}]}`.
- `GET /directory/v1/participants/{p}`: 200 `{"participantId", "name", "did", "contactEmail"}` or 404. Fault `DIRECTORY:NO_CONTACT`: `contactEmail` is `null`.
- Seeded directory: `provider-01` "Provider One", `provider-01@example.test`; `consumer-01` "Consumer One", `consumer-01@example.test`; `rogue-01` "Rogue Participant", no email. DIDs per Appendix E.1.
- `GET /consent?contractNegotiationId=<nid>`: a plain HTML page "Consent manager stub" (target of the UI link). It does not change anything.

### C.6 Control endpoints (tests only, no auth)

| Method and path | Body or query | Response |
|---|---|---|
| `POST /_control/catalogue/assets` | asset (C.5); `resourceDescriptionId` defaults to `rd-<assetId>`, `consentRequired` to false | 201 asset |
| `POST /_control/negotiations` | `{contractNegotiationId?, contractAgreementId?, contractDefinitionId?, contractOfferId?, providerId, consumerId, assetId, stopAfter?, faults?: [{service, mode}], consumerVcOverride?, providerVcOverride?}`. Ids default to UUIDs (`def-<8 hex>`, `offer-<8 hex>` for definition and offer). `stopAfter`: one of the C.2 states. An override is a VC object, or `{"notFound": true}`. | 201 `{contractNegotiationId, contractAgreementId, contractDefinitionId, contractOfferId, state}`; starts C.2 in the background |
| `GET /_control/negotiations/{nid}` | | 200 `{contractNegotiationId, state, terminationReason, steps: [{state, at}], callbacks: [{participantId, endpoint, value, at}]}` (`endpoint`: `isPresent`, `isValid`, `signed`, `vcExchange`) |
| `POST /_control/negotiations/{nid}/advance` | | 202; continues after a `stopAfter` |
| `POST /_control/faults` | `{contractNegotiationId, service, mode}`; `service:mode` one of `SIGNER:FAIL`, `VERIFIER:FAIL`, `VERIFIER:REVOKED`, `WALLET:UNAVAILABLE`, `WALLET:FULL`, `WALLET:CORRUPTED`, `DIRECTORY:NO_CONTACT`. A fault applies to every matching call for that nid, from any party, until removed. | 201 |
| `DELETE /_control/faults?contractNegotiationId=<nid>` | | 204 |
| `PUT /_control/negotiations/{nid}/overrides` | `{consumerVcOverride?, providerVcOverride?}` | 200 |
| `GET /_control/calls?contractNegotiationId=<nid>` | | 200 array `{at, method, path, status, body}` of inbound calls |

A test that drives CM directly (without the choreography) registers the nid's faults first, then calls CM with that nid. Callbacks are recorded even for nids the stub did not start.

## Appendix D. Kafka messages

All JSON, UTF-8, key = nid, header `correlationId`.

### D.1 `simpl.contract.lifecycle.v1`

```json
{
  "messageId": "6a1f0c3e-2b4d-4e8f-9a7b-1c2d3e4f5a6b",
  "messageType": "SUSPENDED",
  "contractNegotiationId": "9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
  "contractAgreementId": "677186f6-1179-4cf1-b845-68d1c00c185f",
  "senderParticipantId": "provider-01",
  "senderRole": "PROVIDER",
  "recipientParticipantId": "consumer-01",
  "reason": "Breach of usage policy",
  "actor": "provider-admin",
  "occurredAt": "2026-10-29T10:00:00Z"
}
```

`messageType`: `SUSPENDED`, `REACTIVATION_REQUESTED`, `REACTIVATED`, `REVOCATION_REQUESTED`, `REVOCATION_ACCEPTED`, `REVOCATION_DENIED`, `TERMINATED`.

### D.2 `simpl.contract.status.v1`

```json
{
  "eventId": "0b9c8d7e-6f5a-4b3c-2d1e-0f9a8b7c6d5e",
  "participantId": "consumer-01",
  "partyRole": "CONSUMER",
  "contractNegotiationId": "9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
  "contractAgreementId": "677186f6-1179-4cf1-b845-68d1c00c185f",
  "previousStatus": "ACTIVE",
  "newStatus": "SUSPENDED",
  "reasonCode": null,
  "reason": "Breach of usage policy",
  "trigger": "COUNTERPARTY",
  "actor": null,
  "occurredAt": "2026-10-29T10:00:01Z"
}
```

### D.3 `notifications`

```json
{
  "channel": "EMAIL",
  "to": ["consumer-01@example.test"],
  "cc": [],
  "subject": "Usage contract offer could not be validated: 9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
  "message": "The signed usage contract offer 9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5 could not be validated. Reason: INVALID_CONSUMER_SIGNATURE."
}
```

Only the five fields named in 21966. Technical context goes in headers (`correlationId`, `contractNegotiationId`).

## Appendix E. Test keys and the proof algorithm

### E.1 Keys

Ed25519 private keys given as 32-byte seeds (hex). Public key multibase = `z` + base58btc(`0xed 0x01` + 32-byte public key). DID = `did:key:` + multibase. Verification method = DID + `#` + multibase.

| Participant | Seed (hex) | Public key (hex) | DID |
|---|---|---|---|
| `provider-01` | `11` repeated 32 times | `d04ab232742bb4ab3a1368bd4615e4e6d0224ab71a016baf8520a332c9778737` | `did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S` |
| `consumer-01` | `22` repeated 32 times | `a09aa5f47a6759802ff955f8dc2d2a14a5c99d23be97f864127ff9383455a4f0` | `did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK` |
| `rogue-01` | `33` repeated 32 times | `17cb79fb2b4120f2b1ec65e4198d6e08b28e813feb01e4a400839b85e18080ce` | `did:key:z6Mkg49NtQR2LyYRDCQFK4w1VVHqhypZSSRo7HsyuN7SV7v5` |

These keys are for the test environment only.

### E.2 `eddsa-jcs-2022` as used here

1. `document` = the VC without `proof`.
2. `proofConfig` = `{"type": "DataIntegrityProof", "cryptosuite": "eddsa-jcs-2022", "created": <RFC 3339>, "verificationMethod": <DID#multibase>, "proofPurpose": "assertionMethod", "@context": document["@context"]}`.
3. `hashData` = SHA-256(JCS(`proofConfig`)) followed by SHA-256(JCS(`document`)) (64 bytes).
4. `signature` = Ed25519 sign(`hashData`).
5. `proof` = `proofConfig` without `@context`, plus `"proofValue": "z" + base58btc(signature)`.
6. Verify: remove `proof`, rebuild `proofConfig` from it (without `proofValue`, with the document `@context`), recompute `hashData`, check the signature with the key decoded from `verificationMethod`.

JCS (RFC 8785) for these documents (strings and arrays only, no numbers) equals JSON with keys sorted by code point, no whitespace, UTF-8 without escaping non-ASCII.

### E.3 Worked vector

Signed with the `provider-01` key. `usageContractHash` is SHA-256 of the UTF-8 string `<p>x</p>`. A correct implementation reproduces `proofValue` exactly (Ed25519 is deterministic).

```json
{
  "@context": ["https://www.w3.org/ns/credentials/v2"],
  "id": "urn:uuid:00000000-0000-4000-8000-000000000001",
  "type": ["VerifiableCredential", "UsageContractCredential"],
  "issuer": "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S",
  "validFrom": "2026-10-29T10:00:00Z",
  "validUntil": "2027-10-29T10:00:00Z",
  "credentialSubject": {
    "id": "urn:simpl:contract:9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
    "contractNegotiationId": "9e82cdd6-4aba-4a77-8dcc-f1a9f56d82c5",
    "contractAgreementId": "677186f6-1179-4cf1-b845-68d1c00c185f",
    "contractDefinitionId": "def-1",
    "assetId": "asset-1",
    "resourceDescriptionId": "rd-1",
    "providerId": "provider-01",
    "consumerId": "consumer-01",
    "usageContractHash": "31d8e07ec305ac4e2515d1f0b1c8c603b3858044eb7d97807d6d0970838cb0be",
    "signerRole": "PROVIDER",
    "signedAt": "2026-10-29T10:00:00Z"
  },
  "proof": {
    "type": "DataIntegrityProof",
    "cryptosuite": "eddsa-jcs-2022",
    "created": "2026-10-29T10:00:00Z",
    "verificationMethod": "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S#z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S",
    "proofPurpose": "assertionMethod",
    "proofValue": "z4wkqX9F8vbz1ww6tHENYSuN874yUNAYS27thqUbxBaKbFdzg8cRuYN3ppciKXSsxfax54nf2JEVCfj2n7MDYi3gC"
  }
}
```
