Feature: Register, read and remove external vocabularies
  An external vocabulary is a registered cached copy of a vocabulary
  Simpl-Open does not control. Registration checks Turtle syntax only. The
  name, the namespace URI and each alias URI are unique (compared without a
  trailing '#' or '/').

  Background:
    Given I am authenticated as "ga-admin"

  @D30 @D41 @D27 @D56 @D73 @D58
  Scenario: Register an external vocabulary
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                        |
      | name           | ${Terms}                                     |
      | namespaceUri   | http://ext.example.org/${uid}/terms/         |
      | aliasUris      | https://alias2.example.org/${uid}/terms      |
      | aliasUris      | https://alias1.example.org/${uid}/terms#     |
      | sourceUri      | https://source.example.org/${uid}/terms.ttl  |
      | description    | Cached copy for validation.                  |
      | vocabularyFile | fixture:external_terms.ttl                   |
    Then the response status is 201
    And the response field "name" is "${Terms}"
    And the response field "href" is "/external-vocabularies/${Terms}"
    And the response header "Location" is "/external-vocabularies/${Terms}"
    And the response field "namespaceUri" is "http://ext.example.org/${uid}/terms/"
    And the response field "aliasUris" is the list "https://alias2.example.org/${uid}/terms|https://alias1.example.org/${uid}/terms#"
    And the response field "sourceUri" is "https://source.example.org/${uid}/terms.ttl"
    And the response field "description" is "Cached copy for validation."
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" is a UTC time to the second
    And the response field "validationWarnings" is empty or absent

  @D41 @D16
  Scenario Outline: A registered external vocabulary can be read by name
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms" and alias "https://alias.example.org/${uid}/terms"
    And I am authenticated as "<user>"
    When I send a GET request to "/external-vocabularies/${Terms}"
    Then the response status is 200
    And the response field "name" is "${Terms}"
    And the response field "namespaceUri" is "http://ext.example.org/${uid}/terms"
    And the response field "aliasUris" is the list "https://alias.example.org/${uid}/terms"
    And the response field "createdBy" is "${email:ga-admin}"

    Examples:
      | user      |
      | ga-admin  |
      | ga-viewer |

  @D41 @D29 @D70
  Scenario: The content of an external vocabulary is downloaded by name
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    When I send a GET request to "/external-vocabularies/${Terms}/content"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response has an attachment named "${Terms}.ttl"
    And the response body is the content uploaded for the external vocabulary "Terms"

  @D30 @D41 @D16
  Scenario Outline: The content of an external vocabulary is downloaded by namespace or alias
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms" and alias "https://alias.example.org/${uid}/terms"
    And I am authenticated as "<user>"
    When I send a POST request to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "<uri>"}
      """
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response has an attachment named "${Terms}.ttl"
    And the response body is the content uploaded for the external vocabulary "Terms"

    Examples:
      | user      | uri                                       |
      | ga-admin  | http://ext.example.org/${uid}/terms/      |
      | ga-viewer | http://ext.example.org/${uid}/terms       |
      | ga-admin  | https://alias.example.org/${uid}/terms#   |

  @D66 @negative
  Scenario: Content by an unknown namespace gives 404 with the URI as sent
    When I send a POST request to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "http://nothing.example.org/${uid}/terms/"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri 'http://nothing.example.org/${uid}/terms/' was not found."
    And the problem has 1 issue
    And the problem has an issue in "body" named "namespaceUri"

  @D73 @negative
  Scenario: Content by a namespace that is not an absolute URI gives 400
    When I send a POST request to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "ext.example.org/terms"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "namespaceUri"

  @D41 @D22
  Scenario: The external vocabulary list is paged and in name order
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    Then the external vocabulary list contains "Terms" in name order

  @D41 @D21 @negative
  Scenario Outline: The external vocabulary list takes no filter or sort parameters
    When I send a GET request to "/external-vocabularies?<query>"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query          | param    |
      | sort=name      | sort     |
      | name=Terms     | name     |
      | pageSize=101   | pageSize |
      | page=0         | page     |

  @D41 @D66 @negative
  Scenario Outline: An unknown external vocabulary name gives 404
    When I send a <method> request to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${Ghost}' was not found."
    And the problem has 1 issue
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                  |
      | GET    | /external-vocabularies/${Ghost}         |
      | GET    | /external-vocabularies/${Ghost}/content |
      | DELETE | /external-vocabularies/${Ghost}         |

  @D41 @D42
  Scenario: Delete an external vocabulary; a later read gives 404 and it can be registered again
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    When I send a DELETE request to "/external-vocabularies/${Terms}"
    Then the response status is 204
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"
    When I send a GET request to "/external-vocabularies/${Terms}"
    Then the response is a problem with status 404
    When I send a GET request to "/external-vocabularies/${Terms}/content"
    Then the response is a problem with status 404
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                |
      | name           | ${Terms}                             |
      | namespaceUri   | http://ext.example.org/${uid}/terms  |
      | vocabularyFile | fixture:external_terms.ttl           |
    Then the response status is 201

  @D42 @D12
  Scenario: Registering clears the external reference warning for later versions; removal brings it back
    Given a vocabulary "Main" exists
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix t: <http://ext.example.org/${uid}/terms/> .
      ex:Thing a skos:Concept ; skos:related t:title .
      """
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 't' -> 'http://ext.example.org/${uid}/terms/'"
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix t: <http://ext.example.org/${uid}/terms/> .
      ex:Thing a skos:Concept ; skos:related t:title , t:creator .
      """
    Then the response status is 201
    And the response has no validation warnings
    Given the external vocabulary "Terms" is deleted
    When I create a new version of "Main" with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix t: <http://ext.example.org/${uid}/terms/> .
      ex:Thing a skos:Concept ; skos:related t:creator .
      """
    Then the response status is 201
    And the response has a validation warning "externalReferenceSkipped" with location "prefix 't' -> 'http://ext.example.org/${uid}/terms/'"

  @D42 @D41
  Scenario: Removing a referenced external vocabulary leaves stored internal versions untouched
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    And a vocabulary "Main" exists with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex: <https://example.org/vocab/${uid}/main#> .
      @prefix t: <http://ext.example.org/${uid}/terms/> .
      ex:Thing a skos:Concept ; skos:related t:title .
      """
    When I send a DELETE request to "/external-vocabularies/${Terms}"
    Then the response status is 204
    And version 1 of "Main" has version status "ACTIVE"
    When I send a GET request to "/content/${Main}/versions/1"
    Then the response body is the content uploaded for version 1 of "Main"

  @D30 @D41
  Scenario: Registration checks syntax only: semantic problems give no error and no warning
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                       |
      | name           | ${Terms}                                    |
      | namespaceUri   | http://ext.example.org/${uid}/terms         |
      | vocabularyFile | fixture:external_with_semantic_error.ttl    |
    Then the response status is 201
    And the response field "validationWarnings" is empty or absent

  @D30 @D41 @D9 @negative
  Scenario: Registration with invalid Turtle is rejected with one syntax issue
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                 |
      | name           | ${Terms}                              |
      | namespaceUri   | http://ext.example.org/${uid}/terms   |
      | vocabularyFile | fixture:syntax_error.ttl              |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with detail starting with "Encountered invalid Turtle syntax" and location matching "line 3(,.*)?"
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

  @D70 @negative
  Scenario Outline: Registration of a file with no statements is rejected
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                 |
      | name           | ${Terms}                              |
      | namespaceUri   | http://ext.example.org/${uid}/terms   |
      | vocabularyFile | <file>                                |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And the problem has an issue with detail "Vocabulary contains no statements"
    And every issue is in "body" named "vocabularyFile"
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

    Examples:
      | file                     |
      | empty                    |
      | fixture:s6_no_statements.ttl |

  @D30 @D56 @negative
  Scenario Outline: A registration with a registered name, in any case, gives 409
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                  |
      | name           | <name>                                 |
      | namespaceUri   | http://other.example.org/${uid}/terms  |
      | vocabularyFile | fixture:external_terms.ttl             |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And no external vocabulary is registered for namespace "http://other.example.org/${uid}/terms"

    Examples:
      | name           |
      | ${Terms}       |
      | ${Terms.upper} |

  @D30 @D55 @D66 @negative
  Scenario Outline: A registration with a registered namespace or alias gives 409 naming the URI as sent
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms" and alias "https://alias.example.org/${uid}/terms"
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                         |
      | name           | ${Other}                      |
      | namespaceUri   | <namespace>                   |
      | vocabularyFile | fixture:external_terms.ttl    |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '<namespace>' is already registered."
    When I send a GET request to "/external-vocabularies/${Other}"
    Then the response is a problem with status 404

    Examples:
      | namespace                                |
      | http://ext.example.org/${uid}/terms#     |
      | https://alias.example.org/${uid}/terms/  |

  @D30 @D55 @negative
  Scenario: An alias that is already registered as a namespace gives 409
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                  |
      | name           | ${Other}                               |
      | namespaceUri   | http://other.example.org/${uid}/terms  |
      | aliasUris      | http://ext.example.org/${uid}/terms/   |
      | vocabularyFile | fixture:external_terms.ttl             |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias 'http://ext.example.org/${uid}/terms/' is already registered."

  @D30 @D55 @negative
  Scenario: A new alias equal to a registered alias gives 409
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms" and alias "https://alias.example.org/${uid}/terms"
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                   |
      | name           | ${Other}                                |
      | namespaceUri   | http://other.example.org/${uid}/terms   |
      | aliasUris      | https://alias.example.org/${uid}/terms# |
      | vocabularyFile | fixture:external_terms.ttl              |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias 'https://alias.example.org/${uid}/terms#' is already registered."
    And no external vocabulary is registered for namespace "http://other.example.org/${uid}/terms"

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "Shared" exists
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                |
      | name           | ${Shared}                            |
      | namespaceUri   | http://ext.example.org/${uid}/terms  |
      | vocabularyFile | fixture:external_terms.ttl           |
    Then the response status is 201
    And vocabulary "Shared" has vocabulary status "ACTIVE"

  @D56 @D73 @D37 @negative
  Scenario Outline: Registration field rules
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                |
      | name           | <name>                               |
      | namespaceUri   | <namespace>                          |
      | <extra>        | <extraValue>                         |
      | vocabularyFile | fixture:external_terms.ttl           |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

    Examples:
      | field        | name        | namespace                            | extra       | extraValue                              |
      | name         | lower${uid} | http://ext.example.org/${uid}/terms  | description | ok                                      |
      | namespaceUri | ${Terms}    | ext.example.org/${uid}/terms         | description | ok                                      |
      | aliasUris    | ${Terms}    | http://ext.example.org/${uid}/terms  | aliasUris   | http://ext.example.org/${uid}/terms/    |
      | aliasUris    | ${Terms}    | http://ext.example.org/${uid}/terms  | aliasUris   | a,b                                     |
      | aliasUris    | ${Terms}    | http://ext.example.org/${uid}/terms  | aliasUris   | relative/path                           |
      | sourceUri    | ${Terms}    | http://ext.example.org/${uid}/terms  | sourceUri   | www.example.org/terms.ttl               |
      | description  | ${Terms}    | http://ext.example.org/${uid}/terms  | description | {{repeat:d:1001}}                       |
      | version      | ${Terms}    | http://ext.example.org/${uid}/terms  | version     | 1                                       |

  @D56 @negative
  Scenario: Repeated aliases are rejected
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                   |
      | name           | ${Terms}                                |
      | namespaceUri   | http://ext.example.org/${uid}/terms     |
      | aliasUris      | https://alias.example.org/${uid}/terms  |
      | aliasUris      | https://alias.example.org/${uid}/terms/ |
      | vocabularyFile | fixture:external_terms.ttl              |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

  @D56 @Q2 @NFR-04
  Scenario: Twenty aliases and a 1000-character description are accepted
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                   |
      | name           | ${Terms}                                |
      | namespaceUri   | http://ext.example.org/${uid}/terms     |
      | description    | {{repeat:d:1000}}                       |
      | aliasUris      | https://a01.example.org/${uid}          |
      | aliasUris      | https://a02.example.org/${uid}          |
      | aliasUris      | https://a03.example.org/${uid}          |
      | aliasUris      | https://a04.example.org/${uid}          |
      | aliasUris      | https://a05.example.org/${uid}          |
      | aliasUris      | https://a06.example.org/${uid}          |
      | aliasUris      | https://a07.example.org/${uid}          |
      | aliasUris      | https://a08.example.org/${uid}          |
      | aliasUris      | https://a09.example.org/${uid}          |
      | aliasUris      | https://a10.example.org/${uid}          |
      | aliasUris      | https://a11.example.org/${uid}          |
      | aliasUris      | https://a12.example.org/${uid}          |
      | aliasUris      | https://a13.example.org/${uid}          |
      | aliasUris      | https://a14.example.org/${uid}          |
      | aliasUris      | https://a15.example.org/${uid}          |
      | aliasUris      | https://a16.example.org/${uid}          |
      | aliasUris      | https://a17.example.org/${uid}          |
      | aliasUris      | https://a18.example.org/${uid}          |
      | aliasUris      | https://a19.example.org/${uid}          |
      | aliasUris      | https://a20.example.org/${uid}          |
      | vocabularyFile | fixture:external_terms.ttl              |
    Then the response status is 201
    And the response field "aliasUris" has 20 items

  @D56 @Q2 @NFR-04 @negative
  Scenario: Twenty-one aliases are rejected
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                   |
      | name           | ${Terms}                                |
      | namespaceUri   | http://ext.example.org/${uid}/terms     |
      | aliasUris      | https://a01.example.org/${uid}          |
      | aliasUris      | https://a02.example.org/${uid}          |
      | aliasUris      | https://a03.example.org/${uid}          |
      | aliasUris      | https://a04.example.org/${uid}          |
      | aliasUris      | https://a05.example.org/${uid}          |
      | aliasUris      | https://a06.example.org/${uid}          |
      | aliasUris      | https://a07.example.org/${uid}          |
      | aliasUris      | https://a08.example.org/${uid}          |
      | aliasUris      | https://a09.example.org/${uid}          |
      | aliasUris      | https://a10.example.org/${uid}          |
      | aliasUris      | https://a11.example.org/${uid}          |
      | aliasUris      | https://a12.example.org/${uid}          |
      | aliasUris      | https://a13.example.org/${uid}          |
      | aliasUris      | https://a14.example.org/${uid}          |
      | aliasUris      | https://a15.example.org/${uid}          |
      | aliasUris      | https://a16.example.org/${uid}          |
      | aliasUris      | https://a17.example.org/${uid}          |
      | aliasUris      | https://a18.example.org/${uid}          |
      | aliasUris      | https://a19.example.org/${uid}          |
      | aliasUris      | https://a20.example.org/${uid}          |
      | aliasUris      | https://a21.example.org/${uid}          |
      | vocabularyFile | fixture:external_terms.ttl              |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

  @D15 @D41 @negative @API-38
  Scenario: A registration that is not multipart/form-data is rejected with 415
    When I send a POST request to "/external-vocabularies" with JSON body
      """
      {"name": "${Terms}", "namespaceUri": "http://ext.example.org/${uid}/terms"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"

  @D16 @D41 @negative @OWASP-02
  Scenario: A viewer can neither register nor delete an external vocabulary
    Given an external vocabulary "Terms" is registered with namespace "http://ext.example.org/${uid}/terms"
    And I am authenticated as "ga-viewer"
    When I send a DELETE request to "/external-vocabularies/${Terms}"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And an external vocabulary is registered for namespace "http://ext.example.org/${uid}/terms"
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                  |
      | name           | ${Other}                               |
      | namespaceUri   | http://other.example.org/${uid}/terms  |
      | vocabularyFile | fixture:external_terms.ttl             |
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And no external vocabulary is registered for namespace "http://other.example.org/${uid}/terms"
