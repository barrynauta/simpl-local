Feature: Agreement operations of the OpenAPI at the Provider
  Finalisation and termination by the connector, agreement reads, the
  agreement file and the search for established agreements (D32, D34, D38,
  P3, P4).

  @SIMPL-19934 @bp07 @D32 @D37 @D38 @P3 @stub @two-party
  Scenario: SIMPL-19934 AC1 - FINALIZED moves the signed Provider contract to ACTIVE
    Given a negotiation where the consumer has verified the provider
    When the connector sets the provider agreement status to "FINALIZED"
    Then the response status is 200
    And the response fields are:
      | field                | value     |
      | contractAgreementId  | ${aid}    |
      | contractDefinitionId | ${did}    |
      | status               | FINALIZED |
    And the provider contract reaches status "ACTIVE"
    And the provider contract history has an entry with:
      | field      | value     |
      | fromStatus | SIGNED    |
      | toStatus   | ACTIVE    |
      | trigger    | CONNECTOR |

  @openapi-only @D32 @D37 @P4
  Scenario: TERMINATED moves a REQUESTED Provider contract to TERMINATED
    Given a contract requested at the provider
    When the connector sets the provider agreement status to "TERMINATED"
    Then the response status is 200
    And the response field "status" is "TERMINATED"
    And the provider contract reaches status "TERMINATED"
    And the provider contract has:
      | field      | value                   |
      | reasonCode | COUNTERPARTY_TERMINATED |

  @openapi-only @D32 @D37 @P4 @stub @two-party
  Scenario: TERMINATED moves a SIGNED Provider contract to TERMINATED
    Given a negotiation where the provider has signed
    When the connector sets the provider agreement status to "TERMINATED"
    Then the response status is 200
    And the response field "status" is "TERMINATED"
    And the provider contract reaches status "TERMINATED"
    And the provider contract has:
      | field      | value                   |
      | reasonCode | COUNTERPARTY_TERMINATED |

  @openapi-only @D32
  Scenario: FINALIZED on a REQUESTED Provider contract is refused with 400 invalidTransition
    Given a contract requested at the provider
    When the connector sets the provider agreement status to "FINALIZED"
    Then the response is a problem with status 400 and type "invalidTransition"
    And the provider contract stays in status "REQUESTED"

  @openapi-only @D32 @stub @two-party @slow
  Scenario Outline: <status> on an ACTIVE Provider contract is refused with 400 invalidTransition
    Given an active contract between both parties
    When the connector sets the provider agreement status to "<status>"
    Then the response is a problem with status 400 and type "invalidTransition"
    And the provider contract stays in status "ACTIVE"

    Examples:
      | status     |
      | FINALIZED  |
      | TERMINATED |

  @openapi-only @D32
  Scenario: TERMINATED on a terminated Provider contract is refused with 400 invalidTransition
    Given a contract requested at the provider
    And the provider contract is terminated by the connector
    When the connector sets the provider agreement status to "TERMINATED"
    Then the response is a problem with status 400 and type "invalidTransition"

  @openapi-only @D32 @D11
  Scenario: A status outside FINALIZED and TERMINATED is refused with 400
    Given a contract requested at the provider
    When the connector sets the provider agreement status to "SUSPENDED"
    Then the response is a problem with status 400 and type "validationFailure"

  @openapi-only @D12
  Scenario: A status change for an unknown agreement is refused with 400
    Given fresh negotiation ids
    When the connector sets the provider agreement status to "TERMINATED"
    Then the response is a problem with status 400 and type "validationFailure"
    And the problem detail contains "Unknown contractAgreementId"

  @openapi-only @D34 @D38
  Scenario: A REQUESTED Provider record reads as technical status CREATED
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/agreements/${aid}"
    Then the response status is 200
    And the response fields are:
      | field                 | value         |
      | contractAgreementId   | ${aid}        |
      | contractDefinitionId  | ${did}        |
      | status                | CREATED       |
      | contractNegotiationId | ${nid}        |
      | assetId               | ${assetId}    |
      | providerId            | ${providerId} |
      | consumerId            | ${consumerId} |

  @openapi-only @D34 @D38 @stub @two-party
  Scenario: A SIGNED Provider record reads as technical status FINALIZING with the signature dates
    Given a negotiation where the provider has signed
    When I send "GET" to the provider CM at "/agreements/${aid}"
    Then the response status is 200
    And the response field "status" is "FINALIZING"
    And the response field "consumerSignatureDate" is an OpenAPI local date-time
    And the response field "providerSignatureDate" is an OpenAPI local date-time

  @openapi-only @D34 @D38 @stub @two-party @slow
  Scenario Outline: An ACTIVE contract reads as technical status FINALIZED at the <party>
    Given an active contract between both parties
    When I send "GET" to the <party> CM at "/agreements/${aid}"
    Then the response status is 200
    And the response field "status" is "FINALIZED"
    And the response field "contractOfferId" is "${offerId}"

    Examples:
      | party    |
      | provider |
      | consumer |

  @openapi-only @D34 @D38
  Scenario: A TERMINATED record reads as technical status TERMINATED
    Given a contract requested at the provider
    And the provider contract is terminated by the connector
    When I send "GET" to the provider CM at "/agreements/${aid}"
    Then the response status is 200
    And the response field "status" is "TERMINATED"

  @openapi-only @D34 @D12
  Scenario: Reading an unknown agreement gives 404
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/agreements/${aid}"
    Then the response is a problem with status 404 and type "notFound"

  @openapi-only @D34
  Scenario: The agreement file is not available before both signatures exist
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/agreements/${aid}/file"
    Then the response is a problem with status 404 and type "notFound"

  @openapi-only @D34
  Scenario: The agreement file of an unknown agreement gives 404
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/agreements/${aid}/file"
    Then the response is a problem with status 404 and type "notFound"

  @openapi-only @D34 @stub @two-party @slow
  Scenario: The agreement file of an active contract has the seven documented lines
    Given an active contract between both parties
    When I send "GET" to the provider CM at "/agreements/${aid}/file"
    Then the response status is 200
    And the response content type is "text/plain"
    And the response body lines match:
      | pattern                                                         |
      | CONTRACT AGREEMENT: ${aid}                                      |
      | CONTRACT DEFINITION: ${did}                                     |
      | ASSET: ${assetId}                                               |
      | PROVIDER ID: ${providerId}                                      |
      | PROVIDER SIGNATURE DATE: \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}    |
      | CONSUMER: ${consumerId}                                         |
      | CONSUMER SIGNATURE DATE: \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}    |

  @openapi-only @D34 @stub @two-party @slow
  Scenario: The search for established agreements finds an active contract
    Given an active contract between both parties
    When I send "POST" to the provider CM at "/agreements/search" with JSON:
      """
      {"assetId": "${assetId}", "consumerId": "${consumerId}"}
      """
    Then the response status is 200
    And the response list "items" has 1 items
    And the item of the response list "items" with "contractAgreementId" "${aid}" has:
      | field      | value         |
      | consumerId | ${consumerId} |

  @openapi-only @D34
  Scenario: The search for established agreements ignores contracts still being established
    Given a contract requested at the provider
    When I send "POST" to the provider CM at "/agreements/search" with JSON:
      """
      {"assetId": "${assetId}"}
      """
    Then the response status is 200
    And the response field "items" is an empty list

  @openapi-only @D34
  Scenario: The search for established agreements matches every given field
    Given a fresh unregistered asset id
    When I send "POST" to the provider CM at "/agreements/search" with JSON:
      """
      {"assetId": "${assetId}", "consumerId": "${consumerId}", "providerId": "${providerId}", "contractDefinitionId": "def-none"}
      """
    Then the response status is 200
    And the response field "items" is an empty list

  @openapi-only @D16 @D11
  Scenario: The search for established agreements refuses a value outside the pattern
    When I send "POST" to the provider CM at "/agreements/search" with JSON:
      """
      {"assetId": "bad asset id"}
      """
    Then the response is a problem with status 400 and type "validationFailure"
