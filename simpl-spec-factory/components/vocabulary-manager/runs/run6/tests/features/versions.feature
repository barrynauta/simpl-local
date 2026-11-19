Feature: Create a new version of a vocabulary via the API
  POST /vocabularies/{vocabularyName}/versions assigns the next version number (D7), makes the
  new version ACTIVE and deprecates every older ACTIVE version in the same request (D4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18191_AC1 @AC_SIMPL-18189_AC1 @D4 @D7 @D26 @D27 @API-12
  Scenario: A new version gets the next number, is ACTIVE and deprecates the older version
    Given the vocabulary "Health" exists
    When I create a new version of "Health"
    Then the response status is 201
    And the response header "Location" is "/vocabularies/{{name:Health}}/versions/2"
    And the response field "href" is "/vocabularies/{{name:Health}}/versions/2"
    And the response field "contentHref" is "{{base}}/content/{{name:Health}}/versions/2"
    And the response field "name" is "{{name:Health}}"
    And the response field "version" is 2
    And the response field "status" is "ACTIVE"
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the response has no validation warnings
    And the version statuses of "Health" are "1:DEPRECATED, 2:ACTIVE"

  @D4 @D25 @D28 @D5
  Scenario: The vocabulary shows the latest version after a new version
    Given the vocabulary "Health" exists
    And I remember the field "creationDate" of "/vocabularies/{{name:Health}}" as "v1created"
    And a second has passed
    When I create a new version of "Health" with:
      | field       | value                      |
      | description | Second description {{run}} |
    Then the response status is 201
    And I remember the response field "creationDate" as "v2created"
    And the field "version" of "/vocabularies/{{name:Health}}" is 2
    And the field "description" of "/vocabularies/{{name:Health}}" is "Second description {{run}}"
    And the field "status" of "/vocabularies/{{name:Health}}" is "ACTIVE"
    And the field "lastUpdateDate" of "/vocabularies/{{name:Health}}" is "{{var:v2created}}"
    And the field "lastUpdateDate" of "/vocabularies/{{name:Health}}/versions/1" is "{{var:v2created}}"
    And the field "creationDate" of "/vocabularies/{{name:Health}}" is "{{var:v1created}}"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18191_AC1 @D4
  Scenario: A third version deprecates the second and leaves the first deprecated
    Given the vocabulary "Health" exists with 2 versions
    When I create a new version of "Health"
    Then the response status is 201
    And the response field "version" is 3
    And the version statuses of "Health" are "1:DEPRECATED, 2:DEPRECATED, 3:ACTIVE"

  @D4
  Scenario: The next version deprecates every ACTIVE version after a reactivation
    Given the vocabulary "Health" exists with 2 versions
    And version 1 of "Health" is set to "ACTIVE"
    When I create a new version of "Health"
    Then the response status is 201
    And the version statuses of "Health" are "1:DEPRECATED, 2:DEPRECATED, 3:ACTIVE"

  @D3
  Scenario: A new version of a DEPRECATED vocabulary keeps the vocabulary DEPRECATED
    Given the vocabulary "Health" exists
    And the vocabulary "Health" is deprecated
    When I create a new version of "Health"
    Then the response status is 201
    And the response field "status" is "ACTIVE"
    And the vocabulary "Health" has the status "DEPRECATED"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario: A request that carries a version number is rejected
    Given the vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field   | value |
      | version | 2     |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "version"
    And the vocabulary "Health" has 1 version
    And version 1 of "Health" has the status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D8 @D37 @negative
  Scenario Outline: A new version without <field> is rejected
    Given the vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field   | value     |
      | <field> | (omitted) |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And the vocabulary "Health" has 1 version

    Examples:
      | field          |
      | description    |
      | changelog      |
      | vocabularyFile |

  @AC_SIMPL-18173_AC2 @D37 @NFR-04 @negative
  Scenario Outline: A new version with an invalid <field> is rejected
    Given the vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And the vocabulary "Health" has 1 version

    Examples:
      | field       | value      |
      | description | (spaces)   |
      | description | chars:1001 |
      | changelog   | (empty)    |
      | changelog   | chars:1001 |

  @D52 @negative
  Scenario: A new version with an unknown form field is rejected
    Given the vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field | value |
      | name  | Other |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And the vocabulary "Health" has 1 version

  @AC_SIMPL-18173_AC3 @D24 @negative
  Scenario: A new version for a vocabulary that does not exist is rejected with 404
    When I create a new version of "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary named "Ghost" is stored

  @D54 @negative
  Scenario: A path name in another case does not match the stored vocabulary
    Given the vocabulary "Health" exists
    When I create a new version of "raw:HEALTH{{runupper}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the vocabulary "Health" has 1 version

  @D51 @negative
  Scenario: A malformed request to an unknown vocabulary gives 400, not 404
    When I create a new version of "Ghost" with:
      | field       | value     |
      | description | (omitted) |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D51 @negative
  Scenario: Invalid content for an unknown vocabulary gives 404, not 400
    When I create a new version of "Ghost" from the file "syntax_error_line3.ttl"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D49 @negative
  Scenario: A new version for an invalid path name is rejected with 400
    When I create a new version of "raw:lower{{run}}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "vocabularyName" with type "urn:problem-type:belgif:input-validation:schemaViolation"

  @AC_SIMPL-18173_AC6 @D15 @D48 @API-38 @negative
  Scenario: A new version that is not multipart is rejected with 415
    Given the vocabulary "Health" exists
    When I create a new version of "Health" as JSON
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the vocabulary "Health" has 1 version

  @D15 @NFR-04 @negative
  Scenario: A new version above 5 MB is rejected with 413
    Given the vocabulary "Health" exists
    When I create a new version of "Health" with:
      | field          | value          |
      | vocabularyFile | turtle:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the vocabulary "Health" has 1 version

  @D46 @D8 @D48 @negative
  Scenario: Resubmitting the latest version unchanged is refused and deprecates nothing
    Given the vocabulary "Health" exists
    When I resubmit the latest version of "Health" unchanged with a new change log
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '{{name:Health}}' already has this content and description."
    And the vocabulary "Health" has 1 version
    And version 1 of "Health" has the status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given the vocabulary "Health" exists with 2 versions
    When I resubmit the latest version of "Health" with the description padded with spaces
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 2 of vocabulary '{{name:Health}}' already has this content and description."
    And the vocabulary "Health" has 2 versions

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given the vocabulary "Health" exists
    When I resubmit the content of the latest version of "Health" with a new description
    Then the response status is 201
    And the response field "version" is 2

  @D46
  Scenario: New content with the same description creates a new version
    Given the vocabulary "Health" exists
    When I submit new content for "Health" with the description of the latest version
    Then the response status is 201
    And the response field "version" is 2

  @D43
  Scenario: Five overlapping new versions are serialised
    Given the vocabulary "Health" exists
    When I create 5 new versions of "Health" in parallel
    Then every parallel response has the status 201
    And the parallel responses got the version numbers 2 to 6 each once
    And the parallel responses have distinct Location headers
    And the vocabulary "Health" has 6 versions
    And the version statuses of "Health" are "1:DEPRECATED, 2:DEPRECATED, 3:DEPRECATED, 4:DEPRECATED, 5:DEPRECATED, 6:ACTIVE"

  @AC_SIMPL-18190_AC1 @D7
  Scenario: The version numbers count up from 1 without gaps
    Given the vocabulary "Health" exists with 3 versions
    When I list the versions of "Health" with ""
    Then the response status is 200
    And the version numbers are "3, 2, 1"
