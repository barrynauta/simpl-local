Feature: Change the vocabulary status and the version status via the API
  The vocabulary status is changed only by PATCH /vocabularies/{vocabularyName}; it never
  changes a version status (D3). PATCH .../versions/{version} sets only that version's
  status (D4). A request for the current status is a conflict (D4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3 @D25
  Scenario: A vocabulary is deprecated and the versions keep their status
    Given the vocabulary "Health" exists with 2 versions
    And I remember the field "lastUpdateDate" of "/vocabularies/{{name:Health}}" as "before"
    And a second has passed
    When I set the status of "Health" to "DEPRECATED"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "name" is "{{name:Health}}"
    And the response field "status" is "DEPRECATED"
    And the response field "version" is 2
    And the response field "href" is "/vocabularies/{{name:Health}}"
    And the response field "lastUpdateDate" is later than "before"
    And the vocabulary "Health" has the status "DEPRECATED"
    And the version statuses of "Health" are "1:DEPRECATED, 2:ACTIVE"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3
  Scenario: A deprecated vocabulary is set back to ACTIVE
    Given the vocabulary "Health" exists
    And the vocabulary "Health" is deprecated
    When I set the status of "Health" to "ACTIVE"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And the vocabulary "Health" has the status "ACTIVE"
    And version 1 of "Health" has the status "ACTIVE"

  @D4 @D48 @negative
  Scenario Outline: Asking for the current vocabulary status is a conflict
    Given the vocabulary "Health" exists
    And <setup>
    When I set the status of "Health" to "<status>"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"
    And the vocabulary "Health" has the status "<status>"

    Examples:
      | setup                                   | status     |
      | the vocabulary "Health" has a new version | ACTIVE     |
      | the vocabulary "Health" is deprecated   | DEPRECATED |

  @AC_SIMPL-18175_AC2 @D24 @negative
  Scenario: A status change for a vocabulary that does not exist gives 404
    When I set the status of "Ghost" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18175_AC3 @D4 @D24 @negative
  Scenario Outline: An unsupported status value is rejected and the status stays the same
    Given the vocabulary "Health" exists
    When I send "PATCH" to "/vocabularies/{{name:Health}}" with the JSON body:
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the vocabulary "Health" has the status "ACTIVE"

    Examples:
      | body                     |
      | {"status": "RETIRED"}    |
      | {"status": "deprecated"} |
      | {"status": ""}           |
      | {"status": null}         |
      | {}                       |

  @D52 @OWA-API3 @negative
  Scenario: A status change with an unknown property is rejected
    Given the vocabulary "Health" exists
    When I send "PATCH" to "/vocabularies/{{name:Health}}" with the JSON body:
      """
      {"status": "DEPRECATED", "name": "Other"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And the vocabulary "Health" has the status "ACTIVE"

  @D52 @negative
  Scenario: A status change with a query parameter is rejected
    Given the vocabulary "Health" exists
    When I send "PATCH" to "/vocabularies/{{name:Health}}?force=true" with the JSON body:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "force"
    And the vocabulary "Health" has the status "ACTIVE"

  @D15 @D48 @API-38 @negative
  Scenario: A status change that is not JSON is rejected with 415
    Given the vocabulary "Health" exists
    When I send "PATCH" to "/vocabularies/{{name:Health}}" with the body "status=DEPRECATED" as "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Health" has the status "ACTIVE"

  @D51 @negative
  Scenario: A malformed status change for an unknown vocabulary gives 400, not 404
    When I send "PATCH" to "/vocabularies/{{name:Ghost}}" with the JSON body:
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D49 @negative
  Scenario: A status change for an invalid path name is rejected with 400
    When I send "PATCH" to "/vocabularies/lower{{run}}" with the JSON body:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And the problem has an issue in "path" named "vocabularyName" with type "urn:problem-type:belgif:input-validation:schemaViolation"

  @D54 @negative
  Scenario: A status change with the name in another case gives 404
    Given the vocabulary "Health" exists
    When I set the status of "raw:HEALTH{{runupper}}" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the vocabulary "Health" has the status "ACTIVE"

  @D43 @negative
  Scenario: Two identical overlapping status changes give 200 and then 409
    Given the vocabulary "Health" exists
    When two requests set the status of "Health" to "DEPRECATED" at the same time
    Then the parallel responses are one 200 and one 409
    And the vocabulary "Health" has the status "DEPRECATED"

  # ---- version status -----------------------------------------------------------------

  @D4 @D41 @D3
  Scenario: A version is deprecated by hand and the vocabulary status stays the same
    Given the vocabulary "Health" exists
    And I remember the field "lastUpdateDate" of "/vocabularies/{{name:Health}}" as "before"
    And a second has passed
    When I set the status of version 1 of "Health" to "DEPRECATED"
    Then the response status is 200
    And the response field "version" is 1
    And the response field "status" is "DEPRECATED"
    And the response field "href" is "/vocabularies/{{name:Health}}/versions/1"
    And the response field "lastUpdateDate" is later than "before"
    And the vocabulary "Health" has the status "ACTIVE"
    And version 1 of "Health" has the status "DEPRECATED"

  @D25
  Scenario: A version status change updates the last update date of the vocabulary
    Given the vocabulary "Health" exists
    And I remember the field "lastUpdateDate" of "/vocabularies/{{name:Health}}" as "before"
    And a second has passed
    When I set the status of version 1 of "Health" to "DEPRECATED"
    Then the response status is 200
    When I send "GET" to "/vocabularies/{{name:Health}}"
    Then the response field "lastUpdateDate" is later than "before"

  @D4 @D41
  Scenario: Reactivating an older version gives two ACTIVE versions
    Given the vocabulary "Health" exists with 2 versions
    When I set the status of version 1 of "Health" to "ACTIVE"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And the version statuses of "Health" are "1:ACTIVE, 2:ACTIVE"

  @D4 @D41 @D48 @negative
  Scenario: Asking for the current version status is a conflict
    Given the vocabulary "Health" exists with 2 versions
    When I set the status of version 1 of "Health" to "DEPRECATED"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"
    And the version statuses of "Health" are "1:DEPRECATED, 2:ACTIVE"

  @D41 @D55 @negative
  Scenario: A status change for a version that does not exist gives 404
    Given the vocabulary "Health" exists
    When I set the status of version 9 of "Health" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 9 of vocabulary '{{name:Health}}' was not found."

  @D41 @negative
  Scenario: A version status change for a vocabulary that does not exist gives 404
    When I set the status of version 1 of "Ghost" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D4 @negative
  Scenario: An unsupported version status value is rejected
    Given the vocabulary "Health" exists
    When I set the status of version 1 of "Health" to "ARCHIVED"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of "Health" has the status "ACTIVE"

  @D49 @negative
  Scenario Outline: A version status change for the invalid version <version> is rejected with 400
    Given the vocabulary "Health" exists
    When I send "PATCH" to "/vocabularies/{{name:Health}}/versions/<version>" with the JSON body:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "version" with type "urn:problem-type:belgif:input-validation:schemaViolation"
    And version 1 of "Health" has the status "ACTIVE"

    Examples:
      | version |
      | 0       |
      | -1      |
      | one     |
      | 1.5     |

  @D3 @D19
  Scenario: The status filter of the list shows the vocabulary status
    Given the vocabularies "Kept, Dropped" exist
    And the vocabulary "Dropped" is deprecated
    When I list the vocabularies with "name={{run}}&status=DEPRECATED"
    Then the response status is 200
    And the item names are "Dropped"
    When I list the vocabularies with "name={{run}}&status=ACTIVE"
    Then the item names are "Kept"
