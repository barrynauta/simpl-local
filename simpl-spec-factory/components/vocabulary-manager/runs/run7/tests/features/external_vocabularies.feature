Feature: External vocabularies via the API
  Register, read, download and delete cached copies of external vocabularies
  (D30, D41, D42, D56, D66, D70, D73).

  @D41 @D27 @D30 @D73 @D58
  Scenario: An external vocabulary is registered and returned as sent
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field     | value                                |
      | aliasUris | https://alias.example.org/${rid}/b/  |
      | aliasUris | https://alias.example.org/${rid}/a#  |
    Then the response status is 201
    And the response header "Location" is "/external-vocabularies/${E:Dc}"
    And the response field "href" equals "/external-vocabularies/${E:Dc}"
    And the response field "name" equals "${E:Dc}"
    And the response field "namespaceUri" equals "${NS:Dc}"
    And the response field "aliasUris" equals ["https://alias.example.org/${rid}/b/", "https://alias.example.org/${rid}/a#"]
    And the response field "sourceUri" equals "https://source.example.org/${rid}/Dc.ttl"
    And the response field "description" equals "Cached copy of Dc for validation."
    And the response field "createdBy" equals "${EMAIL:ga-admin}"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "validationWarnings" is absent
    When "ga-viewer" gets external vocabulary "Dc"
    Then the response status is 200
    And the response field "name" equals "${E:Dc}"
    And the response field "aliasUris" equals ["https://alias.example.org/${rid}/b/", "https://alias.example.org/${rid}/a#"]
    And the external vocabulary list contains "Dc"

  @D56
  Scenario: Registration needs only a name, a namespace URI and a file
    When "ga-admin" registers external vocabulary "Min" with the fields
      | field       | value  |
      | description | <omit> |
      | sourceUri   | <omit> |
    Then the response status is 201

  @D41 @D22
  Scenario: The external vocabulary list is paged and ordered by name
    Given external vocabulary "Dc" is registered
    When "ga-viewer" sends GET "/external-vocabularies?pageSize=100"
    Then the response status is 200
    And the response fields "page, pageSize, total, totalPages, self, first" are present
    And the response field "pageSize" equals 100
    And the items are in name order by code point

  @D41 @D21 @negative
  Scenario Outline: The external vocabulary list takes no filter or sort parameter (<query>)
    When "ga-admin" sends GET "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"

    Examples:
      | query        | name     |
      | sort=name    | sort     |
      | name=Dc      | name     |
      | pageSize=101 | pageSize |
      | page=0       | page     |

  @D41 @D29 @D70
  Scenario: The content of an external vocabulary is downloaded by name
    Given external vocabulary "Dc" is registered
    When "ga-viewer" downloads external vocabulary "Dc"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the download file name is "${E:Dc}.ttl"
    And the response body is the content sent for external vocabulary "Dc"

  @D41 @D30 @D66
  Scenario: The content of an external vocabulary is found by namespace or alias URI
    Given external vocabulary "Dc" is registered with the fields
      | field     | value                               |
      | aliasUris | http://alias.example.org/${rid}/dc  |
    When "ga-viewer" downloads the external content for namespace "${NS:Dc}/"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the download file name is "${E:Dc}.ttl"
    And the response body is the content sent for external vocabulary "Dc"
    When "ga-viewer" downloads the external content for namespace "http://alias.example.org/${rid}/dc#"
    Then the response status is 200
    And the response body is the content sent for external vocabulary "Dc"

  @D66 @negative
  Scenario: The content lookup of an unknown namespace gives 404 with the URI as sent
    When "ga-admin" downloads the external content for namespace "http://nothing.example.org/${rid}/ns#"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri 'http://nothing.example.org/${rid}/ns#' was not found."
    And the problem has exactly one issue, in "body" named "namespaceUri"

  @D73 @negative
  Scenario: The content lookup rejects a namespace that is not an absolute URI
    When "ga-admin" downloads the external content for namespace "relative/ns"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "namespaceUri"

  @D41 @D66 @negative
  Scenario: Reads of an unknown external vocabulary give 404
    When "ga-admin" gets external vocabulary "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${E:Ghost}' was not found."
    And the problem has exactly one issue, in "path" named "name"
    When "ga-admin" downloads external vocabulary "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${E:Ghost}' was not found."
    When "ga-admin" deletes external vocabulary "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '${E:Ghost}' was not found."

  @D49 @negative
  Scenario: An external vocabulary name in a path that breaks the name rules gives 400
    When "ga-admin" sends GET "/external-vocabularies/bad-${rid}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | type                                                     | in   | name |
      | urn:problem-type:belgif:input-validation:schemaViolation | path | name |

  @D41 @D42
  Scenario: Deleting an external vocabulary removes it and leaves internal vocabularies alone
    Given external vocabulary "Dc" is registered
    And vocabulary "Alpha" exists with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${NS:Dc}#> .
      @prefix ex: <http://example.org/${rid}/alpha#> .
      ex:Concept a skos:Concept ; skos:related dc:title .
      """
    When "ga-admin" deletes external vocabulary "Dc"
    Then the response status is 204
    When "ga-admin" gets external vocabulary "Dc"
    Then the response status is 404
    When "ga-admin" downloads the external content for namespace "${NS:Dc}"
    Then the response status is 404
    And version 1 of "Alpha" has status "ACTIVE"
    And the content of version 1 of "Alpha" is unchanged

  @D42 @D12
  Scenario: Registering an external namespace clears the warning and removing it brings it back
    When "ga-admin" creates vocabulary "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${NS:Dc}#> .
      @prefix ex: <http://example.org/${rid}/alpha#> .
      ex:Concept a skos:Concept ; skos:related dc:title .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                     | location                      |
      | externalReferenceSkipped | prefix 'dc' -> '${NS:Dc}#' |
    Given external vocabulary "Dc" is registered
    When "ga-admin" creates a new version of "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${NS:Dc}#> .
      @prefix ex: <http://example.org/${rid}/alpha#> .
      ex:Concept a skos:Concept ; skos:related dc:title , dc:creator .
      """
    Then the response status is 201
    And the response has no validation warnings
    Given external vocabulary "Dc" was deleted
    When "ga-admin" creates a new version of "Alpha" with this content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix dc: <${NS:Dc}#> .
      @prefix ex: <http://example.org/${rid}/alpha#> .
      ex:Concept a skos:Concept ; skos:related dc:title , dc:creator , dc:date .
      """
    Then the response status is 201
    And the validation warnings are exactly
      | code                     | location                      |
      | externalReferenceSkipped | prefix 'dc' -> '${NS:Dc}#' |

  @D41
  Scenario: An external vocabulary can be registered again after deletion
    Given external vocabulary "Dc" is registered
    And external vocabulary "Dc" was deleted
    When "ga-admin" registers external vocabulary "Dc"
    Then the response status is 201

  @D30 @D56 @negative
  Scenario: A duplicate external name is rejected with 409, also in another case
    Given external vocabulary "Dc" is registered
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field        | value                                |
      | namespaceUri | http://other.example.org/${rid}/dc   |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    When "ga-admin" registers external vocabulary "${EU:Dc}" with the fields
      | field        | value                                |
      | namespaceUri | http://other.example.org/${rid}/dc2  |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And external vocabulary "${EU:Dc}" is not stored

  @D30 @D55 @D66 @negative
  Scenario: A namespace or alias that is already registered is rejected with 409
    Given external vocabulary "Dc" is registered with the fields
      | field     | value                               |
      | aliasUris | http://alias.example.org/${rid}/dc  |
    When "ga-admin" registers external vocabulary "Other" with the fields
      | field        | value         |
      | namespaceUri | ${NS:Dc}/     |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias '${NS:Dc}/' is already registered."
    When "ga-admin" registers external vocabulary "Other" with the fields
      | field     | value                                |
      | aliasUris | http://alias.example.org/${rid}/dc#  |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias 'http://alias.example.org/${rid}/dc#' is already registered."
    When "ga-admin" registers external vocabulary "Other" with the fields
      | field        | value                               |
      | namespaceUri | http://alias.example.org/${rid}/dc  |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And external vocabulary "Other" is not stored

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given vocabulary "Shared" exists
    When "ga-admin" registers external vocabulary "${V:Shared}"
    Then the response status is 201
    And the response field "name" equals "${V:Shared}"

  @D30 @D41 @D9 @negative
  Scenario: Registration with invalid Turtle is rejected with one syntax issue
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field          | value                    |
      | vocabularyFile | fixture:syntax_error.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | in   | name           | detail                              | location             |
      | body | vocabularyFile | ^Encountered invalid Turtle syntax | ~^line 3(, column [0-9]+)?$ |
    And external vocabulary "Dc" is not stored

  @D30 @D41
  Scenario: Registration runs no semantic validation and returns no warnings
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field          | value                   |
      | vocabularyFile | fixture:semantic_s3.ttl |
    Then the response status is 201
    And the response field "validationWarnings" is absent

  @D70 @negative
  Scenario Outline: Registration of a file with no statements is rejected (<case>)
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field          | value   |
      | vocabularyFile | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | in   | name           | detail                            |
      | body | vocabularyFile | Vocabulary contains no statements |
    And external vocabulary "Dc" is not stored

    Examples:
      | case          | value                     |
      | comments only | fixture:comments_only.ttl |
      | 0 bytes       | <empty>                   |

  @D56 @Q2 @NFR-04
  Scenario: Registration accepts 20 alias URIs and returns them in the order sent
    When "ga-admin" registers external vocabulary "Dc" with 20 alias URIs
    Then the response status is 201
    And the response field "aliasUris[0]" equals "http://alias.example.org/${rid}/Dc/0"
    And the response field "aliasUris[19]" equals "http://alias.example.org/${rid}/Dc/19"

  @D56 @Q2 @NFR-04 @negative
  Scenario: Registration rejects 21 alias URIs
    When "ga-admin" registers external vocabulary "Dc" with 21 alias URIs
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And external vocabulary "Dc" is not stored

  @D56 @D70 @D73 @negative
  Scenario Outline: Registration rejects invalid aliases (<case>)
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field     | value     |
      | aliasUris | <first>   |
      | aliasUris | <second>  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And external vocabulary "Dc" is not stored

    Examples:
      | case                         | first                               | second                               |
      | alias equals the namespace   | ${NS:Dc}#                           | http://alias.example.org/${rid}/x    |
      | repeated alias               | http://alias.example.org/${rid}/x   | http://alias.example.org/${rid}/x/   |
      | comma list is one bad URI    | a,b                                 | http://alias.example.org/${rid}/y    |
      | relative alias               | relative/path                       | http://alias.example.org/${rid}/z    |

  @D73 @negative
  Scenario Outline: Registration rejects a <field> that is not an absolute URI
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field   | value         |
      | <field> | not a uri     |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And external vocabulary "Dc" is not stored

    Examples:
      | field        |
      | namespaceUri |
      | sourceUri    |

  @D56 @D37 @negative
  Scenario Outline: Registration rejects invalid fields (<case>)
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"

    Examples:
      | case                   | field          | value        |
      | description too long   | description    | ${R:d:1001}  |
      | name too short         | name           | Eq           |
      | name lower case        | name           | e${rid}lower |
      | no name                | name           | <omit>       |
      | no namespace URI       | namespaceUri   | <omit>       |
      | no file                | vocabularyFile | <omit>       |
      | unknown field          | colour         | blue         |

  @D56 @Q2 @NFR-04
  Scenario: Registration accepts a description of 1000 characters
    When "ga-admin" registers external vocabulary "Dc" with the fields
      | field       | value       |
      | description | ${R:d:1000} |
    Then the response status is 201

  @D15 @D41 @Q2 @NFR-04 @negative
  Scenario: Registration rejects a file over 5 MB with 413
    When "ga-admin" registers external vocabulary "Big" with a valid file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And external vocabulary "Big" is not stored

  @D15 @D41 @negative @API-38
  Scenario: Registration that is not multipart/form-data is rejected with 415
    When "ga-admin" sends POST "/external-vocabularies" with JSON
      """
      {"name": "E${rid}Json", "namespaceUri": "http://example.org/${rid}/json"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D49 @negative
  Scenario: A DELETE on an external name that breaks the name rules gives 400
    When "ga-admin" sends DELETE "/external-vocabularies/bad-${rid}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "name"

  @D52 @negative
  Scenario: A query parameter on DELETE is rejected and nothing is deleted
    Given external vocabulary "Dc" is registered
    When "ga-admin" sends DELETE "/external-vocabularies/${E:Dc}?force=true"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And the external vocabulary list contains "Dc"

  @D52 @negative
  Scenario: A query parameter on the external POST operations is rejected
    When "ga-admin" sends a multipart POST to "/external-vocabularies?mode=fast" with the fields
      | field          | value                    |
      | name           | ${E:Q}                   |
      | namespaceUri   | ${NS:Q}                  |
      | vocabularyFile | fixture:external.ttl     |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "mode"
    And external vocabulary "Q" is not stored
    When "ga-admin" sends POST "/external-vocabularies/content?mode=fast" with JSON
      """
      {"namespaceUri": "http://example.org/${rid}/ns"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "mode"
    When "ga-admin" sends POST "/external-vocabulary/searches?mode=fast" with JSON
      """
      {"namespaceUri": "http://example.org/${rid}/ns"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "mode"

  @D41 @D22
  Scenario: The external vocabulary list pages with 20 items by default
    When "ga-viewer" sends GET "/external-vocabularies"
    Then the response status is 200
    And the response field "page" equals 1
    And the response field "pageSize" equals 20

  @D41 @D27 @D58
  Scenario: The external vocabulary is returned with all its fields
    Given external vocabulary "Dc" is registered
    When "ga-viewer" gets external vocabulary "Dc"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "href" equals "/external-vocabularies/${E:Dc}"
    And the response field "name" equals "${E:Dc}"
    And the response field "namespaceUri" equals "${NS:Dc}"
    And the response field "sourceUri" equals "https://source.example.org/${rid}/Dc.ttl"
    And the response field "description" equals "Cached copy of Dc for validation."
    And the response field "createdBy" equals "${EMAIL:ga-admin}"
    And the response field "creationDate" is a UTC timestamp to the second
