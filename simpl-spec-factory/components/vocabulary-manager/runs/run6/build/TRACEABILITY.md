# Traceability

Criteria ids: where a story numbers its criteria (AC1, AC2, ...) that number is used. Where it does not, the scenarios are numbered AC1, AC2, ... in the order the story lists them (the convention the decisions use, for example SIMPL-18173/AC6, SIMPL-18190/AC2).

Code paths are relative to `backend/src/main/java/eu/simpl/vocabularymanager/` (BE) and `ui/src/` (UI). Test names: `BE-T` = `backend/src/test/java/eu/simpl/vocabularymanager/`, `UI-T` = `ui/tests/`, `SMOKE` = `smoke.sh`. The UI views have no component tests of their own (only the helpers they use are unit-tested); they were checked by hand where noted, and the step 60 suite covers them.

## Stories

| Story / AC | What | Code | Own tests |
|---|---|---|---|
| SIMPL-18161 AC1 | List with name, description, latest version, status, dates, actions menu | UI `views/VocabularyList.vue`; BE `api/VocabularyController.list` | BE-T `api/ApiIntegrationTest.mainFlow`; UI-T `format.test.ts` |
| SIMPL-18161 AC2 | List load error with detail (D34) | UI `views/VocabularyList.vue`, `errors.ts` | UI-T `errors.test.ts` |
| SIMPL-18162 AC1 | Paged list with metadata | BE `api/VocabularyController`, `api/Paging`, `api/Representations` | `ApiIntegrationTest.mainFlow`, `ParametersTest.pagingDefaultsAndLimits`, SMOKE |
| SIMPL-18162 AC2 | Wrong paging gives 400 | BE `api/Paging` | `ParametersTest.pagingDefaultsAndLimits`, `ApiIntegrationTest.readValidation` |
| SIMPL-18162 AC3 | No role gives 403 | BE `security/SecurityConfig` | `ApiIntegrationTest.accessRules` |
| SIMPL-18162 AC4 | Internal error gives 500 with detail | BE `web/GlobalExceptionHandler`, `store/StoreFailureException` | `SparqlClientTest.storeErrorIsAFailureNotUnavailable` |
| SIMPL-18163 AC1 | Filter in the UI (status, name contains) | UI `views/VocabularyList.vue` | by hand |
| SIMPL-18163 AC2 | Sortable columns, D50 click rule | UI `views/VocabularyList.vue` `sortBy` | by hand |
| SIMPL-18164 AC1 | Filter and sort on name, status, creationDate, lastUpdated | BE `api/ListParameters`, `store/SparqlVocabularyStore`, `store/SortOrder` | `ParametersTest.dateMatchesTheWholeUtcDayAndDateTimeTheSecond`, `ApiIntegrationTest.mainFlow` |
| SIMPL-18164 AC2 | No match gives empty 200 | BE `store/SparqlVocabularyStore.listVocabularies` | `ApiIntegrationTest.readValidation` |
| SIMPL-18164 AC3 | Paging with sort, stable ties (D23, D58) | BE `store/SortOrder` | `ParametersTest.linksIncludePrevNextAndLast`, `VocabularyServiceTest.readsAndSearch` |
| SIMPL-18164 AC4 | Unsupported or invalid filter or sort gives 400 | BE `api/ListParameters`, `api/RequestChecks` | `ParametersTest.filtersReportInvalidValues`, `ApiIntegrationTest.readValidation` |
| SIMPL-18165 AC1 | Download latest and see full details | UI `views/VocabularyList.vue`, `views/VocabularyDetails.vue`; BE `api/ContentController` | `ApiIntegrationTest.mainFlow`, SMOKE |
| SIMPL-18166 AC1 | Version metadata and content | BE `api/VocabularyController.getVersion`, `api/ContentController` | `ApiIntegrationTest.mainFlow` |
| SIMPL-18166 AC2 | Unknown version gives 404 | BE `problem/Problems` | `VocabularyServiceTest.versionOfUnknownVocabularyIsNotFound`, `ApiIntegrationTest.readValidation` |
| SIMPL-18167 AC1 | Details page with version history columns (D35) | UI `views/VocabularyDetails.vue` | by hand |
| SIMPL-18168 AC1 | Version list newest first | BE `api/VocabularyController.versions` | `ApiIntegrationTest.mainFlow` |
| SIMPL-18168 AC2 | Unknown vocabulary gives 404 | BE `service/VocabularyService` | `ApiIntegrationTest.readValidation` |
| SIMPL-18169 AC1 | Create form: Name, Description, File; no version or change log (D32) | UI `views/VocabularyCreate.vue` | by hand |
| SIMPL-18169 AC2 | Field, uniqueness and format checks with messages (D37) | UI `format.ts`, `views/VocabularyCreate.vue`; BE `api/RequestChecks` | UI-T `format.test.ts`; `ApiIntegrationTest.orderOfChecksOnWrites` |
| SIMPL-18170 AC1 | Create gives 201, version 1 ACTIVE, vocabulary ACTIVE | BE `service/VocabularyService.createVocabulary` | `VocabularyServiceTest.createGivesVersionOneActiveWithTimesToTheSecond`, SMOKE |
| SIMPL-18170 AC2 | GET returns the metadata, status changeable later | BE `api/VocabularyController` | `ApiIntegrationTest.mainFlow` |
| SIMPL-18170 AC3 | Invalid data gives 400, nothing stored (D8) | BE `service/VocabularyService` | `VocabularyServiceTest.invalidContentStoresNothing` |
| SIMPL-18172 AC1 | Update form pre-filled with description and current content (D33) | UI `views/VocabularyUpdate.vue` | by hand |
| SIMPL-18172 AC2 | New version created, success notification | UI `views/VocabularyUpdate.vue`, `notifications.ts` | UI-T `notifications.test.ts`, `texts.test.ts` |
| SIMPL-18172 AC3 | Unexpected error: generic message, retry or support (D60) | UI `errors.ts` | UI-T `errors.test.ts` |
| SIMPL-18173 AC1 | New version: next number, ACTIVE, older DEPRECATED | BE `service/VocabularyService.createVersion` | `VocabularyServiceTest.newVersionDeprecatesEveryOlderActiveVersionAndKeepsVocabularyStatus`, SMOKE |
| SIMPL-18173 AC2 | Invalid data gives 400 | BE `api/RequestChecks`, `validation/*` | `ApiIntegrationTest.orderOfChecksOnWrites` |
| SIMPL-18173 AC3 | Unknown vocabulary gives 404 | BE `service/VocabularyService` | `VocabularyServiceTest.versionOfUnknownVocabularyIsNotFound` |
| SIMPL-18173 AC4 | No admin role gives 403 | BE `security/SecurityConfig` | `ApiIntegrationTest.accessRules`, SMOKE |
| SIMPL-18173 AC5 | No token gives 401 | BE `security/SecurityConfig` | `ApiIntegrationTest.accessRules`, SMOKE |
| SIMPL-18173 AC6 | Unsupported format gives 415 (D15) | BE `api/RequestChecks` | `ApiIntegrationTest.orderOfChecksOnWrites` |
| SIMPL-18174 AC1 | Deprecate / Activate action, message (D60) | UI `views/VocabularyList.vue`, `views/VocabularyDetails.vue` | UI-T `texts.test.ts` |
| SIMPL-18174 AC2 | Vocabulary gone: not found message | UI `errors.ts`, views | UI-T `errors.test.ts` |
| SIMPL-18174 AC3 | Update fails: status unchanged, message | UI views (`statusUpdateError`) | by hand |
| SIMPL-18175 AC1 | PATCH status, returns vocabulary | BE `api/VocabularyController.changeStatus`, `service/VocabularyService.changeVocabularyStatus` | `VocabularyServiceTest.statusChangesFollowTheStateTable`, `ApiIntegrationTest.jsonBodies` |
| SIMPL-18175 AC2 | Unknown vocabulary gives 404 | BE `service/VocabularyService` | `ApiIntegrationTest.jsonBodies` |
| SIMPL-18175 AC3 | Unsupported status gives 400 | BE `api/StatusBody` | `ApiIntegrationTest.jsonBodies` |
| SIMPL-18176 AC1 | Viewer may read history | BE `security/SecurityConfig` | `ApiIntegrationTest.accessRules` |
| SIMPL-18176 AC2 | Deprecated versions shown and labelled | UI `views/VocabularyDetails.vue` | by hand |
| SIMPL-18176 AC3 | Version panel with metadata incl. creator | UI `views/VocabularyDetails.vue` | by hand |
| SIMPL-18178 AC1 | Warning on deprecated vocabulary (D36) | UI `views/VocabularyList.vue`, `views/VocabularyDetails.vue` | by hand |
| SIMPL-18180 AC1 | Syntax check on file choice in the browser (D13) | UI `turtleCheck.ts` | UI-T `turtleCheck.test.ts` |
| SIMPL-18180 AC2, AC3 | Server validation on create and new version | BE `validation/ContentValidator` | `ContentValidatorTest.*`, `ApiIntegrationTest.validationReport` |
| SIMPL-18180 AC4 | 'Syntax check passed.' and proceed | UI `turtleCheck.ts` | UI-T `turtleCheck.test.ts` |
| SIMPL-18180 AC5 | Syntax error with line (and column) | UI `turtleCheck.ts`; BE `validation/TurtleParser` | UI-T `turtleCheck.test.ts`; `ContentValidatorTest.syntaxErrorGivesOneIssueWithLineAndColumnAndStopsSemanticValidation` |
| SIMPL-18180 AC6, AC7 | All structure errors in one report | BE `validation/SemanticRules`; UI `components/ProblemReport.vue` | `ContentValidatorTest.allSemanticRulesRunAndAllErrorsAreReturned` |
| SIMPL-18180 AC8 | Submit disabled while errors show | UI `views/VocabularyCreate.vue`, `views/VocabularyUpdate.vue` | by hand |
| SIMPL-18181 AC1 | Valid syntax proceeds to semantic validation | BE `validation/ContentValidator` | `ContentValidatorTest.validContentPassesWithoutWarnings` |
| SIMPL-18181 AC2 | Invalid syntax: 400, first error with location, no semantic run, nothing stored | BE `validation/TurtleParser` | `ContentValidatorTest.syntaxErrorGivesOneIssueWithLineAndColumnAndStopsSemanticValidation`, `ContentValidatorTest.undefinedPrefixIsASyntaxError` |
| SIMPL-18182 AC1 to AC3 | Semantic step after syntax, report on failure | BE `validation/ContentValidator`; UI `components/ProblemReport.vue` | `ContentValidatorTest.allSemanticRulesRunAndAllErrorsAreReturned` |
| SIMPL-18183 AC1 | Semantics pass: created | BE `service/VocabularyService` | `ApiIntegrationTest.mainFlow` |
| SIMPL-18183 AC2 | Semantic errors: 400 with all errors, warnings for external namespaces | BE `validation/SemanticRules`, `validation/ReferenceChecker` | `ContentValidatorTest.allSemanticRulesRunAndAllErrorsAreReturned`, `ContentValidatorTest.unregisteredExternalNamespaceGivesWarning`, `ContentValidatorTest.untaggedLabelsCountAsOneLanguage` |
| SIMPL-18184 AC1 | Active references: no warning | BE `validation/ReferenceChecker` | `ContentValidatorTest.activeReferenceGivesNoWarningAndOwnReferenceIsIgnored` |
| SIMPL-18184 AC2 | Deprecated reference: warning, not error (D11 overrides the AC), recommends latest ACTIVE (D47) | BE `validation/ReferenceChecker` | `ContentValidatorTest.deprecatedVersionReferenceRecommendsLatestActive`, `...WithoutActiveVersionHasNoRecommendation`, `...deprecatedVocabularyMakesActiveVersionEffectivelyDeprecated` |
| SIMPL-18184 AC3 | Missing reference: S7 error | BE `validation/ReferenceChecker` | `ContentValidatorTest.allSemanticRulesRunAndAllErrorsAreReturned` |
| SIMPL-18184 AC4 | UI warnings panel after submit (D14) | UI `components/NoticePanel.vue`, `notifications.ts` | UI-T `notifications.test.ts` |
| SIMPL-18186 AC1 | Stored and ACTIVE in the same request (D6) | BE `service/VocabularyService` | `VocabularyServiceTest.createGivesVersionOneActiveWithTimesToTheSecond` |
| SIMPL-18187 AC1, AC2 | 201 with Location and href | BE `api/VocabularyController` | `ApiIntegrationTest.mainFlow` |
| SIMPL-18189 AC1 to AC3 | Version assigned by the system, shown in the notification | BE `service/VocabularyService`; UI `views/VocabularyUpdate.vue` | `VocabularyServiceTest.newVersionDeprecatesEveryOlderActiveVersionAndKeepsVocabularyStatus`, UI-T `texts.test.ts` |
| SIMPL-18190 AC1 | Next number in the 201 | BE `service/VocabularyService` | SMOKE, `ApiIntegrationTest.mainFlow` |
| SIMPL-18190 AC2 | `version` field gives 400 (D7) | BE `api/RequestChecks` | `ApiIntegrationTest.orderOfChecksOnWrites` |
| SIMPL-18191 AC1 | New ACTIVE, all older DEPRECATED | BE `service/VocabularyService` | `VocabularyServiceTest.newVersionDeprecatesEveryOlderActiveVersionAndKeepsVocabularyStatus`, `VocabularyServiceTest.parallelVersionCreationsAreSerialised` |
| SIMPL-18193 AC1, AC2 | UI success and report panel (D14) | UI `components/ProblemReport.vue`, `components/NoticePanel.vue` | UI-T `notifications.test.ts` |
| SIMPL-18194 AC1, AC2 | 400 report with all errors; 201 with warnings | BE `problem/Problems`, `api/Representations` | `ApiIntegrationTest.validationReport` |
| SIMPL-18195 AC1 to AC3 | Actions hidden without admin, 'not authorized' on direct URL or 403, login redirect | UI `auth.ts`, `errors.ts`, views | UI-T `errors.test.ts` |
| SIMPL-18196 AC1 to AC4 | 401 and 403 on every secured operation | BE `security/SecurityConfig` | `ApiIntegrationTest.accessRules`, `RealmKeySourceTest.*`, SMOKE |
| SIMPL-18452 AC1, AC2 | Select a version, download it (`<Name>-v<n>.ttl`) | UI `views/VocabularyDetails.vue`; BE `api/ContentController` | `ApiIntegrationTest.mainFlow`, SMOKE |
| SIMPL-18559 AC1, AC2 | Deprecated version warning and link to latest ACTIVE | UI `views/VocabularyDetails.vue` | UI-T `texts.test.ts` |

