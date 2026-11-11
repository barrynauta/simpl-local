<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider updates common metadata of a new version of a Resource description via UI (Improvement)

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.08 Create a new version of a resource description](../README.md#bp05b08-create-a-new-version-of-a-resource-description).

In the backlog (Jira) it is item SIMPL-32140. It has no user stories yet.

## What this Epic delivers

#### Improvement

This epic was created to address improvement activities needed to align the implementation with the latest API Definition Guidelines, specifically for aspects that were not previously handled correctly.

#### Storycard

**As a** Provider end user\
**I want to** update the common metadata of a new version of a resource description via UI\
**So that** I can align the resource description with the latest information related to my resource

#### Acceptance criteria

**Scenario:** A Provider updates common metadata of a new version of a resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Common metadata" in the wizard\
   **Then** the common metadata are prefilled with the values from the previous version\
   **And** I can update common metadata.

---

Back to [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
