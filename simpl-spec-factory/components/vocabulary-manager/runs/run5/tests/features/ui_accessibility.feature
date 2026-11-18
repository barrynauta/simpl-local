Feature: Accessibility of the main UI pages
  One axe check (WCAG 2.1 levels A and AA) per main page: vocabulary list, vocabulary
  details, the creation form and the new version form (screens of D31, D33, D35).
  The violations of each page are written to reports/axe/<page>.json.

  @ui @a11y @D31 @D34
  Scenario: The vocabulary list has no WCAG 2.1 AA violations
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And a vocabulary "{Beta}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{run}"
    Then the page has no WCAG 2.1 AA violations, reported as "vocabulary-list"

  @ui @a11y @D31 @D35 @D36
  Scenario: The vocabulary details page has no WCAG 2.1 AA violations
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies/{Alpha}"
    And I select version 1
    Then the page has no WCAG 2.1 AA violations, reported as "vocabulary-details"

  @ui @a11y @D32 @D37
  Scenario: The creation form has no WCAG 2.1 AA violations
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "New Vocabulary +"
    Then the page has no WCAG 2.1 AA violations, reported as "create-form"

  @ui @a11y @D33
  Scenario: The new version form has no WCAG 2.1 AA violations
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I click "Update" in the actions menu of "{Alpha}"
    Then the page has no WCAG 2.1 AA violations, reported as "new-version-form"
