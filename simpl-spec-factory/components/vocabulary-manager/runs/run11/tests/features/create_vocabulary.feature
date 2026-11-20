Feature: Create a vocabulary through the API
  POST /vocabularies stores version 1 as ACTIVE in the same request when the content passes validation.

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @AC_SIMPL-18194_AC2 @D6 @D7 @D10 @D26 @D27 @D31 @D58 @D110 @E2 @D39 @D79 @API-36
  Scenario: A valid vocabulary is created as version 1, ACTIVE
    Given a create vocabulary request for "A"
    And the request field "description" is "Healthcare resource types ${SID}"
    And the request field "changelog" is "Initial version."
    When I submit the request
    Then the response status is 201
    And the response content type is JSON
    And the response header "Location" is "/vocabularies/${A}/versions/1"
    And the response field "href" equals the response header "Location"
    And the response field "name" is "${A}"
    And the response field "version" is 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Healthcare resource types ${SID}"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${EMAIL:admin}"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    And the response field "contentHref" is "${CB}/content/${A}/versions/1"
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/1"
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @AC_SIMPL-18187_AC1 @D3 @D25 @D27 @D28
  Scenario: The created vocabulary can be read back with its metadata
    Given a create vocabulary request for "A"
    And the request field "description" is "First description ${SID}"
    When I submit the request
    Then the response status is 201
    When I send a GET request to "/vocabularies/${A}"
    Then the response status is 200
    And the response field "name" is "${A}"
    And the response field "version" is 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "First description ${SID}"
    And the response field "createdBy" is "${EMAIL:admin}"
    And the response field "href" is "/vocabularies/${A}"
    And the response field "contentHref" is "${CB}/content/${A}"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    When I send a GET request to "/vocabularies/${A}/versions/1"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And the response field "changelog" is "Initial version."

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status can be changed to DEPRECATED and back to ACTIVE
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response field "status" is "DEPRECATED"
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"

  @D1
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list
    Given a create vocabulary request for "Cpsv"
    And the request file is the fixture "cpsv-like.ttl"
    When I submit the request
    Then the response status is 201
    When I send a GET request to "/vocabularies?name=${Cpsv}"
    Then the response status is 200
    And the item names are "${Cpsv}"

  @AC_SIMPL-18170_AC3 @D24 @D37 @D8 @negative
  Scenario Outline: A required field that is missing is rejected: <field>
    Given a create vocabulary request for "A"
    And the request field "<field>" is removed
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary "A" is stored

    Examples:
      | field          |
      | name           |
      | description    |
      | changelog      |
      | vocabularyFile |

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario Outline: A name that breaks the naming rules is rejected: <name>
    Given a create vocabulary request for "A"
    And the request field "name" is "<name>"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary with the requested name is stored

    Examples:
      | name                 |
      | v${SID}lower         |
      | 1V${SID}             |
      | V${SID}-dash         |
      | V${SID}_under        |
      | V${SID} space        |
      | Ab                   |

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario Outline: A blank text field is rejected: <field>
    Given a create vocabulary request for "A"
    And the request field "<field>" is "   "
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary "A" is stored

    Examples:
      | field       |
      | description |
      | changelog   |

  @AC_SIMPL-18170_AC3 @AC_SIMPL-18169_AC2 @D15 @D37 @D8 @negative @API-46
  Scenario: A duplicate name is rejected with 409
    Given a vocabulary "A" exists
    And a create vocabulary request for "A"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '${A}' already exists."
    And exactly one vocabulary "A" is stored
    And vocabulary "A" has 1 version

  @D37 @D54 @negative
  Scenario: Names are unique without regard to case
    Given a vocabulary "Alpha" exists
    And a create vocabulary request for "AlphaCase"
    And the request field "name" is "${upper:Alpha}"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And exactly one vocabulary "Alpha" is stored

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, not 400
    Given a vocabulary "A" exists
    And a create vocabulary request for "A"
    And the request file is the fixture "syntax-error.ttl"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @AC_SIMPL-18190_AC2 @D7 @D52 @D8 @negative @OWA-API3
  Scenario: A version form field on creation is rejected
    Given a create vocabulary request for "A"
    And the request has an extra part "version" with value "1"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "version"
    And no vocabulary "A" is stored

  @D52 @D8 @negative @OWA-API3
  Scenario Outline: An unknown form field is rejected: <field>
    Given a create vocabulary request for "A"
    And the request has an extra part "<field>" with value "<value>"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "body" named "<field>"
    And no vocabulary "A" is stored

    Examples:
      | field     | value                |
      | status    | DEPRECATED           |
      | createdBy | someone@example.com  |
      | colour    | blue                 |

  @D52 @negative
  Scenario: A query parameter on POST /vocabularies is rejected
    Given a create vocabulary request for "A"
    When I submit the request with the query "?dryRun=true"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary "A" is stored

  @AC_SIMPL-18169_AC2 @D15 @negative @API-38 @API-46
  Scenario Outline: A request that is not multipart/form-data gets 415: <content type>
    Given a create vocabulary request for "A"
    When I submit the request as "<content type>"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no vocabulary "A" is stored

    Examples:
      | content type                      |
      | application/json                  |
      | text/turtle                       |
      | application/x-www-form-urlencoded |

  @D15 @D70 @D8 @negative
  Scenario: A file that is not Turtle is rejected as a syntax error
    Given a create vocabulary request for "A"
    And the request file is not Turtle
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue with detail starting "Encountered invalid Turtle syntax" and location matching "line \d+(, column \d+)?"
    And no vocabulary "A" is stored

  @D121
  Scenario: A multipart part with an empty name is ignored
    Given a create vocabulary request for "A"
    And the request has an extra part "" with value "ignored"
    When I submit the request
    Then the response status is 201
    And the response field "name" is "${A}"

  @D110 @D8 @negative @NFR-04
  Scenario: A multipart request with more than 64 parts is rejected
    Given a create vocabulary request for "A"
    And the request has 61 extra parts
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And no vocabulary "A" is stored
