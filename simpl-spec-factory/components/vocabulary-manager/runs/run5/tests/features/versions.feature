Feature: Create a new version of a vocabulary via the API
  POST /vocabularies/{vocabularyName}/versions assigns the next version number, sets the
  new version ACTIVE and every older ACTIVE version DEPRECATED in one transaction (D4, D7).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @AC_SIMPL-18189_AC1 @D4 @D7 @D10 @D26 @D27 @API-12
  Scenario: A new version gets the next number, becomes ACTIVE and deprecates the previous one
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value                       |
      | description    | Second description {run}.   |
      | changelog      | Added a concept.            |
      | vocabularyFile | fixture:cpsv-like.ttl       |
    Then the response status is 201
    And the response header "Location" is "/vocabularies/{Alpha}/versions/2"
    And the response field "href" is "/vocabularies/{Alpha}/versions/2"
    And the response field "contentHref" ends with "/content/{Alpha}/versions/2"
    And the response field "name" is "{Alpha}"
    And the response field "version" equals 2
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Second description {run}."
    And the response field "changelog" is "Added a concept."
    And the response field "createdBy" identifies the user with role "GA_VOCABULARY_ADMIN"
    And the response field "creationDate" is a UTC time to the second
    And the response field "validationWarnings" equals []
    And version 1 of "{Alpha}" has status "DEPRECATED"
    And version 2 of "{Alpha}" has status "ACTIVE"
    And the vocabulary "{Alpha}" has 2 versions

  @AC_SIMPL-18173_AC1 @D4 @D25 @D28
  Scenario: Creating a version updates the vocabulary's latest values and dates
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "/vocabularies/{Alpha}"
    And I remember the response field "creationDate" as "created"
    And I create a new version of "{Alpha}" with the form
      | field          | value                     |
      | description    | Latest description {run}. |
      | changelog      | A change.                 |
      | vocabularyFile | fixture:cpsv-like.ttl     |
    Then the response status is 201
    And the vocabulary "{Alpha}" has the field "version" equal to 2
    And the vocabulary "{Alpha}" has the field "description" equal to "Latest description {run}."
    And the vocabulary "{Alpha}" has the field "creationDate" equal to "{saved:created}"
    And the "lastUpdateDate" of the vocabulary "{Alpha}" equals the "creationDate" of version 2
    And the "lastUpdateDate" of version 1 of "{Alpha}" equals the "creationDate" of version 2
    And version 1 of "{Alpha}" has the field "latestVersionHref" equal to "/vocabularies/{Alpha}/versions/2"
    And version 2 of "{Alpha}" has the field "latestVersionHref" equal to "/vocabularies/{Alpha}/versions/2"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @D4 @D7
  Scenario: A third version deprecates all previous versions
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I create a new version of "{Alpha}"
    Then the response status is 201
    And the response field "version" equals 3
    And version 1 of "{Alpha}" has status "DEPRECATED"
    And version 2 of "{Alpha}" has status "DEPRECATED"
    And version 3 of "{Alpha}" has status "ACTIVE"

  @D4 @D41
  Scenario: After a manual reactivation the next version deprecates every ACTIVE version
    Given a vocabulary "{Alpha}" exists with 2 versions
    And version 1 of "{Alpha}" has status "ACTIVE"
    When I create a new version of "{Alpha}"
    Then the response status is 201
    And the response field "version" equals 3
    And version 1 of "{Alpha}" has status "DEPRECATED"
    And version 2 of "{Alpha}" has status "DEPRECATED"
    And version 3 of "{Alpha}" has status "ACTIVE"

  @D7
  Scenario: The number is the highest existing number plus one, also when the latest version is deprecated
    Given a vocabulary "{Alpha}" exists with 2 versions
    And version 2 of "{Alpha}" has status "DEPRECATED"
    When I create a new version of "{Alpha}"
    Then the response status is 201
    And the response field "version" equals 3

  @D3
  Scenario: A new version of a DEPRECATED vocabulary is allowed and the vocabulary stays DEPRECATED
    Given a vocabulary "{Alpha}" exists
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    When I create a new version of "{Alpha}"
    Then the response status is 201
    And the response field "status" is "ACTIVE"
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: Invalid or incomplete version data is rejected with feedback (<case>)
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value         |
      | description    | <description> |
      | changelog      | <changelog>   |
      | vocabularyFile | default       |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

    Examples:
      | case                      | description    | changelog    | field       |
      | description empty         |                | A change.    | description |
      | description blank         | {blank}        | A change.    | description |
      | description of 1001 chars | {chars:1001}   | A change.    | description |
      | changelog empty           | Changed.       |              | changelog   |
      | changelog blank           | Changed.       | {blank}      | changelog   |
      | changelog of 1001 chars   | Changed.       | {chars:1001} | changelog   |

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A version request without a required field is rejected (<field> missing)
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field   | value  |
      | <keep1> | <val1> |
      | <keep2> | <val2> |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And the vocabulary "{Alpha}" has 1 version

    Examples:
      | field          | keep1       | val1     | keep2          | val2      |
      | description    | changelog   | Changed. | vocabularyFile | default   |
      | changelog      | description | Changed. | vocabularyFile | default   |
      | vocabularyFile | description | Changed. | changelog      | A change. |

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario Outline: A manual version number is rejected and no version is created (version <value>)
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value     |
      | description    | Changed.  |
      | changelog      | A change. |
      | version        | <value>   |
      | vocabularyFile | default   |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "version"
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

    Examples:
      | value |
      | 2     |
      | 7     |

  @AC_SIMPL-18173_AC3 @D24 @D41 @D48 @negative
  Scenario: A version for a vocabulary that does not exist is rejected with 404
    When I create a new version of "{Ghost}"
    Then the response is a problem with status 404
    And the problem has an issue in "path" named "vocabularyName"
    And no vocabulary named "{Ghost}" is stored

  @AC_SIMPL-18173_AC4 @D16 @D17 @D46 @D8 @negative @OWASP-02
  Scenario Outline: A user without GA_VOCABULARY_ADMIN cannot create a version (<who>)
    Given a vocabulary "{Alpha}" exists
    And <who step>
    When I create a new version of "{Alpha}"
    Then the response is a problem with status 403
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

    Examples:
      | who     | who step                                                               |
      | viewer  | I am a Governance Authority user with role "GA_VOCABULARY_VIEWER"      |
      | no role | I am an authenticated user with no vocabulary management role          |

  @AC_SIMPL-18173_AC5 @D17 @D8 @negative @OWASP-08
  Scenario: An unauthenticated version request is rejected with 401
    Given a vocabulary "{Alpha}" exists
    And I am not authenticated
    When I create a new version of "{Alpha}"
    Then the response is a problem with status 401
    And the vocabulary "{Alpha}" has 1 version

  @AC_SIMPL-18173_AC6 @D15 @D8 @negative @API-38
  Scenario Outline: A version request in an unsupported format is rejected with 415 (<content type>)
    Given a vocabulary "{Alpha}" exists
    When I send "POST" to "/vocabularies/{Alpha}/versions" with the body "<body>" as "<content type>"
    Then the response is a problem with status 415
    And the vocabulary "{Alpha}" has 1 version

    Examples:
      | content type     | body                                         |
      | application/json | {}                                           |
      | text/turtle      | @prefix ex: <https://vocab.example.org/x#> . |

  @AC_SIMPL-18173_AC2 @D15 @D8 @negative
  Scenario: A version file above 5 MB gets 413 and changes nothing
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value        |
      | description    | Too large.   |
      | changelog      | A change.    |
      | vocabularyFile | size:5242881 |
    Then the response is a problem with status 413
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

  @D46 @D8 @D48 @negative @API-46
  Scenario: Resubmitting the latest version unchanged is refused and deprecates nothing
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value                   |
      | description    | Description of {Alpha}. |
      | changelog      | A different changelog.  |
      | vocabularyFile | version:1               |
    Then the response is a problem with status 409
    And the problem detail is "Version 1 of vocabulary '{Alpha}' already has this content and description."
    And the vocabulary "{Alpha}" has 1 version
    And version 1 of "{Alpha}" has status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value                       |
      | description    | {blank}Description of {Alpha}.{blank} |
      | changelog      | A change.                   |
      | vocabularyFile | version:1                   |
    Then the response is a problem with status 409

  @D46
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "{Alpha}" exists with 2 versions
    When I create a new version of "{Alpha}" with the form
      | field          | value                   |
      | description    | Description of {Alpha}. |
      | changelog      | Back to the first.      |
      | vocabularyFile | version:1               |
    Then the response status is 201
    And the response field "version" equals 3

  @D46 @D33
  Scenario: A change of the description only creates a new version
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value                     |
      | description    | A better description.     |
      | changelog      | Description only.         |
      | vocabularyFile | version:1                 |
    Then the response status is 201
    And the response field "version" equals 2
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @D46
  Scenario: A change of the content only creates a new version
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value                   |
      | description    | Description of {Alpha}. |
      | changelog      | Content only.           |
      | vocabularyFile | fixture:cpsv-like.ttl   |
    Then the response status is 201
    And the response field "version" equals 2

  @D51 @D46 @negative
  Scenario: Field checks run before the identical-version check (400 before 409)
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value                   |
      | description    | Description of {Alpha}. |
      | vocabularyFile | version:1               |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "changelog"

  @D51 @negative
  Scenario: A malformed request to an unknown vocabulary gives 400, not 404
    When I create a new version of "{Ghost}" with the form
      | field          | value     |
      | changelog      | A change. |
      | vocabularyFile | default   |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "description"

  @D51 @negative
  Scenario: Size is checked before existence (413 before 404)
    When I create a new version of "{Ghost}" with the form
      | field          | value        |
      | description    | Too large.   |
      | changelog      | A change.    |
      | vocabularyFile | size:5242881 |
    Then the response is a problem with status 413

  @D51 @negative
  Scenario: Media type is checked before existence (415 before 404)
    When I send "POST" to "/vocabularies/{Ghost}/versions" with the body "{}" as "application/json"
    Then the response is a problem with status 415

  @D51 @D46 @negative
  Scenario: An unchanged resubmission is refused even when the content gives warnings
    Given a vocabulary "{Alpha}" exists with content
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix unreg: <https://unregistered-{run}.example.org/terms/> .
      @prefix ex: <https://vocab.example.org/{run}/warn#> .
      ex:Scheme a skos:ConceptScheme ;
        skos:prefLabel "Scheme"@en ;
        unreg:creator "Somebody" .
      """
    When I create a new version of "{Alpha}" with the form
      | field          | value                   |
      | description    | Description of {Alpha}. |
      | changelog      | Again.                  |
      | vocabularyFile | version:1               |
    Then the response is a problem with status 409

  @D52 @D8 @negative @BLG-req-valid
  Scenario: An unknown form field on a version request is rejected
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" with the form
      | field          | value     |
      | description    | Changed.  |
      | changelog      | A change. |
      | name           | {Other}   |
      | vocabularyFile | default   |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "name"
    And the vocabulary "{Alpha}" has 1 version

  @D52 @negative
  Scenario: A query parameter on a version request is rejected
    Given a vocabulary "{Alpha}" exists
    When I send "POST" to "/vocabularies/{Alpha}/versions?version=2" with the form
      | field          | value     |
      | description    | Changed.  |
      | changelog      | A change. |
      | vocabularyFile | default   |
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "version"
    And the vocabulary "{Alpha}" has 1 version

  @D54 @negative
  Scenario: The vocabulary name in the path is matched exactly
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{upper:Alpha}"
    Then the response is a problem with status 404
    And the vocabulary "{Alpha}" has 1 version

  @D43 @D4 @D7
  Scenario: Overlapping version creations are serialised
    Given a vocabulary "{Alpha}" exists
    When I create 5 new versions of "{Alpha}" in parallel
    Then the parallel responses have the statuses [201, 201, 201, 201, 201]
    And the parallel responses created the versions [2, 3, 4, 5, 6]
    And the parallel responses have distinct Location headers
    And the vocabulary "{Alpha}" has 6 versions
    And version 6 of "{Alpha}" has status "ACTIVE"
    And version 1 of "{Alpha}" has status "DEPRECATED"
    And version 2 of "{Alpha}" has status "DEPRECATED"
    And version 3 of "{Alpha}" has status "DEPRECATED"
    And version 4 of "{Alpha}" has status "DEPRECATED"
    And version 5 of "{Alpha}" has status "DEPRECATED"
