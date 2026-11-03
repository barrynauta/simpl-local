# Traceability

Code paths: `CM/` = `contract-manager/src/main/java/eu/simpl/cm/manager/`, `UIBE/` = `contract-ui-backend/src/main/java/eu/simpl/cm/uibe/`, `STUB/` = `stubs/src/main/java/eu/simpl/cm/stubs/`, `COMMON/` = `common/src/main/java/eu/simpl/cm/common/`, `UI/` = `ui/src/`.

Tests: `DIT` = `common/.../proof/DataIntegrityTest`, `SMT` = `contract-manager/.../domain/StateMachineTest`, `CVT` = `.../service/CredentialValidatorTest`, `OHT` = `.../service/OfferHtmlTest`, `PIT` = `.../it/ProviderFlowIT`, `CIT` = `.../it/ConsumerFlowIT`, `UIT` = `contract-ui-backend/.../UibeIT`, `SIT` = `stubs/.../StubIT`, `SMOKE` = `smoke.sh` (live stack), `UI-RUN` = the one-off headless browser run described in `BUILD-LOG.md` (not part of `mvn verify`).

## Stories

| Story | Verdict | Implemented in | Own tests |
|---|---|---|---|
| SIMPL-32509 verify existing usage contract | In | `POST /agreements/{nid}/search`: `EstablishmentService.search` | PIT `search`, SMOKE |
| SIMPL-32511 create usage contract offer | In (at the Consumer per 35526) | `EstablishmentService.create` | CIT `create`, `createErrors` |
| SIMPL-32505 Consumer accepts offer via UI | In | `UI/views/Details.vue`, `UIBE/OfferService.decide`, `EstablishmentService.offerDecision` | UIT `offersAcceptAndCmFailure`, CIT, SMOKE, UI-RUN |
| SIMPL-19927 Consumer stores signed contract in wallet | In (other location out) | `SigningService.consumerSign`, `doConsumerVerify`, `walletError` | CIT, OHT `walletErrorsD57`, live check in BUILD-LOG |
| SIMPL-19933 Provider validates Consumer-signed offer | In | `CredentialValidator`, `SigningService.doProviderValidateAndSign` | CVT, PIT |
| SIMPL-19934 Provider signs | In (async, no "200 Finalized") | `SigningService.doProviderValidateAndSign` | PIT `wholeProviderSideToActiveThenLifecycle` |
| SIMPL-19938 Provider stores in wallet | In (other location out) | same, `walletError` | PIT `missingVcAndTechnicalErrorAndWallet` |
| SIMPL-21928 Consumer signs offer | In | `SigningService.consumerSign` | CIT `signingFailureAndRejectAndExpiry` |
| SIMPL-21948 Consumer validates Provider-signed contract | In | `CredentialValidator`, `SigningService.doConsumerVerify` | CVT, CIT |
| SIMPL-21963 Consumer delivers signed offer to Provider | In (through `/signed` and `vcExchange`) | `SigningService.consumerSign`, `GET /verifications/{nid}/vc`, `STUB/StubController.vcExchange` | PIT, SMOKE |
| SIMPL-21966 Provider notifies Consumer of incorrect signature | In (email delivery out) | `SigningService`, `ContractService.email` | PIT `incorrectlySignedConsumerVcIsRejectedAndNotified`, SMOKE |
| SIMPL-28846 store contract clause documents | In | `CM/web/ClauseController` | PIT `clauseUpload` |
| SIMPL-30959 overview of usage contracts | In | `UI/views/Overview.vue`, `GET /contracts` (UIBE, CM) | UIT, UI-RUN |
| SIMPL-30960 details of a usage contract | In | `UI/views/Details.vue`, `GET /contracts/{nid}` | CIT, UI-RUN |
| SIMPL-31047 Provider updates consent status | In (consent manager out; link only) | `LifecycleService.consent`, `UibeController.enrich`, `Details.vue` | UIT `detailsFilterConsentForUsersWithoutTheRole`, PIT |
| SIMPL-31053 request revocation | In | `LifecycleAction.REQUEST_REVOCATION` | SMT |
| SIMPL-31076 respond to revocation | In | `ACCEPT_REVOCATION`, `DENY_REVOCATION` | SMT `onlyTheOtherPartyAnswersARevocationRequest` |
| SIMPL-31115 Provider suspends | In | `LifecycleAction.SUSPEND`, lifecycle message | PIT, SMOKE, UI-RUN |
| SIMPL-31116 Consumer requests reactivation | In | `REQUEST_REACTIVATION` | CIT, SMT |
| SIMPL-31117 Provider reactivates | In | `REACTIVATE` | SMOKE, SMT |
| SIMPL-31118 Provider terminates | In | `TERMINATE` | SMT |
| SIMPL-34633 aggregated audit data | In (rule package out) | `GET /audit/contractStatusCounts` | PIT `errorsFollowD11AndD12` |
| SIMPL-35440 CM returns VC for nid | In | `GET /verifications/{nid}/vc` | PIT |
| SIMPL-35526 CM prepares contract data for UI | In | `EstablishmentService.create`, `UibeClient` | CIT |
| SIMPL-35584 CM validates and signs (Provider) | In on `/issue` | `EstablishmentService.issue`, `SigningService` | PIT |
| SIMPL-35660 CM signs on Consumer side | In on `PATCH /contractOffers` | `offerDecision`, `consumerSign` | CIT |
| SIMPL-35677 CM verifies on Consumer side | In on `PUT /verifications` | `EstablishmentService.verify` | CIT |
| SIMPL-35692 terminate unsigned offers after X days | In | `UIBE/OfferService.expire`, `ExpiryScheduler` | UIT `expiryRun`, CIT, SMOKE |
| SIMPL-35858 `/isPresent` | Stub contract | `STUB/StubController.callback` | SIT, SMOKE |
| SIMPL-35863 `/isValid` | Stub contract | same | SIT, SMOKE |
| SIMPL-35888 `/vcExchange` | Stub contract | `STUB/StubController.vcExchange` | SMOKE |
| SIMPL-19824 | Out (replaced by 32509; "ACK now, result later" used) | | |
| SIMPL-19829 | Out (replaced by 32511) | | |
| SIMPL-22690 | Out (replaced by 32505; "UI never calls CM" kept) | | |
| SIMPL-21924 | Out (catalogue UI) | | |
| SIMPL-35889 | Out (no acceptance criteria) | | |

