> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30214**

# SIMPL-30214: [UI][IMPROVEMENT] Allow user responsible for self description to read and update the status of a resource consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30214** (User story)

## Text

_This story extends SIMPL-28599, SIMPL-28602 to allow the SD_PUBLISHER user to view and update the status of a consent assertion._\
----\
**As a**

Data Provider End User who is responsible for creating and publishing the resource descriptions on the catalogue (SD_PUBLISHER)

**I want**

via UI, view the details of a resource consent assertion and update its status

**So that**

I can manage consent assertions linked to my resource descriptions

**Acceptance Criteria**

1. [ALL ACs of SIMPL-28599 must extend also to the SD_PUBLISHER role]
1. [ALL ACs of SIMPL-28602 must extend also to the SD_PUBLISHER role, with an exception below]
1. [IMPROVES AC4 of SIMPL-28602]\
   **GIVEN** that I am an authenticated user without the CONSENT_M role and without the SD_PUBLISHER role\
   **WHEN** I open the detail page for a resource consent assertion\
   **THEN** I see the "Role Restricted Access" page\
   **AND** a message stating "Access denied. Your role does not grant permission to view this content."
1. **GIVEN** that I am an authenticated user with the SD_PUBLISHER role (and not CONSENT_M role)\
   **WHEN** I open the detail page for a resource consent assertion\
   **THEN** the button to edit the consent assertion is not visible
1. **GIVEN** that I am an authenticated user with the SD_PUBLISHER role (and not CONSENT_M role)\
   **WHEN** I open the detail page for a contract consent assertion\
   **THEN** I see an error stating "You don't have the permissions to see this consent assertion"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
