> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14739](../README.md) / **Epic-SIMPL-21310**

# SIMPL-21310: A Provider updates the deployment script of a new version of an infrastructure Resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14739](../README.md) (L2) › **SIMPL-21310** (Epic)

## Text

#### Storycard

**As a** Provider end user\
**I want to** update the deployment script of a new version of an infrastructure resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider updates the deployment script of a new version of an infrastructure Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new version of an infrastructure resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** the deployment script is prefilled with the values from the previous version\
   **Then** I can update the deployment script.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14739](../README.md) (L2) A Provider updates the resource type specific metadata of a new version of a Resource description |
| **Children** | None. |
