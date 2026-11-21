Feature: List, filter, sort and page vocabularies
  GET /vocabularies (SIMPL-18162, SIMPL-18164, D19 to D23, D58, D59, D78, D110, D111, D112).
  Every scenario filters on its own run id, so other data in the store does not matter (E4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18162_AC1 @D25 @D22 @BLG-col-paging @API-36
  Scenario: The list is paged and each vocabulary carries its metadata
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 200
    And the response media type is "application/json"
    And the response has the fields "page, pageSize, total, totalPages, self, first, items"
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the response field "total" equals 1
    And the response field "totalPages" equals 1
    And every item has the fields "href, contentHref, name, version, description, status, createdBy, creationDate, lastUpdateDate"
    And the response field "items.0.href" equals "/vocabularies/{{Health}}"
    And the response field "items.0.version" equals 1
    And the response field "items.0.status" equals "ACTIVE"

  @AC_SIMPL-18162_AC2 @D22 @D91 @D112 @negative @NFR-04 @Q2
  Scenario Outline: A wrong paging value is rejected (<query>)
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
      | page=2147483648     | page     |
      | pageSize=0          | pageSize |
      | pageSize=101        | pageSize |
      | pageSize=ten        | pageSize |

  @D22 @NFR-04 @Q2
  Scenario: A page size of 100 is accepted
    When I send "GET" to "/vocabularies?name={{run}}&pageSize=100"
    Then the status is 200
    And the response field "pageSize" equals 100

  @D91 @D22
  Scenario: The largest page number is accepted and gives an empty page
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies?name={{run}}&page=2147483647"
    Then the status is 200
    And the response field "items" has 0 items
    And the response field "total" equals 1

  @AC_SIMPL-18164_AC4 @D21 @negative @BLG-req-valid
  Scenario: An unknown query parameter is rejected with one issue
    When I send "GET" to "/vocabularies?colour=blue"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "colour"

  @D52 @D138 @negative
  Scenario: A query parameter given twice is rejected with one issue
    When I send "GET" to "/vocabularies?page=1&page=2"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "page"

  @AC_SIMPL-18164_AC4 @D19 @D20 @D23 @D116 @D130 @negative
  Scenario Outline: An unsupported sort field or an invalid filter value is rejected (<query>)
    When I send "GET" to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"
    And the response field "items" is absent

    Examples:
      | query                                  | param        |
      | sort=description                       | sort         |
      | sort=version                           | sort         |
      | status=RETIRED                         | status       |
      | status=active                          | status       |
      | name=a-b                               | name         |
      | name=                                  | name         |
      | name=Aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa | name |
      | creationDate=yesterday                 | creationDate |
      | creationDate=2026-02-30                | creationDate |
      | creationDate=2026-13-01                | creationDate |
      | lastUpdated=2026-11-20T25:00:00Z       | lastUpdated  |
      | creationDate=2026-11-20T10:00:00%2B19:00 | creationDate |

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given these vocabularies exist
      | name           |
      | {{Alpha}}      |
      | {{Beta}}       |
    When I send "GET" to "/vocabularies?name=lpha{{run}}"
    Then the item names are "{{Alpha}}"
    When I send "GET" to "/vocabularies?name=ALPHA{{run}}"
    Then the item names are "{{Alpha}}"
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the item names are "{{Alpha}}, {{Beta}}"

  @D19 @NFR-04 @Q2
  Scenario: A name filter of 64 characters is accepted
    When I send "GET" to "/vocabularies?name={{Long64}}"
    Then the status is 200

  @AC_SIMPL-18164_AC1 @D3 @D19
  Scenario: The status filter uses the vocabulary status
    Given these vocabularies exist
      | name      | status     |
      | {{Alpha}} | ACTIVE     |
      | {{Beta}}  | DEPRECATED |
    When I send "GET" to "/vocabularies?name={{run}}&status=DEPRECATED"
    Then the item names are "{{Beta}}"
    And every item has "status" equal to "DEPRECATED"
    When I send "GET" to "/vocabularies?name={{run}}&status=ACTIVE"
    Then the item names are "{{Alpha}}"

  @D3
  Scenario: The list shows the vocabulary status, not the version status
    Given a vocabulary "{{Alpha}}" exists
    And version 1 of vocabulary "{{Alpha}}" has status "DEPRECATED"
    When I send "GET" to "/vocabularies?name={{run}}&status=ACTIVE"
    Then the item names are "{{Alpha}}"

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: A date filter matches every value on that UTC day
    Given a vocabulary "{{Alpha}}" exists
    When I send "GET" to "/vocabularies/{{Alpha}}"
    And I remember the response field "creationDate" as "created"
    And I remember the response field "lastUpdateDate" as "updated"
    And I send "GET" to "/vocabularies?name={{run}}&creationDate={{date:created}}"
    Then the item names are "{{Alpha}}"
    When I send "GET" to "/vocabularies?name={{run}}&lastUpdated={{date:updated}}"
    Then the item names are "{{Alpha}}"

  @AC_SIMPL-18164_AC2 @D20
  Scenario: A filter that matches nothing returns an empty result, not an error
    Given a vocabulary "{{Alpha}}" exists
    When I send "GET" to "/vocabularies/{{Alpha}}"
    And I remember the response field "creationDate" as "created"
    And I send "GET" to "/vocabularies?name={{run}}&creationDate={{nextdate:created}}"
    Then the status is 200
    And the response field "items" has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @AC_SIMPL-18164_AC1 @D20 @D58 @D110
  Scenario: A date-time filter matches values equal to the second
    Given a vocabulary "{{Alpha}}" exists
    When I send "GET" to "/vocabularies/{{Alpha}}"
    And I remember the response field "creationDate" as "created"
    And I send "GET" to "/vocabularies?name={{run}}&creationDate={{created}}"
    Then the item names are "{{Alpha}}"
    When I send "GET" to "/vocabularies?name={{run}}&lastUpdated=1999-01-01T00:00:00Z"
    Then the response field "total" equals 0

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario: Sort by name compares code points, ascending and descending
    Given these vocabularies exist
      | name       |
      | {{Ab}}     |
      | {{AZ}}     |
      | {{Bc}}     |
    When I send "GET" to "/vocabularies?name={{run}}&sort=name"
    Then the item names are "{{AZ}}, {{Ab}}, {{Bc}}"
    When I send "GET" to "/vocabularies?name={{run}}&sort=-name"
    Then the item names are "{{Bc}}, {{Ab}}, {{AZ}}"

  @D23
  Scenario: The default order is name ascending
    Given these vocabularies exist
      | name       |
      | {{Cc}}     |
      | {{Aa}}     |
      | {{Bb}}     |
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the item names are "{{Aa}}, {{Bb}}, {{Cc}}"

  @AC_SIMPL-18164_AC1 @D23 @D128
  Scenario: Sort by status puts ACTIVE first and breaks ties by name ascending
    Given these vocabularies exist
      | name   | status     |
      | {{Aa}} | DEPRECATED |
      | {{Bb}} | ACTIVE     |
      | {{Cc}} | ACTIVE     |
      | {{Dd}} | DEPRECATED |
    When I send "GET" to "/vocabularies?name={{run}}&sort=status"
    Then the item names are "{{Bb}}, {{Cc}}, {{Aa}}, {{Dd}}"
    When I send "GET" to "/vocabularies?name={{run}}&sort=-status"
    Then the item names are "{{Aa}}, {{Dd}}, {{Bb}}, {{Cc}}"

  @AC_SIMPL-18164_AC1 @D23
  Scenario: Sort by creation date
    Given these vocabularies are created one second apart
      | name   |
      | {{Cc}} |
      | {{Aa}} |
      | {{Bb}} |
    When I send "GET" to "/vocabularies?name={{run}}&sort=creationDate"
    Then the item names are "{{Cc}}, {{Aa}}, {{Bb}}"
    When I send "GET" to "/vocabularies?name={{run}}&sort=-creationDate"
    Then the item names are "{{Bb}}, {{Aa}}, {{Cc}}"

  @AC_SIMPL-18164_AC1 @D23 @D25
  Scenario: Sort by last update date follows the latest change
    Given these vocabularies are created one second apart
      | name   |
      | {{Aa}} |
      | {{Bb}} |
    And vocabulary "{{Aa}}" has 1 more versions created one second apart
    When I send "GET" to "/vocabularies?name={{run}}&sort=-lastUpdated"
    Then the item names are "{{Aa}}, {{Bb}}"
    When I send "GET" to "/vocabularies?name={{run}}&sort=lastUpdated"
    Then the item names are "{{Bb}}, {{Aa}}"

  @AC_SIMPL-18164_AC3 @D22 @D78 @D110 @D111 @BLG-col-paging
  Scenario: Pages keep the sort order and the links keep the request
    Given these vocabularies exist
      | name   |
      | {{Aa}} |
      | {{Bb}} |
      | {{Cc}} |
      | {{Dd}} |
      | {{Ee}} |
    When I send "GET" to "/vocabularies?name={{run}}&sort=-name&pageSize=2&page=2"
    Then the status is 200
    And the item names are "{{Cc}}, {{Bb}}"
    And the response field "page" equals 2
    And the response field "pageSize" equals 2
    And the response field "total" equals 5
    And the response field "totalPages" equals 3
    And the response field "self" equals "/vocabularies?page=2&pageSize=2&name={{run}}&sort=-name"
    And the response field "first" equals "/vocabularies?page=1&pageSize=2&name={{run}}&sort=-name"
    And the response field "prev" equals "/vocabularies?page=1&pageSize=2&name={{run}}&sort=-name"
    And the response field "next" equals "/vocabularies?page=3&pageSize=2&name={{run}}&sort=-name"
    And the response field "last" equals "/vocabularies?page=3&pageSize=2&name={{run}}&sort=-name"
    When I send "GET" to "/vocabularies?name={{run}}&sort=-name&pageSize=2&page=3"
    Then the item names are "{{Aa}}"
    And the response field "next" is absent

  @D111 @D22
  Scenario: Links carry page and pageSize also when the request did not send them
    Given a vocabulary "{{Aa}}" exists
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the response field "self" equals "/vocabularies?page=1&pageSize=20&name={{run}}"
    And the response field "first" equals "/vocabularies?page=1&pageSize=20&name={{run}}"
    And the response field "last" equals "/vocabularies?page=1&pageSize=20&name={{run}}"
    And the response field "prev" is absent
    And the response field "next" is absent

  @D22 @D59
  Scenario: A page beyond the last page is empty and its prev link points to the last page
    Given these vocabularies exist
      | name   |
      | {{Aa}} |
      | {{Bb}} |
      | {{Cc}} |
    When I send "GET" to "/vocabularies?name={{run}}&pageSize=2&page=5"
    Then the status is 200
    And the response field "items" has 0 items
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the response field "prev" equals "/vocabularies?page=2&pageSize=2&name={{run}}"
    And the response field "last" equals "/vocabularies?page=2&pageSize=2&name={{run}}"
    And the response field "next" is absent

  @AC_SIMPL-18164_AC2 @D22 @D59
  Scenario: An empty result has self and first links only
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 200
    And the response field "items" has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0
    And the response field "self" is present
    And the response field "first" is present
    And the response field "last" is absent
    And the response field "prev" is absent
    And the response field "next" is absent

  @D63 @negative @API-37
  Scenario: An Accept header the list cannot serve gives 406
    Given the request header "Accept" is "application/xml"
    When I send "GET" to "/vocabularies"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D162 @negative
  Scenario: The Accept check comes before the query check on reads
    Given the request header "Accept" is "text/html"
    When I send "GET" to "/vocabularies?pageSize=0"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D63 @D127 @D151
  Scenario Outline: Accept <accept> is served with application/json
    Given the request header "Accept" is "<accept>"
    When I send "GET" to "/vocabularies?name={{run}}"
    Then the status is 200
    And the response media type is "application/json"

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | application/*            |
      | */*                      |
