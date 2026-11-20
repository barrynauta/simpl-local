@wip
Feature: Criteria that a black-box API run cannot trigger
  These scenarios document the expected behaviour. They need fault injection inside the component,
  which a black-box HTTP suite cannot do. They are excluded from every run mode.

  @AC_SIMPL-18162_AC4 @D24 @D44 @negative @API-44
  Scenario: An internal error while listing returns 500 with a detail and no metadata
    Given the store answers the list query with an error that is not a timeout or a connection failure
    When I send a "GET" request to "/vocabularies"
    Then the response is a problem with status 500 and type "urn:problem-type:belgif:internalServerError"
    And the problem has a detail
    And the response field "items" is absent or null
