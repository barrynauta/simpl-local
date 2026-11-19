Feature: Create a new version of a vocabulary through the API
  POST /vocabularies/{vocabularyName}/versions numbers the new version
  (highest plus 1), makes it ACTIVE and deprecates every older ACTIVE version in
  the same transaction. The vocabulary status does not change.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18191_AC1 @AC_SIMPL-18189_AC1 @AC_SIMPL-18186_AC1
  @D4 @D6 @D7 @D26 @D27
  Scenario: A new version gets the next number, is ACTIVE and deprecates the older version
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value                     |
      | description    | Second description ${uid} |
      | changelog      | Added diagnostics.        |
      | vocabularyFile | default                   |
    Then the response status is 201
    And the response field "version" is "2"
    And the response field "status" is "ACTIVE"
    And the response field "name" is "${Health}"
    And the response field "description" is "Second description ${uid}"
    And the response field "changelog" is "Added diagnostics."
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "href" is "/vocabularies/${Health}/versions/2"
    And the response field "contentHref" is "${contentBase}/content/${Health}/versions/2"
    And the response field "validationWarnings" is an empty list
    And the response header "Location" is "/vocabularies/${Health}/versions/2"
    And I remember the response field "creationDate" as "v2created"
    And version 1 of "Health" has version status "DEPRECATED"
    And version 2 of "Health" has version status "ACTIVE"
    And vocabulary "Health" has vocabulary status "ACTIVE"

  @AC_SIMPL-18191_AC1 @D4 @D25
  Scenario: The deprecated older version gets the creation time of the new version as last update date
    Given a vocabulary "Health" exists
    When I create a new version of "Health"
    Then the response status is 201
    And I remember the response field "creationDate" as "v2created"
    When I send a GET request to "/vocabularies/${Health}/versions/1"
    Then the response field "status" is "DEPRECATED"
    And the response field "lastUpdateDate" equals the remembered "v2created"

  @AC_SIMPL-18173_AC1 @D25 @D28 @D5
  Scenario: The vocabulary shows the latest version number and description and keeps creator and creation date
    Given a vocabulary "Health" exists
    And I remember field "creationDate" of GET "/vocabularies/${Health}" as "created"
    And I remember field "lastUpdateDate" of GET "/vocabularies/${Health}" as "updated"
    And I wait for the next second
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value              |
      | description    | Latest text ${uid} |
      | changelog      | Second             |
      | vocabularyFile | default            |
    Then the response status is 201
    When I send a GET request to "/vocabularies/${Health}"
    Then the response field "version" is "2"
    And the response field "description" is "Latest text ${uid}"
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" equals the remembered "created"
    And the response field "lastUpdateDate" is later than the remembered "updated"
    And the response field "status" is "ACTIVE"

  @AC_SIMPL-18191_AC1 @D4 @D7
  Scenario: Every older ACTIVE version is deprecated, also after a manual reactivation
    Given a vocabulary "Health" exists with 3 versions
    And the version status of version 1 of "Health" is set to "ACTIVE"
    And version 1 of "Health" has version status "ACTIVE"
    And version 3 of "Health" has version status "ACTIVE"
    When I create a new version of "Health"
    Then the response status is 201
    And the response field "version" is "4"
    And versions 1 to 3 of "Health" have version status "DEPRECATED"
    And version 4 of "Health" has version status "ACTIVE"

  @AC_SIMPL-18190_AC2 @D7 @D8 @D51 @negative
  Scenario: A request that carries a version form field is rejected and creates nothing
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value     |
      | version        | 7         |
      | description    | Manual    |
      | changelog      | Manual    |
      | vocabularyFile | default   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "version"
    And vocabulary "Health" has 1 version
    And version 1 of "Health" has version status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A new version with invalid or incomplete data is rejected with feedback
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value     |
      | <part1>        | <value1>  |
      | <part2>        | <value2>  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And vocabulary "Health" has 1 version
    And version 1 of "Health" has version status "ACTIVE"

    Examples:
      | field          | part1       | value1            | part2          | value2    |
      | changelog      | description | Next              | vocabularyFile | default   |
      | description    | changelog   | Next              | vocabularyFile | default   |
      | vocabularyFile | description | Next              | changelog      | Next      |

  @AC_SIMPL-18173_AC2 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: Changelog and description of a new version are limited to 1000 characters after trimming
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value          |
      | description    | <description>  |
      | changelog      | <changelog>    |
      | vocabularyFile | default        |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And vocabulary "Health" has 1 version

    Examples:
      | field       | description       | changelog         |
      | changelog   | Next              | {{repeat:c:1001}} |
      | changelog   | Next              | {{repeat: :4}}    |
      | description | {{repeat:d:1001}} | Next              |

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative
  Scenario: A new version of a vocabulary that does not exist is rejected with 404
    When I create a new version of "Ghost"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has 1 issue
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary whose name contains "${uid}" is stored

  @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @negative @OWASP-02 @SEC-05
  Scenario Outline: A user without GA_VOCABULARY_ADMIN cannot create a version
    Given a vocabulary "Health" exists
    And I am authenticated as "<user>"
    When I create a new version of "Health"
    Then the response is a problem with status 403 and type "urn:problem-type:simpl:forbidden"
    And the problem title is "Forbidden"
    And vocabulary "Health" has 1 version

    Examples:
      | user      |
      | ga-viewer |
      | ga-norole |

  @AC_SIMPL-18173_AC5 @D17 @negative @OWASP-08
  Scenario: A new version without a token is rejected with 401
    Given a vocabulary "Health" exists
    And I am not authenticated
    When I create a new version of "Health"
    Then the response is a problem with status 401 and type "urn:problem-type:simpl:unauthorized"
    And the problem title is "Unauthorized"
    And vocabulary "Health" has 1 version

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38
  Scenario: A new version request that is not multipart/form-data is rejected with 415
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with JSON body
      """
      {"description": "Next", "changelog": "Next"}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "Health" has 1 version

  @D3 @AC_SIMPL-18173_AC1
  Scenario: A DEPRECATED vocabulary may get a new version and stays DEPRECATED
    Given a vocabulary "Health" exists
    And the vocabulary status of "Health" is set to "DEPRECATED"
    When I create a new version of "Health"
    Then the response status is 201
    And the response field "version" is "2"
    And the response field "status" is "ACTIVE"
    And vocabulary "Health" has vocabulary status "DEPRECATED"
    And version 1 of "Health" has version status "DEPRECATED"

  @D46 @D8 @negative
  Scenario: Resubmitting the latest version unchanged is refused with 409 and deprecates nothing
    Given a vocabulary "Health" exists with description "Same text"
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value               |
      | description    | Same text           |
      | changelog      | A different log     |
      | vocabularyFile | uploaded:Health:1   |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '${Health}' already has this content and description."
    And vocabulary "Health" has 1 version
    And version 1 of "Health" has version status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "Health" exists with description "Same text"
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value                          |
      | description    | {{repeat: :2}}Same text{{repeat: :2}} |
      | changelog      | Again                          |
      | vocabularyFile | uploaded:Health:1              |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And vocabulary "Health" has 1 version

  @D46 @D51 @negative
  Scenario: Field checks come before the identical-version check
    Given a vocabulary "Health" exists with description "Same text"
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value             |
      | description    | Same text         |
      | changelog      | {{repeat: :3}}    |
      | vocabularyFile | uploaded:Health:1 |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "changelog"
    And vocabulary "Health" has 1 version

  @D37 @Q2 @NFR-04
  Scenario: Description and changelog of exactly 1000 characters are accepted on a new version
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value             |
      | description    | {{repeat:d:1000}} |
      | changelog      | {{repeat:c:1000}} |
      | vocabularyFile | default           |
    Then the response status is 201
    And the response field "version" is "2"

  @D46 @D33
  Scenario: A description-only change creates a new version with the same content
    Given a vocabulary "Health" exists with description "Old text"
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value             |
      | description    | New text          |
      | changelog      | Description only  |
      | vocabularyFile | uploaded:Health:1 |
    Then the response status is 201
    And the response field "version" is "2"
    When I send a GET request to "/content/${Health}/versions/2"
    Then the response status is 200
    And the response body is the content uploaded for version 1 of "Health"

  @D46
  Scenario: Same description with changed content creates a new version
    Given a vocabulary "Health" exists with description "Same text"
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value      |
      | description    | Same text  |
      | changelog      | New terms  |
      | vocabularyFile | default    |
    Then the response status is 201
    And the response field "version" is "2"

  @D46
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "Health" exists with description "Same text"
    And vocabulary "Health" has a new version
    When I send a POST request to "/vocabularies/${Health}/versions" with these form parts
      | part           | value             |
      | description    | Same text         |
      | changelog      | Back to version 1 |
      | vocabularyFile | uploaded:Health:1 |
    Then the response status is 201
    And the response field "version" is "3"

  @D43 @D82
  Scenario: Five overlapping version creations all succeed with distinct numbers
    Given a vocabulary "Health" exists
    When I create 5 new versions of "Health" in parallel
    Then all parallel responses have status 201
    And the parallel responses created versions 2 to 6 once each with distinct Location headers
    And vocabulary "Health" has 6 versions
    And version 6 of "Health" has version status "ACTIVE"
    And versions 1 to 5 of "Health" have version status "DEPRECATED"

  @D43 @D82 @D37
  Scenario: Two overlapping creations of the same name give one 201 and one 409
    When I create the vocabulary "Twin" 2 times in parallel
    Then the parallel responses have the statuses "201,409"
    And 1 vocabulary whose name contains "${Twin}" is stored

  @D43 @D4
  Scenario: Two overlapping identical status changes give 200 and 409
    Given a vocabulary "Health" exists
    When I send 2 PATCH requests to "/vocabularies/${Health}" with status "DEPRECATED" in parallel
    Then the parallel responses have the statuses "200,409"
    And vocabulary "Health" has vocabulary status "DEPRECATED"
