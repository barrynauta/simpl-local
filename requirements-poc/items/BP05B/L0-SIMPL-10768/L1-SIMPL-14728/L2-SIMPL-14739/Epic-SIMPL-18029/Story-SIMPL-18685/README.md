> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14739](../../README.md) / [Epic-SIMPL-18029](../README.md) / **Story-SIMPL-18685**

# SIMPL-18685: [UI] Update data resource type specific metadata to new RD version

**User story (implementable work)** · Development: **Developed**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14739](../../README.md) (L2) › [SIMPL-18029](../README.md) (Epic) › **SIMPL-18685** (User story)

## Text

#### Storycard

**As a** Provider end user\
**I want to** update the data resource type specific metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider updates data resource type specific metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Data metadata" in the wizard\
   **Then** the data resource type specific metadata are prefilled with the values from the previous version\
   **And** I can update the data resource type specific metadata.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-18029](../README.md) (Epic) A Provider updates the resource type specific metadata of a new version of a  Resource description via UI |
