Feature: Search vocabularies by name and version
  POST /vocabularies/searches is a read-only lookup, not paged, 1 to 100 items, results in
  request order (D38, D68, D153). Consumers read the current status through it (D45).

  Background:
    Given I am authenticated as "ga-admin"

  @D38 @D25 @D45 @D153
  Scenario: Latest and versioned references come back in request order
    Given a vocabulary "Alpha" exists with 3 versions
    And a vocabulary "Bravo" exists with 2 versions
    When I search for the vocabularies
      | name    | version |
      | {Bravo} | 1       |
      | {Alpha} |         |
    Then the response status is 200
    And the search result has the items
      | name    | version | status     | latest | href                             | contentHref                       |
      | {Bravo} | 1       | DEPRECATED | false  | /vocabularies/{Bravo}/versions/1 | {base}/content/{Bravo}/versions/1 |
      | {Alpha} | 3       | ACTIVE     | true   | /vocabularies/{Alpha}            | {base}/content/{Alpha}            |
    And the page fields are page 1, pageSize 100, total 2, totalPages 1
    And the response has the links "none" and no others

  @D38 @D3 @D45
  Scenario: A latest result carries the vocabulary status, a versioned result the version status
    Given a vocabulary "Alpha" exists
    And the vocabulary status of "Alpha" is "DEPRECATED"
    When I search for the vocabularies
      | name    | version |
      | {Alpha} |         |
      | {Alpha} | 1       |
    Then the search result has the items
      | name    | version | status     | latest |
      | {Alpha} | 1       | DEPRECATED | true   |
      | {Alpha} | 1       | ACTIVE     | false  |

  @D38
  Scenario: Missing names and versions are left out
    Given a vocabulary "Alpha" exists
    When I search for the vocabularies
      | name    | version |
      | {Ghost} |         |
      | {Alpha} | 7       |
      | {Alpha} | 1       |
    Then the search result has the items
      | name    | version | latest |
      | {Alpha} | 1       | false  |
    And the page fields are page 1, pageSize 100, total 1, totalPages 1

  @D38 @D153
  Scenario: A search that finds nothing has total 0 and totalPages 0
    When I search for the vocabularies
      | name    | version |
      | {Ghost} |         |
    Then the response status is 200
    And the response lists no items
    And the page fields are page 1, pageSize 100, total 0, totalPages 0
    And the response has the links "none" and no others

  @D38
  Scenario: A repeated reference gives one result at its first position
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    When I search for the vocabularies
      | name    | version |
      | {Alpha} |         |
      | {Bravo} | 1       |
      | {Alpha} |         |
      | {Bravo} | 1       |
    Then the search result has the items
      | name    | latest |
      | {Alpha} | true   |
      | {Bravo} | false  |

  @D38
  Scenario: A reference with and without the latest version number are two results
    Given a vocabulary "Alpha" exists with 2 versions
    When I search for the vocabularies
      | name    | version |
      | {Alpha} | 2       |
      | {Alpha} |         |
    Then the search result has the items
      | name    | version | latest |
      | {Alpha} | 2       | false  |
      | {Alpha} | 2       | true   |

  @D54
  Scenario: Search by name is exact
    Given a vocabulary "Alpha" exists
    When I search for the vocabularies
      | name          | version |
      | {Alpha:upper} |         |
    Then the response status is 200
    And the response lists no items

  @D38 @D16 @AC_SIMPL-18176_AC1
  Scenario: The viewer may search
    Given a vocabulary "Alpha" exists
    And I am authenticated as "ga-viewer"
    When I search for the vocabularies
      | name    | version |
      | {Alpha} |         |
    Then the response status is 200
    And the response has 1 items

  @D38 @Q2 @NFR-04
  Scenario: A search with 100 references is accepted
    Given a vocabulary "Alpha" exists
    When I search for 100 references to the vocabulary "Alpha"
    Then the response status is 200
    And the response has 2 items

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario: A search with 101 references is rejected
    Given a vocabulary "Alpha" exists
    When I search for 101 references to the vocabulary "Alpha"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @negative
  Scenario Outline: An invalid search request is rejected: <case>
    When I send POST "/vocabularies/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                            | body                                                        | name             |
      | no items                        | {"items": []}                                               | items            |
      | invalid name in the first item  | {"items": [{"name": "lower{tok}"}]}                         | items[0].name    |
      | version 0 in the second item    | {"items": [{"name": "A{tok}"}, {"name": "B{tok}", "version": 0}]} | items[1].version |
      | version as a string             | {"items": [{"name": "A{tok}", "version": "1"}]}             | items[0].version |
      | version with a fraction         | {"items": [{"name": "A{tok}", "version": 1.5}]}             | items[0].version |
      | version with an exponent        | {"items": [{"name": "A{tok}", "version": 1e0}]}             | items[0].version |
      | version above int32             | {"items": [{"name": "A{tok}", "version": 2147483648}]}      | items[0].version |

  @D38 @D52 @negative
  Scenario Outline: A search request without items or with unknown properties is rejected: <case>
    When I send POST "/vocabularies/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has some issue in "body"

    Examples:
      | case                        | body                                            |
      | an empty object             | {}                                              |
      | an unknown property         | {"items": [{"name": "A{tok}"}], "page": 1}      |
      | an unknown property in item | {"items": [{"name": "A{tok}", "latest": true}]} |

  @D38 @D21 @D52 @negative
  Scenario: A query parameter on the search is rejected
    When I send POST "/vocabularies/searches?page=1" with the JSON body
      """
      {"items": [{"name": "A{tok}"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "page"

  @D15 @negative @API-38
  Scenario: A search that is not JSON gets 415
    When I send POST "/vocabularies/searches" with the body "name=A" of type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D150 @Q2 @NFR-04
  Scenario: A JSON body of exactly 1 MB is accepted
    Given a vocabulary "Alpha" exists
    When I send POST "/vocabularies/searches" with a JSON body of exactly 1048576 bytes
    Then the response status is 200

  @D150 @D110 @Q2 @NFR-04 @negative
  Scenario: A JSON body over 1 MB gets 413
    When I send POST "/vocabularies/searches" with a JSON body of exactly 1048577 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the problem detail is "The request body is larger than 1 MB."
