<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider starts the creation of a new version of a Resource description via UI

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.08 Create a new version of a resource description](../README.md#bp05b08-create-a-new-version-of-a-resource-description).

In the backlog (Jira) it is item SIMPL-19293. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider end user\
**I want to** start the creation of a new version of a Resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider starts the creation of a new version of a Resource description via UI

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I demand to create a new version of this Resource description\
   **Then** I am guided to the first step of the wizard based on the latest version of the underlying schema for this resource type.

## User stories

1. [[UI] A Provider starts the creation of a new version of a Resource description via UI](#user-story-1-ui-a-provider-starts-the-creation-of-a-new-version-of-a-resource-description-via-ui)

### User story 1: [UI] A Provider starts the creation of a new version of a Resource description via UI

Backlog item SIMPL-21651

#### Storycard

**As a** Provider end user\
**I want to** start the creation of a new version of a Resource description via UI\
**So that** I can align the resource description with the latest information related to my resource.

#### Acceptance criteria

**Scenario:** A Provider starts the creation of a new version of a resource description via UI - positive case

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I demand to create a new version of this Resource description (by pressing the "Create New Version" button on the resource description detail view)\
   **Then** I am guided to the first step of the wizard based on the latest version of the underlying schema for this resource type (obtained via API).

**Scenario:** A Provider starts the creation of a new version of a resource description via UI - API error

2. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am consulting the full contents of a Resource description\
   **When** I demand to create a new version of this Resource description (by pressing the "Create New Version" button on the resource description detail view)Then\
   **And** the API to retrieve the latest version of the schema of the updated resource description throws an error\
   **Then** the creation of a new version of a resource description is not possible\
   **And** a notification is shown that a new version of the resource description cannot be created.

**Scenario:** A Provider starts the creation of a new version of a resource description via UI - not authenticated

3. **Given** I am not authenticated as Provider\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

**Scenario:** A Provider starts the creation of a new version of a resource description via UI  - no role

4. **Given** I am authenticated as Provider\
   **And** I do not have a role authorising the management of resource rescriptions\
   **When** try to open the resource description management page\
   **Then** I cannot open the resource description management page.

#### Additional notes

Trigger for a new version on the detail page:

(image not shown)

(image not shown)

---

Back to [5B.3.1 A Provider updates the common metadata of a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b31-a-provider-updates-the-common-metadata-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
