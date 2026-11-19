# Coverage of the Vocabulary Manager API suite

Generated tables come from `tools/coverage.py` (scenario tags); the reasons are written by hand.
Kinds: (API) runs with `./run.sh api`; (store-down) with `./run.sh store-down`; (UI) runs in a browser with
`./run.sh ui` (Playwright steps from step 60, section 7); (wip) cannot be provoked from outside and runs in no mode.

Totals: 424 scenarios (outline rows counted), of which 374 API, 7 store-down, 41 UI (all automated, 0 @manual), 2 wip.

## 1. Acceptance criteria

All 85 live criteria of `ids.json` have at least one scenario. SIMPL-393 and SIMPL-395 have no criteria
(D1); SIMPL-28680 is out of the inventory (D61).

| Criterion | Scenarios |
|---|---|
| SIMPL-18161/AC1 | ui_list.feature: The list shows every vocabulary with its metadata and a row actions menu (UI) |
| SIMPL-18161/AC2 | ui_list.feature: A load error of the list is shown with the problem detail (UI) |
| SIMPL-18162/AC1 | vocabulary_list.feature: The list returns a page of vocabularies with their metadata (API) |
| SIMPL-18162/AC2 | vocabulary_list.feature: Wrong paging parameters are rejected (<query>) (API) |
| SIMPL-18162/AC3 | access_control.feature: A user with neither role gets 403 on <operation> (API) |
| SIMPL-18162/AC4 | not_runnable.feature: An internal error on the list gives 500 with the problem detail (wip) |
| SIMPL-18163/AC1 | ui_list.feature: The name and status filters narrow the list (UI)<br>ui_list.feature: The creation date filter narrows the list (UI) |
| SIMPL-18163/AC2 | ui_list.feature: A click on a sortable column sorts it; a click on the active sort column reverses it (UI) |
| SIMPL-18164/AC1 | vocabulary_list.feature: The name filter is a case-insensitive contains match (API)<br>vocabulary_list.feature: The status filter uses the vocabulary status (API)<br>vocabulary_list.feature: The creation date filter matches a whole UTC day or an exact second (API)<br>vocabulary_list.feature: The last update filter matches a whole UTC day or an exact second (API)<br>vocabulary_list.feature: The default order is name ascending by code point (API)<br>vocabulary_list.feature: The list sorts on <sort> with ties broken by name ascending (API) |
| SIMPL-18164/AC2 | vocabulary_list.feature: A filter that matches nothing returns an empty result without error (API) |
| SIMPL-18164/AC3 | vocabulary_list.feature: Paging works together with sorting and keeps the order (API) |
| SIMPL-18164/AC4 | vocabulary_list.feature: An unsupported or invalid filter or sort is rejected (<query>) (API) |
| SIMPL-18165/AC1 | ui_list.feature: The download action downloads the latest version and the details show its metadata (UI) |
| SIMPL-18166/AC1 | versions.feature: The full metadata of a version is returned, with the latest version link (API)<br>versions.feature: The latest content is downloaded as the stored Turtle bytes (API)<br>versions.feature: A specific version's content is downloaded as the stored Turtle bytes (API) |
| SIMPL-18166/AC2 | versions.feature: A version that does not exist gives 404 (API)<br>versions.feature: A version of a vocabulary that does not exist gives 404 (API) |
| SIMPL-18167/AC1 | ui_details.feature: The history action opens the details page with all versions newest first (UI) |
| SIMPL-18168/AC1 | versions.feature: The version history lists all versions newest first with their metadata (API) |
| SIMPL-18168/AC2 | versions.feature: The version history of a vocabulary that does not exist gives 404 (API) |
| SIMPL-18169/AC1 | ui_forms.feature: The create form has name, description and file, and no version or change log field (UI) |
| SIMPL-18169/AC2 | ui_forms.feature: Field errors are shown under the fields and nothing is sent (UI)<br>ui_forms.feature: A duplicate name is reported above the form (UI)<br>ui_forms.feature: A file over 5 MB is refused in the browser (UI)<br>ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI) |
| SIMPL-18170/AC1 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API) |
| SIMPL-18170/AC2 | create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>create_vocabulary.feature: The vocabulary status can be changed to DEPRECATED and back to ACTIVE (API) |
| SIMPL-18170/AC3 | create_vocabulary.feature: A request without the required field <field> is rejected and nothing is stored (API)<br>create_vocabulary.feature: A name that breaks the name rules is rejected (<case>) (API)<br>create_vocabulary.feature: A name of 2 characters is rejected (API)<br>create_vocabulary.feature: A name of 65 characters is rejected (API)<br>create_vocabulary.feature: A <field> that breaks the length rules is rejected (<case>) (API)<br>validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API) |
| SIMPL-18172/AC1 | ui_forms.feature: The update form is pre-filled with the latest description and content (UI) |
| SIMPL-18172/AC2 | ui_forms.feature: A new version is created from the form and the confirmation names its number (UI) |
| SIMPL-18172/AC3 | ui_forms.feature: An unexpected error on version creation advises to retry or contact support (UI) |
| SIMPL-18173/AC1 | versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API) |
| SIMPL-18173/AC2 | validation.feature: A new version with a syntax error is rejected and nothing changes (API)<br>validation.feature: All semantic errors are returned in one response (API)<br>versions.feature: A new version with <case> is rejected with validation feedback (API) |
| SIMPL-18173/AC3 | versions.feature: A new version for a vocabulary that does not exist is rejected with 404 (API) |
| SIMPL-18173/AC4 | access_control.feature: A viewer gets 403 on the write operation <operation> (API) |
| SIMPL-18173/AC5 | access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| SIMPL-18173/AC6 | create_vocabulary.feature: A create request that is not multipart/form-data is rejected with 415 (API)<br>versions.feature: A new version request that is not multipart/form-data is rejected with 415 (API) |
| SIMPL-18174/AC1 | ui_list.feature: The status of a vocabulary is changed from the list (UI) |
| SIMPL-18174/AC2 | ui_list.feature: A status change of a vocabulary that is no longer available (UI) |
| SIMPL-18174/AC3 | ui_list.feature: A failed status change keeps the status in the UI (UI) |
| SIMPL-18175/AC1 | vocabulary_status.feature: The vocabulary status is set to DEPRECATED and the versions keep their status (API)<br>vocabulary_status.feature: A DEPRECATED vocabulary is set back to ACTIVE (API) |
| SIMPL-18175/AC2 | vocabulary_status.feature: A status change for a vocabulary that does not exist gives 404 (API) |
| SIMPL-18175/AC3 | vocabulary_status.feature: A status other than ACTIVE or DEPRECATED is rejected and the status stays (<body>) (API) |
| SIMPL-18176/AC1 | access_control.feature: A viewer may use the read operation <operation> (API)<br>ui_details.feature: A viewer sees deprecated versions labelled as deprecated (UI) |
| SIMPL-18176/AC2 | ui_details.feature: A viewer sees deprecated versions labelled as deprecated (UI) |
| SIMPL-18176/AC3 | ui_details.feature: Opening a deprecated version shows all its metadata (UI) |
| SIMPL-18178/AC1 | ui_list.feature: A deprecated vocabulary shows a warning in the list and on its details page (UI) |
| SIMPL-18180/AC1 | ui_forms.feature: A chosen file is checked for syntax in the browser (UI) |
| SIMPL-18180/AC2 | ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI) |
| SIMPL-18180/AC3 | ui_forms.feature: A new version is created from the form and the confirmation names its number (UI) |
| SIMPL-18180/AC4 | ui_forms.feature: A chosen file is checked for syntax in the browser (UI) |
| SIMPL-18180/AC5 | ui_forms.feature: A syntax error in the chosen file shows its line and blocks submit (UI) |
| SIMPL-18180/AC6 | ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI) |
| SIMPL-18180/AC7 | ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI) |
| SIMPL-18180/AC8 | ui_forms.feature: A syntax error in the chosen file shows its line and blocks submit (UI) |
| SIMPL-18181/AC1 | validation.feature: Valid content passes both steps and the response confirms creation without errors (API)<br>validation.feature: Content with valid syntax goes on to semantic validation (API) |
| SIMPL-18181/AC2 | validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: A new version with a syntax error is rejected and nothing changes (API)<br>validation.feature: After a syntax error semantic validation does not run (API) |
| SIMPL-18182/AC1 | ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI) |
| SIMPL-18182/AC2 | ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI) |
| SIMPL-18182/AC3 | ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI) |
| SIMPL-18183/AC1 | validation.feature: Valid content passes both steps and the response confirms creation without errors (API)<br>validation.feature: S4 one untagged and one tagged preferred label are not an error (API)<br>validation.feature: An unregistered external namespace gives a warning and the vocabulary is created (API) |
| SIMPL-18183/AC2 | validation.feature: Content with valid syntax goes on to semantic validation (API)<br>validation.feature: S1 a literal as the object of an object property is a validation error (API)<br>validation.feature: S2 a resource as the object of a datatype property is a validation error (API)<br>validation.feature: S4 two preferred labels with the same language tag are a validation error (API)<br>validation.feature: S4 two untagged preferred labels count as one language (API)<br>validation.feature: S5 a resource typed with two disjoint classes is a validation error (API)<br>validation.feature: S6 content with no triples is a validation error (API)<br>validation.feature: All semantic errors are returned in one response (API) |
| SIMPL-18184/AC1 | validation.feature: References to ACTIVE versions give no deprecated-version warning (API) |
| SIMPL-18184/AC2 | validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API)<br>validation.feature: A reference to a version of a DEPRECATED vocabulary warns without recommending a version (API)<br>validation.feature: A reference to a deprecated version with no ACTIVE version left gives the first sentence only (API)<br>validation.feature: A reference without a version resolves to the latest version, whatever its status (API)<br>validation.feature: Each reference to a deprecated version gives its own warning (API) |
| SIMPL-18184/AC3 | validation.feature: S7 a reference to a version that does not exist blocks a new version (API)<br>validation.feature: S7 a reference without a version to a vocabulary that does not exist blocks creation (API) |
| SIMPL-18184/AC4 | ui_forms.feature: A reference to a deprecated version is shown in the warnings panel after creation (UI) |
| SIMPL-18186/AC1 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API) |
| SIMPL-18187/AC1 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API) |
| SIMPL-18187/AC2 | versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API) |
| SIMPL-18189/AC1 | ui_forms.feature: A new version is created from the form and the confirmation names its number (UI) |
| SIMPL-18189/AC2 | ui_forms.feature: A new version is created from the form and the confirmation names its number (UI) |
| SIMPL-18189/AC3 | ui_forms.feature: A new version is created from the form and the confirmation names its number (UI) |
| SIMPL-18190/AC1 | versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API) |
| SIMPL-18190/AC2 | versions.feature: A version form field is rejected and no version is created (API) |
| SIMPL-18191/AC1 | ui_forms.feature: A new version is created from the form and the confirmation names its number (UI)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API)<br>versions.feature: Every older ACTIVE version is deprecated, also after a manual reactivation (API) |
| SIMPL-18193/AC1 | ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI) |
| SIMPL-18193/AC2 | ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI) |
| SIMPL-18194/AC1 | validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: S1 a literal as the object of an object property is a validation error (API)<br>validation.feature: All semantic errors are returned in one response (API) |
| SIMPL-18194/AC2 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>validation.feature: Valid content passes both steps and the response confirms creation without errors (API) |
| SIMPL-18195/AC1 | ui_list.feature: An admin sees the create, update and status actions (UI) |
| SIMPL-18195/AC2 | ui_list.feature: A viewer does not see the restricted actions and a direct form URL is refused (UI) |
| SIMPL-18195/AC3 | ui_list.feature: A user without a token is sent to the login page (UI) |
| SIMPL-18196/AC1 | access_control.feature: A viewer may use the read operation <operation> (API)<br>access_control.feature: An admin may use every operation (<operation>) (API) |
| SIMPL-18196/AC2 | access_control.feature: A user with neither role gets 403 on <operation> (API)<br>access_control.feature: A viewer gets 403 on the write operation <operation> (API)<br>access_control.feature: A viewer's write changes nothing (API) |
| SIMPL-18196/AC3 | access_control.feature: A tampered token is rejected on a write and nothing is stored (API)<br>access_control.feature: A request without a token is rejected with 401 on <operation> (API)<br>access_control.feature: A token that is not valid is rejected with 401 (<caller>) (API) |
| SIMPL-18196/AC4 | access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| SIMPL-18452/AC1 | ui_details.feature: Opening a deprecated version shows all its metadata (UI) |
| SIMPL-18452/AC2 | ui_details.feature: A selected version is downloaded as its Turtle file (UI) |
| SIMPL-18559/AC1 | ui_details.feature: A deprecated version links to the latest ACTIVE version (UI) |
| SIMPL-18559/AC2 | ui_details.feature: A deprecated version without an ACTIVE version shows only the warning (UI) |

