# TRACEABILITY.md

Stories, acceptance criteria and decisions mapped to code and to the build's own tests.

Criterion ids: stories that number their criteria keep those numbers. For the others, ACn is the n-th Given block
of the story, in the order of the file (the same convention the decisions use, for example SIMPL-18173/AC6).

Paths are short: `be/` = `backend/src/main/java/eu/simpl/vocabularymanager/`, `bt/` = the same under
`backend/src/test/java/`, `ui/` = `ui/src/`, `ut/` = `ui/tests/`. Test names are method or `it` names.

## Stories

| Story | AC | Code | Own tests |
|---|---|---|---|
| SIMPL-18161 View all vocabularies (UI) | AC1 list with name, latest version, status, dates, actions | `ui/views/VocabularyList.vue`, `ui/api.ts` | `ut/list.spec.ts` shows rows... |
| | AC2 empty state and load error (D34) | same | `ut/list.spec.ts` shows the empty state..., shows the load error... |
| SIMPL-18162 List metadata (API) | AC1 200 paged list with metadata | `be/api/VocabularyController.java` list, `be/store/FusekiVocabularyRepository.java`, `be/api/Representations.java` | `VocabularyApiTest.listFiltersSortsAndPages` |
| | AC2 wrong paging gives 400 | `be/api/QueryParsers.java` | `RequestParsingTest.rejectsInvalidValues`, `VocabularyApiTest.invalidQueryParametersAreRejected` |
| | AC3 no role gives 403 | `be/web/RouteTable.java`, `be/security/SecurityConfig.java` | `VocabularyApiTest.accessRulesAndTokenChecks` |
| | AC4 internal error gives 500 problem | `be/api/ApiExceptionHandler.java` | `RequestParsingTest.problemsHaveDecidedTypes`; 503 path in `StoreDownTest.storeDownGives503` |
| SIMPL-18163 Sort and filter (UI) | AC1 filters 'Name contains', 'Vocabulary status', 'Created on' (D72) | `ui/views/VocabularyList.vue` | `ut/list.spec.ts` shows rows... |
| | AC2 sortable columns, D50 click rule | same | `ut/list.spec.ts` ...sorts as D50 says |
| SIMPL-18164 Filters and sorting (API) | AC1 to AC4 filters, sort, invalid values, 403 | `be/api/QueryParsers.java`, `be/store/FusekiVocabularyRepository.java` (ORDER BY, LIMIT, OFFSET, COUNT, D85) | `VocabularyApiTest.listFiltersSortsAndPages`, `invalidQueryParametersAreRejected`, `RequestParsingTest.parsesFilters`, `pagingLinks` |
| SIMPL-18165 Download latest and view details (UI) | AC1 | `ui/views/VocabularyDetails.vue`, `ui/components/ActionsMenu.vue` | `ut/details.spec.ts` shows the history... |
| SIMPL-18166 Version details (API) | AC1 200 version metadata, AC2 404 | `be/api/VocabularyController.java`, `be/api/ContentController.java` | `VocabularyApiTest.downloadsReturnTheBytesAsUploaded`, `VocabularyServiceTest.notFoundDistinguishesVocabularyAndVersion` |
| SIMPL-18167 Version history (UI) | AC1 | `ui/views/VocabularyDetails.vue` (pageSize 100, further pages, D35) | `ut/details.spec.ts` |
| SIMPL-18168 Version history (API) | AC1 200 newest first, AC2 404 | `be/api/VocabularyController.java` versions | `VocabularyApiTest.newVersionDeprecatesOlderActiveVersions`, `listFiltersSortsAndPages` |
| SIMPL-18169 Create vocabulary (UI) | AC1 access to the form (admin only, D18) | `ui/views/VocabularyForm.vue`, `ui/router.ts` (route /vocabularies/new, D74) | `ut/form.spec.ts` refuses a user without the admin role |
| | AC2 validation and submit | same, `ui/components/ValidationReport.vue` | `ut/form.spec.ts` shows the validation report..., keeps Submit enabled with field errors... |
| SIMPL-18170 Create vocabulary (API) | AC1 201 with Location, AC2 400, AC3 409 | `be/api/VocabularyController.java`, `be/service/VocabularyService.java` | `VocabularyApiTest.createsVocabularyWithVersionOneActive`, `duplicateNameIsConflictCaseInsensitively`, `VocabularyServiceTest.createStoresVersionOne` |
| SIMPL-18172 New version (UI) | AC1 open the pre-filled form (D33) | `ui/views/VocabularyForm.vue` (route /vocabularies/<Name>/versions/new) | `ut/form.spec.ts` pre-fills the new version form... |
| | AC2 submit creates the version (D46 line, no confirmation) | same | same |
| | AC3 errors (D24, D60) | same | `ut/form.spec.ts` shows the validation report of a 400 and the 409 detail |
| SIMPL-18173 New version (API) | AC1 201 version n+1, AC2 400, AC3 403, AC4 404, AC5 413, AC6 415 | `be/api/VocabularyController.java`, `be/api/RequestChecks.java`, `be/api/MultipartForm.java` | `VocabularyApiTest.newVersionDeprecatesOlderActiveVersions`, `orderOfChecksOnWrites`, `accessRulesAndTokenChecks` |
| SIMPL-18174 Status change (UI) | AC1 Deprecate/Activate, AC2 404 text, AC3 error text (D60, D72) | `ui/components/ActionsMenu.vue`, `ui/views/VocabularyList.vue` | `ut/list.spec.ts` changes the status from the actions menu |
| SIMPL-18175 Status change (API) | AC1 200, AC2 409 same status, AC3 400/404 | `be/service/VocabularyService.java` status change | `VocabularyApiTest.statusChangesAndConflicts`, `VocabularyServiceTest.statusChanges` |
| SIMPL-18176 Deprecated versions (UI) | AC1 access, AC2 browse the history, AC3 version panel metadata | `ui/views/VocabularyDetails.vue` | `ut/details.spec.ts` |
| SIMPL-18178 Warning on deprecated vocabularies (UI) | AC1 icon with name and tooltip (D36, D72), banner | `ui/views/VocabularyList.vue`, `ui/views/VocabularyDetails.vue` | `ut/list.spec.ts` ...the deprecation icon name..., `ut/details.spec.ts` shows no recommendation... |
| SIMPL-18180 Syntax and structure validation (UI) | AC1 to AC3 check on file choice in the browser (D13) | `ui/turtleCheck.ts`, `ui/views/VocabularyForm.vue` | `ut/form.spec.ts` checks Turtle in the browser |
| | AC4 'Syntax check passed.' | same | same |
| | AC5 syntax error with line (D60) | same | same; disables Submit on a syntax error... |
| | AC6 structure errors (server, D9) | `ui/components/ValidationReport.vue` | `ut/form.spec.ts` shows the validation report of a 400... |
| | AC7 several errors in one report | same | same |
| | AC8 fix and resubmit | `ui/views/VocabularyForm.vue` (form stays open) | same |
| SIMPL-18181 Syntax validation (API) | AC1 valid passes, AC2 one issue with line and column | `be/validation/TurtleParser.java` | `ContentRulesTest.syntaxErrorHasLineAndColumn`, `VocabularyApiTest.syntaxErrorGivesOneIssueWithLine` |
| SIMPL-18182 Semantic validation (UI) | AC1 runs on submit after syntax, AC2 success, AC3 errors | `ui/views/VocabularyForm.vue`, `ui/components/ValidationReport.vue` | `ut/form.spec.ts` |
| SIMPL-18183 Semantic validation (API) | AC1 valid, AC2 all errors S1 to S7 | `be/validation/SemanticRules.java`, `be/validation/ReferenceAnalyzer.java`, `ReferenceEvaluator.java` | `ContentRulesTest.s1ToS6`, `evaluatesReferencesAsDecided`, `VocabularyApiTest.semanticErrorsAreAllReported`, `emptyFileFailsWithNoStatements` |
| SIMPL-18184 Deprecated reference check (UI) | AC1 no warning, AC2 to AC4 warning shown, creation continues (D11, D47) | `be/validation/ReferenceAnalyzer.java`, `ReferenceEvaluator.java`, `ui/components/WarningsPanel.vue` | `ContentRulesTest.classifiesReferences`, `VocabularyApiTest.referencesGiveWarningsAndMissingReferencesFail` |
| SIMPL-18186 Automatic publication (UI) | AC1 stored and ACTIVE on success, notification (D6, D14, D72) | `ui/views/VocabularyForm.vue`, `ui/notice.ts` | `ut/form.spec.ts` |
| SIMPL-18187 Publish (API) | AC1 create, AC2 new version: 201, Location, href | `be/api/VocabularyController.java` | `VocabularyApiTest.createsVocabularyWithVersionOneActive`, `newVersionDeprecatesOlderActiveVersions` |
| SIMPL-18189 Automatic versioning (UI) | AC1 number assigned, AC2 no manual number, AC3 feedback 'Version <n> of <Name> was created.' | `ui/views/VocabularyForm.vue` | `ut/form.spec.ts` |
| SIMPL-18190 Automatic versioning (API) | AC1 n+1, AC2 version field rejected (D7) | `be/service/VocabularyService.java`, `be/api/RequestChecks.java` | `VocabularyApiTest.versionFieldIsRejected`, `parallelVersionCreationsAllSucceed`, `StoreModelTest.twoInstancesOnOneStoreKeepVersionNumbersUnique` |
| SIMPL-18191 Automatic deprecation (UI) | AC1 older versions shown DEPRECATED | `ui/views/VocabularyDetails.vue` | `ut/details.spec.ts` |
| SIMPL-18193 Validation report (UI) | AC1 report region (D90), AC2 success | `ui/components/ValidationReport.vue`, `WarningsPanel.vue` | `ut/form.spec.ts`, `ut/a11y.spec.ts` |
| SIMPL-18194 Validation report (API) | AC1 400 report with issues, AC2 201 with validationWarnings | `be/api/Problems.java`, `be/api/Representations.java` | `VocabularyApiTest.semanticErrorsAreAllReported`, `referencesGiveWarningsAndMissingReferencesFail` |
| SIMPL-18195 Access control (UI) | AC1 admin sees actions, AC2 viewer does not, AC3 direct URL message (D18) | `ui/auth.ts`, `ui/router.ts`, views | `ut/list.spec.ts` ...hides admin actions for a viewer, `ut/form.spec.ts` refuses a user without the admin role |
| SIMPL-18196 Access control (API) | AC1 to AC4: 401 no or bad token, wrong audience or issuer (E5, E7), 403 role | `be/security/*`, `be/web/RouteTable.java` | `VocabularyApiTest.accessRulesAndTokenChecks`, `DefaultDenyTest`, `RealmKeySourceTest` |
| SIMPL-18452 Download any version (UI) | AC1, AC2 | `ui/views/VocabularyDetails.vue`, `ui/components/ActionsMenu.vue` | `ut/details.spec.ts` |
| SIMPL-18559 Warning on deprecated versions (UI) | AC1 warning with latest ACTIVE link, AC2 without (D36, D64) | `ui/views/VocabularyDetails.vue` | `ut/details.spec.ts` shows the history with deprecation warnings..., shows no recommendation... |
| SIMPL-393 CPS vocabulary | met by the generic upload, no seed data (D1) | as SIMPL-18170 | `smoke.sh` |
| SIMPL-395 | not built: replaced by the SIMPL-181xx stories (D1) | | |
| SIMPL-28680 | not built: Schema Manager, out of scope (D1, D61) | | |

