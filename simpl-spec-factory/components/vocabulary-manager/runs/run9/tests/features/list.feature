Feature: List, filter, sort and page vocabularies
  GET /vocabularies. Every scenario filters on its own run id through the name filter, which is a
  case-insensitive 'contains' match (D19), so the store never has to be empty (E4).

  @AC_SIMPL-18162_AC1 @D22 @D25 @E4 @BLG-col-paging @API-36
  Scenario: The list is paginated and every vocabulary carries its metadata
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the response status is 200
    And the response media type is "application/json"
    And the page fields are page 1, pageSize 20, total 2 and totalPages 1
    And every item has the fields "name, version, description, status, createdBy, creationDate, lastUpdateDate, href, contentHref"
    And the paging link "self" is "/vocabularies" with the parameters "name=${run}&page=1&pageSize=20"
    And the paging link "first" is "/vocabularies" with the parameters "name=${run}&page=1&pageSize=20"
    And the paging link "last" is "/vocabularies" with the parameters "name=${run}&page=1&pageSize=20"
    And the paging link "prev" is absent
    And the paging link "next" is absent

  @AC_SIMPL-18162_AC1 @D3 @D25
  Scenario: The list shows the vocabulary status and the latest version number
    Given a vocabulary "Alpha" exists with 2 versions
    And the vocabulary "Alpha" was set to "DEPRECATED"
    When I send a "GET" request to "/vocabularies?name=${Alpha}"
    Then the items are, in order
      | name     | version | status     |
      | ${Alpha} | 2       | DEPRECATED |

  @AC_SIMPL-18162_AC2 @AC_SIMPL-18164_AC4 @D22 @D91 @negative @NFR-04
  Scenario Outline: A wrong pagination parameter is rejected with 400 and no metadata (<parameter>=<value>)
    Given a vocabulary "Alpha" exists
    When I send a "GET" request to "/vocabularies?name=${run}&<parameter>=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"
    And the problem has a detail
    And the response field "items" is absent or null

    Examples:
      | parameter | value       |
      | page      | 0           |
      | page      | -1          |
      | page      | abc         |
      | page      | 1.5         |
      | page      | 2147483648  |
      | pageSize  | 0           |
      | pageSize  | 101         |
      | pageSize  | abc         |
      | pageSize  | 2.5         |

  @D22 @Q2 @NFR-04
  Scenario: A page size of 100 is accepted
    When I send a "GET" request to "/vocabularies?name=${run}&pageSize=100"
    Then the response status is 200
    And the response field "pageSize" is 100

  @D91 @D22
  Scenario: The largest 32-bit page number gives an empty page
    Given a vocabulary "Alpha" exists
    When I send a "GET" request to "/vocabularies?name=${run}&page=2147483647"
    Then the response status is 200
    And the response field "items" is an empty list
    And the response field "total" is 1

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send a "GET" request to "/vocabularies?name=pha${run}"
    Then the items are named, in order
      | name     |
      | ${Alpha} |
    When I send a "GET" request to "/vocabularies?name=${upper:Beta}"
    Then the items are named, in order
      | name    |
      | ${Beta} |

  @AC_SIMPL-18164_AC4 @D19 @negative
  Scenario Outline: An invalid name filter is rejected (<case>)
    When I send a "GET" request to "/vocabularies?name=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "name"

    Examples:
      | case          | value           |
      | empty         |                 |
      | hyphen        | ab-c            |
      | 65 characters | ${repeat:a:65}  |

  @D19
  Scenario: A name filter of 1 character and of 64 characters is accepted
    When I send a "GET" request to "/vocabularies?name=a"
    Then the response status is 200
    When I send a "GET" request to "/vocabularies?name=${repeat:a:64}"
    Then the response status is 200
    And the response field "total" is 0

  @AC_SIMPL-18164_AC1 @D3
  Scenario: The status filter returns only vocabularies with that vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And the vocabulary "Beta" was set to "DEPRECATED"
    When I send a "GET" request to "/vocabularies?name=${run}&status=DEPRECATED"
    Then the items are named, in order
      | name    |
      | ${Beta} |
    When I send a "GET" request to "/vocabularies?name=${run}&status=ACTIVE"
    Then the items are named, in order
      | name     |
      | ${Alpha} |

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: The creationDate filter with a date-time matches to the second
    Given a vocabulary "Alpha" exists
    And the clock moves to the next second
    And a vocabulary "Beta" exists
    And I remember the vocabulary "Alpha" as "alpha"
    When I send a "GET" request to "/vocabularies?name=${run}&creationDate=${remembered:alpha:creationDate}"
    Then the items are named, in order
      | name     |
      | ${Alpha} |

  @AC_SIMPL-18164_AC1 @D20
  Scenario: The creationDate and lastUpdated filters with a date match the whole UTC day
    Given a vocabulary "Alpha" exists
    And the clock moves to the next second
    And a vocabulary "Beta" exists
    And the vocabularies "Alpha" and "Beta" were created on the same UTC day
    And I remember the vocabulary "Alpha" as "alpha"
    When I send a "GET" request to "/vocabularies?name=${run}&creationDate=${day:alpha:creationDate}"
    Then the items are named, in order
      | name     |
      | ${Alpha} |
      | ${Beta}  |
    When I send a "GET" request to "/vocabularies?name=${run}&lastUpdated=${day:alpha:lastUpdateDate}"
    Then the items are named, in order
      | name     |
      | ${Alpha} |
      | ${Beta}  |

  @AC_SIMPL-18164_AC2 @D20
  Scenario Outline: A filter that matches nothing returns an empty result set without an error (<filter>)
    Given a vocabulary "Alpha" exists
    When I send a "GET" request to "/vocabularies?name=${run}&<filter>"
    Then the response status is 200
    And the response field "items" is an empty list
    And the page fields are page 1, pageSize 20, total 0 and totalPages 0
    And the paging link "self" is "/vocabularies" with the parameters "name=${run}&<filter>&page=1&pageSize=20"
    And the paging link "first" is "/vocabularies" with the parameters "name=${run}&<filter>&page=1&pageSize=20"
    And the paging link "last" is absent
    And the paging link "prev" is absent
    And the paging link "next" is absent

    Examples:
      | filter                            |
      | status=DEPRECATED                 |
      | creationDate=2001-01-01           |
      | lastUpdated=2001-01-01T10:00:00Z  |

  @AC_SIMPL-18164_AC4 @D20 @D21 @negative
  Scenario Outline: An invalid filter value is rejected (<parameter>=<value>)
    When I send a "GET" request to "/vocabularies?name=${run}&<parameter>=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"
    And the response field "items" is absent or null

    Examples:
      | parameter    | value          |
      | status       | RETIRED        |
      | status       | active         |
      | creationDate | 2024-02-30     |
      | creationDate | yesterday      |
      | lastUpdated  | 2024-13-01     |
      | sort         | description    |
      | sort         | +name          |

  @AC_SIMPL-18164_AC4 @D21 @negative @BLG-req-valid
  Scenario: An unsupported filter or sort field is rejected as an unknown query parameter
    When I send a "GET" request to "/vocabularies?name=${run}&description=x"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "description"

  @D52 @negative
  Scenario: A query parameter given twice is rejected
    When I send a "GET" request to "/vocabularies?name=${run}&page=1&page=2"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "page"

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario: The default order is name ascending by code point
    Given a vocabulary "Ab" exists
    And a vocabulary "AZ" exists
    And a vocabulary "Ba" exists
    When I send a "GET" request to "/vocabularies?name=${run}"
    Then the items are named, in order
      | name  |
      | ${AZ} |
      | ${Ab} |
      | ${Ba} |
    When I send a "GET" request to "/vocabularies?name=${run}&sort=-name"
    Then the items are named, in order
      | name  |
      | ${Ba} |
      | ${Ab} |
      | ${AZ} |

  @AC_SIMPL-18164_AC1 @D23
  Scenario: Sorting by status breaks ties by name ascending
    Given a vocabulary "Charlie" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the vocabulary "Bravo" was set to "DEPRECATED"
    When I send a "GET" request to "/vocabularies?name=${run}&sort=status"
    Then the items are named, in order
      | name       |
      | ${Alpha}   |
      | ${Charlie} |
      | ${Bravo}   |
    When I send a "GET" request to "/vocabularies?name=${run}&sort=-status"
    Then the items are named, in order
      | name       |
      | ${Bravo}   |
      | ${Alpha}   |
      | ${Charlie} |

  @AC_SIMPL-18164_AC1 @AC_SIMPL-18164_AC3 @D23 @D58
  Scenario: Sorting by creation date and by last update date, together with paging
    Given a vocabulary "Charlie" exists
    And the clock moves to the next second
    And a vocabulary "Bravo" exists
    And the clock moves to the next second
    And a vocabulary "Alpha" exists
    And the clock moves to the next second
    And the vocabulary "Charlie" was set to "DEPRECATED"
    When I send a "GET" request to "/vocabularies?name=${run}&sort=creationDate"
    Then the items are named, in order
      | name       |
      | ${Charlie} |
      | ${Bravo}   |
      | ${Alpha}   |
    When I send a "GET" request to "/vocabularies?name=${run}&sort=-creationDate"
    Then the items are named, in order
      | name       |
      | ${Alpha}   |
      | ${Bravo}   |
      | ${Charlie} |
    When I send a "GET" request to "/vocabularies?name=${run}&sort=lastUpdated"
    Then the items are named, in order
      | name       |
      | ${Bravo}   |
      | ${Alpha}   |
      | ${Charlie} |
    When I send a "GET" request to "/vocabularies?name=${run}&sort=-lastUpdated&pageSize=2&page=2"
    Then the items are named, in order
      | name     |
      | ${Bravo} |
    And the page fields are page 2, pageSize 2, total 3 and totalPages 2

  @AC_SIMPL-18164_AC3 @D22 @D23 @D78 @D59
  Scenario: Pagination with sorting returns the requested page and links that keep filter and sort
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send a "GET" request to "/vocabularies?name=${run}&sort=-name&pageSize=1&page=2"
    Then the response status is 200
    And the items are named, in order
      | name     |
      | ${Bravo} |
    And the page fields are page 2, pageSize 1, total 3 and totalPages 3
    And the paging link "self" is "/vocabularies" with the parameters "name=${run}&sort=-name&pageSize=1&page=2"
    And the paging link "first" is "/vocabularies" with the parameters "name=${run}&sort=-name&pageSize=1&page=1"
    And the paging link "prev" is "/vocabularies" with the parameters "name=${run}&sort=-name&pageSize=1&page=1"
    And the paging link "next" is "/vocabularies" with the parameters "name=${run}&sort=-name&pageSize=1&page=3"
    And the paging link "last" is "/vocabularies" with the parameters "name=${run}&sort=-name&pageSize=1&page=3"

  @D22 @D59
  Scenario: A page beyond the last returns an empty page with the right totals and prev pointing to the last page
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    When I send a "GET" request to "/vocabularies?name=${run}&pageSize=1&page=5"
    Then the response status is 200
    And the response field "items" is an empty list
    And the page fields are page 5, pageSize 1, total 2 and totalPages 2
    And the paging link "prev" is "/vocabularies" with the parameters "name=${run}&pageSize=1&page=2"
    And the paging link "last" is "/vocabularies" with the parameters "name=${run}&pageSize=1&page=2"
    And the paging link "next" is absent
