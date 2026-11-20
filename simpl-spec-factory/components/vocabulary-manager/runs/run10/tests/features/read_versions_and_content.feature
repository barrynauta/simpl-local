@versions @content
Feature: Read vocabulary versions, the version history and the vocabulary content
  GET .../versions/{version} returns one vocabulary version, GET .../versions the history
  (newest first, D23), and the content operations return the stored Turtle bytes (D29).

  Background:
    Given I use the token of "ga-admin"

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D29
  Scenario: A vocabulary version is returned with its full metadata and its content
    Given a vocabulary "A" exists with 2 versions
    When I send "GET" to "/vocabularies/${A}/versions/1"
    Then the status is 200
    And the media type is "application/json"
    And the response field "href" is "/vocabularies/${A}/versions/1"
    And the response field "contentHref" is "${base}/content/${A}/versions/1"
    And the response field "latestVersionHref" is "/vocabularies/${A}/versions/2"
    And the response field "name" is "${A}"
    And the response field "version" equals 1
    And the response field "description" is "Description of A"
    And the response field "changelog" is "Initial version."
    And the response field "status" is "DEPRECATED"
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "creationDate" is a UTC timestamp
    And the response field "lastUpdateDate" is a UTC timestamp
    When I send "GET" to "/content/${A}/versions/1" with the header "Accept" set to "text/turtle"
    Then the status is 200
    And the body is the file of version 1 of "A"

  @D5
  Scenario: latestVersionHref is filled on the latest version too
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}/versions/1"
    Then the response field "latestVersionHref" is "/vocabularies/${A}/versions/1"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}/versions/3"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 3 of vocabulary '${A}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/${Missing}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @D48 @D24 @D66 @negative
  Scenario: A vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/${Missing}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "vocabularyName"

  @D54 @negative
  Scenario: Reads match the stored name exactly
    Given a vocabulary "Case" exists
    When I send "GET" to "/vocabularies/${upper:Case}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    When I send "GET" to "/content/${upper:Case}" with the header "Accept" set to "text/turtle"
    Then the status is 404

  @D49 @D91 @D112 @negative
  Scenario Outline: An invalid path value on <path> gives 400 with one path issue named <param>
    When I send "GET" to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "<param>"
    And every issue has the type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | path                                     | param          |
      | /vocabularies/lowercase                  | vocabularyName |
      | /vocabularies/Ab                         | vocabularyName |
      | /vocabularies/${long:65}                 | vocabularyName |
      | /vocabularies/lowercase/versions         | vocabularyName |
      | /vocabularies/lowercase/versions/1       | vocabularyName |
      | /vocabularies/Valid1Name/versions/0      | version        |
      | /vocabularies/Valid1Name/versions/abc    | version        |
      | /vocabularies/Valid1Name/versions/2.0    | version        |
      | /vocabularies/Valid1Name/versions/%2B2   | version        |
      | /vocabularies/Valid1Name/versions/2147483648 | version    |
      | /content/lowercase                       | vocabularyName |
      | /content/Valid1Name/versions/0           | version        |
      | /content/Valid1Name/versions/1e1         | version        |

  # ---------------------------------------------------------------- history

  @AC_SIMPL-18168_AC1 @D23 @D26
  Scenario: The version history lists every version, newest first, with its metadata
    Given a vocabulary "A" exists with 3 versions
    When I send "GET" to "/vocabularies/${A}/versions"
    Then the status is 200
    And the response field "total" equals 3
    And the response field "totalPages" equals 1
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the item versions are "3, 2, 1"
    And the response field "items[0].status" is "ACTIVE"
    And the response field "items[1].status" is "DEPRECATED"
    And the response field "items[2].status" is "DEPRECATED"
    And the response field "items[2].changelog" is "Initial version."
    And the response field "items[0].changelog" is "Change 3 of A"
    And the response field "items[0].description" is "Description of A version 3"
    And the response field "items[0].createdBy" is "${email:ga-admin}"
    And the response field "items[0].creationDate" is a UTC timestamp
    And the response field "items[0].lastUpdateDate" is a UTC timestamp
    And the response field "items[2].href" is "/vocabularies/${A}/versions/1"
    And the response field "items[2].contentHref" is "${base}/content/${A}/versions/1"

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The history of a vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/${Missing}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18168_AC1 @D23
  Scenario Outline: The history sorted by <sort>, ties broken by version descending
    # Versions are more than 1 s apart; version 1 is reactivated so the statuses differ.
    Given a vocabulary "A" exists
    And I wait 1.1 seconds
    And vocabulary "A" has a new version with the file "valid"
    And I wait 1.1 seconds
    And vocabulary "A" has a new version with the file "valid"
    And version 1 of vocabulary "A" has the status "ACTIVE"
    When I send "GET" to "/vocabularies/${A}/versions?sort=<sort>"
    Then the status is 200
    And the item versions are "<order>"
    And the version items are sorted by "<sort>"

    Examples:
      | sort          | order   |
      | name          | 3, 2, 1 |
      | -name         | 3, 2, 1 |
      | status        | 3, 1, 2 |
      | -status       | 2, 3, 1 |
      | creationDate  | 1, 2, 3 |
      | -creationDate | 3, 2, 1 |

  @D23 @D21 @negative
  Scenario Outline: An unsupported history sort value <value> gives 400
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}/versions?sort=<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "sort"

    Examples:
      | value       |
      | lastUpdated |
      | version     |

  @D22 @D78 @D111
  Scenario: The history is paged
    Given a vocabulary "A" exists with 3 versions
    When I send "GET" to "/vocabularies/${A}/versions?pageSize=2"
    Then the item versions are "3, 2"
    And the response field "totalPages" equals 2
    And the link "next" has the query parameter "page" with value "2"
    And the link "next" has the query parameter "pageSize" with value "2"
    And the link "next" has the path "/vocabularies/${A}/versions"
    When I send "GET" to "/vocabularies/${A}/versions?pageSize=2&page=2"
    Then the item versions are "1"
    And the link "prev" has the query parameter "page" with value "1"

  @D22 @D21 @negative
  Scenario Outline: A wrong query parameter on the history gives 400: <query>
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}/versions?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query        | param    |
      | pageSize=101 | pageSize |
      | page=0       | page     |
      | status=x     | status   |

  @D21 @negative
  Scenario: A query parameter on a single vocabulary read gives 400
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}?version=1"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 1 issue
    And the problem has an issue in "query" named "version"

  # ---------------------------------------------------------------- content

  @D29 @D5 @D70
  Scenario: Download of the latest version returns its bytes as Turtle with the file name <Name>.ttl
    Given a vocabulary "A" exists with 2 versions
    When I send "GET" to "/content/${A}" with the header "Accept" set to "text/turtle"
    Then the status is 200
    And the content type is exactly "text/turtle"
    And the header "Content-Disposition" names the file "${A}.ttl"
    And the body is the file of version 2 of "A"

  @D29 @D70
  Scenario: Download of a given version returns its bytes with the file name <Name>-v<n>.ttl
    Given a vocabulary "A" exists with 2 versions
    When I send "GET" to "/content/${A}/versions/1" with the header "Accept" set to "text/turtle"
    Then the status is 200
    And the content type is exactly "text/turtle"
    And the header "Content-Disposition" names the file "${A}-v1.ttl"
    And the body is the file of version 1 of "A"

  @D29 @D70
  Scenario: Downloads return the uploaded bytes unchanged
    Given a vocabulary "A" exists with the file "exact_bytes"
    When I send "GET" to "/content/${A}/versions/1" with the header "Accept" set to "*/*"
    Then the status is 200
    And the body is the file of version 1 of "A"

  @D5 @D29
  Scenario: The latest version is the highest number even when it is DEPRECATED
    Given a vocabulary "A" exists with 2 versions
    And version 2 of vocabulary "A" has the status "DEPRECATED"
    And version 1 of vocabulary "A" has the status "ACTIVE"
    When I send "GET" to "/content/${A}" with the header "Accept" set to "text/turtle"
    Then the body is the file of version 2 of "A"
    When I send "GET" to "/vocabularies/${A}"
    Then the response field "version" equals 2

  @D63
  Scenario: A JSON read without an Accept header is served
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}" with the header "Accept" set to ""
    Then the status is 200
    And the media type is "application/json"

  @D63
  Scenario: A content download without an Accept header is served
    Given a vocabulary "A" exists
    When I send "GET" to "/content/${A}" with the header "Accept" set to ""
    Then the status is 200

  @D24 @D66 @negative
  Scenario: Download of a vocabulary that does not exist gives 404
    When I send "GET" to "/content/${Missing}" with the header "Accept" set to "text/turtle"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${Missing}' was not found."

  @D24 @D55 @negative
  Scenario: Download of a version that does not exist gives 404
    Given a vocabulary "A" exists
    When I send "GET" to "/content/${A}/versions/2" with the header "Accept" set to "text/turtle"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 2 of vocabulary '${A}' was not found."

  @D63 @API-37 @negative
  Scenario Outline: A content download with Accept <accept> gives 406
    Given a vocabulary "A" exists
    When I send "GET" to "<path>" with the header "Accept" set to "<accept>"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem has no issues

    Examples:
      | path                     | accept           |
      | /content/${A}            | application/json |
      | /content/${A}/versions/1 | application/xml  |

  @D63 @API-37 @negative
  Scenario: A JSON read with Accept text/turtle gives 406
    Given a vocabulary "A" exists
    When I send "GET" to "/vocabularies/${A}/versions/1" with the header "Accept" set to "text/turtle"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
