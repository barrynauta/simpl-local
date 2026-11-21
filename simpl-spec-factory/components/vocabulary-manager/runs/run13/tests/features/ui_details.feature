Feature: UI vocabulary details and version history
  Screen Vocabulary details at /vocabularies/<Name> (D31, D35). Version panel D35 and D163,
  deprecation warnings D36, D64, D115, D118. Gherkin only; step 60 adds the browser steps.

  @ui @AC_SIMPL-18167_AC1 @AC_SIMPL-18176_AC2 @D35 @D60 @D72
  Scenario: History opens the details page with every version, newest first
    Given a vocabulary "Alpha" exists with 3 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "History"
    Then I am on the UI page "/vocabularies/{Alpha}"
    And I see the heading "{Alpha}"
    And the table has the column headers "Version, Status, Date created, Description, Change log, Actions"
    And the rows are ordered "3, 2, 1"
    And the row of "3" shows "Active" in the column "Status"
    And the row of "2" shows "Deprecated" in the column "Status"
    And the row of "1" shows "Deprecated" in the column "Status"
    And the row of "1" shows "Initial version." in the column "Change log"
    And the link "Back to vocabulary list" points to "/vocabularies"

  @ui @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D163 @D27
  Scenario: Selecting a deprecated version shows its metadata in a panel
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Alpha}"
    And I open version 1 in the version list
    Then I am on the UI page "/vocabularies/{Alpha}/versions/1"
    And the row of version 1 has aria-current "true"
    And the region "Version 1" contains "Deprecated"
    And the region "Version 1" contains "{admin_email}"
    And the region "Version 1" contains "Initial version."
    And the region "Version 1" contains "Description of {Alpha}."
    And the region "Version 1" contains a UTC date

  @ui @AC_SIMPL-18452_AC2 @D29
  Scenario: The download in the version panel gives the content of that version
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Alpha}/versions/1"
    And I click the button "Download"
    Then a file named "{Alpha}-v1.ttl" is downloaded with the content of version 1 of "Alpha"

  @ui @AC_SIMPL-18559_AC1 @D36 @D64 @D118
  Scenario: A deprecated version warns and links to the latest ACTIVE version
    Given a vocabulary "Alpha" exists with 3 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Alpha}/versions/1"
    Then the region "Version 1" contains "This version is deprecated."
    And the link "Latest ACTIVE version: 3" points to "/vocabularies/{Alpha}/versions/3"

  @ui @AC_SIMPL-18559_AC2 @D36 @D64
  Scenario: Without an ACTIVE version the warning has no link
    Given a vocabulary "Alpha" exists
    And version 1 of "Alpha" has the version status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Alpha}/versions/1"
    Then the region "Version 1" contains "This version is deprecated."
    And I do not see the text "Latest ACTIVE version"

  @ui @D36 @D64 @D3 @D115
  Scenario: Versions of a DEPRECATED vocabulary warn without a link and the page shows a banner
    Given a vocabulary "Alpha" exists with 2 versions
    And the vocabulary status of "Alpha" is "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Alpha}/versions/2"
    Then I see a status message containing "This vocabulary is deprecated. Its use is discouraged."
    And the region "Version 2" contains "This version is deprecated."
    And I do not see the text "Latest ACTIVE version"

  @ui @D64
  Scenario: A reactivated old version never links to itself
    Given a vocabulary "Alpha" exists with 2 versions
    And version 2 of "Alpha" has the version status "DEPRECATED"
    And version 1 of "Alpha" has the version status "ACTIVE"
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Alpha}/versions/2"
    Then the link "Latest ACTIVE version: 1" points to "/vocabularies/{Alpha}/versions/1"
    When I open the UI page "/vocabularies/{Alpha}/versions/1"
    Then the region "Version 1" does not contain "This version is deprecated."
    And the region "Version 1" does not contain "Latest ACTIVE version: 1"

  @ui @D35 @D60 @negative
  Scenario: An unknown vocabulary shows not found with a link back to the list
    Given I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies/{Ghost}"
    Then I see an alert containing "Vocabulary not found."
    And the link "Back to vocabulary list" points to "/vocabularies"

  @ui @D35 @D72 @D114 @negative
  Scenario: A load error of the history shows the history text and the problem detail
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-viewer"
    And the API answers GET "/vocabularies/{Alpha}/versions" with the status 500 and the detail "Stubbed history failure {tok}."
    When I open the UI page "/vocabularies/{Alpha}"
    Then I see exactly one alert
    And I see an alert containing "The version history could not be loaded."
    And I see an alert containing "Stubbed history failure {tok}."

  @ui @AC_SIMPL-18167_AC1 @D35 @D85
  Scenario: The details page loads further version pages when there are more than 100 versions
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-viewer"
    And the API answers GET "/vocabularies/{Alpha}/versions" with a version list of total 101
    When I open the UI page "/vocabularies/{Alpha}"
    Then the version list requested page 2 with pageSize 100
    And the version list shows 101 rows
