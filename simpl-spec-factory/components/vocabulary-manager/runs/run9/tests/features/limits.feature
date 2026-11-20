Feature: Upload limits and time budget
  The vocabularyFile part may hold at most 5 MB (5 242 880 bytes, D15, D76); the whole multipart
  request at most 6 MB (D76). Each limit has a test at the limit and one above (Q2, NFR-04).

  @D15 @D76 @D71 @Q1 @Q2 @NFR-04
  Scenario: A file of exactly 5 MB is accepted and stored within 30 seconds
    When I create the vocabulary "Big" with a file of 5242880 bytes
    Then the response status is 201
    And the response time is below 30 seconds
    When I send a "GET" request to "/content/${Big}"
    Then the response body is the uploaded content of version 1 of "Big"

  @D15 @D76 @D71 @Q1 @Q2 @NFR-04
  Scenario: A new version with a file of exactly 5 MB is accepted within 30 seconds
    Given a vocabulary "Big" exists
    When I create a new version of "Big" with a file of 5242880 bytes
    Then the response status is 201
    And the response time is below 30 seconds

  @D15 @D76 @D8 @Q2 @negative @NFR-04 @API-46
  Scenario: A file of 5 MB plus one byte is rejected with 413 and nothing is stored
    When I create the vocabulary "Huge" with a file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the problem title is "Payload Too Large"
    And the problem type follows API-46 and API-47
    And no vocabulary named "Huge" is stored

  @D15 @D76 @D8 @Q2 @negative @NFR-04
  Scenario: A new version with a file of 5 MB plus one byte is rejected with 413
    Given a vocabulary "Huge" exists
    When I create a new version of "Huge" with a file of 5242881 bytes
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the vocabulary "Huge" has 1 versions

  @D41 @D15 @D76 @Q2 @negative @NFR-04
  Scenario: Registering an external vocabulary with a file over 5 MB is rejected with 413
    When I send a multipart POST to "/external-vocabularies" with
      | part           | value            |
      | name           | ${ext:Huge}      |
      | namespaceUri   | ${extns:Huge}#   |
      | vocabularyFile | @bytes:5242881   |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no external vocabulary with namespace "${extns:Huge}#" is stored

  @D76 @D51 @Q2 @negative @NFR-04
  Scenario: A multipart request over 6 MB is rejected with 413 even when the file is under 5 MB, size comes before field rules
    When I send a multipart POST to "/vocabularies" with
      | part           | value               |
      | name           | Wide${run}          |
      | description    | ${repeat:w:2200000} |
      | changelog      | Initial version.    |
      | vocabularyFile | @bytes:4194304      |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary of this scenario is stored

  @D51 @negative
  Scenario: The size check runs before the name rules
    When I send a multipart POST to "/vocabularies" with
      | part           | value            |
      | name           | not-valid        |
      | description    | Description      |
      | changelog      | Initial version. |
      | vocabularyFile | @bytes:5242881   |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D51 @D15 @negative
  Scenario: The media type check runs before the size check
    When I send a "POST" request to "/vocabularies" with content type "text/plain" and 6500000 bytes
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D92 @D44 @Q2
  Scenario: Parallel uploads beyond the limit of 4 get 503 with Retry-After, never another error
    When I send 6 uploads of 3000000 bytes in parallel
    Then every parallel response is 201 or a 503 with the detail "Too many uploads in progress. Retry later." and Retry-After "5"
