Feature: Existence check of a usage contract (BP07.03)
  The connector asks the Provider CM whether the Consumer already has an open
  contract for the asset. CM answers at once and reports the result to the
  connector through /isPresent (SIMPL-32509, SIMPL-35858, D19, E1, E2).

  @SIMPL-32509 @bp07 @D19 @D37 @E1
  Scenario: SIMPL-32509 AC1 - no open contract exists, the Provider record is created in REQUESTED
    Given a fresh unregistered asset id
    And fresh negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the response fields are:
      | field                          | value         |
      | exists                         | false         |
      | contractAgreementId            | ${aid}        |
      | contractDefinitionId           | ${did}        |
      | contractSearchTO.assetId       | ${assetId}    |
      | contractSearchTO.consumerId    | ${consumerId} |
      | contractSearchTO.providerId    | ${providerId} |
    And the provider contract has:
      | field                | value         |
      | status               | REQUESTED     |
      | partyRole            | PROVIDER      |
      | assetId              | ${assetId}    |
      | consumerId           | ${consumerId} |
      | providerId           | ${providerId} |
      | contractAgreementId  | ${aid}        |
      | contractDefinitionId | ${did}        |

  @SIMPL-35858 @D19 @E1 @stub
  Scenario: SIMPL-35858 AC1 - the existence result false is reported to the connector
    Given a fresh asset
    And fresh negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the stub received the callback "isPresent" with "false" from the provider
    And the stub received no callback "isPresent" with "true" from the provider

  @SIMPL-32509 @SIMPL-35858 @bp07 @D19 @D37 @E2 @stub
  Scenario: SIMPL-32509 AC1 - an open contract exists, the answer names it and no record is created
    Given a contract requested at the provider
    And a second set of negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the response fields are:
      | field                | value       |
      | exists               | true        |
      | contractAgreementId  | ${firstAid} |
      | contractDefinitionId | ${firstDid} |
    And the stub received the callback "isPresent" with "true" from the provider
    And the provider has no contract for the negotiation

  @SIMPL-32509 @D19 @D37
  Scenario: A contract in a terminal status does not count as existing
    Given a contract requested at the provider
    And the provider contract is terminated by the connector
    And a second set of negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the response field "exists" is "false"
    And the provider contract reaches status "REQUESTED"

  @SIMPL-32509 @D19
  Scenario: A contract with the same asset but another Consumer does not count
    Given a contract requested at the provider
    And a second set of negotiation ids
    When the connector asks the provider CM to check existence with "consumerId" set to "consumer-other-${nid}"
    Then the response status is 200
    And the response field "exists" is "false"

  @SIMPL-32509 @D19
  Scenario: A repeated existence check for the same negotiation does not create a second record
    Given a contract requested at the provider
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the response field "exists" is "false"
    And the provider has exactly one contract for the negotiation

  @SIMPL-32509 @D19
  Scenario: providerId defaults to the CM's own participant id
    Given a fresh unregistered asset id
    And fresh negotiation ids
    When the connector asks the provider CM to check existence without "providerId"
    Then the response status is 200
    And the response field "exists" is "false"
    And the provider contract has:
      | field      | value         |
      | providerId | ${providerId} |

  @SIMPL-32509 @D19
  Scenario: SIMPL-32509 AC2 - an asset unknown to the catalogue does not fail the existence check
    Given a fresh unregistered asset id
    And fresh negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the response field "exists" is "false"
    And the provider contract reaches status "REQUESTED"

  @SIMPL-32509 @D19 @D60 @stub
  Scenario: The Provider record is enriched from the catalogue and the directory
    Given a fresh asset with:
      | field        | value                 |
      | resourceName | Air quality sensors   |
    And fresh negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the provider contract has:
      | field                 | value              |
      | resourceDescriptionId | ${rdId}            |
      | resourceName          | ${resourceName}    |
      | providerName          | Provider One       |
      | consumerName          | Consumer One       |

  @SIMPL-32509 @D19 @D11
  Scenario Outline: SIMPL-32509 AC4 - an existence check without <field> is refused with 400
    Given a fresh unregistered asset id
    And fresh negotiation ids
    When the connector asks the provider CM to check existence without "<field>"
    Then the response is a problem with status 400 and type "validationFailure"
    And the provider has no contract for the negotiation

    Examples:
      | field      |
      | assetId    |
      | consumerId |

  @SIMPL-32509 @D16 @D11
  Scenario: SIMPL-32509 AC4 - a consumerId outside the allowed pattern is refused with 400
    Given a fresh unregistered asset id
    And fresh negotiation ids
    When the connector asks the provider CM to check existence with "consumerId" set to "bad id"
    Then the response is a problem with status 400 and type "validationFailure"

  @SIMPL-32509 @D11
  Scenario: SIMPL-32509 AC4 - an existence check with a non-JSON body is refused with 415
    Given fresh negotiation ids
    When I send "POST" to the provider CM at "/agreements/${nid}/search" with content type "text/plain" and body "x"
    Then the response is a problem with status 415 and type "unsupportedMediaType"

  @SIMPL-32509 @D11
  Scenario: SIMPL-32509 AC4 - an existence check that excludes JSON in Accept is refused with 406
    Given fresh negotiation ids
    When I send "POST" to the provider CM at "/agreements/${nid}/search" with header "Accept" set to "application/xml" and JSON:
      """
      {"assetId": "asset-x", "consumerId": "consumer-01"}
      """
    Then the response is a problem with status 406 and type "notAcceptable"

  @SIMPL-32509 @D19 @D43 @stub
  Scenario Outline: The initial consent status follows the catalogue asset (<consentRequired>)
    Given a fresh asset with:
      | field           | value             |
      | consentRequired | <consentRequired> |
    And fresh negotiation ids
    When the connector asks the provider CM to check existence
    Then the response status is 200
    And the provider contract has:
      | field         | value     |
      | consentStatus | <consent> |

    Examples:
      | consentRequired | consent     |
      | true            | PENDING     |
      | false           | NOT_DEFINED |
