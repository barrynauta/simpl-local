# Traceability: Vocabulary Manager, run 7

Every in-scope story, acceptance criterion and decision, mapped to the code and to my own tests.
The stories number their criteria inconsistently; AC ids below follow the order of the scenarios in each story file (AC1 is the first scenario).

## Abbreviations

Backend paths are below `backend/src/main/java/eu/simpl/vocabulary/`. Test paths are below `backend/src/test/java/eu/simpl/vocabulary/`.

| Code | File | Test | File |
|---|---|---|---|
| RT | `api/RouteTable.java` | VIT | `api/VocabularyApiIT.java` |
| EF | `api/filter/EntryFilter.java` | XIT | `api/ExternalVocabularyApiIT.java` |
| GF | `api/filter/RequestGateFilter.java` | SDIT | `api/StoreDownIT.java` |
| VC | `api/controller/VocabularyController.java` | TVT | `domain/validation/TurtleValidatorTest.java` |
| SC | `api/controller/SearchController.java` | RAT | `domain/validation/ReferenceAnalyzerTest.java` |
| XC | `api/controller/ExternalVocabularyController.java` | RRT | `api/request/RequestRulesTest.java` |
| HC | `api/controller/HealthController.java` | FRT | `api/filter/FilterRulesTest.java` |
| AM | `api/ApiMapper.java` | SRT | `service/ServiceRulesTest.java` |
| PR | `api/problem/Problems.java` | SUT | `security/SecurityUnitTest.java` |
| MF | `api/request/MultipartForm.java` | UIC | `ui/e2e/ui_check.py` (Playwright + axe) |
| FR, QR | `api/request/FieldRules.java`, `QueryRules.java` | UIT | `ui/tests/rules.test.ts` (Vitest) |
| VS | `service/VocabularyService.java` | SMK | `smoke.sh` |
| XS, SS | `service/ExternalVocabularyService.java`, `SearchService.java` | BRT | `ops/backup-restore-test.sh` |
| NL | `service/NameLocks.java` | LOAD | `ops/load-check.py` |
| CV | `service/ContentValidation.java` | | |
| TV, RA | `domain/validation/TurtleValidator.java`, `ReferenceAnalyzer.java` | | |
| FS, SPC | `store/FusekiVocabularyStore.java`, `store/SparqlClient.java` | | |
| SEC, RKS | `security/SecurityConfig.java`, `security/RealmKeySource.java` | | |
| VL, VD, VF | `ui/src/views/VocabularyList.vue`, `VocabularyDetails.vue`, `VocabularyForm.vue` | | |

## Stories and acceptance criteria

