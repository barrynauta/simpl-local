> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14738](../README.md) / **Epic-SIMPL-19293**

# SIMPL-19293: A Provider starts the creation of a new version of a Resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14738](../README.md) (L2) › **SIMPL-19293** (Epic)

## Text

#### Storycard

**As a** Provider end user\
**I want to** start the creation of a new version of a Resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider starts the creation of a new version of a Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I demand to create a new version of this Resource description\
   **Then** I am guided to the first step of the wizard based on the latest version of the underlying schema for this resource type.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14738](../README.md) (L2) A Provider updates the common metadata of a new version of a Resource description |
| **Children** | [SIMPL-21651](Story-SIMPL-21651/README.md) (User story) [UI] A Provider starts the creation of a new version of a Resource description via UI |
