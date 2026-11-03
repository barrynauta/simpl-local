Feature: Validate-and-sign trigger without a prior negotiation at the stub
  The /issue trigger runs at the Provider only and needs a Provider record,
  either from the existence check or from its body (D25, D12).

  @SIMPL-35584 @D25 @D12
  Scenario: validate-and-sign without a body and without a Provider record is refused with 400
    Given fresh negotiation ids
    When the connector asks the provider CM to validate and sign without a body
    Then the response is a problem with status 400 and type "validationFailure"
    And the provider has no contract for the negotiation

  @SIMPL-35584 @SIMPL-21963 @D25 @D26 @D27 @stub @two-party
  Scenario: SIMPL-21963 AC3 - validate-and-sign with a body creates the record, and without a Consumer signature it is rejected
    Given a fresh asset
    And fresh negotiation ids
    When the connector asks the provider CM to validate and sign
    Then the response status is 201
    And the response field "status" is "INITIATED"
    And the provider contract reaches status "FAILED"
    And the provider contract has:
      | field                | value                 |
      | contractAgreementId  | ${aid}                |
      | contractDefinitionId | ${did}                |
      | validationResult     | REJECTED              |
      | validationReasonCode | MISSING_CONTRACT_DATA |
    And the stub received the callback "isValid" with "false" from the provider

  @SIMPL-35584 @D25 @D2
  Scenario: validate-and-sign with a body naming another Provider is refused with 400 wrongParty
    Given a fresh unregistered asset id
    And fresh negotiation ids
    And the variable "providerId" is "provider-other"
    When the connector asks the provider CM to validate and sign
    Then the response is a problem with status 400 and type "wrongParty"
    And the provider has no contract for the negotiation

  @openapi-only @D16
  Scenario: validate-and-sign with a contractAgreementId that is not a UUID is refused
    Given fresh negotiation ids
    When I send "POST" to the provider CM at "/credentials/agreements/not-a-uuid/definitions/${did}/issue"
    Then the response is a problem with status 400 and type "argumentTypeMismatch"

  @openapi-only @D11
  Scenario: validate-and-sign with a non-JSON body is refused with 415
    Given fresh negotiation ids
    When I send "POST" to the provider CM at "/credentials/agreements/${aid}/definitions/${did}/issue" with content type "text/plain" and body "x"
    Then the response is a problem with status 415 and type "unsupportedMediaType"
