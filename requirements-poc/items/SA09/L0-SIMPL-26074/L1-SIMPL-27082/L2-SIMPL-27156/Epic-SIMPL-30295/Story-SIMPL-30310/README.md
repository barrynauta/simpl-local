> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30310**

# SIMPL-30310: [UI][IMPROVEMENT] User responsible for self description views catalogue UI link in a resource consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30310** (User story)

## Text

This US improves SIMPL-28602 in such a way that resource consent assertions details page will include a link to the catalogue UI, which will show a prefiltered list with all the resource descriptions (zero or more) linked to the assertion.

*Note for testers: this User Story only covers the presence of the link and verifies that it contains the assertion ID. The behaviour of the linked page is out of scope and will be addressed in other User Stories.*\
----\
**As a**

Data Provider End User who is responsible for creating and publishing the resource descriptions on the catalogue (SD_PUBLISHER)

**I want**

via UI, view the link that brings me to the related resource descriptions list in the catalogue

**So that**

I can quickly navigate to the entities linked to a resource consent assertion without having to manually search for them in the catalogue UI.

**Acceptance Criteria**

1. **GIVEN** that I am an authenticated user with the SD_PUBLISHER role\
   **AND** the GET /consentAssertions/consentAssertionId returns the URL of the catalogue in the linkedResourceUrl parameter (it may not be present depending on backend configurations, as per SIMPL-30299)\
   **WHEN** I open the detail page for a resource consent assertion\
   **THEN** I see a link "View resource descriptions"
1. **GIVEN** that I am an authenticated user with the SD_PUBLISHER role\
   **AND** the GET /consentAssertions/consentAssertionId does NOT returns the URL of the catalogue in the linkedResourceUrl parameter (it may not be present depending on backend configurations, as per SIMPL-30299)\
   **WHEN** I open the detail page for a resource consent assertion\
   **THEN** I don't see the link "View resource descriptions"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
