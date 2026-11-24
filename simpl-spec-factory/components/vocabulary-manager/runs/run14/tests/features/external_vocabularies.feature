Feature: Register, read, download and remove external vocabularies
  Test basis: the OpenAPI responses plus D30, D41, D42, D56, D66, D70, D73, D147, D148 (no story, D41).

  Background:
    Given I am authenticated as "ga-admin"

  @D41 @D30 @D27 @D56 @D73 @D110 @D58
  Scenario: An external vocabulary is registered with all its fields
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value                       |
      | name           | $Ext                        |
      | namespaceUri   | $EXT/dc/terms/              |
      | aliasUris      | $EXT/alias/two              |
      | aliasUris      | $EXT/alias/one#             |
      | description    | Cached copy for validation. |
      | sourceUri      | $EXT/source/terms.ttl       |
      | vocabularyFile | @valid                      |
    Then the response status is 201
    And the response media type is "application/json"
    And the response header "Location" is "/external-vocabularies/$Ext"
    And the response field "href" equals "/external-vocabularies/$Ext"
    And the response field "name" equals "$Ext"
    And the response field "namespaceUri" equals "$EXT/dc/terms/"
    And the response field "aliasUris" equals ["$EXT/alias/two", "$EXT/alias/one#"]
    And the response field "sourceUri" equals "$EXT/source/terms.ttl"
    And the response field "description" equals "Cached copy for validation."
    And the response field "createdBy" equals "ga-admin@example.com"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "validationWarnings" is absent

  @D41 @D110
  Scenario: Only the required fields are needed and aliasUris is always present
    When I register the external vocabulary "$Ext" with namespace "$EXT/min"
    Then the response status is 201
    And the response field "aliasUris" equals []
    When I send a GET request to "/external-vocabularies/$Ext"
    Then the response status is 200
    And the response field "name" equals "$Ext"
    And the response field "namespaceUri" equals "$EXT/min"
    And the response field "aliasUris" equals []
    And the response field "createdBy" equals "ga-admin@example.com"

  @D41
  Scenario: The list of external vocabularies is paged and in name order
    Given an external vocabulary "$ExtB" is registered with namespace "$EXT/b"
    And an external vocabulary "$ExtA" is registered with namespace "$EXT/a"
    When I send a GET request to "/external-vocabularies"
    Then the response status is 200
    And the response has the fields "page, pageSize, total, totalPages, items, self, first"
    And the response field "pageSize" equals 20
    And the external vocabularies "$ExtA" and "$ExtB" are listed in this order

  @D41 @D22 @negative
  Scenario Outline: The external list rejects bad paging values and any other parameter: <query>
    When I send a GET request to "/external-vocabularies?<query>"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "<param>"

    Examples:
      | query           | param    |
      | page=0          | page     |
      | pageSize=101    | pageSize |
      | pageSize=x      | pageSize |
      | sort=-name      | sort     |
      | namespaceUri=a  | namespaceUri |

  @D41 @D29 @D70
  Scenario: The content of an external vocabulary is downloaded by name
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/dl"
    When I send a GET request to "/external-vocabularies/$Ext/content"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response is an attachment named "$Ext.ttl"
    And the response body is the uploaded file of the external vocabulary "$Ext"

  @D41 @D30 @D73
  Scenario Outline: The content is found by namespace or alias, normalised: <uri>
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/ns#" and aliases "$EXT/alias"
    When I send a POST request to "/external-vocabularies/content" with JSON:
      """
      {"namespaceUri": "<uri>"}
      """
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response is an attachment named "$Ext.ttl"
    And the response body is the uploaded file of the external vocabulary "$Ext"

    Examples:
      | uri          |
      | $EXT/ns#     |
      | $EXT/ns      |
      | $EXT/ns/     |
      | $EXT/alias   |
      | $EXT/alias#  |

  @D66 @D41 @negative
  Scenario: Content for an unknown namespace gives 404 with the URI as sent
    When I send a POST request to "/external-vocabularies/content" with JSON:
      """
      {"namespaceUri": "$EXT/unknown/"}
      """
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri '$EXT/unknown/' was not found."
    And the problem has 1 issue
    And issue 1 is in "body" named "namespaceUri"

  @D73 @D52 @negative
  Scenario Outline: The content lookup rejects an invalid body: <body>
    When I send a POST request to "/external-vocabularies/content" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                        | name         |
      | {"namespaceUri": "relative/path"}           | namespaceUri |
      | {}                                          | namespaceUri |
      | {"namespaceUri": "$EXT/a", "name": "X"}     | name         |

  @D15 @API-38 @negative
  Scenario: The content lookup that is not application/json gets 415
    When I send a POST request to "/external-vocabularies/content" with the body "namespaceUri=x" as "text/plain"
    Then the response is a problem with status 415

  @D63 @negative
  Scenario: The content lookup with Accept application/json gets 406
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/acc"
    And the request header "Accept" is "application/json"
    When I send a POST request to "/external-vocabularies/content" with JSON:
      """
      {"namespaceUri": "$EXT/acc"}
      """
    Then the response is a problem with status 406

  @D41 @D42
  Scenario: Removing an external vocabulary deletes its metadata and content
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/del"
    When I send a DELETE request to "/external-vocabularies/$Ext"
    Then the response status is 204
    When I send a GET request to "/external-vocabularies/$Ext"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '$Ext' was not found."
    And the problem has 1 issue
    And issue 1 is in "path" named "name"
    When I send a GET request to "/external-vocabularies/$Ext/content"
    Then the response is a problem with status 404
    When I send a POST request to "/external-vocabularies/content" with JSON:
      """
      {"namespaceUri": "$EXT/del"}
      """
    Then the response is a problem with status 404

  @D41 @D204 @negative
  Scenario Outline: An unknown external vocabulary gives 404: <method> <path>
    When I send a <method> request to "<path>"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '$Missing' was not found."
    And the problem has 1 issue
    And issue 1 is in "path" named "name"

    Examples:
      | method | path                                |
      | GET    | /external-vocabularies/$Missing     |
      | GET    | /external-vocabularies/$Missing/content |
      | DELETE | /external-vocabularies/$Missing     |

  @D30
  Scenario: An external vocabulary can be registered again after removal
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/again"
    And the external vocabulary "$Ext" is removed
    When I register the external vocabulary "$Ext" with namespace "$EXT/again"
    Then the response status is 201

  @D42
  Scenario: Removal does not touch internal vocabularies that reference the namespace
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/ref#"
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix r: <$EXT/ref#> .
      @prefix ex: <https://example.org/$RUN/alpha#> .
      ex:Scheme a skos:ConceptScheme ; skos:related r:Thing .
      """
    And a vocabulary "$Alpha" exists with that file
    When I send a DELETE request to "/external-vocabularies/$Ext"
    Then the response status is 204
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

  @D30 @D147 @negative @API-46
  Scenario: A registered name is refused with 409 and the name text
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/one"
    When I register the external vocabulary "$Ext" with namespace "$EXT/two"
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with name '$Ext' is already registered."

  @D56 @D147 @negative
  Scenario: The external name is unique ignoring case
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/one"
    When I register the external vocabulary "$Ext!upper" with namespace "$EXT/two"
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"

  @D30 @D55 @D66 @negative
  Scenario Outline: A namespace or alias already registered is refused with 409: <namespace>
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/taken#" and aliases "$EXT/taken-alias"
    When I register the external vocabulary "$Other" with namespace "<namespace>"
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '<namespace>' is already registered."
    And the external vocabulary "$Other" is not registered

    Examples:
      | namespace          |
      | $EXT/taken#        |
      | $EXT/taken/        |
      | $EXT/taken         |
      | $EXT/taken-alias#  |

  @D30 @D147 @negative
  Scenario: An alias that equals a registered namespace is refused with 409
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/main"
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value        |
      | name           | $Other       |
      | namespaceUri   | $EXT/fresh   |
      | aliasUris      | $EXT/main#   |
      | vocabularyFile | @valid       |
    Then the response is a problem with status 409
    And the problem detail is "An external vocabulary with namespace or alias '$EXT/main#' is already registered."

  @D147 @negative
  Scenario: The name conflict is checked before the namespace conflict
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/first"
    When I register the external vocabulary "$Ext" with namespace "$EXT/first"
    Then the response is a problem with status 409
    And the problem detail is "An external vocabulary with name '$Ext' is already registered."

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "$Shared" exists
    When I register the external vocabulary "$Shared" with namespace "$EXT/shared"
    Then the response status is 201

  @D30 @D41 @D9 @negative
  Scenario: Invalid Turtle is refused with one syntax issue
    Given the vocabulary file is:
      """
      @prefix ex: <https://example.org/x#> .
      ex:a ex:b .
      """
    When I register the external vocabulary "$Ext" with namespace "$EXT/bad"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "vocabularyFile"
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And issue 1 has a location matching "line 2(, column \d+)?"
    And the external vocabulary "$Ext" is not registered

  @D30 @D41
  Scenario: Registration runs no semantic validation and gives no warnings
    Given the vocabulary file is:
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix other: <https://unregistered.example.org/$RUN#> .
      @prefix ex: <https://example.org/$RUN/sem#> .
      ex:hasPart a owl:ObjectProperty .
      ex:car ex:hasPart "wheel" ; other:x other:y .
      """
    When I register the external vocabulary "$Ext" with namespace "$EXT/sem"
    Then the response status is 201
    And the response field "validationWarnings" is absent

  @D70 @negative
  Scenario: Registration refuses a file without statements
    Given the vocabulary file is:
      """
      @prefix ex: <https://example.org/x#> .
      """
    When I register the external vocabulary "$Ext" with namespace "$EXT/empty"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Vocabulary contains no statements"
    And issue 1 is in "body" named "vocabularyFile"

  @D124
  Scenario: Relative IRIs in an external file resolve against its namespace
    Given the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      <#Term> a skos:Concept ; skos:prefLabel "Term"@en .
      """
    When I register the external vocabulary "$Ext" with namespace "$EXT/rel"
    Then the response status is 201

  @D56 @D37 @D73 @negative @Q2 @NFR-04
  Scenario Outline: Invalid registration fields are refused and nothing is registered: <case>
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value        |
      | name           | <name>       |
      | namespaceUri   | <namespace>  |
      | description    | <description> |
      | vocabularyFile | @valid       |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary has a name containing "$RUN"

    Examples:
      | case                      | name   | namespace     | description    | field        |
      | lower-case name           | ext$RUN | $EXT/n1      | d              | name         |
      | name with a hyphen        | E-$RUN | $EXT/n2       | d              | name         |
      | name of 65 characters     | $N65   | $EXT/n3       | d              | name         |
      | relative namespace        | $Ext   | relative/ns   | d              | namespaceUri |
      | namespace without scheme  | $Ext   | //host/ns     | d              | namespaceUri |
      | description of 1001 chars | $Ext   | $EXT/n4       | @repeat:d:1001 | description  |

  @D56 @Q2 @NFR-04
  Scenario: A description of 1000 characters is accepted
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value          |
      | name           | $Ext           |
      | namespaceUri   | $EXT/d1000     |
      | description    | @repeat:d:1000 |
      | vocabularyFile | @valid         |
    Then the response status is 201

  @D148
  Scenario: A blank description is treated as absent
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value       |
      | name           | $Ext        |
      | namespaceUri   | $EXT/blank  |
      | description    | @repeat: :4 |
      | vocabularyFile | @valid      |
    Then the response status is 201
    And the response field "description" is absent

  @D41 @D37 @negative
  Scenario Outline: A registration without a required part is refused: <missing>
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part   | value  |
      | <p1>   | <v1>   |
      | <p2>   | <v2>   |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<missing>"

    Examples:
      | missing        | p1           | v1      | p2             | v2     |
      | name           | namespaceUri | $EXT/m1 | vocabularyFile | @valid |
      | namespaceUri   | name         | $Ext    | vocabularyFile | @valid |
      | vocabularyFile | name         | $Ext    | namespaceUri   | $EXT/m2 |

  @D73 @negative
  Scenario: A relative sourceUri is refused
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value          |
      | name           | $Ext           |
      | namespaceUri   | $EXT/src       |
      | sourceUri      | files/terms.ttl |
      | vocabularyFile | @valid         |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "sourceUri"
    And the external vocabulary "$Ext" is not registered

  @D56 @D70 @D73 @negative
  Scenario Outline: Invalid alias URIs are refused: <case>
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value     |
      | name           | $Ext      |
      | namespaceUri   | $EXT/main |
      | aliasUris      | <alias1>  |
      | aliasUris      | <alias2>  |
      | vocabularyFile | @valid    |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "aliasUris"
    And the external vocabulary "$Ext" is not registered

    Examples:
      | case                         | alias1        | alias2        |
      | alias equals the namespace   | $EXT/main#    | $EXT/other    |
      | alias repeats another alias  | $EXT/al       | $EXT/al/      |
      | comma is not a separator     | a,b           | $EXT/z        |
      | relative alias               | relative      | $EXT/z        |

  @D56 @Q2 @NFR-04
  Scenario: 20 alias URIs are accepted and come back in the order sent
    When I register an external vocabulary "$Ext" with 20 alias URIs
    Then the response status is 201
    And the response field "aliasUris" has 20 entries
    And the response field "aliasUris[0]" equals "$EXT/alias/0"
    And the response field "aliasUris[19]" equals "$EXT/alias/19"

  @D56 @negative @Q2 @NFR-04
  Scenario: 21 alias URIs are refused
    When I register an external vocabulary "$Ext" with 21 alias URIs
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "aliasUris"
    And the external vocabulary "$Ext" is not registered

  @D15 @D41 @D76 @D169 @negative @Q2 @NFR-04
  Scenario: An external file over 5 MB is refused with 413
    Given the vocabulary file is a valid Turtle file of 5242881 bytes
    When I register the external vocabulary "$Ext" with namespace "$EXT/big"
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"
    And the external vocabulary "$Ext" is not registered

  @D15 @D41 @API-38 @negative
  Scenario: A registration that is not multipart/form-data gets 415
    When I send a POST request to "/external-vocabularies" with the body "{}" as "application/json"
    Then the response is a problem with status 415
    And the problem type is "urn:problem-type:simpl:unsupportedMediaType"

  @D52 @negative
  Scenario: An unknown registration field is refused
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value     |
      | name           | $Ext      |
      | namespaceUri   | $EXT/unk  |
      | version        | 1         |
      | vocabularyFile | @valid    |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "version"
    And the external vocabulary "$Ext" is not registered

  @D49 @negative
  Scenario Outline: An invalid external name in the path gives 400: <method> <path>
    When I send a <method> request to "<path>"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "path" named "name"
    And issue 1 has type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | method | path                                 |
      | GET    | /external-vocabularies/ab            |
      | GET    | /external-vocabularies/$ext/content  |
      | DELETE | /external-vocabularies/$ext          |

  @D54 @negative
  Scenario: The external name in a path must match exactly
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/case"
    When I send a GET request to "/external-vocabularies/$Ext!upper"
    Then the response is a problem with status 404

  @D52 @negative
  Scenario: A query parameter on DELETE is rejected and nothing is removed
    Given an external vocabulary "$Ext" is registered with namespace "$EXT/qd"
    When I send a DELETE request to "/external-vocabularies/$Ext?force=true"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "force"
    And the external vocabulary "$Ext" is registered

  @D52 @negative
  Scenario: A query parameter on the registration is rejected and nothing is registered
    When I send a multipart POST request to "/external-vocabularies?dryRun=true" with the parts:
      | part           | value     |
      | name           | $Ext      |
      | namespaceUri   | $EXT/qp   |
      | vocabularyFile | @valid    |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "dryRun"
    And the external vocabulary "$Ext" is not registered

  @D195 @negative
  Scenario: A registration field sent twice is rejected
    When I send a multipart POST request to "/external-vocabularies" with the parts:
      | part           | value     |
      | name           | $Ext      |
      | namespaceUri   | $EXT/tw1  |
      | namespaceUri   | $EXT/tw2  |
      | vocabularyFile | @valid    |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "namespaceUri"
    And the external vocabulary "$Ext" is not registered
