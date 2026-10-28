@api @health @D3
Feature: Every service answers a health check without authentication
  Digest D3 and 5.1: GET /health answers 200 {"status":"UP"} without a token.

  @cat
  Scenario: catalogue-be A is healthy
    When the health of "cat-a" is requested without a token
    Then the status is 200
    And the field "status" is "UP"

  @qma
  Scenario: query mapper adapter A is healthy
    When the health of "qma-a" is requested without a token
    Then the status is 200
    And the field "status" is "UP"

  @cbe
  Scenario: client backend A is healthy
    When the health of "cbe-a" is requested without a token
    Then the status is 200
    And the field "status" is "UP"

  @federation @cat
  Scenario: catalogue-be B is healthy
    When the health of "cat-b" is requested without a token
    Then the status is 200
    And the field "status" is "UP"

  @federation @qma
  Scenario Outline: adapter <service> is healthy
    When the health of "<service>" is requested without a token
    Then the status is 200
    And the field "status" is "UP"

    Examples:
      | service |
      | qma-b   |
      | qma-c   |

  @stub
  Scenario Outline: stub <service> is healthy
    When the health of "<service>" is requested without a token
    Then the status is 200
    And the field "status" is "UP"

    Examples:
      | service      |
      | schema-stub  |
      | quality-stub |
