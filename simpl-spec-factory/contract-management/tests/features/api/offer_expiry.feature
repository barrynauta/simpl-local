@uibe @stub
Feature: Expiry of offers that are not decided in time
  A UIBE scheduler terminates offers in CREATED that are older than the
  timeout. CM first, then the UIBE table. Tests trigger a run with asOf
  instead of waiting (SIMPL-35692, D44, D82, B.4, E8).

  @SIMPL-35692 @bp07 @D44 @D82 @D37 @E8 @P4 @two-party
  Scenario: SIMPL-35692 AC1 - a pending offer older than the timeout is terminated at both parties
    Given an offer waiting for the consumer decision
    When the consumer UIBE runs the offer expiry as of the timeout plus 1 minute for the negotiation
    Then the response status is 200
    And the response field "expired" contains "${nid}"
    And the response field "skipped" is an empty list
    And the response field "failed" is an empty list
    And the consumer UIBE offer has status "TERMINATED"
    And the consumer contract reaches status "TERMINATED"
    And the consumer contract has:
      | field      | value         |
      | reasonCode | OFFER_EXPIRED |
    And the consumer contract history has an entry with:
      | field    | value      |
      | toStatus | TERMINATED |
      | trigger  | SCHEDULER  |
    And the stub received the callback "signed" with "false" from the consumer
    And the stub negotiation ends in state "TERMINATED" with reason "CONSUMER_DECLINED"
    And the provider contract reaches status "TERMINATED"

  @SIMPL-35692 @D44 @D82 @oidc @two-party
  Scenario: SIMPL-35692 AC4 - a terminated offer is shown with status TERMINATED
    Given an offer waiting for the consumer decision
    When the consumer UIBE runs the offer expiry as of the timeout plus 1 minute for the negotiation
    And I send "GET" to the consumer UIBE at "/contractOffers" as "consumer-admin"
    Then the item of the response list "." with "contractNegotiationId" "${nid}" has:
      | field  | value      |
      | status | TERMINATED |
    When I send "GET" to the consumer UIBE at "/contracts/${nid}" as "consumer-admin"
    Then the response fields are:
      | field       | value      |
      | status      | TERMINATED |
      | offerStatus | TERMINATED |

  @SIMPL-35692 @D44 @D82 @consumer
  Scenario: SIMPL-35692 AC2 - a recent pending offer is not terminated
    Given an offer created at the consumer
    When the consumer UIBE runs the offer expiry as of now for the negotiation
    Then the response status is 200
    And the response field "skipped" contains "${nid}"
    And the response field "expired" does not contain "${nid}"
    And the consumer UIBE offer still has status "CREATED"
    And the consumer contract stays in status "REQUESTED"

  @SIMPL-35692 @D44 @D82 @two-party
  Scenario: SIMPL-35692 AC3 - an old offer that was already decided is not changed
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    And the consumer accepts the offer
    And the consumer contract reaches status "SIGNED"
    When the consumer UIBE runs the offer expiry as of the timeout plus 1 minute for the negotiation
    Then the response status is 200
    And the response field "skipped" contains "${nid}"
    And the consumer UIBE offer still has status "ACCEPTED"
    And the consumer contract stays in status "SIGNED"

  @SIMPL-35692 @D44 @consumer
  Scenario: When CM refuses the expiry the offer is left unchanged and reported as failed
    Given an offer created at the consumer
    And the connector reports the offer status "REJECTED" to the consumer CM
    When the consumer UIBE runs the offer expiry as of the timeout plus 1 minute for the negotiation
    Then the response status is 200
    And the response field "failed" contains "${nid}"
    And the consumer UIBE offer still has status "CREATED"

  @SIMPL-35692 @D44 @consumer
  Scenario: CM terminates on a reject with reason EXPIRED instead of rejecting
    Given an offer created at the consumer
    When I send "PATCH" to the consumer CM at "/contractOffers/${nid}/reject?reason=EXPIRED"
    Then the response status is 200
    And the response field "status" is "INITIATED"
    And the consumer contract reaches status "TERMINATED"
    And the consumer contract has:
      | field      | value         |
      | reasonCode | OFFER_EXPIRED |
    And the stub received the callback "signed" with "false" from the consumer

  @SIMPL-35692 @D66 @rbac
  Scenario: The expiry trigger needs the UIBE API key
    When the offer expiry is run without an API key
    Then the response is a problem with status 401 and type "unauthorized"
