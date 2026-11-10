> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [functional-requirements](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-14687](../../README.md) / [L2-SIMPL-14864](../README.md) / **Epic-SIMPL-17799**

# SIMPL-17799: The Governance Authority checks the version number of a new version of a Resource description

**Epic (deliverable, testable feature)** · Status **Defined**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-14687](../../README.md) (L1) › [SIMPL-14864](../README.md) (L2) › **SIMPL-17799** (Epic)

## Text

#### Storycard

**As** Governance Authority\
**I want to** automatically check the version number of a new version of a resource description\
**So that** the published resource descriptions are consistent, reliable and compliant with pre-defined standards.

#### Acceptance criteria

**Scenario:** The Governance Authority checks the version number of a new version of a resource description(Version number validation - positive case)

1. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with a correct version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **Then** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "valid".

**Scenario:** The Governance Authority checks the version number of a new version of a resource description(Version number validation - negative case)

2. **Given** an algorithm to calculate and verify version numbers of resource description is defined by me, the Governance Authority\
   **When** I, the Governance Authority, receive a new version of a resource description with an incorrect version number\
   **And** the Provider is authorised with identity attributes to manage resource descriptions\
   **Then** it is checked that the version number of the new version of a Resource description is correct based on the version numbering algorithm\
   **And** version number validation for the new version of a Resource description has the result "invalid".

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-14864](../README.md) (L2) The Governance Authority validates the versioning of a new version of a Resource description |
| **Children** | [SIMPL-21596](Story-SIMPL-21596/README.md) (User story) [API] The Governance Authority checks the version number of a new version of a Resource description |
