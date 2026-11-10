> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-28600**

# SIMPL-28600: [UI] Data Provider updates consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-28600** (User story)

## Text

**As a**

Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions

**I want to**

via UI, update the details of a consent assertion including name, description and metadata

**So that**

I can correct or enrich the assertion's information without having to recreate it.

**Acceptance Criteria**

1. **GIVEN** that I am logged in with CONSENT_M role\
   **WHEN** I open the edit page for a consent assertion\
   **THEN** I see\
   the name of the assertion (editable form, mandatory, min length 3, max length 255)\
   the type of the assertion\
   the description of the assertion (editable form, mandatory, min length 3, max length 2048)\
   a metadata section that allows both seeing existing metadata, update existing key-value pairs and add new key-value pairs (key min length 1, key max length 22, value min length 1, value max length 2048)\
   the assertion status (note the button to update the consent assertion is intentionally omitted in this view)\
   the creation and update timestamps of the assertion\
   a table showing the list of parties involved in the assertion, showing for each party\
   a "Type" column that shows the party type (CONSUMER or PROVIDER)\
   a "Participant ID" column showing the ID of the participant\
   a "Notified on status change" column showing if the participant must be notified upon assertion status change\
   a "Save" button\
   a "Cancel" button
1. **GIVEN** that I am logged in with CONSENT_M role\
   **AND** updated the consent assertion with valid data\
   **WHEN** I click on the Save button\
   **THEN** the name, description and metadata are updated\
   **AND** a success message appears that says that the consent assertion has been updated
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I fill name and description with invalid values\
   **THEN** the save button is disabled\
   **AND** an error messages are shown on the respective fields{}
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I fill two or more metadata key-value pairs with the same key (case-insensitive match)\
   **THEN** the save button becomes or remains disabled\
   **AND** an error is shown saying that the metadata keys should be unique
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I reach the maximum number of metadata entries configured in the application configuration (default to 10)\
   **THEN** the UI prevents me from adding more metadata entries
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I click on the cancel button\
   **THEN** a pop-up appears with the text "All unsaved changes will be lost. Do you want to proceed?"\
   **AND** upon confirmation I am redirected to the assertion details or list page (depending on the page I navigated from)
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** I open the page to update a consent assertion\
   **WHEN** I don't fill both the key and value of a metadata entry\
   **THEN** the save button becomes or remains disabled\
   **AND** an error is shown saying that the metadata must contain key and value
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** the participant agent is not fully onboarded (note. GET tier1/v2/participant API of the authentication provider component returns an empty participant ID or a 404)\
   **WHEN** I open the page to update a consent assertion\
   **THEN** I am redirected to a new error page\
   **AND** I see a message stating *"The Simpl-Open agent is not fully onboarded. Complete the onboarding of the agent to create the consent assertions"*

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
