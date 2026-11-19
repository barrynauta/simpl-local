@ui
Feature: Create a vocabulary and a new version in the UI
  Create form and new version form (D13, D14, D32, D33, D37, D46, D60).

  @AC_SIMPL-18169_AC1 @D32 @D60
  Scenario: The creation form has name, description and file, and no version or change log
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    Then I see the text "Name"
    And I see the text "Description"
    And I see the text "Vocabulary file"
    And the form has no field labelled "Change log"
    And the form has no field labelled "Version"

  @AC_SIMPL-18169_AC2 @AC_SIMPL-18186_AC1 @AC_SIMPL-18180_AC2 @AC_SIMPL-18193_AC1 @AC_SIMPL-18182_AC2 @D6 @D14 @D32
  Scenario: A valid new vocabulary is created and published as version 1
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{{name:Health}}"
    And I fill in "Description" with "Health terms {{run}}"
    And I choose the file "w3c_namespaces_only.ttl" for "Vocabulary file"
    Then I see the text "Syntax check passed."
    When I click "Submit"
    Then I see the text "Version 1 of {{name:Health}} was created."
    And version 1 of "Health" has the status "ACTIVE"
    And the field "changelog" of "/vocabularies/{{name:Health}}/versions/1" is "Initial version."

  @AC_SIMPL-18169_AC2 @D37 @negative
  Scenario: Field errors are shown before submit
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "lower case"
    And I fill in "Description" with ""
    Then I see a field error under "Name"
    And I see a field error under "Description"
    When I click "Submit"
    Then no vocabulary of this run is stored

  @AC_SIMPL-18169_AC2 @D37 @D15 @negative
  Scenario: A duplicate name is reported above the form
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{{name:Health}}"
    And I fill in "Description" with "Duplicate"
    And I choose the file "w3c_namespaces_only.ttl" for "Vocabulary file"
    And I click "Submit"
    Then I see the text "Vocabulary with name '{{name:Health}}' already exists."

  @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D13 @D60 @negative
  Scenario: A syntax error is shown with its line when the file is chosen
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{{name:Broken}}"
    And I fill in "Description" with "Broken file"
    And I choose the file "syntax_error_line3.ttl" for "Vocabulary file"
    Then I see the text "line 3"
    And the control "Submit" is disabled
    And no vocabulary named "Broken" is stored

  @AC_SIMPL-18180_AC4 @D13
  Scenario: A valid file passes the upload check
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I choose the file "w3c_namespaces_only.ttl" for "Vocabulary file"
    Then I see the text "Syntax check passed."

  @D13 @D15 @negative
  Scenario: A file above 5 MB is refused in the browser
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I choose the file "turtle:5242881" for "Vocabulary file"
    Then I see a field error under "Vocabulary file"
    And the control "Submit" is disabled

  @AC_SIMPL-18180_AC6 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D9 @D14 @negative
  Scenario: Semantic errors are listed in the report panel on submit
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{{name:Many}}"
    And I fill in "Description" with "Several errors"
    And I choose the file "several_semantic_errors.ttl" for "Vocabulary file"
    And I click "Submit"
    Then I see the text "Literal value for object property"
    And I see the text "Property declared as both object and datatype property"
    And I see the text "Duplicate preferred label for the same language"
    And I see the control "Submit"
    And no vocabulary named "Many" is stored

  @AC_SIMPL-18172_AC1 @D33
  Scenario: The update form is pre-filled with the latest version
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Update" in the row of "Health"
    Then the field "Description" holds "Description of Health {{run}}"
    And I see the text "{{name:Health}}.ttl (current content)"
    And the field "Change log" holds ""

  @AC_SIMPL-18172_AC2 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @AC_SIMPL-18180_AC3 @D14 @D33 @D46
  Scenario: A new version is created from the form with the assigned version number
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Update" in the row of "Health"
    Then I see the text "Submitting creates version 2 and deprecates every older ACTIVE version."
    And I do not see the text "Version number"
    When I fill in "Description" with "Second description {{run}}"
    And I fill in "Change log" with "Second version"
    And I click "Submit"
    Then I see the text "Version 2 of {{name:Health}} was created."
    And the version statuses of "Health" are "1:DEPRECATED, 2:ACTIVE"

  @AC_SIMPL-18172_AC3 @D24 @D44 @D60 @negative
  Scenario: An unexpected error during version creation
    Given the vocabulary "Health" exists
    And the API is stubbed to answer "POST" "/vocabularies/{{name:Health}}/versions" with status 500
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Update" in the row of "Health"
    And I fill in "Change log" with "Second version"
    And I click "Submit"
    Then I see the text "Something went wrong. Retry later or contact support."
    And the vocabulary "Health" has 1 version

  @D46 @negative
  Scenario: An unchanged resubmission is reported above the form
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Update" in the row of "Health"
    And I fill in "Change log" with "No real change"
    And I click "Submit"
    Then I see the text "Version 1 of vocabulary '{{name:Health}}' already has this content and description."
    And the vocabulary "Health" has 1 version

  @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: A reference to a deprecated version shows a warning after creation
    Given the vocabulary "Base" exists with 2 versions
    And the vocabulary "User" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:User}}"
    And I click "Update" in the row of "User"
    And I choose the file "reference_to_base_v1.ttl" for "Vocabulary file"
    And I fill in "Change log" with "Uses Base"
    And I click "Submit"
    Then I see the text "Version 2 of {{name:User}} was created."
    And I see the text "Referenced internal vocabulary '{{name:Base}}' version 1 is deprecated. Use version 2, the latest ACTIVE version."
