> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [SA09](../../../../../README.md) / [L0-SIMPL-26074](../../../../README.md) / [L1-SIMPL-26083](../../../README.md) / [L2-SIMPL-27195](../../README.md) / [Epic-SIMPL-27206](../README.md) / **Story-SIMPL-28592**

# SIMPL-28592: [API] Data Provider returns consent assertion status

**User story (implementable work)** · Development: **Developed**

**Trace:** [SA09](../../../../../README.md) › [SIMPL-26074](../../../../README.md) (L0) › [SIMPL-26083](../../../README.md) (L1) › [SIMPL-27195](../../README.md) (L2) › [SIMPL-27206](../README.md) (Epic) › **SIMPL-28592** (User story)

## Text

**As a** Simpl-Open component\
**I want to** via API, retrieve the status of a consent assertion by its identifier\
**So that** access control decisions can be enforced based on the current assertion status

**Acceptance Criteria**

1. **Given** I am any caller\
   **When** I invoke the GET /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the consentAssertionId corresponds to an existing assertion\
   **Then** I obtain a 200 OK response\
   **And** the body of the response contains the overall status of the assertion.

2. **Given** I am any caller\
   **When** I invoke the GET /tier1/v1/consentAssertions/{consentAssertionId}/status API\
   **And** the consentAssertionId does not correspond to an existing assertion\
   **Then** I obtain a 404 Not Found response

## Where it fits

| | |
| --- | --- |
| **Business process** | [SA09](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/sa09-data-provider-manages-consent-assertions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/sa09-data-provider-manages-consent-assertions)) |
| **Parent** | [SIMPL-27206](../README.md) (Epic) Data Provider withdraws and reactivates consent assertions |
