@ui
Feature: Status change and access control in the UI
  Status change actions 'Deprecate' and 'Activate' (D60, D72) and restricted actions (D18, SIMPL-18195).

  @AC_SIMPL-18174_AC1 @D60 @D3
  Scenario: Deprecating a vocabulary from the list
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Deprecate" in the menu "Actions for ${Alpha}"
    Then I see a message with role status "Status of ${Alpha} changed to Deprecated."
    And the row of "${Alpha}" shows the status "Deprecated"
    And the menu "Actions for ${Alpha}" offers "Activate" and not "Deprecate"

  @AC_SIMPL-18174_AC1 @D60
  Scenario: Activating a DEPRECATED vocabulary from the list
    Given a vocabulary "Alpha" exists
    And the status of vocabulary "Alpha" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Activate" in the menu "Actions for ${Alpha}"
    Then I see a message with role status "Status of ${Alpha} changed to Active."
    And the row of "${Alpha}" shows the status "Active"

  @AC_SIMPL-18174_AC2 @D24 @D72 @D114
  Scenario: A status change for a vocabulary that is no longer available
    Given a vocabulary "Alpha" exists
    And the API answers PATCH /vocabularies/${Alpha} with 404
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Deprecate" in the menu "Actions for ${Alpha}"
    Then I see one alert with the text "Vocabulary not found. The status was not changed."
    And the row of "${Alpha}" shows the status "Active"

  @AC_SIMPL-18174_AC3 @D24 @D72 @D114
  Scenario: A status change that fails during processing
    Given a vocabulary "Alpha" exists
    And the API answers PATCH /vocabularies/${Alpha} with 500 and the problem detail "Store update failed"
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Deprecate" in the menu "Actions for ${Alpha}"
    Then I see one alert with the text "The vocabulary status could not be updated." followed by "Something went wrong. Retry later or contact support." and "Store update failed"
    And the row of "${Alpha}" shows the status "Active"

  @AC_SIMPL-18195_AC1 @D18 @D16
  Scenario: An administrator sees and can use every action
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    Then I see the button "New Vocabulary +"
    And the menu "Actions for ${Alpha}" offers "Update" and "Deprecate"

  @AC_SIMPL-18195_AC2 @D18 @D16
  Scenario: A viewer does not see restricted actions
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    Then I do not see the button "New Vocabulary +"
    And the menu "Actions for ${Alpha}" offers "View vocabulary details", "History" and "Download vocabulary" only

  @AC_SIMPL-18195_AC2 @D18 @D74
  Scenario Outline: A viewer who opens a restricted form directly sees the authorisation message: <page>
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary viewer
    When I open the page "<page>"
    Then I see the text "You are not authorized to perform this action."

    Examples:
      | page                                |
      | /vocabularies/new                   |
      | /vocabularies/${Alpha}/versions/new |

  @AC_SIMPL-18195_AC2 @D18
  Scenario: A 403 from the API shows the authorisation message
    Given a vocabulary "Alpha" exists
    And the API answers PATCH /vocabularies/${Alpha} with 403
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Deprecate" in the menu "Actions for ${Alpha}"
    Then I see the text "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC2 @D18
  Scenario: A user without a vocabulary role sees the authorisation message on the list
    Given I am logged in to the UI as a user without a vocabulary role
    When I open the vocabulary list
    Then I see the text "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC3 @D18
  Scenario: A user without a session is sent to the Keycloak login page
    Given I am not logged in to the UI
    When I open the page "/vocabularies"
    Then I am on the Keycloak login page of the realm "simpl"
