Feature: Contract consent status at the Provider
  The consent manager, here the test, changes the consent status through CM.
  The UI shows a link to the consent manager for users with consent_manager
  (SIMPL-31047, D43, D68, A.5).

  @SIMPL-31047 @D43 @D37 @stub
  Scenario: SIMPL-31047 AC1 - a contract with consent defined offers the consent update action
    Given a contract requested at the provider for an asset with consent required
    Then the provider contract has:
      | field         | value   |
      | consentStatus | PENDING |
    And the provider contract allowed actions are "UPDATE_CONSENT"

  @SIMPL-31047 @D43 @D61 @stub
  Scenario: SIMPL-31047 AC3 - a consent change is visible in the details and in the history
    Given a contract requested at the provider for an asset with consent required
    When I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "GRANTED"}
      """
    Then the response status is 200
    And the response field "consentStatus" is "GRANTED"
    And the response field "status" is "REQUESTED"
    And the provider contract has:
      | field         | value   |
      | consentStatus | GRANTED |

  @SIMPL-31047 @D43 @D49 @stub @events
  Scenario: A consent change publishes a status event with an unchanged status
    Given I listen to the Kafka topic "simpl.contract.status.v1"
    And a contract requested at the provider for an asset with consent required
    When I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "WITHDRAWN"}
      """
    Then the response status is 200
    And a message for the negotiation is published on "simpl.contract.status.v1" with:
      | field          | value     |
      | previousStatus | REQUESTED |
      | newStatus      | REQUESTED |

  @SIMPL-31047 @D43
  Scenario: SIMPL-31047 AC4 - a contract without consent defined offers no consent action and refuses a change
    Given a contract requested at the provider
    Then the provider contract allowed actions are ""
    When I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "GRANTED"}
      """
    Then the response is a problem with status 409 and type "invalidTransition"
    And the provider contract has:
      | field         | value       |
      | consentStatus | NOT_DEFINED |

  @SIMPL-31047 @D43 @stub
  Scenario: A consent change on a terminated contract is refused with 409
    Given a contract requested at the provider for an asset with consent required
    And the provider contract is terminated by the connector
    When I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "GRANTED"}
      """
    Then the response is a problem with status 409 and type "invalidTransition"

  @SIMPL-31047 @D43 @consumer @stub
  Scenario: A consent change on a Consumer record is refused with 409
    Given an offer created at the consumer
    When I send "PUT" to the consumer CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "GRANTED"}
      """
    Then the response is a problem with status 409 and type "invalidTransition"

  @SIMPL-31047 @D43 @D11 @stub
  Scenario Outline: A consent value outside PENDING, GRANTED and WITHDRAWN is refused with 400 (<value>)
    Given a contract requested at the provider for an asset with consent required
    When I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "<value>"}
      """
    Then the response is a problem with status 400 and type "validationFailure"

    Examples:
      | value       |
      | NOT_DEFINED |
      | APPROVED    |

  @SIMPL-31047 @D43 @D12
  Scenario: A consent change for an unknown negotiation gives 404
    Given fresh negotiation ids
    When I send "PUT" to the provider CM at "/contracts/${nid}/consentStatus" with JSON:
      """
      {"consentStatus": "GRANTED"}
      """
    Then the response is a problem with status 404 and type "notFound"

  @SIMPL-31047 @D43 @D68 @uibe @oidc @stub
  Scenario: SIMPL-31047 AC2 - a user with consent_manager gets the consent manager link
    Given a contract requested at the provider for an asset with consent required
    When I send "GET" to the provider UIBE at "/contracts/${nid}" as "provider-admin"
    Then the response status is 200
    And the response field "allowedActions" contains "UPDATE_CONSENT"
    And the response field "consentManagerUrl" is "${consentManagerUrl}?contractNegotiationId=${nid}"

  @SIMPL-31047 @D43 @D68 @uibe @oidc @stub
  Scenario: A user without consent_manager gets neither the consent action nor the link
    Given a contract requested at the provider for an asset with consent required
    When I send "GET" to the provider UIBE at "/contracts/${nid}" as "provider-operator"
    Then the response status is 200
    And the response field "allowedActions" does not contain "UPDATE_CONSENT"
    And the response field "consentManagerUrl" is absent or null

  @SIMPL-31047 @D43 @stub
  Scenario: SIMPL-31047 AC2 - the consent manager link target answers
    Given fresh negotiation ids
    When I send "GET" to the stub at "/consent?contractNegotiationId=${nid}"
    Then the response status is 200
    And the response body contains "Consent manager stub"
