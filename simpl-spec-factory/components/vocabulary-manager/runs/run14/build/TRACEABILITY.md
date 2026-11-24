# Traceability

Code paths are relative to `backend/src/main/java/eu/simpl/vocabularymanager/` (BE) and `ui/src/` (UI). Tests:
backend `backend/src/test/java/...` (class.method), UI `ui/tests/*.spec.ts`, and `smoke.sh`. Acceptance criteria
are numbered in the order of the scenarios in each story (AC1 is the first Given/When/Then).

## Stories in scope (D1)

| Story | AC | Implementation | Own tests |
|---|---|---|---|
| SIMPL-18161 View all vocabularies (UI) | AC1 list with actions menu, AC2 load error | UI views/ListView.vue, components/ActionsMenu.vue (D34, D72, D100, D159) | ListView.spec (columns, empty state, load error, actions menu) |
| SIMPL-18162 Metadata of all vocabularies (API) | AC1 200 paged list, AC2 400 wrong pagination, AC3 403, AC4 500 | BE web/VocabularyController.getVocabularies, service/VocabularyService.list, error/ProblemHandler; AC4 by handler code (D179) | VocabularyApiTest.listFiltersSortsAndPages, listParameterErrors; AccessApiTest.tokenAndRoleChecks; StoreBehaviourTest.storeRefusingTheUserIs500 (500 path) |
| SIMPL-18163 Sort and filter (UI) | AC1 filters, AC2 sort | UI ListView.vue (filters 'Name contains', 'Vocabulary status', 'Created on'; sort buttons with aria-sort, D50, D163) | ListView.spec (sort reversal and request) |
| SIMPL-18164 Filters and sorting (API) | AC1 filter and sort, AC2 empty result, AC3 paging with sort, AC4 invalid parameters 400 | BE VocabularyController, RequestRules, Paging, VocabularyRepository.list/count (D19 to D23, D78, D85, D111) | VocabularyApiTest.listFiltersSortsAndPages, listParameterErrors; WebRulesTest |
| SIMPL-18165 Download newest version (UI) | AC1 | UI ListView 'Download vocabulary', DetailsView 'Download vocabulary'; BE ContentController.downloadVocabulary | VocabularyApiTest.createAndVersionLifecycle; smoke.sh |
| SIMPL-18166 Full details of a version (API) | AC1 metadata, AC2 404 | BE VocabularyController.getVocabularyVersion, ContentController (D26, D29) | VocabularyApiTest.createAndVersionLifecycle; AccessApiTest.routingAnswers |
| SIMPL-18167 Version history (UI) | AC1 details page | UI DetailsView.vue (D35, D163) | DetailsView.spec |
| SIMPL-18168 Version history (API) | AC1 list, AC2 404 | BE getVocabularyVersions (D23, D154, D196) | VocabularyApiTest.createAndVersionLifecycle, listFiltersSortsAndPages |
| SIMPL-18169 Create vocabulary (UI) | AC1 form and required fields, AC2 validation of input | UI CreateView.vue, lib/forms.ts (D32, D37, D62, D101, D141) | CreateView.spec |
| SIMPL-18170 Create vocabulary (API) | AC1 validates, AC2 version 1 ACTIVE, AC3 rejects invalid data | BE createVocabulary, VocabularyService.create, VocabularyRepository.insertVocabulary | VocabularyApiTest.createAndVersionLifecycle, createFieldRules; smoke.sh |
| SIMPL-18172 New version (UI) | AC1 pre-filled form, AC2 creates version, AC3 error handling | UI NewVersionView.vue (D33, D46, D113, D180) | NewVersionView.spec |
| SIMPL-18173 New version (API) | AC1 creates and deprecates older, AC2 invalid data 400, AC3 unknown vocabulary 404, AC4 403, AC5 401, AC6 unsupported format 415 | BE createVocabularyVersion, VocabularyService.createVersion, VocabularyRepository.insertVersion, GatekeeperFilter | VocabularyApiTest (lifecycle, createFieldRules, parallel); AccessApiTest.negotiationAndOrder; smoke.sh |
| SIMPL-18174 Status change (UI) | AC1 change, AC2 404 text, AC3 error text | UI ListView/DetailsView with ConfirmDialog (D60, D72, D114, D209) | ListView.spec (confirmation and success) |
| SIMPL-18175 Status change (API) | AC1 change, AC2 404, AC3 400 unsupported status | BE updateVocabularyStatus, VocabularyService.changeStatus (D3, D4) | VocabularyApiTest.statusChangesFollowTheStateTables |
| SIMPL-18176 Deprecated versions (UI) | AC1 access, AC2 display, AC3 metadata | UI DetailsView.vue version table and panel | DetailsView.spec |
| SIMPL-18178 Warning on deprecated vocabularies (UI) | AC1 | UI ListView warning icon, DetailsView banner (D36, D72, D115) | ListView.spec, DetailsView.spec |
| SIMPL-18180 Syntax and structure validation (UI) | AC1 on upload, AC2 and AC3 on submit, AC4 success message, AC5 syntax errors, AC6 structure errors, AC7 all errors, AC8 blocked until fixed | UI components/FileField.vue (N3.js, D13), ValidationReport.vue, CreateView/NewVersionView; BE ContentValidator | CreateView.spec (browser check), NewVersionView.spec (report); ValidationApiTest |
| SIMPL-18181 Syntax validation (API) | AC1 passes, AC2 syntax errors 400 | BE validation/TurtleSyntax, ContentValidator | ValidationApiTest.syntaxErrorIsOneIssueWithLineAndColumn; ValidationRulesTest |
| SIMPL-18182 Semantic validation (UI) | AC1 after syntax, AC2 success, AC3 report | UI ValidationReport.vue, WarningsPanel.vue | NewVersionView.spec |
| SIMPL-18183 Semantic validation (API) | AC1 passes, AC2 fails with all errors | BE validation/SemanticRules, References, ContentValidator (S1 to S7) | ValidationApiTest.allSemanticErrorsInOneReport, referencesGiveErrorsAndWarnings; ValidationRulesTest |
| SIMPL-18184 Deprecated references on new version (UI) | AC1 to AC3 status check, AC4 warning shown | BE ContentValidator.internalOutcome (D11, D47); UI WarningsPanel on the details page (D14) | ValidationApiTest.referencesGiveErrorsAndWarnings |
| SIMPL-18186 Automatic publication (UI) | AC1 ACTIVE after validation | BE create and new version set ACTIVE (D6); UI success notice | VocabularyApiTest.createAndVersionLifecycle |
| SIMPL-18187 Publish via API | AC1 create persisted, AC2 version persisted | BE repositories (one transaction per write, D8) | VocabularyApiTest.createAndVersionLifecycle; StoreBehaviourTest.storedDataConformsToTheShapes |
| SIMPL-18189 Automatic versioning (UI) | AC1 next number, AC2 no manual input, AC3 confirmation with number | UI NewVersionView ('Submitting creates version n+1'), notice 'Version n of Name was created.' | NewVersionView.spec |
| SIMPL-18190 Automatic versioning (API) | AC1 next number, AC2 version field 400 | BE VocabularyService.createVersion, MultipartForm.checkShape (D7) | VocabularyApiTest.createFieldRules, parallelVersionsGetDistinctNumbers |
| SIMPL-18191 Automatic deprecation (UI) | AC1 | BE VocabularyRepository.insertVersion (older ACTIVE to DEPRECATED, D4) | VocabularyApiTest.parallelVersionsGetDistinctNumbers; smoke.sh |
| SIMPL-18193 Validation report (UI) | AC1 success, AC2 report | UI ValidationReport.vue (D90, D109), WarningsPanel.vue | NewVersionView.spec |
| SIMPL-18194 Validation report (API) | AC1 report with locations, AC2 success with warnings | BE ContentValidator, VersionResponse.validationWarnings (D10, D14) | ValidationApiTest |
| SIMPL-18195 Access control (UI) | AC1 admin actions, AC2 restricted hidden, AC3 login | UI lib/auth.ts (login-required), isAdmin checks in views (D18) | ListView.spec, CreateView.spec, ExternalViews.spec |
| SIMPL-18196 Access control (API) | AC1 processed, AC2 403, AC3 401, AC4 all endpoints protected | BE GatekeeperFilter, JwtAuthenticator, Operation table, HandlerRuleVerifier (D16, D17, D84) | AccessApiTest; ContractTest; RealmKeyCacheTest; smoke.sh |
| SIMPL-18452 Details and download of all versions (UI) | AC1 access version, AC2 download | UI DetailsView version panel and 'Download' | DetailsView.spec |
| SIMPL-18559 Deprecated version warnings (UI) | AC1 warning with link to latest ACTIVE, AC2 warning without | UI DetailsView activeLinkFor (D36, D64, D118) | DetailsView.spec |