### Criteria without a runnable API scenario, and why

- **UI only (browser scenarios, `./run.sh ui`):** SIMPL-18161/AC1, AC2; SIMPL-18163/AC1, AC2;
  SIMPL-18165/AC1; SIMPL-18167/AC1; SIMPL-18169/AC1, AC2; SIMPL-18172/AC1, AC2, AC3; SIMPL-18174/AC1, AC2, AC3;
  SIMPL-18176/AC2, AC3; SIMPL-18178/AC1; SIMPL-18180/AC1 to AC8; SIMPL-18182/AC1 to AC3; SIMPL-18184/AC4;
  SIMPL-18189/AC1 to AC3; SIMPL-18193/AC1, AC2; SIMPL-18195/AC1 to AC3; SIMPL-18452/AC1, AC2; SIMPL-18559/AC1, AC2.
  The server behaviour behind several of them is also checked through the API (for example SIMPL-18186/AC1,
  SIMPL-18191/AC1 and SIMPL-18176/AC1 have API scenarios; SIMPL-18180 and SIMPL-18182 rest on the same
  server validation as SIMPL-18181 and SIMPL-18183).
- **Not testable from outside:** SIMPL-18162/AC4 (internal error, 500). A black-box test cannot make the
  store fail with an error that is not a timeout or a lost connection; those give 503 (D44), which the
  store-down mode checks. The scenario is kept as `@wip`.
- **Out of scope by a decision:** none of the live criteria. The deprecated-reference criterion
  SIMPL-18184/AC2 is tested as a warning (201 with `deprecatedVocabularyReference`), because D11 and D47
  turn the error into a warning; the story text says error.

## 2. Decisions

