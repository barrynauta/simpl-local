@stub @two-party @slow
Feature: Lifecycle of an established usage contract
  Suspend, reactivate, revoke and terminate. The acting party changes its
  record and sends a lifecycle message; the other party applies the same
  transition. Both show the same status within 10 seconds (D37 L1 to L7,
  D39 to D42, D47, D48).

  @SIMPL-31115 @D37 @L1
  Scenario: SIMPL-31115 AC1 to AC3 - the Provider suspends an active contract and the Consumer sees it
    Given an active contract between both parties
    Then the provider contract allowed actions are "SUSPEND, REQUEST_REVOCATION, TERMINATE"
    When the provider user performs "SUSPEND" with reason "Breach of usage policy"
    Then the response status is 200
    And the response field "status" is "SUSPENDED"
    And the provider contract reaches status "SUSPENDED"
    And the consumer contract reaches status "SUSPENDED"
    And the consumer contract history has an entry with:
      | field      | value                  |
      | fromStatus | ACTIVE                 |
      | toStatus   | SUSPENDED              |
      | trigger    | COUNTERPARTY           |
      | reason     | Breach of usage policy |

  @SIMPL-31115 @D39 @D37
  Scenario: SIMPL-31115 AC2 - a suspension with an empty reason is refused with 400
    Given an active contract between both parties
    When the provider user performs "SUSPEND" with reason ""
    Then the response is a problem with status 400 and type "validationFailure"
    And the provider contract stays in status "ACTIVE"

  @SIMPL-31115 @D39
  Scenario: SIMPL-31115 AC2 - a suspension with a reason of only spaces is refused with 400
    Given an active contract between both parties
    When the provider user performs "SUSPEND" with reason "    "
    Then the response is a problem with status 400 and type "validationFailure"
    And the provider contract stays in status "ACTIVE"

  @SIMPL-31115 @D39
  Scenario: A suspension without a reason field is refused with 400
    Given an active contract between both parties
    When the provider user performs "SUSPEND" without a reason
    Then the response is a problem with status 400 and type "validationFailure"
    And the provider contract stays in status "ACTIVE"

  @SIMPL-31115 @D39
  Scenario: A reason longer than 1000 characters is refused with 400
    Given an active contract between both parties
    And the variable "long" is a text of 1001 characters
    When the provider user performs "SUSPEND" with reason "${long}"
    Then the response is a problem with status 400 and type "validationFailure"

  @SIMPL-31115 @D39
  Scenario: A reason of exactly 1000 characters is accepted
    Given an active contract between both parties
    And the variable "long" is a text of 1000 characters
    When the provider user performs "SUSPEND" with reason "${long}"
    Then the response status is 200

  @SIMPL-31115 @D37 @D40
  Scenario: The Consumer cannot suspend, the answer is 409
    Given an active contract between both parties
    Then the consumer contract allowed actions are "REQUEST_REVOCATION"
    When the consumer user performs "SUSPEND" with reason "Not allowed"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the consumer contract stays in status "ACTIVE"
    And the provider contract stays in status "ACTIVE"

  @SIMPL-31116 @D37 @L2
  Scenario: SIMPL-31116 AC1 to AC3 - the Consumer requests the reactivation of a suspended contract
    Given an active contract between both parties that is now "SUSPENDED"
    Then the consumer contract allowed actions are "REQUEST_REACTIVATION"
    And the provider contract allowed actions are "REACTIVATE, TERMINATE"
    When the consumer user performs "REQUEST_REACTIVATION" with reason "Issue fixed"
    Then the response status is 200
    And both contracts reach status "REACTIVATION_REQUESTED"

  @SIMPL-31116 @D39
  Scenario: SIMPL-31116 AC2 - a reactivation request needs a justification
    Given an active contract between both parties that is now "SUSPENDED"
    When the consumer user performs "REQUEST_REACTIVATION" without a reason
    Then the response is a problem with status 400 and type "validationFailure"
    And the consumer contract stays in status "SUSPENDED"

  @SIMPL-31116 @D37 @D40
  Scenario: The Provider cannot request a reactivation, the answer is 409
    Given an active contract between both parties that is now "SUSPENDED"
    When the provider user performs "REQUEST_REACTIVATION" with reason "Not allowed"
    Then the response is a problem with status 409 and type "invalidTransition"

  @SIMPL-31117 @D37 @L3
  Scenario Outline: SIMPL-31117 AC1 to AC3 - the Provider reactivates a contract that is <status>
    Given an active contract between both parties that is now "<status>"
    Then the provider contract allowed actions are "REACTIVATE, TERMINATE"
    When the provider user performs "REACTIVATE" without a reason
    Then the response status is 200
    And both contracts reach status "ACTIVE"

    Examples:
      | status                 |
      | SUSPENDED              |
      | REACTIVATION_REQUESTED |

  @SIMPL-31117 @D37 @D40
  Scenario: The Consumer cannot reactivate, the answer is 409
    Given an active contract between both parties that is now "REACTIVATION_REQUESTED"
    When the consumer user performs "REACTIVATE" with reason "Not allowed"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the consumer contract stays in status "REACTIVATION_REQUESTED"

  @SIMPL-31053 @D37 @D41 @L4
  Scenario Outline: SIMPL-31053 AC1 to AC3 - the <party> requests the revocation of an active contract
    Given an active contract between both parties
    Then the <party> contract allowed actions are "<before>"
    When the <party> user performs "REQUEST_REVOCATION" with reason "No longer needed"
    Then the response status is 200
    And both contracts reach status "REVOCATION_REQUESTED"
    And the provider contract has:
      | field                 | value       |
      | revocationRequestedBy | <requester> |
    And the consumer contract has:
      | field                 | value       |
      | revocationRequestedBy | <requester> |

    Examples:
      | party    | requester | before                                 |
      | provider | PROVIDER  | SUSPEND, REQUEST_REVOCATION, TERMINATE |
      | consumer | CONSUMER  | REQUEST_REVOCATION                     |

  @SIMPL-31053 @D39
  Scenario: SIMPL-31053 AC1 - a revocation request needs a reason
    Given an active contract between both parties
    When the consumer user performs "REQUEST_REVOCATION" without a reason
    Then the response is a problem with status 400 and type "validationFailure"
    And the consumer contract stays in status "ACTIVE"

  @SIMPL-31076 @D37 @D41 @L5
  Scenario Outline: SIMPL-31076 AC1, AC2 and AC4 - the <responder> accepts the revocation requested by the <requester>
    Given an active contract between both parties with a revocation requested by the <requester>
    Then the <responder> contract allowed actions are "<responderActions>"
    When the <responder> user performs "ACCEPT_REVOCATION" without a reason
    Then the response status is 200
    And both contracts reach status "REVOKED"
    And the provider contract field "revocationRequestedBy" is null
    And the consumer contract field "revocationRequestedBy" is null

    Examples:
      | requester | responder | responderActions                           |
      | provider  | consumer  | ACCEPT_REVOCATION, DENY_REVOCATION         |
      | consumer  | provider  | ACCEPT_REVOCATION, DENY_REVOCATION, TERMINATE |

  @SIMPL-31076 @D37 @D41 @L6
  Scenario Outline: SIMPL-31076 AC3 and AC4 - the <responder> denies the revocation with a justification
    Given an active contract between both parties with a revocation requested by the <requester>
    When the <responder> user performs "DENY_REVOCATION" with reason "Contract terms still apply"
    Then the response status is 200
    And both contracts reach status "ACTIVE"
    And the <requester> contract field "revocationRequestedBy" is null
    And the <requester> contract history has an entry with:
      | field    | value                      |
      | toStatus | ACTIVE                     |
      | trigger  | COUNTERPARTY               |
      | reason   | Contract terms still apply |

    Examples:
      | requester | responder |
      | provider  | consumer  |
      | consumer  | provider  |

  @SIMPL-31076 @D39
  Scenario: SIMPL-31076 AC3 - a denial needs a justification
    Given an active contract between both parties with a revocation requested by the provider
    When the consumer user performs "DENY_REVOCATION" without a reason
    Then the response is a problem with status 400 and type "validationFailure"
    And the consumer contract stays in status "REVOCATION_REQUESTED"

  @SIMPL-31076 @D41 @D40
  Scenario Outline: The requesting <requester> cannot answer its own revocation request (<action>)
    Given an active contract between both parties with a revocation requested by the <requester>
    When the <requester> user performs "<action>" with reason "Self answer"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the <requester> contract stays in status "REVOCATION_REQUESTED"

    Examples:
      | requester | action            |
      | provider  | ACCEPT_REVOCATION |
      | provider  | DENY_REVOCATION   |
      | consumer  | ACCEPT_REVOCATION |
      | consumer  | DENY_REVOCATION   |

  @SIMPL-31118 @D37 @D42 @L7
  Scenario Outline: SIMPL-31118 AC1 to AC3 - the Provider terminates a contract that is <status>
    Given an active contract between both parties that is now "<status>"
    Then the provider contract allowed actions contain "TERMINATE"
    When the provider user performs "TERMINATE" with reason "Termination conditions met"
    Then the response status is 200
    And both contracts reach status "TERMINATED"
    And the consumer contract history has an entry with:
      | field    | value                      |
      | toStatus | TERMINATED                 |
      | trigger  | COUNTERPARTY               |
      | reason   | Termination conditions met |

    Examples:
      | status                 |
      | ACTIVE                 |
      | SUSPENDED              |
      | REACTIVATION_REQUESTED |
      | REVOCATION_REQUESTED   |

  @SIMPL-31118 @D39
  Scenario: SIMPL-31118 AC2 - a termination needs a justification
    Given an active contract between both parties
    When the provider user performs "TERMINATE" without a reason
    Then the response is a problem with status 400 and type "validationFailure"
    And the provider contract stays in status "ACTIVE"

  @SIMPL-31118 @D37 @D40
  Scenario: The Consumer cannot terminate, the answer is 409
    Given an active contract between both parties
    When the consumer user performs "TERMINATE" with reason "Not allowed"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the consumer contract stays in status "ACTIVE"

  @SIMPL-31118 @D37 @D42
  Scenario Outline: SIMPL-31118 AC1 - a contract that is <status> cannot be terminated or changed
    Given an active contract between both parties that is now "<status>"
    Then the provider contract allowed actions are ""
    And the consumer contract allowed actions are ""
    When the provider user performs "<action>" with reason "Too late"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the provider contract stays in status "<status>"

    Examples:
      | status     | action             |
      | REVOKED    | TERMINATE          |
      | TERMINATED | TERMINATE          |
      | TERMINATED | REACTIVATE         |
      | REVOKED    | REQUEST_REVOCATION |

  @SIMPL-31115 @D37 @D40
  Scenario Outline: An action outside the table for the current state is refused with 409 (<action> on <status>)
    Given an active contract between both parties that is now "<status>"
    When the <party> user performs "<action>" with reason "Out of order"
    Then the response is a problem with status 409 and type "invalidTransition"
    And the <party> contract stays in status "<status>"

    Examples:
      | status               | party    | action               |
      | ACTIVE               | provider | REACTIVATE           |
      | ACTIVE               | consumer | REQUEST_REACTIVATION |
      | ACTIVE               | consumer | ACCEPT_REVOCATION    |
      | SUSPENDED            | provider | SUSPEND              |
      | SUSPENDED            | consumer | REQUEST_REVOCATION   |
      | REVOCATION_REQUESTED | provider | SUSPEND              |

  @openapi-only @D37 @D11
  Scenario: An unknown action is refused with 400
    Given an active contract between both parties
    When the provider user performs "PAUSE" with reason "x"
    Then the response is a problem with status 400 and type "validationFailure"

  @openapi-only @D40
  Scenario: Two concurrent suspensions of one contract: one wins, the other gets 409
    Given an active contract between both parties
    When the provider user sends "SUSPEND" twice at the same time with reason "Race"
    Then one response is 200 and the other is a problem with status 409 and type "invalidTransition"
    And both contracts reach status "SUSPENDED"
    And the provider contract history has 1 entries with:
      | field    | value     |
      | toStatus | SUSPENDED |
