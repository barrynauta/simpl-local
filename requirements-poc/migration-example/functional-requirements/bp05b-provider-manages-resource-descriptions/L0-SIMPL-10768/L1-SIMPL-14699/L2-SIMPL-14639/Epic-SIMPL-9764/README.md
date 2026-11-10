> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14699](../../README.md) / [L2-SIMPL-14639](../README.md) / **Epic-SIMPL-9764**

# SIMPL-9764: A Provider requests the revocation of a resource description via UI or API

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14699](../../README.md) (L1) › [SIMPL-14639](../README.md) (L2) › **SIMPL-9764** (Epic)

## Text

#### Storycard

**As a** Provider\
**I want to** be able to revoke a Resource description via UI or API\
**So that** the Resource description cannot be used any more.

#### Acceptance criteria

**Scenario:** A Provider requests the revocation of a Resource description via UI or API

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **When** I want to revoke a Resource description\
   **Then** a revocation request is sent to the Governance authority.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14639](../README.md) (L2) A Provider requests the revocation of a resource description |
| **Children** | [SIMPL-19784](Story-SIMPL-19784/README.md) (User story) [UI] A Provider requests the revocation of a Resource description via UI<br>[SIMPL-19785](Story-SIMPL-19785/README.md) (User story) [API] Provider requests the revocation of a Resource description via API (SD tooling) |
