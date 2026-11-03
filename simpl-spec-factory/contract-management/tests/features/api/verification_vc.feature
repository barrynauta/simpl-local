Feature: Retrieval of the party's own signed VC
  GET /verifications/{nid}/vc returns this party's own signed VC, 404 before
  it is issued (SIMPL-35440, D5, D12, A.1).

  @SIMPL-35440 @D5 @D12
  Scenario: SIMPL-35440 AC2 - a contract that is not signed yet has no VC
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/verifications/${nid}/vc"
    Then the response is a problem with status 404 and type "notFound"

  @SIMPL-35440 @D5 @D12
  Scenario: SIMPL-35440 AC2 - an unknown negotiation has no VC
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/verifications/${nid}/vc"
    Then the response is a problem with status 404 and type "notFound"

  @SIMPL-35440 @D5 @D24 @stub @two-party
  Scenario: SIMPL-35440 AC1 - the Consumer returns its VC once it has signed
    Given a negotiation where the consumer has signed
    When I send "GET" to the consumer CM at "/verifications/${nid}/vc"
    Then the response status is 200
    And the response is the "CONSUMER" credential held in the consumer wallet
    And the response is a credential of D53 and D55 for the negotiation
