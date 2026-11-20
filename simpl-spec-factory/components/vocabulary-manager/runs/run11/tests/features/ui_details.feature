@ui
Feature: Vocabulary details and version history in the UI
  Screen 'Vocabulary details' at /vocabularies/<Name> with the version history (D35, D36, D64, D118).

  @AC_SIMPL-18167_AC1 @D35 @D72 @D75
  Scenario: History opens the details page with the version history, newest first
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    When I choose "History" in the menu "Actions for ${Alpha}"
    Then I am on the page "/vocabularies/${Alpha}"
    And I see "${Alpha}" in the header
    And I see a table with the column headers "Version, Status, Date created, Description, Change log, Actions"
    And the version rows are "2, 1" in this order
    And the row of version 2 shows the status "Active" and the row of version 1 the status "Deprecated"
    And the row of version 1 shows the change log "Initial version."
    And each version row has a button "Actions for version <n>" with the action "Download"

  @AC_SIMPL-18176_AC1 @D16 @D35
  Scenario: A viewer may open the version history, including deprecated versions
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Alpha}"
    Then I see the version rows "2, 1"

  @AC_SIMPL-18176_AC2 @D35 @D6
  Scenario: Deprecated versions are listed and labelled Deprecated
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has 3 versions
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Alpha}"
    Then the rows of versions 1 and 2 show the status "Deprecated"
    And the row of version 3 shows the status "Active"

  @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D75 @D27
  Scenario: Selecting a deprecated version opens its panel with all metadata
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    And I am on the details page of "${Alpha}"
    When I select version 1
    Then I see a region named "Version 1"
    And the region shows the version "1", the status "Deprecated", the date created as "YYYY-MM-DD HH:mm UTC", the creator "${EMAIL:admin}", the description and the change log "Initial version."
    And the region has a button "Download"

  @AC_SIMPL-18452_AC2 @D29
  Scenario: Download in the version panel downloads that version
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    And I am on the details page of "${Alpha}"
    When I select version 1
    And I click the button "Download" in the region "Version 1"
    Then a file "${Alpha}-v1.ttl" is downloaded with the content of version 1 of "Alpha"

  @AC_SIMPL-18559_AC1 @D36 @D64 @D118
  Scenario: A deprecated version links to the latest ACTIVE version
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    And I am on the details page of "${Alpha}"
    When I select version 1
    Then the region "Version 1" shows the warning "This version is deprecated."
    And the region "Version 1" has one link with the text "Latest ACTIVE version: 2"
    When I click the link "Latest ACTIVE version: 2"
    Then I see a region named "Version 2"
    And the region "Version 2" shows no deprecation warning

  @AC_SIMPL-18559_AC2 @D36
  Scenario: A deprecated version without any ACTIVE version shows only the warning
    Given a vocabulary "Alpha" exists
    And the status of version 1 of vocabulary "Alpha" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary viewer
    And I am on the details page of "${Alpha}"
    When I select version 1
    Then the region "Version 1" shows the warning "This version is deprecated."
    And the region "Version 1" has no link with a text starting "Latest ACTIVE version"

  @AC_SIMPL-18559_AC1 @D64 @D47 @D3
  Scenario: A version of a DEPRECATED vocabulary shows the warning without a link
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And the status of vocabulary "Alpha" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary viewer
    And I am on the details page of "${Alpha}"
    When I select version 2
    Then the region "Version 2" shows the warning "This version is deprecated."
    And the region "Version 2" has no link with a text starting "Latest ACTIVE version"

  @D36 @D64
  Scenario: The version rows of the history show the deprecation warning too
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Alpha}"
    Then the row of version 1 shows "This version is deprecated." and the link "Latest ACTIVE version: 2"
    And the row of version 2 shows no deprecation warning

  @D35 @D85
  Scenario: The history loads every version when there are more than 100
    Given a vocabulary "Many" exists
    And vocabulary "Many" has 101 versions
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Many}"
    Then I see 101 version rows

  @D35 @D60
  Scenario: The details page of an unknown vocabulary says it was not found
    Given I am logged in to the UI as a vocabulary viewer
    When I open the page "/vocabularies/${Missing}"
    Then I see the text "Vocabulary not found."
    And I see a link "Back to vocabulary list" that opens "/vocabularies"

  @D35 @D72 @D114
  Scenario: A load error of the history shows one alert with the problem detail
    Given a vocabulary "Alpha" exists
    And the API answers GET /vocabularies/${Alpha}/versions with 500 and the problem detail "Store query failed"
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Alpha}"
    Then I see one alert with the text "The version history could not be loaded." followed by "Store query failed"

  @Q6 @D35 @NFR-10 @a11y
  Scenario: The details page has no accessibility violations
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Alpha}"
    Then axe reports no WCAG 2.1 AA violations
    And I can select version 1 and reach "Download" with the keyboard only
