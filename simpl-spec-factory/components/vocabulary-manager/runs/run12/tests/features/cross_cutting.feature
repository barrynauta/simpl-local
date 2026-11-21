Feature: Cross-cutting HTTP behaviour
  Correlation id (D86), problem bodies (D48, D55, D158), errors not reachable from outside.

  @D86
  Scenario: A correlation id sent by the client is returned
    Given I am authenticated as "ga-admin"
    And the request header "X-Correlation-Id" is "corr-{{run}}"
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 200
    And the response header "X-Correlation-Id" equals "corr-{{run}}"

  @D86
  Scenario: A correlation id is created when the client sends none
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 200
    And the response header "X-Correlation-Id" is present

  @D86
  Scenario: A W3C traceparent header is accepted
    Given I am authenticated as "ga-admin"
    And the request header "traceparent" is "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 200

  @D158 @negative
  Scenario: A URL that is too long gives 414
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/vocabularies?name={{run}}" followed by a query of 200000 characters
    Then the status is 414
    And the response media type is "application/problem+json"

  @D158 @D122 @negative
  Scenario: Request headers that are too large give 431, or 400 when the front proxy answers
    Given I am authenticated as "ga-admin"
    And the request header "X-Padding" is a value of 200000 characters
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 431 or 400
    And the response media type is "application/problem+json"

  @AC_SIMPL-18162_AC4 @wip
  Scenario: An internal error answers 500 with a problem body and no metadata
    # No black-box way to cause a 500: the store outage gives 503 (D44). Kept for step 70 fault injection.
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 500 and type "urn:problem-type:belgif:internalServerError"
    And the response field "items" is absent
