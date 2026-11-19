@ui
Feature: Vocabulary details, version history and access control in the UI
  Screen Vocabulary details at /vocabularies/<Name> with the version history (D35, D36),
  and restricted actions (D18).

  @AC_SIMPL-18167_AC1 @AC_SIMPL-18176_AC2 @D35 @D60
  Scenario: The history opens the details page with all versions newest first
    Given the vocabulary "Health" exists with 3 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "View vocabulary details" in the row of "Health"
    Then I am on the page "/vocabularies/{{name:Health}}"
    And I see the text "{{name:Health}}"
    And I see the columns "Version, Status, Date created, Description, Change log, Actions"
    And the version rows are "3, 2, 1"
    And the row of version 1 shows "Deprecated"
    And I see the text "Back to vocabulary list"

  @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35
  Scenario: Selecting a deprecated version shows all its metadata
    Given the vocabulary "Health" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Health}}"
    And I click "1" in the row of version 1
    Then I see the text "Initial version."
    And I see the text "{{email:ga-admin}}"
    And I see the text "Description of Health {{run}}"
    And the version panel shows "Deprecated"

  @AC_SIMPL-18452_AC2 @D29
  Scenario: The selected version is downloaded
    Given the vocabulary "Health" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Health}}"
    And I click "Download" in the row of version 1
    Then a file "{{name:Health}}-v1.ttl" is downloaded with the content of version 1 of "Health"

  @AC_SIMPL-18559_AC1 @D36
  Scenario: A deprecated version links to the latest ACTIVE version
    Given the vocabulary "Health" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Health}}"
    Then I see the text "This version is deprecated."
    And I see the text "Latest ACTIVE version: 2"

  @AC_SIMPL-18559_AC2 @D36
  Scenario: A deprecated version without an ACTIVE version shows only the warning
    Given the vocabulary "Health" exists
    And version 1 of "Health" is set to "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Health}}"
    Then I see the text "This version is deprecated."
    And I do not see the text "Latest ACTIVE version"

  @AC_SIMPL-18178_AC1 @D36 @D3
  Scenario: The details page of a deprecated vocabulary shows a banner
    Given the vocabulary "Health" exists
    And the vocabulary "Health" is deprecated
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Health}}"
    Then I see the text "This vocabulary is deprecated. Its use is discouraged."
    And I see the text "This version is deprecated."

  @D35 @negative
  Scenario: The details page of an unknown vocabulary
    Given I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/{{name:Ghost}}"
    Then I see the text "Vocabulary not found."
    And I see the text "Back to vocabulary list"

  @AC_SIMPL-18195_AC1 @D18 @D16
  Scenario: The admin sees every vocabulary management action
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I open the actions menu in the row of "Health"
    Then I see the control "New Vocabulary +"
    And I see the control "Update"
    And I see the control "Deprecate"

  @AC_SIMPL-18195_AC2 @AC_SIMPL-18176_AC1 @D18 @D16 @negative
  Scenario: A viewer does not see the restricted actions
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I open the actions menu in the row of "Health"
    Then I do not see the control "New Vocabulary +"
    And I do not see the control "Update"
    And I do not see the control "Deprecate"
    And I see the control "Download"

  @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario Outline: Opening a restricted form directly shows the authorization message
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "<user>"
    When I open the URL of the "<form>" form of "Health" directly
    Then I see the text "You are not authorized to perform this action."

    Examples:
      | user      | form        |
      | ga-viewer | create      |
      | ga-viewer | new version |
      | ga-norole | create      |

  @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: A user without a role is told they are not authorized
    Given I am signed in to the UI as "ga-norole"
    When I open the page "/vocabularies"
    Then I see the text "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a token is sent to the login page
    Given I am not signed in to the UI
    When I open the page "/vocabularies"
    Then I am sent to the login page of the identity provider
