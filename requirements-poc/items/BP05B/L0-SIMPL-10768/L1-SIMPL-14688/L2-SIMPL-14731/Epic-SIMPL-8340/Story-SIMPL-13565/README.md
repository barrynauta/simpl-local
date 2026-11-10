> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-8340](../README.md) / **Story-SIMPL-13565**

# SIMPL-13565: [UI] Selecting, Filling, and Submitting the Resource Source Template (Provider)

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-8340](../README.md) (Epic) › **SIMPL-13565** (User story)

## Text

**As a** provider,\
**I want to** select a resource source template associated with a sharing method, fill in the required details, and submit it as part of the resource description,\
**so that** I can configure and provide the necessary source address and metadata for resource sharing.

**Acceptance Criteria:**

- **AC1 - Template Selection**\
   **Given** I have selected a resource sharing method,\
   **When** I view the associated resource source templates,\
   **Then** I should be able to select one template from the list.
- **AC2 - Display of Template Fields**\
   **Given** a resource source template is selected,\
   **When** the UI loads,\
   **Then** I should see all fields defined by the selected template, including required and optional fields.
- **AC3 - Submission Action**\
   **Given** the template fields are displayed,\
   **When** I fill in the form\
   **And** I click the "Submit" button,\
   **Then** the data should be submitted for validation.

**Link to Figma mockup:**

[https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&t=LT29Q6aB2kEPn6ew-0](https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2369-12311&t=LT29Q6aB2kEPn6ew-0)

https://www.figma.com/design/YZZ6QWqLcVHlNhjnI81Wzm/Catalogue--negotiation-and-transfer-UX-UI?node-id=2565-53401&t=Tg2TKqZO3dR3zidj-4

**Internal page (including conceptual Data Model):**

[an internal page]

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-8340](../README.md) (Epic) A Provider defines the resource source address for resource sharing |
| **Also under** | [SIMPL-12250](../../../../L1-SIMPL-14728/L2-SIMPL-14738/Epic-SIMPL-12250/README.md) (Epic) A Provider updates the resource source address for resource sharing |
