Feature: Create a vocabulary through the API
  POST /vocabularies stores a validated vocabulary as version 1, ACTIVE, in the same request
  (D6). Field rules come from D37, the media type and size rules from D15.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @AC_SIMPL-18194_AC2 @D6 @D10 @D25 @D26 @D27 @D29 @D31 @API-36 @E2 @D39
  Scenario: A valid vocabulary is created as version 1, ACTIVE
    When I create the vocabulary "Health" with:
      | field       | value                                          |
      | description | Vocabulary defining healthcare resource types. |
      | changelog   | Initial version.                               |
    Then the response status is 201
    And the response content type is "application/json"
    And the Location header is the href of version 1 of "Health"
    And the response is version 1 of "Health" with status "ACTIVE"
    And the field "description" is "Vocabulary defining healthcare resource types."
    And the field "changelog" is "Initial version."
    And the field "createdBy" is the email of "ga-admin"
    And the field "contentHref" is "${content:Health@1}"
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @AC_SIMPL-18187_AC1 @D3 @D5 @D25 @D27 @D28 @E2
  Scenario: The created vocabulary can be read back with its metadata
    Given a vocabulary "Health" exists with:
      | field       | value                 |
      | description | Healthcare resources. |
    When I send GET "/vocabularies/${name:Health}"
    Then the response status is 200
    And the response is vocabulary "Health" with latest version 1 and status "ACTIVE"
    And the field "description" is "Healthcare resources."
    And the field "createdBy" is the email of "ga-admin"
    And the field "contentHref" is "${content:Health}"

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status is ACTIVE by default and can be changed by hand both ways
    Given a vocabulary "Health" exists
    When I change the status of vocabulary "Health" to "DEPRECATED"
    Then the response status is 200
    And the vocabulary "Health" has status "DEPRECATED"
    When I change the status of vocabulary "Health" to "ACTIVE"
    Then the response status is 200
    And the vocabulary "Health" has status "ACTIVE"

  @AC_SIMPL-18187_AC1 @D29
  Scenario: The stored content is downloadable byte for byte
    Given a vocabulary "Health" exists
    When I send GET "/content/${name:Health}/versions/1"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body is the content of version 1 of "Health"

  @D1
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list
    When I create the vocabulary "Cpsv" with the file "cpsv-like.ttl"
    Then the response status is 201
    When I send GET "/vocabularies?name=${run}"
    Then the response status is 200
    And the list holds the vocabularies "Cpsv" in this order

  @AC_SIMPL-18170_AC3 @D37 @D8 @D24 @negative @API-44
  Scenario Outline: An invalid or incomplete create request is rejected: <case>
    When I create the vocabulary "Bad" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400
    And the vocabulary "Bad" does not exist

    Examples:
      | case                          | field       | value       |
      | name missing                  | name        | <absent>    |
      | name starts lower case        | name        | v${runid}x  |
      | name with a hyphen            | name        | ${run}-Bad  |
      | name too short                | name        | <name:2>    |
      | name too long                 | name        | <name:65>   |
      | description missing           | description | <absent>    |
      | description empty             | description | <empty>     |
      | description only spaces       | description | <spaces>    |
      | description of 1001 chars     | description | <text:1001> |
      | changelog missing             | changelog   | <absent>    |
      | changelog empty               | changelog   | <empty>     |
      | changelog only spaces         | changelog   | <spaces>    |
      | changelog of 1001 chars       | changelog   | <text:1001> |
      | file missing                  | file        | <absent>    |

  @D37 @D39
  Scenario Outline: Field lengths at the upper limit are accepted (<field>)
    When I create the vocabulary "Edge" with:
      | field   | value   |
      | <field> | <value> |
    Then the response status is 201

    Examples:
      | field       | value       |
      | name        | <name:64>   |
      | description | <text:1000> |
      | changelog   | <text:1000> |

  @AC_SIMPL-18170_AC3 @D15 @D30 @D37 @negative @API-46 @API-47
  Scenario: A duplicate vocabulary name is rejected with 409
    Given a vocabulary "Health" exists
    When I create the vocabulary "Health"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '${name:Health}' already exists."
    And the vocabulary "Health" has 1 version

  @AC_SIMPL-18170_AC3 @D37 @negative @API-46
  Scenario: Vocabulary names are unique regardless of case
    Given a vocabulary "Health" exists
    When I create the vocabulary "Upper" with:
      | field | value             |
      | name  | ${uname:Health}   |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the vocabulary "Health" has 1 version

  @AC_SIMPL-18170_AC3 @D9 @D15 @D8 @negative
  Scenario: A file that is not Turtle is rejected as a syntax error
    When I create the vocabulary "Json" with the file "not-turtle.json"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And every issue is a vocabulary file issue
    And the vocabulary "Json" does not exist

  @D15 @D8 @negative @API-46 @API-47 @API-12
  Scenario: A file larger than 5 MB is rejected with 413
    When I create the vocabulary "Huge" with:
      | field | value         |
      | file  | <bytes:5242881> |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the problem detail is "Uploaded vocabulary file is too large. The maximum allowed upload size is 5 MB. Please upload a smaller Turtle file."
    And the vocabulary "Huge" does not exist

  @D15
  Scenario: A file of exactly 5 MB is accepted
    When I create the vocabulary "Big" with:
      | field | value           |
      | file  | <bytes:5242880> |
    Then the response status is 201

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38 @API-46
  Scenario Outline: A create request that is not multipart/form-data is rejected with 415 (<ctype>)
    When I send POST "/vocabularies" with the content type "<ctype>" and the body:
      """
      {"name": "${run}Json", "description": "d", "changelog": "c"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Json" does not exist

    Examples:
      | ctype                             |
      | application/json                  |
      | text/turtle                       |
      | application/x-www-form-urlencoded |

  @D43
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When I create the vocabulary "Race" 2 times in parallel
    Then the responses have the statuses 201, 409
    And the vocabulary "Race" has 1 version

