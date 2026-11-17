@ui @stub
Feature: Contract details and the offer decision screen
  SIMPL-30960, SIMPL-32505, D71 to D75.

  @SIMPL-30960 @D71 @D72 @D75 @consumer
  Scenario: SIMPL-30960 AC1 - the details show the offer fields and the human-readable offer
    Given an offer created at the consumer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the element "contract-details" is visible
    And the element "detail-negotiation-id" shows "${nid}"
    And the element "detail-resource-description-id" shows "${rdId}"
    And the element "detail-resource-name" shows "${resourceName}"
    And the element "detail-provider-name" shows "Provider One"
    And the element "detail-consumer-name" shows "Consumer One"
    And the element "detail-status" shows "Requested"
    And the element "human-readable" contains "${resourceName}"

  @SIMPL-30960 @D71
  Scenario: SIMPL-30960 AC1 - the details have a button back to the overview
    Given a contract requested at the provider
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    And I click "back-to-overview"
    Then the element "contracts-table" is visible

  @SIMPL-30960 @D71 @consumer
  Scenario: The offer URL of the notification opens the same details
    Given an offer created at the consumer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the offer URL of the negotiation
    Then the element "detail-negotiation-id" shows "${nid}"
    And the element "action-accept" is visible

  @SIMPL-30960 @D75 @consumer
  Scenario: The human-readable offer is shown sanitised
    Given a fresh asset with:
      | field        | value                      |
      | resourceName | <img src=x onerror=alert(1)> |
    And an offer created at the consumer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the human-readable offer shows no script and no external resource

  @SIMPL-32505 @D71 @D73 @C5 @two-party
  Scenario: SIMPL-32505 AC1 and AC2 - the Consumer accepts the offer and sees Accepted or a later status
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the element "action-accept" is visible
    When I click "action-accept"
    # C5: signing starts at once (D24), so a later establishment status also satisfies AC2
    Then the element "detail-status" shows one of "Accepted, Signed, Active"

  @SIMPL-32505 @D73 @two-party @slow
  Scenario Outline: SIMPL-32505 AC3 - there is no accept button for a contract that is <label>
    Given an active contract between both parties that is now "<status>"
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the element "action-accept" is absent
    And the element "detail-status" shows "<label>"

    Examples:
      | status     | label      |
      | ACTIVE     | Active     |
      | SUSPENDED  | Suspended  |
      | TERMINATED | Terminated |

  @SIMPL-32505 @D73 @two-party
  Scenario: SIMPL-32505 AC3 - there is no accept button for a rejected offer
    Given an offer waiting for the consumer decision
    And the consumer rejects the offer
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the element "action-accept" is absent
    And the element "detail-status" shows "Rejected"

  @SIMPL-35660 @D71 @two-party
  Scenario: The Consumer rejects the offer in the UI and sees Rejected
    Given an offer waiting for the consumer decision
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    And I click "action-reject"
    Then the element "detail-status" shows "Rejected"
