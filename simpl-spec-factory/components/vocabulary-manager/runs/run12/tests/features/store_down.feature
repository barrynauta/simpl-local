@store_down
Feature: Behaviour while the vocabulary store is unreachable
  Run with ./run.sh store-down against STORE_DOWN_API_URL, a backend whose store URL points
  to a port where nothing listens (D44). Not part of the default API run.

  @D44 @D48 @NFR-07 @ARCH-35 @API-44
  Scenario: A read answers 503 with Retry-After when the store is down
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" equals "5"

  @D44 @D8 @API-44
  Scenario: A create answers 503 with Retry-After when the store is down
    Given I am authenticated as "ga-admin"
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value            |
      | name           | {{Health}}       |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | file:valid.ttl   |
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" equals "5"

  @D44 @NFR-07 @MACH-08 @BLG-hlth-res
  Scenario: Health reports DOWN when the store does not answer
    Given I am not authenticated
    When I send "GET" to "/health"
    Then the status is 503
    And the response field "status" equals "DOWN"

  @D17 @D44 @D162 @ARCH-35
  Scenario: Token checks still work while the store is down
    Given I am not authenticated
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    Given I am authenticated as "ga-norole"
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"

  @D162 @ARCH-35 @negative
  Scenario: A query error is reported without touching the store
    Given I am authenticated as "ga-admin"
    When I send "GET" to "/vocabularies?pageSize=0"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D51 @negative
  Scenario: A field error on a write is reported before the store is needed
    Given I am authenticated as "ga-admin"
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | lower{{run}}   |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
