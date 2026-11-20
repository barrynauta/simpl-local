# Coverage of the Vocabulary Manager black-box suite

Generated from the tags of `features/*.feature` (step 40, run 9). '(UI)' marks a browser scenario of `ui.feature` (Playwright steps added in step 60, run with `./run.sh ui`; see section 5). '(wip)' marks a scenario that cannot run black box. Scenario names of outlines are given once; their example rows are separate runs.

## 1. Acceptance criteria

Live criteria: every criterion of `ids.json` except SIMPL-28680 (out of scope, D1, D61). SIMPL-393 and SIMPL-395 have no criteria (D1).

| Criterion | Scenarios |
|---|---|
| SIMPL-18161/AC1 | ui: The vocabulary list shows every vocabulary with its columns and actions (UI) |
| SIMPL-18161/AC2 | ui: A failure to load the list is shown with the problem detail (<status>) (UI) |
| SIMPL-18162/AC1 | create_vocabulary: A CPSV-like vocabulary can be uploaded and found in the list; list: The list is paginated and every vocabulary carries its metadata; list: The list shows the vocabulary status and the latest version number |
| SIMPL-18162/AC2 | list: A wrong pagination parameter is rejected with 400 and no metadata (<parameter>=<value>) |
| SIMPL-18162/AC3 | access: A user with neither role is refused with 403 on <operation>; access: A user without a role gets no vocabulary metadata |
| SIMPL-18162/AC4 | wip: An internal error while listing returns 500 with a detail and no metadata (wip) |
| SIMPL-18163/AC1 | ui: Filtering the list by status and by name (UI) |
| SIMPL-18163/AC2 | ui: Clicking sortable column headers sorts ascending and reverses the active sort (UI) |
| SIMPL-18164/AC1 | list: The name filter is a case-insensitive contains match; list: The status filter returns only vocabularies with that vocabulary status; list: The creationDate filter with a date-time matches to the second; list: The creationDate and lastUpdated filters with a date match the whole UTC day; and 3 more |
| SIMPL-18164/AC2 | list: A filter that matches nothing returns an empty result set without an error (<filter>) |
| SIMPL-18164/AC3 | list: Sorting by creation date and by last update date, together with paging; list: Pagination with sorting returns the requested page and links that keep filter and sort |
| SIMPL-18164/AC4 | list: A wrong pagination parameter is rejected with 400 and no metadata (<parameter>=<value>); list: An invalid name filter is rejected (<case>); list: An invalid filter value is rejected (<parameter>=<value>); list: An unsupported filter or sort field is rejected as an unknown query parameter |
| SIMPL-18165/AC1 | read: The latest content is downloaded byte for byte as text/turtle with the file name <Name>.ttl; read: The latest content is the content of the newest version; ui: Downloading the newest version from the list and reviewing its details (UI) |
| SIMPL-18166/AC1 | read: The full details of an existing version are returned as JSON with a content link |
| SIMPL-18166/AC2 | read: A version that does not exist gives 404 with the decided detail; read: A version of a vocabulary that does not exist gives 404 |
| SIMPL-18167/AC1 | ui: The history opens the details page with all versions newest first (UI) |
| SIMPL-18168/AC1 | read: The version history lists all versions newest first with the metadata for comparison |
| SIMPL-18168/AC2 | read: The version history of a vocabulary that does not exist gives 404 |
| SIMPL-18169/AC1 | ui: The creation form has name, description and file, and no version or change log field (UI) |
| SIMPL-18169/AC2 | ui: Field errors are shown under the fields and nothing is sent (UI); ui: A duplicate name is reported above the form (UI); ui: A file over 5 MB is refused in the browser (UI) |
| SIMPL-18170/AC1 | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier |
| SIMPL-18170/AC2 | create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; create_vocabulary: Version 1 of the created vocabulary is ACTIVE; create_vocabulary: The vocabulary status can be changed to DEPRECATED and back to ACTIVE |
| SIMPL-18170/AC3 | create_vocabulary: A create request with a missing <part> is rejected and nothing is stored; create_vocabulary: A create request with an invalid name is rejected (<case>); create_vocabulary: A name of 2 characters is rejected; create_vocabulary: A create request with an invalid description is rejected (<case>); and 3 more |
| SIMPL-18172/AC1 | ui: The Update form is pre-filled with the latest version (UI) |
| SIMPL-18172/AC2 | ui: Submitting the Update form creates the next version and confirms its number (UI) |
| SIMPL-18172/AC3 | ui: An unexpected error during version creation advises to retry or contact support (<status>) (UI) |
| SIMPL-18173/AC1 | versions: A new version gets the next number, is ACTIVE and deprecates the previous version; versions: All previous versions are DEPRECATED after a third version |
| SIMPL-18173/AC2 | validation: A new version with invalid Turtle is rejected, no version is created and none is deprecated; validation: A semantic error on a new version leaves the vocabulary unchanged; versions: A new version request with an invalid <part> is rejected and nothing changes |
| SIMPL-18173/AC3 | versions: A new version for a vocabulary that does not exist is rejected with 404 |
| SIMPL-18173/AC4 | access: A viewer is refused with 403 on the write operation <operation> and nothing changes; access: A user without a role who tries to create a version gets 403 and no version is created |
| SIMPL-18173/AC5 | access: Creating a version without a token is refused and no version is created |
| SIMPL-18173/AC6 | create_vocabulary: A create request that is not multipart/form-data is rejected with 415 (<ctype>); versions: A new version request that is not multipart/form-data is rejected with 415 |
| SIMPL-18174/AC1 | ui: Deprecating and reactivating a vocabulary from the list (UI) |
| SIMPL-18174/AC2 | ui: Changing the status of a vocabulary that is no longer available (UI) |
| SIMPL-18174/AC3 | ui: A failed status change leaves the status unchanged (UI) |
| SIMPL-18175/AC1 | status: Deprecating a vocabulary returns the updated vocabulary and leaves the versions unchanged; status: Reactivating a vocabulary returns ACTIVE and leaves the versions unchanged |
| SIMPL-18175/AC2 | status: Changing the status of a vocabulary that does not exist gives 404 |
| SIMPL-18175/AC3 | status: A status value other than ACTIVE or DEPRECATED is rejected and the status stays (<body>); status: A status change that is not application/json is rejected with 415 (<path>) |
| SIMPL-18176/AC1 | ui: A viewer may see deprecated versions (UI) |
| SIMPL-18176/AC2 | ui: The history opens the details page with all versions newest first (UI) |
| SIMPL-18176/AC3 | ui: Selecting a version shows all its metadata and a download (UI) |
| SIMPL-18178/AC1 | ui: A deprecated vocabulary shows a warning in the list and on its details page (UI) |
| SIMPL-18180/AC1 | ui: Choosing a valid file runs the syntax check in the browser (UI); ui: Choosing a file with a syntax error shows the line and blocks submit (UI) |
| SIMPL-18180/AC2 | ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI) |
| SIMPL-18180/AC3 | ui: Semantic errors on submit of a new version appear in the validation report (UI) |
| SIMPL-18180/AC4 | ui: Choosing a valid file runs the syntax check in the browser (UI) |
| SIMPL-18180/AC5 | ui: Choosing a file with a syntax error shows the line and blocks submit (UI) |
| SIMPL-18180/AC6 | ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI) |
| SIMPL-18180/AC7 | ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI) |
| SIMPL-18180/AC8 | ui: Choosing a file with a syntax error shows the line and blocks submit (UI) |
| SIMPL-18181/AC1 | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; validation: A valid new version is confirmed by the 201 response without error details |
| SIMPL-18181/AC2 | validation: A create with invalid Turtle is rejected with one syntax issue and nothing is stored; validation: A new version with invalid Turtle is rejected, no version is created and none is deprecated; validation: Semantic validation does not run when the syntax check fails |
| SIMPL-18182/AC1 | ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI); ui: Semantic errors on submit of a new version appear in the validation report (UI) |
| SIMPL-18182/AC2 | ui: A semantically valid submit succeeds without a validation report (UI) |
| SIMPL-18182/AC3 | ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI); ui: Semantic errors on submit of a new version appear in the validation report (UI) |
| SIMPL-18183/AC1 | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; references: An unregistered external namespace gives the warning externalReferenceSkipped and the version is created; validation: A valid new version is confirmed by the 201 response without error details |
| SIMPL-18183/AC2 | validation: A semantic error is reported with its detail and location and nothing is stored (<rule>); validation: A semantic error on a new version leaves the vocabulary unchanged; validation: All semantic errors are returned in one response |
| SIMPL-18184/AC1 | references: References to ACTIVE internal versions pass without a deprecated-version warning; references: An internal reference in the form <form> is recognised |
| SIMPL-18184/AC2 | references: A reference to a deprecated version is a warning that recommends the latest ACTIVE version; references: A reference to a version of a DEPRECATED vocabulary says the vocabulary is deprecated and recommends nothing; references: A reference without a version resolves to the latest version, and no version is recommended when none other is ACTIVE; references: The recommended version is the latest ACTIVE version, also when it is older than the referenced one; and 1 more |
| SIMPL-18184/AC3 | references: A reference to a version that does not exist is error S7 and no version is created; references: A reference to a vocabulary name that does not exist is error S7 |
| SIMPL-18184/AC4 | ui: A reference to a deprecated version shows a warning that recommends the latest ACTIVE version (UI) |
| SIMPL-18186/AC1 | ui: A valid new vocabulary is published as Active and the details page opens (UI) |
| SIMPL-18187/AC1 | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE |
| SIMPL-18187/AC2 | versions: A new version gets the next number, is ACTIVE and deprecates the previous version |
| SIMPL-18189/AC1 | ui: The new version gets the next number without manual input (UI) |
| SIMPL-18189/AC2 | ui: Submitting the Update form creates the next version and confirms its number (UI) |
| SIMPL-18189/AC3 | ui: Submitting the Update form creates the next version and confirms its number (UI) |
| SIMPL-18190/AC1 | versions: A new version gets the next number, is ACTIVE and deprecates the previous version |
| SIMPL-18190/AC2 | versions: A new version request that carries a version field is rejected and nothing is created |
| SIMPL-18191/AC1 | ui: Submitting the Update form creates the next version and confirms its number (UI) |
| SIMPL-18193/AC1 | ui: A valid new vocabulary is published as Active and the details page opens (UI); ui: A semantically valid submit succeeds without a validation report (UI) |
| SIMPL-18193/AC2 | ui: Choosing a file with a syntax error shows the line and blocks submit (UI); ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI); ui: Semantic errors on submit of a new version appear in the validation report (UI) |
| SIMPL-18194/AC1 | validation: A create with invalid Turtle is rejected with one syntax issue and nothing is stored; validation: A new version with invalid Turtle is rejected, no version is created and none is deprecated; validation: A semantic error is reported with its detail and location and nothing is stored (<rule>); validation: All semantic errors are returned in one response |
| SIMPL-18194/AC2 | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; validation: A valid new version is confirmed by the 201 response without error details |
| SIMPL-18195/AC1 | ui: An admin sees all vocabulary management actions (UI) |
| SIMPL-18195/AC2 | ui: A viewer does not see restricted actions and is refused on the form URL (UI); ui: Any 403 from the API shows the authorisation message (UI) |
| SIMPL-18195/AC3 | ui: A user without a token is sent to the Keycloak login page (UI) |
| SIMPL-18196/AC1 | access: A viewer may use the read operation <operation>; access: An admin may use the operation <operation> |
| SIMPL-18196/AC2 | access: A user with neither role is refused with 403 on <operation>; access: A viewer is refused with 403 on the write operation <operation> and nothing changes |
| SIMPL-18196/AC3 | access: A request without a token is refused with 401 on <operation>; access: A token that is not valid for this service is refused with 401 (<kind>); access: A write with a token that is not valid for this service is refused and stores nothing (<kind>) |
| SIMPL-18196/AC4 | access: A request without a token is refused with 401 on <operation> |
| SIMPL-18452/AC1 | ui: Selecting a version shows all its metadata and a download (UI) |
| SIMPL-18452/AC2 | read: The latest content is downloaded byte for byte as text/turtle with the file name <Name>.ttl; read: A selected version is downloaded with the file name <Name>-v<n>.ttl; ui: Downloading a selected version gives the file of that version (UI) |
| SIMPL-18559/AC1 | ui: A deprecated version points to the latest ACTIVE version (UI) |
| SIMPL-18559/AC2 | ui: A deprecated version without any ACTIVE version shows only the warning (UI) |

