@events @stub
Feature: Kafka messages between the parties and for observability
  Lifecycle messages on simpl.contract.lifecycle.v1, status events on
  simpl.contract.status.v1, emails on notifications (Appendix D, D46 to D51).

  @openapi-only @D49 @D50 @D51 @D14
  Scenario: The existence check publishes a status event for the new Provider record
    Given I listen to the Kafka topic "simpl.contract.status.v1"
    And a contract requested at the provider
    Then a message for the negotiation is published on "simpl.contract.status.v1" with:
      | field                 | value         |
      | participantId         | ${providerId} |
      | partyRole             | PROVIDER      |
      | contractNegotiationId | ${nid}        |
      | contractAgreementId   | ${aid}        |
      | previousStatus        | null          |
      | newStatus             | REQUESTED     |
      | trigger               | CONNECTOR     |
    And the last message key is the negotiation id
    And the last message has the header "correlationId"
    And the last message field "occurredAt" is an RFC 3339 UTC date-time
    And the last message field "eventId" is present

  @SIMPL-31115 @D46 @D47 @two-party @slow
  Scenario: SIMPL-31115 - a suspension sends one lifecycle message to the Consumer
    Given an active contract between both parties
    And I listen to the Kafka topic "simpl.contract.lifecycle.v1"
    When the provider user performs "SUSPEND" with reason "Breach of usage policy"
    Then a message for the negotiation is published on "simpl.contract.lifecycle.v1" with:
      | field                  | value                  |
      | messageType            | SUSPENDED              |
      | contractNegotiationId  | ${nid}                 |
      | contractAgreementId    | ${aid}                 |
      | senderParticipantId    | ${providerId}          |
      | senderRole             | PROVIDER               |
      | recipientParticipantId | ${consumerId}          |
      | reason                 | Breach of usage policy |
    And the last message field "messageId" is present
    And the last message field "occurredAt" is an RFC 3339 UTC date-time
    And the last message key is the negotiation id
    And the last message has the header "correlationId"
    And exactly 1 messages for the negotiation are published on "simpl.contract.lifecycle.v1" with:
      | field       | value     |
      | messageType | SUSPENDED |

  @SIMPL-31115 @D47 @D49 @two-party @slow
  Scenario: Both parties publish a status event for a suspension, the receiver with trigger COUNTERPARTY
    Given an active contract between both parties
    And I listen to the Kafka topic "simpl.contract.status.v1"
    When the provider user performs "SUSPEND" with reason "Breach of usage policy"
    Then a message for the negotiation is published on "simpl.contract.status.v1" with:
      | field          | value         |
      | participantId  | ${providerId} |
      | previousStatus | ACTIVE        |
      | newStatus      | SUSPENDED     |
      | trigger        | USER          |
    And a message for the negotiation is published on "simpl.contract.status.v1" with:
      | field          | value                  |
      | participantId  | ${consumerId}          |
      | partyRole      | CONSUMER               |
      | previousStatus | ACTIVE                 |
      | newStatus      | SUSPENDED              |
      | trigger        | COUNTERPARTY           |
      | reason         | Breach of usage policy |
      | actor          | null                   |

  @SIMPL-31116 @D47 @two-party @slow
  Scenario: SIMPL-31116 - the receiving party does not answer a lifecycle message with one of its own
    Given an active contract between both parties that is now "SUSPENDED"
    And I listen to the Kafka topic "simpl.contract.lifecycle.v1"
    When the consumer user performs "REQUEST_REACTIVATION" with reason "Issue fixed"
    Then a message for the negotiation is published on "simpl.contract.lifecycle.v1" with:
      | field       | value                  |
      | messageType | REACTIVATION_REQUESTED |
      | senderRole  | CONSUMER               |
    And both contracts reach status "REACTIVATION_REQUESTED"
    And no message for the negotiation is published on "simpl.contract.lifecycle.v1" with:
      | field      | value    |
      | senderRole | PROVIDER |

  @SIMPL-31053 @SIMPL-31076 @D47 @two-party @slow
  Scenario Outline: The lifecycle message type of <action> is <type>
    Given an active contract between both parties with a revocation requested by the provider
    And I listen to the Kafka topic "simpl.contract.lifecycle.v1"
    When the consumer user performs "<action>" with reason "Answer"
    Then a message for the negotiation is published on "simpl.contract.lifecycle.v1" with:
      | field       | value         |
      | messageType | <type>        |
      | senderRole  | CONSUMER      |
      | recipientParticipantId | ${providerId} |

    Examples:
      | action            | type                |
      | ACCEPT_REVOCATION | REVOCATION_ACCEPTED |
      | DENY_REVOCATION   | REVOCATION_DENIED   |

  @SIMPL-31118 @D47 @two-party @slow
  Scenario: A termination sends a TERMINATED lifecycle message
    Given an active contract between both parties
    And I listen to the Kafka topic "simpl.contract.lifecycle.v1"
    When the provider user performs "TERMINATE" with reason "Termination conditions met"
    Then a message for the negotiation is published on "simpl.contract.lifecycle.v1" with:
      | field       | value      |
      | messageType | TERMINATED |
      | senderRole  | PROVIDER   |

  @SIMPL-31115 @D48 @two-party @slow
  Scenario: A lifecycle message that does not fit the receiver's state is rejected and recorded
    Given an active contract between both parties that is now "TERMINATED"
    When a lifecycle message "SUSPENDED" from the provider to the consumer is published for the negotiation
    Then the consumer contract history has an entry with:
      | field     | value                         |
      | entryType | COUNTERPARTY_MESSAGE_REJECTED |
    And the consumer contract stays in status "TERMINATED"

  @SIMPL-31115 @D47 @two-party @slow
  Scenario: A lifecycle message is applied once even when delivered twice
    Given an active contract between both parties
    When a lifecycle message "SUSPENDED" from the provider to the consumer is published twice for the negotiation
    Then the consumer contract reaches status "SUSPENDED"
    And the consumer contract history has 1 entries with:
      | field    | value     |
      | toStatus | SUSPENDED |

  @SIMPL-31115 @D47 @two-party @slow
  Scenario: A lifecycle message whose sender role may not perform the transition is not applied
    Given an active contract between both parties
    When a lifecycle message "SUSPENDED" from the consumer to the provider is published for the negotiation
    Then the provider contract stays in status "ACTIVE"

  @SIMPL-31115 @D47 @two-party @slow
  Scenario: A lifecycle message for another recipient is ignored
    Given an active contract between both parties
    When a lifecycle message "SUSPENDED" from the provider to "consumer-other" is published for the negotiation
    Then the consumer contract stays in status "ACTIVE"
