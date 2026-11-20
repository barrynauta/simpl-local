Feature: Register, read and remove external vocabularies
  External vocabularies are cached copies used to resolve references (D30, D41, D42, D56, D66, D70, D73).
  Registration checks Turtle syntax only.

  Background:
    Given I am a vocabulary administrator

  @D41 @D30 @D27 @D56 @D73 @D58
  Scenario: An external vocabulary is registered
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}/"
    And the request has an extra part "aliasUris" with value "${URI:DcSecond}/"
    And the request has an extra part "aliasUris" with value "${URI:DcFirst}#"
    And the request has an extra part "description" with value "Cached copy for validation ${SID}"
    And the request has an extra part "sourceUri" with value "https://source.example.org/${SID}/dc.ttl"
    When I submit the request
    Then the response status is 201
    And the response header "Location" is "/external-vocabularies/${Dc}"
    And the response field "href" is "/external-vocabularies/${Dc}"
    And the response field "name" is "${Dc}"
    And the response field "namespaceUri" is "${URI:Dc}/"
    And the response field "aliasUris" lists "${URI:DcSecond}/, ${URI:DcFirst}#"
    And the response field "description" is "Cached copy for validation ${SID}"
    And the response field "sourceUri" is "https://source.example.org/${SID}/dc.ttl"
    And the response field "createdBy" is "${EMAIL:admin}"
    And the response field "creationDate" is a UTC timestamp to the second
    When I send a GET request to "/external-vocabularies/${Dc}"
    Then the response status is 200
    And the response field "name" is "${Dc}"
    And the response field "namespaceUri" is "${URI:Dc}/"
    And the response field "aliasUris" lists "${URI:DcSecond}/, ${URI:DcFirst}#"
    When I send a GET request to "/external-vocabularies/${Dc}/content"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response header "Content-Disposition" names the attachment "${Dc}.ttl"
    And the response body equals the uploaded file

  @D110 @D56
  Scenario: aliasUris is present as an empty list when none were sent
    Given an external vocabulary registration request for "Plain" with namespace "${URI:Plain}"
    When I submit the request
    Then the response status is 201
    And the response field "aliasUris" is an empty list

  @D30 @D10
  Scenario: Registration checks syntax only: semantic errors and unknown namespaces give no error and no warning
    Given an external vocabulary registration request for "Loose" with namespace "${URI:Loose}"
    And the request file is
      """
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix l: <${URI:Loose}#> .
      @prefix other: <${URI:NotRegistered}#> .
      l:mixed a owl:ObjectProperty , owl:DatatypeProperty .
      l:Thing l:mixed other:Thing .
      """
    When I submit the request
    Then the response status is 201
    And the response has no validation warnings field or an empty one

  @D41
  Scenario: Relative IRIs in an external file resolve against its namespace URI
    Given an external vocabulary registration request for "Rel" with namespace "${URI:Rel}#"
    And the request file is
      """
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      <#Term> a rdfs:Class ; rdfs:label "Term"@en .
      """
    When I submit the request
    Then the response status is 201

  @D41 @D9 @negative
  Scenario: An external file with a syntax error is rejected with one issue
    Given an external vocabulary registration request for "Bad" with namespace "${URI:Bad}"
    And the request file is the fixture "syntax-error.ttl"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "body" named "vocabularyFile"
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax" and location matching "line 3(, column \d+)?"
    And no external vocabulary "Bad" is stored

  @D70 @negative
  Scenario Outline: An external file with no statements is rejected: <case>
    Given an external vocabulary registration request for "Empty" with namespace "${URI:Empty}"
    And <file step>
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "body" named "vocabularyFile"
    And the problem has an issue with detail "Vocabulary contains no statements"
    And no external vocabulary "Empty" is stored

    Examples:
      | case          | file step                                 |
      | empty file    | the request file is empty                 |
      | prefixes only | the request file is the fixture "prefixes-only.ttl" |

  @D30 @D56 @negative @API-46
  Scenario: A duplicate external name is rejected, without regard to case
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}"
    And an external vocabulary registration request for "Other" with namespace "${URI:Other}"
    And the request field "name" is "${upper:Dc}"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And no external vocabulary "Other" is stored

  @D30 @D55 @D66 @D73 @negative
  Scenario Outline: A namespace or alias already registered is rejected: <field> <value>
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}" and aliases "${URI:DcAlias}"
    And an external vocabulary registration request for "Other" with namespace "${URI:Other}"
    And the request <how> "<value>"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '<value>' is already registered."
    And no external vocabulary "Other" is stored

    Examples:
      | field       | how                                      | value             |
      | namespace   | field "namespaceUri" is                  | ${URI:Dc}/        |
      | namespace   | field "namespaceUri" is                  | ${URI:DcAlias}#   |
      | alias       | has an extra part "aliasUris" with value | ${URI:Dc}#        |
      | alias       | has an extra part "aliasUris" with value | ${URI:DcAlias}    |

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "Shared" exists
    And an external vocabulary registration request for "Shared" with namespace "${URI:Shared}"
    When I submit the request
    Then the response status is 201
    And exactly one vocabulary "Shared" is stored

  @D56 @negative
  Scenario Outline: Invalid alias URIs are rejected: <case>
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    And the request has an extra part "aliasUris" with value "<first>"
    And the request has an extra part "aliasUris" with value "<second>"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary "Dc" is stored

    Examples:
      | case                          | first             | second            |
      | alias equals the namespace    | ${URI:Dc}#        | ${URI:DcAlias}    |
      | repeated alias                | ${URI:DcAlias}    | ${URI:DcAlias}/   |
      | comma-separated is one URI    | ${URI:DcAlias}    | a,b               |

  @D56 @Q2 @NFR-04
  Scenario: Twenty alias URIs are accepted
    Given an external vocabulary registration request for "Many" with namespace "${URI:Many}"
    And the request has 20 alias URIs
    When I submit the request
    Then the response status is 201
    And the response field "aliasUris" has 20 entries

  @D56 @Q2 @NFR-04 @negative
  Scenario: Twenty-one alias URIs are rejected
    Given an external vocabulary registration request for "Many" with namespace "${URI:Many}"
    And the request has 21 alias URIs
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary "Many" is stored

  @D73 @negative
  Scenario Outline: A URI that is not absolute is rejected: <field>
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    And the request <how> "<value>"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary "Dc" is stored

    Examples:
      | field        | how                                      | value          |
      | namespaceUri | field "namespaceUri" is                  | purl.org/dc    |
      | namespaceUri | field "namespaceUri" is                  | /relative/path |
      | aliasUris    | has an extra part "aliasUris" with value | not a uri      |
      | sourceUri    | has an extra part "sourceUri" with value | dc.ttl         |

  @D56 @D37 @negative
  Scenario Outline: Invalid external registration fields are rejected: <field>
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    And <change>
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary "Dc" is stored

    Examples:
      | field          | change                                                     |
      | name           | the request field "name" is "lower${SID}"                  |
      | name           | the request field "name" is removed                        |
      | namespaceUri   | the request field "namespaceUri" is removed                |
      | vocabularyFile | the request field "vocabularyFile" is removed              |
      | colour         | the request has an extra part "colour" with value "blue"   |

  @D56 @Q2 @NFR-04
  Scenario: An external description of 1000 characters is accepted
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    And the request has an extra part "description" with value "x"
    And the request field "description" has 1000 characters
    When I submit the request
    Then the response status is 201

  @D56 @Q2 @NFR-04 @negative
  Scenario: An external description of 1001 characters is rejected
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    And the request has an extra part "description" with value "x"
    And the request field "description" has 1001 characters
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And no external vocabulary "Dc" is stored

  @D15 @negative @API-38
  Scenario: An external registration that is not multipart/form-data gets 415
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    When I submit the request as "application/json"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no external vocabulary "Dc" is stored

  @D41 @D22
  Scenario: The external vocabulary list is paged and sorted by name
    Given an external vocabulary "Bravo" is registered with namespace "${URI:Bravo}"
    And an external vocabulary "Alpha" is registered with namespace "${URI:Alpha}"
    When I send a GET request to "/external-vocabularies?pageSize=100"
    Then the response status is 200
    And the response field "pageSize" is 100
    And the response field "self" is present
    And the response field "first" is present
    And the external vocabulary list holds "${Alpha}" before "${Bravo}"

  @D41 @D21 @negative
  Scenario Outline: The external vocabulary list has no filter or sort parameters: <query>
    When I send a GET request to "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query         | parameter |
      | sort=name     | sort      |
      | name=Dc       | name      |
      | pageSize=101  | pageSize  |
      | page=0        | page      |

  @D41 @D29 @D66 @D30 @D73
  Scenario: External content is found by namespace URI or alias, normalised
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}" and aliases "${URI:DcAlias}"
    When I send a POST request to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "${URI:Dc}/"}
      """
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response header "Content-Disposition" names the attachment "${Dc}.ttl"
    And the response body equals the content of external vocabulary "Dc"
    When I send a POST request to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "${URI:DcAlias}#"}
      """
    Then the response status is 200
    And the response body equals the content of external vocabulary "Dc"

  @D66 @negative
  Scenario: External content for an unknown namespace gives 404 naming the URI as sent
    When I send a POST request to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "${URI:Unknown}/"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri '${URI:Unknown}/' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "body" named "namespaceUri"

  @D73 @negative
  Scenario Outline: An invalid external content request is rejected: <body>
    When I send a POST request to "/external-vocabularies/content" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<name>"

    Examples:
      | body                                          | name         |
      | {"namespaceUri": "relative/path"}             | namespaceUri |
      | {}                                            | namespaceUri |
      | {"namespaceUri": "${URI:A}", "extra": true}   | extra        |

  @D63 @negative
  Scenario: External content by namespace with an Accept header it cannot serve gets 406
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}"
    When I send a POST request to "/external-vocabularies/content" with JSON and the header "Accept" set to "application/json"
      """
      {"namespaceUri": "${URI:Dc}"}
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D41 @D42
  Scenario: Deleting an external vocabulary removes its metadata and content
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}"
    When I send a DELETE request to "/external-vocabularies/${Dc}"
    Then the response status is 204
    When I send a GET request to "/external-vocabularies/${Dc}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${Dc}' was not found."
    When I send a GET request to "/external-vocabularies/${Dc}/content"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send a POST request to "/external-vocabularies/content" with JSON
      """
      {"namespaceUri": "${URI:Dc}"}
      """
    Then the response status is 404

  @D41 @D42
  Scenario: Deleting an external vocabulary does not touch internal vocabularies that reference it
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}"
    And a vocabulary "A" exists with the Turtle
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <${NS:A}> .
      @prefix dc: <${URI:Dc}#> .
      ex:Concept1 a skos:Concept ; skos:related dc:Term .
      """
    When I send a DELETE request to "/external-vocabularies/${Dc}"
    Then the response status is 204
    And version 1 of vocabulary "A" has status "ACTIVE"
    When I send a GET request to "/content/${A}"
    Then the response body equals the content of version 1 of vocabulary "A"

  @D30
  Scenario: A removed external vocabulary can be registered again
    Given an external vocabulary "Dc" is registered with namespace "${URI:Dc}"
    When I send a DELETE request to "/external-vocabularies/${Dc}"
    Then the response status is 204
    Given an external vocabulary registration request for "Dc" with namespace "${URI:Dc}"
    When I submit the request
    Then the response status is 201

  @D41 @D66 @negative
  Scenario Outline: An unknown external name gives 404 with one issue: <method> <path>
    When I send a <method> request to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${Missing}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                  |
      | GET    | /external-vocabularies/${Missing}         |
      | GET    | /external-vocabularies/${Missing}/content |
      | DELETE | /external-vocabularies/${Missing}         |

  @D49 @negative
  Scenario Outline: An invalid external name in the path gives 400: <method> <path>
    When I send a <method> request to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "name" with type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | method | path                               |
      | GET    | /external-vocabularies/dc          |
      | GET    | /external-vocabularies/bad-name/content |
      | DELETE | /external-vocabularies/Ab          |
