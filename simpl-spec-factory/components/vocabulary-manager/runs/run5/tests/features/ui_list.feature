Feature: Vocabulary list in the UI
  Screen: Vocabulary list at /vocabularies (D31, D34). Gherkin only in step 40; the
  browser steps come in step 60. Data is set up through the API.

  @ui @AC_SIMPL-18161_AC1 @D5 @D31 @D34 @D60
  Scenario: The list shows every vocabulary with its columns and actions
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    Then I see the columns ["Actions", "Name", "Description", "Latest version", "Status", "Date created", "Last updated", "Creator"]
    And the row of "{Alpha}" shows "2"
    And the row of "{Alpha}" shows the status "Active"
    And the row of "{Alpha}" shows "Description 1 of run {run}."
    And the actions menu of "{Alpha}" offers ["View vocabulary details", "Download vocabulary"]

  @ui @AC_SIMPL-18161_AC1 @D60
  Scenario: The menu item Vocabularies opens the list
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/"
    And I click "Vocabularies"
    Then I am on the UI page "/vocabularies"

  @ui @D34
  Scenario: The list pages on the server with 20 rows
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    Then the vocabulary list loads with page size 20

  @ui @D34
  Scenario: An empty result shows the empty state
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "Nomatch{run}"
    Then I see the text "No vocabularies found."

  @ui @AC_SIMPL-18161_AC2 @D24 @D34 @negative
  Scenario: A load error says the list could not be loaded and shows the detail
    Given I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    And the API answers "GET" "/vocabularies" with status 500 for the UI
    When I open the UI page "/vocabularies"
    Then I see the text "The vocabulary list could not be loaded."
    And I see the text "Internal Server Error"

  @ui @AC_SIMPL-18163_AC1 @D19 @D3
  Scenario: Filters narrow the list to matching vocabularies
    Given a vocabulary "{Alpha}" exists
    And a vocabulary "{Beta}" exists
    And the vocabulary "{Beta}" has status "DEPRECATED"
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{run}"
    And I filter the list by "Status" with "Deprecated"
    Then the list shows only the vocabularies ["{Beta}"]

  @ui @AC_SIMPL-18163_AC1 @D19
  Scenario: The name filter matches part of the name in any case
    Given a vocabulary "{Alpha}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{upper:Alpha}"
    Then the list shows only the vocabularies ["{Alpha}"]

  @ui @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario: Clicking a sortable column header sorts and a second click reverses
    Given a vocabulary "{Alpha}" exists
    And a vocabulary "{Beta}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{run}"
    Then the list shows the vocabularies ["{Alpha}", "{Beta}"] in this order
    When I click the column header "Name"
    Then the list shows the vocabularies ["{Beta}", "{Alpha}"] in this order
    When I click the column header "Name"
    Then the list shows the vocabularies ["{Alpha}", "{Beta}"] in this order

  @ui @AC_SIMPL-18163_AC2 @D23 @D50
  Scenario Outline: A sortable column sorts ascending on the first click (<column>)
    Given a vocabulary "{Alpha}" exists
    And I wait until the next second
    And a vocabulary "{Beta}" exists
    And I wait until the next second
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{run}"
    And I click the column header "<column>"
    Then the list shows the vocabularies <order> in this order

    Examples:
      | column       | order                  |
      | Status       | ["{Beta}", "{Alpha}"]  |
      | Date created | ["{Alpha}", "{Beta}"]  |
      | Last updated | ["{Beta}", "{Alpha}"]  |

  @ui @AC_SIMPL-18178_AC1 @D36
  Scenario: A deprecated vocabulary shows a deprecation warning in the list
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And a vocabulary "{Beta}" exists
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{run}"
    Then the row of "{Alpha}" shows a warning icon with the text "This vocabulary is deprecated. Its use is discouraged."
    And the row of "{Beta}" shows no warning

  @ui @AC_SIMPL-18165_AC1 @D29 @D5
  Scenario: The download action gives the latest version's content
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I click "Download vocabulary" in the actions menu of "{Alpha}"
    Then a file named "{Alpha}.ttl" is downloaded with the content of version 2 of "{Alpha}"

  @ui @AC_SIMPL-18165_AC1 @D29 @D35
  Scenario: View vocabulary details shows the full details of the latest version
    Given a vocabulary "{Alpha}" exists with 2 versions
    And I am logged in to the UI as a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I open the UI page "/vocabularies"
    And I filter the list by "Name" with "{Alpha}"
    And I click "View vocabulary details" in the actions menu of "{Alpha}"
    Then I am on the UI page "/vocabularies/{Alpha}"
    And I see the text "Description 1 of run {run}."
