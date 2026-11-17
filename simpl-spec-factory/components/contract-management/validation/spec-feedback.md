# Spec feedback: Contract Management

Step 29 of the spec factory, assembled on 17 Nov 2026 by `spec-factory/pipeline/build_feedback.py` (engine 34f6f24). For the owners of the stories, the API definitions, the CDMs and the business process. Every point cites the ids it is about and the question that needs an answer; the full evidence is in the linked files.

**What this is.** An automated reading of the published specifications, as a builder would read them, before anything is built. A finding says where two careful readers would build different things, or where a builder would have to guess. It is not a judgement on the people who wrote the specs: most points are normal for specifications written by several teams over time.

## In short

- **Input:** 36 stories with 138 live acceptance criteria, 26 API operations, 31 business process requirements, and the CDMs of each API.
- **Findings:** 98: **21 to decide before building**, 53 to confirm, 24 small.
- **Mechanical checks:** 45 hits on the acceptance criteria and 78 on the API and CDM names; the agents triaged them, and the real ones are part of the findings above.
- **Coverage:** 54 of 138 criteria link to no operation and no process requirement; 13 of 26 operations and 18 of 31 process requirements have no criterion.

## Decide first

The blockers. A builder or tester cannot proceed on these without a decision, and two builders would decide differently.

### Behaviour and interactions

**F-001** (AC-C04). Three stories describe the existence check with three different interaction models: an async ACK plus a later callback, a synchronous 200 with a boolean, and a Contract Manager call to an EDC endpoint. The live criteria also place the check on different sides.

- Where: SIMPL-19824/AC1, SIMPL-19824/AC2, SIMPL-32509/AC1, SIMPL-35858/AC1
- Question: Which model is the target for the existence check: the existing async search endpoint with a callback, the new synchronous validateExistenceContract endpoint, or the /isPresent callback to EDC; and on which side (Provider or Consumer) does the Contract Manager run it?
  - Option: Async: POST /agreements/{contractNegotiationId}/search returns an ACK, result reported to EDC via /isPresent (SIMPL-19824 + SIMPL-35858)
  - Option: Sync: POST /resourceDescriptions/{resourceDescriptionId}/validateExistenceContract returns 200 with exists (SIMPL-32509), no callback

**F-002** (AC-C04). The create endpoint is assigned to the Provider in two stories and the process, and moved to the Consumer side in a third. The response also differs: an ACK with a later callback, a full offer body, or a 201 that triggers the Contract UI backend.

- Where: SIMPL-19829/AC1, SIMPL-32511/AC1, SIMPL-35526/AC1
- Question: On which side does /create run, and what does it return: ACK plus callback to the Provider Connector (SIMPL-19829), the offer body with usageContract and usageContractHash (SIMPL-32511), or 201 with a push to the Contract UI backend and a user notification (SIMPL-35526)?
  - Option: Provider side, creates the offer and returns it (SIMPL-19829 / SIMPL-32511, BP7.3.1)
  - Option: Consumer side, prepares offer data for the UI and notifies the user (SIMPL-35526)

**F-003** (AC-C04). Validation and Provider signing are specified twice with different observable outcomes. The older stories say validation is asynchronous and checked through persisted state and logs; the newer ones return 201 after validating and signing, or call EDC /isValid. SIMPL-19934 expects 200 with 'Finalized' from a signing call.

- Where: SIMPL-19933/AC10, SIMPL-19934/AC1, SIMPL-21948/AC10, SIMPL-35584/AC1, SIMPL-35677/AC1
- Question: Which stories define the target validation and signing flow on each side, and which ones are superseded? What does a test observe: the HTTP response, the persisted state, or the EDC callback?
  - Option: New flow: SIMPL-35584 (Provider) and SIMPL-35677 (Consumer) with EDC callbacks; SIMPL-19933, 19934, 21948 describe the same steps and only add the rejection reasons
  - Option: Old flow: SIMPL-19933, 19934, 21948 stay normative; SIMPL-35584 and 35677 only add the EDC trigger

**F-004** (AC-C04). Each story uses its own set of status names for the same offer or contract, and the two APIs use yet other enums. A UI overview, a filter and the scheduler cannot be built without one agreed set and a mapping.

- Where: SIMPL-30959/AC1, SIMPL-21924/AC2, SIMPL-32511/AC1, SIMPL-32505/AC2, SIMPL-32505/AC3, SIMPL-35692/AC4 and 4 more
- Question: What is the one status set for a usage contract offer and a usage contract (including Suspended, Revocation requested, Revoked, Reactivation requested, Terminated), and how does each map to the API enums?

**F-005** (AC-C05). The lifecycle stories (revoke, respond, suspend, reactivate, terminate) need operations that no OpenAPI offers, and each has a criterion where the other party sees the new status. No story says how the change reaches the other participant.

- Where: SIMPL-31053/AC3, SIMPL-31076/AC4.1, SIMPL-31076/AC4.2, SIMPL-31115/AC3, SIMPL-31116/AC3, SIMPL-31117/AC3 and 1 more
- Question: Which backend owns the lifecycle state and operations (contract-manager-be or contract-ui-be), and through which channel (EDC, Kafka, other) is a status change passed to the other participant?

**F-006** (AC-C03). The wallet failure criteria ask the user to name another secure location, but storage happens inside an automatic backend flow with no user in the loop. A builder can read this as a UI prompt or as a failed step with an error.

