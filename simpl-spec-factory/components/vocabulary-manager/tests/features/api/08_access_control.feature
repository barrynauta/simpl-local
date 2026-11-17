@api @rbac
Feature: Role-based access control on the API
  GA_VOCABULARY_VIEWER may read, GA_VOCABULARY_ADMIN may read and write (D3).
  No token gives 401, no vocabulary role gives 403 (SIMPL-18196, D4).
  Note: in the upstream deployment a gateway enforces this; a stack without the gateway is expected to fail here.

  @SIMPL-18196 @D4
  Scenario Outline: Without a token every vocabulary endpoint is refused
    Given I am not authenticated
    When I send GET "<path>" accepting "*/*"
    Then the response is a problem with status 401

    Examples:
      | path                       |
      | /vocabularies              |
      | /vocabularies/Anything     |
      | /content/Anything          |
      | /external-vocabularies     |

  @SIMPL-18196 @SIMPL-18162 @D4
  Scenario: A user without a vocabulary role is forbidden to read
    Given I am authenticated as "norole"
    When I send GET "/vocabularies"
    Then the response is a problem with status 403

  @SIMPL-18196 @D3
  Scenario: A viewer may read
    Given a vocabulary "{Alpha}" exists
    And I am authenticated as "viewer"
    When I send GET "/vocabularies/{Alpha}"
    Then the response status is 200

  @SIMPL-18196 @D3
  Scenario: A viewer may not create a vocabulary
    Given I am authenticated as "viewer"
    When I create vocabulary "{Alpha}" from "valid_v1.ttl"
    Then the response is a problem with status 403
    And vocabulary "{Alpha}" does not exist

  @SIMPL-18196 @SIMPL-18173 @D3
  Scenario: A viewer may not create a new version
    Given a vocabulary "{Alpha}" exists
    And I am authenticated as "viewer"
    When I create a new version of "{Alpha}" from "valid_v2.ttl"
    Then the response is a problem with status 403
    And vocabulary "{Alpha}" has 1 versions

  @SIMPL-18196 @D3
  Scenario: A viewer may not change the status
    Given a vocabulary "{Alpha}" exists
    And I am authenticated as "viewer"
    When I send PATCH "/vocabularies/{Alpha}" with JSON
      """
      {"status": "DEPRECATED"}
      """
    Then the response is a problem with status 403
    And vocabulary "{Alpha}" has status "ACTIVE"
