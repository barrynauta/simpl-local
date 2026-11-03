@uibe @stub
Feature: Contract overview, details and actions through the contract UI backend
  UIBE checks the user token and role, then calls its own CM. It adds the
  offer data from its own table (SIMPL-30959, SIMPL-30960, D7, B.1 to B.3).

  @SIMPL-30959 @D7 @oidc @consumer
  Scenario: SIMPL-30959 AC1 - the Consumer overview lists the offer with the Provider name as counterparty
    Given an offer created at the consumer
    When I send "GET" to the consumer UIBE at "/contracts?counterpartyName=provider%20one&contractNegotiationId=${nid}" as "consumer-admin"
    Then the response status is 200
    And the response list "items" has 1 items
    And the item of the response list "items" with "contractNegotiationId" "${nid}" has:
      | field                 | value           |
      | resourceDescriptionId | ${rdId}         |
      | resourceName          | ${resourceName} |
      | providerName          | Provider One    |
      | status                | REQUESTED       |

  @SIMPL-30959 @D7 @oidc
  Scenario: The Provider overview maps counterpartyName to the Consumer name
    Given a contract requested at the provider
    When I send "GET" to the provider UIBE at "/contracts?counterpartyName=consumer%20one&contractNegotiationId=${nid}" as "provider-admin"
    Then the response status is 200
    And the response list "items" has 1 items
    When I send "GET" to the provider UIBE at "/contracts?counterpartyName=provider%20one&contractNegotiationId=${nid}" as "provider-admin"
    Then the response status is 200
    And the response field "items" is an empty list

  @SIMPL-30959 @D7 @oidc
  Scenario: SIMPL-30959 AC2 - an overview without matching contracts is an empty list
    Given fresh negotiation ids
    When I send "GET" to the consumer UIBE at "/contracts?contractNegotiationId=${nid}" as "consumer-admin"
    Then the response status is 200
    And the response field "items" is an empty list

  @SIMPL-30960 @D7 @oidc @consumer
  Scenario: SIMPL-30960 AC1 - the Consumer details add the offer data of UIBE
    Given an offer created at the consumer
    When I send "GET" to the consumer UIBE at "/contracts/${nid}" as "consumer-admin"
    Then the response status is 200
    And the response fields are:
      | field                 | value     |
      | contractNegotiationId | ${nid}    |
      | status                | REQUESTED |
      | offerStatus           | CREATED   |
    And the response field "humanReadableData" contains "${resourceName}"
    And the response field "hashData" is the SHA-256 of the response field "humanReadableData"
    And the response field "allowedActions" contains "ACCEPT"
    And the response field "history" is present

  @SIMPL-30960 @D7 @oidc
  Scenario: The Provider details have no offer data
    Given a contract requested at the provider
    When I send "GET" to the provider UIBE at "/contracts/${nid}" as "provider-admin"
    Then the response status is 200
    And the response fields are:
      | field             | value |
      | offerStatus       | null  |
      | humanReadableData | null  |
      | hashData          | null  |

  @SIMPL-30960 @D7 @D12 @oidc
  Scenario: The details of an unknown negotiation give 404
    Given fresh negotiation ids
    When I send "GET" to the provider UIBE at "/contracts/${nid}" as "provider-admin"
    Then the response is a problem with status 404

  @SIMPL-31115 @D7 @D61 @two-party @oidc @slow
  Scenario: A lifecycle action through UIBE records the user name as the actor
    Given an active contract between both parties
    When the provider user "provider-operator" performs "SUSPEND" with reason "Breach of usage policy"
    Then the response status is 200
    And the response field "status" is "SUSPENDED"
    And the provider contract history has an entry with:
      | field    | value                  |
      | toStatus | SUSPENDED              |
      | trigger  | USER                   |
      | actor    | provider-operator      |
      | reason   | Breach of usage policy |
