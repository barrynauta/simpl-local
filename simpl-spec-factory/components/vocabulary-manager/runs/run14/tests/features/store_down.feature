@store_down
Feature: Behaviour when the vocabulary store is down
  Run with ./run.sh store-down against the second backend whose store URL points where nothing
  listens (STORE_DOWN_API_URL, D44). Reads, searches and writes answer 503 with Retry-After 5;
  GET /health answers 503 DOWN. Checks that need no store still run first (D51, D162).

  @D44 @D103 @NFR-07 @ARCH-35 @negative
  Scenario: GET /health reports DOWN with 503
    Given I am not authenticated
    When I send a GET request to "/health"
    Then the response status is 503
    And the response field "status" equals "DOWN"

  @D44 @D103 @negative @API-44 @AC_SIMPL-18162_AC4
  Scenario Outline: An operation that needs the store answers 503 with Retry-After: <method> <path>
    Given I am authenticated as "ga-admin"
    When I send a <method> request to "<path>"
    Then the response is a problem with status 503
    And the problem type is "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"
    And the response holds no items

    Examples:
      | method | path                            |
      | GET    | /vocabularies                   |
      | GET    | /vocabularies/$Alpha            |
      | GET    | /vocabularies/$Alpha/versions   |
      | GET    | /vocabularies/$Alpha/versions/1 |
      | GET    | /content/$Alpha                 |
      | GET    | /external-vocabularies          |

  @D44 @D8 @negative
  Scenario: Creating a vocabulary answers 503 with Retry-After
    Given I am authenticated as "ga-admin"
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 503
    And the problem type is "urn:problem-type:belgif:serviceUnavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @negative
  Scenario: Creating a new version answers 503 with Retry-After
    Given I am authenticated as "ga-admin"
    When I create a new version of "$Alpha"
    Then the response is a problem with status 503
    And the problem type is "urn:problem-type:belgif:serviceUnavailable"
    And the response header "Retry-After" is "5"

  @D44 @negative
  Scenario: A status change answers 503 with Retry-After
    Given I am authenticated as "ga-admin"
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 503
    And the problem type is "urn:problem-type:belgif:serviceUnavailable"
    And the response header "Retry-After" is "5"

  @D44 @D38 @negative
  Scenario: A search answers 503 with Retry-After
    Given I am authenticated as "ga-viewer"
    When I send a POST request to "/vocabularies/searches" with JSON:
      """
      {"items": [{"name": "$Alpha"}]}
      """
    Then the response is a problem with status 503
    And the problem type is "urn:problem-type:belgif:serviceUnavailable"
    And the response header "Retry-After" is "5"

  @D44 @D17 @D162 @negative
  Scenario: Without a token the answer is still 401
    Given I am not authenticated
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401
    And the problem type is "urn:problem-type:simpl:unauthorized"

  @D44 @D17 @D162 @negative
  Scenario: Without a role the answer is still 403
    Given I am authenticated as "ga-norole"
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 403

  @D162 @negative
  Scenario: A bad query value is reported without touching the store
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies?pageSize=0"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "pageSize"

  @D51 @negative
  Scenario: A malformed create request is reported before the store is needed
    Given I am authenticated as "ga-admin"
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "description"
