@store_down
Feature: Behaviour when the vocabulary store is not available
  These scenarios run against a second backend whose store URL points at a port where nothing
  listens (STORE_DOWN_API_URL, decision D44). Run them with ./run.sh store-down.

  @D44 @MSV-07 @ARCH-06 @negative
  Scenario: The list returns 503 with Retry-After when the store is down
    Given I am authenticated as "ga-admin"
    When I list the vocabularies with ""
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem title is "Service Unavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"
    And the response took less than 30 seconds

  @D44 @D8 @NFR-07 @negative
  Scenario: A create request returns 503 with Retry-After when the store is down
    Given I am authenticated as "ga-admin"
    When I create the vocabulary "Health"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem detail is "The vocabulary store is not available. Retry later."
    And the response header "Retry-After" is "5"

  @D44 @MACH-08 @NFR-07 @BLG-hlth-res @negative
  Scenario: The health operation reports DOWN when the store does not answer
    Given I am not authenticated
    When I send "GET" to "/health"
    Then the response status is 503
    And the health status is "DOWN"

  @D17 @D44 @SOA-05 @ARCH-35 @negative
  Scenario: Requests without a token are still refused with 401 while the store is down
    Given I am not authenticated
    When I list the vocabularies with ""
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"

  @D16 @D44 @SOA-05 @negative
  Scenario: Requests without a role are still refused with 403 while the store is down
    Given I am authenticated as "ga-norole"
    When I list the vocabularies with ""
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
