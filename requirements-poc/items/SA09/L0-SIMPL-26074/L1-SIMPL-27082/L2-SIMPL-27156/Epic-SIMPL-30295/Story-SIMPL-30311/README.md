> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-30295](../README.md) / **Story-SIMPL-30311**

# SIMPL-30311: [API][IMPROVEMENT] User responsible for managing contracts gets contract UI filter url in a contract consent assertion

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-30295](../README.md) (Epic) › **SIMPL-30311** (User story)

## Text

*This US improves task SIMPL-28594 and SIMPL-28595 in such a way that contract* *consent assertions will include a link to the contract UI, which will show a prefiltered list with the only contract linked to the assertion.*\
----

**As a** Data Provider End User who is responsible for managing contracts (CONTRACT_M)\
**I want** via API, obtain a direct link from a consent assertion to the related contract in the contract manager\
**So that** I can quickly navigate to the contract linked to a contract consent assertion without having to manually search for it in the contract UI

**Acceptance Criteria**

1. [IMPROVES AC1 of SIMPL-28594]

1. **Given** I am an authenticated user with CONTRACT_M role or CONSENT_M role\
   **And** the contract UI template URL is configured in the application configuration of the consent manager backend component\
   **When** I send a GET /tier1/v1/consentAssertions API request\
   **Then** I receive a 200 OK response\
   **And** I receive a paginated list of consent assertions\
   **And** in addition to the parameters already listed, each assertion contains a linkedResourceUrl field with a URL of the contract UI

   - [IMPROVES AC1 of SIMPL-28595]

2. **Given** I am an authenticated user with the CONTRACT_M role or CONSENT_M role\
   **And** the contract UI template URL is configured in the application configuration of the consent manager backend component\
   **And** a consent assertion has been created in the past\
   **When** I GET /tier1/v1/consentAssertions/{consentAssertionId} on that assertion\
   **Then** I receive a 200 Ok response\
   **And** in addition to the parameters already listed, the response also contains a linkedResourceUrl field with a URL of the contract UI

   - The documentation of the consent management backend component must include a dedicated configuration that allows to configure a contractUrlTemplate parameter in the format of "https://example.com?contractConsentAssertionId=%s, where %s will be replaced with the consent assertion id.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-30295](../README.md) (Epic) Data provider manages consent assertions - Improvement |
