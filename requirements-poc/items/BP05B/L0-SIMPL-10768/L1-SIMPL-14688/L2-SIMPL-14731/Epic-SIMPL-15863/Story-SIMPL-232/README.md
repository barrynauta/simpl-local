> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-15863](../README.md) / **Story-SIMPL-232**

# SIMPL-232: [UI] Displaying Self-description Fields in the UI - SD Tool

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-15863](../README.md) (Epic) › **SIMPL-232** (User story)

## Text

**As a** service provider\
**I want to** see the fields I need to specify for the self-description\
**So that** I can understand the mandatory and optional structure required for self-descriptions

**Linked Sovereign X US ID:** TAE_US_027, TAE_US_029

**Technical** Specification:

The UI Frontend for the SD-Tool should show the required fields for the self-description of the Service and if they are mandatory or have constraints. Which fields and their properties should be defined by a schema in YAML that can change and automatically leads to different UI.

**UI-Wireframe:**\
mock up docu (internal page)\
(image not shown)

**Acceptance Criteria:**

1. **A service provider can** **see the fields in the UI.**
1. **A service provider can** **see if a field is mandatory or not.**
1. **A service provider can** **see if a field has constraints.**
1. **Three different types of Service Offerings (APP, Data and Infra)**
1. **The UI is changed if the YAML Configuration of the schema changes.**

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-15863](../README.md) (Epic) A Provider adds common metadata to a new Resource description via UI |
