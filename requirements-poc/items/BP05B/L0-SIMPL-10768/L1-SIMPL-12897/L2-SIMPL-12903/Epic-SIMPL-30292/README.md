> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../README.md) · [What the levels mean](../../../../../../levels.md) · [All items](../../../../../../all-items.md) · [By business process](../../../../../../processes.md)

Folder: [items](../../../../../README.md) / [BP05B](../../../../README.md) / [L0-SIMPL-10768](../../../README.md) / [L1-SIMPL-12897](../../README.md) / [L2-SIMPL-12903](../README.md) / **Epic-SIMPL-30292**

# SIMPL-30292: A Provider triggers the update of the resource consent assertion related to a selected resource description via UI

**Epic (deliverable, testable feature)**

**Trace:** [BP05B](../../../../README.md) › [SIMPL-10768](../../../README.md) (L0) › [SIMPL-12897](../../README.md) (L1) › [SIMPL-12903](../README.md) (L2) › **SIMPL-30292** (Epic)

## Text

**As a** Provider\
**I want to** trigger the update of the resource consent assertion related to a selected resource description via UI\
**So that** I can ensure that the access to the resource is aligned the resource consent status.

**Acceptance criterion 1**

1. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions and consent\
   **And** I am retrieving the details of a resource description in the catalogue\
   **When** I check the resource consent status\
   **And** I choose to update the resource consent status\
   **Then** I am redirected to the consent manager to perform the change

**Acceptance criterion 2**

2. **Given** I am authenticated as a Provider\
   **And** I have a role authorising the management of resource descriptions and consent\
   **When** I have updated the status of a resource consent assertion\
   **Then** I am redirected to the catalogue\
   **And** the resource descriptions affected by the resource consent assertion are shown.

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-12903](../README.md) (L2) A Provider consults the details of one of its own resource descriptions |
| **Children** | [SIMPL-36072](Story-SIMPL-36072/README.md) (User story) A Provider selects a resource description and triggers the update of the resource consent status |
