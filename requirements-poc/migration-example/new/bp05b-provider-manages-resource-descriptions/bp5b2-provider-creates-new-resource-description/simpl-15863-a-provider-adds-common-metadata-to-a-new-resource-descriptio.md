<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider adds common metadata to a new Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.01 Create a new resource description](../README.md#bp05b01-create-a-new-resource-description).

In the backlog (Jira) it is item SIMPL-15863. It has 2 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** add common metadata to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema.

#### Acceptance criteria

**Scenario:** A Provider end user adds common metadata to a new resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Common metadata" in the wizard\
   **Then** I can add common metadata as defined in the schema of the selected resource type to the new resource description.

**Scenario:** A Provider end user finishes adding common metadata to a new resource description via UI

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Common metadata" in the wizard\
   **And** I have added common metadata to the new resource description\
   **Then** I can navigate to the next step in the wizard.

## User stories

1. [[UI] Displaying Self-description Fields in the UI - SD Tool](#user-story-1-ui-displaying-self-description-fields-in-the-ui---sd-tool)
2. [[UI] Provider adds metadata property groups to a new Resource description](#user-story-2-ui-provider-adds-metadata-property-groups-to-a-new-resource-description)

### User story 1: [UI] Displaying Self-description Fields in the UI - SD Tool

Backlog item SIMPL-232

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

### User story 2: [UI] Provider adds metadata property groups to a new Resource description

Backlog item SIMPL-18343

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

---

Back to [5B.2.2 A Provider adds common metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b22-a-provider-adds-common-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
