# Coverage

Generated from the feature tags on 17 Nov 2026 (`python tools/tag_report.py` lists the scenarios per tag).
Counts are scenario blocks; a Scenario Outline counts once here and once per example row in behave.

## Acceptance criteria

| Criterion | Story | Scenarios |
|---|---|---|
| SIMPL-18161/AC1 | View All vocabularies and their definitions via UI | ui_list: The list shows every vocabulary with its columns and row actions (UI) |
| SIMPL-18161/AC2 | View All vocabularies and their definitions via UI | ui_list: A load error is reported with the problem detail (UI) |
| SIMPL-18162/AC1 | Retrieve metadata of all vocabularies via API | list_vocabularies: The list is paged and every vocabulary carries its metadata<br>list_vocabularies: A viewer gets the same list |
| SIMPL-18162/AC2 | Retrieve metadata of all vocabularies via API | list_vocabularies: Wrong pagination parameters are rejected with 400 (<query>) |
| SIMPL-18162/AC3 | Retrieve metadata of all vocabularies via API | access_control: A user with no vocabulary role gets 403 on <operation> |
| SIMPL-18162/AC4 | Retrieve metadata of all vocabularies via API | none (see below) |
| SIMPL-18163/AC1 | Sort and filter vocabulary list via UI | list_vocabularies: The name filter is a case-insensitive contains match<br>ui_list: Filters narrow the list (UI) |
| SIMPL-18163/AC2 | Sort and filter vocabulary list via UI | ui_list: A sortable column sorts ascending, then descending (<column>) (UI) |
| SIMPL-18164/AC1 | Apply filters and sorting via vocabulary list API | list_vocabularies: The default order is name ascending<br>list_vocabularies: The list sorts by <sort><br>list_vocabularies: Sorting by name descending gives the reverse order<br>list_vocabularies: The name filter is a case-insensitive contains match<br>list_vocabularies: The status filter uses the vocabulary status<br>list_vocabularies: A creation date filter with a date matches the whole UTC day<br>list_vocabularies: A creation date filter with a date-time matches to the second<br>list_vocabularies: A last update filter with a date matches the whole UTC day |
| SIMPL-18164/AC2 | Apply filters and sorting via vocabulary list API | list_vocabularies: An empty result has no last link<br>list_vocabularies: Filters that match nothing return an empty list, not an error |
| SIMPL-18164/AC3 | Apply filters and sorting via vocabulary list API | list_vocabularies: Pagination is applied together with sorting |
| SIMPL-18164/AC4 | Apply filters and sorting via vocabulary list API | list_vocabularies: Wrong pagination parameters are rejected with 400 (<query>)<br>list_vocabularies: An invalid name filter is rejected (<case>)<br>list_vocabularies: An unsupported or invalid filter or sort value is rejected (<query>)<br>list_vocabularies: An unknown query parameter is rejected with one issue (<param>) |
| SIMPL-18165/AC1 | Download and view full details of the newest vocabulary version in the UI | ui_list: The download action gives the latest version (UI) |
| SIMPL-18166/AC1 | Retrieve full details of a vocabulary version via API | vocabulary_details: One version is returned with its full metadata<br>vocabulary_details: A version's content is downloaded as stored |
| SIMPL-18166/AC2 | Retrieve full details of a vocabulary version via API | vocabulary_details: A version that does not exist gives 404 (<case>) |
| SIMPL-18167/AC1 | View version history of a vocabulary in the UI | ui_details: Opening the history shows the details page with every version, newest first (UI) |
| SIMPL-18168/AC1 | Retrieve version history of a vocabulary via API | vocabulary_details: The version history lists every version, newest first, with its metadata<br>vocabulary_details: A viewer can read the version history |
| SIMPL-18168/AC2 | Retrieve version history of a vocabulary via API | vocabulary_details: The history of an unknown vocabulary gives 404 |
| SIMPL-18169/AC1 | Create a new vocabulary via UI | ui_create: The creation form has name, description and file, but no version or change log (UI) |
| SIMPL-18169/AC2 | Create a new vocabulary via UI | ui_create: The creation form reports invalid input (<case>) (UI) |
| SIMPL-18170/AC1 | Create a new vocabulary via API | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE |
| SIMPL-18170/AC2 | Create a new vocabulary via API | create_vocabulary: The created vocabulary can be read back with its metadata<br>create_vocabulary: The vocabulary status is ACTIVE by default and can be changed by hand both ways<br>status: A deprecated vocabulary is restored to ACTIVE |
| SIMPL-18170/AC3 | Create a new vocabulary via API | create_vocabulary: An invalid or incomplete create request is rejected: <case><br>create_vocabulary: A duplicate vocabulary name is rejected with 409<br>create_vocabulary: Vocabulary names are unique regardless of case<br>create_vocabulary: A file that is not Turtle is rejected as a syntax error |
| SIMPL-18172/AC1 | Create a new version of a vocabulary via UI | ui_create: The update form is pre-filled with the latest version (UI) |
| SIMPL-18172/AC2 | Create a new version of a vocabulary via UI | ui_create: Submitting the update form creates the next version and deprecates the older ones (UI) |
| SIMPL-18172/AC3 | Create a new version of a vocabulary via UI | ui_create: An unexpected error during version creation is reported (<code>) (UI) |
| SIMPL-18173/AC1 | Create a new version of a vocabulary via API | create_version: A new version gets the next number, is ACTIVE and deprecates the previous one<br>create_version: Every previous version is DEPRECATED after the third version |
| SIMPL-18173/AC2 | Create a new version of a vocabulary via API | create_version: An invalid or incomplete new version is rejected: <case> |
| SIMPL-18173/AC3 | Create a new version of a vocabulary via API | create_version: A new version of a vocabulary that does not exist is rejected with 404 |
| SIMPL-18173/AC4 | Create a new version of a vocabulary via API | access_control: A viewer gets 403 on the write operation <operation> and nothing changes |
| SIMPL-18173/AC5 | Create a new version of a vocabulary via API | create_version: A new version without authentication is rejected |
| SIMPL-18173/AC6 | Create a new version of a vocabulary via API | create_version: A new version in an unsupported format is rejected with 415<br>create_vocabulary: A create request that is not multipart/form-data is rejected with 415 (<ctype>) |
| SIMPL-18174/AC1 | Update vocabulary status via UI | ui_status: A vocabulary is deprecated from the list (UI)<br>ui_status: A deprecated vocabulary is restored to Active, the only status offered (UI) |
| SIMPL-18174/AC2 | Update vocabulary status via UI | ui_status: A vocabulary that is no longer available is reported as not found (UI) |
| SIMPL-18174/AC3 | Update vocabulary status via UI | ui_status: A failed status update leaves the status unchanged (UI) |
| SIMPL-18175/AC1 | Update vocabulary status via API | status: A vocabulary is deprecated and the updated vocabulary is returned<br>status: A deprecated vocabulary is restored to ACTIVE |
| SIMPL-18175/AC2 | Update vocabulary status via API | status: Changing the status of an unknown vocabulary gives 404 |
| SIMPL-18175/AC3 | Update vocabulary status via API | status: An unsupported status value is rejected and nothing changes (<value>) |
| SIMPL-18176/AC1 | Access deprecated vocabulary versions in UI | ui_details: A viewer sees deprecated versions labelled as such (UI) |
| SIMPL-18176/AC2 | Access deprecated vocabulary versions in UI | ui_details: A viewer sees deprecated versions labelled as such (UI) |
| SIMPL-18176/AC3 | Access deprecated vocabulary versions in UI | ui_details: Selecting a deprecated version shows all its metadata (UI) |
| SIMPL-18178/AC1 | Warning on deprecated vocabularies in Vocabulary Management UI | ui_list: A deprecated vocabulary carries a warning in the list and on its details page (UI) |
| SIMPL-18180/AC1 | Validate vocabulary syntax and structure via UI | ui_validation: A valid file passes the upload check (UI)<br>ui_validation: A syntax error is shown on upload with its position and blocks the submit (UI) |
| SIMPL-18180/AC2 | Validate vocabulary syntax and structure via UI | ui_validation: Semantic errors on creation are listed in the report panel (UI) |
| SIMPL-18180/AC3 | Validate vocabulary syntax and structure via UI | ui_validation: All semantic errors of a new version are listed together (UI) |
| SIMPL-18180/AC4 | Validate vocabulary syntax and structure via UI | ui_validation: A valid file passes the upload check (UI)<br>ui_validation: A semantically valid submission is confirmed (UI) |
| SIMPL-18180/AC5 | Validate vocabulary syntax and structure via UI | ui_validation: A syntax error is shown on upload with its position and blocks the submit (UI) |
| SIMPL-18180/AC6 | Validate vocabulary syntax and structure via UI | ui_validation: Semantic errors on creation are listed in the report panel (UI) |
| SIMPL-18180/AC7 | Validate vocabulary syntax and structure via UI | ui_validation: All semantic errors of a new version are listed together (UI) |
| SIMPL-18180/AC8 | Validate vocabulary syntax and structure via UI | ui_validation: A syntax error is shown on upload with its position and blocks the submit (UI)<br>ui_validation: The flow cannot complete until the errors are fixed (UI) |
| SIMPL-18181/AC1 | Validate vocabulary syntax via API | validation: Valid content passes both steps and the 201 is the success indicator<br>validation: A valid new version passes both steps |
| SIMPL-18181/AC2 | Validate vocabulary syntax via API | validation: A vocabulary with a Turtle syntax error is rejected with one located issue<br>validation: Semantic validation does not run after a syntax error<br>validation: A new version with a syntax error creates nothing and deprecates nothing |
| SIMPL-18182/AC1 | Validate vocabulary semantics via UI | ui_validation: Semantic errors on creation are listed in the report panel (UI) |
| SIMPL-18182/AC2 | Validate vocabulary semantics via UI | ui_validation: A semantically valid submission is confirmed (UI) |
| SIMPL-18182/AC3 | Validate vocabulary semantics via UI | ui_validation: Semantic errors on creation are listed in the report panel (UI)<br>ui_validation: All semantic errors of a new version are listed together (UI) |
| SIMPL-18183/AC1 | Validate vocabulary semantics via API | validation: Valid content passes both steps and the 201 is the success indicator<br>validation: A valid new version passes both steps<br>validation: An unregistered external namespace gives the warning externalReferenceSkipped |
| SIMPL-18183/AC2 | Validate vocabulary semantics via API | validation: Semantic rule <rule> blocks the creation<br>validation: A file without statements is rejected with location document<br>validation: All semantic errors are returned in one response<br>validation: A new version with semantic errors creates nothing and deprecates nothing<br>validation: A missing reference and a semantic error are reported together |
| SIMPL-18184/AC1 | Automatic check for deprecated vocabularies during new version creation in UI | validation: A reference to an ACTIVE internal version gives no warning<br>validation: A reference without a version resolves to the latest version |
| SIMPL-18184/AC2 | Automatic check for deprecated vocabularies during new version creation in UI | validation: A reference to a version replaced by a newer one gives a deprecation warning, not an error<br>validation: Each deprecated reference gives its own warning<br>validation: Without an ACTIVE version the deprecation warning has no recommendation<br>validation: A version of a DEPRECATED vocabulary counts as deprecated |
| SIMPL-18184/AC3 | Automatic check for deprecated vocabularies during new version creation in UI | validation: A reference to a version that does not exist is a validation error<br>validation: A reference to a vocabulary that does not exist is a validation error |
| SIMPL-18184/AC4 | Automatic check for deprecated vocabularies during new version creation in UI | ui_validation: A reference to a deprecated version shows a warning that names it and the ACTIVE version (UI) |
| SIMPL-18186/AC1 | Automatic publication of vocabularies UI | create_version: A new version gets the next number, is ACTIVE and deprecates the previous one<br>create_vocabulary: A valid vocabulary is created as version 1, ACTIVE<br>ui_create: A valid new vocabulary is published as ACTIVE version 1 (UI) |
| SIMPL-18187/AC1 | Publish vocabulary via API | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE<br>create_vocabulary: The created vocabulary can be read back with its metadata<br>create_vocabulary: The stored content is downloadable byte for byte |
| SIMPL-18187/AC2 | Publish vocabulary via API | create_version: A new version gets the next number, is ACTIVE and deprecates the previous one<br>create_version: The vocabulary shows the new latest version and its description, its status unchanged |
| SIMPL-18189/AC1 | Automatic versioning of vocabularies via UI | ui_create: Submitting the update form creates the next version and deprecates the older ones (UI) |
| SIMPL-18189/AC2 | Automatic versioning of vocabularies via UI | ui_create: Submitting the update form creates the next version and deprecates the older ones (UI) |
| SIMPL-18189/AC3 | Automatic versioning of vocabularies via UI | ui_create: A valid new vocabulary is published as ACTIVE version 1 (UI)<br>ui_create: Submitting the update form creates the next version and deprecates the older ones (UI) |
| SIMPL-18190/AC1 | Automatic versioning of vocabularies via API | create_version: A new version gets the next number, is ACTIVE and deprecates the previous one |
| SIMPL-18190/AC2 | Automatic versioning of vocabularies via API | create_version: A request that carries a version number is rejected and creates nothing |
| SIMPL-18191/AC1 | Automatic deprecation of previous vocabulary version UI | create_version: A new version gets the next number, is ACTIVE and deprecates the previous one<br>create_version: Every previous version is DEPRECATED after the third version<br>ui_create: Submitting the update form creates the next version and deprecates the older ones (UI) |
| SIMPL-18193/AC1 | Receive vocabulary validation report via UI | ui_validation: A valid file passes the upload check (UI)<br>ui_validation: A semantically valid submission is confirmed (UI) |
| SIMPL-18193/AC2 | Receive vocabulary validation report via UI | ui_validation: Semantic errors on creation are listed in the report panel (UI)<br>ui_validation: All semantic errors of a new version are listed together (UI)<br>ui_validation: The flow cannot complete until the errors are fixed (UI) |
| SIMPL-18194/AC1 | Receive vocabulary validation report via API | validation: A vocabulary with a Turtle syntax error is rejected with one located issue<br>validation: A new version with a syntax error creates nothing and deprecates nothing<br>validation: Semantic rule <rule> blocks the creation<br>validation: All semantic errors are returned in one response |
| SIMPL-18194/AC2 | Receive vocabulary validation report via API | create_version: A new version gets the next number, is ACTIVE and deprecates the previous one<br>create_vocabulary: A valid vocabulary is created as version 1, ACTIVE<br>validation: Valid content passes both steps and the 201 is the success indicator |
| SIMPL-18195/AC1 | Role-based access control for vocabulary management UI | ui_access: An administrator sees every action (UI) |
| SIMPL-18195/AC2 | Role-based access control for vocabulary management UI | ui_access: A viewer does not see the restricted actions (UI)<br>ui_access: Opening a restricted form directly shows an authorisation message (<user>) (UI)<br>ui_access: A 403 from the API shows the authorisation message (UI) |
| SIMPL-18195/AC3 | Role-based access control for vocabulary management UI | ui_access: A user without a token is sent to sign in (UI) |
| SIMPL-18196/AC1 | Role-based access control for vocabulary management API | access_control: A viewer may use the read operation <operation><br>access_control: An administrator may use every operation (<operation>) |
| SIMPL-18196/AC2 | Role-based access control for vocabulary management API | access_control: A user with no vocabulary role gets 403 on <operation><br>access_control: A viewer gets 403 on the write operation <operation> and nothing changes<br>access_control: A user with no vocabulary role cannot change anything |
| SIMPL-18196/AC3 | Role-based access control for vocabulary management API | access_control: A request without a token is refused with 401 on <operation><br>access_control: A token that is <kind> is refused with 401 |
| SIMPL-18196/AC4 | Role-based access control for vocabulary management API | access_control: A request without a token is refused with 401 on <operation> |
| SIMPL-18452/AC1 | Download and view full details of all vocabulary versions in the UI | ui_details: Selecting a deprecated version shows all its metadata (UI) |
| SIMPL-18452/AC2 | Download and view full details of all vocabulary versions in the UI | ui_details: A selected version is downloaded as its own file (UI) |
| SIMPL-18559/AC1 | Warning on deprecated vocabulary versions in Vocabulary History UI | ui_details: A deprecated version links to the latest ACTIVE version (UI) |
| SIMPL-18559/AC2 | Warning on deprecated vocabulary versions in Vocabulary History UI | ui_details: Without an ACTIVE version only the deprecation warning is shown (UI) |
| SIMPL-28680/AC1 | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile | none (see below) |
| SIMPL-28680/AC2 | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile | none (see below) |
| SIMPL-28680/AC3 | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile | none (see below) |
| SIMPL-28680/AC4 | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile | none (see below) |
| SIMPL-28680/AC5 | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile | none (see below) |
| SIMPL-28680/AC6 | [API] Governance Authority update metadata quality governance rules by publishing an updated Metadata Quality Profile | none (see below) |

