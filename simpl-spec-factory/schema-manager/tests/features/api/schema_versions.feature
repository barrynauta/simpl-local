Feature: Version history of a schema through the API
  SIMPL-13345 and its enhancement SIMPL-32786: a paginated list of versions,
  selected by schemaType (default RESOURCE).

  @SIMPL-13345 @SIMPL-32786 @D25
  Scenario: A viewer lists the versions of a schema with all metadata
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And I am "ga-viewer"
    When I request the versions of schema "$A"
    Then the response status is 200
    And the response content type is "application/json"
    And the JSON object has the fields "page,totalPages,total,pageSize,next,metadata"
    And the JSON field "total" is the number 2
    And every item of "metadata" has the fields "name,version,changelog,created,createdBy,schemaType,resourceType,status,graphName"
    And the versions in "metadata" are "1,2"
    And the JSON field "metadata[0].name" is "$A"
    And the JSON field "metadata[0].changelog" is "Initial version."
    And the JSON field "metadata[1].changelog" is "Change number 2 of $A"
    And the JSON field "metadata[0].schemaType" is "RESOURCE"
    And the JSON field "metadata[0].resourceType" is "$RT"
    And the JSON field "metadata[0].createdBy" is "ga-admin"
    And the JSON field "metadata[0].created" is an ISO timestamp
    And the JSON field "metadata[0].graphName" is an absolute URI ending with "/schemas/$A/1"
    And the JSON field "metadata[1].graphName" is an absolute URI ending with "/schemas/$A/2"

  @SIMPL-13345 @SIMPL-20530 @D16 @D25
  Scenario: Version status is derived from the concept
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    When I request the versions of schema "$A"
    Then the JSON field "metadata[0].status" is "REVOKED"
    And the JSON field "metadata[1].status" is "PUBLISHED"

  @SIMPL-13345 @D26
  Scenario: Versions are ordered numerically, so version 10 comes after version 2
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has 10 new versions
    When I request the versions of schema "$A"
    Then the response status is 200
    And the versions in "metadata" are "1,2,3,4,5,6,7,8,9,10,11"

  @SIMPL-13345 @D26
  Scenario: Versions are paged and the next link keeps the schema type
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has 2 new versions
    When I send a GET request to "/schemas/$A/versions?pageSize=2"
    Then the response status is 200
    And the JSON field "totalPages" is the number 2
    And the versions in "metadata" are "1,2"
    And the JSON field "next" contains "page=2"
    When I follow the "next" link
    Then the response status is 200
    And the versions in "metadata" are "3"
    And the JSON field "next" is null

  @SIMPL-13345 @D26 @openapi-only
  Scenario: A versions page past the end gives 404
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/versions?page=3"
    Then the response is a "notFound" problem with status 404

  @SIMPL-32786 @D11
  Scenario: An explicit schemaType RESOURCE selects the same versions as the default
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/versions?schemaType=RESOURCE"
    Then the response status is 200
    And the versions in "metadata" are "1"

  @SIMPL-32786 @D11
  Scenario: A matching resourceType filter selects the RESOURCE concept
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/versions?resourceType=$RT"
    Then the response status is 200
    And the versions in "metadata" are "1"

  @SIMPL-32786 @D11
  Scenario: A different resourceType gives 404
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/versions?resourceType=$RT2"
    Then the response is a "notFound" problem with status 404

  @SIMPL-32786 @D11
  Scenario: A RESOURCE schema is not found with schemaType SEARCH
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/versions?schemaType=SEARCH"
    Then the response is a "notFound" problem with status 404

  @SIMPL-33537 @D13 @D27
  Scenario: resourceType with schemaType SEARCH is rejected on the versions list
    When I send a GET request to "/schemas/AnySchema/versions?schemaType=SEARCH&resourceType=Data"
    Then the response is a "badRequest" problem with status 400

  @SIMPL-13345 @SIMPL-32786 @D12
  Scenario Outline: An invalid schema name in the path gives 400
    Given I am "ga-viewer"
    When I send a GET request to "/schemas/<name>/versions"
    Then the response is a "badRequest" problem with status 400

    Examples:
      | name                                                              |
      | Infrastructure%24chema%21                                         |
      | infrastructureSchema                                              |
      | Ab                                                                |
      | Infrastructure%20Schema                                           |
      | Abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcde |

  @SIMPL-13345 @SIMPL-32786
  Scenario: An unknown schema gives 404
    Given I am "ga-viewer"
    When I send a GET request to "/schemas/UnknownSchema$A/versions"
    Then the response is a "notFound" problem with status 404

  @SIMPL-32786 @D26
  Scenario Outline: Invalid paging parameters on the versions list are rejected
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a GET request to "/schemas/$A/versions?<query>"
    Then the response is a "badRequest" problem with status 400

    Examples:
      | query        |
      | page=0       |
      | pageSize=0   |
      | pageSize=101 |
      | schemaType=X |
