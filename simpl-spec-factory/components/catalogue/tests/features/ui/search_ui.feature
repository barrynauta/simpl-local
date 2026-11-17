@ui @bp06 @D70
Feature: Search UI on ui-a
  Quick and advanced search, catalogue selection, quality filters and result tabs. Selectors
  of digest section 7. Behaviour of D71 to D74.

  Background:
    Given I am logged in to the UI as "consumer-a"

  # ------------------------------------------------------------------ catalogue selection (D71)

  @SIMPL-31102 @D71
  Scenario: The quick search tab offers the own and the federated catalogues
    When I open the "quick-search-tab" tab
    Then the browser requested "/api/v1/catalogues"
    And "catalogue-option-local" is visible
    And "catalogue-option-local" is checked
    And "catalogue-option-catalogue-b" is visible
    And "catalogue-option-catalogue-c" is visible
    And "catalogue-option-catalogue-slow" is visible
    And "catalogue-option-catalogue-d" is disabled
    And "catalogue-error" is not visible

  @SIMPL-31103 @D71
  Scenario: The advanced search tab offers the own and the federated catalogues
    When I open the "advanced-search-tab" tab
    Then the browser requested "/api/v1/catalogues"
    And "catalogue-option-local" is checked
    And "catalogue-option-catalogue-b" is visible
    And "catalogue-option-catalogue-d" is disabled

  @SIMPL-31102 @D71
  Scenario: A failing catalogue list leaves only the own catalogue and shows an error
    Given the browser request to "/api/v1/catalogues" fails
    When I open the "quick-search-tab" tab
    Then "catalogue-error" is visible
    And "catalogue-option-local" is visible
    And "catalogue-option-catalogue-b" is not visible

  @SIMPL-31103 @D71
  Scenario: A failing catalogue list on the advanced tab shows an error
    Given the browser request to "/api/v1/catalogues" fails
    When I open the "advanced-search-tab" tab
    Then "catalogue-error" is visible
    And "catalogue-option-catalogue-b" is not visible

  @D71
  Scenario: At least one catalogue must be selected to search
    When I open the "quick-search-tab" tab
    And I type "anything" into "search-input"
    And I uncheck "catalogue-option-local"
    Then "search-button" is disabled

  # ------------------------------------------------------------------ quality filters (D72)

  @SIMPL-29292 @D72
  Scenario: The quick search tab has the quality filters
    When I open the "quick-search-tab" tab
    Then "min-score-input" is visible
    And "min-classification-select" is visible
    And "min-classification-select" has the options ", Red, Yellow, Green" in this order

  @SIMPL-29293 @D72
  Scenario: The advanced search tab has the quality filters
    When I open the "advanced-search-tab" tab
    Then "min-score-input" is visible
    And "min-classification-select" has the options ", Red, Yellow, Green" in this order

  @SIMPL-29292 @D72
  Scenario Outline: An invalid minimum score shows an error and blocks the search (<value>)
    When I open the "quick-search-tab" tab
    And I type "anything" into "search-input"
    And I type "<value>" into "min-score-input"
    Then "min-score-error" is visible
    And "search-button" is disabled

    Examples:
      | value |
      | 101   |
      | -1    |
      | high  |

  @SIMPL-29292 @D72 @D54
  Scenario: Quick search with a minimum score shows only matching own results
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    When I open the "quick-search-tab" tab
    And I type "{token}" into "search-input"
    And I type "80" into "min-score-input"
    And I click "search-button"
    Then "results-panel-catalogue-a" shows the result row of "green"
    And "results-panel-catalogue-a" does not show the result row of "yellow"

  @SIMPL-29292 @D72
  Scenario: Empty quality filters are not sent
    Given "provider-a" has published a "DataOffering" resource description "green"
    When I open the "quick-search-tab" tab
    And I type "{token}" into "search-input"
    And I click "search-button"
    Then the browser requested "/api/v1/selfDescriptions?q={token}"
    And no browser request to "/api/v1/selfDescriptions" had the parameter "minimumScore"
    And no browser request to "/api/v1/selfDescriptions" had the parameter "minimumClassification"
    And "results-panel-catalogue-a" shows the result row of "green"

  @SIMPL-29293 @D72 @D54
  Scenario: Advanced search with a minimum classification shows only matching own results
    Given "provider-a" has published a "DataOffering" resource description "green"
    And a "DataOffering" resource description "yellow" by "provider-a"
    And the quality stub scores "yellow" with "overall=75"
    And "provider-a" has published "yellow"
    When I open the "advanced-search-tab" tab
    And I select "DataOfferingShape" in "schema-select"
    And I type "{token}" into "adv-field-GeneralServiceProperties-name"
    And I select "Green" in "min-classification-select"
    And I click "search-button"
    Then "results-panel-catalogue-a" shows the result row of "green"
    And "results-panel-catalogue-a" does not show the result row of "yellow"

  # ------------------------------------------------------------------ results (D73)

  @SIMPL-31141 @SIMPL-31142 @D73
  Scenario: Results come in one tab per catalogue with main properties
    Given "provider-a" has published a "DataOffering" resource description "own"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When I open the "quick-search-tab" tab
    And I check "catalogue-option-catalogue-b"
    And I type "{token}" into "search-input"
    And I click "search-button"
    Then the result tabs are "catalogue-a, catalogue-b" in this order
    And "results-tab-catalogue-a" contains the text "Data space A catalogue"
    And "results-tab-catalogue-b" contains the text "Data space B catalogue"
    And "results-panel-catalogue-a" shows the result row of "own"
    And "results-panel-catalogue-a" does not show the result row of "partner"
    And the result row of "own" in "results-panel-catalogue-a" shows "result-score"
    And the result row of "own" in "results-panel-catalogue-a" shows "result-classification"
    When I click "results-tab-catalogue-b"
    Then "results-panel-catalogue-b" shows the result row of "partner"
    And the result row of "partner" in "results-panel-catalogue-b" does not show "result-score"

  @SIMPL-31140 @D73 @D39
  Scenario: A failing federated catalogue shows a message in its tab
    Given "provider-a" has published a "DataOffering" resource description "own"
    When I open the "quick-search-tab" tab
    And I check "catalogue-option-catalogue-c"
    And I type "{token}" into "search-input"
    And I click "search-button"
    Then "results-panel-catalogue-a" shows the result row of "own"
    When I click "results-tab-catalogue-c"
    Then "group-status-catalogue-c" contains the text "Data space C catalogue"
    And "results-panel-catalogue-c" shows 0 result rows

  @SIMPL-31140 @D73
  Scenario: An empty catalogue shows No results
    When I open the "quick-search-tab" tab
    And I type "{token}" into "search-input"
    And I click "search-button"
    Then "group-status-catalogue-a" shows the text "No results"

  @SIMPL-31142 @D73 @D38
  Scenario: Paging one tab leaves the other tabs alone
    Given "provider-a" has published a "DataOffering" resource description "first"
    And "provider-a" has published a "DataOffering" resource description "second"
    And "provider-a" has published a "DataOffering" resource description "third"
    And "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When I open the "quick-search-tab" tab
    And I check "catalogue-option-catalogue-b"
    And I type "{token}" into "search-input"
    And I click "search-button"
    Then "page-indicator-catalogue-a" shows the text "Page 1 of 1"
    When I click "results-tab-catalogue-b"
    Then "page-indicator-catalogue-b" shows the text "Page 1 of 1"

  # ------------------------------------------------------------------ advanced form (D74)

  @SIMPL-35874 @D74
  Scenario: With only the own catalogue the user picks a RESOURCE schema
    When I open the "advanced-search-tab" tab
    Then "schema-select" is visible
    And "schema-select" offers the choices "DataOfferingShape, ApplicationOfferingShape, InfrastructureOfferingShape"
    When I select "DataOfferingShape" in "schema-select"
    Then "adv-field-GeneralServiceProperties-name" is visible
    And "adv-field-ProviderInformation-providedBy" is visible
    And "adv-field-OfferingPrice-price-op1" is visible
    And "adv-field-OfferingPrice-price-value1" is visible
    And "adv-field-OfferingPrice-price-op2" is visible
    And "adv-field-OfferingPrice-price-value2" is visible

  @SIMPL-35874 @D74
  Scenario: With a federated catalogue the form uses the SEARCH schema without a choice
    When I open the "advanced-search-tab" tab
    And I check "catalogue-option-catalogue-b"
    Then the browser requested "/api/v2/schemas?schemaType=SEARCH"
    And "schema-select" is not visible
    And "adv-field-GeneralServiceProperties-name" is visible
    And "adv-field-OfferingPrice-price-op1" is visible
    And "adv-field-ProviderInformation-providedBy" is not visible

  @SIMPL-35874 @D74
  Scenario: Unchecking the federated catalogue rebuilds the form
    When I open the "advanced-search-tab" tab
    And I check "catalogue-option-catalogue-b"
    And I uncheck "catalogue-option-catalogue-b"
    Then "schema-select" is visible

  @SIMPL-35874 @D74 @D29
  Scenario: The advanced form sends only filled fields in the D29 body
    When I open the "advanced-search-tab" tab
    And I select "DataOfferingShape" in "schema-select"
    And I type "{token}" into "adv-field-GeneralServiceProperties-name"
    And I select ">=" in "adv-field-OfferingPrice-price-op1"
    And I type "50" into "adv-field-OfferingPrice-price-value1"
    And I click "search-button"
    Then the last advanced search request body is the JSON
      """
      {"catalogueIds": ["local"], "simpl:GeneralServiceProperties": {"name": "{token}"}, "simpl:OfferingPrice": {"price": {"op1": ">=", "value1": 50}}}
      """
