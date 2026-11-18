Feature: List, filter, sort and page vocabularies
  GET /vocabularies returns the vocabularies with values of their latest version (D25).
  Filters: D19 and D20. Paging: D22. Sorting: D23. Unknown parameters: D21.
  The store is shared, so every scenario filters on its own run prefix ${run}.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18162_AC1 @D22 @D25 @D27 @BLG-col-paging @API-36
  Scenario: The list is paged and every vocabulary carries its metadata
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send GET "/vocabularies?name=${run}"
    Then the response status is 200
    And the response content type is "application/json"
    And the page fields are page 1, pageSize 20, total 2 and totalPages 1
    And the page links "self, first" are present
    And every list item has the fields "href, contentHref, name, version, description, status, createdBy, creationDate, lastUpdateDate"
    And the list holds the vocabularies "Alpha, Beta" in this order

  @AC_SIMPL-18162_AC1 @D22 @D16
  Scenario: A viewer gets the same list
    Given a vocabulary "Alpha" exists
    And I am authenticated as "ga-viewer"
    When I send GET "/vocabularies?name=${run}"
    Then the response status is 200
    And the list holds the vocabularies "Alpha" in this order

  @AC_SIMPL-18162_AC2 @AC_SIMPL-18164_AC4 @D22 @D21 @negative @BLG-err-problem @D39
  Scenario Outline: Wrong pagination parameters are rejected with 400 (<query>)
    When I send GET "/vocabularies?<query>"
    Then the response is a problem with status 400
    And the response has no "items" field

    Examples:
      | query          |
      | page=0         |
      | page=-1        |
      | page=abc       |
      | page=1.5       |
      | pageSize=0     |
      | pageSize=101   |
      | pageSize=many  |

  @D22
  Scenario: A page size of 100 is accepted
    When I send GET "/vocabularies?name=${run}&pageSize=100"
    Then the response status is 200
    And the field "pageSize" is the number 100

  @AC_SIMPL-18164_AC2 @D22
  Scenario: An empty result has no last link
    When I send GET "/vocabularies?name=${run}"
    Then the response status is 200
    And the list holds no items
    And the page fields are page 1, pageSize 20, total 0 and totalPages 0
    And the page links "self, first" are present
    And the page links "last, prev, next" are absent

  @AC_SIMPL-18164_AC3 @D22 @D23 @BLG-col-paging
  Scenario: Pagination is applied together with sorting
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And a vocabulary "Gamma" exists
    When I send GET "/vocabularies?name=${run}&sort=-name&pageSize=2&page=1"
    Then the response status is 200
    And the list holds the vocabularies "Gamma, Beta" in this order
    And the page fields are page 1, pageSize 2, total 3 and totalPages 2
    And the page links "self, first, next, last" are present
    And the page links "prev" are absent
    When I send GET "/vocabularies?name=${run}&sort=-name&pageSize=2&page=2"
    Then the list holds the vocabularies "Alpha" in this order
    And the page links "self, first, prev, last" are present
    And the page links "next" are absent

  @D22
  Scenario: A page beyond the last returns an empty page with the right totals
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies?name=${run}&page=5"
    Then the response status is 200
    And the list holds no items
    And the page fields are page 5, pageSize 20, total 1 and totalPages 1

  @AC_SIMPL-18164_AC1 @D23
  Scenario: The default order is name ascending
    Given a vocabulary "Gamma" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send GET "/vocabularies?name=${run}"
    Then the list holds the vocabularies "Alpha, Beta, Gamma" in this order

  @AC_SIMPL-18164_AC1 @D23 @D25
  Scenario Outline: The list sorts by <sort>
    Given a vocabulary "Gamma" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And a new version of "Gamma" exists
    And the vocabulary "Alpha" has status "DEPRECATED"
    When I send GET "/vocabularies?name=${run}&sort=<sort>"
    Then the response status is 200
    And the list holds the vocabularies "Alpha, Beta, Gamma" in any order
    And the list is sorted by "<sort>" with ties broken by name ascending

    Examples:
      | sort          |
      | name          |
      | -name         |
      | status        |
      | -status       |
      | creationDate  |
      | -creationDate |
      | lastUpdated   |
      | -lastUpdated  |

  @AC_SIMPL-18164_AC1 @D23
  Scenario: Sorting by name descending gives the reverse order
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send GET "/vocabularies?name=${run}&sort=-name"
    Then the list holds the vocabularies "Beta, Alpha" in this order

  @AC_SIMPL-18164_AC1 @AC_SIMPL-18163_AC1 @D19 @D39
  Scenario: The name filter is a case-insensitive contains match
    Given a vocabulary "Shaclshapes" exists
    And a vocabulary "Other" exists
    When I send GET "/vocabularies?name=${runlower}shacl"
    Then the response status is 200
    And the list holds the vocabularies "Shaclshapes" in this order
    When I send GET "/vocabularies?name=CLSHAP"
    Then the response status is 200
    And the list contains the vocabulary "Shaclshapes"

  @AC_SIMPL-18164_AC4 @D19 @negative
  Scenario Outline: An invalid name filter is rejected (<case>)
    When I send GET "/vocabularies?name=<value>"
    Then the response is a problem with status 400

    Examples:
      | case              | value      |
      | empty             |            |
      | hyphen            | a-b        |
      | space             | a%20b      |
      | 65 characters     | ${x:65}    |

  @AC_SIMPL-18164_AC1 @D3 @D25
  Scenario: The status filter uses the vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And the vocabulary "Beta" has status "DEPRECATED"
    When I send GET "/vocabularies?name=${run}&status=DEPRECATED"
    Then the list holds the vocabularies "Beta" in this order
    When I send GET "/vocabularies?name=${run}&status=ACTIVE"
    Then the list holds the vocabularies "Alpha" in this order

  @D3
  Scenario: A deprecated latest version does not change the vocabulary status in the list
    Given a vocabulary "Alpha" exists
    And version 1 of vocabulary "Alpha" has status "DEPRECATED"
    When I send GET "/vocabularies?name=${run}&status=ACTIVE"
    Then the list holds the vocabularies "Alpha" in this order

  @AC_SIMPL-18164_AC1 @D20
  Scenario: A creation date filter with a date matches the whole UTC day
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies?name=${run}&creationDate=${today}"
    Then the response status is 200
    And the list holds the vocabularies "Alpha" in this order
    And every list item has a creationDate on the current UTC day

  @AC_SIMPL-18164_AC1 @D20
  Scenario: A creation date filter with a date-time matches to the second
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies/${name:Alpha}"
    And I remember the field "creationDate" as "created"
    And I send GET "/vocabularies?name=${run}&creationDate=${second:created}"
    Then the response status is 200
    And the list contains the vocabulary "Alpha"
    And every list item has a creationDate equal to the saved "created" to the second

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: A last update filter with a date matches the whole UTC day
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies?name=${run}&lastUpdated=${today}"
    Then the response status is 200
    And the list holds the vocabularies "Alpha" in this order

  @AC_SIMPL-18164_AC2 @D20
  Scenario: Filters that match nothing return an empty list, not an error
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies?name=${run}&creationDate=2000-01-01"
    Then the response status is 200
    And the list holds no items
    When I send GET "/vocabularies?name=${run}&lastUpdated=2000-01-01T00:00:00Z"
    Then the response status is 200
    And the list holds no items

  @AC_SIMPL-18164_AC4 @D20 @D21 @D23 @negative
  Scenario Outline: An unsupported or invalid filter or sort value is rejected (<query>)
    When I send GET "/vocabularies?<query>"
    Then the response is a problem with status 400
    And the response has no "items" field

    Examples:
      | query                       |
      | creationDate=2024-02-30     |
      | creationDate=2024-13-01     |
      | creationDate=17-11-2026     |
      | lastUpdated=yesterday       |
      | lastUpdated=2024-01-01T25:00:00Z |
      | status=ARCHIVED             |
      | status=active               |
      | sort=version                |
      | sort=-description           |
      | sort=lastUpdateDate         |

  @AC_SIMPL-18164_AC4 @D21 @negative @BLG-req-valid @D39
  Scenario Outline: An unknown query parameter is rejected with one issue (<param>)
    When I send GET "/vocabularies?<param>=x"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And the problem has an issue in "query" named "<param>"

    Examples:
      | param       |
      | foo         |
      | description |
      | createdBy   |

  @D5 @D25
  Scenario: The vocabulary shows its highest version number whatever that version's status
    Given a vocabulary "Alpha" exists with 2 versions
    And version 2 of vocabulary "Alpha" has status "DEPRECATED"
    When I send GET "/vocabularies?name=${run}"
    Then the field "items[0].version" is the number 2
    And the field "items[0].status" is "ACTIVE"
