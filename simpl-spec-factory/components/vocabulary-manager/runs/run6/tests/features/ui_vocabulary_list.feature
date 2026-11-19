@ui
Feature: Vocabulary list in the UI
  Screen Vocabulary list at /vocabularies (D31, D34). Texts are the decided texts (D34, D36, D60).

  @AC_SIMPL-18161_AC1 @D5 @D31 @D34 @D60
  Scenario: The list shows each vocabulary with its metadata
    Given the vocabulary "Health" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    Then I see the text "Vocabularies"
    When I set the filter "Name" to "{{name:Health}}"
    Then I see the columns "Name, Description, Latest version, Status, Date created, Last updated, Creator"
    And the row of "Health" shows "2"
    And the row of "Health" shows "Active"
    And the row of "Health" shows "{{email:ga-admin}}"
    And the row of "Health" shows "UTC"

  @AC_SIMPL-18161_AC2 @D34 @D24 @negative
  Scenario: A load error is shown with the problem detail
    Given the API is stubbed to answer "GET" "/vocabularies" with status 500
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    Then I see the text "The vocabulary list could not be loaded."
    And I see the text "Internal Server Error"

  @D34
  Scenario: An empty list shows the empty state
    Given the API is stubbed to answer "GET" "/vocabularies" with status 200
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    Then I see the text "No vocabularies found."

  @AC_SIMPL-18163_AC1 @D19 @D3
  Scenario: The list is filtered on name and on status
    Given the vocabularies "Apple, Pineapple, Banana" exist
    And the vocabulary "Banana" is deprecated
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "apple{{runlower}}"
    Then the vocabulary list rows are "Apple, Pineapple"
    When I set the filter "Name" to "{{run}}"
    And I set the filter "Status" to "Deprecated"
    Then the vocabulary list rows are "Banana"

  @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: A click on a sortable column header sorts and a second click reverses
    Given the vocabularies "Cherry, Apple, Banana" exist, created one second apart
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{run}}"
    Then the vocabulary list rows are "Apple, Banana, Cherry"
    When I click the column header "Name"
    Then the vocabulary list rows are "Cherry, Banana, Apple"
    When I click the column header "Date created"
    Then the vocabulary list rows are "Cherry, Apple, Banana"
    When I click the column header "Date created"
    Then the vocabulary list rows are "Banana, Apple, Cherry"

  @AC_SIMPL-18178_AC1 @D36
  Scenario: A deprecated vocabulary shows a deprecation warning in the list
    Given the vocabulary "Old" exists
    And the vocabulary "Old" is deprecated
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Old}}"
    Then the row of "Old" shows "Deprecated"
    And the row of "Old" shows "This vocabulary is deprecated. Its use is discouraged."

  @AC_SIMPL-18165_AC1 @D29 @D5
  Scenario: The download action gives the latest version
    Given the vocabulary "Health" exists with 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Download" in the row of "Health"
    Then a file "{{name:Health}}.ttl" is downloaded with the content of version 2 of "Health"
    When I click "View vocabulary details" in the row of "Health"
    Then I am on the page "/vocabularies/{{name:Health}}"
    And I see the text "{{name:Health}}"

  @AC_SIMPL-18174_AC1 @D60 @D3
  Scenario: The admin deprecates and reactivates a vocabulary from the list
    Given the vocabulary "Health" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Deprecate" in the row of "Health"
    Then I see the text "Status of {{name:Health}} changed to Deprecated."
    And the row of "Health" shows "Deprecated"
    And the vocabulary "Health" has the status "DEPRECATED"
    When I click "Activate" in the row of "Health"
    Then I see the text "Status of {{name:Health}} changed to Active."
    And the vocabulary "Health" has the status "ACTIVE"

  @AC_SIMPL-18174_AC2 @D24 @negative
  Scenario: A status change for a vocabulary that is no longer available
    Given the vocabulary "Health" exists
    And the API is stubbed to answer "PATCH" "/vocabularies/{{name:Health}}" with status 404
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Deprecate" in the row of "Health"
    Then I see a message that the vocabulary could not be found
    And the row of "Health" shows "Active"

  @AC_SIMPL-18174_AC3 @D24 @D60 @negative
  Scenario: A failed status change leaves the status unchanged
    Given the vocabulary "Health" exists
    And the API is stubbed to answer "PATCH" "/vocabularies/{{name:Health}}" with status 500
    And I am signed in to the UI as "ga-admin"
    When I open the page "/vocabularies"
    And I set the filter "Name" to "{{name:Health}}"
    And I click "Deprecate" in the row of "Health"
    Then I see the text "Something went wrong. Retry later or contact support."
    And the row of "Health" shows "Active"
