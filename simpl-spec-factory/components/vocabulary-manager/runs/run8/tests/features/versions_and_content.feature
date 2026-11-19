Feature: Read vocabulary versions and download vocabulary content
  GET .../versions lists the version history, GET .../versions/{version}
  returns one version, GET /content/... returns the stored Turtle bytes.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18166_AC1 @D5
  Scenario: The latest version links to itself as latest version
    Given a vocabulary "Health" exists with 2 versions
    When I send a GET request to "/vocabularies/${Health}/versions/2"
    Then the response status is 200
    And the response field "latestVersionHref" is "/vocabularies/${Health}/versions/2"
    And the response field "status" is "ACTIVE"

  @AC_SIMPL-18166_AC1 @AC_SIMPL-18176_AC3 @D26 @D27 @D29 @D5 @D58
  Scenario Outline: The full details of an existing version are returned as JSON
    Given a vocabulary "Health" exists with 2 versions
    And I am authenticated as "<user>"
    When I send a GET request to "/vocabularies/${Health}/versions/1"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "name" is "${Health}"
    And the response field "version" is "1"
    And the response field "status" is "DEPRECATED"
    And the response field "description" is "Description of ${Health}"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "href" is "/vocabularies/${Health}/versions/1"
    And the response field "contentHref" is "${contentBase}/content/${Health}/versions/1"
    And the response field "latestVersionHref" is "/vocabularies/${Health}/versions/2"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second

    Examples:
      | user      |
      | ga-admin  |
      | ga-viewer |

  @D5 @D25 @D29 @D38
  Scenario: The latest version is the highest number even when it is DEPRECATED and an older one is ACTIVE
    Given a vocabulary "Health" exists with 2 versions
    And the version status of version 2 of "Health" is set to "DEPRECATED"
    And the version status of version 1 of "Health" is set to "ACTIVE"
    And I remember field "description" of GET "/vocabularies/${Health}/versions/2" as "v2description"
    When I send a GET request to "/vocabularies?name=${uid}"
    Then the response field "items[0].version" is "2"
    And the response field "items[0].description" is "${saved:v2description}"
    When I send a GET request to "/vocabularies/${Health}"
    Then the response field "version" is "2"
    And the response field "description" is "${saved:v2description}"
    When I send a GET request to "/content/${Health}"
    Then the response status is 200
    And the response body is the content uploaded for version 2 of "Health"
    When I send a POST request to "/vocabularies/searches" with JSON body
      """
      {"items": [{"name": "${Health}"}]}
      """
    Then the response field "items[0].latest" is "true"
    And the response field "items[0].version" is "2"
    When I send a GET request to "/vocabularies/${Health}/versions/1"
    Then the response field "latestVersionHref" is "/vocabularies/${Health}/versions/2"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "Health" exists
    When I send a GET request to "/vocabularies/${Health}/versions/5"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 5 of vocabulary '${Health}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/${Ghost}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18168_AC1 @AC_SIMPL-18176_AC2 @D23 @D22 @D26
  Scenario Outline: The version history lists all versions newest first with their metadata
    Given a vocabulary "Health" exists with 3 versions
    And I am authenticated as "<user>"
    When I send a GET request to "/vocabularies/${Health}/versions"
    Then the response status is 200
    And the response field "total" is "3"
    And the response field "page" is "1"
    And the response field "pageSize" is "20"
    And the response items have the versions "3,2,1"
    And every item has the fields "name,version,description,changelog,status,creationDate,href,contentHref,createdBy"
    And the response field "items[0].status" is "ACTIVE"
    And the response field "items[1].status" is "DEPRECATED"
    And the response field "items[2].status" is "DEPRECATED"
    And the response field "items[2].changelog" is "Initial version."

    Examples:
      | user      |
      | ga-admin  |
      | ga-viewer |

  @AC_SIMPL-18168_AC1 @D23
  Scenario Outline: The version history sorts by the supported fields with ties by version descending
    Given a vocabulary "Health" exists with 3 versions a second apart
    And the version status of version 1 of "Health" is set to "ACTIVE"
    When I send a GET request to "/vocabularies/${Health}/versions?sort=<sort>"
    Then the response status is 200
    And the response field "total" is "3"
    And the version items are sorted by "<field>" "<direction>"
    And the response items have the versions "<order>"

    Examples:
      | sort          | field        | direction  | order |
      | name          | name         | ascending  | 3,2,1 |
      | -name         | name         | descending | 3,2,1 |
      | status        | status       | ascending  | 3,1,2 |
      | -status       | status       | descending | 2,3,1 |
      | creationDate  | creationDate | ascending  | 1,2,3 |
      | -creationDate | creationDate | descending | 3,2,1 |

  @D23
  Scenario: Sorting the history by name breaks ties by version descending
    Given a vocabulary "Health" exists with 3 versions
    When I send a GET request to "/vocabularies/${Health}/versions?sort=name"
    Then the response items have the versions "3,2,1"

  @D22
  Scenario: The version history is paged
    Given a vocabulary "Health" exists with 3 versions
    When I send a GET request to "/vocabularies/${Health}/versions?pageSize=2&page=2"
    Then the response status is 200
    And the response items have the versions "1"
    And the response field "total" is "3"
    And the response field "totalPages" is "2"
    And the link "prev" has path "/vocabularies/${Health}/versions" and query "page=1&pageSize=2"
    And the link "next" is absent

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The history of a vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/${Ghost}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Ghost}' was not found."

  @D21 @D23 @negative
  Scenario Outline: The history rejects unsupported sort values and parameters
    Given a vocabulary "Health" exists
    When I send a GET request to "/vocabularies/${Health}/versions?<query>"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query               | param    |
      | sort=lastUpdated    | sort     |
      | sort=version        | sort     |
      | status=ACTIVE       | status   |
      | pageSize=101        | pageSize |

  # ---- content ----------------------------------------------------------------

  @AC_SIMPL-18166_AC1 @AC_SIMPL-18165_AC1 @D29 @D70 @D5
  Scenario Outline: The latest content is downloaded as the stored Turtle bytes
    Given a vocabulary "Health" exists with 2 versions
    And I am authenticated as "<user>"
    When I send a GET request to "/content/${Health}"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response has an attachment named "${Health}.ttl"
    And the response body is the content uploaded for version 2 of "Health"

    Examples:
      | user      |
      | ga-admin  |
      | ga-viewer |

  @AC_SIMPL-18166_AC1 @AC_SIMPL-18452_AC2 @D29 @D70
  Scenario: A specific version is downloaded with a versioned file name
    Given a vocabulary "Health" exists with 2 versions
    When I send a GET request to "/content/${Health}/versions/1"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response has an attachment named "${Health}-v1.ttl"
    And the response body is the content uploaded for version 1 of "Health"

  @D29
  Scenario: Downloaded bytes are the uploaded bytes, comments and layout included
    Given a vocabulary "Health" exists with content
      """
      # A comment that a serialiser would drop.
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex:   <https://example.org/vocab/${uid}#> .

      ex:Thing    a   skos:Concept ;    skos:prefLabel "Thing ü"@en .
      """
    When I send a GET request to "/content/${Health}"
    Then the response status is 200
    And the response body is the content uploaded for version 1 of "Health"

  @D63 @D29 @negative @API-37
  Scenario Outline: A download that accepts only JSON is refused with 406
    Given a vocabulary "Health" exists
    When I send a GET request to "<path>" with header "Accept" set to "application/json"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

    Examples:
      | path                            |
      | /content/${Health}              |
      | /content/${Health}/versions/1   |

  @D63 @D29
  Scenario Outline: A download accepts text/turtle and */*
    Given a vocabulary "Health" exists
    When I send a GET request to "/content/${Health}" with header "Accept" set to "<accept>"
    Then the response status is 200

    Examples:
      | accept      |
      | text/turtle |
      | */*         |
      | <none>      |

  @D63 @negative @API-37
  Scenario: A JSON read that accepts only Turtle is refused with 406
    Given a vocabulary "Health" exists
    When I send a GET request to "/vocabularies/${Health}" with header "Accept" set to "text/turtle"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D24 @D66 @negative
  Scenario Outline: Downloading content of a vocabulary or version that does not exist gives 404
    Given a vocabulary "Health" exists
    When I send a GET request to "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "<detail>"

    Examples:
      | path                          | detail                                              |
      | /content/${Ghost}             | Vocabulary with name '${Ghost}' was not found.      |
      | /content/${Ghost}/versions/1  | Vocabulary with name '${Ghost}' was not found.      |
      | /content/${Health}/versions/4 | Version 4 of vocabulary '${Health}' was not found.  |

  # ---- path values -------------------------------------------------------------

  @D49 @negative
  Scenario Outline: A path name or version that breaks its pattern gives 400
    When I send a <method> request to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And every issue has type "urn:problem-type:belgif:input-validation:schemaViolation"
    And every issue is in "path" named "<param>"

    Examples:
      | method | path                                  | param          |
      | GET    | /vocabularies/lower${uid}             | vocabularyName |
      | GET    | /vocabularies/Ab                      | vocabularyName |
      | GET    | /vocabularies/{{name:65}}             | vocabularyName |
      | GET    | /vocabularies/${Health}/versions/0    | version        |
      | GET    | /vocabularies/${Health}/versions/abc  | version        |
      | GET    | /vocabularies/${Health}/versions/-1   | version        |
      | GET    | /vocabularies/lower${uid}/versions    | vocabularyName |
      | GET    | /content/lower${uid}                  | vocabularyName |
      | GET    | /content/${Health}/versions/0         | version        |
      | GET    | /external-vocabularies/lower${uid}    | name           |
      | DELETE | /external-vocabularies/lower${uid}    | name           |
      | GET    | /external-vocabularies/Bad_${uid}/content | name       |

  @D49 @negative
  Scenario: A PATCH on a path name that breaks the pattern gives 400
    When I send a PATCH request to "/vocabularies/lower${uid}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400
    And every issue is in "path" named "vocabularyName"

  @D54 @negative
  Scenario: Paths match the stored name exactly
    Given a vocabulary "Health" exists
    When I send a GET request to "/vocabularies/${Health.upper}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send a GET request to "/content/${Health.upper}"
    Then the response is a problem with status 404
