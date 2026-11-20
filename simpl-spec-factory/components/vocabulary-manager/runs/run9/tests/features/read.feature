Feature: Read vocabularies, versions, version history and content
  GET operations for one vocabulary (D25), its versions (D26, D23) and its content (D29, D70).

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D29
  Scenario: The full details of an existing version are returned as JSON with a content link
    Given a vocabulary "Health" exists with 2 versions
    When I send a "GET" request to "/vocabularies/${Health}/versions/1"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "href" is "/vocabularies/${Health}/versions/1"
    And the response field "contentHref" matches "^https?://.+/content/${Health}/versions/1$"
    And the response field "latestVersionHref" is "/vocabularies/${Health}/versions/2"
    And the response field "name" is "${Health}"
    And the response field "version" is 1
    And the response field "status" is "DEPRECATED"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${adminEmail}"
    And the response has the fields "description, creationDate, lastUpdateDate"
    When I send a "GET" request to "/content/${Health}/versions/1"
    Then the response status is 200
    And the response body is the uploaded content of version 1 of "Health"

  @D5
  Scenario: latestVersionHref is also filled on the latest version itself
    Given a vocabulary "Health" exists
    When I send a "GET" request to "/vocabularies/${Health}/versions/1"
    Then the response field "latestVersionHref" is "/vocabularies/${Health}/versions/1"

  @D5 @D25
  Scenario: The latest version is the highest number, also when it is DEPRECATED
    Given a vocabulary "Health" exists with 2 versions
    And version 2 of "Health" was set to "DEPRECATED"
    And version 1 of "Health" was set to "ACTIVE"
    When I send a "GET" request to "/vocabularies/${Health}"
    Then the response field "version" is 2
    When I send a "GET" request to "/content/${Health}"
    Then the response body is the uploaded content of version 2 of "Health"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404 with the decided detail
    Given a vocabulary "Health" exists
    When I send a "GET" request to "/vocabularies/${Health}/versions/2"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 2 of vocabulary '${Health}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send a "GET" request to "/vocabularies/${Ghost}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D91 @D55 @negative
  Scenario: The largest 32-bit version number is a valid path value that is not found
    Given a vocabulary "Health" exists
    When I send a "GET" request to "/vocabularies/${Health}/versions/2147483647"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 2147483647 of vocabulary '${Health}' was not found."

  @AC_SIMPL-18168_AC1 @D23 @D26 @D35
  Scenario: The version history lists all versions newest first with the metadata for comparison
    Given a vocabulary "Health" exists with 3 versions
    When I send a "GET" request to "/vocabularies/${Health}/versions"
    Then the response status is 200
    And the version numbers of the items are "3,2,1"
    And every item has the fields "href, contentHref, name, version, description, changelog, status, createdBy, creationDate, lastUpdateDate"
    And the page fields are page 1, pageSize 20, total 3 and totalPages 1
    And the items are, in order
      | version | status     | href                                |
      | 3       | ACTIVE     | /vocabularies/${Health}/versions/3  |
      | 2       | DEPRECATED | /vocabularies/${Health}/versions/2  |
      | 1       | DEPRECATED | /vocabularies/${Health}/versions/1  |

  @D23
  Scenario: The version history can be sorted, ties broken by version descending
    Given a vocabulary "Health" exists with 3 versions created one second apart
    And version 1 of "Health" was set to "ACTIVE"
    When I send a "GET" request to "/vocabularies/${Health}/versions?sort=status"
    Then the version numbers of the items are "3,1,2"
    When I send a "GET" request to "/vocabularies/${Health}/versions?sort=-status"
    Then the version numbers of the items are "2,3,1"
    When I send a "GET" request to "/vocabularies/${Health}/versions?sort=creationDate"
    Then the version numbers of the items are "1,2,3"
    When I send a "GET" request to "/vocabularies/${Health}/versions?sort=-creationDate"
    Then the version numbers of the items are "3,2,1"
    When I send a "GET" request to "/vocabularies/${Health}/versions?sort=name"
    Then the version numbers of the items are "3,2,1"

  @D22 @D23
  Scenario: The version history is paged
    Given a vocabulary "Health" exists with 3 versions
    When I send a "GET" request to "/vocabularies/${Health}/versions?pageSize=2&page=2"
    Then the version numbers of the items are "1"
    And the page fields are page 2, pageSize 2, total 3 and totalPages 2
    And the paging link "prev" is "/vocabularies/${Health}/versions" with the parameters "pageSize=2&page=1"
    And the paging link "next" is absent

  @D21 @D23 @negative
  Scenario Outline: An invalid query parameter on the version history is rejected (<query>)
    Given a vocabulary "Health" exists
    When I send a "GET" request to "/vocabularies/${Health}/versions?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"

    Examples:
      | query            | name     |
      | sort=lastUpdated | sort     |
      | sort=version     | sort     |
      | pageSize=101     | pageSize |
      | status=ACTIVE    | status   |

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The version history of a vocabulary that does not exist gives 404
    When I send a "GET" request to "/vocabularies/${Ghost}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D24 @D66 @negative
  Scenario Outline: A vocabulary that does not exist gives 404 on <path>
    When I send a "GET" request to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

    Examples:
      | path                  |
      | /vocabularies/${Ghost} |
      | /content/${Ghost}      |

  @D54 @negative
  Scenario Outline: Paths match the stored name exactly (<path>)
    Given a vocabulary "Health" exists
    When I send a "GET" request to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | path                                |
      | /vocabularies/${upper:Health}       |
      | /vocabularies/${upper:Health}/versions |
      | /content/${upper:Health}            |

  @D49 @negative
  Scenario Outline: An invalid path value gives 400 with one schemaViolation issue (<path>)
    Given a vocabulary "Health" exists
    When I send a "<method>" request to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "<name>"
    And the issue in "path" named "<name>" has the type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | method | path                                         | name           |
      | GET    | /vocabularies/lower${run}                    | vocabularyName |
      | GET    | /vocabularies/Ab                             | vocabularyName |
      | GET    | /vocabularies/${pad:65}                      | vocabularyName |
      | GET    | /vocabularies/lower${run}/versions           | vocabularyName |
      | GET    | /vocabularies/${Health}/versions/0           | version        |
      | GET    | /vocabularies/${Health}/versions/abc         | version        |
      | GET    | /vocabularies/${Health}/versions/-1          | version        |
      | GET    | /vocabularies/${Health}/versions/2147483648  | version        |
      | GET    | /content/lower${run}                         | vocabularyName |
      | GET    | /content/${Health}/versions/0                | version        |
      | GET    | /external-vocabularies/lower${run}           | name           |
      | GET    | /external-vocabularies/lower${run}/content   | name           |
      | DELETE | /external-vocabularies/Ab                    | name           |

  @D49 @negative
  Scenario Outline: An invalid path value on PATCH gives 400 (<path>)
    Given a vocabulary "Health" exists
    When I send a "PATCH" request to "<path>" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "<name>"
    And the vocabulary "Health" has the status "ACTIVE"

    Examples:
      | path                                  | name           |
      | /vocabularies/lower${run}             | vocabularyName |
      | /vocabularies/${Health}/versions/zero | version        |

  @AC_SIMPL-18165_AC1 @AC_SIMPL-18452_AC2 @D29 @D70
  Scenario: The latest content is downloaded byte for byte as text/turtle with the file name <Name>.ttl
    Given a vocabulary "Health" exists with the file "exact-bytes.ttl"
    When I send a "GET" request to "/content/${Health}"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response is a download named "${Health}.ttl"
    And the response body is the uploaded content of version 1 of "Health"

  @AC_SIMPL-18452_AC2 @D29 @D70
  Scenario: A selected version is downloaded with the file name <Name>-v<n>.ttl
    Given a vocabulary "Health" exists with 2 versions
    When I send a "GET" request to "/content/${Health}/versions/1"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the response is a download named "${Health}-v1.ttl"
    And the response body is the uploaded content of version 1 of "Health"

  @AC_SIMPL-18165_AC1 @D29 @D5
  Scenario: The latest content is the content of the newest version
    Given a vocabulary "Health" exists with 2 versions
    When I send a "GET" request to "/content/${Health}"
    Then the response body is the uploaded content of version 2 of "Health"

  @D24 @D55 @negative
  Scenario: Downloading a version that does not exist gives 404
    Given a vocabulary "Health" exists
    When I send a "GET" request to "/content/${Health}/versions/3"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 3 of vocabulary '${Health}' was not found."

  @D21 @negative
  Scenario Outline: An unknown query parameter on a GET is rejected (<path>)
    Given a vocabulary "Health" exists
    When I send a "GET" request to "<path>?unknown=1"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "unknown"

    Examples:
      | path                                 |
      | /vocabularies/${Health}              |
      | /vocabularies/${Health}/versions/1   |
      | /content/${Health}                   |
      | /content/${Health}/versions/1        |
      | /external-vocabularies               |
