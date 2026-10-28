Feature: Change the status of a schema through the API
  SIMPL-13360 (revoke) generalised by SIMPL-20529 (revoke and publish), with
  the slot rule of D16 and the transitions of D18.

  @SIMPL-13360 @SIMPL-20529 @D18
  Scenario: An admin revokes a published schema
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am "ga-admin"
    When I revoke schema "$A"
    Then the response status is 200
    And the response content type is "application/json"
    And the JSON field "name" is "$A"
    And the JSON field "status" is "REVOKED"
    And the JSON field "schemaType" is "RESOURCE"
    And the JSON field "resourceType" is "$RT"
    And the JSON field "latestVersion" is "1"
    And the JSON field "title" is "Title of $A"
    And the JSON field "created" is an ISO timestamp
    And the JSON field "modified" is an ISO timestamp
    And schema "$A" has status "REVOKED"
    And version "1" of schema "$A" has status "REVOKED"

  @SIMPL-20529 @SIMPL-17066 @D16 @D18
  Scenario: An admin publishes a revoked schema when no other schema of its slot is published
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I publish schema "$A"
    Then the response status is 200
    And the JSON field "name" is "$A"
    And the JSON field "status" is "PUBLISHED"
    And schema "$A" has status "PUBLISHED"

  @SIMPL-20529 @D16 @D18
  Scenario: Publishing is refused while another schema of the same resource type is published
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    When I publish schema "$B"
    Then the response is a "conflict" problem with status 409
    And the problem detail is "Another schema is in PUBLISHED state for the resource type."
    And schema "$A" has status "PUBLISHED"
    And schema "$B" has status "REVOKED"

  @SIMPL-20529 @D18
  Scenario: Revoking a revoked schema gives 409
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    When I revoke schema "$A"
    Then the response is a "conflict" problem with status 409

  @SIMPL-20529 @D18
  Scenario: Publishing a published schema gives 409
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I publish schema "$A"
    Then the response is a "conflict" problem with status 409

  @SIMPL-13360 @SIMPL-20529 @D18
  Scenario Outline: An invalid status value is rejected
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I set the status of schema "$A" to "<status>"
    Then the response is a "badRequest" problem with status 400
    And schema "$A" has status "PUBLISHED"

    Examples:
      | status    |
      | ARCHIVED  |
      | revoked   |
      |           |

  @SIMPL-20529 @D18
  Scenario: A body without status is rejected
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a PATCH request to "/schemas/$A" with JSON body
      """
      {"state": "REVOKED"}
      """
    Then the response is a "badRequest" problem with status 400

  @SIMPL-13360 @SIMPL-20529
  Scenario: Changing the status of an unknown schema gives 404
    When I send a PATCH request to "/schemas/NonExistent$A" with JSON body
      """
      {"status": "REVOKED"}
      """
    Then the response is a "notFound" problem with status 404

  @SIMPL-20529 @D11
  Scenario: A resourceType that does not match the schema gives 404
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a PATCH request to "/schemas/$A?resourceType=$RT2" with JSON body
      """
      {"status": "REVOKED"}
      """
    Then the response is a "notFound" problem with status 404
    And schema "$A" has status "PUBLISHED"

  @SIMPL-20529 @D11
  Scenario: A RESOURCE schema is not found with schemaType SEARCH
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a PATCH request to "/schemas/$A?schemaType=SEARCH" with JSON body
      """
      {"status": "REVOKED"}
      """
    Then the response is a "notFound" problem with status 404

  @SIMPL-20529 @D11
  Scenario: A matching resourceType selects the schema
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a PATCH request to "/schemas/$A?resourceType=$RT&schemaType=RESOURCE" with JSON body
      """
      {"status": "REVOKED"}
      """
    Then the response status is 200
    And the JSON field "status" is "REVOKED"

  @SIMPL-33537 @D27
  Scenario: resourceType with schemaType SEARCH is rejected on PATCH
    When I send a PATCH request to "/schemas/AnySchema?schemaType=SEARCH&resourceType=Data" with JSON body
      """
      {"status": "REVOKED"}
      """
    Then the response is a "badRequest" problem with status 400

  @SIMPL-20529 @D12
  Scenario: An invalid schema name gives 400
    When I send a PATCH request to "/schemas/bad-name" with JSON body
      """
      {"status": "REVOKED"}
      """
    Then the response is a "badRequest" problem with status 400

  @SIMPL-20529 @D8
  Scenario: An Accept header other than JSON or problem JSON gives 406
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a PATCH request to "/schemas/$A" with JSON body and header "Accept" set to "text/plain"
      """
      {"status": "REVOKED"}
      """
    Then the response is a "notAcceptable" problem with status 406
    And schema "$A" has status "PUBLISHED"

  @SIMPL-20529 @D8 @openapi-only
  Scenario: A PATCH body that is not JSON is rejected with 415
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    When I send a PATCH request to "/schemas/$A" with content type "text/plain" and body
      """
      status=REVOKED
      """
    Then the response is a "unsupportedMediaType" problem with status 415
    And schema "$A" has status "PUBLISHED"
