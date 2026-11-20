Feature: Change the vocabulary status and the version status
  PATCH /vocabularies/{vocabularyName} sets the vocabulary status only (D3).
  PATCH /vocabularies/{vocabularyName}/versions/{version} sets one version status only (D4, D41).

  @AC_SIMPL-18175_AC1 @D3 @D25 @API-12
  Scenario: Deprecating a vocabulary returns the updated vocabulary and leaves the versions unchanged
    Given a vocabulary "Health" exists with 2 versions
    And I remember the vocabulary "Health" as "before"
    And the clock moves to the next second
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "name" is "${Health}"
    And the response field "status" is "DEPRECATED"
    And the response field "version" is 2
    And the response field "href" is "/vocabularies/${Health}"
    And the response field "lastUpdateDate" differs from "lastUpdateDate" of the remembered "before"
    And the response field "creationDate" equals "creationDate" of the remembered "before"
    And version 2 of "Health" has the status "ACTIVE"
    And version 1 of "Health" has the status "DEPRECATED"

  @AC_SIMPL-18175_AC1 @D3
  Scenario: Reactivating a vocabulary returns ACTIVE and leaves the versions unchanged
    Given a vocabulary "Health" exists
    And version 1 of "Health" was set to "DEPRECATED"
    And the vocabulary "Health" was set to "DEPRECATED"
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And version 1 of "Health" has the status "DEPRECATED"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: Changing the status of a vocabulary that does not exist gives 404
    When I send a "PATCH" request to "/vocabularies/${Ghost}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And no vocabulary named "Ghost" is stored

  @AC_SIMPL-18175_AC3 @D4 @negative
  Scenario Outline: A status value other than ACTIVE or DEPRECATED is rejected and the status stays (<body>)
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "status"
    And the vocabulary "Health" has the status "ACTIVE"

    Examples:
      | body                       |
      | {"status": "RETIRED"}      |
      | {"status": "deprecated"}   |
      | {"status": ""}             |
      | {"status": null}           |
      | {"status": 1}              |
      | {}                         |

  @D51 @negative
  Scenario: An invalid status for an unknown vocabulary gives 400, request shape comes before 404
    When I send a "PATCH" request to "/vocabularies/${Ghost}" with JSON
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D52 @negative @OWA-API3 @BLG-req-valid
  Scenario: An unknown JSON property in a status change is rejected
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "DEPRECATED", "name": "Renamed"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "name"
    And the vocabulary "Health" has the status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on PATCH is rejected
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}?force=true" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And the vocabulary "Health" has the status "ACTIVE"

  @AC_SIMPL-18175_AC3 @D4 @D15 @negative @API-38
  Scenario Outline: A status change that is not application/json is rejected with 415 (<path>)
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "<path>" with content type "text/plain" and body
      """
      status=DEPRECATED
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Health" has the status "ACTIVE"
    And version 1 of "Health" has the status "ACTIVE"

    Examples:
      | path                              |
      | /vocabularies/${Health}           |
      | /vocabularies/${Health}/versions/1 |

  @D4 @negative @API-46
  Scenario: Asking a vocabulary for its current status gives 409
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status ACTIVE"
    And the problem type follows API-46 and API-47

  @D4 @negative
  Scenario: Asking a DEPRECATED vocabulary for DEPRECATED gives 409
    Given a vocabulary "Health" exists
    And the vocabulary "Health" was set to "DEPRECATED"
    When I send a "PATCH" request to "/vocabularies/${Health}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status DEPRECATED"

  @D4 @D41 @D26
  Scenario: Deprecating one version sets only that version and returns it
    Given a vocabulary "Health" exists with 2 versions
    When I send a "PATCH" request to "/vocabularies/${Health}/versions/2" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response field "version" is 2
    And the response field "status" is "DEPRECATED"
    And the response field "href" is "/vocabularies/${Health}/versions/2"
    And version 1 of "Health" has the status "DEPRECATED"
    And the vocabulary "Health" has the status "ACTIVE"

  @D25 @D4 @D41
  Scenario: A version status change updates the last update date of the vocabulary
    Given a vocabulary "Health" exists
    And I remember the vocabulary "Health" as "before"
    And the clock moves to the next second
    When I send a "PATCH" request to "/vocabularies/${Health}/versions/1" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response field "lastUpdateDate" differs from "lastUpdateDate" of the remembered "before"
    And the response field "creationDate" equals "creationDate" of the remembered "before"
    And the response field "status" is "ACTIVE"

  @D4 @D41
  Scenario: Reactivating an older version leaves the other versions unchanged
    Given a vocabulary "Health" exists with 2 versions
    When I send a "PATCH" request to "/vocabularies/${Health}/versions/1" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And version 2 of "Health" has the status "ACTIVE"

  @D4 @negative
  Scenario: Asking a version for its current status gives 409
    Given a vocabulary "Health" exists with 2 versions
    When I send a "PATCH" request to "/vocabularies/${Health}/versions/1" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"

  @D4 @negative
  Scenario: An invalid version status is rejected
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}/versions/1" with JSON
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of "Health" has the status "ACTIVE"

  @D41 @D55 @D66 @negative
  Scenario: Changing the status of a version that does not exist gives 404
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "/vocabularies/${Health}/versions/4" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 4 of vocabulary '${Health}' was not found."
    When I send a "PATCH" request to "/vocabularies/${Ghost}/versions/1" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."

  @D43
  Scenario: Two identical overlapping status changes give 200 and 409
    Given a vocabulary "Health" exists
    When I set the status of "Health" to "DEPRECATED" twice in parallel
    Then the parallel responses have the statuses "200,409"
    And the vocabulary "Health" has the status "DEPRECATED"
