@versions @create
Feature: Create a new version of a vocabulary through the API
  POST /vocabularies/{vocabularyName}/versions assigns the next version number, sets the new
  version ACTIVE and every older ACTIVE version DEPRECATED in the same transaction (D4, D7).

  Background:
    Given I use the token of "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @D4 @D5 @D7 @D10 @D26 @D27 @D28
  Scenario: A new version gets the next number, is ACTIVE and deprecates the older version
    Given a vocabulary "A" exists
    When I create a new version of "A" with the description "Second description"
    Then the status is 201
    And the header "Location" is "/vocabularies/${A}/versions/2"
    And the response field "href" is "/vocabularies/${A}/versions/2"
    And the response field "contentHref" is "${base}/content/${A}/versions/2"
    And the response field "name" is "${A}"
    And the response field "version" equals 2
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Second description"
    And the response field "changelog" is "Change 2 of A"
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" is a UTC timestamp
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/2"
    And the response has no validation warnings
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And vocabulary "A" has 2 versions

  @AC_SIMPL-18173_AC1 @D4 @D5 @D25 @D28
  Scenario: After a new version the older version and the vocabulary show the change
    Given a vocabulary "A" exists
    When I create a new version of "A" with the description "Second description"
    And I remember the response field "creationDate" as "v2created"
    And I send "GET" to "/vocabularies/${A}/versions/1"
    Then the response field "status" is "DEPRECATED"
    And the response field "lastUpdateDate" is the remembered "v2created"
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/2"
    And the response field "description" is "Description of A"
    When I send "GET" to "/vocabularies/${A}"
    Then the response field "version" equals 2
    And the response field "description" is "Second description"
    And the response field "status" is "ACTIVE"
    And the response field "lastUpdateDate" is not earlier than the remembered "v2created"
    And the field "creationDate" of "/vocabularies/${A}" equals the field "creationDate" of "/vocabularies/${A}/versions/1"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @D4 @D7
  Scenario: A third version deprecates every older version
    Given a vocabulary "A" exists with 2 versions
    When I create a new version of "A"
    Then the status is 201
    And the response field "version" equals 3
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And version 2 of vocabulary "A" has the status "DEPRECATED"
    And version 3 of vocabulary "A" has the status "ACTIVE"

  @D4
  Scenario: After a manual reactivation two versions are ACTIVE and the next version deprecates both
    Given a vocabulary "A" exists with 2 versions
    And version 1 of vocabulary "A" has the status "ACTIVE"
    Then version 1 of vocabulary "A" has the status "ACTIVE"
    And version 2 of vocabulary "A" has the status "ACTIVE"
    When I create a new version of "A"
    Then the status is 201
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And version 2 of vocabulary "A" has the status "DEPRECATED"
    And version 3 of vocabulary "A" has the status "ACTIVE"

  @D3
  Scenario: A new version of a DEPRECATED vocabulary is created and the vocabulary stays DEPRECATED
    Given a vocabulary "A" exists
    And vocabulary "A" has the status "DEPRECATED"
    When I create a new version of "A"
    Then the status is 201
    And the response field "status" is "ACTIVE"
    And vocabulary "A" has the status "DEPRECATED"
    And vocabulary "A" has the latest version 2

  @D3
  Scenario: Creating a version does not change the vocabulary status ACTIVE
    Given a vocabulary "A" exists
    When I create a new version of "A"
    Then the status is 201
    And vocabulary "A" has the status "ACTIVE"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario: A request that carries a version field is rejected and creates nothing
    Given a vocabulary "A" exists
    When I send a multipart "POST" to "/vocabularies/${A}/versions" with these parts
      | name           | value         |
      | vocabularyFile | @file:valid:A |
      | description    | Desc          |
      | changelog      | Change        |
      | version        | 2             |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "version"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative
  Scenario: A new version of a vocabulary that does not exist is rejected with 404
    When I create a new version of "Missing"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "vocabularyName"
    And the vocabulary list filtered by name "${Missing}" has 0 items

  @D51 @negative
  Scenario: A malformed request to an unknown name gives 400, because field rules come before 404
    When I send a multipart "POST" to "/vocabularies/${Missing}/versions" with these parts
      | name           | value         |
      | vocabularyFile | @file:valid:A |
      | description    | Desc          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @AC_SIMPL-18173_AC2 @D8 @D37 @negative
  Scenario Outline: A new version without <part> is rejected and changes nothing
    Given a vocabulary "A" exists
    When I send a multipart "POST" to "/vocabularies/${A}/versions" with these parts
      | name   | value   |
      | <p1>   | <v1>    |
      | <p2>   | <v2>    |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

    Examples:
      | part           | p1             | v1            | p2          | v2     |
      | vocabularyFile | description    | Desc          | changelog   | Change |
      | description    | vocabularyFile | @file:valid:A | changelog   | Change |
      | changelog      | vocabularyFile | @file:valid:A | description | Desc   |

  @AC_SIMPL-18173_AC2 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: An invalid changelog (<case>) is rejected and changes nothing
    Given a vocabulary "A" exists
    When I send a multipart "POST" to "/vocabularies/${A}/versions" with these parts
      | name           | value         |
      | vocabularyFile | @file:valid:A |
      | description    | Desc          |
      | changelog      | <changelog>   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"
    And vocabulary "A" has 1 version

    Examples:
      | case            | changelog  |
      | empty           | @empty     |
      | spaces only     | @spaces:4  |
      | 1001 characters | @text:1001 |

  @D37 @Q2 @NFR-04
  Scenario: A changelog of 1000 characters is at the limit and is accepted
    Given a vocabulary "A" exists
    When I create a new version of "A" with the changelog "${text:1000}"
    Then the status is 201
    And the response field "changelog" is "${text:1000}"

  @AC_SIMPL-18173_AC2 @D37 @Q2 @NFR-04 @negative
  Scenario: A description of 1001 characters on a new version is rejected
    Given a vocabulary "A" exists
    When I create a new version of "A" with the description "${text:1001}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And vocabulary "A" has 1 version

  @AC_SIMPL-18173_AC6 @D15 @D8 @API-38 @negative
  Scenario: A new version that is not multipart gives 415 and changes nothing
    Given a vocabulary "A" exists
    When I send "POST" to "/vocabularies/${A}/versions" with the JSON body
      """
      {"description": "Desc", "changelog": "Change"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @D15 @D8 @Q2 @NFR-04 @negative
  Scenario: A new version with a file above 5 MB gives 413 and changes nothing
    Given a vocabulary "A" exists
    When I create a new version of "A" with a file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @D15 @D71 @Q2 @NFR-04
  Scenario: A new version with a file of exactly 5 MB is accepted
    Given a vocabulary "A" exists
    When I create a new version of "A" with a file of 5242880 bytes
    Then the status is 201
    And version 1 of vocabulary "A" has the status "DEPRECATED"

  @D52 @negative
  Scenario: A query parameter on POST versions is rejected
    Given a vocabulary "A" exists
    When I send a multipart "POST" to "/vocabularies/${A}/versions?version=2" with these parts
      | name           | value         |
      | vocabularyFile | @file:valid:A |
      | description    | Desc          |
      | changelog      | Change        |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "version"
    And vocabulary "A" has 1 version

  @D49 @negative
  Scenario: A new version for a name that breaks the pattern gives 400 for the path
    When I send a multipart "POST" to "/vocabularies/not-a-name/versions" with these parts
      | name           | value         |
      | vocabularyFile | @file:valid:A |
      | description    | Desc          |
      | changelog      | Change        |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "vocabularyName"
    And every issue has the type "urn:problem-type:belgif:input-validation:schemaViolation"

  @D46 @D8 @API-46 @API-47 @negative
  Scenario: Resubmitting the latest version unchanged gives 409 and deprecates nothing
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file and description of version 1
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '${A}' already has this content and description."
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has the status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check ignores the changelog
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file of version 1, its description and the changelog "A different changelog"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And vocabulary "A" has 1 version

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file of version 1 and the description "   Description of A   "
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And vocabulary "A" has 1 version

  @D46 @negative
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "A" exists with 2 versions
    When I create a new version of "A" with the file and description of version 2
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 2 of vocabulary '${A}' already has this content and description."
    When I create a new version of "A" with the file and description of version 1
    Then the status is 201
    And the response field "version" equals 3

  @D46 @D33
  Scenario: A description-only change with the same file creates a new version
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file of version 1 and the description "A changed description"
    Then the status is 201
    And the response field "version" equals 2
    And version 1 of vocabulary "A" has the status "DEPRECATED"

  @D46 @D51 @negative
  Scenario: The identical-version check runs after the field checks
    Given a vocabulary "A" exists
    When I create a new version of "A" with the file of version 1, its description and the changelog ""
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"
    And vocabulary "A" has 1 version

  @D43 @D82 @D4
  Scenario: Overlapping new versions are all created with distinct numbers
    # D43 asks for 5 parallel requests; D92 allows at most 4 uploads at the same time per instance, so the suite sends 4.
    Given a vocabulary "A" exists
    When I send 4 new versions of "A" in parallel
    Then the parallel responses have the statuses "201, 201, 201, 201"
    And the parallel responses got the versions 2 to 5 once each
    And vocabulary "A" has 5 versions
    And version 5 of vocabulary "A" has the status "ACTIVE"
    And version 1 of vocabulary "A" has the status "DEPRECATED"
    And version 2 of vocabulary "A" has the status "DEPRECATED"
    And version 3 of vocabulary "A" has the status "DEPRECATED"
    And version 4 of vocabulary "A" has the status "DEPRECATED"
