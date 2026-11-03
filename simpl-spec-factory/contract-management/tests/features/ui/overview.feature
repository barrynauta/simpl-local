@ui
Feature: Contract overview screen
  SIMPL-30959, D71 to D74.

  @SIMPL-30959 @D71 @D72 @stub @consumer
  Scenario: SIMPL-30959 AC1 - the Consumer overview shows the overview columns of an offer
    Given an offer created at the consumer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the overview
    Then the element "contracts-table" is visible
    And the row of the negotiation shows "cell-negotiation-id" as "${nid}"
    And the row of the negotiation shows "cell-resource-description-id" as "${rdId}"
    And the row of the negotiation shows "cell-resource-name" as "${resourceName}"
    And the row of the negotiation shows "cell-counterparty-name" as "Provider One"
    And the row of the negotiation shows "cell-status" as "Requested"

  @SIMPL-30959 @D71 @D73 @stub @consumer
  Scenario: SIMPL-30959 AC1 - an offer in Requested offers accept and reject, and a button to the details
    Given an offer created at the consumer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the overview
    Then the element "open-details-${nid}" is visible
    When I click the button "open-details-${nid}" of the negotiation
    Then the element "action-accept" is visible
    And the element "action-reject" is visible

  @SIMPL-30959 @D71 @stub @consumer
  Scenario Outline: SIMPL-30959 AC1 - the overview filters by <filter>
    Given an offer created at the consumer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the overview
    And I filter the overview by "<filter>" with "<value>"
    And I click "filter-apply"
    Then the row of the negotiation is visible

    Examples:
      | filter                   | value           |
      | filter-negotiation-id    | ${nid}          |
      | filter-resource-name     | ${resourceName} |
      | filter-counterparty-name | provider one    |
      | filter-status            | Requested       |

  @SIMPL-30959 @D71 @D72
  Scenario: SIMPL-30959 AC2 - an empty overview still shows the table header
    Given fresh negotiation ids
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the overview
    And I filter the overview by "filter-negotiation-id" with "${nid}"
    And I click "filter-apply"
    Then the element "contracts-empty" is visible
    And the table header shows the columns "Contract negotiation ID, Resource description ID, Resource name, Provider name, Status"

  @SIMPL-30959 @D74 @rbac @oidc
  Scenario Outline: SIMPL-30959 AC3 - a <party> user without the role cannot open the contract manager UI
    When I log in to the <party> UI as "<user>"
    Then the only visible element is "access-denied"
    And the element "contracts-table" is absent

    Examples:
      | party    | user            |
      | provider | provider-norole |
      | consumer | consumer-norole |

  @SIMPL-35692 @D44 @D72 @stub @two-party
  Scenario: SIMPL-35692 AC4 - an expired offer is shown as Terminated
    Given an offer waiting for the consumer decision
    And the consumer UIBE runs the offer expiry as of the timeout plus 1 minute for the negotiation
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the overview
    Then the row of the negotiation shows "cell-status" as "Terminated"