### Criteria without a scenario

| Criterion | Why |
|---|---|
| SIMPL-18162/AC4 | Not testable from outside: an internal error (500) cannot be caused on purpose against a healthy stack. D44 turns store outages into 503, covered in health_and_resilience (@store_down). |
| SIMPL-28680/AC1 | Out of scope by D1: the Metadata Quality Profile belongs to Schema Manager. |
| SIMPL-28680/AC2 | Out of scope by D1: the Metadata Quality Profile belongs to Schema Manager. |
| SIMPL-28680/AC3 | Out of scope by D1: the Metadata Quality Profile belongs to Schema Manager. |
| SIMPL-28680/AC4 | Out of scope by D1: the Metadata Quality Profile belongs to Schema Manager. |
| SIMPL-28680/AC5 | Out of scope by D1: the Metadata Quality Profile belongs to Schema Manager. |
| SIMPL-28680/AC6 | Out of scope by D1: the Metadata Quality Profile belongs to Schema Manager. |

Stories without criteria: SIMPL-393 (met by the generic upload, D1; scenario 'A CPSV-like vocabulary can be uploaded and found in the list') and SIMPL-395 (replaced by the SIMPL-181xx stories, D1).

UI criteria have scenarios tagged `@ui`, run in a browser since step 60 (`features/steps/ui_steps.py`). No `@ui` scenario is tagged `@manual`: all could be automated. Where a UI criterion also has server behaviour (SIMPL-18186, SIMPL-18189, SIMPL-18191, SIMPL-18184 AC1 to AC3) it is also checked through the API.

