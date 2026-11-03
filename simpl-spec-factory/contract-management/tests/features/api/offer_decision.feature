@stub
Feature: The Consumer accepts or rejects the offer, then the Consumer signs (BP07.05 to BP07.07)
  The Consumer user decides through UIBE. UIBE calls its CM. On accept CM signs
  asynchronously, stores its VC in the wallet and tells the connector
  (SIMPL-32505, SIMPL-35660, SIMPL-21928, D23, D24, E4 to E7).

  @SIMPL-32505 @D23 @D9 @D37 @E4 @two-party @uibe
  Scenario: SIMPL-32505 AC2 - the Consumer accepts the offer and sees the status Accepted
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    When the consumer accepts the offer
    Then the response status is 200
    And the response fields are:
      | field                 | value    |
      | contractNegotiationId | ${nid}   |
      | status                | ACCEPTED |
      | contractData          | null     |
    And the response field "humanReadableData" is not null
    And the response field "hashData" is not null
    And the response field "createdDate" is an OpenAPI local date-time
    And the response field "lastUpdatedDate" is an OpenAPI local date-time
    And the consumer contract history has an entry with:
      | field      | value     |
      | entryType  | TRANSITION|
      | fromStatus | REQUESTED |
      | toStatus   | ACCEPTED  |
      | trigger    | USER      |

  @SIMPL-32505 @D61 @two-party @uibe @oidc
  Scenario: The accepting user is recorded as the actor in the history
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    When the consumer user "consumer-admin" sends the offer decision "accept"
    Then the response status is 200
    And the consumer contract history has an entry with:
      | field    | value          |
      | toStatus | ACCEPTED       |
      | actor    | consumer-admin |

  @SIMPL-35660 @SIMPL-21928 @SIMPL-19927 @bp07 @D24 @D37 @E5 @two-party @slow
  Scenario: SIMPL-35660 AC1 and SIMPL-21928 AC1 - after the accept the Consumer signs and informs the connector
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    When the consumer accepts the offer
    Then the response status is 200
    And the consumer contract reaches status "SIGNED"
    And the consumer contract field "consumerSignatureDate" is a date-time
    And the consumer contract field "ownCredentialId" is not null
    And the consumer wallet holds a credential signed as "CONSUMER"
    And the stub received the callback "signed" with "true" from the consumer
    And the stub negotiation reaches state "CONSUMER_SIGNED"

  @SIMPL-21928 @D24 @D61 @two-party @slow
  Scenario: SIMPL-21928 AC2 - the signing is traceable by negotiation id in the history
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    When the consumer accepts the offer
    Then the consumer contract reaches status "SIGNED"
    And the consumer contract history has an entry with:
      | field      | value      |
      | entryType  | TRANSITION |
      | fromStatus | ACCEPTED   |
      | toStatus   | SIGNED     |
      | trigger    | SYSTEM     |
    And every consumer contract history entry has the fields of D61

  @SIMPL-35660 @D4 @D23 @D38 @consumer
  Scenario: SIMPL-35660 AC1 - CM answers the accept with 200 INITIATED, not 201
    Given an offer created at the consumer
    When the consumer UIBE sends the offer decision "accept" to the consumer CM
    Then the response status is 200
    And the response fields are:
      | field                 | value     |
      | contractNegotiationId | ${nid}    |
      | status                | INITIATED |
    And the consumer contract reaches status "SIGNED"
    And the stub received the callback "signed" with "true" from the consumer

  @SIMPL-21928 @SIMPL-35660 @D24 @D59 @D37 @E6 @P4 @two-party @slow
  Scenario: SIMPL-21928 AC4 - a signer failure leaves the offer unsigned and ends the negotiation
    Given an offer waiting for the consumer decision with the fault "SIGNER:FAIL"
    When the consumer accepts the offer
    Then the response status is 200
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field      | value          |
      | reasonCode | SIGNING_FAILED |
    And the consumer contract field "consumerSignatureDate" is null
    And the stub received the callback "signed" with "false" from the consumer
    And the stub received no callback "signed" with "true" from the consumer
    And the consumer wallet holds no credential signed as "CONSUMER"
    And the stub negotiation ends in state "TERMINATED" with reason "CONSUMER_DECLINED"
    And the provider contract reaches status "TERMINATED"
    And the provider contract has:
      | field      | value                   |
      | reasonCode | COUNTERPARTY_TERMINATED |

  @SIMPL-19927 @D24 @D57 @E6 @two-party @slow
  Scenario Outline: A wallet failure while the Consumer signs fails the contract with <code>
    Given an offer waiting for the consumer decision with the fault "WALLET:<mode>"
    When the consumer accepts the offer
    Then the response status is 200
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field      | value  |
      | reasonCode | <code> |
    And the consumer contract history has an entry with:
      | field      | value     |
      | toStatus   | FAILED    |
      | reasonCode | <code>    |
      | reason     | <message> |
    And the stub received the callback "signed" with "false" from the consumer
    And the provider contract reaches status "TERMINATED"

    Examples:
      | mode        | code               | message                              |
      | UNAVAILABLE | WALLET_UNAVAILABLE | Secure storage location unavailable  |
      | FULL        | WALLET_FULL        | Secure storage location is full      |
      | CORRUPTED   | WALLET_CORRUPTED   | Secure storage location is corrupted |

  @SIMPL-35660 @SIMPL-30959 @bp07 @D23 @D37 @E7 @P4 @two-party
  Scenario: SIMPL-35660 AC3 - the Consumer rejects the offer, nothing is signed and the connector is told
    Given an offer waiting for the consumer decision
    When the consumer rejects the offer
    Then the response status is 200
    And the consumer contract reaches status "REJECTED"
    And the consumer contract field "consumerSignatureDate" is null
    And the stub received the callback "signed" with "false" from the consumer
    And the consumer wallet holds 0 credentials for the negotiation
    And the stub negotiation ends in state "TERMINATED" with reason "CONSUMER_DECLINED"
    And the provider contract reaches status "TERMINATED"
    And the provider contract has:
      | field      | value                   |
      | reasonCode | COUNTERPARTY_TERMINATED |

  @SIMPL-35660 @D23 @D9 @two-party @uibe
  Scenario: The reject answer of UIBE is the offer object with status REJECTED
    Given an offer waiting for the consumer decision
    When the consumer rejects the offer
    Then the response status is 200
    And the response field "status" is "REJECTED"
    And the response field "contractNegotiationId" is "${nid}"
    And the consumer UIBE offer has status "REJECTED"

  @SIMPL-32505 @SIMPL-30959 @D37 @two-party
  Scenario: SIMPL-32505 AC1 - an offer in REQUESTED offers the actions accept and reject
    Given an offer waiting for the consumer decision
    Then the consumer contract allowed actions are "ACCEPT, REJECT"
    And the provider contract allowed actions are ""

  @SIMPL-32505 @D23 @two-party @uibe
  Scenario: SIMPL-32505 AC3 - an offer that is already accepted cannot be accepted again
    Given an offer waiting for the consumer decision that stops after "CONSUMER_SIGNED"
    When the consumer accepts the offer
    And the consumer accepts the offer
    Then the response is a problem with status 400 and type "offerNotPending"
    And the consumer UIBE offer still has status "ACCEPTED"

  @SIMPL-32505 @D23 @two-party @uibe
  Scenario: SIMPL-32505 AC3 - a rejected offer cannot be accepted
    Given an offer waiting for the consumer decision
    When the consumer rejects the offer
    And the consumer accepts the offer
    Then the response is a problem with status 400 and type "offerNotPending"
    And the consumer contract stays in status "REJECTED"
    And the consumer UIBE offer still has status "REJECTED"

  @SIMPL-32505 @D23 @D37 @two-party @slow
  Scenario Outline: SIMPL-32505 AC3 - a contract in <status> cannot be accepted
    Given an active contract between both parties that is now "<status>"
    When the consumer accepts the offer
    Then the response is a problem with status 400 and type "offerNotPending"
    And the consumer contract stays in status "<status>"
    And the consumer contract allowed actions are "<actions>"

    Examples:
      | status     | actions                                |
      | ACTIVE     | REQUEST_REVOCATION                     |
      | SUSPENDED  | REQUEST_REACTIVATION                   |
      | TERMINATED |                                        |

  @SIMPL-35660 @D23 @two-party
  Scenario: CM refuses a decision when the Consumer contract is no longer REQUESTED
    Given a negotiation where the consumer has signed
    When the consumer UIBE sends the offer decision "reject" to the consumer CM
    Then the response is a problem with status 400 and type "offerNotPending"
    And the consumer contract stays in status "SIGNED"

  @SIMPL-35660 @D12
  Scenario: CM refuses a decision for an unknown negotiation id with 400
    Given fresh negotiation ids
    When the consumer UIBE sends the offer decision "accept" to the provider CM
    Then the response is a problem with status 400 and type "validationFailure"
    And the problem detail contains "Unknown contractNegotiationId"

  @SIMPL-35660 @D2
  Scenario: CM refuses a decision on a Provider record with 400 wrongParty
    Given a contract requested at the provider
    When the consumer UIBE sends the offer decision "accept" to the provider CM
    Then the response is a problem with status 400 and type "wrongParty"
    And the provider contract stays in status "REQUESTED"

  @openapi-only @D11
  Scenario: CM refuses an operation other than accept or reject
    Given a contract requested at the provider
    When the consumer UIBE sends the offer decision "approve" to the provider CM
    Then the response is a problem with status 400 and type "argumentTypeMismatch"

  @SIMPL-32505 @D23 @consumer @uibe @oidc
  Scenario: When CM refuses the decision UIBE answers with the same code and keeps the offer CREATED
    Given an offer created at the consumer
    And the connector reports the offer status "REJECTED" to the consumer CM
    When the consumer user "consumer-admin" sends the offer decision "accept"
    Then the response is a problem with status 400 and type "offerNotPending"
    And the consumer UIBE offer still has status "CREATED"

  @SIMPL-32505 @D12 @uibe @oidc @consumer
  Scenario: UIBE answers 404 for a decision on an unknown offer
    Given fresh negotiation ids
    When the consumer user "consumer-admin" sends the offer decision "accept"
    Then the response is a problem with status 404
