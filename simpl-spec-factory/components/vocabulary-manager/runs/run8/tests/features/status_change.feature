Feature: Change the vocabulary status and a version status through the API
  PATCH /vocabularies/{vocabularyName} sets only the vocabulary status.
  PATCH /vocabularies/{vocabularyName}/versions/{version} sets only that
  version's status. Asking for the current status gives 409.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3 @D25 @D27
  Scenario: Deprecate a vocabulary; its versions keep their status
    Given a vocabulary "Health" exists
    And I remember field "lastUpdateDate" of GET "/vocabularies/${Health}" as "before"
    And I wait for the next second
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "name" is "${Health}"
    And the response field "status" is "DEPRECATED"
    And the response field "version" is "1"
    And the response field "href" is "/vocabularies/${Health}"
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "lastUpdateDate" is later than the remembered "before"
    And vocabulary "Health" has vocabulary status "DEPRECATED"
    And version 1 of "Health" has version status "ACTIVE"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3
  Scenario: Reactivate a DEPRECATED vocabulary
    Given a vocabulary "Health" exists
    And the vocabulary status of "Health" is set to "DEPRECATED"
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And vocabulary "Health" has vocabulary status "ACTIVE"

  @D4 @D48 @negative @API-46
  Scenario Outline: Asking for the current vocabulary status gives 409
    Given a vocabulary "Health" exists
    And the vocabulary status of "Health" is set to "<setup>"
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      {"status": "<status>"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"
    And vocabulary "Health" has vocabulary status "<status>"

    Examples:
      | setup      | status     |
      | DEPRECATED | DEPRECATED |

  @D4 @negative
  Scenario: Asking for ACTIVE on a new vocabulary gives 409
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I send a PATCH request to "/vocabularies/${Ghost}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary whose name contains "${uid}" is stored

  @AC_SIMPL-18175_AC3 @D4 @D24 @negative
  Scenario Outline: A status other than ACTIVE or DEPRECATED is rejected and the status stays
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "status"
    And vocabulary "Health" has vocabulary status "ACTIVE"

    Examples:
      | body                     |
      | {"status": "ARCHIVED"}   |
      | {"status": "deprecated"} |
      | {"status": ""}           |
      | {"status": null}         |
      | {}                       |

  @D52 @negative @OWA-API3 @BLG-req-valid
  Scenario: An unknown JSON property is rejected and the status stays
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      {"status": "DEPRECATED", "name": "Renamed${uid}"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "name"
    And vocabulary "Health" has vocabulary status "ACTIVE"
    And no vocabulary whose name contains "Renamed${uid}" is stored

  @D15 @negative @API-38
  Scenario: A status change that is not application/json is rejected with 415
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}" with body "status=DEPRECATED" and content type "application/x-www-form-urlencoded"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "Health" has vocabulary status "ACTIVE"

  @D51 @negative
  Scenario: A malformed status change to an unknown vocabulary gives 400
    When I send a PATCH request to "/vocabularies/${Ghost}" with JSON body
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400

  @D51 @D4 @negative
  Scenario: A status change for a version that does not exist gives 404
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}/versions/2" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 2 of vocabulary '${Health}' was not found."

  @D16 @D17 @AC_SIMPL-18196_AC2 @negative @OWASP-02
  Scenario: A viewer cannot change the vocabulary status
    Given a vocabulary "Health" exists
    And I am authenticated as "ga-viewer"
    When I send a PATCH request to "/vocabularies/${Health}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And vocabulary "Health" has vocabulary status "ACTIVE"

  # ---- version status (API only) ------------------------------------------

  @D4 @D41 @D26 @D3
  Scenario: Deprecate a single version; the vocabulary status stays
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response field "version" is "1"
    And the response field "status" is "DEPRECATED"
    And the response field "href" is "/vocabularies/${Health}/versions/1"
    And the response field "lastUpdateDate" is a UTC time to the second
    And version 1 of "Health" has version status "DEPRECATED"
    And vocabulary "Health" has vocabulary status "ACTIVE"

  @D4 @D41
  Scenario: Reactivating an old version leaves two ACTIVE versions
    Given a vocabulary "Health" exists with 2 versions
    When I send a PATCH request to "/vocabularies/${Health}/versions/1" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And version 1 of "Health" has version status "ACTIVE"
    And version 2 of "Health" has version status "ACTIVE"

  @D25 @D4
  Scenario: A version status change updates the last update date of the vocabulary
    Given a vocabulary "Health" exists
    And I remember field "lastUpdateDate" of GET "/vocabularies/${Health}" as "before"
    And I wait for the next second
    When I send a PATCH request to "/vocabularies/${Health}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    When I send a GET request to "/vocabularies/${Health}"
    Then the response field "lastUpdateDate" is later than the remembered "before"
    And the response field "status" is "ACTIVE"

  @D4 @D41 @negative
  Scenario: Asking for the current version status gives 409
    Given a vocabulary "Health" exists with 2 versions
    When I send a PATCH request to "/vocabularies/${Health}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"

  @D4 @D41 @D55 @negative
  Scenario: A version status change for an unknown version gives 404
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}/versions/9" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 9 of vocabulary '${Health}' was not found."

  @D4 @D41 @D66 @negative
  Scenario: A version status change for an unknown vocabulary gives 404
    When I send a PATCH request to "/vocabularies/${Ghost}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."

  @D4 @negative
  Scenario: A version status other than ACTIVE or DEPRECATED is rejected
    Given a vocabulary "Health" exists
    When I send a PATCH request to "/vocabularies/${Health}/versions/1" with JSON body
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "status"
    And version 1 of "Health" has version status "ACTIVE"

  @D16 @negative @OWASP-02
  Scenario: A viewer cannot change a version status
    Given a vocabulary "Health" exists
    And I am authenticated as "ga-viewer"
    When I send a PATCH request to "/vocabularies/${Health}/versions/1" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And version 1 of "Health" has version status "ACTIVE"
