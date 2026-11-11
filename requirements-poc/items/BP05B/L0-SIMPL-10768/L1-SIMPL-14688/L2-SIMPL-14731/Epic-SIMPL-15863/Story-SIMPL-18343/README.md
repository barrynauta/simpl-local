> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14731](../../README.md) / [Epic-SIMPL-15863](../README.md) / **Story-SIMPL-18343**

# SIMPL-18343: [UI] Provider adds metadata property groups to a new Resource description

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14731](../../README.md) (L2) › [SIMPL-15863](../README.md) (Epic) › **SIMPL-18343** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** add metadata to a new resource description via UI\
**So that** the contents of the resource description is complete for each property group

#### Acceptance criteria

**Scenario:** A Provider end user adds common metadata to a new resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I initiate the creation of a Resource Description in the UI\
   **When** I select a resource type\
   **Then** a screen is displayed allowing me to fill in the Resource Description properties\
   **And** the screen is compliant with the schema linked to the selected resource type, displaying a tab for each property group of the schema\
   **And** each tab displays a field for each of the properties in the property group, compliant with the resource type\
   **And** each tab contains buttons allowing me to move to the next or previous tab (if applicable)\
   **And** the last tab contains a button to sign and submit the Resource Description

#### Mockups:

This is an Example for one group for common properties. UI depends on the properties that are available on the groups.

Example:

| Wizard: step "Common metadata" |
| --- |
| !image-2025-09-13-20-50-55-010.png | thumbnail! |

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-15863](../README.md) (Epic) A Provider adds common metadata to a new Resource description via UI |
