@ui @a11y @NFR-10
Feature: Accessibility of the main UI pages
  NFR-10: the user interface meets WCAG 2.1 level AA. One axe check (rules tagged wcag2a,
  wcag2aa, wcag21a, wcag21aa) per main page of D31 and the two forms of D32 and D33.
  Violations fail the scenario; the full axe result, with what axe marks incomplete for a
  manual check, is written to reports/a11y/.

  @D31 @D34
  Scenario: Accessibility of the Vocabulary list page
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    Then I see the text "Vocabularies"
    And the page has no WCAG 2.1 AA violations

  @D31 @D35 @D36
  Scenario: Accessibility of the Vocabulary details page
    Given the vocabulary "Health" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Health}}"
    Then I see the text "Back to vocabulary list"
    And the page has no WCAG 2.1 AA violations

  @D32 @D37 @D60
  Scenario: Accessibility of the create form
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I click "New Vocabulary +"
    Then I see the text "Vocabulary file"
    And the page has no WCAG 2.1 AA violations

  @D33 @D46 @D60
  Scenario: Accessibility of the new version form
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Update" in the row of "Health"
    Then I see the text "Change log"
    And the page has no WCAG 2.1 AA violations
