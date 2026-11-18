Feature: Update the vocabulary status and the version status via the API
  PATCH /vocabularies/{vocabularyName} changes only the vocabulary status (D3).
  PATCH .../versions/{version} changes only that version's status (D4, D41).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"

  @AC_SIMPL-18175_AC1 @D3 @D25 @API-12
  Scenario: Deprecating a vocabulary returns the updated vocabulary and leaves the versions unchanged
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I change the status of "{Alpha}" to "DEPRECATED"
    Then the response status is 200
    And the response content type is "application/json"
    And the response field "name" is "{Alpha}"
    And the response field "status" is "DEPRECATED"
    And the response field "version" equals 2
    And the response field "href" is "/vocabularies/{Alpha}"
    And the response field "lastUpdateDate" is a UTC time to the second
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And version 2 of "{Alpha}" has status "ACTIVE"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @AC_SIMPL-18175_AC1 @D3 @D25
  Scenario: Reactivating a DEPRECATED vocabulary returns it as ACTIVE
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    When I change the status of "{Alpha}" to "ACTIVE"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And version 1 of "{Alpha}" has status "ACTIVE"

  @AC_SIMPL-18175_AC1 @D25 @D58
  Scenario: A status change moves the vocabulary's last update date
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "/vocabularies/{Alpha}"
    And I remember the response field "lastUpdateDate" as "before"
    And I remember the response field "creationDate" as "created"
    And I wait until the next second
    And I change the status of "{Alpha}" to "DEPRECATED"
    Then the response status is 200
    And the response field "lastUpdateDate" is not earlier than the remembered "before"
    And the response field "lastUpdateDate" matches "^(?!{saved:before}$)"
    And the response field "creationDate" is "{saved:created}"

  @D4 @D48 @negative @API-46
  Scenario Outline: Asking for the current vocabulary status is a conflict (<status>)
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "<setup>"
    When I change the status of "{Alpha}" to "<status>"
    Then the response is a problem with status 409
    And the problem detail is "Vocabulary is already in status <status>"
    And the vocabulary "{Alpha}" has status "<status>"

    Examples:
      | setup      | status     |
      | DEPRECATED | DEPRECATED |

  @D4 @negative
  Scenario: Asking for ACTIVE on a new vocabulary is a conflict
    Given a vocabulary "{Alpha}" exists
    When I change the status of "{Alpha}" to "ACTIVE"
    Then the response is a problem with status 409
    And the problem detail is "Vocabulary is already in status ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D48 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I change the status of "{Ghost}" to "DEPRECATED"
    Then the response is a problem with status 404
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary named "{Ghost}" is stored

  @AC_SIMPL-18175_AC3 @D4 @D24 @negative
  Scenario Outline: An unsupported status value is rejected and the status stays (<case>)
    Given a vocabulary "{Alpha}" exists
    When I send "PATCH" to "/vocabularies/{Alpha}" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400
    And the vocabulary "{Alpha}" has status "ACTIVE"

    Examples:
      | case             | body                         |
      | unknown value    | {"status": "ARCHIVED"}       |
      | lower case value | {"status": "deprecated"}     |
      | empty value      | {"status": ""}               |
      | null value       | {"status": null}             |
      | number           | {"status": 1}                |
      | missing status   | {}                           |
      | not JSON         | status=DEPRECATED            |

  @D52 @negative @OWA-API3 @BLG-req-valid
  Scenario: An unknown property in the status request is rejected
    Given a vocabulary "{Alpha}" exists
    When I send "PATCH" to "/vocabularies/{Alpha}" with JSON
      """
      {"status": "DEPRECATED", "name": "{Other}"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "name"
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on PATCH is rejected
    Given a vocabulary "{Alpha}" exists
    When I send "PATCH" to "/vocabularies/{Alpha}?force=true" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "force"
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @D15 @negative @API-38
  Scenario: A status request that is not JSON gets 415
    Given a vocabulary "{Alpha}" exists
    When I send "PATCH" to "/vocabularies/{Alpha}" with the body "status=DEPRECATED" as "application/x-www-form-urlencoded"
    Then the response is a problem with status 415
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @D51 @negative
  Scenario: A malformed status request to an unknown vocabulary gives 400
    When I send "PATCH" to "/vocabularies/{Ghost}" with JSON
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400

  @D51 @negative
  Scenario: The media type is checked before existence on PATCH (415 before 404)
    When I send "PATCH" to "/vocabularies/{Ghost}" with the body "status=DEPRECATED" as "text/plain"
    Then the response is a problem with status 415

  @D54 @negative
  Scenario: The name in the PATCH path is matched exactly
    Given a vocabulary "{Alpha}" exists
    When I change the status of "{upper:Alpha}" to "DEPRECATED"
    Then the response is a problem with status 404
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @D43 @D4
  Scenario: Two identical overlapping status changes give 200 and 409
    Given a vocabulary "{Alpha}" exists
    When I change the status of "{Alpha}" to "DEPRECATED" 2 times in parallel
    Then the parallel responses have the statuses [200, 409]
    And the vocabulary "{Alpha}" has status "DEPRECATED"

  @D4 @D41 @D27 @API-12
  Scenario: Deprecating one version returns that version and leaves the vocabulary status
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I change the status of version 2 of "{Alpha}" to "DEPRECATED"
    Then the response status is 200
    And the response field "version" equals 2
    And the response field "status" is "DEPRECATED"
    And the response field "href" is "/vocabularies/{Alpha}/versions/2"
    And the response field "createdBy" identifies the user with role "GA_VOCABULARY_ADMIN"
    And the vocabulary "{Alpha}" has status "ACTIVE"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @D4 @D41
  Scenario: Reactivating an older version allows two ACTIVE versions
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I change the status of version 1 of "{Alpha}" to "ACTIVE"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And version 1 of "{Alpha}" has status "ACTIVE"
    And version 2 of "{Alpha}" has status "ACTIVE"

  @D4 @D25
  Scenario: A version status change moves the vocabulary's last update date
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I send "GET" to "/vocabularies/{Alpha}"
    And I remember the response field "lastUpdateDate" as "before"
    And I wait until the next second
    And I change the status of version 1 of "{Alpha}" to "ACTIVE"
    And I send "GET" to "/vocabularies/{Alpha}"
    Then the response field "lastUpdateDate" is not earlier than the remembered "before"
    And the response field "lastUpdateDate" matches "^(?!{saved:before}$)"

  @D4 @D41 @negative
  Scenario: Asking for the current version status is a conflict
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I change the status of version 1 of "{Alpha}" to "DEPRECATED"
    Then the response is a problem with status 409
    And the problem detail is "Vocabulary version is already in status DEPRECATED"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @D4 @D41 @negative
  Scenario: An unsupported version status value is rejected
    Given a vocabulary "{Alpha}" exists
    When I send "PATCH" to "/vocabularies/{Alpha}/versions/1" with JSON
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400
    And version 1 of "{Alpha}" has status "ACTIVE"

  @D41 @D55 @D24 @negative
  Scenario: A status change for a version that does not exist gives 404
    Given a vocabulary "{Alpha}" exists
    When I change the status of version 9 of "{Alpha}" to "DEPRECATED"
    Then the response is a problem with status 404
    And the problem detail is "Version 9 of vocabulary '{Alpha}' was not found."

  @D41 @negative
  Scenario: A version status change for a vocabulary that does not exist gives 404
    When I change the status of version 1 of "{Ghost}" to "DEPRECATED"
    Then the response is a problem with status 404

  @D49 @negative
  Scenario Outline: An invalid path value on PATCH gives 400 (<case>)
    Given a vocabulary "{Alpha}" exists
    When I send "PATCH" to "<path>" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "<parameter>"
    And every issue has the type "urn:problem-type:belgif:input-validation:schemaViolation"
    And version 1 of "{Alpha}" has status "ACTIVE"

    Examples:
      | case                     | path                              | parameter      |
      | name in lower case       | /vocabularies/alpha{run}          | vocabularyName |
      | name too short           | /vocabularies/Ab                  | vocabularyName |
      | version zero             | /vocabularies/{Alpha}/versions/0  | version        |
      | version not a number     | /vocabularies/{Alpha}/versions/v1 | version        |
      | version negative         | /vocabularies/{Alpha}/versions/-1 | version        |
