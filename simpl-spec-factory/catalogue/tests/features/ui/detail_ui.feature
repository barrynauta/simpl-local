@ui @wip @bp06 @detail
Feature: Detail page on ui-a
  One detail page for own and federated RDs (D75): every credentialSubject property as a path
  and value row, quality for own RDs only, an error and no rows on any error. No schema (D42).

  Background:
    Given I am logged in to the UI as "consumer-a"

  @SIMPL-31143 @D75 @D57
  Scenario: The detail of an own resource description shows all properties and the quality
    Given "provider-a" has published a "DataOffering" resource description "own"
    When I open the "quick-search-tab" tab
    And I type "{token}" into "search-input"
    And I click "search-button"
    And I open the result row of "own" in "results-panel-catalogue-a"
    Then the detail page shows a property row for every property of "own"
    And the detail page shows the property "simpl:generalServiceProperties/simpl:name" with the value "{token} own data"
    And "detail-quality-overall-score" shows the text "100"
    And "detail-quality-overall-classification" shows the text "Green"
    And "detail-quality-dimension-Completeness" is visible
    And "detail-quality-dimension-Accuracy" is visible
    And "detail-quality-dimension-Validity" is visible
    And "detail-error" is not visible

  @SIMPL-31148 @D75 @D42
  Scenario: The detail of a federated resource description looks the same without quality
    Given "provider-b" has published a "DataOffering" resource description "partner" discoverable by "PARTNER_CONSUMER"
    When I open the detail page of "partner" in the catalogue "catalogue-b"
    Then the detail page shows a property row for every property of "partner"
    And the detail page shows the property "simpl:providerInformation/simpl:providedBy" with the value "provider-b"
    And "detail-quality-overall-score" is not visible
    And "detail-error" is not visible

  @SIMPL-31148 @D75
  Scenario: A downstream failure shows an error and no details
    When I open the detail page of "missing" in the catalogue "catalogue-c"
    Then "detail-error" is visible
    And the detail page shows no property rows

  @SIMPL-31148 @D75
  Scenario: An invalid detail request shows an error and no details
    When I open the detail page of "missing" in the catalogue "catalogue-zz"
    Then "detail-error" is visible
    And the detail page shows no property rows

  @D75 @D30
  Scenario: A resource description the Consumer may not discover shows an error
    Given "provider-a" has published a "DataOffering" resource description "other" discoverable by "OTHER"
    When I open the detail page of "other" in the catalogue "catalogue-a"
    Then "detail-error" is visible
    And the detail page shows no property rows
