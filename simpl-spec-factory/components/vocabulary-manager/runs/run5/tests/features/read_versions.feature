Feature: Retrieve a vocabulary, its version history and its content via the API
  GET .../versions lists the versions newest first (D23). Full details of a version are
  its metadata (JSON) plus the content download (D29).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"

  @AC_SIMPL-18168_AC1 @D23 @D26 @D27 @D22
  Scenario: The version history lists all versions with their metadata, newest first
    Given a vocabulary "{Alpha}" exists with 3 versions
    When I send "GET" to "/vocabularies/{Alpha}/versions"
    Then the response status is 200
    And the response has the paging fields
    And the response field "total" equals 3
    And the response items have the "version" values [3, 2, 1]
    And the response items have the "status" values ["ACTIVE", "DEPRECATED", "DEPRECATED"]
    And the response items have the "changelog" values ["Change 3.", "Change 2.", "Initial version."]
    And every item has the fields "href, contentHref, name, version, description, changelog, status, createdBy, creationDate, lastUpdateDate"
    And the response field "items[2].href" is "/vocabularies/{Alpha}/versions/1"
    And the response field "items[2].contentHref" ends with "/content/{Alpha}/versions/1"
    And the response field "items[2].description" is "Description of {Alpha}."

  @AC_SIMPL-18168_AC1 @D23
  Scenario Outline: The version history can be sorted (<sort>)
    Given a vocabulary "{Alpha}" exists with 3 versions
    When I send "GET" to "/vocabularies/{Alpha}/versions?sort=<sort>"
    Then the response status is 200
    And the response items have the "version" values <order>

    Examples:
      | sort    | order     |
      | status  | [3, 2, 1] |
      | -status | [2, 1, 3] |
      | name    | [3, 2, 1] |
      | -name   | [3, 2, 1] |

  @AC_SIMPL-18168_AC1 @D22 @D23
  Scenario: The version history is paged
    Given a vocabulary "{Alpha}" exists with 3 versions
    When I send "GET" to "/vocabularies/{Alpha}/versions?pageSize=1&page=2"
    Then the response status is 200
    And the response items have the "version" values [2]
    And the response field "total" equals 3
    And the response field "totalPages" equals 3
    And the response has the links ["self", "first", "prev", "next", "last"]

  @D23 @D21 @negative
  Scenario: The version history does not sort by last update
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "/vocabularies/{Alpha}/versions?sort=lastUpdated"
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "sort"

  @AC_SIMPL-18168_AC2 @D24 @D48 @negative
  Scenario: The version history of a vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/{Ghost}/versions"
    Then the response is a problem with status 404
    And the problem has an issue in "path" named "vocabularyName"

  @AC_SIMPL-18166_AC1 @D5 @D26 @D27 @D29
  Scenario: The metadata of one version is returned in JSON
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I send "GET" to "/vocabularies/{Alpha}/versions/1"
    Then the response status is 200
    And the response content type is "application/json"
    And the response field "href" is "/vocabularies/{Alpha}/versions/1"
    And the response field "contentHref" ends with "/content/{Alpha}/versions/1"
    And the response field "latestVersionHref" is "/vocabularies/{Alpha}/versions/2"
    And the response field "name" is "{Alpha}"
    And the response field "version" equals 1
    And the response field "status" is "DEPRECATED"
    And the response field "description" is "Description of {Alpha}."
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" identifies the user with role "GA_VOCABULARY_ADMIN"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second

  @AC_SIMPL-18166_AC1 @D29
  Scenario: The content of one version is downloaded as the uploaded Turtle
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I send "GET" to "/content/{Alpha}/versions/1"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response header "Content-Disposition" contains "attachment"
    And the response header "Content-Disposition" contains "filename="
    And the response header "Content-Disposition" contains "{Alpha}-v1.ttl"
    And the response body is the content of version 1 of "{Alpha}"

  @D29 @D5
  Scenario: The latest content is downloaded under the vocabulary name
    Given a vocabulary "{Alpha}" exists with 2 versions
    And version 2 of "{Alpha}" has status "DEPRECATED"
    When I send "GET" to "/content/{Alpha}"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response header "Content-Disposition" contains "attachment"
    And the response header "Content-Disposition" contains "{Alpha}.ttl"
    And the response body is the content of version 2 of "{Alpha}"

  @AC_SIMPL-18166_AC2 @D24 @D55 @D48 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "/vocabularies/{Alpha}/versions/9"
    Then the response is a problem with status 404
    And the problem detail is "Version 9 of vocabulary '{Alpha}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @negative
  Scenario Outline: Reads of something that does not exist give 404 (<path>)
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                            |
      | /vocabularies/{Ghost}           |
      | /vocabularies/{Ghost}/versions/1 |
      | /content/{Ghost}                |
      | /content/{Ghost}/versions/1     |
      | /content/{Alpha}/versions/2     |

  @D54 @negative
  Scenario Outline: Names in paths are matched exactly (<path>)
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "<path>"
    Then the response is a problem with status 404

    Examples:
      | path                                 |
      | /vocabularies/{upper:Alpha}          |
      | /vocabularies/{upper:Alpha}/versions |
      | /content/{upper:Alpha}               |

  @D49 @negative
  Scenario Outline: An invalid path value on GET gives 400 (<path>)
    When I send "GET" to "<path>"
    Then the response is a problem with status 400
    And the problem has exactly 1 issue
    And the problem has an issue in "path" named "<parameter>"
    And every issue has the type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | path                              | parameter      |
      | /vocabularies/alpha{run}          | vocabularyName |
      | /vocabularies/Ab                  | vocabularyName |
      | /vocabularies/Bad_{run}/versions  | vocabularyName |
      | /vocabularies/Z{run}{chars:54}    | vocabularyName |
      | /vocabularies/{Alpha}/versions/0  | version        |
      | /vocabularies/{Alpha}/versions/x  | version        |
      | /content/alpha{run}               | vocabularyName |
      | /content/{Alpha}/versions/0       | version        |
      | /external-vocabularies/ext{run}   | name           |

  @D16 @AC_SIMPL-18176_AC1
  Scenario: A viewer can read deprecated versions
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I send "GET" to "/vocabularies/{Alpha}/versions/1"
    Then the response status is 200
    And the response field "status" is "DEPRECATED"
    When I send "GET" to "/content/{Alpha}/versions/1"
    Then the response status is 200
    And the response body is the content of version 1 of "{Alpha}"
