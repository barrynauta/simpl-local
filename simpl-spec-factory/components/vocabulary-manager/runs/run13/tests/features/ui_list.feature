Feature: UI vocabulary list
  Screen Vocabulary list at /vocabularies (D31). Columns D100 and D159, filters D72, sorting D23,
  D50, D163, paging D34 and D115, deprecation warning D36 and D72, texts D60, D72, D114.
  Gherkin only; step 60 adds the browser steps.

  @ui @AC_SIMPL-18161_AC1 @D100 @D159 @D60 @D72 @D75 @D58
  Scenario: The list shows every vocabulary with the decided columns and row actions
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    Then I see the heading "Vocabularies"
    And the table has the column headers "Name, Status, Latest version, Description, Creator, Date created, Last updated, Actions"
    And the row of "{Alpha}" shows "Active" in the column "Status"
    And the row of "{Alpha}" shows "2" in the column "Latest version"
    And the row of "{Alpha}" shows "Description of {Alpha} version 2." in the column "Description"
    And the row of "{Alpha}" shows "{admin_email}" in the column "Creator"
    And the row of "{Alpha}" shows a UTC date in the column "Date created"
    When I open the actions menu "Actions for {Alpha}"
    Then I see the text "View vocabulary details"
    And I see the text "History"
    And I see the text "Download vocabulary"
    And I see the text "Update"
    And I see the text "Deprecate"

  @ui @AC_SIMPL-18161_AC2 @D34 @D24 @D72 @D114 @negative
  Scenario: A load error says the list could not be loaded and shows the problem detail
    Given I am signed in to the UI as "ga-admin"
    And the API answers GET "/vocabularies" with the status 500 and the detail "Stubbed list failure {tok}."
    When I open the UI page "/vocabularies"
    Then I see exactly one alert
    And I see an alert containing "The vocabulary list could not be loaded."
    And I see an alert containing "Stubbed list failure {tok}."

  @ui @AC_SIMPL-18161_AC2 @D34 @D44 @D72 @negative
  Scenario: A 503 while loading shows the list text, not the generic text
    Given I am signed in to the UI as "ga-admin"
    And the API answers GET "/vocabularies" with the status 503 and the detail "Stubbed store outage {tok}."
    When I open the UI page "/vocabularies"
    Then I see an alert containing "The vocabulary list could not be loaded."
    And I see an alert containing "Stubbed store outage {tok}."
    And I do not see the text "Something went wrong. Retry later or contact support."

  @ui @D34
  Scenario: An empty result shows the empty state
    Given I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "Nothing{tok}"
    Then I see the text "No vocabularies found."

  @ui @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: The name filter shows only matching vocabularies
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "lpha{tok}"
    Then the rows are ordered "{Alpha}"

  @ui @AC_SIMPL-18163_AC1 @D72 @D3
  Scenario: The status filter shows only deprecated vocabularies
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the vocabulary status of "Bravo" is "DEPRECATED"
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I select "Deprecated" in "Vocabulary status"
    Then the rows are ordered "{Bravo}"

  @ui @AC_SIMPL-18163_AC1 @D20 @D72
  Scenario: The creation date filter shows the vocabularies created on that day
    Given a vocabulary "Alpha" exists
    And I remember the field "creationDate" of the vocabulary "Alpha" as "created"
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I fill in "Created on" with "{$created:date}"
    Then the rows are ordered "{Alpha}"

  @ui @AC_SIMPL-18163_AC2 @D23 @D50 @D163
  Scenario: A click on the active sort column reverses it and the header shows the direction
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    Then the rows are ordered "{Alpha}, {Bravo}"
    And the column header "Name" has aria-sort "ascending"
    When I click the column header "Name"
    Then the rows are ordered "{Bravo}, {Alpha}"
    And the column header "Name" has aria-sort "descending"

  @ui @AC_SIMPL-18163_AC2 @D23 @D50 @D163
  Scenario: A click on another sortable column sorts it ascending
    Given a vocabulary "Bravo" exists
    And I wait one second
    And a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I click the column header "Date created"
    Then the rows are ordered "{Bravo}, {Alpha}"
    And the column header "Date created" has aria-sort "ascending"
    And the column header "Name" has aria-sort "none"

  @ui @D34 @D115
  Scenario: The list pages on the server with 20 rows
    Given 21 vocabularies of this scenario exist
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    Then the list shows 20 rows
    And I see the button "Previous page"
    And I see the button "Next page"
    And I see the text "Page 1 of 2"
    When I click the button "Next page"
    Then the list shows 1 rows
    And I see the text "Page 2 of 2"

  @ui @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A deprecated vocabulary shows a warning icon in the list
    Given a vocabulary "Alpha" exists
    And the vocabulary status of "Alpha" is "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    Then the row of "{Alpha}" shows "Deprecated" in the column "Status"
    And the status icon of "{Alpha}" has the accessible name "This vocabulary is deprecated. Its use is discouraged."

  @ui @AC_SIMPL-18165_AC1 @D29
  Scenario: The download action downloads the latest version
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "Download vocabulary"
    Then a file named "{Alpha}.ttl" is downloaded with the content of version 2 of "Alpha"

  @ui @AC_SIMPL-18165_AC1 @D29 @D35
  Scenario: The details of the latest version can be reviewed from the list
    Given a vocabulary "Alpha" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "View vocabulary details"
    Then I am on the UI page "/vocabularies/{Alpha}"
    And I see the heading "{Alpha}"
    And I see the text "Description of {Alpha} version 2."

  @ui @AC_SIMPL-18174_AC1 @D60 @D3
  Scenario: The admin deprecates a vocabulary from the list without a confirmation
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "Deprecate"
    Then I see a status message containing "Status of {Alpha} changed to Deprecated."
    And the row of "{Alpha}" shows "Deprecated" in the column "Status"
    And the vocabulary "Alpha" has the vocabulary status "DEPRECATED" in the API

  @ui @AC_SIMPL-18174_AC1 @D60 @D4
  Scenario: The status action offers only the other status
    Given a vocabulary "Alpha" exists
    And the vocabulary status of "Alpha" is "DEPRECATED"
    And I am signed in to the UI as "ga-admin"
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    Then I see the text "Activate"
    And I do not see the text "Deprecate"
    When I choose the menu item "Activate"
    Then I see a status message containing "Status of {Alpha} changed to Active."
    And the row of "{Alpha}" shows "Active" in the column "Status"

  @ui @AC_SIMPL-18174_AC2 @D24 @D72 @D114 @negative
  Scenario: A vocabulary that is no longer available is reported and nothing changes
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    And the API answers PATCH "/vocabularies/{Alpha}" with the status 404
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "Deprecate"
    Then I see exactly one alert
    And I see an alert containing "Vocabulary not found. The status was not changed."
    And the row of "{Alpha}" shows "Active" in the column "Status"

  @ui @AC_SIMPL-18174_AC3 @D24 @D72 @D114 @D60 @negative
  Scenario Outline: A failed status change says the status could not be updated: <status>
    Given a vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    And the API answers PATCH "/vocabularies/{Alpha}" with the status <status>
    When I open the UI page "/vocabularies"
    And I fill in "Name contains" with "{tok}"
    And I open the actions menu "Actions for {Alpha}"
    And I choose the menu item "Deprecate"
    Then I see exactly one alert
    And I see an alert containing "The vocabulary status could not be updated."
    And I see an alert containing "Something went wrong. Retry later or contact support."
    And the row of "{Alpha}" shows "Active" in the column "Status"

    Examples:
      | status |
      | 500    |
      | 503    |
