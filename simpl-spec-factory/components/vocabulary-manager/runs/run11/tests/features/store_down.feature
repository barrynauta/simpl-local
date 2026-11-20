@store_down
Feature: Behaviour when the vocabulary store is not reachable
  These scenarios run only with ./run.sh store-down, against the second backend at
  STORE_DOWN_API_URL whose store URL points at a port where nothing listens (D44, build profile).

  @D44 @MACH-08 @NFR-07 @BLG-hlth-res @negative
  Scenario: The health operation reports DOWN
    Given I am not authenticated
    When I send a GET request to "/health"
    Then the response status is 503
    And the response field "status" is "DOWN"

  @D44 @D48 @ARCH-06 @negative
  Scenario: Reading the vocabulary list returns 503 with Retry-After
    Given I am a vocabulary viewer
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"
    And the problem reveals no implementation details

  @D44 @D8 @negative
  Scenario: Creating a vocabulary returns 503 with Retry-After
    Given I am a vocabulary administrator
    And a create vocabulary request for "A"
    When I submit the request
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @negative
  Scenario Outline: Other store reads return 503: <path>
    Given I am a vocabulary administrator
    When I send a GET request to "<path>"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the response header "Retry-After" is "5"

    Examples:
      | path                           |
      | /vocabularies/${A}             |
      | /vocabularies/${A}/versions    |
      | /content/${A}                  |
      | /external-vocabularies         |

  @D44 @D17 @negative
  Scenario: The token is checked before the store is needed
    Given I am not authenticated
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D44 @D17 @negative
  Scenario: The role is checked before the store is needed
    Given I am a vocabulary viewer
    And a create vocabulary request for "A"
    When I submit the request
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D44 @D21 @D22 @negative
  Scenario: Request shape errors are reported before the store is needed
    Given I am a vocabulary administrator
    When I send a GET request to "/vocabularies?pageSize=101"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
