Feature: List, filter, sort and page vocabularies through the API
  GET /vocabularies returns the vocabularies with values of their latest version (D25), paged
  (D22, D59, D78, D111), filtered (D19, D20) and sorted (D23, D58, D128). Every scenario filters
  on its own scenario id, so it never depends on an empty store (E4).

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18162_AC1 @D25 @D27 @D5 @D58 @E2 @E4 @API-36
  Scenario: The list returns the metadata of each vocabulary
    Given a vocabulary "A" exists with description "Alpha ${SID}"
    And vocabulary "A" has a new version with description "Alpha two ${SID}"
    When I send a GET request to "/vocabularies?name=${SID}"
    Then the response status is 200
    And the response content type is JSON
    And the response field "total" is 1
    And the response field "page" is 1
    And the response field "pageSize" is 20
    And the response field "totalPages" is 1
    And the response field "items[0].name" is "${A}"
    And the response field "items[0].version" is 2
    And the response field "items[0].description" is "Alpha two ${SID}"
    And the response field "items[0].status" is "ACTIVE"
    And the response field "items[0].createdBy" is "${EMAIL:admin}"
    And the response field "items[0].creationDate" is a UTC timestamp to the second
    And the response field "items[0].lastUpdateDate" is a UTC timestamp to the second
    And the response field "items[0].href" is "/vocabularies/${A}"
    And the response field "items[0].contentHref" is "${CB}/content/${A}"

  @AC_SIMPL-18162_AC1 @D22 @D111 @D110
  Scenario: Paging links always carry page and pageSize first
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies?name=${SID}"
    Then the response status is 200
    And the response field "self" is "/vocabularies?page=1&pageSize=20&name=${SID}"
    And the response field "first" is "/vocabularies?page=1&pageSize=20&name=${SID}"
    And the response field "last" is "/vocabularies?page=1&pageSize=20&name=${SID}"
    And the response field "prev" is absent
    And the response field "next" is absent

  @AC_SIMPL-18164_AC3 @AC_SIMPL-18162_AC1 @D22 @D23 @D78 @D110 @D111 @BLG-col-paging
  Scenario: Pagination is applied together with sorting and keeps the request in its links
    Given a vocabulary "Charlie" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    When I send a GET request to "/vocabularies?name=${SID}&sort=-name&pageSize=2&page=1"
    Then the response status is 200
    And the item names are "${Charlie}, ${Bravo}"
    And the response field "total" is 3
    And the response field "totalPages" is 2
    And the response field "page" is 1
    And the response field "pageSize" is 2
    And the response field "self" is "/vocabularies?page=1&pageSize=2&name=${SID}&sort=-name"
    And the response field "first" is "/vocabularies?page=1&pageSize=2&name=${SID}&sort=-name"
    And the response field "next" is "/vocabularies?page=2&pageSize=2&name=${SID}&sort=-name"
    And the response field "last" is "/vocabularies?page=2&pageSize=2&name=${SID}&sort=-name"
    And the response field "prev" is absent
    When I send a GET request to "/vocabularies?name=${SID}&sort=-name&pageSize=2&page=2"
    Then the response status is 200
    And the item names are "${Alpha}"
    And the response field "prev" is "/vocabularies?page=1&pageSize=2&name=${SID}&sort=-name"
    And the response field "next" is absent
    And the response field "last" is "/vocabularies?page=2&pageSize=2&name=${SID}&sort=-name"

  @AC_SIMPL-18164_AC3 @D22 @D59
  Scenario: A page beyond the last returns an empty page with the right totals
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send a GET request to "/vocabularies?name=${SID}&pageSize=2&page=5"
    Then the response status is 200
    And the response has no items
    And the response field "total" is 3
    And the response field "totalPages" is 2
    And the response field "page" is 5
    And the response field "self" is "/vocabularies?page=5&pageSize=2&name=${SID}"
    And the response field "first" is "/vocabularies?page=1&pageSize=2&name=${SID}"
    And the response field "prev" is "/vocabularies?page=2&pageSize=2&name=${SID}"
    And the response field "last" is "/vocabularies?page=2&pageSize=2&name=${SID}"
    And the response field "next" is absent

  @AC_SIMPL-18164_AC2 @D22 @D59 @E4
  Scenario: A filter with no match returns an empty result without an error
    When I send a GET request to "/vocabularies?name=${SID}Nothing"
    Then the response status is 200
    And the response has no items
    And the response field "total" is 0
    And the response field "totalPages" is 0
    And the response field "self" is "/vocabularies?page=1&pageSize=20&name=${SID}Nothing"
    And the response field "first" is "/vocabularies?page=1&pageSize=20&name=${SID}Nothing"
    And the response field "last" is absent
    And the response field "prev" is absent
    And the response field "next" is absent

  @AC_SIMPL-18164_AC2 @D20
  Scenario: A creation date filter on another day returns an empty result
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies?name=${SID}&creationDate=2001-01-01"
    Then the response status is 200
    And the response has no items
    And the response field "total" is 0

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given a vocabulary "HealthTerms" exists
    And a vocabulary "LegalTerms" exists
    When I send a GET request to "/vocabularies?name=${SID}health"
    Then the response status is 200
    And the item names are "${HealthTerms}"
    When I send a GET request to "/vocabularies?name=${SID}"
    Then the item names are "${HealthTerms}, ${LegalTerms}"

  @AC_SIMPL-18164_AC1 @D3 @D19
  Scenario: The status filter matches the vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And vocabulary "Alpha" has a new version
    And the status of vocabulary "Bravo" is changed to "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${SID}&status=DEPRECATED"
    Then the response status is 200
    And the item names are "${Bravo}"
    And the response field "items[0].status" is "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${SID}&status=ACTIVE"
    Then the item names are "${Alpha}"

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: A creation date filter with a date matches every value on that UTC day
    Given a vocabulary "A" exists
    And I remember the field "creationDate" of "/vocabularies/${A}" as "created"
    When I send a GET request to "/vocabularies?name=${SID}&creationDate=${R:created:date}"
    Then the response status is 200
    And the item names are "${A}"

  @AC_SIMPL-18164_AC1 @D20 @D58 @D110
  Scenario Outline: A creation date filter with a date-time matches to the second: <value>
    Given a vocabulary "A" exists
    And I remember the field "creationDate" of "/vocabularies/${A}" as "created"
    When I send a GET request to "/vocabularies?name=${SID}&creationDate=<value>"
    Then the response status is 200
    And the item names are "<names>"

    Examples:
      | value                        | names |
      | ${R:created}                 | ${A}  |
      | ${R:created:noz}             | ${A}  |
      | ${R:created:offset+01:00}    | ${A}  |
      | ${R:created:offset-05:30}    | ${A}  |
      | ${R:created:+1s}             |       |
      | ${R:created:-1s}             |       |

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: A last update filter matches the time of the last change
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the clock has moved on by more than one second
    And vocabulary "Alpha" has a new version
    And I remember the field "lastUpdateDate" of "/vocabularies/${Alpha}" as "updated"
    And I remember the field "lastUpdateDate" of "/vocabularies/${Bravo}" as "bravo"
    And the remembered "updated" and "bravo" fall on the same UTC day
    When I send a GET request to "/vocabularies?name=${SID}&lastUpdated=${R:updated}"
    Then the response status is 200
    And the item names are "${Alpha}"
    When I send a GET request to "/vocabularies?name=${SID}&lastUpdated=${R:updated:date}"
    Then the response status is 200
    And the item names are "${Alpha}, ${Bravo}"

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario Outline: Sorting by name: <sort>
    Given a vocabulary "Bravo" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Charlie" exists
    When I send a GET request to "/vocabularies?name=${SID}<sort>"
    Then the response status is 200
    And the item names are "<names>"

    Examples:
      | sort        | names                          |
      |             | ${Alpha}, ${Bravo}, ${Charlie} |
      | &sort=name  | ${Alpha}, ${Bravo}, ${Charlie} |
      | &sort=-name | ${Charlie}, ${Bravo}, ${Alpha} |

  @D58 @D23
  Scenario: The name sort compares by code point
    Given a vocabulary "Ab" exists
    And a vocabulary "AZ" exists
    When I send a GET request to "/vocabularies?name=${SID}&sort=name"
    Then the response status is 200
    And the item names are "${AZ}, ${Ab}"

  @AC_SIMPL-18164_AC1 @D23 @D128
  Scenario Outline: Sorting by vocabulary status with ties broken by name: <sort>
    Given a vocabulary "Charlie" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Alpha" exists
    And the status of vocabulary "Bravo" is changed to "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${SID}&sort=<sort>"
    Then the response status is 200
    And the item names are "<names>"

    Examples:
      | sort    | names                          |
      | status  | ${Alpha}, ${Charlie}, ${Bravo} |
      | -status | ${Bravo}, ${Alpha}, ${Charlie} |

  @AC_SIMPL-18164_AC1 @D23
  Scenario Outline: Sorting by creation date: <sort>
    Given a vocabulary "Bravo" exists
    And the clock has moved on by more than one second
    And a vocabulary "Alpha" exists
    When I send a GET request to "/vocabularies?name=${SID}&sort=<sort>"
    Then the response status is 200
    And the item names are "<names>"

    Examples:
      | sort          | names              |
      | creationDate  | ${Bravo}, ${Alpha} |
      | -creationDate | ${Alpha}, ${Bravo} |

  @AC_SIMPL-18164_AC1 @D23 @D25
  Scenario Outline: Sorting by last update date: <sort>
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the clock has moved on by more than one second
    And the status of vocabulary "Alpha" is changed to "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${SID}&sort=<sort>"
    Then the response status is 200
    And the item names are "<names>"

    Examples:
      | sort         | names              |
      | lastUpdated  | ${Bravo}, ${Alpha} |
      | -lastUpdated | ${Alpha}, ${Bravo} |

  @AC_SIMPL-18164_AC4 @AC_SIMPL-18162_AC2 @D21 @D22 @D91 @D112 @D79 @negative @NFR-04
  Scenario Outline: Invalid paging values are rejected: <query>
    When I send a GET request to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"
    And the response field "items" is absent

    Examples:
      | query               | parameter |
      | page=0              | page      |
      | page=-1             | page      |
      | page=abc            | page      |
      | page=2.0            | page      |
      | page=1e1            | page      |
      | page=%2B2           | page      |
      | page=%202           | page      |
      | page=2147483648     | page      |
      | pageSize=0          | pageSize  |
      | pageSize=101        | pageSize  |
      | pageSize=ten        | pageSize  |
      | pageSize=2147483648 | pageSize  |

  @D22 @NFR-04
  Scenario: A page size of 100 is accepted
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies?name=${SID}&pageSize=100"
    Then the response status is 200
    And the response field "pageSize" is 100
    And the item names are "${A}"

  @D91
  Scenario: The highest 32-bit page number is a valid page
    When I send a GET request to "/vocabularies?name=${SID}&page=2147483647"
    Then the response status is 200
    And the response has no items

  @AC_SIMPL-18164_AC4 @D19 @D20 @D21 @D23 @D130 @D116 @negative
  Scenario Outline: Unsupported or invalid filter and sort values are rejected: <query>
    When I send a GET request to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"
    And the response field "items" is absent

    Examples:
      | query                                  | parameter    |
      | sort=description                       | sort         |
      | sort=createdBy                         | sort         |
      | sort=+name                             | sort         |
      | status=RETIRED                         | status       |
      | status=active                          | status       |
      | name=a-b                               | name         |
      | name=                                  | name         |
      | name=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA | name |
      | creationDate=yesterday                 | creationDate |
      | creationDate=2026-02-30                | creationDate |
      | creationDate=2026-13-01                | creationDate |
      | creationDate=2026-11-20T25:00:00Z      | creationDate |
      | creationDate=2026-11-20T10:00:00%2B19:00 | creationDate |
      | lastUpdated=2026-02-30                 | lastUpdated  |
      | lastUpdated=2026-11-20T10:00:00-18:30  | lastUpdated  |

  @AC_SIMPL-18164_AC4 @D21 @negative
  Scenario Outline: An unsupported filter field is rejected with one issue: <parameter>
    When I send a GET request to "/vocabularies?<parameter>=x"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | parameter   |
      | description |
      | createdBy   |
      | version     |
      | foo         |

  @D19
  Scenario: The name filter accepts one character and 64 characters
    When I send a GET request to "/vocabularies?name=Z"
    Then the response status is 200
    When I send a GET request to "/vocabularies?name=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
    Then the response status is 200

  @D52 @D138 @negative
  Scenario Outline: A query parameter given twice is rejected with one issue: <parameter>
    When I send a GET request to "/vocabularies?<parameter>=<a>&<parameter>=<b>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | parameter | a      | b      |
      | page      | 1      | 2      |
      | name      | ${SID} | ${SID} |
      | sort      | name   | -name  |
