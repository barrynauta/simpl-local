Feature: Health of the Vocabulary Manager API
  GET /health needs no token and reports UP, DEGRADED or DOWN (D17, D44).

  @D17 @D44 @MACH-08 @NFR-07 @BLG-hlth-res
  Scenario: The health operation answers UP without a token when the store is reachable
    When I request the health status without a token
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "status" equals "UP"

  @D17 @D16 @NFR-07
  Scenario: The health operation needs no role
    When "ga-norole" sends GET "/health"
    Then the response status is 200
    And the response field "status" equals "UP"