## Decisions

| Decision | Topic | Scenarios |
|---|---|---|
| D1 | Stories outside this build | 1: create_vocabulary (1) |
| D2 | CDM snapshot | none (see below) |
| D3 | Vocabulary status and version status | 15: create_version (2), create_vocabulary (2), list_vocabularies (2), search (1), status (3), ui_details (1), ui_list (2), validation (2) |
| D4 | Version transitions and same-status requests | 20: create_version (4), create_vocabulary (1), status (9), ui_create (1), ui_status (2), validation (3) |
| D5 | Meaning of latest version | 12: create_version (1), create_vocabulary (1), list_vocabularies (1), search (1), ui_details (1), ui_list (2), vocabulary_details (5) |
| D6 | Publication and manual review | 4: create_version (1), create_vocabulary (1), ui_create (1), ui_status (1) |
| D7 | Version numbering | 4: create_version (3), ui_create (1) |
| D8 | Nothing stored on failure | 14: access_control (1), create_version (4), create_vocabulary (3), health_and_resilience (1), validation (5) |
| D9 | Validation steps and rules | 16: create_vocabulary (1), external_vocabularies (1), ui_validation (2), validation (12) |
| D10 | Validation errors versus validation warnings | 11: create_version (1), create_vocabulary (1), validation (9) |
| D11 | Reference to a deprecated internal version | 7: ui_validation (1), validation (6) |
| D12 | What a vocabulary reference is | 15: validation (15) |
| D13 | Validation on file upload in the UI | 2: ui_validation (2) |
| D14 | Validation report and success feedback | 10: ui_create (2), ui_validation (5), validation (3) |
| D15 | File format, size and media type | 12: create_version (2), create_vocabulary (5), external_vocabularies (2), search (1), status (1), ui_create (1) |
| D16 | Roles and what they allow | 12: access_control (6), list_vocabularies (1), ui_access (2), ui_details (1), vocabulary_details (2) |
| D17 | Where 401 and 403 come from | 11: access_control (9), create_version (1), health_and_resilience (1) |
| D18 | Restricted actions in the UI | 5: ui_access (5) |
| D19 | Name filter | 3: list_vocabularies (2), ui_list (1) |
| D20 | Date filters | 5: list_vocabularies (5) |
| D21 | Unknown query parameters | 8: external_vocabularies (1), list_vocabularies (3), search (1), vocabulary_details (3) |
| D22 | Pagination limits | 12: external_vocabularies (2), list_vocabularies (7), vocabulary_details (3) |
| D23 | Sorting and default order | 10: list_vocabularies (5), ui_list (1), vocabulary_details (4) |
| D24 | Status codes for generic criteria | 12: create_version (1), create_vocabulary (1), status (1), ui_access (1), ui_create (1), ui_list (1), ui_status (2), vocabulary_details (4) |
| D25 | The vocabulary object (VocabularyCategory) | 12: create_version (1), create_vocabulary (2), list_vocabularies (5), search (1), status (2), vocabulary_details (1) |
| D26 | The vocabulary version object | 5: create_version (1), create_vocabulary (1), search (1), vocabulary_details (2) |
| D27 | Creator | 8: create_version (1), create_vocabulary (2), external_vocabularies (1), list_vocabularies (1), ui_details (1), vocabulary_details (2) |
| D28 | Description | 2: create_version (1), create_vocabulary (1) |
| D29 | Content and download | 8: create_vocabulary (2), external_vocabularies (1), ui_details (1), ui_list (1), vocabulary_details (3) |
| D30 | External vocabulary identity and registration | 9: create_vocabulary (1), external_vocabularies (6), search (1), validation (1) |
| D31 | One term per concept | 3: create_vocabulary (1), ui_details (1), ui_list (1) |
| D32 | Changelog on creation | 2: ui_create (2) |
| D33 | New version form | 4: create_version (1), ui_create (3) |
| D34 | List paging and empty state | 3: ui_list (3) |
| D35 | Details and history page | 4: ui_details (4) |
| D36 | Deprecation warnings in the UI | 4: ui_details (3), ui_list (1) |
| D37 | Form field rules and messages | 8: create_version (2), create_vocabulary (4), external_vocabularies (1), ui_create (1) |
| D38 | Search operations: paging, limits, order | 17: search (17) |
| D39 | Decided behaviour the OpenAPI file does not yet show | 9: access_control (1), create_version (1), create_vocabulary (2), list_vocabularies (3), search (1), vocabulary_details (1) |
| D40 | Security scheme in the build | 1: access_control (1) |
| D41 | Test basis for operations without a story | 20: external_vocabularies (14), search (1), status (5) |
| D42 | External vocabularies in the process | 4: external_vocabularies (1), validation (3) |
| D43 | Concurrent changes to one vocabulary | 3: create_version (1), create_vocabulary (1), status (1) |
| D44 | Store and identity provider slow or down | 8: access_control (2), health_and_resilience (5), ui_create (1) |
| D45 | How consumers learn of publication and deprecation | none (see below) |
| D46 | Limits and abuse cases on version creation | 9: access_control (2), create_version (5), ui_create (2) |
| E1 | Identity provider and test users | 10: access_control (10) |
| E2 | Ports and base URLs | 5: create_vocabulary (2), search (1), validation (1), vocabulary_details (1) |
| E3 | Store and stubs | none (see below) |

