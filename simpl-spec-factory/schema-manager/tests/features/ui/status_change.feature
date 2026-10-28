@ui @D46
Feature: Revoke and publish from the schemas list
  SIMPL-13361 (revoke with confirmation), SIMPL-20530 (revoke and publish),
  SIMPL-14701 and SIMPL-32497 (a revoked schema stays visible). D51, D52.

  @SIMPL-13361 @SIMPL-20530 @D51
  Scenario: Revoke asks for confirmation and Cancel sends nothing
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas"
    And I click "action-revoke-$A"
    Then "confirm-dialog" is visible
    When I click "cancel-button"
    Then "confirm-dialog" is not rendered
    And the UI sent no request to the API

  @SIMPL-13361 @SIMPL-20530 @SIMPL-14701 @SIMPL-32497 @D51
  Scenario: Confirmed revoke shows the success text and the row shows REVOKED
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas"
    And I click "action-revoke-$A"
    And I click "confirm-button"
    Then the UI sent the request "PATCH /schemas/$A"
    And "notification-success" shows 'Schema "$A" revoked successfully.'
    And the row "schema-row-$A" shows "REVOKED" in the column "Status"
    And "action-publish-$A" is visible

  @SIMPL-20530 @D52
  Scenario: Publish a revoked schema
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And schema "$A" has been revoked
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas"
    And I click "action-publish-$A"
    Then "confirm-dialog" is not rendered
    And "notification-success" shows "Schema $A published successfully!"
    And the row "schema-row-$A" shows "PUBLISHED" in the column "Status"

  @SIMPL-20530 @D18 @D52
  Scenario: Publishing while another schema of the resource type is published shows the 409 detail
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas"
    And I click "action-publish-$B"
    Then "notification-error" shows "Another schema is in PUBLISHED state for the resource type."
    And the row "schema-row-$B" shows "REVOKED" in the column "Status"

  @SIMPL-13361 @D51
  Scenario: Revoke is offered only on published rows and Publish only on revoked rows
    Given the RESOURCE schema "$A" exists for resource type "$RT"
    And the RESOURCE schema "$B" exists for resource type "$RT"
    And I am signed in to the UI as "ga-admin"
    When I open the page "/schemas"
    Then "action-revoke-$A" is visible
    And "action-publish-$A" is not rendered
    And "action-publish-$B" is visible
    And "action-revoke-$B" is not rendered
