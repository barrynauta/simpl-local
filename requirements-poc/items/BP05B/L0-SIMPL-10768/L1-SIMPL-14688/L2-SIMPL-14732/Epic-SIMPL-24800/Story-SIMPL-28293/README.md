> **Simpl-Open requirements, proof of concept.** One requirement item, shown with its context. New here? [Start here](../../../../../../../README.md) · [What the levels mean](../../../../../../../levels.md) · [All items](../../../../../../../all-items.md) · [By business process](../../../../../../../processes.md)

Folder: [items](../../../../../../README.md) / [BP05B](../../../../../README.md) / [L0-SIMPL-10768](../../../../README.md) / [L1-SIMPL-14688](../../../README.md) / [L2-SIMPL-14732](../../README.md) / [Epic-SIMPL-24800](../README.md) / **Story-SIMPL-28293**

# SIMPL-28293: [UI] Add infrastructure deployment script link

**User story (implementable work)** · Status **DevTeam PBI is ready for Implementation check**

**Trace:** [BP05B](../../../../../README.md) › [SIMPL-10768](../../../../README.md) (L0) › [SIMPL-14688](../../../README.md) (L1) › [SIMPL-14732](../../README.md) (L2) › [SIMPL-24800](../README.md) (Epic) › **SIMPL-28293** (User story)

## Text

**As a** Provider end user,\
**I want to** link a deployment script to a new infrastructure resource description via UI,\
**so that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

**Acceptance criteria:**

**AC1 - A Provider adds a link to the infrastructure deployment script to a new resource description via UI**

**Given** I am authenticated as a Provider end user\
**and** I have a role authorising the management of Resource Descriptions\
**and** I am creating a new infrastructure resource description\
**When** I reach the step "Asset Properties" in the wizard\
**Then** the list of active deployment scripts is retrieved\
**And** this list is presented in a dropdown\
**And** I can select one of the available deployments scripts

**AC2 - A Provider finishes adding a link to the infrastructure deployment script to a new resource description via UI**

**Given** I am authenticated as a Provider end user\
**and** I have a role authorising the management of Resource Descriptions\
**and** I am creating a new infrastructure resource description\
**When** I reach the step "Asset Properties" in the wizard

**and** I select an infrastructure deployment script\
**Then** the selected deployment script is associated with the current resource description being created\
**and** I can navigate to the next step in the wizard

## Where it fits

| | |
| --- | --- |
| **Business process** | [BP05B](../../../../../README.md) ([public process page](https://code.europa.eu/simpl/simpl-open/foundations/business-processes/-/blob/main/bp05b-provider-manages-resource-descriptions/README.md) · [requirements site](https://simpl-programme.ec.europa.eu/book-page/bp05b-provider-manages-resource-descriptions)) |
| **Parent** | [SIMPL-24800](../README.md) (Epic) A Provider links a deployment script to a new infrastructure Resource description via UI |
