Feature: Create a vocabulary through the API
  POST /vocabularies stores version 1 as ACTIVE in the same request (D6) and returns the
  vocabulary version with validationWarnings (D10, D26). Field rules: D37. Media type and size: D15, D76.

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @D6 @D7 @D10 @D26 @D27 @D31 @D58 @API-12 @API-36
  Scenario: A valid vocabulary is created as version 1, ACTIVE, with its identifier
    When I create the vocabulary "Health" with the description "Healthcare resource types."
    Then the response status is 201
    And the response media type is "application/json"
    And the response header "Location" is "/vocabularies/${Health}/versions/1"
    And the response field "href" is "/vocabularies/${Health}/versions/1"
    And the response header "Location" equals the response field "href"
    And the response field "contentHref" matches "^https?://.+/content/${Health}/versions/1$"
    And the response field "name" is "${Health}"
    And the response field "version" is 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Healthcare resource types."
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${adminEmail}"
    And the response field "creationDate" is a time in UTC to the second
    And the response field "lastUpdateDate" is a time in UTC to the second
    And there are no validation warnings

  @AC_SIMPL-18170_AC2 @AC_SIMPL-18187_AC1 @D3 @D5 @D6 @D25 @D27 @D28
  Scenario: The created vocabulary can be read with its metadata, version 1 and status ACTIVE
    Given a vocabulary "Health" exists with the description "Healthcare resource types."
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response status is 200
    And the response field "name" is "${Health}"
    And the response field "version" is 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Healthcare resource types."
    And the response field "createdBy" is "${adminEmail}"
    And the response field "href" is "/vocabularies/${Health}"
    And the response field "contentHref" matches "^https?://.+/content/${Health}$"
    And the response field "creationDate" is a time in UTC to the second
    And the response field "lastUpdateDate" is a time in UTC to the second

  @AC_SIMPL-18170_AC2 @D6
  Scenario: Version 1 of the created vocabulary is ACTIVE
    Given a vocabulary "Health" exists
    When I send a "GET" request to "/vocabularies/${Health}/versions/1"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And the response field "version" is 1
    And the response field "createdBy" is "${adminEmail}"

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status can be changed to DEPRECATED and back to ACTIVE
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response field "status" is "DEPRECATED"
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario Outline: A create request with a missing <part> is rejected and nothing is stored
    When I send a multipart POST to "/vocabularies" with
      | part           | value                |
      | <p1>           | <v1>                 |
      | <p2>           | <v2>                 |
      | <p3>           | <v3>                 |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And no vocabulary of this scenario is stored

    Examples:
      | part           | p1          | v1          | p2          | v2              | p3             | v3      |
      | name           | description | Description | changelog   | Initial version. | vocabularyFile | @valid  |
      | description    | name        | Miss${run}  | changelog   | Initial version. | vocabularyFile | @valid  |
      | changelog      | name        | Miss${run}  | description | Description      | vocabularyFile | @valid  |
      | vocabularyFile | name        | Miss${run}  | description | Description      | changelog      | Initial version. |

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative @NFR-04
  Scenario Outline: A create request with an invalid name is rejected (<case>)
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | <name>           |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary of this scenario is stored

    Examples:
      | case                  | name            |
      | starts lower case     | ${run}Lower     |
      | has a hyphen          | Ab-${run}       |
      | has a space           | Ab ${run}       |
      | 65 characters         | ${pad:65}       |
      | empty                 |                 |

  @AC_SIMPL-18170_AC3 @D37 @negative @NFR-04
  Scenario: A name of 2 characters is rejected
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | Ab               |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"

  @D37 @Q2 @NFR-04
  Scenario: A name of 64 characters is accepted
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | ${pad:64}        |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
    Then the response status is 201
    And exactly 1 vocabularies of this scenario are stored

  @D37 @Q2 @NFR-04
  Scenario: A name of 3 characters is accepted
    When I create a vocabulary with a free name of 3 characters
    Then the response status is 201
    And the response field "version" is 1

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative @NFR-04
  Scenario Outline: A create request with an invalid description is rejected (<case>)
    When I create the vocabulary "Desc" with the description "<description>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And no vocabulary named "Desc" is stored

    Examples:
      | case             | description           |
      | empty            |                       |
      | only spaces      | ${repeat: :3}         |
      | 1001 characters  | ${repeat:d:1001}      |

  @D37 @Q2 @NFR-04
  Scenario: A description of 1000 characters is accepted
    When I create the vocabulary "Desc" with the description "${repeat:d:1000}"
    Then the response status is 201

  @D37
  Scenario: A description of 1000 characters with surrounding spaces is accepted, the limit counts after trimming
    When I create the vocabulary "Desc" with the description "  ${repeat:d:1000}  "
    Then the response status is 201

  @AC_SIMPL-18170_AC3 @D15 @D37 @D51 @negative
  Scenario: A duplicate name is rejected with 409 and the decided detail
    Given a vocabulary "Twin" exists
    When I create the vocabulary "Twin"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '${Twin}' already exists."
    And the problem type follows API-46 and API-47
    And the vocabulary "Twin" has 1 versions

  @D37 @negative
  Scenario: A name that differs only in case is a duplicate
    Given a vocabulary "Twin" exists
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | ${upper:Twin}    |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And exactly 1 vocabularies of this scenario are stored

  @D51 @negative
  Scenario: A duplicate name with invalid content gives 409, conflicts come before content validation
    Given a vocabulary "Twin" exists
    When I create the vocabulary "Twin" with the file "syntax-error.ttl"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @AC_SIMPL-18173_AC6 @D15 @D8 @negative @API-38 @API-46
  Scenario Outline: A create request that is not multipart/form-data is rejected with 415 (<ctype>)
    When I send a "POST" request to "/vocabularies" with content type "<ctype>" and body
      """
      {"name": "Json${run}", "description": "d", "changelog": "c"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the problem type follows API-46 and API-47
    And no vocabulary of this scenario is stored

    Examples:
      | ctype            |
      | application/json |
      | text/plain       |
      | text/turtle      |

  @D52 @D8 @negative @OWA-API3 @BLG-req-valid
  Scenario: An unknown form field is rejected with one issue naming it
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | Extra${run}      |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
      | status         | DEPRECATED       |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "status"
    And no vocabulary of this scenario is stored

  @D70 @D52 @negative
  Scenario: A multipart part with an empty name is an unknown field
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | Extra${run}      |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
      |                | something        |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named ""
    And no vocabulary of this scenario is stored

  @D52 @negative
  Scenario: A query parameter on POST /vocabularies is rejected
    When I send a multipart POST to "/vocabularies?dryRun=true" with
      | part           | value            |
      | name           | Query${run}      |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @valid           |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary of this scenario is stored

  @D1 @AC_SIMPL-18162_AC1
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list
    Given a vocabulary "Cpsv" exists with the file "cpsv-like.ttl"
    When I send a "GET" request to "/vocabularies?name=${Cpsv}"
    Then the response status is 200
    And the items are named, in order
      | name    |
      | ${Cpsv} |

  @D43 @D82
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When I create the vocabulary "Racer" twice in parallel
    Then the parallel responses have the statuses "201,409"
    And exactly 1 vocabularies of this scenario are stored
