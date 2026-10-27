@ui
Feature: Vocabulary list in the UI

  Background:
    Given I am signed in to the UI as "admin"

  @SIMPL-18161 @D22
  Scenario: The list shows every vocabulary with its key data
    Given a vocabulary "{Alpha}" exists
    When I open the Vocabulary Management page
    Then I see a row for "{Alpha}" with: actions menu, name, description, current version, status, date created, last updated, creator
    And the actions menu of "{Alpha}" offers "View vocabulary details" and "Download vocabulary"

  @SIMPL-18161
  Scenario: A failing list request is reported
    Given the API fails when the vocabulary list is requested
    When I open the Vocabulary Management page
    Then I am told that the vocabulary list could not be loaded
    And I see the details of the error

  @SIMPL-18163 @D23
  Scenario: Filter by status and by part of the name
    Given a vocabulary "{Alpha}" exists
    And a vocabulary "{Beta}" exists
    And vocabulary "{Beta}" is deprecated
    When I open the Vocabulary Management page
    And I filter on name "{RUN}"
    Then I see "{Alpha}" and "{Beta}"
    When I also filter on status "Deprecated"
    Then I see "{Beta}" and not "{Alpha}"
    When I clear the name filter
    Then every row shows status "Deprecated"

  @SIMPL-18163
  Scenario: Sort by clicking a column header
    Given a vocabulary "{Bravo}" exists
    And a vocabulary "{Alpha}" exists
    When I open the Vocabulary Management page filtered on name "{RUN}"
    And I click the "Name" column header
    Then the rows are ordered by name
    When I click the "Name" column header again
    Then the rows are ordered by name in the opposite direction

  @SIMPL-18178 @D25
  Scenario: Deprecated vocabularies carry a warning in the list
    Given a vocabulary "{Alpha}" exists
    And vocabulary "{Alpha}" is deprecated
    When I open the Vocabulary Management page
    Then the row of "{Alpha}" shows a clear deprecation warning
