> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14728](../../../README.md) / [L2-SIMPL-14739](../../README.md) / [Epic-SIMPL-24801](../README.md) / **Story-SIMPL-28325**

# SIMPL-28325: [UI] Update infrastructure deployment script link

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14728](../../../README.md) (L1) › [SIMPL-14739](../../README.md) (L2) › [SIMPL-24801](../README.md) (Epic) › **SIMPL-28325** (User story)

## Text

**As a** Provider end user,\
**I want to** update the link to the deployment script in a new version of an infrastructure resource description via UI,\
**so that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

**Acceptance criteria:**

**AC1 - A Provider updates the link to the infrastructure deployment script in a new version of a resource description via UI**

**Given** I am authenticated as a Provider end user\
**and** I have a role authorising the management of Resource Descriptions\
**and** I am creating a new version of an infrastructure resource description\
**When** I reach the step "Asset Properties" in the wizard\
**Then** the infrastructure resource type specific metadata are prefilled with the values from the previous version, including the previously selected deployment script\
**And** the list of active deployment scripts is retrieved\
**And** this list is presented in a dropdown\
**And** I can select one of the available deployments scripts.

**AC2 - A Provider finishes updating the link to the infrastructure deployment script in a new version of a resource description via UI**

**Given** I am authenticated as a Provider end user\
**and** I have a role authorising the management of Resource Descriptions\
**and** I am creating a new version of an infrastructure resource description\
**When** I reach the step "Asset Properties" in the wizard

**and** I update the link to the infrastructure deployment script\
**Then** the updated deployment script is associated with the new version of the resource description being created\
**and** I can navigate to the next step in the wizard

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24801](../README.md) (Epic) A Provider updates the link to the deployment script in a new version of an infrastructure Resource description via UI |
