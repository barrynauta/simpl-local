Feature: Create a vocabulary through the API
  A Governance Authority user with GA_VOCABULARY_ADMIN creates a vocabulary
  with POST /vocabularies. Version 1 is validated, stored and ACTIVE in the
  same request (publish). Nothing is stored when the request fails.

  Background:
    Given I am authenticated as "ga-admin"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18181_AC1 @AC_SIMPL-18183_AC1 @AC_SIMPL-18194_AC2 @AC_SIMPL-18186_AC1
  @D6 @D7 @D10 @D26 @D27 @D31 @D58 @BLG-stat-codes @API-12 @API-36
  Scenario: A valid vocabulary is created as version 1 and is ACTIVE
    When I create the vocabulary "Health"
    Then the response status is 201
    And the response media type is "application/json"
    And the response field "name" is "${Health}"
    And the response field "version" is "1"
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Description of ${Health}"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "href" is "/vocabularies/${Health}/versions/1"
    And the response field "contentHref" is "${contentBase}/content/${Health}/versions/1"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    And the response field "validationWarnings" is an empty list
    And the response header "Location" is "/vocabularies/${Health}/versions/1"
    And the Location header is the response field "href"

  @AC_SIMPL-18170_AC2 @AC_SIMPL-18187_AC1 @D3 @D5 @D25 @D27 @D28
  Scenario: The created vocabulary can be read back with all provided metadata
    Given a vocabulary "Health" exists with description "Healthcare resource types ${uid}"
    When I send a GET request to "/vocabularies/${Health}"
    Then the response status is 200
    And the response field "name" is "${Health}"
    And the response field "version" is "1"
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Healthcare resource types ${uid}"
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "href" is "/vocabularies/${Health}"
    And the response field "contentHref" is "${contentBase}/content/${Health}"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    And version 1 of "Health" has version status "ACTIVE"

  @AC_SIMPL-18170_AC2 @D5 @D26 @D27
  Scenario: Version 1 of a new vocabulary is ACTIVE and is its latest version
    Given a vocabulary "Health" exists
    When I send a GET request to "/vocabularies/${Health}/versions/1"
    Then the response status is 200
    And the response field "status" is "ACTIVE"
    And the response field "version" is "1"
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" is "${email:ga-admin}"
    And the response field "latestVersionHref" is "/vocabularies/${Health}/versions/1"

  @D1
  Scenario: A CPSV-like vocabulary uploaded by the user can be found in the list (no seed data)
    When I create the vocabulary "Cpsv" with the fixture "cpsv_like.ttl"
    Then the response status is 201
    When I send a GET request to "/vocabularies?name=${Cpsv}"
    Then the response status is 200
    And the response items have the names "${Cpsv}"

  @AC_SIMPL-18170_AC3 @AC_SIMPL-18169_AC2 @D8 @D24 @D37 @D52 @negative @BLG-err-problem @API-44 @API-45 @OWT-A05
  Scenario Outline: A create request without a required part is rejected and stores nothing
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value                |
      | <part1>        | <value1>             |
      | <part2>        | <value2>             |
      | <part3>        | <value3>             |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<missing>"
    And no vocabulary whose name contains "${uid}" is stored

    Examples:
      | missing        | part1       | value1      | part2       | value2        | part3          | value3      |
      | name           | description | Some text   | changelog   | Initial version. | vocabularyFile | default  |
      | description    | name        | ${Missing}  | changelog   | Initial version. | vocabularyFile | default  |
      | changelog      | name        | ${Missing}  | description | Some text     | vocabularyFile | default     |
      | vocabularyFile | name        | ${Missing}  | description | Some text     | changelog      | Initial version. |

  @AC_SIMPL-18170_AC3 @D37 @D49 @D8 @negative @OWT-A05
  Scenario Outline: A name that breaks the name rules is rejected and stores nothing
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | <name>           |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"
    And no vocabulary whose name contains "${uid}" is stored

    Examples:
      | case                   | name             |
      | starts lower case      | lower${uid}      |
      | contains a hyphen      | Bad-${uid}       |
      | contains a space       | Bad ${uid}       |
      | contains an underscore | Bad_${uid}       |
      | 65 characters          | {{name:65}}      |

  @AC_SIMPL-18170_AC3 @D37 @Q2 @NFR-04 @negative
  Scenario: A two-character name is rejected
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | Ab               |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "name"

  @D37 @Q2 @NFR-04
  Scenario: A name of exactly 64 characters is accepted
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | {{name:64}}      |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response status is 201
    And 1 vocabulary whose name contains "${uid}" is stored

  @D37 @Q2 @NFR-04
  Scenario: A name of exactly 3 characters is accepted
    When I create a vocabulary with a fresh 3-character name
    Then the response status is 201
    And the response field "name" is "${saved:shortName}"

  @AC_SIMPL-18170_AC3 @D37 @D8 @Q2 @NFR-04 @negative
  Scenario Outline: Description and changelog must hold 1 to 1000 characters after trimming
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value          |
      | name           | ${Limits}      |
      | description    | <description>  |
      | changelog      | <changelog>    |
      | vocabularyFile | default        |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "<field>"
    And no vocabulary whose name contains "${uid}" is stored

    Examples:
      | field       | description          | changelog            |
      | description | {{repeat:d:1001}}    | Initial version.     |
      | description | {{repeat: :5}}       | Initial version.     |
      | changelog   | Some text            | {{repeat:c:1001}}    |
      | changelog   | Some text            | {{repeat: :5}}       |

  @D37 @Q2 @NFR-04
  Scenario: Description and changelog of exactly 1000 characters are accepted
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value             |
      | name           | ${Limits}         |
      | description    | {{repeat:d:1000}} |
      | changelog      | {{repeat:c:1000}} |
      | vocabularyFile | default           |
    Then the response status is 201

  @D37 @Q2
  Scenario: The 1000-character limit applies after trimming
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value                              |
      | name           | ${Limits}                          |
      | description    | {{repeat: :3}}{{repeat:d:1000}}{{repeat: :3}} |
      | changelog      | Initial version.                   |
      | vocabularyFile | default                            |
    Then the response status is 201

  @AC_SIMPL-18169_AC2 @D15 @D37 @D51 @negative @API-46 @BLG-prb-type
  Scenario: A duplicate vocabulary name is refused with 409
    Given a vocabulary "Health" exists
    When I create the vocabulary "Health"
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And the problem detail is "Vocabulary with name '${Health}' already exists."
    And 1 vocabulary whose name contains "${Health}" is stored

  @D37 @D54 @negative
  Scenario: Names are unique regardless of case
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | ${Health.upper}  |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"
    And 1 vocabulary whose name contains "${Health}" is stored

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, not a validation report
    Given a vocabulary "Health" exists
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value                      |
      | name           | ${Health}                  |
      | description    | Some text                  |
      | changelog      | Initial version.           |
      | vocabularyFile | fixture:syntax_error.ttl   |
    Then the response is a problem with status 409 and type "urn:problem-type:simpl:conflict"

  @D52 @D8 @negative @BLG-req-valid @OWA-API3
  Scenario: An unknown form part is rejected with one issue naming it
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | ${Extra}         |
      | description    | Some text        |
      | changelog      | Initial version. |
      | status         | DEPRECATED       |
      | vocabularyFile | default          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named "status"
    And no vocabulary whose name contains "${uid}" is stored

  @D70 @D52 @negative
  Scenario: A multipart part with an empty name is an unknown field
    When I send a POST request to "/vocabularies" with these form parts
      | part           | value            |
      | name           | ${Extra}         |
      | description    | Some text        |
      | changelog      | Initial version. |
      | <empty name>   | stray            |
      | vocabularyFile | default          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "body" named ""
    And no vocabulary whose name contains "${uid}" is stored

  @D52 @negative @BLG-req-valid
  Scenario: A query parameter on POST /vocabularies is rejected
    When I send a POST request to "/vocabularies?dryRun=true" with these form parts
      | part           | value            |
      | name           | ${Extra}         |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 400 and type "urn:problem-type:belgif:badRequest"
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary whose name contains "${uid}" is stored

  @AC_SIMPL-18169_AC2 @D15 @D48 @negative @API-38 @API-46 @API-47
  Scenario: A create request that is not multipart/form-data is refused with 415
    When I send a POST request to "/vocabularies" with JSON body
      """
      {"name": "${Json}", "description": "Some text", "changelog": "Initial version."}
      """
    Then the response is a problem with status 415 and type "urn:problem-type:simpl:unsupportedMediaType"
    And no vocabulary whose name contains "${uid}" is stored

  @D63 @D48 @negative @API-37 @API-46
  Scenario: A create request that accepts only XML is refused with 406
    When I send a POST request to "/vocabularies" with header "Accept" set to "application/xml" and these form parts
      | part           | value            |
      | name           | ${Xml}           |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 406 and type "urn:problem-type:simpl:notAcceptable"
    And the problem title is "Not Acceptable"
    And the problem has no issues
    And no vocabulary whose name contains "${uid}" is stored

  @D63
  Scenario Outline: Accept headers a JSON operation accepts
    When I send a POST request to "/vocabularies" with header "Accept" set to "<accept>" and these form parts
      | part           | value            |
      | name           | ${Ok}            |
      | description    | Some text        |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response status is 201

    Examples:
      | accept                   |
      | application/json         |
      | application/problem+json |
      | */*                      |
      | <none>                   |
