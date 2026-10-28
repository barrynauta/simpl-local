@ui @D46
Feature: Schemas list page
  SIMPL-15837 and SIMPL-32790 (list, filters, sorting, errors), SIMPL-20067
  (download latest), D48 (role-dependent actions), D53 and D56. Test ids of D46.

  @SIMPL-15837 @SIMPL-32790 @D45 @D53
  Scenario: The list shows the columns of D53 and calls GET /schemas
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas"
    Then "schema-list-table" is visible
    And "schema-list-table" has the column headers "Schema Name,Title,Description,Schema Type,Resource Type,Latest Version,Status,Creation Date,Creator,Actions"
    And the UI sent the request "GET /schemas?schemaType=RESOURCE&page=1&pageSize=20"
    And "filter-schema-type" has the value "RESOURCE"

  @SIMPL-15837 @SIMPL-32790 @D53
  Scenario: Changing a filter reloads page 1 with the matching query
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    And I select "PUBLISHED" in "filter-status"
    Then the UI sent the request "GET /schemas?status=PUBLISHED&schemaType=RESOURCE&page=1&pageSize=20"
    When I select "Data" in "filter-resource-type"
    Then the UI sent the request "GET /schemas?status=PUBLISHED&resourceType=Data&schemaType=RESOURCE&page=1&pageSize=20"

  @SIMPL-32790 @D53
  Scenario: The SEARCH filter disables the resource type filter
    Given I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    And I select "SEARCH" in "filter-schema-type"
    Then "filter-resource-type" is disabled
    And the UI sent the request "GET /schemas?schemaType=SEARCH&page=1&pageSize=20"

  @SIMPL-15837 @D53
  Scenario: Sorting by name happens in the browser
    Given I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    And I click the column header "Schema Name"
    Then the rows of "schema-list-table" are ordered by "Schema Name" ascending
    And the UI sent no request to the API

  @SIMPL-15837 @SIMPL-32790 @D45 @D53
  Scenario: Clicking a name opens the details page
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    And I click "schema-row-$A"
    Then the browser shows the page "/schemas/$A?schemaType=RESOURCE"

  @SIMPL-15837 @D53 @search-serial
  Scenario: No match for the filters shows the empty state
    Given no SEARCH schema is published
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    And I select "SEARCH" in "filter-schema-type"
    And I select "PUBLISHED" in "filter-status"
    Then "empty-state" shows "No schemas match the selected filters."
    And "pagination-next" is not rendered

  @SIMPL-15837 @SIMPL-32790 @D48
  Scenario: A user without a schema role sees no table
    Given I am signed in to the UI as "ga-norole"
    When I open the page "/schemas"
    Then "error-state" shows "You do not have permission to view this page."
    And "schema-list-table" is not rendered

  @SIMPL-20530 @D48
  Scenario: A viewer sees downloads but no create or status actions
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    Then "action-download-$A" is visible
    And "create-schema-button" is not rendered
    And "action-revoke-$A" is not rendered
    And "action-new-version-$A" is not rendered

  @SIMPL-20067 @D56
  Scenario: Download the latest content from the list
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas"
    Then the first action of "schema-row-$A" is "action-download-$A"
    When I click "action-download-$A"
    Then the UI sent the request "GET /schemas/$A"
    And a file named "$A-2.ttl" is downloaded with the content uploaded for version "2" of schema "$A"
