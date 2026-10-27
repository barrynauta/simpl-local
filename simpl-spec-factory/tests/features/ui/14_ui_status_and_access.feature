@ui
Feature: Status change and access control in the UI

  @SIMPL-18174
  Scenario: Deprecate and restore a vocabulary
    Given I am signed in to the UI as "admin"
    And a vocabulary "{Alpha}" exists
    When I change the status of "{Alpha}" to "Deprecated"
    Then I see a confirmation that the status was updated
    And the row of "{Alpha}" shows status "Deprecated"
    When I change the status of "{Alpha}" to "Active"
    Then the row of "{Alpha}" shows status "Active"

  @SIMPL-18174
  Scenario: A failing status change leaves the status unchanged
    Given I am signed in to the UI as "admin"
    And a vocabulary "{Alpha}" exists
    And the API fails when a status is changed
    When I change the status of "{Alpha}" to "Deprecated"
    Then I am told that the status could not be updated
    And the row of "{Alpha}" still shows status "Active"

  @SIMPL-18195 @D26
  Scenario: A viewer does not see the management actions
    Given I am signed in to the UI as "viewer"
    And a vocabulary "{Alpha}" exists
    When I open the Vocabulary Management page
    Then I see "{Alpha}" in the list
    And "New Vocabulary +", "Update" and the status change action are not shown

  @SIMPL-18195
  Scenario: Without signing in, access is refused and I am asked to sign in
    Given I am not signed in to the UI
    When I open the Vocabulary Management page
    Then I am asked to sign in
