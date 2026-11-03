Feature: Health, error format and common HTTP conventions
  Both backends expose a health check and answer errors as RFC 9457 problem
  details (digest D11 to D16).

  @openapi-only @D13
  Scenario: The provider CM health check answers without authentication
    When I send "GET" to the provider CM at "/health" without an API key
    Then the response status is 200
    And the response content type is "application/vnd.spring-boot.actuator.v3+json"
    And the response field "status" is "UP"

  @openapi-only @D13 @consumer
  Scenario: The consumer CM health check answers without authentication
    When I send "GET" to the consumer CM at "/health" without an API key
    Then the response status is 200
    And the response content type is "application/vnd.spring-boot.actuator.v3+json"
    And the response field "status" is "UP"

  @openapi-only @D13 @uibe
  Scenario Outline: The <party> UIBE health check answers without authentication
    When I send "GET" to the <party> UIBE at "/health" without credentials
    Then the response status is 200
    And the response content type is "application/vnd.spring-boot.actuator.v3+json"
    And the response field "status" is "UP"

    Examples: provider
      | party    |
      | provider |

    @consumer
    Examples: consumer
      | party    |
      | consumer |

  @openapi-only @D11
  Scenario: A wrong method is answered with 405 and a problem body
    When I send "DELETE" to the provider CM at "/health"
    Then the response is a problem with status 405 and type "methodNotSupported"

  @openapi-only @D11
  Scenario: An Accept header that excludes JSON is answered with 406
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/agreements/${aid}" with header "Accept" set to "application/xml"
    Then the response is a problem with status 406 and type "notAcceptable"

  @openapi-only @D11
  Scenario: A body that is not JSON is answered with 415
    Given fresh negotiation ids
    When I send "POST" to the provider CM at "/agreements/search" with content type "text/plain" and body "assetId=x"
    Then the response is a problem with status 415 and type "unsupportedMediaType"

  @openapi-only @D11 @D16
  Scenario: A contractAgreementId that is not a UUID is answered with 400 argumentTypeMismatch
    When I send "GET" to the provider CM at "/agreements/not-a-uuid"
    Then the response is a problem with status 400 and type "argumentTypeMismatch"

  @openapi-only @D16
  Scenario: A contractNegotiationId outside the allowed pattern is refused
    Given fresh negotiation ids
    And a fresh unregistered asset id
    And the variable "nid" is "bad_id!"
    When the connector asks the provider CM to check existence
    Then the response is a problem with status 400 and type "argumentTypeMismatch"

  @openapi-only @D16
  Scenario: An assetId longer than 100 characters is refused
    Given fresh negotiation ids
    And a fresh unregistered asset id
    And the variable "long" is a text of 101 characters
    When the connector asks the provider CM to check existence with "assetId" set to "${long}"
    Then the response is a problem with status 400 and type "validationFailure"

  @openapi-only @D11
  Scenario: An unknown path is answered with a problem body
    When I send "GET" to the provider CM at "/no-such-path"
    Then the response is a problem with status 404 and type "notFound"

  @openapi-only @D14
  Scenario: A given correlation id is echoed on the response
    When I send "GET" to the provider CM at "/health" with header "X-Correlation-Id" set to "corr-test-0001"
    Then the response status is 200
    And the response header "X-Correlation-Id" is "corr-test-0001"

  @openapi-only @D14
  Scenario: A correlation id is generated when none is given
    When I send "GET" to the provider CM at "/health"
    Then the response status is 200
    And the response header "X-Correlation-Id" is a UUID

  @openapi-only @D14 @D11
  Scenario: The correlation id is echoed on an error response too
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/agreements/${aid}" with header "X-Correlation-Id" set to "corr-err-0001"
    Then the response status is 404
    And the response header "X-Correlation-Id" is "corr-err-0001"

  @openapi-only @D14 @D61
  Scenario: The correlation id of the request is stored in the history entry
    Given fresh negotiation ids
    And a fresh unregistered asset id
    When I send "POST" to the provider CM at "/agreements/${nid}/search" with header "X-Correlation-Id" set to "corr-${nid}" and JSON:
      """
      {"contractAgreementId": "${aid}", "contractDefinitionId": "${did}", "assetId": "${assetId}",
       "providerId": "${providerId}", "consumerId": "${consumerId}"}
      """
    Then the response status is 200
    And the provider contract history has an entry with:
      | field         | value       |
      | toStatus      | REQUESTED   |
      | correlationId | corr-${nid} |

  @openapi-only @D14 @uibe
  Scenario: UIBE echoes the correlation id too
    When I send "GET" to the provider UIBE at "/health" without credentials
    Then the response status is 200
    And the response header "X-Correlation-Id" is a UUID