- Where: SIMPL-19927/AC2, SIMPL-19927/AC3, SIMPL-19927/AC4, SIMPL-19938/AC2.1, SIMPL-19938/AC2.2, SIMPL-19938/AC3.1 and 3 more
- Question: When the wallet is unavailable, full or corrupted during the automatic signing flow, what happens: a user prompt for another location, or the step fails, the error is logged and EDC is told (for example /signed/{id}/false)? What does 'another secure location' mean, if anything?
  - Option: UI prompt to choose another storage location (as written)
  - Option: No prompt: the flow fails with an error, the contract is not finalised and EDC is informed

**F-007** (AC-C03). The story holds two live versions of AC1 to AC4 with different actors and outcomes. A builder does not know which version counts.

- Where: SIMPL-19938
- Question: Which version of AC1 to AC4 is the live one: the user-driven list items (AC1.1 to AC4.1) or the system-level headings (AC1.2 to AC4.2)?
  - Option: List-item version (AC1.1, AC2.1, AC3.1, AC4.1)
  - Option: Heading version (AC1.2, AC2.2, AC3.2, AC4.2)

**F-008** (AC-C03). The criterion expects the user to see the result of the access-policy and existence checks right after clicking, but the story's own technical description says the UI only starts the negotiation and does not poll. The checks run later at the Provider.

- Where: SIMPL-21924/AC2
- Question: Does the catalogue UI show the validation result (positive, access denied, contract exists), or only that the request was sent, with the result shown later in the contract manager UI?
  - Option: The UI waits for or polls the Provider checks and shows the result (AC2 to AC4 as written)
  - Option: The UI shows 'request sent' only; results appear later in the contract manager UI

**F-010** (AC-C05). The story calls the accept and reject operation with POST and expects 201, while contract-manager-be defines PATCH /contractOffers/{contractNegotiationId}/{operation} returning 200. Tests from the story and a build from the API will disagree.

- Where: SIMPL-35660/AC1
- Question: Is the operation PATCH with 200 (as in the OpenAPI) or POST with 201 (as in the story)?
  - Option: PATCH, 200 (OpenAPI)
  - Option: POST, 201 (story)

**F-011** (AC-C05). The story calls /verifications with POST and expects '202 Created'; the OpenAPI defines PUT returning 200 with a VC in the request body. The story also says the Contract Manager fetches the VC itself through EDC, so the body may not be needed.

- Where: SIMPL-35677/AC1
- Question: Which method and status code apply (PUT/200, POST/201 or POST/202), and does EDC still send the VC in the body or does the Contract Manager fetch it via /vcExchange?
  - Option: PUT, 200, VC in body (OpenAPI)
  - Option: POST, 202 Accepted, no body, VC fetched via EDC (story)

**F-013** (AC-C05). The validation rules are delegated to an upload API that is not in the input set, and the story itself lists format, size, endpoint and errors as still to be clarified. A tester cannot write the pass or fail case for AC1 and AC3.

- Where: SIMPL-28846/AC1
- Question: Which upload operation, supported MIME types and maximum file size apply, and which component exposes the upload?

### One language for the domain

**D-001** (DDD-01). The main business object has four live names. The business process and the stories say 'usage contract'; the CDM, the API paths and schemas say 'contract agreement'; newer API stories say 'contract'; the dynamic view says 'Usage Contract Agreement'. A test writer working from the stories and a code writer working from the API will name the same thing differently.

- Where: SIMPL-35584, SIMPL-21928, SIMPL-35660, SIMPL-21948, BP7.7.2
- Question: Which single term names the legally binding contract between Consumer and Provider, in the stories, the CDM class and the API schemas?
  - Option: Proposed: 'usage contract' everywhere (business process and 30+ stories use it); rename the CDM class to UsageContract and treat ContractAgreement as a deprecated alias until the API moves
  - Option: Keep 'contract agreement' in the CDM and the API, and record in the glossary that it is the technical name of 'usage contract' (weaker: DDD-01 asks for one term)
  - Option: Rewrite the stories to 'contract agreement' (not proposed: against the business process)

**D-002** (DDD-02). 'Contract agreement' names two different concepts. The Contract Consumption CDM separates the EDC operational agreement (EDCContractAgreement) from the business ContractAgreement owned by the Contract Manager and says no identifier equivalence is implied. Yet the Contract Manager API is called by the EDC connector with a contractAgreementId in the path, so the same id appears to serve both. A builder cannot tell whether contractAgreementId is the EDC agreement id or a Contract Manager key.

- Where: SIMPL-35526/AC1, SIMPL-35584/AC1
- Question: Is the Contract Manager's contractAgreementId the EDC agreement id, or its own key with a separate reference to the EDC agreement?
  - Option: Same id: the usage contract is keyed by the EDC agreement id; then the Contract Consumption CDM's 'no identifier equivalence' note must change
  - Option: Different ids: the Contract Manager mints its own usage contract id and stores the EDC agreement id as a reference (for example edcContractAgreementId); the API paths then need renaming

**D-003** (DDD-02). 'ContractOffer' names two different concepts. In the Contract Manager CDM it is an external class (the EDC contract offer, referenced by contractOfferId). In both APIs it is the human-readable presentation of the offer (HTML, hash, status), which the Contract UI CDM calls HumanReadableContractData. The usage contract offer of the business process is a third reading. Tests and code built from different sources will attach offer status to different objects.

