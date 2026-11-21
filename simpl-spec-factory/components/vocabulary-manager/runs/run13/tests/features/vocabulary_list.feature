Feature: List, filter, sort and page vocabularies
  GET /vocabularies shows the vocabulary with values from its latest version (D25). Name filter
  D19, date filters D20 and D130, paging D22, D59, D78, D110, D111, sorting D23, D58, D128.
  Strict query checks D21, D91, D112, D138. Each scenario filters on its own token (E4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18162_AC1 @D25 @D22 @D110 @D111 @BLG-col-paging
  Scenario: The list is paged and every item carries the vocabulary metadata
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies?name={tok}"
    Then the response status is 200
    And the response media type is "application/json"
    And the page fields are page 1, pageSize 20, total 1, totalPages 1
    And the response has the links "self, first, last" and no others
    And the link "self" is "/vocabularies?page=1&pageSize=20&name={tok}"
    And the link "first" is "/vocabularies?page=1&pageSize=20&name={tok}"
    And the link "last" is "/vocabularies?page=1&pageSize=20&name={tok}"
    And the response JSON has
      | field                   | value                   |
      | items[0].href           | /vocabularies/{Alpha}   |
      | items[0].contentHref    | {base}/content/{Alpha}  |
      | items[0].name           | {Alpha}                 |
      | items[0].version        | 1                       |
      | items[0].description    | Description of {Alpha}. |
      | items[0].status         | ACTIVE                  |
      | items[0].createdBy      | {admin_email}           |
      | items[0].creationDate   | <date-time>             |
      | items[0].lastUpdateDate | <date-time>             |

  @AC_SIMPL-18162_AC1 @AC_SIMPL-18176_AC1 @D16 @D165
  Scenario: The viewer gets the same list, creator included
    Given a vocabulary "Alpha" exists
    And I am authenticated as "ga-viewer"
    When I send GET "/vocabularies?name={tok}"
    Then the response status is 200
    And the response lists the vocabularies "{Alpha}" in this order
    And the response JSON has
      | field              | value         |
      | items[0].createdBy | {admin_email} |

  @D25 @D3
  Scenario: A list item shows the latest version number and the vocabulary status
    Given a vocabulary "Alpha" exists with 3 versions
    And the vocabulary status of "Alpha" is "DEPRECATED"
    When I send GET "/vocabularies?name={tok}"
    Then the response JSON has
      | field            | value      |
      | items[0].version | 3          |
      | items[0].status  | DEPRECATED |
    And version 3 of "Alpha" has the version status "ACTIVE"

  @AC_SIMPL-18164_AC1 @D19
  Scenario Outline: The name filter is a case-insensitive contains match: <case>
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    When I send GET "/vocabularies?name=<filter>"
    Then the response status is 200
    And the response lists the vocabularies "<expected>"

    Examples:
      | case               | filter              | expected         |
      | token only         | {tok}               | {Alpha}, {Bravo} |
      | lower case token   | {tok:lower}         | {Alpha}, {Bravo} |
      | full name          | {Alpha}             | {Alpha}          |
      | full name in lower | {Alpha:lower}       | {Alpha}          |
      | inner part         | ravo{tok}           | {Bravo}          |

  @D19 @Q2 @NFR-04
  Scenario Outline: The name filter accepts <case>
    When I send GET "/vocabularies?name=<filter>"
    Then the response status is 200

    Examples:
      | case                 | filter                                                           |
      | one character        | q                                                                |
      | 64 characters        | Abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij1234 |
      | lower case and digits | abc123                                                          |

  @AC_SIMPL-18164_AC4 @D19 @D21 @Q2 @NFR-04 @negative
  Scenario Outline: The name filter rejects <case>
    When I send GET "/vocabularies?name=<filter>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "name"
    And the response JSON has
      | field | value    |
      | items | <absent> |

    Examples:
      | case             | filter                                                            |
      | 65 characters    | Abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij12345 |
      | an empty value   |                                                                   |
      | a hyphen         | ab-cd                                                             |
      | a space          | ab%20cd                                                           |

  @AC_SIMPL-18164_AC1 @D3
  Scenario: The status filter uses the vocabulary status
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And the vocabulary status of "Bravo" is "DEPRECATED"
    When I send GET "/vocabularies?name={tok}&status=DEPRECATED"
    Then the response lists the vocabularies "{Bravo}" in this order
    When I send GET "/vocabularies?name={tok}&status=ACTIVE"
    Then the response lists the vocabularies "{Alpha}" in this order

  @AC_SIMPL-18164_AC1 @D20 @D58 @D110
  Scenario Outline: The creationDate filter matches <case>
    Given a vocabulary "Alpha" exists
    And I remember the field "creationDate" of the vocabulary "Alpha" as "created"
    When I send GET "/vocabularies?name={tok}&creationDate=<value>"
    Then the response status is 200
    And the response lists the vocabularies "{Alpha}" in this order

    Examples:
      | case                                   | value                 |
      | the UTC day                            | {$created:date}       |
      | the date-time to the second            | {$created}            |
      | a date-time without offset, read as UTC | {$created:nooffset}  |
      | the same instant with offset +01:00    | {$created:offset1h}   |

  @AC_SIMPL-18164_AC2 @D20
  Scenario Outline: The creationDate filter excludes <case>
    Given a vocabulary "Alpha" exists
    And I remember the field "creationDate" of the vocabulary "Alpha" as "created"
    When I send GET "/vocabularies?name={tok}&creationDate=<value>"
    Then the response status is 200
    And the response lists no items

    Examples:
      | case                | value              |
      | one second later    | {$created:plus1s}  |
      | the next day        | {$created:plus1d}  |

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: The lastUpdated filter matches the last update date
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And I wait one second
    And a new version of "Alpha" exists
    And I remember the field "lastUpdateDate" of the vocabulary "Alpha" as "updated"
    When I send GET "/vocabularies?name={tok}&lastUpdated={$updated}"
    Then the response lists the vocabularies "{Alpha}" in this order
    When I send GET "/vocabularies?name={tok}&lastUpdated={$updated:date}"
    Then the response includes the vocabulary "{Alpha}"

  @AC_SIMPL-18164_AC4 @D20 @D130 @D21 @D116 @negative
  Scenario Outline: An invalid date filter is rejected: <case>
    When I send GET "/vocabularies?name={tok}&<parameter>=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | case                       | parameter    | value                     |
      | not a date                 | creationDate | yesterday                 |
      | 30 February                | creationDate | 2024-02-30                |
      | month 13                   | lastUpdated  | 2024-13-01                |
      | hour 25                    | creationDate | 2024-03-01T25:00:00Z      |
      | offset beyond 18:00        | creationDate | 2024-03-01T00:00:00%2B19:00 |
      | offset beyond minus 18:00  | lastUpdated  | 2024-03-01T00:00:00-18:30 |

  @D130
  Scenario: An offset of 18:00 is accepted
    When I send GET "/vocabularies?name={tok}&creationDate=2024-03-01T00:00:00%2B18:00"
    Then the response status is 200
    And the response lists no items

  @AC_SIMPL-18164_AC2 @D22 @D59 @D111
  Scenario: A filter that matches nothing returns an empty page without error
    When I send GET "/vocabularies?name=Nothing{tok}"
    Then the response status is 200
    And the response lists no items
    And the page fields are page 1, pageSize 20, total 0, totalPages 0
    And the response has the links "self, first" and no others
    And the link "self" is "/vocabularies?page=1&pageSize=20&name=Nothing{tok}"

  @AC_SIMPL-18164_AC1 @D23 @D128 @D58 @BLG-col-sort
  Scenario Outline: The list sorts by <sort>
    Given a vocabulary "Bravo" exists
    And I wait one second
    And a vocabulary "Alpha" exists
    And I wait one second
    And a vocabulary "Charlie" exists
    And I wait one second
    And the vocabulary status of "Charlie" is "DEPRECATED"
    And I wait one second
    And a new version of "Alpha" exists
    When I send GET "/vocabularies?name={tok}<sort param>"
    Then the response status is 200
    And the response lists the vocabularies "<order>" in this order

    Examples:
      | sort          | sort param          | order                         |
      | default       |                     | {Alpha}, {Bravo}, {Charlie}   |
      | name          | &sort=name          | {Alpha}, {Bravo}, {Charlie}   |
      | -name         | &sort=-name         | {Charlie}, {Bravo}, {Alpha}   |
      | status        | &sort=status        | {Alpha}, {Bravo}, {Charlie}   |
      | -status       | &sort=-status       | {Charlie}, {Alpha}, {Bravo}   |
      | creationDate  | &sort=creationDate  | {Bravo}, {Alpha}, {Charlie}   |
      | -creationDate | &sort=-creationDate | {Charlie}, {Alpha}, {Bravo}   |
      | lastUpdated   | &sort=lastUpdated   | {Bravo}, {Charlie}, {Alpha}   |
      | -lastUpdated  | &sort=-lastUpdated  | {Alpha}, {Charlie}, {Bravo}   |

  @AC_SIMPL-18164_AC1 @D23
  Scenario Outline: Ties are broken by name ascending when sorting by <sort>
    Given a vocabulary "Bravo" exists
    And a vocabulary "Alpha" exists
    And a vocabulary "Charlie" exists
    When I send GET "/vocabularies?name={tok}&sort=<sort>"
    Then the response status is 200
    And the vocabulary items are sorted by "<sort>"

    Examples:
      | sort          |
      | status        |
      | -status       |
      | creationDate  |
      | -creationDate |
      | lastUpdated   |
      | -lastUpdated  |

  @D58
  Scenario: The name sort compares by code point
    Given a vocabulary "Ab" exists
    And a vocabulary "AZ" exists
    When I send GET "/vocabularies?name={tok}&sort=name"
    Then the response lists the vocabularies "{AZ}, {Ab}" in this order

  @AC_SIMPL-18164_AC3 @D22 @D78 @D110 @D111 @BLG-col-paging
  Scenario: Paging keeps the sort order and every link keeps the request
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send GET "/vocabularies?sort=-name&name={tok}&page=1&pageSize=2"
    Then the response lists the vocabularies "{Charlie}, {Bravo}" in this order
    And the page fields are page 1, pageSize 2, total 3, totalPages 2
    And the response has the links "self, first, next, last" and no others
    And the link "self" is "/vocabularies?page=1&pageSize=2&sort=-name&name={tok}"
    And the link "first" is "/vocabularies?page=1&pageSize=2&sort=-name&name={tok}"
    And the link "next" is "/vocabularies?page=2&pageSize=2&sort=-name&name={tok}"
    And the link "last" is "/vocabularies?page=2&pageSize=2&sort=-name&name={tok}"
    When I send GET "/vocabularies?sort=-name&name={tok}&page=2&pageSize=2"
    Then the response lists the vocabularies "{Alpha}" in this order
    And the page fields are page 2, pageSize 2, total 3, totalPages 2
    And the response has the links "self, first, prev, last" and no others
    And the link "prev" is "/vocabularies?page=1&pageSize=2&sort=-name&name={tok}"

  @D22 @D59
  Scenario: A page beyond the last page is empty and its prev link points to the last page
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    And a vocabulary "Charlie" exists
    When I send GET "/vocabularies?name={tok}&pageSize=2&page=5"
    Then the response status is 200
    And the response lists no items
    And the page fields are page 5, pageSize 2, total 3, totalPages 2
    And the response has the links "self, first, prev, last" and no others
    And the link "prev" is "/vocabularies?page=2&pageSize=2&name={tok}"

  @D22 @D91 @Q2 @NFR-04
  Scenario Outline: Paging accepts <case>
    When I send GET "/vocabularies?name={tok}&<query>"
    Then the response status is 200

    Examples:
      | case                 | query           |
      | pageSize 100         | pageSize=100    |
      | pageSize 1           | pageSize=1      |
      | page 1               | page=1          |
      | the largest page     | page=2147483647 |

  @AC_SIMPL-18162_AC2 @AC_SIMPL-18164_AC4 @D22 @D91 @D112 @Q2 @NFR-04 @negative
  Scenario Outline: Wrong paging is rejected: <case>
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies?name={tok}&<parameter>=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"
    And the response JSON has
      | field | value    |
      | items | <absent> |

    Examples:
      | case                  | parameter | value      |
      | pageSize above 100    | pageSize  | 101        |
      | pageSize 0            | pageSize  | 0          |
      | negative pageSize     | pageSize  | -1         |
      | page 0                | page      | 0          |
      | page not a number     | page      | abc        |
      | page with a fraction  | page      | 2.0        |
      | page with an exponent | page      | 1e1        |
      | page with a plus sign | page      | %2B2       |
      | page with a space     | page      | %202       |
      | page above int32      | page      | 2147483648 |
      | empty pageSize        | pageSize  |            |

  @AC_SIMPL-18164_AC4 @D21 @D23 @negative
  Scenario Outline: An unsupported sort or filter value is rejected: <case>
    When I send GET "/vocabularies?name={tok}&<parameter>=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | case                  | parameter | value       |
      | unknown sort field    | sort      | description |
      | sort on createdBy     | sort      | -createdBy  |
      | status not in the set | status    | ARCHIVED    |
      | status in lower case  | status    | active      |

  @AC_SIMPL-18164_AC4 @D21 @negative @BLG-req-valid
  Scenario: An unknown query parameter is rejected with one issue
    When I send GET "/vocabularies?name={tok}&description=healthcare"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "query" named "description"
    And the response JSON has
      | field | value    |
      | items | <absent> |

  @D138 @D52 @negative
  Scenario Outline: A query parameter given twice is rejected with one issue: <parameter>
    When I send GET "/vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | parameter | query                          |
      | page      | name={tok}&page=1&page=2       |
      | name      | name={tok}&name=other          |
      | sort      | name={tok}&sort=name&sort=name |
