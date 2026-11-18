Feature: Create a vocabulary and see validation in the UI
  The browser checks Turtle syntax when a file is chosen (D13); the server runs syntax and
  semantic validation on submit and is authoritative (D9, D14).

  @ui @AC_SIMPL-18169_AC1 @D32 @D60
  Scenario: The creation form has name, description and file, and no version or change log
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    Then I see the form fields ["Name", "Description", "Vocabulary file"]
    And I do not see the form fields ["Version", "Change log"]
    And I see the action "Submit"
    And I see the action "Cancel"

  @ui @AC_SIMPL-18169_AC2 @AC_SIMPL-18186_AC1 @AC_SIMPL-18193_AC1 @AC_SIMPL-18180_AC2 @AC_SIMPL-18182_AC2 @D6 @D14 @D32
  Scenario: A valid vocabulary is created and shown as Active
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Created in the UI."
    And I choose the fixture "w3c-and-own-namespaces.ttl" as the vocabulary file
    And I submit
    Then a success notification says "Version 1 of {Alpha} was created."
    And version 1 of "{Alpha}" has the field "changelog" equal to "Initial version."
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @ui @AC_SIMPL-18169_AC2 @D37 @negative
  Scenario Outline: Field errors are shown under the field before submit (<case>)
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "<name>"
    And I fill in "Description" with "<description>"
    And I choose the fixture "w3c-and-own-namespaces.ttl" as the vocabulary file
    Then the "Submit" button is disabled
    And no vocabulary named "<name>" is stored

    Examples:
      | case                  | name         | description   |
      | name in lower case    | alpha{run}   | A description |
      | name too short        | Ab           | A description |
      | description missing   | {Alpha}      |               |

  @ui @AC_SIMPL-18169_AC2 @D15 @D37 @negative
  Scenario: A duplicate name is reported above the form
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Again."
    And I choose the fixture "w3c-and-own-namespaces.ttl" as the vocabulary file
    And I submit
    Then an error message says "Vocabulary with name '{Alpha}' already exists."
    And the form is still open

  @ui @AC_SIMPL-18169_AC2 @D13 @D15 @negative
  Scenario: A file above 5 MB is refused in the browser
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I choose a file of 5242881 bytes as the vocabulary file
    Then the "Submit" button is disabled

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @AC_SIMPL-18193_AC1 @D13
  Scenario: Choosing a valid file runs the syntax check and confirms it
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I choose the fixture "w3c-and-own-namespaces.ttl" as the vocabulary file
    Then I see the text "Syntax check passed."

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D13 @D60 @negative
  Scenario: Choosing a file with a syntax error shows its line and blocks submit
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Broken file."
    And I choose the fixture "syntax-error.ttl" as the vocabulary file
    Then I see the text "line 6"
    And the "Submit" button is disabled
    And no vocabulary named "{Alpha}" is stored

  @ui @AC_SIMPL-18180_AC6 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D9 @D14 @negative
  Scenario: A semantic error found on submit is listed in the report panel and the form stays open
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Semantic error."
    And I choose the fixture "s6-no-statements.ttl" as the vocabulary file
    And I submit
    Then the validation report lists an issue with detail "Vocabulary contains no statements" and location "document"
    And the form is still open
    And no vocabulary named "{Alpha}" is stored

  @ui @AC_SIMPL-18180_AC7 @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D9 @D14 @negative
  Scenario: Several semantic errors are all listed in one report
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "Three errors."
    And I choose the fixture "multiple-semantic-errors.ttl" as the vocabulary file
    And I submit
    Then the validation report lists an issue with detail "Literal value for object property"
    And the validation report lists an issue with detail "Property declared as both object and datatype property"
    And the validation report lists an issue with detail "Duplicate preferred label for the same language"
    And the form is still open

  @ui @AC_SIMPL-18193_AC1 @D14 @D10
  Scenario: Warnings are shown after a successful creation
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    And I fill in "Name" with "{Alpha}"
    And I fill in "Description" with "With a warning."
    And I choose the fixture "external-unregistered.ttl" as the vocabulary file
    And I submit
    Then a success notification says "Version 1 of {Alpha} was created."
    And the warnings panel lists "prefix 'unreg' -> 'https://unregistered-{run}.example.org/terms/'"
