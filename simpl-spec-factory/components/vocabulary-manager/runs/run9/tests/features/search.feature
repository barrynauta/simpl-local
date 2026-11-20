Feature: Search vocabularies and external vocabularies
  Read-only batch lookups for consumers (D38, D45, D68). Neither search is paged.

  @D38 @D25 @D26 @D45
  Scenario: Internal search returns latest and versioned results in request order
    Given a vocabulary "Health" exists with 2 versions
    And a vocabulary "Legacy" exists
    And the vocabulary "Legacy" was set to "DEPRECATED"
    When I send a "POST" request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${Legacy}", "version": 1}, {"name": "${Health}"}, {"name": "${Health}", "version": 1}]}
      """
    Then the response status is 200
    And the items are, in order
      | name      | version | latest | status     | href                                 |
      | ${Legacy} | 1       | false  | ACTIVE     | /vocabularies/${Legacy}/versions/1   |
      | ${Health} | 2       | true   | ACTIVE     | /vocabularies/${Health}              |
      | ${Health} | 1       | false  | DEPRECATED | /vocabularies/${Health}/versions/1   |
    And the page fields are page 1, pageSize 100, total 3 and totalPages 1
    And the response has no paging links
    And the response field "items[1].contentHref" is "${contentBase}/content/${Health}"
    And the response field "items[2].contentHref" is "${contentBase}/content/${Health}/versions/1"

  @D38 @D3
  Scenario: A latest result carries the vocabulary status
    Given a vocabulary "Legacy" exists
    And the vocabulary "Legacy" was set to "DEPRECATED"
    When I send a "POST" request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${Legacy}"}]}
      """
    Then the items are, in order
      | name      | version | latest | status     |
      | ${Legacy} | 1       | true   | DEPRECATED |

  @D38 @D54
  Scenario: Missing names and versions are left out, repeated references give one result, name match is exact
    Given a vocabulary "Health" exists
    When I send a "POST" request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${Ghost}"}, {"name": "${Health}", "version": 5}, {"name": "${Health}"}, {"name": "${upper:Health}"}, {"name": "${Health}"}, {"name": "${Health}", "version": 1}, {"name": "${Health}", "version": 1}]}
      """
    Then the response status is 200
    And the items are, in order
      | name      | version | latest |
      | ${Health} | 1       | true   |
      | ${Health} | 1       | false  |
    And the page fields are page 1, pageSize 100, total 2 and totalPages 1

  @D38
  Scenario: A search with no match returns an empty result with totalPages 0
    When I send a "POST" request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${Ghost}"}]}
      """
    Then the response status is 200
    And the response field "items" is an empty list
    And the page fields are page 1, pageSize 100, total 0 and totalPages 0
    And the response has no paging links

  @D38 @Q2 @NFR-04
  Scenario: An internal search with 100 references is accepted
    When I search vocabularies with 100 references
    Then the response status is 200
    And the response field "total" is 0

  @D38 @D68 @Q2 @negative @NFR-04
  Scenario: An internal search with 101 references is rejected
    When I search vocabularies with 101 references
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @D68 @negative
  Scenario Outline: An invalid internal search body is rejected (<case>)
    When I send a "POST" request to "/vocabularies/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                    | body                                                      | name             |
      | empty items             | {"items": []}                                             | items            |
      | no items                | {}                                                        | items            |
      | invalid name            | {"items": [{"name": "Valid${run}"}, {"name": "bad-name"}]} | items[1].name    |
      | version 0               | {"items": [{"name": "Valid${run}", "version": 0}]}        | items[0].version |
      | version text            | {"items": [{"name": "Valid${run}", "version": "one"}]}    | items[0].version |
      | version too large       | {"items": [{"name": "Valid${run}", "version": 2147483648}]} | items[0].version |
      | unknown top property    | {"items": [{"name": "Valid${run}"}], "page": 1}           | page             |

  @D38 @D52 @negative
  Scenario Outline: Query parameters on a search are rejected (<path>)
    When I send a "POST" request to "<path>?page=1" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "page"

    Examples:
      | path                           | body                                           |
      | /vocabularies/searches         | {"items": [{"name": "Valid${run}"}]}           |
      | /external-vocabulary/searches  | {"namespaceUri": "https://none.example.org/x"} |

  @D15 @negative @API-38
  Scenario Outline: A search body that is not application/json is rejected with 415 (<path>)
    When I send a "POST" request to "<path>" with content type "application/x-www-form-urlencoded" and body
      """
      name=Abc
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

    Examples:
      | path                            |
      | /vocabularies/searches          |
      | /external-vocabulary/searches   |
      | /external-vocabularies/content  |

  @D38 @D30 @D73
  Scenario: External search by namespace URI, normalised
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}/" and alias "${extns:DcAlias}#"
    When I send a "POST" request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "${extns:Dc}"}
      """
    Then the response status is 200
    And the items are, in order
      | name       | namespaceUri  | href                                  |
      | ${ext:Dc}  | ${extns:Dc}/  | /external-vocabularies/${ext:Dc}      |
    And the page fields are page 1, pageSize 100, total 1 and totalPages 1
    And the response has no paging links

  @D38 @D30
  Scenario: External search by one alias URI
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}/" and alias "${extns:DcAlias}#"
    When I send a "POST" request to "/external-vocabulary/searches" with JSON
      """
      {"aliasUri": "${extns:DcAlias}/"}
      """
    Then the items are, in order
      | name      |
      | ${ext:Dc} |

  @D38
  Scenario: Batch external search gives each vocabulary once, in the order of the first matching URI
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#" and alias "${extns:DcAlias}#"
    And an external vocabulary "Schema" is registered with namespace "${extns:Schema}/"
    When I send a "POST" request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUris": ["${extns:Nothing}", "${extns:Schema}", "${extns:Dc}"], "aliasUris": ["${extns:DcAlias}"]}
      """
    Then the items are named, in order
      | name          |
      | ${ext:Schema} |
      | ${ext:Dc}     |
    And the page fields are page 1, pageSize 100, total 2 and totalPages 1

  @D38
  Scenario: Namespace URIs come before alias URIs in the order
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#" and alias "${extns:DcAlias}#"
    And an external vocabulary "Schema" is registered with namespace "${extns:Schema}/"
    When I send a "POST" request to "/external-vocabulary/searches" with JSON
      """
      {"aliasUris": ["${extns:DcAlias}"], "namespaceUris": ["${extns:Schema}"]}
      """
    Then the items are named, in order
      | name          |
      | ${ext:Schema} |
      | ${ext:Dc}     |

  @D38
  Scenario: An external search with no match returns an empty result
    When I send a "POST" request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "${extns:Nothing}"}
      """
    Then the response status is 200
    And the response field "items" is an empty list
    And the page fields are page 1, pageSize 100, total 0 and totalPages 0

  @D38 @Q2 @NFR-04
  Scenario: An external search with 100 URIs in total is accepted
    When I search external vocabularies with 60 namespace URIs and 40 alias URIs
    Then the response status is 200

  @D38 @D68 @Q2 @negative @NFR-04
  Scenario Outline: An external search with more than 100 URIs is rejected (<a> + <b>)
    When I search external vocabularies with <a> namespace URIs and <b> alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | a   | b  | name          |
      | 101 | 0  | namespaceUris |
      | 0   | 101 | aliasUris    |
      | 60  | 41 | aliasUris     |

  @D38 @D68 @negative @BLG-req-valid
  Scenario Outline: An external search body that fits no or several oneOf branches is rejected (<case>)
    When I send a "POST" request to "/external-vocabulary/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                         | body                                                                                    | name          |
      | empty object                 | {}                                                                                      | body          |
      | namespaceUri and aliasUri    | {"namespaceUri": "https://a.example.org/x", "aliasUri": "https://a.example.org/y"}      | body          |
      | namespaceUri and the batch   | {"namespaceUri": "https://a.example.org/x", "namespaceUris": ["https://a.example.org/y"]} | body        |
      | empty namespaceUris          | {"namespaceUris": []}                                                                   | namespaceUris |
      | empty aliasUris              | {"namespaceUris": ["https://a.example.org/x"], "aliasUris": []}                         | aliasUris     |

  @D16 @D38
  Scenario: A viewer may search
    Given a vocabulary "Health" exists
    And I am "ga-viewer"
    When I send a "POST" request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${Health}"}]}
      """
    Then the response status is 200
    And the response has 1 items
