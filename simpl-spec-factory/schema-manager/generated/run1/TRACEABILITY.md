# Traceability

Each in-scope story and each decision D1 to D63, mapped to the code and to the own tests.

Code paths are relative to `backend/src/main/java/eu/simpl/schemamanager/` or start with `ui/`.
Test abbreviations:

| Abbreviation | Test |
|---|---|
| `IT.<method>` | `backend/src/test/java/eu/simpl/schemamanager/api/ApiIT.java` (Testcontainers PostgreSQL, mocked JWT, VM client and mail) |
| `VT.<method>` | `backend/src/test/java/eu/simpl/schemamanager/validation/SchemaValidatorTest.java` |
| `NT.<method>` | `backend/src/test/java/eu/simpl/schemamanager/web/NegotiationAndParamsTest.java` |
| `SMOKE` | `smoke.sh` against the running stack |
| `UIC` | `ui-check/check.mjs` (Playwright against the running stack) |

"500 paths" in some stories (an unexpected server error) are handled by `web/ErrorAdvice.other`
and by transaction rollback, but they are not tested: a test cannot force them (D61).

## Stories

| Story | Scope | Code | Own tests |
|---|---|---|---|
| SIMPL-13343 List schemas (API) | In | `web/SchemaController.list`, `service/SchemaService.list`, `persistence/SchemaRepository.listConcepts`, `web/Params` | IT.createVersionListAndRetrieve, IT.paginationEnvelope, IT.missingTokenGives401ProblemJson, IT.roleMatrix, NT.queryParameterRules, SMOKE |
| SIMPL-13345 Versions of a schema (API) | In | `SchemaController.versions`, `SchemaService.versions` | IT.createVersionListAndRetrieve, IT.newVersionValidation (bad name 400) |
| SIMPL-13346 Versions of a schema (UI) | In | `ui/src/views/SchemaDetails.vue` | UIC |
| SIMPL-13347 Latest content (API) | In | `SchemaController.latest` | IT.createVersionListAndRetrieve, SMOKE |
| SIMPL-13353 Create schema (API) | In | `SchemaController.create`, `SchemaService.create`, `validation/SchemaValidator` | IT.createVersionListAndRetrieve, IT.createValidation, IT.searchSchemasHaveOneGlobalSlot (409), SMOKE |
| SIMPL-13355 Create schema (UI) | In | `ui/src/views/CreateSchema.vue` | UIC |
| SIMPL-13357 New version (API) | In, in part replaced (D14, D15) | `SchemaController.newVersion`, `SchemaService.newVersion` | IT.createVersionListAndRetrieve, IT.newVersionValidation, SMOKE |
| SIMPL-13358 New version (UI) | In, SemVer field replaced (D50, D55) | `ui/src/views/NewVersion.vue` | UIC |
| SIMPL-13360 Revoke (API) | In | `SchemaController.patch`, `SchemaService.changeStatus` | IT.slotRuleForResource |
| SIMPL-13361 Revoke (UI) | In | `SchemaList.vue` (`confirm-dialog`, revoke) | UIC |
| SIMPL-13364 Version content (API) | In | `SchemaController.version` | IT.createVersionListAndRetrieve, SMOKE |
| SIMPL-13365 Version download (UI) | In | `SchemaDetails.vue` `download` | UIC |
| SIMPL-14354 Retain revoked (API) | In | No delete; GETs serve REVOKED concepts | IT.slotRuleForResource, IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-14701 Retain revoked (UI) | Partly (D21) | Revoked versions stay listed and downloadable in the UI | UIC (revoked schema still listed) |
| SIMPL-14703 Syntax and default properties | In | `SchemaValidator.syntax`, `defaultProperties` | VT.syntaxErrorHasPrefixAndPosition, VT.missingNodeShapeIsASyntaxError, VT.missingDefaultPropertyListsEachMissingIri, IT.createValidation, IT.newVersionValidation |
| SIMPL-14709 Notify on new schema | In (RESOURCE only) | `notify/ProviderMailNotifier` | IT.providerMailOnPublishAndRevoke, SMOKE |
| SIMPL-14712 Notify on new version | In (RESOURCE only) | `ProviderMailNotifier` | IT.providerMailOnPublishAndRevoke, SMOKE |
| SIMPL-14791 Notify on revocation | In (RESOURCE only) | `ProviderMailNotifier` | IT.providerMailOnPublishAndRevoke |
| SIMPL-14795 Syntax error message (API) | In | `SchemaValidator.SYNTAX_PREFIX` | VT.syntaxErrorHasPrefixAndPosition, IT.createValidation |
| SIMPL-14796 Missing default property message (API) | In (prefix per D33) | `SchemaValidator.DEFAULT_PREFIX` | VT.missingDefaultPropertyListsEachMissingIri, IT.createValidation |
| SIMPL-14797 Semantic error message (API) | In | `SchemaValidator.SEMANTIC_PREFIX` | VT.unknownLocalVocabularyIsASemanticError, IT.createValidation |
| SIMPL-15837 Schema list (UI) | In | `ui/src/views/SchemaList.vue` | UIC |
| SIMPL-17066 Access control Tier 1 | In | `security/SecurityConfig` | IT.roleMatrix, IT.missingTokenGives401ProblemJson, SMOKE, UIC |
| SIMPL-19992 Webhook subscriptions | In | `web/WebhookController`, `persistence/WebhookRepository` | IT.webhookSubscriptionsAndDelivery, NT.webhookUrlRules |
| SIMPL-20067 Download latest (UI) | In | `SchemaList.vue` `download` | UIC |
| SIMPL-20528 Initial version number | In | `SchemaService.create` (version 1) | IT.createVersionListAndRetrieve |
| SIMPL-20529 Change status (API) | In | `SchemaController.patch`, `SchemaService.changeStatus` | IT.slotRuleForResource, IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-20530 Change status (UI) | In | `SchemaList.vue` (publish, revoke) | UIC |
| SIMPL-20534 Next version number | In | `SchemaService.newVersion` (`max + 1`) | IT.createVersionListAndRetrieve (version field ignored) |
| SIMPL-20686 Status on create | In | `SchemaService.create` | IT.slotRuleForResource |
| SIMPL-20770 No version field in UI | In | `NewVersion.vue` | UIC |
| SIMPL-22407 Semantic validation | In | `SchemaValidator.semantic`, `validation/VocabularyClient` | VT.unknownTermIsASemanticError, VT.knownTermPasses, VT.allErrorsAreCollected, VT.externalPrefixGivesWarning, VT.vocabularyManagerOutageGives500, IT.createValidation |
| SIMPL-24172 Syntax error message (UI) | In | `CreateSchema.vue`, `NewVersion.vue` show `detail` | UIC (syntax error on create) |
| SIMPL-24173 Missing default property message (UI) | In | same | Not separately checked in UIC; same code path as SIMPL-24172 |
| SIMPL-24174 Semantic error message (UI) | In | same | Not separately checked in UIC; same code path as SIMPL-24172 |
| SIMPL-24175 Access control Tier 2 (webhooks) | Partly (D7) | `/webhooks/**` needs a valid token, no role | IT.roleMatrix, IT.missingTokenGives401ProblemJson |
| SIMPL-26105 UUID and Version default properties | In | `SM_DEFAULT_PROPERTIES` default | VT.missingDefaultPropertyListsEachMissingIri |
| SIMPL-28815 Provider adds billing schema | **Out** (digest 2.2) | Not implemented: catalogue wizard, not schema management | None |
| SIMPL-31098 Publish federated search schema | In | `SchemaService.create`, `changeStatus` (200, not 201, D18) | IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-32258 List schemas with schemaType | In | `SchemaController.list` | IT.searchSchemasHaveOneGlobalSlot, IT.createVersionListAndRetrieve |
| SIMPL-32436 Retain revoked, RESOURCE only | Partly (D21, D22) | SEARCH revoke keeps the concept, status REVOKED | IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-32497 Retain revoked (UI) | Partly | List shows REVOKED SEARCH rows with the SEARCH filter | UIC (filter only) |
| SIMPL-32786 Versions list with schemaType (API) | In | `SchemaController.versions` | IT.createVersionListAndRetrieve, IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-32787 Versions list with schemaType (UI) | In | `SchemaDetails.vue` columns | UIC |
| SIMPL-32788 Latest content with schemaType (API) | In | `SchemaController.latest` | IT.createVersionListAndRetrieve |
| SIMPL-32790 Schema list with schemaType (UI) | In | `SchemaList.vue` filter and column | UIC |
| SIMPL-32797 Version download with schemaType (UI) | In (D57) | `SchemaDetails.vue` uses the page type | UIC |
| SIMPL-32798 Version details (API) | In (D10) | `SchemaController.version` (JSON metadata) | IT.createVersionListAndRetrieve |
| SIMPL-32800 Create schema with schemaType (UI) | In | `CreateSchema.vue` | UIC |
| SIMPL-32801 Status on create with schemaType | In | `SchemaService.create` | IT.slotRuleForResource, IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-32802 Create schema with schemaType (API) | In | `SchemaController.create` | IT.searchSchemasHaveOneGlobalSlot, IT.createValidation |
| SIMPL-32807 New version with schemaType (API) | In | `SchemaController.newVersion` | IT.newVersionValidation, IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-32808 New version with schemaType (UI) | In | `NewVersion.vue` | UIC |
| SIMPL-33537 API for federated search schema | In | `schemaType` across `SchemaController`, `SchemaService` | IT.searchSchemasHaveOneGlobalSlot |
| SIMPL-33694 Notify only for RESOURCE | In | `ProviderMailNotifier.onEvent` | IT.searchSchemasHaveOneGlobalSlot (no SEARCH mail) |

