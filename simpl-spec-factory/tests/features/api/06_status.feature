@api
Feature: Update the vocabulary status via the API
  The status of a vocabulary can be set manually to DEPRECATED and back to ACTIVE.

  Background:
    Given I am authenticated as "admin"

  @SIMPL-18175
  Scenario: Deprecate a vocabulary
    Given a vocabulary "{Alpha}" exists
    When I send PATCH "/vocabularies/{Alpha}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And the response JSON at "status" is "DEPRECATED"
    And vocabulary "{Alpha}" has status "DEPRECATED"

  @SIMPL-18175 @SIMPL-18170
  Scenario: Restore a deprecated vocabulary to ACTIVE
    Given a vocabulary "{Alpha}" exists
    And vocabulary "{Alpha}" is deprecated
    When I send PATCH "/vocabularies/{Alpha}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response status is 200
    And vocabulary "{Alpha}" has status "ACTIVE"

  @SIMPL-18175
  Scenario: Status change of a vocabulary that does not exist
    When I send PATCH "/vocabularies/{Ghost}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 404

  @SIMPL-18175
  Scenario: An unsupported status value is rejected and nothing changes
    Given a vocabulary "{Alpha}" exists
    When I send PATCH "/vocabularies/{Alpha}" with JSON
      """
      {"status": "ARCHIVED"}
      """
    Then the response is a problem with status 400
    And vocabulary "{Alpha}" has status "ACTIVE"

  @D10
  Scenario: Deprecating a vocabulary deprecates all its versions
    Given vocabulary "{Alpha}" has 2 versions
    And vocabulary "{Alpha}" is deprecated
    Then version 1 of "{Alpha}" has status "DEPRECATED"
    And version 2 of "{Alpha}" has status "DEPRECATED"

  @D10
  Scenario: Re-activating a vocabulary re-activates only its latest version
    Given vocabulary "{Alpha}" has 2 versions
    And vocabulary "{Alpha}" is deprecated
    When I send PATCH "/vocabularies/{Alpha}" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then version 2 of "{Alpha}" has status "ACTIVE"
    And version 1 of "{Alpha}" has status "DEPRECATED"

  @openapi-only @D10
  Scenario: A single version can be deprecated
    Given vocabulary "{Alpha}" has 2 versions
    When I send PATCH "/vocabularies/{Alpha}/versions/2" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response status is 200
    And version 2 of "{Alpha}" has status "DEPRECATED"

  @openapi-only @D10
  Scenario: An older version cannot become ACTIVE while another version is ACTIVE
    Given vocabulary "{Alpha}" has 2 versions
    When I send PATCH "/vocabularies/{Alpha}/versions/1" with JSON
      """
      {"status": "ACTIVE"}
      """
    Then the response is a problem with status 409
    And version 1 of "{Alpha}" has status "DEPRECATED"
