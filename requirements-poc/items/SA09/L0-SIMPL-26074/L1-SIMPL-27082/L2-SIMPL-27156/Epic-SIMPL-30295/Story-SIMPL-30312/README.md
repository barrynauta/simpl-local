> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30312**

# SIMPL-30312: [UI][IMPROVEMENT] User responsible for managing contracts gets views contract UI filter url in a contract consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30312** (User story)

## Text

*This US improves SIMPL-28602 in such a way that contract* *consent assertions details page will include a link to the contract UI,* *which will show a prefiltered list with the only contract linked to the assertion.*

*Note for testers: this User Story only covers the presence of the link and verifies that it contains the assertion ID. The behaviour of the linked page is out of scope and will be addressed in other User Stories.*\
----

**As a** Data Provider End User who is responsible for creating and managing contracts (CONTRACT_M), viewing the details of a contract consent assertion\
**I want** via UI, view the link that brings me to the related contract in the contract manager\
**So that** I can quickly navigate to the contract linked to a contract consent assertion without having to manually search for them in the contract UI

**Acceptance Criteria**

1. **Given** that I am an authenticated user with the CONTRACT_M role\
   **And** the GET /consentAssertions/consentAssertionId returns the URL of the contract UI in the linkedResourceUrl parameter (it may not be present depending on backend configurations, as per SIMPL-30299)\
   **When** I open the detail page for a contract consent assertion\
   **Then** I see a link "View contract"

2. **Given** that I am an authenticated user with the CONTRACT_M role\
   **And** the GET /consentAssertions/consentAssertionId does NOT return the URL of the contract UI in the linkedResourceUrl parameter (it may not be present depending on backend configurations, as per SIMPL-30299)\
   **When** I open the detail page for a contract consent assertion\
   **Then** I don't see the link "View contract"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
