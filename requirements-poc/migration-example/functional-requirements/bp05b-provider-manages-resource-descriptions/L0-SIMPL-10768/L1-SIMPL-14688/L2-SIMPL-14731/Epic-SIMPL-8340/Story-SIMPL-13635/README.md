> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-8340](../README.md) / **Story-SIMPL-13635**

# SIMPL-13635: [API] Resource Source Address Validation Upon Provider Resource Submission

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-8340](../README.md) (Epic) › **SIMPL-13635** (User story)

## Text

**As a** provider,\
**I want to** validate a resource source address submitted as part of the resource description,\
**So that** I can ensure the resource source address is valid before completing the submission process.

**Acceptance Criteria:**

- **AC1 - Successful Validation**

1. **Given** I am a provider filling in a resource source address based on a resource source template\
   **And** all fields are valid with respect to the template (correct types, formats, and values),\
   **When** I submit the resource source address\
   **Then** the submitted resource source address is successfully validated against resource source template,\
   **And** a success message is shown, and the resource source address is stored.

   - AC2 - Failed Validation

2. **Given** I am a provider filling in a resource source address based on a resource source template\
   **And** some provided input is not valid for the selected template\
   **When** I submit the resource source address\
   **Then** validation against the selected template fails\
   **And** the validation errors are displayed\
   **And** the submission is blocked until all validation issues are resolved.

**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&t=LT29Q6aB2kEPn6ew-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2565-53401&t=Tg2TKqZO3dR3zidj-4)

**Internal page (including conceptual Data Model):**

(internal page)

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-8340](../README.md) (Epic) A Provider defines the resource source address for resource sharing |
