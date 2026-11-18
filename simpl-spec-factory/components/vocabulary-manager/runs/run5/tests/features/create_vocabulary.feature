Feature: Create a new vocabulary via the API
  POST /vocabularies stores the vocabulary and its version 1 as ACTIVE in one request
  when the content passes validation (publish, D6). Nothing is stored on failure (D8).

  Background:
    Given I am a Governance Authority user with role "GA_VOCABULARY_ADMIN"

  @AC_SIMPL-18170_AC1 @AC_SIMPL-18187_AC1 @AC_SIMPL-18186_AC1 @D5 @D6 @D10 @D26 @D27 @D31 @D58 @E2 @API-12 @API-36
  Scenario: A valid vocabulary is created as version 1 with status ACTIVE
    When I create the vocabulary "{Alpha}"
    Then the response status is 201
    And the response content type is "application/json"
    And the response header "Location" is "/vocabularies/{Alpha}/versions/1"
    And the response field "href" is "/vocabularies/{Alpha}/versions/1"
    And the response field "contentHref" ends with "/content/{Alpha}/versions/1"
    And the response field "name" is "{Alpha}"
    And the response field "version" equals 1
    And the response field "status" is "ACTIVE"
    And the response field "description" is "Description of {Alpha}."
    And the response field "changelog" is "Initial version."
    And the response field "createdBy" identifies the user with role "GA_VOCABULARY_ADMIN"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    And the response field "validationWarnings" equals []

  @AC_SIMPL-18170_AC2 @AC_SIMPL-18187_AC1 @D3 @D5 @D25 @D27 @D28 @D58
  Scenario: The created vocabulary can be read with its metadata and version 1
    Given a vocabulary "{Alpha}" exists with the description "Healthcare resource types of {run}."
    When I send "GET" to "/vocabularies/{Alpha}"
    Then the response status is 200
    And the response field "href" is "/vocabularies/{Alpha}"
    And the response field "contentHref" ends with "/content/{Alpha}"
    And the response field "name" is "{Alpha}"
    And the response field "version" equals 1
    And the response field "description" is "Healthcare resource types of {run}."
    And the response field "status" is "ACTIVE"
    And the response field "createdBy" identifies the user with role "GA_VOCABULARY_ADMIN"
    And the response field "creationDate" is a UTC time to the second
    And the response field "lastUpdateDate" is a UTC time to the second
    And version 1 of "{Alpha}" has status "ACTIVE"
    And version 1 of "{Alpha}" has the field "changelog" equal to "Initial version."
    And version 1 of "{Alpha}" has the field "latestVersionHref" equal to "/vocabularies/{Alpha}/versions/1"

  @AC_SIMPL-18170_AC2 @D3 @D4
  Scenario: The vocabulary status can be changed to DEPRECATED and back to ACTIVE
    Given a vocabulary "{Alpha}" exists
    When I change the status of "{Alpha}" to "DEPRECATED"
    Then the response status is 200
    And the vocabulary "{Alpha}" has status "DEPRECATED"
    When I change the status of "{Alpha}" to "ACTIVE"
    Then the response status is 200
    And the vocabulary "{Alpha}" has status "ACTIVE"

  @AC_SIMPL-18187_AC1 @D29
  Scenario: The created content is available for download unchanged
    Given a vocabulary "{Alpha}" exists
    When I send "GET" to "/content/{Alpha}/versions/1"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body is the content of version 1 of "{Alpha}"

  @AC_SIMPL-18170_AC3 @D37 @D8 @D24 @negative @OWT-A05 @BLG-err-problem
  Scenario Outline: Invalid or incomplete metadata is rejected and nothing is stored (<case>)
    When I create a vocabulary with the form
      | field          | value         |
      | name           | <name>        |
      | description    | <description> |
      | changelog      | <changelog>   |
      | vocabularyFile | default       |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And no vocabulary named "<name>" is stored

    Examples:
      | case                          | name                       | description       | changelog         | field       |
      | name starts in lower case     | alpha{run}                 | A description.    | Initial version.  | name        |
      | name too short                | Ab                         | A description.    | Initial version.  | name        |
      | name with a hyphen            | Alpha-{run}                | A description.    | Initial version.  | name        |
      | name with a space             | Alpha {run}                | A description.    | Initial version.  | name        |
      | name of 65 characters         | Z{run}{chars:54}           | A description.    | Initial version.  | name        |
      | description empty             | {Alpha}                    |                   | Initial version.  | description |
      | description blank             | {Alpha}                    | {blank}           | Initial version.  | description |
      | description of 1001 chars     | {Alpha}                    | {chars:1001}      | Initial version.  | description |
      | changelog empty               | {Alpha}                    | A description.    |                   | changelog   |
      | changelog blank               | {Alpha}                    | A description.    | {blank}           | changelog   |
      | changelog of 1001 chars       | {Alpha}                    | A description.    | {chars:1001}      | changelog   |

  @AC_SIMPL-18170_AC3 @D37 @negative
  Scenario: The name "XY" of two characters is rejected
    When I create a vocabulary with the form
      | field          | value            |
      | name           | XY               |
      | description    | A description.   |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "name"

  @D37
  Scenario Outline: Field lengths at the limit are accepted (<case>)
    When I create a vocabulary with the form
      | field          | value         |
      | name           | <name>        |
      | description    | <description> |
      | changelog      | <changelog>   |
      | vocabularyFile | default       |
    Then the response status is 201

    Examples:
      | case                       | name               | description  | changelog         |
      | name of 64 characters      | Z{run}{chars:53}   | Limits.      | Initial version.  |
      | description of 1000 chars  | {Alpha}            | {chars:1000} | Initial version.  |
      | changelog of 1000 chars    | {Alpha}            | Limits.      | {chars:1000}      |
      | description of one char    | {Alpha}            | D            | C                 |

  @AC_SIMPL-18170_AC3 @D37 @D8 @negative
  Scenario Outline: A request without a required field is rejected (<field> missing)
    When I create a vocabulary with the form
      | field   | value   |
      | <keep1> | <val1>  |
      | <keep2> | <val2>  |
      | <keep3> | <val3>  |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And no vocabulary of this scenario is stored

    Examples:
      | field          | keep1       | val1            | keep2       | val2           | keep3          | val3             |
      | name           | description | A description.  | changelog   | Initial.       | vocabularyFile | default          |
      | description    | name        | {Alpha}         | changelog   | Initial.       | vocabularyFile | default          |
      | changelog      | name        | {Alpha}         | description | A description. | vocabularyFile | default          |
      | vocabularyFile | name        | {Alpha}         | description | A description. | changelog      | Initial version. |

  @AC_SIMPL-18170_AC3 @D9 @D8 @negative
  Scenario: An empty vocabulary file is rejected and nothing is stored
    When I create a vocabulary with the form
      | field          | value            |
      | name           | {Alpha}          |
      | description    | A description.   |
      | changelog      | Initial version. |
      | vocabularyFile | empty            |
    Then the response is a problem with status 400
    And no vocabulary named "{Alpha}" is stored

  @AC_SIMPL-18170_AC3 @D15 @D37 @D48 @D8 @negative @API-46
  Scenario: A duplicate vocabulary name is a conflict
    Given a vocabulary "{Alpha}" exists
    When I create the vocabulary "{Alpha}"
    Then the response is a problem with status 409
    And the problem detail is "Vocabulary with name '{Alpha}' already exists."
    And the problem type follows the simpl URN form
    And the vocabulary "{Alpha}" has 1 version

  @AC_SIMPL-18170_AC3 @D37 @D54 @negative
  Scenario: Names are unique without regard to case
    Given a vocabulary "{Alpha}" exists
    When I create the vocabulary "{upper:Alpha}"
    Then the response is a problem with status 409
    And exactly one vocabulary named "{Alpha}" is stored

  @D51 @D15 @negative
  Scenario: A duplicate name with invalid content gives 409, not 400 (order of checks)
    Given a vocabulary "{Alpha}" exists
    When I create a vocabulary with the form
      | field          | value                    |
      | name           | {Alpha}                  |
      | description    | A description.           |
      | changelog      | Initial version.         |
      | vocabularyFile | fixture:syntax-error.ttl |
    Then the response is a problem with status 409

  @D51 @negative
  Scenario: A field error with invalid content gives the field error (400 before validation)
    When I create a vocabulary with the form
      | field          | value                    |
      | name           | {Alpha}                  |
      | description    | {blank}                  |
      | changelog      | Initial version.         |
      | vocabularyFile | fixture:syntax-error.ttl |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "description"

  @AC_SIMPL-18173_AC6 @D15 @D48 @D8 @negative @API-38 @API-46
  Scenario Outline: A create request that is not multipart/form-data gets 415 (<content type>)
    When I send "POST" to "/vocabularies" with the body "<body>" as "<content type>"
    Then the response is a problem with status 415
    And the problem type follows the simpl URN form
    And no vocabulary of this scenario is stored

    Examples:
      | content type      | body                                               |
      | application/json  | {}                                                 |
      | text/turtle       | @prefix ex: <https://vocab.example.org/x#> .       |
      | text/plain        | name={Alpha}                                       |

  @D15 @D48 @D8 @negative @API-46
  Scenario: A vocabulary file above 5 MB gets 413 and nothing is stored
    When I create a vocabulary with the form
      | field          | value            |
      | name           | {Alpha}          |
      | description    | Too large.       |
      | changelog      | Initial version. |
      | vocabularyFile | size:5242881     |
    Then the response is a problem with status 413
    And the problem type follows the simpl URN form
    And no vocabulary named "{Alpha}" is stored

  @D15
  Scenario: A vocabulary file of exactly 5 MB is accepted
    When I create a vocabulary with the form
      | field          | value            |
      | name           | {Alpha}          |
      | description    | At the limit.    |
      | changelog      | Initial version. |
      | vocabularyFile | size:5242880     |
    Then the response status is 201

  @D51 @D15 @negative
  Scenario: Size is checked before the fields (413 before 400)
    When I create a vocabulary with the form
      | field          | value        |
      | name           | lowercase    |
      | vocabularyFile | size:5242881 |
    Then the response is a problem with status 413

  @D52 @D8 @negative @OWA-API3 @BLG-req-valid
  Scenario Outline: A form field the API does not define is rejected (<field>)
    When I create a vocabulary with the form
      | field          | value            |
      | name           | {Alpha}          |
      | description    | A description.   |
      | changelog      | Initial version. |
      | <field>        | <value>          |
      | vocabularyFile | default          |
    Then the response is a problem with status 400
    And the problem has an issue in "body" named "<field>"
    And no vocabulary named "{Alpha}" is stored

    Examples:
      | field          | value                |
      | colour         | blue                 |
      | status         | DEPRECATED           |
      | createdBy      | someone@example.com  |
      | version        | 7                    |
      | creationDate   | 2020-01-01T00:00:00Z |

  @D52 @D8 @negative
  Scenario: A query parameter on POST /vocabularies is rejected
    When I send "POST" to "/vocabularies?dryRun=true" with the form
      | field          | value            |
      | name           | {Alpha}          |
      | description    | A description.   |
      | changelog      | Initial version. |
      | vocabularyFile | default          |
    Then the response is a problem with status 400
    And the problem has an issue in "query" named "dryRun"
    And no vocabulary named "{Alpha}" is stored

  @D43 @D37
  Scenario: Two overlapping creations with the same name give one 201 and one 409
    When I create the vocabulary "{Alpha}" 2 times in parallel
    Then the parallel responses have the statuses [201, 409]
    And exactly one vocabulary named "{Alpha}" is stored

  @D1 @D6
  Scenario: A CPSV-like vocabulary can be uploaded and found in the list (SIMPL-393 by generic upload)
    When I create the vocabulary "{Cpsv}" with the fixture "cpsv-like.ttl"
    Then the response status is 201
    And the vocabulary list filtered by name "{run}" has the names ["{Cpsv}"]

  @D57 @D29
  Scenario: Relative IRIs are accepted and the stored bytes stay as uploaded
    When I create the vocabulary "{Alpha}" with the fixture "relative-iris.ttl"
    Then the response status is 201
    And the content of version 1 of "{Alpha}" is unchanged
