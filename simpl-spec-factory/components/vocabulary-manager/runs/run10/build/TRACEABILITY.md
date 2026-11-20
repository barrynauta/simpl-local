# Traceability

Acceptance criteria are numbered per story in the order they appear (AC1, AC2, ...) where the story does not number
them. Code paths are relative to `backend/src/main/java/eu/simpl/vocabularymanager/` (BE) and `ui/src/` (UI). Own
tests: backend tests in `backend/src/test/java/eu/simpl/vocabularymanager/` (class names), UI component tests in
`ui/tests/` (`*.spec.ts`), stack checks `smoke.sh` and `scripts/ui-check.py`.

## Stories

| Story / AC | Behaviour built | Code | Own tests |
|---|---|---|---|
| SIMPL-18161 AC1 | List with Actions menu, name, description, latest version, status, date created, last updated, creator (D100) | UI `views/VocabularyList.vue`; BE `api/VocabularyController#getVocabularies` | `VocabularyList.spec.ts`, `ListApiTest`, ui-check |
| SIMPL-18161 AC2 | Load error: 'The vocabulary list could not be loaded.' + problem detail, one alert (D34, D114) | UI `VocabularyList.vue` | `VocabularyList.spec.ts` (alert) |
| SIMPL-18162 AC1 | Paged list with name, version, description, status, creationDate, lastUpdateDate | BE `VocabularyController#getVocabularies`, `request/Paging` | `ListApiTest`, `VocabularyLifecycleApiTest`, smoke |
| SIMPL-18162 AC2 | Wrong paging parameter: 400 with detail | BE `request/Paging`, `FieldRules.positiveInt` | `ListApiTest.invalidParametersGive400`, `RequestRulesTest` |
| SIMPL-18162 AC3 | Without role: 403 | BE `security/OperationAuthorizationManager` | `AccessAndOrderApiTest.rolesDecideAccess`, smoke |
| SIMPL-18162 AC4 | Internal error: 500 problem with detail; store down 503 (D44) | BE `error/ProblemHandler`, `store/SparqlStore` | `SparqlStoreTest`, `StoreDownApiTest` |
| SIMPL-18163 AC1 | Filters 'Name contains', 'Vocabulary status', 'Created on' (D72) | UI `VocabularyList.vue` | ui-check (name filter), `ListApiTest` (API) |
| SIMPL-18163 AC2 | Sortable headers; active column reverses (D23, D50) | UI `VocabularyList.vue#sortBy` | `VocabularyList.spec.ts` (second click descending) |
| SIMPL-18164 AC1 | Filters name (contains), status, creationDate, lastUpdated; sort values (D19, D20, D23, D58) | BE `VocabularyController#getVocabularies`, `store/VocabularyRepository#list` | `ListApiTest` |
| SIMPL-18164 AC2 | No match: 200 empty items, total 0 | same | `ListApiTest.pagingLinks...` (empty) |
| SIMPL-18164 AC3 | Paging with sort, stable order, links keep parameters (D22, D59, D78, D111) | `request/Paging` | `ListApiTest`, `RequestRulesTest.pagingLinks` |
| SIMPL-18164 AC4 | Unsupported filter or sort, invalid value: 400 (D21, D52) | `request/QueryParameters`, controller | `ListApiTest.invalidParametersGive400` |
| SIMPL-18165 AC1 | Download latest from the list action; details page shows full metadata (D29) | UI `VocabularyList.vue` (Download vocabulary), `VocabularyDetails.vue`; BE `api/ContentController` | `VocabularyLifecycleApiTest.downloadsReturnStoredBytes`, smoke |
| SIMPL-18166 AC1 | Version metadata (JSON with contentHref) plus content download | BE `VocabularyController#getVocabularyVersion`, `ContentController` | `VocabularyLifecycleApiTest` |
| SIMPL-18166 AC2 | Missing version: 404 'Version <n> of vocabulary ...' | BE `service/VocabularyService#getVersion` | `AccessAndOrderApiTest.invalidPathValuesGive400` (404 at max int), `VocabularyServiceTest` |
| SIMPL-18167 AC1 | 'History' opens Vocabulary details with versions newest first: Version, Status, Date created, Description, Change log, Actions (D35) | UI `VocabularyDetails.vue` | `VocabularyDetails.spec.ts`, ui-check |
| SIMPL-18168 AC1 | Version list, newest first, paged, sort values (D23) | BE `VocabularyController#getVocabularyVersions` | `VocabularyLifecycleApiTest`, `ListApiTest.versionListPagingAndSort` |
| SIMPL-18168 AC2 | Unknown vocabulary: 404 | `VocabularyService#versions` | `ListApiTest.versionListPagingAndSort` |
| SIMPL-18169 AC1 | 'New Vocabulary +' opens form with Name, Description, Vocabulary file; no version or change log field (D32) | UI `components/VocabularyForm.vue`, `views/CreateVocabulary.vue` | `VocabularyForm.spec.ts`, ui-check |
| SIMPL-18169 AC2 | Required fields, formats, unique name (409), supported format (415 API, browser syntax check D13) with messages | UI `VocabularyForm.vue`; BE `FieldRules`, `VocabularyService#create` | `VocabularyForm.spec.ts`, `VocabularyLifecycleApiTest.duplicateName...` |
| SIMPL-18170 AC1 | Create: 201 with Location, version 1 ACTIVE, vocabulary ACTIVE | BE `VocabularyController#createVocabulary`, `VocabularyRepository#insertVocabulary` | `VocabularyLifecycleApiTest.createsVocabularyWithActiveVersionOne`, smoke |
| SIMPL-18170 AC2 | Retrieve shows metadata and version 1 ACTIVE; status can change later | `getVocabulary`, `updateVocabularyStatus` | `VocabularyLifecycleApiTest` |
| SIMPL-18170 AC3 | Invalid or incomplete data: 400, nothing created (D8) | `Checks`, `ContentValidation` | `ValidationApiTest.syntaxError...NothingIsStored`, `AccessAndOrderApiTest.shapeErrors...` |
| SIMPL-18172 AC1 | 'Update' opens form pre-filled with description and current content (D33) | UI `VocabularyForm.vue` (mode version) | `VocabularyForm.spec.ts` (NewVersion), ui-check |
| SIMPL-18172 AC2 | Submit creates version, older ACTIVE deprecated, notification 'Version <n> of <Name> was created.' (D14, D113) | UI `VocabularyForm.vue`, `VocabularyDetails.vue`; BE `VocabularyService#createVersion` | ui-check, `VocabularyDetails.spec.ts` |
| SIMPL-18172 AC3 | Unexpected error: nothing created; 'Something went wrong. Retry later or contact support.' + detail (D60) | UI `texts.ts#writeErrorText` | `VocabularyServiceTest` (no store change on failure), UI text in code |
| SIMPL-18173 AC1 | New version: next number, ACTIVE, older ACTIVE deprecated, response with version details | BE `VocabularyRepository#insertVersion` | `VocabularyLifecycleApiTest.newVersionDeprecates...`, `ConcurrencyApiTest`, smoke |
| SIMPL-18173 AC2 | Invalid or incomplete data: 400 with validation feedback | `Checks`, `ContentValidation` | `AccessAndOrderApiTest.shapeErrors...`, `ValidationApiTest` |
| SIMPL-18173 AC3 | Unknown vocabulary: 404 | `VocabularyService#createVersion` | `VocabularyLifecycleApiTest.newVersionForUnknownVocabularyIs404` |
| SIMPL-18173 AC4 | Not authorised: 403 | `OperationAuthorizationManager` | `AccessAndOrderApiTest.rolesDecideAccess` |
| SIMPL-18173 AC5 | Not authenticated: 401 | `TokenAuthenticationFilter`, entry point | `AccessAndOrderApiTest.noTokenGives401...` |
| SIMPL-18173 AC6 | Unsupported format: 415 (D15) | `web/MediaTypeFilter` | `AccessAndOrderApiTest.unsupportedMediaType...`, `ApiOperationTest.contentTypeRules` |
| SIMPL-18174 AC1 | Status action 'Deprecate'/'Activate', updated status shown, message 'Status of <Name> changed to ...' (D60) | UI `VocabularyList.vue`, `VocabularyDetails.vue` | `VocabularyList.spec.ts`, ui-check |
| SIMPL-18174 AC2 | Vocabulary gone: 'Vocabulary not found. The status was not changed.' (D72) | UI `VocabularyList.vue#setStatus` | `VocabularyList.spec.ts` |
| SIMPL-18174 AC3 | Failure: 'The vocabulary status could not be updated.' + D60 text + detail; status unchanged | UI `VocabularyList.vue#setStatus` | code review (text constant in `texts.ts`) |
| SIMPL-18175 AC1 | PATCH status: 200 with the vocabulary | BE `VocabularyController#updateVocabularyStatus` | `VocabularyLifecycleApiTest.vocabularyStatusChanges...` |
| SIMPL-18175 AC2 | Unknown vocabulary: 404 | `VocabularyService#changeStatus` | `VocabularyServiceTest`, `AccessAndOrderApiTest` |
| SIMPL-18175 AC3 | Other value: 400, unchanged | `VocabularyController#statusOf` | `VocabularyLifecycleApiTest` (status OLD) |
| SIMPL-18176 AC1 | Either role reads versions including deprecated (D16) | `ApiOperation` READ rules | `AccessAndOrderApiTest.rolesDecideAccess`, `VocabularyDetails.spec.ts` (viewer) |
| SIMPL-18176 AC2 | Deprecated versions shown and labelled 'Deprecated' in the version list | UI `VocabularyDetails.vue` | `VocabularyDetails.spec.ts` |
| SIMPL-18176 AC3 | Selecting a version opens panel 'Version <n>' with version, status, date created, creator, description, change log | UI `VocabularyDetails.vue` | `VocabularyDetails.spec.ts`, ui-check |
| SIMPL-18178 AC1 | Deprecated vocabulary: icon with name and tooltip in the list, banner on details (D36, D72, D115) | UI `VocabularyList.vue`, `VocabularyDetails.vue` | `VocabularyList.spec.ts`, `VocabularyDetails.spec.ts`, ui-check |
| SIMPL-18180 AC1 | Browser syntax check on file choice: errors with line, or 'Syntax check passed.'; 5 MB refusal (D13, D76) | UI `turtleCheck.ts`, `VocabularyForm.vue#chooseFile` | `VocabularyForm.spec.ts` (checkTurtle, syntax passed) |
| SIMPL-18180 AC2, AC3 | Server validation on submit of create and new version | BE `service/ContentValidation` | `ValidationApiTest` |
| SIMPL-18180 AC4 | Success: 201 and notification; syntax check message | UI | ui-check |
| SIMPL-18180 AC5 | Syntax errors with location; flow blocked | BE `TurtleSyntaxValidator`; UI Submit disabled on upload error (D62) | `ValidationRulesTest`, `ValidationApiTest` |
| SIMPL-18180 AC6, AC7 | Structure (semantic) errors, all in one response | BE `SemanticValidator`, `ReferenceChecker` | `ValidationApiTest.semanticErrorsAreAllReported` |
| SIMPL-18180 AC8 | Cannot continue until fixed (400 keeps form open, report shown) | UI `VocabularyForm.vue` | `VocabularyForm.spec.ts` (report) |
| SIMPL-18181 AC1 | Valid syntax passes to semantic validation | `ContentValidation#validate` | `ValidationRulesTest` |
| SIMPL-18181 AC2 | Invalid syntax: 400 with one issue, location, semantic not run, nothing stored, no deprecation | `TurtleSyntaxValidator` | `ValidationApiTest.syntaxError...`, `ValidationRulesTest.syntaxError...` |
| SIMPL-18182 AC1 to AC3 | Semantic validation after syntax; success; structured report (UI report panel) | BE `ContentValidation`; UI report region | `ValidationApiTest`, `VocabularyForm.spec.ts` |
| SIMPL-18183 AC1 | Semantic validation passes: created | same | `VocabularyLifecycleApiTest` |
| SIMPL-18183 AC2 | Semantic issues: 400 with all errors, not created | same | `ValidationApiTest.semanticErrorsAreAllReported` |
| SIMPL-18184 AC1 | All referenced versions ACTIVE: no warning | `validation/ReferenceChecker` | `ValidationApiTest.registeredExternalNamespaceGivesNoWarning`, `ValidationRulesTest` |
| SIMPL-18184 AC2 | Deprecated reference: warning (not error, D11) with recommendation (D47) | `ReferenceChecker#checkInternal` | `ValidationApiTest.internalReferences...`, `ValidationRulesTest.referenceCheck...` |
| SIMPL-18184 AC3 | Missing referenced version: 400 S7 | same | same |
| SIMPL-18184 AC4 | UI shows the warning in the 'Validation warnings' panel (D14, D90, D109) | UI `components/WarningsPanel.vue` | `VocabularyDetails.spec.ts` |
| SIMPL-18186 AC1 | Validated version stored ACTIVE in the same request (D6) | `VocabularyRepository` | `VocabularyLifecycleApiTest` |
| SIMPL-18187 AC1, AC2 | Persisted, 201 with Location and href identifier, linked to the vocabulary (D31) | controllers, repository | `VocabularyLifecycleApiTest`, `PhysicalModelShapesTest` |
| SIMPL-18189 AC1 to AC3 | Number assigned automatically; no version field in the form; confirmation with number | UI `VocabularyForm.vue`, `VocabularyDetails.vue` | ui-check ('Version 2 of ... was created.') |
| SIMPL-18190 AC1 | Next number assigned and returned | `VocabularyService#createVersion` | `VocabularyLifecycleApiTest`, `ConcurrencyApiTest` |
| SIMPL-18190 AC2 | `version` form field: 400, nothing created (D7) | `VocabularyController#createVocabularyVersion` | `VocabularyLifecycleApiTest.manualVersionFieldIsRejected` |
| SIMPL-18191 AC1 | New version ACTIVE, every older ACTIVE version DEPRECATED (D4) | `VocabularyRepository#insertVersion` | `VocabularyLifecycleApiTest.versionStatusChangeAllowsTwoActiveVersions` |
| SIMPL-18193 AC1 | UI confirms success | UI | ui-check |
| SIMPL-18193 AC2 | UI report with nature and location of each error; flow blocked | UI `VocabularyForm.vue` | `VocabularyForm.spec.ts` |
| SIMPL-18194 AC1 | 400 report with all errors, detail and location | BE `ContentValidation` | `ValidationApiTest` |
| SIMPL-18194 AC2 | Success: 201 without error details, `validationWarnings` always present (D10) | `Representations#created` | `VocabularyLifecycleApiTest` |
| SIMPL-18195 AC1 | Admin sees create, update, status change | UI `isAdmin()` checks | `VocabularyList.spec.ts`, ui-check |
| SIMPL-18195 AC2 | Without admin role: actions hidden; form URL shows 'You are not authorized to perform this action.' (D18) | UI | `VocabularyForm.spec.ts`, ui-check (viewer) |
| SIMPL-18195 AC3 | Not authenticated: Keycloak login | UI `auth.ts` (login-required) | ui-check (login) |
| SIMPL-18196 AC1 to AC4 | Token and role on every operation; 401, 403; default deny; health public (D16, D17, D84) | `security/*`, `web/ApiOperation` | `AccessAndOrderApiTest`, `HandlerRulesTest`, `TokenValidationTest`, smoke |
| SIMPL-18452 AC1 | Select a version: panel | UI `VocabularyDetails.vue` | `VocabularyDetails.spec.ts` |
| SIMPL-18452 AC2 | Download of the selected version ('<Name>-v<n>.ttl') | UI `api.ts#download`; BE `ContentController` | `VocabularyLifecycleApiTest.downloadsReturnStoredBytes` |
| SIMPL-18559 AC1 | Deprecated version: 'This version is deprecated.' + 'Latest ACTIVE version: <n>' link, never to itself (D36, D64) | UI `VocabularyDetails.vue#recommended` | `VocabularyDetails.spec.ts` |
| SIMPL-18559 AC2 | No ACTIVE version or DEPRECATED vocabulary: first sentence only | same | `VocabularyDetails.spec.ts` |
| SIMPL-393 | Met by the generic upload; no seed data (D1) | create operation | `VocabularyLifecycleApiTest` |
| SIMPL-395 | Not built: replaced by the SIMPL-181xx stories (D1) | none | none |
| SIMPL-28680 | Not built: out of scope (D1, D61) | none | none |

