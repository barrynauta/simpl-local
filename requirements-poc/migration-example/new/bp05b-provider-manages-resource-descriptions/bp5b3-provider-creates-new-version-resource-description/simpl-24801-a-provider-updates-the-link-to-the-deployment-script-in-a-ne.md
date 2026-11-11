<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the link to the deployment script in a new version of an infrastructure Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.09 Update resource type specific metadata](../README.md#bp05b09-update-resource-type-specific-metadata).

In the backlog (Jira) it is item SIMPL-24801. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

**As a** Provider end user,\
**I want to** update the link to the deployment script in a new version of an infrastructure resource description via UI,\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

**Acceptance criteria:**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new version of an infrastructure resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** the deployment script is prefilled with the values from the previous version\
   **And** I can retrieve the list of active deployment scripts to link to the new version of the resource description.\
   **And** I can select one of the available deployments scripts to link it to the new version of the resource description.

## User stories

1. [[UI] Update infrastructure deployment script link](#user-story-1-ui-update-infrastructure-deployment-script-link)

### User story 1: [UI] Update infrastructure deployment script link

Backlog item SIMPL-28325

**As a** Provider end user,\
**I want to** update the link to the deployment script in a new version of an infrastructure resource description via UI,\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

**Acceptance criteria:**

**AC1 - A Provider updates the link to the infrastructure deployment script in a new version of a resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new version of an infrastructure resource description\
   **When** I reach the step "Asset Properties" in the wizard\
   **Then** the infrastructure resource type specific metadata are prefilled with the values from the previous version, including the previously selected deployment script\
   **And** the list of active deployment scripts is retrieved\
   **And** this list is presented in a dropdown\
   **And** I can select one of the available deployments scripts.

**AC2 - A Provider finishes updating the link to the infrastructure deployment script in a new version of a resource description via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new version of an infrastructure resource description\
   **When** I reach the step "Asset Properties" in the wizard\
   **And** I update the link to the infrastructure deployment script\
   **Then** the updated deployment script is associated with the new version of the resource description being created\
   **And** I can navigate to the next step in the wizard

---

Back to [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
