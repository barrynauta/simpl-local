> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-26083](../../../README.md) / [L2-SIMPL-27195](../../README.md) / [Epic-SIMPL-27206](../README.md) / **Story-SIMPL-28599**

# SIMPL-28599: [UI] Data Provider updates consent assertion status from consent assertion details page

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-26083](../../../README.md) (L1) › [SIMPL-27195](../../README.md) (L2) › [SIMPL-27206](../README.md) (Epic) › **SIMPL-28599** (User story)

## Text

*This story describes only the status change action. The rest of the page is described in SIMPL-28602.*\
----

**As a** Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions\
**I want to** via UI, update the status of a consent assertion from the consent assertion details page\
**So that** the consent status of assertions accurately reflects the current consent basis for the linked data resources

**Acceptance Criteria**

1. **Given** that I am logged in with CONSENT_M role\
   **When** I open the detail page for a consent assertion\
   **And** the assertion is in ACTIVE status\
   **Then** I see a button that allows me to withdraw the consent assertion

2. **Given** that I am logged in with CONSENT_M role\
   **When** I open the detail page for a consent assertion\
   **And** the assertion is in WITHDRAWN status\
   **Then** I see a button that allows me to Activate the consent assertion

3. **Given** that I am logged in with CONSENT_M role\
   **When** I click on the button to activate a consent assertion\
   **Then** I see a pop-up window asking for the reason of activation

4. **Given** that I am logged in with CONSENT_M role\
   **When** I click on the button to withdraw a consent assertion\
   **Then** I see a pop-up window asking for the reason of withdrawal

5. **Given** that I am logged in with CONSENT_M role\
   **When** I fill the reason of activation (min 3, max 1024 characters, the length validation will exclude whitespaces before and after the user input))\
   **Then** the Activate button is enabled

6. **Given** that I am logged in with CONSENT_M role\
   **When** I fill the reason of withdrawal (min 3, max 1024 characters, the length validation will exclude whitespaces before and after the user input))\
   **Then** the Withdraw button is enabled

7. **Given** that I am logged in with CONSENT_M role\
   **And** I clicked on the button to withdraw a consent assertion\
   **And** the consent assertion status is still ACTIVE at the time of confirmation, matching the status at the initial fetch\
   **When** I click on the button to confirm the withdrawal\
   **Then** a pop-up stating that the operation comleted succesfully appears\
   **And** the new status is displayed

8. **Given** that I am logged in with CONSENT_M role\
   **And** I clicked on the button to activate a consent assertion\
   **And** the consent assertion status is still WITHDRAWN at the time of confirmation, matching the status at the initial fetch\
   **When** I click on the button to confirm the activation\
   **Then** a pop-up stating that the operation completed succesfully appears\
   **And** the new status is displayed

9. **Given** that I am logged in with CONSENT_M role\
   **And** I clicked on the button to activate a consent assertion\
   **And** the consent assertion status is no longer WITHDRAWN at the time of confirmation\
   **When** I click on the button to confirm the activation\
   **Then** an error is shown, saying that "The status of the assertion has changed, please refresh the page"

10. **Given** that I am logged in with CONSENT_M role\
   **And** I clicked on the button to withdraw a consent assertion\
   **And** the consent assertion status is no longer ACTIVE at the time of confirmation\
   **When** I click on the button to confirm the withdrawal\
   **Then** an error is shown, saying that "The status of the assertion has changed, please refresh the page"

11. **Given** that I am logged in with CONSENT_M role\
   **When** I click on the button to update the status of a consent assertion\
   **And** a technical error occurs (network or 5XX backend error)\
   **Then** I see a popup that tells that says "Operation failed. Please try again later"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27206](../README.md) (Epic) Data Provider withdraws and reactivates consent assertions |
