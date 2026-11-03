Feature: Verification trigger outside the SIGNED state
  PUT /verifications needs a known Consumer record in SIGNED (D31, D12).

  @SIMPL-35677 @D12 @consumer
  Scenario: Verification for an unknown negotiation id is refused with 400
    Given a fresh unregistered asset id
    And fresh negotiation ids
    When the connector sends a credential to the consumer CM for the negotiation
    Then the response is a problem with status 400 and type "validationFailure"
    And the problem detail contains "Unknown contractNegotiationId"

  @SIMPL-35677 @D31 @consumer @stub
  Scenario: Verification of a contract that is still REQUESTED is refused with 400
    Given an offer created at the consumer
    When the connector sends a credential to the consumer CM for the negotiation
    Then the response is a problem with status 400 and type "validationFailure"
    And the consumer contract stays in status "REQUESTED"