- Where: SIMPL-19829/AC1, SIMPL-22690/AC1, SIMPL-32511/AC1
- Question: Are the EDC contract offer, the machine-readable usage contract offer and the human-readable offer presentation one concept or three, and which of them owns the offer status?
  - Option: Proposed: three terms. 'usage contract offer' (business object with the status), 'human-readable usage contract offer' (its HTML rendering and hash, a value object), 'EDC contract offer' (external, contractOfferId only)
  - Option: One concept 'usage contract offer' whose rendering is an attribute; drop HumanReadableContractData and mark the EDC offer as a reference only

**D-006** (DDD-04). The accepted offer has two values on the main path. The Contract UI backend stores ACCEPTED and forwards the decision to the Contract Manager, whose offer status enum has APPROVED instead. The stories say 'Accepted'. A builder of either side has to guess the translation.

- Where: SIMPL-32505/AC2, SIMPL-30959/AC1
- Question: Is the accepted state ACCEPTED or APPROVED in both APIs?
  - Option: Proposed: ACCEPTED (stories, UI API, cdm.puml and the 'accept' operation all use it)
  - Option: APPROVED in both

### Entities, identities and states

**D-004** (DDD-09). The usage contract (and its offer) has no single identity. Offer, verification, VC and one search endpoint are keyed by contractNegotiationId; the agreement, credential and status endpoints by contractAgreementId. The same collection /agreements is addressed by a negotiation id in one path and an agreement id in another. The UI overview of usage contracts shows 'Contract negotiation ID' as the contract's id. The offer is created through a path that already requires a contractAgreementId, before any agreement exists.

- Where: SIMPL-30959/AC1, SIMPL-32511/AC1, SIMPL-35440/AC1
- Question: Which identifier identifies a usage contract offer and the resulting usage contract over the whole lifecycle, and under which name do the APIs expose it?
  - Option: One id for the whole lifecycle (for example contractNegotiationId from the first request), with contractAgreementId as an attribute set at finalisation
  - Option: Two entities with two ids: usage contract offer keyed by contractNegotiationId, usage contract keyed by contractAgreementId, with a written link; then /agreements/{contractNegotiationId}/search and the offer-creating .../create path need new names

**D-005** (DDD-12). No lifecycle is defined once for the usage contract offer or the usage contract. The Contract Manager CDM types the status as ContractAgreementStatus but never lists its values; the Contract UI CDM has no value set at all (the values sit only in cdm.puml next to the API). The stories add Suspended, Revocation requested, Revoked, Reactivation requested and Terminated with transitions, and no API operation changes them. The 23a script found 18 value-set mismatches; they all trace back to this missing definition.

- Where: SIMPL-30959/AC1, SIMPL-31053/AC2, SIMPL-31076/AC2, SIMPL-31115/AC2, SIMPL-31116/AC2, SIMPL-31117/AC2 and 2 more
- Question: Where is the one state machine for the usage contract offer and the usage contract written down (states, allowed transitions, who triggers each), and which operation performs each transition?
  - Option: Proposed: one state machine for the usage contract in the Contract Manager CDM, with the offer states as its first phase (Requested, Accepted, Rejected, Signed) and the contract states after it (Active, Suspended, Reactivation requested, Revocation requested, Revoked, Terminated)
  - Option: Two state machines: usage contract offer (in the Contract UI CDM) and usage contract (in the Contract Manager CDM), with the hand-over state named

### Error cases and testability

**F-009** (AC-C02). The only criterion has no oracle: no endpoint, no format, no 'given moment', and the 'audit rule package' from the user story is not described anywhere.

- Where: SIMPL-34633/AC1
- Question: What is the endpoint and response format of the audit data, which status set is counted, what is 'a given moment' (on request, scheduled), and what is the audit rule package?

**F-012** (AC-C01). The story has no criterion for the main failure: the consumer VC is invalid or cannot be fetched. A builder must guess whether the call returns an error code, returns 201 and reports false to EDC, or stays pending.

- Where: SIMPL-35584
- Question: What does validateAndIssue return and do when the consumer VC fails technical or business validation, when the VC cannot be fetched via EDC, and when the Signer, VC Issuer or wallet fails?
  - Option: Synchronous error code (4xx or 5xx), contract not signed
  - Option: 201 accepted, then EDC is told via /isValid/{id}/false or /signed/{id}/false

**F-014** (AC-C02). The success outcome of both validation stories is 'updated according to the agreed lifecycle' without naming the status, and the technical-error case allows two end states. There is no pass condition on the main path.

- Where: SIMPL-19933/AC1, SIMPL-19933/AC8, SIMPL-21948/AC1, SIMPL-21948/AC8
- Question: Which status does the negotiation or contract get after a successful validation, and after a technical error: FAILED or PENDING_RETRY (with retry)?
  - Option: Technical error ends in FAILED
  - Option: Technical error ends in PENDING_RETRY and is retried

### API guidelines and coverage

**P-001** (API-12). Two state-changing operations of the published API have no decision: build or retire. The decisions put Consumer signing inside the accept call (D5) and Provider signing inside validateAndIssue (D7), and treat the stories linked to these operations (SIMPL-19934, 21928, 21963) as superseded or made concrete elsewhere (D1). Yet the OpenAPI still offers /issue, which starts signing on its own, and PATCH .../status, which lets any caller with the API key set FINALIZED or TERMINATED directly. That bypasses the D12 state machine: Active is reached only through D7 or a successful D8 validation. One builder will implement them from the OpenAPI, another will leave them out, and the tests have no oracle for either.

