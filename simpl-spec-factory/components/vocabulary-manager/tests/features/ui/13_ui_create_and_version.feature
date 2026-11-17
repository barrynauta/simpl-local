@ui
Feature: Create a vocabulary and new versions in the UI

  Background:
    Given I am signed in to the UI as "admin"

  @SIMPL-18169 @D12
  Scenario: The creation form asks only for name, description and file
    When I open the Vocabulary Management page
    And I click "New Vocabulary +"
    Then I see a creation form with the required fields Name, Description and File upload
    And there is no field for version or change log

  @SIMPL-18169 @SIMPL-18186 @SIMPL-18180 @SIMPL-18193
  Scenario: A valid vocabulary is created and becomes active
    When I create vocabulary "{Alpha}" in the UI from "valid_v1.ttl"
    Then I see a confirmation that syntax and structure validation passed
    And "{Alpha}" appears in the list with version 1 and status "Active"

  @SIMPL-18169 @D13
  Scenario: Validation messages for missing fields and a duplicate name
    Given a vocabulary "{Alpha}" exists
    When I submit the creation form with name "{Alpha}" and no description
    Then I see an error that the description is required
    When I add a description and submit again
    Then I see an error that the name "{Alpha}" is already used
    And no vocabulary is created

  @SIMPL-18180 @SIMPL-18193 @D16
  Scenario: Syntax errors are listed with their location and block the flow
    When I upload "syntax_errors.ttl" in the creation form for "{Alpha}"
    Then validation runs automatically on upload
    And I see a list of the syntax errors, each with its location
    And I cannot submit the form until the errors are fixed

  @SIMPL-18182 @SIMPL-18193 @D17
  Scenario: Semantic errors are reported after syntax validation
    When I create vocabulary "{Alpha}" in the UI from "class_and_property.ttl"
    Then I see a structured report of the semantic errors
    And "{Alpha}" is not created

  @SIMPL-18172 @SIMPL-18189 @SIMPL-18191
  Scenario: A new version gets the next number automatically and replaces the active one
    Given a vocabulary "{Alpha}" exists
    When I open the actions menu of "{Alpha}" and click "Update"
    Then the form is pre-filled with the current metadata
    And there is no field for the version number
    When I enter a change summary, upload "valid_v2.ttl" and submit
    Then I see a confirmation that version 2 of "{Alpha}" was created
    And the history of "{Alpha}" shows version 2 "Active" and version 1 "Deprecated"

  @SIMPL-18172
  Scenario: An error during version creation is reported and nothing is created
    Given a vocabulary "{Alpha}" exists
    And the API fails when a new version is created
    When I submit a new version of "{Alpha}" from "valid_v2.ttl"
    Then I see an error that the operation failed, with advice to retry or contact support
    And the history of "{Alpha}" shows only version 1

  @SIMPL-18184 @D25
  Scenario: Selecting a deprecated version as a reference shows a warning
    Given vocabulary "{Alpha}" has 2 versions
    And a vocabulary "{Beta}" exists
    When I start a new version of "{Beta}" that references version 1 of "{Alpha}"
    Then I see a warning that version 1 of "{Alpha}" is deprecated
    And the warning recommends version 2, the latest active version
