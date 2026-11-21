# Traceability

Code paths are relative to `backend/src/main/java/eu/simpl/vocabularymanager/` (BE) and
`ui/src/` (UI). Backend tests are in `backend/src/test/java/eu/simpl/vocabularymanager/`; UI tests
in `ui/tests/`. Acceptance criteria without an id in the story are numbered in story order (AC1,
AC2, ...). "smoke" means `smoke.sh`.

## Stories and acceptance criteria

| Story | AC | Code | Own tests |
|---|---|---|---|
| SIMPL-18161 View all vocabularies (UI) | AC1 list with columns and actions menu | UI `views/VocabularyList.vue`, `components/ActionsMenu.vue` (D100, D159) | `list.test.ts`; UI walk-through (BUILD-LOG) |
| | AC2 load error with details | `VocabularyList.vue`, `format.ts` (D34, D72, D114) | `list.test.ts` (one load-error alert) |
| SIMPL-18162 Metadata of all vocabularies (API) | AC1 200 paged list with metadata | BE `api/VocabularyController.list`, `store/VocabularyStore.list`, `api/Dto` | `it/VocabularyApiTest.listFiltersSortAndPaging`, smoke |
| | AC2 wrong pagination 400 | `api/QueryParams.positiveInt` (D22, D91) | `VocabularyApiTest.requestShapeErrors`, `api/RequestRulesTest.queryParams` |
| | AC3 no role 403 | `config/SecurityConfig` (D16) | `it/AccessAndProtocolTest.tokenAndRoles` |
| | AC4 internal error 500 with details | `api/ProblemHandler.other`, `store` errors to 500 (D44) | `DomainUnitTest` (store failures); 503 path in `it/StoreDownTest` |
| SIMPL-18163 Sort and filter (UI) | AC1 filter | `VocabularyList.vue` (Name contains, Vocabulary status, Created on; D72) | `list.test.ts` (status filter) |
| | AC2 sortable columns | `VocabularyList.vue` (D23, D50, D128, D163) | `list.test.ts` (sort toggling) |
| SIMPL-18164 Filters and sorting (API) | AC1 filter and sort | `VocabularyController.list`, `VocabularyStore.orderBy` (D19, D20, D23, D58) | `VocabularyApiTest.listFiltersSortAndPaging` |
| | AC2 no match empty set | same | `listFiltersSortAndPaging` (lastUpdated=2001-01-01) |
| | AC3 paging with sort | `Dto.PageDto.paged` (D22, D59, D78, D111) | `listFiltersSortAndPaging`, `RequestRulesTest.pagingLinks` |
| | AC4 unsupported filter or sort 400 | `QueryParams.parse`, `VocabularyController.list` (D21, D52, D138) | `requestShapeErrors`, `RequestRulesTest.queryParams` |
| SIMPL-18165 Download newest version (UI) | AC1 download and details | UI `api.ts` (download), list action 'Download vocabulary', details page (D29) | `list.test.ts`, `details.test.ts`; BE `VocabularyApiTest.createVersionListDownloadSearch` |
| SIMPL-18166 Full details of a version (API) | AC1 metadata and content | `VocabularyController.version`, `downloadVersion` (D26, D29) | `createVersionListDownloadSearch` |
| | AC2 unknown version 404 | `VocabularyService.versionNotFound` (D55) | `requestShapeErrors`, `statusChangesAndConflicts` |
| SIMPL-18167 Version history (UI) | AC1 details page with version list | UI `views/VocabularyDetails.vue` (D35, D163) | `details.test.ts` |
| SIMPL-18168 Version history (API) | AC1 list of versions | `VocabularyController.versions`, `VocabularyStore.listVersions` (D23, D154) | `createVersionListDownloadSearch`, `statusChangesAndConflicts` |
| | AC2 unknown vocabulary 404 | `VocabularyService.vocabularyNotFound` (D66) | `requestShapeErrors` |
| SIMPL-18169 Create vocabulary (UI) | AC1 form with Name, Description, file; no version or change log | UI `views/CreateVocabulary.vue` (D32, D60, D74, D141) | `create.test.ts` |
| | AC2 validation and error messages | `CreateVocabulary.vue`, `components/ValidationReport.vue` (D37, D62, D101, D161) | `create.test.ts` |
| SIMPL-18170 Create vocabulary (API) | AC1 201 with identifier, version 1 ACTIVE, status ACTIVE | `VocabularyController.create`, `VocabularyService.create`, `VocabularyStore.insertVocabulary` | `createVersionListDownloadSearch`, smoke |
| | AC2 GET shows metadata; status changeable | `VocabularyController.get`, `changeStatus` | `statusChangesAndConflicts` |
| | AC3 invalid data 400, nothing created | `RequestRules`, `ContentValidator` (D8, D37) | `requestShapeErrors`, `validationReportAndWarnings` |
| SIMPL-18172 New version (UI) | AC1 pre-filled form | UI `views/NewVersion.vue` (D33, D46) | `newversion.test.ts` |
| | AC2 creates version, notification | `NewVersion.vue`, `flash.ts` (D14, D113) | `newversion.test.ts` |
| | AC3 error, advice to retry | `submitError.ts` (D60) | `newversion.test.ts` (500 text) |
| SIMPL-18173 New version (API) | AC1 next number, ACTIVE, older DEPRECATED | `VocabularyService.createVersion`, `VocabularyStore.insertVersion` (D4, D7) | `createVersionListDownloadSearch`, `statusChangesAndConflicts`, `parallelNewVersionsGetDistinctNumbers` |
| | AC2 invalid data rejected | `RequestRules`, `ContentValidator` | `requestShapeErrors` |
| | AC3 unknown vocabulary rejected (404) | `VocabularyService.get` | `requestShapeErrors` |
| | AC4 no permission 403 | `SecurityConfig` | `AccessAndProtocolTest.tokenAndRoles`, smoke |
| | AC5 not authenticated 401 | `BearerTokenFilter`, entry point | `tokenAndRoles`, smoke |
| | AC6 unsupported format 415 | `NegotiationFilter.checkContentType` (D15) | `AccessAndProtocolTest.negotiationOrder` |
| SIMPL-18174 Status change (UI) | AC1 change and notification | UI list and details, `format.ts` (D60) | `list.test.ts`; UI walk-through |
| | AC2 vocabulary gone (404) | `format.ts statusChangeError` (D72) | `format.test.ts` |
| | AC3 update fails | same (D72, D114) | `format.test.ts`, `list.test.ts` |
| SIMPL-18175 Status change (API) | AC1 updated metadata | `VocabularyController.changeStatus`, `VocabularyService.changeStatus` | `statusChangesAndConflicts` |
| | AC2 unknown vocabulary 404 | same | `requestShapeErrors` |
| | AC3 other value 400, unchanged | `VocabularyController.statusBody` | `statusChangesAndConflicts` |
| SIMPL-18176 Deprecated versions (UI) | AC1 any role reads history | UI routes, `SecurityConfig` READ rule | `details.test.ts` (viewer) |
| | AC2 deprecated versions shown and labelled | `VocabularyDetails.vue` | `details.test.ts` |
| | AC3 metadata of a version | version panel region (D35, D75) | `details.test.ts` |
| SIMPL-18178 Warning on deprecated vocabularies (UI) | AC1 warning | list icon with accessible name, details banner (D36, D72, D115) | `list.test.ts`, `details.test.ts` |
| SIMPL-18180 Syntax and structure validation (UI) | AC1 on upload | UI `components/VocabularyFileField.vue`, `turtle.ts` (n3, D13) | `turtle.test.ts`, `create.test.ts` |
| | AC2, AC3 on submit (create, version) | server validation (D9) | `create.test.ts`, `newversion.test.ts`; BE `validationReportAndWarnings` |
| | AC4 success message | 'Syntax check passed.' (D13) | `create.test.ts` |
| | AC5, AC6, AC7 all errors with location, one report | `ValidationReport.vue` (D90, D109); BE `SemanticRules`, `ContentValidator` | `create.test.ts`; `validation/SemanticRulesTest` |
| | AC8 cannot continue with errors | Submit disabled on upload errors (D62, D76) | `create.test.ts` |
| SIMPL-18181 Syntax validation (API) | AC1 valid passes to semantic step | `ContentValidator.validateInternal` | `validationReportAndWarnings` |
| | AC2 invalid: 400 one issue with line and column, nothing stored | `ParsedTurtle.parse` (D9, D8) | `SemanticRulesTest.syntaxErrorHasLineAndColumn`, `validationReportAndWarnings` |
| SIMPL-18182 Semantic validation (UI) | AC1 to AC3 | server rules shown in the report region | `create.test.ts` (400 report) |
| SIMPL-18183 Semantic validation (API) | AC1 passes, created | `SemanticRules`, `ContentValidator` | `SemanticRulesTest.validFileHasNoFindings`, `createVersionListDownloadSearch` |
| | AC2 all errors, not created | same (D9, D125) | `SemanticRulesTest` (S1 to S6), `ReferencesTest.reportIsTruncatedAt1000`, `validationReportAndWarnings` |
| SIMPL-18184 Deprecated references | AC1 ACTIVE references: no warning | `ContentValidator.deprecatedWarning` | `ExternalApiTest` (no warnings), `ReferencesTest` |
| | AC2 deprecated reference: warning, version created (D11 overrides 'fails') | same (D11, D47) | `ReferencesTest.deprecatedReferenceTexts`, `validationReportAndWarnings` |
| | AC3 missing reference: error | S7 (D12, D47, D69) | `ReferencesTest.missingReferencesAreErrors` |
| | AC4 UI warning | `components/ValidationWarnings.vue` (D14) | `details.test.ts` (creation warnings) |
| SIMPL-18186 Automatic publication (UI) | AC1 stored as ACTIVE | `VocabularyStore.insertVocabulary`, `insertVersion` (D6) | `createVersionListDownloadSearch` |
| SIMPL-18187 Publish (API) | AC1, AC2 persisted, Location and href | `VocabularyController.created` (D26, D31) | `createVersionListDownloadSearch` |
| SIMPL-18189 Automatic versioning (UI) | AC1 to AC3 | `NewVersion.vue`, notification 'Version n of Name was created.' (D14) | `newversion.test.ts` |
| SIMPL-18190 Automatic versioning (API) | AC1 next number | `VocabularyService.createVersion` (D7) | `createVersionListDownloadSearch` |
| | AC2 manual version 400 | `VERSION_FIELDS` shape check (D7) | `requestShapeErrors` |
| SIMPL-18191 Automatic deprecation (UI) | AC1 | `VocabularyStore.insertVersion` op 2 (D4) | `statusChangesAndConflicts`, `TwoInstancesTest` |
| SIMPL-18193 Validation report (UI) | AC1 success, AC2 report and block | `ValidationReport.vue`, `create.test.ts` | `create.test.ts` |
| SIMPL-18194 Validation report (API) | AC1 report with details and location | `ContentValidator`, `Issue.invalidInput` (D10, D65) | `validationReportAndWarnings` |
| | AC2 success without errors | 201 with `validationWarnings` (D10, D14) | `createVersionListDownloadSearch` |
| SIMPL-18195 Access control (UI) | AC1 admin actions | `auth.ts`, views (D18) | `list.test.ts`; UI walk-through |
| | AC2 hidden for others, message on direct URL | same | `create.test.ts`, `newversion.test.ts` (viewer message); UI walk-through |
| | AC3 not authenticated: login | `main.ts` (login-required) | UI walk-through |
| SIMPL-18196 Access control (API) | AC1 role processes | `SecurityConfig`, `Routes.ALL` | `tokenAndRoles` |
| | AC2 no role 403 | same | `tokenAndRoles` |
| | AC3 no token 401 | `BearerTokenFilter` | `tokenAndRoles`, `TokenVerifierTest` |
| | AC4 every endpoint protected | `RouteRuleCheck` (D84) | `DomainUnitTest.handlerWithoutRuleFailsTheStart` |
| SIMPL-18452 Download any version (UI) | AC1 select a version, AC2 download | `VocabularyDetails.vue` version panel and Download (D35, D163) | `details.test.ts` |
| SIMPL-18559 Deprecated versions warning (UI) | AC1 warning and link to latest ACTIVE | `VocabularyDetails.vue`, `format.ts recommendedVersion` (D36, D64, D118) | `details.test.ts`, `format.test.ts` |
| | AC2 warning without ACTIVE version | same | `format.test.ts` |
| SIMPL-28680 | all | not built: out of scope (D1, D61) | none |
| SIMPL-393 | AC | met by the generic upload; no seed data (D1) | smoke (upload of any Turtle) |
| SIMPL-395 | all | replaced by the SIMPL-181xx stories (D1) | none |