| Story | AC | What | Code | Own tests |
|---|---|---|---|---|
| SIMPL-18161 | AC1 | List with actions menu, name, description, latest version, status, dates, creator | VL, AM, VC.list | UIC (list, axe), VIT.listFiltersSortsAndPages |
| SIMPL-18161 | AC2 | List load error message and detail (D34) | VL `TEXT.listError` | not automated (needs an API stub; D24) |
| SIMPL-18162 | AC1 | Paginated list with metadata, 200 | VC.list, AM.page | VIT.listFiltersSortsAndPages, SMK |
| SIMPL-18162 | AC2 | Wrong pagination gives 400 with detail | QR.page/pageSize | VIT, RRT.pagingRules |
| SIMPL-18162 | AC3 | No role gives 403 | GF | VIT.accessRules |
| SIMPL-18162 | AC4 | Internal error gives 500 problem | ApiExceptionHandler, EF | covered by handler design; StoreDownIT covers 503 |
| SIMPL-18163 | AC1 | UI filters (name contains, status, created on) | VL | UIC (name filter) |
| SIMPL-18163 | AC2 | Sortable columns, asc and desc (D23, D50) | VL.sortBy | UIC (aria-sort) |
| SIMPL-18164 | AC1 | Filter and sort on name, status, creationDate, lastUpdated | VS.listVocabularies, QR | VIT, SRT.vocabularyOrder, SRT.dateFilters |
| SIMPL-18164 | AC2 | No match gives empty 200 | VS, Page | VIT (creationDate=2001-01-01) |
| SIMPL-18164 | AC3 | Paging keeps sort | AM.page, AM.link | VIT (prev link keeps sort) |
| SIMPL-18164 | AC4 | Unsupported filter, sort or value gives 400 | QR, QueryParameters | VIT, RRT |
| SIMPL-18165 | AC1 | Download latest from the list, view full details | VL 'Download vocabulary', VD | UIC (details), VIT.downloadsReturnStoredBytes |
| SIMPL-18166 | AC1 | Version details as JSON | VC.version | VIT.downloadsReturnStoredBytes |
| SIMPL-18166 | AC2 | Unknown version gives 404 | VS.getVersion, PR.versionNotFound | VIT |
| SIMPL-18167 | AC1 | History on the details page with all columns | VD | UIC (rows, columns) |
| SIMPL-18168 | AC1 | Version history list | VC.versions | VIT.newVersionDeprecatesOlderActiveVersions |
| SIMPL-18168 | AC2 | Unknown vocabulary gives 404 | VS.listVersions | VIT.listFiltersSortsAndPages |
| SIMPL-18169 | AC1 | 'New Vocabulary +' form: Name, Description, file; no version or change log | VF (create) | UIC |
| SIMPL-18169 | AC2 | Required fields, formats, unique name, supported format, messages | VF.validate, VC.create, VS (409) | UIC, UIT, VIT.requestShapeErrorsAreReportedTogether, VIT.duplicateNameIsConflictEvenWithInvalidContent |
| SIMPL-18170 | AC1 | Create returns identifier, metadata, version 1 ACTIVE, vocabulary ACTIVE | VC.create, VS.createVocabulary | VIT.createsVocabularyWithVersionOneActive, SMK |
| SIMPL-18170 | AC2 | Retrieve shows metadata; status can change later | VC.get, VC.updateStatus | VIT |
| SIMPL-18170 | AC3 | Invalid data gives 400, nothing created | VC.create, D8 | VIT |
| SIMPL-18172 | AC1 | 'Update' opens pre-filled form | VF (version), VL, VD | UIC |
| SIMPL-18172 | AC2 | New version ACTIVE, older DEPRECATED, notification | VS.createVersion, FS.createVersion, VD flash | UIC, VIT |
| SIMPL-18172 | AC3 | Unexpected error: nothing created, error with advice | VF.handleError (D60 text) | not automated (needs a 500 stub; D24) |
| SIMPL-18173 | AC1 | New version via API, next number, ACTIVE, older DEPRECATED | VC.createVersion, VS | VIT, SMK |
| SIMPL-18173 | AC2-AC6 | Invalid data 400, unknown vocabulary 404, no role 403, no token 401, unsupported format 415 | VC, GF, SEC | VIT.newVersionDeprecatesOlderActiveVersions, VIT.accessRules |
| SIMPL-18174 | AC1 | Status change in UI with notification | VL.changeStatus, VD.changeStatus | UIC (Deprecate, Activate) |
| SIMPL-18174 | AC2 | Vocabulary gone: 'Vocabulary not found. The status was not changed.' | VL, VD | not automated (needs 404 stub; D24) |
| SIMPL-18174 | AC3 | Update fails: D72 text, status unchanged | VL, VD | not automated (needs 500 stub) |
| SIMPL-18175 | AC1 | PATCH status returns vocabulary | VC.updateStatus, VS | VIT.statusChangesAndConflicts |
| SIMPL-18175 | AC2 | Unknown vocabulary 404 | VS | VIT |
| SIMPL-18175 | AC3 | Other value 400, unchanged | Requests.statusBody | VIT |
| SIMPL-18176 | AC1 | Viewer may read history | GF (READ), VD | VIT (viewer reads), UIC (viewer) |
| SIMPL-18176 | AC2 | Deprecated versions listed and labelled | VD | UIC |
| SIMPL-18176 | AC3 | Version panel with metadata incl. creator and change log | VD panel | UIC (region 'Version 1') |
| SIMPL-18178 | AC1 | Warning on deprecated vocabularies (icon name and tooltip, banner) | StatusCell.vue, VD banner | UIC (img name) |
| SIMPL-18180 | AC1 | Check on upload (browser, syntax only, D13) | VF.onFile, rules.ts checkTurtle | UIC, UIT |
| SIMPL-18180 | AC2, AC3 | Server validation on submit (create, version) | CV, TV | VIT |
| SIMPL-18180 | AC4 | Success message 'Syntax check passed.' | VF | UIC |
| SIMPL-18180 | AC5-AC7 | Errors with location; all semantic errors in one report | TV, VF report | TVT.allRulesRunAndReportEveryError, UIC |
| SIMPL-18180 | AC8 | Cannot proceed with errors | VF submitDisabled, server 400 | UIC (submit disabled) |
| SIMPL-18181 | AC1 | Valid syntax passes to semantic step | CV.validateInternal | VIT, TVT |
| SIMPL-18181 | AC2 | Syntax error: rejected, no semantic step, nothing stored, structured error with location | TV.parse, CV | VIT.syntaxErrorStopsBeforeSemanticValidation, TVT |
| SIMPL-18182 | AC1-AC3 | Semantic validation after syntax on UI submit, report | CV, VF report region | UIC, VIT |
| SIMPL-18183 | AC1 | Semantic pass creates | CV, VS | VIT |
| SIMPL-18183 | AC2 | Semantic errors returned, nothing created | TV.semanticErrors, RA | TVT, RAT, VIT.referencesGiveErrorsAndWarnings |
| SIMPL-18184 | AC1 | All referenced versions ACTIVE: no warning | RA.check | RAT |
| SIMPL-18184 | AC2 | Deprecated reference: warning, version created (D11 overrides the 'not created' text) | RA.warnIfDeprecated | RAT, VIT |
| SIMPL-18184 | AC3 | Missing referenced version: error, not created | RA.checkInternal (S7) | RAT, VIT |
| SIMPL-18184 | AC4 | UI shows the deprecated-version warning after submit (D14) | VD warnings region | UIC (Validation warnings region) |
| SIMPL-18186 | AC1 | Stored and ACTIVE after validation | VS | VIT |
| SIMPL-18187 | AC1, AC2 | Persisted, linked, Location and href as identifier | VC.created, FS | VIT.createsVocabularyWithVersionOneActive |
| SIMPL-18189 | AC1-AC3 | UI assigns version automatically, shows 'Version n of Name was created.' | VF, VD | UIC |
| SIMPL-18190 | AC1 | Next number assigned, returned | VS.createVersion | VIT |
| SIMPL-18190 | AC2 | 'version' field gives 400, nothing created | VC.createVersion | VIT |
| SIMPL-18191 | AC1 | New version ACTIVE, previous DEPRECATED | FS.createVersion | VIT.parallelVersionsAreSerialised |
| SIMPL-18193 | AC1 | UI confirms success | VD notification | UIC |
| SIMPL-18193 | AC2 | UI shows structured report, flow blocked | VF 'Validation report' region | UIC (syntax), manual for server report |
| SIMPL-18194 | AC1 | 400 report lists all errors with detail and location | CV, TV, RA | VIT, TVT |
| SIMPL-18194 | AC2 | 201 confirms without error details | VC.created | VIT |
| SIMPL-18195 | AC1 | Admin sees create, update, status actions | VL, VD | UIC |
| SIMPL-18195 | AC2 | Without role: actions hidden; direct URL shows authorization message | VL, VF | UIC (viewer) |
| SIMPL-18195 | AC3 | No token: Keycloak login | auth.ts (`login-required`) | UIC (login flow) |
| SIMPL-18196 | AC1-AC4 | Bearer token and role on every operation; 403; 401 | SEC, GF, RT | VIT.accessRules, SUT, SMK |
| SIMPL-18452 | AC1 | Select a version for review | VD panel | UIC |
| SIMPL-18452 | AC2 | Download the selected version | VD download | UIC (filename '<Name>-v1.ttl') |
| SIMPL-18559 | AC1 | Deprecated version warning with 'Latest ACTIVE version: n' link | VD.activeLink | UIC |
| SIMPL-18559 | AC2 | Warning without link when no ACTIVE version | VD.activeLink | code review only |

