<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.5 - The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) / [5B.5.2 The Governance Authority notifies a Provider about an inactive schema](../bp5b5-governance-authority-publishes-resource-description.md#5b52-the-governance-authority-notifies-a-provider-about-an-inactive-schema) / Epic
</div>

# Epic: The Governance Authority notifies a Provider about an invalid schema

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.5.2 The Governance Authority notifies a Provider about an inactive schema](../bp5b5-governance-authority-publishes-resource-description.md#5b52-the-governance-authority-notifies-a-provider-about-an-inactive-schema), under high-level requirement [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md). In the process it belongs to step [BP05B.06 Remediate validation issues](../README.md#bp05b06-remediate-validation-issues).

In the backlog (Jira) it is item SIMPL-17798. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

#### Storycard

**As** Governance Authority\
**I want to** notify a Provider about an invalid schema of a new resource description or a new version of a resource description\
**So that** the Provider can correct the errors.

#### Acceptance criteria

**Scenario:** The Governance Authority notifies a Provider about an inactive schema of a new resource description or a new version of a resource description (Schema status validation - notification in negative case)

1. **Given** I have received a new resource description or a new version of a resource description with an inactive schema\
   **When** it is checked if the schema of that resource description is active\
   **And** schema status validation for the resource description or new version of a resource description has the result "invalid"\
   **Then** a notification about an inactive schema error is sent to the Provider.

**Scenario:** The Governance Authority notifies a Provider about a different resource type of a new version of a resource description (Schema type validation - notification in negative case)

2. **Given** I have received a new version of a resource description with resource type different to the resource type of the previous version\
   **When** it is checked if the schema of that resource description has the same resource type as the previous version\
   **And** schema type validation for the new version of the resource description has the result "invalid"\
   **Then** a notification about a wrong resource type is sent to the Provider.

## User stories

1. [[API] The Governance Authority notifies a Provider about an invalid schema](#user-story-1-api-the-governance-authority-notifies-a-provider-about-an-invalid-schema) · Developed

### User story 1: [API] The Governance Authority notifies a Provider about an invalid schema

**Development:** Developed · backlog item SIMPL-21597

#### Storycard

**As** Governance Authority\
**I want to** notify a Provider about an invalid schema of a new resource description or a new version of a resource description\
**So that** the Provider can correct the errors.

#### Acceptance criteria

**Scenario:** The Governance Authority notifies a Provider about an inactive schema of a new resource description or a new version of a resource description (Schema status validation - notification in negative case)

1. **Given** I have received a new resource description or a new version of a resource description with an inactive schema from a Provider\
   **When** it is checked if the schema of that resource description is active\
   **And** schema status validation for the resource description or new version of a resource description has the result "invalid"\
   **Then** a notification about an inactive schema error is sent to the Provider.

**Scenario:** The Governance Authority notifies a Provider about a different resource type of a new version of a resource description (Schema type validation - notification in negative case)

2. **Given** I have received a new resource description or a new version of a resource description with an inactive schema from a Provider\
   **When** it is checked if the schema of that resource description has the same resource type as the previous version\
   **And** schema type validation for the new version of the resource description has the result "invalid"\
   **Then** a notification about a wrong resource type is sent to the Provider.

#### Technical details

A http response with a suitable error code and a meaningful error description is sufficient.

---

Back to [5B.5.2 The Governance Authority notifies a Provider about an inactive schema](../bp5b5-governance-authority-publishes-resource-description.md#5b52-the-governance-authority-notifies-a-provider-about-an-inactive-schema) · [5B.5 The Governance Authority publishes a Resource description](../bp5b5-governance-authority-publishes-resource-description.md) · [BP05B process page](../README.md)
