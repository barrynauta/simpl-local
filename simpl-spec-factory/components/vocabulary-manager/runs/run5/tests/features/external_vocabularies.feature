Feature: Register, read and remove external vocabularies
  An external vocabulary is a registered cached copy used to resolve references (D30).
  The test basis is the OpenAPI plus D30, D41, D42 and D56; there is no story yet (D41).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"

  @D41 @D30 @D27 @D58 @API-12
  Scenario: Registering an external vocabulary returns it with its creator
    When I register an external vocabulary with the form
      | field          | value                                              |
      | name           | {Ext}                                              |
      | namespaceUri   | https://ext-{run}.example.org/terms/               |
      | aliasUris      | https://alias1-{run}.example.org/terms/            |
      | aliasUris      | https://alias2-{run}.example.org/terms#            |
      | sourceUri      | https://source-{run}.example.org/terms.ttl         |
      | description    | Cached copy for run {run}.                         |
      | vocabularyFile | text:@prefix t: <https://ext-{run}.example.org/terms/> . t:a a t:B . |
    Then the response status is 201
    And the response header "Location" is "/external-vocabularies/{Ext}"
    And the response field "href" is "/external-vocabularies/{Ext}"
    And the response field "name" is "{Ext}"
    And the response field "namespaceUri" is "https://ext-{run}.example.org/terms/"
    And the response field "aliasUris" equals ["https://alias1-{run}.example.org/terms/", "https://alias2-{run}.example.org/terms#"]
    And the response field "sourceUri" is "https://source-{run}.example.org/terms.ttl"
    And the response field "description" is "Cached copy for run {run}."
    And the response field "createdBy" identifies the user with role "GA_VOCABULARY_ADMIN"
    And the response field "creationDate" is a UTC time to the second

  @D41 @D30
  Scenario: A registered external vocabulary can be read and its content downloaded
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms#"
    And I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"
    When I send "GET" to "/external-vocabularies/{Ext}"
    Then the response status is 200
    And the response field "name" is "{Ext}"
    And the response field "namespaceUri" is "https://ext-{run}.example.org/terms#"
    When I send "GET" to "/external-vocabularies/{Ext}/content"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response header "Content-Disposition" contains "attachment"
    And the response header "Content-Disposition" contains "{Ext}.ttl"
    And the response body is the content of the external vocabulary "{Ext}"

  @D41 @D22
  Scenario: The external vocabulary list is paged and ordered by name
    Given an external vocabulary "{ExtCharlie}" is registered with namespace "https://c-{run}.example.org/ns"
    And an external vocabulary "{ExtAlpha}" is registered with namespace "https://a-{run}.example.org/ns"
    When I send "GET" to "/external-vocabularies?pageSize=1"
    Then the response status is 200
    And the response has the paging fields
    And the response field "pageSize" equals 1
    And the response has 1 item
    And the external vocabulary list shows "{ExtAlpha}" before "{ExtCharlie}"

  @D41 @D21 @negative
  Scenario Outline: The external vocabulary list takes no filter or sort (<query>)
    When I send "GET" to "/external-vocabularies?<query>"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query       | parameter |
      | sort=name   | sort      |
      | name=Abc    | name      |
      | pageSize=0  | pageSize  |
      | page=0      | page      |
      | pageSize=101 | pageSize |

  @D30 @D41 @D8 @negative
  Scenario: Invalid Turtle is rejected with one syntax issue and nothing is registered
    When I register an external vocabulary with the form
      | field          | value                                |
      | name           | {Ext}                                |
      | namespaceUri   | https://ext-{run}.example.org/terms# |
      | vocabularyFile | fixture:syntax-error.ttl             |
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And every issue is in "body" named "vocabularyFile"
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax"
    And the first issue has a location matching "^line 6, column \d+$"
    And no external vocabulary named "{Ext}" is registered

  @D30 @D41
  Scenario: Registration runs no semantic validation and gives no warnings
    When I register an external vocabulary with the form
      | field          | value                                |
      | name           | {Ext}                                |
      | namespaceUri   | https://ext-{run}.example.org/terms# |
      | vocabularyFile | fixture:multiple-semantic-errors.ttl |
    Then the response status is 201
    And the response field "validationWarnings" is absent or empty

  @D30 @D56 @D48 @negative @API-46
  Scenario: A second external vocabulary with the same name is a conflict
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/one"
    When I register the external vocabulary "{Ext}" with namespace "https://ext-{run}.example.org/two"
    Then the response is a problem with status 409
    And the problem type follows the simpl URN form

  @D56 @negative
  Scenario: External names are unique without regard to case
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/one"
    When I register the external vocabulary "{upper:Ext}" with namespace "https://ext-{run}.example.org/two"
    Then the response is a problem with status 409
    And no external vocabulary named "{upper:Ext}" is registered

  @D30 @D55 @negative
  Scenario: A second external vocabulary with the same namespace is a conflict
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/shared"
    When I register the external vocabulary "{Other}" with namespace "https://ext-{run}.example.org/shared"
    Then the response is a problem with status 409
    And the problem detail is "An external vocabulary with namespace or alias 'https://ext-{run}.example.org/shared' is already registered."
    And no external vocabulary named "{Other}" is registered

  @D30 @negative
  Scenario Outline: Namespace and alias URIs are unique after normalisation (<case>)
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/shared#" and alias "https://alias-{run}.example.org/shared/"
    When <step>
    Then the response is a problem with status 409
    And no external vocabulary named "{Other}" is registered

    Examples:
      | case                         | step                                                                                                                         |
      | namespace without '#'        | I register the external vocabulary "{Other}" with namespace "https://ext-{run}.example.org/shared"                          |
      | namespace equals an alias    | I register the external vocabulary "{Other}" with namespace "https://alias-{run}.example.org/shared"                        |
      | alias equals the namespace   | I register the external vocabulary "{Other}" with namespace "https://other-{run}.example.org/ns" and alias "https://ext-{run}.example.org/shared/" |

  @D56
  Scenario: An external vocabulary may share its name with an internal vocabulary
    Given a vocabulary "{Shared}" exists
    When I register the external vocabulary "{Shared}" with namespace "https://ext-{run}.example.org/shared"
    Then the response status is 201
    And the vocabulary "{Shared}" has status "ACTIVE"

  @D56 @D37 @negative
  Scenario Outline: Invalid registration fields are rejected (<case>)
    When I register an external vocabulary with the form
      | field          | value         |
      | name           | <name>        |
      | namespaceUri   | <namespace>   |
      | description    | <description> |
      | vocabularyFile | default       |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary named "<name>" is registered

    Examples:
      | case                      | name          | namespace                          | description  | field        |
      | name in lower case        | ext{run}      | https://ext-{run}.example.org/ns   | A copy.      | name         |
      | name with a hyphen        | Ext-{run}     | https://ext-{run}.example.org/ns   | A copy.      | name         |
      | description of 1001 chars | {Ext}         | https://ext-{run}.example.org/ns   | {chars:1001} | description  |
      | namespace not a URI       | {Ext}         | not a uri                          | A copy.      | namespaceUri |

  @D41 @negative
  Scenario Outline: A registration without a required field is rejected (<field> missing)
    When I register an external vocabulary with the form
      | field   | value  |
      | <keep1> | <val1> |
      | <keep2> | <val2> |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And no external vocabulary named "{Ext}" is registered

    Examples:
      | field          | keep1        | val1                             | keep2          | val2                             |
      | name           | namespaceUri | https://ext-{run}.example.org/ns | vocabularyFile | default                          |
      | namespaceUri   | name         | {Ext}                            | vocabularyFile | default                          |
      | vocabularyFile | name         | {Ext}                            | namespaceUri   | https://ext-{run}.example.org/ns |

  @D56
  Scenario: Description and aliases are optional
    When I register an external vocabulary with the form
      | field          | value                            |
      | name           | {Ext}                            |
      | namespaceUri   | https://ext-{run}.example.org/ns |
      | vocabularyFile | default                          |
    Then the response status is 201

  @D56
  Scenario: Twenty aliases are allowed
    When I register the external vocabulary "{Ext}" with namespace "https://ext-{run}.example.org/ns" and 20 aliases
    Then the response status is 201
    And the response field "aliasUris[19]" is present

  @D56 @negative
  Scenario: More than twenty aliases are rejected
    When I register the external vocabulary "{Ext}" with namespace "https://ext-{run}.example.org/ns" and 21 aliases
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary named "{Ext}" is registered

  @D56 @negative
  Scenario Outline: An alias that repeats the namespace or another alias is rejected (<case>)
    When I register an external vocabulary with the form
      | field          | value                               |
      | name           | {Ext}                               |
      | namespaceUri   | https://ext-{run}.example.org/ns#   |
      | aliasUris      | <alias1>                            |
      | aliasUris      | <alias2>                            |
      | vocabularyFile | default                             |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "aliasUris"
    And no external vocabulary named "{Ext}" is registered

    Examples:
      | case                     | alias1                              | alias2                              |
      | alias equals namespace   | https://ext-{run}.example.org/ns/   | https://alias-{run}.example.org/a   |
      | alias repeated           | https://alias-{run}.example.org/a#  | https://alias-{run}.example.org/a/  |

  @D52 @negative @OWA-API3
  Scenario: An unknown registration field is rejected
    When I register an external vocabulary with the form
      | field          | value                            |
      | name           | {Ext}                            |
      | namespaceUri   | https://ext-{run}.example.org/ns |
      | createdBy      | someone@example.com              |
      | vocabularyFile | default                          |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "createdBy"
    And no external vocabulary named "{Ext}" is registered

  @D15 @D30 @negative
  Scenario: An external vocabulary file above 5 MB gets 413
    When I register an external vocabulary with the form
      | field          | value                            |
      | name           | {Ext}                            |
      | namespaceUri   | https://ext-{run}.example.org/ns |
      | vocabularyFile | size:5242881                     |
    Then the response is a problem with status 413
    And no external vocabulary named "{Ext}" is registered

  @D15 @D30 @negative @API-38
  Scenario: A registration that is not multipart/form-data gets 415
    When I send "POST" to "/external-vocabularies" with JSON
      """
      {"name": "{Ext}", "namespaceUri": "https://ext-{run}.example.org/ns"}
      """
    Then the response is a problem with status 415
    And no external vocabulary named "{Ext}" is registered

  @D41 @D42
  Scenario: Removing an external vocabulary deletes it and leaves internal vocabularies alone
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms#"
    And a vocabulary "{Alpha}" exists with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ext: <https://ext-{run}.example.org/terms#> .
      @prefix ex: <https://vocab.example.org/{run}/uses#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        ext:creator "Somebody" .
      """
    When I send "DELETE" to "/external-vocabularies/{Ext}"
    Then the response status is 204
    And the response body is empty
    When I send "GET" to "/external-vocabularies/{Ext}"
    Then the response is a problem with status 404
    When I send "GET" to "/external-vocabularies/{Ext}/content"
    Then the response is a problem with status 404
    And no external vocabulary named "{Ext}" is registered
    And the vocabulary "{Alpha}" has status "ACTIVE"
    And the content of version 1 of "{Alpha}" is unchanged

  @D41 @D30
  Scenario: A removed external vocabulary can be registered again
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms#"
    When I send "DELETE" to "/external-vocabularies/{Ext}"
    Then the response status is 204
    When I register the external vocabulary "{Ext}" with namespace "https://ext-{run}.example.org/terms#"
    Then the response status is 201

  @D41 @D55 @D48 @negative
  Scenario Outline: An unknown external vocabulary gives 404 with its fixed text (<method> <path>)
    When I send "<method>" to "<path>"
    Then the response is a problem with status 404
    And the problem detail is "External vocabulary with name '{Ghost}' was not found."
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "name"

    Examples:
      | method | path                                  |
      | GET    | /external-vocabularies/{Ghost}         |
      | GET    | /external-vocabularies/{Ghost}/content |
      | DELETE | /external-vocabularies/{Ghost}         |

  @D54 @negative
  Scenario: External vocabulary names in paths are matched exactly
    Given an external vocabulary "{Ext}" is registered with namespace "https://ext-{run}.example.org/terms#"
    When I send "GET" to "/external-vocabularies/{upper:Ext}"
    Then the response is a problem with status 404
