@uibe @consumer @stub
Feature: Offer operations of the contract UI backend
  UIBE keeps its own offer table. CM creates offers with the UIBE API key; the
  UI reads and decides with a user token (D7 to D10, D66, B.5).

  @openapi-only @D8 @D9 @oidc
  Scenario: The offer list is a JSON array with the example field names
    Given an offer created at the consumer
    When I send "GET" to the consumer UIBE at "/contractOffers" as "consumer-admin"
    Then the response status is 200
    And the response is a JSON list
    And the response list "." contains an item with "contractNegotiationId" "${nid}"
    And the item of the response list "." with "contractNegotiationId" "${nid}" has:
      | field        | value   |
      | status       | CREATED |
      | contractData | null    |

  @openapi-only @D9 @oidc
  Scenario: One offer is read with the example field names
    Given an offer created at the consumer
    When I send "GET" to the consumer UIBE at "/contractOffers/${nid}" as "consumer-admin"
    Then the response status is 200
    And the response field "contractNegotiationId" is "${nid}"
    And the response field "humanReadableData" is not null
    And the response field "hashData" is the SHA-256 of the response field "humanReadableData"
    And the response field "status" is "CREATED"
    And the response field "createdDate" is an OpenAPI local date-time
    And the response field "lastUpdatedDate" is an OpenAPI local date-time
    And the response field "humanReadable" is absent
    And the response field "hash" is absent

  @openapi-only @D12 @oidc
  Scenario: Reading an unknown offer gives 404
    Given fresh negotiation ids
    When I send "GET" to the consumer UIBE at "/contractOffers/${nid}" as "consumer-admin"
    Then the response is a problem with status 404

  @openapi-only @D8 @oidc
  Scenario: The offer list of a party without offers is an empty array, not 404
    When I send "GET" to the provider UIBE at "/contractOffers" as "provider-admin"
    Then the response status is 200
    And the response is a JSON list

  @openapi-only @D66 @D9
  Scenario: CM-side creation of an offer row with the API key answers 201 with offerStatus and offerUrl
    Given fresh negotiation ids
    When I send "POST" to the consumer UIBE at "/contractOffers/${nid}" with the UIBE API key and JSON:
      """
      {"humanReadable": "<p>Offer ${nid}</p>", "hash": "1111111111111111111111111111111111111111111111111111111111111111"}
      """
    Then the response status is 201
    And the response field "offerStatus" is "CREATED"
    And the response field "offerUrl" contains "/contractOffers/${nid}"

  @openapi-only @D66 @D9 @oidc
  Scenario: Posting an offer row again replaces the presentation and keeps the status
    Given an offer created at the consumer
    And the consumer accepts the offer
    When I send "POST" to the consumer UIBE at "/contractOffers/${nid}" with the UIBE API key and JSON:
      """
      {"humanReadable": "<p>Replaced ${nid}</p>", "hash": "2222222222222222222222222222222222222222222222222222222222222222"}
      """
    Then the response status is 201
    And the response field "offerStatus" is "ACCEPTED"
    And the consumer UIBE offer has status "ACCEPTED"

  @openapi-only @D66 @rbac
  Scenario: Creating an offer row without the API key is refused with 401
    Given fresh negotiation ids
    When I send "POST" to the consumer UIBE at "/contractOffers/${nid}" with the API key "wrong-key" and JSON:
      """
      {"humanReadable": "<p>x</p>", "hash": "x"}
      """
    Then the response is a problem with status 401 and type "unauthorized"

  @openapi-only @D66 @rbac @oidc
  Scenario: Creating an offer row with a user token instead of the API key is refused with 401
    Given fresh negotiation ids
    When I send "POST" to the consumer UIBE at "/contractOffers/${nid}" as "consumer-admin" with JSON:
      """
      {"humanReadable": "<p>x</p>", "hash": "x"}
      """
    Then the response is a problem with status 401 and type "unauthorized"

  @openapi-only @D9 @D11
  Scenario: Creating an offer row without humanReadable is refused with 400
    Given fresh negotiation ids
    When I send "POST" to the consumer UIBE at "/contractOffers/${nid}" with the UIBE API key and JSON:
      """
      {"hash": "x"}
      """
    Then the response is a problem with status 400 and type "validationFailure"

  @openapi-only @D11 @oidc
  Scenario: An offer decision other than accept or reject is refused with 400
    Given an offer created at the consumer
    When the consumer user "consumer-admin" sends the offer decision "approve"
    Then the response is a problem with status 400 and type "argumentTypeMismatch"
    And the consumer UIBE offer still has status "CREATED"
