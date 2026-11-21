Feature: Vocabulary list in the UI
  Gherkin only; step 60 adds the browser steps (D31, D34, D36, D50, D72, D75, D100, D115, D159, D163).

  @ui @AC_SIMPL-18161_AC1 @D100 @D159 @D72 @D75 @D34 @D60
  Scenario: The list shows every vocabulary with the decided columns and row actions
    Given a vocabulary "{{Health}}" exists with description "Health terms"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then I see a table with the column headers "Name, Status, Latest version, Description, Creator, Date created, Last updated, Actions"
    And the row of "{{Health}}" shows "Active"
    And the row of "{{Health}}" shows "Health terms"
    And the row of "{{Health}}" shows "{{adminEmail}}"
    And the actions menu of "{{Health}}" offers "View vocabulary details, History, Download vocabulary, Update, Deprecate"

  @ui @D60
  Scenario: Dates are shown in UTC
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then the row of "{{Health}}" shows text matching "\d{4}-\d{2}-\d{2} \d{2}:\d{2} UTC"

  @ui @AC_SIMPL-18161_AC2 @D34 @D72 @D114 @negative
  Scenario Outline: A load error of the list shows the decided text and the problem detail (<code>)
    Given the API answers <code> with detail "Stub detail {{run}}" to "GET" "/vocabularies"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    Then there is exactly one alert
    And I see an alert containing "The vocabulary list could not be loaded."
    And I see an alert containing "Stub detail {{run}}"
    And I do not see the text "Something went wrong"

    Examples:
      | code |
      | 500  |
      | 503  |

  @ui @D34
  Scenario: An empty list shows the empty state
    Given the API answers 200 to "GET" "/vocabularies"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    Then I see the text "No vocabularies found."

  @ui @D34 @D115
  Scenario: The list pages on the server with 20 rows per page
    Given 21 numbered vocabularies "{{Page}}" exist
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then the vocabulary list shows 20 rows
    And I see the button "Previous page"
    And I see the button "Next page"
    And I see the text "Page 1 of 2"
    When I click the button "Next page"
    Then the vocabulary list shows 1 rows
    And I see the text "Page 2 of 2"

  @ui @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: The name filter shows only matching vocabularies
    Given these vocabularies exist
      | name      |
      | {{Alpha}} |
      | {{Beta}}  |
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "lpha{{run}}"
    Then the vocabulary list shows the names in the order "{{Alpha}}"

  @ui @AC_SIMPL-18163_AC1 @D72
  Scenario: The status filter shows only deprecated vocabularies
    Given these vocabularies exist
      | name      | status     |
      | {{Alpha}} | ACTIVE     |
      | {{Beta}}  | DEPRECATED |
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I select "Deprecated" in "Vocabulary status"
    Then the vocabulary list shows the names in the order "{{Beta}}"

  @ui @AC_SIMPL-18163_AC2 @D23 @D50 @D163
  Scenario: A click on the active sort column reverses it, a click on another column sorts it ascending
    Given these vocabularies exist
      | name   | status     |
      | {{Aa}} | DEPRECATED |
      | {{Bb}} | ACTIVE     |
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then the vocabulary list shows the names in the order "{{Aa}}, {{Bb}}"
    When I click the column header "Name"
    Then the vocabulary list shows the names in the order "{{Bb}}, {{Aa}}"
    When I click the column header "Status"
    Then the vocabulary list shows the names in the order "{{Bb}}, {{Aa}}"
    When I click the column header "Status"
    Then the vocabulary list shows the names in the order "{{Aa}}, {{Bb}}"

  @ui @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A DEPRECATED vocabulary shows a warning icon in the list
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then the row of "{{Health}}" shows "Deprecated"
    And the status icon of "{{Health}}" is named "This vocabulary is deprecated. Its use is discouraged."

  @ui @AC_SIMPL-18165_AC1 @D29
  Scenario: Download vocabulary gives the latest version's content
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "Download vocabulary" in the actions menu of "{{Health}}"
    Then a file named "{{Health}}.ttl" is downloaded with the content of version 2 of "{{Health}}"

  @ui @AC_SIMPL-18165_AC1 @D29 @D35
  Scenario: View vocabulary details shows the full details of the latest version
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "View vocabulary details" in the actions menu of "{{Health}}"
    Then I am on the page "/vocabularies/{{Health}}"

  @ui @AC_SIMPL-18174_AC1 @D60 @D3
  Scenario: Deprecate and activate a vocabulary from the list
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "Deprecate" in the actions menu of "{{Health}}"
    Then I see a status message "Status of {{Health}} changed to Deprecated."
    And the row of "{{Health}}" shows "Deprecated"
    And the actions menu of "{{Health}}" offers "Activate"
    When I choose "Activate" in the actions menu of "{{Health}}"
    Then I see a status message "Status of {{Health}} changed to Active."
    And the row of "{{Health}}" shows "Active"

  @ui @AC_SIMPL-18174_AC2 @D24 @D72 @D114 @negative
  Scenario: A status change for a vocabulary that is no longer available shows not found
    Given a vocabulary "{{Health}}" exists
    And the API answers 404 to "PATCH" "/vocabularies/{{Health}}"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "Deprecate" in the actions menu of "{{Health}}"
    Then I see an alert containing "Vocabulary not found. The status was not changed."
    And the row of "{{Health}}" shows "Active"

  @ui @AC_SIMPL-18174_AC3 @D72 @D114 @negative
  Scenario Outline: A failed status change keeps the status (<code>)
    Given a vocabulary "{{Health}}" exists
    And the API answers <code> to "PATCH" "/vocabularies/{{Health}}"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    And I choose "Deprecate" in the actions menu of "{{Health}}"
    Then I see an alert containing "The vocabulary status could not be updated."
    And I see an alert containing "Something went wrong. Retry later or contact support."
    And the row of "{{Health}}" shows "Active"

    Examples:
      | code |
      | 500  |
      | 503  |

  @ui @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: An admin sees the restricted actions
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then I see the button "New Vocabulary +"
    And the actions menu of "{{Health}}" offers "Update, Deprecate"

  @ui @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario: A user without a vocabulary role sees the authorisation message and no restricted action
    Given I am signed in to the UI as "ga-norole"
    When I open the page "/vocabularies"
    Then I see the text "You are not authorized to perform this action."
    And I do not see the button "New Vocabulary +"

  @ui @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario Outline: A user without the admin role does not see restricted actions (<user>)
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "<user>"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "{{run}}"
    Then I do not see the button "New Vocabulary +"
    And the actions menu of "{{Health}}" does not offer "Update"
    And the actions menu of "{{Health}}" does not offer "Deprecate"

    Examples:
      | user      |
      | ga-viewer |

  @ui @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario Outline: Opening a restricted form directly shows the authorisation message (<route>)
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the page "<route>"
    Then I see the text "You are not authorized to perform this action."

    Examples:
      | route                                   |
      | /vocabularies/new                       |
      | /vocabularies/{{Health}}/versions/new   |

  @ui @AC_SIMPL-18195_AC2 @D18 @negative
  Scenario: A 403 from the API shows the authorisation message
    Given the API answers 403 to "GET" "/vocabularies"
    And I am signed in to the UI as "ga-norole"
    When I open the page "/vocabularies"
    Then I see the text "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a token is sent to the login page
    Given I am not signed in to the UI
    When I open the page "/vocabularies"
    Then I am sent to the Keycloak login page

  @ui @a11y @Q6 @NFR-10
  Scenario Outline: The UI pages have no accessibility violations (<route>)
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "<route>"
    Then the page has no axe violations

    Examples:
      | route                                 |
      | /vocabularies                         |
      | /vocabularies/{{Health}}              |
      | /vocabularies/new                     |
      | /vocabularies/{{Health}}/versions/new |

  @ui @Q6 @NFR-10
  Scenario Outline: Every action is reachable by keyboard (<route>)
    Given a vocabulary "{{Health}}" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "<route>"
    Then every action on the page can be reached and used with the keyboard only

    Examples:
      | route                                 |
      | /vocabularies                         |
      | /vocabularies/{{Health}}              |
      | /vocabularies/new                     |
      | /vocabularies/{{Health}}/versions/new |
