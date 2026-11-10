> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-12897](../../README.md) / [L2-SIMPL-12903](../README.md) / **Epic-SIMPL-12905**

# SIMPL-12905: A Provider consults the full contents of one of its own Resource descriptions via UI and API

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-12897](../../README.md) (L1) › [SIMPL-12903](../README.md) (L2) › **SIMPL-12905** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** retrieve the full contents of one of my own Resource descriptions via UI or API\
**So that** I can check it and decide if any action should be taken.

#### Acceptance criteria

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I request to retrieve one particular of my own Resource descriptions via UI or API\
   **Then** the full contents of this Resource description is returned.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12903](../README.md) (L2) A Provider consults the details of one of its own resource descriptions |
| **Children** | [SIMPL-9838](Story-SIMPL-9838/README.md) (User story) Display self-description details<br>[SIMPL-11293](Story-SIMPL-11293/README.md) (User story) API to provide the full content of a publisher's resource description<br>[SIMPL-15622](Story-SIMPL-15622/README.md) (User story) [API] API to provide the full contents of a provider's own resource description to the UI |
