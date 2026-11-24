Feature: Create a new version of a vocabulary via the API
  POST /vocabularies/{vocabularyName}/versions assigns the next number (D7), sets the new version
  ACTIVE and every older ACTIVE version DEPRECATED in the same transaction (D4). The vocabulary
  status does not change (D3). A rejected request stores nothing (D8).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18187_AC2 @AC_SIMPL-18190_AC1 @AC_SIMPL-18191_AC1 @AC_SIMPL-18186_AC1 @D4 @D7 @D26 @D27 @D5 @D10
  Scenario: A new version gets the next number, is ACTIVE and deprecates the previous one
    Given a vocabulary "$Alpha" exists
    When I create a new version of "$Alpha" with description "Extended diagnostics." and changelog "Added diagnostics."
    Then the response status is 201
    And the response header "Location" is "/vocabularies/$Alpha/versions/2"
    And the response field "href" equals "/vocabularies/$Alpha/versions/2"
    And the response field "contentHref" equals "$CB/content/$Alpha/versions/2"
    And the response field "latestVersionHref" equals "/vocabularies/$Alpha/versions/2"
    And the response field "name" equals "$Alpha"
    And the response field "version" equals 2
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Extended diagnostics."
    And the response field "changelog" equals "Added diagnostics."
    And the response field "createdBy" equals "ga-admin@example.com"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response has no validation warnings
    And version 1 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 2 of vocabulary "$Alpha" has version status "ACTIVE"
    And version 1 of "$Alpha" was last updated when version 2 was created

  @AC_SIMPL-18173_AC1 @AC_SIMPL-18191_AC1 @D4
  Scenario: All previous versions are DEPRECATED after several new versions
    Given vocabulary "$Alpha" has 3 versions
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response field "version" equals 4
    And version 1 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 2 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 3 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 4 of vocabulary "$Alpha" has version status "ACTIVE"

  @D4
  Scenario: A new version deprecates every ACTIVE version after a manual reactivation
    Given vocabulary "$Alpha" has 2 versions
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response field "version" equals 3
    And version 1 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 2 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 3 of vocabulary "$Alpha" has version status "ACTIVE"

  @D7
  Scenario: The next number is the highest number plus 1, whatever the statuses
    Given vocabulary "$Alpha" has 3 versions
    And version 3 of vocabulary "$Alpha" has version status "DEPRECATED"
    When I create a new version of "$Alpha"
    Then the response field "version" equals 4

  @D25 @D28 @D5 @AC_SIMPL-18187_AC2
  Scenario: The vocabulary shows the latest version's number and description after a new version
    Given a vocabulary "$Alpha" exists with description "First description."
    And I save field "creationDate" of "/vocabularies/$Alpha" as "created"
    And a second has passed
    When I create a new version of "$Alpha" with description "Second description."
    Then the response status is 201
    And I save the response field "creationDate" as "v2created"
    When I send a GET request to "/vocabularies/$Alpha"
    Then the response field "version" equals 2
    And the response field "description" equals "Second description."
    And the response field "creationDate" is the saved "created"
    And the response field "lastUpdateDate" is the saved "v2created"
    And the response field "createdBy" equals "ga-admin@example.com"

  @D3
  Scenario: A new version for a DEPRECATED vocabulary is created and the vocabulary stays DEPRECATED
    Given a vocabulary "$Alpha" exists
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response field "status" equals "ACTIVE"
    And vocabulary "$Alpha" has vocabulary status "DEPRECATED"

  @D3
  Scenario: Creating a version leaves an ACTIVE vocabulary ACTIVE
    Given a vocabulary "$Alpha" exists
    When I create a new version of "$Alpha"
    Then vocabulary "$Alpha" has vocabulary status "ACTIVE"

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative @Q2 @NFR-04
  Scenario Outline: Invalid or incomplete version data is rejected and nothing changes: <case>
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value         |
      | description    | <description> |
      | changelog      | <changelog>   |
      | vocabularyFile | @valid        |
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

    Examples:
      | case                       | description    | changelog      | field       |
      | empty description          | @empty         | c              | description |
      | blank description          | @repeat: :4    | c              | description |
      | description of 1001 chars  | @repeat:d:1001 | c              | description |
      | empty changelog            | d              | @empty         | changelog   |
      | blank changelog            | d              | @repeat: :4    | changelog   |
      | changelog of 1001 chars    | d              | @repeat:c:1001 | changelog   |

  @D37 @Q2 @NFR-04
  Scenario: A description and changelog of 1000 characters are accepted on a new version
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value          |
      | description    | @repeat:d:1000 |
      | changelog      | @repeat:c:1000 |
      | vocabularyFile | @valid         |
    Then the response status is 201
    And the response field "version" equals 2

  @AC_SIMPL-18173_AC2 @D37 @D8 @negative
  Scenario Outline: A new version without a required part is rejected: <missing>
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part | value |
      | <p1> | <v1>  |
      | <p2> | <v2>  |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<missing>"
    And vocabulary "$Alpha" has 1 version

    Examples:
      | missing        | p1          | v1 | p2             | v2     |
      | description    | changelog   | c  | vocabularyFile | @valid |
      | changelog      | description | d  | vocabularyFile | @valid |
      | vocabularyFile | description | d  | changelog      | c      |

  @AC_SIMPL-18173_AC2 @D52 @negative
  Scenario: A name field on a new version is an unknown field
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | description    | d      |
      | changelog      | c      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "name"
    And vocabulary "$Alpha" has 1 version

  @AC_SIMPL-18190_AC2 @D7 @D8 @negative
  Scenario Outline: A manual version number is rejected and no version is created: <value>
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value   |
      | description    | d       |
      | changelog      | c       |
      | version        | <value> |
      | vocabularyFile | @valid  |
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has 1 issue
    And issue 1 is in "body" named "version"
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

    Examples:
      | value |
      | 2     |
      | 7     |
      | 1     |

  @AC_SIMPL-18190_AC2 @D7
  Scenario: After a rejected manual number the next version still gets number 2
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value  |
      | description    | d      |
      | changelog      | c      |
      | version        | 5      |
      | vocabularyFile | @valid |
    Then the response status is 400
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response field "version" equals 2

  @AC_SIMPL-18173_AC3 @D24 @D66 @negative
  Scenario: A new version for a vocabulary that does not exist is rejected with 404
    When I create a new version of "$Missing"
    Then the response is a problem with status 404
    And the problem type is "urn:problem-type:belgif:resourceNotFound"
    And the problem detail is "Vocabulary with name '$Missing' was not found."
    And the problem has 1 issue
    And issue 1 is in "path" named "vocabularyName"
    And 0 vocabularies have a name containing "$RUN"

  @D54 @negative
  Scenario: The path name must match the stored name exactly
    Given a vocabulary "$Alpha" exists
    When I create a new version of "$Alpha!upper"
    Then the response is a problem with status 404
    And vocabulary "$Alpha" has 1 version

  @D51 @negative
  Scenario: A malformed request to an unknown name gives 400, not 404
    When I send a multipart POST request to "/vocabularies/$Missing/versions" with the parts:
      | part           | value  |
      | description    | d      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "changelog"

  @D49 @negative
  Scenario Outline: An invalid name in the path gives 400: <name>
    When I send a multipart POST request to "/vocabularies/<name>/versions" with the parts:
      | part           | value  |
      | description    | d      |
      | changelog      | c      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "path" named "vocabularyName"
    And issue 1 has type "urn:problem-type:belgif:input-validation:schemaViolation"

    Examples:
      | name   |
      | ab     |
      | Ab     |
      | $alpha |
      | $N65   |

  @AC_SIMPL-18173_AC6 @D15 @API-38 @negative @API-46
  Scenario Outline: A new version in an unsupported format is rejected with 415: <ctype>
    Given a vocabulary "$Alpha" exists
    When I send a POST request to "/vocabularies/$Alpha/versions" with the body "<body>" as "<ctype>"
    Then the response is a problem with status 415
    And the problem type is "urn:problem-type:simpl:unsupportedMediaType"
    And vocabulary "$Alpha" has 1 version

    Examples:
      | ctype            | body             |
      | application/json | {}               |
      | text/turtle      | @prefix x: <a> . |
      | text/plain       | hello            |

  @AC_SIMPL-18173_AC2 @D15 @D9 @negative
  Scenario: A binary file is rejected as a syntax error
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is the fixture "binary.png"
    When I create a new version of "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "vocabularyFile"
    And issue 1 has a detail starting with "Encountered invalid Turtle syntax"
    And vocabulary "$Alpha" has 1 version

  @D15 @D76 @D169 @negative @Q2 @NFR-04
  Scenario: A new version with a file of 5 MB plus one byte is refused with 413
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is a valid Turtle file of 5242881 bytes
    When I create a new version of "$Alpha"
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"
    And the problem has 1 issue
    And issue 1 is in "body" named "vocabularyFile"
    And vocabulary "$Alpha" has 1 version

  @D15 @D71 @Q1 @Q2 @NFR-04 @slow
  Scenario: A new version with a file of exactly 5 MB is accepted
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is a valid Turtle file of 5242880 bytes
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response field "version" equals 2

  @D46 @D8 @D194 @negative @API-46
  Scenario: Resubmitting the latest version unchanged is refused and deprecates nothing
    Given a vocabulary "$Alpha" exists with description "Same description."
    And the vocabulary file is the current content of "$Alpha"
    When I create a new version of "$Alpha" with description "Same description." and changelog "Other changelog."
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And the problem detail is "Version 1 of vocabulary '$Alpha' already has this content and description."
    And vocabulary "$Alpha" has 1 version
    And version 1 of vocabulary "$Alpha" has version status "ACTIVE"

  @D46 @negative
  Scenario: The identical-version check compares the trimmed description
    Given a vocabulary "$Alpha" exists with description "Same description."
    And the vocabulary file is the current content of "$Alpha"
    When I create a new version of "$Alpha" with description "  Same description.  " and changelog "c"
    Then the response is a problem with status 409
    And vocabulary "$Alpha" has 1 version

  @D46 @D33
  Scenario: A description-only change creates a new version
    Given a vocabulary "$Alpha" exists with description "Old description."
    And the vocabulary file is the current content of "$Alpha"
    When I create a new version of "$Alpha" with description "New description." and changelog "Description only."
    Then the response status is 201
    And the response field "version" equals 2
    When I send a GET request to "/content/$Alpha/versions/2"
    Then the response body is the uploaded file of "$Alpha" version 1

  @D46
  Scenario: A content-only change creates a new version
    Given a vocabulary "$Alpha" exists with description "Same description."
    When I create a new version of "$Alpha" with description "Same description." and changelog "Content only."
    Then the response status is 201
    And the response field "version" equals 2

  @D46 @D5 @negative
  Scenario: The identical-version check compares with the latest version only
    Given a vocabulary "$Alpha" exists with description "Same description."
    And the vocabulary file is the current content of "$Alpha"
    And a new version of "$Alpha" is created with description "Other description."
    When I create a new version of "$Alpha" with description "Same description." and changelog "Back to version 1."
    Then the response status is 201
    And the response field "version" equals 3

  @D46 @D51 @negative
  Scenario: The identical-version check runs after the field checks
    Given a vocabulary "$Alpha" exists with description "Same description."
    And the vocabulary file is the current content of "$Alpha"
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value             |
      | description    | Same description. |
      | vocabularyFile | @file             |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "changelog"

  @D43 @D82 @D119 @D4
  Scenario: Four overlapping new versions all succeed with distinct numbers
    Given a vocabulary "$Alpha" exists
    When I send 4 new versions of "$Alpha" in parallel
    Then all parallel responses have status 201
    And the parallel responses created versions 2 to 5 once each
    And the parallel responses have distinct Location headers
    And vocabulary "$Alpha" has 5 versions
    And version 5 of vocabulary "$Alpha" has version status "ACTIVE"
    And version 1 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 2 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 3 of vocabulary "$Alpha" has version status "DEPRECATED"
    And version 4 of vocabulary "$Alpha" has version status "DEPRECATED"

  @D57 @D171 @D12
  Scenario: Relative IRIs resolve against the content URL of the new version and are the vocabulary itself
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix self: <#> .
      self:Scheme a skos:ConceptScheme ; skos:prefLabel "Relative"@en .
      <#Other> a skos:Concept ; skos:inScheme self:Scheme .
      """
    When I create a new version of "$Alpha"
    Then the response status is 201
    And the response has no validation warning "externalReferenceSkipped"
    And the response has no validation warnings

  @D195 @negative
  Scenario: A form field sent twice on a new version is rejected
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies/$Alpha/versions" with the parts:
      | part           | value  |
      | description    | d      |
      | changelog      | c1     |
      | changelog      | c2     |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "changelog"
    And vocabulary "$Alpha" has 1 version
