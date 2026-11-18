# Traceability

Stories, acceptance criteria and decisions mapped to the code and to my own tests. Criteria without an id in the story
are numbered AC1, AC2, ... in the order they appear.

Test abbreviations (backend tests in `backend/src/test/java/eu/simpl/vocabularymanager/`):

| Short | Test |
|---|---|
| CV | `validation/ContentValidatorTest` |
| VS | `service/VocabularyServiceTest` |
| TV | `security/TokenValidatorTest` |
| WH | `web/WebHelpersTest` |
| IT | `it/VocabularyApiIT` (real Tomcat, embedded Fuseki, mocked identity provider) |
| SD | `it/StoreDownIT` |
| AH | `web/ApiExceptionHandlerTest` |
| UT | `ui/tests/validation.test.ts` (Vitest) |
| E2E | `ui/e2e/ui_smoke.py` (Playwright, run against the stack, not part of `mvn verify`) |
| SM | `smoke.sh` |

Code abbreviations: `VC` = `web/VocabularyController`, `EC` = `web/ExternalVocabularyController`, `CC` =
`web/ContentController`, `VSvc` = `service/VocabularyService`, `ESvc` = `service/ExternalVocabularyService`, UI views in
`ui/src/views/`.

## Stories

| Story | AC | Code | Tests |
|---|---|---|---|
| SIMPL-18161 View all vocabularies (UI) | AC1 list with actions menu, name, description, latest version, status, date created, last updated, creator | `VocabularyList.vue` | E2E (row content) |
| | AC2 load error: informed, error detail shown | `VocabularyList.vue` (`LIST_NOT_LOADED` + detail) | E2E (stubbed 500) |
| SIMPL-18162 Metadata of all vocabularies (API) | AC1 200, paged list with metadata | `VC.list`, `CollectionPage` | IT mainFlow, SM |
| | AC2 wrong paging parameter 400 | `Paging` | WH pagingDefaultsAndLimits, IT queryAndPathRules |
| | AC3 no role 403 | `AuthenticationFilter` | IT accessRules, SM |
| | AC4 internal error 500 with detail | `ApiExceptionHandler` | `web/ApiExceptionHandlerTest` (AH) |
| SIMPL-18163 Sort and filter (UI) | AC1 filter by status, name contains | `VocabularyList.vue` | E2E (name filter, empty state) |
| | AC2 sortable column headers asc/desc | `VocabularyList.vue` `sortBy` (D50) | E2E (aria-sort descending after first click on Name) |
| SIMPL-18164 Filters and sorting (API) | AC1 filter and sort on name, status, creationDate, lastUpdated | `VC.list`, `VC.vocabularyOrder`, `DateFilter` | WH vocabularyOrder, dateFilters; IT statusChanges, queryAndPathRules |
| | AC2 no match gives empty result, 200 | `VC.list` | IT queryAndPathRules (page beyond), E2E empty state |
| | AC3 paging with sorting | `CollectionPage.of` | WH pageLinks |
| | AC4 unsupported filter or sort 400 | `RequestInspector.queryIssues`, `VC.sortValue` | IT queryAndPathRules |
| SIMPL-18165 Download latest version (UI) | AC1 download newest version, review details | `VocabularyList.vue` (Download vocabulary), `VocabularyDetails.vue` | E2E (download), IT mainFlow (content) |
| SIMPL-18166 Full details of a version (API) | AC1 machine-readable metadata | `VC.version`, `CC.version` | IT mainFlow |
| | AC2 unknown version: not found | `VSvc.versionNotFound` | VS unknownVersionIsNotFoundWithFixedText, IT statusChanges |
| SIMPL-18167 Version history (UI) | AC1 details page with versions: version, status, date created, description, change log, download | `VocabularyDetails.vue` | E2E |
| SIMPL-18168 Version history (API) | AC1 list of all versions | `VC.versions` | IT mainFlow |
| | AC2 unknown vocabulary: not found | `VSvc.listVersions` | IT queryAndPathRules |
| SIMPL-18169 Create vocabulary (UI) | AC1 form with Name, Description, file; no version or change log field | `VocabularyForm.vue` (mode create) | E2E |
| | AC2 validation of required fields, formats, unique name, file format | `ui/src/lib/validation.ts`, server 400/409/415 | UT form rules, E2E report |
| SIMPL-18170 Create vocabulary (API) | AC1 created, version 1 ACTIVE, vocabulary ACTIVE | `VC.create`, `VSvc.createVocabulary` | VS creationStores…, IT mainFlow, SM |
| | AC2 retrieve shows metadata and version 1 ACTIVE; status can change later | `VC.get`, `VC.updateStatus` | IT mainFlow, statusChanges |
| | AC3 invalid or incomplete data: 400, nothing created | `FormFields`, `ContentValidator` | IT fieldRules, VS invalidContentStoresNothing |
| SIMPL-18172 New version (UI) | AC1 Update opens pre-filled form | `NewVersion.vue`, `VocabularyForm.vue` (D33) | E2E (current content label, next version line) |
| | AC2 submit creates version, ACTIVE, older DEPRECATED, notification | `VocabularyForm.vue`, `VSvc.createVersion` | E2E, VS newVersionDeprecates… |
| | AC3 unexpected error: nothing created, error notice, advice to retry or contact support | `ui/src/lib/errors.ts` (`GENERIC_ERROR`) | E2E (stubbed 500) |
| SIMPL-18173 New version (API) | AC1 next number, ACTIVE, older DEPRECATED, details returned | `VSvc.createVersion`, `FusekiVocabularyStore.createVersion` | IT mainFlow, VS, SM |
| | AC2 invalid data rejected with feedback | `FormFields`, `ContentValidator` | IT orderOfChecksOnWrites, validationReport |
| | AC3 unknown vocabulary rejected (404) | `VSvc.getVocabulary` | VS versionOfUnknownVocabularyIsNotFound, IT orderOfChecksOnWrites |
| | AC4 not authorised 403 | `AuthenticationFilter` | IT accessRules, SM |
| | AC5 not authenticated 401 | `AuthenticationFilter` | IT accessRules, SM |
| | AC6 unsupported format 415 | `RequestInspector.requireMultipart` | IT orderOfChecksOnWrites |
| SIMPL-18174 Status change (UI) | AC1 status-change action, updated status shown, notification | `VocabularyList.vue`, `VocabularyDetails.vue` (`changeStatus`) | E2E (Deprecate, message, banner) |
| | AC2 vocabulary no longer available: not found, no change | same, 404 branch | E2E (stubbed 404) |
| | AC3 update fails: could not be updated, status unchanged | same, `STATUS_NOT_UPDATED` | E2E (stubbed 500) |
| SIMPL-18175 Status change (API) | AC1 status updated, metadata returned | `VC.updateStatus`, `VSvc.updateVocabularyStatus` | IT statusChanges |
| | AC2 unknown vocabulary 404 | `VSvc.getVocabulary` | IT statusChanges |
| | AC3 unsupported status 400, unchanged | `VC.statusFromBody` | IT statusChanges |
| SIMPL-18176 Deprecated versions (UI) | AC1 any role may view versions | `AccessRules.READ` | IT accessRules, E2E (viewer) |
| | AC2 deprecated versions listed and labelled | `VocabularyDetails.vue` | E2E |
| | AC3 version metadata: number, date, creator, change log | `VocabularyDetails.vue` panel | E2E (creator in panel) |
| SIMPL-18178 Warning on deprecated vocabularies (UI) | AC1 warning on a deprecated vocabulary | `VocabularyList.vue`, `VocabularyDetails.vue` (D36) | E2E |
| SIMPL-18180 Syntax and structure validation (UI) | AC1 check on upload | `VocabularyForm.vue onFile`, `lib/validation.ts checkTurtle` | UT, E2E |
| | AC2, AC3 validation on submit (create, version) | server, `ContentValidator` | IT validationReport, E2E |
| | AC4 success message, proceed | `SYNTAX_PASSED` | UT, E2E |
| | AC5 syntax errors with location, flow blocked | `checkTurtle` (line), submit disabled | UT, E2E |
| | AC6 structure (semantic) errors with location | report panel | E2E (S4 report) |
| | AC7 all errors in one response | `SemanticRules` | CV allSemanticRulesRun… |
| | AC8 cannot continue until fixed | submit disabled / 400 keeps form open | E2E |
| SIMPL-18181 Syntax validation (API) | AC1 valid syntax passes to semantic validation | `ContentValidator.validate` | CV validContentPasses… |
| | AC2 invalid syntax: rejected, no semantic step, nothing stored, located errors | `TurtleParser` | CV syntaxError…, VS invalidContentStoresNothing, IT validationReport |
| SIMPL-18182 Semantic validation (UI) | AC1 after syntax, on submit | server | IT validationReport |
| | AC2 success | 201 notification | E2E |
| | AC3 structured report | report panel | E2E |
| SIMPL-18183 Semantic validation (API) | AC1 passes and creates | `ContentValidator` | CV, IT mainFlow |
| | AC2 errors returned, nothing created; external references warn; all errors reported | `SemanticRules`, `ReferenceChecker` | CV, IT validationReport |
| SIMPL-18184 Deprecated references (UI) | AC1 all ACTIVE: no warning | `ReferenceChecker` | CV activeReferenceGivesNoWarning |
| | AC2 deprecated: warning with recommendation (D11 makes it a warning, not a block) | `ReferenceChecker.deprecatedText` | CV deprecated…, IT deprecatedReferenceWarning |
| | AC3 missing referenced version: error | S7 | CV missing…IsS7 |
| | AC4 UI shows the warning with recommendation | details page warnings panel (D14) | E2E (warnings panel shows the external warning; the deprecated case uses the same panel) |
| SIMPL-18186 Automatic publication (UI) | AC1 stored as ACTIVE | `VSvc.createVocabulary`, `createVersion` | VS, IT |
| SIMPL-18187 Publish via API | AC1 vocabulary persisted, identifier returned | `VC.create` Location and href | IT mainFlow |
| | AC2 version persisted, linked, identifier returned | `VC.createVersion` | IT mainFlow |
| SIMPL-18189 Automatic versioning (UI) | AC1 next number assigned | server | E2E |
| | AC2 no manual version input | `VocabularyForm.vue` has no version field | E2E |
| | AC3 confirmation shows number | `versionCreated` | UT, E2E |
| SIMPL-18190 Automatic versioning (API) | AC1 next number in response | `VSvc.createVersion` | IT mainFlow |
| | AC2 manual version number rejected | `FormFields` (`version` field) | IT orderOfChecksOnWrites |
| SIMPL-18191 Automatic deprecation (UI) | AC1 new ACTIVE, all older DEPRECATED | `FusekiVocabularyStore.createVersion` | IT parallelVersionCreations…, E2E |
| SIMPL-18193 Validation report (UI) | AC1 success confirmed | notification | E2E |
| | AC2 report with nature and location, flow blocked | report panel | E2E |
| SIMPL-18194 Validation report (API) | AC1 structured report with all errors and locations | `ApiExceptionHandler.validation` | IT validationReport, CV |
| | AC2 success without error details | 201 body | IT mainFlow |
| SIMPL-18195 RBAC (UI) | AC1 admin sees create, update, status change | `isAdmin()` in views | E2E |
| | AC2 no role: hidden, authorization message | views, `NOT_AUTHORIZED` | E2E (viewer) |
| | AC3 not authenticated: login prompt | `lib/auth.ts` (`login-required`) | E2E (login flow) |
| SIMPL-18196 RBAC (API) | AC1 role holder processed | `AuthenticationFilter`, `AccessRules` | IT accessRules |
| | AC2 no role 403 | same | IT accessRules, SM |
| | AC3 not authenticated 401 | same | IT accessRules, SM |
| | AC4 every endpoint needs token and role | `AccessRules.required` | TV accessRules |
| SIMPL-18452 Download any version (UI) | AC1 select a version | `VocabularyDetails.vue` (`?version=`) | E2E |
| | AC2 download it | `download()` | E2E (`<Name>-v1.ttl`) |
| SIMPL-18559 Deprecated version warning (UI) | AC1 warning plus link to latest ACTIVE version | `VocabularyDetails.vue` | E2E |
| | AC2 warning only when no ACTIVE version | `recommended()` returns null | E2E (no active version) |

