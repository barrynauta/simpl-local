Feature: Create a new vocabulary via the API
  POST /vocabularies stores a valid vocabulary as version 1, ACTIVE, in the same request
  (SIMPL-18170, SIMPL-18187, D6, D7, D25 to D29, D37).

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @AC_SIMPL-18194_AC2 @D6 @D7 @D10 @D26 @D27 @D31 @D58 @E1 @E2 @API-36
  Scenario: A valid vocabulary is created as version 1 and ACTIVE
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field       | value                      |
      | description | Healthcare resource types. |
      | changelog   | Initial version.           |
    Then the response status is 201
    And the response media type is "application/json"
    And the response header "Location" is "/vocabularies/${V:Alpha}/versions/1"
    And the response header "Location" equals the response field "href"
    And the response field "name" equals "${V:Alpha}"
    And the response field "version" equals 1
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Healthcare resource types."
    And the response field "changelog" equals "Initial version."
    And the response field "createdBy" equals "${EMAIL:ga-admin}"
    And the response field "contentHref" equals "${CB}/content/${V:Alpha}/versions/1"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @D3 @D5 @D25 @D27 @D28 @D58
  Scenario: The created vocabulary is returned with its metadata, version 1 and status ACTIVE
    Given vocabulary "Alpha" exists with the fields
      | field       | value                      |
      | description | Healthcare resource types. |
    When "ga-admin" gets vocabulary "Alpha"
    Then the response status is 200
    And the response field "name" equals "${V:Alpha}"
    And the response field "version" equals 1
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Healthcare resource types."
    And the response field "createdBy" equals "${EMAIL:ga-admin}"
    And the response field "href" equals "/vocabularies/${V:Alpha}"
    And the response field "contentHref" equals "${CB}/content/${V:Alpha}"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    When "ga-admin" gets version 1 of "Alpha"
    Then the response status is 200
    And the response field "status" equals "ACTIVE"
    And the response field "changelog" equals "Initial version."

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status can be changed to DEPRECATED and back to ACTIVE
    Given vocabulary "Alpha" exists
    When "ga-admin" sets the status of vocabulary "Alpha" to "DEPRECATED"
    Then the response status is 200
    And vocabulary "Alpha" has status "DEPRECATED"
    When "ga-admin" sets the status of vocabulary "Alpha" to "ACTIVE"
    Then the response status is 200
    And vocabulary "Alpha" has status "ACTIVE"

  @D1
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list (SIMPL-393 met by the generic upload)
    When "ga-admin" creates vocabulary "Cpsv" with the file "cpsv_like.ttl"
    Then the response status is 201
    And vocabulary "Cpsv" is stored

  @AC_SIMPL-18170_AC3 @D8 @D37 @D24 @negative @BLG-err-problem
  Scenario Outline: A request without the required field <field> is rejected and nothing is stored
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field   | value  |
      | <field> | <omit> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary of this scenario is stored

    Examples:
      | field          |
      | name           |
      | description    |
      | changelog      |
      | vocabularyFile |

  @AC_SIMPL-18170_AC3 @D8 @D37 @D24 @negative
  Scenario Outline: A name that breaks the name rules is rejected (<case>)
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field | value  |
      | name  | <name> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary of this scenario is stored

    Examples:
      | case                         | name             |
      | starts with lower case       | v${rid}lower     |
      | holds a hyphen               | V${rid}-Hyphen   |
      | holds a space                | V${rid} Space    |
      | starts with a digit          | 1V${rid}Digit    |
      | empty                        | <empty>          |

  @AC_SIMPL-18170_AC3 @D37 @Q2 @NFR-04 @negative
  Scenario: A name of 2 characters is rejected
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field | value |
      | name  | Vq    |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary named "Vq" is stored

  @D37 @Q2 @NFR-04
  Scenario: A name of exactly 3 characters passes the field checks
    # The store is never reset (E4), so an earlier run may hold the same short name.
    # 409 comes after the field checks (D51), so 201 and 409 both show the name was accepted.
    When "ga-admin" creates vocabulary "${NAME:3}"
    Then the response status is one of "201, 409"
    And the response status is not "400"

  @D37 @Q2 @NFR-04
  Scenario: A name of exactly 64 characters is accepted
    When "ga-admin" creates vocabulary "${NAME:64}"
    Then the response status is 201
    And the response field "name" equals "${NAME:64}"
    And vocabulary "${NAME:64}" is stored

  @AC_SIMPL-18170_AC3 @D37 @Q2 @NFR-04 @negative
  Scenario: A name of 65 characters is rejected
    When "ga-admin" creates vocabulary "${NAME:65}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary of this scenario is stored

  @D37 @Q2 @NFR-04
  Scenario: A description and a change log of 1000 characters are accepted
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field       | value       |
      | description | ${R:d:1000} |
      | changelog   | ${R:c:1000} |
    Then the response status is 201
    And the response field "description" equals "${R:d:1000}"

  @AC_SIMPL-18170_AC3 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: A <field> that breaks the length rules is rejected (<case>)
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary of this scenario is stored

    Examples:
      | case             | field       | value       |
      | 1001 characters  | description | ${R:d:1001} |
      | empty            | description | <empty>     |
      | only blanks      | description | ${R: :5}    |
      | 1001 characters  | changelog   | ${R:c:1001} |
      | empty            | changelog   | <empty>     |
      | only blanks      | changelog   | ${R: :5}    |

  @D15 @D37 @D51 @negative
  Scenario: A duplicate name is rejected with 409
    Given vocabulary "Alpha" exists
    When "ga-admin" creates vocabulary "Alpha"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '${V:Alpha}' already exists."
    And the problem type is a simpl problem type in lowerCamelCase
    And vocabulary "Alpha" has 1 version

  @D37 @D54 @negative
  Scenario: A name that differs only in case from a stored name is rejected with 409
    Given vocabulary "Alpha" exists
    When "ga-admin" creates vocabulary "${VU:Alpha}"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And no vocabulary named "${VU:Alpha}" is stored

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, not a validation error
    Given vocabulary "Alpha" exists
    When "ga-admin" creates vocabulary "Alpha" with the file "syntax_error.ttl"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D43 @D8
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When "ga-admin" creates vocabulary "Alpha" twice in parallel
    Then the parallel response statuses are "201, 409"
    And vocabulary "Alpha" has 1 version

  @D52 @D79 @negative @BLG-req-valid
  Scenario: An unknown form field is rejected and nothing is stored
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field  | value |
      | colour | blue  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "colour"
    And no vocabulary of this scenario is stored

  @D70 @D52 @negative
  Scenario: A multipart part with an empty name is an unknown field
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field | value |
      |       | stray |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named ""
    And no vocabulary of this scenario is stored

  @D52 @negative
  Scenario: A query parameter on POST /vocabularies is rejected
    When "ga-admin" sends a multipart POST to "/vocabularies?dryRun=true" with the fields
      | field          | value             |
      | name           | ${V:Alpha}        |
      | description    | A description.    |
      | changelog      | Initial version.  |
      | vocabularyFile | valid             |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18173_AC6 @D15 @D51 @negative @API-38 @API-46
  Scenario: A create request that is not multipart/form-data is rejected with 415
    When "ga-admin" sends POST "/vocabularies" with a valid vocabulary as JSON
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no vocabulary of this scenario is stored

  @D15 @Q2 @Q1 @D71 @NFR-04 @NFR-03 @slow
  Scenario: A file of exactly 5 MB is accepted within 30 seconds
    When "ga-admin" creates vocabulary "Big" with a valid file of 5242880 bytes
    Then the response status is 201
    And the request took at most 30 seconds
    When "ga-admin" downloads version 1 of "Big"
    Then the response status is 200
    And the response body is the content sent for version 1 of "Big"

  @D15 @D48 @Q2 @NFR-04 @negative @API-46
  Scenario: A file of 5 MB plus one byte is rejected with 413
    When "ga-admin" creates vocabulary "Big" with a valid file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary of this scenario is stored

  @D76 @Q2 @NFR-04 @negative
  Scenario: A multipart request over 6 MB is rejected with 413 even when the file is small
    When "ga-admin" creates vocabulary "Big" with the fields
      | field       | value           |
      | description | ${R:d:6400000}  |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary of this scenario is stored

  @D76 @D51 @Q2 @NFR-04 @negative
  Scenario: A request just under 6 MB with a 5 MB file is not refused for size
    When "ga-admin" creates vocabulary "Big" with the fields
      | field          | value                   |
      | vocabularyFile | valid of 5242880 bytes  |
      | description    | ${R:d:700000}           |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And no vocabulary of this scenario is stored

  @D51 @D15 @negative
  Scenario: The size check comes before the field checks
    When "ga-admin" creates vocabulary "Big" with the fields
      | field          | value                   |
      | vocabularyFile | valid of 5242881 bytes  |
      | name           | <omit>                  |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D51 @negative
  Scenario: The media type check comes before the size check
    When "ga-admin" sends POST "/vocabularies" with a body of 6400000 bytes of type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D51 @D9 @negative
  Scenario: The field checks come before content validation
    When "ga-admin" creates vocabulary "Alpha" with the fields
      | field          | value                    |
      | name           | v${rid}lower             |
      | vocabularyFile | fixture:syntax_error.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And the problem has no issue named "vocabularyFile"
