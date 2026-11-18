Feature: Vocabulary list in the UI
  Gherkin only; step 60 adds the browser steps. Data is created through the API.
  Screens and texts: D31, D34, D36. Sorting: D23. Filters: D19.

  @ui @AC_SIMPL-18161_AC1 @D5 @D31 @D34
  Scenario: The list shows every vocabulary with its columns and row actions
    Given a vocabulary "Alpha" exists
    And I sign in to the UI as "ga-viewer"
    When I open the vocabulary list
    Then the vocabulary list shows the columns "Actions, Name, Description, Latest version, Status, Date created, Last updated, Creator"
    And the row of vocabulary "Alpha" shows "Active"
    And the row of vocabulary "Alpha" has the actions "View vocabulary details, Download vocabulary"

  @ui @AC_SIMPL-18161_AC2 @D34 @D24
  Scenario: A load error is reported with the problem detail
    Given the API is stubbed to answer 500 to "getVocabularies"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    Then I see "The vocabulary list could not be loaded."
    And I see "Internal Server Error"

  @ui @D34
  Scenario: An empty list shows a message
    Given the API is stubbed to answer 200 to "getVocabularies with no items"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    Then I see "No vocabularies found."

  @ui @AC_SIMPL-18163_AC1 @D19 @D3
  Scenario: Filters narrow the list
    Given a vocabulary "Shaclshapes" exists
    And a vocabulary "Other" exists
    And the vocabulary "Other" has status "DEPRECATED"
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I set the filter "Name" to "${runlower}shacl"
    Then the vocabulary list shows "Shaclshapes" in this order
    When I set the filter "Name" to "${run}"
    And I set the filter "Status" to "Deprecated"
    Then the vocabulary list shows "Other" in this order

  @ui @AC_SIMPL-18163_AC2 @D23
  Scenario Outline: A sortable column sorts ascending, then descending (<column>)
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I set the filter "Name" to "${run}"
    And I click the column header "<column>" <clicks> times
    Then the vocabulary list shows "<order>" in this order

    Examples:
      | column | clicks | order       |
      | Name   | 1      | Alpha, Beta |
      | Name   | 2      | Beta, Alpha |

  @ui @AC_SIMPL-18178_AC1 @D36 @D3
  Scenario: A deprecated vocabulary carries a warning in the list and on its details page
    Given a vocabulary "Old" exists
    And the vocabulary "Old" has status "DEPRECATED"
    And I sign in to the UI as "ga-viewer"
    When I open the vocabulary list
    Then the row of vocabulary "Old" shows "This vocabulary is deprecated. Its use is discouraged."
    When I open the details of vocabulary "Old"
    Then I see "This vocabulary is deprecated. Its use is discouraged."

  @ui @AC_SIMPL-18165_AC1 @D29 @D5
  Scenario: The download action gives the latest version
    Given a vocabulary "Health" exists with 2 versions
    And I sign in to the UI as "ga-admin"
    When I open the vocabulary list
    And I click "Download vocabulary" for vocabulary "Health"
    Then a file "${name:Health}.ttl" is downloaded with the content of version 2 of "Health"
    When I click "View vocabulary details" for vocabulary "Health"
    Then I see "${name:Health}"
