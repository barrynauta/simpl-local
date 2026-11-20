Feature: Search vocabularies and external vocabularies
  The two read-only searches are not paged; every reference matches at most one result (D38, D68, D137).

  Background:
    Given I am a vocabulary viewer

  @D38 @D25 @D16 @D45
  Scenario: Internal search returns results in request order and leaves out missing references
    Given a vocabulary "Alpha" exists
    And vocabulary "Alpha" has a new version
    And a vocabulary "Bravo" exists
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      {"items": [
        {"name": "${Bravo}"},
        {"name": "${Missing}"},
        {"name": "${Alpha}", "version": 1},
        {"name": "${Alpha}", "version": 9},
        {"name": "${Alpha}"}
      ]}
      """
    Then the response status is 200
    And the response content type is JSON
    And the item names are "${Bravo}, ${Alpha}, ${Alpha}"
    And the item versions are "1, 1, 2"
    And the response field "items[0].latest" is true
    And the response field "items[0].href" is "/vocabularies/${Bravo}"
    And the response field "items[0].contentHref" is "${CB}/content/${Bravo}"
    And the response field "items[1].latest" is false
    And the response field "items[1].status" is "DEPRECATED"
    And the response field "items[1].href" is "/vocabularies/${Alpha}/versions/1"
    And the response field "items[1].contentHref" is "${CB}/content/${Alpha}/versions/1"
    And the response field "items[2].latest" is true
    And the response field "items[2].status" is "ACTIVE"
    And the response field "page" is 1
    And the response field "pageSize" is 100
    And the response field "total" is 3
    And the response field "totalPages" is 1
    And the response field "self" is absent
    And the response field "first" is absent
    And the response field "next" is absent
    And the response field "prev" is absent
    And the response field "last" is absent

  @D38 @D3 @D25
  Scenario: A latest result carries the vocabulary status, a version result its own status
    Given a vocabulary "A" exists
    And the status of vocabulary "A" is changed to "DEPRECATED"
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${A}"}, {"name": "${A}", "version": 1}]}
      """
    Then the response status is 200
    And the item statuses are "DEPRECATED, ACTIVE"
    And the response field "items[0].latest" is true
    And the response field "items[1].latest" is false

  @D38
  Scenario: A repeated reference gives one result at its first position
    Given a vocabulary "Alpha" exists
    And a vocabulary "Bravo" exists
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      {"items": [
        {"name": "${Alpha}", "version": 1},
        {"name": "${Bravo}"},
        {"name": "${Alpha}", "version": 1},
        {"name": "${Bravo}"}
      ]}
      """
    Then the response status is 200
    And the item names are "${Alpha}, ${Bravo}"
    And the response field "total" is 2

  @D38 @D54
  Scenario: A search with no match returns an empty result, and names are matched exactly
    Given a vocabulary "A" exists
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${upper:A}"}, {"name": "${Missing}"}]}
      """
    Then the response status is 200
    And the response has no items
    And the response field "total" is 0
    And the response field "totalPages" is 0

  @D38 @Q2 @NFR-04
  Scenario: A search with 100 references is accepted
    When I search vocabularies with 100 references to unknown names
    Then the response status is 200
    And the response has no items

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario: A search with 101 references is rejected
    When I search vocabularies with 101 references to unknown names
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @D68 @negative
  Scenario Outline: An invalid internal search body is rejected: <body>
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                                        | name             |
      | {"items": []}                                               | items            |
      | {}                                                          | items            |
      | {"items": [{"name": "lowercase"}]}                          | items[0].name    |
      | {"items": [{"name": "${A}"}, {"name": "${A}", "version": 0}]} | items[1].version |
      | {"items": [{"name": "${A}", "version": "one"}]}             | items[0].version |
      | {"items": [{"version": 1}]}                                 | items[0].name    |
      | {"items": [{"name": "${A}"}], "page": 1}                    | page             |

  @D38 @D52 @negative
  Scenario Outline: A query parameter on a search is rejected: <query>
    When I send a POST request to "/vocabularies/searches?<query>" with JSON
      """
      {"items": [{"name": "${A}"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query      | parameter |
      | page=1     | page      |
      | pageSize=5 | pageSize  |
      | sort=name  | sort      |

  @D15 @negative @API-38
  Scenario: An internal search that is not application/json gets 415
    When I send a POST request to "/vocabularies/searches" with content type "text/plain" and body
      """
      {"items": [{"name": "${A}"}]}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D38 @D30 @D16
  Scenario: External search by namespace URI, alias URI and batch, with normalised URIs
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}/" and aliases "${URI:DcAlias}"
    And an external vocabulary "Schema" is registered with namespace "${URI:Schema}"
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "${URI:Dc}"}
      """
    Then the response status is 200
    And the item names are "${Dc}"
    And the response field "items[0].namespaceUri" is "${URI:Dc}/"
    And the response field "items[0].aliasUris" lists "${URI:DcAlias}"
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"aliasUri": "${URI:DcAlias}#"}
      """
    Then the response status is 200
    And the item names are "${Dc}"
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUris": ["${URI:Schema}/", "${URI:Unknown}", "${URI:Dc}#"], "aliasUris": ["${URI:DcAlias}"]}
      """
    Then the response status is 200
    And the item names are "${Schema}, ${Dc}"
    And the response field "page" is 1
    And the response field "pageSize" is 100
    And the response field "total" is 2
    And the response field "totalPages" is 1
    And the response field "self" is absent
    And the response field "next" is absent

  @D38
  Scenario: External search by alias URIs follows the order of the first matching URI
    Given an external vocabulary "One" is registered with namespace "${URI:One}" and aliases "${URI:OneAlias}"
    And an external vocabulary "Two" is registered with namespace "${URI:Two}" and aliases "${URI:TwoAlias}"
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"aliasUris": ["${URI:TwoAlias}", "${URI:OneAlias}", "${URI:TwoAlias}/"]}
      """
    Then the response status is 200
    And the item names are "${Two}, ${One}"
    And the response field "total" is 2

  @D38
  Scenario: External search with no match returns an empty result
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "${URI:Nothing}"}
      """
    Then the response status is 200
    And the response has no items
    And the response field "total" is 0
    And the response field "totalPages" is 0

  @D38 @D68 @D137 @negative
  Scenario Outline: An external search body that matches no or several criteria is rejected: <body>
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                                                      | name          |
      | {}                                                                        | body          |
      | {"namespaceUri": "${URI:A}", "aliasUri": "${URI:B}"}                      | body          |
      | {"namespaceUri": "${URI:A}", "namespaceUris": ["${URI:B}"]}               | body          |
      | {"aliasUri": "${URI:A}", "aliasUris": ["${URI:B}"]}                       | body          |
      | {"namespaceUris": []}                                                     | namespaceUris |
      | {"aliasUris": []}                                                         | aliasUris     |

  @D38 @D52 @negative
  Scenario: An unknown property in an external search is rejected
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "${URI:A}", "colour": "blue"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D38 @Q2 @NFR-04
  Scenario Outline: An external search with 100 URIs in total is accepted: <n> and <m>
    When I search external vocabularies with <n> namespace URIs and <m> alias URIs
    Then the response status is 200
    And the response has no items

    Examples:
      | n   | m  |
      | 100 | 0  |
      | 0   | 100|
      | 60  | 40 |

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario Outline: An external search with more than 100 URIs is rejected: <n> and <m>
    When I search external vocabularies with <n> namespace URIs and <m> alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | n   | m   | name          |
      | 101 | 0   | namespaceUris |
      | 0   | 101 | aliasUris     |
      | 60  | 41  | aliasUris     |

  @D38 @D52 @negative
  Scenario: A query parameter on an external search is rejected
    When I send a POST request to "/external-vocabulary/searches?pageSize=10" with JSON
      """
      {"namespaceUri": "${URI:A}"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "pageSize"

  @D15 @negative @API-38
  Scenario: An external search that is not application/json gets 415
    When I send a POST request to "/external-vocabulary/searches" with content type "application/xml" and body
      """
      <namespaceUri>x</namespaceUri>
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D110 @D38 @negative
  Scenario: A bad element of an external search array is named with its index
    When I send a POST request to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUris": ["${URI:A}", 5]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "namespaceUris[1]"

  @D38 @D5
  Scenario: A reference with and without the latest version number gives two results
    Given a vocabulary "A" exists
    When I send a POST request to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "${A}"}, {"name": "${A}", "version": 1}]}
      """
    Then the response status is 200
    And the item names are "${A}, ${A}"
    And the item versions are "1, 1"
    And the response field "items[0].latest" is true
    And the response field "items[1].latest" is false
    And the response field "total" is 2
