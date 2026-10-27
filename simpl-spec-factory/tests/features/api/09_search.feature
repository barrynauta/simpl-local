@api @openapi-only
Feature: Search several vocabulary references at once
  OpenAPI searchVocabularies: resolves latest or versioned references; missing ones are omitted.

  Background:
    Given I am authenticated as "admin"

  Scenario: Latest and versioned references are resolved, missing ones omitted
    Given vocabulary "{Alpha}" has 2 versions
    When I send POST "/vocabularies/searches" with JSON
      """
      {"items": [{"name": "{Alpha}"}, {"name": "{Alpha}", "version": 1}, {"name": "{Ghost}"}]}
      """
    Then the response status is 200
    And the list has 2 items
    And the response JSON at "items.0.name" is "{Alpha}"

  Scenario: A search without items is rejected
    When I send POST "/vocabularies/searches" with JSON
      """
      {}
      """
    Then the response is a problem with status 400