## BP07 steps

| Step | Verdict | Implemented in | Tests |
|---|---|---|---|
| BP07.01 request offer | Out; the stub starts it | `STUB/ControlController.start` | SMOKE |
| BP07.02 access policy | Out; stub assumes allowed | | |
| BP07.03 verify existence | In | `EstablishmentService.search` | PIT, live E2 check in BUILD-LOG |
| BP07.04 provide usage contract | Partly | `EstablishmentService.create` | CIT |
| BP07.05 review terms | In | `Details.vue`, UIBE, CM | UI-RUN |
| BP07.06 sign (Consumer) | In | `SigningService.consumerSign` | CIT |
| BP07.07 store (Consumer) | In | same, and `doConsumerVerify` | CIT, SMOKE |
| BP07.08 validate (Provider) | In | `CredentialValidator` | CVT, PIT |
| BP07.09 sign (Provider) | In | `doProviderValidateAndSign` | PIT |
| BP07.10 store (Provider) | In | same | PIT, SMOKE |
| BP07.11 notify Consumer | In through the stub | `STUB/Choreography` steps 4 and 5 | SMOKE |
| BP07.12 confirm (Consumer) | In | `EstablishmentService.verify` | CIT, SMOKE |
| BP10 | Out | | |

## Decisions

Where each decision lives in the code is in `docs/decisions.md`. Tests per decision:

