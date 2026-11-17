Feature: Contract read model of the CM (Appendix A.2 and A.3)
  The overview and the details the UI needs, with filters, allowed actions and
  the history (SIMPL-30959, SIMPL-30960, D6, D37, D60, D61).

  @SIMPL-30959 @D6 @stub
  Scenario: SIMPL-30959 AC1 - the overview lists a contract with the overview fields
    Given a fresh asset with:
      | field        | value                 |
      | resourceName | Overview item ${uuid} |
    And a contract requested at the provider
    When I send "GET" to the provider CM at "/contracts?contractNegotiationId=${nid}"
    Then the response status is 200
    And the response list "items" has 1 items
    And the item of the response list "items" with "contractNegotiationId" "${nid}" has:
      | field                 | value         |
      | contractAgreementId   | ${aid}        |
      | contractDefinitionId  | ${did}        |
      | assetId               | ${assetId}    |
      | resourceDescriptionId | ${rdId}       |
      | providerId            | ${providerId} |
      | providerName          | Provider One  |
      | consumerId            | ${consumerId} |
      | consumerName          | Consumer One  |
      | partyRole             | PROVIDER      |
      | status                | REQUESTED     |
      | reasonCode            | null          |
      | consentStatus         | NOT_DEFINED   |
    And the response field "items.0.createdDate" is a date-time
    And the response field "items.0.lastUpdatedDate" is a date-time

  @SIMPL-30959 @D6
  Scenario: SIMPL-30959 AC2 - a filter that matches nothing gives an empty list
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/contracts?contractNegotiationId=${nid}"
    Then the response status is 200
    And the response field "items" is an empty list

  @SIMPL-30959 @D6 @stub
  Scenario: SIMPL-30959 AC1 - the overview filters by resource name, contains and ignoring case
    Given the variable "tag" is "${uuid}"
    And a fresh asset with:
      | field        | value                   |
      | resourceName | Filter Probe ${tag} One |
    And a contract requested at the provider
    When I send "GET" to the provider CM at "/contracts?resourceName=filter%20probe%20${tag}"
    Then the response status is 200
    And the response list "items" contains an item with "contractNegotiationId" "${nid}"
    And every item of the response list "items" has "resourceName" containing "filter probe ${tag}" ignoring case

  @SIMPL-30959 @D6 @stub
  Scenario: SIMPL-30959 AC1 - the overview filters by counterparty name and status
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/contracts?consumerName=consumer%20one&status=REQUESTED"
    Then the response status is 200
    And the response list "items" contains an item with "contractNegotiationId" "${nid}"
    And every item of the response list "items" has "status" "REQUESTED"
    And every item of the response list "items" has "consumerName" containing "consumer one" ignoring case
    And the response list "items" is sorted by "createdDate" descending

  @SIMPL-30959 @D6
  Scenario: The overview accepts several statuses separated by commas
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/contracts?contractNegotiationId=${nid}&status=ACTIVE,REQUESTED"
    Then the response status is 200
    And the response list "items" has 1 items

  @SIMPL-30959 @D6
  Scenario: A status filter that excludes the contract does not list it
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/contracts?contractNegotiationId=${nid}&status=ACTIVE"
    Then the response status is 200
    And the response field "items" is an empty list

  @SIMPL-30959 @D6 @D11
  Scenario: An unknown status filter is refused with 400
    When I send "GET" to the provider CM at "/contracts?status=PENDING"
    Then the response is a problem with status 400 and type "argumentTypeMismatch"

  @SIMPL-30960 @D6 @D60 @C3
  Scenario: SIMPL-30960 AC1 - the details carry the record, the allowed actions and the history
    Given a contract requested at the provider
    When I send "GET" to the provider CM at "/contracts/${nid}"
    Then the response status is 200
    And the response fields are:
      | field                 | value         |
      | contractNegotiationId | ${nid}        |
      | status                | REQUESTED     |
      | partyRole             | PROVIDER      |
      | revocationRequestedBy | null          |
      | validationResult      | null          |
      | humanReadable         | null          |
    And the response field "contractOfferId" is present
    And the response field "validationReasonCode" is present
    And the response field "validatedAt" is present
    And the response field "consumerSignatureDate" is present
    And the response field "providerSignatureDate" is present
    And the response field "usageContractHash" is present
    And the response field "allowedActions" is an empty list
    # C3: connector callback entries may follow later, so the entries are not counted
    And the response fields are:
      | field                | value      |
      | history.0.entryType  | TRANSITION |
      | history.0.fromStatus | null       |
      | history.0.toStatus   | REQUESTED  |
      | history.0.trigger    | CONNECTOR  |
      | history.0.actor      | null       |

  @SIMPL-30960 @D6 @D60 @consumer @stub
  Scenario: SIMPL-30960 AC1 - the Consumer details carry the human-readable offer
    Given an offer created at the consumer
    When I send "GET" to the consumer CM at "/contracts/${nid}"
    Then the response status is 200
    And the response field "humanReadable" contains "${resourceName}"
    And the response field "usageContractHash" is the SHA-256 of the response field "humanReadable"
    And the response field "allowedActions" contains "ACCEPT"
    And the response field "allowedActions" contains "REJECT"

  @SIMPL-30960 @D6 @D12
  Scenario: The details of an unknown negotiation give 404
    Given fresh negotiation ids
    When I send "GET" to the provider CM at "/contracts/${nid}"
    Then the response is a problem with status 404 and type "notFound"

  @SIMPL-30960 @D61 @stub @two-party @slow
  Scenario: The history is append-only and shown oldest first
    Given an active contract between both parties
    Then the provider contract history is ordered oldest first
    And the consumer contract history is ordered oldest first
    And every provider contract history entry has the fields of D61
    And every consumer contract history entry has the fields of D61
