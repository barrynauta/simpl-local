Feature: Register, read and remove external vocabularies
  Test basis: the OpenAPI responses plus D30, D41 and D42 (no story of their own).

  Background:
    Given I am authenticated as "ga-admin"

  @D41 @D30 @D27 @API-36
  Scenario: An external vocabulary is registered
    When I register the external vocabulary "Dc" with:
      | field       | value                                   |
      | aliasUris   | https://alias.example/${runid}/dc#      |
      | description | Cached copy of DCMI terms.              |
      | sourceUri   | https://source.example/${runid}/dc.ttl  |
    Then the response status is 201
    And the response content type is "application/json"
    And the response header "Location" contains "/external-vocabularies/${ext:Dc}"
    And the response is the external vocabulary "Dc"
    And the field "aliasUris" holds "https://alias.example/${runid}/dc#"
    And the field "description" is "Cached copy of DCMI terms."
    And the field "sourceUri" is "https://source.example/${runid}/dc.ttl"

  @D30 @D41
  Scenario: Registration checks syntax only, without semantic validation or warnings
    When I register the external vocabulary "Loose" with:
      | field | value                              |
      | file  | s1-literal-for-object-property.ttl |
    Then the response status is 201
    And the response body has no field "validationWarnings"

  @D30 @D41 @D9 @negative
  Scenario: Invalid Turtle is rejected with one syntax issue
    When I register the external vocabulary "Broken" with:
      | field | value                  |
      | file  | syntax-error-line4.ttl |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And every issue is a vocabulary file issue
    And the problem has an issue whose detail starts with "Encountered invalid Turtle syntax"
    And the external vocabulary "Broken" does not exist

  @D30 @negative @API-46
  Scenario Outline: A registration that reuses an identity or a lookup key gives 409 (<case>)
    Given an external vocabulary "First" is registered with:
      | field        | value                                 |
      | namespaceUri | https://first.example/${runid}/ns#    |
      | aliasUris    | https://first-alias.example/${runid}/ns/ |
    When I register the external vocabulary "Second" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

    Examples:
      | case                         | field        | value                                     |
      | same name                    | name         | ${ext:First}                              |
      | same namespace               | namespaceUri | https://first.example/${runid}/ns#        |
      | namespace without hash       | namespaceUri | https://first.example/${runid}/ns         |
      | namespace equals an alias    | namespaceUri | https://first-alias.example/${runid}/ns   |
      | alias equals the namespace   | aliasUris    | https://first.example/${runid}/ns/        |

  @D41 @D37 @negative
  Scenario Outline: An incomplete registration is rejected with 400 (<case>)
    When I register the external vocabulary "Bad" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400
    And the external vocabulary "Bad" does not exist

    Examples:
      | case               | field        | value       |
      | namespace missing  | namespaceUri | <absent>    |
      | file missing       | file         | <absent>    |
      | name missing       | name         | <absent>    |
      | name lower case    | name         | e${runid}x  |

  @D15 @D41 @negative @API-46
  Scenario: A registration file over 5 MB gives 413
    When I register the external vocabulary "Huge" with:
      | field | value           |
      | file  | <bytes:5242881> |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D15 @D41 @negative @API-38 @API-46
  Scenario: A registration that is not multipart gives 415
    When I send POST "/external-vocabularies" with the content type "application/json" and the body:
      """
      {"name": "E${runid}Json", "namespaceUri": "https://json.example/${runid}"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D41 @D22 @BLG-col-paging
  Scenario: The external vocabularies are listed by name, paged
    Given an external vocabulary "Dc" is registered
    When I send GET "/external-vocabularies?pageSize=100"
    Then the response status is 200
    And the field "page" is the number 1
    And the field "pageSize" is the number 100
    And the page links "self, first" are present
    And the external list is sorted by name ascending
    And every list item has the fields "href, name, namespaceUri, createdBy, creationDate"

  @D41 @D22 @D21 @negative
  Scenario Outline: The external list takes no filter or sort parameters (<query>)
    When I send GET "/external-vocabularies?<query>"
    Then the response is a problem with status 400

    Examples:
      | query         |
      | sort=name     |
      | name=Dc       |
      | pageSize=101  |
      | page=0        |

  @D41
  Scenario: One external vocabulary is read by name
    Given an external vocabulary "Dc" is registered
    And I am authenticated as "ga-viewer"
    When I send GET "/external-vocabularies/${ext:Dc}"
    Then the response status is 200
    And the response is the external vocabulary "Dc"

  @D41 @D29
  Scenario: The content of an external vocabulary is downloaded by name
    Given an external vocabulary "Dc" is registered
    When I send GET "/external-vocabularies/${ext:Dc}/content"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response is a download named "${ext:Dc}.ttl"
    And the response body is the content of the external vocabulary "Dc"

  @D30 @D41
  Scenario Outline: The content of an external vocabulary is found by namespace (<case>)
    Given an external vocabulary "Dc" is registered with:
      | field        | value                                |
      | namespaceUri | https://dc.example/${runid}/terms/   |
      | aliasUris    | https://alias.example/${runid}/terms |
    When I send POST "/external-vocabularies/content" with the JSON body:
      """
      {"namespaceUri": "<uri>"}
      """
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body is the content of the external vocabulary "Dc"

    Examples:
      | case                | uri                                   |
      | namespace           | https://dc.example/${runid}/terms/    |
      | namespace with hash | https://dc.example/${runid}/terms#    |
      | alias               | https://alias.example/${runid}/terms/ |

  @D41 @negative
  Scenario: Content for an unknown namespace gives 404
    When I send POST "/external-vocabularies/content" with the JSON body:
      """
      {"namespaceUri": "https://none.example/${runid}/x#"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D41 @negative
  Scenario Outline: An unknown external vocabulary gives 404 with the external body (<method> <path>)
    When I send <method> "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${ext:Ghost}' was not found."
    And the problem has 1 issue
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                     |
      | GET    | /external-vocabularies/${ext:Ghost}         |
      | GET    | /external-vocabularies/${ext:Ghost}/content |
      | DELETE | /external-vocabularies/${ext:Ghost}         |

  @D41 @D42
  Scenario: Removing an external vocabulary deletes metadata and content and keeps internal vocabularies
    Given an external vocabulary "Dc" is registered
    And a vocabulary "User" exists with the content:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${extns:Dc}> .
      @prefix ex: <${own}> .
      ex:Scheme a skos:ConceptScheme ;
        dc:title "Title" .
      """
    When I send DELETE "/external-vocabularies/${ext:Dc}"
    Then the response status is 204
    And the external vocabulary "Dc" does not exist
    When I send GET "/external-vocabularies/${ext:Dc}/content"
    Then the response status is 404
    When I send GET "/content/${name:User}/versions/1"
    Then the response status is 200
    And the response body is the content of version 1 of "User"
    And the versions of "User" have the statuses "1=ACTIVE"

  @D30
  Scenario: A removed external vocabulary can be registered again
    Given an external vocabulary "Dc" is registered
    And the external vocabulary "Dc" is removed
    When I register the external vocabulary "Dc"
    Then the response status is 201
