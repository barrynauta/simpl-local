Feature: Validation feedback in the UI
  The browser checks syntax on file choice (D13); the server runs syntax and semantic
  validation on submit (D9). Report and warnings panels: D14.

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC4 @AC_SIMPL-18193_AC1 @D13
  Scenario: A valid file passes the upload check
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I choose the file "valid-skos.ttl"
    Then I see "Syntax check passed."

  @ui @AC_SIMPL-18180_AC1 @AC_SIMPL-18180_AC5 @AC_SIMPL-18180_AC8 @D13 @negative
  Scenario: A syntax error is shown on upload with its position and blocks the submit
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I choose the file "syntax-error-line4.ttl"
    Then I see "line 4"
    And the button "Submit" is disabled

  @ui @AC_SIMPL-18180_AC2 @AC_SIMPL-18180_AC6 @AC_SIMPL-18182_AC1 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D9 @D14 @negative
  Scenario: Semantic errors on creation are listed in the report panel
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I fill in "Name" with "${run}Semantic"
    And I fill in "Description" with "Has a semantic error."
    And I choose the file "s1-literal-for-object-property.ttl"
    And I submit the form
    Then the report panel lists an issue "Literal value for object property" at "statement"
    And the form stays open

  @ui @AC_SIMPL-18180_AC3 @AC_SIMPL-18180_AC7 @AC_SIMPL-18182_AC3 @AC_SIMPL-18193_AC2 @D9 @D14 @negative
  Scenario: All semantic errors of a new version are listed together
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    And I fill in "Change log" with "Broken on purpose."
    And I choose the file "three-semantic-errors.ttl"
    And I submit the form
    Then the report panel lists 3 issues
    And the form stays open

  @ui @AC_SIMPL-18180_AC8 @AC_SIMPL-18193_AC2 @D14 @negative
  Scenario: The flow cannot complete until the errors are fixed
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I fill in "Name" with "${run}Fixed"
    And I fill in "Description" with "Fixed after the report."
    And I choose the file "s4-duplicate-preflabel.ttl"
    And I submit the form
    Then the report panel lists 1 issues
    When I choose the file "valid-skos.ttl"
    And I submit the form
    Then I see "Version 1 of ${run}Fixed was created."

  @ui @AC_SIMPL-18182_AC2 @AC_SIMPL-18193_AC1 @AC_SIMPL-18180_AC4 @D14
  Scenario: A semantically valid submission is confirmed
    Given I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "New Vocabulary +"
    And I fill in "Name" with "${run}Good"
    And I fill in "Description" with "Valid."
    And I choose the file "valid-skos.ttl"
    And I submit the form
    Then I see "Version 1 of ${run}Good was created."

  @ui @AC_SIMPL-18184_AC4 @D11 @D14
  Scenario: A reference to a deprecated version shows a warning that names it and the ACTIVE version
    Given a vocabulary "Base" exists with 2 versions
    And a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Update" for vocabulary "Health"
    And I fill in "Change log" with "Reuses an old base."
    And I choose the file "references-base-v1.ttl"
    And I submit the form
    Then I see "Version 2 of ${name:Health} was created."
    And the warnings panel lists "Referenced internal vocabulary '${name:Base}' version 1 is deprecated. Use version 2, the latest ACTIVE version."