Out of scope (D1, D61): SIMPL-28680 (Schema Manager), SIMPL-395 (replaced by the 181xx stories). SIMPL-393 is met by the generic upload: no seed data; a CPSV-like Turtle file can be uploaded like any other.

## Decisions

| Id | Where | Own tests |
|---|---|---|
| D1, D61 | Scope: only the 181xx stories, 18452, 18559; no seed data | n/a |
| D2 | CDM snapshot used for names | n/a |
| D3 | Separate vocabulary and version status (FS, VS); effectively deprecated in RA and VD | VIT.referencesGiveErrorsAndWarnings, UIC |
| D4 | Version PATCH (VC), creation deprecates every ACTIVE version (FS.createVersion), 409 texts (VS) | VIT.statusChangesAndConflicts |
| D5 | Latest version = highest number; latestVersionHref always filled (AM) | VIT.newVersionDeprecatesOlderActiveVersions |
| D6 | ACTIVE on creation; UI labels Active, Deprecated (format.ts) | VIT, UIT |
| D7 | Integers from 1; 'version' field 400 | VIT |
| D8 | One store request per write (FS); validation before any write | VIT.requestShapeErrorsAreReportedTogether, SDIT |
| D9, D53, D65 | TV: syntax then S1 to S6; RA: S7; location texts | TVT, RAT, VIT |
| D10 | Closed warning codes (ValidationWarning); validationWarnings always present | VIT |
| D11, D47 | RA.warnIfDeprecated texts | RAT, VIT |
| D12, D69 | RA.classify: internal shapes, W3C, own namespace, self ignored | RAT |
| D13, D60, D62, D76 | VF.onFile (N3.js), field errors, Submit disabled only on upload errors | UIC, UIT |
| D14, D72 | Report region on 400; notification and warnings on the details page | UIC |
| D15, D76 | 5 MB file part, 6 MB request (MF, GF), 415 (GF) | VIT.uploadLimits, VIT.accessRules |
| D16 | Realm roles per operation (RT, GF) | VIT.accessRules |
| D17 | 401 and 403 problem types (SEC, PR); auth before body checks | VIT.accessRules |
| D18 | Admin actions hidden; direct URL message (VF) | UIC |
| D19 | Name filter contains, case-insensitive, relaxed pattern (QR, VS) | VIT, RRT |
| D20, D58 | DateFilter: day in UTC or second | SRT.dateFilters, VIT |
| D21, D52 | Unknown, repeated, malformed query parameters; unknown fields (QueryParameters, MF, controllers) | RRT, VIT |
| D22, D59, D78 | Page limits, links, prev beyond last, links keep the query (AM.page) | VIT, SRT.pages |
| D23, D50 | Sort orders and ties (VS); UI column sort | SRT.vocabularyOrder, SRT.versionOrder, UIC |
| D24 | Status codes for generic criteria | VIT |
| D25, D26, D27 | Wire mapping (AM); createdBy from email or preferred_username (Roles) | VIT.creatorFallsBackToUsername |
| D28 | Description per version; list shows latest | VIT |
| D29, D70 | Bytes as uploaded, text/turtle without charset, filenames (Requests.turtle) | VIT.downloadsReturnStoredBytes |
| D30, D56, D73 | External identity, aliases, URIs stored as sent (XC, XS) | XIT |
| D31, D74, D75 | Glossary terms, routes, roles and labels in the UI | UIC |
| D32, D33 | Create sends 'Initial version.'; new version form pre-filled, sends current content | UIC |
| D34, D35 | Paging 20 rows; details loads versions 100 per page | VL, VD (`listAllVersions`) |
| D36, D64 | Deprecation warnings, link rules | UIC |
| D37 | Field rules (FR, rules.ts) | RRT.fieldRules, UIT |
| D38, D68 | Searches unpaged, limits, order, issue names (SC, SS) | VIT.searchResolvesReferencesInOrder, XIT |
| D39, D79 | Decided contract changes built (see DEVIATIONS.md) | fuzz run, VIT |
| D40, E5 | OIDC through Keycloak; audience and issuer checked (SEC) | VIT.accessRules |
| D41, D42 | External operations, delete without reference check | XIT |
| D43, D71, D81 | Per-name lock (NL) with 30 s wait, one replica | VIT.parallelVersionsAreSerialised, SRT.locksSerialiseAndTimeOut |
| D44 | Timeouts and retries (SPC), 503 body and Retry-After (PR), key cache (RKS), health (HC) | SDIT, SUT, VIT.unknownSigningKeyWithIdentityProviderDownFailsClosed |
| D45 | No events; pull only | n/a |
| D46 | Identical-version 409 by SHA-256 of bytes and trimmed description (VS) | VIT, UIC |
| D48 | belgif types for 400, 404, 500, 503; simpl types for 401, 403, 406, 409, 413, 415 (PR) | VIT |
| D49 | Invalid path values 400 (Requests.checkPathName, checkPathVersion) | VIT |
| D51, D63 | Order: 405, firewall, 401, 403, 406, 415, 413, 400, 404, 409, validation (EF, GF, controllers, services) | VIT.duplicateNameIsConflictEvenWithInvalidContent, FRT |
| D54 | Exact names in paths and search | VIT.createsVocabularyWithVersionOneActive |
| D55, D66 | Fixed not-found and conflict texts (PR, XS) | VIT, XIT |
| D57 | Base IRI of a new version (CV.baseIri) | TVT.prefixesAreCollectedAndResolvedAgainstTheBase |
| D67, D77 | 405 before auth, firewall 400, fixed segments win (RT, EF); actuator on management port only | VIT.routingRules, FRT.routeTable |
| D80 | Logs carry the token subject only (Roles.userId) | code review |
| E1 | Realm, clients, users (`keycloak/realm-simpl.json`) | SMK, UIC |
| E2, E4 | Ports and URLs from environment (`application.yaml`, `compose.yaml`, nginx config.json) | SMK |
| E3 | Fuseki dataset 'vocabulary' (`fuseki/Dockerfile`) | VIT (embedded Fuseki), SMK |
| E6 | Reuse choices: see REUSE.md | n/a |
| Q1 to Q6 | See QUALITY.md | LOAD, BRT, UIC (axe) |

## Not implemented or not tested automatically

- UI error states that need an API stub (SIMPL-18161 AC2, 18172 AC3, 18174 AC2 and AC3): implemented with the decided texts, not covered by my own UI check because it runs against the real stack without stubs.
- SIMPL-18559 AC2 (no ACTIVE version at all): implemented, checked by code review only.
