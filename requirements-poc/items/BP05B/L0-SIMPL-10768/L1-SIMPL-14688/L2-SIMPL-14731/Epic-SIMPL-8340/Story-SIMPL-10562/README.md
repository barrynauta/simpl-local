> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-8340](../README.md) / **Story-SIMPL-10562**

# SIMPL-10562: [API] Extend the SD Tooling API for Retrieving Available Resource Sharing Methods and Templates (Provider)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-8340](../README.md) (Epic) › **SIMPL-10562** (User story)

## Text

**As a** provider user authorised to manage resource descriptions,\
**I want to** retrieve via an API a list of available resource sharing methods and their associated templates,\
**So that** I can provide a source address compliant with the requested sharing method when completing a resource description.

**Acceptance Criteria:**

- **Positive case**

1. **Given** I am authorised to manage resource descriptions,\
   **When** I request available resource sharing methods via the API,\
   **Then** I receive a list of supported resource sharing methods, each including its associated template and metadata needed for the resource description.

   - Negative case - unauthorised user

2. **Given** I am not authorised to manage resource descriptions,\
   **When** I attempt to retrieve available resource sharing methods via the API,\
   **Then** I receive an appropriate error response (e.g., 403 Forbidden).

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-8340](../README.md) (Epic) A Provider defines the resource source address for resource sharing |
