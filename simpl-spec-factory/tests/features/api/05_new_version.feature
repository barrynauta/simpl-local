@api
Feature: Create a new version of a vocabulary via the API

  Background:
    Given I am authenticated as "admin"

  @SIMPL-18173 @SIMPL-18190 @SIMPL-18187 @D7
  Scenario: The next version number is assigned automatically
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" from "valid_v2.ttl"
    Then the response status is 201
    And the response JSON at "version" is 2
    And the response JSON at "status" is "ACTIVE"

  @SIMPL-18173 @SIMPL-18191 @D9
  Scenario: The new version is ACTIVE and all previous versions are DEPRECATED
    Given vocabulary "{Alpha}" has 2 versions
    When I create a new version of "{Alpha}" from "valid_v2.ttl"
    Then the response status is 201
    And version 3 of "{Alpha}" has status "ACTIVE"
    And version 2 of "{Alpha}" has status "DEPRECATED"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @SIMPL-18173
  Scenario: The latest metadata reflects the new version
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" from "valid_v2.ttl"
    And I send GET "/vocabularies/{Alpha}"
    Then the response JSON at "version" is 2

  @SIMPL-18190 @D8
  Scenario: A manual version number is rejected
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" from "valid_v2.ttl" with form field "version" set to "7"
    Then the response is a problem with status 400
    And vocabulary "{Alpha}" has 1 versions

  @SIMPL-18173
  Scenario: A new version for a vocabulary that does not exist is rejected
    When I create a new version of "{Ghost}" from "valid_v2.ttl"
    Then the response is a problem with status 404

  @SIMPL-18173 @SIMPL-18181
  Scenario: An invalid new version is rejected and nothing changes
    Given a vocabulary "{Alpha}" exists
    When I create a new version of "{Alpha}" from "syntax_errors.ttl"
    Then the response is a problem with status 400
    And vocabulary "{Alpha}" has 1 versions
    And version 1 of "{Alpha}" has status "ACTIVE"

  @D11
  Scenario: A new version of a deprecated vocabulary is a conflict
    Given a vocabulary "{Alpha}" exists
    And vocabulary "{Alpha}" is deprecated
    When I create a new version of "{Alpha}" from "valid_v2.ttl"
    Then the response is a problem with status 409
