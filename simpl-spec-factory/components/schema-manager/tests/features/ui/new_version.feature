@ui @D46
Feature: Create New Version form
  SIMPL-13358, replaced in part by SIMPL-32808 (schema type) and SIMPL-20770
  (no version field). Decision D50.

  @SIMPL-20770 @SIMPL-32808 @D50 @D55
  Scenario: The form has changelog and file, a read-only schema type and no version field
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/$A/versions/new?schemaType=RESOURCE"
    Then "field-changelog" is visible
    And "field-file" is visible
    And "field-schema-type" is read-only and shows "RESOURCE"
    And "submit-button" is disabled

  @SIMPL-13358 @SIMPL-32808 @D50
  Scenario: A blank changelog or no file keeps the submit button disabled
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/$A/versions/new?schemaType=RESOURCE"
    And I fill "field-changelog" with "A change."
    Then "submit-button" is disabled
    When I choose the file "valid-resource-v2.ttl" in "field-file"
    Then "submit-button" is enabled
    When I clear "field-changelog"
    Then "submit-button" is disabled

  @SIMPL-13358 @SIMPL-32808 @SIMPL-20770 @D50
  Scenario: A new version is submitted without a version field and shown on the details page
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/$A/versions/new?schemaType=RESOURCE"
    And I fill "field-changelog" with "A change."
    And I choose the file "valid-resource-v2.ttl" in "field-file"
    And I click "submit-button"
    Then the UI sent the request "POST /schemas/$A/versions"
    And the browser shows the page "/schemas/$A?schemaType=RESOURCE"
    And "version-row-2" is visible

  @SIMPL-32808 @D50 @search-serial
  Scenario: A new version of a SEARCH schema keeps the schema type SEARCH
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/$S/versions/new?schemaType=SEARCH"
    Then "field-schema-type" is read-only and shows "SEARCH"

  @SIMPL-13358 @SIMPL-24172 @D50
  Scenario: An API error keeps me on the form with the detail at the top
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/$A/versions/new?schemaType=RESOURCE"
    And I fill "field-changelog" with "A change."
    And I choose the file "syntax-error.ttl" in "field-file"
    And I click "submit-button"
    Then "notification-error" shows "Syntax Error: "
    And the browser shows the page "/schemas/$A/versions/new?schemaType=RESOURCE"
