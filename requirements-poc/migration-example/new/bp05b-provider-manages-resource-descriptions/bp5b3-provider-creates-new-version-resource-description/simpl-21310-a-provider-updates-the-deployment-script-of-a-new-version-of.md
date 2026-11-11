<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates the deployment script of a new version of an infrastructure Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.09 Update resource type specific metadata](../README.md#bp05b09-update-resource-type-specific-metadata).

In the backlog it is item SIMPL-21310, status **Defined**. It has no user stories yet.

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** update the deployment script of a new version of an infrastructure resource description via UI\
**So that** I can ensure that the new resource description includes all relevant information to be compliant with the data space schema of the selected resource type.

#### Acceptance criteria

**Scenario:** A Provider updates the deployment script of a new version of an infrastructure Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource Descriptions\
   **And** I am creating a new version of an infrastructure resource description\
   **When** I reach the step "Resource type specific metadata" in the wizard\
   **Then** the deployment script is prefilled with the values from the previous version\
   **Then** I can update the deployment script.

---

Back to [5B.3.2 A Provider updates the resource type specific metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b32-a-provider-updates-the-resource-type-specific-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
