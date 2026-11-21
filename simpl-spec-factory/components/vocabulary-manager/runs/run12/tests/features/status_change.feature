Feature: Change the vocabulary status and the version status
  PATCH /vocabularies/{vocabularyName} (SIMPL-18175) and PATCH .../versions/{version} (D4, D41).
  The two statuses are independent (D3). Same-status requests give 409 (D4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18175_AC1 @D3 @D25 @D160 @API-12
  Scenario: Deprecating a vocabulary returns its metadata and leaves the versions unchanged
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    And the response media type is "application/json"
    And the response field "name" equals "{{Health}}"
    And the response field "status" equals "DEPRECATED"
    And the response field "version" equals 2
    And the response field "href" equals "/vocabularies/{{Health}}"
    And the response field "lastUpdateDate" is a UTC timestamp
    And vocabulary "{{Health}}" has status "DEPRECATED"
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=ACTIVE"

  @AC_SIMPL-18175_AC1 @D3 @D4
  Scenario: A DEPRECATED vocabulary can be set back to ACTIVE
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has status "DEPRECATED"
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    And the response field "status" equals "ACTIVE"
    And vocabulary "{{Health}}" has status "ACTIVE"

  @D25
  Scenario: A status change updates the last update date of the vocabulary
    Given a vocabulary "{{Health}}" exists
    And one second passes
    When I send "GET" to "/vocabularies/{{Health}}"
    And I remember the response field "lastUpdateDate" as "before"
    And I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response field "lastUpdateDate" is later than "{{before}}"
    And the response field "creationDate" is a UTC timestamp

  @D4 @D48 @negative @API-46
  Scenario Outline: Asking for the current vocabulary status gives 409 (<status>)
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has status "<setup>"
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "<status>"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"
    And vocabulary "{{Health}}" has status "<status>"

    Examples:
      | setup      | status     |
      | ACTIVE     | ACTIVE     |
      | DEPRECATED | DEPRECATED |

  @AC_SIMPL-18175_AC3 @D4 @negative
  Scenario Outline: A status value other than ACTIVE or DEPRECATED is rejected and the status stays (<body>)
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And vocabulary "{{Health}}" has status "ACTIVE"

    Examples:
      | body                     |
      | {"status": "RETIRED"}    |
      | {"status": "deprecated"} |
      | {"status": ""}           |
      | {"status": null}         |
      | {"status": 1}            |
      | {}                       |
      | not json                 |

  @D52 @OWA-API3 @negative
  Scenario: A JSON property the contract does not define is rejected
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "DEPRECATED", "description": "Changed through PATCH"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "description"
    And vocabulary "{{Health}}" has status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on PATCH is rejected
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}?force=true" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "force"
    And vocabulary "{{Health}}" has status "ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: Changing the status of a vocabulary that does not exist gives 404
    When I send "PATCH" to "/vocabularies/{{Missing}}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D51 @negative
  Scenario: An invalid status for a vocabulary that does not exist gives 400, not 404
    When I send "PATCH" to "/vocabularies/{{Missing}}" with JSON body
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D49 @negative
  Scenario: An invalid name in the PATCH path gives 400 for the path
    When I send "PATCH" to "/vocabularies/bad-name" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D15 @negative @API-38
  Scenario: A PATCH that is not application/json gets 415
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}" with body "status=DEPRECATED" and content type "application/x-www-form-urlencoded"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "{{Health}}" has status "ACTIVE"

  @D150 @D110 @NFR-04 @Q2 @negative
  Scenario: A JSON body over 1 MB gives 413
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}" with a JSON body of 1048577 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the problem detail is "The request body is larger than 1 MB."
    And the problem has no issues
    And vocabulary "{{Health}}" has status "ACTIVE"

  # ------------------------------------------------------------ version status

  @D4 @D41 @D160
  Scenario: A version can be deprecated by hand
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    And the response field "status" equals "DEPRECATED"
    And the response field "version" equals 1
    And the response field "href" equals "/vocabularies/{{Health}}/versions/1"
    And vocabulary "{{Health}}" has status "ACTIVE"

  @D4 @D41
  Scenario: Reactivating an older version gives two ACTIVE versions
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I send "PATCH" to "/vocabularies/{{Health}}/versions/1" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    And the versions of vocabulary "{{Health}}" have the statuses "1=ACTIVE, 2=ACTIVE"

  @D160 @D58
  Scenario: A version status change sets the last update date of the version and the vocabulary
    Given a vocabulary "{{Health}}" exists
    And one second passes
    When I send "PATCH" to "/vocabularies/{{Health}}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    And I remember the response field "lastUpdateDate" as "changed"
    And I send "GET" to "/vocabularies/{{Health}}"
    Then the response field "lastUpdateDate" equals "{{changed}}"

  @D4 @D160 @negative
  Scenario: Asking for the current version status gives 409 and changes nothing
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    And I remember the response field "lastUpdateDate" as "before"
    And I send "PATCH" to "/vocabularies/{{Health}}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the response field "lastUpdateDate" equals "{{before}}"

  @D4 @negative
  Scenario: An unsupported version status is rejected
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}/versions/1" with JSON body
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @D41 @D55 @negative
  Scenario: Changing the status of a version that does not exist gives 404
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}/versions/5" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 5 of vocabulary '{{Health}}' was not found."

  @D41 @D66 @negative
  Scenario: Changing a version status of a vocabulary that does not exist gives 404
    When I send "PATCH" to "/vocabularies/{{Missing}}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."

  @D49 @negative
  Scenario: An invalid version number in the PATCH path gives 400 for the path
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}/versions/0" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "version"

  @D150 @D52 @NFR-04 @Q2 @negative
  Scenario: A JSON body of exactly 1 MB passes the size check
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}" with a JSON body of 1048576 bytes
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "pad"
    And vocabulary "{{Health}}" has status "ACTIVE"
