Feature: Retrieve, filter, sort and page the vocabulary list via the API
  GET /vocabularies returns the vocabularies with values of their latest version (D25).
  The store is shared, so every scenario filters on its own run id (E4).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"

  @AC_SIMPL-18162_AC1 @D5 @D22 @D25 @D27 @D28 @BLG-col-paging @API-36
  Scenario: The list is paged and each vocabulary carries its metadata
    Given a vocabulary "{Apple}" exists with 2 versions
    And a vocabulary "{Banana}" exists
    When I send "GET" to "/vocabularies?name={run}"
    Then the response status is 200
    And the response content type is "application/json"
    And the response has the paging fields
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the response field "total" equals 2
    And the response field "totalPages" equals 1
    And every item has the fields "href, contentHref, name, version, description, status, createdBy, creationDate, lastUpdateDate"
    And the response items have the "name" values ["{Apple}", "{Banana}"]
    And the response items have the "version" values [2, 1]
    And the response items have the "status" values ["ACTIVE", "ACTIVE"]
    And the response field "items[0].href" is "/vocabularies/{Apple}"
    And the response field "items[0].contentHref" ends with "/content/{Apple}"
    And the response field "items[0].description" is "Description 1 of run {run}."
    And the response field "items[1].description" is "Description of {Banana}."

  @D3 @D25
  Scenario: The list shows the vocabulary status, not a version status
    Given a vocabulary "{Apple}" exists
    And the vocabulary "{Apple}" has status "DEPRECATED"
    And a vocabulary "{Banana}" exists with 2 versions
    And version 2 of "{Banana}" has status "DEPRECATED"
    When I send "GET" to "/vocabularies?name={run}"
    Then the response items have the "status" values ["DEPRECATED", "ACTIVE"]

  @AC_SIMPL-18162_AC2 @D22 @negative
  Scenario Outline: A wrong paging parameter is rejected and no metadata is returned (<query>)
    Given a vocabulary "{Apple}" exists
    When I send "GET" to "/vocabularies?name={run}&<query>"
    Then the response is a problem with status 400
    And the problem detail is present
    And the problem has an issue in "query" named "<parameter>"
    And the response has no "items" field

    Examples:
      | query        | parameter |
      | page=0       | page      |
      | page=-1      | page      |
      | page=abc     | page      |
      | page=1.5     | page      |
      | pageSize=0   | pageSize  |
      | pageSize=101 | pageSize  |
      | pageSize=x   | pageSize  |

  @AC_SIMPL-18162_AC3 @D16 @D17 @negative @OWASP-02
  Scenario: A user without a vocabulary management role gets 403 and no metadata
    Given a vocabulary "{Apple}" exists
    And I am an authenticated user with no vocabulary management role
    When I send "GET" to "/vocabularies?name={run}"
    Then the response is a problem with status 403
    And the problem detail is present
    And the response has no "items" field

  @AC_SIMPL-18162_AC4 @wip @negative
  Scenario: An internal error gives 500 with details and no metadata
    Given a vocabulary "{Apple}" exists
    When I send "GET" to "/vocabularies?name={run}"
    Then the response is a problem with status 500
    And the problem detail is present
    And the response has no "items" field

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given a vocabulary "{Apple}" exists
    And a vocabulary "{Banana}" exists
    When I send "GET" to "/vocabularies?name={upper:Banana}"
    Then the response status is 200
    And the response items have the "name" values ["{Banana}"]
    When I send "GET" to "/vocabularies?name={lower:Apple}"
    Then the response items have the "name" values ["{Apple}"]
    When I send "GET" to "/vocabularies?name={run}"
    Then the response items have the "name" values ["{Apple}", "{Banana}"]

  @D19
  Scenario: A name filter of one character is allowed
    When I send "GET" to "/vocabularies?name=a&pageSize=1"
    Then the response status is 200

  @AC_SIMPL-18164_AC1 @D3
  Scenario: The status filter matches the vocabulary status
    Given a vocabulary "{Apple}" exists
    And the vocabulary "{Apple}" has status "DEPRECATED"
    And a vocabulary "{Banana}" exists
    When I send "GET" to "/vocabularies?name={run}&status=DEPRECATED"
    Then the response status is 200
    And the response items have the "name" values ["{Apple}"]
    When I send "GET" to "/vocabularies?name={run}&status=ACTIVE"
    Then the response items have the "name" values ["{Banana}"]

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: The creation date filter matches a whole UTC day or one second
    Given a vocabulary "{Apple}" exists
    And I wait until the next second
    And a vocabulary "{Banana}" exists
    When I send "GET" to "/vocabularies/{Apple}"
    And I remember the response field "creationDate" as "appleCreated"
    And I send "GET" to "/vocabularies?name={run}&creationDate={saved:appleCreated}"
    Then the response status is 200
    And the response items have the "name" values ["{Apple}"]
    When I send "GET" to "/vocabularies?name={run}&creationDate={today}"
    Then the response items have the "name" values ["{Apple}", "{Banana}"]
    When I send "GET" to "/vocabularies?name={run}&creationDate=2000-01-01"
    Then the response has 0 items

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: The last update filter matches a whole UTC day or one second
    Given a vocabulary "{Apple}" exists
    And a vocabulary "{Banana}" exists
    And I wait until the next second
    And the vocabulary "{Banana}" has status "DEPRECATED"
    When I send "GET" to "/vocabularies/{Banana}"
    And I remember the response field "lastUpdateDate" as "bananaUpdated"
    And I send "GET" to "/vocabularies?name={run}&lastUpdated={saved:bananaUpdated}"
    Then the response status is 200
    And the response items have the "name" values ["{Banana}"]
    When I send "GET" to "/vocabularies?name={run}&lastUpdated={today}"
    Then the response items have the "name" values ["{Apple}", "{Banana}"]

  @AC_SIMPL-18164_AC1 @D23
  Scenario Outline: Sorting by name (<sort>)
    Given a vocabulary "{Banana}" exists
    And a vocabulary "{Cherry}" exists
    And a vocabulary "{Apple}" exists
    When I send "GET" to "/vocabularies?name={run}<sort>"
    Then the response status is 200
    And the response items have the "name" values <order>

    Examples:
      | sort        | order                                  |
      |             | ["{Apple}", "{Banana}", "{Cherry}"]    |
      | &sort=name  | ["{Apple}", "{Banana}", "{Cherry}"]    |
      | &sort=-name | ["{Cherry}", "{Banana}", "{Apple}"]    |

  @AC_SIMPL-18164_AC1 @D23
  Scenario Outline: Sorting by status breaks ties by name ascending (<sort>)
    Given a vocabulary "{Cherry}" exists
    And a vocabulary "{Apple}" exists
    And a vocabulary "{Banana}" exists
    And the vocabulary "{Banana}" has status "DEPRECATED"
    When I send "GET" to "/vocabularies?name={run}&sort=<sort>"
    Then the response items have the "name" values <order>

    Examples:
      | sort    | order                                |
      | status  | ["{Apple}", "{Cherry}", "{Banana}"]  |
      | -status | ["{Banana}", "{Apple}", "{Cherry}"]  |

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario Outline: Sorting by creation date (<sort>)
    Given a vocabulary "{Cherry}" exists
    And I wait until the next second
    And a vocabulary "{Apple}" exists
    And I wait until the next second
    And a vocabulary "{Banana}" exists
    When I send "GET" to "/vocabularies?name={run}&sort=<sort>"
    Then the response items have the "name" values <order>

    Examples:
      | sort          | order                                |
      | creationDate  | ["{Cherry}", "{Apple}", "{Banana}"]  |
      | -creationDate | ["{Banana}", "{Apple}", "{Cherry}"]  |

  @AC_SIMPL-18164_AC1 @D23 @D25
  Scenario Outline: Sorting by last update date (<sort>)
    Given a vocabulary "{Apple}" exists
    And a vocabulary "{Banana}" exists
    And I wait until the next second
    And the vocabulary "{Apple}" has status "DEPRECATED"
    When I send "GET" to "/vocabularies?name={run}&sort=<sort>"
    Then the response items have the "name" values <order>

    Examples:
      | sort         | order                    |
      | lastUpdated  | ["{Banana}", "{Apple}"]  |
      | -lastUpdated | ["{Apple}", "{Banana}"]  |

  @D58 @D23
  Scenario: The name sort compares by code point
    Given a vocabulary "N{run}Ab" exists
    And a vocabulary "N{run}AZ" exists
    When I send "GET" to "/vocabularies?name={run}&sort=name"
    Then the response items have the "name" values ["N{run}AZ", "N{run}Ab"]

  @AC_SIMPL-18164_AC2 @D22
  Scenario: A filter without matches returns an empty page without an error
    When I send "GET" to "/vocabularies?name=Nomatch{run}"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0
    And the response has the links ["self", "first"]
    And the response has none of the links ["prev", "next", "last"]

  @AC_SIMPL-18164_AC3 @D22 @D23 @D59 @BLG-col-paging
  Scenario: A page of a sorted list keeps the sort order and has paging links
    Given a vocabulary "{Apple}" exists
    And a vocabulary "{Banana}" exists
    And a vocabulary "{Cherry}" exists
    And a vocabulary "{Damson}" exists
    And a vocabulary "{Elder}" exists
    When I send "GET" to "/vocabularies?name={run}&sort=-name&pageSize=2&page=2"
    Then the response status is 200
    And the response items have the "name" values ["{Cherry}", "{Banana}"]
    And the response field "page" equals 2
    And the response field "pageSize" equals 2
    And the response field "total" equals 5
    And the response field "totalPages" equals 3
    And the response has the links ["self", "first", "prev", "next", "last"]
    And the "prev" link points to page 1
    And the "next" link points to page 3
    And the "last" link points to page 3
    When I send "GET" to "/vocabularies?name={run}&sort=-name&pageSize=2&page=3"
    Then the response items have the "name" values ["{Apple}"]
    And the response has none of the links ["next"]

  @AC_SIMPL-18164_AC3 @D22 @D59
  Scenario: A page beyond the last page is empty and points back to the last page
    Given a vocabulary "{Apple}" exists
    And a vocabulary "{Banana}" exists
    And a vocabulary "{Cherry}" exists
    When I send "GET" to "/vocabularies?name={run}&pageSize=2&page=5"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the "prev" link points to page 2
    And the response has none of the links ["next"]

  @D22
  Scenario: A page size of 100 is allowed
    When I send "GET" to "/vocabularies?name={run}&pageSize=100"
    Then the response status is 200
    And the response field "pageSize" equals 100

  @AC_SIMPL-18164_AC4 @D19 @D20 @D21 @D52 @negative @BLG-req-valid
  Scenario Outline: An unsupported or invalid filter or sort is rejected (<case>)
    When I send "GET" to "/vocabularies?<query>"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "<parameter>"
    And the response has no "items" field

    Examples:
      | case                         | query                                | parameter    |
      | unsupported sort field       | sort=description                     | sort         |
      | unknown sort value           | sort=foo                             | sort         |
      | invalid status value         | status=ARCHIVED                      | status       |
      | status in lower case         | status=active                        | status       |
      | date in wrong format         | creationDate=18-11-2026              | creationDate |
      | impossible date              | creationDate=2026-02-30              | creationDate |
      | impossible last update date  | lastUpdated=2026-13-01               | lastUpdated  |
      | name with a hyphen           | name=Ab-c                            | name         |
      | name of 65 characters        | name={chars:65}                      | name         |
      | empty name                   | name=                                | name         |
      | unknown filter field         | colour=blue                          | colour       |
      | repeated parameter           | name={run}&name={run}                | name         |

  @AC_SIMPL-18164_AC4 @D21 @negative
  Scenario: An unknown query parameter gives exactly one issue
    When I send "GET" to "/vocabularies?name={run}&owner=me"
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And the problem has an issue in "query" named "owner"

  @D21 @D52 @negative
  Scenario Outline: An unknown query parameter is rejected on every GET operation (<operation>)
    Given a vocabulary "{Subject}" exists
    And an external vocabulary "{ExtSubject}" is registered with namespace "https://ext-{run}.example.org/subject#"
    When I call the operation "<operation>" with the query parameter "unknownParam=1"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "unknownParam"

    Examples:
      | operation                          |
      | getVocabularies                    |
      | getVocabulary                      |
      | getVocabularyVersions              |
      | getVocabularyVersion               |
      | downloadVocabulary                 |
      | downloadVocabularyVersion          |
      | getExternalVocabularies            |
      | getExternalVocabulary              |
      | getExternalVocabularyContentByName |

  @D52 @negative
  Scenario: A query parameter on DELETE is rejected
    Given an external vocabulary "{ExtSubject}" is registered with namespace "https://ext-{run}.example.org/subject#"
    And I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"
    When I call the operation "deleteExternalVocabulary" with the query parameter "force=true"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "force"
    And the external vocabulary "{ExtSubject}" is registered
