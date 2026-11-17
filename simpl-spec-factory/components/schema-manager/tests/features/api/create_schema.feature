Feature: Create a schema through the API
  SIMPL-13353 and its enhancement SIMPL-32802, with the version rules of
  SIMPL-20528. Multipart with the flat OpenAPI form fields (D29).

  @SIMPL-13353 @SIMPL-32802 @SIMPL-20528 @D1 @D2 @D14 @D28 @D29 @D63
  Scenario: An admin creates a RESOURCE schema without a schemaType
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response status is 201
    And the response content type is "application/json"
    And the response header "Location" is an absolute URI ending with "/schemas/$A"
    And the JSON field "schemaName" is "$A"
    And the JSON field "schemaVersion" is "1"
    And the JSON field "validationWarnings" is an empty list
    And the JSON field "status" is "PUBLISHED"
    And the JSON field "schemaType" is "RESOURCE"
    And the JSON field "resourceType" is "$RT"
    And schema "$A" is listed with
      | field        | value               |
      | name         | $A                  |
      | title        | Title of $A         |
      | description  | Description of $A   |
      | resourceType | $RT                 |
      | schemaType   | RESOURCE            |
      | version      | 1                   |
      | status       | PUBLISHED           |
      | created      | <timestamp>         |

  @SIMPL-32802 @SIMPL-33537
  Scenario: An explicit schemaType RESOURCE behaves like the default
    When I create the RESOURCE schema "$A" for resource type "$RT" with
      | field      | value    |
      | schemaType | RESOURCE |
    Then the response status is 201
    And the JSON field "schemaType" is "RESOURCE"

  @SIMPL-13353 @D19
  Scenario: The first version gets the changelog "Initial version."
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    Then version "1" of schema "$A" has the changelog "Initial version."
    And schema "$A" has the versions "1"

  @SIMPL-13353 @D58
  Scenario: The stored content is the uploaded file, byte for byte
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I request version "1" of schema "$A"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$A"

  @SIMPL-20528 @SIMPL-20534 @D14
  Scenario: A client-supplied version field is ignored on create
    When I create the RESOURCE schema "$A" for resource type "$RT" with
      | field   | value |
      | version | 1.0.0 |
    Then the response status is 201
    And the JSON field "schemaVersion" is "1"

  @SIMPL-13353 @SIMPL-32802 @D12 @D17 @D49
  Scenario: A second schema with the same name is rejected, whatever its resource type
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I create the RESOURCE schema "$A" for resource type "$RT2"
    Then the response is a "conflict" problem with status 409
    And the problem detail is "A schema with that name already exists."
    And listing "schemaType=RESOURCE&resourceType=$RT2" is empty

  @SIMPL-32802 @D12
  Scenario: A second schema with the same name and resource type is rejected
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response is a "conflict" problem with status 409
    And schema "$A" has the versions "1"

  @SIMPL-32801 @SIMPL-32802 @D12
  Scenario: The name of a revoked schema cannot be reused
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I create the RESOURCE schema "$A" for resource type "$RT"
    Then the response is a "conflict" problem with status 409

  @SIMPL-13353 @D12
  Scenario: Names are case-sensitive
    Given I am "ga-admin"
    When I create the RESOURCE schema "$A" for resource type "$RT" with
      | field | value       |
      | name  | CaseTest$A  |
    Then the response status is 201
    When I create the RESOURCE schema "$B" for resource type "$RT2" with
      | field | value       |
      | name  | Casetest$A  |
    Then the response status is 201

  @SIMPL-13353 @SIMPL-32802 @D12
  Scenario Outline: An invalid name is rejected
    When I create the RESOURCE schema "$A" for resource type "$RT" with
      | field | value  |
      | name  | <name> |
    Then the response is a "badRequest" problem with status 400

    Examples:
      | name                                                              |
      | lowerCase$A                                                       |
      | Ab                                                                |
      | Data Asset$A                                                      |
      | Data-Asset$A                                                      |
      | Abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcde |

  @SIMPL-32802 @D13
  Scenario Outline: An invalid resourceType is rejected
    When I create the RESOURCE schema "$A" for resource type "<rt>"
    Then the response is a "badRequest" problem with status 400

    Examples:
      | rt          |
      | data        |
      | data-schema |
      | Da          |
      | Data Type   |

  @SIMPL-32802 @D13 @D20
  Scenario: A RESOURCE schema without resourceType is rejected
    When I create the RESOURCE schema "$A" without a resource type
    Then the response is a "badRequest" problem with status 400

  @SIMPL-32802 @SIMPL-33537 @D13
  Scenario: A SEARCH schema with a resourceType is rejected
    When I create the SEARCH schema "$S" with
      | field        | value |
      | resourceType | $RT   |
    Then the response is a "badRequest" problem with status 400
    And schema "$S" is not listed as a SEARCH schema

  @SIMPL-32802 @D27
  Scenario: An unknown schemaType is rejected
    When I create the RESOURCE schema "$A" for resource type "$RT" with
      | field      | value |
      | schemaType | OTHER |
    Then the response is a "badRequest" problem with status 400

  @SIMPL-13353 @D20
  Scenario Outline: A missing or blank required field is rejected
    When I create the RESOURCE schema "$A" for resource type "$RT" with
      | field   | value   |
      | <field> | <value> |
    Then the response is a "badRequest" problem with status 400
    And listing "resourceType=$RT" is empty

    Examples:
      | field       | value  |
      | name        | <omit> |
      | title       | <omit> |
      | description | <omit> |
      | schemaFile  | <omit> |
      | title       |        |
      | description |        |

  @SIMPL-13353 @SIMPL-32802 @D8 @openapi-only
  Scenario: A create that is not multipart is rejected with 415
    When I send a POST request to "/schemas" with content type "application/json" and body
      """
      {"name": "$A", "title": "T", "description": "D", "resourceType": "$RT"}
      """
    Then the response is a "unsupportedMediaType" problem with status 415

  @SIMPL-13353 @D8 @openapi-only
  Scenario: A create with an unacceptable Accept header gives 406
    When I create the RESOURCE schema "$A" for resource type "$RT" with header "Accept" set to "application/xml"
    Then the response is a "notAcceptable" problem with status 406
    And listing "resourceType=$RT" is empty

  @SIMPL-13353 @D23
  Scenario: A name conflict is reported before content validation
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I create the RESOURCE schema "$A" for resource type "$RT" from "syntax-error.ttl"
    Then the response is a "conflict" problem with status 409

  @SIMPL-13353 @D23
  Scenario: Metadata validation comes before content validation
    When I create the RESOURCE schema "$A" for resource type "data" from "syntax-error.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail does not start with "Syntax Error: "
