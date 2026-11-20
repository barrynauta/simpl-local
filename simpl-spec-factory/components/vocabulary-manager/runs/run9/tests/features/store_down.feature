@store_down
Feature: Behaviour when the vocabulary store is not reachable
  Run with './run.sh store-down' against the second backend whose store URL points to a port where
  nothing listens (STORE_DOWN_API_URL, D44). The identity provider is up.

  @D44 @negative @API-44 @ARCH-35 @OWT-A10
  Scenario: Listing vocabularies returns 503 with Retry-After
    When I send a "GET" request to "/vocabularies"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"
    And the response field "items" is absent or null

  @D44 @D8 @negative @API-44 @OWT-A10
  Scenario: Creating a vocabulary returns 503 with Retry-After
    When I create the vocabulary "Offline"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @negative
  Scenario Outline: Other store operations return 503 (<method> <path>)
    When I send a "<method>" request to "<path>"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the response header "Retry-After" is "5"

    Examples:
      | method | path                               |
      | GET    | /vocabularies/Offline${run}         |
      | GET    | /vocabularies/Offline${run}/versions |
      | GET    | /content/Offline${run}              |
      | GET    | /external-vocabularies              |

  @D44 @MACH-08 @NFR-07 @BLG-hlth-res
  Scenario: The health check reports DOWN with 503
    Given I am "anonymous"
    When I send a "GET" request to "/health"
    Then the response status is 503
    And the response field "status" is "DOWN"

  @D44 @D17 @negative
  Scenario: Without a token the request is still refused with 401, before the store is used
    Given I am "anonymous"
    When I send a "GET" request to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D44 @D16 @negative
  Scenario: A viewer who tries to write is still refused with 403
    Given I am "ga-viewer"
    When I create the vocabulary "Offline"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
