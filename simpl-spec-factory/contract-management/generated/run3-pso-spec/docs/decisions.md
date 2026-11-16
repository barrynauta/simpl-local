# How each decision is implemented

Paths are relative to `build/backend` unless noted. CM = `contract-manager/src/main/java/eu/simpl/cm/manager`,
UIBE = `contract-ui-backend/src/main/java/eu/simpl/cm/uibe`, STUB = `stubs/src/main/java/eu/simpl/cm/stubs`.

| ID | Implementation |
|---|---|
| D1 | CM `EstablishmentController` (connector and UIBE callers), UIBE `OfferController`; CM calls UIBE through `client/UibeClient`. |
| D2 | `CM_PARTICIPANT_ID`; side from body ids (`create`, `issue`, `search`) or the stored `partyRole`; 400 `wrongParty` (`ApiException.wrongParty`). |
| D3 | No orchestrator. Stub calls CM over HTTP, CM calls `/edc/...` (`client/StubClient`). |
| D4 | Only the OpenAPI paths exist. PATCH offers 200, PUT verifications 200, behaviour of 35584 on `/issue`, of 32509 on `/agreements/{nid}/search`. |
| D5 | `GET /verifications/{nid}/vc` returns `ownCredential` of the record. |
| D6 | `ContractController`: `/contracts`, `/contracts/{nid}`, `/contracts/{nid}/lifecycleActions`. |
| D7 | UIBE `ContractsController`, `OfferController.expiry`. |
| D8 | `GET /contractOffers` returns a JSON array, empty when none. |
| D9 | `OfferService.view`: `contractNegotiationId`, `humanReadableData`, `contractData` (null), `hashData`, `status`, `createdDate`, `lastUpdatedDate`; requests read `humanReadable`, `hash`. |
| D10 | UIBE offer status CREATED, ACCEPTED, REJECTED, TERMINATED. CM `PUT .../update` accepts CREATED, APPROVED, REJECTED only. |
| D11 | `common/ProblemWriter` writes the body directly (never empty, 401 included); `ApiExceptionHandler` maps 405, 406, 415, 400, 404, 413, 500; `instance` is the request path. Spring problem details and the error page are off. |
| D12 | `requireRecord`/`requireByAgreement` give 404 with "Unknown contractNegotiationId" or "Unknown contractAgreementId" on every operation on an existing record. |
| D13 | CM `web/HealthController` behind the API key filter; 503 when `select 1` fails. UIBE health open. Compose health checks send the CM key. |
| D14 | `common/CorrelationIdFilter`, `CorrelationContext` (carried into background tasks), header on outbound calls and Kafka headers. |
| D15 | `service/Dates`: OpenAPI responses without offset, added operations RFC 3339 with `Z`. |
| D16 | `common/Ids`: UUID for aid, pattern 1 to 100 chars for the other ids, path and body. |
| D84 | API key filter first; path format in the controller; body parsing by Spring (`@RequestBody(required=false)` so a missing body is a field error after the 404); then record, fields, party, state in each service method. |
| D17 | Stub `POST /_control/negotiations` and `Choreography`. |
| D18 | One nid and one aid shared by both parties. |
| D19 | `EstablishmentService.search`: open duplicates query, response with `exists` and `contractSearchTO`, Provider record created in REQUESTED with enrichment, `isPresent` callback after the response. |
| D20 | `EstablishmentService.create`: order as the row; UIBE first, record kept only when UIBE accepted; 201 with the 32511 fields. |
| D21 | `service/OfferHtml`: fragment in the given order, every value escaped, SHA-256 lowercase hex. |
| D22 | `Notifier.email` after `/create`; `NOTIFICATION_SKIPPED` without contact email. |
| D23 | UIBE `OfferService.decide` (CREATED check, CM call, status after success); CM `offerDecision` (REQUESTED check, accept or reject). |
| D24 | `EstablishmentService.consumerSign`: issue, wallet, SIGNED, `signed/true`; failures to FAILED and `signed/false`. |
| D25 | `EstablishmentService.issue`: `{}` is no body; record by aid or by body nid (created if absent); path ids stored; 201; async validation. |
| D26 | `providerValidateAndSign` calls `GET /edc/{own}/vcExchange/{nid}/request`; 404 gives `MISSING_CONTRACT_DATA`, other errors a technical failure. |
| D27 | `service/CredentialValidator`, checks 2 to 7 in order; result and reason stored on the record and in a VALIDATION history entry. |
| D28 | `isDuplicate`: last result SUCCESS or REJECTED (or a validation in progress) gives an IGNORED history entry and the normal response; FAILED keeps the status and allows a retry. |
| D29 | `providerValidateAndSign`: `isValid/true`, own VC, both VCs to the wallet, SIGNED, `signed/true`; rejection: FAILED, `isValid/false`, email. |
| D30 | `Notifier.email` with the subject and message of D30, only on REJECTED. |
| D31 | `EstablishmentService.verify` and `consumerVerify`. |
| D32 | `EstablishmentService.agreementStatus`. |
| D33 | `replaceOffer`, `offerStatusUpdate`. |
| D34 | `agreement`, `agreementFile`, `searchAgreements`. |
| D35 | `CONTRACT_VC_VALIDITY` sets `validUntil`; checked only in validation. |
| D36 | Only the two emails; status events for everything. |
| D37 | `domain/ContractStatus`, `domain/LifecycleAction`, `domain/StateMachine`; E and P transitions in `EstablishmentService`, L in `LifecycleService`. |
| D38 | `ContractStatus.technical()`. |
| D39 | `LifecycleService.reason`: trimmed, required where the table says, at most 1000 characters. |
| D40 | 401 and 403 at UIBE, 409 `invalidTransition` at CM, optimistic locking (`@Version`) for races. |
| D41 | `revocationRequestedBy` set on L4, cleared on L5 and L6, checked by `LifecycleAction.allowed`. |
| D42 | TERMINATE allowed from the established statuses only. |
| D43 | `consentStatus` from the catalogue `consentRequired`; `PUT /contracts/{nid}/consentStatus`; UIBE adds `consentManagerUrl`. |
| D44 | UIBE `OfferService.expire` and `ExpiryScheduler`; CM `reason=EXPIRED`. |
| D45 | Status change and events only. |
| D46 | `events/KafkaConfig` creates the three topics with one partition. |
| D47 | `LifecycleService.onCounterpartyMessage`: recipient filter, own messages ignored, `processed_message` table, sender role check, trigger COUNTERPARTY, no reply. |
| D48 | `COUNTERPARTY_MESSAGE_REJECTED` history entry. |
| D49 | `ContractStore.publishStatus` on every transition, also on creation. |
| D50 | Key = nid, one partition, group `cm-<participantId>`, `auto-offset-reset: earliest`, idempotent consumer. |
| D51 | Kafka on `localhost:19092` (EXTERNAL listener). |
| D52 | One stub service holds all trust services; CM has no keys. |
| D53 | STUB `IssuerController.issue`, `crypto/DataIntegrity`. |
| D54 | STUB `Participants` with the three seeds. |
| D55 | Subject built in `consumerSign`, copied in `providerValidateAndSign`. |
| D56 | Stub overrides and faults; see smoke.sh section 2. |
| D57 | `walletError`, `walletMessage`. |
| D58 | Provider stores both VCs, Consumer stores its own then the Provider VC. |
| D59 | Issue failure gives FAILED `SIGNING_FAILED`, never an HTTP 500. |
| D60 | `domain/ContractRecord`. |
| D61 | `domain/HistoryEntry`, append only, no update or delete endpoint. |
| D62 | `LifecycleService.counts`. |
| D63 | `service/ClauseService`. |
| D64 | One log line per transition with nid and correlation id (`ContractStore.writeTransition`). |
| D65 | `web/ApiKeyFilter`. |
| D66 | UIBE `SecurityConfig` two chains, `MachineApiKeyFilter`. |
| D67 | `build/keycloak/*.json`, Keycloak `KC_HOSTNAME` and `KC_HOSTNAME_BACKCHANNEL_DYNAMIC`. |
| D68 | `hasAuthority(ROLE_contract_manager)`; `UPDATE_CONSENT` removed without `consent_manager`. |
| D69 | Issuer validator and the participant check in `SecurityConfig.converter`. |
| D70 | Stub checks `X-Api-Key: test-edc-key` on `/edc/...`; CM sends it. |
| D71 to D75 | `build/ui/src`: `views/Overview.vue`, `views/Details.vue`, `components/ReasonDialog.vue`, `App.vue`, `labels.ts`. |
| D76 | `build/docker-compose.yml` (project `cm-factory-pso`). |
| D77 | Environment of each service in the compose file. |
| D78 | Stub, realms and compose file in this build. |
| D79 | All asynchronous steps finish in well under a second in the reference environment. |
| D80 | `StubClient` sends `X-Contract-Negotiation-Id`; stub faults and overrides are per nid. |
| D81 | Nothing is deleted; tests can register assets. |
| D82 | `POST /admin/offerExpiry/run` with `asOf`. |
| D83 | Stub seed: directory of C.5 and `asset-seed-01`. |
| D85 | Not built (the gateway stand-in is the factory's); CM ignores `Authorization` and works with or without a key. `pso-check.sh` emulates it. |
| C1 to C6 | C1: duplicates do not change the stored result. C2: credential ids in A.3. C3: `CONNECTOR_CALLBACK` after each callback. C4: P1 and the other connector transitions use trigger CONNECTOR. C5: details show the current status. C6: column labels as the story. |
