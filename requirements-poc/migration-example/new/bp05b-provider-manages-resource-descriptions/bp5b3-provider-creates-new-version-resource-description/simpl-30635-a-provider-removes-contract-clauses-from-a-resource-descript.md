<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.3 - A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) / [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description) / Epic
</div>

# Epic: A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description), under high-level requirement [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md). In the process it belongs to step [BP05B.10 Update policies and contract related metadata](../README.md#bp05b10-update-policies-and-contract-related-metadata).

In the backlog (Jira) it is item SIMPL-30635. It has 1 user story, each with acceptance criteria that say when the work is done. For each story, the development status shows whether it is Planned, In development or Developed (built, not necessarily released yet).

## What this Epic delivers

**As a** Provider\
**I want to** remove contract clauses from a resource description, resulting in a new version of the resource description\
**So that** the terms and conditions for using and providing a resource are updated

**Acceptance criteria:**

**Scenario:** A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step "Contract clauses" in the wizard\
   **Then** the contract clauses attributes are prefilled with the values from the previous version\
   **And** I can remove contract clauses from the resource description

## User stories

1. [A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description](#user-story-1-a-provider-removes-contract-clauses-from-a-resource-description-via-ui-resulting-in-a-new-version-of-the-resource-description) · Planned

### User story 1: A Provider removes contract clauses from a resource description via UI, resulting in a new version of the resource description

**Development:** Planned · backlog item SIMPL-30641

**As a** Provider end user\
**I want to** remove contract clauses from a resource description via UI\
**So that** I can ensure that the new version of the resource description includes the updated set of contract clauses

**Acceptance Criteria**

**AC1 - A Provider removes contract clauses from a resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **When** I reach the step 'Contract Clauses' in the wizard\
   **Then** the UI shows the properties of the contract clauses prefilled with the values from the previous version\
   **And** I can remove one or more of the previously defined contract clauses

**AC2 - A Provider finishes removing contract clauses via UI, resulting in a new version of a resource description**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I reach the step 'Contract Clauses' in the wizard\
   **When** I choose to remove one or more of the previously defined contract clauses in the UI wizard,\
   **Then** those contract clauses are not associated anymore in the new version of the resource description\
   **And** I can navigate to the next step in the wizard

**References / Supporting Material / Documentation / Validation rules:** Contract Clauses management (internal page)

---

Back to [5B.3.5 A Provider updates the set of contract clauses of a new version of a resource description](../bp5b3-provider-creates-new-version-resource-description.md#5b35-a-provider-updates-the-set-of-contract-clauses-of-a-new-version-of-a-resource-description) · [5B.3 A Provider creates a new version of a Resource description](../bp5b3-provider-creates-new-version-resource-description.md) · [BP05B process page](../README.md)
