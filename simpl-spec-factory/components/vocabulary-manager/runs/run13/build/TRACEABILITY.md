# Traceability: Vocabulary Manager, run 13

Every in-scope story and acceptance criterion, and every decision, mapped to the code and the build's own tests. Acceptance criteria without an id in the story are numbered AC1, AC2, ... in the order of their Given/When/Then blocks.

Path abbreviations:

- `BE/` = `backend/src/main/java/eu/simpl/vocabularymanager/`
- `BT/` = `backend/src/test/java/eu/simpl/vocabularymanager/`
- `UI/` = `ui/src/`, `UT/` = `ui/tests/`
- Smoke = `smoke.sh` (finish line, runs against the stack)

## Stories and acceptance criteria

| Story / AC | Code | Own tests | Status |
|---|---|---|---|
| SIMPL-18161 AC1 list with rows | `UI/views/VocabularyList.vue`, `BE/api/VocabularyController.java#getVocabularies` | `UT/list.spec.ts` (decided columns), `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18161 AC2 list load error | `UI/views/VocabularyList.vue`, `UI/messages.ts` (D34, D72) | `UT/list.spec.ts` (one alert with load error) | done |
| SIMPL-18162 AC1 200 | `BE/api/VocabularyController.java#getVocabularies`, `BE/store/VocabularyRepository.java#list` | `BT/VocabularyFlowIntegrationTest`, `BT/ApiEdgeCasesIntegrationTest#listFiltersSortAndPaging`, smoke | done |
| SIMPL-18162 AC2 400 | `BE/api/QueryParameters.java`, `BE/api/RequestRules.java` | `BT/ApiEdgeCasesIntegrationTest#queryParameterRules`, `BT/api/RequestRulesTest` | done |
| SIMPL-18162 AC3 403 | `BE/web/RequestGateFilter.java`, `BE/web/OperationCatalog.java` | `BT/VocabularyFlowIntegrationTest#accessRules` | done |
| SIMPL-18162 AC4 500 | `BE/web/ApiExceptionHandler.java#handleUnexpected`, `#handleStoreError` | `BT/StoreDownIntegrationTest` (503 path, D44); 500 body shape unit-covered through `ProblemWriter` | done |
| SIMPL-18163 AC1 filter | `UI/views/VocabularyList.vue` (Name contains, Vocabulary status, Created on) | `UT/list.spec.ts` | done |
| SIMPL-18163 AC2 sort columns | `UI/views/VocabularyList.vue` (D23, D50, D163) | `UT/list.spec.ts` (reverses active sort column, D50) | done |
| SIMPL-18164 AC1 filters | `BE/store/VocabularyRepository.java#list` (FILTER in store, D85), `BE/api/RequestRules.java#dateFilter` | `BT/ApiEdgeCasesIntegrationTest#listFiltersSortAndPaging`, `BT/api/RequestRulesTest#dateFilters` | done |
| SIMPL-18164 AC2 empty result | same | `BT/ApiEdgeCasesIntegrationTest#queryParameterRules` (page beyond last) | done |
| SIMPL-18164 AC3 paging | `BE/api/Representations.java#page` | `BT/api/RepresentationsTest`, `BT/ApiEdgeCasesIntegrationTest` | done |
| SIMPL-18164 AC4 invalid parameters | `BE/api/QueryParameters.java`, `BE/api/RequestRules.java` | `BT/ApiEdgeCasesIntegrationTest#queryParameterRules` | done |
| SIMPL-18165 AC1 download newest | `UI/views/VocabularyList.vue` ('Download vocabulary'), `UI/api.ts`, `BE/api/VocabularyController.java#downloadVocabulary` | `UT/api.spec.ts` (filename), `BT/VocabularyFlowIntegrationTest`, smoke | done |
| SIMPL-18166 AC1 version details | `BE/api/VocabularyController.java#getVocabularyVersion`, `#downloadVocabularyVersion` | `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18166 AC2 version not found | `BE/error/Problems.java#versionNotFound` | `BT/ApiEdgeCasesIntegrationTest#versionStatusAndNotFoundTexts` | done |
| SIMPL-18167 AC1 history page | `UI/views/VocabularyDetails.vue`, `UI/router.ts` | `UT/details.spec.ts` | done |
| SIMPL-18168 AC1 version list | `BE/api/VocabularyController.java#getVocabularyVersions` | `BT/VocabularyFlowIntegrationTest`, smoke | done |
| SIMPL-18168 AC2 not found | `BE/error/Problems.java#vocabularyNotFound` | `BT/ApiEdgeCasesIntegrationTest#queryParameterRules` | done |
| SIMPL-18169 AC1 creation form | `UI/views/CreateVocabulary.vue` | `UT/createForm.spec.ts` (decided fields) | done |
| SIMPL-18169 AC2 input validation | `UI/validation.ts`, `UI/views/CreateVocabulary.vue` (D37, D62, D101) | `UT/createForm.spec.ts`, `UT/components.spec.ts` | done |
| SIMPL-18170 AC1 create, version 1 ACTIVE | `BE/service/VocabularyService.java#create`, `BE/store/VocabularyRepository.java#insertVocabulary` | `BT/VocabularyFlowIntegrationTest`, smoke | done |
| SIMPL-18170 AC2 retrieve created | `BE/api/VocabularyController.java#getVocabulary` | `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18170 AC3 invalid data, nothing created | `BE/api/VocabularyController.java#createVocabulary`, `BE/validation/*` (D8) | `BT/VocabularyFlowIntegrationTest#validationReportAndWarnings` (list with name filter stays empty) | done |
| SIMPL-18172 AC1 pre-filled form | `UI/views/NewVersion.vue` (D33, D46 line) | `UT/newVersionForm.spec.ts` | done |
| SIMPL-18172 AC2 create version | `UI/views/NewVersion.vue`, `BE/service/VocabularyService.java#createVersion` | `UT/newVersionForm.spec.ts`, `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18172 AC3 unexpected error | `UI/errors.ts`, `UI/views/NewVersion.vue` (D60) | `UT/newVersionForm.spec.ts` (500 with detail) | done |
| SIMPL-18173 AC1 new version | `BE/service/VocabularyService.java#createVersion` | `BT/VocabularyFlowIntegrationTest`, `BT/ConcurrencyIntegrationTest`, smoke | done |
| SIMPL-18173 AC2 validation feedback | `BE/validation/ContentValidator.java` | `BT/VocabularyFlowIntegrationTest#validationReportAndWarnings` | done |
| SIMPL-18173 AC3 rejected (unknown vocabulary) | `BE/service/VocabularyService.java#get` | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites` | done |
| SIMPL-18173 AC4 insufficient permissions | `BE/web/RequestGateFilter.java` | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites`, smoke | done |
| SIMPL-18173 AC5 rejected (too large, 413) | `BE/api/MultipartForm.java`, multipart limits in `application.properties` | `BT/ApiEdgeCasesIntegrationTest#sizeLimits` | done |
| SIMPL-18173 AC6 unsupported format (415) | Spring MVC `consumes`, `BE/web/ApiExceptionHandler.java#handleMediaType` | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites` | done |
| SIMPL-18174 AC1 status change UI | `UI/views/VocabularyList.vue` (Deprecate/Activate, D60) | `UT/list.spec.ts` (changes the status) | done |
| SIMPL-18174 AC2 not found | `UI/views/VocabularyList.vue` (D72 text) | `UT/list.spec.ts` (missing vocabulary) | done |
| SIMPL-18174 AC3 update error | `UI/views/VocabularyList.vue`, `UI/messages.ts` | covered by message tests in `UT/components.spec.ts` | done |
| SIMPL-18175 AC1 status update | `BE/service/VocabularyService.java#setStatus`, `BE/store/VocabularyRepository.java#setVocabularyStatus` | `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18175 AC2 not found | same | `BT/ApiEdgeCasesIntegrationTest#versionStatusAndNotFoundTexts` (case-sensitive path, D54) | done |
| SIMPL-18175 AC3 invalid status | `BE/api/VocabularyController.java#statusBody` | `BT/ApiEdgeCasesIntegrationTest#versionStatusAndNotFoundTexts` | done |
| SIMPL-18176 AC1 authentication | `UI/auth.ts` (login required) | `UT/components.spec.ts` | done |
| SIMPL-18176 AC2 display deprecated versions | `UI/views/VocabularyDetails.vue` | `UT/details.spec.ts` | done |
| SIMPL-18176 AC3 metadata of deprecated versions | `UI/views/VocabularyDetails.vue` (version panel) | `UT/details.spec.ts` | done |
| SIMPL-18178 AC1 deprecation warning | `UI/views/VocabularyList.vue`, `UI/views/VocabularyDetails.vue` (D36, D72, D115) | `UT/list.spec.ts`, `UT/details.spec.ts` | done |
| SIMPL-18180 AC1 check on upload | `UI/turtleCheck.ts`, `UI/components/FileField.vue` (D13, D60, D76) | `UT/turtleCheck.spec.ts`, `UT/components.spec.ts` | done |
| SIMPL-18180 AC2 check on submit (create) | `BE/validation/TurtleSyntax.java`, `SemanticRules.java` | `BT/validation/*Test`, `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18180 AC3 check on submit (version) | same, `BE/service/VocabularyService.java#validate` | same | done |
| SIMPL-18180 AC4 successful validation | `UI/views/*` notification (D14) | `UT/createForm.spec.ts` | done |
| SIMPL-18180 AC5 syntax errors | `BE/validation/TurtleSyntax.java`, `UI/components/ValidationReport.vue` | `BT/validation/TurtleSyntaxTest`, `UT/createForm.spec.ts` | done |
| SIMPL-18180 AC6 structure (semantic) errors | `BE/validation/SemanticRules.java` | `BT/validation/SemanticRulesTest` | done |
| SIMPL-18180 AC7 multiple errors | `BE/validation/ContentValidator.java#report` (all errors, file order, cap 1000) | `BT/validation/ContentValidatorTest` | done |
| SIMPL-18180 AC8 errors block submission | `UI/components/FileField.vue` (D62), backend 400 stores nothing | `UT/components.spec.ts`, `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18181 AC1, AC2 syntax via API | `BE/validation/TurtleSyntax.java` | `BT/validation/TurtleSyntaxTest`, `BT/VocabularyFlowIntegrationTest#validationReportAndWarnings` | done |
| SIMPL-18182 AC1 to AC3 semantics via UI | `UI/components/ValidationReport.vue`, `WarningsPanel.vue` (D90, D109) | `UT/createForm.spec.ts`, `UT/details.spec.ts` | done |
| SIMPL-18183 AC1, AC2 semantics via API | `BE/validation/SemanticRules.java`, `ContentValidator.java` | `BT/validation/SemanticRulesTest`, `ContentValidatorTest` | done |
| SIMPL-18184 AC1 to AC3 reference status check | `BE/validation/ReferenceAnalyzer.java`, `ContentValidator.java#checkInternal`, `BE/store/VocabularyRepository.java#referenceState` (one query, Q5) | `BT/validation/ReferenceAnalyzerTest`, `ContentValidatorTest` | done (warning, not block, D11) |
| SIMPL-18184 AC4 deprecated warning shown | `UI/components/WarningsPanel.vue` | `UT/details.spec.ts` (creation notice and warnings) | done |
| SIMPL-18186 AC1 automatic ACTIVE | `BE/store/VocabularyRepository.java#insertVocabulary` | `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18187 AC1, AC2 publish | `BE/api/VocabularyController.java` (Location and href, D31) | `BT/VocabularyFlowIntegrationTest` | done |
| SIMPL-18189 AC1 to AC3 automatic versioning UI | `UI/views/NewVersion.vue` (no version field, notification 'Version <n> of <Name> was created.') | `UT/newVersionForm.spec.ts`, `UT/createForm.spec.ts` | done |
| SIMPL-18190 AC1 next number | `BE/service/VocabularyService.java#createVersion` | `BT/ConcurrencyIntegrationTest` | done |
| SIMPL-18190 AC2 version field rejected | `BE/api/MultipartForm.java#checkNames` | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites` | done |
| SIMPL-18191 AC1 deprecation of older versions | `BE/store/VocabularyRepository.java#insertVersion` (same transaction) | `BT/VocabularyFlowIntegrationTest`, `BT/ConcurrencyIntegrationTest`, smoke | done |
| SIMPL-18193 AC1, AC2 report via UI | `UI/components/ValidationReport.vue` | `UT/createForm.spec.ts` | done |
| SIMPL-18194 AC1, AC2 report via API | `BE/validation/ContentValidator.java`, `BE/api/dto/VersionCreatedResponse.java` | `BT/VocabularyFlowIntegrationTest#validationReportAndWarnings` | done |
| SIMPL-18195 AC1 admin actions | `UI/views/*` (role from token) | `UT/list.spec.ts` (admin actions) | done |
| SIMPL-18195 AC2 hidden for others | same (D18) | `UT/createForm.spec.ts`, `UT/newVersionForm.spec.ts` (not authorized) | done |
| SIMPL-18195 AC3 login prompt | `UI/auth.ts` | `UT/components.spec.ts` | done |
| SIMPL-18196 AC1 to AC4 API access control | `BE/web/RequestGateFilter.java`, `BE/security/TokenVerifier.java`, `BE/web/OperationCatalog.java`, `BE/web/HandlerRuleCheck.java` | `BT/VocabularyFlowIntegrationTest#accessRules`, `BT/security/*`, `BT/web/HandlerRuleCheckTest`, smoke | done |
| SIMPL-18452 AC1, AC2 any version details and download | `UI/views/VocabularyDetails.vue` (panel, Download) | `UT/details.spec.ts` | done |
| SIMPL-18559 AC1 warning and link to latest ACTIVE | `UI/views/VocabularyDetails.vue` (D36, D64, D118) | `UT/details.spec.ts` | done |
| SIMPL-18559 AC2 no ACTIVE version | same | `UT/details.spec.ts` (first sentence only) | done |
| SIMPL-28680 (all) | not built | none | out of scope (D1, D61) |
| SIMPL-393 | met by generic upload, no seed data (D1) | `BT/VocabularyFlowIntegrationTest` uploads Turtle | done as decided |
| SIMPL-395 | replaced by the SIMPL-181xx stories (D1) | none | out of scope |

