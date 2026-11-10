> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-29113**

# SIMPL-29113: [DEPRECATED] [UI] Data Provider views the details of a consent assertion

**User story (implementable work)** · Status **PBI Cancelled**

> 🔴 **Cancelled.** Kept here for history. It is not part of the current requirements.

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-29113** (User story)

## Text

*SIMPL-28602 describes how the detail page is rendered. The status update is described in SIMPL-28599.*\
----\
**As a**

Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions

**I want to**

via UI, view the details of a consent assertion

**So that**

I can have a clear overview of the assertion data, parties and status

**Acceptance Criteria**

1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I open the detail page of a consent assertion\
   **THEN** I see\
   the assertion name\
   the assertion description\
   the list of metadata key-value pairs\
   the assertion type\
   the creation timestamp\
   the update timestamp\
   the status of the assertion\
   a button to change the status of the assertion (see SIMPL-28599)\
   a table containing the list of the parties involved in the assertion
1. **GIVEN** that I am logged in with CONSENT_M role\
   **AND** updated the consent assertion with valid data\
   **WHEN** I click on the Save button\
   **THEN** a success message appears that says that the consent assertion has been updated
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I fill name and description with invalid values\
   **THEN** the save button is disabled\
   **AND** an error messages are shown on the respective fields{}
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I fill two or more metadata key-value pairs with the same key\
   **THEN** the submit button becomes or remains disabled\
   **AND** an error is shown saying that the metadata keys should be unique
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** the participant agent is not fully onboarded (note. GET tier1/v2/participant API of the authentication provider component returns an empty participant ID)\
   **WHEN** I open the page to create a consent assertion\
   **THEN** I am redirected to a new error page\
   **AND** I see a message stating *"The Simpl-Open agent is not fully onboarded. Complete the onboarding of the agent to create the consent assertions"*

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
