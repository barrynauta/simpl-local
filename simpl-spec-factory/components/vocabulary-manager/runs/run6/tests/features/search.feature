Feature: Search vocabularies by name and version
  POST /vocabularies/searches resolves 1 to 100 references in one read-only request (D38).
  It is not paged; results come in request order; missing references are left out.

  Background:
    Given I am authenticated as "ga-admin"

  @D38 @D25 @D45
  Scenario: Latest and versioned references come back in request order
    Given the vocabulary "Health" exists with 3 versions
    And the vocabulary "Legal" exists with 2 versions
    When I search for vocabularies with:
      """
      {"items": [{"name": "{{name:Legal}}", "version": 1}, {"name": "{{name:Health}}"}]}
      """
    Then the response status is 200
    And the response media type is "application/json"
    And the search results are:
      | name   | version | status     | latest | href                                   | contentHref                                     |
      | Legal  | 1       | DEPRECATED | false  | /vocabularies/{{name:Legal}}/versions/1 | {{base}}/content/{{name:Legal}}/versions/1      |
      | Health | 3       | ACTIVE     | true   | /vocabularies/{{name:Health}}           | {{base}}/content/{{name:Health}}                |
    And the response field "page" is 1
    And the response field "pageSize" is 100
    And the response field "total" is 2
    And the response field "totalPages" is 1
    And the response has none of the links "self, first, prev, next, last"

  @D38 @D3 @D25
  Scenario: A latest result carries the vocabulary status, a versioned result its own status
    Given the vocabulary "Health" exists with 2 versions
    And the vocabulary "Health" is deprecated
    When I search for vocabularies with:
      """
      {"items": [{"name": "{{name:Health}}"}, {"name": "{{name:Health}}", "version": 2}]}
      """
    Then the response status is 200
    And the search results are:
      | name   | version | status     | latest |
      | Health | 2       | DEPRECATED | true   |
      | Health | 2       | ACTIVE     | false  |

  @D38
  Scenario: Missing names and versions are left out
    Given the vocabulary "Health" exists
    When I search for vocabularies with:
      """
      {"items": [{"name": "{{name:Ghost}}"}, {"name": "{{name:Health}}", "version": 5}, {"name": "{{name:Health}}", "version": 1}]}
      """
    Then the response status is 200
    And the search results are:
      | name   | version | status | latest |
      | Health | 1       | ACTIVE | false  |
    And the response field "total" is 1

  @D38
  Scenario: No match gives an empty result
    When I search for vocabularies with:
      """
      {"items": [{"name": "{{name:Ghost}}"}]}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "total" is 0
    And the response field "totalPages" is 0

  @D38
  Scenario: A repeated reference gives one result at its first position
    Given the vocabularies "Health, Legal" exist
    When I search for vocabularies with:
      """
      {"items": [{"name": "{{name:Health}}"}, {"name": "{{name:Legal}}", "version": 1}, {"name": "{{name:Health}}"}, {"name": "{{name:Legal}}", "version": 1}]}
      """
    Then the response status is 200
    And the search results are:
      | name   | version | status | latest |
      | Health | 1       | ACTIVE | true   |
      | Legal  | 1       | ACTIVE | false  |

  @D38
  Scenario: A reference with and without the latest version number are two results
    Given the vocabulary "Health" exists
    When I search for vocabularies with:
      """
      {"items": [{"name": "{{name:Health}}", "version": 1}, {"name": "{{name:Health}}"}]}
      """
    Then the search results are:
      | name   | version | status | latest |
      | Health | 1       | ACTIVE | false  |
      | Health | 1       | ACTIVE | true   |

  @D54 @D38
  Scenario: The search by name is exact
    Given the vocabulary "Health" exists
    When I search for vocabularies with:
      """
      {"items": [{"name": "HEALTH{{runupper}}"}]}
      """
    Then the response status is 200
    And the response has 0 items

  @D38 @NFR-04
  Scenario: A search with 100 references is accepted
    When I search for vocabularies with 100 different names
    Then the response status is 200

  @D38 @NFR-04 @negative
  Scenario: A search with 101 references is rejected
    When I search for vocabularies with 101 different names
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "items"

  @D38 @negative
  Scenario Outline: A search with an invalid body is rejected: <case>
    When I search for vocabularies with:
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

    Examples:
      | case             | body                                                 |
      | no items         | {"items": []}                                        |
      | items missing    | {}                                                   |
      | invalid name     | {"items": [{"name": "lower"}]}                       |
      | version zero     | {"items": [{"name": "Health{{run}}", "version": 0}]} |
      | unknown property | {"items": [{"name": "Health{{run}}", "label": "x"}]} |
      | unknown root     | {"items": [{"name": "Health{{run}}"}], "page": 1}    |
      | not JSON         | {"items": [                                          |

  @D38 @D52 @negative
  Scenario: A search with a paging parameter is rejected
    When I send "POST" to "/vocabularies/searches?page=1" with the JSON body:
      """
      {"items": [{"name": "{{name:Ghost}}"}]}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "page"

  @D15 @API-38 @negative
  Scenario: A search that is not JSON is rejected with 415
    When I send "POST" to "/vocabularies/searches" with the body "name=Health" as "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
