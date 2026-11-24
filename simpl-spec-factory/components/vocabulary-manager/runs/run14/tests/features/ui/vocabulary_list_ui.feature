Feature: Vocabulary list in the UI
  Browser steps in features/steps/ui_steps.py (step 60). Texts and labels are the decided ones
  (D18, D34, D36, D50, D60, D72, D75, D100, D115, D159, D163).

  @ui @AC_SIMPL-18161_AC1 @D34 @D100 @D159 @D75 @D72 @D60
  Scenario: The Vocabulary list shows every vocabulary with the decided columns and row actions
    Given vocabularies "$Apple, $Banana" exist
    And I am logged in to the UI as "ga-viewer"
    When I open the menu item "Vocabularies"
    Then I am on the route "/vocabularies"
    When I filter "Name contains" with "$RUN"
    Then the table has the column headers "Name, Status, Latest version, Description, Creator, Date created, Last updated, Actions"
    And the row of "$Apple" shows the status "Active", the latest version "1" and the creator "ga-admin@example.com"
    And the dates in the row of "$Apple" have the form "YYYY-MM-DD HH:mm UTC"
    When I open the menu "Actions for $Apple"
    Then the menu offers "View vocabulary details", "History" and "Download vocabulary"

  @ui @AC_SIMPL-18161_AC2 @D34 @D72 @D114 @D24
  Scenario: A load error of the list is shown with the problem detail
    Given the API answers 500 with detail "Store exploded" to GET /vocabularies
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    Then one alert shows "The vocabulary list could not be loaded." followed by "Store exploded"

  @ui @D34 @D177
  Scenario: An empty list shows the empty state
    Given the API answers GET /vocabularies with an empty page
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    Then I see the text "No vocabularies found."
    And I do not see a page counter

  @ui @D34 @D115 @D22
  Scenario: The list pages on the server with 20 rows per page
    Given 21 vocabularies of this scenario exist
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$RUN"
    Then the table shows 20 rows
    And I see the text "Page 1 of 2"
    When I press the button "Next page"
    Then the table shows 1 row
    And I see the text "Page 2 of 2"
    When I press the button "Previous page"
    Then I see the text "Page 1 of 2"

  @ui @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: The name filter shows only the matching vocabularies
    Given vocabularies "$Apple, $Banana" exist
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple!lower"
    Then the table shows only "$Apple"

  @ui @AC_SIMPL-18163_AC1 @D3 @D72
  Scenario: The status filter shows only DEPRECATED vocabularies
    Given vocabularies "$Apple, $Banana" exist
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$RUN"
    And I choose "Deprecated" in the filter "Vocabulary status"
    Then the table shows only "$Apple"

  @ui @AC_SIMPL-18163_AC1 @D20 @D72
  Scenario: The creation date filter shows the vocabularies created on that day
    Given vocabularies "$Apple, $Banana" exist
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$RUN"
    And I set the filter "Created on" to the UTC day of the stored creationDate of "$Apple"
    Then the table shows "$Apple"
    And the table shows "$Banana" when its stored creationDate has the same UTC day

  @ui @AC_SIMPL-18163_AC2 @D23 @D50 @D163 @D128
  Scenario: A click on a sortable column header sorts and a second click reverses
    Given vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$RUN"
    Then the rows are in the order "$Apple, $Banana, $Cherry"
    And the column header "Name" has aria-sort "ascending"
    When I press the sort button of the column "Name"
    Then the rows are in the order "$Cherry, $Banana, $Apple"
    And the column header "Name" has aria-sort "descending"
    When I press the sort button of the column "Date created"
    Then the rows are in the order "$Cherry, $Apple, $Banana"
    And the column header "Date created" has aria-sort "ascending"
    And the column header "Name" has aria-sort "none"

  @ui @AC_SIMPL-18163_AC2 @D23 @D159
  Scenario: Only the decided columns are sortable
    Given I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    Then the columns "Name, Status, Date created, Last updated" have a sort button
    And the columns "Latest version, Description, Creator, Actions" have no sort button

  @ui @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A DEPRECATED vocabulary shows a warning icon in the list
    Given a vocabulary "$Apple" exists
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    Then the row of "$Apple" shows the status "Deprecated"
    And the row of "$Apple" has a warning icon named "This vocabulary is deprecated. Its use is discouraged."

  @ui @AC_SIMPL-18195_AC1 @D18 @D16
  Scenario: The admin sees the create, update and status actions
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    Then I see the button "New Vocabulary +"
    When I open the menu "Actions for $Apple"
    Then the menu offers "Update" and "Deprecate"

  @ui @AC_SIMPL-18195_AC2 @D18 @D16
  Scenario: A viewer does not see the restricted actions
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-viewer"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    Then I do not see the button "New Vocabulary +"
    When I open the menu "Actions for $Apple"
    Then the menu does not offer "Update", "Deprecate" or "Activate"

  @ui @AC_SIMPL-18195_AC2 @D18 @D74
  Scenario Outline: A viewer who opens a form route directly is told it is not authorised: <route>
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-viewer"
    When I open the route "<route>"
    Then I see the text "You are not authorized to perform this action."

    Examples:
      | route                            |
      | /vocabularies/new                |
      | /vocabularies/$Apple/versions/new |

  @ui @AC_SIMPL-18195_AC2 @D18 @D24
  Scenario: A 403 from the API shows the authorization message
    Given a vocabulary "$Apple" exists
    And the API answers 403 to PATCH /vocabularies/$Apple
    And I am logged in to the UI as "ga-admin"
    When I deprecate "$Apple" from the list and confirm
    Then I see the text "You are not authorized to perform this action."

  @ui @AC_SIMPL-18195_AC3 @D18
  Scenario: A user without a token is sent to the Keycloak login page
    Given I am not logged in to the UI
    When I open the route "/vocabularies"
    Then I am on the Keycloak login page

  @ui @AC_SIMPL-18174_AC1 @D60 @D209 @D3
  Scenario: The admin deprecates a vocabulary after confirming
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "Deprecate" in the menu "Actions for $Apple"
    Then a dialog "Update vocabulary status" asks "Do you want to deprecate '$Apple'?" with focus on "Cancel"
    When I press the button "Confirm"
    Then a status message shows "Status of $Apple changed to Deprecated."
    And the row of "$Apple" shows the status "Deprecated"
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"

  @ui @AC_SIMPL-18174_AC1 @D60 @D209
  Scenario: The admin activates a DEPRECATED vocabulary
    Given a vocabulary "$Apple" exists
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "Activate" in the menu "Actions for $Apple"
    Then a dialog "Update vocabulary status" asks "Do you want to activate '$Apple'?" with focus on "Cancel"
    When I press the button "Confirm"
    Then a status message shows "Status of $Apple changed to Active."
    And vocabulary "$Apple" has vocabulary status "ACTIVE"

  @ui @D209
  Scenario Outline: Cancelling the status dialog changes nothing and sends nothing: <how>
    Given a vocabulary "$Apple" exists
    And I am logged in to the UI as "ga-admin"
    When I open the route "/vocabularies"
    And I filter "Name contains" with "$Apple"
    And I choose "Deprecate" in the menu "Actions for $Apple"
    And I close the dialog with <how>
    Then no PATCH request was sent
    And vocabulary "$Apple" has vocabulary status "ACTIVE"

    Examples:
      | how             |
      | the Cancel button |
      | the Escape key  |

  @ui @AC_SIMPL-18174_AC2 @D24 @D72 @D114
  Scenario: A vocabulary that is no longer available is reported and no status change is shown
    Given a vocabulary "$Apple" exists
    And the API answers 404 to PATCH /vocabularies/$Apple
    And I am logged in to the UI as "ga-admin"
    When I deprecate "$Apple" from the list and confirm
    Then one alert shows "Vocabulary not found. The status was not changed."
    And the row of "$Apple" shows the status "Active"

  @ui @AC_SIMPL-18174_AC3 @D24 @D72 @D114 @D44
  Scenario Outline: A failed status change is reported and the status stays unchanged: <status>
    Given a vocabulary "$Apple" exists
    And the API answers <status> with detail "Broken" to PATCH /vocabularies/$Apple
    And I am logged in to the UI as "ga-admin"
    When I deprecate "$Apple" from the list and confirm
    Then one alert shows "The vocabulary status could not be updated." followed by "Something went wrong. Retry later or contact support." and "Broken"
    And the row of "$Apple" shows the status "Active"

    Examples:
      | status |
      | 500    |
      | 503    |
