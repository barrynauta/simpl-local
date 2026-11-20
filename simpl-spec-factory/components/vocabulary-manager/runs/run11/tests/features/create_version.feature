Feature: Create a new version of a vocabulary through the API
  POST /vocabularies/{vocabularyName}/versions assigns the next version number, stores the new
  version as ACTIVE and deprecates every older ACTIVE version in the same transaction (D4, D7).

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @AC_SIMPL-18189_AC1 @AC_SIMPL-18186_AC1 @D4 @D6 @D7 @D26 @D27 @D10
  Scenario: A new version gets the next number, becomes ACTIVE and deprecates the previous one
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request field "description" is "Second description ${SID}"
    And the request field "changelog" is "Added concept two."
    When I submit the request
    Then the response status is 201
    And the response header "Location" is "/vocabularies/${A}/versions/2"
    And the response field "href" equals the response header "Location"
    And the response field "name" is "${A}"
    And the response field "version" is 2
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Second description ${SID}"
    And the response field "changelog" is "Added concept two."
    And the response field "createdBy" is "${EMAIL:admin}"
    And the response field "contentHref" is "${CB}/content/${A}/versions/2"
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/2"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response has no validation warnings
    And version 1 of vocabulary "A" has status "DEPRECATED"
    And version 2 of vocabulary "A" has status "ACTIVE"
    And vocabulary "A" has latest version 2

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18191_AC1 @D4
  Scenario: Every older version is DEPRECATED after several new versions
    Given a vocabulary "A" exists
    And vocabulary "A" has 3 versions
    And a new version request for vocabulary "A"
    When I submit the request
    Then the response status is 201
    And the response field "version" is 4
    And versions 1, 2 and 3 of vocabulary "A" have status "DEPRECATED"
    And version 4 of vocabulary "A" has status "ACTIVE"

  @D4
  Scenario: Older versions deprecated by a new version take its creation time as last update date
    Given a vocabulary "A" exists
    And the clock has moved on by more than one second
    And a new version request for vocabulary "A"
    When I submit the request
    Then the response status is 201
    And I remember the response field "creationDate" as "v2created"
    When I send a GET request to "/vocabularies/${A}/versions/1"
    Then the response field "status" is "DEPRECATED"
    And the response field "lastUpdateDate" equals the remembered "v2created"

  @D4 @D3
  Scenario: After a reactivation the next new version deprecates every ACTIVE version
    Given a vocabulary "A" exists
    And vocabulary "A" has 2 versions
    And the status of version 1 of vocabulary "A" is changed to "ACTIVE"
    And a new version request for vocabulary "A"
    When I submit the request
    Then the response status is 201
    And the response field "version" is 3
    And versions 1 and 2 of vocabulary "A" have status "DEPRECATED"
    And version 3 of vocabulary "A" has status "ACTIVE"

  @D3
  Scenario: A new version of a DEPRECATED vocabulary is created and the vocabulary stays DEPRECATED
    Given a vocabulary "A" exists
    And the status of vocabulary "A" is changed to "DEPRECATED"
    And a new version request for vocabulary "A"
    When I submit the request
    Then the response status is 201
    And the response field "status" is "ACTIVE"
    And vocabulary "A" has status "DEPRECATED"
    And version 2 of vocabulary "A" has status "ACTIVE"

  @D28 @D25 @D5
  Scenario: The vocabulary shows the description and number of its latest version
    Given a vocabulary "A" exists with description "First ${SID}"
    And vocabulary "A" has a new version with description "Second ${SID}"
    When I send a GET request to "/vocabularies/${A}"
    Then the response status is 200
    And the response field "version" is 2
    And the response field "description" is "Second ${SID}"
    When I send a GET request to "/vocabularies/${A}/versions/1"
    Then the response field "description" is "First ${SID}"

  @D25 @D27
  Scenario: The vocabulary keeps the creator and creation date of version 1
    Given a vocabulary "A" exists
    And I remember the field "creationDate" of "/vocabularies/${A}" as "created"
    And the clock has moved on by more than one second
    And vocabulary "A" has a new version
    When I send a GET request to "/vocabularies/${A}"
    Then the response field "creationDate" equals the remembered "created"
    And the response field "createdBy" is "${EMAIL:admin}"
    And the response field "lastUpdateDate" is later than the remembered "created"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario: A manual version number is rejected and nothing is created
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request has an extra part "version" with value "7"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "version"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: An incomplete new version request is rejected: <field> missing
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request field "<field>" is removed
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

    Examples:
      | field          |
      | description    |
      | changelog      |
      | vocabularyFile |

  @AC_SIMPL-18173_AC2 @D52 @negative
  Scenario: A name field on a new version request is an unknown field
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request has an extra part "name" with value "${A}"
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And vocabulary "A" has 1 version

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative
  Scenario: A new version for a vocabulary that does not exist is rejected with 404
    Given a new version request for vocabulary "Missing"
    When I submit the request
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary "Missing" is stored

  @D51 @negative
  Scenario: A malformed request to an unknown vocabulary gives 400, not 404
    Given a new version request for vocabulary "Missing"
    And the request field "changelog" is removed
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @D54 @negative
  Scenario: The vocabulary name in the path is matched exactly
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies/${upper:A}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send a GET request to "/vocabularies/${upper:A}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send a GET request to "/content/${upper:A}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send a PATCH request to "/vocabularies/${upper:A}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And vocabulary "A" has status "ACTIVE"

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38
  Scenario Outline: A new version request that is not multipart/form-data gets 415: <content type>
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    When I submit the request as "<content type>"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "A" has 1 version

    Examples:
      | content type     |
      | application/json |
      | text/turtle      |

  @D52 @negative
  Scenario: A query parameter on a new version request is rejected
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    When I submit the request with the query "?version=2"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "version"
    And vocabulary "A" has 1 version

  @D46 @D8 @negative @API-46
  Scenario: Resubmitting the latest version unchanged is refused and deprecates nothing
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is the content of version 1 of vocabulary "A"
    And the request description is the description of version 1 of vocabulary "A"
    And the request field "changelog" is "A different changelog does not matter"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '${A}' already has this content and description."
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is the content of version 1 of vocabulary "A"
    And the request description is the description of version 1 of vocabulary "A" surrounded by spaces
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And vocabulary "A" has 1 version

  @D46 @D5 @negative
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    And a new version request for vocabulary "A"
    And the request file is the content of version 2 of vocabulary "A"
    And the request description is the description of version 2 of vocabulary "A"
    When I submit the request
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 2 of vocabulary '${A}' already has this content and description."
    And vocabulary "A" has 2 versions

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is the content of version 1 of vocabulary "A"
    And the request field "description" is "Only the description changed ${SID}"
    When I submit the request
    Then the response status is 201
    And the response field "version" is 2
    And version 1 of vocabulary "A" has status "DEPRECATED"

  @D46
  Scenario: A content-only change creates a new version
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request description is the description of version 1 of vocabulary "A"
    When I submit the request
    Then the response status is 201
    And the response field "version" is 2

  @D46 @D51 @negative
  Scenario: The identical-version check runs after the field checks
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is the content of version 1 of vocabulary "A"
    And the request description is the description of version 1 of vocabulary "A"
    And the request field "changelog" is "   "
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @D43 @D119 @D82
  Scenario: Four overlapping new versions all succeed with distinct numbers
    Given a vocabulary "A" exists
    When I submit 4 new version requests for vocabulary "A" in parallel
    Then every parallel response has status 201
    And the parallel responses carry the versions 2, 3, 4 and 5 each exactly once
    And the parallel responses carry distinct Location headers
    And vocabulary "A" has 5 versions
    And version 5 of vocabulary "A" has status "ACTIVE"
    And versions 1, 2, 3 and 4 of vocabulary "A" have status "DEPRECATED"

  @D43 @D37 @negative
  Scenario: Two overlapping creations of one name give one 201 and one 409
    When I submit 2 create requests for vocabulary "A" in parallel
    Then the parallel responses have the statuses 201 and 409
    And exactly one vocabulary "A" is stored
    And vocabulary "A" has 1 version
