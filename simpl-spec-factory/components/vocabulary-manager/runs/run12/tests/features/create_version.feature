Feature: Create a new version of a vocabulary
  POST /vocabularies/{vocabularyName}/versions (SIMPL-18173, SIMPL-18187, SIMPL-18190).
  The new version is ACTIVE and every older ACTIVE version becomes DEPRECATED in the same transaction (D4).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @AC_SIMPL-18194_AC2 @D4 @D7 @D10 @D26 @D27 @D5 @API-12
  Scenario: A new version gets the next number, is ACTIVE, and deprecates the previous version
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}" with description "Second edition"
    Then the status is 201
    And the response header "Location" equals "/vocabularies/{{Health}}/versions/2"
    And the response field "href" equals "/vocabularies/{{Health}}/versions/2"
    And the response field "contentHref" is the contentBase followed by "/content/{{Health}}/versions/2"
    And the response field "name" equals "{{Health}}"
    And the response field "version" equals 2
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Second edition"
    And the response field "changelog" equals "Changed for a test."
    And the response field "createdBy" equals "{{adminEmail}}"
    And the response field "latestVersionHref" equals "/vocabularies/{{Health}}/versions/2"
    And there are no validation warnings
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=ACTIVE"
    And vocabulary "{{Health}}" has latest version 2

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @D4 @D7
  Scenario: Every previous version is DEPRECATED after several new versions
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 2 more versions
    When I create a new version of "{{Health}}"
    Then the status is 201
    And the response field "version" equals 4
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=DEPRECATED, 3=DEPRECATED, 4=ACTIVE"

  @D4 @D58
  Scenario: The deprecated versions get the creation time of the new version as last update date
    Given a vocabulary "{{Health}}" exists
    And one second passes
    When I create a new version of "{{Health}}"
    And I remember the response field "creationDate" as "v2created"
    And I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the response field "status" equals "DEPRECATED"
    And the response field "lastUpdateDate" equals "{{v2created}}"
    When I send "GET" to "/vocabularies/{{Health}}"
    Then the response field "lastUpdateDate" is not earlier than "{{v2created}}"

  @D25 @D28
  Scenario: The vocabulary shows the description of its latest version and the creator of version 1
    Given a vocabulary "{{Health}}" exists with description "First description"
    And vocabulary "{{Health}}" has a new version with description "Second description"
    When I send "GET" to "/vocabularies/{{Health}}"
    Then the response field "description" equals "Second description"
    And the response field "version" equals 2
    And the response field "createdBy" equals "{{adminEmail}}"
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the response field "description" equals "First description"

  @D4
  Scenario: After a manual reactivation the next new version deprecates every ACTIVE version
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"
    When I create a new version of "{{Health}}"
    Then the status is 201
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=DEPRECATED, 3=ACTIVE"

  @D3
  Scenario: A new version may be created for a DEPRECATED vocabulary and the vocabulary stays DEPRECATED
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has status "DEPRECATED"
    When I create a new version of "{{Health}}"
    Then the status is 201
    And the response field "status" equals "ACTIVE"
    And vocabulary "{{Health}}" has status "DEPRECATED"
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=ACTIVE"

  @D3
  Scenario: Creating a version never changes an ACTIVE vocabulary status
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{Health}}"
    Then the status is 201
    And vocabulary "{{Health}}" has status "ACTIVE"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario: A version number in the request is rejected and no version is created
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field          | value             |
      | description    | Manual numbering  |
      | changelog      | Tried version 5.  |
      | version        | 5                 |
      | vocabularyFile | file:valid.ttl    |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "version"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A new version without <field> is rejected and nothing changes
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field    | value    |
      | <keep1>  | <value1> |
      | <keep2>  | <value2> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

    Examples:
      | field          | keep1       | value1        | keep2          | value2         |
      | description    | changelog   | Changed.      | vocabularyFile | file:valid.ttl |
      | changelog      | description | New text      | vocabularyFile | file:valid.ttl |
      | vocabularyFile | description | New text      | changelog      | Changed.       |

  @AC_SIMPL-18173_AC2 @D37 @negative @NFR-04 @Q2
  Scenario: A changelog of 1001 characters is rejected
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field          | value          |
      | description    | New text       |
      | changelog      | repeat:1001:c  |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "changelog"
    And vocabulary "{{Health}}" has 1 version

  @D52 @OWA-API3 @negative
  Scenario: A name field on a new version is an unknown field
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field          | value          |
      | name           | {{Other}}      |
      | description    | New text       |
      | changelog      | Changed.       |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "name"
    And vocabulary "{{Health}}" has 1 version

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative @API-12
  Scenario: A new version for a vocabulary that does not exist is rejected with 404
    When I create a new version of "{{Missing}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And no vocabulary named "{{Missing}}" is stored

  @D54 @negative
  Scenario: The path name must match the stored name exactly
    Given a vocabulary "{{Health}}" exists
    When I create a new version of "{{upper:Health}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And vocabulary "{{Health}}" has 1 version

  @D49 @negative
  Scenario: An invalid name in the path gives 400 for the path
    When I send "POST" to "/vocabularies/bad-name/versions" with a multipart body
      | field          | value          |
      | description    | New text       |
      | changelog      | Changed.       |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And issue 0 has type "urn:problem-type:belgif:input-validation:schemaViolation"

  @D51 @negative
  Scenario: A malformed request to an unknown name gives 400, not 404
    When I send "POST" to "/vocabularies/{{Missing}}/versions" with a multipart body
      | field          | value          |
      | description    | New text       |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38
  Scenario: A new version sent as JSON gets 415
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with body "{}" and content type "application/json"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @D15 @D76 @NFR-04 @Q2 @negative
  Scenario: A new version file one byte over 5 MB is rejected with 413
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field          | value             |
      | description    | Big               |
      | changelog      | Big file.         |
      | vocabularyFile | generated:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @D15 @D76 @D71 @NFR-04 @Q2 @Q1
  Scenario: A new version file of exactly 5 MB is accepted
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field          | value             |
      | description    | Big               |
      | changelog      | Big file.         |
      | vocabularyFile | generated:5242880 |
    Then the status is 201
    And the response field "version" equals 2

  @D46 @D8 @negative
  Scenario: Resubmitting the latest version unchanged is refused with 409 and deprecates nothing
    Given a vocabulary "{{Health}}" exists with description "Same text"
    When I create a new version of "{{Health}}" with the content of version 1 and description "Same text"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '{{Health}}' already has this content and description."
    And vocabulary "{{Health}}" has 1 version
    And version 1 of vocabulary "{{Health}}" has status "ACTIVE"

  @D46
  Scenario: The identical-version check trims the description
    Given a vocabulary "{{Health}}" exists with description "Same text"
    When I create a new version of "{{Health}}" with the content of version 1 and description "  Same text  "
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D46
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "{{Health}}" exists with description "Same text"
    And vocabulary "{{Health}}" has 1 more versions
    When I create a new version of "{{Health}}" with the content of version 1 and description "Same text"
    Then the status is 201
    And the response field "version" equals 3

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given a vocabulary "{{Health}}" exists with description "Same text"
    When I create a new version of "{{Health}}" with the content of version 1 and description "Better text"
    Then the status is 201
    And the response field "version" equals 2
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=ACTIVE"

  @D46 @D51 @negative
  Scenario: The field checks run before the identical-version check
    Given a vocabulary "{{Health}}" exists with description "Same text"
    When I send "POST" to "/vocabularies/{{Health}}/versions" with a multipart body
      | field          | value          |
      | description    | Same text      |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @D43 @D82 @D119
  Scenario: Four overlapping new versions all succeed with distinct numbers
    Given a vocabulary "{{Health}}" exists
    When I send 4 new versions of "{{Health}}" in parallel
    Then all 4 responses have status 201
    And the created versions are 2 to 5, each exactly once
    And the Location headers are all different
    And the versions of vocabulary "{{Health}}" have the statuses "1=DEPRECATED, 2=DEPRECATED, 3=DEPRECATED, 4=DEPRECATED, 5=ACTIVE"

  @D43 @D4
  Scenario: Two overlapping identical status changes give 200 and then 409
    Given a vocabulary "{{Health}}" exists
    When I send 2 identical "PATCH" requests to "/vocabularies/{{Health}}" in parallel with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the responses have the statuses "200, 409"
    And vocabulary "{{Health}}" has status "DEPRECATED"

  @D52 @negative
  Scenario: A query parameter on POST .../versions is rejected
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies/{{Health}}/versions?version=2" with a multipart body
      | field          | value          |
      | description    | New text       |
      | changelog      | Changed.       |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "version"
    And vocabulary "{{Health}}" has 1 version
