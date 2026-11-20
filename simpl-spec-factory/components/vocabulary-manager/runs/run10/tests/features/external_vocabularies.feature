@external
Feature: Register, read and remove external vocabularies
  An external vocabulary is a registered cached copy, identified by its name; its namespace URI and
  alias URIs are unique lookup keys compared without a trailing '#' or '/' (D30, D41, D56, D73).
  Every scenario uses its own namespaces, because the store is shared (E4).

  Background:
    Given I use the token of "ga-admin"

  @D30 @D41 @D27 @D73 @D58
  Scenario: Register an external vocabulary
    When I register the external vocabulary "E" with namespace "${extns:e}/" and aliases "${extns:z}# ${extns:a}"
    Then the status is 201
    And the media type is "application/json"
    And the header "Location" is "/external-vocabularies/${E}"
    And the response field "href" is "/external-vocabularies/${E}"
    And the response field "name" is "${E}"
    And the response field "namespaceUri" is "${extns:e}/"
    And the response field "aliasUris" equals ["${extns:z}#", "${extns:a}"]
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" is a UTC timestamp
    And the response field "validationWarnings" is absent or empty

  @D41 @D56 @D110
  Scenario: Register with description and source; read it back by name
    When I register the external vocabulary "E" with namespace "${extns:e}", description "Cached copy for tests" and source "https://source.example.org/terms.ttl"
    Then the status is 201
    And the response field "aliasUris" equals []
    When I send "GET" to "/external-vocabularies/${E}"
    Then the status is 200
    And the response field "name" is "${E}"
    And the response field "namespaceUri" is "${extns:e}"
    And the response field "description" is "Cached copy for tests"
    And the response field "sourceUri" is "https://source.example.org/terms.ttl"
    And the response field "aliasUris" equals []
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "href" is "/external-vocabularies/${E}"

  @D41 @D29 @D70
  Scenario: Download the content of an external vocabulary by name
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    When I send "GET" to "/external-vocabularies/${E}/content" with the header "Accept" set to "text/turtle"
    Then the status is 200
    And the content type is exactly "text/turtle"
    And the header "Content-Disposition" names the file "${E}.ttl"
    And the body is the file of the external vocabulary "E"

  @D41 @D30
  Scenario Outline: Download the content of an external vocabulary by namespace or alias: <uri>
    Given an external vocabulary "E" is registered with namespace "${extns:e}" and aliases "${extns:ealias}#"
    When I send "POST" to "/external-vocabularies/content" with the header "Accept" set to "text/turtle" and the JSON body
      """
      {"namespaceUri": "<uri>"}
      """
    Then the status is 200
    And the content type is exactly "text/turtle"
    And the body is the file of the external vocabulary "E"

    Examples:
      | uri               |
      | ${extns:e}        |
      | ${extns:e}/       |
      | ${extns:e}#       |
      | ${extns:ealias}   |
      | ${extns:ealias}/  |

  @D66 @negative
  Scenario: Content by namespace for an unknown URI gives 404 with the URI as sent
    When I send "POST" to "/external-vocabularies/content" with the JSON body
      """
      {"namespaceUri": "${extns:unknown}/"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri '${extns:unknown}/' was not found."
    And the problem has an issue in "body" named "namespaceUri"

  @D73 @negative
  Scenario Outline: Content by namespace with an invalid body is rejected: <case>
    When I send "POST" to "/external-vocabularies/content" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | case             | body                                                 | name         |
      | relative URI     | {"namespaceUri": "dc/terms"}                         | namespaceUri |
      | missing URI      | {}                                                   | namespaceUri |
      | unknown property | {"namespaceUri": "https://a.example/x", "x": 1}      | x            |

  @D15 @API-38 @negative
  Scenario: Content by namespace that is not JSON gives 415
    When I send "POST" to "/external-vocabularies/content" with content type "text/plain" and the body
      """
      https://a.example/x
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D63 @API-37 @negative
  Scenario: Content by namespace with Accept application/json gives 406
    When I send "POST" to "/external-vocabularies/content" with the header "Accept" set to "application/json" and the JSON body
      """
      {"namespaceUri": "https://a.example/x"}
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D41 @D22
  Scenario: The external vocabulary list is paged and ordered by name
    Given an external vocabulary "Zulu" is registered with namespace "${extns:z}"
    And an external vocabulary "Alpha" is registered with namespace "${extns:a}"
    When I send "GET" to "/external-vocabularies?pageSize=100"
    Then the status is 200
    And the response field "page" equals 1
    And the response field "pageSize" equals 100
    And the items are sorted by name
    And the link "self" is present
    And the link "first" is present
    And the external vocabulary list contains "Zulu"
    And the external vocabulary list contains "Alpha"

  @D41 @D21 @negative
  Scenario Outline: The external vocabulary list takes no filter or sort parameters: <query>
    When I send "GET" to "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query        | param    |
      | name=Dublin  | name     |
      | sort=name    | sort     |
      | pageSize=101 | pageSize |
      | page=0       | page     |

  @D30 @D41 @API-46 @negative
  Scenario: A duplicate name is rejected with 409
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    When I register the external vocabulary "E" with namespace "${extns:other}"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D56 @negative
  Scenario: A name that differs only in case is a duplicate
    Given an external vocabulary "Case" is registered with namespace "${extns:e}"
    When I send a multipart "POST" to "/external-vocabularies" with these parts
      | name           | value                 |
      | vocabularyFile | @extfile:${extns:x}   |
      | name           | ${upper:Case}         |
      | namespaceUri   | ${extns:x}            |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D30 @D55 @D66 @negative
  Scenario Outline: A namespace or alias that is already registered is rejected with 409: <case>
    Given an external vocabulary "E" is registered with namespace "${extns:e}" and aliases "${extns:ealias}"
    When I register the external vocabulary "F" with namespace "<namespace>" and aliases "<aliases>"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '<conflict>' is already registered."

    Examples:
      | case                                 | namespace       | aliases          | conflict         |
      | same namespace with a trailing '#'   | ${extns:e}#     |                  | ${extns:e}#      |
      | namespace equals an existing alias   | ${extns:ealias}/ |                 | ${extns:ealias}/ |
      | alias equals an existing namespace   | ${extns:f}      | ${extns:e}/      | ${extns:e}/      |

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "Shared" exists
    When I register the external vocabulary "Shared" with namespace "${extns:shared}"
    Then the status is 201
    And "/vocabularies/${Shared}" answers 200

  @D30 @D41 @D9 @negative
  Scenario: Invalid Turtle is rejected with one syntax issue
    When I register the external vocabulary "E" with namespace "${extns:e}" and the file "syntax_error"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with a detail starting with "Encountered invalid Turtle syntax" and the location matching "^line 3(, column \d+)?$"
    When I send "GET" to "/external-vocabularies/${E}"
    Then the status is 404

  @D30 @D41
  Scenario: Registration runs no semantic validation and gives no warnings
    When I register the external vocabulary "E" with namespace "${extns:e}" and the file "external_with_semantic_error"
    Then the status is 201
    And the response field "validationWarnings" is absent or empty

  @D70 @negative
  Scenario: A file with no statements is rejected
    When I register the external vocabulary "E" with namespace "${extns:e}" and the file "empty"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue with the detail "Vocabulary contains no statements"
    And every issue is in "body" named "vocabularyFile"

  @D41 @D56 @D73 @negative
  Scenario Outline: An invalid registration is rejected: <case>
    When I send a multipart "POST" to "/external-vocabularies" with these parts
      | name           | value               |
      | vocabularyFile | @extfile:${extns:e} |
      | name           | <name>              |
      | namespaceUri   | <namespace>         |
      | <extra>        | <extra value>       |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<issue>"
    And the external vocabulary list does not contain "${E}"

    Examples:
      | case                           | name        | namespace       | extra       | extra value        | issue        |
      | relative namespace             | ${E}        | dc/terms        | description | ok                 | namespaceUri |
      | name breaks the pattern        | ${lower:E}  | ${extns:e}      | description | ok                 | name         |
      | relative alias                 | ${E}        | ${extns:e}      | aliasUris   | relative/alias     | aliasUris    |
      | comma list is one alias        | ${E}        | ${extns:e}      | aliasUris   | a,b                | aliasUris    |
      | alias equals namespace         | ${E}        | ${extns:e}      | aliasUris   | ${extns:e}#        | aliasUris    |
      | relative source                | ${E}        | ${extns:e}      | sourceUri   | terms.ttl          | sourceUri    |
      | description of 1001 characters | ${E}        | ${extns:e}      | description | @text:1001         | description  |
      | unknown field                  | ${E}        | ${extns:e}      | version     | 1                  | version      |

  @D56 @negative
  Scenario: A repeated alias is rejected
    When I register the external vocabulary "E" with namespace "${extns:e}" and aliases "${extns:x}/ ${extns:x}#"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"

  @D41 @negative
  Scenario: A registration without a namespace URI is rejected
    When I send a multipart "POST" to "/external-vocabularies" with these parts
      | name           | value               |
      | vocabularyFile | @extfile:${extns:e} |
      | name           | ${E}                |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "namespaceUri"

  @D56 @Q2 @NFR-04
  Scenario: 20 aliases are at the limit and are accepted, in the order sent
    When I register the external vocabulary "E" with namespace "${extns:e}" and aliases "${extns:a01} ${extns:a02} ${extns:a03} ${extns:a04} ${extns:a05} ${extns:a06} ${extns:a07} ${extns:a08} ${extns:a09} ${extns:a10} ${extns:a11} ${extns:a12} ${extns:a13} ${extns:a14} ${extns:a15} ${extns:a16} ${extns:a17} ${extns:a18} ${extns:a20} ${extns:a19}"
    Then the status is 201
    And the response field "aliasUris[18]" is "${extns:a20}"
    And the response field "aliasUris[19]" is "${extns:a19}"

  @D56 @Q2 @NFR-04 @negative
  Scenario: 21 aliases are above the limit
    When I register the external vocabulary "E" with namespace "${extns:e}" and aliases "${extns:a01} ${extns:a02} ${extns:a03} ${extns:a04} ${extns:a05} ${extns:a06} ${extns:a07} ${extns:a08} ${extns:a09} ${extns:a10} ${extns:a11} ${extns:a12} ${extns:a13} ${extns:a14} ${extns:a15} ${extns:a16} ${extns:a17} ${extns:a18} ${extns:a19} ${extns:a20} ${extns:a21}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"

  @D56 @Q2 @NFR-04
  Scenario: A description of 1000 characters is at the limit
    When I register the external vocabulary "E" with namespace "${extns:e}", description "${text:1000}" and source "https://source.example.org/x.ttl"
    Then the status is 201

  @D15 @D41 @Q2 @NFR-04 @negative
  Scenario: A file above 5 MB gives 413
    When I send a multipart "POST" to "/external-vocabularies" with these parts
      | name           | value          |
      | vocabularyFile | @size:5242881  |
      | name           | ${E}           |
      | namespaceUri   | ${extns:e}     |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the external vocabulary list does not contain "${E}"

  @D15 @D41 @Q2 @NFR-04
  Scenario: A file of exactly 5 242 880 bytes is at the limit and is accepted
    When I send a multipart "POST" to "/external-vocabularies" with these parts
      | name           | value          |
      | vocabularyFile | @size:5242880  |
      | name           | ${E}           |
      | namespaceUri   | ${extns:e}     |
    Then the status is 201

  @D63 @API-37 @negative
  Scenario Outline: An external read with an Accept header it cannot serve gives 406: <path>
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    When I send "GET" to "<path>" with the header "Accept" set to "<accept>"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

    Examples:
      | path                                | accept           |
      | /external-vocabularies              | text/turtle      |
      | /external-vocabularies/${E}         | application/xml  |
      | /external-vocabularies/${E}/content | application/json |

  @D15 @D41 @API-38 @negative
  Scenario: A registration that is not multipart gives 415
    When I send "POST" to "/external-vocabularies" with the JSON body
      """
      {"name": "${E}", "namespaceUri": "${extns:e}"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D41 @D42
  Scenario: Delete an external vocabulary; its metadata and content are gone and it can be registered again
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    When I send "DELETE" to "/external-vocabularies/${E}"
    Then the status is 204
    When I send "GET" to "/external-vocabularies/${E}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${E}' was not found."
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "name"
    When I send "GET" to "/external-vocabularies/${E}/content" with the header "Accept" set to "text/turtle"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send "POST" to "/external-vocabulary/searches" with the JSON body
      """
      {"namespaceUri": "${extns:e}"}
      """
    Then the response has 0 items
    When I register the external vocabulary "E" with namespace "${extns:e}"
    Then the status is 201

  @D41 @D66 @negative
  Scenario Outline: <method> <path> for an unknown external vocabulary gives 404
    When I send "<method>" to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                    |
      | GET    | /external-vocabularies/${Missing}         |
      | DELETE | /external-vocabularies/${Missing}         |
      | GET    | /external-vocabularies/${Missing}/content |

  @D49 @negative
  Scenario Outline: An external vocabulary name that breaks the pattern gives 400: <method> <path>
    When I send "<method>" to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                               |
      | GET    | /external-vocabularies/bad-name    |
      | DELETE | /external-vocabularies/bad-name    |
      | GET    | /external-vocabularies/ab/content  |

  @D42
  Scenario: Removing a referenced external vocabulary leaves the internal versions untouched
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    And the file variable "ref" is "${extns:e}#"
    And a vocabulary "A" exists with the file "reference"
    When I send "DELETE" to "/external-vocabularies/${E}"
    Then the status is 204
    And vocabulary "A" has the status "ACTIVE"
    And version 1 of vocabulary "A" has the status "ACTIVE"
    When I send "GET" to "/content/${A}/versions/1" with the header "Accept" set to "text/turtle"
    Then the body is the file of version 1 of "A"

  @D52 @negative
  Scenario: A query parameter on DELETE gives 400
    Given an external vocabulary "E" is registered with namespace "${extns:e}"
    When I send "DELETE" to "/external-vocabularies/${E}?force=true"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And "/external-vocabularies/${E}" answers 200

  @D77 @negative
  Scenario: GET on the fixed content path gives 405
    When I send "GET" to "/external-vocabularies/content"
    Then the response is a problem with status 405
