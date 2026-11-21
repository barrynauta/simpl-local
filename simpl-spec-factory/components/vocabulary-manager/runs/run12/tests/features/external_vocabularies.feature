Feature: Register, read and remove external vocabularies
  Test basis: OpenAPI plus D30, D41, D42, D56, D66, D70, D73, D110, D147, D148.

  Background:
    Given I am authenticated as "ga-admin"

  @D41 @D30 @D27 @D73 @D110
  Scenario: Registering an external vocabulary returns 201 with Location and the stored values as sent
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                              |
      | name           | {{Ext}}                                            |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/             |
      | aliasUris      | https://alias2.example.org/{{run}}/terms           |
      | aliasUris      | https://alias1.example.org/{{run}}/terms#          |
      | sourceUri      | https://source.example.org/{{run}}/terms.ttl       |
      | description    | Cached copy for validation.                        |
      | vocabularyFile | file:external-dcterms.ttl                          |
    Then the status is 201
    And the response header "Location" equals "/external-vocabularies/{{Ext}}"
    And the response field "href" equals "/external-vocabularies/{{Ext}}"
    And the response field "name" equals "{{Ext}}"
    And the response field "namespaceUri" equals "https://example.org/ext/{{run}}/terms/"
    And the response field "aliasUris" equals ["https://alias2.example.org/{{run}}/terms", "https://alias1.example.org/{{run}}/terms#"]
    And the response field "sourceUri" equals "https://source.example.org/{{run}}/terms.ttl"
    And the response field "description" equals "Cached copy for validation."
    And the response field "createdBy" equals "{{adminEmail}}"
    And the response field "creationDate" is a UTC timestamp
    When I send "GET" to "/external-vocabularies/{{Ext}}"
    Then the status is 200
    And the response field "namespaceUri" equals "https://example.org/ext/{{run}}/terms/"
    And the response field "aliasUris" equals ["https://alias2.example.org/{{run}}/terms", "https://alias1.example.org/{{run}}/terms#"]

  @D110 @D148 @D56
  Scenario Outline: An optional description that is <case> is not stored, and aliasUris is always present
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | <field>        | <value>                                |
      | vocabularyFile | file:external-dcterms.ttl              |
    Then the status is 201
    And the response field "description" is absent
    And the response field "aliasUris" equals []

    Examples:
      | case   | field       | value                                |
      | absent | sourceUri   | https://source.example.org/{{run}}/x |
      | blank  | description | blank                                |

  @D30 @D41 @negative
  Scenario: Registration checks Turtle syntax and reports one issue
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | vocabularyFile | file:syntax-error.ttl                  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And issue 0 has detail starting with "Encountered invalid Turtle syntax"
    And no external vocabulary named "{{Ext}}" is registered

  @D30 @D41
  Scenario: Registration runs no semantic validation and returns no warnings
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/odd#   |
      | vocabularyFile | file:external-semantic-issue.ttl       |
    Then the status is 201
    And the response field "validationWarnings" is absent or an empty list

  @D70 @negative
  Scenario Outline: Registration rejects a file with no statements (<case>)
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | vocabularyFile | <file>                                 |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And the response field "issues.0.detail" equals "Vocabulary contains no statements"
    And no external vocabulary named "{{Ext}}" is registered

    Examples:
      | case          | file                   |
      | empty file    | empty-file             |
      | comments only | file:comments-only.ttl |

  @D41 @D56 @D73 @negative
  Scenario Outline: A registration that breaks a field rule is rejected (<case>)
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                     |
      | name           | <name>                    |
      | namespaceUri   | <namespace>               |
      | <field>        | <value>                   |
      | vocabularyFile | file:external-dcterms.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<issue>"
    And no external vocabulary is registered for namespace "https://example.org/ext/{{run}}/terms/"

    Examples:
      | case                    | name          | namespace                              | field       | value                                    | issue        |
      | lower-case name         | ext{{run}}    | https://example.org/ext/{{run}}/terms/ | description | x                                        | name         |
      | name of 65 characters   | {{Long65}}    | https://example.org/ext/{{run}}/terms/ | description | x                                        | name         |
      | relative namespace      | {{Ext}}       | ext/{{run}}/terms/                     | description | x                                        | namespaceUri |
      | relative source         | {{Ext}}       | https://example.org/ext/{{run}}/terms/ | sourceUri   | terms.ttl                                | sourceUri    |
      | relative alias          | {{Ext}}       | https://example.org/ext/{{run}}/terms/ | aliasUris   | alias/terms                              | aliasUris    |
      | alias equals namespace  | {{Ext}}       | https://example.org/ext/{{run}}/terms/ | aliasUris   | https://example.org/ext/{{run}}/terms#   | aliasUris    |
      | description too long    | {{Ext}}       | https://example.org/ext/{{run}}/terms/ | description | repeat:1001:d                            | description  |
      | unknown field           | {{Ext}}       | https://example.org/ext/{{run}}/terms/ | colour      | blue                                     | colour       |

  @D41 @negative
  Scenario Outline: A registration without the required field <field> is rejected
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field    | value    |
      | <k1>     | <v1>     |
      | <k2>     | <v2>     |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"

    Examples:
      | field          | k1           | v1                                     | k2             | v2                        |
      | name           | namespaceUri | https://example.org/ext/{{run}}/terms/ | vocabularyFile | file:external-dcterms.ttl |
      | namespaceUri   | name         | {{Ext}}                                | vocabularyFile | file:external-dcterms.ttl |
      | vocabularyFile | name         | {{Ext}}                                | namespaceUri   | https://example.org/ext/{{run}}/terms/ |

  @D56 @negative
  Scenario: A repeated alias is rejected
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | aliasUris      | https://alias.example.org/{{run}}/x/   |
      | aliasUris      | https://alias.example.org/{{run}}/x    |
      | vocabularyFile | file:external-dcterms.ttl              |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary named "{{Ext}}" is registered

  @D56 @NFR-04 @Q2
  Scenario: 20 aliases are accepted and 21 are rejected
    When I register external vocabulary "{{Ext}}" with 20 aliases
    Then the status is 201
    And the response field "aliasUris" has 20 items
    When I register external vocabulary "{{Ext2}}" with 21 aliases
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary named "{{Ext2}}" is registered

  @D15 @NFR-04 @Q2 @negative
  Scenario: An external file over 5 MB gives 413
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | vocabularyFile | generated:5242881                      |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no external vocabulary named "{{Ext}}" is registered

  @D15 @negative @API-38
  Scenario: A registration that is not multipart/form-data gets 415
    When I send "POST" to "/external-vocabularies" with body "{}" and content type "application/json"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D30 @D147 @negative @API-46
  Scenario: A registered name is a conflict, also in another case
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/a/"
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                              |
      | name           | {{Ext}}                            |
      | namespaceUri   | https://example.org/ext/{{run}}/b/ |
      | vocabularyFile | file:external-dcterms.ttl          |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with name '{{Ext}}' is already registered."
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                              |
      | name           | {{upper:Ext}}                      |
      | namespaceUri   | https://example.org/ext/{{run}}/c/ |
      | vocabularyFile | file:external-dcterms.ttl          |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And no external vocabulary is registered for namespace "https://example.org/ext/{{run}}/c/"

  @D30 @D55 @D66 @D147 @negative
  Scenario: A namespace already registered, compared without trailing slash or hash, is a conflict
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/a/"
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                              |
      | name           | {{Other}}                          |
      | namespaceUri   | https://example.org/ext/{{run}}/a# |
      | vocabularyFile | file:external-dcterms.ttl          |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias 'https://example.org/ext/{{run}}/a#' is already registered."
    And no external vocabulary named "{{Other}}" is registered

  @D30 @D55 @D147 @negative
  Scenario: An alias that equals a registered namespace or alias is a conflict
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/a/" and aliases "https://alias.example.org/{{run}}/a/"
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                 |
      | name           | {{Other}}                             |
      | namespaceUri   | https://example.org/ext/{{run}}/new/  |
      | aliasUris      | https://alias.example.org/{{run}}/a   |
      | vocabularyFile | file:external-dcterms.ttl             |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with namespace or alias 'https://alias.example.org/{{run}}/a' is already registered."
    And no external vocabulary named "{{Other}}" is registered

  @D147 @negative
  Scenario: The name conflict is reported before the namespace conflict
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/a/"
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                              |
      | name           | {{Ext}}                            |
      | namespaceUri   | https://example.org/ext/{{run}}/a/ |
      | vocabularyFile | file:external-dcterms.ttl          |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "An external vocabulary with name '{{Ext}}' is already registered."

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "{{Shared}}" exists
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Shared}}                             |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | vocabularyFile | file:external-dcterms.ttl              |
    Then the status is 201

  @D41 @D22 @BLG-col-paging
  Scenario: The external list is paged and sorted by name
    Given an external vocabulary "{{ExtB}}" is registered with namespace "https://example.org/ext/{{run}}/b/"
    And an external vocabulary "{{ExtA}}" is registered with namespace "https://example.org/ext/{{run}}/a/"
    When I send "GET" to "/external-vocabularies?pageSize=100"
    Then the status is 200
    And the response has the fields "page, pageSize, total, totalPages, self, first, items"
    And the response field "pageSize" equals 100
    And the external items of this scenario are in the order "{{ExtA}}, {{ExtB}}"

  @D41 @D21 @negative
  Scenario Outline: The external list has no filter or sort parameters (<query>)
    When I send "GET" to "/external-vocabularies?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "<param>"

    Examples:
      | query        | param    |
      | name=Abc     | name     |
      | sort=name    | sort     |
      | pageSize=101 | pageSize |

  @D41 @D29 @D70
  Scenario: The content of an external vocabulary is downloaded by name and by namespace
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/" and aliases "https://alias.example.org/{{run}}/terms/"
    When I send "GET" to "/external-vocabularies/{{Ext}}/content"
    Then the status is 200
    And the response header "Content-Type" equals "text/turtle"
    And the response header "Content-Disposition" contains "{{Ext}}.ttl"
    And the response body is the external fixture "external-dcterms.ttl"
    When I send "POST" to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "https://alias.example.org/{{run}}/terms#"}
      """
    Then the status is 200
    And the response header "Content-Type" equals "text/turtle"
    And the response body is the external fixture "external-dcterms.ttl"

  @D41 @D66 @negative
  Scenario: Content lookup by an unknown namespace gives 404 with the URI as sent
    When I send "POST" to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "https://example.org/nothing/{{run}}/"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with namespaceUri 'https://example.org/nothing/{{run}}/' was not found."
    And the problem has exactly one issue, in "body" named "namespaceUri"

  @D73 @negative
  Scenario: Content lookup by a relative URI gives 400
    When I send "POST" to "/external-vocabularies/content" with JSON body
      """
      {"namespaceUri": "terms/"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "namespaceUri"

  @D41 @D66 @negative
  Scenario Outline: An unknown external name gives 404 (<method> <suffix>)
    When I send "<method>" to "/external-vocabularies/{{Missing}}<suffix>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "External vocabulary with name '{{Missing}}' was not found."
    And the problem has exactly one issue, in "path" named "name"

    Examples:
      | method | suffix   |
      | GET    |          |
      | GET    | /content |
      | DELETE |          |

  @D49 @negative
  Scenario: An invalid external name in the path gives 400 for the path
    When I send "GET" to "/external-vocabularies/bad-name"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "name"

  @D41 @D42
  Scenario: Removing an external vocabulary deletes metadata and content and leaves internal vocabularies alone
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/"
    And a vocabulary "{{Health}}" exists with content
      """
      @prefix ex: <https://example.org/vm/{{run}}#> .
      @prefix dct: <https://example.org/ext/{{run}}/terms/> .
      ex:Thing dct:title "x" .
      """
    When I send "DELETE" to "/external-vocabularies/{{Ext}}"
    Then the status is 204
    When I send "GET" to "/external-vocabularies/{{Ext}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send "GET" to "/external-vocabularies/{{Ext}}/content"
    Then the status is 404
    And no external vocabulary is registered for namespace "https://example.org/ext/{{run}}/terms/"
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"
    When I send "GET" to "/content/{{Health}}"
    Then the response body is the content uploaded for version 1 of "{{Health}}"

  @D41
  Scenario: A removed external vocabulary can be registered again
    Given an external vocabulary "{{Ext}}" is registered with namespace "https://example.org/ext/{{run}}/terms/"
    And external vocabulary "{{Ext}}" is deleted
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | vocabularyFile | file:external-dcterms.ttl              |
    Then the status is 201

  @D56 @NFR-04 @Q2
  Scenario: An external description of 1000 characters is accepted
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | description    | padded-repeat:1000:d                   |
      | vocabularyFile | file:external-dcterms.ttl              |
    Then the status is 201

  @D15 @NFR-04 @Q2
  Scenario: An external file of exactly 5 MB is accepted
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/big/   |
      | vocabularyFile | generated:5242880                      |
    Then the status is 201

  @D70 @D73 @negative
  Scenario: An aliasUris part holding "a,b" is one URI and is not absolute
    When I send "POST" to "/external-vocabularies" with a multipart body
      | field          | value                                  |
      | name           | {{Ext}}                                |
      | namespaceUri   | https://example.org/ext/{{run}}/terms/ |
      | aliasUris      | a,b                                    |
      | vocabularyFile | file:external-dcterms.ttl              |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary named "{{Ext}}" is registered