| Decision | Scenarios |
|---|---|
| D1 | create_vocabulary.feature: A CPSV-like vocabulary can be uploaded and found in the list (SIMPL-393 met by the generic upload) (API) |
| D2 | none |
| D3 | create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>create_vocabulary.feature: The vocabulary status can be changed to DEPRECATED and back to ACTIVE (API)<br>search.feature: A latest result carries the vocabulary status, a versioned result the version status (API)<br>validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API)<br>validation.feature: A reference to a version of a DEPRECATED vocabulary warns without recommending a version (API)<br>versions.feature: The vocabulary shows the latest version's number and description, and keeps its status (API)<br>and 4 more |
| D4 | create_vocabulary.feature: The vocabulary status can be changed to DEPRECATED and back to ACTIVE (API)<br>validation.feature: A reference to a deprecated version with no ACTIVE version left gives the first sentence only (API)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API)<br>versions.feature: Every older ACTIVE version is deprecated, also after a manual reactivation (API)<br>versions.feature: Five overlapping new versions are serialised and all succeed (API)<br>versions.feature: The status of one version is changed by hand and nothing else changes (API)<br>and 8 more |
| D5 | create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>search.feature: The internal search resolves latest and versioned references in request order (API)<br>ui_list.feature: The list shows every vocabulary with its metadata and a row actions menu (UI)<br>validation.feature: References to ACTIVE versions give no deprecated-version warning (API)<br>validation.feature: A reference without a version resolves to the latest version, whatever its status (API)<br>versions.feature: The vocabulary shows the latest version's number and description, and keeps its status (API)<br>and 3 more |
| D6 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API) |
| D7 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API)<br>versions.feature: A version form field is rejected and no version is created (API) |
| D8 | access_control.feature: A viewer's write changes nothing (API)<br>create_vocabulary.feature: A request without the required field <field> is rejected and nothing is stored (API)<br>create_vocabulary.feature: A name that breaks the name rules is rejected (<case>) (API)<br>create_vocabulary.feature: Two overlapping creations with the same name give one 201 and one 409 (API)<br>validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: A new version with a syntax error is rejected and nothing changes (API)<br>and 4 more |
| D9 | create_vocabulary.feature: The field checks come before content validation (API)<br>external_vocabularies.feature: Registration with invalid Turtle is rejected with one syntax issue (API)<br>ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI)<br>validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: A new version with a syntax error is rejected and nothing changes (API)<br>validation.feature: Valid content passes both steps and the response confirms creation without errors (API)<br>and 11 more |
| D10 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: Valid content passes both steps and the response confirms creation without errors (API)<br>validation.feature: S1 a literal as the object of an object property is a validation error (API)<br>validation.feature: All semantic errors are returned in one response (API)<br>validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API)<br>and 3 more |
| D11 | ui_forms.feature: A reference to a deprecated version is shown in the warnings panel after creation (UI)<br>validation.feature: References to ACTIVE versions give no deprecated-version warning (API)<br>validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API)<br>validation.feature: A reference to a version of a DEPRECATED vocabulary warns without recommending a version (API)<br>validation.feature: A reference to a deprecated version with no ACTIVE version left gives the first sentence only (API)<br>validation.feature: A reference without a version resolves to the latest version, whatever its status (API)<br>and 1 more |
| D12 | external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API)<br>validation.feature: S7 a reference to a version that does not exist blocks a new version (API)<br>validation.feature: S7 a reference without a version to a vocabulary that does not exist blocks creation (API)<br>validation.feature: A content namespace with a name that breaks the name pattern is external (API)<br>validation.feature: References to ACTIVE versions give no deprecated-version warning (API)<br>validation.feature: A reference without a version resolves to the latest version, whatever its status (API)<br>and 7 more |
| D13 | ui_forms.feature: A chosen file is checked for syntax in the browser (UI)<br>ui_forms.feature: A syntax error in the chosen file shows its line and blocks submit (UI) |
| D14 | ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI)<br>ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI)<br>ui_forms.feature: A new version is created from the form and the confirmation names its number (UI)<br>ui_forms.feature: A reference to a deprecated version is shown in the warnings panel after creation (UI)<br>validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: Valid content passes both steps and the response confirms creation without errors (API) |
| D15 | create_vocabulary.feature: A duplicate name is rejected with 409 (API)<br>create_vocabulary.feature: A duplicate name with invalid content gives 409, not a validation error (API)<br>create_vocabulary.feature: A create request that is not multipart/form-data is rejected with 415 (API)<br>create_vocabulary.feature: A file of exactly 5 MB is accepted within 30 seconds (API)<br>create_vocabulary.feature: A file of 5 MB plus one byte is rejected with 413 (API)<br>create_vocabulary.feature: The size check comes before the field checks (API)<br>and 8 more |
| D16 | access_control.feature: A request without a token is rejected with 401 on <operation> (API)<br>access_control.feature: A user with neither role gets 403 on <operation> (API)<br>access_control.feature: A viewer gets 403 on the write operation <operation> (API)<br>access_control.feature: A viewer's write changes nothing (API)<br>access_control.feature: A viewer may use the read operation <operation> (API)<br>access_control.feature: An admin may use every operation (<operation>) (API)<br>and 5 more |
| D17 | access_control.feature: A tampered token is rejected on a write and nothing is stored (API)<br>access_control.feature: A request without a token is rejected with 401 on <operation> (API)<br>access_control.feature: A token that is not valid is rejected with 401 (<caller>) (API)<br>access_control.feature: A user with neither role gets 403 on <operation> (API)<br>access_control.feature: A viewer gets 403 on the write operation <operation> (API)<br>access_control.feature: Authentication is checked before the body (no token and an unsupported body) (API)<br>and 5 more |
| D18 | ui_list.feature: An admin sees the create, update and status actions (UI)<br>ui_list.feature: A viewer does not see the restricted actions and a direct form URL is refused (UI)<br>ui_list.feature: A user without a token is sent to the login page (UI)<br>ui_list.feature: Any 403 from the API shows the not-authorized message (UI) |
| D19 | ui_list.feature: The name and status filters narrow the list (UI)<br>vocabulary_list.feature: The name filter is a case-insensitive contains match (API)<br>vocabulary_list.feature: A name filter outside the relaxed rule is rejected (<case>) (API)<br>vocabulary_list.feature: A one-character lower-case name filter is accepted (API) |
| D20 | ui_list.feature: The creation date filter narrows the list (UI)<br>vocabulary_list.feature: The creation date filter matches a whole UTC day or an exact second (API)<br>vocabulary_list.feature: The last update filter matches a whole UTC day or an exact second (API)<br>vocabulary_list.feature: An unsupported or invalid filter or sort is rejected (<query>) (API) |
| D21 | external_vocabularies.feature: The external vocabulary list takes no filter or sort parameter (<query>) (API)<br>versions.feature: The version history rejects an invalid query (<query>) (API)<br>vocabulary_list.feature: Wrong paging parameters are rejected (<query>) (API)<br>vocabulary_list.feature: An unsupported or invalid filter or sort is rejected (<query>) (API)<br>vocabulary_list.feature: Exactly one issue names an unknown query parameter (API)<br>vocabulary_list.feature: An unknown query parameter on a GET of one vocabulary is rejected (API) |
| D22 | external_vocabularies.feature: The external vocabulary list is paged and ordered by name (API)<br>external_vocabularies.feature: The external vocabulary list pages with 20 items by default (API)<br>versions.feature: The version history is paged and keeps the newest-first order (API)<br>vocabulary_list.feature: The list returns a page of vocabularies with their metadata (API)<br>vocabulary_list.feature: Wrong paging parameters are rejected (<query>) (API)<br>vocabulary_list.feature: A page size of 100 is accepted (API)<br>and 3 more |
| D23 | ui_list.feature: A click on a sortable column sorts it; a click on the active sort column reverses it (UI)<br>versions.feature: The version history lists all versions newest first with their metadata (API)<br>versions.feature: The version history is paged and keeps the newest-first order (API)<br>versions.feature: The version history sorts on <sort> with ties broken by version descending (API)<br>versions.feature: The version history rejects an invalid query (<query>) (API)<br>vocabulary_list.feature: Paging works together with sorting and keeps the order (API)<br>and 4 more |
| D24 | create_vocabulary.feature: A request without the required field <field> is rejected and nothing is stored (API)<br>create_vocabulary.feature: A name that breaks the name rules is rejected (<case>) (API)<br>ui_forms.feature: An unexpected error on version creation advises to retry or contact support (UI)<br>ui_list.feature: A load error of the list is shown with the problem detail (UI)<br>ui_list.feature: A status change of a vocabulary that is no longer available (UI)<br>ui_list.feature: A failed status change keeps the status in the UI (UI)<br>and 9 more |
| D25 | create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>search.feature: The internal search resolves latest and versioned references in request order (API)<br>search.feature: A latest result carries the vocabulary status, a versioned result the version status (API)<br>versions.feature: The vocabulary shows the latest version's number and description, and keeps its status (API)<br>versions.feature: A manual version status change updates the vocabulary's last update date (API)<br>vocabulary_list.feature: The list returns a page of vocabularies with their metadata (API)<br>and 2 more |
| D26 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API)<br>versions.feature: The full metadata of a version is returned, with the latest version link (API)<br>versions.feature: The version history lists all versions newest first with their metadata (API) |
| D27 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>external_vocabularies.feature: An external vocabulary is registered and returned as sent (API)<br>external_vocabularies.feature: The external vocabulary is returned with all its fields (API)<br>ui_details.feature: Opening a deprecated version shows all its metadata (UI)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API)<br>and 1 more |
| D28 | create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>versions.feature: The vocabulary shows the latest version's number and description, and keeps its status (API) |
| D29 | external_vocabularies.feature: The content of an external vocabulary is downloaded by name (API)<br>protocol.feature: Every response with a body carries a Content-Type (API)<br>ui_details.feature: A selected version is downloaded as its Turtle file (UI)<br>ui_list.feature: The download action downloads the latest version and the details show its metadata (UI)<br>validation.feature: Relative IRIs resolve against the content URL of the new version (API)<br>versions.feature: The full metadata of a version is returned, with the latest version link (API)<br>and 2 more |
| D30 | external_vocabularies.feature: An external vocabulary is registered and returned as sent (API)<br>external_vocabularies.feature: The content of an external vocabulary is found by namespace or alias URI (API)<br>external_vocabularies.feature: A duplicate external name is rejected with 409, also in another case (API)<br>external_vocabularies.feature: A namespace or alias that is already registered is rejected with 409 (API)<br>external_vocabularies.feature: Registration with invalid Turtle is rejected with one syntax issue (API)<br>external_vocabularies.feature: Registration runs no semantic validation and returns no warnings (API)<br>and 2 more |
| D31 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>ui_list.feature: The list shows every vocabulary with its metadata and a row actions menu (UI) |
| D32 | ui_forms.feature: The create form has name, description and file, and no version or change log field (UI)<br>ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI) |
| D33 | ui_forms.feature: The update form is pre-filled with the latest description and content (UI)<br>ui_forms.feature: A new version is created from the form and the confirmation names its number (UI)<br>versions.feature: A change of the description only creates a new version (API) |
| D34 | ui_list.feature: The list shows every vocabulary with its metadata and a row actions menu (UI)<br>ui_list.feature: A load error of the list is shown with the problem detail (UI)<br>ui_list.feature: The list shows an empty state when nothing matches (UI) |
| D35 | ui_details.feature: The history action opens the details page with all versions newest first (UI)<br>ui_details.feature: A viewer sees deprecated versions labelled as deprecated (UI)<br>ui_details.feature: Opening a deprecated version shows all its metadata (UI)<br>ui_details.feature: The details page of an unknown vocabulary says so and links back (UI)<br>ui_details.feature: A load error of the history is shown with the problem detail (UI)<br>versions.feature: The version history lists all versions newest first with their metadata (API) |
| D36 | ui_details.feature: A deprecated version links to the latest ACTIVE version (UI)<br>ui_details.feature: A deprecated version without an ACTIVE version shows only the warning (UI)<br>ui_details.feature: A version of a DEPRECATED vocabulary shows no latest ACTIVE version link (UI)<br>ui_list.feature: A deprecated vocabulary shows a warning in the list and on its details page (UI) |
| D37 | create_vocabulary.feature: A request without the required field <field> is rejected and nothing is stored (API)<br>create_vocabulary.feature: A name that breaks the name rules is rejected (<case>) (API)<br>create_vocabulary.feature: A name of 2 characters is rejected (API)<br>create_vocabulary.feature: A name of exactly 3 characters passes the field checks (API)<br>create_vocabulary.feature: A name of exactly 64 characters is accepted (API)<br>create_vocabulary.feature: A name of 65 characters is rejected (API)<br>and 9 more |
| D38 | search.feature: The internal search resolves latest and versioned references in request order (API)<br>search.feature: Missing names and versions are left out and repeated references give one result (API)<br>search.feature: A latest result carries the vocabulary status, a versioned result the version status (API)<br>search.feature: The search by name is exact and no match gives an empty result (API)<br>search.feature: The internal search accepts 100 items (API)<br>search.feature: The internal search rejects 101 items (API)<br>and 10 more |
| D39 | none |
| D40 | access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| D41 | external_vocabularies.feature: An external vocabulary is registered and returned as sent (API)<br>external_vocabularies.feature: The external vocabulary list is paged and ordered by name (API)<br>external_vocabularies.feature: The external vocabulary list takes no filter or sort parameter (<query>) (API)<br>external_vocabularies.feature: The content of an external vocabulary is downloaded by name (API)<br>external_vocabularies.feature: The content of an external vocabulary is found by namespace or alias URI (API)<br>external_vocabularies.feature: Reads of an unknown external vocabulary give 404 (API)<br>and 12 more |
| D42 | external_vocabularies.feature: Deleting an external vocabulary removes it and leaves internal vocabularies alone (API)<br>external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API)<br>validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| D43 | create_vocabulary.feature: Two overlapping creations with the same name give one 201 and one 409 (API)<br>versions.feature: Five overlapping new versions are serialised and all succeed (API)<br>vocabulary_status.feature: Two overlapping identical status changes give 200 and 409 (API) |
| D44 | health.feature: The health operation answers UP without a token when the store is reachable (API)<br>not_runnable.feature: An internal error on the list gives 500 with the problem detail (wip)<br>not_runnable.feature: A token with an unknown key id while the identity provider is down fails closed with 503 (wip)<br>store_down.feature: The health operation reports DOWN (store-down)<br>store_down.feature: An operation that needs the store returns 503 with Retry-After (<operation>) (store-down)<br>store_down.feature: Authentication still works without the store (store-down)<br>and 2 more |
| D45 | search.feature: The internal search resolves latest and versioned references in request order (API) |
| D46 | access_control.feature: A viewer gets 403 on the write operation <operation> (API)<br>ui_forms.feature: The update form is pre-filled with the latest description and content (UI)<br>ui_forms.feature: A new version is created from the form and the confirmation names its number (UI)<br>versions.feature: Resubmitting the latest version unchanged is refused with 409 and deprecates nothing (API)<br>versions.feature: The identical-version check compares the trimmed description and ignores the change log (API)<br>versions.feature: A change of the description only creates a new version (API)<br>and 3 more |
| D47 | ui_details.feature: A version of a DEPRECATED vocabulary shows no latest ACTIVE version link (UI)<br>ui_forms.feature: A reference to a deprecated version is shown in the warnings panel after creation (UI)<br>validation.feature: S7 a reference to a version that does not exist blocks a new version (API)<br>validation.feature: S7 a reference without a version to a vocabulary that does not exist blocks creation (API)<br>validation.feature: S7 a version segment that is not a positive integer is a missing reference (API)<br>validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API)<br>and 4 more |
| D48 | create_vocabulary.feature: A file of 5 MB plus one byte is rejected with 413 (API)<br>protocol.feature: Client errors are 4xx problem details that reveal no implementation details (API) |
| D49 | external_vocabularies.feature: An external vocabulary name in a path that breaks the name rules gives 400 (API)<br>external_vocabularies.feature: A DELETE on an external name that breaks the name rules gives 400 (API)<br>versions.feature: A version path value that is not a positive integer gives 400 (<value>) (API)<br>versions.feature: A PATCH on a version path value that is not a positive integer gives 400 (API)<br>vocabulary_list.feature: A vocabulary name in a path that breaks the name rules gives 400 (<case>) (API)<br>vocabulary_status.feature: A PATCH on a vocabulary name that breaks the name rules gives 400 (API) |
| D50 | ui_list.feature: A click on a sortable column sorts it; a click on the active sort column reverses it (UI) |
| D51 | access_control.feature: Authentication is checked before the body (no token and an unsupported body) (API)<br>access_control.feature: The role is checked before the body (viewer and an invalid body) (API)<br>access_control.feature: The role is checked before a 404 on an unknown vocabulary (API)<br>create_vocabulary.feature: A duplicate name is rejected with 409 (API)<br>create_vocabulary.feature: A duplicate name with invalid content gives 409, not a validation error (API)<br>create_vocabulary.feature: A create request that is not multipart/form-data is rejected with 415 (API)<br>and 11 more |
| D52 | create_vocabulary.feature: An unknown form field is rejected and nothing is stored (API)<br>create_vocabulary.feature: A multipart part with an empty name is an unknown field (API)<br>create_vocabulary.feature: A query parameter on POST /vocabularies is rejected (API)<br>external_vocabularies.feature: A query parameter on DELETE is rejected and nothing is deleted (API)<br>external_vocabularies.feature: A query parameter on the external POST operations is rejected (API)<br>search.feature: A query parameter on a search is rejected (<query>) (API)<br>and 5 more |
| D53 | validation.feature: S4 two untagged preferred labels count as one language (API)<br>validation.feature: S4 one untagged and one tagged preferred label are not an error (API) |
| D54 | create_vocabulary.feature: A name that differs only in case from a stored name is rejected with 409 (API)<br>search.feature: The search by name is exact and no match gives an empty result (API)<br>vocabulary_list.feature: Paths match the stored name exactly (API) |
| D55 | external_vocabularies.feature: A namespace or alias that is already registered is rejected with 409 (API)<br>protocol.feature: An unknown path gives 401 without a token and 404 problem+json with one (API)<br>protocol.feature: A method the server does not know gives 405 before the token check (API)<br>protocol.feature: A method an operation path does not declare gives 405 (<method> <path>) (API)<br>versions.feature: A version that does not exist gives 404 (API)<br>versions.feature: Downloads of unknown vocabularies or versions give 404 (API)<br>and 1 more |
| D56 | external_vocabularies.feature: Registration needs only a name, a namespace URI and a file (API)<br>external_vocabularies.feature: A duplicate external name is rejected with 409, also in another case (API)<br>external_vocabularies.feature: An external vocabulary may share its name with an internal vocabulary (API)<br>external_vocabularies.feature: Registration accepts 20 alias URIs and returns them in the order sent (API)<br>external_vocabularies.feature: Registration rejects 21 alias URIs (API)<br>external_vocabularies.feature: Registration rejects invalid aliases (<case>) (API)<br>and 2 more |
| D57 | validation.feature: Relative IRIs resolve against the content URL of the new version (API) |
| D58 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API)<br>create_vocabulary.feature: The created vocabulary is returned with its metadata, version 1 and status ACTIVE (API)<br>external_vocabularies.feature: An external vocabulary is registered and returned as sent (API)<br>external_vocabularies.feature: The external vocabulary is returned with all its fields (API)<br>versions.feature: A new version gets the next number, becomes ACTIVE and deprecates the older version (API)<br>versions.feature: The full metadata of a version is returned, with the latest version link (API)<br>and 2 more |
| D59 | vocabulary_list.feature: Paging works together with sorting and keeps the order (API)<br>vocabulary_list.feature: A page beyond the last returns an empty page with the right totals (API) |
| D60 | ui_details.feature: The details page of an unknown vocabulary says so and links back (UI)<br>ui_forms.feature: The create form has name, description and file, and no version or change log field (UI)<br>ui_forms.feature: A chosen file is checked for syntax in the browser (UI)<br>ui_forms.feature: A syntax error in the chosen file shows its line and blocks submit (UI)<br>ui_forms.feature: An unexpected error on version creation advises to retry or contact support (UI)<br>ui_forms.feature: A 503 on version creation is shown like a 500 (UI)<br>and 4 more |
| D61 | none |
| E1 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API) |
| E2 | create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API) |
| E3 | none |
| E4 | none |
| D62 | ui_forms.feature: Field errors are shown under the fields and nothing is sent (UI)<br>ui_forms.feature: A file over 5 MB is refused in the browser (UI) |
| D63 | protocol.feature: The Accept check comes after authentication (API)<br>protocol.feature: An Accept header the operation cannot serve gives 406 (<path>) (API)<br>protocol.feature: Accepted Accept headers are served (<path>, <accept>) (API)<br>protocol.feature: The Accept check comes before the media type check (API) |
| D64 | ui_details.feature: A deprecated version links to the latest ACTIVE version (UI)<br>ui_details.feature: A deprecated version without an ACTIVE version shows only the warning (UI)<br>ui_details.feature: A version of a DEPRECATED vocabulary shows no latest ACTIVE version link (UI) |
| D65 | validation.feature: A syntax error is rejected with one issue holding its line and nothing is stored (API)<br>validation.feature: S1 a literal as the object of an object property is a validation error (API)<br>validation.feature: S2 a resource as the object of a datatype property is a validation error (API)<br>validation.feature: S4 two preferred labels with the same language tag are a validation error (API)<br>validation.feature: S4 two untagged preferred labels count as one language (API)<br>validation.feature: S5 a resource typed with two disjoint classes is a validation error (API)<br>and 7 more |
| D66 | external_vocabularies.feature: The content of an external vocabulary is found by namespace or alias URI (API)<br>external_vocabularies.feature: The content lookup of an unknown namespace gives 404 with the URI as sent (API)<br>external_vocabularies.feature: Reads of an unknown external vocabulary give 404 (API)<br>external_vocabularies.feature: A namespace or alias that is already registered is rejected with 409 (API)<br>versions.feature: A new version for a vocabulary that does not exist is rejected with 404 (API)<br>versions.feature: A version of a vocabulary that does not exist gives 404 (API)<br>and 3 more |
| D67 | protocol.feature: An unknown path gives 401 without a token and 404 problem+json with one (API)<br>protocol.feature: Actuator paths do not exist on the API port (API)<br>protocol.feature: A method the server does not know gives 405 before the token check (API)<br>protocol.feature: A method an operation path does not declare gives 405 (<method> <path>) (API)<br>protocol.feature: A path the firewall refuses gives 400 before the token check (<case>) (API) |
| D68 | search.feature: An invalid internal search body is rejected (<case>) (API)<br>search.feature: The external search rejects more than 100 URIs (<a> and <b>) (API)<br>search.feature: An external search body that fits no or several oneOf branches is rejected (<case>) (API)<br>search.feature: An empty URI array in an external search is rejected (<case>) (API) |
| D69 | validation.feature: S7 a version segment that is not a positive integer is a missing reference (API)<br>validation.feature: A content namespace with a name that breaks the name pattern is external (API) |
| D70 | create_vocabulary.feature: A multipart part with an empty name is an unknown field (API)<br>external_vocabularies.feature: The content of an external vocabulary is downloaded by name (API)<br>external_vocabularies.feature: Registration of a file with no statements is rejected (<case>) (API)<br>external_vocabularies.feature: Registration rejects invalid aliases (<case>) (API)<br>validation.feature: A 0-byte file passes the field check and fails validation with S6 (API)<br>versions.feature: The latest content is downloaded as the stored Turtle bytes (API)<br>and 1 more |
| D71 | create_vocabulary.feature: A file of exactly 5 MB is accepted within 30 seconds (API)<br>versions.feature: A new version with a file of exactly 5 MB is created within 30 seconds (API) |
| D72 | ui_details.feature: The history action opens the details page with all versions newest first (UI)<br>ui_details.feature: A load error of the history is shown with the problem detail (UI)<br>ui_forms.feature: A valid new vocabulary is created and the details page confirms version 1 (UI)<br>ui_list.feature: The list shows every vocabulary with its metadata and a row actions menu (UI)<br>ui_list.feature: A load error of the list is shown with the problem detail (UI)<br>ui_list.feature: The name and status filters narrow the list (UI)<br>and 6 more |
| D73 | external_vocabularies.feature: An external vocabulary is registered and returned as sent (API)<br>external_vocabularies.feature: The content lookup rejects a namespace that is not an absolute URI (API)<br>external_vocabularies.feature: Registration rejects invalid aliases (<case>) (API)<br>external_vocabularies.feature: Registration rejects a <field> that is not an absolute URI (API)<br>search.feature: The external search finds by namespace URI or alias URI, without trailing '#' or '/' (API) |
| D74 | ui_forms.feature: The create form has name, description and file, and no version or change log field (UI)<br>ui_forms.feature: The update form is pre-filled with the latest description and content (UI) |
| D75 | ui_details.feature: The history action opens the details page with all versions newest first (UI)<br>ui_details.feature: Opening a deprecated version shows all its metadata (UI)<br>ui_forms.feature: Semantic errors on submit are listed in the validation report and the form stays open (UI)<br>ui_forms.feature: A reference to a deprecated version is shown in the warnings panel after creation (UI)<br>ui_list.feature: The list shows every vocabulary with its metadata and a row actions menu (UI) |
| D76 | create_vocabulary.feature: A multipart request over 6 MB is rejected with 413 even when the file is small (API)<br>create_vocabulary.feature: A request just under 6 MB with a 5 MB file is not refused for size (API)<br>ui_forms.feature: A file over 5 MB is refused in the browser (UI) |
| D77 | protocol.feature: Fixed segments win over a name and a method they do not offer gives 405 (<path>) (API) |
| D78 | versions.feature: The version history is paged and keeps the newest-first order (API)<br>vocabulary_list.feature: Paging works together with sorting and keeps the order (API)<br>vocabulary_list.feature: A page beyond the last returns an empty page with the right totals (API) |
| D79 | create_vocabulary.feature: An unknown form field is rejected and nothing is stored (API)<br>protocol.feature: A method an operation path does not declare gives 405 (<method> <path>) (API) |
| D80 | none |
| D81 | none |
| Q1 | create_vocabulary.feature: A file of exactly 5 MB is accepted within 30 seconds (API)<br>versions.feature: A new version with a file of exactly 5 MB is created within 30 seconds (API) |
| Q2 | create_vocabulary.feature: A name of 2 characters is rejected (API)<br>create_vocabulary.feature: A name of exactly 3 characters passes the field checks (API)<br>create_vocabulary.feature: A name of exactly 64 characters is accepted (API)<br>create_vocabulary.feature: A name of 65 characters is rejected (API)<br>create_vocabulary.feature: A description and a change log of 1000 characters are accepted (API)<br>create_vocabulary.feature: A <field> that breaks the length rules is rejected (<case>) (API)<br>and 16 more |
| Q3 | none |
| Q4 | none |
| Q5 | none |
| Q6 | ui_details.feature: The details page meets WCAG 2.1 AA (UI)<br>ui_forms.feature: Both form pages meet WCAG 2.1 AA (UI)<br>ui_list.feature: The list page meets WCAG 2.1 AA and is usable by keyboard (UI) |
| E5 | access_control.feature: A tampered token is rejected on a write and nothing is stored (API)<br>access_control.feature: A token that is not valid is rejected with 401 (<caller>) (API) |
| E6 | none |

