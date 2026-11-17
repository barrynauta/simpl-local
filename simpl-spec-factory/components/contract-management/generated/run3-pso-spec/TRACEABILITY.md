# Traceability

Code paths are relative to `build/`. Abbreviations: `ES` = `backend/contract-manager/.../service/EstablishmentService.java`,
`LS` = `.../service/LifecycleService.java`, `CV` = `.../service/CredentialValidator.java`, `UI` = `ui/src`.
Tests: `PAT` = `ProviderApiTest`, `CAT` = `ConsumerApiTest`, `SMT` = `StateMachineTest`, `CVT` = `CredentialValidatorTest`,
`OHT` = `OfferHtmlTest`, `UAT` = `UibeApiTest`, `DIT` = `DataIntegrityTest` (stubs), `smoke` = `smoke.sh`, `pso` = `pso-check.sh`.

## Stories

| Story | Scope | Code | Own tests |
|---|---|---|---|
| SIMPL-32509 verify existing contract | In | `ES.search`, `ContractRepository.findOpenDuplicates` | PAT existenceCheckCreatesTheProviderRecordOnce, pso SV05 |
| SIMPL-32511 create offer (fields, 403, 404) | In | `ES.create` | CAT createPreparesTheOffer |
| SIMPL-32505 accept via UI | In | UIBE `OfferService.decide`, CM `ES.offerDecision`, `UI/views/Details.vue` | UAT offerFieldsAndDecision, CAT acceptSignsAndVerificationActivates, smoke 1 |
| SIMPL-19927 Consumer stores in wallet | In (no other location) | `ES.consumerSign`, `ES.consumerVerify`, `walletError` | CAT rejectExpiryAndWalletFailure, smoke 1 (wallet contents) |
| SIMPL-19933 Provider validates | In | `CV`, `ES.providerValidateAndSign` | CVT (all), PAT rejectedConsumerVc..., technicalErrorKeepsStatus... |
| SIMPL-19934 Provider signs | In | `ES.providerValidateAndSign` | PAT providerValidatesSignsAndFinalises |
| SIMPL-19938 Provider stores in wallet | In (no other location) | `ES.providerValidateAndSign` | PAT providerValidatesSigns..., smoke 1 |
| SIMPL-21928 Consumer signs | In | `ES.consumerSign` | CAT acceptSigns... |
| SIMPL-21948 Consumer validates | In | `CV`, `ES.consumerVerify` | CVT consumerAlsoComparesTheHash, CAT acceptSigns... |
| SIMPL-21963 Consumer delivers signed offer | In | `ES.providerValidateAndSign` (vcExchange), stub `EdcController.vcExchange` | PAT providerValidatesSigns..., smoke 1 |
| SIMPL-21966 notify incorrect signature | In (no delivery) | `Notifier`, `ES.providerValidateAndSign` | PAT rejectedConsumerVcFailsTheProviderAndNotifies, smoke 2 |
| SIMPL-28846 clause upload | In | `ClauseService`, `ContractController.upload` | PAT clauseUpload |
| SIMPL-30959 overview | In | `LS.list`, UIBE `ContractsController.list`, `UI/views/Overview.vue` | UAT detailsFilterConsentAction (counterparty mapping); UI checked by hand (BUILD-LOG) |
| SIMPL-30960 details | In | `ContractViews.details`, UIBE `enrich`, `UI/views/Details.vue` | PAT, CAT (details), UAT |
| SIMPL-31047 consent link | In (no consent manager) | `LS.consent`, UIBE `enrich`, `Details.vue` link | PAT consentAndWrongParty, UAT detailsFilterConsentAction |
| SIMPL-31053 request revocation | In | `LifecycleAction.REQUEST_REVOCATION`, `LS.act` | SMT lifecycleTable |
| SIMPL-31076 answer revocation | In | `ACCEPT_REVOCATION`, `DENY_REVOCATION` | SMT lifecycleTable |
| SIMPL-31115 suspend | In | `SUSPEND` | PAT providerValidatesSigns... (lifecycle part), smoke 3 |
| SIMPL-31116 request reactivation | In | `REQUEST_REACTIVATION`, counterparty message | PAT (counterparty message), SMT |
| SIMPL-31117 reactivate | In | `REACTIVATE` | PAT, smoke 3 |
| SIMPL-31118 terminate | In | `TERMINATE` | SMT lifecycleTable |
| SIMPL-34633 audit counts | In (no rule package) | `LS.counts` | PAT providerValidatesSigns... (counts) |
| SIMPL-35440 GET VC | In | `ES.ownCredential` | PAT, CAT |
| SIMPL-35526 prepare offer at Consumer | In | `ES.create`, `UibeClient`, `Notifier` | CAT createPreparesTheOffer, uibeFailureKeepsNoRecord, pso SV04 |
| SIMPL-35584 validate and sign (on `/issue`) | In | `ES.issue` | PAT providerValidatesSigns..., psoUnknownIdsGive404, pso TC_CB_04, SV03 |
| SIMPL-35660 sign at Consumer | In | `ES.offerDecision`, `ES.consumerSign` | CAT acceptSigns..., rejectExpiry... |
| SIMPL-35677 verify at Consumer (on `PUT`) | In | `ES.verify` | CAT acceptSigns..., PAT consentAndWrongParty (wrong party) |
| SIMPL-35692 offer expiry | In | UIBE `OfferService.expire`, `ExpiryScheduler`, CM `reason=EXPIRED` | UAT expiryRun, CAT rejectExpiry..., smoke 4 |
| SIMPL-35858 `/isPresent` | Stub contract | stub `EdcController` | PAT (callback verified), smoke |
| SIMPL-35863 `/isValid` | Stub contract | stub `EdcController` | PAT, CAT, smoke |
| SIMPL-35888 `/vcExchange` | Stub contract | stub `EdcController.vcExchange` | smoke 1, 2 |
| SIMPL-19824, 19829, 22690 | Out (replaced) | Only the parts named in digest 3.2 | |
| SIMPL-21924 | Out (catalogue UI) | | |
| SIMPL-35889 | Out (no criteria) | | |

