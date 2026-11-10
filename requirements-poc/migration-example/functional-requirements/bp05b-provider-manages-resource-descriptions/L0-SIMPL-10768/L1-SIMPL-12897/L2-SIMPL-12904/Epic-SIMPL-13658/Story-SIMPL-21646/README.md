> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../../README.md) · [What the levels mean](../../../../../../../../levels.md) · [All items](../../../../../../../../all-items.md) · [By business process](../../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12904](../../README.md) / [Epic-SIMPL-13658](../README.md) / **Story-SIMPL-21646**

# SIMPL-21646: [UI] A provider consults the version history of a resource description

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12904](../../README.md) (L2) › [SIMPL-13658](../README.md) (Epic) › **SIMPL-21646** (User story)

## Text

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via UI\
**So that** I can review it and decide if any action should be taken.

#### Technical Details

From a technical perspective, the **sd‑tooling‑ui** component will invoke an API exposed by the **sd‑tooling‑be** component (defined through ticket **SIMPL‑21645** and implemented through ticket \SIMPL-21647).\
From a UI perspective, the following implementation is envisaged:

- The user accesses a summary page showing the resource descriptions created by the provider user. This summary displays only the latest version of each resource description.
- For each item in the list of resource descriptions, a button will be available that allows the user to navigate to a new view containing the list of resource descriptions representing previous versions of the current one. By clicking on one of these entries, the user will be able to view its details.

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions via UI

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to view the version history of one of my own resource descriptions\
   **Then** the version history of this resource description is displayed.

**Scenario:** Error while retrieving the version history

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to view the version history of one of my own resource descriptions\
   **And** an error occurs while retrieving the version history\
   **Then** the version history is not displayed\
   **And** I am informed that the version history cannot be retrieved

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-13658](../README.md) (Epic) A Provider consults the version history of one of its own resource descriptions |
