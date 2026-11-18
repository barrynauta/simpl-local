Feature: Read a vocabulary, its version history, one version and its content
  GET /vocabularies/{vocabularyName}, .../versions, .../versions/{version} and the
  content downloads (D5, D25, D26, D29). Version list order: D23.

  Background:
    Given I am authenticated as "ga-admin"

  @D5 @D25
  Scenario: The vocabulary object shows the latest version
    Given a vocabulary "Health" exists with 3 versions
    When I send GET "/vocabularies/${name:Health}"
    Then the response status is 200
    And the response is vocabulary "Health" with latest version 3 and status "ACTIVE"
    And the vocabulary "Health" was created by "ga-admin"

  @D24 @negative
  Scenario: An unknown vocabulary gives 404
    When I send GET "/vocabularies/${name:Missing}"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"
    And the problem detail contains "${name:Missing}"

  @D21 @negative
  Scenario: A vocabulary name that breaks the pattern gives 400
    When I send GET "/vocabularies/lowercase"
    Then the response is a problem with status 400

  @AC_SIMPL-18168_AC1 @D23 @D26 @D27 @D5
  Scenario: The version history lists every version, newest first, with its metadata
    Given a vocabulary "Health" exists with 3 versions
    When I send GET "/vocabularies/${name:Health}/versions"
    Then the response status is 200
    And the version list holds the versions "3, 2, 1" in this order
    And the page fields are page 1, pageSize 20, total 3 and totalPages 1
    And every version in the list belongs to "Health" and has the fields "status, createdBy, changelog, description, creationDate, lastUpdateDate, latestVersionHref"
    And the field "items[2].latestVersionHref" is the href of version 3 of "Health"
    And the field "items[0].latestVersionHref" is the href of version 3 of "Health"

  @AC_SIMPL-18168_AC1 @D16
  Scenario: A viewer can read the version history
    Given a vocabulary "Health" exists with 2 versions
    And I am authenticated as "ga-viewer"
    When I send GET "/vocabularies/${name:Health}/versions"
    Then the response status is 200
    And the version list holds the versions "2, 1" in this order

  @AC_SIMPL-18168_AC2 @D24 @negative
  Scenario: The history of an unknown vocabulary gives 404
    When I send GET "/vocabularies/${name:Missing}/versions"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

  @D23 @D22
  Scenario Outline: The version history sorts by <sort>
    Given a vocabulary "Health" exists with 3 versions
    And version 1 of vocabulary "Health" has status "ACTIVE"
    When I send GET "/vocabularies/${name:Health}/versions?sort=<sort>"
    Then the response status is 200
    And the version list holds the versions "<order>" in this order

    Examples:
      | sort    | order   |
      | name    | 3, 2, 1 |
      | -name   | 3, 2, 1 |
      | status  | 3, 1, 2 |
      | -status | 2, 3, 1 |

  @D23
  Scenario Outline: The version history sorts by <sort>, ties broken by version descending
    Given a vocabulary "Health" exists with 3 versions
    When I send GET "/vocabularies/${name:Health}/versions?sort=<sort>"
    Then the response status is 200
    And the version list is sorted by "<sort>" with ties broken by version descending

    Examples:
      | sort          |
      | creationDate  |
      | -creationDate |

  @D22
  Scenario: The version history is paged
    Given a vocabulary "Health" exists with 3 versions
    When I send GET "/vocabularies/${name:Health}/versions?pageSize=2&page=2"
    Then the response status is 200
    And the version list holds the versions "1" in this order
    And the page fields are page 2, pageSize 2, total 3 and totalPages 2

  @D21 @D22 @D23 @negative
  Scenario Outline: An invalid version history query is rejected (<query>)
    Given a vocabulary "Health" exists
    When I send GET "/vocabularies/${name:Health}/versions?<query>"
    Then the response is a problem with status 400

    Examples:
      | query          |
      | sort=lastUpdated |
      | sort=version   |
      | pageSize=101   |
      | page=0         |
      | status=ACTIVE  |
      | foo=bar        |

  @AC_SIMPL-18166_AC1 @D26 @D27 @D5 @E2
  Scenario: One version is returned with its full metadata
    Given a vocabulary "Health" exists with 2 versions
    When I send GET "/vocabularies/${name:Health}/versions/1"
    Then the response status is 200
    And the response content type is "application/json"
    And the response is version 1 of "Health" with status "DEPRECATED"
    And the field "createdBy" is the email of "ga-admin"
    And the field "contentHref" is "${content:Health@1}"
    And the field "latestVersionHref" is the href of version 2 of "Health"

  @D5 @D39
  Scenario: The latest version links to itself as latest
    Given a vocabulary "Health" exists
    When I send GET "/vocabularies/${name:Health}/versions/1"
    Then the field "latestVersionHref" is the href of version 1 of "Health"

  @AC_SIMPL-18166_AC2 @D24 @negative
  Scenario Outline: A version that does not exist gives 404 (<case>)
    Given a vocabulary "Health" exists
    When I send GET "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | case             | path                                     |
      | unknown version  | /vocabularies/${name:Health}/versions/9  |
      | unknown name     | /vocabularies/${name:Missing}/versions/1 |

  @D21 @negative
  Scenario Outline: A version number that is not a positive integer gives 400 (<version>)
    Given a vocabulary "Health" exists
    When I send GET "/vocabularies/${name:Health}/versions/<version>"
    Then the response is a problem with status 400

    Examples:
      | version |
      | 0       |
      | abc     |
      | -1      |

  @AC_SIMPL-18166_AC1 @D29 @API-36
  Scenario: A version's content is downloaded as stored
    Given a vocabulary "Health" exists with 2 versions
    When I send GET "/content/${name:Health}/versions/1"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response is a download named "${name:Health}-v1.ttl"
    And the response body is the content of version 1 of "Health"

  @D29 @D5
  Scenario: The latest content is the content of the highest version
    Given a vocabulary "Health" exists with 2 versions
    And version 2 of vocabulary "Health" has status "DEPRECATED"
    When I send GET "/content/${name:Health}"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response is a download named "${name:Health}.ttl"
    And the response body is the content of version 2 of "Health"

  @D29 @D16
  Scenario: A viewer can download content
    Given a vocabulary "Health" exists
    And I am authenticated as "ga-viewer"
    When I send GET "/content/${name:Health}/versions/1"
    Then the response status is 200
    And the response body is the content of version 1 of "Health"

  @D24 @negative
  Scenario Outline: Downloading content that does not exist gives 404 (<case>)
    Given a vocabulary "Health" exists
    When I send GET "<path>"
    Then the response is a problem with status 404 and type "urn:problem-type:belgif:resourceNotFound"

    Examples:
      | case            | path                                   |
      | unknown name    | /content/${name:Missing}               |
      | unknown version | /content/${name:Health}/versions/7     |