### Decisions with no scenario, and why

| Decision | Why there is no scenario |
|---|---|
| D2 | CDM snapshot to work from. No observable behaviour. |
| D39 | Records that the decisions win over the OpenAPI file; the decided points are tested under their own decisions (D5, D7, D10, D17, D19, D21, D22, D27, D37, D38). |
| D61 | SIMPL-28680 leaves the inventory; nothing to test. |
| D80 | Logs never hold email or name. Logs are not visible through the API. |
| D81 | Accepted risk: one backend instance. Not observable from outside; D43 tests assume it. |
| E3 | Store is Fuseki. Not observable; the reset sentence is withdrawn by E4. |
| E4 | Shapes the whole suite: configuration only through environment variables, unique run id per scenario, no store reset, list checks through name filters. Not a behaviour of the component. |
| E6 | Reuse choices of the build. Not observable. |
| Q3 | Availability, recovery time and point, backup and restore round trip. Needs the deployment and its backup job; not reachable through the API. Step 70 or the build's own test must cover it (NFR-06, NFR-13). |
| Q4 | Design load. No Gherkin scenario; the load test (`load/locustfile.py`) runs at 20 users. See section 3. |
| Q5 | Coverage, Trivy and the bounded reference query are build-side measurements (step 50, 70), not API behaviour. |