### Criteria with no runnable API scenario, and why

Every live criterion has at least one scenario. These have only UI or @wip scenarios:

| Criterion | Reason |
|---|---|
| SIMPL-18161/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18161/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18162/AC4 | not testable from outside: a 500 needs a store error that is not a timeout or connection failure (D44), which needs fault injection; kept as @wip. The store-down 503 case (D44) is tested in store_down.feature. |
| SIMPL-18163/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18163/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18167/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18169/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18169/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18172/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18172/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18174/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18174/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18174/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18176/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18176/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18176/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18178/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC4 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC5 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC6 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18180/AC8 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18182/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18182/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18184/AC4 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18189/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18193/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18193/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18195/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18195/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18195/AC3 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18452/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18559/AC1 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18559/AC2 | UI only: the criterion is about screens, labels or browser checks (D13, D18, D34 to D36, D60, D72); Gherkin in ui.feature, browser steps in step 60 |
| SIMPL-18172/AC2 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |
| SIMPL-18180/AC7 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |
| SIMPL-18182/AC2 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |
| SIMPL-18186/AC1 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |
| SIMPL-18189/AC1 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |
| SIMPL-18189/AC2 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |
| SIMPL-18191/AC1 | UI story (SR-13): only the UI scenario carries the tag; the server behaviour behind it is tested by the API criteria SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18170, SIMPL-18183 and SIMPL-18194 |