## Decisions

See `docs/decisions.md` for how each one is implemented.

| D | Code | Own tests |
|---|---|---|
| D1 | `application.yml` context path | SMOKE |
| D2 | `SchemaService.conceptUri`, `versionUri` | IT.createVersionListAndRetrieve, IT.searchSchemasHaveOneGlobalSlot, SMOKE |
| D3 | `SecurityConfig` | IT.missingTokenGives401ProblemJson, SMOKE |
| D4 | `SecurityConfig.roles` | IT (all tests use the converter) |
| D5 | `SecurityConfig.filterChain` | IT.roleMatrix, SMOKE |
| D6 | `SecurityConfig` | IT.missingTokenGives401ProblemJson |
| D7 | `SecurityConfig` | IT.roleMatrix, IT.webhookSubscriptionsAndDelivery |
| D8 | `web/Negotiation` | NT.jsonAcceptRules, NT.contentAcceptRules, NT.mediaTypeChecks, IT.createValidation, IT.slotRuleForResource |
| D9 | `SchemaController.latest` | IT.createVersionListAndRetrieve, SMOKE |
| D10 | `SchemaController.version` | IT.createVersionListAndRetrieve |
| D11 | `V1__init.sql`, `SchemaService.find` | IT.searchSchemasHaveOneGlobalSlot, IT.createVersionListAndRetrieve |
| D12 | `Params.isPascal`, `SchemaService.create` | NT.nameRules, IT.createValidation, IT.searchSchemasHaveOneGlobalSlot |
| D13 | `Params.resourceTypeFilter`, `SchemaController.create` | NT.queryParameterRules, IT.createValidation, IT.searchSchemasHaveOneGlobalSlot |
| D14 | `SchemaService.create`, `newVersion` | IT.createVersionListAndRetrieve |
| D15 | `SchemaService.newVersion` | IT.createVersionListAndRetrieve |
| D16 | `SchemaRepository.lockSlot`, partial unique index, `SchemaService` | IT.slotRuleForResource, IT.searchSchemasHaveOneGlobalSlot. Concurrency is not tested by a parallel test |
| D17 | `SchemaService.create` | IT.searchSchemasHaveOneGlobalSlot |
| D18 | `SchemaService.changeStatus` | IT.slotRuleForResource, IT.searchSchemasHaveOneGlobalSlot |
| D19 | `SchemaService.INITIAL_CHANGELOG` | IT.createVersionListAndRetrieve |
| D20 | `SchemaController.create`, `newVersion`, `SchemaService.newVersion` | IT.createValidation, IT.newVersionValidation |
| D21 | No delete | IT.createVersionListAndRetrieve, IT.searchSchemasHaveOneGlobalSlot |
| D22 | `SchemaService.changeStatus` | IT.searchSchemasHaveOneGlobalSlot |
| D23 | Controllers and `SchemaService` order | IT.roleMatrix, IT.newVersionValidation |
| D24 | `SchemaController.listItem` | IT.createVersionListAndRetrieve |
| D25 | `SchemaController.versionItem` | IT.createVersionListAndRetrieve |
| D26 | `Params`, `SchemaRepository`, `SchemaService.versions` | IT.paginationEnvelope, IT.createVersionListAndRetrieve, NT.queryParameterRules |
| D27 | `SchemaController.list` | IT.searchSchemasHaveOneGlobalSlot, NT.queryParameterRules |
| D28 | `SchemaController.create`, `newVersion` | IT.createVersionListAndRetrieve |
| D29 | `SchemaController.multipart`, `file` | IT (all creates), SMOKE |
| D30 | `SchemaValidator.syntax` | VT.syntaxErrorHasPrefixAndPosition, VT.missingNodeShapeIsASyntaxError, VT.emptyDocumentIsASyntaxError |
| D31 | `Settings.defaultPropertyIris` | VT.missingDefaultPropertyListsEachMissingIri, VT.searchSchemasNeedNoDefaultProperties |
| D32 | `SchemaValidator.defaultProperties` | VT.missingDefaultPropertyListsEachMissingIri, IT.createVersionListAndRetrieve (byte for byte) |
| D33 | `SchemaValidator` prefixes | VT, IT.createValidation |
| D34 | `SchemaValidator.isLocal`, `isWellKnown`, `vocabularyName` | VT.vocabularyNameStopsAtHashOrSlash, VT.externalPrefixGivesWarning |
| D35 | `SchemaValidator.semantic` | VT.unknownLocalVocabularyIsASemanticError, VT.unknownTermIsASemanticError, VT.knownTermPasses, VT.allErrorsAreCollected, VT.externalPrefixGivesWarning |
| D36 | `VocabularyClient`, `SchemaValidator.loadVocabulary` | VT.vocabularyManagerOutageGives500, VT.validDocumentPassesWithoutWarningsOrVocabularyCalls; checked by hand against the stack with the stub stopped |
| D37 | `SchemaService.create`, `newVersion` | IT.createValidation, IT.newVersionValidation |
| D38 | (no bus) | n/a |
| D39 | `ProviderMailNotifier` | IT.providerMailOnPublishAndRevoke, SMOKE |
| D40 | `ProviderMailNotifier.subject`, `body` | IT.providerMailOnPublishAndRevoke, SMOKE |
| D41 | `WebhookController` | IT.webhookSubscriptionsAndDelivery, NT.webhookUrlRules |
| D42 | `SchemaEvent`, `SchemaService` | IT.webhookSubscriptionsAndDelivery |
| D43 | `WebhookNotifier.payload` | IT.webhookSubscriptionsAndDelivery |
| D44 | `WebhookNotifier.attempt` | IT.webhookSubscriptionsAndDelivery (delivery); retries not tested automatically |
| D45 | `ui/src/router.ts`, `ui/src/auth.ts` | UIC |
| D46 | `ui/src/views/*.vue`, `ui/src/App.vue` | UIC |
| D47 | `ProviderMailNotifier.onEvent` | IT.searchSchemasHaveOneGlobalSlot |
| D48 | `ui/src/auth.ts`, views | UIC |
| D49 | `CreateSchema.vue` | UIC |
| D50 | `NewVersion.vue` | UIC |
| D51 | `SchemaList.vue` | UIC |
| D52 | `SchemaList.vue` | UIC (409 path) |
| D53 | `SchemaList.vue` | UIC (headers, filters); paging buttons not exercised |
| D54 | `SchemaDetails.vue` | UIC |
| D55 | `NewVersion.vue` | UIC |
| D56 | `SchemaList.vue` | UIC |
| D57 | `SchemaDetails.vue` | UIC |
| D58 | `V1__init.sql`, `SchemaRepository` | IT (PostgreSQL in Testcontainers) |
| D59 | `keycloak/realm-simpl.json`, `SecurityConfig.jwtDecoder` | SMOKE, UIC |
| D60 | `vocabulary-stub/`, `docker-compose.yml`, `config/Settings` | SMOKE; stub checked by hand |
| D61 | (test-side convention) | n/a |
| D62 | `SecurityConfig` (permitAll), `ErrorAdvice.notFound` | IT.outOfScopePathsAnswer404 |
| D63 | `web/Problems` | IT.missingTokenGives401ProblemJson, IT.roleMatrix, IT.createValidation |
