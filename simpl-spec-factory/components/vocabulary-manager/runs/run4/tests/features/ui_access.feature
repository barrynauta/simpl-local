Feature: Role-based access in the UI
  Restricted actions are hidden for users without GA_VOCABULARY_ADMIN (D18).

  @ui @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: An administrator sees every action
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    Then I see the action "New Vocabulary +"
    And the row of vocabulary "Health" has the actions "View vocabulary details, Download vocabulary, Update, Deprecate"

  @ui @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario: A viewer does not see the restricted actions
    Given a vocabulary "Health" exists
    And I sign in to the UI as "ga-viewer"
    When I open the vocabulary list
    Then I do not see the action "New Vocabulary +"
    And I do not see the action "Update"
    And I do not see the action "Deprecate"

  @ui @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario Outline: Opening a restricted form directly shows an authorisation message (<user>)
    Given a vocabulary "Health" exists
    And I sign in to the UI as "<user>"
    When I open the address of the <form> form directly
    Then I see "You are not authorized to perform this action."

    Examples:
      | user      | form                            |
      | ga-viewer | create                          |
      | ga-viewer | new version of "Health"         |
      | ga-norole | create                          |

  @ui @AC_SIMPL-18195_AC2 @D18 @D24 @negative
  Scenario: A 403 from the API shows the authorisation message
    Given a vocabulary "Health" exists
    And the API is stubbed to answer 403 to "updateVocabularyStatus"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Deprecate" for vocabulary "Health"
    Then I see "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a token is sent to sign in
    When I open the UI without signing in
    And I open the UI address "/vocabularies"
    Then I am sent to the sign-in page
