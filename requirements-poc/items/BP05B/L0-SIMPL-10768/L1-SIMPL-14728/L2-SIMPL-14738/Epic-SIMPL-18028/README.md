> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14738](../README.md) / **Epic-SIMPL-18028**

# SIMPL-18028: A Provider updates common metadata of a new version of a  Resource description via UI

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14738](../README.md) (L2) › **SIMPL-18028** (Epic)

## Text

#### Storycard

**As a** Provider end user\
**I want to** update the common metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider updates common metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Common metadata" in the wizard\
   **Then** the common metadata are prefilled with the values from the previous version\
   **And** I can update common metadata.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14738](../README.md) (L2) A Provider updates the common metadata of a new version of a Resource description |
| **Children** | [SIMPL-18683](Story-SIMPL-18683/README.md) (User story) [UI] Provider updates common metadata to a new version of Resource description<br>[SIMPL-25147](Story-SIMPL-25147/README.md) (User story) [API] API for retrieving a resourceAddress info (EdcConnectorAdapter)<br>[SIMPL-25148](Story-SIMPL-25148/README.md) (User story) [API] API for retrieving a resourceAddress info (sd-tooling-be)<br>[SIMPL-25170](Story-SIMPL-25170/README.md) (User story) [UI] Retrieve resourceAddress info for updating/visualizing resource description<br>[SIMPL-25713](Story-SIMPL-25713/README.md) (User story) [API] Implement API to atomically enrich and sign a resourceDescription (SD-Tooling)) |
