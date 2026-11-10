> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30300**

# SIMPL-30300: [API][IMPROVEMENT] User responsible for self description gets catalogue UI filter url in a resource consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30300** (User story)

## Text

This US improves task SIMPL-28594 and SIMPL-28595 in such a way that resource consent assertions will include a link to the catalogue UI, which will show a prefiltered list with all the resource descriptions (zero or more) linked to the assertion\
----\
**As a**

Data Provider End User who is responsible for creating and publishing the resource descriptions on the catalogue (SD_PUBLISHER)

**I want**

via API, obtain a direct link from a consent assertion to the related resource descriptions list in the catalogue

**So that**

I can quickly navigate to the entities linked to a resource consent assertion without having to manually search for them in the catalogue UI.

**Acceptance Criteria**

1. [IMPROVES AC1 of SIMPL-28594]\
   **GIVEN** I am an authenticated user with SD_PUBLISHER role\
   **AND** the catalogue UI template URL is configured in the application configuration of the consent manager backend component\
   **WHEN** I send a GET /tier1/v1/consentAssertions API request\
   **THEN** I receive a 200 OK response\
   **AND** I receive a paginated list of consent assertions\
   **AND** in addition to the parameters already listed, each assertion contains a linkedResourceUrl field with a URL of the catalogue
1. [IMPROVES AC1 of SIMPL-28595]\
   **GIVEN** I am an authenticated user with the SD_PUBLISHER role\
   **AND** the catalogue UI template URL is configured in the application configuration of the consent manager backend component\
   **AND** a consent assertion has been created in the past\
   **WHEN** I GET /tier1/v1/consentAssertions/{consentAssertionId} on that assertion\
   **THEN** I receive a 200 Ok response\
   **AND** in addition to the parameters already listed, the response contains also a linkedResourceUrl field with a URL of the catalogue
1. The documentation of the consent management backend component must include a dedicated configuration that allows to input a catalogueUrlTemplate parameter in the format of "https://example.com?resourceConsentAssertionId=%s, where the %s will be replaced with the consent assertion id.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