- Where: contract-manager-be
- Question: Are POST .../issue and PATCH .../status retired in the target design, or kept? If kept, which D12 transitions may they cause and on which side?
  - Option: Retire both: not built, not in the next OpenAPI version (simplest, matches D5 and D7)
  - Option: Keep /issue as an internal alias of the D5 or D7 signing step, and retire PATCH .../status
  - Option: Keep both, guarded by D12: PATCH .../status only Signed to Active or Closed, with 409 otherwise

## Terms to agree

The draft glossary ([`glossary.md`](./glossary.md)) has 45 terms: 11 already used the same way everywhere, 24 where the draft proposes one term among synonyms, and 10 where the sources disagree on the meaning. One agreed term per concept, used the same way in the stories, the API and the CDM, removes a large part of the findings above.

Open: usage contract offer, EDC contract offer, resource description, existence check, active usage contract, signature, validation result, termination, Contract Orchestrator, EDC contract agreement.

## To confirm

The majors: the build can proceed on a sensible default, but the default should be confirmed.

| Id | Theme | Where | Finding | Question |
|---|---|---|---|---|
| F-015 | Behaviour and interactions | SIMPL-19933/AC2, SIMPL-19933/AC6, SIMPL-19933/AC9, SIMPL-21948/AC2, SIMPL-21948/AC9 | The failure criteria say 'rejected or failed' without saying which, and only the Consumer-side story maps cases to reason codes. The Provider side has no codes. | Which value (REJECTED or FAILED) belongs to each case, and do the Provider-side reasons use the same codes as the SIMPL-21948 table (with CONSUMER instead of PROVIDER)? |
| F-016 | Behaviour and interactions | SIMPL-19933/AC1, SIMPL-19933/AC4, SIMPL-21948/AC1, SIMPL-21948/AC4 | Both validation stories check a validity period, but no API schema or story defines where an offer or contract carries its validity dates. | Which field or rule defines the validity period of an offer and of a contract? |
| F-017 | Behaviour and interactions | SIMPL-19933/AC7, SIMPL-21948/AC7 | Duplicate or replayed submissions must be detected, but the key is not named. Two builders will pick different keys (negotiation id, signature, payload hash). | What makes two submissions 'the same' (contractNegotiationId, signature value, payload hash), and is the result REJECTED or IGNORED? |
| F-018 | Behaviour and interactions | SIMPL-32509/AC1, SIMPL-21924/AC4, SIMPL-19824/AC2 | 'Active usage contract' decides whether a new offer may be created, but the lifecycle stories add Suspended, Revocation requested and Reactivation requested. It is not said whether these count as active. | Which statuses count as 'active' for the existence check? |
| F-019 | Behaviour and interactions | SIMPL-21928/AC1, SIMPL-35660/AC1, SIMPL-32505/AC2 | Consumer signing is described in three places with different triggers: a generic 'signing process is triggered', the accept operation on the Contract Manager, and the UI accept action. It is unclear whether SIMPL-21928 adds behaviour beyond SIMPL-35660. | Is SIMPL-35660 the concrete trigger for SIMPL-21928, so that accept means sign at once, or is signing a separate user step after accept? |
| F-020 | Behaviour and interactions | SIMPL-35440/AC1, SIMPL-35584/AC1 | Two Contract Manager endpoints in the criteria are not in the OpenAPI, so request body, response body and error codes are unknown. | Where are these two operations defined (request and response schema, error codes), and will they be added to contract-manager-be openapi3-v1.yaml? |
| F-022 | Behaviour and interactions | SIMPL-31115/AC2, SIMPL-31117/AC2, SIMPL-31118/AC2, SIMPL-31076/AC2 | The lifecycle stories say why (remove or restore access) but no criterion says what happens to resource access, or who enforces it, when a contract becomes Suspended, Revoked, Terminated or Active again. | Must the Contract Manager tell another component (connector, consumption adapter) about a status change so that access is removed or restored, and how is that observed? |
| F-031 | Behaviour and interactions | SIMPL-32509 | The 401 and 403 cases are about the Consumer's authentication and role, while the call is made by the Provider and the APIs use an API key. Whose credentials are checked is unclear. AC3 is also missing from the numbering. | Is 401/403 about the caller's API key or about the Consumer identity carried in the request, and which role is required? |
| F-032 | Behaviour and interactions | SIMPL-32511/AC1 | The response status 'Requested' is not a value of the response enum, and the added response fields are not yet in the OpenAPI. A typo 'proverId' and '201 Ok' would be copied into tests. | Which enum value is returned on creation, and is the field providerId? |
| F-037 | Behaviour and interactions | SIMPL-35692 | 'Pending' and 'finalized' are not values of the Contract UI backend status enum, so the scheduler's selection is open. 'Terminated' is also not in the enum. A failing reject call to the Contract Manager has no criterion. | Which UI statuses are 'pending' (CREATED only, or CREATED and ACCEPTED) and 'finalized'? Is Terminated a new enum value? What happens if the reject call to the Contract Manager fails? |
| F-038 | Behaviour and interactions | SIMPL-19934/AC1 | The success response differs from the signing operation it links to: 200 in the story, 201 in the OpenAPI. The 'backend API' is not named. | Is the Provider signing call POST .../issue (201) or another operation, and is the returned status FINALIZED or FINALIZING? |
| F-041 | Behaviour and interactions | SIMPL-21966 | The Provider must send an email to the Consumer, but the recipient is 'resolved from participant context' with no source for the address, and retry is deferred to a contract that is not in the input. | Where does the Provider get the Consumer email address, and what retry rule applies when the Notification Service rejects the message? |
| F-043 | Behaviour and interactions | SIMPL-28846/AC6 | Re-upload of identical content can reuse the stored object or create a new one. The criterion and the functional rules allow both, so the returned documentId differs between builds. | For identical content, is a new documentId always created, or is the existing one returned? |
| F-044 | Behaviour and interactions | SIMPL-30959/AC1 | The overview needs fields and a list that the Contract UI backend does not return: GET /contractOffers returns a single ContractOffer with humanReadable, hash and status only. Active contracts live in the Contract Manager, not in the UI backend. | Which API serves the overview of offers and contracts, with which fields and filters? |
| F-047 | Behaviour and interactions | SIMPL-31047 | The consent status shown in the UI has no field in any API, and the story does not say how a change made in the consent manager reaches the contract manager UI or how 'consent not defined' is known. | Which source provides the consent status and the 'defined or not' flag, and is the UI refreshed or does it read the status on load? |
| F-049 | Behaviour and interactions | SIMPL-31118/AC1 | The title says 'active' but the Given allows any status that is 'not revoked or terminated', which includes offers, Suspended and Revocation requested. | From which statuses can a Provider terminate: Active only, or also Suspended, Revocation requested, Reactivation requested and open offers? |
| D-007 | One language for the domain | SIMPL-30959/AC1, SIMPL-30960/AC1, SIMPL-32511/AC1, SIMPL-21924/AC2 | The UI labels for statuses have no written mapping to stored values. 'Requested', 'Signed' and 'Active' have no API value at all; 'Requested' also appears as an API response value in SIMPL-32511 while the enum says CREATED; 'In progress' is used for a newly created usage contract. | For each UI label, which stored value does it show, and where is that mapping kept? |
| D-008 | One language for the domain | SIMPL-31118/AC2, SIMPL-35692/AC1, SIMPL-35692/AC4, SIMPL-35660/AC3 | 'Terminated' means three different things: a usage contract ended by the Provider because its termination conditions are met; an unsigned offer closed by the scheduler after the timeout (which is sent on as a reject); and a contract negotiation that ended without agreement (API value TERMINATED, EDC state). | Should an offer closed by timeout and a contract ended by the Provider carry different state names? |
| D-009 | One language for the domain | SIMPL-19938 | 'Signed usage contract' is used for the offer signed only by the Consumer and for the contract signed by both. The business process keeps them apart ('signed usage contract offer' after the Consumer signs, 'signed usage contract' after the Provider signs), but SIMPL-19938 says the Provider receives 'a signed usage contract' from the Consumer. | Do the stories adopt the business process pair 'signed usage contract offer' (Consumer signature only) and 'signed usage contract' (both signatures)? |
| D-010 | One language for the domain | SIMPL-35677/AC1, SIMPL-21948/AC1, SIMPL-19933/AC1 | The check of the other party's signed offer or contract is called 'validate' in the business process and most stories, and 'verify' in the API and SIMPL-35677, which implements BP 7.8.1. The business process uses 'verify' for a different step (access policy and existing contract, BP 7.2). | Should the API resource and the stories use 'validation' for the signed offer and contract check, and keep 'verify' for the BP 7.2 checks? |
| D-011 | Entities, identities and states | SIMPL-30959/AC1, SIMPL-32509/AC1, SIMPL-35526, SIMPL-32511/AC1 | The resource a contract covers is identified by three different ids under three names: resourceDescriptionId in the stories and the UI overview, assetId in the Contract Manager API and CDM, and selfDescriptionId in SIMPL-35526. No source says how they relate, so the overview cannot show 'Resource description ID' for a contract stored with an assetId. | Is the usage contract linked to the resource description (catalogue identity) or to the EDC asset, and under which name? |
| D-015 | Entities, identities and states | SIMPL-35692/AC1, SIMPL-22690/AC1, SIMPL-35660/AC1 | The usage contract offer is changed in two components with no named root. The Contract UI backend stores the offer and its status and takes the accept or reject decision; the Contract Manager has its own PATCH on the same path, a PUT that sets the offer status 'on UI side', and creates the offer through the agreement path. It is not said which component owns the offer aggregate. | Which component is the root of the usage contract offer (holds its state and performs its transitions), and which only shows it? |
| D-016 | Entities, identities and states | SIMPL-21948/AC2, SIMPL-19933/AC2, SIMPL-31053/AC1, SIMPL-28846/AC2, SIMPL-34633/AC1, SIMPL-21966/AC1 | Several business objects the stories rely on have no CDM class and no API schema: the validation result with its rejection reason, the revocation request with its reason, the suspension and reactivation reasons, the Contract Clause document with its hash metadata, the usage contract audit data and the notification request. The code writer will invent their names. | Which of these are Contract Management business objects that belong in its CDM, and which are owned elsewhere (Notification Service, document storage, logging and monitoring)? |
| D-017 | Entities, identities and states | SIMPL-32511/AC1 | The attributes of the human-readable offer have three sets of names: humanReadable and hash in both APIs, humanReadableData and contractData in the Contract UI CDM, and usageContract (html) and usageContractHash in the SIMPL-32511 response. The hash is one concept with two names. | Which attribute names does the human-readable usage contract offer use in the CDM and both APIs? |
| D-012 | Boundaries with other components | contract-manager-be.md#VerifiableCredential | The Contract Manager CDM defines VerifiableCredential as its own class with its own attributes, while the Issuer Service CDM owns VerifiableCredential with different attributes and the Wallet CDM calls the stored object Credential. The API schema follows the W3C data model, which neither CDM matches. The class should be external and point to its owner. | Should the Contract Manager CDM mark VerifiableCredential as external (owned by the VC Issuer) and keep only the reference it needs? |
| D-013 | Boundaries with other components | contract-manager-be.md | The external classes of the Contract Manager CDM name no owner, and their names differ from the owners' CDMs: Asset (catalogue says ResourceDescription, the consumption CDM says Dataset), Provider and Consumer as classes (the owners model Participant with a role), ContractNegotiation and ContractDefinition (the consumption CDM names them EDCContractNegotiation and EDCContractDefinition, owned by the EDC Connector). | Should the Contract Manager CDM reference external concepts by their owners' names and list each owner? |
| D-014 | Boundaries with other components | SIMPL-22690/AC1, SIMPL-35660/AC1, SIMPL-35677/AC1, SIMPL-35584/AC1 | The Contract Manager API speaks the EDC connector's language: its paths and payloads are keyed on contract negotiation, contract definition, asset and contract offer ids, and its info says it is for 'interaction with the Dataspace connector'. The translation to EDC also sits in two places: the stories have the Contract Manager call EDC endpoints directly, while SIMPL-22690 and flow.puml put a Contract Orchestrator and Kafka in between. The business process notes that negotiation of terms is not a current requirement, yet 'contract negotiation' is the main key. | Where is the one translation layer between Contract Management and the EDC connector (Contract Manager itself, or the Contract Orchestrator), and which business terms does the Contract Manager API use instead of EDC terms? |
| D-019 | Boundaries with other components | cross | There is no context map for Contract Management, although it depends on at least seven other contexts (EDC connector, VC Issuer, Signer Service, Wallet, Notification Service, Consent Management, Federated Catalogue). The register links a concept reference index that is not in the snapshot. The consumption and consent CDMs carry traceability tables; the two Contract Management CDMs do not, so the consent CDM's reference to ContractAgreement is not mirrored. | Who draws the context map for Contract Management, and does it live with the CDM register or with the component? |
| F-023 | Error cases and testability | SIMPL-31053/AC2, SIMPL-31076/AC3, SIMPL-31115/AC2, SIMPL-31116/AC2, SIMPL-31118/AC2 | Reasons and justifications are asked for in five actions, but no criterion says whether they are mandatory, what limits apply, or whether the other party sees them. | Is the reason mandatory (and what happens if it is empty), is there a length limit, and is it shown to the other participant? |
| F-024 | Error cases and testability | SIMPL-19927/AC1, SIMPL-19938/AC1.1, SIMPL-19938/AC1.2 | The 'securely stored' outcomes have no oracle beyond the wallet call, and 'access is restricted to authorized roles' names no role. | What does a test check for 'securely stored' (VC present in the wallet, retrievable via GET /agreements/{contractAgreementId}?), and which roles may read it? |
| F-030 | Error cases and testability | SIMPL-32509/AC4 | The catch-all error criterion has no oracle: 'cannot be properly fulfilled' is not a condition, and the code is 'appropriate' with examples. | Which conditions map to 400, 406, 415 and 500 for this endpoint? |
| F-033 | Error cases and testability | SIMPL-35526/AC1 | The user notification has no channel or content, and the failure paths (Contract UI backend or catalogue unavailable, no user email) have no criterion. | How is the user notified (Notification Service email, UI only), and what does /create return when the Contract UI backend call or the notification fails? |
| F-034 | Error cases and testability | SIMPL-35660 | The reject criterion has no response code, signing failures have no criterion, and there is no criterion for a call on the provider side, although the handler acts 'only when called on the consumer side'. | What does the reject call return, what happens when Signer, VC Issuer or wallet fails, and is a provider-side call answered with 400 as in SIMPL-35584/AC3? |
| F-035 | Error cases and testability | SIMPL-35677 | No criterion covers the VC fetch failing (EDC unreachable, other side returns 404 per SIMPL-35440/AC2), and AC3 gives no response code. | When the provider VC cannot be fetched, is that /isValid false, an error response, or a retry? Which code does the call return in AC3? |
| F-036 | Error cases and testability | SIMPL-35440 | The 404 case covers 'not signed yet' only. An unknown contractNegotiationId and an unavailable wallet have no criterion, though the handler checks both. | What is returned for an unknown contractNegotiationId and when the wallet service is unavailable? |
| F-039 | Error cases and testability | SIMPL-21963 | The criteria describe abilities ('can access', 'can continue') and 'sufficient evidence' without an observable check. The story defers the mechanism to other stories. | Which observable item proves delivery (a status on GET /agreements/{contractAgreementId}, a PATCH .../status call, a log entry), and does this story add anything beyond SIMPL-35660 and SIMPL-35584? |
| F-040 | Error cases and testability | SIMPL-21928 | The outcomes 'signing result is registered' and 'rejected' have no observable form: no status, no field, no response code. | Where is the signing result visible (status value, field), and what does a rejected request return (401, 403)? |
| F-042 | Error cases and testability | SIMPL-28846 | 'Clear' error messages have no oracle: no status code, no error format, no message text. | Which status codes and error format (for example RFC 9457 Problem) apply to validation, metadata and storage failures? |
| F-045 | Error cases and testability | SIMPL-30959/AC1 | The overview offers a Reject button, but no UI story says what Reject does or shows, and the replacement accept story has no failure case. | What does the user see after Reject (status 'Rejected'?), and what happens when accept or reject fails? |
| F-046 | Error cases and testability | SIMPL-30960/AC1 | The details view has no criterion for an offer that is not found or a user without the role, while the API has 403 and 404. The detail fields (resource name, Provider name, policies, clauses) are not in ContractOffer. | What does the UI show for 403 and 404, and where do the detail fields come from? |
| F-048 | Error cases and testability | SIMPL-31117 | A reactivation request can only be granted. There is no criterion for the Provider declining it, so the contract could stay in 'Reactivation requested' with no way back to 'Suspended'. | Can the Provider decline a reactivation request, and to which status does the contract go? |
| D-018 | Data model governance | cross | Each API has a CDM in the register, but all three are 'In review, not approved for delivery'. The Contract Manager CDM has two own classes and the Contract UI CDM one, against about ten business objects in the stories. The two Contract Management CDMs still point to API definitions in the old development/contract-billing repositories, not to the APIs they describe. | When will the Contract Manager and Contract UI CDMs be completed (offer, lifecycle, validation result), linked to the current API repositories and approved? |
| P-002 | API guidelines and coverage | contract-manager-be | Four operations have no acceptance criterion and no decision. POST /agreements/search could serve the overview of usage contracts after acceptance, which D14 leaves as an owner question, so a story is missing. The other three belong to the replaced orchestrator design and should be retired: PUT .../update pushes a status from the Provider side to the UI side, which D21 forbids (the Contract Manager is the root, contract-ui-be changes its own status only after its own call succeeds); the Contract Manager's own POST /contractOffers relays the offer to contract-ui-be, which D4 now does inside /create; GET .../file reads a text file from the filesystem that the D12 record and the E8 rendering do not know. Without a decision a builder may implement them. | Which of these four operations stay in the target API? Does POST /agreements/search serve the usage contract overview after acceptance (D14 owner question)? |
| P-003 | API guidelines and coverage | contract-manager-be:POST .../create, contract-manager-be:POST /agreements/{contractNegotiationId}/search, contract-manager-be:PATCH /contractOffers/{contractNegotiationId}/{operation}, contract-manager-be:PUT /verifications/{contractNegotiationId}, contract-ui-be:PATCH /contractOffers/{contractNegotiationId}/{operation}, contract-ui-be:POST /contractOffers/{contractNegotiationId} and 1 more | The OpenAPI files do not list response codes that the decisions define, and list one that a decision forbids. A builder working from the OpenAPI and a tester working from the decisions will disagree, and a contract-testing tool that checks responses against the OpenAPI will reject correct answers. The decisions already ask to add the three new operations (D7, D10, D17); they do not ask to add these codes to the existing ones. | Will the OpenAPI files be updated to the response codes of D3, D4, D5, D8 and D14 before the build, or do the decisions override the OpenAPI for the build? |
| P-004 | API guidelines and coverage | contract-manager-be:POST /agreements/{contractNegotiationId}/search request, contract-manager-be:ContractAgreementCreate, contract-manager-be:POST .../issue request, contract-manager-be:VerifyCredentialRequest, contract-ui-be:ContractOffer | Request schemas allow what the decisions forbid, or forbid what they allow. The search body and ContractAgreementCreate declare no required properties, while D3, D4 and D7 make all five fields required with a 400 otherwise. PUT /verifications marks its body required, while D8 says the body is optional and ignored. The contract-ui-be ContractOffer has no assetId, providerId, consumerId, contractNegotiationId or createdAt, which D4 sends and D14 adds. A schema-validating builder would reject the D8 call without a body and accept a D3 call with missing fields. | Can the schemas be aligned with D3, D4, D7, D8 and D14 (required lists, optional VC body, extra ContractOffer properties)? |
| P-005 | API guidelines and coverage | contract-ui-be | In contract-ui-be every success example contradicts its schema. The GET and PATCH examples use humanReadableData, hashData, contractData, createdDate and lastUpdatedDate; the schema has humanReadable, hash and status, and D14 names createdAt. The PATCH schema is a plain string (as D14 decides) but its example is a full object. The POST example returns offerStatus, which ContractOfferResponse does not have, and the offerUrl points to the backend path while flow.puml says the frontend. A builder and a tester who copy the examples will produce different payloads. D-017 and D21 settle the names in the schemas and the CDM, not these examples, and lastUpdatedDate and offerUrl are decided nowhere. | Is lastUpdatedDate part of ContractOffer, and what does offerUrl point to (frontend or backend, built from which configured base URL)? |
| P-006 | API guidelines and coverage | contract-manager-be:Problem examples, contract-ui-be:Problem examples | The problem detail examples contradict the error format of E5. They use type values E5 does not have (argumentTypeMismatch, badRequest, methodNotSupported, methodNotAllowed, singleCustomException, internalServerError), one type outside the Simpl URN scheme (urn:error:single-custom-exception), and many examples have no type at all. instance is declared format uri but every example holds a relative path. Tests written from E5 will fail against a build that copies the OpenAPI examples. | Do the OpenAPI examples follow E5 in the next version, and is instance a URI reference (relative allowed) as RFC 9457 permits? |
| P-007 | API guidelines and coverage | contract-manager-be:ContractAgreementData.consumerSignatureDate, contract-manager-be:ContractAgreementData.providerSignatureDate, contract-ui-be:ContractOffer createdAt | Date and time values have no declared format. The signature dates are plain strings with an example without time zone, while D8 copies providerSignatureDate from the VC issuanceDate, which is date-time with a zone. The contract-ui-be examples use microseconds without zone. A tester comparing providerSignatureDate with issuanceDate, or ordering offers by creation time for the D13 timeout, needs one format. | Are all timestamps RFC 3339 date-time in UTC with a Z suffix? |
| F-021 | Scope | SIMPL-35858/AC1, SIMPL-35863/AC1, SIMPL-35888/AC1, SIMPL-35889 | Four stories specify endpoints of the EDC connector, not of the Contract Management component. For this component they only define the client side of a call. | Are these stories in scope for the Contract Management build, or only as an interface contract the Contract Manager calls? |
| F-027 | Scope | SIMPL-19824 | The replaced story is still live and holds requirements its replacement lacks: 400 and 405 handling, error logging, and 'the contract establishment process is prevented from proceeding'. | Should SIMPL-19824 be closed, and do its 400, 405, logging and 'prevent proceeding' outcomes carry over to SIMPL-32509? |
| F-028 | Scope | SIMPL-19829 | The replaced story is still live and holds error cases its replacement lacks: 400, 401, 405, 415, 500 and 'no contract offer is created'. | Should SIMPL-19829 be closed, and do its error criteria (including 'no contract offer is created') carry over to SIMPL-32511? |
| F-029 | Scope | SIMPL-22690 | The replaced story is still live and adds requirements its replacement lacks: a failure case, idempotency per contractNegotiationId, an immutable audit record and correlationId. Its criteria also point to an API (SIMPL-28977) that is not in the input. | Should SIMPL-22690 be closed, and do the failure case, idempotency and audit requirements carry over to SIMPL-32505? What does 'remains active' mean as a status? |
| F-050 | Scope | SIMPL-21924 | AC1, AC3 and AC4 describe the catalogue UI (button, error messages), which is another component. Only AC2 creates state in the contract manager. | Are the catalogue UI criteria part of this component's build, or only the contract manager part of AC2? |

