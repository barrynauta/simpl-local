Feature: Register, read and remove external vocabularies
  The test basis is the OpenAPI plus D30, D41, D42, D56, D66, D70 and D73 (no story exists).

  @D41 @D30 @D27 @D73 @D58 @API-12
  Scenario: Registering an external vocabulary returns 201 with its metadata
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                                    |
      | name           | ${ext:Dc}                                |
      | namespaceUri   | ${extns:Dc}/                             |
      | aliasUris      | ${extns:DcB}#                            |
      | aliasUris      | ${extns:DcA}#                            |
      | sourceUri      | https://www.example.org/dc/terms.ttl     |
      | description    | Cached copy of the test terms.           |
      | vocabularyFile | @file:external-dc.ttl                    |
    Then the response status is 201
    And the response header "Location" is "/external-vocabularies/${ext:Dc}"
    And the response field "href" is "/external-vocabularies/${ext:Dc}"
    And the response field "name" is "${ext:Dc}"
    And the response field "namespaceUri" is "${extns:Dc}/"
    And the response field "aliasUris" is the list "${extns:DcB}#, ${extns:DcA}#"
    And the response field "sourceUri" is "https://www.example.org/dc/terms.ttl"
    And the response field "description" is "Cached copy of the test terms."
    And the response field "createdBy" is "${adminEmail}"
    And the response field "creationDate" is a time in UTC to the second
    And the response has no field "validationWarnings" or it is an empty list

  @D41 @D30
  Scenario: A registered external vocabulary can be read by name
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#" and alias "${extns:DcAlias}#"
    When I send a "GET" request to "/external-vocabularies/${ext:Dc}"
    Then the response status is 200
    And the response field "name" is "${ext:Dc}"
    And the response field "namespaceUri" is "${extns:Dc}#"
    And the response field "aliasUris" is the list "${extns:DcAlias}#"
    And the response field "createdBy" is "${adminEmail}"

  @D41 @D29 @D70
  Scenario: The content of an external vocabulary is downloaded by name, byte for byte
    Given an external vocabulary "Dc" is registered
    When I send a "GET" request to "/external-vocabularies/${ext:Dc}/content"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response is a download named "${ext:Dc}.ttl"
    And the response body is the uploaded content of the external vocabulary "Dc"

  @D30 @D41 @D66
  Scenario Outline: The content of an external vocabulary is found by namespace or alias URI (<case>)
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#" and alias "${extns:DcAlias}/"
    When I send a "POST" request to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "<uri>"}
      """
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response is a download named "${ext:Dc}.ttl"
    And the response body is the uploaded content of the external vocabulary "Dc"

    Examples:
      | case                     | uri                |
      | namespace as registered  | ${extns:Dc}#       |
      | namespace without hash   | ${extns:Dc}        |
      | namespace with slash     | ${extns:Dc}/       |
      | alias                    | ${extns:DcAlias}   |

  @D66 @negative
  Scenario: Content lookup by an unknown namespace gives 404 with the URI as sent
    When I send a "POST" request to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "${extns:Nothing}/"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri '${extns:Nothing}/' was not found."
    And the problem has exactly one issue, in "body" named "namespaceUri"

  @D73 @D52 @negative
  Scenario Outline: An invalid content lookup body is rejected (<case>)
    When I send a "POST" request to "/external-vocabularies/content" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case             | body                                                          | name         |
      | relative URI     | {"namespaceUri": "dc/terms"}                                  | namespaceUri |
      | missing          | {}                                                            | namespaceUri |
      | unknown property | {"namespaceUri": "https://a.example.org/x", "name": "Abc"}    | name         |

  @D41 @D66 @negative
  Scenario Outline: An external vocabulary that does not exist gives 404 on <method> <path>
    When I send a "<method>" request to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${ext:Ghost}' was not found."
    And the problem has exactly one issue, in "path" named "name"

    Examples:
      | method | path                                       |
      | GET    | /external-vocabularies/${ext:Ghost}         |
      | GET    | /external-vocabularies/${ext:Ghost}/content |
      | DELETE | /external-vocabularies/${ext:Ghost}         |

  @D41 @D42
  Scenario: Removing an external vocabulary returns 204 and later reads give 404
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#"
    When I send a "DELETE" request to "/external-vocabularies/${ext:Dc}"
    Then the response status is 204
    When I send a "GET" request to "/external-vocabularies/${ext:Dc}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send a "GET" request to "/external-vocabularies/${ext:Dc}/content"
    Then the response status is 404
    And no external vocabulary with namespace "${extns:Dc}#" is stored

  @D41 @D42
  Scenario: Removing an external vocabulary does not touch internal vocabularies that reference it
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}/"
    And a vocabulary "Health" exists
    When I create a new version of "Health" with a file that declares the prefix "dc" for "${extns:Dc}/"
    Then the response status is 201
    When I send a "DELETE" request to "/external-vocabularies/${ext:Dc}"
    Then the response status is 204
    And the vocabulary "Health" has 2 versions
    And version 2 of "Health" has the status "ACTIVE"

  @D41
  Scenario: A removed external vocabulary can be registered again
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#"
    And the external vocabulary "Dc" was removed
    When I register the external vocabulary "Dc" with namespace "${extns:Dc}#"
    Then the response status is 201

  @D41 @D22
  Scenario: The external vocabulary list is paged and ordered by name
    Given an external vocabulary "Alpha" is registered
    When I send a "GET" request to "/external-vocabularies?pageSize=100"
    Then the response status is 200
    And the response field "page" is 1
    And the response field "pageSize" is 100
    And totalPages is total divided by pageSize, rounded up
    And the items are sorted by "name" by code point
    And the paging link "self" is "/external-vocabularies" with the parameters "page=1&pageSize=100"

  @D41 @D21 @D22 @negative
  Scenario Outline: The external vocabulary list has no filter or sort parameters (<query>)
    When I send a "GET" request to "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"

    Examples:
      | query          | name     |
      | sort=name      | sort     |
      | name=Abc       | name     |
      | pageSize=101   | pageSize |
      | page=0         | page     |

  @D41 @D9 @D30 @negative
  Scenario: Registering invalid Turtle gives one syntax issue and stores nothing
    When I register the external vocabulary "Broken" with the file "syntax-error.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax" and whose location matches "^line 3(, column \d+)?$"
    And no external vocabulary with namespace "https://ext.example.org/${run}/Broken#" is stored

  @D30 @D41
  Scenario: Registration runs no semantic validation and gives no warnings
    When I register the external vocabulary "Loose" with the file "s5-disjoint.ttl"
    Then the response status is 201
    And the response has no field "validationWarnings" or it is an empty list

  @D70 @negative
  Scenario: Registering a file with no statements is rejected
    When I register the external vocabulary "Hollow" with the file "s6-comments-only.ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And the problem has an issue with the detail "Vocabulary contains no statements"
    And no external vocabulary with namespace "https://ext.example.org/${run}/Hollow#" is stored

  @D30 @D56 @negative @API-46
  Scenario: Registering an existing name is rejected with 409, also in another case
    Given an external vocabulary "Dc" is registered
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                    |
      | name           | ${ext:Dc}                |
      | namespaceUri   | ${extns:Other}#          |
      | vocabularyFile | @file:external-dc.ttl    |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem type follows API-46 and API-47
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                    |
      | name           | DC${run}                 |
      | namespaceUri   | ${extns:Other}#          |
      | vocabularyFile | @file:external-dc.ttl    |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And no external vocabulary with namespace "${extns:Other}#" is stored

  @D30 @D55 @D66 @negative
  Scenario Outline: A namespace or alias that is already registered is rejected with 409 (<case>)
    Given an external vocabulary "Dc" is registered with namespace "${extns:Dc}#" and alias "${extns:DcAlias}#"
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                    |
      | name           | ${ext:Second}            |
      | namespaceUri   | <namespace>              |
      | aliasUris      | <alias>                  |
      | vocabularyFile | @file:external-dc.ttl    |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '<conflict>' is already registered."
    And no external vocabulary named "Second" is stored

    Examples:
      | case                               | namespace         | alias               | conflict           |
      | same namespace, other trailing '/' | ${extns:Dc}/      | ${extns:Fresh2}#    | ${extns:Dc}/       |
      | namespace equals an alias          | ${extns:DcAlias}  | ${extns:Fresh2}#    | ${extns:DcAlias}   |
      | alias equals the namespace         | ${extns:Fresh}#   | ${extns:Dc}         | ${extns:Dc}        |

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "Shared" exists
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                  |
      | name           | ${Shared}              |
      | namespaceUri   | ${extns:Shared}#       |
      | vocabularyFile | @file:external-dc.ttl  |
    Then the response status is 201

  @D56 @Q2 @NFR-04
  Scenario: 20 alias URIs are accepted
    When I register the external vocabulary "Many" with 20 alias URIs
    Then the response status is 201
    And the response field "aliasUris[19]" is "https://ext.example.org/${run}/Many/alias19#"

  @D56 @Q2 @negative @NFR-04
  Scenario: 21 alias URIs are rejected
    When I register the external vocabulary "TooMany" with 21 alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary with namespace "https://ext.example.org/${run}/TooMany#" is stored

  @D56 @D70 @D73 @negative
  Scenario Outline: An invalid registration field is rejected (<case>)
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                  |
      | name           | <name>                 |
      | namespaceUri   | <namespace>            |
      | aliasUris      | <alias>                |
      | sourceUri      | <source>               |
      | description    | <description>          |
      | vocabularyFile | @file:external-dc.ttl  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary with namespace "${extns:Bad}#" is stored

    Examples:
      | case                         | field        | name        | namespace      | alias                              | source                 | description      |
      | name lower case              | name         | low${run}   | ${extns:Bad}#  | @omit                              | @omit                  | @omit            |
      | name too short               | name         | Ab          | ${extns:Bad}#  | @omit                              | @omit                  | @omit            |
      | name missing                 | name         | @omit       | ${extns:Bad}#  | @omit                              | @omit                  | @omit            |
      | namespace missing            | namespaceUri | Bad${run}   | @omit          | @omit                              | @omit                  | @omit            |
      | relative namespace           | namespaceUri | Bad${run}   | dc/terms       | ${extns:Bad}#                      | @omit                  | @omit            |
      | relative alias               | aliasUris    | Bad${run}   | ${extns:Bad}#  | dc/terms                           | @omit                  | @omit            |
      | comma is not a separator     | aliasUris    | Bad${run}   | ${extns:Bad}#  | not a uri,https://a.example.org/x  | @omit                  | @omit            |
      | alias equals the namespace   | aliasUris    | Bad${run}   | ${extns:Bad}#  | ${extns:Bad}/                      | @omit                  | @omit            |
      | relative source              | sourceUri    | Bad${run}   | ${extns:Bad}#  | @omit                              | terms.ttl              | @omit            |
      | description 1001 characters  | description  | Bad${run}   | ${extns:Bad}#  | @omit                              | @omit                  | ${repeat:d:1001} |

  @D56 @negative
  Scenario: A repeated alias is rejected
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                  |
      | name           | Bad${run}              |
      | namespaceUri   | ${extns:Bad}#          |
      | aliasUris      | ${extns:BadAlias}#     |
      | aliasUris      | ${extns:BadAlias}/     |
      | vocabularyFile | @file:external-dc.ttl  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary with namespace "${extns:Bad}#" is stored

  @D56 @Q2 @NFR-04
  Scenario: A description of 1000 characters is accepted on registration
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                  |
      | name           | ${ext:Long}            |
      | namespaceUri   | ${extns:Long}#         |
      | description    | ${repeat:d:1000}       |
      | vocabularyFile | @file:external-dc.ttl  |
    Then the response status is 201

  @D41 @D15 @negative @API-38
  Scenario: A registration that is not multipart/form-data is rejected with 415
    When I send a "POST" request to "/external-vocabularies" with content type "application/json" and body
      """
      {"name": "Json${run}", "namespaceUri": "https://ext.example.org/${run}/Json#"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no external vocabulary with namespace "https://ext.example.org/${run}/Json#" is stored

  @D52 @negative
  Scenario: An unknown form field on registration is rejected
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value                  |
      | name           | Bad${run}              |
      | namespaceUri   | ${extns:Bad}#          |
      | createdBy      | someone@example.org    |
      | vocabularyFile | @file:external-dc.ttl  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "createdBy"
    And no external vocabulary with namespace "${extns:Bad}#" is stored


  @D52 @negative
  Scenario: A query parameter on DELETE is rejected and nothing is removed
    Given an external vocabulary "Dc" is registered
    When I send a "DELETE" request to "/external-vocabularies/${ext:Dc}?x=1"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "x"
    And the external vocabulary "Dc" is stored