Notes on partly covered decisions:
- D44: the identity-provider part (unknown key id while Keycloak is down gives 503) is a `@wip` scenario;
  D44 itself says unit tests cover it. Health DEGRADED (store up, Keycloak down) cannot be set up from the suite.
- D43: overlap between a version creation and a status PATCH has two valid outcomes; only the fixed cases are
  tested (five parallel versions, two parallel same-name creations, two identical PATCHes).
- D46: the gateway call-rate limit (OWA-API4) is outside this build by D46; not tested.
- D76: the 6 MB request cap is tested above the cap and just under it (5 MB file plus 0.7 MB field gives 400,
  not 413). A request of exactly 6 MB cannot be built from valid fields only.
- D71: the 30 s budget at the 5 MB limit is checked in two `@slow` API scenarios and in the load test.

## 3. Quality targets (NFR-13)

| Target | Checked by | Notes |
|---|---|---|
| Q1 reads p95 <= 300 ms | `load/locustfile.py`, tasks for every read operation (lists, details, versions, version, both downloads, both searches, external content, external list); `reports/load.json` per operation | Step 70 runs `./run.sh load`. |
| Q1 writes p95 <= 3 s up to 1 MB | Load tasks `POST /vocabularies [<=1 MB]` and `POST /vocabularies/{vocabularyName}/versions [<=1 MB]` (1 MB files) | |
| Q1 write at 5 MB <= 30 s (D71) | Load task `POST .../versions [5 MB]` (3 times per run) and the API scenarios "A file of exactly 5 MB is accepted within 30 seconds" and "A new version with a file of exactly 5 MB is created within 30 seconds" | |
| Q1 throughput 20 reads/s and 1 write/s, no errors | Load test: 18 reader users at 20 req/s together; one writer starts one write per second with an open arrival rate (a write does not wait for the previous one); the 5 MB writes run on their own user and vocabulary and are not counted in the throughput; `throughput` block and `error_rate` per operation | |
| Q2 limits (NFR-04) | Scenarios tagged `@Q2`: file 5 MB at the limit and plus one byte (D15), request over 6 MB (D76), pageSize 100 and 101 (D22), name 3/64 and 2/65 characters, description and change log 1000 and 1001 (D37), search 100 and 101 items, 100 and 101 URIs (D38), 20 and 21 aliases, external description 1000 and 1001 (D56) | Each limit gives its defined error. |
| Q3 availability and recovery | Not measured by this suite | Needs the deployment and backup tooling (see section 2). |
| Q4 design load | Load test runs 20 concurrent users on one backend. Before the measured run it seeds the full Q4 volume (1000 vocabularies with 20 versions each) in parallel (`LOAD_SEED_PARALLEL`, default 16), outside the measurement. The seed has a fixed id (`LOAD_SEED_ID`, default `q4seed`) and is reused by later runs, which only add what is missing. The measured run lasts `LOAD_DURATION` after the seed. Reads use only seeded vocabularies; writes go to per-run vocabularies, so no read downloads a 5 MB file. | `reports/load.json` reports `measured_at_q4_volume`; `meets_all_targets` is false when the seed is smaller than Q4, so a reduced seed is never reported as met. |
| Q5 maintainability | Not measured by this suite | Build-side (JaCoCo, Trivy). |
| Q6 accessibility (NFR-10) | UI scenarios tagged `@Q6` (axe on list, details and both forms; keyboard reach) | Browser steps come in step 60. |

