<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.4 - A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md) / [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md#5b41-a-provider-signs-and-submits-a-new-resource-description-or-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider receives notifications about validation errors via UI or API

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md#5b41-a-provider-signs-and-submits-a-new-resource-description-or-a-new-version-of-a-resource-description), under high-level requirement [5B.4 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md). In the process it belongs to step [BP05B.04 Sign and submit the resource description](../README.md#bp05b04-sign-and-submit-the-resource-description).

In the backlog (Jira) it is item SIMPL-2282. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

#### Storycard

**As a** Provider\
**I want to** receive notifications about errors during validation on Governance Authority via UI or API\
**So that** I can to correct the Resource description and resubmit it for publication.

#### Acceptance criteria

**Scenario:** A provider receives notifications about validation errors via UI or API

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I have created a Resource description or a new version of a Resource description\
   **And** there are errors in the Resource description or new version of a Resource description\
   **When** I submit the new Resource description or new version of a Resource description\
   **And** the Governance Authority validates the resource description or new version of a Resource description\
   **Then** the validation errors are provided via UI or API

## User stories

1. [[UI] A Provider views notifications about validation errors](#user-story-1-ui-a-provider-views-notifications-about-validation-errors)

### User story 1: [UI] A Provider views notifications about validation errors

Backlog item SIMPL-21642

#### Storycard

**As a** Provider end user\
**I want to** view notifications about errors during validation on Governance Authority side\
**So that** I can correct the Resource description and resubmit it for publication.

#### Acceptance criteria

**Scenario:** A provider views notifications about validation errors

1. **Given** I am authenticated as Provider\
   **And** I have a role authorising the management of Resource descriptions\
   **And** I have created a Resource description or a new version of a Resource description\
   **And** there are errors in the Resource description or new version of a Resource description\
   **When** I submit the new Resource description or new version of a Resource description\
   **And** the Governance Authority validates the resource description or new version of a Resource description\
   **Then** the validation errors are shown in the UI

---

Back to [5B.4.1 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md#5b41-a-provider-signs-and-submits-a-new-resource-description-or-a-new-version-of-a-resource-description) · [5B.4 A Provider signs and submits a new Resource description or a new version of a Resource description](../bp5b4-provider-signs-and-submits-new-resource-description-or-new-version-resource.md) · [BP05B process page](../README.md)
