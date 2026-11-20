@store_down
Feature: Behaviour when the vocabulary store is unreachable
  Runs against the second backend whose store URL points where nothing listens (STORE_DOWN_API_URL).
  The operation answers 503 with Retry-After and stores nothing (D44); token and role checks are
  local and still come first (D17, D44).

  @D48 @D44 @D103 @negative
  Scenario Outline: <method> <path> gives 503 Service Unavailable with Retry-After
    Given I use the store-down backend
    And I use the token of "ga-admin"
    When I send "<method>" to "<path>"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the header "Retry-After" is "5"

    Examples:
      | method | path                                  |
      | GET    | /vocabularies                         |
      | GET    | /vocabularies/AnyVocabulary           |
      | GET    | /vocabularies/AnyVocabulary/versions  |
      | GET    | /external-vocabularies                |

  @D44 @D8 @negative
  Scenario: A create gives 503 Service Unavailable with Retry-After
    Given I use the store-down backend
    And I use the token of "ga-admin"
    When I create the vocabulary "A"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the header "Retry-After" is "5"

  @D44 @D38 @negative
  Scenario: A search gives 503 Service Unavailable
    Given I use the store-down backend
    And I use the token of "ga-viewer"
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "AnyVocabulary"}]}
      """
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the header "Retry-After" is "5"

  @D44 @NFR-07 @MACH-08
  Scenario: Health reports DOWN with 503
    Given I use the store-down backend
    And I use no token
    When I send "GET" to "/health"
    Then the status is 503
    And the response field "status" is "DOWN"

  @D17 @D44 @ARCH-35 @negative
  Scenario: Without a token the store-down backend still answers 401
    Given I use the store-down backend
    And I use no token
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D17 @D44 @ARCH-35 @negative
  Scenario: A viewer still gets 403 on a write when the store is down
    Given I use the store-down backend
    And I use the token of "ga-viewer"
    When I create the vocabulary "A"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
