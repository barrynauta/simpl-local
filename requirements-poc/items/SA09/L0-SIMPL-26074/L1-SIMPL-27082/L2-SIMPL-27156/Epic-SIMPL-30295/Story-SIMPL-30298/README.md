> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30298**

# SIMPL-30298: [UI][IMPROVEMENT] Allow user responsible for contract management to read and update the status of a contract consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30298** (User story)

## Text

_This story extends SIMPL-28599, SIMPL-28602 to allow the CONTRACT_M user to view and update the status of a consent assertion._\
----\
**As a**

Data Provider End User who is responsible for managing contract (CONTRACT_M)

**I want**

via UI, view the details of a contract consent assertion and update its status

**So that**

I can manage consent assertions linked to established contracts

**Acceptance Criteria**

1. [ALL ACs of SIMPL-28599 must extend also to the CONTRACT_M role]
1. [ALL ACs of SIMPL-28602 must extend also to the CONTRACT_M role, with an exception below]
1. [IMPROVES AC4 of SIMPL-28602]\
   **GIVEN** that I am an authenticated user without the CONSENT_M role and without the CONTRACT_M role\
   **WHEN** I open the detail page for a contract consent assertion\
   **THEN** I see the "Role Restricted Access" page\
   **AND** a message stating "Access denied. Your role does not grant permission to view this content."
1. **GIVEN** that I am an authenticated user with the CONTRACT_M role\
   **WHEN** I open the detail page for a contract consent assertion\
   **THEN** the button to edit the consent assertion is not visible
1. **GIVEN** that I am an authenticated user with the CONTRACT_M role\
   **AND** the GET /consentAssertions/consentAssertionId returns the URL of the catalogue in the linkedResourceUrl parameter (it may not be present depending on backend configurations, as per SIMPL-30311)\
   **WHEN** I open the detail page for a contract consent assertion\
   **THEN** I see a link "View contracts"
1. **GIVEN** that I am an authenticated user with the CONTRACT_M role\
   **AND** the GET /consentAssertions/consentAssertionId does NOT return the URL of the catalogue in the linkedResourceUrl parameter (it may not be present depending on backend configurations, as per SIMPL-30311)\
   **WHEN** I open the detail page for a contract consent assertion\
   **THEN** I don't see the link "View contracts"
1. **GIVEN** that I am an authenticated user with the CONTRACT_M role\
   **WHEN** I open the detail page for a resource consent assertion\
   **THEN** I see an error stating "you don't have the permissions to see this consent assertion"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
