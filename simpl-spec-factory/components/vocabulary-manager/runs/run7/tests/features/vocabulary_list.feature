Feature: Vocabulary list with filters, sorting and paging via the API
  GET /vocabularies (SIMPL-18162, SIMPL-18164, D19 to D23, D58, D59, D78).
  The store is shared, so every scenario filters on its own run id (E4).

  @AC_SIMPL-18162_AC1 @D16 @D22 @D25 @BLG-col-paging
  Scenario: The list returns a page of vocabularies with their metadata
    Given vocabularies "Alpha, Beta" exist
    When "ga-viewer" lists vocabularies with the query "name=${rid}"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "total" equals 2
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the response field "totalPages" equals 1
    And the item names are "Alpha, Beta"
    And every item has the fields "name, version, description, status, creationDate, lastUpdateDate, createdBy, href, contentHref"
    And the response field "items[0].href" equals "/vocabularies/${V:Alpha}"
    And the response field "items[0].contentHref" equals "${CB}/content/${V:Alpha}"
    And the response field "items[0].version" equals 1
    And the response field "items[0].status" equals "ACTIVE"
    And the link "self" points to page 1 with the request query
    And the link "first" points to page 1 with the request query
    And the link "last" points to page 1 with the request query
    And the links "prev, next" are absent

  @AC_SIMPL-18162_AC2 @D22 @D21 @negative
  Scenario Outline: Wrong paging parameters are rejected (<query>)
    When "ga-admin" lists vocabularies with the query "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"
    And the response field "detail" is present
    And the response field "items" is absent

    Examples:
      | query        | name     |
      | page=0       | page     |
      | page=-1      | page     |
      | page=abc     | page     |
      | page=1.5     | page     |
      | pageSize=0   | pageSize |
      | pageSize=101 | pageSize |
      | pageSize=x   | pageSize |

  @D22 @Q2 @NFR-04
  Scenario: A page size of 100 is accepted
    Given vocabulary "Alpha" exists
    When "ga-admin" lists vocabularies with the query "name=${rid}&pageSize=100"
    Then the response status is 200
    And the response field "pageSize" equals 100

  @AC_SIMPL-18164_AC3 @D22 @D23 @D59 @D78 @BLG-col-paging
  Scenario: Paging works together with sorting and keeps the order
    Given vocabularies "Alpha, Beta, Gamma" exist
    When "ga-admin" lists vocabularies with the query "name=${rid}&sort=-name&pageSize=2"
    Then the response status is 200
    And the item names are "Gamma, Beta"
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the link "self" points to page 1 with the request query
    And the link "first" points to page 1 with the request query
    And the link "next" points to page 2 with the request query
    And the link "last" points to page 2 with the request query
    And the link "prev" is absent
    When "ga-admin" lists vocabularies with the query "name=${rid}&sort=-name&pageSize=2&page=2"
    Then the item names are "Alpha"
    And the link "prev" points to page 1 with the request query
    And the link "next" is absent

  @D22 @D59 @D78
  Scenario: A page beyond the last returns an empty page with the right totals
    Given vocabularies "Alpha, Beta, Gamma" exist
    When "ga-admin" lists vocabularies with the query "name=${rid}&pageSize=2&page=5"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the link "prev" points to page 2 with the request query
    And the link "next" is absent

  @AC_SIMPL-18164_AC2 @D22
  Scenario: A filter that matches nothing returns an empty result without error
    When "ga-admin" lists vocabularies with the query "name=${rid}"
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0
    And the link "self" points to page 1 with the request query
    And the link "first" points to page 1 with the request query
    And the links "prev, next, last" are absent

  @AC_SIMPL-18164_AC1 @D19
  Scenario: The name filter is a case-insensitive contains match
    Given vocabularies "Alpha, Beta" exist
    When "ga-admin" lists vocabularies with the query "name=${RID}alp"
    Then the response status is 200
    And the item names are "Alpha"
    When "ga-admin" lists vocabularies with the query "name=${rid}"
    Then the item names are "Alpha, Beta"

  @D19 @negative
  Scenario Outline: A name filter outside the relaxed rule is rejected (<case>)
    When "ga-admin" lists vocabularies with the query "name=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "name"

    Examples:
      | case             | value     |
      | empty            |           |
      | hyphen           | a-b       |
      | 65 characters    | ${R:a:65} |

  @D19
  Scenario: A one-character lower-case name filter is accepted
    When "ga-admin" lists vocabularies with the query "name=q&pageSize=1"
    Then the response status is 200

  @AC_SIMPL-18164_AC1 @D3 @D23
  Scenario: The status filter uses the vocabulary status
    Given vocabularies "Alpha, Beta" exist
    And a new version of "Alpha" was created
    And vocabulary "Beta" has status "DEPRECATED"
    When "ga-admin" lists vocabularies with the query "name=${rid}&status=DEPRECATED"
    Then the item names are "Beta"
    When "ga-admin" lists vocabularies with the query "name=${rid}&status=ACTIVE"
    Then the item names are "Alpha"

  @AC_SIMPL-18164_AC1 @D20 @D58
  Scenario: The creation date filter matches a whole UTC day or an exact second
    Given vocabulary "Alpha" exists
    And I save the field "creationDate" of vocabulary "Alpha" as "created"
    When "ga-admin" lists vocabularies with the query "name=${rid}&creationDate=${SD:created}"
    Then the item names are "Alpha"
    When "ga-admin" lists vocabularies with the query "name=${rid}&creationDate=${S:created}"
    Then the item names are "Alpha"
    When "ga-admin" lists vocabularies with the query "name=${rid}&creationDate=2001-01-01"
    Then the response has 0 items

  @AC_SIMPL-18164_AC1 @D20 @D25
  Scenario: The last update filter matches a whole UTC day or an exact second
    Given vocabulary "Alpha" exists
    And a new version of "Alpha" was created
    And I save the field "lastUpdateDate" of vocabulary "Alpha" as "updated"
    When "ga-admin" lists vocabularies with the query "name=${rid}&lastUpdated=${SD:updated}"
    Then the item names are "Alpha"
    When "ga-admin" lists vocabularies with the query "name=${rid}&lastUpdated=${S:updated}"
    Then the item names are "Alpha"
    When "ga-admin" lists vocabularies with the query "name=${rid}&lastUpdated=2001-01-01T00:00:00Z"
    Then the response has 0 items

  @AC_SIMPL-18164_AC1 @D23 @D58
  Scenario: The default order is name ascending by code point
    Given vocabularies "Ab, AZ, Ba" exist
    When "ga-admin" lists vocabularies with the query "name=${rid}"
    Then the item names are "AZ, Ab, Ba"
    When "ga-admin" lists vocabularies with the query "name=${rid}&sort=name"
    Then the item names are "AZ, Ab, Ba"
    When "ga-admin" lists vocabularies with the query "name=${rid}&sort=-name"
    Then the item names are "Ba, Ab, AZ"

  @AC_SIMPL-18164_AC1 @D23
  Scenario Outline: The list sorts on <sort> with ties broken by name ascending
    Given vocabularies "Alpha, Beta, Gamma" exist, created one second apart
    And one second passes
    And vocabulary "Beta" has status "DEPRECATED"
    And one second passes
    And a new version of "Alpha" was created
    When "ga-admin" lists vocabularies with the query "name=${rid}&sort=<sort>"
    Then the response status is 200
    And the item names are "<order>"

    Examples:
      | sort          | order              |
      | status        | Alpha, Gamma, Beta |
      | -status       | Beta, Alpha, Gamma |
      | creationDate  | Alpha, Beta, Gamma |
      | -creationDate | Gamma, Beta, Alpha |
      | lastUpdated   | Gamma, Beta, Alpha |
      | -lastUpdated  | Alpha, Beta, Gamma |

  @AC_SIMPL-18164_AC4 @D20 @D21 @D23 @negative
  Scenario Outline: An unsupported or invalid filter or sort is rejected (<query>)
    When "ga-admin" lists vocabularies with the query "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"
    And the response field "items" is absent

    Examples:
      | query                        | name         |
      | sort=description             | sort         |
      | sort=version                 | sort         |
      | status=RETIRED               | status       |
      | status=active                | status       |
      | creationDate=2026-02-30      | creationDate |
      | creationDate=yesterday       | creationDate |
      | lastUpdated=2026-13-01       | lastUpdated  |
      | description=x                | description  |
      | createdBy=ga-admin           | createdBy    |

  @D21 @D52 @negative
  Scenario: Exactly one issue names an unknown query parameter
    When "ga-admin" lists vocabularies with the query "colour=blue"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "colour"

  @D52 @negative
  Scenario: A query parameter given twice is rejected
    When "ga-admin" lists vocabularies with the query "page=1&page=2"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "page"

  @D21 @negative
  Scenario: An unknown query parameter on a GET of one vocabulary is rejected
    Given vocabulary "Alpha" exists
    When "ga-admin" sends GET "/vocabularies/${V:Alpha}?expand=versions"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "expand"

  @D49 @negative
  Scenario Outline: A vocabulary name in a path that breaks the name rules gives 400 (<case>)
    When "ga-admin" sends GET "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | type                                                     | in   | name           |
      | urn:problem-type:belgif:input-validation:schemaViolation | path | vocabularyName |

    Examples:
      | case                 | path                               |
      | lower-case start     | /vocabularies/lower${rid}          |
      | too short            | /vocabularies/Ab                   |
      | too long             | /vocabularies/${NAME:65}           |
      | versions path        | /vocabularies/lower${rid}/versions |
      | content path         | /content/lower${rid}               |

  @D54 @D24 @negative
  Scenario: Paths match the stored name exactly
    Given vocabulary "Alpha" exists
    When "ga-admin" sends GET "/vocabularies/${VU:Alpha}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${VU:Alpha}' was not found."
