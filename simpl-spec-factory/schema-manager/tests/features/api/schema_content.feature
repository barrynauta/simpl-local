Feature: Raw schema content through the API
  SIMPL-13347 and SIMPL-32788: GET /schemas/{schemaName} serves the content of
  the highest version (D9, not in the OpenAPI). SIMPL-13364 and SIMPL-32798:
  GET /schemas/{schemaName}/{version} serves one version, or its metadata on
  Accept application/json (D10).

  @SIMPL-13347 @SIMPL-32788 @D9 @D2 @D6
  Scenario: The latest content is the raw content of the highest version
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am "ga-viewer"
    When I request the latest content of schema "$A"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response header "Content-Location" is an absolute URI ending with "/schemas/$A/2"
    And the response body is the content uploaded for version "2" of schema "$A"

  @SIMPL-32788 @D9 @D11
  Scenario: An explicit schemaType RESOURCE gives the same content
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A?schemaType=RESOURCE"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$A"

  @SIMPL-32788 @D11
  Scenario: A matching resourceType selects the concept, a different one gives 404
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A?resourceType=$RT"
    Then the response status is 200
    When I send a GET request to "/schemas/$A?resourceType=$RT2"
    Then the response is a "notFound" problem with status 404

  @SIMPL-32788 @D11
  Scenario: A RESOURCE schema is not found as SEARCH
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A?schemaType=SEARCH"
    Then the response is a "notFound" problem with status 404

  @SIMPL-13347 @SIMPL-32788 @D9
  Scenario: The latest content of an unknown schema gives 404
    Given I am "ga-viewer"
    When I send a GET request to "/schemas/NonExistentSchema$A"
    Then the response is a "notFound" problem with status 404

  @SIMPL-13347 @D12
  Scenario: An invalid schema name gives 400 before any lookup
    When I send a GET request to "/schemas/nonPascal"
    Then the response is a "badRequest" problem with status 400

  @SIMPL-33537 @D27
  Scenario: resourceType with schemaType SEARCH is rejected on the latest content
    When I send a GET request to "/schemas/AnySchema?schemaType=SEARCH&resourceType=Data"
    Then the response is a "badRequest" problem with status 400

  @SIMPL-13364 @SIMPL-32798 @D10 @D2
  Scenario: A specific version returns its own raw content
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am "ga-viewer"
    When I request version "1" of schema "$A"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body is the content uploaded for version "1" of schema "$A"
    And the response header "Content-Location" is an absolute URI ending with "/schemas/$A/1"
    And the response header "Location" is an absolute URI ending with "/schemas/$A/1"
    When I request version "2" of schema "$A"
    Then the response status is 200
    And the response body is the content uploaded for version "2" of schema "$A"

  @SIMPL-13364 @D8 @D10
  Scenario: Accept text/turtle or */* gives the content
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/1" with header "Accept" set to "text/turtle"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$A"
    When I send a GET request to "/schemas/$A/1" with header "Accept" set to "*/*"
    Then the response status is 200
    And the response content type is "text/turtle"

  @SIMPL-32798 @D10 @D25
  Scenario: Accept application/json gives the metadata of the version
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    When I request version "2" of schema "$A" as JSON
    Then the response status is 200
    And the response content type is "application/json"
    And the JSON field "name" is "$A"
    And the JSON field "version" is "2"
    And the JSON field "changelog" is "Change number 2 of $A"
    And the JSON field "schemaType" is "RESOURCE"
    And the JSON field "resourceType" is "$RT"
    And the JSON field "status" is "PUBLISHED"
    And the JSON field "createdBy" is "ga-admin"
    And the JSON field "created" is an ISO timestamp
    And the JSON field "graphName" is an absolute URI ending with "/schemas/$A/2"

  @SIMPL-32798 @D10 @D16
  Scenario: The metadata of a superseded version shows it as revoked
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    When I request version "1" of schema "$A" as JSON
    Then the response status is 200
    And the JSON field "version" is "1"
    And the JSON field "status" is "REVOKED"

  @SIMPL-13364 @SIMPL-32798 @D10
  Scenario Outline: A version that does not exist gives 404 and no content
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/<version>"
    Then the response is a "notFound" problem with status 404

    Examples:
      | version |
      | 9.9.9   |
      | 2       |
      | 0       |
      | -1      |
      | abc     |
      | 1.0.0   |

  @SIMPL-13364 @SIMPL-32798
  Scenario: A version of an unknown schema gives 404
    When I send a GET request to "/schemas/NeverCreated$A/1"
    Then the response is a "notFound" problem with status 404

  @SIMPL-32798 @D11
  Scenario: The version endpoint selects by schemaType and resourceType
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/1?schemaType=RESOURCE&resourceType=$RT"
    Then the response status is 200
    When I send a GET request to "/schemas/$A/1?schemaType=SEARCH"
    Then the response is a "notFound" problem with status 404
    When I send a GET request to "/schemas/$A/1?resourceType=$RT2"
    Then the response is a "notFound" problem with status 404

  @SIMPL-33537 @D27
  Scenario: resourceType with schemaType SEARCH is rejected on the version endpoint
    When I send a GET request to "/schemas/AnySchema/1?schemaType=SEARCH&resourceType=Data"
    Then the response is a "badRequest" problem with status 400