## Decisions

| Decisions | Code | Own tests |
|---|---|---|
| D1, D2, D61 scope, CDM | whole build; `docs/logical-data-model.md` | n/a |
| D3, D4, D160 statuses, transitions, same-status 409, dates | `VocabularyService.changeStatus`, `changeVersionStatus`, `VocabularyStore.change*Status`, `insertVersion` | `statusChangesAndConflicts`, `DomainUnitTest.statusChangeRaceGives409OnRetry` |
| D5, D25, D26, D27, D28, D29, D31 objects and fields | `api/Dto`, `domain/Model` | `createVersionListDownloadSearch` |
| D6, D7, D8 publish, numbering, nothing stored on failure | `VocabularyStore` (one update per write), `VERSION_FIELDS` | `requestShapeErrors`, `validationReportAndWarnings` |
| D9, D53, D65, D125, D140, D142, D143, D144 validation rules | `validation/ParsedTurtle`, `SemanticRules`, `ContentValidator` | `SemanticRulesTest`, `ReferencesTest` |
| D10, D11, D12, D47, D69, D145, D146 warnings and references | `validation/References`, `ContentValidator`, `store/ReferenceStore` | `ReferencesTest`, `validationReportAndWarnings`, `ExternalApiTest` |
| D13, D14, D18, D32 to D36, D50, D60, D62, D64, D72, D74, D75, D76 (UI part), D90, D100, D101, D109, D113 to D115, D118, D123, D141, D159, D161, D163, D167, Q6 | `ui/src` (see `ui/UI-NOTES.md` table) | `ui/tests/*.test.ts` (32); axe run (BUILD-LOG) |
| D15, D70, D76, D110, D150 media types and sizes | `NegotiationFilter`, `MultipartForm`, `JsonBody`, `application.yaml` | `negotiationOrder`, `requestShapeErrors` |
| D16, D17, D40, D84, E1, E5, E7 access | `config/SecurityConfig`, `security/*`, `api/Routes`, `config/RouteRuleCheck`, `keycloak/*.json` | `AccessAndProtocolTest`, `TokenVerifierTest`, `DomainUnitTest` |
| D19 to D23, D58, D59, D78, D111, D128, D130, D154 list queries | `VocabularyController.list`, `Validation.dateRange`, `VocabularyStore.orderBy`, `Dto.PageDto` | `listFiltersSortAndPaging`, `RequestRulesTest` |
| D24, D49, D54, D55, D66 status codes and texts | `ProblemHandler`, `Validation.pathName`, services | `requestShapeErrors`, `ExternalApiTest` |
| D30, D41, D42, D56, D73, D124, D147, D148 external vocabularies | `api/ExternalController`, `domain/ExternalService`, `store/ExternalStore` | `ExternalApiTest` |
| D37, D52, D121, D138 field rules, unknown fields | `RequestRules`, `MultipartForm`, `QueryParams` | `requestShapeErrors`, `RequestRulesTest` |
| D38, D68, D137, D153 searches | `SearchController`, `ExternalController.searchUris`, `SearchService` | `createVersionListDownloadSearch`, `searchValidation` (both test classes) |
| D39, D79, D87, D91, D93, D116, D133 contract | `docs/openapi-decided.yaml`, `tools/make_openapi_decided.py`, `Routes` | `OpenApiContractTest` |
| D43, D82, D92, D119, D157, D71 concurrency and budgets | `VocabularyStore` conditional updates, `WriteRetry`, `UploadLimiter` | `parallelNewVersionsGetDistinctNumbers`, `TwoInstancesTest`, `DomainUnitTest` |
| D44, D88, D103, D108, D129, D134, D152 store and identity provider down | `StoreClient`, `TokenVerifier`, `HealthController`, `stack.sh store-down-up` | `StoreDownTest`, `TokenVerifierTest`; manual check on port 8199 (BUILD-LOG) |
| D45 pull only | no events published | n/a |
| D46 identical version | `VocabularyService.createVersion` | `duplicateNameAndIdenticalVersion`, `DomainUnitTest.identicalVersionIs409` |
| D48, D104, D122, D158 problem types | `error/*`, `config/ProblemErrorReportValve`, `gateway/default.conf`, UI nginx | `AccessAndProtocolTest`; probe (encoded slash, TRACE) in BUILD-LOG |
| D51, D63, D127, D151, D162 order of checks | filter order in `config/WebConfig`, controllers | `negotiationOrder`, `tokenAndRoles` |
| D57 base IRI | `ContentValidator.validateInternal` | `SemanticRulesTest.relativeIrisResolveAgainstBase` |
| D67, D77, D126, D139, D155, D166 routing, 405 | `RouteGuardFilter`, `Routes.match`, gateway | `methodsAndUnknownPaths`, `RequestRulesTest.routes` |
| D80, D96, D105, D165 logs | `CorrelationFilter`, structured logging, `fuseki/log4j2.properties` | log inspection (BUILD-LOG) |
| D83, D98, D102, D107, D120, D131 stack | `docker-compose.yml`, `fuseki/*`, `stack.sh` | stack start, smoke on two replicas |
| D85 bounded queries | every query in `store/*` has LIMIT | code review |
| D86, D94, D108, D132 libraries and tracing | Micrometer Tracing (Brave), Jena PSS, Nimbus | `TokenVerifierTest` |
| D89, D156, Q3 backup, restore, compaction | `ops/*` | round trip run (BUILD-LOG) |
| D95 lint and format | Spotless Palantir in `pom.xml`; ESLint and Prettier in UI | `mvn verify`, `npm run build` |
| D97, D117 CDM, licence, glossary | `docs/logical-data-model.md`, `LICENSE`, `docs/glossary.md` | n/a |
| D99, D136, Q1, Q4 load | measured with a reduced load (QUALITY.md) | perf script (BUILD-LOG) |
| D164 failed-token limiting | gateway's task, not built | DEVIATIONS.md |
| Q2 limits | see QUALITY.md | tests named there |
| Q5 maintainability | JaCoCo gate 80/60 in `pom.xml` | `mvn verify` |
| E2, E4 ports and URLs from environment | `application.yaml`, compose | stack |
| E3, D106 Fuseki, file bytes in store | `store/*` | all integration tests |
| E6 reuse | REUSE.md | n/a |
