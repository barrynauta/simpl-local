Feature: Status change and access control in the UI
  Status change: 'Deprecate' on an ACTIVE vocabulary, 'Activate' on a DEPRECATED one, no
  confirmation (D60). Users without GA_VOCABULARY_ADMIN do not see write actions (D18).

  @ui @AC_SIMPL-18174_AC1 @AC_SIMPL-18195_AC1 @D3 @D4 @D60
  Scenario: Deprecating a vocabulary updates the status and confirms it
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I click "Deprecate" in the actions menu of "{Alpha}"
    Then a success notification says "Status of {Alpha} changed to Deprecated."
    And the row of "{Alpha}" shows the status "Deprecated"
    And the vocabulary "{Alpha}" has status "DEPRECATED"

  @ui @AC_SIMPL-18174_AC1 @D4 @D60
  Scenario: A DEPRECATED vocabulary offers only 'Activate'
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I open the actions menu of "{Alpha}"
    Then I do not see the action "Deprecate"
    When I click "Activate" in the actions menu of "{Alpha}"
    Then a success notification says "Status of {Alpha} changed to Active."
    And the row of "{Alpha}" shows the status "Active"

  @ui @AC_SIMPL-18174_AC2 @D24 @negative
  Scenario: A vocabulary that is no longer available is reported and nothing changes
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    And the API answers "PATCH" "/vocabularies/{Alpha}" with status 404 for the UI
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I click "Deprecate" in the actions menu of "{Alpha}"
    Then I see the text "Vocabulary not found."
    And the row of "{Alpha}" shows the status "Active"

  @ui @AC_SIMPL-18174_AC3 @D24 @D60 @negative
  Scenario: A failed status update is reported and the status stays
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    And the API answers "PATCH" "/vocabularies/{Alpha}" with status 500 for the UI
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I click "Deprecate" in the actions menu of "{Alpha}"
    Then an error message says "Something went wrong. Retry later or contact support."
    And the row of "{Alpha}" shows the status "Active"
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @ui @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: An admin sees create, update and status change actions
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    Then I see the action "New Vocabulary +"
    And the actions menu of "{Alpha}" offers ["View vocabulary details", "Download vocabulary", "Update", "Deprecate"]

  @ui @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario Outline: A user without GA_VOCABULARY_ADMIN does not see write actions (<who>)
    Given a vocabulary "{Alpha}" exists
    And <login step>
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    Then I do not see the action "New Vocabulary +"
    And the actions menu of "{Alpha}" offers ["View vocabulary details", "Download vocabulary"]

    Examples:
      | who    | login step                                                                        |
      | viewer | I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER" |

  @ui @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: A viewer who opens the create form URL gets the authorisation message
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the create form by its URL
    Then I see the text "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: A viewer who opens the new version form URL gets the authorisation message
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the new version form of "{Alpha}" by its URL
    Then I see the text "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC2 @D18 @D24 @negative
  Scenario: A 403 from the API shows the authorisation message
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    And the API answers "PATCH" "/vocabularies/{Alpha}" with status 403 for the UI
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I click "Deprecate" in the actions menu of "{Alpha}"
    Then I see the text "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario: A user with no vocabulary management role cannot use the screens
    Given I am logged in to the UI as a user with no vocabulary management role
    When I open the UI page "/vocabularies"
    Then I see the text "You are not authorized to perform this action."
    And I do not see the action "New Vocabulary +"

  @ui @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user who is not logged in is sent to the login page
    Given I am not logged in to the UI
    When I open the UI page "/vocabularies"
    Then I am sent to the login page
