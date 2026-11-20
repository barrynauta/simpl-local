@status
Feature: Change the vocabulary status and the version status through the API
  PATCH /vocabularies/{vocabularyName} sets only the vocabulary status; PATCH .../versions/{version}
  sets only that version's status (D3, D4). Asking for the current status gives 409.

  Background:
    Given I use the token of "ga-admin"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3 @D25
  Scenario: Deprecate a vocabulary and activate it again; its versions are unchanged
    Given a vocabulary "A" exists
    And I remember the field "lastUpdateDate" of "/vocabularies/${A}" as "before"
    And I wait 1.1 seconds
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    And the media type is "application/json"
    And the response field "name" is "${A}"
    And the response field "href" is "/vocabularies/${A}"
    And the response field "status" is "DEPRECATED"
    And the response field "version" equals 1
    And the response field "description" is "Description of A"
    And the response field "lastUpdateDate" is later than the remembered "before"
    And vocabulary "A" has the status "DEPRECATED"
    And version 1 of vocabulary "A" has the status "ACTIVE"
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    And the response field "status" is "ACTIVE"
    And vocabulary "A" has the status "ACTIVE"
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @D3
  Scenario: Deprecating a vocabulary with several versions changes no version status
    Given a vocabulary "A" exists with 2 versions
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And version 2 of vocabulary "A" has the status "ACTIVE"

  @D4 @API-46 @API-47 @negative
  Scenario Outline: Asking for the current vocabulary status <status> gives 409
    Given a vocabulary "A" exists
    And vocabulary "A" has the status "<status>"
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "<status>"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"
    And vocabulary "A" has the status "<status>"

    Examples:
      | status     |
      | ACTIVE     |
      | DEPRECATED |

  @AC_SIMPL-18175_AC2 @D24 @D66 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I send "PATCH" to "/vocabularies/${Missing}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "vocabularyName"
    And the vocabulary list filtered by name "${Missing}" has 0 items

  @AC_SIMPL-18175_AC3 @D4 @D24 @negative
  Scenario Outline: An unsupported status value <body> is rejected and the status is unchanged
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And vocabulary "A" has the status "ACTIVE"

    Examples:
      | body                     |
      | {"status": "ARCHIVED"}   |
      | {"status": "active"}     |
      | {"status": "Deprecated"} |
      | {"status": ""}           |
      | {"status": null}         |
      | {"status": 1}            |
      | {"status": ["ACTIVE"]}   |
      | {}                       |
      | []                       |
      | not json                 |

  @AC_SIMPL-18175_AC3 @D52 @OWA-API3 @BLG-req-valid @negative
  Scenario: An unknown JSON property is rejected and the status is unchanged
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body
      """
      {"status": "DEPRECATED", "version": 7}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "body" named "version"
    And vocabulary "A" has the status "ACTIVE"

  @D51 @negative
  Scenario: An invalid status for an unknown vocabulary gives 400, because the request shape comes before 404
    When I send "PATCH" to "/vocabularies/${Missing}" with the JSON body
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D15 @API-38 @negative
  Scenario: A status change that is not JSON gives 415
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}" with content type "text/plain" and the body
      """
      DEPRECATED
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "A" has the status "ACTIVE"

  @D110 @NFR-04 @Q2 @negative
  Scenario: A JSON body over 1 MB gives 413
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}" with a JSON body of 1100000 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And vocabulary "A" has the status "ACTIVE"

  @D110 @NFR-04 @Q2
  Scenario: A JSON body of exactly 1 MB is at the limit and is accepted
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}" with the JSON body padded with spaces to 1048576 bytes
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    And vocabulary "A" has the status "DEPRECATED"

  @D52 @negative
  Scenario: A query parameter on PATCH is rejected
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}?status=DEPRECATED" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "status"
    And vocabulary "A" has the status "ACTIVE"

  @D49 @negative
  Scenario Outline: A vocabulary name that breaks the pattern in the PATCH path gives 400: <name>
    When I send "PATCH" to "/vocabularies/<name>" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "vocabularyName"
    And every issue has the type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | name       |
      | lowercase  |
      | Ab         |
      | ${long:65} |
      | A_b_c      |

  @D54 @negative
  Scenario: Paths match the stored name exactly
    Given a vocabulary "Case" exists
    When I send "PATCH" to "/vocabularies/${upper:Case}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And vocabulary "Case" has the status "ACTIVE"

  @D43 @D4
  Scenario: Two identical overlapping status changes give 200 and then 409
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}" twice in parallel with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the parallel responses have the statuses "200, 409"
    And vocabulary "A" has the status "DEPRECATED"

  # ---------------------------------------------------------------- version status

  @D4 @D41
  Scenario: Deprecate a single version; the vocabulary status and other versions are unchanged
    Given a vocabulary "A" exists with 2 versions
    When I send "PATCH" to "/vocabularies/${A}/versions/2" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    And the response field "href" is "/vocabularies/${A}/versions/2"
    And the response field "version" equals 2
    And the response field "status" is "DEPRECATED"
    And the response field "lastUpdateDate" is a UTC timestamp
    And vocabulary "A" has the status "ACTIVE"
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And version 2 of vocabulary "A" has the status "DEPRECATED"

  @D4 @D41
  Scenario: Reactivating an older version leaves two ACTIVE versions
    Given a vocabulary "A" exists with 2 versions
    When I send "PATCH" to "/vocabularies/${A}/versions/1" with the JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    And the response field "status" is "ACTIVE"
    And version 1 of vocabulary "A" has the status "ACTIVE"
    And version 2 of vocabulary "A" has the status "ACTIVE"

  @D25 @D4
  Scenario: A version status change updates the vocabulary's last update date
    Given a vocabulary "A" exists with 2 versions
    And I remember the field "lastUpdateDate" of "/vocabularies/${A}" as "before"
    And I wait 1.1 seconds
    When I send "PATCH" to "/vocabularies/${A}/versions/1" with the JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    When I send "GET" to "/vocabularies/${A}"
    Then the response field "lastUpdateDate" is later than the remembered "before"

  @D4 @API-46 @negative
  Scenario: Asking for the current version status gives 409
    Given a vocabulary "A" exists with 2 versions
    When I send "PATCH" to "/vocabularies/${A}/versions/1" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"
    When I send "PATCH" to "/vocabularies/${A}/versions/2" with the JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status ACTIVE"

  @D41 @D55 @negative
  Scenario: A version status change for an unknown version gives 404
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}/versions/5" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 5 of vocabulary '${A}' was not found."
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @D41 @D66 @negative
  Scenario: A version status change for an unknown vocabulary gives 404
    When I send "PATCH" to "/vocabularies/${Missing}/versions/1" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."

  @D4 @negative
  Scenario Outline: An unsupported version status value <body> is rejected
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}/versions/1" with the JSON body
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of vocabulary "A" has the status "ACTIVE"

    Examples:
      | body                   |
      | {"status": "RETIRED"}  |
      | {"status": null}       |
      | {}                     |

  @D49 @D91 @D112 @negative
  Scenario Outline: A version in the PATCH path that is not a positive 32-bit integer gives 400: <version>
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}/versions/<version>" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "version"
    And version 1 of vocabulary "A" has the status "ACTIVE"

    Examples:
      | version    |
      | 0          |
      | -1         |
      | abc        |
      | 1.0        |
      | 1e1        |
      | %2B1       |
      | %201       |
      | 2147483648 |

  @D91 @D24
  Scenario: The largest 32-bit version number is a valid path value and gives 404
    Given a vocabulary "A" exists
    When I send "PATCH" to "/vocabularies/${A}/versions/2147483647" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
