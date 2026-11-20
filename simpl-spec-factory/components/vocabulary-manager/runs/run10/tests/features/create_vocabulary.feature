@create
Feature: Create a vocabulary through the API
  POST /vocabularies stores a validated vocabulary with version 1 ACTIVE in the same request
  (publish, D6). The response is the vocabulary version with validationWarnings (D10, D26).

  Background:
    Given I use the token of "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @D5 @D6 @D7 @D10 @D26 @D27 @D31 @D58 @E2 @API-36
  Scenario: A valid vocabulary is created with version 1 ACTIVE
    When I create the vocabulary "A"
    Then the status is 201
    And the media type is "application/json"
    And the header "Location" is "/vocabularies/${A}/versions/1"
    And the response field "href" is "/vocabularies/${A}/versions/1"
    And the response field "contentHref" is "${base}/content/${A}/versions/1"
    And the response field "name" is "${A}"
    And the response field "version" equals 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Description of A"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" is a UTC timestamp
    And the response field "lastUpdateDate" is a UTC timestamp
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/1"
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @D3 @D5 @D25 @D27 @D28
  Scenario: The created vocabulary can be read back with its metadata and version 1
    When I create the vocabulary "A"
    And I remember the response field "creationDate" as "created"
    And I send "GET" to "/vocabularies/${A}"
    Then the status is 200
    And the response field "href" is "/vocabularies/${A}"
    And the response field "contentHref" is "${base}/content/${A}"
    And the response field "name" is "${A}"
    And the response field "version" equals 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Description of A"
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" is the remembered "created"
    And the response field "lastUpdateDate" is a UTC timestamp
    When I send "GET" to "/vocabularies/${A}/versions/1"
    Then the status is 200
    And the response field "status" is "ACTIVE"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${email:ga-admin}"

  @AC_SIMPL-18187_AC1 @D1 @D12
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list (SIMPL-393 through the generic upload)
    When I create the vocabulary "Cpsv" with the file "cpsv_like"
    Then the status is 201
    And the response has no validation warnings
    And the vocabulary list filtered by name "${Cpsv}" has 1 item

  @AC_SIMPL-18170_AC3 @D8 @D24 @D37 @D79 @negative
  Scenario Outline: A request without the <part> part is rejected and stores nothing
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value          |
      | <p1>           | <v1>           |
      | <p2>           | <v2>           |
      | <p3>           | <v3>           |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And the vocabulary list filtered by name "${A}" has 0 items

    Examples:
      | part           | p1             | v1           | p2          | v2      | p3          | v3               |
      | vocabularyFile | name           | ${A}         | description | Desc    | changelog   | Initial version. |
      | description    | vocabularyFile | @file:valid  | name        | ${A}    | changelog   | Initial version. |
      | changelog      | vocabularyFile | @file:valid  | name        | ${A}    | description | Desc             |

  @AC_SIMPL-18170_AC3 @D37 @negative
  Scenario: A request without a name is rejected
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value       |
      | vocabularyFile | @file:valid |
      | description    | Desc        |
      | changelog      | Initial.    |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"

  @AC_SIMPL-18170_AC3 @D8 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: An invalid name "<name>" is rejected and stores nothing
    When I create the vocabulary named "<name>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And the vocabulary list filtered by name "<filter>" has 0 items

    Examples: pattern and length
      | name                | filter          |
      | ${lower:Lower}      | ${lower:Lower}  |
      | ${A}_x              | ${A}            |
      | ${A}-x              | ${A}            |
      | 9${A}               | ${A}            |
      | ${A} x              | ${A}            |
      | ${long:65}          | ${long:64}      |

  @AC_SIMPL-18170_AC3 @D37 @Q2 @NFR-04 @negative
  Scenario: A two-character name is below the limit and is rejected
    When I create the vocabulary named "Ab"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"

  @D37 @Q2 @NFR-04 @D51
  Scenario: A three-character name is at the limit and passes the field check
    # The store is shared and never reset (E4), so the name may already exist: 409 then also proves the field check passed.
    When I create the vocabulary named "Vtq"
    Then the status is one of "201, 409"

  @D37 @Q2 @NFR-04
  Scenario: A 64-character name is at the limit and is accepted
    When I create the vocabulary named "${long:64}"
    Then the status is 201
    And the vocabulary list filtered by name "${long:64}" has 1 item

  @AC_SIMPL-18170_AC3 @D37 @D79 @Q2 @NFR-04 @negative
  Scenario Outline: An invalid description (<case>) is rejected and stores nothing
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value            |
      | vocabularyFile | @file:valid      |
      | name           | ${A}             |
      | description    | <description>    |
      | changelog      | Initial version. |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And the vocabulary list filtered by name "${A}" has 0 items

    Examples:
      | case                 | description |
      | empty                | @empty      |
      | spaces only          | @spaces:3   |
      | 1001 characters      | @text:1001  |

  @D37 @Q2 @NFR-04
  Scenario: A description of 1000 characters is at the limit and is accepted
    When I create the vocabulary "A" with the description "${text:1000}"
    Then the status is 201
    And the response field "description" is "${text:1000}"

  @D37 @Q2 @NFR-04
  Scenario: A description of 1000 characters plus surrounding spaces is accepted (limit after trimming)
    When I create the vocabulary "A" with the description "  ${text:1000}  "
    Then the status is 201

  @AC_SIMPL-18170_AC3 @D37 @negative
  Scenario: An empty changelog is rejected and stores nothing
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value       |
      | vocabularyFile | @file:valid |
      | name           | ${A}        |
      | description    | Desc        |
      | changelog      | @empty      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"
    And the vocabulary list filtered by name "${A}" has 0 items

  @D48 @AC_SIMPL-18170_AC3 @D15 @D30 @D37 @D51 @API-46 @API-47 @negative
  Scenario: A duplicate name is rejected with 409 and only one vocabulary is stored
    Given a vocabulary "A" exists
    When I create the vocabulary "A"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '${A}' already exists."
    And the vocabulary list filtered by name "${A}" has 1 item

  @D37 @negative
  Scenario: A name that differs only in case from a stored name is rejected with 409
    Given a vocabulary "Case" exists
    When I create the vocabulary named "${upper:Case}"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the vocabulary list filtered by name "${Case}" has 1 item

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, because conflicts are checked before content validation
    Given a vocabulary "A" exists
    When I create the vocabulary "A" with the file "syntax_error"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D51 @negative
  Scenario: A duplicate name with a missing description gives 400, because field rules come before conflicts
    Given a vocabulary "A" exists
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value            |
      | vocabularyFile | @file:valid      |
      | name           | ${A}             |
      | changelog      | Initial version. |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"

  @AC_SIMPL-18170_AC3 @D52 @D79 @OWA-API3 @BLG-req-valid @negative
  Scenario Outline: An unknown form field "<field>" is rejected and stores nothing
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value            |
      | vocabularyFile | @file:valid      |
      | name           | ${A}             |
      | description    | Desc             |
      | changelog      | Initial version. |
      | <field>        | 1                |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And the vocabulary list filtered by name "${A}" has 0 items

    Examples:
      | field     |
      | version   |
      | status    |
      | createdBy |
      | foo       |

  @D70 @D52 @negative
  Scenario: A multipart part with an empty name is an unknown field
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value            |
      | vocabularyFile | @file:valid      |
      | name           | ${A}             |
      | description    | Desc             |
      | changelog      | Initial version. |
      |                | stray            |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named ""
    And the vocabulary list filtered by name "${A}" has 0 items

  @D110 @NFR-04 @Q2 @negative
  Scenario: A multipart request with more than 64 parts is rejected
    When I send a multipart "POST" to "/vocabularies" with 61 extra parts
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the vocabulary list filtered by name "${A}" has 0 items

  @D52 @D21 @negative
  Scenario Outline: A query parameter on POST /vocabularies is rejected: <query>
    When I send a multipart "POST" to "/vocabularies?<query>" with these parts
      | name           | value            |
      | vocabularyFile | @file:valid      |
      | name           | ${A}             |
      | description    | Desc             |
      | changelog      | Initial version. |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"
    And the vocabulary list filtered by name "${A}" has 0 items

    Examples:
      | query    | param |
      | foo=bar  | foo   |
      | page=1   | page  |

  @D48 @AC_SIMPL-18173_AC6 @D15 @D8 @API-38 @API-46 @API-47 @negative
  Scenario Outline: A create request that is not multipart (<ctype>) gives 415 and stores nothing
    When I send "POST" to "/vocabularies" with content type "<ctype>" and the body
      """
      {"name": "${A}", "description": "Desc", "changelog": "Initial version."}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary list filtered by name "${A}" has 0 items

    Examples:
      | ctype                             |
      | application/json                  |
      | text/turtle                       |
      | application/x-www-form-urlencoded |

  @D48 @D15 @D76 @D8 @Q2 @NFR-04 @API-46 @API-47 @negative
  Scenario: A file of 5 242 881 bytes is above the limit: 413 and nothing stored
    When I create the vocabulary "A" with a file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the vocabulary list filtered by name "${A}" has 0 items

  @D15 @D71 @D76 @Q2 @NFR-04
  Scenario: A file of exactly 5 242 880 bytes is at the limit and is accepted
    When I create the vocabulary "A" with a file of 5242880 bytes
    Then the status is 201
    When I send "GET" to "/content/${A}" with the header "Accept" set to "text/turtle"
    Then the status is 200
    And the body is the file of version 1 of "A"

  @D76 @Q2 @NFR-04 @negative
  Scenario: A multipart request over 6 MB with a small file is rejected with 413
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value            |
      | vocabularyFile | @file:valid      |
      | name           | ${A}             |
      | description    | @text:6400000    |
      | changelog      | Initial version. |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the vocabulary list filtered by name "${A}" has 0 items

  @D76 @D51 @Q2 @NFR-04 @negative
  Scenario: A multipart request of exactly 6 MB is not refused for its size
    # No valid request can reach 6 MB (file at most 5 MB, text fields at most 1000 characters), so the
    # description carries the padding: the answer is the field error, not 413, because 413 is checked first (D51).
    When I send a multipart "POST" to "/vocabularies" of exactly 6291456 bytes with a padded description
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And the vocabulary list filtered by name "${A}" has 0 items

  @D51 @D76 @negative
  Scenario: Size is checked before the field rules: an oversized file with an invalid name gives 413
    When I send a multipart "POST" to "/vocabularies" with these parts
      | name           | value            |
      | vocabularyFile | @size:5300000    |
      | name           | not-valid        |
      | description    | Desc             |
      | changelog      | Initial version. |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D48 @D51 @D63 @API-37 @API-46 @API-47 @negative
  Scenario: The Accept header is checked before the media type: 406 wins over 415
    When I send "POST" to "/vocabularies" with the header "Accept" set to "application/xml" and the JSON body
      """
      {"name": "${A}"}
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues

  @D43 @D37
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When I create the vocabulary "A" twice in parallel
    Then the parallel responses have the statuses "201, 409"
    And the 409 response has the type "urn:problem-type:simpl:conflict"
    And the vocabulary list filtered by name "${A}" has 1 item