## Decisions

| Decision | Code | Own tests |
|---|---|---|
| D1, D61 scope | only the operations of the vocabulary manager OpenAPI | `ContractConformanceTest.everyOperationHasARouteWithItsOperationId` |
| D2, D87, D97 CDM, LDM, physical model, decided contract | `docs/logical-data-model.md`, `docs/physical-model.md`, `docs/shapes/`, `docs/openapi-decided.yaml`, `docs/glossary.md`, `LICENSE` (EUPL 1.2) | `StoreModelTest.storedDataConformsToTheShapes`, `ContractConformanceTest` |
| D3, D4 statuses and transitions | `be/service/VocabularyService.java`, `be/domain/` | `VocabularyServiceTest.statusChanges`, `versionStatusChanges`, `VocabularyApiTest.statusChangesAndConflicts` |
| D5 latest version, latestVersionHref | `be/api/Representations.java` | `VocabularyApiTest.newVersionDeprecatesOlderActiveVersions` |
| D6 publish on success | `VocabularyService` | `VocabularyServiceTest.createStoresVersionOne` |
| D7 version numbers, version field | `VocabularyService`, `RequestChecks` | `VocabularyApiTest.versionFieldIsRejected` |
| D8 nothing stored on failure | one conditional update per write in `FusekiVocabularyRepository` | `VocabularyServiceTest.validationErrorsStoreNothing` |
| D9, D53, D65 rules and locations | `be/validation/` | `ContentRulesTest.s1ToS6`, `syntaxErrorHasLineAndColumn` |
| D10 errors and warnings | `be/validation/`, `be/api/Representations.java` | `VocabularyApiTest.referencesGiveWarningsAndMissingReferencesFail` |
| D11, D12, D47, D57, D69 references | `be/validation/ReferenceAnalyzer.java`, `ReferenceEvaluator.java`, one store query per upload (Q5) | `ContentRulesTest.classifiesReferences`, `evaluatesReferencesAsDecided`, `parsesPrefixesSubjectsAndRelativeIris` |
| D13, D60, D62, D76 browser check | `ui/turtleCheck.ts`, `ui/views/VocabularyForm.vue` | `ut/form.spec.ts` |
| D14, D72, D90 report, notification, regions | `ui/components/ValidationReport.vue`, `WarningsPanel.vue`, `ui/notice.ts` | `ut/form.spec.ts`, `ut/a11y.spec.ts` |
| D15, D76 size and media types | `be/api/MultipartForm.java`, `RequestChecks` | `VocabularyApiTest.orderOfChecksOnWrites`, `RequestParsingTest.checksAcceptAndContentType` |
| D16, D84 roles, default deny | `be/web/RouteTable.java`, `SecurityConfig` | `DefaultDenyTest.everyHandlerHasARoleRule`, `VocabularyApiTest.accessRulesAndTokenChecks` |
| D17, D40, E5, E7 401, 403, issuer, audience | `be/security/` | `VocabularyApiTest.accessRulesAndTokenChecks` |
| D18 UI restricted actions | `ui/auth.ts`, `ui/router.ts` | `ut/form.spec.ts`, `ut/list.spec.ts` |
| D19, D20, D21, D22, D52, D59, D78, D91 query parameters, paging, links | `be/api/QueryParsers.java` | `RequestParsingTest.*`, `VocabularyApiTest.invalidQueryParametersAreRejected` |
| D23, D50, D58 sorting, times | `FusekiVocabularyRepository`, `ui/views/VocabularyList.vue` | `VocabularyApiTest.listFiltersSortsAndPages`, `ut/list.spec.ts` |
| D24 status codes | `ApiExceptionHandler` | API tests |
| D25, D26, D27, D28 objects, creator, description | `Representations`, `be/security/CurrentUser.java` | `VocabularyApiTest.createsVocabularyWithVersionOneActive` |
| D29, D70 downloads | `be/api/ContentController.java`, `be/store/ContentChunks.java` | `VocabularyApiTest.downloadsReturnTheBytesAsUploaded` |
| D30, D41, D42, D56, D66, D73 external vocabularies | `be/api/ExternalVocabularyController.java`, `be/service/ExternalVocabularyService.java`, `FusekiExternalVocabularyRepository` | `ExternalVocabularyApiTest.*`, `VocabularyServiceTest.externalService`, `ContentRulesTest.uriKeys` |
| D31 terms | code, API, UI texts use the glossary terms | review |
| D32, D33, D35, D36, D64, D74, D75 UI forms and details | `ui/views/` | `ut/*.spec.ts` |
| D34 list paging and states | `ui/views/VocabularyList.vue` | `ut/list.spec.ts` |
| D37 field rules | `be/api/FieldRules.java`, `ui/views/VocabularyForm.vue` | `VocabularyApiTest.orderOfChecksOnWrites`, `ut/form.spec.ts` |
| D38, D68 searches | `be/api/SearchController.java` | `VocabularyApiTest.searchResolvesReferencesInOrder`, `ExternalVocabularyApiTest.registerFindSearchDownloadAndDelete`, `VocabularyServiceTest.searchDropsRepeatedReferences` |
| D39, D79 decided contract | `docs/openapi-decided.yaml`, `docs/tools/make_decided_openapi.py` | `ContractConformanceTest.*` |
| D43, D82 concurrency through the store | conditional updates with a write token, retry at most 5 times | `VocabularyApiTest.parallelVersionCreationsAllSucceed`, `StoreModelTest.twoInstancesOnOneStoreKeepVersionNumbersUnique`, `VocabularyServiceTest.newVersionRetriesWhenTheLatestVersionMoved`, `newVersionGivesUpAfterTheRetries`, `createLosingTheRaceIsConflict` |
| D44, D71, D88 store and identity provider down | `be/store/FusekiSparqlStore.java`, `be/security/RealmKeySource.java`, `HealthController` | `StoreDownTest.storeDownGives503`, `RealmKeySourceTest.*`; stack: `./stack.sh store-down-up` |
| D45 pull only | no events published | n/a |
| D46 identical version | `VocabularyService` | `VocabularyApiTest.identicalVersionIsConflictAndDescriptionOnlyChangeIsAllowed`, `VocabularyServiceTest.identicalVersionIsRefusedBeforeValidation` |
| D48 problem types | `be/api/Problems.java` | `RequestParsingTest.problemsHaveDecidedTypes` |
| D49, D54 path values and case | `RequestChecks` | `RequestParsingTest.pathValues` |
| D51, D63 order of checks, 406 | controllers, `RequestChecks` | `VocabularyApiTest.orderOfChecksOnWrites`, `RequestParsingTest.checksAcceptAndContentType` |
| D55 fixed texts | `Problems` | API tests |
| D67, D77, D98 before routing, management port | `be/web/RouteFilter.java`, `ProblemErrorReportValve`, `application.yaml`, `compose.yaml` | `VocabularyApiTest.requestsRejectedBeforeRouting`, `DefaultDenyTest.fixedSegmentsWin` |
| D80, D86, D96 logs, metrics, trace | `be/web/CorrelationFilter.java`, `WebConfig`, `application.yaml` | `VocabularyApiTest.responsesCarryCorrelationId` |
| D81 | withdrawn by D82 | |
| D83 store access | `fuseki/config.ttl`, `fuseki/entrypoint.sh`, `compose.yaml` | stack; `ops/backup-restore-test.sh` |
| D85 bounded queries | `FusekiVocabularyRepository`, `FusekiExternalVocabularyRepository` | `VocabularyApiTest.listFiltersSortsAndPages` |
| D89, Q3 backup and restore | `ops/backup.sh`, `ops/restore.sh`, `ops/backup.cron.example`, `ops/backup-cronjob.example.yaml`, `fuseki/ops/StoreOps.java` | `ops/backup-restore-test.sh` (passed this run) |
| D92 upload slots | `be/service/UploadSlots.java` | `VocabularyServiceTest.fifthUploadIsRefused` |
| D93, D94 request validation, libraries | see REUSE.md | `ContractConformanceTest` |
| D95 lint, format, UI tests | Spotless in `pom.xml`, `ui/package.json` build script | `mvn verify`, `npm run build` |
| D99, Q1, Q4 load | `perf/measure.py` | see QUALITY.md (not the full D99 seed) |
| Q2 limits | see QUALITY.md | API tests at and above each limit |
| Q5 coverage and scan | JaCoCo, Trivy | QUALITY.md, SECURITY-SCAN.md |
| Q6 accessibility | `ui/` | `ut/a11y.spec.ts` |
| E1 to E7 environment | `keycloak/realm-simpl.json`, `keycloak/realm-simpl-other.json`, `compose.yaml`, `application.yaml` (all values from the environment) | stack, `smoke.sh` |

## Not implemented

- SIMPL-395 and SIMPL-28680: out of scope (D1, D61).
- `semanticReasoningSkipped` warning: reserved, not emitted (D10).
- The full D99 load test (seed of 20 000 versions, 5 minutes): not run; a shorter own measurement is in QUALITY.md.
