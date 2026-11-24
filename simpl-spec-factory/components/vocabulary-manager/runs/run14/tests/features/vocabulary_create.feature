Feature: Create a new vocabulary via the API
  POST /vocabularies stores version 1 as ACTIVE and the vocabulary as ACTIVE in the same request
  (publish, D6). A rejected request stores nothing (D8). Order of checks: D51.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @D6 @D7 @D10 @D26 @D27 @D5 @D58 @D110 @API-36 @API-12
  Scenario: A valid vocabulary is created as version 1, ACTIVE
    When I create vocabulary "$Alpha" with description "Healthcare resource types."
    Then the response status is 201
    And the response media type is "application/json"
    And the response header "Location" is "/vocabularies/$Alpha/versions/1"
    And the response field "href" equals "/vocabularies/$Alpha/versions/1"
    And the response field "contentHref" equals "$CB/content/$Alpha/versions/1"
    And the response field "latestVersionHref" equals "/vocabularies/$Alpha/versions/1"
    And the response field "name" equals "$Alpha"
    And the response field "version" equals 1
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Healthcare resource types."
    And the response field "changelog" equals "Initial version."
    And the response field "createdBy" equals "ga-admin@example.com"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @D3 @D25 @D27 @D28 @D5
  Scenario: The created vocabulary is returned with its metadata and ACTIVE status
    Given a vocabulary "$Alpha" exists with description "Legal entity types."
    When I send a GET request to "/vocabularies/$Alpha"
    Then the response status is 200
    And the response field "href" equals "/vocabularies/$Alpha"
    And the response field "contentHref" equals "$CB/content/$Alpha"
    And the response field "name" equals "$Alpha"
    And the response field "version" equals 1
    And the response field "description" equals "Legal entity types."
    And the response field "status" equals "ACTIVE"
    And the response field "createdBy" equals "ga-admin@example.com"
    And the response field "creationDate" is a UTC timestamp to the second
    And the response field "lastUpdateDate" is a UTC timestamp to the second
    When I send a GET request to "/vocabularies/$Alpha/versions/1"
    Then the response status is 200
    And the response field "status" equals "ACTIVE"
    And the response field "changelog" equals "Initial version."
    And the response field "createdBy" equals "ga-admin@example.com"

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status can later be changed to DEPRECATED and back to ACTIVE
    Given a vocabulary "$Alpha" exists
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response field "status" equals "DEPRECATED"
    When I send a PATCH request to "/vocabularies/$Alpha" with JSON:
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And the response field "status" equals "ACTIVE"

  @AC_SIMPL-18187_AC1 @D31 @D26
  Scenario: The Location of the created vocabulary leads to the stored version
    When I create vocabulary "$Alpha"
    Then the response status is 201
    When I send a GET request to "/vocabularies/$Alpha/versions/1"
    Then the response status is 200
    And the response field "name" equals "$Alpha"
    And the response field "version" equals 1
    And 1 vocabulary has a name containing "$Alpha"

  @D1
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list (SIMPL-393)
    Given the vocabulary file is:
      """
      @prefix cpsv: <https://example.org/$RUN/cpsv#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      cpsv:PublicService a owl:Class ; rdfs:label "Public Service"@en .
      cpsv:hasInput a owl:ObjectProperty ; rdfs:domain cpsv:PublicService .
      """
    When I create vocabulary "$Cpsv"
    Then the response status is 201
    When I send a GET request to "/vocabularies?name=$Cpsv"
    Then the response status is 200
    And the response items are named "$Cpsv"

  @AC_SIMPL-18170_AC3 @D37 @D51 @D8 @negative @Q2 @NFR-04 @BLG-err-problem
  Scenario Outline: Invalid or incomplete metadata is rejected and nothing is stored: <case>
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value         |
      | name           | <name>        |
      | description    | <description> |
      | changelog      | <changelog>   |
      | vocabularyFile | @valid        |
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And 0 vocabularies have a name containing "$RUN"
    And no vocabulary is named exactly "<name>"

    Examples:
      | case                         | name          | description        | changelog          | field       |
      | name too short               | Ab            | d                  | c                  | name        |
      | name of 65 characters        | $N65          | d                  | c                  | name        |
      | name starts lower case       | $alpha        | d                  | c                  | name        |
      | name with a hyphen           | Al-pha$RUN    | d                  | c                  | name        |
      | name with a space            | Al pha$RUN    | d                  | c                  | name        |
      | empty name                   | @empty        | d                  | c                  | name        |
      | empty description            | $Alpha        | @empty             | c                  | description |
      | blank description            | $Alpha        | @repeat: :5        | c                  | description |
      | description of 1001 chars    | $Alpha        | @repeat:d:1001     | c                  | description |
      | empty changelog              | $Alpha        | d                  | @empty             | changelog   |
      | blank changelog              | $Alpha        | d                  | @repeat: :3        | changelog   |
      | changelog of 1001 chars      | $Alpha        | d                  | @repeat:c:1001     | changelog   |

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario Outline: A request without a required part is rejected and nothing is stored: <missing>
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part   | value   |
      | <p1>   | <v1>    |
      | <p2>   | <v2>    |
      | <p3>   | <v3>    |
    Then the response is a problem with status 400
    And the problem type is "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<missing>"
    And 0 vocabularies have a name containing "$RUN"

    Examples:
      | missing        | p1          | v1     | p2          | v2 | p3             | v3     |
      | name           | description | d      | changelog   | c  | vocabularyFile | @valid |
      | description    | name        | $Alpha | changelog   | c  | vocabularyFile | @valid |
      | changelog      | name        | $Alpha | description | d  | vocabularyFile | @valid |
      | vocabularyFile | name        | $Alpha | description | d  | changelog      | c      |

  @D37 @Q2 @NFR-04
  Scenario: A name of 64 characters is accepted
    When I create vocabulary "$N64"
    Then the response status is 201
    And the response field "name" equals "$N64"

  @D37 @Q2 @NFR-04
  Scenario: A name of 3 characters is accepted
    When I create vocabulary "$N3"
    Then the response status is 201
    And the response field "name" equals "$N3"

  @D37 @Q2 @NFR-04
  Scenario: A description of 1000 characters and a changelog of 1000 characters are accepted
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value          |
      | name           | $Alpha         |
      | description    | @repeat:d:1000 |
      | changelog      | @repeat:c:1000 |
      | vocabularyFile | @valid         |
    Then the response status is 201

  @D37
  Scenario: The description limit counts after trimming
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value          |
      | name           | $Alpha         |
      | description    | @padded:d:1000 |
      | changelog      | @padded:c:1000 |
      | vocabularyFile | @valid         |
    Then the response status is 201

  @D15 @D37 @negative @API-46
  Scenario: A duplicate name is refused with 409 and the first vocabulary is unchanged
    Given a vocabulary "$Alpha" exists
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '$Alpha' already exists."
    And 1 vocabulary has a name containing "$Alpha"
    And vocabulary "$Alpha" has 1 version

  @D37 @negative
  Scenario: A name that differs only in case is a duplicate
    Given a vocabulary "$Alpha" exists
    When I create vocabulary "$Alpha!upper"
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"
    And 1 vocabulary has a name containing "$Alpha"

  @D43 @negative
  Scenario: Two overlapping creations of the same name give one 201 and one 409
    When I create vocabulary "$Alpha" twice in parallel
    Then the parallel responses have the status codes "201, 409"
    And 1 vocabulary has a name containing "$Alpha"

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, not 400
    Given a vocabulary "$Alpha" exists
    And the vocabulary file is:
      """
      this is not turtle
      """
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 409
    And the problem type is "urn:problem-type:simpl:conflict"

  @D51 @negative
  Scenario: A malformed request with a duplicate name gives 400, not 409
    Given a vocabulary "$Alpha" exists
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | description    | d      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "changelog"

  @D52 @negative @BLG-req-valid
  Scenario: An unknown form field is rejected and nothing is stored
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | description    | d      |
      | changelog      | c      |
      | status         | ACTIVE |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "status"
    And 0 vocabularies have a name containing "$RUN"

  @D52 @D7 @negative
  Scenario: A version field on creation is an unknown field
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | description    | d      |
      | changelog      | c      |
      | version        | 1      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "version"
    And 0 vocabularies have a name containing "$RUN"

  @D195 @negative
  Scenario: A form field sent twice is rejected
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | name           | $Beta  |
      | description    | d      |
      | changelog      | c      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "name"
    And 0 vocabularies have a name containing "$RUN"

  @D52 @negative
  Scenario: A query parameter on the create operation is rejected
    When I send a multipart POST request to "/vocabularies?dryRun=true" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | description    | d      |
      | changelog      | c      |
      | vocabularyFile | @valid |
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "query" named "dryRun"
    And 0 vocabularies have a name containing "$RUN"

  @D169 @negative @Q2 @NFR-04
  Scenario: More than 64 parts are refused with one issue named body
    When I send a multipart POST request to "/vocabularies" with 65 parts
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 is in "body" named "body"
    And 0 vocabularies have a name containing "$RUN"

  @D15 @API-38 @negative @API-46
  Scenario Outline: A create request that is not multipart/form-data gets 415: <ctype>
    When I send a POST request to "/vocabularies" with the body "<body>" as "<ctype>"
    Then the response is a problem with status 415
    And the problem type is "urn:problem-type:simpl:unsupportedMediaType"
    And 0 vocabularies have a name containing "$RUN"

    Examples:
      | ctype                             | body             |
      | application/json                  | {}               |
      | text/turtle                       | @prefix x: <a> . |
      | application/x-www-form-urlencoded | name=$Alpha      |

  @D15 @D76 @D169 @negative @Q2 @NFR-04 @API-46
  Scenario: A file of 5 MB plus one byte is refused with 413
    Given the vocabulary file is a valid Turtle file of 5242881 bytes
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"
    And the problem has 1 issue
    And issue 1 is in "body" named "vocabularyFile"
    And 0 vocabularies have a name containing "$RUN"

  @D15 @D71 @Q1 @Q2 @NFR-04 @slow
  Scenario: A file of exactly 5 MB is accepted
    Given the vocabulary file is a valid Turtle file of 5242880 bytes
    When I create vocabulary "$Alpha"
    Then the response status is 201
    When I send a GET request to "/content/$Alpha"
    Then the response status is 200
    And the response body is the uploaded file of "$Alpha" version 1

  @D76 @D169 @negative @Q2 @NFR-04
  Scenario: A request of more than 6 MB with a small file is refused with 413 and no issues
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value             |
      | name           | $Alpha            |
      | description    | @repeat:d:6400000 |
      | changelog      | c                 |
      | vocabularyFile | @valid            |
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"
    And the problem has no issues
    And 0 vocabularies have a name containing "$RUN"

  @D169 @D37 @negative
  Scenario: A large text field under 6 MB is judged by its field rule, not by size
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value             |
      | name           | $Alpha            |
      | description    | @repeat:d:2000000 |
      | changelog      | c                 |
      | vocabularyFile | @valid            |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "description"
    And 0 vocabularies have a name containing "$RUN"

  @D51 @D169 @negative
  Scenario: Size is checked before the request shape
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value         |
      | name           | lower         |
      | vocabularyFile | @size:5242881 |
    Then the response is a problem with status 413
    And the problem type is "urn:problem-type:simpl:payloadTooLarge"

  @D63 @API-37 @negative @API-46
  Scenario: An Accept header the operation cannot serve gets 406 and nothing is stored
    Given the request header "Accept" is "application/xml"
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 406
    And the problem type is "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues
    And 0 vocabularies have a name containing "$RUN"

  @D63 @D51 @negative
  Scenario: 406 comes before 415
    Given the request header "Accept" is "application/xml"
    When I send a POST request to "/vocabularies" with the body "{}" as "application/json"
    Then the response is a problem with status 406

  @D63 @D151 @D127
  Scenario Outline: Accepted Accept headers on create: <accept>
    Given the request header "Accept" is "<accept>"
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response media type is "application/json"

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | application/*            |
      | */*                      |

  @D63
  Scenario: No Accept header is accepted
    Given the request has no Accept header
    When I create vocabulary "$Alpha"
    Then the response status is 201

  @D70 @D9 @negative
  Scenario: An empty file passes the field check and fails validation with S6
    Given the vocabulary file is empty
    When I create vocabulary "$Alpha"
    Then the response is a problem with status 400
    And the problem has 1 issue
    And issue 1 has detail "Vocabulary contains no statements"
    And issue 1 has location "document"
    And issue 1 is in "body" named "vocabularyFile"
    And 0 vocabularies have a name containing "$RUN"

  @D12 @D10
  Scenario: A file that uses only its own namespace and W3C namespaces gives no warnings
    Given the vocabulary file is:
      """
      @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
      @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
      @prefix owl: <http://www.w3.org/2002/07/owl#> .
      @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
      @prefix skos: <http://www.w3.org/2004/02/skos/core#> .
      @prefix me: <https://example.org/$RUN/own#> .
      me:Thing a owl:Class ; rdfs:label "Thing"@en .
      me:size a owl:DatatypeProperty ; rdfs:range xsd:integer .
      me:t1 a me:Thing ; me:size "3"^^xsd:integer ; skos:prefLabel "t1"@en .
      """
    When I create vocabulary "$Alpha"
    Then the response status is 201
    And the response has no validation warnings

  @D27 @D16 @D165
  Scenario: The creator of a version is the user who sent the request
    When I create vocabulary "$Alpha"
    Then the response field "createdBy" equals "ga-admin@example.com"
    When I read "/vocabularies/$Alpha" as "ga-viewer"
    Then the response field "createdBy" equals "ga-admin@example.com"

  @D121 @D52
  Scenario: A multipart part with an empty name is ignored
    When I send a multipart POST request to "/vocabularies" with the parts:
      | part           | value  |
      | name           | $Alpha |
      | description    | d      |
      | changelog      | c      |
      | @noname        | x      |
      | vocabularyFile | @valid |
    Then the response status is 201
    And 1 vocabulary has a name containing "$Alpha"

  @D51 @D15 @negative
  Scenario: The media type is checked before the size
    When I send a POST request to "/vocabularies" with a JSON body of 6500000 bytes
    Then the response is a problem with status 415
    And the problem type is "urn:problem-type:simpl:unsupportedMediaType"
