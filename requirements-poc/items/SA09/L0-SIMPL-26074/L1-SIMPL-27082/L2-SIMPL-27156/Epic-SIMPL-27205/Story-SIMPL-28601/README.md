> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-28601**

# SIMPL-28601: [UI] Data Provider lists and filters consent assertions

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-28601** (User story)

## Text

**As a**

Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions

**I want to**

via UI, view the list of consent assertions created within the system

**so that**

I have a clear overview of the consent assertions created within my agent, which allows me to properly manage the assertions.

**Acceptance Criteria**

1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** no consent assertions have been created before\
   **WHEN** I open the page to list consent assertions\
   THEN I see a message "There are no consent assertions currently available. To create a new consent assertion click the button below and fill in the required information"\
   **AND** I see a button to create a consent assertion
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** consent assertions have been created in the past\
   **WHEN** I open the page to list consent assertions\
   **THEN** the page displays a paginated table with the following column headers "Name", "Type", "Status", "Actions"
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** consent assertions have been created in the past\
   **WHEN** I open the page to list consent assertions\
   **THEN** the table displays a paginated list of consent assertions with their corresponding values for "Name", "Type", "Status"\
   **AND** the "Action" column contains\
   an icon to navigate to the consent assertion edit page (SIMPL-28600)\
   an icon to navigate to the consent assertion detail page (SIMPL-29113)\
   an icon to change the status of the assertion (if the assertion is in the ACTIVE or WITHDRAWN status) (SIMPL-29116)\
   an icon to open the list of key-pair metadata, visible if the assertion has some metadata defined
1. **GIVEN** I am an authenticated user with CONSENT_M role\
   **AND** an assertion has some metadata defined\
   **WHEN** I click on the medatata icon\
   **THEN** a pop-up showing the list of key-value pair appears
1. **GIVEN** I am an authenticated user with CONSENT_M role\
   **WHEN** I open the page to list consent assertions\
   **AND** I specify search filters\
   Name (text area with case-insensitive partial match)\
   Type (single-select dropdown)\
   Status (single-select dropdown)\
   Metadata (text area with case-insensitive partial match on metadata keys and values)\
   **THEN** the system filters the consent assertion table to display only the items that match the selected filter criteria.
1. **GIVEN** I am an authenticated user with CONSENT_M role\
   **WHEN** I filter consent assertions\
   **AND** no assertion match the criteria\
   **THEN** a message is shown in the table: "No data to display"
1. **GIVEN** I am an authenticated user with CONSENT_M role\
   **WHEN** I open the page to list consent assertions\
   **AND** I specify the sort criterion (Name, Type, Status)\
   **THEN** the system sorts the consent assertions table to display items reflecting the sorting criterion.
1. **GIVEN** that I am not logged in with the CONSENT_M role\
   **WHEN** I open the page to list consent assertions\
   **THEN** I see the "Role Restricted Access" page\
   **AND** a message stating "Access denied. Your role does not grant permission to view this content."
1. **GIVEN** that I am not logged in\
   **WHEN** I open the page to list consent assertions\
   **THEN** I am redirected to the agent login page
1. **GIVEN** that I am logged in with the CONSENT_M role\
   **AND** the participant agent is not fully onboarded (note. GET tier1/v2/participant API of the authentication provider component returns an empty participant ID or a 404)\
   **WHEN** I open the page to list consent assertions\
   **THEN** I am redirected to a new error page\
   **AND** I see a message stating *"The Simpl-Open agent is not fully onboarded. Complete the onboarding of the agent to create the consent assertions"*

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
