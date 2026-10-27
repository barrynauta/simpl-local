# Traceability

Backend paths are relative to `backend/src/main/java/eu/simpl/vocabulary/`, backend tests to
`backend/src/test/java/eu/simpl/vocabulary/`, UI paths to `ui/src/`.
Own tests: `ApiIT` = Spring Boot API integration test (in-memory dataset, mocked JWT decoder, real security
chain); unit tests by class name; `e2e` = headless Chrome walkthrough `ui/e2e/check.mjs` against the running stack;
`smoke` = `smoke.sh`.

Status: **Done** = implemented and covered by an own test. **Done (untested)** = implemented, no automated own test.
**Partial** = implemented with a stated limit.

## Stories (32 in scope)

| Story | Summary | Code | Own tests | Status |
|---|---|---|---|---|
| SIMPL-18161 | UI list of vocabularies | `views/ListView.vue` (columns per D22, three-dot actions menu, error box "could not be loaded" with Problem details) | e2e "admin sees the vocabulary list" | Done |
| SIMPL-18162 | API list metadata, paging, 400, 403, 500 | `web/VocabularyController.list`, `web/RequestChecks.paging/page`, `config/SecurityConfig`, `web/ApiExceptionHandler.internal` | `ApiIT.listFilterSortAndPaging`, `invalidListParametersAre400`, `noRoleIs403OnReadAndWrite`; smoke | Done (500 path untested) |
| SIMPL-18163 | UI filter and sort | `ListView.vue` (name contains, status, dates, sortable headers) | none automated beyond list load | Done (untested) |
| SIMPL-18164 | API filter and sort | `VocabularyController.list`, `listComparator` | `ApiIT.listFilterSortAndPaging`, `invalidListParametersAre400` | Done |
| SIMPL-18165 | UI download newest version | `ListView.vue download`, `DetailsView.vue`, `api.ts download` | none automated (API download covered) | Done (untested) |
| SIMPL-18166 | API version details | `VocabularyController.version` | `ApiIT.newVersionDeprecatesPreviousAndHistoryIsListed` (200 and 404) | Done |
| SIMPL-18167 | UI version history | `views/DetailsView.vue` (name header, download, version, status, date, description, change log) | e2e "history shows v1 deprecated" | Done |
| SIMPL-18168 | API version history | `VocabularyController.versions`, `versionComparator` | `ApiIT.newVersionDeprecatesPreviousAndHistoryIsListed` | Done |
| SIMPL-18169 | UI create vocabulary | `views/CreateView.vue` (name, description, file; no version or change log), `api.ts create` | e2e create flow | Done |
| SIMPL-18170 | API create vocabulary | `VocabularyController.create`, `service/VocabularyService.create`, `domain/VocabularyLifecycle.create` | `ApiIT.createReturns201WithVersionOneActive`, `duplicateNameIs409`, `missingFieldsAndBadNameAre400WithAllIssues`; `VocabularyLifecycleTest.createMakesVersionOneActive`; smoke | Done |
| SIMPL-18172 | UI new version | `views/NewVersionView.vue` (prefilled description and content, change summary, upload, confirmation) | e2e "new version form is prefilled", history check | Done |
| SIMPL-18173 | API new version | `VocabularyController.createVersion`, `VocabularyService.createVersion`, `VocabularyLifecycle.addVersion` | `ApiIT.newVersionDeprecatesPreviousAndHistoryIsListed`, `newVersionOfUnknownVocabularyIs404`, `viewerCanReadAndSearchButNotWrite`; `VocabularyLifecycleTest`; smoke | Done |
| SIMPL-18174 | UI status change | `components/StatusChange.vue`, used in list and details | e2e "status changed to Deprecated" | Done |
| SIMPL-18175 | API status change | `VocabularyController.patch`, `VocabularyLifecycle.changeStatus` | `ApiIT.statusChangeRoundTrip`; `VocabularyLifecycleTest` | Done |
| SIMPL-18176 | UI deprecated versions | `DetailsView.vue` (history includes deprecated, status filter), `views/VersionView.vue` (all metadata incl. creator and change log) | e2e history check | Done |
| SIMPL-18178 | UI deprecation warning | `ListView.vue` banner and row text, `components/DeprecationWarning.vue` on details | e2e "list shows deprecation warning" | Done |
| SIMPL-18180 | UI syntax and structure validation | `turtle.ts precheck` on file selection (AC1), server validation on submit (AC2, AC3), `components/ProblemReport.vue` (AC5 to AC7), submit disabled while pre-check fails (AC8) | e2e "pre-check shows syntax error", "pre-check passes" | Partial: browser pre-check stops at the first syntax error; the server report on submit lists all |
| SIMPL-18181 | API syntax validation | `validation/TurtleSyntaxValidator`, `validation/ContentValidator` (semantic skipped when syntax fails) | `TurtleSyntaxValidatorTest`; `ApiIT.syntaxErrorsAre400WithLocations` | Partial: multi-error recovery is a heuristic (see BUILD-LOG) |
| SIMPL-18182 | UI semantic validation | Server validation on submit; report in `ProblemReport.vue` | e2e "validation report shown" | Done |
| SIMPL-18183 | API semantic validation | `validation/SemanticValidator` (all issues, warnings for unresolvable external namespaces) | `SemanticValidatorTest`; `ApiIT.deprecatedReferenceBlocksAndActiveReferencePasses`, `externalNamespaceGivesWarningOn201` | Done |
| SIMPL-18184 | Deprecated reference check (UI) | Server: `SemanticValidator` (error per deprecated version, recommends latest ACTIVE; missing version error). UI: picker warning and content-reference warnings in `NewVersionView.vue` | `SemanticValidatorTest.referenceToDeprecatedVersionIsAnErrorWithRecommendation`, `missingVocabularyAndMissingVersionAreErrors`; `ApiIT.deprecatedReferenceBlocksAndActiveReferencePasses` | Done (UI warnings untested) |
| SIMPL-18186 | UI automatic publication | No publish step: stored version is ACTIVE (`VocabularyLifecycle`), UI confirmation says "status Active" | e2e create confirmation | Done |
| SIMPL-18187 | API automatic publication | `VocabularyService.create/createVersion` persist directly; 201 with `href`, `Location` | `ApiIT.createReturns201WithVersionOneActive` | Done |
| SIMPL-18189 | UI automatic versioning | `NewVersionView.vue` (no version input; confirmation names the assigned version) | e2e new version flow | Done |
| SIMPL-18190 | API automatic versioning, reject manual version | `VocabularyLifecycle.addVersion`, `VocabularyController.rejectVersionField` | `ApiIT.versionFieldIsRejected`, `newVersionDeprecatesPreviousAndHistoryIsListed` | Done |
| SIMPL-18191 | UI automatic deprecation of previous versions | `VocabularyLifecycle.addVersion`; UI confirmation and history | `VocabularyLifecycleTest.newVersionIncrementsAndDeprecatesAllPrevious`; e2e history | Done |
| SIMPL-18193 | UI validation report | `components/ProblemReport.vue` (table with problem, location, field) | e2e "validation report shown" | Done |
| SIMPL-18194 | API validation report | 400 `VocabularyValidationProblem` with all issues and locations; 201 without error details | `ApiIT.syntaxErrorsAre400WithLocations`, `structureErrorIs400`, `createReturns201WithVersionOneActive` | Done |
| SIMPL-18195 | UI role-based access | `auth.ts` roles from token, `session.isAdmin` hides actions, router guard redirects to sign-in, no-role banner, 403 message in `ProblemReport.vue` | e2e "viewer sees no create or status actions", "user without role sees access denied" | Done |
| SIMPL-18196 | API role-based access | `config/SecurityConfig` | `ApiIT.noTokenIs401ProblemDetails`, `invalidTokenIs401`, `noRoleIs403OnReadAndWrite`, `viewerCanReadAndSearchButNotWrite`; smoke | Done |
| SIMPL-18452 | UI download selected version | `DetailsView.vue` per-version download, `VersionView.vue` | none automated (API download covered by `ApiIT.downloadReturnsUploadedTurtle`) | Done (UI untested) |
| SIMPL-18559 | UI warning on deprecated versions in history | `DetailsView.vue` row warning with link to latest ACTIVE version (AC1) or "no active version" (AC2) | e2e "history shows v1 deprecated with warning" | Done |

