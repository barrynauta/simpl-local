@stub @two-party @bp07
Feature: A whole usage contract establishment between Provider and Consumer
  The stub plays both connectors (Appendix C.2). The only human step is the
  Consumer's decision (D17). Both CMs end with the same ids (D18).

  @D17 @D18 @D37 @D58 @slow
  Scenario: BP07 - an accepted offer ends ACTIVE at both parties with both VCs in both wallets
    Given a negotiation started at the stub
    When the stub negotiation reaches state "OFFERED"
    And the consumer accepts the offer
    Then the stub negotiation reaches state "FINALIZED"
    And the stub negotiation passed the states "STARTED, EXISTENCE_CHECKED, OFFERED, CONSUMER_SIGNED, PROVIDER_SIGNED, CONSUMER_VERIFIED, FINALIZED"
    And both contracts reach status "ACTIVE"
    And the provider contract has:
      | field               | value    |
      | contractAgreementId | ${aid}   |
      | partyRole           | PROVIDER |
    And the consumer contract has:
      | field               | value    |
      | contractAgreementId | ${aid}   |
      | partyRole           | CONSUMER |
    And the provider wallet holds 2 credentials for the negotiation
    And the consumer wallet holds 2 credentials for the negotiation
    And the stub received the callback "isPresent" with "false" from the provider
    And the stub received the callback "signed" with "true" from the consumer
    And the stub received the callback "isValid" with "true" from the provider
    And the stub received the callback "signed" with "true" from the provider
    And the stub received the callback "isValid" with "true" from the consumer

  @SIMPL-32509 @D19 @E2 @slow
  Scenario: BP07.03 - a second request while a contract is open ends at the existence check
    Given an active contract between both parties
    When a negotiation is started at the stub
    Then the stub negotiation ends in state "TERMINATED" with reason "EXISTING_CONTRACT"
    And the stub received the callback "isPresent" with "true" from the provider
    And the provider has no contract for the negotiation
    And the consumer has no contract for the negotiation

  @SIMPL-32511 @D20
  Scenario: BP07.04 - an offer that cannot be prepared ends the negotiation at both parties
    Given a fresh asset with:
      | field      | value    |
      | providerId | rogue-01 |
    And a negotiation started at the stub
    Then the stub negotiation ends in state "TERMINATED" with reason "OFFER_FAILED"
    And the provider contract reaches status "TERMINATED"
    And the consumer has no contract for the negotiation

  @SIMPL-19933 @D29 @E11 @P2 @slow
  Scenario: BP07.08 - a Provider rejection reaches the Consumer as FAILED with PROVIDER_REJECTED
    Given a negotiation started at the stub that stops after "CONSUMER_SIGNED"
    When the stub negotiation reaches state "OFFERED"
    And the consumer accepts the offer
    And the stub negotiation reaches state "CONSUMER_SIGNED"
    And the stub override "consumerVcOverride" is the "invalid signature" variant of the consumer credential
    And the stub negotiation is advanced
    Then the stub negotiation ends in state "TERMINATED" with reason "PROVIDER_REJECTED"
    And the provider contract reaches status "FAILED"
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field      | value             |
      | reasonCode | PROVIDER_REJECTED |

  @SIMPL-21948 @D31 @E10 @P4 @slow
  Scenario: BP07.12 - a Consumer rejection of the Provider VC terminates the Provider contract
    Given a negotiation started at the stub that stops after "PROVIDER_SIGNED"
    When the stub negotiation reaches state "OFFERED"
    And the consumer accepts the offer
    And the stub negotiation reaches state "PROVIDER_SIGNED"
    And the stub override "providerVcOverride" is the "expired" variant of the provider credential
    And the stub negotiation is advanced
    Then the stub negotiation ends in state "TERMINATED" with reason "CONSUMER_REJECTED"
    And the consumer contract reaches status "FAILED"
    And the consumer contract has:
      | field                | value                   |
      | validationReasonCode | CONTRACT_VALIDITY_ISSUE |
    And the provider contract reaches status "TERMINATED"
    And the provider contract has:
      | field      | value                   |
      | reasonCode | COUNTERPARTY_TERMINATED |
