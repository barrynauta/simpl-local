@ui
Feature: Create, new version, status change and access in the UI
  Gherkin only; step 60 adds the browser steps (D60, D72, D74, D75 give labels, texts and routes).

  @AC_SIMPL-18169_AC1 @D32 @D60 @D74
  Scenario: The creation form has Name, Description and Vocabulary file, and no version or change log
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user clicks the button "New Vocabulary +"
    Then the user is on the route "/vocabularies/new"
    And the user sees the fields "Name, Description, Vocabulary file"
    And the user sees no field "Change log" and no field "Version"
    And the user sees the buttons "Submit, Cancel"

  @AC_SIMPL-18169_AC2 @D37 @D62 @D101 @negative
  Scenario: Submit with empty fields shows every field error and sends nothing
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user clicks the button "Submit"
    Then the user sees an error under "Name", "Description" and "Vocabulary file"
    And no request was sent to the API
    And the focus is on the field "Name"

  @AC_SIMPL-18169_AC2 @D37 @negative
  Scenario: An invalid name shows a field error
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user types "lower-case" in the field "Name"
    And the user leaves the field "Name"
    Then the user sees an error under "Name"

  @AC_SIMPL-18169_AC2 @D37 @D15 @negative
  Scenario: A duplicate name shows the 409 detail above the form
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user fills the creation form with name "${A}", description "Dup" and the file "valid"
    And the user clicks the button "Submit"
    Then the user sees an alert above the form with the text "Vocabulary with name '${A}' already exists."

  @AC_SIMPL-18169_AC2 @D76 @D62 @negative
  Scenario: A file over 5 MB is refused in the browser
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user chooses a file of 5242881 bytes for "Vocabulary file"
    Then the user sees the error "The file is larger than 5 MB." under "Vocabulary file"
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @D13
  Scenario: A chosen file is syntax checked in the browser
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user chooses the file "valid" for "Vocabulary file"
    Then the user sees the text "Syntax check passed."
    And the button "Submit" is enabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @D13 @D60 @D62 @negative
  Scenario: A syntax error in the chosen file is shown with its line and blocks Submit
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user chooses the file "syntax_error" for "Vocabulary file"
    Then the user sees a syntax error that names line 3
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D90 @D109 @negative
  Scenario: Semantic errors on submit appear in the validation report and the form stays open
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user fills the creation form with name "${A}", description "Report" and the file "multiple_errors"
    And the user clicks the button "Submit"
    Then the user sees a region "Validation report" whose first line is an alert "3 errors found"
    And the region "Validation report" lists each issue with its detail and location
    And the user is on the route "/vocabularies/new"
    And the vocabulary list filtered by name "${A}" has 0 items

  @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC4 @AC_SIMPL-18182_AC2 @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @D14 @D72
  Scenario: A valid creation opens the details page with the success notification
    Given the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user fills the creation form with name "${A}", description "Created in the UI" and the file "valid"
    And the user clicks the button "Submit"
    Then the user is on the route "/vocabularies/${A}"
    And the user sees a status message "Version 1 of ${A} was created."
    And the version row "1" shows the status "Active"
    And the field "changelog" of "/vocabularies/${A}/versions/1" is "Initial version."

  @D14 @D109 @D90
  Scenario: Warnings of a creation appear in the warnings panel
    Given the file variable "ref" is "${extns:unregistered}#"
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/new"
    And the user fills the creation form with name "${A}", description "With warning" and the file "reference"
    And the user clicks the button "Submit"
    Then the user sees a region "Validation warnings" whose first line is a status "1 warning found"
    And the region "Validation warnings" lists the location "prefix 'r' -> '${extns:unregistered}#'"

  @AC_SIMPL-18172_AC1 @D33 @D46 @D74
  Scenario: Update opens the new version form pre-filled with the latest version
    Given a vocabulary "A" exists with 2 versions
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "Update" in the menu "Actions for ${A}"
    Then the user is on the route "/vocabularies/${A}/versions/new"
    And the field "Description" holds "Description of A version 2"
    And the field "Vocabulary file" shows "${A}.ttl (current content)"
    And the field "Change log" is empty and required
    And the user sees the text "Submitting creates version 3 and deprecates every older ACTIVE version."

  @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D14 @D113
  Scenario: Submitting the new version form creates the next version and deprecates the older one
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/${A}/versions/new"
    Then the user sees no field "Version"
    When the user types "New content" in the field "Change log"
    And the user chooses the file "valid" for "Vocabulary file"
    And the user clicks the button "Submit"
    Then the user is on the route "/vocabularies/${A}"
    And the user sees a status message "Version 2 of ${A} was created."
    And the user sees a region "Version 2"
    And the version row "2" shows the status "Active"
    And the version row "1" shows the status "Deprecated"

  @D33
  Scenario: A description-only change creates a new version with the current content
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/${A}/versions/new"
    And the user types " and more" at the end of the field "Description"
    And the user types "Description only" in the field "Change log"
    And the user clicks the button "Submit"
    Then the user sees a status message "Version 2 of ${A} was created."

  @AC_SIMPL-18172_AC3 @D24 @D60 @negative
  Scenario: An unexpected error on submit advises to retry or contact support
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    And the user's API answers "POST /vocabularies/${A}/versions" with 500 and detail "Boom"
    When the user opens the route "/vocabularies/${A}/versions/new"
    And the user types "Change" in the field "Change log"
    And the user clicks the button "Submit"
    Then the user sees an alert with the text "Something went wrong. Retry later or contact support." and "Boom"
    And vocabulary "A" has 1 version

  @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: A reference to a deprecated version shows a warning recommending the latest ACTIVE version
    Given a vocabulary "R" exists with 2 versions
    And a vocabulary "A" exists
    And the file variable "ref" is "${base}/content/${R}/versions/1#"
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies/${A}/versions/new"
    And the user types "Uses R" in the field "Change log"
    And the user chooses the file "reference" for "Vocabulary file"
    And the user clicks the button "Submit"
    Then the user sees a region "Validation warnings" listing "Referenced internal vocabulary '${R}' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  @AC_SIMPL-18174_AC1 @D60 @D72
  Scenario Outline: Change the vocabulary status with <action>
    Given a vocabulary "A" exists
    And vocabulary "A" has the status "<from>"
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "<action>" in the menu "Actions for ${A}"
    Then the user sees a status message "Status of ${A} changed to <label>."
    And the row "${A}" shows the status "<label>"
    And vocabulary "A" has the status "<to>"

    Examples:
      | from       | action    | to         | label      |
      | ACTIVE     | Deprecate | DEPRECATED | Deprecated |
      | DEPRECATED | Activate  | ACTIVE     | Active     |

  @AC_SIMPL-18174_AC2 @D24 @D72 @negative
  Scenario: A status change for a vocabulary that is no longer available
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    And the user's API answers "PATCH /vocabularies/${A}" with 404 and detail "Vocabulary with name '${A}' was not found."
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "Deprecate" in the menu "Actions for ${A}"
    Then the user sees one alert with the text "Vocabulary not found. The status was not changed."
    And the row "${A}" shows the status "Active"

  @AC_SIMPL-18174_AC3 @D24 @D72 @D114 @negative
  Scenario: A failed status change keeps the shown status
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    And the user's API answers "PATCH /vocabularies/${A}" with 500 and detail "Update failed"
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "Deprecate" in the menu "Actions for ${A}"
    Then the user sees one alert with the text "The vocabulary status could not be updated." followed by "Update failed"
    And the row "${A}" shows the status "Active"

  @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: An admin sees every restricted action
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    Then the user sees the button "New Vocabulary +"
    And the menu "Actions for ${A}" holds "Update" and "Deprecate"

  @AC_SIMPL-18195_AC2 @AC_SIMPL-18176_AC1 @D16 @D18 @negative
  Scenario: A viewer does not see the restricted actions and is refused on the form routes
    Given a vocabulary "A" exists
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    Then the user does not see the button "New Vocabulary +"
    And the menu "Actions for ${A}" holds neither "Update" nor "Deprecate"
    When the user opens the route "/vocabularies/new"
    Then the user sees the text "You are not authorized to perform this action."
    When the user opens the route "/vocabularies/${A}/versions/new"
    Then the user sees the text "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: A 403 from the API shows the authorization message
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    And the user's API answers "PATCH /vocabularies/${A}" with 403 and detail "Forbidden"
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "Deprecate" in the menu "Actions for ${A}"
    Then the user sees the text "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a token is sent to the Keycloak login page
    Given the user is not signed in to the UI
    When the user opens the route "/vocabularies"
    Then the user is on the Keycloak login page
