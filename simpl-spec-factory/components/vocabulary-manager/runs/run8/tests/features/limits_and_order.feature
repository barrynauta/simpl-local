Feature: Upload limits and the order of checks on write operations
  The vocabularyFile part may hold at most 5 MB (5 242 880 bytes); the whole
  multipart request at most 6 MB. Above a limit the API answers 413 with the
  UploadTooLargeResponse body. Checks run in the order of D51 and D63:
  401, 403, 406, 415, 413, 400, 404, 409, then content validation.

  Background:
    Given I am authenticated as "ga-admin"

  @D15 @D71 @D76 @Q1 @Q2 @NFR-04
  Scenario: A file of exactly 5 MB is accepted within 30 seconds
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | ${Big}           |
      | description    | Five megabytes   |
      | changelog      | Initial version. |
      | vocabularyFile | size:5242880     |
    Then the response status is 201
    And the request took at most 30 seconds
    When I send a GET request to "/content/${Big}"
    Then the response body is the content uploaded for version 1 of "Big"

  @D15 @D76 @D48 @Q2 @NFR-04 @negative @API-46
  Scenario: A file one byte over 5 MB is refused with 413 and nothing is stored
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | ${Big}           |
      | description    | Too big          |
      | changelog      | Initial version. |
      | vocabularyFile | size:5242881     |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary whose name contains "${uid}" is stored

  @D15 @D71 @Q2 @NFR-04
  Scenario: A new version with a file of exactly 5 MB is accepted
    Given a vocabulary "Big" exists
    When I send a POST request to "/vocabularies/${Big}/versions" with these form parts
      | part           | value          |
      | description    | Five megabytes |
      | changelog      | Bigger         |
      | vocabularyFile | size:5242880   |
    Then the response status is 201
    And the request took at most 30 seconds

  @D15 @Q2 @NFR-04 @negative
  Scenario: A new version with a file over 5 MB is refused with 413 and deprecates nothing
    Given a vocabulary "Big" exists
    When I send a POST request to "/vocabularies/${Big}/versions" with these form parts
      | part           | value          |
      | description    | Too big        |
      | changelog      | Too big        |
      | vocabularyFile | size:5242881   |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And vocabulary "Big" has 1 version
    And version 1 of "Big" has version status "ACTIVE"

  @D15 @D41 @Q2 @NFR-04 @negative
  Scenario: An external vocabulary file over 5 MB is refused with 413
    When I send a POST request to "/external-vocabularies" with these form parts
      | part           | value                                |
      | name           | ${Big}                               |
      | namespaceUri   | http://big.example.org/${uid}/terms  |
      | vocabularyFile | size:5242881                         |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no external vocabulary is registered for namespace "http://big.example.org/${uid}/terms"

  @D76 @Q2 @NFR-04 @negative
  Scenario: A multipart request over 6 MB is refused with 413 even when the file is small
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value                 |
      | name           | ${Big}                |
      | description    | {{repeat:d:6400000}}  |
      | changelog      | Initial version.      |
      | vocabularyFile | default               |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary whose name contains "${uid}" is stored

  @D76 @D51 @Q2 @NFR-04 @negative
  Scenario: A request just under 6 MB is not refused for its size
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value                |
      | name           | ${Big}               |
      | description    | {{repeat:d:700000}}  |
      | changelog      | Initial version.     |
      | vocabularyFile | size:5242880         |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "description"
    And no vocabulary whose name contains "${uid}" is stored

  # ---- order of checks (D51, D63) ----------------------------------------------

  @D51 @D63 @D17 @negative
  Scenario: 401 comes before 406, 415 and 400
    Given I am not authenticated
    When I send a POST request to "/vocabularies" with header "Accept" set to "application/xml" and JSON body
      """
      {"name": "lower"}
      """
    Then the response is a problem with status 401

  @D51 @D63 @negative
  Scenario: 403 comes before 406 and 415
    Given I am authenticated as "ga-viewer"
    When I send a POST request to "/vocabularies" with header "Accept" set to "application/xml" and JSON body
      """
      {"name": "lower"}
      """
    Then the response is a problem with status 403

  @D51 @D63 @negative
  Scenario: 406 comes before 415
    When I send a POST request to "/vocabularies" with header "Accept" set to "application/xml" and JSON body
      """
      {"name": "lower"}
      """
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"

  @D51 @negative
  Scenario: 413 comes before 400
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value         |
      | name           | lower${uid}   |
      | vocabularyFile | size:5242881  |
    Then the response is a problem with status 413

  @D51 @D49 @negative
  Scenario: 400 comes before 404 for a new version of an unknown vocabulary
    When I send a POST request to "/vocabularies/${Ghost}/versions" with these form parts
      | part           | value    |
      | description    | Next     |
      | vocabularyFile | default  |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "changelog"

  @D51 @negative
  Scenario: 404 comes before content validation
    When I send a POST request to "/vocabularies/${Ghost}/versions" with these form parts
      | part           | value                    |
      | description    | Next                     |
      | changelog      | Next                     |
      | vocabularyFile | fixture:syntax_error.ttl |
    Then the response is a problem with status 404
