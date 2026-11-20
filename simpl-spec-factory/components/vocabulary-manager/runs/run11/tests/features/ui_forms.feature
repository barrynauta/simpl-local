@ui
Feature: Create, new version and validation feedback in the UI
  Create form at /vocabularies/new and new version form at /vocabularies/<Name>/versions/new (D74).
  Labels and texts from D13, D14, D33, D37, D46, D60, D62, D76, D90, D101, D109, D113, D141.

  @AC_SIMPL-18169_AC1 @D32 @D60 @D74 @D141
  Scenario: The create form has the required fields and no version or change log field
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list
    When I click the button "New Vocabulary +"
    Then I am on the page "/vocabularies/new"
    And I see the fields "Name", "Description" and "Vocabulary file", each marked required with a visible "*" and aria-required="true"
    And I see no field "Change log" and no field for a version
    And I see the buttons "Submit" and "Cancel"

  @AC_SIMPL-18169_AC2 @D37 @D62 @D101
  Scenario: Submit with empty fields shows every field error and sends nothing
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I click the button "Submit"
    Then I see an error under "Name", under "Description" and under "Vocabulary file"
    And the focus is on the field "Name"
    And no request was sent to POST /vocabularies

  @AC_SIMPL-18169_AC2 @D37
  Scenario: A name that breaks the rules shows a field error before submit
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I type "bad name" into the field "Name" and leave the field
    Then I see an error under "Name"
    And the button "Submit" is enabled

  @AC_SIMPL-18169_AC2 @D15 @D37
  Scenario: A duplicate name is reported above the form
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I fill in "Name" with "${Alpha}", "Description" with "Duplicate" and choose a valid Turtle file for "Vocabulary file"
    And I click the button "Submit"
    Then I see one element with role alert above the form
    And the form stays open

  @AC_SIMPL-18169_AC2 @D76 @D62
  Scenario: A file over 5 MB is refused in the browser and Submit is disabled
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I choose a Turtle file of 5242881 bytes for "Vocabulary file"
    Then I see the error "The file is larger than 5 MB." under "Vocabulary file"
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @AC_SIMPL-18193_AC1 @D13
  Scenario: A file with valid syntax passes the upload check
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I choose a valid Turtle file for "Vocabulary file"
    Then I see the text "Syntax check passed."
    And the button "Submit" is enabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @D13 @D60 @D62
  Scenario: A file with a syntax error is reported on upload with its line and Submit is disabled
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I choose the fixture "syntax-error.ttl" for "Vocabulary file"
    Then I see a syntax error that names line 3
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC4 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC2 @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @D14 @D32 @D72 @D90
  Scenario: Submitting a valid vocabulary creates version 1 and opens its details page
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I fill in "Name" with "${Alpha}", "Description" with "Created in the UI" and choose a valid Turtle file for "Vocabulary file"
    And I click the button "Submit"
    Then I am on the page "/vocabularies/${Alpha}"
    And I see a message with role status "Version 1 of ${Alpha} was created."
    And the row of version 1 shows the status "Active" and the change log "Initial version."

  @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D90 @D109 @D75
  Scenario: Semantic errors on submit are listed in the validation report and the form stays open
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I fill in "Name" with "${Alpha}", "Description" with "Bad semantics" and choose a Turtle file with the errors S1, S3 and S5 for "Vocabulary file"
    And I click the button "Submit"
    Then I see a region named "Validation report" above the form
    And its first line has role alert and the text "3 errors found"
    And the report lists the details "Literal value for object property", "Property declared as both object and datatype property" and "Resource is an instance of disjoint classes", each with its location
    And the form stays open
    And no vocabulary "Alpha" is stored

  @AC_SIMPL-18180_AC6 @D109
  Scenario: One semantic error is counted in the singular
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    When I fill in "Name" with "${Alpha}", "Description" with "One error" and choose a Turtle file with the error S3 for "Vocabulary file"
    And I click the button "Submit"
    Then the first line of the region "Validation report" is "1 error found"

  @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D14
  Scenario: After fixing the errors the vocabulary can be submitted
    Given I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/new"
    And I submitted the form with a Turtle file with the error S3 and see the region "Validation report"
    When I choose a valid Turtle file for "Vocabulary file"
    And I click the button "Submit"
    Then I see a message with role status "Version 1 of ${Alpha} was created."

  @AC_SIMPL-18172_AC1 @D33 @D46 @D60 @D74
  Scenario: Update opens the new version form pre-filled with the latest version
    Given a vocabulary "Alpha" exists with description "Latest description"
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Update" in the menu "Actions for ${Alpha}"
    Then I am on the page "/vocabularies/${Alpha}/versions/new"
    And the field "Description" holds "Latest description"
    And the field "Vocabulary file" shows "${Alpha}.ttl (current content)"
    And the field "Change log" is empty and required
    And I see the text "Submitting creates version 2 and deprecates every older ACTIVE version."
    And I see no field for a version

  @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D14 @D113 @D4
  Scenario: Submitting a new version creates the next version and confirms its number
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/${Alpha}/versions/new"
    When I fill in "Change log" with "Added concept two" and choose a valid changed Turtle file for "Vocabulary file"
    And I click the button "Submit"
    Then I am on the page "/vocabularies/${Alpha}"
    And I see a message with role status "Version 2 of ${Alpha} was created."
    And I see a region named "Version 2"
    And the row of version 2 shows the status "Active" and the row of version 1 the status "Deprecated"

  @D33
  Scenario: A description-only change creates a new version from the current content
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/${Alpha}/versions/new"
    When I replace the field "Description" with "Only the description changed" and fill in "Change log" with "Reworded"
    And I click the button "Submit"
    Then I see a message with role status "Version 2 of ${Alpha} was created."

  @D46 @D37
  Scenario: Resubmitting the latest version unchanged shows the conflict above the form
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/${Alpha}/versions/new"
    When I fill in "Change log" with "No change"
    And I click the button "Submit"
    Then I see one element with role alert above the form
    And the form stays open

  @AC_SIMPL-18184_AC4 @D11 @D14 @D47 @D109
  Scenario: A reference to a deprecated version shows a warning naming it and the latest ACTIVE version
    Given a vocabulary "Ref" exists
    And vocabulary "Ref" has a new version
    And a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/${Alpha}/versions/new"
    When I fill in "Change log" with "Uses Ref" and choose a Turtle file that references version 1 of "Ref" for "Vocabulary file"
    And I click the button "Submit"
    Then I see a message with role status "Version 2 of ${Alpha} was created."
    And I see a region named "Validation warnings" whose first line has role status and the text "1 warning found"
    And the region lists "Referenced internal vocabulary '${Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version." with its location

  @AC_SIMPL-18172_AC3 @D24 @D60 @D114
  Scenario: An unexpected error on submit tells the user to retry or contact support
    Given a vocabulary "Alpha" exists
    And the API answers POST /vocabularies/${Alpha}/versions with 500 and the problem detail "Unexpected failure"
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/${Alpha}/versions/new"
    When I fill in "Change log" with "Will fail" and choose a valid changed Turtle file for "Vocabulary file"
    And I click the button "Submit"
    Then I see an alert with the text "Something went wrong. Retry later or contact support." and "Unexpected failure"
    And the form stays open

  @AC_SIMPL-18172_AC3 @D44 @D60
  Scenario: A 503 on submit is shown like a 500
    Given a vocabulary "Alpha" exists
    And the API answers POST /vocabularies/${Alpha}/versions with 503 and the problem detail "The vocabulary store is not available. Retry later."
    And I am logged in to the UI as a vocabulary administrator
    And I am on the page "/vocabularies/${Alpha}/versions/new"
    When I fill in "Change log" with "Will fail" and choose a valid changed Turtle file for "Vocabulary file"
    And I click the button "Submit"
    Then I see an alert with the text "Something went wrong. Retry later or contact support." and "The vocabulary store is not available. Retry later."

  @Q6 @D74 @NFR-10 @a11y
  Scenario Outline: The form pages have no accessibility violations: <page>
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    When I open the page "<page>"
    Then axe reports no WCAG 2.1 AA violations
    And every field and button can be reached with the keyboard

    Examples:
      | page                             |
      | /vocabularies/new                |
      | /vocabularies/${Alpha}/versions/new |
