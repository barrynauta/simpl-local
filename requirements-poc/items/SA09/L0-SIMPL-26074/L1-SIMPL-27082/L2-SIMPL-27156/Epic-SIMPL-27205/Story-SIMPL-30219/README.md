> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-30219**

# SIMPL-30219: [UI][IMPROVEMENT] Add creation date and update date to consent assertion list

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-30219** (User story)

## Text

**As a** Simpl-Open End User responsible with the Consent Manager (CONSENT_M) role, responsible for managing consent assertions\
**I want to** via UI, view the creation and update date of consent assertions in the list\
**So that** I can quickly identify the most recently created or modified assertions and keep an accurate overview of consent activity

**Acceptance Criteria**

1. **Given** that I am logged in with the CONSENT_M role\
   **And** consent assertions have been created in the past\
   **When** I open the page to list consent assertions\
   **Then** the page displays a paginated table with these additional column headers Creation Timestamp and Last Update Timestamp\
   **And** the columns shows the date and time of the creation or update of the assertion

2. **Given** I am an authenticated user with CONSENT_M role\
   **When** I open the page to list consent assertions\
   **And** I specify the sort criterion (Creation Date, Last Update Timestamp)\
   **Then** the system sorts the consent assertions table to display items reflecting the sorting criterion (both ascending and descending).

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
