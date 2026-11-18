Feature: Read-only search operations
  POST /vocabularies/searches and POST /external-vocabulary/searches are not paged, take at
  most 100 references and keep a fixed order (D38). Viewers may use them (D16).

  Background:
    Given I am authenticated as "ga-viewer"

  # ---------------------------------------------------------------- internal search
  @D38 @D5 @D25 @D26 @E2
  Scenario: Latest and versioned references come back in request order
    Given a vocabulary "Health" exists with 3 versions
    And a vocabulary "Legacy" exists
    And version 1 of vocabulary "Legacy" has status "DEPRECATED"
    When I search vocabularies for:
      | name            | version |
      | ${name:Health}  |         |
      | ${name:Legacy}  | 1       |
    Then the response status is 200
    And the search results are:
      | name           | version | status     | latest |
      | ${name:Health} | 3       | ACTIVE     | true   |
      | ${name:Legacy} | 1       | DEPRECATED | false  |
    And the page fields are page 1, pageSize 100, total 2 and totalPages 1
    And the page links "self, first, prev, next, last" are absent

  @D38 @D3
  Scenario: A latest result carries the vocabulary status, a versioned result its own status
    Given a vocabulary "Health" exists
    And the vocabulary "Health" has status "DEPRECATED"
    When I search vocabularies for:
      | name           | version |
      | ${name:Health} |         |
      | ${name:Health} | 1       |
    Then the search results are:
      | name           | version | status     | latest |
      | ${name:Health} | 1       | DEPRECATED | true   |
      | ${name:Health} | 1       | ACTIVE     | false  |

  @D38
  Scenario: Missing names and versions are left out
    Given a vocabulary "Health" exists
    When I search vocabularies for:
      | name            | version |
      | ${name:Missing} |         |
      | ${name:Health}  | 4       |
      | ${name:Health}  | 1       |
    Then the response status is 200
    And the search results are:
      | name           | version | status | latest |
      | ${name:Health} | 1       | ACTIVE | false  |
    And the page fields are page 1, pageSize 100, total 1 and totalPages 1

  @D38
  Scenario: Nothing found gives an empty result with zero pages
    When I search vocabularies for:
      | name            | version |
      | ${name:Missing} |         |
    Then the response status is 200
    And the list holds no items
    And the page fields are page 1, pageSize 100, total 0 and totalPages 0

  @D38
  Scenario: A repeated reference gives one result at its first position
    Given a vocabulary "Alpha" exists with 2 versions
    And a vocabulary "Beta" exists
    When I search vocabularies for:
      | name          | version |
      | ${name:Alpha} | 1       |
      | ${name:Beta}  |         |
      | ${name:Alpha} | 1       |
      | ${name:Beta}  |         |
    Then the search results are:
      | name          | version | status     | latest |
      | ${name:Alpha} | 1       | DEPRECATED | false  |
      | ${name:Beta}  | 1       | ACTIVE     | true   |

  @D38
  Scenario: A latest reference and the same version by number are two results
    Given a vocabulary "Alpha" exists with 2 versions
    When I search vocabularies for:
      | name          | version |
      | ${name:Alpha} |         |
      | ${name:Alpha} | 2       |
    Then the search results are:
      | name          | version | status | latest |
      | ${name:Alpha} | 2       | ACTIVE | true   |
      | ${name:Alpha} | 2       | ACTIVE | false  |

  @D38
  Scenario: One hundred references are accepted
    When I search vocabularies with 100 references
    Then the response status is 200

  @D38 @negative @D39
  Scenario: More than one hundred references are rejected
    When I search vocabularies with 101 references
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "items"

  @D38 @negative @OWA-API3 @BLG-req-valid
  Scenario Outline: An invalid search body is rejected (<case>)
    When I send POST "/vocabularies/searches" with the JSON body:
      """
      <body>
      """
    Then the response is a problem with status 400

    Examples:
      | case                 | body                                              |
      | no items             | {"items": []}                                     |
      | items missing        | {}                                                |
      | name pattern         | {"items": [{"name": "lower"}]}                    |
      | version zero         | {"items": [{"name": "${run}A", "version": 0}]}    |
      | extra item property  | {"items": [{"name": "${run}A", "status": "ACTIVE"}]} |
      | extra root property  | {"items": [{"name": "${run}A"}], "page": 1}       |

  @D38 @D21 @negative
  Scenario Outline: The search takes no query parameters (<query>)
    When I send POST "/vocabularies/searches?<query>" with the JSON body:
      """
      {"items": [{"name": "${run}Alpha"}]}
      """
    Then the response is a problem with status 400

    Examples:
      | query      |
      | page=1     |
      | pageSize=5 |
      | foo=bar    |

  @D15 @negative @API-38 @API-46
  Scenario: A search that is not JSON gives 415
    When I send POST "/vocabularies/searches" with the content type "text/plain" and the body:
      """
      items
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  # ---------------------------------------------------------------- external search
  @D38 @D30 @D41
  Scenario Outline: An external vocabulary is found by <case>
    Given an external vocabulary "Dc" is registered with:
      | field        | value                                 |
      | namespaceUri | https://dc.example/${runid}/terms     |
      | aliasUris    | https://alias.example/${runid}/terms/ |
    When I send POST "/external-vocabulary/searches" with the JSON body:
      """
      <body>
      """
    Then the response status is 200
    And the external search results are "Dc" in this order
    And the page fields are page 1, pageSize 100, total 1 and totalPages 1
    And the page links "self, first, prev, next, last" are absent

    Examples:
      | case                     | body                                                         |
      | namespace                | {"namespaceUri": "https://dc.example/${runid}/terms"}       |
      | namespace with hash      | {"namespaceUri": "https://dc.example/${runid}/terms#"}      |
      | alias                    | {"aliasUri": "https://alias.example/${runid}/terms/"}       |
      | alias without slash      | {"aliasUri": "https://alias.example/${runid}/terms"}        |
      | several URIs of the same | {"namespaceUris": ["https://dc.example/${runid}/terms", "https://dc.example/${runid}/terms/"], "aliasUris": ["https://alias.example/${runid}/terms"]} |

  @D38
  Scenario: Batch results follow the first matching URI, namespaces before aliases
    Given an external vocabulary "One" is registered
    And an external vocabulary "Two" is registered
    And an external vocabulary "Three" is registered with:
      | field     | value                               |
      | aliasUris | https://alias.example/${runid}/three |
    When I send POST "/external-vocabulary/searches" with the JSON body:
      """
      {"namespaceUris": ["${extns:Two}", "https://none.example/${runid}", "${extns:One}"],
       "aliasUris": ["https://alias.example/${runid}/three", "${extns:Two}"]}
      """
    Then the response status is 200
    And the external search results are "Two, One, Three" in this order

  @D38
  Scenario: No match gives an empty result
    When I send POST "/external-vocabulary/searches" with the JSON body:
      """
      {"namespaceUri": "https://none.example/${runid}/nothing"}
      """
    Then the response status is 200
    And the list holds no items
    And the page fields are page 1, pageSize 100, total 0 and totalPages 0

  @D38 @negative
  Scenario Outline: An external search body that fits no single branch is rejected (<case>)
    When I send POST "/external-vocabulary/searches" with the JSON body:
      """
      <body>
      """
    Then the response is a problem with status 400

    Examples:
      | case                         | body                                                                          |
      | empty object                 | {}                                                                            |
      | namespace and alias          | {"namespaceUri": "https://a.example/x", "aliasUri": "https://b.example/x"}    |
      | namespace and namespace list | {"namespaceUri": "https://a.example/x", "namespaceUris": ["https://b.example/x"]} |
      | empty namespace list         | {"namespaceUris": []}                                                         |
      | empty alias list             | {"aliasUris": []}                                                             |
      | unknown property             | {"uri": "https://a.example/x"}                                                |

  @D38
  Scenario: One hundred URIs in total are accepted
    When I search external vocabularies with 60 namespace URIs and 40 alias URIs
    Then the response status is 200

  @D38 @negative
  Scenario: More than one hundred URIs in total are rejected
    When I search external vocabularies with 60 namespace URIs and 41 alias URIs
    Then the response is a problem with status 400

  @D38 @negative
  Scenario: The external search takes no query parameters
    When I send POST "/external-vocabulary/searches?page=1" with the JSON body:
      """
      {"namespaceUri": "https://none.example/${runid}"}
      """
    Then the response is a problem with status 400