SIMPL-28680/AC1 to AC6: out of scope (D1, D61), no scenario.

Note on SIMPL-18184/AC2: the criterion asks for a blocking error; D11 (with D47) makes it a warning, and the suite tests the warning. SIMPL-18184/AC3 is tested as error S7 (D12, D47).

## 2. Decisions

| Decision | Topic | Scenarios |
|---|---|---|
| D1 | Stories outside this build | create_vocabulary: A CPSV-like vocabulary can be uploaded and found in the list |
| D2 | CDM snapshot | none, see below |
| D3 | Vocabulary status and version status | create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; create_vocabulary: The vocabulary status can be changed to DEPRECATED and back to ACTIVE; list: The list shows the vocabulary status and the latest version number; and 6 more |
| D4 | Version transitions and same-status requests | create_vocabulary: The vocabulary status can be changed to DEPRECATED and back to ACTIVE; status: A status value other than ACTIVE or DEPRECATED is rejected and the status stays (<body>); status: A status change that is not application/json is rejected with 415 (<path>); and 12 more |
| D5 | Meaning of latest version | create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; read: The full details of an existing version are returned as JSON with a content link; read: latestVersionHref is also filled on the latest version itself; and 5 more |
| D6 | Publication and manual review | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; create_vocabulary: Version 1 of the created vocabulary is ACTIVE |
| D7 | Version numbering | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; ui: The new version gets the next number without manual input (UI); versions: A new version gets the next number, is ACTIVE and deprecates the previous version; and 2 more |
| D8 | Nothing stored on failure | create_vocabulary: A create request with a missing <part> is rejected and nothing is stored; create_vocabulary: A create request with an invalid name is rejected (<case>); create_vocabulary: A create request with an invalid description is rejected (<case>); and 15 more |
| D9 | Validation steps and rules | external: Registering invalid Turtle gives one syntax issue and stores nothing; ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI); ui: Semantic errors on submit of a new version appear in the validation report (UI); and 9 more |
| D10 | Validation errors versus validation warnings | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; references: A reference to a deprecated version is a warning that recommends the latest ACTIVE version; references: An unregistered external namespace gives the warning externalReferenceSkipped and the version is created; and 4 more |
| D11 | Reference to a deprecated internal version | references: References to ACTIVE internal versions pass without a deprecated-version warning; references: A reference to a deprecated version is a warning that recommends the latest ACTIVE version; references: A reference to a version of a DEPRECATED vocabulary says the vocabulary is deprecated and recommends nothing; and 4 more |
| D12 | What a vocabulary reference is | references: References to ACTIVE internal versions pass without a deprecated-version warning; references: An internal reference in the form <form> is recognised; references: A reference without a version resolves to the latest version, and no version is recommended when none other is ACTIVE; and 8 more |
| D13 | Validation on file upload in the UI | ui: Choosing a valid file runs the syntax check in the browser (UI); ui: Choosing a file with a syntax error shows the line and blocks submit (UI) |
| D14 | Validation report and success feedback | ui: A valid new vocabulary is published as Active and the details page opens (UI); ui: Submitting the Update form creates the next version and confirms its number (UI); ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI); and 4 more |
| D15 | File format, size and media type | create_vocabulary: A duplicate name is rejected with 409 and the decided detail; create_vocabulary: A create request that is not multipart/form-data is rejected with 415 (<ctype>); external: A registration that is not multipart/form-data is rejected with 415; and 11 more |
| D16 | Roles and what they allow | access: A request without a token is refused with 401 on <operation>; access: A user with neither role is refused with 403 on <operation>; access: A user without a role gets no vocabulary metadata; and 11 more |
| D17 | Where 401 and 403 come from | access: A request without a token is refused with 401 on <operation>; access: A user with neither role is refused with 403 on <operation>; access: A viewer is refused with 403 on the write operation <operation> and nothing changes; and 7 more |
| D18 | Restricted actions in the UI | ui: An admin sees all vocabulary management actions (UI); ui: A viewer does not see restricted actions and is refused on the form URL (UI); ui: Any 403 from the API shows the authorisation message (UI); and 1 more |
| D19 | Name filter | list: The name filter is a case-insensitive contains match; list: An invalid name filter is rejected (<case>); list: A name filter of 1 character and of 64 characters is accepted; and 1 more |
| D20 | Date filters | list: The creationDate filter with a date-time matches to the second; list: The creationDate and lastUpdated filters with a date match the whole UTC day; list: A filter that matches nothing returns an empty result set without an error (<filter>); and 1 more |
| D21 | Unknown query parameters | external: The external vocabulary list has no filter or sort parameters (<query>); list: An invalid filter value is rejected (<parameter>=<value>); list: An unsupported filter or sort field is rejected as an unknown query parameter; and 2 more |
| D22 | Pagination limits | external: The external vocabulary list is paged and ordered by name; external: The external vocabulary list has no filter or sort parameters (<query>); list: The list is paginated and every vocabulary carries its metadata; and 6 more |
| D23 | Sorting and default order | list: The default order is name ascending by code point; list: Sorting by status breaks ties by name ascending; list: Sorting by creation date and by last update date, together with paging; and 6 more |
| D24 | Status codes for generic criteria | read: A version that does not exist gives 404 with the decided detail; read: A version of a vocabulary that does not exist gives 404; read: The version history of a vocabulary that does not exist gives 404; and 9 more |
| D25 | The vocabulary object (VocabularyCategory) | create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; list: The list is paginated and every vocabulary carries its metadata; list: The list shows the vocabulary status and the latest version number; and 5 more |
| D26 | The vocabulary version object | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; read: The full details of an existing version are returned as JSON with a content link; read: The version history lists all versions newest first with the metadata for comparison; and 3 more |
| D27 | Creator | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; external: Registering an external vocabulary returns 201 with its metadata; and 1 more |
| D28 | Description | create_vocabulary: The created vocabulary can be read with its metadata, version 1 and status ACTIVE; versions: The vocabulary shows the description of its latest version, older versions keep theirs |
| D29 | Content and download | cross_cutting: A download accepts <accept>; external: The content of an external vocabulary is downloaded by name, byte for byte; read: The full details of an existing version are returned as JSON with a content link; and 5 more |
| D30 | External vocabulary identity and registration | external: Registering an external vocabulary returns 201 with its metadata; external: A registered external vocabulary can be read by name; external: The content of an external vocabulary is found by namespace or alias URI (<case>); and 7 more |
| D31 | One term per concept | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; ui: The vocabulary list shows every vocabulary with its columns and actions (UI) |
| D32 | Changelog on creation | ui: The creation form has name, description and file, and no version or change log field (UI) |
| D33 | New version form | ui: The Update form is pre-filled with the latest version (UI); ui: Submitting the Update form creates the next version and confirms its number (UI); versions: A description-only change creates a new version |
| D34 | List paging and empty state | ui: The vocabulary list shows every vocabulary with its columns and actions (UI); ui: A failure to load the list is shown with the problem detail (<status>) (UI); ui: An empty list says that no vocabularies were found (UI) |
| D35 | Details and history page | read: The version history lists all versions newest first with the metadata for comparison; ui: Downloading the newest version from the list and reviewing its details (UI); ui: The history opens the details page with all versions newest first (UI); and 3 more |
| D36 | Deprecation warnings in the UI | ui: A deprecated vocabulary shows a warning in the list and on its details page (UI); ui: A deprecated version points to the latest ACTIVE version (UI); ui: A deprecated version without any ACTIVE version shows only the warning (UI); and 1 more |
| D37 | Form field rules and messages | create_vocabulary: A create request with a missing <part> is rejected and nothing is stored; create_vocabulary: A create request with an invalid name is rejected (<case>); create_vocabulary: A name of 2 characters is rejected; and 11 more |
| D38 | Search operations: paging, limits, order | search: Internal search returns latest and versioned results in request order; search: A latest result carries the vocabulary status; search: Missing names and versions are left out, repeated references give one result, name match is exact; and 14 more |
| D39 | Decided behaviour the OpenAPI file does not yet show | none, see below |
| D40 | Security scheme in the build | none, see below |
| D41 | Test basis for operations without a story | external: Registering an external vocabulary returns 201 with its metadata; external: A registered external vocabulary can be read by name; external: The content of an external vocabulary is downloaded by name, byte for byte; and 15 more |
| D42 | External vocabularies in the process | external: Removing an external vocabulary returns 204 and later reads give 404; external: Removing an external vocabulary does not touch internal vocabularies that reference it; references: Registering the external vocabulary later clears the warning on the next version; and 1 more |
| D43 | Concurrent changes to one vocabulary | create_vocabulary: Two overlapping creations with the same name give one 201 and one 409; status: Two identical overlapping status changes give 200 and 409; versions: Five overlapping new versions all succeed and get distinct numbers |
| D44 | Store and identity provider slow or down | access: GET /health needs no token and reports UP; limits: Parallel uploads beyond the limit of 4 get 503 with Retry-After, never another error; store_down: Listing vocabularies returns 503 with Retry-After (store-down); and 6 more |
| D45 | How consumers learn of publication and deprecation | search: Internal search returns latest and versioned results in request order |
| D46 | Limits and abuse cases on version creation | access: A viewer is refused with 403 on the write operation <operation> and nothing changes; access: A user without a role who tries to create a version gets 403 and no version is created; ui: The Update form is pre-filled with the latest version (UI); and 5 more |
| D47 | Deprecated and missing reference texts | references: A reference to a deprecated version is a warning that recommends the latest ACTIVE version; references: A reference to a version of a DEPRECATED vocabulary says the vocabulary is deprecated and recommends nothing; references: A reference without a version resolves to the latest version, and no version is recommended when none other is ACTIVE; and 5 more |
| D48 | Problem types: belgif and simpl | cross_cutting: Every error is problem+json with a lowerCamelCase type (<case>) |
| D49 | Invalid name in a path | read: An invalid path value gives 400 with one schemaViolation issue (<path>); read: An invalid path value on PATCH gives 400 (<path>); versions: A new version for an invalid path name is rejected with 400 |
| D50 | Click on the active sort column | ui: Clicking sortable column headers sorts ascending and reverses the active sort (UI) |
| D51 | Order of checks on write operations | access: Authentication runs before body validation; access: The role check runs before body validation; access: The role check runs before the not-found check on a write; and 11 more |
| D52 | Unknown fields and parameters | create_vocabulary: An unknown form field is rejected with one issue naming it; create_vocabulary: A multipart part with an empty name is an unknown field; create_vocabulary: A query parameter on POST /vocabularies is rejected; and 7 more |
| D53 | Labels without a language tag (S4) | validation: Two untagged preferred labels on one subject are an S4 error with language none; validation: One untagged and one tagged preferred label are allowed |
| D54 | Case of names in paths | read: Paths match the stored name exactly (<path>); search: Missing names and versions are left out, repeated references give one result, name match is exact |
| D55 | Problem texts the decisions do not give | access: An unknown path gives 404 problem+json with a token; access: A method the server does not know returns 405 before the token check (<user>); access: A method that a fixed path does not offer returns 405 (<path>); and 5 more |
| D56 | External vocabulary registration details | external: Registering an existing name is rejected with 409, also in another case; external: An external vocabulary may share its name with an internal vocabulary; external: 20 alias URIs are accepted; and 4 more |
| D57 | Base IRI of uploaded content | validation: Relative IRIs resolve against the content URL of the new version |
| D58 | Times and name order | create_vocabulary: A valid vocabulary is created as version 1, ACTIVE, with its identifier; external: Registering an external vocabulary returns 201 with its metadata; list: The creationDate filter with a date-time matches to the second; and 2 more |
| D59 | prev link beyond the last page | list: Pagination with sorting returns the requested page and links that keep filter and sort; list: A page beyond the last returns an empty page with the right totals and prev pointing to the last page |
| D60 | UI texts and steps | ui: The creation form has name, description and file, and no version or change log field (UI); ui: An unexpected error during version creation advises to retry or contact support (<status>) (UI); ui: Deprecating and reactivating a vocabulary from the list (UI); and 1 more |
| D61 | SIMPL-28680 leaves the live set | none, see below |
| E1 | Identity provider and test users | access: A viewer may use the read operation <operation>; access: An admin may use the operation <operation> |
| E2 | Ports and base URLs | none, see below |
| E3 | Store and stubs | none, see below |
| E4 | Run environment from the build profile | list: The list is paginated and every vocabulary carries its metadata |
| D62 | Submit with field errors | ui: Field errors are shown under the fields and nothing is sent (UI); ui: A file over 5 MB is refused in the browser (UI); ui: Choosing a file with a syntax error shows the line and blocks submit (UI) |
| D63 | 406 Not Acceptable | cross_cutting: An Accept header a JSON operation cannot serve gives 406 (<accept>); cross_cutting: An Accept header a download cannot serve gives 406 (<accept>); cross_cutting: A JSON operation accepts <accept>; and 5 more |
| D64 | Latest ACTIVE version link in the UI | ui: A deprecated version points to the latest ACTIVE version (UI); ui: A deprecated version without any ACTIVE version shows only the warning (UI); ui: A version of a DEPRECATED vocabulary recommends no version (UI) |
| D65 | Location texts of issues and warnings | validation: A create with invalid Turtle is rejected with one syntax issue and nothing is stored; validation: A semantic error is reported with its detail and location and nothing is stored (<rule>); validation: Two untagged preferred labels on one subject are an S4 error with language none; and 1 more |
| D66 | Not-found and conflict texts | external: The content of an external vocabulary is found by namespace or alias URI (<case>); external: Content lookup by an unknown namespace gives 404 with the URI as sent; external: An external vocabulary that does not exist gives 404 on <method> <path>; and 7 more |
| D67 | Requests rejected before routing | access: An unknown path gives 401 without a token; access: An unknown path gives 404 problem+json with a token; access: Actuator paths do not exist on the API port (<user>); and 2 more |
| D68 | Issue names in search requests | search: An internal search with 101 references is rejected; search: An invalid internal search body is rejected (<case>); search: An external search with more than 100 URIs is rejected (<a> + <b>); and 1 more |
| D69 | Internal reference shapes | references: A version that is not a positive integer is error S7 with the version as given; references: A content URL without a valid name is an external reference (<case>) |
| D70 | Content and multipart details | create_vocabulary: A multipart part with an empty name is an unknown field; external: The content of an external vocabulary is downloaded by name, byte for byte; external: Registering a file with no statements is rejected; and 5 more |
| D71 | Time budget of writes | limits: A file of exactly 5 MB is accepted and stored within 30 seconds; limits: A new version with a file of exactly 5 MB is accepted within 30 seconds |
| D72 | UI labels and texts | ui: The vocabulary list shows every vocabulary with its columns and actions (UI); ui: A failure to load the list is shown with the problem detail (<status>) (UI); ui: Filtering the list by status and by name (UI); and 6 more |
| D73 | URI form, order and validation | external: Registering an external vocabulary returns 201 with its metadata; external: An invalid content lookup body is rejected (<case>); external: An invalid registration field is rejected (<case>); and 1 more |
| D74 | Routes of the forms | ui: The creation form has name, description and file, and no version or change log field (UI); ui: The Update form is pre-filled with the latest version (UI) |
| D75 | UI semantics the tests may use | ui: The vocabulary list shows every vocabulary with its columns and actions (UI); ui: Selecting a version shows all its metadata and a download (UI) |
| D76 | Where the upload limit applies | limits: A file of exactly 5 MB is accepted and stored within 30 seconds; limits: A new version with a file of exactly 5 MB is accepted within 30 seconds; limits: A file of 5 MB plus one byte is rejected with 413 and nothing is stored; and 4 more |
| D77 | Paths that can match the same URL | access: A method that a fixed path does not offer returns 405 (<path>) |
| D78 | Paging links keep the request | list: Pagination with sorting returns the requested page and links that keep filter and sort |
| D79 | Contract changes | none, see below |
| D80 | Personal data in logs | none, see below |
| D81 | Several backend instances | none, see below |
| Q1 | Response time and throughput | limits: A file of exactly 5 MB is accepted and stored within 30 seconds; limits: A new version with a file of exactly 5 MB is accepted within 30 seconds |
| Q2 | Limits | create_vocabulary: A name of 64 characters is accepted; create_vocabulary: A name of 3 characters is accepted; create_vocabulary: A description of 1000 characters is accepted; and 16 more |
| Q3 | Availability and recovery | none, see below |
| Q4 | Design load and scaling | none, see below |
| Q5 | Maintainability | none, see below |
| Q6 | Accessibility | ui: The <page> has no axe violations (WCAG 2.1 AA) (UI) |
| E5 | Tokens: audience and issuer | access: A token that is not valid for this service is refused with 401 (<kind>); access: A write with a token that is not valid for this service is refused and stores nothing (<kind>) |
| E6 | Reuse choices | none, see below |
| E7 | Test clients for token checks | access: A token that is not valid for this service is refused with 401 (<kind>); access: A write with a token that is not valid for this service is refused and stores nothing (<kind>) |
| D82 | Concurrency through the store | create_vocabulary: Two overlapping creations with the same name give one 201 and one 409; versions: Five overlapping new versions all succeed and get distinct numbers |
| D83 | Store access | none, see below |
| D84 | Authorisation by default deny | access: A request without a token is refused with 401 on <operation>; access: An unknown path gives 401 without a token; access: An unknown path gives 404 problem+json with a token |
| D85 | Bounded store queries | none, see below |
| D86 | Metrics and trace context | cross_cutting: The service returns the X-Correlation-Id it received; cross_cutting: The service creates an X-Correlation-Id when none is sent; cross_cutting: A W3C traceparent header is accepted |
| D87 | Model and contract deliverables | none, see below |
| D88 | Realm key refetch | none, see below |
| D89 | Restore and later writes | none, see below |
| D90 | Validation report semantics | ui: Semantic errors on submit of a new vocabulary appear in the validation report (UI); ui: A reference to a deprecated version shows a warning that recommends the latest ACTIVE version (UI) |
| D91 | Integer range | list: A wrong pagination parameter is rejected with 400 and no metadata (<parameter>=<value>); list: The largest 32-bit page number gives an empty page; read: The largest 32-bit version number is a valid path value that is not found |
| D92 | Concurrent uploads | limits: Parallel uploads beyond the limit of 4 get 503 with Retry-After, never another error; versions: Five overlapping new versions all succeed and get distinct numbers |
| D93 | Request validation against the contract | none, see below |
| D94 | Library choices | none, see below |
| D95 | Lint, format and UI tests | none, see below |
| D96 | Structured log fields | none, see below |
| D97 | CDM, licence and glossary in the build | none, see below |
| D98 | Local stack hardening | none, see below |
| D99 | Load test shape | none, see below |

