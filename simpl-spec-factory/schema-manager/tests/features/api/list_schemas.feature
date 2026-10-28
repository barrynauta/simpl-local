Feature: List schemas through the API
  SIMPL-13343 and its extension SIMPL-32258: a paginated list of schema
  metadata, filtered by status, resourceType and schemaType (default RESOURCE).

  @SIMPL-13343 @SIMPL-32258 @D24
  Scenario: A viewer lists schemas and each item carries the metadata
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-viewer"
    When I list schemas with "resourceType=$RT"
    Then the response status is 200
    And the response content type is "application/json"
    And the JSON field "metadata" is a list of 1 items
    And every item of "metadata" has the fields "name,title,description,resourceType,schemaType,version,changelog,status,created,modified,createdBy"
    And the JSON field "metadata[0].name" is "$A"
    And the JSON field "metadata[0].title" is "Title of $A"
    And the JSON field "metadata[0].description" is "Description of $A"
    And the JSON field "metadata[0].resourceType" is "$RT"
    And the JSON field "metadata[0].schemaType" is "RESOURCE"
    And the JSON field "metadata[0].version" is "1"
    And the JSON field "metadata[0].changelog" is "Initial version."
    And the JSON field "metadata[0].status" is "PUBLISHED"
    And the JSON field "metadata[0].created" is an ISO timestamp
    And the JSON field "metadata[0].modified" is an ISO timestamp
    And the JSON field "metadata[0].createdBy" is "ga-admin"

  @SIMPL-13343 @SIMPL-32258 @D24
  Scenario: The list item shows the version and changelog of the highest version
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    When I list schemas with "resourceType=$RT"
    Then the response status is 200
    And the JSON field "metadata[0].version" is "2"
    And the JSON field "metadata[0].changelog" is "Change number 2 of $A"

  @SIMPL-13343 @SIMPL-32258 @D26
  Scenario: The list uses the Belgif paging envelope with 20 items per page by default
    Given I am "ga-admin"
    When I list schemas
    Then the response status is 200
    And the JSON object has the fields "page,totalPages,total,pageSize,next,metadata"
    And the JSON field "page" is the number 1
    And the JSON field "pageSize" is the number 20

  @SIMPL-13343 @D26
  Scenario: Pages follow the requested size and the next link keeps the filters
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And the RESOURCE schema "$C" exists for resource type "$RT"
    When I list schemas with "resourceType=$RT&pageSize=2"
    Then the response status is 200
    And the JSON field "page" is the number 1
    And the JSON field "pageSize" is the number 2
    And the JSON field "total" is the number 3
    And the JSON field "totalPages" is the number 2
    And the names in "metadata" are "$A,$B"
    And the JSON field "next" contains "resourceType=$RT"
    And the JSON field "next" contains "page=2"
    When I follow the "next" link
    Then the response status is 200
    And the JSON field "page" is the number 2
    And the names in "metadata" are "$C"
    And the JSON field "next" is null

  @SIMPL-13343 @D26
  Scenario: A page past the end of the schema list is empty
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I list schemas with "resourceType=$RT&page=5"
    Then the response status is 200
    And the JSON field "metadata" is an empty list

  @SIMPL-13343 @D26
  Scenario: The schema list is ordered by name
    Given the RESOURCE schema "$C" exists for resource type "$RT"
    And the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    When I list schemas with "resourceType=$RT"
    Then the names in "metadata" are "$A,$B,$C"

  @SIMPL-13343 @D26
  Scenario Outline: Invalid paging parameters are rejected
    When I list schemas with "<query>"
    Then the response is a "badRequest" problem with status 400

    Examples:
      | query        |
      | page=0       |
      | page=abc     |
      | pageSize=0   |
      | pageSize=101 |
      | pageSize=x   |

  @SIMPL-13343 @SIMPL-32258 @D27
  Scenario: Status and resourceType filters combine with AND
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And the RESOURCE schema "$C" exists for resource type "$RT2"
    When I list schemas with "status=REVOKED&resourceType=$RT"
    Then the response status is 200
    And the names in "metadata" are "$B"
    When I list schemas with "status=PUBLISHED&resourceType=$RT"
    Then the names in "metadata" are "$A"

  @SIMPL-32258 @SIMPL-33537 @D27
  Scenario: Without schemaType the list returns RESOURCE schemas only
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I list schemas with "resourceType=$RT"
    Then every item of "metadata" has "schemaType" equal to "RESOURCE"
    When I list schemas with "schemaType=RESOURCE&resourceType=$RT"
    Then the names in "metadata" are "$A"

  @SIMPL-32258 @SIMPL-33537 @D27
  Scenario: The schemaType filter SEARCH returns SEARCH schemas only
    Given I am "ga-viewer"
    When I list schemas with "schemaType=SEARCH&pageSize=100"
    Then the response status is 200
    And no item of "metadata" has "schemaType" other than "SEARCH"

  @SIMPL-13343 @SIMPL-32258 @D27
  Scenario Outline: Invalid filter values give a problem
    When I list schemas with "<query>"
    Then the response is a "badRequest" problem with status 400

    Examples:
      | query                           |
      | status=INVALID_STATUS           |
      | status=published                |
      | schemaType=OTHER                |
      | resourceType=data-schema        |
      | resourceType=data               |
      | resourceType=Ab                 |

  @SIMPL-32258 @SIMPL-33537 @D13 @D27
  Scenario: resourceType together with schemaType SEARCH is rejected
    When I list schemas with "schemaType=SEARCH&resourceType=Data"
    Then the response is a "badRequest" problem with status 400

  @SIMPL-13343 @D27
  Scenario: Unknown query parameters are ignored
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I list schemas with "resourceType=$RT&colour=blue"
    Then the response status is 200
    And the names in "metadata" are "$A"
