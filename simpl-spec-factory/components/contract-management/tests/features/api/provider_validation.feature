@stub @two-party
Feature: The Provider validates the Consumer-signed offer and signs (BP07.08 to BP07.10)
  The connector calls the Provider CM /issue. CM answers 201 INITIATED at once,
  fetches the Consumer VC through /vcExchange, validates it, then signs,
  stores both VCs and informs the connector (SIMPL-19933, 19934, 19938, 21963,
  21966, 35584, 35863, 35888, D25 to D30, D56 to D59, P1, P2).

  Background:
    Given a negotiation where the consumer has signed

  @SIMPL-35584 @SIMPL-19933 @SIMPL-19934 @SIMPL-35863 @bp07 @D25 @D29 @D37 @P1
  Scenario: SIMPL-35584 AC1 and SIMPL-19933 AC1 - a valid Consumer VC is validated and the Provider signs
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the response fields are:
      | field                | value     |
      | contractAgreementId  | ${aid}    |
      | contractDefinitionId | ${did}    |
      | status               | INITIATED |
    And the provider contract reaches status "SIGNED"
    And the provider contract has:
      | field            | value   |
      | validationResult | SUCCESS |
    And the provider contract field "validatedAt" is a date-time
    And the provider contract field "consumerSignatureDate" is a date-time
    And the provider contract field "providerSignatureDate" is a date-time
    And the stub received the callback "isValid" with "true" from the provider
    And the stub received the callback "signed" with "true" from the provider

  @SIMPL-19933 @D25 @D28
  Scenario: SIMPL-19933 AC10 - the trigger answers before the validation result is known
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the response field "status" is "INITIATED"
    And the response field "validationResult" is absent

  @SIMPL-21963 @SIMPL-35888 @D26 @D80
  Scenario: SIMPL-21963 AC1 and AC2 - the Provider fetches the Consumer VC through the connector and continues
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the stub recorded a call "GET" to a path starting with "/edc/${providerId}/vcExchange/${nid}/request"
    And the provider contract reaches status "SIGNED"

  @SIMPL-21963 @D61 @D14
  Scenario: SIMPL-21963 AC4 - the delivery and the validation are traceable in the Provider history
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "SIGNED"
    And the provider contract history has an entry with:
      | field            | value      |
      | entryType        | VALIDATION |
      | toStatus         | REQUESTED  |
    And the provider contract history has an entry with:
      | field      | value      |
      | entryType  | TRANSITION |
      | fromStatus | REQUESTED  |
      | toStatus   | SIGNED     |
      | trigger    | CONNECTOR  |

  @SIMPL-19938 @SIMPL-35584 @D29 @D58
  Scenario: SIMPL-19938 AC1 - the Provider stores the Consumer VC and its own VC and returns its own VC through the API
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "SIGNED"
    And the provider wallet holds 2 credentials for the negotiation
    And the provider wallet holds a credential signed as "CONSUMER"
    And the provider wallet holds a credential signed as "PROVIDER"
    When I send "GET" to the provider CM at "/verifications/${nid}/vc"
    Then the response status is 200
    And the response is the "PROVIDER" credential held in the provider wallet
    And the response is a credential of D53 and D55 for the negotiation
    And the response field "credentialSubject.signerRole" is "PROVIDER"
    And the response field "issuer" is "did:key:z6MktULudTtAsAhRegYPiZ6631RV3viv12qd4GQF8z1xB22S"

  @SIMPL-19934 @D29 @D55
  Scenario: The Provider VC copies the Consumer VC subject except signerRole and signedAt
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "SIGNED"
    When I send "GET" to the consumer CM at "/verifications/${nid}/vc"
    Then I remember the response field "credentialSubject.usageContractHash" as "consumerHash"
    And I remember the response field "credentialSubject.resourceDescriptionId" as "consumerRd"
    When I send "GET" to the provider CM at "/verifications/${nid}/vc"
    Then the response fields are:
      | field                                   | value          |
      | credentialSubject.usageContractHash     | ${consumerHash}|
      | credentialSubject.resourceDescriptionId | ${consumerRd}  |
      | credentialSubject.contractDefinitionId  | ${did}         |
      | credentialSubject.assetId               | ${assetId}     |
      | credentialSubject.providerId            | ${providerId}  |
      | credentialSubject.consumerId            | ${consumerId}  |
      | credentialSubject.signerRole            | PROVIDER       |

  @SIMPL-19933 @D35
  Scenario: The Provider VC is valid for the default contract validity
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "SIGNED"
    When I send "GET" to the provider CM at "/verifications/${nid}/vc"
    Then the response status is 200
    And the credential validity is 365 days

  @SIMPL-19933 @SIMPL-21948 @D27 @D56 @D37 @P2
  Scenario Outline: SIMPL-19933 <ac> - a Consumer VC with <kind> is rejected with <code>
    Given the stub override "consumerVcOverride" is the "<kind>" variant of the consumer credential
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract reaches status "FAILED"
    And the provider contract has:
      | field                | value    |
      | validationResult     | REJECTED |
      | validationReasonCode | <code>   |
      | reasonCode           | <code>   |
    And the stub received the callback "isValid" with "false" from the provider
    And the stub received no callback "signed" from the provider
    And the provider wallet holds no credential signed as "PROVIDER"

    Examples:
      | ac  | kind                 | code                                 |
      | AC2 | invalid signature    | INVALID_CONSUMER_SIGNATURE           |
      | AC2 | tampered subject     | INVALID_CONSUMER_SIGNATURE           |
      | AC3 | other assetId        | PAYLOAD_INTEGRITY_MISMATCH           |
      | AC3 | other agreement id   | PAYLOAD_INTEGRITY_MISMATCH           |
      | AC3 | wrong signerRole     | PAYLOAD_INTEGRITY_MISMATCH           |
      | AC4 | expired              | CONTRACT_VALIDITY_ISSUE              |
      | AC4 | not yet valid        | CONTRACT_VALIDITY_ISSUE              |
      | AC5 | untrusted issuer     | CONSUMER_CREDENTIAL_TRUST_ISSUE      |
      | AC5 | wrong trusted issuer | CONSUMER_CREDENTIAL_TRUST_ISSUE      |
      | AC6 | missing assetId      | SCHEMA_OR_CONTENT_VALIDATION_FAILURE |

  @SIMPL-19933 @D27 @D56
  Scenario: SIMPL-19933 AC5 - a revoked Consumer credential is rejected with a trust issue
    Given the stub fault "VERIFIER:REVOKED" is set for the negotiation
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract reaches status "FAILED"
    And the provider contract has:
      | field                | value                           |
      | validationResult     | REJECTED                        |
      | validationReasonCode | CONSUMER_CREDENTIAL_TRUST_ISSUE |
    And the stub received the callback "isValid" with "false" from the provider

  @SIMPL-19933 @SIMPL-21963 @SIMPL-35888 @D26 @D27 @D56
  Scenario: SIMPL-19933 AC9 and SIMPL-21963 AC3 - a missing Consumer VC is rejected with MISSING_CONTRACT_DATA
    Given the stub override "consumerVcOverride" is "not found"
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract reaches status "FAILED"
    And the provider contract has:
      | field                | value                 |
      | validationResult     | REJECTED              |
      | validationReasonCode | MISSING_CONTRACT_DATA |
    And the stub received the callback "isValid" with "false" from the provider
    And the provider wallet holds no credential signed as "PROVIDER"

  @SIMPL-19933 @D27 @D28 @D56
  Scenario: SIMPL-19933 AC8 - a technical error fails the validation, changes nothing and a retry succeeds
    Given the stub fault "VERIFIER:FAIL" is set for the negotiation
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract has:
      | field                | value                      |
      | validationResult     | FAILED                     |
      | validationReasonCode | TECHNICAL_VALIDATION_ERROR |
    And the provider contract stays in status "REQUESTED"
    And the stub received no callback "isValid" from the provider
    And the stub received no callback "signed" from the provider
    When the stub faults are cleared for the negotiation
    And the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract reaches status "SIGNED"
    And the provider contract has:
      | field            | value   |
      | validationResult | SUCCESS |

  @SIMPL-19933 @D28
  Scenario: SIMPL-19933 AC7 - a replayed submission after a successful validation is ignored
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "SIGNED"
    And I remember the number of "isValid" callbacks from the provider
    And I remember the number of "signed" callbacks from the provider
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract history has an entry with:
      | field            | value                            |
      | entryType        | VALIDATION                       |
      | reasonCode       | DUPLICATE_OR_REPLAYED_SUBMISSION |
    And the provider contract has:
      | field            | value   |
      | validationResult | SUCCESS |
    And the provider contract stays in status "SIGNED"
    And the number of "isValid" callbacks from the provider is unchanged
    And the number of "signed" callbacks from the provider is unchanged

  @SIMPL-19933 @D28
  Scenario: SIMPL-19933 AC7 - a replayed submission after a rejection is ignored
    Given the stub override "consumerVcOverride" is the "expired" variant of the consumer credential
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "FAILED"
    And I remember the number of "isValid" callbacks from the provider
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract history has an entry with:
      | field      | value                            |
      | entryType  | VALIDATION                       |
      | reasonCode | DUPLICATE_OR_REPLAYED_SUBMISSION |
    And the provider contract stays in status "FAILED"
    And the provider contract has:
      | field                | value                   |
      | validationReasonCode | CONTRACT_VALIDITY_ISSUE |
    And the number of "isValid" callbacks from the provider is unchanged

  @SIMPL-19934 @D29 @D59 @D37 @P2
  Scenario: SIMPL-19934 AC2 - a signer failure after a successful validation fails the contract with SIGNING_FAILED
    Given the stub fault "SIGNER:FAIL" is set for the negotiation
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract reaches status "FAILED"
    And the provider contract has:
      | field            | value          |
      | validationResult | SUCCESS        |
      | reasonCode       | SIGNING_FAILED |
    And the stub received the callback "isValid" with "true" from the provider
    And the stub received the callback "signed" with "false" from the provider

  @SIMPL-19938 @D29 @D57 @P2
  Scenario Outline: SIMPL-19938 <ac> - a Provider wallet that is <state> fails the contract with <code>
    Given the stub fault "WALLET:<mode>" is set for the negotiation
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the provider contract reaches status "FAILED"
    And the provider contract has:
      | field      | value  |
      | reasonCode | <code> |
    And the provider contract history has an entry with:
      | field      | value     |
      | toStatus   | FAILED    |
      | reasonCode | <code>    |
      | reason     | <message> |
    And the stub received the callback "signed" with "false" from the provider

    Examples:
      | ac  | state       | mode        | code               | message                              |
      | AC2 | unavailable | UNAVAILABLE | WALLET_UNAVAILABLE | Secure storage location unavailable  |
      | AC3 | full        | FULL        | WALLET_FULL        | Secure storage location is full      |
      | AC4 | corrupted   | CORRUPTED   | WALLET_CORRUPTED   | Secure storage location is corrupted |

  @SIMPL-19938 @D29
  Scenario: SIMPL-19938 AC5 - a contract whose validation fails is not stored
    Given the stub override "consumerVcOverride" is the "invalid signature" variant of the consumer credential
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "FAILED"
    And the provider wallet holds no credential signed as "PROVIDER"
    And the provider wallet holds no credential signed as "CONSUMER"
    When I send "GET" to the provider CM at "/verifications/${nid}/vc"
    Then the response is a problem with status 404 and type "notFound"

  @SIMPL-21966 @D30 @events
  Scenario: SIMPL-21966 AC1 and AC2 - a rejected Consumer VC notifies the Consumer through the notifications topic
    Given I listen to the Kafka topic "notifications"
    And the stub override "consumerVcOverride" is the "invalid signature" variant of the consumer credential
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "FAILED"
    And a message for the negotiation is published on "notifications" with:
      | field   | value                                                 |
      | channel | EMAIL                                                 |
      | to.0    | consumer-01@example.test                              |
      | subject | Usage contract offer could not be validated: ${nid}   |
      | message | <contains> INVALID_CONSUMER_SIGNATURE                 |
    And the last message has exactly the fields "channel, to, cc, subject, message"
    And the last message has the header "correlationId"
    And the provider contract history has an entry with:
      | field     | value             |
      | entryType | NOTIFICATION_SENT |

  @SIMPL-21966 @D30 @events
  Scenario: SIMPL-21966 AC3 - without a Consumer contact email no notification is published
    Given I listen to the Kafka topic "notifications"
    And the stub fault "DIRECTORY:NO_CONTACT" is set for the negotiation
    And the stub override "consumerVcOverride" is the "expired" variant of the consumer credential
    When the connector asks the provider CM to validate and sign
    Then the provider contract reaches status "FAILED"
    And no message for the negotiation is published on "notifications" with:
      | field   | value                                               |
      | subject | Usage contract offer could not be validated: ${nid} |
    And the provider contract history has an entry with:
      | field     | value                |
      | entryType | NOTIFICATION_SKIPPED |

  @SIMPL-21966 @D30 @events
  Scenario: No notification is published after a technical validation error
    Given I listen to the Kafka topic "notifications"
    And the stub fault "VERIFIER:FAIL" is set for the negotiation
    When the connector asks the provider CM to validate and sign
    Then the provider contract has:
      | field            | value  |
      | validationResult | FAILED |
    And no message for the negotiation is published on "notifications" with:
      | field   | value                                               |
      | subject | Usage contract offer could not be validated: ${nid} |

  @SIMPL-35584 @D25 @D2
  Scenario: SIMPL-35584 AC3 - validate-and-sign called at the Consumer is refused with 400
    When the connector asks the consumer CM to validate and sign
    Then the response is a problem with status 400 and type "wrongParty"
    And the consumer contract stays in status "SIGNED"

  @SIMPL-35584 @D25
  Scenario: validate-and-sign without a body uses the Provider record of the existence check
    When the connector asks the provider CM to validate and sign without a body
    Then the response status is 201
    And the response field "status" is "INITIATED"
    And the provider contract reaches status "SIGNED"
