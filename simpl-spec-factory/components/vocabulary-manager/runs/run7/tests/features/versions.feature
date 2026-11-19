Feature: Vocabulary versions via the API
  New versions, automatic numbering and deprecation, version reads and downloads
  (SIMPL-18173, SIMPL-18190, SIMPL-18187, SIMPL-18191, SIMPL-18166, SIMPL-18168, D4, D5, D7, D29, D43, D46).

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @AC_SIMPL-18186_AC1 @D4 @D7 @D26 @D27 @D58
  Scenario: A new version gets the next number, becomes ACTIVE and deprecates the older version
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the fields
      | field       | value                         |
      | description | Extended with diagnostics.    |
      | changelog   | Added a diagnostics scheme.   |
    Then the response status is 201
    And the response header "Location" is "/vocabularies/${V:Alpha}/versions/2"
    And the response header "Location" equals the response field "href"
    And the response field "version" equals 2
    And the response field "name" equals "${V:Alpha}"
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Extended with diagnostics."
    And the response field "changelog" equals "Added a diagnostics scheme."
    And the response field "createdBy" equals "${EMAIL:ga-admin}"
    And the response field "contentHref" equals "${CB}/content/${V:Alpha}/versions/2"
    And the response has no validation warnings
    And I save the response field "creationDate" as "v2created"
    And version 1 of "Alpha" has status "DEPRECATED"
    And the field "lastUpdateDate" of version 1 of "Alpha" matches the saved "v2created"

  @D3 @D25 @D28 @D5
  Scenario: The vocabulary shows the latest version's number and description, and keeps its status
    Given vocabulary "Alpha" exists
    And I save the field "creationDate" of vocabulary "Alpha" as "created"
    And one second passes
    When "ga-admin" creates a new version of "Alpha" with the fields
      | field       | value                   |
      | description | Second description.     |
    Then the response status is 201
    And I save the response field "creationDate" as "v2created"
    When "ga-admin" gets vocabulary "Alpha"
    Then the response field "version" equals 2
    And the response field "description" equals "Second description."
    And the response field "status" equals "ACTIVE"
    And the response field "creationDate" matches the saved "created"
    And the response field "lastUpdateDate" matches the saved "v2created"

  @AC_SIMPL-18191_AC1 @D4
  Scenario: Every older ACTIVE version is deprecated, also after a manual reactivation
    Given vocabulary "Alpha" has 3 versions
    And version 1 of "Alpha" has status "ACTIVE"
    And version 3 of "Alpha" has status "ACTIVE"
    When "ga-admin" creates a new version of "Alpha"
    Then the response status is 201
    And the response field "version" equals 4
    And versions "1, 2, 3" of "Alpha" have status "DEPRECATED"
    And version 4 of "Alpha" has status "ACTIVE"

  @D3
  Scenario: A DEPRECATED vocabulary may get a new version and stays DEPRECATED
    Given vocabulary "Alpha" exists
    And vocabulary "Alpha" has status "DEPRECATED"
    When "ga-admin" creates a new version of "Alpha"
    Then the response status is 201
    And the response field "status" equals "ACTIVE"
    And vocabulary "Alpha" has status "DEPRECATED"
    And version 1 of "Alpha" has status "DEPRECATED"

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario: A version form field is rejected and no version is created
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the fields
      | field   | value |
      | version | 7     |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "version"
    And vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A new version with <case> is rejected with validation feedback
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the fields
      | field   | value   |
      | <field> | <value> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has status "ACTIVE"

    Examples:
      | case                         | field          | value       |
      | no change log                | changelog      | <omit>      |
      | a blank change log           | changelog      | ${R: :3}    |
      | a change log of 1001 chars   | changelog      | ${R:c:1001} |
      | no description               | description    | <omit>      |
      | a description of 1001 chars  | description    | ${R:d:1001} |
      | no file                      | vocabularyFile | <omit>      |
      | an unknown field             | colour         | blue        |

  @D37 @Q2 @NFR-04
  Scenario: A change log of 1000 characters is accepted on a new version
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the fields
      | field     | value       |
      | changelog | ${R:c:1000} |
    Then the response status is 201
    And the response field "changelog" equals "${R:c:1000}"

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative
  Scenario: A new version for a vocabulary that does not exist is rejected with 404
    When "ga-admin" creates a new version of "Unknown"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${V:Unknown}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And no vocabulary named "Unknown" is stored

  @D51 @negative
  Scenario: A malformed new version request to an unknown name gives 400, not 404
    When "ga-admin" creates a new version of "Unknown" with the fields
      | field     | value  |
      | changelog | <omit> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38
  Scenario: A new version request that is not multipart/form-data is rejected with 415
    Given vocabulary "Alpha" exists
    When "ga-admin" sends POST "/vocabularies/${V:Alpha}/versions" with JSON
      """
      {"description": "As JSON", "changelog": "As JSON", "vocabularyFile": "@prefix ex: <http://example.org/x#> . ex:a ex:b ex:c ."}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "Alpha" has 1 version

  @D15 @Q2 @NFR-04 @negative
  Scenario: A new version with a file over 5 MB is rejected with 413
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with a valid file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And vocabulary "Alpha" has 1 version

  @D15 @D71 @Q1 @Q2 @NFR-04 @slow
  Scenario: A new version with a file of exactly 5 MB is created within 30 seconds
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with a valid file of 5242880 bytes
    Then the response status is 201
    And the request took at most 30 seconds
    And version 1 of "Alpha" has status "DEPRECATED"

  @D46 @D8 @negative
  Scenario: Resubmitting the latest version unchanged is refused with 409 and deprecates nothing
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the same content and description as version 1
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '${V:Alpha}' already has this content and description."
    And vocabulary "Alpha" has 1 version
    And version 1 of "Alpha" has status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description and ignores the change log
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the same content and description as version 1
      | field       | value                  |
      | description | <description>          |
      | changelog   | A different change log. |
    Then the response status is 409
    When "ga-admin" creates a new version of "Alpha" with the same content and description as version 1
      | field       | value              |
      | description | "  <description>  "  |
    Then the response status is 409
    And vocabulary "Alpha" has 1 version

  @D46 @D33
  Scenario: A change of the description only creates a new version
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the same content as version 1 and a new description
    Then the response status is 201
    And the response field "version" equals 2

  @D46
  Scenario: The identical-version check compares with the latest version only
    Given vocabulary "Alpha" has 2 versions
    When "ga-admin" creates a new version of "Alpha" with the same content and description as version 1
    Then the response status is 201
    And the response field "version" equals 3

  @D43 @D4
  Scenario: Five overlapping new versions are serialised and all succeed
    Given vocabulary "Alpha" exists
    When "ga-admin" creates 5 new versions of "Alpha" in parallel
    Then the parallel response statuses are "201, 201, 201, 201, 201"
    And the parallel responses carry the version numbers 2 to 6, each once
    And the parallel responses carry distinct Location headers
    And vocabulary "Alpha" has 6 versions
    And version 6 of "Alpha" has status "ACTIVE"
    And versions "1, 2, 3, 4, 5" of "Alpha" have status "DEPRECATED"

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D29 @D58
  Scenario: The full metadata of a version is returned, with the latest version link
    Given vocabulary "Alpha" has 2 versions
    When "ga-viewer" gets version 1 of "Alpha"
    Then the response status is 200
    And the response media type is "application/json"
    And the response field "href" equals "/vocabularies/${V:Alpha}/versions/1"
    And the response field "contentHref" equals "${CB}/content/${V:Alpha}/versions/1"
    And the response field "latestVersionHref" equals "/vocabularies/${V:Alpha}/versions/2"
    And the response field "name" equals "${V:Alpha}"
    And the response field "version" equals 1
    And the response field "status" equals "DEPRECATED"
    And the response field "changelog" equals "Initial version."
    And the response field "createdBy" equals "${EMAIL:ga-admin}"
    And the response fields "description, creationDate, lastUpdateDate" are present
    And the response field "creationDate" is a UTC timestamp to the second

  @D5
  Scenario: The latest version carries a link to itself as the latest version
    Given vocabulary "Alpha" has 2 versions
    When "ga-admin" gets version 2 of "Alpha"
    Then the response status is 200
    And the response field "latestVersionHref" equals "/vocabularies/${V:Alpha}/versions/2"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given vocabulary "Alpha" exists
    When "ga-admin" gets version 3 of "Alpha"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 3 of vocabulary '${V:Alpha}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When "ga-admin" gets version 1 of "Unknown"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${V:Unknown}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D49 @negative
  Scenario Outline: A version path value that is not a positive integer gives 400 (<value>)
    Given vocabulary "Alpha" exists
    When "ga-admin" sends <method> "/<root>/${V:Alpha}/versions/<value>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem issues are exactly
      | type                                                     | in   | name    |
      | urn:problem-type:belgif:input-validation:schemaViolation | path | version |

    Examples:
      | method | root         | value |
      | GET    | vocabularies | 0     |
      | GET    | vocabularies | abc   |
      | GET    | vocabularies | -1    |
      | GET    | content      | 0     |
      | GET    | content      | abc   |

  @D49 @negative
  Scenario: A PATCH on a version path value that is not a positive integer gives 400
    Given vocabulary "Alpha" exists
    When "ga-admin" sends PATCH "/vocabularies/${V:Alpha}/versions/abc" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "version"

  @AC_SIMPL-18168_AC1 @D23 @D26 @D35
  Scenario: The version history lists all versions newest first with their metadata
    Given vocabulary "Alpha" has 3 versions
    When "ga-viewer" lists the versions of "Alpha"
    Then the response status is 200
    And the response field "total" equals 3
    And the response field "page" equals 1
    And the response field "pageSize" equals 20
    And the response field "totalPages" equals 1
    And the item versions are "3, 2, 1"
    And every item has the fields "href, contentHref, name, version, description, changelog, status, creationDate, lastUpdateDate, createdBy"
    And the response field "items[0].status" equals "ACTIVE"
    And the response field "items[1].status" equals "DEPRECATED"
    And the response field "items[2].status" equals "DEPRECATED"
    And the response field "items[2].changelog" equals "Initial version."

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The version history of a vocabulary that does not exist gives 404
    When "ga-admin" lists the versions of "Unknown"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${V:Unknown}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D22 @D23 @D78 @BLG-col-paging
  Scenario: The version history is paged and keeps the newest-first order
    Given vocabulary "Alpha" has 3 versions
    When "ga-admin" lists the versions of "Alpha" with the query "pageSize=2"
    Then the response status is 200
    And the item versions are "3, 2"
    And the response field "totalPages" equals 2
    And the link "next" points to page 2 with the request query
    And the link "last" points to page 2 with the request query
    And the link "prev" is absent
    When "ga-admin" lists the versions of "Alpha" with the query "pageSize=2&page=2"
    Then the item versions are "1"
    And the link "prev" points to page 1 with the request query
    And the link "next" is absent

  @D23
  Scenario Outline: The version history sorts on <sort> with ties broken by version descending
    Given vocabulary "Alpha" has 3 versions, created one second apart
    And version 3 of "Alpha" has status "DEPRECATED"
    And version 1 of "Alpha" has status "ACTIVE"
    When "ga-admin" lists the versions of "Alpha" with the query "sort=<sort>"
    Then the response status is 200
    And the item versions are "<order>"

    Examples:
      | sort          | order   |
      | name          | 3, 2, 1 |
      | -name         | 3, 2, 1 |
      | status        | 1, 3, 2 |
      | -status       | 3, 2, 1 |
      | creationDate  | 1, 2, 3 |
      | -creationDate | 3, 2, 1 |

  @D21 @D23 @negative
  Scenario Outline: The version history rejects an invalid query (<query>)
    Given vocabulary "Alpha" exists
    When "ga-admin" lists the versions of "Alpha" with the query "<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<name>"

    Examples:
      | query            | name     |
      | sort=lastUpdated | sort     |
      | sort=version     | sort     |
      | status=ACTIVE    | status   |
      | pageSize=101     | pageSize |
      | page=0           | page     |

  @AC_SIMPL-18166_AC1 @D29 @D5 @D70
  Scenario: The latest content is downloaded as the stored Turtle bytes
    Given vocabulary "Alpha" has 2 versions
    When "ga-viewer" downloads the latest content of "Alpha"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the download file name is "${V:Alpha}.ttl"
    And the response body is the content sent for version 2 of "Alpha"

  @AC_SIMPL-18166_AC1 @D29 @D70
  Scenario: A specific version's content is downloaded as the stored Turtle bytes
    Given vocabulary "Alpha" has 2 versions
    When "ga-viewer" downloads version 1 of "Alpha"
    Then the response status is 200
    And the response content type is exactly "text/turtle"
    And the download file name is "${V:Alpha}-v1.ttl"
    And the response body is the content sent for version 1 of "Alpha"

  @D24 @D55 @D66 @negative
  Scenario: Downloads of unknown vocabularies or versions give 404
    Given vocabulary "Alpha" exists
    When "ga-admin" downloads version 2 of "Alpha"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 2 of vocabulary '${V:Alpha}' was not found."
    When "ga-admin" downloads the latest content of "Unknown"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${V:Unknown}' was not found."

  @D4 @D41
  Scenario: The status of one version is changed by hand and nothing else changes
    Given vocabulary "Alpha" has 2 versions
    And I save the field "lastUpdateDate" of version 2 of "Alpha" as "before"
    And one second passes
    When "ga-admin" sets the status of version 2 of "Alpha" to "DEPRECATED"
    Then the response status is 200
    And the response field "status" equals "DEPRECATED"
    And the response field "version" equals 2
    And the response field "lastUpdateDate" is later than the saved "before"
    And vocabulary "Alpha" has status "ACTIVE"
    And version 1 of "Alpha" has status "DEPRECATED"
    When "ga-admin" sets the status of version 1 of "Alpha" to "ACTIVE"
    Then the response status is 200
    And version 1 of "Alpha" has status "ACTIVE"
    And version 2 of "Alpha" has status "DEPRECATED"

  @D4 @D41 @negative
  Scenario: Asking a version for its current status gives 409
    Given vocabulary "Alpha" has 2 versions
    When "ga-admin" sets the status of version 1 of "Alpha" to "DEPRECATED"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary version is already in status DEPRECATED"

  @D4 @D41 @negative
  Scenario Outline: An unsupported version status is rejected with 400 (<body>)
    Given vocabulary "Alpha" exists
    When "ga-admin" sends PATCH "/vocabularies/${V:Alpha}/versions/1" with JSON
      """
      <body>
      """
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And version 1 of "Alpha" has status "ACTIVE"

    Examples:
      | body                                  |
      | {"status": "RETIRED"}                 |
      | {"status": "active"}                  |
      | {}                                    |
      | {"status": "DEPRECATED", "note": "x"} |

  @D41 @D55 @negative
  Scenario: A status change of a version that does not exist gives 404
    Given vocabulary "Alpha" exists
    When "ga-admin" sets the status of version 4 of "Alpha" to "DEPRECATED"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 4 of vocabulary '${V:Alpha}' was not found."

  @D25 @D4
  Scenario: A manual version status change updates the vocabulary's last update date
    Given vocabulary "Alpha" has 2 versions
    And I save the field "lastUpdateDate" of vocabulary "Alpha" as "before"
    And one second passes
    When "ga-admin" sets the status of version 1 of "Alpha" to "ACTIVE"
    Then the response status is 200
    When "ga-admin" gets vocabulary "Alpha"
    Then the response field "lastUpdateDate" is later than the saved "before"

  @D46 @D51 @negative
  Scenario: The identical-version check comes after the field checks
    Given vocabulary "Alpha" exists
    When "ga-admin" creates a new version of "Alpha" with the same content and description as version 1
      | field     | value   |
      | changelog | <empty> |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"
    And vocabulary "Alpha" has 1 version

  @D46 @D51 @negative
  Scenario: The identical-version check comes after the 404
    Given vocabulary "Alpha" exists
    When "ga-admin" sends the content of version 1 of "Alpha" as a new version of "Unknown"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '${V:Unknown}' was not found."