The 24 minor findings (wording, numbering, duplicates) are in [`ac-findings.json`](./ac-findings.json) and [`domain-findings.json`](./domain-findings.json).

## Coverage

From the links between criteria, operations and process requirements (step 12b, [`../inventory/inventory-review.md`](../inventory/inventory-review.md)). A gap is not always wrong, but each one is a place where the specs do not say how the parts connect.

- Operations with no criterion (13): `contract-consumption-adapter-be:GET /transfers/{transferProcessId}/dataAddress`, `contract-consumption-adapter-be:GET /resourceAddresses/templates/{templateId}/schema`, `contract-consumption-adapter-be:POST /transfers`, `contract-consumption-adapter-be:GET /resourceAddresses/sharingMethods/{sharingMethodId}/templates`, `contract-consumption-adapter-be:GET /resourceAddresses/templates/{templateId}/uiSchema`, `contract-consumption-adapter-be:POST /connectorCatalog/assets`, `contract-consumption-adapter-be:GET /contracts/{contractNegotiationId}`, `contract-consumption-adapter-be:GET /transfers/{transferProcessId}`, `contract-manager-be:GET /health`, `contract-manager-be:GET /agreements/{contractAgreementId}/file`, `contract-manager-be:POST /agreements/search`, `contract-manager-be:PUT /contractOffers/{contractNegotiationId}/update`, `contract-ui-be:GET /health`
- Process requirements with no criterion (18): `BP07.01`, `BP07.02`, `BP07.03`, `BP07.04`, `BP07.05`, `BP07.06`, `BP07.07`, `BP07.08`, `BP07.09`, `BP07.10`, `BP07.11`, `BP07.12`, `BP7.3.2`, `BP10.01`, `BP10.02`, `BP10.03`, `BP10.04`, `BP10.05`
- Criteria with no operation and no process requirement: 54, listed in the review.

