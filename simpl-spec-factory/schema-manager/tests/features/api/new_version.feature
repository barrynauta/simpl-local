Feature: Create a new version of a schema through the API
  SIMPL-13357, replaced in part by SIMPL-32807 (schemaType, no client version)
  and SIMPL-20534 (server-assigned next number).

  @SIMPL-13357 @SIMPL-32807 @SIMPL-20534 @D2 @D14 @D16 @D28
  Scenario: An admin adds a version to a published RESOURCE schema without a schemaType
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-admin"
    When I add a new version to schema "$A"
    Then the response status is 201
    And the response header "Location" is an absolute URI ending with "/schemas/$A/2"
    And the JSON field "name" is "$A"
    And the JSON field "title" is "Title of $A"
    And the JSON field "description" is "Description of $A"
    And the JSON field "resourceType" is "$RT"
    And the JSON field "schemaType" is "RESOURCE"
    And the JSON field "version" is "2"
    And the JSON field "changelog" is "Change number 2 of $A"
    And the JSON field "status" is "PUBLISHED"
    And the JSON field "validationWarnings" is an empty list
    And version "2" of schema "$A" has status "PUBLISHED"
    And version "1" of schema "$A" has status "REVOKED"
    And schema "$A" has status "PUBLISHED"

  @SIMPL-32807 @SIMPL-33537
  Scenario: An explicit schemaType RESOURCE behaves like the default
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" with
      | field      | value    |
      | schemaType | RESOURCE |
    Then the response status is 201
    And the JSON field "version" is "2"
    And the JSON field "schemaType" is "RESOURCE"

  @SIMPL-20534 @D14
  Scenario: Each new version is the highest number plus one
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has 2 new versions
    When I add a new version to schema "$A"
    Then the response status is 201
    And the JSON field "version" is "4"
    And schema "$A" has the versions "1,2,3,4"

  @SIMPL-20534 @SIMPL-32807 @D14 @D15
  Scenario: A client-supplied version field is ignored and never causes 409
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" with
      | field   | value |
      | version | 1     |
    Then the response status is 201
    And the JSON field "version" is "2"
    When I add a new version to schema "$A" with
      | field   | value |
      | version | 1.1.0 |
    Then the response status is 201
    And the JSON field "version" is "3"

  @SIMPL-13357 @D58
  Scenario: The new version stores the uploaded content
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A"
    And I request version "2" of schema "$A"
    Then the response body is the content uploaded for version "2" of schema "$A"

  @SIMPL-13357 @SIMPL-32807
  Scenario: A version for an unknown RESOURCE schema gives 404
    Given I am "ga-admin"
    When I add a new version to the unknown schema "NonExistent$A" with schema type "RESOURCE"
    Then the response is a "notFound" problem with status 404

  @SIMPL-32807 @D11
  Scenario: A version for a RESOURCE name with schemaType SEARCH gives 404
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" with
      | field      | value  |
      | schemaType | SEARCH |
    Then the response is a "notFound" problem with status 404
    And schema "$A" has the versions "1"

  @SIMPL-32807
  Scenario: A version for an unknown SEARCH schema gives 404
    When I add a new version to the unknown schema "NonExistent$S" with schema type "SEARCH"
    Then the response is a "notFound" problem with status 404

  @SIMPL-13357 @SIMPL-32807 @D20
  Scenario Outline: Missing or blank metadata is rejected
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" with
      | field   | value   |
      | <field> | <value> |
    Then the response is a "badRequest" problem with status 400
    And schema "$A" has the versions "1"

    Examples:
      | field      | value  |
      | changelog  | <omit> |
      | changelog  |        |
      | schemaFile | <omit> |

  @SIMPL-32807 @D20
  Scenario: A different resourceType on a new version is rejected
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" with
      | field        | value |
      | resourceType | $RT2  |
    Then the response is a "badRequest" problem with status 400
    And schema "$A" has the versions "1"

  @SIMPL-32807 @D20
  Scenario: The same resourceType on a new version is accepted
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" with
      | field        | value |
      | resourceType | $RT   |
    Then the response status is 201

  @SIMPL-13357 @SIMPL-32807 @D30
  Scenario: An invalid SHACL file is rejected and the history is unchanged
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I add a new version to schema "$A" from "syntax-error.ttl"
    Then the response is a "badRequest" problem with status 400
    And the problem detail starts with "Syntax Error: "
    And schema "$A" has the versions "1"
    And version "1" of schema "$A" has status "PUBLISHED"

  @SIMPL-13357 @D12
  Scenario: An invalid schema name in the path gives 400 before the lookup
    When I add a new version to the unknown schema "lowercase" with schema type "RESOURCE"
    Then the response is a "badRequest" problem with status 400

  @SIMPL-13357 @D23
  Scenario: An unknown schema gives 404 before content validation
    When I add a new version to schema "$NOPE" from "syntax-error.ttl"
    Then the response is a "notFound" problem with status 404

  @SIMPL-13357 @D8 @openapi-only
  Scenario: A new version that is not multipart is rejected with 415
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a POST request to "/schemas/$A/versions" with content type "application/json" and body
      """
      {"changelog": "A change."}
      """
    Then the response is a "unsupportedMediaType" problem with status 415
