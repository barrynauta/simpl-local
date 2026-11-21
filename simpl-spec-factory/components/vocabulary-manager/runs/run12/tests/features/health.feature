Feature: Health of the Vocabulary Manager API
  GET /health needs no token (D17) and reports UP, DEGRADED or DOWN (D44, D108, D134).

  @D17 @D44 @D108 @NFR-07 @MACH-08 @BLG-hlth-res
  Scenario: Health answers without a token while the store is up
    Given I am not authenticated
    When I send "GET" to "/health"
    Then the status is 200
    And the response media type is "application/json"
    And the response field "status" matches "UP|DEGRADED"

  @D17 @D44 @NFR-07
  Scenario: Health also answers with a token of a user without a role
    Given I am authenticated as "ga-norole"
    When I send "GET" to "/health"
    Then the status is 200

  @D149 @D21 @negative @BLG-req-valid
  Scenario: An unknown query parameter on GET /health is rejected
    Given I am not authenticated
    When I send "GET" to "/health?verbose=true"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "verbose"
