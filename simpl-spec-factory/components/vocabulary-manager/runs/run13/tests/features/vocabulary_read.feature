Feature: Read a vocabulary, its versions and its content
  Version metadata D26 and D27, version history D23 and D154, downloads D29 and D70,
  not-found texts D55 and D66, path checks D49, D54 and D91.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D29
  Scenario: A version is returned with its full metadata and its content can be downloaded
    Given a vocabulary "Alpha" exists with 2 versions
    When I send GET "/vocabularies/{Alpha}/versions/1"
    Then the response status is 200
    And the response media type is "application/json"
    And the response JSON has
      | field             | value                                     |
      | href              | /vocabularies/{Alpha}/versions/1          |
      | contentHref       | {base}/content/{Alpha}/versions/1         |
      | latestVersionHref | /vocabularies/{Alpha}/versions/2          |
      | name              | {Alpha}                                   |
      | version           | 1                                         |
      | description       | Description of {Alpha}.                   |
      | changelog         | Initial version.                          |
      | status            | DEPRECATED                                |
      | createdBy         | {admin_email}                             |
      | creationDate      | <date-time>                               |
      | lastUpdateDate    | <date-time>                               |
    When I send GET "/content/{Alpha}/versions/1"
    Then the response status is 200
    And the response body is the uploaded content of version 1 of "Alpha"

  @AC_SIMPL-18166_AC1 @AC_SIMPL-18176_AC1 @D16
  Scenario: The viewer can read a deprecated version
    Given a vocabulary "Alpha" exists with 2 versions
    And I am authenticated as "ga-viewer"
    When I send GET "/vocabularies/{Alpha}/versions/1"
    Then the response status is 200
    And the response JSON has
      | field  | value      |
      | status | DEPRECATED |

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies/{Alpha}/versions/5"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 5 of vocabulary '{Alpha}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send GET "/vocabularies/{Ghost}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{Ghost}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "vocabularyName"

  @D24 @D66 @negative
  Scenario: A vocabulary that does not exist gives 404
    When I send GET "/vocabularies/{Ghost}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{Ghost}' was not found."
    And the problem has exactly one issue
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18168_AC1 @D23 @D26
  Scenario: The version history lists every version, newest first, with its metadata
    Given a vocabulary "Alpha" exists with 3 versions
    When I send GET "/vocabularies/{Alpha}/versions"
    Then the response status is 200
    And the response lists the versions "3, 2, 1" in this order
    And the page fields are page 1, pageSize 20, total 3, totalPages 1
    And the response JSON has
      | field                | value                            |
      | items[0].status      | ACTIVE                           |
      | items[1].status      | DEPRECATED                       |
      | items[2].status      | DEPRECATED                       |
      | items[2].href        | /vocabularies/{Alpha}/versions/1 |
      | items[2].changelog   | Initial version.                 |
      | items[2].description | Description of {Alpha}.          |
      | items[2].createdBy   | {admin_email}                    |
      | items[2].creationDate | <date-time>                     |
      | items[0].changelog   | <present>                        |

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The history of a vocabulary that does not exist gives 404
    When I send GET "/vocabularies/{Ghost}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{Ghost}' was not found."

  @AC_SIMPL-18168_AC1 @D23 @D154 @D128
  Scenario Outline: The version history sorts by <sort>
    Given a vocabulary "Alpha" exists with 3 versions
    When I send GET "/vocabularies/{Alpha}/versions?sort=<sort>"
    Then the response status is 200
    And the response lists the versions "<order>" in this order

    Examples:
      | sort          | order   |
      | name          | 3, 2, 1 |
      | -name         | 3, 2, 1 |
      | status        | 3, 2, 1 |
      | -status       | 2, 1, 3 |
      | -creationDate | 3, 2, 1 |

  @D23
  Scenario: The version history sorts by creationDate with ties broken by version descending
    Given a vocabulary "Alpha" exists with 3 versions
    When I send GET "/vocabularies/{Alpha}/versions?sort=creationDate"
    Then the response status is 200
    And the version items are sorted by "creationDate"

  @D21 @negative
  Scenario Outline: The version history rejects <case>
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies/{Alpha}/versions?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<parameter>"

    Examples:
      | case                     | query            | parameter |
      | a sort it does not offer | sort=lastUpdated | sort      |
      | an unknown parameter     | status=ACTIVE    | status    |
      | pageSize above 100       | pageSize=101     | pageSize  |

  @D22 @D35 @D85 @D111 @BLG-col-paging
  Scenario: The version history is paged
    Given a vocabulary "Alpha" exists with 3 versions
    When I send GET "/vocabularies/{Alpha}/versions?pageSize=2"
    Then the response lists the versions "3, 2" in this order
    And the page fields are page 1, pageSize 2, total 3, totalPages 2
    And the response has the links "self, first, next, last" and no others
    And the link "next" is "/vocabularies/{Alpha}/versions?page=2&pageSize=2"
    When I send GET "/vocabularies/{Alpha}/versions?pageSize=2&page=2"
    Then the response lists the versions "1" in this order

  @D29 @D70 @D5 @API-36
  Scenario: The latest content is downloaded as uploaded, as text/turtle with the file name <Name>.ttl
    Given a vocabulary "Alpha" exists with 2 versions
    When I send GET "/content/{Alpha}"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the download file name is "{Alpha}.ttl"
    And the response body is the uploaded content of version 2 of "Alpha"

  @D29 @D70
  Scenario: A version's content is downloaded with the file name <Name>-v<n>.ttl
    Given a vocabulary "Alpha" exists with 2 versions
    When I send GET "/content/{Alpha}/versions/1"
    Then the response status is 200
    And the response Content-Type is exactly "text/turtle"
    And the download file name is "{Alpha}-v1.ttl"
    And the response body is the uploaded content of version 1 of "Alpha"

  @D29 @D70
  Scenario: Downloads keep the exact bytes: line ends, UTF-8 text and comments
    When I create the vocabulary "Alpha" with the fixture "bytes-exact.ttl"
    Then the response status is 201
    When I send GET "/content/{Alpha}/versions/1"
    Then the response body is the uploaded content of version 1 of "Alpha"
    When I send GET "/content/{Alpha}"
    Then the response body is the uploaded content of version 1 of "Alpha"

  @D24 @D66 @negative
  Scenario Outline: A download of something that does not exist gives 404: <case>
    Given a vocabulary "Alpha" exists
    When I send GET "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "<detail>"

    Examples:
      | case                 | path                          | detail                                            |
      | unknown vocabulary   | /content/{Ghost}              | Vocabulary with name '{Ghost}' was not found.     |
      | unknown version      | /content/{Alpha}/versions/9   | Version 9 of vocabulary '{Alpha}' was not found.  |

  @D49 @D91 @D112 @D116 @negative
  Scenario Outline: An invalid path value gives 400: <case>
    Given a vocabulary "Alpha" exists
    When I send <method> "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue
    And the problem has an issue in "path" named "<parameter>"
    And the issue in "path" named "<parameter>" has the type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | case                       | method | path                                    | parameter      |
      | name in lower case         | GET    | /vocabularies/lower{tok}                | vocabularyName |
      | name of 2 characters       | GET    | /vocabularies/Ab                        | vocabularyName |
      | name with a hyphen         | GET    | /vocabularies/Bad-{tok}                 | vocabularyName |
      | name of 65 characters      | GET    | /vocabularies/Abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij12345 | vocabularyName |
      | version 0                  | GET    | /vocabularies/{Alpha}/versions/0        | version        |
      | version not a number       | GET    | /vocabularies/{Alpha}/versions/abc      | version        |
      | version with a fraction    | GET    | /vocabularies/{Alpha}/versions/1.0      | version        |
      | version with a plus sign   | GET    | /vocabularies/{Alpha}/versions/%2B1     | version        |
      | version above int32        | GET    | /vocabularies/{Alpha}/versions/2147483648 | version      |
      | negative version           | GET    | /vocabularies/{Alpha}/versions/-1       | version        |
      | download name in lower case | GET   | /content/lower{tok}                     | vocabularyName |
      | download version 0         | GET    | /content/{Alpha}/versions/0             | version        |
      | history of an invalid name | GET    | /vocabularies/lower{tok}/versions       | vocabularyName |
      | external name in lower case | GET   | /external-vocabularies/lower{tok}       | name           |
      | external content invalid   | GET    | /external-vocabularies/lower{tok}/content | name         |
      | delete external invalid    | DELETE | /external-vocabularies/lower{tok}       | name           |

  @D49 @negative
  Scenario: A status change on an invalid version number gives 400
    Given a vocabulary "Alpha" exists
    When I send PATCH "/vocabularies/{Alpha}/versions/0" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "version"

  @D49 @negative
  Scenario: A status change on an invalid vocabulary name gives 400
    When I send PATCH "/vocabularies/lower{tok}" with the JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "path" named "vocabularyName"

  @D91 @negative
  Scenario: The largest valid version number is valid but not found
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies/{Alpha}/versions/2147483647"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D54 @negative
  Scenario Outline: Paths match the stored name exactly: <path>
    Given a vocabulary "Alpha" exists
    When I send GET "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | path                                 |
      | /vocabularies/{Alpha:upper}          |
      | /vocabularies/{Alpha:upper}/versions |
      | /content/{Alpha:upper}               |