| ID | Tests | ID | Tests | ID | Tests |
|---|---|---|---|---|---|
| D1 | UIT, PIT | D29 | PIT, SMOKE | D57 | OHT, PIT, live |
| D2 | PIT (`wrongParty` on create), CIT | D30 | PIT, SMOKE | D58 | SMOKE |
| D3 | SMOKE | D31 | CIT, SMOKE | D59 | CIT |
| D4 | PIT, CIT | D32 | PIT | D60 | PIT, CIT |
| D5 | PIT | D33 | CIT | D61 | PIT, CIT |
| D6 | PIT, CIT | D34 | PIT, SMOKE | D62 | PIT |
| D7 | UIT | D35 | review only | D63 | PIT |
| D8 | UIT | D36 | review only | D64 | review only |
| D9 | UIT | D37 | SMT, PIT, CIT, SMOKE | D65 | PIT, SMOKE |
| D10 | CIT, UIT | D38 | SMT, PIT | D66 | UIT, SMOKE |
| D11 | PIT | D39 | OHT, PIT | D67 | SMOKE, UI-RUN |
| D12 | PIT, CIT | D40 | UIT, SMOKE | D68 | UIT |
| D13 | PIT, UIT | D41 | SMT | D69 | UIT, SMOKE |
| D14 | live check | D42 | SMT | D70 | SIT |
| D15 | review only | D43 | UIT, PIT | D71 | UI-RUN |
| D16 | PIT | D44 | UIT, CIT, SMOKE | D72 | UI-RUN |
| D17 | SMOKE | D45 | not applicable | D73 | UI-RUN |
| D18 | SMOKE | D46 | live check | D74 | UI-RUN |
| D19 | PIT, live E2 check | D47 | CIT, SMOKE | D75 | UI-RUN (escaped markup) |
| D20 | CIT | D48 | CIT | D76 | compose up |
| D21 | OHT, CIT | D49 | live Kafka read | D77 | compose up |
| D22 | live check (`NOTIFICATION_SKIPPED`) | D50 | review only | D78 | compose up |
| D23 | UIT, CIT | D51 | live Kafka read | D79 | SMOKE timings |
| D24 | CIT | D52 | SIT | D80 | SMOKE (faults per nid) |
| D25 | PIT | D53 | DIT, SIT | D81 | SMOKE |
| D26 | PIT | D54 | DIT, SIT | D82 | UIT, SMOKE |
| D27 | CVT | D55 | PIT, CIT | D83 | SIT |
| D28 | PIT | D56 | CVT, SIT, SMOKE | | |

## Not implemented

| Item | Reason |
|---|---|
| "Specify another secure location" (19927, 19938) | Out of scope by the digest (D57). |
| Audit rule package (34633) | Out of scope (D62). |
| Consent manager itself (31047) | Out of scope; the stub page does nothing (D43). |
| Email delivery (21966) | Out; Notification Service. Only the Kafka message. |
| Catalogue UI, consumption adapter, connector policy engine, billing | Out (scope table). |
| Frontend unit tests | Optional in the brief; replaced by one headless browser run during the build (not repeatable from `mvn verify`). |
| Testcontainers against real Postgres and Kafka in `mvn verify` | Not done: integration tests use H2 in PostgreSQL mode and mocked Kafka. Postgres and Kafka are only exercised by `smoke.sh`. |

## Clarifications after run 1 (run 1.1)

| ID | Implemented in | Own tests |
|---|---|---|
| C1 | `SigningService.recordDuplicate`, `isDuplicate` | PIT `wholeProviderSideToActiveThenLifecycle` (duplicate part) |
| C2 | `QueryService.details` | PIT (credential ids), live check |
| C3 | no change (`ContractService.callback`) | PIT (callbacks verified) |
| C4 | `SigningService` triggers | PIT (P1 and P2 triggers), live check |
| C5 | no change | SMOKE, UI-RUN (run 1) |
| C6 | `UI/views/Overview.vue` | UI build only |
