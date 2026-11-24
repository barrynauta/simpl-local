Feature: Vocabulary details, version history and downloads in the UI
  Browser steps in features/steps/ui_steps.py (step 60). D29, D35, D36, D64, D72, D75, D115, D118, D163.
  D64's self-link case is not reachable: a version that shows the warning is effectively deprecated,
  so it is never the latest ACTIVE version of an ACTIVE vocabulary (D47); the DEPRECATED-vocabulary
  case of D64 is tested below.

  @ui @AC_SIMPL-18167_AC1 @D35 @D75 @D31
  Scenario: History opens the details page with the version list, newest first
    Given vocabulary "$Apple" has 3 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "History" in the menu "Actions for $Apple"
    Then I am on the route "/vocabularies/$Apple"
    And the header shows the name "$Apple", the status "Active" and the latest description
    And the version table has the column headers "Version, Status, Date created, Description, Change log, Actions"
    And the version rows are in the order "3, 2, 1"
    And the version statuses are "Active, Deprecated, Deprecated"

  @ui @D35 @D72
  Scenario: View vocabulary details opens the same page
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "View vocabulary details" in the menu "Actions for $Apple"
    Then I am on the route "/vocabularies/$Apple"
    And I see the link "Back to vocabulary list"

  @ui @D35
  Scenario: An unknown vocabulary shows the not-found message with a link back
    Given I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Missing"
    Then I see the text "Vocabulary not found."
    And I see the link "Back to vocabulary list"

  @ui @D35 @D72 @D114
  Scenario: A load error of the history shows the problem detail
    Given a vocabulary "$Apple" exists
    And the API answers 500 with detail "Broken" to GET /vocabularies/$Apple/versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple"
    Then one alert shows "The version history could not be loaded." followed by "Broken"

  @ui @D35 @D85
  Scenario: The history loads every version when there are more than 100
    Given vocabulary "$Apple" has 101 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple"
    Then the version table shows 101 rows

  @ui @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC2 @D35 @D16
  Scenario: A viewer sees deprecated versions labelled Deprecated
    Given vocabulary "$Apple" has 2 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple"
    Then the row of version 1 shows the status "Deprecated"

  @ui @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D163 @D75
  Scenario: Selecting a version opens its panel with all metadata
    Given vocabulary "$Apple" has 2 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple"
    And I select the version 1
    Then I am on the route "/vocabularies/$Apple/versions/1"
    And the row of version 1 has aria-current "true"
    And the region "Version 1" shows the version, status "Deprecated", date created, creator "ga-admin@example.com", description and change log
    And the region "Version 1" has the button "Download"

  @ui @AC_SIMPL-18452_AC2 @D29
  Scenario: Download in the version panel gives the content of that version
    Given vocabulary "$Apple" has 2 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple/versions/1"
    And I press the button "Download" in the region "Version 1"
    Then a file "$Apple-v1.ttl" is downloaded with the content of version 1 of "$Apple"

  @ui @AC_SIMPL-18165_AC1 @D29 @D72
  Scenario: Download vocabulary in the list gives the latest content
    Given vocabulary "$Apple" has 2 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "Download vocabulary" in the menu "Actions for $Apple"
    Then a file "$Apple.ttl" is downloaded with the content of version 2 of "$Apple"

  @ui @AC_SIMPL-18165_AC1 @D29
  Scenario: The full details of the latest version are shown on the details page
    Given vocabulary "$Apple" has 2 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple/versions/2"
    Then the region "Version 2" shows the version, status "Active", date created, creator "ga-admin@example.com", description and change log

  @ui @AC_SIMPL-18559_AC1 @D36 @D118 @D64
  Scenario: A deprecated version warns and links to the latest ACTIVE version
    Given vocabulary "$Apple" has 2 versions
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple/versions/1"
    Then the region "Version 1" shows "This version is deprecated."
    And the region "Version 1" has the link "Latest ACTIVE version: 2" to "/vocabularies/$Apple/versions/2"
    And the row of version 1 shows "This version is deprecated." and the link "Latest ACTIVE version: 2"

  @ui @AC_SIMPL-18559_AC2 @D36
  Scenario: Without an ACTIVE version only the deprecation warning is shown
    Given vocabulary "$Apple" has 2 versions
    And version 2 of vocabulary "$Apple" has version status "DEPRECATED"
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple/versions/1"
    Then the region "Version 1" shows "This version is deprecated."
    And the region "Version 1" has no "Latest ACTIVE version" link

  @ui @AC_SIMPL-18559_AC1 @AC_SIMPL-18178_AC1 @D36 @D64 @D115 @D3
  Scenario: A DEPRECATED vocabulary shows the banner and recommends no version
    Given vocabulary "$Apple" has 2 versions
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies/$Apple/versions/1"
    Then a status banner shows "This vocabulary is deprecated. Its use is discouraged."
    And the region "Version 1" shows "This version is deprecated."
    And the region "Version 1" has no "Latest ACTIVE version" link
