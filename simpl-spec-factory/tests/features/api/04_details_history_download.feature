@api
Feature: Vocabulary details, version history and download via the API

  Background:
    Given I am authenticated as "admin"

  @SIMPL-18166
  Scenario: Full details of a specific version
    Given vocabulary "{Alpha}" has 2 versions
    When I send GET "/vocabularies/{Alpha}/versions/1"
    Then the response status is 200
    And the response JSON at "name" is "{Alpha}"
    And the response JSON at "version" is 1
    And the response JSON at "status" is "DEPRECATED"
    And the response JSON has a non-empty "changelog"
    And the response JSON has a non-empty "creationDate"

  @SIMPL-18166
  Scenario: A version that does not exist is not found
    Given a vocabulary "{Alpha}" exists
    When I send GET "/vocabularies/{Alpha}/versions/99"
    Then the response is a problem with status 404

  @SIMPL-18166
  Scenario: A vocabulary that does not exist is not found
    When I send GET "/vocabularies/{Ghost}"
    Then the response is a problem with status 404

  @SIMPL-18168
  Scenario: The version history lists all versions
    Given vocabulary "{Alpha}" has 3 versions
    When I send GET "/vocabularies/{Alpha}/versions"
    Then the response status is 200
    And the list has 3 items

  @SIMPL-18168
  Scenario: The history of a vocabulary that does not exist is not found
    When I send GET "/vocabularies/{Ghost}/versions"
    Then the response is a problem with status 404

  @SIMPL-18165 @D24
  Scenario: Download the newest version as Turtle
    Given vocabulary "{Alpha}" has 2 versions
    When I send GET "/content/{Alpha}" accepting "text/turtle"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body contains "Test vocabulary, version 2"

  @SIMPL-18452 @D24
  Scenario: Download a specific older version as Turtle
    Given vocabulary "{Alpha}" has 2 versions
    When I send GET "/content/{Alpha}/versions/1" accepting "text/turtle"
    Then the response status is 200
    And the response content type is "text/turtle"
    And the response body contains "Test vocabulary, version 1"

  @SIMPL-18176
  Scenario: Deprecated versions stay retrievable
    Given vocabulary "{Alpha}" has 2 versions
    When I send GET "/vocabularies/{Alpha}/versions/1"
    Then the response status is 200
    And the response JSON at "status" is "DEPRECATED"
