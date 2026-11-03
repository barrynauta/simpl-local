@ui @stub
Feature: Consent status in the UI
  SIMPL-31047, D43, D68, D72, D73.

  @SIMPL-31047 @D43 @D72
  Scenario: SIMPL-31047 AC1 and AC2 - a consent manager user is redirected to the consent manager
    Given a contract requested at the provider for an asset with consent required
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    Then the element "detail-consent-status" shows "PENDING"
    And the element "action-update-consent" is visible
    When I click "action-update-consent"
    Then the browser is redirected to the consent manager for the negotiation

  @SIMPL-31047 @D43
  Scenario: SIMPL-31047 AC3 - a consent change is shown in the details
    Given a contract requested at the provider for an asset with consent required
    And I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "GRANTED"}
      """
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    Then the element "detail-consent-status" shows "GRANTED"

  @SIMPL-31047 @D73
  Scenario: SIMPL-31047 AC4 - no consent action when consent is not defined
    Given a contract requested at the provider
    And I am logged in to the provider UI as "provider-admin"
    When I open the details of the negotiation
    Then the element "action-update-consent" is absent

  @SIMPL-31047 @D68 @D73 @rbac
  Scenario: No consent action for a user without consent_manager
    Given a contract requested at the provider for an asset with consent required
    And I am logged in to the provider UI as "provider-operator"
    When I open the details of the negotiation
    Then the element "action-update-consent" is absent
