Feature: Create a vocabulary
  POST /vocabularies stores version 1 as ACTIVE in the same request (SIMPL-18170, SIMPL-18187, D6, D7).
  Field rules (D37), unknown fields (D52), sizes (D15, D76) and the order of checks (D51).

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @D6 @D7 @D10 @D14 @D26 @D27 @D31 @D5 @D110 @API-12 @API-36
  Scenario: A valid vocabulary is created with version 1 ACTIVE
    When I create vocabulary "{{Health}}"
    Then the status is 201
    And the response media type is "application/json"
    And the response header "Location" equals "/vocabularies/{{Health}}/versions/1"
    And the response field "href" equals "/vocabularies/{{Health}}/versions/1"
    And the response field "contentHref" is the contentBase followed by "/content/{{Health}}/versions/1"
    And the response field "name" equals "{{Health}}"
    And the response field "version" equals 1
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Description of {{Health}}"
    And the response field "changelog" equals "Initial version."
    And the response field "createdBy" equals "{{adminEmail}}"
    And the response field "latestVersionHref" equals "/vocabularies/{{Health}}/versions/1"
    And the response field "creationDate" is a UTC timestamp
    And the response field "lastUpdateDate" is a UTC timestamp
    And there are no validation warnings

  @AC_SIMPL-18170_AC2 @D3 @D25 @D27 @D28 @D58
  Scenario: The created vocabulary can be read back with its metadata
    When I create vocabulary "{{Health}}"
    And I remember the response field "creationDate" as "created"
    And I send "GET" to "/vocabularies/{{Health}}"
    Then the status is 200
    And the response field "href" equals "/vocabularies/{{Health}}"
    And the response field "contentHref" is the contentBase followed by "/content/{{Health}}"
    And the response field "name" equals "{{Health}}"
    And the response field "version" equals 1
    And the response field "status" equals "ACTIVE"
    And the response field "description" equals "Description of {{Health}}"
    And the response field "createdBy" equals "{{adminEmail}}"
    And the response field "creationDate" equals "{{created}}"
    And the response field "lastUpdateDate" is a UTC timestamp
    When I send "GET" to "/vocabularies/{{Health}}/versions/1"
    Then the status is 200
    And the response field "status" equals "ACTIVE"
    And the response field "changelog" equals "Initial version."

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status can be changed to DEPRECATED and back to ACTIVE after creation
    Given a vocabulary "{{Health}}" exists
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "DEPRECATED"}
      """
    Then the status is 200
    When I send "PATCH" to "/vocabularies/{{Health}}" with JSON body
      """
      {"status": "ACTIVE"}
      """
    Then the status is 200
    And the response field "status" equals "ACTIVE"

  @D1
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list (SIMPL-393 through the generic upload)
    When I create vocabulary "{{Cpsv}}" with file "cpsv-like.ttl"
    Then the status is 201
    When I send "GET" to "/vocabularies?name={{Cpsv}}"
    Then the status is 200
    And the item names are "{{Cpsv}}"

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative @BLG-err-problem
  Scenario Outline: A request without the required field <field> is rejected and nothing is stored
    When I send "POST" to "/vocabularies" with a multipart body
      | field       | value       |
      | <keep1>     | <value1>    |
      | <keep2>     | <value2>    |
      | <keep3>     | <value3>    |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary named "{{Health}}" is stored

    Examples:
      | field          | keep1       | value1     | keep2       | value2        | keep3          | value3           |
      | name           | description | Some text  | changelog   | First.        | vocabularyFile | file:valid.ttl   |
      | description    | name        | {{Health}} | changelog   | First.        | vocabularyFile | file:valid.ttl   |
      | changelog      | name        | {{Health}} | description | Some text     | vocabularyFile | file:valid.ttl   |
      | vocabularyFile | name        | {{Health}} | description | Some text     | changelog      | First.           |

  @AC_SIMPL-18170_AC3 @D37 @D49 @negative @NFR-04 @Q2
  Scenario Outline: A name that breaks the name rules is rejected (<case>)
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | <name>         |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "name"
    And no vocabulary named "<name>" is stored

    Examples:
      | case                 | name                 |
      | lower-case start     | health{{run}}        |
      | digit start          | 9Health{{run}}       |
      | hyphen               | Health-{{run}}       |
      | underscore           | Health_{{run}}       |
      | 65 characters        | {{Long65}}           |

  @AC_SIMPL-18170_AC3 @D37 @negative @NFR-04 @Q2
  Scenario: A name of 2 characters is rejected
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | Ab             |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "name"

  @D37 @NFR-04 @Q2
  Scenario: A name of exactly 64 characters is accepted
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | {{Long64}}     |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
    Then the status is 201
    And the response field "name" equals "{{Long64}}"

  @D37 @NFR-04 @Q2
  Scenario: Description and changelog of 1000 characters after trimming are accepted
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value                |
      | name           | {{Health}}           |
      | description    | padded-repeat:1000:d |
      | changelog      | padded-repeat:1000:c |
      | vocabularyFile | file:valid.ttl       |
    Then the status is 201

  @AC_SIMPL-18170_AC3 @D37 @negative @NFR-04 @Q2
  Scenario Outline: A <field> that breaks the length rule is rejected (<case>)
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | {{Health}}     |
      | <field>        | <value>        |
      | <other>        | Fine text      |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "<field>"
    And no vocabulary named "{{Health}}" is stored

    Examples:
      | case             | field       | value            | other       |
      | 1001 characters  | description | repeat:1001:d    | changelog   |
      | blank            | description | blank            | changelog   |
      | empty            | description |                  | changelog   |
      | 1001 characters  | changelog   | repeat:1001:c    | description |
      | blank            | changelog   | blank            | description |

  @AC_SIMPL-18170_AC3 @D52 @OWA-API3 @BLG-req-valid @negative
  Scenario Outline: A form field the contract does not define is rejected (<field>)
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | {{Health}}     |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
      | <field>        | <value>        |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "<field>"
    And no vocabulary named "{{Health}}" is stored

    Examples:
      | field     | value                 |
      | status    | DEPRECATED            |
      | createdBy | intruder@example.com  |
      | version   | 7                     |
      | colour    | blue                  |

  @D52 @negative
  Scenario: Two unknown form fields give one issue each
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | {{Health}}     |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
      | colour         | blue           |
      | shape          | round          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has 2 issues
    And the problem has an issue in "body" named "colour"
    And the problem has an issue in "body" named "shape"

  @D52 @negative
  Scenario: A query parameter on POST /vocabularies is rejected
    When I send "POST" to "/vocabularies?dryRun=true" with a multipart body
      | field          | value          |
      | name           | {{Health}}     |
      | description    | Some text      |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "query" named "dryRun"
    And no vocabulary named "{{Health}}" is stored

  @D110 @D52 @negative @NFR-04
  Scenario: A multipart request with more than 64 parts is rejected
    When I send "POST" to "/vocabularies" with a multipart body of 65 parts
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And no vocabulary named "{{Parts}}" is stored

  @AC_SIMPL-18170_AC3 @D15 @D37 @D51 @negative @API-46
  Scenario: A duplicate name is rejected with 409
    Given a vocabulary "{{Health}}" exists
    When I create vocabulary "{{Health}}"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '{{Health}}' already exists."
    And vocabulary "{{Health}}" has 1 version

  @D37 @negative
  Scenario: Names are unique case-insensitively
    Given a vocabulary "{{Health}}" exists
    When I create vocabulary "{{upper:Health}}"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And 1 vocabulary of this scenario is stored

  @D43 @D82 @D37
  Scenario: Two overlapping creations of the same name give one 201 and one 409
    When I send 2 requests to create vocabulary "{{Health}}" in parallel
    Then the responses have the statuses "201, 409"
    And 1 vocabulary of this scenario is stored

  @AC_SIMPL-18173_AC6 @D15 @negative @API-38 @API-46
  Scenario Outline: A request that is not multipart/form-data gets 415 (<ctype>)
    When I send "POST" to "/vocabularies" with body "<body>" and content type "<ctype>"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no vocabulary of this scenario is stored

    Examples:
      | ctype                             | body                                                  |
      | application/json                  | {}                                                    |
      | text/turtle                       | @prefix ex: <https://example.org/x#> . ex:a ex:b ex:c . |
      | application/x-www-form-urlencoded | name={{Health}}&description=x&changelog=y             |

  @D15 @D76 @NFR-04 @Q2 @negative @API-46
  Scenario: A vocabulary file one byte over 5 MB is rejected with 413
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value             |
      | name           | {{Health}}        |
      | description    | Some text         |
      | changelog      | First.            |
      | vocabularyFile | generated:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary named "{{Health}}" is stored

  @D15 @D76 @D71 @NFR-04 @Q2 @Q1
  Scenario: A vocabulary file of exactly 5 MB is accepted
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value             |
      | name           | {{Health}}        |
      | description    | Some text         |
      | changelog      | First.            |
      | vocabularyFile | generated:5242880 |
    Then the status is 201
    When I send "GET" to "/content/{{Health}}"
    Then the status is 200
    And the response body is the content uploaded for version 1 of "{{Health}}"

  @D76 @D51 @NFR-04 @Q2 @negative
  Scenario: A multipart request over 6 MB is rejected with 413 even when the file part is under 5 MB
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value             |
      | name           | {{Health}}        |
      | description    | repeat:1400000:d  |
      | changelog      | First.            |
      | vocabularyFile | generated:5000000 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary named "{{Health}}" is stored

  @D51 @negative
  Scenario: The media type check comes before the size check
    When I send "POST" to "/vocabularies" with a body of 6500000 bytes and content type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D51 @negative
  Scenario: The size check comes before the field checks
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value             |
      | name           | lower-case-name   |
      | vocabularyFile | generated:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D51 @negative
  Scenario: The field checks come before the duplicate name check
    Given a vocabulary "{{Health}}" exists
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value          |
      | name           | {{Health}}     |
      | changelog      | First.         |
      | vocabularyFile | file:valid.ttl |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"

  @D51 @negative
  Scenario: A duplicate name with invalid content gives 409, not a validation report
    Given a vocabulary "{{Health}}" exists
    When I create vocabulary "{{Health}}" with file "syntax-error.ttl"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D63 @D51 @negative @API-37 @API-46
  Scenario: An Accept header the operation cannot serve gives 406 and stores nothing
    Given the request header "Accept" is "application/xml"
    When I create vocabulary "{{Health}}"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues
    And no vocabulary named "{{Health}}" is stored

  @D63 @D51 @negative
  Scenario: The Accept check comes before the media type check
    Given the request header "Accept" is "application/xml"
    When I send "POST" to "/vocabularies" with body "x" and content type "text/plain"
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D63 @D151 @D127
  Scenario Outline: Accept <accept> is served with application/json on success
    Given the request header "Accept" is "<accept>"
    When I create vocabulary "{{Health}}"
    Then the status is 201
    And the response media type is "application/json"

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | */*                      |
      | application/*            |

  @D70 @D9 @negative
  Scenario: A 0-byte vocabulary file passes the field check and fails validation with S6
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value      |
      | name           | {{Health}} |
      | description    | Some text  |
      | changelog      | First.     |
      | vocabularyFile | empty-file |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "vocabularyFile"
    And the problem has an issue with detail "Vocabulary contains no statements" and location "document"
    And no vocabulary named "{{Health}}" is stored

  @D76 @D51 @NFR-04 @Q2 @negative
  Scenario: A multipart request of exactly 6 MB passes the size check
    When I send a create request of exactly 6291456 bytes padded with the unknown field "padding"
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly one issue, in "body" named "padding"
    And no vocabulary named "{{Health}}" is stored

  @D121 @D52
  Scenario: A multipart part with an empty name is ignored
    When I send "POST" to "/vocabularies" with a multipart body
      | field          | value            |
      | name           | {{Health}}       |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | file:valid.ttl   |
      |                | ignored          |
    Then the status is 201
    And the response field "version" equals 1

  @D63
  Scenario: A create without an Accept header is served
    Given the request has no Accept header
    When I create vocabulary "{{Health}}"
    Then the status is 201
    And the response media type is "application/json"
