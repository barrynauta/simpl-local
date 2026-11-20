Feature: Limits of the component
  Every limit is enforced with a defined error, with a test at the limit and one above (Q2, NFR-04).
  Limits of D22 and D38 are in the list and search features.

  Background:
    Given I am a vocabulary administrator

  @D15 @D76 @D71 @Q1 @Q2 @NFR-04
  Scenario: A vocabulary file of exactly 5 MB is accepted within 30 seconds
    Given a create vocabulary request for "A"
    And the request file is valid Turtle of exactly 5242880 bytes
    When I submit the request
    Then the response status is 201
    And the response took less than 30 seconds
    When I send a GET request to "/content/${A}/versions/1"
    Then the response status is 200
    And the response body equals the uploaded file

  @D15 @D76 @D71 @Q2 @NFR-04
  Scenario: A new version with a file of exactly 5 MB is accepted within 30 seconds
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is valid Turtle of exactly 5242880 bytes
    When I submit the request
    Then the response status is 201
    And the response took less than 30 seconds
    And the response field "version" is 2

  @D15 @D76 @D48 @D8 @Q2 @NFR-04 @negative @API-46
  Scenario: A vocabulary file one byte over 5 MB gets 413
    Given a create vocabulary request for "A"
    And the request file is valid Turtle of exactly 5242881 bytes
    When I submit the request
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary "A" is stored

  @D15 @D76 @D8 @Q2 @NFR-04 @negative
  Scenario: A new version file one byte over 5 MB gets 413 and changes nothing
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request file is valid Turtle of exactly 5242881 bytes
    When I submit the request
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And vocabulary "A" has 1 version
    And version 1 of vocabulary "A" has status "ACTIVE"

  @D15 @D41 @Q2 @NFR-04 @negative
  Scenario: An external file one byte over 5 MB gets 413
    Given an external vocabulary registration request for "Big" with namespace "${URI:Big}"
    And the request file is valid Turtle of exactly 5242881 bytes
    When I submit the request
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no external vocabulary "Big" is stored

  @D51 @negative
  Scenario: The size is checked before the request fields
    Given a create vocabulary request for "A"
    And the request file is valid Turtle of exactly 5242881 bytes
    And the request field "description" is removed
    When I submit the request
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D51 @negative
  Scenario: The media type is checked before the size
    Given a create vocabulary request for "A"
    And the request file is valid Turtle of exactly 5242881 bytes
    When I submit the request as "text/turtle"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D76 @Q2 @NFR-04 @negative
  Scenario: A multipart request over 6 MB gets 413 even when the file is small
    Given a create vocabulary request for "A"
    And the request field "description" has 6400000 bytes
    When I submit the request
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary "A" is stored

  @D122 @D48 @NFR-04 @negative
  Scenario: A request over 7 MB gets a 413 problem
    Given a create vocabulary request for "A"
    And the request field "description" has 7600000 bytes
    When I submit the request
    Then the response is a problem with status 413 and a type starting "urn:problem-type:simpl:"
    And no vocabulary "A" is stored

  @D110 @D48 @NFR-04 @negative
  Scenario: A JSON body over 1 MB gets 413
    When I send a POST request to "/vocabularies/searches" with a JSON body of 1100000 bytes
    Then the response is a problem with status 413 and a type starting "urn:problem-type:simpl:"

  @D37 @Q2 @NFR-04
  Scenario Outline: A name of 64 characters is accepted: <n>
    Given a create vocabulary request for "A"
    And the request field "name" is a unique name of <n> characters
    When I submit the request
    Then the response status is 201

    Examples:
      | n  |
      | 64 |

  @D37 @Q2 @NFR-04 @negative
  Scenario Outline: Names of 2 and 65 characters are rejected: <n>
    Given a create vocabulary request for "A"
    And the request field "name" is a unique name of <n> characters
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary with the requested name is stored

    Examples:
      | n  |
      | 65 |
      | 2  |

  @D37 @Q2 @NFR-04
  Scenario Outline: A <field> of 1000 characters is accepted on creation
    Given a create vocabulary request for "A"
    And the request field "<field>" has 1000 characters
    When I submit the request
    Then the response status is 201

    Examples:
      | field       |
      | description |
      | changelog   |

  @D37 @Q2 @NFR-04
  Scenario: Limits count characters after trimming
    Given a create vocabulary request for "A"
    And the request field "description" has 1000 characters surrounded by spaces
    When I submit the request
    Then the response status is 201

  @D37 @Q2 @NFR-04 @negative
  Scenario Outline: A <field> of 1001 characters is rejected on creation
    Given a create vocabulary request for "A"
    And the request field "<field>" has 1001 characters
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary "A" is stored

    Examples:
      | field       |
      | description |
      | changelog   |

  @D37 @Q2 @NFR-04
  Scenario Outline: A <field> of 1000 characters is accepted on a new version
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request field "<field>" has 1000 characters
    When I submit the request
    Then the response status is 201

    Examples:
      | field       |
      | description |
      | changelog   |

  @D37 @Q2 @NFR-04 @negative
  Scenario Outline: A <field> of 1001 characters is rejected on a new version
    Given a vocabulary "A" exists
    And a new version request for vocabulary "A"
    And the request field "<field>" has 1001 characters
    When I submit the request
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And vocabulary "A" has 1 version

    Examples:
      | field       |
      | description |
      | changelog   |
