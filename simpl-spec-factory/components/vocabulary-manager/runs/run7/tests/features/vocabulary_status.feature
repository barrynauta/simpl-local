Feature: Update the vocabulary status via the API
  PATCH /vocabularies/{vocabularyName} sets only the vocabulary status (SIMPL-18175, D3, D4).

  @AC_SIMPL-18175_AC1 @D3 @D4 @D25
  Scenario: The vocabulary status is set to DEPRECATED and the versions keep their status
    Given vocabulary "Alpha" has 2 versions
    And I save the field "lastUpdateDate" of vocabulary "Alpha" as "before"
    And one second passes
    When "ga-admin" sets the status of vocabulary "Alpha" to "DEPRECATED"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "name" equals "${V:Alpha}"
    And the response field "status" equals "DEPRECATED"
    And the response field "version" equals 2
    And the response field "href" equals "/vocabularies/${V:Alpha}"
    And the response fields "description, createdBy, creationDate, lastUpdateDate, contentHref" are present
    And the response field "lastUpdateDate" is later than the saved "before"
    And vocabulary "Alpha" has status "DEPRECATED"
    And version 2 of "Alpha" has status "ACTIVE"
    And version 1 of "Alpha" has status "DEPRECATED"

  @AC_SIMPL-18175_AC1 @D3 @D4
  Scenario: A DEPRECATED vocabulary is set back to ACTIVE
    Given vocabulary "Alpha" exists
    And vocabulary "Alpha" has status "DEPRECATED"
    When "ga-admin" sets the status of vocabulary "Alpha" to "ACTIVE"
    Then the response status is 200
    And the response field "status" equals "ACTIVE"
    And vocabulary "Alpha" has status "ACTIVE"
    And version 1 of "Alpha" has status "ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When "ga-admin" sets the status of vocabulary "Unknown" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${V:Unknown}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @AC_SIMPL-18175_AC3 @D4 @D24 @negative
  Scenario Outline: A status other than ACTIVE or DEPRECATED is rejected and the status stays (<body>)
    Given vocabulary "Alpha" exists
    When "ga-admin" sends PATCH "/vocabularies/${V:Alpha}" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And vocabulary "Alpha" has status "ACTIVE"

    Examples:
      | body                    |
      | {"status": "RETIRED"}   |
      | {"status": "deprecated"} |
      | {"status": ""}          |
      | {"status": null}        |
      | {}                      |

  @D52 @negative @BLG-req-valid
  Scenario: An unknown JSON property is rejected and the status stays
    Given vocabulary "Alpha" exists
    When "ga-admin" sends PATCH "/vocabularies/${V:Alpha}" with JSON
      """
      {"status": "DEPRECATED", "name": "Other"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "name"
    And vocabulary "Alpha" has status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on PATCH is rejected
    Given vocabulary "Alpha" exists
    When "ga-admin" sends PATCH "/vocabularies/${V:Alpha}?force=true" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And vocabulary "Alpha" has status "ACTIVE"

  @D4 @D51 @negative
  Scenario Outline: Asking for the current status gives 409 (<status>)
    Given vocabulary "Alpha" exists
    And vocabulary "Alpha" has status "<status>"
    When "ga-admin" sets the status of vocabulary "Alpha" to "<status>"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"

    Examples:
      | status     |
      | ACTIVE     |
      | DEPRECATED |

  @D43 @D4
  Scenario: Two overlapping identical status changes give 200 and 409
    Given vocabulary "Alpha" exists
    When "ga-admin" sets the status of vocabulary "Alpha" to "DEPRECATED" twice in parallel
    Then the parallel response statuses are "200, 409"
    And vocabulary "Alpha" has status "DEPRECATED"

  @D15 @negative @API-38
  Scenario: A PATCH that is not application/json is rejected with 415
    Given vocabulary "Alpha" exists
    When "ga-admin" sends PATCH "/vocabularies/${V:Alpha}" with the body "status=DEPRECATED" of type "application/x-www-form-urlencoded"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "Alpha" has status "ACTIVE"

  @D51 @negative
  Scenario: A malformed PATCH to an unknown name gives 400, not 404
    When "ga-admin" sends PATCH "/vocabularies/${V:Unknown}" with JSON
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D49 @negative
  Scenario: A PATCH on a vocabulary name that breaks the name rules gives 400
    When "ga-admin" sends PATCH "/vocabularies/lower${rid}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | type                                                     | in   | name           |
      | urn:problem-type:belgif:input-validation:schemaViolation | path | vocabularyName |
