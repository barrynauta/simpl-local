@api @openapi-only
Feature: External vocabularies
  A copied external vocabulary can be registered with its namespace and aliases, found,
  downloaded and deleted (OpenAPI only; R07.02 "External reference mapping management").
  Note: external vocabularies use fixed names with the run id; they are deleted at the end.

  Background:
    Given I am authenticated as "admin"

  Scenario: Register, find by namespace and alias, download and delete
    When I register external vocabulary "{Dcat}" with namespace "https://ext.example.org/{RUN}/ns#" and alias "https://alias.example.org/{RUN}/ns#"
    Then the response status is 201
    When I send GET "/external-vocabularies/{Dcat}"
    Then the response status is 200
    And the response JSON at "namespaceUri" contains "https://ext.example.org/{RUN}/ns"
    When I send POST "/external-vocabulary/searches" with JSON
      """
      {"aliasUri": "https://alias.example.org/{RUN}/ns#"}
      """
    Then the response status is 200
    And the list contains "{Dcat}"
    When I send GET "/external-vocabularies/{Dcat}/content" accepting "text/turtle"
    Then the response status is 200
    And the response body contains "Copied external vocabulary"
    When I send POST "/external-vocabularies/content" with JSON accepting "text/turtle"
      """
      {"namespaceUri": "https://ext.example.org/{RUN}/ns#"}
      """
    Then the response status is 200
    When I send DELETE "/external-vocabularies/{Dcat}"
    Then the response status is 204
    When I send GET "/external-vocabularies/{Dcat}"
    Then the response is a problem with status 404

  Scenario: A duplicate external vocabulary name is a conflict
    When I register external vocabulary "{Dcat}" with namespace "https://ext.example.org/{RUN}/a#" and alias "https://alias.example.org/{RUN}/a#"
    And I register external vocabulary "{Dcat}" with namespace "https://ext.example.org/{RUN}/b#" and alias "https://alias.example.org/{RUN}/b#"
    Then the response is a problem with status 409
    When I send DELETE "/external-vocabularies/{Dcat}"
    Then the response status is 204

  @D17 @D20
  Scenario: A registered external namespace does not cause a warning
    When I register external vocabulary "{Dcat}" with namespace "https://unregistered.example.org/ns#" and alias "https://alias.example.org/{RUN}/c#"
    And I create vocabulary "{Alpha}" from "unknown_external.ttl"
    Then the response status is 201
    When I send DELETE "/external-vocabularies/{Dcat}"
    Then the response status is 204
