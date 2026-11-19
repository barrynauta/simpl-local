@store_down
Feature: Behaviour when the store is not reachable
  Runs only with ./run.sh store-down, against a backend whose store URL points at a port where
  nothing listens (STORE_DOWN_API_URL, D44). Scenarios here create nothing.

  @D44 @MACH-08 @NFR-07 @BLG-hlth-res
  Scenario: The health operation reports DOWN
    When I request the health status without a token
    Then the response status is 503
    And the response field "status" equals "DOWN"

  @D44 @negative @API-44
  Scenario Outline: An operation that needs the store returns 503 with Retry-After (<operation>)
    When "ga-admin" calls the operation "<operation>"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

    Examples:
      | operation                          |
      | GET /vocabularies                  |
      | POST /vocabularies                 |
      | GET /vocabularies/{vocabularyName} |
      | POST /vocabularies/searches        |
      | GET /external-vocabularies         |

  @D44 @D17 @ARCH-35 @negative
  Scenario: Authentication still works without the store
    When "anonymous" calls the operation "GET /vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    When "ga-norole" calls the operation "GET /vocabularies"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
