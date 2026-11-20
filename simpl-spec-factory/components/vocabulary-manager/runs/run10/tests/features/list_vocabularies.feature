@list
Feature: List vocabularies with filters, sorting and paging
  GET /vocabularies returns a paged list of vocabularies (D22, D25). Every scenario filters on the
  names it created itself, because the store is shared and never reset (E4).

  Background:
    Given I use the token of "ga-admin"

  @AC_SIMPL-18162_AC1 @D22 @D25 @D27 @D28 @D58 @BLG-col-paging
  Scenario: The list is paged and each vocabulary carries its metadata
    Given a vocabulary "A" exists with 2 versions
    When I send "GET" to "/vocabularies?name=${prefix}"
    Then the status is 200
    And the media type is "application/json"
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the response field "total" equals 1
    And the response field "totalPages" equals 1
    And the response has 1 item
    And the response field "items[0].name" is "${A}"
    And the response field "items[0].version" equals 2
    And the response field "items[0].description" is "Description of A version 2"
    And the response field "items[0].status" is "ACTIVE"
    And the response field "items[0].createdBy" is "${email:ga-admin}"
    And the response field "items[0].creationDate" is a UTC timestamp
    And the response field "items[0].lastUpdateDate" is a UTC timestamp
    And the response field "items[0].href" is "/vocabularies/${A}"
    And the response field "items[0].contentHref" is "${base}/content/${A}"

  @AC_SIMPL-18162_AC1 @D16
  Scenario: A viewer gets the paged list
    Given a vocabulary "A" exists
    And I use the token of "ga-viewer"
    When I send "GET" to "/vocabularies?name=${A}"
    Then the status is 200
    And the response has 1 item

  @AC_SIMPL-18162_AC2 @AC_SIMPL-18164_AC4 @D22 @D91 @D112 @Q2 @NFR-04 @negative
  Scenario Outline: A wrong paging parameter <query> gives 400 and no list
    When I send "GET" to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"
    And the response field "items" is absent

    Examples:
      | query               | param    |
      | page=0              | page     |
      | page=-1             | page     |
      | page=abc            | page     |
      | page=2.0            | page     |
      | page=1e1            | page     |
      | page=%2B2           | page     |
      | page=%202           | page     |
      | page=               | page     |
      | page=2147483648     | page     |
      | pageSize=0          | pageSize |
      | pageSize=101        | pageSize |
      | pageSize=abc        | pageSize |
      | pageSize=-5         | pageSize |
      | pageSize=20.5       | pageSize |

  @D22 @Q2 @NFR-04
  Scenario: pageSize 100 is at the limit and is accepted
    When I send "GET" to "/vocabularies?pageSize=100&name=${prefix}"
    Then the status is 200
    And the response field "pageSize" equals 100

  @D22 @D91
  Scenario: The largest page number is accepted and gives an empty page
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies?name=${A}&page=2147483647"
    Then the status is 200
    And the response has 0 items
    And the response field "total" equals 1
    And the response field "totalPages" equals 1

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive 'contains' match
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send "GET" to "/vocabularies?name=${lower:Alpha}"
    Then the status is 200
    And the item names are "Alpha"
    When I send "GET" to "/vocabularies?name=${prefix}"
    Then the item names are "Alpha, Beta"
    When I send "GET" to "/vocabularies?name=${upper:Beta}"
    Then the item names are "Beta"

  @AC_SIMPL-18164_AC2 @D19 @D22
  Scenario: A filter that matches nothing gives an empty page without error
    When I send "GET" to "/vocabularies?name=${prefix}Nothing"
    Then the status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0
    And the response field "page" equals 1
    And the link "self" is present
    And the link "first" is present
    And the link "last" is absent
    And the link "prev" is absent
    And the link "next" is absent

  @AC_SIMPL-18164_AC4 @D19 @negative
  Scenario Outline: An invalid name filter <value> gives 400
    When I send "GET" to "/vocabularies?name=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "name"
    And the response field "items" is absent

    Examples:
      | value      |
      | a-b        |
      | a_b        |
      | %20        |
      |            |
      | ${long:65} |

  @AC_SIMPL-18164_AC1 @D3 @D19
  Scenario: The status filter returns only vocabularies with that vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists with 2 versions
    And vocabulary "Beta" has the status "DEPRECATED"
    When I send "GET" to "/vocabularies?name=${prefix}&status=DEPRECATED"
    Then the status is 200
    And the item names are "Beta"
    And the response field "items[0].status" is "DEPRECATED"
    When I send "GET" to "/vocabularies?name=${prefix}&status=ACTIVE"
    Then the item names are "Alpha"

  @D3
  Scenario: The list shows the vocabulary status, not the version status
    Given a vocabulary "A" exists with 2 versions
    And version 2 of vocabulary "A" has the status "DEPRECATED"
    When I send "GET" to "/vocabularies?name=${A}"
    Then the response field "items[0].status" is "ACTIVE"
    And the response field "items[0].version" equals 2

  @AC_SIMPL-18164_AC4 @negative
  Scenario Outline: An invalid status filter <value> gives 400
    When I send "GET" to "/vocabularies?status=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "status"

    Examples:
      | value    |
      | OBSOLETE |
      | active   |

  @AC_SIMPL-18164_AC1 @D20 @D58 @D110
  Scenario: The creation date filter matches a date, an exact date-time and a date-time without offset
    Given a vocabulary "A" exists
    And I remember the field "creationDate" of "/vocabularies/${A}" as "created"
    Then the vocabulary list filtered by "creationDate" with the date of the remembered "created" contains "A"
    And the vocabulary list filtered by "creationDate" with the date-time of the remembered "created" contains "A"
    And the vocabulary list filtered by "creationDate" with the date-time without offset of the remembered "created" contains "A"
    When I send "GET" to "/vocabularies?name=${A}&creationDate=2000-01-01"
    Then the status is 200
    And the response has 0 items

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: The last updated filter matches the last update date
    Given a vocabulary "A" exists
    And vocabulary "A" has the status "DEPRECATED"
    And I remember the field "lastUpdateDate" of "/vocabularies/${A}" as "updated"
    Then the vocabulary list filtered by "lastUpdated" with the date of the remembered "updated" contains "A"
    And the vocabulary list filtered by "lastUpdated" with the date-time of the remembered "updated" contains "A"
    When I send "GET" to "/vocabularies?name=${A}&lastUpdated=2000-01-01T00:00:00Z"
    Then the status is 200
    And the response has 0 items

  @AC_SIMPL-18164_AC4 @D20 @negative
  Scenario Outline: An invalid date filter <param>=<value> gives 400
    When I send "GET" to "/vocabularies?<param>=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"

    Examples:
      | param        | value      |
      | creationDate | 2024-02-30 |
      | creationDate | 2024-13-01 |
      | creationDate | yesterday  |
      | creationDate | 01-03-2024 |
      | lastUpdated  | 2024-02-30 |
      | lastUpdated  | 2024-01-01Tnonsense |

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario Outline: Sorting by <sort> orders the results, ties broken by name ascending
    # Creations and updates are more than 1 s apart, so every date differs (D58 stores seconds).
    Given a vocabulary "Charlie" exists
    And I wait 1.1 seconds
    And a vocabulary "Alpha" exists
    And I wait 1.1 seconds
    And a vocabulary "Bravo" exists
    And I wait 1.1 seconds
    And vocabulary "Bravo" has the status "DEPRECATED"
    And I wait 1.1 seconds
    And vocabulary "Alpha" has a new version with the file "valid"
    When I send "GET" to "/vocabularies?name=${prefix}&sort=<sort>"
    Then the status is 200
    And the item names are "<order>"
    And the vocabulary items are sorted by "<sort>"

    Examples:
      | sort          | order                  |
      | name          | Alpha, Bravo, Charlie  |
      | -name         | Charlie, Bravo, Alpha  |
      | status        | Alpha, Charlie, Bravo  |
      | -status       | Bravo, Alpha, Charlie  |
      | creationDate  | Charlie, Alpha, Bravo  |
      | -creationDate | Bravo, Alpha, Charlie  |
      | lastUpdated   | Charlie, Bravo, Alpha  |
      | -lastUpdated  | Alpha, Bravo, Charlie  |

  @D23 @D58
  Scenario: The default order is name ascending by code point
    Given a vocabulary "Ab" exists
    And a vocabulary "AZ" exists
    And a vocabulary "B" exists
    When I send "GET" to "/vocabularies?name=${prefix}"
    Then the item names are "AZ, Ab, B"
    When I send "GET" to "/vocabularies?name=${prefix}&sort=-name"
    Then the item names are "B, Ab, AZ"

  @D23
  Scenario: Name sort on explicit request matches the expected order
    Given a vocabulary "Bravo" exists
    And a vocabulary "Alpha" exists
    When I send "GET" to "/vocabularies?name=${prefix}&sort=name"
    Then the item names are "Alpha, Bravo"

  @AC_SIMPL-18164_AC4 @D21 @negative
  Scenario Outline: An unsupported sort value <value> gives 400
    When I send "GET" to "/vocabularies?sort=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "sort"
    And the response field "items" is absent

    Examples:
      | value        |
      | version      |
      | -description |
      | Name         |
      | name,status  |

  @AC_SIMPL-18164_AC4 @D21 @D52 @negative
  Scenario Outline: An unknown query parameter gives 400 with one issue: <query>
    When I send "GET" to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query                | param       |
      | foo=bar              | foo         |
      | description=x        | description |
      | version=1            | version     |

  @AC_SIMPL-18164_AC4 @D52 @negative
  Scenario Outline: A repeated query parameter gives 400: <query>
    When I send "GET" to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

    Examples:
      | query                |
      | page=1&page=2        |
      | sort=name&sort=-name |

  @AC_SIMPL-18164_AC3 @D22 @D23 @D78 @D111 @D110
  Scenario: Paging together with sorting keeps the order and the links keep the request
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send "GET" to "/vocabularies?name=${prefix}&sort=-name&pageSize=2"
    Then the status is 200
    And the item names are "Charlie, Bravo"
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the response field "page" equals 1
    And the response field "pageSize" equals 2
    And the link "self" has the query parameter "page" with value "1"
    And the link "first" has the query parameter "page" with value "1"
    And the link "next" has the query parameter "page" with value "2"
    And the link "last" has the query parameter "page" with value "2"
    And the link "prev" is absent
    And the link "next" has the query parameter "sort" with value "-name"
    And the link "next" has the query parameter "name" with value "${prefix}"
    And the link "next" has the query parameter "pageSize" with value "2"
    And the link "next" has the path "/vocabularies"
    And the link "next" starts with the parameters page and pageSize
    When I send "GET" to "/vocabularies?name=${prefix}&sort=-name&pageSize=2&page=2"
    Then the item names are "Alpha"
    And the link "prev" has the query parameter "page" with value "1"
    And the link "next" is absent
    And the link "last" has the query parameter "page" with value "2"
    And the pages of "/vocabularies?name=${prefix}&sort=-name" with page size 1 together give the full sorted list

  @D22 @D59
  Scenario: A page beyond the last page is empty and its prev link points to the last page
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send "GET" to "/vocabularies?name=${prefix}&pageSize=2&page=5"
    Then the status is 200
    And the response has 0 items
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the link "prev" has the query parameter "page" with value "2"
    And the link "next" is absent
    And the link "last" has the query parameter "page" with value "2"

  @D22 @D111
  Scenario: Defaults page 1 and pageSize 20 are used and appear in every link
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies?name=${A}"
    Then the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the link "self" has the query parameter "page" with value "1"
    And the link "self" has the query parameter "pageSize" with value "20"
    And the link "first" has the query parameter "pageSize" with value "20"
    And the link "last" has the query parameter "page" with value "1"
    And the link "self" has the query parameter "name" with value "${A}"

  @D63 @API-37 @negative
  Scenario: An Accept header the list cannot serve gives 406
    When I send "GET" to "/vocabularies" with the header "Accept" set to "text/turtle"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"

  @D63
  Scenario Outline: The list accepts the Accept header <accept>
    When I send "GET" to "/vocabularies?pageSize=1" with the header "Accept" set to "<accept>"
    Then the status is 200

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | */*                      |
