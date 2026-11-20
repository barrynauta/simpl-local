Feature: Create a new version of a vocabulary
  POST /vocabularies/{vocabularyName}/versions assigns the next number (D7), stores the version as
  ACTIVE and deprecates every older ACTIVE version in the same transaction (D4).

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @AC_SIMPL-18187_AC2 @D4 @D7 @D26 @D10 @API-12
  Scenario: A new version gets the next number, is ACTIVE and deprecates the previous version
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the description "Second description."
    Then the response status is 201
    And the response header "Location" is "/vocabularies/${Health}/versions/2"
    And the response field "href" is "/vocabularies/${Health}/versions/2"
    And the response field "contentHref" matches "^https?://.+/content/${Health}/versions/2$"
    And the response field "name" is "${Health}"
    And the response field "version" is 2
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Second description."
    And the response field "changelog" is "Changed content."
    And the response field "createdBy" is "${adminEmail}"
    And there are no validation warnings
    And version 1 of "Health" has the status "DEPRECATED"
    And version 2 of "Health" has the status "ACTIVE"
    And the vocabulary "Health" has 2 versions

  @AC_SIMPL-18173_AC1 @D4
  Scenario: All previous versions are DEPRECATED after a third version
    Given a vocabulary "Health" exists with 3 versions
    Then versions 1 to 2 of "Health" have the status "DEPRECATED"
    And version 3 of "Health" has the status "ACTIVE"

  @D4
  Scenario: Deprecated versions get the creation time of the new version as their last update date
    Given a vocabulary "Health" exists
    When I create a new version of "Health"
    Then the response status is 201
    And version 1 of "Health" has a last update date equal to the creation date of version 2

  @D4 @D25
  Scenario: The vocabulary last update date and latest version follow the new version
    Given a vocabulary "Health" exists
    And I remember version 1 of "Health" as "v1"
    And the clock moves to the next second
    And a new version of "Health" was created
    And I remember version 2 of "Health" as "v2"
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response field "version" is 2
    And the response field "lastUpdateDate" equals "creationDate" of the remembered "v2"
    And the response field "creationDate" equals "creationDate" of the remembered "v1"
    And the response field "createdBy" is "${adminEmail}"

  @D28 @D25
  Scenario: The vocabulary shows the description of its latest version, older versions keep theirs
    Given a vocabulary "Health" exists with the description "First text."
    When I create a new version of "Health" with the description "Second text."
    Then the response status is 201
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response field "description" is "Second text."
    When I send a "GET" request to "/vocabularies/${Health}/versions/1"
    Then the response field "description" is "First text."

  @D4 @D3
  Scenario: After a manual reactivation two versions are ACTIVE and the next version deprecates both
    Given a vocabulary "Health" exists with 2 versions
    And version 1 of "Health" was set to "ACTIVE"
    Then version 1 of "Health" has the status "ACTIVE"
    And version 2 of "Health" has the status "ACTIVE"
    When I create a new version of "Health"
    Then the response field "version" is 3
    And versions 1 to 2 of "Health" have the status "DEPRECATED"
    And version 3 of "Health" has the status "ACTIVE"
    And version 1 of "Health" has a last update date equal to the creation date of version 3
    And version 2 of "Health" has a last update date equal to the creation date of version 3

  @D3
  Scenario: A new version may be created for a DEPRECATED vocabulary and the vocabulary stays DEPRECATED
    Given a vocabulary "Health" exists
    And the vocabulary "Health" was set to "DEPRECATED"
    When I create a new version of "Health"
    Then the response status is 201
    And the response field "status" is "ACTIVE"
    And the vocabulary "Health" has the status "DEPRECATED"

  @D7 @D4
  Scenario: Version numbers continue from the highest number, also when the latest version is DEPRECATED
    Given a vocabulary "Health" exists with 2 versions
    And version 2 of "Health" was set to "DEPRECATED"
    When I create a new version of "Health"
    Then the response field "version" is 3

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative @OWA-API3
  Scenario: A new version request that carries a version field is rejected and nothing is created
    Given a vocabulary "Health" exists
    When I send a multipart POST to "/vocabularies/${Health}/versions" with
      | part           | value            |
      | description    | Manual number    |
      | changelog      | Manual number    |
      | vocabularyFile | @valid           |
      | version        | 7                |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "version"
    And the vocabulary "Health" has 1 versions
    And version 1 of "Health" has the status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A new version request with an invalid <part> is rejected and nothing changes
    Given a vocabulary "Health" exists
    When I send a multipart POST to "/vocabularies/${Health}/versions" with
      | part           | value         |
      | description    | <description> |
      | changelog      | <changelog>   |
      | vocabularyFile | <file>        |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And the vocabulary "Health" has 1 versions
    And version 1 of "Health" has the status "ACTIVE"

    Examples:
      | part           | description      | changelog        | file   |
      | changelog      | Description      | @omit            | @valid |
      | changelog      | Description      | ${repeat:c:1001} | @valid |
      | changelog      | Description      | ${repeat: :3}    | @valid |
      | changelog      | Description      |                  | @valid |
      | description    | @omit            | Changed          | @valid |
      | description    | ${repeat:d:1001} | Changed          | @valid |
      | description    | ${repeat: :3}    | Changed          | @valid |
      | vocabularyFile | Description      | Changed          | @omit  |

  @D37 @Q2 @NFR-04
  Scenario: A changelog of 1000 characters is accepted
    Given a vocabulary "Health" exists
    When I send a multipart POST to "/vocabularies/${Health}/versions" with
      | part           | value            |
      | description    | Description      |
      | changelog      | ${repeat:c:1000} |
      | vocabularyFile | @valid           |
    Then the response status is 201

  @AC_SIMPL-18173_AC3 @D24 @D51 @D66 @negative
  Scenario: A new version for a vocabulary that does not exist is rejected with 404
    When I create a new version of "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And no vocabulary named "Ghost" is stored

  @D51 @negative
  Scenario: A malformed request to an unknown name gives 400, request shape comes before 404
    When I send a multipart POST to "/vocabularies/${Ghost}/versions" with
      | part           | value       |
      | description    | Description |
      | vocabularyFile | @valid      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38
  Scenario: A new version request that is not multipart/form-data is rejected with 415
    Given a vocabulary "Health" exists
    When I send a "POST" request to "/vocabularies/${Health}/versions" with content type "application/json" and body
      """
      {"description": "d", "changelog": "c"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Health" has 1 versions

  @D46 @D8 @negative
  Scenario: Resubmitting the latest version unchanged is refused with 409 and deprecates nothing
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the content and description of version 1 and the changelog "Another changelog"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '${Health}' already has this content and description."
    And the vocabulary "Health" has 1 versions
    And version 1 of "Health" has the status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the content of version 1 and its description padded with spaces
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the vocabulary "Health" has 1 versions

  @D46
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "Health" exists with 2 versions
    When I create a new version of "Health" with the content and description of version 1 and the changelog "Back to 1"
    Then the response status is 201
    And the response field "version" is 3

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given a vocabulary "Health" exists
    When I create a new version of "Health" with the content of version 1 and the description "Only the description changed."
    Then the response status is 201
    And the response field "version" is 2
    And version 1 of "Health" has the status "DEPRECATED"

  @D46 @D51 @negative
  Scenario: The field checks run before the identical-version check
    Given a vocabulary "Health" exists
    When I send a multipart POST to "/vocabularies/${Health}/versions" with
      | part           | value                     |
      | description    | @description-of:Health:1  |
      | vocabularyFile | @content-of:Health:1      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"
    And the vocabulary "Health" has 1 versions

  @D52 @negative @BLG-req-valid
  Scenario: An unknown form field on a new version is rejected
    Given a vocabulary "Health" exists
    When I send a multipart POST to "/vocabularies/${Health}/versions" with
      | part           | value       |
      | description    | Description |
      | changelog      | Changed     |
      | vocabularyFile | @valid      |
      | status         | DEPRECATED  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "status"
    And the vocabulary "Health" has 1 versions

  @D49 @negative
  Scenario: A new version for an invalid path name is rejected with 400
    When I send a multipart POST to "/vocabularies/lowercase${run}/versions" with
      | part           | value       |
      | description    | Description |
      | changelog      | Changed     |
      | vocabularyFile | @valid      |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And the issue in "path" named "vocabularyName" has the type "urn:problem-type:belgif:input-validation:schemaViolation"

  @D43 @D82 @D92
  Scenario: Five overlapping new versions all succeed and get distinct numbers
    Given a vocabulary "Busy" exists
    When I send 5 new versions of "Busy" in parallel
    Then the parallel responses have the statuses "201,201,201,201,201"
    And the parallel responses have distinct Location headers for versions 2 to 6
    And the vocabulary "Busy" has 6 versions
    And version 6 of "Busy" has the status "ACTIVE"
    And versions 1 to 5 of "Busy" have the status "DEPRECATED"
