# Traceability

Acceptance criteria are numbered AC1, AC2, ... in the order the scenarios appear in each story where
the story does not number them itself. Paths: `B` = `backend/src/main/java/eu/simpl/vocabularymanager/`,
`T` = `backend/src/test/java/eu/simpl/vocabularymanager/`, `U` = `ui/src/`, `UT` = `ui/tests/`.
"smoke" = `smoke.sh`; "browser" = the one-off Playwright check recorded in BUILD-LOG.md.

## Stories

| Story | AC | Implemented in | Own tests |
|---|---|---|---|
| SIMPL-18161 View all vocabularies (UI) | AC1 list with actions menu, name, description, version, status, dates, creator (D100) | `U views/VocabularyList.vue`, `U components/ActionsMenu.vue` | `UT VocabularyList.spec.ts` (columns), browser |
| | AC2 load error with detail (D34, D114) | `VocabularyList.vue` load() | `UT VocabularyList.spec.ts` (one alert) |
| SIMPL-18162 Metadata of all vocabularies (API) | AC1 paginated list with metadata | `B web/VocabularyController.getVocabularies`, `B store/VocabularyRepository.list`, `B web/Representations.page` | `T it/RequestRulesIT.listFiltersSortsAndPages`, smoke |
| | AC2 wrong pagination 400 (D22) | `VocabularyController.paging` | `RequestRulesIT.queryParameterRules` |
| | AC3 no role 403 (D16) | `B security/SecurityConfig` | `RequestRulesIT.tokensAndRoles` |
| | AC4 internal error 500 | `B error/GlobalExceptionHandler.unexpected`, `SparqlClient.guarded` | not forced in a test (no way to make the real stack fail with 500 on demand); 503 path tested in `StoreDownIT` |
| SIMPL-18163 Sort and filter (UI) | AC1 filters (D72) | `VocabularyList.vue` filters | browser (Name contains) |
| | AC2 sortable headers (D23, D50) | `VocabularyList.vue` sortBy | `UT VocabularyList.spec.ts` (reverse active column) |
| SIMPL-18164 Filters and sorting (API) | AC1 filter and sort on name, status, creationDate, lastUpdated (D19, D20, D23, D58, D128) | `VocabularyController`, `B validation/DateFilters`, `VocabularyRepository.listOrder` | `RequestRulesIT.listFiltersSortsAndPages`, `T unit/HelpersTest.dates` |
| | AC2 no match gives empty set | `VocabularyRepository.list` | `RequestRulesIT.listFiltersSortsAndPages` (lastUpdated=2001-01-01) |
| | AC3 paging with sort (D78, D111) | `Representations.page/link` | `RequestRulesIT.listFiltersSortsAndPages`, `T web/RoutesTest.linksPutPagingFirstAndEncodeValues` |
| | AC4 unsupported or invalid values 400 (D21, D52, D138) | `B validation/QueryParameters` | `RequestRulesIT.queryParameterRules`, `HelpersTest.queryStrings` |
| SIMPL-18165 Download newest version (UI) | AC1 download and full details (D29) | `VocabularyList.vue` 'Download vocabulary', `U api.ts download`, details page | browser (details), `UT VocabularyDetails.spec.ts` |
| SIMPL-18166 Full details of a version (API) | AC1 metadata and content (D29) | `VocabularyController.getVocabularyVersion`, `B web/ContentController` | `VocabularyLifecycleIT.newVersionDeprecates...` |
| | AC2 unknown version 404 (D55) | `VocabularyService.versionNotFound` | `VocabularyLifecycleIT.statusChangesFollowTheStateTable` |
| SIMPL-18167 Version history (UI) | AC1 details page with the version list (D35) | `U views/VocabularyDetails.vue` | `UT VocabularyDetails.spec.ts`, browser |
| SIMPL-18168 Version history (API) | AC1 list of versions, newest first (D23) | `VocabularyController.getVocabularyVersions`, `VocabularyRepository.listVersions` | `VocabularyLifecycleIT`, `RequestRulesIT.listFiltersSortsAndPages` |
| | AC2 unknown vocabulary 404 | `VocabularyService.versions` | `VocabularyLifecycleIT.notFoundTextsAndPathRules` |
| SIMPL-18169 Create vocabulary (UI) | AC1 form with Name, Description, file; no version or change log (D32, D60, D141) | `U views/VocabularyCreate.vue`, `U components/VocabularyForm.vue` | `UT VocabularyCreate.spec.ts` |
| | AC2 validation and messages (D37, D62, D101) | `U validation.ts`, `VocabularyForm.vue` | `UT VocabularyCreate.spec.ts`, `UT validation.spec.ts` |
| SIMPL-18170 Create vocabulary (API) | AC1 validated, version 1 ACTIVE, vocabulary ACTIVE | `VocabularyController.createVocabulary`, `B service/VocabularyService.create` | `VocabularyLifecycleIT.createsVersionOneActive...`, smoke |
| | AC2 retrieval shows metadata; status can change later | `getVocabulary`, `setStatus` | `VocabularyLifecycleIT.statusChangesFollowTheStateTable` |
| | AC3 invalid data rejected, nothing stored (D8) | `RequestChecks`, `ContentValidator` | `RequestRulesIT.formFieldRules`, `ValidationIT.syntaxError...StoresNothing` |
| SIMPL-18172 New version (UI) | AC1 pre-filled form (D33) | `U views/VocabularyNewVersion.vue` | `UT VocabularyNewVersion.spec.ts`, browser |
| | AC2 creation, deprecation, notification (D113) | `VocabularyNewVersion.vue`, details page flash | browser |
| | AC3 error, advice to retry (D60) | `VocabularyForm.describeFailure` | covered by code review only (no UI test for 500) |
| SIMPL-18173 New version (API) | AC1 next number, ACTIVE, older DEPRECATED | `VocabularyService.addVersion`, `VocabularyRepository.addVersion` | `VocabularyLifecycleIT.newVersionDeprecates...`, smoke |
| | AC2 invalid data rejected | `RequestChecks`, `ContentValidator` | `RequestRulesIT.formFieldRules` |
| | AC3 unknown vocabulary rejected (404) | `VocabularyService.addVersion` | `VocabularyLifecycleIT.notFoundTextsAndPathRules` |
| | AC4 no role 403, AC5 no token 401 | `SecurityConfig` | `RequestRulesIT.tokensAndRoles`, smoke |
| | AC6 unsupported format 415 (D15) | `B web/NegotiationFilter` | `RequestRulesIT.negotiationOrder` |
| SIMPL-18174 Status change (UI) | AC1 Deprecate/Activate with message (D60) | `VocabularyList.vue` changeStatus | `UT VocabularyList.spec.ts`, browser |
| | AC2 404 message (D72) | `VocabularyList.vue` statusFailure | `UT VocabularyList.spec.ts` |
| | AC3 failure message, status unchanged | `VocabularyList.vue` statusFailure | code review only |
| SIMPL-18175 Status change (API) | AC1 updated metadata | `VocabularyController.updateVocabularyStatus`, `VocabularyService.setStatus` | `VocabularyLifecycleIT.statusChangesFollowTheStateTable` |
| | AC2 unknown 404 | `VocabularyService.vocabularyNotFound` | `VocabularyLifecycleIT.notFoundTextsAndPathRules` |
| | AC3 other value 400 | `RequestChecks.status` | `VocabularyLifecycleIT.statusChangesFollowTheStateTable` |
| SIMPL-18176 Deprecated versions (UI) | AC1 viewer can read; AC2 deprecated shown and labelled; AC3 metadata panel (D35) | `VocabularyDetails.vue` | `UT VocabularyDetails.spec.ts` |
| SIMPL-18178 Warning on deprecated vocabularies (UI) | AC1 icon with text (D36, D72) and banner (D115) | `VocabularyList.vue`, `VocabularyDetails.vue` | `UT VocabularyList.spec.ts`, `UT VocabularyDetails.spec.ts`, browser |
| SIMPL-18180 Syntax and structure (UI) | AC1 check on upload (D13); AC2/AC3 server check on submit; AC4 'Syntax check passed.'; AC5 to AC7 errors with location, all errors; AC8 blocked | `U validation.ts checkTurtle`, `VocabularyForm.vue`, `U components/ValidationReport.vue` | `UT validation.spec.ts`, `UT VocabularyCreate.spec.ts`, browser |
| SIMPL-18181 Syntax (API) | AC1 passes to semantic step; AC2 rejected with one located error, nothing stored (D9) | `B content/TurtleParser`, `ContentValidator` | `ValidationIT.syntaxError...`, `T unit/ContentRulesTest.syntaxErrorHasLineAndColumn` |
| SIMPL-18182 Semantics (UI) | AC1 to AC3 server report shown | `ValidationReport.vue` | `UT VocabularyCreate.spec.ts` (report) |
| SIMPL-18183 Semantics (API) | AC1 created when valid; AC2 all errors returned (S1 to S7, D9, D53, D65, D140) | `B content/SemanticRules`, `B content/ReferenceChecker` | `ValidationIT.semanticErrorsAreAllReportedInFileOrder`, `ContentRulesTest` |
| SIMPL-18184 Deprecated references | AC1 ACTIVE references pass; AC2 deprecated reference is a warning, not an error (D11, D47); AC3 missing version is an error (S7); AC4 warnings shown in the UI (D14) | `ReferenceChecker`, `U components/ValidationWarnings.vue` | `ValidationIT.internalReferencesGiveErrorsAndDeprecationWarnings`, `ContentRulesTest.referenceShapesAndWarnings` |
| SIMPL-18186 Automatic publication (UI) | AC1 stored as ACTIVE (D6) | `VocabularyService.create/addVersion` | `VocabularyLifecycleIT.createsVersionOneActive...` |
| SIMPL-18187 Publish (API) | AC1/AC2 persisted, identifier = Location and href (D31) | `VocabularyController.createdResponse` | `VocabularyLifecycleIT.createsVersionOneActive...` |
| SIMPL-18189 Automatic versioning (UI) | AC1/AC2 no version field; AC3 message with number | `VocabularyForm.vue`, `VocabularyNewVersion.vue` | `UT VocabularyNewVersion.spec.ts` (no version sent), browser |
| SIMPL-18190 Automatic versioning (API) | AC1 next number; AC2 version field 400 (D7) | `VocabularyController.createVocabularyVersion` | `RequestRulesIT.formFieldRules` |
| SIMPL-18191 Automatic deprecation (UI) | AC1 new ACTIVE, older DEPRECATED | backend as SIMPL-18173 | `VocabularyLifecycleIT.parallelNewVersions...`, smoke |
| SIMPL-18193 Validation report (UI) | AC1 success; AC2 structured report (D90, D109) | `ValidationReport.vue` | `UT VocabularyCreate.spec.ts` |
| SIMPL-18194 Validation report (API) | AC1 structured report; AC2 success without error details | `ContentValidator.report`, `Representations.created` | `ValidationIT` |
| SIMPL-18195 RBAC (UI) | AC1 admin sees actions; AC2 hidden and message (D18); AC3 login | `U auth.ts`, `VocabularyList.vue`, `VocabularyCreate.vue` | `UT VocabularyCreate.spec.ts` (not authorized), browser (login) |
| SIMPL-18196 RBAC (API) | AC1 authorised; AC2 403; AC3 401; AC4 all endpoints protected (D84) | `SecurityConfig`, `JwtAuthenticationFilter`, `TokenVerifier`, `B web/RouteRuleCheck` | `RequestRulesIT.tokensAndRoles`, `RoutesTest.handlerWithoutRuleIsReported`, `RealmKeysTest` |
| SIMPL-18452 Download any version (UI) | AC1 select a version; AC2 download | `VocabularyDetails.vue` panel and Download | `UT VocabularyDetails.spec.ts`, browser |
| SIMPL-18559 Deprecated version warning (UI) | AC1 warning and link to latest ACTIVE (D64, D118); AC2 warning only | `VocabularyDetails.vue` activeLink | `UT VocabularyDetails.spec.ts` |
| SIMPL-28680, SIMPL-393, SIMPL-395 | out of scope (D1, D61): no seed data, no deletion of internal vocabularies | none | none |

