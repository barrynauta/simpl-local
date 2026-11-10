> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-26083](../../../README.md) / [L2-SIMPL-27195](../../README.md) / [Epic-SIMPL-27206](../README.md) / **Story-SIMPL-29116**

# SIMPL-29116: [UI] Data Provider updates consent assertion status from consent assertion list

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-26083](../../../README.md) (L1) › [SIMPL-27195](../../README.md) (L2) › [SIMPL-27206](../README.md) (Epic) › **SIMPL-29116** (User story)

## Text

**As a**

Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions

**I want to**

via UI, update the status of a consent assertion from the consent assertion list page

**So that**

the consent status of assertions accurately reflects the current consent basis for the linked data resources

Acceptance Criteria

1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I open the page that lists consent assertions\
   **AND** an assertion in the list is in ACTIVE status\
   **THEN** I see a button in the actions column that allows me to withdraw the consent assertion
1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I open the page that lists consent assertions\
   **AND** an assertion in the list is in WITHDRAWN status\
   **THEN** I see a button in the actions column that allows me to Activate the consent assertion
1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I click on the button to activate a consent assertion\
   **THEN** I see a pop-up window asking for the reason of activation
1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I click on the button to withdraw a consent assertion\
   **THEN** I see a pop-up window asking for the reason of withdrawal
1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I fill the reason of activation (min 3, max 1024 characters, the length validation will exclude whitespaces before and after the user input)\
   **THEN** the Activate button is enabled
1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I fill the reason of withdrawal (min 3, max 1024 characters, the length validation will exclude whitespaces before and after the user input))\
   **THEN** the Withdraw button is enabled
1. **GIVEN** that I am logged in with CONSENT_M role\
   **AND** I clicked on the button to withdraw a consent assertion from the list\
   **AND** the consent assertion status is still ACTIVE at the time of confirmation, matching the status at the initial fetch of the list\
   **WHEN** I click on the button to confirm the withdrawal\
   **THEN** the consent assertion is withdrawn\
   **AND** the list of assertion is updated\
   **AND** the previously existing filters remain set
1. **GIVEN** that I am logged in with CONSENT_M role\
   **AND** I clicked on the button to activate a consent assertion from the list\
   **AND** the consent assertion status is still WITHDRAWN at the time of confirmation, matching the status at the initial fetch of the list\
   **WHEN** I click on the button to confirm the activation\
   **THEN** the consent assertion is activated\
   **AND** the list of assertion is updated\
   **AND** the previously existing filters remain set
1. **GIVEN** that I am logged in with CONSENT_M role\
   **AND** I clicked on the button to activate a consent assertion from the list\
   **AND** the consent assertion status is no longer WITHDRAWN at the time of confirmation\
   **WHEN** I click on the button to confirm the activation\
   **THEN** an error is shown, saying that "The status of the assertion has changed, please refresh the page"
1. **GIVEN** that I am logged in with CONSENT_M role\
   **AND** I clicked on the button to withdraw a consent assertion from the list\
   **AND** the consent assertion status is no longer ACTIVE at the time of confirmation\
   **WHEN** I click on the button to confirm the withdrawal\
   **THEN** an error is shown, saying that "The status of the assertion has changed, please refresh the page"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27206](../README.md) (Epic) Data Provider withdraws and reactivates consent assertions |
