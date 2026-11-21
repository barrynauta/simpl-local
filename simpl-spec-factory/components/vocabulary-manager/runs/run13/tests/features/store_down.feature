@store_down
Feature: The vocabulary store is not reachable
  Runs only with ./run.sh store-down, against the second backend whose store URL points at a
  port where nothing listens (STORE_DOWN_API_URL, D44). The checks that do not need the store
  still answer (D17, D162).

  @D44 @MACH-08 @NFR-07 @BLG-hlth-res
  Scenario: The health operation reports DOWN with 503
    Given I am not authenticated
    When I send GET "/health"
    Then the response status is 503
    And the response JSON has
      | field  | value |
      | status | DOWN  |

  @D44 @D48 @negative @ARCH-35
  Scenario Outline: <operation> returns 503 when the store is down
    Given I am authenticated as "ga-admin"
    When I call the operation "<operation>"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

    Examples:
      | operation                 |
      | getVocabularies           |
      | createVocabulary          |
      | getVocabulary             |
      | getVocabularyVersions     |
      | createVocabularyVersion   |
      | updateVocabularyStatus    |
      | searchVocabularies        |
      | downloadVocabulary        |
      | getExternalVocabularies   |
      | createExternalVocabulary  |

  @D44 @D17 @negative
  Scenario: Without a token the answer is still 401
    Given I am not authenticated
    When I send GET "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D44 @D17 @negative
  Scenario: Without the admin role a write is still 403
    Given I am authenticated as "ga-viewer"
    When I call the operation "createVocabulary"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D44 @D162 @negative
  Scenario: A query error is reported without touching the store
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies?pageSize=101"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
