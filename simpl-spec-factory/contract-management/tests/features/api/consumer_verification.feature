@stub @two-party
Feature: The Consumer confirms the Provider-signed contract (BP07.11, BP07.12)
  The connector sends the Provider VC to the Consumer CM with PUT
  /verifications. CM answers 200 INITIATED, validates, stores the Provider VC
  and reports the result through /isValid (SIMPL-21948, 35677, 19927, 35863,
  D27, D28, D31, D57, E9, E10).

  Background:
    Given a negotiation where the provider has signed

  @SIMPL-35677 @SIMPL-21948 @SIMPL-19927 @SIMPL-35863 @bp07 @D31 @D4 @D37 @E9
  Scenario: SIMPL-35677 AC1 and SIMPL-21948 AC1 - a valid Provider VC activates the contract at the Consumer
    When the connector sends the "valid" variant of the provider credential to the consumer CM
    Then the response status is 200
    And the response fields are:
      | field                 | value     |
      | contractNegotiationId | ${nid}    |
      | status                | INITIATED |
    And the consumer contract reaches status "ACTIVE"
    And the consumer contract has:
      | field            | value   |
      | validationResult | SUCCESS |
    And the consumer contract field "providerSignatureDate" is a date-time
    And the consumer contract field "counterpartyCredentialId" is not null
    And the stub received the callback "isValid" with "true" from the consumer

  @SIMPL-19927 @D31 @D58
  Scenario: SIMPL-19927 AC1 - the fully signed contract is archived in the Consumer wallet
    When the connector sends the "valid" variant of the provider credential to the consumer CM
    Then the consumer contract reaches status "ACTIVE"
    And the consumer wallet holds 2 credentials for the negotiation
    And the consumer wallet holds a credential signed as "PROVIDER"
    And the consumer wallet holds a credential signed as "CONSUMER"

  @SIMPL-35440 @D5 @D58
  Scenario: SIMPL-35440 AC1 - each party returns only its own VC
    When the connector sends the "valid" variant of the provider credential to the consumer CM
    Then the consumer contract reaches status "ACTIVE"
    When I send "GET" to the consumer CM at "/verifications/${nid}/vc"
    Then the response status is 200
    And the response content type is "application/json"
    And the response is a credential of D53 and D55 for the negotiation
    And the response field "credentialSubject.signerRole" is "CONSUMER"
    And the response field "issuer" is "did:key:z6MkqGC3nWZhYieEVTVDKW5v588CiGfsDSmRVG9ZwwWTvLSK"

  @SIMPL-21948 @SIMPL-35677 @D27 @D56 @D37 @E10 @P4
  Scenario Outline: SIMPL-21948 <ac> - a Provider VC with <kind> is rejected with <code>
    When the connector sends the "<kind>" variant of the provider credential to the consumer CM
    Then the response status is 200
    And the response field "status" is "INITIATED"
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field                | value    |
      | validationResult     | REJECTED |
      | validationReasonCode | <code>   |
      | reasonCode           | <code>   |
    And the stub received the callback "isValid" with "false" from the consumer
    And the consumer wallet holds no credential signed as "PROVIDER"

    Examples:
      | ac  | kind                    | code                                 |
      | AC2 | invalid signature       | INVALID_PROVIDER_SIGNATURE           |
      | AC2 | tampered subject        | INVALID_PROVIDER_SIGNATURE           |
      | AC3 | other assetId           | PAYLOAD_INTEGRITY_MISMATCH           |
      | AC3 | other usageContractHash | PAYLOAD_INTEGRITY_MISMATCH           |
      | AC3 | wrong signerRole        | PAYLOAD_INTEGRITY_MISMATCH           |
      | AC4 | expired                 | CONTRACT_VALIDITY_ISSUE              |
      | AC4 | not yet valid           | CONTRACT_VALIDITY_ISSUE              |
      | AC5 | untrusted issuer        | PROVIDER_CREDENTIAL_TRUST_ISSUE      |
      | AC5 | wrong trusted issuer    | PROVIDER_CREDENTIAL_TRUST_ISSUE      |
      | AC6 | missing assetId         | SCHEMA_OR_CONTENT_VALIDATION_FAILURE |

  @SIMPL-21948 @D27 @D56
  Scenario: SIMPL-21948 AC5 - a revoked Provider credential is rejected with a trust issue
    Given the stub fault "VERIFIER:REVOKED" is set for the negotiation
    When the connector sends the "valid" variant of the provider credential to the consumer CM
    Then the response status is 200
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field                | value                           |
      | validationReasonCode | PROVIDER_CREDENTIAL_TRUST_ISSUE |

  @SIMPL-21948 @D27 @D28 @D56
  Scenario: SIMPL-21948 AC8 - a technical error fails the validation, changes nothing and a retry succeeds
    Given the stub fault "VERIFIER:FAIL" is set for the negotiation
    When the connector sends the "valid" variant of the provider credential to the consumer CM
    Then the response status is 200
    And the consumer contract has:
      | field                | value                      |
      | validationResult     | FAILED                     |
      | validationReasonCode | TECHNICAL_VALIDATION_ERROR |
    And the consumer contract stays in status "SIGNED"
    And the stub received no callback "isValid" from the consumer
    When the stub faults are cleared for the negotiation
    And the connector sends the same provider credential to the consumer CM again
    Then the response status is 200
    And the consumer contract reaches status "ACTIVE"

  @SIMPL-21948 @D28
  Scenario: SIMPL-21948 AC7 - a replayed Provider VC after success is ignored
    When the connector sends the "valid" variant of the provider credential to the consumer CM
    Then the consumer contract reaches status "ACTIVE"
    And I remember the number of "isValid" callbacks from the consumer
    When the connector sends the same provider credential to the consumer CM again
    Then the response status is 200
    And the consumer contract history has an entry with:
      | field      | value                            |
      | entryType  | VALIDATION                       |
      | reasonCode | DUPLICATE_OR_REPLAYED_SUBMISSION |
    And the consumer contract stays in status "ACTIVE"
    And the number of "isValid" callbacks from the consumer is unchanged

  @SIMPL-19927 @D31 @D57 @E10
  Scenario Outline: SIMPL-19927 <ac> - a Consumer wallet that is <state> fails the contract with <code>
    Given the stub fault "WALLET:<mode>" is set for the negotiation
    When the connector sends the "valid" variant of the provider credential to the consumer CM
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
    And the stub received the callback "isValid" with "false" from the consumer

    Examples:
      | ac  | state       | mode        | code               | message                              |
      | AC2 | unavailable | UNAVAILABLE | WALLET_UNAVAILABLE | Secure storage location unavailable  |
      | AC3 | full        | FULL        | WALLET_FULL        | Secure storage location is full      |
      | AC4 | corrupted   | CORRUPTED   | WALLET_CORRUPTED   | Secure storage location is corrupted |

  @SIMPL-35677 @D31 @D2
  Scenario: SIMPL-35677 AC4 - verification called at the Provider is refused with 400
    When the connector sends the "valid" variant of the provider credential to the provider CM
    Then the response is a problem with status 400 and type "wrongParty"
    And the provider contract stays in status "SIGNED"

  @SIMPL-35677 @D31 @D11
  Scenario: A verification body that is not JSON is refused with 415
    When I send "PUT" to the consumer CM at "/verifications/${nid}" with content type "text/plain" and body "x"
    Then the response is a problem with status 415 and type "unsupportedMediaType"
