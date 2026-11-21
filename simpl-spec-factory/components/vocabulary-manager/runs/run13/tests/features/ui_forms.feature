Feature: UI create and new version forms
  Create form /vocabularies/new, new version form /vocabularies/<Name>/versions/new (D74).
  Upload check in the browser D13, D60, D62, D76; field rules D37, D101, D141; report and
  warnings D14, D90, D109; after success D72, D113; new version form D33, D46; errors D161.
  Gherkin only; step 60 adds the browser steps.

  @ui @AC_SIMPL-18169_AC1 @D32 @D60 @D74 @D141
  Scenario: The create form has the required fields and no version or change log field
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I click the button "New Vocabulary +"
    Then I am on the UI page "/vocabularies/new"
    And I see the field "Name"
    And I see the field "Description"
    And I see the field "Vocabulary file"
    And the field "Name" is required
    And the field "Description" is required
    And the field "Vocabulary file" is required
    And I do not see the field "Change log"
    And I do not see the field "Version"
    And I see the button "Submit"
    And I see the button "Cancel"

  @ui @AC_SIMPL-18169_AC2 @D37 @D62 @D101 @negative
  Scenario: Submit with empty fields shows every field error and sends nothing
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I click the button "Submit"
    Then the field "Name" shows an error
    And the field "Description" shows an error
    And the field "Vocabulary file" shows an error
    And the focus is on the field "Name"
    And no request was sent to the API

  @ui @AC_SIMPL-18169_AC2 @D37 @D62 @negative
  Scenario: A name that breaks the name rule shows a field error and does not disable Submit
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "lower case name"
    And I fill in "Description" with "Some text."
    And I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    Then the field "Name" shows an error
    And the button "Submit" is enabled
    When I click the button "Submit"
    Then no request was sent to the API
    And the focus is on the field "Name"

  @ui @AC_SIMPL-18169_AC2 @D37 @D161 @D15 @negative
  Scenario: A duplicate name shows the API detail above the form and keeps the values
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Another one."
    And I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see an alert containing "Vocabulary with name '{Alpha}' already exists."
    And the field "Name" contains "{Alpha}"
    And the field "Description" contains "Another one."

  @ui @AC_SIMPL-18169_AC2 @D76 @D62 @Q2 @negative
  Scenario: A file over 5 MB is refused in the browser and Submit is disabled
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I choose a valid Turtle file of 5242881 bytes for "Vocabulary file"
    Then the field "Vocabulary file" shows the error "The file is larger than 5 MB."
    And the button "Submit" is disabled

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D13 @D60 @D62 @negative
  Scenario: A syntax error is shown with its line when the file is chosen and blocks Submit
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Some text."
    And I choose the fixture file "syntax-error-line3.ttl" for "Vocabulary file"
    Then I see the text "line 3"
    And the button "Submit" is disabled

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @AC_SIMPL-18193_AC1 @D13
  Scenario: A valid file passes the upload check
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    Then I see the text "Syntax check passed."
    And the button "Submit" is enabled

  @ui @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D90 @D109 @D75 @negative
  Scenario: Semantic errors on submit are listed in the validation report and the form stays open
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Some text."
    And I choose the fixture file "ui-two-semantic-errors.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then the region "Validation report" contains "2 errors found"
    And the region "Validation report" lists 2 issues
    And the region "Validation report" contains "Duplicate preferred label for the same language"
    And the region "Validation report" contains "Resource is an instance of disjoint classes"
    And the region "Validation report" contains "language 'en'"
    And I am on the UI page "/vocabularies/new"
    And the field "Name" contains "{Alpha}"

  @ui @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D14 @D8
  Scenario: After fixing the errors the vocabulary can be created
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Some text."
    And I choose the fixture file "ui-two-semantic-errors.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then the region "Validation report" contains "2 errors found"
    When I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see a status message containing "Version 1 of {Alpha} was created."

  @ui @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC4 @AC_SIMPL-18182_AC2 @AC_SIMPL-18193_AC1 @AC_SIMPL-18186_AC1 @D14 @D32 @D72
  Scenario: A valid vocabulary is created and its details page opens with the notification
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Created in the UI."
    And I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I am on the UI page "/vocabularies/{Alpha}"
    And I see a status message containing "Version 1 of {Alpha} was created."
    And I do not see the region "Validation warnings"
    And the row of "1" shows "Active" in the column "Status"
    And the row of "1" shows "Initial version." in the column "Change log"

  @ui @D14 @D109 @D10
  Scenario: Warnings of a successful creation are shown on the details page
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Uses an unregistered namespace."
    And I choose the fixture file "ui-external-reference.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see a status message containing "Version 1 of {Alpha} was created."
    And the region "Validation warnings" contains "1 warning found"
    And the region "Validation warnings" contains "prefix 'ext' -> "

  @ui @AC_SIMPL-18172_AC1 @D33 @D46 @D74 @D141
  Scenario: The update form is pre-filled with the latest description and content
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "Update"
    Then I am on the UI page "/vocabularies/{Alpha}/versions/new"
    And the field "Description" contains "Description of {Alpha} version 2."
    And the field "Vocabulary file" shows the file "{Alpha}.ttl (current content)"
    And the field "Change log" is empty
    And the field "Change log" is required
    And I see the text "Submitting creates version 3 and deprecates every older ACTIVE version."
    And I do not see the field "Version"

  @ui @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D14 @D33 @D113
  Scenario: A new version is created from the update form and shown with its number
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/{Alpha}/versions/new"
    And I fill in "Change log" with "Added concepts."
    And I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I am on the UI page "/vocabularies/{Alpha}" or "/vocabularies/{Alpha}/versions/2"
    And I see a status message containing "Version 2 of {Alpha} was created."
    And the region "Version 2" contains "Active"
    And the row of version 2 has aria-current "true"
    And the row of "1" shows "Deprecated" in the column "Status"

  @ui @D33 @D46
  Scenario: A description-only change creates a new version with the current content
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/{Alpha}/versions/new"
    And I fill in "Description" with "Only the description changed."
    And I fill in "Change log" with "Description update."
    And I click the button "Submit"
    Then I see a status message containing "Version 2 of {Alpha} was created."
    And version 2 of "Alpha" exists in the API
    And version 2 of "Alpha" has the same content as version 1 in the API

  @ui @D46 @D161 @negative
  Scenario: An unchanged resubmission shows the 409 detail above the form
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/{Alpha}/versions/new"
    And I fill in "Change log" with "Nothing changed."
    And I click the button "Submit"
    Then I see an alert containing "Version 1 of vocabulary '{Alpha}' already has this content and description."
    And the field "Change log" contains "Nothing changed."
    And version 2 of "Alpha" does not exist in the API

  @ui @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: A reference to a deprecated version shows a warning that names it and recommends the latest ACTIVE version
    Given a vocabulary "Ref" exists with 2 versions
    And a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies/{Alpha}/versions/new"
    And I fill in "Change log" with "References an old version."
    And I choose the fixture file "ui-deprecated-reference.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see a status message containing "Version 2 of {Alpha} was created."
    And the region "Validation warnings" contains "Referenced internal vocabulary '{Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  @ui @AC_SIMPL-18172_AC3 @D24 @D60 @D161 @negative
  Scenario Outline: An unexpected error during version creation advises to retry or contact support: <status>
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    And the API answers POST "/vocabularies/{Alpha}/versions" with the status <status>
    When I open the UI page "/vocabularies/{Alpha}/versions/new"
    And I fill in "Change log" with "Will fail."
    And I click the button "Submit"
    Then I see an alert containing "Something went wrong. Retry later or contact support."
    And I am on the UI page "/vocabularies/{Alpha}/versions/new"
    And version 2 of "Alpha" does not exist in the API

    Examples:
      | status |
      | 500    |
      | 503    |

  @ui @AC_SIMPL-18169_AC2 @D15 @D76 @D37 @negative
  Scenario: A 413 from the server shows its detail above the form
    Given I am signed in to the UI as "ga-admin"
    And the API answers POST "/vocabularies" with the status 413 and the detail "Stubbed upload too large {tok}."
    When I open the UI page "/vocabularies/new"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Some text."
    And I choose the fixture file "cpsv-like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see an alert containing "Stubbed upload too large {tok}."
    And I am on the UI page "/vocabularies/new"
