Feature: Criteria that cannot run against the API from outside
  Kept as documentation; tagged @wip and left out of every run mode.

  @wip @AC_SIMPL-18162_AC4 @D24 @negative
  Scenario: An internal error while listing vocabularies gives 500 with the problem detail
    # A black-box API test cannot make the backend fail with a non-store error. The UI tests
    # stub a 500 (D24, ui_list.feature); the store-down case is 503 (D44, store_down.feature).
    Given I am authenticated as "ga-admin"
    When I send GET "/vocabularies"
    Then the response is a problem with status 500 and type "urn:problem-type:belgif:internalServerError"
    And the response JSON has
      | field | value    |
      | items | <absent> |
