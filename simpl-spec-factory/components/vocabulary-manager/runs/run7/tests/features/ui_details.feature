@ui
Feature: Vocabulary details and version history in the UI
  One screen holds the vocabulary header and the version list (D35). Gherkin only; step 60 adds the steps.

  @AC_SIMPL-18167_AC1 @D35 @D72 @D75
  Scenario: The history action opens the details page with all versions newest first
    Given vocabulary "Alpha" has 3 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies"
    And I fill in "Name contains" with "${V:Alpha}"
    And I choose "History" in the actions menu "Actions for ${V:Alpha}"
    Then I am on the page "/vocabularies/${V:Alpha}"
    And I see the text "${V:Alpha}"
    And the table includes the columns "Version, Status, Date created, Description, Change log, Actions"
    And the version rows are "3, 2, 1"
    And the version row 1 shows "Initial version."

  @AC_SIMPL-18176_AC1 @AC_SIMPL-18176_AC2 @D16 @D35
  Scenario: A viewer sees deprecated versions labelled as deprecated
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    Then the version rows are "2, 1"
    And the version row 1 shows "Deprecated"
    And the version row 2 shows "Active"

  @AC_SIMPL-18176_AC3 @AC_SIMPL-18452_AC1 @D35 @D27 @D75
  Scenario: Opening a deprecated version shows all its metadata
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    And I open the version 1
    Then I see the region "Version 1"
    And the region "Version 1" shows "Deprecated"
    And the region "Version 1" shows "${EMAIL:ga-admin}"
    And the region "Version 1" shows "Initial version."
    And the region "Version 1" shows "Description of Alpha"
    And the region "Version 1" shows "UTC"

  @AC_SIMPL-18452_AC2 @D29
  Scenario: A selected version is downloaded as its Turtle file
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    And I choose "Download" in the actions menu "Actions for version 1"
    Then a file named "${V:Alpha}-v1.ttl" is downloaded with the content of version 1 of "Alpha"

  @AC_SIMPL-18559_AC1 @D36 @D64
  Scenario: A deprecated version links to the latest ACTIVE version
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    Then the version row 1 shows "This version is deprecated."
    And the version row 1 shows "Latest ACTIVE version: 2"
    And the version row 2 does not show "This version is deprecated."
    And the link "Latest ACTIVE version: 2" points to version 2 of "Alpha"

  @AC_SIMPL-18559_AC2 @D36 @D64
  Scenario: A deprecated version without an ACTIVE version shows only the warning
    Given vocabulary "Alpha" has 2 versions
    And version 2 of "Alpha" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    Then the version row 1 shows "This version is deprecated."
    And the version row 1 does not show "Latest ACTIVE version"

  @D36 @D47 @D64
  Scenario: A version of a DEPRECATED vocabulary shows no latest ACTIVE version link
    Given vocabulary "Alpha" has 2 versions
    And vocabulary "Alpha" has status "DEPRECATED"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    Then the version row 2 shows "This version is deprecated."
    And the version row 2 does not show "Latest ACTIVE version"
    And the version row 1 does not show "Latest ACTIVE version"

  @D35 @D60 @negative
  Scenario: The details page of an unknown vocabulary says so and links back
    Given I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Ghost}"
    Then I see the text "Vocabulary not found."
    And the link "Back to vocabulary list" leads to "/vocabularies"

  @D35 @D72 @negative
  Scenario: A load error of the history is shown with the problem detail
    Given vocabulary "Alpha" exists
    And the API answers GET "/vocabularies/${V:Alpha}/versions" with status 500
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    Then I see an alert "The version history could not be loaded."

  @Q6 @NFR-10 @a11y
  Scenario: The details page meets WCAG 2.1 AA
    Given vocabulary "Alpha" has 2 versions
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/vocabularies/${V:Alpha}"
    Then the page has no accessibility violations