## 4. Rules checked

| Rule | Scenarios |
|---|---|
|  | 84 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| #> | 1 scenarios, for example validation.feature: An unregistered external namespace gives a warning and the vocabulary is created (API) |
| ${CB}/content/${V:Alpha}#> | 1 scenarios, for example validation.feature: References to the vocabulary being versioned are ignored (API) |
| ${CB}/content/${V:Alpha}/> | 1 scenarios, for example validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API) |
| ${CB}/content/${V:Alpha}/versions/1#> | 6 scenarios, for example validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API) |
| ${CB}/content/${V:Alpha}/versions/5#> | 1 scenarios, for example validation.feature: S7 a reference without a version to a vocabulary that does not exist blocks creation (API) |
| ${CB}/content/${V:Alpha}/versions/9#> | 1 scenarios, for example validation.feature: References to the vocabulary being created are ignored (API) |
| ${CB}/content/${V:Alpha}/versions/abc#> | 1 scenarios, for example validation.feature: A content namespace with a name that breaks the name pattern is external (API) |
| ${CB}/content/${V:Beta}/versions/4#> | 1 scenarios, for example validation.feature: Relative IRIs resolve against the content URL of the new version (API) |
| ${CB}/content/${V:Ghost}#> | 1 scenarios, for example validation.feature: S7 a version segment that is not a positive integer is a missing reference (API) |
| ${CB}/content/lower${rid}#> | 1 scenarios, for example validation.feature: References to ACTIVE versions give no deprecated-version warning (API) |
| ${NS:Dc}#> | 4 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| ${NS:Dc}/> | 1 scenarios, for example validation.feature: Warning codes come from the closed set (API) |
| API-36 | 3 scenarios, for example create_vocabulary.feature: A valid vocabulary is created as version 1 and ACTIVE (API) |
| API-37 | 2 scenarios, for example protocol.feature: An Accept header the operation cannot serve gives 406 (<path>) (API) |
| API-38 | 5 scenarios, for example create_vocabulary.feature: A create request that is not multipart/form-data is rejected with 415 (API) |
| API-44 | 2 scenarios, for example protocol.feature: Client errors are 4xx problem details that reveal no implementation details (API) |
| API-46 | 5 scenarios, for example access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| API-47 | 3 scenarios, for example access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| ARCH-32 | 2 scenarios, for example access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| ARCH-35 | 1 scenarios, for example store_down.feature: Authentication still works without the store (store-down) |
| BLG-col-paging | 3 scenarios, for example versions.feature: The version history is paged and keeps the newest-first order (API) |
| BLG-err-problem | 4 scenarios, for example access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| BLG-err-sanit | 1 scenarios, for example protocol.feature: Client errors are 4xx problem details that reveal no implementation details (API) |
| BLG-hlth-res | 2 scenarios, for example health.feature: The health operation answers UP without a token when the store is reachable (API) |
| BLG-prb-type | 1 scenarios, for example access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| BLG-req-valid | 2 scenarios, for example create_vocabulary.feature: An unknown form field is rejected and nothing is stored (API) |
| MACH-08 | 2 scenarios, for example health.feature: The health operation answers UP without a token when the store is reachable (API) |
| NFR-03 | 1 scenarios, for example create_vocabulary.feature: A file of exactly 5 MB is accepted within 30 seconds (API) |
| NFR-04 | 22 scenarios, for example create_vocabulary.feature: A name of 2 characters is rejected (API) |
| NFR-07 | 3 scenarios, for example health.feature: The health operation answers UP without a token when the store is reachable (API) |
| NFR-10 | 3 scenarios, for example ui_details.feature: The details page meets WCAG 2.1 AA (UI) |
| OWASP-02 | 8 scenarios, for example access_control.feature: A request without a token is rejected with 401 on <operation> (API) |
| OWASP-08 | 3 scenarios, for example access_control.feature: A tampered token is rejected on a write and nothing is stored (API) |
| SEC-05 | 4 scenarios, for example access_control.feature: A user with neither role gets 403 on <operation> (API) |
| c: | 5 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| calias: | 1 scenarios, for example validation.feature: Warning codes come from the closed set (API) |
| dd: | 1 scenarios, for example validation.feature: References to ACTIVE versions give no deprecated-version warning (API) |
| df: | 1 scenarios, for example validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| dfs: | 1 scenarios, for example validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| elf: | 1 scenarios, for example validation.feature: Relative IRIs resolve against the content URL of the new version (API) |
| host: | 1 scenarios, for example validation.feature: S7 a version segment that is not a positive integer is a missing reference (API) |
| http://alias.example.org/${rid}/dc#> | 1 scenarios, for example validation.feature: Warning codes come from the closed set (API) |
| http://example.org/${rid}/alpha#> | 4 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| http://example.org/${rid}/alpha3#> | 1 scenarios, for example validation.feature: References to the vocabulary being created are ignored (API) |
| http://example.org/${rid}/beta#> | 11 scenarios, for example validation.feature: S7 a version segment that is not a positive integer is a missing reference (API) |
| http://example.org/${rid}/gamma#> | 3 scenarios, for example validation.feature: S7 a reference without a version to a vocabulary that does not exist blocks creation (API) |
| http://example.org/${rid}/multi#> | 1 scenarios, for example validation.feature: S7 a reference to a version that does not exist blocks a new version (API) |
| http://example.org/${rid}/ok#> | 1 scenarios, for example validation.feature: Content with valid syntax goes on to semantic validation (API) |
| http://example.org/${rid}/s1#> | 1 scenarios, for example validation.feature: S2 a resource as the object of a datatype property is a validation error (API) |
| http://example.org/${rid}/s2#> | 1 scenarios, for example validation.feature: S4 two preferred labels with the same language tag are a validation error (API) |
| http://example.org/${rid}/s4#> | 3 scenarios, for example validation.feature: S4 two untagged preferred labels count as one language (API) |
| http://example.org/${rid}/s5#> | 1 scenarios, for example validation.feature: S6 content with no triples is a validation error (API) |
| http://unregistered.example.org/${rid}/ns#> | 1 scenarios, for example validation.feature: W3C core namespaces and the own namespace give no warning (API) |
| http://unregistered.example.org/${rid}/other/> | 1 scenarios, for example validation.feature: After a syntax error semantic validation does not run (API) |
| http://www.w3.org/1999/02/22-rdf-syntax-ns#> | 1 scenarios, for example validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| http://www.w3.org/2000/01/rdf-schema#> | 1 scenarios, for example validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| http://www.w3.org/2001/XMLSchema#> | 1 scenarios, for example validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| http://www.w3.org/2002/07/owl#> | 5 scenarios, for example validation.feature: S2 a resource as the object of a datatype property is a validation error (API) |
| http://www.w3.org/2004/02/skos/core#> | 25 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| kos: | 25 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| ld: | 1 scenarios, for example validation.feature: References to the vocabulary being created are ignored (API) |
| lpha: | 8 scenarios, for example validation.feature: S7 a reference without a version to a vocabulary that does not exist blocks creation (API) |
| lphalatest: | 1 scenarios, for example validation.feature: A reference to a deprecated version warns and recommends the latest ACTIVE version (API) |
| nk: | 2 scenarios, for example validation.feature: W3C core namespaces and the own namespace give no warning (API) |
| prefix | 83 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |
| sd: | 1 scenarios, for example validation.feature: A registered external namespace or alias gives no warning, compared without trailing '#' or '/' (API) |
| uture: | 1 scenarios, for example validation.feature: References to the vocabulary being created are ignored (API) |
| wl: | 5 scenarios, for example validation.feature: S2 a resource as the object of a datatype property is a validation error (API) |
| x: | 27 scenarios, for example external_vocabularies.feature: Registering an external namespace clears the warning and removing it brings it back (API) |



