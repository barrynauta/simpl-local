@search
Feature: Search vocabularies and external vocabularies
  The two search operations are read-only lookups that are not paged (D38). Consumers such as
  Schema Manager read the status through them each time (D45).

  Background:
    Given I use the token of "ga-admin"

  # ---------------------------------------------------------------- internal search

  @D38 @D45 @D5 @D25
  Scenario: References resolve in request order, missing ones are left out, and nothing is paged
    Given a vocabulary "A" exists with 3 versions
    And a vocabulary "B" exists with 2 versions
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${A}"}, {"name": "${Missing}"}, {"name": "${B}", "version": 1}, {"name": "${A}", "version": 9}]}
      """
    Then the status is 200
    And the media type is "application/json"
    And the search results are "A, B@1"
    And the response field "items[0].version" equals 3
    And the response field "items[0].status" is "ACTIVE"
    And the response field "items[0].href" is "/vocabularies/${A}"
    And the response field "items[0].contentHref" is "${base}/content/${A}"
    And the response field "items[1].status" is "DEPRECATED"
    And the response field "items[1].href" is "/vocabularies/${B}/versions/1"
    And the response field "items[1].contentHref" is "${base}/content/${B}/versions/1"
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 2
    And the response field "totalPages" equals 1
    And the response has no paging links

  @D38
  Scenario: A latest reference carries the vocabulary status; a version reference carries the version status
    Given a vocabulary "A" exists with 2 versions
    And vocabulary "A" has the status "DEPRECATED"
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${A}"}, {"name": "${A}", "version": 2}]}
      """
    Then the status is 200
    And the search results are "A, A@2"
    And the response field "items[0].status" is "DEPRECATED"
    And the response field "items[0].version" equals 2
    And the response field "items[1].status" is "ACTIVE"

  @D38
  Scenario: A repeated reference gives one result at its first position
    Given a vocabulary "A" exists with 2 versions
    And a vocabulary "B" exists
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${B}"}, {"name": "${A}", "version": 1}, {"name": "${B}"}, {"name": "${A}", "version": 1}]}
      """
    Then the status is 200
    And the search results are "B, A@1"
    And the response field "total" equals 2

  @D38
  Scenario: A name reference and a reference to its latest version are two results
    Given a vocabulary "A" exists with 2 versions
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${A}", "version": 2}, {"name": "${A}"}]}
      """
    Then the search results are "A@2, A"

  @D38 @D54
  Scenario: Search by name is exact, also in case
    Given a vocabulary "Case" exists
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${upper:Case}"}]}
      """
    Then the status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D38 @Q2 @NFR-04
  Scenario: 100 references are at the limit and are accepted
    Given a vocabulary "A" exists with 2 versions
    When I search vocabularies with 100 references to "A"
    Then the status is 200
    And the search results are "A@1, A@2"

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario: 101 references are above the limit
    When I search vocabularies with 101 references to "A"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @negative
  Scenario Outline: An invalid search body is rejected: <case>
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                     | body                                                         | name             |
      | empty items              | {"items": []}                                                | items            |
      | invalid name             | {"items": [{"name": "lower"}]}                               | items[0].name    |
      | name too short           | {"items": [{"name": "${A}"}, {"name": "Ab"}]}                | items[1].name    |
      | version zero             | {"items": [{"name": "${A}"}, {"name": "${A}", "version": 0}]} | items[1].version |
      | version as text          | {"items": [{"name": "${A}", "version": "1"}]}                | items[0].version |
      | version with fraction    | {"items": [{"name": "${A}", "version": 1.5}]}                | items[0].version |
      | version with exponent    | {"items": [{"name": "${A}", "version": 1e1}]}                | items[0].version |
      | version above int32      | {"items": [{"name": "${A}", "version": 2147483648}]}         | items[0].version |
      | missing name             | {"items": [{"version": 1}]}                                  | items[0].name    |

  @D38 @D52 @negative
  Scenario Outline: An unknown property in a search body is rejected: <case>
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case               | body                                              | name   |
      | top-level property | {"items": [{"name": "${A}"}], "page": 1}          | page   |

  @D38 @negative
  Scenario: A search body without items is rejected
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @D52 @negative
  Scenario: An unknown property in a search item is rejected
    When I send "POST" to "/vocabularies/searches" with the JSON body
      """
      {"items": [{"name": "${A}", "status": "ACTIVE"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body"

  @D38 @D52 @D21 @negative
  Scenario Outline: A query parameter on the search gives 400: <query>
    When I send "POST" to "/vocabularies/searches?<query>" with the JSON body
      """
      {"items": [{"name": "${A}"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query      | param    |
      | page=1     | page     |
      | pageSize=5 | pageSize |
      | foo=bar    | foo      |

  @D15 @API-38 @negative
  Scenario: A search that is not JSON gives 415
    When I send "POST" to "/vocabularies/searches" with content type "text/plain" and the body
      """
      items
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D110 @NFR-04 @negative
  Scenario: A search body over 1 MB gives 413
    When I send "POST" to "/vocabularies/searches" with a JSON body of 1100000 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D77 @negative
  Scenario: GET on the search path gives 405, the fixed segment is never read as a name
    When I send "GET" to "/vocabularies/searches"
    Then the response is a problem with status 405

  # ---------------------------------------------------------------- external search

  @D38 @D30 @D41
  Scenario: Search an external vocabulary by namespace URI and by alias URI
    Given an external vocabulary "E" is registered with namespace "${extns:e}" and aliases "${extns:ealias}"
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "${extns:e}"}
      """
    Then the status is 200
    And the item names are "E"
    And the response field "items[0].namespaceUri" is "${extns:e}"
    And the response field "items[0].aliasUris[0]" is "${extns:ealias}"
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 1
    And the response field "totalPages" equals 1
    And the response has no paging links
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"aliasUri": "${extns:ealias}"}
      """
    Then the status is 200
    And the item names are "E"

  @D38 @D30
  Scenario Outline: External search normalises a trailing '#' or '/': <uri>
    Given an external vocabulary "E" is registered with namespace "${extns:e}/"
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "<uri>"}
      """
    Then the status is 200
    And the item names are "E"

    Examples:
      | uri           |
      | ${extns:e}    |
      | ${extns:e}#   |
      | ${extns:e}/   |

  @D38
  Scenario: Batch external search orders results by the first matching URI, namespaces before aliases, each once
    Given an external vocabulary "E" is registered with namespace "${extns:e}" and aliases "${extns:ealias}"
    And an external vocabulary "F" is registered with namespace "${extns:f}" and aliases "${extns:falias}"
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUris": ["${extns:none}", "${extns:f}"], "aliasUris": ["${extns:ealias}", "${extns:falias}"]}
      """
    Then the status is 200
    And the item names are "F, E"
    And the response field "total" equals 2
    And the response has no paging links

  @D38
  Scenario: An external search without a match gives an empty result
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "${extns:nothing}"}
      """
    Then the status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D38 @D68 @negative
  Scenario Outline: An external search body that fits no or several oneOf branches gives 400 named body: <case>
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "body"

    Examples:
      | case                         | body                                                               |
      | empty object                 | {}                                                                 |
      | namespaceUri with aliasUri   | {"namespaceUri": "https://a.example/x", "aliasUri": "https://b.example/y"} |
      | namespaceUri with an array   | {"namespaceUri": "https://a.example/x", "namespaceUris": ["https://b.example/y"]} |
      | aliasUri with an array       | {"aliasUri": "https://a.example/x", "aliasUris": ["https://b.example/y"]} |

  @D38 @D68 @D52 @negative
  Scenario: An unknown property in an external search body is rejected
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "https://a.example/x", "name": "X"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body"

  @D38 @negative
  Scenario Outline: An empty URI array is rejected: <array>
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"<array>": []}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body"

    Examples:
      | array         |
      | namespaceUris |
      | aliasUris     |

  @D38 @D68 @Q2 @NFR-04
  Scenario Outline: <ns> namespace URIs and <aliases> alias URIs are at the limit of 100
    When I search external vocabularies with <ns> namespace URIs and <aliases> alias URIs
    Then the status is 200

    Examples:
      | ns  | aliases |
      | 100 | 0       |
      | 0   | 100     |
      | 60  | 40      |

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario Outline: <ns> namespace URIs and <aliases> alias URIs are above the limit; the issue is named <name>
    When I search external vocabularies with <ns> namespace URIs and <aliases> alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | ns  | aliases | name          |
      | 101 | 0       | namespaceUris |
      | 0   | 101     | aliasUris     |
      | 60  | 41      | aliasUris     |

  @D73 @D110 @negative
  Scenario Outline: A URI that is not absolute is rejected: <case>
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                 | body                                                         | name             |
      | relative namespace   | {"namespaceUri": "dc/terms"}                                 | namespaceUri     |
      | relative alias       | {"aliasUri": "terms"}                                        | aliasUri         |
      | bad array element    | {"namespaceUris": ["https://a.example/x", "not a uri"]}      | namespaceUris[1] |

  @D52 @negative
  Scenario: A query parameter on the external search gives 400
    When I send "POST" to "/external-vocabulary/searches?page=1" with the JSON body
      """
      {"namespaceUri": "https://a.example/x"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "page"

  @D15 @API-38 @negative
  Scenario: An external search that is not JSON gives 415
    When I send "POST" to "/external-vocabulary/searches" with content type "application/xml" and the body
      """
      <namespaceUri>https://a.example/x</namespaceUri>
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
