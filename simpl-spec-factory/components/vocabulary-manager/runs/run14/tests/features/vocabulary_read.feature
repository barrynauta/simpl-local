Feature: Read vocabularies, versions, history and content via the API
  GET /vocabularies (paged list), GET /vocabularies/{vocabularyName}, the version list and a version,
  and the content downloads (D5, D25, D26, D29, D35, D162).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18162_AC1 @D22 @D25 @API-36
  Scenario: The list of vocabularies is paged and every item carries its metadata
    Given a vocabulary "$Alpha" exists
    And a vocabulary "$Beta" exists
    When I send a GET request to "/vocabularies?name=$RUN"
    Then the response status is 200
    And the response media type is "application/json"
    And the response has the fields "page, pageSize, total, totalPages, items, self, first"
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the response field "total" equals 2
    And the response field "totalPages" equals 1
    And the response items are named "$Alpha, $Beta"
    And every item has the fields "href, contentHref, name, version, description, status, createdBy, creationDate, lastUpdateDate"

  @AC_SIMPL-18162_AC1 @D16
  Scenario: A viewer gets the paged list too
    Given a vocabulary "$Alpha" exists
    And I am authenticated as "ga-viewer"
    When I send a GET request to "/vocabularies?name=$RUN"
    Then the response status is 200
    And the response items are named "$Alpha"

  @AC_SIMPL-18162_AC2 @D22 @D91 @D112 @negative @Q2 @NFR-04 @BLG-err-problem
  Scenario Outline: Wrong pagination parameters are rejected without metadata: <query>
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "/vocabularies?<query>"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "query" named "<param>"
    And the response holds no items

    Examples:
      | query               | param    |
      | page=0              | page     |
      | page=-1             | page     |
      | page=abc            | page     |
      | page=2.0            | page     |
      | page=%2B2           | page     |
      | page=%202           | page     |
      | page=1e1            | page     |
      | page=2147483648     | page     |
      | pageSize=0          | pageSize |
      | pageSize=101        | pageSize |
      | pageSize=-5         | pageSize |
      | pageSize=ten        | pageSize |
      | pageSize=20.5       | pageSize |
      | pageSize=           | pageSize |

  @D22 @Q2 @NFR-04
  Scenario: The highest page size of 100 is accepted
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "/vocabularies?pageSize=100&name=$RUN"
    Then the response status is 200
    And the response field "pageSize" equals 100

  @AC_SIMPL-18162_AC4 @D179 @wip
  Scenario: An internal error while listing gives 500 with the detail and no metadata
    Given I am authenticated as "ga-admin"
    When I send a GET request to "/vocabularies"
    Then the response is a problem with status 500
    And the problem type is "urn:problem-type:belgif:internalServerError"
    And the response holds no items

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D29
  Scenario: A version is returned with its full metadata and its content can be downloaded
    Given vocabulary "$Alpha" has 2 versions
    When I send a GET request to "/vocabularies/$Alpha/versions/1"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "href" equals "/vocabularies/$Alpha/versions/1"
    And the response field "contentHref" equals "$CB/content/$Alpha/versions/1"
    And the response field "latestVersionHref" equals "/vocabularies/$Alpha/versions/2"
    And the response field "name" equals "$Alpha"
    And the response field "version" equals 1
    And the response field "status" equals "DEPRECATED"
    And the response field "createdBy" equals "ga-admin@example.com"
    And the response has the fields "description, changelog, creationDate, lastUpdateDate"
    And the response field "validationWarnings" is absent
    When I send a GET request to "/content/$Alpha/versions/1"
    Then the response status is 200
    And the response body is the uploaded file of "$Alpha" version 1

  @D5
  Scenario: latestVersionHref is filled on the latest version itself
    Given vocabulary "$Alpha" has 2 versions
    When I send a GET request to "/vocabularies/$Alpha/versions/2"
    Then the response field "latestVersionHref" equals "/vocabularies/$Alpha/versions/2"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "/vocabularies/$Alpha/versions/3"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 3 of vocabulary '$Alpha' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/$Missing/versions/1"
    Then the response is a problem with status 404
    And the problem detail is "Vocabulary with name '$Missing' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @D24 @D66 @D204 @negative
  Scenario: A vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/$Missing"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '$Missing' was not found."
    And the problem has 1 issue
    And issue 1 is in "path" named "vocabularyName"

  @AC_SIMPL-18168_AC1 @D23 @D26 @D35
  Scenario: The version history lists every version, newest first, with its metadata
    Given vocabulary "$Alpha" has 3 versions
    When I send a GET request to "/vocabularies/$Alpha/versions"
    Then the response status is 200
    And the response field "total" equals 3
    And the response item versions are "3, 2, 1"
    And every item has the fields "href, contentHref, name, version, description, changelog, status, creationDate, lastUpdateDate, createdBy"
    And the response field "items[0].status" equals "ACTIVE"
    And the response field "items[1].status" equals "DEPRECATED"
    And the response field "items[2].status" equals "DEPRECATED"
    And the response field "items[2].href" equals "/vocabularies/$Alpha/versions/1"

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The history of a vocabulary that does not exist gives 404
    When I send a GET request to "/vocabularies/$Missing/versions"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '$Missing' was not found."

  @D22 @D23
  Scenario: The version history is paged
    Given vocabulary "$Alpha" has 3 versions
    When I send a GET request to "/vocabularies/$Alpha/versions?pageSize=2"
    Then the response item versions are "3, 2"
    And the response field "totalPages" equals 2
    When I send a GET request to "/vocabularies/$Alpha/versions?pageSize=2&page=2"
    Then the response item versions are "1"

  @D23 @D154 @D196 @D128
  Scenario Outline: The version history sorts on a supported field: <sort>
    Given a vocabulary "$Alpha" exists
    And a second has passed
    And a new version of "$Alpha" is created
    And a second has passed
    And a new version of "$Alpha" is created
    And version 3 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"
    When I send a GET request to "/vocabularies/$Alpha/versions?sort=<sort>"
    Then the response status is 200
    And the response item versions are "<order>"

    Examples:
      | sort          | order   |
      | name          | 3, 2, 1 |
      | -name         | 3, 2, 1 |
      | status        | 1, 3, 2 |
      | -status       | 3, 2, 1 |
      | creationDate  | 1, 2, 3 |
      | -creationDate | 3, 2, 1 |

  @D21 @D23 @negative
  Scenario Outline: An unsupported sort value on the version history is rejected: <sort>
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "/vocabularies/$Alpha/versions?sort=<sort>"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "sort"

    Examples:
      | sort         |
      | lastUpdated  |
      | version      |
      | -description |

  @AC_SIMPL-18165_AC1 @D29 @D5 @D70
  Scenario: The latest content is downloaded as Turtle with the vocabulary name as file name
    Given vocabulary "$Alpha" has 2 versions
    When I send a GET request to "/content/$Alpha"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response is an attachment named "$Alpha.ttl"
    And the response body is the uploaded file of "$Alpha" version 2

  @AC_SIMPL-18452_AC2 @D29 @D70
  Scenario: A version's content is downloaded with the version in the file name
    Given vocabulary "$Alpha" has 2 versions
    When I send a GET request to "/content/$Alpha/versions/1"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the response is an attachment named "$Alpha-v1.ttl"
    And the response body is the uploaded file of "$Alpha" version 1

  @D29 @D5
  Scenario: The latest content follows the highest version number, also when it is DEPRECATED
    Given vocabulary "$Alpha" has 2 versions
    And version 2 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"
    When I send a GET request to "/content/$Alpha"
    Then the response body is the uploaded file of "$Alpha" version 2
    When I send a GET request to "/vocabularies/$Alpha"
    Then the response field "version" equals 2

  @D29 @D70
  Scenario: The downloaded bytes are exactly the uploaded bytes
    Given the vocabulary file has these exact bytes:
      """
      # Comment kept as uploaded: é ü ß
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix ex:   <https://example.org/$RUN/bytes#> .

      ex:Scheme    a skos:ConceptScheme ;   skos:prefLabel "Crème brûlée"@fr .
      """
    And a vocabulary "$Alpha" exists with that file
    When I send a GET request to "/content/$Alpha/versions/1"
    Then the response status is 200
    And the response body is the uploaded file of "$Alpha" version 1

  @D24 @negative
  Scenario Outline: Content of something that does not exist gives 404: <path>
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "<path>"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | path                         |
      | /content/$Missing            |
      | /content/$Missing/versions/1 |
      | /content/$Alpha/versions/2   |

  @D63 @API-37 @negative
  Scenario Outline: A download with an Accept header it cannot serve gets 406: <accept>
    Given a vocabulary "$Alpha" exists
    And the request header "Accept" is "<accept>"
    When I send a GET request to "/content/$Alpha"
    Then the response is a problem with status 406
    And the problem type is "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"

    Examples:
      | accept           |
      | application/json |
      | text/html        |

  @D63
  Scenario Outline: A download accepts text/turtle, */* or no Accept header: <accept>
    Given a vocabulary "$Alpha" exists
    And the request header "Accept" is "<accept>"
    When I send a GET request to "/content/$Alpha"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"

    Examples:
      | accept      |
      | text/turtle |
      | */*         |

  @D63
  Scenario: A download without an Accept header is served
    Given a vocabulary "$Alpha" exists
    And the request has no Accept header
    When I send a GET request to "/content/$Alpha/versions/1"
    Then the response status is 200

  @D63 @API-37 @negative
  Scenario Outline: A JSON read with an Accept header it cannot serve gets 406: <path>
    Given a vocabulary "$Alpha" exists
    And the request header "Accept" is "application/xml"
    When I send a GET request to "<path>"
    Then the response is a problem with status 406
    And the problem type is "urn:problem-type:simpl:notAcceptable"

    Examples:
      | path                            |
      | /vocabularies                   |
      | /vocabularies/$Alpha            |
      | /vocabularies/$Alpha/versions   |
      | /vocabularies/$Alpha/versions/1 |
      | /external-vocabularies          |

  @D151 @D127 @D63
  Scenario Outline: A JSON read accepts the decided Accept values and answers application/json: <accept>
    Given a vocabulary "$Alpha" exists
    And the request header "Accept" is "<accept>"
    When I send a GET request to "/vocabularies/$Alpha"
    Then the response status is 200
    And the response media type is "application/json"

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | application/*            |
      | */*                      |

  @D162 @D63 @negative
  Scenario: 406 comes before a bad query value on reads
    Given the request header "Accept" is "application/xml"
    When I send a GET request to "/vocabularies?page=0"
    Then the response is a problem with status 406

  @D162 @negative
  Scenario: A bad query value is reported before 404 on reads
    When I send a GET request to "/vocabularies/$Missing/versions?pageSize=500"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "pageSize"

  @D21 @D52 @negative @BLG-req-valid
  Scenario Outline: An unknown query parameter is rejected on every GET operation: <path>
    Given a vocabulary "$Alpha" exists
    And an external vocabulary "$Ext" is registered with namespace "$EXT/uq"
    When I send a GET request to "<path>"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "query" named "<param>"

    Examples:
      | path                                         | param    |
      | /vocabularies?createdBy=ga-admin             | createdBy |
      | /vocabularies?version=1                      | version  |
      | /vocabularies/$Alpha?expand=all              | expand   |
      | /vocabularies/$Alpha/versions?status=ACTIVE  | status   |
      | /vocabularies/$Alpha/versions/1?format=ttl   | format   |
      | /content/$Alpha?download=true                | download |
      | /content/$Alpha/versions/1?download=true     | download |
      | /external-vocabularies?name=$Ext             | name     |
      | /external-vocabularies?sort=name             | sort     |
      | /external-vocabularies/$Ext?full=true        | full     |
      | /external-vocabularies/$Ext/content?x=1      | x        |

  @D138 @D52 @negative
  Scenario Outline: A query parameter given twice is rejected with one issue: <query>
    When I send a GET request to "/vocabularies?<query>"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "<param>"

    Examples:
      | query                           | param    |
      | page=1&page=2                   | page     |
      | pageSize=10&pageSize=10         | pageSize |
      | sort=name&sort=-name            | sort     |
      | status=ACTIVE&status=DEPRECATED | status   |

  @D49 @negative
  Scenario Outline: An invalid name in a read path gives 400: <path>
    When I send a GET request to "<path>"
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "path" named "<param>"
    And issue 1 has type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | path                                  | param          |
      | /vocabularies/ab                      | vocabularyName |
      | /vocabularies/Ab                      | vocabularyName |
      | /vocabularies/$alpha                  | vocabularyName |
      | /vocabularies/$N65                    | vocabularyName |
      | /vocabularies/$alpha/versions         | vocabularyName |
      | /vocabularies/$alpha/versions/1       | vocabularyName |
      | /content/$alpha                       | vocabularyName |
      | /content/$alpha/versions/1            | vocabularyName |
      | /vocabularies/$Alpha/versions/0       | version        |
      | /vocabularies/$Alpha/versions/x1      | version        |
      | /vocabularies/$Alpha/versions/1.5     | version        |
      | /vocabularies/$Alpha/versions/%2B1    | version        |
      | /vocabularies/$Alpha/versions/2147483648 | version     |
      | /content/$Alpha/versions/0            | version        |
      | /content/$Alpha/versions/-3           | version        |

  @D91 @D49
  Scenario: The highest 32-bit version number is a valid path value
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "/vocabularies/$Alpha/versions/2147483647"
    Then the response is a problem with status 404

  @D54 @negative
  Scenario Outline: A name in another case gives 404 on reads: <path>
    Given a vocabulary "$Alpha" exists
    When I send a GET request to "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                                |
      | /vocabularies/$Alpha!upper          |
      | /vocabularies/$Alpha!upper/versions |
      | /content/$Alpha!upper               |

  @D25 @D160
  Scenario: lastUpdateDate of the vocabulary follows its last change
    Given a vocabulary "$Alpha" exists
    And I save field "lastUpdateDate" of "/vocabularies/$Alpha" as "first"
    And a second has passed
    And a new version of "$Alpha" is created
    And I save field "creationDate" of "/vocabularies/$Alpha/versions/2" as "second"
    When I send a GET request to "/vocabularies/$Alpha"
    Then the response field "lastUpdateDate" is the saved "second"
    And the response field "lastUpdateDate" is later than the saved "first"

  @D86
  Scenario: The service returns the correlation id it was given
    Given the request header "X-Correlation-Id" is "corr-$RUN"
    When I send a GET request to "/vocabularies?name=$RUN"
    Then the response status is 200
    And the response header "X-Correlation-Id" is "corr-$RUN"

  @D86
  Scenario: The service creates a correlation id when none is sent
    When I send a GET request to "/vocabularies?name=$RUN"
    Then the response header "X-Correlation-Id" is present
