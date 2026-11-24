Feature: Change the vocabulary status and a version status via the API
  PATCH /vocabularies/{vocabularyName} sets only the vocabulary status (D3).
  PATCH /vocabularies/{vocabularyName}/versions/{version} sets only that version's status (D4, D41).
  The same status gives 409 (D4); any other value 400.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18175_AC1 @D3 @D25 @D4 @D208
  Scenario: The vocabulary status is set to DEPRECATED and the versions keep their status
    Given vocabulary "$Alpha" has 2 versions
    And I save field "lastUpdateDate" of "/vocabularies/$Alpha" as "before"
    And a second has passed
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "status" equals "DEPRECATED"
    And the response field "name" equals "$Alpha"
    And the response field "href" equals "/vocabularies/$Alpha"
    And the response field "version" equals 2
    And the response field "lastUpdateDate" is later than the saved "before"
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"
    And version 2 of vocabulary "$Alpha" has version status "ACTIVE"
    And version 1 of vocabulary "$Alpha" has version status "DEPRECATED"

  @AC_SIMPL-18175_AC1 @D3
  Scenario: The vocabulary status is restored to ACTIVE
    Given a vocabulary "$Alpha" exists
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" equals "ACTIVE"
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I send a PATCH request to "/vocabularies/$Missing" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '$Missing' was not found."
    And the problem has 1 issue
    And issue 1 is in "path" named "vocabularyName"

  @AC_SIMPL-18175_AC3 @D4 @negative
  Scenario Outline: A status value other than ACTIVE or DEPRECATED is rejected: <body>
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

    Examples:
      | body                     |
      | {"status": "ARCHIVED"}   |
      | {"status": "active"}     |
      | {"status": "Deprecated"} |
      | {"status": ""}           |
      | {"status": null}         |
      | {"status": 1}            |
      | {}                       |
      | {"state": "DEPRECATED"}  |
      | not json                 |

  @AC_SIMPL-18175_AC3 @D4 @negative
  Scenario: An invalid status value names the status field
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "status"

  @D52 @negative
  Scenario: An unknown property next to a valid status is rejected and nothing changes
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED", "reason": "old"}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "reason"
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

  @D52 @negative
  Scenario: A query parameter on a PATCH is rejected
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha?force=true" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "force"
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

  @D4 @negative @API-46
  Scenario: Asking for the current status ACTIVE gives 409
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status ACTIVE"

  @D4 @negative
  Scenario: Asking for the current status DEPRECATED gives 409 and changes nothing
    Given a vocabulary "$Alpha" exists
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"
    And I save field "lastUpdateDate" of "/vocabularies/$Alpha" as "before"
    And a second has passed
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409
    And the problem detail is "Vocabulary is already in status DEPRECATED"
    When I read "/vocabularies/$Alpha" as "ga-admin"
    Then the response field "lastUpdateDate" is the saved "before"

  @D43 @D4 @negative
  Scenario: Two identical overlapping status changes give 200 and 409
    Given a vocabulary "$Alpha" exists
    When I send 2 identical requests to "/vocabularies/$Alpha" in parallel with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the parallel responses have the status codes "200, 409"
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"

  @D15 @API-38 @negative @API-46
  Scenario Outline: A status change that is not application/json gets 415: <ctype>
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with the body "status=DEPRECATED" as "<ctype>"
    Then the response is a problem with status 415
    And the problem type is "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

    Examples:
      | ctype                             |
      | text/plain                        |
      | application/x-www-form-urlencoded |
      | application/merge-patch+json      |

  @D150 @D110 @negative @Q2 @NFR-04
  Scenario: A JSON body over 1 MB gets 413
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with a JSON body of 1048577 bytes
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"
    And the problem detail is "The request body is larger than 1 MB."
    And the problem has no issues
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

  @D63 @negative
  Scenario: An Accept header the PATCH cannot serve gets 406 and nothing changes
    Given a vocabulary "$Alpha" exists
    And the request header "Accept" is "text/turtle"
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 406
    And the problem type is "urn:problem-type:simpl:notAcceptable"
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

  @D51 @negative
  Scenario: A malformed status change to an unknown name gives 400, not 404
    When I send a PATCH request to "/vocabularies/$Missing" with JSON:
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400

  @D51 @D4 @negative
  Scenario: An unknown vocabulary gives 404 before the same-status conflict
    When I send a PATCH request to "/vocabularies/$Missing" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 404

  @D49 @negative
  Scenario Outline: An invalid name in the PATCH path gives 400: <name>
    When I send a PATCH request to "/vocabularies/<name>" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "path" named "vocabularyName"
    And issue 1 has type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | name   |
      | ab     |
      | $alpha |
      | $N65   |

  @D54 @negative
  Scenario: A PATCH with the name in another case gives 404 and changes nothing
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha!upper" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"

  @D4 @D41 @D160 @D25
  Scenario: A version status is set to DEPRECATED and the dates are updated
    Given a vocabulary "$Alpha" exists
    And I save field "lastUpdateDate" of "/vocabularies/$Alpha" as "before"
    And a second has passed
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "status" equals "DEPRECATED"
    And the response field "version" equals 1
    And the response field "href" equals "/vocabularies/$Alpha/versions/1"
    And the response field "lastUpdateDate" is later than the saved "before"
    And I save the response field "lastUpdateDate" as "changed"
    When I read "/vocabularies/$Alpha" as "ga-admin"
    Then the response field "lastUpdateDate" is the saved "changed"
    And the response field "status" equals "ACTIVE"

  @D4 @D41
  Scenario: Reactivating an older version leaves two ACTIVE versions
    Given vocabulary "$Alpha" has 2 versions
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" equals "ACTIVE"
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"
    And version 2 of vocabulary "$Alpha" has version status "ACTIVE"

  @D4 @negative
  Scenario: A version PATCH with the current status gives 409
    Given vocabulary "$Alpha" has 2 versions
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"
    When I send a PATCH request to "/vocabularies/$Alpha/versions/2" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409
    And the problem detail is "Vocabulary version is already in status ACTIVE"

  @D4 @D41 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha/versions/9" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 9 of vocabulary '$Alpha' was not found."

  @D41 @D66 @negative
  Scenario: A version PATCH on a vocabulary that does not exist gives 404
    When I send a PATCH request to "/vocabularies/$Missing/versions/1" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404
    And the problem detail is "Vocabulary with name '$Missing' was not found."

  @D4 @negative
  Scenario Outline: A version status value other than ACTIVE or DEPRECATED is rejected: <body>
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      <body>
      """
    Then the response is a problem with status 400
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

    Examples:
      | body                   |
      | {"status": "RETIRED"}  |
      | {"status": "active"}   |
      | {}                     |

  @D49 @D91 @D112 @negative
  Scenario Outline: An invalid version in the PATCH path gives 400: <version>
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha/versions/<version>" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "path" named "version"
    And issue 1 has type "urn:problem-type:belgif:input-validation:schemaViolation"
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

    Examples:
      | version    |
      | 0          |
      | -1         |
      | abc        |
      | 1.0        |
      | 1e1        |
      | 2147483648 |

  @D16 @negative
  Scenario: A viewer cannot change a version status
    Given a vocabulary "$Alpha" exists
    And I am authenticated as "ga-viewer"
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

  @D3
  Scenario: A version PATCH never changes the vocabulary status
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha/versions/1" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And vocabulary "$Alpha" has vocabulary status "ACTIVE"
