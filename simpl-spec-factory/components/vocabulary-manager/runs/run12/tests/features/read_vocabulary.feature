Feature: Read a vocabulary, its versions and its content
  GET /vocabularies/{vocabularyName}, .../versions, .../versions/{version}, /content/... (SIMPL-18166, SIMPL-18168, D5, D23, D29, D49, D54, D66).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18168_AC1 @D23 @D26 @D27 @D5 @D35
  Scenario: The version history lists all versions newest first with their metadata
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 2 more versions
    When I send "GET" to "/vocabularies/{{Health}}/versions"
    Then the status is 200
    And the item versions are "3, 2, 1"
    And every item has the fields "href, contentHref, latestVersionHref, name, version, description, changelog, status, createdBy, creationDate, lastUpdateDate"
    And every item has "latestVersionHref" equal to "/vocabularies/{{Health}}/versions/3"
    And the response field "items.2.changelog" equals "Initial version."
    And the response field "items.2.status" equals "DEPRECATED"
    And the response field "items.0.status" equals "ACTIVE"
    And the response field "total" equals 3

  @AC_SIMPL-18168_AC2 @D24 @D66 @negative
  Scenario: The version history of a vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/{{Missing}}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D23 @D154
  Scenario: Version list sort values sort on their field with ties broken by version descending
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 2 more versions created one second apart
    When I send "GET" to "/vocabularies/{{Health}}/versions?sort=creationDate"
    Then the item versions are "1, 2, 3"
    When I send "GET" to "/vocabularies/{{Health}}/versions?sort=-creationDate"
    Then the item versions are "3, 2, 1"
    When I send "GET" to "/vocabularies/{{Health}}/versions?sort=status"
    Then the item versions are "3, 2, 1"
    When I send "GET" to "/vocabularies/{{Health}}/versions?sort=-status"
    Then the item versions are "2, 1, 3"
    When I send "GET" to "/vocabularies/{{Health}}/versions?sort=name"
    Then the item versions are "3, 2, 1"
    When I send "GET" to "/vocabularies/{{Health}}/versions?sort=-name"
    Then the item versions are "3, 2, 1"

  @D22 @D78 @D111
  Scenario: The version history is paged
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 2 more versions
    When I send "GET" to "/vocabularies/{{Health}}/versions?pageSize=2&page=2"
    Then the item versions are "1"
    And the response field "total" equals 3
    And the response field "totalPages" equals 2
    And the response field "self" equals "/vocabularies/{{Health}}/versions?page=2&pageSize=2"
    And the response field "prev" equals "/vocabularies/{{Health}}/versions?page=1&pageSize=2"

  @D21 @D23 @negative
  Scenario Outline: An invalid query on the version history gives 400 (<query>)
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies/{{Health}}/versions?<query>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "<param>"

    Examples:
      | query           | param    |
      | sort=lastUpdated| sort     |
      | sort=version    | sort     |
      | status=ACTIVE   | status   |
      | pageSize=101    | pageSize |
      | page=0          | page     |

  @D162 @negative
  Scenario: A query error on an unknown vocabulary gives 400, not 404
    When I send "GET" to "/vocabularies/{{Missing}}/versions?pageSize=0"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @D29
  Scenario: The full details of a version are its metadata and its content
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the status is 200
    And the response media type is "application/json"
    And the response field "href" equals "/vocabularies/{{Health}}/versions/1"
    And the response field "contentHref" is the contentBase followed by "/content/{{Health}}/versions/1"
    And the response field "latestVersionHref" equals "/vocabularies/{{Health}}/versions/2"
    And the response field "name" equals "{{Health}}"
    And the response field "version" equals 1
    And the response field "status" equals "DEPRECATED"
    And the response field "changelog" equals "Initial version."
    And the response field "createdBy" equals "{{adminEmail}}"
    And the response field "creationDate" is a UTC timestamp
    And the response field "lastUpdateDate" is a UTC timestamp
    When I send "GET" to "/content/{{Health}}/versions/1"
    Then the status is 200
    And the response body is the content uploaded for version 1 of "{{Health}}"

  @D5
  Scenario: The latest version links to itself as latest version
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the response field "latestVersionHref" equals "/vocabularies/{{Health}}/versions/1"

  @AC_SIMPL-18166_AC2 @D24 @D55 @negative
  Scenario: A version that does not exist gives 404
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies/{{Health}}/versions/9"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 9 of vocabulary '{{Health}}' was not found."

  @AC_SIMPL-18166_AC2 @D24 @D66 @negative
  Scenario: A version of a vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/{{Missing}}/versions/1"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."

  @D49 @D91 @D112 @D116 @negative
  Scenario Outline: An invalid version in the path gives 400 for the path (<version>)
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies/{{Health}}/versions/<version>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "version"
    And issue 0 has type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | version    |
      | 0          |
      | -1         |
      | abc        |
      | 2.0        |
      | 1e1        |
      | 2147483648 |

  @D49 @negative
  Scenario Outline: An invalid vocabulary name in the path gives 400 for the path (<path>)
    When I send "GET" to "<path>"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "path" named "vocabularyName"
    And issue 0 has type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | path                              |
      | /vocabularies/lowercase           |
      | /vocabularies/Ab                  |
      | /vocabularies/Bad-Name            |
      | /vocabularies/bad/versions        |
      | /vocabularies/bad/versions/1      |
      | /content/bad                      |
      | /content/bad/versions/1           |
      | /vocabularies/{{Long65}}          |

  @D24 @D66 @negative
  Scenario: A vocabulary that does not exist gives 404
    When I send "GET" to "/vocabularies/{{Missing}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."
    And the problem has exactly one issue, in "path" named "vocabularyName"

  @D54 @negative
  Scenario: Paths match the stored name exactly
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies/{{upper:Health}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D52 @negative
  Scenario: A query parameter on GET of one vocabulary is rejected
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/vocabularies/{{Health}}?expand=versions"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "expand"

  @AC_SIMPL-18166_AC1 @D29 @D70 @D5
  Scenario: The latest content is downloaded as uploaded with the name as file name
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I send "GET" to "/content/{{Health}}"
    Then the status is 200
    And the response header "Content-Type" equals "text/turtle"
    And the response header "Content-Disposition" contains "attachment"
    And the response header "Content-Disposition" contains "{{Health}}.ttl"
    And the response body is the content uploaded for version 2 of "{{Health}}"

  @AC_SIMPL-18166_AC1 @D29 @D70
  Scenario: A version's content is downloaded with the version in the file name
    Given a vocabulary "{{Health}}" exists
    And vocabulary "{{Health}}" has 1 more versions
    When I send "GET" to "/content/{{Health}}/versions/1"
    Then the status is 200
    And the response header "Content-Type" equals "text/turtle"
    And the response header "Content-Disposition" contains "{{Health}}-v1.ttl"
    And the response body is the content uploaded for version 1 of "{{Health}}"

  @D29 @D24 @negative
  Scenario: Content of a vocabulary or version that does not exist gives 404
    Given a vocabulary "{{Health}}" exists
    When I send "GET" to "/content/{{Missing}}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '{{Missing}}' was not found."
    When I send "GET" to "/content/{{Health}}/versions/4"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Version 4 of vocabulary '{{Health}}' was not found."

  @D63 @negative @API-37
  Scenario: A content download with Accept application/json gives 406
    Given a vocabulary "{{Health}}" exists
    And the request header "Accept" is "application/json"
    When I send "GET" to "/content/{{Health}}"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D63 @D127
  Scenario Outline: A content download accepts <accept>
    Given a vocabulary "{{Health}}" exists
    And the request header "Accept" is "<accept>"
    When I send "GET" to "/content/{{Health}}/versions/1"
    Then the status is 200
    And the response header "Content-Type" equals "text/turtle"

    Examples:
      | accept      |
      | text/turtle |
      | */*         |

  @D63 @negative
  Scenario: A JSON read with Accept text/turtle gives 406
    Given a vocabulary "{{Health}}" exists
    And the request header "Accept" is "text/turtle"
    When I send "GET" to "/vocabularies/{{Health}}"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D63
  Scenario: A JSON read without an Accept header is served with application/json
    Given a vocabulary "{{Health}}" exists
    And the request has no Accept header
    When I send "GET" to "/vocabularies/{{Health}}"
    Then the status is 200
    And the response media type is "application/json"

  @D63
  Scenario: A content download without an Accept header is served as text/turtle
    Given a vocabulary "{{Health}}" exists
    And the request has no Accept header
    When I send "GET" to "/content/{{Health}}"
    Then the status is 200
    And the response header "Content-Type" equals "text/turtle"