## Decisions

| Decision | Where | Tests |
|---|---|---|
| D1, D61 scope | only in-scope stories built | none |
| D2, D87, D97, D117 CDM, LDM, physical model, decided contract | `docs/architecture.md`, `docs/physical-model.ttl`, `docs/openapi-decided.yaml` | `StoredDataShapesIT`, `DecidedContractTest` |
| D3, D4 statuses and transitions | `VocabularyService`, `VocabularyRepository` | `VocabularyLifecycleIT` |
| D5 latest version, latestVersionHref | `Representations.version` | `VocabularyLifecycleIT` |
| D6, D7, D8 | `VocabularyService` | `VocabularyLifecycleIT`, `ValidationIT` |
| D9, D10, D53, D65, D125, D140 validation | `content/*` | `ValidationIT`, `ContentRulesTest` |
| D11, D12, D47, D69 references | `ReferenceChecker` | `ValidationIT`, `ContentRulesTest` |
| D13, D60, D62, D76, D101 upload check and form | `U validation.ts`, `VocabularyForm.vue` | `UT validation.spec.ts`, `UT VocabularyCreate.spec.ts` |
| D14, D90, D109 report and warnings | `ValidationReport.vue`, `ValidationWarnings.vue`, flash | `UT VocabularyCreate.spec.ts`, browser |
| D15, D76, D110 sizes and media types | `NegotiationFilter`, `MultipartForm`, `JsonBody`, `application.yaml` | `RequestRulesIT.negotiationOrder`, `formFieldRules` |
| D16, D17, D84, D132 access | `security/*`, `RouteRuleCheck` | `RequestRulesIT.tokensAndRoles`, `RealmKeysTest`, `RoutesTest` |
| D18, D74, D75 UI access, routes, semantics | `U router.ts`, views | `UT VocabularyCreate.spec.ts`, browser |
| D19 to D23, D50, D58, D59, D78, D111, D128 list | `VocabularyController`, `VocabularyRepository`, `Representations` | `RequestRulesIT.listFiltersSortsAndPages`, `HelpersTest` |
| D24, D49, D54, D55, D66 status codes and texts | services, `RequestChecks` | `VocabularyLifecycleIT.notFoundTextsAndPathRules`, `SearchAndExternalIT` |
| D25 to D28, D80 data and creator | `Representations`, `TokenVerifier.user` | `VocabularyLifecycleIT.createsVersionOneActive...`, `RealmKeysTest` (toString without email) |
| D29, D70 downloads | `ContentController` | `VocabularyLifecycleIT.newVersionDeprecates...` |
| D30, D41, D42, D56, D73, D121, D124 external vocabularies | `ExternalVocabularyController`, `ExternalVocabularyService`, `ExternalVocabularyRepository` | `SearchAndExternalIT` |
| D31 glossary terms | code, UI texts, `docs/glossary.md` | none |
| D32 to D37, D46, D72, D100, D113 to D115, D118, D141 UI | views and components | UI tests, browser |
| D38, D68, D137 searches | `SearchRequests`, controllers | `SearchAndExternalIT` |
| D39, D79, D91, D112, D116 contract changes | code and `docs/openapi-decided.yaml` | `DecidedContractTest`, `HelpersTest.integers` |
| D40, E1, E5, E7 identity | `keycloak/*.json`, `TokenVerifier` | `RequestRulesIT.tokensAndRoles` (issuer, audience, expiry, unknown key) |
| D43, D82, D119 concurrency | conditional updates in `VocabularyRepository`, retry loop in `VocabularyService` | `VocabularyLifecycleIT.parallelNewVersions...` (one instance), `tools/parallel-check.sh` (two replicas, result in QUALITY.md) |
| D44, D71, D103, D134 store down, budgets, breakers | `SparqlClient`, `InfrastructureConfig`, `HealthController` | `StoreDownIT`, `BreakerRuleTest` |
| D45 pull only | no events | none |
| D48, D104, D122 problem types and proxy errors | `error/*`, `ui/nginx/nginx.conf` | `ProblemTypesTest`, own probe (BUILD-LOG) |
| D51, D63, D67, D126, D127, D133, D139 order of checks | filters | `RequestRulesIT.methodsAndUnknownPaths`, `negotiationOrder` |
| D52, D138 unknown fields and parameters | `QueryParameters`, `MultipartForm`, `JsonBody` | `RequestRulesIT`, `SearchAndExternalIT` |
| D57 base IRI | `VocabularyService.contentUrl`, `TurtleParser` | `ContentRulesTest.relativeIrisResolve...` |
| D83, D98, D107, D120, D131 store access, stack hardening | `fuseki/*`, `docker-compose.yml`, `stack.sh` | own probe (anonymous query 401, admin 404) in BUILD-LOG |
| D85 bounded queries | repositories | code review; `listFiltersSortsAndPages` |
| D86, D94, D96, D105 metrics, tracing, logs | `TraceHeaders`, `CorrelationFilter`, `application.yaml` | log and metric sample in BUILD-LOG |
| D88, D108, D129 key fetch timing | `RealmKeys` | `RealmKeysTest` |
| D89, Q3 backup and restore | `ops/*` | `ops/backup-restore-test.sh` (PASS) |
| D92 upload limit | `UploadLimiter` | `UploadLimiterTest` |
| D93 request validation tied to the contract | `ApiRoutes`, `DecidedContractTest` | `DecidedContractTest` |
| D95 lint and format | Spotless in `mvn verify`, ESLint and Prettier in `npm run build` | build |
| D99, D102, D136, Q1, Q4 load and replicas | `docker-compose.yml` replicas 2, `tools/load.py` | QUALITY.md |
| D106 file bytes in the store | `VocabularyRepository` (base64 literal) | `VocabularyLifecycleIT` (bytes as uploaded) |
| D123 CSP | `ui/nginx/nginx.conf` | header check and browser run without console errors |
| D130 offsets | `DateFilters` | `HelpersTest.dates`, `RequestRulesIT` |
| D135 no single-implementation interfaces | whole code | none |
| E2, E4 configuration | `application.yaml`, `docker-compose.yml` | none |
| E3, E6 store and reuse | Fuseki, REUSE.md | none |
