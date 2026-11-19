# Traceability: Vocabulary Manager (run 8)

Every in-scope story criterion and every decision, mapped to the code that implements it and to the build's own tests.

## Conventions

- Criterion ids: the Given/When/Then scenarios of each story in `input/specs/stories/`, numbered AC1, AC2, ... in the order they appear. This is the numbering the decisions use (for example SIMPL-18173/AC6, SIMPL-18190/AC2).
- Backend code paths are relative to `backend/src/main/java/eu/simpl/vocabularymanager/` and UI paths to `ui/src/`.
- Test abbreviations. All JUnit tests are in `backend/src/test/java/eu/simpl/vocabularymanager/`.
  - VAT: `api/VocabularyApiTest`
  - SST: `api/SecurityAndShapeApiTest`
  - EAT: `api/ExternalVocabularyApiTest`
  - CFT: `api/ConcurrencyAndFailureTest`
  - SHT: `store/PhysicalModelShaclTest`
  - DVT: `unit/DomainAndValidationTest`
  - IUT: `unit/InfrastructureUnitTest`
  - SMOKE: `smoke.sh`
  - BRT: `ops/backup-restore-test.sh`
  - FUZZ: the Schemathesis run in `reports/fuzz-schemathesis.txt`
  - PW: the manual Playwright checks run once against the live stack. They are not kept as tests in the repository.
- The UI has no automated test of its own in this repository. Where a criterion is UI behaviour, the test column says so. For such criteria it names the manual PW check, if one was done, and the API test that covers the server side.
- PW checks done:
  - (a) Keycloak login, then the list loads.
  - (b) List column headers: Actions, Name, Description, Latest version, Status, Date created, Last updated, Creator. The 'New Vocabulary +' button is present for ga-admin.
  - (c) The create form has the labels Name, Description and Vocabulary file, and no Change log field.
  - (d) A file with a syntax error gives 'Line 3: ...' and disables Submit; a valid file gives 'Syntax check passed.'.
  - (e) A create redirects to `/vocabularies/<Name>` with the status message 'Version 1 of <Name> was created.'.
  - (f) The version table headers on the details page are Version, Status, Date created, Description, Change log and Actions.

## Stories in and out of scope

| Story | Scope | Reason |
|---|---|---|
| SIMPL-181xx (30 stories) | in | D1 |
| SIMPL-18452, SIMPL-18559 | in | D1 |
| SIMPL-393 | met by the generic upload, no own criteria | D1: no seed data. A CPSV-like Turtle file can be uploaded with POST /vocabularies. It is covered by VAT `createReturnsVersionOneActiveWithLinksAndCreator`. |
| SIMPL-395 | out | D1: replaced by the SIMPL-181xx stories. No deletion of internal vocabularies, no wrapping of catalogue calls. |
| SIMPL-28680 | out | D1, D61: belongs to Schema Manager and is not in the live set. |

## Criteria

