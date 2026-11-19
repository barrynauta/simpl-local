Feature: Search vocabularies and external vocabularies
  POST /vocabularies/searches and POST /external-vocabulary/searches are
  read-only, not paged, limited to 100 references, and open to both roles.

  Background:
    Given I am authenticated as "ga-admin"

  @D38
  Scenario: Repeated references give one result; name and name with latest version give two
    Given a vocabulary "Alpha" exists with 2 versions
    When I send a POST request to "/vocabularies/searches" with JSON body
      """
      {"items": [
        {"name": "${Alpha}", "version": 2},
        {"name": "${Alpha}"},
        {"name": "${Alpha}", "version": 2},
        {"name": "${Alpha}"}
      ]}
      """
    Then the response status is 200
    And the response field "total" is "2"
    And the response field "items[0].latest" is "false"
    And the response field "items[0].version" is "2"
    And the response field "items[1].latest" is "true"
    And the response field "items[1].version" is "2"

  @D38 @D25 @D5 @D16 @D45
  Scenario Outline: An internal search returns results in request order with latest and versioned metadata
    Given a vocabulary "Alpha" exists with 2 versions
    And a vocabulary "Beta" exists
    And the vocabulary status of "Beta" is set to "DEPRECATED"
    And I am authenticated as "<user>"
    When I send a POST request to "/vocabularies/searches" with JSON body
      """
      {"items": [
        {"name": "${Beta}"},
        {"name": "${Alpha}", "version": 1},
        {"name": "${Ghost}"},
        {"name": "${Alpha}"}
      ]}
      """
    Then the response status is 200
    And the response items have the names "${Beta},${Alpha},${Alpha}"
    And the response field "items[0].latest" is "true"
    And the response field "items[0].version" is "1"
    And the response field "items[0].status" is "DEPRECATED"
    And the response field "items[0].href" is "/vocabularies/${Beta}"
    And the response field "items[0].contentHref" is "${contentBase}/content/${Beta}"
    And the response field "items[1].latest" is "false"
    And the response field "items[1].version" is "1"
    And the response field "items[1].status" is "DEPRECATED"
    And the response field "items[1].href" is "/vocabularies/${Alpha}/versions/1"
    And the response field "items[1].contentHref" is "${contentBase}/content/${Alpha}/versions/1"
    And the response field "items[2].latest" is "true"
    And the response field "items[2].version" is "2"
    And the response field "items[2].status" is "ACTIVE"
    And the response field "page" is "1"
    And the response field "pageSize" is "100"
    And the response field "total" is "3"
    And the response field "totalPages" is "1"
    And the link "self" is absent
    And the link "first" is absent
    And the link "next" is absent
    And the link "prev" is absent
    And the link "last" is absent

    Examples:
      | user      |
      | ga-admin  |
      | ga-viewer |

  @D38 @D54
  Scenario: A search with no match returns an empty result; names match exactly
    Given a vocabulary "Alpha" exists
    When I send a POST request to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "${Alpha.upper}"}, {"name": "${Alpha}", "version": 5}]}
      """
    Then the response status is 200
    And the response has no items
    And the response field "total" is "0"
    And the response field "totalPages" is "0"

  @D38 @Q2 @NFR-04
  Scenario: A search with 100 references is accepted
    When I send a POST request to "/vocabularies/searches" with a search for 100 distinct names
    Then the response status is 200
    And the response has no items

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario: A search with 101 references is rejected
    When I send a POST request to "/vocabularies/searches" with a search for 101 distinct names
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @D68 @D52 @negative
  Scenario Outline: An invalid internal search body is rejected with the decided issue name
    When I send a POST request to "/vocabularies/searches" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named one of "<name>"

    Examples:
      | body                                                        | name             |
      | {"items": []}                                               | items            |
      | {}                                                          | items            |
      | {"items": [{"name": "lower${uid}"}]}                        | items[0].name    |
      | {"items": [{"name": "${Alpha}"}, {"name": "Ab"}]}           | items[1].name    |
      | {"items": [{"name": "${Alpha}"}, {"name": "${Alpha}", "version": 0}]} | items[1].version |
      | {"items": [{"name": "${Alpha}", "version": "one"}]}         | items[0].version |
      | {"items": [{"name": "${Alpha}"}], "page": 1}                | page             |
      | {"items": [{"name": "${Alpha}", "latest": true}]}           | items[0].latest\|latest |

  @D38 @D52 @negative
  Scenario: Query parameters on a search are rejected
    When I send a POST request to "/vocabularies/searches?page=1" with JSON body
      """
      {"items": [{"name": "${Alpha}"}]}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "page"

  @D38 @D52 @negative
  Scenario: Query parameters on an external search are rejected
    When I send a POST request to "/external-vocabulary/searches?pageSize=10" with JSON body
      """
      {"namespaceUri": "http://ext.example.org/${uid}/terms"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "pageSize"

  @D15 @negative @API-38
  Scenario: A search that is not application/json is rejected with 415
    When I send a POST request to "/vocabularies/searches" with body "name=Alpha" and content type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D77 @negative
  Scenario: A method the fixed search path does not offer gives 405
    When I send a GET request to "/vocabularies/searches"
    Then the response is a problem with status 405

  # ---- external search -------------------------------------------------------

  @D38 @D30 @D73 @D16 @D41
  Scenario Outline: An external vocabulary is found by its namespace or alias, normalised
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms" and alias "https://alias.example.org/${uid}/terms/"
    And I am authenticated as "<user>"
    When I send a POST request to "/external-vocabulary/searches" with JSON body
      """
      <body>
      """
    Then the response status is 200
    And the response items have the names "${Terms}"
    And the response field "items[0].namespaceUri" is "http://ext.example.org/${uid}/terms"
    And the response field "items[0].aliasUris" is the list "https://alias.example.org/${uid}/terms/"
    And the response field "total" is "1"
    And the response field "totalPages" is "1"
    And the response field "pageSize" is "100"
    And the link "self" is absent

    Examples:
      | user      | body                                                              |
      | ga-admin  | {"namespaceUri": "http://ext.example.org/${uid}/terms#"}          |
      | ga-viewer | {"namespaceUri": "http://ext.example.org/${uid}/terms/"}          |
      | ga-admin  | {"aliasUri": "https://alias.example.org/${uid}/terms"}            |
      | ga-admin  | {"namespaceUris": ["http://ext.example.org/${uid}/terms"]}        |
      | ga-admin  | {"aliasUris": ["https://alias.example.org/${uid}/terms#"]}        |

  @D38
  Scenario: A batch external search returns each match once in the order of the first matching URI
    Given an external vocabulary "Second" is registered with namespace "http://second.example.org/${uid}/ns"
    And an external vocabulary "First" is registered with namespace "http://first.example.org/${uid}/ns" and alias "http://first-alias.example.org/${uid}/ns"
    When I send a POST request to "/external-vocabulary/searches" with JSON body
      """
      {"namespaceUris": ["http://unknown.example.org/${uid}", "http://first.example.org/${uid}/ns/", "http://second.example.org/${uid}/ns"],
       "aliasUris": ["http://first-alias.example.org/${uid}/ns"]}
      """
    Then the response status is 200
    And the response items have the names "${First},${Second}"
    And the response field "total" is "2"

  @D38
  Scenario: An external search without a match returns an empty result
    When I send a POST request to "/external-vocabulary/searches" with JSON body
      """
      {"namespaceUri": "http://nothing.example.org/${uid}"}
      """
    Then the response status is 200
    And the response has no items
    And the response field "total" is "0"
    And the response field "totalPages" is "0"

  @D38 @D68 @D73 @negative
  Scenario Outline: An external search body that fits no or several oneOf branches is rejected
    When I send a POST request to "/external-vocabulary/searches" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                                                         | name          |
      | {}                                                                           | body          |
      | {"namespaceUri": "http://a.example.org/x", "aliasUri": "http://b.example.org/x"} | body      |
      | {"namespaceUri": "http://a.example.org/x", "namespaceUris": ["http://b.example.org/x"]} | body |
      | {"namespaceUris": []}                                                        | namespaceUris |
      | {"aliasUris": []}                                                            | aliasUris     |
      | {"namespaceUri": "not-absolute"}                                             | namespaceUri  |
      | {"namespaceUri": "http://a.example.org/x", "name": "x"}                       | name          |

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario Outline: An external search holds at most 100 URIs in total
    When I send a POST request to "/external-vocabulary/searches" with <a> namespace URIs and <b> alias URIs
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<name>"

    Examples:
      | a   | b  | name          |
      | 101 | 0  | namespaceUris |
      | 0   | 101| aliasUris     |
      | 60  | 41 | aliasUris     |

  @D38 @Q2 @NFR-04
  Scenario: An external search with 100 URIs is accepted
    When I send a POST request to "/external-vocabulary/searches" with 60 namespace URIs and 40 alias URIs
    Then the response status is 200
