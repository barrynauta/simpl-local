@ui
Feature: Vocabulary list and Vocabulary details in the UI
  Gherkin only; step 60 adds the browser steps. UI tests use roles, decided labels and decided
  texts only (D75). Test data is created through the API steps.

  @AC_SIMPL-18161_AC1 @D31 @D34 @D72 @D75 @D100 @D60
  Scenario: The vocabulary list shows every decided column and the row actions
    Given a vocabulary "A" exists
    And the user "ga-viewer" is signed in to the UI
    When the user opens the menu item "Vocabularies"
    Then the user is on the route "/vocabularies"
    And the user sees a table with the column headers "Name, Status, Description, Creator, Date created, Last updated, Actions"
    And the user filters the list with "Name contains" set to "${A}"
    And the user sees a row for "${A}" with status "Active", description "Description of A", creator "${email:ga-admin}" and dates as "YYYY-MM-DD HH:mm UTC"
    When the user opens the menu "Actions for ${A}"
    Then the user sees the menu items "View vocabulary details, History, Download vocabulary"

  @AC_SIMPL-18161_AC2 @D34 @D72 @D114 @D24 @negative
  Scenario: A list load error says the list could not be loaded and shows the problem detail
    Given the user "ga-viewer" is signed in to the UI
    And the user's API answers "GET /vocabularies" with 500 and detail "Store exploded"
    When the user opens the route "/vocabularies"
    Then the user sees one alert with the text "The vocabulary list could not be loaded." followed by "Store exploded"

  @D34
  Scenario: An empty list says no vocabularies were found
    Given the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${prefix}Nothing"
    Then the user sees the text "No vocabularies found."

  @D34 @D115
  Scenario: The list pages on the server with 20 rows per page
    Given the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    Then the user sees the buttons "Previous page, Next page" and a text "Page <n> of <m>"
    And the user sees at most 20 rows

  @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: Filter by name and by vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And vocabulary "Beta" has the status "DEPRECATED"
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${lower:Beta}"
    Then the user sees only the rows "${Beta}"
    When the user filters the list with "Name contains" set to "${prefix}"
    And the user filters the list with "Vocabulary status" set to "Deprecated"
    Then the user sees only the rows "${Beta}"

  @AC_SIMPL-18163_AC1 @D20 @D72
  Scenario: Filter by creation date
    Given a vocabulary "A" exists
    And the user "ga-viewer" is signed in to the UI
    And I remember the field "creationDate" of "/vocabularies/${A}" as "created"
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user filters the list with "Created on" set to the date of the remembered "created"
    Then the user sees only the rows "${A}"

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario Outline: Sort by the column <column>
    # Bravo is created first and deprecated last, more than 1 s apart, so every column differs.
    Given a vocabulary "Bravo" exists
    And I wait 1.1 seconds
    And a vocabulary "Alpha" exists
    And I wait 1.1 seconds
    And vocabulary "Bravo" has the status "DEPRECATED"
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${prefix}"
    And the user clicks the column header "<column>"
    Then the rows are in the order "<ascending>"
    When the user clicks the column header "<column>"
    Then the rows are in the order "<descending>"

    Examples:
      | column       | ascending          | descending         |
      | Status       | ${Alpha}, ${Bravo} | ${Bravo}, ${Alpha} |
      | Date created | ${Bravo}, ${Alpha} | ${Alpha}, ${Bravo} |
      | Last updated | ${Alpha}, ${Bravo} | ${Bravo}, ${Alpha} |

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: The default order is Name ascending and a first click on Name reverses it
    Given a vocabulary "Bravo" exists
    And a vocabulary "Alpha" exists
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${prefix}"
    Then the rows are sorted by "Name" ascending
    When the user clicks the column header "Name"
    Then the rows are sorted by "Name" descending

  @D23
  Scenario: Description, Creator and Actions are not sortable
    Given the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    Then the column headers "Description, Creator, Actions" are not sortable

  @AC_SIMPL-18178_AC1 @D36 @D72 @D115
  Scenario: A deprecated vocabulary shows a warning in the list and a banner on its details page
    Given a vocabulary "A" exists
    And vocabulary "A" has the status "DEPRECATED"
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    Then the row "${A}" has a warning icon named "This vocabulary is deprecated. Its use is discouraged."
    When the user opens the route "/vocabularies/${A}"
    Then the user sees a status banner with the text "This vocabulary is deprecated. Its use is discouraged."

  @AC_SIMPL-18165_AC1 @D29 @D72
  Scenario: Download the latest version from the list
    Given a vocabulary "A" exists with 2 versions
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "Download vocabulary" in the menu "Actions for ${A}"
    Then the user receives the file "${A}.ttl" with the content of version 2 of "A"
    When the user chooses "View vocabulary details" in the menu "Actions for ${A}"
    Then the user is on the route "/vocabularies/${A}"

  @AC_SIMPL-18167_AC1 @D35 @D72 @D75
  Scenario: History opens the details page with the version list, newest first
    Given a vocabulary "A" exists with 3 versions
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies"
    And the user filters the list with "Name contains" set to "${A}"
    And the user chooses "History" in the menu "Actions for ${A}"
    Then the user is on the route "/vocabularies/${A}"
    And the user sees the heading "${A}"
    And the user sees a table with the column headers "Version, Status, Date created, Description, Change log, Actions"
    And the version rows are "3, 2, 1"
    And the version row "1" shows the status "Deprecated" and the change log "Initial version."

  @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC2 @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D75
  Scenario: A deprecated version is labelled and its panel shows all metadata
    Given a vocabulary "A" exists with 2 versions
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${A}"
    Then the version row "1" shows the status "Deprecated"
    When the user selects the version row "1"
    Then the user sees a region "Version 1" with the version, status "Deprecated", date created, creator "${email:ga-admin}", description "Description of A", change log "Initial version." and a "Download" button

  @AC_SIMPL-18452_AC2 @D29
  Scenario: Download a selected version
    Given a vocabulary "A" exists with 2 versions
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${A}"
    And the user chooses "Download" in the menu "Actions for version 1"
    Then the user receives the file "${A}-v1.ttl" with the content of version 1 of "A"

  @AC_SIMPL-18559_AC1 @D36 @D64
  Scenario: A deprecated version links to the latest ACTIVE version
    Given a vocabulary "A" exists with 3 versions
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${A}"
    Then the version row "1" shows the warning "This version is deprecated." and a link "Latest ACTIVE version: 3"
    When the user follows the link "Latest ACTIVE version: 3"
    Then the user sees a region "Version 3"
    And the version row "3" shows no deprecation warning

  @AC_SIMPL-18559_AC2 @D36
  Scenario: A deprecated version without any ACTIVE version shows only the warning
    Given a vocabulary "A" exists
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${A}"
    Then the version row "1" shows the warning "This version is deprecated." and no "Latest ACTIVE version" line

  @D64 @D3
  Scenario: Versions of a DEPRECATED vocabulary show no Latest ACTIVE version line
    Given a vocabulary "A" exists with 2 versions
    And vocabulary "A" has the status "DEPRECATED"
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${A}"
    Then the version row "2" shows the warning "This version is deprecated." and no "Latest ACTIVE version" line
    And the version row "1" shows the warning "This version is deprecated." and no "Latest ACTIVE version" line

  @D35 @D72 @negative
  Scenario: Details of a vocabulary that does not exist
    Given the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${Missing}"
    Then the user sees the text "Vocabulary not found."
    And the user sees the link "Back to vocabulary list"

  @D35 @D72 @D114 @negative
  Scenario: A history load error shows the decided text and the problem detail
    Given a vocabulary "A" exists
    And the user "ga-viewer" is signed in to the UI
    And the user's API answers "GET /vocabularies/${A}/versions" with 503 and detail "Store not available"
    When the user opens the route "/vocabularies/${A}"
    Then the user sees one alert with the text "The version history could not be loaded." followed by "Store not available"

  @D35
  Scenario: The details page loads every version when there are more than 100
    Given a vocabulary "A" exists with 101 versions
    And the user "ga-viewer" is signed in to the UI
    When the user opens the route "/vocabularies/${A}"
    Then the version rows are 101 rows from "101" down to "1"

  @Q6 @NFR-10 @a11y
  Scenario Outline: The <page> page has no axe violations and every action is reachable by keyboard
    Given a vocabulary "A" exists
    And the user "ga-admin" is signed in to the UI
    When the user opens the route "<route>"
    Then the page has no WCAG 2.1 AA axe violations
    And every action on the page is reachable by keyboard

    Examples:
      | page              | route                           |
      | list              | /vocabularies                   |
      | details           | /vocabularies/${A}              |
      | create form       | /vocabularies/new               |
      | new version form  | /vocabularies/${A}/versions/new |
