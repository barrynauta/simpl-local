> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14732](../README.md) / **Epic-SIMPL-17409**

# SIMPL-17409: A Provider adds resource type specific metadata to a new Resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14732](../README.md) (L2) › **SIMPL-17409** (Epic)

## Text

#### Storycard

**As a** Provider end user\
**I want to** add resource type specific metadata to a new resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider adds resource type specific metadata to a new Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** I can add resource type specific metadata as defined in the schema of the selected resource type to the new resource description.

**Scenario:** A Provider finishes adding resource type specific metadata to a new Resource description via UI

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **And** I have added resource type specific metadata to the new resource description\
   **Then** I can navigate to the next step in the wizard.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14732](../README.md) (L2) A Provider adds resource type specific metadata to a new Resource description |
| **Children** | [SIMPL-18344](Story-SIMPL-18344/README.md) (User story) [UI] Add data resource type specific metadata<br>[SIMPL-20969](Story-SIMPL-20969/README.md) (User story) [UI] Add application resource type specific metadata<br>[SIMPL-21590](Story-SIMPL-21590/README.md) (User story) [UI] Add infrastructure resource type specific metadata |