### Decisions with no scenario, and why

| Decision | Reason |
|---|---|
| D40 | Scopes not checked: every token of the suite asks only for the openid scope, so every successful read and write in the role matrix shows that the read and write scopes are not required. No separate scenario. |
| D2 | CDM snapshot: a rule for writers, no observable behaviour. |
| D39 | Records that the decisions win over the OpenAPI file. The decided behaviour it lists is tested under D5, D7, D10, D17, D19, D21, D22, D27, D37, D38. |
| D61 | SIMPL-28680 leaves the live set: nothing to test. |
| E2 | Default ports and URLs: the suite reads every value from environment variables (E4). contentBase is learnt from contentHref or CONTENT_BASE_URL. |
| E3 | Store choice (Fuseki): not observable black box. The reset sentence is withdrawn by E4. |
| D79 | Summary list of contract changes; each change is tested under its own decision. |
| D80 | Personal data in logs: logs are not reachable through the API. |
| D81 | Withdrawn by D82. |
| Q3 | Availability and backup and restore: operational, measured in step 70 with the stack scripts, not by HTTP scenarios. |
| Q4 | Design load: used as the load profile of load/ (seed of D99, 20 users); no separate scenario. |
| Q5 | Coverage, Trivy and the one-query reference check: build-side measurements (mvn verify, Trivy), not observable black box. |
| E6 | Reuse choices: build-side. |
| D83 | Store users and network: not reachable from the API. Note: build.yaml still lists a Fuseki host port 3141 for debugging, which D83 forbids; the suite does not test it. |
| D85 | Bounded store queries: internal; the observable limits (pageSize 100, search 100) are tested under D22 and D38. |
| D87 | Model and contract deliverables in docs/: reviewed, not tested. |
| D88 | Key refetch window: needs a token signed by a new realm key; D44 puts the identity provider cases in unit tests. |
| D89 | Restore procedure: operational (step 70). |
| D93 | REUSE.md and request validation choice: build-side. |
| D94 | Library choices: build-side. |
| D95 | Lint, format and UI tests: build-side. |
| D96 | Structured log fields: logs are not reachable through the API. |
| D97 | CDM, licence and glossary files: build-side. |
| D98 | Local stack hardening: deployment, not the API. |
| D99 | Load test shape: implemented by load/ (see section 3), not by a scenario. |

