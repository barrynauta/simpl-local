@ui @D46
Feature: Create Schema form
  SIMPL-13355 and SIMPL-32800 (form, schema type, three 409 paths), SIMPL-24172,
  SIMPL-24173 and SIMPL-24174 (validation messages shown from the problem
  detail). Decision D49.

  @SIMPL-32800 @D49
  Scenario: The form defaults to RESOURCE with the resource type enabled
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    Then "field-schema-type" has the value "RESOURCE"
    And "field-schema-type" offers the options "RESOURCE,SEARCH"
    And "field-resource-type" offers the options "Data,Application,Infrastructure"
    And "field-resource-type" is enabled
    And "submit-button" is disabled

  @SIMPL-32800 @D49
  Scenario: Choosing SEARCH disables and clears the resource type
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I select "Data" in "field-resource-type"
    And I select "SEARCH" in "field-schema-type"
    Then "field-resource-type" is disabled
    And "field-resource-type" is empty

  @SIMPL-13355 @SIMPL-32800 @D12 @D49
  Scenario: An invalid name keeps the submit button disabled
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Data" and file "valid-resource.ttl"
    And I fill "field-name" with "lower case"
    Then "submit-button" is disabled

  @SIMPL-13355 @SIMPL-32800 @D49
  Scenario: A successful create shows the success text and opens the details page
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Data" and file "valid-resource.ttl"
    And I click "submit-button"
    Then "notification-success" shows 'Schema "$A" created successfully.'
    And the browser shows the page "/schemas/$A?schemaType=RESOURCE"

  @SIMPL-32800 @D49 @search-serial
  Scenario: Creating a SEARCH schema sends no resource type
    Given no SEARCH schema is published
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the SEARCH schema "$S" with file "valid-search.ttl"
    And I click "submit-button"
    Then the UI sent the request "POST /schemas"
    And "notification-success" shows 'Schema "$S" created successfully.'

  @SIMPL-13355 @SIMPL-32800 @D49
  Scenario: A name clash shows the fixed text and stays on the form
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Data" and file "valid-resource.ttl"
    And I click "submit-button"
    Then "notification-error" shows "A schema with that name already exists."
    And the browser shows the page "/schemas/new"

  @SIMPL-32800 @D17 @D49 @search-serial
  Scenario: A second SEARCH schema shows the fixed text
    Given no SEARCH schema is published
    And the SEARCH schema "$S" exists
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the SEARCH schema "$T" with file "valid-search.ttl"
    And I click "submit-button"
    Then "notification-error" shows "A published federated search schema already exists."

  @SIMPL-24172 @SIMPL-13355 @D33 @D45
  Scenario: A syntax error shows the problem detail
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Data" and file "syntax-error.ttl"
    And I click "submit-button"
    Then "notification-error" shows "Syntax Error: "
    And the browser shows the page "/schemas/new"

  @SIMPL-24173 @D33 @D45
  Scenario: Missing default properties show the problem detail
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Data" and file "missing-defaults.ttl"
    And I click "submit-button"
    Then "notification-error" shows "Default Properties Error: "

  @SIMPL-24174 @D33 @D45
  Scenario: A semantic error shows the problem detail
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Data" and file "semantic-unresolved-shape.ttl"
    And I click "submit-button"
    Then "notification-error" shows "Semantic Validation Error: "

  @SIMPL-24172 @SIMPL-24173 @SIMPL-24174 @D33
  Scenario: A valid schema shows no validation error
    Given I am signed in to the UI as "ga-admin"
    When I open the page "/schemas/new"
    And I fill the create form for the RESOURCE schema "$A" with resource type "Infrastructure" and file "valid-resource.ttl"
    And I click "submit-button"
    Then "notification-error" is not rendered
