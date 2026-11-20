@ui
Feature: Vocabulary list in the UI
  Screen 'Vocabulary list' at /vocabularies (D31, D34, D72, D100, D115). Step 60 adds the browser steps.
  Data is created through the API with the scenario id; the UI is filtered on it where a list is checked.

  @AC_SIMPL-18161_AC1 @D100 @D75 @D60 @D58
  Scenario: The list shows each vocabulary with the decided columns
    Given a vocabulary "Alpha" exists with description "Alpha description"
    And I am logged in to the UI as a vocabulary viewer
    When I open the menu item "Vocabularies"
    And I type the scenario id into the filter "Name contains"
    Then I see a table with the column headers "Name, Status, Description, Creator, Date created, Last updated, Actions"
    And the row of "${Alpha}" shows the status "Active", the description "Alpha description" and the creator "${EMAIL:admin}"
    And the row of "${Alpha}" shows the date created and last updated as "YYYY-MM-DD HH:mm UTC"
    And the row of "${Alpha}" has a button "Actions for ${Alpha}"

  @AC_SIMPL-18161_AC1 @AC_SIMPL-18165_AC1 @D72 @D18
  Scenario: The row actions menu of an administrator
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary administrator
    And I am on the vocabulary list filtered on the scenario id
    When I click the button "Actions for ${Alpha}"
    Then I see the menu items "View vocabulary details, History, Download vocabulary, Update, Deprecate"

  @AC_SIMPL-18161_AC2 @D34 @D72 @D114 @D24
  Scenario: A load error of the list shows one alert with the problem detail
    Given the API answers GET /vocabularies with 500 and the problem detail "Store query failed"
    And I am logged in to the UI as a vocabulary viewer
    When I open the vocabulary list
    Then I see one alert with the text "The vocabulary list could not be loaded." followed by "Store query failed"

  @AC_SIMPL-18161_AC2 @D44 @D72
  Scenario: A 503 while loading the list shows the list load error, not the generic text
    Given the API answers GET /vocabularies with 503 and the problem detail "The vocabulary store is not available. Retry later."
    And I am logged in to the UI as a vocabulary viewer
    When I open the vocabulary list
    Then I see one alert with the text "The vocabulary list could not be loaded." followed by "The vocabulary store is not available. Retry later."
    And I do not see the text "Something went wrong. Retry later or contact support."

  @D34
  Scenario: An empty list says that no vocabularies were found
    Given I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list
    When I type "Zz${SID}none" into the filter "Name contains"
    Then I see the text "No vocabularies found."

  @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: Filtering on part of the name
    Given a vocabulary "ShaclShapes" exists
    And a vocabulary "Other" exists
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list
    When I type "${SID}shacl" into the filter "Name contains"
    Then the list shows only "${ShaclShapes}"

  @AC_SIMPL-18163_AC1 @D3 @D72
  Scenario: Filtering on the vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the status of vocabulary "Bravo" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Deprecated" in the filter "Vocabulary status"
    Then the list shows only "${Bravo}"

  @AC_SIMPL-18163_AC1 @D20 @D72
  Scenario: Filtering on the creation date
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    When I choose the UTC date of the creation date of "${Alpha}" in the filter "Created on"
    Then the list shows only "${Alpha}"
    When I choose the date "2001-01-01" in the filter "Created on"
    Then I see the text "No vocabularies found."

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: Clicking the active sort column reverses the order
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    Then the list shows "${Alpha}, ${Bravo}" in this order
    When I click the column header "Name"
    Then the list shows "${Bravo}, ${Alpha}" in this order
    When I click the column header "Name"
    Then the list shows "${Alpha}, ${Bravo}" in this order

  @AC_SIMPL-18163_AC2 @D23 @D50 @D128
  Scenario: Clicking another sortable column sorts it ascending first
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the status of vocabulary "Alpha" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    When I click the column header "Status"
    Then the list shows "${Bravo}, ${Alpha}" in this order
    When I click the column header "Status"
    Then the list shows "${Alpha}, ${Bravo}" in this order

  @AC_SIMPL-18163_AC2 @D23
  Scenario: Only the decided columns are sortable
    Given I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list
    Then the column headers "Name, Status, Date created, Last updated" are sortable
    And the column headers "Description, Creator, Actions" are not sortable

  @D34 @D115
  Scenario: The list pages on the server with 20 rows per page
    Given 21 vocabularies exist with the scenario id in their name
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    Then I see 20 rows and the text "Page 1 of 2"
    When I click the button "Next page"
    Then I see 1 row and the text "Page 2 of 2"
    When I click the button "Previous page"
    Then I see the text "Page 1 of 2"

  @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A DEPRECATED vocabulary shows a warning icon in the list
    Given a vocabulary "Old" exists
    And the status of vocabulary "Old" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    Then the row of "${Old}" shows the status "Deprecated"
    And the row of "${Old}" has a warning icon with the accessible name and tooltip "This vocabulary is deprecated. Its use is discouraged."

  @AC_SIMPL-18178_AC1 @D36 @D115
  Scenario: The details page of a DEPRECATED vocabulary shows the deprecation banner
    Given a vocabulary "Old" exists
    And the status of vocabulary "Old" is changed to "DEPRECATED"
    And I am logged in to the UI as a vocabulary viewer
    When I open the details page of "${Old}"
    Then I see an element with role status and the text "This vocabulary is deprecated. Its use is discouraged."

  @AC_SIMPL-18165_AC1 @D29 @D72
  Scenario: Download vocabulary downloads the latest version
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    When I choose "Download vocabulary" in the menu "Actions for ${Alpha}"
    Then a file "${Alpha}.ttl" is downloaded with the content of version 2 of "Alpha"

  @AC_SIMPL-18165_AC1 @D29 @D35 @D72
  Scenario: View vocabulary details shows the full details of the latest version
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    When I choose "View vocabulary details" in the menu "Actions for ${Alpha}"
    Then I am on the page "/vocabularies/${Alpha}"
    And I see the vocabulary name, its status "Active" and the description of version 1 in the header

  @D60 @Q6 @NFR-10 @a11y
  Scenario: The list page has no accessibility violations and works by keyboard
    Given a vocabulary "Alpha" exists
    And I am logged in to the UI as a vocabulary viewer
    And I am on the vocabulary list filtered on the scenario id
    Then axe reports no WCAG 2.1 AA violations
    And I can open "Actions for ${Alpha}" and choose "View vocabulary details" with the keyboard only

  @D123
  Scenario: The UI sends the decided Content-Security-Policy
    When I load the UI start page
    Then the response header "Content-Security-Policy" has default-src 'self', frame-ancestors 'none' and no 'unsafe-inline'
