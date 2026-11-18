Feature: Error format, health and an unavailable store
  Errors are RFC 9457 problem details (application/problem+json). Problem types are
  belgif for 400, 404, 500 and 503 and simpl for 401, 403, 409, 413 and 415 (D48).

  @D55 @negative @BLG-err-problem @API-44
  Scenario: An unknown path gives 404 as a problem
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I send "GET" to "/no-such-resource-{run}"
    Then the response status is 404 with a problem body

  @D55 @negative @BLG-err-problem
  Scenario Outline: A method the resource does not support gives 405 as a problem (<method> <path>)
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    And a vocabulary "{Alpha}" exists
    When I send "<method>" to "<path>"
    Then the response status is 405 with a problem body

    Examples:
      | method | path                             |
      | DELETE | /vocabularies/{Alpha}            |
      | PUT    | /vocabularies/{Alpha}            |
      | DELETE | /vocabularies/{Alpha}/versions/1 |

  @D48 @negative @BLG-err-sanit @OWT-A02 @OWA-API8 @API-44
  Scenario Outline: Problem bodies reveal no implementation details (<case>)
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    And a vocabulary "{Alpha}" exists
    When I send "<method>" to "<path>" with JSON
      """
      <body>
      """
    Then the response status is <status> with a problem body
    And the problem reveals no implementation details

    Examples:
      | case                     | method | path                         | body                     | status |
      | malformed JSON           | PATCH  | /vocabularies/{Alpha}        | {"status":               | 400    |
      | wrong JSON type          | PATCH  | /vocabularies/{Alpha}        | ["DEPRECATED"]           | 400    |
      | unknown version          | PATCH  | /vocabularies/{Alpha}/versions/99999 | {"status": "ACTIVE"} | 404 |
      | malformed search         | POST   | /vocabularies/searches       | {"items": "all"}         | 400    |

  @D9 @negative @BLG-err-sanit @OWT-A02
  Scenario: A syntax error report reveals no implementation details
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I create the vocabulary "{Alpha}" with the fixture "syntax-error.ttl"
    Then the response is a problem with status 400
    And the problem reveals no implementation details

  @D17 @negative @BLG-err-sanit
  Scenario: A rejected token reveals no implementation details
    Given I send a token with a tampered signature
    When I send "GET" to "/vocabularies?name={run}"
    Then the response is a problem with status 401
    And the problem reveals no implementation details

  @D44 @MACH-08 @BLG-hlth-res
  Scenario: The health operation reports UP when the store answers
    Given I am not authenticated
    When I send "GET" to "/health"
    Then the response status is 200
    And the response content type is "application/json"
    And the response field "status" equals "UP"

  @D44 @D8 @store-down @negative @ARCH-06 @OWT-A10 @API-44
  Scenario: A read fails with 503 and Retry-After when the store is unreachable
    Given the API runs with an unreachable store
    And I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 503
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @D8 @store-down @negative @ARCH-06 @OWT-A10
  Scenario: A write fails with 503 and Retry-After when the store is unreachable
    Given the API runs with an unreachable store
    And I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I create the vocabulary "{Alpha}"
    Then the response is a problem with status 503
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @store-down @MACH-08
  Scenario: The health operation reports DOWN when the store is unreachable
    Given the API runs with an unreachable store
    And I am not authenticated
    When I send "GET" to "/health"
    Then the response status is 503
    And the response field "status" equals "DOWN"

  @D44 @D17 @store-down @negative
  Scenario: Access rules still apply when the store is unreachable
    Given the API runs with an unreachable store
    And I am not authenticated
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 401
