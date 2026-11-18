Feature: Vocabulary details and version history in the UI
  One screen holds the header and the version history (D35). Warnings: D36.

  @ui @AC_SIMPL-18167_AC1 @D35 @D31
  Scenario: Opening the history shows the details page with every version, newest first
    Given a vocabulary "Health" exists with 3 versions
    And I sign in to the UI as "ga-viewer"
    When I open the vocabulary list
    And I click "View vocabulary details" for vocabulary "Health"
    Then I am on the UI address "/vocabularies/${name:Health}"
    And I see "${name:Health}"
    And the version list shows the columns "Version, Status, Date created, Description, Change log, Actions"
    And the version list shows the versions "3, 2, 1" in this order

  @ui @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC2 @D35 @D16
  Scenario: A viewer sees deprecated versions labelled as such
    Given a vocabulary "Health" exists with 2 versions
    And I sign in to the UI as "ga-viewer"
    When I open the details of vocabulary "Health"
    Then the row of version 1 shows "Deprecated"
    And the row of version 2 shows "Active"

  @ui @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D27
  Scenario: Selecting a deprecated version shows all its metadata
    Given a vocabulary "Health" exists with 2 versions
    And I sign in to the UI as "ga-viewer"
    When I open the details of vocabulary "Health"
    And I select version 1
    Then the version panel shows "Version, Status, Date created, Creator, Description, Change log, Download"
    And I see "${email:ga-admin}"

  @ui @AC_SIMPL-18452_AC2 @D29
  Scenario: A selected version is downloaded as its own file
    Given a vocabulary "Health" exists with 2 versions
    And I sign in to the UI as "ga-viewer"
    When I open the details of vocabulary "Health"
    And I select version 1
    And I click "Download"
    Then a file "${name:Health}-v1.ttl" is downloaded with the content of version 1 of "Health"

  @ui @AC_SIMPL-18559_AC1 @D36 @D5
  Scenario: A deprecated version links to the latest ACTIVE version
    Given a vocabulary "Health" exists with 2 versions
    And I sign in to the UI as "ga-viewer"
    When I open the details of vocabulary "Health"
    Then the row of version 1 shows "This version is deprecated."
    And the row of version 1 shows "Latest ACTIVE version: 2"
    And the link "Latest ACTIVE version: 2" leads to version 2

  @ui @AC_SIMPL-18559_AC2 @D36
  Scenario: Without an ACTIVE version only the deprecation warning is shown
    Given a vocabulary "Health" exists with 2 versions
    And version 2 of vocabulary "Health" has status "DEPRECATED"
    And I sign in to the UI as "ga-viewer"
    When I open the details of vocabulary "Health"
    Then the row of version 1 shows "This version is deprecated."
    And the row of version 1 does not show "Latest ACTIVE version"

  @ui @D36 @D3
  Scenario: The versions of a deprecated vocabulary are effectively deprecated
    Given a vocabulary "Health" exists
    And the vocabulary "Health" has status "DEPRECATED"
    And I sign in to the UI as "ga-viewer"
    When I open the details of vocabulary "Health"
    Then the row of version 1 shows "This version is deprecated."

  @ui @D35 @negative
  Scenario: An unknown vocabulary shows a not-found message with a way back
    Given I sign in to the UI as "ga-viewer"
    When I open the UI address "/vocabularies/${name:Missing}"
    Then I see "Vocabulary not found."
    And I see the action "Back to the list"