Decisions that are only partly testable from outside:

- D44: the identity-provider-down case and the retry timings are not tested (D44 itself moves them to unit tests). Store down is tested in `store_down.feature` (run mode store-down).
- D43, D82: overlapping writes are tested with real parallel requests; the scenario cannot prove serialisation beyond the outcomes D43 lists. The two-instance test of D82 is the build's own.
- D92: the suite sends 6 parallel 3 MB uploads and accepts only 201 or the decided 503; whether a 503 occurs depends on timing, so it does not require one.
- D46: the call-rate limit (OWA-API4) is outside this build (gateway); not tested.
- D86: only X-Correlation-Id and acceptance of traceparent are observable; metrics live on the management port that is not exposed (D67, D98).
- D67, D84: the management port and the 'no handler without a rule' build check are not reachable; unknown paths, /actuator paths, 405 and the firewall 400 are tested.

## 3. Quality targets (Q decisions, NFR-13)

| Target | Checked by | Notes |
|---|---|---|
| Q1 response time and throughput | `load/` (Locust, `./run.sh load`): one task per operation, offered 20 reads/s and 1 write/s for 5 minutes after seeding (D99); p50, p95, p99 and error rate per operation in `reports/load.json`; verdicts against p95 300 ms (reads) and 3 s (writes up to 1 MB), achieved rate at least 95 % of offered, no errors. 5 MB writes: separate step in `run_load.py` (target 30 s, D71). The write mix has a share of 1 MB new versions, reported apart as createVocabularyVersion1MB; the first 30 s (LOAD_WARMUP_S) are a warm-up left out of the statistics and the API scenarios in `limits.feature` (response under 30 s). | Step 70 runs it. |
| Q2 limits | `limits.feature`, plus limit scenarios in create_vocabulary, versions, list, search, external (each at the limit and above): file 5 MB (D15, D76), request 6 MB (D76, above only: a request at exactly 6 MB cannot be built with valid fields because the file is capped at 5 MB and fields at 1000 characters), name 3 and 64 (D37), description and changelog 1000 (D37, D56), pageSize 100 (D22), search 100 items and 100 URIs (D38), 20 aliases (D56), 32-bit integers (D91), 4 parallel uploads (D92). | NFR-04 |
| Q3 availability and recovery | Not measured by this suite: backup and restore round trip and availability are operational checks of step 70. | NFR-13: listed as not measured here. |
| Q4 design load | Used as the load profile (seed 1000 x 20 versions, 20 read users). | |
| Q5 maintainability | Not measured by this suite: coverage and Trivy are build-side (step 50 and 70). | |
| Q6 accessibility | `ui.feature`: 'The <page> has no axe violations' for the four pages (axe-core through Playwright, step 60). | NFR-10 |

