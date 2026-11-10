> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30081**

# SIMPL-30081: [API][IMPROVEMENT] Allow user responsible for self description to read and update the status of a resource consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30081** (User story)

## Text

_This story extends SIMPL-28594, SIMPL-28595, SIMPL-28591 to allow the SD_PUBLISHER user to read and update the status of consent assertions._\
----\
**As a**

Data Provider End User who is responsible for creating and publishing the resource descriptions on the catalogue (SD_PUBLISHER)\
**

**I want** **

via API, to read the list of consent assertions, read the details of a resource consent assertion and update its status\
**

**So that**

I can manage consent assertions linked to my resource descriptions

**Acceptance Criteria**

1. [ALL ACS of SIMPL-28594 must extend to the SD_PUBLISHER role, specific exceptions below]
1. [EXTENDS AC1 of SIMPL-28594]\
   **GIVEN** I am an authenticated user with SD_PUBLISHER role (and not CONSENT_M role)\
   **WHEN** I send a GET /tier1/v1/consentAssertions API request\
   **THEN** I receive a 200 OK response\
   **AND** the response contains only assertions of type RESOURCE
1. [EXTENDS AC5 of SIMPL-28594]\
   **GIVEN** I am an authenticated user without CONSENT_M role or SD_PUBLISHER role\
   **WHEN** I send a GET /tier1/v1/consentAssertions\
   **THEN** I receive a 403 Forbidden response
1. [ALL ACS of SIMPL-28595 must extend to the SD_PUBLISHER role, specific exceptions below]
1. [EXTENDS AC5 of SIMPL-28595]\
   **GIVEN** I am an authenticated user without CONSENT_M or SD_PUBLISHER role\
   **WHEN** I send a GET /tier1/v1/consentAssertions/{consentAssertionId}\
   **THEN** I receive a 403 Forbidden response
1. **GIVEN** I am an authenticated user with SD_PUBLISHER role\
   **WHEN** I send a GET /tier1/v1/consentAssertions/{consentAssertionId}\
   **AND** the targeted consent assertion is not of type RESOURCE\
   **THEN** I receive a 403 error message\
   **AND** the response contains the message "You can only view resource consent assertions"
1. [ALL ACS of SIMPL-28591 must extend to the SD_PUBLISHER role, specific exceptions below]
1. [EXTENDS AC 11 of SIMPL-28591]\
   **GIVEN** I am an authenticated user without the CONSENT_M role or the SD_PUBLISHER role\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **THEN** I receive a 403 Forbidden response
1. **GIVEN** I am an authenticated user with SD_PUBLISHER role\
   **WHEN** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **AND** the targeted consent assertion is not of type RESOURCE\
   **THEN** I receive a 403 error message\
   **AND** the response contains the message "You can only change the status of resource consent assertions"

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
