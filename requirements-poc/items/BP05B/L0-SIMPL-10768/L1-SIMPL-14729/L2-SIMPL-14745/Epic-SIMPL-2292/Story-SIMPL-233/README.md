> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14729](../../../README.md) / [L2-SIMPL-14745](../../README.md) / [Epic-SIMPL-2292](../README.md) / **Story-SIMPL-233**

# SIMPL-233: Interactively Create a Service Self-Description

**User story (implementable work)** · Status **Ready for DG Acceptance**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14729](../../../README.md) (L1) › [SIMPL-14745](../../README.md) (L2) › [SIMPL-2292](../README.md) (Epic) › **SIMPL-233** (User story)

## Text

**As a** service provider **I want** interactively fill a self-description for my dataset in the UI **so that** I can ensuring compliance with the required structure and metadata.

**Linked Sovereign X US ID:** TAE_US_029, TAE_US_032, TAE_US_033

**Technical** Specification:

The UI Frontend for the SD-Tool should support the user in specifying the self-description. Depending on the allowed data type the SD-Tool should support appropriate input methods, for instance a date picker for a date attribute.

**Acceptance Criteria:**

1. **A service provider can only input numbers if the field is numerical.**
1. -**A service provider can use a date picker for fields that contain date information.**-
1. **Supports DATA, -APP, Infrastructure-**
1. -**A service provider is provided feedback if he reaches the maximum length for a text field.**-
1. **It is possible to use tab to jump to the next input field.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-2292](../README.md) (Epic) A Provider signs and  submits a new Resource description or a new version of a Resource description |
