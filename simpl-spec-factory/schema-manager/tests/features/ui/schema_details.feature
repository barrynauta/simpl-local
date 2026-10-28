@ui @D46
Feature: Schema details page with the versions table
  SIMPL-13346 and SIMPL-32787 (versions table), SIMPL-13365 and SIMPL-32797
  (download a version). Decisions D54 and D57.

  @SIMPL-13346 @SIMPL-32787 @D54
  Scenario: The details page shows the header and the versions table
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas/$A?schemaType=RESOURCE"
    Then the UI sent the request "GET /schemas/$A/versions?schemaType=RESOURCE"
    And "versions-table" has the column headers "Version,Status,Schema Type,Resource Type,Creation Date,Creator,Latest Change"
    And the rows of "versions-table" are ordered by "Version" descending
    And the row "version-row-2" shows "PUBLISHED" in the column "Status"
    And the row "version-row-1" shows "REVOKED" in the column "Status"
    And the row "version-row-1" shows "Initial version." in the column "Latest Change"
    And the row "version-row-1" shows "RESOURCE" in the column "Schema Type"

  @SIMPL-13346 @SIMPL-32787 @D54
  Scenario: The versions table sorts by Version and by Creation Date in the browser
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas/$A?schemaType=RESOURCE"
    And I click the column header "Version"
    Then the rows of "versions-table" are ordered by "Version" ascending
    When I click the column header "Creation Date"
    Then the rows of "versions-table" are ordered by "Creation Date" ascending
    And the UI sent no request to the API

  @SIMPL-32787 @D54 @search-serial
  Scenario: The details page of a SEARCH schema asks for SEARCH versions
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas/$S?schemaType=SEARCH"
    Then the UI sent the request "GET /schemas/$S/versions?schemaType=SEARCH"
    And the row "version-row-1" shows "SEARCH" in the column "Schema Type"

  @SIMPL-13365 @SIMPL-32797 @D57
  Scenario: Download a version from the versions table
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas/$A?schemaType=RESOURCE"
    And I click "action-download-version-1"
    Then the UI sent the request "GET /schemas/$A/1?schemaType=RESOURCE"
    And a file named "$A-1.ttl" is downloaded with the content uploaded for version "1" of schema "$A"
    And "notification-success" shows "Schema version 1 downloaded successfully."

  @SIMPL-32797 @D57
  Scenario: The download page has no schema type selection box
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas/$A?schemaType=RESOURCE"
    Then "field-schema-type" is not rendered

  @SIMPL-13358 @SIMPL-20530 @D48 @D54
  Scenario: The New Version button is shown to admins only
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-viewer"
    When I open the page "/schemas/$A?schemaType=RESOURCE"
    Then "action-new-version-$A" is not rendered