## 4. Rules checked

| Rule | Where |
|---|---|
| OWASP-02, SEC-05, ARCH-32 | access.feature role matrix (401, 403 on every secured operation) |
| OWASP-08, OWT-A07 | access.feature token kinds: no audience, other issuer, tampered signature, tampered payload, malformed |
| API-46, API-47 (D48: API-46 only on the simpl codes) | step 'the problem type follows API-46 and API-47' in access, create, status, limits, external, cross_cutting |
| API-36 | Content-Type checks on JSON, problem and download responses |
| API-37, BLG-cnt-negot | cross_cutting.feature (406, D63) |
| API-38 | 415 scenarios on every write and search operation |
| API-12, API-44, BLG-stat-codes | status codes per operation; 4xx for client errors, 503 for the store (store_down.feature) |
| BLG-err-problem | every error is checked as application/problem+json with status and type |
| BLG-req-valid, OWA-API3 | unknown fields, query parameters and the version field rejected (D7, D52) |
| BLG-col-paging | list.feature paging |
| BLG-hlth-res, MACH-08, NFR-07 | GET /health UP (api) and DOWN (store-down). Graceful shutdown (NFR-07) needs the stack to be stopped during a write; not done by this suite. |
| NFR-04 | limits, see Q2 |
| NFR-03 | load/ |
| OWT-A02 | /actuator paths not served on the API port (D67) |
| OWT-A10, ARCH-35 | nothing stored after a failure (D8); store down gives a defined 503 |
| NFR-10 | ui.feature axe scenarios (step 60) |