## Decisions

| Decision | Where | Own tests |
|---|---|---|
| D1, D2, D61 | scope as above; CDM in `docs/architecture.md` | n/a |
| D3, D4 | `VocabularyService`, `VocabularyRepository`; UI effectively deprecated | `VocabularyLifecycleApiTest`, `VocabularyServiceTest` |
| D5 | `Representations` (latestVersionHref always filled) | `VocabularyLifecycleApiTest` |
| D6, D8 | one store transaction per request | `ValidationApiTest`, `PhysicalModelShapesTest` |
| D7 | version field rejected | `VocabularyLifecycleApiTest.manualVersionFieldIsRejected` |
| D9, D53, D65 | `TurtleSyntaxValidator`, `SemanticValidator` | `ValidationRulesTest`, `ValidationApiTest` |
| D10, D11, D12, D47, D69 | `ReferenceChecker`, `ValidationWarning` | `ValidationRulesTest`, `ValidationApiTest` |
| D13, D62, D76, D101 | `ui/src/turtleCheck.ts`, `VocabularyForm.vue` | `VocabularyForm.spec.ts` |
| D14, D90, D109, D113 | `VocabularyForm.vue`, `WarningsPanel.vue`, `notice.ts` | `VocabularyForm.spec.ts`, `VocabularyDetails.spec.ts` |
| D15, D70, D76, D110 | `MultipartForm`, `JsonBody`, `MediaTypeFilter`, Tomcat limits | `AccessAndOrderApiTest` |
| D16, D17, D84, D40, E1, E5, E7 | `security/*`, `ApiOperation`, realm import | `AccessAndOrderApiTest`, `HandlerRulesTest`, `TokenValidationTest`, smoke (noaud, simpl-other checked by hand, see BUILD-LOG) |
| D18, D60, D72, D75, D100, D114, D115 | UI views and `texts.ts` | UI specs, ui-check |
| D19, D20, D21, D22, D23, D58, D59, D78, D111, D112 | `VocabularyController`, `Paging`, `QueryParameters`, `FieldRules` | `ListApiTest`, `RequestRulesTest` |
| D24, D49, D54, D55, D66 | `Problems`, `Checks` | `AccessAndOrderApiTest`, `SearchAndExternalApiTest` |
| D25 to D29, D31 | `Representations`, `Dtos` | `VocabularyLifecycleApiTest`, `DecidedContractTest` |
| D30, D41, D42, D56, D73 | `ExternalVocabularyService`, `ExternalVocabularyController` | `SearchAndExternalApiTest` |
| D32, D33, D46 | UI form; `VocabularyService#createVersion` | `VocabularyForm.spec.ts`, `VocabularyLifecycleApiTest.identicalVersionIsRefusedWith409` |
| D34 to D37, D50, D64, D74 | UI | UI specs |
| D38, D68 | `SearchService`, controllers | `SearchAndExternalApiTest` |
| D39, D79, D87, D91, D93, D116 | `docs/openapi-decided.yaml`, `scripts/make-decided-openapi.py` | `DecidedContractTest` |
| D43, D82, D92 | conditional updates, `UploadLimiter` | `ConcurrencyApiTest`, `VocabularyServiceTest` |
| D44, D71, D88, D103, D108 | `SparqlStore`, `RealmKeyProvider`, `HealthController`, breakers | `StoreDownApiTest`, `SparqlStoreTest`, `TokenValidationTest` |
| D45 | no events published (pull only) | n/a |
| D48, D104 | `Problems`, `ProblemException` | all API tests |
| D51, D63 | filter order, controller order | `AccessAndOrderApiTest` |
| D52 | unknown fields and parameters | `AccessAndOrderApiTest`, `ListApiTest`, `SearchAndExternalApiTest`; empty part name: not met (DEVIATIONS) |
| D57 | base IRI | `ValidationRulesTest.relativeIrisResolve...` |
| D67, D77 | `RoutingFilter`, firewall, nginx 405 map | `AccessAndOrderApiTest`, `ApiOperationTest` |
| D80, D86, D96, D105 | `CorrelationFilter`, structured logging, `OutboundHeaders` | log output checked by hand (BUILD-LOG) |
| D81 | withdrawn by D82 | n/a |
| D83, D98, D107 | `fuseki/config.ttl`, compose networks | stack check (BUILD-LOG) |
| D85 | every query has LIMIT | `VocabularyRepository`, `ExternalVocabularyRepository` (code review) |
| D89, Q3 | `scripts/backup.sh`, `restore.sh` | `scripts/backup-restore-test.sh` |
| D94, D108 | Jena PSS and update objects; Nimbus with own timing | code |
| D95 | Spotless check in `mvn verify`; ESLint, Prettier and Vitest in `npm run build` | build |
| D97, D117 | `LICENSE`, `docs/glossary.md`, LDM in `docs/architecture.md` | n/a |
| D99, Q1 | `scripts/load-test.py` | `security/load-test-result.txt` |
| D102 | 2 backend replicas behind the proxy | smoke on the 2-replica stack |
| D106 | content chunks in the store | `VocabularyLifecycleApiTest.largeContentIsChunked...` |
| E2, E3, E4, E6 | `application.yml`, compose | n/a |