### OpenAPI-only operations (no story)

| Operation | Code | Own tests | Status |
|---|---|---|---|
| `GET /health` | `web/HealthController` | `ApiIT.healthIsPublic`; smoke; compose healthcheck | Done |
| `POST /vocabularies/searches` | `VocabularyController.search` | `ApiIT.searchReturnsLatestAndVersionedItems` | Done |
| `PATCH /vocabularies/{name}/versions/{version}` | `VocabularyController.patchVersion`, `VocabularyLifecycle.changeVersionStatus` | `ApiIT.versionStatusRules`; `VocabularyLifecycleTest` | Done (no UI) |
| `GET/POST /external-vocabularies`, `GET/DELETE /external-vocabularies/{name}`, `GET /external-vocabularies/{name}/content`, `POST /external-vocabularies/content`, `POST /external-vocabulary/searches` | `web/ExternalVocabularyController`, `service/ExternalVocabularyService` | `ApiIT.externalVocabularyLifecycle`; `SemanticValidatorTest.registeredExternalAndBuiltInNamespacesGiveNoWarning` | Done (no UI) |

## Decisions D1 to D30

| # | Code | Own tests | Status |
|---|---|---|---|
| D1 | all controllers under `/v1` | `ApiIT` (every operation called at least once) | Done |
| D2 | `SecurityConfig.filterChain` | `ApiIT.healthIsPublic`, `noTokenIs401ProblemDetails` | Done |
| D3 | `SecurityConfig.filterChain`, `authenticationConverter` | `ApiIT.viewerCanReadAndSearchButNotWrite`, `externalVocabularyLifecycle` (viewer DELETE 403) | Done |
| D4 | `SecurityConfig.problemEntryPoint`, `problemDeniedHandler`; `docs/openapi.yaml` | `ApiIT.noTokenIs401ProblemDetails`, `invalidTokenIs401`, `noRoleIs403OnReadAndWrite`; smoke | Done |
| D5 | `problem/*`, `web/ApiExceptionHandler` | `ApiIT` content type assertions | Done |
| D6 | `keycloak/simpl-realm.json`, `docker-compose.yml` | smoke (password grant for all three users, roles enforced) | Done |
| D7 | `VocabularyLifecycle` | `VocabularyLifecycleTest` | Done |
| D8 | `VocabularyController.rejectVersionField` | `ApiIT.versionFieldIsRejected` | Done |
| D9 | `VocabularyLifecycle.addVersion`, `JenaStore.save` | `VocabularyLifecycleTest`, `ApiIT.newVersionDeprecatesPreviousAndHistoryIsListed` | Done |
| D10 | `VocabularyLifecycle.changeStatus`, `changeVersionStatus` | `VocabularyLifecycleTest`, `ApiIT.statusChangeRoundTrip`, `versionStatusRules` | Done |
| D11 | `VocabularyService.createVersion` | `ApiIT.newVersionOfDeprecatedVocabularyIs409`, `VocabularyLifecycleTest` | Done |
| D12 | `ui/src/api.ts create` | e2e create | Done |
| D13 | `VocabularyService.create`, `CreateView.vue` | `ApiIT.duplicateNameIs409` | Done |
| D14 | lifecycle, UI labels | e2e confirmation text | Done |
| D15 | `service/Upload` | `UploadTest`, `ApiIT.nonTurtleIs415` | Done |
| D16 | `TurtleSyntaxValidator`, `StructureValidator` | `TurtleSyntaxValidatorTest`, `StructureValidatorTest`, `ApiIT.structureErrorIs400` | Done |
| D17 | `SemanticValidator` | `SemanticValidatorTest` (a, b, c, d, warnings) | Done |
| D18 | `InternalReference`, `app.reference-base` | `SemanticValidatorTest.latestReferenceResolvesToLatestVersion`, `ownNamespaceIsNotAReference` | Done |
| D19 | `VocabularyService.checkUpload` | `ApiIT.deprecatedReferenceBlocksAndActiveReferencePasses` | Done |
| D20 | `ResourceMapper.created`, `notify.ts` | `ApiIT.externalNamespaceGivesWarningOn201` | Done |
| D21 | Pico CSS, own components | n/a | Done |
| D22 | `ListView.vue` | e2e list | Done |
| D23 | `VocabularyController.list`, `ListView.vue nameParam` | `ApiIT.listFilterSortAndPaging` (lower-case input matches) | Done |
| D24 | `ContentController` | `ApiIT.downloadReturnsUploadedTurtle`; smoke | Done |
| D25 | `DeprecationWarning.vue`, `ListView.vue`, `NewVersionView.vue` | e2e list and history warnings | Done (picker warning untested) |
| D26 | `session.isAdmin` in views | e2e viewer check | Done |
| D27 | compose `fuseki`, `JenaStore` | smoke (runs against Fuseki); `ApiIT` runs the same SPARQL on an in-memory dataset | Done |
| D28 | 400 body is the report | `ApiIT.syntaxErrorsAre400WithLocations` | Done |
| D29 | `RequestChecks.paging` | `ApiIT.invalidListParametersAre400` (pageSize 101) | Done |
| D30 | `app.max-upload-bytes`, `VocabularyService.checkUpload` | `ApiIT.tooLargeIs413` (with a lowered limit) | Done |

## Not implemented

| Item | Reason |
|---|---|
| UI for external vocabularies and for version-level status change | No UI story; API only |
| `DEGRADED` health status | No rule in the specs for when the API is "partially available" |
| Separate validation notification (BP02B.06) | D28: the synchronous response is the report |
| Manual semantic review, sunset policy, offering vocabularies to other participants | Out of scope per the digest |
