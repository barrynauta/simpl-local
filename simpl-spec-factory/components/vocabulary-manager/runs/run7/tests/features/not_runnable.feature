@wip
Feature: Behaviour that cannot be provoked through the API
  Kept as Gherkin so the coverage table points at them. Not part of any run mode.

  @AC_SIMPL-18162_AC4 @D44 @negative
  Scenario: An internal error on the list gives 500 with the problem detail
    Given the store answers a query with an unexpected error
    When "ga-admin" lists vocabularies with the query "name=${rid}"
    Then the response is a problem with status 500 and type "urn:problem-type:belgif:internalServerError"
    And the response field "items" is absent

  @D44 @negative
  Scenario: A token with an unknown key id while the identity provider is down fails closed with 503
    Given the identity provider is down and the token names an unknown key id
    When "ga-admin" lists vocabularies with the query "name=${rid}"
    Then the response is a problem with status 503 and type "urn:problem-type:belgif:serviceUnavailable"
    And the problem detail is "The identity provider is not available. Retry later."
