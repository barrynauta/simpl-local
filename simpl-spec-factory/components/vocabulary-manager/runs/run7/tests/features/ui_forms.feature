@ui
Feature: Create and new version forms in the UI
  Routes /vocabularies/new and /vocabularies/<Name>/versions/new (D74). Gherkin only; step 60 adds the steps.

  @AC_SIMPL-18169_AC1 @D32 @D60 @D74
  Scenario: The create form has name, description and file, and no version or change log field
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click the button "New Vocabulary +"
    Then I am on the page "/vocabularies/new"
    And the form has the fields "Name, Description, Vocabulary file"
    And the form has no field "Change log"
    And the form has no field "Version"

  @AC_SIMPL-18169_AC2 @D37 @D62 @negative
  Scenario: Field errors are shown under the fields and nothing is sent
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "lower${rid}"
    And I choose the file "cpsv_like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then the field "Name" shows an error
    And the field "Description" shows an error
    And the focus is on the field "Name"
    And no write request was sent to the API

  @AC_SIMPL-18169_AC2 @D15 @D37 @negative
  Scenario: A duplicate name is reported above the form
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "${V:Alpha}"
    And I fill in "Description" with "Duplicate"
    And I choose the file "cpsv_like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see an alert "Vocabulary with name '${V:Alpha}' already exists."

  @AC_SIMPL-18169_AC2 @D76 @D62 @negative
  Scenario: A file over 5 MB is refused in the browser
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I choose a valid file of 5242881 bytes for "Vocabulary file"
    Then the field "Vocabulary file" shows the error "The file is larger than 5 MB."
    And the button "Submit" is disabled

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @D13 @D60
  Scenario: A chosen file is checked for syntax in the browser
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I choose the file "cpsv_like.ttl" for "Vocabulary file"
    Then I see the text "Syntax check passed."
    And the button "Submit" is enabled

  @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @D13 @D60 @negative
  Scenario: A syntax error in the chosen file shows its line and blocks submit
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I choose the file "syntax_error.ttl" for "Vocabulary file"
    Then I see the text "line 3"
    And the button "Submit" is disabled

  @AC_SIMPL-18169_AC2 @AC_SIMPL-18180_AC2 @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @AC_SIMPL-18182_AC2 @D14 @D32 @D72
  Scenario: A valid new vocabulary is created and the details page confirms version 1
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "${V:Alpha}"
    And I fill in "Description" with "Created in the UI"
    And I choose the file "cpsv_like.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I am on the page "/vocabularies/${V:Alpha}"
    And I see a status message "Version 1 of ${V:Alpha} was created."
    And the version row 1 shows "Active"
    And the version row 1 shows "Initial version."

  @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D9 @D75 @negative
  Scenario: Semantic errors on submit are listed in the validation report and the form stays open
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    And I fill in "Name" with "${V:Alpha}"
    And I fill in "Description" with "Has semantic errors"
    And I choose the file "semantic_s3.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see the region "Validation report"
    And the region "Validation report" shows "Property declared as both object and datatype property"
    And the region "Validation report" shows "resource 'http://example.org/${rid}/s3#hasPart'"
    And I am on the page "/vocabularies/new"

  @AC_SIMPL-18172_AC1 @D33 @D46 @D74
  Scenario: The update form is pre-filled with the latest description and content
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "Update" in the actions menu "Actions for ${V:Alpha}"
    Then I am on the page "/vocabularies/${V:Alpha}/versions/new"
    And the field "Description" has the value "Description of Alpha revision 2"
    And I see the text "${V:Alpha}.ttl (current content)"
    And the field "Change log" is empty
    And I see the text "Submitting creates version 3 and deprecates every older ACTIVE version."

  @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D14 @D33 @D46
  Scenario: A new version is created from the form and the confirmation names its number
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/${V:Alpha}/versions/new"
    Then the form has no field "Version"
    When I fill in "Description" with "Second version from the UI"
    And I fill in "Change log" with "Changed the description."
    And I click the button "Submit"
    Then I see a status message "Version 2 of ${V:Alpha} was created."
    And the version row 2 shows "Active"
    And the version row 1 shows "Deprecated"

  @AC_SIMPL-18184_AC4 @D11 @D14 @D47 @D75
  Scenario: A reference to a deprecated version is shown in the warnings panel after creation
    Given vocabulary "Ref" has 2 versions
    And vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/${V:Alpha}/versions/new"
    And I fill in "Change log" with "Uses Ref version 1."
    And I choose the file "deprecated_reference.ttl" for "Vocabulary file"
    And I click the button "Submit"
    Then I see the region "Validation warnings"
    And the region "Validation warnings" shows "Referenced internal vocabulary '${V:Ref}' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  @AC_SIMPL-18172_AC3 @D24 @D44 @D60 @negative
  Scenario: An unexpected error on version creation advises to retry or contact support
    Given vocabulary "Alpha" exists
    And the API answers POST "/vocabularies/${V:Alpha}/versions" with status 500
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/${V:Alpha}/versions/new"
    And I fill in "Change log" with "Will fail."
    And I click the button "Submit"
    Then I see an alert "Something went wrong. Retry later or contact support."
    And vocabulary "Alpha" has 1 version

  @D44 @D60 @negative
  Scenario: A 503 on version creation is shown like a 500
    Given vocabulary "Alpha" exists
    And the API answers POST "/vocabularies/${V:Alpha}/versions" with status 503
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/${V:Alpha}/versions/new"
    And I fill in "Change log" with "Will fail."
    And I click the button "Submit"
    Then I see an alert "Something went wrong. Retry later or contact support."

  @Q6 @NFR-10 @a11y
  Scenario: Both form pages meet WCAG 2.1 AA
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies/new"
    Then the page has no accessibility violations
    When I open the page "/vocabularies/${V:Alpha}/versions/new"
    Then the page has no accessibility violations
