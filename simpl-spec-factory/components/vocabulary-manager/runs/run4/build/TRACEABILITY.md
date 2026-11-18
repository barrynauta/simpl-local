# Traceability

Acceptance criteria are numbered per story in the order they appear in `input/specs/stories/` (AC1 is the first
scenario). Paths are relative to `work/`. Short names:

- Backend code: `B/` = `backend/src/main/java/eu/simpl/vocabularymanager/`; tests: `T/` = `backend/src/test/java/eu/simpl/vocabularymanager/`.
- UI code: `U/` = `ui/src/`. UI checks: `ui/src/turtleCheck.test.ts` (Vitest), `ui/e2e/ui_check.py` (own Playwright check, run by hand against the stack), `smoke.sh`.
- Test classes: `ApiIT` = `T/api/ApiIntegrationTest`, `SvcT` = `T/service/VocabularyServiceTest`, `ValT` = `T/validation/VocabularyValidatorTest`, `QryT` = `T/service/QueryHelpersTest`, `TokT` = `T/security/TokenValidatorTest`, `HttpT` = `T/store/HttpSparqlClientTest`, `UiCheck` = `ui/e2e/ui_check.py`.

## Stories

| Story / AC | What | Code | Own tests |
|---|---|---|---|
| SIMPL-18161 AC1 | List with actions menu, name, description, latest version, status, dates, creator | `U/views/VocabularyList.vue`, `U/components/ActionsMenu.vue` | UiCheck |
| SIMPL-18161 AC2 | Load error message and detail | `VocabularyList.vue` (`loadError`) | UiCheck (stubbed 500) |
| SIMPL-18162 AC1 | Paged list with metadata | `B/api/VocabularyController.list`, `B/api/ApiMapper.page` | ApiIT `listFiltersSortsPagesAndRejectsUnknownParameters`, smoke |
| SIMPL-18162 AC2 | Wrong paging gives 400 | `B/api/QueryParameters.pageRequest` | ApiIT (page=0, pageSize=101, pageSize=x) |
| SIMPL-18162 AC3 | No role gives 403 | `B/security/SecurityConfig` | ApiIT `rolesDecideAccess` |
| SIMPL-18162 AC4 | Internal error gives 500 with detail | `B/api/ApiExceptionHandler.unexpected`, `B/store/HttpSparqlClient` | HttpT `storeErrorThatIsNotATimeoutGives500` |
| SIMPL-18163 AC1 | UI filters (name contains, status, dates) | `VocabularyList.vue` (filters) | UiCheck (name filter) |
| SIMPL-18163 AC2 | Sortable columns asc then desc | `VocabularyList.vue` (`sortBy`) | none automated for the UI; API sort in ApiIT and QryT |
| SIMPL-18164 AC1 | Filter and sort on name, status, creationDate, lastUpdated | `B/service/VocabularyFilter`, `DateFilter`, `SortOrder` | QryT, ApiIT, SvcT `listFiltersSortsAndPages` |
| SIMPL-18164 AC2 | No match gives empty 200 | `VocabularyController.list` | ApiIT (status=DEPRECATED, empty) |
| SIMPL-18164 AC3 | Paging keeps sort order | `B/service/Page`, `SortOrder` | ApiIT (`sort=-name&pageSize=2`) |
| SIMPL-18164 AC4 | Invalid filter or sort gives 400 | `VocabularyController.list` | ApiIT (sort=version, status=active, name=a-b, dates) |
| SIMPL-18165 AC1 | Download latest from the list, view details | `VocabularyList.vue` (Download vocabulary, View vocabulary details), `B/api/ContentController.latest` | ApiIT `newVersionFlowDownloadAndHistory`, smoke |
| SIMPL-18166 AC1 | Version metadata, machine-readable | `VocabularyController.version` | ApiIT |
| SIMPL-18166 AC2 | Unknown version gives 404 | `B/error/Problems.versionNotFound` | ApiIT, SvcT `unknownVocabularyOrVersionIsNotFound` |
| SIMPL-18167 AC1 | Details page with version history columns | `U/views/VocabularyDetails.vue` | UiCheck |
| SIMPL-18168 AC1 | Version history list | `VocabularyController.versions` | ApiIT, smoke |
| SIMPL-18168 AC2 | Unknown vocabulary gives 404 | `VocabularyService.listVersions` | SvcT |
| SIMPL-18169 AC1 | "New Vocabulary +" opens form: Name, Description, File upload; no version or change log | `U/views/VocabularyCreate.vue`, `U/components/FileUpload.vue` | UiCheck |
| SIMPL-18169 AC2 | Required fields, formats, unique name, supported format; messages | `VocabularyCreate.vue`, `U/labels.ts` (`nameError`, `textError`), `B/api/FormFields` | ApiIT `createRejectsBadFieldsDuplicatesTooLargeAndWrongMediaType`, Vitest |
| SIMPL-18170 AC1 | Create: 201, version 1 ACTIVE, vocabulary ACTIVE, identifier | `VocabularyController.create`, `VocabularyService.create` | ApiIT `createReturns201WithLocationWarningsAndCreator`, SvcT, smoke |
| SIMPL-18170 AC2 | Read back metadata, status can change later | `VocabularyController.get`, `updateStatus` | ApiIT, SvcT `statusChangesAndSameStatusConflicts` |
| SIMPL-18170 AC3 | Invalid data rejected, nothing created | `FormFields`, `VocabularyService.validate` | ApiIT `validationErrorsAreReportedAndNothingIsStored`, SvcT `invalidContentStoresNothing` |
| SIMPL-18172 AC1 | Update form pre-filled (description, current content) | `U/views/VocabularyUpdate.vue` | UiCheck |
| SIMPL-18172 AC2 | New version ACTIVE, older DEPRECATED, notification | `VocabularyUpdate.vue`, `VocabularyService.createVersion`, `FusekiVocabularyRepository.addVersion` | SvcT `newVersionDeprecatesEveryOlderActiveVersion`, UiCheck |
| SIMPL-18172 AC3 | Unexpected error: nothing created, retry or contact support | `U/errors.ts` (`RETRY_ADVICE`), D8 single transaction | ApiIT `storeDownGives503WithRetryAfter` |
| SIMPL-18173 AC1 | New version via API: next number, ACTIVE, older DEPRECATED | `VocabularyController.createVersion` | ApiIT, SvcT, smoke |
| SIMPL-18173 AC2 | Invalid data rejected with feedback | `FormFields`, validator | ApiIT |
| SIMPL-18173 AC3 | Unknown vocabulary rejected (404) | `VocabularyService.createVersion` | ApiIT (`createVersion("Unknown")`) |
| SIMPL-18173 AC4 | Not authorised gives 403 | `SecurityConfig` | ApiIT, smoke |
| SIMPL-18173 AC5 | Not authenticated gives 401 | `BearerTokenFilter`, entry point | ApiIT, smoke |
| SIMPL-18173 AC6 | Unsupported format gives 415 | `consumes` multipart, `ApiExceptionHandler.unsupported` | ApiIT |
| SIMPL-18174 AC1 | Status change action, shown and confirmed | `U/statusChange.ts`, list and details views | UiCheck |
| SIMPL-18174 AC2 | Vocabulary no longer available: not found message, no change | `statusChange.ts` (404 branch) | UiCheck (stubbed 404 on PATCH) |
| SIMPL-18174 AC3 | Update fails: message, status unchanged | `statusChange.ts` | none automated |
| SIMPL-18175 AC1 | PATCH sets status, returns vocabulary | `VocabularyController.updateStatus` | ApiIT `statusPatchesValidateAndConflict` |
| SIMPL-18175 AC2 | Unknown vocabulary gives 404 | `VocabularyService.updateStatus` | ApiIT |
| SIMPL-18175 AC3 | Other status value gives 400, no change | `VocabularyController.statusOf` | ApiIT |
| SIMPL-18176 AC1 | Viewer can see versions including deprecated | `SecurityConfig` (GET for both roles), details page | ApiIT (viewer GETs) |
| SIMPL-18176 AC2 | Deprecated versions shown and labelled | `VocabularyDetails.vue` | UiCheck |
| SIMPL-18176 AC3 | Version panel with version, date, creator, change log | `VocabularyDetails.vue` (version panel) | UiCheck |
| SIMPL-18178 AC1 | Warning on deprecated vocabularies in the list | `VocabularyList.vue`, `U/labels.ts` (`VOCABULARY_DEPRECATED`) | UiCheck |
| SIMPL-18180 AC1 | Syntax check when a file is chosen | `FileUpload.vue`, `U/turtleCheck.ts` | Vitest, UiCheck |
| SIMPL-18180 AC2, AC3 | Validation on submit (create, new version) | `VocabularyService.validate` | ApiIT, SvcT |
| SIMPL-18180 AC4 | Success message, flow continues | `FileUpload.vue` ('Syntax check passed.') | UiCheck |
| SIMPL-18180 AC5 | Syntax errors with location, flow blocked | `FileUpload.vue`, `B/validation/TurtleSyntaxValidator` | UiCheck, ValT |
| SIMPL-18180 AC6, AC7 | Structure (semantic) errors, all in one response | `B/validation/SemanticRules`, `ValidationReport.vue` | ValT `allSemanticRulesRunAndReturnEveryError` |
| SIMPL-18180 AC8 | Cannot continue until errors are fixed | submit disabled while upload check fails; server 400 | UiCheck |
| SIMPL-18181 AC1 | Valid syntax passes to semantic validation | `VocabularyValidator.validate` | ValT |
| SIMPL-18181 AC2 | Syntax error: rejected, semantic not run, nothing stored, location | `TurtleSyntaxValidator`, `VocabularyValidator` | ValT, ApiIT, SvcT |
| SIMPL-18182 AC1 to AC3 | Semantic validation on UI submit; report on failure | `VocabularyCreate.vue`, `VocabularyUpdate.vue`, `ValidationReport.vue` | ApiIT (report body), UiCheck (report panel) |
| SIMPL-18183 AC1 | Semantics pass: created | `VocabularyService` | SvcT |
| SIMPL-18183 AC2 | Semantic issues: list of errors, not created | `SemanticRules`, `ReferenceRules` | ValT |
| SIMPL-18184 AC1 | References to ACTIVE versions: no warning | `ReferenceRules` | ValT `referenceToActiveVersionGivesNoWarning` |
| SIMPL-18184 AC2 | Deprecated reference: warning with latest ACTIVE advice (D11 overrides 'error') | `ReferenceRules.deprecatedDescription` | ValT, SvcT `deprecatedReferenceWarnsAndRegisteredExternalDoesNot` |
| SIMPL-18184 AC3 | Missing referenced version: error S7 | `ReferenceRules.checkInternal` | ValT `missingInternalVocabularyIsS7`, `missingInternalVersionIs...` |
| SIMPL-18184 AC4 | UI shows the deprecated-reference warning | `NotificationArea.vue` (warnings panel) | UiCheck |
| SIMPL-18186 AC1 | Stored and ACTIVE automatically | `VocabularyService.create/createVersion` | SvcT |
| SIMPL-18187 AC1, AC2 | Persisted, identifier (Location, href) | `VocabularyController.createdResponse` | ApiIT, smoke |
| SIMPL-18189 AC1 to AC3 | UI: number assigned, no manual input, confirmation with number | `VocabularyUpdate.vue` (no version field, 'Version <n> of <Name> was created.') | UiCheck |
| SIMPL-18190 AC1 | Next number assigned, response shows it | `VocabularyService.createVersion` | ApiIT, smoke |
| SIMPL-18190 AC2 | Manual version field gives 400, nothing created | `MultipartForm.read` (forbidden `version`) | ApiIT |
| SIMPL-18191 AC1 | New version ACTIVE, older DEPRECATED | `FusekiVocabularyRepository.addVersion` | SvcT, smoke |
| SIMPL-18193 AC1 | UI confirms success | `NotificationArea.vue` | UiCheck |
| SIMPL-18193 AC2 | UI report of errors with nature and location | `ValidationReport.vue` | UiCheck, ApiIT |
| SIMPL-18194 AC1 | API report lists all errors with detail and location | `ApiExceptionHandler`, `Issue.invalidInput` | ValT, ApiIT |
| SIMPL-18194 AC2 | Success confirmed in the 201 response | `ApiMapper.created` | ApiIT |
| SIMPL-18195 AC1 | Admin sees create, update, status change | `U/auth.ts` (`isAdmin`), views | UiCheck |
| SIMPL-18195 AC2 | Others: actions hidden, direct URL shows authorisation message | views, `U/labels.ts` (`NOT_AUTHORIZED`) | UiCheck (ga-viewer) |
| SIMPL-18195 AC3 | Not signed in: sent to Keycloak login | `U/auth.ts` (`login-required`) | UiCheck (login) |
| SIMPL-18196 AC1 to AC4 | Token and role on every endpoint; 403; 401 | `B/security/*` | ApiIT, TokT, smoke |
| SIMPL-18452 AC1, AC2 | Select a version, download its file | `VocabularyDetails.vue`, `ContentController.version` | UiCheck (download filename), ApiIT |
| SIMPL-18559 AC1 | Deprecated version warning with link to latest ACTIVE version | `VocabularyDetails.vue` | UiCheck |
| SIMPL-18559 AC2 | Warning without link when no ACTIVE version | `VocabularyDetails.vue` (`latestActive` undefined) | Vitest (`latestActiveVersion`) |
| SIMPL-393 | Met by the generic upload; no seed data (D1) | none | none |
| SIMPL-395, SIMPL-28680 | Out of scope (D1) | not implemented | none |