E5 (audience and issuer): the tampered and unsigned token scenarios show only the signature check. A token
with a valid signature but without the audience `vocabulary-manager-be`, or from another issuer, needs a
second Keycloak client or realm. The build profile names only the test client `vocabulary-tests`, which
carries the audience mapper (E5), so the suite cannot obtain such a token. Not tested; the code writer's own
tests should cover it.

Rules that a black-box test can observe but this suite does not check:
- OWA-API4 (call-rate limit): applied by the Tier 1 gateway in production, not in this build (D46).
- API-25 (HTTPS only): the local stack of the build profile is plain HTTP.
- API-39 (`Vary: Accept`): no operation negotiates between several success media types; each answers one type.
- NFR-07 graceful shutdown: needs control over the process; only the health part is tested.

## 5. Assumptions where the decisions leave room

- The top-level `type` of every 400 is `urn:problem-type:belgif:badRequest` (OpenAPI examples, D48, D67).
- A field-rule 400 (D37, D56, D73) carries an issue with `in: body` and `name:` the field (D55 says tests check `in` and `name`).
- Absent and `null` are both accepted where a link or field must not be there (D22, D38).
- S1 location is checked by its prefix (subject and predicate IRIs); D65's "literal as written" does not fix quoting.
- Syntax location: `line 3` is checked, the column is optional (D60, D65).
- A 3-character name may already exist from an earlier run on the shared store; 201 and 409 both prove it passed the field checks (D51).
- `Content-Disposition` may quote the file name or not.
- The store-down mode expects 503 on the list, a read by name, a create, the internal search and the external list (all need the store, D44).
- Time comparisons wait 1.2 s first, because times are kept to the second (D58).
- A firewall-refused path (encoded slash, `%00`) gets 400 even without a token (D67).
- The tampered and unsigned tokens are built from a real admin token; both must give 401 (D17, OWASP-08).
- The external list check pages through the whole list (it has no filter, D41).

