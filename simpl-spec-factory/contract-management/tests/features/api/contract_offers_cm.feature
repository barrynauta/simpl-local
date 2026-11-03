Feature: Offer presentation and offer status updates at the Consumer CM
  CM POST /contractOffers/{nid} replaces the offer presentation and forwards it
  to UIBE. PUT /contractOffers/{nid}/update is how the connector tells the
  Consumer that the Provider rejected or failed (D33, D10, E11).

  @openapi-only @D33 @D9 @consumer @stub @uibe @oidc
  Scenario: A new presentation of a REQUESTED offer is stored and forwarded to UIBE, which keeps the offer status
    Given an offer created at the consumer
    When the consumer CM forwards a new offer presentation "<p>Revised offer ${nid}</p>"
    Then the response status is 200
    And the response fields are:
      | field                 | value     |
      | contractNegotiationId | ${nid}    |
      | status                | INITIATED |
    And the consumer contract has:
      | field             | value                       |
      | humanReadable     | <p>Revised offer ${nid}</p> |
      | usageContractHash | ${newHash}                  |
    When I send "GET" to the consumer UIBE at "/contractOffers/${nid}" as "consumer-admin"
    Then the response fields are:
      | field             | value                       |
      | humanReadableData | <p>Revised offer ${nid}</p> |
      | hashData          | ${newHash}                  |
      | status            | CREATED                     |

  @openapi-only @D33 @stub @two-party
  Scenario: A new presentation is refused once the offer is no longer REQUESTED
    Given a negotiation where the consumer has signed
    When the consumer CM forwards a new offer presentation "<p>Too late</p>"
    Then the response is a problem with status 400 and type "offerNotPending"

  @openapi-only @D33 @D12 @consumer
  Scenario: A new presentation for an unknown negotiation is refused with 400
    Given fresh negotiation ids
    When the consumer CM forwards a new offer presentation "<p>x</p>"
    Then the response is a problem with status 400 and type "validationFailure"

  @openapi-only @D33 @D2
  Scenario: A new presentation sent to the Provider CM is refused with 400
    Given a contract requested at the provider
    When I send "POST" to the provider CM at "/contractOffers/${nid}" with JSON:
      """
      {"humanReadable": "<p>x</p>", "hash": "2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881"}
      """
    Then the response is a problem with status 400

  @openapi-only @D33 @D37 @E11 @consumer @stub
  Scenario: The connector reports REJECTED for a REQUESTED Consumer contract, which fails with PROVIDER_REJECTED
    Given an offer created at the consumer
    When the connector reports the offer status "REJECTED" to the consumer CM
    Then the response status is 200
    And the response field "status" is "INITIATED"
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field      | value             |
      | reasonCode | PROVIDER_REJECTED |
    And the consumer contract history has an entry with:
      | field      | value                                 |
      | toStatus   | FAILED                                |
      | reason     | Provider validation or signing failed |

  @openapi-only @D33 @D37 @E11 @stub @two-party
  Scenario: The connector reports REJECTED for a SIGNED Consumer contract, which fails with PROVIDER_REJECTED
    Given a negotiation where the consumer has signed
    When the connector reports the offer status "REJECTED" to the consumer CM
    Then the response status is 200
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field      | value             |
      | reasonCode | PROVIDER_REJECTED |

  @openapi-only @D33 @consumer @stub
  Scenario Outline: The connector reports <status>, which only adds a history entry
    Given an offer created at the consumer
    When the connector reports the offer status "<status>" to the consumer CM
    Then the response status is 200
    And the response field "status" is "INITIATED"
    And the consumer contract stays in status "REQUESTED"

    Examples:
      | status   |
      | APPROVED |
      | CREATED  |

  @openapi-only @D10 @consumer @stub
  Scenario Outline: The connector reports an offer status outside the CM vocabulary (<status>)
    Given an offer created at the consumer
    When the connector reports the offer status "<status>" to the consumer CM
    Then the response is a problem with status 400 and type "validationFailure"
    And the consumer contract stays in status "REQUESTED"

    Examples:
      | status   |
      | SIGNED   |
      | ACCEPTED |

  @openapi-only @D33 @D12 @consumer
  Scenario: A status update for an unknown negotiation is refused with 400
    Given fresh negotiation ids
    When the connector reports the offer status "REJECTED" to the consumer CM
    Then the response is a problem with status 400 and type "validationFailure"
    And the problem detail contains "Unknown contractNegotiationId"

  @openapi-only @D11 @consumer
  Scenario: A status update with a non-JSON body is refused with 415
    Given fresh negotiation ids
    When I send "PUT" to the consumer CM at "/contractOffers/${nid}/update" with content type "text/plain" and body "REJECTED"
    Then the response is a problem with status 415 and type "unsupportedMediaType"
