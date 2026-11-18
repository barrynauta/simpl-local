Feature: Vocabulary details, version history and deprecated versions in the UI
  Screen: Vocabulary details at /vocabularies/<Name>, one screen with the header and the
  version history, newest first (D35). Effectively deprecated versions show a warning (D36).

  @ui @AC_SIMPL-18167_AC1 @D35 @D31 @D60
  Scenario: Opening the history shows the details page with all versions, newest first
    Given a vocabulary "{Alpha}" exists with 3 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I click "View vocabulary details" in the actions menu of "{Alpha}"
    Then I am on the UI page "/vocabularies/{Alpha}"
    And I see the text "{Alpha}"
    And I see the columns ["Version", "Status", "Date created", "Description", "Change log", "Actions"]
    And the version list shows the versions [3, 2, 1] in this order
    And the row of version 1 shows "Deprecated"
    And the row of version 3 shows "Active"
    And the row of version 1 shows "Initial version."

  @ui @D35 @D24 @negative
  Scenario: An unknown vocabulary shows 'Vocabulary not found.' with a link back
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Ghost}"
    Then I see the text "Vocabulary not found."
    And I see the action "Back to vocabulary list"

  @ui @D35 @D24 @negative
  Scenario: A history load error shows the error and its detail
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    And the API answers "GET" "/vocabularies/{Alpha}/versions" with status 500 for the UI
    When I open the UI page "/vocabularies/{Alpha}"
    Then I see the text "The version history could not be loaded."

  @ui @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC2 @D16 @D35
  Scenario: A viewer sees deprecated versions, labelled as deprecated
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Alpha}"
    Then the version list shows the versions [2, 1] in this order
    And the row of version 1 shows "Deprecated"

  @ui @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D27
  Scenario: Selecting a deprecated version shows all its metadata
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Alpha}"
    And I select version 1
    Then the version panel shows "1"
    And the version panel shows "Deprecated"
    And the version panel shows "Initial version."
    And the version panel shows "Description of {Alpha}."
    And the version panel shows "{creator:GA_VOCABULARY_ADMIN}"
    And the version panel shows "UTC"

  @ui @AC_SIMPL-18452_AC2 @D29
  Scenario: Downloading a selected version gives the file of that version
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Alpha}"
    And I click "Download" for version 1
    Then a file named "{Alpha}-v1.ttl" is downloaded with the content of version 1 of "{Alpha}"

  @ui @AC_SIMPL-18559_AC1 @D36 @D3
  Scenario: A deprecated version shows a warning with a link to the latest ACTIVE version
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Alpha}"
    Then the row of version 1 shows "This version is deprecated."
    And the text "Latest ACTIVE version: 2" links to version 2 of "{Alpha}"
    And the row of version 2 shows no warning

  @ui @AC_SIMPL-18559_AC2 @D36
  Scenario: Without an ACTIVE version the warning has no link
    Given a vocabulary "{Alpha}" exists with 2 versions
    And version 2 of "{Alpha}" has status "DEPRECATED"
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Alpha}"
    Then the row of version 2 shows "This version is deprecated."
    And I do not see the text "Latest ACTIVE version"

  @ui @AC_SIMPL-18178_AC1 @AC_SIMPL-18559_AC2 @D36 @D3
  Scenario: A DEPRECATED vocabulary shows the banner and every version as deprecated
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies/{Alpha}"
    Then I see the text "This vocabulary is deprecated. Its use is discouraged."
    And the row of version 1 shows "This version is deprecated."
