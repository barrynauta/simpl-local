> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14732](../README.md) / **Epic-SIMPL-21309**

# SIMPL-21309: A Provider uploads a deployment script to a new infrastructure Resource description via UI

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14732](../README.md) (L2) › **SIMPL-21309** (Epic)

## Text

#### Storycard

**As a** Provider end user\
**I want to** add a deployment script to a new infrastructure resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider adds a deployment script to a new infrastructure resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new infrastructure resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** I can add a deployment script to the new resource description.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14732](../README.md) (L2) A Provider adds resource type specific metadata to a new Resource description |
| **Children** | [SIMPL-10289](Story-SIMPL-10289/README.md) (User story) [UI] Select Resource Type "Infrastructure" and Open Deployment Script Upload<br>[SIMPL-13605](Story-SIMPL-13605/README.md) (User story) [UI] Upload Deployment Script in Popup Window |