## Decisions

| Decision | Where | Own tests |
|---|---|---|
| D1, D61 scope | only the stories above | n/a |
| D2, D87, D97, D117 CDM, LDM, PDM, contract | `docs/logical-data-model.md`, `docs/physical-model-shapes.ttl`, `docs/openapi-decided.yaml`, `scripts/make_openapi_decided.py` | `BT/PhysicalModelShaclTest`, `BT/ContractConformanceTest` |
| D3, D4, D160 statuses and transitions | `BE/service/VocabularyService.java#setStatus`, `#setVersionStatus`, `BE/store/VocabularyRepository.java#setStatus` | `BT/VocabularyFlowIntegrationTest`, `BT/ApiEdgeCasesIntegrationTest#versionStatusAndNotFoundTexts` |
| D5 latest version, latestVersionHref | `BE/api/Representations.java` | `BT/VocabularyFlowIntegrationTest` |
| D6, D8 publish, nothing stored on failure | `BE/store/VocabularyRepository.java` (one update request per write) | `BT/VocabularyFlowIntegrationTest#validationReportAndWarnings` |
| D7 version numbering, version field refused | `BE/api/VocabularyController.java` (VERSION_FIELDS) | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites` |
| D9, D53, D65, D125, D140, D142 to D144 rules S1 to S7 | `BE/validation/SemanticRules.java`, `TurtleSyntax.java`, `ContentValidator.java` | `BT/validation/*` |
| D10, D11, D47 warnings | `BE/validation/ContentValidator.java`, `BE/domain/ValidationWarning.java` | `BT/validation/ContentValidatorTest` |
| D12, D69, D145, D146 references | `BE/validation/ReferenceAnalyzer.java` | `BT/validation/ReferenceAnalyzerTest` |
| D13, D60, D62, D76, D101 upload check in the UI | `UI/turtleCheck.ts`, `UI/components/FileField.vue` | `UT/turtleCheck.spec.ts`, `UT/components.spec.ts` |
| D14, D72, D90, D109, D113, D114 feedback | `UI/components/ValidationReport.vue`, `WarningsPanel.vue`, `UI/views/*` | `UT/*.spec.ts` |
| D15, D76, D150 sizes and media types | `application.properties` (multipart limits), `BE/api/MultipartForm.java`, `BE/api/JsonBody.java` | `BT/ApiEdgeCasesIntegrationTest#sizeLimits` |
| D16, D17, D84, D139 roles, 401/403, default deny | `BE/web/OperationCatalog.java`, `RequestGateFilter.java`, `HandlerRuleCheck.java` | `BT/VocabularyFlowIntegrationTest#accessRules`, `BT/web/*Test` |
| D18 restricted actions in the UI | `UI/views/*` | `UT/createForm.spec.ts`, `UT/newVersionForm.spec.ts` |
| D19 to D23, D50, D58, D78, D111, D128, D154 list queries | `BE/api/VocabularyController.java`, `BE/store/VocabularyRepository.java`, `BE/api/Representations.java` | `BT/ApiEdgeCasesIntegrationTest`, `BT/api/*Test` |
| D21, D52, D138, D149 unknown and repeated parameters | `BE/api/QueryParameters.java`, `MultipartForm.java#checkNames`, `JsonBody.java#unknownProperties` | `BT/ApiEdgeCasesIntegrationTest` |
| D24, D49, D54, D55, D66 status codes and texts | `BE/error/Problems.java` | `BT/ApiEdgeCasesIntegrationTest` |
| D25 to D29, D31 objects and naming | `BE/api/dto/*`, `BE/api/Representations.java` | `BT/VocabularyFlowIntegrationTest` |
| D30, D41, D42, D56, D70, D73, D124, D147, D148 external vocabularies | `BE/api/ExternalVocabularyController.java`, `BE/service/ExternalVocabularyService.java`, `BE/store/ExternalVocabularyRepository.java` | `BT/ApiEdgeCasesIntegrationTest#externalRegistrationRules`, `BT/VocabularyFlowIntegrationTest#externalVocabularies` |
| D32 to D37, D46 UI forms | `UI/views/CreateVocabulary.vue`, `NewVersion.vue` | `UT/createForm.spec.ts`, `UT/newVersionForm.spec.ts` |
| D38, D68, D137, D153 searches | `BE/api/SearchController.java` | `BT/ApiEdgeCasesIntegrationTest#searchValidation`, `BT/VocabularyFlowIntegrationTest` |
| D39, D79, D91, D110, D116 contract changes | `docs/openapi-decided.yaml` | `BT/ContractConformanceTest`; Schemathesis run on both contracts (`BUILD-LOG.md`) |
| D40, E1, E5, E7, D108, D132 tokens | `BE/security/TokenVerifier.java`, `keycloak/realm-*.json` | `BT/security/TokenVerifierTest`; live probe with E7 clients (`BUILD-LOG.md`) |
| D43, D82, D119, D157 concurrency | `BE/service/VocabularyService.java`, `WriteRetry.java`, conditional updates in `VocabularyRepository.java` | `BT/ConcurrencyIntegrationTest`, `BT/service/WriteRetryTest` |
| D44, D88, D103, D129, D134, D152 store and IdP down | `BE/store/SparqlStoreClient.java`, `BE/security/RealmKeyProvider.java`, `BE/config/ResilienceConfig.java`, `BE/api/HealthController.java` | `BT/StoreDownIntegrationTest`, `BT/security/RealmKeyProviderTest`, `TokenVerifierTest`; `./stack.sh store-down-up` probe |
| D45 pull only | no events published | n/a |
| D46 identical version | `BE/service/VocabularyService.java#rejectIdentical` | `BT/VocabularyFlowIntegrationTest#validationReportAndWarnings` |
| D48, D104, D122, D158 problem types | `BE/error/*`, `BE/web/ProblemErrorReportValve.java`, `ui/nginx/problem-pages.inc` | `BT/ApiEdgeCasesIntegrationTest#routingAndMethods`, live probes |
| D51, D162 order of checks | `RequestGateFilter.java` then controllers then services | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites` |
| D57 base IRI | `BE/service/VocabularyService.java#validate` | `BT/validation/TurtleSyntaxTest#relativeIrisResolveAgainstTheBase` |
| D63, D127, D151 Accept | `BE/web/AcceptCheck.java` | `BT/web/AcceptCheckTest`, `BT/ApiEdgeCasesIntegrationTest#routingAndMethods` |
| D67, D77, D126, D155, D166 routing, 405, firewall | `BE/web/RequestGateFilter.java`, `OperationCatalog.java`, `BE/config/ServerConfig.java`, `ui/nginx/vocabulary-manager.conf.template` | `BT/web/OperationCatalogTest`, `BT/ApiEdgeCasesIntegrationTest#routingAndMethods` |
| D71, D92 time budget, upload bulkhead | `application.properties` (timeouts), `BE/config/ResilienceConfig.java#uploadBulkhead` | measured, `QUALITY.md` |
| D80, D96, D105, D165 logs | `application.properties` (logstash JSON), MDC in `RequestGateFilter.java` and controllers | live log check (`QUALITY.md`) |
| D83, D98, D107, D120 store access and ports | `fuseki/config.ttl`, `fuseki/entrypoint.sh`, `compose.yaml` | live probe (`BUILD-LOG.md`) |
| D86 metrics and trace context | `BE/web/OperationObservationConvention.java`, `BE/web/TraceHeaders.java` | live metrics check (`QUALITY.md`) |
| D89, D156, Q3 backup, restore, compaction | `scripts/backup.sh`, `restore.sh`, `compact.sh`, `backup-restore-test.sh` | round trip run (`QUALITY.md`) |
| D93 request validation tied to the contract | `BE/api/RequestRules.java` constants | `BT/ContractConformanceTest` |
| D94, D108 libraries | Jena ParameterizedSparqlString and update objects, Nimbus, Micrometer Tracing | `REUSE.md` |
| D95 format, lint, UI tests | Spotless (Palantir) in `pom.xml`, ESLint and Prettier in `ui/` | `mvn verify`, UI Docker build stage |
| D99, D136, Q1, Q4 load | `scripts/loadtest.py` | `reports/loadtest.txt`, `QUALITY.md` |
| D100, D159 list columns | `UI/views/VocabularyList.vue` | `UT/list.spec.ts` |
| D102 two backends | `compose.yaml` (`replicas: 2`, no container name or port per backend) | smoke on two backends |
| D106 file bytes in the store | `BE/store/ContentCodec.java` | `BT/store/ContentCodecTest` |
| D112 integers | `BE/api/RequestRules.java#positiveInt`, `JsonBody.java#positiveInt` | `BT/api/RequestRulesTest` |
| D115, D118, D123, D141, D161, D163, D167 UI details | `UI/*`, `ui/nginx/ui-headers.inc.template` | `UT/*.spec.ts` |
| D121 empty part name ignored | `BE/api/MultipartForm.java#read` | `BT/ApiEdgeCasesIntegrationTest#orderOfChecksOnWrites` |
| D130 offsets | `BE/api/RequestRules.java#dateFilter` | `BT/api/RequestRulesTest#dateFilters` |
| D131 start command | `stack.sh` | finish line |
| D133 framework routing | Spring MVC mappings with `consumes`, `produces` | `BT/ApiEdgeCasesIntegrationTest` |
| D135 no single-implementation interfaces | whole backend | n/a, see `DEVIATIONS.md` (CODE-11) |
| D164 failed-token limiting | not built (gateway) | see `DEVIATIONS.md` |
| E2, E4 configuration from environment | `application.properties`, `compose.yaml` | n/a |
| E3, E6 store and reuse choices | `fuseki/`, `REUSE.md` | n/a |
| Q2 limits | see `QUALITY.md` | `BT/ApiEdgeCasesIntegrationTest#sizeLimits`, `#queryParameterRules`, `#searchValidation` |
| Q5 maintainability | JaCoCo 94.2 % lines, 83.4 % branches; Trivy | `reports/jacoco/`, `SECURITY-SCAN.md` |
| Q6 accessibility | `UI/*` (labels, roles, keyboard) | not measured with axe in this build (no browser in the build step), see `BUILD-LOG.md` |
