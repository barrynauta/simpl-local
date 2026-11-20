Feature: Read a vocabulary, its version history and one version through the API
  GET /vocabularies/{vocabularyName}, GET .../versions and GET .../versions/{version} (D5, D23, D25 to D27).

  Background:
    Given I am a vocabulary administrator

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D58
  Scenario: The full metadata of an existing version is returned
    Given a vocabulary "A" exists with description "First ${SID}"
    And vocabulary "A" has a new version with description "Second ${SID}"
    When I send a GET request to "/vocabularies/${A}/versions/1"
    Then the response status is 200
    And the response content type is JSON
    And the response field "name" is "${A}"
    And the response field "version" is 1
    And the response field "description" is "First ${SID}"
    And the response field "changelog" is "Initial version."
    And the response field "status" is "DEPRECATED"
    And the response field "createdBy" is "${EMAIL:admin}"
    And the response field "href" is "/vocabularies/${A}/versions/1"
    And the response field "contentHref" is "${CB}/content/${A}/versions/1"
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/2"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    And the response field "validationWarnings" is absent

  @AC_SIMPL-18166_AC1 @D5
  Scenario: The latest version links to itself as latest version
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies/${A}/versions/1"
    Then the response status is 200
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/1"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies/${A}/versions/3"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 3 of vocabulary '${A}' was not found."
    And the problem reveals no implementation details

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/${Missing}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @D5 @D25 @D3
  Scenario: The vocabulary shows its latest version even when that version is DEPRECATED
    Given a vocabulary "A" exists
    And vocabulary "A" has a new version
    And the status of version 2 of vocabulary "A" is changed to "DEPRECATED"
    When I send a GET request to "/vocabularies/${A}"
    Then the response status is 200
    And the response field "version" is 2
    And the response field "status" is "ACTIVE"

  @D24 @D66 @negative
  Scenario: A vocabulary that does not exist gives 404 with one issue
    When I send a GET request to "/vocabularies/${Missing}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18168_AC1 @D23 @D26 @D27
  Scenario: The version history lists all versions, newest first, with their metadata
    Given a vocabulary "A" exists
    And vocabulary "A" has 3 versions
    When I send a GET request to "/vocabularies/${A}/versions"
    Then the response status is 200
    And the item versions are "3, 2, 1"
    And the item statuses are "ACTIVE, DEPRECATED, DEPRECATED"
    And the response field "total" is 3
    And the response field "items[2].changelog" is "Initial version."
    And the response field "items[0].name" is "${A}"
    And the response field "items[0].href" is "/vocabularies/${A}/versions/3"
    And the response field "items[0].contentHref" is "${CB}/content/${A}/versions/3"
    And the response field "items[0].createdBy" is "${EMAIL:admin}"
    And the response field "items[0].description" is present
    And the response field "items[0].changelog" is present
    And the response field "items[0].creationDate" is a UTC timestamp to the second
    And the response field "items[0].lastUpdateDate" is a UTC timestamp to the second
    And the response field "items[2].latestVersionHref" is "/vocabularies/${A}/versions/3"

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The version history of a vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/${Missing}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @D23
  Scenario Outline: Sorting the version history: <sort>
    Given a vocabulary "A" exists
    And vocabulary "A" has 3 versions
    And the status of version 1 of vocabulary "A" is changed to "ACTIVE"
    When I send a GET request to "/vocabularies/${A}/versions?sort=<sort>"
    Then the response status is 200
    And the item versions are "<versions>"

    Examples:
      | sort          | versions |
      | status        | 3, 1, 2  |
      | -status       | 2, 3, 1  |
      | name          | 3, 2, 1  |
      | -creationDate | 3, 2, 1  |

  @D22 @D23 @D111
  Scenario: The version history is paged
    Given a vocabulary "A" exists
    And vocabulary "A" has 3 versions
    When I send a GET request to "/vocabularies/${A}/versions?pageSize=1&page=2"
    Then the response status is 200
    And the item versions are "2"
    And the response field "total" is 3
    And the response field "totalPages" is 3
    And the response field "self" is "/vocabularies/${A}/versions?page=2&pageSize=1"
    And the response field "prev" is "/vocabularies/${A}/versions?page=1&pageSize=1"
    And the response field "next" is "/vocabularies/${A}/versions?page=3&pageSize=1"
    And the response field "first" is "/vocabularies/${A}/versions?page=1&pageSize=1"
    And the response field "last" is "/vocabularies/${A}/versions?page=3&pageSize=1"

  @D21 @D22 @D23 @negative
  Scenario Outline: Invalid version history parameters are rejected: <query>
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies/${A}/versions?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query         | parameter   |
      | sort=lastUpdated | sort     |
      | sort=version  | sort        |
      | pageSize=101  | pageSize    |
      | page=0        | page        |
      | status=ACTIVE | status      |
      | name=${SID}   | name        |

  @D21 @negative
  Scenario Outline: An unknown query parameter on a read of one resource is rejected: <path>
    Given a vocabulary "A" exists
    When I send a GET request to "<path>?foo=bar"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "query" named "foo"

    Examples:
      | path                          |
      | /vocabularies/${A}            |
      | /vocabularies/${A}/versions/1 |
      | /content/${A}                 |
      | /content/${A}/versions/1      |

  @D49 @D116 @D91 @negative
  Scenario Outline: An invalid name or version in the path gives 400: <path>
    When I send a GET request to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "path" named "<parameter>" with type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | path                                  | parameter      |
      | /vocabularies/lowercase               | vocabularyName |
      | /vocabularies/Ab                      | vocabularyName |
      | /vocabularies/Bad-Name                | vocabularyName |
      | /vocabularies/${A}x${A}x${A}x${A}x${A}x${A} | vocabularyName |
      | /vocabularies/lowercase/versions      | vocabularyName |
      | /vocabularies/lowercase/versions/1    | vocabularyName |
      | /vocabularies/${A}/versions/0         | version        |
      | /vocabularies/${A}/versions/abc       | version        |
      | /vocabularies/${A}/versions/1.0       | version        |
      | /vocabularies/${A}/versions/-1        | version        |
      | /vocabularies/${A}/versions/2147483648 | version       |
      | /content/lowercase                    | vocabularyName |
      | /content/${A}/versions/0              | version        |

  @D49 @D91 @negative
  Scenario: A valid version number that is not stored gives 404, not 400
    Given a vocabulary "A" exists
    When I send a GET request to "/vocabularies/${A}/versions/2147483647"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