### Decisions without a scenario

| Decision | Why |
|---|---|
| D2 | Not observable: it fixes which CDM snapshot the writers read. |
| D45 | Not observable as behaviour: it says no events are published. The pull path it relies on (search status, content) is tested under D38 and D29. |
| E3 | Not observable: the store technology. The suite does not reset the store (build profile), so the E3 sentence "tests reset the dataset" is not followed; every scenario uses its own run id instead. |

## Rules

Rules from `rules.json` that a black-box test can observe, and where they are checked:

| Rule | Where |
|---|---|
| API-12 | 2 scenarios, for example: access_control: An administrator may use every operation (<operation>) |
| API-36 | 7 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| API-38 | 5 scenarios, for example: create_version: A new version in an unsupported format is rejected with 415 |
| API-44 | 4 scenarios, for example: create_vocabulary: An invalid or incomplete create request is rejected: <case> |
| API-46 | 19 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| API-47 | 5 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| ARCH-32 | 1 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| BLG-col-paging | 3 scenarios, for example: external_vocabularies: The external vocabularies are listed by name, paged |
| BLG-err-problem | 4 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| BLG-err-sanit | 1 scenarios, for example: health_and_resilience: A read returns 503 with Retry-After when the store is unreachable (store down) |
| BLG-hlth-res | 2 scenarios, for example: access_control: The health operation needs no token |
| BLG-req-valid | 2 scenarios, for example: list_vocabularies: An unknown query parameter is rejected with one issue (<param>) |
| MACH-08 | 3 scenarios, for example: access_control: The health operation needs no token |
| OWA-API3 | 2 scenarios, for example: create_version: A request that carries a version number is rejected and creates nothing |
| OWASP-02 | 6 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| OWASP-08 | 2 scenarios, for example: access_control: A request without a token is refused with 401 on <operation> |
| SEC-05 | 4 scenarios, for example: access_control: A user with no vocabulary role gets 403 on <operation> |

