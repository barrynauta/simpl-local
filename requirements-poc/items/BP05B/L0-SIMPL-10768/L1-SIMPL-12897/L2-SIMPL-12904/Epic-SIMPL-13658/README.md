> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-12897](../../README.md) / [L2-SIMPL-12904](../README.md) / **Epic-SIMPL-13658**

# SIMPL-13658: A Provider consults the version history of one of its own resource descriptions

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-12897](../../README.md) (L1) › [SIMPL-12904](../README.md) (L2) › **SIMPL-13658** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** retrieve the version history of one of my own resource descriptions via UI or API\
**So that** I can review it and decide if any action should be taken.

#### Acceptance criteria

**Scenario:** A Provider consults the version history of one of its own resource descriptions

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **When** I request to retrieve the version history of one of my own resource descriptions via UI or API\
   **Then** the version history of this resource description is returned.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12904](../README.md) (L2) A Provider consults the version history of one of its own resource descriptions |
| **Children** | [SIMPL-21646](Story-SIMPL-21646/README.md) (User story) [UI] A provider consults the version history of a resource description<br>[SIMPL-21647](Story-SIMPL-21647/README.md) (User story) [API] A provider consults the version history of a resource description via API (SD tooling)<br>[SIMPL-21648](Story-SIMPL-21648/README.md) (User story) [API] The Governance authority provides the version history of a resource description via API (Query mapper) |
