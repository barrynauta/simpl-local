@ui @stub @two-party @slow
Feature: Lifecycle actions in the UI
  SIMPL-31053, 31076, 31115 to 31118, D71 to D73.

  @SIMPL-31115 @D71 @D72
  Scenario: SIMPL-31115 AC1 to AC3 - the Provider suspends with a reason and the Consumer sees Suspended
    Given an active contract between both parties
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    And I click "action-suspend"
    And I type "Breach of usage policy" into "reason-input"
    And I click "reason-submit"
    Then the element "detail-status" shows "Suspended"
    When I log in to the consumer UI as "consumer-admin"
    And I open the overview
    Then the row of the negotiation shows "cell-status" as "Suspended"

  @SIMPL-31115 @D71
  Scenario: The reason dialog refuses a blank reason
    Given an active contract between both parties
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    And I click "action-suspend"
    Then the reason submit is refused while the reason is blank

  @SIMPL-31116 @D71
  Scenario: SIMPL-31116 AC1 to AC3 - the Consumer requests reactivation with a justification
    Given an active contract between both parties that is now "SUSPENDED"
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    And I click "action-request-reactivation"
    And I type "Issue fixed" into "reason-input"
    And I click "reason-submit"
    Then the element "detail-status" shows "Reactivation requested"

  @SIMPL-31117 @D71
  Scenario Outline: SIMPL-31117 AC1 to AC3 - the Provider reactivates a contract that is <label>
    Given an active contract between both parties that is now "<status>"
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    And I click "action-reactivate"
    Then the element "detail-status" shows "Active"

    Examples:
      | status                 | label                  |
      | SUSPENDED              | Suspended              |
      | REACTIVATION_REQUESTED | Reactivation requested |

  @SIMPL-31053 @D71
  Scenario Outline: SIMPL-31053 AC1 to AC3 - the <party> requests revocation and both see Revocation requested
    Given an active contract between both parties
    And I am logged in to the <party> UI as "<user>"
    When I open the details of the negotiation
    And I click "action-request-revocation"
    And I type "No longer needed" into "reason-input"
    And I click "reason-submit"
    Then the element "detail-status" shows "Revocation requested"

    Examples:
      | party    | user           |
      | provider | provider-admin |
      | consumer | consumer-admin |

  @SIMPL-31076 @D71 @D73
  Scenario: SIMPL-31076 AC1 and AC2 - the responding Consumer accepts the revocation
    Given an active contract between both parties with a revocation requested by the provider
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the element "action-deny-revocation" is visible
    When I click "action-accept-revocation"
    Then the element "detail-status" shows "Revoked"

  @SIMPL-31076 @D71
  Scenario: SIMPL-31076 AC3 and AC4 - the responding Provider denies with a justification
    Given an active contract between both parties with a revocation requested by the consumer
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    And I click "action-deny-revocation"
    And I type "Contract terms still apply" into "reason-input"
    And I click "reason-submit"
    Then the element "detail-status" shows "Active"

  @SIMPL-31076 @D73
  Scenario: The requesting party sees no answer buttons
    Given an active contract between both parties with a revocation requested by the provider
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    Then the element "action-accept-revocation" is absent
    And the element "action-deny-revocation" is absent

  @SIMPL-31118 @D71
  Scenario: SIMPL-31118 AC1 to AC3 - the Provider terminates with a justification
    Given an active contract between both parties
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    And I click "action-terminate"
    And I type "Termination conditions met" into "reason-input"
    And I click "reason-submit"
    Then the element "detail-status" shows "Terminated"

  @SIMPL-31118 @D73
  Scenario Outline: SIMPL-31118 AC1 - no terminate button for a contract that is <label>
    Given an active contract between both parties that is now "<status>"
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    Then the element "action-terminate" is absent

    Examples:
      | status     | label      |
      | REVOKED    | Revoked    |
      | TERMINATED | Terminated |

  @SIMPL-31115 @D73
  Scenario: The Consumer sees no suspend or terminate button
    Given an active contract between both parties
    And I am logged in to the consumer UI as "consumer-admin"
    When I open the details of the negotiation
    Then the element "action-suspend" is absent
    And the element "action-terminate" is absent
    And the element "action-request-revocation" is visible
