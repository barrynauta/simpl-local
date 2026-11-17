@api
Feature: Retrieve, filter and sort the vocabulary list via the API
  Each scenario creates its own vocabularies whose names share the scenario's run id {RUN},
  and filters on that run id so other data does not interfere (name filter = contains, D23).

  Background:
    Given I am authenticated as "admin"

  @SIMPL-18162
  Scenario: The list is paginated and items carry the metadata
    Given a vocabulary "{Alpha}" exists
    When I send GET "/vocabularies?name={RUN}"
    Then the response status is 200
    And the list contains "{Alpha}"
    And the response JSON at "items.0.name" is "{Alpha}"
    And the response JSON at "items.0.version" is 1
    And the response JSON at "items.0.status" is "ACTIVE"
    And the response JSON has a non-empty "items.0.description"
    And the response JSON has a non-empty "items.0.creationDate"
    And the response JSON has a non-empty "items.0.lastUpdateDate"
    And the response JSON has a non-empty "page"

  @SIMPL-18162 @D29
  Scenario Outline: Wrong pagination parameters are rejected
    When I send GET "/vocabularies?<query>"
    Then the response is a problem with status 400

    Examples:
      | query          |
      | page=0         |
      | page=abc       |
      | pageSize=0     |
      | pageSize=101   |

  @SIMPL-18164 @D23
  Scenario: Filter by status
    Given a vocabulary "{Alpha}" exists
    And a vocabulary "{Beta}" exists
    And vocabulary "{Beta}" is deprecated
    When I send GET "/vocabularies?name={RUN}&status=DEPRECATED"
    Then the response status is 200
    And the list contains "{Beta}"
    And the list does not contain "{Alpha}"
    And every item in the list has "status" "DEPRECATED"

  @SIMPL-18164 @SIMPL-18163 @D23
  Scenario: Filter by part of the name
    Given a vocabulary "{Alpha}" exists
    When I send GET "/vocabularies?name={RUN}"
    Then the list contains "{Alpha}"

  @SIMPL-18164
  Scenario: A filter without matches returns an empty list, not an error
    When I send GET "/vocabularies?name=Nomatch{RUN}"
    Then the response status is 200
    And the response JSON at "items" is an empty list

  @SIMPL-18164
  Scenario Outline: Sort by a supported field
    Given a vocabulary "{Charlie}" exists
    And a vocabulary "{Alpha}" exists
    And a vocabulary "{Bravo}" exists
    When I send GET "/vocabularies?name={RUN}&sort=<sort>"
    Then the response status is 200
    And the list has 3 items
    And the list is ordered by "name" <direction>

    Examples:
      | sort  | direction  |
      | name  | ascending  |
      | -name | descending |

  @SIMPL-18164
  Scenario: Pagination keeps the sort order across pages
    Given a vocabulary "{Charlie}" exists
    And a vocabulary "{Alpha}" exists
    And a vocabulary "{Bravo}" exists
    When I send GET "/vocabularies?name={RUN}&sort=name&page=2&pageSize=2"
    Then the response status is 200
    And the list has 1 items
    And the list contains "{Charlie}"

  @SIMPL-18164
  Scenario Outline: An unsupported sort or filter value is rejected
    When I send GET "/vocabularies?<query>"
    Then the response is a problem with status 400

    Examples:
      | query                  |
      | sort=colour            |
      | status=ARCHIVED        |
      | creationDate=yesterday |
