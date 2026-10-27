@api
Feature: Syntax, structure and semantic validation on creation and new version
  Validation runs as part of the create and new-version requests; there is no separate
  validation endpoint. Errors block, warnings do not (R07.02, D20).

  Background:
    Given I am authenticated as "admin"

  @SIMPL-18181 @SIMPL-18194 @SIMPL-18180
  Scenario: Syntax errors are reported with their location and nothing is created
    When I create vocabulary "{Alpha}" from "syntax_errors.ttl"
    Then the response is a problem with status 400
    And the problem lists at least 1 issues
    And every issue has a location
    And vocabulary "{Alpha}" does not exist

  @SIMPL-18180 @D16
  Scenario: An empty vocabulary is a structure error
    When I create vocabulary "{Alpha}" from "empty.ttl"
    Then the response is a problem with status 400
    And the problem lists at least 1 issues
    And vocabulary "{Alpha}" does not exist

  @SIMPL-18180 @D16
  Scenario: A vocabulary without an ontology or concept scheme header is a structure error
    When I create vocabulary "{Alpha}" from "no_header.ttl"
    Then the response is a problem with status 400
    And vocabulary "{Alpha}" does not exist

  @SIMPL-18183 @SIMPL-18182 @D17
  Scenario: An IRI declared both as a class and as a property is a semantic error
    When I create vocabulary "{Alpha}" from "class_and_property.ttl"
    Then the response is a problem with status 400
    And an issue mentions "Thing"
    And vocabulary "{Alpha}" does not exist

  @SIMPL-18183 @D17 @D18
  Scenario: A reference to an existing term of an active internal version is valid
    Given a vocabulary "{Alpha}" exists
    When I create vocabulary "{Beta}" referencing term "Dataset" of "{Alpha}" version 1
    Then the response status is 201

  @SIMPL-18183 @D17 @D18
  Scenario: A reference to the latest internal version is valid
    Given a vocabulary "{Alpha}" exists
    When I create vocabulary "{Beta}" referencing term "Dataset" of the latest "{Alpha}"
    Then the response status is 201

  @SIMPL-18183 @D17 @D18
  Scenario: A reference to a term that does not exist is a semantic error
    Given a vocabulary "{Alpha}" exists
    When I create vocabulary "{Beta}" referencing term "Unicorn" of "{Alpha}" version 1
    Then the response is a problem with status 400
    And an issue mentions "Unicorn"
    And vocabulary "{Beta}" does not exist

  @SIMPL-18184 @D17 @D18
  Scenario: A reference to a deprecated internal version is rejected
    Given vocabulary "{Alpha}" has 2 versions
    When I create vocabulary "{Beta}" referencing term "Dataset" of "{Alpha}" version 1
    Then the response is a problem with status 400
    And an issue mentions "{Alpha}"
    And vocabulary "{Beta}" does not exist

  @SIMPL-18184 @D17 @D18 @D19
  Scenario: A reference to an internal version that does not exist is rejected
    Given a vocabulary "{Alpha}" exists
    When I create vocabulary "{Beta}" referencing term "Dataset" of "{Alpha}" version 9
    Then the response is a problem with status 400
    And vocabulary "{Beta}" does not exist

  @SIMPL-18183 @D17 @D20
  Scenario: An unregistered external namespace gives a warning, not an error
    When I create vocabulary "{Alpha}" from "unknown_external.ttl"
    Then the response status is 201
    And the response has validation warnings

  @SIMPL-18194
  Scenario: A valid submission returns no error details
    When I create vocabulary "{Alpha}" from "valid_v1.ttl"
    Then the response status is 201
