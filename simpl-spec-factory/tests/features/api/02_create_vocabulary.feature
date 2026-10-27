@api
Feature: Create a new vocabulary via the API
  As a Governance Authority
  I want to create a new vocabulary by submitting its metadata and content via the API
  so that I can automate vocabulary creation

  Background:
    Given I am authenticated as "admin"

  @SIMPL-18170 @SIMPL-18187 @SIMPL-18186
  Scenario: A valid vocabulary is created as version 1, ACTIVE
    When I create vocabulary "{Alpha}" from "valid_v1.ttl"
    Then the response status is 201
    And the response JSON at "name" is "{Alpha}"
    And the response JSON at "version" is 1
    And the response JSON at "status" is "ACTIVE"

  @SIMPL-18170
  Scenario: The created vocabulary can be retrieved with its metadata
    Given a vocabulary "{Alpha}" exists
    When I send GET "/vocabularies/{Alpha}"
    Then the response status is 200
    And the response JSON at "name" is "{Alpha}"
    And the response JSON at "version" is 1
    And the response JSON at "status" is "ACTIVE"
    And the response JSON at "description" is "Test vocabulary {Alpha}"

  @SIMPL-18170 @D16
  Scenario: A SKOS concept scheme is accepted as a vocabulary
    When I create vocabulary "{Alpha}" from "skos_scheme.ttl"
    Then the response status is 201

  @SIMPL-18170
  Scenario Outline: A request without a required field is rejected and nothing is created
    When I create vocabulary "{Alpha}" from "valid_v1.ttl" without the "<field>" field
    Then the response is a problem with status 400
    And vocabulary "{Alpha}" does not exist

    Examples:
      | field          |
      | description    |
      | changelog      |
      | vocabularyFile |

  @SIMPL-18170 @openapi-only
  Scenario Outline: A name that does not follow the naming rule is rejected
    When I create a vocabulary named "<name>" from "valid_v1.ttl"
    Then the response is a problem with status 400

    Examples:
      | name             | why                         |
      | lowercaseStart   | must start with a capital   |
      | Has Space        | only letters and digits     |
      | Ab               | shorter than 3 characters   |

  @SIMPL-18169 @D13
  Scenario: A duplicate vocabulary name is a conflict
    Given a vocabulary "{Alpha}" exists
    When I create vocabulary "{Alpha}" from "valid_v1.ttl"
    Then the response is a problem with status 409
    And vocabulary "{Alpha}" has 1 versions

  @D15
  Scenario: A file that is not Turtle is rejected
    When I upload "not_turtle.txt" as vocabulary "{Alpha}" with content type "text/plain"
    Then the response status is one of 400, 415
    And vocabulary "{Alpha}" does not exist
