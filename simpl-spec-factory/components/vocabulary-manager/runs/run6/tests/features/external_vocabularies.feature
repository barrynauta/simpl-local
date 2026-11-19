Feature: Register, read, search and remove external vocabularies
  An external vocabulary is a registered cached copy of a vocabulary Simpl-Open does not control.
  Its name is its identity; its namespace URI and alias URIs are unique lookup keys, compared after
  removing a trailing '#' or '/' (D30, D41, D56). Registration checks Turtle syntax only.

  Background:
    Given I am authenticated as "ga-admin"

  @D41 @D30 @D27 @D58
  Scenario: An external vocabulary is registered
    When I register the external vocabulary "Terms" with:
      | field     | value                         |
      | aliasUris | {{ext:TermsAlias}}            |
      | aliasUris | {{ext:TermsOther}}/           |
    Then the response status is 201
    And the response media type is "application/json"
    And the response header "Location" is "/external-vocabularies/{{name:Terms}}"
    And the response field "href" is "/external-vocabularies/{{name:Terms}}"
    And the response field "name" is "{{name:Terms}}"
    And the response field "namespaceUri" matches "{{ext:Terms}}[#/]?"
    And the response field "sourceUri" is "https://source.example.org/{{run}}/terms.ttl"
    And the response field "description" is "Cached copy of Terms {{run}}"
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the response field "creationDate" is a UTC time to the second
    And the response aliasUris are "{{ext:TermsAlias}}, {{ext:TermsOther}}" in any order

  @D41
  Scenario: A registered external vocabulary can be read by name and is in the list
    Given the external vocabulary "Terms" is registered
    When I send "GET" to "/external-vocabularies/{{name:Terms}}"
    Then the response status is 200
    And the response field "name" is "{{name:Terms}}"
    And the response field "href" is "/external-vocabularies/{{name:Terms}}"
    And the external vocabulary list holds "Terms"

  @D41 @D22
  Scenario: The external vocabulary list is paged and ordered by name
    Given the external vocabulary "Terms" is registered
    When I send "GET" to "/external-vocabularies?pageSize=100"
    Then the response status is 200
    And the response has the fields "page, pageSize, total, totalPages, self, first, items"
    And the response field "pageSize" is 100
    And the item names on this page are sorted by code point

  @D41 @D21 @negative
  Scenario Outline: The external vocabulary list rejects the parameter <query>
    When I send "GET" to "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query          | parameter |
      | name=Terms     | name      |
      | sort=name      | sort      |
      | pageSize=101   | pageSize  |
      | page=0         | page      |

  @D41 @D29
  Scenario: The content of an external vocabulary is downloaded by name
    Given the external vocabulary "Terms" is registered
    When I send "GET" to "/external-vocabularies/{{name:Terms}}/content"
    Then the response status is 200
    And the response media type is "text/turtle"
    And the download file name is "{{name:Terms}}.ttl"
    And the response body equals the content registered for "Terms"

  @D30 @D41
  Scenario Outline: The content of an external vocabulary is found by the namespace form <form>
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}" and the alias "{{ext:TermsAlias}}"
    When I request the external content for the namespace "<uri>"
    Then the response status is 200
    And the response media type is "text/turtle"
    And the response body equals the content registered for "Terms"

    Examples:
      | form             | uri                  |
      | as registered    | {{ext:Terms}}        |
      | with a slash     | {{ext:Terms}}/       |
      | with a hash      | {{ext:Terms}}#       |
      | alias            | {{ext:TermsAlias}}   |
      | alias with slash | {{ext:TermsAlias}}/  |

  @D15 @D41 @API-38 @negative
  Scenario: A content request by namespace that is not JSON is rejected with 415
    When I send "POST" to "/external-vocabularies/content" with the body "namespaceUri={{ext:Terms}}" as "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D21 @D52 @negative
  Scenario Outline: An unknown query parameter is rejected on <method> <path>
    Given the external vocabulary "Terms" is registered
    When I send "<method>" to "<path>?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"
    And the external vocabulary "Terms" is still registered

    Examples:
      | method | path                                       | query        | parameter |
      | GET    | /external-vocabularies/{{name:Terms}}/content | format=ttl   | format    |
      | DELETE | /external-vocabularies/{{name:Terms}}         | force=true   | force     |

  @D41 @negative
  Scenario: Content for an unknown namespace gives 404
    When I request the external content for the namespace "{{ext:Nothing}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D41 @D55 @negative
  Scenario Outline: An unknown external vocabulary name gives 404 on <path>
    When I send "<method>" to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '{{name:Ghost}}' was not found."
    And the problem has 1 issue
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                      |
      | GET    | /external-vocabularies/{{name:Ghost}}         |
      | GET    | /external-vocabularies/{{name:Ghost}}/content |
      | DELETE | /external-vocabularies/{{name:Ghost}}         |

  @D49 @negative
  Scenario Outline: An invalid external vocabulary name in the path gives 400 on <method>
    When I send "<method>" to "/external-vocabularies/lower{{run}}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "name" with type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | method |
      | GET    |
      | DELETE |

  @D41 @D42
  Scenario: Removing an external vocabulary deletes metadata and content
    Given the external vocabulary "Terms" is registered
    When I send "DELETE" to "/external-vocabularies/{{name:Terms}}"
    Then the response status is 204
    When I send "GET" to "/external-vocabularies/{{name:Terms}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send "GET" to "/external-vocabularies/{{name:Terms}}/content"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send "DELETE" to "/external-vocabularies/{{name:Terms}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D30
  Scenario: A removed external vocabulary can be registered again
    Given the external vocabulary "Terms" is registered
    When I send "DELETE" to "/external-vocabularies/{{name:Terms}}"
    Then the response status is 204
    When I register the external vocabulary "Terms"
    Then the response status is 201

  @D42 @D41
  Scenario: Removing a referenced external vocabulary leaves internal versions alone
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}"
    When I create the vocabulary "User" with the prefix "dct" for "{{ext:Terms}}/"
    Then the response status is 201
    And the response has no validation warnings
    When I send "DELETE" to "/external-vocabularies/{{name:Terms}}"
    Then the response status is 204
    And version 1 of "User" has the status "ACTIVE"
    When I send "GET" to "/content/{{name:User}}/versions/1"
    Then the response body equals the content of version 1 of "User"
    When I create a new version of "User" with the prefix "dct" for "{{ext:Terms}}/"
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '{{ext:Terms}}/'"

  @D42 @D12
  Scenario: Registering the external vocabulary later clears the warning on the next version
    Given the vocabulary "User" exists
    When I create a new version of "User" with the prefix "dct" for "{{ext:Terms}}#"
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 'dct' -> '{{ext:Terms}}#'"
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}"
    When I create a new version of "User" with the prefix "dct" for "{{ext:Terms}}#"
    Then the response status is 201
    And the response has no validation warnings

  # ---- registration rules ---------------------------------------------------------------

  @D30 @D41 @D48 @negative
  Scenario: A duplicate external vocabulary name is rejected with 409
    Given the external vocabulary "Terms" is registered
    When I register the external vocabulary "Terms" with:
      | field        | value                  |
      | namespaceUri | {{ext:OtherNamespace}} |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D56 @negative
  Scenario: An external vocabulary name that differs only in case is a duplicate
    Given the external vocabulary "Terms" is registered
    When I register the external vocabulary "Terms" with:
      | field        | value                  |
      | name         | TERMS{{runupper}}      |
      | namespaceUri | {{ext:OtherNamespace}} |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And exactly one external vocabulary named "Terms" in any case is registered, with its original name

  @D30 @D55 @negative
  Scenario Outline: A namespace or alias that is already registered is rejected: <case>
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}" and the alias "{{ext:TermsAlias}}"
    When I register the external vocabulary "Second" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail starts with "An external vocabulary with namespace or alias '"
    And the problem detail ends with "' is already registered."
    And the external vocabulary "Second" is not registered

    Examples:
      | case                         | field        | value               |
      | same namespace               | namespaceUri | {{ext:Terms}}       |
      | namespace with slash         | namespaceUri | {{ext:Terms}}/      |
      | namespace equals an alias    | namespaceUri | {{ext:TermsAlias}}# |
      | alias equals a namespace     | aliasUris    | {{ext:Terms}}/      |
      | alias equals an alias        | aliasUris    | {{ext:TermsAlias}}  |

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given the vocabulary "Shared" exists
    When I register the external vocabulary "Shared"
    Then the response status is 201
    And the field "name" of "/vocabularies/{{name:Shared}}" is "{{name:Shared}}"

  @D30 @D41
  Scenario: Registration does not run semantic validation and gives no warnings
    When I register the external vocabulary "Loose" with:
      | field          | value                                  |
      | vocabularyFile | file:external_with_semantic_error.ttl |
    Then the response status is 201
    And the response carries no validation warnings

  @D30 @D41 @D9 @negative
  Scenario: Registration with invalid Turtle is rejected with one syntax issue
    When I register the external vocabulary "Broken" with:
      | field          | value                          |
      | vocabularyFile | file:external_syntax_error.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile" with type "urn:problem-type:belgif:input-validation:invalidInput"
    And the first issue has a detail starting with "Encountered invalid Turtle syntax"
    And the first issue has a location matching "line 2, column \d+"
    And the external vocabulary "Broken" is not registered

  @D56 @D41 @negative
  Scenario Outline: A registration with an invalid <field> is rejected
    When I register the external vocabulary "Bad" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And the external vocabulary "Bad" is not registered

    Examples:
      | field          | value             |
      | name           | (omitted)         |
      | namespaceUri   | (omitted)         |
      | vocabularyFile | (omitted)         |
      | description    | chars:1001        |
      | aliasUris      | {{ext:Bad}}/      |
      | colour         | blue              |

  @D56 @negative
  Scenario: An invalid external vocabulary name is rejected
    When I register the external vocabulary "Bad" with:
      | field | value        |
      | name  | lower{{run}} |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And the external vocabulary "raw:lower{{run}}" is not registered

  @D56 @negative
  Scenario: Two equal aliases are rejected
    When I register the external vocabulary "Bad" with:
      | field     | value              |
      | aliasUris | {{ext:TwinAlias}}  |
      | aliasUris | {{ext:TwinAlias}}# |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And the external vocabulary "Bad" is not registered

  @D56 @NFR-04
  Scenario: Twenty aliases are accepted
    When I register the external vocabulary "Many" with 20 alias URIs
    Then the response status is 201

  @D56 @NFR-04 @negative
  Scenario: Twenty-one aliases are rejected
    When I register the external vocabulary "Many" with 21 alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And the external vocabulary "Many" is not registered

  @D56
  Scenario: The description is optional
    When I register the external vocabulary "Plain" with:
      | field       | value     |
      | description | (omitted) |
      | sourceUri   | (omitted) |
    Then the response status is 201

  @D15 @D41 @NFR-04 @negative
  Scenario: A registration file above 5 MB is rejected with 413
    When I register the external vocabulary "Big" with:
      | field          | value          |
      | vocabularyFile | turtle:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the external vocabulary "Big" is not registered

  @D15 @D41 @API-38 @negative
  Scenario: A registration that is not multipart is rejected with 415
    When I register the external vocabulary "Json" as JSON
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the external vocabulary "Json" is not registered

  # ---- external search ------------------------------------------------------------------

  @D38 @D30
  Scenario Outline: An external vocabulary is found by <criterion>
    Given the external vocabulary "Terms" is registered with the namespace "{{ext:Terms}}" and the alias "{{ext:TermsAlias}}"
    When I search for external vocabularies with:
      """
      {"<criterion>": "<uri>"}
      """
    Then the response status is 200
    And the external search results are "Terms"
    And the response field "total" is 1
    And the response field "page" is 1
    And the response field "pageSize" is 100
    And the response field "totalPages" is 1
    And the response has none of the links "self, first, prev, next, last"

    Examples:
      | criterion    | uri                  |
      | namespaceUri | {{ext:Terms}}        |
      | namespaceUri | {{ext:Terms}}/       |
      | aliasUri     | {{ext:TermsAlias}}   |
      | aliasUri     | {{ext:TermsAlias}}#  |

  @D38 @D30
  Scenario: A batch search orders results by the first matching URI and lists each once
    Given the external vocabulary "First" is registered with the namespace "{{ext:First}}" and the alias "{{ext:FirstAlias}}"
    And the external vocabulary "Second" is registered with the namespace "{{ext:Second}}"
    When I search for external vocabularies with:
      """
      {"namespaceUris": ["{{ext:Nothing}}", "{{ext:Second}}/", "{{ext:First}}"], "aliasUris": ["{{ext:FirstAlias}}"]}
      """
    Then the response status is 200
    And the external search results are "Second, First"
    And the response field "total" is 2

  @D38 @D30
  Scenario: Namespace URIs come before alias URIs in the result order
    Given the external vocabulary "First" is registered with the namespace "{{ext:First}}" and the alias "{{ext:FirstAlias}}"
    And the external vocabulary "Second" is registered with the namespace "{{ext:Second}}"
    When I search for external vocabularies with:
      """
      {"aliasUris": ["{{ext:FirstAlias}}"], "namespaceUris": ["{{ext:Second}}"]}
      """
    Then the external search results are "Second, First"

  @D38
  Scenario: An external search without a match returns an empty result
    When I search for external vocabularies with:
      """
      {"namespaceUri": "{{ext:Nothing}}"}
      """
    Then the response status is 200
    And the response has 0 items
    And the response field "total" is 0
    And the response field "totalPages" is 0

  @D38 @negative
  Scenario Outline: An external search body that does not fit exactly one form is rejected: <case>
    When I search for external vocabularies with:
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

    Examples:
      | case                     | body                                                                          |
      | empty object             | {}                                                                            |
      | namespace and alias      | {"namespaceUri": "{{ext:A}}", "aliasUri": "{{ext:B}}"}                         |
      | single and batch         | {"namespaceUri": "{{ext:A}}", "namespaceUris": ["{{ext:B}}"]}                  |
      | empty namespace array    | {"namespaceUris": []}                                                         |
      | empty alias array        | {"namespaceUris": ["{{ext:A}}"], "aliasUris": []}                              |
      | unknown property         | {"namespaceUri": "{{ext:A}}", "colour": "blue"}                                |

  @D38 @NFR-04
  Scenario: An external search with 100 URIs in total is accepted
    When I search for external vocabularies with 60 namespace URIs and 40 alias URIs
    Then the response status is 200

  @D38 @NFR-04 @negative
  Scenario: An external search with 101 URIs in total is rejected
    When I search for external vocabularies with 60 namespace URIs and 41 alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D38 @D52 @negative
  Scenario: An external search with a query parameter is rejected
    When I send "POST" to "/external-vocabulary/searches?pageSize=10" with the JSON body:
      """
      {"namespaceUri": "{{ext:A}}"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "pageSize"
