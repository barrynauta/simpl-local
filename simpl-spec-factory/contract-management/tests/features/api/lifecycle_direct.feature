Feature: Lifecycle actions on contracts that are not established
  Lifecycle actions exist only for established contracts (D37, D40, A.4).

  @SIMPL-31115 @D37 @D40
  Scenario: A lifecycle action on a REQUESTED Provider contract is refused with 409
    Given a contract requested at the provider
    When the provider user performs "SUSPEND" with reason "Too early"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the provider contract stays in status "REQUESTED"

  @SIMPL-31118 @D42 @D40
  Scenario: The Provider cannot terminate a contract that is still being established
    Given a contract requested at the provider
    When the provider user performs "TERMINATE" with reason "Too early"
    Then the response is a problem with status 409 and type "invalidTransition"

  @openapi-only @D6 @D12
  Scenario: A lifecycle action on an unknown negotiation gives 404
    Given fresh negotiation ids
    When the provider user performs "SUSPEND" with reason "x"
    Then the response is a problem with status 404 and type "notFound"