| Criterion | Short | Code | Own tests |
|---|---|---|---|
| SIMPL-18161/AC1 | List of vocabularies with actions menu and columns | `views/VocabularyList.vue`, `components/ActionsMenu.vue`. API: `web/api/VocabularyController` (GET /vocabularies) | UI: PW (a), (b). API: VAT `listFiltersSortsAndPages`, SMOKE |
| SIMPL-18161/AC2 | List load error shown | `views/VocabularyList.vue` (TEXT.listLoadError plus problem detail, D34, D72) | UI: none |
| SIMPL-18162/AC1 | GET /vocabularies 200 | `web/api/VocabularyController`, `store/VocabularyRepository` | VAT `listFiltersSortsAndPages`, SMOKE |
| SIMPL-18162/AC2 | 400 Bad request | `web/routing/RequestShapeInterceptor`, `web/api/ListQuery`, `web/api/Paging` | VAT `listFiltersSortsAndPages` (13 invalid queries) |
| SIMPL-18162/AC3 | 403 Forbidden | `security/SecurityConfig` | SST `rolesDecideReadAndWrite` |
| SIMPL-18162/AC4 | 500 Internal error | `web/problem/ApiExceptionHandler`, `store/HttpSparqlStore` | CFT `storeErrorsAndTimeouts` |
| SIMPL-18163/AC1 | Filter in UI | `views/VocabularyList.vue` (Name contains, Vocabulary status, Created on) | UI: none. API: VAT `listFiltersSortsAndPages` |
| SIMPL-18163/AC2 | Sort by column in UI | `views/VocabularyList.vue` (`sortBy`, aria-sort, D23, D50) | UI: none. API: VAT `listFiltersSortsAndPages` |
| SIMPL-18164/AC1 | Filter and sort API | `web/api/ListQuery`, `store/VocabularyRepository` (ORDER BY in the store, D85) | VAT `listFiltersSortsAndPages` |
| SIMPL-18164/AC2 | No match gives empty result | same | VAT `listFiltersSortsAndPages` (name=Zz...) |
| SIMPL-18164/AC3 | Pagination with sorting | `web/api/Paging`, `web/api/Representations` | VAT `listFiltersSortsAndPages` (page 2 of 3, beyond last page) |
| SIMPL-18164/AC4 | Unsupported filter or sort gives 400 | `web/api/ListQuery`, `web/routing/RequestShapeInterceptor` | VAT `listFiltersSortsAndPages`, `versionListSortsAndValidates`, FUZZ |
| SIMPL-18165/AC1 | Download newest version in UI | `views/VocabularyList.vue` ('Download vocabulary'), `api.ts`. API: `web/api/ContentController` | UI: none. API: VAT `contentDownloadsReturnTheStoredBytes`, SMOKE |
| SIMPL-18166/AC1 | Version details via API | `web/api/VocabularyController` (GET .../versions/{version}), `web/api/Representations` | VAT `versionListSortsAndValidates`, `contentDownloadsReturnTheStoredBytes` |
| SIMPL-18166/AC2 | Version not found | `web/problem/Problems`, `service/VocabularyService` | VAT `versionStatusChangeAndReactivation` (version 99), `contentDownloadsReturnTheStoredBytes` |
| SIMPL-18167/AC1 | History opens details page | `views/VocabularyList.vue` ('History' to /vocabularies/<Name>), `views/VocabularyDetails.vue` | UI: PW (f), partly (details page reached after create, not through 'History') |
| SIMPL-18168/AC1 | Version history via API | `web/api/VocabularyController` (GET .../versions), `store/VocabularyRepository` | VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus`, `versionListSortsAndValidates`, SMOKE |
| SIMPL-18168/AC2 | Vocabulary not found | `web/problem/Problems` (D66) | VAT `versionListSortsAndValidates`, `versionStatusChangeAndReactivation` |
| SIMPL-18169/AC1 | Creation form | `views/VocabularyForm.vue`, `router.ts` (/vocabularies/new, D74) | UI: PW (c) |
| SIMPL-18169/AC2 | Form validates input | `views/VocabularyForm.vue`, `fieldRules.ts` (D37, D62). API: `domain/FieldRules` | UI: none. API: VAT `fieldRulesOnCreateAndNewVersion`, DVT `nameRules` |
| SIMPL-18170/AC1 | Create via API validates | `web/api/VocabularyController`, `service/VocabularyService`, `validation/*` | VAT `createReturnsVersionOneActiveWithLinksAndCreator`, `semanticRulesReportEveryError`, SMOKE |
| SIMPL-18170/AC2 | Created vocabulary has version 1 ACTIVE | `service/VocabularyService`, `store/VocabularyRepository` | VAT `createReturnsVersionOneActiveWithLinksAndCreator` |
| SIMPL-18170/AC3 | Invalid data rejected | `web/api/VocabularyController`, `web/body/MultipartForm` | VAT `fieldRulesOnCreateAndNewVersion`, `syntaxErrorStopsBeforeSemanticValidation`, `emptyFileFailsWithNoStatements` |
| SIMPL-18172/AC1 | Update form pre-filled | `views/VocabularyForm.vue` ('<Name>.ttl (current content)', D33) | UI: none |
| SIMPL-18172/AC2 | New version created from UI | `views/VocabularyForm.vue`, `api.ts` | UI: none. API: VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus` |
| SIMPL-18172/AC3 | Unexpected error, nothing created | `views/VocabularyForm.vue` (TEXT.genericError, D60). API: D8 | UI: none. API: CFT `storeErrorsAndTimeouts`, `unreachableStoreGives503AndHealthDown` |
| SIMPL-18173/AC1 | New version via API | `web/api/VocabularyController`, `service/VocabularyService`, `service/ConditionalWrite` | VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus`, SMOKE |
| SIMPL-18173/AC2 | Invalid data rejected | same, `web/body/MultipartForm` | VAT `fieldRulesOnCreateAndNewVersion`, `semanticRulesReportEveryError` |
| SIMPL-18173/AC3 | Unknown vocabulary rejected | `service/VocabularyService` | VAT `fieldRulesOnCreateAndNewVersion` (Nope... gives 404) |
| SIMPL-18173/AC4 | Insufficient permissions | `security/SecurityConfig` | SST `rolesDecideReadAndWrite`, SMOKE (viewer gets 403) |
| SIMPL-18173/AC5 | Not authenticated | `security/TokenAuthenticationFilter`, `security/SecurityConfig` | SST `missingOrInvalidTokensGive401`, SMOKE |
| SIMPL-18173/AC6 | Unsupported format gives 415 | `web/routing/RequestShapeInterceptor` | SST `mediaTypesAndAcceptHeader` |
| SIMPL-18174/AC1 | Status change in UI | `views/VocabularyList.vue`, `views/VocabularyDetails.vue` ('Deprecate' or 'Activate', D60) | UI: none. API: VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus` |
| SIMPL-18174/AC2 | Vocabulary no longer available | `views/VocabularyList.vue` (TEXT.statusNotFound, D72) | UI: none |
| SIMPL-18174/AC3 | Update fails | `views/VocabularyList.vue` (TEXT.statusFailed plus D60 text) | UI: none |
| SIMPL-18175/AC1 | PATCH status | `web/api/VocabularyController`, `service/VocabularyService` | VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus` |
| SIMPL-18175/AC2 | Unknown vocabulary gives 404 | same | VAT `versionStatusChangeAndReactivation` |
| SIMPL-18175/AC3 | Other value gives 400 | `domain/VocabularyStatus`, `web/body/JsonBody` | VAT `versionStatusChangeAndReactivation`, DVT `statusValuesAreExact` |
| SIMPL-18176/AC1 | Viewer may see deprecated versions | `security/SecurityConfig` (read rule), `views/VocabularyDetails.vue` | UI: none. API: SST `rolesDecideReadAndWrite` |
| SIMPL-18176/AC2 | Deprecated versions displayed | `views/VocabularyDetails.vue` (all versions, pageSize 100, D35) | UI: PW (f), partly. API: VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus` |
| SIMPL-18176/AC3 | Metadata of a deprecated version | `views/VocabularyDetails.vue` (version panel, region 'Version <n>') | UI: none. API: VAT `versionListSortsAndValidates` |
| SIMPL-18178/AC1 | Warning on a deprecated vocabulary | `views/VocabularyList.vue` (icon with accessible name and tooltip, D72), `views/VocabularyDetails.vue` (banner, D36) | UI: none |
| SIMPL-18180/AC1 | Validation on upload | `turtleCheck.ts` (n3 parser, D13), `views/VocabularyForm.vue` | UI: PW (d) |
| SIMPL-18180/AC2 | Validation on submit (create) | server: `validation/VocabularyContentValidator` | UI: none. API: VAT `syntaxErrorStopsBeforeSemanticValidation`, `semanticRulesReportEveryError` |
| SIMPL-18180/AC3 | Validation on submit (version) | same | UI: none. API: VAT `fieldRulesOnCreateAndNewVersion` |
| SIMPL-18180/AC4 | Successful validation shown | `views/VocabularyForm.vue` ('Syntax check passed.'), details page notification | UI: PW (d), (e) |
| SIMPL-18180/AC5 | Syntax errors shown | `turtleCheck.ts`, `views/VocabularyForm.vue` (report panel 'Validation report') | UI: PW (d) for the browser check. API: VAT `syntaxErrorStopsBeforeSemanticValidation`, DVT `parserReportsFirstSyntaxErrorWithLineAndColumn` |
| SIMPL-18180/AC6 | Structure errors shown | `views/VocabularyForm.vue` (report panel lists issues) | UI: none. API: VAT `semanticRulesReportEveryError` |
| SIMPL-18180/AC7 | Multiple errors | `validation/SemanticRules` (all rules run, D9) | UI: none. API: VAT `semanticRulesReportEveryError`, DVT `semanticRulesOneByOne` |
| SIMPL-18180/AC8 | Cannot proceed until fixed | `views/VocabularyForm.vue` (Submit disabled on upload errors, D62; server 400 keeps form open) | UI: PW (d) |
| SIMPL-18181/AC1 | Syntax errors via API | `validation/TurtleParser` (Apache Jena RIOT) | VAT `syntaxErrorStopsBeforeSemanticValidation`, DVT `parserReportsFirstSyntaxErrorWithLineAndColumn` |
| SIMPL-18181/AC2 | Valid syntax passes | same | VAT `createReturnsVersionOneActiveWithLinksAndCreator` |
| SIMPL-18182/AC1 | Semantic after syntax | `validation/VocabularyContentValidator` | UI: none. API: VAT `syntaxErrorStopsBeforeSemanticValidation`, DVT `validatorCombinesStepsAndChecksExternalContent` |
| SIMPL-18182/AC2 | Semantic success | same | UI: PW (e). API: VAT `createReturnsVersionOneActiveWithLinksAndCreator` |
| SIMPL-18182/AC3 | Semantic error report | `views/VocabularyForm.vue` report panel | UI: none. API: VAT `semanticRulesReportEveryError` |
| SIMPL-18183/AC1 | Semantic fails via API | `validation/SemanticRules`, `validation/ReferenceRules` | VAT `semanticRulesReportEveryError`, DVT `semanticRulesOneByOne` |
| SIMPL-18183/AC2 | Semantic passes via API | same | VAT `createReturnsVersionOneActiveWithLinksAndCreator` |
| SIMPL-18184/AC1 | Status of referenced versions checked | `validation/ReferenceRules`, `store/VocabularyRepository.lookup` (one query, Q5) | VAT `referenceWarningsForDeprecatedAndExternalNamespaces`, DVT `referenceRulesWarnAndErrorWithOneLookup` |
| SIMPL-18184/AC2 | Deprecated reference (warn, not block, D11) | same | VAT `referenceWarningsForDeprecatedAndExternalNamespaces` |
| SIMPL-18184/AC3 | Missing reference blocks (S7) | same | VAT `semanticRulesReportEveryError`, DVT `referenceRulesWarnAndErrorWithOneLookup` |
| SIMPL-18184/AC4 | UI warning for deprecated reference | `components/WarningsPanel.vue` ('Validation warnings', D14) | UI: none. API: VAT `referenceWarningsForDeprecatedAndExternalNamespaces` |
| SIMPL-18186/AC1 | Automatic publication as ACTIVE | `service/VocabularyService` (D6) | UI: PW (e). API: VAT `createReturnsVersionOneActiveWithLinksAndCreator` |
| SIMPL-18187/AC1 | Vocabulary persisted | `store/VocabularyRepository` | VAT `createReturnsVersionOneActiveWithLinksAndCreator`, SHT `storedDataConformsToTheShapes`, BRT |
| SIMPL-18187/AC2 | Version persisted | same | VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus`, SHT |
| SIMPL-18189/AC1 | Next version identifier in UI | server: `service/VocabularyService`. UI: `views/VocabularyForm.vue` (D46 line) | UI: none. API: VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus` |
| SIMPL-18189/AC2 | No manual version input | `views/VocabularyForm.vue` (no version field) | UI: PW (c), create form only |
| SIMPL-18189/AC3 | Confirmation with version | `views/VocabularyDetails.vue` (`versionCreated`, D14) | UI: PW (e) |
| SIMPL-18190/AC1 | Automatic next number via API | `service/VocabularyService`, `service/ConditionalWrite` (D7, D82) | VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus`, CFT `parallelVersionsOnOneInstanceGetDistinctNumbers` |
| SIMPL-18190/AC2 | Manual version gives 400 | `web/api/VocabularyController`, `web/body/MultipartForm` (D7) | VAT `fieldRulesOnCreateAndNewVersion` |
| SIMPL-18191/AC1 | New ACTIVE, older DEPRECATED | `store/VocabularyRepository` (one conditional update, D4) | UI: none. API: VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus`, `versionStatusChangeAndReactivation`, CFT |
| SIMPL-18193/AC1 | UI confirms success | `views/VocabularyDetails.vue` notification | UI: PW (e) |
| SIMPL-18193/AC2 | UI report with nature and location | `views/VocabularyForm.vue` report panel, `components/ProblemMessage.vue` | UI: none. API: VAT `semanticRulesReportEveryError` |
| SIMPL-18194/AC1 | API report with all errors and locations | `web/problem/Problems`, `web/problem/ProblemIssue` (D10, D65) | VAT `semanticRulesReportEveryError`, `syntaxErrorStopsBeforeSemanticValidation` |
| SIMPL-18194/AC2 | Success inside the 201 | `web/api/Representations` (validationWarnings always present) | VAT `createReturnsVersionOneActiveWithLinksAndCreator` |
| SIMPL-18195/AC1 | Admin sees actions | `context.ts` (`isAdmin`), `views/*.vue` | UI: PW (b) ('New Vocabulary +' for ga-admin) |
| SIMPL-18195/AC2 | Restricted actions hidden | same, `views/VocabularyForm.vue` (TEXT.notAuthorized, D18) | UI: none |
| SIMPL-18195/AC3 | Unauthenticated user prompted | `auth.ts` (keycloak-js `login-required`) | UI: PW (a) |
| SIMPL-18196/AC1 | Authorised request processed | `security/SecurityConfig`, `web/routing/OperationCatalogue` | SST `rolesDecideReadAndWrite`, SMOKE |
| SIMPL-18196/AC2 | Wrong role gives 403 | same | SST `rolesDecideReadAndWrite`, SMOKE |
| SIMPL-18196/AC3 | No token gives 401 | `security/TokenAuthenticationFilter`, `security/TokenValidator` | SST `missingOrInvalidTokensGive401`, SMOKE |
| SIMPL-18196/AC4 | Every endpoint needs a token and a role | `web/routing/OperationCatalogue`, `config/HandlerRuleVerifier` (D84) | IUT `everyOperationHasARoleRuleAndUnknownRequestsResolveToNothing`, `handlerWithoutRuleIsReported`, SST `unknownPathsAndMethods` |
| SIMPL-18452/AC1 | Select a version | `views/VocabularyDetails.vue` (version panel) | UI: none. API: VAT `versionListSortsAndValidates` |
| SIMPL-18452/AC2 | Download the selected version | `views/VocabularyDetails.vue` (Download), `web/api/ContentController` (`<Name>-v<n>.ttl`) | UI: none. API: VAT `contentDownloadsReturnTheStoredBytes` |
| SIMPL-18559/AC1 | Warning and link to the latest ACTIVE version | `views/VocabularyDetails.vue` (`effectivelyDeprecated`, `activeLink`, D36, D64) | UI: none |
| SIMPL-18559/AC2 | Warning without an ACTIVE version | same | UI: none |

Totals: 85 criteria in 32 stories, all mapped to code. 41 are server behaviour and each has an automated own test. 44 are UI behaviour and have no automated UI test in the repository. Of those 44, 13 had a manual Playwright check, two of them only partly (SIMPL-18167/AC1, SIMPL-18176/AC2), and 31 had none. Most of the 31 have their server side covered by an API test.

## Decisions

| Id | Topic | Code | Own tests | Status |
|---|---|---|---|---|
| D1 | Scope | whole build; no seed data, no deletion of internal vocabularies | (scope) | implemented |
| D2 | CDM snapshot | `docs/logical-data-model.md` | review only | implemented |
| D3 | Two statuses, effectively deprecated | `service/VocabularyService`, `validation/ReferenceRules`, `views/VocabularyDetails.vue` | VAT `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus`, `referenceWarningsForDeprecatedAndExternalNamespaces` | implemented |
| D4 | Version transitions, same status 409 | `service/VocabularyService`, `store/VocabularyRepository` | VAT `versionStatusChangeAndReactivation` | implemented |
| D5 | Latest version, latestVersionHref | `web/api/Representations` | VAT `createReturnsVersionOneActiveWithLinksAndCreator`, `newVersionDeprecatesOlderActiveVersionsAndKeepsVocabularyStatus` | implemented |
| D6 | Publish = ACTIVE on store | `service/VocabularyService` | VAT `createReturnsVersionOneActiveWithLinksAndCreator` | implemented |
| D7 | Integer versions, version field 400 | `service/VocabularyService`, `web/api/VocabularyController` | VAT `fieldRulesOnCreateAndNewVersion` | implemented |
| D8 | Nothing stored on failure | `store/VocabularyRepository` (one update per write) | VAT `fieldRulesOnCreateAndNewVersion`, `syntaxErrorStopsBeforeSemanticValidation`, CFT | implemented |
| D9 | Syntax then S1 to S7 | `validation/TurtleParser`, `validation/SemanticRules`, `validation/ReferenceRules` | VAT `semanticRulesReportEveryError`, DVT `semanticRulesOneByOne` | implemented |
| D10 | Errors versus warnings | `validation/ValidationWarning`, `web/api/Representations` | VAT `referenceWarningsForDeprecatedAndExternalNamespaces` | implemented (semanticReasoningSkipped reserved, not emitted, as decided) |
| D11 | Deprecated reference warns | `validation/ReferenceRules` | VAT `referenceWarningsForDeprecatedAndExternalNamespaces`, DVT | implemented |
| D12 | Reference recognition | `validation/ReferenceRules` | DVT `referenceRulesWarnAndErrorWithOneLookup`, EAT `registerGetListContentAndDelete` | implemented |
| D13 | Browser syntax check | `turtleCheck.ts` (n3 2.13.12), `views/VocabularyForm.vue` | PW (d) | implemented, UI not auto-tested |
| D14 | Report and success feedback | `views/VocabularyForm.vue`, `views/VocabularyDetails.vue`, `components/WarningsPanel.vue` | PW (e) | implemented, UI not auto-tested |
| D15 | Turtle, 5 MB, 413, 415, 409 | `web/body/MultipartForm`, `web/routing/RequestShapeInterceptor` | SST `uploadLimits`, `mediaTypesAndAcceptHeader`, VAT `duplicateNameIsCaseInsensitiveAndWinsOverInvalidContent` | implemented |
| D16 | Roles | `web/routing/OperationCatalogue`, `security/SecurityConfig` | SST `rolesDecideReadAndWrite`, IUT | implemented |
| D17 | 401 and 403 | `security/TokenAuthenticationFilter`, `security/SecurityConfig` | SST `missingOrInvalidTokensGive401`, `rolesDecideReadAndWrite` | implemented |
| D18 | Restricted actions hidden in UI | `context.ts`, `views/*.vue` | PW (b), partly | implemented, UI not auto-tested |
| D19 | Name filter contains | `web/api/ListQuery`, `store/VocabularyRepository` | VAT `listFiltersSortsAndPages`, FUZZ (ST201 on name=000 is this decided change) | implemented |
| D20 | Date filters | `web/api/ListQuery` | VAT `listFiltersSortsAndPages` | implemented |
| D21 | Unknown query parameters 400 | `web/routing/RequestShapeInterceptor` | VAT `listFiltersSortsAndPages`, SST `healthNeedsNoTokenAndReportsUp` | implemented |
| D22 | Paging limits and links | `web/api/Paging` | VAT `listFiltersSortsAndPages`, EAT | implemented |
| D23 | Sorting | `web/api/ListQuery`, `views/VocabularyList.vue` | VAT `listFiltersSortsAndPages`, `versionListSortsAndValidates` | implemented |
| D24 | Status codes | `web/problem/Problems` | VAT, SST | implemented |
| D25 | Vocabulary object | `web/api/Representations`, `store/VocabularyRepository` | VAT `createReturnsVersionOneActiveWithLinksAndCreator` | implemented |
| D26 | Version object, Location | `web/api/Representations`, `web/api/Responses` | VAT `createReturnsVersionOneActiveWithLinksAndCreator` | implemented |
| D27 | Creator | `security/TokenValidator` (email, else preferred_username) | SST `rolesDecideReadAndWrite` (no email), VAT | implemented |
| D28 | Description per version | `store/VocabularyRepository` | VAT `createReturnsVersionOneActiveWithLinksAndCreator` | implemented |
| D29 | Content download | `web/api/ContentController` | VAT `contentDownloadsReturnTheStoredBytes`, SMOKE | implemented |
| D30 | External identity and keys | `service/ExternalVocabularyService`, `store/ExternalVocabularyRepository` | EAT `registrationConflictsAndRules` | implemented |
| D31 | Glossary terms | code, API and `texts.ts` | review only | implemented |
| D32 | Changelog 'Initial version.' | `texts.ts` (`initialChangelog`), `views/VocabularyForm.vue` | PW (c) | implemented |
| D33 | New version form | `views/VocabularyForm.vue` | none | implemented, UI not tested |
| D34 | List paging and empty state | `views/VocabularyList.vue` | none | implemented, UI not tested |
| D35 | Details and history page | `views/VocabularyDetails.vue` | PW (f) | implemented, UI not auto-tested |
| D36 | Deprecation warnings | `views/VocabularyList.vue`, `views/VocabularyDetails.vue` | none | implemented, UI not tested |
| D37 | Field rules | `domain/FieldRules`, `fieldRules.ts` | VAT `fieldRulesOnCreateAndNewVersion`, DVT `nameRules`, `versionTextAndUriRules` | implemented |
| D38 | Search operations | `web/api/VocabularyController`, `web/api/ExternalVocabularyController` | VAT `searchReturnsResultsInRequestOrder`, EAT `searchByUris` | implemented |
| D39 | Decisions over OpenAPI | `docs/openapi-decided.yaml` | FUZZ | implemented |
| D40 | OIDC via Keycloak | `keycloak/realm-simpl.json`, `auth.ts`, `security/*` | SMOKE | implemented |
| D41 | External operations test basis | `web/api/ExternalVocabularyController` | EAT (all) | implemented |
| D42 | External delete without check | `service/ExternalVocabularyService` | EAT `registerGetListContentAndDelete` | implemented |
| D43 | Concurrent changes | `service/ConditionalWrite` (via D82) | CFT `parallelVersionsOnOneInstanceGetDistinctNumbers`, `twoInstancesOnOneStoreSerialiseThroughTheStore` | implemented |
| D44 | Store and IdP down | `store/HttpSparqlStore`, `security/RealmKeySource`, `web/api/HealthController` | CFT `unreachableStoreGives503AndHealthDown`, `storeErrorsAndTimeouts`, `healthIsDegradedWithoutKeysAndIdentityProvider`, SST `identityProviderDownFailsClosedForUnknownKeys`, IUT `keySourceFailsClosedAndRecovers` | implemented |
| D45 | Pull only, no events | no messaging in the build | (absence) | implemented |
| D46 | Identical version 409 | `service/VocabularyService`, `texts.ts` (`nextVersionLine`) | VAT `identicalVersionIsRefusedAndDescriptionOnlyChangeIsAccepted` | implemented |
| D47 | Reference texts | `validation/ReferenceRules` | VAT `referenceWarningsForDeprecatedAndExternalNamespaces`, `semanticRulesReportEveryError`, DVT | implemented |
| D48 | Problem types | `web/problem/Problems` | SST, CFT | implemented (deviation from API-46 recorded in DEVIATIONS.md) |
| D49 | Invalid path values 400 | `web/routing/RequestShapeInterceptor` | VAT `fieldRulesOnCreateAndNewVersion`, `versionListSortsAndValidates`, EAT | implemented |
| D50 | Active sort column click | `views/VocabularyList.vue` (`sortBy`) | none | implemented, UI not tested |
| D51 | Order of checks | `security/*`, `web/routing/RequestShapeInterceptor`, controllers, `service/*` | SST `uploadLimits`, `mediaTypesAndAcceptHeader`, VAT `duplicateNameIsCaseInsensitiveAndWinsOverInvalidContent`, `fieldRulesOnCreateAndNewVersion` | implemented |
| D52 | Unknown fields and parameters | `web/body/MultipartForm`, `web/body/JsonBody`, `web/routing/RequestShapeInterceptor` | VAT `fieldRulesOnCreateAndNewVersion`, `searchReturnsResultsInRequestOrder` | implemented |
| D53 | Untagged labels | `validation/SemanticRules` | DVT `semanticRulesOneByOne`, VAT | implemented |
| D54 | Exact case in paths | `store/VocabularyRepository` | VAT `duplicateNameIsCaseInsensitiveAndWinsOverInvalidContent` | implemented |
| D55 | Fixed problem texts | `web/problem/Problems` | VAT `versionStatusChangeAndReactivation`, EAT `registrationConflictsAndRules` | implemented |
| D56 | External registration details | `service/ExternalVocabularyService` | EAT `registrationConflictsAndRules` | implemented |
| D57 | Base IRI | `service/VocabularyService`, `validation/TurtleParser` | DVT `parserResolvesRelativeIrisAgainstTheVersionUrl` | implemented |
| D58 | UTC times, code point order | `store/StoreModel`, SPARQL ORDER BY | VAT `createReturnsVersionOneActiveWithLinksAndCreator` (time format) | implemented; code point order not tested separately |
| D59 | prev beyond last page | `web/api/Paging` | VAT `listFiltersSortsAndPages` | implemented |
| D60 | UI texts | `texts.ts`, `format.ts` | PW (c), (e) partly | implemented, UI not auto-tested |
| D61 | SIMPL-28680 out | (scope) | (scope) | implemented |
| D62 | Submit with field errors | `views/VocabularyForm.vue` (`focusFirstError`) | PW (d), upload error case only | implemented, UI not auto-tested |
| D63 | 406 | `web/routing/RequestShapeInterceptor` | SST `mediaTypesAndAcceptHeader`, VAT `contentDownloadsReturnTheStoredBytes` | implemented |
| D64 | Latest ACTIVE link rules | `views/VocabularyDetails.vue` (`activeLink`) | none | implemented, UI not tested |
| D65 | Location texts | `validation/SemanticRules`, `validation/PrefixDeclaration`, `validation/TurtleSyntaxException` | VAT `semanticRulesReportEveryError`, DVT | implemented (the literal in S1 is written without quotes, following the OpenAPI example) |
| D66 | Not-found texts | `web/problem/Problems` | VAT `versionStatusChangeAndReactivation`, EAT `registerGetListContentAndDelete` | implemented |
| D67 | Before routing: 405, firewall 400, token | `web/routing/RoutingGuardFilter`, `security/SecurityConfig` (RequestRejectedHandler), `config/TomcatConfig`, management port | SST `unknownPathsAndMethods`, `tomcatLevelErrorsHaveProblemBodies`, `managementPortServesHealthAndMetricsWithoutToken`, FUZZ | implemented |
| D68 | Search issue names | `web/api/VocabularyController`, `web/api/ExternalVocabularyController` | VAT `searchReturnsResultsInRequestOrder`, EAT `searchByUris` | implemented |
| D69 | Internal reference shapes | `validation/ReferenceRules` | VAT `semanticRulesReportEveryError`, DVT | implemented |
| D70 | Content and multipart details | `web/api/ContentController`, `web/body/MultipartForm` (empty part name reported) | VAT `fieldRulesOnCreateAndNewVersion`, `emptyFileFailsWithNoStatements`, EAT `registrationConflictsAndRules` | implemented |
| D71 | Time budget of writes | `config/VocabularyManagerProperties` (update timeout 45 s, budget 30 s) | measured: 5 MB create 21.9 s (QUALITY.md), SST `uploadLimits` | implemented |
| D72 | UI labels and texts | `views/*.vue`, `texts.ts` | PW (b), (e) | implemented, UI not auto-tested |
| D73 | Absolute URIs, order as sent | `domain/FieldRules`, `service/ExternalVocabularyService` | EAT `registerGetListContentAndDelete`, `registrationConflictsAndRules`, DVT | implemented |
| D74 | Form routes | `router.ts` | PW (c) | implemented |
| D75 | UI semantics for tests | `views/*.vue`, `components/*.vue` (roles, region names, aria labels) | PW partly | implemented, UI not auto-tested |
| D76 | Part 5 MB, request 6 MB | `web/body/MultipartForm`, `web/routing/RequestShapeInterceptor`, `views/VocabularyForm.vue` | SST `uploadLimits` | implemented |
| D77 | Fixed segment over parameter | `web/routing/OperationCatalogue` | SST `unknownPathsAndMethods` (GET /vocabularies/searches gives 405), IUT | implemented |
| D78 | Links keep the request | `web/api/Paging` | VAT `listFiltersSortsAndPages` | implemented |
| D79 | Contract changes | build behaviour plus `docs/openapi-decided.yaml` | FUZZ (ST202 and ST201 findings are these changes) | implemented |
| D80 | No personal data in logs | `web/trace/TraceContext` (MDC userId = sub), `security/TokenAuthenticationFilter` | review only | implemented, no own test |
| D81 | One instance (withdrawn) | n/a | n/a | withdrawn by D82 |
| D82 | Concurrency through the store | `service/ConditionalWrite`, `store/VocabularyRepository` (change records) | CFT `twoInstancesOnOneStoreSerialiseThroughTheStore` | implemented |
| D83 | Store access | `fuseki/config.ttl`, `fuseki/entrypoint.sh`, `compose.yaml` (internal store-net, no port), `store/HttpSparqlStore` | BRT | implemented |
| D84 | Default deny | `security/SecurityConfig.decide`, `config/HandlerRuleVerifier` | IUT `handlerWithoutRuleIsReported`, `everyOperationHasARoleRuleAndUnknownRequestsResolveToNothing`, SST `unknownPathsAndMethods` | implemented |
| D85 | Bounded store queries | `store/VocabularyRepository`, `store/ExternalVocabularyRepository` | VAT `listFiltersSortsAndPages`, DVT `referenceRulesWarnAndErrorWithOneLookup` | implemented; the LIMIT rule is checked by review, not by a test |
| D86 | Metrics and trace context | `web/trace/CorrelationFilter`, `web/trace/TraceContext` (forwarded in `store/HttpSparqlStore`, `security/RealmKeySource`), Micrometer `http_server_requests` | SST `correlationHeadersAreReturned`, `managementPortServesHealthAndMetricsWithoutToken` | implemented |
| D87 | Model and contract deliverables | `docs/logical-data-model.md`, `docs/physical-model.shacl.ttl`, `docs/openapi-decided.yaml` | SHT `storedDataConformsToTheShapes` | implemented |
| E1 | Keycloak realm, clients, users | `keycloak/realm-simpl.json` | SMOKE | implemented |
| E2 | Ports and base URLs | `application.yml`, `compose.yaml` (ports from build.yaml) | SMOKE | implemented |
| E3 | Fuseki dataset 'vocabulary' | `fuseki/*` | BRT, SHT | implemented (reset sentence withdrawn by E4) |
| E4 | Environment from the profile | `application.yml` (every value from env), `stack.sh` | SMOKE | implemented |
| E5 | Audience and issuer | `security/TokenValidator` | SST `missingOrInvalidTokensGive401` | implemented |
| E6 | Reuse choices | see REUSE.md | n/a | implemented |
| E7 | noaud client and simpl-other realm | `keycloak/realm-simpl.json`, `keycloak/realm-simpl-other.json` | probed on the live stack (401 for both); the JUnit equivalent is SST `missingOrInvalidTokensGive401` | implemented |
| Q1 | Response time and throughput | whole stack | measured in QUALITY.md (reads p95 30 ms at 20/s, writes p95 83 ms at 1/s, 5 MB in 21.9 s) | met |
| Q2 | Limits | see D15, D22, D37, D38, D56, D71, D76 | SST `uploadLimits`, VAT, EAT (tests at and above each limit) | implemented |
| Q3 | Availability and recovery | `ops/backup.sh`, `ops/restore.sh` | BRT | partly: backup and restore and their round trip are in place; the daily schedule and the off-host copy are left to the deployment |
| Q4 | Design load | single instance, scale out allowed (D82) | load runs in QUALITY.md (20 readers, 1 writer; second run with 1000 vocabularies stored) | partly: 1000 vocabularies with 2 versions each were loaded, not 20 versions each |
| Q5 | Maintainability | JaCoCo check in `pom.xml` (line 80 %, branch 60 %) | `mvn verify`: line 94.5 %, branch 83.7 %; Trivy 0 HIGH/CRITICAL in own images | implemented |
| Q6 | Accessibility | roles, labels and aria attributes in `views/*.vue` | none: no axe run in this build | not verified |

## Not implemented or not verified

- Q6: no axe check was run. WCAG 2.1 AA is not shown.
- Q3: there is no daily schedule and no copy outside the host. The scripts exist and the round trip passes.
- Q4: the design-load data set was loaded with 1000 vocabularies of 2 versions each, not 20 versions each.
- UI criteria: 31 of the 44 UI criteria have no own test, not even a manual check. They are listed above.