## 6. Points in the decisions found unclear

- SIMPL-18184/AC2 asks for an error; D11 and D47 make it a warning. Tested as a warning.
- D70 says "'a,b' is one URI and fails D73"; an absolute URI with a comma in the path is valid RFC 3986, so the test sends the literal `a,b` (no scheme).
- D76 cannot be tested at exactly 6 MB with valid fields only (unknown fields would give 400); tested above and just under.
- D52 and D68 do not fix the issue name of an unknown property inside a search item (`latest` or `items[0].latest`); both are accepted.
- D38 and D68 do not fix whether an empty `namespaceUris` or `aliasUris` array is a "no oneOf branch" error (`name: body`) or an array error; only 400 and `in: body` are checked.
- D38 says searches carry no links; it does not say whether `page`, `pageSize` and `total` may be absent. They are checked as given (1, 100, count).

## 7. UI automation (step 60)

Run of 19 Nov 2026 against the build at `UI_URL` (http://localhost:4433): 41 `@ui` scenarios automated,
41 passed, 0 failed, 0 `@manual`. Each scenario opens a new browser context (no stored session) and signs in
on the Keycloak login page.

Accessibility (Q6, `@a11y`): axe-core with the WCAG 2.0 and 2.1 A and AA rules on the list, details, create
form and new version form pages. 0 violations on all four. The keyboard check on the list page reached every
visible action with Tab. Results per page are in `reports/a11y/`.

Elements are found only by the roles, labels, column names and texts of D18, D31, D35, D36, D60, D72, D74 and
D75. Routes come from D31 and D74; API stubs (D24, E3) match the API path by its end and only on fetch or
XHR calls, so no proxy path of the build is assumed.

Readings where the decisions leave the form open (no scenario changed):

- 'the button X' accepts a button or a link. D18 and D60 name 'New Vocabulary +' and 'Update' as actions
  without a role. 'Submit' and 'Cancel' are checked as buttons (D60).
- 'I open the version n' clicks the control in that row's 'Version' cell, or the cell. D35 says selecting a
  version opens the panel, not how it is selected.
- 'the link ... points to version n' follows the link and expects the region 'Version n' (D36, D75) on the
  details page of the vocabulary.
- A field error (D37, D76) is the field's accessible description or error message (aria-describedby or
  aria-errormessage), or aria-invalid. D37 says "under the field" and gives no role.
- Header and cell texts leave out parts hidden from assistive technology (the sort arrow).
- The sign-in uses Keycloak's own login page labels ('Username or email', 'Password', 'Sign In'); they come
  from the identity provider of E1, not from the build.
- The menu item 'Vocabularies' (D60) is looked for inside the navigation landmark, as a link, menu item or
  button.

Differences between the build and the specs or decisions: none found in this run.

Questions for the decision owners (would make the UI suite stricter):

1. D18, D60: should 'New Vocabulary +' and 'Update' be buttons or links?
2. D35: how is a version selected to open its panel (the version number as a control, or a row action)?
3. D36: what does the 'Latest ACTIVE version' link open (the panel of that version, or a route)?
4. D37: how is a field error tied to its field (aria-describedby, aria-errormessage, aria-invalid)?
