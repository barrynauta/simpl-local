@consumer @stub
Feature: Preparing the usage contract offer at the Consumer (BP07.04)
  The connector asks the Consumer CM to prepare the offer data. CM reads the
  asset and the party names, builds the human-readable offer and its hash,
  stores the record in REQUESTED, hands the offer to its UIBE and notifies the
  Consumer user (SIMPL-32511, SIMPL-35526, SIMPL-30960, D20 to D22, E3).

  Background:
    Given a fresh asset with:
      | field        | value                     |
      | resourceName | Air quality data set      |
      | description  | Hourly air quality values |
    And fresh negotiation ids

  @SIMPL-32511 @SIMPL-35526 @bp07 @D20 @D38 @E3
  Scenario: SIMPL-32511 AC1 and SIMPL-35526 AC1 - the offer is created and returned with all fields
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And the response fields are:
      | field                   | value            |
      | contractAgreementId     | ${aid}           |
      | contractDefinitionId    | ${did}           |
      | status                  | CREATED          |
      | contractNegotiationId   | ${nid}           |
      | assetId                 | ${assetId}       |
      | resourceDescriptionId   | ${rdId}          |
      | resourceDescriptionName | ${resourceName}  |
      | providerId              | ${providerId}    |
      | providerName            | Provider One     |
      | consumerId              | ${consumerId}    |
      | consumerName            | Consumer One     |
      | contractOfferId         | ${offerId}       |
      | contractStatus          | REQUESTED        |
      | offerUrl                | ${consumerUiUrl}/contractOffers/${nid} |
    And the response field "usageContract" is not null
    And the response field "usageContractHash" is the SHA-256 of the response field "usageContract"
    And the consumer contract has:
      | field             | value     |
      | status            | REQUESTED |
      | partyRole         | CONSUMER  |
      | contractOfferId   | ${offerId}|
    And the consumer contract field "usageContractHash" is the SHA-256 of its "humanReadable"

  @SIMPL-30960 @D21
  Scenario: The human-readable offer lists the resource, the parties, the policies and the clauses
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And the response field "usageContract" contains "Air quality data set"
    And the response field "usageContract" contains "${rdId}"
    And the response field "usageContract" contains "${assetId}"
    And the response field "usageContract" contains "Provider One"
    And the response field "usageContract" contains "Consumer One"
    And the response field "usageContract" contains "Hourly air quality values"
    And the response field "usageContract" contains "Research use only"
    And the response field "usageContract" contains "clauses.pdf"
    And the response field "usageContract" does not contain "<html"

  @SIMPL-30960 @D21 @D75
  Scenario: Every value in the human-readable offer is HTML-escaped
    Given a fresh asset with:
      | field        | value                             |
      | resourceName | <script>alert(1)</script> & Co    |
    And fresh negotiation ids
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And the response field "usageContract" does not contain "<script>"
    And the response field "usageContract" contains "&lt;script&gt;"
    And the response field "usageContractHash" is the SHA-256 of the response field "usageContract"

  @SIMPL-35526 @D20 @D7 @uibe @oidc
  Scenario: SIMPL-35526 AC1 - the prepared offer is handed to the Consumer UIBE with status CREATED
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And I remember the response field "usageContractHash" as "hash"
    When I send "GET" to the consumer UIBE at "/contractOffers/${nid}" as "consumer-admin"
    Then the response status is 200
    And the response fields are:
      | field                 | value   |
      | contractNegotiationId | ${nid}  |
      | status                | CREATED |
      | hashData              | ${hash} |
      | contractData          | null    |
    And the response field "humanReadableData" contains "Air quality data set"

  @SIMPL-35526 @D20 @D70
  Scenario: SIMPL-35526 - the Consumer CM does not call the connector back after creating the offer
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And the stub received no connector callback for the negotiation

  @SIMPL-35526 @SIMPL-21966 @D22 @events
  Scenario: SIMPL-35526 AC1 - the Consumer user is notified that an offer is available
    Given I listen to the Kafka topic "notifications"
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And a message for the negotiation is published on "notifications" with:
      | field   | value                                                   |
      | channel | EMAIL                                                   |
      | to.0    | consumer-01@example.test                                |
      | subject | Usage contract offer available for review: ${nid}       |
      | message | <contains> ${consumerUiUrl}/contractOffers/${nid} |
    And the last message has exactly the fields "channel, to, cc, subject, message"
    And the last message has the header "correlationId"
    And the consumer contract history has an entry with:
      | field     | value             |
      | entryType | NOTIFICATION_SENT |

  @SIMPL-35526 @D22 @events
  Scenario: Without a contact email no notification is published and the offer is still created
    Given I listen to the Kafka topic "notifications"
    And the stub fault "DIRECTORY:NO_CONTACT" is set for the negotiation
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And no message for the negotiation is published on "notifications"
    And the consumer contract history has an entry with:
      | field     | value                |
      | entryType | NOTIFICATION_SKIPPED |

  @SIMPL-32511 @D20
  Scenario: SIMPL-32511 AC2 - an asset not owned by the Provider in the request is refused with 403
    Given a fresh asset with:
      | field      | value    |
      | providerId | rogue-01 |
    And fresh negotiation ids
    When the connector asks the consumer CM to create the offer
    Then the response is a problem with status 403 and type "forbidden"
    And the consumer has no contract for the negotiation

  @SIMPL-32511 @D20
  Scenario: SIMPL-32511 AC3 - an asset that does not exist is refused with 404
    Given a fresh unregistered asset id
    When the connector asks the consumer CM to create the offer
    Then the response is a problem with status 404 and type "notFound"
    And the consumer has no contract for the negotiation

  @SIMPL-35526 @D20 @D2
  Scenario: The create operation is refused at the Provider with 400 wrongParty
    When the connector asks the provider CM to create the offer with "consumerId" set to "${consumerId}"
    Then the response is a problem with status 400 and type "wrongParty"

  @SIMPL-32511 @D20
  Scenario: A second create for a known negotiation id is refused with 400 alreadyExists
    Given an offer created at the consumer
    When the connector asks the consumer CM to create the offer
    Then the response is a problem with status 400 and type "alreadyExists"
    And the consumer has exactly one contract for the negotiation

  @SIMPL-32511 @D20 @D11
  Scenario Outline: A create request without <field> is refused with 400
    When the connector asks the consumer CM to create the offer without "<field>"
    Then the response is a problem with status 400 and type "validationFailure"
    And the consumer has no contract for the negotiation

    Examples:
      | field                 |
      | contractNegotiationId |
      | assetId               |
      | providerId            |
      | consumerId            |
      | contractOfferId       |

  @SIMPL-32511 @D20 @D16
  Scenario: A create request with an id outside the allowed pattern is refused with 400
    When the connector asks the consumer CM to create the offer with "contractOfferId" set to "offer id with spaces"
    Then the response is a problem with status 400 and type "validationFailure"

  @openapi-only @D11
  Scenario: A create request with a non-JSON body is refused with 415
    When I send "POST" to the consumer CM at "/credentials/agreements/${aid}/definitions/${did}/create" with content type "text/plain" and body "x"
    Then the response is a problem with status 415 and type "unsupportedMediaType"

  @openapi-only @D16
  Scenario: A create request with a contractAgreementId that is not a UUID is refused with 400
    When I send "POST" to the consumer CM at "/credentials/agreements/not-a-uuid/definitions/${did}/create" with JSON:
      """
      {"contractNegotiationId": "${nid}", "assetId": "${assetId}", "providerId": "${providerId}",
       "consumerId": "${consumerId}", "contractOfferId": "${offerId}"}
      """
    Then the response is a problem with status 400 and type "argumentTypeMismatch"

  @SIMPL-32511 @D49 @events
  Scenario: Creating the record publishes a status event with previousStatus null
    Given I listen to the Kafka topic "simpl.contract.status.v1"
    When the connector asks the consumer CM to create the offer
    Then the response status is 201
    And a message for the negotiation is published on "simpl.contract.status.v1" with:
      | field          | value       |
      | participantId  | consumer-01 |
      | partyRole      | CONSUMER    |
      | previousStatus | null        |
      | newStatus      | REQUESTED   |
      | trigger        | CONNECTOR   |
    And the last message key is the negotiation id
