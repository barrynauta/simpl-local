Feature: Change the status of a vocabulary or of one version
  PATCH /vocabularies/{vocabularyName} sets the vocabulary status only; PATCH
  .../versions/{version} sets that version's status only (D3, D4). Same status gives 409.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18175_AC1 @D3 @D25 @API-36
  Scenario: A vocabulary is deprecated and the updated vocabulary is returned
    Given a vocabulary "Health" exists with 2 versions
    When I change the status of vocabulary "Health" to "DEPRECATED"
    Then the response status is 200
    And the response content type is "application/json"
    And the response is vocabulary "Health" with latest version 2 and status "DEPRECATED"
    And the vocabulary "Health" has status "DEPRECATED"
    And the versions of "Health" have the statuses "1=DEPRECATED, 2=ACTIVE"

  @AC_SIMPL-18175_AC1 @AC_SIMPL-18170_AC2 @D3
  Scenario: A deprecated vocabulary is restored to ACTIVE
    Given a vocabulary "Health" exists
    And the vocabulary "Health" has status "DEPRECATED"
    When I change the status of vocabulary "Health" to "ACTIVE"
    Then the response status is 200
    And the field "status" is "ACTIVE"
    And the vocabulary "Health" has status "ACTIVE"
    And the versions of "Health" have the statuses "1=ACTIVE"

  @D25
  Scenario: A status change moves the last update date forward
    Given a vocabulary "Health" exists
    When I send GET "/vocabularies/${name:Health}"
    And I remember the field "lastUpdateDate" as "before"
    And I change the status of vocabulary "Health" to "DEPRECATED"
    Then the response status is 200
    And the vocabulary "Health" has a lastUpdateDate not before the saved "before"

  @D4 @negative @API-46 @API-47
  Scenario Outline: Asking for the current vocabulary status gives 409 (<status>)
    Given a vocabulary "Health" exists
    And the vocabulary "Health" has status "<start>"
    When I change the status of vocabulary "Health" to "<status>"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status <status>"

    Examples:
      | start      | status     |
      | DEPRECATED | DEPRECATED |

  @D4 @negative @API-46
  Scenario: Asking for ACTIVE on an ACTIVE vocabulary gives 409
    Given a vocabulary "Health" exists
    When I change the status of vocabulary "Health" to "ACTIVE"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary is already in status ACTIVE"

  @AC_SIMPL-18175_AC2 @D24 @negative
  Scenario: Changing the status of an unknown vocabulary gives 404
    When I change the status of vocabulary "Missing" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail contains "${name:Missing}"

  @AC_SIMPL-18175_AC3 @D4 @negative
  Scenario Outline: An unsupported status value is rejected and nothing changes (<value>)
    Given a vocabulary "Health" exists
    When I send PATCH "/vocabularies/${name:Health}" with the JSON body:
      """
      <body>
      """
    Then the response is a problem with status 400
    And the vocabulary "Health" has status "ACTIVE"

    Examples:
      | value    | body                     |
      | ARCHIVED | {"status": "ARCHIVED"}   |
      | active   | {"status": "active"}     |
      | empty    | {"status": ""}           |
      | null     | {"status": null}         |
      | missing  | {}                       |
      | number   | {"status": 1}            |

  @D15 @negative @API-38 @API-46
  Scenario: A status change that is not JSON gives 415
    Given a vocabulary "Health" exists
    When I send PATCH "/vocabularies/${name:Health}" with the content type "text/plain" and the body:
      """
      DEPRECATED
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Health" has status "ACTIVE"

  @D43 @D4
  Scenario: Two identical overlapping status changes give 200 and 409
    Given a vocabulary "Health" exists
    When I change the status of vocabulary "Health" to "DEPRECATED" 2 times in parallel
    Then the responses have the statuses 200, 409
    And the vocabulary "Health" has status "DEPRECATED"

  # ---------------------------------------------------------------- version status
  @D4 @D3 @D41
  Scenario: One version is deprecated by hand; the vocabulary status stays
    Given a vocabulary "Health" exists
    When I change the status of version 1 of vocabulary "Health" to "DEPRECATED"
    Then the response status is 200
    And the response is version 1 of "Health" with status "DEPRECATED"
    And the vocabulary "Health" has status "ACTIVE"

  @D4 @D41
  Scenario: A version is reactivated and other versions are untouched
    Given a vocabulary "Health" exists with 3 versions
    When I change the status of version 1 of vocabulary "Health" to "ACTIVE"
    Then the response status is 200
    And the versions of "Health" have the statuses "1=ACTIVE, 2=DEPRECATED, 3=ACTIVE"

  @D4 @D41 @negative @API-46
  Scenario: Asking for the current version status gives 409
    Given a vocabulary "Health" exists with 2 versions
    When I change the status of version 1 of vocabulary "Health" to "DEPRECATED"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"

  @D4 @D41 @negative
  Scenario Outline: A version status change on something unknown gives 404 (<case>)
    Given a vocabulary "Health" exists
    When I send PATCH "<path>" with the JSON body:
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | case            | path                                      |
      | unknown version | /vocabularies/${name:Health}/versions/4   |
      | unknown name    | /vocabularies/${name:Missing}/versions/1  |

  @D4 @D41 @negative
  Scenario: An unsupported version status value gives 400
    Given a vocabulary "Health" exists
    When I send PATCH "/vocabularies/${name:Health}/versions/1" with the JSON body:
      """
      {"status": "RETIRED"}
      """
    Then the response is a problem with status 400
    And the versions of "Health" have the statuses "1=ACTIVE"
