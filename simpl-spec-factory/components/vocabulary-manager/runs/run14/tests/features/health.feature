Feature: Health of the Vocabulary Manager API
  GET /health needs no token and reports UP, DEGRADED or DOWN (D44, D108, D134).

  @D44 @D108 @D17 @MACH-08 @NFR-07 @API-36
  Scenario: Health is UP without a token while the store answers
    Given I am not authenticated
    When I send a GET request to "/health"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "status" equals "UP"

  @D44 @D17 @MACH-08
  Scenario: Health ignores a valid token and still answers
    Given I am authenticated as "ga-norole"
    When I send a GET request to "/health"
    Then the response status is 200
    And the response field "status" equals "UP"

  @D149 @D21 @negative @BLG-err-problem
  Scenario: An unknown query parameter on GET /health is rejected
    Given I am not authenticated
    When I send a GET request to "/health?verbose=true"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "query" named "verbose"
