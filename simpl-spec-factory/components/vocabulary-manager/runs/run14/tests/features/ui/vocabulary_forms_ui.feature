Feature: Create and new version forms in the UI
  Browser steps in features/steps/ui_steps.py (step 60). D13, D14, D32, D33, D37, D46, D60, D62, D72, D74,
  D76, D90, D101, D109, D113, D141, D161, D180.

  @ui @AC_SIMPL-18169_AC1 @D32 @D60 @D74 @D141
  Scenario: The creation form has Name, Description and Vocabulary file, and no version or change log
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I press the button "New Vocabulary +"
    Then I am on the route "/vocabularies/new"
    And the form has the required fields "Name", "Description" and "Vocabulary file" marked with '*'
    And the form has no field "Change log" and no field for a version
    And the form has the buttons "Submit" and "Cancel"

  @ui @AC_SIMPL-18169_AC2 @AC_SIMPL-18180_AC2 @AC_SIMPL-18186_AC1 @D32 @D72 @D14
  Scenario: A valid creation opens the details page with the success notification
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Fruit terms."
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then I am on the route "/vocabularies/$Apple"
    And a status message shows "Version 1 of $Apple was created."
    And version 1 of vocabulary "$Apple" has version status "ACTIVE"
    And the stored changelog of version 1 of "$Apple" is "Initial version."

  @ui @AC_SIMPL-18169_AC2 @D37 @D62 @D101
  Scenario: Submit with empty fields shows every field error and sends nothing
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I press the button "Submit"
    Then each of "Name", "Description" and "Vocabulary file" shows a field error
    And the focus is on the field "Name"
    And no POST request was sent

  @ui @AC_SIMPL-18169_AC2 @D37
  Scenario Outline: A name that breaks the rules shows a field error: <name>
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "<name>" and leave the field
    Then the field "Name" shows a field error

    Examples:
      | name       |
      | ab         |
      | lowerCase  |
      | With-Dash  |

  @ui @AC_SIMPL-18169_AC2 @D37 @D161
  Scenario: A duplicate name shows the API detail above the form and keeps the values
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Again."
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then one alert shows "Vocabulary with name '$Apple' already exists."
    And the field "Name" still holds "$Apple"

  @ui @AC_SIMPL-18169_AC2 @D15 @D76 @D62
  Scenario: A file over 5 MB is refused in the browser and Submit is disabled
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I choose a valid Turtle file of 5242881 bytes for "Vocabulary file"
    Then the field "Vocabulary file" shows the error "The file is larger than 5 MB."
    And the button "Submit" is disabled

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @D13
  Scenario: Choosing a valid file runs the syntax check and shows that it passed
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I choose a valid Turtle file for "Vocabulary file"
    Then I see the text "Syntax check passed."
    And the button "Submit" is enabled

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @D13 @D60 @D62
  Scenario: Choosing a file with a syntax error shows the line and disables Submit
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I choose for "Vocabulary file" a Turtle file with a syntax error on line 3
    Then the upload check shows an error on line 3
    And the button "Submit" is disabled
    When I choose a valid Turtle file for "Vocabulary file"
    Then the button "Submit" is enabled

  @ui @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D14 @D90 @D109 @D9
  Scenario: Semantic errors on submit are shown in the validation report and the form stays open
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Fruit terms."
    And I choose for "Vocabulary file" a Turtle file with a literal value for an object property
    And I press the button "Submit"
    Then the region "Validation report" starts with an alert "1 error found"
    And the region "Validation report" lists "Literal value for object property" with its location
    And I am on the route "/vocabularies/new"
    And 0 vocabularies have a name containing "$Apple"

  @ui @AC_SIMPL-18180_AC7 @AC_SIMPL-18193_AC2 @D9 @D109
  Scenario: Several semantic errors are all shown in one report
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Fruit terms."
    And I choose for "Vocabulary file" a Turtle file with 3 semantic errors
    And I press the button "Submit"
    Then the region "Validation report" starts with an alert "3 errors found"
    And the region "Validation report" lists 3 issues with detail and location

  @ui @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D14
  Scenario: After fixing the errors the same form can be submitted
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Fruit terms."
    And I choose for "Vocabulary file" a Turtle file with a literal value for an object property
    And I press the button "Submit"
    Then the region "Validation report" is shown
    When I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then a status message shows "Version 1 of $Apple was created."

  @ui @AC_SIMPL-18182_AC2 @AC_SIMPL-18193_AC1 @AC_SIMPL-18180_AC4 @D14
  Scenario: A semantically valid submit confirms success
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Fruit terms."
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then a status message shows "Version 1 of $Apple was created."
    And the region "Validation report" is not shown

  @ui @AC_SIMPL-18172_AC1 @D33 @D74 @D46
  Scenario: The Update action opens the new version form pre-filled
    Given a vocabulary "$Apple" exists with description "Fruit terms."
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "Update" in the menu "Actions for $Apple"
    Then I am on the route "/vocabularies/$Apple/versions/new"
    And the field "Description" holds "Fruit terms."
    And the field "Vocabulary file" shows "$Apple.ttl (current content)"
    And the field "Change log" is empty and required
    And I see the text "Submitting creates version 2 and deprecates every older ACTIVE version."
    And the form has no field for a version

  @ui @AC_SIMPL-18172_AC2 @AC_SIMPL-18180_AC3 @AC_SIMPL-18189_AC1 @AC_SIMPL-18189_AC2 @AC_SIMPL-18189_AC3 @AC_SIMPL-18191_AC1 @D14 @D113 @D180 @D4
  Scenario: Submitting the update form creates the next version and shows its number
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/$Apple/versions/new"
    And I fill "Change log" with "Added pears."
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then I am on the route "/vocabularies/$Apple/versions/2"
    And a status message shows "Version 2 of $Apple was created."
    And the version list shows version 2 as "Active" and version 1 as "Deprecated"

  @ui @AC_SIMPL-18172_AC2 @D33
  Scenario: A description-only change creates a new version with the current content
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/$Apple/versions/new"
    And I fill "Description" with "Better description." and "Change log" with "Description only."
    And I press the button "Submit"
    Then a status message shows "Version 2 of $Apple was created."
    And the content of version 2 of "$Apple" equals the content of version 1

  @ui @D46 @D161
  Scenario: Resubmitting the unchanged latest version shows the conflict above the form
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/$Apple/versions/new"
    And I fill "Change log" with "Nothing changed."
    And I press the button "Submit"
    Then one alert shows "Version 1 of vocabulary '$Apple' already has this content and description."
    And vocabulary "$Apple" has 1 version

  @ui @AC_SIMPL-18172_AC3 @D24 @D60 @D44
  Scenario Outline: An unexpected error during version creation advises retry or support: <status>
    Given a vocabulary "$Apple" exists
    And the API answers <status> with detail "Broken" to POST /vocabularies/$Apple/versions
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/$Apple/versions/new"
    And I fill "Change log" with "Added pears."
    And I choose a valid Turtle file for "Vocabulary file"
    And I press the button "Submit"
    Then one alert shows "Something went wrong. Retry later or contact support." followed by "Broken"
    And vocabulary "$Apple" has 1 version

    Examples:
      | status |
      | 500    |
      | 503    |

  @ui @AC_SIMPL-18184_AC4 @D11 @D14 @D47
  Scenario: A reference to a deprecated version shows a warning that names it and recommends the latest ACTIVE version
    Given vocabulary "$Beta" has 2 versions
    And a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/$Apple/versions/new"
    And I fill "Change log" with "Uses Beta."
    And I choose for "Vocabulary file" a Turtle file that references version 1 of "$Beta"
    And I press the button "Submit"
    Then a status message shows "Version 2 of $Apple was created."
    And the region "Validation warnings" starts with a status line "1 warning found"
    And the region "Validation warnings" lists "Referenced internal vocabulary '$Beta' version 1 is deprecated. Use version 2, the latest ACTIVE version."

  @ui @D10 @D14 @D109
  Scenario: An unregistered external namespace shows a warning after creation
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I fill "Name" with "$Apple" and "Description" with "Fruit terms."
    And I choose for "Vocabulary file" a Turtle file with an unregistered external namespace
    And I press the button "Submit"
    Then a status message shows "Version 1 of $Apple was created."
    And the region "Validation warnings" starts with a status line "1 warning found"

  @ui @D60
  Scenario: Cancel on the creation form leaves the form without a request
    Given I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies/new"
    And I press the button "Cancel"
    Then I am no longer on the route "/vocabularies/new"
    And no POST request was sent
