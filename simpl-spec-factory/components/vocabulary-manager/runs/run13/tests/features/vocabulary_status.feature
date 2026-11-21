Feature: Change the vocabulary status and a version status
  PATCH /vocabularies/{vocabularyName} changes only the vocabulary status (D3).
  PATCH .../versions/{version} changes only that version's status (D4, D41).
  A request for the current status gives 409 (D4); a change sets lastUpdateDate (D25, D160).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3 @D25
  Scenario: The vocabulary is deprecated and its versions keep their status
    Given a vocabulary "Alpha" exists with 2 versions
    When I set the vocabulary status of "Alpha" to "DEPRECATED"
    Then the response status is 200
    And the response media type is "application/json"
    And the response JSON has
      | field       | value                  |
      | href        | /vocabularies/{Alpha}  |
      | contentHref | {base}/content/{Alpha} |
      | name        | {Alpha}                |
      | version     | 2                      |
      | status      | DEPRECATED             |
      | createdBy   | {admin_email}          |
    And the vocabulary "Alpha" has the vocabulary status "DEPRECATED"
    And version 2 of "Alpha" has the version status "ACTIVE"
    And version 1 of "Alpha" has the version status "DEPRECATED"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3
  Scenario: A DEPRECATED vocabulary is set back to ACTIVE
    Given a vocabulary "Alpha" exists
    And the vocabulary status of "Alpha" is "DEPRECATED"
    When I set the vocabulary status of "Alpha" to "ACTIVE"
    Then the response status is 200
    And the response JSON has
      | field  | value  |
      | status | ACTIVE |
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

  @D25 @D160
  Scenario: A status change sets the last update date of the vocabulary
    Given a vocabulary "Alpha" exists
    And I remember the field "lastUpdateDate" of the vocabulary "Alpha" as "before"
    And I wait one second
    When I set the vocabulary status of "Alpha" to "DEPRECATED"
    Then the response status is 200
    And the response field "lastUpdateDate" is later than "{$before}"

  @D4 @D160 @negative @API-46
  Scenario Outline: Asking for the current vocabulary status gives 409 and changes nothing: <status>
    Given a vocabulary "Alpha" exists
    And the vocabulary status of "Alpha" is "<status>"
    And I remember the state of the vocabulary "Alpha"
    When I set the vocabulary status of "Alpha" to "<status>"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"
    And the vocabulary "Alpha" is unchanged

    Examples:
      | status     |
      | ACTIVE     |
      | DEPRECATED |

  @AC_SIMPL-18175_AC3 @D4 @negative
  Scenario Outline: An unsupported status value is rejected and the status stays: <case>
    Given a vocabulary "Alpha" exists
    When I send PATCH "/vocabularies/{Alpha}" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

    Examples:
      | case                | body                    |
      | another value       | {"status": "ARCHIVED"}  |
      | lower case          | {"status": "deprecated"} |
      | empty string        | {"status": ""}          |
      | null                | {"status": null}        |
      | a number            | {"status": 1}           |
      | missing status      | {}                      |
      | not JSON            | status=DEPRECATED       |

  @D52 @negative @BLG-req-valid @OWA-API3
  Scenario: An unknown property in the status request is rejected
    Given a vocabulary "Alpha" exists
    When I send PATCH "/vocabularies/{Alpha}" with the JSON body
      """
      {"status": "DEPRECATED", "description": "Changed through the back door"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on a status change is rejected
    Given a vocabulary "Alpha" exists
    When I send PATCH "/vocabularies/{Alpha}?force=true" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I set the vocabulary status of "Ghost" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{Ghost}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @D15 @negative @API-38
  Scenario: A status change that is not JSON gets 415
    Given a vocabulary "Alpha" exists
    When I send PATCH "/vocabularies/{Alpha}" with the body "status=DEPRECATED" of type "application/x-www-form-urlencoded"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

  @D4 @D41
  Scenario: A single version is deprecated by hand and the vocabulary status stays
    Given a vocabulary "Alpha" exists
    When I set the version status of version 1 of "Alpha" to "DEPRECATED"
    Then the response status is 200
    And the response JSON has
      | field   | value                            |
      | href    | /vocabularies/{Alpha}/versions/1 |
      | version | 1                                |
      | status  | DEPRECATED                       |
    And version 1 of "Alpha" has the version status "DEPRECATED"
    And the vocabulary "Alpha" has the vocabulary status "ACTIVE"

  @D4 @D41
  Scenario: A reactivated old version is ACTIVE next to the latest, and the next version deprecates both
    Given a vocabulary "Alpha" exists with 2 versions
    When I set the version status of version 1 of "Alpha" to "ACTIVE"
    Then the response status is 200
    And version 1 of "Alpha" has the version status "ACTIVE"
    And version 2 of "Alpha" has the version status "ACTIVE"
    When I create a new version of "Alpha"
    Then the response is the created version 3 of "Alpha"
    And version 1 of "Alpha" has the version status "DEPRECATED"
    And version 2 of "Alpha" has the version status "DEPRECATED"

  @D160 @D25
  Scenario: A version status change sets the last update date of the version and the vocabulary
    Given a vocabulary "Alpha" exists
    And I remember the field "lastUpdateDate" of the vocabulary "Alpha" as "before"
    And I wait one second
    When I set the version status of version 1 of "Alpha" to "DEPRECATED"
    Then the response status is 200
    And the response field "lastUpdateDate" is later than "{$before}"
    When I remember the response field "lastUpdateDate" as "changed"
    And I send GET "/vocabularies/{Alpha}"
    Then the response JSON has
      | field          | value       |
      | lastUpdateDate | {$changed}  |

  @D4 @D160 @negative
  Scenario Outline: Asking for the current version status gives 409: <status>
    Given a vocabulary "Alpha" exists
    And version 1 of "Alpha" has the version status "<status>"
    And I remember the state of the vocabulary "Alpha"
    When I set the version status of version 1 of "Alpha" to "<status>"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status <status>"
    And the vocabulary "Alpha" is unchanged

    Examples:
      | status     |
      | ACTIVE     |
      | DEPRECATED |

  @D4 @D41 @negative
  Scenario: An unsupported version status is rejected
    Given a vocabulary "Alpha" exists
    When I send PATCH "/vocabularies/{Alpha}/versions/1" with the JSON body
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of "Alpha" has the version status "ACTIVE"

  @D41 @D55 @negative
  Scenario: A status change for a version that does not exist gives 404
    Given a vocabulary "Alpha" exists
    When I set the version status of version 9 of "Alpha" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 9 of vocabulary '{Alpha}' was not found."

  @D41 @D66 @negative
  Scenario: A version status change for a vocabulary that does not exist gives 404
    When I set the version status of version 1 of "Ghost" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{Ghost}' was not found."

  @D51 @negative
  Scenario: A malformed status change on an unknown vocabulary gives 400
    When I send PATCH "/vocabularies/{Ghost}" with the JSON body
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D51 @negative
  Scenario: The not-found check runs before the same-status check
    When I set the vocabulary status of "Ghost" to "ACTIVE"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
