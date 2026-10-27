@api @openapi-only
Feature: Health
  The service reports its health without authentication (OpenAPI: getHealth, security []).

  Scenario: Health is available without a token
    Given I am not authenticated
    When I send GET "/health"
    Then the response status is 200
    And the response JSON at "status" is "UP"
