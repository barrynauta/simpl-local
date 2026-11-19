Feature: Version history, version details and content download via the API
  GET .../versions lists every version, newest first (D23). GET .../versions/{version}
  returns the version metadata; GET /content/... returns the stored Turtle bytes (D29).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18168_AC1 @D23 @D26 @D27 @AC_SIMPL-18176_AC2
  Scenario: The version history lists all versions newest first with their metadata
    Given the vocabulary "Health" exists with 3 versions
    When I list the versions of "Health" with ""
    Then the response status is 200
    And the response media type is "application/json"
    And the version numbers are "3, 2, 1"
    And the response field "total" is 3
    And every item has the fields "href, contentHref, name, version, description, changelog, status, createdBy, creationDate, lastUpdateDate"
    And the response field "items.0.status" is "ACTIVE"
    And the response field "items.1.status" is "DEPRECATED"
    And the response field "items.2.status" is "DEPRECATED"
    And the response field "items.2.changelog" is "Initial version."
    And the response field "items.2.href" is "/vocabularies/{{name:Health}}/versions/1"
    And the response field "items.2.contentHref" is "{{base}}/content/{{name:Health}}/versions/1"

  @AC_SIMPL-18168_AC1 @D23
  Scenario Outline: The version history sorts on <sort>
    Given the vocabulary "Health" exists with 3 versions
    When I list the versions of "Health" with "sort=<sort>"
    Then the response status is 200
    And the version numbers are "<order>"

    Examples: sorting on a shared value breaks ties by version descending
      | sort          | order   |
      | name          | 3, 2, 1 |
      | -name         | 3, 2, 1 |

  @AC_SIMPL-18168_AC1 @D23 @D58
  Scenario: The version history sorts on creation date
    Given the vocabulary "Health" exists
    And a second has passed
    And the vocabulary "Health" has a new version
    And a second has passed
    And the vocabulary "Health" has a new version
    When I list the versions of "Health" with "sort=creationDate"
    Then the version numbers are "1, 2, 3"
    When I list the versions of "Health" with "sort=-creationDate"
    Then the version numbers are "3, 2, 1"

  @D23
  Scenario: The version history sorts on status with ties by version descending
    Given the vocabulary "Health" exists with 3 versions
    When I list the versions of "Health" with "sort=status"
    Then the items with the same status are grouped and sorted by version descending
    And I keep the status order as the ascending order
    When I list the versions of "Health" with "sort=-status"
    Then the items with the same status are grouped and sorted by version descending
    And the status groups come in the reverse order of the ascending sort

  @D22 @D23
  Scenario: The version history is paged
    Given the vocabulary "Health" exists with 3 versions
    When I list the versions of "Health" with "pageSize=1&page=2"
    Then the response status is 200
    And the version numbers are "2"
    And the response field "total" is 3
    And the response field "totalPages" is 3
    And the response has the links "self, first, prev, next, last" and not ""

  @D21 @D22 @negative
  Scenario Outline: The version history rejects the parameter <query>
    Given the vocabulary "Health" exists
    When I list the versions of "Health" with "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | query            | parameter |
      | sort=lastUpdated | sort      |
      | sort=version     | sort      |
      | pageSize=101     | pageSize  |
      | page=0           | page      |
      | status=ACTIVE    | status    |

  @AC_SIMPL-18168_AC2 @D24 @negative
  Scenario: The version history of a vocabulary that does not exist gives 404
    When I list the versions of "Ghost" with ""
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18166_AC1 @D26 @D5 @D27 @AC_SIMPL-18176_AC3
  Scenario: The details of an older version point to the latest version
    Given the vocabulary "Health" exists with 2 versions
    When I send "GET" to "/vocabularies/{{name:Health}}/versions/1"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "name" is "{{name:Health}}"
    And the response field "version" is 1
    And the response field "status" is "DEPRECATED"
    And the response field "changelog" is "Initial version."
    And the response field "description" is "Description of Health {{run}}"
    And the response field "createdBy" is "{{email:ga-admin}}"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    And the response field "href" is "/vocabularies/{{name:Health}}/versions/1"
    And the response field "contentHref" is "{{base}}/content/{{name:Health}}/versions/1"
    And the response field "latestVersionHref" is "/vocabularies/{{name:Health}}/versions/2"

  @AC_SIMPL-18166_AC1 @D29
  Scenario: The full details of a version are its metadata and its content
    Given the vocabulary "Health" exists with 2 versions
    When I send "GET" to "/content/{{name:Health}}/versions/1"
    Then the response status is 200
    And the response media type is "text/turtle"
    And the download file name is "{{name:Health}}-v1.ttl"
    And the response body equals the content of version 1 of "Health"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: The details of a version that does not exist give 404
    Given the vocabulary "Health" exists
    When I send "GET" to "/vocabularies/{{name:Health}}/versions/7"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 7 of vocabulary '{{name:Health}}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @negative
  Scenario: The details of a version of an unknown vocabulary give 404
    When I send "GET" to "/vocabularies/{{name:Ghost}}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D49 @negative
  Scenario Outline: The details of the invalid version <version> give 400
    Given the vocabulary "Health" exists
    When I send "GET" to "/vocabularies/{{name:Health}}/versions/<version>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "version" with type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | version |
      | 0       |
      | abc     |
      | -3      |

  # ---- content download ----------------------------------------------------------------

  @D29 @D5 @AC_SIMPL-18165_AC1
  Scenario: The content of the latest version is downloaded byte for byte
    Given the vocabulary "Health" exists with 2 versions
    When I send "GET" to "/content/{{name:Health}}"
    Then the response status is 200
    And the response media type is "text/turtle"
    And the download file name is "{{name:Health}}.ttl"
    And the response body equals the content of version 2 of "Health"

  @D29
  Scenario: Non-ASCII content is returned exactly as uploaded
    Given the vocabulary "Health" exists with the content of the file "utf8_content.ttl"
    When I send "GET" to "/content/{{name:Health}}/versions/1"
    Then the response status is 200
    And the response body equals the file "utf8_content.ttl"

  @D5 @D29
  Scenario: The latest content is the highest version even when it is deprecated
    Given the vocabulary "Health" exists with 2 versions
    And version 2 of "Health" is set to "DEPRECATED"
    And version 1 of "Health" is set to "ACTIVE"
    When I send "GET" to "/content/{{name:Health}}"
    Then the response status is 200
    And the response body equals the content of version 2 of "Health"

  @D24 @D29 @negative
  Scenario Outline: Downloading content that does not exist gives 404 on <path>
    Given the vocabulary "Health" exists
    When I send "GET" to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | path                                  |
      | /content/{{name:Ghost}}               |
      | /content/{{name:Ghost}}/versions/1    |
      | /content/{{name:Health}}/versions/4   |

  @D21 @negative
  Scenario Outline: An unknown query parameter on a content download is rejected: <path>
    Given the vocabulary "Health" exists
    When I send "GET" to "<path>?format=ttl"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "format"

    Examples:
      | path                                |
      | /content/{{name:Health}}            |
      | /content/{{name:Health}}/versions/1 |

  @D49 @negative
  Scenario: Downloading content with an invalid path name gives 400
    When I send "GET" to "/content/lower{{run}}"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "vocabularyName" with type "urn:problem-type:belgif:input-validation:schemaViolation"
