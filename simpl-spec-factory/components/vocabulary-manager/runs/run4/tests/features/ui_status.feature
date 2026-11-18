Feature: Change the vocabulary status in the UI
  The status-change action offers only the other status (D4). Errors: D24.

  @ui @AC_SIMPL-18174_AC1 @D4 @D6
  Scenario: A vocabulary is deprecated from the list
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Deprecate" for vocabulary "Health"
    Then the row of vocabulary "Health" shows "Deprecated"
    And I see a success notification

  @ui @AC_SIMPL-18174_AC1 @D4
  Scenario: A deprecated vocabulary is restored to Active, the only status offered
    Given a vocabulary "Health" exists
    And the vocabulary "Health" has status "DEPRECATED"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    Then I do not see the action "Deprecate"
    When I click "Activate" for vocabulary "Health"
    Then the row of vocabulary "Health" shows "Active"

  @ui @AC_SIMPL-18174_AC2 @D24 @negative
  Scenario: A vocabulary that is no longer available is reported as not found
    Given a vocabulary "Health" exists
    And the API is stubbed to answer 404 to "updateVocabularyStatus"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Deprecate" for vocabulary "Health"
    Then I see "not found"
    And the row of vocabulary "Health" shows "Active"

  @ui @AC_SIMPL-18174_AC3 @D24 @negative
  Scenario: A failed status update leaves the status unchanged
    Given a vocabulary "Health" exists
    And the API is stubbed to answer 500 to "updateVocabularyStatus"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Deprecate" for vocabulary "Health"
    Then I see "could not be updated"
    And the row of vocabulary "Health" shows "Active"
