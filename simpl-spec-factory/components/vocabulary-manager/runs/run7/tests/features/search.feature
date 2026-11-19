Feature: Search operations for consumers
  POST /vocabularies/searches and POST /external-vocabulary/searches are read-only and not paged
  (D38, D68, D54, D16, D45).

  @D38 @D5 @D25 @D45
  Scenario: The internal search resolves latest and versioned references in request order
    Given vocabulary "Alpha" has 2 versions
    And vocabulary "Beta" exists
    When "ga-viewer" searches vocabularies with the items
      | name  | version |
      | Beta  |         |
      | Alpha | 1       |
      | Alpha |         |
    Then the response status is 200
    And the items
      | name  | version | status     | latest | href                                 | contentHref                                |
      | Beta  | 1       | ACTIVE     | true   | /vocabularies/${V:Beta}              | ${CB}/content/${V:Beta}                    |
      | Alpha | 1       | DEPRECATED | false  | /vocabularies/${V:Alpha}/versions/1  | ${CB}/content/${V:Alpha}/versions/1        |
      | Alpha | 2       | ACTIVE     | true   | /vocabularies/${V:Alpha}             | ${CB}/content/${V:Alpha}                   |
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 3
    And the response field "totalPages" equals 1
    And the links "self, first, prev, next, last" are absent

  @D38
  Scenario: Missing names and versions are left out and repeated references give one result
    Given vocabulary "Alpha" has 2 versions
    When "ga-admin" searches vocabularies with the items
      | name    | version |
      | Ghost   |         |
      | Alpha   | 2       |
      | Alpha   | 9       |
      | Alpha   |         |
      | Alpha   | 2       |
      | Alpha   |         |
    Then the response status is 200
    And the items
      | name  | version | latest |
      | Alpha | 2       | false  |
      | Alpha | 2       | true   |
    And the response field "total" equals 2

  @D38 @D3 @D25
  Scenario: A latest result carries the vocabulary status, a versioned result the version status
    Given vocabulary "Alpha" exists
    And vocabulary "Alpha" has status "DEPRECATED"
    When "ga-admin" searches vocabularies with the items
      | name  | version |
      | Alpha |         |
      | Alpha | 1       |
    Then the items
      | name  | version | status     | latest |
      | Alpha | 1       | DEPRECATED | true   |
      | Alpha | 1       | ACTIVE     | false  |

  @D38 @D54
  Scenario: The search by name is exact and no match gives an empty result
    Given vocabulary "Alpha" exists
    When "ga-admin" searches vocabularies with the items
      | name        | version |
      | ${VU:Alpha} |         |
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D38 @Q2 @NFR-04
  Scenario: The internal search accepts 100 items
    Given vocabulary "Alpha" exists
    When "ga-admin" searches vocabularies with 100 items for "Alpha"
    Then the response status is 200
    And the response has 1 item

  @D38 @Q2 @NFR-04 @negative
  Scenario: The internal search rejects 101 items
    When "ga-admin" searches vocabularies with 101 items for "Alpha"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @D68 @negative
  Scenario Outline: An invalid internal search body is rejected (<case>)
    When "ga-admin" sends POST "/vocabularies/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                      | body                                                    | name             |
      | no items                  | {"items": []}                                           | items            |
      | items missing             | {}                                                      | items            |
      | invalid name              | {"items": [{"name": "lower"}]}                          | items[0].name    |
      | invalid second name       | {"items": [{"name": "Valid${rid}"}, {"name": "a-b"}]}   | items[1].name    |
      | version zero              | {"items": [{"name": "Valid${rid}", "version": 0}]}      | items[0].version |
      | unknown top property      | {"items": [{"name": "Valid${rid}"}], "page": 1}         | page             |

  @D38 @D52 @negative
  Scenario Outline: A query parameter on a search is rejected (<query>)
    When "ga-admin" sends POST "/vocabularies/searches?<query>" with JSON
      """
      {"items": [{"name": "Valid${rid}"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"

    Examples:
      | query       | name     |
      | page=1      | page     |
      | pageSize=50 | pageSize |
      | sort=name   | sort     |

  @D15 @negative @API-38
  Scenario: A search that is not application/json is rejected with 415
    When "ga-admin" sends POST "/vocabularies/searches" with the body "name=Alpha" of type "application/x-www-form-urlencoded"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D38 @D30 @D73
  Scenario: The external search finds by namespace URI or alias URI, without trailing '#' or '/'
    Given external vocabulary "Dc" is registered with the fields
      | field     | value                               |
      | aliasUris | http://alias.example.org/${rid}/dc  |
    When "ga-viewer" sends POST "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "${NS:Dc}/"}
      """
    Then the response status is 200
    And the external item names are "Dc"
    And the response field "items[0].namespaceUri" equals "${NS:Dc}"
    And the response field "items[0].aliasUris" equals ["http://alias.example.org/${rid}/dc"]
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 1
    And the response field "totalPages" equals 1
    And the links "self, first, prev, next, last" are absent
    When "ga-viewer" sends POST "/external-vocabulary/searches" with JSON
      """
      {"aliasUri": "http://alias.example.org/${rid}/dc#"}
      """
    Then the response status is 200
    And the external item names are "Dc"

  @D38
  Scenario: The batch external search orders by the first matching URI and lists each vocabulary once
    Given external vocabulary "One" is registered
    And external vocabulary "Two" is registered with the fields
      | field     | value                                |
      | aliasUris | http://alias.example.org/${rid}/two  |
    When "ga-admin" sends POST "/external-vocabulary/searches" with JSON
      """
      {"namespaceUris": ["http://nothing.example.org/${rid}", "${NS:Two}", "${NS:One}#", "${NS:Two}/"],
       "aliasUris": ["${NS:One}"]}
      """
    Then the response status is 200
    And the external item names are "Two, One"
    And the response field "total" equals 2
    When "ga-admin" sends POST "/external-vocabulary/searches" with JSON
      """
      {"namespaceUris": ["${NS:One}"], "aliasUris": ["http://alias.example.org/${rid}/two/"]}
      """
    Then the external item names are "One, Two"

  @D38
  Scenario: An external search with no match returns an empty result
    When "ga-admin" sends POST "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "http://nothing.example.org/${rid}/ns"}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D38 @Q2 @NFR-04
  Scenario: The external search accepts 100 URIs in total
    When "ga-admin" searches external vocabularies with 50 namespace URIs and 50 alias URIs
    Then the response status is 200

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario Outline: The external search rejects more than 100 URIs (<a> and <b>)
    When "ga-admin" searches external vocabularies with <a> namespace URIs and <b> alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | a   | b  | name          |
      | 101 | 0  | namespaceUris |
      | 0   | 101 | aliasUris    |
      | 51  | 50 | aliasUris     |

  @D38 @D68 @negative
  Scenario Outline: An external search body that fits no or several oneOf branches is rejected (<case>)
    When "ga-admin" sends POST "/external-vocabulary/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                         | body                                                                              | name          |
      | empty object                 | {}                                                                                | body          |
      | namespaceUri and aliasUri    | {"namespaceUri": "http://a.example.org/x", "aliasUri": "http://b.example.org/y"}  | body          |
      | namespaceUri and the batch   | {"namespaceUri": "http://a.example.org/x", "namespaceUris": ["http://b.example.org/y"]} | body    |

  @D38 @D52 @negative
  Scenario: An unknown property inside a search item is rejected
    # D52 fixes in: body; D68 does not fix the name for an unknown item property.
    When "ga-admin" sends POST "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "Valid${rid}", "latest": true}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named one of "latest, items[0].latest"

  @D38 @D68 @negative
  Scenario Outline: An empty URI array in an external search is rejected (<case>)
    # Either reading of D38 and D68 gives 400 in: body; the issue name is left open.
    When "ga-admin" sends POST "/external-vocabulary/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body"

    Examples:
      | case                  | body                                                            |
      | empty namespace array | {"namespaceUris": []}                                           |
      | empty alias array     | {"namespaceUris": ["http://a.example.org/x"], "aliasUris": []}  |
