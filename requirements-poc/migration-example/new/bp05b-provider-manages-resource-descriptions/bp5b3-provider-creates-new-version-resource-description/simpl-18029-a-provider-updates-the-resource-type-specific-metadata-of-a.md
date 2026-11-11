<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the resource type specific metadata of a new version of a Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.09 Update resource type specific metadata](../README.md#bp05b09-update-resource-type-specific-metadata).

In the backlog (Jira) it is item SIMPL-18029. It has 3 user stories, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** update the resource type specific metadata of a new version of resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider updates resource type specific metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** the resource type specific metadata are prefilled with the values from the previous version\
   **And** I can update the resource type specific metadata.

## User stories

1. [[UI] Update data resource type specific metadata to new RD version](#user-story-1-ui-update-data-resource-type-specific-metadata-to-new-rd-version)
2. [[UI] Update application resource type specific metadata to new RD version](#user-story-2-ui-update-application-resource-type-specific-metadata-to-new-rd-version)
3. [[UI] Update infrastructure resource type specific metadata to new RD version](#user-story-3-ui-update-infrastructure-resource-type-specific-metadata-to-new-rd-version)

### User story 1: [UI] Update data resource type specific metadata to new RD version

Backlog item SIMPL-18685

#### Storycard

**As a** Provider end user\
**I want to** update the data resource type specific metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider updates data resource type specific metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Data metadata" in the wizard\
   **Then** the data resource type specific metadata are prefilled with the values from the previous version\
   **And** I can update the data resource type specific metadata.

### User story 2: [UI] Update application resource type specific metadata to new RD version

Backlog item SIMPL-20970

#### Storycard

**As a** Provider end user\
**I want to** update the application resource type specific metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider updates application resource type specific metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Application metadata" in the wizard\
   **Then** the application resource type specific metadata are prefilled with the values from the previous version\
   **And** I can update the application resource type specific metadata.

### User story 3: [UI] Update infrastructure resource type specific metadata to new RD version

Backlog item SIMPL-21641

#### Storycard

**As a** Provider end user\
**I want to** update the infrastructure resource type specific metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

*Note: Out of scope is updating the deployment script. This story only covers general infrastructure metadata.*

**Scenario:** A Provider updates infrastructure resource type specific metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Infrastructure metadata" in the wizard\
   **Then** the infrastructure resource type specific metadata are prefilled with the values from the previous version\
   **And** I can update the infrastructure resource type specific metadata.

---

Back to [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
