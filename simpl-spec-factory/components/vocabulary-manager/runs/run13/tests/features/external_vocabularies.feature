Feature: Register, read, search and remove external vocabularies
  Test basis D41 (no story). Identity and keys D30, registration details D56, D70, D73, D147,
  D148, not-found texts D41 and D66, removal D42.

  Background:
    Given I am authenticated as "ga-admin"

  @D41 @D30 @D27 @D73 @D110 @D58
  Scenario: An external vocabulary is registered and returned as sent
    When I register an external vocabulary with the parts
      | part           | value                                            |
      | name           | {Ext}                                            |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns/          |
      | aliasUris      | https://alias2-{tok:lower}.example.org/ns        |
      | aliasUris      | http://alias1-{tok:lower}.example.org/ns#        |
      | sourceUri      | https://source.example.org/{tok:lower}/terms.ttl |
      | description    | Cached copy for validation.                      |
      | vocabularyFile | file:external.ttl                                |
    Then the response status is 201
    And the response header "Location" is "/external-vocabularies/{Ext}"
    And the response JSON has
      | field        | value                                            |
      | href         | /external-vocabularies/{Ext}                     |
      | name         | {Ext}                                            |
      | namespaceUri | https://ext-{tok:lower}.example.org/ns/          |
      | aliasUris[0] | https://alias2-{tok:lower}.example.org/ns        |
      | aliasUris[1] | http://alias1-{tok:lower}.example.org/ns#        |
      | sourceUri    | https://source.example.org/{tok:lower}/terms.ttl |
      | description  | Cached copy for validation.                      |
      | createdBy    | {admin_email}                                    |
      | creationDate | <date-time>                                      |
    When I send GET "/external-vocabularies/{Ext}"
    Then the response status is 200
    And the response JSON has
      | field        | value                                   |
      | name         | {Ext}                                   |
      | namespaceUri | https://ext-{tok:lower}.example.org/ns/ |
      | aliasUris[1] | http://alias1-{tok:lower}.example.org/ns# |
      | createdBy    | {admin_email}                           |

  @D41 @D110 @D148
  Scenario: Without aliases the list is present and empty; a blank description is absent
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | description    | blank:5                                |
      | vocabularyFile | file:external.ttl                      |
    Then the response status is 201
    And the response JSON has
      | field       | value        |
      | aliasUris   | <empty list> |
      | description | <absent>     |

  @D41 @D29
  Scenario: The content of an external vocabulary is downloaded by name as uploaded
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | vocabularyFile | file:external.ttl                      |
    Then the response status is 201
    When I send GET "/external-vocabularies/{Ext}/content"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the download file name is "{Ext}.ttl"
    And the response body is the fixture "external.ttl"

  @D41 @D66 @D30
  Scenario Outline: The content is found by namespace or alias, with or without a trailing character: <case>
    When I register an external vocabulary with the parts
      | part           | value                                       |
      | name           | {Ext}                                       |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns      |
      | aliasUris      | https://alias-{tok:lower}.example.org/ns/   |
      | vocabularyFile | file:external.ttl                           |
    And I send POST "/external-vocabularies/content" with the JSON body
      """
      {"namespaceUri": "<uri>"}
      """
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response body is the fixture "external.ttl"

    Examples:
      | case                 | uri                                      |
      | namespace as is      | https://ext-{tok:lower}.example.org/ns   |
      | namespace with '#'   | https://ext-{tok:lower}.example.org/ns#  |
      | alias without '/'    | https://alias-{tok:lower}.example.org/ns |

  @D66 @negative
  Scenario: Content for an unknown namespace gives 404 naming the URI as sent
    When I send POST "/external-vocabularies/content" with the JSON body
      """
      {"namespaceUri": "https://unknown-{tok:lower}.example.org/ns/"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri 'https://unknown-{tok:lower}.example.org/ns/' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "body" named "namespaceUri"

  @D73 @negative
  Scenario: Content lookup with a relative URI is rejected
    When I send POST "/external-vocabularies/content" with the JSON body
      """
      {"namespaceUri": "terms/ns"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "namespaceUri"

  @D41 @negative
  Scenario Outline: An unknown external vocabulary name gives 404: <method> <path>
    When I send <method> "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '{Ghost}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                  |
      | GET    | /external-vocabularies/{Ghost}        |
      | GET    | /external-vocabularies/{Ghost}/content |
      | DELETE | /external-vocabularies/{Ghost}        |

  @D41 @D42
  Scenario: Removing an external vocabulary deletes its metadata and content
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    When I send DELETE "/external-vocabularies/{Ext}"
    Then the response status is 204
    And the response body is empty
    And the external vocabulary "Ext" does not exist
    When I send GET "/external-vocabularies/{Ext}/content"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send POST "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "https://ext-{tok:lower}.example.org/ns"}
      """
    Then the response lists no items

  @D41 @D42
  Scenario: Removing an external vocabulary does not touch internal vocabularies
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    And a vocabulary "Alpha" exists
    And I remember the state of the vocabulary "Alpha"
    When I send DELETE "/external-vocabularies/{Ext}"
    Then the response status is 204
    And the vocabulary "Alpha" is unchanged

  @D30 @D41
  Scenario: A removed external vocabulary can be registered again
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    And the external vocabulary "Ext" is removed
    When I register the external vocabulary "Ext" with the namespace "https://ext-{tok:lower}.example.org/ns"
    Then the response status is 201

  @D41 @D22 @D21
  Scenario: The external vocabulary list is paged and sorted by name
    Given an external vocabulary "Zulu" is registered with the namespace "https://zulu-{tok:lower}.example.org/ns"
    And an external vocabulary "Alpha" is registered with the namespace "https://alpha-{tok:lower}.example.org/ns"
    When I send GET "/external-vocabularies"
    Then the response status is 200
    And the response JSON has
      | field    | value  |
      | page     | 1      |
      | pageSize | 20     |
      | self     | <present> |
      | first    | <present> |
    And the external vocabulary items are sorted by name
    And the external vocabulary list contains "Zulu" on some page
    And the external vocabulary list contains "Alpha" on some page

  @D41 @D21 @D22 @negative
  Scenario Outline: The external vocabulary list rejects <case>
    When I send GET "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | case               | query          | parameter |
      | a name filter      | name=Abc       | name      |
      | a sort             | sort=name      | sort      |
      | pageSize above 100 | pageSize=101   | pageSize  |
      | page 0             | page=0         | page      |

  @D30 @D41 @D9 @negative
  Scenario: Registration checks Turtle syntax
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | vocabularyFile | file:syntax-error-line3.ttl            |
    Then the validation report has one syntax error on line 3
    And the external vocabulary "Ext" does not exist

  @D30 @D41
  Scenario: Registration runs no semantic validation
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | vocabularyFile | file:external-semantic-errors.ttl      |
    Then the response status is 201
    And the external vocabulary "Ext" exists

  @D70 @negative
  Scenario: Registration rejects a file with no statements
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | vocabularyFile | empty-file                             |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the issue in "body" named "vocabularyFile" has the detail "Vocabulary contains no statements"
    And the external vocabulary "Ext" does not exist

  @D30 @D147 @negative @API-46
  Scenario: A name that is already registered gives 409
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    When I register an external vocabulary with the parts
      | part           | value                                    |
      | name           | {Ext:upper}                              |
      | namespaceUri   | https://other-{tok:lower}.example.org/ns |
      | vocabularyFile | file:external.ttl                        |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the external vocabulary "Ext:upper" does not exist

  @D30 @D147 @negative
  Scenario: The name conflict is reported before a namespace conflict
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns"
    When I register the external vocabulary "Ext" with the namespace "https://ext-{tok:lower}.example.org/ns"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with name '{Ext}' is already registered."

  @D30 @D55 @D66 @D147 @negative
  Scenario Outline: A namespace or alias that is already registered gives 409: <case>
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns" and the aliases "https://alias-{tok:lower}.example.org/ns"
    When I register an external vocabulary with the parts
      | part           | value             |
      | name           | {Other}           |
      | namespaceUri   | <namespace>       |
      | aliasUris      | <alias>           |
      | vocabularyFile | file:external.ttl |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '<reported>' is already registered."
    And the external vocabulary "Other" does not exist

    Examples:
      | case                         | namespace                                 | alias                                     | reported                                  |
      | same namespace with '/'      | https://ext-{tok:lower}.example.org/ns/   | https://free-{tok:lower}.example.org/a    | https://ext-{tok:lower}.example.org/ns/   |
      | namespace equals an alias    | https://alias-{tok:lower}.example.org/ns# | https://free-{tok:lower}.example.org/a    | https://alias-{tok:lower}.example.org/ns# |
      | alias equals a namespace     | https://free-{tok:lower}.example.org/n    | https://ext-{tok:lower}.example.org/ns#   | https://ext-{tok:lower}.example.org/ns#   |

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "Shared" exists
    When I register the external vocabulary "Shared" with the namespace "https://shared-{tok:lower}.example.org/ns"
    Then the response status is 201

  @D56 @Q2 @NFR-04
  Scenario: Twenty aliases are accepted
    When I register an external vocabulary with 20 aliases
    Then the response status is 201

  @D56 @Q2 @NFR-04 @negative
  Scenario: Twenty-one aliases are rejected
    When I register an external vocabulary with 21 aliases
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And the external vocabulary "Ext" does not exist

  @D56 @D73 @D70 @negative
  Scenario Outline: An invalid alias is rejected: <case>
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | aliasUris      | <alias 1>                              |
      | aliasUris      | <alias 2>                              |
      | vocabularyFile | file:external.ttl                      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And the external vocabulary "Ext" does not exist

    Examples:
      | case                          | alias 1                                  | alias 2                                   |
      | alias equals the namespace    | https://ext-{tok:lower}.example.org/ns#  | https://a-{tok:lower}.example.org/x       |
      | alias repeated                | https://a-{tok:lower}.example.org/x      | https://a-{tok:lower}.example.org/x/      |
      | comma-separated, not split    | a,b                                      | https://a-{tok:lower}.example.org/x       |
      | relative alias                | https://a-{tok:lower}.example.org/x      | relative/path                             |

  @D73 @negative
  Scenario Outline: A URI that is not absolute is rejected: <part>
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | <namespace>                            |
      | sourceUri      | <source>                               |
      | vocabularyFile | file:external.ttl                      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"

    Examples:
      | part         | namespace                              | source                 |
      | namespaceUri | ext-{tok:lower}.example.org/ns         | https://s.example.org/ |
      | sourceUri    | https://ext-{tok:lower}.example.org/ns | /local/file.ttl        |

  @D56 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: The registration fields follow their rules: <case>
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | <name>                                 |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | description    | <description>                          |
      | vocabularyFile | file:external.ttl                      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"

    Examples:
      | case                    | part        | name       | description         |
      | name in lower case      | name        | lower{tok} | Some text.          |
      | description over 1000   | description | {Ext}      | text-of-length:1001 |

  @D56 @Q2 @NFR-04
  Scenario: A description of 1000 characters is accepted
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | description    | text-of-length:1000                    |
      | vocabularyFile | file:external.ttl                      |
    Then the response status is 201

  @D41 @D15 @Q2 @NFR-04 @negative
  Scenario: A file above 5 MB gets 413
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | vocabularyFile | turtle-of-size:5242881                 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the external vocabulary "Ext" does not exist

  @D41 @D15 @negative @API-38
  Scenario: A registration sent as JSON gets 415
    When I send POST "/external-vocabularies" with the JSON body
      """
      {"name": "{Ext}", "namespaceUri": "https://ext-{tok:lower}.example.org/ns"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D52 @negative @BLG-req-valid
  Scenario: An unknown registration part is rejected
    When I register an external vocabulary with the parts
      | part           | value                                  |
      | name           | {Ext}                                  |
      | namespaceUri   | https://ext-{tok:lower}.example.org/ns |
      | createdBy      | someone@example.org                    |
      | vocabularyFile | file:external.ttl                      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "createdBy"

  @D38 @D30 @D153
  Scenario Outline: The external search finds by <case>
    Given an external vocabulary "Ext" is registered with the namespace "https://ext-{tok:lower}.example.org/ns" and the aliases "https://alias-{tok:lower}.example.org/ns"
    When I send POST "/external-vocabulary/searches" with the JSON body
      """
      <body>
      """
    Then the response status is 200
    And the external search result lists "Ext" in this order
    And the page fields are page 1, pageSize 100, total 1, totalPages 1
    And the response has the links "none" and no others

    Examples:
      | case                  | body                                                            |
      | namespace             | {"namespaceUri": "https://ext-{tok:lower}.example.org/ns"}      |
      | namespace with '#'    | {"namespaceUri": "https://ext-{tok:lower}.example.org/ns#"}     |
      | alias with '/'        | {"aliasUri": "https://alias-{tok:lower}.example.org/ns/"}       |
      | batch of namespaces   | {"namespaceUris": ["https://none-{tok:lower}.example.org/x", "https://ext-{tok:lower}.example.org/ns/"]} |
      | batch, matched twice  | {"namespaceUris": ["https://ext-{tok:lower}.example.org/ns"], "aliasUris": ["https://alias-{tok:lower}.example.org/ns"]} |

  @D38
  Scenario: Batch results follow the first matching URI, namespaces before aliases
    Given an external vocabulary "Ext1" is registered with the namespace "https://one-{tok:lower}.example.org/ns" and the aliases "https://one-alias-{tok:lower}.example.org/ns"
    And an external vocabulary "Ext2" is registered with the namespace "https://two-{tok:lower}.example.org/ns"
    When I send POST "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUris": ["https://two-{tok:lower}.example.org/ns"], "aliasUris": ["https://one-alias-{tok:lower}.example.org/ns"]}
      """
    Then the external search result lists "Ext2, Ext1" in this order

  @D38 @D153
  Scenario: An external search that finds nothing is empty
    When I send POST "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "https://none-{tok:lower}.example.org/ns"}
      """
    Then the response status is 200
    And the response lists no items
    And the page fields are page 1, pageSize 100, total 0, totalPages 0

  @D38 @D68 @D137 @D73 @negative
  Scenario Outline: An invalid external search is rejected: <case>
    When I send POST "/external-vocabulary/searches" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                              | body                                                                             | name             |
      | namespace and alias together      | {"namespaceUri": "https://a.example.org/x", "aliasUri": "https://b.example.org/y"} | body          |
      | single and batch together         | {"namespaceUri": "https://a.example.org/x", "namespaceUris": ["https://b.example.org/y"]} | body   |
      | an empty object                   | {}                                                                               | body             |
      | an empty namespace array          | {"namespaceUris": []}                                                            | namespaceUris    |
      | an empty alias array              | {"aliasUris": []}                                                                | aliasUris        |
      | a relative namespace URI          | {"namespaceUri": "relative/x"}                                                   | namespaceUri     |
      | a relative URI in a batch         | {"namespaceUris": ["https://a.example.org/x", "relative/x"]}                     | namespaceUris[1] |

  @D38 @D68 @Q2 @NFR-04
  Scenario: An external search with 100 URIs in total is accepted
    When I search the external vocabularies with 50 namespace URIs and 50 alias URIs
    Then the response status is 200

  @D38 @D68 @Q2 @NFR-04 @negative
  Scenario Outline: An external search with more than 100 URIs is rejected: <case>
    When I search the external vocabularies with <namespaces> namespace URIs and <aliases> alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case                | namespaces | aliases | name          |
      | namespaces only     | 101        | 0       | namespaceUris |
      | aliases only        | 0          | 101     | aliasUris     |
      | both arrays         | 51         | 50      | aliasUris     |
