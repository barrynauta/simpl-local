Feature: Vocabulary search and external vocabulary search
  Read-only batch lookups for consumers such as Schema Manager (D38, D45). Neither search
  is paged; all results come in one response. Viewers may call them (D16).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"

  @D38 @D25 @D5
  Scenario: A search by name returns the latest version with the vocabulary status
    Given a vocabulary "{Alpha}" exists with 2 versions
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{Alpha}"}]}
      """
    Then the response status is 200
    And the response has 1 item
    And the response field "items[0].name" is "{Alpha}"
    And the response field "items[0].version" equals 2
    And the response field "items[0].status" is "DEPRECATED"
    And the response field "items[0].latest" equals true
    And the response field "items[0].href" is "/vocabularies/{Alpha}"
    And the response field "items[0].contentHref" ends with "/content/{Alpha}"

  @D38 @D26
  Scenario: A search by name and version returns that version with its own status
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{Alpha}", "version": 1}]}
      """
    Then the response status is 200
    And the response field "items[0].version" equals 1
    And the response field "items[0].status" is "DEPRECATED"
    And the response field "items[0].latest" equals false
    And the response field "items[0].href" is "/vocabularies/{Alpha}/versions/1"
    And the response field "items[0].contentHref" ends with "/content/{Alpha}/versions/1"

  @D38
  Scenario: Results keep the request order, leave out missing references and collapse repeats
    Given a vocabulary "{Alpha}" exists with 2 versions
    And a vocabulary "{Beta}" exists
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [
        {"name": "{Beta}"},
        {"name": "{Ghost}"},
        {"name": "{Alpha}", "version": 2},
        {"name": "{Alpha}", "version": 7},
        {"name": "{Beta}"},
        {"name": "{Alpha}"},
        {"name": "{Alpha}", "version": 2}
      ]}
      """
    Then the response status is 200
    And the response items have the "name" values ["{Beta}", "{Alpha}", "{Alpha}"]
    And the response items have the "latest" values [true, false, true]
    And the response items have the "version" values [1, 2, 2]
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 3
    And the response field "totalPages" equals 1
    And the response has no paging links

  @D38
  Scenario: A search without matches returns an empty result
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{Ghost}"}]}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D45 @D38 @D3
  Scenario: A consumer sees a deprecation through the search at use time
    Given a vocabulary "{Alpha}" exists
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{Alpha}", "version": 1}]}
      """
    Then the response field "items[0].status" is "ACTIVE"
    Given a new version of "{Alpha}" exists
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{Alpha}", "version": 1}]}
      """
    Then the response field "items[0].status" is "DEPRECATED"

  @D38 @D54
  Scenario: The search by name is exact
    Given a vocabulary "{Alpha}" exists
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{upper:Alpha}"}]}
      """
    Then the response status is 200
    And the response has 0 items

  @D38
  Scenario: A search with 100 items is allowed
    When I search vocabularies with 100 distinct names
    Then the response status is 200

  @D38 @negative
  Scenario: A search with more than 100 items is rejected
    When I search vocabularies with 101 distinct names
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "items"

  @D38 @D52 @negative @BLG-req-valid
  Scenario Outline: A malformed vocabulary search is rejected (<case>)
    When I send "POST" to "/vocabularies/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400

    Examples:
      | case                     | body                                            |
      | no items                 | {"items": []}                                   |
      | items missing            | {}                                              |
      | unknown top property     | {"items": [{"name": "{Alpha}"}], "page": 1}     |
      | unknown item property    | {"items": [{"name": "{Alpha}", "latest": true}]} |
      | invalid name             | {"items": [{"name": "alpha"}]}                  |
      | version zero             | {"items": [{"name": "{Alpha}", "version": 0}]}  |
      | version not an integer   | {"items": [{"name": "{Alpha}", "version": "1"}]} |
      | not JSON                 | items=Alpha                                     |

  @D38 @D52 @negative
  Scenario: Paging parameters on a search are rejected
    When I send "POST" to "/vocabularies/searches?page=1&pageSize=100" with JSON
      """
      {"items": [{"name": "{Alpha}"}]}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "page"

  @D15 @negative @API-38
  Scenario: A vocabulary search that is not JSON gets 415
    When I send "POST" to "/vocabularies/searches" with the body "name={Alpha}" as "text/plain"
    Then the response is a problem with status 415

  @D38 @D30
  Scenario Outline: An external vocabulary is found by namespace or alias, normalised (<case>)
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms/" and alias "https://alias-{run}.example.org/terms#"
    When I send "POST" to "/external-vocabulary/searches" with JSON
      """
      <body>
      """
    Then the response status is 200
    And the response items have the "name" values ["{Ext}"]
    And the response field "items[0].href" is "/external-vocabularies/{Ext}"

    Examples:
      | case                         | body                                                             |
      | namespace as registered      | {"namespaceUri": "https://ext-{run}.example.org/terms/"}         |
      | namespace without slash      | {"namespaceUri": "https://ext-{run}.example.org/terms"}          |
      | namespace with hash          | {"namespaceUri": "https://ext-{run}.example.org/terms#"}         |
      | alias without hash           | {"aliasUri": "https://alias-{run}.example.org/terms"}            |
      | batch by namespace           | {"namespaceUris": ["https://ext-{run}.example.org/terms"]}       |
      | batch by alias               | {"aliasUris": ["https://alias-{run}.example.org/terms/"]}        |

  @D38 @D30
  Scenario: A batch external search orders by first match and lists each vocabulary once
    Given an external vocabulary "{ExtA}" is registered with namespace "https://a-{run}.example.org/ns" and alias "https://a-alias-{run}.example.org/ns"
    And an external vocabulary "{ExtB}" is registered with namespace "https://b-{run}.example.org/ns"
    When I send "POST" to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUris": ["https://b-{run}.example.org/ns#", "https://unknown-{run}.example.org/ns", "https://a-{run}.example.org/ns/"],
       "aliasUris": ["https://a-alias-{run}.example.org/ns"]}
      """
    Then the response status is 200
    And the response items have the "name" values ["{ExtB}", "{ExtA}"]
    And the response field "total" equals 2
    And the response field "totalPages" equals 1
    And the response has no paging links

  @D38
  Scenario: An external search without a match returns an empty result
    When I send "POST" to "/external-vocabulary/searches" with JSON
      """
      {"namespaceUri": "https://nothing-{run}.example.org/ns"}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "totalPages" equals 0

  @D38 @negative
  Scenario Outline: An external search body that fits no single oneOf branch is rejected (<case>)
    When I send "POST" to "/external-vocabulary/searches" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400

    Examples:
      | case                         | body                                                                                     |
      | empty object                 | {}                                                                                       |
      | namespace and alias          | {"namespaceUri": "https://x-{run}.example.org/a", "aliasUri": "https://x-{run}.example.org/b"} |
      | namespace and namespaces     | {"namespaceUri": "https://x-{run}.example.org/a", "namespaceUris": ["https://x-{run}.example.org/b"]} |
      | empty namespace array        | {"namespaceUris": []}                                                                    |
      | empty alias array            | {"aliasUris": []}                                                                        |
      | unknown property             | {"namespaceUri": "https://x-{run}.example.org/a", "page": 1}                             |

  @D38 @negative
  Scenario Outline: An external search with more than 100 URIs is rejected (<n> + <m>)
    When I search external vocabularies with <n> namespace URIs and <m> alias URIs
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"

    Examples:
      | n   | m  | field         |
      | 101 | 0  | namespaceUris |
      | 0   | 101| aliasUris     |

  @D38 @negative
  Scenario: An external search with 60 namespace URIs and 41 alias URIs is over the total limit
    When I search external vocabularies with 60 namespace URIs and 41 alias URIs
    Then the response is a problem with status 400

  @D38
  Scenario: An external search with 100 URIs in total is allowed
    When I search external vocabularies with 60 namespace URIs and 40 alias URIs
    Then the response status is 200

  @D29 @D30 @D41
  Scenario Outline: External content is downloaded by namespace or alias (<case>)
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms/" and alias "https://alias-{run}.example.org/terms"
    When I send "POST" to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "<uri>"}
      """
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response header "Content-Disposition" contains "{Ext}.ttl"
    And the response body is the content of the external vocabulary "{Ext}"

    Examples:
      | case                | uri                                      |
      | namespace           | https://ext-{run}.example.org/terms/     |
      | namespace with hash | https://ext-{run}.example.org/terms#     |
      | alias               | https://alias-{run}.example.org/terms/   |

  @D41 @negative
  Scenario: External content for an unknown namespace gives 404
    When I send "POST" to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "https://nothing-{run}.example.org/ns"}
      """
    Then the response is a problem with status 404

  @D52 @negative
  Scenario: An unknown property in the external content request is rejected
    When I send "POST" to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "https://nothing-{run}.example.org/ns", "format": "ttl"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "format"