Not checked: API-39 (Vary: Accept). No resource of this API negotiates between several representations: JSON operations serve JSON, downloads serve Turtle, and errors are problem+json. No decision fixes a Vary header, so the suite leaves it open. API-25 (HTTPS): the local stack of the build profile is plain HTTP by design.


## 5. UI automation (step 60)

`./run.sh ui` runs every `@ui` scenario (49 runs once outline rows are expanded) in headless Chromium with
Playwright for Python. Steps are in `features/steps/ui_steps.py`. Each scenario opens a new browser
context with no stored session, signs in on the Keycloak login page of E1 and creates its own data
through the API. Results go to `reports/ui.json` (behave JSON); the axe results per page go to
`reports/a11y/<page>.json`.

How elements are found: routes of D31 and D74 only; tables by the column headers of D23 and D35; row
menus 'Actions for <Name>' and 'Actions for version <n>' (D75); regions 'Version <n>', 'Validation
report', 'Validation warnings' (D75, D90); messages by role status and role alert (D75); fields by the
labels of D60 and D72; texts of D18, D34, D35, D36, D46, D60, D72, D76. API stubs (D24) answer the
browser's own fetch or XHR calls to the API path of E2 (`/v1/...`) with a problem+json body. No test
id, CSS class or accessible name was taken from the build.