## Out of scope (D1, D61)

| Story | Reason |
|---|---|
| SIMPL-28680 | Belongs to Schema Manager (D1, D61) |
| SIMPL-393 | Met by the generic upload; no seed data (D1) |
| SIMPL-395 | Replaced by the SIMPL-181xx stories (D1) |

## Decisions

| Decisions | Implementation | Tests |
|---|---|---|
| D1, D2, D61 | scope as above; data model traced to the CDM (docs/data-model.md) | |
| D3, D4, D160, D208 | VocabularyService.changeStatus/changeVersionStatus, VocabularyRepository.update*Status; effectively deprecated in ContentValidator and DetailsView | VocabularyApiTest.statusChangesFollowTheStateTables |
| D5, D25, D26, D28, D184 | ApiMapper, VocabularyRepository (latestVersion, latestDescription) | VocabularyApiTest.createAndVersionLifecycle, searchFollowsD38 |
| D6, D7, D8 | VocabularyService.create/createVersion; MultipartForm.checkShape; one SPARQL update per write | VocabularyApiTest |
| D9, D10, D53, D65, D125, D140 to D145, D171, D181, D199 | TurtleSyntax, SemanticRules, ContentValidator, ApiException (truncation) | ValidationApiTest, ValidationRulesTest |
| D11, D12, D47, D69, D146, D192, D198 | References, ContentValidator.internalOutcome | ValidationApiTest.referencesGiveErrorsAndWarnings, ValidationRulesTest.referencesAreClassified |
| D13, D60, D62, D76, D101, D141, D200 | UI FileField.vue, CreateView, NewVersionView, ExternalRegisterView | CreateView.spec |
| D14, D72, D90, D109, D113, D114, D180 | UI notices.ts, ValidationReport, WarningsPanel, DetailsView | NewVersionView.spec, DetailsView.spec |
| D15, D70, D76, D150, D169 | GatekeeperFilter.checkSize, MultipartForm, JsonBody | VocabularyApiTest.createFieldRules, AccessApiTest.negotiationAndOrder |
| D16, D17, D18, D84, D197 | Operation, GatekeeperFilter, JwtAuthenticator, HandlerRuleVerifier; UI isAdmin | AccessApiTest, ContractTest |
| D19 to D23, D50, D58, D78, D111, D128, D130, D154, D168, D196 | RequestRules, QueryParameters, Paging, VocabularyController sort maps | VocabularyApiTest.list*, WebRulesTest |
| D24, D49, D54, D55, D66 | RequestRules path checks, not-found texts in services | AccessApiTest.routingAnswers, ExternalApiTest |
| D27, D80, D165 | AuthenticatedUser.creator, logs with subject only | RealmKeyCacheTest.authenticatorReadsCreatorAndRoles |
| D29, D70 | ContentController.turtle | VocabularyApiTest.createAndVersionLifecycle |
| D30, D41, D42, D56, D73, D124, D137, D147, D148 | ExternalVocabularyController, ExternalVocabularyService, ExternalVocabularyRepository | ExternalApiTest |
| D31, D74, D75, D100, D115, D159, D163 | UI router.ts and views (labels, roles, routes) | UI specs |
| D32, D33, D46, D161, D194 | UI CreateView (changelog 'Initial version.'), NewVersionView; BE identical check by SHA-256 | VocabularyApiTest.createAndVersionLifecycle, NewVersionView.spec |
| D34, D35, D36, D64, D118 | UI ListView, DetailsView | ListView.spec, DetailsView.spec |
| D37, D195 | RequestRules field rules, MultipartForm repeated fields | VocabularyApiTest.createFieldRules |
| D38, D68, D110, D153, D182, D205 | VocabularyController.searchVocabularies, ExternalVocabularyController.searchExternalVocabularies, Paging.searchPage | VocabularyApiTest.searchFollowsD38, ExternalApiTest |
| D39, D79, D87, D91, D116, D203 | docs/openapi-decided.yaml (scripts/make_decided_openapi.py) | ContractTest |
| D40, E1, E5, E7, E8 | keycloak/realm-*.json; JwtAuthenticator | AccessApiTest; smoke.sh |
| D43, D71, D82, D119, D157, D176 | ConditionalWrite, conditional updates with receipts in the repositories, receipt cleanup | VocabularyApiTest.parallelVersionsGetDistinctNumbers, StoreBehaviourTest.twoInstancesOnOneStoreNeverReuseANumber |
| D44, D88, D103, D108, D129, D134, D152, D174, D201 | StoreClient, RealmKeyCache, HealthController, AppConfig breakers, compose health checks | StoreBehaviourTest, RealmKeyCacheTest, AccessApiTest.healthNeedsNoTokenAndKeepsItsRules; stack.sh store-down-up (manual check in BUILD-LOG) |
| D45 | no events | |
| D48, D104, D158 | ProblemType, ApiException, ProblemHandler, ProblemErrorReportValve | AccessApiTest |
| D51, D63, D127, D151, D162, D170 | GatekeeperFilter, controller order | AccessApiTest.negotiationAndOrder |
| D52, D121, D138 | QueryParameters.requireOnly, MultipartForm, JsonBody.requireOnly | VocabularyApiTest, WebRulesTest |
| D57 | ContentValidator base IRI | ValidationRulesTest.prefixesKeepTheDeclaredText |
| D67, D77, D120, D126, D139, D155, D166, D172, D178, D190 | RouteMatcher, GatekeeperFilter, ui/nginx/*.conf, management port | AccessApiTest.routingAnswers, ContractTest.proxyAllowMapMatchesTheTable, smoke.sh |
| D83, D107, D193 | fuseki/config.ttl, compose (no port, internal network) | StoreBehaviourTest (uses the real config) |
| D85, D188 | VocabularyRepository (ORDER BY, LIMIT, OFFSET, COUNT in the store) | VocabularyApiTest.listFiltersSortsAndPages |
| D86, D96, D105 | TraceHeaders, GatekeeperFilter MDC, application.yml structured logging | (checked in logs, BUILD-LOG) |
| D89, D156, Q3 | scripts/backup.sh, restore.sh, compact.sh, backup-restore-test.sh | backup-restore-test.sh (QUALITY.md) |
| D92 | UploadSlots, GatekeeperFilter | (not covered by an own automated test, see BUILD-LOG) |
| D93, D94, D133, D206 | REUSE.md choices | ContractTest |
| D95 | Spotless in mvn verify; ESLint, Prettier and Vitest in the UI image build | mvn verify, ui Dockerfile |
| D97, D117 | LICENSE, docs/glossary.md, docs/data-model.md | |
| D98, D102, D189, D202 | docker-compose.yml (127.0.0.1 ports, 2 backend replicas, limits as variables) | smoke.sh on the 2-replica stack |
| D99, D136, D183, Q1 | scripts/loadtest.py | QUALITY.md |
| D106 | content in the store (VocabularyRepository) | StoreBehaviourTest.storedDataConformsToTheShapes |
| D122, D123 | ui/nginx (body limit 7 MB, CSP header) | (manual, BUILD-LOG) |
| D131 | stack.sh, .gitignore | |
| D164, D185 | DEVIATIONS.md | |
| D173 | fuseki/log4j2.properties, nginx log_format | |
| D175 | SECURITY-SCAN.md | |
| D177 | ui/UI-NOTES.md | |
| D186 | THREATS.md | |
| D187, D135 | DEVIATIONS.md | |
| D191 | QUALITY.md | |
| D207 | docs/data-model.md differences | |
| D209, D210 | UI ConfirmDialog, ExternalListView, ExternalRegisterView | ListView.spec, ExternalViews.spec |
| Q2 | limits listed in docs/api-notes.md | VocabularyApiTest.createFieldRules, listParameterErrors, AccessApiTest |
| Q4, Q5, Q6 | QUALITY.md | |
