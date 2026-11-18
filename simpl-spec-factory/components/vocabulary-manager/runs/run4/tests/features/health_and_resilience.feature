Feature: Health and behaviour when the store is down
  GET /health needs no token (D17). D44 fixes 503 bodies when the store is unreachable.
  Scenarios tagged @store_down need a separate deployment whose store URL points at a port
  where nothing listens; they are excluded by default (see README).

  @D44 @MACH-08 @BLG-hlth-res @API-36
  Scenario: Health reports UP when the store answers
    Given I am not authenticated
    When I send GET "/health"
    Then the response status is 200
    And the response content type is "application/json"
    And the field "status" is "UP"

  @D44 @MACH-08 @store_down @negative @API-44
  Scenario: Health reports DOWN when the store does not answer
    Given I am not authenticated
    When I send GET "/health"
    Then the response status is 503
    And the field "status" is "DOWN"

  @D44 @store_down @negative @API-44 @BLG-err-sanit
  Scenario: A read returns 503 with Retry-After when the store is unreachable
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @D8 @store_down @negative @API-44
  Scenario: A write returns 503 with Retry-After when the store is unreachable
    Given I am authenticated as "ga-admin"
    When I send POST "/vocabularies" with a valid multipart vocabulary form
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @D17 @store_down @negative
  Scenario: Authentication still runs first when the store is down
    Given I am not authenticated
    When I send GET "/vocabularies"
    Then the response status is 401
