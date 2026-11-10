> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-12897](../../../README.md) / [L2-SIMPL-12903](../../README.md) / [Epic-SIMPL-30292](../README.md) / **Story-SIMPL-36072**

# SIMPL-36072: A Provider selects a resource description and triggers the update of the resource consent status

**User story (implementable work)** · Status **PBI Defined**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-12897](../../../README.md) (L1) › [SIMPL-12903](../../README.md) (L2) › [SIMPL-30292](../README.md) (Epic) › **SIMPL-36072** (User story)

## Text

**As a** Provider\
**I want to** trigger the update of the resource consent assertion related to a selected resource description via UI\
**So that** I can ensure that the access to the resource is aligned the resource consent status.

**Acceptance criteria**

**AC1 - Select a resource from the catalogue for which I possibly want to update the resource consent status**

**Given** I am authenticated as a Provider user\
**and** I have a role authorising the management of resource descriptions and consent\
**and** one of my own resource descriptions has a linked resource consent assertion\
**When** I retrieve the details of this resource description from the catalogue\
**Then** the resource consent status is displayed\
**And** a button to update the resource consent status is available

**AC2 - I decide to update the resource consent status**

**Given** I am authenticated as a Provider user\
**and** I have a role authorising the management of resource descriptions and consent\
**and** a resource description along with a resource consent status is displayed\
**When** I choose to update the resource consent status\
**Then** I am redirected to the consent manager to perform the change

**AC3 - Observe the result of the consent change in the catalogue**

**Given** I am authenticated as a Provider\
**and** I have a role authorising the management of resource descriptions and consent\
**When** I have updated the status of a resource consent assertion\
**Then** I am redirected to the catalogue\
**and** the resource descriptions affected by the resource consent assertion are shown

**AC4 - Select a resource from the catalogue for which I want to update the resource consent status (Negative scenario: Resource consent not defined)**

**Given** I am authenticated as a Provider user\
**and** I have a role authorising the management of resource descriptions and consent\
**and** a selected resource for which resource consent is not defined\
**When** I retrieve the details of this resource description from the catalogue\
**Then** the resource consent status is not displayed\
**And** a button to update the resource consent status is not shown

**AC5 -** **I decide to update the resource consent status** **(Negative scenario: Consent manager not available)**

**Given** I am authenticated as a Provider user\
**and** I have a role authorising the management of resource descriptions and consent\
**and** a resource description along with a resource consent status is displayed\
**and** the consent manager is currently not available\
**When** I choose to update the resource consent status\
**Then** I receive an error message that shows that the consent manager is not available

**AC6 - Observe the result of the consent change in the catalogue (Negative scenario: Catalogue not available)**

**Given** I am authenticated as a Provider\
**and** I have a role authorising the management of resource descriptions and consent\
**and** the catalogue is not available\
**When** I have updated the status of a resource consent assertion\
**Then** I am redirected to the catalogue\
**And** I receive an error message that shows that the catalogue is not available

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-30292](../README.md) (Epic) A Provider triggers the update of the resource consent assertion related to a selected resource description via UI |