Observable rules not tested, and why:

- API-37 (406 for an unsatisfiable Accept header): only the OpenAPI lists 406; no decision fixes it, and the brief asks for a decision tag on every scenario without a criterion. Left out.
- API-39 (Vary: Accept): no operation negotiates between several success media types.
- API-46 for 400, 404, 500 and 503: the OpenAPI and D44 fix `urn:problem-type:belgif:...` types for these, which the rule does not allow. The decisions win, so API-46 and API-47 are checked only on 401, 403, 409, 413 and 415 (simpl types).
- BLG-req-valid issue type `unknownInput`: D21 fixes 400 with one issue (in: query, name) but not the issue type, so only those are checked.
- OWASP-08 expired token: a truly expired but validly signed token cannot be produced from outside; the 'expired' case re-signs nothing, so it is also a tampered token.
- OWA-API4 (call-rate limit): D46 places it at the Tier 1 gateway, outside this build. The rule id is not in rules.json for this run.
- SIMPL-18162/AC4 and other 500 paths: not reachable from outside (see above).

## UI automation (step 60)

Browser steps for every `@ui` scenario, written against the rendered pages only. Results: `reports/ui.json`, `reports/ui-plain.txt`.

Accessibility: `ui_accessibility.feature` (`@ui @a11y`) runs axe-core with the WCAG 2.1 A and AA rules on the vocabulary list, the details page (with a version panel open), the creation form and the new version form. Raw results: `reports/a11y-<page>.json`.

Test-writer adjustments (scenario meaning unchanged):

- Action labels the specs do not fix. SIMPL-18174, D4 and D18 name only "the status-change action"; the steps accept "Deprecate" or "Change status to Deprecated", and "Activate" or "Change status to Active". D35 asks for "a link back to the list"; the step accepts "Back to list", "Back to the list" or "Back to the vocabulary list".
- ui_create "The creation form reports invalid input (unsupported file)": the expected text was the server detail "Encountered invalid Turtle syntax". Under D13 the browser check shows the syntax error with its line and keeps Submit disabled, so the server is never called. The expected text is now "line 1" (the browser report), as in the ui_validation syntax-error scenario. The submit step does nothing when Submit is disabled.
- Row actions sit in a three-dot menu; the steps open it. A row that is not on the first page is found by typing its name in the Name filter.

Build differences against the decisions (scenarios kept as written):

- ui_list "A sortable column sorts ascending, then descending (Name)", both rows. D23: "first click ascending, second click descending; default Name ascending". The build toggles the current order, so the first click on Name (already ascending by default) sorts descending and the second ascending. D23 does not say what a click on the column that is already sorted does; the spec owners may want to settle it.
