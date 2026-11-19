@ui
Feature: Vocabulary list in the UI
  Gherkin only; step 60 adds the browser steps. Labels and texts are the decided ones (D60, D72, D75).

  @AC_SIMPL-18161_AC1 @D5 @D31 @D34 @D60 @D72 @D75
  Scenario: The list shows every vocabulary with its metadata and a row actions menu
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    Then the table includes the columns "Name, Description, Latest version, Status, Date created, Last updated"
    And the row of "Alpha" shows "${V:Alpha}"
    And the row of "Alpha" shows "Description of Alpha"
    And the row of "Alpha" shows "Active"
    And the row of "Alpha" shows "${EMAIL:ga-admin}"
    And the row of "Alpha" shows "UTC"
    And the actions menu "Actions for ${V:Alpha}" offers "View vocabulary details, History, Download vocabulary"

  @AC_SIMPL-18161_AC2 @D24 @D34 @D72 @negative
  Scenario: A load error of the list is shown with the problem detail
    Given the API answers GET "/vocabularies" with status 500 and detail "Stubbed list failure ${rid}"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    Then I see an alert "The vocabulary list could not be loaded."
    And I see the text "Stubbed list failure ${rid}"

  @D34
  Scenario: The list shows an empty state when nothing matches
    Given I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "Nothing${rid}"
    Then I see the text "No vocabularies found."

  @AC_SIMPL-18163_AC1 @D19 @D72
  Scenario: The name and status filters narrow the list
    Given vocabularies "Alpha, Beta" exist
    And vocabulary "Beta" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${rid}"
    Then the vocabulary rows are "Alpha, Beta"
    When I select "Deprecated" in "Vocabulary status"
    Then the vocabulary rows are "Beta"

  @AC_SIMPL-18163_AC1 @D20 @D72
  Scenario: The creation date filter narrows the list
    Given vocabulary "Alpha" exists
    And I save the field "creationDate" of vocabulary "Alpha" as "created"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${rid}"
    And I fill in "Created on" with "${SD:created}"
    Then the vocabulary rows are "Alpha"

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: A click on a sortable column sorts it; a click on the active sort column reverses it
    Given vocabularies "Alpha, Beta, Gamma" exist, created one second apart
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${rid}"
    Then the vocabulary rows are "Alpha, Beta, Gamma"
    When I click the column header "Name"
    Then the vocabulary rows are "Gamma, Beta, Alpha"
    When I click the column header "Date created"
    Then the vocabulary rows are "Alpha, Beta, Gamma"
    When I click the column header "Date created"
    Then the vocabulary rows are "Gamma, Beta, Alpha"

  @AC_SIMPL-18178_AC1 @D36 @D72
  Scenario: A deprecated vocabulary shows a warning in the list and on its details page
    Given vocabulary "Alpha" exists
    And vocabulary "Alpha" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    Then the warning icon of "Alpha" is named "This vocabulary is deprecated. Its use is discouraged."
    When I open the page "/vocabularies/${V:Alpha}"
    Then I see the text "This vocabulary is deprecated. Its use is discouraged."

  @AC_SIMPL-18165_AC1 @D29 @D72
  Scenario: The download action downloads the latest version and the details show its metadata
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "Download vocabulary" in the actions menu "Actions for ${V:Alpha}"
    Then a file named "${V:Alpha}.ttl" is downloaded with the content of version 2 of "Alpha"
    When I choose "View vocabulary details" in the actions menu "Actions for ${V:Alpha}"
    Then I am on the page "/vocabularies/${V:Alpha}"
    And the version row 2 shows "Active"

  @AC_SIMPL-18174_AC1 @D60 @D72
  Scenario: The status of a vocabulary is changed from the list
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "Deprecate" in the actions menu "Actions for ${V:Alpha}"
    Then I see a status message "Status of ${V:Alpha} changed to Deprecated."
    And the row of "Alpha" shows "Deprecated"
    And the actions menu "Actions for ${V:Alpha}" offers "Activate"
    When I choose "Activate" in the actions menu "Actions for ${V:Alpha}"
    Then I see a status message "Status of ${V:Alpha} changed to Active."

  @AC_SIMPL-18174_AC2 @D24 @D72 @negative
  Scenario: A status change of a vocabulary that is no longer available
    Given vocabulary "Alpha" exists
    And the API answers PATCH "/vocabularies/${V:Alpha}" with status 404
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "Deprecate" in the actions menu "Actions for ${V:Alpha}"
    Then I see an alert "Vocabulary not found. The status was not changed."
    And the row of "Alpha" shows "Active"

  @AC_SIMPL-18174_AC3 @D24 @D60 @D72 @negative
  Scenario: A failed status change keeps the status in the UI
    Given vocabulary "Alpha" exists
    And the API answers PATCH "/vocabularies/${V:Alpha}" with status 500
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "Deprecate" in the actions menu "Actions for ${V:Alpha}"
    Then I see an alert "The vocabulary status could not be updated."
    And I see the text "Something went wrong. Retry later or contact support."
    And the row of "Alpha" shows "Active"

  @AC_SIMPL-18195_AC1 @D16 @D18
  Scenario: An admin sees the create, update and status actions
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    Then I see the button "New Vocabulary +"
    And the actions menu "Actions for ${V:Alpha}" offers "Update, Deprecate"

  @AC_SIMPL-18195_AC2 @D16 @D18 @negative
  Scenario: A viewer does not see the restricted actions and a direct form URL is refused
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    Then I do not see the button "New Vocabulary +"
    And the actions menu "Actions for ${V:Alpha}" does not offer "Update, Deprecate, Activate"
    When I open the page "/vocabularies/new"
    Then I see the text "You are not authorized to perform this action."
    When I open the page "/vocabularies/${V:Alpha}/versions/new"
    Then I see the text "You are not authorized to perform this action."

  @AC_SIMPL-18195_AC3 @D18 @negative
  Scenario: A user without a token is sent to the login page
    Given I am not signed in to the UI
    When I open the page "/vocabularies"
    Then I am sent to the login page

  @Q6 @NFR-10 @a11y
  Scenario: The list page meets WCAG 2.1 AA and is usable by keyboard
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    Then the page has no accessibility violations
    When I move through the page with the keyboard only
    Then every action on the page was reachable by keyboard

  @D18 @D24 @negative
  Scenario: Any 403 from the API shows the not-authorized message
    Given vocabulary "Alpha" exists
    And the API answers PATCH "/vocabularies/${V:Alpha}" with status 403
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "Deprecate" in the actions menu "Actions for ${V:Alpha}"
    Then I see an alert "You are not authorized to perform this action."

  @D60
  Scenario: The menu item and the date format follow the decided texts
    Given vocabulary "Alpha" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    Then I see the menu item "Vocabularies"
    When I fill in "Name contains" with "${V:Alpha}"
    Then the row of "Alpha" shows a date matching "[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2} UTC"
