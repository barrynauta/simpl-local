@api @cat @schemas
Feature: catalogue-be stores the SHACL shapes used at publication
  Only SHACL shapes are stored (D58). The Schema Manager webhook sets the status of a shape
  (D60). Recalculation is acknowledged only (D56). Tests create their own shapes with unique
  names and never touch the seed shapes (D80).

  @D58
  Scenario: The shape list has the seed shapes
    When "schema-viewer-a" sends GET "/schemas" to "cat-a"
    Then the status is 200
    And the field "shapes" is a list containing "DataOfferingShape"
    And the field "shapes" is a list containing "ApplicationOfferingShape"
    And the field "shapes" is a list containing "InfrastructureOfferingShape"
    And the field "shapes" is a list containing "FederatedSearchShape"
    And the field "shapes" is a list containing "NoThresholdShape"
    And the field "shapes" is a list containing "NoProfileShape"
    And the field "ontologies" is an empty list
    And the field "vocabularies" is an empty list

  @D58
  Scenario: A seed shape is returned as Turtle
    When "schema-viewer-a" sends GET "/schemas/DataOfferingShape" to "cat-a"
    Then the status is 200
    And the response content type is "text/turtle"
    And the response body contains "DataOfferingShape"
    And the response body contains "GeneralServiceProperties"

  @D58
  Scenario: An unknown shape is 404
    When "schema-viewer-a" sends GET "/schemas/NoSuch{token}Shape" to "cat-a"
    Then the response is a problem with status 404

  @D58
  Scenario: A new shape is stored, listed, returned and deleted
    Given a test shape "s1"
    When "schema-admin-a" uploads the shape "s1" to "cat-a"
    Then the status is 201
    And the response header "Location" contains "{s1.name}"
    When "schema-viewer-a" sends GET "/schemas" to "cat-a"
    Then the field "shapes" is a list containing "{s1.name}"
    When "schema-viewer-a" sends GET "/schemas/{s1.name}" to "cat-a"
    Then the status is 200
    And the response body contains "{s1.name}"
    When "schema-admin-a" sends DELETE "/schemas/{s1.name}" to "cat-a"
    Then the status is 200
    When "schema-viewer-a" sends GET "/schemas/{s1.name}" to "cat-a"
    Then the response is a problem with status 404

  @D58
  Scenario: Uploading a shape name twice is a conflict
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    When "schema-admin-a" uploads the shape "s1" to "cat-a"
    Then the response is a problem with status 409

  @D58
  Scenario: A shape without an offering target class is rejected
    Given a test shape "s1" without an offering target class
    When "schema-admin-a" uploads the shape "s1" to "cat-a"
    Then the response is a problem with status 400

  @D58
  Scenario: Text that is not Turtle is rejected
    When "schema-admin-a" sends POST "/schemas" to "cat-a" with the content type "text/turtle"
      """
      this is not turtle @prefix
      """
    Then the response is a problem with status 400

  @D58
  Scenario: Deleting an unknown shape is 404
    When "schema-admin-a" sends DELETE "/schemas/NoSuch{token}Shape" to "cat-a"
    Then the response is a problem with status 404

  @D58 @D59
  Scenario: A resource description can be published against a new shape
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    And a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "s1"
    When "provider-a" publishes "rd1"
    Then the status is 201

  @D59
  Scenario: A new shape starts at version 1
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    And a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "s1" version "2"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:schemaNotSupported" with status 422

  # ------------------------------------------------------------------ webhook (D60)

  @D60
  Scenario: A revoked shape no longer accepts publications
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    When the Schema Manager sends "SchemaRevoked" for the shape "s1" to "cat-a"
    Then the status is 200
    Given a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "s1"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:schemaNotSupported" with status 422

  @D60
  Scenario: A shape published again accepts publications again
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    And the Schema Manager has sent "SchemaRevoked" for the shape "s1" to "cat-a"
    And the Schema Manager has sent "SchemaPublished" for the shape "s1" to "cat-a"
    And a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "s1"
    When "provider-a" publishes "rd1"
    Then the status is 201

  @D60
  Scenario: A published event with a version sets the stored version
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    And the Schema Manager has sent "SchemaPublished" with version "2" for the shape "s1" to "cat-a"
    And a "DataOffering" resource description "old" by "provider-a"
    And "old" conforms to the shape "s1" version "1"
    And a "DataOffering" resource description "new" by "provider-a"
    And "new" conforms to the shape "s1" version "2"
    When "provider-a" publishes "old"
    Then the response is a problem "catalogue:schemaNotSupported" with status 422
    When "provider-a" publishes "new"
    Then the status is 201

  @D60
  Scenario: An event for an unknown name is remembered for a later upload
    Given a test shape "s1"
    And the Schema Manager has sent "SchemaRevoked" for the shape "s1" to "cat-a"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    And a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "s1"
    When "provider-a" publishes "rd1"
    Then the response is a problem "catalogue:schemaNotSupported" with status 422

  @D60
  Scenario: An event without a schema name is rejected
    When the Schema Manager sends "SchemaPublished" without a schema name to "cat-a"
    Then the response is a problem with status 400

  @D60
  Scenario: A body that is not a schema change event is rejected
    When "anonymous" sends POST "/webhooks/schema-events" to "cat-a"
      """
      {"eventType": "SomethingElse", "data": {}}
      """
    Then the response is a problem with status 400

  # ------------------------------------------------------------------ recalculation (D56)

  @D56 @quality
  Scenario: A recalculation request is acknowledged with the count of active resource descriptions
    Given a test shape "s1"
    And "schema-admin-a" has uploaded the shape "s1" to "cat-a"
    And a "DataOffering" resource description "rd1" by "provider-a"
    And "rd1" conforms to the shape "s1"
    And "provider-a" has published "rd1"
    And a "DataOffering" resource description "rd2" by "provider-a"
    And "rd2" conforms to the shape "s1"
    And "provider-a" has published "rd2"
    And a "DataOffering" resource description "rd3" by "provider-a"
    And "rd3" conforms to the shape "s1"
    And "provider-a" has published "rd3"
    And "provider-a" has revoked "rd3"
    When "schema-admin-a" sends POST "/schemas/{s1.name}/recalcQualityScore" to "cat-a"
      """
      {"profileVersion": "1.0.0", "profileId": "urn:simpl:mqp:default"}
      """
    Then the status is 202
    And the field "status" is "ACCEPTED"
    And the field "schemaName" is "{s1.name}"
    And the field "profileVersion" is "1.0.0"
    And the field "estimatedResourceCount" is "2"

  @D56
  Scenario Outline: A recalculation request with a bad schema name is 400 (<name>)
    When "schema-admin-a" sends POST "/schemas/<name>/recalcQualityScore" to "cat-a"
      """
      {"profileVersion": "1.0.0"}
      """
    Then the response is a problem "catalogue:invalidInput" with status 400

    Examples:
      | name                |
      | dataOfferingShape   |
      | Ab                  |
      | Data%20OfferingShape |

  @D56
  Scenario: A recalculation request without a profile version is 400
    When "schema-admin-a" sends POST "/schemas/DataOfferingShape/recalcQualityScore" to "cat-a"
      """
      {"profileId": "urn:simpl:mqp:default"}
      """
    Then the response is a problem with status 400

  @D56
  Scenario: A recalculation request for an unknown shape is 404
    When "schema-admin-a" sends POST "/schemas/NoSuch{token}Shape/recalcQualityScore" to "cat-a"
      """
      {"profileVersion": "1.0.0"}
      """
    Then the response is a problem "catalogue:schemaNotFound" with status 404

  @D56 @quality
  Scenario: A recalculation request for a shape without a quality profile is 404
    When "schema-admin-a" sends POST "/schemas/NoProfileShape/recalcQualityScore" to "cat-a"
      """
      {"profileVersion": "1.0.0"}
      """
    Then the response is a problem "catalogue:activeQualityProfileNotFound" with status 404

  # ------------------------------------------------------------------ access (5.4, D67)

  @rbac @D66
  Scenario Outline: Reading shapes needs a schema role (<who>)
    When "<who>" sends GET "/schemas" to "cat-a"
    Then the status is <status>

    Examples:
      | who             | status |
      | anonymous       | 401    |
      | consumer-a      | 403    |
      | provider-a      | 403    |
      | schema-viewer-a | 200    |
      | schema-admin-a  | 200    |

  @rbac @D67
  Scenario: Writing shapes needs a valid token but no role
    Given a test shape "s1"
    When "nobody-a" uploads the shape "s1" to "cat-a"
    Then the status is 201

  @rbac @D67
  Scenario: Writing shapes without a token is 401
    Given a test shape "s1"
    When "anonymous" uploads the shape "s1" to "cat-a"
    Then the response is a problem with status 401

  # ------------------------------------------------------------------ out of scope (D10)

  @D10 @openapi-only
  Scenario Outline: An out-of-scope operation exists but does not succeed (<method> <path>)
    When "schema-admin-a" sends <method> "<path>" to "cat-a"
    Then the status is not a success

    Examples:
      | method | path                     |
      | GET    | /query                   |
      | POST   | /query                   |
      | GET    | /session                 |
      | DELETE | /session                 |
      | GET    | /verification            |
      | GET    | /schemas/latest?type=SHAPE |
      | PUT    | /schemas/NoSuch{token}Shape |
