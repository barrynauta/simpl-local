> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14732](../README.md) / **Epic-SIMPL-24800**

# SIMPL-24800: A Provider links a deployment script to a new infrastructure Resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14732](../README.md) (L2) › **SIMPL-24800** (Epic)

## Text

**As a** Provider end user,\
**I want to** link a deployment script to a new infrastructure resource description via UI,\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

**Acceptance criteria:**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new infrastructure resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** I can retrieve the list of active deployment scripts to link to the new resource description.\
   **And** I can select one of the available deployments scripts to link it to the new resource description.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14732](../README.md) (L2) A Provider adds resource type specific metadata to a new Resource description |
| **Children** | [SIMPL-28293](Story-SIMPL-28293/README.md) (User story) [UI] Add infrastructure deployment script link |
