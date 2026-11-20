Feature: Change the status of a vocabulary or of one version through the API
  PATCH /vocabularies/{vocabularyName} sets the vocabulary status only; PATCH .../versions/{version}
  sets that version's status only (D3, D4, D41). Same-status requests give 409.

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3 @D25
  Scenario: Deprecating a vocabulary returns the updated vocabulary and leaves its versions unchanged
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    And I remember the field "lastUpdateDate" of "/vocabularies/${A}" as "before"
    And the clock has moved on by more than one second
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response content type is JSON
    And the response field "name" is "${A}"
    And the response field "status" is "DEPRECATED"
    And the response field "version" is 2
    And the response field "href" is "/vocabularies/${A}"
    And the response field "lastUpdateDate" is later than the remembered "before"
    And vocabulary "A" has status "DEPRECATED"
    And version 2 of vocabulary "A" has status "ACTIVE"
    And version 1 of vocabulary "A" has status "DEPRECATED"

  @AC_SIMPL-18175_AC1 @D3
  Scenario: A DEPRECATED vocabulary can be made ACTIVE again
    Given a vocabulary "A" exists
    And the status of vocabulary "A" is changed to "DEPRECATED"
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And vocabulary "A" has status "ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I send a PATCH request to "/vocabularies/${Missing}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary "Missing" is stored

  @AC_SIMPL-18175_AC3 @D4 @D24 @negative
  Scenario Outline: An unsupported status value is rejected and the status stays unchanged: <body>
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "status"
    And vocabulary "A" has status "ACTIVE"

    Examples:
      | body                      |
      | {"status": "RETIRED"}     |
      | {"status": "deprecated"}  |
      | {"status": ""}            |
      | {"status": null}          |
      | {"status": 1}             |
      | {}                        |

  @D52 @negative @OWA-API3
  Scenario: An unknown property in the status request is rejected
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "DEPRECATED", "version": 5}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "body" named "version"
    And vocabulary "A" has status "ACTIVE"

  @D24 @negative @BLG-err-sanit @OWT-A02
  Scenario: A status request that is not valid JSON is rejected without internal details
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "DEPRECATED"
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem reveals no implementation details
    And vocabulary "A" has status "ACTIVE"

  @D15 @negative @API-38
  Scenario: A status request that is not application/json gets 415
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}" with content type "text/plain" and body
      """
      DEPRECATED
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "A" has status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on a status change is rejected
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}?force=true" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And vocabulary "A" has status "ACTIVE"

  @D4 @negative @API-46
  Scenario Outline: A vocabulary status change to the current status gives 409: <status>
    Given a vocabulary "A" exists
    And the status of vocabulary "A" is changed to "<setup>"
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "<status>"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"
    And vocabulary "A" has status "<status>"

    Examples:
      | setup      | status     |
      | DEPRECATED | DEPRECATED |

  @D4 @negative
  Scenario: Activating an ACTIVE vocabulary gives 409
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status ACTIVE"

  @D4 @D41 @D26
  Scenario: Deprecating one version changes only that version
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    When I send a PATCH request to "/vocabularies/${A}/versions/2" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response field "version" is 2
    And the response field "status" is "DEPRECATED"
    And the response field "href" is "/vocabularies/${A}/versions/2"
    And version 1 of vocabulary "A" has status "DEPRECATED"
    And vocabulary "A" has status "ACTIVE"

  @D4 @D41
  Scenario: Reactivating an older version allows two ACTIVE versions
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    When I send a PATCH request to "/vocabularies/${A}/versions/1" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And versions 1 and 2 of vocabulary "A" have status "ACTIVE"

  @D4 @D41 @negative
  Scenario Outline: A version status change to the current status gives 409: <status>
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    When I send a PATCH request to "/vocabularies/${A}/versions/<version>" with JSON
      """
      {"status": "<status>"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status <status>"

    Examples:
      | version | status     |
      | 1       | DEPRECATED |
      | 2       | ACTIVE     |

  @D4 @D41 @negative
  Scenario: An unsupported version status is rejected
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}/versions/1" with JSON
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of vocabulary "A" has status "ACTIVE"

  @D41 @D55 @negative
  Scenario: A status change for a version that does not exist gives 404
    Given a vocabulary "A" exists
    When I send a PATCH request to "/vocabularies/${A}/versions/4" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 4 of vocabulary '${A}' was not found."

  @D41 @D66 @negative
  Scenario: A version status change for a vocabulary that does not exist gives 404
    When I send a PATCH request to "/vocabularies/${Missing}/versions/1" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."

  @D49 @negative
  Scenario Outline: A status change with an invalid path value gives 400: <path>
    When I send a PATCH request to "<path>" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "<parameter>" with type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | path                                | parameter      |
      | /vocabularies/bad_name              | vocabularyName |
      | /vocabularies/${A}/versions/0       | version        |
      | /vocabularies/${A}/versions/x       | version        |

  @D25 @D4
  Scenario: A version status change updates the vocabulary's last update date
    Given a vocabulary "A" exists
    And I remember the field "lastUpdateDate" of "/vocabularies/${A}" as "before"
    And the clock has moved on by more than one second
    When I send a PATCH request to "/vocabularies/${A}/versions/1" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    When I send a GET request to "/vocabularies/${A}"
    Then the response field "lastUpdateDate" is later than the remembered "before"

  @D43 @D4 @negative
  Scenario: Two identical overlapping status changes give 200 and 409
    Given a vocabulary "A" exists
    When I send 2 PATCH requests to "/vocabularies/${A}" in parallel with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the parallel responses have the statuses 200 and 409
    And vocabulary "A" has status "DEPRECATED"
