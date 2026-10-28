Feature: Revoked schemas are retained
  SIMPL-14354, SIMPL-14701 (API side), SIMPL-32436 and SIMPL-32497 (RESOURCE
  part). Nothing is ever deleted (D21): a revoked schema and all its versions
  stay listed and retrievable, so references in resource descriptions resolve.

  @SIMPL-14354 @SIMPL-32436 @D21
  Scenario: A revoked schema and its properties are still returned
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And schema "$A" has been revoked
    And I am "ga-admin"
    When I list schemas with "status=REVOKED&resourceType=$RT"
    Then the response status is 200
    And the names in "metadata" are "$A"
    And the JSON field "metadata[0].title" is "Title of $A"
    And the JSON field "metadata[0].version" is "2"
    When I request the versions of schema "$A"
    Then the response status is 200
    And the versions in "metadata" are "1,2"
    And every item of "metadata" has "status" equal to "REVOKED"

  @SIMPL-14701 @SIMPL-32436 @SIMPL-32497 @D21 @D9 @D10
  Scenario: The versioned URI of a revoked schema still resolves
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    And schema "$A" has been revoked
    And I am "ga-viewer"
    When I request version "1" of schema "$A"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$A"
    When I request version "2" of schema "$A"
    Then the response status is 200
    And the response body is the content uploaded for version "2" of schema "$A"
    When I request the latest content of schema "$A"
    Then the response status is 200
    And the response header "Content-Location" is an absolute URI ending with "/schemas/$A/2"

  @SIMPL-14354 @D21
  Scenario: A superseded version stays retrievable after a new version
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has a new version
    When I request version "1" of schema "$A"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$A"

  @SIMPL-14354 @D16 @D21
  Scenario: A schema revoked on create is retained too
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    When I request version "1" of schema "$B"
    Then the response status is 200
    And the response body is the content uploaded for version "1" of schema "$B"