Out of scope (D1, D61): SIMPL-28680 (Schema Manager); SIMPL-393 is met by the generic upload (no seed data); SIMPL-395
is replaced by the SIMPL-181xx stories.

## Decisions

| Decision | Code | Tests |
|---|---|---|
| D1 scope | only the operations of the OpenAPI, no seed data | n/a |
| D2 CDM snapshot | glossary terms in code | n/a |
| D3 two statuses, effectively deprecated | `Status`, `VSvc`, `ReferenceChecker`, `VocabularyDetails.vue` | VS newVersionDeprecates…, CV deprecatedVocabularyMakes… |
| D4 version transitions, 409 same status | `VSvc.updateVersionStatus`, `updateVocabularyStatus`, `FusekiVocabularyStore.createVersion` | VS sameStatusIsConflict, IT statusChanges |
| D5 latest version, latestVersionHref | `Representations.version`, `created` | IT mainFlow |
| D6 publication | `VSvc` stores ACTIVE | VS |
| D7 version numbers, version field 400 | `VSvc.createVersion`, `FormFields` | IT orderOfChecksOnWrites |
| D8 nothing stored on failure | one SPARQL update per write | VS invalidContentStoresNothing, identicalVersion… |
| D9 validation steps and rules | `TurtleParser`, `SemanticRules`, `ReferenceChecker` | CV |
| D10 errors and warnings | `ValidationError`, `ValidationWarning`, `CreatedVersionBody` | CV, IT |
| D11 deprecated reference warning | `ReferenceChecker` | CV, IT deprecatedReferenceWarning |
| D12 references | `ReferenceChecker` | CV |
| D13 browser syntax check | `ui/src/lib/validation.ts` | UT, E2E |
| D14 report and success feedback | `VocabularyForm.vue`, `VocabularyDetails.vue` | E2E |
| D15 format, size, media type | `RequestInspector`, `ApiException.payloadTooLarge` | IT orderOfChecksOnWrites |
| D16 roles | `AccessRules` | TV, IT accessRules |
| D17 401 and 403 in the service | `AuthenticationFilter`, `TokenValidator` | TV, IT accessRules |
| D18 restricted UI actions | views, `NOT_AUTHORIZED` | E2E |
| D19 name filter | `VC.list` | IT mainFlow (lower-case contains) |
| D20 date filters | `DateFilter` | WH dateFilters |
| D21 unknown query parameters | `RequestInspector.queryIssues` | IT queryAndPathRules |
| D22 paging | `Paging`, `CollectionPage` | WH |
| D23 sorting | `VC.vocabularyOrder`, `versionOrder`, `VocabularyList.vue` | WH |
| D24 status codes | `ApiException` | IT |
| D25 vocabulary object | `Representations.vocabulary` | IT mainFlow |
| D26 version object | `Representations.version` | IT mainFlow |
| D27 creator | `TokenValidator.toUser` | TV creatorFallsBack…, IT mainFlow |
| D28 description per version | store model | VS newVersionDeprecates… (latest description) |
| D29 content and download | `CC`, `TurtleResponses` | IT mainFlow, SM |
| D30 external identity | `ESvc.register` | VS externalRegistration…, IT externalVocabularies |
| D31 one term per concept | names in code and UI | n/a |
| D32 changelog 'Initial version.' from UI | `VocabularyForm.vue` | E2E |
| D33 new version form | `VocabularyForm.vue` | E2E |
| D34 list paging and empty state | `VocabularyList.vue` | E2E |
| D35 details and history page | `VocabularyDetails.vue`, `getAllVersions` | E2E |
| D36 deprecation warnings | `VocabularyList.vue`, `VocabularyDetails.vue` | E2E |
| D37 field rules | `FormFields`, `VocabularyNames`, `lib/validation.ts` | WH formFields, IT fieldRules, UT |
| D38 search operations | `VC.search`, `EC.search`, `VSvc.search`, `ESvc.search` | VS searchKeepsOrder…, IT searchRules, externalVocabularies |
| D39 OpenAPI catch-up | `docs/API.md` | n/a |
| D40 security scheme | Keycloak realm, `TokenValidator` | TV |
| D41 external operations, version status change | `EC`, `ESvc`, `VC.updateVersionStatus` | IT externalVocabularies, statusChanges |
| D42 external vocabularies outside the process | `ESvc.delete` without reference check | IT externalVocabularies |
| D43 serialised changes | `NameLocks` | VS parallelVersionCreations…, IT parallelVersionCreations… |
| D44 store and identity provider down | `SparqlClient`, `RealmKeys`, `HealthController` | SD, TV unknownKeyWithIdentityProviderDown…, cachedKeysKeepWorking… |
| D45 pull only | no events | n/a |
| D46 identical version 409 | `VSvc.createVersion`, next version line in UI | VS identicalVersion…, E2E |
| D47 texts of references | `ReferenceChecker` | CV, DeprecatedTextTest |
| D48 problem types | `ApiException` | IT accessRules, SD |
| D49 invalid path values 400 | `PathValues` | IT queryAndPathRules |
| D50 sort click | `VocabularyList.vue sortBy` | E2E |
| D51 order of checks | controllers, `VSvc` | IT orderOfChecksOnWrites |
| D52 unknown fields and parameters | `FormFields`, `RequestInspector`, JSON checks | IT fieldRules, searchRules, statusChanges |
| D53 untagged labels | `SemanticRules.duplicatePrefLabels` | CV untaggedLabels… |
| D54 exact names in paths | store lookup by exact IRI | IT mainFlow (upper-case path 404), VS duplicateName… |
| D55 fixed problem texts, 404 and 405 bodies | `VSvc.versionNotFound`, `ESvc` conflict text, `ApiExceptionHandler` | VS, IT queryAndPathRules |
| D56 external registration details | `EC.register`, `EC.aliases` | IT externalVocabularies |
| D57 base IRI | `VSvc.versionContentUrl` passed to the parser | CV ownReferencesAndRelativePrefixes… |
| D58 UTC seconds, code point order | `VSvc.now`, comparators | VS creationStores…, WH vocabularyOrder… |
| D59 prev link beyond last page | `CollectionPage.of` | WH pageLinks |
| D60 UI texts and steps | `ui/src/lib/messages.ts`, views | UT, E2E |
| D61 SIMPL-28680 out | not built | n/a |
| E1 identity provider and users | `keycloak/simpl-realm.json` | SM |
| E2 ports and URLs | `application.yml`, `compose.yaml` | SM |
| E3 Fuseki store | `fuseki/Dockerfile`, `FusekiVocabularyStore` | IT (embedded Fuseki), SM |
| E4 environment from the build profile | `.env`, `compose.yaml`, `/config.json` | SM |