## BP07 steps

| Step | Code | Tests |
|---|---|---|
| BP07.01, 02 | Out; stub `POST /_control/negotiations` starts the flow and assumes "allowed" | smoke |
| BP07.03 verify existence | `ES.search` | PAT existenceCheck... |
| BP07.04 provide offer | `ES.create` (data), stub (transport) | CAT createPreparesTheOffer |
| BP07.05 review terms | `UI/views/Details.vue`, UIBE `ContractsController.details` | UAT, by hand |
| BP07.06 sign (Consumer) | `ES.offerDecision`, `ES.consumerSign` | CAT |
| BP07.07 store (Consumer) | `ES.consumerSign` (wallet) | CAT, smoke |
| BP07.08 validate (Provider) | `CV`, `ES.providerValidateAndSign` | CVT, PAT |
| BP07.09 sign (Provider) | `ES.providerValidateAndSign` | PAT |
| BP07.10 store (Provider) | `ES.providerValidateAndSign` (both VCs) | PAT, smoke |
| BP07.11 notify Consumer | callbacks to the stub, stub relays | smoke |
| BP07.12 confirm (Consumer) | `ES.verify`, `ES.consumerVerify` | CAT, smoke |
| BP10 | Out | |

## Decisions

The implementation of every decision is listed in `docs/decisions.md`. Tests per decision:

| Decision | Tests |
|---|---|
| D2 wrong party | PAT createRunsAtTheConsumerOnly, consentAndWrongParty |
| D8, D9, D10 offer fields and statuses | UAT offerFieldsAndDecision, CAT rejectExpiry... (`SIGNED` refused) |
| D11 problem bodies | PAT psoHealthNeedsKey, psoUnauthorisedComesFirst, pathFormatBeforeRecordAndMediaType; pso SV03 to SV07 |
| D12 404 for unknown ids | PAT psoUnknownIdsGive404; pso TC_CB_02, 04, 06, SV07 |
| D13, D65 health and API key | PAT psoHealthNeedsKey; pso TC_CB_07 |
| D16, D84 id formats and order of checks | PAT pathFormatBeforeRecordAndMediaType, psoUnauthorisedComesFirst, psoUnknownIdsGive404 |
| D19 | PAT existenceCheckCreatesTheProviderRecordOnce |
| D20, D21, D22 | CAT createPreparesTheOffer, uibeFailureKeepsNoRecord; OHT |
| D23 | UAT offerFieldsAndDecision, CAT acceptSigns... |
| D24, D57, D59 | CAT acceptSigns..., rejectExpiryAndWalletFailure |
| D25, D26, D29, D30 | PAT providerValidatesSigns..., rejectedConsumerVc..., technicalErrorKeepsStatus... |
| D27 | CVT (each check and the order) |
| D28, C1 | PAT providerValidatesSigns... (duplicate), technicalErrorKeepsStatusAndAllowsRetry |
| D31 | CAT acceptSignsAndVerificationActivates |
| D32 | PAT providerValidatesSigns... (FINALIZED, then 400) |
| D33 | CAT rejectExpiryAndWalletFailure (update) |
| D34 | PAT providerValidatesSigns... (agreement, file, search) |
| D37, D38, D42 | SMT (all) |
| D39, D40, D41 | SMT, PAT (missing reason 400, wrong party 409) |
| D43 | PAT consentAndWrongParty, UAT detailsFilterConsentAction |
| D44, D82 | UAT expiryRun, CAT rejectExpiry..., smoke 4 |
| D47, D48 | PAT (counterparty message applied, misfit rejected) |
| D53, D54, Appendix E | DIT (keys, DIDs, worked vector, tamper) |
| D63 | PAT clauseUpload |
| D66 to D69 | UAT tokensAndRoles, machineOperationsNeedTheKey, detailsFilterConsentAction; smoke 5 |
| D71 to D75 | UI tests (`ui/src/labels.test.ts`) and a manual browser check (BUILD-LOG) |
| D76, D77, D83 | the compose file; smoke and pso run against it |
| D85 | pso (gateway emulated) |
| Other rows (D1, D3 to D7, D14, D15, D18, D35, D36, D45, D46, D49 to D52, D55, D56, D58, D60 to D62, D64, D70, D78 to D81) | covered indirectly by smoke and the integration tests; no dedicated test |

## Not implemented

| Item | Reason |
|---|---|
| D85 gateway stand-in | The brief says not to build it; `pso-check.sh` emulates it. |
| "Specify another secure location" (19927, 19938) | Out per digest 3.1. |
| Consent manager, audit rule package, email delivery, clause retrieval | Out per digest 3.1. |
| UI end-to-end tests | Optional in the brief; the UI was checked once in a browser instead. |
