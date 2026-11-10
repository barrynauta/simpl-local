> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14688](../../README.md) / [L2-SIMPL-14730](../README.md) / **Epic-SIMPL-2285**

# SIMPL-2285: A Provider retrieves from the Governance Authority a list of published resource types

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14688](../../README.md) (L1) › [SIMPL-14730](../README.md) (L2) › **SIMPL-2285** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** retrieve a list of published resource types from the Governance Authority\
**So that** I can start the creation of a new resource description.

#### Acceptance criteria

**Scenario:** A Provider retrieves a list of published resource types

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource Descriptions\
   **When** I request a list of published resource types from the Governance Authority\
   **Then** I receive a list containing the published resource types.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14730](../README.md) (L2) A provider retrieves a Resource type |
| **Children** | [SIMPL-330](Story-SIMPL-330/README.md) (User story) [API] API for the Self-Description Tool<br>[SIMPL-15867](Story-SIMPL-15867/README.md) (User story) [UI] A provider consults a list of published resource types in the resource description manager UI<br>[SIMPL-16118](Story-SIMPL-16118/README.md) (User story) [API] A provider resource description manager retrieves a list of active schemas via API for the UI |
