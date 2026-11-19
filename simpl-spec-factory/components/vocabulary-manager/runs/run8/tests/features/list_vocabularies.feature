Feature: List, filter, sort and page vocabularies through the API
  GET /vocabularies returns a page of vocabularies (latest version values).
  The store is never reset, so every scenario filters on its own unique names.

  Background:
    Given I am authenticated as "ga-admin"

  @D22 @Q2 @NFR-04
  Scenario: A page size of 100 is accepted
    Given a vocabulary "Alpha" exists
    When I send a GET request to "/vocabularies?name=${uid}&pageSize=100"
    Then the response status is 200
    And the response field "pageSize" is "100"
    And the response items have the names "${Alpha}"

  @AC_SIMPL-18162_AC1 @D16 @D22 @D25 @D5 @BLG-col-paging
  Scenario Outline: A user with a vocabulary role gets a paginated list with the metadata of each vocabulary
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And a vocabulary "Beta" exists
    And I am authenticated as "<user>"
    When I send a GET request to "/vocabularies?name=${uid}"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "page" is "1"
    And the response field "pageSize" is "20"
    And the response field "total" is "2"
    And the response field "totalPages" is "1"
    And the response items have the names "${Alpha},${Beta}"
    And every item has the fields "name,version,description,status,creationDate,lastUpdateDate,href,contentHref,createdBy"
    And the response field "items[0].version" is "2"
    And the response field "items[1].version" is "1"
    And the response field "items[0].href" is "/vocabularies/${Alpha}"

    Examples:
      | user      |
      | ga-admin  |
      | ga-viewer |

  @AC_SIMPL-18162_AC2 @AC_SIMPL-18164_AC4 @D22 @D21 @Q2 @NFR-04 @negative
  Scenario Outline: Wrong pagination parameters are rejected with the detail of the error
    When I send a GET request to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has a detail
    And the problem has an issue in "query" named "<param>"
    And the response field "items" is absent

    Examples:
      | query         | param    |
      | page=0        | page     |
      | page=-1       | page     |
      | page=abc      | page     |
      | page=1.5      | page     |
      | pageSize=0    | pageSize |
      | pageSize=101  | pageSize |
      | pageSize=many | pageSize |

  @AC_SIMPL-18162_AC3 @D16 @D17 @negative @OWASP-02
  Scenario: A user without a vocabulary role gets 403 and no metadata
    Given a vocabulary "Alpha" exists
    And I am authenticated as "ga-norole"
    When I send a GET request to "/vocabularies?name=${uid}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem has a detail
    And the response field "items" is absent

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    When I send a GET request to "/vocabularies?name=${Alpha.upper}"
    Then the response status is 200
    And the response items have the names "${Alpha}"
    When I send a GET request to "/vocabularies?name=pha${uid}"
    Then the response items have the names "${Alpha}"

  @AC_SIMPL-18164_AC4 @D19 @negative
  Scenario Outline: A name filter that breaks the relaxed pattern is rejected
    When I send a GET request to "/vocabularies?name=<value>"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "name"
    And the response field "items" is absent

    Examples:
      | value           |
      | a-b             |
      | a%20b           |
      |                 |
      | {{repeat:a:65}} |

  @D19
  Scenario: A one-character lower-case name filter is accepted
    When I send a GET request to "/vocabularies?name=q&pageSize=1"
    Then the response status is 200

  @AC_SIMPL-18164_AC1 @D3
  Scenario: Filter on the vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Beta" exists
    And the vocabulary status of "Beta" is set to "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${uid}&status=DEPRECATED"
    Then the response status is 200
    And the response items have the names "${Beta}"
    And the response field "items[0].status" is "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${uid}&status=ACTIVE"
    Then the response items have the names "${Alpha}"

  @AC_SIMPL-18164_AC4 @negative
  Scenario: An unknown status value is rejected
    When I send a GET request to "/vocabularies?status=RETIRED"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "status"

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: Date filters with a date match the whole UTC day
    Given a vocabulary "Alpha" exists
    And I wait for the next second
    And a vocabulary "Bravo" exists
    When I send a GET request to "/vocabularies/${Alpha}"
    And I remember the date of the response field "creationDate" as "createdDay"
    And I remember the date of the response field "lastUpdateDate" as "updatedDay"
    And I send a GET request to "/vocabularies?name=${uid}&creationDate=${saved:createdDay}"
    Then the response status is 200
    And the response items are those of "${Alpha},${Bravo}" whose "creationDate" falls on the day "${saved:createdDay}"
    When I send a GET request to "/vocabularies?name=${uid}&lastUpdated=${saved:updatedDay}"
    Then the response items are those of "${Alpha},${Bravo}" whose "lastUpdateDate" falls on the day "${saved:updatedDay}"

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: Date filters with a date-time match the value to the second
    Given a vocabulary "Alpha" exists
    And I wait for the next second
    And a vocabulary "Bravo" exists
    When I send a GET request to "/vocabularies/${Alpha}"
    And I remember the response field "creationDate" as "created"
    And I remember the response field "lastUpdateDate" as "updated"
    And I send a GET request to "/vocabularies?name=${uid}&creationDate=${saved:created}"
    Then the response status is 200
    And the response items have the names "${Alpha}"
    When I send a GET request to "/vocabularies?name=${uid}&lastUpdated=${saved:updated}"
    Then the response items have the names "${Alpha}"

  @AC_SIMPL-18164_AC2 @D20 @D22
  Scenario: A filter that matches nothing returns an empty page without error
    Given a vocabulary "Alpha" exists
    When I send a GET request to "/vocabularies?name=${uid}&creationDate=2001-01-01"
    Then the response status is 200
    And the response has no items
    And the response field "total" is "0"
    And the response field "totalPages" is "0"
    And the link "self" has path "/vocabularies" and query "name=${uid}&creationDate=2001-01-01&page=1&pageSize=20"
    And the link "first" has path "/vocabularies" and query "name=${uid}&creationDate=2001-01-01&page=1&pageSize=20"
    And the link "last" is absent
    And the link "prev" is absent
    And the link "next" is absent

  @AC_SIMPL-18164_AC4 @D20 @negative
  Scenario Outline: A date filter value that is not a valid date is rejected
    When I send a GET request to "/vocabularies?<param>=<value>"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "<param>"

    Examples:
      | param        | value      |
      | creationDate | 2024-02-30 |
      | creationDate | 2024-13-01 |
      | lastUpdated  | 2024-02-30 |
      | lastUpdated  | yesterday  |

  @AC_SIMPL-18164_AC1 @D23 @D58 @BLG-col-sort
  Scenario Outline: Sort by each supported field and direction
    Given a vocabulary "Charlie" exists
    And I wait for the next second
    And a vocabulary "Alpha" exists
    And I wait for the next second
    And a vocabulary "Bravo" exists
    And I wait for the next second
    And the vocabulary status of "Bravo" is set to "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${uid}&sort=<sort>"
    Then the response status is 200
    And the response field "total" is "3"
    And the vocabulary items are sorted by "<field>" "<direction>"
    And the response items have the names "<order>"

    Examples:
      | sort          | field        | direction  | order                          |
      | name          | name         | ascending  | ${Alpha},${Bravo},${Charlie}   |
      | -name         | name         | descending | ${Charlie},${Bravo},${Alpha}   |
      | status        | status       | ascending  | ${Alpha},${Charlie},${Bravo}   |
      | -status       | status       | descending | ${Bravo},${Alpha},${Charlie}   |
      | creationDate  | creationDate | ascending  | ${Charlie},${Alpha},${Bravo}   |
      | -creationDate | creationDate | descending | ${Bravo},${Alpha},${Charlie}   |
      | lastUpdated   | lastUpdated  | ascending  | ${Charlie},${Alpha},${Bravo}   |
      | -lastUpdated  | lastUpdated  | descending | ${Bravo},${Alpha},${Charlie}   |

  @AC_SIMPL-18164_AC1 @D23
  Scenario: The default order is name ascending and named orders are exact
    Given a vocabulary "Charlie" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    When I send a GET request to "/vocabularies?name=${uid}"
    Then the response items have the names "${Alpha},${Bravo},${Charlie}"
    When I send a GET request to "/vocabularies?name=${uid}&sort=-name"
    Then the response items have the names "${Charlie},${Bravo},${Alpha}"

  @D23
  Scenario: Status sort breaks ties by name ascending
    Given a vocabulary "Charlie" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the vocabulary status of "Charlie" is set to "DEPRECATED"
    When I send a GET request to "/vocabularies?name=${uid}&sort=status"
    Then the response items have the names "${Alpha},${Bravo},${Charlie}"
    When I send a GET request to "/vocabularies?name=${uid}&sort=-status"
    Then the response items have the names "${Charlie},${Alpha},${Bravo}"

  @D58 @D23
  Scenario: The name sort compares by code point
    Given a vocabulary "Ab" exists
    And a vocabulary "AZ" exists
    When I send a GET request to "/vocabularies?name=${uid}&sort=name"
    Then the response items have the names "${AZ},${Ab}"

  @AC_SIMPL-18164_AC4 @D21 @D52 @negative @BLG-req-valid
  Scenario Outline: Unsupported sort or filter parameters are rejected
    When I send a GET request to "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"
    And the response field "items" is absent

    Examples:
      | query                | param       |
      | sort=description     | sort        |
      | sort=+name           | sort        |
      | sort=lastUpdateDate  | sort        |
      | description=x        | description |
      | createdBy=someone    | createdBy   |
      | version=1            | version     |

  @D52 @negative
  Scenario: A query parameter given twice is rejected
    When I send a GET request to "/vocabularies?pageSize=10&pageSize=20"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "pageSize"

  @AC_SIMPL-18164_AC3 @D22 @D23 @D78
  Scenario: Pagination is applied together with sorting
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send a GET request to "/vocabularies?name=${uid}&sort=-name&pageSize=2"
    Then the response status is 200
    And the response field "page" is "1"
    And the response field "pageSize" is "2"
    And the response field "total" is "3"
    And the response field "totalPages" is "2"
    And the response items have the names "${Charlie},${Bravo}"
    And the link "self" has path "/vocabularies" and query "name=${uid}&sort=-name&page=1&pageSize=2"
    And the link "first" has path "/vocabularies" and query "name=${uid}&sort=-name&page=1&pageSize=2"
    And the link "next" has path "/vocabularies" and query "name=${uid}&sort=-name&page=2&pageSize=2"
    And the link "last" has path "/vocabularies" and query "name=${uid}&sort=-name&page=2&pageSize=2"
    And the link "prev" is absent
    When I send a GET request to "/vocabularies?name=${uid}&sort=-name&pageSize=2&page=2"
    Then the response items have the names "${Alpha}"
    And the link "prev" has path "/vocabularies" and query "name=${uid}&sort=-name&page=1&pageSize=2"
    And the link "next" is absent
    And the link "last" has path "/vocabularies" and query "name=${uid}&sort=-name&page=2&pageSize=2"

  @D22 @D59 @D78
  Scenario: A page beyond the last returns an empty page whose prev link points to the last page
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send a GET request to "/vocabularies?name=${uid}&pageSize=2&page=5"
    Then the response status is 200
    And the response has no items
    And the response field "total" is "3"
    And the response field "totalPages" is "2"
    And the link "prev" has path "/vocabularies" and query "name=${uid}&page=2&pageSize=2"
    And the link "next" is absent

  @D63 @negative @API-37
  Scenario: A list request that accepts only XML is refused with 406
    When I send a GET request to "/vocabularies" with header "Accept" set to "application/xml"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"

  @wip @AC_SIMPL-18162_AC4 @D24 @negative
  Scenario: An internal error returns 500 with the detail of the error and no metadata
    # A black-box test cannot make the component fail internally on demand.
    # D44 maps an unreachable store to 503 (store_down.feature), not 500.
    # Kept as documentation; it is excluded from every run mode.
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 500 and type "urn:problem-type:belgif:internalServerError"
    And the response field "items" is absent