No scenario is tagged `@manual`. Every `@ui` scenario could be automated with the decided texts.

### Assumptions (no decision fixes these)

- Filters are applied the way a user submits a form: Enter in 'Name contains', and a form submit
  after a choice in 'Vocabulary status'. A build that applies filters on input also passes.
- 'Selecting a version' (D35) is a click on the Version cell of its row (the control inside it, if any).
- The page controls of D34 are found by a button or link whose name contains 'next'. Question Q-UI-1.
- 'The banner' of D36 on the details page is checked as visible text; no role is decided. Question Q-UI-2.
- A field error 'under' a field (D37) is text after the field inside its own wrapper, or the target of
  its aria-describedby or aria-errormessage. The chosen file name is not counted as an error.
- The menu item 'Vocabularies' (D60) is looked for inside a navigation landmark.
- Keyboard reachability (Q6) is checked with Tab, and Enter plus arrow keys inside an open row menu.

### Questions for the decisions

- Q-UI-1 (D34): what are the names of the page controls (for example 'Next page', 'Previous page')?
- Q-UI-2 (D36): which role does the deprecation banner on the details page have (none, status, note)?
- Q-UI-3 (D62, D37): should every empty required field show its error on Submit, also 'Description' and
  'Vocabulary file', before any file is chosen? The scenario reads D37 and D62 that way.

### Result against run 9 (19 Nov 2026)

49 UI runs: 47 pass, 2 fail. Both failures are differences between the build and the specs or decisions;
the scenarios are kept as they are.

| Scenario | Class | What the build does |
|---|---|---|
| ui.feature:7 The vocabulary list shows every vocabulary with its columns and actions | build differs from SIMPL-18161/AC1 | The list has no 'Creator' column. Headers are Name, Status, Latest version, Description, Date created, Last updated, Actions. |
| ui.feature:118 Field errors are shown under the fields and nothing is sent | build differs from D37 and D62 | With Name invalid and Description and file empty, Submit shows only the Name error. No error under 'Description' or 'Vocabulary file', and the focus stays on 'Submit' instead of moving to 'Name' (seen in a manual check; behave stops at the first failed step). Nothing is sent, which is correct. |

Accessibility (Q6, NFR-10): axe-core, tags wcag2a, wcag2aa, wcag21a, wcag21aa, on the list, the details
page, the creation form and the new version form: 0 violations on each. Keyboard reachability of every
list action: passes.
