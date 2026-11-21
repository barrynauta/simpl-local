Feature: Create and update forms and the vocabulary details page in the UI
  Gherkin only; step 60 adds the browser steps (D13, D14, D32, D33, D35, D36, D46, D60, D62, D64, D72, D74, D76, D90, D101, D109, D113, D118, D161, D163).

  @ui @AC_SIMPL-18169_AC1 @D32 @D60 @D74 @D141
  Scenario: The create form has name, description and file, and no version or change log
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click the button "New Vocabulary +"
    Then I am on the page "/vocabularies/new"
    And the field "Name" is empty
    And the field "Description" is empty
    And the form has no field "Change log"
    And the form has no field "Version"
    And I see the button "Submit"
    And I see the button "Cancel"

  @ui @AC_SIMPL-18169_AC2 @D37 @D62 @D101 @negative
  Scenario: Submit with empty fields shows every field error, sends nothing and focuses the first field
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I click the button "Submit"
    Then the field "Name" shows an error
    And the field "Description" shows an error
    And the field "Vocabulary file" shows an error
    And the focus is on the field "Name"
    And no request was sent to the API

  @ui @AC_SIMPL-18169_AC2 @D37 @D161 @negative
  Scenario: A duplicate name shows the API detail above the form and keeps the values
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "{{Health}}"
    And I fill in "Description" with "Duplicate"
    And I choose the file "valid.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see an alert containing "Vocabulary with name '{{Health}}' already exists."
    And the field "Name" contains "{{Health}}"

  @ui @AC_SIMPL-18169_AC2 @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @D13 @D60 @D62 @negative
  Scenario: A file with a syntax error is checked on upload and Submit stays disabled
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I choose the file "syntax-error.ttl" for "Vocabulary file"
    Then I see the text "line 3"
    And the button "Submit" is disabled

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @D13
  Scenario: A valid file passes the upload check
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I choose the file "valid.ttl" for "Vocabulary file"
    Then I see the text "Syntax check passed."
    And the button "Submit" is enabled

  @ui @AC_SIMPL-18169_AC2 @D76 @NFR-04 @negative
  Scenario: A file over 5 MB is refused in the browser
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I choose a file of 5242881 bytes for "Vocabulary file"
    Then the field "Vocabulary file" shows the error "The file is larger than 5 MB."
    And the button "Submit" is disabled

  @ui @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18180_AC8 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D90 @D109 @negative
  Scenario: Semantic errors on submit are listed in the validation report and the form stays open
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "{{Health}}"
    And I fill in "Description" with "Has errors"
    And I choose the file "syntax-error-free-semantic-errors.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then the region "Validation report" contains "2 errors found"
    And the region "Validation report" contains "Property declared as both object and datatype property"
    And the region "Validation report" contains "Duplicate preferred label for the same language"
    And I am on the page "/vocabularies/new"

  @ui @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @AC_SIMPL-18182_AC2 @AC_SIMPL-18180_AC4 @D6 @D14 @D72
  Scenario: A valid new vocabulary is created ACTIVE and its details page opens with the notification
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "{{Health}}"
    And I fill in "Description" with "Created in the UI"
    And I choose the file "valid.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I am on the page "/vocabularies/{{Health}}"
    And I see a status message "Version 1 of {{Health}} was created."
    And the row of version 1 shows "Active"
    And the row of version 1 shows "Initial version."

  @ui @D14 @D109
  Scenario: Warnings after a successful creation are shown in the warnings region
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "{{Health}}"
    And I fill in "Description" with "With a warning"
    And I choose the file "unregistered-reference.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then the region "Validation warnings" contains "1 warning found"

  @ui @AC_SIMPL-18172_AC1 @D33 @D46 @D74
  Scenario: The update form is pre-filled with the latest description and content
    Given a vocabulary "{{Health}}" exists with description "Current text"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "Update" in the actions menu of "{{Health}}"
    Then I am on the page "/vocabularies/{{Health}}/versions/new"
    And the field "Description" contains "Current text"
    And I see the text "{{Health}}.ttl (current content)"
    And the field "Change log" is empty
    And I see the text "Submitting creates version 2 and deprecates every older ACTIVE version."

  @ui @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D7 @D14 @D113
  Scenario: Submitting the update form creates the next version and deprecates the previous one
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Health}}/versions/new"
    And I fill in "Description" with "Second edition"
    And I fill in "Change log" with "Added terms."
    Then the form has no field "Version"
    When I click the button "Submit"
    Then I am on one of the pages "/vocabularies/{{Health}}" and "/vocabularies/{{Health}}/versions/2"
    And the row of version 2 is selected
    And the region "Version 2" contains "Second edition"
    And I see a status message "Version 2 of {{Health}} was created."
    And the row of version 2 shows "Active"
    And the row of version 1 shows "Deprecated"

  @ui @AC_SIMPL-18172_AC3 @D44 @D60 @negative
  Scenario Outline: An unexpected error on version creation advises to retry or contact support (<code>)
    Given a vocabulary "{{Health}}" exists
    And the API answers <code> to "POST" "/vocabularies/{{Health}}/versions"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Health}}/versions/new"
    And I fill in "Change log" with "Will fail."
    And I click the button "Submit"
    Then I see an alert containing "Something went wrong. Retry later or contact support."
    And vocabulary "{{Health}}" has 1 version

    Examples:
      | code |
      | 500  |
      | 503  |

  @ui @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: A deprecated reference in a new version shows a warning with the latest ACTIVE version
    Given a vocabulary "{{Base}}" exists
    And vocabulary "{{Base}}" has 1 more versions
    And a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Health}}/versions/new"
    And I choose the file "reference-base-v1.ttl" for "Vocabulary file"
    And I fill in "Change log" with "Uses base."
    And I click the button "Submit"
    Then the region "Validation warnings" contains "Referenced internal vocabulary '{{Base}}' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  @ui @D46 @D161 @negative
  Scenario: Resubmitting the unchanged latest version shows the conflict above the form
    Given a vocabulary "{{Health}}" exists with description "Same text"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Health}}/versions/new"
    And I fill in "Change log" with "No change."
    And I click the button "Submit"
    Then I see an alert containing "Version 1 of vocabulary '{{Health}}' already has this content and description."

  @ui @AC_SIMPL-18167_AC1 @D35 @D163
  Scenario: History opens the details page with the versions newest first
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 2 more versions
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "History" in the actions menu of "{{Health}}"
    Then I am on the page "/vocabularies/{{Health}}"
    And I see the text "{{Health}}"
    And I see a table with the column headers "Version, Status, Date created, Description, Change log, Actions"
    And the version list shows the versions in the order "3, 2, 1"

  @ui @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC2 @AC_SIMPL-18176_AC3 @D35 @D27
  Scenario: A viewer sees deprecated versions labelled and can open their metadata
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{Health}}"
    Then the row of version 1 shows "Deprecated"
    When I click the link "1"
    Then I am on the page "/vocabularies/{{Health}}/versions/1"
    And the region "Version 1" contains "Deprecated"
    And the region "Version 1" contains "{{adminEmail}}"
    And the region "Version 1" contains "Initial version."
    And the region "Version 1" contains "Description of {{Health}}"
    And the region "Version 1" contains text matching "\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC"

  @ui @AC_SIMPL-18452_AC1 @AC_SIMPL-18452_AC2 @D29 @D35
  Scenario: A selected version can be downloaded
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Health}}/versions/1"
    And I choose "Download" in the actions menu of version 1
    Then a file named "{{Health}}-v1.ttl" is downloaded with the content of version 1 of "{{Health}}"

  @ui @AC_SIMPL-18559_AC1 @D36 @D64 @D118
  Scenario: A deprecated version links to the latest ACTIVE version
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{Health}}/versions/1"
    Then the region "Version 1" contains "This version is deprecated."
    And the link "Latest ACTIVE version: 2" points to "/vocabularies/{{Health}}/versions/2"

  @ui @AC_SIMPL-18559_AC2 @D36
  Scenario: A deprecated version without an ACTIVE version shows only the warning
    Given a vocabulary "{{Health}}" exists
    And version 1 of vocabulary "{{Health}}" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{Health}}/versions/1"
    Then the region "Version 1" contains "This version is deprecated."
    And the region "Version 1" does not contain "Latest ACTIVE version"

  @ui @D64 @D36 @D115
  Scenario: Versions of a DEPRECATED vocabulary show the warning without a link, and the banner shows
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    And vocabulary "{{Health}}" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{Health}}/versions/1"
    Then I see a status message "This vocabulary is deprecated. Its use is discouraged."
    And the region "Version 1" contains "This version is deprecated."
    And the region "Version 1" does not contain "Latest ACTIVE version"

  @ui @D35 @D60 @negative
  Scenario: The details page of an unknown vocabulary shows not found with a link back
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Missing}}"
    Then I see the text "Vocabulary not found."
    And the link "Back to vocabulary list" points to "/vocabularies"

  @ui @D35 @D72 @negative
  Scenario: A load error of the history shows the decided text
    Given a vocabulary "{{Health}}" exists
    And the API answers 500 with detail "Stub detail {{run}}" to "GET" "/vocabularies/{{Health}}/versions"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/{{Health}}"
    Then there is exactly one alert
    And I see an alert containing "The version history could not be loaded."
    And I see an alert containing "Stub detail {{run}}"
    And I do not see the text "Something went wrong"