## The data models (CDMs)

| API | CDM | Register status | Approved | Branch |
|---|---|---|---|---|
| contract-manager-be | contract-manager-be.md | In review | No | develop |
| contract-ui-be | contract-ui-be.md | In review | No | develop |
| contract-consumption-adapter-be | contract-consumption-adapter-be.md | In review | No | feature/add-cdm |

See [`cdm/README.md`](../../../cdm/README.md) for the state of the CDM register as a whole.

## How this was made

| Step | What | File |
|---|---|---|
| 12 | every story, criterion, operation and process requirement with a stable id, and the links between them | [`../inventory/`](../inventory/inventory.md) |
| 20a, 20b | the acceptance criteria: structure and wording (script), then completeness, testability, ambiguity, consistency and correctness (agent) | [`ac-checks.md`](./ac-checks.md), [`ac-review.md`](./ac-review.md) |
| 21a, 21b | the API definitions against the API, security and OWASP rules, and the coverage between operations and stories | [`api-checks.md`](./api-checks.md), [`api-review.md`](./api-review.md) |
| 23a, 23b | the domain language: API against CDM against the stories, by the `DDD-` and CDM rules; the draft glossary | [`domain-checks.md`](./domain-checks.md), [`domain-review.md`](./domain-review.md) |

The agents worked only from the published specs and each other's output, not from code or earlier builds. Their findings were not checked one by one by a person; treat each as a well-argued question, not a verdict.

