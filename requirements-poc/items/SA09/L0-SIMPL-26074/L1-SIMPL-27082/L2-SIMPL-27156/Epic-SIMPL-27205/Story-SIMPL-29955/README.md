> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-27082](../../../README.md) / [L2-SIMPL-27156](../../README.md) / [Epic-SIMPL-27205](../README.md) / **Story-SIMPL-29955**

# SIMPL-29955: [API][IMPROVEMENT] Data provider configures the maximum amount of metadata key-value pairs for a consent assertion

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-27082](../../../README.md) (L1) › [SIMPL-27156](../../README.md) (L2) › [SIMPL-27205](../README.md) (Epic) › **SIMPL-29955** (User story)

## Text

**As a** Simpl-Open Platform Administrator responsible for operating and configuring the Consent Management backend\
**I want to** configure a maximum size limit for the metadata key-value pair list on consent assertions\
**So that** the size of metadata remains within acceptable bounds and does not impact system performance or log manageability.

**Acceptance Criteria**

1. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the PATCH /tier1/v1/consentAssertions/{consentAssertionId} API\
   **And** the key-value metadata list length is greater than the configured max size\
   **Then** I obtain a 400 Bad Request\
   **And** the response contains a message stating "Maximum number of keypairs allowed is X"

2. **Given** I am an authenticated user with the Consent Manager role (CONSENT_M)\
   **When** I invoke the POST /tier1/v1/consentAssertions API\
   **And** the key-value metadata list length is greater than the configured max size\
   **Then** I obtain a 400 Bad Request\
   **And** the response contains a message stating "Maximum number of keypairs allowed is X"

   - The consent.metadata.max-size property must be documented in the component properties documentation and must have a default of 10 items.

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27205](../README.md) (Epic) Data Provider consults and updates consent assertions |
