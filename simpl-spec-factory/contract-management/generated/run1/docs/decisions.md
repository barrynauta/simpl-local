# How each decision is implemented

Paths: `CM/` = `contract-manager/src/main/java/eu/simpl/cm/manager/`, `UIBE/` = `contract-ui-backend/src/main/java/eu/simpl/cm/uibe/`, `STUB/` = `stubs/src/main/java/eu/simpl/cm/stubs/`, `COMMON/` = `common/src/main/java/eu/simpl/cm/common/`, `UI/` = `ui/src/`.

Where a decision left room, the choice is written here and in `../BUILD-LOG.md`.

| ID | Implementation | Code |
|---|---|---|
| D1 | Separate services; CM `PATCH /contractOffers` takes the CM key, UIBE `PATCH` takes a user token, UIBE `POST /contractOffers` takes the UIBE key. | `CM/web/CmController`, `UIBE/UibeController`, `UIBE/ApiKeyFilter` |
| D2 | `CM_PARTICIPANT_ID`; side from body ids or the stored `partyRole`; wrong side gives 400 `wrongParty`. One image for both parties. | `CM/service/EstablishmentService` |
| D3 | No orchestrator; stub calls CM over HTTP and CM calls stub callbacks. | `STUB/Choreography`, `CM/clients/StubClient` |
| D4 | Only the OpenAPI paths are built; codes 200 for PATCH and PUT. | `CM/web/CmController` |
| D5 | `GET /verifications/{nid}/vc` returns the own VC (stored on the record when it was issued). | `EstablishmentService.ownVc` |
| D6 | `GET /contracts`, `GET /contracts/{nid}`, `POST /contracts/{nid}/lifecycleActions`. | `CM/service/QueryService`, `LifecycleService` |
| D7 | UIBE versions of the three, plus `POST /admin/offerExpiry/run`. | `UIBE/UibeController` |
| D8 | UIBE `GET /contractOffers` returns an array, empty when none. | `UIBE/OfferService.all` |
| D9 | Requests read `humanReadable`/`hash`; responses use `contractNegotiationId`, `humanReadableData`, `contractData` (null), `hashData`, `status`, `createdDate`, `lastUpdatedDate`. | `UIBE/OfferService.view` |
| D10 | UIBE offer status CREATED, ACCEPTED, REJECTED, TERMINATED; CM update accepts CREATED, APPROVED, REJECTED only. | `UIBE/OfferService`, `EstablishmentService.updateOffer` |
| D11 | All errors written as problem JSON by one advice and the filters; all listed types; no stack traces. | `COMMON/web/GlobalErrorHandler`, `Problems` |
| D12 | 400 `validationFailure` "Unknown contractNegotiationId" where no 404 is documented; 404 elsewhere. | `EstablishmentService.byNidOr400`, `QueryService` |
| D13 | `GET {base}/health` without auth, `application/vnd.spring-boot.actuator.v3+json`, `SELECT 1` check, 503 problem. | `CM/web/HealthController`, `UIBE/HealthController` |
| D14 | `X-Correlation-Id` filter, MDC, outbound headers, Kafka header `correlationId`. | `COMMON/web/CorrelationIdFilter`, `ContractService.headers` |
| D15 | `Times.local` for OpenAPI responses, `Times.rfc3339` for added operations and events. | `COMMON/web/Times` |
| D16 | `Ids.pathId`, `Ids.pathUuid`, `JsonBody.requiredId`. | `COMMON/web/Ids`, `JsonBody` |
| D17 | `POST /_control/negotiations` runs C.2 on a virtual thread. | `STUB/ControlController`, `Choreography` |
| D18 | One nid and one aid shared by both parties. | stub generates them once |
| D19 | Open-status search on own records, other nid; response with `exists` and `contractSearchTO`; Provider record created when nothing exists; callback after commit. Repeated search for one nid does not create a second record. | `EstablishmentService.search` |
| D20 | Consumer only; validation, `alreadyExists`, catalogue 404 and 403, directory names, HTML and hash, UIBE post (500 and no record on failure), email, 201 with all fields. | `EstablishmentService.create` |
| D21 | HTML fragment in the required order, all values escaped, SHA-256 lowercase hex. | `CM/service/OfferHtml` |
| D22 | `EmailNotification` to the Consumer directory email; `NOTIFICATION_SKIPPED` without email. | `ContractService.email` |
| D23 | UIBE checks CREATED, calls CM, sets ACCEPTED or REJECTED only on CM success, passes CM errors through; CM checks REQUESTED. Repeated accept is 400 `offerNotPending`. | `UIBE/OfferService.decide`, `EstablishmentService.offerDecision` |
| D24 | Issue, wallet, SIGNED with `consumerSignatureDate`, `/signed/true`; failures give FAILED with `SIGNING_FAILED` or `WALLET_*` and `/signed/false`. | `SigningService.consumerSign` |
| D25 | Provider only; body optional (record by aid without body); 201 `INITIATED`; work after the response. | `EstablishmentService.issue` |
| D26 | `GET /edc/{own}/vcExchange/{nid}/request`; 404 gives `MISSING_CONTRACT_DATA`; other errors are technical. | `SigningService.doProviderValidateAndSign` |
| D27 | One algorithm, checks 2 to 7 in order, first failure wins, party in the code name. Required fields: `@context`, `type`, `issuer`, `credentialSubject`, `validFrom`, `validUntil`, `proof` and all eleven D55 subject fields. | `CM/service/CredentialValidator` |
| D28 | Duplicate when the stored `validationResult` is SUCCESS or REJECTED. Per C1 the stored result, code and `validatedAt` stay unchanged; only a VALIDATION history entry with `DUPLICATE_OR_REPLAYED_SUBMISSION` and the text `Validation result IGNORED` is written. After FAILED a new trigger validates again. A second trigger while one runs is also a duplicate. | `SigningService.isDuplicate`, `recordDuplicate`, `claim` |
| D29 | Success: `/isValid/true`, own VC with subject copied, both VCs in the wallet, SIGNED with both dates, `/signed/true`. Rejected: FAILED, `/isValid/false`, email. Issue or wallet failure: FAILED, `/signed/false`. | `SigningService.doProviderValidateAndSign` |
| D30 | Email "could not be validated" with nid and reason; skipped and failed entries. | `SigningService`, `ContractService.email` |
| D31 | Consumer only, VC in body, record SIGNED; success stores the VC, ACTIVE with `providerSignatureDate` from `signedAt`, `/isValid/true`. | `EstablishmentService.verify`, `SigningService.doConsumerVerify` |
| D32 | `FINALIZED` from SIGNED to ACTIVE; `TERMINATED` from REQUESTED or SIGNED with `COUNTERPARTY_TERMINATED`; else 400 `invalidTransition`. | `EstablishmentService.agreementStatus` |
| D33 | Consumer only; `POST` replaces HTML and hash while REQUESTED; `PUT .../update` `REJECTED` gives FAILED `PROVIDER_REJECTED`. | `EstablishmentService.transferOffer`, `updateOffer` |
| D34 | Agreement read, seven-line file (`\n`, no trailing newline), established search. | `QueryService` |
| D35 | `validUntil` = signing time + `CONTRACT_VC_VALIDITY`; checked only during validation. | `SigningService.issue`, `CmSettings` |
| D36 | Only the two emails; every change is a status event and visible in the UI. | `ContractService` |
| D37 | `ContractStatus` sets; `LifecycleAction` holds the L table; E and P transitions in the establishment services; 409 for any other lifecycle combination; one history entry and one status event per transition. | `CM/domain/`, `LifecycleService` |
| D38 | `ContractStatus.technical()`; trigger responses keep fixed values. | `CM/domain/ContractStatus` |
| D39 | One `reason`; required ones non-blank after trim; max 1000 characters. | `LifecycleService.checkReason` |
| D40 | 401 and 403 in UIBE; 409 for wrong party or state; optimistic locking gives 409 to the loser. | `UIBE/UserContext`, `ContractRecord.version`, `GlobalErrorHandler` |
| D41 | `revocationRequestedBy` set on L4 at both parties, cleared on L5 and L6. | `LifecycleService.apply` |
| D42 | TERMINATE from established statuses only. | `LifecycleAction.TERMINATE` |
| D43 | `consentStatus` from catalogue `consentRequired`; A.5 endpoint; UIBE adds `consentManagerUrl`; no blocking. | `Enricher`, `LifecycleService.consent`, `UibeController.enrich` |
| D44 | UIBE run: CREATED and older than `asOf` minus timeout; CM `reject?reason=EXPIRED` first, then TERMINATED in UIBE; failures retried next run. Cron `OFFER_EXPIRY_CRON` in UTC. | `UIBE/OfferService.expire`, `ExpiryScheduler` |
| D45 | Status and events only. | |
| D46 | Three topics created by CM (`NewTopic` beans, 1 partition); broker auto-create is off. | `CM/events/KafkaConfig`, `docker-compose.yml` |
| D47 | Listener filters recipient and sender, deduplicates on `messageId` (table `processed_message`), checks `senderRole` and the transition, applies it with trigger `COUNTERPARTY`, publishes no message back. | `LifecycleService.onCounterpartyMessage` |
| D48 | Not fitting: nothing changes, `COUNTERPARTY_MESSAGE_REJECTED` when the nid is known. | same |
| D49 | Status event on every transition and on creation (`previousStatus: null`), after commit. | `ContractService.statusEvent` |
| D50 | One partition, group `cm-<participantId>`, `auto-offset-reset=earliest`. | `LifecycleListener`, `application.properties` |
| D51 | Host listener `localhost:19092`. | `docker-compose.yml` |
| D52 | One stub; CM never holds keys. | `STUB/StubController` |
| D53 | VC 2.0 with `DataIntegrityProof` `eddsa-jcs-2022`. | `COMMON/proof/DataIntegrity`, `Jcs` |
| D54 | Seeds `11`, `22`, `33`; `did:key`; only `provider-01` and `consumer-01` trusted. | `STUB/StubState`, `COMMON/proof/DidKey` |
| D55 | Credential subject with eleven fields; the Provider copies the Consumer subject and changes `signerRole` and `signedAt`. | `SigningService.subjectFor` |
| D56 | All injection paths work through the stub (issue for any participant and dates, faults, overrides). | `STUB/` |
| D57 | 503, 507, 500 mapped; anything else is unavailable; history reason text is the story message. | `SigningService.walletError` |
| D58 | Each wallet ends with two VCs; `GET /verifications/{nid}/vc` returns the own one. | `SigningService` |
| D59 | `SIGNER:FAIL` gives FAILED `SIGNING_FAILED`, not an HTTP 500. | `SigningService` |
| D60 | All fields on `ContractRecord`, own database per CM. | `CM/domain/ContractRecord` |
| D61 | Append-only `history_entry`; no API changes it; oldest first. Extra entry types: `CONSENT_STATUS_CHANGED`, `OFFER_UPDATE` (see build log). | `ContractService.writeHistory` |
| D62 | Counts of all eleven statuses, zeros included. | `QueryService.counts` |
| D63 | Multipart upload, three media types, `CLAUSE_MAX_BYTES`, new id each time, content stored, never returned. | `CM/web/ClauseController` |
| D64 | One log line per transition with nid and correlation id. | `ContractService.transition` |
| D65 | CM key filter, 401 only. | `CM/web/ApiKeyFilter` |
| D66 | UIBE key filter for machine operations; JWT for user operations; API key alone gives 401 there. | `UIBE/SecurityConfig`, `ApiKeyFilter` |
| D67 | Realm imports with client, roles, hardcoded `participant_id` mapper, users; `KC_HOSTNAME=http://localhost:19080`, `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. | `env/keycloak/*.json`, `docker-compose.yml` |
| D68 | `contract_manager` for user operations; `UPDATE_CONSENT` also needs `consent_manager`. | `UIBE/UserContext`, `UibeController.enrich` |
| D69 | Issuer check (401), `participant_id` check (403). | `UIBE/SecurityConfig`, `UserContext` |
| D70 | Stub requires `X-Api-Key: test-edc-key` on `/edc/...`; CM always sends it. | `STUB/StubController`, `CM/clients/StubClient` |
| D71 | Overview, details, filters, actions, reason dialog with blank check. | `UI/views/` |
| D72 | All listed `data-testid` values. | `UI/views/Overview.vue`, `Details.vue`, `App.vue` |
| D73 | Buttons rendered only for `allowedActions`. | `UI/views/Details.vue` |
| D74 | Without `contract_manager` only `access-denied` is shown in the main area. | `UI/App.vue` |
| D75 | DOMPurify with scripts, styles, frames, images, links and URL attributes removed. | `UI/views/Details.vue` |
| D76 | Compose project, service names, host ports, Postgres 16 and the four databases. | `docker-compose.yml`, `env/postgres/init.sql` |
| D77 | Keys and settings by environment variables. | `CM/CmSettings`, `UIBE/UibeSettings` |
| D78 | Stub, realms and compose are in this build. | |
| D79 | Every asynchronous step completes in well under a second in the reference stack. | |
| D80 | `X-Contract-Negotiation-Id` on every CM call to the stub; faults and overrides per nid. | `CM/clients/StubClient`, `STUB/StubState` |
| D81 | No deletes; tests generate their own assets. | |
| D82 | `POST /admin/offerExpiry/run` with `asOf` and ids. | `UIBE/UibeController.expiry` |
| D83 | Directory of C.5 and asset `asset-seed-01` (`consentRequired: true`) at start. | `STUB/StubState` |

## Clarifications after run 1

| ID | Implementation | Code |
|---|---|---|
| C1 | Duplicate triggers leave `validationResult`, `validationReasonCode`, `validatedAt` untouched; IGNORED only in history. | `SigningService.recordDuplicate`, `isDuplicate` |
| C2 | `ownCredentialId` and `counterpartyCredentialId` in `GET /contracts/{nid}` (null until set); UIBE passes them on. | `QueryService.details` |
| C3 | Already so: the `CONNECTOR_CALLBACK` entry is written after the callback returns. No change. | `ContractService.callback` |
| C4 | Transitions caused by `/issue` or `PUT /verifications` (P1, P2, E9, E10) carry trigger `CONNECTOR`, also when they run in the background. Consumer signing (E5, E6) stays `SYSTEM`. | `SigningService` |
| C5 | No change: the details page reloads and shows the current status after accept. | `UI/views/Details.vue` |
| C6 | Overview headers "Contract negotiation ID", "Resource description ID", "Resource name", "Provider name" (Consumer UI) or "Consumer name" (Provider UI), "Status". | `UI/views/Overview.vue` |
