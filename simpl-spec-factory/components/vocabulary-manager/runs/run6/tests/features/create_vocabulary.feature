Feature: Create a new vocabulary via the API
  POST /vocabularies stores a new vocabulary with version 1 when the content passes validation.
  The vocabulary and version 1 are ACTIVE in the same request (publish, D6).
  Any 4xx or 5xx leaves the store unchanged (D8).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @D6 @D7 @D10 @D14 @D26 @D27 @D31 @D58 @API-12 @API-36
  Scenario: A valid vocabulary is created as version 1 and ACTIVE
    When I create the vocabulary "Health"
    Then the response status is 201
    And the response media type is "application/json"
    And the response header "Location" is "/vocabularies/{{name:Health}}/versions/1"
    And the response field "href" is "/vocabularies/{{name:Health}}/versions/1"
    And the response field "contentHref" matches ".*/content/{{name:Health}}/versions/1"
    And the response field "name" is "{{name:Health}}"
    And the response field "version" is 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Description of Health {{run}}"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @D3 @D5 @D25 @D27 @D28 @E2
  Scenario: The created vocabulary can be read back with version 1 ACTIVE
    Given the vocabulary "Health" exists
    When I send "GET" to "/vocabularies/{{name:Health}}"
    Then the response status is 200
    And the response field "name" is "{{name:Health}}"
    And the response field "href" is "/vocabularies/{{name:Health}}"
    And the response field "contentHref" is "{{base}}/content/{{name:Health}}"
    And the response field "version" is 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Description of Health {{run}}"
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    When I send "GET" to "/vocabularies/{{name:Health}}/versions/1"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the response field "latestVersionHref" is "/vocabularies/{{name:Health}}/versions/1"

  @D1 @AC_SIMPL-18162_AC1
  Scenario: A CPSV-like vocabulary is uploaded and found in the list
    When I create the vocabulary "CorePublicServices" from the file "cpsv_like.ttl"
    Then the response status is 201
    When I list the vocabularies with "name={{name:CorePublicServices}}"
    Then the response status is 200
    And the item names are "CorePublicServices"

  @AC_SIMPL-18170_AC3 @D8 @D24 @D37 @NFR-04 @negative
  Scenario Outline: A create request without <field> is rejected and nothing is stored
    When I create the vocabulary "Health" with:
      | field   | value     |
      | <field> | (omitted) |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary named "Health" is stored

    Examples:
      | field          |
      | name           |
      | description    |
      | changelog      |
      | vocabularyFile |

  @AC_SIMPL-18170_AC3 @D8 @D37 @negative
  Scenario Outline: A create request with an invalid name is rejected
    When I create the vocabulary "Health" with:
      | field | value  |
      | name  | <name> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary of this run is stored

    Examples: names that break the rule of D37
      | name                                                                   |
      | lower{{run}}                                                           |
      | Has-hyphen{{run}}                                                      |
      | Has space{{run}}                                                       |
      | Accentué{{run}}                                                        |
      | A{{run}}01234567890123456789012345678901234567890123456789012           |

  @AC_SIMPL-18170_AC3 @D37 @negative
  Scenario: A create request with a name of two characters is rejected
    When I create the vocabulary "Health" with:
      | field | value |
      | name  | Ab    |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"

  @D37 @NFR-04
  Scenario: A name of 64 characters is accepted
    When I create the vocabulary "Health" with:
      | field | value                                                          |
      | name  | L{{run}}0123456789012345678901234567890123456789012345678901    |
    Then the response status is 201
    And the response field "name" matches "L{{run}}\d{52}"

  @AC_SIMPL-18170_AC3 @D8 @D37 @NFR-04 @negative
  Scenario Outline: A create request with an invalid <field> is rejected
    When I create the vocabulary "Health" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary named "Health" is stored

    Examples:
      | field       | value      |
      | description | (empty)    |
      | description | (spaces)   |
      | description | chars:1001 |
      | changelog   | (empty)    |
      | changelog   | (spaces)   |
      | changelog   | chars:1001 |

  @D37 @NFR-04
  Scenario: A description and a changelog of 1000 characters are accepted
    When I create the vocabulary "Health" with:
      | field       | value      |
      | description | chars:1000 |
      | changelog   | chars:1000 |
    Then the response status is 201

  @AC_SIMPL-18170_AC3 @D52 @OWA-API3 @BLG-req-valid @negative
  Scenario Outline: A create request with the unknown form field <extra> is rejected
    When I create the vocabulary "Health" with:
      | field   | value   |
      | <extra> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<extra>"
    And no vocabulary named "Health" is stored

    Examples:
      | extra     | value    |
      | colour    | blue     |
      | version   | 7        |
      | status    | ACTIVE   |
      | createdBy | intruder |

  @D52 @negative
  Scenario: A create request with a query parameter is rejected
    When I create the vocabulary "Health" with a query parameter "dryRun=true"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary named "Health" is stored

  @AC_SIMPL-18170_AC3 @D15 @D37 @D48 @API-46 @negative
  Scenario: A duplicate vocabulary name is rejected with 409
    Given the vocabulary "Health" exists
    When I create the vocabulary "Health"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '{{name:Health}}' already exists."
    And the vocabulary "Health" has 1 version

  @D37 @negative
  Scenario: A name that differs only in case is a duplicate
    Given the vocabulary "Health" exists
    When I create the vocabulary "Health" with:
      | field | value               |
      | name  | HEALTH{{runupper}}  |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And exactly one vocabulary named "Health" in any case is stored

  @D51 @negative
  Scenario: A duplicate name with invalid content gives 409, not 400
    Given the vocabulary "Health" exists
    When I create the vocabulary "Health" from the file "syntax_error_line3.ttl"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @AC_SIMPL-18170_AC3 @D15 @D48 @API-38 @API-46 @negative
  Scenario: A create request that is not multipart is rejected with 415
    When I create the vocabulary "Health" as JSON
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the problem type follows the simpl URN form
    And no vocabulary named "Health" is stored

  @D15 @D48 @NFR-04 @API-46 @negative
  Scenario: A file one byte above 5 MB is rejected with 413
    When I create the vocabulary "Health" with:
      | field          | value          |
      | vocabularyFile | turtle:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary named "Health" is stored

  @D15 @NFR-04
  Scenario: A file of exactly 5 MB is accepted
    When I create the vocabulary "Health" with:
      | field          | value          |
      | vocabularyFile | turtle:5242880 |
    Then the response status is 201

  @D51 @negative
  Scenario: The size limit is checked before the request shape
    When I create the vocabulary "Health" with:
      | field          | value          |
      | vocabularyFile | turtle:5242881 |
      | description    | (omitted)      |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D51 @D9 @negative
  Scenario: A missing field is reported before the content is validated
    When I create the vocabulary "Health" with:
      | field          | value                       |
      | vocabularyFile | file:syntax_error_line3.ttl |
      | changelog      | (omitted)                   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @D58
  Scenario: Times are UTC to the second with a Z
    When I create the vocabulary "Health"
    Then the response status is 201
    And the response field "creationDate" matches "\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z"
    And the response field "lastUpdateDate" matches "\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z"

  @D27
  Scenario: The creator of a vocabulary is the user who sent the request
    When I create the vocabulary "Health"
    Then the response status is 201
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the field "createdBy" of "/vocabularies/{{name:Health}}" is "{{email:ga-admin}}"

  @D43 @negative
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When two requests create the vocabulary "Twin" at the same time
    Then the parallel responses are one 201 and one 409
    And exactly one vocabulary named "Twin" in any case is stored
