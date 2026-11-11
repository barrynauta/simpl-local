<div style="background-color:#f8f8f8;border:1px solid #d1d5da;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
📍 You are here [🏠 Home](https://code.europa.eu/simpl/simpl-open) / [Foundations](https://code.europa.eu/simpl/simpl-open/foundations) / [Business Processes](https://code.europa.eu/simpl/simpl-open/foundations/business-processes) / [BP05B - Provider manages resource descriptions](../README.md) / [5B.2 - A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) / [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) / Epic
</div>

# Epic: A Provider adds contract clauses to a new Resource description via UI [Extension]

An Epic is a feature that can be tested as a whole. This one delivers part of detailed requirement [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description), under high-level requirement [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md). In the process it belongs to step [BP05B.03 Add policy and contract related metadata](../README.md#bp05b03-add-policy-and-contract-related-metadata).

In the backlog it is item SIMPL-30655, status **Defined**. It has 1 user story, each with acceptance criteria that say when the work is done.

## What this Epic delivers

This epic extends the creation and association of contract clauses to a new resource description via UI, by allowing the Provider to remove contract clauses before submitting the resource description.

## User stories

1. [A Provider removes contract clauses from a resource description via UI, when creating a new resource description](#user-story-1-a-provider-removes-contract-clauses-from-a-resource-description-via-ui-when-creating-a-new-resource-description) (PBI Defined)

### User story 1: A Provider removes contract clauses from a resource description via UI, when creating a new resource description

**Status:** PBI Defined (backlog item SIMPL-30657)

**As a** Provider end user\
**I want to** remove previously added contract clauses via UI before the submission of the resource description\
**So that** I can ensure that the resource description includes the updated set of contract clauses

**Acceptance Criteria**

**AC1 - A Provider removes contract clauses from a resource description via UI**

1. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new resource description\
   **And** I have reached the step 'Contract Clauses' in the wizard\
   **When** I have created one or more contract clauses\
   **Then** I can remove one or more of the previously defined contract clauses

**AC2 - A Provider finishes removing contract clauses via UI**

2. **Given** I am authenticated as a Provider end user\
   **And** I have a role authorising the management of resource descriptions\
   **And** I am creating a new version of a resource description\
   **And** I have reached the step 'Contract Clauses' in the wizard\
   **And** I have created one or more contract clauses\
   **When** I choose to remove one or more of the previously defined contract clauses in the UI wizard,\
   **Then** those contract clauses are not part of the set of contract clauses that will be associated with the resource description anymore\
   **And** I can navigate to the next step in the wizard

**References / Supporting Material / Documentation / Validation rules:** Contract Clauses management (internal page)

---

Back to [5B.2.6 A Provider adds a set of contract clauses to a new resource description](../bp5b2-provider-creates-new-resource-description.md#5b26-a-provider-adds-a-set-of-contract-clauses-to-a-new-resource-description) · [5B.2 A Provider creates a new Resource description](../bp5b2-provider-creates-new-resource-description.md) · [BP05B process page](../README.md)
