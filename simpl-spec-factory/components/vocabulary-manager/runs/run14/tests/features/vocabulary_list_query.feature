Feature: Filter, sort and page the vocabulary list via the API
  GET /vocabularies filters on name (contains, case-insensitive, D19), status (vocabulary status, D3),
  creationDate and lastUpdated (D20), sorts as D23 and D58, pages as D22, D59, D78, D110 and D111.
  Every scenario restricts the list to its own vocabularies with name=$RUN (E4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18164_AC1 @D23 @D58 @D128
  Scenario Outline: The list sorts on a supported field and direction: <sort>
    Given vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart
    And a second has passed
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    When I send a GET request to "/vocabularies?name=$RUN&sort=<sort>"
    Then the response status is 200
    And the response items are named "<order>"

    Examples:
      | sort          | order                     |
      | name          | $Apple, $Banana, $Cherry  |
      | -name         | $Cherry, $Banana, $Apple  |
      | status        | $Banana, $Cherry, $Apple  |
      | -status       | $Apple, $Banana, $Cherry  |
      | creationDate  | $Cherry, $Apple, $Banana  |
      | -creationDate | $Banana, $Apple, $Cherry  |
      | lastUpdated   | $Cherry, $Banana, $Apple  |
      | -lastUpdated  | $Apple, $Banana, $Cherry  |

  @D23
  Scenario: The default order is name ascending
    Given vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart
    When I send a GET request to "/vocabularies?name=$RUN"
    Then the response items are named "$Apple, $Banana, $Cherry"

  @D58 @D23
  Scenario: The name sort compares by code point
    Given a vocabulary "$Ab" exists
    And a vocabulary "$AZ" exists
    When I send a GET request to "/vocabularies?name=$RUN&sort=name"
    Then the response items are named "$AZ, $Ab"

  @D23
  Scenario: Ties are broken by name ascending
    Given a vocabulary "$Delta" exists
    And a vocabulary "$Bravo" exists
    And a vocabulary "$Charlie" exists
    When I send a GET request to "/vocabularies?name=$RUN&sort=-status"
    Then the response items are named "$Bravo, $Charlie, $Delta"

  @AC_SIMPL-18164_AC1 @D19
  Scenario Outline: The name filter matches a part of the name, ignoring case: <filter>
    Given a vocabulary "$Apple" exists
    And a vocabulary "$Banana" exists
    When I send a GET request to "/vocabularies?name=<filter>"
    Then the response status is 200
    And the response items are named "<names>"

    Examples:
      | filter         | names           |
      | $Apple         | $Apple          |
      | $Apple!upper   | $Apple          |
      | $Apple!lower   | $Apple          |
      | $RUN           | $Apple, $Banana |
      | $RUN!upper     | $Apple, $Banana |

  @D19 @Q2 @NFR-04
  Scenario: A name filter of one character is valid
    When I send a GET request to "/vocabularies?name=Q&pageSize=1"
    Then the response status is 200

  @AC_SIMPL-18164_AC1 @D3
  Scenario: The status filter uses the vocabulary status
    Given a vocabulary "$Apple" exists
    And a vocabulary "$Banana" exists
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    And version 1 of vocabulary "$Banana" has version status "DEPRECATED"
    When I send a GET request to "/vocabularies?name=$RUN&status=DEPRECATED"
    Then the response items are named "$Apple"
    When I send a GET request to "/vocabularies?name=$RUN&status=ACTIVE"
    Then the response items are named "$Banana"

  @AC_SIMPL-18164_AC1 @D20
  Scenario: A creation date matches every vocabulary created on that UTC day
    Given a vocabulary "$Apple" exists
    And a vocabulary "$Banana" exists
    And I save field "creationDate" of "/vocabularies/$Apple" as "created"
    And I save the date of "created" as "day"
    And the scenario is skipped unless "$Banana" was created on the UTC day "${day}"
    When I send a GET request to "/vocabularies?name=$RUN&creationDate=${day}"
    Then the response items are named "$Apple, $Banana"

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: A creation date-time matches to the second
    Given vocabularies "$Apple, $Banana" exist, created one second apart
    And I save field "creationDate" of "/vocabularies/$Banana" as "created"
    When I send a GET request to "/vocabularies?name=$RUN&creationDate=${created}"
    Then the response items are named "$Banana"

  @D168 @D20
  Scenario: A plus sign in a date-time filter is an offset
    Given vocabularies "$Apple, $Banana" exist, created one second apart
    And I save field "creationDate" of "/vocabularies/$Banana" as "created"
    And I save the time "created" as "local" shifted to "+01:00"
    When I send a GET request to "/vocabularies?name=$RUN&creationDate=${local}"
    Then the response status is 200
    And the response items are named "$Banana"

  @D110 @D20
  Scenario: A date-time without an offset is read as UTC
    Given vocabularies "$Apple, $Banana" exist, created one second apart
    And I save field "creationDate" of "/vocabularies/$Apple" as "created"
    And I save the time "created" without its offset as "plain"
    When I send a GET request to "/vocabularies?name=$RUN&creationDate=${plain}"
    Then the response items are named "$Apple"

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: The lastUpdated filter matches the last update date
    Given vocabularies "$Apple, $Banana" exist, created one second apart
    And a second has passed
    And vocabulary "$Apple" has vocabulary status "DEPRECATED"
    And I save field "lastUpdateDate" of "/vocabularies/$Apple" as "updated"
    When I send a GET request to "/vocabularies?name=$RUN&lastUpdated=${updated}"
    Then the response items are named "$Apple"

  @AC_SIMPL-18164_AC1 @D20
  Scenario: Filters combine
    Given vocabularies "$Apple, $Banana" exist, created one second apart
    And vocabulary "$Banana" has vocabulary status "DEPRECATED"
    And I save field "creationDate" of "/vocabularies/$Banana" as "created"
    And I save the date of "created" as "day"
    When I send a GET request to "/vocabularies?name=$RUN&status=DEPRECATED&creationDate=${day}&sort=-name"
    Then the response items are named "$Banana"

  @AC_SIMPL-18164_AC2 @D22 @D59
  Scenario Outline: A filter that matches nothing gives an empty result, not an error: <query>
    Given a vocabulary "$Apple" exists
    When I send a GET request to "/vocabularies?<query>"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0
    And the link "self" is present
    And the link "first" is present
    And the link "last" is absent
    And the link "prev" is absent
    And the link "next" is absent

    Examples:
      | query                                  |
      | name=Nomatch$RUN                       |
      | name=$RUN&status=DEPRECATED            |
      | name=$RUN&creationDate=2000-01-01      |
      | name=$RUN&lastUpdated=2001-02-03T04:05:06Z |

  @AC_SIMPL-18164_AC3 @D22 @D78 @D110 @D111
  Scenario: Pages keep the requested sort order and the links keep the request
    Given vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart
    When I send a GET request to "/vocabularies?name=$RUN&sort=-name&pageSize=2"
    Then the response status is 200
    And the response items are named "$Cherry, $Banana"
    And the response field "page" equals 1
    And the response field "pageSize" equals 2
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the link "self" is "/vocabularies?page=1&pageSize=2&name=$RUN&sort=-name"
    And the link "first" is "/vocabularies?page=1&pageSize=2&name=$RUN&sort=-name"
    And the link "next" is "/vocabularies?page=2&pageSize=2&name=$RUN&sort=-name"
    And the link "last" is "/vocabularies?page=2&pageSize=2&name=$RUN&sort=-name"
    And the link "prev" is absent
    When I follow the "next" link
    Then the response status is 200
    And the response items are named "$Apple"
    And the response field "page" equals 2
    And the link "prev" is "/vocabularies?page=1&pageSize=2&name=$RUN&sort=-name"
    And the link "next" is absent

  @AC_SIMPL-18164_AC3 @D23
  Scenario Outline: Every page follows the sort order: <sort>
    Given vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart
    When I send a GET request to "/vocabularies?name=$RUN&sort=<sort>&pageSize=1&page=2"
    Then the response items are named "<second>"

    Examples:
      | sort          | second  |
      | name          | $Banana |
      | creationDate  | $Apple  |
      | -lastUpdated  | $Apple  |
      | status        | $Banana |

  @D111
  Scenario: Paging links always carry page and pageSize
    Given a vocabulary "$Apple" exists
    When I send a GET request to "/vocabularies?name=$RUN"
    Then the link "self" is "/vocabularies?page=1&pageSize=20&name=$RUN"
    And the link "first" is "/vocabularies?page=1&pageSize=20&name=$RUN"
    And the link "last" is "/vocabularies?page=1&pageSize=20&name=$RUN"

  @D22 @D59 @D110
  Scenario: A page beyond the last page is empty and its prev link points to the last page
    Given vocabularies "$Cherry, $Apple, $Banana" exist, created one second apart
    When I send a GET request to "/vocabularies?name=$RUN&pageSize=2&page=5"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the link "prev" is "/vocabularies?page=2&pageSize=2&name=$RUN"
    And the link "last" is "/vocabularies?page=2&pageSize=2&name=$RUN"
    And the link "next" is absent

  @AC_SIMPL-18164_AC4 @D19 @D20 @D21 @D23 @D116 @D130 @D168 @negative @BLG-err-problem
  Scenario Outline: An unsupported or invalid filter or sort is rejected: <query>
    Given a vocabulary "$Apple" exists
    When I send a GET request to "/vocabularies?name=$RUN&<query>"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "query" named "<param>"
    And the response holds no items

    Examples:
      | query                                  | param        |
      | sort=version                           | sort         |
      | sort=createdBy                         | sort         |
      | sort=+name                             | sort         |
      | sort=NAME                              | sort         |
      | status=RETIRED                         | status       |
      | status=active                          | status       |
      | creationDate=2024-02-30                | creationDate |
      | creationDate=2024-13-01                | creationDate |
      | creationDate=yesterday                 | creationDate |
      | creationDate=2024-01-01T25:00:00Z      | creationDate |
      | creationDate=2024-01-01T10:00:00+19:00 | creationDate |
      | lastUpdated=2024-1-1                   | lastUpdated  |
      | lastUpdated=2024-02-30T10:00:00Z       | lastUpdated  |
      | createdBy=ga-admin                     | createdBy    |
      | description=x                          | description  |

  @AC_SIMPL-18164_AC4 @D19 @D168 @negative
  Scenario Outline: A name filter outside the relaxed pattern is rejected: <filter>
    When I send a GET request to "/vocabularies?name=<filter>"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "name"
    And the response holds no items

    Examples:
      | filter      |
      | Ab-c        |
      | Ab+c        |
      | Ab%20c      |
      | Ab_c        |
      | $N65        |
      |             |
