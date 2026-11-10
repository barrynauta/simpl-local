> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14728](../../README.md) / [L2-SIMPL-14743](../README.md) / **Epic-SIMPL-16252**

# SIMPL-16252: A Provider updates the contract clauses, resulting in a new version of the resource description via UI

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14728](../../README.md) (L1) › [SIMPL-14743](../README.md) (L2) › **SIMPL-16252** (Epic)

## Text

**As a** Provider\
**I want to** update contract clauses resulting in a new version of the resource description\
**So that** the terms and conditions for using and providing a resource are defined

**Acceptance criteria:**

**Scenario:** A Provider updates the contract clauses, resulting in a new version of the resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Contract clauses" in the wizard\
   **Then** the contract clauses attributes are prefilled with the values from the previous version\
   **And** I can update the contents of the attributes.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14743](../README.md) (L2) A Provider updates the set of contract clauses of a new version of a resource description |
| **Children** | [SIMPL-26274](Story-SIMPL-26274/README.md) (User story) A Provider updates the contract clauses, resulting in a new version of the resource description via UI |
