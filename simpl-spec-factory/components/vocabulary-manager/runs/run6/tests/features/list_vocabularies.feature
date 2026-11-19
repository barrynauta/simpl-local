Feature: List, filter, sort and page vocabularies via the API
  GET /vocabularies returns a paged list of vocabularies with values of their latest version
  (D25). The store is shared, so every scenario filters on the names it created (E4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18162_AC1 @D22 @D25 @D5 @BLG-col-paging @API-36
  Scenario: The list is paged and each vocabulary carries its metadata
    Given the vocabularies "Alpha, Beta" exist
    When I list the vocabularies with "name={{run}}"
    Then the response status is 200
    And the response media type is "application/json"
    And the response has the fields "page, pageSize, total, totalPages, self, first, items"
    And the response field "page" is 1
    And the response field "pageSize" is 20
    And the response field "total" is 2
    And the response field "totalPages" is 1
    And every item has the fields "href, contentHref, name, version, description, status, createdBy, creationDate, lastUpdateDate"
    And the item names are "Alpha, Beta"
    And the response field "items.0.href" is "/vocabularies/{{name:Alpha}}"
    And the response field "items.0.contentHref" is "{{base}}/content/{{name:Alpha}}"
    And the response field "items.0.version" is 1
    And the response field "items.0.status" is "ACTIVE"

  @AC_SIMPL-18162_AC1 @D5 @D25 @D28
  Scenario: The list shows the latest version number and its description
    Given the vocabulary "Alpha" exists with 3 versions
    When I list the vocabularies with "name={{name:Alpha}}"
    Then the response status is 200
    And the response field "items.0.version" is 3
    And the response field "items.0.description" matches "Description of Alpha variant \d+"

  @AC_SIMPL-18162_AC2 @AC_SIMPL-18164_AC4 @D22 @D21 @NFR-04 @negative
  Scenario Outline: A wrong paging parameter <query> is rejected with 400
    When I list the vocabularies with "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has a detail
    And the problem has an issue in "query" named "<parameter>"
    And the response holds no metadata

    Examples:
      | query        | parameter |
      | page=0       | page      |
      | page=-1      | page      |
      | page=abc     | page      |
      | page=1.5     | page      |
      | pageSize=0   | pageSize  |
      | pageSize=101 | pageSize  |
      | pageSize=x   | pageSize  |

  @D22 @NFR-04
  Scenario: A page size of 100 is accepted
    Given the vocabulary "Alpha" exists
    When I list the vocabularies with "name={{run}}&pageSize=100"
    Then the response status is 200
    And the response field "pageSize" is 100

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given the vocabularies "Apple, Banana, Pineapple" exist
    When I list the vocabularies with "name=apple{{runlower}}"
    Then the response status is 200
    And the item names are "Apple, Pineapple"
    When I list the vocabularies with "name=APPLE{{runupper}}"
    Then the item names are "Apple, Pineapple"

  @AC_SIMPL-18164_AC4 @D19 @negative
  Scenario Outline: An invalid name filter <value> is rejected
    When I list the vocabularies with "name=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "name"

    Examples:
      | value                                                              |
      |                                                                    |
      | has-hyphen                                                         |
      | has%20space                                                        |
      | A1234567890123456789012345678901234567890123456789012345678901234   |

  @D19 @NFR-04
  Scenario: A name filter of 64 characters is accepted
    When I list the vocabularies with "name=A123456789012345678901234567890123456789012345678901234567890123"
    Then the response status is 200
    And the response has 0 items

  @D19
  Scenario: A name filter of one character is accepted
    When I list the vocabularies with "name=q"
    Then the response status is 200

  @AC_SIMPL-18164_AC1 @D3
  Scenario: The status filter returns only vocabularies with that status
    Given the vocabularies "Kept, Dropped, Gone" exist
    And the vocabulary "Dropped" is deprecated
    And the vocabulary "Gone" is deprecated
    When I list the vocabularies with "name={{run}}&status=DEPRECATED&sort=name"
    Then the response status is 200
    And every item has the status "DEPRECATED"
    And the item names are "Dropped, Gone"

  @AC_SIMPL-18164_AC4 @D21 @negative
  Scenario Outline: An invalid value <query> of a known filter is rejected
    When I list the vocabularies with "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query                   | parameter    |
      | status=RETIRED          | status       |
      | status=active           | status       |
      | creationDate=yesterday  | creationDate |
      | creationDate=2026-02-30 | creationDate |
      | lastUpdated=2026-13-01  | lastUpdated  |
      | lastUpdated=01-01-2026  | lastUpdated  |

  @AC_SIMPL-18164_AC1 @D20
  Scenario: A creation date filter matches every vocabulary created on that UTC day
    Given the vocabulary "Alpha" exists
    And I remember the field "creationDate" of "/vocabularies/{{name:Alpha}}" as "created"
    When I list the vocabularies with "name={{run}}&creationDate={{datepart:created}}"
    Then the response status is 200
    And the item names are "Alpha"
    When I list the vocabularies with "name={{run}}&creationDate=2001-01-01"
    Then the response status is 200
    And the response has 0 items

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: A creation date-time filter matches the value to the second
    Given the vocabulary "Alpha" exists
    And I remember the field "creationDate" of "/vocabularies/{{name:Alpha}}" as "created"
    When I list the vocabularies with "name={{run}}&creationDate={{var:created}}"
    Then the response status is 200
    And the item names are "Alpha"
    When I list the vocabularies with "name={{run}}&creationDate={{plus1s:created}}"
    Then the response status is 200
    And the response has 0 items
    When I list the vocabularies with "name={{run}}&creationDate=2001-01-01T00:00:00Z"
    Then the response has 0 items

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: A last update filter matches the last update date of the vocabulary
    Given the vocabulary "Alpha" exists
    And the vocabulary "Alpha" has a new version
    And I remember the field "lastUpdateDate" of "/vocabularies/{{name:Alpha}}" as "updated"
    When I list the vocabularies with "name={{run}}&lastUpdated={{datepart:updated}}"
    Then the item names are "Alpha"
    When I list the vocabularies with "name={{run}}&lastUpdated={{var:updated}}"
    Then the item names are "Alpha"
    When I list the vocabularies with "name={{run}}&lastUpdated=2001-01-01"
    Then the response has 0 items

  @AC_SIMPL-18164_AC2 @D22
  Scenario: A filter without matches returns an empty page without an error
    When I list the vocabularies with "name={{run}}"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" is 0
    And the response field "totalPages" is 0
    And the response has the links "self, first" and not "prev, next, last"

  @AC_SIMPL-18164_AC1 @D23
  Scenario: The default order is name ascending
    Given the vocabularies "Cherry, Apple, Banana" exist
    When I list the vocabularies with "name={{run}}"
    Then the item names are "Apple, Banana, Cherry"

  @AC_SIMPL-18164_AC1 @D23
  Scenario: Sorting on name ascending and descending
    Given the vocabularies "Cherry, Apple, Banana" exist
    When I list the vocabularies with "name={{run}}&sort=name"
    Then the item names are "Apple, Banana, Cherry"
    When I list the vocabularies with "name={{run}}&sort=-name"
    Then the item names are "Cherry, Banana, Apple"

  @D58 @D23
  Scenario: The name sort compares by code point
    Given the vocabularies "Ab, AZ" exist
    When I list the vocabularies with "name={{run}}&sort=name"
    Then the item names are "AZ, Ab"

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario: Sorting on creation date ascending and descending
    Given the vocabularies "Cherry, Apple, Banana" exist, created one second apart
    When I list the vocabularies with "name={{run}}&sort=creationDate"
    Then the item names are "Cherry, Apple, Banana"
    When I list the vocabularies with "name={{run}}&sort=-creationDate"
    Then the item names are "Banana, Apple, Cherry"

  @AC_SIMPL-18164_AC1 @D23 @D25
  Scenario: Sorting on the last update date
    Given the vocabularies "Cherry, Apple, Banana" exist, created one second apart
    And a second has passed
    And the vocabulary "Cherry" has a new version
    When I list the vocabularies with "name={{run}}&sort=lastUpdated"
    Then the item names are "Apple, Banana, Cherry"
    When I list the vocabularies with "name={{run}}&sort=-lastUpdated"
    Then the item names are "Cherry, Banana, Apple"

  @AC_SIMPL-18164_AC1 @D23
  Scenario: Sorting on status groups the statuses and breaks ties by name
    Given the vocabularies "Delta, Alpha, Charlie, Bravo" exist
    And the vocabulary "Charlie" is deprecated
    And the vocabulary "Alpha" is deprecated
    When I list the vocabularies with "name={{run}}&sort=status"
    Then the items with the same status are grouped and sorted by name ascending
    And I keep the status order as the ascending order
    When I list the vocabularies with "name={{run}}&sort=-status"
    Then the status groups come in the reverse order of the ascending sort
    And the items with the same status are grouped and sorted by name ascending

  @AC_SIMPL-18164_AC4 @D21 @D52 @negative
  Scenario Outline: An unsupported sort or filter <query> is rejected and returns no result set
    When I list the vocabularies with "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue in "query" named "<parameter>"
    And the response holds no metadata

    Examples:
      | query               | parameter |
      | sort=description    | sort      |
      | sort=+name          | sort      |
      | sort=version        | sort      |
      | colour=blue         | colour    |
      | createdBy=someone   | createdBy |

  @D52 @negative
  Scenario: A query parameter given twice is rejected
    When I list the vocabularies with "page=1&page=1"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @AC_SIMPL-18164_AC3 @D22 @D23
  Scenario: Paging keeps the requested sort order across pages
    Given the vocabularies "Echo, Alpha, Delta, Bravo, Charlie" exist
    When I list the vocabularies with "name={{run}}&sort=-name&pageSize=2"
    Then the response status is 200
    And the item names are "Echo, Delta"
    And the response field "total" is 5
    And the response field "totalPages" is 3
    And the response has the links "self, first, next, last" and not "prev"
    And the "next" link points to page 2
    And the "last" link points to page 3
    When I list the vocabularies with "name={{run}}&sort=-name&pageSize=2&page=2"
    Then the response status is 200
    And the item names are "Charlie, Bravo"
    And the response field "page" is 2
    And the response has the links "self, first, prev, next, last" and not ""
    When I list the vocabularies with "name={{run}}&sort=-name&pageSize=2&page=3"
    Then the item names are "Alpha"
    And the response has the links "self, first, prev, last" and not "next"

  @D22 @D59
  Scenario: A page beyond the last page is empty and points back to the last page
    Given the vocabularies "Alpha, Bravo, Charlie" exist
    When I list the vocabularies with "name={{run}}&pageSize=2&page=5"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" is 3
    And the response field "totalPages" is 2
    And the response field "page" is 5
    And the "prev" link points to page 2
    And the response has the links "self, first, prev, last" and not "next"

  @D54 @D24 @negative
  Scenario: Reading a vocabulary with its name in another case gives 404
    Given the vocabulary "Health" exists
    When I send "GET" to "/vocabularies/HEALTH{{runupper}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem has an issue in "path" named "vocabularyName"

  @D24 @negative
  Scenario: Reading a vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/{{name:Ghost}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem has an issue in "path" named "vocabularyName"

  @D49 @negative
  Scenario Outline: Reading a vocabulary with the invalid path name <name> gives 400
    When I send "GET" to "/vocabularies/<name>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "vocabularyName" with type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | name                                                               |
      | lower{{run}}                                                       |
      | Ab                                                                 |
      | Has-hyphen                                                         |
      | A1234567890123456789012345678901234567890123456789012345678901234   |

  @D21 @negative
  Scenario: An unknown query parameter on a read by name is rejected
    Given the vocabulary "Health" exists
    When I send "GET" to "/vocabularies/{{name:Health}}?expand=versions"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "expand"

  @AC_SIMPL-18162_AC4 @wip
  Scenario: An internal error while listing returns 500 with the detail of the error
    Given I am authenticated as "ga-admin"
    When I list the vocabularies with "name={{run}}"
    Then the response is a problem with status 500 and type "urn:problem-type:belgif:internalServerError"
    And the problem has a detail
    And the response holds no metadata
