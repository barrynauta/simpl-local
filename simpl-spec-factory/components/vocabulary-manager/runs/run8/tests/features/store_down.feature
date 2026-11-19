@store_down
Feature: The store is unreachable
  A second backend runs with its store URL on a port where nothing listens
  (STORE_DOWN_API_URL, build profile). Operations that need the store answer
  503 with a fixed problem body and Retry-After; GET /health answers DOWN.
  Run with ./run.sh store-down.

  Background:
    Given I use the backend whose store is down

  @D44 @D48 @NFR-07 @OWT-A10 @negative
  Scenario: A list request returns 503 with Retry-After
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @D8 @NFR-07 @OWT-A10 @negative
  Scenario: A create request returns 503 with Retry-After
    Given I am authenticated as "ga-admin"
    When I create the vocabulary "Down"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @negative
  Scenario: A version read returns 503
    Given I am authenticated as "ga-viewer"
    When I send a GET request to "/vocabularies/${Down}/versions/1"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the response header "Retry-After" is "5"

  @D44 @NFR-07 @MACH-08
  Scenario: GET /health reports DOWN with 503
    Given I am not authenticated
    When I send a GET request to "/health"
    Then the response status is 503
    And the response field "status" is "DOWN"

  @D44 @D17 @negative
  Scenario: The token check still runs first
    Given I am not authenticated
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