## Decisions

| Decision | Code | Own tests |
|---|---|---|
| D1 Scope | stories above; no seed data | none |
| D2 CDM snapshot | glossary names in code (`Vocabulary`, `VocabularyVersion`, `ExternalVocabulary`) | none |
| D3 Two statuses, effective deprecation | `B/domain/VocabularyStatus`, `InternalVocabularyState.isEffectivelyDeprecated`, `U/labels.ts` | SvcT `versionOfDeprecatedVocabularyKeepsVocabularyDeprecated`, ValT |
| D4 Transitions, 409 on same status, 400 on other value | `VocabularyService.updateStatus/updateVersionStatus`, `addVersion` | SvcT, ApiIT |
| D5 Latest version, latestVersionHref | `ApiMapper.version/created`, `InternalVocabularyState` | ApiIT, SvcT |
| D6 Publish is automatic | `VocabularyService.create/createVersion` | SvcT |
| D7 Integer numbers, `version` field refused | `VocabularyService.createVersion`, `MultipartForm` | ApiIT, SvcT |
| D8 Nothing stored on failure | validation before the single store update | SvcT `invalidContentStoresNothing`, `duplicateName...` |
| D9 Syntax then semantic, S1 to S7 | `B/validation/*` | ValT |
| D10 Errors versus warnings, closed codes | `B/domain/WarningCode`, `ValidationOutcome` | ValT, ApiIT |
| D11 Deprecated reference warning text | `ReferenceRules.deprecatedDescription` | ValT |
| D12 What a reference is | `ReferenceRules` | ValT `externalNamespacesWarnUnlessRegisteredW3cOrOwn`, `referenceToOwnVocabularyIsIgnored` |
| D13 UI syntax check, 5 MB, submit disabled | `FileUpload.vue`, `turtleCheck.ts` | Vitest, UiCheck |
| D14 Report panel, success notification, warnings panel | `ValidationReport.vue`, `NotificationArea.vue` | UiCheck |
| D15 Turtle only, 5 MB, 413, 415, 409 | `VocabularyController.checkSize`, `ApiExceptionHandler` | ApiIT, smoke-level manual check (6 MB and 30 MB uploads gave 413) |
| D16 Roles | `SecurityConfig` | ApiIT `rolesDecideAccess` |
| D17 401 and 403 in the service, before body checks | `BearerTokenFilter`, `SecurityConfig` | ApiIT, TokT |
| D18 UI restricted actions | views, `NOT_AUTHORIZED` | UiCheck |
| D19 Name filter | `VocabularyController.list` (`NAME_FILTER`), `VocabularyFilter` | ApiIT, QryT |
| D20 Date filters | `DateFilter` | QryT |
| D21 Unknown query parameters | `QueryParameters.read` | ApiIT |
| D22 Paging limits and links | `QueryParameters.pageRequest`, `ApiMapper.page` | ApiIT, QryT |
| D23 Sorting | `SortOrder`, `VocabularyList.vue` | QryT, ApiIT |
| D24 Status codes for generic criteria | `Problems`, `ApiExceptionHandler` | ApiIT |
| D25 Vocabulary object | `ApiMapper.vocabulary`, `FusekiVocabularyRepository` | ApiIT, SvcT |
| D26 Version object, Location | `ApiMapper.version/created` | ApiIT |
| D27 Creator | `TokenValidator.userId`, `Caller` | TokT `creatorFallsBackToPreferredUsername`, ApiIT |
| D28 Description on the version | `VocabularyVersion`, repository query of the latest description | SvcT |
| D29 Content and download | `ContentController`, Base64 content in the store | ApiIT, smoke (byte compare) |
| D30 External identity and registration | `ExternalVocabularyService.register`, `ExternalVocabulary.matches` | SvcT, ApiIT |
| D31 Terms | glossary terms in code and UI labels | none |
| D32 Changelog 'Initial version.' from the UI | `U/api.ts` (`createVocabulary`) | UiCheck |
| D33 New version form | `VocabularyUpdate.vue` | UiCheck |
| D34 List paging, empty and error states | `VocabularyList.vue` | UiCheck (error state) |
| D35 Details and history page | `VocabularyDetails.vue`, `U/api.ts` (`listAllVersions`) | UiCheck |
| D36 Deprecation warnings | `VocabularyList.vue`, `VocabularyDetails.vue` | UiCheck |
| D37 Field rules | `FormFields`, `PathParameters`, `U/labels.ts` | ApiIT, Vitest |
| D38 Searches | `VocabularyController.search`, `ExternalVocabularyController.search`, `VocabularyService.search` | SvcT `searchKeepsRequestOrderDropsMissingAndRepeats`, ApiIT |
| D39 Decided behaviour over the OpenAPI file | see `docs/api-notes.md` | ApiIT |
| D40 OpenID Connect via Keycloak | `U/auth.ts`, `TokenValidator` | TokT, UiCheck |
| D41 External operations | `ExternalVocabularyController` | ApiIT `externalVocabularyLifecycle`, SvcT |
| D42 External removal without reference check | `ExternalVocabularyService.delete` | SvcT |
| D43 Serialised changes | `VocabularyLocks` | SvcT `parallelVersionCreationsAreSerialised`, `lockTimeoutGivesServiceUnavailable`; manual run on the stack (5 parallel requests gave versions 2 to 6) |
| D44 Timeouts, retries, 503, health | `HttpSparqlClient`, `RealmKeys`, `HealthController` | HttpT, TokT, ApiIT `healthNeedsNoTokenAndReportsStoreDown` |
| D45 Pull only, no events | no event publishing | none |
| D46 Identical version 409, UI line | `VocabularyService.createVersion`, `VocabularyUpdate.vue` | SvcT, ApiIT, UiCheck |
| E1 Keycloak realm | `keycloak/realm-simpl.json` | smoke |
| E2 Ports and base URLs | `docker-compose.yml` (host ports from build.yaml), `application.yml` | smoke |
| E3 Fuseki dataset 'vocabulary' | `fuseki/Dockerfile`, `HttpSparqlClient` | smoke |
