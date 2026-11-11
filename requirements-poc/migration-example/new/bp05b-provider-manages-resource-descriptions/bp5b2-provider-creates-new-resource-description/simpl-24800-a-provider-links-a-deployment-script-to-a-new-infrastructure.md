<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider links a deployment script to a new infrastructure Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.02 Add resource type specific metadata](../README.md#bp05b02-add-resource-type-specific-metadata).

In the backlog it is item SIMPL-24800, status **Defined**. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

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

## User stories

1. [[UI] Add infrastructure deployment script link](#user-story-1-ui-add-infrastructure-deployment-script-link) (DevTeam PBI is ready for Implementation check)

### User story 1: [UI] Add infrastructure deployment script link

**Status:** DevTeam PBI is ready for Implementation check (backlog item SIMPL-28293)

**As a** Provider end user,\
**I want to** link a deployment script to a new infrastructure resource description via UI,\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

**Acceptance criteria:**

**AC1 - A Provider adds a link to the infrastructure deployment script to a new resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new infrastructure resource description\
   **When** I reach the step "Asset Properties" in the wizard\
   **Then** the list of active deployment scripts is retrieved\
   **And** this list is presented in a dropdown\
   **And** I can select one of the available deployments scripts

**AC2 - A Provider finishes adding a link to the infrastructure deployment script to a new resource description via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new infrastructure resource description\
   **When** I reach the step "Asset Properties" in the wizard\
   **And** I select an infrastructure deployment script\
   **Then** the selected deployment script is associated with the current resource description being created\
   **And** I can navigate to the next step in the wizard

---

Back to [5B.2.3 A Provider adds resource type specific metadata to a new Resource description](../bp5b2-provider-creates-new-resource-description.md#5b23-a-provider-adds-resource-type-specific-metadata-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
