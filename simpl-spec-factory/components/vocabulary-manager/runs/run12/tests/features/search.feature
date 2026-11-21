Feature: Search vocabularies and external vocabularies
  POST /vocabularies/searches and POST /external-vocabulary/searches are read-only, not paged (D38, D68, D137, D153).

  Background:
    Given I am authenticated as "ga-admin"

  @D38 @D153 @D25 @D45
  Scenario: Results come in request order, missing references are left out
    Given a vocabulary "{{Alpha}}" exists
    And vocabulary "{{Alpha}}" has 1 more versions
    And a vocabulary "{{Beta}}" exists
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "{{Beta}}"}, {"name": "{{Missing}}"}, {"name": "{{Alpha}}", "version": 1}, {"name": "{{Alpha}}", "version": 7}]}
      """
    Then the status is 200
    And the item names are "{{Beta}}, {{Alpha}}"
    And the response field "items.0.latest" equals true
    And the response field "items.0.href" equals "/vocabularies/{{Beta}}"
    And the response field "items.0.contentHref" is the contentBase followed by "/content/{{Beta}}"
    And the response field "items.1.latest" equals false
    And the response field "items.1.version" equals 1
    And the response field "items.1.status" equals "DEPRECATED"
    And the response field "items.1.href" equals "/vocabularies/{{Alpha}}/versions/1"
    And the response field "items.1.contentHref" is the contentBase followed by "/content/{{Alpha}}/versions/1"
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "total" equals 2
    And the response field "totalPages" equals 1
    And the response field "self" is absent
    And the response field "first" is absent
    And the response field "next" is absent
    And the response field "last" is absent

  @D38 @D3 @D5
  Scenario: A latest result carries the latest version number and the vocabulary status
    Given a vocabulary "{{Alpha}}" exists
    And vocabulary "{{Alpha}}" has 2 more versions
    And vocabulary "{{Alpha}}" has status "DEPRECATED"
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "{{Alpha}}"}, {"name": "{{Alpha}}", "version": 3}]}
      """
    Then the response field "total" equals 2
    And the response field "items.0.latest" equals true
    And the response field "items.0.version" equals 3
    And the response field "items.0.status" equals "DEPRECATED"
    And the response field "items.1.latest" equals false
    And the response field "items.1.version" equals 3
    And the response field "items.1.status" equals "ACTIVE"

  @D38
  Scenario: A repeated reference gives one result at its first position
    Given a vocabulary "{{Alpha}}" exists
    And a vocabulary "{{Beta}}" exists
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "{{Alpha}}"}, {"name": "{{Beta}}", "version": 1}, {"name": "{{Alpha}}"}, {"name": "{{Beta}}", "version": 1}]}
      """
    Then the item names are "{{Alpha}}, {{Beta}}"

  @D38 @D153
  Scenario: No match gives an empty result with totalPages 0
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "{{Missing}}"}]}
      """
    Then the status is 200
    And the response field "items" has 0 items
    And the response field "total" equals 0
    And the response field "totalPages" equals 0

  @D54
  Scenario: The search by name is exact
    Given a vocabulary "{{Alpha}}" exists
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "{{upper:Alpha}}"}]}
      """
    Then the response field "items" has 0 items

  @D38 @NFR-04 @Q2
  Scenario: 100 references are accepted
    When I send "POST" to "/vocabularies/searches" with 100 references
    Then the status is 200

  @D38 @D68 @NFR-04 @Q2 @negative
  Scenario: 101 references are rejected
    When I send "POST" to "/vocabularies/searches" with 101 references
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @D68 @negative
  Scenario Outline: An invalid search body is rejected (<case>)
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case               | body                                              | name             |
      | empty items        | {"items": []}                                     | items            |
      | invalid name       | {"items": [{"name": "bad-name"}]}                 | items[0].name    |
      | second item name   | {"items": [{"name": "Good"}, {"name": "x"}]}      | items[1].name    |
      | version zero       | {"items": [{"name": "Good", "version": 0}]}       | items[0].version |
      | version fraction   | {"items": [{"name": "Good", "version": 1.5}]}     | items[0].version |

  @D38 @D52 @OWA-API3 @negative
  Scenario Outline: An unknown property in the search body is rejected (<case>)
    When I send "POST" to "/vocabularies/searches" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue

    Examples:
      | case              | body                                          |
      | unknown in item   | {"items": [{"name": "Good", "colour": "x"}]}  |
      | unknown top level | {"items": [{"name": "Good"}], "colour": "x"}  |

  @D38 @D52 @negative
  Scenario Outline: A query parameter on the search is rejected (<param>)
    When I send "POST" to "/vocabularies/searches?<param>=1" with JSON body
      """
      {"items": [{"name": "Good"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "<param>"

    Examples:
      | param    |
      | page     |
      | pageSize |
      | sort     |

  @D15 @negative @API-38
  Scenario: A search that is not application/json gets 415
    When I send "POST" to "/vocabularies/searches" with body "items=Good" and content type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D150 @NFR-04 @negative
  Scenario: A search body over 1 MB gives 413
    When I send "POST" to "/vocabularies/searches" with a JSON body of 1048577 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the problem detail is "The request body is larger than 1 MB."

  # ------------------------------------------------------------ external search

  @D38 @D30
  Scenario: External search by namespace URI or alias URI, normalised
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/" and aliases "https://alias.example.org/{{run}}/terms"
    When I send "POST" to "/external-vocabulary/searches" with JSON body
      """
      {"namespaceUri": "https://example.org/ext/{{run}}/terms#"}
      """
    Then the status is 200
    And the item names are "{{Ext}}"
    And the response field "items.0.namespaceUri" equals "https://example.org/ext/{{run}}/terms/"
    When I send "POST" to "/external-vocabulary/searches" with JSON body
      """
      {"aliasUri": "https://alias.example.org/{{run}}/terms/"}
      """
    Then the item names are "{{Ext}}"

  @D38 @D153
  Scenario: External batch search keeps order, namespaces before aliases, each vocabulary once
    Given an external vocabulary "{{ExtA}}" is registered with namespace "https://example.org/ext/{{run}}/a/" and aliases "https://alias.example.org/{{run}}/a/"
    And an external vocabulary "{{ExtB}}" is registered with namespace "https://example.org/ext/{{run}}/b/"
    When I send "POST" to "/external-vocabulary/searches" with JSON body
      """
      {"namespaceUris": ["https://example.org/ext/{{run}}/b", "https://example.org/ext/{{run}}/none/"], "aliasUris": ["https://alias.example.org/{{run}}/a", "https://example.org/ext/{{run}}/b/"]}
      """
    Then the status is 200
    And the item names are "{{ExtB}}, {{ExtA}}"
    And the response field "total" equals 2
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the response field "totalPages" equals 1
    And the response field "self" is absent

  @D38 @D153
  Scenario: External search with no match gives an empty result
    When I send "POST" to "/external-vocabulary/searches" with JSON body
      """
      {"namespaceUri": "https://example.org/nothing/{{run}}/"}
      """
    Then the status is 200
    And the response field "items" has 0 items
    And the response field "totalPages" equals 0

  @D38 @D68 @D137 @D73 @negative
  Scenario Outline: An invalid external search body is rejected (<case>)
    When I send "POST" to "/external-vocabulary/searches" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                           | body                                                                       | name          |
      | empty object                   | {}                                                                         | body          |
      | namespaceUri and aliasUri      | {"namespaceUri": "https://a.example/x", "aliasUri": "https://b.example/y"}  | body          |
      | namespaceUri and namespaceUris | {"namespaceUri": "https://a.example/x", "namespaceUris": ["https://b.example/y"]} | body   |
      | empty namespaceUris            | {"namespaceUris": []}                                                      | namespaceUris |
      | empty aliasUris                | {"aliasUris": []}                                                          | aliasUris     |
      | relative element               | {"namespaceUris": ["not a uri"]}                                           | namespaceUris[0] |

  @D38 @D68 @NFR-04 @Q2 @negative
  Scenario Outline: More than 100 URIs in an external search are rejected (<arrays>)
    When I send an external search with <namespaces> namespace URIs and <aliases> alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | arrays           | namespaces | aliases | name          |
      | namespaces only  | 101        | 0       | namespaceUris |
      | aliases only     | 0          | 101     | aliasUris     |
      | both arrays      | 60         | 41      | aliasUris     |

  @D38 @NFR-04 @Q2
  Scenario: 100 URIs in an external search are accepted
    When I send an external search with 50 namespace URIs and 50 alias URIs
    Then the status is 200

  @D15 @negative @API-38
  Scenario: An external search that is not application/json gets 415
    When I send "POST" to "/external-vocabulary/searches" with body "namespaceUri=x" and content type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
