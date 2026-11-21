Feature: Create a vocabulary
  POST /vocabularies stores version 1 as ACTIVE in the same request when it passes
  validation (D6). Field rules D37, limits D15 and D76, order of checks D51.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @D6 @D7 @D10 @D26 @D27 @D5 @D58 @D110 @D31 @D39 @E1 @E2 @E4 @API-36
  Scenario: A valid vocabulary is created as version 1 and published as ACTIVE
    When I create the vocabulary "Alpha"
    Then the response is the created version 1 of "Alpha"
    And the response media type is "application/json"
    And the response JSON has
      | field       | value                   |
      | description | Description of {Alpha}. |
      | changelog   | Initial version.        |
    And the response has no validation warnings

  @AC_SIMPL-18170_AC2 @D25 @D27 @D28 @D3 @D58
  Scenario: The created vocabulary can be read with its metadata and version 1
    Given a vocabulary "Alpha" exists
    When I send GET "/vocabularies/{Alpha}"
    Then the response status is 200
    And the response JSON has
      | field          | value                     |
      | href           | /vocabularies/{Alpha}     |
      | contentHref    | {base}/content/{Alpha}    |
      | name           | {Alpha}                   |
      | version        | 1                         |
      | description    | Description of {Alpha}.   |
      | status         | ACTIVE                    |
      | createdBy      | {admin_email}             |
      | creationDate   | <date-time>               |
      | lastUpdateDate | <date-time>               |
    And version 1 of "Alpha" has the version status "ACTIVE"

  @AC_SIMPL-18187_AC1 @D1 @D29
  Scenario: A CPSV-like vocabulary is uploaded, listed and downloaded as uploaded
    When I create the vocabulary "Cpsv" with the fixture "cpsv-like.ttl"
    Then the response is the created version 1 of "Cpsv"
    When I send GET "/vocabularies?name={tok}"
    Then the response lists the vocabularies "{Cpsv}" in this order
    When I send GET "/content/{Cpsv}"
    Then the response body is the uploaded content of version 1 of "Cpsv"

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative @OWT-A10
  Scenario: A request without a vocabulary file is rejected and nothing is stored
    When I create a vocabulary with the parts
      | part        | value            |
      | name        | {Alpha}          |
      | description | Some text.       |
      | changelog   | Initial version. |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "vocabularyFile"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative @OWT-A10
  Scenario: A request without a description is rejected and nothing is stored
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "description"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18170_AC3 @D37 @D32 @D8 @negative
  Scenario: A request without a changelog is rejected and nothing is stored
    When I create a vocabulary with the parts
      | part           | value          |
      | name           | {Alpha}        |
      | description    | Some text.     |
      | vocabularyFile | default-turtle |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "changelog"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario: A request without a name is rejected and nothing is stored
    When I create a vocabulary with the parts
      | part           | value            |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary of this scenario is stored

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario Outline: A name that breaks the name rule is rejected: <case>
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | <name>           |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary of this scenario is stored

    Examples:
      | case                     | name            |
      | starts with lower case   | lower{tok}      |
      | starts with a digit      | 9Digit{tok}     |
      | contains a hyphen        | Has-Hyphen{tok} |
      | contains a space         | Has Space{tok}  |
      | contains an underscore   | Under_{tok}     |

  @AC_SIMPL-18170_AC3 @D37 @Q2 @NFR-04 @negative
  Scenario Outline: A name of <length> characters is rejected
    When I create a vocabulary with a name of <length> characters
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary with that name is stored

    Examples:
      | length |
      | 2      |
      | 65     |

  @AC_SIMPL-18170_AC1 @D37 @Q2 @NFR-04
  Scenario Outline: A name of <length> characters is accepted
    When I create a vocabulary with a name of <length> characters
    Then the response status is 201
    And the vocabulary with that name is stored

    Examples:
      | length |
      | 3      |
      | 64     |

  @AC_SIMPL-18170_AC3 @D37 @D79 @Q2 @NFR-04 @negative
  Scenario Outline: The <part> rule is enforced: <case>
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | <description>    |
      | changelog      | <changelog>      |
      | vocabularyFile | default-turtle   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<part>"
    And no vocabulary of this scenario is stored

    Examples:
      | part        | case                       | description         | changelog           |
      | description | 1001 characters            | text-of-length:1001 | Initial version.    |
      | description | blank after trimming       | blank:3             | Initial version.    |
      | changelog   | 1001 characters            | Some text.          | text-of-length:1001 |
      | changelog   | empty                      | Some text.          | empty-text          |

  @AC_SIMPL-18170_AC1 @D37 @Q2 @NFR-04
  Scenario Outline: The <part> limit is accepted at <case>
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | <description>    |
      | changelog      | <changelog>      |
      | vocabularyFile | default-turtle   |
    Then the response is the created version 1 of "Alpha"

    Examples:
      | part        | case                                 | description         | changelog           |
      | description | 1000 characters                      | text-of-length:1000 | Initial version.    |
      | description | 1 character                          | D                   | Initial version.    |
      | description | 1000 characters after trimming       | padded-text:1004    | Initial version.    |
      | changelog   | 1000 characters                      | Some text.          | text-of-length:1000 |

  @AC_SIMPL-18169_AC2 @AC_SIMPL-18170_AC3 @D15 @D37 @negative @API-46 @API-47
  Scenario: A duplicate name is refused with 409 and only one vocabulary is stored
    Given a vocabulary "Alpha" exists
    When I create the vocabulary "Alpha"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '{Alpha}' already exists."
    And the problem type follows the Simpl URN form
    And exactly 1 vocabulary of this scenario is stored

  @AC_SIMPL-18170_AC3 @D37 @negative
  Scenario: Names are unique without regard to case
    Given a vocabulary "Alpha" exists
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha:upper}   |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And exactly 1 vocabulary of this scenario is stored

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, not 400
    Given a vocabulary "Alpha" exists
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | file:syntax-error-line3.ttl |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @AC_SIMPL-18170_AC3 @D15 @negative @API-38 @API-46
  Scenario: A JSON body instead of multipart/form-data gets 415
    When I send POST "/vocabularies" with the JSON body
      """
      {"name": "{Alpha}", "description": "Some text.", "changelog": "Initial version."}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And the problem type follows the Simpl URN form
    And no vocabulary of this scenario is stored

  @D15 @D71 @D76 @Q1 @Q2 @NFR-04
  Scenario: A file of exactly 5 MB is accepted within 30 seconds
    When I create a vocabulary with the parts
      | part           | value                 |
      | name           | {Alpha}               |
      | description    | Some text.            |
      | changelog      | Initial version.      |
      | vocabularyFile | turtle-of-size:5242880 |
    Then the response is the created version 1 of "Alpha"
    And the response arrived within 30 seconds
    When I send GET "/content/{Alpha}"
    Then the response body is the uploaded content of version 1 of "Alpha"

  @D15 @D76 @Q2 @NFR-04 @negative @API-46
  Scenario: A file one byte above 5 MB gets 413 and nothing is stored
    When I create a vocabulary with the parts
      | part           | value                  |
      | name           | {Alpha}                |
      | description    | Some text.             |
      | changelog      | Initial version.       |
      | vocabularyFile | turtle-of-size:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And the problem type follows the Simpl URN form
    And no vocabulary of this scenario is stored

  @D76 @D51 @Q2 @NFR-04 @negative
  Scenario: A multipart request of exactly 6 MB passes the size check
    When I create a vocabulary with the parts
      | part           | value                   |
      | name           | {Alpha}                 |
      | description    | Some text.              |
      | changelog      | Initial version.        |
      | vocabularyFile | default-turtle          |
      | padding        | pad-request-to:6291456  |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "padding"
    And no vocabulary of this scenario is stored

  @D76 @D122 @Q2 @NFR-04 @negative
  Scenario: A multipart request above 6 MB gets 413
    When I create a vocabulary with the parts
      | part           | value                   |
      | name           | {Alpha}                 |
      | description    | Some text.              |
      | changelog      | Initial version.        |
      | vocabularyFile | default-turtle          |
      | padding        | pad-request-to:6291457  |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"
    And no vocabulary of this scenario is stored

  @D51 @D15 @negative
  Scenario: The size check runs before the field checks
    When I create a vocabulary with the parts
      | part           | value                  |
      | name           | {Alpha}                |
      | changelog      | Initial version.       |
      | vocabularyFile | turtle-of-size:5242881 |
    Then the response is a problem with status 413 and type "urn:problem-type:simpl:payloadTooLarge"

  @D51 @D15 @negative
  Scenario: The media type check runs before the size check
    When I send POST "/vocabularies" with a body of 5242881 bytes of type "text/plain"
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"

  @D52 @D79 @negative @BLG-req-valid @OWA-API3
  Scenario: Unknown parts are rejected with one issue per unknown name
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
      | status         | DEPRECATED       |
      | createdBy      | someone@else     |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has exactly 2 issues
    And the problem has an issue in "body" named "status"
    And the problem has an issue in "body" named "createdBy"
    And no vocabulary of this scenario is stored

  @D52 @negative @BLG-req-valid
  Scenario: A query parameter on the create operation is rejected
    When I send a multipart POST to "/vocabularies?dryRun=true" with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary of this scenario is stored

  @D121
  Scenario: A part with an empty name is ignored
    When I create a vocabulary with the parts
      | part           | value            |
      | name           | {Alpha}          |
      | description    | Some text.       |
      | changelog      | Initial version. |
      | vocabularyFile | default-turtle   |
      |                | ignored          |
    Then the response is the created version 1 of "Alpha"