## Out of scope

| Story | Reason |
|---|---|
| SIMPL-28680 | Schema Manager story (D1, D61) |
| SIMPL-393 | Met by the generic upload; no seed data (D1) |
| SIMPL-395 | Replaced by the SIMPL-181xx stories (D1) |

## Decisions

| Decision | Code | Own tests |
|---|---|---|
| D1, D2, D61 | Scope only | n/a |
| D3 vocabulary and version status | `service/VocabularyService`, `validation/ReferenceChecker` | `VocabularyServiceTest.newVersionDeprecatesEveryOlderActiveVersionAndKeepsVocabularyStatus`, `ContentValidatorTest.deprecatedVocabularyMakesActiveVersionEffectivelyDeprecated` |
| D4 transitions, 409 same status | `service/VocabularyService.changeVocabularyStatus`, `changeVersionStatus` | `VocabularyServiceTest.statusChangesFollowTheStateTable`, `versionStatusChangeAllowsTwoActiveVersions` |
| D5 latest version, latestVersionHref | `api/Representations` | `ApiIntegrationTest.mainFlow` |
| D6 automatic publication | `service/VocabularyService` | `VocabularyServiceTest.createGivesVersionOneActiveWithTimesToTheSecond` |
| D7 integer versions, version field 400 | `service/VocabularyService`, `api/RequestChecks` | `ApiIntegrationTest.orderOfChecksOnWrites` |
| D8 nothing stored on failure | one SPARQL update per write, `store/SparqlVocabularyStore` | `VocabularyServiceTest.invalidContentStoresNothing` |
| D9, D53, D57 validation rules | `validation/*` | `ContentValidatorTest.*` |
| D10, D14 errors versus warnings | `validation/ContentValidator`, `problem/Problems` | `ContentValidatorTest.*`, `ApiIntegrationTest.validationReport` |
| D11, D12, D47 references | `validation/ReferenceChecker` | `ContentValidatorTest.deprecated*`, `registeredExternalNamespaceOrAliasGivesNoWarning`, `unregisteredExternalNamespaceGivesWarning` |
| D13, D60 UI syntax check | `ui/src/turtleCheck.ts` | UI-T `turtleCheck.test.ts` |
| D15 format, size, media type | `api/RequestChecks`, `api/MultipartForm`, Tomcat limits in `application.yaml` | `ApiIntegrationTest.orderOfChecksOnWrites`, `TomcatLimitsIntegrationTest` |
| D16, D17, D40 access | `security/SecurityConfig` | `ApiIntegrationTest.accessRules`, `RealmKeySourceTest.realmRolesAreReadFromRealmAccess` |
| D18, D60 UI access and texts | `ui/src/auth.ts`, `errors.ts`, `texts.ts` | UI-T `errors.test.ts`, `texts.test.ts` |
| D19, D20, D21, D22, D23, D58, D59 list rules | `api/ListParameters`, `api/Paging`, `store/SortOrder` | `ParametersTest.*` |
| D24, D49 status codes and path values | `api/UriValues`, `problem/Problems` | `ParametersTest.pathAndTextRules`, `ApiIntegrationTest.readValidation` |
| D25, D26, D27, D28 objects and creator | `api/Representations`, `api/Callers` | `ApiIntegrationTest.mainFlow` |
| D29 download | `api/ContentController` | `ApiIntegrationTest.mainFlow`, SMOKE (byte comparison) |
| D30, D41, D42, D56 external vocabularies | `service/ExternalVocabularyService`, `api/ExternalVocabularyController` | `ExternalVocabularyServiceTest.*`, `ApiIntegrationTest.externalVocabularies` |
| D31 terms | glossary terms in code and UI | n/a |
| D32, D33, D34, D35, D36, D50 UI | `ui/src/views/*` | by hand |
| D37 field rules | `api/RequestChecks`; `ui/src/format.ts` | `ParametersTest.pathAndTextRules`, `TomcatLimitsIntegrationTest.longDescriptionIsAFieldErrorNotATooLargeUpload`, UI-T `format.test.ts` |
| D38 searches | `api/VocabularyController.search`, `api/ExternalSearchBody` | `VocabularyServiceTest.readsAndSearch`, `ExternalVocabularyServiceTest.searchKeepsFirstMatchOrderAndDeduplicates`, `ApiIntegrationTest.jsonBodies` |
| D39 | see `docs/api-notes.md` | Schemathesis run |
| D43 serialised changes | `service/VocabularyLocks` | `VocabularyLocksTest`, `VocabularyServiceTest.parallelVersionCreationsAreSerialised` |
| D44 store and identity provider down | `store/SparqlClient`, `security/RealmKeySource`, `api/HealthController` | `SparqlClientTest.*`, `RealmKeySourceTest.*`, `StoreDownIntegrationTest`; live check on port 8199 |
| D45 pull only | no events published | n/a |
| D46 identical version | `service/VocabularyService` | `VocabularyServiceTest.identicalContentAndDescriptionIsAConflictButChangelogIsIgnored` |
| D48 problem types | `problem/Problems` | `ApiIntegrationTest.*` |
| D51 order of checks | controllers and `service/VocabularyService` | `ApiIntegrationTest.orderOfChecksOnWrites`, `VocabularyServiceTest.duplicateNameIsCaseInsensitiveConflictEvenWithInvalidContent` |
| D52 unknown fields and parameters | `api/RequestChecks`, `api/JsonBodyReader` | `ApiIntegrationTest.jsonBodies`, `readValidation` |
| D54 exact case in paths | `store/SparqlVocabularyStore.findVocabulary` | `ApiIntegrationTest.readValidation` |
| D55 fixed texts | `problem/Problems` | `ApiIntegrationTest.*` |
| E1 Keycloak realm | `keycloak/realm-simpl.json` | SMOKE |
| E2, E4 configuration from environment | `backend/src/main/resources/application.yaml`, `compose.yaml` | n/a |
| E3 Fuseki store | `fuseki/Dockerfile`, `store/*` | `ApiIntegrationTest` (embedded Fuseki) |
